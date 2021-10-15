from argparse import ArgumentParser

import grpc

from greeter_pb2 import HelloRequest
from greeter_pb2_grpc import GreeterStub


def greet(port, greeting):
    with grpc.insecure_channel(f"localhost:{port}") as channel:
        stub = GreeterStub(channel)
        response = stub.SayHello(HelloRequest(**greeting))

    print(f"Client received the greeting: {response.message}")


if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument("-p", "--port", type=int, required=True)

    args = parser.parse_args()

    greet(args.port, {"name": "Andres"})
