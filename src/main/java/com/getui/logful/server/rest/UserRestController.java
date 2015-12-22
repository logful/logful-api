package com.getui.logful.server.rest;

import com.getui.logful.server.entity.ClientUser;
import com.getui.logful.server.mongod.MongoUserInfoRepository;
import com.getui.logful.server.util.ControllerUtil;
import com.getui.logful.server.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
public class UserRestController extends BaseRestController {

    @Autowired
    private MongoUserInfoRepository mongoUserInfoRepository;

    @RequestMapping(value = "/api/users",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listUser(final WebRequest webRequest) {
        /*
        String platform = webRequest.getParameter("platform");
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new NotAcceptableException();
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
        */

        // TODO
        return "{}";
    }

    @RequestMapping(value = "/api/user/{uid}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String viewUser(@PathVariable String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
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
        } else {
            throw new NotFoundException();
        }
    }

    private Criteria addCriteria(Criteria criteria, String key, String value) {
        if (!ControllerUtil.isEmpty(value)) {
            criteria.and(key).is(value);
        }
        return criteria;
    }

}
