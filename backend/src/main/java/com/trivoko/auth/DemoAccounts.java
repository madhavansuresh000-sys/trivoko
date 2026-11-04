package com.trivoko.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The one-click demo accounts of the live site (Phase 12, decision D13) and whether they are switched on.
 * Used by DemoLoginController (the buttons) and DemoGuard (what those shared accounts may not change).
 */
@Component
public class DemoAccounts {

	/** Button order = list order. Never the admin: one stranger could block every shop on the live demo. */
	static final List<DemoAccount> ALL = List.of(
			new DemoAccount("customer", "ravi@trivoko.test", "Ravi", "Customer - shop, pay, track, return"),
			new DemoAccount("seller", "chennai.mobiles@trivoko.test", "Chennai Mobiles", "Seller - pack, ship, earnings"));

	private final boolean enabled;

	public DemoAccounts(@Value("${app.demo.one-click:false}") boolean oneClick,
			@Value("${app.demo.password:}") String demoPassword) {
		// no DEMO_PASSWORD = the demo accounts are locked (DevDataSeeder), so no back door around that either
		this.enabled = oneClick && !demoPassword.isBlank();
	}

	public boolean enabled() {
		return enabled;
	}

	/** The buttons to show; empty when switched off. */
	public List<DemoAccount> list() {
		return enabled ? ALL : List.of();
	}

	public Optional<DemoAccount> byKey(String key) {
		return list().stream().filter(a -> a.key().equals(key)).findFirst();
	}

	/** True when this email is one of the SHARED one-click accounts (and they are switched on). */
	public boolean isShared(String email) {
		return list().stream().anyMatch(a -> a.email().equals(email));
	}

	/** One button: key for the URL, who it is, and one line on what to try. */
	public record DemoAccount(String key, String email, String name, String tryThis) {
	}

}
