package com.igexin.log.restapi.entity;

import org.springframework.data.annotation.Id;

public class WeedAttachFileMeta {

    @Id
    private String id;

    private String fid;

    private String attachmentId;

    private long timestamp;

    private long size;

    public static WeedAttachFileMeta create(String attachmentId, long timestamp) {
        WeedAttachFileMeta meta = new WeedAttachFileMeta();
        meta.setAttachmentId(attachmentId);
        meta.setTimestamp(timestamp);
        return meta;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
