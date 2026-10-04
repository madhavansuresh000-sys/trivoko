package com.trivoko.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Only the user module may use this (ArchUnit rule). */
public interface AddressRepository extends JpaRepository<Address, Long> {

	/** Newest first, so "the newest remaining address" is simply the first one. */
	List<Address> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	Optional<Address> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

}
