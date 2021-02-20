package authorization.endpoint;

import authorization.ReactiveOAuth2RequestFactory;
import authorization.ReactiveTokenGranter;
import authorization.client.ReactiveClientDetailsService;
import authorization.impl.DefaultReactiveOAuth2RequestFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public class AbstractReactiveEndpoint implements InitializingBean {

  protected final Log logger = LogFactory.getLog(AbstractReactiveEndpoint.class);

  private WebResponseExceptionTranslator<OAuth2Exception> providerExceptionHandler = new WebfluxResponseExceptionTranslator();

  private ReactiveTokenGranter tokenGranter;

  private ReactiveClientDetailsService clientDetailsService;

  private ReactiveOAuth2RequestFactory oAuth2RequestFactory;

  private ReactiveOAuth2RequestFactory defaultOAuth2RequestFactory;

  public void afterPropertiesSet() {
//    Assert.state(tokenGranter != null, "TokenGranter must be provided");
//    Assert.state(clientDetailsService != null, "ClientDetailsService must be provided");
    defaultOAuth2RequestFactory = new DefaultReactiveOAuth2RequestFactory(clientDetailsService);
    if (oAuth2RequestFactory == null) {
      oAuth2RequestFactory = defaultOAuth2RequestFactory;
    }
  }

  public void setProviderExceptionHandler(WebResponseExceptionTranslator<OAuth2Exception> providerExceptionHandler) {
    this.providerExceptionHandler = providerExceptionHandler;
  }

  protected ReactiveTokenGranter getTokenGranter() {
    return tokenGranter;
  }

  public void setTokenGranter(ReactiveTokenGranter tokenGranter) {
    this.tokenGranter = tokenGranter;
  }

  protected WebResponseExceptionTranslator<OAuth2Exception> getExceptionTranslator() {
    return providerExceptionHandler;
  }

  protected ReactiveOAuth2RequestFactory getOAuth2RequestFactory() {
    return oAuth2RequestFactory;
  }

  public void setOAuth2RequestFactory(ReactiveOAuth2RequestFactory oAuth2RequestFactory) {
    this.oAuth2RequestFactory = oAuth2RequestFactory;
  }

  protected ReactiveOAuth2RequestFactory getDefaultOAuth2RequestFactory() {
    return defaultOAuth2RequestFactory;
  }

  protected ReactiveClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  public void setClientDetailsService(ReactiveClientDetailsService clientDetailsService) {
    this.clientDetailsService = clientDetailsService;
  }

}