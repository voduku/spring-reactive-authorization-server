package uaa.model.response;

import java.util.List;
import lombok.Data;

/**
 * Author: chautn on 6/14/2018 10:27 AM
 */
@Data
public class RestResult<T> {

  public static final String STATUS_SUCCESS = "success";
  public static final String STATUS_ERROR = "error";

  private String status;

  private List<String> messages;

  private String message;

  private T data;

  private Integer code;
}
