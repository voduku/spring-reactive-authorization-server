package com.example.uaa.model.dto;

import java.io.Serializable;
import java.util.Set;
import lombok.Data;

/**
 * @author VuDo
 * @since 2/15/2021
 */
@Data
public class RoleDTO implements Serializable {

  private String role;
  private String status;
  private String statusNote;
  private Set<String> functions;
}