package com.orchestrate.api.auth.oauth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orchestrate.api.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Pure unit tests (no Spring context) asserting this handler's redirect now targets
 * "/login?error=..." rather than "/?error=..." — "/" became static-only marketing this session, and
 * "/login" already has the error-banner machinery to render these codes.
 */
class OAuthAuthenticationFailureHandlerTest {

  private static final AppProperties PROPS =
      new AppProperties("http://localhost:3000", null, null, null);

  @Test
  void redirectsToLoginWithGenericFailureCode() throws Exception {
    OAuthAuthenticationFailureHandler handler = new OAuthAuthenticationFailureHandler(PROPS);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
    HttpServletResponse response = mock(HttpServletResponse.class);

    handler.onAuthenticationFailure(request, response, new BadCredentialsException("denied"));

    verify(response)
        .sendRedirect("http://localhost:3000/login?error=" + OAuthErrorCodes.GENERIC_FAILURE);
  }

  @Test
  void redirectsToLoginWithUnverifiedEmailCode() throws Exception {
    OAuthAuthenticationFailureHandler handler = new OAuthAuthenticationFailureHandler(PROPS);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/login/oauth2/code/github");
    HttpServletResponse response = mock(HttpServletResponse.class);

    OAuth2Error error =
        new OAuth2Error(OAuthErrorCodes.UNVERIFIED_EMAIL, "no verified email", null);
    handler.onAuthenticationFailure(
        request, response, new OAuth2AuthenticationException(error, error.getErrorCode()));

    verify(response)
        .sendRedirect("http://localhost:3000/login?error=" + OAuthErrorCodes.UNVERIFIED_EMAIL);
  }
}
