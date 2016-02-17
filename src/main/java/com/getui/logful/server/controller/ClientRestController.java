package com.getui.logful.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.ServerProperties;
import com.getui.logful.server.auth.ApplicationKeyPairManager;
import com.getui.logful.server.entity.AttachFileMeta;
import com.getui.logful.server.entity.ClientUser;
import com.getui.logful.server.entity.CrashFileMeta;
import com.getui.logful.server.entity.GlobalConfig;
import com.getui.logful.server.mongod.ClientUserRepository;
import com.getui.logful.server.mongod.GlobalConfigRepository;
import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.parse.LogFileParseTask;
import com.getui.logful.server.parse.LogFileProperties;
import com.getui.logful.server.rest.BaseRestController;
import com.getui.logful.server.util.*;
import com.getui.logful.server.weed.WeedFSClientService;
import com.getui.logful.server.weed.WeedFSMeta;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
public class ClientRestController extends BaseRestController {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRestController.class);

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private ClientUserRepository clientUserRepository;

    @Autowired
    private GlobalConfigRepository globalConfigRepository;

    @Autowired
    GraylogClientService graylogClientService;

    @Autowired
    WeedFSClientService weedFSClientService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    ApplicationKeyPairManager applicationKeyPairManager;

    @RequestMapping(value = "/log/info/upload",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String clientUserReport(@RequestBody String payload) {
        JSONObject object = new JSONObject(payload);
        String sdkVersion = object.optString("sdkVersion");
        String signature = object.optString("signature");
        String chunk = object.optString("chunk");
        if (StringUtils.isEmpty(sdkVersion) || StringUtils.isEmpty(signature) || StringUtils.isEmpty(chunk)) {
            throw new BadRequestException();
        }
        int apiVersion = VersionUtil.version(sdkVersion);
        if (apiVersion == VersionUtil.API_VERSION_V1) {
            String content = decryptPayload(signature, chunk);
            if (StringUtils.isEmpty(content)) {
                throw new BadRequestException();
            }

            ClientUser clientUser = genClientUser(content);
            if (clientUser == null) {
                throw new BadRequestException();
            }

            String clientId = applicationKeyPairManager.getClientId();
            if (!StringUtils.isEmpty(clientId)) {
                clientUser.setClientId(clientId);
                return v1ClientUserReport(clientUser);
            } else {
                throw new InternalServerException();
            }
        } else {
            throw new BadRequestException();
        }
    }

    /**
     * Upload log file.
     *
     * @param payload Payload data
     * @param logFile Encrypt log file
     */
    @RequestMapping(value = "/log/file/upload",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
    public String uploadLogFile(@RequestParam("payload") String payload,
                                @RequestParam("logFile") MultipartFile logFile) {
        JSONObject object = new JSONObject(payload);
        String sdkVersion = object.optString("sdkVersion");
        String signature = object.optString("signature");
        String chunk = object.optString("chunk");
        if (StringUtils.isEmpty(sdkVersion) || StringUtils.isEmpty(signature) || StringUtils.isEmpty(chunk)) {
            throw new BadRequestException();
        }
        int apiVersion = VersionUtil.version(sdkVersion);
        if (apiVersion == VersionUtil.API_VERSION_V1) {
            byte[] security = applicationKeyPairManager.decrypt(Base64.decodeBase64(signature));
            if (security == null) {
                throw new BadRequestException();
            }

            String content = decryptPayload(security, chunk);
            if (StringUtils.isEmpty(content)) {
                throw new BadRequestException();
            }

            LogFileProperties properties = genProperties(content, logFile.getOriginalFilename(), security);
            if (properties == null) {
                throw new BadRequestException();
            }

            v1UploadLogFile(properties, logFile);
            return accepted();
        } else {
            throw new BadRequestException("Unknown version!");
        }
    }

    /**
     * Upload crash report file.
     *
     * @param payload    Payload
     * @param reportFile Crash report file
     */
    @RequestMapping(value = "/log/crash/upload",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String uploadCrashReport(@RequestParam("payload") String payload,
                                    @RequestParam("reportFile") MultipartFile reportFile) {
        JSONObject object = new JSONObject(payload);
        String sdkVersion = object.getString("sdkVersion");
        int apiVersion = VersionUtil.version(sdkVersion);
        if (apiVersion == VersionUtil.API_VERSION_V1) {
            String fileSum = object.getString("fileSum");
            JSONObject meta = object.getJSONObject("meta");
            ObjectMapper mapper = new ObjectMapper();
            try {
                CrashFileMeta fileMeta = mapper.readValue(meta.toString(), CrashFileMeta.class);
                fileMeta.setClientId(applicationKeyPairManager.getClientId());
                v1UploadCrashReport(fileMeta, fileSum, reportFile);
                return ok();
            } catch (Exception e) {
                throw new BadRequestException();
            }
        } else {
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
     */
    @RequestMapping(value = "/log/attachment/upload",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String uploadAttachment(@RequestParam("sdkVersion") String sdkVersion,
                                   @RequestParam("platform") String platform,
                                   @RequestParam("uid") String uid,
                                   @RequestParam("appId") String appId,
                                   @RequestParam("fileSum") String fileSum,
                                   @RequestParam("attachmentId") String attachmentId,
                                   @RequestParam("attachmentFile") MultipartFile attachmentFile) {
        int apiVersion = VersionUtil.version(sdkVersion);
        if (apiVersion == VersionUtil.API_VERSION_V1) {
            v1UploadAttachment(StringUtil.platformNumber(platform), uid, appId, fileSum, attachmentId, attachmentFile);
            return ok();
        } else {
            throw new BadRequestException("Unknown version!");
        }
    }

    @Deprecated
    @RequestMapping(value = "/log/uploadLogFile",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void oldUploadLogFile(@RequestParam("platform") String platform,
                                 @RequestParam("uid") String uid,
                                 @RequestParam("appId") String appId,
                                 @RequestParam("loggerName") String loggerName,
                                 @RequestParam("layouts") String layouts,
                                 @RequestParam("level") String level,
                                 @RequestParam("alias") String alias,
                                 @RequestParam("fileSum") String fileSum,
                                 @RequestParam("logFile") MultipartFile logFile) {
        JSONObject object = new JSONObject();
        object.put("platform", StringUtil.platformNumber(platform));
        object.put("uid", uid);
        object.put("appId", appId);
        object.put("loggerName", loggerName);
        object.put("layouts", layouts);
        object.put("level", level);
        object.put("fileSum", fileSum);
        object.put("alias", alias);

        LogFileProperties properties = genProperties(object.toString(), logFile.getOriginalFilename(), appId.getBytes());
        if (properties == null) {
            throw new BadRequestException();
        }

        properties.setCompatible(true);
        v1UploadLogFile(properties, logFile);
    }

    // ---------------------------------- Detail rest controller version function api ------------------------------ //

    private String v1ClientUserReport(final ClientUser clientUser) {
        clientUserRepository.save(clientUser);
        GlobalConfig config = globalConfigRepository.read();

        boolean granted = config.getGrantClients().contains(clientUser.getClientId()) &&
                clientUser.getLevel() < config.getLevel();

        JSONObject object = new JSONObject();
        object.put("granted", granted);

        return object.toString();
        /*
        ClientUser info = ClientUser.create(webRequest);
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
        */
    }

    private void v1UploadLogFile(LogFileProperties properties,
                                 MultipartFile logFile) {
        if (properties == null) {
            throw new BadRequestException();
        }
        int queueSize = threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size();
        int capacity = BaseRestController.queueCapacity(serverProperties.getParser().getQueueCapacity());
        if (queueSize < capacity) {
            String filePath = serverProperties.tempDir() + "/" + properties.getFilename();
            File file = new File(filePath);
            try {
                FileUtil.transferTo(logFile, file);
                String sum = Checksum.fileMD5(file.getAbsolutePath());
                if (!sum.equalsIgnoreCase(properties.getFileSum())) {
                    throw new InternalServerException();
                }
            } catch (Exception e) {
                throw new InternalServerException();
            }

            LogFileParseTask task = new LogFileParseTask(properties, graylogClientService, weedFSClientService);
            threadPoolTaskExecutor.submit(task);
        } else {
            throw new InternalServerException();
        }
    }

    private void v1UploadCrashReport(CrashFileMeta fileMeta,
                                     String fileSum,
                                     MultipartFile reportFile) {
        if (weedFSClientService.writeQueueSize() < serverProperties.getWeed().getQueueCapacity()) {
            String key = StringUtil.randomUid();
            String extension = FilenameUtils.getExtension(reportFile.getOriginalFilename());
            if (StringUtils.isNotEmpty(extension)) {
                String filePath = serverProperties.weedDir() + "/" + key + "." + extension;
                File file = new File(filePath);
                try {
                    FileUtil.transferTo(reportFile, file);
                    String fileSumString = Checksum.fileMD5(file.getAbsolutePath());
                    if (!fileSumString.equalsIgnoreCase(fileSum)) {
                        throw new InternalServerException();
                    }
                    weedFSClientService.write(WeedFSMeta.create(key, extension, fileMeta));
                } catch (Exception e) {
                    throw new InternalServerException();
                }
            } else {
                throw new BadRequestException();
            }
        } else {
            throw new InternalServerException();
        }
    }

    private void v1UploadAttachment(int platform,
                                    String uid,
                                    String appId,
                                    String fileSum,
                                    String attachmentId,
                                    MultipartFile attachmentFile) {
        if (weedFSClientService.writeQueueSize() < serverProperties.getWeed().getQueueCapacity()) {
            String key = StringUtil.attachmentKey(platform, uid, appId, attachmentId);
            String extension = FilenameUtils.getExtension(attachmentFile.getOriginalFilename());
            if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(extension)) {
                String filePath = serverProperties.weedDir() + "/" + key + "." + extension;
                File file = new File(filePath);
                try {
                    FileUtil.transferTo(attachmentFile, file);
                    String fileSumString = Checksum.fileMD5(file.getAbsolutePath());
                    if (!fileSumString.equalsIgnoreCase(fileSum)) {
                        throw new InternalServerException();
                    }
                    weedFSClientService.write(WeedFSMeta.create(key, extension, AttachFileMeta.create(key)));
                } catch (Exception e) {
                    throw new InternalServerException();
                }
            } else {
                throw new BadRequestException();
            }
        } else {
            throw new InternalServerException();
        }
    }

    private LogFileProperties genProperties(String attr, String originalFilename, byte[] security) {
        if (StringUtils.isEmpty(attr) || StringUtils.isEmpty(originalFilename)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            LogFileProperties properties = mapper.readValue(attr, LogFileProperties.class);

            String filename = StringUtil.randomUid();

            properties.setClientId(applicationKeyPairManager.getClientId());
            properties.setSecurity(security);
            properties.setFilename(filename);
            properties.setOriginalFilename(originalFilename);
            properties.setWorkPath(serverProperties.getPath());

            if (StringUtils.isEmpty(properties.getUid()) ||
                    StringUtils.isEmpty(properties.getAppId()) ||
                    StringUtils.isEmpty(properties.getLoggerName()) ||
                    StringUtils.isEmpty(properties.getFileSum())) {
                return null;
            }

            return properties;
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        return null;
    }

    private ClientUser genClientUser(String attr) {
        if (StringUtils.isEmpty(attr)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(attr, ClientUser.class);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        return null;
    }

    private String decryptPayload(String signature, String chunk) {
        byte[] security = applicationKeyPairManager.decrypt(Base64.decodeBase64(signature));
        if (security == null) {
            return null;
        }
        return decryptPayload(security, chunk);
    }

    private String decryptPayload(byte[] security, String chunk) {
        return CryptoTool.AESDecrypt(security, Base64.decodeBase64(chunk));
    }
}
