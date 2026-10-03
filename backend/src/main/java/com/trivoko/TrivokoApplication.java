package com.trivoko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TriVoKo - a multi-seller marketplace.
 *
 * One Spring Boot app split into modules (a "modular monolith"): every module is one package
 * under com.trivoko (catalog, order, payment ...). A module may call another module's SERVICE,
 * never its repository or tables. See package-info.java in each module.
 */
@SpringBootApplication
public class TrivokoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrivokoApplication.class, args);
	}

}
