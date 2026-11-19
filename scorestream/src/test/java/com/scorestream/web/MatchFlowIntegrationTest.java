package com.scorestream.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.SimpleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A match from kickoff to final whistle, checking scores, the live feed, the cache and stats. */
class MatchFlowIntegrationTest extends IntegrationTestBase {

    private String admin;
    private long home;
    private long away;
    private long matchId;

    @BeforeEach
    void createMatch() throws Exception {
        admin = adminToken();
        home = createTeam(admin, "Mumbai Mariners", "MUM");
        away = createTeam(admin, "Delhi Dynamos", "DEL");
        matchId = createMatch(admin, home, away);
    }

    private void updateScore(int homeScore, int awayScore, String status, Integer minute, int expected)
            throws Exception {
        String json = "{\"homeScore\":" + homeScore + ",\"awayScore\":" + awayScore + ",\"status\":\"" + status
                + "\",\"minute\":" + minute + "}";
        int actual = send(patch("/api/matches/" + matchId + "/score"), admin, json).getResponse().getStatus();
        assertThat(actual).as("PATCH score status").isEqualTo(expected);
    }

    @Test
    void theLiveFeedFollowsTheMatchAndTheCacheIsClearedOnEveryUpdate() throws Exception {
        mvc.perform(get("/api/matches/live")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        assertThat(cacheManager.getCache("liveMatches").get(SimpleKey.EMPTY))
                .as("the live list is cached after a read").isNotNull();

        updateScore(1, 0, "LIVE", 12, 200);
        assertThat(cacheManager.getCache("liveMatches").get(SimpleKey.EMPTY))
                .as("a score update clears the cached live list").isNull();

        mvc.perform(get("/api/matches/live"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].homeTeam.shortName").value("MUM"))
                .andExpect(jsonPath("$[0].homeScore").value(1))
                .andExpect(jsonPath("$[0].minute").value(12));

        updateScore(2, 1, "LIVE", 70, 200);
        mvc.perform(get("/api/matches/live")).andExpect(jsonPath("$[0].homeScore").value(2));
        mvc.perform(get("/api/matches/" + matchId)).andExpect(jsonPath("$.awayScore").value(1));

        updateScore(2, 1, "FINISHED", null, 200);
        mvc.perform(get("/api/matches/live")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/matches/" + matchId))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.minute").doesNotExist());
    }

    @Test
    void aSingleMatchIsCachedAndRefreshedAfterAnUpdate() throws Exception {
        mvc.perform(get("/api/matches/" + matchId)).andExpect(jsonPath("$.homeScore").value(0));
        assertThat(cacheManager.getCache("match").get(matchId)).isNotNull();

        updateScore(3, 0, "LIVE", 50, 200);

        assertThat(cacheManager.getCache("match").get(matchId)).isNull();
        mvc.perform(get("/api/matches/" + matchId)).andExpect(jsonPath("$.homeScore").value(3));
    }

    @Test
    void businessRulesAreEnforced() throws Exception {
        updateScore(1, 0, "SCHEDULED", null, 400);   // a scheduled match can't have goals
        updateScore(0, 0, "LIVE", 1, 200);
        updateScore(0, 0, "SCHEDULED", null, 409);   // live can't go back to scheduled
        updateScore(1, 1, "FINISHED", null, 200);
        updateScore(2, 1, "FINISHED", null, 409);    // finished is final
        updateScore(-1, 0, "LIVE", 5, 400);          // validation: no negative scores

        int sameTeam = send(post("/api/matches"), admin,
                "{\"homeTeamId\":" + home + ",\"awayTeamId\":" + home
                        + ",\"venue\":\"X\",\"kickoff\":\"2026-12-01T19:30:00\"}").getResponse().getStatus();
        assertThat(sameTeam).isEqualTo(400);
    }

    @Test
    void playerStatsAreRecordedAndListed() throws Exception {
        long striker = createPlayer(admin, home, "Rohan Mehta", 9);
        long outsider = createPlayer(admin, createTeam(admin, "Kolkata Knights", "KOL"), "Sameer Das", 6);
        String stat = "{\"playerId\":" + striker + ",\"goals\":2,\"assists\":1,\"minutesPlayed\":90}";

        assertThat(send(post("/api/matches/" + matchId + "/stats"), admin, stat).getResponse().getStatus())
                .as("no stats before kickoff").isEqualTo(400);

        updateScore(2, 0, "LIVE", 80, 200);
        assertThat(send(post("/api/matches/" + matchId + "/stats"), admin, stat).getResponse().getStatus())
                .isEqualTo(201);
        assertThat(send(post("/api/matches/" + matchId + "/stats"), admin, stat).getResponse().getStatus())
                .as("one stat line per player per match").isEqualTo(409);
        String outsiderStat = "{\"playerId\":" + outsider + ",\"goals\":0,\"assists\":0,\"minutesPlayed\":90}";
        assertThat(send(post("/api/matches/" + matchId + "/stats"), admin, outsiderStat).getResponse().getStatus())
                .as("player must be in one of the two teams").isEqualTo(400);

        mvc.perform(get("/api/players/" + striker + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fixture").value("MUM vs DEL"))
                .andExpect(jsonPath("$[0].goals").value(2));
    }

    @Test
    void matchesCanBeFilteredByStatusAndTeam() throws Exception {
        long third = createTeam(admin, "Kolkata Knights", "KOL");
        createMatch(admin, third, away);
        updateScore(0, 0, "LIVE", 1, 200);

        mvc.perform(get("/api/matches").param("status", "LIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchId));
        mvc.perform(get("/api/matches").param("teamId", String.valueOf(away)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/matches").param("teamId", String.valueOf(home)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
