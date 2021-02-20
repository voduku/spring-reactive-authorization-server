package uaa.model.oauth;

import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author VuDo
 * @since 2/11/2021
 */
@NoArgsConstructor
public class Auth implements Authentication, CredentialsContainer {

  private Set<GrantedAuthority> authorities;
  private UserDetails userDetails;
  private AuthRequest oauth2Request;
  private boolean authenticated;

  public Auth(AuthRequest oauth2Request) {
    this.oauth2Request = oauth2Request;
    this.authorities = null;
    this.authenticated = false;
  }

  public Auth(AuthRequest oauth2Request, Set<GrantedAuthority> authorities) {
    this.oauth2Request = oauth2Request;
    this.authorities = authorities;
    this.authenticated = false;
  }

  public Auth(Map<String, String> tokenRequestParameters, Set<GrantedAuthority> authorities) {
    this.oauth2Request = AuthRequest.fromParams(tokenRequestParameters);
    this.authorities = authorities;
    this.authenticated = false;
  }

  public String getPassword() {
    return oauth2Request.getPassword();
  }

  public String getTenantId() {
    return oauth2Request.getTenantId();
  }

  @Override
  public Set<GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public AuthRequest getCredentials() {
    return this.oauth2Request;
  }

  @Override
  public Object getDetails() {
    // not use this field
    return null;
  }

  @Override
  public UserDetails getPrincipal() {
    return this.userDetails;
  }

  public void setPrincipal(UserDetails userDetails) {
    this.userDetails = userDetails;
  }

  @Override
  public boolean isAuthenticated() {
    return this.authenticated || this.getPrincipal() == null;
  }

  @Override
  public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
    this.authenticated = authenticated;
  }

  @Override
  public String getName() {
    return this.getPrincipal() != null ? this.getPrincipal().getName() : this.getCredentials().getTenantId();
  }

  @Override
  public void eraseCredentials() {
    this.oauth2Request = null;
  }

}

