package com.trivoko.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.BadRequestException;
import com.trivoko.order.SellerSalesService.DayPoint;
import com.trivoko.seller.SellerAccess;

import lombok.RequiredArgsConstructor;

/**
 * The seller's dashboard (sketch 6) and the sales chart. In the order module because most numbers are about
 * packages; the balance and the product counts come from the seller and catalogue services.
 */
@RestController
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
public class SellerDashboardController {

	private static final Set<Integer> PERIODS = Set.of(7, 30, 90);

	private final SellerAccess sellerAccess;

	private final SellerSalesService sales;

	public record Dashboard(String shopName, long toPack, long toShip, int lowStock, BigDecimal balance,
			long waitingForApproval, BigDecimal todaySales) {
	}

	@GetMapping("/api/seller/dashboard")
	public Dashboard dashboard() {
		return sales.dashboard(sellerAccess.currentSellerId());
	}

	/** One point per day, oldest first, today last. days = 7, 30 or 90. */
	@GetMapping("/api/seller/sales")
	public List<DayPoint> sales(@RequestParam(defaultValue = "7") int days) {
		if (!PERIODS.contains(days)) {
			throw new BadRequestException("days must be 7, 30 or 90");
		}
		return sales.daily(sellerAccess.currentSellerId(), days, LocalDate.now());
	}

}
