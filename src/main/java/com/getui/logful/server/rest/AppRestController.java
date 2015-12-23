package com.getui.logful.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.mongod.ApplicationRepository;
import com.getui.logful.server.mongod.QueryCondition;
import com.getui.logful.server.util.ControllerUtil;
import com.getui.logful.server.util.RSAUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.security.KeyPair;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
public class AppRestController extends BaseRestController {

    @Autowired
    LogfulProperties logfulProperties;

    @Autowired
    ApplicationRepository applicationRepository;

    @RequestMapping(value = "/api/apps",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listApps(WebRequest request) {
        QueryCondition condition = new QueryCondition(request);
        List<SimpleClientDetails> apps = applicationRepository.findAll(condition);
        return listToJson(apps);
    }

    @RequestMapping(value = "/api/app",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String createApp(@RequestBody String payload) {
        // TODO scope create app.
        try {
            JSONObject object = new JSONObject(payload);
            String name = object.optString("name");
            String appId = object.getString("appId");
            if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(appId)) {
                SimpleClientDetails simpleClientDetails = new SimpleClientDetails();

                simpleClientDetails.setAccessTokenValiditySeconds(logfulProperties.getAccessTokenValiditySeconds());
                simpleClientDetails.setRefreshTokenValiditySeconds(logfulProperties.getRefreshTokenValiditySeconds());

                simpleClientDetails.setName(name);
                simpleClientDetails.setAppId(appId);
                simpleClientDetails.setCreateDate(new Date());

                simpleClientDetails.authorizedGrantTypes("refresh_token", "client_credentials");
                simpleClientDetails.authorities("logful_client");
                simpleClientDetails.scopes("client");

                String key = key(simpleClientDetails);
                String secret = secret(simpleClientDetails);

                simpleClientDetails.setClientId(key);
                simpleClientDetails.setClientSecret(secret);

                KeyPair keyPair = RSAUtil.generateKeyPair();
                simpleClientDetails.addKeyPair(keyPair);

                applicationRepository.save(simpleClientDetails);
                return created();
            } else {
                throw new BadRequestException();
            }
        } catch (Exception e) {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.PUT,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateApp(@PathVariable String id, @RequestBody String payload) {
        try {
            SimpleClientDetails temp = new ObjectMapper().readValue(payload, SimpleClientDetails.class);
            // TODO update
        } catch (Exception e) {
            throw new BadRequestException();
        }
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
        return created();
        */
        return "";
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.DELETE,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteApp(@PathVariable String id) {
        if (!applicationRepository.delete(id)) {
            throw new InternalServerException();
        }
    }

    private String key(SimpleClientDetails clientDetails) {
        String[] parts = {UUID.randomUUID().toString(),
                "RANDOM-SPWZPXTGL8LT6OLNMOQU3E4GWS2L7UHR",
                clientDetails.getName(),
                clientDetails.getAppId(),
                String.valueOf(clientDetails.getCreateDate().getTime())};
        return DigestUtils.md5Hex(StringUtils.join(parts, "").getBytes());
    }

    private String secret(SimpleClientDetails clientDetails) {
        String[] parts = {UUID.randomUUID().toString(),
                "RANDOM-XBU88X2VQN2JWTS5GBPNHKLAFK5ACPJJ",
                clientDetails.getName(),
                clientDetails.getAppId(),
                String.valueOf(clientDetails.getCreateDate().getTime())};
        return DigestUtils.md5Hex(StringUtils.join(parts, "").getBytes());
    }

}
