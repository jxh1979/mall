-- 文件版本：V0.01
-- 抽成规则同日起换版按真实复核转正先后选择，不以起草时间、更新时间或行标识猜序。
CREATE SEQUENCE turnover_rent_rule_activation_seq AS BIGINT;

ALTER TABLE turnover_rent_rules
    ADD COLUMN activation_sequence BIGINT,
    ADD CONSTRAINT chk_turnover_rent_rule_activation_sequence
        CHECK (activation_sequence IS NULL OR (activation_sequence > 0 AND is_draft = FALSE));

CREATE UNIQUE INDEX uq_turnover_rent_rule_activation_sequence
    ON turnover_rent_rules (activation_sequence)
    WHERE activation_sequence IS NOT NULL;

COMMENT ON COLUMN turnover_rent_rules.activation_sequence IS
    '复核转正事务内分配的不可变审批顺序；已有记录不回填，同日起顺序缺失时不猜序';

CREATE FUNCTION preserve_turnover_rent_rule_activation_sequence()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.activation_sequence IS NOT NULL
       AND NEW.activation_sequence IS DISTINCT FROM OLD.activation_sequence THEN
        RAISE EXCEPTION '营业额抽成规则的转正序号不得修改';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_turnover_rent_rule_activation_sequence_immutable
    BEFORE UPDATE OF activation_sequence ON turnover_rent_rules
    FOR EACH ROW EXECUTE FUNCTION preserve_turnover_rent_rule_activation_sequence();
