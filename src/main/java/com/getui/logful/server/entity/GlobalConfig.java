package com.getui.logful.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "global_config")
public class GlobalConfig {

    @Id
    private String id;

    private int level;

    private List<String> grantClients = new ArrayList<>();

    public List<String> getGrantClients() {
        return grantClients;
    }

    public void setGrantClients(List<String> grantClients) {
        this.grantClients = grantClients;
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

    public void setLevel(int level) {
        this.level = level;
    }

}
