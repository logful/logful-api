package com.igexin.log.restapi.weed;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.entity.FileMeta;
import com.igexin.log.restapi.mongod.MongoFileMetaRepository;
import com.igexin.log.restapi.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class WeedFSHandlerThread {

    private final ReentrantLock lock;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread handlerThread;

    private final BlockingQueue<WeedFSFile> queue;

    private String workingDir;

    @Autowired
    MongoFileMetaRepository mongoFileMetaRepository;

    public WeedFSHandlerThread() {
        this.lock = new ReentrantLock();
        this.queue = new LinkedBlockingQueue<WeedFSFile>(Constants.QUEUE_CAPACITY);

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

                        if (weedFSFile != null && !StringUtil.isEmpty(workingDir)) {
                            JSONObject object = weedFSFile.responseObject();
                            if (object != null) {
                                boolean success = object.has("fid") && object.has("fileName")
                                        && object.has("size") && object.has("fileUrl");
                                if (success) {
                                    String fid = object.optString("fid");
                                    String filename = object.optString("fileName");
                                    long size = object.optLong("size");
                                    String fileUrl = object.optString("fileUrl");

                                    String[] temp1 = filename.split("-");
                                    String[] temp2 = fileUrl.split("/")[0].split(":");

                                    FileMeta fileMeta = FileMeta.create(
                                            Short.parseShort(temp1[0]),
                                            temp1[1],
                                            temp1[2],
                                            temp1[3],
                                            temp1[4],
                                            Short.parseShort(temp1[5]),
                                            Integer.parseInt(temp1[6]),
                                            temp2[0],
                                            Short.parseShort(temp2[1]),
                                            fid,
                                            size
                                    );

                                    mongoFileMetaRepository.save(fileMeta);
                                    String filePath = workingDir + "/" + filename;
                                    FileUtils.deleteQuietly(new File(filePath));
                                }
                            }
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

    public void config(String dirPath) {
        this.workingDir = dirPath;
    }

    public void start() {
        handlerThread.start();
    }

    public void handle(WeedFSFile weedFSFile) throws InterruptedException {
        queue.put(weedFSFile);
    }

    public void stop() {
        running.set(false);
        handlerThread.interrupt();
    }

}
