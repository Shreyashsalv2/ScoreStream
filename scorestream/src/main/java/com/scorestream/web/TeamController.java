package com.scorestream.web;

import com.scorestream.dto.CreateTeamRequest;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerResponse;
import com.scorestream.dto.TeamResponse;
import com.scorestream.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @Operation(summary = "List teams (paged; e.g. ?page=0&size=20&sort=name)")
    public PageResponse<TeamResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return teamService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one team")
    public TeamResponse get(@PathVariable Long id) {
        return teamService.get(id);
    }

    @GetMapping("/{id}/players")
    @Operation(summary = "A team's squad (paged)")
    public PageResponse<PlayerResponse> players(@PathVariable Long id,
                                                @PageableDefault(size = 30, sort = "shirtNumber") Pageable pageable) {
        return teamService.players(id, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a team (admin)")
    public TeamResponse create(@Valid @RequestBody CreateTeamRequest request) {
        return teamService.create(request);
    }
}
