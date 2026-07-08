# 数据存档与审批后删除范式设计 v0.1

## 1. 背景与原则

用户拍板：**"所有的数据都应该有存档或删除的能力，可以要求审批，但不能说所有数据录入以后就没有变化了。"**

以通票权益模板（`entitlement_templates`）为首个试点：配置错误的模板、过期无效模板，应允许**存档**（软下线、可恢复）或**删除**（彻底移除、走上级审批）。

范式两条腿，语义严格区分：

| 能力 | 语义 | 可逆 | 是否要审批 | 数据落地 |
|------|------|------|-----------|---------|
| **存档 (archive)** | 软标记下线，业务不再可选用，但历史引用（已发放权益、已绑票种、结算流水）仍完整可查 | ✅ 可取消存档恢复 | 否（`manage` 权限即可，属日常运营） | 加 `archived_at` / `archived_by` / `archive_reason` 列（V271，存档必填理由）；列表默认过滤 |
| **删除 (delete)** | 从主表物理移除该行 | ❌ 不可逆 | ✅ 是（申请→上级审批→执行） | 两段式：申请列打标 → 审批通过后 `DELETE` |

> **⚠️ 2026-07-07 更新（业务HTML「存档及删除需要填写存档及删除理由。原则上存档删除需要最高权限批准」代码强制落地）：**
> ① **填理由强制**：存档与删除申请均须填写**非空理由**，空理由由服务层 `requireText` 直接 400 拒绝。存档理由落新增的 `archive_reason` 列（迁移 V271，覆盖全部 11 张存档删除范式表），取消存档时清空；删除理由沿用 `delete_reason` 列（由此前可空改为强制非空）。7 域全覆盖：票种 / 价格规则 / 权益模板 / 商户 / 商户服务工单 / 员工档案 / 卡座预订 / 酒吧目录（分类·商品·套餐）/ 设施设备 / 集中清理作业。
> ② **最高权限批准**：删除审批权限码 `*.approve`（`critical` + `requires_approval`）仅授予平台最高管理员 `role_platform_admin`（系统内最高角色，无更高独立角色；「owner」为凭证账户非 RBAC 角色）；无任何更低角色可批，且申请人禁自审。故「原则上需最高权限批准」**现状已满足**，审批端点无需收紧。

## 2. 为什么存档=软标记、删除=硬删+审批

- **存档**面向"配错了/过期了、先下线别再用、但别丢历史"。这是高频、低危操作，用软删除范式（本仓已有先例 `V182 merchant_fee_notification_recipient.deleted_at`、`V179 site_entry_reports.deleted_at`），列表 `WHERE archived_at IS NULL` 默认过滤即可。**取消存档**只是把 `archived_at` 置回 NULL。
- **删除**面向"这条记录本就不该存在/该彻底下线"。**⚠️2026-07-07 更新（PR#53，用户拍板"所谓删除只是存到数据库中、不在前台展示"）：删除已改为软删除——不物理抹除，执行为 `UPDATE delete_status='approved'` 使行保留在库、被前台读取（`delete_status <> 'approved'`）过滤隐藏，可查/可审计/可恢复；集中清理工具亦由物理删改为批量隐藏；数据按分类归纳。** 仍**必须走审批**且**执行前校验无活引用**（避免隐藏被引用行留悬空关系）；组合套餐软删时子项一并保留。唯 PIPL 生物识别照片（`ticket_face_photos.purge_after`）等法定必销数据、以及"重算替换"类（合同审批步骤/角色权限分配/套餐明细替换/结算草稿）仍走物理删。

## 3. 复用现有审批基建的取舍（诚实标注）

本仓已有通用审批基建 `supervisor_review_decisions`（V050/V053/V057）+ `SupervisorReviewService`，语义为"评估动作→命中策略则 `blocked=pending_review`→上级 `approve`（校验审批人职级 rank、**禁止发起人自审**）"。

**取舍决策**：`SupervisorReviewService.approve` 只把 `review_status` 改成 `approved` **并留痕/发事件**，**没有"批准后回调执行业务动作"的钩子**——它是一个放行/阻断的裁决层，不是编排层。若强接删除流，需要额外的 "pending→轮询/回调→执行 DELETE" 层与跨域耦合。

因此试点采用**折中方案**（复用审批**范式与原则**，不硬塞跨域评估表）：

- 删除申请状态机落在**通票模板自身的列**上（`delete_status` + 申请人/申请时间/审批人/审批时间/理由），一次数据库往返即可推进，无跨域回调。
- **审批权限用新增的分级权限码** `ticket.entitlement.approve`（`requires_approval=TRUE`, `risk_level=critical`），与 `ticket.entitlement.manage`（发起删除申请）**职责分离**——沿用 `supervisor_review` 的"发起权≠审批权"原则。
- **禁止自审**：审批端点校验 `delete_requested_by != 当前审批人`，与 `SupervisorReviewService` 的 `SUPERVISOR_SELF_REVIEW_FORBIDDEN` 同源。

> 未来若需要跨实体统一审批中心，可把此状态机迁到 `supervisor_review` 并补一个"批准后执行回调注册表"。当前试点不做，避免为 1 张表引入编排层（YAGNI）。

## 4. 表结构改动（V253）

`entitlement_templates` 加 5 列，全部可空/带默认，向后兼容：

```sql
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS archived_at           TIMESTAMPTZ,           -- 非空=已存档
    ADD COLUMN IF NOT EXISTS archived_by           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status         VARCHAR(16) NOT NULL DEFAULT 'none',  -- none|pending|approved
    ADD COLUMN IF NOT EXISTS delete_requested_by   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason         TEXT;

ALTER TABLE entitlement_templates
    ADD CONSTRAINT chk_et_delete_status
    CHECK (delete_status IN ('none','pending','approved'));
```

行删除后记录不复存在，故审批人/审批时间无需持久化在本表（留痕靠 `AuditEventService` 事件 `ticket.entitlement.delete_approved`）。`delete_status='approved'` 是执行 DELETE 前的瞬时态（同事务内置 approved 再 DELETE，或 approved 后立即 DELETE）。

## 5. 状态机

**存档轴**（archived_at）：
```
active(archived_at IS NULL) --archive--> archived(archived_at=now)
archived --unarchive--> active
```

**删除轴**（delete_status）：
```
none --request-delete[需 manage 权限]--> pending(记 requested_by/at/reason)
pending --approve[需 approve 权限, 审批人≠申请人, 无活引用]--> DELETE(物理移除)
pending --reject[需 approve 权限]--> none
```

约束：
- 只能对**已存档**的模板发起删除申请（先下线再申请删，防误删在用模板）。
- 审批执行 DELETE 前校验 `ticket_type_entitlements` / `issued_entitlements` 无引用；有引用则拒 `ENTITLEMENT_TEMPLATE_IN_USE`。

## 6. 权限码（V253 seed）

| 权限码 | 名称 | risk | requires_approval | 谁有 |
|--------|------|------|-------------------|------|
| `ticket.entitlement.manage`（已存在） | 管理权益模板 | high | TRUE | 发起存档/取消存档/删除申请 |
| `ticket.entitlement.approve`（新增） | 审批权益模板删除 | critical | TRUE | 审批/驳回删除申请 |

`ticket.entitlement.approve` seed 给 `role_platform_admin`。发起人（manage）与审批人（approve）职责分离。

## 7. API 端点设计

沿用现有 `/api/v1/ticketing/entitlement-templates` 前缀，全部带 `idempotencyKey`：

| 方法 | 路径 | 权限 | 动作 |
|------|------|------|------|
| GET | `/ticketing/entitlement-templates` | `ticket.entitlement.view` | 列表（默认排除 archived + delete_status='approved'）；`?includeArchived=true` 显示存档 |
| POST | `/ticketing/entitlement-templates/{id}/archive` | `ticket.entitlement.manage` | 存档 |
| POST | `/ticketing/entitlement-templates/{id}/unarchive` | `ticket.entitlement.manage` | 取消存档 |
| POST | `/ticketing/entitlement-templates/{id}/delete-request` | `ticket.entitlement.manage` | 发起删除申请（须先存档） |
| POST | `/ticketing/entitlement-templates/{id}/delete-approve` | `ticket.entitlement.approve` | 审批：`{decision: approved\|rejected}`，approved 即执行 DELETE |

## 8. 前端（TicketingPoliciesPage）

权益模板表格加"操作"列：
- 未存档行：`存档` 按钮（`manage`）。
- 已存档行：`取消存档` + `申请删除`（`manage`）；若 `delete_status=pending` 显示"待审批删除"+ `批准删除`/`驳回`（`approve`）。
- 列表默认只显示未存档；加"显示已存档"开关。
- 全中文，按钮走 `PermissionButton` 按权限码显隐。

## 9. 推广到其它实体的路线图

范式三件套可复用：`archived_at/archived_by` 列 + `delete_status` 状态机 + `.manage`/`.approve` 权限对 + 前端操作列。优先级按"高频配错/过期 且 有历史引用需保全"：

| 优先级 | 实体 | 理由 | 预估工作量 |
|--------|------|------|-----------|
| P1 | `ticket_types` 票种、`price_rules` 派生价规则、`dynamic_pricing_rules` 动态定价 | 与通票同域、同页、配错高频 | 各 ~0.5 人日（照抄本试点） |
| P1 | `entitlement_templates`（本试点） | 已完成 | — |
| P2 | 合同模板 / 审批链模板、角色/权限模板、公告模板 | 配置类、误配需下线保历史 | 各 ~1 人日（域分散） |
| P2 | 商户 / 场地 / 点位主数据 | 停业需存档不删（结算历史依赖） | 各 ~1 人日 |
| P3 | 交易类（订单/核销/结算流水） | **只存档不硬删**（财税留存合规），删除轴应禁用或走更高审批 | 逐表评估，~2 人日/域 |

**取舍与未覆盖面（诚实标注）**：
- 试点删除审批为**单级**（申请→一次审批）。多级审批链未做；若某实体需要逐级上提，应改接 `supervisor_review` 的 rank 逐级机制。
- 删除审批状态机在实体自身列，**不是**统一审批中心。跨实体聚合"待审批删除"看板未做（每实体各查各的）。
- 交易类实体（P3）的"删除"语义存疑——多数应**禁止硬删**，仅允许存档/冲销。推广前需逐域与业务确认删除是否合法。
- 本试点**不引入**批量存档/删除。

## 10. 铺开进度与三条硬约束（2026-07-07 更新）

§9 路线图已开始落地。实际进度（均已合并 origin/main 并部署测试机）：

| 实体 | 迁移 | PR | 状态 | 引用校验方式 |
|------|------|----|----|----|
| 权益模板 `entitlement_templates`（试点） | V255 | #15 | ✅ 已上线 | 硬编码两表（ticket_type_entitlements + issued_entitlements） |
| **商户主数据 `merchants`** | V259 | #24 | ✅ 已上线 | **元数据驱动**（见下约束一） |
| **票种 `ticket_types` + 派生价 `ticket_price_rules`** | V260 | #29 | ✅ 已上线 | 元数据驱动（ticket_type_id / price_rule_id） |
| **酒吧商品/分类/套餐 `bar_products`/`bar_product_categories`/`bar_combos`** | V262 | #31 | ✅ 已上线 | 商品=显式清单（见约束一例外）；分类=含已存档商品；套餐=恒 0 |
| 平台管理员=全部权限 | V261 | #29 | ✅ 已上线 | 历史漏授 25 条补齐并固化，新权限迁移须同步授 `role_platform_admin` |

铺开过程中每个实体都经独立复核，挖出并修复了真缺陷，沉淀为**范式必备的三条硬约束**（后续实体照做）：

### 约束一：删除前引用校验必须"元数据驱动"，不能硬编码表清单
商户主数据初版用硬编码 12 张表清单做引用计数，复核发现全库约 86 张表带 `merchant_id`，**漏掉了结算(settlement_transactions/items)、票单(ticket_orders/refunds)、储值(customer_stored_value_accounts)等资金承载表**——硬删会造成资金记录指向已删商户的孤儿。
**正解**：动态枚举 `information_schema` 里所有带该外键列的**基表**逐表 COUNT，仅排除纯审计历史表（audit_events/evidence_*，保留悬空引用属预期留痕）。自动覆盖未来新表，不再随加表漂移。表名来自系统元数据非用户输入，无注入风险。
**例外**：当外键列名是**通用名**（如 `product_id` 可能出现在库存/游乐等别域表）时，元数据驱动会误算别域同名列——此时改用**显式清单**并加守护测试（断言"引用该主键的外键表集合==清单"，新增引用表则测试红，强制同步）。域限定名（`merchant_id`/`ticket_type_id`）用元数据驱动，通用名用显式清单+守护测试。

### 约束二：必须补"引用侧守卫"——已存档实体拒绝被新引用/下单
只有删除侧校验不够。票种上线后复核发现：**已存档的票种仍可被配派生价、绑权益、甚至终端直传编号下单**——"存档=软下线"名存实亡，且持续产生的新引用会让审批删除**永远过不了引用校验**（与硬删闸互相打架）。
**正解**：在所有"引用该实体"的入口（配价规/绑权益/下单/加入套餐等）加守卫，拒绝引用已存档或待删除（`archived_at != null || delete_status != 'none'`）的实体，错误码 `XXX_NOT_SELLABLE/NOT_BINDABLE/NOT_CONFIGURABLE`。守卫放在扣库存/占用等副作用之前（拒单不动库存）。**每个新铺实体都要带这条**。

### 约束三：物理删除必须走审计留痕
酒吧目录删除初版的 `approveDelete` 直接物理 DELETE、**零审计留痕**——删完全系统查不到"谁在何时以何理由删了什么"，与本范式的合规初衷冲突。
**正解**：删除生命周期（申请/审批）走带审计的执行器（记操作者/实体/事件/时间）。若该执行器强制幂等键而删除审批 DTO 无幂等键，用"只审计不强制幂等"的写变体（删除审批状态机天然防重放：删后重放 404、驳回后重放 400，幂等可豁免）。**可逆的编辑（create/archive/unarchive）可走轻量 CRUD 不套；不可逆的物理删除必须留痕。**

### 交互层：创建/编辑流＝「＋新建」入口 → 单屏引导表单 → 确认页
配套 UX 铺开（引导式流程范式 GuidedFlowModal，见业务系统地图）：后台各页的"新建/录入"从内联填表改为「＋新建」按钮→引导弹窗→确认创建。存档/删除操作列不受影响。
**2026-07-07 细化（PR #76）：默认单屏（singlePage）——所有字段并到一屏（各步作分节堆叠）→「下一步」一次跑完各步校验 → 确认页 → 提交；仅当"后步要用前步结果做判断"时才走逐步多步。** 详见 §12 本轮 UI/交互/主题打磨。

### 仍未做（诚实标注）
- **交易类 P3（订单/核销/结算流水/账单）仅存档不硬删**：迁移与前端尚未铺，删除轴应禁用；待批次实施。
- **业务记录 Tier-B（员工/客户/设备/工单/预约）**：存档+审批删除尚未铺。
- **批量存档/删除 + 跨实体"待审批删除"聚合看板**：未做。
- **多级审批链**：删除审批仍单级（申请→一次审批）。

## 11. 第二批铺开(2026-07-07 续)：业务记录删除 + 集中清理工具 + 酒吲门店纠正

§10 之后继续铺，均已合并 origin/main 并部署测试机：

### 11.1 业务记录删除 Tier-B（V266，PR #48，后端）
把「存档+审批后删除」范式铺到 4 个业务记录实体（非资金）——照 §10 三条硬约束：

| 实体 | 引用校验(countReferences 拒删覆盖) |
|------|------|
| 商户服务工单 `merchant_service_work_orders` | 非终态(status∉closed/cancelled)即拒删；子表 actions 为 ON DELETE CASCADE 不计 |
| 卡座预订 `bar_reservations` | bar_orders / bar_bills / bar_table_sessions / bar_reservation_invitation_members（guest_checkins CASCADE 不计） |
| 员工档案 `staff_profiles` | 按 user_id 数 staff_attendance_records / staff_leave_requests / staff_level_assignments |
| 设施设备 `facility_equipment` | facility_work_orders.equipment_id |

禁自审、须先存档、审计留痕、rejected 回 none、取消存档回退 pending→none 全按范式。**本批仅后端**（前端存档/删除操作列按用户要求留后续）。16 JDBC 集成测试全绿。

### 11.2 集中数据清理工具 Tier-D（V265，PR #47）
用户反馈"逐条删除藏在 存档→显示已存档→申请删除→审批 多步里太隐蔽、只看到存档"。建集中入口 `data_cleanup_jobs`（平台级台账）：
- **只删已存档且超 N 天的数据，绝不删活数据**；
- **dry-run 预览**（只统计将删条数+样本，不删）→ **提交** → **他人审批**（禁自审）→ **批量执行**（一事务，部分失败整批回滚）；
- **删前逐行再校验无活引用**——引用计数改为**外键目录(pg_constraint)驱动**（复核修：原按列名匹配会漏扫差异命名外键，如权益模板真实外键列 entitlement_template_id≠声明的 template_id，导致漏判照删触发外键违约整批回滚；改查 pg_constraint 后免疫所有列名差异，排除 ON DELETE CASCADE 子表）；
- 支持 7 类已存档配置实体清理；权限 `data.cleanup.request`(发起)≠`data.cleanup.approve`(审批)。
- 前端「数据清理工具」页(ADM-OPS-002)：选范围→预览(醒目"仅预览未删除")→提交→审批执行→留痕。

> 这就是 §10 "仍未做" 里列的**批量清理工具**，本批已补齐。剩余未做收敛为：**Tier-C 资金/交易类(订单/核销/结算流水/账单)仅存档不硬删** + **多级审批链**。

### 11.3 酒吲门店建模纠正（V267，进行中）
§10.交互层 曾提"酒吲门店可新建"。**用户 2026-07-07 拍板纠正:酒吲门店就是商户本身**——酒吲=业态 merchant_type='bar' 的商户，经招商/商户准入正路引进，不该凭空"新建门店"。故：删除此前误建的独立 `bars` 门店主数据表+管理页+BarVenue 全套（V263），`bar_id` 语义改为酒吲商户的 merchant_id；新建卡座/商品/套餐的"酒吲"从选门店改为**选酒吲商户**；V267 先把老 bar_id seed 成酒吲商户再 DROP bars（不丢历史）。**教训:实体建模决策要带"业务来路"视角(这东西正常怎么引进/是谁的下级)，别只给技术建模角度，否则易造出绕过正规业务流程的平行概念。**


## 12. 后台 UI / 交互 / 主题打磨（2026-07-07，均已合并 origin/main 并部署测试机）

本轮不改业务规则，只提升后台可用性；权限门控 / 审计留痕 / 表单校验 / data-testid 一律保持不变。

### 12.1 创建流单屏化（PR #76）
`GuidedFlowModal` 加 `singlePage`（默认开）：各步字段并到一屏、一次跑完各步校验、保留确认页核对。全站创建弹窗一律「一屏填 + 复核」；仅"后步要用前步结果做判断"的传 `singlePage={false}` 走逐步（目前无）。另新增更薄的单页表单弹窗 `FormModal`（一屏表单、无步进条），用于风控预警 / 库存移动等 2–3 字段场景。

### 12.2 四种交互形式（替代"满屏按钮"）
- **「操作▾」行内下拉 `RowActions`**（PR #70/#71/#74/#75）：多动作表格行收进下拉，按权限过滤后 0=灰显—/1=直接按钮/2+=下拉；危险动作红。已铺票务 / 设施 / 合同 / 酒吧商品·套餐 / 商户 / 权限授权 / 薪资等全部多动作表格页。
- **按钮三级层次**（PR #77）：主（暗色填充）/ 次（描边 `.admin-secondary-button`）/ 文字（`.admin-text-button`）三级视觉层次。
- **表格图标按钮 `IconButton`**（PR #77）：单动作用图标 + 悬停提示（@ant-design/icons + antd Tooltip），命中区 36×36，保留 aria-label 可访问性。
- **开关 `PermissionSwitch`**（PR #77）：布尔开关设置（第三方开关页 / 短信通知开关页）用 antd Switch 替代「开启/关闭」按钮，权限门控（无权限禁用、fail-safe 不触发切换）。

### 12.3 触摸友好（PR #72/#73）
下拉菜单项 / 选择项 ≥44px、表格按钮 ≥36px、表单输入 40px；下拉点击触发（非悬停）。设备定调：管理后台基本电脑用（平板另有独立平板端），但现场也可能触摸屏，故所有交互按触摸尺寸做。

### 12.4 深色模式（PR #78 + 修 #79）
右上角 🌙/☀ 切换按钮（localStorage 记忆选择，首次进入跟随系统 `prefers-color-scheme`）；`<html>` 打 `data-theme`；`:root[data-theme="dark"] .admin-shell/.admin-auth-shell` 覆盖全套 `--adm-*` 令牌（强调色用暖金 `#d99a2b`，呼应服务业暖调、与浅色中性深色区分）；引入 `--adm-on-accent` 令主按钮字色随主题翻转；antd 组件经 `ConfigProvider` 切 `defaultAlgorithm ↔ darkAlgorithm` 一并主题化（含 portal 内 Modal/Dropdown/Switch/Tabs）。**坑：`--adm-*` 令牌定义在 `.admin-shell` 而非 `:root`，深色覆盖须写 `:root[data-theme=dark] .admin-shell` 提高特异性，否则被更近的 `.admin-shell` 本地令牌遮蔽（内容区不变暗，只有 antd portal 变暗）。**


## 13. 全站引导页对抗式 UI/UX 复核 + 修复(2026-07-07)

对 39 个后台页做对抗式真机复核(Playwright 无头 · 1440 浅/深 + 375 · 打开创建弹窗 · 对比度/console/横滚探针),并修复发现项。

### 13.1 覆盖与方法
- **平台 32 页**(深度 14 + 广度 18)以 admin 直审。
- **账号态守卫 7 页**(商户专区 5 + 外包 2)用**临时账号 + 临时数据法**审计,全程可逆、审后清零:
  - 商户:存原 `password_hash` → 临时改成 admin 的 hash + 向 `user_permission_overrides` 插 `grant` 权限 → 插 `merchant_bills`/`contracts`/`contract_settlements`/`merchant_fee_notification_recipient` 各一行(scope=商户 merchant-1)→ 审填充后布局 → 删数据/删覆盖/还原 hash。
  - 外包:库中无 `outsourced_vendor` 账号,临时建 `external_partner_orgs` + 外包账号(**`organization_id` = 项目范围**,`requireProjectScope` 读的就是它)+ 凭证 + 权限 + 插 `site_entry_reports`(`project_id=organization_id` 且 `vendor_user_id` 匹配才显)→ 审 → 全删。
  - **清零校验**:9 张表 tmpaudit 残留全 0、覆盖表归 0、商户 hash 还原。
- **结论**:无 Critical/High、无发布阻断;账号态守卫 + 权限门工作正确无越权;本会话交互改造(单屏/操作▾/深色/触摸/标签)零回归。

### 13.2 已修复(PR #85 + #86,609 单测绿,真机验证生效)
1. 账号类型 `platform_admin`→**平台管理员**、`outsourced_vendor`→**外包账号**(`customer/i18n/statusLabels.ts`,admin 侧 re-export)。
2. 账单费用构成 `manual`→**手工登记**、`contract`→**合同**(`FEE_TYPE_LABELS`)。
3. 排班/考勤/退款额度/退款规则 空表加「暂无 X」占位(`colSpan={99}` + `.admin-empty-cell`),替代裸表头。
4. 能耗「启用」原生 `checkbox`→**antd Switch**(与新交互范式一致)。
5. 外包进场报备**去双重报错**(`onError` 改 no-op,只留 `AdminDataState` 内联「读取失败 + 重试」卡片,不再叠加顶部全局横幅)。
6. 顶栏窄屏(≤640px)隐藏「营业中」标签 + 用户名截断,消除长用户名下的溢出横滚。
7. 访问治理**岗位码中文化**(auditor_internal/external、outsourced_construction/facility/security_cleaning、ops_manager → 中文)+ **职级** `lvl_m<N>_<lower/upper>` → `M<N>·初级/高级`(`AccessGovernancePage` 内 `POSITION_LABELS` + `zhPosition`/`zhLevel`)。

### 13.3 本批修复(B/C/D)
- **B 角色/权限模板显示英文(数据侧)**:`roles.role_name` 被种成 `platform_admin`/`OPS_MANAGER` 等英文码 → **数据订正**为中文名(仅英文/全大写码类,按 id 语义映射)。
- **C 表格 UUID 截断不统一**:新增共享 `shortId()` 显示助手(长 id 显尾段 + 悬停看全),统一 id 列展示,消除「有的截断有的显全串」。
- **D 测试脏数据**:清理 `ticket_types` 的「B1P345 Type」占位、`service_catalog` 的 AuditDance/dbg 测试项等明显测试污染(仅按明确测试标识,不碰真实业务数据)。
