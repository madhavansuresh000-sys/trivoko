package com.trivoko.seller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.Money;
import com.trivoko.common.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * The seller's account book. When a package is DELIVERED the seller earns 90% of what the customer paid for
 * that package; TriVoKo keeps 10% (the marketplace fee). No bank payouts - the balance is what TriVoKo owes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

	static final BigDecimal SELLER_SHARE = new BigDecimal("0.90");

	private final LedgerRepository ledger;

	/** +90% of the package total, once per package (the UNIQUE key also stops a double click). */
	@Transactional
	public void recordSale(Long sellerId, Long packageId, BigDecimal packageTotal, String orderNumber) {
		if (ledger.existsByPackageIdAndKind(packageId, LedgerEntry.Kind.SALE)) {
			return;
		}
		BigDecimal amount = packageTotal.multiply(SELLER_SHARE).setScale(2, RoundingMode.HALF_UP);
		ledger.save(new LedgerEntry(sellerId, packageId, orderNumber, LedgerEntry.Kind.SALE, amount,
				"Delivered: " + orderNumber + " (90% of Rs " + Money.round(packageTotal).toPlainString() + ")"));
	}

	public BigDecimal balance(Long sellerId) {
		return Money.round(ledger.balance(sellerId));
	}

	public LedgerPage page(Long sellerId, Pageable pageable) {
		return new LedgerPage(balance(sellerId), PageResponse.from(ledger.findBySellerIdOrderByIdDesc(sellerId, pageable),
				l -> new Line(l.getId(), l.getKind(), l.getAmount(), l.getNote(), l.getOrderNumber(), l.getCreatedAt())));
	}

	public record LedgerPage(BigDecimal balance, PageResponse<Line> lines) {
	}

	public record Line(Long id, LedgerEntry.Kind kind, BigDecimal amount, String note, String orderNumber,
			LocalDateTime createdAt) {
	}

}
