package com.getui.logful.server.auth.repository;

import com.getui.logful.server.auth.model.OAuth2AuthenticationRefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuth2RefreshTokenRepository extends MongoRepository<OAuth2AuthenticationRefreshToken, String> {

    OAuth2AuthenticationRefreshToken findByTokenId(String tokenId);
}
