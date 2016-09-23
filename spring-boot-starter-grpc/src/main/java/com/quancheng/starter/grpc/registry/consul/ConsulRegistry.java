package com.quancheng.starter.grpc.registry.consul;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quancheng.starter.grpc.registry.URL;
import com.quancheng.starter.grpc.registry.URLParamType;
import com.quancheng.starter.grpc.registry.consul.client.GrpcConsulClient;
import com.quancheng.starter.grpc.registry.support.CommonFailBackRegistry;
import com.quancheng.starter.grpc.registry.support.ServiceListener;

public class ConsulRegistry extends CommonFailBackRegistry {

    private static final Logger                                                log                 = LoggerFactory.getLogger(ConsulRegistry.class);

    private GrpcConsulClient                                                   client;
    private ConsulHeartbeatManager                                             heartbeatManager;
    private int                                                                lookupInterval;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, List<URL>>>    serviceCache        = new ConcurrentHashMap<String, ConcurrentHashMap<String, List<URL>>>();
    private ConcurrentHashMap<String, Long>                                    lookupGroupServices = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> serviceListeners    = new ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>>();
    private ThreadPoolExecutor                                                 notifyExecutor;

    public ConsulRegistry(URL url, GrpcConsulClient client){
        super(url);
        this.client = client;
        heartbeatManager = new ConsulHeartbeatManager(client);
        heartbeatManager.start();
        lookupInterval = getUrl().getIntParameter(URLParamType.registrySessionTimeout.getName(),
                                                  ConsulConstants.DEFAULT_LOOKUP_INTERVAL);
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(20000);
        notifyExecutor = new ThreadPoolExecutor(10, 30, 30 * 1000, TimeUnit.MILLISECONDS, workQueue);
        log.info("ConsulRegistry init finish.");
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> getServiceListeners() {
        return serviceListeners;
    }

    @Override
    protected void doRegister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.registerService(service);
        heartbeatManager.addHeartbeatServcieId(service.getId());
    }

    @Override
    protected void doUnregister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.unregisterService(service.getId());
        heartbeatManager.removeHeartbeatServiceId(service.getId());
    }

    @Override
    protected void doAvailable(URL url) {
        if (url != null) {
            heartbeatManager.setHeartbeatOpen(true);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url != null) {
            heartbeatManager.setHeartbeatOpen(false);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support unavailable by urls yet");
        }
    }

    @Override
    protected void subscribeService(URL url, ServiceListener serviceListener) {
        addServiceListener(url, serviceListener);
        startListenerThreadIfNewService(url);
    }

    private void startListenerThreadIfNewService(URL url) {
        String group = url.getGroup();
        if (!lookupGroupServices.containsKey(group)) {
            Long value = lookupGroupServices.putIfAbsent(group, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(group);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeService(URL url, ServiceListener listener) {
        ConcurrentHashMap<URL, ServiceListener> listeners = serviceListeners.get(ConsulUtils.getUrlClusterInfo(url));
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(url);
            }
        }
    }

    private void addServiceListener(URL url, ServiceListener serviceListener) {
        String service = ConsulUtils.getUrlClusterInfo(url);
        ConcurrentHashMap<URL, ServiceListener> map = serviceListeners.get(service);
        if (map == null) {
            serviceListeners.putIfAbsent(service, new ConcurrentHashMap<URL, ServiceListener>());
            map = serviceListeners.get(service);
        }
        synchronized (map) {
            map.put(url, serviceListener);
        }
    }

    @Override
    protected List<URL> discoverService(URL url) {
        String service = ConsulUtils.getUrlClusterInfo(url);
        String group = url.getGroup();
        String version = url.getVersion();
        List<URL> serviceUrls = new ArrayList<URL>();
        ConcurrentHashMap<String, List<URL>> serviceMap = serviceCache.get(group);
        if (serviceMap == null) {
            synchronized (group.intern()) {
                serviceMap = serviceCache.get(group);
                if (serviceMap == null) {
                    ConcurrentHashMap<String, List<URL>> groupUrls = lookupServiceUpdate(group);
                    updateServiceCache(group, groupUrls, false);
                    serviceMap = serviceCache.get(group);
                }
            }
        }
        if (serviceMap != null) {
            List<URL> serviceUrlsCache = serviceMap.get(service);
            for (URL serviceUrlCache : serviceUrlsCache) {
                if (serviceUrlCache.getVersion().equals(version)) {
                    serviceUrls.add(serviceUrlCache);
                }
            }
        }
        return serviceUrls;
    }

    private class ServiceLookupThread extends Thread {

        private String group;

        public ServiceLookupThread(String group){
            super("ServiceLookup task");
            this.group = group;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(lookupInterval);
                    ConcurrentHashMap<String, List<URL>> groupUrls = lookupServiceUpdate(group);
                    updateServiceCache(group, groupUrls, true);
                } catch (Throwable e) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private class NotifyService extends Thread {

        private String    service;
        private List<URL> urls;

        public NotifyService(String service, List<URL> urls){
            super("Service Notify task");
            this.service = service;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<URL, ServiceListener> listeners = serviceListeners.get(service);
            synchronized (listeners) {
                for (Map.Entry<URL, ServiceListener> entry : listeners.entrySet()) {
                    ServiceListener serviceListener = entry.getValue();
                    serviceListener.notifyService(entry.getKey(), getUrl(), urls);
                }
            }
        }
    }

    private ConcurrentHashMap<String, List<URL>> lookupServiceUpdate(String group) {
        Long lastConsulIndexId = lookupGroupServices.get(group) == null ? 0L : lookupGroupServices.get(group);
        ConsulResponse<List<ConsulService>> response = lookupConsulService(group, lastConsulIndexId);
        if (response != null) {
            List<ConsulService> services = response.getValue();
            if (services != null && !services.isEmpty() && response.getConsulIndex() > lastConsulIndexId) {
                ConcurrentHashMap<String, List<URL>> groupUrls = new ConcurrentHashMap<String, List<URL>>();
                for (ConsulService service : services) {
                    try {
                        URL url = ConsulUtils.buildUrl(service);
                        String cluster = ConsulUtils.getUrlClusterInfo(url);
                        List<URL> urlList = groupUrls.get(cluster);
                        if (urlList == null) {
                            urlList = new ArrayList<URL>();
                            groupUrls.put(cluster, urlList);
                        }
                        urlList.add(url);
                    } catch (Exception e) {
                        log.error("convert consul service to url fail! service:" + service, e);
                    }
                }
                lookupGroupServices.put(group, response.getConsulIndex());
                return groupUrls;
            } else {
                log.info(group + " no need update, lastIndex:" + lastConsulIndexId);
            }
        }
        return null;
    }

    private ConsulResponse<List<ConsulService>> lookupConsulService(String serviceName, Long lastConsulIndexId) {
        ConsulResponse<List<ConsulService>> response = client.lookupHealthService(ConsulUtils.convertGroupToServiceName(serviceName),
                                                                                  lastConsulIndexId);
        return response;
    }

    private void updateServiceCache(String group, ConcurrentHashMap<String, List<URL>> groupUrls, boolean needNotify) {
        if (groupUrls != null && !groupUrls.isEmpty()) {
            ConcurrentHashMap<String, List<URL>> groupMap = serviceCache.get(group);
            if (groupMap == null) {
                serviceCache.put(group, groupUrls);
            }
            for (Map.Entry<String, List<URL>> entry : groupUrls.entrySet()) {
                boolean change = true;
                if (groupMap != null) {
                    List<URL> oldUrls = groupMap.get(entry.getKey());
                    List<URL> newUrls = entry.getValue();
                    if (newUrls == null || newUrls.isEmpty() || ConsulUtils.isSame(entry.getValue(), oldUrls)) {
                        change = false;
                    } else {
                        groupMap.put(entry.getKey(), newUrls);
                    }
                }
                if (change && needNotify) {
                    notifyExecutor.execute(new NotifyService(entry.getKey(), entry.getValue()));
                    StringBuilder sb = new StringBuilder();
                    for (URL url : entry.getValue()) {
                        sb.append(url.getUri()).append(";");
                    }
                }
            }
        }
    }

}
