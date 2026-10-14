package com.trivoko.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Only the user module may use this (ArchUnit rule); other modules call UserService. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/** The admin's user search (Phase 6): q null = everyone; q comes from Likes.contains. */
	@Query("""
			select u from User u
			where :q is null or lower(u.email) like :q escape '\\' or lower(u.fullName) like :q escape '\\'
			   or u.phone like :q escape '\\'""")
	Page<User> search(String q, Pageable pageable);

	/** For lists that show "who": id + email of many users in one query. */
	List<User> findByIdIn(Collection<Long> ids);

}
