package com.getui.logful.server.auth;

import com.getui.logful.server.auth.ApplicationKeyPairManager.ClientKeyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleTokenEnhancer implements TokenEnhancer {

    @Autowired
    ApplicationKeyPairManager applicationKeyPairManager;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        String clientId = authentication.getOAuth2Request().getClientId();
        ClientKeyPair clientKeyPair = applicationKeyPairManager.getKeyPair(clientId);
        if (clientKeyPair != null) {
            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put("public_key", clientKeyPair.getPublicKeyBase64());
            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        }
        return accessToken;
    }

}
