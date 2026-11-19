package com.scorestream.web;

import com.scorestream.domain.MatchStatus;
import com.scorestream.dto.CreateMatchRequest;
import com.scorestream.dto.MatchResponse;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerStatResponse;
import com.scorestream.dto.RecordStatRequest;
import com.scorestream.dto.UpdateScoreRequest;
import com.scorestream.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    @Operation(summary = "Search matches by status and/or team (paged, newest kickoff first)")
    public PageResponse<MatchResponse> search(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) Long teamId,
            @PageableDefault(size = 20, sort = "kickoff", direction = Sort.Direction.DESC) Pageable pageable) {
        return matchService.search(status, teamId, pageable);
    }

    @GetMapping("/live")
    @Operation(summary = "Matches being played right now (cached)")
    public List<MatchResponse> live() {
        return matchService.liveMatches();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one match (cached)")
    public MatchResponse get(@PathVariable Long id) {
        return matchService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Schedule a match (admin)")
    public MatchResponse create(@Valid @RequestBody CreateMatchRequest request) {
        return matchService.create(request);
    }

    @PatchMapping("/{id}/score")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update the score and status (admin)")
    public MatchResponse updateScore(@PathVariable Long id, @Valid @RequestBody UpdateScoreRequest request) {
        return matchService.updateScore(id, request);
    }

    @PostMapping("/{id}/stats")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Record a player's stats for this match (admin)")
    public PlayerStatResponse recordStat(@PathVariable Long id, @Valid @RequestBody RecordStatRequest request) {
        return matchService.recordStat(id, request);
    }
}
