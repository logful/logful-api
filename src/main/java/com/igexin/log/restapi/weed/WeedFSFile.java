package com.igexin.log.restapi.weed;

import org.json.JSONObject;

public class WeedFSFile {

    private String key;

    private String extension;

    private byte[] response;

    public static WeedFSFile create(byte[] response) {
        WeedFSFile weedFSFile = new WeedFSFile();
        weedFSFile.response = response;
        return weedFSFile;
    }

    public static WeedFSFile create(String key, String extension) {
        WeedFSFile weedFSFile = new WeedFSFile();
        weedFSFile.key = key;
        weedFSFile.extension = extension;
        return weedFSFile;
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

    public String filename() {
        return key + "." + extension;
    }

    public JSONObject responseObject() {
        if (response != null && response.length > 0) {
            String jsonString = new String(response);
            return new JSONObject(jsonString);
        }
        return null;
    }

}
