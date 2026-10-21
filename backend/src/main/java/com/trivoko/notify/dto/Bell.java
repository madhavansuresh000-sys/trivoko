package com.trivoko.notify.dto;

import java.util.List;

/** The bell: how many unread, and the newest 20. */
public record Bell(long unread, List<NotificationView> items) {
}
