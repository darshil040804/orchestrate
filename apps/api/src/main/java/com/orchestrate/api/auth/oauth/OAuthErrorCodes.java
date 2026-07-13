package com.orchestrate.api.auth.oauth;

/**
 * Shared error codes surfaced to the frontend via {@code ?error=<code>} on OAuth redirects.
 *
 * <p>"No verified email" can be detected in two structurally different places — {@link
 * GitHubOAuth2UserService} (before authentication completes, via a thrown exception) and {@link
 * OAuthAuthenticationSuccessHandler}'s own Google check (after authentication completes, via a
 * direct redirect) — so both reference these constants instead of each hardcoding the same string,
 * which would risk drifting out of sync.
 */
public final class OAuthErrorCodes {

  public static final String UNVERIFIED_EMAIL = "oauth_unverified_email";
  public static final String GENERIC_FAILURE = "oauth_failed";

  private OAuthErrorCodes() {}
}
