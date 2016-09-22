package org.lognet.springboot.grpc.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by alexf on 28-Jan-16.
 */

@SpringBootApplication
public class DemoApp implements CommandLineRunner {

    // @GRpcReference(serviceName = "org.lognet.springboot.grpc.demo.GreeterService", group = "default", version =
    // "1.0")
    // private GreeterGrpc.GreeterFutureStub greeterFutureStub;

    public static void main(String[] args) {

        SpringApplication.run(DemoApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // String name = "John";
        // final GreeterOuterClass.HelloRequest helloRequest =
        // GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        // final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        // System.out.println(reply);

    }

}
