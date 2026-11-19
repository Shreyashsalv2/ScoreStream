package com.scorestream.service;

import com.scorestream.domain.Match;
import com.scorestream.domain.MatchStatus;
import com.scorestream.domain.Player;
import com.scorestream.domain.PlayerStat;
import com.scorestream.domain.Position;
import com.scorestream.domain.Team;
import com.scorestream.dto.CreateMatchRequest;
import com.scorestream.dto.MatchResponse;
import com.scorestream.dto.PlayerStatResponse;
import com.scorestream.dto.RecordStatRequest;
import com.scorestream.dto.UpdateScoreRequest;
import com.scorestream.repository.MatchRepository;
import com.scorestream.repository.PlayerStatRepository;
import com.scorestream.web.BadRequestException;
import com.scorestream.web.ConflictException;
import com.scorestream.web.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests: MatchService on its own, with its dependencies replaced by Mockito mocks.
 * No Spring context and no database, so these run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matches;
    @Mock
    private PlayerStatRepository stats;
    @Mock
    private TeamService teamService;
    @Mock
    private PlayerService playerService;

    @InjectMocks
    private MatchService matchService;

    private Team home;
    private Team away;

    @BeforeEach
    void setUp() {
        home = team(1L, "Mumbai Mariners", "MUM");
        away = team(2L, "Delhi Dynamos", "DEL");
    }

    // ---------- create ----------

    @Test
    void create_rejectsATeamPlayingItself() {
        var request = new CreateMatchRequest(1L, 1L, "Arena", LocalDateTime.now());

        assertThatThrownBy(() -> matchService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can't play itself");
        verifyNoInteractions(matches);
    }

    @Test
    void create_failsWhenATeamDoesNotExist() {
        when(teamService.find(1L)).thenReturn(home);
        when(teamService.find(99L)).thenThrow(new NotFoundException("Team", 99L));

        assertThatThrownBy(() -> matchService.create(new CreateMatchRequest(1L, 99L, "Arena", LocalDateTime.now())))
                .isInstanceOf(NotFoundException.class);
        verify(matches, never()).save(any());
    }

    @Test
    void create_savesAScheduledMatchAtZeroZero() {
        when(teamService.find(1L)).thenReturn(home);
        when(teamService.find(2L)).thenReturn(away);
        when(matches.save(any(Match.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        MatchResponse result = matchService.create(new CreateMatchRequest(1L, 2L, "  Arena  ", LocalDateTime.now()));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(result.homeScore()).isZero();
        assertThat(result.venue()).isEqualTo("Arena");
    }

    // ---------- updateScore ----------

    @Test
    void updateScore_setsScoreStatusAndMinute() {
        Match match = match(MatchStatus.SCHEDULED);
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match));

        MatchResponse result = matchService.updateScore(10L, new UpdateScoreRequest(1, 0, MatchStatus.LIVE, 12));

        assertThat(result.homeScore()).isEqualTo(1);
        assertThat(result.awayScore()).isZero();
        assertThat(result.status()).isEqualTo(MatchStatus.LIVE);
        assertThat(result.minute()).isEqualTo(12);
    }

    @Test
    void updateScore_clearsTheMinuteWhenTheMatchFinishes() {
        Match match = match(MatchStatus.LIVE);
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match));

        MatchResponse result = matchService.updateScore(10L, new UpdateScoreRequest(2, 1, MatchStatus.FINISHED, 90));

        assertThat(result.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(result.minute()).isNull();
    }

    @Test
    void updateScore_rejectsChangesToAFinishedMatch() {
        Match match = match(MatchStatus.FINISHED);
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.updateScore(10L, new UpdateScoreRequest(5, 5, MatchStatus.LIVE, 1)))
                .isInstanceOf(ConflictException.class);
        assertThat(match.getHomeScore()).isZero();
    }

    @Test
    void updateScore_rejectsMovingALiveMatchBackToScheduled() {
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.LIVE)));

        assertThatThrownBy(() -> matchService.updateScore(10L, new UpdateScoreRequest(0, 0, MatchStatus.SCHEDULED, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateScore_rejectsAScoreOnAScheduledMatch() {
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));

        assertThatThrownBy(() -> matchService.updateScore(10L, new UpdateScoreRequest(1, 0, MatchStatus.SCHEDULED, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateScore_failsForAnUnknownMatch() {
        when(matches.findWithTeamsById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.updateScore(404L, new UpdateScoreRequest(1, 0, MatchStatus.LIVE, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Match 404 not found");
    }

    // ---------- recordStat ----------

    @Test
    void recordStat_rejectsAMatchThatHasNotStarted() {
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));

        assertThatThrownBy(() -> matchService.recordStat(10L, new RecordStatRequest(5L, 1, 0, 90)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hasn't started");
        verifyNoInteractions(stats);
    }

    @Test
    void recordStat_rejectsAPlayerFromAnotherTeam() {
        Team other = team(3L, "Kolkata Knights", "KOL");
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.LIVE)));
        when(playerService.find(5L)).thenReturn(player(5L, other));

        assertThatThrownBy(() -> matchService.recordStat(10L, new RecordStatRequest(5L, 1, 0, 90)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't play for either team");
    }

    @Test
    void recordStat_rejectsASecondStatLineForTheSamePlayer() {
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.LIVE)));
        when(playerService.find(5L)).thenReturn(player(5L, home));
        when(stats.existsByMatchIdAndPlayerId(10L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> matchService.recordStat(10L, new RecordStatRequest(5L, 1, 0, 90)))
                .isInstanceOf(ConflictException.class);
        verify(stats, never()).save(any());
    }

    @Test
    void recordStat_savesTheStatLine() {
        when(matches.findWithTeamsById(10L)).thenReturn(Optional.of(match(MatchStatus.FINISHED)));
        when(playerService.find(5L)).thenReturn(player(5L, away));
        when(stats.existsByMatchIdAndPlayerId(10L, 5L)).thenReturn(false);
        when(stats.save(any(PlayerStat.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerStatResponse result = matchService.recordStat(10L, new RecordStatRequest(5L, 2, 1, 90));

        assertThat(result.matchId()).isEqualTo(10L);
        assertThat(result.fixture()).isEqualTo("MUM vs DEL");
        assertThat(result.goals()).isEqualTo(2);
        assertThat(result.assists()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private Match match(MatchStatus status) {
        Match match = withId(new Match(home, away, "Arena", LocalDateTime.of(2026, 11, 20, 19, 0)), 10L);
        if (status != MatchStatus.SCHEDULED) {
            match.updateScore(0, 0, status, status == MatchStatus.LIVE ? 1 : null);
        }
        return match;
    }

    private static Team team(Long id, String name, String shortName) {
        return withId(new Team(name, shortName, "City"), id);
    }

    private static Player player(Long id, Team team) {
        return withId(new Player("Player " + id, Position.FORWARD, 9, team), id);
    }

    /** Entities get their id from the database; in unit tests we set it directly. */
    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
