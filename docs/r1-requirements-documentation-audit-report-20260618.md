# 第一轮 需求文档审计报告 R1（2026-06-18）

> 审计对象：`/home/james/complex-management`（综合体管理系统）当前工作区全量需求/产品/开发设计/接口/数据库迁移/验收/Runbook/生产证据文档。
> 审计方法：documentation-specialist（审计工作流）+ pm-utility-pm-critic 反向质疑 + deliver-acceptance-criteria / deliver-edge-cases（验收与边界）+ dean-user-story-mapping / deliver-user-stories（角色路径）+ gstack-spec / gstack-review（可开发规格与对抗式复核）。
> 边界：**只读代码、不改代码**；只修正/新增文档，不删除既有文档。
> 机器标记：R1_REQ_DOC_AUDIT_READY、INDEPENDENTLY_VERIFIED_AGAINST_CODE_20260618、SUPERSEDES_NOTHING（与既有审计并存、互补，不取代）。

## 0. 一句话结论

本项目文档体系**异常成熟**（已建立 S0 入口、统一基线 D1–D13、需求追溯矩阵、OpenAPI 自动操作矩阵、能力开关矩阵、partial mock 覆盖、生产阻断台账、数据隔离自动基线）。本轮不再引用会漂移的静态数量作为当前事实；当前数量以 `npm run verify:*`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 和 `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` §3A 自动区块为准。本轮做三件事：①对抗式独立验证既有审计与"已修"声明是否与代码现实一致；②对用户点名的业务闭环做 file:line 级矛盾下钻；③把不可开发/不可验收/相互矛盾的内容补成可开发规格并逐项关闭。

可否进入下一阶段（结论与既有审计一致）：自有系统开发 / 内网联调 / 产品测试验收 **可以**；完整后台、真实供应商联调 **条件可以**；公开生产上线 **不可以**（外部证据未关闭）。

## 1. 本轮读取的文档（清单见报告末 §8）

入口/治理类、统一基线、需求追溯矩阵、需求-代码 CSV、能力开关、partial mock、外部依赖闸口、生产阻断台账、数据隔离规格、SMS/字段映射占位、AI 本地模型规格、v0.1/v0.2 文档审计，以及 6 份核心业务开发设计文档（bar / ticketing / amusement / supply-chain / operation-traceability / finance）、完整后台 SRS + 页面权限字段矩阵、平台治理 PRD、P0 权限矩阵。

## 2. 对抗式独立验证（本轮核心增量）

我把既有审计的"代码事实"声明与工作区现实逐项核对：

| 声明出处 | 声明值 | 实测值（2026-06-18 工作区） | 结论 |
|---|---|---|---|
| `development-documentation-audit-v0.2.md` §1 | OpenAPI operation 数历史快照 | 当前 OpenAPI 数量以 `verify:operation-matrix` / `verify:openapi-closure` 与生成矩阵为准 | ✅ 已按 R1-FINDING-A 改为动态口径 |
| `development-documentation-audit-v0.2.md` §1 | 历史迁移快照 | 已降格为历史审计材料；当前迁移/operation 数量不得引用该静态数字 | ✅ 通过 R1-FINDING-A 治理口径收口 |
| `data-isolation-and-tenant-scope-security-spec-v0.1.md` §3/§3A | 历史人工清单 + 自动迁移隔离基线 | 已新增 `npm run docs:data-isolation-spec` / `npm run verify:data-isolation-spec`，§3A 从 Flyway 迁移目录自动生成，当前解析到最新迁移并可校验 | ✅ 已脚本化，防止再次手抄漂移 |
| v0.1 对抗式审计 | 198 ops / V041 | 已被 v0.2 标注为历史 | ✅ 已正确降级 |
| `00-...entry` p0-first-coding-package 引用 | v0.2 称"已创建该文件" | 文件存在（55 行），入口引用 15 处 | ✅ 落地 |
| v0.2 新增 3 文档 | data-isolation / sms-placeholder / first-coding-package | 三者均存在且为 2026-06-18 08:3x 创建 | ✅ 落地 |

**R1-FINDING-A（P1，文档治理）**：**已关闭（2026-06-21）**。计数漂移治理口径已落地：OpenAPI operation 数量以 `verify:operation-matrix` / `verify:openapi-closure` 和生成矩阵为准；迁移 scope 基线以 `docs:data-isolation-spec` 自动生成、`verify:data-isolation-spec` 校验；`verify:delivery-gates` 已接入该校验。历史审计中的静态迁移数/operation 数只作为历史快照，不再作为当前验收事实。

## 3. 用户点名业务闭环的 file:line 级矛盾下钻（v0.2 未覆盖）

下表为本轮新发现、带证据的跨文档矛盾/不可开发点。完整证据见 §6 各草案。

| 编号 | 闭环 | 矛盾/缺口 | 证据 | 等级 |
|---|---|---|---|---|
| R1-LOOP-01 | 卡座提成 → 财务台账 | **已关闭（2026-06-21）**：卡座销售提成计算后生成 `ar_ap_items(direction=payable, source_type=employee_commission, source_id=bar_commission_calculations.id)`，FinanceService 白名单和 source 追踪已覆盖 | BarBillingService / FinanceBarReceivableLedgerService / FinanceServiceTests / BarServiceTests | Closed |
| R1-LOOP-02 | 扫码支付（聚合支付意图）| **已关闭（2026-06-21）**：酒吧账单已提供 `POST /api/v1/bar/bills/{bill_id}/pay-intents`，复用 `PaymentIntentRequest`、`PaymentChannelPolicy` 与 `PaymentIntentRecord`，OpenAPI/Controller/Service 测试覆盖 | BarController / BarBillingService / BarServiceTests / openapi-p0 | Closed |
| R1-LOOP-03 | 卡座订台定金（留痕）| **已关闭（2026-06-21）**：酒吧侧已实现订台定金规则、`pay-deposit`、`check-in-deposit`、取消退款/没收，事件口径为 `bar.reservation_deposit_*`，并有 Deposit 通配测试覆盖 | BarEntryService / BarController / BarServiceTests#*Deposit* / openapi-p0 | Closed |
| R1-LOOP-04 | 存酒作废（高价值双证据）| **已关闭（2026-06-21）**：`saved_bottles` 已含 `item_id`、`is_high_value`、`high_value_threshold_cents`；高价值作废需照片+视频双证据，生成 `inventory_high_value_reviews(subject_type=saved_bottle_void)` 并冻结为 `disputed_frozen` | InventoryService / JdbcInventoryRepository / InventoryServiceTests#*HighValue* | Closed |
| R1-LOOP-05 | 取酒核身 | **已关闭（2026-06-21）**：取酒请求支持标签/姓名核对；不一致时 claim 状态为 `pending_identity_review` 且存酒转入 `pending_identity_review`；争议流程可转 `disputed`，高价值作废冻结为 `disputed_frozen`；手机号只作辅助输入不作为唯一核身 | InventoryService / SavedBottleClaimRequest / InventoryServiceTests#*SavedBottle* | Closed |
| R1-LOOP-06 | 酒水权益领取 / 库存事件 | **已关闭（2026-06-21）**：库存/酒水事件按 `inventory.*` 作为分析域权威事件；`bar.stock` 仅保留为表名/权限名/历史别名，不作为审计事件消费 | operation-traceability / bar design / grep backend 无 bar.stock 事件 | Closed |
| R1-LOOP-07 | 全过程留痕 | **已关闭（2026-06-21）**：已实现 `GET /api/v1/evidence/traceability/auto-capture-rate`，阈值与窗口可验收；测试覆盖时间线权限、阈值计算和 JDBC 聚合 | EvidenceEventController / TraceabilityService / TraceabilityJdbcIntegrationTests / EvidenceEventControllerTests | Closed |
| R1-LOOP-08 | 酒水兑换 / 卡座入场 verify 接口 | **已关闭（2026-06-21）**：`POST /api/v1/bar/entitlement-redemptions/drink` 与 `POST /api/v1/bar/reservations/{id}/entry-allowance/verify` 均已进入 OpenAPI、操作矩阵和契约附录；卡座 verify 不消耗名额，正式 `check-in-guest` 在事务中锁定 reservation 防并发超额 | BarController / BarService / BarEntryService / BarServiceTests / BarControllerTests / openapi-p0 | Closed |
| R1-LOOP-09 | 后台手动录入入职路径 | **已关闭（2026-06-21）**：SRS 与能力开关已正式承认后台手动录入为企业微信关闭时默认入口；私人手机注册仍保留为另一条入口 | full-admin-srs FR-ADM-ACC-001 / switches §4 / AccessUsersPage / AccessServiceTests | Closed |
| R1-LOOP-10 | 核销人工放行 | **已关闭（2026-06-21）**：`TicketVerificationService.manualOverride` 已写入 `status=manual_overridden`，V067 状态约束包含该状态，事件/权限链已对齐 | TicketVerificationService / V067__ticket_multiperson_reverse.sql / TicketingServiceTests | Closed |
| R1-LOOP-11 | 二维码刷新周期 | **已关闭（2026-06-21）**：二维码刷新周期采用票种级配置，默认 60 秒；`ticket_credentials.refresh_interval_seconds` 与 `ticket_types.refresh_interval_seconds` 均已落库并约束 {30,60} | V064 / V113 / TicketVerificationService / TicketCatalogService | Closed |
| R1-LOOP-12 | 多人票 × 容量 | **已关闭（2026-06-21）**：游乐核销采用“全有或全无”，按权益 `numPeople` 一次性占用场次/点位设备/项目安全三维容量；任一维度余量不足 N 则整单拒绝并回补权益 | AmusementService / CapacityClaimContext / JdbcAmusementRepository / V132 / AmusementServiceTests | Closed |
| R1-LOOP-13 | 支付方式枚举对账 | **已关闭（2026-06-21）**：`PaymentChannelPolicy` 统一中文/英文/供应商别名，支付意图与酒吧结算分别归并为 canonical 渠道；酒吧结算不再自维护自由词表 | PaymentChannelPolicy / BarBillingService / PaymentChannelPolicyTests | Closed |
| R1-LOOP-14 | 后台手动录入字段/同意凭据/事件 | **已关闭（2026-06-21）**：ADM-ACC-001 已补 organization_id/merchant_id、初始密码、phone_hash、consent_reference、evidence_attachment_ids；事件统一为 `access.user.changed` 与 `permission_template.applied` | full-admin-page-permission-field-matrix / openapi-p0 / AccessServiceTests | Closed |

> 说明：上述均为**文档层矛盾**，是否在代码中已规避需第二轮代码审计核实（见 §7）。本轮按"文档须能让产品/开发/测试/运营/供应商执行"的标准判定，凡文档自相矛盾或缺承接即记缺口。

## 4. 用户点名能力开关 / AI / mock 的审计结论

- **能力开关**：14 项能力（聚合支付、团购售票、企微、人脸/年龄、MDM、对象存储、开放平台、餐饮/零售 POS、机器人、视频、闸机/NFC/扫码、160 本地 AI、NAS 备份）已在 `integration-capability-switches-v0.1.md` 开关化，默认除"160 本地 AI"与"NAS 备份"外**全部关闭**。**本轮新增缺口**：`sms_notification` 能力开关已在 v0.2 的 SMS 占位文档定义，但**尚未并入** `integration-capability-switches-v0.1.md` 主矩阵与 `-latest.json`（落地动作见占位文档 §5）→ 详见交付物《第三方能力开关与 mock 策略》。
- **160 本地大模型替代闭环**：`t3d-local-llm-ai-quality-v0.1.md` 已固化 loopback-only、脱敏（手机号/身份证/邮箱/卡号）、4000 字截断、置信度<0.60 或超时/非法 JSON 时强制人工复核、**禁止 AI 直接决策**（支付/退款/核销/处罚/提成/合同/库存/设备）。闭环**自洽且可验收**（`verify:t3d-local-llm-ai-quality`）。无新 P0/P1。
- **mock/stub 是否足以支撑验收**：8 条 partial 需求（REQ-17/21/23b/25/32/40/41/44）均有 mock/adapter/页面/后端测试/E2E 替身（`partial-requirement-mock-coverage-v0.1.md`），**足以联调与业务验收**；真实支付/POS/人脸/视频/机器人/设备/对象存储/开放平台/MDM/NAS 证据仍按生产阻断台账回填。R1-LOOP-08 已关闭：兑换/入场 verify 接口均已进入 OpenAPI、操作矩阵和契约附录。

## 5. 风险汇总（P0/P1/P2/P3）

- **P0（文档层）**：本轮**未发现会立即把开发带偏的开放 P0 文档缺陷**。ISO-GAP-01 已关闭：跨租户越权 finding 系列已由安全基线生成器纳管，`verify:tenant-isolation` 已通过并进入交付闸口。
- **P1**：R1-FINDING-A 已关闭；R1-LOOP-01~09 业务闭环缺口已按 §6 草案逐项关闭或转入可验证工程项。
- **P2**：业务闭环类 R1-LOOP-01~14 已关闭；剩余 P2 为文档维护性问题（完成审计文档高度重复、audit-map CSV 滞后、dev-design `merchant_id` 写成 uuid），不阻塞当前编码验收。
- **P3**：`_archive/**` 旧计数、markdown 格式噪音。

## 6. 可开发规格草案（把不可开发/矛盾内容补成可执行口径）

以下草案以"统一基线 D1–D13 优先、低等级文档不反向覆盖高等级"为原则。落地时应回写到对应 S2 设计文档与 OpenAPI。

### 草案 R1-SPEC-01｜卡座提成入财务台账（解 R1-LOOP-01）
- `bar_commission_calculations` 计算后生成 `ar_ap_items`：`source_type='employee_commission'`，`source_id = bar_commission_calculations.id`（非 bill_id，避免与账单应收冲突），`direction='payable'`，`counterparty=销售人员`。已由 `FinanceServiceTests#createPayableItemAcceptsEmployeeCommissionSourceForBarSeatCommission` 验证。
- finance §6 生成流程增"员工提成"分支：触发时机=账单 settle 且提成已计算；金额=可计提消费额×费率（15%/10%，D13）；幂等键=commission.id。
- 验收：Given 卡座账单结清且归属销售人员，When 计算提成，Then 生成唯一 payable `ar_ap_item` 且重复触发不产生第二条。

### 已落地 R1-SPEC-02｜酒吧账单聚合支付意图统一（解 R1-LOOP-02、R1-LOOP-13）
- 酒吧账单支付统一走 `POST /api/v1/finance/payment-intents`（与票务同源），`bar/bills/{id}/pay-intents` 仅作为业务封装转发，不自建二维码语义。
- 支付方式枚举统一映射已落地：`PaymentChannelPolicy` 将中文/英文/供应商别名归一为 canonical 渠道；支付意图允许 `aggregate_pay/wechat/alipay/unionpay/bank_card/mock`，酒吧结算归并为 `aggregate_pay/cash/pos/bank_transfer/other`。
- 验收：Given 酒吧账单，When 发起聚合支付，Then 复用 finance payment-intent 语义并在对账时按映射归并。

### 草案 R1-SPEC-03｜订台定金支付事件补齐（解 R1-LOOP-03）
- 酒吧侧补事件 `reservation.deposit_paid`（客人/一体机/销售工作台付定金完成），与权威字典 traceability §4.1 对齐；bar §5.1.2/§6.3/§9.1 增该事件触发点与 `deposit_status` 流转。

### 草案 R1-SPEC-04｜存酒高价值判定与核身状态（解 R1-LOOP-04、R1-LOOP-05）
- `saved_bottles` 增 `is_high_value BOOLEAN`、`high_value_threshold_cents BIGINT`（或外键 `product_id → bar_products.is_high_value`，二选一并在设计文档固化）；作废时据此决定是否强制照片+视频双证据+主管复核。
- 存酒状态机已支持 `pending_identity_review`（取酒核身不一致）/`disputed_frozen`（高价值作废复核冻结）两态；取酒接口通过 `customerNameOrNickname` / `bottleLabelValue` 核身，不一致落 `pending_identity_review`。
- `customer_phone` 明确为"可逆加密 + 哈希索引列"，支持按手机号精确查询，不用不可逆脱敏。

### 草案 R1-SPEC-05｜库存事件名统一（解 R1-LOOP-06）
- 按 D4 唯一事件字典：酒吧库存动作统一改用 `inventory.*`（`inventory.inbound_confirmed`/`inventory.consume_*`/`inventory.surplus_recorded`/`inventory.damaged`/`inventory.loss_exception`）；`bar.stock` 仅作为表名/权限或旧别名，不进入分析域事件消费。

### 草案 R1-SPEC-06｜自动留痕率 P0 验收 KPI + 过程枚举补齐（解 R1-LOOP-07）
- 定义 P0 目标：关键动作（核销/支付/出品/送达/库存变动）自动留痕率（capture_source≠manual）**≥ 80%**（可配置阈值）；新增验收条目"自动留痕率统计页 + 阈值告警"。
- `process_type` 增 `bar_table_commission`、`saved_bottle_lifecycle`；`business_object_type` 增 `commission`、`saved_bottle`。

### 已落地 R1-SPEC-07｜兑换/卡座入场 verify 接口入契约（解 R1-LOOP-08）
- 把 `POST /api/v1/bar/entitlement-redemptions/drink`、卡座入场名额 verify 接口正式写入 `openapi-p0.yaml` + operation 验收矩阵 + closure 附录（含权限、x-auth-context、事件、幂等），再开放联调/验收。

### 草案 R1-SPEC-08｜后台手动录入入职路径正式化（解 R1-LOOP-09、R1-LOOP-14）
- 在 `platform-governance-prd` 与 SRS FR-ADM-ACC-001 显式承认**两条并行入职入口**：①私人手机 App 注册→审批；②IT/HR 后台手动录入（企微关闭时为默认入口）。
- ADM-ACC-001 字段表已补 `organization_id`、`merchant_id`、`password`、`phone_hash`、`consent_reference`、`evidence_attachment_ids`；手动建号路径承接告知/授权证据。
- 单次有效消耗事件码统一：以 P0 矩阵 `permission_template.applied`（含 single_use 消耗语义）为准，页面矩阵 `TEMP_PERMISSION_USED` 改为引用该码或显式登记别名映射。

### 已落地 R1-SPEC-09｜核销人工放行状态 / 二维码刷新周期 / 多人票容量（解 R1-LOOP-10~12）
- 票务核销记录状态机增 `manual_overridden` 态，与事件 `verification.manual_overridden` 对齐。
- 二维码刷新周期：定为**票种级可配置**，默认 60 秒；`ticket_credentials` 增 `refresh_interval_seconds`（取值约束 {30,60}）。
- 多人票 × 容量：定义"全有或全无"——N 人一次核销，任一容量维度余量 < N 即整单拒绝并提示，不做部分放行（P0 口径，部分放行后置）。

## 7. 第二轮代码审计需重点核实的需求编号

以下文档矛盾需在代码层确认"是否已规避/已实现"，否则升级为代码缺陷：

| 第二轮检查项 | 关联需求 | 关联草案 |
|---|---|---|
| 提成是否真生成 `ar_ap_items`（source_type/source_id/幂等）| REQ-26b | R1-SPEC-01 |
| 酒吧账单是否复用 finance payment-intent | REQ-21 | R1-SPEC-02 |
| 定金支付是否落 `reservation.deposit_paid` 事件 | REQ-25 / REQ-50 | R1-SPEC-03 |
| `saved_bottles` 是否有高价值字段、核身复核状态、手机号是否可查 | REQ-30 / REQ-31 / REQ-32 | R1-SPEC-04 |
| 库存事件实际写 `inventory.*` 还是 `bar.stock.*`，分析域消费哪个 | REQ-22 / REQ-23 / REQ-40 | R1-SPEC-05 |
| 是否统计 capture_source 自动留痕率；process_type 是否含提成/存酒 | REQ-34 / REQ-40 | R1-SPEC-06 |
| 兑换/卡座入场 verify 接口是否在 OpenAPI 中 | REQ-23 / REQ-26b | 已关闭：OpenAPI + 操作矩阵 + 契约附录已覆盖 |
| 后台手动录入是否有独立入口/org_id/merchant_id 字段/同意凭据 | REQ-63 / REQ-59 | R1-SPEC-08 |
| 核销 `manual_overridden` 状态、`refresh_interval_seconds` 字段、多人票容量校验 | REQ-05 / REQ-08 / REQ-46 | R1-SPEC-09 |
| 迁移 scope 基线是否由脚本生成并纳入校验 | REQ-12（积分余额）/ REQ-69（复核发起人）| 已关闭：`docs:data-isolation-spec` + `verify:data-isolation-spec` |

## 8. 本轮读取文档清单与产出（详见汇报与各交付物）
本报告为第一轮 5 份交付物之一。配套交付物：`r1-documentation-map-20260618.md`、`r1-requirement-api-page-table-test-mapping-matrix-20260618.md`、`r1-third-party-capability-switch-and-mock-strategy-20260618.md`、`r1-pending-supplier-information-backfill-checklist-20260618.md`。
