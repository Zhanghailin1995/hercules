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
package org.hercules.impl;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;
import org.hercules.proto.RpcRequests;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 * Ping request processor.
 *
 * @author boyan (boyan@alibaba-inc.com)
 * @author jiachun.fjc
 */
public class PingRequestProcessor implements RpcProcessor<RpcRequests.PingRequest> {

    @Override
    public void handleRequest(final RpcContext rpcCtx, final RpcRequests.PingRequest request) {
        rpcCtx.sendResponse( //
                RpcFactoryHelper //
                        .responseFactory() //
                        .newResponse(RpcRequests.ErrorResponse.getDefaultInstance(), 0, "OK"));
    }

    @Override
    public String interest() {
        if (RpcFactoryHelper.rpcFactory().factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            return RpcRequests.PingRequest.getDescriptor().getFullName();
        }
        return RpcRequests.PingRequest.class.getName();
    }
}
