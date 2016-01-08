package com.getui.logful.server.rest;

import com.getui.logful.server.entity.CrashFileMeta;
import com.getui.logful.server.mongod.CrashFileMetaRepository;
import com.getui.logful.server.util.ControllerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
public class CrashRestController extends BaseRestController {

    @Autowired
    CrashFileMetaRepository crashFileMetaRepository;

    @Autowired
    WeedRestController weedRestController;

    @RequestMapping(value = "/api/crash/file",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public String listCrashFiles(final WebRequest request) {
        Query query = queryCondition(request);
        List<CrashFileMeta> list = crashFileMetaRepository.findAll(query);
        return listToJson(list);
    }

    @RequestMapping(value = "/api/crash/file/{id}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<InputStreamResource> readCrashFile(@PathVariable String id) {
        CrashFileMeta meta = crashFileMetaRepository.findById(id);
        if (meta == null) {
            throw new NotFoundException();
        } else {
            return weedRestController.getFile(meta.getFid());
        }
    }

}
