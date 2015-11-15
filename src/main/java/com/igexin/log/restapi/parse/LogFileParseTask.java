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

    private GraylogClientService graylogService;

    private WeedFSClientService weedService;

    private LogFileProperties properties;

    private AtomicBoolean decryptFailed = new AtomicBoolean(false);

    public static LogFileParseTask create(final LogFileProperties properties,
                                          final GraylogClientService graylogService,
                                          final WeedFSClientService weedService) {
        LogFileParseTask task = new LogFileParseTask();
        task.properties = properties;
        task.graylogService = graylogService;
        task.weedService = weedService;
        return task;
    }

    @Override
    public void run() {
        if (properties == null) {
            return;
        }
        String currentPath = properties.tempPath();
        String inFilePath = currentPath + "/" + properties.getFilename();
        String unzipFilename = StringUtil.randomUid();

        // Check temp dir exist.
        File tempDir = new File(currentPath);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                return;
            }
        }

        String outFilePath = currentPath + "/" + unzipFilename;
        boolean successful = GzipTool.decompress(inFilePath, outFilePath);
        if (successful) {
            LocalFileSender localFileSender = LocalFileSender.create(properties);
            senderList.add(localFileSender);

            // 日志级别为 verbose 的默认不发送到 graylog.
            if (properties.getLevel() != Constants.VERBOSE) {
                senderList.add(graylogService);
            }

            LogFileParser parser = new LogFileParser();
            parser.setListener(this);
            parser.parse(outFilePath);
        }

        // Delete upload zip file.
        File file = new File(inFilePath);
        FileUtils.deleteQuietly(file);
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
    public void result(String inFilePath, boolean successful) {
        for (SenderInterface object : senderList) {
            if (object instanceof LocalFileSender) {
                LocalFileSender sender = (LocalFileSender) object;
                try {
                    sender.release();
                    LogFileProperties properties = sender.getProperties();
                    if (properties != null) {
                        if (successful && !decryptFailed.get()) {
                            WeedFSMeta meta = properties.create();
                            if (meta != null) {
                                weedService.write(meta);
                            }
                        } else {
                            // Decrypt log file failed.
                            File destDir = new File(properties.errorPath());
                            try {
                                FileUtils.moveFileToDirectory(new File(inFilePath), destDir, true);
                            } catch (IOException e) {
                                LOG.error("Exception", e);
                            }

                            // Delete decrypt failed failed.
                            FileUtils.deleteQuietly(new File(properties.outFilePath()));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Exception", e);
                }
            }
        }
        // Delete encrypted file.
        FileUtils.deleteQuietly(new File(inFilePath));
    }

}
