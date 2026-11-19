package com.scorestream.service;

import com.scorestream.config.CacheConfig;
import com.scorestream.domain.Match;
import com.scorestream.domain.MatchStatus;
import com.scorestream.domain.Player;
import com.scorestream.domain.PlayerStat;
import com.scorestream.domain.Team;
import com.scorestream.dto.CreateMatchRequest;
import com.scorestream.dto.MatchResponse;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerStatResponse;
import com.scorestream.dto.RecordStatRequest;
import com.scorestream.dto.UpdateScoreRequest;
import com.scorestream.repository.MatchRepository;
import com.scorestream.repository.PlayerStatRepository;
import com.scorestream.web.BadRequestException;
import com.scorestream.web.ConflictException;
import com.scorestream.web.NotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matches;
    private final PlayerStatRepository stats;
    private final TeamService teamService;
    private final PlayerService playerService;

    public MatchService(MatchRepository matches, PlayerStatRepository stats, TeamService teamService,
                        PlayerService playerService) {
        this.matches = matches;
        this.stats = stats;
        this.teamService = teamService;
        this.playerService = playerService;
    }

    public PageResponse<MatchResponse> search(MatchStatus status, Long teamId, Pageable pageable) {
        return PageResponse.from(matches.search(status, teamId, pageable).map(MatchResponse::from));
    }

    /**
     * The busiest read in the app: every open scoreboard polls it. Cached in Redis for a few
     * seconds (spring.cache.redis.time-to-live) and cleared whenever a score changes.
     */
    @Cacheable(CacheConfig.LIVE_MATCHES)
    public List<MatchResponse> liveMatches() {
        return matches.findByStatusOrderByKickoffAsc(MatchStatus.LIVE).stream().map(MatchResponse::from).toList();
    }

    @Cacheable(cacheNames = CacheConfig.MATCH, key = "#id")
    public MatchResponse get(Long id) {
        return MatchResponse.from(find(id));
    }

    @Transactional
    public MatchResponse create(CreateMatchRequest request) {
        if (Objects.equals(request.homeTeamId(), request.awayTeamId())) {
            throw new BadRequestException("A team can't play itself");
        }
        Team home = teamService.find(request.homeTeamId());
        Team away = teamService.find(request.awayTeamId());
        Match saved = matches.save(new Match(home, away, request.venue().trim(), request.kickoff()));
        return MatchResponse.from(saved);
    }

    /** Updates the score and clears both caches, so the next read shows the new score. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.LIVE_MATCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.MATCH, key = "#id")
    })
    public MatchResponse updateScore(Long id, UpdateScoreRequest request) {
        Match match = find(id);
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new ConflictException("Match " + id + " is finished; its score can't change");
        }
        if (request.status() == MatchStatus.SCHEDULED) {
            if (match.getStatus() == MatchStatus.LIVE) {
                throw new ConflictException("A live match can't go back to scheduled");
            }
            if (request.homeScore() != 0 || request.awayScore() != 0) {
                throw new BadRequestException("A scheduled match must have a 0-0 score");
            }
        }
        match.updateScore(request.homeScore(), request.awayScore(), request.status(), request.minute());
        return MatchResponse.from(match);
    }

    @Transactional
    public PlayerStatResponse recordStat(Long matchId, RecordStatRequest request) {
        Match match = find(matchId);
        if (match.getStatus() == MatchStatus.SCHEDULED) {
            throw new BadRequestException("Match " + matchId + " hasn't started yet");
        }
        Player player = playerService.find(request.playerId());
        Long teamId = player.getTeam().getId();
        if (!teamId.equals(match.getHomeTeam().getId()) && !teamId.equals(match.getAwayTeam().getId())) {
            throw new BadRequestException(player.getName() + " doesn't play for either team in this match");
        }
        if (stats.existsByMatchIdAndPlayerId(matchId, player.getId())) {
            throw new ConflictException(player.getName() + " already has stats for match " + matchId);
        }
        PlayerStat saved = stats.save(new PlayerStat(match, player, request.goals(), request.assists(),
                request.minutesPlayed()));
        return PlayerStatResponse.from(saved);
    }

    private Match find(Long id) {
        return matches.findWithTeamsById(id).orElseThrow(() -> new NotFoundException("Match", id));
    }
}
