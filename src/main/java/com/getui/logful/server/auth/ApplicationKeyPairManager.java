package com.getui.logful.server.auth;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.auth.repository.SimpleClientDetailsRepository;
import com.getui.logful.server.util.RSAUtil;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedHashMap;

@Component
public class ApplicationKeyPairManager {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationKeyPairManager.class);

    @Autowired
    SimpleClientDetailsRepository simpleClientDetailsRepository;

    private LinkedHashMap<String, ClientKeyPair> keyPairMap = new LinkedHashMap<>();

    public byte[] decrypt(byte[] data) {
        if (data == null) {
            return null;
        }
        PrivateKey privateKey = getPrivateKey();
        if (privateKey != null) {
            try {
                return RSAUtil.decrypt(data, privateKey);
            } catch (Exception e) {
                LOG.error("Exception", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public PrivateKey getPrivateKey() {
        String clientId = getClientId();
        if (!StringUtils.isEmpty(clientId)) {
            ClientKeyPair keyPair = getKeyPair(clientId);
            if (keyPair != null) {
                return keyPair.getPrivateKey();
            }
        }
        return null;
    }

    public String getClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getClass().isAssignableFrom(OAuth2Authentication.class)) {
                OAuth2Authentication oAuth2Authentication = ((OAuth2Authentication) authentication);
                return oAuth2Authentication.getOAuth2Request().getClientId();
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
                KeyPair keyPair = simpleClientDetails.keyPair();
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

    public static class ClientKeyPair {

        private PublicKey publicKey;

        private PrivateKey privateKey;

        private String pemPublicKey;

        public ClientKeyPair(KeyPair keyPair) {
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
            PemObject pemObject = new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded());
            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PEMWriter(writer);
            try {
                pemWriter.writeObject(pemObject);
                pemWriter.close();
                this.pemPublicKey = writer.toString();
            } catch (IOException e) {
                LOG.error("Exception", e);
            }
        }

        public String getPemPublicKey() {
            return pemPublicKey;
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
