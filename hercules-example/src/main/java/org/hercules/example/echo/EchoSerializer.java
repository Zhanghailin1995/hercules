package org.hercules.example.echo;

import com.alipay.remoting.CustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.google.protobuf.Message;
import org.hercules.error.MessageClassNotFoundException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;

/**
 * org.hercules.example.echo.EchoSerializer
 *
 * @author zhanghailin
 */
public class EchoSerializer implements CustomSerializer {

    public static final EchoSerializer INSTANCE = new EchoSerializer();

    private static Map<String/* class name in java file */, MethodHandle> PARSE_METHODS_4J            = new HashMap<>();

    static {
        try {
            PARSE_METHODS_4J.put(Echo.EchoRequest.class.getName(), getParseFromMethodHandle(Echo.EchoRequest.class));
            PARSE_METHODS_4J.put(Echo.EchoResponse.class.getName(), getParseFromMethodHandle(Echo.EchoResponse.class));
        } catch (final Throwable t) {
            t.printStackTrace(); // NOPMD
        }

    }

    private static MethodHandle getParseFromMethodHandle(Class<?> clazz) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandle parseFromHandler = MethodHandles.lookup().findStatic(clazz, "parseFrom",
                methodType(clazz, byte[].class));
        return parseFromHandler;
    }


    @Override
    public <T extends RequestCommand> boolean serializeHeader(T request, InvokeContext invokeContext) throws SerializationException {
        return false;
    }

    @Override
    public <T extends ResponseCommand> boolean serializeHeader(T response) throws SerializationException {
        return false;
    }

    @Override
    public <T extends RequestCommand> boolean deserializeHeader(T request) throws DeserializationException {
        return false;
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeHeader(T response, InvokeContext invokeContext) throws DeserializationException {
        return false;
    }

    @Override
    public <T extends RequestCommand> boolean serializeContent(T request, InvokeContext invokeContext) throws SerializationException {
        final RpcRequestCommand cmd = (RpcRequestCommand) request;
        final Message msg = (Message) cmd.getRequestObject();
        cmd.setContent(msg.toByteArray());
        return true;
    }

    @Override
    public <T extends ResponseCommand> boolean serializeContent(T response) throws SerializationException {
        final RpcResponseCommand cmd = (RpcResponseCommand) response;
        final Message msg = (Message) cmd.getResponseObject();
        cmd.setContent(msg.toByteArray());
        return true;
    }

    @Override
    public <T extends RequestCommand> boolean deserializeContent(T request) throws DeserializationException {
        final RpcRequestCommand cmd = (RpcRequestCommand) request;
        final String className = cmd.getRequestClass();
        cmd.setRequestObject(newMessageByJavaClassName(className, request.getContent()));
        return true;
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeContent(T response, InvokeContext invokeContext) throws DeserializationException {
        final RpcResponseCommand cmd = (RpcResponseCommand) response;
        final String className = cmd.getResponseClass();
        cmd.setResponseObject(newMessageByJavaClassName(className, response.getContent()));
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Message> T newMessageByJavaClassName(final String className, final byte[] bs) {
        final MethodHandle handle = PARSE_METHODS_4J.get(className);
        if (handle == null) {
            throw new MessageClassNotFoundException(className + " not found");
        }
        try {
            return (T) handle.invoke(bs);
        } catch (Throwable t) {
            throw new org.hercules.error.SerializationException(t);
        }
    }

}
