package com.trivoko.wishlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Every 10 minutes (app.wishlist.alert-every): sends the price-drop alerts. Waiting is the point - five price
 * edits in one minute reach the job as one marked row, so the customer gets ONE message with the final price.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PriceAlertJob {

	private static final Logger log = LoggerFactory.getLogger(PriceAlertJob.class);

	private final PriceAlertService alerts;

	@Scheduled(fixedDelayString = "${app.wishlist.alert-every:10m}", initialDelayString = "${app.wishlist.alert-every:10m}")
	public void run() {
		int sent = alerts.runOnce();
		if (sent > 0) {
			log.info("{} price-drop alert(s) sent", sent);
		}
	}

}
