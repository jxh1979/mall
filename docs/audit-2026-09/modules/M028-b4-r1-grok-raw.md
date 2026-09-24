CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（文档与接口定义模块；M028 FILES 闭集 2 文件，本 diff 只改其中规格文档）
findings 正本：`docs/audit-2026-09/findings.md` V2.89，已定稿且归属「是」：M028-001…010（010 冲突，其余细化）；GR-06 只登记、不得混入
独立性：本席未读 `M028-b4-r1-self-raw.md`（`ls` 见该路径存在，本会话未打开）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 日志 / 持久临时产物。一次性 `git worktree add --detach` 基于 origin/main 核生成器后已 `git worktree remove --force`，无残留目录。旧 B1/B2 可对照（`M028-b1-grok-raw.md` V0.01+V0.02、findings V2.89）；本轮以 V2.89 定稿最小处置为准。检查脚本 `/home/james/audit-tmp/m028_check.py` 只读复跑。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程、不写 `docs/arc/audits/`。
3. `graft`：`graft grep "listArApItemsPaged"`（6 hits / 3 files）与 `graft grep "findComprehensiveServiceFee"`（5 hits / 3 files）。tokens saved ≈ 49,863 + 21,470 = 71,333。证明 001/007 点名方法可从当前仓复现。
4. `grilling` / `grill-me`：对抗线自问自答；本体「等用户 / 派子代理」作废。
5. `ponytail-review`：只找可删的多余抽象；不改码。结论见 §6。
6. `verification-before-completion`：PASS 前当场复跑 `m028_check.py`（before blob 16 红 / 工作区 after 0 绿）、两树 `git diff --binary` 与冻结 diff `cmp`、自动区块 SHA、origin/main worktree 两生成器 `--check` 与 `verify:entry-anchors`、`complex_test` 只读 SELECT。
7. `systematic-debugging` / `diagnosing-bugs`：只借读错误→复现→追数据流。红环＝对 before blob 跑同一检查（16 处）；绿环＝对 after / 工作区。本体 instrumentation / 最小改码试假说作废。本步不修。
8. `tdd` / `test-driven-development`：本 diff 是规格手写段；160 §6 文档类不强制落测试文件。加测试类会改他文件，闸 3 越界。类型 4 用既有 `m028_check.py` + 源码/迁移/库/其他文档复现。
9. `requesting-code-review`：B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
10. `codebase-design`：本模块两文件是浅模块（隔离语义、谓词、表清单几乎等于全文）。本 diff 只校正手写声称，不改 Interface、不加深 locality、不把手写事实迁进生成器（M029）。
11. `writing-for-agents`：本 raw 给两席落齐后的下游席读；步骤 + 完成标准（两轴票）写死。
12. `long-running-background-tasks`：已读；本步无长任务、无 monitor。
13. `i-have-adhd`：存在（`disable-model-invocation: true`），未开持久模式。编号、无寒暄；本文件第一行仍必须是判据锁哈希。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`（一次性 detach 是 D-028 允许的只读核，不是该技能本体）、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts` 无合并。

缺口（实找无独立目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 全文一行与 ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值，仅作旁证，不以它们代替实算。`LOCK-AMEND-COUNT: 1` 只读见到，未改。findings 文首「版本：V2.89」。

冻结 diff：
```
wc -c /home/james/audit-tmp/m028_b3_r1.diff → 20181
sha256sum → 8f3af045f3008273b08cdf0915bc54e06a11c300e3fa4f45725d79ae50901fa2
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `cc096d3ffa4999cf80dc6ed025fcafe07e45bd3d`
- after  `c08263bda933e7769db142af135ab204deae164e`

```
git diff --binary <before> <after> | sha256sum
→ 8f3af045f3008273b08cdf0915bc54e06a11c300e3fa4f45725d79ae50901fa2
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m028_b3_r1.diff → CMP_OK
git diff --stat：1 file changed, 18 insertions(+), 17 deletions(-)
git diff --name-status：仅 M docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
hunks=10（冻结 diff 10 个 @@）
+/- 实数：+18 / -17（净 +1 行；文件 371→372）
```

blob / mode：
- before `442fe33c64e774f074aa2fbfbf2097db96252d0e` mode `100644` 内容 SHA256 `c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad`（371 行）
- after  `828f5d22cdce128908c8be0f159733681bebf1c7` mode `100644` 内容 SHA256 `e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9`（372 行 / 42137 bytes）
- 工作区 `git hash-object` = after blob `828f5d22…`；`cmp` 工作区与 after blob 相同。两树 mode 均为 `100644`。分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=b99a9e5deba0bca6fd053a62f18bf1e9bd382d44`。

自动区块（`<!-- DATA_ISOLATION_SPEC_AUTO_START -->` … `END`，after L57–L234）：
- before / after / 工作区 SHA256 均为 `81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335`
- 与派发「修前修后一致」声明一致。手写段之外无变更。

CSV：`docs/domain-ownership-scope-audit-v0.1.csv` 两树 diff 0 字节；工作区 SHA256 `d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa`。HTML 两树无名。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.89 归属「是」的 ID（M028-001…010）；归不到即越界。GR-06 与其余只登记项不得出现在变更集。

模块闭集：`plan/modules.md` M028 FILES = 规格文档 + CSV。`git diff --name-only`＝仅规格文档 1 文件（CSV 按定稿不改）。

环境（D-028）：主工作区迁移含未发布 V427，在主工作区 cwd 跑生成器 `--check` 必红，不作证据。本席第一次误在主工作区 cwd 调用 worktree 内脚本，SPEC/CSV `--check` 均 exit 1（预言命中）；改 `cd` 进 origin/main worktree 后两生成器与条目锚点均 exit 0。以第二次为准。

---

## 2. 定稿最小处置（审计标尺，摘 V2.89）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M028-001 | 细化 | 是 | 括号内改为「取 `currentMerchantScope()` 与 `currentProjectScopeQuietly()` 两维范围，下推 `repository.listArApItemsPaged(merchantId, projectId, …)`；平台口径两者皆空＝全量」 |
| M028-002 | 细化 | 是 | 去行号：L238 改「`p0-openapi-operation-acceptance-matrix-v0.1.md`「5. 边界与异常场景」表「数据范围穿透」行」；L342 改「`domain-ownership-scope-audit-v0.1.csv` 的 `supervisor_review_decisions` 行」。不换新行号 |
| M028-003 | 细化 | 是 | 四处改成迁移原值（「inventory 9 表」×2、VARCHAR(80)、VARCHAR(64)） |
| M028-004 | 细化 | 是 | ISO-GAP-06 行改为「**已关闭**：存酒手机号列已按 PiiCipher 加密落库（写入点 `JdbcInventoryRepository`），V396 补 `enc:v1:` 信封 CHECK」、级别 Closed；§3 V061 括注改为「敏感字段 at-rest 加密已落地，见 §6 ISO-GAP-06」 |
| M028-005 | 细化 | 是 | 拆两类谓词；删「一律」与「对常规数据语义等价，但」；禁止哨兵两条理由原样保留。不把 12/23 处写入规格 |
| M028-006 | 细化 | 是 | §8 改述：CSV 是全部表的 scope 分类闭集、§3A 是迁移中出现 scope 列的表的自动基线、§3 只作背景不参与对应；新增表须 (1) 运行 `npm run docs:domain-ownership-scope` 与 `npm run docs:data-isolation-spec` 重生成（不手改），(2) 补 AC-ISO-1/2。末句「CSV 不再是 orphan…」保留 |
| M028-007 | 细化 | 是 | 证据句改为「证据见 `JdbcSettlementBillRepository` 配置读取方法（`findComprehensiveServiceFee`）上的同一教训注释」 |
| M028-008 | 细化 | 是 | 两个小标题各加「V140 初版」；不可变清单注明 cam 两表已随 V274 删除；两份清单后加「以上为 V140 初版清单，非现行闭集；后续迁移（如 V148 `supervisor_review_decisions`）继续追加，现行受保护表以库内调用 `complex_block_mutation()` 的触发器为准」。不重列现行闭集 |
| M028-009 | 细化 | 是 | L370「详见 `platform-governance-prd-v0.1.md` §4」、L371「需求口径见 `platform-governance-prd-v0.1.md` §4.1」 |
| M028-010 | 冲突 | 是 | 审计人员「可见/可操作范围」单元格改为「内部稽核：本综合体全部数据只读；外部审计/监管：只读划定的范围与时间段；两类都不能修改任何业务数据；查看企微原文/视频证据/财务敏感信息属敏感操作」。不改 HTML |

文首 V0.02→V0.03：B2 写入集合明文「同批把文首版本 V0.02→V0.03」；本席 B2-r1（`M028-b1-grok-raw.md` V0.02）「**文首 V0.02 → V0.03：同意属同批最小处置。**」。归因到本批 001–010 伴随元数据，不是无 ID 新缺陷。

只登记不入队（不得混进本 diff）：GR-06 ISO-GAP-04 的 `superpowers/plans/2026-06-15-security-audit-fixes.md` 裸路径；§3／§3A／CSV 三表分类差异（M029）；handovers/steps/exceptions 简写；PRD §4.1 标题「代码待落地」；V427 环境差。本 diff 未碰这些。

---

## 3. diff 全文（冻结件 20181 bytes，与 `git diff --binary` 字节级相同）

```
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
 
@@ -274,7 +274,7 @@ V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例
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

index 行两侧均为 `100644`：普通源文件 mode 保留，无 mode hunk。自动区块不在任何 hunk 的 `+`/`-` 集。

---

## 4. diff 逐块归因（闸 3）

`+`/`-` 行闭集＝18 插入 + 17 删除。上下文行无 `+`/`-`，不入归因。

### 4.1 H01 `@@ -1,4 +1,4`

| 行 | 内容 | ID |
|---|---|---|
| `-# …规格 V0.02` | 删过期文首版本 | 随批（B2 写入集合 + 本席 B2「同意属同批最小处置」；001–010 全部改手写段） |
| `+# …规格 V0.03` | 文首升版 | 同上 |

### 4.2 H02 `@@ -30,7 +30,7`

| 行 | 内容 | ID |
|---|---|---|
| `-…isPresent() ? listArApItemsByMerchant : listArApItems` | 删已不在 `FinanceService.listArApItems()` 的三元式 | M028-001 |
| `+…两维范围，下推 listArApItemsPaged…两者皆空＝全量` | 定稿括注原文 | M028-001 |

### 4.3 H03 `@@ -38,11 +38,11`

| 行 | 内容 | ID |
|---|---|---|
| `- V046 … inventory 10 表` | 删错误表数 | M028-003 |
| `+ V046 … inventory 9 表` | 迁移原值 | M028-003 |
| `- V049 … VARCHAR(?)` | 删未知列宽 | M028-003 |
| `+ V049 … VARCHAR(64)` | V049 原文 | M028-003 |
| `- V051 … VARCHAR(64)` | 删错误列宽 | M028-003 |
| `+ V051 … VARCHAR(80)` | V051 原文 | M028-003 |

表头 5 列未动；三行仍 5 列。

### 4.4 H04 `@@ -50,7 +50,7`

| 行 | 内容 | ID |
|---|---|---|
| `-…at-rest 加密为后续项…ISO-GAP` | 删过期「后续项」 | M028-004 |
| `+…加密已落地，见 §6 ISO-GAP-06` | 定稿括注原文 | M028-004 |

### 4.5 H05 `@@ -235,7 +235,7`

| 行 | 内容 | ID |
|---|---|---|
| `-…matrix-v0.1.md:272-273` | 删漂移行号（现为票务接口行） | M028-002 |
| `+…「5. 边界与异常场景」表「数据范围穿透」行` | 定稿不漂移指称 | M028-002 |

### 4.6 H06 `@@ -265,7 +265,7`

| 行 | 内容 | ID |
|---|---|---|
| `- ISO-GAP-06 …加密未实现… P1` | 删过期 P1 | M028-004 |
| `+ ISO-GAP-06 **已关闭**…enc:v1: 信封 CHECK \| Closed` | 定稿原文 + 级别列 Closed | M028-004 |

ISO-GAP-04 行在 hunk 上下文、无 `+`/`-`（GR-06 未混入）。表仍 3 列。

### 4.7 H07 `@@ -274,7 +274,7`

| 行 | 内容 | ID |
|---|---|---|
| `- V046：inventory 10 表` | 第二处错误表数 | M028-003 |
| `+ V046：inventory 9 表` | 迁移原值 ×2 之第二处 | M028-003 |

### 4.8 H08 `@@ -301,7 +301,7`

同一物理行承载 005 + 007（定稿就是改「该句」）。

| 行 | 内容 | ID |
|---|---|---|
| `-…读路径一律写…对常规数据语义等价，但 ①…②…证据见 SEC-P2-…001` | 删「一律」+「语义等价」+ 悬空证据 ID | M028-005 + M028-007 |
| `+…分两类写…两类均禁止 COALESCE…①…②…证据见 JdbcSettlementBillRepository…findComprehensiveServiceFee` | 拆两类 + 保留哨兵理由 + 替换证据 | M028-005 + M028-007 |

### 4.9 H09 `@@ -339,19 +339,20`

| 行 | 内容 | ID |
|---|---|---|
| `-…csv:131…` | 删漂移行号（现为 facility_inspection_templates） | M028-002 |
| `+…csv 的 supervisor_review_decisions 行…` | 定稿不漂移指称 | M028-002 |
| `-本规格 §3 表 ↔ CSV 逐行对应…(1)(2)(3)…` | 删不可复现的逐行对应 | M028-006 |
| `+CSV 是闭集，§3A 是自动基线…npm run 两生成器…(2) 补用例。CSV 不再是 orphan…` | 定稿改述；末句保留 | M028-006 |
| `-不可变表…15 张）`（无 V140 初版、无 V274） | 现在时闭集口径 | M028-008 |
| `+不可变表…V140 初版 15 张；其中 cam 两表已随 V274 删除` | 小标题 + V274 注明 | M028-008 |
| `-永久留痕表…15 张）` | 现在时闭集口径 | M028-008 |
| `+永久留痕表…V140 初版 15 张` | 小标题 | M028-008 |
| `+以上为 V140 初版清单，非现行闭集；…V148 supervisor_review_decisions…complex_block_mutation()` | 定稿要求的追加句 | M028-008 |

### 4.10 H10 `@@ -363,9 +364,9`

| 行 | 内容 | ID |
|---|---|---|
| `-审计人员…跨域只读留痕/审计/复核记录…` | 比 HTML 窄的单元格 | M028-010 |
| `+审计人员…内部稽核：本综合体全部数据只读；…` | 定稿单元格原文 | M028-010 |
| `-…详见治理 PRD §4…` | 未指明文件 | M028-009 |
| `+…详见 platform-governance-prd-v0.1.md §4…` | 定稿定位 | M028-009 |
| `-…需求口径见治理 PRD §4。` | 未指明文件且节号应是 4.1 | M028-009 |
| `+…需求口径见 platform-governance-prd-v0.1.md §4.1。` | 定稿定位 | M028-009 |

§10 表仍 5 列 3 数据行；只改审计人员第三单元格，身份/明确不可/落地承接未动。

### 4.11 归因结论

18 插入、17 删除均可落到 V2.89 归属「是」的 ID（文首版本＝B2 授权随批）。无「顺手优化 / 手改自动区块 / 改 CSV / 改 HTML / 改生成器 / 改仓储 / 把 GR-06 或只登记项写入 / 重列 append-only 闭集 / 把 12/23 处写入规格」。  
`git diff --name-only` 闭集＝规格文档 1 文件 ⊂ M028 FILES。  
**归因票：PASS。**

---

## 5. 内容审计

### 5.1 三线（本步范围＝该 diff，不是整模块重审）

**第一性原理**
1. 删掉这些改写，V2.89 十条「是」仍红：过期参考实现、漂移行号、错误表数/列宽、已落地加密仍标未实现、「一律」谓词、§3↔CSV 逐行对应、悬空证据 ID、已 DROP 的 cam 表现在时清单、未指明文件的治理 PRD、比 HTML 窄的审计人员范围。必须存在。
2. 声称的现行事实：下列命令/源码/迁移/库/其他文档已核（类型 4）。
3. 未验证前提：无。本席未改生产代码。主工作区生成器 `--check` 因 V427 必红，已改在 origin/main worktree 核。

**顺从式**  
假定 B3 意图正确，逐 ID 对照最小处置原文与 after 对应句，见 5.3。

**对抗式（grilling 自答）**
- Q：文首版本无独立 ID，是否越界？→ 否。V2.89 B2 写入集合与本席 B2「同意属同批最小处置」写死。修后若仍标 V0.02 本身即 GEN-016。
- Q：H08 一行改了两条 ID，是否把 007 塞进 005 越界？→ 否。007 的悬空 ID 就在 005 所改的那一句末；定稿就是改该句。
- Q：005 多写了「scope 列为 NULL 的旧行按 §4 AC-ISO-4 默认不可见」是否发明？→ 定稿已写「旧行 NULL 按 AC-ISO-4 不可见」；加「§4」是同文档指针，不是新规则。未写入 12/23 处。
- Q：006「二者均由生成器维护」是否超出最小处置？→ 文首/§3A 已声明生成器维护；restatement。`package.json` 确有两脚本。
- Q：008 把 V274 注写进不可变表小标题而非「清单后」？→ 定稿要「注明」cam 已删；标题内注明满足。清单后另有完整追加句。未重列 41 张。
- Q：010 是否改了 HTML？→ 两树 `*.html` 无名。单元格前三句＝治理地图 L676；尾句＝原单元格已有敏感操作句。
- Q：是否手改自动区块/CSV？→ 自动区块 SHA 修前修后相同；CSV 两树 0 字节；CSV SHA 仍 `d935779d…`。
- Q：GR-06 是否混入？→ after ISO-GAP-04 行与 before 逐字相同，仍含裸路径 `superpowers/plans/2026-06-15-security-audit-fixes.md`。
- Q：表格列数？→ 6 张表（含未改的 §1/§3A/§7）表头列数＝数据行列数；§3 仍 5 列 14 行；ISO-GAP 仍 3 列；§10 仍 5 列 3 行。
- Q：是否业务发明？→ 001–009 是文档过期校正；010 是把单元格拉回 HTML 原文。未新造隔离谓词、未改加密算法、未改 PRD 过期标题、未改迁移。

### 5.2 改后声称 vs 现行源码/迁移/库/其他文档

**001** `FinanceService.java:162-175`（origin/main 与工作区同文）：`currentMerchantScope().orElse(null)` + `currentProjectScopeQuietly().orElse(null)` → `repository.listArApItemsPaged(merchantId, projectId, keyword, page, size)`。注释「平台口径两者皆 null=全量」。after 括注与定稿逐字相同。`isPresent() ? listArApItemsByMerchant` 在 after 手写段 0 命中。

**002** `p0-openapi-operation-acceptance-matrix-v0.1.md` L791 `## 5. 边界与异常场景`，L798 `| 数据范围穿透 | … | 每个查询和写入接口必须按 project/merchant/…过滤 |`。L272–273 确为票务接口行（`confirmTicketRefundPayout` / `approveTicketRefund`），旧行号失效。CSV L281 `supervisor_review_decisions,… no direct scope column detected`；L131 为 `facility_inspection_templates`。after 手写段无 `` `….md:digits` `` / `` `….csv:digits` ``。

**003** `V046__inventory_security_fixes.sql`：`^ALTER TABLE inventory_\w+ ADD COLUMN`＝9（items/suppliers/purchase_requests/stock_balances/stock_movements/stock_adjustments/stock_batches/risk_alerts/supplier_reconciliations）+ 另表 `saved_bottles`。`V049` 四表 `merchant_id VARCHAR(64)`。`V051` `merchant_id VARCHAR(80)`。after「inventory 9 表」×2；V049 64；V051 80。`inventory 10 表` / `VARCHAR(?)` 0 命中。

**004** `JdbcInventoryRepository.java:1306` `piiCipher.encrypt(savedBottle.customerPhone())`；`JdbcInventoryRowMapper.java:285` 解密。`V396__pii_phone_envelope_check_widen.sql` 头注「下列五列自 ISO-GAP-06 起已按 PiiCipher 加密落库」并 `CHECK (… LIKE 'enc:v1:%')`。`complex_test`：`chk_saved_bottles_customer_phone_pii` / `chk_saved_bottles_transfer_to_phone_pii` 存在；定义为 `NULL OR '' OR ~~ 'enc:v1:%'`。after ISO-GAP-06 句与定稿逐字相同，级别 Closed；V061 括注与定稿逐字相同。`at-rest 加密未实现` / `为后续项` 0 命中。

**005** 两类谓词现行都在：参数级 `(:merchant_id IS NULL OR merchant_id = :merchant_id)` 例 `JdbcFinanceRepository.java:168-169`；列级 `(project_id IS NULL OR project_id = :project_id)` 例 `JdbcSettlementBillRepository.java:632-633`。after 删「一律」「对常规数据语义等价」；COALESCE 公式与 ①② 理由原样；未写 12/23。

**006** after §3 历史 14 行、§3A 自动 170 行、CSV 335 数据行，并不逐行对应。`package.json`：`docs:domain-ownership-scope` / `docs:data-isolation-spec`。末句「CSV 不再是 orphan，由本规格正式承接解释。」保留。`逐行对应` 0 命中。

**007** `JdbcSettlementBillRepository.java:624-626` 方法注释：项目维改用 `(project_id IS NULL OR project_id = :project_id)`，原 COALESCE 哨兵会把真实 id 恰为 `__DEFAULT__` 的行对所有项目可见。`findComprehensiveServiceFee` 在 L628。after 证据句与定稿逐字相同。`SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001` 0 命中。

**008** `V140__append_only_enforcement_and_runtime_role.sql` immutable 15 含 cam 两表、delete_only 15。`V274__drop_abolished_cam_tables.sql` L6–7 `DROP TABLE IF EXISTS cam_trueup_lines/runs`。`V148__supervisor_review_decisions_append_only.sql` 给该表建 `complex_block_mutation` DELETE 触发器。`complex_test`：`to_regclass(cam_*)` 空；`complex_block_mutation` 触发器 41；`supervisor_review_decisions` 有该触发器。after 未把 41 写成新清单。handovers/steps/exceptions 简写未展开（只登记）。

**009** `platform-governance-prd-v0.1.md` L153 `## 4. 角色与使用场景`（含外包/审计人员）；L174 `### 4.1 外包供应商自助下属账号管理（需求口径，代码待落地）`。`platform-governance-product-spec-v0.2.md` L105 `## 4. 治理总览页`——旧「治理 PRD §4」会指错文件。after 两处带文件名。PRD §4.1 过期标题未在本模块改（只登记）。手写段无未加反引号的「治理 PRD §4」。

**010** `platform-governance-map-for-operations-v0.1.html:676`：「审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。」after 单元格与定稿逐字相同。HTML 不在 diff。

### 5.3 按 ID 核最小处置

001–010 after 对应句均含定稿要求的确切文字（005/006 在定稿句意内的少量连接词不构成新声称）。文首 V0.03。十条「是」均落地。

### 5.4 禁区

- 三份需求 HTML：两树 diff 无名。010 不改 HTML。
- 其他文件：`git diff --name-only` 仅本规格。CSV 未动。
- 自动区块：SHA 修前修后一致。
- GR-06 / 只登记项：ISO-GAP-04 路径未改；未重列闭集；未改 PRD 标题；未改生成器。
- mode：`100644`→`100644`。
- 业务发明：无。

### 5.5 类型 4 当场复跑（verification-before-completion）

修前（本席对 before blob 跑与 `m028_check.py` 同逻辑，不改 before 树）：
```
title: …规格 V0.02
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
RED ×16（001 三元式；002 两处行号；003 四处；004 加密后续项；005 一律+语义等价；006 逐行对应；007 悬空 ID；008 cam 未注+未标初版；009 治理 PRD；010 范围过窄）
problems=16
```
与 `/home/james/audit-tmp/m028_b3r1_red.log` 16 条原文一致（该日志 SHA256 `910de2e387f1c3dce9e8b90a05891b1a703473e3095335e74584275759e74fd9`）。

修后本席当场：
```
python3 /home/james/audit-tmp/m028_check.py
title: …规格 V0.03
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
problems=0
CHECK_EXIT=0
```
与 `/home/james/audit-tmp/m028_b3r1_green.log` 同结论（SHA256 `58f6fb01f208d71088a2e71f5a8fba09144ac1668d24333fcd7bd1352ca0a0ed`）。

origin/main 一次性 worktree（已删除；cwd＝worktree；拷入 after 规格 SHA `e5125166…`）：
```
SPEC_CHECK_EXIT=0  「data isolation spec is in sync with Flyway migration scope baseline.」
CSV_CHECK_EXIT=0   「DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified」
ANCHOR_GATE_EXIT=0 「[entry-anchors] PASS：…正文无行号锚点…」
V427 in WT=0；V427 in main=1
```
与 `m028_b3r1_gen_spec.log` / `m028_b3r1_gen_csv.log` / `m028_b3r1_anchor.log` 同结论。本席第一次在主工作区 cwd 误跑得红，不作证据。

`complex_test` 只读（`-h 127.0.0.1 -U complex`，口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD` 读取且不回显）：cam 两表 `to_regclass` 空；触发器 41；存酒手机号 CHECK 存在且 `enc:v1:`；`supervisor_review_decisions` 有 `complex_block_mutation`。无任何写语句。

执行日志 `/home/james/audit-tmp/m028_b3r1_run.log`（SHA256 `a82d046dc6cb531d212f54ff121911e2a5831aea03c92f1e60db9c958e4cf991`）：BEFORE/AFTER tree、diff sha `8f3af045…`、1 file 18+/17-、hunks=10、after 内容 SHA `e5125166…`、`other files touched: 0`、csv unchanged `d935779d…` 与本席实算一致。本席不把该日志当唯一证据。

检查脚本 SHA256 `c2836c72b1754493b6f4ceeef63f5e95f7607073506b32a8824f41771f703a21`（只读，未改）。

**内容票：PASS。**

---

## 6. ponytail-review（只读）

无额外可删抽象。文首升版是 B2 指定随批，不是膨胀。手写段按定稿替换/补一句（008 净 +1 行）。自动区块、CSV、生成器、仓储、HTML 零改。  
`net: -0 lines possible.`（相对定稿最小集）  
Lean already. Ship.

---

## 7. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | PASS | 18+/17- 均可归 M028-001…010 或 B2 授权随批文首版本；无无 ID 行；闭集 1 文件；自动区块/CSV/HTML/GR-06 未入变更集 |
| 内容 | PASS | 十条定稿最小处置落地且可从现行源码/迁移/库/其他文档复现；无业务发明；表格列数完好；检查脚本红 16→绿 0；worktree 两生成器与条目锚点 exit 0 |
| 本席 B4-r1 | PASS | 两轴均 PASS；任一 FAIL 即 FAIL，此处未触发 |

本票不是模块 PASS，不是授权改归属。B4 双 PASS 仍须席甲本次完整 raw 合取。本席未读 self-raw。

步骤：B4-r1。模块 M028。B3 次数＝本轮审的 r1。未写 findings/ledger。
graft 本轮 2 次：tokens saved ≈ 71,333。
