package com.quancheng.starter.grpc.registry.support;

import java.util.List;

import com.quancheng.starter.grpc.registry.URL;

public interface ServiceListener {

    void notifyService(URL refUrl, URL registryUrl, List<URL> urls);
}
