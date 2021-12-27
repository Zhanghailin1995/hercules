package org.hercules.impl.grpc;

import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.SystemPropertyUtil;
import io.grpc.util.MutableHandlerRegistry;
import org.hercules.RpcClient;
import org.hercules.RpcFactory;
import org.hercules.RpcResponseFactory;
import org.hercules.RpcServer;
import org.hercules.proto.RpcRequests;
import org.hercules.util.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * org.hercules.impl.grpc.GrpcRpcFactory
 *
 * @author zhanghailin
 */
@SPI(priority = 1)
public class GrpcRpcFactory implements RpcFactory {


    static final String FIXED_METHOD_NAME = "_call";

    static final int RPC_SERVER_PROCESSOR_POOL_SIZE = SystemPropertyUtil
            .getInt(
                    "hercules.grpc.default_rpc_server_processor_pool_size",
                    100);

    static final int RPC_MAX_INBOUND_MESSAGE_SIZE = SystemPropertyUtil.getInt(
            "hercules.grpc.max_inbound_message_size.bytes",
            4 * 1024 * 1024);

    private static final RpcResponseFactory RESPONSE_FACTORY = new GrpcResponseFactory();

    private static final Map<String, Message> parserClasses = new ConcurrentHashMap<>();

    private static final MarshallerRegistry defaultMarshallerRegistry = new MarshallerRegistry() {
        @Override
        public Message findResponseInstanceByRequest(final String reqCls) {
            return MarshallerHelper
                    .findRespInstance(reqCls);
        }

        @Override
        public void registerResponseInstance(final String reqCls,
                                             final Message respIns) {
            MarshallerHelper.registerRespInstance(
                    reqCls, respIns);
        }
    };

    static {
        if (Utils.isRpcProcessorInterestPreferProtoName()) {
            parserClasses.put(Utils.generateGrpcServiceName(RpcRequests.PingRequest.getDescriptor().getFullName()), RpcRequests.PingRequest.getDefaultInstance());
            defaultMarshallerRegistry.registerResponseInstance(Utils.generateGrpcServiceName(RpcRequests.PingRequest.getDescriptor().getFullName()),
                    RpcRequests.ErrorResponse.getDefaultInstance());
        } else {
            parserClasses.put(RpcRequests.PingRequest.class.getName(), RpcRequests.PingRequest.getDefaultInstance());
            defaultMarshallerRegistry.registerResponseInstance(RpcRequests.PingRequest.class.getName(),
                    RpcRequests.ErrorResponse.getDefaultInstance());
        }
    }

    @Override
    public RpcFactoryHelper.RpcFactoryType factoryType() {
        return RpcFactoryHelper.RpcFactoryType.GRPC;
    }

    @Override
    public void registerProtobufSerializer(final String className, final Object... args) {
        parserClasses.put(className, (Message) args[0]);
        if (args.length == 2) {
            defaultMarshallerRegistry.registerResponseInstance(className, (Message) args[1]);
        }
    }

    @Override
    public RpcClient createRpcClient(final ConfigHelper<RpcClient> helper) {
        final RpcClient rpcClient = new GrpcClient(parserClasses, getMarshallerRegistry());
        if (helper != null) {
            helper.config(rpcClient);
        }
        return rpcClient;
    }

    @Override
    public RpcServer createRpcServer(Endpoint endpoint, ConfigHelper<RpcServer> helper) {
        final int port = Requires.requireNonNull(endpoint, "endpoint").getPort();
        Requires.requireTrue(port > 0 && port < 0xFFFF, "port out of range:" + port);
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
        final Server server = ServerBuilder.forPort(port) //
                .fallbackHandlerRegistry(handlerRegistry) //
                .directExecutor() //
                .maxInboundMessageSize(RPC_MAX_INBOUND_MESSAGE_SIZE) //
                .build();
        final RpcServer rpcServer = new GrpcServer(server, handlerRegistry, parserClasses, getMarshallerRegistry());
        if (helper != null) {
            helper.config(rpcServer);
        }
        return rpcServer;
    }

    @Override
    public RpcResponseFactory getRpcResponseFactory() {
        return RESPONSE_FACTORY;
    }

    public MarshallerRegistry getMarshallerRegistry() {
        return defaultMarshallerRegistry;
    }

}
