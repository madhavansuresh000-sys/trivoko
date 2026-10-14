package com.trivoko.admin;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.admin.AdminSellerController.AdminPages;
import com.trivoko.common.PageResponse;
import com.trivoko.user.UserService;
import com.trivoko.user.dto.AdminUserView;

import lombok.RequiredArgsConstructor;

/** The admin's user page (ADMIN only, SecurityConfig): search, block, unblock. Each change is in the audit log. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserService userService;

	@GetMapping
	public PageResponse<AdminUserView> list(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page) {
		return userService.search(q, AdminPages.of(page, Sort.by(Sort.Direction.DESC, "id")));
	}

	@PostMapping("/{id}/block")
	public AdminUserView block(@PathVariable Long id) {
		return userService.block(id);
	}

	@PostMapping("/{id}/unblock")
	public AdminUserView unblock(@PathVariable Long id) {
		return userService.unblock(id);
	}

}
