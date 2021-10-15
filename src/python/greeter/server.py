from argparse import ArgumentParser
from concurrent import futures

import grpc

from greeter_pb2 import HelloReply
from greeter_pb2_grpc import GreeterServicer, add_GreeterServicer_to_server

MAX_CONNECTIONS = 10


class Greeter(GreeterServicer):
    def SayHello(self, request, context):
        name = request.name
        print(f"Server received the name: {name}")
        return HelloReply(message=f"Hello, {name}!")


def serve(port):
    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=MAX_CONNECTIONS)
    )
    add_GreeterServicer_to_server(Greeter(), server)
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    server.wait_for_termination()


if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument("-p", "--port", type=int, required=True)

    args = parser.parse_args()

    serve(args.port)
