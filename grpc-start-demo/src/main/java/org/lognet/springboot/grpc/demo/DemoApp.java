package org.lognet.springboot.grpc.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by alexf on 28-Jan-16.
 */

@SpringBootApplication
public class DemoApp {

    // @GRpcReference(serviceName = "org.lognet.springboot.grpc.demo.GreeterService", group = "default", version =
    // "1.0")
    // private GreeterGrpc.GreeterFutureStub greeterFutureStub;

    public static void main(String[] args) {

        SpringApplication.run(DemoApp.class, args);
    }

}
