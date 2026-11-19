package com.scorestream.dto;

import com.scorestream.domain.Team;

import java.io.Serializable;

/** The short form of a team used inside other responses. */
public record TeamSummary(Long id, String name, String shortName) implements Serializable {

    public static TeamSummary from(Team team) {
        return new TeamSummary(team.getId(), team.getName(), team.getShortName());
    }
}
