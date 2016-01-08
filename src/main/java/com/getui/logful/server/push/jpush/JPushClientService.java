package com.getui.logful.server.push.jpush;

import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.audience.AudienceTarget;
import com.getui.logful.server.Constants;
import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.push.PushResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JPushClientService {

    private static final Logger LOG = LoggerFactory.getLogger(JPushClientService.class);

    @Autowired
    private LogfulProperties logfulProperties;

    private JPushClient pusher;

    private JPushClient getPusher() {
        if (pusher == null) {
            String key = logfulProperties.getJpushKey();
            String secret = logfulProperties.getJpushSecret();
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(secret)) {
                LOG.warn("JPushClient not specify key or secret!");
                return null;
            }
            pusher = new JPushClient(secret, key);
        }
        return pusher;
    }

    public PushResponse push(JPushParams params, String payload) throws Exception {
        JPushClient pusher = getPusher();
        if (pusher == null) {
            throw new Exception("JPush client create failed!");
        }

        Audience.Builder audienceBuilder = Audience.newBuilder();
        if (params.getAlias() != null && params.getAlias().size() > 0) {
            audienceBuilder.addAudienceTarget(AudienceTarget.alias(params.getAlias()));
        }
        if (params.getTags() != null && params.getAlias().size() > 0) {
            audienceBuilder.addAudienceTarget(AudienceTarget.tag(params.getTags()));
        }
        if (params.getRegistrationIds() != null && params.getRegistrationIds().size() > 0) {
            audienceBuilder.addAudienceTarget(AudienceTarget.registrationId(params.getRegistrationIds()));
        }

        Platform platform = Platform.all();
        if (params.getPlatform() == Constants.PLATFORM_ALL) {
            platform = Platform.all();
        } else if (params.getPlatform() == Constants.PLATFORM_ALL) {
            platform = Platform.android();
        } else if (params.getPlatform() == Constants.PLATFORM_ALL) {
            platform = Platform.ios();
        }

        PushPayload pushPayload = PushPayload.newBuilder()
                .setPlatform(platform)
                .setAudience(audienceBuilder.build())
                .setMessage(Message.content(payload))
                .build();

        PushResponse response = new PushResponse("jPush");
        PushResult result = pusher.sendPush(pushPayload);
        if (result.isResultOK()) {
            response.setStatus(HttpStatus.OK);
        } else {
            response.setPayload(result.toString());
        }

        return response;
    }
}
