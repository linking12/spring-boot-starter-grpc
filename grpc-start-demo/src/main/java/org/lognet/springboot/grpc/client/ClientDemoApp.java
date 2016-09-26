package org.lognet.springboot.grpc.client;

import org.lognet.springboot.grpc.proto.GreeterGrpc;
import org.lognet.springboot.grpc.proto.GreeterOuterClass;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;

import com.quancheng.starter.grpc.GRpcReference;

/**
 * Created by alexf on 28-Jan-16.
 */

@SpringBootApplication
public class ClientDemoApp implements CommandLineRunner, EmbeddedServletContainerCustomizer {

    @GRpcReference(interfaceName = "org.lognet.springboot.grpc.proto.GreeterGrpc", group = "group1", version = "1.0.0.0")
    private GreeterGrpc.GreeterFutureStub greeterFutureStub;

    public static void main(String[] args) {

        SpringApplication.run(ClientDemoApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String name = "John";
        final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        System.out.println(reply);

    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        container.setPort(8081);

    }

}
