package com.getui.logful.server.parse;

import com.getui.logful.server.Constants;
import com.getui.logful.server.entity.LogLine;
import com.getui.logful.server.util.StringUtil;
import com.getui.logful.server.weed.WeedFSClientService;
import com.getui.logful.server.weed.WeedFSMeta;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class LogFileParseTask implements Runnable, LogFileParser.ParserEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(LogFileParseTask.class);

    private List<SenderInterface> senderList = new ArrayList<>();

    private final LogFileProperties properties;

    private final WeedFSClientService weedService;

    private GraylogClientService graylogService;

    private final LocalFileSender localFileSender;

    public LogFileParseTask(final LogFileProperties properties,
                            final GraylogClientService graylogService,
                            final WeedFSClientService weedService) {
        this.properties = properties;
        this.graylogService = graylogService;
        this.weedService = weedService;
        this.localFileSender = LocalFileSender.create(properties);
    }

    @Override
    public void run() {
        if (properties == null) {
            return;
        }
        try {
            GZIPInputStream stream = new GZIPInputStream(new FileInputStream(properties.cacheFilePath()));
            // 日志级别为 verbose 的默认不发送到 graylog.
            if (properties.getLevel() != Constants.VERBOSE) {
                senderList.add(graylogService);
            }
            senderList.add(localFileSender);
            new LogFileParser(this).parse(properties.getAppId(), properties.getCryptoVersion(), stream);
        } catch (Exception e) {
            LOG.error("Exception", e);
            // TODO
            moveToErrorDir();
        }
    }

    @Override
    public void output(long timestamp, String tag, String msg, short layoutId, int attachmentId) {
        String platform = properties.getPlatform();
        String appId = properties.getAppId();
        int level = properties.getLevel();
        String uid = properties.getUid();
        String name = properties.getLoggerName();
        String layout = properties.getLayout(layoutId);
        String alias = properties.getAlias();

        String attachment = null;
        if (attachmentId != -1) {
            attachment = StringUtil.attachmentKey(platform, uid, appId, String.valueOf(attachmentId));
        }
        LogLine logLine = LogLine.create(platform, uid, appId,
                name, layout, level, timestamp, tag, msg, alias,
                attachment);
        for (SenderInterface sender : senderList) {
            sender.send(logLine);
        }
    }

    @Override
    public void result(boolean successful) {
        try {
            localFileSender.release();
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        if (successful) {
            WeedFSMeta meta = properties.create();
            if (meta != null) {
                weedService.write(meta);
            }
        } else {
            // TODO
            moveToErrorDir();
        }

        FileUtils.deleteQuietly(new File(properties.cacheFilePath()));
    }

    private void moveToErrorDir() {
        try {
            File destDir = new File(properties.errorPath());
            FileUtils.moveFileToDirectory(new File(properties.cacheFilePath()), destDir, true);
        } catch (IOException ex) {
            LOG.error("Exception", ex);
        }
    }

}
