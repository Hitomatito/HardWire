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
