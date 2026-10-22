package com.trivoko.flashsale;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashClaimRepository extends JpaRepository<FlashClaim, Long> {

	boolean existsByItemIdAndUserId(Long itemId, Long userId);

	Optional<FlashClaim> findByItemIdAndUserId(Long itemId, Long userId);

	List<FlashClaim> findByUserIdAndStatusIn(Long userId, Collection<ClaimStatus> statuses);

	List<FlashClaim> findByOrderId(Long orderId);

	List<FlashClaim> findByStatusAndExpiresAtBefore(ClaimStatus status, LocalDateTime before);

}
