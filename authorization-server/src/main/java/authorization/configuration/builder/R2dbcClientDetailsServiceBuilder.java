package authorization.configuration.builder;

import authorization.client.R2dbcClientDetailsService;
import authorization.client.ReactiveClientDetailsService;
import io.r2dbc.spi.ConnectionFactory;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.util.Assert;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public class R2dbcClientDetailsServiceBuilder extends ReactiveClientDetailsServiceBuilder<R2dbcClientDetailsServiceBuilder> {

  private final Set<ClientDetails> clientDetails = new HashSet<>();

  private ConnectionFactory connectionFactory;

  private PasswordEncoder passwordEncoder; // for writing client secrets

  public R2dbcClientDetailsServiceBuilder connectionFactory(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
    return this;
  }

  public R2dbcClientDetailsServiceBuilder passwordEncoder(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    return this;
  }

  @Override
  protected void addClient(String clientId, ClientDetails value) {
    clientDetails.add(value);
  }

  @Override
  protected ReactiveClientDetailsService performBuild() {
    Assert.state(connectionFactory != null, "You need to provide a connection factory");
    R2dbcClientDetailsService clientDetailsService = new R2dbcClientDetailsService(connectionFactory);
    if (passwordEncoder != null) {
      // This is used to encode secrets as they are added to the database (if it isn't set then the user has top
      // pass in pre-encoded secrets)
      clientDetailsService.setPasswordEncoder(passwordEncoder);
    }
    for (ClientDetails client : clientDetails) {
      clientDetailsService.addClientDetails(client);
    }
    return clientDetailsService;
  }

}
