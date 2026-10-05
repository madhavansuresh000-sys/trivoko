package com.trivoko.order;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.trivoko.common.Money;
import com.trivoko.notify.NotificationKind;
import com.trivoko.notify.NotificationService;
import com.trivoko.seller.SellerService;

import lombok.RequiredArgsConstructor;

/**
 * The messages an order sends when it is PAID (bell + email, sent after the commit):
 *   the customer : "Order TV-100123 is placed"            - what they paid, how many packages
 *   each seller  : "New package to pack: TV-100123 (...)" - ONLY their own items (they never see the others)
 */
@Component
@RequiredArgsConstructor
class OrderNotifications {

	private final NotificationService notifications;

	private final SellerService sellers;

	void paid(Order order) {
		int boxes = order.getPackages().size();
		notifications.create(order.getUserId(), NotificationKind.ORDER_PLACED,
				"Order " + order.getNumber() + " is placed",
				"Thank you! You paid Rs " + Money.round(order.getGrandTotal()).toPlainString() + " once. Your order comes in "
						+ boxes + (boxes == 1 ? " package" : " packages") + ", one from each seller: "
						+ order.getPackages().stream().map(OrderPackage::getSellerName).collect(Collectors.joining(", ")) + ".",
				"/orders/" + order.getNumber(), true);

		for (OrderPackage p : order.getPackages()) {
			String items = p.getItems().stream()
				.map(i -> i.getQuantity() + " x " + i.getProductName() + " (" + i.getVariantLabel() + ")")
				.collect(Collectors.joining("\n"));
			notifications.create(sellers.ownerOf(p.getSellerId()), NotificationKind.NEW_PACKAGE,
					"New package to pack: " + order.getNumber() + " (" + p.getSellerName() + ")",
					"Please pack and ship:\n" + items + "\n\nDeliver to: " + order.getShipCity() + ", "
							+ order.getShipState() + " " + order.getShipPincode() + ".",
					null, true); // the seller dashboard link comes in Phase 5
		}
	}

}
