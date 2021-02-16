package com.example.uaa.model.oauth;

import static java.util.stream.Collectors.toList;

import com.example.uaa.model.dto.ProfileDTO;
import com.example.uaa.model.dto.RoleDTO;
import com.example.uaa.model.enumerator.Role;
import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/**
 * @author VuDo
 * @since 2/11/2021
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails implements OAuth2AuthenticatedPrincipal, Principal, Serializable {

  private String tenantId;
  private Long tenantProfileId;
  private Long userId;
  private List<Role> roles;
  private Long profileId;
  private String fullName;
  private String displayName;
  private String username;
  private String avatar;
  private String profileType;
  private String email;
  private List<GrantedAuthority> authorities;

  public UserDetails(String tenantId) {
    this.tenantId = tenantId;
  }

  public UserDetails(ProfileDTO profile) {
    tenantId = profile.getTenantId();
    tenantProfileId = profile.getDefaultProfileId();
    userId = profile.getUserId();
    roles = profile.getRoles().stream().map(RoleDTO::getRole).map(Role::valueOf).collect(toList());
    profileId = profile.getProfileId();
    fullName = profile.getFullName();
    displayName = profile.getDisplayName();
    username = profile.getUsername();
    avatar = profile.getAvatar();
    profileType = profile.getProfileType();
    email = profile.getEmail();
    authorities = roles.stream().map(Role::name).map(SimpleGrantedAuthority::new).collect(toList());
  }

  @Override
  public String getName() {
    return username + "_" + profileId;
  }

  @Override
  public Map<String, Object> getAttributes() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("tenantId", tenantId);
    attributes.put("tenantProfileId", tenantProfileId);
    attributes.put("userId", userId);
    attributes.put("roles", roles);
    attributes.put("profileId", profileId);
    attributes.put("fullName", fullName);
    attributes.put("displayName", displayName);
    attributes.put("username", username);
    attributes.put("avatar", avatar);
    attributes.put("profileType", profileType);
    attributes.put("email", email);
    return attributes;
  }
}
