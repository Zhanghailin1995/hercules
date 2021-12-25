package org.hercules.example.echo;

import org.hercules.RpcClient;
import org.hercules.RpcFactory;
import org.hercules.proto.RpcRequests;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.example.echo.EchoClient
 *
 * @author zhanghailin
 */
public class EchoClient {

    public static void main(String[] args) throws InterruptedException {
        final Endpoint target = new Endpoint("127.0.0.1", 19992);
        // get rpc factory
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();
        // bolt and grpc register serializer is different
        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)) {
            rpcFactory.registerProtobufSerializer(Echo.EchoRequest.class.getName(),
                    Echo.EchoRequest.getDefaultInstance(), Echo.EchoResponse.getDefaultInstance());
        } else {
            rpcFactory.registerProtobufSerializer(Echo.EchoRequest.class.getName(), EchoSerializer.INSTANCE);
            rpcFactory.registerProtobufSerializer(Echo.EchoResponse.class.getName(), EchoSerializer.INSTANCE);
        }
        // create
        final RpcClient client = rpcFactory.createRpcClient();
        client.init(null);



        final RpcRequests.PingRequest ping = RpcRequests.PingRequest.newBuilder() //
                .setSendTimestamp(System.currentTimeMillis()) //
                .build();

        try {
            final Object resp = client.invokeSync(target, ping, 3000);
            System.out.println(resp);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 15; i++) {
            final Echo.EchoRequest req = Echo.EchoRequest.newBuilder() //
                    .setReqMsg(String.valueOf(System.currentTimeMillis())) //
                    .build();
            try {
                final Object resp = client.invokeSync(target, req, 3000);
                System.out.println(resp);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }
}
