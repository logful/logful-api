package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.util.CryptoTool;
import com.igexin.log.restapi.util.GzipTool;
import com.igexin.log.restapi.util.StringUtil;
import com.igexin.log.restapi.weed.WeedFSClientService;
import com.igexin.log.restapi.weed.WeedFSMeta;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogFileParseTask implements Runnable, LogFileParser.ParserEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(LogFileParseTask.class);

    private List<SenderInterface> senderList = new ArrayList<>();

    private AtomicBoolean decryptFailed = new AtomicBoolean(false);

    private String originalFilePath;

    private String unzipFilePath;

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
        String currentPath = properties.tempPath();

        originalFilePath = currentPath + "/" + properties.getFilename();

        String unzipFilename = StringUtil.randomUid();
        // Check temp dir exist.
        File tempDir = new File(currentPath);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                return;
            }
        }

        unzipFilePath = currentPath + "/" + unzipFilename;
        boolean successful = GzipTool.decompress(originalFilePath, unzipFilePath);
        if (successful) {
            // 日志级别为 verbose 的默认不发送到 graylog.
            if (properties.getLevel() != Constants.VERBOSE) {
                senderList.add(graylogService);
            }

            senderList.add(localFileSender);

            new LogFileParser(this).parse(unzipFilePath);
        } else {
            FileUtils.deleteQuietly(new File(originalFilePath));
            FileUtils.deleteQuietly(new File(unzipFilePath));
        }
    }

    @Override
    public void output(long timestamp, String encryptedTag, String encryptedMsg, short layoutId, int attachmentId) {
        if (decryptFailed.get()) {
            return;
        }

        String platform = properties.getPlatform();
        String appId = properties.getAppId();
        int level = properties.getLevel();
        String uid = properties.getUid();
        String name = properties.getLoggerName();
        String layout = properties.getLayout(layoutId);

        String alias = properties.getAlias();

        String tag = CryptoTool.AESDecrypt(appId, encryptedTag);
        String msg = CryptoTool.AESDecrypt(appId, encryptedMsg);

        if (StringUtil.decryptError(tag) || StringUtil.decryptError(msg)) {
            decryptFailed.set(true);
            return;
        }

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
        if (localFileSender != null) {
            try {
                localFileSender.release();
            } catch (Exception e) {
                LOG.error("Exception", e);
            }
        }
        if (successful && !decryptFailed.get()) {
            WeedFSMeta meta = properties.create();
            if (meta != null) {
                weedService.write(meta);
            }
        } else {
            File destDir = new File(properties.errorPath());
            if (!StringUtil.isEmpty(unzipFilePath)) {
                try {
                    FileUtils.moveFileToDirectory(new File(unzipFilePath), destDir, true);
                } catch (IOException e) {
                    LOG.error("Exception", e);
                }
            }
        }

        if (!StringUtil.isEmpty(originalFilePath)) {
            FileUtils.deleteQuietly(new File(originalFilePath));
        }
        if (!StringUtil.isEmpty(unzipFilePath)) {
            FileUtils.deleteQuietly(new File(unzipFilePath));
        }
    }

}
