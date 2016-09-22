package com.quancheng.starter.grpc.trace;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.quancheng.starter.grpc.GrpcConstants;
import com.quancheng.starter.grpc.RpcContext;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class TraceClientInterceptor implements ClientInterceptor {

    private final GenerateTraceId generateTraceId;

    public TraceClientInterceptor(){
        generateTraceId = GenerateTraceId.getInstance();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String traceId = RpcContext.getContext().getAttachment("grpc_header_trace_key");
                if (StringUtils.isBlank(traceId)) {
                    headers.put(GrpcConstants.GRPC_TRACE_KEY, generateTraceId.getTraceId().toString());
                } else {
                    RpcContext.getContext().removeAttachment("grpc_header_trace_key");
                    headers.put(GrpcConstants.GRPC_TRACE_KEY, traceId);
                }
                copyThreadLocalToMetadata(headers);
                super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {

                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }

    private void copyThreadLocalToMetadata(Metadata headers) {
        Map<String, String> attachments = RpcContext.getContext().getAttachments();
        Map<String, Object> values = RpcContext.getContext().get();
        try {
            if (!attachments.isEmpty()) {
                byte[] attachmentsBytes = new Gson().toJson(attachments).getBytes();
                headers.put(GrpcConstants.GRPC_CONTEXT_ATTACHMENTS, attachmentsBytes);
            }
            if (!values.isEmpty()) {
                byte[] attachmentsValues = new Gson().toJson(values).getBytes();
                headers.put(GrpcConstants.GRPC_CONTEXT_VALUES, attachmentsValues);
            }
        } catch (Throwable e) {
        }

    }

}
