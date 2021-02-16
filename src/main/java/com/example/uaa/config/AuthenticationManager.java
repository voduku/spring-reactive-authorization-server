package com.example.uaa.config;

import com.example.uaa.client.ProfileClient;
import com.example.uaa.common.ResourceBundle;
import com.example.uaa.model.enumerator.AuthenticationType;
import com.example.uaa.model.oauth.Auth;
import com.example.uaa.model.oauth.AuthRequest;
import com.example.uaa.model.oauth.UserDetails;
import com.example.uaa.model.request.AuthUserRequest;
import com.example.uaa.model.response.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AuthenticationManager implements ReactiveAuthenticationManager {

  private final ProfileClient profileClient;

  private final ResourceBundle resourceBundle;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) throws AuthenticationException {
    Auth auth = (Auth) authentication;
    AuthRequest authRequest = auth.getCredentials();
    return loadUserProfile(auth.getCredentials())
        .doOnNext(response -> {
          if (response == null || response.getUser() == null) {
            throw new OAuth2Exception(resourceBundle.getMessage("err.invalid.user"));
          }
        })
        .doOnNext(response -> verifyAuthentication(auth.getCredentials(), response))
        .flatMap(response -> createPrincipal(authRequest.getTenantId(), response))
        .flatMap(principal -> {
          auth.setPrincipal(principal);
          auth.getAuthorities().addAll(principal.getAuthorities());
          auth.setAuthenticated(true);
          auth.eraseCredentials();
          return Mono.just(auth);
        });
  }

  // ===== PRIVATE METHODS =======

  private Mono<AuthUserResponse> loadUserProfile(AuthRequest auth2Request) {
    return Mono.just(new AuthUserRequest(auth2Request))
        .flatMap(a -> setGrantType(a, auth2Request))
        .doOnNext(a -> log.info("userName= {}, tenantId ={}, client={} login", a.getUsername(), a.getTenantId(), a.getClient()))
        .flatMap(profileClient::loadAuthUserProfile);
  }

  private Mono<AuthUserRequest> setGrantType(AuthUserRequest userRequest, AuthRequest auth2Request) {
    return Mono.just(auth2Request.getGrantType()).map(AuthenticationType::authenticationType)
        .doOnNext(authenticationType -> {
          switch (authenticationType) {
            case INTERNAL:
              userRequest.setGrantType(authenticationType);
            case ANONYMOUS:
              userRequest.setGrantType(authenticationType);
              userRequest.setAnonymousId(auth2Request.getAnonymousId());
            case FACEBOOK:
            case GOOGLE:
              userRequest.setGrantType(authenticationType);
              userRequest.setToken(auth2Request.getToken());
          }
        }).map(type -> userRequest);
  }

  private void verifyAuthentication(AuthRequest credentials, AuthUserResponse response) {
    AuthenticationType authenticationType = AuthenticationType.authenticationType(credentials.getGrantType());
    switch (authenticationType) {
      case FACEBOOK:
      case GOOGLE:
      case ANONYMOUS:
        checkProfileIsBanned(response);
        break;
      case INTERNAL:
        break;
      case PASSWORD:
        if (!StringUtils.hasText(credentials.getPassword())) {
          throw new OAuth2Exception(resourceBundle.getMessage("err.require.param", "password"));
        }

        String md5InputPassword = encryptPassword(credentials.getPassword());
        if (!md5InputPassword.equals(response.getUser().getPassword())) {
//          ifThrow(response.getUser().getAttemptCount() > 2, resourceBundle.getMessage("err.incorrect.password"), 101);
          throw new BadCredentialsException(resourceBundle.getMessage("err.incorrect.password"));
        }
        checkProfileIsBanned(response);
        break;
      default:
        throw new InvalidGrantException(resourceBundle.getMessage("err.invalid.param", "grantType" + credentials.getGrantType()));
    }
  }

  private String encryptPassword(String password) {
    return DigestUtils.md5DigestAsHex(password.getBytes());
  }

  private void checkProfileIsBanned(AuthUserResponse response) {
    Flux.fromIterable(response.getProfiles())
        .subscribe(p -> {
          if (p.getIsBanned() != null && p.getIsBanned()) {
            throw new BadCredentialsException(resourceBundle.getMessage("err.profile.is-banned", p.getBannedNote()));
          }
        });
  }

  private Mono<UserDetails> createPrincipal(String tenantId, AuthUserResponse auth) {
    return Flux.fromIterable(auth.getProfiles())
        .filter(p -> p.getDefaultProfile() != null && p.getDefaultProfile() && p.getTenantId().equals(tenantId))
        .next()
        .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("User information not found")))
        .map(UserDetails::new);
  }
}
