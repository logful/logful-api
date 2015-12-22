package com.getui.logful.server.util;

import com.getui.logful.server.Constants;
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

    public static String attachmentKey(int platform, String uid, String appId, String attachmentId) {
        String temp = String.valueOf(platform) + "-" + uid + "-" + appId.toLowerCase() + "-" + attachmentId;
        return Checksum.md5(temp);
    }

    public static boolean decryptError(String message) {
        return isEmpty(message) || StringUtils.equals(message, Constants.CRYPTO_ERROR);
    }

    public static long weedTTLToSecond(String ttl) {
        if (StringUtil.isEmpty(ttl)) {
            throw new IllegalArgumentException("No ttl specify!");
        }
        long seconds;
        int length = ttl.length();
        try {
            int value = Integer.parseInt(ttl.substring(0, length - 1));
            String unit = ttl.substring(length - 1, length);
            switch (unit) {
                case "m":
                    seconds = value * 60;
                    break;
                case "h":
                    seconds = value * 60 * 60;
                    break;
                case "d":
                    seconds = value * 24 * 60 * 60;
                    break;
                case "w":
                    seconds = value * 7 * 24 * 60 * 60;
                    break;
                case "M":
                    seconds = value * 30 * 24 * 60 * 60;
                    break;
                case "y":
                    seconds = value * 365 * 24 * 60 * 60;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown unit!");
            }
            return seconds;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value format error!");
        }
    }
}
