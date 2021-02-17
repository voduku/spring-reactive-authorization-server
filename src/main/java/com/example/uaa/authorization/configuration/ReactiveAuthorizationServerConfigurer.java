package com.example.uaa.authorization.configuration;

import com.example.uaa.authorization.configuration.configurers.ReactiveAuthorizationServerEndpointsConfigurer;
import com.example.uaa.authorization.configuration.configurers.ReactiveClientDetailsServiceConfigurer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.provider.ClientDetailsService;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public interface ReactiveAuthorizationServerConfigurer {

  /**
   * Configure the {@link ClientDetailsService}, e.g. declaring individual clients and their properties. Note that password grant is not enabled (even if some
   * clients are allowed it) unless an {@link AuthenticationManager} is supplied to the {@link #configure(org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer)}.
   * At least one client, or a fully formed custom {@link ClientDetailsService} must be declared or the server will not start.
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
