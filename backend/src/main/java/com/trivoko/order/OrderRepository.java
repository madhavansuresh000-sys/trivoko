package com.trivoko.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByNumber(String number);

	boolean existsByNumber(String number);

	/** My orders, newest first. */
	Page<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

	/** One unpaid order at a time per customer. */
	Optional<Order> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, OrderStatus status);

	/** The expiry job: unpaid orders whose 10 minutes are over. */
	List<Order> findByStatusAndHoldExpiresAtBefore(OrderStatus status, LocalDateTime time);

	boolean existsByUserIdAndStatusAndCouponCode(Long userId, OrderStatus status, String couponCode);

}
