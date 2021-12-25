package org.hercules.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.hercules.*;
import org.hercules.error.InvokeTimeoutException;
import org.hercules.error.RemotingException;
import org.hercules.error.RpcError;
import org.hercules.option.RpcOptions;
import org.hercules.proto.RpcRequests;
import org.hercules.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Abstract RPC client service based.
 * <p>
 * org.hercules.impl.AbstractClientService
 *
 * @author zhanghailin
 */
public abstract class AbstractClientService implements ClientService {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractClientService.class);

    protected volatile RpcClient rpcClient;
    protected ThreadPoolExecutor rpcExecutor;
    protected RpcOptions rpcOptions;

    public RpcClient getRpcClient() {
        return this.rpcClient;
    }

    @Override
    public boolean isConnected(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        return rc != null && isConnected(rc, endpoint);
    }

    private static boolean isConnected(final RpcClient rpcClient, final Endpoint endpoint) {
        return rpcClient.checkConnection(endpoint);
    }

    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        if (this.rpcClient != null) {
            return true;
        }
        this.rpcOptions = rpcOptions;
        return initRpcClient(this.rpcOptions.getRpcProcessorThreadPoolSize());
    }

    protected void configRpcClient(final RpcClient rpcClient) {
        // NO-OP
    }

    protected boolean initRpcClient(final int rpcProcessorThreadPoolSize) {
        final RpcFactory factory = RpcFactoryHelper.rpcFactory();
        this.rpcClient = factory.createRpcClient(factory.defaultClientConfigHelper(this.rpcOptions));
        configRpcClient(this.rpcClient);
        this.rpcClient.init(null);
        this.rpcExecutor = ThreadPoolUtil.newBuilder() //
                .poolName("JRaft-RPC-Processor") //
                .enableMetric(true) //
                .coreThreads(rpcProcessorThreadPoolSize / 3) //
                .maximumThreads(rpcProcessorThreadPoolSize) //
                .keepAliveSeconds(60L) //
                .workQueue(new ArrayBlockingQueue<>(10000)) //
                .threadFactory(new NamedThreadFactory("RPC-Processor-", true)) //
                .build();
        return true;
    }

    @Override
    public synchronized void shutdown() {
        if (this.rpcClient != null) {
            this.rpcClient.shutdown();
            this.rpcClient = null;
            this.rpcExecutor.shutdown();
        }
    }

    @Override
    public boolean connect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            throw new IllegalStateException("Client service is uninitialized.");
        }
        if (isConnected(rc, endpoint)) {
            return true;
        }
        try {
            final RpcRequests.PingRequest req = RpcRequests.PingRequest.newBuilder() //
                    .setSendTimestamp(System.currentTimeMillis()) //
                    .build();
            final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) rc.invokeSync(endpoint, req,
                    this.rpcOptions.getRpcConnectTimeoutMs());
            return resp.getErrorCode() == 0;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final RemotingException e) {
            LOG.error("Fail to connect {}, remoting exception: {}.", endpoint, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disconnect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            return true;
        }
        LOG.info("Disconnect from {}.", endpoint);
        rc.closeConnection(endpoint);
        return true;
    }

    @Override
    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        return invokeWithDone(endpoint, request, null, done, timeoutMs, rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, ctx, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        final RpcClient rc = this.rpcClient;
        final FutureImpl<Message> future = new FutureImpl<>();
        final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;
        try {
            if (rc == null) {
                future.failure(new IllegalStateException("Client service is uninitialized."));
                // TODO
                // should be in another thread to avoid dead locking.
                RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RpcError.EINTERNAL,
                        "Client service is uninitialized."));
                return future;
            }

            rc.invokeAsync(endpoint, request, ctx, new InvokeCallback() {

                @SuppressWarnings({"unchecked", "ConstantConditions"})
                @Override
                public void complete(final Object result, final Throwable err) {
                    if (future.isCancelled()) {
                        onCanceled(request, done);
                        return;
                    }

                    if (err == null) {
                        Status status = Status.OK();
                        Message msg;
                        if (result instanceof RpcRequests.ErrorResponse) {
                            status = handleErrorResponse((RpcRequests.ErrorResponse) result);
                            msg = (Message) result;
                        } else if (result instanceof Message) {
                            final Descriptors.FieldDescriptor fd = ((Message) result).getDescriptorForType() //
                                    .findFieldByNumber(RpcResponseFactory.ERROR_RESPONSE_NUM);
                            if (fd != null && ((Message) result).hasField(fd)) {
                                final RpcRequests.ErrorResponse eResp = (RpcRequests.ErrorResponse) ((Message) result).getField(fd);
                                status = handleErrorResponse(eResp);
                                msg = eResp;
                            } else {
                                msg = (T) result;
                            }
                        } else {
                            msg = (T) result;
                        }
                        if (done != null) {
                            try {
                                if (status.isOk()) {
                                    done.setResponse((T) msg);
                                }
                                done.run(status);
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.setResult(msg);
                        }
                    } else {
                        if (done != null) {
                            try {
                                done.run(new Status(err instanceof InvokeTimeoutException ? RpcError.ETIMEDOUT
                                        : RpcError.EINTERNAL, "RPC exception:" + err.getMessage()));
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.failure(err);
                        }
                    }
                }
            }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done,
                    new Status(RpcError.EINTR, "Sending rpc was interrupted"));
        } catch (final RemotingException e) {
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RpcError.EINTERNAL,
                    "Fail to send a RPC request:" + e.getMessage()));

        }

        return future;
    }

    private static Status handleErrorResponse(final RpcRequests.ErrorResponse eResp) {
        final Status status = new Status();
        status.setCode(eResp.getErrorCode());
        if (eResp.hasErrorMsg()) {
            status.setErrorMsg(eResp.getErrorMsg());
        }
        return status;
    }

    private <T extends Message> void onCanceled(final Message request, final RpcResponseClosure<T> done) {
        if (done != null) {
            try {
                done.run(new Status(RpcError.ECANCELED, "RPC request was canceled by future."));
            } catch (final Throwable t) {
                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
            }
        }
    }
}
