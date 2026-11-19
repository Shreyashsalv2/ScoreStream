package com.scorestream.repository;

import com.scorestream.domain.PlayerStat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerStatRepository extends JpaRepository<PlayerStat, Long> {

    /** A player's stat lines, newest match first, with the match and both teams loaded. */
    @EntityGraph(attributePaths = {"match", "match.homeTeam", "match.awayTeam"})
    List<PlayerStat> findByPlayerIdOrderByMatchKickoffDesc(Long playerId);

    boolean existsByMatchIdAndPlayerId(Long matchId, Long playerId);
}
