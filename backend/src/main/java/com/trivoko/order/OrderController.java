package com.trivoko.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.PageResponse;
import com.trivoko.order.dto.OrderViews.OrderDetail;
import com.trivoko.order.dto.OrderViews.OrderSummary;

import lombok.RequiredArgsConstructor;

/** My orders (logged in; only my own - someone else's number is 404). Placing an order: CheckoutController. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping
	public PageResponse<OrderSummary> mine(@AuthenticationPrincipal AuthUser user,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return orderService.mine(user.id(), PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
	}

	@GetMapping("/{number}")
	public OrderDetail one(@AuthenticationPrincipal AuthUser user, @PathVariable String number) {
		return orderService.mine(user.id(), number);
	}

}
