package com.trivoko.notify;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.trivoko.notify.NotificationService.EmailQueued;
import com.trivoko.user.User;
import com.trivoko.user.UserService;

/**
 * Sends the emails of notifications (Spring Mail; in dev they land in Mailpit: http://localhost:8026).
 *
 *   1) right after the commit that created the notification (@TransactionalEventListener AFTER_COMMIT)
 *   2) if that failed (mail server down), the retry job tries again every minute - up to 5 times.
 *
 * The network call to the mail server is made OUTSIDE a database transaction (copied from EventHub).
 */
@Component
public class EmailSender {

	private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

	static final int MAX_ATTEMPTS = 5;

	private final NotificationRepository notifications;

	private final UserService users;

	private final JavaMailSender mail;

	private final TransactionTemplate tx;

	private final String from;

	private final String frontendUrl;

	EmailSender(NotificationRepository notifications, UserService users, JavaMailSender mail,
			PlatformTransactionManager txManager, @Value("${app.mail.from}") String from,
			@Value("${app.frontend-url}") String frontendUrl) {
		this.notifications = notifications;
		this.users = users;
		this.mail = mail;
		this.tx = new TransactionTemplate(txManager);
		this.from = from;
		this.frontendUrl = frontendUrl;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void afterCommit(EmailQueued queued) {
		send(queued.notificationId());
	}

	/**
	 * The retry job: every email still PENDING that was created before createdBefore (the job passes
	 * "1 minute ago", so it does not race with the after-commit send). Returns how many went out.
	 */
	public int sendPending(LocalDateTime createdBefore) {
		int sent = 0;
		for (Long id : notifications.findPendingEmailIds(PageRequest.of(0, 50))) {
			Boolean old = tx.execute(s -> notifications.findById(id).map(n -> n.getCreatedAt().isBefore(createdBefore)).orElse(false));
			if (Boolean.TRUE.equals(old) && send(id)) {
				sent++;
			}
		}
		return sent;
	}

	/** Sends one notification's email. Never throws: a failure is counted and retried later. */
	boolean send(Long notificationId) {
		// 1) read what to send (short transaction)
		SimpleMailMessage message = tx.execute(s -> notifications.findById(notificationId)
			.filter(n -> n.getEmailStatus() == EmailStatus.PENDING)
			.map(this::toMessage)
			.orElse(null));
		if (message == null) {
			return false; // already sent, or bell-only
		}
		// 2) talk to the mail server (no transaction open)
		boolean ok = trySend(message, notificationId);
		// 3) remember the result
		tx.executeWithoutResult(s -> notifications.findById(notificationId).ifPresent(n -> {
			n.setEmailAttempts(n.getEmailAttempts() + 1);
			if (ok) {
				n.setEmailStatus(EmailStatus.SENT);
				n.setEmailedAt(LocalDateTime.now());
			}
			else if (n.getEmailAttempts() >= MAX_ATTEMPTS) {
				n.setEmailStatus(EmailStatus.FAILED);
			}
		}));
		return ok;
	}

	private boolean trySend(SimpleMailMessage message, Long notificationId) {
		try {
			mail.send(message);
			return true;
		}
		catch (MailException ex) {
			log.warn("Email for notification {} failed: {}", notificationId, ex.getMessage());
			return false;
		}
	}

	private SimpleMailMessage toMessage(Notification n) {
		User user = users.getById(n.getUserId());
		SimpleMailMessage m = new SimpleMailMessage();
		m.setFrom(from);
		m.setTo(user.getEmail());
		m.setSubject(n.getTitle());
		StringBuilder text = new StringBuilder()
			.append("Hi ").append(user.getFullName()).append(",\n\n")
			.append(n.getBody()).append("\n");
		if (n.getLink() != null) {
			text.append("\nOpen TriVoKo: ").append(frontendUrl).append(n.getLink()).append("\n");
		}
		text.append("\n- TriVoKo\n(You get this email because you use TriVoKo. See all messages under the bell in the shop.)\n");
		m.setText(text.toString());
		return m;
	}

}
