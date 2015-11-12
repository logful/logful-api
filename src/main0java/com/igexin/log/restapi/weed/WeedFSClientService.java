package com.igexin.log.restapi.weed;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.mongod.MongoFileMetaRepository;
import com.igexin.log.restapi.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class WeedFSClientService implements ChannelFutureListener {

    private static final Logger LOG = LoggerFactory.getLogger(WeedFSClientService.class);

    @Autowired
    LogfulProperties logfulProperties;

    @Autowired
    MongoFileMetaRepository mongoFileMetaRepository;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory(getClass(), true));

    private final BlockingQueue<WeedFSFile> writeQueue = new LinkedBlockingQueue<>(Constants.WEED_QUEUE_CAPACITY);

    private final BlockingQueue<WeedFSFile> handleQueue = new LinkedBlockingQueue<>(Constants.WEED_QUEUE_CAPACITY);

    @PostConstruct
    public void create() {
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
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.remoteAddress(socketAddress);

        URI uri;
        try {
            uri = new URI(String.format("http://%s:%d/submit?ttl=%s", host, port, logfulProperties.weedTimeToLive()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Weed fs submit uri syntax error!");
        }

        String weedDir = logfulProperties.weedDir();
        final WeedFSWriterThread weedFSWriterThread = new WeedFSWriterThread(uri, weedDir, writeQueue);
        final WeedFSHandlerThread weedFSHandlerThread = new WeedFSHandlerThread(weedDir, mongoFileMetaRepository, handleQueue);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel channel) throws Exception {
                channel.pipeline().addLast(new HttpResponseDecoder());
                channel.pipeline().addLast(new HttpRequestEncoder());
                channel.pipeline().addLast(new SimpleChannelInboundHandler<HttpObject>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                        if (msg instanceof HttpContent) {
                            HttpContent httpContent = (HttpContent) msg;

                            ByteBuf content = httpContent.content();
                            if (content.isReadable()) {
                                byte[] result = new byte[content.readableBytes()];
                                content.readBytes(result);

                                // Put handler thread to process success archive log file.
                                write(new WeedFSFile(result));
                            }
                        }
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        weedFSWriterThread.start(ctx.channel());
                        weedFSHandlerThread.start();

                        LOG.info("Weed fs socket channel connected!");
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        weedFSWriterThread.stop();
                        weedFSHandlerThread.stop();

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
        }, Constants.RECONNECT_WEED_DELAY, TimeUnit.SECONDS);
    }

    public void write(WeedFSFile file) {
        try {
            writeQueue.put(file);
        } catch (InterruptedException e) {
            LOG.error("Exception", e);
        }
    }
}
