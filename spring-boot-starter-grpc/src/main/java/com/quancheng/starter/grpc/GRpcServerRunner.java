package com.quancheng.starter.grpc;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.StandardMethodMetadata;

import com.google.common.collect.Lists;
import com.quancheng.starter.grpc.autoconfigure.GRpcProperties;
import com.quancheng.starter.grpc.internal.GRpcHeaderServerInterceptor;
import com.quancheng.starter.grpc.metrics.MetricsConfiguration;
import com.quancheng.starter.grpc.metrics.MonitoringServerInterceptor;
import com.quancheng.starter.grpc.registry.Registry;
import com.quancheng.starter.grpc.registry.RegistryFactory;
import com.quancheng.starter.grpc.registry.URL;
import com.quancheng.starter.grpc.registry.URLParamType;
import com.quancheng.starter.grpc.registry.util.NetUtils;
import com.quancheng.starter.grpc.trace.GrpcTracer;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opentracing.contrib.grpc.ServerTracingInterceptor;

@Order(value = 0)
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

    private static final Logger        log = LoggerFactory.getLogger(GRpcServerRunner.class);

    @Autowired
    private GRpcProperties             grpcProperties;

    @Autowired
    private AbstractApplicationContext applicationContext;

    private Server                     remoteServer;

    private Server                     inprocessServer;

    private final GrpcTracer           grpcTracer;

    private final MetricsConfiguration metricsConfiguration;

    public GRpcServerRunner(MetricsConfiguration metricsConfiguration){
        this.grpcTracer = new GrpcTracer();
        this.metricsConfiguration = metricsConfiguration;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        Collection<ServerInterceptor> globalInterceptors = getTypedBeansWithAnnotation(GRpcGlobalInterceptor.class,
                                                                                       ServerInterceptor.class);
        final int port = grpcProperties.getServerPort();
        final ServerBuilder<?> remoteServerBuilder = ServerBuilder.forPort(port);
        final InProcessServerBuilder inprocessServerBuild = InProcessServerBuilder.forName(GrpcConstants.GRPC_IN_LOCAL_PROCESS);
        for (BindableService bindableService : getTypedBeansWithAnnotation(GRpcService.class, BindableService.class)) {
            ServerServiceDefinition serviceDefinition = bindableService.bindService();
            GRpcService gRpcServiceAnn = bindableService.getClass().getAnnotation(GRpcService.class);
            serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
            remoteServerBuilder.addService(serviceDefinition);
            inprocessServerBuild.addService(serviceDefinition);
            doRegistryService(gRpcServiceAnn);
            log.info("'{}' service has been registered.", bindableService.getClass().getName());

        }
        remoteServer = remoteServerBuilder.build().start();
        inprocessServer = inprocessServerBuild.build().start();
        log.info("gRPC Server started, listening on port {}.", grpcProperties.getServerPort());
        startDaemonAwaitThread();
    }

    private void doRegistryService(GRpcService gRpcService) {
        URL consulURL = new URL("consul", grpcProperties.getConsulIp(), grpcProperties.getConsulPort(), "");
        Registry consulRegistry = RegistryFactory.getRegistry(consulURL);
        Map<String, String> params = new HashMap<String, String>();
        String group = grpcProperties.getServiceGroup() != null ? grpcProperties.getServiceGroup() : gRpcService.group();
        String version = grpcProperties.getServcieVersion() != null ? grpcProperties.getServcieVersion() : gRpcService.version();
        String serviceName = gRpcService.interfaceName();
        if (StringUtils.isBlank(serviceName) || StringUtils.isBlank(group) || StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("interfaceName or group or version is null");
        }
        params.put(URLParamType.protocol.getName(), GrpcConstants.DEFAULT_PROTOCOL);
        params.put(URLParamType.group.getName(), group);
        params.put(URLParamType.version.getName(), version);
        URL serviceURL = new URL(GrpcConstants.DEFAULT_PROTOCOL, NetUtils.getLocalAddress().getHostAddress(),
                                 grpcProperties.getServerPort(), serviceName, params);
        consulRegistry.register(serviceURL);
        consulRegistry.available(serviceURL);
    }

    private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GRpcService gRpcService,
                                                     Collection<ServerInterceptor> globalInterceptors) {

        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors()).map(interceptorClass -> {
            try {
                return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ? applicationContext.getBean(interceptorClass) : interceptorClass.newInstance();
            } catch (Exception e) {
                throw new BeanCreationException("Failed to create interceptor instance.", e);
            }
        });

        List<ServerInterceptor> interceptors = this.buildStarterInterceptor();
        List<ServerInterceptor> userInterceptors = Stream.concat(gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                                                                 privateInterceptors).distinct().collect(Collectors.toList());
        interceptors.addAll(userInterceptors);
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private List<ServerInterceptor> buildStarterInterceptor() {
        List<ServerInterceptor> interceptors = Lists.newArrayList();
        interceptors.add(new ServerTracingInterceptor(this.grpcTracer));
        interceptors.add(new GRpcHeaderServerInterceptor());
        MonitoringServerInterceptor monitoringInterceptor = MonitoringServerInterceptor.create(metricsConfiguration);
        interceptors.add(monitoringInterceptor);
        return interceptors;
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread() {

            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.remoteServer.awaitTermination();
                    GRpcServerRunner.this.inprocessServer.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("gRPC server stopped.", e);
                }
            }

        };
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down gRPC server ...");
        Optional.ofNullable(remoteServer).ifPresent(Server::shutdown);
        Optional.ofNullable(inprocessServer).ifPresent(Server::shutdown);
        log.info("gRPC server stopped.");
    }

    private <T> Collection<T> getTypedBeansWithAnnotation(Class<? extends Annotation> annotationType,
                                                          Class<T> beanType) throws Exception {

        return Stream.of(applicationContext.getBeanNamesForType(beanType)).filter(name -> {
            BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
            if (beanDefinition.getSource() instanceof StandardMethodMetadata) {
                StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition.getSource();
                return metadata.isAnnotated(annotationType.getName());
            }
            return null != applicationContext.getBeanFactory().findAnnotationOnBean(name, annotationType);
        }).map(name -> applicationContext.getBeanFactory().getBean(name, beanType)).collect(Collectors.toList());

    }

}
