package com.trivoko.search;

import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.trivoko.catalog.Product;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

/**
 * Builds the search index from the products table every time the app starts. After that Hibernate Search keeps it
 * up to date by itself whenever a product is saved. Because of this rebuild, the index folder can always be deleted
 * safely - the database is the truth, the index is only a fast copy of its words.
 */
@Component
@RequiredArgsConstructor
public class SearchIndexer {

	private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

	private final EntityManagerFactory emf;

	@EventListener(ApplicationReadyEvent.class)
	public void rebuild() throws InterruptedException {
		long start = System.currentTimeMillis();
		Search.mapping(emf).scope(Product.class).massIndexer().startAndWait();
		log.info("Search index rebuilt in {} ms", System.currentTimeMillis() - start);
	}

}
