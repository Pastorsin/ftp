import hashlib
import logging
from argparse import ArgumentParser
from pathlib import Path

import grpc

from ftp_pb2 import ReadRequest, WriteRequest
from ftp_pb2_grpc import FtpStub

ROOT = Path(__file__).parent
DB = ROOT / "db" / "client"

WINDOW_SIZE = 100_000  # 100KB

# Checksum
def valid_checksum(response):
    received_checksum = response.checksum
    generated_checksum = generate_checksum(response.content)

    logging.info(f"Checksum received:\t{received_checksum}")
    logging.info(f"Checksum generated:\t{generated_checksum}")

    return generated_checksum == received_checksum


def generate_checksum(content):
    return hashlib.md5(content).digest()


# [4.1]. Read from server
def read_from_server(port, filename, window):
    file = DB / filename
    stream = open(file, "wb")

    error = eof = False
    total_bytes_read = 0

    while not eof and not error:
        payload = {
            "filename": filename,
            "position": total_bytes_read,
            "offset": window,
        }

        response = read_remote(port, payload)
        logging.info(f"Bytes read: {response.bytes_read}")

        eof = response.bytes_read == -1

        if not eof:
            if valid_checksum(response):
                stream.write(response.content)
                total_bytes_read += response.bytes_read
            else:
                logging.error("Checksum is invalid.. abort")
                error = True
                remove_from_db(file)

    stream.close()


def read_remote(port, payload):
    with grpc.insecure_channel(f"localhost:{port}") as channel:
        request = ReadRequest(**payload)
        response = FtpStub(channel).read(request)

    return response


def remove_from_db(file_path):
    file_path.unlink(missing_ok=True)


# [4.2] Write to server
def write_to_server(port, filename, window):
    buffer = read_from_db(filename)

    eof = error = False
    bytes_written = 0

    while not eof and not error:
        chunk = buffer[bytes_written : bytes_written + window]

        payload = {
            "filename": filename,
            "content": chunk,
            "offset": bytes_written,
            "destroy_mode": bytes_written == 0,
            "checksum": generate_checksum(chunk),
        }

        response = write_remote(port, payload)

        bytes_written = response.bytes_written
        logging.info(f"Bytes written: {bytes_written} of {len(buffer)}")

        eof = bytes_written == len(buffer)

        if response.error:
            logging.error("Checksum is invalid.. abort")
            error = True


def write_remote(port, payload):
    with grpc.insecure_channel(f"localhost:{port}") as channel:
        request = WriteRequest(**payload)
        response = FtpStub(channel).write(request)

    return response


def read_from_db(filename):
    path = DB / filename

    with open(path, "rb") as file:
        return file.read()


def main():
    # Init logging
    logging.basicConfig(
        level=logging.DEBUG,
        format="[%(filename)s]: %(levelname)s - %(message)s",
    )

    # Parse args
    parser = ArgumentParser()

    parser.add_argument("-p", "--port", type=int, required=True)
    parser.add_argument("filename", type=str)

    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("-r", "--read", action="store_true")
    group.add_argument("-w", "--write", action="store_true")

    args = parser.parse_args()

    # Call operations
    metadata = {
        "port": args.port,
        "filename": args.filename,
        "window": WINDOW_SIZE,
    }

    if args.read:
        read_from_server(**metadata)
    else:
        write_to_server(**metadata)


if __name__ == "__main__":
    main()
