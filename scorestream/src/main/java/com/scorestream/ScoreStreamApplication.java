package com.scorestream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

// Users log in through AuthService with JWTs, so Spring's default in-memory user isn't needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableCaching
public class ScoreStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoreStreamApplication.class, args);
    }
}
