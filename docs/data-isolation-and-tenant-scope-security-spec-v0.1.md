# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.1

> 创建日期：2026-06-18
> 状态：权威安全规格（补齐此前缺口——隔离模型此前仅存在于各迁移 .sql 注释中，无单一权威文档）。
> 适用范围：开发、测试、安全、验收。本文档化的是**已实现**的迁移 V044–V052 与既有 scope 过滤行为，不引入新需求。
> 关联：盘点表 `docs/domain-ownership-scope-audit-v0.1.csv`（逐表 scope 映射，本规格正式引用并解释它）；越权必测口径 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`（"数据范围穿透"）、`docs/full-admin-ui-acceptance-matrix-v0.1.md`（UI-ADM-MER-002）。

机器识别标记：DATA_ISOLATION_SPEC_READY、TENANT_SCOPE_INJECTION_FROM_TOKEN_ONLY、SCOPE_AUDIT_CSV_AUTHORITATIVE_LINK。

> 补登（2026-06-18，附加不删原文）：迁移基线已推进到 **V061**。本规格 §3 逐表清单与 §6 缺口表已扩展登记至 V044–V061（含 V055/V057/V059/V060/V061 新增带租户数据的表）。上文「V044–V052」为初版范围声明，实际覆盖以 §3/§6 现行内容为准。

## 1. 隔离边界与 scope 语义

| scope | 含义 | 来源 | 层级 |
|---|---|---|---|
| `organization_id` | 组织/集团（角色归属）。NULL = 平台级全局角色 | 安全 token | 最高 |
| `merchant_id` | 商户（经营主体）。绝大多数业务表的隔离键 | 安全 token | 中 |
| `project_id` | 综合体/项目（如平面图） | 安全 token / 继承 | 中 |
| `venue_id` / `bar_venue_id` / `space_id` | 场地/酒吧/点位（继承到 merchant） | 继承 | 低 |

scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
- `direct`：表自带 scope 列（如 `merchant_id`），直接过滤。
- `inherited`：无自带列，通过外键 join 到带 scope 列的父表过滤。
- `single_complex_default`：单综合体默认全局可见，多综合体启用时再收紧。

## 2. 权威约定：scope 注入只能来自安全 token（**绝不取请求参数**）

迁移 V044/V046 的 .sql 注释已确立、本规格正式提升为强制条款：

> 隔离列（`merchant_id`/`organization_id`/`project_id`）的值**在写入时由调用方的安全 token 注入**，**绝不允许从请求体或查询参数读取**。读取查询必须由服务端按 token 的 scope 强制追加过滤条件，不依赖前端传参。

- 写入：Service 从 `AuthorizationService.currentMerchantScope()` / `currentActor()` 取 scope 落库。
- 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scope 为空）才走全量 `listXxx()`。参考实现：`FinanceService.listArApItems()`（`currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems`）。
- 旧行兼容：隔离列均为 **nullable**（`ADD COLUMN IF NOT EXISTS … VARCHAR`），存量旧行 `merchant_id IS NULL`；部分索引 `WHERE merchant_id IS NOT NULL` 只对已落 scope 的行建索引。**回填策略**见 §5。

## 3. 已落隔离列清单（迁移 V044–V052，逐表）

| 迁移 | 表 | 列 | 类型 | 索引/约束 |
|---|---|---|---|---|
| V044 | `ar_ap_items` | merchant_id | VARCHAR(64) | 部分索引；另加 `uq_daily_close_scope_date UNIQUE(business_date,scope_type,scope_id)` on finance_daily_closes |
| V046 | inventory 10 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
| V047 | bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills | merchant_id | VARCHAR(100) | 各部分索引 |
| V048 | `roles` | organization_id | VARCHAR(64) | 部分索引（NULL=平台全局角色）|
| V049 | amusement 4 表 | merchant_id | VARCHAR(?) | 无索引（**缺口，见 §6**）|
| V051 | `compliance_reminders` | merchant_id | VARCHAR(64) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
| V052 | `mall_floor_plans` | merchant_id | VARCHAR(64) | 部分索引 |
| V055 | `staff_level_assignments` | organization_id + merchant_id | VARCHAR(64) | 两个部分索引（org/merchant）；`staff_levels` 为平台全局参考数据 `single_complex_default`，无 scope 列 |
| V057 | `supervisor_review_decisions` | （无 scope 列） | — | 本次新增 `approval_level` / `required_approver_rank` 仅为审批留痕字段，**非隔离键**；该表仍无 organization/merchant scope 列（隔离裁决见 §6 / §7 注） |
| V059 | `business_process_instances` | merchant_id + project_id | VARCHAR(64) | `direct`；商户+综合体双键直接过滤 |
| V059 | `business_process_step_records` | （无自带列） | — | `inherited`：经 `process_instance_id` 外键 join `business_process_instances` 继承 merchant_id/project_id 过滤 |
| V059 | `service_responsibility_records` | merchant_id | VARCHAR(64) | `direct`；商户级直接过滤 |
| V060 | `customer_points_rules` | project_id | VARCHAR(64) | `direct`，综合体/项目级隔离 |
| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（**敏感字段 at-rest 加密为后续项**，见 §6 ISO-GAP） |

> 注：迁移用 retrofit 方式事后加列，类型为 `VARCHAR`；早期 dev-design 文档把 merchant_id 写成 `uuid`，以本规格与迁移现实为准（待回头修正 design 文档，P2）。完整逐表 scope 映射见 `domain-ownership-scope-audit-v0.1.csv`。

## 4. 运行时越权拦截与验收标准（Given/When/Then）

每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` 绑定）：

- **AC-ISO-1 读隔离**：
  - Given 商户 A 的员工 token，存在商户 B 的数据；
  - When 调用任一列表/详情接口；
  - Then 只返回 A 的数据；B 的数据不出现（按 direct 列过滤或 inherited join 过滤）。
- **AC-ISO-2 写隔离 / 注入不可被参数覆盖**：
  - Given 商户 A token，请求体里**伪造** `merchant_id=B`；
  - When 提交写操作；
  - Then 落库的 scope 必须是 token 的 A，请求体中的 B 被忽略（不得越权写入 B）。
- **AC-ISO-3 平台级**：Given 平台管理员（scope 空）→ 可见全量。
- **AC-ISO-4 旧行可见性**：明确 `merchant_id IS NULL` 旧行对商户角色是否可见（默认：不可见，除非显式回填）。

逐表用例应覆盖 §3 全部表。建议新增脚本闸口 `verify:tenant-isolation`（占位，见 §6）做契约级守卫。

## 5. 旧行回填策略（待执行）

- 存量 `merchant_id IS NULL` 行需按业务归属回填正确 scope，否则商户角色看不到历史数据。
- 回填后将相关部分索引/约束按需 VALIDATE（含 V046 的 `chk_stock_adjustment_type` NOT VALID → VALIDATE）。
- 回填动作应有迁移或一次性脚本 + 审计留痕；本规格不指定具体数值，由数据负责人按真实归属执行。

## 6. 仍缺口（doc/工程，已登记为本轮 P0/P1）

| 编号 | 缺口 | 级别 |
|---|---|---|
| ISO-GAP-01 | 安全 finding 台账 `security-scans/findings-register-v1.md` / `formal-security-baseline-v1.md` **无跨租户越权 finding 系列**（V044–V061 修的安全问题在权威台账不可见）→ 应补 `SEC-*-TENANT-ISOLATION-*` 条目（2026-06-18 已在 findings-register-v1.md 末尾手工补登 manual-tenant-isolation 小节，待生成器纳管）| P0 |
| ISO-GAP-02 | 无 `verify:tenant-isolation` 自动闸口；隔离目前靠人工/单测，机审基线只统计 OpenAPI auth context，验不到 DB 行级隔离（待建）| P1 |
| ISO-GAP-03 | V049 amusement 4 表的 merchant_id **无索引**（其余域均建了部分索引）→ 性能/一致性缺口 | P1 |
| ISO-GAP-04 | V044–V061 缺 TDD 风格实现/回归 plan（对齐 `superpowers/plans/2026-06-15-security-audit-fixes.md` 的 V038/V039 记录）→ 详见本节末「ISO-GAP-04 TDD 回归 plan 待补」 | P1 |
| ISO-GAP-05 | dev-design（bar/amusement/foundation）把 merchant_id 写成 `uuid`，与迁移 `VARCHAR` retrofit 不符 | P2 |
| ISO-GAP-06 | `saved_bottles.customer_phone` 等 V061 新增敏感字段 **at-rest 加密未实现**（明文存储）→ 应补加密/脱敏方案 | P1 |

> **缺口登记状态（截至 2026-06-18，迁移基线 V061）**：V053 / V054 / V055（staff_level_assignments + staff_levels）/ V057（supervisor_review_decisions）/ V059（business_process_instances + business_process_step_records + service_responsibility_records）/ V060（customer_points_rules）/ V061（saved_bottles 补字段）均已在本规格 §3 逐表清单登记，scope_type/scope_source 已标注。后续新增带租户数据的表须继续按 §8 双向绑定登记。

### ISO-GAP-04 TDD 回归 plan 待补（F7）

V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例集**，隔离行为只散落在域内单测与人工口径中，无单一权威回归清单。回归 plan 落地前，应至少覆盖以下表（按 AC-ISO-1 读隔离 / AC-ISO-2 写注入不可被参数覆盖 / AC-ISO-3 平台级 / AC-ISO-4 旧行可见性 四条）：

- V044：`ar_ap_items`（direct merchant_id）+ finance_daily_closes scope 唯一约束
- V046：inventory 10 表 + `saved_bottles`（direct merchant_id）
- V047：bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills（direct merchant_id）
- V048：`roles`（direct organization_id，NULL=平台全局）
- V049：amusement 4 表（direct merchant_id，无索引）
- V051：`compliance_reminders`（direct merchant_id）
- V052：`mall_floor_plans`（direct merchant_id）
- V055：`staff_level_assignments`（direct org+merchant）；`staff_levels` 平台全局参考无 scope（只测全量可见）
- V057：`supervisor_review_decisions`（无 scope 列 → 测审批留痕字段不被当作隔离键；隔离裁决待治理）
- V059：`business_process_instances`（direct merchant_id+project_id）、`business_process_step_records`（inherited via process_instance_id）、`service_responsibility_records`（direct merchant_id）
- V060：`customer_points_rules`（direct project_id，综合体级）
- V061：`saved_bottles`（direct merchant_id；新增 customer_phone 等敏感字段加密为 ISO-GAP-06）

该 plan 应对齐 `verify:tenant-isolation`（ISO-GAP-02）契约闸口，作为机审守卫与单测的回归底座。

## 6A. 租户隔离机审闸口与越权台账（verify:tenant-isolation）

> 修正(2026-06-19 R5业务HTML缺口审计 F8/F9/F10)：依据业务HTML/治理HTML「商户看不到其他商户 / 平台全局」，与本规格 §6 缺口登记——**ISO-GAP-02** 自认"无 `verify:tenant-isolation` 自动闸口；隔离靠人工/单测，机审基线只统计 OpenAPI auth context，验不到 DB 行级隔离（待建）"，**ISO-GAP-01** 自认权威安全 finding 台账"无跨租户越权 finding 系列"（手工补登待生成器纳管）。本章把"机审闸口 + 越权台账"补成可落地规格，闭合"机审可发现未隔离查询、越权请求被拒并留痕"。

### 6A.1 CI 机审闸口（`verify:tenant-isolation`）

闸口职责：扫描含 `merchant_id` / `project_id`（及 `organization_id` 等 scope 列，见 §1 语义）的表与查询，校验是否**强制注入 scope**，缺失即失败。

- **扫描对象：**
  - **表清单**：以 `domain-ownership-scope-audit-v0.1.csv` 为权威表清单（§8 双向绑定），逐表确认 scope_type/scope_source 已登记；新增带租户数据的表未登记 → 失败。
  - **查询/Repository**：扫描访问上述表的查询（JPA/Repository/原生 SQL），校验 scope 谓词来自**安全 token 注入**（§2 权威约定：scope 绝不取请求参数）；存在未注入 scope 的读/写查询 → 失败。
- **失败口径（fail-closed）：** 任一"应隔离表的查询缺 scope 注入""scope 来源为请求参数""新增表未在 CSV/§3 登记"→ 闸口判失败，阻断合入。
- **白名单例外：** 平台全局参考表（如 `staff_levels`、`roles` NULL=平台全局）须显式登记为"无 scope/平台全局"，方可豁免行级 scope 注入校验，避免误报。
- **与现有机审关系：** 现有机审基线只统计 OpenAPI auth context（验不到 DB 行级隔离），本闸口补 DB/查询级行级隔离校验，二者互补，不重复。

### 6A.2 越权 finding 台账（证据留痕）

- **台账落点：** 在权威安全 finding 台账（`security-scans/findings-register-v1.md` / `formal-security-baseline-v1.md`）补 `SEC-*-TENANT-ISOLATION-*` 系列条目（对齐 ISO-GAP-01），由生成器纳管，不再仅手工补登。
- **每条 finding 记录证据：** 构造的越权请求（跨商户/跨项目读或写）、token 的真实 scope、请求体中尝试越权的 scope、实际结果（被拒/被忽略写入 token scope）、对应表与用例编号（AC-ISO-1/2/3/4，见 §4、§6 ISO-GAP-04 表清单）。
- **机审发现项入台账：** `verify:tenant-isolation` 扫出的"未隔离查询/未登记表"也作为 finding 入台账并跟踪闭环，形成"机审发现 → 台账登记 → 修复 → 回归"链路。

### 6A.3 验收

- **越权被拒并留痕：** 构造跨商户、跨项目越权请求（读与写），读被隔离（看不到他人数据）、写被强制落 token 的 scope（请求体越权 scope 被忽略，见 §4 Given/When/Then），且每条用例在越权台账留下证据条目。
- **机审可发现未隔离查询：** 故意引入一条"应隔离表缺 scope 注入"或"新增表未登记 CSV/§3"的变更，`verify:tenant-isolation` 必须判失败并阻断；修复后通过。
- **回归底座对齐：** 验收用例集对齐 §6 ISO-GAP-04 表清单（V044–V061）与 AC-ISO-1/2/3/4 四条，作为机审与单测共用回归底座。

> **待业务确认：** `verify:tenant-isolation` 机审闸口纳入哪条 CI 流水（避免与现有 gate 如 `verify:openapi-closure`、安全基线机审冲突或重复阻断），由治理/工程负责人裁定接入点与阻断级别。

## 7. V050 主管复核持久化数据模型（此前仅在 .sql，本规格补字段级文档）

表 `supervisor_review_decisions`（V050 新建）：

| 列 | 类型 | 说明 |
|---|---|---|
| review_id | VARCHAR(64) PK | 复核记录主键 |
| policy_code | VARCHAR(100) NOT NULL | 命中的复核策略码 |
| action_type / object_type | VARCHAR(100) NOT NULL | 触发动作类型 / 对象类型 |
| object_id | VARCHAR(256) NOT NULL | 对象 ID |
| blocked | BOOLEAN NOT NULL | 是否默认阻断 |
| review_status | VARCHAR(50) NOT NULL | pending_review / approved / … |
| required_level | INT NOT NULL | 要求主管级别 |
| reviewer_level | INT | 实际审批人级别 |
| decision | VARCHAR(50) | 审批决定 |
| reason | TEXT | 审批意见 |
| evidence_attachment_ids | JSONB NOT NULL DEFAULT '[]' | 证据附件 |
| evaluated_at | TIMESTAMPTZ NOT NULL | 评估时间 |

索引：`idx_supervisor_review_decisions_pending` 部分索引 `WHERE review_status='pending_review'`。
业务/权限/事件口径见 `platform-governance-product-spec-v0.2.md`、OpenAPI `governance.supervisor_review.view/manage/approve`。
**隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131` 标注 "no direct scope column detected"）——需决策复核记录是否需按 organization/merchant 隔离（登记为 ISO-GAP，待治理负责人裁决）。

## 8. 字段级验收 = 与盘点 CSV 双向绑定

本规格 §3 表 ↔ `domain-ownership-scope-audit-v0.1.csv` 逐行对应。任何新增带租户数据的表，必须：(1) 在 CSV 增行并标 scope_type/scope_source；(2) 在本规格 §3 增行；(3) 补 AC-ISO-1/2 用例。CSV 不再是 orphan，由本规格正式承接解释。
