-- 文件版本：V0.01
-- M009-013/038：历史资料保持为空，不推断项目或公司归属。
ALTER TABLE projects
    ADD COLUMN owner_person TEXT,
    ADD COLUMN address TEXT,
    ADD COLUMN business_scope TEXT;

ALTER TABLE merchants
    ADD COLUMN project_id VARCHAR(64) REFERENCES projects(id);

CREATE INDEX idx_merchants_project_id ON merchants(project_id);

CREATE TABLE business_entity_projects (
    business_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    PRIMARY KEY (business_entity_id, project_id)
);

CREATE INDEX idx_business_entity_projects_project_id
    ON business_entity_projects(project_id);
