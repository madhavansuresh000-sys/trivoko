package com.trivoko.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.catalog.dto.CategoryNode;
import com.trivoko.catalog.dto.BrandCount;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductDetail;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The public catalogue: anyone can browse, no login needed. */
@RestController
@RequestMapping("/api")
@Tag(name = "Catalogue", description = "Browse products and categories (public)")
public class ProductController {

	/** Biggest page we send, so nobody can ask for all products at once. */
	static final int MAX_PAGE_SIZE = 48;

	private final ProductService productService;
	private final CategoryService categoryService;

	ProductController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping("/products")
	@Operation(summary = "List products with filters, sorting and paging")
	public PageResponse<ProductCard> list(
			@Parameter(description = "Category slug; a top category includes its sub-categories", example = "phones")
			@RequestParam(required = false) String category,
			@Parameter(description = "One or more brands", example = "Volta")
			@RequestParam(name = "brand", required = false) List<String> brands,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@Parameter(description = "true = hide products that are sold out")
			@RequestParam(defaultValue = "false") boolean inStock,
			@Parameter(description = "Seller slug (shop page)", example = "chennai-mobiles")
			@RequestParam(required = false) String seller,
			@Parameter(description = "newest or price") @RequestParam(defaultValue = "newest") String sort,
			@Parameter(description = "asc or desc (for price)") @RequestParam(defaultValue = "asc") String dir,
			@Parameter(description = "Search words; every word must be in the name or the brand", example = "volta case")
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Products per page, at most 48") @RequestParam(defaultValue = "24") int size) {

		if (page < 0) {
			throw new BadRequestException("page must be 0 or more");
		}
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BadRequestException("minPrice must not be greater than maxPrice");
		}
		int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
		ProductSearch search = new ProductSearch(category, brands, minPrice, maxPrice, inStock, seller, q);
		return productService.search(search, PageRequest.of(page, pageSize, toSort(sort, dir)));
	}

	/** Declared before /products/{slug}; Spring prefers the fixed path "brands" over the {slug} pattern anyway. */
	@GetMapping("/products/brands")
	@Operation(summary = "Brands with product counts, for the brand filter")
	public List<BrandCount> brands(
			@Parameter(description = "Category slug; empty = the whole shop", example = "phones")
			@RequestParam(required = false) String category) {
		return productService.brands(category);
	}

	@GetMapping("/products/{slug}")
	@Operation(summary = "One product with its variants, photos and seller")
	public ProductDetail get(@PathVariable String slug) {
		return productService.getBySlug(slug);
	}

	@GetMapping("/categories")
	@Operation(summary = "The category tree")
	public List<CategoryNode> categories() {
		return categoryService.tree();
	}

	/** id is always the last sort key, so two products with the same price keep a stable order across pages. */
	private static Sort toSort(String sort, String dir) {
		Sort.Direction direction = switch (dir) {
			case "asc" -> Sort.Direction.ASC;
			case "desc" -> Sort.Direction.DESC;
			default -> throw new BadRequestException("dir must be asc or desc");
		};
		return switch (sort) {
			case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
			case "price" -> Sort.by(new Sort.Order(direction, "priceFrom"), new Sort.Order(direction, "id"));
			default -> throw new BadRequestException("sort must be newest or price");
		};
	}

}
