package com.trivoko.notify;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.notify.dto.Bell;
import com.trivoko.notify.dto.NotificationView;

import lombok.RequiredArgsConstructor;

/** The bell. Login required; only your own notifications. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService service;

	@GetMapping
	public Bell mine(@AuthenticationPrincipal AuthUser user) {
		return service.mine(user.id());
	}

	@PostMapping("/{id}/read")
	public NotificationView read(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
		return service.markRead(user.id(), id);
	}

	@PostMapping("/read-all")
	public Map<String, Integer> readAll(@AuthenticationPrincipal AuthUser user) {
		return Map.of("marked", service.markAllRead(user.id()));
	}

}
