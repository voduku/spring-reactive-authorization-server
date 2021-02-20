package authorization.client;

import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import reactor.core.publisher.Mono;

/**
 * A service that provides the details about an OAuth2 client.
 *
 * @author VuDo
 * @since 2/11/2021
 */
public interface ReactiveClientDetailsService {

  /**
   * Load a client by the client id. This method must not return null.
   *
   * @param clientId The client id.
   * @return Mono of the client details (never null).
   * @throws ClientRegistrationException If the client account is locked, expired, disabled, or invalid for any other reason.
   */
  Mono<ClientDetails> loadClientByClientId(String clientId) throws ClientRegistrationException;
}
