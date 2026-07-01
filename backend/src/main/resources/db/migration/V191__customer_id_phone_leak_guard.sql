-- V191 兜底自检：把任何"手机号派生"的客人主键改为随机不可反推 id，并级联所有引用。
--
-- 背景：历史上 phoneLogin 曾用 id = "customer_" + 手机号，主键内嵌明文手机号，
-- 从主键即可反推手机号、架空 phone 列的 AES-GCM 加密+盲索引。生成逻辑已改为随机 UUID
-- （见 CustomerService），本迁移是对"历史/生产可能遗留行"的一次性兜底修复。
--
-- 判据：id 形如 customer_<纯数字>（新的随机方案含十六进制字母，测试夹具为 sv_notif_ 等前缀，均不命中）。
-- 正常库无此类行 → 本迁移为 no-op（不触碰任何约束/数据）。
--
-- 覆盖面：动态重指向全库所有名为 customer_id 的列（19 张表，含 amusement_*/bar_*/saved_bottles/
-- ticket_orders/储值流水 等 11 张无外键声明的表），避免硬编码漏表导致残留泄露值。
-- ponytail: 审计日志经 subject_id(泛型)关联而非 customer_id 列，重写审计历史属合规禁忌，本迁移有意不覆盖。
--
-- 机制：8 个引用 customers 的外键均为 NO ACTION，原地改主键会与子行互相卡死；
-- 故在本事务内临时将这些外键设为可延迟并延迟检查，改主键→重指向子行，提交前一致后再复原。
DO $$
DECLARE
    rec      RECORD;
    col      RECORD;
    fk       RECORD;
    new_id   TEXT;
BEGIN
    -- 无泄露行则直接返回（真实库常态：no-op，不改任何约束）
    IF NOT EXISTS (SELECT 1 FROM customers WHERE id ~ '^customer_[0-9]+$') THEN
        RETURN;
    END IF;

    -- 临时放开 8 个 customers 外键的延迟性，使父/子主键改写可在同一事务内闭合
    FOR fk IN
        SELECT conrelid::regclass AS child, conname
        FROM pg_constraint
        WHERE contype = 'f' AND confrelid = 'customers'::regclass
    LOOP
        EXECUTE format('ALTER TABLE %s ALTER CONSTRAINT %I DEFERRABLE INITIALLY IMMEDIATE',
                       fk.child, fk.conname);
    END LOOP;
    SET CONSTRAINTS ALL DEFERRED;

    FOR rec IN SELECT id FROM customers WHERE id ~ '^customer_[0-9]+$' LOOP
        new_id := 'customer_' || replace(gen_random_uuid()::text, '-', '');
        UPDATE customers SET id = new_id WHERE id = rec.id;
        FOR col IN
            SELECT table_name
            FROM information_schema.columns
            WHERE table_schema = 'public' AND column_name = 'customer_id'
        LOOP
            EXECUTE format('UPDATE public.%I SET customer_id = %L WHERE customer_id = %L',
                           col.table_name, new_id, rec.id);
        END LOOP;
    END LOOP;

    -- 强制立即校验（父/子已一致；不一致则本迁移事务回滚）后复原外键为不可延迟
    SET CONSTRAINTS ALL IMMEDIATE;
    FOR fk IN
        SELECT conrelid::regclass AS child, conname
        FROM pg_constraint
        WHERE contype = 'f' AND confrelid = 'customers'::regclass
    LOOP
        EXECUTE format('ALTER TABLE %s ALTER CONSTRAINT %I NOT DEFERRABLE',
                       fk.child, fk.conname);
    END LOOP;
END $$;
