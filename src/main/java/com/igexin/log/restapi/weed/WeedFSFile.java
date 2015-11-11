package com.igexin.log.restapi.weed;

import org.json.JSONObject;

public class WeedFSFile {

    private String filename;

    private byte[] response;

    public WeedFSFile(byte[] response) {
        this.response = response;
    }

    public WeedFSFile(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public JSONObject responseObject() {
        if (response != null && response.length > 0) {
            String jsonString = new String(response);
            return new JSONObject(jsonString);
        }
        return null;
    }

}
