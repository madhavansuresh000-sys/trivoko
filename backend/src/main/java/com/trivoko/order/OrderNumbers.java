package com.trivoko.order;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Order numbers people can read on the phone: "TV-" + 6 digits (TV-100000 .. TV-999999).
 * Random (not 1, 2, 3 ...), so nobody can guess how many orders the shop has, or someone else's number.
 */
@Component
@RequiredArgsConstructor
class OrderNumbers {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final OrderRepository orders;

	String next() {
		String number;
		do {
			number = "TV-" + (100_000 + RANDOM.nextInt(900_000));
		}
		while (orders.existsByNumber(number)); // the UNIQUE key is the final guard
		return number;
	}

}
