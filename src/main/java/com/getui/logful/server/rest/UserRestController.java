package com.getui.logful.server.rest;

import com.getui.logful.server.entity.ClientUser;
import com.getui.logful.server.mongod.ClientUserRepository;
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
    private ClientUserRepository clientUserRepository;

    @RequestMapping(value = "/api/user",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listUser(final WebRequest request) {
        Query query = queryCondition(request);
        List<ClientUser> users = clientUserRepository.findAll(query);
        return writeListAsJson(users);
    }

    @RequestMapping(value = "/api/user/{uid}",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String viewUser(@PathVariable String uid) {
        Criteria criteria = Criteria.where("uid").is(uid);
        List<ClientUser> users = clientUserRepository.findAll(criteria);
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

}
