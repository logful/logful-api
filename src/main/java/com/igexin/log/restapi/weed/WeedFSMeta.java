package com.igexin.log.restapi.weed;

import com.igexin.log.restapi.entity.WeedAttachFileMeta;
import com.igexin.log.restapi.entity.WeedLogFileMeta;

public class WeedFSMeta {

    public static final int TYPE_LOG = 0x01;

    public static final int TYPE_ATTACHMENT = 0x02;

    private int type;

    private WeedLogFileMeta logFileMeta;

    private WeedAttachFileMeta attachFileMeta;

    public static WeedFSMeta createLogFileMeta(WeedLogFileMeta logFileMeta) {
        WeedFSMeta weedFSMeta = new WeedFSMeta();
        weedFSMeta.type = TYPE_LOG;
        weedFSMeta.logFileMeta = logFileMeta;
        return weedFSMeta;
    }

    public static WeedFSMeta createAttachFileMeta(WeedAttachFileMeta attachFileMeta) {
        WeedFSMeta weedFSMeta = new WeedFSMeta();
        weedFSMeta.type = TYPE_ATTACHMENT;
        weedFSMeta.attachFileMeta = attachFileMeta;
        return weedFSMeta;
    }

    public WeedLogFileMeta getLogFileMeta() {
        return logFileMeta;
    }

    public void setLogFileMeta(WeedLogFileMeta logFileMeta) {
        this.logFileMeta = logFileMeta;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public WeedAttachFileMeta getAttachFileMeta() {
        return attachFileMeta;
    }

    public void setAttachFileMeta(WeedAttachFileMeta attachFileMeta) {
        this.attachFileMeta = attachFileMeta;
    }


}
