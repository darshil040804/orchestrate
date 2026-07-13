package com.orchestrate.api.org;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrganizationMembershipRepository
    extends JpaRepository<OrganizationMembership, UUID> {

  Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  List<OrganizationMembership> findByOrganizationId(UUID organizationId);

  List<OrganizationMembership> findByUserId(UUID userId);

  /**
   * Pessimistic write-lock on every OWNER-role row for this org. Must be called (and its result
   * used, not a fresh unlocked count) before deciding whether an owner-removing/demoting action
   * would leave zero owners — see {@link OrgService}'s {@code requireNotLastOwner} for why an
   * unlocked COUNT alone is racy under concurrent requests targeting different owners of the same
   * org.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<OrganizationMembership> findByOrganizationIdAndRole(
      UUID organizationId, OrganizationRole role);
}
