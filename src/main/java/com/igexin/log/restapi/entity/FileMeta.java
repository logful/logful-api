package com.igexin.log.restapi.entity;

public class FileMeta {

    private short platform;

    private String uid;

    private String appId;

    private String loggerName;

    private String dateString;

    private short level;

    private int fragment;

    private String host;

    private short port;

    private String fid;

    private long size;

    public static FileMeta create(short platform,
                                  String uid,
                                  String appId,
                                  String loggerName,
                                  String dateString,
                                  short level,
                                  int fragment,
                                  String host,
                                  short port,
                                  String fid,
                                  long size) {
        FileMeta meta = new FileMeta();
        meta.setPlatform(platform);
        meta.setUid(uid);
        meta.setAppId(appId);
        meta.setLoggerName(loggerName);
        meta.setDateString(dateString);
        meta.setLevel(level);
        meta.setFragment(fragment);
        meta.setHost(host);
        meta.setPort(port);
        meta.setFid(fid);
        meta.setSize(size);
        return meta;
    }

    public short getPlatform() {
        return platform;
    }

    public void setPlatform(short platform) {
        this.platform = platform;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public short getLevel() {
        return level;
    }

    public void setLevel(short level) {
        this.level = level;
    }

    public int getFragment() {
        return fragment;
    }

    public void setFragment(int fragment) {
        this.fragment = fragment;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
