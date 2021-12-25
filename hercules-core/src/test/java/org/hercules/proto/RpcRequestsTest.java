package org.hercules.proto;

import org.hercules.RpcClient;
import org.hercules.RpcServer;
import org.hercules.impl.PingRequestProcessor;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;
import org.junit.Test;

/**
 * org.hercules.proto.RpcRequestsTest
 *
 * @author zhanghailin
 */
public class RpcRequestsTest {

    @Test
    public void testPingRequest()  throws InterruptedException {
        ProtobufMsgFactory.load();

        final RpcServer server = RpcFactoryHelper.rpcFactory().createRpcServer(new Endpoint("127.0.0.1", 19991));
        server.registerProcessor(new PingRequestProcessor());
        server.init(null);

        final Endpoint target = new Endpoint("127.0.0.1", 19991);

        final RpcClient client = RpcFactoryHelper.rpcFactory().createRpcClient();
        client.init(null);

        final RpcRequests.PingRequest req = RpcRequests.PingRequest.newBuilder() //
                .setSendTimestamp(System.currentTimeMillis()) //
                .build();

        for (int i = 0; i < 3; i++) {
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
