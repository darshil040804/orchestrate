package com.orchestrate.api.auth.oauth;

import com.orchestrate.api.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class OAuthAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private static final Logger log =
      LoggerFactory.getLogger(OAuthAuthenticationFailureHandler.class);

  private final AppProperties props;

  public OAuthAuthenticationFailureHandler(AppProperties props) {
    this.props = props;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    String errorCode = OAuthErrorCodes.GENERIC_FAILURE;
    if (exception instanceof OAuth2AuthenticationException oauth2Ex
        && OAuthErrorCodes.UNVERIFIED_EMAIL.equals(oauth2Ex.getError().getErrorCode())) {
      errorCode = OAuthErrorCodes.UNVERIFIED_EMAIL;
    }
    log.info("OAuth login failed: provider={} errorCode={}", registrationId(request), errorCode);
    response.sendRedirect(
        props.frontendUrl() + "/?error=" + UriUtils.encode(errorCode, StandardCharsets.UTF_8));
  }

  /**
   * This handler is only wired to the OAuth2 login callback path
   * (/login/oauth2/code/{registrationId}), so the registration id is reliably the last path segment
   * — simpler than threading it through the various exception types that can land here.
   */
  private String registrationId(HttpServletRequest request) {
    String path = request.getRequestURI();
    int lastSlash = path.lastIndexOf('/');
    return lastSlash >= 0 && lastSlash < path.length() - 1
        ? path.substring(lastSlash + 1)
        : "unknown";
  }
}
