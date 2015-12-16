package com.getui.logful.server.schedule;

import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.mongod.MongoLogMessageRepository;
import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.weed.WeedFSClientService;
import com.getui.logful.server.weed.WeedFSMeta;
import com.getui.logful.server.weed.WeedQueueRepository;
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
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScheduledTasks {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final int PAGE_LIMIT = 2048;

    private static final long MAX_EXEC_MILLISECOND = 3600000;

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private MongoLogMessageRepository mongoLogMessageRepository;

    @Autowired
    private WeedQueueRepository weedQueueRepository;

    @Autowired
    private WeedFSClientService weedFSClientService;

    @Autowired
    private GraylogClientService graylogClientService;

    private AtomicLong startExecTime = new AtomicLong(0);

    @Scheduled(cron = "*/300 * * * * *")
    @Async
    public void retryPutQueue() {
        weedFSClientService.resetServerError();

        if (graylogClientService.isConnected()) {
            List<LogMessage> list = mongoLogMessageRepository.findAllNotSendLimit(PAGE_LIMIT);
            for (LogMessage logMessage : list) {
                graylogClientService.send(logMessage);
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
        startExecTime.set(System.currentTimeMillis());

        final long ttl = logfulProperties.expires() * 1000;
        final Path path = Paths.get(logfulProperties.getPath());
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    long current = System.currentTimeMillis();
                    if (current - startExecTime.get() >= MAX_EXEC_MILLISECOND) {
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    long current = System.currentTimeMillis();

                    if (attrs.isRegularFile()) {
                        if (!Files.isHidden(file)) {
                            long diff = current - attrs.creationTime().toMillis();
                            if (diff >= ttl) {
                                Files.delete(file);
                            }
                        }
                    }

                    if (current - startExecTime.get() >= MAX_EXEC_MILLISECOND) {
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    long current = System.currentTimeMillis();
                    if (current - startExecTime.get() >= MAX_EXEC_MILLISECOND) {
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
    }

}
