package com.igexin.log.restapi.weed;

import com.igexin.log.restapi.entity.FileMeta;
import com.igexin.log.restapi.mongod.MongoFileMetaRepository;
import com.igexin.log.restapi.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class WeedFSHandlerThread {

    private final ReentrantLock lock;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread handlerThread;

    public WeedFSHandlerThread(final String dirPath, final MongoFileMetaRepository repository, final BlockingQueue<WeedFSFile> queue) {
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
                            JSONObject object = weedFSFile.responseObject();
                            if (object != null) {
                                boolean success = object.has("fid") && object.has("fileName")
                                        && object.has("size") && object.has("fileUrl");
                                if (success) {
                                    String fid = object.optString("fid");
                                    String filename = object.optString("fileName");
                                    long size = object.optLong("size");

                                    String[] temp1 = filename.split("-");

                                    FileMeta fileMeta = FileMeta.create(
                                            Short.parseShort(temp1[0]),
                                            temp1[1],
                                            temp1[2],
                                            temp1[3],
                                            temp1[4],
                                            Short.parseShort(temp1[5]),
                                            Integer.parseInt(temp1[6]),
                                            fid,
                                            size
                                    );

                                    repository.save(fileMeta);
                                    String filePath = dirPath + "/" + filename;
                                    FileUtils.deleteQuietly(new File(filePath));
                                }
                            }
                            weedFSFile = null;
                        }
                    } catch (Exception e) {
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
