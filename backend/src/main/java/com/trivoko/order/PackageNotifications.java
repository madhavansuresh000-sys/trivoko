package com.trivoko.order;

import org.springframework.stereotype.Component;

import com.trivoko.common.Money;
import com.trivoko.notify.NotificationKind;
import com.trivoko.notify.NotificationService;
import com.trivoko.seller.SellerService;

import lombok.RequiredArgsConstructor;

/**
 * The message for every package move (spec section 6). Created inside the move's transaction; emails go out
 * after the commit, in the background.
 *
 * <pre>
 *   PACKED                 -> customer, bell only
 *   SHIPPED, DELIVERED     -> customer, bell + email
 *   CANCELLED by seller    -> customer, bell + email (reason + refund)
 *   CANCELLED by customer  -> seller,   bell + email
 * </pre>
 */
@Component
@RequiredArgsConstructor
class PackageNotifications {

	private final NotificationService notifications;

	private final SellerService sellers;

	void moved(OrderPackage p) {
		Order order = p.getOrder();
		String box = "Your " + p.getSellerName() + " package of order " + order.getNumber();
		String link = "/orders/" + order.getNumber();
		switch (p.getStatus()) {
			case PACKED -> notifications.create(order.getUserId(), NotificationKind.PACKAGE_PACKED,
					box + " is packed", p.getSellerName() + " has packed your items. They ship soon.", link, false);
			case SHIPPED -> notifications.create(order.getUserId(), NotificationKind.PACKAGE_SHIPPED,
					box + " is on its way", "Tracking number: " + p.getTrackingNumber() + ".", link, true);
			case DELIVERED -> notifications.create(order.getUserId(), NotificationKind.PACKAGE_DELIVERED,
					box + " is delivered", "Enjoy your purchase! You can return items within 7 days.", link, true);
			case CANCELLED -> cancelled(p, order, box, link);
			default -> {
			}
		}
	}

	private void cancelled(OrderPackage p, Order order, String box, String link) {
		String refund = "Rs " + Money.round(p.getTotal()).toPlainString();
		boolean byCustomer = p.getEvents().getLast().getActor() == PackageEvent.Actor.CUSTOMER;
		if (byCustomer) {
			notifications.create(sellers.ownerOf(p.getSellerId()), NotificationKind.PACKAGE_CANCELLED,
					"Package cancelled by the customer: " + order.getNumber(),
					"Do not ship this package. The customer was refunded " + refund + ".", null, true);
		}
		else {
			notifications.create(order.getUserId(), NotificationKind.PACKAGE_CANCELLED, box + " was cancelled",
					p.getSellerName() + " cancelled it: " + p.getCancelReason() + ". " + refund
							+ " is refunded to your payment method.", link, true);
		}
	}

}
