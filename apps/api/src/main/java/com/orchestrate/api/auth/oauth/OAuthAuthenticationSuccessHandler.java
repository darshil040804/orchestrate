package com.orchestrate.api.auth.oauth;

import com.orchestrate.api.auth.AuthService;
import com.orchestrate.api.auth.AuthService.IssuedTokens;
import com.orchestrate.api.auth.AuthService.OAuthLoginResult;
import com.orchestrate.api.auth.CookieFactory;
import com.orchestrate.api.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Runs after Spring Security has already exchanged the code and loaded the OAuth2/OIDC user.
 * Extracts a provider-verified email uniformly from either principal shape, finds-or-creates the
 * User and issues tokens via {@link AuthService#loginOrSignupViaOAuth}, sets the exact same cookies
 * password login uses via {@link CookieFactory}, then redirects to the frontend.
 */
@Component
public class OAuthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private static final Logger log =
      LoggerFactory.getLogger(OAuthAuthenticationSuccessHandler.class);

  private final AuthService authService;
  private final CookieFactory cookieFactory;
  private final AppProperties props;

  public OAuthAuthenticationSuccessHandler(
      AuthService authService, CookieFactory cookieFactory, AppProperties props) {
    this.authService = authService;
    this.cookieFactory = cookieFactory;
    this.props = props;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    String email = extractVerifiedEmail(authentication);
    if (email == null) {
      // Google's OIDC email_verified came back false/absent. This is caught here rather than via
      // OAuthAuthenticationFailureHandler because Spring Security already considers this
      // authentication successful by the time we're in this handler — it structurally cannot be
      // rerouted through the failure handler. Same error code as the GitHub case (shared constant
      // in OAuthErrorCodes), not a second hardcoded literal.
      response.sendRedirect(
          props.frontendUrl() + "/login?error=" + OAuthErrorCodes.UNVERIFIED_EMAIL);
      return;
    }

    OAuthLoginResult result = authService.loginOrSignupViaOAuth(email);
    IssuedTokens tokens = result.tokens();
    log.info(
        "OAuth login succeeded: provider={} userId={} newAccount={}",
        registrationId(authentication),
        tokens.user().getId(),
        result.newAccount());

    ResponseCookie access = cookieFactory.accessCookie(tokens.accessToken());
    ResponseCookie refresh = cookieFactory.refreshCookie(tokens.refreshToken());
    response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());

    response.sendRedirect(props.frontendUrl() + "/app");
  }

  private String registrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
      return oauth2Token.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  /**
   * Google arrives as an {@link OidcUser}: defensively re-check {@code email_verified} here too
   * (don't just trust that Google always sets it). GitHub arrives as a plain {@link OAuth2User}
   * whose {@code email} attribute is guaranteed provider-verified by {@link
   * GitHubOAuth2UserService} — any GitHub login lacking one already failed before reaching here.
   */
  private String extractVerifiedEmail(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof OidcUser oidcUser) {
      Boolean verified = oidcUser.getEmailVerified();
      return (verified != null && verified) ? oidcUser.getEmail() : null;
    }
    if (principal instanceof OAuth2User oAuth2User) {
      Object email = oAuth2User.getAttribute("email");
      return email == null ? null : email.toString();
    }
    return null;
  }
}
