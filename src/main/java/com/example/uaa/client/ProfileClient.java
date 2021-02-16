package com.example.uaa.client;

import com.example.uaa.model.dto.ProfileDTO;
import com.example.uaa.model.request.AuthUserRequest;
import com.example.uaa.model.response.AuthUserResponse;
import com.example.uaa.model.response.RestResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 1/4/2021
 */
@Slf4j
@Component
public class ProfileClient {

  private final WebClient webClient;

  public ProfileClient(WebClient.Builder builder) {
    this.webClient = builder.baseUrl("http://profile/profile").build();
  }

  public Mono<AuthUserResponse> loadAuthUserProfile(AuthUserRequest request) {
    return webClient.post().uri("/users/auth")
        .body(Mono.just(request), AuthUserRequest.class)
        .retrieve().bodyToMono(RestResult.class)
        .doOnNext(body -> log.info(body.toString()))
        .map(body -> new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(body.getData(), AuthUserResponse.class));
  }

  public Mono<ProfileDTO> getProfile(Long profileId) {
    return webClient.get().uri(uriBuilder -> uriBuilder.path("/profiles/{profileId}").build(profileId))
        .retrieve().bodyToMono(RestResult.class)
        .map(RestResult<ProfileDTO>::getData);
  }
}
