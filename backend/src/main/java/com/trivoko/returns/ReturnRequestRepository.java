package com.trivoko.returns;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

	boolean existsByOrderItemId(Long orderItemId);

	Optional<ReturnRequest> findByIdAndCustomerId(Long id, Long customerId);

	Optional<ReturnRequest> findByIdAndSellerId(Long id, Long sellerId);

	List<ReturnRequest> findByCustomerIdOrderByIdDesc(Long customerId);

	List<ReturnRequest> findByOrderId(Long orderId);

	Page<ReturnRequest> findBySellerIdAndStatus(Long sellerId, ReturnStatus status, Pageable pageable);

	Page<ReturnRequest> findBySellerId(Long sellerId, Pageable pageable);

	Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

	/** The auto-approve job: REQUESTED for more than 2 days with no answer from the seller. */
	List<ReturnRequest> findByStatusAndCreatedAtBefore(ReturnStatus status, LocalDateTime before);

}
