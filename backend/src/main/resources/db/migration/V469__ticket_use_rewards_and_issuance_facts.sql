-- 文件版本：V0.01
-- TK-01/02/03、FS-022：核销奖励与合同销售方发行费事实。
CREATE TABLE ticket_reward_config_versions (
    id VARCHAR(64) PRIMARY KEY,
    kind VARCHAR(24) NOT NULL CHECK(kind IN ('points','growth','national','sales_contract')),
    ticket_type_id VARCHAR(64) REFERENCES ticket_types(id),
    project_id VARCHAR(64),
    version VARCHAR(24) NOT NULL CHECK(version ~ '^V[0-9]+\.[0-9]{2}$'),
    payload JSONB NOT NULL CHECK(jsonb_typeof(payload)='object'),
    effective_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL CHECK(status IN ('pending_review','approved','rejected')),
    review_id VARCHAR(128) UNIQUE,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    CHECK ((kind='national' AND ticket_type_id IS NULL AND project_id IS NULL)
        OR (kind<>'national' AND ticket_type_id IS NOT NULL AND project_id IS NOT NULL)),
    CHECK(published_at IS NULL OR published_at<effective_at)
);
CREATE UNIQUE INDEX uq_ticket_reward_config_effective ON ticket_reward_config_versions
    (kind,COALESCE(ticket_type_id,''),COALESCE(project_id,''),effective_at);
CREATE UNIQUE INDEX uq_ticket_reward_config_version ON ticket_reward_config_versions
    (kind,COALESCE(ticket_type_id,''),COALESCE(project_id,''),version);

CREATE TABLE ticket_credential_presenters (
    credential_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_credentials(id),
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE ticket_order_sales_contracts (
    order_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_orders(id),
    contract_version_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    project_id VARCHAR(64) NOT NULL,
    seller_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    seller_merchant_id VARCHAR(64) REFERENCES merchants(id),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE ticket_use_rewards (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES ticket_orders(id),
    parent_ticket_id VARCHAR(64) NOT NULL REFERENCES ticket_instances(id),
    slot_index INTEGER NOT NULL CHECK(slot_index>0),
    use_index INTEGER NOT NULL CHECK(use_index>0),
    verification_id VARCHAR(64) NOT NULL REFERENCES ticket_verifications(id),
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    project_id VARCHAR(64) NOT NULL,
    points_rule_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    growth_rule_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    wait_rule_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    points BIGINT NOT NULL CHECK(points>=0),
    growth BIGINT NOT NULL CHECK(growth>=0),
    waiting_hours INTEGER NOT NULL CHECK(waiting_hours>=0),
    verified_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL CHECK(status IN ('pending','confirmed','cancelled','reversed')),
    confirmed_at TIMESTAMPTZ,
    confirmed_points BIGINT NOT NULL DEFAULT 0 CHECK(confirmed_points>=0 AND confirmed_points<=points),
    reversed_at TIMESTAMPTZ,
    reversal_source VARCHAR(128),
    UNIQUE(verification_id)
);
CREATE UNIQUE INDEX uq_ticket_reward_active_use ON ticket_use_rewards(parent_ticket_id,slot_index,use_index)
    WHERE status IN ('pending','confirmed');
CREATE TABLE ticket_reward_uses (
    verification_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_verifications(id),
    reward_id VARCHAR(64) NOT NULL REFERENCES ticket_use_rewards(id)
);
CREATE INDEX idx_ticket_rewards_pending ON ticket_use_rewards(available_at,id) WHERE status='pending';
CREATE INDEX idx_ticket_rewards_customer ON ticket_use_rewards(customer_id,verified_at,id);
CREATE TABLE customer_growth_ledger (
    id VARCHAR(80) PRIMARY KEY,
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    reward_id VARCHAR(64) NOT NULL REFERENCES ticket_use_rewards(id),
    change_type VARCHAR(24) NOT NULL CHECK(change_type IN ('earn','reverse')),
    growth_delta BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(reward_id,change_type)
);
CREATE TABLE customer_points_recoveries (
    reward_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_use_rewards(id),
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    original_points BIGINT NOT NULL CHECK(original_points>0),
    remaining_points BIGINT NOT NULL CHECK(remaining_points>=0 AND remaining_points<=original_points),
    status VARCHAR(24) NOT NULL CHECK(status IN ('pending','recovered','waived','cancelled')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE ticket_points_issuance_fees (
    reward_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_use_rewards(id),
    price_version_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    seller_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    seller_merchant_id VARCHAR(64) REFERENCES merchants(id),
    receiver_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    project_id VARCHAR(64) NOT NULL,
    points BIGINT NOT NULL CHECK(points>0),
    unit_price_cent NUMERIC(18,6) NOT NULL CHECK(unit_price_cent>0),
    amount_cent BIGINT NOT NULL CHECK(amount_cent>=0),
    confirmed_at TIMESTAMPTZ NOT NULL
);
CREATE TRIGGER ticket_growth_immutable BEFORE UPDATE OR DELETE ON customer_growth_ledger
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TRIGGER ticket_fee_immutable BEFORE UPDATE OR DELETE ON ticket_points_issuance_fees
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TRIGGER ticket_sales_snapshot_immutable BEFORE UPDATE OR DELETE ON ticket_order_sales_contracts
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TRIGGER ticket_presenter_immutable BEFORE UPDATE OR DELETE ON ticket_credential_presenters
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TRIGGER ticket_reward_use_immutable BEFORE UPDATE OR DELETE ON ticket_reward_uses
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE FUNCTION protect_ticket_reward_config() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' OR (to_jsonb(NEW)-ARRAY['status','review_id','approved_at','published_at'])
       IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','review_id','approved_at','published_at']) THEN
        RAISE EXCEPTION '奖励规则版本事实不能覆盖或删除' USING ERRCODE='23514';
    END IF;
    IF OLD.published_at IS NOT NULL AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION '已公告奖励规则只允许追加新版本' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER ticket_reward_config_immutable BEFORE UPDATE OR DELETE ON ticket_reward_config_versions
    FOR EACH ROW EXECUTE FUNCTION protect_ticket_reward_config();

INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status) VALUES
 ('ticket.reward.config.view','查看门票奖励规则与销售合同','ticketing','medium',false,'active'),
 ('ticket.reward.config.manage','提交门票奖励规则与销售合同','ticketing','high',true,'active');
INSERT INTO role_permissions(role_id,permission_code) VALUES
 ('role_platform_admin','ticket.reward.config.view'),('role_platform_admin','ticket.reward.config.manage');
-- 审批策略由根代理按域内、跨域、全国参数、合同各自的现有审批配置接入。

