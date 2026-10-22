package com.trivoko.order;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.trivoko.order.ReportService.DaySales;
import com.trivoko.order.ReportService.TopProduct;
import com.trivoko.order.ReportService.TopSeller;

/**
 * The admin's reports (Phase 6): GROUP BY queries, so MySQL adds up the numbers and sends back only the
 * totals (one row per day / product / shop), not every package.
 */
interface ReportRepository extends Repository<OrderPackage, Long> {

	/** One row per day that had sales: day, sum of package totals, number of packages. */
	@Query("""
			select new com.trivoko.order.ReportService$DaySales(extract(date from p.placedAt), sum(p.total), count(p))
			from OrderPackage p
			where p.status in :sold and p.placedAt >= :from
			group by extract(date from p.placedAt)""")
	List<DaySales> salesPerDay(Collection<PackageStatus> sold, LocalDateTime from);

	/** Best sellers by money: units and item value (before the coupon) per product. */
	@Query("""
			select new com.trivoko.order.ReportService$TopProduct(i.productSlug, i.productName, sum(i.quantity), sum(i.lineTotal))
			from OrderItem i join i.orderPackage p
			where p.status in :sold and p.placedAt >= :from
			group by i.productSlug, i.productName
			order by sum(i.lineTotal) desc, i.productSlug""")
	List<TopProduct> topProducts(Collection<PackageStatus> sold, LocalDateTime from, Pageable limit);

	/** Shops by money: packages and the sum of their totals. */
	@Query("""
			select new com.trivoko.order.ReportService$TopSeller(p.sellerId, p.sellerName, count(p), sum(p.total))
			from OrderPackage p
			where p.status in :sold and p.placedAt >= :from
			group by p.sellerId, p.sellerName
			order by sum(p.total) desc, p.sellerId""")
	List<TopSeller> topSellers(Collection<PackageStatus> sold, LocalDateTime from, Pageable limit);

}
