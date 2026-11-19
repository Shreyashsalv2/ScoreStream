package com.scorestream.web;

import com.scorestream.dto.CreatePlayerRequest;
import com.scorestream.dto.PageResponse;
import com.scorestream.dto.PlayerResponse;
import com.scorestream.dto.PlayerStatResponse;
import com.scorestream.service.PlayerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    @Operation(summary = "List players, optionally filtered by team (paged)")
    public PageResponse<PlayerResponse> list(@RequestParam(required = false) Long teamId,
                                             @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return playerService.list(teamId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one player")
    public PlayerResponse get(@PathVariable Long id) {
        return playerService.get(id);
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "A player's stats per match, newest first")
    public List<PlayerStatResponse> stats(@PathVariable Long id) {
        return playerService.stats(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a player to a team (admin)")
    public PlayerResponse create(@Valid @RequestBody CreatePlayerRequest request) {
        return playerService.create(request);
    }
}
