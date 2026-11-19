# ScoreStream — Live Sports Score & News Feed Backend

A REST API for a live football app: teams, squads, fixtures, live scores, player stats and news. Built with **Java 17, Spring Boot 3, Spring Data JPA (Hibernate), MySQL and Redis**, secured with **Spring Security + JWT**, and tested with **JUnit 5 and Mockito**.

## Features

* **20 REST endpoints** across teams, players, matches, news and auth (full list below)
* **Layered design**: Controller → Service → Repository, with request and response DTOs (Java records) so entities never leak into the API
* **Relationships**: Team → Players (one-to-many), Match → Teams (many-to-one), News ↔ Teams (many-to-many through a join table)
* **Paging, sorting and filtering** on every list (`?page=0&size=20&sort=name`, `?status=LIVE`, `?teamId=3`), with indexes on the columns the busiest queries filter on
* **Redis caching** of live scores with a short time-to-live, cleared on every score update. If Redis goes down, the API logs a warning and reads from MySQL instead of failing (about 1 second per request while Redis is down, then back to normal by itself).
* **JWT auth with roles**: anyone can read; only `ADMIN` can create, update or delete
* **One error format** for every failure (400, 401, 403, 404, 405, 409, 500) via `@RestControllerAdvice`
* **Business rules**: a finished match can't change, a live match can't go back to scheduled, stats only for players in that match, one stat line per player per match, unique shirt numbers per team
* **Swagger UI** at `/swagger-ui.html`
* **Docker Compose** for the app + MySQL + Redis, and **GitHub Actions CI** that runs every test on each push

## Run it

**Option 1: Docker (easiest)**: needs Docker only.
```bash
docker compose up --build
```
Open http://localhost:8080/swagger-ui.html. Demo data (4 teams, players, a live match, news) is added on first start.

**Option 2: locally**: needs Java 17+ and MySQL and Redis running on their default ports.
```bash
./mvnw spring-boot:run
```
Settings are in `src/main/resources/application.yml` and can be overridden with environment variables (see `.env.example`).

**Run the tests**: needs nothing else, because tests use an in-memory H2 database and an in-memory cache.
```bash
./mvnw test
```

## Try it

```bash
# Anyone can read
curl localhost:8080/api/matches/live

# Log in as the admin (default local credentials: admin / admin12345)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# Update a live score (admin only); the cached live feed is cleared automatically
curl -X PATCH localhost:8080/api/matches/2/score -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"homeScore":2,"awayScore":0,"status":"LIVE","minute":55}'
```
In Swagger UI, call `/api/auth/login`, copy the token, click **Authorize** and paste it to try the admin endpoints.

## Endpoints

| Method | Path | Who | What |
|---|---|---|---|
| POST | `/api/auth/register` | anyone | Create a USER account, returns a JWT |
| POST | `/api/auth/login` | anyone | Log in, returns a JWT |
| GET | `/api/teams` | anyone | List teams (paged) |
| GET | `/api/teams/{id}` | anyone | One team |
| GET | `/api/teams/{id}/players` | anyone | A team's squad (paged) |
| POST | `/api/teams` | admin | Create a team |
| GET | `/api/players` | anyone | List players, `?teamId=` filter (paged) |
| GET | `/api/players/{id}` | anyone | One player |
| GET | `/api/players/{id}/stats` | anyone | A player's stats per match |
| POST | `/api/players` | admin | Add a player to a team |
| GET | `/api/matches` | anyone | Search by `?status=` and `?teamId=` (paged) |
| GET | `/api/matches/live` | anyone | Live matches (**cached**) |
| GET | `/api/matches/{id}` | anyone | One match (**cached**) |
| POST | `/api/matches` | admin | Schedule a match |
| PATCH | `/api/matches/{id}/score` | admin | Update score and status (clears the cache) |
| POST | `/api/matches/{id}/stats` | admin | Record a player's stats for a match |
| GET | `/api/news` | anyone | Latest news, `?teamId=` filter (paged) |
| GET | `/api/news/{id}` | anyone | One article |
| POST | `/api/news` | admin | Publish an article tagged with teams |
| DELETE | `/api/news/{id}` | admin | Delete an article |

## Tests

**45 tests, all passing** (`./mvnw test`, about 20 seconds):

| Suite | Type | Tests |
|---|---|---|
| `MatchServiceTest` | Unit (Mockito) | 13 |
| `AuthServiceTest` | Unit (Mockito) | 5 |
| `JwtServiceTest` | Unit | 5 |
| `SecurityAndErrorsIntegrationTest` | Integration (MockMvc) | 14 |
| `MatchFlowIntegrationTest` | Integration (MockMvc) | 5 |
| `CatalogIntegrationTest` | Integration (MockMvc) | 3 |

* **Unit tests** (`service/`, `security/`) test one class with its dependencies replaced by Mockito mocks: match rules, login and registration, JWT signing, expiry and a forged ADMIN token.
* **Integration tests** (`web/`) start the whole app on H2 and call the API through MockMvc: 401 vs 403, validation messages, 404/405/409, the full match flow from kickoff to final whistle, cache clearing, paging, filters, and the news-to-teams many-to-many.

## Design notes

* **Why cache live matches?** Every open scoreboard polls `/api/matches/live`, so it's by far the most-read endpoint, while scores change only a few times a match. Caching it for 15 seconds takes most of that load off MySQL. Clearing the cache on every update means a new score shows up on the very next request instead of after the TTL.
* **Why JWT?** The API is stateless: each request carries a signed token with the username and role, so the server doesn't need sessions and doesn't hit the database to check who's calling.
* **N+1 queries**: list queries use `@EntityGraph` to load each match's teams in the same query instead of one extra query per row; `default_batch_fetch_size` batches the rest.
* **What I'd add next**: Flyway migrations instead of `ddl-auto=update`, WebSockets to push score changes instead of polling, rate limiting on login, and Testcontainers to run integration tests against real MySQL and Redis.

## Project layout

```
src/main/java/com/scorestream/
  config/       security rules, cache error handling, Swagger, demo data seeder
  domain/       JPA entities: Team, Player, Match, PlayerStat, NewsArticle, AppUser
  dto/          request and response records with validation rules
  repository/   Spring Data JPA repositories and queries
  security/     JWT service, auth filter, JSON 401/403 responses
  service/      business logic and caching
  web/          REST controllers, exceptions, global error handler
src/test/java/com/scorestream/
  service/, security/   unit tests (JUnit 5 + Mockito)
  web/                  integration tests (Spring Boot + MockMvc + H2)
```
