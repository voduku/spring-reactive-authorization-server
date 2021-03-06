package uaa.router;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import uaa.handler.AuthHandler;
import uaa.model.oauth.AuthRequest;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AuthRouter extends BaseRouter {

  private final AuthHandler authHandler;

  @Bean
  public RouterFunction<ServerResponse> router() {
    return route()
        .POST("/login", this::login)
        .GET("/details", this::details)
        .build();
  }

  @NonNull
  public Mono<ServerResponse> login(ServerRequest request) {
    return request
        .bodyToMono(AuthRequest.class)
        .flatMap(authHandler::login)
        .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Couldn't get authentication details")));
  }

  @NonNull
  public Mono<ServerResponse> details(ServerRequest request) {
    return getCurrentAuthentication()
        .map(authentication -> {
          return ImmutableMap.of("principal", authentication.getPrincipal(),
              "authorities", authentication.getAuthorities());
        })
        .flatMap(dto -> ServerResponse.ok().bodyValue(dto))
        .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Couldn't get authentication details")));
  }
}
