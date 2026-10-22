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

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdbStream implements Closeable {
    private AdbConnection adbConn;
    private int localId;
    private int remoteId;
    private AtomicBoolean writeReady;
    private Queue<byte[]> readQueue;
    private boolean isClosed;

    public AdbStream(AdbConnection adbConn, int localId) {
        this.adbConn = adbConn;
        this.localId = localId;
        this.readQueue = new ConcurrentLinkedQueue<>();
        this.writeReady = new AtomicBoolean(false);
        this.isClosed = false;
    }

    void addPayload(byte[] payload) {
        synchronized (readQueue) {
            readQueue.add(payload);
            readQueue.notifyAll();
        }
    }

    void sendReady() throws IOException {
        adbConn.channel.writex(AdbProtocol.generateReady(localId, remoteId));
    }

    void updateRemoteId(int remoteId) {
        this.remoteId = remoteId;
    }

    void readyForWrite() {
        writeReady.set(true);
    }

    void notifyClose() {
        isClosed = true;
        synchronized (this) {
            notifyAll();
        }
        synchronized (readQueue) {
            readQueue.notifyAll();
        }
    }

    public byte[] read() throws InterruptedException, IOException {
        return read(0);
    }

    public byte[] read(long timeoutMs) throws InterruptedException, IOException {
        byte[] data = null;
        synchronized (readQueue) {
            if (timeoutMs > 0) {
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (true) {
                    data = readQueue.poll();
                    if (data != null) break;
                    if (isClosed) break;
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) return null;
                    readQueue.wait(remaining);
                }
            } else {
                while (true) {
                    data = readQueue.poll();
                    if (data != null) break;
                    if (isClosed) break;
                    readQueue.wait();
                }
            }
            if (data == null && isClosed) {
                return null;
            }
        }
        return data;
    }

    public void write(String payload) throws IOException, InterruptedException {
        write((payload + "\0").getBytes("UTF-8"));
    }

    public void write(byte[] payload) throws IOException, InterruptedException {
        synchronized (this) {
            while (!isClosed && !writeReady.compareAndSet(true, false))
                wait();
            if (isClosed) {
                throw new IOException("Stream closed");
            }
        }
        adbConn.channel.writex(AdbProtocol.generateWrite(localId, remoteId, payload));
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (isClosed) return;
            notifyClose();
        }
        adbConn.channel.writex(AdbProtocol.generateClose(localId, remoteId));
    }

    public boolean isClosed() {
        return isClosed;
    }

    public int getRemoteId() {
        return remoteId;
    }
}
