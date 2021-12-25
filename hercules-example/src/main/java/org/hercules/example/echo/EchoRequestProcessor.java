package org.hercules.example.echo;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;

/**
 * com.qifeizn.rpc.echo.EchoRequestProcessor
 *
 * @author zhanghailin
 */
public class EchoRequestProcessor implements RpcProcessor<Echo.EchoRequest> {

    @Override
    public void handleRequest(RpcContext rpcCtx, Echo.EchoRequest request) {
        Echo.EchoResponse.Builder builder = Echo.EchoResponse.newBuilder();
        Echo.EchoResponse resp = builder
                .setRespMsg(request.getReqMsg())
                .build();
        rpcCtx.sendResponse(resp);
    }

    @Override
    public String interest() {
        return Echo.EchoRequest.class.getName();
    }
}
