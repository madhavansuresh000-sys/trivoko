package com.trivoko.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	/** For the cart: the variants with their product, shop and photos in one query (no N+1). */
	@EntityGraph(attributePaths = { "product", "product.seller", "product.images" })
	List<ProductVariant> findByIdIn(Collection<Long> ids);

}
