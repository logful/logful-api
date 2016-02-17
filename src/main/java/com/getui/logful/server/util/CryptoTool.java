package com.getui.logful.server.util;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.Certificate;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class CryptoTool {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoTool.class);

    private static final byte[] errorBytes = new byte[]{0x00, 0x00};

    private static final String KEY_SUFFIX = "A8P20vWlvfSu3JMO6tBjgr05UvjHAh2x";

    private static Cipher cipher;

    private static ConcurrentHashMap<String, Certificate> keyMap = new ConcurrentHashMap<>();

    private static Cipher cbcCipher;

    @Deprecated
    public synchronized static String AESDecrypt(String appId, byte[] data) {
        if (StringUtils.isEmpty(appId) || data == null) {
            return null;
        }

        if (ArrayUtils.isEquals(data, errorBytes)) {
            return null;
        }

        if (ArrayUtils.isEquals(data, Constants.CRYPTO_ERROR.getBytes())) {
            return null;
        }

        try {
            Certificate certificate = keyMap.get(appId);
            if (certificate == null) {
                byte[] key = Base64.encodeBase64((appId + KEY_SUFFIX).getBytes());
                certificate = security(new String(key), key.length);
                if (certificate != null) {
                    keyMap.put(appId, certificate);
                }
            }

            if (certificate == null) {
                return null;
            }

            String cipherText = new String(Base64.decodeBase64(data), "UTF-8");
            String[] parts = cipherText.split("__");
            if (parts.length != 2) {
                return null;
            }

            byte[] cipherData = Base64.decodeBase64(parts[1].getBytes());

            if (cbcCipher == null) {
                cbcCipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            }

            byte[] newKey = Arrays.copyOf(certificate.getKey(), 32);

            SecretKeySpec aesKey = new SecretKeySpec(newKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(certificate.getIv());
            cbcCipher.init(Cipher.DECRYPT_MODE, aesKey, iv);

            byte[] out = new byte[cbcCipher.getOutputSize(cipherData.length)];
            int length = cbcCipher.update(cipherData, 0, cipherData.length, out, 0);
            length += cbcCipher.doFinal(out, length);

            return new String(out, 0, length, "UTF-8");
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        return null;
    }

    public synchronized static String AESDecrypt(byte[] security, byte[] data) {
        if (security == null || data == null) {
            return null;
        }

        if (ArrayUtils.isEquals(data, errorBytes)) {
            return null;
        }

        if (ArrayUtils.isEquals(data, Constants.CRYPTO_ERROR.getBytes())) {
            return null;
        }

        try {
            if (cipher == null) {
                cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
            }
            SecretKeySpec key = new SecretKeySpec(security, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] out = new byte[cipher.getOutputSize(data.length)];
            int length = cipher.update(data, 0, data.length, out, 0);
            length += cipher.doFinal(out, length);
            return new String(out, 0, length, "UTF-8");
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
        return null;
    }

    static {
        String path = System.getProperty("user.dir");
        if (SystemUtils.IS_OS_LINUX) {
            System.load(path + "/" + "liblogful.so");
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            System.load(path + "/" + "liblogful.dylib");
        }
    }

    public static native Certificate security(String keyStr, int keyLen);
}
