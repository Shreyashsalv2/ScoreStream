package com.scorestream.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAndErrorsIntegrationTest extends IntegrationTestBase {

    private static final String TEAM_JSON = "{\"name\":\"Pune Panthers\",\"shortName\":\"PUN\",\"city\":\"Pune\"}";

    @Test
    void readsArePublic() throws Exception {
        mvc.perform(get("/api/teams")).andExpect(status().isOk());
        mvc.perform(get("/api/matches/live")).andExpect(status().isOk());
        mvc.perform(get("/api/news")).andExpect(status().isOk());
    }

    @Test
    void writingWithoutATokenIs401WithAJsonBody() throws Exception {
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/teams"));
    }

    @Test
    void writingWithAnInvalidTokenIs401() throws Exception {
        mvc.perform(post("/api/teams").header("Authorization", "Bearer not.a.real-token")
                        .contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aNormalUserCannotWrite() throws Exception {
        String token = userToken();
        mvc.perform(post("/api/teams").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void anAdminCanWriteAndDuplicatesAre409() throws Exception {
        String token = adminToken();
        mvc.perform(post("/api/teams").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.shortName").value("PUN"));
        mvc.perform(post("/api/teams").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void registeringTwiceWithTheSameNameIs409() throws Exception {
        userToken();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fan_1\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void theWrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void invalidInputIs400WithEachFieldExplained() throws Exception {
        mvc.perform(post("/api/teams").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"shortName\":\"TOOLONG\",\"city\":\"Pune\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.shortName").value("must be exactly 3 letters"));
    }

    @Test
    void brokenJsonIs400() throws Exception {
        mvc.perform(post("/api/teams").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMissingRecordIs404() throws Exception {
        mvc.perform(get("/api/matches/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match 999999 not found"));
    }

    @Test
    void aBadIdOrFilterValueIs400() throws Exception {
        mvc.perform(get("/api/matches/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/matches").param("status", "HALFTIME")).andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownSortFieldIs400NotACrash() throws Exception {
        mvc.perform(get("/api/teams").param("sort", "nonsense")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/matches").param("sort", "nonsense")).andExpect(status().isBadRequest());
    }

    @Test
    void anUnsupportedMethodIs405() throws Exception {
        mvc.perform(put("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void swaggerDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ScoreStream API"));
    }
}
