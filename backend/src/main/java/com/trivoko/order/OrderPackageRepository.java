package com.trivoko.order;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPackageRepository extends JpaRepository<OrderPackage, Long> {

	/** The seller's packing table: their packages in these statuses, newest first. */
	Page<OrderPackage> findBySellerIdAndStatusInOrderByIdDesc(Long sellerId, Collection<PackageStatus> statuses, Pageable pageable);

	long countBySellerIdAndStatus(Long sellerId, PackageStatus status);

	/** Sales chart: my packages placed since this time, in these statuses. */
	java.util.List<OrderPackage> findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(Long sellerId,
			java.time.LocalDateTime from, Collection<PackageStatus> statuses);

}
