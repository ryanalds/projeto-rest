package edu.ifrn.apigateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {
    @Bean
    CacheManager cacheManager(@Value("${app.cache.ttl-minutes:10}") long ttlMinutes) {
        CaffeineCacheManager manager = new CaffeineCacheManager("catalog-items", "catalog-pages");
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(2_000)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes)));
        return manager;
    }
}
