package com.getui.logful.server.auth.config;

import com.getui.logful.server.auth.OAuth2RepositoryTokenStore;
import com.getui.logful.server.auth.SimpleTokenEnhancer;
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
    SimpleTokenEnhancer simpleTokenEnhancer;

    @Autowired
    OAuth2RepositoryTokenStore oAuth2RepositoryTokenStore;

    @Autowired
    SimpleClientDetailService simpleClientDetailService;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.tokenStore(oAuth2RepositoryTokenStore)
                .tokenEnhancer(simpleTokenEnhancer);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(simpleClientDetailService);
        /*
        clients.inMemory()
                .withClient("4c6c1fc86fa3f7fe715d3c1937e167f3")
                .secret("288017f4110cb92f2e9a29fbefca0fd2")
                .resourceIds("RESOURCE_ID")
                .authorizedGrantTypes("refresh_token", "client_credentials")
                .authorities("ROLE_CLIENT")
                .scopes("client");
                */
    }


}
