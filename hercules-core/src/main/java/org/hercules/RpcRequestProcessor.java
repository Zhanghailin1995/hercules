package org.hercules;

import com.google.protobuf.Message;
import org.hercules.util.RpcFactoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * org.hercules.RpcRequestProcessor
 *
 * @author zhanghailin
 */
public abstract class RpcRequestProcessor<T extends Message> implements RpcProcessor<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Executor executor;
    private final Message defaultResp;

    public abstract Message processRequest(final T request, final RpcRequestClosure done);

    public RpcRequestProcessor(Executor executor, Message defaultResp) {
        super();
        this.executor = executor;
        this.defaultResp = defaultResp;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final T request) {
        try {
            final Message msg = processRequest(request, new RpcRequestClosure(rpcCtx, this.defaultResp));
            if (msg != null) {
                rpcCtx.sendResponse(msg);
            }
        } catch (final Throwable t) {
            LOG.error("handleRequest {} failed", request, t);
            rpcCtx.sendResponse(RpcFactoryHelper //
                    .responseFactory() //
                    .newResponse(defaultResp(), -1, "handleRequest internal error"));
        }
    }

    @Override
    public Executor executor() {
        return this.executor;
    }

    public Message defaultResp() {
        return this.defaultResp;
    }
}
