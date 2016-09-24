package com.quancheng.starter.grpc.metrics;

import java.time.Clock;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public class MonitoringClientInterceptor implements ClientInterceptor {

    private final Clock                 clock;
    private final MetricsConfiguration         configuration;
    private final ClientMetrics.Factory clientMetricsFactory;

    public static MonitoringClientInterceptor create(MetricsConfiguration configuration) {
        return new MonitoringClientInterceptor(Clock.systemDefaultZone(), configuration,
                                               new ClientMetrics.Factory(configuration));
    }

    private MonitoringClientInterceptor(Clock clock, MetricsConfiguration configuration,
                                        ClientMetrics.Factory clientMetricsFactory){
        this.clock = clock;
        this.configuration = configuration;
        this.clientMetricsFactory = clientMetricsFactory;
    }

    @Override
    public <R, S> ClientCall<R, S> interceptCall(MethodDescriptor<R, S> methodDescriptor, CallOptions callOptions,
                                                 Channel channel) {
        ClientMetrics metrics = clientMetricsFactory.createMetricsForMethod(methodDescriptor);
        return new MonitoringClientCall<>(channel.newCall(methodDescriptor, callOptions), metrics,
                                          GrpcMethod.of(methodDescriptor), configuration, clock);
    }
}
