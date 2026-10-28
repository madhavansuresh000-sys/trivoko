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

	/** Phase 9 reviews: did this customer receive at least one of these variants? */
	@Query("""
			select count(p) > 0 from OrderPackage p join p.items i
			where p.order.userId = :userId and p.status = :status and i.variantId in :variantIds""")
	boolean existsWithItem(@Param("userId") Long userId, @Param("status") PackageStatus status,
			@Param("variantIds") Collection<Long> variantIds);

	long countBySellerIdAndStatus(Long sellerId, PackageStatus status);

	/** Sales chart: my packages placed since this time, in these statuses. */
	java.util.List<OrderPackage> findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(Long sellerId,
			java.time.LocalDateTime from, Collection<PackageStatus> statuses);

}
