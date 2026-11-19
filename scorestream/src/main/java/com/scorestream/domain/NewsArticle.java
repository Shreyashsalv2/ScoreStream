package com.scorestream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "news_articles", indexes = @Index(name = "idx_news_published_at", columnList = "published_at"))
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    /**
     * Many-to-Many: one article can be about several teams, and one team appears in many
     * articles. JPA stores the pairs in the news_teams join table.
     */
    @ManyToMany
    @JoinTable(name = "news_teams",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id"))
    private Set<Team> teams = new HashSet<>();

    protected NewsArticle() {
        // for JPA
    }

    public NewsArticle(String title, String content, LocalDateTime publishedAt, Set<Team> teams) {
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.teams = new HashSet<>(teams);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Set<Team> getTeams() {
        return teams;
    }
}
