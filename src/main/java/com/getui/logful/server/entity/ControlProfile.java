package com.getui.logful.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "control_profile")
public class ControlProfile {

    @Id
    private String id;
    /**
     * 配置文件名称.
     */
    private String name;
    /**
     * 更新时间.
     */
    private Long updateTime;
    /**
     * 平台.
     */
    private Integer platform;
    /**
     * 用户 uid
     */
    private String uid;
    /**
     * 用户别名.
     */
    private String alias;
    /**
     * 应用 id.
     */
    private String appId;
    /**
     * 释放允许上传.
     */
    private Boolean shouldUpload;
    /**
     * 计划类型.
     */
    private Integer scheduleType;
    /**
     * 定时时间.
     */
    private List<Schedule> scheduleArray;
    /**
     * 间隔时间.
     */
    private Long scheduleTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Schedule> getScheduleArray() {
        return scheduleArray;
    }

    public List<String> scheduleDate() {
        List<String> temp = new ArrayList<>();
        if (scheduleArray != null) {
            for (Schedule schedule : scheduleArray) {
                temp.add(schedule.getTimeString());
            }
        }
        return temp;
    }

    public void setScheduleArray(List<Schedule> scheduleArray) {
        this.scheduleArray = scheduleArray;
    }

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
    }

    public Long getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(Long scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Boolean getShouldUpload() {
        return shouldUpload;
    }

    public void setShouldUpload(Boolean shouldUpload) {
        this.shouldUpload = shouldUpload;
    }

    public Integer getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(Integer scheduleType) {
        this.scheduleType = scheduleType;
    }

    private static class Schedule {

        private String timeString;

        public String getTimeString() {
            return timeString;
        }

        public void setTimeString(String timeString) {
            this.timeString = timeString;
        }

    }

}
