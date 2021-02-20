package uaa.model.enumerator;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;

/**
 * @author VuDo
 * @since 2/11/2021
 */
public enum AuthenticationType {
  PASSWORD,
  FACEBOOK,
  GOOGLE,
  ANONYMOUS,
  INTERNAL;

  public static AuthenticationType authenticationType(String type) {
    for (AuthenticationType authenticationType : values()) {
      if (authenticationType.name().equalsIgnoreCase(type)) {
        return authenticationType;
      }
    }
    throw new UnsupportedGrantTypeException("grant type is not supported");
  }

  public static List<String> toBeIgnored() {
    return ImmutableList.of(ANONYMOUS.name(), INTERNAL.name());
  }
}
