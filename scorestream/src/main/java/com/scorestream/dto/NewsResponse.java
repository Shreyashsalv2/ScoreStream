package com.scorestream.dto;

import com.scorestream.domain.NewsArticle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record NewsResponse(Long id, String title, String content, LocalDateTime publishedAt, List<TeamSummary> teams)
        implements Serializable {

    public static NewsResponse from(NewsArticle article) {
        List<TeamSummary> teams = article.getTeams().stream()
                .map(TeamSummary::from)
                .sorted(Comparator.comparing(TeamSummary::name))
                .toList();
        return new NewsResponse(article.getId(), article.getTitle(), article.getContent(), article.getPublishedAt(),
                teams);
    }
}
