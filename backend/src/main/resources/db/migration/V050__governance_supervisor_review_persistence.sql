CREATE TABLE IF NOT EXISTS supervisor_review_decisions (
    review_id         VARCHAR(64)   PRIMARY KEY,
    policy_code       VARCHAR(100)  NOT NULL,
    action_type       VARCHAR(100)  NOT NULL,
    object_type       VARCHAR(100)  NOT NULL,
    object_id         VARCHAR(256)  NOT NULL,
    blocked           BOOLEAN       NOT NULL,
    review_status     VARCHAR(50)   NOT NULL,
    required_level    INT           NOT NULL,
    reviewer_level    INT,
    decision          VARCHAR(50),
    reason            TEXT,
    evidence_attachment_ids JSONB   NOT NULL DEFAULT '[]',
    evaluated_at      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_supervisor_review_decisions_pending
    ON supervisor_review_decisions (review_status)
    WHERE review_status = 'pending_review';
