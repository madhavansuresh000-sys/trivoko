package com.trivoko.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.user.dto.AddressRequest;
import com.trivoko.user.dto.AddressView;

import lombok.RequiredArgsConstructor;

/**
 * A user's delivery addresses. Two rules live here:
 *   - at most 5 addresses per user
 *   - exactly one is the default (the first one added; after deleting the default, the newest left)
 * Every method takes the logged-in user's id, and only ever finds THAT user's addresses, so someone
 * else's address simply "does not exist" (404) for you.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

	static final int MAX_ADDRESSES = 5;

	private final AddressRepository addresses;

	@Transactional(readOnly = true)
	public List<AddressView> list(Long userId) {
		return addresses.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(AddressService::view).toList();
	}

	@Transactional
	public AddressView add(Long userId, AddressRequest request) {
		long count = addresses.countByUserId(userId);
		if (count >= MAX_ADDRESSES) {
			throw new BusinessRuleException("You can save at most " + MAX_ADDRESSES + " addresses. Delete one first.");
		}
		Address address = new Address();
		address.setUserId(userId);
		copy(request, address);
		address.setDefault(count == 0); // the first address is the default
		return view(addresses.save(address));
	}

	@Transactional
	public AddressView update(Long userId, Long id, AddressRequest request) {
		Address address = mine(userId, id);
		copy(request, address);
		return view(address);
	}

	@Transactional
	public AddressView makeDefault(Long userId, Long id) {
		Address chosen = mine(userId, id);
		addresses.findByUserIdOrderByCreatedAtDescIdDesc(userId).forEach(a -> a.setDefault(a == chosen));
		return view(chosen);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		Address address = mine(userId, id);
		addresses.delete(address);
		addresses.flush();
		if (address.isDefault()) {
			// the list is newest first, so the first remaining one is the newest
			addresses.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().findFirst().ifPresent(a -> a.setDefault(true));
		}
	}

	/** Checkout (order module): one of MY addresses; someone else's id is simply "not found" (404). */
	@Transactional(readOnly = true)
	public AddressView get(Long userId, Long id) {
		return view(mine(userId, id));
	}

	/** Checkout without a chosen address: my default one, if I have any. */
	@Transactional(readOnly = true)
	public java.util.Optional<AddressView> defaultOf(Long userId) {
		return addresses.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().filter(Address::isDefault)
			.findFirst().map(AddressService::view);
	}

	private Address mine(Long userId, Long id) {
		return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Address", id));
	}

	private static void copy(AddressRequest r, Address a) {
		a.setName(r.name().trim());
		a.setPhone(r.phone());
		a.setLine1(r.line1().trim());
		a.setLine2(r.line2() == null || r.line2().isBlank() ? null : r.line2().trim());
		a.setCity(r.city().trim());
		a.setState(r.state().trim());
		a.setPincode(r.pincode());
	}

	private static AddressView view(Address a) {
		return new AddressView(a.getId(), a.getName(), a.getPhone(), a.getLine1(), a.getLine2(), a.getCity(),
				a.getState(), a.getPincode(), a.isDefault());
	}

}
