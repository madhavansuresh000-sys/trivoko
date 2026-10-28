package com.trivoko.wishlist;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.wishlist.WishlistService.WishlistEntry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** My wishlist (any logged-in user). PUT = add, so pressing the heart twice is safe. */
@RestController
@RequestMapping("/api/me/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Saved products; price-drop alerts watch these")
public class WishlistController {

	private final WishlistService wishlist;

	@GetMapping
	@Operation(summary = "My saved products, newest first, with how much cheaper each is now")
	public List<WishlistEntry> list(@AuthenticationPrincipal AuthUser user) {
		return wishlist.list(user.id());
	}

	@GetMapping("/ids")
	@Operation(summary = "Ids of my saved products (to fill the hearts)")
	public List<Long> ids(@AuthenticationPrincipal AuthUser user) {
		return wishlist.ids(user.id());
	}

	@PutMapping("/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Save a product (at most 100)")
	public void add(@AuthenticationPrincipal AuthUser user, @PathVariable Long productId) {
		try {
			wishlist.add(user.id(), productId);
		}
		catch (DataIntegrityViolationException twice) {
			// two taps at the same instant: UNIQUE(user_id, product_id) kept one row - which is what they wanted
		}
	}

	@DeleteMapping("/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Remove a saved product (no error if it was not saved)")
	public void remove(@AuthenticationPrincipal AuthUser user, @PathVariable Long productId) {
		wishlist.remove(user.id(), productId);
	}

}
