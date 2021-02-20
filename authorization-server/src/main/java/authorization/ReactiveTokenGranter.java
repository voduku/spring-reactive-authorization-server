package authorization;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.TokenRequest;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/11/2021
 */
public interface ReactiveTokenGranter {

  Mono<OAuth2AccessToken> grant(String grantType, TokenRequest tokenRequest);
}
