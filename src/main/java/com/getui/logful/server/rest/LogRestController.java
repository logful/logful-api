package com.getui.logful.server.rest;

import com.getui.logful.server.Constants;
import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.AttachFileMeta;
import com.getui.logful.server.entity.LogFileMeta;
import com.getui.logful.server.mongod.AttachFileMetaRepository;
import com.getui.logful.server.mongod.LogFileMetaRepository;
import com.getui.logful.server.parse.LocalFileSender;
import com.getui.logful.server.parse.LogFileParser;
import com.getui.logful.server.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class LogRestController extends BaseRestController {

    private static final Logger LOG = LoggerFactory.getLogger(LogRestController.class);

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private LogFileMetaRepository logFileMetaRepository;

    @Autowired
    private AttachFileMetaRepository attachFileMetaRepository;

    @Autowired
    private WeedRestController weedRestController;

    @RequestMapping(value = "/api/log/files",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String listFiles(final WebRequest request) {
        String platform = request.getParameter("platform");
        String uid = request.getParameter("uid");
        String appId = request.getParameter("appId");
        String date = request.getParameter("date");

        if (!StringUtil.isEmpty(platform)
                && !StringUtil.isEmpty(uid)
                && !StringUtil.isEmpty(appId)
                && !StringUtil.isEmpty(date)) {
            Criteria criteria = Criteria.where("platform").is(StringUtil.platformNumber(platform));
            criteria.and("uid").is(uid);
            criteria.and("appId").is(appId);
            criteria.and("date").is(date);

            List<LogFileMeta> fileMetaList = logFileMetaRepository.findAllByCriteria(criteria);

            JSONArray array = new JSONArray();
            for (LogFileMeta fileMeta : fileMetaList) {
                JSONObject fileMetaObject = new JSONObject();

                fileMetaObject.put("filename", fileMeta.originalFilename());
                fileMetaObject.put("level", fileMeta.getLevel());
                fileMetaObject.put("size", fileMeta.getSize());
                fileMetaObject.put("fid", fileMeta.getFid());

                array.put(fileMetaObject);
            }

            return array.toString();
        } else {
            throw new NotAcceptableException();
        }
    }

    @RequestMapping(value = "/api/log/decrypt",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<InputStreamResource> decrypt(@RequestParam("appId") final String appId,
                                                       @RequestParam("file") MultipartFile file) {
        File cacheDir = new File(logfulProperties.cacheDir());
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new InternalServerException();
            }
        }

        File cacheFile = new File(logfulProperties.cacheDir() + "/" + StringUtil.randomUid());
        try {
            file.transferTo(cacheFile);
        } catch (IOException e) {
            throw new InternalServerException();
        }

        final File outFile = new File(logfulProperties.cacheDir() + "/" + StringUtil.randomUid() + ".log");

        final LocalFileSender fileSender = LocalFileSender.create(outFile.getAbsolutePath());
        LogFileParser parser = new LogFileParser(new LogFileParser.ParserEventListener() {
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
            public void result(boolean successful) {
                try {
                    fileSender.release();
                } catch (Exception e) {
                    throw new InternalServerException();
                }
                if (!successful) {
                    throw new InternalServerException();
                }
            }
        });

        try {
            parser.parse(appId, VersionUtil.CRYPTO_UPDATE_2, new FileInputStream(cacheFile.getAbsolutePath()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentLength(outFile.length());
            headers.setContentDispositionFormData("attachment", outFile.getName());
            try {
                InputStreamResource stream = new InputStreamResource(new FileInputStream(outFile));
                return new ResponseEntity<>(stream, headers, HttpStatus.OK);
            } catch (FileNotFoundException e) {
                throw new NotFoundException();
            }
        } catch (FileNotFoundException e) {
            try {
                fileSender.release();
            } catch (Exception ex) {
                LOG.error("Exception", ex);
            }
            throw new InternalServerException();
        }
    }

    @RequestMapping(value = "/api/log/attachment/{id}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<InputStreamResource> getAttachment(@PathVariable String id) {
        Criteria criteria = Criteria.where("attachmentId").is(id);
        AttachFileMeta meta = attachFileMetaRepository.findOneByCriteria(criteria);
        if (meta != null) {
            return weedRestController.getFile(meta.getFid());
        } else {
            throw new NotFoundException();
        }
    }
}
