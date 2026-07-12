package com.orchestrate.api.auth.jwt;

import com.orchestrate.api.config.AppProperties;
import com.orchestrate.api.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Signs and verifies short-lived HS256 access-token JWTs (jjwt). */
@Service
public class JwtService {

  private final SecretKey key;
  private final AppProperties props;

  public JwtService(AppProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccessToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .issuedAt(java.util.Date.from(now))
        .expiration(java.util.Date.from(now.plus(props.jwt().accessTtl())))
        .signWith(key)
        .compact();
  }

  /** Parse + verify an access token. Returns empty for any invalid/expired/tampered token. */
  public Optional<AccessTokenPrincipal> parse(String token) {
    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      UUID userId = UUID.fromString(claims.getSubject());
      String email = claims.get("email", String.class);
      return Optional.of(new AccessTokenPrincipal(userId, email));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public record AccessTokenPrincipal(UUID userId, String email) {}
}
