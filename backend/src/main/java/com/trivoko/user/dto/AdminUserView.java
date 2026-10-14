package com.trivoko.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A person as the admin's user list shows them (Phase 6). blocked = enabled is false. Never the password hash. */
public record AdminUserView(Long id, String email, String fullName, String phone, List<String> roles, boolean blocked,
		LocalDateTime createdAt) {
}
