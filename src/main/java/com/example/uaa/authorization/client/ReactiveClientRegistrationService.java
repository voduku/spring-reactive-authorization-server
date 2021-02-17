package com.example.uaa.authorization.client;

import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public interface ReactiveClientRegistrationService {

  Mono<Void> addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException;

  Mono<Void> updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException;

  Mono<Void> updateClientSecret(String clientId, String secret) throws NoSuchClientException;

  Mono<Void> removeClientDetails(String clientId) throws NoSuchClientException;

  Flux<ClientDetails> listClientDetails();
}
