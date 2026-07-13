package com.orchestrate.api.config;

import com.orchestrate.api.auth.jwt.JwtAuthenticationFilter;
import com.orchestrate.api.auth.oauth.GitHubOAuth2UserService;
import com.orchestrate.api.auth.oauth.OAuthAuthenticationFailureHandler;
import com.orchestrate.api.auth.oauth.OAuthAuthenticationSuccessHandler;
import com.orchestrate.api.user.AppUserDetailsService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Handles the OAuth2 login redirect dance for Google/GitHub. Matched narrowly to just these paths
   * and given higher priority (@Order(1)) than the main API chain below, so it never affects
   * /api/** request handling.
   *
   * <p>// TODO(scale-out): this chain relies on in-memory HttpSession state (the default
   * HttpSessionOAuth2AuthorizationRequestRepository) to hold the pending authorization request
   * across the redirect round-trip. Fine for a single instance; if this ever scales horizontally
   * without sticky sessions, it needs a shared session store (e.g. Redis-backed Spring Session).
   */
  @Bean
  @Order(1)
  SecurityFilterChain oauth2LoginFilterChain(
      HttpSecurity http,
      GitHubOAuth2UserService gitHubOAuth2UserService,
      OAuthAuthenticationSuccessHandler oAuthSuccessHandler,
      OAuthAuthenticationFailureHandler oAuthFailureHandler)
      throws Exception {
    http.securityMatcher("/oauth2/**", "/login/oauth2/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        // GET-only OAuth2 redirect dance; nothing state-changing to protect with CSRF here,
        // consistent with the main chain's existing CSRF-disabled posture.
        .csrf(AbstractHttpConfigurer::disable)
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(gitHubOAuth2UserService))
                    .successHandler(oAuthSuccessHandler)
                    .failureHandler(oAuthFailureHandler));
    // Deliberately NOT stateless — see the scale-out TODO above.
    return http.build();
  }

  /**
   * Ties stored Google/GitHub access/refresh tokens to the login session (already used by the chain
   * above for the authorization-request dance) instead of Spring Boot's autoconfigured default: an
   * app-wide, unbounded, never-pruned InMemoryOAuth2AuthorizedClientService. Nothing in this app
   * reads the stored client after login, so there's no reason to retain it beyond the session's
   * lifetime.
   */
  @Bean
  OAuth2AuthorizedClientRepository authorizedClientRepository() {
    return new HttpSessionOAuth2AuthorizedClientRepository();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
      throws Exception {
    http.cors(org.springframework.security.config.Customizer.withDefaults())
        // CSRF disabled: auth rides on cookies, mitigated by SameSite=Lax in dev.
        // TODO(deploy phase): if the frontend (Vercel) and backend (AWS) do NOT end up under one
        // root domain as subdomains (app.X.com / api.X.com), SameSite=Lax will not send the cookie
        // cross-site — we must switch to SameSite=None; Secure AND add real CSRF token protection
        // before any prod deploy. Domain decision is deferred to the deploy phase.
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/health")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/password-reset/request",
                        "/api/auth/password-reset/confirm")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/verify-email")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(
      AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(AppProperties props) {
    CorsConfiguration configuration = new CorsConfiguration();
    // TODO(deploy phase): app.frontend-url must point at the real prod frontend origin once its
    // domain is known.
    configuration.setAllowedOrigins(List.of(props.frontendUrl()));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true); // required so the browser sends/receives auth cookies

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
