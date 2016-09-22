package com.quancheng.starter.grpc.registry;

import java.util.Collection;
import java.util.List;

public interface Registry {

    // register
    void register(URL url);

    void unregister(URL url);

    void available(URL url);

    void unavailable(URL url);

    Collection<URL> getRegisteredServiceUrls();

    // discovery
    void subscribe(URL url, NotifyListener listener);

    void unsubscribe(URL url, NotifyListener listener);

    List<URL> discover(URL url);

    URL getUrl();
}
