package authorization.endpoint;

import authorization.token.ReactiveResourceServerTokenServices;
import java.util.Map;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 3/8/2021
 */
@FrameworkEndpoint
public class ReactiveCheckTokenEndpoint {

  private static final String TOKEN_PARAM = "token";
  protected final Log logger = LogFactory.getLog(getClass());
  private final ReactiveResourceServerTokenServices resourceServerTokenServices;
  private AccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
  private WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator = new WebfluxResponseExceptionTranslator();

  public ReactiveCheckTokenEndpoint(ReactiveResourceServerTokenServices resourceServerTokenServices) {
    this.resourceServerTokenServices = resourceServerTokenServices;
  }

  /**
   * @param exceptionTranslator the exception translator to set
   */
  public void setExceptionTranslator(WebResponseExceptionTranslator<OAuth2Exception> exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * @param accessTokenConverter the accessTokenConverter to set
   */
  public void setAccessTokenConverter(AccessTokenConverter accessTokenConverter) {
    this.accessTokenConverter = accessTokenConverter;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public Mono<ServerResponse> checkToken(ServerRequest request) {
    String token = request.queryParam(TOKEN_PARAM).orElseThrow(() -> new InvalidTokenException("Token was not recognised"));
    return resourceServerTokenServices.readAccessToken(token)
        .switchIfEmpty(Mono.error(new InvalidTokenException("Token was not recognised")))
        .doOnNext(tk -> {
          if (tk.isExpired()) {
            throw new InvalidTokenException("Token has expired");
          }
        })
        .map(Container::new)
        .flatMap(this::loadAuthentication)
        .map(container -> (Map<String, Object>) accessTokenConverter.convertAccessToken(container.accessToken, container.authentication))
        .doOnNext(response -> response.put("active", true))
        .flatMap(this::getResponse)
        .onErrorResume(InvalidTokenException.class, this::handleException);
  }

  private Mono<Container> loadAuthentication(Container container) {
    return Mono.just(container)
        .map(Container::getAccessToken)
        .map(OAuth2AccessToken::getValue)
        .flatMap(resourceServerTokenServices::loadAuthentication)
        .map(container::setAuthentication);
  }

  private Mono<ServerResponse> getResponse(Map<String, ?> response) {
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .bodyValue(response);
  }

  private <T> Mono<ServerResponse> renderErrorResponseEntity(ResponseEntity<T> entity) {
    ServerResponse.BodyBuilder response = ServerResponse.status(entity.getStatusCodeValue())
        .contentType(MediaType.APPLICATION_JSON);
    return entity.getBody() == null ? response.build() : response.bodyValue(entity.getBody());
  }

  @SneakyThrows
  public Mono<ServerResponse> handleException(Exception e) {
    logger.info("Handling error: " + e.getClass().getSimpleName() + ", " + e.getMessage());
    // This isn't an oauth resource, so we don't want to send an
    // unauthorized code here. The client has already authenticated
    // successfully with basic auth and should just
    // get back the invalid token error.
    InvalidTokenException e400 = new InvalidTokenException(e.getMessage()) {
      @Override
      public int getHttpErrorCode() {
        return 400;
      }
    };
    return renderErrorResponseEntity(exceptionTranslator.translate(e400));
  }

  @Data
  private static class Container {

    private OAuth2AccessToken accessToken;
    private OAuth2Authentication authentication;

    public Container(OAuth2AccessToken accessToken) {
      this.accessToken = accessToken;
    }

    public Container setAuthentication(OAuth2Authentication authentication) {
      this.authentication = authentication;
      return this;
    }
  }
}
