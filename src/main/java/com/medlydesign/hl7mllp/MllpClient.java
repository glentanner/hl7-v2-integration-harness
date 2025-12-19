package com.medlydesign.hl7mllp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class MllpClient {
    private static final byte VT = 0x0B;
    private static final byte FS = 0x1C;
    private static final byte CR = 0x0D;

    public static String sendAndReceiveAck(String host, int port, int readTimeoutMs, String hl7Payload) throws IOException {
        byte[] hl7Bytes = hl7Payload.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream framed = new ByteArrayOutputStream(hl7Bytes.length + 3);
        framed.write(VT);
        framed.write(hl7Bytes);
        framed.write(FS);
        framed.write(CR);

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(readTimeoutMs);

            OutputStream out = socket.getOutputStream();
            out.write(framed.toByteArray());
            out.flush();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream ack = new ByteArrayOutputStream(1024);

            int b;
            boolean sawFs = false;
            while ((b = in.read()) != -1) {
                if (sawFs) {
                    if (b == CR) break;   // FS CR ends message
                    ack.write(FS);
                    sawFs = false;
                }

                if (b == FS) {
                    sawFs = true;
                } else if (b != VT) {
                    ack.write(b);
                }
            }
            return ack.toString(StandardCharsets.UTF_8);
        }
    }
}
