package com.getui.logful.server.entity;

public class Certificate {

    private byte[] key;

    private byte[] iv;

    public Certificate(byte[] key, byte[] iv) {
        this.key = key;
        this.iv = iv;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

}
