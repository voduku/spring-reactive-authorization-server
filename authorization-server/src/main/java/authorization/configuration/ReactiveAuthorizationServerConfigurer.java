package authorization.configuration;

import authorization.configuration.configurers.ReactiveAuthorizationServerEndpointsConfigurer;
import authorization.configuration.configurers.ReactiveAuthorizationServerSecurityConfigurer;
import authorization.configuration.configurers.ReactiveClientDetailsServiceConfigurer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.provider.ClientDetailsService;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public interface ReactiveAuthorizationServerConfigurer {

  /**
   * Configure the security of the Authorization Server, which means in practical terms the /oauth/token endpoint. The /oauth/authorize endpoint also needs to
   * be secure, but that is a normal user-facing endpoint and should be secured the same way as the rest of your UI, so is not covered here. The default
   * settings cover the most common requirements, following recommendations from the OAuth2 spec, so you don't need to do anything here to get a basic server up
   * and running.
   *
   * @param security a fluent configurer for security features
   */
  void configure(ReactiveAuthorizationServerSecurityConfigurer security);

  /**
   * Configure resource matching due to limitation only 1 ServerHttpSecurity bean can will be picked up.
   *
   * @param security a fluent configurer for security features
   */
  void configure(ServerHttpSecurity security);

  /**
   * Configure the {@link ClientDetailsService}, e.g. declaring individual clients and their properties. Note that password grant is not enabled (even if some
   * clients are allowed it) unless an {@link AuthenticationManager} is supplied to the {@link #configure(ReactiveClientDetailsServiceConfigurer)}. At least one
   * client, or a fully formed custom {@link ClientDetailsService} must be declared or the server will not start.
   *
   * @param clients the client details configurer
   */
  void configure(ReactiveClientDetailsServiceConfigurer clients) throws Exception;

  /**
   * Configure the non-security features of the Authorization Server endpoints, like token store, token customizations, user approvals and grant types. You
   * shouldn't need to do anything by default, unless you need password grants, in which case you need to provide an {@link AuthenticationManager}.
   *
   * @param endpoints the endpoints configurer
   */
  void configure(ReactiveAuthorizationServerEndpointsConfigurer endpoints) throws Exception;
}
