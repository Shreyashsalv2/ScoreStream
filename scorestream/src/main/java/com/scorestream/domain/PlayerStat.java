package com.scorestream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One player's numbers in one match. A player can have only one stat line per match. */
@Entity
@Table(name = "player_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_stat_match_player", columnNames = {"match_id", "player_id"}))
public class PlayerStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int goals;

    @Column(nullable = false)
    private int assists;

    @Column(name = "minutes_played", nullable = false)
    private int minutesPlayed;

    protected PlayerStat() {
        // for JPA
    }

    public PlayerStat(Match match, Player player, int goals, int assists, int minutesPlayed) {
        this.match = match;
        this.player = player;
        this.goals = goals;
        this.assists = assists;
        this.minutesPlayed = minutesPlayed;
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public Player getPlayer() {
        return player;
    }

    public int getGoals() {
        return goals;
    }

    public int getAssists() {
        return assists;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }
}
