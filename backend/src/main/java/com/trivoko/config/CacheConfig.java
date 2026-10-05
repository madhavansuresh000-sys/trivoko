package com.trivoko.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on @Cacheable. Spring Boot then builds a Caffeine cache manager (in memory) with the settings of
 * spring.cache.caffeine.spec in application.yml (max 500 entries, 60 seconds). First user: the seller's sales
 * chart (Phase 5). Without this annotation @Cacheable silently does nothing - found by DashboardTest.salesAreCached.
 */
@Configuration
@EnableCaching
class CacheConfig {
}
