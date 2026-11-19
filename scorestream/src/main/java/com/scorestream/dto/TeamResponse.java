package com.scorestream.dto;

import com.scorestream.domain.Team;

import java.io.Serializable;

public record TeamResponse(Long id, String name, String shortName, String city) implements Serializable {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getCity());
    }
}
