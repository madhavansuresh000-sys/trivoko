package com.trivoko.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.auth.AuthUser;
import com.trivoko.user.Role;

/** Every admin action leaves a row: who did what, to which record, and when. */
@SpringBootTest
@Transactional
class AuditServiceTest {

	@Autowired
	private AuditService audit;

	@AfterEach
	void logout() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void recordsTheLoggedInUserAsTheActor() {
		AuthUser admin = new AuthUser(1L, "admin@trivoko.test", "TriVoKo Admin", Set.of(Role.CUSTOMER, Role.ADMIN));
		SecurityContextHolder.getContext()
			.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of()));

		audit.record("SELLER_APPROVED", "SELLER", 9L, "Erode Organics");

		List<AuditLog> rows = audit.latestFor("SELLER", 9L);
		assertThat(rows).hasSize(1);
		AuditLog row = rows.get(0);
		assertThat(row.getActorUserId()).isEqualTo(1L);
		assertThat(row.getAction()).isEqualTo("SELLER_APPROVED");
		assertThat(row.getDetails()).isEqualTo("Erode Organics");
		assertThat(row.getCreatedAt()).isNotNull();
	}

	@Test
	void systemActionsHaveNoActor() {
		audit.record("PRICE_CHANGED", "VARIANT", 1L, "79999.00 -> 74999.00");
		assertThat(audit.latestFor("VARIANT", 1L).get(0).getActorUserId()).isNull();
	}

}
