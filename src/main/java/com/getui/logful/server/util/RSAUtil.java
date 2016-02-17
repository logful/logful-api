package com.getui.logful.server.util;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RSAUtil.class);

    private static KeyPairGenerator generator;

    private static KeyFactory keyFactory;

    private static Cipher encryptCipher;

    private static Cipher decryptCipher;

    public static KeyPair generateKeyPair() throws Exception {
        if (generator == null) {
            generator = KeyPairGenerator.getInstance("RSA");
        }
        generator.initialize(2048);
        return generator.genKeyPair();
    }

    public static PublicKey publicKey(Object object) throws Exception {
        if (keyFactory == null) {
            keyFactory = KeyFactory.getInstance("RSA");
        }
        return keyFactory.generatePublic(new X509EncodedKeySpec((byte[]) object));
    }

    public static PrivateKey privateKey(Object object) throws Exception {
        if (keyFactory == null) {
            keyFactory = KeyFactory.getInstance("RSA");
        }
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec((byte[]) object));
    }

    public static String keyToString(Key key) {
        return Base64.encodeBase64String(key.getEncoded());
    }

    // RSA/ECB/PKCS1Padding equals RSA/None/PKCS1Padding
    public static byte[] encrypt(byte[] data, PublicKey key) throws Exception {
        if (encryptCipher == null) {
            encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        return encryptCipher.doFinal(data);
    }

    public synchronized static byte[] decrypt(byte[] data, PrivateKey key) throws Exception {
        if (decryptCipher == null) {
            decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
        return decryptCipher.doFinal(data);
    }
}
