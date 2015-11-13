package com.igexin.log.restapi.util;

import com.igexin.log.restapi.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.UUID;

public class StringUtil {

    public static boolean isEmpty(String string) {
        return !(string != null && string.length() > 0);
    }

    public static String randomUid() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    public static int level(String levelString) {
        if (levelString.equalsIgnoreCase("verbose")) {
            return 1;
        }
        if (levelString.equalsIgnoreCase("debug")) {
            return 2;
        }
        if (levelString.equalsIgnoreCase("info")) {
            return 3;
        }
        if (levelString.equalsIgnoreCase("warn")) {
            return 4;
        }
        if (levelString.equalsIgnoreCase("error")) {
            return 5;
        }
        if (levelString.equalsIgnoreCase("exception")) {
            return 6;
        }
        if (levelString.equalsIgnoreCase("fatal")) {
            return 7;
        }
        return 1;
    }

    public static String levelString(int level) {
        if (level == 1) {
            return "verbose";
        }
        if (level == 2) {
            return "debug";
        }
        if (level == 3) {
            return "info";
        }
        if (level == 4) {
            return "warn";
        }
        if (level == 5) {
            return "error";
        }
        if (level == 6) {
            return "exception";
        }
        if (level == 7) {
            return "fatal";
        }
        return "verbose";
    }

    public static int platformNumber(String platformString) {
        if (platformString.equalsIgnoreCase("android")) {
            return Constants.PLATFORM_ANDROID;
        }
        if (platformString.equalsIgnoreCase("iOS")) {
            return Constants.PLATFORM_IOS;
        }
        return 0;
    }

    public static String platformString(int platform) {
        if (platform == Constants.PLATFORM_ANDROID) {
            return "android";
        }
        if (platform == Constants.PLATFORM_IOS) {
            return "iOS";
        }
        return "unknown";
    }

    public static String attachmentKey(String platform, String uid, String appId, String attachmentId) {
        String temp = String.format("%s-%s-%s-%s",
                platform.toLowerCase(),
                uid.toLowerCase(),
                appId.toLowerCase(),
                attachmentId);
        return Checksum.md5(temp);
    }

    public static boolean decryptError(String message) {
        return isEmpty(message) || StringUtils.equals(message, Constants.CRYPTO_ERROR);
    }
}
