package com.trivoko.notify;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.notify.dto.Bell;
import com.trivoko.notify.dto.NotificationView;

/**
 * The bell (and the emails behind it), copied from EventHub.
 *
 * create() is called INSIDE the transaction of the change it talks about (e.g. "order paid"), so the
 * message and the change are saved together or not at all. The email goes out only AFTER the commit
 * (EmailSender listens for EmailQueued) - a payment that rolls back never sends "your order is placed".
 */
@Service
public class NotificationService {

	static final int BELL_SIZE = 20;

	private final NotificationRepository notifications;

	private final ApplicationEventPublisher publisher;

	/** false = bell only, no emails (e.g. a demo server without a mail account). MAIL_ENABLED */
	private final boolean mailEnabled;

	NotificationService(NotificationRepository notifications, ApplicationEventPublisher publisher,
			@Value("${app.mail.enabled:true}") boolean mailEnabled) {
		this.notifications = notifications;
		this.publisher = publisher;
		this.mailEnabled = mailEnabled;
	}

	@Transactional
	public void create(Long userId, NotificationKind kind, String title, String body, String link, boolean email) {
		Notification n = new Notification();
		n.setUserId(userId);
		n.setKind(kind);
		n.setTitle(title);
		n.setBody(body);
		n.setLink(link);
		boolean sendEmail = email && mailEnabled;
		n.setEmailStatus(sendEmail ? EmailStatus.PENDING : EmailStatus.NONE);
		notifications.save(n);
		if (sendEmail) {
			publisher.publishEvent(new EmailQueued(n.getId())); // handled only AFTER the commit
		}
	}

	@Transactional(readOnly = true)
	public Bell mine(Long userId) {
		return new Bell(notifications.countByUserIdAndReadAtIsNull(userId),
				notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, BELL_SIZE)).stream()
					.map(NotificationView::from).toList());
	}

	@Transactional
	public NotificationView markRead(Long userId, Long id) {
		Notification n = notifications.findByIdAndUserId(id, userId) // someone else's = 404
			.orElseThrow(() -> new ResourceNotFoundException("Notification", id));
		if (n.getReadAt() == null) {
			n.setReadAt(LocalDateTime.now());
		}
		return NotificationView.from(n);
	}

	@Transactional
	public int markAllRead(Long userId) {
		return notifications.markAllRead(userId, LocalDateTime.now());
	}

	/** "Notification X has an email to send" - published inside the transaction, handled after it. */
	public record EmailQueued(Long notificationId) {
	}

}
