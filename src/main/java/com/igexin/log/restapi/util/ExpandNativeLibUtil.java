package com.igexin.log.restapi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.*;

public class ExpandNativeLibUtil {

    private static class ClassHolder {
        static ExpandNativeLibUtil util = new ExpandNativeLibUtil();
    }

    public static ExpandNativeLibUtil util() {
        return ClassHolder.util;
    }

    public void expand() {
        try {
            String path = System.getProperty("user.dir");
            InputStream inputStream;
            File libFile;
            if (SystemUtils.IS_OS_LINUX) {
                inputStream = getClass().getResourceAsStream("/native/linux/liblogful.so");
                libFile = new File(path + "/" + "liblogful.so");
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                inputStream = getClass().getResourceAsStream("/native/darwin/liblogful.dylib");
                libFile = new File(path + "/" + "liblogful.dylib");
            } else {
                throw new RuntimeException("Unknown os.");
            }
            if (libFile.exists() && libFile.isFile()) {
                if (!libFile.delete()) {
                    throw new RuntimeException("Delete exist native lib failed.");
                }
            }
            if (libFile.createNewFile()) {
                OutputStream outputStream = new FileOutputStream(libFile);
                IOUtils.copy(inputStream, outputStream);
                inputStream.close();
                outputStream.close();
                System.setProperty("java.library.path", path);
            } else {
                throw new RuntimeException("Create native lib file failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Copy native lib failed.", e);
        }
    }

}
