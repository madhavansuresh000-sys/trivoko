package com.trivoko.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.common.Money;
import com.trivoko.order.SellerDashboardController.Dashboard;
import com.trivoko.seller.LedgerService;
import com.trivoko.seller.SellerService;

import lombok.RequiredArgsConstructor;

/**
 * Numbers for the seller's dashboard and sales chart. A "sale" = a package that was PLACED and not cancelled
 * (placed, packed, shipped or delivered), counted on the day it was placed, at the package total.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSalesService {

	private static final EnumSet<PackageStatus> SOLD = EnumSet.of(PackageStatus.PLACED, PackageStatus.PACKED,
			PackageStatus.SHIPPED, PackageStatus.DELIVERED);

	private final OrderPackageRepository packages;

	private final ProductService productService;

	private final LedgerService ledger;

	private final SellerService sellers;

	public Dashboard dashboard(Long sellerId) {
		LocalDate today = LocalDate.now();
		BigDecimal todaySales = packages.findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(sellerId,
				today.atStartOfDay(), SOLD).stream().map(OrderPackage::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Dashboard(sellers.shopName(sellerId),
				packages.countBySellerIdAndStatus(sellerId, PackageStatus.PLACED),
				packages.countBySellerIdAndStatus(sellerId, PackageStatus.PACKED),
				productService.lowStock(sellerId).size(),
				ledger.balance(sellerId),
				productService.countWaitingForApproval(sellerId),
				Money.round(todaySales));
	}

	/**
	 * The chart: one point per day for the last `days` days (today last), days without sales = 0.
	 * Cached for a minute per seller and period (spring.cache.caffeine spec): a chart does not need to be
	 * accurate to the second, and the query reads many packages.
	 */
	@Cacheable(cacheNames = "sellerSales", key = "#sellerId + '-' + #days + '-' + #today")
	public List<DayPoint> daily(Long sellerId, int days, LocalDate today) {
		LocalDate first = today.minusDays(days - 1L);
		Map<LocalDate, DayPoint> points = new LinkedHashMap<>();
		for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
			points.put(d, new DayPoint(d, Money.ZERO, 0));
		}
		for (OrderPackage p : packages.findBySellerIdAndPlacedAtGreaterThanEqualAndStatusIn(sellerId, first.atStartOfDay(), SOLD)) {
			LocalDate day = p.getPlacedAt().toLocalDate();
			points.computeIfPresent(day, (d, old) -> new DayPoint(d, old.total().add(p.getTotal()), old.packages() + 1));
		}
		return new ArrayList<>(points.values());
	}

	public record DayPoint(LocalDate date, BigDecimal total, int packages) {
	}

}
