package org.lognet.springboot.grpc.demo;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.lognet.springboot.grpc.proto.GreeterGrpc;
import org.lognet.springboot.grpc.proto.GreeterOuterClass;

import com.google.common.base.Preconditions;
import com.quancheng.starter.grpc.internal.ConsulNameResolver;

import io.grpc.Attributes;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;

public class TestMain {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        // URL registerUrl = new URL("consul", "192.168.99.103", 8500, "");
        // Registry registry = new ConsulRegistryFactory().getRegistry(registerUrl);
        // URL refUrl = new URL(GrpcConstants.DEFAULT_PROTOCOL, "", 0,
        // "org.lognet.springboot.grpc.demo.GreeterService");
        // registry.subscribe(refUrl, new NotifyListener() {
        //
        // @Override
        // public void notify(URL registryUrl, List<URL> urls) {
        // System.out.print(registryUrl.toFullStr() + urls);
        // }
        //
        // });
        ManagedChannel channel = ManagedChannelBuilder.forTarget("consul:///192.168.99.103:8500").nameResolverFactory(new NameResolver.Factory() {

            @Override
            public NameResolver newNameResolver(URI targetUri, Attributes params) {
                String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
                Preconditions.checkArgument(targetPath.startsWith("/"),
                                            "the path component (%s) of the target (%s) must start with '/'",
                                            targetPath, targetUri);
                String name = targetPath.substring(1);
                Attributes attributes = Attributes.newBuilder().set(ConsulNameResolver.PARAMS_DEFAULT_SERVICESNAME,
                                                                    "org.lognet.springboot.grpc.demo.GreeterService").build();
                return new ConsulNameResolver(targetUri.getAuthority(), name, attributes);
            }

            @Override
            public String getDefaultScheme() {
                return "consul";
            }

        }).usePlaintext(true).build();
        // ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext(true).build();
        String name = "John";
        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
        final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        System.out.println(reply);

    }

}
