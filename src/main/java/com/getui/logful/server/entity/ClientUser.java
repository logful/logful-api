package com.getui.logful.server.entity;

import com.getui.logful.server.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.zip.CRC32;

@Document(collection = "client_user")
public class ClientUser {

    @Id
    private String id;

    private int platform;
    private String clientId;
    private String uid;
    private String alias;
    private String model;
    private String imei;
    private String macAddress;
    private String osVersion;
    private String appId;
    private int version;
    private String versionString;
    private int level;
    private boolean recordOn;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isRecordOn() {
        return recordOn;
    }

    public void setRecordOn(boolean recordOn) {
        this.recordOn = recordOn;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
        if (!StringUtils.isEmpty(uid)) {
            CRC32 crc32 = new CRC32();
            crc32.update(uid.getBytes());
            long crcValue = crc32.getValue();
            this.level = (int) (crcValue % 100);
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getVersionString() {
        return versionString;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("platform", StringUtil.platformString(platform));
        jsonObject.put("uid", uid);
        jsonObject.put("alias", alias);
        jsonObject.put("model", model);
        jsonObject.put("imei", imei);
        jsonObject.put("macAddress", macAddress);
        jsonObject.put("osVersion", osVersion);
        jsonObject.put("appId", appId);
        jsonObject.put("version", version);
        jsonObject.put("versionString", versionString);

        return jsonObject;
    }
}
