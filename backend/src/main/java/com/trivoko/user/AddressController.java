package com.trivoko.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.user.dto.AddressRequest;
import com.trivoko.user.dto.AddressView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** "My addresses" for the logged-in user (SecurityConfig: login needed). */
@RestController
@RequestMapping("/api/me/addresses")
@RequiredArgsConstructor
public class AddressController {

	private final AddressService addressService;

	@GetMapping
	public List<AddressView> list(@AuthenticationPrincipal AuthUser user) {
		return addressService.list(user.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AddressView add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddressRequest request) {
		return addressService.add(user.id(), request);
	}

	@PutMapping("/{id}")
	public AddressView update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
			@Valid @RequestBody AddressRequest request) {
		return addressService.update(user.id(), id, request);
	}

	@PutMapping("/{id}/default")
	public AddressView makeDefault(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
		return addressService.makeDefault(user.id(), id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
		addressService.delete(user.id(), id);
	}

}
