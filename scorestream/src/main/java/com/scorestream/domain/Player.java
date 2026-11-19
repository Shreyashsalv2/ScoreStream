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

@Entity
@Table(name = "players", indexes = @Index(name = "idx_players_team", columnList = "team_id"))
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    @Column(name = "shirt_number", nullable = false)
    private int shirtNumber;

    /** Many players belong to one team. LAZY: the team is only loaded when it's needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    protected Player() {
        // for JPA
    }

    public Player(String name, Position position, int shirtNumber, Team team) {
        this.name = name;
        this.position = position;
        this.shirtNumber = shirtNumber;
        this.team = team;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public int getShirtNumber() {
        return shirtNumber;
    }

    public Team getTeam() {
        return team;
    }
}
