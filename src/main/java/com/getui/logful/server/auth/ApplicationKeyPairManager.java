package com.getui.logful.server.auth;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.auth.repository.SimpleClientDetailsRepository;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedHashMap;

@Component
public class ApplicationKeyPairManager {

    @Autowired
    SimpleClientDetailsRepository simpleClientDetailsRepository;

    private LinkedHashMap<String, ClientKeyPair> keyPairMap = new LinkedHashMap<>();

    public PrivateKey getPrivateKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getClass().isAssignableFrom(OAuth2Authentication.class)) {
                OAuth2Authentication oAuth2Authentication = ((OAuth2Authentication) authentication);
                String clientId = oAuth2Authentication.getOAuth2Request().getClientId();
                ClientKeyPair keyPair = getKeyPair(clientId);
                if (keyPair != null) {
                    return keyPair.getPrivateKey();
                }
            }
        }
        return null;
    }

    public void addKeyPair(String clientId, KeyPair keyPair) {
        keyPairMap.put(clientId, new ClientKeyPair(keyPair));
    }

    public ClientKeyPair getKeyPair(String clientId) {
        ClientKeyPair clientKeyPair = keyPairMap.get(clientId);
        if (clientKeyPair == null) {
            SimpleClientDetails simpleClientDetails = simpleClientDetailsRepository.findByClientId(clientId);
            if (simpleClientDetails != null) {
                KeyPair keyPair = simpleClientDetails.getKeyPair();
                if (keyPair != null) {
                    clientKeyPair = new ClientKeyPair(keyPair);
                    keyPairMap.put(clientId, clientKeyPair);
                    return clientKeyPair;
                }
            }
        } else {
            return clientKeyPair;
        }
        return null;
    }

    public class ClientKeyPair {

        private PublicKey publicKey;

        private PrivateKey privateKey;

        private String publicKeyBase64;

        public ClientKeyPair(KeyPair keyPair) {
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
            this.publicKeyBase64 = Base64.encodeBase64String(keyPair.getPublic().getEncoded());
        }

        public String getPublicKeyBase64() {
            return publicKeyBase64;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

    }

}
