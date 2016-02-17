package com.getui.logful.server.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.ServerProperties;
import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.mongod.*;
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
    ServerProperties serverProperties;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ClientUserRepository clientUserRepository;

    @Autowired
    LogFileMetaRepository logFileMetaRepository;

    @Autowired
    CrashFileMetaRepository crashFileMetaRepository;

    private static final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = "/api/app",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String listApps(WebRequest request) {
        QueryCondition condition = new QueryCondition(request);
        List<SimpleClientDetails> apps = applicationRepository.findAll(condition);
        return writeListAsJson(apps);
    }

    @RequestMapping(value = "/api/app",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String createApp(@RequestBody String payload) {
        // TODO scope create app.
        try {
            JSONObject object = new JSONObject(payload);
            String name = object.optString("name");
            if (StringUtils.isNotEmpty(name)) {
                SimpleClientDetails simpleClientDetails = new SimpleClientDetails();

                simpleClientDetails.setAccessTokenValiditySeconds(serverProperties.getAccessTokenValiditySeconds());
                simpleClientDetails.setRefreshTokenValiditySeconds(serverProperties.getRefreshTokenValiditySeconds());

                simpleClientDetails.setName(name);
                simpleClientDetails.setDescription(object.optString("description"));
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
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public SimpleClientDetails getDetail(@PathVariable String id) {
        SimpleClientDetails item = applicationRepository.findById(id);
        if (item != null) {
            return item;
        } else {
            throw new NotFoundException();
        }
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.PUT,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public SimpleClientDetails updateApp(@PathVariable String id, @RequestBody String payload) {
        try {
            SimpleClientDetails temp = new ObjectMapper().readValue(payload, SimpleClientDetails.class);
            if (StringUtils.isEmpty(temp.getName())) {
                throw new BadRequestException("name is empty!");
            } else {
                temp.setUpdateDate(new Date());
                if (applicationRepository.update(temp)) {
                    return temp;
                } else {
                    throw new BadRequestException("update failed!");
                }
            }
        } catch (Exception e) {
            throw new NotAcceptableException();
        }
    }

    @RequestMapping(value = "/api/app/{id}",
            method = RequestMethod.DELETE,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public String deleteApp(@PathVariable String id) {
        if (!applicationRepository.delete(id)) {
            throw new InternalServerException();
        }
        return deleted();
    }

    @RequestMapping(value = "/api/app/statistic/{id}",
            method = RequestMethod.GET,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String appStatistic(@PathVariable String id) {
        SimpleClientDetails client = applicationRepository.findById(id);
        if (client == null) {
            throw new NotFoundException();
        }

        Statistic statistic = new Statistic();

        Statistic.User user = new Statistic.User();
        user.setCount(clientUserRepository.countAll(client.getClientId()));

        Statistic.Log log = new Statistic.Log();
        log.setCount(logFileMetaRepository.countAll(client.getClientId()));

        Statistic.Crash crash = new Statistic.Crash();
        crash.setCount(crashFileMetaRepository.countAll(client.getClientId()));

        statistic.setUser(user);
        statistic.setLog(log);
        statistic.setCrash(crash);

        try {
            return mapper.writeValueAsString(statistic);
        } catch (JsonProcessingException e) {
            throw new InternalServerException();
        }
    }

    private String key(SimpleClientDetails clientDetails) {

        String[] parts = {UUID.randomUUID().toString(),
                "RANDOM-SPWZPXTGL8LT6OLNMOQU3E4GWS2L7UHR",
                clientDetails.getName(),
                String.valueOf(clientDetails.getCreateDate().getTime())};
        return DigestUtils.md5Hex(StringUtils.join(parts, "").getBytes());
    }

    private String secret(SimpleClientDetails clientDetails) {
        String[] parts = {UUID.randomUUID().toString(),
                "RANDOM-XBU88X2VQN2JWTS5GBPNHKLAFK5ACPJJ",
                clientDetails.getName(),
                String.valueOf(clientDetails.getCreateDate().getTime())};
        return DigestUtils.md5Hex(StringUtils.join(parts, "").getBytes());
    }

    private static class Statistic {

        private User user;
        private Log log;
        private Crash crash;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public Log getLog() {
            return log;
        }

        public void setLog(Log log) {
            this.log = log;
        }

        public Crash getCrash() {
            return crash;
        }

        public void setCrash(Crash crash) {
            this.crash = crash;
        }

        private static class User {
            private long count;

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }
        }

        private static class Log {
            private long count;

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }
        }

        private static class Crash {
            private long count;

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }
        }
    }

}
