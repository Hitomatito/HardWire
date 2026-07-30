package com.hitomatito.hardwire.adblib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AdbMessage {
    private ByteBuffer mMessageBuffer;
    private byte[] payload;

    private AdbMessage() {}

    public AdbMessage(int command, int arg0, int arg1, byte[] data) {
        mMessageBuffer = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        mMessageBuffer.putInt(0, command);
        mMessageBuffer.putInt(4, arg0);
        mMessageBuffer.putInt(8, arg1);
        mMessageBuffer.putInt(12, (data == null ? 0 : data.length));
        mMessageBuffer.putInt(16, (data == null ? 0 : checksum(data)));
        mMessageBuffer.putInt(20, command ^ 0xFFFFFFFF);
        payload = data;
    }

    public AdbMessage(int command, int arg0, int arg1) {
        this(command, arg0, arg1, (byte[]) null);
    }

    public static AdbMessage parseAdbMessage(AdbChannel in) throws IOException {
        AdbMessage msg = new AdbMessage();
        ByteBuffer packet = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        in.readx(packet.array(), AdbProtocol.ADB_HEADER_LENGTH);
        msg.mMessageBuffer = packet;
        if (msg.getPayloadLength() != 0) {
            msg.setPayload(new byte[msg.getPayloadLength()]);
            in.readx(msg.getPayload(), msg.getPayloadLength());
        }
        return msg;
    }

    public static int checksum(byte[] payload) {
        int checksum = 0;
        for (byte b : payload) {
            if (b >= 0)
                checksum += b;
            else
                checksum += b + 256;
        }
        return checksum;
    }

    public int getCommand() { return mMessageBuffer.getInt(0); }
    public int getArg0() { return mMessageBuffer.getInt(4); }
    public int getArg1() { return mMessageBuffer.getInt(8); }
    public int getPayloadLength() { return mMessageBuffer.getInt(12); }
    public int getChecksum() { return mMessageBuffer.getInt(16); }
    public int getMagic() { return mMessageBuffer.getInt(20); }
    public byte[] getMessage() { return mMessageBuffer.array(); }
    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }
}
