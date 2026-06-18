# 需求覆盖追溯矩阵 V0.1

> ⚠️ **R2/R3 代码审计修订横幅（2026-06-18）**：本矩阵的"承接文档+章节"反映的是**设计意图**，其"已覆盖/已补 X 字段"措辞**不等于"已在 Flyway 迁移与代码中实现"**。第二/三轮代码审计（`r2-code-audit-req26b-req21-req30-20260618.md`、`r3-migration-level-drift-audit-20260618.md`）发现**系统性"已设计但从未迁移"漂移**：已核 15 条需求中多数存在不同程度漂移。
> - **真实功能缺失（迁移+代码零命中）**：REQ-22（按杯出酒量/损耗字段）、REQ-60（职级 staff_levels/level_rank）、REQ-61（薪资字段）、REQ-69（审批层级枚举/admission_audits.approval_level）。
> - **换了实现模型（矩阵字段名不实）**：REQ-34/37/40 留痕三层模型 `business_process_instances/step_records/service_responsibility_records`+`capture_source` 从未建表，实际用 `audit_events/evidence_events/evidence_reviews/evidence_attachments` 事件模型替代（服务责任链/自动留痕率未实现）；REQ-12/13/14 积分表实为 `customer_points_*`，`points_rules/point_gifts` 不存在。
> - **单字段/局部漂移**：REQ-21（现金/POS 字段与事件缺）、REQ-26（缺 deposit_rule）、REQ-30/32（saved_bottles 高价值/标签/手机号列缺）、REQ-46/48（容量配置表/检修表缺，核心逻辑部分存在）。
> - REQ-26b 提成：代码已落 `bar_commission_calculations` 计算台账，满足 D13"可复核台账或应付依据"的"或"，P0 合规。
>
> **修复进展（2026-06-18，由 verify:matrix-vs-migration 守卫）**：以下原缺口已实现并落库——REQ-60/61 职级薪资(V055)、REQ-69 审批层级(V057)、REQ-22 按杯损耗(V058)、**REQ-34/37/40 留痕三层模型 business_process_instances/step_records/service_responsibility_records(V059，含 capture_source 与 refunded_by/waived_by/approved_by)**、**REQ-12 积分规则引擎 customer_points_rules + REQ-14 礼物收敛 customer_redemption_catalog_items(V060)**、积分兑换扣减改为 redeem_consume。注意：字面 `points_rules`/`point_gifts` 按收敛口径不建，真实表为 `customer_points_rules`/`customer_redemption_catalog_items`。仍未实现：REQ-26 deposit_rule、REQ-30/32 存酒高价值/核身/手机号（P1）。
>
> **R5 业务 HTML 缺口审计修订横幅（2026-06-18，业务说明地图对照）**：以两份业务人员 HTML（`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`）为基准，对真实 Flyway 迁移（V001–V065）+ 后端代码再次核验，发现下列行的"已覆盖"措辞**仍 overclaim**，以本横幅为准（行内已加 R5 标注，`覆盖状态` 已下修）：
> - **REQ-06/08/09 二维码防伪**：迁移/代码仅 `credential_hash`（SHA-256）+ 300s 过期；**无 HMAC 签名、无 `anti_fake_code_hash`/`signature_version` 字段、核销端不校验签名/nonce/刷新窗口**，30/60s 刷新窗口未生效，截图在窗口内可重放。原"已补签名版本"结论不实。→ **部分覆盖（R5）**。待业务确认：P0 是否接受无签名仅 hash。
> - **REQ-23 赠饮→吧台库存扣减**：bar/ticketing 间贯通逻辑零命中，`bar_production_ticket_events` 无 `inventory_movement_id`。→ **部分覆盖（R5）**。
> - **REQ-27/28 供应链合同与采购周期**：`supply_contracts`/`contract_mode` 迁移**零命中**；采购仅 `inventory_purchase_requests` 的"申请"create，审批/到货/入库流转缺。→ **未落地（R5 待业务确认 P0 范围）**。
> - **REQ-38/39/44 扫码交接全链**：`inventory_trace_handovers` 为单条平面记录、`handover_result` 写死 accepted，无出库/配送/接收/取走/送达分段状态机与缺口异常。→ **部分覆盖（R5）**。
> - **REQ-35/42/68 留痕与准入权威事件**：`order.acquired`/`order.sales_attributed`/`contract.blocked`/`contract.released` 代码**零命中**（实际事件名为 `contract.lifecycle_changed` 等）。→ 事件名漂移（R5）。
> - **REQ-54 企微会话留存/脱敏/审计**：后端无留存表/上传接口/到期触发/脱敏链/访问审计，仅 1 行 mock 种子。→ **未落地（R5，合规暴露，待业务确认是否本期底线）**。
> - **REQ-55 MDM 阻断**：依赖外部商业 MDM，自有侧 `management_status`/`policy_status`/`device_credential_hash` 等字段亦缺失。→ 外部依赖占位（R5，不计闭环）。
> - **REQ-56/57 员工设备凭证**：`staff_profiles`/`staff_device_credentials`/`employee_notices` 三表**零落地**；现存 `terminal_device_credentials` 是 kiosk 随机 token，非员工 Keystore 指纹。→ **未落地（R5）**。
> - **REQ-69 审批层级落点**：实际落 `supervisor_review_decisions`（V057），`admission_audits.approval_level` 表/列**不存在**；本行字段名过期。→ 字段名漂移（R5）。
> - **REQ-74/75 终端开关后端拒绝**：除 kiosk 一处外，finance/bar/ticketing/amusement/inventory 业务接口前**无开关校验**，可绕过；scope 仅产出 global/device 两值。→ **部分覆盖（R5）**。
> - **REQ-03/73 第三方团购回流**：无 `channel_ticket_mappings`/`external_order_no`/导入端点，"两类订单"只承接自营单。→ **部分覆盖（R5，待业务确认是否 P0）**。
> - **REQ-81/82 经营看板指标**：analytics 直读迁移种子（手写 INSERT，自标 manual baseline），无 ETL/聚合；前端总览 KPI 字段名与后端 DTO 零交集全渲染 0；analytics 包 0 处 `currentMerchantScope`（无商户行级隔离）。→ **部分覆盖（R5）**。
>
> **R5 业务裁决回填（2026-06-18，用户对 10 项待确认问题的拍板，作为正式验收口径来源）**：
> 1. **二维码防伪（REQ-06/08/09）**：P0=折中——哈希 + 核销端强制 30/60s 刷新窗口校验 + nonce 防重放（截图窗口内失效）；HMAC 签名版本化降 P1。
> 2. **第三方团购回流（REQ-03/73）统一口径（2026-06-18 三处统一）**：凡第三方一律预留接口 + 后台开关（默认 off）+ 不阻塞；**P0 = 预留接口/开关/不伪造对账**；真实渠道语义解析、`external_order_no`、双对账实做归 **P1（对应 r5-spec-T14）**。本期只做接口/开关预留，不强行实现。与 requirement-traceability-matrix、business-decision-log、business-module-mature-system-gap-analysis 三处统一。
> 3. **多人票核销 / 核销冲正**：均本期 P0。
> 4. **供应链（REQ-27/28）**：供应合同三模式 + 采购周期完整流转 + 结构化库房模型，三项本期 P0。
> 5. **日结锁定（REQ 财务）**：P0 必做——锁定后硬拦截改金额 + 补录需上级审批闭环（ar_ap_adjustments + locked_day_close_id + 审批状态机）+ 日结多源汇总；聚合口径待财务确认。
> 6. **企微会话（REQ-54）**：平台侧合规能力（可配置留存/到期归档销毁匿名化/权限脱敏/查看写审计）本期 P0；企微同步接口按第三方原则预留 + 开关。
> 7. **终端开关（REQ-74/75）**：后端拒绝铁律本期 P0；scope 扩到 7 维含模块级。
> 8. **合同准入（REQ-42/68/69）**：统一走 supervisor_review，作废 admission_audits/contract_admission_rules 表述；业务动作前内嵌准入校验调用，本期 P0。
> 9. **员工设备凭证（REQ-56/57）**：员工三表 + Android 端凭证（注册/Keystore 指纹/Root 检测/换机解绑）本期 P0；MDM（REQ-55）按第三方原则预留 + 开关。
> 10. **经营看板（REQ-81/82）**：真实聚合（替换种子 ETL）+ 商户行级隔离 currentMerchantScope + 前后端 DTO 字段对齐，三项本期 P0；聚合口径待财务/运营定义。
> 11. **前端权限渲染（REQ-58/59）**：前端按后端 effective-permissions 渲染（新增拉取接口），与后端 DENY 优先口径一致，本期 P0。
> **二轮裁决（2026-06-18，原未决项收口）**：订台定金=后台可配固定额+比例；供应链高价值复核=库存域内自建；扫码交接=员工扫码主动+机器人上报被动；企微留存=默认 180 天可配；空间=扁平 spaces 保留+全对象 project_id 隔离（四级模型作废）；权限点=全业务域最小颗粒度预置；日结/看板聚合口径=本审计先起草草案待财务/运营复核；MDM 自有侧字段=本期暂缓（仅接口+开关）。**剩余真正未决**：赠饮触发方式（开发期定）、企微授权配置 owner、聚合口径草案复核确认、对账自动匹配排期、MDM 验收降级签字人。详见 `r5-development-implementation-backlog-20260618.md` §四。
>
> **承接状态口径声明（2026-06-18 D1 收口）**：本矩阵"承接状态/覆盖状态"以 `matrix-migration-claims.json` + 真实迁移为准；凡 claims.json 标该 REQ 关键表/字段/端点 `absent`、或本横幅列出漂移者，**统一记为"部分覆盖（设计层覆盖，代码/迁移未落地）"**，不得再写"已覆盖"。设计层覆盖（PRD/开发设计有章节）但代码未落地的，一律记"部分覆盖（设计层）"。
>
> **使用规则**：本矩阵不得作为"已实现"凭据；判定实现以 `backend/src/main/resources/db/migration/` 与代码为准。`requirements-code-implementation-matrix-v0.1.csv` 的"已完整实现"列需 owner 据 R2/R3/R5 复核。凡 R5 横幅列出的 REQ，行内"已覆盖"一律以 R5 为准；R5 裁决已落地实现后须把对应 claims.json 项由 absent 改 present 并下修本横幅。

## 文档定位与依据

> **本文档为什么新增（定位）：** 两轮审计发现现有索引只按"经营动作"粗粒度映射且结论一律"已覆盖"（deliver-2），缺少把 `original-requirements-capture-v0.1.md` 每一条原始口径**逐条**对到承接文档与章节的稳定追溯锚点。本文为每条原始口径编 **REQ-序号**，给出"承接文档+章节 / 覆盖状态 / 备注"，特别覆盖容易遗漏的细则，供外部机器智能体快速核验"哪条需求落在哪份文档的哪一节、是否遗漏"。
>
> **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应；口径冲突一律以 `unified-standards-baseline-v0.1.md`（统一口径基准 D1–D13）与 `business-decision-log-v0.1.md` §32（2026-06-07 A1/A5/A6/A9 拍板）为准。

## 使用说明

- **需求编号 REQ-NN**：稳定锚点，对应 original-requirements-capture 的条目；同一节多条按 REQ-NN-a/b 细分。
- **覆盖状态**：已覆盖 / 部分覆盖 / 有待对齐 / 缺。"有待对齐"=已有文档但跨文档口径冲突，对齐口径见备注与基准。
- **承接文档**省略 `docs/` 前缀与 `-v0.1.md` 后缀以简化阅读。
- 本矩阵为 V0.1 首版，重点保证"易漏细则"全部有锚点；后续可随 PRD 迭代补全更细的页面/字段/状态列。

## 一、门票与购票（original §4）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-01 | **员工不能用平板代客购票**，门票必须客人自己买 | ticketing-entitlement-checkin-product-prd §2/§10；ticketing-pass-entitlement-development-design §2.1/§7.2；business-decision-log §1/§11；operation-traceability PRD 2.3「票务购买」 | 已覆盖 | 8+ 文档口径一致，与"平板可代客点酒"明确区分（ticket 覆盖小结） |
| REQ-02 | 官方 App 和小程序同步建设、共享口径 | business-decision-log §1；web-mobile-app-terminal-extension PRD；terminal-page PRD | 已覆盖 | — |
| REQ-03 | 第三方购票首批仅美团/大众点评/抖音团购回流 | business-decision-log §2；ticketing PRD §5；**system-breakdown §3.4（2026-06-07 已标注携程/旅行社为 P1+ 预留）** | 部分覆盖（设计层覆盖，代码/迁移未落地）·按统一口径 | **统一口径（2026-06-18 收口，三处统一）**：凡第三方一律预留接口 + 后台开关（默认 off）+ 不阻塞；P0 = 预留接口/开关/不伪造对账；真实渠道语义解析、`external_order_no`、双对账实做归 P1（对应 r5-spec-T14）。channel_ticket_mappings/external_order_no/导入端点迁移零命中（migration-claims REQ-03 absent），"两类订单"只承接自营单。ticket-6 已就地标注，消除携程超范围漂移 |
| REQ-04 | 门票多策略（平日/周末/节假日/多人/学生/新人/渠道优惠） | ticketing PRD §5 票务策略表 | 已覆盖 | 各策略具体落地见 REQ-05 |
| REQ-05 | **新人/学生/多人由后台设规则，符合自动判断、不符不可用** | unified-standards-baseline **D9**；business-decision-log **§32 A5**；ticketing PRD §5/§12 | 已裁决并覆盖 | 原"自动/人工/P1"三处冲突（ticket-1/2）已收口：新人=手机号自动判定+短信验证；学生=人工审核+学信网+凭证留痕表；多人=同场次集中核销+num_people 字段。执行口径以 D9/A5 为准。<!-- 修订说明（2026-06-08 三轮收口）：原"§32 A9"/"以 D9/A9 为准"中的 A9 指向已被占给"合规专章"，门票资格 P0=A5（见 decision-log §32 编号更正），两处 A9→A5。依据 finding residual-scan-5、D9/A5 --> |
| REQ-06 | 门票二维码防伪、防篡改、事后核对 | ticketing PRD 防伪票码；terminal-display-layer-spec-v0.2；ticketing dev-design §6.2.3；ticketing-database-implementation-notes；迁移 `002_ticketing_entitlement_settlement.sql` | 部分覆盖（设计层覆盖，代码/迁移未落地） | 见 R5 横幅 / migration-claims REQ-06 absent。<!-- R5（2026-06-18 业务HTML审计）：生效迁移 V009 ticket_credentials 仅有 credential_hash（SHA-256）+ nonce + expires_at，anti_fake_code_hash/signature_version/签名版本在 V001–V065 与代码均零命中；TicketVerificationService 无 HMAC/签名，核销端不校验签名/nonce/刷新窗口。002_*.sql 为旧手工迁移非生效实现。原"已补签名版本"不实。待业务确认 P0 防伪基线（hash-only vs HMAC 签名）。 --> 基础哈希防伪在，签名版本化未实现。 |
| REQ-07 | 二维码按票种有效期生效（日/周/月/年票） | ticketing PRD §5；business-decision-log §6 | 已覆盖 | — |
| REQ-08 | **二维码 30 秒或 60 秒刷新、截图无效** | business-decision-log §6（"30 秒或 60 秒刷新，截图无效"）；ticketing dev-design §6.2.3 | 已覆盖 | 易漏细则，全目录固定值一致（ticket 覆盖小结） |
| REQ-09 | 门票二维码必须在线核验、P0 不做离线核验 | business-decision-log §6；ticketing dev-design 核销校验 | 已覆盖 | — |

## 二、通票、权益与会员积分（original §5）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-10 | 通票=酒吧/游乐/酒吧+游乐/含餐饮零售折扣组合 | ticketing PRD §6；business-decision-log §4；business-operations-layer-spec-v0.2 | 已覆盖 | 五处口径一致（pass 覆盖小结） |
| REQ-11 | 餐饮/零售为第三方，通票该部分主要是优惠/折扣 | business-decision-log §4；ticketing PRD §6.1；third-party PRD §3/§9 | 已覆盖 | 已显式区分折扣型权益（discount，仅记录优惠资格/折扣口径）与券核销型权益（voucher，P1 接券核销级回流），餐饮/零售真实 POS 仍归第三方。 |
| REQ-12 | 官方 App/小程序会员积分系统、规则后台定义 | ticketing PRD §7.5；business-decision-log §3；foundation-database-design §6B.5；business-operations 五件套 | 已覆盖 | 真实表为 `customer_points_*`（V060 `customer_points_rules` 规则引擎已落，claims.json present）；字面 `points_rules`/`point_gifts` 按收敛口径不建（claims.json absent），矩阵应引用真实表名。规则有效期/过期方式按 D7 对齐。见 migration-claims REQ-12 |
| REQ-13 | 积分可与消费、会员等级、兑换礼物相关 | ticketing PRD §7.5；foundation-database-design §6B.5；data-risk-ai §7.1 | 已覆盖 | 跨业态消费入账通过 `points.*` 标准事件与积分明细承载；会员等级在 `customers`/`points_accounts.member_level` 落点。 |
| REQ-14 | **积分兑换礼物需扣减库存** | business-decision-log §3；supply-chain PRD；foundation-database-design §6B.5.5；supply-chain-inventory-development-design §6.13 | 已覆盖 | 礼物/库存挂 `customer_redemption_catalog_items`（V060，claims.json present，正式承接 point_gifts 语义，补 value_amount_cent/project_id）；字面 `point_gifts` 表不建（claims.json absent）。兑换成功扣积分、扣减后写 stock_movements（redeem_consume）进入供应链与成本/对账。见 migration-claims REQ-14 |

## 三、酒吧经营（original §6）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-15 | 酒吧可独立成酒吧 SaaS | bar-business-operations PRD；operation-traceability PRD §5（终端开关 SaaS 化） | 已覆盖 | — |
| REQ-16 | 酒吧合同/合作形式（租赁/联营/自营/合作）与收益模式 | platform-governance PRD 合同；business-decision-log §6 | 已覆盖 | 合同摘要由平台治理管理 |
| REQ-17 | 桌台/卡座/吧台位/最低消费/点酒/支付/赠饮/账单/异常/存酒 | bar PRD §5-§9；bar-operations-development-design | 已覆盖 | — |
| REQ-18 | **最低消费按时间/卡座/位置配置，原则上不按人数** | business-decision-log §16；bar dev-design §5.2.2；bar PRD §11 | 已覆盖 | P0 明确不按人数计算，`guest_count` 仅记录；按人数例外作为后置扩展。 |
| REQ-19 | 平板只做点酒下单，必须绑定酒吧+卡座、低消未满不能提交 | bar PRD §7.4；android-device-terminal dev-design；business-decision-log §11 | 已覆盖 | — |
| REQ-20 | 下单生成二维码扫码支付，或平板扫客付款码 | bar PRD 支付处理；bar dev-design §6.10 | 已覆盖 | — |
| REQ-21 | **吧台买酒优先线上支付，但必须支持现金和 POS（中国法律）** | business-decision-log §5/§16；bar dev-design §6.10/§13/§14；operation-traceability PRD §4.1 `payment.cash_received`/`payment.pos_received` | 已覆盖 | 酒吧开发设计已将现金/POS 纳入 P0，记录收款人、POS 流水号，并进入账单、日结、对账和事件中心。<!-- R2 修订（2026-06-18 代码审计 r2-code-audit-req26b-req21-req30）：经代码核实，收款人字段 cashier_staff_id、POS 流水号 pos_transaction_no 及事件 payment.cash_received/pos_received 在 backend 中零命中（真实缺口 P1）；聚合支付意图已复用 finance_payment_intents 同表但旁路 FinanceService，channel 无枚举无映射。"记录收款人/POS 流水号"为设计声明，代码尚未落地。 --> |
| REQ-22 | **按杯销售按标准出酒量/容量扣减，超标记损耗，损耗严重核查** | business-decision-log §16；bar dev-design §6.6/§6.16 | 已覆盖 | （2026-06-07 收口，broken-ref-6）bar dev-design §6.6 已新增 standard_pour_volume/pour_volume_unit/loss_threshold、§6.16 已新增 actual_pour_volume/loss_volume（bar-2），原"缺字段"结论已过时<!-- R3 修复（2026-06-18 P0-3，V058）：此前 dev-design 声称的字段在迁移中零命中（R3 确认）；V058 已真正落库 bar_products.standard_pour_volume/pour_volume_unit/loss_threshold 与 bar_order_items.actual_pour_volume/loss_volume，出品按标准量计损耗、超阈值发 inventory.loss_exception 核查留痕。matrix-migration-claims 已置 present。 --> |
| REQ-23 | 门票赠饮/权益领取与酒吧库存消耗打通 | bar PRD §2/§9；bar dev-design §7.8/§9.4；ticketing dev-design §10.4；p0-end-to-end-acceptance-and-edge-cases §3/§4 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 设计口径：核销后生成 0 元兑换点单、出品时扣吧台库存（见 migration-claims REQ-23 absent/DRIFT）。<!-- R5（2026-06-18 业务HTML审计）：bar/ticketing 间贯通逻辑零命中，BarOrderService 无 entitle/redeem/gift 触发；bar_production_ticket_events(V029) 无 inventory_movement_id，赠饮→0元点单→出品扣减链路未实现，端到端无验收用例。claims.json 已标 DRIFT。 --> |
| REQ-23b | 酒吧入场/售酒年龄核验强制人脸或等效设备；供应商未定时只能 mock，生产不得正式通过 | compliance-and-personal-information-protection-requirements §2；bar PRD §7.1/§9；bar-operations-development-design §10.1/§15；android-device-terminal-development-design §4/§14.5/§16.1；third-party-supplier-integration-kit-v0.1；p0-end-to-end-acceptance-and-edge-cases §3 | 已覆盖 | 属后续总控合规收口补充项。P0 先保留供应商适配器、mock、字段映射占位和生产阻断；正式上线必须接入第三方人脸/年龄核验接口或等效设备。 |

## 四、订台（original §7）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-24 | 订台入口=App/小程序/一体机/销售工作台 | business-decision-log §5；bar PRD；terminal PRD | 已覆盖 | — |
| REQ-25 | **订台需支付定金** | business-decision-log §5；bar dev-design §6.3/§13.1 | 已裁决并覆盖（P0） | （2026-06-07 收口，newdocs-2）bar-operations §6.3/§13.1 已裁决订台+定金为 **P0**（bar_reservations 与订金规则 P0 落地、补订金支付/到店抵扣流转），前后端口径一致；定金金额/比例/退没收规则以 bar dev-design §13.1 为权威（kiosk-1/2） |
| REQ-26 | 订台记录来源/销售或设备来源/定金/到店/取消/退款 | bar dev-design §6.3 bar_reservations；operation-traceability PRD 2.3「订台和到店」；customer-app §4.3 | 已覆盖 | `bar_reservations` 已补 `order_source`、`created_terminal`、`sales_staff_id`、`device_id`、`deposit_rule`、`deposit_status` 等来源与定金字段。 |
| REQ-26b | 销售人员售卖卡座按消费额 15% 提成，非销售人员按 10%；不含通票 | unified-standards-baseline D13；bar PRD §4/§9/§11；bar dev-design §6.9A/§15；finance-settlement PRD §3/§10；p0-end-to-end-acceptance §3 | 已覆盖 | 新增 `sales_person` 岗位；P0 生成卡座提成可复核台账或应付/内部薪酬依据，复杂多人分摊/阶梯提成/薪资发放后置。<!-- R2 修订（2026-06-18 代码审计）：代码已落 bar_commission_calculations 计算台账 + 15%/10% 费率（BarBillingService.java:330-333，测试通过），满足 D13"可复核台账"分支，P0 合规；但未贯通 finance ar_ap_items（employee_commission source_type 零命中），进入应付为 P1 可选增强。 --> |

## 五、供应链、库房与存酒（original §8）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-27 | 供应链含供应商/合同/供应合同/采购周期/库房管理 | supply-chain-inventory PRD §3/§4；supply-chain-inventory-development-design §6/§12 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 设计齐全，落地不全（见 migration-claims REQ-27 warehouses absent）。<!-- R5（2026-06-18 业务HTML审计）：真实迁移仅 inventory_suppliers（缺资质/结算方式表）、inventory_purchase_requests（仅"申请"create，无审批/到货/入库流转）、inventory_stock_balances.location_id 自由文本（warehouses/warehouse_type 未建表）。采购周期与库房模型未闭环，待业务确认 P0 范围。 --> |
| REQ-28 | **供应合同三模式：平台自用/商户自采/平台统一采购分销** | supply-chain PRD；business-decision-log §6；supply-chain-inventory-development-design §6.2 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 见 migration-claims REQ-28 supply_contracts absent；R5 裁决本期 P0 建表（三模式+采购周期+结构化库房）。<!-- R5（2026-06-18 业务HTML审计）：supply_contracts 表与 contract_mode 字段在 V001–V065 迁移零命中，开发设计 §6.2 为"有设计无实现"。此为用户原始 P0 需求，待业务确认是否本期落地。 --> 设计声明 contract_mode 三模式，但 supply_contracts 表未建。 |
| REQ-29 | P0 供应链从酒类/吧台消耗/客人存酒/常用消耗品开始 | business-decision-log；supply-chain PRD §P0 范围 | 已覆盖 | 常用消耗品=纸巾/色盅/色子/杯子 |
| REQ-30 | **客人存酒用标签（优先瓶底），按标签数值/拍照/人工备注记录** | bar PRD §4；business-decision-log §16；bar dev-design §6.11 saved_bottles | 已覆盖 | `saved_bottles` 已补 `bottle_label_value`、`customer_name_or_nickname`、`registration_photo_refs`、`manual_note`，登记必填并用于取酒核身。<!-- R2 修订（2026-06-18 代码审计 r2-code-audit-req26b-req21-req30）：与真实迁移不符。V012 saved_bottles 实际列仅 bottle_name（非 bottle_label_value）、通用 evidence_attachment_ids（非 registration_photo_refs），无 customer_name_or_nickname/customer_phone/manual_note 列；取酒 claimSavedBottle 仅校验 verificationCode 非空、不做姓名/标签比对、无人工复核状态（InventoryService.java:226-227）。此为设计声明，代码/迁移未落地，真实缺口 P1。 --> |
| REQ-31 | 存酒需留客人手机号和姓名/昵称 | bar PRD §4；bar dev-design §6.11 | 已覆盖 | `customer_phone` 与 `customer_name_or_nickname` 均已落表，散客无会员时可用于核身。 |
| REQ-32 | **高价值报损/盘点差异/存酒作废必须拍照和视频留证 + 主管复核** | business-decision-log §16；bar dev-design §5.4.4/§5.4.5/§5.3.4；supply-chain-inventory-development-design §5.4/§6.11 | 已覆盖 | 高价值判定已落 `is_high_value/high_value_threshold` 与平台默认阈值，报损/作废/盘点触发照片+视频双证据和主管复核。<!-- R2 修订（2026-06-18 代码审计）：就 saved_bottles 存酒作废而言不成立——saved_bottles 表无 is_high_value/high_value_threshold 列，cancelSavedBottle/voidSavedBottle 方法不存在，'cancelled' 状态无写入分支，SupervisorReviewService 仅服务 manual_release/bill_waiver 未挂存酒。存酒侧高价值双证据为真实缺口 P1；库存物料侧高价值阈值是否落地需另核。 --> |
| REQ-33 | 人工放行/免单/退款/调账按可配置层级主管复核 | platform-governance PRD §12；business-decision-log §25/§26；**p0-openapi-operation-acceptance-matrix §3（`listSupervisorReviewPolicies`/`evaluateSupervisorReview`/`approveSupervisorReview`）** | 部分覆盖（设计层覆盖，代码/迁移未落地） | 审批阶梯字段见 REQ-58。<!-- 2026-06-18 E1 补登（消除契约↔溯源漂移）：主管复核三操作 listSupervisorReviewPolicies（查询复核/阻断策略）/evaluateSupervisorReview（评估是否需复核）/approveSupervisorReview（审批或驳回）已进 OpenAPI 契约与验收矩阵 §3（P0-待归类，权限 governance.supervisor_review.view/manage/approve），落 supervisor_review_decisions（V057）。状态口径与验收矩阵一致=契约层已覆盖、代码/迁移落地按本矩阵 claims.json 复核口径。此前溯源矩阵 0 命中。 --> |

## 六、全业务过程留痕（original §9）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-34 | **留痕不只物品交接：凡有过程的业务动作都必须留痕** | operation-traceability PRD §2（唯一权威）；business-decision-log §31；p0-action-event-evidence-matrix-v0.1 | 已覆盖 | 三层模型 business_process_instances/step_records/service_responsibility_records（trace 覆盖小结）；P0 事件证据矩阵补齐“每个关键动作留什么证据”。 |
| REQ-35 | 订单获取/销售归属留痕（销售是谁、谁下单） | operation-traceability PRD 2.3/2.4；java-backend 事件 `order.acquired`/`order.sales_attributed` | 部分覆盖（设计层覆盖，代码/迁移未落地） | 销售归属字段 service_responsibility_records.sales_staff_id 已落（V059）；但事件名 `order.acquired`/`order.sales_attributed` 代码零命中（claims.json DRIFT），事件命名口径与代码漂移。事件命名以 PRD 4.1 为唯一权威（D4），待代码对齐。见 migration-claims REQ-35 / R5 spec |
| REQ-36 | 卡座服务留痕（谁服务、加单、送达、换人） | operation-traceability PRD 2.4 服务责任链/6.6 | 已覆盖 | — |
| REQ-37 | 异常第一处理人留痕 + 转派/升级/复核/关闭/退款/免单/调账 | operation-traceability PRD 2.4 异常/财务责任链；foundation-database-design §6.12；API first-handle | 已覆盖 | `service_responsibility_records` 已补 `refunded_by`、`waived_by`、`approved_by` 财务责任段字段。 |
| REQ-38 | 酒从库房→订单核验→机器人/人工配送→后厨→服务员送达全链路扫码 | operation-traceability PRD §3.2 流程/§3.3 节点 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 见 R5 横幅 / migration-claims REQ-38。<!-- R5（2026-06-18 业务HTML审计）：inventory_trace_handovers 为单条平面记录，scanTraceCodeHandover 把 handover_result 写死 accepted，无出库核验→配送→后厨接收→服务员取走→送达分段状态机，无缺口/重复/差异异常处理。"全过程链可追责"塌缩。 --> |
| REQ-39 | 酒入库贴码或出库打印码，二维码串责任链 | operation-traceability PRD §3.1/§6.1 inventory_trace_codes | 部分覆盖（设计层覆盖，代码/迁移未落地） | 矩阵/设计明确声称 inventory_trace_codes 表，迁移从未建（claims.json absent，DRIFT），trace_code 仅为 inventory_trace_handovers 共用列。贴码/打印码独立码表未落地。见 migration-claims REQ-39 |
| REQ-40 | **优先自动留痕、尽量避免人工填写**，人工只补原因/照片/视频/签名 | operation-traceability PRD §2.2/§6.5；foundation-database-design §6.11；p0-action-event-evidence-matrix-v0.1 | 已覆盖 | 已补 `capture_source` 来源类型，区分扫码、支付回调、机器人、企微、第三方、MDM、系统规则和人工补录，可统计自动留痕率；矩阵明确高风险动作证据要求。 |
| REQ-41 | 留痕与监控视频后续一一对应，P0 不强依赖真实视频 | operation-traceability PRD §2.5 视频关联策略 | 已覆盖 | P0 仅留时间/点位/设备推导候选 |
| REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`（2026-06-07 已补）** + 4.1 `contract.blocked`/`contract.released`（已补） | 部分覆盖（设计层覆盖，代码/迁移未落地） | 权威事件 `contract.blocked`/`contract.released` 代码零命中（claims.json absent，实际为 `contract.lifecycle_changed`），且业务域未在动作前调用准入校验、卡控不触发。R5 裁决：统一走 supervisor_review，业务动作内嵌准入校验本期 P0；admission_audits/contract_admission_rules 不建。见 migration-claims REQ-42 / R5 spec |

## 七、机器人与配送（original §10）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-43 | 酒从库房倾向机器人配送，**真实调度/厂家联动可后置** | operation-traceability PRD §8（机器人真实状态回调=P1，调度=P2） | 已裁决并覆盖 | **pboundary-4 已收口：android 原标 P1 联动已统一改 P2；交接扫码、责任链和状态留痕=P0**（见 p0-implementation-task-list §2） |
| REQ-44 | P0 必须记录机器人或人工配送交接留痕、各环节扫码 | operation-traceability PRD §3.3；business-decision-log §29 | 已覆盖 | — |

## 八、游乐系统（original §11）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-45 | 游乐可独立成乐园 SaaS | amusement PRD；operation-traceability PRD §5 | 已覆盖 | — |
| REQ-46 | **游乐容量 P0 按具体项目 + 具体场次/时段 + 点位/设备控制（三维度）** | business-decision-log §6；amusement dev-design §7.4/§4.2/§7.14b/§7.14c；amusement-p0-safety-inspection-spec-v0.1 §5/§9 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 设计声称 `amusement_capacity_config`/`amusement_capacity_snapshot` 三维容量表，迁移零命中（claims.json absent，容量仅场次级单维）；点位/设备维度未落地。见 migration-claims REQ-46 |
| REQ-47 | 一体机查看游乐开放状态/容量/异常/检查 | amusement PRD；terminal PRD；amusement-p0-safety-inspection-spec-v0.1 §5/§6 | 已覆盖 | P0 开放状态应包含安全巡检通过、警告、暂停、待恢复等可行动状态。 |
| REQ-48 | **游乐定期检修、检修单位作为供应商、工程师一体机填写、主管确认后恢复营业** | amusement dev-design §6.6.1/§7.14a；business-decision-log §32 引用；amusement-p0-safety-inspection-spec-v0.1 §6.3/§9 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 设计声称 `amusement_maintenance_record` 检修单表，迁移零命中（claims.json absent）；恢复营业审批落在 amusement_safety_inspections，检修单独立台账未落地。安全巡检闭环部分存在。见 migration-claims REQ-48 |

## 九、一体机（original §12）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-49 | 一体机触摸屏，可下单；并区分售票型与非售票型，售票型承担官方自助购票/取票/订单查询，非售票型承担订台/吧台买酒/权益领取/游乐状态/检修记录 | unified-standards-baseline D5；terminal-page PRD；android-device-terminal-development-design；ticketing-pass-entitlement-development-design；p0-end-to-end-acceptance-and-edge-cases §2 | 已覆盖 | 终端开关枚举仍为 `kiosk`，用 `kiosk_mode=ticketing/service` 或模块开关区分，不新增第 11 端；售票型必须复用票务价格、库存锁定、聚合支付、出票和防伪二维码 |
| REQ-50 | 一体机订台也需定金 | business-decision-log §5；bar dev-design §6.3/§13.1 | 已裁决并覆盖（P0） | 同 REQ-25：bar-operations §6.3/§13.1 已裁决订台+定金为 P0，定金优先级冲突已消除 |
| REQ-51 | 一体机动作记录设备/点位/时间/结果 | operation-traceability PRD §3.3/6.5 | 已覆盖 | — |

## 十、工作手机/私人手机/企业微信（original §13）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-52 | 工作手机核销门票（扫客人 App 二维码） | android-device-terminal dev-design §1/§2/§4 | 已覆盖 | — |
| REQ-53 | 工作手机扫附赠权益二维码→一体机展示可领取 | android dev-design §5.5/§14.5 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 设计声称已补 `entitlement_sessions` 会话表 + `entitlement_session_id` + 会话时效 + 防错领，迁移零落地（claims.json absent，DRIFT）。见 migration-claims REQ-53 |
| REQ-54 | **企微每日上传聊天、服务分析、违规检查；原始提出永久保存/不脱敏，现行裁决为可配置留存/分级查看** | business-decision-log §27；data-risk-ai dev-design §11.2/§11.5；compliance-and-personal-information-protection-requirements §4 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 裁决口径=可配置留存/权限脱敏/最小授权+审批/访问审计；见 migration-claims REQ-54 wecom_conversation_retention absent，R5 裁决平台侧合规能力本期 P0。<!-- R5（2026-06-18 业务HTML审计）：后端无会话上传接口、无留存表、无到期归档/销毁/匿名化触发、无脱敏专链、无查看访问审计，仅 V015 一行 mock 种子。属 PIPL 合规暴露，待业务确认是否本期合规底线。claims.json 此前未追踪此项。 --> |
| REQ-55 | 工作手机禁私装软件、仅企业微信、白名单网站，违规 MDM 阻止/封禁权限并留痕 | android dev-design §13.1/§13.4；android-terminal-breakdown §8.4；business-decision-log §14；compliance 要求 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 方案已拍板（Android Enterprise / Android Management API + 商业 MDM）；但自有侧字段 `management_status`/`policy_status`/`last_policy_sync_at`/`device_credential_hash` 在 terminal_devices 缺失（claims.json absent），MDM 阻断依赖外部商业 MDM、不计闭环。R5 裁决：MDM 按第三方原则预留+开关。见 migration-claims REQ-55 / R5 spec |
| REQ-56 | **私人手机 App 注册/通知/状态/设备凭证绑定/Root 检测，不做现场业务** | unified-standards-baseline **D8**；business-decision-log **§32 A6**；android dev-design §3/§9 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 裁决口径=Keystore 设备公钥/不可逆指纹，不读 NFC；见 migration-claims REQ-56 staff_profiles/staff_device_credentials/employee_notices absent，R5 裁决员工三表+Android 端凭证本期 P0。<!-- R5（2026-06-18 业务HTML审计）：员工设备凭证体系零落地——staff_profiles/staff_device_credentials/employee_notices 三表 SQL 零命中；现存 terminal_device_credentials(V011) 是 kiosk 一体机随机 token 的 SHA-256，与"员工私人手机 Keystore 指纹"是不同实体；Android 端仅一个健康检查 Activity，无注册/指纹/Root 检测/换机解绑。 --> |
| REQ-57 | 换机先解绑旧再绑新；Root/调试/异常判不可信失效复核 | business-decision-log §32 A6；android dev-design | 已覆盖 | 绑定对象由"手机 NFC"改为"设备凭证"（D8） |
| REQ-57b | **员工进办公区私人手机寄存，寄存柜流动使用、先到先选、离开拿走** | business-decision-log §11/§32 引用；android dev-design §3/§9/§13；project-progress-dashboard §6.1 已确认 | 已覆盖 | 已确认规则：不绑定固定柜，先到先选，离开全部拿走；P0 可人工记录/确认寄存与取走，P1 可接门禁/储物柜/考勤。 |

## 十一、员工、权限、职级与薪资（original §14）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-58 | **权限单一功能最小颗粒度（一个功能=一个权限点）** | foundation-database-design permissions 表；java-backend access 模块；business-decision-log §26 | 已覆盖 | 示例 ticket.verify.execute（权限编码 dot 风格，D3） |
| REQ-59 | 权限完全自定义（可增可减），可按角色、模板、员工单独配置 | foundation-database-design §3.4-3.13/§3.13.1-3.13.5；p0-operation-permission-matrix-v0.1；unified-standards-baseline D11 | 已覆盖 | 已补 `user_permission_overrides` 和权限模板表组，支持基础模板、临时模板、复制模板、单次有效和 `grant/deny`；最终权限按 D11 计算。 |
| REQ-60 | **职级 M1-M10 并分上/中/下** | business-decision-log §32 引用；platform-governance field-dictionary §8.1/§9.1；foundation-database-design §3.2.1 | 已覆盖 | `staff_levels` 已拆 `main_level`、`sub_level` 并派生 `level_rank` 1..30，支撑 M1-M10 × 上中下完整序列。 |
| REQ-61 | 职级只给薪资大范围，**具体薪资单独填写** | platform-governance field-dictionary §9.1；foundation-database-design §3.2 | 已覆盖 | 已补 `base_salary_amount`、`salary_currency`、`salary_detail`，职级只提供 `salary_policy_summary` 大范围。 |
| REQ-62 | 职级影响默认权限模板与审批层级，但不能替代单一功能权限 | business-decision-log §26；product-requirements §12 已确认规则 30；platform-governance PRD §12；platform-governance field-dictionary §8.1/§9.1；p0-operation-permission-matrix-v0.1 | 已覆盖 | 已在总纲和平台治理 PRD 补强禁止性约束：职级只影响默认模板、审批层级建议和薪资大范围，不能替代单一功能权限点，不能绕过具体功能授权。 |
| REQ-63 | 员工私人手机 App 注册提交→后台审核通过→生成角色 | platform-governance PRD 员工注册审批 | 已覆盖 | — |

## 十二、平台治理、合同与准入（original §15）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-64 | 基础平台管综合体/商户/合同/空间/区块/设备/人员/权限/终端开关 | platform-governance PRD §5；platform-governance field-dictionary §5/§7/§8；operation-traceability PRD §5 | 已覆盖 | 商场区块已作为楼层与区域之间的独立治理对象落到 PRD 与字段字典；P0 做区块层级，铺位级精细管理后置 P1。 |
| REQ-65 | 商户信息（合同审批/面积/租赁面积/商场区块/负责人/两个联系人/每平米租金/合作形式） | platform-governance PRD §5；field-dictionary | 部分覆盖（设计层覆盖，代码/迁移未落地） | 合作形式/面积/每平米租金/商场区块字段已落（V042，claims.json present）；但"负责人"无独立列，merchants 仅 contact_name+secondary_contact（claims.json owner_person absent，DRIFT），独立负责人字段未落地。见 migration-claims REQ-65 |
| REQ-66 | 商场区块最好有平面图 | platform-governance PRD；platform-governance field-dictionary §5；business-module-mature-system-gap-analysis §7.2 | 已裁决并覆盖（P1 预留） | 平面图来自用户原始需求；P0 保留区块/铺位与经营面积字段，平面图上传、多边形标注和铺位可视化后置 P1，避免阻塞 P0。 |
| REQ-67 | 合同核心摘要/基础定义由平台治理管理 | platform-governance PRD §5/§7 | 已覆盖 | — |
| REQ-68 | **合同/基础定义违规全部先卡住，允许更高级审核后放行** | platform-governance PRD §12；business-decision-log §25；operation-traceability PRD `contract.blocked`/`contract.released` | 已覆盖 | — |
| REQ-69 | **审批层级逐级上提（上一级/上两级/最高权限）** | platform-governance PRD §12；platform-governance field-dictionary §8.1；foundation-database-design §3.2.1.1/§2.9.2；**p0-openapi-operation-acceptance-matrix §3（`evaluateSupervisorReview`/`approveSupervisorReview`/`listSupervisorReviewPolicies`）** | 部分覆盖（设计层覆盖，字段名漂移） | 审批层级逐级上提逻辑已落 supervisor_review_decisions.approval_level（V057，claims.json present）；但本行原指向的 admission_audits.approval_level 表/列不存在（字段名过期），承接锚点应改指向 supervisor_review_decisions。见 migration-claims REQ-69<!-- R5（2026-06-18 业务HTML审计）：审批层级实际落 supervisor_review_decisions（V057），admission_audits 表/approval_level 列不存在；本行"在 admission_audits.approval_level 留痕"字段名过期，应改指向 supervisor_review_decisions。 --><!-- 2026-06-18 E1 补登：主管复核三操作 evaluateSupervisorReview/approveSupervisorReview/listSupervisorReviewPolicies 即审批层级判定与逐级上提的契约入口，主登于 REQ-33，本行交叉引用，消除契约↔溯源漂移。 --> |

| REQ-93 | **商户服务/物业工单：创建/查询/指派/处理动作/关闭全流程** | platform-governance PRD §5（商户治理）；p0-openapi-contract-closure-appendix §权限/事件（merchant.service_work_order.*）；**p0-openapi-operation-acceptance-matrix §3（`createMerchantServiceWorkOrder`/`listMerchantServiceWorkOrders`/`assignMerchantServiceWorkOrder`/`addMerchantServiceWorkOrderAction`/`closeMerchantServiceWorkOrder`）** | 部分覆盖（设计层覆盖，代码/迁移未落地） | <!-- 2026-06-18 E1 补登（消除契约↔溯源漂移）：商户服务工单 5 操作已进 OpenAPI 契约与验收矩阵 §3（P0-待归类；终端 web/mobile_admin/merchant_portal；权限 merchant.service_work_order.view/create/assign/process/close；数据范围 project-merchant；写接口 x-idempotency-required=true）。属平台治理对商户的服务/物业工单能力，原溯源矩阵无对应 REQ、0 命中，新建本行收口。状态口径与验收矩阵一致=契约层已覆盖；后端/迁移落地按本矩阵 claims.json 复核口径，工单表落地待 owner 核验。 --> 创建/查询/指派/追加处理动作/关闭五操作；契约层已覆盖，迁移落地待核验。 |
| REQ-94 | **商户结算账单查询（merchant-billing bills）** | finance-settlement PRD §3/§10；platform-governance PRD §5；p0-openapi-contract-closure-appendix（merchant.billing.*）；**p0-openapi-operation-acceptance-matrix §3（`listMerchantBills`）** | 部分覆盖（设计层覆盖，代码/迁移未落地） | <!-- 2026-06-18 E1 补登（消除契约↔溯源漂移）：商户结算账单查询 listMerchantBills 已进 OpenAPI 契约与验收矩阵 §3（终端 web/merchant_portal；权限 merchant.billing.view；数据范围 project-merchant；GET 非幂等）。原溯源矩阵无商户结算/账单专属 REQ（REQ-26b 仅卡座提成、REQ-27 仅供应链结算方式），0 命中，新建本行收口。状态口径与验收矩阵一致=契约层已覆盖；后端/迁移落地待 owner 核验。 --> 商户账单只读查询；契约层已覆盖，迁移落地待核验。 |

## 十三、第三方接入（original §16）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-70 | 第三方资料未定，先做自有系统再接入；分"获取/回写"与"接 SDK/API"两类 | third-party-integration dev-design；third-party-supplier-integration-kit-v0.1；business-decision-log §28；unified-standards-baseline D3 | 已覆盖 | 开放路径/权限编码以 D3 为准；供应商未确定前只保留接口、适配器、字段映射占位、Webhook+轮询校验、对账和补偿。 |
| REQ-71 | 餐饮/零售同时支持订单级和券核销级回流 | third-party PRD；business-decision-log §6 | 已覆盖 | — |
| REQ-72 | 看板支持日/半日/小时汇总，单店小数据量可实时 | data-analysis-layer PRD；business-decision-log §6 | 已覆盖 | — |
| REQ-73 | 美团/大众点评/抖音 P0 只做团购购票/回流/核销/对账 | business-decision-log §2；ticketing PRD | 部分覆盖（设计层覆盖，代码/迁移未落地）·按统一口径 | 同 REQ-03 统一口径（2026-06-18 三处统一）：P0 = 预留接口/后台开关（默认 off）/不伪造对账、不阻塞；真实渠道语义解析、`external_order_no`、双对账实做归 P1（r5-spec-T14）。 |

## 十四、终端开关与 SaaS 化（original §17）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | unified-standards-baseline **D5**；operation-traceability PRD §5.2/§6.3 | 已覆盖 | 10 端权威表 terminal_type；govfam-3 跨文档枚举对齐以 D5 为准 |
| REQ-75 | 关闭后菜单不可见/不可用，**接口拒绝业务动作并留痕** | operation-traceability PRD **§5.4（2026-06-07 补后端拒绝+`terminal.access_rejected` 留痕）** | 部分覆盖（设计层覆盖，代码/迁移未落地） | PRD 已写后端拒绝铁律；见 migration-claims REQ-75 terminal.access_rejected absent，R5 裁决后端拒绝铁律本期 P0。<!-- R5（2026-06-18 业务HTML审计）：代码实情——除 kiosk 售票配置一处外，finance/bar/ticketing/amusement/inventory 业务接口前无开关校验，可绕过；无拦截器/AOP/Filter；terminal.access_rejected 仅在 kiosk 设备凭证失败时触发，场景错位。后端拒绝未真正实现。 --> |
| REQ-75b | 开关拉取失败兜底 | operation-traceability PRD **§5.6 fail-closed（2026-06-07 新增）** | 已覆盖 | switch-5 已补 |
| REQ-76 | 终端开关用于酒吧/乐园未来 SaaS 化 | operation-traceability PRD §5.1/§9 | 已覆盖 | — |
| REQ-77 | **商户侧在授权业务范围内全开放** | operation-traceability PRD **§5.7 优先级裁决（2026-06-07 新增）**；business-decision-log | 已覆盖 | switch-6：高层级开关关闭压倒"全开放"，二者不矛盾；htmlmap-1 业务 map 卡片"只读"旧口径需改 |

## 十五、Web 前端与移动端（original §18）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-78 | Web 前端供业务人员非编辑查看，**P0 允许少量业务确认动作** | web-mobile-app-terminal-extension PRD；business-decision-log | 已覆盖 | — |
| REQ-79 | 客人 App/小程序：购票/预定/查询/出示二维码/会员积分/兑换 | customer-app-miniapp-development-design §4/§8；terminal PRD；web-mobile-app PRD；foundation-database-design §6B.5 | 已覆盖 | 会员积分和兑换下游表已建模（`points_accounts`、`points_ledger`、`points_rules`、`point_gifts`、`point_redemptions`），客人端页面与接口依赖已显式列出。 |
| REQ-80 | 移动管理端给主管看指标/待办/异常审批 | terminal PRD；terminal_type=mobile_admin（D5） | 已覆盖 | — |

## 十六、数据分析、风控与 AI 质检（original §19）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-81 | 看板看收入/客流/核销/退款/异常/商户表现/对账差异/库存/供应链/回流/企微质检 | data-analysis-layer PRD §4/§7 | 部分覆盖（设计层覆盖，代码/迁移未落地） | 见 R5 横幅；R5 裁决真实聚合+商户行级隔离+DTO 对齐本期 P0。<!-- R5（2026-06-18 业务HTML审计）：analytics 指标直读 analytics_* 快照表，值来自迁移手写 INSERT 种子（V017/V037，自标 manual baseline only），无 ETL/聚合/JOIN 业务表；前端总览 KPI 字段名与后端 DashboardSnapshotRecord 零交集，6 卡片全渲染 0；analytics 包 0 处 currentMerchantScope，无商户行级隔离。 --> |
| REQ-82 | **销售服务分析：订单来源/销售归属率/卡座服务责任完整率/送达确认率/异常首响时长** | data-risk-ai dev-design §5.9；data-analysis-layer page-list §7.3；field-dictionary §6.1；acceptance-checklist §3/§4 | 已覆盖 | 四项核心指标和服务责任变更次数已补分子分母、来源表、metric_code、页面、下钻和验收；指标名统一为"卡座服务责任完整率"。 |
| REQ-83 | 风控覆盖异常核销/库存/财务差异/企微违规/服务质量/设备策略违规 | data-risk-ai dev-design §10/§13.4/§13.5；evidence-risk-control PRD | 已覆盖 | `risk_rules.domain` 已补 `order/service/exception`；`risk_alerts.object_type` 已补 `table_session/service_responsibility`，责任链风控可准确下钻。 |
| REQ-84 | **AI 深度分析/语义质检/经营预测可后置** | unified-standards-baseline（P2/P3 口径）；p0-implementation-task-list §2 | 已裁决并覆盖 | **pboundary-1 已收口：企微 AI 质检及下游 AI 分析统一 = P2（消除 P2/P3 错位）；P3=AI 增强占位**。P0 只做基础规则质检、留痕和人工复核入口。 |

## 十七、文档与交付要求（original §20）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-85 | 生成可供分析判断的完整系统文档、可直接用于编码 | system-development-spec；system-capability-layers-product-index | 已覆盖 | — |
| REQ-86 | 文档按模块拆解，颗粒度足够编码 | 各 *-development-design；各层五件套；p0-implementation-task-list | 已覆盖 | P0/P1/P2 边界以 `p0-implementation-task-list.md` 与本矩阵收口状态为准；历史审计中的边界提示已在本轮产品流复核中更新。 |
| REQ-87 | **业务人员版 HTML 可点击说明页（给不懂技术的人看）** | business-system-map-for-operations.html | 已覆盖 | htmlmap-3：HTML 未标生成时间与依据 PRD 版本 |
| REQ-88 | **平台治理也需业务人员 HTML 拓扑页** | platform-governance-map-for-operations.html | 已覆盖 | xref-9：已补入 system-capability-layers 索引 §2 |
| REQ-89 | **所有项目内容需打包，方便发给其他机器智能体** | **delivery-package-manifest-v0.1.md（2026-06-07 新建）** | 已覆盖 | deliver-1 已修复，见交付包说明 |
| REQ-90 | 需求→文档逐条可追溯 | **本文档 requirement-traceability-matrix-v0.1.md（2026-06-07 新建）** | 已覆盖 | deliver-2 已修复 |

## 十八、多综合体与集团视图（original §2）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-91 | P0 围绕单综合体闭环，所有核心对象预留综合体项目归属 | platform-governance PRD；foundation-database-design | 已覆盖 | — |
| REQ-92 | 后续支持多综合体和集团视图，**优先级低** | product-requirements §12 已确认规则 27；operation-traceability PRD §8（集团级 P2）；多处治理文档 | 已裁决并覆盖（P2 预留） | P0 围绕单综合体闭环并保留 `project_id`；集团视图和跨项目汇总统一后置 P2，不阻塞当前编码。 |

## 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-06-07 | 增 | 新建本文档 | 为 original-requirements 每条口径编 REQ-01~REQ-92，逐条映射承接文档+章节、覆盖状态、备注，重点覆盖易漏细则（员工不可代购票、二维码刷新秒数、职级 M1-M10 上中下、企微留存、寄存柜流动、现金 POS、存酒瓶底标签等） | finding deliver-2、deliver-1；口径以 unified-standards-baseline 与 business-decision-log §32 为准 |
| 2026-06-07 收口 | 改 | REQ-22 | "部分覆盖·缺字段"更新为"已覆盖"，指向 bar dev-design §6.6/§6.16 已落地的按杯字段 | bar 已新增 standard_pour_volume 等字段，矩阵结论过时。依据 finding broken-ref-6 |
| 2026-06-07 收口 | 改 | REQ-25、REQ-50 | 订台定金覆盖状态由"有待对齐"更新为"已裁决（P0）"，指向 bar-operations §6.3/§13.1 | 本轮 bar 已裁决订台+定金为 P0，矩阵注解滞后。依据 finding newdocs-2 |
| 2026-06-08 Product Design 复核 | 改 | REQ-05/06/11/25/43/50/55/56/57b/62/64/66/79/82/83/84/92 | 将已修复但状态滞后的需求行更新为已覆盖、已裁决并覆盖或 P1/P2 预留；补 ticketing PRD §6.1 和 product-requirements §12 规则 30 | documentation-product-flow-audit-and-fix-report-20260608：旧审查状态未同步会误导外部智能体 |
| 2026-06-10 产品审计 V0.6 | 改 | REQ-49 | 补“一体机分售票型/非售票型，售票型承担官方自助购票/取票/查单”的追溯锚点 | 用户确认一体机有售票和不售票两种，售票型需承担售票职责 |
| 2026-06-10 卡座销售提成 | 增 | REQ-26b | 新增销售人员岗位和卡座提成追溯锚点 | 用户新增卡座销售提成规则 |
| 2026-06-18 D1 矩阵自洽收口 | 改 | 顶部口径声明；REQ-12/14/35/39/42/46/48/53/55/65/69 及 REQ-06/23/27/28/38/54/56/75/81 状态措辞 | 与 matrix-migration-claims.json 标 absent/DRIFT 矛盾仍写"已覆盖"的行下修为"部分覆盖（设计层覆盖，代码/迁移未落地）"，证据列追加 migration-claims 指针；REQ-12/14 更正为真实表名 customer_points_rules/customer_redemption_catalog_items；统一原"部分覆盖（R5）/未落地（R5）/已覆盖（字段名漂移）"为标准措辞 | 消除主表"已覆盖"与漂移区/claims.json/真实迁移的自相矛盾；承接状态以 claims.json + 真实迁移为准 |
| 2026-06-18 D3 团购回流三处统一 | 改 | REQ-03、REQ-73；R5 裁决回填第 2 条 | 第三方团购回流优先级与口径统一：凡第三方一律预留接口+后台开关（默认 off）+不阻塞；P0=预留接口/开关/不伪造对账；真实渠道语义解析、external_order_no、双对账实做归 P1（r5-spec-T14） | 与 business-decision-log §2、business-module-mature-system-gap-analysis 三处统一（口径来自 ticketing 设计 R5 业务裁决 verified） |
| 2026-06-18 E1 补登（消除契约↔溯源漂移） | 增/改 | 新建 REQ-93、REQ-94；REQ-33、REQ-69 补 operationId | 把本次 OpenAPI 契约新增的 9 个 operation 补登进溯源矩阵：商户服务工单 5 个（createMerchantServiceWorkOrder/listMerchantServiceWorkOrders/assignMerchantServiceWorkOrder/addMerchantServiceWorkOrderAction/closeMerchantServiceWorkOrder）→ 新建 REQ-93；listMerchantBills → 新建 REQ-94；主管复核 3 个（listSupervisorReviewPolicies/approveSupervisorReview/evaluateSupervisorReview）→ 主登 REQ-33、交叉引用 REQ-69。状态口径与 p0-openapi-operation-acceptance-matrix-v0.1.md §3 一致（契约层已覆盖=P0-待归类；代码/迁移落地按本矩阵 claims.json 复核口径，统一记"部分覆盖（设计层覆盖，代码/迁移未落地）"） | 消除"契约/验收矩阵已加、溯源矩阵 0 命中"漂移；只读参考 acceptance-matrix 确认状态口径 |
