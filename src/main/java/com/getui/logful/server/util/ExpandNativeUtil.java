package com.getui.logful.server.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ExpandNativeUtil {

    public static void expand() {
        ExpandNativeUtil util = new ExpandNativeUtil();
        util.expandLibrary();
    }

    public void expandLibrary() {
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
            } else {
                throw new RuntimeException("Create native lib file failed.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Copy native lib failed.", e);
        }
    }

}
