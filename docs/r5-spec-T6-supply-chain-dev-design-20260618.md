# T6 供应链三模式 + 采购周期 + 结构化库房 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T6**，W1 供应链域。
> **裁决依据**：裁决④（三模式合同 + 采购周期完整流转 + 结构化库房 三项 P0）；二轮裁决（高价值复核=**库存域内自建**；扫码交接=员工扫码主动 + 机器人上报被动，缺口进复核——交接见 backlog T17，本规格含域内高价值复核表）。
> **现状基准**（已核）：`inventory_suppliers`(仅 name/type/contact/status，无资质/结算)、`inventory_purchase_requests`(单态 status，无审批/到货/入库流转)、`inventory_stock_balances.location_id`(自由文本)、`inventory_stock_batches`(V027,有 supplier_id)；**`supply_contracts`/`warehouses`/`purchase_cycles` 均未建**；`isHighValue` 仅校验附件数≥2，复核未接入。
> **迁移**：取下一连续版本（示意 **V069**，实际以合并顺序为准，注意与 T4/T7 错开版本号）。约定：金额=整数分（D1）、CHECK 中英枚举（D7）、隔离列 project_id/merchant_id。

---

## 1. 数据模型（迁移草案）

```sql
-- 1) 供应合同三模式
CREATE TABLE IF NOT EXISTS supply_contracts (
    id VARCHAR(64) PRIMARY KEY,
    contract_no VARCHAR(64) NOT NULL UNIQUE,
    supplier_id VARCHAR(64) NOT NULL REFERENCES inventory_suppliers(id),
    project_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),                          -- 商户自采时归属商户
    contract_mode VARCHAR(40) NOT NULL,               -- 三模式
    supply_scope TEXT,                                -- 供货范围
    price_terms TEXT,                                 -- 价格口径摘要
    payment_term_days INTEGER,                        -- 账期
    valid_from DATE, valid_to DATE,
    summary TEXT,                                     -- 合同核心摘要
    purchasable_scope JSONB NOT NULL DEFAULT '[]',    -- 可采购物料/范围（卡控用）
    approval_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_supply_contracts_mode CHECK (contract_mode IN
      ('platform_self_use','merchant_self_purchase','platform_purchase_distribution')),
    CONSTRAINT chk_supply_contracts_approval CHECK (approval_status IN ('draft','approved','rejected')),
    CONSTRAINT chk_supply_contracts_status CHECK (status IN ('active','suspended','expired'))
);

-- 2) 结构化库房（库位仍用 location_id，挂 warehouse）
CREATE TABLE IF NOT EXISTS warehouses (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    warehouse_type VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_warehouses_type CHECK (warehouse_type IN
      ('main','bar','bar_counter','consumable','gift','saved_bottle')),
    CONSTRAINT chk_warehouses_status CHECK (status IN ('active','archived'))
);
ALTER TABLE inventory_stock_balances ADD COLUMN IF NOT EXISTS warehouse_id VARCHAR(64) REFERENCES warehouses(id);

-- 3) 采购周期：扩 inventory_purchase_requests 状态机 + 流转字段 + 留痕事件表
ALTER TABLE inventory_purchase_requests
  ADD COLUMN IF NOT EXISTS supply_contract_id VARCHAR(64) REFERENCES supply_contracts(id),
  ADD COLUMN IF NOT EXISTS approved_by VARCHAR(64),
  ADD COLUMN IF NOT EXISTS ordered_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS actual_arrival_date DATE,
  ADD COLUMN IF NOT EXISTS received_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS received_by VARCHAR(64);
-- 状态枚举收口（原自由 status → CHECK 全流程）
ALTER TABLE inventory_purchase_requests
  ADD CONSTRAINT chk_inv_purchase_status CHECK (status IN
    ('submitted','approved','rejected','ordered','expected','arrived','received'));

CREATE TABLE IF NOT EXISTS inventory_purchase_request_events (
    id VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL REFERENCES inventory_purchase_requests(id) ON DELETE CASCADE,
    from_status VARCHAR(32), to_status VARCHAR(32) NOT NULL,
    operator_user_id VARCHAR(64) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4) 库存域内高价值复核（裁决：域内自建，不走 governance）
CREATE TABLE IF NOT EXISTS inventory_high_value_reviews (
    id VARCHAR(64) PRIMARY KEY,
    subject_type VARCHAR(40) NOT NULL,                -- 报损/盘点差异/存酒作废
    subject_id VARCHAR(64) NOT NULL,                  -- 关联 movement/adjustment/saved_bottle
    photo_attachment_ids JSONB NOT NULL DEFAULT '[]',
    video_attachment_ids JSONB NOT NULL DEFAULT '[]',
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer_user_id VARCHAR(64),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_inv_hv_subject CHECK (subject_type IN ('damage_report','stock_variance','saved_bottle_void')),
    CONSTRAINT chk_inv_hv_status CHECK (review_status IN ('pending','approved','rejected'))
);
```

## 2. 状态机
- **采购周期**：`submitted →(审批) approved/rejected → ordered → expected →(到货) arrived →(入库确认) received`；每步写 `inventory_purchase_request_events`；`received` 时生成入库 `inventory_stock_movements`(gain) 并落 warehouse/库位。
- **高价值复核（域内）**：报损/盘点差异/存酒作废触发 `is_high_value` → 建 `inventory_high_value_reviews(pending)`，要求照片+视频双证据 → 主管 approve 后动作生效（出库扣减/作废落账），reject 则回滚。
- **合同**：`draft →(审批) approved/rejected`；approved 且在有效期内方可采购，`purchasable_scope` 卡控可采购物料。

## 3. Service
`SupplyContractService`（建/审批/卡控可采购范围）；`PurchaseRequestService`（提交→审批→下单→到货→入库全流转 + 事件）；`WarehouseService`（库房 CRUD，库存挂 warehouse_id）；`InventoryHighValueReviewService`（域内复核，替换"仅校验附件数≥2"）。全部 `currentMerchantScope` 隔离（商户自采只见本商户合同/采购）。

## 4. API（P0，节选）
`POST /api/v1/inventory/supply-contracts`、`/{id}/approve`；`POST /api/v1/inventory/purchase-requests/{id}/{approve|order|arrive|receive}`；`POST /api/v1/inventory/warehouses`；`POST /api/v1/inventory/high-value-reviews/{id}/review`。

## 5. 事件 / 权限
- 事件：`supply_contract.approved`、`inventory.purchase_status_changed`、`inventory.received`、`inventory.high_value_review_created/decided`。
- 权限：`inventory.supply_contract.manage`、`inventory.purchase.approve`、`inventory.warehouse.manage`、`inventory.high_value.review`。

## 6. 验收（GWT）
- 三模式：Given 合同 contract_mode=merchant_self_purchase，When 平台角色查，Then 按 scope 可见；超 purchasable_scope 采购被卡。
- 采购：Given submitted 采购单，When 走 approve→order→arrive→receive，Then 状态逐级流转、每步留事件、received 生成入库流水到指定库房。
- 库房：Given warehouse_type=saved_bottle，When 存酒入区，Then 库存挂该 warehouse_id。
- 高价值：Given 高价值报损无双证据，Then 不能提交；有双证据→pending→主管 approve 后扣减生效。

## 7. 测试 / 依赖 / claims
- 测试：CHECK 拒非法 mode/type/status；采购全流转 + 非法跳步拒绝；隔离；高价值复核闭环；`MigrationVersions.latest()`。
- 依赖：`inventory_suppliers/inventory_items/inventory_stock_*`、`saved_bottles`。
- **claims 同步**：supply_contracts/warehouses present；inventory_purchase_requests 状态机 present；inventory_high_value_reviews present；矩阵 REQ-27/28 行下修转"已落地"。
- **关联 T17**：扫码交接全链状态机另见 T17 规格（员工扫码主动+机器人被动）。

## 8. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T6 规格（三模式合同+采购周期状态机+结构化库房+域内高价值复核） | R5 backlog T6；裁决④+二轮裁决；现状取自 V012/V027 |
