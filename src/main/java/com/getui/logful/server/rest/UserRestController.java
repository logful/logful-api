package com.getui.logful.server.rest;

import com.getui.logful.server.entity.ClientUser;
import com.getui.logful.server.mongod.MongoClientUserRepository;
import com.getui.logful.server.util.ControllerUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
public class UserRestController extends BaseRestController {

    @Autowired
    private MongoClientUserRepository mongoClientUserRepository;

    @RequestMapping(value = "/api/user",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listUser(final WebRequest request) {
        Query query = queryCondition(request);
        List<ClientUser> users = mongoClientUserRepository.findAll(query);
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
        List<ClientUser> users = mongoClientUserRepository.findAll(criteria);
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
