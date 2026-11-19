package com.scorestream.repository;

import com.scorestream.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByShortNameIgnoreCase(String shortName);
}
