package com.scorestream.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teams, players and news: pagination, filters and the news-to-teams many-to-many link. */
class CatalogIntegrationTest extends IntegrationTestBase {

    @Test
    void teamsArePaged() throws Exception {
        String admin = adminToken();
        createTeam(admin, "Alpha FC", "ALP");
        createTeam(admin, "Bravo FC", "BRA");
        createTeam(admin, "Charlie FC", "CHA");

        mvc.perform(get("/api/teams").param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Alpha FC"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/teams").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Charlie FC"));
    }

    @Test
    void aSquadListsOnlyThatTeamsPlayersAndShirtNumbersAreUnique() throws Exception {
        String admin = adminToken();
        long mumbai = createTeam(admin, "Mumbai Mariners", "MUM");
        long delhi = createTeam(admin, "Delhi Dynamos", "DEL");
        createPlayer(admin, mumbai, "Rohan Mehta", 9);
        createPlayer(admin, mumbai, "Vikram Singh", 1);
        createPlayer(admin, delhi, "Kabir Khan", 9);

        mvc.perform(get("/api/teams/" + mumbai + "/players"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].shirtNumber").value(1));
        mvc.perform(get("/api/players").param("teamId", String.valueOf(delhi)))
                .andExpect(jsonPath("$.content[0].name").value("Kabir Khan"));
        mvc.perform(get("/api/teams/999999/players")).andExpect(status().isNotFound());

        int duplicate = send(post("/api/players"), admin,
                "{\"name\":\"Someone\",\"position\":\"DEFENDER\",\"shirtNumber\":9,\"teamId\":" + mumbai + "}")
                .getResponse().getStatus();
        assertThat(duplicate).isEqualTo(409);
    }

    @Test
    void newsIsLinkedToEveryTeamItMentions() throws Exception {
        String admin = adminToken();
        long mumbai = createTeam(admin, "Mumbai Mariners", "MUM");
        long delhi = createTeam(admin, "Delhi Dynamos", "DEL");
        long kolkata = createTeam(admin, "Kolkata Knights", "KOL");

        var created = send(post("/api/news"), admin,
                "{\"title\":\"Derby day\",\"content\":\"Mumbai host Delhi.\",\"teamIds\":[" + mumbai + "," + delhi
                        + "]}");
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        long articleId = ((Number) com.jayway.jsonpath.JsonPath
                .read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/news").param("teamId", String.valueOf(mumbai)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/news").param("teamId", String.valueOf(delhi)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/news").param("teamId", String.valueOf(kolkata)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/news/" + articleId))
                .andExpect(jsonPath("$.teams", hasSize(2)))
                .andExpect(jsonPath("$.teams[0].shortName").value("DEL")); // sorted by name

        int unknownTeam = send(post("/api/news"), admin,
                "{\"title\":\"X\",\"content\":\"Y\",\"teamIds\":[999999]}").getResponse().getStatus();
        assertThat(unknownTeam).isEqualTo(404);

        assertThat(send(delete("/api/news/" + articleId), admin, null).getResponse().getStatus()).isEqualTo(204);
        mvc.perform(get("/api/news/" + articleId)).andExpect(status().isNotFound());
    }
}
