package com.orchestrate.api.auth.token;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface EmailVerificationTokenRepository
    extends CrudRepository<EmailVerificationToken, UUID> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
