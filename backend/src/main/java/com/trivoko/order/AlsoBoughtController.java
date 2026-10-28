package com.trivoko.order;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductRef;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * "Customers also bought" under the product page (Phase 10 step 6). Lives in the order module because the answer
 * comes from order lines (order calls catalog, never the other way round). Public: GET /api/products/** is.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Catalogue")
public class AlsoBoughtController {

	/** At most this many cards under the product. */
	static final int SHOW = 6;

	private final AlsoBoughtRanking ranking;

	private final ProductService productService;

	@GetMapping("/api/products/{slug}/also-bought")
	@Operation(summary = "Up to 6 products bought in the same paid orders, most often first")
	public List<ProductCard> alsoBought(@PathVariable String slug) {
		ProductRef product = productService.liveRef(slug);
		List<Long> others = ranking.otherVariants(product.id(), product.variantIds());
		return productService.liveCardsOfVariants(others, product.id(), SHOW);
	}

}
