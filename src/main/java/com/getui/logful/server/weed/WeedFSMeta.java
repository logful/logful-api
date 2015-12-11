package com.getui.logful.server.weed;

import com.getui.logful.server.entity.WeedAttachFileMeta;
import com.getui.logful.server.entity.WeedLogFileMeta;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "weed_write_queue")
public class WeedFSMeta {

    public static final int TYPE_LOG = 0x01;

    public static final int TYPE_ATTACHMENT = 0x02;

    public static final int STATE_NORMAL = 0x01;

    public static final int STATE_DELETED = 0x02;

    public static final int STATE_SUCCESSFUL = 0x03;

    @Id
    private String id;

    private int type;

    private String key;

    private String extension;

    private int status;

    private Date writeDate;

    private byte[] response;

    private WeedLogFileMeta logFileMeta;

    private WeedAttachFileMeta attachFileMeta;

    public WeedFSMeta() {
        this.status = STATE_NORMAL;
        this.writeDate = new Date();
    }

    public static WeedFSMeta create(String key, String extension, WeedLogFileMeta meta) {
        WeedFSMeta weedFSMeta = new WeedFSMeta();
        weedFSMeta.type = TYPE_LOG;
        weedFSMeta.extension = extension;
        weedFSMeta.key = key;
        weedFSMeta.logFileMeta = meta;
        return weedFSMeta;
    }

    public static WeedFSMeta create(String key, String extension, WeedAttachFileMeta meta) {
        WeedFSMeta weedFSMeta = new WeedFSMeta();
        weedFSMeta.type = TYPE_ATTACHMENT;
        weedFSMeta.key = key;
        weedFSMeta.extension = extension;
        weedFSMeta.attachFileMeta = meta;
        return weedFSMeta;
    }

    public static WeedFSMeta create(byte[] response) {
        WeedFSMeta weedFSMeta = new WeedFSMeta();
        weedFSMeta.response = response;
        return weedFSMeta;
    }

    public Date getWriteDate() {
        return writeDate;
    }

    public void setWriteDate(Date writeDate) {
        this.writeDate = writeDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public WeedLogFileMeta getLogFileMeta() {
        return logFileMeta;
    }

    public void setLogFileMeta(WeedLogFileMeta logFileMeta) {
        this.logFileMeta = logFileMeta;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public WeedAttachFileMeta getAttachFileMeta() {
        return attachFileMeta;
    }

    public void setAttachFileMeta(WeedAttachFileMeta attachFileMeta) {
        this.attachFileMeta = attachFileMeta;
    }

    public String filename() {
        return key + "." + extension;
    }

    public JSONObject responseObject() {
        if (response != null && response.length > 0) {
            String jsonString = new String(response);
            return new JSONObject(jsonString);
        }
        return null;
    }

}
