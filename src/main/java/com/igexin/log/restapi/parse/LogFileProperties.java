package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class LogFileProperties {

    private String platform;

    private String uid;

    private String appId;

    private int level;

    private String loggerName;

    private String layouts;

    private String alias;

    private String filename;

    private String workPath;

    private String originalFilename;

    private final ConcurrentHashMap<Integer, String> layoutMap = new ConcurrentHashMap<>();

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLayouts() {
        return layouts;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setLayouts(String layouts) {
        this.layouts = layouts;

        layoutMap.clear();
        JSONArray jsonArray = new JSONArray(layouts);
        int length = jsonArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            layoutMap.put(jsonObject.optInt("id"), jsonObject.optString("layout"));
        }
    }

    public String getLayout(short layoutId) {
        Integer key = (int) layoutId;
        return layoutMap.get(key);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getWorkPath() {
        return workPath;
    }

    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String tempPath() {
        return workPath + "/" + Constants.LOG_FILE_TEMP_DIR;
    }

    public String weedPath() {
        return workPath + "/" + Constants.WEED_TEMP_DIR;
    }

    public String errorPath() {
        return workPath + "/" + Constants.ERROR_DIR + "/" + platform.toLowerCase();
    }

    public String outputFilename() {
        return StringUtil.logFileName(getPlatform(), getUid(), getAppId(), getOriginalFilename());
    }
}
