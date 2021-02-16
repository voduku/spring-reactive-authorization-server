package com.example.uaa.oauth.endpoint;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.example.uaa.oauth.ReactiveTokenGranter;
import com.example.uaa.oauth.client.ReactiveClientDetailsService;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.oauth2.sdk.GrantType;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import lombok.Data;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@ReactiveFrameworkEndpoint
public class ReactiveTokenEndpoint extends AbstractReactiveEndpoint {

  private final OAuth2RequestValidator oAuth2RequestValidator = new DefaultOAuth2RequestValidator();

  public ReactiveTokenEndpoint(ReactiveClientDetailsService clientDetailsService, ReactiveTokenGranter tokenGranter) {
    super.setClientDetailsService(clientDetailsService);
    super.setTokenGranter(tokenGranter);
  }

//  @PostMapping("/oauth/token")
//  public Mono<ResponseEntity<OAuth2AccessToken>> postAccessToken(Principal principal, @RequestParam
//      Map<String, String> parameters) {
//    return Mono.just(new RequestContainer(body, request.attributes()))
//        .doOnNext(this::getClientId)
//        .flatMap(this::getClientDetails)
//        .flatMap(this::createTokenRequest)
//        .doOnNext(this::validateClientId)
//        .doOnNext(this::validateScope)
//        .doOnNext(this::validateTokenRequest)
//        .flatMap(this::grant)
//        .doOnNext(this::validateToken)
//        .flatMap(this::getResponse);
//  }

  @Bean
  public RouterFunction<ServerResponse> tokenRoute() {
    return route().POST("/oauth/token", this::postAccessToken).build();
  }


  public Mono<ServerResponse> postAccessToken(@NonNull ServerRequest request) {
    return request.formData()
        .map(MultiValueMap::toSingleValueMap)
        .flatMap(formData -> postAccessToken(null, formData));
  }


  public Mono<ServerResponse> postAccessToken(Principal principal, Map<String, String> parameters) {
    return Mono.just(new RequestContainer(principal, parameters))
        .doOnNext(this::getClientId)
        .flatMap(this::getClientDetails)
        .flatMap(this::createTokenRequest)
        .doOnNext(this::validateClientId)
        .doOnNext(this::validateScope)
        .doOnNext(this::validateTokenRequest)
        .flatMap(this::grant)
        .doOnNext(this::validateToken)
        .flatMap(this::getResponse);
  }

  protected void getClientId(RequestContainer container) {
    if (container.isClientCredentials) {
      return;
    }
    Authentication client = (Authentication) container.principal;
    if (!client.isAuthenticated()) {
      throw new InsufficientAuthenticationException("The client is not authenticated.");
    }
    String clientId = client.getName();
    if (client instanceof OAuth2Authentication) {
      // Might be a client and user combined authentication
      clientId = ((OAuth2Authentication) client).getOAuth2Request().getClientId();
    }
    container.clientId = clientId;
  }

  protected Mono<RequestContainer> getClientDetails(RequestContainer container) {
    return getClientDetailsService().loadClientByClientId(container.clientId)
        .map(container::setClientDetails);
  }

  protected Mono<RequestContainer> createTokenRequest(RequestContainer container) {
    return getOAuth2RequestFactory().createTokenRequest(container.parameters, container.clientDetails)
        .map(container::setTokenRequest);
  }

  private void validateClientId(RequestContainer container) {
    String clientId = container.clientId;
    String tokenRequestClientId = container.tokenRequest.getClientId();
    if (clientId != null && !clientId.equals("")) {
      // Only validate the client details if a client authenticated during this
      // request.
      if (!clientId.equals(tokenRequestClientId)) {
        // double check to make sure that the client ID in the token request is the same as that in the
        // authenticated client
        throw new InvalidClientException("Given client ID does not match authenticated client");
      }
    }
  }

  private void validateScope(RequestContainer container) {
    ClientDetails authenticatedClient = container.clientDetails;
    TokenRequest tokenRequest = container.tokenRequest;
    if (authenticatedClient != null) {
      oAuth2RequestValidator.validateScope(tokenRequest, authenticatedClient);
    }
  }

  private void validateTokenRequest(RequestContainer container) {
    TokenRequest tokenRequest = container.tokenRequest;
    Map<String, String> parameters = container.parameters;
    if (!StringUtils.hasText(tokenRequest.getGrantType())) {
      throw new InvalidRequestException("Missing grant type");
    }
    if (tokenRequest.getGrantType().equals("implicit")) {
      throw new InvalidGrantException("Implicit grant type not supported from token endpoint");
    }
    if (isAuthCodeRequest(parameters)) {
      // The scope was requested or determined during the authorization step
      if (!tokenRequest.getScope().isEmpty()) {
        logger.debug("Clearing scope of incoming token request");
        tokenRequest.setScope(Collections.emptySet());
      }
    }
    if (isRefreshTokenRequest(parameters)) {
      // A refresh token has its own default scopes, so we should ignore any added by the factory here.
      tokenRequest.setScope(OAuth2Utils.parseParameterList(parameters.get(OAuth2Utils.SCOPE)));
    }
  }

  private Mono<OAuth2AccessToken> grant(RequestContainer container) {
    return getTokenGranter().grant(container.tokenRequest.getGrantType(), container.tokenRequest);
  }

  private void validateToken(OAuth2AccessToken token) {
    if (token == null) {
      throw new UnsupportedGrantTypeException("Unsupported grant type");
    }
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<OAuth2Exception>> handleException(Exception e) throws Exception {
    if (logger.isErrorEnabled()) {
      logger.error("Handling error: " + e.getClass().getSimpleName() + ", " + e.getMessage(), e);
    }
    return Mono.just(getExceptionTranslator().translate(e));
  }

  @ExceptionHandler(ClientRegistrationException.class)
  public Mono<ResponseEntity<OAuth2Exception>> handleClientRegistrationException(Exception e) throws Exception {
    if (logger.isWarnEnabled()) {
      logger.warn("Handling error: " + e.getClass().getSimpleName() + ", " + e.getMessage());
    }
    return Mono.just(getExceptionTranslator().translate(new BadClientCredentialsException()));
  }

  @ExceptionHandler(OAuth2Exception.class)
  public Mono<ResponseEntity<OAuth2Exception>> handleException(OAuth2Exception e) throws Exception {
    if (logger.isWarnEnabled()) {
      logger.warn("Handling error: " + e.getClass().getSimpleName() + ", " + e.getMessage());
    }
    return Mono.just(getExceptionTranslator().translate(e));
  }

//  private Mono<ServerResponse> getResponse(OAuth2AccessToken accessToken) {
//    return ServerResponse.ok()
//        .contentType(MediaType.APPLICATION_JSON)
//        .header("Cache-Control", "no-store")
//        .header("Pragma", "no-cache")
//        .bodyValue(accessToken);
//  }

  private Mono<ServerResponse> getResponse(OAuth2AccessToken accessToken) {
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .bodyValue(accessToken);
  }

  private boolean isRefreshTokenRequest(Map<String, String> parameters) {
    return "refresh_token".equals(parameters.get("grant_type")) && parameters.get("refresh_token") != null;
  }

  private boolean isAuthCodeRequest(Map<String, String> parameters) {
    return "authorization_code".equals(parameters.get("grant_type")) && parameters.get("code") != null;
  }

  //grant_type=client_credentials&client_id=auth-service&client_secret=auth-service-client-secret
  @Data
  private static class RequestContainer {

    private Principal principal;
    private Map<String, String> parameters;
    private String clientId;
    private ClientDetails clientDetails;
    private TokenRequest tokenRequest;
    private boolean isClientCredentials = false;

    @SuppressWarnings("unchecked")
    public RequestContainer(String body, Map<String, Object> attributes) {
      if (!StringUtils.hasLength(body)) {
        principal = (Authentication) attributes.get("principal");
        parameters = (Map<String, String>) attributes.get("parameters");
      } else {
        String[] params = body.split("&");
        clientId = extractParamValue(params[1]);
        isClientCredentials = true;
        parameters = ImmutableMap.of(
            "grant_type", extractParamValue(params[0]),
            "client_id", clientId,
            "client_secret", extractParamValue(params[1])
        );
      }
    }

    public RequestContainer(Principal principal, Map<String, String> parameters) {
      this.principal = principal;
      this.parameters = parameters;
      if (parameters.containsKey("grant_type") && parameters.get("grant_type").equals(GrantType.CLIENT_CREDENTIALS.getValue())) {
        clientId = parameters.get("client_id");
        isClientCredentials = true;
      }
    }

    public RequestContainer setClientDetails(ClientDetails clientDetails) {
      this.clientDetails = clientDetails;
      return this;
    }

    public RequestContainer setTokenRequest(TokenRequest tokenRequest) {
      this.tokenRequest = tokenRequest;
      return this;
    }

    private String extractParamValue(String param) {
      return param.split("=")[1];
    }
  }
}