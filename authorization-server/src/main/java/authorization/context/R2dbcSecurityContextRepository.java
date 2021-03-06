package authorization.context;

import authorization.client.ReactiveClientDetailsService;
import authorization.token.ReactiveTokenStore;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 3/6/2021
 */
public class R2dbcSecurityContextRepository implements ServerSecurityContextRepository {

  private final ReactiveTokenStore tokenStore;
  private final ReactiveClientDetailsService clientDetailsService;
  private final String GRANT_TYPE = "grant_type";
  private final String CLIENT_CREDENTIALS = "client_credentials";
  private final String CLIENT_ID = "client_id";
  private final String CLIENT_SECRET = "client_secret";

  public R2dbcSecurityContextRepository(ReactiveTokenStore tokenStore, ReactiveClientDetailsService clientDetailsService) {
    this.tokenStore = tokenStore;
    this.clientDetailsService = clientDetailsService;
  }

  @Override
  public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
    return Mono.empty();
  }

  @Override
  public Mono<SecurityContext> load(ServerWebExchange exchange) {
    String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (token != null) {
      return Mono.just(token)
          .map(tk -> tk.replace("Bearer ", ""))
          .flatMap(tokenStore::readAuthentication)
          .map(SecurityContextImpl::new);
    }
    return checkClientCredentials(exchange);
  }

  private Mono<SecurityContext> checkClientCredentials(ServerWebExchange exchange) {
    return exchange.getFormData()
        .flatMap(this::validateClientCredentials);
  }

  private Mono<SecurityContext> validateClientCredentials(MultiValueMap<String, String> data) {
    if (data.containsKey(GRANT_TYPE) && data.containsKey(CLIENT_ID) && data.containsKey(CLIENT_SECRET)) {
      String grantType = data.getFirst(GRANT_TYPE);
      String clientId = data.getFirst(CLIENT_ID);
      String clientSecret = data.getFirst(CLIENT_SECRET);
      if (grantType != null && grantType.equalsIgnoreCase(CLIENT_CREDENTIALS) && clientId != null && clientSecret != null) {
        return clientDetailsService.loadClientByClientId(clientId)
            .doOnNext(clientDetails -> {
              if (!Objects.equals(clientDetails.getClientSecret(), clientSecret)) {
                throw new BadCredentialsException("wrong credentials");
              }
            })
            .map(clientDetails -> new SecurityContextImpl(new PreAuthenticatedAuthenticationToken(null, null, clientDetails.getAuthorities())));
      }
    }
    return Mono.empty();
  }
}

class EmptyPrincipal implements Principal {

  @Override
  public String getName() {
    return null;
  }
}