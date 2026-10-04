package com.trivoko.seller;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Only the seller module may use this (ArchUnit rule); other modules call SellerService. */
public interface SellerRepository extends JpaRepository<Seller, Long> {

	Optional<Seller> findBySlug(String slug);

	Optional<Seller> findBySlugAndStatus(String slug, SellerStatus status);

	Optional<Seller> findByOwnerId(Long ownerId);

}
