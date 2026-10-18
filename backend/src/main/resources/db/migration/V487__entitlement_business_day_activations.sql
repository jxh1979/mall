-- 文件版本：V0.01
-- TK17: 已核销的完整游乐营业日激活原件。旧 V227 自然日记录保持原状，不推测综合体或回填。
CREATE TABLE entitlement_business_day_activations (
    id VARCHAR(64) PRIMARY KEY,
    entitlement_id VARCHAR(64) NOT NULL REFERENCES issued_entitlements(id),
    complex_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    business_day_version_id VARCHAR(64) NOT NULL REFERENCES amusement_business_day_configs(id),
    business_date DATE NOT NULL,
    business_start_at TIMESTAMPTZ NOT NULL,
    business_end_at TIMESTAMPTZ NOT NULL,
    first_activated_at TIMESTAMPTZ NOT NULL,
    first_amusement_verification_id VARCHAR(64) REFERENCES amusement_verifications(id),
    first_ticket_verification_id VARCHAR(64) REFERENCES ticket_verifications(id),
    used_times_after INTEGER NOT NULL CHECK (used_times_after > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('active','reversed')),
    reversed_at TIMESTAMPTZ,
    CONSTRAINT chk_ebda_window CHECK (business_start_at <= first_activated_at AND first_activated_at < business_end_at),
    CONSTRAINT chk_ebda_reversal CHECK ((status='active' AND reversed_at IS NULL) OR (status='reversed' AND reversed_at IS NOT NULL))
);
CREATE INDEX idx_ebda_current ON entitlement_business_day_activations(entitlement_id, complex_id, business_end_at) WHERE status='active';
CREATE UNIQUE INDEX uq_ebda_first_admission ON entitlement_business_day_activations(first_amusement_verification_id) WHERE first_amusement_verification_id IS NOT NULL;
CREATE UNIQUE INDEX uq_ebda_active_window ON entitlement_business_day_activations(entitlement_id, complex_id, business_start_at) WHERE status='active';
COMMENT ON TABLE entitlement_business_day_activations IS '每次消费激活的完整营业日原件；重入先沿用仍有效的已锁窗口，不因后来改版重复消费；冲正保留原件。';

-- 游乐直连按真实项目/场次授权，可没有物理设备或点位；交叉事实不伪造二者。
-- 通用票务核销仍保持设备、点位必填。SQL CHECK 显式处理 source_type 为 NULL，避免 UNKNOWN 放行。
ALTER TABLE ticket_verifications ALTER COLUMN device_id DROP NOT NULL;
ALTER TABLE ticket_verifications ALTER COLUMN point_id DROP NOT NULL;
ALTER TABLE ticket_verifications ADD CONSTRAINT chk_tv_actual_amusement_context CHECK (
    (COALESCE(source_type,'')='amusement_online' AND amusement_verification_id IS NOT NULL)
    OR (COALESCE(source_type,'')='amusement_online_reversal' AND reverses_verification_id IS NOT NULL)
    OR (device_id IS NOT NULL AND point_id IS NOT NULL)
);

-- 两种受限来源必须指向同一权益的真实入场/反向原件；不能仅伪填 source_type 取得空上下文例外。
CREATE FUNCTION require_ticket_amusement_origin() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.source_type='amusement_online' AND NOT EXISTS (
        SELECT 1 FROM amusement_verifications a WHERE a.id=NEW.amusement_verification_id
          AND a.entitlement_id=NEW.entitlement_id AND a.direction='entry' AND a.verification_result='success'
    ) THEN
        RAISE EXCEPTION 'amusement_online requires the actual successful admission for the same entitlement';
    END IF;
    IF NEW.source_type='amusement_online_reversal' AND NOT EXISTS (
        SELECT 1 FROM ticket_verifications v JOIN amusement_verifications a ON a.id=v.amusement_verification_id
         WHERE v.id=NEW.reverses_verification_id AND v.entitlement_id=NEW.entitlement_id
           AND v.source_type='amusement_online' AND a.entitlement_id=NEW.entitlement_id
           AND a.direction='entry' AND a.verification_result='success'
    ) THEN
        RAISE EXCEPTION 'amusement_online_reversal requires the actual original ticket admission';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_ticket_amusement_origin BEFORE INSERT OR UPDATE ON ticket_verifications
    FOR EACH ROW EXECUTE FUNCTION require_ticket_amusement_origin();

