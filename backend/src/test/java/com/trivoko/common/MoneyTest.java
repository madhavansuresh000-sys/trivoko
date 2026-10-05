package com.trivoko.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Rupees and paise (Stripe counts in paise: ₹1 = 100 paise). */
class MoneyTest {

	@Test
	void rupeesBecomePaise() {
		assertThat(Money.toPaise(new BigDecimal("15400.00"))).isEqualTo(1_540_000L);
		assertThat(Money.toPaise(new BigDecimal("99.5"))).isEqualTo(9_950L);
		assertThat(Money.toPaise(new BigDecimal("3147.30"))).isEqualTo(314_730L);
	}

	@Test
	void roundIsTwoPlacesHalfUp() {
		assertThat(Money.round(new BigDecimal("99.805"))).isEqualByComparingTo("99.81");
		assertThat(Money.round(new BigDecimal("99.804"))).isEqualByComparingTo("99.80");
		assertThat(Money.round(new BigDecimal("5")).scale()).isEqualTo(2);
	}

	@Test
	void missingAmountIsAProgrammingError() {
		assertThatThrownBy(() -> Money.toPaise(null)).isInstanceOf(IllegalArgumentException.class);
	}

}
