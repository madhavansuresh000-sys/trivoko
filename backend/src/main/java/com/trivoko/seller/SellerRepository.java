package com.trivoko.seller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Only the seller module may use this (ArchUnit rule); other modules call SellerService. */
public interface SellerRepository extends JpaRepository<Seller, Long> {

	Optional<Seller> findBySlug(String slug);

	Optional<Seller> findBySlugAndStatus(String slug, SellerStatus status);

	Optional<Seller> findByOwnerId(Long ownerId);

	List<Seller> findByStatusOrderByIdAsc(SellerStatus status);

	/** The admin's shop list: status null = every status; q null = no search (q comes from Likes.contains). */
	@Query("""
			select s from Seller s
			where (:status is null or s.status = :status)
			  and (:q is null or lower(s.shopName) like :q escape '\\' or lower(s.city) like :q escape '\\'
			       or s.slug like :q escape '\\')""")
	Page<Seller> search(SellerStatus status, String q, Pageable pageable);

	long countByStatus(SellerStatus status);

}
