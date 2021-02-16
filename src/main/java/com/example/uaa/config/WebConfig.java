package com.example.uaa.config;

import com.example.uaa.common.ResourceBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebConfig {
  @Bean
  ResourceBundle messageSource() {
    ResourceBundle resourceBundle = new ResourceBundle();
    resourceBundle.setBasename("messages.messages");
    resourceBundle.setDefaultEncoding("UTF-8");
    return resourceBundle;
  }
}