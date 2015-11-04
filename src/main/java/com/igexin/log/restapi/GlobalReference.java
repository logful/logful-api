package com.igexin.log.restapi;

import com.igexin.log.restapi.entity.Layout;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.mongod.MongoDecryptErrorRepository;
import com.igexin.log.restapi.mongod.MongoLogLineRepository;
import org.graylog2.gelfclient.GelfTransportListener;

import java.util.HashMap;
import java.util.Map;

public class GlobalReference implements GelfTransportListener {

    private static final int MAX_CAPACITY = 1000;
    private MongoLogLineRepository logLineRepository;
    private MongoDecryptErrorRepository decryptErrorRepository;
    private LogfulProperties properties;
    private boolean connected = false;
    private HashMap<String, Layout> layoutMap = new HashMap<>();

    @Override
    public void connected() {
        this.connected = true;
    }

    @Override
    public void disconnected() {
        this.connected = false;
    }

    @Override
    public void retrySuccessful(LogLine logLine) {
        if (logLineRepository != null && logLine != null) {
            if (logLine.getId() != null && logLine.getId().length() > 0) {
                logLine.setStatus(LogLine.STATE_SUCCESSFUL);
                logLineRepository.save(logLine);
            }
        }
    }

    @Override
    public void failed(LogLine logLine) {
        if (logLineRepository != null && logLine != null) {
            // 保存发送失败的日志到 mongodb
            logLine.setStatus(LogLine.STATE_FAILED);
            logLineRepository.save(logLine);
        }
    }

    private static class ClassHolder {
        static GlobalReference instance = new GlobalReference();
    }

    public static GlobalReference reference() {
        return ClassHolder.instance;
    }

    public static void listen() {
        GlobalReference reference = reference();
        LogfulApplication.transport.setListener(reference);
    }

    public static boolean isConnected() {
        GlobalReference reference = reference();
        return reference.connected;
    }

    public static void saveLogLine(LogLine logLine) {
        MongoLogLineRepository logLineRepository = logLineRepository();
        if (logLineRepository != null) {
            logLineRepository.save(logLine);
        }
    }

    public static MongoLogLineRepository logLineRepository() {
        GlobalReference reference = reference();
        return reference.logLineRepository;
    }

    public static MongoDecryptErrorRepository decryptErrorRepository() {
        GlobalReference reference = reference();
        return reference.decryptErrorRepository;
    }

    public static LogfulProperties properties() {
        GlobalReference reference = reference();
        return reference.properties;
    }

    public static void saveLogLineRepository(MongoLogLineRepository logLineRepository) {
        GlobalReference reference = reference();
        reference.logLineRepository = logLineRepository;
    }

    public static void saveDecryptErrorRepository(MongoDecryptErrorRepository decryptErrorRepository) {
        GlobalReference reference = reference();
        reference.decryptErrorRepository = decryptErrorRepository;
    }

    public static void saveProperties(LogfulProperties properties) {
        GlobalReference reference = reference();
        reference.properties = properties;
    }

    public static Layout getLayout(String template) {
        GlobalReference reference = reference();
        Layout layout = reference.layoutMap.get(template);
        if (layout == null) {
            layout = new Layout(template);
            int size = reference.layoutMap.size();
            if (size >= MAX_CAPACITY) {
                int itemToRemove = size - MAX_CAPACITY;
                // 移除超出最大容量的元素
                for (int i = 0; i < itemToRemove; i++) {
                    Map.Entry<String, Layout> entry = reference.layoutMap.entrySet().iterator().next();
                    reference.layoutMap.remove(entry.getKey());
                }
            }
            // 缓存已解析的模板
            reference.layoutMap.put(template, layout);
        }
        return layout;
    }

}
