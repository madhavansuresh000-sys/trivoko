package com.trivoko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Title and description shown at the top of Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI trivokoOpenApi() {
		return new OpenAPI().info(new Info()
				.title("TriVoKo API")
				.version("0.0 (Phase 0)")
				.description("""
						Multi-seller marketplace: one cart, one payment, one package per seller.
						Standouts: flash sale, returns and refunds, price-drop alerts, smart search."""));
	}

}
