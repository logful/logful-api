package com.getui.logful.server.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;

public class FileUtil {

    /**
     * Copy file.
     *
     * @param inFile  Source file path
     * @param outFile Target file path
     */
    public static void copy(String inFile, String outFile) {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(inFile).getChannel();
            outChannel = new FileOutputStream(outFile).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outChannel != null) {
                try {
                    outChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Merge file.
     *
     * @param outFilePath Output target file path
     * @param inFilePaths Input file paths
     */
    public static boolean merge(String outFilePath, String... inFilePaths) {
        if (StringUtil.isEmpty(outFilePath)) {
            return false;
        }

        if (inFilePaths == null) {
            return false;
        }

        File outFile = new File(outFilePath);
        File parent = outFile.getParentFile();
        if (!parent.exists()) {
            boolean successful = parent.mkdirs();
            if (!successful) {
                return false;
            }
        }

        if (!outFile.exists()) {
            try {
                if (!outFile.createNewFile()) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }

        try {
            FileChannel outFileChannel = new FileOutputStream(outFile, true).getChannel();
            FileChannel inFileChannel;
            for (String inFilePath : inFilePaths) {
                File inFile = new File(inFilePath);
                if (inFile.exists() && inFile.isFile()) {
                    inFileChannel = new FileInputStream(inFile).getChannel();
                    inFileChannel.transferTo(0, inFileChannel.size(), outFileChannel);
                    inFileChannel.close();
                }
            }
            outFileChannel.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean delete(String[] paths) {
        boolean successful = true;

        for (String path : paths) {
            File file = new File(path);
            if (!FileUtils.deleteQuietly(file)) {
                successful = false;
            }
        }
        return successful;
    }

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
