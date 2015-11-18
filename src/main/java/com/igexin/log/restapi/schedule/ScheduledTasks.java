package com.igexin.log.restapi.schedule;

import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.mongod.MongoLogLineRepository;
import com.igexin.log.restapi.parse.GraylogClientService;
import com.igexin.log.restapi.weed.WeedFSClientService;
import com.igexin.log.restapi.weed.WeedFSMeta;
import com.igexin.log.restapi.weed.WeedQueueRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduledTasks {

    private static final int PAGE_LIMIT = 2048;

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private MongoLogLineRepository mongoLogLineRepository;

    @Autowired
    private WeedQueueRepository weedQueueRepository;

    @Autowired
    private WeedFSClientService weedFSClientService;

    @Autowired
    private GraylogClientService graylogClientService;

    @Scheduled(cron = "*/300 * * * * *")
    @Async
    public void retryPutQueue() {
        weedFSClientService.resetServerError();

        if (graylogClientService.isConnected()) {
            List<LogLine> logLineList = mongoLogLineRepository.findAllNotSendLimit(PAGE_LIMIT);
            for (LogLine logLine : logLineList) {
                graylogClientService.send(logLine);
            }
        }

        if (weedFSClientService.isConnected()) {
            List<WeedFSMeta> weedFSMetaList = weedQueueRepository.findAllNotWriteLimit(PAGE_LIMIT);
            for (WeedFSMeta weedFSMeta : weedFSMetaList) {
                weedFSClientService.write(weedFSMeta);
            }
        }
    }

    //@Scheduled(cron = "0 10 1 ? * *") // 每天凌晨 1:10 执行
    @Scheduled(cron = "0 0 */1 * * *") // 每一小时执行一次
    @Async
    public void clearSystem() {
        String weedPath = logfulProperties.weedDir();
        ConcurrentHashMap<String, WeedFSMeta> map = weedFSClientService.getWeedMetaMap();
        for (Map.Entry<String, WeedFSMeta> entry : map.entrySet()) {
            File file = new File(weedPath + "/" + entry.getKey() + "." + entry.getValue().getExtension());
            if (!file.exists()) {
                map.remove(entry.getKey());
            }
        }

        long ttl = logfulProperties.ttlSeconds() * 1000;
        long current = System.currentTimeMillis();

        File dir = new File(weedPath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (current - file.lastModified() >= ttl) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }

}
