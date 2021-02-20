package authorization;

import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public interface ReactiveSecurityContextAccessor {


  /**
   * @return true if the current context represents a user
   */
  Mono<Boolean> isUser();

  /**
   * Get the current granted authorities (never null)
   */
  Flux<GrantedAuthority> getAuthorities();
}
