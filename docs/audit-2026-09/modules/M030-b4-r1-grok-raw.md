CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
进度：模块 M030 / 共 471，B3 第 1 次
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 修复 diff（质量检查记录模块；M030 FILES 闭集 1 文件 `security-scans/tenant-isolation-baseline.json`）
findings 正本：`docs/audit-2026-09/findings.md` V2.95，已定稿且归属「是」：仅 M030-001（细化）
独立性：本席未读 `M030-b4-r1-self-raw.md`（`ls` 见该路径存在、5457 bytes、mtime Sep 20 20:47，本会话未打开正文）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 持久日志 / 审计临时产物。一次性 `git worktree add --detach /home/james/wt-m030-b4-grok-ro origin/main` 拷入 before/after 基线实跑扫描器后已 `git worktree remove --force`，目录已不存在。旧 B1/B2 可对照（`M030-b1-grok-raw.md` V0.01+V0.02）；本轮以 V2.95 定稿最小处置为准。不对任何数据库执行写语句；未带 `--write-baseline`。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：已读开工路由段。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读开头；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程、不写 `docs/arc/audits/`。
3. `graft`：已读并调用。`graft grep "known_findings" --in security-scans/` 无索引（JSON 不在 graft 范围）。`graft grep "staleBaseline" --in tools/`：4 hits / 1 file，`tools/verify-tenant-isolation.mjs:L186-L337`，tokens saved ≈ **3,293**。证明陈旧键提示路径可从当前仓复现，且本 diff 未改该文件。
4. `grilling`：已读开头；对抗线自问自答；本体「等用户 / 派子代理」作废。
5. `ponytail-review`：已读；只找可删的多余抽象；不改码。结论见 §7。
6. `verification-before-completion`：已读。PASS 前当场：两树 `git diff --binary` 与冻结 diff `cmp`、JSON.parse、剩余字段逐字、origin/main worktree 扫描器默认 / `--strict`、`verify:security-baseline`、worktree 删除。
7. `requesting-code-review`：已读。B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
8. `long-running-background-tasks`：已读；本步 sha256 / node / worktree 均前台完成，未挂 monitor。
9. `codebase-design`：已核存在。本 diff 不改接口/缝，只删棘轮清单两个对象；深度模块语汇无本面。
10. `diagnosing-bugs`：已核存在。收窄为「RED 命令（before 基线默认路径出 2 条陈旧键）→ GREEN 命令（after 基线默认路径 0 条陈旧键）」；不加诊断桩。
11. `writing-for-agents`：本 raw 给下游席甲归并用；结论与证据共置。

明确不用（提示词划掉或本步不适用；本席未另开这些 SKILL.md 正文当流程跑完）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`（一次性 detach 是 D-028 允许的只读核，不是该技能本体）、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts`：`test -f .git/MERGE_HEAD` → NO_MERGE。`audit-context-building` 要求派 function-analyzer 子代理，与「只允许两席」冲突，不执行。`test-driven-development` / `tdd`：本文件纯 JSON、本 diff 无行为代码，不强制落测试（与 B2 确认「不加版本标记、不造假测试」一致）。

缺口（实找无独立可调用技能目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。`i-have-adhd` 在环境技能列表中；本文件第一行仍必须是判据锁哈希（A2 交叉报数优先于该技能「首行即下一步」），CLI 不回结论。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 全文一行与本席实算同值，仅作旁证，不以它们代替实算。`criteria-lock.md` 文首版本 V0.11；`LOCK-AMEND-COUNT: 1` 只读见到，未改。findings 文首「版本：V2.95」；`sha256sum docs/audit-2026-09/findings.md` → `16c466fa502fc1d3ff0486383da507cee03da6d4ddbef6d07f12e177605e7c13`。

冻结 diff：
```
wc -c /home/james/audit-tmp/m030_b3_r1.diff → 1440
sha256sum → d8e4449d1af84ef21ed31c87af6cb00cfdeeabb2fdcad29f873acee0ad736d83
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `9c214f11e1a52ddc01a267443fba97f59c0540a9`
- after  `6a1ead3603bbba4a1d941aa57c49756f90c5384b`

```
git diff --binary <before> <after> | sha256sum
→ d8e4449d1af84ef21ed31c87af6cb00cfdeeabb2fdcad29f873acee0ad736d83
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m030_b3_r1.diff → CMP_OK
git diff --stat：1 file changed, 8 deletions(-)
git diff --name-status：仅 M  security-scans/tenant-isolation-baseline.json
git diff --numstat：0  8  security-scans/tenant-isolation-baseline.json
hunks=1（冻结 diff 1 个 @@）
+/- 实数：+0 / -8
git diff --summary（mode/create/delete）：空
禁止路径（扫描器 / 仓储 / 安全台账 / HTML / findings / ledger / 锁）：name-only 无匹配
```

分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=d215fde0b51a0ca3121cf3a3145e1ec31f12621a`。`test -f .git/MERGE_HEAD` → **NO_MERGE**。

blob / mode（`git ls-tree`；两树比较）：

| 路径 | before mode/blob | after mode/blob | 工作区 hash-object vs after |
|---|---|---|---|
| `security-scans/tenant-isolation-baseline.json` | `100644` `f75a02636ae88ac16e0abcd407a80be4985d4b23` | `100644` `7bc98032050d2dd7d86c520aeee08dabeb538dbf` | 相同 |
| `tools/verify-tenant-isolation.mjs`（扫描器，禁止改） | `100644` `c260a2db466cf581a57102d95fe080c751501d5c` | 同左 | 同左（= origin/main） |
| `tools/generate-formal-security-baseline.mjs`（禁止改） | `100644` `70daac83aad922cd4c2abd92a67c4d55e6535667` | 同左 | — |
| `security-scans/findings-register-v1.md`（安全台账，禁止改） | `100644` `371782ee2c424a8c4057aaf788ebc54d1779e3e4` | 同左 | — |
| `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java`（仓储，禁止改） | `100644` `680fda305730eae25b21359fab0c43feb1f45c14` | 同左 | — |

工作区磁盘 mode（`stat -c %a`）：基线 JSON `664`。git 树内 mode 修前修后 `100644`，未变；`git diff --summary` 无 mode 行。

内容 SHA256（本席对 blob 实算）：

| 路径 | before | after |
|---|---|---|
| 基线 JSON | `7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15`（1431 B / 22 行） | `65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2`（995 B / 14 行） |

工作区文件 SHA256 与 after 相同；`wt==after` True。两端均以 `\n` 结尾。

模块闭集：`plan/modules.md` M030 FILES = 1 个 `security-scans/tenant-isolation-baseline.json`。扫描器 / 仓储 / 安全台账 / 生成器不在写入集合。origin/main 同路径 blob `931b651781f2a005190e0ecc2ca845c6fa2b3a8f`（内容 SHA256 `6b7d6d0236d45367230fc47d85ca32f356de26b01735f9b943eae4c80fdc808f`）与 before/after 均不同：属主工作区既有中性化副本（OPEN PR #446，B1/B2 只登记「随检查点带入」），**不是本批两树之差**。闸 3 只审两 tree 之差。

环境（D-028）：主工作区迁移含未发布 V427（`V427__m009_project_profiles_and_associations.sql` 存在于主工作区、不存在于 origin/main）。扫描器实跑证据取自 origin/main 一次性 worktree（HEAD `d215fde0`，无 V427）。主工作区直接跑扫描器不作证据。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.95 归属「是」的 ID（仅 M030-001）；归不到即越界。只登记不入队项不得出现在变更集。

---

## 2. 定稿最小处置（审计标尺，摘 V2.95）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M030-001 | 细化 | 是 | 从 `known_findings` 删除 `F1:UNSCOPED_DIRECT_READ:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d` 两个对象。保留仍被复现的两条 F1（`revenue_stream_attribution`、`integration_channel_store_map`）与 `note`、`generated_at_source` 原文逐字不动。不跑 `--write-baseline`（会用扫描器内置文案覆盖中性 note 与活键 text）。不改扫描器、仓储、安全台账。纯 JSON 无注释语法，不加版本标记（B2 双席已确认）。 |

只登记不入队（不得混进本 diff）：`tools/generate-formal-security-baseline.mjs:304-310` 与 `security-scans/findings-register-v1.md:27` 过时叙述（安全基线生成器与台账所在模块）；`--write-baseline` 内置旧 note 的 SEC-* 一一对应在现行台账不存在（扫描器模块，M029 已闭环）；`generated_at_source` 不是一次扫描定位符；origin/main `JdbcAccessRepository.java:747` 按 id 无范围读（扫描器头注 B3 盲区）；主工作区相对 origin/main 的 5 行中性化差异（OPEN PR #446）随检查点带入；陈旧键只提示不失败是扫描器既定行为。本 diff 未碰这些。

B2 本席票（`M030-b1-grok-raw.md` V0.02）：写入集合＝本模块 1 文件；删除对象 ①②，修后只留 ③④；保持 2 空格缩进、文件末 `\n`、无尾逗号、无注释。

---

## 3. diff 全文（冻结件 1440 bytes，与 `git diff --binary` 字节级相同）

```
diff --git a/security-scans/tenant-isolation-baseline.json b/security-scans/tenant-isolation-baseline.json
index f75a0263..7bc98032 100644
--- a/security-scans/tenant-isolation-baseline.json
+++ b/security-scans/tenant-isolation-baseline.json
@@ -2,14 +2,6 @@
   "note": "verify:tenant-isolation 已知缺口 baseline（棘轮底座）。闸口对此清单之外的新增 finding 硬失败。本清单只登记扫描器已发现、尚未收紧的项，并指向 generated_at_source。不得把缺口写成已修复或豁免。收紧后从本清单移除。重建：node tools/verify-tenant-isolation.mjs --write-baseline。",
   "generated_at_source": "static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv",
   "known_findings": [
-    {
-      "key": "F1:UNSCOPED_DIRECT_READ:access_grant_requests",
-      "text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 access_grant_requests 有 SELECT 读，未见 project_id = : 的 scoped 查询。尚未收紧。证据：generated_at_source。"
-    },
-    {
-      "key": "F2:UNREGISTERED_TABLE:d",
-      "text": "[F2 UNREGISTERED_TABLE] 扫描器报未登记表名 d。尚未收紧。证据：generated_at_source。"
-    },
     {
       "key": "F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution",
       "text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 revenue_stream_attribution 存在无项目范围的读。尚未收紧。证据：generated_at_source。"
```

hunk 窗口 `@@ -2,14 +2,6 @@`：旧 14 行 = 3 行未改上下文（note / generated_at_source / 数组开）+ 8 行删除 + 3 行未改上下文（活 F1 第一条的 `{` / key / text）。第二条活 F1 与文件尾在窗口外、unified diff 不列出，本席用 before/after 全文 JSON.parse 核其逐字未变（见 §5）。

---

## 4. 逐块／逐行归因（闸 3）

归属「是」的已定稿 ID 闭集：`{M030-001}`。本 diff `+` 行 = 0，`-` 行 = 8。文件头 / index / `---` / `+++` / `@@` 是 diff 元数据，不是被改文件内容。

| 块 | 路径 | 行类 | 原文（截） | 归属 ID | 理由 |
|---|---|---|---|---|---|
| H1-d1 | `security-scans/tenant-isolation-baseline.json` | `-` | `    {` | M030-001 | 陈旧对象 ① 开括号 |
| H1-d2 | 同上 | `-` | `"key": "F1:UNSCOPED_DIRECT_READ:access_grant_requests"` | M030-001 | 定稿点名删除的第一键 |
| H1-d3 | 同上 | `-` | `"text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 access_grant_requests …"` | M030-001 | 随该对象删除；不是改写 text |
| H1-d4 | 同上 | `-` | `    },` | M030-001 | 对象 ① 闭 |
| H1-d5 | 同上 | `-` | `    {` | M030-001 | 陈旧对象 ② 开括号 |
| H1-d6 | 同上 | `-` | `"key": "F2:UNREGISTERED_TABLE:d"` | M030-001 | 定稿点名删除的第二键 |
| H1-d7 | 同上 | `-` | `"text": "[F2 UNREGISTERED_TABLE] 扫描器报未登记表名 d。…"` | M030-001 | 随该对象删除 |
| H1-d8 | 同上 | `-` | `    },` | M030-001 | 对象 ② 闭 |

未改上下文（不入归因表、亦非越界）：`note` 行、`generated_at_source` 行、`known_findings` 开数组、活 F1 `revenue_stream_attribution` 的 `{`/key/text。窗口外未列出的活 F1 `integration_channel_store_map` 对象与文件尾 `]`/`}`/`\n`：before/after 对象相等（见 §5）。

**归因票：PASS。** 8 条 `-` 全部落在 M030-001；0 条 `+`；无第三文件；无归不到 ID 的内容行。

---

## 5. 内容审计（是否按定稿最小处置）

### 5.1 只删两个对象、无新增行

before `known_findings` 四键：
1. `F1:UNSCOPED_DIRECT_READ:access_grant_requests`
2. `F2:UNREGISTERED_TABLE:d`
3. `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution`
4. `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map`

after 仅 3、4，顺序保留。`removed_keys` 恰好是定稿点名的两个。`plus=0 minus=8`。

### 5.2 note、generated_at_source、两条活 F1 逐字不动

Python 对两 blob `json.loads` 后：
- `note_same True`（195 字；含「重建：node tools/verify-tenant-isolation.mjs --write-baseline。」文档句，未当修复工具执行）
- `generated_at_source_same True` = `static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv`
- `remain_objects_equal True`：两条活 F1 的 `key` 与 `text` 字节级相同

活 F1 原文（after，与 before 相同）：
- `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` → `[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 revenue_stream_attribution 存在无项目范围的读。尚未收紧。证据：generated_at_source。`
- `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` → `[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 integration_channel_store_map 存在无商户范围的读。尚未收紧。证据：generated_at_source。`

### 5.3 未跑 `--write-baseline`

扫描器 L273–282 若执行会：(1) 用扫描器长 `text` 覆盖活键；(2) 把 `note` 写成「每条须在 findings-register 有对应 SEC-*-TENANT-ISOLATION-* 证据」。现值 note 仍是中性短句（「本清单只登记扫描器已发现、尚未收紧的项…」），活键 text 仍是中性短句，**不是** `--strict` 当场打出的长文案（「在 JdbcIntegrationRepository.java 中有 SELECT 读…AC-ISO-1，§4」）。顶层三键闭集未扩。worktree 内跑完默认与 `--strict` 后再 `sha256sum` 仍为 `65384ebe…` / blob `7bc98032`，文件未被扫描器回写。argv 未含 `--write-baseline`。

### 5.4 改后 JSON 可解析、形状合法

- `json.loads(after)` 成功；顶层键恰好 `note` / `generated_at_source` / `known_findings`
- 无 `version` / `file_version`；文件内无 `//` 或 `/*`（B2 确认：纯 JSON 不加版本标记）
- `json.dumps(after, ensure_ascii=False, indent=2)+"\n" == after.decode()` → True（2 空格、无尾逗号、末 `\n`）
- before 同样 `indent=2+\n` 自洽，说明删除后未改 pretty-print 口径

### 5.5 origin/main 一次性 worktree 实跑（D-028）

```
git worktree add --detach /home/james/wt-m030-b4-grok-ro origin/main
# HEAD = d215fde0b51a0ca3121cf3a3145e1ec31f12621a
# 无 V427；扫描器 blob = c260a2db…（与两树及 origin/main 相同，M029-002 已合并）
ln -sfn <main>/node_modules <wt>/node_modules
# 未带 --write-baseline
```

**RED**（拷入 before blob `7c178f96…`）：
```
本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] 注意：baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d
[tenant-isolation] PASS（棘轮）：…2 条已知缺口…
RED_DEFAULT_EXIT=0
```

**GREEN 默认**（拷入 after blob `65384ebe…`）：
```
本次扫出 finding：2（已登记 baseline：2，新增回归：0）
（无「已不再复现」行；rg 仅命中 PASS 行）
[tenant-isolation] PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中…
GREEN_DEFAULT_EXIT=0
```

**GREEN `--strict`**：
```
[tenant-isolation] FAIL: strict 模式， 2 项租户隔离问题（fail-closed，阻断合入）：
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）在 …JdbcIntegrationRepository.java…
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）在 …JdbcSettlementConfigWriteRepository.java…
STRICT_EXIT=1
```
两条活 F1 与清单剩余键一一对应；无 `access_grant_requests`、无 `d`。`--strict` 仍红这两条是定稿要求（拿掉活键会改变闸口策略，禁止）。

**`npm run verify:security-baseline`**（after 文件仍在 worktree）：`SECBASE_EXIT=0`（`verify:prod-docs-access` 过；`verify-formal-security-baseline.mjs --strict` 过）。与 B1 观察「删键不会让该闸因内容红」一致。

全链 `verify:predeploy` 本席未在 worktree 重跑（链含后端/Android，超出本步「扫描器默认 + --strict」强制核）。席甲 B3 日志 `/home/james/audit-tmp/m030_b3r1_run.log` 写 `PREDEPLOY_EXIT=0`，只作其 B3 旁证，不构成本席独立 PASS 声称。

worktree 内 after 文件在全部扫描后仍 `65384ebe…`。随后 `git worktree remove --force /home/james/wt-m030-b4-grok-ro`；`test ! -e` → WORKTREE_GONE；`git worktree list` 无 m030。主工作区 after blob 未变，`NO_MERGE`。

### 5.6 越界面

- 变更集文件 = 1 = 模块 FILES 闭集。
- 扫描器 / 仓储 / 安全台账 / 正式基线生成器：两树 blob 全等。
- 无 HTML、无 lock、无 findings、无 ledger、无 CSV、无迁移。
- 无版本注释、无新顶层键、无活键改写。
- 未把「stale 只提示不失败」改成失败（那是扫描器政策，M029 已审、本批只登记）。

### 5.7 grilling 对抗（自问自答，不等人）

1. **会不会其实跑了 `--write-baseline`，只是碰巧键数对了？** 否。writeBaseline 会换 note 与活键长 text；现值与 before 逐字相同，且与 `--strict` 当场长文案不同。
2. **会不会删错活键、只剩陈旧键？** 否。after 两键正是 `--strict` 仍红的两表。
3. **会不会留下尾逗号让 CI 的 JSON.parse FATAL？** 否。`dumps(indent=2)+\n` 与 after 字节相等。
4. **会不会主工作区 V427 让扫描器伪绿/伪红？** 证据来自 origin/main worktree，无 V427。
5. **会不会 origin/main 扫描器尚未含 M029-002，`d` 仍被扫出？** origin/main 扫描器 blob = `c260a2db` = 两树扫描器 = M029 已合并；RED 把 `d` 列为 stale 而非当前 finding。
6. **8 行里有没有「顺手」改 note 空格？** hunk 里 note / generated_at_source 是上下文（空格前缀），不是 `-`/`+`。

**内容票：PASS。**

---

## 6. ponytail-review（只读，不改码）

8 行删除、0 行新增，对象就是定稿要删的两个。无新抽象、无新依赖、无版本键、无注释。

`Lean already. Ship.`
`net: -0 lines possible`（再删就会拿掉仍复现的活 F1，超出 001）。

---

## 7. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | **PASS** | 8 条 `-` 全归 M030-001；0 条 `+`；无第三路径 |
| 内容（最小处置） | **PASS** | 只删两陈旧对象；note / generated_at_source / 两条活 F1 逐字不动；JSON 可解析；未跑 `--write-baseline`；origin/main worktree 默认路径 exit 0 且无陈旧提示，`--strict` 只红两条活 F1；未改扫描器/仓储/台账 |
| 本席 B4-r1 | **PASS** | 两轴均 PASS；任一 FAIL 即 FAIL 的合取在本席内部已满足 |

本票不是模块收口，不是授权改 findings 归属。合取仍待席甲本次完整 self raw 落盘后归并。步骤仍 B4-r1；M030 只读见到 `IN_PROGRESS`；B3 累计只读见到 1。

本席独立实算锁哈希（报告第一行）= `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。
