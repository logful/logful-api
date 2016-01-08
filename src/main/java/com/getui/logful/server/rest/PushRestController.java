package com.getui.logful.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getui.logful.server.push.MessagePayload;
import com.getui.logful.server.push.PushParams;
import com.getui.logful.server.push.PushResponse;
import com.getui.logful.server.push.getui.GetuiPushClientService;
import com.getui.logful.server.push.jpush.JPushClientService;
import com.getui.logful.server.util.ControllerUtil;
import org.json.JSONObject;
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
    JPushClientService jPushClientService;

    @RequestMapping(value = "/api/push",
            method = RequestMethod.POST,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String pushMessage(@RequestBody String payload) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            MessagePayload message = mapper.readValue(payload, MessagePayload.class);
            String pushPayloadString = message.payload();
            PushParams params = message.getParams();

            List<PushResponse> responses = new ArrayList<>();
            if (params.getGetui() != null) {
                try {
                    responses.add(getuiPushClientService.push(params.getGetui(), pushPayloadString));
                } catch (Exception e) {
                    throw new InternalServerException(e.getMessage());
                }
            }
            if (params.getjPush() != null) {
                try {
                    responses.add(jPushClientService.push(params.getjPush(), pushPayloadString));
                } catch (Exception e) {
                    throw new InternalServerException(e.getMessage());
                }
            }

            boolean success = true;
            JSONObject object = new JSONObject();
            for (PushResponse response : responses) {
                if (response.getStatus() != HttpStatus.OK) {
                    success = false;
                }
                object.put(response.getExtra(), response.getPayload());
            }

            if (success) {
                return ok();
            } else {
                throw new InternalServerException(object.toString());
            }
        } catch (IOException e) {
            throw new BadRequestException();
        }
    }

}
