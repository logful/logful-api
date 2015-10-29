package org.graylog2.gelfclient;

import com.igexin.log.restapi.entity.LogLine;

public interface GelfTransportListener {

    void connected();

    void disconnected();

    void retrySuccessful(LogLine logLine);

    void failed(LogLine logLine);
}
