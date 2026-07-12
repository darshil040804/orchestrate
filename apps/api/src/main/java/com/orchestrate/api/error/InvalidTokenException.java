package com.orchestrate.api.error;

/** Raised for a malformed, expired, already-used, or otherwise unusable opaque token. */
public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException(String message) {
    super(message);
  }
}
