package com.trivoko.seller;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.seller.dto.SellerProfile;

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

}
