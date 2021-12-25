package org.hercules.impl.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.hercules.RpcResponseFactory;
import org.hercules.proto.RpcRequests;
import org.hercules.util.Requires;

/**
 * Helper to create error response for GRPC implementation.
 * org.hercules.impl.grpc.GrpcResponseFactory
 *
 * @author zhanghailin
 */
public class GrpcResponseFactory  implements RpcResponseFactory {

    @Override
    public Message newResponse(final Message parent, final int code, final String fmt, final Object... args) {
        final RpcRequests.ErrorResponse.Builder eBuilder = RpcRequests.ErrorResponse.newBuilder();
        eBuilder.setErrorCode(code);
        if (fmt != null) {
            eBuilder.setErrorMsg(String.format(fmt, args));
        }

        if (parent == null || parent instanceof RpcRequests.ErrorResponse) {
            return eBuilder.build();
        }

        final Descriptors.FieldDescriptor errFd = parent //
                .getDescriptorForType() //
                .findFieldByNumber(ERROR_RESPONSE_NUM);

        Requires.requireNonNull(errFd, "errFd");
        final Message.Builder builder = parent.toBuilder();
        for (final Descriptors.FieldDescriptor fd : parent.getDescriptorForType().getFields()) {
            builder.setField(fd, parent.getField(fd));
        }
        return builder //
                .setField(errFd, eBuilder.build()) //
                .build();
    }
}
