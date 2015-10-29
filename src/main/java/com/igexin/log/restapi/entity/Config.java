package com.igexin.log.restapi.entity;

import org.springframework.data.annotation.Id;

public class Config {

    @Id
    private String id;

    private int level;

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
