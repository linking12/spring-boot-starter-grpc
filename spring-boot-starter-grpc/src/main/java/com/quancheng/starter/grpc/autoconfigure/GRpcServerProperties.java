package com.quancheng.starter.grpc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grpc")
public class GRpcServerProperties {

    private int    serverPort = 6565;

    private String consulIp;

    private int    consulPort;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getConsulIp() {
        return consulIp;
    }

    public void setConsulIp(String consulIp) {
        this.consulIp = consulIp;
    }

    public int getConsulPort() {
        return consulPort;
    }

    public void setConsulPort(int consulPort) {
        this.consulPort = consulPort;
    }

}
