package authorization.impl;

import authorization.ReactiveSecurityContextAccessor;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public class DefaultReactiveSecurityContextAccessor implements ReactiveSecurityContextAccessor {

  @Override
  public Mono<Boolean> isUser() {
    return getUserAuthentication().map(Objects::nonNull);
  }

  @Override
  public Flux<GrantedAuthority> getAuthorities() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .flatMapMany(authorities -> Flux.fromIterable(new HashSet<GrantedAuthority>(authorities)))
        .switchIfEmpty(Flux.empty());
  }

  private Mono<Authentication> getUserAuthentication() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(this::getAuthentication);
  }

  private Authentication getAuthentication(Authentication authentication) {
    return authentication instanceof OAuth2Authentication ? ((OAuth2Authentication) authentication).getUserAuthentication() : authentication;
  }
}
