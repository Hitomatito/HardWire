package com.hitomatito.hardwire.adblib;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.ConcurrentHashMap;

public class AdbConnection implements Closeable {
    private static final String TAG = "HW:AdbConn";
    AdbChannel channel;
    private int lastLocalId;
    private Thread connectionThread;
    private boolean connectAttempted;
    private boolean connected;
    private int maxData;
    private AdbCrypto crypto;
    private boolean sentSignature;
    private boolean publicKeySent;
    private ConcurrentHashMap<Integer, AdbStream> openStreams;

    private AdbConnection() {
        openStreams = new ConcurrentHashMap<>();
        lastLocalId = 0;
        connectionThread = createConnectionThread();
    }

    public static AdbConnection create(AdbChannel channel, AdbCrypto crypto) throws IOException {
        AdbConnection newConn = new AdbConnection();
        newConn.crypto = crypto;
        newConn.channel = channel;
        return newConn;
    }

    private Thread createConnectionThread() {
        final AdbConnection conn = this;
        return new Thread(() -> {
            while (!connectionThread.isInterrupted()) {
                try {
                    AdbMessage msg = AdbMessage.parseAdbMessage(channel);
                    if (!AdbProtocol.validateMessage(msg))
                        continue;

                    switch (msg.getCommand()) {
                        case AdbProtocol.CMD_OKAY:
                        case AdbProtocol.CMD_WRTE:
                        case AdbProtocol.CMD_CLSE:
                            if (!conn.connected) continue;

                            AdbStream waitingStream = openStreams.get(msg.getArg1());
                            if (waitingStream == null) continue;

                            synchronized (waitingStream) {
                                if (msg.getCommand() == AdbProtocol.CMD_OKAY) {
                                    waitingStream.updateRemoteId(msg.getArg0());
                                    waitingStream.readyForWrite();
                                    waitingStream.notify();
                                } else if (msg.getCommand() == AdbProtocol.CMD_WRTE) {
                                    waitingStream.addPayload(msg.getPayload());
                                    waitingStream.sendReady();
                                } else if (msg.getCommand() == AdbProtocol.CMD_CLSE) {
                                    conn.openStreams.remove(msg.getArg1());
                                    waitingStream.notifyClose();
                                }
                            }
                            break;

                        case AdbProtocol.CMD_AUTH:
                            AdbMessage packet;
                            if (msg.getArg0() == AdbProtocol.AUTH_TYPE_TOKEN) {
                                if (conn.sentSignature) {
                                    packet = AdbProtocol.generateAuth(
                                        AdbProtocol.AUTH_TYPE_RSA_PUBLIC,
                                        conn.crypto.getAdbPublicKeyPayload());
                                    conn.publicKeySent = true;
                                    Log.d(TAG, "AUTH TOKEN recibido -> enviando clave publica RSA (publicKeySent=true)");
                                } else {
                                    packet = AdbProtocol.generateAuth(
                                        AdbProtocol.AUTH_TYPE_SIGNATURE,
                                        conn.crypto.signAdbTokenPayload(msg.getPayload()));
                                    conn.sentSignature = true;
                                    Log.d(TAG, "AUTH TOKEN recibido -> enviando firma");
                                }
                                conn.channel.writex(packet);
                            } else {
                                Log.d(TAG, "AUTH recibido arg0=" + msg.getArg0());
                            }
                            break;

                        case AdbProtocol.CMD_CNXN:
                            synchronized (conn) {
                                conn.maxData = msg.getArg1();
                                conn.connected = true;
                                conn.notifyAll();
                                Log.d(TAG, "CNXN recibido -> conexion ADB ESTABLECIDA");
                            }
                            break;

                        default:
                            break;
                    }
                } catch (java.io.EOFException e) {
                    Log.e(TAG, "connectionThread EOF: conexion cerrada por el remote");
                    break;
                } catch (java.io.IOException e) {
                    Log.e(TAG, "connectionThread IO error: " + e.getMessage());
                    if (e.getMessage() != null && (e.getMessage().contains("closed") || e.getMessage().contains("EOF"))) break;
                    Log.w(TAG, "connectionThread IO recoverable, continuando...");
                    continue;
                } catch (Exception e) {
                    Log.w(TAG, "connectionThread excepcion recoverable: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    continue;
                }
            }

            synchronized (conn) {
                cleanupStreams();
                conn.notifyAll();
                conn.connectAttempted = false;
            }
        });
    }

    public int getMaxData() throws InterruptedException, IOException {
        if (!connectAttempted)
            throw new IllegalStateException("connect() must be called first");
        synchronized (this) {
            if (!connected) wait();
            if (!connected) throw new IOException("Connection failed");
        }
        return maxData;
    }

    public void connect() throws IOException, InterruptedException {
        connect(30000);
    }

    public void connect(long timeoutMs) throws IOException, InterruptedException {
        if (connected)
            throw new IllegalStateException("Already connected");
        Log.d(TAG, "connect() iniciando, timeoutMs=" + timeoutMs);
        channel.writex(AdbProtocol.generateConnect());
        Log.d(TAG, "CNXN enviado, arrancando hilo de conexion");
        connectAttempted = true;
        connectionThread.start();
        synchronized (this) {
            if (!connected) wait(timeoutMs);
            if (!connected) {
                if (publicKeySent) {
                    Log.e(TAG, "connect() fallo: dispositivo NO AUTORIZADO (publicKeySent=true, nunca llego CNXN)");
                    throw new IOException("device unauthorized: accept the debugging prompt on the target device");
                }
                Log.e(TAG, "connect() fallo: TIMEOUT esperando CNXN (publicKeySent=" + publicKeySent + ")");
                throw new IOException("Connection failed (timeout waiting for device)");
            }
        }
        Log.d(TAG, "connect() exitoso");
    }

    public AdbStream open(String destination) throws IOException, InterruptedException {
        int localId = ++lastLocalId;
        Log.d(TAG, "open('" + destination + "') localId=" + localId);
        if (!connectAttempted)
            throw new IllegalStateException("connect() must be called first");

        synchronized (this) {
            if (!connected) wait();
            if (!connected) throw new IOException("Connection failed");
        }

        AdbStream stream = new AdbStream(this, localId);
        openStreams.put(localId, stream);
        channel.writex(AdbProtocol.generateOpen(localId, destination));

        synchronized (stream) {
            stream.wait(10000);
        }

        if (stream.isClosed() && stream.getRemoteId() == 0) {
            Log.e(TAG, "open('" + destination + "') -> TIMEOUT o REMOTE REFUSED");
            openStreams.remove(localId);
            throw new ConnectException("Stream open timed out or rejected by remote peer");
        }

        Log.d(TAG, "open('" + destination + "') -> OK");
        return stream;
    }

    private void cleanupStreams() {
        for (AdbStream s : openStreams.values()) {
            try {
                s.close();
            } catch (IOException e) {}
        }
        openStreams.clear();
    }

    @Override
    public void close() throws IOException {
        if (connectionThread == null) return;
        channel.close();
        connectionThread.interrupt();
        try {
            connectionThread.join();
        } catch (InterruptedException e) { }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isConnectionThreadAlive() {
        return connectionThread != null && connectionThread.isAlive();
    }

    public boolean isHealthy() {
        return connected && isConnectionThreadAlive();
    }
}
