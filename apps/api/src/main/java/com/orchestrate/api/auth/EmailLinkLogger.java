package com.orchestrate.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder for a real email provider. For now the verification/reset links are logged to the
 * console so the flow is testable end-to-end without wiring an SMTP/API provider.
 *
 * <p>TODO Phase 1 fast-follow: send these via a real email provider (e.g. Resend/SES) instead of
 * logging, and stop logging the raw token.
 */
@Component
public class EmailLinkLogger {

  private static final Logger log = LoggerFactory.getLogger(EmailLinkLogger.class);

  // Base URL of the API; verification/reset are API endpoints for now (no frontend pages yet).
  private static final String BASE_URL = "http://localhost:8080";

  public void sendVerificationLink(String email, String rawToken) {
    log.info(
        "[EMAIL:verify] To={} — verify: {}/api/auth/verify-email?token={}",
        email,
        BASE_URL,
        rawToken);
  }

  public void sendPasswordResetLink(String email, String rawToken) {
    log.info(
        "[EMAIL:reset] To={} — reset token (POST /api/auth/password-reset/confirm): {}",
        email,
        rawToken);
  }

  public void sendOrgInvitationLink(String email, String orgName, String rawToken) {
    log.info(
        "[EMAIL:invite] To={} — invited to '{}'. Accept (POST /api/invitations/accept): {}",
        email,
        orgName,
        rawToken);
  }
}
