package com.scorestream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Three-letter code shown on scoreboards, e.g. "MUM". */
    @Column(name = "short_name", nullable = false, unique = true, length = 3)
    private String shortName;

    @Column(nullable = false, length = 100)
    private String city;

    /** One team has many players (One-to-Many). The Player side owns the foreign key. */
    @OneToMany(mappedBy = "team")
    private List<Player> players = new ArrayList<>();

    protected Team() {
        // for JPA
    }

    public Team(String name, String shortName, String city) {
        this.name = name;
        this.shortName = shortName;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCity() {
        return city;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
