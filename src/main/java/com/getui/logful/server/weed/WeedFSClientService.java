package com.getui.logful.server.weed;

import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.AttachFileMeta;
import com.getui.logful.server.entity.LogFileMeta;
import com.getui.logful.server.mongod.AttachFileMetaRepository;
import com.getui.logful.server.mongod.LogFileMetaRepository;
import com.getui.logful.server.util.StringUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WeedFSClientService implements ChannelFutureListener {

    private static final Logger LOG = LoggerFactory.getLogger(WeedFSClientService.class);

    private static Queue queue;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory(getClass(), true));

    private final ConcurrentHashMap<String, WeedFSMeta> weedMetaMap = new ConcurrentHashMap<>();

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private LogFileMetaRepository logFileMetaRepository;

    @Autowired
    private AttachFileMetaRepository attachFileMetaRepository;

    @Autowired
    private WeedQueueRepository weedQueueRepository;

    private AtomicBoolean connected = new AtomicBoolean(false);

    private AtomicBoolean serverError = new AtomicBoolean(false);

    public ConcurrentHashMap<String, WeedFSMeta> getWeedMetaMap() {
        return weedMetaMap;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isServerError() {
        return serverError.get();
    }

    public void resetServerError() {
        serverError.set(false);
    }

    public int writeQueueSize() {
        return queue.writeQueue.size();
    }

    @PostConstruct
    public void create() {
        MongoOperations operations = logFileMetaRepository.getOperations();
        try {
            DBCollection collection;

            BasicDBObject index = new BasicDBObject("writeDate", 1);
            BasicDBObject options = new BasicDBObject("expireAfterSeconds", logfulProperties.expires());

            collection = operations.getCollection(operations.getCollectionName(LogFileMeta.class));
            collection.createIndex(index, options);

            collection = operations.getCollection(operations.getCollectionName(AttachFileMeta.class));
            collection.createIndex(index, options);

            collection = operations.getCollection(operations.getCollectionName(WeedFSMeta.class));
            collection.createIndex(index, options);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
        queue = new Queue(logfulProperties);
        createBootstrap(workerGroup);
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            LOG.debug("Weed master connected!");
        } else {
            LOG.error("Weed master connected failed: {}", future.cause().getLocalizedMessage());
            // Reconnect weed fs.
            reconnect(future.channel().eventLoop());
        }
    }

    public void createBootstrap(EventLoopGroup eventLoopGroup) {
        String host = logfulProperties.weedMasterHost();
        int port = logfulProperties.weedMasterPort();
        if (StringUtil.isEmpty(host)) {
            throw new IllegalArgumentException("Weed master host can not be null!");
        }

        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("Weed master port Not valid!");
        }

        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, logfulProperties.getWeed().getConnectTimeout());
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.remoteAddress(socketAddress);

        URI uri;
        try {
            uri = new URI(String.format("http://%s:%d/submit?ttl=%s", host, port, logfulProperties.getTtl()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Weed fs submit uri syntax error!");
        }

        String weedDir = logfulProperties.weedDir();
        final WeedFSWriteThread writeThread = new WeedFSWriteThread(uri, weedDir, weedQueueRepository, queue.writeQueue);
        final WeedFSReadThread readThread = new WeedFSReadThread(weedDir,
                logFileMetaRepository,
                attachFileMetaRepository,
                weedQueueRepository,
                weedMetaMap,
                queue.readQueue);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel channel) throws Exception {
                channel.pipeline().addLast(new HttpResponseDecoder());
                channel.pipeline().addLast(new HttpRequestEncoder());
                channel.pipeline().addLast("aggregator", new HttpObjectAggregator(65535));
                channel.pipeline().addLast(new SimpleChannelInboundHandler<HttpObject>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                        if (msg instanceof FullHttpResponse) {
                            FullHttpResponse response = (FullHttpResponse) msg;
                            HttpResponseStatus status = response.getStatus();
                            if (status.code() == 201) {
                                serverError.set(false);
                                // Created
                                ByteBuf content = response.content();
                                if (content != null && content.isReadable()) {
                                    byte[] result = new byte[content.readableBytes()];
                                    content.readBytes(result);
                                    try {
                                        queue.readQueue.put(WeedFSMeta.create(result));
                                    } catch (InterruptedException e) {
                                        LOG.error("Exception", e);
                                    }
                                }
                            } else if (status.code() == 500) {
                                // TODO
                                serverError.set(true);
                            }
                        }
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        connected.set(true);
                        serverError.set(false);

                        writeThread.start(ctx.channel());
                        readThread.start();

                        LOG.info("Weed fs socket channel connected!");
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        connected.set(false);

                        writeThread.stop();
                        readThread.stop();

                        reconnect(ctx.channel().eventLoop());
                        LOG.info("Will reconnect weed fs!");
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        ctx.close();
                        LOG.error("Exception", cause);
                    }
                });
            }
        });
        bootstrap.connect().addListener(this);
    }

    public void reconnect(final EventLoopGroup eventLoopGroup) {
        eventLoopGroup.schedule(new Runnable() {
            @Override
            public void run() {
                createBootstrap(eventLoopGroup);
            }
        }, logfulProperties.getWeed().getReconnectDelay(), TimeUnit.MILLISECONDS);
    }

    public void write(WeedFSMeta meta) {
        if (connected.get()) {
            if (!serverError.get()) {
                try {
                    queue.writeQueue.put(meta);
                    weedMetaMap.put(meta.getKey(), meta);
                } catch (InterruptedException e) {
                    LOG.error("Exception", e);
                }
            } else {
                // Weed server unexpected error just delete file.
                File file = new File(logfulProperties.weedDir() + "/" + meta.filename());
                FileUtils.deleteQuietly(file);
            }
        } else {
            weedQueueRepository.save(meta);
        }
    }

    private static class Queue {

        public final BlockingQueue<WeedFSMeta> writeQueue;

        public final BlockingQueue<WeedFSMeta> readQueue;

        public Queue(LogfulProperties properties) {
            this.writeQueue = new LinkedBlockingQueue<>(properties.getWeed().getQueueCapacity());
            this.readQueue = new LinkedBlockingQueue<>(properties.getWeed().getQueueCapacity());
        }
    }
}
