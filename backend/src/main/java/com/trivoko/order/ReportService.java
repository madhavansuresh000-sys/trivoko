package com.trivoko.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.Money;

import lombok.RequiredArgsConstructor;

/**
 * The admin's sales register (Phase 6). A "sale" = a package that was PLACED and not cancelled or expired
 * (placed, packed, shipped or delivered), counted on the day it was placed - the same rule as the seller's chart.
 * Cached for a minute (the global Caffeine spec): reports do not need to be exact to the second.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

	static final EnumSet<PackageStatus> SOLD = EnumSet.of(PackageStatus.PLACED, PackageStatus.PACKED,
			PackageStatus.SHIPPED, PackageStatus.DELIVERED);

	/** TriVoKo keeps 10% of every sale (the seller ledger gets 90%, Phase 5). */
	static final BigDecimal FEE_RATE = new BigDecimal("0.10");

	private static final int TOP = 10;

	private final ReportRepository reports;

	/** One point per day for the last `days` days (today last); days without sales are 0. */
	@Cacheable(cacheNames = "adminReports", key = "'sales-' + #days + '-' + #today")
	public List<DaySales> salesPerDay(int days, LocalDate today) {
		LocalDate first = today.minusDays(days - 1L);
		Map<LocalDate, DaySales> points = new LinkedHashMap<>();
		for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
			points.put(d, new DaySales(d, Money.ZERO, 0L));
		}
		for (DaySales row : reports.salesPerDay(SOLD, first.atStartOfDay())) {
			points.computeIfPresent(row.date(), (d, empty) -> new DaySales(d, Money.round(row.total()), row.packages()));
		}
		return new ArrayList<>(points.values());
	}

	@Cacheable(cacheNames = "adminReports", key = "'products-' + #days + '-' + #today")
	public List<TopProduct> topProducts(int days, LocalDate today) {
		return reports.topProducts(SOLD, today.minusDays(days - 1L).atStartOfDay(), PageRequest.of(0, TOP));
	}

	@Cacheable(cacheNames = "adminReports", key = "'sellers-' + #days + '-' + #today")
	public List<TopSeller> topSellers(int days, LocalDate today) {
		return reports.topSellers(SOLD, today.minusDays(days - 1L).atStartOfDay(), PageRequest.of(0, TOP));
	}

	/** Today's sales for the overview cards (not cached: it is one small query). */
	public DaySales today(LocalDate today) {
		return reports.salesPerDay(SOLD, today.atStartOfDay()).stream().filter(d -> d.date().equals(today)).findFirst()
			.map(d -> new DaySales(d.date(), Money.round(d.total()), d.packages()))
			.orElse(new DaySales(today, Money.ZERO, 0L));
	}

	public record DaySales(LocalDate date, BigDecimal total, Long packages) {
	}

	public record TopProduct(String slug, String name, Long units, BigDecimal revenue) {
	}

	/** fee = TriVoKo's 10% of the shop's sales. */
	public record TopSeller(Long sellerId, String shopName, Long packages, BigDecimal revenue, BigDecimal fee) {

		/** The query fills the first four; the fee is worked out here. */
		public TopSeller(Long sellerId, String shopName, Long packages, BigDecimal revenue) {
			this(sellerId, shopName, packages, revenue, Money.round(revenue.multiply(FEE_RATE)));
		}

	}

}
