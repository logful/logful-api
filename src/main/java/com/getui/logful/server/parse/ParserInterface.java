package com.getui.logful.server.parse;

import java.io.InputStream;

public interface ParserInterface {

    void parse(byte[] security, int version, InputStream inputStream);
}
