-- P1-A 财务项目级隔离(与储值/积分同范式):ar_ap_items 补 project_id(综合体维度),
-- 与既有 merchant_id 两维并存(merchant 商户维 + project 综合体维),不破坏既有商户级隔离。
-- 可空:落账时由创建方 currentProjectScopeQuietly 写入;历史行按来源单据回填,推不出的留 null。

ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_project
    ON ar_ap_items (project_id)
 WHERE project_id IS NOT NULL;

-- 回填①:合同租金(source_type='contract'/'contract_rent',source_id 形如 contractId 或 contractId|period)
--        → 从 contracts.project_id 派生。split_part 取管道符前的 contract id。
UPDATE ar_ap_items a
   SET project_id = c.project_id
  FROM contracts c
 WHERE a.project_id IS NULL
   AND a.source_type IN ('contract', 'contract_rent')
   AND c.id = split_part(a.source_id, '|', 1)
   AND c.project_id IS NOT NULL;

-- 回填②:物业费(source_type='property_fee',source_id=物业费账单 id)→ property_fee_bills.project_id。
UPDATE ar_ap_items a
   SET project_id = b.project_id
  FROM property_fee_bills b
 WHERE a.project_id IS NULL
   AND a.source_type = 'property_fee'
   AND b.id = a.source_id
   AND b.project_id IS NOT NULL;

-- 回填③:结算/CAM(source_type IN ('settlement','cam'),source_id=结算账单 id)→ settlement_bills.project_id。
UPDATE ar_ap_items a
   SET project_id = s.project_id
  FROM settlement_bills s
 WHERE a.project_id IS NULL
   AND a.source_type IN ('settlement', 'cam')
   AND s.id = a.source_id
   AND s.project_id IS NOT NULL;

-- 说明:source_type='manual'(手工录入,无来源单据)、'energy_fee'(source_id 为不透明 token,
-- 无稳定 project 反查路径)、'ticket_distribution'(分销,无综合体归属)等历史行 project_id 留 null;
-- 平台口径查询(project scope=null)本就全量,留 null 不影响可见性;新行落账起即带 project_id。
