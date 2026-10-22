package com.trivoko.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;
import com.trivoko.order.AdminOrderService.AdminOrderDetail;
import com.trivoko.order.AdminOrderService.AdminOrderRow;

import lombok.RequiredArgsConstructor;

/** Every order, newest first, for the admin (ADMIN only: /api/admin/** in SecurityConfig). Read-only. */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

	private final AdminOrderService adminOrders;

	@GetMapping
	public PageResponse<AdminOrderRow> list(@RequestParam(required = false) String q,
			@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page) {
		return adminOrders.search(q, status, PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Direction.DESC, "id")));
	}

	@GetMapping("/{number}")
	public AdminOrderDetail get(@PathVariable String number) {
		return adminOrders.get(number);
	}

}
