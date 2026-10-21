package com.trivoko.notify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** The bell: my newest messages. */
	List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable page);

	long countByUserIdAndReadAtIsNull(Long userId);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	/** "Mark all as read" in ONE update statement. */
	@Modifying
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

	/** For the email sender: emails still to send, oldest first. */
	@Query("select n.id from Notification n where n.emailStatus = com.trivoko.notify.EmailStatus.PENDING order by n.id")
	List<Long> findPendingEmailIds(Pageable page);

}
