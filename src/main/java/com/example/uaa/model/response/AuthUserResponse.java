package com.example.uaa.model.response;

import com.example.uaa.model.dto.ProfileDTO;
import com.example.uaa.model.dto.UserDTO;
import java.util.List;
import lombok.Data;

/**
 * Author: chautn on 6/20/2018 3:57 PM
 */
@Data
public class AuthUserResponse {

  UserDTO user;
  List<ProfileDTO> profiles;
}
