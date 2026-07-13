package com.orchestrate.api.error;

public class MembershipNotFoundException extends RuntimeException {

  public MembershipNotFoundException() {
    super("Membership not found");
  }
}
