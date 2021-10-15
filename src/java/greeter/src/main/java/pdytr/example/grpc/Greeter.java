package pdytr.example.grpc;

import io.grpc.stub.StreamObserver;

import pdytr.example.grpc.GreeterOuterClass.HelloRequest;
import pdytr.example.grpc.GreeterOuterClass.HelloReply;
import pdytr.example.grpc.GreeterGrpc.GreeterImplBase;

public class Greeter extends GreeterImplBase {
  @Override
  public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
    String name = request.getName();

    // HelloRequest has toString auto-generated.
    System.out.println("Server received the name: " + name);

    // You must use a builder to construct a new Protobuffer object
    String message = "Hello there, " + name;
    HelloReply response = HelloReply.newBuilder().setMessage(message).build();

    // Use responseObserver to send a single response back
    responseObserver.onNext(response);

    // When you are done, you must call onCompleted.
    responseObserver.onCompleted();
  }
}