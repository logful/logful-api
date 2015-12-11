package com.getui.logful.server.controller;

import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.Config;
import com.getui.logful.server.entity.ControlProfile;
import com.getui.logful.server.entity.UserInfo;
import com.getui.logful.server.entity.WeedAttachFileMeta;
import com.getui.logful.server.mongod.MongoConfigRepository;
import com.getui.logful.server.mongod.MongoControlProfileRepository;
import com.getui.logful.server.mongod.MongoUserInfoRepository;
import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.parse.LogFileParseTask;
import com.getui.logful.server.parse.LogFileProperties;
import com.getui.logful.server.util.Checksum;
import com.getui.logful.server.util.ControllerUtil;
import com.getui.logful.server.util.StringUtil;
import com.getui.logful.server.util.VersionUtil;
import com.getui.logful.server.weed.WeedFSClientService;
import com.getui.logful.server.weed.WeedFSMeta;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class ClientRestController {

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private MongoUserInfoRepository mongoUserInfoRepository;

    @Autowired
    private MongoConfigRepository mongoConfigRepository;

    @Autowired
    private MongoControlProfileRepository mongoControlProfileRepository;

    @Autowired
    GraylogClientService graylogClientService;

    @Autowired
    WeedFSClientService weedFSClientService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * Upload system info.
     *
     * @param webRequest WebRequest
     * @return Config response
     */
    @RequestMapping(value = "/log/info/upload",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String uploadSystemInfo(final WebRequest webRequest) {
        String platform = webRequest.getParameter("platform");
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        String sdkVersion = webRequest.getParameter("sdkVersion");
        if (StringUtil.isEmpty(sdkVersion)) {
            throw new BadRequestException("No version specify!");
        }

        switch (VersionUtil.version(sdkVersion)) {
            case V1:
                return v1UploadSystemInfo(webRequest);
            default:
                throw new BadRequestException("Unknown version!");
        }
    }

    /**
     * Upload log file.
     *
     * @param sdkVersion Sdk Version
     * @param platform   Platform
     * @param uid        User unique id
     * @param appId      Application id
     * @param loggerName Logger name
     * @param layouts    Message layout template
     * @param level      Log level
     * @param alias      User alias
     * @param fileSum    File MD5 sum
     * @param logFile    Log file
     * @return Response result
     */
    @RequestMapping(value = "/log/file/upload",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String uploadLogFile(@RequestParam("sdkVersion") String sdkVersion,
                                @RequestParam("platform") String platform,
                                @RequestParam("uid") String uid,
                                @RequestParam("appId") String appId,
                                @RequestParam("loggerName") String loggerName,
                                @RequestParam("layouts") String layouts,
                                @RequestParam("level") String level,
                                @RequestParam("alias") String alias,
                                @RequestParam("fileSum") String fileSum,
                                @RequestParam("logFile") MultipartFile logFile) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        switch (VersionUtil.version(sdkVersion)) {
            case V1:
                return v1UploadLogFile(platform, uid, appId, VersionUtil.CRYPTO_UPDATE_2, loggerName, layouts, level, alias, fileSum, logFile);
            default:
                throw new BadRequestException("Unknown version!");
        }
    }

    /**
     * Upload crash report file.
     *
     * @param sdkVersion Sdk Version
     * @param platform   Platform
     * @param uid        User unique id
     * @param appId      Application id
     * @param fileSum    File MD5 sum
     * @param reportFile Crash report file
     * @return Response result
     */
    @RequestMapping(value = "/log/crash/upload",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String uploadCrashReport(@RequestParam("sdkVersion") String sdkVersion,
                                    @RequestParam("platform") String platform,
                                    @RequestParam("uid") String uid,
                                    @RequestParam("appId") String appId,
                                    @RequestParam("fileSum") String fileSum,
                                    @RequestParam("reportFile") MultipartFile reportFile) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        switch (VersionUtil.version(sdkVersion)) {
            case V1:
                return v1UploadCrashReport(platform, uid, appId, fileSum, reportFile);
            default:
                throw new BadRequestException("Unknown version!");
        }
    }

    /**
     * Upload attachment.
     *
     * @param sdkVersion     Sdk Version
     * @param platform       Platform
     * @param uid            User unique id
     * @param appId          Application id
     * @param fileSum        File MD5 sum
     * @param attachmentId   Attachment id
     * @param attachmentFile Attachment file
     * @return Response result
     */
    @RequestMapping(value = "/log/attachment/upload",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String uploadAttachment(@RequestParam("sdkVersion") String sdkVersion,
                                   @RequestParam("platform") String platform,
                                   @RequestParam("uid") String uid,
                                   @RequestParam("appId") String appId,
                                   @RequestParam("fileSum") String fileSum,
                                   @RequestParam("attachmentId") String attachmentId,
                                   @RequestParam("attachmentFile") MultipartFile attachmentFile) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        switch (VersionUtil.version(sdkVersion)) {
            case V1:
                return v1UploadAttachment(platform, uid, appId, fileSum, attachmentId, attachmentFile);
            default:
                throw new BadRequestException("Unknown version!");
        }
    }

    // --------------------------------------------- Old api ------------------------------------------------------ //

    /**
     * Upload system info file
     *
     * @param platform Platform
     * @param uid      User unique id
     * @param appId    Application id
     * @param fileSum  File MD5 sum
     * @param infoFile System info file
     * @return Response result
     */
    @RequestMapping(value = "/log/uploadSystemInfo",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String oldUploadSystemInfo(@RequestParam("platform") String platform,
                                      @RequestParam("uid") String uid,
                                      @RequestParam("appId") String appId,
                                      @RequestParam("fileSum") String fileSum,
                                      @RequestParam("infoFile") MultipartFile infoFile) {
        return "{}";
    }

    /**
     * Upload log file
     *
     * @param platform   Platform
     * @param uid        User unique id
     * @param appId      Application id
     * @param loggerName Logger name
     * @param layouts    Message layout template
     * @param level      Log level
     * @param alias      User alias
     * @param fileSum    File MD5 sum
     * @param logFile    Log file
     * @return Response result
     */
    @RequestMapping(value = "/log/uploadLogFile",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String oldUploadLogFile(@RequestParam("platform") String platform,
                                   @RequestParam("uid") String uid,
                                   @RequestParam("appId") String appId,
                                   @RequestParam("loggerName") String loggerName,
                                   @RequestParam("layouts") String layouts,
                                   @RequestParam("level") String level,
                                   @RequestParam("alias") String alias,
                                   @RequestParam("fileSum") String fileSum,
                                   @RequestParam("logFile") MultipartFile logFile) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        return v1UploadLogFile(platform, uid, appId, VersionUtil.CRYPTO_UPDATE_1, loggerName, layouts, level, alias, fileSum, logFile);
    }

    /**
     * Upload crash report file
     *
     * @param platform   Platform
     * @param uid        User unique id
     * @param appId      Application id
     * @param fileSum    File MD5 sum
     * @param reportFile Crash report file
     * @return Response result
     */
    @RequestMapping(value = "/log/uploadCrashReport",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String oldUploadCrashReport(@RequestParam("platform") String platform,
                                       @RequestParam("uid") String uid,
                                       @RequestParam("appId") String appId,
                                       @RequestParam("fileSum") String fileSum,
                                       @RequestParam("reportFile") MultipartFile reportFile) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        return v1UploadCrashReport(platform, uid, appId, fileSum, reportFile);
    }

    // ---------------------------------- Detail rest controller version function api ------------------------------ //

    private String v1UploadSystemInfo(final WebRequest webRequest) {
        UserInfo info = UserInfo.create(webRequest);
        mongoUserInfoRepository.save(info);

        Config config = mongoConfigRepository.read();

        boolean shouldUpload = false;
        List<ControlProfile> profileList = mongoControlProfileRepository.findAllByUser(info);
        List<Long> scheduleTime = new ArrayList<>();
        List<String> scheduleDate = new ArrayList<>();
        for (ControlProfile profile : profileList) {
            if (profile.getShouldUpload()) {
                shouldUpload = true;
                Integer scheduleType = profile.getScheduleType();
                if (scheduleType == 1) {
                    scheduleDate.addAll(profile.scheduleDate());
                } else if (scheduleType == 2) {
                    scheduleTime.add(profile.getScheduleTime());
                }
            } else {
                break;
            }
        }

        JSONObject object = new JSONObject();
        object.put("level", info.getLevel());
        object.put("targetLevel", config.getLevel());
        if (shouldUpload) {
            if (scheduleTime.size() > 0) {
                Collections.sort(scheduleTime);
                JSONObject temp1 = new JSONObject();
                temp1.put("scheduleType", 2);
                temp1.put("scheduleTime", scheduleTime.get(scheduleTime.size() - 1));
                object.put("shouldUpload", true);
                object.put("schedule", temp1);
            } else {
                if (scheduleDate.size() > 0) {
                    JSONArray scheduleArray = new JSONArray();
                    for (String timeString : scheduleDate) {
                        JSONObject dateObject = new JSONObject();
                        dateObject.put("timeString", timeString);
                        scheduleArray.put(dateObject);
                    }
                    JSONObject temp2 = new JSONObject();
                    temp2.put("scheduleType", 1);
                    temp2.put("scheduleArray", scheduleArray);
                    object.put("shouldUpload", true);
                    object.put("schedule", temp2);
                } else {
                    object.put("shouldUpload", false);
                }
            }
        } else {
            object.put("shouldUpload", true);
        }
        return object.toString();
    }

    private String v1UploadLogFile(String platform,
                                   String uid,
                                   String appId,
                                   int cryptoVersion,
                                   String loggerName,
                                   String layouts,
                                   String level,
                                   String alias,
                                   String fileSum,
                                   MultipartFile logFile) {
        int queueSize = threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size();
        int capacity = ControllerUtil.queueCapacity(logfulProperties.getParser().getQueueCapacity());
        if (queueSize < capacity) {
            String tempDirPath = logfulProperties.tempDir();
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                if (tempDir.mkdirs()) {
                    throw new ServerException();
                }
            }

            String filename = StringUtil.randomUid();
            String originalFilename = logFile.getOriginalFilename();
            String filePath = tempDirPath + "/" + filename;
            File file = new File(filePath);
            try {
                logFile.transferTo(file);
                // Check uploaded file sum.
                String sum = Checksum.fileMD5(file.getAbsolutePath());
                if (!sum.equalsIgnoreCase(fileSum)) {
                    throw new ServerException();
                }
            } catch (IOException e) {
                throw new ServerException();
            }

            LogFileProperties properties = new LogFileProperties();
            properties.setPlatform(platform);
            properties.setUid(uid);
            properties.setAppId(appId);
            properties.setCryptoVersion(cryptoVersion);
            properties.setLevel(Integer.parseInt(level));
            properties.setLoggerName(loggerName);
            properties.setAlias(alias);
            properties.setLayouts(layouts);
            properties.setFilename(filename);
            properties.setOriginalFilename(originalFilename);
            properties.setWorkPath(logfulProperties.getPath());

            LogFileParseTask task = new LogFileParseTask(properties, graylogClientService, weedFSClientService);
            threadPoolTaskExecutor.submit(task);

            return responseJson(0, "");
        } else {
            throw new ServerException();
        }
    }

    private String v1UploadCrashReport(String platform,
                                       String uid,
                                       String appId,
                                       String fileSum,
                                       MultipartFile reportFile) {
        String reportDirPath = logfulProperties.crashReportDir(platform) + "/" + appId + "/" + uid;
        File reportDir = new File(reportDirPath);
        if (!reportDir.exists()) {
            if (!reportDir.mkdirs()) {
                // Create crash report dir failed
                throw new ServerException();
            }
        }

        File file = new File(reportDirPath + "/" + reportFile.getOriginalFilename());
        try {
            reportFile.transferTo(file);
            // Check uploaded file sum
            String fileSumString = Checksum.fileMD5(file.getAbsolutePath());
            if (!fileSumString.equalsIgnoreCase(fileSum)) {
                throw new ServerException();
            }
        } catch (IOException e) {
            throw new ServerException();
        }

        //TODO

        return responseJson(0, "");
    }

    private String v1UploadAttachment(String platform,
                                      String uid,
                                      String appId,
                                      String fileSum,
                                      String attachmentId,
                                      MultipartFile attachmentFile) {
        if (weedFSClientService.writeQueueSize() < logfulProperties.getWeed().getQueueCapacity()) {
            String key = StringUtil.attachmentKey(platform, uid, appId, attachmentId);
            String extension = FilenameUtils.getExtension(attachmentFile.getOriginalFilename());
            if (!StringUtil.isEmpty(key) && !StringUtil.isEmpty(extension)) {
                File dir = new File(logfulProperties.weedDir());
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw new ServerException();
                    }
                }
                String filePath = logfulProperties.weedDir() + "/" + key + "." + extension;
                File file = new File(filePath);
                try {
                    attachmentFile.transferTo(file);
                    // Check uploaded file sum
                    String fileSumString = Checksum.fileMD5(file.getAbsolutePath());
                    if (!fileSumString.equalsIgnoreCase(fileSum)) {
                        throw new ServerException();
                    }
                    // Write attachment file to weed fs.
                    weedFSClientService.write(WeedFSMeta.create(key, extension, WeedAttachFileMeta.create(key)));
                } catch (IOException e) {
                    throw new ServerException();
                }
            } else {
                throw new BadRequestException();
            }
            return responseJson(0, "");
        } else {
            throw new ServerException();
        }
    }

    /**
     * Response json result.
     *
     * @param result      Result code
     * @param description Description message
     * @return JSON string
     */
    private String responseJson(int result, String description) {
        JSONObject object = new JSONObject();
        object.put("result", result);
        object.put("description", description);
        return object.toString();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class BadRequestException extends RuntimeException {

        public BadRequestException() {
            super();
        }

        public BadRequestException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public class ServerException extends RuntimeException {

    }

}
