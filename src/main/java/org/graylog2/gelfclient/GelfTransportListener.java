package org.graylog2.gelfclient;

import com.getui.logful.server.entity.LogMessage;

public interface GelfTransportListener {

    void connected();

    void disconnected();

    void retrySuccessful(LogMessage logMessage);

    void failed(LogMessage logMessage);
}
