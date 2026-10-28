package com.trivoko.config;

import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Turns on @Cacheable. Spring Boot then builds a Caffeine cache manager (in memory) with the settings of
 * spring.cache.caffeine.spec in application.yml (max 500 entries, 60 seconds). First user: the seller's sales
 * chart (Phase 5). Without this annotation @Cacheable silently does nothing - found by DashboardTest.salesAreCached.
 */
@Configuration
@EnableCaching
class CacheConfig {

	/**
	 * Phase 10: "Customers also bought" is kept for 1 hour instead of the 60-second default (buying habits change
	 * slowly). A customizer keeps everything Spring Boot set up and only adds this one cache.
	 */
	@Bean
	CacheManagerCustomizer<CaffeineCacheManager> alsoBoughtCache() {
		return manager -> manager.registerCustomCache("alsoBought",
				Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(1_000).build());
	}

}
