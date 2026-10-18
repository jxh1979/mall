-- 文件版本：V0.01
CREATE TABLE site_entry_report_history (
    report_id VARCHAR(64) NOT NULL REFERENCES site_entry_reports(id),
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    action VARCHAR(24) NOT NULL CHECK (action IN ('completion','acceptance')),
    actor_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    attachment_ids JSONB NOT NULL CHECK (jsonb_typeof(attachment_ids) = 'array' AND jsonb_array_length(attachment_ids) > 0),
    note TEXT,
    result VARCHAR(24),
    completion_sequence INTEGER,
    PRIMARY KEY (report_id,sequence),
    UNIQUE (report_id,completion_sequence),
    FOREIGN KEY (report_id,completion_sequence) REFERENCES site_entry_report_history(report_id,sequence),
    CHECK ((action='completion' AND result IS NULL AND completion_sequence IS NULL)
        OR (action='acceptance' AND result IS NOT NULL AND result IN ('pass','rectify')
            AND completion_sequence IS NOT NULL AND completion_sequence < sequence))
);
CREATE TRIGGER site_entry_history_immutable BEFORE UPDATE OR DELETE ON site_entry_report_history
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
