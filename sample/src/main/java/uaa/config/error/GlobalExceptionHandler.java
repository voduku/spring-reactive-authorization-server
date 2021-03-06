package uaa.config.error;

import java.util.Map;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 3/6/2021
 */
@Order(-2)
@Component
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

//  @Bean
//  public ServerCodecConfigurer serverCodecConfigurer(){
//    return new DefaultServerCodecConfigurer();
//  }

  public GlobalExceptionHandler(GlobalErrorAttributes attributes, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
    super(attributes, new WebProperties.Resources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }

  private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    Map<String, Object> errorPropertiesMap = getErrorAttributes(request, ErrorAttributeOptions.of(Include.values()));

    return ServerResponse
        .status(resolve(errorPropertiesMap.get("status")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(errorPropertiesMap);
  }


  private HttpStatus resolve(Object httpStatus) {
    return httpStatus instanceof Integer ? HttpStatus.resolve((int) httpStatus) : HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
