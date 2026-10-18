-- 文件版本：V0.01
-- 无可信领取时刻沿排班上一级审批；原事件保留，不回填历史记录。
ALTER TABLE staff_work_phone_contexts
    ADD COLUMN checkout_attendance_id VARCHAR(64) REFERENCES staff_attendance_records(id);

CREATE TABLE staff_attendance_receipt_reviews (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL REFERENCES staff_work_phone_events(id),
    context_id VARCHAR(64) NOT NULL REFERENCES staff_work_phone_contexts(id),
    shift_id VARCHAR(64) NOT NULL REFERENCES staff_shift_schedules(id),
    shift_updated_at TIMESTAMPTZ NOT NULL,
    actual_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL CHECK (btrim(reason)<>''),
    handover_evidence TEXT NOT NULL CHECK (btrim(handover_evidence)<>''),
    original_event JSONB NOT NULL,
    review_id VARCHAR(64) NOT NULL UNIQUE REFERENCES supervisor_review_decisions(review_id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('pending','approved','rejected')),
    initiated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    applied_at TIMESTAMPTZ,
    CHECK ((status='approved') = (applied_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_staff_receipt_pending_review
    ON staff_attendance_receipt_reviews(event_id) WHERE status='pending';
CREATE INDEX idx_staff_receipt_review_event
    ON staff_attendance_receipt_reviews(event_id,created_at);

CREATE FUNCTION enforce_staff_receipt_review_immutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'staff receipt review evidence cannot be deleted';
    END IF;
    IF OLD.status <> 'pending' THEN
        RAISE EXCEPTION 'final staff receipt review cannot be changed';
    END IF;
    IF ROW(NEW.id, NEW.event_id, NEW.context_id, NEW.shift_id, NEW.shift_updated_at,
           NEW.actual_at, NEW.reason, NEW.handover_evidence, NEW.original_event,
           NEW.review_id, NEW.initiated_by, NEW.created_at)
       IS DISTINCT FROM
       ROW(OLD.id, OLD.event_id, OLD.context_id, OLD.shift_id, OLD.shift_updated_at,
           OLD.actual_at, OLD.reason, OLD.handover_evidence, OLD.original_event,
           OLD.review_id, OLD.initiated_by, OLD.created_at) THEN
        RAISE EXCEPTION 'staff receipt review original facts cannot be changed';
    END IF;
    IF NEW.status NOT IN ('approved', 'rejected') OR NEW.decided_at IS NULL
       OR ((NEW.status = 'approved') <> (NEW.applied_at IS NOT NULL)) THEN
        RAISE EXCEPTION 'staff receipt review must conclude once from pending';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_staff_receipt_review_immutable
    BEFORE UPDATE OR DELETE ON staff_attendance_receipt_reviews
    FOR EACH ROW EXECUTE FUNCTION enforce_staff_receipt_review_immutable();

ALTER TABLE staff_attendance_receipt_reviews
    ADD CONSTRAINT ck_staff_receipt_review_decision_time
    CHECK ((status = 'pending') = (decided_at IS NULL));
