package com.getui.logful.server.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.*;

@Document(collection = "oauth2_client")
public class SimpleClientDetails {

    private static final String[] GRANT_TYPE = new String[]{"refresh_token", "client_credentials"};

    @Id
    private String id;
    @Indexed
    private String clientId;
    private Set<String> resourceIds;
    private boolean secretRequired;
    @Indexed
    private String clientSecret;
    private boolean scoped;
    private Set<String> scope;
    private Set<String> authorizedGrantTypes;
    private Set<String> registeredRedirectUri;
    private Collection<String> authorities;
    private Integer accessTokenValiditySeconds;
    private Integer refreshTokenValiditySeconds;
    private boolean autoApprove;
    private Map<String, Object> additionalInformation;

    public SimpleClientDetails() {
        BaseClientDetails temp = new BaseClientDetails();

        this.clientId = "525b8747323d49078a96e49f0189de98";
        this.authorizedGrantTypes = new HashSet<>(Arrays.asList(GRANT_TYPE));

        this.setAccessTokenValiditySeconds(temp.getAccessTokenValiditySeconds());
        this.setRefreshTokenValiditySeconds(temp.getRefreshTokenValiditySeconds());

        this.registeredRedirectUri = new HashSet<>();

        this.clientSecret = "02ce8e2adba94ae5a4807e3f12ea34f3";
        this.scope = new HashSet<>(Arrays.asList(new String[]{"client"}));
        this.authorities = new HashSet<>(Arrays.asList(new String[]{"logful_client"}));

        // TODO
        this.resourceIds = new HashSet<>();

        this.setAdditionalInformation(new HashMap<String, Object>());
    }

    public SimpleClientDetails(ClientDetails clientDetails) {
        this.accessTokenValiditySeconds = clientDetails.getAccessTokenValiditySeconds();
        this.additionalInformation = clientDetails.getAdditionalInformation();
        this.authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        this.clientId = clientDetails.getClientId();
        this.clientSecret = clientDetails.getClientSecret();
        this.refreshTokenValiditySeconds = clientDetails.getRefreshTokenValiditySeconds();
        this.registeredRedirectUri = clientDetails.getRegisteredRedirectUri();
        this.resourceIds = clientDetails.getResourceIds();
        this.scope = clientDetails.getScope();
        this.scoped = clientDetails.isScoped();
        this.secretRequired = clientDetails.isSecretRequired();
        // this.id = clientDetails.getClientId();
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public boolean isSecretRequired() {
        return secretRequired;
    }

    public void setSecretRequired(boolean secretRequired) {
        this.secretRequired = secretRequired;
    }

    public boolean isScoped() {
        return scoped;
    }

    public void setScoped(boolean scoped) {
        this.scoped = scoped;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    public Set<String> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public Set<String> getRegisteredRedirectUri() {
        return registeredRedirectUri;
    }

    public void setRegisteredRedirectUri(Set<String> registeredRedirectUri) {
        this.registeredRedirectUri = registeredRedirectUri;
    }

    public Collection<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<String> authorities) {
        this.authorities = authorities;
    }

    public Integer getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public Integer getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}
