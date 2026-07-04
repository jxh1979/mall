# 商户-铺位-合同 权威数据模型 ER（v0.1·以代码为准）

> 生成：2026-07-04。**权威口径来源 = 现行迁移与代码**（`origin/main`，非任何单份 PRD）。本文用于消解「招商获客 / 地产全链 / 商户专区」三份 PRD 对同一数据模型各说各话的问题：三份 PRD 的关系模型段一律引用本文，本文与代码冲突时以代码为准、随迁移演进更新。
>
> 采集范围：`V002/V005/V010`（基表）、`V042/V147`（商户扩展）、`V080`（合同-空间）、`V162`（合同域重建）、`V184/V203`（空间字典/状态）、`governance/PlatformService.java`、`contract/ContractService.java`。
>
> 名词四层（本项目统一）：**平台 → 项目公司 → 管理公司/SPV → 商户**。注意：代码里没有名为 `organizations`/`org_type` 的表，四层主体统一落在 **`business_entities`（工商主体）** 一张表，用 `entity_role` 文本字段区分角色（当前**无 DB 枚举约束**，见 §5 待收口）。

---

## 1. 实体清单（以代码表为准）

| 业务名 | 表 | 关键作用 | 隔离维度 |
|---|---|---|---|
| 工商主体 | `business_entities` | 四层主体的统一载体（平台/项目公司/管理公司·SPV/商户对应的法人或经营主体），`entity_role` 区分角色 | 平台级主数据 |
| 商户 | `merchants` | 综合体内一个经营租户/联营方/服务商的档案 | 经营主体 scope（`business_entity_id`） |
| 商户-四类主体绑定 | `business_entity_bindings`（`binding_type='four_party'`） | **商户四类主体绑定的权威、可版本化载体**（每商户至多一条 active） | 按 merchant |
| 场地/项目（业态单元） | `projects` | ⚠️**非「项目公司」**，是综合体内的**业态经营单元/场馆**（`venue_type`，如 bar/amusement/ticketing），可选挂 `merchant_id` | 综合体 |
| 空间/铺位 | `spaces` | 楼栋→楼层→区块→**铺位**的层级空间树（`parent_space_id` 自引用），铺位是招商/租赁最小标的 | project + merchant |
| 空间类型字典 | `space_type_dictionary` | `spaces.space_type` 的硬校验字典（building/floor/zone/**shop**/public_area/parking/booth/other） | 平台级 |
| 合同 | `contracts` | 商户↔铺位的租赁契约，携带四类主体（`*_subject_id`）、租金条款、8 业务态生命周期 | 综合体（`project_id`） |
| 合同-空间关联 | `contract_spaces` | **合同 ↔ 铺位 多对多**（一合同多铺位、一铺位可被不同时段合同复用） | 综合体 |
| 合同租金账单 | `contract_rent_bills` | 逐（合同,期）计费 → 联动 `ar_ap_items` 应收（UNIQUE 幂等） | 综合体 |
| 合同审批链/变更/结算 | `contract_approval_flows`/`_steps`/`contract_approvals`/`contract_changes`/`contract_settlements`/`space_expiry_color_rules` | V162 重建的审批（分组并行会签）/6类变更/退租结算/铺位到期着色 | 综合体（配置表 project 可空=平台默认） |

---

## 2. 关键字段与外键（逐表）

### 2.1 `business_entities`（V005）— 四层主体统一表
- `id` PK / `entity_name` / **`entity_role` VARCHAR(32) NOT NULL**（角色，无 CHECK；应承载「项目公司 / 管理公司·SPV / 商户主体」等，见 §5）/ `unified_social_credit_code`（统一社会信用代码）/ `legal_representative` / `bank_account_ref` / `status`。
- **无外键**指向自身层级；四层关系目前靠 `entity_role` 文本 + 绑定表表达，不是硬外键树。

### 2.2 `merchants`（V005 + V042 + V147）
- 主档：`id` PK / `merchant_name` / `merchant_type`（无 CHECK）/ `status` / `owner_person`（负责人，V147）/ `contact_*`+`secondary_contact_*`（第一/第二联系人）。
- **主体快照列**：`business_entity_id`（经营主体，隔离 scope 依据）、`contract_entity_id`（合同主体）、`settlement_entity_id`（结算主体）——均为 `VARCHAR(64)`，**无 DB 外键约束**指向 `business_entities`。
- 租赁快照：`mall_block_code`、`area_square_meter`、`leased_area_square_meter`、`rent_per_square_meter`、`cooperation_form`（合作模式，无 CHECK）。
- ⚠️ 这三列是**档案快照**；四类主体的**权威、可变更历史**在 `business_entity_bindings`（§2.3）。

### 2.3 `business_entity_bindings`（V002 基表 + V010 扩展）— 四类主体权威绑定
- 通用绑定基列：`id UUID` / `source_entity_type` / `source_entity_id` / `target_entity_type` / `target_entity_id` / `binding_type` / `status`。
- 四方绑定扩展列（V010）：`merchant_id`、**`operation_entity_id`（运营主体）**、**`contract_entity_id`（合同主体）**、**`settlement_entity_id`（结算主体）**、`effective_from/to`、`change_reason`、`updated_at`。
- **铁律**：`uq_business_entity_bindings_active_merchant` — **每商户至多一条 `status='active'` 绑定**。代码（`PlatformService.saveBusinessEntityBinding` / `changeBusinessEntityBinding`）落地为：新建/激活时校验已存在 active 绑定则 `409 ACTIVE_BINDING_EXISTS`；停用/挂起写 `effective_to`。
- 语义：**这里是「四类主体后续可拆」的落点** —— 转商户初期四类可都指向同一 `business_entity`；后续拆分 = 换一条新的 active 绑定，指向不同 `operation/contract/settlement` 主体。**「经营对象」= merchant 自身**；运营/合同/结算三主体各一列。

### 2.4 `spaces`（V005 + V184 + V203）— 空间/铺位树
- `id` PK / `project_id`（挂业态单元，可空）/ `merchant_id`（当前占用商户，可空）/ **`parent_space_id`（自引用，构成 楼栋→楼层→区块→铺位 层级）** / `space_name` / **`space_type`（硬校验 ∈ `space_type_dictionary` active，铺位=`shop`）** / `floor_no` / `area_square_meter` / `polygon_points`（平面图多边形）/ **`status` CHECK ∈ (`active`,`unavailable`)**（V203，仅 2 值）。
- ⚠️ **代码里空间没有「空置/洽谈中/已租/即将到期/已退租」这类租赁状态枚举**（PRD §2 的「租赁状态着色看板」6 态是 PRD 愿景，未落 DB）。实际到期着色由 §2.7 `space_expiry_color_rules` 按「距到期天数」计算，不是 status 字段。

### 2.5 `contracts`（V010 基 + V080 + V162 扩展）
- 基列（V010）：`id` PK / **`merchant_id` NOT NULL**（合同归属商户）/ **`contract_subject_id` NOT NULL（合同主体）** / `operation_subject_id`（运营主体，可空）/ **`settlement_subject_id` NOT NULL（结算主体）** / `effective_date` / `expiry_date` / `rent_terms` JSONB / `shareholder_ratio_for_report` / `lifecycle_status`。
- V080：`allowed_business_scope` JSONB（准入闸经营范围）。
- V162 扩展：`commission_mode`（抽成4模式）、`energy_fee_basis`（公区能耗分摊4模式）、`deposit_cent`、`penalty_terms`、`settlement_rules`、`snapshot`（生效固化快照）、`project_id`（综合体）、`terminated_at`/`termination_reason`、`renewed_from_contract_id`（续约链）。
- **`lifecycle_status` CHECK 8 业务态**（V162）：`drafting`/`pending_confirmation`/`pending_effective`/`effective`/`renewing`/`expired`/`terminated`/`archived`。
- ⚠️ 合同的三个 `*_subject_id` 是**合同各自携带的主体**，与商户当前 active 绑定（§2.3）**可不一致**（合同快照 vs 商户当前绑定），无外键强绑。

### 2.6 `contract_spaces`（V080 + V162）— 合同↔铺位
- `contract_id` FK→`contracts` / `space_id` FK→`spaces` / `project_id`（V162）/ PK(`contract_id`,`space_id`)。
- **多对多**：一合同可租多铺位；一铺位可被不同时段/合同复用（选关联表而非 `spaces.contract_id` 即为此）。

### 2.7 V162 其余（审批/变更/结算/账单/着色）
- `contract_approval_flows` + `_steps`：可配审批链；**同 `(flow,stage_seq)` 多行 = 一个并行会签阶段**；每综合体至多一条 active 链（NULL project=平台默认）。默认链：阶段①业务/财务/法务并行 → 阶段②总经理终审。
- `contract_approvals`：审批实例留痕；`change_id` 空=新签、非空=变更走同一链。
- `contract_changes`：**6 类变更 CHECK**（`renewal`/`move_out`/`area_change`/`rent_change`/`business_scope_change`/`early_termination`）——**无转租**（换租户须先退租再招商）。
- `contract_settlements`：退租/到期结算，`trigger_type`∈(move_out/expiry/early_termination)，`status`∈(in_progress/disputed/settled)，`settlement_items` 逐例顺序执行。
- `contract_rent_bills`：逐(合同,期)计费，`ar_ap_item_id` FK→`ar_ap_items`，`uq_contract_rent_bills_contract_period` 幂等。
- `space_expiry_color_rules`：可配到期着色（分类名+颜色+提前天数），默认「即将到期/30天/#F59E0B」。

---

## 3. 权威关系图（文字 ER）

```
business_entities (四层主体统一表, entity_role 区分)
        ▲ (软引用, 无FK)                          ▲ (软引用, 无FK)
        │                                          │
   merchants.business_entity_id            contracts.contract_subject_id
   merchants.contract_entity_id                     .operation_subject_id
   merchants.settlement_entity_id                   .settlement_subject_id
        │  (档案快照)                          (合同自带主体, 快照, 可!=商户当前绑定)
        │
   business_entity_bindings  ──(每商户至多一条 active, 权威可版本化)
        merchant_id / operation_entity_id / contract_entity_id / settlement_entity_id
        │
   merchants (1) ──< contracts (N)          [merchant_id, 一商户可多合同]
        │                    │
        │                    └──< contract_spaces >── spaces   [合同↔铺位 多对多]
        │                                                │
   spaces.merchant_id (当前占用)                     spaces.parent_space_id (自引用: 楼栋→层→区块→铺位)
        │                                                │
   projects (业态单元/场馆, venue_type)  ──(1:N)──  spaces.project_id
```

## 4. 三条「一个商户能否…」的权威回答（以代码为准）

| 问题 | 答案 | 依据 |
|---|---|---|
| 一个商户可否多铺位？ | **可**。商户通过合同占用铺位，`contract_spaces` 多对多；`spaces.merchant_id` 亦可直接标占用。 | `contract_spaces` PK 多对多 |
| 一个商户可否多合同？ | **可**。`contracts.merchant_id` 一对多；含续约链 `renewed_from_contract_id`。 | `contracts` FK + 索引 `idx_contracts_merchant_status` |
| 一个商户可否多业态？ | **可**。合同 `allowed_business_scope` 是数组；`projects.venue_type` 按业态单元建，商户可关联多个业态场地。 | `contracts.allowed_business_scope` JSONB / `projects` |
| 结算主体如何绑定？ | 权威在 `business_entity_bindings.settlement_entity_id`（每商户 active 唯一）；`merchants.settlement_entity_id` 是档案快照；合同另带 `contracts.settlement_subject_id`（该合同结算主体，快照）。三处语义分层：商户当前绑定 / 商户档案快照 / 合同签署时快照。 | §2.2 / §2.3 / §2.5 |

## 5. 与 PRD 表述的已知差异 / 待收口（供三份 PRD 对齐时标注）

1. **无 `organizations`/`org_type` 表**：四层主体全在 `business_entities`，用 `entity_role` 文本区分，且**该字段当前无 DB CHECK / 代码枚举校验**（`PlatformService.createBusinessEntity` 仅 requireText 非空）。名词四层是**业务概念分层**，DB 层是**一表多角色**。→ PRD 勿臆造独立「项目公司表/SPV 表」；如需收紧 `entity_role` 取值，标「后台可配 + 待补 CHECK」。
2. **`projects` ≠ 项目公司**：它是**业态经营单元/场馆**（`venue_type`）。名词四层的「项目公司」是 `business_entities` 里的一个 `entity_role`，不是 `projects` 表。PRD 谈「综合体/项目级隔离」时的 `project_id` 是**综合体维度**，勿与 `projects` 表混为一谈。
3. **空间租赁状态只有 2 值**（`active`/`unavailable`，V203）：PRD 的「空置/洽谈中/已租/即将到期/已退租/不可租」6 态着色看板**未落 DB status**；到期着色实由 `space_expiry_color_rules` 按天数算。→ PRD 应标该 6 态为「看板派生/待建」，非 DB 枚举。
4. **四类主体拆分落点 = `business_entity_bindings`**（每商户 active 唯一），非在 `merchants` 三列改写。「后续可拆」= 新建 active 绑定替换。
5. **合同主体 vs 商户绑定不强一致**：合同 `*_subject_id` 是签署时快照，与商户当前 active 绑定无外键强绑，可不同。PRD 勿声称「合同主体永远等于商户结算主体」。
6. **无转租**：`contract_changes` 6 类不含转租；换租户 = 先退租再招商。
