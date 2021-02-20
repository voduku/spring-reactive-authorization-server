package authorization.configuration;

import authorization.client.ReactiveClientDetailsService;
import authorization.configuration.builder.ReactiveClientDetailsServiceBuilder;
import authorization.configuration.configurers.ReactiveClientDetailsServiceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * @author VuDo
 * @since 2/14/2021
 */
public class ReactiveClientDetailsServiceConfiguration {

  private final ReactiveClientDetailsServiceConfigurer configurer = new ReactiveClientDetailsServiceConfigurer(new ReactiveClientDetailsServiceBuilder<>());

  @Bean
  public ReactiveClientDetailsServiceConfigurer clientDetailsServiceConfigurer() {
    return configurer;
  }

  @Bean
  @Lazy
  @Scope(proxyMode = ScopedProxyMode.INTERFACES)
  public ReactiveClientDetailsService clientDetailsService() throws Exception {
    return configurer.and().build();
  }
}
