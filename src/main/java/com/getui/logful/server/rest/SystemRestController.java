package com.getui.logful.server.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.entity.ControlProfile;
import com.getui.logful.server.entity.GlobalConfig;
import com.getui.logful.server.mongod.ApplicationRepository;
import com.getui.logful.server.mongod.ControlProfileRepository;
import com.getui.logful.server.mongod.GlobalConfigRepository;
import com.getui.logful.server.system.StatsService;
import com.getui.logful.server.system.SystemStats;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SystemRestController extends BaseRestController {

    @Autowired
    private GlobalConfigRepository globalConfigRepository;

    @Autowired
    private ControlProfileRepository controlProfileRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StatsService statsService;

    private static final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = "/api/system/status",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String status() {
        SystemStats stats = statsService.systemStats();
        try {
            return mapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            throw new InternalServerException();
        }
    }

    @RequestMapping(value = "/api/system/level",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String level() {
        GlobalConfig config = globalConfigRepository.read();

        JSONObject object = new JSONObject();
        object.put("level", config.getLevel());

        return object.toString();
    }

    @RequestMapping(value = "/api/system/level",
            method = RequestMethod.PUT,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateLevel(@RequestBody String payload) {
        try {
            JSONObject object = new JSONObject(payload);
            if (object.has("level")) {
                int level = object.optInt("level");
                GlobalConfig config = globalConfigRepository.read();
                config.setLevel(level);
                globalConfigRepository.save(config);

                return updated();
            } else {
                throw new NotAcceptableException();
            }
        } catch (JSONException e) {
            throw new NotAcceptableException();
        }
    }

    @RequestMapping(value = "/api/system/grant",
            method = RequestMethod.PUT,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String addGrantClient(@RequestBody String payload) {
        try {
            String[] ids = new ObjectMapper().readValue(payload, String[].class);
            if (ids.length == 0) {
                throw new BadRequestException();
            }
            List<SimpleClientDetails> records = applicationRepository.findByClientIds(ids);
            if (records != null && records.size() == ids.length) {
                globalConfigRepository.addClient(ids);
                return updated();
            } else {
                throw new BadRequestException();
            }
        } catch (Exception e) {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/grant",
            method = RequestMethod.DELETE,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public String removeGrantClient(@RequestBody String payload) {
        try {
            String[] ids = new ObjectMapper().readValue(payload, String[].class);
            if (ids.length == 0) {
                throw new BadRequestException();
            }
            globalConfigRepository.removeClient(ids);
            return deleted();
        } catch (Exception e) {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/profiles",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ControlProfile>> listProfile() {
        List<ControlProfile> profiles = controlProfileRepository.findAll();
        return new ResponseEntity<>(profiles, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/system/profile",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String createProfile(@RequestBody ControlProfile profile) {
        boolean successful = controlProfileRepository.save(profile);
        if (successful) {
            return created();
        } else {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.PUT,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateProfile(@PathVariable String id, @RequestBody ControlProfile profile) {
        profile.setId(id);
        boolean successful = controlProfileRepository.save(profile);
        if (successful) {
            return updated();
        } else {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ControlProfile viewProfile(@PathVariable String id) {
        ControlProfile profile = controlProfileRepository.find(id);
        if (profile != null) {
            return profile;
        } else {
            throw new NotFoundException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.DELETE,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public String deleteProfile(@PathVariable String id) {
        if (!controlProfileRepository.delete(id)) {
            throw new BadRequestException();
        }
        return deleted();
    }

}
