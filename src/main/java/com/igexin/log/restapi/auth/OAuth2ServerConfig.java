package com.igexin.log.restapi.auth;

import com.igexin.log.restapi.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
public class OAuth2ServerConfig {

    private static final String RESOURCE_ID = "REST_API";

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId(RESOURCE_ID);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/log/info/**").authenticated()
                    .antMatchers("/log/file/**").authenticated()
                    .antMatchers("/log/crash/**").authenticated()
                    .antMatchers("/log/attachment/**").authenticated();
        }

    }

    @Configuration
    @EnableAuthorizationServer
    protected static class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.inMemory()
                    .withClient(Constants.CLIENT_ID)
                    .secret(Constants.CLIENT_SECRET)
                    .resourceIds(RESOURCE_ID)
                    .authorizedGrantTypes("refresh_token", "client_credentials")
                    .authorities("ROLE_CLIENT")
                    .scopes("client");
        }

    }
}
