package com.igexin.log.restapi.rest;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.parse.LocalFileSender;
import com.igexin.log.restapi.parse.LogFileParser;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.util.CryptoTool;
import com.igexin.log.restapi.util.DateTimeUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
public class LogRestController extends BaseRestController {

    @Autowired
    private LogfulProperties logfulProperties;

    @RequestMapping(value = "/api/log/files",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String listFiles() {
        return "";
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
        parser.parse(cacheFile.getAbsolutePath());

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
    }
}
