package authorization.impl;

import static io.r2dbc.postgresql.util.PredicateUtils.not;

import authorization.ReactiveOAuth2RequestFactory;
import authorization.ReactiveSecurityContextAccessor;
import authorization.client.ReactiveClientDetailsService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public class DefaultReactiveOAuth2RequestFactory implements ReactiveOAuth2RequestFactory {

  private final ReactiveClientDetailsService clientDetailsService;

  private ReactiveSecurityContextAccessor securityContextAccessor = new DefaultReactiveSecurityContextAccessor();

  private boolean checkUserScopes = false;

  public DefaultReactiveOAuth2RequestFactory(ReactiveClientDetailsService clientDetailsService) {
    this.clientDetailsService = clientDetailsService;
  }

  /**
   * @param securityContextAccessor the security context accessor to set
   */
  public void setSecurityContextAccessor(ReactiveSecurityContextAccessor securityContextAccessor) {
    this.securityContextAccessor = securityContextAccessor;
  }

  /**
   * Flag to indicate that scopes should be interpreted as valid authorities. No scopes will be granted to a user unless they are permitted as a granted
   * authority to that user.
   *
   * @param checkUserScopes the checkUserScopes to set (default false)
   */
  public void setCheckUserScopes(boolean checkUserScopes) {
    this.checkUserScopes = checkUserScopes;
  }

  public Mono<AuthorizationRequest> createAuthorizationRequest(Map<String, String> authorizationParameters) {

    String clientId = authorizationParameters.get(OAuth2Utils.CLIENT_ID);
    String state = authorizationParameters.get(OAuth2Utils.STATE);
    String redirectUri = authorizationParameters.get(OAuth2Utils.REDIRECT_URI);
    Set<String> responseTypes = OAuth2Utils.parseParameterList(authorizationParameters.get(OAuth2Utils.RESPONSE_TYPE));

    return extractScopes(authorizationParameters, clientId)
        .map(scopes -> new AuthorizationRequest(authorizationParameters, Collections.emptyMap(), clientId, scopes,
            null, null, false, state, redirectUri, responseTypes))
        .zipWith(clientDetailsService.loadClientByClientId(clientId))
        .map(tuple -> {
          AuthorizationRequest request = tuple.getT1();
          request.setResourceIdsAndAuthoritiesFromClientDetails(tuple.getT2());
          return request;
        });

  }

  public Mono<OAuth2Request> createOAuth2Request(AuthorizationRequest request) {
    return Mono.fromSupplier(request::createOAuth2Request);
  }

  public Mono<TokenRequest> createTokenRequest(Map<String, String> requestParameters, ClientDetails authenticatedClient) {

    String clientId = requestParameters.get(OAuth2Utils.CLIENT_ID);
    if (clientId == null) {
      // if the clientId wasn't passed in in the map, we add pull it from the authenticated client object
      clientId = authenticatedClient.getClientId();
    } else {
      // otherwise, make sure that they match
      if (!clientId.equals(authenticatedClient.getClientId())) {
        throw new InvalidClientException("Given client ID does not match authenticated client");
      }
    }
    String grantType = requestParameters.get(OAuth2Utils.GRANT_TYPE);

    String finalClientId = clientId;
    return extractScopes(requestParameters, clientId)
        .map(scopes -> new TokenRequest(requestParameters, finalClientId, scopes, grantType))
        .switchIfEmpty(Mono.just(new TokenRequest(requestParameters, finalClientId, null, grantType)));
  }

  public Mono<TokenRequest> createTokenRequest(AuthorizationRequest authorizationRequest, String grantType) {
    return Mono.fromSupplier(() -> new TokenRequest(authorizationRequest.getRequestParameters(), authorizationRequest.getClientId(),
        authorizationRequest.getScope(), grantType));
  }

  public Mono<OAuth2Request> createOAuth2Request(ClientDetails client, TokenRequest tokenRequest) {
    return Mono.fromSupplier(() -> tokenRequest.createOAuth2Request(client));
  }


  private Mono<Set<String>> extractScopes(Map<String, String> requestParameters, String clientId) {
    return Mono.just(OAuth2Utils.parseParameterList(requestParameters.get(OAuth2Utils.SCOPE)))
        .filter(not(CollectionUtils::isEmpty))
        .switchIfEmpty(getScopesFromClient(clientId))
        .flatMap(scopes -> checkUserScopes ? checkUserScopes(scopes) : Mono.just(scopes));
  }

  private Mono<Set<String>> getScopesFromClient(String clientId) {
    return clientDetailsService.loadClientByClientId(clientId)
        .map(ClientDetails::getScope);
  }

  private Mono<Set<String>> checkUserScopes(Set<String> scopes) {
    return securityContextAccessor.isUser()
        .flatMap(isUser -> isUser ? Mono.empty() : Mono.just(scopes))
        .switchIfEmpty(getNonUserScopes(scopes));
  }

  private Mono<Set<String>> getNonUserScopes(Set<String> scopes) {
    return securityContextAccessor.getAuthorities()
        .map(authority -> scopes.stream().map(String::toUpperCase)
            .filter(scope -> Arrays.asList(scope, "ROLE_" + scope.toUpperCase()).contains(authority.getAuthority()))
            .findFirst().orElse(null)
        ).filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
