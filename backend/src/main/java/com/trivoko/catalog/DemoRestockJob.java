package com.trivoko.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Live demo only (DEMO_ONE_CLICK=true, decision D13): every night at 03:30 the sample shops are restocked, like a
 * shopkeeper refilling the shelves before opening, so a recruiter tomorrow never finds the demo shop sold out.
 * Off on a laptop and in tests (both properties must be "true").
 */
@Component
@ConditionalOnProperty(name = { "app.scheduling.enabled", "app.demo.one-click" }, havingValue = "true")
@RequiredArgsConstructor
public class DemoRestockJob {

	private static final Logger log = LoggerFactory.getLogger(DemoRestockJob.class);

	private final ProductService productService;

	@Scheduled(cron = "0 30 3 * * *")
	public void run() {
		int sizes = productService.restockDemoStock();
		if (sizes > 0) {
			log.info("Demo shop restocked: {} sizes back to their starting stock", sizes);
		}
	}

}
