package com.trivoko.notify;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on @Async: emails are sent on a background thread (Spring Boot's "applicationTaskExecutor"), so a
 * slow mail server never makes the customer's "Pay" request wait. Found in the browser check: before this,
 * three emails x a 5-second timeout made the payment page give up although the order WAS paid.
 */
@Configuration
@EnableAsync
class AsyncConfig {
}
