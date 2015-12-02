package com.igexin.log.restapi.rest;

import com.igexin.log.restapi.entity.UserInfo;
import com.igexin.log.restapi.mongod.MongoUserInfoRepository;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
public class UidRestController extends BaseRestController {

    @Autowired
    private MongoUserInfoRepository mongoUserInfoRepository;

    @RequestMapping(value = "/api/users",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listUser(final WebRequest webRequest) {
        String platform = webRequest.getParameter("platform");
        if (!ControllerUtil.checkPlatform(platform)) {
            throw new NotAcceptableException();
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

    @RequestMapping(value = "/api/user/{uid}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String viewUser(@PathVariable String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
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
