package com.igexin.log.restapi.entity;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.util.DateTimeUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.springframework.data.annotation.Id;

public class LogLine {

    public static final int STATE_NORMAL = 0x01;

    /**
     * 上传到 graylog 失败
     */
    public static final int STATE_FAILED = 0x02;

    /**
     * 上传到 graylog 成功
     */
    public static final int STATE_SUCCESSFUL = 0x03;

    @Id
    private String id;

    private String platform;

    private String uid;

    private String appId;

    /**
     * Logger name
     */
    private String loggerName;

    private long timestamp;

    private String tag;

    private String message;

    /**
     * Log message layout template
     */
    private String msgLayout;

    private int level;

    /**
     * 是否上传到 graylog
     */
    private int status;

    /**
     * 别名
     */
    private String alias;

    private String attachment;

    public static LogLine create(String platform, String uid, String appId,
                                 String loggerName, String msgLayout,
                                 int level, long timestamp, String tag,
                                 String msg, String alias, String attachment) {
        LogLine logLine = new LogLine();
        logLine.setPlatform(platform);
        logLine.setUid(uid);
        logLine.setAppId(appId);
        logLine.setLoggerName(loggerName);
        logLine.setMsgLayout(msgLayout);
        logLine.setLevel(level);
        logLine.setTimestamp(timestamp);
        logLine.setTag(tag);
        logLine.setMessage(msg);
        logLine.setStatus(STATE_NORMAL);
        logLine.setAlias(alias);
        logLine.setAttachment(attachment);
        return logLine;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
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

    public String getMsgLayout() {
        return msgLayout;
    }

    public void setMsgLayout(String msgLayout) {
        this.msgLayout = msgLayout;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public LogLine setLevel(int level) {
        this.level = level;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LogLine setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public LogLine setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public LogLine setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        String temp;
        if (StringUtil.isEmpty(attachment)) {
            temp = String.format("%s%s%s%s%s",
                    DateTimeUtil.timeString(timestamp),
                    Constants.LOG_LINE_SEPARATOR,
                    tag,
                    Constants.LOG_LINE_SEPARATOR,
                    message);
        } else {
            temp = String.format("%s%s%s%s%s%s%s",
                    DateTimeUtil.timeString(timestamp),
                    Constants.LOG_LINE_SEPARATOR,
                    tag,
                    Constants.LOG_LINE_SEPARATOR,
                    message,
                    Constants.LOG_LINE_SEPARATOR,
                    attachment);
        }
        return temp.replaceAll(System.getProperty("line.separator"), Constants.NEW_LINE_CHARACTER);
    }
}
