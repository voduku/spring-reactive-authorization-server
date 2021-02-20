package uaa;

import authorization.EnableReactiveAuthorizationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableReactiveAuthorizationServer
public class UaaApplication {

  public static void main(String[] args) {
    SpringApplication.run(UaaApplication.class, args);
  }

}
