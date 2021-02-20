package uaa.model.request;

import lombok.Data;
import uaa.model.enumerator.AuthenticationType;
import uaa.model.oauth.AuthRequest;

/**
 * Author: chautn on 6/13/2018 3:49 PM
 */
@Data
public class AuthUserRequest {

  private String tenantId;
  private Long userId;
  private String username;
  private String email;
  private String facebookId;
  private String googleId;
  private String password;
  private String token;
  private AuthenticationType grantType;
  // only for login
  private Boolean attemptToLogin;
  private String client;
  private String anonymousId;
  private Long providerId;

  public AuthUserRequest(AuthRequest request) {
    this.tenantId = request.getTenantId();
    this.username = request.getUsername();
    this.email = request.getEmail();
    this.password = request.getPassword();
    this.attemptToLogin = true;
    this.client = request.getClient();
    this.userId = request.getUserId();
    this.providerId = request.getProviderId();
  }
}
