package com.igexin.log.restapi.entity;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.web.context.request.WebRequest;

import java.util.zip.CRC32;

public class UserInfo {

    @Id
    private String id;

    private int platform;

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

    public static UserInfo create(WebRequest webRequest) {
        UserInfo userInfo = new UserInfo();
        userInfo.platform = StringUtil.platformNumber(webRequest.getParameter("platform"));

        userInfo.uid = webRequest.getParameter("uid");
        userInfo.alias = webRequest.getParameter("alias");
        userInfo.model = webRequest.getParameter("model");
        userInfo.imei = webRequest.getParameter("imei");
        userInfo.macAddress = webRequest.getParameter("macAddress");
        userInfo.osVersion = webRequest.getParameter("osVersion");

        userInfo.appId = webRequest.getParameter("appId");

        String version = webRequest.getParameter("version");
        if (version != null && version.length() > 0) {
            userInfo.version = Integer.parseInt(version);
        }

        userInfo.versionString = webRequest.getParameter("versionString");

        if (!ControllerUtil.isEmpty(userInfo.getUid())) {
            CRC32 crc32 = new CRC32();
            crc32.update(userInfo.getUid().getBytes());
            long crcValue = crc32.getValue();

            userInfo.level = (int) (crcValue % 100);
        }

        return userInfo;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public static int getPlatformAndroid() {
        return Constants.PLATFORM_ANDROID;
    }

    public static int getPlatformIos() {
        return Constants.PLATFORM_IOS;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
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


    @Override
    public int hashCode() {
        String string = String.format("%d%s%s%s%s%s%s%s%d%s",
                platform, uid,
                alias, model,
                imei, macAddress,
                osVersion, appId,
                version, versionString);
        return string.hashCode();
    }

    @Override
    public String toString() {
        return "";
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
