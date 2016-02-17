package com.getui.logful.server.push;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MessagePayload {

    private long timestamp;
    private boolean interrupt;
    private boolean on;
    private long interval;
    private long frequency;
    private List<String> clientIds;
    private List<String> aliases;

    public List<String> getAliases() {
        if (aliases == null) {
            return new ArrayList<>();
        }
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getClientIds() {
        if (clientIds == null) {
            return new ArrayList<>();
        }
        return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds;
    }

    public boolean isInterrupt() {
        return interrupt;
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        JSONObject root = new JSONObject();

        JSONObject object = new JSONObject();
        object.put("timestamp", timestamp);
        object.put("on", on);
        object.put("interrupt", interrupt);
        object.put("interval", interval);
        object.put("frequency", frequency);

        root.put("logful", object);

        return root.toString();
    }
}
