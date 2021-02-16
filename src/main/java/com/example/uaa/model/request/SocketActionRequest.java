package com.example.uaa.model.request;

import com.example.uaa.model.enumerator.ForceLogoutEvent;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author VuDo
 * @since 2/11/2021
 */
@Data
@NoArgsConstructor
public class SocketActionRequest {

  private String channelId;
  private String fromUserType;
  private Long fromUserId;
  private String type;
  private String toUserType;
  private Long toUserId;
  private Long createdTime;
  private Map<String, Object> data;

  public SocketActionRequest(Long toUserId, String type, Map<String, Object> data) {
    this.toUserId = toUserId;
    this.type = type;
    this.data = data;
    this.createdTime = System.currentTimeMillis();
  }

  public static SocketActionRequest forceLogoutNotification(Long toUserId, Map<String, Object> data) {
    return new SocketActionRequest(toUserId, ForceLogoutEvent.SINGLE_SESSION_FORCE_LOGOUT.name(), data);
  }
}
