package uaa.model.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author VuDo
 * @since 2/11/2021
 */
@Getter
@Setter
@ToString
public class AuthRequest {

  private String tenantId;
  private String profileType;
  private String grantType;
  @JsonIgnore
  private Long userId;
  private String username;
  private String password;
  private String email;
  private String phone;
  private String otp;
  private String token;
  private String captchaToken;
  private String client;
  private String anonymousId;
  private Long providerId;

  public static AuthRequest fromParams(Map<String, String> params) {
    if (params != null && params.size() > 0) {
      AuthRequest request = new AuthRequest();
      request.setTenantId(params.get("client_id"));
      request.setProfileType(params.get("profile_type"));
      request.setGrantType(params.get("grant_type"));
      request.setUsername(params.get("username"));
      request.setPassword(params.get("password"));
      request.setEmail(params.get("email"));
      request.setPhone(params.get("phone"));
      request.setOtp(params.get("otp"));
      request.setToken(params.get("authorization.token"));
      request.setCaptchaToken(params.get("capchaToken"));
      request.setClient(params.get("authorization.client"));
      request.setUserId(params.get("user_id") != null ? Long.parseLong(params.get("user_id")) : null);
      request.setAnonymousId(params.get("anonymousId"));
      request.setProviderId(params.get("providerId") != null ? Long.parseLong(params.get("providerId")) : null);
      return request;
    } else {
      return null;
    }
  }

  public Map<String, String> toParamsMap() {
    Map<String, String> params = new HashMap<>();
    if (tenantId != null) {
      params.put("client_id", tenantId);
    }
    if (profileType != null) {
      params.put("profile_type", profileType);
    }
    if (grantType != null) {
      params.put("grant_type", grantType);
    }
    if (username != null) {
      params.put("username", username);
    }
    if (password != null) {
      params.put("password", password);
    }
    if (email != null) {
      params.put("email", email);
    }
    if (phone != null) {
      params.put("phone", phone);
    }
    if (otp != null) {
      params.put("otp", otp);
    }
    if (token != null) {
      params.put("authorization.token", token);
    }
    if (captchaToken != null) {
      params.put("capchaToken", captchaToken);
    }
    if (client != null) {
      params.put("authorization.client", client);
    }
    if (userId != null) {
      params.put("user_id", userId.toString());
    }

    if (anonymousId != null) {
      params.put("anonymousId", anonymousId);
    }

    if (providerId != null) {
      params.put("providerId", providerId.toString());
    }
    return params;
  }
}

