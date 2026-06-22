# V099–V124 新功能开发与验收统一规格 v0.1

> 范围：本会话新落地的迁移 **V099–V124**（含 V112/V113/V114/V117/V122 旁支，本文聚焦任务清单 19 项）与 OpenAPI **386 个 operation** 中新增的相关端点。
> 定位：把散落/缺失于各 PRD / dev-design / 验收文档中的新功能，统一补成**单一可开发 + 可验收**规格，作为开发与测试的承接锚点。
> 实证原则：每条数据表迁移号、operationId / 路径 / 权限码、事件名均经 `grep` 后端 / 迁移 / `docs/openapi/openapi-p0.json` 核对；不确定者标“待确认”，不臆造。
> 决策溯源：`docs/business-decision-log-v0.1.md` §36 R6-D01..D14（已业务确认 2026-06-19）。
> 编写日期：2026-06-20。

---

## 0. 阅读约定与全局实证结论

### 0.1 权限码位置
OpenAPI 端点的权限码以 **`x-permission`** 扩展字段落在每个 operation 上（如 `x-permission: risk_alert.report`）。本文“权限点”一节直接引用该字段并与迁移 `permissions` seed 交叉核对。`x-permission: not_applicable` 表示 C 端只读、无后台权限码（积分查询/兑换类）。

### 0.2 事件名命名两类
后端事件名存在两种形态，二者并存且**均为代码真实常量**：
- **业务事件名**（点分隔、过去式/状态式，如 `points.earned`、`exception.resolved`、`amusement.incident.over_capacity`）——领域事件 / audit 落库的 `event_name`。
- **能力码 / GuardedAction 码**（如 `finance.cam.allocate`、`bar.order_item.void`）——准入闸 capability，亦可能复用为审计 `permission_code`。

凡文档字典名与代码不符，**以代码为准并注明**（见各功能“事件”小节）。

### 0.3 19 项功能 operationId / 事件 实证总览

| # | 功能 | 主迁移 | 有无 operationId | 有无业务事件 | 备注 |
|---|------|--------|:---:|:---:|------|
| 1 | 准入闸 over_contract_scope/over_space | V099 | 间接（无独立端点，挂在被守护动作上） | ✅ `admission.*` | 策略 seed 行，判定在 AdmissionGuard |
| 2 | 积分入账引擎 earn/confirm/deduct | V100/V101 | ✅ 查询/转赠/冻结类 | ✅ `points.earned/deducted` | pending→confirmed→reversed |
| 3 | 订台定金 | （读 V-既有 bar_deposit_rules） | ✅ `createBarDepositRule` / `payBarReservationDeposit` | 见 bar 事件 | CustomerService 读规则 |
| 4 | 存酒散客/核身/转赠/争议 | V102 | ✅ `createSavedBottle` 等 6 个 | ✅ `inventory.saved_bottle_*` | customer_id 可空 |
| 5 | pour cost 强制采集 | （V-既有 + 服务层） | 无独立端点（挂 voidBarOrderItem/出品） | ✅ `inventory.loss_exception` | `POUR_VOLUME_REQUIRED` 拒单 |
| 6 | 统一异常责任链 | V103 | ✅ `report/claim/transfer/escalate/resolve/dismiss` | ✅ `exception.*` | 5 态状态机 |
| 7 | 游乐超容 over_capacity | V104 | 无独立端点（挂核销）+ 配置端点 | ✅ `amusement.incident.over_capacity` | 软/硬阈值 |
| 8 | 对账差异强制化 | V105 | ✅ `createFinanceReconciliationDifference` | ✅ `finance.reconciliation.*` | expected/actual/source |
| 9 | 企微违规命中 | V106 | ✅ `listWecomRiskHits`/`reviewWecomRiskHit` | ✅ `wecom.risk.hit_detected/_reviewed` | 规则码 seed |
| 10 | MDM 门控 + 设备生命周期 | V107 | ✅ `markTerminalDevice*`/`handleMdmViolation` | ✅ `terminal.device.*`/`mdm.violation.handled` | repair/lost/found/scrapped/blocked |
| 11 | 库存差异→AR/AP | V108 | ✅ `postInventoryVariance` | ✅ `finance.inventory_variance.post` | source_type=inventory_difference |
| 12 | 第三方回流渠道语义 | V109 | ✅ `runChannelAutoReconciliation` | ✅ `integration.channel_reconciliation.*` | channel/external_channel_order_id |
| 13 | 综合体计费 + CAM true-up | V110/V121 | ✅ `billTurnoverRent`/`recordDepositAction`/`allocateCam`/`trueUpCam` | ✅ `finance.{turnover_rent,deposit,cam}*` | refund_direction |
| 14 | 终端 capability 补全 + 拒绝留痕 | V111 | ✅ `listCapabilitySwitches`/`updateCapabilitySwitch` | ✅ `terminal.switch.*` | D5 10 端权威码 |
| 15 | 工单 SLA 自动升级 | V120 | 无独立端点（定时任务） | ✅ `merchant_service.work_order.escalated` | sla_breached→escalated |
| 16 | 检修供应商 FK | V116/V123 | 无独立端点（数据约束） | n/a | NOT VALID→VALIDATE |
| 17 | void 驳回反向冲销 | V117/V124 | ✅ `decideBarOrderItemVoid` | ✅ `bar.order_item_void_decided` | waiver_reversal/refund_reversal |
| 18 | 集团多项目视图 | V115 | ✅ `getGroupOverview` | n/a（ETL job） | segment_type=project |
| 19 | 多综合体 project_id scoped 通道 | （无新表） | ✅ 多 `listXxxByProject` | n/a | 仓储层方法 |

> 实证：`openapi-p0.json` 共 **386** 个 operation（`python3` 解析 paths 计数）；上表“有 operationId”均经 grep 命中并附 path/perm。

---

## 1. 准入闸 over_contract_scope / over_space（V099）

**需求/决策来源**：R6 全闭环准入判定路径（business-decision-log §22/§24/§30 隔离与准入口径）；PRD：`docs/business-operations-layer-spec-v0.2.md`（主管复核）。补 V068 seed 缺失的 over_* 策略，消除“无 over_* 策略=判定引擎空转”的 fail-open。

**数据表**：`supervisor_review_policies`（V099 INSERT 行，不新建表）。关键列：`policy_code`(唯一)、`action_type`、`object_type`、`trigger_mode`∈{over_space, over_contract_scope}、`outcome`(V068 chk_srp_outcome 仅 allow/warn/block，本批一律 `block`)、`approval_level`∈{one_level_up, two_levels_up, top_authority}、`required_level`(2/3/4)、`enabled=true`。
匹配约束（重要）：`JdbcAdmissionPolicyRepository.findEnabledPolicy` 按 `(action_type, object_type)` 命中且 `ORDER BY policy_code LIMIT 1`——每对只生效一条；故本批仅补 V068 未占用的对。

**分配口径**：
- `over_space`（对象可解析到 space → 物理越界 → 上两级 `two_levels_up`/required 3）：bar.bill_reopen、amusement.reopen、amusement.incident_close、amusement.group_booking_approve。
- `over_contract_scope`（无空间归属 → 经营范围越界）：酒吧/票务/库存类 → 上一级 `one_level_up`/required 2；财务类（finance.refund / finance.offline_payment_confirm / finance.locked_backfill_approve）→ 最高权限 `top_authority`/required 4。

**API/operationId**：无独立端点。判定挂在**被守护的业务动作**上（`@GuardedAction` + `AdmissionGuard`）。`block` → `SupervisorReviewService.blockForAdmission` 建 pending 复核。

**事件**（grep 后端真实常量，`admission.*`）：
- `admission.blocked`（命中 over_*，先卡住）、`admission.review`、`admission.escalated`、`admission.released`、`admission.warned`。
- **fail-closed 证据事件**：`admission.eval_failed_closed`（判定异常按卡住处理）、`admission.space_unresolved_closed`（空间无法解析按卡住处理）、`admission.eval_skipped`。

**状态流转**：业务动作 → AdmissionGuard 命中 over_* 策略 → `block` → 建 supervisor_review(pending) → 上 N 级审批放行/驳回。

**权限点**：随被守护动作的 capability（如 `finance.refund`、`bar.bill_reopen`）；放行审批走 supervisor_review 角色等级（required_level）。

**闭环**：fail-closed——判定异常 / 空间不可解析时一律按 `block` 处理，不放行（`admission.eval_failed_closed` / `admission.space_unresolved_closed`）。

**G/W/T 验收**：
- G: 某 `(action_type, object_type)` 已被 V099 补 over_space 策略且 enabled。W: 操作员对超出合同租赁空间的对象发起该动作。T: 返回 block，落 `admission.blocked` 事件，生成 pending supervisor_review，required_level=3（two_levels_up），未达级别不可放行。
- G: AdmissionGuard 评估过程抛异常或对象 space 无法解析。W: 执行被守护动作。T: 系统 fail-closed 卡住（非放行），落 `admission.eval_failed_closed` 或 `admission.space_unresolved_closed`。
- G: 财务类 finance.refund 命中 over_contract_scope。W: 发起退款。T: required_level=4（top_authority），仅最高权限可放行。

---

## 2. 积分入账引擎 earn/confirm/deduct（V100 + V101）

**需求/决策来源**：R6-D06（积分 earn 触发点：支付入 pending、履约转 confirmed、退款按比例 deduct）。PRD：`docs/finance-settlement-product-prd-v0.1.md`、积分模块。

**数据表**：
- `customer_points_ledger`（V100 扩列）：新增 `change_type`(earn/deduct，兼容既有 expire/check_in/transfer)、`status`(pending/confirmed/reversed，默认 confirmed)、`source_doc_type`、`source_doc_id`。幂等唯一索引 `uq_customer_points_ledger_source_doc (source_doc_type, source_doc_id, change_type)` partial（仅带单据流水）。
- `audit_events`（V101 扩列）：新增 `event_category`(loyalty/order/refund...)、`subject_type`(customer_points/customer_points_transfer...)，两可空列 + partial 索引。

**API/operationId**（grep openapi）：
- `getCustomerPoints` GET `/api/v1/customer/points`（perm not_applicable）
- `getCustomerPointsLedger` GET `/api/v1/customer/points/ledger`（not_applicable）
- `redeemCustomerPoints` POST `/api/v1/customer/points/redemptions`（not_applicable）
- `customerPointsTransfer` POST `/api/v1/customer/points/transfers`（perm `customer.points.transfer`）
- `freezePointsAccount` POST `/api/v1/loyalty/points-accounts/{customer_id}/freeze`、`unfreezePointsAccount` …/unfreeze（perm `customer.points.freeze`）
- `savePointsTransferLimit` POST `/api/v1/loyalty/points-transfer-limits`（perm `customer.points.transfer_limit`）
> earn/confirm/deduct 本身**无独立端点**：由领域事件驱动（`PointsEarnRequestedEvent`→`earnPointsPending`、`PointsConfirmRequestedEvent`→`confirmPoints`、退款→`LoyaltyService.deductPoints`）。

**事件**（grep 真实常量）：`points.earned`、`points.deducted`、`points.transferred`、`points.transfer_limit_saved`。
> 注：代码服务常量 `STATUS_PENDING/CONFIRMED/REVERSED`（`LoyaltyService`）确认状态机；earn 写入时事件名为 `points.earned`（pending 入账亦发该名，按 status 字段区分），confirm 转 confirmed 为幂等 noop（无 pending 时不动作）。

**状态流转**：支付成功 → `earnPointsPending`(status=pending) → 履约/核销 → `confirmPoints`(pending→confirmed) → 退款 → `deductPoints`(按已 confirmed 比例冲减，status→reversed)。幂等：同 `(source_doc_type, source_doc_id, change_type)` 至多一条。

**权限点**：转赠 `customer.points.transfer`；转赠限额 `customer.points.transfer_limit`；冻结/解冻 `customer.points.freeze`。查询/兑换为 C 端 not_applicable。

**闭环**：权益闭环——pending 不可用、confirmed 方可用、退款等比例 reversed；审计闭环——经 `event_category=loyalty` + `subject_type=customer_points` 可按类目/主体检索全链路。

**G/W/T 验收**：
- G: 会员订单支付成功。W: 触发 PointsEarnRequestedEvent。T: ledger 写一条 status=pending、change_type=earn、source_doc_* 非空；积分不可用；落 `points.earned`。
- G: 该订单已 pending 入账。W: 履约完成触发 PointsConfirmRequestedEvent。T: 该流水 pending→confirmed；重复触发为幂等 noop。
- G: 已 confirmed 100 积分的订单发生 50% 退款。W: 调 deductPoints(refundRatioBp=5000)。T: 冲减 50 积分，写 change_type=deduct/status=reversed 流水，落 `points.deducted`；同一退款单重放不重复冲减（唯一索引）。

---

## 3. 订台定金（CustomerService 读 bar_deposit_rules）

**需求/决策来源**：bar PRD（`docs/bar-business-operations-product-prd-v0.1.md`）订台定金；CustomerService 复用 `bar_deposit_rules`。

**数据表**：`bar_deposit_rules`（既有表，本会话由服务层读取；`BarDepositRuleRequest`/`JdbcBarRepository`/`JdbcCustomerRepository` 引用）。

**API/operationId**（grep openapi）：
- `createBarDepositRule` POST `/api/v1/bar/deposit-rules`（perm `bar.deposit_rule.manage`）
- `payBarReservationDeposit` POST `/api/v1/bar/reservations/{id}/pay-deposit`（perm `bar.reservation.manage`）
- `checkInBarReservationDeposit` POST `/api/v1/bar/reservations/{id}/checkin`（perm `bar.reservation.manage`）

**事件**：随 bar 预订/结算事件（`bar.*`，详见 BarService）；CustomerService 仅读 `bar_deposit_rules` 计算应收定金，不另发事件（**待确认**是否有 `bar.reservation.deposit_paid` 独立事件，未在 grep 命中独立常量）。

**权限点**：规则维护 `bar.deposit_rule.manage`；定金支付/核销 `bar.reservation.manage`。

**资金闭环**：定金按规则计算 → 支付 → check-in 时核销/抵扣账单。

**G/W/T 验收**：
- G: 已配置 bar_deposit_rules。W: 客服为某订台调 payBarReservationDeposit。T: 按规则计算定金金额并记录；CustomerService 读取的规则与 bar 域一致。
- G: 已支付定金的订台到店。W: 调 checkInBarReservationDeposit。T: 定金抵扣进账单，状态流转 check-in。

---

## 4. 存酒散客 / 核身 / 转赠 / 争议（V102）

**需求/决策来源**：R6-D05（散客可存、customer_id 可空、不限瓶数、存期默认 90 天后台可配、作废主管复核）。openapi SavedBottleRequest/SavedBottleClaimRequest 新字段。

**数据表**：
- `saved_bottles`（V102）：`customer_id` DROP NOT NULL（散客化）；补 `registration_photo_refs` JSONB、`manual_note`、转赠受让 4 列（`transferred_at`/`transfer_to_name_or_nickname`/`transfer_to_phone`/`transfer_to_phone_hash`）；`chk_saved_bottles_status` 扩为 {stored,claimed,cancelled,expired,pending_identity_review,disputed_frozen,**transferred,disputed**}。
- `saved_bottle_transfers`（V102 新表）：`saved_bottle_id` FK、受让信息、`transfer_quantity`(>0)、`operator_id`、`status`(CHECK 仅 'transferred')、`evidence_attachment_ids` JSONB、`created_at`。

**API/operationId**（grep openapi，perm 均 `bar.saved_bottle.manage`）：
- `createSavedBottle` POST `/api/v1/inventory/saved-bottles`
- `listSavedBottles` GET `/api/v1/inventory/saved-bottles`
- `claimSavedBottle` POST `…/{id}/claim`
- `cancelSavedBottle` POST `…/{id}/cancel`
- `transferSavedBottle` POST `…/{id}/transfer`
- `disputeSavedBottle` POST `…/{id}/dispute`

**事件**（grep 真实常量）：`inventory.saved_bottle_claim`、`inventory.saved_bottle_cancel`、`inventory.saved_bottle_transfer`、`inventory.saved_bottle_dispute`。

**状态流转**：stored →(claim)→ claimed；stored →(transfer)→ transferred（原瓶终态，写 saved_bottle_transfers）；stored →(dispute)→ disputed（受理态，区别于高价值作废冻结 disputed_frozen）；stored →(cancel)→ cancelled（主管复核）；到期 → expired；散客登记 → pending_identity_review（核身）。

**权限点**：`bar.saved_bottle.manage`（所有动作）。作废走主管复核（R6-D05）。

**库存/权益闭环**：散客凭手机号存酒（customer_id 可空）；转赠原瓶终态留痕 + 证据；争议入状态机冻结取酒；作废主管复核 + 留痕。

**G/W/T 验收**：
- G: 无 customer_id 的散客。W: createSavedBottle 仅传手机号+姓名/昵称+登记照片。T: 记录成功（customer_id NULL），状态 stored/pending_identity_review，registration_photo_refs 落库。
- G: 一瓶 stored 酒。W: transferSavedBottle 传受让人信息+证据。T: 原瓶状态 transferred（终态），saved_bottle_transfers 新增一行 status='transferred'，落 `inventory.saved_bottle_transfer`。
- G: 一瓶 stored 酒被举报争议。W: disputeSavedBottle。T: 状态→disputed，取酒被阻断，落 `inventory.saved_bottle_dispute`。

---

## 5. pour cost 强制采集（bar 按杯出品 actual_pour_volume 强制）

**需求/决策来源**：REQ-22 / R6（bar §17.4 按杯出酒量/损耗）。代码：`BarOrderService`（lines 237–342）。

**数据表**：bar 出品/损耗既有列（`BarProductRecord.standardPourVolume`、出品项 `actual_pour_volume`、`BarPourLoss`）。无新迁移；为服务层强制采集逻辑。

**API/operationId**：无独立端点。挂在出品/结算路径上（`BarOrderService.recordPourCost`）。

**事件**（grep 真实常量）：`inventory.loss_exception`（按杯出品 actual>standard 超阈值或兜底模式时落）。

**模式（服务开关）**：
- 拒绝模式（默认，从严）：按杯出品缺 `actual_pour_volume` → 抛 **`POUR_VOLUME_REQUIRED`** 拒单。
- 标准量兜底模式：缺量时按 `standard_pour_volume × quantity` 计、loss=0，但**必落** `inventory.loss_exception`（reason=`pour_volume_fallback`），消除“不传则 loss=0”静默绕过。
- 非按杯出品项（未配置 standard_pour_volume）：维持原行为，loss=0。

**权限点**：随 bar 出品/结算 capability（`bar.order_item.*`）。

**库存/成本闭环**：按杯强制采集实际倒酒量 → 与标准量比对算损耗 → 超阈值落异常，杜绝静默 loss=0。

**G/W/T 验收**：
- G: 某商品配置了 standard_pour_volume（按杯出品）、系统处于拒绝模式。W: 出品时未传 actual_pour_volume。T: 拒单，错误码 `POUR_VOLUME_REQUIRED`。
- G: 同上、系统处于兜底模式。W: 缺 actual_pour_volume 出品。T: 按 standard×quantity 计、loss=0，落 `inventory.loss_exception`(reason=pour_volume_fallback)。
- G: 按杯出品且 actual_pour_volume > standard。W: 出品。T: 记损耗，超阈值落 `inventory.loss_exception`。

---

## 6. 统一异常责任链 risk_alerts / risk_alert_actions（V103）

**需求/决策来源**：`docs/evidence-risk-layer-spec-v0.2.md` §8 统一异常责任链。建立跨域汇聚层，统一上报→认领→转交→升级→关闭。

**数据表**：
- `risk_alerts`（V103 新表）：`alert_id` PK、`source_domain`(CHECK 10 域：ticketing/bar/amusement/inventory/finance/wecom/device/thirdparty/analytics/platform)、`alert_type`、`risk_level`(normal/suspicious/high/critical)、`status`(open/handling/escalated/resolved/dismissed)、`summary`、`first_reported_by`、`first_handler_id`、`escalated_to`、`resolved_by`、`resolution_conclusion`、`project_id`、`merchant_id`、`idempotency_key`(唯一 partial)。
  - **关键 CHECK**：`chk_risk_alerts_resolved_conclusion`——status=resolved 时 `resolution_conclusion` 必须非空（“异常关闭必须有结论”）。
- `risk_alert_actions`（V103 新表，append-only）：`action_id` PK、`alert_id` FK CASCADE、`action_type`(CHECK report/claim/transfer/escalate/resolve/dismiss)、`actor_id`、`from_status`、`to_status`、`reason`、`evidence_refs` JSONB(必为 array)。

**API/operationId**（grep openapi，evidence 统一域）：
- `reportRiskAlert` POST `/api/v1/evidence/risk-alerts`（perm `risk_alert.report`）
- `searchRiskAlerts` GET `/api/v1/evidence/risk-alerts`（`risk_alert.view`）
- `getRiskAlertDetail` GET `…/{alert_id}`（`risk_alert.view`）
- `claimRiskAlert` POST `…/{alert_id}/claim`（`risk_alert.claim`）
- `transferRiskAlert` POST `…/{alert_id}/transfer`（`risk_alert.transfer`）
- `escalateRiskAlert` POST `…/{alert_id}/escalate`（`risk_alert.escalate`）
- `resolveRiskAlert` POST `…/{alert_id}/resolve`（`risk_alert.resolve`）
- `dismissRiskAlert` POST `…/{alert_id}/dismiss`（`risk_alert.dismiss`）
> 注意区分**孤岛 analytics 域**端点（旧）：`createRiskAlert` POST `/api/v1/analytics/risk-alerts`（perm `analytics.risk_alert.manage`）、`listRiskAlerts` GET（`analytics.risk_alert.view`）、`resolveRiskAlert` POST `/api/v1/analytics/risk-alerts/{id}/resolve`（`analytics.risk_alert.manage`）。统一层为 `/evidence/risk-alerts/*`，孤岛 `analytics_risk_alerts` 为既有桥接对象（V103 不强制改孤岛）。

**事件**（grep 真实常量，`exception.*`）：`exception.reported`、`exception.claimed`、`exception.transferred`、`exception.escalated`、`exception.resolved`、`exception.dismissed`、`exception.alerts_viewed`，及各 `_failed` 配对（report_failed/claim_failed/transfer_failed/escalate_failed/resolve_failed/dismiss_failed/alerts_view_failed）。
> 字典名 vs 代码：业务侧称“责任链 5 态/6 动作”，代码事件统一以 `exception.*` 前缀发布（与统一层动作 report/claim/transfer/escalate/resolve/dismiss 对应）。**以代码为准**。

**状态流转**（5 态）：open →(claim)→ handling →(escalate)→ escalated →(resolve)→ resolved / →(dismiss)→ dismissed。resolve 必须携结论（DB CHECK 强制）。

**权限点**（V103 seed，domain=evidence）：`risk_alert.report/claim/transfer/escalate/resolve/dismiss/view`；resolve/dismiss `requires_approval=TRUE`、transfer/escalate risk_level=high。授予 `role_platform_admin`。

**闭环**：跨域异常统一汇聚 + append-only 责任链留痕；resolve 强制结论（DB CHECK）保证关闭可审计。

**G/W/T 验收**：
- G: 任一域产生异常。W: reportRiskAlert（带 idempotency_key）。T: risk_alerts 建 open 行，risk_alert_actions 写 report 流水，落 `exception.reported`；重放同 idempotency_key 不重复建。
- G: 一条 escalated 告警。W: resolveRiskAlert 不传 resolution_conclusion。T: 被拒（DB CHECK `chk_risk_alerts_resolved_conclusion`），无法置 resolved。
- G: open 告警。W: claim→transfer→escalate→resolve（带结论）。T: status 依次流转，risk_alert_actions 4 条 append-only 留痕，from/to_status 对应，落对应 `exception.*` 事件。

---

## 7. 游乐超容 over_capacity（V104）

**需求/决策来源**：R6-D10（双阈值：硬上限阻断 + 软上限告警放行，处置模式后台可切换；超容必落事件）。amusement §6.4/§7.18。

**数据表**：
- `amusement_incidents`（V104 扩列）：补 `incident_type`(CHECK accident/fault/complaint/risk/**over_capacity**，默认 accident 回填存量)。
- `amusement_over_capacity_policy`（V104 新表，项目维一行）：`project_id` PK FK、`operating_capacity`(软上限，默认 0)、`disposal_mode`(CHECK **block_hard**/warn_only，默认 block_hard)、`enabled`。未配置项目退化为“仅硬上限阻断、无软上限告警”（向后兼容，`AmusementOverCapacityPolicy.disabled` 占位）。

**API/operationId**：超容判定**无独立端点**，挂在核销路径上。配置端点见 capacity_config 管理（`amusement.capacity_config.manage`，grep 命中 `amusement.capacity_config.changed/viewed`）。

**事件**（grep 真实常量）：`amusement.incident.over_capacity`（AmusementService line 851；超容达软/硬上限均落 incident + 该事件）。

**处置模式**：
- `block_hard`（默认）：达安全容量（硬上限，V083）阻断放行 + 达运营容量（软上限）告警放行，**均落 incident + 事件**。
- `warn_only`：**安全容量（硬上限）仍阻断（F-OPS-03 不可旁路、触发自动停运）**，运营容量（软上限）告警放行；二者均**必落 incident + 事件**（`disposal_mode` 仅作留痕标注）。

**权限点**：容量配置 `amusement.capacity_config.manage`；超容事件随核销 capability。

**闭环**：核销在场达软/硬上限 → 落 over_capacity incident + 事件（含放行人/原因/时段）；硬上限 block_hard 不可人工绕过（安全优先）。

**G/W/T 验收**：
- G: 项目配置 operating_capacity=80、安全容量=100、disposal_mode=block_hard。W: 核销使在场达 80。T: 告警放行，落 `amusement.incident.over_capacity`（incident_type=over_capacity）。
- G: 同上、在场达 100。W: 再核销。T: 硬阻断（SESSION_CAPACITY_EXHAUSTED 口径），落 over_capacity incident + 事件。
- G: disposal_mode=warn_only。W: 在场达安全容量。T: **仍硬阻断（SESSION_CAPACITY_EXHAUSTED，F-OPS-03 不可旁路）**，必落 incident + 事件。
- G: 项目未配置 over_capacity_policy。W: 核销。T: 退化为仅硬上限阻断、无软上限告警（向后兼容）。

---

## 8. 对账差异强制化（V105）

**需求/决策来源**：R6-D07（平台侧 expected 系统取数禁手填；第三方侧 source=manual 进复核）。finance §6C。

**数据表**：`finance_reconciliation_differences`（V105 扩列 + CHECK 收紧）：补 `expected_amount_cent`、`actual_amount_cent`、`source`(CHECK system/manual，默认 system)；`difference_type` 收紧为四类 `chk_frd_difference_type`(amount/count/status/missing，先 UPDATE 遗留 'amount_mismatch'→'amount')。

**API/operationId**（grep openapi）：`createFinanceReconciliationDifference` POST `/api/v1/finance/reconciliation/differences`（perm `finance.reconciliation.difference.create`）。

**事件**（grep 真实常量）：`finance.reconciliation.difference.create`、`finance.reconciliation_batch.viewed`、`finance.reconciliation.view`。

**权限点**（V105 seed）：`finance.reconciliation.difference.create`（domain=finance, high, requires_approval=TRUE），授予 role_platform_admin。

**资金闭环**：平台侧 expected 系统取数（source=system）；第三方侧未接入前 source=manual 进复核；纯手填差异（无系统取数支撑）由服务层拒绝。

**G/W/T 验收**：
- G: 平台侧对账。W: createFinanceReconciliationDifference 提交时 source=system。T: expected 由系统取数，差异四类之一落库；纯手填无 expected 被服务层拒绝。
- G: 第三方接口未接入。W: 人工录入差异 source=manual。T: 强制进入复核流。
- G: 传入 difference_type='amount_mismatch'。T: 被 CHECK 拒绝（仅 amount/count/status/missing）。

---

## 9. 企微违规命中 wecom_risk_hits（V106）

**需求/决策来源**：R6-D01（企微授权与配置）。data-risk §11.4A。命中表零建表 → 补齐命中表 + 规则码 seed + 复核队列衔接。

**数据表**：
- `wecom_risk_hits`（V106 新表）：`hit_id` PK、`conversation_id`/`message_id` FK CASCADE、`rule_code`、`risk_level`(CHECK normal/suspicious/high/critical)、`matched_snippet`(脱敏)、`status`(CHECK open/reviewing/confirmed/dismissed，默认 open)、`reviewer_id`、`review_note`、`project_id`、`alert_id`(关联复核队列)。唯一 `uq_wecom_risk_hits_message_rule (message_id, rule_code)` 幂等。
- `analytics_risk_rules`（V106 seed 5 条 domain=compliance）：`STAFF_PRIVATE_DEAL`(high)、`STAFF_PRIVATE_PAYMENT`(critical)、`ABUSE`(high)、`ILLEGAL_PROMISE`(high)、`LURE_PRIVATE_WECHAT`(high)；keywords 落 `rule_config_json`（由硬编码 RISK_KEYWORDS 升级为可配置）。
- `analytics_risk_alerts`（V106 补 `source_domain` 可空列）：企微命中标 `source_domain='wecom'`（统一表就位后按此口径迁移）。

**API/operationId**（grep openapi，**两个 listWecomRiskHits** 并存）：
- `listWecomRiskHits` GET `/api/v1/compliance/wecom/risk-hits`（perm `wecom.risk_hit.view`）
- `listWecomRiskHits` GET `/api/v1/integration/wecom/risk-hits`（perm `integration.provider_adapter.view`）— 集成域只读视图
- `reviewWecomRiskHit` POST `/api/v1/compliance/wecom/risk-hits/{id}/review`（perm `wecom.risk_hit.review`）
- `syncWecomConversation` POST `/api/v1/compliance/wecom/conversations/sync`（perm `wecom.sync`）

**事件**（grep 真实常量）：`wecom.risk.hit_detected`（命中检出）、`wecom.risk.hit_reviewed`（复核处置）、`integration.wecom_risk_hit.viewed`（集成域查询）、`integration.wecom.sync`。
> 错误码常量：`WECOM_RISK_HIT_NOT_FOUND`、`WECOM_RISK_HIT_ALREADY_RESOLVED`。

**状态流转**：open →(领取)→ reviewing →(确认违规)→ confirmed / →(误报)→ dismissed。

**权限点**（V106 seed，domain=compliance）：`wecom.risk_hit.view`(medium)、`wecom.risk_hit.review`(high, requires_approval=TRUE)。

**第三方占位**：企微为外部依赖；同步走 `wecom.sync`，规则可配（rule_config_json keywords 后台增减、enabled 启停生效）。

**G/W/T 验收**：
- G: 启用规则 STAFF_PRIVATE_PAYMENT。W: syncWecomConversation 拉到含“私收款”消息。T: wecom_risk_hits 建 open 命中（rule_code=STAFF_PRIVATE_PAYMENT, risk_level=critical），matched_snippet 脱敏，落 `wecom.risk.hit_detected`，写 analytics_risk_alerts(source_domain=wecom)。
- G: 同一消息同一规则码已有命中。W: 重复 sync。T: 不产生重复命中（`uq_wecom_risk_hits_message_rule`）。
- G: 一条 open 命中。W: reviewWecomRiskHit 确认违规。T: status→confirmed，reviewer_id/review_note 回填，落 `wecom.risk.hit_reviewed`；对已处置命中再 review 报 `WECOM_RISK_HIT_ALREADY_RESOLVED`。

---

## 10. MDM 门控 + 设备生命周期（V107）

**需求/决策来源**：R6-D03（商业 MDM 未签约前“终端开关 + 人工准入”代偿）。android §12.3 / `docs/mdm-android-enterprise-implementation-plan-v0.1.md`。

**数据表**：`terminal_devices`（V107）：`status` 补 CHECK（bound/returned/unbound/disabled/**repair/lost/found/scrapped/blocked**）；`management_status` 补 lost/retired/blocked（增量于 unmanaged/enrolled/compliant/violation）。`mdm_policy_violations`（V107）：补 `blocked_at`、`disposition`(CHECK acknowledged/device_blocked/dismissed)。
> 命名映射（以代码为准）：文档 in_use→代码 bound；checked_out→returned；retired→scrapped（设备报废）；management_status 的 retired=纳管退役（语义不同）。

**API/operationId**（grep openapi）：
- `markTerminalDeviceRepair` POST `/api/v1/terminals/devices/{id}/repair`（perm `terminal.device.lifecycle`）
- `markTerminalDeviceLost` POST `…/{id}/lost`（`terminal.device.lifecycle`）
- `markTerminalDeviceFound` POST `…/{id}/found`（`terminal.device.lifecycle`）
- `markTerminalDeviceScrap` POST `…/{id}/scrap`（`terminal.device.lifecycle`）
- `returnTerminalDevice` POST `/api/v1/terminals/devices/return`（perm `terminal.device.bind`）
- `detectMdmViolations` POST `/api/v1/terminals/mdm/violations/detect`（perm `mdm.device.manage`）
- `reportMdmPolicyViolation` POST `/api/v1/terminals/mdm/violations`（`mdm.device.manage`）
- `handleMdmViolation` POST `/api/v1/terminals/mdm/violations/{id}/handle`（perm `mdm.violation.handle`）
- `enrollMdmDevice` POST `/api/v1/terminals/mdm/devices/enroll`（`mdm.device.manage`）

**事件**（grep 真实常量）：`terminal.device.repaired`、`terminal.device.lost`、`terminal.device.found`、`terminal.device.scrapped`（及 `_failed` 配对）；`mdm.violation.handled`、`mdm.policy_checked`、`device.policy_violation`。

**状态流转**：bound ⇄ repair；bound →lost→ found（可再 bound）/ →scrapped（终态）/ →blocked（终态，违规处置 device_blocked 联动 management_status=blocked + 撤销凭证）。

**权限点**（V107 seed）：`terminal.device.lifecycle`(terminal, high, requires_approval=TRUE)、`mdm.violation.handle`(terminal, high, requires_approval=TRUE)。

**设备闭环**：违规检测（detectMdmViolations 服务端检测，非纯外部上报）→ 处置 handleMdmViolation：disposition=device_blocked → 设备 blocked + management_status=blocked + 撤销凭证；=dismissed 误报驳回。

**G/W/T 验收**：
- G: 一台 bound 设备送修。W: markTerminalDeviceRepair。T: status→repair，落 `terminal.device.repaired`。
- G: 一台挂失设备找回。W: markTerminalDeviceFound。T: status→found（可再 bound），落 `terminal.device.found`。
- G: detectMdmViolations 检出一台违规设备。W: handleMdmViolation disposition=device_blocked。T: mdm_policy_violations 置 blocked_at/disposition，设备 management_status=blocked、凭证撤销，落 `mdm.violation.handled`。

---

## 11. 库存差异 → AR/AP（V108）

**需求/决策来源**：R6-D08（按差异原因路由科目方向；供应商责任→AP）。finance-ar-ap §6D。

**数据表**：
- `stocktake`（V108 新表）：盘点单头，`status`(CHECK counting/reviewing/closed)、`warehouse_id`、`merchant_id`、`business_date`。
- `inventory_variance`（V108 新表）：`variance_qty`/`variance_amount_cent`(带符号)、`reason`(CHECK damage/shortage/overage/supplier_fault)、`supplier_id`(责任供应商→AP 对手方)、`review_status`(CHECK pending/approved/rejected)、`posted_ar_ap_item_id` FK(幂等防重复入账，唯一 partial)。
- `ar_ap_items.source_type` 允许 `inventory_difference`（DB 无 CHECK，服务层 SOURCE_TYPES 枚举）。

**API/operationId**（grep openapi）：`postInventoryVariance` POST `/api/v1/finance/inventory-variances/post`（perm `finance.inventory_variance.post`）。

**事件**（grep 真实常量）：`finance.inventory_variance.post`（能力码/审计）、`finance.inventory_variance_post`。`inventory.counted` 为盘点侧。

**状态流转**：variance review_status pending →(复核)→ approved（可入账）/ rejected（不入账）；approved → finance 生成 ar_ap_items(source_type=inventory_difference) 并回填 posted_ar_ap_item_id。

**权限点**（V108 seed）：`finance.inventory_variance.post`(finance, high, requires_approval=TRUE)。

**资金闭环**：报损/盘亏→损益（费用）；盘盈→冲减成本；supplier_fault→AP（responsible supplier_id）。幂等：同 variance 行只入账一次（`uq_inventory_variance_posted`）。

**G/W/T 验收**：
- G: 一条 approved 的 inventory_variance（reason=supplier_fault, supplier_id=S1）。W: postInventoryVariance。T: 生成 ar_ap_items(source_type=inventory_difference, AP, 对手方 S1)，回填 posted_ar_ap_item_id，落 `finance.inventory_variance.post`。
- G: 同一 variance 已入账。W: 重复 post。T: 幂等不重复入账（唯一索引）。
- G: review_status=pending/rejected。W: post。T: 拒绝入账（仅 approved 可入账）。

---

## 12. 第三方回流渠道语义（V109）

**需求/决策来源**：R6-D02（资料未到=开关 off + mock + 映射骨架先行）。third-party §14.11b/§2B。

**数据表**：
- `integration_order_syncs`（V109 扩列）：补 `channel`(CHECK meituan/dianping/douyin/koubei/eleme/alipay/wechat/internal/unknown，默认 unknown)、`external_channel_order_id`；渠道级幂等 `uq_integration_order_syncs_channel_order (channel, external_channel_order_id)` partial。
- `integration_reconciliation_differences`（V109 补 `source_domain` CHECK platform/thirdparty，默认 platform）：自动对账写 thirdparty，人工录入写 platform。

**API/operationId**（grep openapi）：`runChannelAutoReconciliation` POST `/api/v1/integration/reconciliation/channel-auto`（perm `integration.reconciliation.manage`）。`mapPayload` 见 `IntegrationOrderSyncService`/`IntegrationService`（适配器字段映射骨架）。

**事件**（grep 真实常量）：`integration.channel_reconciliation.completed`/`integration.channel_reconciliation.failed`；`integration.reconciliation_difference.created/resolved`。

**权限点**：`integration.reconciliation.manage`（运行自动对账）、`integration.reconciliation.resolve`、`integration.reconciliation.view`。

**第三方回写闭环**：mock 适配器产出标 `source=mock` 样例（R6-D02 严禁伪造对账）；渠道级幂等防重复回流；自动对账差异标 source_domain=thirdparty 衔接统一异常链。

**G/W/T 验收**：
- G: meituan 回流一笔订单（external_channel_order_id=X）。W: 重复回流同渠道同单号。T: 不重复（`uq_integration_order_syncs_channel_order`）。
- G: 第三方接口未接入。W: runChannelAutoReconciliation。T: 产出差异标 source_domain=thirdparty，落 `integration.channel_reconciliation.completed`；mock 数据带 source=mock 不污染真实对账。
- G: 传入未知 channel 值。T: 被 CHECK 拒绝（仅占位集合内）。

---

## 13. 综合体计费 + CAM true-up + refund_direction（V110 + V121）

**需求/决策来源**：finance-ar-ap §14（营业额抽成 + 保证金台账 + CAM 分摊）；V121 审计补 CAM 年度 true-up + 统一退款方向维度。

**数据表**：
- `turnover_rent_rules`（V110）：`rate_bps`(CHECK 0–10000)、`min/max_amount_cent`(保底/封顶 clamp)、`effective_from/to`、`status`(active/inactive)。
- `deposit_ledger`（V110）：`action`(CHECK collect/refund/deduct/forfeit)、`amount_cent`(>0)、`balance_after_cent`(>=0)、`ar_ap_item_id` FK。
- `cam_allocations`（V110 + V121 扩列）：`allocation_batch_id`、`weight_bps`(CHECK 0–10000)、`allocated_amount_cent`、唯一(batch,merchant)；V121 补 `settlement_period`、`trued_up`(防重复 true-up)。
- `cam_trueup_runs`（V121 新表）：`settlement_period`、`actual_total_amount_cent`、`prepaid_total_amount_cent`、`status`(completed/void)。
- `cam_trueup_lines`（V121 新表）：`diff_amount_cent`(实际应摊−已预收，带符号，多退少补)、`ar_ap_item_id` FK，唯一(run,merchant)。
- `finance_refunds`（V121 补 `refund_direction` CHECK original/cash/third_party/mixed，NOT NULL 回填 original)。
- `ar_ap_items.source_type` 增 turnover_rent/cam/deposit（服务层枚举）。

**API/operationId**（grep openapi）：
- `billTurnoverRent` POST `/api/v1/finance/turnover-rent/bills`（perm `finance.turnover_rent.bill`）
- `recordDepositAction` POST `/api/v1/finance/deposits/actions`（perm `finance.deposit.manage`）
- `allocateCam` POST `/api/v1/finance/cam/allocations`（perm `finance.cam.allocate`）
- `trueUpCam` POST `/api/v1/finance/cam/trueups`（perm `finance.cam.trueup`）

**事件**（grep 真实常量）：`finance.turnover_rent.bill`/`finance.turnover_rent_bill`、`finance.deposit_action`、`finance.cam.allocate`/`finance.cam_allocate`、`finance.cam.trueup`/`finance.cam_trueup`。

**权限点**（V110/V121 seed）：`finance.turnover_rent.bill`、`finance.deposit.manage`、`finance.cam.allocate`、`finance.cam.trueup`（均 finance, high, requires_approval=TRUE）。

**资金闭环**：营业额抽成（回流营业额来自 integration_order_syncs，只读）→ clamp 保底/封顶 → 应收；保证金四动作 + 运行余额；CAM 权重预收 → 年度 true-up 逐户差额（>0 补收应收 / <0 退还应付 / =0 不生成台账），生成 source_type=cam 的 ar_ap_items；退款统一 refund_direction 维度。

**G/W/T 验收**：
- G: 商户配 rate_bps=500、min=10000、max=50000 cent；回流营业额 80000。W: billTurnoverRent。T: 抽成 4000 < min → clamp 至 10000，生成应收 ar_ap_items(source_type=turnover_rent)。
- G: 某 settlement_period 已预收 CAM 合计 prepaid，实际 actual。W: trueUpCam。T: 逐户 diff=actual_allocated−prepaid；diff>0 补收应收、diff<0 退还应付、diff=0 不生成台账；cam_allocations.trued_up=true 防重复。
- G: 一笔退款。W: 创建 finance_refunds 不传 refund_direction。T: 默认 original（CHECK 通过）；传非枚举值被拒。

---

## 14. 终端开关 capability 补全 + 拒绝留痕（V111）

**需求/决策来源**：统一口径基准 D5 / PRD §6.3「10 端权威码」。`docs/integration-capability-switches-v0.1.md`。

**数据表**：`terminal_capability_switches`（V111）：先 UPDATE 漂移别名（customer_app→app、customer_miniapp→miniapp、web_admin→web、staff_personal_app→staff_private_phone、merchant_web→merchant_portal、staff_tablet→tablet），再换 `chk_tcs_terminal_type` = D5 10 端权威码 ∪ {'*'}：`app/miniapp/web/tablet/staff_work_phone/staff_private_phone/merchant_portal/merchant_app/kiosk/mobile_admin/*`。

**API/operationId**（grep openapi）：
- `listCapabilitySwitches` GET `/api/v1/terminals/capability-switches`（perm `terminal.switch.view`）
- `updateCapabilitySwitch` PUT `/api/v1/terminals/capability-switches/{id}`（perm `terminal.switch.manage`）

**事件**（grep 真实常量）：`terminal.switch.*`（查询/变更，详见 TerminalController）；拒绝留痕随被关闭能力的 GuardedAction 落审计（**待确认**独立“拒绝事件”常量名）。

**权限点**：`terminal.switch.view`、`terminal.switch.manage`。

**闭环**：能力开关按 D5 权威码作用域；运行时写入漂移别名经 CHECK 收敛；能力关闭时拒绝并留痕。

**G/W/T 验收**：
- G: 运行时尝试以 `customer_app` 写入 terminal_type。T: 服务层/CHECK 收敛为 `app`（或拒绝非权威别名）。
- G: 某终端某能力开关置 off。W: 该终端发起该能力动作。T: 被拒绝并留痕（GuardedAction 拒绝路径）。
- G: 传入 D5 之外的 terminal_type。T: 被 `chk_tcs_terminal_type` 拒绝。

---

## 15. 工单 SLA 自动升级（V120）

**需求/决策来源**：审计④ P2（V022 仅静态 due_at，无 sla_breached 标记与升级留痕）。

**数据表**：`merchant_service_work_orders`（V120）：补 `sla_breached`(默认 false)、`escalated_at`；partial 索引 `idx_merchant_service_work_orders_sla_due`(due_at 已过、未关闭/取消、未升级)。`merchant_service_work_order_actions`（V120）：`action_type` CHECK 增 `escalated`。

**API/operationId**：无独立端点。由定时任务 `MerchantServiceService.escalateOverdueWorkOrders`（开关 `merchant.service.sla-escalation.scheduled.enabled=true` 才注册）。

**事件**（grep 真实常量）：`merchant_service.work_order.escalated`。

**状态流转**：升级**不改 status**，仅打标 `sla_breached=true` + `escalated_at` + 写一条 `escalated` 动作留痕。

**权限点**：定时任务系统态执行（无交互权限）。

**闭环**：SLA 超时自动升级 + 留痕，命中后 escalated_at 置非空自动移出 partial 索引（防重复升级）。

**G/W/T 验收**：
- G: 一张工单 due_at 已过、status 非 resolved/closed/cancelled、escalated_at 为空。W: SLA 扫描任务运行。T: 打标 sla_breached=true + escalated_at，写 escalated 动作流水，落 `merchant_service.work_order.escalated`；status 不变。
- G: 已升级工单。W: 任务再次扫描。T: 已被 partial 索引排除，不重复升级。
- G: 开关未启用。W: 部署。T: 任务不注册（不自动升级）。

---

## 16. 检修供应商 FK（V116 + V123）

**需求/决策来源**：审计④ P2 / amuse-3 / 设计 §6.6.1（maintenance_vendor_id 原软引用、悬空风险）。

**数据表**：`amusement_maintenance_record.maintenance_vendor_id` → `inventory_suppliers(id)`。V116 加外键 `fk_amusement_maint_vendor` **NOT VALID**（仅约束新增/更新行）；V123 清洗存量悬空（回挂占位供应商 `__legacy_orphan_vendor__`，status=archived 业务侧不可选，因列 NOT NULL 不可置空、不删记录保留审计）后 **VALIDATE CONSTRAINT** 收紧。

**API/operationId**：无独立端点（数据完整性约束）。Service 层 `AmusementMaintenanceService.requireExistingVendor`（存在 + status=active 近似把关）。

**事件**：n/a（DB 约束）。

**权限点**：随检修单维护 capability。

**设备/供应商闭环**：检修单必须挂真实存在供应商（FK 存在性 + 服务层 active 校验）。

**G/W/T 验收**：
- G: V123 已执行。W: 新建检修单引用不存在的 maintenance_vendor_id。T: 被 `fk_amusement_maint_vendor` 拒绝。
- G: 引用 status=archived 的占位供应商。W: 业务侧选择检修单位。T: requireExistingVendor 不放行（仅 active）。
- G: 存量悬空检修记录。W: V123 迁移。T: 回挂占位供应商，记录保留，VALIDATE 通过（不删记录、不置空）。

---

## 17. void 驳回反向冲销（V117 + V124）

**需求/决策来源**：R6-D14（退菜退酒升 P0，强复核+留痕）；延期项④（驳回须对称回滚已施加副作用）。

**数据表**：`bar_bill_adjustments`（V124）：`chk_bar_bill_adjustment_type` 扩为五种 ∪ {**waiver_reversal, refund_reversal**}；放宽 `chk_bar_bill_adjustment_refunded_at`——refund/refund_reversal 必带 refunded_at（落经营日窗口），其余类型不得带。

**API/operationId**（grep openapi）：
- `voidBarOrderItem` POST `/api/v1/bar/orders/{order_id}/void-item`（perm `bar.order_item.void`）
- `decideBarOrderItemVoid` POST `/api/v1/bar/order-item-voids/{void_id}/decision`（perm `bar.order_item.void`）

**事件**（grep 真实常量）：`bar.order_item_voided`（发起 void）、`bar.order_item_void_decided`（审批决策，及 `_decide_failed`）。冲销类型 `bar_order_item_void_reject`、`waiver_reversal`、`refund_reversal`（`BarOrderService` lines 787/835）。

**状态流转**：voidOrderItem 时同步施加副作用（库存回补、账单减项 waiver、已结退款过账 refund），流水落 pending → 审批 rejected = void 无效 → 对称回滚：
- 未结账单原减项 waiver → 反向 `waiver_reversal`（加回应付，refunded_at NULL）。
- 已结账单原减项 refund → 反向 `refund_reversal`（加回应付 + 对冲退款应收，带 refunded_at；经营日净额归集使 refund_cent 净额归零，不重复计退款）。

**权限点**：`bar.order_item.void`（发起 + 决策）。

**资金/库存闭环**：驳回时把已施加副作用对称回滚、恢复 void 前状态；refund_reversal 经营日窗口净额 `refund − refund_reversal` 使净额归零（`JdbcBarBillingRepository` line 283）。

**G/W/T 验收**：
- G: 未结账单的退酒 void 已施加 waiver 减项、流水 pending。W: decideBarOrderItemVoid=rejected。T: 写一条 `waiver_reversal` 加回应付（refunded_at NULL），库存回补对称回滚，落 `bar.order_item_void_decided`。
- G: 已结账单的退酒 void 已过账 refund。W: 驳回。T: 写 `refund_reversal`（加回应付 + 带 refunded_at），经营日 refund 净额归零，不重复计入退款额。
- G: refund_reversal 缺 refunded_at。T: 被 `chk_bar_bill_adjustment_refunded_at` 拒绝。

---

## 18. 集团多项目视图 group-overview（V115）

**需求/决策来源**：R6-D13（商户级 revenue.net 权威源 V075/V078）；集团总览（R6 跨项目汇总）。不新建快照表，复用 V037 `analytics_business_metric_snapshots` 平台口径 ETL。

**数据表**：`analytics_business_metric_snapshots`（既有，V070 唯一键含 segment_type，segment_id=project_id 独立成行）。`DashboardAggregationJob` 末尾按 project_id 维 upsert `segment_type='project'` 行（与 platform/merchant 并存不互破）。V115 仅补权限。

**API/operationId**（grep openapi）：`getGroupOverview` GET `/api/v1/analytics/group-overview`（perm `analytics.group.view`）。返回 metric_key → actual_value（segment_type='platform' 口径 rollup）。

**事件**：n/a（ETL job 产出快照）。

**权限点**（V115 seed）：`analytics.group.view`(analytics, high)——跨商户聚合，仅平台/集团管理员；与 `analytics.dashboard.view`（看板下钻，可下放商户）区分。

**闭环**：DashboardAggregationJob 同步产出 segment_type='project' 行；集团总览只读跨项目聚合。

**G/W/T 验收**：
- G: DashboardAggregationJob 已运行。W: getGroupOverview。T: 返回 segment_type='project'/'platform' 口径的 metric 合计；商户角色无 `analytics.group.view` 被拒。
- G: 当前单综合体（单 project）。W: 查询。T: project 维 rollup = 平台口径合计（不破坏 platform/merchant 行）。

---

## 19. 多综合体 project_id scoped 通道（listXxxByProject）

**需求/决策来源**：多综合体隔离（§24 商户隔离 / §30 数据权限）。无新迁移，为仓储层按 project_id 维度查询通道。

**数据表**：无新表。涉及既有多表的 `project_id` 列。grep 命中仓储方法（**实证 15 个 `listXxxByProject`**，分布 8 个 JdbcXxxRepository）：
`listBirthdayRulesByProject`、`listCheckInTiersByProject`、`listDashboardSnapshotsByProject`、`listMemberLevelsByProject`、`listMembershipPlansByProject`、`listMembershipsByProject`、`listPointsRulesByProject`、`listPointsTransferLimitsByProject`、`listProcessInstancesByProject`、`listProfilesByProject`、`listRedemptionCatalogItemsByProject`、`listShiftsByProject`、`listStoredValueAccountsByProject`、`listSupplyContractsByProject`、`listWarehousesByProject`。
> 实证修正：任务清单称“18 表 listXxxByProject”，**实际 grep 命中 15 个** `listXxxByProject` 方法（分布于 access/staff、analytics、bar(×2)、customer、evidence(接口+实现)、inventory 等仓储）。若期望 18，差额 3 项**待确认**（可能为命名不同的 project-scoped 查询，或尚未补齐）。

**API/operationId**：随各域列表端点透传 project 维度参数（未单列独立 operationId 集合）。

**事件**：n/a（查询通道）。

**权限点**：随各域查询 capability + project 维度隔离校验。

**隔离闭环**：跨项目查询按 project_id scoped，配合 verify:tenant-isolation gate（R6-D12 fail-closed）防越权。

**G/W/T 验收**：
- G: 多 project 数据并存。W: 以 project A 维度调 listWarehousesByProject(A)。T: 仅返回 project A 的仓库，不泄漏 project B。
- G: 用户无 project A 数据权限。W: 查询 project A scoped 通道。T: 被隔离校验拒绝（verify:tenant-isolation fail-closed）。

---

## 与现有文档的关系

本文为 **V099–V124 新功能的权威开发与验收承接锚点**。旧文档与本文的关系如下，旧文后续在对应处回填指向本文：

| 旧文档 | 与本文关系 |
|--------|-----------|
| `docs/business-decision-log-v0.1.md` §36（R6-D01..D14） | **决策权威源**。本文每功能“需求/决策来源”指向对应 R6 编号；本文为决策的可开发/可验收落地。 |
| `docs/evidence-risk-layer-spec-v0.2.md` §8 | 功能 6（统一异常责任链）规格在彼，本文补 operationId/事件/CHECK 实证与 G/W/T；彼处回填指向本文功能 6。 |
| `docs/finance-ar-ap-ledger-development-design-v0.1.md` / `finance-settlement-product-prd-v0.1.md` | 功能 2/8/11/13 财务侧设计；本文补迁移号/operationId/source_type/refund_direction 实证。 |
| `docs/bar-business-operations-product-prd-v0.1.md` / `bar-operations-development-design.md` | 功能 3/5/17（订台定金、pour cost、void 冲销）；本文补 V117/V124 CHECK 与冲销事件实证。 |
| `docs/amusement-*` PRD/dev-design | 功能 7/16（超容、检修 FK）；本文补 V104/V116/V123 实证。 |
| `docs/android-device-terminal-development-design.md` / `mdm-android-enterprise-implementation-plan-v0.1.md` | 功能 10/14（设备生命周期、capability）；本文补 V107/V111 命名映射（以代码为准）。 |
| `docs/data-risk-ai-development-design.md` §11.4A | 功能 9（企微违规命中）；本文补 V106 规则码 seed 与两域 listWecomRiskHits 实证。 |
| `docs/integration-capability-switches-v0.1.md` | 功能 14（D5 10 端权威码）。 |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 功能 19（project scoped）+ R6-D12 verify:tenant-isolation。 |
| `docs/openapi/openapi-p0.json`（386 op） | **契约权威源**。本文 operationId/path/`x-permission` 均引自此；契约变更以此为准。 |
| 各 PRD / requirement-traceability-matrix / ui-acceptance-matrix | 后续回填一列指向本文对应功能小节，作为新功能的开发承接与 QA 用例锚点。 |

### 实证待确认项（汇总）
- 功能 3：CustomerService 读 bar_deposit_rules 后是否发独立 `bar.reservation.deposit_paid` 事件——grep 未命中独立常量，**待确认**。
- 功能 14：能力开关关闭时的独立“拒绝事件”常量名——未单独命中，疑随 GuardedAction 拒绝路径，**待确认**。
- 功能 19：`listXxxByProject` 实际 grep 命中 **15** 个（非任务清单所述 18），差额 3 项**待确认**。
