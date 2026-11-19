package com.scorestream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "may contain only letters, digits and _") String username,
        // BCrypt only uses the first 72 bytes of a password, so longer ones are rejected.
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
