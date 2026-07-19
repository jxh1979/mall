-- V302: 分红期间关账锁的判定索引。
-- 关账判定 = 某商户某月被"非 preview 且 status∈(approved,paid)"的计算单按 [period_start,period_end] 覆盖。
-- 该判定在费用台账每次写入(手工录入/冲销)与自动挂接(按期跨商户 NOT EXISTS)时高频命中，
-- 加部分索引支撑 (merchant_id, period_start, period_end) 的覆盖查找；仅索引，无新表/新约束故无需 GRANT。
CREATE INDEX IF NOT EXISTS ix_dividend_statements_lock
    ON dividend_statements (merchant_id, period_start, period_end)
    WHERE kind <> 'preview' AND status IN ('approved', 'paid');
