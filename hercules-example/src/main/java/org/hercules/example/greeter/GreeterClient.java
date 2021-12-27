package org.hercules.example.greeter;

import org.hercules.RpcClient;
import org.hercules.RpcFactory;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * org.hercules.example.echo.GreeterClient
 *
 * @author zhanghailin
 */
public class GreeterClient {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("hercules.rpc.rpc_processor_interest_prefer_proto_name", "true");
        final Endpoint target = new Endpoint("127.0.0.1", 19992);
        // get rpc factory
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();
        // bolt and grpc register serializer is different
        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC) && Utils.isRpcProcessorInterestPreferProtoName()) {
            rpcFactory.registerProtobufSerializer(
                    Utils.generateGrpcServiceName(Greeter.GreeterRequest.getDescriptor().getFullName()), // full method name greeter.GreeterRequestService/_call
                    Greeter.GreeterRequest.getDefaultInstance(),
                    Greeter.GreeterReply.getDefaultInstance());
        } else {
            throw new UnsupportedOperationException("not support bolt");
        }
        // create
        final RpcClient client = rpcFactory.createRpcClient();
        client.init(null);

        for (int i = 0; i < 15; i++) {
            final Greeter.GreeterRequest req = Greeter.GreeterRequest.newBuilder() //
                    .setName("zhanghailin") //
                    .build();
            try {
                long start = System.currentTimeMillis();
                final Object resp = client.invokeSync(target, req, 3000);
                long end = System.currentTimeMillis();
                System.out.println(resp);
                System.out.println("cost: " + (end-start));
            } catch (final Exception e) {
                e.printStackTrace();
            }
            // Thread.sleep(1000);
        }
    }
}
