package com.igexin.log.restapi.util;

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

    public static String attachmentName(String platform, String uid, String appId, String attachmentId) {
        String temp = String.format("%s-%s-%s-%s",
                platform.toLowerCase(),
                uid.toLowerCase(),
                appId.toLowerCase(),
                attachmentId);
        return Checksum.md5(temp);
    }
}
