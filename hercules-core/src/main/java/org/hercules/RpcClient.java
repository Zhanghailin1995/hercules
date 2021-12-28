package org.hercules;

import org.hercules.error.RemotingException;
import org.hercules.option.RpcOptions;
import org.hercules.util.Endpoint;

import java.util.concurrent.CompletableFuture;

/**
 * org.hercules.RpcClient
 *
 * @author zhanghailin
 */
public interface RpcClient extends Lifecycle<RpcOptions> {

    /**
     * Check connection for given address.
     *
     * @param endpoint target address
     * @return true if there is a connection and the connection is active and writable.
     */
    boolean checkConnection(final Endpoint endpoint);

    /**
     * Check connection for given address and async to create a new one if there is no connection.
     *
     * @param endpoint       target address
     * @param createIfAbsent create a new one if there is no connection
     * @return true if there is a connection and the connection is active and writable.
     */
    boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent);

    /**
     * Close all connections of a address.
     *
     * @param endpoint target address
     */
    void closeConnection(final Endpoint endpoint);

    /**
     * Synchronous invocation.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param timeoutMs timeout millisecond
     * @return invoke result
     */
    default Object invokeSync(final Endpoint endpoint, final Object request, final long timeoutMs)
            throws InterruptedException, RemotingException {
        return invokeSync(endpoint, request, null, timeoutMs);
    }

    /**
     * Synchronous invocation using a invoke context.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param ctx       invoke context
     * @param timeoutMs timeout millisecond
     * @return invoke result
     */
    Object invokeSync(final Endpoint endpoint, final Object request, final InvokeContext ctx,
                      final long timeoutMs) throws InterruptedException, RemotingException;

    /**
     * invoke with a future.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param ctx       invoke context
     * @param timeoutMs timeout millisecond
     * @return invoke result
     */
    CompletableFuture<Object> invokeWithFuture(final Endpoint endpoint, final Object request,
                                               final InvokeContext ctx, final long timeoutMs) throws InterruptedException, RemotingException;


    /**
     * Asynchronous invocation with a callback.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param callback  invoke callback
     * @param timeoutMs timeout millisecond
     */
    default void invokeAsync(final Endpoint endpoint, final Object request, final InvokeCallback callback,
                             final long timeoutMs) throws InterruptedException, RemotingException {
        invokeAsync(endpoint, request, null, callback, timeoutMs);
    }

    /**
     * Asynchronous invocation with a callback.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param ctx       invoke context
     * @param callback  invoke callback
     * @param timeoutMs timeout millisecond
     */
    void invokeAsync(final Endpoint endpoint, final Object request, final InvokeContext ctx, final InvokeCallback callback,
                     final long timeoutMs) throws InterruptedException, RemotingException;

}
