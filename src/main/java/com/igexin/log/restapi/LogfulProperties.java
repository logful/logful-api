package com.igexin.log.restapi;

import com.igexin.log.restapi.util.StringUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "logful")
public class LogfulProperties {

    private String path;

    private Parser parser;

    private Weed weed;

    private Graylog graylog;

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

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

    public long ttlSeconds() {
        return getWeed().getTtlSeconds();
    }

    public static class Parser {

        private int maxThreads;

        private int queueCapacity;

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Weed {

        private Master master;

        private int connectTimeout;

        private int reconnectDelay;

        private int queueCapacity;

        private String ttl;

        private long ttlSeconds;

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReconnectDelay() {
            return reconnectDelay;
        }

        public void setReconnectDelay(int reconnectDelay) {
            this.reconnectDelay = reconnectDelay;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getTtl() {
            return ttl;
        }

        public void setTtl(String ttl) {
            this.ttlSeconds = StringUtil.weedTTLToSecond(ttl);
            this.ttl = ttl;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
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

        private int connectTimeout;

        private int reconnectDelay;

        private int queueCapacity;

        private int sendBufferSize;

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReconnectDelay() {
            return reconnectDelay;
        }

        public void setReconnectDelay(int reconnectDelay) {
            this.reconnectDelay = reconnectDelay;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getSendBufferSize() {
            return sendBufferSize;
        }

        public void setSendBufferSize(int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
        }

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
