# C4 暂存改动 P1 质量缺口 可编码修复开发规格 V0.1（2026-06-18）

> **任务**：R6 批次 **C4=出规格交研发**（业务确认）。承接本批暂存改动审计出的 P1 质量缺口，逐项给出可编码修复规格。
> **范围裁决**：本文档只出规格，不改代码、不跑测试/构建。三组缺口：①本批新功能测试覆盖缺口（动态定价 trace / 商户工单 / 督导复核）；②integration `testProviderAdapterMock` 占位即通过 + webhook 无 mapPayload；③inventory 高价值注销文案与实现不一致。
> **优先级**：全部 **P1**。
> **现状核验基准**（已逐项 Grep/Read 核实，见各节"现状证据"）：
> - V041 已加 `ticket_orders.dynamic_pricing_rule_id / base_amount_cent / dynamic_adjustment_cent`；`TicketOrderService.createOrder` 已落库三列。
> - `MerchantServiceService` 5 op（list/create/assign/addAction/close）、`SupervisorReviewService` 3 op（listPolicies/evaluate/approve）均已有暂存测试文件，但覆盖存在断言缺口（见 §1）。
> - `ProviderAdapterService.testProviderAdapterMockOnce` 无条件写 `"passed"`（第 269 行）。
> - `IntegrationService.receiveWebhook` 仅落信封（status=accepted），无 mapPayload。
> - `InventoryService.cancelSavedBottle` 高价值分支文案声称 "supervisor review" 但实现仅校验 `evidenceAttachmentIds().size() < 2`（第 339–341 行）。
>
> **重要关联（前置阅读）**：inventory 高价值复核的领域归属在 **r5-spec-T6** 已裁决——**库存域内自建** `inventory_high_value_reviews` 表 + `InventoryHighValueReviewService`，**不走 governance 统一 `supervisor_review` 队列**。本文档 §3 据此对齐，不另起复核方案。

---

## 目录

- [1. 动态定价 trace / 商户工单 / 督导复核 测试补强（P1）](#1-动态定价-trace--商户工单--督导复核-测试补强p1)
  - [1.1 动态定价 trace 落库测试](#11-动态定价-trace-落库测试)
  - [1.2 商户工单 5 op 契约/单元测试](#12-商户工单-5-op-契约单元测试)
  - [1.3 督导复核 3 op 单元测试](#13-督导复核-3-op-单元测试)
- [2. integration mock 真实判定 + webhook mapPayload 最小落地（P1）](#2-integration-mock-真实判定--webhook-mappayload-最小落地p1)
  - [2.1 testProviderAdapterMock 按 scenario/mockPayload 判定 passed/failed](#21-testprovideradaptermock-按-scenariomockpayload-判定-passedfailed)
  - [2.2 webhook mapPayload 最小落地](#22-webhook-mappayload-最小落地)
- [3. inventory 高价值文案与实现对齐（P1）](#3-inventory-高价值文案与实现对齐p1)
- [4. 与 R5 backlog 的关系](#4-与-r5-backlog-的关系)
- [5. 验收总览与排期建议](#5-验收总览与排期建议)
- [6. 变更记录](#6-变更记录)

---

## 1. 动态定价 trace / 商户工单 / 督导复核 测试补强（P1）

> **审计原始命题修正（重要）**：本批暂存改动 **并非"零测试"**。经核验，`MerchantServiceServiceTests`（含 create/assign/addAction/close/幂等/隔离/状态机等 ≥20 个 `@Test`）、`SupervisorReviewServiceTests`（14 个 `@Test`）、`TicketingServiceTests`（含 `publishedDynamicPricingRuleChangesFormalOrderAmount` 等动态定价用例）均已存在且为本批暂存/修改文件。
> 真实缺口是 **断言深度不足**：动态定价 trace 的**三列落库分解**未被任何用例断言；工单/督导虽有用例，但缺少若干边界/审计/契约维度。本节据此给出"补强"清单，而非"从零新建"。

### 1.1 动态定价 trace 落库测试

**现状证据**
- 迁移：`backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql`（新增 `dynamic_pricing_rule_id` / `base_amount_cent` / `dynamic_adjustment_cent`，并回填 `base_amount_cent = amount_cent`）。
- 计价落库：`backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java` `createOrder`（第 36–85 行）：
  - `baseAmount = unitPrice * quantity`；`dynamicAdjustment = dynamicPrice.adjustmentCent() * quantity`；`finalAmount = dynamicPrice.finalUnitPriceCent() * quantity`；落库 `dynamicPrice.ruleId()` / `baseAmount` / `dynamicAdjustment`（第 75–77 行）。
- 测试缺口：`TicketingServiceTests.createTicketOrderComputesAmountAndRequiresPositiveQuantity` 仅断言 `order.amountCent()`；`publishedDynamicPricingRuleChangesFormalOrderAmount`（第 798 行）断言最终金额变化，但 **未断言** `baseAmountCent` / `dynamicAdjustmentCent` / `dynamicPricingRuleId` 三列分解。即"最终价"被覆盖，"可审计的计价 trace 分解"无测试。

**目标**
确保动态定价生效与不生效两条路径下，`ticket_orders` 的计价 trace 三列（base / adjustment / ruleId）落库正确，且满足恒等式 `base + adjustment == final(amountCent)`，并随 quantity 线性放大。

**改法要点（测试代码，不改业务逻辑）**
- 在 `TicketingServiceTests` 补用例（命名见验收口径），复用既有 `service` / fake repository 装配。
- 至少覆盖：发布的降价规则、加价规则、无匹配规则（adjustment=0、ruleId=null）三场景。
- 断言读取 `TicketOrderRecord` 的三列访问器（`baseAmountCent()` / `dynamicAdjustmentCent()` / `dynamicPricingRuleId()`）。
- 若 JDBC 集成层需覆盖，复用 `TicketingJdbcIntegrationTests`，断言数据库回读三列与服务层一致（防 RowMapper 漏列）。

**验收口径（测试用例描述）**
1. `createTicketOrderRecordsDynamicPricingTraceWhenRuleApplies`：发布一条降价规则并匹配 → 单价被调低；断言 `baseAmountCent == unitPrice*qty`、`dynamicAdjustmentCent < 0`、`baseAmountCent + dynamicAdjustmentCent == amountCent`、`dynamicPricingRuleId == 已发布规则 id`。
2. `createTicketOrderRecordsZeroTraceWhenNoDynamicRule`：无匹配规则 → `dynamicAdjustmentCent == 0`、`baseAmountCent == amountCent`、`dynamicPricingRuleId == null`。
3. `dynamicPricingTraceScalesWithQuantity`：qty=3 时三列均为单份 ×3，恒等式成立。
4. （集成）`ticketOrderDynamicTraceColumnsRoundTripThroughJdbc`：写入后从库回读三列与服务返回一致。

---

### 1.2 商户工单 5 op 契约/单元测试

**现状证据**
- 服务：`backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceService.java`，5 op：`listWorkOrders`（第 61 行）、`createWorkOrder`（77）、`assignWorkOrder`（92）、`addAction`（110）、`closeWorkOrder`（128）；状态机白名单 `created→assigned→{processing,pending_merchant,resolved}`，终态 `closed/cancelled`。
- 已有测试：`MerchantServiceServiceTests`（含状态机非法跳步/回退、幂等同请求/异请求冲突、商户 scope 隔离、安全上下文取 reportedBy/operator 等）、`MerchantServiceControllerTests`、`MerchantServiceJdbcIntegrationTests`。
- 缺口：①`closeWorkOrder` 仅有"需 resolution + 合法满意度分"与"成功落 resolution"，缺**从非 resolved 态直接 close 的非法路径**断言；②`addAction` 缺**评论(comment)始终允许且不推进状态**的显式断言；③控制器层 5 op 的**权限点/HTTP 状态码契约**未逐 op 锁定。

**目标**
将工单 5 op 的状态机、幂等、隔离、审计、权限契约补齐至"每 op 至少一条成功路径 + 一条主失败路径 + 一条契约（权限/审计）"。

**改法要点（测试代码）**
- 单元层在 `MerchantServiceServiceTests` 补失败/边界用例；契约层在 `MerchantServiceControllerTests` 用 MockMvc 断言权限点与状态码。
- 复用既有 fake repository / `authorizationService` stub 模式（参照同文件既有 setUp）。

**验收口径（测试用例描述）**
1. `closeWorkOrderFromNonResolvedStatusThrowsInvalidState`：created/assigned 态直接 close → `INVALID_STATE` 冲突。
2. `addCommentActionAllowedInAnyNonTerminalStatusWithoutStatusChange`：comment 类 action 在 assigned/processing 态均允许且状态不变。
3. `eachWorkOrderOpEmitsSuccessAuditEventWithExpectedEventName`：5 op 成功路径分别落 `merchant_service.work_order.{created,assigned,...}` 审计事件。
4. （契约）`workOrderEndpointsRequireServiceWorkOrderPermissions`：5 op 控制器分别要求 `merchant.service_work_order.{create,assign,...}` 权限；无权返回 403。
5. （契约）`closeWorkOrderRejectsInvalidSatisfactionScoreReturns400`：满意度分越界 → 400 + `VALIDATION_FAILED`。

---

### 1.3 督导复核 3 op 单元测试

**现状证据**
- 服务：`backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java`，3 op：`listPolicies`（第 49 行）、`evaluate`（62）、`approve`（123）。
- 已有测试：`SupervisorReviewServiceTests`（14 `@Test`）、`SupervisorReviewControllerTests`，以及 `ApprovalLevelTests`（未跟踪新文件）。
- 缺口：需确认 `evaluate` 命中/未命中策略两分支、`approve` 幂等与重复审批拒绝、`approve` 跨租户/越权拒绝、决策记录(`SupervisorReviewDecisionRecord`)字段落库均有断言；以及审计事件名锁定。

**目标**
督导复核 3 op 的"命中判定 / 审批闭环 / 幂等与越权"三类不变量被显式断言。

**改法要点（测试代码）**
- 在 `SupervisorReviewServiceTests` 校验既有覆盖并补差额用例；控制器层补权限契约。

**验收口径（测试用例描述）**
1. `evaluateReturnsReviewRequiredWhenPolicyMatches` / `evaluateReturnsAutoPassWhenNoPolicyMatches`：命中与未命中两分支决策结果与 `SupervisorReviewDecisionRecord` 字段断言。
2. `approveIsIdempotentForSameIdempotencyKey` / `approveSecondTimeOnDecidedReviewThrowsConflict`：幂等返回首结果；对已决策项重复审批拒绝。
3. `approveCrossTenantReviewThrowsForbidden`：越权审批被拒。
4. （契约）`supervisorReviewEndpointsRequireReviewPermission`：3 op 权限点与 403 契约。

---

## 2. integration mock 真实判定 + webhook mapPayload 最小落地（P1）

### 2.1 testProviderAdapterMock 按 scenario/mockPayload 判定 passed/failed

**现状证据**
- `backend/src/main/java/com/complex/platform/integration/ProviderAdapterService.java` `testProviderAdapterMockOnce`（第 256–282 行）：除"providerCode 不匹配或 status=disabled 抛 `PROVIDER_ADAPTER_NOT_TESTABLE`"外，**无条件**构造 `AdapterTestRecord(..., "passed", ...)`（第 269 行硬编码 `"passed"`）。`request.scenario()` 与 `request.mockPayload()` 仅原样回显进 `result`，不参与判定。
- 测试侧印证：`IntegrationServiceTests` 第 334 行 `assertThat(result.testStatus()).isEqualTo("passed")` —— 不存在任何 `failed` 用例，因为实现永远 passed。
- 文档定位：`docs/third-party-integration-development-design.md` §14（适配器配置、`status_mapping`、`mock_enabled`）确立 mock 仅用于联调占位；mock 测试应能模拟失败场景以验证错误码映射占位。

**目标**
让 mock 测试成为"可判定"的最小桩：依据 `scenario` 与 `mockPayload` 决定 `passed` / `failed`，并在 `failed` 时回填失败原因/错误码占位，使前端联调能演练失败分支，且为后续真实适配器留下判定接缝。

**改法要点（业务代码 + 测试）**
- 定义最小判定规则（无需引外部依赖）：
  - 优先级 1：`mockPayload` 内显式期望，如 `mockPayload.expect == "failed"`（或 `mockPayload.force_status`）→ 直接采用该状态；
  - 优先级 2：`scenario` 语义约定——以 `success`/`paid`/`ok` 等开头判 `passed`；以 `failure`/`failed`/`error`/`timeout`/`reject` 等开头判 `failed`；
  - 兜底：未识别 scenario 默认 `passed`（保持向后兼容，避免误判既有联调脚本）。
- `failed` 时：`AdapterTestRecord.result` 写入结构化失败信息占位（如 `{"scenario":..,"adapter_id":..,"reason_code":"MOCK_SCENARIO_FAILURE","mock_expect":..}`）；`testStatus="failed"`。审计 `support.recordSuccess(..., "status", status)` 已带 status，保持不变即可（测试动作本身成功，业务判定为 failed）。
- 将判定逻辑收敛为一个私有纯函数（如 `resolveMockTestStatus(scenario, mockPayload)`）便于单测。
- 不改 `PROVIDER_ADAPTER_NOT_TESTABLE` 前置校验。

**验收口径（测试用例描述）**
1. `mockTestReturnsFailedWhenScenarioIndicatesFailure`：scenario=`failure_timeout` → `testStatus=="failed"`，`result` 含 `reason_code`。
2. `mockTestReturnsPassedWhenScenarioIndicatesSuccess`：scenario=`success_order_paid` → `passed`。
3. `mockTestRespectsExplicitMockPayloadForceStatus`：`mockPayload.expect="failed"` 覆盖 scenario → `failed`。
4. `mockTestDefaultsToPassedForUnknownScenario`：未知 scenario → `passed`（向后兼容）。
5. `mockTestStillRejectsNonTestableAdapter`：disabled / providerCode 不匹配仍抛 `PROVIDER_ADAPTER_NOT_TESTABLE`（回归既有第 260–262 行）。
6. （回归修订）将 `IntegrationServiceTests` 第 334 行既有用例的 scenario 显式设为 success 语义，避免"默认 passed"掩盖回归。

---

### 2.2 webhook mapPayload 最小落地

**现状证据**
- `backend/src/main/java/com/complex/platform/integration/IntegrationService.java` `receiveWebhook`（第 137–203 行）：校验端点/启用/签名后，幂等去重，落 `OpenApiEventRecord`，`processStatus="accepted"`，`payload` 原样存信封，`mapping`/解析结果字段（构造第 175 行 `null`）为空。即"到达=被动留痕"，无字段映射、无标准事件产出。
- 文档定位：`docs/third-party-integration-development-design.md` 顶部 R5 横幅明示"webhook 入站仅落信封(status=accepted)，无 mapPayload"；§13 事件映射与 §14.3 `integration_endpoint_mappings(request_mapping/response_mapping/status_mapping)` 已为映射预留表结构与字段。**业务裁决（2026-06-18）**：第三方一律"预留接口 + 后台开关，本期只做接口/开关/字段映射占位，真实回流/对账待厂商资料与开关开启后交付"。

**目标（受裁决约束的"最小落地"，非完整对账）**
在不破坏"开关未开即只留信封"原则下，提供一个**可被开关启用**的 `mapPayload` 接缝：当端点/适配器配置了 `request_mapping`（或 `status_mapping`）且映射开关开启时，对入站 payload 做一次声明式字段映射，产出标准事件投影写入事件记录的 `mapping` 字段（替换当前的 `null`），并把 `processStatus` 由 `accepted` 细化为 `mapped`（映射成功）/ `accepted`（无映射配置，维持现状）/ `map_failed`（映射异常，落错误占位、不抛断业务）。

**改法要点（业务代码 + 测试）**
- 新增私有方法 `Map<String,Object> mapPayload(endpoint/adapter mapping, payload)`：按 `request_mapping`（源字段路径 → 标准字段名）做浅层取值映射；`status_mapping` 将供应商状态码映射为平台标准状态；无配置则返回空映射且不改 `accepted` 行为。
- 调用点：`receiveWebhook` 在保存 `OpenApiEventRecord` 前调用，结果写入 `mapping`（当前第 174–175 行处的 `Map.of("accepted",..)` 与 `null`）。
- 开关：复用既有适配器/端点开关字段（如 webhook 映射启用位）；开关关闭一律走 `accepted`，确保裁决"不阻塞其它功能"。
- `map_failed` 不抛异常中断 webhook 接收（保证幂等去重与签名校验语义不变），仅记录失败占位 + 审计 detail 带 `mapped=false`。
- 不实现对账差异/自动回写（明确超出本期裁决范围，列入 backlog）。

**验收口径（测试用例描述）**
1. `webhookWithoutMappingConfigStaysAcceptedEnvelopeOnly`：无映射配置 → `processStatus=="accepted"`，`mapping` 为信封占位（回归现状）。
2. `webhookWithMappingConfigProducesStandardEventProjection`：配置 `request_mapping` + 开关开 → `processStatus=="mapped"`，`mapping` 含映射后的标准字段。
3. `webhookStatusMappingTranslatesProviderStatusToPlatformStatus`：`status_mapping` 将供应商状态映射为平台标准状态。
4. `webhookMappingFailureRecordsMapFailedWithoutBreakingIngest`：映射异常 → `processStatus=="map_failed"`，事件仍落库、幂等去重仍生效、不抛 5xx。
5. `webhookMappingDisabledBySwitchFallsBackToAccepted`：开关关闭即便有配置也走 `accepted`（裁决兜底）。

---

## 3. inventory 高价值文案与实现对齐（P1）

**现状证据**
- `backend/src/main/java/com/complex/platform/inventory/InventoryService.java` `cancelSavedBottle`（第 314–347 行）：
  - 注释（第 314 行）："高价值存酒必须提供照片+视频双证据（>=2 附件）方可作废（**主管复核闭环为后续**）"。
  - 实现（第 339–341 行）：`if (savedBottle.isHighValue() && safeList(actual.evidenceAttachmentIds()).size() < 2) throw HIGH_VALUE_EVIDENCE_REQUIRED(... "and supervisor review")`。
  - 即：错误文案声称 "supervisor review"，但实现**仅校验附件数 ≥ 2**，未联动任何复核。
- 文档定位：`docs/supply-chain-inventory-development-design-v0.1.md` 第 10 行 R5 横幅自认"`isHighValue` 仅校验附件数≥2，`SupervisorReviewService` 未被 inventory 调用，代码注释自认'复核闭环为后续'"。
- **领域归属已裁决（前置）**：`docs/r5-spec-T6-supply-chain-dev-design-20260618.md`（第 4、94、98、101、104–105 行）明确——高价值报损/盘点差异/**存酒作废**复核 = **库存域内自建** `inventory_high_value_reviews` 表 + `InventoryHighValueReviewService`，**不走 governance 统一 `supervisor_review` 队列**；流程 `pending → approved（动作生效/扣减/作废落账）/ rejected（回滚）`。

**目标**
消除"文案声称复核、实现仅校验附件"的不一致。给出两条可选路径，并明确推荐项与边界。

**两条路径与裁决**
- **路径 A（推荐，最终态，归属 T6）**：接入 T6 的库存域内 `inventory_high_value_reviews` 复核闭环——`cancelSavedBottle` 命中 `isHighValue` 时，不直接置 `cancelled`，而是建 `inventory_high_value_reviews(subject_type=存酒作废, pending)` 并要求照片+视频双证据；主管 `approve` 后才作废落账，`reject` 回滚。**此为 T6 §既定规格范围**——本文档不重复其表结构/服务设计，仅声明"C4 item③ 的最终修复由 T6 交付"。
- **路径 B（过渡，仅当 T6 未排进同一里程碑时）**：把文案改为与实现一致——错误码 `HIGH_VALUE_EVIDENCE_REQUIRED`，message 去掉 "and supervisor review"，改为"高价值存酒作废需照片+视频双证据（附件数≥2）；主管复核见 T6 库存域内复核"。同步修订第 314 行注释，避免"声称复核"误导。

**改法要点**
- 若走路径 A：在 T6 实施时一并改造 `cancelSavedBottle`，本文档不另行出代码规格（防止与 T6 冲突）。
- 若走路径 B：仅改文案/注释（业务行为不变），并在 message/注释中**指明 T6 已定域内自建复核**为最终态，保留升级路径。
- 二者择一，避免"附件校验 + 文案改 + 复核接入"三事并做造成行为漂移。

**验收口径（测试用例描述）**
- 路径 A（由 T6 用例覆盖，本文档引用不重复）：`highValueSavedBottleVoidRequiresApprovedReviewBeforeCancelled`、`rejectedReviewKeepsSavedBottleStored`。
- 路径 B（过渡）：
  1. `highValueVoidWithFewerThanTwoAttachmentsThrowsWithAttachmentWording`：附件<2 → `HIGH_VALUE_EVIDENCE_REQUIRED`，message 含"附件数≥2"且**不含**"supervisor review"。
  2. `highValueVoidWithTwoAttachmentsSucceeds`：附件=2 → 作废成功（行为不变，回归保护）。
  3. `nonHighValueVoidIgnoresAttachmentRule`：非高价值无附件可作废（回归保护）。

---

## 4. 与 R5 backlog 的关系

| C4 缺口项 | 与 R5 backlog 关系 | 处置 |
|---|---|---|
| §1.1 动态定价 trace 测试 | V041 + `createOrder` 计价落库属本批已落地功能（非 R5 backlog Txx 新建项），仅测试断言深度不足 | C4 内独立补强，不依赖 Txx |
| §1.2 商户工单测试 | 工单 5 op 为本批已落地功能 | C4 内补强 |
| §1.3 督导复核测试 | 督导复核 3 op 为本批已落地功能 | C4 内补强 |
| §2.1 mock 真实判定 | 对应 `r1-third-party-capability-switch-and-mock-strategy` 与第三方集成设计 §14；裁决"本期只做接口/开关/映射占位"——mock 判定属占位质量提升，**不触碰真实回流** | C4 内独立实现 |
| §2.2 webhook mapPayload | 第三方集成设计 R5 横幅明列缺口；裁决限定"映射占位、不做对账"——本节即该占位的最小落地 | C4 内最小落地；对账/回写仍在 backlog |
| §3 inventory 高价值复核 | **直接归属 r5-spec-T6**（已裁决库存域内自建复核） | **路径 A 由 T6 交付**；T6 未同里程碑时，C4 内走路径 B 过渡文案对齐 |

---

## 5. 验收总览与排期建议

| 项 | 优先级 | 改动类型 | 主要文件 | 建议批次 |
|---|---|---|---|---|
| §1.1 动态定价 trace 测试 | P1 | 仅测试 | `TicketingServiceTests` / `TicketingJdbcIntegrationTests` | 可立即并行 |
| §1.2 工单测试补强 | P1 | 仅测试 | `MerchantServiceServiceTests` / `MerchantServiceControllerTests` | 可立即并行 |
| §1.3 督导复核测试补强 | P1 | 仅测试 | `SupervisorReviewServiceTests` / `SupervisorReviewControllerTests` | 可立即并行 |
| §2.1 mock 判定 | P1 | 业务+测试 | `ProviderAdapterService` / `IntegrationServiceTests` | 独立小改 |
| §2.2 webhook mapPayload | P1 | 业务+测试 | `IntegrationService` / `IntegrationServiceTests` | 与 §2.1 同人较优 |
| §3 高价值文案对齐 | P1 | 路径 A 随 T6 / 路径 B 仅文案 | `InventoryService`（+T6 域内复核） | 跟随 T6 决策 |

**统一约束**（沿用 r5-spec 体例）：金额=整数分（D1）；事件名点+下划线、与平台事件字典一致（D4）；新增枚举走 CHECK 中英对应（D7）；隔离列 `project_id`/`merchant_id`（涉库改动时）；任何迁移以合并顺序取下一连续 Flyway 版本，并核 `MigrationVersions.latest()`。本批 §1/§2 不引入新迁移；§3 路径 A 的迁移由 T6 承载。

---

## 6. 变更记录

| 日期 | 变更 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 C4 暂存改动 P1 质量缺口修复规格（动态定价 trace/工单/督导测试补强 + integration mock 真实判定 + webhook mapPayload 最小落地 + inventory 高价值文案对齐）。修正"零测试"命题为"断言深度不足"。inventory 复核归属指向 r5-spec-T6 库存域内自建。 | C4 业务确认；现状取自 V041、`ProviderAdapterService`/`IntegrationService`/`InventoryService` 核验；裁决取自第三方集成设计 R5 横幅 + r5-spec-T6 |
