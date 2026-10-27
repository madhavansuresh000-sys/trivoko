package com.trivoko.catalog;

import java.math.BigDecimal;

/** The price changes of one product in a time window: lowest old price, lowest new price, how many changes. */
public record PriceWindow(BigDecimal lowestOld, BigDecimal lowestNew, long changes) {
}
