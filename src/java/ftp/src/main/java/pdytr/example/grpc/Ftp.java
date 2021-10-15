package pdytr.example.grpc;

import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileInputStream;
import java.io.File;

import pdytr.example.grpc.FtpOuterClass.*;

public class Ftp extends FtpGrpc.FtpImplBase {
    private static String DB = "db/server";
    private ByteString content;
    private ByteString checksum;

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        try {
            File file = new File(DB, request.getFilename());
            FileInputStream stream = new FileInputStream(file);

            int position = request.getPosition();
            int offset = request.getOffset();

            byte[] content = new byte[offset];

            stream.skip(position);
            int bytesRead = stream.read(content, 0, offset);

            byte[] checksum = generateChecksum(content);

            ReadResponse response = ReadResponse.newBuilder()
                    .setContent(ByteString.copyFrom(content))
                    .setBytesRead(bytesRead)
                    .setChecksum(ByteString.copyFrom(checksum)).build();

            responseObserver.onNext(response);

            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObserver.onCompleted();
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

    @Override
    public void write(WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        try {
            long bytesWritten = 0;
            boolean error = false;

            File file = new File(DB, request.getFilename());

            content = request.getContent();
            checksum = request.getChecksum();

            if (validChecksum(content, checksum)) {
                boolean appendMode = !request.getDestroyMode();
                FileOutputStream stream = new FileOutputStream(file, appendMode);

                stream.write(content.toByteArray(), 0, request.getOffset());
                stream.close();

                bytesWritten = file.length();
            } else {
                error = true;
                file.delete();
            }

            WriteResponse response = WriteResponse.newBuilder()
                .setBytesWritten(bytesWritten)
                .setError(error).build();

            responseObserver.onNext(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
        responseObserver.onCompleted();
    }
}