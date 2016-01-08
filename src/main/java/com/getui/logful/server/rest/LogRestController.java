package com.getui.logful.server.rest;

import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.AttachFileMeta;
import com.getui.logful.server.entity.LogFileMeta;
import com.getui.logful.server.mongod.AttachFileMetaRepository;
import com.getui.logful.server.mongod.LogFileMetaRepository;
import com.getui.logful.server.util.ControllerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

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

    @RequestMapping(value = "/api/log/file",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String listLogFiles(final WebRequest request) {
        Query query = queryCondition(request);
        List<LogFileMeta> list = logFileMetaRepository.findAll(query);
        return listToJson(list);
    }

    @RequestMapping(value = "/api/log/file/{id}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<InputStreamResource> readLogFile(@PathVariable String id) {
        LogFileMeta meta = logFileMetaRepository.findById(id);
        if (meta == null) {
            throw new NotFoundException();
        } else {
            return weedRestController.getFile(meta.getFid());
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
