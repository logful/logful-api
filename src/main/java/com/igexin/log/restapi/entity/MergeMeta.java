package com.igexin.log.restapi.entity;

import com.igexin.log.restapi.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MergeMeta {

    private String loggerName;

    private String dateString;

    private String levelString;

    private String workPath;

    private List<LogFile> inFileList = new ArrayList<>();

    public static MergeMeta create(String[] params, String rootPath) {
        MergeMeta meta = new MergeMeta();
        meta.setLoggerName(params[0]);
        meta.setDateString(params[1]);
        meta.setLevelString(params[2]);
        meta.setWorkPath(rootPath);
        return meta;
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

    public String getWorkPath() {
        return workPath;
    }

    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }

    public String fullPath() {
        if (!StringUtil.isEmpty(workPath)
                && !StringUtil.isEmpty(loggerName)
                && !StringUtil.isEmpty(dateString)
                && !StringUtil.isEmpty(levelString)) {
            return String.format("%s/%s-%s-%s.bin", workPath, loggerName, dateString, levelString);
        }
        return null;
    }

    public List<LogFile> getInFileList() {
        return inFileList;
    }

    public void setInFileList(List<LogFile> inFileList) {
        this.inFileList = inFileList;
    }

    public void addLogFile(LogFile logFile) {
        if (logFile != null) {
            inFileList.add(logFile);
        }
    }

    public String[] getInFilePaths() {
        Collections.sort(inFileList);

        int length = inFileList.size();
        String[] paths = new String[length];

        for (int i = 0; i < length; i++) {
            LogFile logFile = inFileList.get(i);
            paths[i] = logFile.fullPath();
        }

        return paths;
    }
}
