package com.scorestream.web;

import com.jayway.jsonpath.JsonPath;
import com.scorestream.repository.AppUserRepository;
import com.scorestream.repository.MatchRepository;
import com.scorestream.repository.NewsArticleRepository;
import com.scorestream.repository.PlayerRepository;
import com.scorestream.repository.PlayerStatRepository;
import com.scorestream.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests start the whole application (security, controllers, services, JPA) on an
 * in-memory H2 database and send real HTTP requests through MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected CacheManager cacheManager;
    @Autowired
    private PlayerStatRepository stats;
    @Autowired
    private NewsArticleRepository news;
    @Autowired
    private MatchRepository matches;
    @Autowired
    private PlayerRepository players;
    @Autowired
    private TeamRepository teams;
    @Autowired
    private AppUserRepository users;

    /** Every test starts from an empty database (except the admin account) and empty caches. */
    @BeforeEach
    void resetState() {
        stats.deleteAll();
        news.deleteAll();
        matches.deleteAll();
        players.deleteAll();
        teams.deleteAll();
        users.findAll().stream().filter(u -> !u.getUsername().equals("admin")).forEach(users::delete);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    protected String adminToken() throws Exception {
        return login("admin", "admin12345");
    }

    protected String userToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fan_1\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    protected String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    /** Sends a JSON request as the given user and returns the response body. */
    protected MvcResult send(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(request).andReturn();
    }

    protected long createTeam(String token, String name, String shortName) throws Exception {
        MvcResult result = send(post("/api/teams"), token,
                "{\"name\":\"" + name + "\",\"shortName\":\"" + shortName + "\",\"city\":\"Test City\"}");
        return idFrom(result, 201);
    }

    protected long createPlayer(String token, long teamId, String name, int number) throws Exception {
        MvcResult result = send(post("/api/players"), token,
                "{\"name\":\"" + name + "\",\"position\":\"FORWARD\",\"shirtNumber\":" + number
                        + ",\"teamId\":" + teamId + "}");
        return idFrom(result, 201);
    }

    protected long createMatch(String token, long homeId, long awayId) throws Exception {
        MvcResult result = send(post("/api/matches"), token,
                "{\"homeTeamId\":" + homeId + ",\"awayTeamId\":" + awayId
                        + ",\"venue\":\"Test Arena\",\"kickoff\":\"2026-12-01T19:30:00\"}");
        return idFrom(result, 201);
    }

    private static long idFrom(MvcResult result, int expectedStatus) throws Exception {
        String body = result.getResponse().getContentAsString();
        if (result.getResponse().getStatus() != expectedStatus) {
            throw new AssertionError("Expected " + expectedStatus + " but got "
                    + result.getResponse().getStatus() + ": " + body);
        }
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
