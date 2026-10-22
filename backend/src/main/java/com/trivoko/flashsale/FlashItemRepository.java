package com.trivoko.flashsale;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashItemRepository extends JpaRepository<FlashSaleItem, Long> {

	List<FlashSaleItem> findBySaleIdOrderByIdAsc(Long saleId);

	List<FlashSaleItem> findBySaleIdIn(Collection<Long> saleIds);

	List<FlashSaleItem> findBySellerIdOrderByIdDesc(Long sellerId);

	/**
	 * THE CLAIM. One statement that checks and changes in the same breath: MySQL locks the row, tests
	 * "sold < quantity" and the sale window, and adds one. Two people can never both take the last unit.
	 * 1 = you got a unit, 0 = sold out / not live.
	 */
	@Modifying
	@Query("""
			update FlashSaleItem i set i.sold = i.sold + 1
			where i.id = :id and i.sold < i.quantity
			  and exists (select 1 from FlashSale s where s.id = i.saleId and s.startsAt <= :now and s.endsAt > :now)""")
	int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

	/** THE GIVE-BACK: an unpaid claim returns its unit. Never below zero. */
	@Modifying
	@Query("update FlashSaleItem i set i.sold = i.sold - 1 where i.id = :id and i.sold > 0")
	int giveBack(@Param("id") Long id);

	/** The numbers in the database right now (not a cached entity): sold. */
	@Query("select i.sold from FlashSaleItem i where i.id = :id")
	Integer soldNow(@Param("id") Long id);

	/**
	 * Units not yet claimed, read WITHOUT locking the row (a plain snapshot read). Only used to turn people away
	 * quickly once a deal is sold out; the locking UPDATE above is still the only thing that gives a unit.
	 */
	@Query("select i.quantity - i.sold from FlashSaleItem i where i.id = :id")
	Integer leftNow(@Param("id") Long id);

}
