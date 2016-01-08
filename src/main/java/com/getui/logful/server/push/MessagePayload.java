package com.getui.logful.server.push;

import org.json.JSONObject;

public class MessagePayload {

    private boolean on;
    private long interval;
    private long frequency;
    private PushParams params;

    public PushParams getParams() {
        return params;
    }

    public void setParams(PushParams params) {
        this.params = params;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    public String payload() {
        JSONObject object = new JSONObject();
        object.put("on", on);
        object.put("interval", interval);
        object.put("frequency", frequency);
        return object.toString();
    }
}
