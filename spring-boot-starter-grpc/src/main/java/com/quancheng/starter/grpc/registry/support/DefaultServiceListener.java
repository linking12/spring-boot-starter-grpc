package com.quancheng.starter.grpc.registry.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.quancheng.starter.grpc.registry.NotifyListener;
import com.quancheng.starter.grpc.registry.URL;
import com.quancheng.starter.grpc.registry.URLParamType;
import com.quancheng.starter.grpc.registry.util.ConcurrentHashSet;

public class DefaultServiceListener implements ServiceListener {

    private FailbackRegistry                  registry;
    private URL                               refUrl;
    private ConcurrentHashSet<NotifyListener> notifySet;
    private Map<String, List<URL>>            groupServiceCache;

    public DefaultServiceListener(URL refUrl){
        this.refUrl = refUrl;
        notifySet = new ConcurrentHashSet<NotifyListener>();
        groupServiceCache = new ConcurrentHashMap<String, List<URL>>();
    }

    public void setRegistry(FailbackRegistry registry) {
        this.registry = registry;
    }

    public void addNotifyListener(NotifyListener notifyListener) {
        notifySet.add(notifyListener);
    }

    public void removeNotifyListener(NotifyListener notifyListener) {
        notifySet.remove(notifyListener);
    }

    @Override
    public void notifyService(URL serviceUrl, URL registryUrl, List<URL> urls) {
        if (registry == null) {
            throw new RuntimeException("registry must be set.");
        }
        URL urlCopy = serviceUrl.createCopy();
        String groupName = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        groupServiceCache.put(groupName, urls);
        List<URL> finalResult = new ArrayList<URL>();
        finalResult.addAll(discoverOneGroup(refUrl));
        for (NotifyListener notifyListener : notifySet) {
            notifyListener.notify(registry.getUrl(), finalResult);
        }
    }

    private List<URL> discoverOneGroup(URL urlCopy) {
        String group = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        List<URL> list = groupServiceCache.get(group);
        if (list == null) {
            list = registry.discover(urlCopy);
            groupServiceCache.put(group, list);
        }
        return list;
    }

}
