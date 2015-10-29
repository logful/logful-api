package com.igexin.log.restapi.controller;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.GlobalReference;
import com.igexin.log.restapi.RestApiProperties;
import com.igexin.log.restapi.entity.Config;
import com.igexin.log.restapi.entity.DecryptError;
import com.igexin.log.restapi.entity.UserInfo;
import com.igexin.log.restapi.mongod.MongoConfigRepository;
import com.igexin.log.restapi.mongod.MongoDecryptErrorRepository;
import com.igexin.log.restapi.mongod.MongoUserInfoRepository;
import com.igexin.log.restapi.parse.LocalFileSender;
import com.igexin.log.restapi.parse.LogFileParser;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.util.CryptoTool;
import com.igexin.log.restapi.util.DateTimeUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;
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
    private RestApiProperties restApiProperties;

    @Autowired
    private MongoUserInfoRepository mongoUserInfoRepository;

    @Autowired
    private MongoDecryptErrorRepository mongoDecryptErrorRepository;

    @Autowired
    private MongoConfigRepository mongoConfigRepository;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 获取当前服务器状态信息.
     *
     * @return 状态信息
     */
    @RequestMapping(value = "/web/status",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String status() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        jsonObject.put("version", "1.0.0");
        jsonObject.put("graylog_connected", GlobalReference.isConnected());
        return jsonObject.toString();
    }

    /**
     * 获取当前程序资源占用情况.
     *
     * @return 资源占用信息
     */
    @RequestMapping(value = "/web/resource",
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

        jsonObject.put("capacity", Constants.QUEUE_CAPACITY);
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
    @RequestMapping(value = "/web/decrypt",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String decryptLogFile(@RequestParam("appId") final String appId,
                                 @RequestParam("logFile") MultipartFile logFile) {
        String cacheDirPath = restApiProperties.cacheDir();
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

                String line = String.format("%s|%s|%s", DateTimeUtil.timeString(timestamp), tag, msg);
                fileSender.write(line);
            }

            @Override
            public void result(String inFilePath, boolean successful) {
                if (!successful) {
                    throw new ServerException();
                }
                fileSender.close();
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
    @RequestMapping(value = "/web/download/{uri}",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadLogFile(@PathVariable String uri) {

        String filename;
        try {
            filename = new String(Base64.decodeBase64(uri), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServerException();
        }

        String filePath = restApiProperties.cacheDir() + "/" + filename;
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
     * Fetch decrypted log file list.
     *
     * @param webRequest WebRequest
     * @return Decrypted log file list in json string
     */
    @RequestMapping(value = "/web/log/files",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String fetchDecryptedLogFileList(final WebRequest webRequest) {
        String platform = webRequest.getParameter("platform");
        String uid = webRequest.getParameter("uid");
        String appId = webRequest.getParameter("appId");
        String date = webRequest.getParameter("date");
        String startDate = webRequest.getParameter("startDate");
        String endDate = webRequest.getParameter("endDate");

        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        JSONArray jsonArray = new JSONArray();
        if (ControllerUtil.isEmpty(date)
                && ControllerUtil.isEmpty(startDate)
                && ControllerUtil.isEmpty(endDate)) {
            return jsonArray.toString();
        }

        if (ControllerUtil.isEmpty(appId) || ControllerUtil.isEmpty(uid)) {
            return jsonArray.toString();
        }

        String logDirPath = restApiProperties.decryptedDir(platform) + "/" + appId + "/" + uid;
        File logDir = new File(logDirPath);
        if (!logDir.exists() || !logDir.isDirectory()) {
            return jsonArray.toString();
        }

        File[] files = logDir.listFiles();
        if (files == null) {
            return jsonArray.toString();
        }

        String uriPrefix = "?platform=" + platform.toLowerCase() + "&appId=" + appId + "&uid=" + uid + "&filename=";
        if (!ControllerUtil.isEmpty(date)) {
            try {
                String dateString = DateTimeUtil.logFileNameDateString(new DateTime(date).toDate());
                for (File file : files) {
                    String[] parts = file.getName().replace(".bin", "").split("-");
                    if (parts.length >= 3) {
                        if (parts[1].equalsIgnoreCase(dateString)) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("filename", file.getName());
                            jsonObject.put("level", StringUtil.level(parts[2]));
                            jsonObject.put("size", file.length());
                            jsonObject.put("uri", uriPrefix + file.getName());
                            jsonArray.put(jsonObject);
                        }
                    }
                }
            } catch (Exception e) {
                throw new BadRequestException();
            }
        }

        if (!ControllerUtil.isEmpty(startDate) && !ControllerUtil.isEmpty(endDate)) {
            long startTime, endTime;
            try {
                startTime = new DateTime(startDate).toDate().getTime();
                endTime = new DateTime(endDate).toDate().getTime();
                for (File file : files) {
                    String[] parts = file.getName().replace(".bin", "").split("-");
                    if (parts.length >= 3) {
                        long dateTime = new DateTime(parts[2]).toDate().getTime();
                        if (dateTime >= startTime && dateTime <= endTime) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("filename", file.getName());
                            jsonObject.put("level", StringUtil.level(parts[2]));
                            jsonObject.put("size", file.length());

                            String filename = StringUtils.replace(file.getName(), ".bin", "");
                            jsonObject.put("uri", uriPrefix + filename);

                            jsonArray.put(jsonObject);
                        }
                    }
                }
            } catch (Exception e) {
                throw new BadRequestException();
            }
        }

        return jsonArray.toString();
    }

    /**
     * Download decrypted log file.
     *
     * @param platform Platform
     * @param uid      User unique id
     * @param appId    Application id
     * @param filename Log file name
     * @return Log file stream
     */
    @RequestMapping(value = "/web/log/fetch",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadLogFile(@RequestParam("platform") String platform,
                                                               @RequestParam("appId") String appId,
                                                               @RequestParam("uid") String uid,
                                                               @RequestParam("filename") String filename) {
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new BadRequestException();
        }

        if (!filename.contains(".bin")) {
            filename = filename + ".bin";
        }

        String filePath = restApiProperties.decryptedDir(platform) + "/" + appId + "/" + uid + "/" + filename;
        File logFile = new File(filePath);
        if (logFile.exists() && logFile.isFile()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentLength(logFile.length());
            headers.setContentDispositionFormData("attachment", StringUtils.replace(filename, ".bin", ".log"));

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
     * Get user unique id.
     *
     * @param webRequest WebRequest
     * @return Unique id list
     */
    @RequestMapping(value = "/web/uid",
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
    @RequestMapping(value = "/web/uid/{platform}/{uid}",
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

        Criteria criteria = Criteria.where("platform").is(UserInfo.platformNumber(platform));
        criteria.and("uid").is(uid);

        List<UserInfo> userInfoList = mongoUserInfoRepository.findAllByCriteria(criteria);
        if (userInfoList.size() > 0) {
            UserInfo first = userInfoList.get(0);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("platform", UserInfo.platformString(first.getPlatform()));
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
    @RequestMapping(value = "/web/clear",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String clearCache() {
        boolean successful = true;
        String cacheDirPath = restApiProperties.cacheDir();
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
     * Fetch decrypt error list.
     *
     * @return Decrypt error list
     */
    @RequestMapping(value = "/web/errors",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String fetchDecryptErrorList() {
        List<DecryptError> errorList = mongoDecryptErrorRepository.findAll();

        JSONArray jsonArray = new JSONArray();

        for (DecryptError error : errorList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uid", error.getUid());
            jsonObject.put("timestamp", error.getTimestamp());

            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }

    /**
     * Set log system gray level.
     *
     * @param level Level value
     * @return Response
     */
    @RequestMapping(value = "/web/level",
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
    @RequestMapping(value = "/web/level",
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
    @RequestMapping(value = "/web/attachment/download/{uri}",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable String uri) {
        if (StringUtil.isEmpty(uri)) {
            throw new BadRequestException();
        }
        String filename = uri + ".jpg";
        File file = new File(restApiProperties.attachmentDir() + "/" + filename);
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
    }

    private Criteria addCriteria(Criteria criteria, String key, String value) {
        if (!ControllerUtil.isEmpty(value)) {
            criteria.and(key).is(value);
        }
        return criteria;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class BadRequestException extends RuntimeException {
        //
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {
        //
    }

    @ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
    public class ServerException extends RuntimeException {

    }

}
