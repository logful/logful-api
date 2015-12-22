package com.getui.logful.server.util;

public class VersionUtil {

    public static final int CRYPTO_V1 = 0x01;

    public static final int CRYPTO_V2 = 0x02;

    public enum Version {
        V1, V2
    }

    public static Version version(String versionString) {
        // TODO
        return Version.V1;
    }

}
