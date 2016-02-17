package com.getui.logful.server.push;

import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetuiPushClientService {

    private static final Logger LOG = LoggerFactory.getLogger(GetuiPushClientService.class);

    public PushResponse push(String appId,
                             String appKey,
                             String masterSecret,
                             List<Target> targets,
                             String payload) throws Exception {
        IGtPush pusher = new IGtPush(appKey, masterSecret);

        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTransmissionContent(payload);
        template.setTransmissionType(1);

        ListMessage message = new ListMessage();
        message.setData(template);

        String taskId = pusher.getContentId(message);

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

        pusher.close();

        return response;
    }
}