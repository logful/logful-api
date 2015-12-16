package com.getui.logful.server.rest;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.mongod.ApplicationRepository;
import com.getui.logful.server.util.ControllerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestController
public class AppRestController extends BaseRestController {

    @Autowired
    ApplicationRepository applicationRepository;

    @RequestMapping(value = "/api/apps",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseBody
    public String listApps(WebRequest request) {
        /*
        QueryCondition condition = new QueryCondition(request);
        List<Application> apps = applicationRepository.findAll(condition);
        JSONArray array = new JSONArray();
        for (Application app : apps) {
            array.put(app.toJSONObject());
        }
        return array.toString();
        */
        return "{}";
    }

    @RequestMapping(value = "/api/app",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String createApp(@RequestBody String payload) {
        SimpleClientDetails simpleClientDetails = new SimpleClientDetails();
        applicationRepository.save(simpleClientDetails);
        /*
        if (StringUtils.isEmpty(application.getName()) || StringUtils.isEmpty(application.getAppId())) {
            throw new BadRequestException();
        }
        application.setCreateTime(System.currentTimeMillis());
        try {
            KeyPair keyPair = RSAUtil.generateKeyPair();
            application.setKey(uid(application));
            application.setSecret(uid(application));
            application.setPublicKey(keyPair.getPublic().getEncoded());
            application.setPrivateKey(keyPair.getPrivate().getEncoded());
            applicationRepository.save(application);
            return created();
        } catch (Exception e) {
            throw new InternalServerException();
        }
        */
        return "{}";
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.PUT,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateApp(@PathVariable String id, @RequestBody String payload) {
        /*
        if (StringUtils.isEmpty(application.getName()) || StringUtils.isEmpty(application.getAppId())) {
            throw new BadRequestException();
        }
        application.setId(id);
        application.setUpdateTime(System.currentTimeMillis());
        try {
            applicationRepository.save(application);
        } catch (Exception e) {
            throw new BadRequestException();
        }
        */
        return created();
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.DELETE,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public String deleteApp(@PathVariable String id) {
        return null;
    }

    private String uid(SimpleClientDetails clientDetails) {
        /*
        String[] parts = {UUID.randomUUID().toString(),
                application.getName(),
                application.getAppId(),
                String.valueOf(application.getCreateTime())};
        return DigestUtils.md5Hex(ArrayUtils.addAll(StringUtils.join(parts, "").getBytes(), application.getPublicKey()));
        */
        return "";
    }

}