package uaa.config;

import authorization.ReactiveTokenGranter;
import authorization.client.ReactiveClientDetailsService;
import authorization.token.ReactiveAuthorizationServerTokenServices;
import authorization.token.ReactiveTokenStore;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uaa.client.SocketClient;
import uaa.common.ResourceBundle;
import uaa.model.enumerator.AuthenticationType;
import uaa.model.enumerator.Role;
import uaa.model.oauth.Auth;
import uaa.model.oauth.UserDetails;
import uaa.model.request.SocketActionRequest;

/**
 * @author VuDo
 * @since 2/11/2021
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenGranter implements ReactiveTokenGranter {

  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private final ReactiveAuthenticationManager authenticationManager;
  private final ReactiveClientDetailsService clientDetailsService;
  private final ReactiveAuthorizationServerTokenServices tokenServices;
  private final ResourceBundle resourceBundle;
  private final ReactiveTokenStore tokenStore;
  private final SocketClient socketClient;
  @Value("${setting.enable-single-session}")
  private Boolean enableSingleSession;

  @Override
  @Transactional
  public Mono<OAuth2AccessToken> grant(String grantType, TokenRequest tokenRequest) {
    return clientDetailsService.loadClientByClientId(tokenRequest.getClientId())
        .map(client -> new GrantContainer(grantType, tokenRequest, client))
        .doOnNext(this::validateTokenRequest)
        .flatMap(this::createAuthentication)
        .doOnNext(this::validateAdditionInformation)
        .map(GrantContainer::getOAuth2Authentication)
        .doOnNext(auth -> auth.setAuthenticated(true))
        .doOnNext(OAuth2Authentication::eraseCredentials)
        .flatMap(tokenServices::createAccessToken);
  }

  private void validateAdditionInformation(GrantContainer container) {
    if (container.getAdditionalInformation().get("profileId") != null) {
      container.getPrincipal().setTenantProfileId(container.getClientProfileId());
    }
  }

  private Mono<GrantContainer> createAuthentication(GrantContainer container) {
    return Mono.just(container)
        .flatMap(this::authenticate)
        .map(container::setOAuth2Authentication)
        .doOnNext(o -> cleanOldAccessToken(container))
        .subscribeOn(Schedulers.parallel());
  }

  private Mono<OAuth2Authentication> authenticate(GrantContainer container) {
    String grantType = container.grantType;
    Auth authentication = container.authentication;
    OAuth2Request request = container.authRequest;
    if (grantType.equals(CLIENT_CREDENTIALS)) {
      authentication.setPrincipal(new UserDetails(authentication.getTenantId()));
      authentication.getAuthorities().add(new SimpleGrantedAuthority(Role.INTERNAL.name()));
      return Mono.just(new OAuth2Authentication(request, authentication));
    } else {
      return authenticationManager.authenticate(authentication).map(auth -> new OAuth2Authentication(request, auth));
    }
  }

  private void validateTokenRequest(GrantContainer container) {
    String grantType = container.getGrantType();
    Set<String> authorizedGrantTypes = container.getAuthorizedGrantTypes();
    if (authorizedGrantTypes != null && !authorizedGrantTypes.isEmpty() && !authorizedGrantTypes.contains(grantType)) {
      throw new InvalidClientException(resourceBundle.getMessage("err.invalid.param", "grantType=" + grantType));
    }
  }

  private void cleanOldAccessToken(GrantContainer container) {
    if (enableSingleSession == null || !enableSingleSession || AuthenticationType.toBeIgnored().contains(container.grantType)) {
      return;
    }
    String clientId = container.clientDetails.getClientId();
    Auth authentication = container.getAuthentication();
    Mono.just(authentication)
        .doOnNext(auth -> removeAccessToken(clientId, auth.getName()))
        .map(Authentication::getPrincipal)
        .cast(UserDetails.class)
        .map(UserDetails::getProfileId)
        .subscribe(this::sendForceLogoutMessageViaWs);
  }

  private void removeAccessToken(String clientId, String name) {
    tokenStore.findTokensByClientIdAndUserName(clientId, name)
        .doOnEach(token -> tokenStore.removeAccessToken(token.get()))
        .subscribe();
  }

  private void sendForceLogoutMessageViaWs(Long profileId) {
    Mono.just(profileId)
        .map(id -> SocketActionRequest.forceLogoutNotification(id, ImmutableMap.of("message", resourceBundle.getMessage("err.access.revoked"))))
        .doOnNext(socketClient::sendUserNotification)
        .doOnError(e -> log.error("can not send SINGLE_SESSION_FORCE_LOGOUT action for profileId: {}", profileId.toString()))
        .subscribe();
  }

  @Data
  private static class GrantContainer {

    String grantType;
    Auth authentication;
    OAuth2Request authRequest;
    ClientDetails clientDetails;
    OAuth2Authentication oAuth2Authentication;

    public GrantContainer(String grantType, TokenRequest tokenRequest, ClientDetails clientDetails) {
      this.grantType = grantType;
      this.clientDetails = clientDetails;
      this.authRequest = tokenRequest.createOAuth2Request(clientDetails);
      this.authentication = new Auth(tokenRequest.getRequestParameters(), getClientAuthorities(clientDetails));
    }

    public Set<String> getAuthorizedGrantTypes() {
      return clientDetails.getAuthorizedGrantTypes();
    }

    public Map<String, Object> getAdditionalInformation() {
      return clientDetails.getAdditionalInformation();
    }

    public GrantContainer setOAuth2Authentication(OAuth2Authentication oAuth2Authentication) {
      this.oAuth2Authentication = oAuth2Authentication;
      return this;
    }

    public UserDetails getPrincipal() {
      return (UserDetails) this.oAuth2Authentication.getPrincipal();
    }

    public Long getClientProfileId() {
      Object profileId = clientDetails.getAdditionalInformation().get("profileId");
      return profileId instanceof String ? Long.valueOf((String) profileId) : (Long) profileId;
    }

    private Set<GrantedAuthority> getClientAuthorities(ClientDetails client) {
      Set<GrantedAuthority> authorities = new HashSet<>(client.getAuthorities());
      if (!CollectionUtils.isEmpty(client.getScope())) {
        for (String scope : client.getScope()) {
          authorities.add(new SimpleGrantedAuthority(scope));
        }
      }
      return authorities;
    }
  }
}
