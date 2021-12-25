package org.hercules.impl.bolt;

import com.alipay.remoting.CustomSerializer;
import com.alipay.remoting.CustomSerializerManager;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.rpc.RpcConfigManager;
import com.alipay.remoting.rpc.RpcConfigs;
import io.grpc.netty.shaded.io.netty.util.internal.SystemPropertyUtil;
import org.hercules.RpcClient;
import org.hercules.RpcFactory;
import org.hercules.RpcServer;
import org.hercules.option.RpcOptions;
import org.hercules.proto.ProtobufSerializer;
import org.hercules.proto.RpcRequests;
import org.hercules.util.Endpoint;
import org.hercules.util.Requires;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jiachun.fjc
 */
@SPI
public class BoltRpcFactory implements RpcFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BoltRpcFactory.class);

    static final int CHANNEL_WRITE_BUF_LOW_WATER_MARK = SystemPropertyUtil.getInt(
            "hercules.bolt.channel_write_buf_low_water_mark",
            256 * 1024);
    static final int CHANNEL_WRITE_BUF_HIGH_WATER_MARK = SystemPropertyUtil.getInt(
            "hercules.bolt.channel_write_buf_high_water_mark",
            512 * 1024);

    static {
        CustomSerializerManager.registerCustomSerializer(RpcRequests.PingRequest.class.getName(), ProtobufSerializer.INSTANCE);
        CustomSerializerManager.registerCustomSerializer(RpcRequests.ErrorResponse.class.getName(), ProtobufSerializer.INSTANCE);
    }

    @Override
    public RpcFactoryHelper.RpcFactoryType factoryType() {
        return RpcFactoryHelper.RpcFactoryType.BOLT;
    }

    @Override
    public void registerProtobufSerializer(final String className, final Object... args) {
        CustomSerializerManager.registerCustomSerializer(className, (CustomSerializer) args[0]);
    }

    @Override
    public RpcClient createRpcClient(ConfigHelper<RpcClient> helper) {
        final com.alipay.remoting.rpc.RpcClient boltImpl = new com.alipay.remoting.rpc.RpcClient();
        final RpcClient rpcClient = new BoltRpcClient(boltImpl);
        if (helper != null) {
            helper.config(rpcClient);
        }
        return rpcClient;
    }

    @Override
    public RpcServer createRpcServer(Endpoint endpoint, ConfigHelper<RpcServer> helper) {
        final int port = Requires.requireNonNull(endpoint, "endpoint").getPort();
        Requires.requireTrue(port > 0 && port < 0xFFFF, "port out of range:" + port);
        final com.alipay.remoting.rpc.RpcServer boltImpl = new com.alipay.remoting.rpc.RpcServer(port, true, false);
        final RpcServer rpcServer = new BoltRpcServer(boltImpl);
        if (helper != null) {
            helper.config(rpcServer);
        }
        return rpcServer;
    }

    @Override
    public ConfigHelper<RpcClient> defaultClientConfigHelper(final RpcOptions opts) {
        return ins -> {
            final BoltRpcClient client = (BoltRpcClient) ins;
            final InvokeContext ctx = new InvokeContext();
            ctx.put(InvokeContext.BOLT_CRC_SWITCH, opts.isEnableRpcChecksum());
            client.setDefaultInvokeCtx(ctx);
        };
    }

    @Override
    public void ensurePipeline() {
        // enable `bolt.rpc.dispatch-msg-list-in-default-executor` system property
        if (RpcConfigManager.dispatch_msg_list_in_default_executor()) {
            System.setProperty(RpcConfigs.DISPATCH_MSG_LIST_IN_DEFAULT_EXECUTOR, "false");
            LOG.warn("JRaft SET {} to be false for replicator pipeline optimistic.",
                    RpcConfigs.DISPATCH_MSG_LIST_IN_DEFAULT_EXECUTOR);
        }
    }
}
