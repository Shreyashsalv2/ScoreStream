package com.scorestream.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * If Redis is down, caching errors are logged and ignored: the method simply runs and reads
 * from MySQL. The API gets slower but keeps working, instead of failing every request.
 * The cache time-to-live is set in application.yml (spring.cache.redis.time-to-live).
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    public static final String LIVE_MATCHES = "liveMatches";
    public static final String MATCH = "match";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache read failed for {}::{}, using the database: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for {}::{}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache evict failed for {}::{}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache clear failed for {}: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
