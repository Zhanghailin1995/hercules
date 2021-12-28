package org.hercules.example.benchmark;

import org.hercules.RpcClient;
import org.hercules.error.RemotingException;
import org.hercules.util.Endpoint;
import org.hercules.util.RpcFactoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * org.hercules.example.benchmark.GrpcBenchmarkClient
 *
 * @author zhanghailin
 */
public class BenchmarkClient {

    private static final byte[] BYTES = new byte[128];

    static {
        new Random().nextBytes(BYTES);
    }

    public static void main(String[] args) throws RemotingException, InterruptedException {
        System.setProperty("hercules.bolt.channel_write_buf_low_water_mark", String.valueOf(32 * 1024 * 1024));
        System.setProperty("hercules.bolt.channel_write_buf_high_water_mark", String.valueOf(64 * 1024 * 1024));
        System.setProperty("bolt.netty.flush_consolidation", "true");
        final Endpoint target = new Endpoint("127.0.0.1", 19981);

        final RpcClient client = RpcFactoryHelper.rpcFactory().createRpcClient();
        client.init(null);
        int processors = Runtime.getRuntime().availableProcessors();
        callWithFuture(client, target, processors << 1);
    }

    private static void callWithFuture(final RpcClient rpcClient, final Endpoint address, int threads) {
        // warmup
        for (int i = 0; i < 10000; i++) {
            try {
                rpcClient.invokeWithFuture(address, new Request<byte[]>(BYTES), 5000);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        final int t = 80000;
        long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(threads);
        final AtomicLong count = new AtomicLong();
        final int futureSize = 80;

        for (int i = 0; i < threads; i++) {
            new Thread(new Runnable() {
                List<CompletableFuture<Object>> futures = new ArrayList<>(futureSize);

                @Override
                public void run() {
                    for (int i = 0; i < t; i++) {
                        try {
                            CompletableFuture<Object> f = rpcClient.invokeWithFuture(address, new Request<byte[]>(BYTES), 5000);
                            futures.add(f);
                            if (futures.size() == futureSize) {
                                int fSize = futures.size();
                                for (int j = 0; j < fSize; j++) {
                                    try {
                                        futures.get(j).get();
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                }
                                futures.clear();
                            }
                            if (count.getAndIncrement() % 10000 == 0) {
                                System.out.println("count=" + count.get());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!futures.isEmpty()) {
                        int fSize = futures.size();
                        for (int j = 0; j < fSize; j++) {
                            try {
                                futures.get(j).get();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                        futures.clear();
                    }
                    latch.countDown();
                }
            }, "benchmark_" + i).start();
        }

        try {
            latch.await();
            System.out.println("count=" + count.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long second = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Request count: " + count.get() + ", time: " + second + " second, qps: "
                + count.get() / second);
    }


}
