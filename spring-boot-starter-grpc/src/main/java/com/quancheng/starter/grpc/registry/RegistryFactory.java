package com.quancheng.starter.grpc.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import com.quancheng.starter.grpc.registry.consul.ConsulConstants;
import com.quancheng.starter.grpc.registry.consul.ConsulRegistry;
import com.quancheng.starter.grpc.registry.consul.client.GrpcConsulClient;
import com.quancheng.starter.grpc.registry.consul.client.GrpcConsulEcwidClient;

public class RegistryFactory {

    private static ConcurrentHashMap<String, Registry> registries = new ConcurrentHashMap<String, Registry>();

    private static final ReentrantLock                 lock       = new ReentrantLock();

    public static Registry getRegistry(URL url) {
        String registryUri = url.getUri();
        try {
            lock.lock();
            Registry registry = registries.get(registryUri);
            if (registry != null) {
                return registry;
            }
            switch (url.getProtocol()) {
                case "consul":
                    registry = new ConsulRegistryFactory(url).createRegistry();
                    break;
                default:
                    registry = new ConsulRegistryFactory(url).createRegistry();
                    break;
            }
            if (registry == null) {
                throw new RuntimeException("Create registry false for url:" + url);
            }
            registries.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Create registry false for url:" + url, e);
        } finally {
            lock.unlock();
        }
    }

    private static class ConsulRegistryFactory {

        private final URL url;

        public ConsulRegistryFactory(URL url){
            this.url = url;
        }

        public ConsulRegistry createRegistry() {
            String host = ConsulConstants.DEFAULT_HOST;
            int port = ConsulConstants.DEFAULT_PORT;
            if (StringUtils.isNotBlank(url.getHost())) {
                host = url.getHost();
            }
            if (url.getPort() > 0) {
                port = url.getPort();
            }
            GrpcConsulClient client = new GrpcConsulEcwidClient(host, port);
            return new ConsulRegistry(url, client);
        }

    }

}
