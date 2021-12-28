package org.hercules.example.benchmark.grpc;

import org.hercules.RpcFactory;
import org.hercules.RpcServer;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.example.benchmark.grpc.GrpcBenchmarkServer
 *
 * @author zhanghailin
 */
public class GrpcBenchmarkServer {

    public static void main(String[] args) throws InterruptedException {

        // Bolt also support protobuf serialize. but we need to register
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();

        GrpcBenchmarkProcessor processor = new GrpcBenchmarkProcessor();

        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)) {
            rpcFactory.registerProtobufSerializer(
                    processor.interest(),
                    Benchmark.BenchRequest.getDefaultInstance(),
                    Benchmark.BenchReply.getDefaultInstance());
        } else {
            throw new UnsupportedOperationException("not support bolt");
        }

        final RpcServer server = rpcFactory.createRpcServer(new Endpoint("127.0.0.1", 19991));
        server.registerProcessor(processor);
        server.init(null);

        server.awaitTermination();
    }
}
