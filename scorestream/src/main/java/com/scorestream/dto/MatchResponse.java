package com.scorestream.dto;

import com.scorestream.domain.Match;
import com.scorestream.domain.MatchStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Serializable because live matches are cached in Redis. */
public record MatchResponse(
        Long id,
        TeamSummary homeTeam,
        TeamSummary awayTeam,
        String venue,
        LocalDateTime kickoff,
        MatchStatus status,
        int homeScore,
        int awayScore,
        Integer minute
) implements Serializable {

    public static MatchResponse from(Match match) {
        return new MatchResponse(match.getId(), TeamSummary.from(match.getHomeTeam()),
                TeamSummary.from(match.getAwayTeam()), match.getVenue(), match.getKickoff(),
                match.getStatus(), match.getHomeScore(), match.getAwayScore(), match.getMinute());
    }
}
