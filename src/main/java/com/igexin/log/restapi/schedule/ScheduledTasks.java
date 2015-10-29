package com.igexin.log.restapi.schedule;

import com.igexin.log.restapi.GlobalReference;
import com.igexin.log.restapi.RestApiProperties;
import com.igexin.log.restapi.entity.LogFile;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.entity.MergeMeta;
import com.igexin.log.restapi.mongod.MongoLogLineRepository;
import com.igexin.log.restapi.parse.GrayLogSender;
import com.igexin.log.restapi.util.FileUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledTasks {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final int PAGE_LIMIT = 5000;
    private static final int THREAD_SIZE = 10;
    private static final long MERGE_BEFORE_DAY = 2;
    private static final long DELETE_BEFORE_DAY = 7;

    @Autowired
    private RestApiProperties restApiProperties;

    @Autowired
    private MongoLogLineRepository mongoDbLogLineRepository;

    private ThreadPoolExecutor executor;

    private ThreadPoolExecutor getExecutor() {
        if (executor == null || executor.isTerminated()) {
            executor =
                    new ThreadPoolExecutor(THREAD_SIZE, THREAD_SIZE, 0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>());
        }
        return executor;
    }

    /**
     * 重发发送到 GrayLog 失败的日志内容.
     */
    @Scheduled(cron = "*/10 * * * * *")
    public void resendFailedLogLine() {
        GlobalReference.listen();
        if (GlobalReference.isConnected()) {
            GlobalReference.saveLogLineRepository(mongoDbLogLineRepository);
            GrayLogSender logSender = new GrayLogSender();
            while (true) {
                List<LogLine> logLineList = mongoDbLogLineRepository.findAllFailedLimit(PAGE_LIMIT);
                if (logLineList == null || logLineList.size() == 0) {
                    break;
                }
                for (LogLine logLine : logLineList) {
                    if (logLine.getStatus() == LogLine.STATE_FAILED) {
                        logSender.send(logLine);
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
        String androidDirPath = restApiProperties.decryptedDir("android");
        String iosDirPath = restApiProperties.decryptedDir("ios");

        recursionDir(androidDirPath);
        recursionDir(iosDirPath);

        clearAttachmentDir();
    }

    /**
     * Recursion dir path.
     *
     * @param dirPath Dir path
     */
    private void recursionDir(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            // Get app dir file array.
            File[] appDirs = dir.listFiles();
            if (appDirs != null) {
                for (File appDir : appDirs) {
                    if (appDir.exists() && appDir.isDirectory()) {
                        // iterate uid dir.
                        iterateUidDir(appDir.listFiles());
                    }
                }
            }
        }
    }

    /**
     * Clear expired attachment file.
     */
    private void clearAttachmentDir() {
        String dirPath = restApiProperties.attachmentDir();
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
            getExecutor().submit(new DeleteFileTask(deletePaths.toArray(new String[size])));
        }
    }

    /**
     * Iterate uid dir.
     *
     * @param dirs Uid dirs
     */
    private void iterateUidDir(File[] dirs) {
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir.exists() && dir.isDirectory()) {
                    iterateLogFile(dir.listFiles());
                }
            }
        }
    }

    /**
     * Iterate log file.
     *
     * @param files Log files
     */
    private void iterateLogFile(File[] files) {
        if (files != null) {
            HashMap<String, MergeMeta> metaMap = new HashMap<>();
            List<String> deletePaths = new ArrayList<>();
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    // Check file expired
                    String[] parts = file.getName().replace(".bin", "").split("-");
                    if (parts.length >= 3) {
                        long time = dateToTime(parts[1]);
                        if (time != -1) {
                            long days = diffDays(time);
                            if (days >= MERGE_BEFORE_DAY && days < DELETE_BEFORE_DAY) {
                                // Check and merge log file
                                if (validate(parts)) {
                                    String rootPath = file.getParent();
                                    LogFile logFile = LogFile.create(parts, rootPath, file.getName());

                                    String key = key(parts);
                                    MergeMeta meta = metaMap.get(key);
                                    if (meta == null) {
                                        meta = MergeMeta.create(parts, rootPath);
                                    }

                                    meta.addLogFile(logFile);
                                    metaMap.put(key, meta);
                                }
                            } else if (days >= DELETE_BEFORE_DAY) {
                                // Delete expired file
                                deletePaths.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            int size = deletePaths.size();
            if (size > 0) {
                getExecutor().submit(new DeleteFileTask(deletePaths.toArray(new String[size])));
            }

            if (metaMap.size() > 0) {
                getExecutor().submit(new MergeTask(metaMap));
            }
        }
    }

    private long dateToTime(String dateString) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        try {
            Date date = format.parse(dateString);
            if (!dateString.equals(format.format(date))) {
                return -1;
            }
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long diffDays(long time) {
        Date today = new Date();
        long diff = today.getTime() - time;
        return diff / (24 * 60 * 60 * 1000);
    }

    private boolean validate(String[] parts) {
        if (parts.length == 4) {
            int fragment = 0;
            if (!StringUtil.isEmpty(parts[3])) {
                try {
                    fragment = Integer.parseInt(parts[3]);
                } catch (Exception e) {
                    return false;
                }
            }
            return !StringUtil.isEmpty(parts[0])
                    && !StringUtil.isEmpty(parts[1])
                    && !StringUtil.isEmpty(parts[2])
                    && fragment > 0;
        }
        return false;
    }

    private String key(String[] parts) {
        return String.format("%s-%s-%s", parts[0], parts[1], parts[2]);
    }

    private class MergeTask implements Runnable {

        private HashMap<String, MergeMeta> metaMap;

        public MergeTask(HashMap<String, MergeMeta> metaMap) {
            this.metaMap = metaMap;
        }

        @Override
        public void run() {
            if (metaMap != null) {
                for (MergeMeta meta : metaMap.values()) {
                    String[] paths = meta.getInFilePaths();
                    if (FileUtil.merge(meta.fullPath(), paths)) {
                        FileUtil.delete(paths);
                    }
                }
            }
        }
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
