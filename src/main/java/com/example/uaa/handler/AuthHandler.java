package com.example.uaa.handler;

import com.example.uaa.model.oauth.Auth;
import com.example.uaa.model.oauth.AuthRequest;
import com.example.uaa.oauth.endpoint.ReactiveTokenEndpoint;
import com.example.uaa.oauth.token.ReactiveTokenStore;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

  public Mono<Map<String, Object>> details(String token){
    return tokenStore.readAuthentication(token.toLowerCase().replace("bearer ",""))
        .map(authentication -> ImmutableMap.of(
            "principal", authentication.getPrincipal(),
            "authorities", authentication.getAuthorities()
        ));
  }
}
