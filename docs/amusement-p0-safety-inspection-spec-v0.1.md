# 游乐 P0 安全巡检闭环规格 V0.1

> **口径修订（2026-08-06 R10）：「场次容量」已废止**——BSM「容量控制」条 明确「**只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量**」（DEC-R5-2026-08-04-04；R8 用户裁决 D-02）。本文原「场次容量」一律改称「**项目容量（实时在玩人数）**」；双阈值不变（运营容量软上限告警放行、安全容量硬上限阻断并自动停运）。仍按场次/时段或点位/设备维度罗列的页面、接口、组件与字段只作历史结构参考，容量口径一律以本条为准，相关字段待代码轮下线，期间不得据以新增受控维度。

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

> **⚠️ D6 字段口径修正（以真实迁移 V014 为准，2026-06-18，只读核验后回填，未改代码）**
> 本规格 §4 原把若干字段/表声称为「现有」，与真实迁移 `V014__p0_amusement_safety_session_verification.sql` 不符。以真实迁移为准统一口径，两文件（本规格 + `amusement-operations-development-design.md`）一致：
> - **容量计数列统一为 `used_capacity`**：真实 `amusement_sessions` 实际列为 `capacity`/`used_capacity`（带 CHECK `0 ≤ used_capacity ≤ capacity`），不存在 `checked_in_count`。
> - **真实迁移中不存在、下文标为「目标增补字段（P0，尚未迁移）」**：`amusement_safety_inspections.session_id`、`.device_id`、`.inspector_id`；独立的 `amusement_reopen_approvals` 表；`amusement_safety_template_items` 子表及其 `severity` 列。
> - 真实现状：巡检主记录仅挂 `project_id`，单项结果与严重度内嵌在 `items_json`；模板巡检项内嵌在 `amusement_safety_templates.items_json`；恢复审批用 `amusement_safety_inspections.reopen_status` 列 + `amusement_safety_actions` 行表达；`severity` 仅作为 V024（P1）`amusement_incidents.severity` 存在，与巡检项严重度无关。
> - 这些是「目标」而非「已存在」，消除「声称已存在」的假象；落地需新迁移。

## 1. 文档定位

本文档是游乐基础运营 P0 的执行型规格，专门固化“开放前阻断式安全巡检、严重项暂停开放、整改复检、主管恢复、核销联动和全程留痕”的闭环。

本文不替代 `amusement-business-operations-product-prd-v0.1.md` 和 `amusement-operations-development-design.md`，而是把其中容易被实现遗漏的 P0 安全闭环抽出来，作为后端、前端、员工端、测试和运营验收的共同准入标准。

## 2. P0 范围边界

| 类别 | P0 必须做 | P0 不做 |
|---|---|---|
| 开放前安全 | 每个营业日/场次开放前必须完成巡检；未通过不得开放和核销 | 不做复杂行业安全评估模型 |
| 营业中安全 | 支持运营中抽检；严重项自动暂停项目/场次/设备点位 | 不做 AI 视频识别安全判断 |
| 恢复开放 | 严重项整改后必须复检，并由主管确认恢复 | 不允许巡检员单人闭环严重项 |
| 核销联动 | 核销时必须校验项目状态、场次状态、容量和安全状态 | 不允许安全未通过时人工普通核销放行 |
| 留痕证据 | 巡检、失败、整改、复检、恢复、人工例外全部写事件和证据 | 不要求 P0 建事故工单系统 |
| 后续能力 | P1 做安全告知/电子免责/事故工单；P2 做预约排队、团体/包场和集团视图 | 不把 P1/P2 能力提前压入 P0 |

## 3. 角色与权限

| 角色 | P0 可做 | P0 不可做 |
|---|---|---|
| 游乐运营人员 | 查看项目、场次、容量、安全状态，发起整改 | 不能审批严重项恢复开放 |
| 巡检员 | 创建/提交巡检、上传现场证据、提交普通问题整改结果 | 不能绕过严重项阻断 |
| 游乐主管 | 维护项目容量（实时在玩人数）、批准严重项恢复开放（原含「审核人工放行」，已废止·见文首口径） | 不能删除巡检和失败记录 |
| 安全负责人/总控 | 查看全部安全留痕、抽查巡检闭环、导出审计证据 | 不直接替代现场主管审批 |
| 核销员工 | 只在项目安全状态允许时核销 | 安全阻断一律不得绕过（原含「人工放行」，已废止·见文首口径） |

权限点统一写入 `p0-operation-permission-matrix-v0.1.md`：

| 权限点 | 含义 | 审批 |
|---|---|---|
| `amusement.safety_template.view` | 查看巡检模板和模板版本 | 否 |
| `amusement.safety_template.manage` | 创建、复制、发布、停用巡检模板 | 可配置 |
| `amusement.safety_inspection.view` | 查看巡检任务、记录和证据 | 否 |
| `amusement.safety_inspection.create` | 创建开放前/营业中/复检任务 | 可配置 |
| `amusement.safety_inspection.submit` | 提交巡检结果 | 否 |
| `amusement.safety_inspection.rectify` | 提交整改说明和证据 | 否 |
| `amusement.safety_inspection.recheck` | 提交复检结果 | 否 |
| `amusement.safety_inspection.approve_reopen` | 审批严重项恢复开放 | 是 |

## 4. 核心对象

> 口径说明（D6）：下表「字段/表是否真实存在」列已按真实迁移 V014 标注。标「目标增补」者为 P0 尚未迁移，不得当作已落地能力验收（验收口径见各 AC，但实现前需先落迁移）。

| 对象 | 关键字段 | 说明 | 真实迁移现状（V014） |
|---|---|---|---|
| `amusement_safety_templates` | `id`, `template_name`, `items_json`, `status`, `effective_from` | 巡检模板；巡检项内嵌 `items_json` | 真实存在；无 `project_type`/`version` 列，巡检项不拆子表 |
| `amusement_safety_template_items` | `item_id`, `template_id`, `item_name`, `severity`, `requires_photo`, `sort_order` | 巡检项；严重度决定是否阻断 | **整表目标增补（P0，尚未迁移）**：真实无此子表，项内嵌于模板 `items_json`；`severity` 列亦不存在（severity 仅在 V024/P1 `amusement_incidents`） |
| `amusement_safety_inspections` | 真实列：`id`, `project_id`, `template_id`, `items_json`, `overall_result`, `inspection_status`, `rectification_status`, `reopen_status`, `inspected_at`, `submitted_at`, `rechecked_at`, `approved_at`, `operator_note` | 巡检主记录（仅挂项目） | 真实存在；`session_id`/`device_id`/`inspector_id` 为**目标增补字段（P0，尚未迁移）**，真实表无这三列 |
| `amusement_safety_inspection_items` | `inspection_id`, `item_id`, `result`, `remark`, `evidence_ids` | 单项结果和证据 | **整表目标增补（P0，尚未迁移）**：真实无此子表，单项结果内嵌于 `amusement_safety_inspections.items_json` |
| `amusement_reopen_approvals` | `approval_id`, `inspection_id`, `approver_id`, `decision`, `reason`, `approved_at` | 严重项恢复审批 | **整表目标增补（P0，尚未迁移）**：真实无此表，恢复审批以 `amusement_safety_inspections.reopen_status` 列 + `amusement_safety_actions` 行表达 |

严重度定义（**承载口径**：当前真实迁移无 `amusement_safety_template_items.severity` 列，严重度内嵌在 `items_json` 中；下列枚举为目标口径，结构化 `severity` 列为目标增补字段，P0 尚未迁移）：

| 严重度 | 含义 | 系统动作 |
|---|---|---|
| `notice` | 记录提醒，不影响开放 | 写事件，可继续开放 |
| `warning` | 有轻微风险，需要主管知晓 | 写事件并通知主管，可继续开放 |
| `blocking` | 阻断项，不允许开放或继续核销 | 项目/场次/设备暂停，进入整改复检 |
| `critical` | 重大风险，需安全负责人关注 | 项目/场次/设备暂停，通知主管和安全负责人 |

## 5. 状态机

### 5.1 巡检状态

| 状态 | 可进入条件 | 可流转到 |
|---|---|---|
| `draft` | 巡检任务已创建 | `submitted`, `cancelled` |
| `submitted` | 巡检员提交结果 | `passed`, `warning`, `blocked` |
| `passed` | 全部必检项通过 | 结束 |
| `warning` | 存在提醒/警告项但无阻断项 | 结束或后续整改 |
| `blocked` | 存在 `blocking` 或 `critical` 项 | `rectifying` |
| `rectifying` | 运营提交整改中 | `rechecked` |
| `rechecked` | 复检完成 | `reopen_pending`, `blocked` |
| `reopen_pending` | 复检通过，等待主管恢复 | `reopened`, `blocked` |
| `reopened` | 主管确认恢复 | 结束 |
| `cancelled` | 误建任务取消 | 结束，必须留原因 |

### 5.2 项目/场次/设备状态影响

> 现状标注（D11，2026-06-18）：真实迁移 `amusement_projects.open_status` 仅 `open/paused/closed/maintenance` 四态，下表 `open_with_warning`/`abnormal`/`reopen_pending` 等为目标态，尚未迁移；且真实 `approveReopen` 只改 `amusement_safety_inspections.reopen_status`，**不联动 `amusement_projects.open_status`**，下表「主管审批恢复 → open」目前不会自动发生（候选验收口径：主管确认后项目 `open_status` 应自动恢复为 `open` 并产生 `amusement.project.resumed` 事件，详见开发设计文档 D11 第 2 项）。

| 触发 | 项目/场次/设备状态 | 核销影响 |
|---|---|---|
| 无当日有效开放前巡检 | `preparing` 或不可开放 | 拒绝核销 |
| 开放前巡检通过 | `open` | 允许继续校验容量和权益 |
| 警告项存在 | `open_with_warning` 或 `open` + 风险标记 | 允许核销，但主管可见 |
| 阻断/重大项存在 | `paused` 或 `abnormal` | 拒绝普通核销 |
| 整改未复检 | `paused` 或 `abnormal` | 拒绝普通核销 |
| 复检通过但未主管审批 | `reopen_pending` | 拒绝普通核销 |
| 主管审批恢复 | `open` | 恢复核销校验 |

## 6. P0 业务流程

### 6.1 开放前巡检

1. 系统按营业日、项目、场次或设备点位生成开放前巡检任务。
2. 巡检员逐项填写通过/不通过/不适用，必拍项必须上传照片或证据。
3. 提交时系统计算最高严重度。
4. 全部通过时，项目/场次可进入 `open`。
5. 存在 `warning` 时，项目/场次可开放，但必须通知主管并写风险标记。
6. 存在 `blocking` 或 `critical` 时，项目/场次/设备点位进入暂停或异常，不得开放。

### 6.2 营业中抽检

1. 运营、主管或系统策略可创建营业中抽检任务。
2. 营业中抽检发现阻断/重大项时，立即暂停对应范围。
3. 已排队或准备核销的客人必须收到明确拒绝原因，不能静默失败。
4. 已完成的历史核销不回滚；后续核销全部按新安全状态阻断。

### 6.3 整改、复检与恢复

1. 运营人员提交整改说明、责任人、完成时间和证据。
2. 巡检员或指定复检人提交复检结果。
3. 复检仍失败时继续保持暂停。
4. 复检通过后进入 `reopen_pending`。
5. 游乐主管审批恢复开放；审批必须记录原因、审批人、时间和证据引用。
6. 恢复后写 `amusement.safety_inspection.reopened` 事件，并恢复核销校验。

## 7. API 契约补充

| 方法 | 路径 | 用途 | 幂等 | 主要权限 |
|---|---|---|---|---|
| `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | 查询项目/场次是否允许开放或核销 | 否 | `amusement.safety_inspection.view` |
| `GET/POST` | `/api/v1/amusement/safety-templates` | 查询/维护巡检模板 | `POST` 需要 | `GET=amusement.safety_template.view`；`POST=amusement.safety_template.manage` |
| `GET/POST` | `/api/v1/amusement/safety-inspections` | 查询/创建巡检任务 | `POST` 需要 | `GET=amusement.safety_inspection.view`；`POST=amusement.safety_inspection.create` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | 提交巡检结果 | 是 | `amusement.safety_inspection.submit` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | 提交整改说明和证据 | 是 | `amusement.safety_inspection.rectify` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | 提交复检结果 | 是 | `amusement.safety_inspection.recheck` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | 主管审批恢复开放 | 是 | `amusement.safety_inspection.approve_reopen` |

接口响应、错误码、幂等头、分页和事件声明必须遵守 `p0-api-contract-pack-v0.1.md`。

## 8. 事件与证据

| 动作 | 成功事件 | 失败/拒绝事件 | 必须留痕 |
|---|---|---|---|
| 创建巡检 | `amusement.safety_inspection.created` | `amusement.safety_inspection.create_failed` | 创建人、项目、场次、模板版本 |
| 提交巡检 | `amusement.safety_inspection.submitted` | `amusement.safety_inspection.submit_failed` | 单项结果、证据、最高严重度 |
| 巡检通过 | `amusement.safety_inspection.passed` | - | 项目/场次状态变化 |
| 巡检警告 | `amusement.safety_inspection.warning_found` | - | 警告项、通知对象 |
| 阻断发现 | `amusement.safety_inspection.blocked` | - | 阻断项、暂停范围、拒绝核销原因 |
| 整改提交 | `amusement.safety_inspection.rectification_submitted` | `amusement.safety_inspection.rectification_rejected` | 整改人、说明、证据 |
| 复检 | `amusement.safety_inspection.rechecked` | `amusement.safety_inspection.recheck_failed` | 复检人、结果、证据 |
| 恢复开放 | `amusement.safety_inspection.reopened` | `amusement.safety_inspection.reopen_rejected` | 审批人、审批结论、原因 |
| 核销安全拒绝 | - | `amusement.verification_failed` | 失败原因 `SAFETY_BLOCKED` 或 `SAFETY_REOPEN_PENDING` |

## 9. 验收标准

### AC-AMU-SAFE-001 开放前巡检阻断

Given 某游乐项目当天没有有效开放前巡检通过记录，  
When 核销员工尝试对该项目执行核销，  
Then 系统必须拒绝核销，并返回“未完成开放前安全巡检”的明确原因。

### AC-AMU-SAFE-002 严重项自动暂停

Given 巡检员提交的巡检结果中存在 `blocking` 或 `critical` 项，  
When 系统保存巡检结果，  
Then 对应项目、场次或设备点位必须进入暂停/异常状态，且后续普通核销全部失败。

### AC-AMU-SAFE-003 警告项不阻断但必须通知

Given 巡检结果只包含 `notice` 或 `warning` 项，  
When 系统保存巡检结果，  
Then 项目可以开放，但必须写入警告事件，并向主管或配置的负责人产生待处理提醒。

### AC-AMU-SAFE-004 恢复开放必须主管审批

Given 严重项已经整改并复检通过，  
When 巡检员或运营人员尝试直接恢复开放，  
Then 系统必须拒绝；只有具备 `amusement.safety_inspection.approve_reopen` 权限的主管审批后才能恢复。

### AC-AMU-SAFE-005 核销同时校验安全与容量

Given 某项目巡检已通过但项目容量（实时在玩人数）已满（原称「场次容量」，已废止·见文首口径），  
When 核销员工执行核销，  
Then 系统必须按容量原因拒绝核销；安全通过不得绕过容量控制。

### AC-AMU-SAFE-006 留痕可追溯

Given 审计人员查询某项目某营业日安全闭环，  
When 输入项目、场次或设备点位，  
Then 系统必须展示巡检、失败项、整改、复检、恢复审批、相关核销拒绝和证据引用。

### AC-AMU-SAFE-007 P1/P2 能力不得误入 P0

Given 开发或供应商交付游乐 P0，  
When 验收人员检查功能范围，  
Then 不得要求 P0 完成电子免责、事故工单、预约排队、团体包场或集团视图；这些只保留后续扩展入口。

## 10. 边界场景

| 场景 | 处理 |
|---|---|
| 巡检任务重复提交 | 按幂等键只保留一次成功提交，重复请求返回原结果 |
| 巡检模板缺失 | 不允许开放项目；提示管理员先配置模板 |
| 必拍项未上传证据 | 不允许提交通过 |
| 巡检通过后营业中发现严重项 | 立即暂停对应范围，历史核销不回滚，后续核销拒绝 |
| 设备点位失败但项目其他点位正常 | 只暂停失败点位；如果该点位是项目必要点位，则暂停整个项目 |
| 主管试图无复检审批恢复 | 拒绝，必须先有复检通过记录 |
| 复检通过后主管拒绝恢复 | 继续保持暂停，并记录拒绝原因 |
| 离线状态提交巡检 | P0 不允许离线通过高风险安全巡检；可暂存草稿，但正式提交必须在线 |
| 安全阻断的处置 | 安全阻断一律不得绕过，必须先走整改复检恢复（原含「人工放行」，已废止·见文首口径） |
| 营业日跨零点 | 以项目营业日配置为准，不直接按自然日切断 |

## 11. 反模式

| 不允许 | 正确做法 |
|---|---|
| 只在页面上提示“请巡检”，但核销接口不校验 | 核销接口必须服务端校验有效巡检状态 |
| 巡检失败只写备注，不改变项目状态 | 阻断/重大项必须改变项目、场次或设备状态 |
| 主管口头通知恢复开放 | 必须通过审批接口恢复并留事件 |
| 用删除巡检记录修正误操作 | 只能作废或冲正，保留原记录 |
| 把电子免责和事故工单塞进 P0 验收 | P0 只保留后续入口，正式功能排 P1 |

## 12. 引用

| 内容 | 文档 |
|---|---|
| 游乐 PRD | `amusement-business-operations-product-prd-v0.1.md` |
| 游乐开发设计 | `amusement-operations-development-design.md` |
| P0 API 契约 | `p0-api-contract-pack-v0.1.md` |
| P0 权限矩阵 | `p0-operation-permission-matrix-v0.1.md` |
| P0 留痕矩阵 | `p0-action-event-evidence-matrix-v0.1.md` |
| P0 端到端验收 | `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| P0 任务映射 | `p0-task-api-permission-event-acceptance-map-v0.1.md` |

## 13. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-10 | V0.1 | 新增游乐 P0 安全巡检闭环规格，固化开放前巡检、阻断、整改、复检、主管恢复、核销联动、事件留痕、验收标准和边界场景 |
| 2026-06-18 | V0.1 修正 | D6 三方字段口径统一：以真实迁移 V014 为准，容量列统一 `used_capacity`；将真实不存在的 `session_id/device_id/inspector_id`、`amusement_reopen_approvals` 表、`amusement_safety_template_items.severity` 标为「目标增补字段（P0，尚未迁移）」，消除「声称已存在」假象；§5.2 补 D11 现状标注（approveReopen 不联动 open_status） |
