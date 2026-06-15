ALTER TABLE analytics_ai_quality_tasks
    DROP CONSTRAINT IF EXISTS chk_analytics_ai_quality_tasks_mode;

ALTER TABLE analytics_ai_quality_tasks
    ADD CONSTRAINT chk_analytics_ai_quality_tasks_mode
    CHECK (processing_mode IN ('manual_review', 'rule_engine', 'external_ai_pending', 'local_llm_suggestion'));
