package authorization.configuration;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import authorization.ReactiveOAuth2RequestFactory;
import authorization.ReactiveTokenGranter;
import authorization.client.ReactiveClientDetailsService;
import authorization.configuration.ReactiveAuthorizationServerEndpointsConfiguration.TokenKeyEndpointRegistrar;
import authorization.configuration.configurers.ReactiveAuthorizationServerEndpointsConfigurer;
import authorization.endpoint.FrameworkEndpointReactiveHandlerMapping;
import authorization.endpoint.ReactiveTokenEndpoint;
import authorization.token.ReactiveAuthorizationServerTokenServices;
import authorization.token.ReactiveConsumerTokenServices;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurer;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.TokenKeyEndpoint;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@Configuration(proxyBeanMethods = false)
@Import(TokenKeyEndpointRegistrar.class)
public class ReactiveAuthorizationServerEndpointsConfiguration {

  private final ReactiveAuthorizationServerEndpointsConfigurer endpoints = new ReactiveAuthorizationServerEndpointsConfigurer();

  @Autowired
  private ReactiveClientDetailsService clientDetailsService;

  @Autowired
  private final List<ReactiveAuthorizationServerConfigurer> configurers = Collections.emptyList();

  @PostConstruct
  public void init() {
    for (ReactiveAuthorizationServerConfigurer configurer : configurers) {
      try {
        configurer.configure(endpoints);
      } catch (Exception e) {
        throw new IllegalStateException("Cannot configure endpoints", e);
      }
    }
    endpoints.setClientDetailsService(clientDetailsService);
  }

/* TODO uncomment after reactive endpoint is created

  @Bean
  public AuthorizationEndpoint authorizationEndpoint() {
    AuthorizationEndpoint authorizationEndpoint = new AuthorizationEndpoint();
    FrameworkEndpointHandlerMapping mapping = getEndpointsConfigurer().getFrameworkEndpointHandlerMapping();
    authorizationEndpoint.setUserApprovalPage(extractPath(mapping, "/oauth/confirm_access"));
    authorizationEndpoint.setProviderExceptionHandler(exceptionTranslator());
    authorizationEndpoint.setErrorPage(extractPath(mapping, "/oauth/error"));
    authorizationEndpoint.setTokenGranter(tokenGranter());
    authorizationEndpoint.setClientDetailsService(clientDetailsService);
    authorizationEndpoint.setAuthorizationCodeServices(authorizationCodeServices());
    authorizationEndpoint.setOAuth2RequestFactory(oauth2RequestFactory());
    authorizationEndpoint.setOAuth2RequestValidator(oauth2RequestValidator());
    authorizationEndpoint.setUserApprovalHandler(userApprovalHandler());
    authorizationEndpoint.setRedirectResolver(redirectResolver());
    return authorizationEndpoint;
  }
*/

//  @Bean
//  ConnectionFactory postgresConnectionFactory(
//      @Value("${spring.r2dbc.url}") String host,
//      @Value("${spring.r2dbc.port}") Integer port,
//      @Value("${spring.r2dbc.database}") String database,
//      @Value("${spring.r2dbc.username}") String username,
//      @Value("${spring.r2dbc.password}") String password
//  ) {
//    return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
//        .host(host)
//        .port(port)
//        .database(database)
//        .username(username)
//        .password(password)
//        .build());
//  }

  @Bean
  public ReactiveTokenEndpoint tokenEndpoint() {
    ReactiveTokenEndpoint tokenEndpoint = new ReactiveTokenEndpoint();
    tokenEndpoint.setClientDetailsService(clientDetailsService);
    tokenEndpoint.setProviderExceptionHandler(exceptionTranslator());
    tokenEndpoint.setTokenGranter(tokenGranter());
    tokenEndpoint.setOAuth2RequestFactory(oauth2RequestFactory());
    return tokenEndpoint;
  }

  @Bean
  public RouterFunction<ServerResponse> tokenRoute(ReactiveTokenEndpoint tokenEndpoint) {
    return route().POST("/oauth/token", tokenEndpoint::postAccessToken).build();
  }

  /* TODO uncomment after reactive endpoint is created

    @Bean
    public CheckTokenEndpoint checkTokenEndpoint() {
      CheckTokenEndpoint endpoint = new CheckTokenEndpoint(getEndpointsConfigurer().getResourceServerTokenServices());
      endpoint.setAccessTokenConverter(getEndpointsConfigurer().getAccessTokenConverter());
      endpoint.setExceptionTranslator(exceptionTranslator());
      return endpoint;
    }


    @Bean
    public WhitelabelApprovalEndpoint whitelabelApprovalEndpoint() {
      return new WhitelabelApprovalEndpoint();
    }

    @Bean
    public WhitelabelErrorEndpoint whitelabelErrorEndpoint() {
      return new WhitelabelErrorEndpoint();
    }

  */
  @Bean
  public FrameworkEndpointReactiveHandlerMapping oauth2EndpointHandlerMapping() {
    return getEndpointsConfigurer().getFrameworkEndpointHandlerMapping();
  }


  @Bean
  public FactoryBean<ReactiveConsumerTokenServices> consumerTokenServices() {
    return new AbstractFactoryBean<ReactiveConsumerTokenServices>() {

      @Override
      public Class<?> getObjectType() {
        return ReactiveConsumerTokenServices.class;
      }

      @NonNull
      @Override
      protected ReactiveConsumerTokenServices createInstance() {
        return getEndpointsConfigurer().getConsumerTokenServices();
      }
    };
  }

  /**
   * This needs to be a <code>@Bean</code> so that it can be
   * <code>@Transactional</code> (in case the token store supports them). If
   * you are overriding the token services in an {@link AuthorizationServerConfigurer} consider making it a
   * <code>@Bean</code> for the same reason (assuming you need transactions,
   * e.g. for a JDBC token store).
   *
   * @return an AuthorizationServerTokenServices
   */
  @Bean
  public FactoryBean<ReactiveAuthorizationServerTokenServices> defaultAuthorizationServerTokenServices() {
    return new AuthorizationServerTokenServicesFactoryBean(endpoints);
  }

  public ReactiveAuthorizationServerEndpointsConfigurer getEndpointsConfigurer() {
    if (!endpoints.isTokenServicesOverride()) {
      try {
        endpoints.tokenServices(endpoints.getDefaultAuthorizationServerTokenServices());
      } catch (Exception e) {
        throw new BeanCreationException("Cannot create token services", e);
      }
    }
    return endpoints;
  }

  private ReactiveOAuth2RequestFactory oauth2RequestFactory() {
    return getEndpointsConfigurer().getOAuth2RequestFactory();
  }

  private WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator() {
    return getEndpointsConfigurer().getExceptionTranslator();
  }

  private RedirectResolver redirectResolver() {
    return getEndpointsConfigurer().getRedirectResolver();
  }

  private ReactiveTokenGranter tokenGranter() {
    return getEndpointsConfigurer().getTokenGranter();
  }

  private String extractPath(FrameworkEndpointHandlerMapping mapping, String page) {
    String path = mapping.getPath(page);
    if (path.contains(":")) {
      return path;
    }
    return "forward:" + path;
  }

  protected static class AuthorizationServerTokenServicesFactoryBean
      extends AbstractFactoryBean<ReactiveAuthorizationServerTokenServices> {

    private ReactiveAuthorizationServerEndpointsConfigurer endpoints;

    protected AuthorizationServerTokenServicesFactoryBean() {
    }

    public AuthorizationServerTokenServicesFactoryBean(
        ReactiveAuthorizationServerEndpointsConfigurer endpoints) {
      this.endpoints = endpoints;
    }

    @Override
    public Class<?> getObjectType() {
      return ReactiveAuthorizationServerTokenServices.class;
    }

    @NonNull
    @Override
    protected ReactiveAuthorizationServerTokenServices createInstance() {
      return endpoints.getDefaultAuthorizationServerTokenServices();
    }
  }

  @Component
  protected static class TokenKeyEndpointRegistrar implements BeanDefinitionRegistryPostProcessor {

    private BeanDefinitionRegistry registry;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
      String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
          JwtAccessTokenConverter.class, false, false);
      if (names.length > 0) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TokenKeyEndpoint.class);
        builder.addConstructorArgReference(names[0]);
        registry.registerBeanDefinition(TokenKeyEndpoint.class.getName(), builder.getBeanDefinition());
      }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
      this.registry = registry;
    }

  }

}