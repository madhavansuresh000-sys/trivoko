package com.trivoko.notify.dto;

import java.time.LocalDateTime;

import com.trivoko.notify.Notification;
import com.trivoko.notify.NotificationKind;

/** One line under the bell. */
public record NotificationView(Long id, NotificationKind kind, String title, String body, String link,
		LocalDateTime createdAt, boolean read) {

	public static NotificationView from(Notification n) {
		return new NotificationView(n.getId(), n.getKind(), n.getTitle(), n.getBody(), n.getLink(), n.getCreatedAt(),
				n.getReadAt() != null);
	}

}
