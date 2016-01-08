package com.getui.logful.server.push.jpush;

import java.util.List;

public class JPushParams {

    private int platform;
    private List<String> tags;
    private List<String> alias;
    private List<String> registrationIds;

    public List<String> getRegistrationIds() {
        return registrationIds;
    }

    public void setRegistrationIds(List<String> registrationIds) {
        this.registrationIds = registrationIds;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }
}
