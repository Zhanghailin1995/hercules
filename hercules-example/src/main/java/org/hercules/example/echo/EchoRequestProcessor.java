package org.hercules.example.echo;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * com.qifeizn.rpc.echo.GreeterRequestProcessor
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
        if (RpcFactoryHelper.rpcFactory().factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            return "/"+Echo.EchoRequest.getDescriptor().getFullName()+"Service";
        }
        return Echo.EchoRequest.class.getName();
    }
}
