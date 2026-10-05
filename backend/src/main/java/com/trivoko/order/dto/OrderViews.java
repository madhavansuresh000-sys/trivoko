package com.trivoko.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.trivoko.order.Order;
import com.trivoko.order.OrderItem;
import com.trivoko.order.OrderPackage;
import com.trivoko.order.OrderStatus;
import com.trivoko.order.PackageStatus;

/** My orders (list) and one order (detail page, invoice). */
public final class OrderViews {

	private OrderViews() {
	}

	public record OrderSummary(String number, OrderStatus status, LocalDateTime createdAt, BigDecimal grandTotal,
			int packageCount, int itemCount, String firstItemName) {
	}

	public record OrderDetail(String number, OrderStatus status, LocalDateTime createdAt, LocalDateTime paidAt,
			LocalDateTime holdExpiresAt, BigDecimal itemsTotal, BigDecimal discountTotal, BigDecimal shippingTotal,
			BigDecimal grandTotal, String couponCode, Order.ShipTo shipTo, List<PackageDetail> packages) {
	}

	public record PackageDetail(Long id, String sellerName, PackageStatus status, BigDecimal itemsTotal,
			BigDecimal discount, BigDecimal shippingFee, BigDecimal total, List<ItemDetail> items) {
	}

	public record ItemDetail(Long variantId, String productSlug, String productName, String variantLabel,
			BigDecimal unitPrice, BigDecimal mrp, int quantity, BigDecimal lineTotal, BigDecimal discountShare) {
	}

	public static OrderSummary summary(Order o) {
		List<OrderItem> items = o.getPackages().stream().flatMap(p -> p.getItems().stream()).toList();
		return new OrderSummary(o.getNumber(), o.getStatus(), o.getCreatedAt(), o.getGrandTotal(), o.getPackages().size(),
				items.stream().mapToInt(OrderItem::getQuantity).sum(), items.isEmpty() ? "" : items.getFirst().getProductName());
	}

	public static OrderDetail detail(Order o) {
		return new OrderDetail(o.getNumber(), o.getStatus(), o.getCreatedAt(), o.getPaidAt(), o.getHoldExpiresAt(),
				o.getItemsTotal(), o.getDiscountTotal(), o.getShippingTotal(), o.getGrandTotal(), o.getCouponCode(),
				o.shipTo(), o.getPackages().stream().map(OrderViews::pack).toList());
	}

	private static PackageDetail pack(OrderPackage p) {
		return new PackageDetail(p.getId(), p.getSellerName(), p.getStatus(), p.getItemsTotal(), p.getDiscount(),
				p.getShippingFee(), p.getTotal(), p.getItems().stream().map(i -> new ItemDetail(i.getVariantId(),
						i.getProductSlug(), i.getProductName(), i.getVariantLabel(), i.getUnitPrice(), i.getMrp(),
						i.getQuantity(), i.getLineTotal(), i.getDiscountShare())).toList());
	}

}
