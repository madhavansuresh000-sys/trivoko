package com.trivoko.user;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A person who can log in: a shopper, a shop owner or the admin. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Always stored lower-case and trimmed (UserService.normalizeEmail). */
	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(length = 10)
	private String phone;

	/** false = blocked by the admin: the login cookie stops working at once (JwtCookieFilter). */
	@Column(nullable = false)
	private boolean enabled = true;

	// roles are needed on every request, and there are at most 3, so load them with the user
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Set<Role> roles = EnumSet.noneOf(Role.class);

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public User(String email, String passwordHash, String fullName, String phone) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.phone = phone;
	}

}
