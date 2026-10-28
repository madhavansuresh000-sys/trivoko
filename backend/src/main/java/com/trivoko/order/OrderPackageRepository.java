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

	/**
	 * Phase 10 "also bought": the OTHER variants that were in the same orders (with this status) as any of these
	 * variants, ranked by how many orders they shared. (Two lines of one order may sit in different packages.)
	 */
	@Query("""
			select other.variantId from OrderItem mine join mine.orderPackage mp join mp.order o,
				OrderItem other join other.orderPackage op
			where op.order = o and o.status = :status
				and mine.variantId in :variantIds and other.variantId not in :variantIds
			group by other.variantId
			order by count(distinct o.id) desc, other.variantId""")
	java.util.List<Long> boughtTogether(@Param("variantIds") Collection<Long> variantIds,
			@Param("status") OrderStatus status, Pageable page);

	long countBySellerIdAndStatus(Long sellerId, PackageStatus status);

	/** Sales chart: my packages placed since this time, in these statuses. */
	java.util.List<OrderPackage> findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(Long sellerId,
			java.time.LocalDateTime from, Collection<PackageStatus> statuses);

}
