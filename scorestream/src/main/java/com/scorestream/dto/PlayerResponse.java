package com.scorestream.dto;

import com.scorestream.domain.Player;
import com.scorestream.domain.Position;

import java.io.Serializable;

public record PlayerResponse(Long id, String name, Position position, int shirtNumber, TeamSummary team)
        implements Serializable {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(player.getId(), player.getName(), player.getPosition(),
                player.getShirtNumber(), TeamSummary.from(player.getTeam()));
    }
}
