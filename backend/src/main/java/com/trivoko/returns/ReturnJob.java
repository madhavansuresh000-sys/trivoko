package com.trivoko.returns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Every 10 minutes: returns a seller left unanswered for 2 days are approved (tests call the service directly). */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReturnJob {

	private static final Logger log = LoggerFactory.getLogger(ReturnJob.class);

	private final ReturnService returns;

	@Scheduled(fixedDelayString = "${app.returns.job-every:10m}", initialDelayString = "${app.returns.job-every:10m}")
	public void run() {
		int approved = returns.autoApproveOld();
		if (approved > 0) {
			log.info("{} return(s) approved automatically (no seller answer in 2 days)", approved);
		}
	}

}
