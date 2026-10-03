package com.trivoko.catalog;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** JpaSpecificationExecutor = findAll(filters, page) for the product listing (ProductSpecifications). */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySlug(String slug);

	/** The listing: seller and category come in the same query (every product card shows them). */
	@Override
	@EntityGraph(attributePaths = { "seller", "category" })
	Page<Product> findAll(Specification<Product> spec, Pageable pageable);

}
