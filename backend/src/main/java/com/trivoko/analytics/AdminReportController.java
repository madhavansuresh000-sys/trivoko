package com.trivoko.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.ProductStatus;
import com.trivoko.common.BadRequestException;
import com.trivoko.order.AdminOrderService;
import com.trivoko.order.ReportService;
import com.trivoko.order.ReportService.DaySales;
import com.trivoko.order.ReportService.TopProduct;
import com.trivoko.order.ReportService.TopSeller;
import com.trivoko.seller.SellerService;
import com.trivoko.seller.SellerStatus;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * The admin's numbers (ADMIN only: /api/admin/** in SecurityConfig). This module owns no tables: it only asks
 * the other modules' services and puts the answers side by side.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReportController {

	private static final Set<Integer> PERIODS = Set.of(7, 30, 90);

	private final ReportService reports;

	private final SellerService sellers;

	private final ProductService products;

	private final UserService users;

	private final AdminOrderService orders;

	/** The overview cards: what waits for the admin, and how today is going. */
	@GetMapping("/overview")
	public Overview overview() {
		LocalDate today = LocalDate.now();
		DaySales sales = reports.today(today);
		return new Overview(sellers.countByStatus(SellerStatus.PENDING), products.countByStatus(ProductStatus.PENDING),
				sellers.countByStatus(SellerStatus.APPROVED), sellers.countByStatus(SellerStatus.BLOCKED),
				users.countUsers(), orders.countCreatedSince(today.atStartOfDay()), sales.packages(), sales.total());
	}

	@GetMapping("/reports/sales")
	public List<DaySales> sales(@RequestParam(defaultValue = "30") int days) {
		return reports.salesPerDay(period(days), LocalDate.now());
	}

	@GetMapping("/reports/top-products")
	public List<TopProduct> topProducts(@RequestParam(defaultValue = "30") int days) {
		return reports.topProducts(period(days), LocalDate.now());
	}

	@GetMapping("/reports/top-sellers")
	public List<TopSeller> topSellers(@RequestParam(defaultValue = "30") int days) {
		return reports.topSellers(period(days), LocalDate.now());
	}

	private static int period(int days) {
		if (!PERIODS.contains(days)) {
			throw new BadRequestException("days must be 7, 30 or 90");
		}
		return days;
	}

	/** returnRate is not here yet: returns arrive in Phase 8. */
	public record Overview(long pendingShops, long pendingProducts, long approvedShops, long blockedShops, long users,
			long ordersToday, long packagesSoldToday, BigDecimal salesToday) {
	}

}
