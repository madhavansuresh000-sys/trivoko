# Your turn (★ Phase 8 steps 2-3): the return state machine

You already know this shape: `order/PackageStatus.java` from Phase 5. A return works the same way -
**one method decides which move is legal; everything else is refused.**

## The picture (who can press what)

```
 REQUESTED --seller approves / job after 2 days--> APPROVED --seller "picked up"--> PICKED_UP
     |                                                                                |
     |                                                       refund ok  <-------------+-------------> refund fails
     |                                                          |                                        |
     |                                                       REFUNDED  <------- admin "retry" ----- REFUND_FAILED
     +--seller rejects--> REJECTED --customer escalates (once)--> ESCALATED --admin--> APPROVED  or  CLOSED
     +--customer cancels--> CANCELLED
```

End states (no way out): REFUNDED, CLOSED, CANCELLED.

## What you type

### 1. In `backend/src/main/java/com/trivoko/returns/ReturnStatus.java`

The nine values are already there. Under them, add:

- `public Set<ReturnStatus> allowedNext()` - a `switch (this)` that returns an `EnumSet` for every value
  (use the table above; end states return `EnumSet.noneOf(ReturnStatus.class)`).
- `public boolean canMoveTo(ReturnStatus next)` - one line.

Tip: put a `;` after `CANCELLED` before you add the methods (an enum with methods needs it).

### 2. A new class `ReturnService` in the same package (just the one method for now)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

    private final ReturnRequestRepository returns;

    /** The ONLY way a return changes status. */
    @Transactional
    public ReturnRequest moveTo(ReturnRequest ret, ReturnStatus next, ReturnEvent.Actor actor, String note) {
        // 1. if the move is not allowed -> throw new BusinessRuleException("A " + ... + " return cannot move to " + ...)
        // 2. change the status
        // 3. add a timeline line  (ret.addEvent(...))
        // 4. saveAndFlush - so two clicks at the same moment fail HERE (@Version), before any money moves
    }
}
```

### 3. ★ Step 3 - your tests: `backend/src/test/java/com/trivoko/returns/ReturnStatusTest.java`

A plain JUnit test (no Spring needed - fast):

- every legal move from the table -> `canMoveTo` is true (a `@ParameterizedTest` with `@CsvSource` is neat here);
- a few illegal ones are false: REFUNDED -> APPROVED, CLOSED -> ESCALATED, PICKED_UP -> CANCELLED, REJECTED -> APPROVED
  (the customer must escalate first);
- every end state has an empty `allowedNext()`.

## Explain-it-back (answer in your own words when we review)

1. Why is it safer to have ONE `allowedNext()` than `if` checks inside every button's method?
2. Why `saveAndFlush` *before* the refund call, not after?
3. REJECTED -> ESCALATED is allowed only once. Which field helps you check "once"? (hint: `escalatedAt`)

When you are done: run `.\mvnw.cmd test -Dtest=ReturnStatusTest` and tell me - I review, then build steps 4-10 on top.
