CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M021-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M021 / 共 471，B3 第 1 次之后的 B4。计划名：商户档案、商户类型与商户侧门户／数据库迁移／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M021-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V2.66 顶部「当前 M021 B2-r1 双PASS定稿」、本席旧 `M021-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、冻结 diff 与两 tree、现行 V428 SQL、`SettlementFormatShareS11Tests` 新例与既有 `@AfterEach`、V384/V366 CHECK 原文、HTML 枚举 L195 与业务地图 L1690、`TicketDistributionSettlementService.ENTITY_BENEFICIARY_FORMATS`／`aggregateFormatRevenue`／`upsertFormatShare`（只读，不在 diff 内）、`application.yml` flyway 块、门禁 spec 现行「最新迁移 V423」、TDD 三份日志、本席对 `complex_test` 的只读 SELECT。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、本席只读探针与既有 RED/GREEN 日志为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M021-001 是否按 findings V2.66 定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID（仅 001）；测试是否按真实现语义。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿证据并本席只读复核，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep`/`ask --source` 定位 `upsertFormatShare`/`aggregateFormatRevenue`/`MERCHANT_BUSINESS_FORMATS`/`ENTITY_BENEFICIARY_FORMATS`/`be_media` | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | CHECK 是词表封口的深模块接口；本 diff 不把第二套枚举写进 Java | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、pg_constraint SELECT、读 RED 违例行） | 本席环境未预导出三变量；禁止 `source /etc/complex/complex.env`；未本席重跑 JDBC（会写库） |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→V428 DROP/ADD→`settleMonth`→`upsertFormatShare`→`chk_sbfs_format`）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑哈希/`cmp`/只读 SELECT；JDBC 8 例不以「应能过」代替，改核 RED/GREEN 日志+surefire XML+库实态 | 本席不重跑会写库的 8 例，缺口见 §1 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 RED 构造器成立（无 V428 时 `chk_sbfs_format` 拒 media 行）+ GREEN 8/8；本席不重写测试 | 纯 SQL 本可不强制测；本轮按定稿走跨单元真实库例，合法 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：3 条 SQL + 1 例复用夹具，ultra 已满足 | 不修 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（复核用 `/tmp/m021_b4_r1_verify.diff` 与三键内存文件均已删）。

graft 本席调用节省合计约 140732 tokens（`upsertFormatShare` / `aggregateFormatRevenue` / `MERCHANT_BUSINESS_FORMATS` / `be_media` / `settleMonth` 映射）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。findings.md 版本 V2.66，本席 `sha256sum` = `000fd0582f0a9354f0803454749fe50daacb0173569b0119ecaeba467ae55b12`。160-audit-prompt.md V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`。

| 项 | 值 |
|---|---|
| 模块 | M021 数据库迁移／第1段（计划 5 文件，有效内容行 103；本轮 B3 写入集合＝新迁移 + 别模块真实库测试类 1 例） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V2.66：M021-001 冲突／是／是；M021-002 技术性／否／否（只登记，本 diff 未改 V200） |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `0ea996b1a680dc068d0486da7f22e68fc68353c6` |
| after（对象类型 tree） | `a5e4fadfe03ff042c00bd240719b76d9ab7341d6` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 0ea996b1a680dc068d0486da7f22e68fc68353c6 a5e4fadfe03ff042c00bd240719b76d9ab7341d6`（无路径过滤） |
| 实际差异 | 4412 bytes；SHA256 `85adf53df99703a9b7fad430374c693ceb4133c9c57df17f80490ab0ff2a8ce9`；2 files changed, 45 insertions(+)；3 hunk；UTF-8 |
| 与冻结文件 `/home/james/audit-tmp/m021_b3_r1.diff` | 字节、SHA256、`cmp` 全等 |
| `git diff --check` | exit 0 |
| `git diff --name-status` | `A` V428 SQL；`M` `SettlementFormatShareS11Tests.java`；无 D/R；无第三路径 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（两改动文件工作区 blob 与 after tree 全等） |
| V147/V199/V200/V348/V366/V384 / 结算 Java / 三 HTML | 本 diff 路径清单为空；两 tree 上 V366/V384/V147/V199/V200/V348/两份业务 HTML blob 全等 |
| V427（M009 遗留） | 两 tree 均含且 blob 同为 `e485a5d29fea55fa714091485aa26facc8c45160`，故不出现在本 diff |
| TDD | RED `/home/james/audit-tmp/m021_b3r1_red.log`（SHA `5bc002f10e67113471425f9491b2c2f7da5e037b271b31a83031d94cc4fb5d01`；8 例 Errors:1=新例 `chk_sbfs_format`，Failures:0）；GREEN 日志文件本身只截到 Flyway 升到 428 的启动段（31 行）；8/8 成功摘自 `m021_b3r1_run.log`（SHA `dcb22134d719780b139c2d0dc50ba5c29b61dd6c6dc0920a8cf7518a17da45d0`，`GREEN_MVN_EXIT=0`，`Tests run: 8, Failures: 0, Errors: 0`）+ 工作区残留 surefire XML `tests="8" errors="0" failures="0" time="4.65"`（含 `mediaShareIsRecordedAndAttributedToMediaEntity`） |
| 只读探针 | 本席未 `source /etc/complex/complex.env`（该文件 URL 指向生产库 `complex`）。只抽三键后把 URL 改写为 `jdbc:postgresql://127.0.0.1:5432/complex_test` 再 SELECT。本席**未**查询生产库。 |

两改动文件 blob（`git rev-parse TREE:path` / 工作区 `git hash-object`）与内容 SHA256：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 |
|---|---|---|---:|---|
| `backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql` | （不存在） | `5920e7d53d41952129085f4626e1baaf82c4c6af` | 13 | `239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589` |
| `backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java` | `19bd09983428761d0b47506bcf360d8e53e20dca` | `f4559e5fb10277741b209e78b883b028d93ff0f8` | 495 | `0649e4dedac9e93442625f530a2612479695e8ab29ca5014f25578f5d752e7fa` |

before 测试文件内容 SHA256 `26f9b8476b5cfd4ee1139b547f8158d07da3981ed47954518cecf99d7d4be591`（7 个 `@Test`，无版本标记、无 `mediaShare`）。after 8 个 `@Test`。

本席未改任何源文件，只新建本 raw。未 commit/push/PR/merge/部署；未对主索引 `git add`。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M021 的集合（GEN-001…018）。本轮只用定稿 001 已引用的 ID（GEN-014 / GEN-006 / GEN-008 / GEN-002 / GEN-003；上位 HTML 枚举 L195、业务地图 L1309 六值业态句、L1690 演出票→传媒公司）。不 LOCK-AMEND。四分类本轮不重开。002 本轮仍「否」，diff 未改 V200。

B2-r1 定稿（findings.md V2.66）最小处置（本席内容票的尺子）：

- **001** 冲突／是：已应用 V366/V384 不可改写 → 新增前向迁移 `V428__m021_settlement_format_share_media.sql`（V0.01；`DROP CONSTRAINT IF EXISTS chk_sbfs_format` 再 ADD 同名六值 CHECK；表为月结留痕小表，**不用 NOT VALID**）；TDD 在既有真实库类 `SettlementFormatShareS11Tests` 加一例 media 业态商户进月结（份额行 `business_format=media`、`beneficiary_kind=entity`、`beneficiary_entity_id=be_media`），RED（CHECK 违例）后加迁移转 GREEN；随新迁移重生成门禁产物并跑全链 `verify:predeploy`（见 §5 对席甲决定②的独立票）；不改结算 Java、不改 V366/V384/HTML。编号：origin/main 最大 V426，主工作区有已应用的 M009 遗留 V427，故取 V428。
- **002** 否：本 diff 未碰，合规。

---

## 3. diff 逐块归因（闸 3）

3 hunk，全部变更行都能绑到 V2.66 归属「是」的唯一 ID **M021-001**。无无 ID 行。验证性测试归因到对应 ID（闸 3 明文允许）。版本标记行按派发口径：SQL V0.01 → 001；测试 V0.01 → 跨单元 TDD 001。

| # | 文件 | hunk | 变更行（摘要） | ID | 越界? |
|---|---|---|---|---|---|
| 1 | `V428__m021_settlement_format_share_media.sql` | `@@ -0,0 +1,13`（新文件 mode 100644） | `//` 注释风格的 `-- 文件版本：V0.01` | 001 | 否 |
| 1 | 同上 | 同上 | L2–L9 注释：V384 漏第三处 CHECK、HTML 六值与「演出票→传媒公司」、不改写 V366/V384 | 001 | 否 |
| 1 | 同上 | 同上 | `ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;` | 001 | 否 |
| 1 | 同上 | 同上 | `ADD CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));` | 001 | 否 |
| 2 | `SettlementFormatShareS11Tests.java` | `@@ -1,4 +1,5` | `// 文件版本：V0.01`（本文件首次版本标记） | 001（跨单元 TDD） | 否 |
| 3 | 同上 | `@@ -362,6 +363,37` | 节标题「⑤ 传媒业态份额（M021-001 / V428）」 | 001 TDD | 否 |
| 3 | 同上 | 同上 | javadoc：六值词表、V428 放开封口、V384 种子 `be_media`、不进酒吧池 | 001 TDD | 否 |
| 3 | 同上 | 同上 | `@Test void mediaShareIsRecordedAndAttributedToMediaEntity`：`newProject`+`seedMerchant(..., "media")`+`settleMonth`；断言 `bar`/`media` 两行、净额 20000、`beneficiary_kind=entity`、`beneficiary_entity_id=be_media`、`ticket_revenue_cent=70000`、`available_pool_cent=50000`、`itemAmounts` 只有酒吧商户 | 001 TDD | 否 |

无第四路径、无删除行、无重命名。M021-002 所需的 V200 不在 diff 内（归属「否」，不动是对的）。

---

## 4. 内容审计

### 4.1 前向迁移是否按定稿最小处置

V428 全文 13 行。可执行语句恰好两条：

```
ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;
ALTER TABLE settlement_bill_format_shares
    ADD CONSTRAINT chk_sbfs_format
    CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
```

- 只 DROP IF EXISTS 后 ADD **同名**六值 CHECK：是。
- 全文无 `NOT VALID`、无 `VALIDATE CONSTRAINT`、无金额列、无 DML、无 GRANT 变更：是。
- 不改 V366/V384：两 tree blob 全等；`git diff` 这两文件 0 字节。
- 不改结算 Java / HTML：`git diff --name-only` 对 `backend/src/main/java/**`、`apps/**`、`docs/*.html` 为空。
- ponytail ultra：3 条物理行的 DDL，复用既有约束名。Lean already. Ship.

### 4.2 六值闭集逐值一致

HTML 枚举 `docs/business-enum-quick-reference-v0.1.html:195` 原文：「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。」

业务地图 L1690：「演出票」→「传媒公司」。L1309 商户管理段同六值。

V384 现行（未改）：

- `chk_merchants_business_format`：`('bar', 'catering', 'retail', 'amusement', 'media', 'other')`
- `chk_leasing_leads_business_format`：NULL 或同一六值

`PlatformService.MERCHANT_BUSINESS_FORMATS`（不在 diff 内，对照用）：`Set.of("bar", "catering", "retail", "amusement", "media", "other")`

V428 ADD 列表与上述 **逐值、同序** 一致。本席 `complex_test` 只读 SELECT 三处 `pg_get_constraintdef` 实态均为同一 ARRAY 六值；`checks_same_vocab_without_media = 0`。

### 4.3 VARCHAR(16) 是否容得下 `media`

V366:31 `business_format VARCHAR(16) NOT NULL`（两 tree 未改）。本席 SELECT `format_type = character varying(16)`。`media` 5 字符；六值最长 `amusement` 9 字符。容得下。本 diff 未扩列宽（也不需要）。

### 4.4 Flyway 版本号与文件名

- 文件名 `V428__m021_settlement_format_share_media.sql` 匹配 `^V\d+__[a-z0-9_]+\.sql$`。
- `origin/main` 最大版本文件 V426。V428 > V426，满足 `verify:migration-version-order`「本分支新增 V 号必须大于 origin/main 当前最大 V 号」。
- 主工作区另有 staged `V427__m009_project_profiles_and_associations.sql`（M009 遗留，不属本模块）。两 tree 均已包含该 blob，故本 diff 不带它。
- `application.yml:30` `validate-on-migrate: true`；yml 树无 `outOfOrder` / `out-of-order`。
- GREEN 日志：`Successfully validated 396 migrations`（RED 为 395）→ `Migrating schema "public" to version "428 - m021 settlement format share media"` → `now at version v428`。
- 本席 SELECT `flyway_schema_history`：`428 | m021 settlement format share media | t | 1696678370`，与 `m021_b3r1_run.log` 行 20 同 checksum。V366 checksum 仍 `1936730037`、V384 仍 `-663687440`——已应用迁移零改写。

### 4.5 测试是否按真实现语义

新例 `mediaShareIsRecordedAndAttributedToMediaEntity`：

| 定稿要求 | 实核 |
|---|---|
| 真实库 `settleMonth` | `settlementService.settleMonth(pid, MONTH)`；类注释写明非 `@Transactional`（内部真提交） |
| media 业态份额行写入 | `seedMerchant("m_fmt_media", "media")`；断言 `shares.keySet()` 含 `media`，净额 20000L |
| `beneficiary_kind=entity` | 断言等于 `"entity"` |
| `beneficiary_entity_id=be_media` 来自 V384 全局种子 | 测试**不**插入 `revenue_stream_attribution`。Java 既有 `ENTITY_BENEFICIARY_FORMATS`：`FORMAT_MEDIA("media") → "performance_ticket"`（`:76/:87-90`），再 `resolveManagementEntity`。本席 SELECT：`rsa_performance_ticket_default` / `project_id IS NULL` / `performance_ticket` / `be_media` / `entity` / `active` / `is_draft=f`；`business_entities.id=be_media` 存在 |
| 不进酒吧池 | `ticket_revenue_cent=70000`（酒吧 50000+传媒 20000）且 `available_pool_cent=50000` |
| 分池明细只有酒吧 | `itemAmounts(pid).containsOnlyKeys("m_fmt_bar_md")` |
| 清场依赖既有 `@AfterEach` | 新例只调既有 `newProject()`（登记 `projectIds`）与 `seedMerchant`（登记 `merchantIds`）。diff **不改** `@AfterEach`。既有清场已 `DELETE settlement_bill_format_shares` / `settlement_items` / `settlement_bills` / `merchants`；`revenue_stream_attribution` 只按 `project_id` 删，全局种子 `project_id NULL` 不被误删。本席 SELECT 残留：`leftover_media_shares=0`、`leftover_test_merchants=0` |

RED 失败行（无 V428 时）已是：

```
Failing row contains (sb_prj_s11_626213e0_2015-05, media, 20000, 0, 0, 20000, entity, be_media, ...)
violates check constraint "chk_sbfs_format"
at TicketDistributionSettlementService.settleMonth:365 → upsertFormatShare:831
```

即 Java 路径在修前就把 `media`/`entity`/`be_media`/20000 写出来了，唯一阻断是五值 CHECK。这正是定稿「不改结算 Java」的证据：修的是封口，不是发明归属。其余 7 例 RED 时 Failures:0（只有新例 Error）。GREEN 8/8。

跨单元 TDD：该类不在 M021 计划 5 文件内。定稿与派发口径写明合法（先例 M015 改 M017、M020 改 M022）。

### 4.6 席甲技术决定（独立审）

**决定① 编号取 V428。** 本席核：`origin/main` 最大 V426；V427 在两 tree 同 blob 且 `complex_test` flyway `427 | m009 project profiles and associations | t`；`validate-on-migrate: true`、无 out-of-order。取 428 避免与已应用 427 撞号，正确。生产库 `complex` 当前版本本席本轮**未查**（只许 `complex_test` SELECT）；该「385」数字来自本席 B1 票对生产库的 SELECT（`M021-b1-grok-raw.md` V0.02），本轮不复用为新鲜探针，只把它当作已报操作员的环境事实。风险陈述成立：若发布 classpath 只带 V428 不带 V427，日后补 427 会低于已应用的 428 而被拒。**票：接受。** 不构成本 diff 内容 FAIL。

**决定② B3 不在主工作区重生成门禁产物。** 定稿字面含「随新迁移重生成 data-isolation spec / domain-ownership 并跑 verify:predeploy」。本席核：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:59` 仍写「最新迁移 **V423**」；该文件两 tree blob 全等 `7bd106b7…`；`origin/main` 已有 V424–V426。若此刻在主工作区跑生成器，产物 diff 会带上 V424–V427 的表/列漂移，那些行**不能**归 M021-001 → 闸 3 必 FAIL。M018 检查点先例正是在基于 `origin/main` 的独立 worktree 里重生成后再 `verify:predeploy`。把该项留在检查点、不塞进本 B3 diff，是闸 3 下唯一不越界的做法。**票：接受。** 义务转到检查点（已报操作员），不把「本 diff 没有 spec/csv hunk」判成内容 FAIL。

---

## 5. 对抗式自问（grilling 结构，不等人）

1. **Q：定稿要重生门禁产物，本 diff 没有，是否内容 FAIL？**  
   A：否。见 §4.6②。闸 3 的尺子是「归因不到的行即越界」，不是「定稿句子里每个未来动作都必须出现在这一份 tree diff」。重生若现在做，越界的是那些行。

2. **Q：新例会不会只测了 INSERT 没测「不进酒吧池」？**  
   A：否。`available_pool_cent=50000` 对 `ticket_revenue_cent=70000`，且 `itemAmounts` 只有酒吧商户。RED 失败行已含 media 份额金额，说明阻断在 CHECK 而非引擎漏算。

3. **Q：`be_media` 会不会是测试自己种的，而不是 V384 全局种子？**  
   A：否。测试夹具不写 `revenue_stream_attribution` / `business_entities`。本席 SELECT 到 V384 那一行全局种子仍在。`@AfterEach` 按 `project_id` 删 rsa，清不掉 `project_id NULL`。

4. **Q：VARCHAR(16) 以后加更长码会不会炸？**  
   A：本轮闭集最长 9 字符。为假想第七值扩列是铁律 6 禁止的「为极端情况加东西」。不修。

5. **Q：注释里抄 HTML 算不算改业务 HTML？**  
   A：否。三份 HTML blob 两 tree 全等。SQL 注释是迁移说明，不是铁律 4 文件。

6. **Q：GREEN 日志文件只有 31 行、没有 surefire 汇总，能否采信 8/8？**  
   A：该文件本身不完整（截在 Spring 启动 WARN）。8/8 另有 `m021_b3r1_run.log` 的 `GREEN_MVN_EXIT=0` + 工作区 surefire XML `tests="8" time="4.65"`（与 run.log `4.650 s` 同）含新例节点。本席未重跑（会写库）。库实态已在 428 且三 CHECK 含 media。缺口记下，不升 FAIL。

---

## 6. 顺从式核对（假定实现正确）

- 定稿文件名、版本头、DROP IF EXISTS、同名 ADD、六值列表、不用 NOT VALID、不改 Java/HTML/已应用迁移：均命中。
- TDD 类路径、方法名、三断言（media 行 / entity / be_media）+ 池与明细：均命中。
- 清场走既有 `@AfterEach`：命中。
- 002 未动：命中。

---

## 7. 最终票

| 轴 | 票 | 理由 |
|---|---|---|
| 内容 | **PASS** | 前向迁移即定稿最小 DDL；六值与 merchants/leasing_leads/HTML L195/`MERCHANT_BUSINESS_FORMATS` 逐值一致；列宽够；Flyway 编号与文件名合规；已应用迁移零改写；测试走真 `settleMonth` 且锁 V384 种子归属与分池边界；席甲两条技术决定在闸 3 下成立 |
| 归因 | **PASS** | 3 hunk / 45 插入行全部归 M021-001；无第三路径；002 未改 |
| 合取本席 | **PASS** | 任一轴 FAIL 即 FAIL；两轴均 PASS |

非阻断观察（不升 FAIL、不新开 ID）：① GREEN 原始日志文件截断，8/8 靠 run.log + surefire XML 交叉；② 门禁产物重生与 `verify:predeploy` 不在本 diff，必须在基于 origin/main 的检查点 worktree 执行（先例 M018），且发布 classpath 须同时带 V427 与 V428；③ 本席本轮未重跑 8 例 JDBC、未查生产库。

本席未改 findings / ledger / 锁 / HTML / 业务代码。
