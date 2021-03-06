package uaa.config;

import authorization.client.ReactiveClientDetailsService;
import authorization.configuration.ReactiveClientDetailsServiceConfiguration;
import authorization.token.R2dbcTokenStore;
import authorization.token.ReactiveTokenServices;
import authorization.token.ReactiveTokenStore;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AuthenticationServiceConfig {


  @Bean
  public ReactiveTokenStore tokenStore(ConnectionFactory connectionFactory) {
    return new R2dbcTokenStore(connectionFactory);
  }

  @Bean
  public ReactiveClientDetailsService clientDetailsService(ConnectionFactory connectionFactory) throws Exception {
    ReactiveClientDetailsServiceConfiguration serviceConfig = new ReactiveClientDetailsServiceConfiguration();
    serviceConfig.clientDetailsServiceConfigurer().r2dbc(connectionFactory);
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
