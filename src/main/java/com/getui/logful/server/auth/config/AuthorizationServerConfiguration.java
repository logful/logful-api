package com.getui.logful.server.auth.config;

import com.getui.logful.server.Constants;
import com.getui.logful.server.auth.OAuth2RepositoryTokenStore;
import com.getui.logful.server.auth.service.SimpleClientDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    @Autowired
    OAuth2RepositoryTokenStore oAuth2RepositoryTokenStore;

    @Autowired
    SimpleClientDetailService simpleClientDetailService;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.tokenStore(oAuth2RepositoryTokenStore);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(simpleClientDetailService);
        /*
        clients.inMemory()
                .withClient(Constants.CLIENT_ID)
                .secret(Constants.CLIENT_SECRET)
                .resourceIds("RESOURCE_ID")
                .authorizedGrantTypes("refresh_token", "client_credentials")
                .authorities("ROLE_CLIENT")
                .scopes("client");*/
    }
}
