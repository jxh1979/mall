# 游乐 P0 安全巡检闭环规格 V0.03

> **口径修订（2026-08-06 R10）：「场次容量」已废止**——BSM「容量控制」条 明确「**只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量**」（DEC-R5-2026-08-04-04；R8 用户裁决 D-02）。本文原「场次容量」一律改称「**项目容量（实时在玩人数）**」；双阈值不变（运营容量软上限告警放行、安全容量硬上限阻断并自动停运）。仍按场次/时段或点位/设备维度罗列的页面、接口、组件与字段只作历史结构参考，容量口径一律以本条为准，相关字段待代码轮下线，期间不得据以新增受控维度。

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

> **M07 当前迁移与代码事实（2026-09-01，核到 V385）**
> - V114 已为 `amusement_projects` 增补 `location_point_id`，并为 `amusement_safety_inspections` 增补 `inspector_id`、`inspection_result`、`abnormal_found`、`abnormal_note`；V143 又增补 `device_id`、设备主数据和到期巡检任务表。上述字段不再标为“尚未迁移”。
> - `amusement_safety_template_items`、`amusement_safety_inspection_items` 和独立 `amusement_reopen_approvals` 仍不存在；单项结果与证据当前内嵌 `items_json`，点位顺序仍缺结构化承载，恢复审批用 `reopen_status` + `amusement_safety_actions`。
> - `AmusementInspectionTaskJob` 与 `generateDueInspectionTasks` 已实现；调度未配置时默认关闭，生产配置已显式开启。设备周期为空时当前代码回退项目级 24 小时，与 HTML“直接采用设备台账登记周期”不一致，登记为待修缺口。
> - `approveReopen` 当前会尝试联动项目状态，但 `reopenProjectIfBlocked` 只把 `maintenance/closed` 翻为 `open`，对 `paused` 不强制翻转；若安全阻断落为 `paused`，主管批准后仍可能无法恢复开放，属待修代码缺口。

## 1. 文档定位

本文档是游乐基础运营 P0 的执行型规格，专门固化“开放前阻断式安全巡检、阻断项暂停开放、整改复检、主管恢复、核销联动和全程留痕”的闭环。

本文不替代 `amusement-business-operations-product-prd-v0.1.md` 和 `amusement-operations-development-design.md`，而是把其中容易被实现遗漏的 P0 安全闭环抽出来，作为后端、前端、员工端、测试和运营验收的共同准入标准。

## 2. P0 范围边界

| 类别 | P0 必须做 | P0 不做 |
|---|---|---|
| 开放前安全 | 每个营业日开放前按项目既定点位顺序完成巡检；未通过不得开放和核销 | 不做复杂行业安全评估模型 |
| 营业中安全 | 按项目配置的最低频次抽检；阻断结论按设备分类/依赖关系停止对应范围 | P0 不让重大严重度名称自动替代阻断结论 |
| 恢复开放 | 阻断项整改后必须复检，并由主管确认恢复 | 不允许巡检员单人闭环阻断项 |
| 核销联动 | 核销时必须校验项目状态、权益指定场次（如有）、单项目容量和安全状态 | 不允许安全未通过时人工核销放行 |
| 留痕证据 | 巡检、失败、整改、复检、恢复和证据补录全部写事件与证据 | 不允许无证据或无主管复核的补录生效 |
| 后续能力 | P1 动态容量；P2 客流预测、自动调度、智能安全预警、游客体验分析 | 团体/包场是现行业务；预约排队、快速通道和离线核销已废止 |

## 3. 角色与权限

| 角色 | P0 可做 | P0 不可做 |
|---|---|---|
| 游乐运营人员 | 查看项目、场次、容量、安全状态，发起整改 | 不能审批阻断项恢复开放 |
| 巡检员 | 创建/提交巡检、上传现场证据、提交普通问题整改结果 | 不能绕过阻断项 |
| 游乐主管 | 配置项目容量上下限、批准阻断项恢复开放（原含「审核人工放行」，已废止·见文首口径） | 不能修改、清零或修正实时在玩人数；不能删除巡检和失败记录 |
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
| `amusement.safety_inspection.approve_reopen` | 审批阻断项恢复开放 | 是 |

## 4. 核心对象

> 口径说明（M07）：下表按 V014 + V114 + V143 更新。标“仍缺”者不得当作已落地能力验收。

| 对象 | 关键字段 | 说明 | 当前迁移现状 |
|---|---|---|---|
| `amusement_safety_templates` | `id`, `template_name`, `items_json`, `status`, `effective_from` | 巡检模板；巡检项内嵌 `items_json` | 真实存在；无 `project_type`/`version` 列，巡检项不拆子表 |
| `amusement_safety_template_items` | `item_id`, `template_id`, `item_name`, `severity`, `requires_photo`, `sort_order` | 巡检项；严重度与业务结论分别记录 | **整表目标增补（P0，尚未迁移）**：真实无此子表，项内嵌于模板 `items_json`；`severity` 列亦不存在（severity 仅在 V024/P1 `amusement_incidents`） |
| `amusement_safety_inspections` | V014 主列 + V114 `inspector_id/inspection_result/abnormal_found/abnormal_note` + V143 `device_id` | 项目/设备巡检主记录，单项结果内嵌 `items_json` | 真实存在；无独立 `session_id`，也不需要为容量增加场次维度 |
| `amusement_safety_inspection_items` | `inspection_id`, `item_id`, `result`, `remark`, `evidence_ids` | 单项结果和证据 | **整表目标增补（P0，尚未迁移）**：真实无此子表，单项结果内嵌于 `amusement_safety_inspections.items_json` |
| `amusement_reopen_approvals` | `approval_id`, `inspection_id`, `approver_id`, `decision`, `reason`, `approved_at` | 阻断项恢复审批 | **整表目标增补（P0，尚未迁移）**：真实无此表，恢复审批以 `amusement_safety_inspections.reopen_status` 列 + `amusement_safety_actions` 行表达 |
| `amusement_devices` | `project_id`, `device_code`, `location_point_id`, `safety_check_interval_hours`, `last_pass_inspection_at` | V143 设备主数据与巡检周期 | 真实存在；关键/辅助分类、依赖关系及审批留痕字段仍缺 |

严重度定义（**承载口径**：当前真实迁移无 `amusement_safety_template_items.severity` 列，严重度内嵌在 `items_json` 中；下列枚举为目标口径，结构化 `severity` 列为目标增补字段，P0 尚未迁移）：

| 严重度 | 含义 | 系统动作 |
|---|---|---|
| `notice` | 记录提醒，不影响开放 | 写事件，可继续开放 |
| `warning` | 有轻微风险，需要主管知晓 | 写事件并通知主管，可继续开放 |
| `blocking` | 阻断项，不允许开放或继续核销 | 按设备分类/依赖关系停设备或项目，进入整改复检 |
| `critical` | 重大风险，需安全负责人关注 | 通知、升级并高优先级处置；不因名称本身自动暂停，若同时需要阻断须显式给出 `blocking` 结论 |

## 5. 状态机

### 5.1 巡检状态

| 状态 | 可进入条件 | 可流转到 |
|---|---|---|
| `draft` | 巡检任务已创建 | `submitted`, `cancelled` |
| `submitted` | 巡检员提交结果 | `passed`, `warning`, `blocked` |
| `passed` | 全部必检项通过 | 结束 |
| `warning` | 存在提醒/警告项但无阻断项 | 结束或后续整改 |
| `blocked` | 存在明确 `blocking` 结论，或设备分类/依赖规则要求停运 | `rectifying` |
| `rectifying` | 运营提交整改中 | `rechecked` |
| `rechecked` | 复检完成 | `reopen_pending`, `blocked` |
| `reopen_pending` | 复检通过，等待主管恢复 | `reopened`, `blocked` |
| `reopened` | 主管确认恢复 | 结束 |
| `cancelled` | 误建任务取消 | 结束，必须留原因 |

### 5.2 项目/场次/设备状态影响

> HTML 项目状态严格为未开放、准备中、开放中、暂停、检修中、已关闭、异常。当前数据库仍只有 `open/paused/closed/maintenance` 四值，`unopened/preparing/abnormal` 为待迁移缺口；不得新增 `open_with_warning`。业务目标是主管审批后恢复 `open`；当前 `approveReopen` 只联动 `maintenance/closed → open`，不会强翻 `paused`，须修复该断链。

| 触发 | 项目/场次/设备状态 | 核销影响 |
|---|---|---|
| 无当日有效开放前巡检 | `preparing` 或不可开放 | 拒绝核销 |
| 开放前巡检通过 | `open` | 允许继续校验容量和权益 |
| 警告项存在 | `open` + 风险标记 | 允许核销，但主管可见；不得新增状态 |
| 阻断项存在 | `paused` 或 `abnormal` | 拒绝普通核销 |
| 整改未复检 | `paused` 或 `abnormal` | 拒绝普通核销 |
| 复检通过但未主管审批 | 项目保持 `paused/maintenance/abnormal`；巡检任务为 `reopen_pending` | 拒绝普通核销 |
| 主管审批恢复 | `open` | 恢复核销校验 |

## 6. P0 业务流程

### 6.1 开放前巡检

1. 系统按营业日、项目、设备和已配置的点位顺序生成开放前巡检任务；场次只用于运营安排，不形成安全容量维度。
2. 巡检员必须按点位顺序逐项填写；缺任一点位、未完成前一点位或未走完全部点位时，巡检不成立、不得标记完成。
3. 必拍项必须上传照片或证据。确需事后补录时，须附照片、录音或录像等证据并经主管复核后生效；补录与原始记录并列展示、互不覆盖。
4. 提交时系统分别记录严重度与业务结论；`critical` 负责通知升级，只有明确 `blocking` 结论或设备分类/依赖规则要求时才阻断。
5. 全部通过时，项目可进入 `open`；存在 `warning` 时保持 `open` 并写风险标记、通知主管。
6. 存在阻断时，按设备关键/辅助分类和依赖关系停用设备或暂停/异常项目，不得新增核销。

### 6.2 营业中抽检

1. 运营、主管或系统按项目配置的最低频次创建营业中抽检任务；频次是须配置上下限的安全阈值，出厂默认每 2 小时至少一次，未达最低频次视为当班未抽检。
2. 营业中抽检发现阻断结论时，按设备分类/依赖关系立即暂停对应范围；重大严重度负责通知升级，不单凭名称自动暂停。
3. 已排队或准备核销的客人必须收到明确拒绝原因，不能静默失败。
4. 已完成的历史核销不回滚；后续核销全部按新安全状态阻断。

### 6.3 整改、复检与恢复

1. 运营人员提交整改说明、责任人、完成时间和证据。
2. 巡检员或指定复检人提交复检结果。
3. 复检仍失败时继续保持暂停。
4. 复检通过后进入 `reopen_pending`。
5. 游乐主管审批恢复开放；审批必须记录原因、审批人、时间和证据引用。
6. 恢复后写 `amusement.safety_inspection.reopened` 事件，并恢复核销校验。

### 6.4 定期检修与设备分级

1. 定期检修不另设周期，直接采用设备台账已登记的安全检查周期；周期缺失时不得用固定 24 小时反向补成业务规则。
2. 辅助设备巡检超期、不合格或故障时只停该设备；项目能否继续运行由工程或安全岗位判断并留痕，没有留痕一律停项目。
3. 关键安全设备无依赖且其余关键设备正常时只停该设备；存在客人必经流程依赖时，整个项目停止新增核销并停止运行。
4. 设备分类和依赖关系变更须工程或安全审批；项目上线后关键设备降为辅助设备，只能在项目停运后生效。

### 6.5 营业日、项目时区与全园故障

- 游乐“当天/每天”按营业日计算，默认当日 10:00 至次日 10:00；起止时点按项目配置为安全阈值，须有上下限、最高权限审批和留痕。时区必须使用项目所在地时间，禁止服务器默认时区。
- 园区容量：园区容量。HTML 未定义更细算法，本规格不得据此增加园区容量控制或阻断规则。
- 巡检、设备、项目状态和证据须按综合体、游乐公司、游乐商户、项目、员工与设备权限隔离；游乐商户不得查看其他商户、其他项目未授权数据。
- 单台设备或单项目故障不直接触发补偿通票。只有全园故障影响整体履约时，才可由游乐公司负责人和工程或安全岗位共同确认后发放补偿通票。
- 全园故障生效后立即停止所有纯游乐票及含游乐权益通票的新销售；补偿只延长原票的核销权，不增加原票人数或次数，并关联原票、故障事件和原因防重复。

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
| 创建巡检 | `amusement.safety_inspection.created` | `amusement.safety_inspection.create_failed` | 创建人、项目、设备/点位、模板版本 |
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

### AC-AMU-SAFE-002 阻断结论按设备范围停运

Given 巡检员提交的巡检结果中存在 `blocking` 结论，
When 系统保存巡检结果，  
Then 系统必须按设备关键/辅助分类和依赖关系停用对应设备或暂停/异常项目；被停止范围的后续核销全部失败。仅有 `critical` 严重度但无阻断结论时，只通知升级、不自动停运。

### AC-AMU-SAFE-003 警告项不阻断但必须通知

Given 巡检结果只包含 `notice` 或 `warning` 项，  
When 系统保存巡检结果，  
Then 项目可以开放，但必须写入警告事件，并向主管或配置的负责人产生待处理提醒。

### AC-AMU-SAFE-004 恢复开放必须主管审批

Given 阻断项已经整改并复检通过，
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

### AC-AMU-SAFE-007 后续增强不得绕过现行闸门

Given 开发 P1 动态容量或 P2 客流预测、自动调度、智能安全预警、游客体验分析，
When 验收人员检查其输入、输出和动作权限，
Then 动态容量只能调整单项目运营容量软上限的已批准配置；P2 只输出经营或安全辅助结果，不得执行核销、放行、停运、恢复、审批或修改在玩人数，并须按综合体、游乐公司、游乐商户、项目、员工和设备权限隔离数据。

### AC-AMU-SAFE-008 点位顺序与证据补录

Given 巡检模板配置了多个有序点位，
When 巡检员漏过任一点位、越过前一点位或提交事后补录，
Then 未走完全部点位的任务不得完成；补录须附照片、录音或录像等证据并经主管复核后生效，且不得覆盖原记录。

### AC-AMU-SAFE-009 营业中最低频次

Given 项目已配置营业中抽检最低频次及上下限，
When 当班未在配置间隔内完成抽检，
Then 系统必须把该班次标记为未抽检并通知责任人；出厂默认值只作为可调整的项目参数，不得硬编码为不可变规则。

### AC-AMU-SAFE-010 设备周期与分类

Given 设备巡检超期、不合格或发生故障，
When 系统判定停运范围，
Then 必须直接使用设备台账登记周期，并依据关键/辅助分类、关键设备依赖关系及工程/安全审批留痕决定停单台设备或整个项目。

### AC-AMU-SAFE-011 营业日与项目时区

Given 游乐规则需要计算“当天”“每天”或巡检有效期，
When 业务发生在跨零点或服务器时区与项目时区不同的时点，
Then 系统必须按项目营业日配置和项目所在地时区计算，禁止使用服务器默认时区。

### AC-AMU-SAFE-012 全园故障

Given 故障范围为单台设备、单项目或整个游乐园，
When 系统决定售票与补偿，
Then 局部故障不得直接发补偿通票；全园故障须停止所有新增游乐售票，经共同确认后按原票人数/次数发放并关联原票与故障事件防重。

## 10. 边界场景

| 场景 | 处理 |
|---|---|
| 巡检任务重复提交 | 按幂等键只保留一次成功提交，重复请求返回原结果 |
| 巡检模板缺失 | 不允许开放项目；提示管理员先配置模板 |
| 必拍项未上传证据 | 不允许提交通过 |
| 巡检通过后营业中发现阻断项 | 按设备分类/依赖关系立即暂停对应范围，历史核销不回滚，后续核销拒绝 |
| 辅助设备或关键设备失败 | 辅助设备由工程/安全岗位留痕判断；关键设备按实际流程依赖与可替代性决定停单台或整个项目 |
| 主管试图无复检审批恢复 | 拒绝，必须先有复检通过记录 |
| 复检通过后主管拒绝恢复 | 继续保持暂停，并记录拒绝原因 |
| 离线状态提交巡检 | P0 不允许离线通过高风险安全巡检；可暂存草稿，但正式提交必须在线 |
| 安全阻断的处置 | 安全阻断一律不得绕过，必须先走整改复检恢复（原含「人工放行」，已废止·见文首口径） |
| 营业日跨零点 | 以项目营业日配置为准，不直接按自然日切断 |

## 11. 反模式

| 不允许 | 正确做法 |
|---|---|
| 只在页面上提示“请巡检”，但核销接口不校验 | 核销接口必须服务端校验有效巡检状态 |
| 巡检失败只写备注，不改变项目状态 | 明确阻断结论须按设备分类/依赖关系改变设备或项目状态；重大严重度单独通知升级 |
| 主管口头通知恢复开放 | 必须通过审批接口恢复并留事件 |
| 用删除巡检记录修正误操作 | 只能作废或冲正，保留原记录 |
| 把未获 HTML 批准的电子免责或深度事故工单列入开发范围 | 不进入现行开发范围；如需新增必须先完成业务立法 |

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
| 2026-09-01 | V0.03 | Grok 只读复审返修：准确登记 `approveReopen` 只联动 `maintenance/closed → open`、不强翻 `paused` 的代码缺口；删除把电子免责和深度事故工单排入 P1 的未授权表述 |
| 2026-09-01 | V0.02 | M07 收口：更新 V114/V143 与 approveReopen 代码事实；删除 `open_with_warning` 与重大即自动阻断；补点位顺序、证据补录、抽检频次、设备分级、营业日/项目时区、园区容量原文、全园故障与商户隔离；固化 James 批准的 P1/P2 边界 |
| 2026-06-10 | V0.01 | 新增游乐 P0 安全巡检闭环规格，固化开放前巡检、阻断、整改、复检、主管恢复、核销联动、事件留痕、验收标准和边界场景 |
| 2026-06-18 | V0.01 修正 | D6 三方字段口径统一：以真实迁移 V014 为准，容量列统一 `used_capacity`；将真实不存在的 `session_id/device_id/inspector_id`、`amusement_reopen_approvals` 表、`amusement_safety_template_items.severity` 标为「目标增补字段（P0，尚未迁移）」，消除「声称已存在」假象；§5.2 补 D11 现状标注（approveReopen 不联动 open_status） |
