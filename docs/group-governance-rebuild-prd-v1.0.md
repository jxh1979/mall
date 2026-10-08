# 集团治理重建 PRD（V1.07·补断点）

> **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。

> 2026-07-27 PRD 审计修订：①平台/集团级角色可按职位模板跨项目公司下钻，项目公司管理员限本项目，商户用户 fail-closed；②五类高危授权审批按治理 HTML 改为不可关闭的强制闸门，配置只能加严；③补授权申请人与审批人职责分离；④补岗位、职级、组织、商户或责任范围变化后立即重算并收回旧范围派生权限。当前实现若不满足这些规则，记为实现待对齐，不反向改写 PRD。

上段为2026-07-27的历史修订摘要，不作为现行触发清单；现行角色装配与权限重算规则以§3及三份业务HTML为准。

> 生成：2026-06-27。业务规则以三份业务 HTML 为最终解释权；迁移、代码和接口只作为当前实现证据，不得反向改写业务规则。本文承接治理 HTML「综合体和集团 / 权限规则」及枚举 HTML §九，M1 需求、V169-V170 和当前代码仅用于说明追溯与实现状态。
> 重建动因：补断点非绿地：现有 RBAC/SupervisorReview 地基不动，只重建 V169-170 的装配闭环+审批激活+下钻。现有权限模板、临时授权、有效权限计算、SupervisorReview 三档审批继续复用；本轮只补“职位 ↔ 模板 ↔ 角色自动适配”“高危授权先审批再生效”“集团按职位模板可见的下钻对标”三处断点。

## 0. 定位与边界
- 不重写 RBAC：继续复用 `permissions`、`roles`、`role_permissions`、`user_roles`、`permission_templates`、`permission_template_items`、`permission_template_applications`、`temporary_permission_grants`。
- 不重写主管复核：高危授权审批复用 `SupervisorReviewService` 与 `supervisor_review_decisions`。敏感动作可要求上一级、上两级或最高权限，这是敏感度要求，不是发起人可选档位，也不等于指定角色档或合同单级。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现代码值 `one_level_up` / `two_levels_up` / `top_authority` 仅为实现证据，不取得业务效力。
- 本轮新增的是治理闭环，不是新权限模型：平台管理员配置岗位模板，HR/平台按用户职位定岗，系统自动授予角色并套模板；高危权限授出前可被审批闸拦住。
- 集团下钻的业务边界：只有项目之上的获授权岗位可跨项目汇总，项目公司管理员限本项目，商户用户不得查看集团总览。**当前实现待对齐**：`GET /api/v1/analytics/group-overview` 校验 `analytics.group.view` 和 `requirePlatformScope()`；后者拦截商户及外包范围，但未按项目级调用者过滤汇总。不得把通过该守卫等同于已认定的平台/集团级角色；项目范围隔离尚未闭环，见 §4。
- 不做：多套审批流引擎、按业务线重造授权模型、把职位等同于权限。职位只驱动默认角色/模板，最终有效权限仍由 RBAC + 临时授权 + 显式拒绝计算。

## 1. 业务对象与状态枚举（当前实现证据；锚点为枚举 HTML §九平台治理，不是 ENUM §十地产运营）

> 下表 `position_role_templates` / `access_grant_requests` 等为当前实现表名与校验取值，三份业务 HTML 零命中；未写入三份 HTML 的内容不因出现在本 PRD 或实现中而取得效力。不得称「逐字落 DB CHECK」或「逐字闭集」。ENUM §十为地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检），与本表无关。
| 对象 | 枚举/字段 | 取值（当前实现 CHECK / 服务校验） | 业务说明 |
|---|---|---|---|
| 职位角色模板 `position_role_templates` | `status` | `active` / `inactive` | 活跃模板参与定岗匹配；同组织、职位、职级仅允许一个活跃模板 |
| 授权申请 `access_grant_requests` | `request_type` | `user_role` / `user_position` / `temporary_permission` / `role_permission` / `user_scope_change` / `position_high_risk_release` / `user_permission_grant` / `user_permission_deny_removal` / `role_project_reapproval` | DB CHECK 九类（V434 八类，V441 加 `role_project_reapproval`）；申请入口接受七类（`user_role`、`user_position`、`temporary_permission`、`user_permission_grant`、`user_permission_deny_removal`、`position_high_risk_release`、`role_project_reapproval`），`user_scope_change` 由跨范围定岗在服务内生成；`position_high_risk_release` 首单由岗位高危挂起在服务内生成，被驳回后可经申请入口对同一挂起再次发起（M002-F20）；`role_permission` 服务层未开放 |
| 授权申请 `access_grant_requests` | `status` | `pending` / `applied` / `rejected` | 待审批 / 已应用 / 已驳回 |
| 权限定义 `permissions` | `requires_approval` | `true` / `false` | 实现字段；五类 HTML 强制高危权限必须为 `true`，后台只能新增高危项或提高审批等级，不能关闭强制项 |
| 权限定义 `permissions` | `risk_level` | V394 已加 CHECK（low / medium / high / critical）；服务按 `critical` / `high` / 其他分档 | `critical` → l3 / 最高审批；`high` → l2 / 上两级；其他 → l1 / 上一级 |
| 临时授权 `temporary_permission_grants` | `status` | 现有表无 CHECK；V170 默认写 `active` | 到期由有效权限查询按 `expires_at` 过滤；撤销写 `revoked_by/revoked_at`，单次授权写 `single_use/consumed/consumed_at` |
| 集团总览 API 参数 | `groupBy` | `project` / `entity` | Controller/Service 校验；非法返回 `VALIDATION_FAILED` |

## 2. 角色装配（V169）
**业务规则**（GOV「角色装配（已确认）」两支与五类高危以 HTML 为准。本节表名、唯一约束、接口取值与回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
- 平台管理员配置 `position_role_templates`：`position_code + staff_level_id` 绑定一个 `role_id` 与一个 `permission_template`。这对应业务口径“职位/层级对应哪个模板”。
- 【实现证据/不取得效力】活跃模板唯一约束：`COALESCE(organization_id,'') + position_code + staff_level_id` 在 `status='active'` 下唯一——三份业务 HTML 零命中，不得立法。
- 创建/更新职位模板必须是平台管理员，服务使用 `requireActorCanGrantImmediate` 检查模板中会立即生效的普通权限不得超出操作者自身有效权限；须审批的高危权限不要求配置者预先持有，实际授出仍须审批。平台管理员没有 wildcard 全权限例外，显式拒绝继续有效。
- 给用户定岗 `assignUserPosition` 时，系统按目标用户组织、职位、职级查模板，写三类结果：`staff_level_assignments`、`user_position_assignments`、`user_roles`，并套用 `permission_templates`。
- 跨组织闸保持原 RBAC 规则：非平台管理员不得给其他组织用户定岗；角色或模板若带组织归属，也必须与目标用户组织一致。自 M736 起，经可跨项目角色持有 `access.role.assign` 的人按该角色可查看的项目判（见平台治理 PRD §12 第 13、14、14B 条 M736 补记）；非平台管理员直接定岗、本次新派生出可跨项目角色的，须先审批（403 `GRANT_REQUIRES_APPROVAL`，改走岗位申请；技术推导，见同一补记⑦）。
- `access.role.assign` 独立为高危权限，V169 设为 `risk_level=high`、`requires_approval=true`，避免把“配角色”混在普通 `access.role.manage` 里。
- 直接分配角色使用 `requireActorCanGrant` 和跨组织闸；撤销角色只校验 `access.role.assign`、同组织与角色可配给目标，撤销前锁目标员工行，不要求操作人持有该角色的权限码（治理地图 V0.16「权限规则」，操作员 2026-09-25 裁决 M002-F11）。直接授出须审批的高危权限仍被拦截并走 §3 申请。同范围定岗按普通权限与待批高危权限分别处理：普通部分随保存生效，高危部分挂起进入审批链，通过后才释放，不因模板含高危权限而整单拒绝；挂起行与员工显式拒绝分行存放，释放或作废只删挂起行；释放单被驳回后该权限仍不生效，可对同一挂起再次提交 `position_high_risk_release` 申请（操作员 2026-09-25 裁决 M002-F20 原句「可以对同一挂起重新发起释放申请」；以下至「定岗或范围变更整单审批落地」为按治理地图第 703 行原句所作的技术细化）；员工仍在同一岗位映射且映射与模板快照没变时，待审单不重复开单、已驳回的不自动重开；已批准生效的高危权限（含定岗时已有员工显式拒绝、挂上该映射后经 `user_permission_deny_removal` 获批的）按（员工、岗位映射、权限码）判定，员工仍挂在同一张映射上、该码仍在该映射须审批集合内即保持生效，不因映射改角色、改模板或所引权限模板内容被修改而重新挂起；映射被修改（如改岗位编码或职级）致员工按原岗位职级改挂另一张映射时沿用原映射改存前的须审批集合，员工自己换岗、改职级到另一张映射、跨项目按新岗位全部重新判断；岗位行 `assigned_at` 记挂上当前映射的时间（同一映射期间不刷新）、`assigned_by` 记最近一次操作人；员工换岗（含改职级、换商户、跨项目）、岗位模板停用（只按 `assigned_position_template_id` 处理实际挂在该映射上的员工）或修改（含修改映射所引权限模板的内容；改映射的岗位编码或职级时，原来挂在该映射上的员工按其当前岗位职级重新派生，找不到映射即收回派生）时，旧挂起与待审释放单一并作废并关闭其复核单，按新岗位或新模板重新判断、需要时重新开单，定岗或范围变更整单审批落地时旧挂起与待审释放单同样作废；存档、取消存档不因此重算在岗员工（依据：F20 裁决更正段（控制席））；最高权限账号不设岗位、不套用岗位模板，给员工定岗、换岗的操作不能指向最高权限账号（操作员 2026-09-25 裁决 M002-F19，治理地图 V0.17 第 703 行）；落到接口（技术推导）：定岗、有岗位时改职级、范围变更与岗位申请不能指向最高权限账号（单独调整职级不带岗位派生、不在此列），存量岗位与名下待审的岗位高危释放、定岗、范围变更申请由 V435 清理；岗位规则不推广为其他授权入口的免审例外。自 M736 起，本条的跨组织闸与撤销角色的「同组织」改按操作人可触及的项目判：经可跨项目角色持有 `access.role.assign` 的人按该角色可查看的项目判，其余人仍是本人所属组织；可跨项目角色由非平台管理员直接分配时 403 `GRANT_REQUIRES_APPROVAL`，须提交角色授权申请，平台管理员直接分配不因其可跨项目而须先审批、角色所含权限属于既有高危权限的仍须先审批（见平台治理 PRD §12 第 13、14、14B 条 M736 补记）。

**关键端点（实际 Controller）**
- `GET /api/v1/access/position-role-templates`
- `POST /api/v1/access/position-role-templates`
- `PUT /api/v1/access/position-role-templates/{id}`
- `DELETE /api/v1/access/position-role-templates/{id}`（恒返回 409 `ARCHIVE_APPROVAL_REQUIRED`，不是删除入口）
- `POST /api/v1/access/position-role-templates/{id}/lifecycle`（`action` = archive / restore / request_delete / approve_delete / reject_delete；存档→申请删除→最高权限审批的真实路径）
- `PUT /api/v1/access/users/{id}/position`
- `POST /api/v1/access/role-assignments`
- `POST /api/v1/access/role-assignments/unassign`
- `GET /api/v1/access/staff-levels`
- `PUT /api/v1/access/users/{id}/staff-level`

**迁移 / 表 / 权限**
- 迁移：`V169__access_role_assignment_closure.sql`
- 新表：`position_role_templates`、`user_position_assignments`
- 复用表：`staff_levels`、`staff_level_assignments`、`roles`、`role_permissions`、`user_roles`、`permission_templates`、`permission_template_applications`
- 权限：`access.role.assign`（`domain=access`、`risk_level=high`、`requires_approval=true`）
- UI：`ADM-ACC-003` 访问治理工作台

## 3. 高危授权审批（V170）
**业务规则**（五类高危与角色装配两支以 GOV 为准。本节表名、status 流程、服务名为当前实现证据，三份业务 HTML 零命中，不取得效力。）
- 审批闸门：资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、**税费规则与税费调整**（共“五类”）——上述权限授出前必须先审批才生效。**（2026-08-03 已收口）** 原 `access.grant_approval.enabled` 配置项可一票关闭这四类强制审批（且关闭时无审计事件、无启动告警，静默失效），属本文此前登记的待对齐缺口；现已从 `AccessService` 移除，审批闸门在生产恒开、无任何配置可关，仅测试构造器保留入参。
- 高危权限配置：迁移清单只是当前实现初值。后台可以新增高危权限、提高风险等级或提高审批层级，但不得把 HTML 明确的强制项（五类）降级为无需审批，也不得移出强制范围。
- 高危覆盖口径（五类）：
  - 资金/核销/退款/调账：如 `finance.ar_ap.manage`、`finance.refund.create`、`finance.property_fee.manage`、`bar.bill.adjust`、`customer.stored_value.refund`、`inventory.stock.adjust`。**持股分红的方案配置、算单、审批和发放同属资金类高危动作，由最高权限审批，提交人不能审批自己的单据。**
  - 提权或跨商户跨项目（综合体）：如 `access.permission_template.manage/apply/revoke`、`access.role.manage`、`access.role.assign`、`access.temporary_grant.create/revoke`、`access.grant_request.review`、`governance.supervisor_review.approve`、`governance.project.manage`、`contract.approve.gm`。
  - 敏感经营数据：不限于内部结算价和成本价，还包括采购价、毛利、分账底价、域内补贴缺口、供应链价格及其他被标记为敏感的数据。查看、导出和修改分别授权；普通用户、会员、前线员工和无相应权限的商户人员不得查看。系统自动使用某价格不代表当前操作人获得查看或修改权限。数据默认按项目隔离，只有项目之上的获授权岗位才能跨项目查看。
  - 高危业务：如 `bar.product.sold_out`、`bar.bill.reopen`、`bar.order_item.void`、`bar.saved_bottle.manage`、`ticket.verify.reverse`、`amusement.verification.override`、`amusement.over_capacity_policy.configure`。
  - **第 5 类 税费规则与税费调整（2026-08-04 对齐平台治理地图/枚举速查表）**：所有税费规则和税费调整均须经过**财务审核、税务审核、法务审核**，再由**顶层平台公司的老板最终确认后生效**；**任一环节驳回均不得生效**；**提交人不得审批自己提交的调整**；全部审批意见和时间必须留痕。**税率、税目不属于普通后台配置项**，不得按普通配置项直接改动。
    - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档权限码 `staff.tax_bracket.manage`（V217，`risk_level=high`、`requires_approval=true`；属薪资个税档，不是本条第 5 类「税费规则与税费调整」）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 复核动作（策略 tax_rule_adjustment_l3，不是 `permissions` 行），但本条 HTML「税费规则与税费调整」在 `permissions` 侧尚无 `finance.tax_*` 权限码落点，**记为实现待对齐（范围收窄为该权限码）**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
- 申请类型当前服务支持七类：`user_role`、`user_position`、`temporary_permission`、`user_permission_grant`、`user_permission_deny_removal`（这两类为员工单独授予 / 解除员工显式拒绝，只收须审批权限，V434）、`position_high_risk_release`（岗位高危释放单被驳回后对同一挂起再次发起，只收仍挂起、无待审单覆盖的码，M002-F20）、`role_project_reapproval`（M736：可跨项目角色增加可查看项目后，原持有人对新增项目的重新审批；须带 `role_id`，只收对该角色有待重新审批项目的员工）。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支；`user_scope_change` 由服务内生成（V392），不是申请入口可填类型。
- 申请时服务先解析目标权限码。角色/岗位授权申请若不含 `requires_approval=true` 的权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权；临时权限申请无论日常或高危均进入主管审批链，其中仅含日常权限的申请使用当前一级主管策略，不直接授出。例外（M736）：角色/岗位授权申请所指角色是可跨项目角色、且发起人不是平台管理员的，只含日常权限也受理，不返回 `GRANT_APPROVAL_NOT_REQUIRED`。`role_project_reapproval` 不论发起人是谁、所指角色是否含 `requires_approval=true` 的权限都受理，不返回 `GRANT_APPROVAL_NOT_REQUIRED`（M736）。
- 审批链不由发起人选择档位，也不按风险值自动映射成可选档或指定角色。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现把 `critical`/`high`/其他 写成 `permission_grant_l3`/`l2`/`l1` 与 `top_authority`/`two_levels_up`/`one_level_up`，属实现证据，不是业务可选档。可跨项目这一项只设下限「高」（M736，操作员 2026-09-25 答复「高风险档」）：所指角色是可跨项目角色、且发起人不是平台管理员的角色/岗位授权申请（含上一条例外受理的只含日常权限的申请），审批档位取各权限风险档与 `high` 一档中较高者——按现行授权政策，不含 `critical` 权限时为 `permission_grant_l2`，含 `critical` 权限的仍为 `permission_grant_l3`。非平台管理员发起、新岗位派生出可跨项目角色的范围变更同样以「高」一档为下限（技术推导，非操作员原话，见平台治理 PRD §12 第 13、14、14B 条 M736 补记⑦）。`role_project_reapproval` 同样以 `high` 一档为下限，平台管理员发起的也一样；减少角色可查看项目的主管复核单取 `high` 一档（技术推导，非操作员原话，见平台治理 PRD §12 M736 补记⑫）。
- 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。
- 【实现证据/不取得效力】流程：申请创建 `access_grant_requests(status=pending)` 并调用 `SupervisorReviewService.blockForAdmission` 生成 `review_id`；审批端调用 SupervisorReview `approve` 后，批准则应用角色/定岗/临时权限/员工单独授予 / 解除员工显式拒绝并写 `applied`，驳回写 `rejected`——三份业务 HTML 零命中上述表名、状态流程与服务名，不得立法。
- 临时权限必须有 `expires_at`；可带 `valid_from`、`single_use`。V170 扩展 `temporary_permission_grants` 的 `status`、`valid_from`、撤销、单次消耗字段；有效权限查询按有效期和消耗状态过滤。
- 角色装配按治理 HTML「角色装配（已确认）」两支执行。同商户同项目：保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。
- 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。统计窗口和频繁阈值从后台授权政策读取，查询不得覆盖；兼容的 `window_days` 仅在等于后台值时接受，两个阈值参数只要传入即返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。

**关键端点（实际 Controller）**
- `POST /api/v1/access/grant-requests`
- `GET /api/v1/access/grant-requests?window_days=...`
- `POST /api/v1/access/grant-requests/{id}/review`
- `GET /api/v1/access/grant-requests/summary`（使用后台授权政策，不传阈值参数）
- `POST /api/v1/access/temporary-grants`
- `POST /api/v1/access/temporary-grants/{id}/revoke`

**迁移 / 表 / 权限**
- 迁移：`V170__access_grant_requests.sql`
- 表：`access_grant_requests`、`temporary_permission_grants`；批量更新 `permissions.requires_approval`
- 权限：`access.grant_request.view`（medium）、`access.grant_request.apply`（low；V170 原 high，V394 已调整）、`access.grant_request.review`（critical、requires approval）、既有 `governance.supervisor_review.approve`
- UI：`ADM-ACC-003` 访问治理工作台

## 4. 集团下钻对标
**业务规则**（可见范围与平台/集团级角色以 GOV 为准。本节 `groupBy`、缺主体归并与快照回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
- 集团视图的业务可见范围仍按治理 HTML 的项目隔离与岗位授权执行。**当前实现**只校验 `analytics.group.view` 和 `requirePlatformScope()`，再按 `groupBy` 汇总全部项目快照；无商户归属的项目级调用者通过守卫后，结果没有按其项目收敛。该范围缺口待后台实现模块处理，不能据本 PRD 判为已经隔离。
- 【实现证据/不取得效力】端点 `GET /api/v1/analytics/group-overview` 接受 `groupBy=project|entity`。`project` 按综合体项目聚合；`entity` 通过 `projects` 关联 `merchants.business_entity_id` 派生，缺主体归 `entity_unassigned`——三份业务 HTML 零命中 `groupBy` 与 `entity_unassigned`，不得立法。
- Controller 同时校验 `analytics.group.view` 和 `requirePlatformScope()`；即使某商户用户误拿到权限，也会因 merchant scope 被拒绝。
- 指标目录来自真实 ETL 快照：`revenue.net`、`revenue.ticket`、`revenue.bar`、`traffic.amusement`、`verify.done`、`risk.pending_review`；商户数由当日 merchant ETL 快照去重统计。
- 【实现证据/不取得效力】优先读取 `analytics_business_metric_snapshots(segment_type='project')`；没有 project 口径快照时回退平台口径单行 `project_1/综合体（默认）`——三份业务 HTML 零命中该回退值，不得立法。
- 【**DEC-R7-P4**】2026-08-05 用户裁决：**提案已否决：阈值不写入 HTML，PRD 数值已删除**（DIFF-B-046②；PGM「集团下钻细则（已确认）」条 与「KPI 可见性分层」条 只写分层、未给对标偏离阈值）。
- 【待决】对标是否标红及如何比较：三份业务 HTML 未写入阈值或公式。本 PRD 不定义业务参数、不给出比较公式。未入 HTML 不得先取得效力；实现不得按已删除公式或任何未入 HTML 的数值编码。现实现残留数值不得作为业务口径。

**关键端点（实际 Controller）**
- `GET /api/v1/analytics/group-overview?groupBy=project`
- `GET /api/v1/analytics/group-overview?groupBy=entity`

**迁移 / 表 / 权限**
- 迁移：权限来自 `V115__analytics_group_overview_permission.sql`；职位授权闭环来自 V169；高危授权审批来自 V170
- 表：`analytics_business_metric_snapshots`、`projects`、`merchants.business_entity_id`
- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**审批属性缺口（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；该结论只涉及审批属性；`requirePlatformScope()` 不替代授权审批，也尚未完成本节所述项目级结果过滤。
- UI：`ADM-ANA-002` 集团总览；可通过 `ADM-ACC-003` 的职位模板授出或收回 `analytics.group.view`

> 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` CHECK 与 §4 `analytics.group.view` 审批属性按 V394 更新为已闭合；§2 关键端点补 lifecycle 并标注 DELETE 恒拒绝；§3 税费权限码登记句按 V217 / SupervisorReview 现状改写。

> 变更记录（2026-09-14，V1.02，M013-012–016）：集团项目隔离现状与审批属性闭合分开；岗位模板普通权限与待批高危、日常临时审批、申请权限风险档、总结查询参数按现有实现订正。

> 变更记录（2026-09-19，V1.03，M013-019/020）：删去角色装配重算规则中与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的职级与责任范围排除；授权申请类型按 V392 六类订正。

> 变更记录（2026-09-23，V1.04，M013-029）：保留2026-07-27历史摘要原文，补明现行角色装配与权限重算规则的引用位置。

> 变更记录（2026-09-25，V1.05，全量审计 M002 CODEX-M002-1-01/02）：授权申请类型按 V434 八类订正，申请入口新增员工单独授予（`user_permission_grant`）与解除员工显式拒绝（`user_permission_deny_removal`）两类、只收须审批权限；岗位高危挂起与员工显式拒绝分行存放，释放或作废只删挂起行。

> 变更记录（2026-09-25，V1.06，全量审计 M002 第 7 轮 M002-F24/F19/F20/F22）：§2 撤销角色口径按治理地图 V0.16 与操作员裁决 M002-F11 改为不要求操作人持有该角色的权限码（原句写撤销也校验操作人持有，与裁决相反）；补岗位高危挂起驳回后对同一挂起再次申请（申请入口增 `position_high_risk_release`，§1、§3 类型表同步为六类）、换岗与模板停用或修改时作废重判并关闭复核单、已批准的按（员工、映射、码）判定，以及最高权限账号不设岗位（治理地图 V0.17）。

> 变更记录（2026-10-08，V1.07，M736 可跨项目角色·批 1 权限治理）：§2 跨组织闸与直接分配、撤销角色两条，§3 授权申请受理与审批档位两条，补记可跨项目角色的口径——按该角色可查看的项目判；非平台管理员直接分配、直接定岗新派生这类角色须先审批；这类角色的申请只含日常权限也受理，审批档位下限为「高」。§0、§4 的集团视图与分析随 M737，本次未改。角色分出去以后增减可查看项目见平台治理 PRD §12 M736 补记⑫，§1 授权申请类型表与 §3 申请类型、受理与档位三条随之补记。
