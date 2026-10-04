package com.trivoko.seller;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.seller.dto.SellerProfile;
import com.trivoko.seller.dto.SellerSummary;

/** The seller module's rules. Other modules call this class, never SellerRepository. */
@Service
@Transactional(readOnly = true)
public class SellerService {

	private final SellerRepository sellers;

	SellerService(SellerRepository sellers) {
		this.sellers = sellers;
	}

	/** A shop page is public only after the admin has approved the shop. */
	public SellerProfile getPublicProfile(String slug) {
		Seller s = sellers.findBySlugAndStatus(slug, SellerStatus.APPROVED)
			.orElseThrow(() -> new ResourceNotFoundException("Seller", slug));
		return new SellerProfile(s.getShopName(), s.getSlug(), s.getCity(), s.getDescription(),
				s.getCreatedAt().toLocalDate());
	}

	/** The shop this user owns (any status), or empty if they never applied. */
	public Optional<SellerSummary> findByOwner(Long userId) {
		return sellers.findByOwnerId(userId).map(SellerService::summary);
	}

	static SellerSummary summary(Seller s) {
		return new SellerSummary(s.getId(), s.getShopName(), s.getSlug(), s.getStatus());
	}

}
