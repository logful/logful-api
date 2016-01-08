package com.getui.logful.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.getui.logful.server.util.StringUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "log_file_meta")
public class LogFileMeta {

    @Id
    private String id;

    private short platform;
    private String clientId;
    private String uid;
    private String appId;
    private String loggerName;
    private Date date;
    private short level;
    private int fragment;
    private String fid;
    private long size;
    @JsonIgnore
    private Date writeDate;

    public static LogFileMeta create(short platform,
                                     String clientId,
                                     String uid,
                                     String appId,
                                     String loggerName,
                                     Date date,
                                     short level,
                                     int fragment) {
        LogFileMeta meta = new LogFileMeta();
        meta.setPlatform(platform);
        meta.setClientId(clientId);
        meta.setUid(uid);
        meta.setAppId(appId);
        meta.setLoggerName(loggerName);
        meta.setDate(date);
        meta.setLevel(level);
        meta.setFragment(fragment);
        meta.setWriteDate(new Date());
        return meta;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getWriteDate() {
        return writeDate;
    }

    public void setWriteDate(Date writeDate) {
        this.writeDate = writeDate;
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
