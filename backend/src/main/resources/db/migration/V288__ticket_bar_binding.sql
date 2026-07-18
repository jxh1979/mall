-- AUD-076 单店票挂酒吧：票种/订单/父票/权益结构化保存酒吧归属（酒吧=业态 bar 的商户，复用商户目录，零新表）。
-- 用户口径：测试环境上线前全删，不做任何存量回填；本迁移只加可空列/索引，无回填 SQL、零停机。
-- 挂错不许改：bar_id 仅建票时写入（无更新入口），纠错=下架旧票存档+重建新票（账目最干净）。

-- ① 票种归属：NULL=综合体通票/平台票；非 NULL=单店票，指向业态为酒吧的商户（业态校验由服务层建票兜底）。
ALTER TABLE ticket_types ADD COLUMN IF NOT EXISTS bar_id VARCHAR(64) REFERENCES merchants(id);
CREATE INDEX IF NOT EXISTS idx_ticket_types_bar ON ticket_types (bar_id) WHERE bar_id IS NOT NULL;

-- ② 订单快照（下单时服务端从票种复制，不信客户端；与既有 merchant_id“受理/销售商户”语义正交）。
ALTER TABLE ticket_orders ADD COLUMN IF NOT EXISTS bar_id VARCHAR(64) REFERENCES merchants(id);
CREATE INDEX IF NOT EXISTS idx_ticket_orders_bar ON ticket_orders (bar_id, created_at DESC) WHERE bar_id IS NOT NULL;

-- ③ 父票实例快照（出票时从订单复制；撤场提前到期按 bar 检索父票）。
ALTER TABLE ticket_instances ADD COLUMN IF NOT EXISTS bar_id VARCHAR(64) REFERENCES merchants(id);
CREATE INDEX IF NOT EXISTS idx_ticket_instances_bar ON ticket_instances (bar_id) WHERE bar_id IS NOT NULL;

-- ④ 权益快照（出票时从订单复制；撤场提前到期按 bar 批量移 valid_to → 走方案2到期规则）。
ALTER TABLE issued_entitlements ADD COLUMN IF NOT EXISTS bar_id VARCHAR(64) REFERENCES merchants(id);
CREATE INDEX IF NOT EXISTS idx_issued_entitlements_bar ON issued_entitlements (bar_id) WHERE bar_id IS NOT NULL;
