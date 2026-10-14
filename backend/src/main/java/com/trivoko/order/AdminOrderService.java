package com.trivoko.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.Likes;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.order.dto.OrderViews;
import com.trivoko.order.dto.OrderViews.OrderDetail;
import com.trivoko.user.User;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * The admin's view of orders (Phase 6): every customer's orders, read-only. The search box takes either an
 * order number ("TV-1001" finds TV-100123) or a customer's full email ("ravi@trivoko.test").
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

	private final OrderRepository orders;

	private final UserService users;

	public PageResponse<AdminOrderRow> search(String q, OrderStatus status, Pageable pageable) {
		String number = null;
		Long userId = null;
		if (q != null && q.contains("@")) {
			User customer = users.findByEmail(q).orElse(null);
			if (customer == null) {
				return PageResponse.from(Page.<Order>empty(pageable), o -> null); // no such customer = no orders
			}
			userId = customer.getId();
		}
		else {
			number = Likes.contains(q);
		}
		Page<Order> page = orders.adminSearch(status, number, userId, pageable);
		Set<Long> customerIds = page.getContent().stream().map(Order::getUserId).collect(Collectors.toSet());
		Map<Long, String> emails = users.emailsOf(customerIds); // one query for the whole page
		return PageResponse.from(page, o -> row(o, emails.get(o.getUserId())));
	}

	public AdminOrderDetail get(String number) {
		Order order = orders.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Order", number));
		return new AdminOrderDetail(users.getById(order.getUserId()).getEmail(), OrderViews.detail(order));
	}

	/** The admin overview: orders created since this moment (paid or not). */
	public long countCreatedSince(LocalDateTime from) {
		return orders.countByCreatedAtGreaterThanEqual(from);
	}

	private static AdminOrderRow row(Order o, String customerEmail) {
		return new AdminOrderRow(o.getNumber(), o.getStatus(), customerEmail, o.getGrandTotal(), o.getPackages().size(),
				o.getCouponCode(), o.getCreatedAt(), o.getPaidAt());
	}

	public record AdminOrderRow(String number, OrderStatus status, String customerEmail, BigDecimal grandTotal,
			int packageCount, String couponCode, LocalDateTime createdAt, LocalDateTime paidAt) {
	}

	public record AdminOrderDetail(String customerEmail, OrderDetail order) {
	}

}
