-- Phase 6: the admin panel.
-- A blocked shop is told WHY (shown on its application page, like the reject reason).
ALTER TABLE sellers
    ADD COLUMN block_reason VARCHAR(300) NULL;

-- the admin's activity log is read newest first, often filtered by action
CREATE INDEX ix_audit_log_action ON audit_log (action, id);
