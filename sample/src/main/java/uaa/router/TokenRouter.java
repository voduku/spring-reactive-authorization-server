package uaa.router;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TokenRouter {

//  private final ReactiveTokenEndpoint tokenEndpoint;

//  @Bean
//  public RouterFunction<ServerResponse> tokenRoute() {
//    return route().POST("/oauth/authorization.token", this::postAccessToken).build();
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
