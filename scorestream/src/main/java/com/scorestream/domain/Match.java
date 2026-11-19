package com.scorestream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A football match. Indexed on status (the "live matches" query) and kickoff time (sorting),
 * because those are the columns the busiest endpoints filter and sort on.
 */
@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_matches_status", columnList = "status"),
        @Index(name = "idx_matches_kickoff", columnList = "kickoff"),
        @Index(name = "idx_matches_home_team", columnList = "home_team_id"),
        @Index(name = "idx_matches_away_team", columnList = "away_team_id")
})
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(nullable = false, length = 100)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime kickoff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    /** Minute of play while the match is live; null otherwise. */
    @Column(name = "match_minute")
    private Integer minute;

    protected Match() {
        // for JPA
    }

    public Match(Team homeTeam, Team awayTeam, String venue, LocalDateTime kickoff) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.venue = venue;
        this.kickoff = kickoff;
    }

    /** Updates the score. Business rules (e.g. a finished match can't change) live in MatchService. */
    public void updateScore(int homeScore, int awayScore, MatchStatus status, Integer minute) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = status;
        this.minute = status == MatchStatus.LIVE ? minute : null;
    }

    public Long getId() {
        return id;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public String getVenue() {
        return venue;
    }

    public LocalDateTime getKickoff() {
        return kickoff;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public Integer getMinute() {
        return minute;
    }
}
