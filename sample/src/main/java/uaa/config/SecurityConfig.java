package uaa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @author VuDo
 * @since 2/6/2021
 */
public class SecurityConfig {

//  @Bean
//  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//    http.authorizeExchange()
//        .pathMatchers("/", "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**", "/login", "/uaa/partners/**/login").permitAll()
//        .pathMatchers("/**").authenticated();
//    return http
//        .httpBasic().disable()
//        .csrf().disable()
//        .formLogin().disable()
//        .logout().disable()
//        .build();
//  }

}
