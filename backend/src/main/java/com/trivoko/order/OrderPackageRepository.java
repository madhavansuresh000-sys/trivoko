package com.trivoko.order;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderPackageRepository extends JpaRepository<OrderPackage, Long> {

	/** The seller's packing table: their packages in these statuses, newest first. */
	Page<OrderPackage> findBySellerIdAndStatusInOrderByIdDesc(Long sellerId, Collection<PackageStatus> statuses, Pageable pageable);

	/** Phase 8 returns: the package that holds this order line. */
	@Query("select p from OrderPackage p join p.items i where i.id = :itemId")
	java.util.Optional<OrderPackage> findByItemId(@Param("itemId") Long itemId);

	long countBySellerIdAndStatus(Long sellerId, PackageStatus status);

	/** Sales chart: my packages placed since this time, in these statuses. */
	java.util.List<OrderPackage> findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(Long sellerId,
			java.time.LocalDateTime from, Collection<PackageStatus> statuses);

}
