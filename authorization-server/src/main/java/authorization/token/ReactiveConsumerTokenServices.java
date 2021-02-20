package authorization.token;

import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public interface ReactiveConsumerTokenServices {

  Mono<Boolean> revokeToken(String tokenValue);

}
