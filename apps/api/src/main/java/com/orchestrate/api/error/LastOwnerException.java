package com.orchestrate.api.error;

public class LastOwnerException extends RuntimeException {

  public LastOwnerException() {
    super("Organization must always have at least one owner");
  }
}
