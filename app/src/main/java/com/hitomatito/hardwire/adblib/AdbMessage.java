/*
 * Copyright (c) 2013, Cameron Gutman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 *   Neither the name of the copyright holder nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
