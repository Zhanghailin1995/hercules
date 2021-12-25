package org.hercules;

import com.google.protobuf.Message;
import org.hercules.util.RpcFactoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * org.hercules.RpcRequestClosure
 *
 * @author zhanghailin
 */
public class RpcRequestClosure implements Closure {

    private static final Logger LOG = LoggerFactory.getLogger(RpcRequestClosure.class);

    private static final AtomicIntegerFieldUpdater<RpcRequestClosure> STATE_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(
                    RpcRequestClosure.class,
                    "state");

    private static final int PENDING = 0;
    private static final int RESPOND = 1;

    private final RpcContext rpcCtx;
    private final Message defaultResp;

    private volatile int state = PENDING;

    public RpcRequestClosure(RpcContext rpcCtx) {
        this(rpcCtx, null);
    }

    public RpcRequestClosure(RpcContext rpcCtx, Message defaultResp) {
        super();
        this.rpcCtx = rpcCtx;
        this.defaultResp = defaultResp;
    }

    public RpcContext getRpcCtx() {
        return rpcCtx;
    }

    public void sendResponse(final Message msg) {
        if (!STATE_UPDATER.compareAndSet(this, PENDING, RESPOND)) {
            LOG.warn("A response: {} sent repeatedly!", msg);
            return;
        }
        this.rpcCtx.sendResponse(msg);
    }

    @Override
    public void run(final Status status) {
        sendResponse(RpcFactoryHelper.responseFactory().newResponse(this.defaultResp, status));
    }
}
