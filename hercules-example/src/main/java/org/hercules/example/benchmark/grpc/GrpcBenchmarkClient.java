package org.hercules.example.benchmark.grpc;

import com.google.protobuf.ZeroByteStringHelper;
import org.hercules.RpcClient;
import org.hercules.RpcFactory;
import org.hercules.error.RemotingException;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * org.hercules.example.benchmark.grpc.GrpcBenchmarkClient
 *
 * @author zhanghailin
 */
@State(Scope.Benchmark)
public class GrpcBenchmarkClient {

// 整体水平差不多，但是grpc似乎毛刺要大，grpc没有很多可以控制的参数
//    # Run complete. Total time: 00:06:20
//
//    Benchmark                                        Mode     Cnt    Score    Error   Units
//    GrpcBenchmarkClient.send1024                    thrpt       3    6.389 ± 13.800  ops/ms
//    GrpcBenchmarkClient.send128                     thrpt       3    6.852 ±  2.111  ops/ms
//    GrpcBenchmarkClient.send1024                     avgt       3    1.085 ±  0.256   ms/op
//    GrpcBenchmarkClient.send128                      avgt       3    1.075 ±  0.078   ms/op
//    GrpcBenchmarkClient.send1024                   sample  209604    1.145 ±  0.014   ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.00    sample            0.369            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.50    sample            1.059            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.90    sample            1.479            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.95    sample            1.647            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.99    sample            2.195            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.999   sample            7.455            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p0.9999  sample          165.151            ms/op
//    GrpcBenchmarkClient.send1024:send1024·p1.00    sample          180.879            ms/op
//    GrpcBenchmarkClient.send128                    sample  227710    1.054 ±  0.002   ms/op
//    GrpcBenchmarkClient.send128:send128·p0.00      sample            0.350            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.50      sample            1.020            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.90      sample            1.393            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.95      sample            1.532            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.99      sample            1.858            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.999     sample            2.775            ms/op
//    GrpcBenchmarkClient.send128:send128·p0.9999    sample            8.802            ms/op
//    GrpcBenchmarkClient.send128:send128·p1.00      sample           24.805            ms/op
//    GrpcBenchmarkClient.send1024                       ss       3    5.245 ± 11.132   ms/op
//    GrpcBenchmarkClient.send128                        ss       3    5.437 ± 12.330   ms/op

    public static final int     CONCURRENCY = 8;
    private static final byte[] BYTES_128   = new byte[128];
    private static final byte[] BYTES_1024  = new byte[1024];
    static {
        new Random().nextBytes(BYTES_128);
        new Random().nextBytes(BYTES_1024);
    }

    private RpcClient client;

    private Endpoint endpoint;

    @Setup
    public void setup() {
        endpoint = new Endpoint("127.0.0.1", 19991);
        // get rpc factory
        RpcFactory rpcFactory = RpcFactoryHelper.rpcFactory();
        GrpcBenchmarkProcessor processor = new GrpcBenchmarkProcessor();
        // bolt and grpc register serializer is different
        if (rpcFactory.factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)) {
            rpcFactory.registerProtobufSerializer(
                    processor.interest(),
                    Benchmark.BenchRequest.getDefaultInstance(),
                    Benchmark.BenchReply.getDefaultInstance());
        } else {
            throw new UnsupportedOperationException("not support bolt");
        }
        client = rpcFactory.createRpcClient();
        client.init(null);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void send128() throws RemotingException, InterruptedException {
        Benchmark.BenchRequest request = Benchmark.BenchRequest.newBuilder()
                .setData(ZeroByteStringHelper.wrap(BYTES_128))
                .build();
        client.invokeSync(endpoint, request, 5000);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void send1024() throws RemotingException, InterruptedException {
        Benchmark.BenchRequest request = Benchmark.BenchRequest.newBuilder()
                .setData(ZeroByteStringHelper.wrap(BYTES_1024))
                .build();
        client.invokeSync(endpoint, request, 5000);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(GrpcBenchmarkClient.class.getSimpleName())//
                .warmupIterations(3)//
                .warmupTime(TimeValue.seconds(10))//
                .measurementIterations(3)//
                .measurementTime(TimeValue.seconds(10))//
                .threads(CONCURRENCY)//
                .forks(1)//
                .build();
        new Runner(opt).run();
    }
}
