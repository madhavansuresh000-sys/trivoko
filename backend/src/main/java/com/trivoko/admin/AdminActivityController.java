package com.trivoko.admin;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.admin.AdminSellerController.AdminPages;
import com.trivoko.common.PageResponse;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * The activity log page (ADMIN only): who changed what and when, newest first. The "who" is shown as an email,
 * looked up for the whole page in one query (UserService cannot be used inside AuditService: UserService itself
 * writes audit lines, and the two services would need each other).
 */
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
public class AdminActivityController {

	private final AuditService audit;

	private final UserService users;

	@GetMapping
	public PageResponse<ActivityRow> list(@RequestParam(required = false) String action,
			@RequestParam(defaultValue = "0") int page) {
		Page<AuditLog> lines = audit.activity(action, AdminPages.of(page, Sort.unsorted()));
		Map<Long, String> emails = users.emailsOf(lines.getContent().stream().map(AuditLog::getActorUserId)
			.filter(Objects::nonNull).collect(Collectors.toSet()));
		return PageResponse.from(lines, l -> new ActivityRow(l.getId(), l.getAction(), l.getEntityType(), l.getEntityId(),
				l.getDetails(), l.getActorUserId() == null ? "system" : emails.getOrDefault(l.getActorUserId(), "user " + l.getActorUserId()),
				l.getCreatedAt()));
	}

	/** actorEmail = "system" for jobs (e.g. the expiry job), not a person. */
	public record ActivityRow(Long id, String action, String entityType, Long entityId, String details,
			String actorEmail, LocalDateTime createdAt) {
	}

}
