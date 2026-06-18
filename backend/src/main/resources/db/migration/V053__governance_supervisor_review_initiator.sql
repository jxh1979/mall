-- Segregation of duties for supervisor review: record who initiated the reviewed action so the
-- approval path can reject self-approval (the initiator approving their own blocked operation).
ALTER TABLE supervisor_review_decisions
    ADD COLUMN IF NOT EXISTS initiated_by VARCHAR(64);
