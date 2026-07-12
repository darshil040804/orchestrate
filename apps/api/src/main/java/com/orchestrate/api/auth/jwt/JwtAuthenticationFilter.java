package com.orchestrate.api.auth.jwt;

import com.orchestrate.api.auth.CookieFactory;
import com.orchestrate.api.auth.jwt.JwtService.AccessTokenPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the access-token cookie, verifies the JWT, and populates the SecurityContext. Stateless: no
 * session is created. Invalid/missing tokens simply leave the context unauthenticated, letting the
 * authorization rules return 401/403.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      readCookie(request, CookieFactory.ACCESS_TOKEN_COOKIE)
          .flatMap(jwtService::parse)
          .ifPresent(principal -> authenticate(principal, request));
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(AccessTokenPrincipal principal, HttpServletRequest request) {
    var authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of()); // no roles yet (Phase 1c)
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private Optional<String> readCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
        return Optional.of(cookie.getValue());
      }
    }
    return Optional.empty();
  }
}
