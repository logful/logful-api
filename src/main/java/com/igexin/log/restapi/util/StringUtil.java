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

    public static String logFileName(String platform, String uid, String appId, String originalFileName) {
        int index = originalFileName.indexOf(".");
        String[] temp = originalFileName.substring(0, index).split("-");
        if (temp.length != 4) {
            return null;
        }
        String[] array = new String[]{
                String.valueOf(StringUtil.platformNumber(platform)),
                uid,
                appId,
                temp[0],
                temp[1],
                String.valueOf(StringUtil.level(temp[2])),
                temp[3]
        };
        return StringUtils.join(array, "-");
    }

    public static String attachmentName(String platform, String uid, String appId, String attachmentId) {
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
