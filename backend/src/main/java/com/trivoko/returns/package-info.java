/**
 * Module <b>returns</b> - the return state machine.
 * <p>
 * Owns: return_requests, return_events (V10). Writes REFUND lines in seller_ledger only through LedgerService.
 * Other modules may call this module's services, never its repositories or tables.
 * Built in Phase 8.
 */
package com.trivoko.returns;
