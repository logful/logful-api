package com.getui.logful.server.auth.repository;

import com.getui.logful.server.auth.model.OAuth2AuthenticationAccessToken;
import com.getui.logful.server.auth.model.OAuth2AuthenticationRefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.stereotype.Repository;

@Repository
public class OAuth2TokenCommonRepository {

    private final MongoOperations operations;

    @Autowired
    public OAuth2TokenCommonRepository(MongoOperations operations) {
        this.operations = operations;
    }

    public void storeAccessToken(OAuth2AuthenticationAccessToken accessToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenId").is(accessToken.getTokenId()));
        OAuth2AuthenticationAccessToken temp = operations.findOne(query, OAuth2AuthenticationAccessToken.class);
        if (temp != null) {
            accessToken.setId(temp.getId());
        }
        operations.save(accessToken);
    }

    public void storeRefreshToken(OAuth2AuthenticationRefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenId").is(refreshToken.getTokenId()));
        OAuth2AuthenticationRefreshToken temp = operations.findOne(query, OAuth2AuthenticationRefreshToken.class);
        if (temp != null) {
            refreshToken.setId(temp.getId());
        }
        operations.save(refreshToken);
    }

    public void removeRefreshToken(OAuth2RefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenId").is(refreshToken.getValue()));
        operations.remove(query, OAuth2AuthenticationRefreshToken.class);
    }

    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("refreshToken").is(refreshToken.getValue()));
        operations.remove(query, OAuth2AuthenticationAccessToken.class);
    }

    public void removeAccessToken(OAuth2AccessToken accessToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenId").is(accessToken.getValue()));
        operations.remove(query, OAuth2AuthenticationAccessToken.class);
    }
}
