package com.scorestream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injected instead of calling LocalDateTime.now() directly, so tests can control "now". */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
