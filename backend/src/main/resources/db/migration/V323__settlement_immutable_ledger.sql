-- V323 结算状态机重构(审计 AUD-012/013/017)：settlement_transactions 从"每权益一行可变累计"
--   改为"每业务事件一行不可变资金事实"(append-only 账务事件账本)。
--   根因:旧模型用 issued_entitlements.used_times 累计值覆盖同一结算行(uq_st_entitlement_verified),
--         跨月核销漏结/重算多算(AUD-012)、核销冲正无负向结算(AUD-013)。
--   新模型:①核销 verification_settled 按 verification_id 逐笔增量不可变行(transaction_no=ST-VS-<verId>);
--         ②冲正 verification_reversed 追加负向不可变行(transaction_no=ST-VR-<reversalId>),携带原 verification_id;
--         ③account 按 business_at(核销/冲正业务时间)聚合增量,不再按首插 created_at;
--         幂等由全局唯一 transaction_no(uq_st_transaction_no,V222 已建)承担,写入一律 append-only(不 UPDATE)。
--   仅加列/放宽 CHECK/删旧唯一索引;既有行 business_at 回填=created_at(旧口径不变),可独立回滚。

-- ① 业务时间列:核销/冲正的业务发生时刻(account 月窗聚合基准,取代首插 created_at 造成的月份漂移)。
--    可空;聚合用 COALESCE(business_at, created_at) 兼容存量行(存量回填=created_at,现状不变)。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS business_at TIMESTAMPTZ;
UPDATE settlement_transactions SET business_at = created_at WHERE business_at IS NULL;

-- ② 事件类型扩容:新增 verification_reversed(核销冲正负向结算事实)。
ALTER TABLE settlement_transactions DROP CONSTRAINT IF EXISTS chk_st_type;
ALTER TABLE settlement_transactions ADD CONSTRAINT chk_st_type
    CHECK (transaction_type IN ('verification_settled','unverified_expired','verification_reversed'));

-- ③ 金额恒等 CHECK:非冲正行仍全部 >=0;冲正行必须全部 <=0(负向,恰为原正向行的取反)。
--    保证任一聚合 SUM 对"核销+冲正"配对天然净为 0,无舍入漂移(资金守恒 DB 级守卫)。
ALTER TABLE settlement_transactions DROP CONSTRAINT IF EXISTS chk_st_amount;
ALTER TABLE settlement_transactions ADD CONSTRAINT chk_st_amount CHECK (
    (transaction_type = 'verification_reversed'
        AND verified_amount_cent <= 0 AND unverified_amount_cent <= 0 AND allocated_revenue_cent <= 0)
    OR (transaction_type <> 'verification_reversed'
        AND verified_amount_cent >= 0 AND unverified_amount_cent >= 0 AND allocated_revenue_cent >= 0)
);

-- ④ 删旧"每权益核销一行"部分唯一索引:新模型每次核销独立成行(按 verification_id),不再累计覆盖。
--    幂等改由全局唯一 transaction_no(uq_st_transaction_no,V222)承担。
DROP INDEX IF EXISTS uq_st_entitlement_verified;

-- ⑤ 月窗聚合索引:按 transaction_type + business_at 取当月核销/冲正增量。
CREATE INDEX IF NOT EXISTS idx_st_type_business
    ON settlement_transactions (transaction_type, business_at);
