package com.scorestream.dto;

import com.scorestream.domain.Position;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Position position,
        @NotNull @Min(1) @Max(99) Integer shirtNumber,
        @NotNull Long teamId
) {
}
