package uaa.handler;

import authorization.endpoint.ReactiveTokenEndpoint;
import authorization.token.ReactiveTokenStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import uaa.model.oauth.Auth;
import uaa.model.oauth.AuthRequest;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@Component
@RequiredArgsConstructor
public class AuthHandler {

  private final ReactiveTokenEndpoint tokenEndpoint;

  private final ReactiveTokenStore tokenStore;
//
//  public Mono<ServerResponse> login(ServerRequest request) {
//    return  request.bodyToMono(AuthRequest.class)
//        .map(rq -> ServerRequest.from(request)
//            .attribute("principal", new Auth(rq))
//            .attribute("parameters", rq.toParamsMap())
//            .build())
//        .flatMap(tokenEndpoint::postAccessToken);
//  }

  public Mono<ServerResponse> login(AuthRequest request) {
    return tokenEndpoint.postAccessToken(new Auth(request, null), request.toParamsMap());
  }

  public Mono<Map<String, Object>> details(String token) {
    return tokenStore.readAuthentication(token.replace("Bearer ", ""))
        .map(authentication -> {
          Map<String, Object> map = new HashMap<>();
          map.put("principal", authentication.getPrincipal());
          map.put("authorities", authentication.getAuthorities());
          return Collections.unmodifiableMap(map);
        });
  }
}
