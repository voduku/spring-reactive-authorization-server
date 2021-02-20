package uaa.router;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/15/2021
 */
public class BaseRouter {

//  protected Mono<ServerWebExchange> getServerWebExchange() {
//    return Mono.deferContextual(Mono::just)
//        .map(ctx -> ctx.get(ServerWebExchange.class));
//  }
//
//  protected Mono<WebSession> getWebSession() {
//    return getServerWebExchange()
//        .flatMap(ServerWebExchange::getSession);
//  }
//
//  protected Mono<SecurityContext> getSecurityContext() {
//    return getWebSession()
//        .flatMap(session -> ReactiveSecurityContextHolder.getContext()
//            .switchIfEmpty(Mono.fromSupplier(() -> {
//              SecurityContext context = new SecurityContextImpl();
//              session.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, context);
//              return context;
//            })));
//  }

  protected Mono<Authentication> getCurrentAuthentication() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication);
  }
}
