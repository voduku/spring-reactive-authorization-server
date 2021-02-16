package com.example.uaa.config;

import com.example.uaa.oauth.ReactiveTokenGranter;
import com.example.uaa.oauth.client.ReactiveClientDetailsService;
import com.example.uaa.oauth.configuration.ReactiveAuthorizationServerConfigurer;
import com.example.uaa.oauth.configuration.ReactiveClientDetailsServiceConfiguration;
import com.example.uaa.oauth.configuration.configurers.ReactiveAuthorizationServerEndpointsConfigurer;
import com.example.uaa.oauth.configuration.configurers.ReactiveClientDetailsServiceConfigurer;
import com.example.uaa.oauth.token.R2dbcTokenStore;
import com.example.uaa.oauth.token.ReactiveTokenServices;
import com.example.uaa.oauth.token.ReactiveTokenStore;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AuthenticationServerConfig implements ReactiveAuthorizationServerConfigurer {

  private final ReactiveClientDetailsService clientDetailsService;
  private final ReactiveTokenServices tokenServices;
  private final ReactiveTokenStore tokenStore;
  private final ReactiveAuthenticationManager authenticationManager;
  private final ReactiveTokenGranter tokenGranter;


  @Override
  public void configure(ReactiveClientDetailsServiceConfigurer clients) {
    clients.withClientDetails(clientDetailsService);
  }

  @Override
  public void configure(ReactiveAuthorizationServerEndpointsConfigurer endpoints) {
    endpoints.clientDetailsService(clientDetailsService)
        .tokenServices(tokenServices)
        .tokenStore(tokenStore)
        .authenticationManager(authenticationManager)
        .tokenGranter(tokenGranter);
  }
}
