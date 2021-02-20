package uaa.model.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by VuLD on 7/20/2017.
 */
@Getter
@Setter
public class ProfileRequest {

  private String tenantId;
  private Long profileId;
  private String profileType;
  private String profileSubType;
  private Boolean defaultProfile;

  // for register request
  private String authenticationType;
  private String username;
  private String email;
  private String phone;
  private String facebookId;
  private String facebookToken;
  private String password;

  private String functionName;
  private List<String> functions;

  // for profile info
  private String firstName;
  private String lastName;
  private String fullName;
  private String nickName;
  private String gender;
  private String birthday;
  private Double height;
  private Double weight;
  private String address;
  private String city;
  private String country;
  private String state;
  private String postalCode;
  private Double rating;
  private String avatar;
  private Double latitude;
  private Double longitude;
  private String note;
  private String bloodType;

  // for provider create consumer
  private String patientCode;
  private Long providerId;
  private Long userId;
  private Boolean isNew;

}
