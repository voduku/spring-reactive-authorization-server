package authorization.configuration.configurers;

import authorization.ReactiveOAuth2RequestFactory;
import authorization.ReactiveTokenGranter;
import authorization.client.R2dbcClientDetailsService;
import authorization.client.ReactiveClientDetailsService;
import authorization.endpoint.FrameworkEndpointReactiveHandlerMapping;
import authorization.endpoint.WebfluxResponseExceptionTranslator;
import authorization.impl.DefaultReactiveOAuth2RequestFactory;
import authorization.token.ReactiveAuthorizationServerTokenServices;
import authorization.token.ReactiveConsumerTokenServices;
import authorization.token.ReactiveResourceServerTokenServices;
import authorization.token.ReactiveTokenServices;
import authorization.token.ReactiveTokenStore;
import io.r2dbc.spi.ConnectionFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.util.ProxyCreator;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@Getter
@Setter
public class ReactiveAuthorizationServerEndpointsConfigurer {

  private ReactiveAuthorizationServerTokenServices tokenServices;

  private ReactiveConsumerTokenServices consumerTokenServices;

  private AuthorizationCodeServices authorizationCodeServices;

  private ReactiveResourceServerTokenServices resourceTokenServices;

  private ConnectionFactory connectionFactory;

  private ReactiveTokenStore tokenStore;

  private AccessTokenConverter accessTokenConverter;

  private ReactiveTokenGranter tokenGranter;

  private ReactiveOAuth2RequestFactory requestFactory;

  private OAuth2RequestValidator requestValidator;

  private ReactiveAuthenticationManager authenticationManager;

  private ReactiveClientDetailsService clientDetailsService;

  private String prefix;

  private Map<String, String> patternMap = new HashMap<>();

  private Set<HttpMethod> allowedTokenEndpointRequestMethods = new HashSet<>();

  private FrameworkEndpointReactiveHandlerMapping frameworkEndpointHandlerMapping;

  private List<Object> interceptors = new ArrayList<>();

  private ReactiveTokenServices defaultTokenServices;

  private UserDetailsService userDetailsService;

  private boolean tokenServicesOverride = false;

  private boolean userDetailsServiceOverride = false;

  private boolean reuseRefreshToken = true;

  private WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator;

  private RedirectResolver redirectResolver;

  public ReactiveAuthorizationServerTokenServices getTokenServices() {
    return ProxyCreator.getProxy(ReactiveAuthorizationServerTokenServices.class, this::tokenServices);
  }


  public AccessTokenConverter getAccessTokenConverter() {
    return accessTokenConverter();
  }


  public ReactiveClientDetailsService getClientDetailsService() {
    return ProxyCreator.getProxy(ReactiveClientDetailsService.class, this::clientDetailsService);
  }

  /**
   * N.B. this method is not part of the public API. To set up a custom ClientDetailsService please use {@link
   * AuthorizationServerConfigurerAdapter#configure(ClientDetailsServiceConfigurer)}.
   */
  public ReactiveAuthorizationServerEndpointsConfigurer clientDetailsService(ReactiveClientDetailsService clientDetailsService) {
    this.clientDetailsService = clientDetailsService;
    return this;
  }

  public ReactiveOAuth2RequestFactory getOAuth2RequestFactory() {
    return ProxyCreator.getProxy(ReactiveOAuth2RequestFactory.class, this::requestFactory);
  }

  public OAuth2RequestValidator getOAuth2RequestValidator() {
    return requestValidator();
  }

  public ReactiveAuthorizationServerEndpointsConfigurer tokenStore(ReactiveTokenStore tokenStore) {
    this.tokenStore = tokenStore;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer reuseRefreshTokens(boolean reuseRefreshToken) {
    this.reuseRefreshToken = reuseRefreshToken;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer accessTokenConverter(AccessTokenConverter accessTokenConverter) {
    this.accessTokenConverter = accessTokenConverter;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer tokenServices(ReactiveAuthorizationServerTokenServices tokenServices) {
    this.tokenServices = tokenServices;
    if (tokenServices != null) {
      this.tokenServicesOverride = true;
    }
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer redirectResolver(RedirectResolver redirectResolver) {
    this.redirectResolver = redirectResolver;
    return this;
  }

  public boolean isTokenServicesOverride() {
    return tokenServicesOverride;
  }

  public boolean isUserDetailsServiceOverride() {
    return userDetailsServiceOverride;
  }

  /**
   * Explicitly disable the approval store, even if one would normally be added automatically (usually when JWT is not used). Without an approval store the user
   * can only be asked to approve or deny a grant without any more granular decisions.
   *
   * @return this for fluent builder
   */


  public ReactiveAuthorizationServerEndpointsConfigurer prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer pathMapping(String defaultPath, String customPath) {
    this.patternMap.put(defaultPath, customPath);
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer addInterceptor(
      HandlerInterceptor interceptor) {
    this.interceptors.add(interceptor);
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer addInterceptor(
      WebRequestInterceptor interceptor) {
    this.interceptors.add(interceptor);
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer exceptionTranslator(WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
    return this;
  }

  /**
   * The AuthenticationManager for the password grant.
   *
   * @param authenticationManager an AuthenticationManager, fully initialized
   * @return this for a fluent style
   */
  public ReactiveAuthorizationServerEndpointsConfigurer authenticationManager(ReactiveAuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer tokenGranter(ReactiveTokenGranter tokenGranter) {
    this.tokenGranter = tokenGranter;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer requestFactory(ReactiveOAuth2RequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer requestValidator(OAuth2RequestValidator requestValidator) {
    this.requestValidator = requestValidator;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer authorizationCodeServices(
      AuthorizationCodeServices authorizationCodeServices) {
    this.authorizationCodeServices = authorizationCodeServices;
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer allowedTokenEndpointRequestMethods(HttpMethod... requestMethods) {
    Collections.addAll(allowedTokenEndpointRequestMethods, requestMethods);
    return this;
  }

  public ReactiveAuthorizationServerEndpointsConfigurer userDetailsService(UserDetailsService userDetailsService) {
    if (userDetailsService != null) {
      this.userDetailsService = userDetailsService;
      this.userDetailsServiceOverride = true;
    }
    return this;
  }

//  public ReactiveTokenGranter getTokenGranter() {
//    return tokenGranter();
//  }

  public FrameworkEndpointReactiveHandlerMapping getFrameworkEndpointHandlerMapping() {
    return frameworkEndpointHandlerMapping();
  }

  public WebResponseExceptionTranslator<OAuth2Exception> getExceptionTranslator() {
    return exceptionTranslator();
  }

  public RedirectResolver getRedirectResolver() {
    return redirectResolver();
  }

  private ReactiveResourceServerTokenServices resourceTokenServices() {
    if (resourceTokenServices == null) {
      if (tokenServices instanceof ResourceServerTokenServices) {
        return (ReactiveResourceServerTokenServices) tokenServices;
      }
      resourceTokenServices = createDefaultTokenServices();
    }
    return resourceTokenServices;
  }

  private Set<HttpMethod> allowedTokenEndpointRequestMethods() {
    // HTTP POST should be the only allowed endpoint request method by default.
    if (allowedTokenEndpointRequestMethods.isEmpty()) {
      allowedTokenEndpointRequestMethods.add(HttpMethod.POST);
    }
    return allowedTokenEndpointRequestMethods;
  }

  private ReactiveConsumerTokenServices consumerTokenServices() {
    if (consumerTokenServices == null) {
      if (tokenServices instanceof ReactiveConsumerTokenServices) {
        return (ReactiveConsumerTokenServices) tokenServices;
      }
      consumerTokenServices = createDefaultTokenServices();
    }
    return consumerTokenServices;
  }

  @NonNull
  private ReactiveAuthorizationServerTokenServices tokenServices() {
    if (tokenServices != null) {
      return tokenServices;
    }
    this.tokenServices = createDefaultTokenServices();
    return tokenServices;
  }

  public ReactiveAuthorizationServerTokenServices getDefaultAuthorizationServerTokenServices() {
    if (defaultTokenServices != null) {
      return defaultTokenServices;
    }
    this.defaultTokenServices = createDefaultTokenServices();
    return this.defaultTokenServices;
  }

  private ReactiveTokenServices createDefaultTokenServices() {
    ReactiveTokenServices tokenServices = new ReactiveTokenServices();
    tokenServices.setTokenStore(tokenStore());
    tokenServices.setSupportRefreshToken(true);
    tokenServices.setReuseRefreshToken(reuseRefreshToken);
    tokenServices.setClientDetailsService(clientDetailsService());
//    addUserDetailsService(tokenServices, this.userDetailsService);
    return tokenServices;
  }


  private AccessTokenConverter accessTokenConverter() {
    if (this.accessTokenConverter == null) {
      accessTokenConverter = new DefaultAccessTokenConverter();
    }
    return this.accessTokenConverter;
  }

  private ReactiveTokenStore tokenStore() {
    return this.tokenStore;
  }

  @NonNull
  private ReactiveClientDetailsService clientDetailsService() {
    if (clientDetailsService == null) {
      this.clientDetailsService = new R2dbcClientDetailsService(connectionFactory);
    }
//    if (this.defaultTokenServices != null) {
//      addUserDetailsService(defaultTokenServices, userDetailsService);
//    }
    return this.clientDetailsService;
  }

//  private void addUserDetailsService(DefaultTokenServices tokenServices, UserDetailsService userDetailsService) {
//    if (userDetailsService != null) {
//      PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
//      provider.setPreAuthenticatedUserDetailsService(new UserDetailsByNameServiceWrapper<>(
//          userDetailsService));
//      tokenServices
//          .setAuthenticationManager(new ProviderManager(Collections.singletonList(provider)));
//    }
//  }


  private AuthorizationCodeServices authorizationCodeServices() {
    if (authorizationCodeServices == null) {
      authorizationCodeServices = new InMemoryAuthorizationCodeServices();
    }
    return authorizationCodeServices;
  }

  private WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator() {
    if (exceptionTranslator != null) {
      return exceptionTranslator;
    }
    exceptionTranslator = new WebfluxResponseExceptionTranslator();
    return exceptionTranslator;
  }

  private RedirectResolver redirectResolver() {
    if (redirectResolver != null) {
      return redirectResolver;
    }
    redirectResolver = new DefaultRedirectResolver();
    return redirectResolver;
  }

  private ReactiveOAuth2RequestFactory requestFactory() {
    if (requestFactory != null) {
      return requestFactory;
    }
    requestFactory = new DefaultReactiveOAuth2RequestFactory(clientDetailsService());
    return requestFactory;
  }

  private OAuth2RequestValidator requestValidator() {
    if (requestValidator != null) {
      return requestValidator;
    }
    requestValidator = new DefaultOAuth2RequestValidator();
    return requestValidator;
  }

//  private TokenGranter tokenGranter() {
//    if (tokenGranter == null) {
//      tokenGranter = new TokenGranter() {
//        private CompositeTokenGranter delegate;
//
//        @Override
//        public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
//          if (delegate == null) {
//            delegate = new CompositeTokenGranter(getDefaultTokenGranters());
//          }
//          return delegate.grant(grantType, tokenRequest);
//        }
//      };
//    }
//    return tokenGranter;
//  }

  private FrameworkEndpointReactiveHandlerMapping frameworkEndpointHandlerMapping() {
    if (frameworkEndpointHandlerMapping == null) {
      frameworkEndpointHandlerMapping = new FrameworkEndpointReactiveHandlerMapping();
      frameworkEndpointHandlerMapping.setMappings(patternMap);
      frameworkEndpointHandlerMapping.setPrefix(prefix);
//      frameworkEndpointHandlerMapping.setInterceptors(interceptors.toArray());
    }
    return frameworkEndpointHandlerMapping;
  }

}
