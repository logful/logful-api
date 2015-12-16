package com.getui.logful.server.util;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.*;

public class RSAUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RSAUtil.class);

    private static KeyPairGenerator generator;

    private static Cipher encryptCipher;

    private static Cipher decryptCipher;

    public static KeyPair generateKeyPair() throws Exception {
        if (generator == null) {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
        }
        return generator.generateKeyPair();
    }

    public static String keyToString(Key key) {
        return Base64.encodeBase64String(key.getEncoded());
    }

    public static byte[] encrypt(byte[] data, PublicKey key) throws Exception {
        if (encryptCipher == null) {
            encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        }
        return encryptCipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, PrivateKey key) throws Exception {
        if (decryptCipher == null) {
            decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        }
        return decryptCipher.doFinal(data);
    }
}
