package com.trivoko.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	/** The top row of the tree (children are loaded from each one). */
	List<Category> findByParentIsNullOrderBySortOrder();

}
