package com.igexin.log.restapi.util;

import com.igexin.log.restapi.Constants;

public class CryptoTool {

    public static synchronized String AESEncrypt(String appId, String string) {
        return CryptoTool.encrypt(appId, string);
    }

    public static synchronized String AESDecrypt(String appId, String string) {
        return CryptoTool.decrypt(appId, string);
    }

    static {
        System.load(Constants.JNI_LIBRARY_PATH);
    }

    public static native String encrypt(String appId, String msg);

    public static native String decrypt(String appId, String msg);

}
