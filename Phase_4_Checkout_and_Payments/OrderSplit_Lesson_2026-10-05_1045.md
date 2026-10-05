# ★ Your task: the Order Split (Phase 4, steps 4 and 5)

MADHAVAN, this is the heart of TriVoKo, the part an interviewer will ask about. **You type it**; I explain
and then review. Two files:

1. `backend/src/test/java/com/trivoko/order/OrderSplitterTest.java` - **your test** (write this FIRST)
2. `backend/src/main/java/com/trivoko/order/OrderSplitter.java` - **the body of `split(...)`** (the empty
   class and the records are ready; only the method body is yours)

## 1. The idea in one picture

Food court: you pay once at the counter, then the dosa stall and the juice stall each get their own token.

```
 Ravi's cart                                        ONE order  TV-100123   pay Rs 3,147.30
 ┌──────────────────────────────────────┐           ┌───────────────────────────────────────┐
 │ Silicone case  Rs 499 x 2  (Chennai)  │  split    │ Package 1  Chennai Mobiles             │
 │ Kovai Run Pro  Rs 2,499 x 1 (Kovai)   │ ───────►  │   case x 2   998.00  share  99.80      │
 │ coupon WELCOME10 = 10% (Rs 349.70)    │           │   items 998.00 - 99.80 + delivery 0    │
 └──────────────────────────────────────┘           │   = 898.20                             │
                                                    │ Package 2  Kovai Sports                │
                                                    │   shoes x 1  2499.00  share 249.90     │
                                                    │   items 2499.00 - 249.90 + delivery 0  │
                                                    │   = 2249.10                            │
                                                    └───────────────────────────────────────┘
                                                      898.20 + 2249.10 = 3147.30  ✓
```

## 2. The rules (exactly what your code must do)

| # | Rule | Example |
|---|---|---|
| 1 | One package per seller, in the order the sellers **first appear** in the lines | Chennai first, Kovai second |
| 2 | `lineTotal = unitPrice × quantity` | 499 × 2 = 998.00 |
| 3 | `itemsTotal` of the order = sum of all line totals | 998 + 2499 = 3497.00 |
| 4 | Each item's **discount share** = `discount × lineTotal ÷ itemsTotal`, rounded to 2 places (HALF_UP) | 349.70 × 998 ÷ 3497 = 99.80 |
| 5 | The **last item of the whole order** gets the remainder instead: `discount − (sum of the other shares)` | 349.70 − 99.80 = 249.90 |
| 6 | Package discount = sum of its items' shares | Chennai 99.80 |
| 7 | **Delivery per package**: package items total (before discount) ≥ ₹499 → 0.00, else 40.00 | 998 ≥ 499 → free |
| 8 | Package total = items total − discount + delivery | 998 − 99.80 + 0 = 898.20 |
| 9 | Order totals = sums over packages; grandTotal = sum of package totals | 3147.30 |

Why rule 5? Rounding each share can lose or add a paisa. Example: a ₹1 coupon shared by three ₹1 items =
0.33 + 0.33 + 0.33 = 0.99, so one paisa is lost. Giving the last item the remainder (0.34) makes the shares
**always** add up to the discount. Why store a share on every item? In Phase 8, when Ravi returns only the shoes,
the refund is exactly `2499.00 − 249.90 = 2249.10`.

## 3. Java tools you will need

```
BigDecimal  - never double for money (0.1 + 0.2 = 0.30000000000000004 with double!)
  a.add(b)  a.subtract(b)  a.multiply(BigDecimal.valueOf(qty))
  a.divide(b, 2, RoundingMode.HALF_UP)        // divide with 2 decimals
  a.compareTo(b) >= 0                          // "a >= b" for BigDecimal (never use equals for money)
  BigDecimal.ZERO, new BigDecimal("0.00")

LinkedHashMap<Long, List<SplitLine>>         // groups by seller AND keeps the first-seen order
  map.computeIfAbsent(sellerId, id -> new ArrayList<>()).add(line);

com.trivoko.common.Money.round(x)            // 2 places, HALF_UP (already written)
```

## 4. Suggested steps (write them in English as comments first, then Java under each)

1. Work out `itemsTotal` of the whole order (rule 2 + 3).
2. Group the lines by seller with a `LinkedHashMap` (rule 1).
3. Keep a running `sharedSoFar` (starts at 0) and remember which line is the **last line of the order**.
4. For every seller (outer loop), for every line (inner loop): compute lineTotal and share (rule 4, or rule 5 for
   the last line), add `ItemPlan` to the package's list, add the share to `sharedSoFar`.
5. After each seller's lines: package items total, discount, delivery (rule 7), total → `PackagePlan`.
6. At the end: add up the packages → `SplitResult`.

Tip for rule 5: "last line of the order" = the last line of the last seller's group, which is the
last element you visit in the loops. A counter of visited lines compared with `lines.size()` works.

## 5. Your test first (TDD)

Create `OrderSplitterTest.java` in `backend/src/test/java/com/trivoko/order/`. Start like this, then write the
assertions yourself:

```java
package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.trivoko.order.OrderSplitter.SplitLine;
import com.trivoko.order.OrderSplitter.SplitResult;

class OrderSplitterTest {

    private static SplitLine line(long variantId, long sellerId, String seller, String price, int qty) {
        return new SplitLine(variantId, sellerId, seller, "slug-" + variantId, "Product " + variantId,
                "Variant", new BigDecimal(price), new BigDecimal(price), qty);
    }

    @Test
    void twoSellersAndACoupon() {
        SplitResult r = OrderSplitter.split(List.of(
                line(1, 1, "Chennai Mobiles", "499.00", 2),
                line(2, 2, "Kovai Sports", "2499.00", 1)), new BigDecimal("349.70"));

        // ★ your assertions: 2 packages in the right order, shares 99.80 and 249.90,
        //   delivery 0 and 0, package totals 898.20 and 2249.10, grandTotal 3147.30
        //   hint: assertThat(r.grandTotal()).isEqualByComparingTo("3147.30");
    }

    // ★ your second test: one cheap package (e.g. Rs 200, no coupon) pays Rs 40 delivery
}
```

## 6. How to run

1. Run the test **before** writing `split` → it must FAIL ("waiting for Madhavan's code"). That is RED.
2. Write `split`, run again → GREEN.
3. In IntelliJ: green ▶ next to the class. Or in a terminal:

```bash
cd backend && ./mvnw.cmd -q test -Dtest=OrderSplitterTest -Djacoco.skip=true
```

4. Tell me **"split done"**. I review it like a senior developer would and add a few extra tests
   (one line only, no coupon, three Rs 1 items with a Rs 1 coupon).

Stuck for more than 15 minutes on one step? Tell me which step - I give a hint, not the answer.
