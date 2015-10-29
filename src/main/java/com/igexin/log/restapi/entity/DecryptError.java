package com.igexin.log.restapi.entity;

import org.springframework.data.annotation.Id;

public class DecryptError {

    @Id
    private String id;

    private long timestamp;

    private String uid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "";
    }
}