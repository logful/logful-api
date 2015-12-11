package com.getui.logful.server.weed;

import com.getui.logful.server.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WeedFSWriteThread {

    private final ReentrantLock lock;

    private final Condition connectedCond;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread writerThread;

    private Channel channel;

    public WeedFSWriteThread(final URI uri,
                             final String dirPath,
                             final WeedQueueRepository queueRepository,
                             final BlockingQueue<WeedFSMeta> queue) {
        this.lock = new ReentrantLock();
        this.connectedCond = lock.newCondition();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                WeedFSMeta weedFSMeta = null;

                while (running.get()) {
                    lock.lock();
                    while (channel == null || !channel.isActive()) {
                        try {
                            connectedCond.await();
                        } catch (InterruptedException e) {
                            if (!running.get()) {
                                break;
                            }
                        }
                    }

                    try {
                        if (weedFSMeta == null) {
                            weedFSMeta = queue.poll(100, TimeUnit.MILLISECONDS);
                        }
                        if (weedFSMeta != null && channel != null && channel.isActive()
                                && uri != null && !StringUtil.isEmpty(dirPath)) {
                            try {
                                String filePath = dirPath + "/" + weedFSMeta.filename();
                                File file = new File(filePath);
                                if (file.exists()) {
                                    String boundary = UUID.randomUUID().toString();
                                    String start = "--" + boundary + "\r\n"
                                            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n"
                                            + "\r\n";
                                    String end = "\r\n" + "--" + boundary + "--\r\n";

                                    ByteBuf buffer = Unpooled.wrappedBuffer(start.getBytes(),
                                            FileUtils.readFileToByteArray(file),
                                            end.getBytes());

                                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                            HttpMethod.POST,
                                            uri.toASCIIString(),
                                            buffer);

                                    HttpHeaders headers = request.headers();
                                    headers.set("Connection", "keep-alive");
                                    headers.set("Content-Type", "multipart/form-data; boundary=" + boundary);
                                    headers.set("Content-Length", request.content().readableBytes());

                                    channel.writeAndFlush(request);
                                } else {
                                    if (!StringUtil.isEmpty(weedFSMeta.getId())) {
                                        weedFSMeta.setStatus(WeedFSMeta.STATE_DELETED);
                                        queueRepository.save(weedFSMeta);
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore any exception.
                            } finally {
                                weedFSMeta = null;
                            }
                        }
                    } catch (InterruptedException e) {
                        // Ignore exception.
                    }
                    lock.unlock();
                }
            }
        };

        this.writerThread = new Thread(runnable);
        writerThread.setName("weed-fs-writer-" + this.writerThread.getId());
        writerThread.setDaemon(true);
    }

    public void start(Channel channel) {
        lock.lock();
        try {
            this.channel = channel;
            this.connectedCond.signalAll();
        } finally {
            lock.unlock();
        }
        writerThread.start();
    }

    public void stop() {
        running.set(false);
        writerThread.interrupt();
    }

}
