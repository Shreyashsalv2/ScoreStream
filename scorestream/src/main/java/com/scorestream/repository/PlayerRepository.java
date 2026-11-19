package com.scorestream.repository;

import com.scorestream.domain.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /** @EntityGraph loads each player's team in the same query (avoids the N+1 problem). */
    @EntityGraph(attributePaths = "team")
    Page<Player> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<Player> findByTeamId(Long teamId, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Optional<Player> findWithTeamById(Long id);

    boolean existsByTeamIdAndShirtNumber(Long teamId, int shirtNumber);
}
