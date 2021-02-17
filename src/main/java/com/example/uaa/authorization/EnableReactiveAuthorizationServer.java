package com.example.uaa.authorization;

import com.example.uaa.authorization.configuration.ReactiveAuthorizationServerEndpointsConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * @author VuDo
 * @since 2/14/2021
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ReactiveAuthorizationServerEndpointsConfiguration.class})
public @interface EnableReactiveAuthorizationServer {

}
