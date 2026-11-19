package com.scorestream.service;

import com.scorestream.domain.Team;
import com.scorestream.dto.CreateTeamRequest;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerResponse;
import com.scorestream.dto.TeamResponse;
import com.scorestream.repository.PlayerRepository;
import com.scorestream.repository.TeamRepository;
import com.scorestream.web.ConflictException;
import com.scorestream.web.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teams;
    private final PlayerRepository players;

    public TeamService(TeamRepository teams, PlayerRepository players) {
        this.teams = teams;
        this.players = players;
    }

    public PageResponse<TeamResponse> list(Pageable pageable) {
        return PageResponse.from(teams.findAll(pageable).map(TeamResponse::from));
    }

    public TeamResponse get(Long id) {
        return TeamResponse.from(find(id));
    }

    public PageResponse<PlayerResponse> players(Long teamId, Pageable pageable) {
        find(teamId); // 404 if the team doesn't exist, instead of an empty list
        return PageResponse.from(players.findByTeamId(teamId, pageable).map(PlayerResponse::from));
    }

    @Transactional
    public TeamResponse create(CreateTeamRequest request) {
        String name = request.name().trim();
        String shortName = request.shortName().toUpperCase(Locale.ROOT);
        if (teams.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A team named '" + name + "' already exists");
        }
        if (teams.existsByShortNameIgnoreCase(shortName)) {
            throw new ConflictException("Short name '" + shortName + "' is already used");
        }
        return TeamResponse.from(teams.save(new Team(name, shortName, request.city().trim())));
    }

    Team find(Long id) {
        return teams.findById(id).orElseThrow(() -> new NotFoundException("Team", id));
    }
}
