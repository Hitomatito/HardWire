package com.hitomatito.hardwire.adblib;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class UsbChannel implements AdbChannel {
    private final UsbDeviceConnection mDeviceConnection;
    private final UsbEndpoint mEndpointOut;
    private final UsbEndpoint mEndpointIn;
    private final UsbInterface mInterface;
    private final int defaultTimeout = 1000;

    private final ConcurrentLinkedDeque<UsbRequest> mInRequestPool = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<UsbRequest, ByteBuffer> mPendingReads = new ConcurrentHashMap<>();

    public UsbChannel(UsbDeviceConnection connection, UsbInterface intf) {
        mDeviceConnection = connection;
        mInterface = intf;

        UsbEndpoint epOut = null;
        UsbEndpoint epIn = null;
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;
                }
            }
        }
        if (epOut == null || epIn == null) {
            throw new IllegalArgumentException("not all endpoints found");
        }
        mEndpointOut = epOut;
        mEndpointIn = epIn;
    }

    private UsbRequest acquireRequest() {
        UsbRequest request = mInRequestPool.pollFirst();
        if (request != null) {
            return request;
        }
        request = new UsbRequest();
        request.initialize(mDeviceConnection, mEndpointIn);
        return request;
    }

    private void releaseRequest(UsbRequest request) {
        mInRequestPool.addLast(request);
    }

    @Override
    @java.lang.SuppressWarnings("NewApi")
    public void readx(byte[] buffer, int length) throws IOException {
        UsbRequest usbRequest = acquireRequest();
        ByteBuffer expected = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        mPendingReads.put(usbRequest, expected);

        if (!usbRequest.queue(expected)) {
            mPendingReads.remove(usbRequest);
            releaseRequest(usbRequest);
            throw new IOException("fail to queue read UsbRequest");
        }

        while (true) {
            UsbRequest wait = mDeviceConnection.requestWait();
            if (wait == null) {
                throw new IOException("Connection.requestWait return null");
            }

            ByteBuffer clientData = mPendingReads.remove(wait);

            if (wait.getEndpoint() == mEndpointOut) {
                // write completed, ignore
            } else if (clientData != null && expected == clientData) {
                releaseRequest(wait);
                break;
            } else {
                throw new IOException("unexpected behavior");
            }
        }
        expected.flip();
        expected.get(buffer);
    }

    private void writex(byte[] buffer) throws IOException {
        int offset = 0;
        int transferred;
        byte[] tmp = new byte[buffer.length];
        System.arraycopy(buffer, 0, tmp, 0, buffer.length);

        while ((transferred = mDeviceConnection.bulkTransfer(mEndpointOut, tmp, buffer.length - offset, defaultTimeout)) >= 0) {
            offset += transferred;
            if (offset >= buffer.length) {
                break;
            } else {
                System.arraycopy(buffer, offset, tmp, 0, buffer.length - offset);
            }
        }
        if (transferred < 0) {
            throw new IOException("bulk transfer fail");
        }
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
        for (UsbRequest req : mInRequestPool) {
            req.close();
        }
        mInRequestPool.clear();
        mPendingReads.clear();
        mDeviceConnection.releaseInterface(mInterface);
        mDeviceConnection.close();
    }
}
