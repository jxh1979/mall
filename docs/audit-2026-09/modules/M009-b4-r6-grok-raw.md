CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 B4-r6 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r6
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r6-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r6 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计第 6 次（操作员批准的追加第 6 次发布修复已执行）只审本对 tree 的修复内容。席甲本轮 B4 自审票本席未读；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：`docs/audit-2026-09/modules/M009-b4-r6-self-raw.md`（路径存在：130385 字节、mtime 2026-09-22 12:33，按互盲禁读正文，不作为判断依据）。未读适配器缓冲、未读席甲本次 B4 意见、未读原始 CLI 给对方。定稿队列以 findings.md V3.56 顶部「当前M009第6次发布修复 B2整批定稿」完整 30 项为准：仅 M009-060/061/062 队列是，M009-063 否。本席既有 B2 V0.18（`M009-b1-grok-raw.md` 追加段）同意的最小修法作为执行绑定对照，不把 B2 票本身当本轮 diff 证据。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；060/061/062 是否按 B2 同意边界修好；每一行能否归因到队列为「是」的已定稿 ID；是否越界改 SQL 业务句、生成器算法/status、手写规格条款、428–431、HTML、锁、sidecar、ledger、findings、主 index、complex_test history、IF NOT EXISTS/outOfOrder/ignore。未新造判据/角色，未询问操作员，未 LOCK-AMEND。这是 B4，不是 B5，不是发布完成。

---

## 0. 技能自核（§6，不冒充）

本席核到且按收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| requesting-code-review | 同 superpowers | B4 请审=只派本席；禁止 `HEAD~1` 基线、禁止子代理复审、禁止无 ID 即刻修 |
| receiving-code-review | 同 superpowers | 本席是外审，不 IMPLEMENT；先核两树 diff 与冻结边界再接受或反驳 |
| verification-before-completion | 同 superpowers | PASS 前独立实算锁哈希、完整 binary diff SHA、编号闸、两生成器 `--check`、三库 SELECT |
| systematic-debugging | 同 superpowers | 只借读错误→复现→追数据流；不改生产码。CSV merchants 列变化按生成器 `directColumns` 优先序解释 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 纯改名 + 跑既有生成器；不改算法、不加兼容补丁 |
| writing-for-agents | 存在 | 本 raw 给下游 B4 合取 / B5 调度读 |
| git-guardrails-claude-code | 存在；可选 | 本席不安装 hook、不以 write-tree 结果当真实 index |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。划掉的 worktree/子代理/research/Arc 全仓 audit 未调用。graft SKILL.md 已读；本轮权威证据是两 tree `git diff --binary`，未调 graft MCP（V0.17 禁 MCP/子代理）。TDD 技能不适用：本批为纯 SQL 改名 + 文档/生成清单，提示词 2.3 类型 4 命令举证。

过程披露：取证初期对脏工作区误执行 `git write-tree --prefix=`，得到 tree `a86628fd3f8b13c88aa625589041296f37aec19e`，**未采用**。真实 index 以 `sha256sum .git/index` 为准。该 write-tree 未改 `.git/index`、未改业务文件。对照落点仅 `.scratch/m009-publication-r6/`，未用 `/tmp`。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；891182 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。ledger 文件首行与本报告独占锁行同一 64 hex。criteria-lock 版本 V0.14。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `69dccbb728feb863971f2fd7aca76da47db11e64`（内容 SHA256 同上，未改锁）。四值均该 64 hex。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r6 只读审计 B3 累计第6次（操作员批准追加一次发布修复已执行）的「修复内容」本身 |
| 正式队列「是」（3） | M009-060、M009-061、M009-062 |
| 正式队列「否」 | 038–059 历史 26 项 + M009-063（仅登记验证边界，挂 060） |
| B3 | 累计 5→6；模块仍 IN_PROGRESS |
| findings.md | V3.56；两树 blob 均为 `9637329949914bbd682518f9c7d014a1fc13aa2e`，等于工作区；本 diff 未改 findings |
| before（对象类型 tree，可解析） | `6919c011af93e31ccbba13f57398f821b13c8c15` |
| after（对象类型 tree，可解析） | `e2871dc1226a626aa47b6f33ab2d805ec1a542ff` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 6919c011af93e31ccbba13f57398f821b13c8c15 e2871dc1226a626aa47b6f33ab2d805ec1a542ff`（无路径过滤） |
| 实际差异 | 125823 bytes；SHA256 `108fb84a3facc14d745f2d878bc906ce4a1b962038343d94aa22a4d9b00857af`；UTF-8；无 NUL；735 行；以换行结束；4 逻辑路径（R100+3×M）；342 插入 / 339 删除 |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3964 / after 3964；`comm -3` 仅 V427 文件名 ↔ V432 文件名（同一 blob） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`d8f12c33766c2a0f39a45d5f4b8d66f6753804e4`（本席未 fetch） |
| 工作区四文件 blob | 全部等于 after（见 §1.1） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `6e9d813c985406aaf14c1645af2b494d342ae657`；三者两树与工作区完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 真实 index 文件 SHA256 | `d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331`（`sha256sum .git/index`，取证前后两次同值） |
| ledger.md | 两树 blob `9482dc462aa556d6c2f1cc9f479871571e48ec4f` 相同；工作区 `d1bba2984482282d3974e291988b33a76d4d336b` 较新（B3 完成后的台账段，不在本对 tree diff 内） |
| 提示词 / runbook | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`（65856 字节）；`docs/audit-2026-09/plan/runbook.md` V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`（10409 字节） |

`git diff --stat --binary`：

```
 ...32__m009_project_profiles_and_associations.sql} |   0
 ...solation-and-tenant-scope-security-spec-v0.1.md |   6 +-
 docs/domain-ownership-scope-audit-v0.1.csv         | 671 +++++++++++----------
 tools/generate-domain-ownership-scope-audit.mjs    |   4 +-
 4 files changed, 342 insertions(+), 339 deletions(-)
```

`git diff --name-status`：

```
R100	backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql	backend/src/main/resources/db/migration/V432__m009_project_profiles_and_associations.sql
M	docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
M	docs/domain-ownership-scope-audit-v0.1.csv
M	tools/generate-domain-ownership-scope-audit.mjs
```

`git diff --numstat`：0/0 改名 SQL；规格与 CSV、生成器见上表。CSV hunk 为 `@@ -1,336 +1,337 @@`：表头 1 行上下文未改列名，随后 335 行删除 + 336 行新增 = **671** 条 `+/-` 行，与声称「671 行 CSV 版本变更」一致。

完整 binary diff 独立落盘 `.scratch/m009-publication-r6/b4-r6-full.diff`（禁止 `/tmp`），再对文件做 SHA256/字节，与管道结果一致。全文内嵌于本报告文末。

### 1.1 工作区四文件仍等于 after 树

| 路径 | before blob | after=worktree blob | 判定 |
|---|---|---|---|
| `backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql` | `e485a5d29fea55fa714091485aa26facc8c45160`（705 字节） | （after 无此路径） | R100 源 |
| `backend/src/main/resources/db/migration/V432__m009_project_profiles_and_associations.sql` | （before 无此路径） | `e485a5d29fea55fa714091485aa26facc8c45160` | MATCH-AFTER；**与 V427 before blob 逐字节相同**；头仍 `-- 文件版本：V0.01` |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `f92e7cd604feaa31320ab2ef4a97b70d765d00c0` | `6f68768eb405d52723044157ee26918df8d2b720` | MATCH-AFTER；标题 V0.03→V0.04 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | `078abd2474ae6a305c2f8ca664e220a6444ee609` | `47fe6601df3c916b7aee2476376581762b90c013` | MATCH-AFTER；335→336 数据行 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `d2b756d093cdceb23cb61a5166764bf14539e38d` | `f530e3ab582dadb55e7ddefbbe89b15b57da2140` | MATCH-AFTER；头 V0.04→V0.05；`file_version` 常量 V0.01→V0.02 |

未改且两树 blob 相同（抽查）：

| 路径 | 两树 blob |
|---|---|
| `tools/generate-data-isolation-spec.mjs` | `174ed1691c41c0c96e5ef9540f13d1db9e3ca548`（头仍 `// 文件版本：V0.01`） |
| `tools/verify-migration-version-order.mjs` | `230d8246c70f5fe2465e03e75e197801375a4902` |
| `V428__m021_settlement_format_share_media.sql` | `5920e7d53d41952129085f4626e1baaf82c4c6af` |
| `V429__retire_verification_manual_override.sql` | `ac3c4bb658cff3051250ad0b13a46fe06a495b9f` |
| `V430__permission_names_no_latin_ai.sql` | `385b7fb47218088c6e2a77917a5a643351049160` |
| `V431__merchant_contact_names_ciphertext.sql` | `5fe129ae2425ede84c80998e29ecd14d27930d51` |

迁移目录：before 399 个 SQL、after 399 个 SQL；唯一文件名差为 V427↔V432。工作区 `readdir` 与 after 文件名集合全等（399，无 V427、有 V432）。

真实 index 仍停在 B3 临时索引规程下的主 index：文件 SHA256 未变。本席未改任何源文件；本 raw 原先不存在，本次新建。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.14 已映射到 M009 的集合。本轮只用定稿已引用的 ID。不 LOCK-AMEND。

| ID | 分类 | 缺陷 | 队列 | 冻结依据原文（本席自读锁，不扩） | B2 V0.18 同意的最小边界 |
|---|---|---|---|---|---|
| M009-060 | 技术性 | 是 | 是 | **GEN-014**「迁移须顺序确定、可在目标数据库执行并保持模式与代码一致」；从属 GEN-002 / GEN-013 | 纯重命名 V427→当时空闲高号（现 432）；SQL 字节不改；头 V0.01 保持；不改 428–431；不开 outOfOrder/ignore |
| M009-061 | 技术性 | 是 | 是 | **GEN-016**「路径/版本/完成声明须可从当前范围复现」；从属 GEN-008 | 跑现生成器重生 CSV；仅既有 `file_version` 常量 V0.01→V0.02 及自身头 V0.04→V0.05；不改分类算法/数组/列结构/status |
| M009-062 | 技术性 | 是 | 是 | **GEN-016** 同上 | 跑原 `docs:data-isolation-spec` 只刷新 AUTO 区；规格标题 V0.03→V0.04；生成器代码不改；手写条款不改 |
| M009-063 | 技术性 | 否 | 否 | GEN-014 后半 + GEN-006 | 只登记：空库全迁、无 427 的 430 库升级独立验证；原 `complex_test` 不改 history、不加 IF NOT EXISTS。**不在本批修复 diff 内出现待修行** |

四分类本轮不重开：060/061/062/063 仍技术性。无添加项。不回开 013/038。

HTML 本 diff 未改，铁律 4 三份只读。本轮不引用新 HTML 句子作为修法。

---

## 3. 逐块归因（闸 3；每一行有 ID）

共 8 个 hunk / 改名块。无未归因行。

### 3.1 060 — R100 纯改名，SQL 原字节

- 块：`rename from V427__…sql` / `rename to V432__…sql`，`similarity index 100%`，无 `@@` 内容 hunk。
- blob：before `e485a5d2…` = after `e485a5d2…`，705 字节，SHA256 `cc8c391bda2bdb2658d5640ceff6036736bfe9d2c3b864de8f13e8474f9a3b13`。
- 头：`-- 文件版本：V0.01`（未伪增）。
- 正文无 `V427`/`V432` 字样。无 `IF NOT EXISTS` / `outOfOrder` / `ignoreMissing` / `ignoreIgnoredMigrations` / `baselineOnMigrate` / `repair`。
- SQL 内容（两树相同，仅文件名变）：`projects` 三列可空 TEXT；`merchants.project_id VARCHAR(64) REFERENCES projects(id)` + `idx_merchants_project_id`；`business_entity_projects` PK `(business_entity_id, project_id)` + 两 FK + `idx_business_entity_projects_project_id`。
- 428/429/430/431 blob 未变。
- **归因 M009-060。未越界。**

### 3.2 061 — 生成器仅两处版本位点

文件 233 行，长度 13854 字节前后相同。去掉两处版本位点后文本全等。仅：

1. hunk `@@ -1,4 +1,4 @@`：`// 文件版本：V0.04` → `V0.05`（自身头，版本铁律）。
2. hunk `@@ -201,7 +201,7 @@`：`file_version: "V0.01"` → `"V0.02"`（既有常量，写出 CSV 列）。

`directColumns` / `inheritedRules` / `status: "covered"` / 列结构未改。`status` 字面量仍硬编码 `"covered"`。

**归因 M009-061。未越界。辅助版本修改已计入，未遗漏。**

### 3.3 061 — CSV 671 行 `+/-`（机械重生）

hunk `@@ -1,336 +1,337 @@`。表头列名未变。独立解析：

| 对照 | before | after |
|---|---:|---:|
| 行（含表头） | 336 | 337 |
| 数据行 | 335 | 336 |
| `file_version` | 全 V0.01 | 全 V0.02 |
| `status` | 全 covered | 全 covered |
| 删除表 | 0 | — |
| 新增表 | — | 仅 `business_entity_projects` |

共同 335 行中：

- **334** 行唯一差异：`file_version` V0.01→V0.02。
- **1** 行 `merchants`：`file_version` 同上，且 `scope_source` `business_entity_id`→`project_id`、`scope_path` 同步改为 `project_id column on merchants`；`scope_type=direct`、`status=covered`、`multi_complex_note` 未变。

这不是改算法：生成器 `directColumns` 数组既有顺序以 `project_id` 为第一优先，高于 `business_entity_id`。V432 给 `merchants` 加上 `project_id` 后，原分类器机械改写该行。B2 V0.18 §4.2 已预告该机械优先。新增 `business_entity_projects` 行：`direct,project_id,…,V0.02,covered`。

**整块归因 M009-061。status 列零变化。未越界。**

### 3.4 062 — 规格标题 + AUTO 区，手写条款不动

生成器 `tools/generate-data-isolation-spec.mjs` 两树 blob 相同，头仍 V0.01（「生成器代码不改」落实）。

规格拆 `<!-- DATA_ISOLATION_SPEC_AUTO_START -->` … `END`：

- **AUTO 前**：仅第 1 行标题 `V0.03`→`V0.04`；其余前置全文在把 V0.03 替换为 V0.04 后与 after 逐字节相同。
- **AUTO 后（手写条款）**：两树逐字节相同。
- **AUTO 内非表行**：8 行中仅摘要行 `最新迁移 **V428**，发现 **170** 张` → `**V432**` / `**172**`。其余非表行相同。
- **AUTO 表**：170→172。新增且仅新增 `business_entity_projects`、`merchants`（均为 `project_id` / direct / 首次出现 V432 / 证据文件 `V432__m009_project_profiles_and_associations.sql`）。共同 170 表行 **0** 条改写。

对应 4 个 hunk：

| hunk | 内容 | ID |
|---|---|---|
| `@@ -1,4 +1,4 @@` | 标题 V0.03→V0.04 | 062（文件自身版本铁律；非手写隔离语义） |
| `@@ -56,7 +56,7 @@` | AUTO 摘要 V428/170→V432/172 | 062 |
| `@@ -104,6 +104,7 @@` | 插入 `business_entity_projects` | 062 |
| `@@ -172,6 +173,7 @@` | 插入 `merchants` | 062 |

标题在 AUTO 块外，但是同一被改文件的版本维护；B2 V0.18 §4.3 已同意归 062、不另开 ID。本席核到手写业务条款零变化，接受该归属。

**归因 M009-062。未越界。辅助标题递增已计入。**

### 3.5 无其它路径

name-status 仅上述 4 逻辑文件。无 Java、无测试、无 HTML、无 lock、无 findings、无 ledger、无 428–431、无 isolation 生成器。063 队列否，diff 中无对 `complex_test` history 的修补、无 IF NOT EXISTS 兼容句。

闸 3：每一行都能归到 060 或 061 或 062。无 ID 行 = 0。越界判定：**PASS**。

---

## 4. B3 后实跑（本席独立；只读）

工作区迁移目录与 after 树文件名集合全等，四目标文件 blob 等于 after，故下列命令在正本 cwd 对 after 内容有效。`--check` 分支只读比较，不 `writeFileSync`。

### 4.1 编号闸 exit 0

命令：`node tools/verify-migration-version-order.mjs`（即 `npm run verify:migration-version-order` 的同一入口）。未 fetch。本地 `origin/main` 最大 V430。相对 origin/main 新增恰好 2 个文件：`V431__merchant_contact_names_ciphertext.sql`（本批 before 已有，非本 diff 引入）、`V432__m009_project_profiles_and_associations.sql`（本 diff 由 V427 改名）。二者均 >430。

```
MIGRATION_VERSION_ORDER_PASS: 本分支新增 2 个迁移,均排在 origin/main 最大号 V430 之后
numbering-exit=0
```

V427 已不在工作区目录，闸不再报「不大于 430」。060 编号面消除。

### 4.2 两生成器 `--check` 均 exit 0

```
node tools/generate-domain-ownership-scope-audit.mjs --check
DOMAIN_OWNERSHIP_SCOPE_PASS: 336 tables classified
csv-check-exit=0

node tools/generate-data-isolation-spec.mjs --check
data isolation spec is in sync with Flyway migration scope baseline.
spec-check-exit=0
```

061/062 的生成清单与现迁移目录一致。未跑写模式 `docs:domain-ownership-scope` / `docs:data-isolation-spec`。

---

## 5. 数据库只读核验（三库；不写）

密钥：仅读取 `/etc/complex/complex.env` 的三 DB 项 `COMPLEX_DATABASE_URL` / `COMPLEX_DATABASE_USERNAME` / `COMPLEX_DATABASE_PASSWORD`。解析得 host=`127.0.0.1` port=`5432` user=`complex`（口令长度 48，不打印）。未 `source` 该文件。未读 `secrets.env`。全部 `psql -v ON_ERROR_STOP=1` 仅 SELECT / 系统目录。未 `repair`、未 DELETE/UPDATE `flyway_schema_history`、未 migrate、未开 outOfOrder/ignore。未调用 flyway CLI validate（避免会话锁）；以 history 行计数与 checksum 作只读等价。

### 5.1 `complex_test`（063：不是本批升级目标）

| 项 | 本席实算 |
|---|---|
| success 行 | 399；fail 0；rank 缺口 0 |
| 最大 version | **431**（不是 432） |
| version=427 | 1（rank=395，desc=`m009 project profiles and associations`，checksum=`216061286`） |
| version=432 | **0** |
| >430 | 仅 431 |
| ≤430 且 success | 398（含未发布 427） |
| `md5(string_agg(row_to_json(h)::text,'' ORDER BY installed_rank))` | **`6258acd4a7f89f16d545dcf0048bbbc5`**（与声称全等） |
| `merchants.project_id` 非空 | 0 / 57 |
| `projects` 行 | 0（三新列无从非空） |
| bep 约束 | 见下，表已存在（历史 427 已建），0 行 |

history 未被 repair/删除。改名并不声称本库再跑 432。**063 约束守住。**

### 5.2 `complex_test_m009_pub_upgrade_20260922`（430 库升级）

| 项 | 本席实算 |
|---|---|
| success 行 | 399；fail 0；rank 缺口 0 |
| 最大 version | **432** |
| version=427 | **0** |
| version=432 | 1（rank=399，checksum=`216061286`，与 complex_test 上 427 checksum **相同**，印证同字节 SQL） |
| >430 | **恰好 431、432 共 2**（rank 398/399） |
| ≤430 且 success | **397**（停在 430 后再只执行 2） |
| `projects` | n=1；`owner_person`/`address`/`business_scope` 非空均为 0 |
| `md5(string_agg((to_jsonb(p)-'owner_person'-'address'-'business_scope')::text,'' ORDER BY p.id))` | **`1cd4ebc13a6720b8ab81224c434a06b5`** |
| `merchants` | n=1；`project_id` 非空 0 |
| `md5(string_agg((to_jsonb(m)-'project_id')::text,'' ORDER BY m.id))` | **`6cfce3db5a2697d96a0675397628b861`** |
| bep 行 | 0 |

旧项目三新列全 NULL，剔除后 md5 仍原值；原商户 `project_id` NULL，剔除后 md5 仍原值。

### 5.3 `complex_test_m009_pub_empty_20260922`（空库全迁）

| 项 | 本席实算 |
|---|---|
| success 行 | 399；fail 0；rank 缺口 0 |
| 最大 version | **432** |
| version=427 | **0** |
| version=432 | 1（checksum 同为 `216061286`） |
| >430 | 431、432 |
| ≤430 且 success | 397 |
| `projects` | n=0 |
| `merchants.project_id` 非空 | 0 / 1（种子行，`project_id` NULL） |

空库全 399 成功且停在 432。无 427 历史。

### 5.4 新 bep PK / 2FK / index（三库同形）

`pg_constraint` / `pg_indexes`（三库均有）：

- `business_entity_projects_pkey` PRIMARY KEY `(business_entity_id, project_id)`（附 unique btree）
- `business_entity_projects_business_entity_id_fkey` FOREIGN KEY → `business_entities(id)`
- `business_entity_projects_project_id_fkey` FOREIGN KEY → `projects(id)`
- `idx_business_entity_projects_project_id` btree `(project_id)`
- `merchants_project_id_fkey` FOREIGN KEY → `projects(id)`
- `idx_merchants_project_id` btree `(project_id)`

与 SQL 原文一致。无 IF NOT EXISTS 痕迹（约束名即默认名，一次创建）。

---

## 6. 第一性原理 / 顺从 / 对抗（B4 收窄到本 diff）

### 6.1 第一性

1. **为何必须存在**：删掉改名则编号闸对 V427≤430 仍红（GEN-014/AGENTS §9）；删掉 CSV/规格刷新则两 `--check` 仍红（GEN-016）。删除后 HTML 项目三字段/多对多并不消失——那些已由 013/038 落在本 SQL 字节里，本批只修执行序号与生成镜像。
2. **声称 vs 实跑**：编号闸 exit 0；两 `--check` exit 0；SQL 改名 100% 相似且 blob 相同；CSV 336 表 / 规格 172 表；隔离库 399 success 停在 432；complex_test history md5 不变。均本席本轮命令输出，不是转述。
3. **未验证前提**：未 fetch 远端 `origin/main`（本地 ref 最大 430）；未跑 flyway CLI validate；未对脏工作区跑全量后端测试（本批无 Java 变更，且主工作区大量无关脏文件，跑全量会污染证据）。这些不是本 diff 的未归因行。

### 6.2 顺从

映射到本批的 GEN-014/002/013/016/008/006：编号顺序、生成清单与迁移一致、规格 AUTO 与现目录一致、旧库完整性未靠篡改 history 取得。060/061/062 最小边界句句有对应 hunk。063 未被写成待修 DDL。

### 6.3 对抗（自问，不派第三席）

| 反例 | 核验 | 结果 |
|---|---|---|
| 改名时改了 SQL 业务句或伪增 V0.02 | blob 相同、头仍 V0.01 | 未发生 |
| 把 013/038 业务再改一轮 | diff 无 Java/无新列语义 | 未发生 |
| 生成器改了 `directColumns`/`status` | 仅 L1 与 L204 | 未发生 |
| CSV 671 行里夹带非版本/非机械分类 | 334 只 FV；merchants 只优先列；+1 新表 | 未发生 |
| 规格手写条款被改 | AUTO 后全文相同 | 未发生 |
| 标题 V0.04 无 ID | 归 062，B2 已同意 | 有 ID |
| 063 被偷偷 repair/删 427 | complex_test 仍有 427、无 432、md5 不变 | 未发生 |
| 加 IF NOT EXISTS / outOfOrder | SQL 全文无这些词；两树无配置改动 | 未发生 |
| 改三 HTML / 锁 / 主 index | blob/SHA 不变 | 未发生 |
| 改 428–431 | blob 不变 | 未发生 |
| 遗漏辅助版本（生成器头、规格头、CSV FV） | §3.2–3.4 均有 ID | 未遗漏 |
| merchants.scope_source 变化=新隔离政策 | 既有数组优先序 + 新列输入 | 机械，归 061 |

无新缺陷。无添加。无 HTML 申请。

---

## 7. 覆盖与结论

| 检查 | 结果 |
|---|---|
| 完整无过滤 binary diff 自算 SHA/字节 | 与派发期望全等 |
| 4 逻辑文件逐块归因 060/061/062 | 全覆盖 |
| 671 行 CSV +/- | 全归 061 |
| 辅助版本位点 | 生成器头、规格头、CSV `file_version` 均有 ID |
| 闸 3 无 ID 行 | 0 |
| 编号闸 / 两 `--check` | 三个 exit 0 |
| 升级库只跑 431+432、399 success、432 | 是 |
| 空库 399 success、432、无 427 | 是 |
| 旧项目/原商户 md5 与新列 NULL | 升级库命中声称哈希；新列全 NULL |
| bep PK+2FK+index | 三库在 |
| complex_test history md5 | 6258acd4… 不变；仍 427 无 432 |
| 三 HTML / 锁四值 / 真实 indexSHA | 均不变 |
| 本席修码/写库/读本轮 self 正文 | 否 |

修复内容：**PASS**。
范围归因（闸 3 越界）：**PASS**。
步骤总结论：**PASS**。

不存在有条件通过。本票不是 B4 双 PASS，不是 B5，不是模块 PASS。B3 累计 6，生命周期仍 IN_PROGRESS。本席 FAIL 否决权未行使。

---

## 8. 完整 git diff --binary 原文

命令：`git diff --binary 6919c011af93e31ccbba13f57398f821b13c8c15 e2871dc1226a626aa47b6f33ab2d805ec1a542ff`

独立实算：125823 bytes，SHA256 `108fb84a3facc14d745f2d878bc906ce4a1b962038343d94aa22a4d9b00857af`。下列为该输出全文（UTF-8，无 NUL）。

~~~~diff
diff --git a/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql b/backend/src/main/resources/db/migration/V432__m009_project_profiles_and_associations.sql
similarity index 100%
rename from backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
rename to backend/src/main/resources/db/migration/V432__m009_project_profiles_and_associations.sql
diff --git a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
index f92e7cd6..6f68768e 100644
--- a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
+++ b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
@@ -1,4 +1,4 @@
-# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03
+# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.04
 
 > 创建日期：2026-06-18
 > 状态：权威安全规格（补齐此前缺口——隔离模型此前仅存在于各迁移 .sql 注释中，无单一权威文档）。
@@ -56,7 +56,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 
 <!-- DATA_ISOLATION_SPEC_AUTO_START -->
 ### 3A. 自动迁移隔离基线（由 `npm run docs:data-isolation-spec` 生成）
-> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V428**，发现 **170** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 `npm run docs:data-isolation-spec`，提交前运行 `npm run verify:data-isolation-spec`。
+> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V432**，发现 **172** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 `npm run docs:data-isolation-spec`，提交前运行 `npm run verify:data-isolation-spec`。
 | 表 | scope 字段 | scope 类型 | 首次出现迁移 | 证据文件 |
 |---|---|---|---|---|
 | `access_grant_requests` | `merchant_id` + `project_id` | direct | V170 | `V170__access_grant_requests.sql` |
@@ -104,6 +104,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | `bar_table_sessions` | `merchant_id` | direct | V019 | `V019__p1_bar_table_session_adjustments.sql` |
 | `bar_tables` | `merchant_id` + `space_id` | direct | V047, V084 | `V047__bar_security_merchant_isolation.sql`<br>`V084__object_space_mapping.sql` |
 | `business_entity_bindings` | `merchant_id` | direct | V010 | `V010__p0_foundation_residual_platform_terminal_evidence.sql` |
+| `business_entity_projects` | `project_id` | direct | V432 | `V432__m009_project_profiles_and_associations.sql` |
 | `business_process_instances` | `merchant_id` + `project_id` + `venue_id` | direct | V059 | `V059__operation_traceability_three_layer.sql` |
 | `business_review_locks` | `project_id` | direct | V216 | `V216__business_review_locks.sql` |
 | `channel_fee_configs` | `project_id` | direct | V327 | `V327__settlement_channel_fee_configs.sql` |
@@ -172,6 +173,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 | `merchant_fee_notification_recipient` | `merchant_id` | direct | V182 | `V182__merchant_fee_notification_recipient.sql` |
 | `merchant_revenue_attribution_rules` | `merchant_id` | direct | V221 | `V221__merchant_revenue_attribution_rules.sql` |
 | `merchant_service_work_orders` | `merchant_id` + `space_id` | direct | V022 | `V022__p1_merchant_service_work_orders.sql` |
+| `merchants` | `project_id` | direct | V432 | `V432__m009_project_profiles_and_associations.sql` |
 | `pass_gift_cost_configs` | `project_id` | direct | V373 | `V373__pass_gift_cost_configs.sql` |
 | `pass_gift_cost_variance_ledger` | `project_id` | direct | V377 | `V377__pass_gift_variance_ledger.sql` |
 | `platform_retention_rules` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
diff --git a/docs/domain-ownership-scope-audit-v0.1.csv b/docs/domain-ownership-scope-audit-v0.1.csv
index 078abd24..47fe6601 100644
--- a/docs/domain-ownership-scope-audit-v0.1.csv
+++ b/docs/domain-ownership-scope-audit-v0.1.csv
@@ -1,336 +1,337 @@
 table_name,scope_type,scope_source,scope_path,multi_complex_note,file_version,status
-access_grant_requests,direct,project_id,project_id column on access_grant_requests,ready for project/merchant scoped query enforcement,V0.01,covered
-access_users,direct,merchant_id,merchant_id column on access_users,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_capacity_config,direct,project_id,project_id column on amusement_capacity_config,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_capacity_snapshot,direct,project_id,project_id column on amusement_capacity_snapshot,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_device_statuses,direct,project_id,project_id column on amusement_device_statuses,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_devices,direct,project_id,project_id column on amusement_devices,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_group_bookings,direct,project_id,project_id column on amusement_group_bookings,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_incident_actions,inherited,inspection/incident id,inspection/incident id -> amusement project/session scope,must join through scope path before multi-complex rollout,V0.01,covered
-amusement_incidents,direct,project_id,project_id column on amusement_incidents,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_inspection_tasks,direct,project_id,project_id column on amusement_inspection_tasks,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_maintenance_record,direct,project_id,project_id column on amusement_maintenance_record,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_manual_overrides,direct,project_id,project_id column on amusement_manual_overrides,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_over_capacity_policy,direct,project_id,project_id column on amusement_over_capacity_policy,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_project_entitlements,direct,project_id,project_id column on amusement_project_entitlements,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_project_rules,direct,project_id,project_id column on amusement_project_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_projects,direct,merchant_id,merchant_id column on amusement_projects,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_queue_entries,direct,project_id,project_id column on amusement_queue_entries,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_reservations,direct,project_id,project_id column on amusement_reservations,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_safety_actions,inherited,inspection/incident id,inspection/incident id -> amusement project/session scope,must join through scope path before multi-complex rollout,V0.01,covered
-amusement_safety_inspections,direct,project_id,project_id column on amusement_safety_inspections,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_safety_notices,direct,project_id,project_id column on amusement_safety_notices,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_safety_templates,direct,merchant_id,merchant_id column on amusement_safety_templates,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_sessions,direct,project_id,project_id column on amusement_sessions,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_verifications,direct,project_id,project_id column on amusement_verifications,ready for project/merchant scoped query enforcement,V0.01,covered
-amusement_waiver_acknowledgements,direct,project_id,project_id column on amusement_waiver_acknowledgements,ready for project/merchant scoped query enforcement,V0.01,covered
-analytics_ai_quality_results,global_reference,global_reference,platform-global AI quality result ledger; unfiltered read gated by requirePlatformScope,intentional platform-global AI governance output; not a tenant access boundary,V0.01,covered
-analytics_ai_quality_tasks,global_reference,global_reference,platform-global AI quality task ledger; unfiltered read gated by requirePlatformScope,intentional platform-global AI governance output; not a tenant access boundary,V0.01,covered
-analytics_ai_thresholds,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_anomaly_config,global_reference,global_reference,platform-global anomaly threshold config (singleton id='default'); read/write gated by requirePlatformScope,intentional platform-global; per-tenant thresholds would require a separate scoped table,V0.01,covered
-analytics_business_metric_snapshots,direct,segment_id,"polymorphic (segment_type, segment_id) self-owned scoped-read channel on analytics_business_metric_snapshots","segment_type in {platform(NULL),merchant,project}; merchant/project rows scoped by segment_id (AC-ISO-1)",V0.01,covered
-analytics_customer_intents,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_dashboard_snapshots,direct,project_id,project_id column on analytics_dashboard_snapshots,ready for project/merchant scoped query enforcement,V0.01,covered
-analytics_merchant_health_scores,direct,merchant_id,merchant_id column on analytics_merchant_health_scores,ready for project/merchant scoped query enforcement,V0.01,covered
-analytics_merchant_summaries,direct,merchant_id,merchant_id column on analytics_merchant_summaries,ready for project/merchant scoped query enforcement,V0.01,covered
-analytics_metric_anomalies,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_metric_definitions,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_risk_alerts,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_risk_rules,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_robot_metrics,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-analytics_staff_service_scores,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.01,covered
-approval_lines,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ar_ap_adjustments,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ar_ap_item_sources,inherited,ar_ap_item_id,ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope,must join through scope path before multi-complex rollout,V0.01,covered
-ar_ap_items,direct,project_id,project_id column on ar_ap_items,ready for project/merchant scoped query enforcement,V0.01,covered
-ar_ap_payments,inherited,ar_ap_item_id,ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope,must join through scope path before multi-complex rollout,V0.01,covered
-audit_events,direct,merchant_id,merchant_id column on audit_events,ready for project/merchant scoped query enforcement,V0.01,covered
-auth_credentials,inherited,user_id/session,user_id/session -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.01,covered
-auth_sessions,inherited,user_id/session,user_id/session -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_bill_adjustments,inherited,bill/order linkage,bill/order linkage -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_bill_operations,inherited,bill/order linkage,bill/order linkage -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_bill_settlements,direct,merchant_id,merchant_id column on bar_bill_settlements,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_bills,direct,merchant_id,merchant_id column on bar_bills,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_combo_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-bar_combos,direct,merchant_id,merchant_id column on bar_combos,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_commission_calculations,inherited,bill_id,bill_id -> bar_bills -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_commission_rate_configs,direct,project_id,project_id column on bar_commission_rate_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_deposit_rules,direct,merchant_id,merchant_id column on bar_deposit_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_gift_redemptions,direct,merchant_id,merchant_id column on bar_gift_redemptions,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_order_item_voids,direct,merchant_id,merchant_id column on bar_order_item_voids,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_order_items,inherited,order_id,order_id -> bar_orders -> table_id/bar_id -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_orders,direct,merchant_id,merchant_id column on bar_orders,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_product_categories,direct,merchant_id,merchant_id column on bar_product_categories,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_production_ticket_events,inherited,order_id,order_id -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_production_tickets,direct,merchant_id,merchant_id column on bar_production_tickets,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_products,direct,merchant_id,merchant_id column on bar_products,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_reservation_invitation_members,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-bar_reservations,direct,merchant_id,merchant_id column on bar_reservations,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_shifts,direct,project_id,project_id column on bar_shifts,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_table_guest_checkins,inherited,reservation/table linkage,reservation_id/table_id -> bar_reservations/bar_tables -> venue/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-bar_table_session_adjustments,direct,merchant_id,merchant_id column on bar_table_session_adjustments,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_table_sessions,direct,merchant_id,merchant_id column on bar_table_sessions,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_tables,direct,merchant_id,merchant_id column on bar_tables,ready for project/merchant scoped query enforcement,V0.01,covered
-bar_void_reason_codes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-business_day_cutoff_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-business_entities,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-business_entity_bindings,direct,merchant_id,merchant_id column on business_entity_bindings,ready for project/merchant scoped query enforcement,V0.01,covered
-business_process_instances,direct,project_id,project_id column on business_process_instances,ready for project/merchant scoped query enforcement,V0.01,covered
-business_process_step_records,inherited,process_instance_id,process_instance_id -> business_process_instances -> merchant_id/project_id,must join through scope path before multi-complex rollout,V0.01,covered
-business_review_locks,direct,project_id,project_id column on business_review_locks,ready for project/merchant scoped query enforcement,V0.01,covered
-channel_fee_configs,direct,project_id,project_id column on channel_fee_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-complex_operating_params,direct,project_id,project_id column on complex_operating_params,ready for project/merchant scoped query enforcement,V0.01,covered
-compliance_expiry_items,direct,merchant_id,merchant_id column on compliance_expiry_items,ready for project/merchant scoped query enforcement,V0.01,covered
-compliance_reminders,direct,merchant_id,merchant_id column on compliance_reminders,ready for project/merchant scoped query enforcement,V0.01,covered
-comprehensive_service_fee_configs,direct,project_id,project_id column on comprehensive_service_fee_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_approval_flow_steps,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-contract_approval_flows,direct,project_id,project_id column on contract_approval_flows,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_approvals,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-contract_changes,direct,project_id,project_id column on contract_changes,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_key_dates,direct,merchant_id,merchant_id column on contract_key_dates,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_rent_bills,direct,project_id,project_id column on contract_rent_bills,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_settlements,direct,project_id,project_id column on contract_settlements,ready for project/merchant scoped query enforcement,V0.01,covered
-contract_spaces,direct,project_id,project_id column on contract_spaces,ready for project/merchant scoped query enforcement,V0.01,covered
-contracts,direct,project_id,project_id column on contracts,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_birthday_rules,direct,project_id,project_id column on customer_birthday_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_check_in_tiers,direct,project_id,project_id column on customer_check_in_tiers,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_check_ins,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-customer_consents,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-customer_member_levels,direct,project_id,project_id column on customer_member_levels,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_points_accounts,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-customer_points_ledger,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-customer_points_redemptions,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-customer_points_rules,direct,project_id,project_id column on customer_points_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_redemption_catalog_items,direct,project_id,project_id column on customer_redemption_catalog_items,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_sessions,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-customer_stored_value_accounts,direct,project_id,project_id column on customer_stored_value_accounts,ready for project/merchant scoped query enforcement,V0.01,covered
-customer_stored_value_transactions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-customers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-data_cleanup_jobs,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.01,covered
-deposit_ledger,direct,merchant_id,merchant_id column on deposit_ledger,ready for project/merchant scoped query enforcement,V0.01,covered
-dividend_deduction_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-dividend_plans,direct,merchant_id,merchant_id column on dividend_plans,ready for project/merchant scoped query enforcement,V0.01,covered
-dividend_reserve_releases,direct,merchant_id,merchant_id column on dividend_reserve_releases,ready for project/merchant scoped query enforcement,V0.01,covered
-dividend_statement_lines,direct,merchant_id,merchant_id column on dividend_statement_lines,ready for project/merchant scoped query enforcement,V0.01,covered
-dividend_statements,direct,merchant_id,merchant_id column on dividend_statements,ready for project/merchant scoped query enforcement,V0.01,covered
-employee_notices,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-energy_anomaly_thresholds,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-energy_meter_readings,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-energy_meters,direct,project_id,project_id column on energy_meters,ready for project/merchant scoped query enforcement,V0.01,covered
-entitlement_day_activations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-entitlement_entry_records,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-entitlement_sessions,direct,project_id,project_id column on entitlement_sessions,ready for project/merchant scoped query enforcement,V0.01,covered
-entitlement_templates,direct,venue_id,venue_id column on entitlement_templates,ready for project/merchant scoped query enforcement,V0.01,covered
-evidence_attachments,direct,merchant_id,merchant_id column on evidence_attachments,ready for project/merchant scoped query enforcement,V0.01,covered
-evidence_events,direct,merchant_id,merchant_id column on evidence_events,ready for project/merchant scoped query enforcement,V0.01,covered
-evidence_reviews,direct,merchant_id,merchant_id column on evidence_reviews,ready for project/merchant scoped query enforcement,V0.01,covered
-external_partner_orgs,direct,project_id,project_id column on external_partner_orgs,ready for project/merchant scoped query enforcement,V0.01,covered
-facility_equipment,direct,project_id,project_id column on facility_equipment,ready for project/merchant scoped query enforcement,V0.01,covered
-facility_inspection_records,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-facility_inspection_tasks,direct,project_id,project_id column on facility_inspection_tasks,ready for project/merchant scoped query enforcement,V0.01,covered
-facility_inspection_templates,direct,project_id,project_id column on facility_inspection_templates,ready for project/merchant scoped query enforcement,V0.01,covered
-facility_work_order_policy,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-facility_work_orders,direct,project_id,project_id column on facility_work_orders,ready for project/merchant scoped query enforcement,V0.01,covered
-finance_daily_close_summaries,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-finance_daily_closes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-finance_offline_payment_confirmations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-finance_payment_intents,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-finance_reconciliation_batches,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-finance_reconciliation_differences,inherited,batch_id,batch_id -> finance_reconciliation_batches -> project/entity scope,must join through scope path before multi-complex rollout,V0.01,covered
-finance_refunds,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-floor_plan_import_candidates,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.01,covered
-floor_plan_import_errors,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.01,covered
-floor_plan_import_tasks,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.01,covered
-floor_plan_shapes,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.01,covered
-giveaway_cost_configs,direct,project_id,project_id column on giveaway_cost_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-high_value_thresholds,direct,project_id,project_id column on high_value_thresholds,ready for project/merchant scoped query enforcement,V0.01,covered
-holiday_calendar,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-idempotency_records,inherited,operation_key,operation_key -> request route + actor + business operation,must join through scope path before multi-complex rollout,V0.01,covered
-integration_channel_store_map,direct,merchant_id,merchant_id column on integration_channel_store_map,ready for project/merchant scoped query enforcement,V0.01,covered
-integration_channel_ticket_type_map,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_compensation_tasks,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_order_syncs,direct,merchant_id,merchant_id column on integration_order_syncs,ready for project/merchant scoped query enforcement,V0.01,covered
-integration_provider_adapter_tests,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_provider_adapters,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_read_ledger,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_reconciliation_batches,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_reconciliation_differences,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_retry_tasks,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_robot_task_events,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_video_access_approvals,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_video_access_audits,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-integration_video_anchors,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.01,covered
-internal_price_catalog,direct,merchant_id,merchant_id column on internal_price_catalog,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_high_value_reviews,direct,merchant_id,merchant_id column on inventory_high_value_reviews,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_item_cost_state,direct,merchant_id,merchant_id column on inventory_item_cost_state,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_items,direct,merchant_id,merchant_id column on inventory_items,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_purchase_request_events,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_purchase_requests,direct,merchant_id,merchant_id column on inventory_purchase_requests,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_risk_alerts,direct,merchant_id,merchant_id column on inventory_risk_alerts,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_stock_adjustments,direct,merchant_id,merchant_id column on inventory_stock_adjustments,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_stock_balances,direct,project_id,project_id column on inventory_stock_balances,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_stock_batches,direct,project_id,project_id column on inventory_stock_batches,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_stock_movements,direct,project_id,project_id column on inventory_stock_movements,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_supplier_reconciliations,direct,merchant_id,merchant_id column on inventory_supplier_reconciliations,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_suppliers,direct,merchant_id,merchant_id column on inventory_suppliers,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_trace_codes,direct,merchant_id,merchant_id column on inventory_trace_codes,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_trace_handover_exceptions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_trace_handover_steps,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_trace_handovers,direct,merchant_id,merchant_id column on inventory_trace_handovers,ready for project/merchant scoped query enforcement,V0.01,covered
-inventory_trace_ownership_history,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_transfer_lines,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_transfer_orders,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-inventory_variance,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-invoice_subject_rules,direct,project_id,project_id column on invoice_subject_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-issued_entitlements,direct,venue_id,venue_id column on issued_entitlements,ready for project/merchant scoped query enforcement,V0.01,covered
-leasing_brands,direct,project_id,project_id column on leasing_brands,ready for project/merchant scoped query enforcement,V0.01,covered
-leasing_follow_ups,direct,project_id,project_id column on leasing_follow_ups,ready for project/merchant scoped query enforcement,V0.01,covered
-leasing_intents,direct,project_id,project_id column on leasing_intents,ready for project/merchant scoped query enforcement,V0.01,covered
-leasing_leads,direct,project_id,project_id column on leasing_leads,ready for project/merchant scoped query enforcement,V0.01,covered
-mall_floor_plans,direct,project_id,project_id column on mall_floor_plans,ready for project/merchant scoped query enforcement,V0.01,covered
-mdm_app_whitelist,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-mdm_policy_violations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-mdm_site_whitelist,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-merchant_bill_disputes,direct,merchant_id,merchant_id column on merchant_bill_disputes,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_bill_dunning_records,direct,merchant_id,merchant_id column on merchant_bill_dunning_records,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_bill_lines,inherited,bill_id,bill_id -> merchant_bills -> merchant_id,must join through scope path before multi-complex rollout,V0.01,covered
-merchant_bills,direct,merchant_id,merchant_id column on merchant_bills,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_expense_entries,direct,merchant_id,merchant_id column on merchant_expense_entries,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_fee_notification_recipient,direct,merchant_id,merchant_id column on merchant_fee_notification_recipient,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_revenue_attribution_rules,direct,merchant_id,merchant_id column on merchant_revenue_attribution_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-merchant_service_work_order_actions,inherited,work_order_id,work_order_id -> merchant_service_work_orders -> merchant_id/space_id,must join through scope path before multi-complex rollout,V0.01,covered
-merchant_service_work_orders,direct,merchant_id,merchant_id column on merchant_service_work_orders,ready for project/merchant scoped query enforcement,V0.01,covered
-merchants,direct,business_entity_id,business_entity_id column on merchants,ready for project/merchant scoped query enforcement,V0.01,covered
-notification_send_log,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-notification_windows,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-open_api_events,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.01,covered
-open_app_credentials,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.01,covered
-open_app_nonce_replays,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.01,covered
-open_app_tokens,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.01,covered
-open_endpoint_configs,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.01,covered
-org_hierarchy,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-partner_voucher_configs,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-partner_voucher_redemptions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-pass_gift_cost_configs,direct,project_id,project_id column on pass_gift_cost_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-pass_gift_cost_variance_ledger,direct,project_id,project_id column on pass_gift_cost_variance_ledger,ready for project/merchant scoped query enforcement,V0.01,covered
-payroll_tax_brackets,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-permission_template_applications,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.01,covered
-permission_template_items,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.01,covered
-permission_template_permissions,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.01,covered
-permission_template_usage_logs,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.01,covered
-permission_templates,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-permissions,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.01,covered
-platform_migration_baseline,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.01,covered
-platform_retention_rules,direct,project_id,project_id column on platform_retention_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-points_accrual_rate_configs,direct,project_id,project_id column on points_accrual_rate_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-position_role_templates,direct,organization_id,organization_id column on position_role_templates,ready for project/merchant scoped query enforcement,V0.01,covered
-production_readiness_check_blockers,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.01,covered
-production_readiness_checks,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.01,covered
-production_readiness_items,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.01,covered
-projects,direct,merchant_id,merchant_id column on projects,ready for project/merchant scoped query enforcement,V0.01,covered
-property_fee_bills,direct,project_id,project_id column on property_fee_bills,ready for project/merchant scoped query enforcement,V0.01,covered
-property_fee_rates,direct,project_id,project_id column on property_fee_rates,ready for project/merchant scoped query enforcement,V0.01,covered
-refund_authority_limits,direct,project_id,project_id column on refund_authority_limits,ready for project/merchant scoped query enforcement,V0.01,covered
-refund_auto_rules,direct,project_id,project_id column on refund_auto_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-revenue_stream_attribution,direct,project_id,project_id column on revenue_stream_attribution,ready for project/merchant scoped query enforcement,V0.01,covered
-risk_alert_actions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-risk_alerts,direct,project_id,project_id column on risk_alerts,ready for project/merchant scoped query enforcement,V0.01,covered
-role_permissions,inherited,role_id/permission_id,role_id/permission_id -> roles/permissions,must join through scope path before multi-complex rollout,V0.01,covered
-roles,direct,organization_id,organization_id column on roles,ready for project/merchant scoped query enforcement,V0.01,covered
-saved_bottle_claims,inherited,saved_bottle_id,saved_bottle_id -> saved_bottles -> merchant/customer scope,must join through scope path before multi-complex rollout,V0.01,covered
-saved_bottle_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-saved_bottles,direct,merchant_id,merchant_id column on saved_bottles,ready for project/merchant scoped query enforcement,V0.01,covered
-service_catalog_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-service_orders,direct,project_id,project_id column on service_orders,ready for project/merchant scoped query enforcement,V0.01,covered
-service_pay_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-service_responsibility_records,direct,merchant_id,merchant_id column on service_responsibility_records,ready for project/merchant scoped query enforcement,V0.01,covered
-settlement_bill_format_shares,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-settlement_bills,direct,project_id,project_id column on settlement_bills,ready for project/merchant scoped query enforcement,V0.01,covered
-settlement_invoice_items,direct,project_id,project_id column on settlement_invoice_items,ready for project/merchant scoped query enforcement,V0.01,covered
-settlement_items,direct,merchant_id,merchant_id column on settlement_items,ready for project/merchant scoped query enforcement,V0.01,covered
-settlement_reconciliation_status,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-settlement_tax_configs,direct,project_id,project_id column on settlement_tax_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-settlement_transactions,direct,project_id,project_id column on settlement_transactions,ready for project/merchant scoped query enforcement,V0.01,covered
-site_entry_reports,direct,project_id,project_id column on site_entry_reports,ready for project/merchant scoped query enforcement,V0.01,covered
-space_expiry_color_rules,direct,project_id,project_id column on space_expiry_color_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-space_type_delete_requests,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-space_type_dictionary,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-spaces,direct,project_id,project_id column on spaces,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_attendance_records,direct,project_id,project_id column on staff_attendance_records,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_base_salaries,direct,project_id,project_id column on staff_base_salaries,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_commission_ledger,direct,project_id,project_id column on staff_commission_ledger,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_device_credentials,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-staff_leave_requests,direct,project_id,project_id column on staff_leave_requests,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_level_assignments,direct,merchant_id,merchant_id column on staff_level_assignments,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_levels,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-staff_locker_records,inherited,staff_profile_id,staff_profile_id -> staff_profiles -> current authenticated employee; cabinet and slot identify storage location,must join through scope path before multi-complex rollout,V0.01,covered
-staff_locker_scan_tokens,inherited,staff_profile_id,staff_profile_id -> staff_profiles -> employee company/merchant scope; one-time scan token presented to the kiosk (U05-006),must join through scope path before multi-complex rollout,V0.01,covered
-staff_payment_codes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-staff_payroll_adjustments,direct,project_id,project_id column on staff_payroll_adjustments,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_payroll_payouts,direct,project_id,project_id column on staff_payroll_payouts,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_payroll_statements,direct,project_id,project_id column on staff_payroll_statements,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_penalty_cap_revisions,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.01,covered
-staff_policy_settings,direct,scope_id,shift_visible_days: scope_id = employer business entity; payroll_penalty_cap_bps: scope_id = global,"employer visibility settings scoped at source; global cap changes require HR, finance, legal and top authority approval",V0.01,covered
-staff_profiles,direct,project_id,project_id column on staff_profiles,ready for project/merchant scoped query enforcement,V0.01,covered
-staff_shift_covers,inherited,shift_id,shift_id -> staff_shifts -> staff_profiles company/merchant scope; cover_user_id must be in the same scope (U06-004),must join through scope path before multi-complex rollout,V0.01,covered
-staff_shift_revisions,inherited,review_id/shift_id,review_id -> supervisor_reviews; published shift_id -> staff_shifts -> staff_profiles company/merchant scope,must join through scope path before multi-complex rollout,V0.01,covered
-staff_shift_schedules,direct,project_id,project_id column on staff_shift_schedules,ready for project/merchant scoped query enforcement,V0.01,covered
-stocktake,direct,merchant_id,merchant_id column on stocktake,ready for project/merchant scoped query enforcement,V0.01,covered
-stored_value_merchant_refund_rules,direct,merchant_id,merchant_id column on stored_value_merchant_refund_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-stored_value_topup_intents,direct,project_id,project_id column on stored_value_topup_intents,ready for project/merchant scoped query enforcement,V0.01,covered
-supervisor_review_decisions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-supervisor_review_nodes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-supervisor_review_policies,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-supplier_qualifications,direct,merchant_id,merchant_id column on supplier_qualifications,ready for project/merchant scoped query enforcement,V0.01,covered
-supplier_settlement_methods,direct,merchant_id,merchant_id column on supplier_settlement_methods,ready for project/merchant scoped query enforcement,V0.01,covered
-supply_contracts,direct,project_id,project_id column on supply_contracts,ready for project/merchant scoped query enforcement,V0.01,covered
-supply_entities,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-temporary_permission_grants,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-terminal_capability_canary_rollouts,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_capability_canary_targets,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_capability_switches,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_device_credentials,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_device_nonces,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_device_signing_keys,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_device_status_events,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_devices,direct,venue_id,venue_id column on terminal_devices,ready for project/merchant scoped query enforcement,V0.01,covered
-terminal_kiosk_configs,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_kiosk_heartbeats,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-terminal_sync_tasks,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.01,covered
-ticket_combo_discount_configs,direct,project_id,project_id column on ticket_combo_discount_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_credentials,inherited,order_id/entitlement_id,order_id/entitlement_id -> ticket_orders/issued_entitlements -> ticket_type/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-ticket_distribution_configs,direct,project_id,project_id column on ticket_distribution_configs,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_distribution_merchant_rates,direct,project_id,project_id column on ticket_distribution_merchant_rates,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_distribution_rank_tiers,direct,project_id,project_id column on ticket_distribution_rank_tiers,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_dynamic_pricing_rules,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_face_photos,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_instances,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_orders,direct,merchant_id,merchant_id column on ticket_orders,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_payment_intents,inherited,order_id,order_id -> ticket_orders -> ticket_type/project scope,must join through scope path before multi-complex rollout,V0.01,covered
-ticket_price_rules,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_qr_refresh_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_refund_lines,inherited,refund_id,refund_id -> ticket_refunds -> order_id -> ticket_orders,must join through scope path before multi-complex rollout,V0.01,covered
-ticket_refunds,direct,merchant_id,merchant_id column on ticket_refunds,ready for project/merchant scoped query enforcement,V0.01,covered
-ticket_slot_holders,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_type_entitlements,inherited,ticket_type_id,ticket_type_id -> ticket_types -> project scope,must join through scope path before multi-complex rollout,V0.01,covered
-ticket_types,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-ticket_verifications,direct,merchant_id,merchant_id column on ticket_verifications,ready for project/merchant scoped query enforcement,V0.01,covered
-turnover_rent_rule_tiers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-turnover_rent_rules,direct,merchant_id,merchant_id column on turnover_rent_rules,ready for project/merchant scoped query enforcement,V0.01,covered
-user_permission_overrides,inherited,user_id,user_id -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.01,covered
-user_position_assignments,direct,merchant_id,merchant_id column on user_position_assignments,ready for project/merchant scoped query enforcement,V0.01,covered
-user_roles,inherited,user_id/role_id,user_id/role_id -> access_users/roles -> organization scope,must join through scope path before multi-complex rollout,V0.01,covered
-venue_business_hours,direct,venue_id,venue_id column on venue_business_hours,ready for project/merchant scoped query enforcement,V0.01,covered
-venue_sessions,direct,venue_id,venue_id column on venue_sessions,ready for project/merchant scoped query enforcement,V0.01,covered
-voucher_definitions,direct,merchant_id,merchant_id column on voucher_definitions,ready for project/merchant scoped query enforcement,V0.01,covered
-voucher_instances,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-voucher_redemption_records,direct,merchant_id,merchant_id column on voucher_redemption_records,ready for project/merchant scoped query enforcement,V0.01,covered
-voucher_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-warehouses,direct,project_id,project_id column on warehouses,ready for project/merchant scoped query enforcement,V0.01,covered
-wecom_access_audits,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-wecom_conversations,direct,project_id,project_id column on wecom_conversations,ready for project/merchant scoped query enforcement,V0.01,covered
-wecom_messages,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-wecom_retention_policies,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
-wecom_risk_hits,direct,project_id,project_id column on wecom_risk_hits,ready for project/merchant scoped query enforcement,V0.01,covered
-wecom_webhook_nonces,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.01,covered
+access_grant_requests,direct,project_id,project_id column on access_grant_requests,ready for project/merchant scoped query enforcement,V0.02,covered
+access_users,direct,merchant_id,merchant_id column on access_users,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_capacity_config,direct,project_id,project_id column on amusement_capacity_config,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_capacity_snapshot,direct,project_id,project_id column on amusement_capacity_snapshot,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_device_statuses,direct,project_id,project_id column on amusement_device_statuses,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_devices,direct,project_id,project_id column on amusement_devices,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_group_bookings,direct,project_id,project_id column on amusement_group_bookings,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_incident_actions,inherited,inspection/incident id,inspection/incident id -> amusement project/session scope,must join through scope path before multi-complex rollout,V0.02,covered
+amusement_incidents,direct,project_id,project_id column on amusement_incidents,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_inspection_tasks,direct,project_id,project_id column on amusement_inspection_tasks,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_maintenance_record,direct,project_id,project_id column on amusement_maintenance_record,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_manual_overrides,direct,project_id,project_id column on amusement_manual_overrides,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_over_capacity_policy,direct,project_id,project_id column on amusement_over_capacity_policy,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_project_entitlements,direct,project_id,project_id column on amusement_project_entitlements,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_project_rules,direct,project_id,project_id column on amusement_project_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_projects,direct,merchant_id,merchant_id column on amusement_projects,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_queue_entries,direct,project_id,project_id column on amusement_queue_entries,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_reservations,direct,project_id,project_id column on amusement_reservations,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_safety_actions,inherited,inspection/incident id,inspection/incident id -> amusement project/session scope,must join through scope path before multi-complex rollout,V0.02,covered
+amusement_safety_inspections,direct,project_id,project_id column on amusement_safety_inspections,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_safety_notices,direct,project_id,project_id column on amusement_safety_notices,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_safety_templates,direct,merchant_id,merchant_id column on amusement_safety_templates,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_sessions,direct,project_id,project_id column on amusement_sessions,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_verifications,direct,project_id,project_id column on amusement_verifications,ready for project/merchant scoped query enforcement,V0.02,covered
+amusement_waiver_acknowledgements,direct,project_id,project_id column on amusement_waiver_acknowledgements,ready for project/merchant scoped query enforcement,V0.02,covered
+analytics_ai_quality_results,global_reference,global_reference,platform-global AI quality result ledger; unfiltered read gated by requirePlatformScope,intentional platform-global AI governance output; not a tenant access boundary,V0.02,covered
+analytics_ai_quality_tasks,global_reference,global_reference,platform-global AI quality task ledger; unfiltered read gated by requirePlatformScope,intentional platform-global AI governance output; not a tenant access boundary,V0.02,covered
+analytics_ai_thresholds,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_anomaly_config,global_reference,global_reference,platform-global anomaly threshold config (singleton id='default'); read/write gated by requirePlatformScope,intentional platform-global; per-tenant thresholds would require a separate scoped table,V0.02,covered
+analytics_business_metric_snapshots,direct,segment_id,"polymorphic (segment_type, segment_id) self-owned scoped-read channel on analytics_business_metric_snapshots","segment_type in {platform(NULL),merchant,project}; merchant/project rows scoped by segment_id (AC-ISO-1)",V0.02,covered
+analytics_customer_intents,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_dashboard_snapshots,direct,project_id,project_id column on analytics_dashboard_snapshots,ready for project/merchant scoped query enforcement,V0.02,covered
+analytics_merchant_health_scores,direct,merchant_id,merchant_id column on analytics_merchant_health_scores,ready for project/merchant scoped query enforcement,V0.02,covered
+analytics_merchant_summaries,direct,merchant_id,merchant_id column on analytics_merchant_summaries,ready for project/merchant scoped query enforcement,V0.02,covered
+analytics_metric_anomalies,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_metric_definitions,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_risk_alerts,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_risk_rules,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_robot_metrics,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+analytics_staff_service_scores,inherited,dimension/project/merchant,dimension/project/merchant fields or derived source table,must join through scope path before multi-complex rollout,V0.02,covered
+approval_lines,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ar_ap_adjustments,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ar_ap_item_sources,inherited,ar_ap_item_id,ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope,must join through scope path before multi-complex rollout,V0.02,covered
+ar_ap_items,direct,project_id,project_id column on ar_ap_items,ready for project/merchant scoped query enforcement,V0.02,covered
+ar_ap_payments,inherited,ar_ap_item_id,ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope,must join through scope path before multi-complex rollout,V0.02,covered
+audit_events,direct,merchant_id,merchant_id column on audit_events,ready for project/merchant scoped query enforcement,V0.02,covered
+auth_credentials,inherited,user_id/session,user_id/session -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.02,covered
+auth_sessions,inherited,user_id/session,user_id/session -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_bill_adjustments,inherited,bill/order linkage,bill/order linkage -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_bill_operations,inherited,bill/order linkage,bill/order linkage -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_bill_settlements,direct,merchant_id,merchant_id column on bar_bill_settlements,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_bills,direct,merchant_id,merchant_id column on bar_bills,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_combo_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+bar_combos,direct,merchant_id,merchant_id column on bar_combos,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_commission_calculations,inherited,bill_id,bill_id -> bar_bills -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_commission_rate_configs,direct,project_id,project_id column on bar_commission_rate_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_deposit_rules,direct,merchant_id,merchant_id column on bar_deposit_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_gift_redemptions,direct,merchant_id,merchant_id column on bar_gift_redemptions,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_order_item_voids,direct,merchant_id,merchant_id column on bar_order_item_voids,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_order_items,inherited,order_id,order_id -> bar_orders -> table_id/bar_id -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_orders,direct,merchant_id,merchant_id column on bar_orders,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_product_categories,direct,merchant_id,merchant_id column on bar_product_categories,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_production_ticket_events,inherited,order_id,order_id -> bar_orders -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_production_tickets,direct,merchant_id,merchant_id column on bar_production_tickets,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_products,direct,merchant_id,merchant_id column on bar_products,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_reservation_invitation_members,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+bar_reservations,direct,merchant_id,merchant_id column on bar_reservations,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_shifts,direct,project_id,project_id column on bar_shifts,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_table_guest_checkins,inherited,reservation/table linkage,reservation_id/table_id -> bar_reservations/bar_tables -> venue/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+bar_table_session_adjustments,direct,merchant_id,merchant_id column on bar_table_session_adjustments,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_table_sessions,direct,merchant_id,merchant_id column on bar_table_sessions,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_tables,direct,merchant_id,merchant_id column on bar_tables,ready for project/merchant scoped query enforcement,V0.02,covered
+bar_void_reason_codes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+business_day_cutoff_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+business_entities,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+business_entity_bindings,direct,merchant_id,merchant_id column on business_entity_bindings,ready for project/merchant scoped query enforcement,V0.02,covered
+business_entity_projects,direct,project_id,project_id column on business_entity_projects,ready for project/merchant scoped query enforcement,V0.02,covered
+business_process_instances,direct,project_id,project_id column on business_process_instances,ready for project/merchant scoped query enforcement,V0.02,covered
+business_process_step_records,inherited,process_instance_id,process_instance_id -> business_process_instances -> merchant_id/project_id,must join through scope path before multi-complex rollout,V0.02,covered
+business_review_locks,direct,project_id,project_id column on business_review_locks,ready for project/merchant scoped query enforcement,V0.02,covered
+channel_fee_configs,direct,project_id,project_id column on channel_fee_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+complex_operating_params,direct,project_id,project_id column on complex_operating_params,ready for project/merchant scoped query enforcement,V0.02,covered
+compliance_expiry_items,direct,merchant_id,merchant_id column on compliance_expiry_items,ready for project/merchant scoped query enforcement,V0.02,covered
+compliance_reminders,direct,merchant_id,merchant_id column on compliance_reminders,ready for project/merchant scoped query enforcement,V0.02,covered
+comprehensive_service_fee_configs,direct,project_id,project_id column on comprehensive_service_fee_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_approval_flow_steps,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+contract_approval_flows,direct,project_id,project_id column on contract_approval_flows,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_approvals,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+contract_changes,direct,project_id,project_id column on contract_changes,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_key_dates,direct,merchant_id,merchant_id column on contract_key_dates,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_rent_bills,direct,project_id,project_id column on contract_rent_bills,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_settlements,direct,project_id,project_id column on contract_settlements,ready for project/merchant scoped query enforcement,V0.02,covered
+contract_spaces,direct,project_id,project_id column on contract_spaces,ready for project/merchant scoped query enforcement,V0.02,covered
+contracts,direct,project_id,project_id column on contracts,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_birthday_rules,direct,project_id,project_id column on customer_birthday_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_check_in_tiers,direct,project_id,project_id column on customer_check_in_tiers,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_check_ins,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+customer_consents,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+customer_member_levels,direct,project_id,project_id column on customer_member_levels,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_points_accounts,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+customer_points_ledger,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+customer_points_redemptions,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+customer_points_rules,direct,project_id,project_id column on customer_points_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_redemption_catalog_items,direct,project_id,project_id column on customer_redemption_catalog_items,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_sessions,inherited,customer_id,customer_id -> customers -> project/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+customer_stored_value_accounts,direct,project_id,project_id column on customer_stored_value_accounts,ready for project/merchant scoped query enforcement,V0.02,covered
+customer_stored_value_transactions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+customers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+data_cleanup_jobs,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.02,covered
+deposit_ledger,direct,merchant_id,merchant_id column on deposit_ledger,ready for project/merchant scoped query enforcement,V0.02,covered
+dividend_deduction_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+dividend_plans,direct,merchant_id,merchant_id column on dividend_plans,ready for project/merchant scoped query enforcement,V0.02,covered
+dividend_reserve_releases,direct,merchant_id,merchant_id column on dividend_reserve_releases,ready for project/merchant scoped query enforcement,V0.02,covered
+dividend_statement_lines,direct,merchant_id,merchant_id column on dividend_statement_lines,ready for project/merchant scoped query enforcement,V0.02,covered
+dividend_statements,direct,merchant_id,merchant_id column on dividend_statements,ready for project/merchant scoped query enforcement,V0.02,covered
+employee_notices,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+energy_anomaly_thresholds,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+energy_meter_readings,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+energy_meters,direct,project_id,project_id column on energy_meters,ready for project/merchant scoped query enforcement,V0.02,covered
+entitlement_day_activations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+entitlement_entry_records,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+entitlement_sessions,direct,project_id,project_id column on entitlement_sessions,ready for project/merchant scoped query enforcement,V0.02,covered
+entitlement_templates,direct,venue_id,venue_id column on entitlement_templates,ready for project/merchant scoped query enforcement,V0.02,covered
+evidence_attachments,direct,merchant_id,merchant_id column on evidence_attachments,ready for project/merchant scoped query enforcement,V0.02,covered
+evidence_events,direct,merchant_id,merchant_id column on evidence_events,ready for project/merchant scoped query enforcement,V0.02,covered
+evidence_reviews,direct,merchant_id,merchant_id column on evidence_reviews,ready for project/merchant scoped query enforcement,V0.02,covered
+external_partner_orgs,direct,project_id,project_id column on external_partner_orgs,ready for project/merchant scoped query enforcement,V0.02,covered
+facility_equipment,direct,project_id,project_id column on facility_equipment,ready for project/merchant scoped query enforcement,V0.02,covered
+facility_inspection_records,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+facility_inspection_tasks,direct,project_id,project_id column on facility_inspection_tasks,ready for project/merchant scoped query enforcement,V0.02,covered
+facility_inspection_templates,direct,project_id,project_id column on facility_inspection_templates,ready for project/merchant scoped query enforcement,V0.02,covered
+facility_work_order_policy,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+facility_work_orders,direct,project_id,project_id column on facility_work_orders,ready for project/merchant scoped query enforcement,V0.02,covered
+finance_daily_close_summaries,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+finance_daily_closes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+finance_offline_payment_confirmations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+finance_payment_intents,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+finance_reconciliation_batches,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+finance_reconciliation_differences,inherited,batch_id,batch_id -> finance_reconciliation_batches -> project/entity scope,must join through scope path before multi-complex rollout,V0.02,covered
+finance_refunds,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+floor_plan_import_candidates,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.02,covered
+floor_plan_import_errors,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.02,covered
+floor_plan_import_tasks,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.02,covered
+floor_plan_shapes,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scope,must join through scope path before multi-complex rollout,V0.02,covered
+giveaway_cost_configs,direct,project_id,project_id column on giveaway_cost_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+high_value_thresholds,direct,project_id,project_id column on high_value_thresholds,ready for project/merchant scoped query enforcement,V0.02,covered
+holiday_calendar,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+idempotency_records,inherited,operation_key,operation_key -> request route + actor + business operation,must join through scope path before multi-complex rollout,V0.02,covered
+integration_channel_store_map,direct,merchant_id,merchant_id column on integration_channel_store_map,ready for project/merchant scoped query enforcement,V0.02,covered
+integration_channel_ticket_type_map,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_compensation_tasks,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_order_syncs,direct,merchant_id,merchant_id column on integration_order_syncs,ready for project/merchant scoped query enforcement,V0.02,covered
+integration_provider_adapter_tests,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_provider_adapters,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_read_ledger,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_reconciliation_batches,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_reconciliation_differences,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_retry_tasks,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_robot_task_events,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_video_access_approvals,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_video_access_audits,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+integration_video_anchors,inherited,provider_code/project linkage,provider_code/project linkage -> integration_provider_adapters,must join through scope path before multi-complex rollout,V0.02,covered
+internal_price_catalog,direct,merchant_id,merchant_id column on internal_price_catalog,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_high_value_reviews,direct,merchant_id,merchant_id column on inventory_high_value_reviews,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_item_cost_state,direct,merchant_id,merchant_id column on inventory_item_cost_state,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_items,direct,merchant_id,merchant_id column on inventory_items,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_purchase_request_events,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_purchase_requests,direct,merchant_id,merchant_id column on inventory_purchase_requests,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_risk_alerts,direct,merchant_id,merchant_id column on inventory_risk_alerts,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_stock_adjustments,direct,merchant_id,merchant_id column on inventory_stock_adjustments,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_stock_balances,direct,project_id,project_id column on inventory_stock_balances,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_stock_batches,direct,project_id,project_id column on inventory_stock_batches,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_stock_movements,direct,project_id,project_id column on inventory_stock_movements,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_supplier_reconciliations,direct,merchant_id,merchant_id column on inventory_supplier_reconciliations,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_suppliers,direct,merchant_id,merchant_id column on inventory_suppliers,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_trace_codes,direct,merchant_id,merchant_id column on inventory_trace_codes,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_trace_handover_exceptions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_trace_handover_steps,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_trace_handovers,direct,merchant_id,merchant_id column on inventory_trace_handovers,ready for project/merchant scoped query enforcement,V0.02,covered
+inventory_trace_ownership_history,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_transfer_lines,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_transfer_orders,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+inventory_variance,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+invoice_subject_rules,direct,project_id,project_id column on invoice_subject_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+issued_entitlements,direct,venue_id,venue_id column on issued_entitlements,ready for project/merchant scoped query enforcement,V0.02,covered
+leasing_brands,direct,project_id,project_id column on leasing_brands,ready for project/merchant scoped query enforcement,V0.02,covered
+leasing_follow_ups,direct,project_id,project_id column on leasing_follow_ups,ready for project/merchant scoped query enforcement,V0.02,covered
+leasing_intents,direct,project_id,project_id column on leasing_intents,ready for project/merchant scoped query enforcement,V0.02,covered
+leasing_leads,direct,project_id,project_id column on leasing_leads,ready for project/merchant scoped query enforcement,V0.02,covered
+mall_floor_plans,direct,project_id,project_id column on mall_floor_plans,ready for project/merchant scoped query enforcement,V0.02,covered
+mdm_app_whitelist,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+mdm_policy_violations,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+mdm_site_whitelist,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+merchant_bill_disputes,direct,merchant_id,merchant_id column on merchant_bill_disputes,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_bill_dunning_records,direct,merchant_id,merchant_id column on merchant_bill_dunning_records,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_bill_lines,inherited,bill_id,bill_id -> merchant_bills -> merchant_id,must join through scope path before multi-complex rollout,V0.02,covered
+merchant_bills,direct,merchant_id,merchant_id column on merchant_bills,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_expense_entries,direct,merchant_id,merchant_id column on merchant_expense_entries,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_fee_notification_recipient,direct,merchant_id,merchant_id column on merchant_fee_notification_recipient,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_revenue_attribution_rules,direct,merchant_id,merchant_id column on merchant_revenue_attribution_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+merchant_service_work_order_actions,inherited,work_order_id,work_order_id -> merchant_service_work_orders -> merchant_id/space_id,must join through scope path before multi-complex rollout,V0.02,covered
+merchant_service_work_orders,direct,merchant_id,merchant_id column on merchant_service_work_orders,ready for project/merchant scoped query enforcement,V0.02,covered
+merchants,direct,project_id,project_id column on merchants,ready for project/merchant scoped query enforcement,V0.02,covered
+notification_send_log,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+notification_windows,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+open_api_events,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.02,covered
+open_app_credentials,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.02,covered
+open_app_nonce_replays,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.02,covered
+open_app_tokens,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.02,covered
+open_endpoint_configs,inherited,open app/endpoint key,open app/endpoint key -> provider/project integration scope,must join through scope path before multi-complex rollout,V0.02,covered
+org_hierarchy,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+partner_voucher_configs,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+partner_voucher_redemptions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+pass_gift_cost_configs,direct,project_id,project_id column on pass_gift_cost_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+pass_gift_cost_variance_ledger,direct,project_id,project_id column on pass_gift_cost_variance_ledger,ready for project/merchant scoped query enforcement,V0.02,covered
+payroll_tax_brackets,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+permission_template_applications,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.02,covered
+permission_template_items,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.02,covered
+permission_template_permissions,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.02,covered
+permission_template_usage_logs,inherited,template_id,template_id -> permission_templates -> organization/global scope,must join through scope path before multi-complex rollout,V0.02,covered
+permission_templates,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+permissions,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.02,covered
+platform_migration_baseline,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.02,covered
+platform_retention_rules,direct,project_id,project_id column on platform_retention_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+points_accrual_rate_configs,direct,project_id,project_id column on points_accrual_rate_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+position_role_templates,direct,organization_id,organization_id column on position_role_templates,ready for project/merchant scoped query enforcement,V0.02,covered
+production_readiness_check_blockers,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.02,covered
+production_readiness_checks,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.02,covered
+production_readiness_items,inherited,owner/blocker code,owner/blocker code -> production evidence scope,must join through scope path before multi-complex rollout,V0.02,covered
+projects,direct,merchant_id,merchant_id column on projects,ready for project/merchant scoped query enforcement,V0.02,covered
+property_fee_bills,direct,project_id,project_id column on property_fee_bills,ready for project/merchant scoped query enforcement,V0.02,covered
+property_fee_rates,direct,project_id,project_id column on property_fee_rates,ready for project/merchant scoped query enforcement,V0.02,covered
+refund_authority_limits,direct,project_id,project_id column on refund_authority_limits,ready for project/merchant scoped query enforcement,V0.02,covered
+refund_auto_rules,direct,project_id,project_id column on refund_auto_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+revenue_stream_attribution,direct,project_id,project_id column on revenue_stream_attribution,ready for project/merchant scoped query enforcement,V0.02,covered
+risk_alert_actions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+risk_alerts,direct,project_id,project_id column on risk_alerts,ready for project/merchant scoped query enforcement,V0.02,covered
+role_permissions,inherited,role_id/permission_id,role_id/permission_id -> roles/permissions,must join through scope path before multi-complex rollout,V0.02,covered
+roles,direct,organization_id,organization_id column on roles,ready for project/merchant scoped query enforcement,V0.02,covered
+saved_bottle_claims,inherited,saved_bottle_id,saved_bottle_id -> saved_bottles -> merchant/customer scope,must join through scope path before multi-complex rollout,V0.02,covered
+saved_bottle_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+saved_bottles,direct,merchant_id,merchant_id column on saved_bottles,ready for project/merchant scoped query enforcement,V0.02,covered
+service_catalog_items,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+service_orders,direct,project_id,project_id column on service_orders,ready for project/merchant scoped query enforcement,V0.02,covered
+service_pay_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+service_responsibility_records,direct,merchant_id,merchant_id column on service_responsibility_records,ready for project/merchant scoped query enforcement,V0.02,covered
+settlement_bill_format_shares,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+settlement_bills,direct,project_id,project_id column on settlement_bills,ready for project/merchant scoped query enforcement,V0.02,covered
+settlement_invoice_items,direct,project_id,project_id column on settlement_invoice_items,ready for project/merchant scoped query enforcement,V0.02,covered
+settlement_items,direct,merchant_id,merchant_id column on settlement_items,ready for project/merchant scoped query enforcement,V0.02,covered
+settlement_reconciliation_status,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+settlement_tax_configs,direct,project_id,project_id column on settlement_tax_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+settlement_transactions,direct,project_id,project_id column on settlement_transactions,ready for project/merchant scoped query enforcement,V0.02,covered
+site_entry_reports,direct,project_id,project_id column on site_entry_reports,ready for project/merchant scoped query enforcement,V0.02,covered
+space_expiry_color_rules,direct,project_id,project_id column on space_expiry_color_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+space_type_delete_requests,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+space_type_dictionary,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+spaces,direct,project_id,project_id column on spaces,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_attendance_records,direct,project_id,project_id column on staff_attendance_records,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_base_salaries,direct,project_id,project_id column on staff_base_salaries,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_commission_ledger,direct,project_id,project_id column on staff_commission_ledger,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_device_credentials,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+staff_leave_requests,direct,project_id,project_id column on staff_leave_requests,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_level_assignments,direct,merchant_id,merchant_id column on staff_level_assignments,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_levels,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+staff_locker_records,inherited,staff_profile_id,staff_profile_id -> staff_profiles -> current authenticated employee; cabinet and slot identify storage location,must join through scope path before multi-complex rollout,V0.02,covered
+staff_locker_scan_tokens,inherited,staff_profile_id,staff_profile_id -> staff_profiles -> employee company/merchant scope; one-time scan token presented to the kiosk (U05-006),must join through scope path before multi-complex rollout,V0.02,covered
+staff_payment_codes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+staff_payroll_adjustments,direct,project_id,project_id column on staff_payroll_adjustments,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_payroll_payouts,direct,project_id,project_id column on staff_payroll_payouts,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_payroll_statements,direct,project_id,project_id column on staff_payroll_statements,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_penalty_cap_revisions,global_reference,global_reference,global reference/configuration table,safe as global reference; tenant-specific overrides require separate scoped table,V0.02,covered
+staff_policy_settings,direct,scope_id,shift_visible_days: scope_id = employer business entity; payroll_penalty_cap_bps: scope_id = global,"employer visibility settings scoped at source; global cap changes require HR, finance, legal and top authority approval",V0.02,covered
+staff_profiles,direct,project_id,project_id column on staff_profiles,ready for project/merchant scoped query enforcement,V0.02,covered
+staff_shift_covers,inherited,shift_id,shift_id -> staff_shifts -> staff_profiles company/merchant scope; cover_user_id must be in the same scope (U06-004),must join through scope path before multi-complex rollout,V0.02,covered
+staff_shift_revisions,inherited,review_id/shift_id,review_id -> supervisor_reviews; published shift_id -> staff_shifts -> staff_profiles company/merchant scope,must join through scope path before multi-complex rollout,V0.02,covered
+staff_shift_schedules,direct,project_id,project_id column on staff_shift_schedules,ready for project/merchant scoped query enforcement,V0.02,covered
+stocktake,direct,merchant_id,merchant_id column on stocktake,ready for project/merchant scoped query enforcement,V0.02,covered
+stored_value_merchant_refund_rules,direct,merchant_id,merchant_id column on stored_value_merchant_refund_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+stored_value_topup_intents,direct,project_id,project_id column on stored_value_topup_intents,ready for project/merchant scoped query enforcement,V0.02,covered
+supervisor_review_decisions,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+supervisor_review_nodes,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+supervisor_review_policies,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+supplier_qualifications,direct,merchant_id,merchant_id column on supplier_qualifications,ready for project/merchant scoped query enforcement,V0.02,covered
+supplier_settlement_methods,direct,merchant_id,merchant_id column on supplier_settlement_methods,ready for project/merchant scoped query enforcement,V0.02,covered
+supply_contracts,direct,project_id,project_id column on supply_contracts,ready for project/merchant scoped query enforcement,V0.02,covered
+supply_entities,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+temporary_permission_grants,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+terminal_capability_canary_rollouts,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_capability_canary_targets,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_capability_switches,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_device_credentials,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_device_nonces,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_device_signing_keys,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_device_status_events,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_devices,direct,venue_id,venue_id column on terminal_devices,ready for project/merchant scoped query enforcement,V0.02,covered
+terminal_kiosk_configs,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_kiosk_heartbeats,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+terminal_sync_tasks,inherited,terminal/device id,terminal/device id -> venue/project deployment scope,must join through scope path before multi-complex rollout,V0.02,covered
+ticket_combo_discount_configs,direct,project_id,project_id column on ticket_combo_discount_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_credentials,inherited,order_id/entitlement_id,order_id/entitlement_id -> ticket_orders/issued_entitlements -> ticket_type/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+ticket_distribution_configs,direct,project_id,project_id column on ticket_distribution_configs,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_distribution_merchant_rates,direct,project_id,project_id column on ticket_distribution_merchant_rates,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_distribution_rank_tiers,direct,project_id,project_id column on ticket_distribution_rank_tiers,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_dynamic_pricing_rules,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_face_photos,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_instances,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_orders,direct,merchant_id,merchant_id column on ticket_orders,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_payment_intents,inherited,order_id,order_id -> ticket_orders -> ticket_type/project scope,must join through scope path before multi-complex rollout,V0.02,covered
+ticket_price_rules,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_qr_refresh_config,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_refund_lines,inherited,refund_id,refund_id -> ticket_refunds -> order_id -> ticket_orders,must join through scope path before multi-complex rollout,V0.02,covered
+ticket_refunds,direct,merchant_id,merchant_id column on ticket_refunds,ready for project/merchant scoped query enforcement,V0.02,covered
+ticket_slot_holders,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_type_entitlements,inherited,ticket_type_id,ticket_type_id -> ticket_types -> project scope,must join through scope path before multi-complex rollout,V0.02,covered
+ticket_types,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+ticket_verifications,direct,merchant_id,merchant_id column on ticket_verifications,ready for project/merchant scoped query enforcement,V0.02,covered
+turnover_rent_rule_tiers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+turnover_rent_rules,direct,merchant_id,merchant_id column on turnover_rent_rules,ready for project/merchant scoped query enforcement,V0.02,covered
+user_permission_overrides,inherited,user_id,user_id -> access_users -> organization scope,must join through scope path before multi-complex rollout,V0.02,covered
+user_position_assignments,direct,merchant_id,merchant_id column on user_position_assignments,ready for project/merchant scoped query enforcement,V0.02,covered
+user_roles,inherited,user_id/role_id,user_id/role_id -> access_users/roles -> organization scope,must join through scope path before multi-complex rollout,V0.02,covered
+venue_business_hours,direct,venue_id,venue_id column on venue_business_hours,ready for project/merchant scoped query enforcement,V0.02,covered
+venue_sessions,direct,venue_id,venue_id column on venue_sessions,ready for project/merchant scoped query enforcement,V0.02,covered
+voucher_definitions,direct,merchant_id,merchant_id column on voucher_definitions,ready for project/merchant scoped query enforcement,V0.02,covered
+voucher_instances,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+voucher_redemption_records,direct,merchant_id,merchant_id column on voucher_redemption_records,ready for project/merchant scoped query enforcement,V0.02,covered
+voucher_transfers,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+warehouses,direct,project_id,project_id column on warehouses,ready for project/merchant scoped query enforcement,V0.02,covered
+wecom_access_audits,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+wecom_conversations,direct,project_id,project_id column on wecom_conversations,ready for project/merchant scoped query enforcement,V0.02,covered
+wecom_messages,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+wecom_retention_policies,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
+wecom_risk_hits,direct,project_id,project_id column on wecom_risk_hits,ready for project/merchant scoped query enforcement,V0.02,covered
+wecom_webhook_nonces,single_complex_default,P0_SINGLE_COMPLEX,single-complex default scope; no direct scope column detected,"before multi-complex rollout, add direct project_id/merchant_id or document parent join",V0.02,covered
diff --git a/tools/generate-domain-ownership-scope-audit.mjs b/tools/generate-domain-ownership-scope-audit.mjs
index d2b756d0..f530e3ab 100644
--- a/tools/generate-domain-ownership-scope-audit.mjs
+++ b/tools/generate-domain-ownership-scope-audit.mjs
@@ -1,4 +1,4 @@
-// 文件版本：V0.04
+// 文件版本：V0.05
 import { readFileSync, writeFileSync, existsSync } from "node:fs";
 import { readdirSync } from "node:fs";
 import { join } from "node:path";
@@ -201,7 +201,7 @@ function buildCsv() {
     return {
       table_name: table,
       ...classification,
-      file_version: "V0.01",
+      file_version: "V0.02",
       status: "covered",
     };
   });
~~~~
