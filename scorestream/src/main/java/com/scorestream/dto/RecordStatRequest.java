package com.scorestream.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordStatRequest(
        @NotNull Long playerId,
        @NotNull @Min(0) @Max(20) Integer goals,
        @NotNull @Min(0) @Max(20) Integer assists,
        @NotNull @Min(0) @Max(130) Integer minutesPlayed
) {
}
