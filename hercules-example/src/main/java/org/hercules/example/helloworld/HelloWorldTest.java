package org.hercules.example.helloworld;

import org.hercules.RpcClient;
import org.hercules.RpcServer;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.example.helloworld.HelloWorldTest
 *
 * @author zhanghailin
 */
public class HelloWorldTest {

    public static void main(String[] args) throws InterruptedException {
        final RpcServer server = RpcFactoryHelper.rpcFactory().createRpcServer(new Endpoint("127.0.0.1", 19991));
        server.registerProcessor(new HelloWorldRequestProcessor());
        server.init(null);

        final Endpoint target = new Endpoint("127.0.0.1", 19991);

        final RpcClient client = RpcFactoryHelper.rpcFactory().createRpcClient();
        client.init(null);

        final HelloWorldRequest req = new HelloWorldRequest();

        try {
            final Object resp = client.invokeSync(target, req, 3000);
            System.out.println(resp);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        try {
            client.invokeAsync(target, req, (result, err) -> System.out.println(result), 3000);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        server.awaitTermination();
    }
}
