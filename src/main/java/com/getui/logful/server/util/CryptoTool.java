package com.getui.logful.server.util;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.Certificate;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class CryptoTool {

    private static final String KEY_SUFFIX = "A8P20vWlvfSu3JMO6tBjgr05UvjHAh2x";

    private static ConcurrentHashMap<String, Certificate> certificates = new ConcurrentHashMap<>();

    public static synchronized String AESDecrypt(String appId, byte[] data, int cryptoVersion) {
        if (cryptoVersion == VersionUtil.CRYPTO_UPDATE_1) {
            return CryptoTool.decrypt(appId, new String(data));
        } else if (cryptoVersion == VersionUtil.CRYPTO_UPDATE_2) {
            try {
                if (StringUtils.equals(new String(data, StandardCharsets.UTF_8), Constants.CRYPTO_ERROR)) {
                    return null;
                }
            } catch (Exception e) {
                // Ignore
            }

            Certificate certificate = certificates.get(appId);
            if (certificate == null) {
                String keyChar = Base64.encodeBase64String((appId + KEY_SUFFIX).getBytes());
                certificate = generateCert(keyChar.length(), keyChar);
                if (certificate != null) {
                    certificates.put(appId, certificate);
                }
            }
            if (certificate != null) {
                return decryptUpdate(certificate.getKey(), certificate.getIv(), data.length, data);
            }
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

    public static native String decrypt(String appId, String msg);

    public static native Certificate generateCert(int keyLen, String keyChar);

    public static native String decryptUpdate(byte[] keyData, byte[] ivData, int length, byte[] data);

}
