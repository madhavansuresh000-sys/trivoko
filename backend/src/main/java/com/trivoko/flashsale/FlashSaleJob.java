package com.trivoko.flashsale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Every 30 seconds: give back claims nobody checked out, hand unsold units back after a sale, tidy the limiter. */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class FlashSaleJob {

	private final FlashClaimService claims;

	private final FlashSaleService sales;

	private final ClaimRateLimiter limiter;

	@Scheduled(fixedDelayString = "${app.flash.job-every}", initialDelayString = "${app.flash.job-every}")
	public void run() {
		claims.expireHeld();
		sales.settleEnded();
		limiter.forgetQuiet();
	}

}
