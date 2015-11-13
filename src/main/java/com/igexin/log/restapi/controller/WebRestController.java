package com.igexin.log.restapi.controller;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.entity.Config;
import com.igexin.log.restapi.entity.ControlProfile;
import com.igexin.log.restapi.entity.UserInfo;
import com.igexin.log.restapi.entity.WeedLogFileMeta;
import com.igexin.log.restapi.mongod.MongoConfigRepository;
import com.igexin.log.restapi.mongod.MongoControlProfileRepository;
import com.igexin.log.restapi.mongod.MongoUserInfoRepository;
import com.igexin.log.restapi.mongod.MongoWeedLogFileMetaRepository;
import com.igexin.log.restapi.parse.GraylogClientService;
import com.igexin.log.restapi.parse.LocalFileSender;
import com.igexin.log.restapi.parse.LogFileParser;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.util.CryptoTool;
import com.igexin.log.restapi.util.DateTimeUtil;
import com.igexin.log.restapi.util.StringUtil;
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
    private MongoWeedLogFileMetaRepository mongoWeedLogFileMetaRepository;

    @Autowired
    GraylogClientService graylogClientService;

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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        jsonObject.put("version", "1.0.0");
        jsonObject.put("graylog_connected", graylogClientService.isConnected());
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

        jsonObject.put("capacity", Constants.PARSER_QUEUE_CAPACITY);
        jsonObject.put("active", threadPoolTaskExecutor.getActiveCount());

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
        LogFileParser parser = new LogFileParser();
        parser.setListener(new LogFileParser.ParserEventListener() {
            @Override
            public void output(long timestamp, String encryptedTag, String encryptedMsg, short layoutId, int attachmentId) {
                String tag = CryptoTool.decrypt(appId, encryptedTag);
                String msg = CryptoTool.decrypt(appId, encryptedMsg);

                String line = String.format("%s%s%s%s%s",
                        DateTimeUtil.timeString(timestamp),
                        Constants.LOG_LINE_SEPARATOR,
                        tag,
                        Constants.LOG_LINE_SEPARATOR,
                        msg);
                fileSender.write(line.replaceAll(System.getProperty("line.separator"), Constants.NEW_LINE_CHARACTER));
            }

            @Override
            public void result(String inFilePath, boolean successful) {
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
        parser.parse(file.getAbsolutePath());

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
            method = RequestMethod.POST)
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

            List<WeedLogFileMeta> fileMetaList = mongoWeedLogFileMetaRepository.findAllByCriteria(criteria);

            JSONArray fileMetaArray = new JSONArray();
            for (WeedLogFileMeta fileMeta : fileMetaList) {
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

        UserInfo info = UserInfo.create(webRequest);
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

        List<UserInfo> userInfoList = mongoUserInfoRepository.findAllByCriteria(criteria);
        JSONArray jsonArray = new JSONArray();
        for (UserInfo userInfo : userInfoList) {
            JSONObject jsonObject = userInfo.toJsonObject();
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

        List<UserInfo> userInfoList = mongoUserInfoRepository.findAllByCriteria(criteria);
        if (userInfoList.size() > 0) {
            UserInfo first = userInfoList.get(0);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("platform", StringUtil.platformString(first.getPlatform()));
            jsonObject.put("uid", first.getUid());
            jsonObject.put("alias", first.getAlias());
            jsonObject.put("model", first.getModel());
            jsonObject.put("imei", first.getImei());
            jsonObject.put("macAddress", first.getMacAddress());
            jsonObject.put("osVersion", first.getOsVersion());

            JSONArray jsonArray = new JSONArray();
            for (UserInfo info : userInfoList) {
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
     * Download attachment file.
     *
     * @param uri Attachment uri
     * @return Attachment resource
     */
    @RequestMapping(value = "/web/util/attachment/download/{uri}",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable String uri) {
        /*
        if (StringUtil.isEmpty(uri)) {
            throw new BadRequestException();
        }
        String filename = uri + ".jpg";
        File file = new File(logfulProperties.attachmentDir() + "/" + filename);
        if (file.exists() || file.isFile()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(file.length());
            headers.setContentDispositionFormData("attachment", filename);

            InputStreamResource inputStreamResource;
            try {
                inputStreamResource = new InputStreamResource(new FileInputStream(file));
                return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
            } catch (FileNotFoundException e) {
                throw new ResourceNotFoundException();
            }
        } else {
            throw new ResourceNotFoundException();
        }
        */
        // TODO
        return null;
    }

    @RequestMapping(value = "/web/control/profile/edit",
            method = RequestMethod.POST)
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
            method = RequestMethod.GET)
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

    @ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
    public class ServerException extends RuntimeException {

    }

}
