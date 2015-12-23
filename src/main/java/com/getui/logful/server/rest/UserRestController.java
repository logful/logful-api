package com.getui.logful.server.rest;

import com.getui.logful.server.entity.ClientUser;
import com.getui.logful.server.mongod.MongoUserInfoRepository;
import com.getui.logful.server.mongod.QueryCondition;
import com.getui.logful.server.util.ControllerUtil;
import org.apache.commons.lang.StringUtils;
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
    public String listUser(final WebRequest request) {
        QueryCondition condition = new QueryCondition(request);

        String platform = request.getParameter("platform");

        if (!StringUtils.isNumeric(platform)) {
            throw new BadRequestException();
        }

        Criteria criteria = Criteria.where("platform").is(Integer.parseInt(platform));

        String[] keys = {"alias", "model", "imei", "macAddress", "osVersion", "appId", "versionString"};
        for (String key : keys) {
            addCriteria(criteria, key, request.getParameter(key));
        }

        String version = request.getParameter("version");
        if (StringUtils.isNumeric(version)) {
            criteria.and("version").is(Integer.parseInt(version));
        }

        List<ClientUser> users = mongoUserInfoRepository.findAll(condition, criteria);
        return listToJson(users);
    }

    @RequestMapping(value = "/api/user/{uid}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String viewUser(@PathVariable String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
        List<ClientUser> users = mongoUserInfoRepository.findAll(criteria);
        if (users != null && users.size() > 0) {
            JSONArray array = new JSONArray();
            for (ClientUser user : users) {
                array.put(user.appObject());
            }

            JSONObject object = users.get(0).baseObject();
            object.put("apps", array);

            return object.toString();
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
