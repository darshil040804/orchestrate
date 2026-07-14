package com.orchestrate.api.error;

public class InvitationNotFoundException extends RuntimeException {

  public InvitationNotFoundException() {
    super("Pending invitation not found");
  }
}
