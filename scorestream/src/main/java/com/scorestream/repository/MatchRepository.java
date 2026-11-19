package com.scorestream.repository;

import com.scorestream.domain.Match;
import com.scorestream.domain.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Matches filtered by status and/or team. A null filter means "any". Uses the
     * status and team indexes on the matches table.
     */
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    @Query("""
            select m from Match m
            where (:status is null or m.status = :status)
              and (:teamId is null or m.homeTeam.id = :teamId or m.awayTeam.id = :teamId)
            """)
    Page<Match> search(@Param("status") MatchStatus status, @Param("teamId") Long teamId, Pageable pageable);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    List<Match> findByStatusOrderByKickoffAsc(MatchStatus status);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    Optional<Match> findWithTeamsById(Long id);
}
