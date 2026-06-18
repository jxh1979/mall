# 第一轮 需求文档审计报告 R1（2026-06-18）

> 审计对象：`/home/james/complex-management`（综合体管理系统）当前工作区全量需求/产品/开发设计/接口/数据库迁移/验收/Runbook/生产证据文档。
> 审计方法：documentation-specialist（审计工作流）+ pm-utility-pm-critic 反向质疑 + deliver-acceptance-criteria / deliver-edge-cases（验收与边界）+ dean-user-story-mapping / deliver-user-stories（角色路径）+ gstack-spec / gstack-review（可开发规格与对抗式复核）。
> 边界：**只读代码、不改代码**；只修正/新增文档，不删除既有文档。
> 机器标记：R1_REQ_DOC_AUDIT_READY、INDEPENDENTLY_VERIFIED_AGAINST_CODE_20260618、SUPERSEDES_NOTHING（与既有审计并存、互补，不取代）。

## 0. 一句话结论

本项目文档体系**异常成熟**（357 个文档文件，已有 S0 入口、统一基线 D1–D13、需求追溯矩阵 REQ-01~REQ-92、OpenAPI 207 ops、能力开关矩阵、partial mock 覆盖、生产阻断台账、并在 **2026-06-18 当天刚完成一份 v0.2 文档审计**）。本轮**不重复造轮子**，而是做三件既有审计未做的事：①**对抗式独立验证**既有审计与"已修"声明是否与代码现实一致（发现新的计数漂移）；②对用户点名的业务闭环做 **file:line 级矛盾下钻**（发现 14 条 v0.2 未捕获的跨文档矛盾/不可开发点）；③把不可开发/不可验收/相互矛盾的内容补成**可开发规格草案**（§6）。

可否进入下一阶段（结论与既有审计一致）：自有系统开发 / 内网联调 / 产品测试验收 **可以**；完整后台、真实供应商联调 **条件可以**；公开生产上线 **不可以**（外部证据未关闭）。

## 1. 本轮读取的文档（清单见报告末 §8）

入口/治理类、统一基线、需求追溯矩阵、需求-代码 CSV、能力开关、partial mock、外部依赖闸口、生产阻断台账、数据隔离规格、SMS/字段映射占位、AI 本地模型规格、v0.1/v0.2 文档审计，以及 6 份核心业务开发设计文档（bar / ticketing / amusement / supply-chain / operation-traceability / finance）、完整后台 SRS + 页面权限字段矩阵、平台治理 PRD、P0 权限矩阵。

## 2. 对抗式独立验证（本轮核心增量）

我把既有审计的"代码事实"声明与工作区现实逐项核对：

| 声明出处 | 声明值 | 实测值（2026-06-18 工作区） | 结论 |
|---|---|---|---|
| `development-documentation-audit-v0.2.md` §1 | OpenAPI 207 operations | `grep` openapi-p0.json = **207** | ✅ 准确 |
| `development-documentation-audit-v0.2.md` §1 | 迁移 52 个 / V001–V052 | 实际 **54 个 / 到 V054**（V053 治理复核发起人、V054 会员积分余额非负）| ❌ **已漂移**：当天的审计已落后 2 个迁移 |
| `data-isolation-and-tenant-scope-security-spec-v0.1.md` §3 | 逐表覆盖 V044–V052 | V053、V054 未纳入 §3；V053 恰好涉及 §7 标注为 ISO-GAP 的 `supervisor_review_decisions`（发起人列）| ❌ **隔离规格已不全**：新迁移未登记 |
| v0.1 对抗式审计 | 198 ops / V041 | 已被 v0.2 标注为历史 | ✅ 已正确降级 |
| `00-...entry` p0-first-coding-package 引用 | v0.2 称"已创建该文件" | 文件存在（55 行），入口引用 15 处 | ✅ 落地 |
| v0.2 新增 3 文档 | data-isolation / sms-placeholder / first-coding-package | 三者均存在且为 2026-06-18 08:3x 创建 | ✅ 落地 |

**R1-FINDING-A（P1，文档治理）**：计数漂移是**系统性、复发性**问题。连"当天"的权威审计（v0.2）和"当天"的数据隔离规格都已落后于同日继续推进的代码（V052 → V054）。根因：多份文档把"迁移数 / operation 数 / 测试数"写成**静态快照**而非引用实时校验输出。**建议口径（沿用 v0.2 §6.1 但升级为强制）**：所有计数类断言改为引用 `npm run verify:*` / `p0-openapi-operation-acceptance-matrix` 实时产物；并新增 `npm run docs:data-isolation-spec`（占位）使隔离规格 §3 由迁移目录自动生成，杜绝手抄漂移。

## 3. 用户点名业务闭环的 file:line 级矛盾下钻（v0.2 未覆盖）

下表为本轮新发现、带证据的跨文档矛盾/不可开发点。完整证据见 §6 各草案。

| 编号 | 闭环 | 矛盾/缺口 | 证据 | 等级 |
|---|---|---|---|---|
| R1-LOOP-01 | 卡座提成 → 财务台账 | `ar_ap_items.source_type` 枚举有 `employee_commission`，但 finance §6 生成流程**无该路径**，`source_id` 口径未定 → 提成入台账不可开发 | finance §4.1:63 / §3:123 / §6 vs bar §6.9A:684 | **P1** |
| R1-LOOP-02 | 扫码支付（聚合支付意图）| 票务走两段式 `orders/{id}/pay-intents → finance/payment-intents`，酒吧账单**无 payment-intents 入口**，聚合支付口径不一致 | bar §7.3:933-935 vs ticketing §7.2:838-839 | **P1** |
| R1-LOOP-03 | 卡座订台定金（留痕）| 客人付定金事件 `reservation.deposit_paid` 仅在 traceability 定义，**酒吧文档完全缺失** → 定金支付动作在酒吧侧无事件落点 | traceability §4.1:179/200 vs bar §5.1.2/§9.1 | **P1** |
| R1-LOOP-04 | 存酒作废（高价值双证据）| `saved_bottles` 表**无 `is_high_value`/外键到物料**，高价值判定在数据模型上断链 | bar §5.3.4:354 vs §6.11:716-741 / §6.13:781 | **P1** |
| R1-LOOP-05 | 取酒核身 | 核身不一致要求"进入人工复核"，但存酒状态机**无"人工复核/争议冻结"态**，验收项 §14-15 无法落地；且 `customer_phone` 脱敏与"按手机号查询"冲突 | bar §5.3.2:329 / §4.5:168-176 / §7.5:957 / 字段:725 | **P1** |
| R1-LOOP-06 | 酒水权益领取 / 库存事件 | 酒吧侧 `bar.stock.*` 与权威字典 `inventory.*` **双轨未映射**，违反统一基线 D4"一字一致"，分析域消费断链 | bar §9.4:1100-1104 vs traceability §4.1.1:213-222 / :12 | **P1** |
| R1-LOOP-07 | 全过程留痕 | "capture_source 自动留痕率"**只有字段、无 P0 目标阈值/验收门槛** → 不可验收；`process_type`/`business_object_type` 枚举**缺卡座提成、存酒** → 两闭环过程时间线不可开发 | traceability §6.5:522-523 / §8:580 / §6.4:490-491 | **P1** |
| R1-LOOP-08 | 酒水兑换 / 卡座入场 verify 接口 | `POST /bar/entitlement-redemptions/drink`、卡座入场名额 verify 接口**未进入 OpenAPI P0 闭环附录**，被本文档执行口径"未进 OpenAPI 不得联调/验收"冻结 | bar §7.8:992-993 / §7:885/13 | **P1** |
| R1-LOOP-09 | 后台手动录入入职路径 | 治理 PRD 把入职描述为"私人手机 App 注册→审批"，**全文未承认后台手动录入为独立入口**，与能力开关 §4"企微关闭时后台手动录入是默认入口"口径冲突 | governance PRD §6.9:189-194 / §7.6:282-286 vs switches §4:37 | **P1** |
| R1-LOOP-10 | 核销人工放行 | 人工放行有事件 `verification.manual_overridden`，但票务核销记录状态机**无 `manual_overridden` 状态** → "人工放行留痕"无状态承载 | ticketing §4.5:185-194 vs traceability:265 | P2 |
| R1-LOOP-11 | 二维码刷新周期 | "30 秒或 60 秒"全目录始终**未拍板取值/未定配置层级**，`ticket_credentials` 无 `refresh_interval_seconds` 字段 | ticketing :44/308/634/646 | P2 |
| R1-LOOP-12 | 多人票 × 容量 | 多人票一次核销 N 人与游乐三维度容量交互（超一维未超另一维时部分放行？全拒？）未定义 | ticketing §5.3:353 vs amusement §6.4:282-288 | P2 |
| R1-LOOP-13 | 支付方式枚举对账 | finance `payment_channel` 用中文枚举、bar 用英文枚举，**无映射**，对账归并规则缺失 | finance §4.2:83 vs bar §6.10:702 | P2 |
| R1-LOOP-14 | 后台手动录入字段/同意凭据/事件 | ADM-ACC-001 字段缺 `org_id/merchant_id` 独立列（治理 §7.6 要求必绑）；手动建号路径无 C4 同意凭据承接；单次有效消耗事件码 `TEMP_PERMISSION_USED` 与 P0 矩阵 `permission_template.applied` 不一致 | csv:9-10 vs governance §7.6:284 / P0 §6:182 | P2 |

> 说明：上述均为**文档层矛盾**，是否在代码中已规避需第二轮代码审计核实（见 §7）。本轮按"文档须能让产品/开发/测试/运营/供应商执行"的标准判定，凡文档自相矛盾或缺承接即记缺口。

## 4. 用户点名能力开关 / AI / mock 的审计结论

- **能力开关**：14 项能力（聚合支付、团购售票、企微、人脸/年龄、MDM、对象存储、开放平台、餐饮/零售 POS、机器人、视频、闸机/NFC/扫码、160 本地 AI、NAS 备份）已在 `integration-capability-switches-v0.1.md` 开关化，默认除"160 本地 AI"与"NAS 备份"外**全部关闭**。**本轮新增缺口**：`sms_notification` 能力开关已在 v0.2 的 SMS 占位文档定义，但**尚未并入** `integration-capability-switches-v0.1.md` 主矩阵与 `-latest.json`（落地动作见占位文档 §5）→ 详见交付物《第三方能力开关与 mock 策略》。
- **160 本地大模型替代闭环**：`t3d-local-llm-ai-quality-v0.1.md` 已固化 loopback-only、脱敏（手机号/身份证/邮箱/卡号）、4000 字截断、置信度<0.60 或超时/非法 JSON 时强制人工复核、**禁止 AI 直接决策**（支付/退款/核销/处罚/提成/合同/库存/设备）。闭环**自洽且可验收**（`verify:t3d-local-llm-ai-quality`）。无新 P0/P1。
- **mock/stub 是否足以支撑验收**：8 条 partial 需求（REQ-17/21/23b/25/32/40/41/44）均有 mock/adapter/页面/后端测试/E2E 替身（`partial-requirement-mock-coverage-v0.1.md`），**足以联调与业务验收**；真实支付/POS/人脸/视频/机器人/设备/对象存储/开放平台/MDM/NAS 证据仍按生产阻断台账回填。**结论：mock 充分性合格**，但 R1-LOOP-08（兑换/入场 verify 接口未进 OpenAPI）会使这两条能力的 mock 验收缺契约锚点，需先补契约。

## 5. 风险汇总（P0/P1/P2/P3）

- **P0（文档层）**：本轮**未发现会立即把开发带偏的开放 P0 文档缺陷**。注意：v0.2 登记的 ISO-GAP-01（安全 finding 台账无跨租户越权条目）仍为 P0 工程/安全待办，本轮文档已承接（数据隔离规格），但 finding 台账回填仍未完成。
- **P1**：R1-FINDING-A（计数漂移复发）；R1-LOOP-01~09（9 条业务闭环矛盾/不可开发，已在 §6 补草案）；隔离规格未纳入 V053/V054。
- **P2**：R1-LOOP-10~14；v0.2 登记的 P2 维护性问题（完成审计文档高度重复、audit-map CSV 滞后、dev-design `merchant_id` 写成 uuid）。
- **P3**：`_archive/**` 旧计数、markdown 格式噪音。

## 6. 可开发规格草案（把不可开发/矛盾内容补成可执行口径）

以下草案以"统一基线 D1–D13 优先、低等级文档不反向覆盖高等级"为原则。落地时应回写到对应 S2 设计文档与 OpenAPI。

### 草案 R1-SPEC-01｜卡座提成入财务台账（解 R1-LOOP-01）
- `bar_table_commissions` 结算后生成 `ar_ap_items`：`source_type='employee_commission'`，`source_id = bar_table_commissions.id`（**非 bill_id**，避免与账单应收冲突 `uq(source_type,source_id,direction)`），`direction='payable'`，`counterparty=销售人员/内部薪酬主体`。
- finance §6 生成流程增"员工提成"分支：触发时机=账单 settle 且提成已计算；金额=可计提消费额×费率（15%/10%，D13）；幂等键=commission.id。
- 验收：Given 卡座账单结清且归属销售人员，When 计算提成，Then 生成唯一 payable `ar_ap_item` 且重复触发不产生第二条。

### 草案 R1-SPEC-02｜酒吧账单聚合支付意图统一（解 R1-LOOP-02、R1-LOOP-13）
- 酒吧账单支付统一走 `POST /api/v1/finance/payment-intents`（与票务同源），`bar/bills/{id}/pay-intents` 仅作为业务封装转发，不自建二维码语义。
- 支付方式枚举统一映射：bar `customer_scan_qr/staff_scan_pay_code/cash/pos` ↔ finance `payment_channel`（聚合支付/现金/POS）建立双列映射表，落 `unified-standards-baseline` 附录。
- 验收：Given 酒吧账单，When 发起聚合支付，Then 复用 finance payment-intent 语义并在对账时按映射归并。

### 草案 R1-SPEC-03｜订台定金支付事件补齐（解 R1-LOOP-03）
- 酒吧侧补事件 `reservation.deposit_paid`（客人/一体机/销售工作台付定金完成），与权威字典 traceability §4.1 对齐；bar §5.1.2/§6.3/§9.1 增该事件触发点与 `deposit_status` 流转。

### 草案 R1-SPEC-04｜存酒高价值判定与核身状态（解 R1-LOOP-04、R1-LOOP-05）
- `saved_bottles` 增 `is_high_value BOOLEAN`、`high_value_threshold_cents BIGINT`（或外键 `product_id → bar_products.is_high_value`，二选一并在设计文档固化）；作废时据此决定是否强制照片+视频双证据+主管复核。
- 存酒状态机增 `pending_identity_review`（取酒核身不一致）/`disputed_frozen`（争议冻结）两态；§7.5 pickup 接口定义请求字段 `claim_name`、`claim_label_value` 并定义不一致 → 落 `pending_identity_review`。
- `customer_phone` 明确为"可逆加密 + 哈希索引列"，支持按手机号精确查询，不用不可逆脱敏。

### 草案 R1-SPEC-05｜库存事件名统一（解 R1-LOOP-06）
- 按 D4 唯一事件字典：酒吧库存动作统一改用 `inventory.*`（`inventory.inbound_confirmed`/`inventory.consume_*`/`inventory.surplus_recorded`/`inventory.damaged`/`inventory.loss_exception`），`bar.stock.*` 仅可作旧别名注释，分析域只消费 `inventory.*`。若保留 `bar.stock.*`，必须在 traceability §4.1 显式登记一对一映射表。

### 草案 R1-SPEC-06｜自动留痕率 P0 验收 KPI + 过程枚举补齐（解 R1-LOOP-07）
- 定义 P0 目标：关键动作（核销/支付/出品/送达/库存变动）自动留痕率（capture_source≠manual）**≥ 80%**（可配置阈值）；新增验收条目"自动留痕率统计页 + 阈值告警"。
- `process_type` 增 `bar_table_commission`、`saved_bottle_lifecycle`；`business_object_type` 增 `commission`、`saved_bottle`。

### 草案 R1-SPEC-07｜兑换/卡座入场 verify 接口入契约（解 R1-LOOP-08）
- 把 `POST /api/v1/bar/entitlement-redemptions/drink`、卡座入场名额 verify 接口正式写入 `openapi-p0.yaml` + operation 验收矩阵 + closure 附录（含权限、x-auth-context、事件、幂等），再开放联调/验收。

### 草案 R1-SPEC-08｜后台手动录入入职路径正式化（解 R1-LOOP-09、R1-LOOP-14）
- 在 `platform-governance-prd` 与 SRS FR-ADM-ACC-001 显式承认**两条并行入职入口**：①私人手机 App 注册→审批；②IT/HR 后台手动录入（企微关闭时为默认入口）。
- ADM-ACC-001 字段表补 `organization_id`、`merchant_id` 独立列（不只 data_scope）；手动建号路径补"告知签署/同意凭据 `employee_notice_id`"承接（满足合规 C4）。
- 单次有效消耗事件码统一：以 P0 矩阵 `permission_template.applied`（含 single_use 消耗语义）为准，页面矩阵 `TEMP_PERMISSION_USED` 改为引用该码或显式登记别名映射。

### 草案 R1-SPEC-09｜核销人工放行状态 / 二维码刷新周期 / 多人票容量（解 R1-LOOP-10~12）
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
| 兑换/卡座入场 verify 接口是否在 OpenAPI 中 | REQ-23 / REQ-26b | R1-SPEC-07 |
| 后台手动录入是否有独立入口/org_id/merchant_id 字段/同意凭据 | REQ-63 / REQ-59 | R1-SPEC-08 |
| 核销 `manual_overridden` 状态、`refresh_interval_seconds` 字段、多人票容量校验 | REQ-05 / REQ-08 / REQ-46 | R1-SPEC-09 |
| 迁移 V053/V054 的隔离/约束是否纳入隔离规格与测试 | REQ-12（积分余额）/ REQ-69（复核发起人）| R1-FINDING-A |

## 8. 本轮读取文档清单与产出（详见汇报与各交付物）
本报告为第一轮 5 份交付物之一。配套交付物：`r1-documentation-map-20260618.md`、`r1-requirement-api-page-table-test-mapping-matrix-20260618.md`、`r1-third-party-capability-switch-and-mock-strategy-20260618.md`、`r1-pending-supplier-information-backfill-checklist-20260618.md`。
