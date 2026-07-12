package com.orchestrate.api.auth.dto;

import com.orchestrate.api.user.User;
import java.util.UUID;

public record UserResponse(UUID id, String email, boolean emailVerified) {

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.isEmailVerified());
  }
}
