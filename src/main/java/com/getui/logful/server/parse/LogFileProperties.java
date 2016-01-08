package com.getui.logful.server.parse;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.LogFileMeta;
import com.getui.logful.server.util.StringUtil;
import com.getui.logful.server.weed.WeedFSMeta;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class LogFileProperties {

    private final ConcurrentHashMap<Integer, String> layoutMap = new ConcurrentHashMap<>();
    private int platform;
    private String clientId;
    private String uid;
    private String appId;
    private int level;
    private String loggerName;
    private String layouts;
    private String alias;
    private String filename;
    private String fileSum;
    private String workPath;
    private String originalFilename;
    private int cryptoVersion;
    private String key;
    private String extension;
    private byte[] security;

    public LogFileProperties() {
        this.key = StringUtil.randomUid();
        this.extension = Constants.LOG_FILE_EXTENSION;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public byte[] getSecurity() {
        return security;
    }

    public void setSecurity(byte[] security) {
        this.security = security;
    }

    public String getFileSum() {
        return fileSum;
    }

    public void setFileSum(String fileSum) {
        this.fileSum = fileSum;
    }

    public int getCryptoVersion() {
        return cryptoVersion;
    }

    public void setCryptoVersion(int cryptoVersion) {
        this.cryptoVersion = cryptoVersion;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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

    public String cacheFilePath() {
        return tempPath() + "/" + getFilename();
    }

    public String tempPath() {
        return workPath + "/" + Constants.LOG_FILE_TEMP_DIR;
    }

    public String weedPath() {
        return workPath + "/" + Constants.WEED_TEMP_DIR;
    }

    public String errorPath() {
        return workPath + "/" + Constants.ERROR_DIR + "/" + StringUtil.platformString(platform).toLowerCase();
    }

    public String outFilePath() {
        return workPath + "/" + Constants.WEED_TEMP_DIR + "/" + key + "." + extension;
    }

    public WeedFSMeta create() {
        int index = originalFilename.indexOf(".");
        String[] temp = originalFilename.substring(0, index).split("-");
        if (temp.length != 4) {
            return null;
        }
        String dateString = temp[1];
        try {
            Date date = DateTime.parse(dateString).toDate();
            int fragment = Integer.parseInt(temp[3]);
            LogFileMeta meta = LogFileMeta.create((short) platform, clientId, uid, appId, loggerName, date, (short) level, fragment);
            return WeedFSMeta.create(key, extension, meta);
        } catch (Exception e) {
            return null;
        }
    }
}
