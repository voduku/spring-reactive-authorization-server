package com.example.uaa;

import com.example.uaa.oauth.EnableReactiveAuthorizationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableReactiveAuthorizationServer
@SpringBootApplication
public class UaaApplication {

  public static void main(String[] args) {
    SpringApplication.run(UaaApplication.class, args);
  }

}
