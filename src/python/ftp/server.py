import hashlib
import logging
from argparse import ArgumentParser
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import grpc

from ftp_pb2 import ReadResponse, WriteResponse
from ftp_pb2_grpc import FtpServicer, add_FtpServicer_to_server

MAX_CONNECTIONS = 10

ROOT = Path(__file__).parent
DB = ROOT / "db" / "server"
EOF = b""


class Ftp(FtpServicer):
    def read(self, request, context):
        with open(DB / request.filename, "rb") as file:
            file.seek(request.position)
            content = file.read(request.offset)

        return ReadResponse(
            content=content,
            bytes_read=-1 if content == EOF else len(content),
            checksum=self._generate_checksum(content),
        )

    def write(self, request, context):
        file_path = DB / request.filename
        bytes_written = 0

        if error := not self._valid_checksum(request):
            file_path.unlink(missing_ok=True)
        else:
            mode = "wb" if request.destroy_mode else "ab"

            with open(file_path, mode) as file:
                file.seek(request.offset)
                file.write(request.content)

            bytes_written = file_path.stat().st_size

        return WriteResponse(bytes_written=bytes_written, error=error)

    def _generate_checksum(self, content):
        return hashlib.md5(content).digest()

    def _valid_checksum(self, request):
        received_checksum = request.checksum
        generated_checksum = self._generate_checksum(request.content)

        logging.info(f"Checksum received:\t{received_checksum}")
        logging.info(f"Checksum generated:\t{generated_checksum}")

        return generated_checksum == received_checksum


def serve(port):
    server = grpc.server(ThreadPoolExecutor(max_workers=MAX_CONNECTIONS))
    add_FtpServicer_to_server(Ftp(), server)
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    server.wait_for_termination()


if __name__ == "__main__":
    # Init logger
    logging.basicConfig(
        level=logging.DEBUG,
        format="[%(filename)s]: %(levelname)s - %(message)s",
    )

    # Parse args
    parser = ArgumentParser()
    parser.add_argument("-p", "--port", type=int, required=True)

    args = parser.parse_args()

    serve(args.port)
