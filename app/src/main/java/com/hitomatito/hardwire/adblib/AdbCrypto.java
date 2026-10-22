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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class AdbCrypto {
    private KeyPair keyPair;
    private AdbBase64 base64;

    public static final int KEY_LENGTH_BITS = 2048;
    public static final int KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8;
    public static final int KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4;

    public static final int[] SIGNATURE_PADDING_AS_INT = new int[]{
        0x00,0x01,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0x00,
        0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,
        0x04,0x14
    };

    public static byte[] SIGNATURE_PADDING;

    static {
        SIGNATURE_PADDING = new byte[SIGNATURE_PADDING_AS_INT.length];
        for (int i = 0; i < SIGNATURE_PADDING.length; i++)
            SIGNATURE_PADDING[i] = (byte) SIGNATURE_PADDING_AS_INT[i];
    }

    private static byte[] convertRsaPublicKeyToAdbFormat(RSAPublicKey pubkey) {
        BigInteger r32, r, rr, rem, n, n0inv;
        r32 = BigInteger.ZERO.setBit(32);
        n = pubkey.getModulus();
        r = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32);
        rr = r.modPow(BigInteger.valueOf(2), n);
        rem = n.remainder(r32);
        n0inv = rem.modInverse(r32);

        int[] myN = new int[KEY_LENGTH_WORDS];
        int[] myRr = new int[KEY_LENGTH_WORDS];
        BigInteger[] res;
        for (int i = 0; i < KEY_LENGTH_WORDS; i++) {
            res = rr.divideAndRemainder(r32);
            rr = res[0];
            rem = res[1];
            myRr[i] = rem.intValue();

            res = n.divideAndRemainder(r32);
            n = res[0];
            rem = res[1];
            myN[i] = rem.intValue();
        }

        ByteBuffer bbuf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        bbuf.putInt(KEY_LENGTH_WORDS);
        bbuf.putInt(n0inv.negate().intValue());
        for (int i : myN) bbuf.putInt(i);
        for (int i : myRr) bbuf.putInt(i);
        bbuf.putInt(pubkey.getPublicExponent().intValue());
        return bbuf.array();
    }

    public static AdbCrypto loadAdbKeyPair(AdbBase64 base64, File privateKey, File publicKey)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        AdbCrypto crypto = new AdbCrypto();

        byte[] privKeyBytes = new byte[(int) privateKey.length()];
        byte[] pubKeyBytes = new byte[(int) publicKey.length()];

        try (FileInputStream privIn = new FileInputStream(privateKey);
             FileInputStream pubIn = new FileInputStream(publicKey)) {
            privIn.read(privKeyBytes);
            pubIn.read(pubKeyBytes);
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);

        crypto.keyPair = new KeyPair(
                keyFactory.generatePublic(publicKeySpec),
                keyFactory.generatePrivate(privateKeySpec));
        crypto.base64 = base64;
        return crypto;
    }

    public static AdbCrypto generateAdbKeyPair(AdbBase64 base64) throws NoSuchAlgorithmException {
        AdbCrypto crypto = new AdbCrypto();
        KeyPairGenerator rsaKeyPg = KeyPairGenerator.getInstance("RSA");
        rsaKeyPg.initialize(KEY_LENGTH_BITS);
        crypto.keyPair = rsaKeyPg.genKeyPair();
        crypto.base64 = base64;
        return crypto;
    }

    public byte[] signAdbTokenPayload(byte[] payload) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
        c.update(SIGNATURE_PADDING);
        return c.doFinal(payload);
    }

    public byte[] getAdbPublicKeyPayload() throws IOException {
        byte[] convertedKey = convertRsaPublicKeyToAdbFormat((RSAPublicKey) keyPair.getPublic());
        StringBuilder keyString = new StringBuilder(720);
        keyString.append(base64.encodeToString(convertedKey));
        keyString.append(" unknown@unknown");
        keyString.append('\0');
        return keyString.toString().getBytes("UTF-8");
    }

    public void saveAdbKeyPair(File privateKey, File publicKey) throws IOException {
        try (FileOutputStream privOut = new FileOutputStream(privateKey);
             FileOutputStream pubOut = new FileOutputStream(publicKey)) {
            privOut.write(keyPair.getPrivate().getEncoded());
            pubOut.write(keyPair.getPublic().getEncoded());
        }
    }
}
