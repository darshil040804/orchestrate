package com.orchestrate.api.auth;

import com.orchestrate.api.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the auth cookies. Access token is site-wide; the refresh token is path-scoped to {@code
 * /api/auth} so the browser only ever sends it to the auth endpoints.
 */
@Component
public class CookieFactory {

  public static final String ACCESS_TOKEN_COOKIE = "access_token";
  public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  private static final String ACCESS_PATH = "/";
  private static final String REFRESH_PATH = "/api/auth";

  private final AppProperties props;

  public CookieFactory(AppProperties props) {
    this.props = props;
  }

  public ResponseCookie accessCookie(String value) {
    return base(ACCESS_TOKEN_COOKIE, value, ACCESS_PATH, props.jwt().accessTtl()).build();
  }

  public ResponseCookie refreshCookie(String value) {
    return base(REFRESH_TOKEN_COOKIE, value, REFRESH_PATH, props.jwt().refreshTtl()).build();
  }

  public ResponseCookie clearAccessCookie() {
    return base(ACCESS_TOKEN_COOKIE, "", ACCESS_PATH, Duration.ZERO).build();
  }

  public ResponseCookie clearRefreshCookie() {
    return base(REFRESH_TOKEN_COOKIE, "", REFRESH_PATH, Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(
      String name, String value, String path, Duration maxAge) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(props.cookies().secure())
        .sameSite(props.cookies().sameSite())
        .path(path)
        .maxAge(maxAge);
  }
}
