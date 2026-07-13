package com.orchestrate.api.auth.oauth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub's {@code /user} endpoint returns {@code email: null} unless the user has made an email
 * public — even when the {@code user:email} scope was granted. This delegate falls back to GitHub's
 * {@code /user/emails} endpoint (authenticated with the same access token) to find a verified
 * email, and fails the whole OAuth exchange if none exists. This app's security model never logs a
 * user in without a verified email; OAuth must not be an exception to that.
 *
 * <p>Note: when GitHub DOES expose a public {@code email} on {@code /user}, it is trustworthy as
 * verified — GitHub only allows a verified address to be set as the public profile email.
 */
@Component
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private static final Logger log = LoggerFactory.getLogger(GitHubOAuth2UserService.class);

  static final String EMAILS_URL = "https://api.github.com/user/emails";

  private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
  private final RestClient restClient;

  public GitHubOAuth2UserService() {
    this(RestClient.create());
  }

  // Package-private: lets tests substitute a RestClient backed by MockRestServiceServer.
  GitHubOAuth2UserService(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User user = delegate.loadUser(userRequest);
    if (!"github".equals(userRequest.getClientRegistration().getRegistrationId())) {
      return user;
    }
    if (user.getAttribute("email") != null) {
      log.info("GitHub OAuth: public profile email used directly, no /user/emails fallback needed");
      return user;
    }

    String verifiedEmail = fetchVerifiedPrimaryEmail(userRequest.getAccessToken().getTokenValue());
    if (verifiedEmail == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuthErrorCodes.UNVERIFIED_EMAIL),
          "GitHub account has no verified email");
    }
    log.info("GitHub OAuth: public email absent, resolved via /user/emails fallback");

    Map<String, Object> attributes = new LinkedHashMap<>(user.getAttributes());
    attributes.put("email", verifiedEmail);
    String nameAttributeKey =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
    return new DefaultOAuth2User(user.getAuthorities(), attributes, nameAttributeKey);
  }

  private String fetchVerifiedPrimaryEmail(String accessToken) {
    List<GitHubEmail> emails;
    try {
      emails =
          restClient
              .get()
              .uri(EMAILS_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
              .retrieve()
              .body(new ParameterizedTypeReference<List<GitHubEmail>>() {});
    } catch (RuntimeException e) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("github_email_lookup_failed"), e.getMessage(), e);
    }
    if (emails == null) {
      return null;
    }
    return emails.stream()
        .filter(GitHubEmail::verified)
        .sorted((a, b) -> Boolean.compare(b.primary(), a.primary()))
        .map(GitHubEmail::email)
        .findFirst()
        .orElse(null);
  }

  private record GitHubEmail(String email, boolean primary, boolean verified) {}
}
