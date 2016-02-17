package com.getui.logful.server.entity;

import com.getui.logful.server.Constants;
import com.getui.logful.server.util.StringUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "log_message")
public class LogMessage {

    public static final int STATE_NORMAL = 0x01;

    public static final int STATE_SUCCESSFUL = 0x02;

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

    private Date writeDate;

    public LogMessage() {
        this.status = STATE_NORMAL;
    }

    public static LogMessage create(int platform, String uid, String appId,
                                    String loggerName, String msgLayout,
                                    int level, long timestamp, String tag,
                                    String msg, String alias, String attachment) {
        LogMessage logMessage = new LogMessage();
        logMessage.setPlatform(StringUtil.platformString(platform));
        logMessage.setUid(uid);
        logMessage.setAppId(appId);
        logMessage.setLoggerName(loggerName);
        logMessage.setMsgLayout(msgLayout);
        logMessage.setLevel(level);
        logMessage.setTimestamp(timestamp);
        logMessage.setTag(tag);
        logMessage.setMessage(msg);
        logMessage.setStatus(STATE_NORMAL);
        logMessage.setAlias(alias);
        logMessage.setAttachment(attachment);
        logMessage.setWriteDate(new Date());
        return logMessage;
    }

    public static LogMessage create(CrashFileMeta fileMeta) {
        return LogMessage.create(fileMeta.getPlatform(), fileMeta.getUid(),
                fileMeta.getAppId(), "crash", null, Constants.FATAL,
                fileMeta.getDate().getTime(), "CRASH_FILE_ID",
                fileMeta.getId(), fileMeta.getAlias(), null);
    }

    public Date getWriteDate() {
        return writeDate;
    }

    public void setWriteDate(Date writeDate) {
        this.writeDate = writeDate;
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

    public LogMessage setLevel(int level) {
        this.level = level;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LogMessage setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public LogMessage setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public LogMessage setMessage(String message) {
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
}
