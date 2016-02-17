package com.getui.logful.server.util;

public class VersionUtil {

    public static final int API_VERSION_V1 = 0x01;

    public static final int API_VERSION_V2 = 0x02;

    public static int version(String versionString) {
        return API_VERSION_V1;
    }

}
