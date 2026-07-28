-- S3「门票综合服务费一张票只收一道」(2026-07-28)——给核销结算流水加父票锚点。
-- 业务口径(business-system-map §「门票综合服务费」):该费＝娱乐平台公司留存/项目管理费,与留存是同一项、
--   分账瀑布中【只扣一次】;所有门票项目都收【一道】。
-- 错在哪:固定额模式按【每笔核销】计提 —— 一张五次通票核销 5 次就收 5 道,对账时一张票上冒出 5 条同名费,
--   业务人员解释不了、商户对不上账。计提时点已由用户拍板＝【该票首笔核销时】。
-- 本列就是那个判定锚点:核销行从权益带上 parent_ticket_id,冲正行原样沿用被冲那行的值
--   (取反把该票净额抵回 0 → 锚点复位,这张票下一次核销可以重新收一道,不能因为退过一次核销就永远免单)。
-- 不建 FK → ticket_instances:本表是 append-only 不可变账本,账本行不该因主数据被删而落不下/被级联动;
--   与既有 merchant_id / order_id 等溯源列同范式(本表现无任何 FK)。
-- 无回填:开发环境、数据最终全部清除;存量无锚点的行 = 不合规数据,消费者一律拒绝计提,不做兼容分支。
-- 表级 GRANT 由 V140(GRANT ... ON ALL TABLES IN SCHEMA public TO complex_app)覆盖,加列自动继承,无需另行 GRANT
--   (V344 已 REVOKE 本表的 UPDATE/DELETE —— append-only 账本,本迁移不动这层)。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS parent_ticket_id VARCHAR(64);

COMMENT ON COLUMN settlement_transactions.parent_ticket_id IS
    'S3:本行所属父票 issued_entitlements.parent_ticket_id;门票综合服务费「一张票只收一道」的判定锚点(冲正行沿用原行值以复位)';

-- 计提前查「该票已落服务费净额」走 (parent_ticket_id) 等值 → 部分索引(只索有锚点的行)。
CREATE INDEX IF NOT EXISTS idx_st_parent_ticket
    ON settlement_transactions (parent_ticket_id)
    WHERE parent_ticket_id IS NOT NULL;
