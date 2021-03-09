package authorization.token;

import authorization.client.ReactiveClientDetailsService;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@Setter
public class ReactiveTokenServices implements ReactiveAuthorizationServerTokenServices, ReactiveResourceServerTokenServices, ReactiveConsumerTokenServices,
    InitializingBean {

  private int refreshTokenValiditySeconds = 60 * 60 * 24 * 30; // default 30 days.

  private int accessTokenValiditySeconds = 60 * 60 * 12; // default 12 hours.

  private boolean supportRefreshToken = false;

  private boolean reuseRefreshToken = true;

  private ReactiveTokenStore tokenStore;

  private ReactiveClientDetailsService clientDetailsService;

  private ReactiveAuthenticationManager authenticationManager;

  //  @Transactional
  public Mono<OAuth2AccessToken> createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
    return Mono.just(new Container(authentication))
        .flatMap(this::getExistingAccessToken)
        .flatMap(this::createAccessTokenIfNotExist);
  }

  @Transactional(noRollbackFor = {InvalidTokenException.class, InvalidGrantException.class})
  public Mono<OAuth2AccessToken> refreshAccessToken(String refreshToken, TokenRequest tokenRequest) throws AuthenticationException {
    return null;
  }

  private Mono<Container> getExistingAccessToken(Container container) {
    return tokenStore.getAccessToken(container.authentication)
        .map(existingAccessToken -> {
          if (existingAccessToken.isExpired()) {
            if (existingAccessToken.getRefreshToken() != null) {
              container.refreshToken = existingAccessToken.getRefreshToken();
              // The token store could remove the refresh token when the
              // access token is removed, but we want to
              // be sure...
              tokenStore.removeRefreshToken(container.refreshToken);
            }
            tokenStore.removeAccessToken(existingAccessToken);
          } else {
            // Re-store the access token in case the authentication has changed
            tokenStore.storeAccessToken(existingAccessToken, container.authentication);
            container.accessToken = existingAccessToken;
          }
          return container;
        })
        .switchIfEmpty(Mono.just(container));
  }

  private Mono<OAuth2AccessToken> createAccessTokenIfNotExist(Container container) {
    if (container.accessToken != null) {
      return Mono.just(container.accessToken);
    }
    return Mono.just(container)
        .flatMap(this::getRefreshToken)
        .map(this::createAccessToken)
        .doOnNext(ctn -> tokenStore.storeAccessToken(ctn.accessToken, ctn.authentication).subscribe())
        .doOnNext(ctn -> tokenStore.storeRefreshToken(ctn.refreshToken, ctn.authentication).subscribe())
        .map(Container::getAccessToken);
  }

  private Mono<Container> getRefreshToken(Container container) {
    // Only create a new refresh token if there wasn't an existing one
    // associated with an expired access token.
    // Clients might be holding existing refresh tokens, so we re-use it in
    // the case that the old access token
    // expired.
    if (container.refreshToken == null) {
      return createRefreshToken(container);
    }
    // But the refresh token itself might need to be re-issued if it has expired.
    else if (container.refreshToken instanceof ExpiringOAuth2RefreshToken) {
      ExpiringOAuth2RefreshToken expiring = (ExpiringOAuth2RefreshToken) container.refreshToken;
      if (System.currentTimeMillis() > expiring.getExpiration().getTime()) {
        return createRefreshToken(container);
      }
    }
    return Mono.just(container);
  }

  public Mono<OAuth2AccessToken> getAccessToken(OAuth2Authentication authentication) {
    return tokenStore.getAccessToken(authentication);
  }

  public Mono<Boolean> revokeToken(String tokenValue) {
    return tokenStore.readAccessToken(tokenValue)
        .doOnNext(accessToken -> {
          if (accessToken.getRefreshToken() != null) {
            tokenStore.removeRefreshToken(accessToken.getRefreshToken());
          }
          tokenStore.removeAccessToken(accessToken);
        })
        .map(accessToken -> true)
        .switchIfEmpty(Mono.just(false));
  }

  public Mono<OAuth2Authentication> loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
    return Mono.just(accessToken)
        .flatMap(tokenStore::readAccessToken)
        .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid access token: " + accessToken)))
        .doOnNext(token -> {
          if (token.isExpired()) {
            tokenStore.removeAccessToken(token);
            throw new InvalidTokenException("Access token expired: " + accessToken);
          }
        }).flatMap(tokenStore::readAuthentication)
        .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid access token: " + accessToken)))
        .doOnNext(authentication -> {
          if (clientDetailsService != null) {
            String clientId = authentication.getOAuth2Request().getClientId();
            try {
              clientDetailsService.loadClientByClientId(clientId);
            } catch (ClientRegistrationException e) {
              throw new InvalidTokenException("Client not valid: " + clientId, e);
            }
          }
        });
  }

  public Mono<OAuth2AccessToken> readAccessToken(String accessToken) {
    return tokenStore.readAccessToken(accessToken);
  }

  public void afterPropertiesSet() {
    Assert.notNull(tokenStore, "tokenStore must be set");
  }

  /**
   * The access token validity period in seconds
   *
   * @param container with clientAuth the current authorization request
   * @return the access token validity period in seconds
   */
  protected int getAccessTokenValiditySeconds(Container container) {
    return container.clientDetails != null ? container.clientDetails.getAccessTokenValiditySeconds() : accessTokenValiditySeconds;

  }

  protected void isSupportRefreshToken(Container container) {
    ClientDetails clientDetails = container.clientDetails;
    container.supportRefreshToken = clientDetails.getAuthorizedGrantTypes().contains("refresh_token");
  }

  /**
   * The refresh token validity period in seconds
   */
  protected void getRefreshTokenValiditySeconds(Container container) {
    container.refreshTokenValiditySeconds = container.clientDetails != null ?
        container.clientDetails.getRefreshTokenValiditySeconds() : refreshTokenValiditySeconds;
  }


  private Container createAccessToken(Container container) {
    DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(UUID.randomUUID().toString());
    int validitySeconds = getAccessTokenValiditySeconds(container);
    if (validitySeconds > 0) {
      token.setExpiration(new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
    }
    token.setRefreshToken(container.refreshToken);
    token.setScope(container.authentication.getOAuth2Request().getScope());
    container.setAccessToken(token);
    return container;
  }

  private Mono<Container> createRefreshToken(Container container) {
    if (clientDetailsService == null || !supportRefreshToken) {
      return Mono.just(container);
    }
    return clientDetailsService.loadClientByClientId(container.authentication.getOAuth2Request().getClientId())
        .map(container::setClientDetails)
        .doOnNext(this::isSupportRefreshToken)
        .doOnNext(this::getRefreshTokenValiditySeconds)
        .map(Container::createRefreshToken);
  }

  @Setter
  @Getter
  private static class Container {

    private ClientDetails clientDetails;
    private boolean supportRefreshToken;
    private int refreshTokenValiditySeconds;
    private int accessTokenValiditySeconds;
    private String refreshTokenValue;
    private OAuth2RefreshToken refreshToken;
    private OAuth2AccessToken accessToken;
    private OAuth2Authentication authentication;

    public Container(OAuth2Authentication authentication) {
      this.authentication = authentication;
      this.refreshTokenValue = UUID.randomUUID().toString();
    }

    public Container setClientDetails(ClientDetails clientDetails) {
      this.clientDetails = clientDetails;
      return this;
    }

    public Container createRefreshToken() {
      refreshToken = refreshTokenValiditySeconds > 0 ?
          new DefaultExpiringOAuth2RefreshToken(refreshTokenValue, new Date(System.currentTimeMillis() + (refreshTokenValiditySeconds * 1000L)))
          : new DefaultOAuth2RefreshToken(refreshTokenValue);
      return this;
    }
  }

}
