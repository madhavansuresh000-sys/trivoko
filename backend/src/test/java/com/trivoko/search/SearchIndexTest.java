package com.trivoko.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.trivoko.catalog.Product;

import jakarta.persistence.EntityManager;

/**
 * Phase 10 steps 1-2: the Lucene index is built when the app starts (every product), it understands upper/lower
 * case, and a product saved through Hibernate is re-indexed by itself.
 */
@SpringBootTest
class SearchIndexTest {

	@Autowired
	private EntityManager em;

	@Autowired
	private TransactionTemplate tx;

	@Autowired
	private JdbcTemplate jdbc;

	private List<Long> idsMatching(String field, String words) {
		return tx.execute(status -> Search.session(em).search(Product.class)
			.select(f -> f.id(Long.class))
			.where(f -> f.match().field(field).matching(words))
			.fetchHits(500));
	}

	@Test
	void everyProductIsInTheIndexAfterStart() {
		long inIndex = tx.execute(status -> Search.session(em).search(Product.class).where(f -> f.matchAll()).fetchTotalHitCount());
		assertThat(inIndex).isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class));
	}

	@Autowired
	private SearchIndexer indexer;

	@Test
	void theIndexIsBuiltBeforeTheWebServerTakesRequests() {
		// Spring Boot starts the web server in phase DEFAULT_PHASE - 1024; a lower phase starts earlier.
		// (Found in the browser check: on ApplicationReadyEvent, searches in the first ~2 s found nothing.)
		assertThat(indexer.isRunning()).isTrue();
		assertThat(indexer.getPhase()).isLessThan(org.springframework.context.SmartLifecycle.DEFAULT_PHASE - 1024);
	}

	@Test
	void capitalLettersDoNotMatter() {
		List<Long> volta = jdbc.queryForList("SELECT id FROM products WHERE brand = 'Volta'", Long.class);
		assertThat(idsMatching("brand", "VOLTA")).containsExactlyInAnyOrderElementsOf(volta);
		assertThat(idsMatching("name", "volta v12")).contains(
				jdbc.queryForObject("SELECT id FROM products WHERE slug = 'volta-v12-5g'", Long.class));
	}

	@Test
	void theCategoryNameIsSearchableToo() {
		long phones = jdbc.queryForObject("""
				SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id WHERE c.name = 'Mobile phones'""",
				Long.class);
		assertThat(phones).isPositive();
		assertThat(idsMatching("category.name", "mobile phones")).hasSizeGreaterThanOrEqualTo((int) phones);
	}

	@Test
	void aRenamedProductIsFoundByItsNewNameAtOnce() {
		long id = jdbc.queryForObject("SELECT id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		String oldName = jdbc.queryForObject("SELECT name FROM products WHERE id = ?", String.class, id);
		try {
			tx.executeWithoutResult(status -> em.find(Product.class, id).setName("Zorblax Galaxy Phone"));
			assertThat(idsMatching("name", "zorblax")).containsExactly(id);
		}
		finally {
			tx.executeWithoutResult(status -> em.find(Product.class, id).setName(oldName));
		}
		assertThat(idsMatching("name", "zorblax")).isEmpty();
	}

}
