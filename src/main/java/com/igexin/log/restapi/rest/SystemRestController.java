package com.igexin.log.restapi.rest;

import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.entity.Config;
import com.igexin.log.restapi.entity.ControlProfile;
import com.igexin.log.restapi.mongod.MongoConfigRepository;
import com.igexin.log.restapi.mongod.MongoControlProfileRepository;
import com.igexin.log.restapi.parse.GraylogClientService;
import com.igexin.log.restapi.util.ControllerUtil;
import com.igexin.log.restapi.weed.WeedFSClientService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

@RestController
public class SystemRestController extends BaseRestController {

    @Autowired
    private LogfulProperties logfulProperties;

    @Autowired
    private GraylogClientService graylogClientService;

    @Autowired
    private WeedFSClientService weedFSClientService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private MongoConfigRepository mongoConfigRepository;

    @Autowired
    private MongoControlProfileRepository mongoControlProfileRepository;

    @RequestMapping(value = "/api/system/status",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String status() {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long startTime = runtimeMXBean.getStartTime();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", "0.2.0");
        jsonObject.put("uptime", System.currentTimeMillis() - startTime);

        JSONObject object1 = new JSONObject();
        object1.put("connected", graylogClientService.isConnected());
        jsonObject.put("graylog", object1);

        JSONObject object2 = new JSONObject();
        object2.put("connected", weedFSClientService.isConnected());
        object2.put("error", weedFSClientService.isServerError());
        jsonObject.put("weed", object2);

        JSONObject object3 = new JSONObject();
        object3.put("total", runtime.totalMemory());
        object3.put("free", runtime.freeMemory());

        object3.put("maxPoolSize", threadPoolTaskExecutor.getMaxPoolSize());
        object3.put("poolSize", threadPoolTaskExecutor.getPoolSize());

        object3.put("capacity", logfulProperties.getParser().getQueueCapacity());
        object3.put("queueSize", threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size());
        jsonObject.put("resource", object3);

        return jsonObject.toString();
    }

    @RequestMapping(value = "/api/system/level",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String level() {
        Config config = mongoConfigRepository.read();

        JSONObject object = new JSONObject();
        object.put("level", config.getLevel());

        return object.toString();
    }

    @RequestMapping(value = "/api/system/level",
            method = RequestMethod.PUT,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateLevel(@RequestBody String payload) {
        try {
            JSONObject object = new JSONObject(payload);
            if (object.has("level")) {
                int level = object.optInt("level");
                Config config = mongoConfigRepository.read();
                config.setLevel(level);
                mongoConfigRepository.save(config);

                return updated();
            } else {
                throw new NotAcceptableException();
            }
        } catch (JSONException e) {
            throw new NotAcceptableException();
        }
    }

    @RequestMapping(value = "/api/system/profiles",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ControlProfile>> listProfile() {
        List<ControlProfile> profiles = mongoControlProfileRepository.findAll();
        return new ResponseEntity<>(profiles, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/system/profile",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String createProfile(@RequestBody ControlProfile profile) {
        boolean successful = mongoControlProfileRepository.save(profile);
        if (successful) {
            return created();
        } else {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.PUT,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String updateProfile(@PathVariable String id, @RequestBody ControlProfile profile) {
        profile.setId(id);
        boolean successful = mongoControlProfileRepository.save(profile);
        if (successful) {
            return updated();
        } else {
            throw new BadRequestException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ControlProfile viewProfile(@PathVariable String id) {
        ControlProfile profile = mongoControlProfileRepository.find(id);
        if (profile != null) {
            return profile;
        } else {
            throw new NotFoundException();
        }
    }

    @RequestMapping(value = "/api/system/profile/{id}",
            method = RequestMethod.DELETE,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public String deleteProfile(@PathVariable String id) {
        if (!mongoControlProfileRepository.delete(id)) {
            throw new BadRequestException();
        }
        return deleted();
    }

}
