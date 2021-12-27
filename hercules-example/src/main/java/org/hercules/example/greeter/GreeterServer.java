package org.hercules.example.greeter;

import org.hercules.RpcFactory;
import org.hercules.RpcServer;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * org.hercules.example.echo.GreeterServer
 *
 * @author zhanghailin
 */
public class GreeterServer {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("hercules.rpc.rpc_processor_interest_prefer_proto_name", "true");
        // Bolt also support protobuf serialize. but we need to register
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();

        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            rpcFactory.registerProtobufSerializer(
                    Utils.generateGrpcServiceName(Greeter.GreeterRequest.getDescriptor().getFullName()),
                    Greeter.GreeterRequest.getDefaultInstance(),
                    Greeter.GreeterReply.getDefaultInstance());
        } else {
            throw new UnsupportedOperationException("not support bolt");
        }

        final RpcServer server = rpcFactory.createRpcServer(new Endpoint("127.0.0.1", 19992));

        // if use BOLT, The second and subsequent parameters are not required
        server.registerProcessor(new GreeterRequestProcessor());

        server.init(null);
        server.awaitTermination();
    }
}
