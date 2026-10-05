package com.trivoko.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	/** For the cart: the variants with their product, shop and photos in one query (no N+1). */
	@EntityGraph(attributePaths = { "product", "product.seller", "product.images" })
	List<ProductVariant> findByIdIn(Collection<Long> ids);

	/**
	 * Checkout: take stock in ONE atomic statement. MySQL locks the row while it checks "stock >= qty", so two
	 * customers can never both take the last unit. 1 = taken, 0 = not enough left.
	 */
	@Modifying(flushAutomatically = true)
	@Query("update ProductVariant v set v.stock = v.stock - :qty where v.id = :id and v.stock >= :qty")
	int take(@Param("id") Long id, @Param("qty") int qty);

	/** Expired order or refund: the stock goes back on the shelf. */
	@Modifying(flushAutomatically = true)
	@Query("update ProductVariant v set v.stock = v.stock + :qty where v.id = :id")
	int giveBack(@Param("id") Long id, @Param("qty") int qty);

	/** The stock in the database right now (not a cached entity). */
	@Query("select v.stock from ProductVariant v where v.id = :id")
	Integer currentStock(@Param("id") Long id);

}
