package com.getui.logful.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.mongod.ApplicationRepository;
import com.getui.logful.server.push.GetuiPushClientService;
import com.getui.logful.server.push.MessagePayload;
import com.getui.logful.server.push.PushResponse;
import com.gexin.rp.sdk.base.impl.Target;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class PushRestController extends BaseRestController {

    @Autowired
    GetuiPushClientService getuiPushClientService;

    @Autowired
    ApplicationRepository applicationRepository;

    @RequestMapping(value = "/api/push/{id}",
            method = RequestMethod.POST,
            produces = BaseRestController.APPLICATION_JSON,
            headers = BaseRestController.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String pushMessage(@PathVariable String id, @RequestBody String payload) {
        SimpleClientDetails client = applicationRepository.findById(id);
        if (client == null) {
            throw new NotFoundException("app not found.");
        }
        if (StringUtils.isEmpty(client.getGetuiAppId())
                || StringUtils.isEmpty(client.getGetuiAppKey())
                || StringUtils.isEmpty(client.getGetuiMasterSecret())) {
            throw new BadRequestException();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            MessagePayload message = mapper.readValue(payload, MessagePayload.class);
            List<String> clientIds = message.getClientIds();
            List<String> aliases = message.getAliases();

            List<Target> targets = new ArrayList<>();
            for (String cid : clientIds) {
                Target target = new Target();
                target.setAppId(client.getGetuiAppId());
                target.setClientId(cid);
                targets.add(target);
            }

            for (String alias : aliases) {
                Target target = new Target();
                target.setAppId(client.getGetuiAppId());
                target.setAlias(alias);
                targets.add(target);
            }

            try {
                PushResponse response = getuiPushClientService.push(
                        client.getGetuiAppId(),
                        client.getGetuiAppKey(),
                        client.getGetuiMasterSecret(),
                        targets,
                        message.payload());
                if (response.ok()) {
                    return ok();
                } else {
                    throw new InternalServerException();
                }
            } catch (Exception e) {
                throw new InternalServerException();
            }
        } catch (IOException e) {
            throw new BadRequestException();
        }
    }

}
