package com.igexin.log.restapi.util;

import com.igexin.log.restapi.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class CryptoTool {

    private static final String KEY_SUFFIX = "A8P20vWlvfSu3JMO6tBjgr05UvjHAh2x";

    private static ConcurrentHashMap<String, String> keyMap = new ConcurrentHashMap<>();

    public static synchronized String AESDecrypt(String appId, byte[] data, int cryptoVersion) {
        if (cryptoVersion == VersionUtil.CRYPTO_UPDATE_1) {
            return CryptoTool.decrypt(appId, new String(data));
        } else if (cryptoVersion == VersionUtil.CRYPTO_UPDATE_2) {
            try {
                if (StringUtils.equals(new String(data, StandardCharsets.UTF_8), Constants.CRYPTO_ERROR)) {
                    return Constants.CRYPTO_ERROR;
                }
            } catch (Exception e) {
                // Ignore
            }

            int length = data.length;

            String keyChar = keyMap.get(appId);
            if (keyChar == null) {
                String concatenate = appId + KEY_SUFFIX;
                keyChar = Base64.encodeBase64String(concatenate.getBytes());
                keyMap.put(appId, keyChar);
            }
            return decryptUpdate(keyChar.length(), keyChar, length, data);
        } else {
            return null;
        }
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

    public static native String decryptUpdate(int keyLen, String keyChar, int length, byte[] data);

}
