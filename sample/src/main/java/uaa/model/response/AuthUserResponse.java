package uaa.model.response;

import java.util.List;
import lombok.Data;
import uaa.model.dto.ProfileDTO;
import uaa.model.dto.UserDTO;

/**
 * Author: chautn on 6/20/2018 3:57 PM
 */
@Data
public class AuthUserResponse {

  UserDTO user;
  List<ProfileDTO> profiles;
}
