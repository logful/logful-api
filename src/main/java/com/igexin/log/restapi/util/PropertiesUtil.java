package com.igexin.log.restapi.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PropertiesUtil {

    private String graylogHost;

    private int graylogPort;

    private static class ClassHolder {
        static PropertiesUtil util = new PropertiesUtil();
    }

    public static PropertiesUtil util() {
        return ClassHolder.util;
    }

    public PropertiesUtil() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/application.properties");
            BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = buffer.readLine()) != null) {
                if (line.contains("logful.graylog.host") && line.charAt(0) != '#') {
                    graylogHost = line.split("=")[1];
                }
                if (line.contains("logful.graylog.port")) {
                    graylogPort = Integer.parseInt(line.split("=")[1]);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Read properties file failed.", e);
        }
    }

    public static String graylogHost() {
        PropertiesUtil util = new PropertiesUtil();
        return util.graylogHost;
    }

    public static int graylogPort() {
        PropertiesUtil util = new PropertiesUtil();
        return util.graylogPort;
    }

}
