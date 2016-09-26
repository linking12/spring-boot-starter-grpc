package org.lognet.springboot.grpc.demo;

import org.lognet.springboot.grpc.proto.GreeterGrpc;
import org.lognet.springboot.grpc.proto.GreeterOuterClass;

import com.quancheng.starter.grpc.GRpcService;

import io.grpc.stub.StreamObserver;

@GRpcService(interceptors = { LogInterceptor.class }, group = "group1", version = "1.0.0.0", interfaceName = "org.lognet.springboot.grpc.proto.GreeterGrpc")
public class GreeterService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(GreeterOuterClass.HelloRequest request,
                         StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello "
                                                                                                                       + request.getName());
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }
}
