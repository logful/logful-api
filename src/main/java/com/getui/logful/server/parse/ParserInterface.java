package com.getui.logful.server.parse;

import java.io.InputStream;

public interface ParserInterface {

    void parse(String appId, boolean compatible, byte[] security, InputStream inputStream);
}
