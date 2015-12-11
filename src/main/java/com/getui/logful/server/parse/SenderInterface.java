package com.getui.logful.server.parse;

import com.getui.logful.server.entity.LogLine;

public interface SenderInterface {

    void send(LogLine logLine);

    void release() throws Exception;

}
