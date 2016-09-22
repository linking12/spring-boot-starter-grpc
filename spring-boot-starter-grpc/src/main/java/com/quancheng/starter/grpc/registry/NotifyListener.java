package com.quancheng.starter.grpc.registry;

import java.util.List;

public interface NotifyListener {

    void notify(URL registryUrl, List<URL> urls);
}
