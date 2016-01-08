package com.getui.logful.server.push;

import com.getui.logful.server.push.getui.GetuiPushParams;
import com.getui.logful.server.push.jpush.JPushParams;

public class PushParams {

    private GetuiPushParams getui;

    private JPushParams jPush;

    public JPushParams getjPush() {
        return jPush;
    }

    public void setjPush(JPushParams jPush) {
        this.jPush = jPush;
    }

    public GetuiPushParams getGetui() {
        return getui;
    }

    public void setGetui(GetuiPushParams getui) {
        this.getui = getui;
    }

}
