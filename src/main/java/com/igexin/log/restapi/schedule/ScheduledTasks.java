package com.igexin.log.restapi.schedule;

import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.mongod.MongoLogLineRepository;
import com.igexin.log.restapi.parse.GraylogClientService;
import com.igexin.log.restapi.util.FileUtil;
import com.igexin.log.restapi.weed.WeedFSClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ScheduledTasks {

    private static final int PAGE_LIMIT = 5000;
    private static final long DELETE_BEFORE_DAY = 7;

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private MongoLogLineRepository mongoDbLogLineRepository;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    WeedFSClientService weedFSClientService;

    @Autowired
    GraylogClientService graylogClientService;

    /**
     * 重发发送到 GrayLog 失败的日志内容.
     */
    @Scheduled(cron = "*/10 * * * * *")
    public void resendFailedLogLine() {
        if (graylogClientService.isConnected()) {
            while (true) {
                List<LogLine> logLineList = mongoDbLogLineRepository.findAllFailedLimit(PAGE_LIMIT);
                if (logLineList == null || logLineList.size() == 0) {
                    break;
                }
                for (LogLine logLine : logLineList) {
                    if (logLine.getStatus() == LogLine.STATE_FAILED) {
                        graylogClientService.send(logLine);
                    }
                }
            }
        }
    }

    /**
     * 删除过期日志文件、合并上传的解密日志文件.
     */
    @Scheduled(cron = "0 10 1 ? * *") // 每天凌晨 1:10 执行
    //@Scheduled(cron = "0 0 */1 * * *") // 每一小时执行一次
    //@Scheduled(cron = "*/10 * * * * *")
    public void clearAndMergeFile() {
        clearAttachmentDir();
    }

    /**
     * Clear expired attachment file.
     */
    private void clearAttachmentDir() {
        String dirPath = logfulProperties.attachmentDir();
        File dir = new File(dirPath);
        List<String> deletePaths = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    long days = diffDays(file.lastModified());
                    if (days >= DELETE_BEFORE_DAY) {
                        deletePaths.add(file.getAbsolutePath());
                    }
                }
            }
        }

        int size = deletePaths.size();
        if (size > 0) {
            threadPoolTaskExecutor.submit(new DeleteFileTask(deletePaths.toArray(new String[size])));
        }
    }

    private long diffDays(long time) {
        Date today = new Date();
        long diff = today.getTime() - time;
        return diff / (24 * 60 * 60 * 1000);
    }

    private class DeleteFileTask implements Runnable {

        private String[] filePaths;

        public DeleteFileTask(String[] filePaths) {
            this.filePaths = filePaths;
        }

        @Override
        public void run() {
            if (filePaths != null) {
                FileUtil.delete(filePaths);
            }
        }
    }

}
