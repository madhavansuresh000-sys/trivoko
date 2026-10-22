package com.trivoko.flashsale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

	/** Sales that have not ended yet (live now or coming), soonest first. */
	List<FlashSale> findByEndsAtAfterOrderByStartsAtAsc(LocalDateTime after);

	List<FlashSale> findAllByOrderByStartsAtDesc();

	/** The sale row as a lock: give-backs and the end-of-sale settle never run at the same time. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from FlashSale s where s.id = :id")
	Optional<FlashSale> lockById(@Param("id") Long id);

	/** Ended a while ago (the grace period lets claims that were in flight at the end finish first). */
	List<FlashSale> findBySettledFalseAndEndsAtBefore(LocalDateTime before);

	/** Two unsettled sales with overlapping windows may not offer the same variant. */
	@Query("""
			select count(i) from FlashSaleItem i join FlashSale s on s.id = i.saleId
			where i.variantId = :variantId and s.id <> :saleId and s.settled = false
			  and s.startsAt < :endsAt and s.endsAt > :startsAt""")
	long countOverlapping(@Param("variantId") Long variantId, @Param("saleId") Long saleId,
			@Param("startsAt") LocalDateTime startsAt, @Param("endsAt") LocalDateTime endsAt);

	@Modifying
	@Query("update FlashSale s set s.settled = true where s.id = :id")
	int markSettled(@Param("id") Long id);

}
