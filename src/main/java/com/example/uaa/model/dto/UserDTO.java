package com.example.uaa.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Author: chautn on 6/11/2018 2:29 PM
 */
@Getter
@Setter
public class UserDTO {

  private Long userId;
  private String username;
  private String password;
  private String phone;
  private String email;
  private String facebookId;
  private String status;
  private String tempPassword;
  private Integer attemptCount;
}
