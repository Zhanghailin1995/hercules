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
package org.hercules.example.benchmark.grpc;

import org.hercules.RpcContext;
import org.hercules.RpcProcessor;
import org.hercules.util.RpcFactoryHelper;
import org.hercules.util.Utils;

/**
 *
 * @author jiachun.fjc
 */
public class GrpcBenchmarkProcessor implements RpcProcessor<Benchmark.BenchRequest> {

    @Override
    public void handleRequest(RpcContext rpcCtx, Benchmark.BenchRequest request) {
        Benchmark.BenchReply reply = Benchmark.BenchReply.newBuilder()
                .setData(request.getData())
                .build();
        rpcCtx.sendResponse(reply);
    }

    @Override
    public String interest() {
        if (RpcFactoryHelper.rpcFactory().factoryType().equals(RpcFactoryHelper.RpcFactoryType.GRPC)
                && Utils.isRpcProcessorInterestPreferProtoName()) {
            return Utils.generateGrpcServiceName(Benchmark.BenchRequest.getDescriptor().getFullName());
        }
        return Benchmark.BenchRequest.class.getName();
    }
}
