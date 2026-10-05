package com.trivoko.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findBySessionId(String sessionId);

	/** The newest payment page of an order (a reused pending order keeps its page). */
	Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

}
