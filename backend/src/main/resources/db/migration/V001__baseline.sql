CREATE TABLE IF NOT EXISTS platform_migration_baseline (
    id BIGSERIAL PRIMARY KEY,
    baseline_code VARCHAR(64) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO platform_migration_baseline (baseline_code, description)
VALUES ('BE-000', 'Formal Java Spring Boot PostgreSQL Flyway backend baseline')
ON CONFLICT (baseline_code) DO NOTHING;
