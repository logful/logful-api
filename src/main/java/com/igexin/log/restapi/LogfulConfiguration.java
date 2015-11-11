package com.igexin.log.restapi;

public class LogfulConfiguration {

    private static class ClassHolder {
        static LogfulConfiguration config = new LogfulConfiguration();
    }

    public static LogfulConfiguration config() {
        return ClassHolder.config;
    }

    public LogfulConfiguration() {

    }

}
