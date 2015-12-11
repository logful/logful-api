package org.graylog2.gelfclient;

import com.getui.logful.server.entity.LogLine;

public interface GelfTransportListener {

    void connected();

    void disconnected();

    void retrySuccessful(LogLine logLine);

    void failed(LogLine logLine);
}
