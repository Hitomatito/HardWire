package com.hitomatito.hardwire.adblib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TcpChannel implements AdbChannel {
    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public TcpChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void readx(byte[] buffer, int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int read = inputStream.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new IOException("EOF reached, expected " + (length - offset) + " more bytes");
            }
            offset += read;
        }
    }

    private void writex(byte[] buffer) throws IOException {
        outputStream.write(buffer);
        outputStream.flush();
    }

    @Override
    public void writex(AdbMessage message) throws IOException {
        writex(message.getMessage());
        if (message.getPayload() != null) {
            writex(message.getPayload());
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
