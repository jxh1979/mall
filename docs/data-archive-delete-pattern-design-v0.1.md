# 数据存档与审批后删除范式设计 v0.1

## 1. 背景与原则

用户拍板：**"所有的数据都应该有存档或删除的能力，可以要求审批，但不能说所有数据录入以后就没有变化了。"**

以通票权益模板（`entitlement_templates`）为首个试点：配置错误的模板、过期无效模板，应允许**存档**（软下线、可恢复）或**删除**（彻底移除、走上级审批）。

范式两条腿，语义严格区分：

| 能力 | 语义 | 可逆 | 是否要审批 | 数据落地 |
|------|------|------|-----------|---------|
| **存档 (archive)** | 软标记下线，业务不再可选用，但历史引用（已发放权益、已绑票种、结算流水）仍完整可查 | ✅ 可取消存档恢复 | 否（`manage` 权限即可，属日常运营） | 加 `archived_at` / `archived_by` 列；列表默认过滤 |
| **删除 (delete)** | 从主表物理移除该行 | ❌ 不可逆 | ✅ 是（申请→上级审批→执行） | 两段式：申请列打标 → 审批通过后 `DELETE` |

## 2. 为什么存档=软标记、删除=硬删+审批

- **存档**面向"配错了/过期了、先下线别再用、但别丢历史"。这是高频、低危操作，用软删除范式（本仓已有先例 `V182 merchant_fee_notification_recipient.deleted_at`、`V179 site_entry_reports.deleted_at`），列表 `WHERE archived_at IS NULL` 默认过滤即可。**取消存档**只是把 `archived_at` 置回 NULL。
- **删除**面向"这条记录本就不该存在/彻底清掉"。物理删除不可逆、可能触及外键（`ticket_type_entitlements`/`issued_entitlements` 均 `REFERENCES entitlement_templates(id)`，非 CASCADE），所以**必须走审批**且**执行前校验无活引用**。

## 3. 复用现有审批基建的取舍（诚实标注）

本仓已有通用审批基建 `supervisor_review_decisions`（V050/V053/V057）+ `SupervisorReviewService`，语义为"评估动作→命中策略则 `blocked=pending_review`→上级 `approve`（校验审批人职级 rank、**禁止发起人自审**）"。

**取舍决策**：`SupervisorReviewService.approve` 只把 `review_status` 改成 `approved` **并留痕/发事件**，**没有"批准后回调执行业务动作"的钩子**——它是一个放行/阻断的裁决层，不是编排层。若强接删除流，需要额外的 "pending→轮询/回调→执行 DELETE" 层与跨域耦合。

因此试点采用**折中方案**（复用审批**范式与原则**，不硬塞跨域评估表）：

- 删除申请状态机落在**通票模板自身的列**上（`delete_status` + 申请人/申请时间/审批人/审批时间/理由），一次数据库往返即可推进，无跨域回调。
- **审批权限用新增的分级权限码** `ticket.entitlement.approve`（`requires_approval=TRUE`, `risk_level=critical`），与 `ticket.entitlement.manage`（发起删除申请）**职责分离**——沿用 `supervisor_review` 的"发起权≠审批权"原则。
- **禁止自审**：审批端点校验 `delete_requested_by != 当前审批人`，与 `SupervisorReviewService` 的 `SUPERVISOR_SELF_REVIEW_FORBIDDEN` 同源。

> 未来若需要跨实体统一审批中心，可把此状态机迁到 `supervisor_review` 并补一个"批准后执行回调注册表"。当前试点不做，避免为 1 张表引入编排层（YAGNI）。

## 4. 表结构改动（V253）

`entitlement_templates` 加 5 列，全部可空/带默认，向后兼容：

```sql
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS archived_at           TIMESTAMPTZ,           -- 非空=已存档
    ADD COLUMN IF NOT EXISTS archived_by           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status         VARCHAR(16) NOT NULL DEFAULT 'none',  -- none|pending|approved
    ADD COLUMN IF NOT EXISTS delete_requested_by   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason         TEXT;

ALTER TABLE entitlement_templates
    ADD CONSTRAINT chk_et_delete_status
    CHECK (delete_status IN ('none','pending','approved'));
```

行删除后记录不复存在，故审批人/审批时间无需持久化在本表（留痕靠 `AuditEventService` 事件 `ticket.entitlement.delete_approved`）。`delete_status='approved'` 是执行 DELETE 前的瞬时态（同事务内置 approved 再 DELETE，或 approved 后立即 DELETE）。

## 5. 状态机

**存档轴**（archived_at）：
```
active(archived_at IS NULL) --archive--> archived(archived_at=now)
archived --unarchive--> active
```

**删除轴**（delete_status）：
```
none --request-delete[需 manage 权限]--> pending(记 requested_by/at/reason)
pending --approve[需 approve 权限, 审批人≠申请人, 无活引用]--> DELETE(物理移除)
pending --reject[需 approve 权限]--> none
```

约束：
- 只能对**已存档**的模板发起删除申请（先下线再申请删，防误删在用模板）。
- 审批执行 DELETE 前校验 `ticket_type_entitlements` / `issued_entitlements` 无引用；有引用则拒 `ENTITLEMENT_TEMPLATE_IN_USE`。

## 6. 权限码（V253 seed）

| 权限码 | 名称 | risk | requires_approval | 谁有 |
|--------|------|------|-------------------|------|
| `ticket.entitlement.manage`（已存在） | 管理权益模板 | high | TRUE | 发起存档/取消存档/删除申请 |
| `ticket.entitlement.approve`（新增） | 审批权益模板删除 | critical | TRUE | 审批/驳回删除申请 |

`ticket.entitlement.approve` seed 给 `role_platform_admin`。发起人（manage）与审批人（approve）职责分离。

## 7. API 端点设计

沿用现有 `/api/v1/ticketing/entitlement-templates` 前缀，全部带 `idempotencyKey`：

| 方法 | 路径 | 权限 | 动作 |
|------|------|------|------|
| GET | `/ticketing/entitlement-templates` | `ticket.entitlement.view` | 列表（默认排除 archived + delete_status='approved'）；`?includeArchived=true` 显示存档 |
| POST | `/ticketing/entitlement-templates/{id}/archive` | `ticket.entitlement.manage` | 存档 |
| POST | `/ticketing/entitlement-templates/{id}/unarchive` | `ticket.entitlement.manage` | 取消存档 |
| POST | `/ticketing/entitlement-templates/{id}/delete-request` | `ticket.entitlement.manage` | 发起删除申请（须先存档） |
| POST | `/ticketing/entitlement-templates/{id}/delete-approve` | `ticket.entitlement.approve` | 审批：`{decision: approved\|rejected}`，approved 即执行 DELETE |

## 8. 前端（TicketingPoliciesPage）

权益模板表格加"操作"列：
- 未存档行：`存档` 按钮（`manage`）。
- 已存档行：`取消存档` + `申请删除`（`manage`）；若 `delete_status=pending` 显示"待审批删除"+ `批准删除`/`驳回`（`approve`）。
- 列表默认只显示未存档；加"显示已存档"开关。
- 全中文，按钮走 `PermissionButton` 按权限码显隐。

## 9. 推广到其它实体的路线图

范式三件套可复用：`archived_at/archived_by` 列 + `delete_status` 状态机 + `.manage`/`.approve` 权限对 + 前端操作列。优先级按"高频配错/过期 且 有历史引用需保全"：

| 优先级 | 实体 | 理由 | 预估工作量 |
|--------|------|------|-----------|
| P1 | `ticket_types` 票种、`price_rules` 派生价规则、`dynamic_pricing_rules` 动态定价 | 与通票同域、同页、配错高频 | 各 ~0.5 人日（照抄本试点） |
| P1 | `entitlement_templates`（本试点） | 已完成 | — |
| P2 | 合同模板 / 审批链模板、角色/权限模板、公告模板 | 配置类、误配需下线保历史 | 各 ~1 人日（域分散） |
| P2 | 商户 / 场地 / 点位主数据 | 停业需存档不删（结算历史依赖） | 各 ~1 人日 |
| P3 | 交易类（订单/核销/结算流水） | **只存档不硬删**（财税留存合规），删除轴应禁用或走更高审批 | 逐表评估，~2 人日/域 |

**取舍与未覆盖面（诚实标注）**：
- 试点删除审批为**单级**（申请→一次审批）。多级审批链未做；若某实体需要逐级上提，应改接 `supervisor_review` 的 rank 逐级机制。
- 删除审批状态机在实体自身列，**不是**统一审批中心。跨实体聚合"待审批删除"看板未做（每实体各查各的）。
- 交易类实体（P3）的"删除"语义存疑——多数应**禁止硬删**，仅允许存档/冲销。推广前需逐域与业务确认删除是否合法。
- 本试点**不引入**批量存档/删除。
