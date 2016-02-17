package com.getui.logful.server.system;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.getui.logful.server.system.jvm.JvmStats;
import com.getui.logful.server.system.os.OsStats;
import com.getui.logful.server.system.process.ProcessStats;

@JsonAutoDetect
public class SystemStats {

    @JsonProperty("jvm")
    public JvmStats jvmStats;

    @JsonProperty("os")
    public OsStats osStats;

    @JsonProperty("process")
    public ProcessStats processStats;

    @JsonProperty("weed")
    public WeedFSStats weedFSStats;

    @JsonProperty("graylog")
    public GraylogStats graylogStats;

    public static SystemStats create(JvmStats jvmStats,
                                     OsStats osStats,
                                     ProcessStats processStats,
                                     WeedFSStats weedFSStats,
                                     GraylogStats graylogStats) {
        SystemStats stats = new SystemStats();
        stats.jvmStats = jvmStats;
        stats.osStats = osStats;
        stats.processStats = processStats;
        stats.weedFSStats = weedFSStats;
        stats.graylogStats = graylogStats;
        return stats;
    }

    @JsonAutoDetect
    public static class WeedFSStats {

        @JsonProperty
        public boolean connected;

        @JsonProperty
        public String error;

        public static WeedFSStats create(boolean connected, String error) {
            WeedFSStats stats = new WeedFSStats();
            stats.connected = connected;
            stats.error = error;
            return stats;
        }
    }

    @JsonAutoDetect
    public static class GraylogStats {

        @JsonProperty
        public boolean connected;

        public static GraylogStats create(boolean connected) {
            GraylogStats stats = new GraylogStats();
            stats.connected = connected;
            return stats;
        }
    }
}
