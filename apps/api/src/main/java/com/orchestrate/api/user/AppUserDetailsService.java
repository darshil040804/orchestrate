package com.orchestrate.api.user;

import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users by email for Spring Security's DaoAuthenticationProvider (used by the login flow).
 *
 * <p>Note: an OAuth user (Phase 1b) will have a null password hash. We surface an empty password to
 * Spring rather than null so DaoAuthenticationProvider fails cleanly with a bad-credentials error
 * if someone tries password login against an OAuth-only account.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public AppUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("No user for email: " + email));

    return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
        .password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
        .authorities(List.of())
        .build();
  }
}
