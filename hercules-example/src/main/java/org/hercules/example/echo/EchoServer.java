package org.hercules.example.echo;

import org.hercules.RpcFactory;
import org.hercules.RpcServer;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.example.echo.EchoServer
 *
 * @author zhanghailin
 */
public class EchoServer {

    public static void main(String[] args) throws InterruptedException {
        // Bolt also support protobuf serialize. but we need to register
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();

        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)) {
            rpcFactory.registerProtobufSerializer(Echo.EchoRequest.class.getName(),
                    Echo.EchoRequest.getDefaultInstance(), Echo.EchoResponse.getDefaultInstance());
        } else {
            rpcFactory.registerProtobufSerializer(Echo.EchoRequest.class.getName(), EchoSerializer.INSTANCE);
            rpcFactory.registerProtobufSerializer(Echo.EchoResponse.class.getName(), EchoSerializer.INSTANCE);
        }

        final RpcServer server = rpcFactory.createRpcServer(new Endpoint("127.0.0.1", 19992));

        // if use BOLT, The second and subsequent parameters are not required
        server.registerProcessor(new EchoRequestProcessor());

        server.init(null);
        server.awaitTermination();
    }
}
