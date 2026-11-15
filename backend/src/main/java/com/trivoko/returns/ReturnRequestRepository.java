package com.trivoko.returns;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

	boolean existsByOrderItemId(Long orderItemId);

	Optional<ReturnRequest> findByIdAndCustomerId(Long id, Long customerId);

	Optional<ReturnRequest> findByIdAndSellerId(Long id, Long sellerId);

	/**
	 * SELECT ... FOR UPDATE: the money steps (picked up, refund) take this lock FIRST, so ten clicks at the same moment
	 * wait in a queue. Without it each click saved a timeline row (that takes a shared lock on the return through its
	 * foreign key) and then updated the return (needs an exclusive lock): two clicks waited for each other - a MySQL
	 * deadlock - and MySQL sometimes cancelled the one real refund (seen once in CI, 15 Nov 2026).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from ReturnRequest r where r.id = :id")
	Optional<ReturnRequest> lockById(@Param("id") Long id);

	List<ReturnRequest> findByCustomerIdOrderByIdDesc(Long customerId);

	List<ReturnRequest> findByOrderId(Long orderId);

	Page<ReturnRequest> findBySellerIdAndStatus(Long sellerId, ReturnStatus status, Pageable pageable);

	Page<ReturnRequest> findBySellerId(Long sellerId, Pageable pageable);

	Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

	/** The auto-approve job: REQUESTED for more than 2 days with no answer from the seller. */
	List<ReturnRequest> findByStatusAndCreatedAtBefore(ReturnStatus status, LocalDateTime before);

}
