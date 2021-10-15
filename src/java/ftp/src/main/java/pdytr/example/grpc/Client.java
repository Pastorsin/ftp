package pdytr.example.grpc;

import static java.lang.Math.min;

import io.grpc.*;

import com.google.protobuf.ByteString;

import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;

import pdytr.example.grpc.FtpOuterClass.*;
import pdytr.example.grpc.FtpGrpc.*;
import static pdytr.example.grpc.FtpGrpc.newBlockingStub;

public class Client {

    private static int WINDOW = 100000; // 100KB
    private static String DB = "db/client";

    private static void read(FtpBlockingStub stub, String filename) {
        try {
            File file = new File(DB, filename);
            FileOutputStream stream = new FileOutputStream(file);

            boolean error = false;
            boolean eof = false;            
            int totalBytesRead = 0;

            while (!eof && !error) {
                ReadRequest request = ReadRequest.newBuilder()
                        .setFilename(filename)
                        .setPosition(totalBytesRead)
                        .setOffset(WINDOW).build();

                ReadResponse response = stub.read(request);

                int bytesRead = response.getBytesRead();
                System.out.printf("Bytes read: %d\n", bytesRead);
                
                eof = bytesRead == -1;

                if (!eof) {
                    ByteString content = response.getContent();
                    ByteString checksum = response.getChecksum();
    
                    if (validChecksum(content, checksum)) {
                        stream.write(content.toByteArray(), 0, bytesRead);
                        totalBytesRead += bytesRead;
                    } else {
                        System.err.println("Checksum is invalid.. abort");
                        error = true;
                        file.delete();
                    }
                }
            }

            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean validChecksum(ByteString content, ByteString checksumReceived)
            throws NoSuchAlgorithmException {
        String received = checksumToString(checksumReceived.toByteArray());
        String generated = checksumToString(generateChecksum(content.toByteArray()));

        System.out.printf("Checksum received:\t%s\n", received);
        System.out.printf("Checksum generated:\t%s\n", generated);

        return received.equals(generated);
    }

    private static String checksumToString(byte[] checksum) {
        StringBuffer stringBuffer = new StringBuffer();

        for (byte bytes : checksum)
            stringBuffer.append(String.format("%02x", bytes & 0xff));

        return new String(stringBuffer);
    }

    private static byte[] generateChecksum(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return messageDigest.digest(message);
    }

    private static void write(FtpBlockingStub stub, String filename) {
        try {
            File file = new File(DB, filename);
            FileInputStream stream = new FileInputStream(file);

            boolean error = false;
            boolean eof = false;            
            long bytesWritten = 0;

            while (!eof && !error) {
                byte[] content = new byte[WINDOW];

                int bytesRemaining = stream.available();
                stream.read(content);

                byte[] checksum = generateChecksum(content);

                WriteRequest request = WriteRequest.newBuilder()
                        .setFilename(filename)
                        .setContent(ByteString.copyFrom(content))
                        .setOffset(min(WINDOW, bytesRemaining))
                        .setDestroyMode(bytesWritten == 0)
                        .setChecksum(ByteString.copyFrom(checksum)).build();

                WriteResponse response = stub.write(request);

                bytesWritten = response.getBytesWritten();
                System.out.printf("Bytes written: %d of %d\n", bytesWritten, file.length());

                eof = bytesWritten == file.length();

                if (response.getError()) {
                    System.err.println("Checksum is invalid.. abort");
                    error = true;
                }
            }

            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Invalid arguments");
            System.err.println("[-read|-write] filename");

            System.exit(1);
        }

        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080").usePlaintext(true).build();

        FtpBlockingStub stub = newBlockingStub(channel);

        String operation = args[0];
        String filename = args[1];

        switch (operation) {
            case "-read":
                read(stub, filename);
                break;
            case "-write":
                write(stub, filename);
                break;
            default:
                System.err.println("Invalid arguments");
                System.err.println("[-read|-write] filename");
        }

        channel.shutdownNow();
        System.exit(0);
    }
}
