package com.scorestream.service;

import com.scorestream.domain.Player;
import com.scorestream.domain.Team;
import com.scorestream.dto.CreatePlayerRequest;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerResponse;
import com.scorestream.dto.PlayerStatResponse;
import com.scorestream.repository.PlayerRepository;
import com.scorestream.repository.PlayerStatRepository;
import com.scorestream.web.ConflictException;
import com.scorestream.web.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository players;
    private final PlayerStatRepository stats;
    private final TeamService teamService;

    public PlayerService(PlayerRepository players, PlayerStatRepository stats, TeamService teamService) {
        this.players = players;
        this.stats = stats;
        this.teamService = teamService;
    }

    /** All players, or only one team's players when teamId is given. */
    public PageResponse<PlayerResponse> list(Long teamId, Pageable pageable) {
        if (teamId != null) {
            return teamService.players(teamId, pageable);
        }
        return PageResponse.from(players.findAllBy(pageable).map(PlayerResponse::from));
    }

    public PlayerResponse get(Long id) {
        return PlayerResponse.from(find(id));
    }

    public List<PlayerStatResponse> stats(Long playerId) {
        find(playerId);
        return stats.findByPlayerIdOrderByMatchKickoffDesc(playerId).stream().map(PlayerStatResponse::from).toList();
    }

    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        Team team = teamService.find(request.teamId());
        if (players.existsByTeamIdAndShirtNumber(team.getId(), request.shirtNumber())) {
            throw new ConflictException(team.getName() + " already has a player with number " + request.shirtNumber());
        }
        Player saved = players.save(new Player(request.name().trim(), request.position(), request.shirtNumber(), team));
        return PlayerResponse.from(saved);
    }

    Player find(Long id) {
        return players.findWithTeamById(id).orElseThrow(() -> new NotFoundException("Player", id));
    }
}
