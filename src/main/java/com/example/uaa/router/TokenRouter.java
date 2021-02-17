package com.example.uaa.router;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.example.uaa.authorization.endpoint.ReactiveTokenEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TokenRouter {

  private final ReactiveTokenEndpoint tokenEndpoint;

//  @Bean
//  public RouterFunction<ServerResponse> tokenRoute() {
//    return route().POST("/oauth/token", this::postAccessToken).build();
//  }
//
//  @NonNull
//  public Mono<ServerResponse> postAccessToken(ServerRequest request) {
//    return request.formData()
//        .map(MultiValueMap::toSingleValueMap)
//        .flatMap(params -> tokenEndpoint.postAccessToken(null, params))
//        .flatMap(a -> ServerResponse.ok().bodyValue(a.getBody()));
//  }
}
