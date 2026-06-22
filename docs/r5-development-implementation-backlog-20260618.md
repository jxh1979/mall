# R5 缺口开发实施任务清单与排期 Backlog（2026-06-18）

> **定位**：本文把 R5 业务 HTML 缺口审计 + 用户 10 题裁决转成可开发、可排期、可验收的实施清单。所有任务均为"文档已裁决、代码尚未实现"，落地后须把 `docs/matrix-migration-claims.json` 对应项由 `absent` 改 `present` 并下修 `requirement-traceability-matrix-v0.1.md` 的 R5 横幅。
>
> **裁决来源**：对话 R5 业务裁决回填（见追溯矩阵 R5 横幅）。**审计证据**：各业务域 PRD/设计文档 H1 后的"⚠️ R5 审计修正标注"块。
>
> **工程口径**：Java 21 + Spring Boot 4.x + PostgreSQL + Flyway（D10）；金额=整数分（D1）；状态枚举给中英映射 + 库 CHECK（D7）；事件命名用唯一事件字典点 + 下划线风格（D4）；10 端权威 terminal_type（D5）；租户隔离统一 `currentMerchantScope`（D11/数据隔离规格）。
> **当前最新迁移 = V065，新迁移从 V066 起连续取号**；迁移版本号与重叠 Java 文件是**串行共享资源**，只有非代码流可并发。验收闸口：`verify:matrix-vs-migration`、`verify:p0-contract`、`verify:openapi`、后端测试全绿。

---

## 一、优先级与波次总览（按依赖排序）

| 波次 | 目标 | 任务 | 并行性 |
|---|---|---|---|
| **W0 底座/共享闸口**（解锁后续，先做） | 建底座表、两类"动作前置闸口"、第三方开关骨架 | T1 员工三表、T2 能力开关 7 维 + 后端拒绝拦截器、T3 准入校验共享组件 | T1 可独立；T2/T3 共享拦截链需协调 |
| **W1 各域 P0 核心**（W0 后并行） | 各业务域核心闭环落地 | T4 二维码折中防伪、T5 多人票+冲正、T6 供应链三模式+采购+库房、T7 日结锁定+补录审批、T8 企微平台侧合规、T9 设备凭证 Android 端、T10 准入埋点、T11 开关埋点 | 跨域可并行；同域/同迁移串行 |
| **W2 展示与闭环** | 看板、前端权限、第三方骨架 | T12 看板聚合+隔离+DTO、T13 前端 effective-permissions、T14 第三方接口/开关预留 | 可并行 |
| **W1.5 审计另含 P0**（PRD 已 P0，无需再确认） | 审计发现但未进 10 题的硬 P0 链 | T15 赠饮核销→扣库存、T16 订台定金闭环、T17 扫码交接全链状态机 | 跨域可并行 |

依赖关系：T9←T1；T10←T3；T11/T14←T2；其余相对独立。**建议先打通 W0 三件底座，再 fan-out W1/W1.5。**

---

## 二、任务卡

### W0 — 底座 / 共享闸口

#### T1 员工三表（员工档案/设备凭证/通知）— 裁决⑨
- **迁移（V066 起）**：`staff_profiles`（员工档案，project_id + merchant_id 隔离、注册审批状态枚举）、`staff_device_credentials`（员工私人手机 Keystore 设备公钥/不可逆指纹哈希、绑定状态、supersedes 换机链、Root/调试失效标记）、`employee_notices`（通知/合规同意凭据，含写入方）。**明确与 kiosk `terminal_device_credentials`(V011) 是两套实体**。
- **Service**：注册→后台审批→生成角色；绑定/换机（解绑旧→绑新待审→生效）；Root/调试检测置失效进复核。
- **API/页面**：员工注册审批页、设备凭证台账；OpenAPI 同步。
- **事件**：`staff.registered`/`staff.approved`/`staff_device.bound`/`staff_device.rebound`/`staff_device.revoked`。
- **权限点**：`staff.profile.review`、`staff.device.manage`。
- **验收（GWT）**：Given 员工提交注册，When 后台审批通过，Then 生成角色与凭证；换机时 Given 旧凭证，When 绑新机，Then 旧凭证 superseded 且新机待审生效；Root 设备绑定即失效并进复核。
- **依赖**：无（底座）。**解锁**：T9。
- **claims 同步**：staff_profiles/staff_device_credentials/employee_notices → present。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T1-staff-tables-dev-design-20260618.md`（V066 DDL + 状态机 + Service + 9 端点 + 事件 + 权限点 + 验收 GWT + 测试计划）。

#### T2 能力开关 7 维模型 + 后端拒绝拦截器 — 裁决⑥（+ 支撑裁决③第三方开关）
- **迁移**：扩 `capability_switch` 支持 scope=全局/项目/商户/业务系统/模块/设备/版本（7 维 + CHECK）。
- **Service/拦截器**：新增统一 AOP/Filter，在 finance/bar/ticketing/amusement/inventory **业务接口前**校验开关，关闭则拒绝并写 `terminal.access_rejected`；拉取失败 fail-closed（§5.6）。
- **API/页面**：开关管理页支持 7 维配置。
- **事件**：`terminal.access_rejected`（修正为业务接口被拒场景，非仅 kiosk 凭证失败）。
- **验收**：Given 某端/模块开关关闭，When 调用其业务接口，Then 后端拒绝 + 留痕；前端隐藏仅作辅助。
- **依赖**：无（底座）。**解锁**：T11、T14。
- **claims 同步**：terminal.access_rejected → present（业务场景）。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md`（与 T3 合并：统一 @GuardedAction 切面闸口链；T2=开关 7 维优先级 device>…>global、默认开启、fail-closed、拒绝留痕）。

#### T3 准入校验共享组件 + supervisor_review 接线 — 裁决⑧
- **裁决**：**统一走 supervisor_review，不建 admission_audits/contract_admission_rules**。
- **Service**：把 `SupervisorReviewService.evaluate` 封装为可被业务域调用的前置准入组件（合同/空间/资质/权限超范围→阻断→按 level_rank 升级审批→放行留原因+责任人）；准入规则改为来自规则表/配置（替换硬编码 `List.of`）。
- **事件**：复用既有事件或统一命名（避免再发 `contract.blocked` 死名，向代码看齐 `contract.lifecycle_changed` 或补权威名——择一收口）。
- **验收**：超合同/超空间动作在任一业务域被阻断，可经配置层级审批放行并留痕。
- **依赖**：supervisor_review（V050/V057 已在）。**解锁**：T10。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md`（T3=supervisor_review_policies 外部化替换硬编码 List.of、三态 allow/warn/block、AdmissionGuard 组件、admission.* 事件收口 REQ-42/68 死名）。

---

### W1 — 各域 P0 核心

#### T4 二维码折中防伪 — 裁决①
- **迁移**：`ticket_credentials` 补 nonce 强校验所需字段、刷新窗口起止；联动票种级 `refresh_interval_seconds`(V064, {30,60})。
- **Service**：`TicketVerificationService` 核销端**强制校验**当前 30/60s 刷新窗口 + nonce 防重放（同 nonce/过窗口拒绝），使截图窗口内失效。**HMAC 签名版本化 → P1**（本期不做，留字段位）。
- **验收**：Given 过刷新窗口或重放 nonce 的二维码，When 核销，Then 拒绝；窗口内有效凭证可核销。
- **依赖**：无。**claims**：保持 signature_version=absent（P1）；新增刷新窗口校验落地后更新备注。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T4-ticket-anticounterfeit-dev-design-20260618.md`（刷新窗口校验+nonce 防重放唯一索引+核对维度 staff_id/merchant_id+签名 P1 预留；建议与 T5 合并迁移）。

#### T5 多人票核销 + 核销冲正 — 裁决②
- **迁移**：`issued_entitlements` 补 `num_people`；`ticket_verifications` 补 `verified_people`、`reversed` 状态。
- **Service**：多人票核销（一次核多人 vs 拆次，按 PRD 口径）；`POST /verifications/{id}/reverse` 冲正端点 + 结算冲正。
- **验收**：多人票按 num_people 校验核销人数；误核销可冲正并回滚权益/结算。
- **依赖**：建议在 T4 之后（同域同表）。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T5-multiperson-reverse-dev-design-20260618.md`（issued_entitlements.num_people+ticket_verifications.verified_people/reverse 关联+reversed 态 CHECK+冲正回滚联动；与 T4 合并迁移）。

#### T6 供应链三模式 + 采购周期 + 结构化库房 — 裁决④
- **迁移**：`supply_contracts`(+`contract_mode` CHECK: platform_self_use/merchant_self_purchase/platform_purchase_distribution、供货范围/价格口径/账期/有效期/审批状态/可采购范围)；采购周期状态机（申请→审批→下单→预计到货→实际到货→入库确认）；`warehouses`(+`warehouse_type`: 总库/酒吧库/吧台库/消耗品库/礼品库/存酒区)。
- **Service**：合同模式卡控可采购范围；采购全状态流转；库存挂结构化库房/库位。
- **验收**：三模式合同可配置并卡控采购；采购单可全状态流转；库存按结构化库房归集。
- **依赖**：无（供应链域较独立，工作量大，可单独排）。**claims**：supply_contracts/warehouses → present。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T6-supply-chain-dev-design-20260618.md`（supply_contracts 三模式+warehouses 六类型+采购周期状态机 submitted→…→received+inventory_purchase_request_events+域内 inventory_high_value_reviews）。

#### T7 日结锁定 + 补录审批 + 多源汇总 — 裁决⑤
- **迁移**：`ar_ap_adjustments`、`ar_ap_items.locked_day_close_id`、补录审批状态机；`finance_daily_closes` 补汇总金额字段。
- **Service**：锁定后硬拦截直改影响金额；补录需上级审批通过；日结聚合票务/酒吧/游乐/供应链/第三方流水。
- **验收**：Given 已锁定日结，When 直改影响金额，Then 被拒；走审批通过后可补录并留痕；日结金额=真实多源汇总。
- **依赖**：无。**未决**：聚合口径待财务确认（见第四节）。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T7-daily-close-lock-dev-design-20260618.md`（finance_daily_close_summaries 六源+locked_day_close_id 锁定拦截+ar_ap_adjustments 补录审批闭环+difference_type 四类 CHECK；前置：ar_ap/reconciliation 两套表 owner 收口）。

#### T8 企微平台侧合规能力 — 裁决⑥
- **迁移**：会话留存表 + 留存策略（可配置期限）、到期归档/销毁/匿名化、脱敏链、查看访问审计表。
- **Service**：会话同步接口（按第三方原则预留 + 开关）；`@Scheduled` 到期处理；查看不脱敏原文走最小授权 + 审批并写访问审计；普通展示按权限脱敏。
- **验收**：留存策略可配；到期触发归档/销毁/匿名化；查看原文产生审计记录。
- **依赖**：T2（开关）。**未决**：留存期限参数、企微授权配置 owner。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T8-wecom-compliance-dev-design-20260618.md`（wecom_conversations/messages/retention_policies(默认180天)/access_audits+到期归档销毁匿名化定时+脱敏 AiTextRedactor+原文最小授权审批审计+同步 webhook 受 T2 开关门控）。

#### T9 设备凭证 Android 端 — 裁决⑨
- **端侧**：Android App 注册、Keystore 设备公钥/不可逆指纹生成与绑定、Root/调试检测、换机解绑重绑；私人手机仅注册/通知/状态，不进现场业务。
- **依赖**：**T1（后端三表）**。**MDM 阻断**：按第三方原则预留 + 开关（不阻塞本任务）。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T9-staff-device-android-dev-design-20260618.md`（端侧 Keystore 设备凭证+不可逆指纹哈希+Play Integrity/Root 检测+换机 supersedes+仅注册/通知/状态；复用 TerminalApiClient/TerminalModels；MDM 自有侧暂缓）。

#### T10 业务动作埋点准入校验 — 裁决⑧
- **范围**：票务/酒吧/游乐/结算业务动作前内嵌调用 T3 准入组件。
- **验收**：四域关键动作前均触发准入校验，超范围被阻断。
- **依赖**：**T3**。**风险**：多域埋点工作量大，需逐域回归。

#### T11 业务接口埋点开关校验 — 裁决⑥
- **范围**：finance/bar/ticketing/amusement/inventory 业务接口接入 T2 拦截器。
- **依赖**：**T2**。

---

### W2 — 展示与闭环

#### T12 看板真实聚合 + 行级隔离 + DTO 对齐 — 裁决⑩
- **改造**：analytics 指标改为真实业务表聚合/ETL（替换 V017/V037 种子）；引入 `currentMerchantScope` 行级隔离；统一前后端 KPI DTO 字段名（修 6 卡片全 0）。
- **验收**：KPI 来自真实聚合且前端有值；商户登录只见自身范围；总览可下钻。
- **依赖**：各业务表（多数已在）。**未决**：聚合口径由财务/运营定义。
- **可编码规格（已出）**：`docs/r5-spec-T12-dashboard-aggregation-dev-design-20260618.md`（@Scheduled ETL 刷新 analytics_business_metric_snapshots 替种子+currentMerchantScope 行级隔离+metric_code map DTO 对齐修 6 卡全 0+下钻；口径用聚合草案）。

#### T13 前端 effective-permissions 渲染 — 裁决⑪
- **改造**：新增后端 effective-permissions 拉取接口；前端 `PermissionGate` 按后端 DENY 优先结果渲染。
- **验收**：前端权限渲染与后端 DENY 优先一致。
- **依赖**：access 模块（已在）。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T13-frontend-effective-permissions-dev-design-20260618.md`（后端新增 effectivePermissions(DENY 优先 grants/denies)+GET /access/effective-permissions+前端 PermissionGate 改读后端集替硬编码角色映射+DENY 优先+fail-safe）。

#### T14 第三方接口/开关预留骨架 — 裁决③通用原则
- **范围**：餐饮/零售 POS、机器人、视频监控、第三方售票、企微同步的接口/Webhook/字段映射/开关骨架占位；视频"点位+时间锚点"字段（是否本期建表见未决）。
- **依赖**：T2（开关）。**口径**：只做接口/开关/映射占位，真实回流/对账/被动留痕待厂商资料 + 开关开启后交付。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T14-thirdparty-scaffolding-dev-design-20260618.md`（统一四件套 webhook 受开关门控+integration_robot_task_events 被动留痕对接 T17+integration_video_anchors 点位时间锚点+调阅审计；不把占位当真实闭环）。

---

### W1.5 — 审计另含 P0（PRD 已 P0，无需 10 题再确认，勿遗漏）

#### T15 赠饮核销→0 元点单→扣吧台库存（REQ-23，PRD §126 已 P0）
- bar/ticketing 打通：核销赠饮权益事件→生成 0 元点单→出品扣库存；`bar_production_ticket_events` 补 `inventory_movement_id`。验收：赠饮核销后吧台库存相应减少且可对账。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T15-gift-redeem-stock-dev-design-20260618.md`（事件驱动 ticket.gift_verified→0 元 bar_order→redeem_consume 扣减→inventory_movement_id 关联+bar_gift_redemptions 幂等+冲正回补）。

#### T16 订台定金闭环（REQ-25 已裁决 P0）
- `createReservation` 落真实定金金额/状态（去硬编码 0）；到店抵扣、取消退/没收。验收：付定金可回查、到店抵扣、取消按规则退/没收。**已裁决**：后台可配固定额/比例 + 退没收时限可配。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T16-reservation-deposit-dev-design-20260618.md`（bar_deposit_rules 固定额/比例+cancel_free_before_minutes+定金支付/到店抵扣/取消退没收闭环+deposit_status CHECK）。

#### T17 扫码交接全链状态机（REQ-38/44 P0）
- `inventory_trace_handovers` 改多段状态机（出库核验→配送→后厨接收→服务员取走→送达）+ 缺口/重复/差异异常进复核（去单条平面 accepted）。验收：交接逐段可追责、缺口进复核。**已裁决**：员工扫码主动 + 机器人上报被动；缺口/重复/差异进复核。
- **可编码规格（已出）**：`docs/_archive/20260622-workspace-tidy/r5-r6-round-specs/r5-spec-T17-trace-handover-chain-dev-design-20260618.md`（表头+inventory_trace_handover_steps 五段链 staff/robot+inventory_trace_handover_exceptions gap/duplicate/diff 复核）。

> 另含 **P1**（不进本期 P0，登记备查）：通用异常处理人链路（risk_alerts 状态机/转派升级）、三层过程留痕 TraceabilityService 对外端点与串联、游乐检修联动 open_status + 现场权益校验、高价值报损/存酒作废复核接入 supervisor_review、联合通票跨商户权益归属。

---

## 三、排期建议

- **Sprint 1（W0 底座）**：T1 ‖ T2 ‖ T3。三者解锁后续，优先并发（注意迁移取号串行：T1/T2/T6/T7 各自占连续 V066+ 段，需协调避免版本冲突）。
- **Sprint 2（W1 跨域核心）**：T4→T5（票务同域串行）‖ T6（供应链，体量大可独立全程）‖ T7（财务）‖ T8（企微，依赖 T2）‖ T9（依赖 T1）。
- **Sprint 3（埋点 + W1.5）**：T10（依赖 T3）‖ T11（依赖 T2）‖ T15 ‖ T16 ‖ T17。
- **Sprint 4（W2 展示）**：T12 ‖ T13 ‖ T14（依赖 T2）。
- **并发约束**：跨域无重叠 Java 文件可并行；同迁移版本段、同域服务类串行。每个任务完成即跑 `verify:matrix-vs-migration`（absent→present 强制更新 claims）+ `verify:p0-contract` + 后端测试。

---

## 四、未决阻塞项 → 二轮裁决（2026-06-18）

| 原未决项 | 影响任务 | 二轮裁决 | 剩余未决 |
|---|---|---|---|
| 订台定金规则 | T16 | **后台可配：固定额与按最低消费比例两种都支持**，退/没收时限后台可配 | 赠饮触发方式（开发期定，建议事件推送） |
| 供应链高价值复核归属 | W1.5 复核接入 | **库存域内自建复核**（不走 governance 统一队列） | — |
| 扫码交接回调触发方/缺口追责 | T17 | **员工扫码主动留痕 + 机器人上报被动留痕**；缺口/重复/差异进复核 | — |
| 企微留存期限参数 | T8 | **后台可配，默认 180 天**；到期归档/销毁/匿名化 | 企微授权配置 owner |
| 空间模型 / project_id 隔离 | 跨域 | **保留扁平 spaces（四级模型作废）**，P0 全对象补 project_id + 隔离、修正 projects 归属方向 | — |
| 权限点首批预置范围 | T13、各域 | **全业务域最小颗粒度预置、实施阶段边配边补** | — |
| 日结/看板聚合口径定义人 | T7、T12 | **由本审计先起草默认聚合口径草案，待财务/运营复核修正定稿** | 财务/运营复核确认 |
| MDM 自有侧字段 + 验收降级签字人 | T9 MDM 预留 | **MDM 自有侧字段本期暂缓**（仅接口+开关占位） | MDM 缺供应商验收降级签字人 |

**剩余真正未决（待业务/owner）**：① 赠饮核销触发方式（T15，开发期可定，建议票务核销事件推送）；② 企微授权配置由谁完成（T8 上线前置）；③ 聚合口径草案的财务/运营复核确认（T7/T12）；④ 对账自动匹配排期（finance）；⑤ MDM 缺供应商时验收降级签字责任人（T9）。

> **新增交付物（已完成草案）**：聚合口径草案（日结多源汇总 + 看板七类指标）已起草 → **`docs/r5-aggregation-metrics-draft-20260618.md`**（六源日结 + 七类指标 metric_code + 真实迁移源表 + 维度 + 行级隔离 + 六个待财务/运营复核点）。作为 T7/T12 口径附件，待财务/运营复核定稿。

---

## 五、变更日志

| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建本 backlog，将 R5 10 题裁决 + 审计另含 P0 转为 T1–T17 任务卡与四 Sprint 排期 | R5 业务 HTML 缺口审计 + 用户 10 题裁决（见追溯矩阵 R5 横幅、各文档 R5 标注） |
