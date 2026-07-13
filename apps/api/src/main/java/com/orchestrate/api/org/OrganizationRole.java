package com.orchestrate.api.org;

/**
 * Role within a single organization. Ordering (via {@link #isAtLeast}) is OWNER &gt; ADMIN &gt;
 * {MEMBER, APPROVER}. Finer owner-vs-admin-tier distinctions and the last-owner invariant are
 * explicit business logic in {@link OrgService}, not encoded here — this enum only knows relative
 * rank.
 */
public enum OrganizationRole {
  OWNER(3),
  ADMIN(2),
  MEMBER(1),
  APPROVER(1);

  private final int rank;

  OrganizationRole(int rank) {
    this.rank = rank;
  }

  /** True if this role's privilege is &gt;= {@code other}'s. MEMBER and APPROVER are equal rank. */
  public boolean isAtLeast(OrganizationRole other) {
    return this.rank >= other.rank;
  }
}
