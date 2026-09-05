package com.kasagichat.api.security.controller.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record UserRegistrationRequest(
    @NotNull @NotBlank @Size(max = 50) String displayName,
    @NotNull Set<Long> agreedTermsIds
) {
}
