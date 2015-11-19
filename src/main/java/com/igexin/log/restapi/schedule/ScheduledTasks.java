package com.igexin.log.restapi.schedule;

import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.mongod.MongoLogLineRepository;
import com.igexin.log.restapi.parse.GraylogClientService;
import com.igexin.log.restapi.weed.WeedFSClientService;
import com.igexin.log.restapi.weed.WeedFSMeta;
import com.igexin.log.restapi.weed.WeedQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduledTasks {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);

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

    @Scheduled(cron = "0 0 */1 * * *") // every hour
    @Async
    public void clearMemory() {
        String weedPath = logfulProperties.weedDir();
        ConcurrentHashMap<String, WeedFSMeta> map = weedFSClientService.getWeedMetaMap();
        for (Map.Entry<String, WeedFSMeta> entry : map.entrySet()) {
            File file = new File(weedPath + "/" + entry.getKey() + "." + entry.getValue().getExtension());
            if (!file.exists()) {
                map.remove(entry.getKey());
            }
        }
    }

    //@Scheduled(cron = "0 10 1 ? * *") // every day 1:10
    @Scheduled(cron = "0 0 */6 * * *")
    @Async
    public void clearFiles() {
        LOG.info("++++++++++ clear system file task start ++++++++++");
        final long ttl = logfulProperties.expires() * 1000;
        final long current = System.currentTimeMillis();
        final Path path = Paths.get(logfulProperties.getPath());
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        if (!Files.isHidden(file)) {
                            long diff = current - attrs.creationTime().toMillis();
                            if (diff >= ttl) {
                                Files.delete(file);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
    }

}
