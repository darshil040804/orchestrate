package com.orchestrate.api.auth.oauth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orchestrate.api.auth.AuthService;
import com.orchestrate.api.auth.AuthService.IssuedTokens;
import com.orchestrate.api.auth.AuthService.OAuthLoginResult;
import com.orchestrate.api.auth.CookieFactory;
import com.orchestrate.api.config.AppProperties;
import com.orchestrate.api.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Pure unit tests (no Spring context, no DB) asserting the exact redirect targets this handler
 * produces — the frontend's "/app" post-login destination and the "/login?error=..." path for the
 * unverified-email branch, changed together with {@link OAuthAuthenticationFailureHandler} this
 * session so a static-only marketing "/" no longer needs to render OAuth errors.
 */
class OAuthAuthenticationSuccessHandlerTest {

  private static final AppProperties PROPS =
      new AppProperties("http://localhost:3000", null, null, null);

  @Test
  void redirectsToAppOnVerifiedEmail() throws Exception {
    AuthService authService = mock(AuthService.class);
    CookieFactory cookieFactory = mock(CookieFactory.class);
    OAuthAuthenticationSuccessHandler handler =
        new OAuthAuthenticationSuccessHandler(authService, cookieFactory, PROPS);

    User user = new User("user@example.com", null);
    when(authService.loginOrSignupViaOAuth("user@example.com"))
        .thenReturn(
            new OAuthLoginResult(new IssuedTokens("access-tok", "refresh-tok", user), false));
    when(cookieFactory.accessCookie("access-tok"))
        .thenReturn(ResponseCookie.from("access_token", "access-tok").build());
    when(cookieFactory.refreshCookie("refresh-tok"))
        .thenReturn(ResponseCookie.from("refresh_token", "refresh-tok").build());

    OidcUser principal = mock(OidcUser.class);
    when(principal.getEmailVerified()).thenReturn(true);
    when(principal.getEmail()).thenReturn("user@example.com");
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(principal);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(response).sendRedirect("http://localhost:3000/app");
    verify(response, never()).sendRedirect(eq("http://localhost:3000/"));
  }

  @Test
  void redirectsToLoginWithErrorOnUnverifiedGoogleEmail() throws Exception {
    AuthService authService = mock(AuthService.class);
    CookieFactory cookieFactory = mock(CookieFactory.class);
    OAuthAuthenticationSuccessHandler handler =
        new OAuthAuthenticationSuccessHandler(authService, cookieFactory, PROPS);

    OidcUser principal = mock(OidcUser.class);
    when(principal.getEmailVerified()).thenReturn(false);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(principal);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(response)
        .sendRedirect("http://localhost:3000/login?error=" + OAuthErrorCodes.UNVERIFIED_EMAIL);
    verify(authService, never()).loginOrSignupViaOAuth(any());
    verify(cookieFactory, never()).accessCookie(any());
  }
}
