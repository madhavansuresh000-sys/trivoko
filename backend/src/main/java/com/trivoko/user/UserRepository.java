package com.trivoko.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Only the user module may use this (ArchUnit rule); other modules call UserService. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

}
