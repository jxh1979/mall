# 统一口径基准 V0.1（跨文档单一权威）

> **本文档为什么新增（定位）：** 两轮审计发现大量"同一口径在多份文档各写一套"的冲突（金额单位、事件命名、终端枚举、状态枚举、开放平台路径/权限编码、风险等级、能力层数、NFC 方案、门票资格 P0…）。逐文件重复修改极易再次发散。本文把这些跨文档共用口径**集中为唯一权威源**，其余文档**引用本文**而非各自定义。凡与本文冲突的旧表述以本文为准。
>
> **依据：** 《文档审查与完善报告 V0.1》系统性问题 S4/S8 + 决策项 A1–A9（部分 2026-06-07 已由用户拍板，下标 ✅）。

---

## D1. 金额单位（✅ 采纳推荐）
- 全局金额一律 **整数分（bigint）**，禁止 `numeric(18,2)` 元。
- 影响：`java-backend §7`、`foundation-database §1`、`data-risk-ai §13` 与全部字段字典金额字段须统一标注"单位：分(整数)"。
- 依据：devtech-3、fielddb-7。

## D2. 能力层模型（✅ 6 层权威）
- 采用**总纲 §8 的 6 层**：平台治理 / 业务经营 / 第三方接入 / 证据留痕与风控 / 数据分析 / 终端展示。
- **财务为"数据分析层下的财务/对账子域"，非独立层**。业务拓扑图的"财务经营层"应标注为子域。"财务经营层/财务结算层"术语统一为"财务经营（子域）"。
- 依据：caplayer-1/2/3、caplayer-8。2026-06-10 后续拍板补充：财务经营子域 P0 必须支持完整应收应付台账，但仍归入数据分析/财务经营子域，不新增独立能力层。

## D3. 开放平台对外规范（✅ 已确认）
- **内部业务 API 路径**统一使用 `/api/v1/{domain}/...`；正式 P0 机器契约为 `docs/openapi/openapi-p0.yaml` 和 `docs/openapi/openapi-p0.json`，operation 追溯事实源为 `p0-openapi-contract-closure-appendix-v0.1.md`。历史文档中的 `/api/admin/*`、`/api/mobile/*`、`/api/bar/*`、`/api/supply/*`、`/api/integration/*` 只可作为页面依赖或旧草案说明，不得直接作为正式后端实现路径。
- P0 OpenAPI 写接口必须绑定字段级 requestBody schema，不得使用 `GenericWrite` 作为正式编码输入；operation 级验收以 `p0-openapi-operation-acceptance-matrix-v0.1.md` 为准，operation 到权限、事件、任务簇、终端和样例准入的追溯以 `p0-openapi-contract-closure-appendix-v0.1.md` 为准。`npm run verify:openapi-closure` 未通过时，不得进入 P0 联调、SDK/mock、接口自动化或供应商联调。
- OpenAPI `x-permission` 只允许填写权限矩阵权限码，或 `public`、`authenticated`、`not_applicable` 保留标记；所有 operation 必须显式声明 `x-auth-context`，允许值为 `public`、`staff-token`、`customer-token`、`device-credential`、`open-app-credential`、`webhook-signature`。客人 Token、设备凭证、开放平台应用凭证、Webhook 签名不得伪装成权限码。
- OpenAPI `x-terminal-allowed` 只能使用 D5 的 10 个终端码。门禁核销设备、人脸/年龄核验设备、监控探针、服务端回调等不是终端类型，必须分别写入 `x-device-capability` 或 `x-invocation-channel`，不得新增 `gate-device`、`authorized-age-device`、`server-to-server` 等伪终端。
- **对外路径前缀**统一：鉴权 `/api/open/auth/token`；动态业务 `/api/open/d/{endpoint_key}/{domain}/...`；回调 `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}`。票务/酒吧**不得**各自定义 `/openapi/ticketing/token` 等鉴权端点，必须复用地基开放鉴权端点。
- **权限编码**统一 **dot 风格** `domain.resource.action`（如 `openplatform.app.manage`），废止 colon 风格 `open_app:create`。
- **鉴权时间窗** 300 秒；**nonce TTL** 600 秒（窗口 2 倍，容时钟偏移），单位明确为秒。
- **开放应用状态机**以 `open-platform-security-auth §5.1` 的 **7 态**为准（含完整流转），大小写统一；`foundation-database open_apps.status` 与之对齐。
- **Webhook/轮询策略**：P0 上线通用 Webhook 接收、签名、幂等、重试日志，并配合定时轮询校验；供应商 SDK/API 到位后再做字段映射、对账口径和异常补偿细化。
- **第三方双向开放**：平台同时支持“第三方调用平台统一 OpenAPI/Webhook”和“平台通过供应商 SDK/API 适配器调用外部成熟服务”。供应商未确定前，只固化接口契约、适配器边界、字段映射占位、Webhook+轮询校验、对账和补偿策略；不得把供应商私有字段写死进票务、酒吧、财务等业务主模型。
- 依据：devtech-1/2/7/8/10。

## D4. 事件命名（✅ 单一事件字典）
- **唯一权威事件字典 = `operation-traceability-and-terminal-switch-prd §4.1`**；`java-backend §14` 与 `foundation-database` 事件清单与之保持一字一致。
- 酒吧/安卓**不得**另起 `bar.sales.attributed` 等命名；统一用 `order.sales_attributed`、`table.service_assigned`、`table.service_changed`、`bar.order_delivered`、`exception.first_handled` 等点+下划线命名。
- **分析域（data-risk-ai）只消费平台标准事件名**，禁止用 `payment.success` 等不存在的事件名；支付用 `payment.cash_received` / `payment.pos_received`。
- 依据：tracewire-1、trace-2、devtech-6。

## D5. 终端类型枚举（✅ 10 端权威表）
以 `terminal_capability_switches.terminal_type` 为唯一权威，**10 端**：

| code | 含义 |
|---|---|
| `app` | 客人 App |
| `miniapp` | 小程序 |
| `web` | Web 前端 |
| `tablet` | 工作人员平板 |
| `staff_work_phone` | 员工工作手机 |
| `staff_private_phone` | 员工私人手机 App |
| `merchant_portal` | 商户侧 Web 门户 |
| `merchant_app` | 商户侧 App |
| `kiosk` | 触摸屏一体机 |
| `mobile_admin` | 移动管理端 |

- 不得用"员工端"笼统合并工作手机/私人手机；不得漏"移动管理端"。`platform-governance` 验收/页面/字段字典、`java-backend`、`terminal-display-layer` 全部对齐本表。
- 触摸屏一体机只有一个终端码 `kiosk`，不得新增 `ticket_kiosk` 等第 11 端；售票型/非售票型用 `kiosk_mode=ticketing/service` 或模块开关表达。售票型承担授权服务人员辅助到店购票/取票/订单查询，绑定酒吧门口/前台时只能售卖本酒吧单店门票；非售票型不显示购票入口。
- OpenAPI、权限矩阵、终端开关、前端路由、Android/一体机客户端必须使用本表 code，不得使用 `admin-web`、`work-phone`、`merchant-web`、`customer-web` 等历史别名。
- 依据：switch-1/2、govfam-3。

## D6. 风险等级中英映射（✅）
- 统一四级并给映射：**低=notice，中=suspicious，高=high，严重=critical**。
- 数据分析层"高风险事件数"口径需明确是否含 critical（建议 high+critical 合计为"高及以上"，并单列 critical）。
- 依据：analytics-8。

## D7. 状态枚举中英映射原则（✅ 原则）+ 关键对象权威状态集合
- **原则：** 每个字段字典状态表必须给"中文 ↔ 英文 code"两列；数据库 CHECK 取值与之逐项对应。建议另建一张全局"中文状态↔英文 code"映射表作为唯一桥接。
- **关键对象权威状态集合：**
  - 商户：与 `merchants.status` 对齐；试运营使用 `trial` 或本地字段字典已确定 code；结算冻结用独立 `settlement_status`，不得塞进商户经营状态。
  - 桌台（6 态）：空闲/已预留/已开台/待清台/停用/异常 → idle/reserved/opened/pending_clear/disabled/abnormal；落 `bar_tables.business_status`，不得用点位 active/inactive 顶替。
  - 账单（6 态）：待结/部分支付/已结/部分退款/已退款/异常 → unpaid/partial_paid/settled/partial_refunded/refunded/abnormal；需建 `bar_bills` 表。
  - 核销（6 态）：成功/拒绝/人工放行/已撤销/待复核/异常 → success/rejected/manual_override/reversed/pending_review/abnormal；统一"已撤销"用词。
  - 游乐项目：补 unopened（未开放）、abnormal（异常）；maintenance 保留为长期停机检修中，临时停用/故障通过 paused/abnormal 与设备级 maintenance 表达。
  - 订单/权益：在 ticketing 迁移显式列 CHECK 取值并与字段字典 §11 对齐（含"已部分使用/已冻结/异常"）。
  - 供应商：新增 `suppliers` 表及含黑名单的 5 态，与开放平台 `service_providers`（技术服务商）区分。
  - 第三方接入：接入关系状态与 `open_apps` 鉴权状态分开建模；前者表达业务合作/供应商接入关系，后者表达开放应用鉴权状态。
- 依据：fielddb-1/2/3/4/5/9/10/11/12/13。

## D8. 员工设备身份方案（✅ 采纳"设备指纹/Keystore凭证"）
- **废止**"安装时读取手机本机 NFC 唯一标识"（Android 技术不可行，phone-3）与"实体 NFC 工卡"方案。
- **私人手机身份 = 安装期生成并存于设备 Keystore 的设备公钥 / 不可逆设备指纹**作为唯一可信标识；换机=先解绑旧设备凭证再绑新；Root/调试/异常判不可信失效复核。
- **仅持久化不可逆哈希/公钥指纹**，不存原始标识（PIPL 数据最小化，compliance-4）。
- 原 `staff_nfc_bindings` / `nfc_uid_hash` 字段改为 `staff_device_credentials` / `device_key_fingerprint`（或保留表名但语义改为设备公钥指纹）。工作手机通过摄像头/扫码模块读取**外部**客人二维码；NFC 仅读取外部 NFC 腕带、标签、卡或设备中的权益标识。
- 依据：phone-3/7、compliance-4、A6（2026-06-07 用户拍板"仅手机设备指纹/Keystore凭证"）。

## D9. 门票资格 P0 口径（✅ 用户拍板）
- **新人优惠：** 以**手机号**为准；用户注册必须登记手机号并**短信验证**。同一手机号未购买/未享受过即判新人，**系统自动判定**，不符不可用。
- **学生票：** **人工审核**（证件 + 学信网核验）；规则与凭证留痕表 P0 落地（含 verify_method、credential_ref、verified_by、result、verified_at）。
- **多人票：** 必须**同一时段全部核销**（如同一晚同场次一次/集中核销，不可拆到不同日期/场次）；`product_entitlement_template`/`issued_entitlement` 增 `num_people`，核销记录放行人数与时段约束。
- **退款：** P0 客人侧按订单/票发起，内部按权益逐项冲正与金额拆分；复杂退款规则引擎 P1。支付通道已确认聚合支付优先，并保留直连接口抽象。
- 需同步消除新人票"自动/人工/P1"三处冲突，以本条为准。
- 依据：ticket-1/2/3/4、A5（2026-06-07 用户拍板）。

## D10. 技术栈路线（✅ 用户拍板：迁移到 Java）
- **正式技术栈 = Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi**。
- 进度、路线、总规格、开发设计、数据库迁移、构建脚本、CI 和验收文档均以正式 Java 栈为准；`project-progress-dashboard` 与 `p0-implementation-task-list.md` 为后续实施入口。
- TypeScript / SQLite / Prisma / Fastify 后端仅可作为历史预研原型，不得作为正式开发、测试、部署、安全审计或供应商交付依据；活跃目录中的相关脚本、示例和安全扫描应迁入归档或标注 legacy。旧后端目录 `apps/api` 必须保留 `LEGACY-PROTOTYPE.md` 提示，且其运行、构建、Prisma、seed 和 smoke 脚本必须保持阻断状态，禁止删除提示或恢复脚本后继续按旧后端开发。
- 正式后端入口为 `backend/`；仓库根目录 API 相关脚本不得重新指向旧 `apps/api`，否则视为 P0 入口回退缺陷。
- `apps/web` 与 `apps/android-terminal` 中仍存在的旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*`、`/api/auth/*` 调用只可作为预研原型和迁移参考；正式联调前必须按 `code-contract-difference-and-migration-audit-v0.1.md` 迁移到 OpenAPI 契约路径，并通过 `verify:p0-contract`。
- 依据：status-1/11、devtech-9、A1（2026-06-07 用户拍板）。

## D11. 权限模板与最终权限计算（✅ 已收口）
- P0 权限必须支持**基础模板、临时模板、模板复制、模板应用、到期失效、单次有效、撤销和审计**，不得要求实施人员逐个员工手工勾选所有权限。
- 基础模板用于长期岗位/角色预设；临时模板用于节假日、夜场、活动、临时支援和突发任务，可设置 `valid_from`、`valid_to`、`single_use`。
- 模板复制复制的是当时权限快照，复制后的新模板独立维护，不随源模板后续修改自动变化。
- 员工最终权限 = 角色权限 + 基础模板授权 + 有效临时模板授权 + 员工单独授予 - 员工/模板显式拒绝；显式拒绝优先级最高。
- 权限模板创建、复制、修改、应用、撤销、到期、单次消耗均必须写审计事件，并保留变更前后快照。
- 权限点细则以 `p0-operation-permission-matrix-v0.1.md` 为准。

## D12. 币种与金额口径（✅ 已收口）
- P0 仅启用人民币 CNY，所有金额仍按 D1 使用**整数分**存储和计算。
- 金额相关表允许保留 `currency` 字段，默认值固定为 `CNY`，用于未来扩展和第三方对账解释；P0 不启用多币种、汇率换算、外币结算。
- 支付通道、渠道或供应商返回非 CNY 时，P0 必须拒绝入账或进入异常复核，不得自动按未知汇率换算。
- 多币种、汇率表、外币应收应付和跨币种结算进入 P2。

## D13. 销售人员岗位与卡座提成口径（✅ 已收口）
- 新增岗位类型 `sales_person`（销售人员），用于卡座销售归属、客户维护、订台工作台和业绩/提成统计；岗位不等同职级，不能替代具体功能权限。
- 卡座销售提成仅针对**卡座/桌台对应酒吧账单的实收可计提消费额**，不含通票、票务权益、卡座预约入场名额、赠酒成本、退款、免单、抹零和不可计提调整。
- 若卡座销售归属人为 `sales_person` 岗位，默认提成率为 15%；若归属人为非销售人员，默认提成率为 10%。
- 一张卡座/一笔订台在同一结算周期只能有一个主销售归属；变更销售归属必须留痕并保留变更前后快照。
- 提成在 P0 生成可复核台账或应付/内部薪酬依据；实际工资发放、个税、复杂多人分摊和阶梯提成可后置。

---

## D14. 新功能统一术语（✅ 沉淀，权威指向 v099-v124-feature spec）`文档goalP2(2026-06-20)`

> 本节把 V099–V124 系列新功能的跨文档共用术语集中为唯一口径，**每条简明定义并指向 `docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md`**（含迁移号、字段、状态机、G/W/T）。术语真实性已经 grep 后端/迁移核实（见各条末"实证"）。

### D14.1 综合体计费术语（turnover rent / deposit / CAM）
- **turnover rent（营业额抽成）**：按商户营业额比例计提的租金，落账方向为应收（`direction=receivable`）；计费引擎产出计费台账行 `sourceType='turnover_rent'`。
- **deposit（保证金四动作）**：保证金台账动作集 = **collect（收取）/ refund（退还）/ deduct（抵扣）/ forfeit（没收）**，单行记一个 action；金额按 D1 整数分。
- **CAM（公摊费）**：~~公共区域分摊费用，按商户份额分摊计入应收。~~ **已废止（2026-06-22 F-OPS-08，V150）**：改为物业费「每平米固定单价 × 商户面积」按期计费、不退不补；`cam_allocations` 旧表只读历史、`allocateCam` 抛 `CAM_ALLOCATION_ABOLISHED`。本术语仅作旧口径历史参考，现行术语见「物业费固定计费」。
- **CAM true-up（年度多退少补）**：~~年度对预收 CAM 与实际成本做差额清算；少收 → 补应收、多收 → 退应付。~~ **已废止（同 F-OPS-08）**：物业费不退不补、无年度 true-up 对账；`cam_trueup_*` 旧表只读、`trueUpCam` 抛 `CAM_TRUEUP_ABOLISHED`。仅作历史口径。
- **库存差异入账科目方向（统一口径）**：盘亏/报损（内部损益对手方）→ **receivable**；盘盈（冲成本）→ **payable**；供应商责任（向供应商挂 AP 应付索赔）→ **payable**。差异行 `reason∈{damage/shortage/overage/supplier_fault}`，`varianceQty/varianceAmountCent` 带符号（负=盘亏/报损，正=盘盈）。
- 实证：`FinanceServiceTests`（turnover_rent / 保证金四动作 / CamTrueUp under/over/exact）、`DepositLedgerRecord`（action=collect|refund|deduct|forfeit）、`InventoryVarianceRecord` + `InventoryVariancePostRequest`（盘亏/报损→损益对手方、供应商责任→供应商 AP）。权威细化见 spec（P1#14 综合体计费 + P2#④ CAM true-up）。

### D14.2 游乐超容术语（硬上限 vs 软上限）
- **硬上限（安全容量，hard cap）**：安全容量上限，达上限**一律阻断**核销（F-OPS-03：人身安全红线，处置模式 `block_hard`/`warn_only` 均**不可旁路**，SESSION_CAPACITY_EXHAUSTED 口径，并触发自动停运）。
- **软上限（运营容量，soft cap）**：运营容量上限，达上限**告警放行**（不阻断）。
- **over_capacity（超容）**：无论硬上限阻断还是软上限告警放行，均落 `over_capacity` incident 并发 `amusement.incident.over_capacity` 事件（超容必留痕）；`disposal_mode` 仅作 incident 留痕标注，不改变硬上限阻断行为。
- 实证：`AmusementServiceTests`（安全容量=硬上限恒阻断且 `warn_only` 不可旁路、运营容量=软上限告警放行，均落 over_capacity 事件）。

### D14.3 积分状态机术语（pending → confirmed → deducted/reversed）
- **pending（支付入账未可用）**：支付成功即写 pending，**不计入可用余额**（pending 不可花）。
- **confirmed（履约可用）**：核销/完成履约后转 confirmed，计入可用余额与保级进度。
- **deducted / reversed（退款冲减）**：退款时按已 confirmed 比例冲减（如退款 60% 则按比例扣减对应积分）。
- 实证：`LoyaltyServiceTests`（earnPointsPending→confirmPoints→deductPoints 端到端：pending 不计余额、confirmed 计余额、退款按比例冲减）。

### D14.4 企微违规命中术语（wecom_risk_hits 命中等级 + 复核状态机）
- **wecom_risk_hits（企微违规命中表）**：企业微信会话违规命中落表；命中等级由**规则码 `rule_code` + `risk_level`** 表达；同一消息同一规则码幂等（唯一约束 `uq_wecom_risk_hits_message_rule`）。
- **复核状态机**：**open → reviewing → confirmed / dismissed**；复核写 `reviewer_id` 并发 `wecom.risk.hit_reviewed` 事件；命中同时入复核队列 `analytics_risk_alerts`（`source_domain='wecom'`）。
- 实证：`WecomConversationServiceTests`（open→reviewing→confirmed/dismissed）、`JdbcWecomComplianceRepository`（rule_code/risk_level、wecom_risk_hits 表）、`WecomComplianceJdbcIntegrationTests`（建表 + analytics_risk_alerts.source_domain 列）。权威细化见 spec（P0#8）。

### D14.5 多综合体隔离术语（project_id scoped 通道）
- **project_id scoped 读通道**：按 `project_id` 做行级隔离的只读通道（隔离闸口 AC-ISO-1，与 CSV `scope_source=project_id` 对齐）。
- **单综合体（单 project）退化口径**：单综合体下全部归 `DEFAULT_PROJECT_ID`，`project_id` 恒定，project 维 rollup = 平台口径合计（口径与现状一致，**不伪造多 project**）。
- **多综合体推广前置**：推广到多综合体前必须补 scoped 读（各域按 project_id 行级隔离，仅命中本 project 数据）。
- 实证：`JdbcTraceabilityRepository`（project_id 行级 scoped 读，AC-ISO-1）、`Customer/Wecom/Inventory/AmusementSafety JdbcIntegrationTests`（P2 多综合体 project_id scoped 读通道按行级隔离）、`AnalyticsServiceTests`/`DashboardAggregationJobNetRevenueTests`（单综合体单 project rollup 不伪造）。

### D14.6 异常责任链术语（risk_alerts 5 态 + 关闭必须有结论）
- **risk_alerts 5 态状态机**：**open → handling → escalated → resolved / dismissed**（领取/转派均落 handling，升级落 escalated，终态为 resolved 或 dismissed）。
- **关闭必须有结论**：进入终态（resolved/dismissed）必须带处置结论；dismissed（如误报）须写结论说明，不允许无结论关闭。
- 升级（escalate）允许从 open/handling → escalated；resolve/dismiss 允许从 open/handling/escalated 进入。
- 实证：`RiskAlertRecord`（状态机注释 open→handling→escalated→resolved/dismissed）、`RiskAlertService`（escalate/resolve/dismiss 的 requireStatusIn 闸口）、`RiskAlertServiceTests`（handling/escalated 流转 + exception.escalated/exception.resolved 事件）。

## D15. 全局金额精度/并发/时间/留痕/导出/通知口径（✅ 用户 2026-07-22 拍板）

> 本节沉淀 2026-07-22 用户在 IR 铁律提取轮拍板的跨域全局口径，权威源＝三份最终解释权业务 HTML（业务系统地图/平台治理地图/业务枚举速查）同步条目。

- **D15.1 金额精度与分账尾差**：金额以**分（整数）为最小单位**（承 D1），对客展示两位小数；一切按比例拆分（通票分账、费用分摊、提成）四舍五入到分，**除不尽的尾差统一归项目公司/平台兜底、单列一笔留痕**，不并入任一方。
  - **落地入口**：`com.complex.platform.common.money.MoneyRounding`（HALF_UP·符号对称·不溢出）为全局唯一舍入/尾差 API；通票分账尾差落 `settlement_bills.rounding_residual_cent`（V343）。
  - **🔴 例外一（2026-07-28 用户拍板 D-20260728-1）**：**代扣个税**（分红代扣股息红利个税、薪资个税税表整除链）**保持向下截断**，与税局申报口径对齐，本条不适用。
  - **🔴 例外二**：结算超池两段拆 `pool_i` 与风险准备金释放额**保持向下取整**——该方向本身即「尾差归平台」且使「不得超总池 / Σ释放≤原留存」硬约束恒成立，改四舍五入反而破坏。
  - **补数式拆分不产生尾差**：凡另一半取 `总额 − 前一半` 定义的（分红劈分链、渠道费/池、留存/总池、服务费/净额）两侧相加恒等，无尾差可单列。
- **D15.2 并发/幂等**：并发冲突一律**以"先处理成功"为准**（转赠撤回/领取、支付回调、核销、充值入账）；同一操作重复提交须幂等，不得重复扣款/入账/核销。
- **D15.3 考勤时间基准**：考勤迟到早退等**按所在地（北京）时间**判定（替代原"以服务器时间为准"）；核销/二维码有效期等安全判定仍以系统权威时间防篡改。2026-08-02 复议对齐落码：当前只在一地经营，落的是全局所在地时区（`common/LocalZone`），不做点位级时区配置；将来真跨地区经营时再按门店/点位配时区（可逆）。
- **D15.4 留痕保存**：留痕记录默认永久保存、不自动到期删除；仅"手动清理留存数据"入口可删，须**最高权限＋删除留痕**，且交易类钱账数据（订单/核销/结算流水/账单）永不可删。
- **D15.5 导出脱敏与加密**：涉及法律要求的导出必须脱敏，法律无要求且不含敏感数据可不脱敏；**所有导出文件必须设打开密码**，密码按导出人账户算法派生（绑本人但非明文引用账户），本人可取回。
- **D15.6 金额变动短信提醒**：客人消费扣款、储值充值/扣减等金额变动须短信提醒本人，受后台短信开关可配（区别于转让的短信验证码确认）。
- **D15.7 最低消费绝对不按人数**：酒吧最低消费按时间/卡座/位置配置、**不按人数计**（2026-07-22 定案取消"原则上"活口，bar-design `by_headcount` 扩展点废弃）。

---

## 变更日志

| 日期 | 类型 | 内容 | 原因/依据 |
|---|---|---|---|
| 2026-06-07 | 增 | 新建本文档 | 把跨文档共用口径集中为单一权威源，消除"同一口径多套并存"（报告 S4/S8），并固化用户 2026-06-07 对 A1/A5/A6/A9 的拍板 |
| 2026-06-10 | 增 | 补 D3 第三方双向开放、D11 权限模板、D12 币种与金额口径 | 用户确认第三方双向接入、权限模板机制和币类默认按建议执行；防止后续 PRD/开发文档各自写散 |
| 2026-06-10 | 增 | 补 D13 销售人员岗位与卡座提成口径 | 用户确认新增销售人员岗位；销售人员售卖卡座按消费额 15% 提成，非销售人员 10%，不含通票 |
| 2026-06-11 | 增 | 补 D3/D10 可编码准入口径：字段级 OpenAPI、`x-auth-context`、旧 Web/Android 路径迁移闸口 | 用户要求直接修改到尽快可编码；防止旧原型和泛化接口继续误导正式 Java/Spring Boot 实现 |
| 2026-06-11 | 改 | 补 D3/D5 OpenAPI 终端枚举硬规则：`x-terminal-allowed` 只允许 D5 10 个终端码，设备能力和服务端调用分别写入扩展字段 | 对抗式审计发现 OpenAPI 中存在 `admin-web`、`work-phone`、`server-to-server` 等伪终端，可能导致终端开关和权限实现分裂 |
| 2026-06-12 | 增 | 补 D10 旧 API 脚本阻断和正式 `backend/` 入口规则 | 防止文档已废止旧 TypeScript/Fastify/SQLite 原型但命令入口仍回退到旧后端 |
| 2026-06-13 | 增/改 | 补 D3/D10 OpenAPI 追溯附录闸口：`verify:openapi-closure` 未通过不得进入 P0 联调、SDK/mock、接口自动化或供应商联调 | 防止旧路径清零后仍遗漏权限、事件、任务簇、终端或样例准入 |
| 2026-06-20 `文档goalP2` | 增 | 新增 D14 新功能统一术语：D14.1 综合体计费（turnover rent/deposit 四动作/CAM/CAM true-up + 库存差异科目方向）、D14.2 游乐超容（硬上限阻断/软上限告警 over_capacity）、D14.3 积分状态机（pending→confirmed→deducted/reversed）、D14.4 企微违规命中（wecom_risk_hits 规则码+risk_level，open→reviewing→confirmed/dismissed）、D14.5 多综合体隔离（project_id scoped 通道）、D14.6 异常责任链（risk_alerts 5 态 + 关闭必须有结论）。每条已 grep 后端/迁移实证，权威指向 v099-v124-feature spec | 把 V099–V124 新功能跨文档共用术语沉淀为单一口径，防止后续各文档各写一套（文档goalP2） |
| 2026-07-22 | 增 | 新增 D15 全局金额精度/并发/考勤时区/留痕保存/导出脱敏加密/金额变动短信 + 最低消费绝对不按人数 | 用户 2026-07-22 IR 铁律提取轮拍板 8 项跨域口径，同步三份最终解释权业务 HTML（10 处）；防止金额舍入/分账尾差、并发幂等、考勤时区、留痕保存期、导出脱敏加密、金额变动短信各文档写散 |
