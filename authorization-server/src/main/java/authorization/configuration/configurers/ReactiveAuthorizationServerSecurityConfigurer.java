package authorization.configuration.configurers;

import lombok.Getter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author VuDo
 * @since 2/25/2021
 */
public class ReactiveAuthorizationServerSecurityConfigurer {

  private PasswordEncoder passwordEncoder; // for client secrets

  private SecurityAccess tokenKeyAccess = SecurityAccess.DENY_ALL;

  private SecurityAccess checkTokenAccess = SecurityAccess.DENY_ALL;

  private ServerHttpSecurity security;

  public ReactiveAuthorizationServerSecurityConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    return this;
  }

  public ReactiveAuthorizationServerSecurityConfigurer tokenKeyAccess(SecurityAccess tokenKeyAccess) {
    this.tokenKeyAccess = tokenKeyAccess;
    return this;
  }

  public ReactiveAuthorizationServerSecurityConfigurer checkTokenAccess(SecurityAccess checkTokenAccess) {
    this.checkTokenAccess = checkTokenAccess;
    return this;
  }

  public ReactiveAuthorizationServerSecurityConfigurer security(Customizer<ServerHttpSecurity> customizer) {
    customizer.customize(security);
    return this;
  }

  public void setSecurity(ServerHttpSecurity security) {
    this.security = security;
  }

  public SecurityAccess getTokenKeyAccess() {
    return tokenKeyAccess;
  }

  public SecurityAccess getCheckTokenAccess() {
    return checkTokenAccess;
  }

  public enum SecurityAccess {
    AUTHENTICATED,
    PERMIT_ALL,
    DENY_ALL,
    AUTHORITY,
    ROLE;

    @Getter
    private String[] value;

    public SecurityAccess hasAuthority(String authority) {
      SecurityAccess access = AUTHORITY;
      access.value = new String[]{authority};
      return access;
    }

    public SecurityAccess hasAnyAuthority(String... authorities) {
      SecurityAccess access = AUTHORITY;
      access.value = authorities;
      return access;
    }

    public SecurityAccess hasRole(String role) {
      SecurityAccess access = ROLE;
      access.value = new String[]{role};
      return access;
    }

    public SecurityAccess hasAnyRole(String... roles) {
      SecurityAccess access = AUTHORITY;
      access.value = roles;
      return access;
    }
  }
}
