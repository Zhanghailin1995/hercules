package org.hercules.impl;

import org.hercules.proto.ProtobufMsgFactory;
import org.hercules.proto.RpcRequests;
import org.hercules.test.MockAsyncContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * org.hercules.impl.PingRequestProcessorTest
 *
 * @author zhanghailin
 */
public class PingRequestProcessorTest {

    @Test
    public void testHandlePing() throws Exception {
        ProtobufMsgFactory.load();
        PingRequestProcessor processor = new PingRequestProcessor();
        MockAsyncContext ctx = new MockAsyncContext();
        processor.handleRequest(ctx, createPingRequest());
        RpcRequests.ErrorResponse response = (RpcRequests.ErrorResponse) ctx.getResponseObject();
        assertEquals(0, response.getErrorCode());
    }

    public static RpcRequests.PingRequest createPingRequest() {
        RpcRequests.PingRequest reqObject = RpcRequests.PingRequest.newBuilder()
                .setSendTimestamp(System.currentTimeMillis()).build();
        return reqObject;
    }
}
