CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T02:49:03Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M029 B3-r1 修复 diff：before tree `c183dbbfa180d53322ff433fdb95f11b3f5b899a` → after tree `d639978e4c5b02b405365eba6f87dcfd73bcb8dd`；冻结 diff `/home/james/audit-tmp/m029_b3_r1.diff`（7864 bytes，SHA256 38c8981d79d45425b8ba70c1d23e4f586ee4e007bcb3b33730b4f5c33745922e），全文见 §2。定稿 ID：M029-001、002（队列「是」）。
- 顺序：Grok 已先派（modules/M029-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| data-isolation-and-tenant-scope-security-spec-v0.1.md | `-` | 001（机械重生成） | ¦ `venue_business_hours` ¦ `venue_id` ¦ inherited-or-location-s |
| data-isolation-and-tenant-scope-security-spec-v0.1.md | `-` | 001（机械重生成） | ¦ `venue_sessions` ¦ `venue_id` ¦ inherited-or-location-scope ¦ |
| data-isolation-and-tenant-scope-security-spec-v0.1.md | `+` | 001（机械重生成） | ¦ `venue_business_hours` ¦ `venue_id` ¦ direct ¦ V226 ¦ `V226__ |
| data-isolation-and-tenant-scope-security-spec-v0.1.md | `+` | 001（机械重生成） | ¦ `venue_sessions` ¦ `venue_id` ¦ direct ¦ V226 ¦ `V226__entitl |
| domain-ownership-scope-audit-v0.1.csv | `-` | 001（机械重生成） | business_process_step_records,single_complex_default,P0_SINGLE_ |
| domain-ownership-scope-audit-v0.1.csv | `+` | 001（机械重生成） | business_process_step_records,inherited,process_instance_id,pro |
| generate-data-isolation-spec.mjs | `+` | 001 | // 文件版本：V0.01 |
| generate-data-isolation-spec.mjs | `-` | 001 | function scopeType(columns) { |
| generate-data-isolation-spec.mjs | `-` | 001 |   const names = [...columns.keys()]; |
| generate-data-isolation-spec.mjs | `-` | 001 |   if (names.includes("merchant_id") ¦¦ names.includes("project_ |
| generate-data-isolation-spec.mjs | `-` | 001 |   return "inherited-or-location-scope"; |
| generate-data-isolation-spec.mjs | `+` | 001 | // 本区块只收自带 scope 列的表：按规格 §1 定义即 direct（与盘点 CSV 同口径），不发射 §1 类型闭集 |
| generate-data-isolation-spec.mjs | `+` | 001 | function scopeType() { |
| generate-data-isolation-spec.mjs | `+` | 001 |   return "direct"; |
| generate-data-isolation-spec.mjs | `-` | 001 |     lines.push(`¦ \`${row.table}\` ¦ ${colText} ¦ ${scopeType(r |
| generate-data-isolation-spec.mjs | `+` | 001 |     lines.push(`¦ \`${row.table}\` ¦ ${colText} ¦ ${scopeType() |
| generate-domain-ownership-scope-audit.mjs | `-` | 001 | // 文件版本：V0.03 |
| generate-domain-ownership-scope-audit.mjs | `+` | 001 | // 文件版本：V0.04 |
| generate-domain-ownership-scope-audit.mjs | `+` | 001 |   { pattern: /^business_process_step_records$/, source: "proces |
| verify-tenant-isolation.mjs | `+` | 002 | // 文件版本：V0.01 |
| verify-tenant-isolation.mjs | `+` | 002 |   // IS [NOT] DISTINCT FROM 是比较运算符而非表引用；不中和会把其后的别名（如 d.merchant |
| verify-tenant-isolation.mjs | `+` | 002 |   sql = sql.replace(/\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b/gi, "I |

- 5 文件 9 个 hunk、22 条 ± 行：两个生成器的改动与版本标记归 001；隔离机审的一行中和、注释与版本标记归 002；两份产物的变化（规格自动区块 2 行类型单元格、CSV 1 行）是 001 的机械重生成结果，B2 双席已同意随批带入并归 001。无归因不到的行；无 mode 变更；第四个工具、baseline JSON、规格手写段未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
index 828f5d22..f92e7cd6 100644
--- a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
+++ b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
@@ -222,8 +222,8 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | `ticket_verifications` | `merchant_id` | direct | V066 | `V066__ticket_anticounterfeit_window_nonce.sql` |
 | `turnover_rent_rules` | `merchant_id` | direct | V110 | `V110__complex_billing.sql` |
 | `user_position_assignments` | `merchant_id` + `organization_id` | direct | V169 | `V169__access_role_assignment_closure.sql` |
-| `venue_business_hours` | `venue_id` | inherited-or-location-scope | V226 | `V226__entitlement_session_kind_batch3.sql` |
-| `venue_sessions` | `venue_id` | inherited-or-location-scope | V226 | `V226__entitlement_session_kind_batch3.sql` |
+| `venue_business_hours` | `venue_id` | direct | V226 | `V226__entitlement_session_kind_batch3.sql` |
+| `venue_sessions` | `venue_id` | direct | V226 | `V226__entitlement_session_kind_batch3.sql` |
 | `voucher_definitions` | `merchant_id` | direct | V301 | `V301__unified_voucher_ledger.sql` |
 | `voucher_redemption_records` | `merchant_id` | direct | V301 | `V301__unified_voucher_ledger.sql` |
 | `warehouses` | `merchant_id` + `project_id` | direct | V085 | `V085__supply_chain_t6.sql` |
diff --git a/docs/domain-ownership-scope-audit-v0.1.csv b/docs/domain-ownership-scope-audit-v0.1.csv
index 1ab73bdf..078abd24 100644
--- a/docs/domain-ownership-scope-audit-v0.1.csv
+++ b/docs/domain-ownership-scope-audit-v0.1.csv
@@ -76,7 +76,7 @@ business_day_cutoff_config,single_complex_default,P0_SINGLE_COMPLEX,single-compl
 business_entities,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
 business_entity_bindings,direct,merchant_id,merchant_id column on business_entity_bindings,ready for project/merchant scoped query enforcement,V0.01,covered
 business_process_instances,direct,project_id,project_id column on business_process_instances,ready for project/merchant scoped query enforcement,V0.01,covered
-business_process_step_records,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
+business_process_step_records,inherited,process_instance_id,process_instance_id -> business_process_instances -> merchant_id/project_id,must join through scope path before multi-complex rollout,V0.01,covered
 business_review_locks,direct,project_id,project_id column on business_review_locks,ready for project/merchant scoped query enforcement,V0.01,covered
 channel_fee_configs,direct,project_id,project_id column on channel_fee_configs,ready for project/merchant scoped query enforcement,V0.01,covered
 complex_operating_params,direct,project_id,project_id column on complex_operating_params,ready for project/merchant scoped query enforcement,V0.01,covered
diff --git a/tools/generate-data-isolation-spec.mjs b/tools/generate-data-isolation-spec.mjs
index 24ce5246..174ed169 100644
--- a/tools/generate-data-isolation-spec.mjs
+++ b/tools/generate-data-isolation-spec.mjs
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 // docs:data-isolation-spec / verify:data-isolation-spec
 // 以 Flyway 迁移目录为事实源，生成数据隔离规格中的自动迁移隔离基线，避免 R1-FINDING-A 的静态计数漂移。
 
@@ -81,10 +82,9 @@ function parseSchema() {
   return { tables, files };
 }
 
-function scopeType(columns) {
-  const names = [...columns.keys()];
-  if (names.includes("merchant_id") || names.includes("project_id") || names.includes("organization_id")) return "direct";
-  return "inherited-or-location-scope";
+// 本区块只收自带 scope 列的表：按规格 §1 定义即 direct（与盘点 CSV 同口径），不发射 §1 类型闭集之外的值（M029-001）。
+function scopeType() {
+  return "direct";
 }
 
 function renderBlock() {
@@ -102,7 +102,7 @@ function renderBlock() {
     const colText = cols.map((c) => `\`${c.column}\``).join(" + ");
     const versions = [...new Set(cols.flatMap((c) => [...c.versions]))].sort((a, b) => Number(a.slice(1)) - Number(b.slice(1)));
     const sources = [...new Set(cols.flatMap((c) => [...c.sources]))].sort();
-    lines.push(`| \`${row.table}\` | ${colText} | ${scopeType(row.columns)} | ${versions.join(", ")} | ${sources.map((s) => `\`${s}\``).join("<br>")} |\n`);
+    lines.push(`| \`${row.table}\` | ${colText} | ${scopeType()} | ${versions.join(", ")} | ${sources.map((s) => `\`${s}\``).join("<br>")} |\n`);
   }
   lines.push(`\n> 自动基线说明：本区块只证明“迁移中出现了 scope 字段”，不替代 \`verify:tenant-isolation\` 对 Repository scope 过滤的机审，也不替代越权 E2E/单测。\n`);
   lines.push(END);
diff --git a/tools/generate-domain-ownership-scope-audit.mjs b/tools/generate-domain-ownership-scope-audit.mjs
index 23c57f4c..d2b756d0 100644
--- a/tools/generate-domain-ownership-scope-audit.mjs
+++ b/tools/generate-domain-ownership-scope-audit.mjs
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import { readFileSync, writeFileSync, existsSync } from "node:fs";
 import { readdirSync } from "node:fs";
 import { join } from "node:path";
@@ -24,6 +24,7 @@ const inheritedRules = [
   { pattern: /^staff_locker_records$/, source: "staff_profile_id", path: "staff_profile_id -> staff_profiles -> current authenticated employee; cabinet and slot identify storage location" },
   { pattern: /^staff_locker_scan_tokens$/, source: "staff_profile_id", path: "staff_profile_id -> staff_profiles -> employee company/merchant scope; one-time scan token presented to the kiosk (U05-006)" },
   { pattern: /^staff_shift_covers$/, source: "shift_id", path: "shift_id -> staff_shifts -> staff_profiles company/merchant scope; cover_user_id must be in the same scope (U06-004)" },
+  { pattern: /^business_process_step_records$/, source: "process_instance_id", path: "process_instance_id -> business_process_instances -> merchant_id/project_id" },
   { pattern: /^bar_order_items$/, source: "order_id", path: "order_id -> bar_orders -> table_id/bar_id -> venue/project scope" },
   { pattern: /^bar_bill/, source: "bill/order linkage", path: "bill/order linkage -> bar_orders -> venue/project scope" },
   { pattern: /^bar_production_ticket/, source: "order_id", path: "order_id -> bar_orders -> venue/project scope" },
diff --git a/tools/verify-tenant-isolation.mjs b/tools/verify-tenant-isolation.mjs
index f5433cac..c260a2db 100644
--- a/tools/verify-tenant-isolation.mjs
+++ b/tools/verify-tenant-isolation.mjs
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 // verify:tenant-isolation
 // 租户隔离机审闸口（对齐 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md §6A.1，
 // 闭合 ISO-GAP-02「无 verify:tenant-isolation 自动闸口」）。
@@ -145,6 +146,8 @@ function extractReferencedTables(sql, cteNames) {
   // EXTRACT(<unit> FROM <expr>) 的 FROM 是函数语法而非表引用；不中和会把 open_time/close_time
   // 等列名(EXTRACT(EPOCH FROM open_time))误判为未登记表。抽取前就地中和该语法 FROM。
   sql = sql.replace(/\bEXTRACT\s*\(\s*\w+\s+FROM\b/gi, "EXTRACT(");
+  // IS [NOT] DISTINCT FROM 是比较运算符而非表引用；不中和会把其后的别名（如 d.merchant_id 的 d）误判为未登记表（M029-002）。
+  sql = sql.replace(/\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b/gi, "IS_DISTINCT");
   const tables = new Set();
   const patterns = [
     new RegExp(`\\bFROM\\s+${TABLE_TOKEN}`, "gi"),
```

## 3. 内容审计

- 001：`scopeType()` 恒返回 `direct` 并注明依据，调用处同步；修后自动区块类型只剩 direct（GREEN 日志），与 CSV 对同一批表的分类一致。CSV 生成器新增规则与既有规则同形；修后 CSV 该行为 inherited、scope_source=process_instance_id。
- 002：在 EXTRACT 中和之后加一行 `IS [NOT] DISTINCT FROM` 中和；修后 --strict 不再出现 `表 'd'`，其余 strict finding 为既有已登记缺口（GREEN 日志计数）。默认路径 exit 0；baseline 里的 `d` 键现会被提示为「已不再复现」，属 M030。
- 重生成后两个 --check、隔离机审默认路径、测试库门禁、全链 verify:predeploy 均 exit 0（一次性 worktree，origin/main + 这 5 个文件）。
- 修前/修后检查原文：
```
[RED]
spec auto-block scope types:  168 direct; 2 inherited-or-location-scope;
RED: 规格自动区块有 2 行类型值不在规格 §1 闭集（direct/inherited/single_complex_default）内
csv business_process_step_records: single_complex_default,P0_SINGLE_COMPLEX ; csv scope types:  173 direct; 7 global_reference; 79 inherited; 76 single_complex_default;
RED: CSV 把 business_process_step_records 归为 single_complex_default，规格手写清单写明它经 process_instance_id 继承
--strict exit=1 ; phantom table 'd' findings: 2 ; all strict findings: 4
RED: 隔离机审把 IS NOT DISTINCT FROM 后的别名 d 当成未登记表（2 条）
after regeneration: spec --check=0 csv --check=0 tenant-isolation(default)=0 test-db=0
changed files in worktree: 
problems=3
[GREEN]
spec auto-block scope types:  170 direct;
csv business_process_step_records: inherited,process_instance_id ; csv scope types:  173 direct; 7 global_reference; 80 inherited; 75 single_complex_default;
--strict exit=1 ; phantom table 'd' findings: 0 ; all strict findings: 2
after regeneration: spec --check=0 csv --check=0 tenant-isolation(default)=0 test-db=0
changed files in worktree:  M docs/data-isolation-and-tenant-scope-security-spec-v0.1.md; M docs/domain-ownership-scope-audit-v0.1.csv; M tools/generate-data-isolation-spec.mjs; M tools/generate-domain-ownership-scope-audit.mjs; M tools/verify-tenant-isolation.mjs;
problems=0
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m029_b3r1_run.log）

```
START=2026-09-21T02:48:41Z HEAD=e456ba96 origin/main=6f3d25d7
check OK 7 subs in 3 files
outputs equal origin/main before fix: 0 (0 = equal)
BEFORE_TREE=c183dbbfa180d53322ff433fdb95f11b3f5b899a
== RED: original tools
RED_CHECK_EXIT=1
spec auto-block scope types:  168 direct; 2 inherited-or-location-scope;
RED: 规格自动区块有 2 行类型值不在规格 §1 闭集（direct/inherited/single_complex_default）内
csv business_process_step_records: single_complex_default,P0_SINGLE_COMPLEX ; csv scope types:  173 direct; 7 global_reference; 79 inherited; 76 single_complex_default;
RED: CSV 把 business_process_step_records 归为 single_complex_default，规格手写清单写明它经 process_instance_id 继承
--strict exit=1 ; phantom table 'd' findings: 2 ; all strict findings: 4
RED: 隔离机审把 IS NOT DISTINCT FROM 后的别名 d 当成未登记表（2 条）
after regeneration: spec --check=0 csv --check=0 tenant-isolation(default)=0 test-db=0
changed files in worktree: 
problems=3
scratch reset clean after RED
== apply patch in main worktree
wrote tools/generate-data-isolation-spec.mjs c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba
wrote tools/generate-domain-ownership-scope-audit.mjs 47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19
wrote tools/verify-tenant-isolation.mjs 56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f
syntax ok tools/generate-data-isolation-spec.mjs
syntax ok tools/generate-domain-ownership-scope-audit.mjs
syntax ok tools/verify-tenant-isolation.mjs
== GREEN: patched tools copied into the scratch worktree
GREEN_CHECK_EXIT=0
spec auto-block scope types:  170 direct;
csv business_process_step_records: inherited,process_instance_id ; csv scope types:  173 direct; 7 global_reference; 80 inherited; 75 single_complex_default;
--strict exit=1 ; phantom table 'd' findings: 0 ; all strict findings: 2
after regeneration: spec --check=0 csv --check=0 tenant-isolation(default)=0 test-db=0
changed files in worktree:  M docs/data-isolation-and-tenant-scope-security-spec-v0.1.md; M docs/domain-ownership-scope-audit-v0.1.csv; M tools/generate-data-isolation-spec.mjs; M tools/generate-domain-ownership-scope-audit.mjs; M tools/verify-tenant-isolation.mjs;
problems=0
== full verify:predeploy in the scratch worktree (patched tools + regenerated outputs)
PREDEPLOY_EXIT=0
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
== bring regenerated outputs back to the main worktree
 tools/generate-data-isolation-spec.mjs                     | 10 +++++-----
 tools/generate-domain-ownership-scope-audit.mjs            |  3 ++-
 tools/verify-tenant-isolation.mjs                          |  3 +++
 5 files changed, 13 insertions(+), 9 deletions(-)
scratch restored clean
scratch removed
AFTER_TREE=d639978e4c5b02b405365eba6f87dcfd73bcb8dd
files changed: docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv tools/generate-data-isolation-spec.mjs tools/generate-domain-ownership-scope-audit.mjs tools/verify-tenant-isolation.mjs 
DIFF bytes=7864 sha256=38c8981d79d45425b8ba70c1d23e4f586ee4e007bcb3b33730b4f5c33745922e
 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md |  4 ++--
 docs/domain-ownership-scope-audit-v0.1.csv                 |  2 +-
 tools/generate-data-isolation-spec.mjs                     | 10 +++++-----
 tools/generate-domain-ownership-scope-audit.mjs            |  3 ++-
 tools/verify-tenant-isolation.mjs                          |  3 +++
 5 files changed, 13 insertions(+), 9 deletions(-)
hunks=9 files=5
tools/generate-data-isolation-spec.mjs 140 6650 c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba
tools/generate-domain-ownership-scope-audit.mjs 233 14340 47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19
tools/verify-tenant-isolation.mjs 339 16841 56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 372 42095 d0474060434d1b91b48c003c9f623de42ff6b3791fc8af8291cb0a322e53cf3e
docs/domain-ownership-scope-audit-v0.1.csv 336 60163 1122f92a09ed43ed0c084e75ef022066fe880e30b843d5450f0911e664c9aa0b
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T02:48:49Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
