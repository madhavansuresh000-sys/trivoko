package com.trivoko.seller.dto;

import com.trivoko.seller.SellerStatus;

/** A short description of a user's own shop, e.g. for "who am I" (/api/auth/me). */
public record SellerSummary(Long id, String shopName, String slug, SellerStatus status) {
}
