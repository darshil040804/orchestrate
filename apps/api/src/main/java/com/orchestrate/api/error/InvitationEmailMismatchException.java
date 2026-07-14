package com.orchestrate.api.error;

public class InvitationEmailMismatchException extends RuntimeException {

  public InvitationEmailMismatchException() {
    super("This invitation was sent to a different email address");
  }
}
