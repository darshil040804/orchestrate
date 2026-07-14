package com.orchestrate.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly-typed binding for the {@code app.*} config block in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, Jwt jwt, Cookies cookies, Tokens tokens) {

  public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}

  public record Cookies(boolean secure, String sameSite) {}

  public record Tokens(
      Duration emailVerificationTtl, Duration passwordResetTtl, Duration orgInvitationTtl) {}
}
