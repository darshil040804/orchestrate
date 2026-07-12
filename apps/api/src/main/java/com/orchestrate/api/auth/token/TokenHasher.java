package com.orchestrate.api.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Generates opaque random tokens and hashes them for storage. We store only the SHA-256 hex of a
 * token so a DB leak never exposes usable tokens; the raw value is handed to the user (cookie/link)
 * and looked up by re-hashing on the way back in.
 */
@Component
public class TokenHasher {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

  /** 32 bytes (256 bits) of entropy, base64url-encoded. */
  public String generateRawToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return BASE64_URL.encodeToString(bytes);
  }

  /**
   * SHA-256 hex of the raw token (64 chars) — matches the {@code varchar(64)} token_hash columns.
   */
  public String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
