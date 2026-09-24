CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T01:37:15Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M028 B3-r1 修复 diff：before tree `cc096d3ffa4999cf80dc6ed025fcafe07e45bd3d` → after tree `c08263bda933e7769db142af135ab204deae164e`；冻结 diff `/home/james/audit-tmp/m028_b3_r1.diff`（20181 bytes，SHA256 8f3af045f3008273b08cdf0915bc54e06a11c300e3fa4f45725d79ae50901fa2），全文见 §2。定稿 ID：M028-001…010（队列「是」）；GR-06 只登记。
- 顺序：Grok 已先派（modules/M028-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| ± | 归因 ID | 行首 70 字 |
|---|---|---|
| `-` | 版本行(001–010同批) | # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.02 |
| `+` | 版本行(001–010同批) | # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03 |
| `-` | 001 | - 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scop |
| `+` | 001 | - 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scop |
| `-` | 003 | ¦ V046 ¦ inventory 10 表 + `saved_bottles` ¦ merchant_id ¦ VARCHAR(64) |
| `+` | 003 | ¦ V046 ¦ inventory 9 表 + `saved_bottles` ¦ merchant_id ¦ VARCHAR(64)  |
| `-` | 003 | ¦ V049 ¦ amusement 4 表 ¦ merchant_id ¦ VARCHAR(?) ¦ 部分索引已补（V065，ISO-G |
| `-` | 003 | ¦ V051 ¦ `compliance_reminders` ¦ merchant_id ¦ VARCHAR(64) ¦ 部分索引；`u |
| `+` | 003 | ¦ V049 ¦ amusement 4 表 ¦ merchant_id ¦ VARCHAR(64) ¦ 部分索引已补（V065，ISO- |
| `+` | 003 | ¦ V051 ¦ `compliance_reminders` ¦ merchant_id ¦ VARCHAR(80) ¦ 部分索引；`u |
| `-` | 004 | ¦ V061 ¦ `saved_bottles` ¦ merchant_id（V046 已加） ¦ VARCHAR(64) ¦ `dire |
| `+` | 004 | ¦ V061 ¦ `saved_bottles` ¦ merchant_id（V046 已加） ¦ VARCHAR(64) ¦ `dire |
| `-` | 002 | 每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix- |
| `+` | 002 | 每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix- |
| `-` | 004 | ¦ ISO-GAP-06 ¦ `saved_bottles.customer_phone` 等 V061 新增敏感字段 **at-rest |
| `+` | 004 | ¦ ISO-GAP-06 ¦ **已关闭**：存酒手机号列已按 PiiCipher 加密落库（写入点 `JdbcInventoryRepo |
| `-` | 003 | - V046：inventory 10 表 + `saved_bottles`（direct merchant_id） |
| `+` | 003 | - V046：inventory 9 表 + `saved_bottles`（direct merchant_id） |
| `-` | 005/007 | - **读路径谓词写法（2026-07-28 P0-D 教训）：** 带 scope 列的表，读路径一律写 `(<scope_col> I |
| `+` | 005/007 | - **读路径谓词写法（2026-07-28 P0-D 教训）：** 带 scope 列的表，读路径分两类写：业务数据表写 `(:<sco |
| `-` | 002 | **隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131 |
| `+` | 002 | **隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv` 的  |
| `-` | 006 | 本规格 §3 表 ↔ `domain-ownership-scope-audit-v0.1.csv` 逐行对应。任何新增带租户数据的表，必 |
| `+` | 006 | `domain-ownership-scope-audit-v0.1.csv` 是全部表的 scope 分类闭集，§3A 是迁移中出现 s |
| `-` | 008 |   - **不可变表（UPDATE+DELETE 双锁，15 张）**：`audit_events`、`evidence_events`、 |
| `-` | 008 |   - **永久留痕表（仅 DELETE 锁，保留幂等 UPDATE，15 张）**：`evidence_reviews`/`eviden |
| `+` | 008 |   - **不可变表（UPDATE+DELETE 双锁，V140 初版 15 张；其中 `cam_trueup_lines`、`cam_t |
| `+` | 008 |   - **永久留痕表（仅 DELETE 锁，保留幂等 UPDATE，V140 初版 15 张）**：`evidence_reviews` |
| `+` | 008 |   - 以上为 V140 初版清单，非现行闭集；后续迁移（如 V148 `supervisor_review_decisions`）继续追 |
| `-` | 010 | ¦ **审计人员** ¦ 平台员工，单独审计角色 ¦ 跨域**只读**留痕/审计/复核记录（按授权综合体范围）；查看企微原文/视频证据/财 |
| `+` | 010 | ¦ **审计人员** ¦ 平台员工，单独审计角色 ¦ 内部稽核：本综合体全部数据只读；外部审计/监管：只读划定的范围与时间段；两类都不能修 |
| `-` | 009 | - **落地现状（2026-06-30 更新，纠正旧"待落地核对"措辞）**：外包与审计已落地——外包账号类型 `outsourced_v |
| `-` | 009 | - **自助下属层（已全部落地，2026-07-02 回写）**：外部协作机构自助开通/停用下属账号、平台设下属账号上限、账号有效期随合同 |
| `+` | 009 | - **落地现状（2026-06-30 更新，纠正旧"待落地核对"措辞）**：外包与审计已落地——外包账号类型 `outsourced_v |
| `+` | 009 | - **自助下属层（已全部落地，2026-07-02 回写）**：外部协作机构自助开通/停用下属账号、平台设下属账号上限、账号有效期随合同 |

- 1 文件 10 个 hunk、35 条 ± 行全部命中 001–010 或随批版本行（§6A.1 同一物理行承载 005 与 007 两条处置）；无归因不到的行；自动区块 SHA 不变、CSV 未动、其他文件 0。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
index 442fe33c..828f5d22 100644
--- a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
+++ b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
@@ -1,4 +1,4 @@
-# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.02
+# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03
 
 > 创建日期：2026-06-18
 > 状态：权威安全规格（补齐此前缺口——隔离模型此前仅存在于各迁移 .sql 注释中，无单一权威文档）。
@@ -30,7 +30,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 > 隔离列（`merchant_id`/`organization_id`/`project_id`）的值**在写入时由调用方的安全 token 注入**，**绝不允许从请求体或查询参数读取**。读取查询必须由服务端按 token 的 scope 强制追加过滤条件，不依赖前端传参。
 
 - 写入：Service 从 `AuthorizationService.currentMerchantScope()` / `currentActor()` 取 scope 落库。
-- 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scope 为空）才走全量 `listXxx()`。参考实现：`FinanceService.listArApItems()`（`currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems`）。
+- 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scope 为空）才走全量 `listXxx()`。参考实现：`FinanceService.listArApItems()`（取 `currentMerchantScope()` 与 `currentProjectScopeQuietly()` 两维范围，下推 `repository.listArApItemsPaged(merchantId, projectId, …)`；平台口径两者皆空＝全量）。
 - 旧行兼容：隔离列均为 **nullable**（`ADD COLUMN IF NOT EXISTS … VARCHAR`），存量旧行 `merchant_id IS NULL`；部分索引 `WHERE merchant_id IS NOT NULL` 只对已落 scope 的行建索引。**回填策略**见 §5。
 
 ## 3. 历史人工隔离列清单（初版快照，仅作背景）
@@ -38,11 +38,11 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | 迁移 | 表 | 列 | 类型 | 索引/约束 |
 |---|---|---|---|---|
 | V044 | `ar_ap_items` | merchant_id | VARCHAR(64) | 部分索引；另加 `uq_daily_close_scope_date UNIQUE(business_date,scope_type,scope_id)` on finance_daily_closes |
-| V046 | inventory 10 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
+| V046 | inventory 9 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
 | V047 | bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills | merchant_id | VARCHAR(100) | 各部分索引 |
 | V048 | `roles` | organization_id | VARCHAR(64) | 部分索引（NULL=平台全局角色）|
-| V049 | amusement 4 表 | merchant_id | VARCHAR(?) | 部分索引已补（V065，ISO-GAP-03 已关闭）|
-| V051 | `compliance_reminders` | merchant_id | VARCHAR(64) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
+| V049 | amusement 4 表 | merchant_id | VARCHAR(64) | 部分索引已补（V065，ISO-GAP-03 已关闭）|
+| V051 | `compliance_reminders` | merchant_id | VARCHAR(80) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
 | V052 | `mall_floor_plans` | merchant_id | VARCHAR(64) | 部分索引 |
 | V055 | `staff_level_assignments` | organization_id + merchant_id | VARCHAR(64) | 两个部分索引（org/merchant）；`staff_levels` 为平台全局参考数据 `single_complex_default`，无 scope 列 |
 | V057 | `supervisor_review_decisions` | （无 scope 列） | — | 本次新增 `approval_level` / `required_approver_rank` 仅为审批留痕字段，**非隔离键**；该表仍无 organization/merchant scope 列（隔离裁决见 §6 / §7 注） |
@@ -50,7 +50,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | V059 | `business_process_step_records` | （无自带列） | — | `inherited`：经 `process_instance_id` 外键 join `business_process_instances` 继承 merchant_id/project_id 过滤 |
 | V059 | `service_responsibility_records` | merchant_id | VARCHAR(64) | `direct`；商户级直接过滤 |
 | V060 | `customer_points_rules` | project_id | VARCHAR(64) | `direct`，综合体/项目级隔离 |
-| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（**敏感字段 at-rest 加密为后续项**，见 §6 ISO-GAP） |
+| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（敏感字段 at-rest 加密已落地，见 §6 ISO-GAP-06） |
 
 > 注：迁移用 retrofit 方式事后加列，类型为 `VARCHAR`；早期 dev-design 文档把 merchant_id 写成 `uuid`，以本规格与迁移现实为准（待回头修正 design 文档，P2）。完整逐表 scope 映射见 `domain-ownership-scope-audit-v0.1.csv`。
 
@@ -235,7 +235,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 
 ## 4. 运行时越权拦截与验收标准（Given/When/Then）
 
-每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` 绑定）：
+每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix-v0.1.md`「5. 边界与异常场景」表「数据范围穿透」行绑定）：
 
 - **AC-ISO-1 读隔离**：
   - Given 商户 A 的员工 token，存在商户 B 的数据；
@@ -265,7 +265,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | ISO-GAP-03 | **已关闭（V065）**：`V065__amusement_merchant_isolation_indexes.sql` 已给 amusement 4 表 merchant_id 建部分索引（原「无索引」P1 为过期标注，2026-07-02 回写并经库内索引核实） | Closed |
 | ISO-GAP-04 | V044–V061 缺 TDD 风格实现/回归 plan（对齐 `superpowers/plans/2026-06-15-security-audit-fixes.md` 的 V038/V039 记录）→ 详见本节末「ISO-GAP-04 TDD 回归 plan 待补」 | P1 |
 | ISO-GAP-05 | dev-design（bar/amusement/foundation）把 merchant_id 写成 `uuid`，与迁移 `VARCHAR` retrofit 不符 | P2 |
-| ISO-GAP-06 | `saved_bottles.customer_phone` 等 V061 新增敏感字段 **at-rest 加密未实现**（明文存储）→ 应补加密/脱敏方案 | P1 |
+| ISO-GAP-06 | **已关闭**：存酒手机号列已按 PiiCipher 加密落库（写入点 `JdbcInventoryRepository`），V396 补 `enc:v1:` 信封 CHECK | Closed |
 
 > **缺口登记状态（截至 2026-06-18，迁移基线 V061）**：V053 / V054 / V055（staff_level_assignments + staff_levels）/ V057（supervisor_review_decisions）/ V059（business_process_instances + business_process_step_records + service_responsibility_records）/ V060（customer_points_rules）/ V061（saved_bottles 补字段）均已在本规格 §3 逐表清单登记，scope_type/scope_source 已标注。后续新增带租户数据的表须继续按 §8 双向绑定登记。
 
@@ -274,7 +274,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例集**，隔离行为只散落在域内单测与人工口径中，无单一权威回归清单。回归 plan 落地前，应至少覆盖以下表（按 AC-ISO-1 读隔离 / AC-ISO-2 写注入不可被参数覆盖 / AC-ISO-3 平台级 / AC-ISO-4 旧行可见性 四条）：
 
 - V044：`ar_ap_items`（direct merchant_id）+ finance_daily_closes scope 唯一约束
-- V046：inventory 10 表 + `saved_bottles`（direct merchant_id）
+- V046：inventory 9 表 + `saved_bottles`（direct merchant_id）
 - V047：bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills（direct merchant_id）
 - V048：`roles`（direct organization_id，NULL=平台全局）
 - V049：amusement 4 表（direct merchant_id，部分索引已补 V065）
@@ -301,7 +301,7 @@ V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例
   - **查询/Repository**：扫描访问上述表的查询（JPA/Repository/原生 SQL），校验 scope 谓词来自**安全 token 注入**（§2 权威约定：scope 绝不取请求参数）；存在未注入 scope 的读/写查询 → 失败。
 - **失败口径（fail-closed）：** 任一"应隔离表的查询缺 scope 注入""scope 来源为请求参数""新增表未在 CSV/§3 登记"→ 闸口判失败，阻断合入。
 - **白名单例外：** 平台全局参考表（如 `staff_levels`、`roles` NULL=平台全局）须显式登记为"无 scope/平台全局"，方可豁免行级 scope 注入校验，避免误报。
-- **读路径谓词写法（2026-07-28 P0-D 教训）：** 带 scope 列的表，读路径一律写 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`，**禁止**用 `COALESCE(<scope_col>, '__DEFAULT__') IN (...)` 哨兵比对。后者对常规数据语义等价，但 ① 机审识别不出 scoped 通道（会误报 F1）；② 依赖写面 rejectSentinel 保证「没有真 id 叫 `__DEFAULT__`」——一旦撞名，该行对**所有**调用方可见 = 真跨租户读（唯一索引只挡顶层默认槽，商户维不挡）。证据见 `SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001`。
+- **读路径谓词写法（2026-07-28 P0-D 教训）：** 带 scope 列的表，读路径分两类写：业务数据表写 `(:<scope> IS NULL OR <scope_col> = :<scope>)`（调用方无该维范围才全量；scope 列为 NULL 的旧行按 §4 AC-ISO-4 默认不可见）；以 NULL 表示平台默认槽的配置类表写 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`。两类均**禁止**用 `COALESCE(<scope_col>, '__DEFAULT__') IN (...)` 哨兵比对：① 机审识别不出 scoped 通道（会误报 F1）；② 依赖写面 rejectSentinel 保证「没有真 id 叫 `__DEFAULT__`」——一旦撞名，该行对**所有**调用方可见 = 真跨租户读（唯一索引只挡顶层默认槽，商户维不挡）。证据见 `JdbcSettlementBillRepository` 配置读取方法（`findComprehensiveServiceFee`）上的同一教训注释。
 - **与现有机审关系：** 现有机审基线只统计 OpenAPI auth context（验不到 DB 行级隔离），本闸口补 DB/查询级行级隔离校验，二者互补，不重复。
 
 ### 6A.2 越权 finding 台账（证据留痕）
@@ -339,19 +339,20 @@ V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例
 
 索引：`idx_supervisor_review_decisions_pending` 部分索引 `WHERE review_status='pending_review'`。
 业务/权限/事件口径见 `platform-governance-product-spec-v0.2.md`、OpenAPI `governance.supervisor_review.view/manage/approve`。
-**隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131` 标注 "no direct scope column detected"）——需决策复核记录是否需按 organization/merchant 隔离（登记为 ISO-GAP，待治理负责人裁决）。
+**隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv` 的 `supervisor_review_decisions` 行标注 "no direct scope column detected"）——需决策复核记录是否需按 organization/merchant 隔离（登记为 ISO-GAP，待治理负责人裁决）。
 
 ## 8. 字段级验收 = 与盘点 CSV 双向绑定
 
-本规格 §3 表 ↔ `domain-ownership-scope-audit-v0.1.csv` 逐行对应。任何新增带租户数据的表，必须：(1) 在 CSV 增行并标 scope_type/scope_source；(2) 在本规格 §3 增行；(3) 补 AC-ISO-1/2 用例。CSV 不再是 orphan，由本规格正式承接解释。
+`domain-ownership-scope-audit-v0.1.csv` 是全部表的 scope 分类闭集，§3A 是迁移中出现 scope 列的表的自动基线，二者均由生成器维护；§3 历史人工清单只作背景，不参与对应。任何新增带租户数据的表，必须：(1) 运行 `npm run docs:domain-ownership-scope` 与 `npm run docs:data-isolation-spec` 重生成 CSV 与 §3A（不手改）；(2) 补 AC-ISO-1/2 用例。CSV 不再是 orphan，由本规格正式承接解释。
 
 ## 9. append-only 防篡改与受限运行时角色（V140）
 
 审计/证据/流水/留痕表的不可变性由 DB 层硬强制，不依赖应用自觉：
 
 - **触发器（全环境生效，对 superuser/owner 亦触发）**：通用阻断函数 `complex_block_mutation()`，UPDATE/DELETE 命中即抛 `COMPLEX_APPEND_ONLY_VIOLATION`。
-  - **不可变表（UPDATE+DELETE 双锁，15 张）**：`audit_events`、`evidence_events`、`customer_stored_value_transactions`、`deposit_ledger`、`risk_alert_actions`、`ar_ap_payments`、`integration_robot_task_events`、`terminal_device_status_events`、`inventory_purchase_request_events`、`integration_video_access_audits`、`wecom_access_audits`、`cam_trueup_lines`、`cam_trueup_runs`、`integration_read_ledger`、`permission_template_usage_logs`。
-  - **永久留痕表（仅 DELETE 锁，保留幂等 UPDATE，15 张）**：`evidence_reviews`/`evidence_attachments`、`ar_ap_item_sources`、`bar_production_ticket_events`、`open_api_events`、`inventory_stock_movements`、`finance_reconciliation_differences`/`batches`、`ticket_payment_intents`/`finance_payment_intents`、`inventory_trace_handovers`/`steps`/`exceptions`、`ticket_refund_lines`、`customer_points_ledger`。
+  - **不可变表（UPDATE+DELETE 双锁，V140 初版 15 张；其中 `cam_trueup_lines`、`cam_trueup_runs` 已随 V274 删除）**：`audit_events`、`evidence_events`、`customer_stored_value_transactions`、`deposit_ledger`、`risk_alert_actions`、`ar_ap_payments`、`integration_robot_task_events`、`terminal_device_status_events`、`inventory_purchase_request_events`、`integration_video_access_audits`、`wecom_access_audits`、`cam_trueup_lines`、`cam_trueup_runs`、`integration_read_ledger`、`permission_template_usage_logs`。
+  - **永久留痕表（仅 DELETE 锁，保留幂等 UPDATE，V140 初版 15 张）**：`evidence_reviews`/`evidence_attachments`、`ar_ap_item_sources`、`bar_production_ticket_events`、`open_api_events`、`inventory_stock_movements`、`finance_reconciliation_differences`/`batches`、`ticket_payment_intents`/`finance_payment_intents`、`inventory_trace_handovers`/`steps`/`exceptions`、`ticket_refund_lines`、`customer_points_ledger`。
+  - 以上为 V140 初版清单，非现行闭集；后续迁移（如 V148 `supervisor_review_decisions`）继续追加，现行受保护表以库内调用 `complex_block_mutation()` 的触发器为准。
 - **CASCADE 中止**：删父业务行的级联删除触达受保护子表即触发 trigger 整事务回滚——等价 `ON DELETE CASCADE → RESTRICT` 的资金/审计保护，且比 FK 改造更鲁棒（未改 FK）。
 - **受限运行时角色 `complex_app`（生产）**：迁移 V140 以 owner 身份创建受限 role，REVOKE 不可变表 UPDATE/DELETE、永久留痕表 DELETE；运行时连接（`application-prod.yml` 的 `spring.datasource`）改用 `complex_app`，Flyway 仍以 owner 跑 DDL。`complex_app` 无 DDL 权限，无法 `DROP TRIGGER` 或 `SET session_replication_role` 绕过。未配置 app 专用 env 时回退 owner 凭证（向后兼容）。
 - **最小权限基线**：上述受限 role + append-only 触发器纳入安全基线检查项（`formal-security-baseline`）。
@@ -363,9 +364,9 @@ V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例
 | 主体 | 身份归属 | 可见 / 可操作范围 | 明确不可 | 落地承接 |
 |---|---|---|---|---|
 | **外包 / 检修单位** | 独立角色账号（账号类型 `outsourced_vendor`，区别于仅作台账的供货供应商记录），不进员工组织树 | 仅派给本单位/本人的工单/巡检/进场报备上下文（`data_scope=assignee`，角色 OUTSOURCED_FACILITY/OUTSOURCED_SECURITY_CLEANING/OUTSOURCED_CONSTRUCTION，细权限均为 `*_assigned`）；按授权范围只读或限定写 | 平台全局、其他商户/项目数据、财务与客人敏感信息、跨工单批量数据 | 已落地（V178 外包账号+facility assignee 强制、V179 进场报备）；检修记录须主管确认才生效，禁止巡检员/检修方单人闭环 |
-| **审计人员** | 平台员工，单独审计角色 | 跨域**只读**留痕/审计/复核记录（按授权综合体范围）；查看企微原文/视频证据/财务敏感信息属敏感操作 | 任何业务写操作、绕过 append-only 改记录、无审批查看敏感原文 | 已落地（V180 两级只读角色：内审 complex 全量只读 / 外审 project 限期只读，0 写权）；查看敏感原文须最小授权+审批并写访问审计（留痕复核节）；审计/证据表 append-only（见 §9） |
+| **审计人员** | 平台员工，单独审计角色 | 内部稽核：本综合体全部数据只读；外部审计/监管：只读划定的范围与时间段；两类都不能修改任何业务数据；查看企微原文/视频证据/财务敏感信息属敏感操作 | 任何业务写操作、绕过 append-only 改记录、无审批查看敏感原文 | 已落地（V180 两级只读角色：内审 complex 全量只读 / 外审 project 限期只读，0 写权）；查看敏感原文须最小授权+审批并写访问审计（留痕复核节）；审计/证据表 append-only（见 §9） |
 | **商户外包员工** | 归属某商户，受该商户授权 | 仅该商户授权业务范围（同商户侧边界） | 跨商户、平台全局（同 §1 商户隔离） | 复用商户侧 scope（§1/§2 token 注入 scope，绝不取请求参数） |
 
 - **统一原则**：三类主体一律走 §2「scope 只来自安全 token」，不得以请求参数放大范围；其访问（含被拒）按 HTML 留痕复核要求写审计（外包检修动作、审计人员敏感查看）。
-- **落地现状（2026-06-30 更新，纠正旧"待落地核对"措辞）**：外包与审计已落地——外包账号类型 `outsourced_vendor` + 三角色（OUTSOURCED_FACILITY / OUTSOURCED_SECURITY_CLEANING / OUTSOURCED_CONSTRUCTION）assignee 级数据范围强制（list 按 assignee 过滤、写操作 FORBIDDEN、跨商户/平台拒绝），审计两级只读角色（V180）；权限点已在 `full-admin-page-permission-field-matrix`（外包工作台 ADM-VND 页），详见治理 PRD §4，verify:full-admin-matrix 已绿。外包账号**独立于供货供应商台账**（供货供应商只作采购/对账对象、零登录账号）。
-- **自助下属层（已全部落地，2026-07-02 回写）**：外部协作机构自助开通/停用下属账号、平台设下属账号上限、账号有效期随合同周期到期自动停用、下属继承机构范围、机构查看下属工作完成情况（`PartnerSubordinateCompletionService` + subordinates-completion 端点 + `ADM-VND-004` 页）均已落地；原「尚未实现」为过期标注。需求口径见治理 PRD §4。
+- **落地现状（2026-06-30 更新，纠正旧"待落地核对"措辞）**：外包与审计已落地——外包账号类型 `outsourced_vendor` + 三角色（OUTSOURCED_FACILITY / OUTSOURCED_SECURITY_CLEANING / OUTSOURCED_CONSTRUCTION）assignee 级数据范围强制（list 按 assignee 过滤、写操作 FORBIDDEN、跨商户/平台拒绝），审计两级只读角色（V180）；权限点已在 `full-admin-page-permission-field-matrix`（外包工作台 ADM-VND 页），详见 `platform-governance-prd-v0.1.md` §4，verify:full-admin-matrix 已绿。外包账号**独立于供货供应商台账**（供货供应商只作采购/对账对象、零登录账号）。
+- **自助下属层（已全部落地，2026-07-02 回写）**：外部协作机构自助开通/停用下属账号、平台设下属账号上限、账号有效期随合同周期到期自动停用、下属继承机构范围、机构查看下属工作完成情况（`PartnerSubordinateCompletionService` + subordinates-completion 端点 + `ADM-VND-004` 页）均已落地；原「尚未实现」为过期标注。需求口径见 `platform-governance-prd-v0.1.md` §4.1。
```

## 3. 内容审计（逐条对定稿最小处置与现状）

- 001 括号内改为「取 currentMerchantScope() 与 currentProjectScopeQuietly() 两维范围，下推 repository.listArApItemsPaged(…)；平台口径两者皆空＝全量」，与 `FinanceService.java:162-172` 一致。
- 002 两处行号改为不漂移的指称：验收矩阵「5. 边界与异常场景」表「数据范围穿透」行（该节与该行存在）；CSV 的 `supervisor_review_decisions` 行（存在且含所引标注）。
- 003 V046「inventory 9 表」×2、V049 VARCHAR(64)、V051 VARCHAR(80)，与迁移原文一致（检查脚本直接对迁移文本比）。
- 004 ISO-GAP-06 改为已关闭并写明承接；§3 V061 行括注同步。与 `JdbcInventoryRepository:1306`、V396 一致。
- 005/007 §6A.1 拆为业务数据表（参数级）与配置默认槽（列级）两类，删「一律」「语义等价」，禁止哨兵的两条理由保留；证据指向改为 `JdbcSettlementBillRepository.findComprehensiveServiceFee` 上的注释（存在，L624-626）。
- 006 §8 改述三者关系与新增表两步；引用的两个 npm 脚本存在；末句保留。
- 008 两个小标题标「V140 初版」、注明 cam 两表已随 V274 删除、清单后补「非现行闭集…以库内触发器为准」；未重列闭集。
- 009 两处补文件名与节号（`platform-governance-prd-v0.1.md` §4 / §4.1，均存在）。
- 010 单元格按治理地图原文改写；未改 HTML。
- 修前/修后检查原文：
```
[RED]
title: # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.02
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
RED: 001 §2 仍引用已不在 FinanceService.listArApItems() 里的三元式
RED: 002 行号引用 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273`
RED: 002 行号引用 `domain-ownership-scope-audit-v0.1.csv:131`
RED: 003 V046 写 10 表，迁移为 9
RED: 003 V046 写 10 表，迁移为 9
RED: 003 V051 列宽写 64，迁移为 80
RED: 003 V049 列宽写 ?，迁移为 64
RED: 004 存酒手机号已加密落库，文档仍标未实现/后续项
RED: 005 §6A.1 仍写「读路径一律写」列级 IS NULL 式
RED: 005 仍称两种写法语义等价
RED: 006 §8 仍称 §3 与 CSV 逐行对应
RED: 007 安全台账中不存在的证据 ID SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001
RED: 008 §9 清单含已被 V274 删除的 cam 表且未注明
RED: 008 §9 两份 15 张清单未标明为 V140 初版、非现行闭集
RED: 009 未指明文件的「治理 PRD §4」引用
RED: 010 审计人员可见范围比治理地图「内部稽核可查看本综合体全部数据」窄
problems=16
[GREEN]
title: # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
problems=0
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m028_b3r1_run.log）

```
START=2026-09-21T01:36:54Z HEAD=e456ba96 origin/main=b99a9e5d
check OK 19 subs; auto block sha 81b83030b0bf22cb
BEFORE_TREE=cc096d3ffa4999cf80dc6ed025fcafe07e45bd3d
== RED: check against old doc
RED_CHECK_EXIT=1
title: # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.02
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
RED: 001 §2 仍引用已不在 FinanceService.listArApItems() 里的三元式
RED: 002 行号引用 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273`
RED: 002 行号引用 `domain-ownership-scope-audit-v0.1.csv:131`
RED: 003 V046 写 10 表，迁移为 9
RED: 003 V046 写 10 表，迁移为 9
RED: 003 V051 列宽写 64，迁移为 80
RED: 003 V049 列宽写 ?，迁移为 64
RED: 004 存酒手机号已加密落库，文档仍标未实现/后续项
RED: 005 §6A.1 仍写「读路径一律写」列级 IS NULL 式
RED: 005 仍称两种写法语义等价
RED: 006 §8 仍称 §3 与 CSV 逐行对应
RED: 007 安全台账中不存在的证据 ID SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001
RED: 008 §9 清单含已被 V274 删除的 cam 表且未注明
RED: 008 §9 两份 15 张清单未标明为 V140 初版、非现行闭集
RED: 009 未指明文件的「治理 PRD §4」引用
RED: 010 审计人员可见范围比治理地图「内部稽核可查看本综合体全部数据」窄
problems=16
== apply
wrote data-isolation-and-tenant-scope-security-spec-v0.1.md e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9 lines 372 auto block unchanged
== GREEN: same check
GREEN_CHECK_EXIT=0
title: # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
problems=0
== generator + anchor gates with the patched file, in a disposable worktree at origin/main
SPEC_CHECK_EXIT=0
CSV_CHECK_EXIT=0
ANCHOR_GATE_EXIT=0
scratch restored clean
scratch removed
data isolation spec is in sync with Flyway migration scope baseline.
DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正�
AFTER_TREE=c08263bda933e7769db142af135ab204deae164e
other files touched: 0
csv unchanged: d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa
DIFF bytes=20181 sha256=8f3af045f3008273b08cdf0915bc54e06a11c300e3fa4f45725d79ae50901fa2
 ...solation-and-tenant-scope-security-spec-v0.1.md | 35 +++++++++++-----------
 1 file changed, 18 insertions(+), 17 deletions(-)
hunks=10 files=1
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 372 42137 e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T01:36:56Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
