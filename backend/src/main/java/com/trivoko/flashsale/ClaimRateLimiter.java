package com.trivoko.flashsale;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.trivoko.common.TooManyRequestsException;

/**
 * "Slow down": at most N claim tries per person and per IP address in a short window (a sliding window kept in
 * memory). It stops a script from hammering the claim button; the atomic UPDATE is what keeps the stock right.
 * Over the limit -> 429 and a Retry-After header. One server only; with many servers the counters would move to
 * Redis (Phase 12 note).
 */
@Component
public class ClaimRateLimiter {

	private final long windowMillis;

	private final int perUser;

	private final int perIp;

	private final ConcurrentHashMap<String, Deque<Long>> tries = new ConcurrentHashMap<>();

	ClaimRateLimiter(@Value("${app.flash.rate-limit.window}") Duration window,
			@Value("${app.flash.rate-limit.per-user}") int perUser, @Value("${app.flash.rate-limit.per-ip}") int perIp) {
		this.windowMillis = window.toMillis();
		this.perUser = perUser;
		this.perIp = perIp;
	}

	/** Counts this try, or throws 429 when the person or the address is over the limit. */
	public void check(Long userId, String ip) {
		long now = System.currentTimeMillis();
		hit("u:" + userId, perUser, now);
		hit("ip:" + ip, perIp, now);
	}

	private void hit(String key, int limit, long now) {
		long[] retry = { 0 };
		tries.compute(key, (k, window) -> {
			Deque<Long> times = window == null ? new ArrayDeque<>() : window;
			while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
				times.pollFirst();
			}
			if (times.size() >= limit) {
				retry[0] = Math.max(1, (windowMillis - (now - times.peekFirst()) + 999) / 1000);
			}
			else {
				times.addLast(now);
			}
			return times;
		});
		if (retry[0] > 0) {
			throw new TooManyRequestsException("Too many tries. Please wait " + retry[0] + " seconds.", retry[0]);
		}
	}

	/** For tests: everybody starts with a clean sheet. */
	void reset() {
		tries.clear();
	}

	/** Called by the minute job: forget people who have been quiet. */
	void forgetQuiet() {
		long now = System.currentTimeMillis();
		for (String key : tries.keySet()) {
			tries.computeIfPresent(key, (k, times) -> times.isEmpty() || now - times.peekLast() >= windowMillis ? null : times);
		}
	}

}
