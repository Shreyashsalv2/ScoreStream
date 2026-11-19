package com.scorestream.repository;

import com.scorestream.domain.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    /** Articles tagged with a team, through the news_teams join table. */
    @Query(value = "select n from NewsArticle n join n.teams t where t.id = :teamId",
            countQuery = "select count(n) from NewsArticle n join n.teams t where t.id = :teamId")
    Page<NewsArticle> findByTeam(@Param("teamId") Long teamId, Pageable pageable);
}
