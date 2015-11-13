package com.igexin.log.restapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "logful")
public class LogfulProperties {

    private String path;

    private Weed weed;

    private Graylog graylog;

    public Graylog getGraylog() {
        return graylog;
    }

    public void setGraylog(Graylog graylog) {
        this.graylog = graylog;
    }

    public Weed getWeed() {
        return weed;
    }

    public void setWeed(Weed weed) {
        this.weed = weed;
    }

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

    public String crashReportDir(String platform) {
        return path + "/" + Constants.CRASH_REPORT_DIR + "/" + platform.toLowerCase();
    }

    public String tempDir() {
        return path + "/" + Constants.LOG_FILE_TEMP_DIR;
    }

    public String weedDir() {
        return path + "/" + Constants.WEED_TEMP_DIR;
    }

    public String graylogHost() {
        return getGraylog().getHost();
    }

    public int graylogPort() {
        return getGraylog().getPort();
    }

    public String weedMasterHost() {
        return getWeed().getMaster().getHost();
    }

    public int weedMasterPort() {
        return getWeed().getMaster().getPort();
    }

    public String weedTimeToLive() {
        return getWeed().getTtl();
    }

    public static class Weed {

        private Master master;

        private String ttl;

        public String getTtl() {
            return ttl;
        }

        public void setTtl(String ttl) {
            this.ttl = ttl;
        }

        public Master getMaster() {
            return master;
        }

        public void setMaster(Master master) {
            this.master = master;
        }

        public static class Master {

            private String host;

            private int port;

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }
        }
    }

    public static class Graylog {

        private String host;

        private int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }

}
