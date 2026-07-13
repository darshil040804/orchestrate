package com.orchestrate.api.error;

public class OwnerActionRequiredException extends RuntimeException {

  public OwnerActionRequiredException() {
    super("Only an organization owner can perform this action");
  }
}
