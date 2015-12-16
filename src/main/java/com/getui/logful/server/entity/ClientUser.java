package com.getui.logful.server.entity;

import com.getui.logful.server.Constants;
import com.getui.logful.server.util.ControllerUtil;
import com.getui.logful.server.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.context.request.WebRequest;

import java.util.zip.CRC32;

@Document(collection = "client_user")
public class ClientUser {

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

    @Indexed(unique = true)
    private String hashString;

    public static ClientUser create(WebRequest webRequest) {
        ClientUser clientUser = new ClientUser();
        clientUser.platform = StringUtil.platformNumber(webRequest.getParameter("platform"));

        clientUser.uid = webRequest.getParameter("uid");
        clientUser.alias = webRequest.getParameter("alias");
        clientUser.model = webRequest.getParameter("model");
        clientUser.imei = webRequest.getParameter("imei");
        clientUser.macAddress = webRequest.getParameter("macAddress");
        clientUser.osVersion = webRequest.getParameter("osVersion");

        clientUser.appId = webRequest.getParameter("appId");

        String version = webRequest.getParameter("version");
        if (version != null && version.length() > 0) {
            clientUser.version = Integer.parseInt(version);
        }

        clientUser.versionString = webRequest.getParameter("versionString");

        if (!ControllerUtil.isEmpty(clientUser.getUid())) {
            CRC32 crc32 = new CRC32();
            crc32.update(clientUser.getUid().getBytes());
            long crcValue = crc32.getValue();

            clientUser.level = (int) (crcValue % 100);
        }

        return clientUser;
    }

    public void generateHashString() {
        String[] array = {
                String.valueOf(platform), uid, alias, model, imei, macAddress,
                osVersion, appId, String.valueOf(version), versionString
        };
        this.hashString = DigestUtils.md5Hex(StringUtils.join(array, ""));
    }

    public static int getPlatformAndroid() {
        return Constants.PLATFORM_ANDROID;
    }

    public static int getPlatformIos() {
        return Constants.PLATFORM_IOS;
    }

    public String getHashString() {
        return hashString;
    }

    public void setHashString(String hashString) {
        this.hashString = hashString;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
