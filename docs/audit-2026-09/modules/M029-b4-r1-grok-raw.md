CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（检查及生成工具模块；M029 FILES 闭集 4 文件，本 diff 改其中 3 个工具 + 两份生成物在生成器拥有的区域）
findings 正本：`docs/audit-2026-09/findings.md` V2.92，已定稿且归属「是」：M029-001、M029-002（均细化）
独立性：本席未读 `M029-b4-r1-self-raw.md`（`ls` 见该路径存在、18932 bytes、mtime Sep 20 19:49，本会话未打开）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 持久日志 / 审计临时产物。一次性 `git worktree add --detach /home/james/wt-m029-b4-grok-ro origin/main` 拷入 5 个 after 文件实跑后已 `git worktree remove --force`，目录已不存在。旧 B1/B2 可对照（`M029-b1-grok-raw.md` V0.01+V0.02）；本轮以 V2.92 定稿最小处置为准。检查脚本 `/home/james/audit-tmp/m029_check.sh` 只在该一次性 worktree 里运行。不对任何数据库执行写语句；未带 `--write-baseline`。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：已读开工路由段。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读开头；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程、不写 `docs/arc/audits/`。
3. `graft`：已读并调用。`graft grep "scopeType" --in tools/`（2 hits / 1 file，tokens saved ≈ 1,414）；`graft grep "extractReferencedTables" --in tools/`（2 hits / 1 file，≈ 3,318）；`graft grep "inheritedRules" --in tools/`（3 hits / 1 file，≈ 3,350）。合计 ≈ **8,082** tokens。证明 001/002 点名符号可从当前仓复现。
4. `grilling`：已读开头；对抗线自问自答；本体「等用户 / 派子代理」作废。
5. `ponytail-review`：已读；只找可删的多余抽象；不改码。结论见 §6。
6. `verification-before-completion`：已读。PASS 前当场：两树 `git diff --binary` 与冻结 diff `cmp`、产物手写段/三处差集实算、origin/main worktree 两生成器 `--check`、隔离机审默认与 `--strict`、测试库门禁、`m029_check.sh`、再生后产物 SHA 比对。
7. `requesting-code-review`：已读。B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
8. `long-running-background-tasks`：已读；本步 sha256 / node / worktree 均前台完成，未挂 monitor。

明确不用（提示词划掉或本步不适用；本席未另开这些 SKILL.md 正文）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`（一次性 detach 是 D-028 允许的只读核，不是该技能本体）、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts`：`test -f .git/MERGE_HEAD` → NO_MERGE。`audit-context-building` 要求派 function-analyzer 子代理，与「只允许两席」冲突，不执行。`systematic-debugging` / `tdd` / `codebase-design` / `writing-for-agents` / `diagnosing-bugs`：本步按 160 §6 表收窄口径借用（红绿环用既有检查脚本；不加测试文件、不加诊断桩），未把这些技能当独立流程跑完。

缺口（实找无独立目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。`i-have-adhd` 在环境技能列表中，本席未打开正文；本文件第一行仍必须是判据锁哈希，CLI 不回结论。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 全文一行与本席实算同值，仅作旁证，不以它们代替实算。`criteria-lock.md` 文首版本 V0.11；`LOCK-AMEND-COUNT: 1` 只读见到，未改。findings 文首「版本：V2.92」；`sha256sum docs/audit-2026-09/findings.md` → `4464f20ed05ac7f39251930efb3121925f6c7152bc4fba1629228642e93f4a0a`。

冻结 diff：
```
wc -c /home/james/audit-tmp/m029_b3_r1.diff → 7864
sha256sum → 38c8981d79d45425b8ba70c1d23e4f586ee4e007bcb3b33730b4f5c33745922e
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `c183dbbfa180d53322ff433fdb95f11b3f5b899a`
- after  `d639978e4c5b02b405365eba6f87dcfd73bcb8dd`

```
git diff --binary <before> <after> | sha256sum
→ 38c8981d79d45425b8ba70c1d23e4f586ee4e007bcb3b33730b4f5c33745922e
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m029_b3_r1.diff → CMP_OK
git diff --stat：5 files changed, 13 insertions(+), 9 deletions(-)
git diff --name-status：仅下列 5 个 M
  docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
  docs/domain-ownership-scope-audit-v0.1.csv
  tools/generate-data-isolation-spec.mjs
  tools/generate-domain-ownership-scope-audit.mjs
  tools/verify-tenant-isolation.mjs
hunks=9（冻结 diff 9 个 @@）
+/- 实数：+13 / -9
git diff --summary（mode/create/delete）：空
禁止路径（HTML / baseline JSON / 第四工具 / findings / ledger / 锁）：name-only 无匹配
```

分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=6f3d25d7bec4d1238455c91e6f2376936317980d`。`test -f .git/MERGE_HEAD` → **NO_MERGE**。

blob / mode（`git ls-tree`；两树比较）：

| 路径 | before mode/blob | after mode/blob | 工作区 hash-object vs after |
|---|---|---|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `100644` `828f5d22cdce128908c8be0f159733681bebf1c7` | `100644` `f92e7cd604feaa31320ab2ef4a97b70d765d00c0` | 相同 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | `100644` `1ab73bdfa6aa90f3d00a47e6425977764a7459fe` | `100644` `078abd2474ae6a305c2f8ca664e220a6444ee609` | 相同 |
| `tools/generate-data-isolation-spec.mjs` | `100644` `24ce52464cef58fc96c722f8a347d656dd348977` | `100644` `174ed1691c41c0c96e5ef9540f13d1db9e3ca548` | 相同 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `100644` `23c57f4c5a94638180505a4504daad21d9ce8858` | `100644` `d2b756d093cdceb23cb61a5166764bf14539e38d` | 相同 |
| `tools/verify-tenant-isolation.mjs` | `100644` `f5433cac0a22d33db0c73474fca75035ca92ef81` | `100644` `c260a2db466cf581a57102d95fe080c751501d5c` | 相同 |
| `tools/verify-test-db-isolation.mjs`（第四工具，不在 diff） | `100755` `642ebb0d3b72557cdbdf2d3dc272ceeaeb256060` | 同左（= origin/main） | 同左 |
| `security-scans/tenant-isolation-baseline.json`（M030，不在 diff） | `100644` `f75a02636ae88ac16e0abcd407a80be4985d4b23` | 同左 | — |

第四工具内容 SHA256 两树均为 `6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea`（33 行 / 1445 bytes）。baseline 两树 blob 相同，**本 diff 未改**。观察：该 baseline blob 与 `origin/main` 的 `931b651781f2a005190e0ecc2ca845c6fa2b3a8f` 不同，属主工作区既有脏树被 write-tree 两侧同时收入，不是本批变更。闸 3 只审两 tree 之差。

工作区磁盘 mode（`stat -c %a`）：两份文档与三工具 `664`，第四工具 `775`。git 树内 mode 修前修后未变；`git diff --summary` 无 mode 行。

内容 SHA256（本席对 blob 实算）：

| 路径 | before | after |
|---|---|---|
| 规格文档 | `e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9`（42137 B / 372 行） | `d0474060434d1b91b48c003c9f623de42ff6b3791fc8af8291cb0a322e53cf3e`（42095 B / 372 行） |
| CSV | `d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa`（60192 B / 336 行） | `1122f92a09ed43ed0c084e75ef022066fe880e30b843d5450f0911e664c9aa0b`（60163 B / 336 行） |
| 规格生成器 | `14d5ca6b2083890691b4ac188a597395f9f2bbe7726fc4c596e83266e3076abf`（6664 B / 140 行） | `c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba`（6650 B / 140 行） |
| CSV 生成器 | `ae8568fa8b1091f694a3462785088ee28023b757a41585298e6fa1af0ec14bfa`（14174 B / 232 行） | `47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19`（14340 B / 233 行） |
| 隔离机审 | `dfec43c3fcc65759297af89d805826fff9b7082e82615b905a00e73068d959e0`（16578 B / 336 行） | `56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f`（16841 B / 339 行） |

模块闭集：`plan/modules.md` M029 FILES = 4 个工具。第四工具 `tools/verify-test-db-isolation.mjs` 不在写入集合。两份生成物属 M028 FILES，按 V2.92 最小处置④与 B2 双席同意随本批带入、只动生成器拥有区域，归因 001。

环境（D-028）：主工作区迁移含未发布 V427（`V427__m009_project_profiles_and_associations.sql` 存在于主工作区、不存在于 origin/main）。工具实跑证据取自 origin/main 一次性 worktree（HEAD `6f3d25d7`，最新迁移 V428，无 V427）。主工作区 `--check` 不作证据。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.92 归属「是」的 ID（M029-001、002）；归不到即越界。只登记不入队项不得出现在变更集。

---

## 2. 定稿最小处置（审计标尺，摘 V2.92）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M029-001 | 细化 | 是 | ① 规格生成器 `scopeType` 改为恒返回 `direct` 并注明依据（本区块只列自带 scope 列的表，按规格 §1 即 direct，与 CSV 同口径），不再发射闭集外的值；调用处同步。② CSV 生成器 `inheritedRules` 增一条 `business_process_step_records`（source：`process_instance_id`；path：`process_instance_id -> business_process_instances -> merchant_id/project_id`）。③ 两文件版本标记（规格生成器新增 V0.01、CSV 生成器 V0.03→V0.04）。④ 机械后果：规格自动区块 2 个单元格 `inherited-or-location-scope`→`direct`（`venue_business_hours`、`venue_sessions`）、CSV 1 行 `single_complex_default`→`inherited`；只在生成器拥有的区域变化，手写段不动。不删写死的 `covered`。 |
| M029-002 | 细化 | 是 | 抽取前与 EXTRACT 同样就地中和 `\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b`（一行）并加注释；`tools/verify-tenant-isolation.mjs` 加版本标记 V0.01。baseline 里的 `d` 键清理属 M030，本模块不动。 |

只登记不入队（不得混进本 diff）：写死 `covered`；测试库门禁 cwd/后缀；`verify:test-db-isolation` 不在 delivery-gates；fail-closed 与棘轮字面差；baseline 陈旧项与两条已登记 F1（M030／台账）；扫描面外 Java；DROP/RENAME；V1000 排序；`globalTables.roles`；CSV 行内 file_version；四工具无单测；D-028/V427。本 diff 未碰这些。

B2 双席已同意 001④ 随批带入并归 001（本席 B2-r1 `M029-b1-grok-raw.md` V0.02：「写入集合＝本模块 3 个工具文件 + 两份生成物在生成器拥有区域的机械重生成」；findings V2.92 同句）。

---

## 3. diff 全文（冻结件 7864 bytes，与 `git diff --binary` 字节级相同）

```
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

---

## 4. diff 逐块归因表（闸 3；每一 `+`/`-` 内容行）

计数核：5 文件、9 hunk、+13 / -9。下表覆盖全部 22 条内容行。无「无 ID」行。

| # | 文件 | hunk | 行（+/-） | 归属 ID | 对应最小处置 | 越界？ |
|---|---|---|---|---|---|---|
| 1 | 规格文档 | 1 | `- \| \`venue_business_hours\` … inherited-or-location-scope …` | M029-001 | ④ 自动区块该行类型单元格 | 否 |
| 2 | 规格文档 | 1 | `+ \| \`venue_business_hours\` … direct …` | M029-001 | ④ 同上 | 否 |
| 3 | 规格文档 | 1 | `- \| \`venue_sessions\` … inherited-or-location-scope …` | M029-001 | ④ 自动区块该行类型单元格 | 否 |
| 4 | 规格文档 | 1 | `+ \| \`venue_sessions\` … direct …` | M029-001 | ④ 同上 | 否 |
| 5 | CSV | 2 | `- business_process_step_records,single_complex_default,…,V0.01,covered` | M029-001 | ④ CSV 该行由 scd 变 inherited（同行 source/path/note 随规则变；status/file_version 仍写死） | 否 |
| 6 | CSV | 2 | `+ business_process_step_records,inherited,process_instance_id,…,V0.01,covered` | M029-001 | ④ 同上 | 否 |
| 7 | 规格生成器 | 3 | `+ // 文件版本：V0.01` | M029-001 | ③ 新增版本标记 | 否 |
| 8 | 规格生成器 | 4 | `- function scopeType(columns) {` 及三行旧体 | M029-001 | ① 去掉闭集外兜底 | 否 |
| 9 | 规格生成器 | 4 | `+ // 本区块只收自带 scope 列的表…（M029-001）。` | M029-001 | ① 注明依据 | 否 |
| 10 | 规格生成器 | 4 | `+ function scopeType() { return "direct"; }` | M029-001 | ① 恒返回 direct | 否 |
| 11 | 规格生成器 | 5 | `- … scopeType(row.columns) …` | M029-001 | ① 调用处同步 | 否 |
| 12 | 规格生成器 | 5 | `+ … scopeType() …` | M029-001 | ① 调用处同步 | 否 |
| 13 | CSV 生成器 | 6 | `- // 文件版本：V0.03` | M029-001 | ③ | 否 |
| 14 | CSV 生成器 | 6 | `+ // 文件版本：V0.04` | M029-001 | ③ | 否 |
| 15 | CSV 生成器 | 7 | `+ { pattern: /^business_process_step_records$/, source: "process_instance_id", path: "process_instance_id -> business_process_instances -> merchant_id/project_id" },` | M029-001 | ② 增一条，字段与定稿逐字相同 | 否 |
| 16 | 隔离机审 | 8 | `+ // 文件版本：V0.01` | M029-002 | 版本标记 | 否 |
| 17 | 隔离机审 | 9 | `+ // IS [NOT] DISTINCT FROM …（M029-002）。` | M029-002 | 一行中和的注释 | 否 |
| 18 | 隔离机审 | 9 | `+ sql = sql.replace(/\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b/gi, "IS_DISTINCT");` | M029-002 | 就地中和一行；可选 NOT；吞掉 FROM | 否 |

hunk 4 的 `-` 共 4 行（函数签名 + names + if + return 兜底），上表第 8 行合并计数；加上第 9–10 的 `+` 3 行（注释 + 新函数 2 行）与版本/调用处，规格生成器合计 +5 / -5，与 `--numstat 5 5` 一致。

**归因票：PASS。** 无归因不到的行。只登记项（删 `covered`、改 baseline、改第四工具、改 HTML/锁/台账、改仓储 SQL）未出现。

---

## 5. 内容审计

### 5.1 M029-001 是否按定稿最小处置

① `scopeType`：after L85–88 注释写明「本区块只收自带 scope 列的表：按规格 §1 定义即 direct（与盘点 CSV 同口径），不发射 §1 类型闭集之外的值（M029-001）」；函数无参、恒 `return "direct"`。旧体按列名发射 `inherited-or-location-scope` 已删除。`renderBlock` 调用由 `scopeType(row.columns)` 改为 `scopeType()`。生成器仍只把 `SCOPE_COLUMNS`（含 `venue_id`）命中的表送进自动区块，与「本区块只列自带 scope 列」一致。

② `inheritedRules`：after L27 精确表名、source/path 与 V2.92 逐字相同。`classify()` 仍是 direct 列 → globalTables → inheritedRules → scd 兜底；该表无 direct 六列、不在 globalTables，命中新规则后不再落 scd。未把父表 `venue_id` 写进 path（规格未声称）。

③ 版本：规格生成器文首新增 `// 文件版本：V0.01`；CSV 生成器 `V0.03`→`V0.04`。

④ 产物：见 §5.3。未删 `status: "covered"`（after L205 仍写死；L224–227 uncovered 检查仍在）。未改手写规格段。

### 5.2 M029-002 是否按定稿最小处置

`extractReferencedTables` 在 EXTRACT 中和之后增加一行：
`sql.replace(/\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b/gi, "IS_DISTINCT")`
加注释点名运算符与误判别名 `d`。正则含可选 `NOT`，与定稿一致。替换值 `IS_DISTINCT`（无括号）等价于 B2 所写「`IS_DISTINCT(` 或等价吞掉 FROM 的替换」：`FROM` 已被匹配吃掉，后续 `\bFROM\s+TABLE_TOKEN` 不再把 `d` 当表。文首新增 `// 文件版本：V0.01`。未改 14 处仓储 SQL。未改 baseline JSON（两树 blob 相同；M030）。

### 5.3 两份产物除 3 处外逐字节未变；手写段未动

对 before/after blob 拆 `<!-- DATA_ISOLATION_SPEC_AUTO_START -->`…`END`：

- 手写前缀 5558 bytes **逐字节相等**
- 手写后缀 16928 bytes **逐字节相等**
- 自动区块 178 行对 178 行，**仅相对行 169–170** 不同：`venue_business_hours` / `venue_sessions` 的 scope 类型单元格 `inherited-or-location-scope`→`direct`；列、迁移、证据文件列未变
- after 自动区块类型计数：`direct=170`，非法类型 0
- 规格文首仍为 V0.03（M028 已收口版本，本 diff 未动）
- 自动区块 SHA256：before `81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335` → after `f1c6360c9436f902e507c3ff4cfb8312c9d0ec8d9c6ba44090b788babac90c5c`（恰因上述 2 单元格）

CSV：336 行对 336 行，**仅 L79** 不同。该行 `file_version` 仍 `V0.01`，`status` 仍 `covered`；`scope_type/source/path/note` 随 inherited 规则变，属同一「1 行」机械后果。类型计数：direct 173 不变；inherited 79→80；scd 76→75。

origin/main worktree 内用 after 生成器再跑一次无 `--check` 写入：规格 SHA256 与 CSV SHA256 与 after blob **相等**（再生幂等）。

### 5.4 改后工具是否正确（D-028 证据环境）

worktree：`git worktree add --detach /home/james/wt-m029-b4-grok-ro origin/main` → detached `6f3d25d7`；软链 `node_modules`；拷入 5 个 after 文件（SHA256 与上表 after 列一致）；未拷第四工具、未拷 baseline、未带 `--write-baseline`。主工作区有 V427、worktree 无；worktree 最新迁移 V428。

本席当场实跑（不作主工作区证据）：

| 命令 | 退出码 | 要点 |
|---|---|---|
| `node tools/generate-data-isolation-spec.mjs --check` | 0 | `data isolation spec is in sync with Flyway migration scope baseline.` |
| `node tools/generate-domain-ownership-scope-audit.mjs --check` | 0 | `DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified` |
| `node tools/verify-tenant-isolation.mjs`（默认棘轮） | 0 | 新增回归 0；扫出 2 条均已在 baseline；**注意 baseline 中 2 条已不再复现**：`F1:UNSCOPED_DIRECT_READ:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d`（后者即 002 预言；清理属 M030） |
| `node tools/verify-tenant-isolation.mjs --strict` | 1 | 表 `'d'` 命中 **0**；剩余 2 条 F1 为只登记的已登记缺口（`integration_channel_store_map`、`revenue_stream_attribution`），非本 diff 引入 |
| `node tools/verify-test-db-isolation.mjs` | 0 | `TEST_DB_ISOLATION_PASS` |
| `/home/james/audit-tmp/m029_check.sh "$WT"` | 0 | 自动区块 `170 direct`；CSV 步骤表 `inherited,process_instance_id`；phantom `'d'`=0；四门禁 0000 |

B3 提供的 `/home/james/audit-tmp/m029_b3r1_red.log`（exit 1，3 处：非法类型 2 行、步骤表 scd、`'d'`×2）与 `m029_b3r1_green.log`（exit 0）与本席 worktree 绿环一致，作旁证。`m029_b3r1_predeploy.log` 显示 worktree 内 `verify:predeploy` 链至 `TEST_DB_ISOLATION_PASS`；本席 B4 **未重跑** 全链 predeploy，不以主工作区 pretender 代替。

核完 `git worktree remove --force`；`ls` 该目录不存在；`git worktree list` 无该路径。过程中曾把 `--strict` stdout 落到 `/tmp/m029_b4_grok_strict.out`，核完已删，未写入 `audit-tmp`、未留在仓库。

### 5.5 业务发明 / 越界 / 模式

- 未发明新 scope 类型、未改 F1 判定面、未把 venue 两表改出 CSV 已有的 `direct`、未把步骤表 path 扩到 `venue_id`。
- 未删写死 `covered`。
- 未改 baseline JSON、第四工具、三 HTML、锁、findings、ledger、仓储 Java、已应用迁移。
- 可执行位：三工具与两文档两树均为 `100644`；第四工具两树均为 `100755`。`git diff --summary` 空。

### 5.6 ponytail-review（只读）

`Lean already. Ship.` `scopeType()` 恒返回本可由模板字面量内联，但定稿点名保留该函数并注明依据，不是多余抽象。`IS_DISTINCT` 替换短于另造解析器。`net: -0` 相对定稿无可再删。

### 5.7 自问（grilling，不问人）

1. 改 M028 文件是否越界？否。V2.92④与 B2 双席明文随批、只动生成器拥有区域；本席拆块证实手写前后缀未动。
2. `--strict` 仍 exit 1 是否 002 没修好？否。成功判据是假表 `'d'` 消失；剩余 2 条 F1 是只登记项。
3. 默认路径打印「`F2:UNREGISTERED_TABLE:d` 已不再复现」是否应在本模块清 baseline？否。定稿写明属 M030。
4. 替换少一个 `(` 是否偏离？否。定稿要的是中和该运算符一行；FROM 已被正则吃掉。

**内容票：PASS。**

---

## 6. 最终判定

- 归因票：**PASS**
- 内容票：**PASS**
- 合取（任一带 FAIL 即 FAIL）：**PASS**

本席 B4-r1 对 before `c183dbbfa180d53322ff433fdb95f11b3f5b899a` → after `d639978e4c5b02b405365eba6f87dcfd73bcb8dd` 整份 diff：**PASS**。

未改 findings 归属（B4 FAIL 才不改；PASS 后由席甲按提示词把 001/002 转「否」——本席只读、不写台账）。
