package org.hercules.example.echo;

import org.hercules.RpcFactory;
import org.hercules.RpcServer;
import org.hercules.impl.PingRequestProcessor;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * org.hercules.example.echo.GreeterServer
 *
 * @author zhanghailin
 */
public class EchoServer {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("hercules.rpc.rpc_processor_interest_prefer_proto_name", "true");
        // Bolt also support protobuf serialize. but we need to register
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();

        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            rpcFactory.registerProtobufSerializer(
                    Utils.generateGrpcServiceName(Echo.EchoRequest.getDescriptor().getFullName()),
                    Echo.EchoRequest.getDefaultInstance(),
                    Echo.EchoResponse.getDefaultInstance());
        } else {
            rpcFactory.registerProtobufSerializer(Echo.EchoRequest.class.getName(), EchoSerializer.INSTANCE);
            rpcFactory.registerProtobufSerializer(Echo.EchoResponse.class.getName(), EchoSerializer.INSTANCE);
        }

        final RpcServer server = rpcFactory.createRpcServer(new Endpoint("127.0.0.1", 19992));

        // if use BOLT, The second and subsequent parameters are not required
        server.registerProcessor(new PingRequestProcessor());
        server.registerProcessor(new EchoRequestProcessor());

        server.init(null);
        server.awaitTermination();
    }
}
