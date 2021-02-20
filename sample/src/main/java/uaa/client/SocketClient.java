package uaa.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uaa.model.request.AuthUserRequest;
import uaa.model.request.SocketActionRequest;

/**
 * @author VuDo
 * @since 2/12/2021
 */
@Configuration(proxyBeanMethods = false)
public class SocketClient {

  private final WebClient webClient;

  public SocketClient(WebClient.Builder builder) {
    this.webClient = builder.baseUrl("http://socket/socket").build();
  }

  public void sendUserNotification(@RequestBody SocketActionRequest request) {
    webClient.post().uri("/action/user-notification")
        .body(Mono.just(request), AuthUserRequest.class)
        .retrieve();
  }
}