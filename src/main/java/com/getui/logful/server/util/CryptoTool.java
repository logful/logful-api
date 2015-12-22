package com.getui.logful.server.util;

import com.getui.logful.server.Constants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class CryptoTool {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoTool.class);

    public static synchronized String AESDecrypt(byte[] security, byte[] data, int version) {
        if (security == null || data == null) {
            return null;
        }

        if (ArrayUtils.isEquals(data, Constants.CRYPTO_ERROR.getBytes())) {
            return null;
        }

        if (version == VersionUtil.CRYPTO_V1) {
            try {
                return CryptoTool.decrypt(new String(security), new String(data));
            } catch (Throwable throwable) {
                LOG.error("Exception", throwable);
                return null;
            }
        } else if (version == VersionUtil.CRYPTO_V2) {
            try {
                byte[] bytes = decryptUpdate(security, data, data.length);
                return new String(removePadding(bytes));
            } catch (Throwable throwable) {
                LOG.error("Exception", throwable);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Remove PKCS#7 padding.
     *
     * @param in Input byte array
     * @return Byte array without padding
     */
    private static byte[] removePadding(byte[] in) {
        if (in == null) {
            return null;
        }

        int len = in.length;
        int count = in[len - 1] & 0xff;
        if (count > len) {
            return null;
        }

        for (int i = 1; i <= count; i++) {
            if (in[len - i] != count) {
                return null;
            }
        }

        return Arrays.copyOfRange(in, 0, len - count);
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

    public static native byte[] decryptUpdate(byte[] key, byte[] data, int length);

}
