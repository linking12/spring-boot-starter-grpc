package com.quancheng.starter.grpc.autoconfigure;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quancheng.starter.grpc.GRpcReferenceRunner;
import com.quancheng.starter.grpc.GRpcServerRunner;
import com.quancheng.starter.grpc.GRpcService;
import com.quancheng.starter.grpc.metrics.MetricsConfiguration;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

@Configuration
@ConditionalOnProperty(prefix = "grpc", name = "consulIp")
@EnableConfigurationProperties(GRpcServerProperties.class)
public class GRpcAutoConfiguration {

    private final GRpcServerProperties grpcProperty;

    public GRpcAutoConfiguration(GRpcServerProperties grpcProperty){
        this.grpcProperty = grpcProperty;
    }

    @Bean
    @ConditionalOnBean(value = GRpcServerProperties.class, annotation = GRpcService.class)
    public GRpcServerRunner grpcServerRunner(MetricsConfiguration metricsConfiguration) {
        return new GRpcServerRunner(metricsConfiguration);
    }

    @Bean
    public BeanPostProcessor grpcReferenceRunner(MetricsConfiguration metricsConfiguration) {
        return new GRpcReferenceRunner(grpcProperty, metricsConfiguration);
    }

    @Bean
    public MetricsConfiguration metricsConfiguration() {
        return MetricsConfiguration.cheapMetricsOnly();
    }

    @Bean
    public CollectorRegistry collectorRegistry(MetricsConfiguration metricsConfiguration) {
        return metricsConfiguration.getCollectorRegistry();
    }

    // @Bean
    // public SpringBootMetricsCollector metricsCollector(final Collection<PublicMetrics> metrics,
    // final CollectorRegistry registry) {
    // return new SpringBootMetricsCollector(metrics).register(registry);
    // }

    @Bean
    public ServletRegistrationBean exporterServlet(CollectorRegistry registry) {
        return new ServletRegistrationBean(new MetricsServlet(registry), "/prometheus");
    }

}
