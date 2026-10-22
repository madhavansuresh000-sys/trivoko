package com.trivoko.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.trivoko.order.OrderItem;
import com.trivoko.order.OrderPackage;
import com.trivoko.order.PackageStatus;

/**
 * A package as its SELLER sees it: only their own items, the ship-to city (the full address is printed on the
 * label in a later phase), and what is left to do.
 */
public record SellerPackageView(Long id, String orderNumber, PackageStatus status, LocalDateTime placedAt,
		List<Line> items, BigDecimal total, String trackingNumber, String cancelReason, String shipName,
		String shipCity, String shipPincode) {

	public record Line(String productName, String variantLabel, int quantity) {
	}

	public static SellerPackageView of(OrderPackage p) {
		return new SellerPackageView(p.getId(), p.getOrder().getNumber(), p.getStatus(), p.getPlacedAt(),
				p.getItems().stream().map((OrderItem i) -> new Line(i.getProductName(), i.getVariantLabel(), i.getQuantity())).toList(),
				p.getTotal(), p.getTrackingNumber(), p.getCancelReason(), p.getOrder().getShipName(),
				p.getOrder().getShipCity(), p.getOrder().getShipPincode());
	}

}
