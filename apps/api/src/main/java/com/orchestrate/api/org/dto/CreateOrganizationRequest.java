package com.orchestrate.api.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(
            regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message =
                "Slug must be lowercase letters/digits, single hyphens, no leading/trailing hyphen")
        String slug) {}
