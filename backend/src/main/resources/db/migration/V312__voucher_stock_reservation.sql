-- V312 发券预占库存（防超发，2026-07-19 用户拍板）。对齐 PRD §5「兑换须先扣礼品库存，不足则拒绝兑换、不扣积分」：
-- V301 方案A 发券完全不动库存，店内实物券可超发过库存（先兑的能核、后兑的核销时才 500 缺货，且积分已扣）。
-- 改为：店内实物券（in_store_goods）发券即从承接商户对应实物「预占」库存
--   （reserved_quantity += n、available_quantity -= n、quantity 不变），可用不足则发券失败（拒绝兑换/发放、不扣分）；
--   核销转实扣（reserved -= n、quantity -= n，available 不变）；过期/作废释放（reserved -= n、available += n）。
-- 预占/释放的并发正确性由 inventory_stock_balances 上的守卫式 UPDATE（WHERE available_quantity >= n /
--   reserved_quantity >= n，单语句行锁）保证；V012 已有 CHECK(available_quantity >= 0 / reserved_quantity >= 0)
--   为负库存兜底硬闸。商户折扣券（merchant_discount）不涉库存不预占；外部伙伴券（external_partner）本地不发实例、不预占。

-- 券实例记「是否已持库存预占」：区分本轮前发的历史店内实物券（不持预占，核销/过期走旧 available 直扣路径），
-- 避免对历史券误从 reserved 扣或误释放。新发的 in_store_goods 券预占成功后置 true。
ALTER TABLE voucher_instances
    ADD COLUMN IF NOT EXISTS stock_reserved boolean NOT NULL DEFAULT false;

-- 过期释放扫描命中（issued + 已预占 + 已过期）：定时作业按此扫描并释放，避免过期未核销的券把库存永久占死。
CREATE INDEX IF NOT EXISTS idx_vi_reserved_expiry
    ON voucher_instances (expires_at)
    WHERE status = 'issued' AND stock_reserved = true;

-- 说明：本迁移仅在既有已授权表 voucher_instances（V301 已 GRANT SELECT/INSERT/UPDATE/DELETE complex_app）上加列；
--   PostgreSQL 列继承表级授权，新列无需另行 GRANT。预占用 inventory_stock_balances 既有列，无新表/新列。
