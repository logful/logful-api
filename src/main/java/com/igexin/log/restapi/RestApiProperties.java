package com.igexin.log.restapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "logful")
public class RestApiProperties {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String cacheDir() {
        return path + "/" + Constants.CACHE_DIR;
    }

    public String errorDir(String platform) {
        return path + "/" + Constants.ERROR_DIR + "/" + platform.toLowerCase();
    }

    public String decryptedDir(String platform) {
        return path + "/" + Constants.DECRYPTED_DIR + "/" + platform.toLowerCase();
    }

    public String crashReportDir(String platform) {
        return path + "/" + Constants.CRASH_REPORT_DIR + "/" + platform.toLowerCase();
    }

    public String tempDir() {
        return path + "/" + Constants.LOG_FILE_TEMP_DIR;
    }

    public String attachmentDir() {
        return path + "/" + Constants.ATTACHMENT_DIR;
    }

}
