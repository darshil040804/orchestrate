package com.orchestrate.api.error;

public class MembershipAlreadyExistsException extends RuntimeException {

  public MembershipAlreadyExistsException() {
    super("User is already a member of this organization");
  }
}
