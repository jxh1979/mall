-- 文件版本：V0.01
-- 全会员待追回总额的小额财务豁免；不写积分余额或发行费，不回填批准配置。
ALTER TABLE ticket_reward_config_versions
    DROP CONSTRAINT ticket_reward_config_versions_kind_check,
    DROP CONSTRAINT ticket_reward_config_versions_check;
ALTER TABLE ticket_reward_config_versions
    ADD CONSTRAINT ticket_reward_config_versions_kind_check
        CHECK(kind IN ('points','growth','national','sales_contract','recovery_waiver')),
    ADD CONSTRAINT ticket_reward_config_versions_check
        CHECK((kind IN ('national','recovery_waiver') AND ticket_type_id IS NULL AND project_id IS NULL)
            OR (kind NOT IN ('national','recovery_waiver') AND ticket_type_id IS NOT NULL AND project_id IS NOT NULL));

CREATE TABLE customer_points_recovery_waivers (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    config_version_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    threshold BIGINT NOT NULL CHECK(threshold>=0),
    waived_points BIGINT NOT NULL CHECK(waived_points>0 AND waived_points<=threshold),
    actor_id VARCHAR(64) NOT NULL REFERENCES access_users(id),
    reason TEXT NOT NULL CHECK(length(trim(reason))>0),
    lines JSONB NOT NULL CHECK(jsonb_typeof(lines)='array' AND jsonb_array_length(lines)>0),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_points_recovery_waivers_customer ON customer_points_recovery_waivers(customer_id,created_at);
CREATE TRIGGER points_recovery_waivers_immutable BEFORE UPDATE OR DELETE ON customer_points_recovery_waivers
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status) VALUES
 ('finance.points_recovery.view','查看待追回积分及财务豁免记录','finance','medium',false,'active'),
 ('finance.points_recovery.waive','办理积分小额财务豁免','finance','high',true,'active');
