package com.trivoko.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.admin.AuditService;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.Money;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.coupon.dto.CouponRequest;
import com.trivoko.coupon.dto.CouponView;

import lombok.RequiredArgsConstructor;

/**
 * The coupon rules. The discount is worked out on the ITEMS total (delivery is not discounted):
 *   FLAT    = min(value, itemsTotal)                     FLAT100   on Rs 999  -> Rs 100
 *   PERCENT = itemsTotal x value / 100, at most the cap   WELCOME10 on Rs 3497 -> Rs 349.70
 * A coupon works only when it is active, inside its dates, the items total reaches its minimum, and this
 * customer has not used it yet (one use per customer, checked again by a UNIQUE key when the order is paid).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

	private final CouponRepository coupons;

	private final CouponRedemptionRepository redemptions;

	private final AuditService audit;

	/**
	 * What this code gives this customer right now. Never throws for a bad code: the checkout page shows the
	 * reason under the coupon box. discount = 0 when error != null.
	 *
	 * @param usedInPendingOrder answered by the order module (it owns orders): is the code already in the
	 *                           customer's other, unpaid order?
	 */
	public CouponQuote quote(String code, Long userId, BigDecimal itemsTotal, boolean usedInPendingOrder) {
		String normalized = normalize(code);
		Coupon coupon = coupons.findByCode(normalized).orElse(null);
		if (coupon == null) {
			return CouponQuote.refused(normalized, "This coupon does not exist");
		}
		LocalDateTime now = LocalDateTime.now();
		if (!coupon.isActive() || now.isBefore(coupon.getValidFrom())) {
			return CouponQuote.refused(normalized, "This coupon is no longer available");
		}
		if (now.isAfter(coupon.getValidUntil())) {
			return CouponQuote.refused(normalized, "This coupon has expired");
		}
		if (redemptions.existsByCouponIdAndUserId(coupon.getId(), userId)) {
			return CouponQuote.refused(normalized, "You have already used " + normalized);
		}
		if (usedInPendingOrder) {
			return CouponQuote.refused(normalized, normalized + " is already used in your unpaid order");
		}
		if (itemsTotal.compareTo(coupon.getMinOrder()) < 0) {
			return CouponQuote.refused(normalized,
					"Add " + rupees(coupon.getMinOrder().subtract(itemsTotal)) + " more to use " + normalized);
		}
		return new CouponQuote(normalized, discount(coupon, itemsTotal), null);
	}

	/** The order was PAID: the customer has now used this code. */
	@Transactional
	public void redeem(String code, Long userId, Long orderId) {
		Coupon coupon = coupons.findByCode(normalize(code))
			.orElseThrow(() -> new ResourceNotFoundException("Coupon", code));
		if (!redemptions.existsByCouponIdAndUserId(coupon.getId(), userId)) {
			redemptions.save(new CouponRedemption(coupon.getId(), userId, orderId));
		}
	}

	/** GET /api/coupons/{code}: does it exist (amounts come from the checkout preview). */
	public CouponView describe(String code) {
		return coupons.findByCode(normalize(code)).filter(Coupon::isActive).map(CouponService::view)
			.orElseThrow(() -> new ResourceNotFoundException("This coupon does not exist"));
	}

	public List<CouponView> list() {
		return coupons.findAllByOrderByIdDesc().stream().map(CouponService::view).toList();
	}

	@Transactional
	public CouponView create(CouponRequest request) {
		String code = normalize(request.code());
		if (coupons.existsByCode(code)) {
			throw new BusinessRuleException("A coupon " + code + " already exists.");
		}
		if (request.type() == CouponType.PERCENT && request.value().compareTo(new BigDecimal("90")) > 0) {
			throw new BusinessRuleException("A percent coupon can take at most 90% off.");
		}
		Coupon coupon = new Coupon();
		coupon.setCode(code);
		coupon.setDescription(request.description().trim());
		coupon.setType(request.type());
		coupon.setValue(Money.round(request.value()));
		coupon.setMaxDiscount(request.type() == CouponType.PERCENT && request.maxDiscount() != null
				? Money.round(request.maxDiscount()) : null);
		coupon.setMinOrder(Money.round(request.minOrder() == null ? BigDecimal.ZERO : request.minOrder()));
		coupon.setValidFrom(request.validFrom() == null ? LocalDateTime.now() : request.validFrom());
		coupon.setValidUntil(request.validUntil());
		Coupon saved = coupons.save(coupon);
		audit.record("COUPON_CREATED", "COUPON", saved.getId(), code);
		return view(saved);
	}

	private static BigDecimal discount(Coupon coupon, BigDecimal itemsTotal) {
		BigDecimal off = coupon.getType() == CouponType.FLAT
				? coupon.getValue()
				: itemsTotal.multiply(coupon.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		if (coupon.getMaxDiscount() != null) {
			off = off.min(coupon.getMaxDiscount());
		}
		return Money.round(off.min(itemsTotal)); // never more than the items cost
	}

	private static String normalize(String code) {
		return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
	}

	/** "Rs 99" for whole rupees, "Rs 99.50" otherwise (for messages). */
	private static String rupees(BigDecimal amount) {
		BigDecimal r = Money.round(amount);
		return "₹" + (r.signum() == 0 || r.stripTrailingZeros().scale() <= 0 ? r.toBigInteger().toString() : r.toPlainString());
	}

	private static CouponView view(Coupon c) {
		return new CouponView(c.getId(), c.getCode(), c.getDescription(), c.getType(), c.getValue(), c.getMaxDiscount(),
				c.getMinOrder(), c.getValidFrom(), c.getValidUntil(), c.isActive());
	}

	/** The answer for one code: discount 0 and a reason when it cannot be used. */
	public record CouponQuote(String code, BigDecimal discount, String error) {

		static CouponQuote refused(String code, String error) {
			return new CouponQuote(code, Money.ZERO, error);
		}

		public boolean valid() {
			return error == null;
		}

	}

}
