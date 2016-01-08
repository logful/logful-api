package com.getui.logful.server.weed;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.AttachFileMeta;
import com.getui.logful.server.entity.CrashFileMeta;
import com.getui.logful.server.entity.LogFileMeta;
import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.mongod.AttachFileMetaRepository;
import com.getui.logful.server.mongod.CrashFileMetaRepository;
import com.getui.logful.server.mongod.LogFileMetaRepository;
import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class WeedFSReadThread {

    private final ReentrantLock lock;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread handlerThread;

    public WeedFSReadThread(final String dirPath,
                            final LogFileMetaRepository logRepository,
                            final AttachFileMetaRepository attachRepository,
                            final CrashFileMetaRepository crashRepository,
                            final GraylogClientService graylogService,
                            final WeedQueueRepository queueRepository,
                            final ConcurrentHashMap<String, WeedFSMeta> weedMetaMap,
                            final BlockingQueue<WeedFSMeta> queue) {
        this.lock = new ReentrantLock();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                WeedFSMeta weedFSMeta = null;

                while (running.get()) {
                    lock.lock();

                    try {
                        if (weedFSMeta == null) {
                            weedFSMeta = queue.poll(100, TimeUnit.MILLISECONDS);
                        }
                        if (weedFSMeta != null && !StringUtil.isEmpty(dirPath)) {
                            try {
                                JSONObject object = weedFSMeta.responseObject();
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
                                                LogFileMeta logFileMeta = meta.getLogFileMeta();
                                                if (logFileMeta != null) {
                                                    logFileMeta.setFid(fid);
                                                    logFileMeta.setSize(size);

                                                    // Save log file meta to db.
                                                    logRepository.save(logFileMeta);
                                                }
                                            } else if (meta.getType() == WeedFSMeta.TYPE_ATTACHMENT) {
                                                // Attachment file.
                                                AttachFileMeta attachFileMeta = meta.getAttachFileMeta();
                                                if (attachFileMeta != null) {
                                                    attachFileMeta.setFid(fid);
                                                    attachFileMeta.setSize(size);

                                                    // Save attachment file meta to db.
                                                    attachRepository.save(attachFileMeta);
                                                }
                                            } else if (meta.getType() == WeedFSMeta.TYPE_CRASH) {
                                                // Crash file.
                                                CrashFileMeta crashFileMeta = meta.getCrashFileMeta();
                                                if (crashFileMeta != null) {
                                                    crashFileMeta.setFid(fid);
                                                    crashFileMeta.setSize(size);

                                                    // Save crash file meta to db.
                                                    crashRepository.save(crashFileMeta);

                                                    // Send to graylog.
                                                    graylogService.send(LogMessage.create(crashFileMeta));
                                                }
                                            }
                                            // Remove WeedFSMeta form map.
                                            weedMetaMap.remove(key);
                                        }

                                        if (!StringUtil.isEmpty(weedFSMeta.getId())) {
                                            weedFSMeta.setStatus(WeedFSMeta.STATE_SUCCESSFUL);
                                            queueRepository.save(weedFSMeta);
                                        }

                                        String filePath = dirPath + "/" + filename;
                                        FileUtils.deleteQuietly(new File(filePath));
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore all exception.
                            } finally {
                                weedFSMeta = null;
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
