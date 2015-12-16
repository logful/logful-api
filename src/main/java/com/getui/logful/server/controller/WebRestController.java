package com.getui.logful.server.controller;

import com.getui.logful.server.Constants;
import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.*;
import com.getui.logful.server.mongod.*;
import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.parse.LocalFileSender;
import com.getui.logful.server.parse.LogFileParser;
import com.getui.logful.server.util.ControllerUtil;
import com.getui.logful.server.util.DateTimeUtil;
import com.getui.logful.server.util.StringUtil;
import com.getui.logful.server.util.VersionUtil;
import com.getui.logful.server.weed.WeedFSClientService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

@RestController
public class WebRestController {

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private MongoUserInfoRepository mongoUserInfoRepository;

    @Autowired
    private MongoConfigRepository mongoConfigRepository;

    @Autowired
    private MongoControlProfileRepository mongoControlProfileRepository;

    @Autowired
    private LogFileMetaRepository logFileMetaRepository;

    @Autowired
    private AttachFileMetaRepository attachFileMetaRepository;

    @Autowired
    private GraylogClientService graylogClientService;

    @Autowired
    private WeedFSClientService weedFSClientService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 获取当前服务器状态信息.
     *
     * @return 状态信息
     */
    @RequestMapping(value = "/web/dashboard/status",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String status() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long startTime = runtimeMXBean.getStartTime();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", "0.2.0");
        jsonObject.put("startTime", startTime);
        jsonObject.put("graylogConnected", graylogClientService.isConnected());
        jsonObject.put("weedFSConnected", weedFSClientService.isConnected());
        jsonObject.put("weedFSServerError", weedFSClientService.isServerError());
        return jsonObject.toString();
    }

    /**
     * 获取当前程序资源占用情况.
     *
     * @return 资源占用信息
     */
    @RequestMapping(value = "/web/dashboard/resource",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String resource() {
        Runtime runtime = Runtime.getRuntime();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", runtime.totalMemory());
        jsonObject.put("free", runtime.freeMemory());

        jsonObject.put("maxPoolSize", threadPoolTaskExecutor.getMaxPoolSize());
        jsonObject.put("poolSize", threadPoolTaskExecutor.getPoolSize());

        jsonObject.put("capacity", logfulProperties.getParser().getQueueCapacity());
        jsonObject.put("queueSize", threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size());

        return jsonObject.toString();
    }

    /**
     * Decrypt upload log file.
     *
     * @param appId   Application id
     * @param logFile Log file
     * @return Response result
     */
    @RequestMapping(value = "/web/util/decrypt/upload",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String decryptLogFile(@RequestParam("appId") final String appId,
                                 @RequestParam("logFile") MultipartFile logFile) {
        String cacheDirPath = logfulProperties.cacheDir();
        File cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            boolean successful = cacheDir.mkdirs();
            if (!successful) {
                // Create cache dir failed
                throw new ServerException();
            }
        }

        File file = new File(cacheDirPath + "/" + logFile.getOriginalFilename());
        try {
            logFile.transferTo(file);
        } catch (IOException e) {
            throw new ServerException();
        }

        String filename = StringUtil.randomUid() + ".log";
        File outFile = new File(cacheDirPath + "/" + filename);

        final LocalFileSender fileSender = LocalFileSender.create(outFile.getAbsolutePath());
        LogFileParser parser = new LogFileParser(new LogFileParser.ParserEventListener() {
            @Override
            public void output(long timestamp, String tag, String msg, short layoutId, int attachmentId) {
                String line = String.format("%s%s%s%s%s",
                        DateTimeUtil.timeString(timestamp),
                        Constants.LOG_LINE_SEPARATOR,
                        tag,
                        Constants.LOG_LINE_SEPARATOR,
                        msg);
                fileSender.write(line.replaceAll(System.getProperty("line.separator"), Constants.NEW_LINE_CHARACTER));
            }

            @Override
            public void result(boolean successful) {
                if (!successful) {
                    throw new ServerException();
                }
                try {
                    fileSender.release();
                } catch (Exception e) {
                    throw new ServerException();
                }
            }
        });

        // TODO

        try {
            parser.parse(appId, VersionUtil.CRYPTO_UPDATE_2, new FileInputStream(outFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String uri = Base64.encodeBase64String(filename.getBytes());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        jsonObject.put("uri", uri);

        return jsonObject.toString();
    }

    /**
     * Download decrypted log file from cache.
     *
     * @param uri Uri string
     * @return Log file stream
     */
    @RequestMapping(value = "/web/util/decrypt/download/{uri}",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadLogFile(@PathVariable String uri) {
        String filename;
        try {
            filename = new String(Base64.decodeBase64(uri), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServerException();
        }

        String filePath = logfulProperties.cacheDir() + "/" + filename;
        File logFile = new File(filePath);
        if (logFile.exists() && logFile.isFile()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentLength(logFile.length());
            headers.setContentDispositionFormData("attachment", filename);

            InputStreamResource inputStreamResource;
            try {
                inputStreamResource = new InputStreamResource(new FileInputStream(filePath));
                return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
            } catch (FileNotFoundException e) {
                throw new ResourceNotFoundException();
            }
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Query log file list.
     *
     * @param json Json payload.
     * @return Log file list
     */
    @RequestMapping(value = "/web/util/log/file/list",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String queryLogFileList(@RequestBody String json) {
        JSONObject object = new JSONObject(json);
        if (checkFields(object, new String[]{"platform", "uid", "appId", "date"})) {
            String platform = object.optString("platform");
            String uid = object.optString("uid");
            String appId = object.optString("appId");
            String date = object.optString("date");

            if (!ControllerUtil.checkPlatform(platform)) {
                throw new BadRequestException();
            }

            Criteria criteria = Criteria.where("platform").is(StringUtil.platformNumber(platform));
            criteria.and("uid").is(uid);
            criteria.and("appId").is(appId);
            criteria.and("date").is(date);

            List<LogFileMeta> fileMetaList = logFileMetaRepository.findAllByCriteria(criteria);

            JSONArray fileMetaArray = new JSONArray();
            for (LogFileMeta fileMeta : fileMetaList) {
                JSONObject fileMetaObject = new JSONObject();

                fileMetaObject.put("filename", fileMeta.originalFilename());
                fileMetaObject.put("level", fileMeta.getLevel());
                fileMetaObject.put("size", fileMeta.getSize());
                fileMetaObject.put("fid", fileMeta.getFid());

                fileMetaArray.put(fileMetaObject);
            }

            return fileMetaArray.toString();
        } else {
            throw new BadRequestException();
        }
    }

    /**
     * Get user unique id.
     *
     * @param webRequest WebRequest
     * @return Unique id list
     */
    @RequestMapping(value = "/web/uid/list",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String searchUid(final WebRequest webRequest) {
        String platform = webRequest.getParameter("platform");
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        ClientUser info = ClientUser.create(webRequest);
        Criteria criteria = Criteria.where("platform").is(info.getPlatform());

        addCriteria(criteria, "alias", info.getAlias());
        addCriteria(criteria, "model", info.getModel());
        addCriteria(criteria, "imei", info.getImei());
        addCriteria(criteria, "macAddress", info.getMacAddress());
        addCriteria(criteria, "osVersion", info.getOsVersion());
        addCriteria(criteria, "appId", info.getAppId());

        String version = webRequest.getParameter("version");
        if (!ControllerUtil.isEmpty(version)) {
            criteria.and("version").is(Integer.parseInt(version));
        }

        addCriteria(criteria, "versionString", info.getVersionString());

        List<ClientUser> clientUserList = mongoUserInfoRepository.findAllByCriteria(criteria);
        JSONArray jsonArray = new JSONArray();
        for (ClientUser clientUser : clientUserList) {
            JSONObject jsonObject = clientUser.toJsonObject();
            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }

    /**
     * View user info by uid.
     *
     * @param platform Platform
     * @param uid      Uid
     * @return User info
     */
    @RequestMapping(value = "web/uid/view/{platform}/{uid}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String viewUid(@PathVariable String platform,
                          @PathVariable String uid) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        Criteria criteria = Criteria.where("platform").is(StringUtil.platformNumber(platform));
        criteria.and("uid").is(uid);

        List<ClientUser> clientUserList = mongoUserInfoRepository.findAllByCriteria(criteria);
        if (clientUserList.size() > 0) {
            ClientUser first = clientUserList.get(0);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("platform", StringUtil.platformString(first.getPlatform()));
            jsonObject.put("uid", first.getUid());
            jsonObject.put("alias", first.getAlias());
            jsonObject.put("model", first.getModel());
            jsonObject.put("imei", first.getImei());
            jsonObject.put("macAddress", first.getMacAddress());
            jsonObject.put("osVersion", first.getOsVersion());

            JSONArray jsonArray = new JSONArray();
            for (ClientUser info : clientUserList) {
                JSONObject object = new JSONObject();
                object.put("appId", info.getAppId());
                object.put("version", info.getVersion());
                object.put("versionString", info.getVersionString());
                jsonArray.put(object);
            }

            jsonObject.put("app", jsonArray);

            return jsonObject.toString();
        }

        return "{}";
    }

    /**
     * Clear cache dir.
     *
     * @return Clear result
     */
    @RequestMapping(value = "/web/util/clear",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String clearCache() {
        boolean successful = true;
        String cacheDirPath = logfulProperties.cacheDir();
        File cacheDir = new File(cacheDirPath);
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        successful = false;
                    }
                }
            }
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", successful);

        return jsonObject.toString();
    }

    /**
     * Set log system gray level.
     *
     * @param level Level value
     * @return Response
     */
    @RequestMapping(value = "/web/control/level",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String setGrayLevel(@RequestParam("level") String level) {
        if (ControllerUtil.checkPlatform(level)) {
            throw new BadRequestException();
        }

        Config config = mongoConfigRepository.read();
        config.setLevel(Integer.parseInt(level));
        mongoConfigRepository.save(config);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", "ok");
        jsonObject.put("description", "");

        return jsonObject.toString();
    }

    /**
     * Get current gray level.
     *
     * @return Gray level response
     */
    @RequestMapping(value = "/web/control/level",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getGrayLevel() {

        Config config = mongoConfigRepository.read();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("level", config.getLevel());
        jsonObject.put("result", "ok");
        jsonObject.put("description", "");

        return jsonObject.toString();
    }

    /**
     * Get attachment meta by id.
     *
     * @param id Attachment id.
     * @return Attachment resource
     */
    @RequestMapping(value = "/web/util/attachment/{id}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getFidByAttachmentId(@PathVariable String id) {
        Criteria criteria = Criteria.where("attachmentId").is(id);
        AttachFileMeta meta = attachFileMetaRepository.findOneByCriteria(criteria);
        if (meta != null) {
            JSONObject object = new JSONObject();
            object.put("fid", meta.getFid());
            object.put("size", meta.getSize());
            return object.toString();
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping(value = "/web/control/profile/edit",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseBody
    public ControlProfile saveControlProfile(@RequestBody ControlProfile profile) {
        boolean successful = mongoControlProfileRepository.save(profile);
        if (successful) {
            return profile;
        } else {
            throw new BadRequestException("Target user repeat!");
        }
    }

    @RequestMapping(value = "/web/control/profile/list",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    public ResponseEntity<List<ControlProfile>> listControlProfile() {
        List<ControlProfile> profiles = mongoControlProfileRepository.findAll();
        return new ResponseEntity<>(profiles, HttpStatus.OK);
    }

    @RequestMapping(value = "/web/control/profile/view/{id}",
            method = RequestMethod.DELETE)
    public void deleteControlProfile(@PathVariable String id) {
        if (!mongoControlProfileRepository.delete(id)) {
            throw new ServerException();
        }
    }

    private Criteria addCriteria(Criteria criteria, String key, String value) {
        if (!ControllerUtil.isEmpty(value)) {
            criteria.and(key).is(value);
        }
        return criteria;
    }

    private boolean checkFields(JSONObject object, String[] fields) {
        for (String field : fields) {
            if (!object.has(field)) {
                return false;
            }
        }
        return true;
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

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {
        //
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public class ServerException extends RuntimeException {

    }

}