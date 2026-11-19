package com.scorestream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be exactly 3 letters") String shortName,
        @NotBlank @Size(max = 100) String city
) {
}
