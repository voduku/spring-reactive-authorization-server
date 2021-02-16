package com.example.uaa.router;

import static org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
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
