package org.hercules.example.helloworld;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;

/**
 * org.hercules.example.helloworld.HelloWorldRequestProcessor
 *
 * @author zhanghailin
 */
public class HelloWorldRequestProcessor implements RpcProcessor<HelloWorldRequest> {

    @Override
    public void handleRequest(RpcContext rpcCtx, HelloWorldRequest request) {
        rpcCtx.sendResponse(new HelloWorldResponse());
    }

    @Override
    public String interest() {
        return HelloWorldRequest.class.getName();
    }
}
