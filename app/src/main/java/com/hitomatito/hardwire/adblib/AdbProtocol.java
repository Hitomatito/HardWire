package com.hitomatito.hardwire.adblib;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class AdbProtocol {
    public static final int ADB_HEADER_LENGTH = 24;
    public static final int CMD_SYNC = 0x434e5953;
    public static final int CMD_CNXN = 0x4e584e43;
    public static final int CONNECT_VERSION = 0x01000000;
    public static final int CONNECT_MAXDATA = 4096;

    public static byte[] CONNECT_PAYLOAD;
    static {
        try {
            CONNECT_PAYLOAD = "host::\0".getBytes("UTF-8");
        } catch (Exception e) {}
    }

    public static final int CMD_AUTH = 0x48545541;
    public static final int AUTH_TYPE_TOKEN = 1;
    public static final int AUTH_TYPE_SIGNATURE = 2;
    public static final int AUTH_TYPE_RSA_PUBLIC = 3;
    public static final int CMD_OPEN = 0x4e45504f;
    public static final int CMD_OKAY = 0x59414b4f;
    public static final int CMD_CLSE = 0x45534c43;
    public static final int CMD_WRTE = 0x45545257;

    public static boolean validateMessage(AdbMessage msg) {
        if (msg.getCommand() != (msg.getMagic() ^ 0xFFFFFFFF))
            return false;
        if (msg.getPayloadLength() != 0) {
            if (AdbMessage.checksum(msg.getPayload()) != msg.getChecksum())
                return false;
        }
        return true;
    }

    public static AdbMessage generateMessage(int cmd, int arg0, int arg1, byte[] payload) {
        return new AdbMessage(cmd, arg0, arg1, payload);
    }

    public static AdbMessage generateConnect() {
        return generateMessage(CMD_CNXN, CONNECT_VERSION, CONNECT_MAXDATA, CONNECT_PAYLOAD);
    }

    public static AdbMessage generateAuth(int type, byte[] data) {
        return generateMessage(CMD_AUTH, type, 0, data);
    }

    public static AdbMessage generateOpen(int localId, String dest) throws java.io.UnsupportedEncodingException {
        java.nio.ByteBuffer bbuf = java.nio.ByteBuffer.allocate(dest.length() + 1);
        bbuf.put(dest.getBytes("UTF-8"));
        bbuf.put((byte)0);
        return generateMessage(CMD_OPEN, localId, 0, bbuf.array());
    }

    public static AdbMessage generateWrite(int localId, int remoteId, byte[] data) {
        return generateMessage(CMD_WRTE, localId, remoteId, data);
    }

    public static AdbMessage generateClose(int localId, int remoteId) {
        return generateMessage(CMD_CLSE, localId, remoteId, null);
    }

    public static AdbMessage generateReady(int localId, int remoteId) {
        return generateMessage(CMD_OKAY, localId, remoteId, null);
    }
}
