package com.trivoko.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.trivoko.catalog.dto.CategoryNode;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductDetail;
import com.trivoko.catalog.dto.ProductDetail.CategoryRef;
import com.trivoko.catalog.dto.ProductDetail.ImageView;
import com.trivoko.catalog.dto.ProductDetail.SellerRef;
import com.trivoko.catalog.dto.VariantView;

/**
 * Entity -> DTO. Entities never leave the service layer: the JSON shape is decided here,
 * so a new column in the database does not suddenly appear in the API.
 */
final class CatalogMapper {

	/** Fewer than this many left = show "only N left". */
	static final int LOW_STOCK = 5;

	static ProductCard toCard(Product p) {
		return new ProductCard(
				p.getId(), p.getSlug(), p.getName(), p.getBrand(),
				p.getPriceFrom(), p.getMrpFrom(), discountPercent(p.getPriceFrom(), p.getMrpFrom()),
				p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl(),
				p.getVariants().stream().anyMatch(v -> v.getStock() > 0),
				p.getSeller().getShopName(), p.getSeller().getSlug(),
				p.getCategory().getSlug());
	}

	static ProductDetail toDetail(Product p) {
		return new ProductDetail(
				p.getId(), p.getSlug(), p.getName(), p.getBrand(), p.getDescription(),
				p.getPriceFrom(), p.getMrpFrom(), discountPercent(p.getPriceFrom(), p.getMrpFrom()),
				toRef(p.getCategory()),
				new SellerRef(p.getSeller().getShopName(), p.getSeller().getSlug(), p.getSeller().getCity()),
				p.getVariants().stream().map(CatalogMapper::toView).toList(),
				p.getImages().stream().map(i -> new ImageView(i.getUrl(), i.getAltText())).toList());
	}

	static VariantView toView(ProductVariant v) {
		int stock = v.getStock();
		return new VariantView(v.getId(), v.getSku(), v.getLabel(), v.getSize(), v.getColour(),
				v.getPrice(), v.getMrp(), discountPercent(v.getPrice(), v.getMrp()),
				stock > 0, stock > 0 && stock < LOW_STOCK ? stock : null);
	}

	static CategoryNode toNode(Category c) {
		return new CategoryNode(c.getId(), c.getName(), c.getSlug(),
				c.getChildren().stream().map(CatalogMapper::toNode).toList());
	}

	private static CategoryRef toRef(Category c) {
		return c == null ? null : new CategoryRef(c.getName(), c.getSlug(), toRef(c.getParent()));
	}

	/** MRP 1,999 and price 1,499 -> 25 (% off, rounded down so we never promise more than is true). */
	static int discountPercent(BigDecimal price, BigDecimal mrp) {
		if (price == null || mrp == null || mrp.compareTo(price) <= 0) {
			return 0;
		}
		return mrp.subtract(price).multiply(BigDecimal.valueOf(100))
			.divide(mrp, 0, RoundingMode.DOWN).intValue();
	}

	private CatalogMapper() {
	}

}
