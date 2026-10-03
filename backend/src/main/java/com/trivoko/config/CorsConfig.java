package com.trivoko.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS = which OTHER websites may call this API from a browser.
 * In dev the Vite proxy makes React and Spring look like one site, so this matters when the
 * React app is served from a different address (npm run preview, or the live site in Phase 12).
 * The list comes from app.cors.allowed-origins in application.yml. (Copied from EventHub.)
 */
@Configuration
public class CorsConfig {

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Location"));
		config.setMaxAge(3600L); // the browser may remember the answer for 1 hour

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
