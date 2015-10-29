package com.igexin.log.restapi.entity;

import org.jetbrains.annotations.NotNull;

public class LogFile implements Comparable<LogFile> {

    private String loggerName;

    private String dateString;

    private String levelString;

    private int fragment;

    private String workPath;

    private String filename;

    public static LogFile create(String[] params, String rootPath, String filename) {
        LogFile logFile = new LogFile();
        logFile.setLoggerName(params[0]);
        logFile.setDateString(params[1]);
        logFile.setLevelString(params[2]);
        logFile.setFragment(Integer.parseInt(params[3]));
        logFile.setWorkPath(rootPath);
        logFile.setFilename(filename);
        return logFile;
    }

    public String getWorkPath() {
        return workPath;
    }

    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getLevelString() {
        return levelString;
    }

    public void setLevelString(String levelString) {
        this.levelString = levelString;
    }

    public int getFragment() {
        return fragment;
    }

    public void setFragment(int fragment) {
        this.fragment = fragment;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String fullPath() {
        return String.format("%s/%s", workPath, filename);
    }

    @Override
    public int compareTo(@NotNull LogFile object) {
        return getFragment() - object.getFragment();
    }
}
