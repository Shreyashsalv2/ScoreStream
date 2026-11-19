package com.scorestream.dto;

import com.scorestream.domain.Match;
import com.scorestream.domain.PlayerStat;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PlayerStatResponse(
        Long matchId,
        String fixture,
        LocalDateTime kickoff,
        int goals,
        int assists,
        int minutesPlayed
) implements Serializable {

    public static PlayerStatResponse from(PlayerStat stat) {
        Match m = stat.getMatch();
        String fixture = m.getHomeTeam().getShortName() + " vs " + m.getAwayTeam().getShortName();
        return new PlayerStatResponse(m.getId(), fixture, m.getKickoff(), stat.getGoals(), stat.getAssists(),
                stat.getMinutesPlayed());
    }
}
