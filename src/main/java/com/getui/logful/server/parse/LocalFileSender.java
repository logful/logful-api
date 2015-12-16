package com.getui.logful.server.parse;

import com.getui.logful.server.entity.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LocalFileSender implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSender.class);

    private OutputStream outputStream;

    private LogFileProperties properties;

    public LogFileProperties getProperties() {
        return properties;
    }

    public static LocalFileSender create(final LogFileProperties properties) {
        LocalFileSender localFileSender = new LocalFileSender();
        localFileSender.properties = properties;
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
        File weedDir = new File(properties.weedPath());
        if (!weedDir.exists()) {
            if (!weedDir.mkdirs()) {
                return null;
            }
        }
        try {
            return new FileOutputStream(properties.outFilePath(), true);
        } catch (FileNotFoundException e) {
            LOG.error("Exception", e);
        }
        return null;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void send(LogMessage logMessage) {
        if (outputStream != null) {
            String line = logMessage.text() + "\n";
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
    public void release() throws Exception {
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
    }

}
