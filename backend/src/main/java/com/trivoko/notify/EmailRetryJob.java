package com.trivoko.notify;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Every minute: emails that could not be sent yet (mail server was down). Off in tests. */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmailRetryJob {

	private final EmailSender emails;

	@Scheduled(fixedDelayString = "${app.mail.retry-every}", initialDelayString = "${app.mail.retry-every}")
	public void run() {
		emails.sendPending(LocalDateTime.now().minusMinutes(1));
	}

}
