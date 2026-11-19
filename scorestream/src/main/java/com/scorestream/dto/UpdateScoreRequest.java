package com.scorestream.dto;

import com.scorestream.domain.MatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateScoreRequest(
        @NotNull @Min(0) @Max(99) Integer homeScore,
        @NotNull @Min(0) @Max(99) Integer awayScore,
        @NotNull MatchStatus status,
        @Min(0) @Max(130) Integer minute
) {
}
