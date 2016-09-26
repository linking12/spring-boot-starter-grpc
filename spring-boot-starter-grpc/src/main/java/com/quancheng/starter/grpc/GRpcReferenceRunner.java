package com.quancheng.starter.grpc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.quancheng.starter.grpc.autoconfigure.GRpcServerProperties;
import com.quancheng.starter.grpc.internal.ConsulNameResolver;
import com.quancheng.starter.grpc.internal.GRpcHeaderClientInterceptor;
import com.quancheng.starter.grpc.metrics.MetricsConfiguration;
import com.quancheng.starter.grpc.metrics.MonitoringClientInterceptor;
import com.quancheng.starter.grpc.trace.GrpcTracer;

import io.grpc.Attributes;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import io.opentracing.contrib.grpc.ClientTracingInterceptor;

public class GRpcReferenceRunner extends InstantiationAwareBeanPostProcessorAdapter {

    private final GRpcServerProperties gRpcServerProperties;

    private final GrpcTracer           grpcTracer;

    private final MetricsConfiguration metricsConfiguration;

    public GRpcReferenceRunner(GRpcServerProperties gRpcServerProperties, MetricsConfiguration metricsConfiguration){
        this.gRpcServerProperties = gRpcServerProperties;
        this.grpcTracer = new GrpcTracer();
        this.metricsConfiguration = metricsConfiguration;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> searchType = bean.getClass();
        while (!Object.class.equals(searchType) && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                GRpcReference reference = field.getAnnotation(GRpcReference.class);
                if (reference != null) {
                    Channel channel = this.generateChannel(reference);
                    String clzzName = field.getType().getName();
                    // 没有破坏pb的生成规则
                    if (clzzName.contains("$")) {
                        try {
                            String parentName = StringUtils.substringBefore(clzzName, "$");
                            Class clzz = Class.forName(parentName);
                            Method method;
                            switch (reference.callType()) {
                                case "future":
                                    method = clzz.getMethod("newFutureStub", io.grpc.Channel.class);
                                    break;
                                case "blocking":
                                    method = clzz.getMethod("newBlockingStub", io.grpc.Channel.class);
                                    break;
                                case "async":
                                    method = clzz.getMethod("newBlockingStub", io.grpc.Channel.class);
                                    break;
                                default:
                                    method = clzz.getMethod("newFutureStub", io.grpc.Channel.class);
                                    break;
                            }
                            Object value = method.invoke(null, channel);
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(bean, value);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("stub definition not correct，do not edit proto generat file",
                                                               e);
                        }
                    } else {
                        throw new IllegalArgumentException("stub definition not correct，do not edit proto generat file");
                    }

                }
            }
            searchType = searchType.getSuperclass();
        }
        return bean;
    }

    private Channel generateChannel(GRpcReference reference) {
        String group = StringUtils.isNotBlank(reference.group()) ? reference.group() : gRpcServerProperties.getReferenceGroup();
        String version = StringUtils.isNotBlank(reference.version()) ? reference.version() : gRpcServerProperties.getReferenceVersion();
        String serviceName = reference.interfaceName();
        if (StringUtils.isBlank(serviceName) || StringUtils.isBlank(group) || StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("interfaceName or group or version is null");
        }
        String consulUrl = "consul:///" + gRpcServerProperties.getConsulIp() + ":"
                           + gRpcServerProperties.getConsulPort();
        ManagedChannel channel = ManagedChannelBuilder.forTarget(consulUrl)//
                                                      .nameResolverFactory(buildNameResolverFactory(serviceName, group,
                                                                                                    version))//
                                                      .loadBalancerFactory(buildLoadBalanceFactory()).usePlaintext(true).build();//
        Channel channelWrap = ClientInterceptors.intercept(channel, buildStarterInterceptor());
        return channelWrap;
    }

    private List<ClientInterceptor> buildStarterInterceptor() {
        List<ClientInterceptor> interceptors = Lists.newArrayList();
        interceptors.add(new ClientTracingInterceptor(this.grpcTracer));
        interceptors.add(new GRpcHeaderClientInterceptor());
        interceptors.add(MonitoringClientInterceptor.create(metricsConfiguration));
        return interceptors;
    }

    private NameResolver.Factory buildNameResolverFactory(String serviceName, String group, String versoin) {
        final Attributes attributesParams = Attributes.newBuilder()//
                                                      .set(ConsulNameResolver.PARAMS_DEFAULT_SERVICESNAME, //
                                                           serviceName)//
                                                      .set(ConsulNameResolver.PARAMS_DEFAULT_GROUP, //
                                                           group) //
                                                      .set(ConsulNameResolver.PARAMS_DEFAULT_VERSION, //
                                                           versoin).build();

        return new NameResolver.Factory() {

            @Override
            public NameResolver newNameResolver(URI targetUri, Attributes params) {
                String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
                Preconditions.checkArgument(targetPath.startsWith("/"),
                                            "the path component (%s) of the target (%s) must start with '/'",
                                            targetPath, targetUri);
                String name = targetPath.substring(1);
                Attributes allParams = Attributes.newBuilder().setAll(attributesParams).setAll(params).build();
                return new ConsulNameResolver(targetUri.getAuthority(), name, allParams);
            }

            @Override
            public String getDefaultScheme() {
                return "consul";
            }

        };
    }

    private LoadBalancer.Factory buildLoadBalanceFactory() {
        return RoundRobinLoadBalancerFactory.getInstance();
    }
}
