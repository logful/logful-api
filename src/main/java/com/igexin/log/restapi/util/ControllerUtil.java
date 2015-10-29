package com.igexin.log.restapi.util;

public class ControllerUtil {

    public static final String HEADER = "Accept=application/json";
    public static final String CONTENT_TYPE = "application/json";

    public static boolean checkPlatform(String platform) {
        return !(platform == null || platform.length() == 0)
                && !(!platform.equalsIgnoreCase("iOS")
                && !platform.equalsIgnoreCase("android"));
    }

    public static boolean isEmpty(String param) {
        return param == null || param.length() == 0;
    }

}
