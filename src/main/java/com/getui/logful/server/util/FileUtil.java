package com.getui.logful.server.util;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class FileUtil {

    public static void transferTo(MultipartFile multiFile, File targetFile) throws Exception {
        File dir = targetFile.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create dir path!");
            }
        }

        if (!targetFile.exists()) {
            if (!targetFile.createNewFile()) {
                throw new IOException("Create new file failed!");
            }
        }

        InputStream inputStream = multiFile.getInputStream();
        OutputStream outputStream = new FileOutputStream(targetFile);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }

}
