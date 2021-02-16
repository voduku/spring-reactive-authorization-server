package com.example.uaa.oauth.configuration.configurers;

import com.example.uaa.oauth.client.ReactiveClientDetailsService;
import com.example.uaa.oauth.configuration.builder.R2dbcClientDetailsServiceBuilder;
import com.example.uaa.oauth.configuration.builder.ReactiveClientDetailsServiceBuilder;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public class ReactiveClientDetailsServiceConfigurer extends SecurityConfigurerAdapter<ReactiveClientDetailsService, ReactiveClientDetailsServiceBuilder<?>> {

  public ReactiveClientDetailsServiceConfigurer(ReactiveClientDetailsServiceBuilder<?> builder) {
    setBuilder(builder);
  }

  public ReactiveClientDetailsServiceBuilder<?> withClientDetails(ReactiveClientDetailsService clientDetailsService) {
    setBuilder(getBuilder().clients(clientDetailsService));
    return this.and();
  }

  public R2dbcClientDetailsServiceBuilder r2dbc(ConnectionFactory connectionFactory) {
    R2dbcClientDetailsServiceBuilder next = getBuilder().r2dbc().connectionFactory(connectionFactory);
    setBuilder(next);
    return next;
  }

  @Override
  public void init(ReactiveClientDetailsServiceBuilder<?> builder) {
  }

  @Override
  public void configure(ReactiveClientDetailsServiceBuilder<?> builder) {
  }
}
