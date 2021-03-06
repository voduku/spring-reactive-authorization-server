package authorization.configuration;

import authorization.client.ReactiveClientDetailsService;
import authorization.configuration.configurers.ReactiveAuthorizationServerSecurityConfigurer;
import authorization.configuration.configurers.ReactiveAuthorizationServerSecurityConfigurer.SecurityAccess;
import authorization.context.R2dbcSecurityContextRepository;
import authorization.endpoint.FrameworkEndpointReactiveHandlerMapping;
import authorization.token.ReactiveTokenStore;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @author VuDo
 * @since 2/25/2021
 */
@Order(0)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration(proxyBeanMethods = false)
@Import(ReactiveAuthorizationServerEndpointsConfiguration.class)
public class ReactiveAuthorizationServerSecurityConfiguration {

  private final List<ReactiveAuthorizationServerConfigurer> configurers;
  private ServerHttpSecurity security;

  @Autowired
  public ReactiveAuthorizationServerSecurityConfiguration(List<ReactiveAuthorizationServerConfigurer> configurers, ServerHttpSecurity security) {
    this.configurers = configurers != null ? configurers : Collections.emptyList();
    this.security = security;
  }

  @Bean
  public SecurityWebFilterChain authorizationServerSecurityFilterChain(ReactiveAuthorizationServerEndpointsConfiguration endpoints,
      ReactiveTokenStore tokenStore, ReactiveClientDetailsService clientDetailsService) {
    ReactiveAuthorizationServerSecurityConfigurer configurer = new ReactiveAuthorizationServerSecurityConfigurer();
    FrameworkEndpointReactiveHandlerMapping handlerMapping = endpoints.oauth2EndpointHandlerMapping();

    String tokenEndpointPath = handlerMapping.getServerPath("/oauth/token");
    String tokenKeyPath = handlerMapping.getServerPath("/oauth/token_key");
    String checkTokenPath = handlerMapping.getServerPath("/oauth/check_token");
    security = security.authorizeExchange(exchange -> {
      exchange.pathMatchers(tokenEndpointPath).authenticated();
      setPathMatcher(exchange, tokenKeyPath, configurer.getTokenKeyAccess());
      setPathMatcher(exchange, checkTokenPath, configurer.getCheckTokenAccess());
    })
        .securityContextRepository(new R2dbcSecurityContextRepository(tokenStore, clientDetailsService));
    configure(configurer);
    return security.build();
  }

  private void setPathMatcher(AuthorizeExchangeSpec exchange, String path, SecurityAccess access) {
    switch (access) {
      case AUTHENTICATED:
        exchange.pathMatchers(path).authenticated();
        break;
      case DENY_ALL:
        exchange.pathMatchers(path).denyAll();
        break;
      case PERMIT_ALL:
        exchange.pathMatchers(path).permitAll();
        break;
      case AUTHORITY:
        exchange.pathMatchers(path).hasAnyAuthority(access.getValue());
        break;
      case ROLE:
        exchange.pathMatchers(path).hasAnyRole(access.getValue());
        break;
    }
  }

  protected void configure(ReactiveAuthorizationServerSecurityConfigurer oauthServer) {
    oauthServer.setSecurity(security);
    for (ReactiveAuthorizationServerConfigurer configurer : configurers) {
      configurer.configure(oauthServer);
    }
  }
}
