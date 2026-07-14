package com.orchestrate.api.auth;

import com.orchestrate.api.config.AppProperties;
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

  private final AppProperties props;

  public EmailLinkLogger(AppProperties props) {
    this.props = props;
  }

  public void sendVerificationLink(String email, String rawToken) {
    log.info(
        "[EMAIL:verify] To={} — verify: {}/verify-email?token={}",
        email,
        props.frontendUrl(),
        rawToken);
  }

  public void sendPasswordResetLink(String email, String rawToken) {
    log.info(
        "[EMAIL:reset] To={} — reset: {}/reset-password/confirm?token={}",
        email,
        props.frontendUrl(),
        rawToken);
  }

  public void sendOrgInvitationLink(String email, String orgName, String rawToken) {
    log.info(
        "[EMAIL:invite] To={} — invited to '{}': {}/invitations/accept?token={}",
        email,
        orgName,
        props.frontendUrl(),
        rawToken);
  }
}
