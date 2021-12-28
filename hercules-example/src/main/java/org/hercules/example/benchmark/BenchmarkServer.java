package org.hercules.example.benchmark;

import org.hercules.RpcServer;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

/**
 * org.hercules.example.benchmark.BenchmarkServer
 *
 * @author zhanghailin
 */
public class BenchmarkServer {

    public static void main(String[] args) {
        System.setProperty("hercules.bolt.channel_write_buf_low_water_mark", String.valueOf(32 * 1024 * 1024));
        System.setProperty("hercules.bolt.channel_write_buf_high_water_mark", String.valueOf(64 * 1024 * 1024));
        System.setProperty("bolt.netty.flush_consolidation", "true");

        final RpcServer server = RpcFactoryHelper.rpcFactory().createRpcServer(new Endpoint("127.0.0.1", 19991));
        server.registerProcessor(new BenchmarkUserProcessor());
        server.init(null);
    }
}
