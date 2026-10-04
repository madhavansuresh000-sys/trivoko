package com.trivoko.order;

import com.trivoko.catalog.ProductRepository;

/**
 * TEST FIXTURE - breaks the module rule on purpose (the order module uses the catalog's repository).
 * ModuleRulesTest feeds it to the rule to prove the rule really catches this mistake.
 * It lives in test code only, so the real application never contains it.
 */
class ModuleRuleBreaker {

	private final ProductRepository products;

	ModuleRuleBreaker(ProductRepository products) {
		this.products = products;
	}

	long countProducts() {
		return products.count();
	}

}
