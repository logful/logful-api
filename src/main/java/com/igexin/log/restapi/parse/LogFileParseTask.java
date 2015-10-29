package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.GlobalReference;
import com.igexin.log.restapi.entity.DecryptError;
import com.igexin.log.restapi.entity.LogFileProperties;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.mongod.MongoDecryptErrorRepository;
import com.igexin.log.restapi.util.CryptoTool;
import com.igexin.log.restapi.util.FileUtil;
import com.igexin.log.restapi.util.GzipTool;
import com.igexin.log.restapi.util.StringUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogFileParseTask implements Runnable, LogFileParser.ParserEventListener {

    public static final int VERBOSE = 0x01;

    private LogFileProperties properties;

    private List<SenderInterface> senderList = new ArrayList<>();

    private boolean decryptFailed = false;

    private DecryptError decryptError = null;

    public static LogFileParseTask create(LogFileProperties properties) {
        LogFileParseTask task = new LogFileParseTask();
        task.setProperties(properties);
        return task;
    }

    public void setProperties(LogFileProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run() {
        if (properties == null) {
            return;
        }
        String workPath = properties.getWorkPath();
        String inFilePath = workPath + "/" + properties.getFilename();
        String unzipFilename = StringUtil.randomUid();

        // Check temp dir exist.
        File tempDir = new File(workPath);
        if (!tempDir.exists()) {
            if (tempDir.mkdirs()) {
                return;
            }
        }

        String outFilePath = workPath + "/" + unzipFilename;
        boolean successful = GzipTool.decompress(inFilePath, outFilePath);
        if (successful) {
            LocalFileSender localFileSender = LocalFileSender.create(properties);
            senderList.add(localFileSender);

            // 日志级别为 verbose 的默认不发送到 graylog.
            if (properties.getLevel() != VERBOSE) {
                GrayLogSender grayLogSender = new GrayLogSender();
                senderList.add(grayLogSender);
            }

            decryptFailed = false;
            decryptError = new DecryptError();
            decryptError.setTimestamp(System.currentTimeMillis());

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
        String platform = properties.getPlatform();
        String appId = properties.getAppId();
        int level = properties.getLevel();
        String uid = properties.getUid();
        String name = properties.getLoggerName();
        String layout = properties.getLayout(layoutId);

        String alias = properties.getAlias();

        String tag = CryptoTool.AESDecrypt(appId, encryptedTag);
        String msg = CryptoTool.AESDecrypt(appId, encryptedMsg);

        if (tag.equalsIgnoreCase(Constants.CRYPTO_ERROR) || msg.equalsIgnoreCase(Constants.CRYPTO_ERROR)) {
            // 解密失败.
            decryptFailed = true;
            decryptError.setUid(uid);
        } else {
            String attachment = null;
            if (attachmentId != -1) {
                attachment = StringUtil.attachmentName(platform, uid, appId, String.valueOf(attachmentId));
            }
            LogLine logLine = LogLine.create(platform, uid, appId,
                    name, layout, level, timestamp, tag, msg, alias,
                    attachment);
            for (SenderInterface sender : senderList) {
                sender.send(logLine);
            }
        }
    }

    @Override
    public void result(String inFilePath, boolean successful) {
        for (SenderInterface sender : senderList) {
            sender.close();
        }
        // 解密失败.
        if (decryptFailed) {
            MongoDecryptErrorRepository repository = GlobalReference.decryptErrorRepository();
            if (repository != null && decryptError != null) {
                DecryptError exist = repository.findByUid(decryptError.getUid());
                if (exist == null) {
                    repository.save(decryptError);
                }
            }
        }
        // 解析文件或解密文件失败.
        if (!successful || decryptFailed) {
            String uid = properties.getUid();
            String appId = properties.getAppId();
            String errorDir = GlobalReference.properties().errorDir(properties.getPlatform());
            String outDirPath = errorDir + "/" + uid + "/" + appId;
            File outDir = new File(outDirPath);
            if (!outDir.exists()) {
                boolean mkDirSuccessful = outDir.mkdirs();
                if (mkDirSuccessful) {
                    String outPath = outDirPath + "/" + properties.getOriginalFilename();
                    FileUtil.copy(inFilePath, outPath);
                }
            }
        }

        // Delete encrypted file.
        File file = new File(inFilePath);
        FileUtils.deleteQuietly(file);
    }

}
