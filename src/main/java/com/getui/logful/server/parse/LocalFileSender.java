package com.getui.logful.server.parse;

import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.util.DateTimeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocalFileSender implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSender.class);

    private BufferedWriter writer;

    private StringBuilder builder;

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
        this.builder = new StringBuilder();
    }

    @Override
    public void send(LogMessage message) {
        if (writer != null) {
            try {
                builder.setLength(0);
                builder.append(DateTimeUtil.timeString(message.getTimestamp()));
                builder.append(" [");
                builder.append(message.getTag());
                builder.append("]: ");
                builder.append(message.getMessage());
                if (StringUtils.isNotEmpty(message.getAttachment())) {
                    builder.append(" attachment<<");
                    builder.append(message.getAttachment());
                    builder.append(">>");
                }
                writer.write(builder.toString());
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
