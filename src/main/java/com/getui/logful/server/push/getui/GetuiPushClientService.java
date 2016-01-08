package com.getui.logful.server.push.getui;

import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.push.PushResponse;
import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetuiPushClientService {

    private static final Logger LOG = LoggerFactory.getLogger(GetuiPushClientService.class);

    @Autowired
    LogfulProperties logfulProperties;

    private IGtPush pusher;

    private IGtPush getPusher() {
        if (pusher == null) {
            String key = logfulProperties.getGetuiKey();
            String secret = logfulProperties.getGetuiSecret();
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(secret)) {
                LOG.warn("Getui push client not specify key or secret!");
                return null;
            }
            pusher = new IGtPush(key, secret);
        }
        return pusher;
    }

    public PushResponse push(GetuiPushParams params, String payload) throws Exception {
        IGtPush pusher = getPusher();
        if (pusher == null) {
            throw new Exception("Getui pusher create failed!");
        }
        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(logfulProperties.getGetuiId());
        template.setAppkey(logfulProperties.getGetuiKey());
        template.setTransmissionContent(payload);
        template.setTransmissionType(1);

        ListMessage message = new ListMessage();
        message.setData(template);

        String taskId = pusher.getContentId(message);

        List<Target> targets = new ArrayList<>();
        if (params.getClientIds() != null) {
            for (String cid : params.getClientIds()) {
                Target target = new Target();
                target.setAppId(logfulProperties.getGetuiId());
                target.setClientId(cid);
                targets.add(target);
            }
        }
        if (params.getAlias() != null) {
            for (String alias : params.getAlias()) {
                Target target = new Target();
                target.setAppId(logfulProperties.getGetuiId());
                target.setAlias(alias);
                targets.add(target);
            }
        }

        Map<String, Object> result = pusher.pushMessageToList(taskId, targets).getResponse();
        PushResponse response = new PushResponse("getui");
        Object resultObject = result.get("result");
        if (resultObject instanceof String) {
            String resultString = (String) resultObject;
            if (StringUtils.equals(resultString, "ok")) {
                response.setStatus(HttpStatus.OK);
            } else {
                response.setPayload(resultString);
            }
        }

        return response;
    }
}