package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LocalFileSender implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSender.class);

    private OutputStream outputStream;

    private String filename;

    public static LocalFileSender create(final LogFileProperties properties) {
        LocalFileSender localFileSender = new LocalFileSender();
        OutputStream outputStream = localFileSender.createOutputStream(properties);
        if (outputStream != null) {
            localFileSender.setOutputStream(outputStream);
        }
        return localFileSender;
    }

    public static LocalFileSender create(String filePath) {
        LocalFileSender localFileSender = new LocalFileSender();
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath, true);
        } catch (FileNotFoundException e) {
            LOG.error("Exception", e);
        }
        if (outputStream != null) {
            localFileSender.setOutputStream(outputStream);
        }
        return localFileSender;
    }

    private OutputStream createOutputStream(LogFileProperties properties) {
        File tempDir = new File(properties.tempPath());
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                return null;
            }
        }

        filename = properties.outputFilename();
        if (StringUtil.isEmpty(filename)) {
            return null;
        }

        String filePath = properties.tempPath() + "/" + filename;
        try {
            return new FileOutputStream(filePath, true);
        } catch (FileNotFoundException e) {
            LOG.error("Exception", e);
        }
        return null;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void send(LogLine logLine) {
        if (outputStream != null) {
            String line = logLine.toString() + "\n";
            try {
                outputStream.write(line.getBytes());
            } catch (IOException e) {
                LOG.error("Exception", e);
            }
        }
    }

    public void write(String line) {
        if (outputStream != null) {
            String data = line + "\n";
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                LOG.error("Exception", e);
            }
        }
    }

    @Override
    public void release() {
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                LOG.error("Exception", e);
            }
        }
    }

    public String getFilename() {
        return filename;
    }

}
