package com.example.uaa.config;

import com.example.uaa.oauth.client.ReactiveClientDetailsService;
import com.example.uaa.oauth.configuration.ReactiveClientDetailsServiceConfiguration;
import com.example.uaa.oauth.token.R2dbcTokenStore;
import com.example.uaa.oauth.token.ReactiveTokenServices;
import com.example.uaa.oauth.token.ReactiveTokenStore;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AuthenticationServiceConfig {

  @Bean
  ConnectionFactory postgresConnectionFactory(
      @Value("${spring.r2dbc.url}") String url,
      @Value("${spring.r2dbc.username}") String username,
      @Value("${spring.r2dbc.password}") String password
  ) throws URISyntaxException {
    UriComponents uri = UriComponentsBuilder.fromUriString(url).build();
    String host = uri.getHost();
    int port = uri.getPort();
    String database = uri.getPathSegments().get(0);
    if (host == null) {
      throw new URISyntaxException("null", "url can't be: ");
    }
    return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
        .host(host)
        .port(port)
        .database(database)
        .username(username)
        .password(password)
        .build());
  }

  @Bean
  public ReactiveTokenStore tokenStore(ConnectionFactory postgresConnectionFactory) {
    return new R2dbcTokenStore(postgresConnectionFactory);
  }

  @Bean
  public ReactiveClientDetailsService clientDetailsService(ConnectionFactory postgresConnectionFactory) throws Exception {
    ReactiveClientDetailsServiceConfiguration serviceConfig = new ReactiveClientDetailsServiceConfiguration();
    serviceConfig.clientDetailsServiceConfigurer().r2dbc(postgresConnectionFactory);
    return serviceConfig.clientDetailsService();
  }

  @Bean
  public ReactiveTokenServices tokenServices(ReactiveTokenStore tokenStore, ReactiveClientDetailsService clientDetailsService) {
    final ReactiveTokenServices tokenServices = new ReactiveTokenServices();
    tokenServices.setTokenStore(tokenStore);
    tokenServices.setSupportRefreshToken(true);
    tokenServices.setClientDetailsService(clientDetailsService);
    tokenServices.setAccessTokenValiditySeconds(0);
    return tokenServices;
  }


}
