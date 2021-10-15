package pdytr.example.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pdytr.example.grpc.GreeterGrpc;
import pdytr.example.grpc.GreeterGrpc.GreeterBlockingStub;
import pdytr.example.grpc.GreeterOuterClass.HelloRequest;
import pdytr.example.grpc.GreeterOuterClass.HelloReply;

public class Client {
  public static void main(String[] args) throws Exception {
    // Channel is the abstraction to connect to a service endpoint
    // Let's use plaintext communication because we don't have certs
    final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080").usePlaintext(true).build();

    // It is up to the client to determine whether to block the call
    // Here we create a blocking stub, but an async stub,
    // or an async stub with Future are always possible.
    GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Ray").build();

    // Finally, make the call using the stub
    HelloReply response = stub.sayHello(request);
    String greeting = response.getMessage();

    System.out.println("Client received the greeting: " + greeting);

    // A Channel should be shutdown before stopping the process.
    channel.shutdownNow();
  }
}