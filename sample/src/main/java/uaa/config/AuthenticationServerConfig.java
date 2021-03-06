package uaa.config;

import authorization.ReactiveTokenGranter;
import authorization.client.ReactiveClientDetailsService;
import authorization.configuration.ReactiveAuthorizationServerConfigurer;
import authorization.configuration.configurers.ReactiveAuthorizationServerEndpointsConfigurer;
import authorization.configuration.configurers.ReactiveAuthorizationServerSecurityConfigurer;
import authorization.configuration.configurers.ReactiveAuthorizationServerSecurityConfigurer.SecurityAccess;
import authorization.configuration.configurers.ReactiveClientDetailsServiceConfigurer;
import authorization.token.ReactiveTokenServices;
import authorization.token.ReactiveTokenStore;
import lombok.RequiredArgsConstructor;
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
  public void configure(ReactiveAuthorizationServerSecurityConfigurer configurer) {
    configurer.checkTokenAccess(SecurityAccess.AUTHENTICATED)
        .tokenKeyAccess(SecurityAccess.PERMIT_ALL)
        .security(http -> http.authorizeExchange()
            .pathMatchers("/", "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**", "/login", "/partners/{providerId:\\d+}/login").permitAll()
            .pathMatchers("/**").authenticated()
            .and()
            .httpBasic().disable()
            .csrf().disable()
            .formLogin().disable()
            .logout().disable()
        );
  }

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
