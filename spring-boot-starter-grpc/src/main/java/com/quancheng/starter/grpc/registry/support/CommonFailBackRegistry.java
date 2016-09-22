package com.quancheng.starter.grpc.registry.support;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.quancheng.starter.grpc.registry.NotifyListener;
import com.quancheng.starter.grpc.registry.URL;

public abstract class CommonFailBackRegistry extends FailbackRegistry {

    private ConcurrentHashMap<URL, DefaultServiceListener> serviceListenerMap;

    public CommonFailBackRegistry(URL url){
        super(url);
        serviceListenerMap = new ConcurrentHashMap<URL, DefaultServiceListener>();
    }

    @Override
    protected void doSubscribe(URL url, final NotifyListener listener) {
        URL urlCopy = url.createCopy();
        DefaultServiceListener manager = getDefaultServiceListener(urlCopy);
        manager.addNotifyListener(listener);
        subscribeService(urlCopy, manager);
        List<URL> urls = doDiscover(urlCopy);
        if (urls != null && urls.size() > 0) {
            this.notify(urlCopy, listener, urls);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        URL urlCopy = url.createCopy();
        DefaultServiceListener manager = serviceListenerMap.get(urlCopy);
        manager.removeNotifyListener(listener);
        unsubscribeService(urlCopy, manager);

    }

    @Override
    protected List<URL> doDiscover(URL url) {
        URL urlCopy = url.createCopy();
        List<URL> finalResult = discoverService(urlCopy);
        return finalResult;
    }

    private DefaultServiceListener getDefaultServiceListener(URL urlCopy) {
        DefaultServiceListener manager = serviceListenerMap.get(urlCopy);
        if (manager == null) {
            manager = new DefaultServiceListener(urlCopy);
            manager.setRegistry(this);
            DefaultServiceListener manager1 = serviceListenerMap.putIfAbsent(urlCopy, manager);
            if (manager1 != null) manager = manager1;
        }
        return manager;
    }

    protected abstract void subscribeService(URL url, ServiceListener listener);

    protected abstract void unsubscribeService(URL url, ServiceListener listener);

    protected abstract List<URL> discoverService(URL url);

}
