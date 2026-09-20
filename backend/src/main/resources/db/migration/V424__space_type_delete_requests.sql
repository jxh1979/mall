CREATE TABLE IF NOT EXISTS space_type_delete_requests (
    id TEXT PRIMARY KEY,
    space_type_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    requested_by TEXT NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL
);
