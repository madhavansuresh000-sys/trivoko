package com.trivoko.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Every minute: unpaid orders older than 10 minutes expire and their stock goes back on sale.
 * Like IRCTC Tatkal: the seat is kept while you pay; if the payment page times out, it goes back to the pool.
 * Switched off in tests (app.scheduling.enabled=false); tests call expireOldHolds() themselves. (From EventHub.)
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OrderExpiryJob {

	private final OrderService orders;

	@Scheduled(fixedDelayString = "${app.order.expiry-check}", initialDelayString = "${app.order.expiry-check}")
	public void run() {
		orders.expireOldHolds();
	}

}
