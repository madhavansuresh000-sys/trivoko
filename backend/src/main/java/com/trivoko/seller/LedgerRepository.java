package com.trivoko.seller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

	Page<LedgerEntry> findBySellerIdOrderByIdDesc(Long sellerId, Pageable pageable);

	boolean existsByPackageIdAndKind(Long packageId, LedgerEntry.Kind kind);

	/** The balance: the sum of every line (0 when there are none). */
	@Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.sellerId = :sellerId")
	BigDecimal balance(@Param("sellerId") Long sellerId);

}
