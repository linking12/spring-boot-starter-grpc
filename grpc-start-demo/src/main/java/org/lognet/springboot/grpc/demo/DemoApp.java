package org.lognet.springboot.grpc.demo;

import org.lognet.springboot.grpc.proto.GreeterGrpc;
import org.lognet.springboot.grpc.proto.GreeterOuterClass;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quancheng.starter.grpc.GrpcConstants;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * Created by alexf on 28-Jan-16.
 */

@SpringBootApplication
public class DemoApp {

    public static void main(String[] args) {

        SpringApplication.run(DemoApp.class, args);
    }

    @Component
    @Order(value = 1)
    public class DefaultCommandLineRunner implements CommandLineRunner {

        @Override
        public void run(String... args) throws Exception {
            ManagedChannel channel = InProcessChannelBuilder.forName(GrpcConstants.GRPC_IN_LOCAL_PROCESS).build();
            String name = "John";
            final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
            final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
            final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
            System.out.println(reply);
        }
    }

}
