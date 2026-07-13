package com.orchestrate.api.auth.dto;

import com.orchestrate.api.auth.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirm(
    @NotBlank String token, @NotBlank @Size(min = 8) @MaxUtf8Bytes(72) String newPassword) {}
