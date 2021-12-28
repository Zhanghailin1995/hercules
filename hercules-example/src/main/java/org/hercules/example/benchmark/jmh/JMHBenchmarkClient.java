package org.hercules.example.benchmark.jmh;

import org.hercules.RpcClient;
import org.hercules.error.RemotingException;
import org.hercules.example.benchmark.Request;
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
 * org.hercules.example.benchmark.jmh.JMHBenchmarkClient
 *
 * @author zhanghailin
 */
@State(Scope.Benchmark)
public class JMHBenchmarkClient {

//    # Run complete. Total time: 00:06:25
//
//    Benchmark                                       Mode     Cnt  Score   Error   Units
//    JMHBenchmarkClient.send1024                    thrpt       3  7.325 ± 1.321  ops/ms
//    JMHBenchmarkClient.send128                     thrpt       3  7.614 ± 0.323  ops/ms
//    JMHBenchmarkClient.send1024                     avgt       3  1.087 ± 0.043   ms/op
//    JMHBenchmarkClient.send128                      avgt       3  1.057 ± 0.011   ms/op
//    JMHBenchmarkClient.send1024                   sample  215802  1.112 ± 0.001   ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.00    sample          0.628           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.50    sample          1.083           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.90    sample          1.202           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.95    sample          1.257           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.99    sample          1.587           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.999   sample          2.393           ms/op
//    JMHBenchmarkClient.send1024:send1024·p0.9999  sample          3.379           ms/op
//    JMHBenchmarkClient.send1024:send1024·p1.00    sample          9.519           ms/op
//    JMHBenchmarkClient.send128                    sample  228314  1.051 ± 0.001   ms/op
//    JMHBenchmarkClient.send128:send128·p0.00      sample          0.578           ms/op
//    JMHBenchmarkClient.send128:send128·p0.50      sample          1.028           ms/op
//    JMHBenchmarkClient.send128:send128·p0.90      sample          1.128           ms/op
//    JMHBenchmarkClient.send128:send128·p0.95      sample          1.176           ms/op
//    JMHBenchmarkClient.send128:send128·p0.99      sample          1.348           ms/op
//    JMHBenchmarkClient.send128:send128·p0.999     sample          2.298           ms/op
//    JMHBenchmarkClient.send128:send128·p0.9999    sample          3.297           ms/op
//    JMHBenchmarkClient.send128:send128·p1.00      sample          4.116           ms/op
//    JMHBenchmarkClient.send1024                       ss       3  2.611 ± 2.656   ms/op
//    JMHBenchmarkClient.send128                        ss       3  2.312 ± 4.468   ms/op


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
        System.setProperty("hercules.bolt.channel_write_buf_low_water_mark", String.valueOf(32 * 1024 * 1024));
        System.setProperty("hercules.bolt.channel_write_buf_high_water_mark", String.valueOf(64 * 1024 * 1024));
        System.setProperty("bolt.netty.flush_consolidation", "true");

        endpoint = new Endpoint("127.0.0.1", 19981);

        client = RpcFactoryHelper.rpcFactory().createRpcClient();
        client.init(null);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void send128() throws RemotingException, InterruptedException {
        client.invokeSync(endpoint, new Request<byte[]>(BYTES_128), 5000);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void send1024() throws RemotingException, InterruptedException {
        client.invokeSync(endpoint, new Request<byte[]>(BYTES_1024), 5000);
    }

    public static void main(String[] args) throws RunnerException {
        System.out.println("run??????");
        Options opt = new OptionsBuilder()//
                .include(JMHBenchmarkClient.class.getSimpleName())//
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
