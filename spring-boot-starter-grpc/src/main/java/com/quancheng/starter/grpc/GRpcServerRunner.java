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
import org.springframework.core.type.StandardMethodMetadata;

import com.google.common.collect.Lists;
import com.quancheng.starter.grpc.autoconfigure.GRpcServerProperties;
import com.quancheng.starter.grpc.registry.Registry;
import com.quancheng.starter.grpc.registry.RegistryFactory;
import com.quancheng.starter.grpc.registry.URL;
import com.quancheng.starter.grpc.registry.URLParamType;
import com.quancheng.starter.grpc.registry.util.NetUtils;
import com.quancheng.starter.grpc.trace.TraceServerInterceptor;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

    private static final Logger        log = LoggerFactory.getLogger(GRpcServerRunner.class);

    @Autowired
    private GRpcServerProperties       gRpcServerProperties;

    @Autowired
    private AbstractApplicationContext applicationContext;

    private Server                     server;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        Collection<ServerInterceptor> globalInterceptors = getTypedBeansWithAnnotation(GRpcGlobalInterceptor.class,
                                                                                       ServerInterceptor.class);
        final int port = gRpcServerProperties.getServerPort();
        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
        // find and register all GRpcService-enabled beans
        for (BindableService bindableService : getTypedBeansWithAnnotation(GRpcService.class, BindableService.class)) {
            ServerServiceDefinition serviceDefinition = bindableService.bindService();
            GRpcService gRpcServiceAnn = bindableService.getClass().getAnnotation(GRpcService.class);
            serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
            serverBuilder.addService(serviceDefinition);
            String superServiceName = bindableService.getClass().getGenericSuperclass().getTypeName();
            String serviceName = StringUtils.split(superServiceName, "$")[0];
            doRegistryService(serviceName);
            log.info("'{}' service has been registered.", bindableService.getClass().getName());

        }
        server = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getServerPort());
        startDaemonAwaitThread();
    }

    private void doRegistryService(String serviceName) {
        URL consulURL = new URL("consul", gRpcServerProperties.getConsulIp(), gRpcServerProperties.getConsulPort(), "");
        Registry consulRegistry = RegistryFactory.getRegistry(consulURL);
        URL serviceURL = buildServiceUrl(serviceName, gRpcServerProperties.getServerPort());
        consulRegistry.register(serviceURL);
        consulRegistry.available(serviceURL);
    }

    private URL buildServiceUrl(String serviceName, int port) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(URLParamType.group.getName(), GrpcConstants.DEFAULT_GROUP);
        params.put(URLParamType.protocol.getName(), GrpcConstants.DEFAULT_PROTOCOL);
        URL url = new URL(GrpcConstants.DEFAULT_PROTOCOL, NetUtils.getLocalAddress().getHostAddress(), port,
                          serviceName, params);
        return url;
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
        List<ServerInterceptor> interceptors = Lists.newArrayList();
        interceptors.add(new TraceServerInterceptor());
        List<ServerInterceptor> userInterceptors = Stream.concat(gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                                                                 privateInterceptors).distinct().collect(Collectors.toList());
        interceptors.addAll(userInterceptors);
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread() {

            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.server.awaitTermination();
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
        Optional.ofNullable(server).ifPresent(Server::shutdown);
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
