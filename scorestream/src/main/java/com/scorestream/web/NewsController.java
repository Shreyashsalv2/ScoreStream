package com.scorestream.web;

import com.scorestream.dto.CreateNewsRequest;
import com.scorestream.dto.NewsResponse;
import com.scorestream.dto.PageResponse;
import com.scorestream.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@Tag(name = "News")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    @Operation(summary = "Latest news, optionally only about one team (paged)")
    public PageResponse<NewsResponse> list(
            @RequestParam(required = false) Long teamId,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return newsService.list(teamId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one article")
    public NewsResponse get(@PathVariable Long id) {
        return newsService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish an article tagged with teams (admin)")
    public NewsResponse create(@Valid @RequestBody CreateNewsRequest request) {
        return newsService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete an article (admin)")
    public void delete(@PathVariable Long id) {
        newsService.delete(id);
    }
}
