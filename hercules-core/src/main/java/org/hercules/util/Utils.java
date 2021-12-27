package org.hercules.util;

import com.google.protobuf.Message;
import io.grpc.netty.shaded.io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * org.hercules.util.Utils
 *
 * @author zhanghailin
 */
public final class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    /**
     * The configured number of available processors. The default is
     * {@link Runtime#availableProcessors()}. This can be overridden by setting the system property
     * "hercules.available_processors".
     */
    private static final int CPUS = SystemPropertyUtil.getInt(
            "hercules.available_processors", Runtime.getRuntime().availableProcessors());

    private static final boolean RPC_PROCESSOR_INTEREST_PREFER_PROTO_NAME = SystemPropertyUtil.getBoolean(
            "hercules.rpc.rpc_processor_interest_prefer_proto_name", false);


    /**
     * ANY IP address 0.0.0.0
     */
    public static final String IP_ANY = "0.0.0.0";

    /**
     * Get system CPUs count.
     */
    public static int cpus() {
        return CPUS;
    }

    public static boolean isRpcProcessorInterestPreferProtoName() {
        LOG.info("hercules.rpc.rpc_processor_interest_prefer_proto_name: {}", RPC_PROCESSOR_INTEREST_PREFER_PROTO_NAME);
        return RPC_PROCESSOR_INTEREST_PREFER_PROTO_NAME;
    }


    public static String generateGrpcServiceName(Message req) {
        // return "/"+req.getDescriptorForType().getFullName()+"Service";
        return req.getDescriptorForType().getFullName()+"Service";
    }

    public static String generateGrpcServiceName(String descriptorFullName) {
        // return "/"+descriptorFullName+"Service";
        return descriptorFullName+"Service";
    }
}
