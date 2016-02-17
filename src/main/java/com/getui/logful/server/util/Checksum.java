package com.getui.logful.server.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Checksum {

    private static final Logger LOG = LoggerFactory.getLogger(Checksum.class);

    public static String fileMD5(String filePath) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(new File(filePath));
            String md5 = DigestUtils.md5Hex(stream);
            stream.close();
            return md5;
        } catch (Exception e) {
            LOG.error("Exception", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    LOG.error("Exception", e);
                }
            }
        }
        return "";
    }
}
