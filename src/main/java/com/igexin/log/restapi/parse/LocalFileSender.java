package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.GlobalReference;
import com.igexin.log.restapi.entity.LogFileProperties;
import com.igexin.log.restapi.entity.LogLine;

import java.io.*;

public class LocalFileSender implements SenderInterface {

    private OutputStream outputStream;

    public static LocalFileSender create(LogFileProperties properties) {
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
            e.printStackTrace();
        }
        if (outputStream != null) {
            localFileSender.setOutputStream(outputStream);
        }
        return localFileSender;
    }

    private OutputStream createOutputStream(LogFileProperties properties) {
        String decryptedDir = GlobalReference.properties().decryptedDir(properties.getPlatform());
        String dirPath = decryptedDir + "/" + properties.getAppId() + "/" + properties.getUid();
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean successful = dir.mkdirs();
            if (!successful) {
                return null;
            }
        }
        String filePath = dirPath + "/" + properties.getOriginalFilename();
        try {
            return new FileOutputStream(filePath, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }

    public void write(String line) {
        if (outputStream != null) {
            String data = line + "\n";
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
