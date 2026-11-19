package com.scorestream.config;

import com.scorestream.domain.AppUser;
import com.scorestream.domain.Match;
import com.scorestream.domain.MatchStatus;
import com.scorestream.domain.NewsArticle;
import com.scorestream.domain.Player;
import com.scorestream.domain.PlayerStat;
import com.scorestream.domain.Position;
import com.scorestream.domain.Role;
import com.scorestream.domain.Team;
import com.scorestream.repository.AppUserRepository;
import com.scorestream.repository.MatchRepository;
import com.scorestream.repository.NewsArticleRepository;
import com.scorestream.repository.PlayerRepository;
import com.scorestream.repository.PlayerStatRepository;
import com.scorestream.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Runs on startup: makes sure the admin account exists, and (when app.seed-demo-data is true and
 * the database is empty) adds a few teams, players, matches and articles so the API has data to
 * show straight away.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository users;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final MatchRepository matches;
    private final PlayerStatRepository stats;
    private final NewsArticleRepository news;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean seedDemoData;

    public DataSeeder(AppUserRepository users, TeamRepository teams, PlayerRepository players,
                      MatchRepository matches, PlayerStatRepository stats, NewsArticleRepository news,
                      PasswordEncoder passwordEncoder, Clock clock,
                      @Value("${app.admin.username}") String adminUsername,
                      @Value("${app.admin.password}") String adminPassword,
                      @Value("${app.seed-demo-data}") boolean seedDemoData) {
        this.users = users;
        this.teams = teams;
        this.players = players;
        this.matches = matches;
        this.stats = stats;
        this.news = news;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.seedDemoData = seedDemoData;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!users.existsByUsername(adminUsername)) {
            users.save(new AppUser(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
            log.info("Created admin account '{}'", adminUsername);
        }
        if (seedDemoData && teams.count() == 0) {
            seedDemo();
            log.info("Added demo data: {} teams, {} players, {} matches", teams.count(), players.count(),
                    matches.count());
        }
    }

    private void seedDemo() {
        Team mumbai = teams.save(new Team("Mumbai Mariners", "MUM", "Mumbai"));
        Team delhi = teams.save(new Team("Delhi Dynamos", "DEL", "New Delhi"));
        Team bengaluru = teams.save(new Team("Bengaluru Blasters", "BLR", "Bengaluru"));
        Team kolkata = teams.save(new Team("Kolkata Knights", "KOL", "Kolkata"));

        Player rohan = players.save(new Player("Rohan Mehta", Position.FORWARD, 9, mumbai));
        Player arjun = players.save(new Player("Arjun Rao", Position.MIDFIELDER, 8, mumbai));
        players.save(new Player("Vikram Singh", Position.GOALKEEPER, 1, mumbai));
        Player kabir = players.save(new Player("Kabir Khan", Position.FORWARD, 10, delhi));
        players.save(new Player("Imran Sheikh", Position.DEFENDER, 4, delhi));
        players.save(new Player("Aditya Nair", Position.FORWARD, 11, bengaluru));
        players.save(new Player("Sameer Das", Position.MIDFIELDER, 6, kolkata));

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        Match finished = matches.save(new Match(mumbai, delhi, "Mumbai Football Arena", now.minusDays(2)));
        finished.updateScore(2, 1, MatchStatus.FINISHED, null);
        stats.save(new PlayerStat(finished, rohan, 2, 0, 90));
        stats.save(new PlayerStat(finished, arjun, 0, 1, 90));
        stats.save(new PlayerStat(finished, kabir, 1, 0, 85));

        Match live = matches.save(new Match(bengaluru, kolkata, "Kanteerava Stadium", now.minusMinutes(40)));
        live.updateScore(1, 0, MatchStatus.LIVE, 38);

        matches.save(new Match(delhi, bengaluru, "Jawaharlal Nehru Stadium", now.plusDays(1)));
        matches.save(new Match(kolkata, mumbai, "Salt Lake Stadium", now.plusDays(3)));

        news.save(new NewsArticle("Rohan Mehta's double sinks Delhi Dynamos",
                "Rohan Mehta scored twice as Mumbai Mariners beat Delhi Dynamos 2-1.",
                now.minusDays(2).plusHours(3), Set.of(mumbai, delhi)));
        news.save(new NewsArticle("Blasters lead Knights at half-time",
                "An early goal puts Bengaluru Blasters ahead of Kolkata Knights.",
                now.minusMinutes(5), Set.of(bengaluru, kolkata)));
        news.save(new NewsArticle("Dynamos name unchanged squad for Bengaluru trip",
                "Delhi Dynamos will travel with the same squad that faced Mumbai.", now.minusHours(6),
                Set.of(delhi)));
    }
}
