package com.getui.logful.server.push;

import org.springframework.http.HttpStatus;

public class PushResponse {

    private HttpStatus status;
    private String payload;
    private String extra;

    public PushResponse(String extra) {
        this.extra = extra;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public boolean ok() {
        return status == HttpStatus.OK;
    }
}
