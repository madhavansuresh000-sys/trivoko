package com.trivoko.search;

import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.trivoko.catalog.Product;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

/**
 * Builds the search index from the products table every time the app starts. After that Hibernate Search keeps it
 * up to date by itself whenever a product is saved. Because of this rebuild, the index folder can always be deleted
 * safely - the database is the truth, the index is only a fast copy of its words.
 * <p>
 * WHEN it runs matters: first it ran on ApplicationReadyEvent, which comes AFTER Tomcat opens the door - for about
 * 2 seconds after a start every search answered "nothing found" (seen in the browser check, 29 Oct). As a
 * SmartLifecycle with an early phase it now runs BEFORE the web server starts taking requests.
 */
@Component
@RequiredArgsConstructor
public class SearchIndexer implements SmartLifecycle {

	/** Lower = earlier. Spring Boot starts the web server at DEFAULT_PHASE - 1024, long after this. */
	static final int PHASE = 0;

	private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

	private final EntityManagerFactory emf;

	private volatile boolean running;

	@Override
	public void start() {
		long started = System.currentTimeMillis();
		try {
			Search.mapping(emf).scope(Product.class).massIndexer().startAndWait();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Search index rebuild was interrupted", e);
		}
		running = true;
		log.info("Search index rebuilt in {} ms", System.currentTimeMillis() - started);
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return PHASE;
	}

}
