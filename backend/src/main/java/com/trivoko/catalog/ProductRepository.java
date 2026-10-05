package com.trivoko.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.trivoko.catalog.dto.BrandCount;

/** JpaSpecificationExecutor = findAll(filters, page) for the product listing (ProductSpecifications). */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySlug(String slug);

	/** The listing: seller and category come in the same query (every product card shows them). */
	@Override
	@EntityGraph(attributePaths = { "seller", "category" })
	Page<Product> findAll(Specification<Product> spec, Pageable pageable);

	/** Brands of the visible products (ACTIVE, shop APPROVED) with a count - the brand filter. */
	@Query("""
			select new com.trivoko.catalog.dto.BrandCount(p.brand, count(p)) from Product p
			where p.status = com.trivoko.catalog.ProductStatus.ACTIVE
			  and p.seller.status = com.trivoko.seller.SellerStatus.APPROVED
			group by p.brand order by p.brand""")
	List<BrandCount> countVisibleByBrand();

	/** Same, only in these categories (a top category + its sub-categories). */
	@Query("""
			select new com.trivoko.catalog.dto.BrandCount(p.brand, count(p)) from Product p
			where p.status = com.trivoko.catalog.ProductStatus.ACTIVE
			  and p.seller.status = com.trivoko.seller.SellerStatus.APPROVED
			  and p.category.id in :categoryIds
			group by p.brand order by p.brand""")
	List<BrandCount> countVisibleByBrandIn(Collection<Long> categoryIds);

}
