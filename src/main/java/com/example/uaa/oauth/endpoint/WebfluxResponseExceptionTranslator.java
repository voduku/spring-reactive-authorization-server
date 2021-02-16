package com.example.uaa.oauth.endpoint;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InsufficientScopeException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.web.util.ThrowableAnalyzer;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public class WebfluxResponseExceptionTranslator implements WebResponseExceptionTranslator<OAuth2Exception> {

  private ThrowableAnalyzer throwableAnalyzer = new ThrowableAnalyzer();

  @Override
  public ResponseEntity<OAuth2Exception> translate(Exception e) {

    // Try to extract a SpringSecurityException from the stacktrace
    Throwable[] causeChain = throwableAnalyzer.determineCauseChain(e);
    Exception ase = (OAuth2Exception) throwableAnalyzer.getFirstThrowableOfType(OAuth2Exception.class, causeChain);

    if (ase != null) {
      return handleOAuth2Exception((OAuth2Exception) ase);
    }

    ase = (AuthenticationException) throwableAnalyzer.getFirstThrowableOfType(AuthenticationException.class,
        causeChain);
    if (ase != null) {
      return handleOAuth2Exception(new WebfluxResponseExceptionTranslator.UnauthorizedException(e.getMessage(), e));
    }

    ase = (AccessDeniedException) throwableAnalyzer.getFirstThrowableOfType(AccessDeniedException.class, causeChain);
    if (ase != null) {
      return handleOAuth2Exception(new WebfluxResponseExceptionTranslator.ForbiddenException(ase.getMessage(), ase));
    }

//    ase = (HttpRequestMethodNotSupportedException) throwableAnalyzer.getFirstThrowableOfType(
//        HttpRequestMethodNotSupportedException.class, causeChain);
//    if (ase != null) {
//      return handleOAuth2Exception(new WebfluxResponseExceptionTranslator.MethodNotAllowed(ase.getMessage(), ase));
//    }

    return handleOAuth2Exception(new WebfluxResponseExceptionTranslator.ServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e));

  }

  private ResponseEntity<OAuth2Exception> handleOAuth2Exception(OAuth2Exception e) {

    int status = e.getHttpErrorCode();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cache-Control", "no-store");
    headers.set("Pragma", "no-cache");
    if (status == HttpStatus.UNAUTHORIZED.value() || (e instanceof InsufficientScopeException)) {
      headers.set("WWW-Authenticate", String.format("%s %s", OAuth2AccessToken.BEARER_TYPE, e.getSummary()));
    }

    return new ResponseEntity<>(e, headers, HttpStatus.valueOf(status));

  }

  public void setThrowableAnalyzer(ThrowableAnalyzer throwableAnalyzer) {
    this.throwableAnalyzer = throwableAnalyzer;
  }

  private static class ForbiddenException extends OAuth2Exception {

    public ForbiddenException(String msg, Throwable t) {
      super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
      return "access_denied";
    }

    @Override
    public int getHttpErrorCode() {
      return 403;
    }

  }

  private static class ServerErrorException extends OAuth2Exception {

    public ServerErrorException(String msg, Throwable t) {
      super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
      return "server_error";
    }

    @Override
    public int getHttpErrorCode() {
      return 500;
    }

  }

  private static class UnauthorizedException extends OAuth2Exception {

    public UnauthorizedException(String msg, Throwable t) {
      super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
      return "unauthorized";
    }

    @Override
    public int getHttpErrorCode() {
      return 401;
    }

  }

  private static class MethodNotAllowed extends OAuth2Exception {

    public MethodNotAllowed(String msg, Throwable t) {
      super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
      return "method_not_allowed";
    }

    @Override
    public int getHttpErrorCode() {
      return 405;
    }

  }
}
