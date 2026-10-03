package com.trivoko.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tiny "are you alive?" endpoint. The React app calls it to show "Backend: UP".
 */
@RestController
@RequestMapping("/api")
public class HealthController {

	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP", "app", "trivoko");
	}

}
