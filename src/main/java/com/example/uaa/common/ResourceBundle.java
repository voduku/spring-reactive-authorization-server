package com.example.uaa.common;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * @author VuDo
 * @since 2/11/2021
 */
public class ResourceBundle extends ResourceBundleMessageSource {

  public String getMessage(String code) {
    return getMessage(code, new Object[0]);
  }

  public String getMessage(String code, Object... params) {
    return getMessage(code, params, "Code not found", LocaleContextHolder.getLocale());
  }

}
