package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.entity.LogLine;

public interface SenderInterface {

    void send(LogLine logLine);

    void release() throws Exception;

}
