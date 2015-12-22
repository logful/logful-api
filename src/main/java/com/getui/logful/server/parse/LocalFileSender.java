package com.getui.logful.server.parse;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.util.DateTimeUtil;
import com.getui.logful.server.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LocalFileSender implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSender.class);

    private OutputStream outputStream;

    private JsonGenerator generator;

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
        JsonFactory jsonFactory = new JsonFactory();
        try {
            this.generator = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);
            this.generator.setPrettyPrinter(new MinimalPrettyPrinter(""));
        } catch (IOException e) {
            LOG.error("Exception", e);
        }
    }

    @Override
    public void send(LogMessage message) {
        if (outputStream != null && generator != null) {
            try {
                generator.writeStartObject();
                generator.writeStringField("date", DateTimeUtil.timeString(message.getTimestamp()));
                generator.writeStringField("tag", message.getTag());
                generator.writeStringField("msg", message.getMessage());

                if (!StringUtil.isEmpty(message.getAttachment())) {
                    generator.writeStringField("attr", message.getAttachment());
                }

                generator.writeEndObject();

                generator.writeRaw('\n');
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
        if (generator != null) {
            generator.flush();
            generator.close();
        }
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
    }

}
