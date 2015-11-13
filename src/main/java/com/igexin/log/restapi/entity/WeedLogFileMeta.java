package com.igexin.log.restapi.entity;

import com.igexin.log.restapi.util.StringUtil;
import org.springframework.data.annotation.Id;

public class WeedLogFileMeta {

    @Id
    private String id;

    private short platform;

    private String uid;

    private String appId;

    private String loggerName;

    private String date;

    private short level;

    private int fragment;

    private String fid;

    private long size;

    public static WeedLogFileMeta create(short platform,
                                         String uid,
                                         String appId,
                                         String loggerName,
                                         String date,
                                         short level,
                                         int fragment) {
        WeedLogFileMeta meta = new WeedLogFileMeta();
        meta.setPlatform(platform);
        meta.setUid(uid);
        meta.setAppId(appId);
        meta.setLoggerName(loggerName);
        meta.setDate(date);
        meta.setLevel(level);
        meta.setFragment(fragment);
        return meta;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String originalFilename() {
        return loggerName + "-" + date + "-" + StringUtil.levelString(level) + "-" + fragment;
    }
}
