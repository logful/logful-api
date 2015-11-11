package com.igexin.log.restapi.util;

import org.apache.commons.lang.SystemUtils;

public class CryptoTool {

    public static synchronized String AESEncrypt(String appId, String string) {
        return CryptoTool.encrypt(appId, string);
    }

    public static synchronized String AESDecrypt(String appId, String string) {
        return CryptoTool.decrypt(appId, string);
    }

    static {
        String path = System.getProperty("user.dir");
        if (SystemUtils.IS_OS_LINUX) {
            System.load(path + "/" + "liblogful.so");
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            System.load(path + "/" + "liblogful.dylib");
        }
    }

    public static native String encrypt(String appId, String msg);

    public static native String decrypt(String appId, String msg);

}
