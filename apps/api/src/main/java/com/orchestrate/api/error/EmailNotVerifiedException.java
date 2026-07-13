package com.orchestrate.api.error;

public class EmailNotVerifiedException extends RuntimeException {

  public EmailNotVerifiedException() {
    super("Email address has not been verified");
  }
}
