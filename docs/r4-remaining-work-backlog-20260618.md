# 剩余工作总清单 R4（2026-06-18）

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

> 定位：把四轮审计（R1 文档审计 / R2-R3 代码核实 / 修复阶段）累积的**全部剩余可做事项**汇成单一清单，供排期。已完成项见 `r3-confirmed-gap-priorities-and-fix-backlog-20260618.md` §5（全部 P0：职级/薪资 V055、审批层级 V057、按杯损耗 V058、留痕三层模型 V059、积分规则/礼物 V060；两护栏）。
> 标记：可现做=自有系统现在即可编码+测试+过闸口；待外部=阻塞于供应商/IT 资料，现只能 mock；owner=非编码、需责任人回填。
> 机器标记：R4_REMAINING_WORK_BACKLOG_READY。

## A. P1 业务代码缺口（可现做，自有系统）

| 编号 | 项 | 来源 | 要点 |
|---|---|---|---|
| A1 | 存酒高价值/核身/手机号 | REQ-30/32, R2/R3, R1-SPEC-04 | `saved_bottles` 补 `is_high_value`/`high_value_threshold_cents`/`bottle_label_value`/`customer_name_or_nickname`/`registration_photo_refs`/`customer_phone`（可逆加密+哈希索引）；状态机增 `pending_identity_review`/`disputed_frozen`；`claimSavedBottle` 加姓名/标签比对→不一致落复核；新增作废接口 `cancelSavedBottle`（高价值强制 photo+video 双证据+主管复核） |
| A2 | 现金/POS 收款留痕 | REQ-21, R2 | `bar_bills`/支付侧补 `cashier_staff_id`/`pos_transaction_no`；发事件 `payment.cash_received`/`payment.pos_received`（D4 权威名） |
| A3 | 支付 channel 枚举+映射 | REQ-21/对账, R1-SPEC-02 | `finance_payment_intents.channel` 加枚举 CHECK；建 bar↔finance channel 映射表，支撑按 channel 对账归并 |
| A4 | 订台定金规则 | REQ-26, R3 | `bar_reservations` 补 `deposit_rule`（金额/比例/退没收规则） |
| A5 | 提成贯通 finance（可选增强）| REQ-26b, R1-SPEC-01 | 提成生成 `ar_ap_items`(source_type=`employee_commission`)；现 `bar_commission_calculations` 已满足 D13 P0，故为 P1 可选 |

## B. 留痕三层模型接线（可现做）—— 让 V059 产生运行态数据

| 编号 | 项 | 要点 |
|---|---|---|
| B1 | 业务动作接线 | 在核销/卡座服务/加单/送达/换人/异常首处理/退款/免单/调账各 service 内调用 `TraceabilityService.recordProcessInstance/recordStep/recordResponsibility`；用上 `process_type` 的 `bar_table_commission`/`saved_bottle_lifecycle` |
| B2 | 自动留痕率 KPI | REQ-40：基于 `capture_source`/`auto_captured` 统计自动留痕率，定 P0 阈值（建议≥80% 可配置）+ 统计页/告警 |
| B3 | 留痕时间线读取 | 把三层表并入 `JdbcEvidenceEventRepository.listTimeline` 的 UNION，复用现有 `GET /api/v1/evidence/timelines/...`（无新端点） |
| B4 | 三层表隔离登记 | 把 `business_process_instances`/`service_responsibility_records` 纳入 `domain-ownership-scope-audit-v0.1.csv` + 隔离规格 §3 + 补 AC-ISO-1/2 越权用例 |

## C. 核销/票务边界小项（可现做）

| 编号 | 项 | 来源 |
|---|---|---|
| C1（**已废止·见文首口径**，本条不再实施） | 核销人工放行状态 | REQ-05, R1-LOOP-10：票务核销记录状态机增 `manual_overridden`，对齐事件 `verification.manual_overridden` |
| C2 | 二维码刷新周期字段 | REQ-08, R1-LOOP-11：`ticket_credentials` 增 `refresh_interval_seconds`（约束 {30,60}，默认 60，票种级可配） |
| C3 | 多人票×容量交互 | REQ-46, R1-LOOP-12：定"全有或全无"——N 人一次核销，任一容量维度余量<N 整单拒绝 |
| C4 | 餐饮券权益容量承接 | REQ-11, R3：餐饮/零售券权益容量维度承接（P3） |

## D. OpenAPI / 契约文档补齐（可现做）

| 编号 | 项 | 要点 |
|---|---|---|
| D1 | BarProductRequest pour 字段 | 创建端点 schema 补 `standard_pour_volume`/`pour_volume_unit`/`loss_threshold`（可选字段，向后兼容） |
| D2 | 积分规则后台端点 | `customer_points_rules` 管理域端点（如 `GET/POST /api/v1/admin/points/rules`）+ 权限码 `customer.points_rule.manage`；走 verify:p0-contract 管线 |
| D3 | 留痕/责任链只读端点（可选）| 若 evidence-risk 页需独立服务责任链查询，新增只读端点；否则保持时间线读取 |

## E. 文档治理 / 护栏 owner 待办（可现做，纯文档/脚本）

| 编号 | 项 | 来源 |
|---|---|---|
| E1 | 重生成 audit-map CSV | `development-documentation-audit-map-v0.1.csv`（297 行）已滞后，漏 0618/full-admin-srs/R1~R4/V055-V060 等 |
| E2 | 重写进度看板 | `project-progress-dashboard.md` 停在旧计数（360 测试/181 op/P0-only）→ 重写为当前态（729 测试/209 op/V060/全 P0 完成）|
| E3 | 历史审计加注记 | gstack/handoff/p1-audit 等历史阶段审计加"历史快照、以 op-matrix 与 verify:* 实时为准"注记 |
| E4 | 挂入 S0 入口 | 把 R1~R4 交付物 + 0618 审计 + 数据隔离规格 + SMS 占位挂入 `00-current-effective-document-entry` |
| E5 | SMS 开关并入主矩阵 | `sms_notification` 并入 `integration-capability-switches-v0.1.md` + `-latest.json` + supplier-mock 样例（登录链路硬依赖 sms_code）|
| E6 | 修 examples 工具 | `npm i -D yaml`——`tools/apply-openapi-p0-examples.mjs` 缺 `yaml` 包无法重生成 yaml（本次 yaml 手工镜像）|
| E7 | 完成审计去重归档 | R1/R2 指出多份验收/完成审计高度重复，保留当前态、其余降级归档（不删）|

## F. 数据隔离工程待办（可现做）

| 编号 | 项 | 来源 |
|---|---|---|
| F1 | finding 台账补越权系列 | ISO-GAP-01（P0 安全）：`security-scans/findings-register-v1.md` 补 `SEC-*-TENANT-ISOLATION-*` |
| F2 | verify:tenant-isolation 闸口 | ISO-GAP-02：DB 行级隔离机审闸口（现只验 OpenAPI auth context）|
| F3 | 隔离规格补新表/新迁移 | 补 V053/V054/V055/V057/V059/V060 到隔离规格 §3 + CSV；改脚本自动生成（杜绝手抄漂移）|
| F4 | V049 amusement 索引 | ISO-GAP-03：V049 4 表 merchant_id 无索引 |
| F5 | merchant_id uuid→VARCHAR | ISO-GAP-05：dev-design 写 uuid 与迁移 VARCHAR 不符，修文档 |
| F6 | 旧行 scope 回填 | `merchant_id IS NULL` 存量行按归属回填 + VALIDATE NOT VALID 约束（V046 chk_stock_adjustment_type）|
| F7 | V044–V052 TDD 回归 plan | ISO-GAP-04：补回归 plan |

## G. 第四轮迁移级抽查（可现做，审计）

| 编号 | 项 |
|---|---|
| G1 | 对剩余"已覆盖"行做迁移级抽查：REQ-02/23/35/36/38/39/52/53/55/64/65 等，扩充 `matrix-migration-claims.json` present/absent 条目，盘全漂移 |

## H. 前端 / 终端（可现做，自有）

| 编号 | 项 |
|---|---|
| H1 | 完整后台 30 页 Playwright 补测（按 `full-admin-ui-acceptance-matrix`）|
| H2 | 新端点前端接线：职级/薪资（`/access/staff-levels`、`/users/{id}/staff-level`）后台页 |
| H3 | Android 工作手机/平板/一体机：扫码/核销/离线摘要按 OpenAPI 对齐（旧 `/api/mobile/*` 迁移） |
| H4 | 客人 App/小程序/Web 积分/兑换页对齐 customer_points_* 真实表 |
| H5 | **`verify:full-admin-matrix` 闸口待绿（2026-06-27 新增，剩 4 项）**：4 个工作台页缺 Playwright e2e —— `ADM-CONT-001`（合同生命周期工作台 `/admin/contracts`）、`ADM-FAC-001`（楼宇运营工作台 `/admin/facility/workbench`）、`ADM-ENE-001`（能源工作台 `/admin/energy/workbench`）、`ADM-ACC-003`（访问治理工作台 `/admin/access/governance`）。这 4 页前端已在 `apps/web/src/admin/adminRoutes.ts`、后端已落地（V162/V165-168/V169），CSV↔adminRoutes↔bespokePages 已对齐（`dcf2004`，闸口 12→4）；只需在 `tests/e2e/full-admin-mvp.spec.ts` 或 `tests/e2e/full-admin-remaining.spec.ts` 补这 4 个 pageId 的 e2e 用例，`verify:full-admin-matrix` 即 4→0 转绿。属真实前端 e2e 工作，需前端单独排期（区别于 H1 的泛化 30 页补测）。|

## I. 外部依赖/供应商联调（待外部资料，现只能 mock，不可宣称生产）

聚合支付 / 团购售票 / 企业微信 / 人脸年龄核验 / MDM / 对象存储 / 开放平台 / 餐饮POS / 零售POS / 机器人 / 视频监控 / 闸机NFC扫码 —— 详见 `integration-capability-switches-v0.1.md` 与 `r1-pending-supplier-information-backfill-checklist-20260618.md`。现可继续做：adapter/mock/字段映射占位/失败场景/Webhook+轮询；不可：真实控制/收款/核验。

## J. 生产前置（owner 回填，非编码）

NAS 自动挂载/定时备份/告警/恢复演练；生产凭证轮换台账；MDM 纳管；HTTPS；安全基线证据 —— 详见 `production-blocker-register-v0.1.md` 与 `r1-pending-supplier-information-backfill-checklist-20260618.md`。

## 实施进展（2026-06-18）

✅ **已完成本轮**：
- **E6**：`npm i -D yaml` 修复 `apply-openapi-p0-examples.mjs`（现可重生成 json+yaml）。
- **E2/E3/E4/E5/E7/F5**（后台代理）：进度看板重写当前态；3 份历史审计加历史快照注记；S0 入口登记 R1~R4 + 新规格；`sms_notification` 并入能力开关矩阵 + latest.json + 验收；audit v0.2 去重注记；foundation-database merchant_id uuid→VARCHAR 注记。
- **G1**（后台代理）：第四轮迁移级抽查 REQ-02/23/35/36/38/39/52/53/55/64/65；新发现 4 处硬漂移（`inventory_trace_codes` 表、`entitlement_sessions` 会话、device 封禁状态机、`order.acquired/sales_attributed` 事件名）+ REQ-23 赠饮→库存链路功能漂移；已并入 claims registry（26 present / 10 absent 守卫）。
- **A1 存酒**（V061）：`saved_bottles` 补 is_high_value/high_value_threshold_cents/bottle_label_value/customer_name_or_nickname/customer_phone/customer_phone_hash；状态机增 pending_identity_review/disputed_frozen；取酒核身（标签不一致→人工复核不出酒）；SHA-256 手机号哈希检索。全套 **730 测试通过**、verify:p0-contract EXIT0、verify:matrix-vs-migration PASS。

✅ **第二批完成（2026-06-18）**：
- **E1/F1/F3-doc/F7**（后台代理）：audit-map CSV 补登 16 行新文档；findings-register 补 9 条 SEC-*-TENANT-ISOLATION-*；数据隔离规格 §3 补 V055/V057/V059/V060/V061 表 + §6 ISO-GAP-06（敏感字段加密）+ ISO-GAP-04 TDD 回归 plan；domain-ownership-scope CSV 重生成（158 表）。
- **A2/A3 现金POS+channel（V062）**：新建 `bar_bill_settlements`（settle_channel 统一词表 + cashier_staff_id + pos_transaction_no），settle 时发 `payment.cash_received`/`payment.pos_received`（D4 权威事件）；channel 与 finance 线下方式对齐可对账归并。全套 **731 测试通过**、verify:p0-contract EXIT0、matrix-vs-migration PASS(158表/26present)。

✅ **第三批完成（2026-06-18）**：
- **A4 订台定金规则（V063）**：`bar_reservations` 补 `deposit_rule`，threaded 全部 7 个 record 构造点 + JDBC（含 BarReservationRequest 向后兼容构造器）。全套 **731 测试通过**、matrix-vs-migration PASS（158表/30present）、p0-contract EXIT0。
- **C1/C3 评估后延后（需专门一轮）**：C1 核销 manual_overridden——`ticket_verifications.status` 无 CHECK 约束，"状态值"非阻塞，原判「真实缺口是无人工放行流程」**已废止·见文首口径**：系统不提供人工放行，该缺口取消、不再实施；C3 多人票×容量——`num_people` 在票务代码零命中，是**跨域逻辑+前置建模**项。二者非"小项"，单列为流程/逻辑专项。

✅ **第四批完成（2026-06-18）**：
- **C2 二维码刷新周期（V064）**：`ticket_credentials` 补 `refresh_interval_seconds`（CHECK {30,60}，默认 60，票种级可配）。
- **A5 提成贯通 finance（代码）**：卡座销售提成（有归属销售人员+金额>0）生成 `ar_ap_items`(source_type=`employee_commission`, payable, source_id=commission.id)；非销售/0 元仅保留计算台账（D13"或"）。
- 全套 **732 测试通过**、p0-contract EXIT0、matrix-vs-migration 158表/30present。

✅ **第五批完成（2026-06-18）**：
- **F4 amusement 隔离索引（V065）**：amusement_safety_templates/projects/safety_notices/incidents 补 merchant_id 部分索引（ISO-GAP-03 闭）。
- **A1 存酒作废接口**：`InventoryService.cancelSavedBottle`——高价值存酒作废强制照片+视频双证据（≥2 附件），状态→cancelled；仅 stored/pending_identity_review/disputed_frozen 可作废；含跨商户校验。
- **B1 留痕接线（代表性）**：`BarOrderService.deliverOrder` 出品送达写 `service_responsibility_records`（经 TraceabilityService，merchant 由 token 注入）——V059 三层模型开始产生运行态数据。
- **C1 人工放行（已废止·见文首口径，不再实施）**：原拟 `TicketingService.manualOverrideVerification`→记录 status=`manual_overridden`，现取消。
- 全套 **734 测试通过**、p0-contract EXIT0、matrix-vs-migration 158表/31present、domain-ownership 158表 PASS。

## 仍待项与状态（可执行规格收口，按性质分类）

| 项 | 性质 | 状态 / 收口 |
|---|---|---|
| B2 自动留痕率 KPI | 后端分析 | 待做：基于 step_records.capture_source/auto_captured 统计自动留痕率，阈值≥80% 可配 + 统计页（analytics 域加 metric + 端点）|
| B3 留痕时间线读取 | 后端 | 待做：三层表并入 evidence timeline UNION（复用现有 timeline 端点，无新端点）|
| B4 三层表隔离 AC-ISO 用例 | 测试 | 待做：business_process_instances/service_responsibility_records 补 AC-ISO-1/2 越权用例 |
| B1 全量接线 | 后端跨域 | 部分完成（仅出品送达）；待扩展到核销/加单/换人/异常首处理/退款/免单/调账 各 service |
| C3 多人票×容量 | 跨域+建模 | 待做：num_people 票务零命中，需先建模多人票核销字段 + amusement 容量"全有或全无"校验（ticketing↔amusement）|
| C4 餐饮券容量 | P3 | 低优先，后置 |
| D1 BarProductRequest pour 字段入 OpenAPI | 契约文档 | 待做：openapi-p0.json BarProductRequest schema 补 standard_pour_volume/pour_volume_unit/loss_threshold（可选字段）+ 重生成 matrix/closure |
| D2 积分规则后台端点 | 契约+端点 | 待做：customer_points_rules 管理域端点 `GET/POST /api/v1/admin/points/rules` + 权限码 customer.points_rule.manage |
| D3 责任链只读端点 | 契约+端点 | 可选：evidence 责任链查询端点 |
| C1/A1-cancel 端点暴露 | 契约+端点 | 待做：manualOverrideVerification、cancelSavedBottle 的 controller+OpenAPI+权限码（现为 service 层）|
| F2 verify:tenant-isolation 闸口 | 工具 | 待做：以 domain-ownership CSV/data-isolation §3 为源，机审每张 direct/inherited 租户表有隔离测试覆盖 |
| F6 旧行 scope 回填 | 数据/owner | 待资料：merchant_id IS NULL 存量行按真实业务归属回填（数据负责人执行）|
| A1 手机号 at-rest 加密 | 安全基建 | ISO-GAP-06：customer_phone 明文→可逆加密；需密钥管理基建（安全/IT）|
| A1 高价值作废主管复核 | 流程 | 待做：作废接 supervisor_review（现为双证据硬门槛）|
| H1-H4 前端/终端 | 前端栈(apps/web)+Android | 待做：完整后台 30 页 Playwright 补测、职级/薪资等新端点前端接线、Android 旧路径迁移、客人端积分页对齐——独立技术栈，单列前端批次 |
| I 外部供应商联调 | 待外部资料 | 阻塞：聚合支付/团购/企微/人脸/MDM/对象存储/开放平台/餐饮零售POS/机器人/视频/闸机——现仅 mock，不可宣称生产 |
| J 生产前置 | owner 回填 | 阻塞：NAS/凭证/MDM/HTTPS/安全基线证据，非编码 |

> 说明：本轮已把全部"可在后端自有系统一次性落地的高价值缺口"完成到测试绿。剩余项分三类：①需 OpenAPI 端点管线/前端栈的暴露层（D、C1/A1 端点、H）；②大型跨域建模（C3）；③阻塞于外部资料/安全基建/owner 数据的（F6、ISO-GAP-06、I、J）。各项已给可执行规格，可逐一接续，不再阻塞当前后端闭环。

## 建议优先顺序（可现做部分）
1. **B1 留痕接线**（让 V059 产生运行态数据，REQ-34/37 的真正价值）→ 2. **A1 存酒**（P1 业务闭环最大块）→ 3. **A2/A3 现金POS+channel**（中国法律+对账）→ 4. **C1/C2/C3 核销小项**（低成本收口）→ 5. **E1-E5 文档治理**（消除滞后/挂入口）→ 6. **F1-F3 隔离收口**（安全）→ 7. **G1 第四轮抽查**（盘全剩余漂移）→ 8. **D1-D2 契约补齐** → 9. **H 前端** → 外部/生产前置按资料到位推进。
