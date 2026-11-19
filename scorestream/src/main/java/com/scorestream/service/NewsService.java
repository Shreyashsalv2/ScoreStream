package com.scorestream.service;

import com.scorestream.domain.NewsArticle;
import com.scorestream.domain.Team;
import com.scorestream.dto.CreateNewsRequest;
import com.scorestream.dto.NewsResponse;
import com.scorestream.dto.PageResponse;
import com.scorestream.repository.NewsArticleRepository;
import com.scorestream.web.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository news;
    private final TeamService teamService;
    private final Clock clock;

    public NewsService(NewsArticleRepository news, TeamService teamService, Clock clock) {
        this.news = news;
        this.teamService = teamService;
        this.clock = clock;
    }

    public PageResponse<NewsResponse> list(Long teamId, Pageable pageable) {
        if (teamId != null) {
            teamService.find(teamId);
            return PageResponse.from(news.findByTeam(teamId, pageable).map(NewsResponse::from));
        }
        return PageResponse.from(news.findAll(pageable).map(NewsResponse::from));
    }

    public NewsResponse get(Long id) {
        return NewsResponse.from(find(id));
    }

    @Transactional
    public NewsResponse create(CreateNewsRequest request) {
        Set<Team> teams = new LinkedHashSet<>();
        for (Long teamId : request.teamIds()) {
            teams.add(teamService.find(teamId));
        }
        NewsArticle article = new NewsArticle(request.title().trim(), request.content().trim(),
                LocalDateTime.now(clock), teams);
        return NewsResponse.from(news.save(article));
    }

    @Transactional
    public void delete(Long id) {
        news.delete(find(id));
    }

    private NewsArticle find(Long id) {
        return news.findById(id).orElseThrow(() -> new NotFoundException("News article", id));
    }
}
