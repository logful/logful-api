package com.getui.logful.server.parse;

import com.getui.logful.server.entity.LogMessage;

public interface SenderInterface {

    void send(LogMessage logMessage);

    void release() throws Exception;

}
