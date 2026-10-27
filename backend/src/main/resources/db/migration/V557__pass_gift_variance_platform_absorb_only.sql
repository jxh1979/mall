-- 文件版本：V0.01
-- V557：赠付差额处理规则下线「结转下期」「回补当期可分配池」，只剩「差额留娱乐平台公司」（18B 复核后裁决第 11 条）。
-- 依据：操作员 2026-10-27 选「对，下线两个选项（推荐）」；ENUM L52「该笔预扣归娱乐平台公司，预扣数与日后实付数的差额也留在娱乐平台公司」。
-- 规则表只把已有非 platform_absorb 的行改为 platform_absorb 再收窄 CHECK（结算 PRD §5.3「已有配置行须回退为 platform_absorb」），
-- 不做别的存量处理（UD-07 / DEC-R6-28：研发阶段无生产存量）。

UPDATE pass_gift_cost_configs SET variance_handling = 'platform_absorb' WHERE variance_handling <> 'platform_absorb';
ALTER TABLE pass_gift_cost_configs DROP CONSTRAINT chk_pgc_variance,
    ADD CONSTRAINT chk_pgc_variance CHECK (variance_handling = 'platform_absorb');
COMMENT ON COLUMN pass_gift_cost_configs.variance_handling IS
    'S7 差额处理规则:只剩 platform_absorb=差额留娱乐平台公司仅留痕(ENUM L52;2026-10-27 操作员裁决下线结转下期与回补当期可分配池)';

-- 差额台账 append-only（V377 trg_append_only_ud）：历史行保留原快照不改写，新写入的行一样拒收两个下线值（NOT VALID 只约束新行）。
ALTER TABLE pass_gift_cost_variance_ledger DROP CONSTRAINT chk_pgcv_handling,
    ADD CONSTRAINT chk_pgcv_handling CHECK (variance_handling = 'platform_absorb') NOT VALID;
COMMENT ON COLUMN pass_gift_cost_variance_ledger.variance_handling IS
    '本行处理规则快照:新行只收 platform_absorb=差额留娱乐平台公司仅留痕(2026-10-27 操作员裁决下线结转下期与回补当期可分配池;历史行保留原快照)';
COMMENT ON COLUMN pass_gift_cost_variance_ledger.carried_in_cent IS
    '已下线(2026-10-27 操作员裁决下线结转下期):不再写入';
