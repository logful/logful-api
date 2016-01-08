package com.getui.logful.server.parse;

import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocalFileSender implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSender.class);

    private BufferedWriter writer;

    public static LocalFileSender create(final LogFileProperties properties) {
        File weedDir = new File(properties.weedPath());
        if (!weedDir.exists()) {
            if (!weedDir.mkdirs()) {
                return null;
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(properties.outFilePath());
            return new LocalFileSender(new BufferedWriter(fileWriter));
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
        return null;
    }

    public LocalFileSender(BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public void send(LogMessage message) {
        if (writer != null) {
            try {
                writer.write(DateTimeUtil.timeString(message.getTimestamp()) +
                        " [" + message.getTag() + "]:" +
                        " " + message.getMessage());
                writer.newLine();
            } catch (IOException e) {
                LOG.error("Exception", e);
            }
        }
    }

    @Override
    public void release() throws Exception {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

}
