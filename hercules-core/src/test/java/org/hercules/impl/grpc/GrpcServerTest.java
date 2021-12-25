package org.hercules.impl.grpc;

import org.hercules.RpcServer;
import org.hercules.impl.PingRequestProcessor;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.impl.grpc.GrpcServerTest
 *
 * @author zhanghailin
 */
public class GrpcServerTest {

    public static void main(String[] args) throws InterruptedException {
        final RpcServer server = RpcFactoryHelper.rpcFactory().createRpcServer(new Endpoint("127.0.0.1", 19991));
        server.registerProcessor(new PingRequestProcessor());
        server.init(null);
        server.awaitTermination();
    }
}
