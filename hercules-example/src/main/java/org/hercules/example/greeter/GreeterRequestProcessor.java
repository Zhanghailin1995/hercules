package org.hercules.example.greeter;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * com.qifeizn.rpc.echo.GreeterRequestProcessor
 *
 * @author zhanghailin
 */
public class GreeterRequestProcessor implements RpcProcessor<Greeter.GreeterRequest> {

    @Override
    public void handleRequest(RpcContext rpcCtx, Greeter.GreeterRequest request) {
        Greeter.GreeterReply.Builder builder = Greeter.GreeterReply.newBuilder();
        Greeter.GreeterReply resp = builder
                .setMessage("hello " + request.getName())
                .build();
        rpcCtx.sendResponse(resp);
    }

    @Override
    public String interest() {
        if (RpcFactoryHelper.rpcFactory().factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            return Utils.generateGrpcServiceName(Greeter.GreeterRequest.getDescriptor().getFullName());
        }
        return Greeter.GreeterRequest.class.getName();
    }
}
