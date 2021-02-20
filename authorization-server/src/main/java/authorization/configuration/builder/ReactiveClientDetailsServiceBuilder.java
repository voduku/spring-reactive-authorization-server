package authorization.configuration.builder;

import authorization.client.ReactiveClientDetailsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

/**
 * @author VuDo
 * @since 2/13/2021
 */
public class ReactiveClientDetailsServiceBuilder<B extends ReactiveClientDetailsServiceBuilder<B>> extends
    SecurityConfigurerAdapter<ReactiveClientDetailsService, B> implements SecurityBuilder<ReactiveClientDetailsService> {

  private final List<ClientBuilder> clientBuilders = new ArrayList<>();

  public R2dbcClientDetailsServiceBuilder r2dbc() {
    return new R2dbcClientDetailsServiceBuilder();
  }

  @SuppressWarnings("rawtypes")
  public ReactiveClientDetailsServiceBuilder<?> clients(final ReactiveClientDetailsService clientDetailsService) {
    return new ReactiveClientDetailsServiceBuilder() {
      @Override
      public ReactiveClientDetailsService build() {
        return clientDetailsService;
      }
    };
  }

  public ClientBuilder withClient(String clientId) {
    ClientBuilder clientBuilder = new ClientBuilder(clientId);
    this.clientBuilders.add(clientBuilder);
    return clientBuilder;
  }

  @Override
  public ReactiveClientDetailsService build() {
    for (ClientBuilder clientDetailsBldr : clientBuilders) {
      addClient(clientDetailsBldr.clientId, clientDetailsBldr.build());
    }
    return performBuild();
  }

  protected void addClient(String clientId, ClientDetails build) {
  }

  protected ReactiveClientDetailsService performBuild() {
    throw new UnsupportedOperationException("Cannot build client services (maybe use inMemory() or jdbc()).");
  }

  public final class ClientBuilder {

    private final String clientId;

    private final Collection<String> authorizedGrantTypes = new LinkedHashSet<>();

    private final Collection<String> authorities = new LinkedHashSet<>();

    private Integer accessTokenValiditySeconds;

    private Integer refreshTokenValiditySeconds;

    private final Collection<String> scopes = new LinkedHashSet<>();

    private final Collection<String> autoApproveScopes = new HashSet<>();

    private String secret;

    private final Set<String> registeredRedirectUris = new HashSet<>();

    private final Set<String> resourceIds = new HashSet<>();

    private boolean autoApprove;

    private final Map<String, Object> additionalInformation = new LinkedHashMap<>();

    private ClientBuilder(String clientId) {
      this.clientId = clientId;
    }

    private ClientDetails build() {
      BaseClientDetails result = new BaseClientDetails();
      result.setClientId(clientId);
      result.setAuthorizedGrantTypes(authorizedGrantTypes);
      result.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
      result.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
      result.setRegisteredRedirectUri(registeredRedirectUris);
      result.setClientSecret(secret);
      result.setScope(scopes);
      result.setAuthorities(AuthorityUtils.createAuthorityList(authorities.toArray(new String[authorities.size()])));
      result.setResourceIds(resourceIds);
      result.setAdditionalInformation(additionalInformation);
      if (autoApprove) {
        result.setAutoApproveScopes(scopes);
      } else {
        result.setAutoApproveScopes(autoApproveScopes);
      }
      return result;
    }

    public ClientBuilder resourceIds(String... resourceIds) {
      this.resourceIds.addAll(Arrays.asList(resourceIds));
      return this;
    }

    public ClientBuilder redirectUris(String... registeredRedirectUris) {
      Collections.addAll(this.registeredRedirectUris, registeredRedirectUris);
      return this;
    }

    public ClientBuilder authorizedGrantTypes(String... authorizedGrantTypes) {
      Collections.addAll(this.authorizedGrantTypes, authorizedGrantTypes);
      return this;
    }

    public ClientBuilder accessTokenValiditySeconds(int accessTokenValiditySeconds) {
      this.accessTokenValiditySeconds = accessTokenValiditySeconds;
      return this;
    }

    public ClientBuilder refreshTokenValiditySeconds(int refreshTokenValiditySeconds) {
      this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
      return this;
    }

    public ClientBuilder secret(String secret) {
      this.secret = secret;
      return this;
    }

    public ClientBuilder scopes(String... scopes) {
      Collections.addAll(this.scopes, scopes);
      return this;
    }

    public ClientBuilder authorities(String... authorities) {
      Collections.addAll(this.authorities, authorities);
      return this;
    }

    public ClientBuilder autoApprove(boolean autoApprove) {
      this.autoApprove = autoApprove;
      return this;
    }

    public ClientBuilder autoApprove(String... scopes) {
      Collections.addAll(this.autoApproveScopes, scopes);
      return this;
    }

    public ClientBuilder additionalInformation(Map<String, ?> map) {
      this.additionalInformation.putAll(map);
      return this;
    }

    public ClientBuilder additionalInformation(String... pairs) {
      for (String pair : pairs) {
        String separator = ":";
        if (!pair.contains(separator) && pair.contains("=")) {
          separator = "=";
        }
        int index = pair.indexOf(separator);
        String key = pair.substring(0, index > 0 ? index : pair.length());
        String value = index > 0 ? pair.substring(index + 1) : null;
        this.additionalInformation.put(key, value);
      }
      return this;
    }

    public ReactiveClientDetailsServiceBuilder<B> and() {
      return ReactiveClientDetailsServiceBuilder.this;
    }

  }

}
