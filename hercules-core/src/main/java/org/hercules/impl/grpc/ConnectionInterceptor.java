/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hercules.impl.grpc;

import io.grpc.*;
import io.grpc.internal.ServerStream;
import io.grpc.internal.ServerStreamHelper;
import io.grpc.netty.shaded.io.grpc.netty.NettyConnectionHelper;
import org.hercules.Connection;
import org.hercules.ConnectionClosedEventListener;

import java.util.List;

/**
 * Intercepting incoming calls to get {@link Connection} and attach to current {@link Context}
 * before that are dispatched by {@link ServerCallHandler}.
 *
 * @author jiachun.fjc
 */
public class ConnectionInterceptor implements ServerInterceptor {

    static final Context.Key<ServerStream> STREAM = Context.key("current-stream");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                                 final Metadata headers,
                                                                 final ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        final ServerStream stream = ServerStreamHelper.getServerStream(call);
        if (stream != null) {
            ctx = ctx.withValue(STREAM, stream);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    public static Connection getCurrentConnection(final List<ConnectionClosedEventListener> listeners) {
        final ServerStream stream = ConnectionInterceptor.STREAM.get();
        if (stream != null) {
            return NettyConnectionHelper.getOrCreateConnection(stream, listeners);
        }
        return null;
    }
}
