package org.hercules.impl.grpc;

import com.google.protobuf.Message;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;
import org.hercules.*;
import org.hercules.proto.ProtobufMsgFactory;
import org.hercules.util.ExecutorServiceHelper;
import org.hercules.util.NamedThreadFactory;
import org.hercules.util.Requires;
import org.hercules.util.ThreadPoolUtil;
import org.hercules.util.internal.ThrowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GRPC RPC server implement.
 * org.hercules.impl.grpc.GrpcServer
 *
 * @author zhanghailin
 */
public class GrpcServer implements RpcServer {

    static {
        ProtobufMsgFactory.load();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

    private static final String EXECUTOR_NAME = "grpc-default-executor";

    private final Server server;
    private final MutableHandlerRegistry handlerRegistry;
    private final Map<String, Message> parserClasses;
    private final MarshallerRegistry marshallerRegistry;
    private final List<ServerInterceptor> serverInterceptors = new CopyOnWriteArrayList<>();
    private final List<ConnectionClosedEventListener> closedEventListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    private ExecutorService defaultExecutor;

    public GrpcServer(Server server, MutableHandlerRegistry handlerRegistry, Map<String, Message> parserClasses,
                      MarshallerRegistry marshallerRegistry) {
        this.server = server;
        this.handlerRegistry = handlerRegistry;
        this.parserClasses = parserClasses;
        this.marshallerRegistry = marshallerRegistry;
        registerDefaultServerInterceptor();
    }

    @Override
    public boolean init(final Void opts) {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("grpc server has started");
        }

        this.defaultExecutor = ThreadPoolUtil.newBuilder() //
                .poolName(EXECUTOR_NAME) //
                .enableMetric(false) //
                .coreThreads(Math.min(20, GrpcRpcFactory.RPC_SERVER_PROCESSOR_POOL_SIZE / 5)) //
                .maximumThreads(GrpcRpcFactory.RPC_SERVER_PROCESSOR_POOL_SIZE) //
                .keepAliveSeconds(60L) //
                .workQueue(new SynchronousQueue<>()) //
                .threadFactory(new NamedThreadFactory(EXECUTOR_NAME + "-", true)) //
                .rejectedHandler((r, executor) -> {
                    throw new RejectedExecutionException("[" + EXECUTOR_NAME + "], task " + r.toString() +
                            " rejected from " +
                            executor.toString());
                })
                .build();

        try {
            this.server.start();
        } catch (final IOException e) {
            ThrowUtil.throwException(e);
        }
        return true;
    }

    @Override
    public void shutdown() {
        if (!this.started.compareAndSet(true, false)) {
            return;
        }
        ExecutorServiceHelper.shutdownAndAwaitTermination(this.defaultExecutor);
        GrpcServerHelper.shutdownAndAwaitTermination(this.server);
    }

    @Override
    public void registerConnectionClosedEventListener(final ConnectionClosedEventListener listener) {
        this.closedEventListeners.add(listener);
    }

    private void registerProtobufSerializer(String className, Object... args) {
        this.parserClasses.put(className, (Message) args[0]);
        this.marshallerRegistry.registerResponseInstance(className, (Message) args[1]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerProcessor(final RpcProcessor processor, Object... args) {
        final String interest = processor.interest();
        if (args != null && args.length >= 2) {
            registerProtobufSerializer(interest, args);
        }
        final Message reqIns = Requires.requireNonNull(this.parserClasses.get(interest), "null default instance: " + interest);
        final MethodDescriptor<Message, Message> method = MethodDescriptor //
                .<Message, Message>newBuilder() //
                .setType(MethodDescriptor.MethodType.UNARY) //
                .setFullMethodName(
                        MethodDescriptor.generateFullMethodName(processor.interest(), GrpcRpcFactory.FIXED_METHOD_NAME)) //
                .setRequestMarshaller(ProtoUtils.marshaller(reqIns)) //
                .setResponseMarshaller(ProtoUtils.marshaller(this.marshallerRegistry.findResponseInstanceByRequest(interest))) //
                .build();

        final ServerCallHandler<Message, Message> handler = ServerCalls.asyncUnaryCall(
                (request, responseObserver) -> {
                    final SocketAddress remoteAddress = RemoteAddressInterceptor.getRemoteAddress();
                    final Connection conn = ConnectionInterceptor.getCurrentConnection(this.closedEventListeners);

                    final RpcContext rpcCtx = new RpcContext() {

                        @Override
                        public void sendResponse(final Object responseObj) {
                            try {
                                responseObserver.onNext((Message) responseObj);
                                responseObserver.onCompleted();
                            } catch (final Throwable t) {
                                LOG.warn("[GRPC] failed to send response.", t);
                            }
                        }

                        @Override
                        public Connection getConnection() {
                            if (conn == null) {
                                throw new IllegalStateException("fail to get connection");
                            }
                            return conn;
                        }

                        @Override
                        public String getRemoteAddress() {
                            // Rely on GRPC's capabilities, not magic (netty channel)
                            return remoteAddress != null ? remoteAddress.toString() : null;
                        }
                    };

                    final RpcProcessor.ExecutorSelector selector = processor.executorSelector();
                    Executor executor;
                    if (selector != null) {
                        executor = selector.select(interest);
                    } else {
                        executor = processor.executor();
                    }

                    if (executor == null) {
                        executor = this.defaultExecutor;
                    }

                    if (executor != null) {
                        executor.execute(() -> processor.handleRequest(rpcCtx, request));
                    } else {
                        processor.handleRequest(rpcCtx, request);
                    }
                });

        final ServerServiceDefinition serviceDef = ServerServiceDefinition //
                .builder(interest) //
                .addMethod(method, handler) //
                .build();
        // toArray() equals toArray(new Object[0])
        this.handlerRegistry
                .addService(ServerInterceptors.intercept(serviceDef, this.serverInterceptors.toArray(new ServerInterceptor[0])));
    }

    @Override
    public int boundPort() {
        return this.server.getPort();
    }

    public void setDefaultExecutor(ExecutorService defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    public Server getServer() {
        return server;
    }

    public MutableHandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public boolean addServerInterceptor(final ServerInterceptor interceptor) {
        return this.serverInterceptors.add(interceptor);
    }

    private void registerDefaultServerInterceptor() {
        this.serverInterceptors.add(new RemoteAddressInterceptor());
        this.serverInterceptors.add(new ConnectionInterceptor());
    }
}
