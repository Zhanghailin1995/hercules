package org.hercules.impl.grpc;

import com.google.protobuf.Message;
import io.grpc.*;
import io.grpc.netty.shaded.io.netty.util.internal.SystemPropertyUtil;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.hercules.InvokeCallback;
import org.hercules.InvokeContext;
import org.hercules.RpcClient;
import org.hercules.error.InvokeTimeoutException;
import org.hercules.error.RemotingException;
import org.hercules.option.RpcOptions;
import org.hercules.util.DirectExecutor;
import org.hercules.util.Endpoint;
import org.hercules.util.Requires;
import org.hercules.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * org.hercules.impl.grpc.GrpcClient
 *
 * @author zhanghailin
 */
public class GrpcClient implements RpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcClient.class);

    private static final int RESET_CONN_THRESHOLD = SystemPropertyUtil.getInt("hercules.grpc.max.conn.failures.to_reset", 2);

    private final Map<Endpoint, ManagedChannel> managedChannelPool = new ConcurrentHashMap<>();
    private final Map<Endpoint, AtomicInteger> transientFailures = new ConcurrentHashMap<>();
    private final Map<String, Message> parserClasses;
    private final MarshallerRegistry marshallerRegistry;

    public GrpcClient(Map<String, Message> parserClasses, MarshallerRegistry marshallerRegistry) {
        this.parserClasses = parserClasses;
        this.marshallerRegistry = marshallerRegistry;
    }

    @Override
    public boolean init(final RpcOptions opts) {
        // do nothing
        return true;
    }

    @Override
    public void shutdown() {
        closeAllChannels();
        this.transientFailures.clear();
    }

    @Override
    public boolean checkConnection(final Endpoint endpoint) {
        return checkConnection(endpoint, false);
    }

    @Override
    public boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent) {
        Requires.requireNonNull(endpoint, "endpoint");
        return checkChannel(endpoint, createIfAbsent);
    }

    @Override
    public void closeConnection(final Endpoint endpoint) {
        Requires.requireNonNull(endpoint, "endpoint");
        closeChannel(endpoint);
    }

    @Override
    public Object invokeSync(final Endpoint endpoint, final Object request, final InvokeContext ctx,
                             final long timeoutMs) throws RemotingException {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        invokeAsync(endpoint, request, ctx, (result, err) -> {
            if (err == null) {
                future.complete(result);
            } else {
                future.completeExceptionally(err);
            }
        }, timeoutMs);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new InvokeTimeoutException(e);
        } catch (final Throwable t) {
            future.cancel(true);
            throw new RemotingException(t);
        }
    }

    @Override
    public void invokeAsync(final Endpoint endpoint, final Object request, final InvokeContext ctx,
                            final InvokeCallback callback, final long timeoutMs) {
        Requires.requireNonNull(endpoint, "endpoint");
        Requires.requireNonNull(request, "request");

        final Executor executor = callback.executor() != null ? callback.executor() : DirectExecutor.INSTANCE;

        final Channel ch = getCheckedChannel(endpoint);
        if (ch == null) {
            executor.execute(() -> callback.complete(null, new RemotingException("Fail to connect: " + endpoint)));
            return;
        }
        final MethodDescriptor<Message, Message> method = getCallMethod(request);
        final CallOptions callOpts = CallOptions.DEFAULT.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

        ClientCalls.asyncUnaryCall(ch.newCall(method, callOpts), (Message) request, new StreamObserver<Message>() {
            @Override
            public void onNext(Message value) {
                executor.execute(() -> callback.complete(value, null));
            }

            @Override
            public void onError(Throwable throwable) {
                executor.execute(() -> callback.complete(null, throwable));
            }

            @Override
            public void onCompleted() {
                // NO-OP
            }
        });
    }

//    @Override
//    public void registerProtobufSerializer(String className, Object... args) {
//        this.parserClasses.put(className, (Message) args[0]);
//        this.marshallerRegistry.registerResponseInstance(className, (Message) args[1]);
//    }

    private MethodDescriptor<Message, Message> getCallMethod(final Object request) {
        // use descriptor full name replace  class name for support cross language call
        final String interest;
        if (request instanceof Message && Utils.isRpcProcessorInterestPreferProtoName()) {
            interest = ((Message) request).getDescriptorForType().getFullName();
        } else {
            interest = request.getClass().getName();
        }
        // find request Instance
        final Message reqIns = Requires.requireNonNull(this.parserClasses.get(interest), "null default instance: "
                + interest);

        return MethodDescriptor //
                .<Message, Message>newBuilder() //
                .setType(MethodDescriptor.MethodType.UNARY) //
                .setFullMethodName(MethodDescriptor.generateFullMethodName(interest, GrpcRpcFactory.FIXED_METHOD_NAME)) //
                .setRequestMarshaller(ProtoUtils.marshaller(reqIns)) //
                .setResponseMarshaller(
                        // find response instance
                        ProtoUtils.marshaller(this.marshallerRegistry.findResponseInstanceByRequest(interest))) //
                .build();

    }

    private ManagedChannel getCheckedChannel(final Endpoint endpoint) {
        final ManagedChannel ch = getChannel(endpoint, true);

        if (checkConnectivity(endpoint, ch)) {
            return ch;
        }

        return null;
    }

    private ManagedChannel getChannel(final Endpoint endpoint, final boolean createIfAbsent) {
        if (createIfAbsent) {
            return this.managedChannelPool.computeIfAbsent(endpoint, this::newChannel);
        } else {
            return this.managedChannelPool.get(endpoint);
        }
    }

    private ManagedChannel newChannel(final Endpoint endpoint) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress(endpoint.getIp(), endpoint.getPort())
                .usePlaintext()
                .directExecutor()
                .maxInboundMessageSize(GrpcRpcFactory.RPC_MAX_INBOUND_MESSAGE_SIZE)
                .build();

        LOG.info("Creating new channel to: {}.", endpoint);

        // The init channel state is IDLE
        notifyWhenStateChanged(ConnectivityState.IDLE, endpoint, ch);

        return ch;

    }

    private ManagedChannel removeChannel(final Endpoint endpoint) {
        return this.managedChannelPool.remove(endpoint);
    }

    private void notifyWhenStateChanged(final ConnectivityState state, final Endpoint endpoint, final ManagedChannel ch) {
        ch.notifyWhenStateChanged(state, () -> onStateChanged(endpoint, ch));
    }

    private void onStateChanged(final Endpoint endpoint, final ManagedChannel ch) {
        final ConnectivityState state = ch.getState(false);

        LOG.info("The channel {} is in state: {}.", endpoint, state);

        switch (state) {
            case READY:
                notifyReady(endpoint);
                notifyWhenStateChanged(ConnectivityState.READY, endpoint, ch);
                break;
            case TRANSIENT_FAILURE:
                notifyFailure(endpoint);
                notifyWhenStateChanged(ConnectivityState.TRANSIENT_FAILURE, endpoint, ch);
                break;
            case SHUTDOWN:
                notifyShutdown(endpoint);
                break;
            case CONNECTING:
                notifyWhenStateChanged(ConnectivityState.CONNECTING, endpoint, ch);
                break;
            case IDLE:
                notifyWhenStateChanged(ConnectivityState.IDLE, endpoint, ch);
                break;
        }
    }

    private void notifyReady(final Endpoint endpoint) {
        LOG.info("The channel {} has successfully established.", endpoint);
        clearConnFailuresCount(endpoint);

    }

    private void notifyFailure(final Endpoint endpoint) {
        LOG.warn("There has been some transient failure on this channel {}.", endpoint);
    }

    private void notifyShutdown(final Endpoint endpoint) {
        LOG.warn("This channel {} has started shutting down. Any new RPCs should fail immediately.", endpoint);
    }


    private int incConnFailuresCount(final Endpoint endpoint) {
        return this.transientFailures.computeIfAbsent(endpoint, ep -> new AtomicInteger()).incrementAndGet();
    }

    private void clearConnFailuresCount(final Endpoint endpoint) {
        this.transientFailures.remove(endpoint);
    }


    private void closeAllChannels() {
        for (final Map.Entry<Endpoint, ManagedChannel> entry : this.managedChannelPool.entrySet()) {
            final ManagedChannel ch = entry.getValue();
            LOG.info("Shutdown managed channel: {}, {}.", entry.getKey(), ch);
            ManagedChannelHelper.shutdownAndAwaitTermination(ch);
        }
    }

    private void closeChannel(final Endpoint endpoint) {
        final ManagedChannel ch = this.managedChannelPool.remove(endpoint);
        LOG.info("Close connection: {}, {}.", endpoint, ch);
        if (ch != null) {
            ManagedChannelHelper.shutdownAndAwaitTermination(ch);
        }
    }


    private boolean checkChannel(final Endpoint endpoint, final boolean createIfAbsent) {
        final ManagedChannel ch = getChannel(endpoint, createIfAbsent);

        if (ch == null) {
            return false;
        }

        return checkConnectivity(endpoint, ch);
    }

    private boolean checkConnectivity(final Endpoint endpoint, final ManagedChannel ch) {
        final ConnectivityState st = ch.getState(false);

        if (st != ConnectivityState.TRANSIENT_FAILURE && st != ConnectivityState.SHUTDOWN) {
            return true;
        }

        final int c = incConnFailuresCount(endpoint);
        if (c < RESET_CONN_THRESHOLD) {
            if (c == RESET_CONN_THRESHOLD - 1) {
                // For sub-channels that are in TRANSIENT_FAILURE state, short-circuit the backoff timer and make
                // them reconnect immediately. May also attempt to invoke NameResolver#refresh
                ch.resetConnectBackoff();
            }
            return true;
        }

        clearConnFailuresCount(endpoint);

        final ManagedChannel removedCh = removeChannel(endpoint);

        if (removedCh == null) {
            // The channel has been removed and closed by another
            return false;
        }

        LOG.warn("Channel[{}] in [INACTIVE] state {} times, it has been removed from the pool.", endpoint, c);

        if (removedCh != ch) {
            // Now that it's removed, close it
            ManagedChannelHelper.shutdownAndAwaitTermination(removedCh, 100);
        }

        ManagedChannelHelper.shutdownAndAwaitTermination(ch, 100);

        return false;
    }
}
