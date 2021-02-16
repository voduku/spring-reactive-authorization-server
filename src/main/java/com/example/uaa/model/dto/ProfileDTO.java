package com.example.uaa.model.dto;

import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Author: chautn on 6/11/2018 5:15 PM
 */
@Getter
@Setter
public class ProfileDTO {

  private Long userId;
  private String tenantId;
  private Long profileId;
  private List<Long> profileIds;
  private Long defaultProfileId;
  private Long rootProfileId;
  private String profileType;
  private String profileSubType;
  private String status;
  private Boolean defaultProfile;

  private String username;
  private String facebookId;
  private String googleId;

  private String displayName;
  private String fullName;
  private String nickName;
  private String firstName;
  private String lastName;
  private String gender;
  private String birthday;
  private String email;
  private String phone;
  private String address;
  private String city;
  private String country;
  private String state;
  private String postalCode;
  private String avatar;
  private String summary;

  private UserDTO user;

  private Timestamp emailVerifiedDate;
  private Timestamp phoneVerifiedDate;
  private List<RoleDTO> roles;

  private String bannedNote;
  private Boolean isBanned;

  private String language;
}
