package com.igexin.log.restapi.weed;

import com.igexin.log.restapi.entity.WeedAttachFileMeta;
import com.igexin.log.restapi.entity.WeedLogFileMeta;
import com.igexin.log.restapi.mongod.MongoWeedAttachFileMetaRepository;
import com.igexin.log.restapi.mongod.MongoWeedLogFileMetaRepository;
import com.igexin.log.restapi.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class WeedFSHandlerThread {

    private final ReentrantLock lock;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread handlerThread;

    public WeedFSHandlerThread(final String dirPath,
                               final MongoWeedLogFileMetaRepository logRepository,
                               final MongoWeedAttachFileMetaRepository attachRepository,
                               final ConcurrentHashMap<String, WeedFSMeta> weedMetaMap,
                               final BlockingQueue<WeedFSFile> queue) {
        this.lock = new ReentrantLock();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                WeedFSFile weedFSFile = null;

                while (running.get()) {
                    lock.lock();

                    try {
                        if (weedFSFile == null) {
                            weedFSFile = queue.poll(100, TimeUnit.MILLISECONDS);
                        }
                        if (weedFSFile != null && !StringUtil.isEmpty(dirPath)) {
                            try {
                                JSONObject object = weedFSFile.responseObject();
                                if (object != null) {
                                    boolean success = object.has("fid") && object.has("fileName")
                                            && object.has("size") && object.has("fileUrl");
                                    if (success) {
                                        String fid = object.optString("fid");
                                        String filename = object.optString("fileName");
                                        long size = object.optLong("size");

                                        String key = FilenameUtils.getBaseName(filename);
                                        WeedFSMeta meta = weedMetaMap.get(key);
                                        if (meta != null) {
                                            if (meta.getType() == WeedFSMeta.TYPE_LOG) {
                                                // Log file.
                                                WeedLogFileMeta logFileMeta = meta.getLogFileMeta();
                                                if (logFileMeta != null) {
                                                    logFileMeta.setFid(fid);
                                                    logFileMeta.setSize(size);

                                                    // Save log file meta to db.
                                                    logRepository.save(logFileMeta);
                                                }
                                            } else if (meta.getType() == WeedFSMeta.TYPE_ATTACHMENT) {
                                                // Attachment file.
                                                WeedAttachFileMeta attachFileMeta = meta.getAttachFileMeta();
                                                if (attachFileMeta != null) {
                                                    attachFileMeta.setFid(fid);
                                                    attachFileMeta.setSize(size);

                                                    // Save attachment file meta to db.
                                                    attachRepository.save(attachFileMeta);
                                                }
                                            }
                                            // Remove WeedFSMeta form map.
                                            weedMetaMap.remove(key);
                                        }
                                        String filePath = dirPath + "/" + filename;
                                        FileUtils.deleteQuietly(new File(filePath));
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore all exception.
                            } finally {
                                weedFSFile = null;
                            }
                        }
                    } catch (InterruptedException e) {
                        // Ignore all exception.
                    }
                    lock.unlock();
                }
            }
        };

        this.handlerThread = new Thread(runnable);
        handlerThread.setName("weed-fs-handler-" + handlerThread.getId());
        handlerThread.setDaemon(true);
    }

    public void start() {
        handlerThread.start();
    }

    public void stop() {
        running.set(false);
        handlerThread.interrupt();
    }

}
