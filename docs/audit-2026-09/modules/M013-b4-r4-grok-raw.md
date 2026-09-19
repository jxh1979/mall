CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M013 B4-r4 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r4
席位：固定 Grok 只读独立审计席乙（grok-4.6/xhigh）
计划：160-audit-prompt.md V0.17（本席读 B4 / §5；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M013-b4-r4-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r4 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B2 双 PASS 票替代本次实际 diff 审计。席甲自审票不得覆盖本席；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。B3 累计已记 4（操作员 2026-09-19 批准的追加一次）；本席未因获批或额度降低任何标准。

未读取：任何 `M013-b4-r4-self-raw.md`（目录中该路径不存在；本席未 `read`/`cat`/`grep` 其正文）。可读：V0.17 B4/§5、findings.md V2.32 顶端定稿、旧 B1/B2/B4/B5。正本 `/home/james/complex-management` 分支 `main`。

本批队列（findings V2.32 定稿，闸 3 唯一来源）：M013-019（冲突）/020/021/022/023（细化、工程缺陷）＝是；M013-010 续记否；001–018＝否。当前 23 项：5 是、18 否。B3 本批已完成，累计 4。文档类：可复现命令+输出，不造执行测试；只读源码/HTML 为实现对照，未跑 Java/HTTP/数据库。不增判据、不 LOCK-AMEND、不造新规则/日志/缺口闭合。

---

## 1. 输入、独立锁实算、tree 与 diff 指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
cd /home/james/complex-management
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。文件 889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值（对照，非本席抄写来源）。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值（对照）。`LOCK-AMEND-COUNT: 1`。criteria-lock 版本仍 V0.11。两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（本 diff 未改锁）。工作区 lock 字节与 after 树全等。

| 项 | 值 |
|---|---|
| 模块 | M013 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段（`plan/modules.md` 4 文件；标记 NORMAL） |
| 阶段 | B4-r4 只读审计 B3 第 4 次整批修复的「修复内容」本身（获批追加一次；标准不降） |
| 正式队列「是」（5） | M013-019、020、021、022、023 |
| 正式队列「否」 | M013-001–018；M013-010 续记否 |
| B3 | 已执行 4 次（本轮为获批追加的第 4 次），模块仍 IN_PROGRESS；本席未降标准 |
| before（对象类型 tree） | `4fe06d65137e6fefa5e2da4061af7260cee291be` |
| after（对象类型 tree） | `ddff52dad5669cf17a598b75490df0bcfb454429` |
| `git cat-file -t` / `-s` | 两者均为 `tree`；size 各 954 |
| 完整 diff 命令 | `git diff --binary 4fe06d65137e6fefa5e2da4061af7260cee291be ddff52dad5669cf17a598b75490df0bcfb454429`（无路径过滤） |
| 实际差异 | 34121 bytes；SHA256 `6b97dbc641237b9711116c6e2de3a298db981b1868efeaff3d66b3d6770a95e0`；4 文件；18 hunk；+23 / −18；173 个 LF、0 个 CR、0 个 NUL |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3777 / after 3777；`comm -3` 空（无增删路径） |
| 顶层 tree 对象 | 仅 `docs` 条目哈希变化：`644fecc777e839e01536967843afe25b7b2c2ef3` → `fd70e93b635e7ce4f868d79249e0829c084e31ae`；`.claude` / `apps` / `backend` / 其它顶层条目 before=after |
| HEAD（对照，非本 diff 基线） | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}`=`ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。本席只审派发 before/after，不以 HEAD 或事后提交替换 |
| 真实索引 | `git write-tree` = `a86628fd3f8b13c88aa625589041296f37aec19e` ≠ after、≠ HEAD^{tree}（工作区另有已暂存的 graft/后端等，不在本两树 diff）。四文档 index blob 仍等于 before（绑定 `33aca9a010…`、集团 `78485824…`、验收包 `c5b1f63c…`、PRD `ead26b29…`），≠ after。本轮未把修复写入真实索引，与 B3 临时索引口径一致 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，before=after；工作区字节亦与 after 全等 |
| 锁/sidecar 两树 blob | 相同：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| findings/ledger 两树 blob | 相同（本 diff 未改）：findings `7737145def846bd9ca41e0cbbfe2b27748392bd2`，ledger `dae567718a7f9462e5d6092373cb4916fd1bf681`。队列未在本 diff 提前清 |
| 源码/迁移/OpenAPI | `git diff --name-only` 仅四 md；AccessService / PlatformService / PlatformController / V427 / `docs/openapi/openapi-p0.yaml` before=after |
| 测试面 | 文档类；本席只读对照，未执行 Maven/Java/HTTP/DB |
| 合并状态 | 无 MERGE_HEAD / rebase-merge / rebase-apply / CHERRY_PICK_HEAD。`On branch main` |
| questions/M013* | 无 |
| 添加项 / HTML 申请 | 0 / 0 |
| 当地日 | `date`：2026-09-19 13:27 PDT；`date -u`：2026-09-19 20:27 UTC。变更记录日 2026-09-19 与当地日、UTC 日一致 |

`git diff --stat --binary`：

```
 docs/business-entity-binding-development-design-v0.1.md | 15 ++++++++-------
 docs/group-governance-rebuild-prd-v1.0.md               | 10 ++++++----
 docs/platform-governance-acceptance-pack-v0.2.md        |  5 +++--
 docs/platform-governance-prd-v0.1.md                    | 11 ++++++-----
 4 files changed, 23 insertions(+), 18 deletions(-)
```

`git diff --numstat`：

```
8	7	docs/business-entity-binding-development-design-v0.1.md
6	4	docs/group-governance-rebuild-prd-v1.0.md
3	2	docs/platform-governance-acceptance-pack-v0.2.md
6	5	docs/platform-governance-prd-v0.1.md
```

`git diff --name-status`：四处 `M`，无 A/D/R。

after 树四文件 vs 工作区（`git cat-file -p <after>:<path>` SHA256 + 行数；工作区 `open().read()`）：

| 文件 | 行 | SHA256 | before=after | 工作区=after |
|---|---|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 154 | `9cdec650f49335a42a8769aaea0fbfa0881f15b77994171b0d381538edf001b8` | 否 | 是（14912 bytes） |
| docs/group-governance-rebuild-prd-v1.0.md | 117 | `7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52` | 否 | 是（20430 bytes） |
| docs/platform-governance-acceptance-pack-v0.2.md | 291 | `56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0` | 否 | 是（35710 bytes） |
| docs/platform-governance-prd-v0.1.md | 971 | `e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c` | 否 | 是（146213 bytes） |

git blob：

| 文件 | before blob | after blob |
|---|---|---|
| business-entity-binding-development-design-v0.1.md | `33aca9a010f048cca9a20ec9ed994e456372a911` | `bad5f736e4a4cf2c6d5657753c965b953568e784` |
| group-governance-rebuild-prd-v1.0.md | `784858240ec30f575849794d2e627c3006fa2210` | `d252bdc30d89bc195f80cb8ed6eda835df60de84` |
| platform-governance-acceptance-pack-v0.2.md | `c5b1f63c2b23d69e5e2b0afcdf53bec9114d3939` | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` |
| platform-governance-prd-v0.1.md | `ead26b294687c2c03feb386141958a0a2f7efe31` | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` |

代码/OpenAPI 两树 blob（未动）：

| 路径 | blob（before=after） |
|---|---|
| AccessService.java | `3720af8cf3a6a5d1b114789b8342fda2b790be1d` |
| PlatformService.java | `b325adc09bd66d73505130138889b7a87f9bd211` |
| PlatformController.java | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |
| V427__m009_project_profiles_and_associations.sql | `e485a5d29fea55fa714091485aa26facc8c45160` |
| docs/openapi/openapi-p0.yaml | `1f056b360dc60c37b6c026c761f0fa57b3688e63` |

`plan/modules.md` `^## M[0-9]` 计数 471。findings 工作区版本 V2.32，全文件 SHA256 `3e35c5b5d73b88f6015d5d491e07fd84e907798661d84b320c3b2a780ae76992`（对照台账，本 diff 未改）。本轮只用已冻结判据核「拟改是否落地 / 是否越界 / 是否与 HTML·代码现状一致」，不 LOCK-AMEND。四分类不重开。

### 1.1 两个 tree 对象原文（`git cat-file -p`）

before `4fe06d65137e6fefa5e2da4061af7260cee291be`：

```
040000 tree 08947cd2a6819915194f6eeb00dc393153ec284a	.claude
100644 blob 73ec4e6be98e3aef0818c9980db4bb1c0c3b9e21	.env.example
040000 tree 225f228fff5e6761fe7d9d2d9dec28f0491a0c8e	.github
100644 blob 5534d829c1db85bb6e57fd8fc0079912f38e70d9	.gitignore
040000 tree 83a3c7afa5a500e1981e8e1da99a9b48b2edbc51	.grok
100644 blob 615e581e9799f0b9c7d8047439211fc5f19d3329	.ignore
100644 blob fcedf6ac1da42226414f23167f54e0d62faa0f9d	.mcp.json
100644 blob 76b7a11c30b508648c9ea22153663d6117fa13ad	.semgrepignore
100644 blob d2a264b09936f1522e34480b6940375cf968e7cc	AGENTS.md
100644 blob 45534fc58d54d285e7da057e79f1fb11a8c05924	CONTEXT.md
100644 blob 6bf56366fbe8eef65bcc31d59c9da674017af807	PROGRESS.md
100644 blob 271e4566af87a6e0b31fec4b451e15ac3435cd44	README.en.md
100644 blob a73d207809a20b8addc79157e0dacfa2e61f76c5	README.md
040000 tree eb8d7be2f34741196d5e6546afa02e56904f6d4f	apps
040000 tree 1ebe13e6d9ed813f92724d361fd46cea5432fe95	backend
040000 tree 644fecc777e839e01536967843afe25b7b2c2ef3	docs
040000 tree ad0fa09586cc36a7b0bb0d8dbaee00ba24251907	ops
100644 blob 63ecc802df8de76786dc69101c01abba6bb33d31	package-lock.json
100644 blob 0281eaa9c5d0dd85e055a8c46ae2a66c42028e2d	package.json
100644 blob 0feddca25bb80aef362c0720529c79fe2b0562f7	playwright.config.ts
100644 blob 63f7eb0684fa3620972f13e8f5665b0fd80c3b92	playwright.real-backend.config.ts
040000 tree a9d847ccb9f5cad651d197eea39ebf9ef7d5e225	security-scans
040000 tree 91e5869ed18579dcb04abdce018fe46b4adb82ad	tests
040000 tree cc4d6b27a3abf2b48b94414f3df9a9cd3325b673	tools
100644 blob 60709a9cae77451b7b13c53dd53996a93e7a53e3	tsconfig.base.json
```

after `ddff52dad5669cf17a598b75490df0bcfb454429`：与上逐行相同，唯一差异为 `docs` 树 `644fecc777e839e01536967843afe25b7b2c2ef3` → `fd70e93b635e7ce4f868d79249e0829c084e31ae`。`backend` 树仍 `1ebe13e6d9ed813f92724d361fd46cea5432fe95`。

完整原样 `git diff --binary` 见文末 §9（34121 bytes，SHA256 已核）。

---

## 2. 强制技能预检与按意图执行（铁律 3）

环境实核（列目录 / 读 SKILL.md），缺则记缺口，不冒充已调用。

| 技能 | 存在 | 本轮用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| using-arc | 是 | 路由：用户指令优先于 Arc 全量审计/评审工作流。Arc audit/review 本体要派子代理、写 `docs/arc/audits/`、向人提问 → **不执行本体** |
| graft | 是 | `graft ask`（PlatformService 项目关联）+ `graft grep assignUserPositionDirect` / `SUBJECT_STATUSES` / `business_entity_projects`。docs 不在索引；Java 命中后按指定行 `read_file`。未冒称 graft 覆盖文档 |
| grilling / grill-me | 是（`skills/productivity/grilling`、`grill-me`） | 对抗式自问结构；**不等操作员、不派子代理**。事实自己读 |
| batch-grill-me | **缺口** | 技能目录无此名 |
| ubiquitous-language | **缺口** | 技能目录无此名 |
| requesting-code-review | 是 | B4＝本席即审；**不派** reviewer 子代理、不用 `HEAD~1`、不即刻修。基线=派发 tree id |
| receiving-code-review | 是 | 本席是外审不是收票方；只借「先核实再下票、禁止表演性同意」；**不 IMPLEMENT** |
| verification-before-completion | 是 | PASS 前实跑 sha256/wc/`git diff --binary`/`git cat-file`/`git diff --check`/22 句包含/HTML·代码对照 |
| systematic-debugging | 是 | 只借读错误→追数据流；**不加** diagnostic instrumentation。B4 期间不修 |
| diagnosing-bugs | 是 | 只读追拟改落地与删后句子完整；文档类无红循环命令，不造 harness |
| test-driven-development / tdd | 是 | 文档类**不强制**落测试；未造假测试 |
| codebase-design | 是 | 删除测试：本 diff 无新模块接口、无加深/抽缝 |
| ponytail / ponytail-review | 是 | B3 已完成；本席审多余行。净 +23/−18，无旁路抽象 |
| i-have-adhd | 是 | 编号、复述「模块 M013 / 共 471，B3 第 4 次」；本 raw 承载正文。CLI 按派发不回正文 |
| writing-for-agents | 是 | 本 raw 给席甲/操作员读：完成条件可检查（tree/SHA/22 句/逐行归属） |
| resolving-merge-conflicts | 是 | 核无进行中 merge/rebase；不 resolve |
| git-guardrails-claude-code | 是（可选，`skills/misc/git-guardrails-claude-code`） | 本 harness 未装 Claude Code hook；**未安装 hook**。等价禁力：本席未 fetch/commit/push/merge/deploy/`reset --hard` |
| brainstorming / writing-plans | 是 | 阶段 A 技能，本轮 B4 **不适用** |
| ~~dispatching-parallel-agents / subagent-driven-development / executing-plans / using-git-worktrees / finishing-a-development-branch / research / ponytail-audit~~ | 划掉或禁第三席 | **未调用** |

未用 MCP。未派任何子代理/第三席。未提问。未改 findings/ledger/源码/文档。未新建脚本或临时日志文件。未读取本轮 self-raw。graft 本轮约节省 74739 tokens（3 次 grep：30047+14129+30563）；`graft ask` 曾被管道截断，不计。

---

## 3. 第一性原理三问（2.3）

1. **这段修复为什么必须存在？** 删掉它：集团 PRD / 平台治理 PRD 仍把「仅职级变化」写成不触发，与 PGM L658「HR 定层级/职位，定完系统自动适配」及 L669「职级影响默认权限模板」和 AccessService:1305–1313 改职级走 `assignUserPositionDirect` 相反（019）；集团 PRD 仍写 request_type 四类（020）；验收包仍把「所有对象继续携带 project_id」写成已完成（021）；绑定设计仍停在 V410、不披露 V427 关联与 status 契约裂口（022/023）。上位是已冻结 HTML/实现现状与 GEN-016 当前事实，不是新业务发明。答得上「为何修」。
2. **声称的行为有没有被实际执行验证过？** 文档类不强制跑 Java。证据类型 4：无过滤 `git diff --binary`（34121 bytes / SHA `6b97dbc6…`）、`git cat-file`、工作区 vs after SHA、22 处拟改精确包含、删后句子完整、版本 +0.01、变更记录日 2026-09-19。类型 1 HTML 原文本轮未改（三 HTML blob 不变）。对照只读 AccessService/PlatformService/PlatformController/V427/OpenAPI 行，**未执行**测试，不把测试通过当证据。
3. **哪一条前提没验证就当真？** （a）未跑 JVM/HTTP/DB，本轮也不需要——本批只改四份文档；（b）不以 HEAD 或真实索引当修复结果；（c）不以「已获批追加 / B3=4」放宽闸 3；（d）PRD:966 与集团 PRD:5 历史记录仍含「仅职级/责任范围」字样，按定稿最小边界「历史记录不改」保留，不当成活规则漏删。

不接受的理由未使用：未写「B2 已 PASS 所以 B4 通过」「额度已用满从宽」「获批所以降低标准」「测试绿所以对」。

---

## 4. 逐 ID 修复内容审计（22 处拟改）

定稿执行文本：findings V2.32「本批逐句原句与拟改」1–22。check：每处拟改在 after/工作区恰好 1 次（#15/#19/#20 为行内子串，因原句本就是单元格/句内片段；整行其余未改）。对应原句在活句中为 0（#7 创建主体行、#13/#16/#22 旧变更记录行按「追加」保留）。

### 4.1 M013-019 — 冲突：删仅职级变化/责任范围变化排除（保留任意手工模板应用）

落地（after 行号）：

| # | 文件:行 | 结果 |
|---|---|---|
| 9 | 集团 PRD:1 标题 V1.02→V1.03 | 与 020 合并一次版本 |
| 12 | 集团 PRD:76 | `本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。` 句完整 |
| 13 | 集团 PRD:117 | 2026-09-19 V1.03 变更记录一行，与 020 合并 |
| 17 | 平台治理 PRD:1 标题 V0.05→V0.06 | 页首唯一当前版本 |
| 18 | 平台治理 PRD:407 第 9 条 | 删「仅职级变化、责任范围变化或」，句完整 |
| 19 | 平台治理 PRD:523 验收标准 | 删「仅职级变化不作为本规则触发；」，余 `；敏感权限变更可追溯` 连贯 |
| 20 | 平台治理 PRD:544 | `本规则不扩展到任意手工模板应用；现有手工授权与高危授权审批规则不因本节放宽。` 完整 |
| 21 | 平台治理 PRD:656 14B | `本条不把任意手工模板应用列为触发条件（DEC-R8-91）。` 完整 |
| 22 | 平台治理 PRD:971 | 2026-09-19 变更日志一行 |

对照（只读，未改）：

- PGM L658：「HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板」
- PGM L669：「职级影响默认权限模板和审批层级，但不能替代功能权限点。」
- AccessService.java:1305–1313 注释与实现：已定岗员工改职级走 `assignUserPositionDirect` 按新职级映射重算（M006-G-02，注释引 PGM-1.10.1/1.10.2）

删除后活规则不再把仅职级变化/责任范围变化列为不触发，与 HTML 和改职级重算代码一致；「任意手工模板应用」排除保留。历史记录未改：集团 PRD:5（2026-07-27 修订说明含「责任范围变化」）、PRD:966（2026-08-10 变更日志含「排除仅职级变化」）——与定稿最小边界一致。未改 HTML、代码、决策台账、锁。

### 4.2 M013-020 — 细化：request_type 六类

| # | 文件:行 | 结果 |
|---|---|---|
| 10 | 集团 PRD:23 | 六值 +「DB CHECK 六类（V392）」+ 申请入口只收前三类 + 两类服务内生成 + `role_permission` 未开放 |
| 11 | 集团 PRD:70 | 原三类句后补服务内生成两类（V392）不是申请入口可填类型 |

对照（只读）：AccessService.java:56–57 定义 `user_scope_change` / `position_high_risk_release`；:1580–1586 `buildGrantRequest` switch 只收 `REQUEST_USER_ROLE` / `USER_POSITION` / `TEMPORARY_PERMISSION`，其它 `VALIDATION_FAILED`。补句与现状一致，未发明第七类、未改代码。版本与 019 合并一次 V1.02→V1.03。

### 4.3 M013-021 — 细化：删「所有对象继续携带 project_id」

| # | 文件:行 | 结果 |
|---|---|---|
| 14 | 验收包:1 标题 V0.05→V0.06 | 页首唯一当前版本 |
| 15 | 验收包:181 G7 单元格 | 现为「…承接基本聚合能力 \|」下一列仍是审批属性说明；未另写新要求句 |
| 16 | 验收包:201 §11 | 2026-09-19 一行 |

活句不再把 PGM 项目归属要求写成已完成事实。changelog 行引用被删短语，属记录而非现行断言。未改 HTML/代码。

### 4.4 M013-022 — 细化：V427 项目关联 / API / 依据

| # | 文件:行 | 结果 |
|---|---|---|
| 1–2 | 绑定设计:1 / :3 | L1 与 L3 同为 V0.12，最后更新 2026-09-19；无变更日志段（本文件本来没有，本 diff 也未新造） |
| 3 | 绑定设计:39 | 补 V427 `business_entity_projects` 多对多；「主体表本身仍无 project_id 列」 |
| 6 | 绑定设计:56 | 项目归属由 V427 承载：创建关联当前项目（最高权限可指定已存在项目），列表按当前项目过滤 |
| 7 | 绑定设计:116 | 追加 `POST /api/v1/platform/business-entities/{entity_id}/projects` 一行 |
| 8 | 绑定设计:154 | §8 依据补 V427 |

对照（只读，行号以工作区为准）：

- V427:13–17 `CREATE TABLE business_entity_projects (business_entity_id, project_id, PRIMARY KEY …)`
- PlatformService:54–56 `SUBJECT_STATUSES` 五值（属 023 对照，本 ID 不改 status）
- PlatformService:92 `listBusinessEntitiesPaged(currentProject(), …)`
- PlatformService:121 `creationProject(request.projectId(), true)`；:133 `associateBusinessEntityProject(saved.id(), projectId)`
- PlatformService:363–381 `associateBusinessEntityProject` + `requireProjectConfigurationAuthority`（`requirePlatformScope` + `isMasterAuthority`，文案「持授权密钥的最高权限」）
- PlatformService:419–429 `creationProject`：最高权限可指定已存在项目
- PlatformService:970 注释：「持授权 U 盘密钥的最高权限」
- PlatformController:27 `@RequestMapping("/api/v1/platform")`；:97–104 `@PostMapping("/business-entities/{entity_id}/projects")`

补句与代码/迁移一致，未改代码、未发明新规则、未闭合 OpenAPI 缺口。

### 4.5 M013-023 — 细化：status 五值与 OpenAPI 四值裂口

| # | 文件:行 | 结果 |
|---|---|---|
| 4 | 绑定设计:49 | 服务只接受 pending_admission / active / suspended / terminated / blacklisted（归档走存档端点） |
| 5 | 绑定设计:55 | 同句补 OpenAPI status 四值与服务五值不一致，归契约模块（同 009） |

对照：PlatformService:53–55 注释「归档只经存档端点」+ `SUBJECT_STATUSES` 五值；`docs/openapi/openapi-p0.yaml` BusinessEntityRequest.status enum 仍为 draft / active / suspended / archived（:29231–29235）。OpenAPI 文件 blob 未改，缺口只披露不闭合。与 022 合并一次版本 V0.11→V0.12。

### 4.6 版本、日期、越界、不新造

- 四文件当前标题/L3 均为 +0.01：绑定 V0.11→V0.12（L1=L3）、集团 V1.02→V1.03、验收包 V0.05→V0.06、PRD V0.05→V0.06。
- 变更记录日期均为 2026-09-19。绑定设计无变更日志段，本 diff 未补。
- 未新造业务规则、未新造日志体系、未把 OpenAPI/代码缺口写闭。
- 三 HTML / 锁 / 代码 / OpenAPI / 主索引四文档 blob 未因本批变为 after。
- 001–018、010 续记：本 diff 无对应改动。

---

## 5. §5 闸 3 逐变更行归因

规则：每一 `+`/`−` 内容行必须归到本批「是」的 ID；版本行与变更记录行归该文件本批 ID。空行随变更记录插入归该文件本批 ID。无无 ID 行。

### 5.1 绑定设计（hunk 5；−7/+8）— 本文件本批 ID＝022/023

| 行类 | 摘要 | ID |
|---|---|---|
| − L1 标题 V0.11 | 版本 | 022/023 |
| ＋ L1 标题 V0.12 | 版本 | 022/023 |
| − L3 版本 V0.11 / 2026-09-14 | 版本 | 022/023 |
| ＋ L3 版本 V0.12 / 2026-09-19 | 版本 | 022/023 |
| − §3.1 迁移句止于「只作历史）。 | 022 | 022 |
| ＋ 同句补 V427 关联表 | 022 | 022 |
| − status「尚无 DB CHECK」 | 023 | 023 |
| ＋ status 补服务五值 | 023 | 023 |
| − 实现差距止于 009 活闭集。 | 023（在原 009 句后追加裂口） | 023 |
| ＋ 同句补 OpenAPI status 四值裂口 | 023 | 023 |
| − 「当前表没有 project_id…证照」无关联说明 | 022 | 022 |
| ＋ 同条补 V427 项目归属承载 | 022 | 022 |
| ＋ §5 API 表新增 POST …/projects 一行 | 022 | 022 |
| − §8 依据止于 V410 | 022 | 022 |
| ＋ §8 补 V427 | 022 | 022 |

### 5.2 集团 PRD（hunk 4；−4/+6）— 本文件本批 ID＝019/020

| 行类 | 摘要 | ID |
|---|---|---|
| − 标题 V1.02 | 版本 | 019/020 |
| ＋ 标题 V1.03 | 版本 | 019/020 |
| − request_type 四值 / 预留 4 类 | 020 | 020 |
| ＋ request_type 六值 / V392 | 020 | 020 |
| − 申请类型三类句（无服务内两类） | 020 | 020 |
| ＋ 同句补 user_scope_change / position_high_risk_release | 020 | 020 |
| − 「仅职级变化、责任范围变化或任意手工模板应用」 | 019 | 019 |
| ＋ 「任意手工模板应用」 | 019 | 019 |
| ＋ （空行，插在旧变更记录与新变更记录之间） | 变更记录 | 019/020 |
| ＋ 变更记录 2026-09-19 V1.03 M013-019/020 | 变更记录 | 019/020 |

### 5.3 验收包（hunk 3；−2/+3）— 本文件本批 ID＝021

| 行类 | 摘要 | ID |
|---|---|---|
| − 标题 V0.05 | 版本 | 021 |
| ＋ 标题 V0.06 | 版本 | 021 |
| − G7「…承接基本聚合能力，所有对象继续携带 project_id」 | 021 | 021 |
| ＋ G7「…承接基本聚合能力」 | 021 | 021 |
| ＋ §11 2026-09-19 一行 | 变更记录 | 021 |

### 5.4 平台治理 PRD（hunk 6；−5/+6）— 本文件本批 ID＝019

| 行类 | 摘要 | ID |
|---|---|---|
| − 标题 V0.05 | 版本 | 019 |
| ＋ 标题 V0.06 | 版本 | 019 |
| − §6.10 第 9 条含仅职级/责任范围排除 | 019 | 019 |
| ＋ 第 9 条只保留任意手工模板 | 019 | 019 |
| − 验收标准含「仅职级变化不作为本规则触发」 | 019 | 019 |
| ＋ 验收标准删该分句 | 019 | 019 |
| − 「不扩展到仅职级变化，也不扩展到任意手工模板应用」 | 019 | 019 |
| ＋ 「不扩展到任意手工模板应用」 | 019 | 019 |
| − 14B「仅职级变化或任意手工模板应用」 | 019 | 019 |
| ＋ 14B 只保留任意手工模板 | 019 | 019 |
| ＋ 变更日志 2026-09-19 一行 | 变更记录 | 019 |

合计变更内容行 −18/+23＝41，全部有 ID。无过滤 diff 仅此四路径。归因结论：PASS。

---

## 6. 对抗式（grilling 结构，自问自答）

1. **获批追加是否降低闸 3？** 否。仍要求 22 处逐字、无无 ID 行、未动 HTML/代码。本席未因 B3=4 放行任何越界。
2. **是否顺手改了绑定设计之外的 Java/OpenAPI 来「闭合」023？** 否。OpenAPI blob 未变；023 只披露裂口。
3. **022 是否把「U 盘密钥」写成新规则？** 否。PlatformService:970 注释已是「持授权 U 盘密钥的最高权限」；:377–381 关联端点要求平台范围+最高权限；:419–429 创建时可指定已存在项目。与拟改逐字一致。
4. **019 删完是否留下残句？** 否。四条活句语法完整；「任意手工模板应用」保留。
5. **活规则是否还排除仅职级变化？** 四文件活句 0 处。仅历史记录（集团:5、PRD:966）与新变更记录叙述（集团:117、PRD:971、验收包:201 引用被删短语）仍出现，属定稿不改历史 / 记录本次删除。
6. **验收包 G7 删完单元格是否裂开？** 否。仍是五列，末列审批属性句未动。
7. **是否新增变更日志段到绑定设计？** 否。该文件无「变更记录/变更日志」命中。
8. **版本是否跳号或 L1≠L3？** 否。皆 +0.01；绑定 L1=L3=V0.12。
9. **真实索引里大量 staged 后端是否算本 diff？** 否。两树 `backend` 哈希相同；本 `git diff --name-only` 仅四 md。那些 staged 条目不在 before/after 差异内。
10. **是否把 PGM 项目归属要求改写成新完成声明？** 021 只删虚假已完成句，未补新要求。PRD:15 条「所有对象必须带综合体项目归属」未改（不在 22 处内，也不在 diff）。

未发现越界、未发现无 ID 行、未发现新造规则。

---

## 7. 顺从式（本批「是」ID × 落地）

| ID | 拟改处数 | 落地 | 越界 | 与对照一致 |
|---|---|---|---|---|
| 019 | 9（含与 020 共用版本/变更记录） | 是 | 无 | PGM L658/L669、AccessService:1305–1313 |
| 020 | 2 内容 + 共用版本/变更记录 | 是 | 无 | AccessService:56–57、:1580–1586 |
| 021 | 3 | 是 | 无 | 绑定设计本就无表级 project_id；V427 为关联表 |
| 022 | 5 内容 + 共用版本 | 是 | 无 | V427、PlatformService:92/121/363–381/419–429、Controller:97 |
| 023 | 2 内容 + 共用版本 | 是 | 无 | PlatformService:54–56；OpenAPI status 四值仍在 |

001–018、010：本 diff 无行可归，符合「否」不动。

---

## 8. 统一判定

- 修复内容：PASS（22 处拟改逐字落地；删后句子完整；版本 +0.01 且页首/标题一致；变更记录 2026-09-19；不新造规则/日志/缺口闭合；三 HTML/锁/代码/OpenAPI 未动；主索引四文档仍为 before blob）。
- 范围归因：PASS（18 hunk、+23/−18、仅四文档；每一变更行归 019–023；无无 ID 行）。
- 席乙 B4-r4 总结论：PASS。
- 本票不是双 PASS，不是 B5，不是模块收口。

---

## 9. 完整原样 diff（无过滤 `git diff --binary`）

命令：`git diff --binary 4fe06d65137e6fefa5e2da4061af7260cee291be ddff52dad5669cf17a598b75490df0bcfb454429`

指纹：34121 bytes；SHA256 `6b97dbc641237b9711116c6e2de3a298db981b1868efeaff3d66b3d6770a95e0`；18 hunk；+23/−18。下文为该命令 stdout 原样（二进制安全；本 diff 无 NUL）。

```diff
diff --git a/docs/business-entity-binding-development-design-v0.1.md b/docs/business-entity-binding-development-design-v0.1.md
index 33aca9a0..bad5f736 100644
--- a/docs/business-entity-binding-development-design-v0.1.md
+++ b/docs/business-entity-binding-development-design-v0.1.md
@@ -1,6 +1,6 @@
-# 四类主体唯一绑定开发设计 V0.11
+# 四类主体唯一绑定开发设计 V0.12
 
-> 版本：V0.11 ｜ 最后更新：2026-09-14 ｜ 状态：现行开发设计
+> 版本：V0.12 ｜ 最后更新：2026-09-19 ｜ 状态：现行开发设计
 > 业务最终解释权：`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。代码、迁移和接口只说明当前实现，不得覆盖三份 HTML。
 
 文档类型：Implementation  
@@ -36,7 +36,7 @@
 
 ### 3.1 `business_entities`
 
-当前迁移：V005 基表 + V354 `archive_reason` 与 `entity_role` CHECK + V410 删除 `parent_entity_id` 并把 `entity_role` 白名单收窄为 `platform` / `project_company` / `management_company`（V318 的父链列已由 V410 删除，只作历史）。
+当前迁移：V005 基表 + V354 `archive_reason` 与 `entity_role` CHECK + V410 删除 `parent_entity_id` 并把 `entity_role` 白名单收窄为 `platform` / `project_company` / `management_company`（V318 的父链列已由 V410 删除，只作历史）；V427 另建 `business_entity_projects` 多对多项目关联表，主体表本身仍无 `project_id` 列。
 
 | 当前字段 | 类型/约束 | 用途 |
 |---|---|---|
@@ -46,14 +46,14 @@
 | `unified_social_credit_code` | `VARCHAR(128)` | 统一社会信用代码 |
 | `legal_representative` | `VARCHAR(255)` | 法定代表人 |
 | `bank_account_ref` | `TEXT` | 银行账户引用 |
-| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK |
+| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`（归档走存档端点） |
 | `archive_reason` | `TEXT` | 存档/下线理由 |
 | `created_at/updated_at` | `TIMESTAMPTZ` | 时间留痕 |
 
 实现差距：
 
-- V318 曾把 `parent_entity_id` 用作固定层级、V354 的角色白名单曾含 HTML 没有的代码值；V410 已删除该列并把白名单收为 HTML 三角色，代码按 HTML 的多对多协作关系收敛，业务读取不得据历史列推导固定行政隶属链。OpenAPI `BusinessEntityRequest.entity_role` 枚举仍是旧六值（operation / contract / settlement / invoice / payment / other），与服务白名单不一致，属契约缺口（M013-009，归 OpenAPI 契约模块处理），不得当作活闭集。
-- 当前表没有 `project_id`、主体联系电话、证照附件或证照有效期字段。证照、资质和保险由合规到期台账承载，不得在本文虚构这些现行列。
+- V318 曾把 `parent_entity_id` 用作固定层级、V354 的角色白名单曾含 HTML 没有的代码值；V410 已删除该列并把白名单收为 HTML 三角色，代码按 HTML 的多对多协作关系收敛，业务读取不得据历史列推导固定行政隶属链。OpenAPI `BusinessEntityRequest.entity_role` 枚举仍是旧六值（operation / contract / settlement / invoice / payment / other），与服务白名单不一致，属契约缺口（M013-009，归 OpenAPI 契约模块处理），不得当作活闭集；同一 schema 的 `status` 枚举仍是 draft / active / suspended / archived，与服务可设五值不一致，同属契约缺口（M013-023，归 OpenAPI 契约模块处理）。
+- 当前表没有 `project_id`、主体联系电话、证照附件或证照有效期字段。项目归属由 V427 `business_entity_projects` 多对多关联承载：创建主体时关联当前项目（持授权 U 盘密钥的最高权限可指定已存在项目），列表按当前项目过滤。证照、资质和保险由合规到期台账承载，不得在本文虚构这些现行列。
 - HTML 的商户/运营主体业务状态为待准入、正常、暂停、终止、归档、黑名单；试运营是商户自己的经营状态，系统不记录。暂停与列入黑名单由招商部门与运营部门决定。退出不立即生效，须进入按项目可配的缓冲期（上下限出厂为 30 至 60 天，出厂默认 30 天）；缓冲期内向该商户客人说明情况，并给出转到其他酒吧、客人自行取走或退款三种处理方式。当前 `status` 未实现该完整词表和缓冲期闭环。
 
 ### 3.2 `business_entity_bindings`
@@ -113,6 +113,7 @@
 |---|---|---|
 | `/api/v1/platform/business-entities` | GET | 真分页查询主体档案 |
 | `/api/v1/platform/business-entities` | POST | 创建主体档案 |
+| `/api/v1/platform/business-entities/{entity_id}/projects` | POST | 为主体追加项目关联（须平台范围且由持授权 U 盘密钥的最高权限执行） |
 | `/api/v1/platform/business-entity-bindings` | GET | 真分页查询四类主体绑定 |
 | `/api/v1/platform/business-entity-bindings` | POST | 未来 `effective_from` 写 `inactive` 且不提前同步商户三主体；已到日期写 `active` 并同步。已有 `active` 或 `inactive` 且 `effective_to` 为空的绑定返回 `ACTIVE_BINDING_EXISTS`；替换商户既有三主体返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` |
 | `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 接受 active、inactive、suspended 并必填原因；已结束期间返回 `BINDING_PERIOD_ENDED`，生效中绑定停用/暂停返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`，未到生效日返回 `BINDING_NOT_EFFECTIVE`。启用还检查商户、三主体、未完成绑定槽位及既有财税主体不被替换 |
@@ -150,4 +151,4 @@
 - PGM「酒吧股权、牌照、场地和资金必须分别记录」
 - BSM「配置数据可以存档、也可以删除（删除要审批）」
 - ENUM「四类主体」「经营主体关系」「商户/运营主体状态」
-- 当前实现：V002、V005、V010、V318、V354、V410（删父链、角色三值）；`PlatformController`、`PlatformService`、`JdbcPlatformRepository`
+- 当前实现：V002、V005、V010、V318、V354、V410（删父链、角色三值）、V427（主体项目关联）；`PlatformController`、`PlatformService`、`JdbcPlatformRepository`
diff --git a/docs/group-governance-rebuild-prd-v1.0.md b/docs/group-governance-rebuild-prd-v1.0.md
index 78485824..d252bdc3 100644
--- a/docs/group-governance-rebuild-prd-v1.0.md
+++ b/docs/group-governance-rebuild-prd-v1.0.md
@@ -1,4 +1,4 @@
-# 集团治理重建 PRD（V1.02·补断点）
+# 集团治理重建 PRD（V1.03·补断点）
 
 > **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。
 
@@ -20,7 +20,7 @@
 | 对象 | 枚举/字段 | 取值（当前实现 CHECK / 服务校验） | 业务说明 |
 |---|---|---|---|
 | 职位角色模板 `position_role_templates` | `status` | `active` / `inactive` | 活跃模板参与定岗匹配；同组织、职位、职级仅允许一个活跃模板 |
-| 授权申请 `access_grant_requests` | `request_type` | `user_role` / `user_position` / `temporary_permission` / `role_permission` | DB CHECK 预留 4 类；当前 `AccessService` 实际支持前三类，`role_permission` 服务层未开放 |
+| 授权申请 `access_grant_requests` | `request_type` | `user_role` / `user_position` / `temporary_permission` / `role_permission` / `user_scope_change` / `position_high_risk_release` | DB CHECK 六类（V392）；申请入口只接受前三类，`user_scope_change`、`position_high_risk_release` 由跨范围定岗与岗位高危挂起在服务内生成，`role_permission` 服务层未开放 |
 | 授权申请 `access_grant_requests` | `status` | `pending` / `applied` / `rejected` | 待审批 / 已应用 / 已驳回 |
 | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 实现字段；五类 HTML 强制高危权限必须为 `true`，后台只能新增高危项或提高审批等级，不能关闭强制项 |
 | 权限定义 `permissions` | `risk_level` | V394 已加 CHECK（low / medium / high / critical）；服务按 `critical` / `high` / 其他分档 | `critical` → l3 / 最高审批；`high` → l2 / 上两级；其他 → l1 / 上一级 |
@@ -67,13 +67,13 @@
   - 高危业务：如 `bar.product.sold_out`、`bar.bill.reopen`、`bar.order_item.void`、`bar.saved_bottle.manage`、`ticket.verify.reverse`、`amusement.verification.override`、`amusement.over_capacity_policy.configure`。
   - **第 5 类 税费规则与税费调整（2026-08-04 对齐平台治理地图/枚举速查表）**：所有税费规则和税费调整均须经过**财务审核、税务审核、法务审核**，再由**顶层平台公司的老板最终确认后生效**；**任一环节驳回均不得生效**；**提交人不得审批自己提交的调整**；全部审批意见和时间必须留痕。**税率、税目不属于普通后台配置项**，不得按普通配置项直接改动。
     - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档权限码 `staff.tax_bracket.manage`（V217，`risk_level=high`、`requires_approval=true`；属薪资个税档，不是本条第 5 类「税费规则与税费调整」）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 复核动作（策略 tax_rule_adjustment_l3，不是 `permissions` 行），但本条 HTML「税费规则与税费调整」在 `permissions` 侧尚无 `finance.tax_*` 权限码落点，**记为实现待对齐（范围收窄为该权限码）**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
-- 申请类型当前服务支持三类：`user_role`、`user_position`、`temporary_permission`。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支。
+- 申请类型当前服务支持三类：`user_role`、`user_position`、`temporary_permission`。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支；另有服务内生成的 `user_scope_change`、`position_high_risk_release` 两类（V392），不是申请入口可填类型。
 - 申请时服务先解析目标权限码。角色/岗位授权申请若不含 `requires_approval=true` 的权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权；临时权限申请无论日常或高危均进入主管审批链，其中仅含日常权限的申请使用当前一级主管策略，不直接授出。
 - 审批链不由发起人选择档位，也不按风险值自动映射成可选档或指定角色。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现把 `critical`/`high`/其他 写成 `permission_grant_l3`/`l2`/`l1` 与 `top_authority`/`two_levels_up`/`one_level_up`，属实现证据，不是业务可选档。
 - 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。
 - 【实现证据/不取得效力】流程：申请创建 `access_grant_requests(status=pending)` 并调用 `SupervisorReviewService.blockForAdmission` 生成 `review_id`；审批端调用 SupervisorReview `approve` 后，批准则应用角色/定岗/临时权限并写 `applied`，驳回写 `rejected`——三份业务 HTML 零命中上述表名、状态流程与服务名，不得立法。
 - 临时权限必须有 `expires_at`；可带 `valid_from`、`single_use`。V170 扩展 `temporary_permission_grants` 的 `status`、`valid_from`、撤销、单次消耗字段；有效权限查询按有效期和消耗状态过滤。
-- 角色装配按治理 HTML「角色装配（已确认）」两支执行。同商户同项目：保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。
+- 角色装配按治理 HTML「角色装配（已确认）」两支执行。同商户同项目：保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。
 - 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。统计窗口和频繁阈值从后台授权政策读取，查询不得覆盖；兼容的 `window_days` 仅在等于后台值时接受，两个阈值参数只要传入即返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。
 
 **关键端点（实际 Controller）**
@@ -113,3 +113,5 @@
 > 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` CHECK 与 §4 `analytics.group.view` 审批属性按 V394 更新为已闭合；§2 关键端点补 lifecycle 并标注 DELETE 恒拒绝；§3 税费权限码登记句按 V217 / SupervisorReview 现状改写。
 
 > 变更记录（2026-09-14，V1.02，M013-012–016）：集团项目隔离现状与审批属性闭合分开；岗位模板普通权限与待批高危、日常临时审批、申请权限风险档、总结查询参数按现有实现订正。
+
+> 变更记录（2026-09-19，V1.03，M013-019/020）：删去角色装配重算规则中与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的职级与责任范围排除；授权申请类型按 V392 六类订正。
diff --git a/docs/platform-governance-acceptance-pack-v0.2.md b/docs/platform-governance-acceptance-pack-v0.2.md
index c5b1f63c..cb89c73a 100644
--- a/docs/platform-governance-acceptance-pack-v0.2.md
+++ b/docs/platform-governance-acceptance-pack-v0.2.md
@@ -1,4 +1,4 @@
-# 平台治理验收包 V0.05
+# 平台治理验收包 V0.06
 
 > 合并说明：本文按 2026-06-10 全量文档收口与合并归档审计执行，用于替代下列分散源文件。源文件已移入 `_archive/20260610-retention-audit/merged-layer-spec-sources/`，后续开发、审计和交接以本文为入口。
 
@@ -178,7 +178,7 @@
 | G4 | 空间/铺位建模 | 部分 | 空间对象须按 GOV 既有口径收敛 | P0 按楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位建模；铺位按 HTML 已定义口径管理，不另立区块层级 |
 | G5 | 合同审批边界 | 已收敛 | 原"P0 仅单级确认、多级审批 P1"与 GOV 多级模型冲突 | 合同新建、到期终止、变更三类各走各自的审批流程模板、模板分别可配（业务参数，按综合体可配，须同时配置上下限），各类阶段数与审批岗位可以不同；每类均按统一审批模型执行：草稿提交时指定这条主线的最终审批人（默认总经理），系统按组织层级逐级自动补全；业务、财务、法务为平行的相关部门线，各指定该部门最终审批人并补全至该部门预先设定的最低起点层级；主线与各条平行线全部通过合同才生效，任一节点驳回则整单退回起草。三类均不得录入即生效：新建未审批不生效、到期不得无审批自动终止、变更未审批不生效。产品细化见 PRD §12 已确认第 18 条 |
 | G6 | 高级审批级别阶梯 | 已补 | 原把"指定角色"与 level_rank 计算写成已建模缺口 | 收口为 GOV 三档：上一级、上两级、最高权限。最高权限是持有授权 U 盘密钥的那一个人，不按职级档位解析；不设指定角色档 |
-| G7 | 集团视图与跨项目汇总 | 已承接；审批属性已闭合，项目范围隔离待对齐 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接基本聚合能力，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`；项目级调用者的汇总结果尚未按本项目过滤（集团治理 PRD §4），不得把审批属性闭合当作隔离已完成 |
+| G7 | 集团视图与跨项目汇总 | 已承接；审批属性已闭合，项目范围隔离待对齐 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接基本聚合能力 | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`；项目级调用者的汇总结果尚未按本项目过滤（集团治理 PRD §4），不得把审批属性闭合当作隔离已完成 |
 
 > 待批准事项：员工数据处理与监控告知的同意凭据尚未写入三份 HTML，不作为本验收包必过项，也不得升为必填字段或注册流程，不另造替换 consent 字段。
 
@@ -198,6 +198,7 @@
 |---|---|---|---|---|
 | 2026-09-11 | 改 | §10A G7 / §10 | 版本升 V0.04；集团视图审批属性缺口按 V394 改为已闭合 | 160 全量审计 M013-002 |
 | 2026-09-14 | 改 | §10A G7 | 版本升 V0.05；区分集团审批属性已闭合与项目范围隔离待对齐 | M013-012；集团治理 PRD §4 与当前集团总览调用链 |
+| 2026-09-19 | 改 | §10A G7 | 版本升 V0.06；删去与主体档案、四类主体绑定表实况不符的「所有对象继续携带 project_id」 | M013-021；四类主体绑定开发设计 §3 |
 | 2026-09-01 | 增/改 | §2 / §2A / §10A G7 | 版本升 V0.03；补金额精度、合同本地 AI、合同当地时间和外部经销商全链验收；集团视图统一为已承接并保留实现缺口 | M01 全量复审 ADV-01/02/04/05/08；三份业务 HTML 现文 |
 | 2026-06-07 | 改 | §6 终端开关页验收 | 终端枚举改为 10 端，对齐字段字典 §3.1 | D5、govfam-3；本轮已再对齐十一端 |
 | 2026-06-07 | 改 | §7 终端开关状态 | 去掉无支撑的"定时启用/定时停用"独立态，改为派生过渡态 | govfam-4 |
diff --git a/docs/platform-governance-prd-v0.1.md b/docs/platform-governance-prd-v0.1.md
index ead26b29..1b21047e 100644
--- a/docs/platform-governance-prd-v0.1.md
+++ b/docs/platform-governance-prd-v0.1.md
@@ -1,4 +1,4 @@
-# 平台治理层产品需求 PRD V0.05
+# 平台治理层产品需求 PRD V0.06
 
 > 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。**三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决**（2026-08-05 R7 补，依据 PGM「解释权声明」条 / DIFF-A-051）。
 
@@ -404,7 +404,7 @@
 6. 将角色、基础模板或临时模板分配给员工或商户管理员。
 7. 权限变更、收回、到期、单次消耗、失效均进入留痕。
 8. 员工职级、岗位、角色和薪资口径可建立关联；职级可由后台维护为 M1-M10，并可分上、中、下。P0 记录职级/岗位/权限摘要和薪资大范围，具体薪资单独填写，完整薪资计算只留接口和摘要字段。
-9. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效，审批通过后方可生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。
+9. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效，审批通过后方可生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。
 
 输出结果：明确“谁能操作、谁能查看”。
 
@@ -520,7 +520,7 @@
 | 使用角色 | 系统管理员、平台运营负责人、商户管理员、现场主管 |
 | 关键字段 | 角色名称、角色类型、适用组织、职级、功能权限、模板名称、模板类型（基础/临时）、【已废止/不得回灌】复制来源模板、模板适用范围、生效时间、失效时间、是否单次有效、授权原因、薪资口径摘要、状态；**无数据范围字段**——数据可见边界由项目与商户两维硬隔离决定，不落配置字段 |
 | 状态 | 草稿、已启用、暂停、已停用、归档 |
-| 验收标准 | 平台角色和商户角色可区分；不同职级、角色和模板可汇总与下钻的层级不同，**数据可见边界本身由项目与商户两维隔离决定、后台不提供设置项**；显式拒绝优先于所有允许项；【已废止/不得回灌】模板可复制并独立维护；临时模板到期或单次消耗后立即失效；保存岗位、职责或同一商户、同一项目范围内的所属组织变更后立即按新岗位模板重算并清除旧范围派生权限，同一商户、同一项目范围内的普通模板权限随保存立即生效；保存所属商户或发生跨项目变更时，新范围权限每次均须先审批、审批通过后方可生效；超模板增权保持未生效直至审批通过；仅职级变化不作为本规则触发；敏感权限变更可追溯 |
+| 验收标准 | 平台角色和商户角色可区分；不同职级、角色和模板可汇总与下钻的层级不同，**数据可见边界本身由项目与商户两维隔离决定、后台不提供设置项**；显式拒绝优先于所有允许项；【已废止/不得回灌】模板可复制并独立维护；临时模板到期或单次消耗后立即失效；保存岗位、职责或同一商户、同一项目范围内的所属组织变更后立即按新岗位模板重算并清除旧范围派生权限，同一商户、同一项目范围内的普通模板权限随保存立即生效；保存所属商户或发生跨项目变更时，新范围权限每次均须先审批、审批通过后方可生效；超模板增权保持未生效直至审批通过；敏感权限变更可追溯 |
 
 > **数据隔离不提供关闭开关（2026-08-07 补，依据 PGM「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」条；R10-U-02）**：**数据的项目与商户隔离不提供关闭开关，与导出脱敏同例**。终端开关、模块开关等任何层级的开关都不得用于关闭项目与商户隔离；**只有项目之上的获授权岗位才可以跨项目汇总**，一个岗位能查看本项目普通业务数据，不代表能查看其他项目或敏感经营数据。
 
@@ -541,7 +541,7 @@
 - 保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效；同一商户、同一项目范围内的普通模板权限随保存立即生效。
 - 保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。
 - 超出新模板范围的增权保持未生效，审批通过后方可生效。
-- 本规则不扩展到仅职级变化，也不扩展到任意手工模板应用；现有手工授权与高危授权审批规则不因本节放宽。
+- 本规则不扩展到任意手工模板应用；现有手工授权与高危授权审批规则不因本节放宽。
 
 ### 7.9 平台基础留痕
 
@@ -653,7 +653,7 @@
 14. 员工最终权限按“角色权限 + 基础模板授权 + 有效临时模板授权 + 员工单独授予 - 员工/模板显式拒绝”计算，显式拒绝优先级最高。
 > **（2026-07-20 安全修复 AUD-011，补记于第 14 条）权限模板应用不是高危授权审批的例外**：`applyPermissionTemplate` 现与角色/岗位/临时授权走同一政策引擎 `requireGrantApprovalIfNeeded`——模板含任一 `requires_approval` 权限时，经模板入口批量应用一律 fail-closed 拒绝（`GRANT_REQUIRES_APPROVAL` 403），不得绕过职责分离/主管复核。高风险授权须改走 `applyGrant → reviewGrant` 主管复核流（该流按权限快照建待审请求、批准后再原子应用）；模板入口对高风险直接拒绝、无待审窗口，故审批期间模板内容变化亦无从偷换。
 14A. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、**税费规则与税费调整**（共“五类”）——上述权限授出前必须先审批才生效；配置只允许新增高危项或提高审批等级，不得关闭这些强制闸门。授权申请人与审批人必须职责分离，申请人不能审批自己的授权申请。**持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不得审批自己的单据。税费规则与税费调整的审批链固定为：财务审核 → 税务审核 → 法务审核 → 顶层平台公司老板最终确认后生效**；任一环节驳回均不得生效，提交人不得审批自己提交的调整，全部审批意见和时间必须留痕；税率、税目不属于普通后台配置项。（依据 PGM「高危授权审批（已确认）」条、ENUM「税费配置」条；与 §7.8.1 及 `group-governance-rebuild-prd-v1.0.md` §3 口径一致）
-14B. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更后必须立即按变更后的岗位权限模板重算有效权限并使旧岗位或旧范围派生权限失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效直至审批通过。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。本条不把仅职级变化或任意手工模板应用列为触发条件（DEC-R8-91）。
+14B. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更后必须立即按变更后的岗位权限模板重算有效权限并使旧岗位或旧范围派生权限失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效直至审批通过。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。本条不把任意手工模板应用列为触发条件（DEC-R8-91）。
 15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**基本聚合能力不再是本 PRD 的未来待办**；上述闭环仅指审批属性，项目级调用者的汇总结果尚未按本项目过滤，仍须按集团治理 PRD §4 对齐项目隔离。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
 16. 所有端都必须有开关接口，支持按全局、综合体、商户、业务系统、模块、版本/设备、灰度控制，高层级关闭压倒低层级打开；关闭后该端或模块不可用，打开后仍受权限、数据范围和设备状态控制；与钱有关的规则（分账比例、结算口径、综合服务费、租金与抽成规则）一律不得使用灰度、不得按商户或设备部分开放，只能按综合体整体统一切换，切换日期由后台配置（业务参数，按综合体统一可配，须同时配置上下限），切换前后各自适用完整的一套规则；灰度只用于不影响金额计算的功能开放。同一套规则之下的参数取值按三份业务 HTML 为该参数写明的粒度配置，不属于灰度或部分开放。开关缺配置行一律默认关闭（opt-in），不得由代码或部署脚本视同打开；开关服务、支付通路、核销与入场等营业必需的能力不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败按兜底关闭处理、写操作一律拒绝，只有留痕不受兜底关闭影响。终端类型为受控十一类（客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户），产品细化见 `platform-governance-product-spec-v0.2.md`「字段字典」部分 §3.1「终端开关字段」，最终业务边界仍以三份业务 HTML 为准。
 17. 所有有过程的业务动作必须留痕，订单获取、销售归属、卡座服务、异常第一处理人，以及酒水和可追责物品从库房到客人的交接过程必须能追溯责任人、设备、地点、时间和结果。已拍板例外：安检环节不产生按人记录，留痕与溯源以设备侧记录为终点。
@@ -968,3 +968,4 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责
 | 2026-08-29 | 改 | §6.3 / §7.2 / §7.3 | 业态收回 ENUM 六值+单一归属，废止维护/绑定开关/复合业态；废止空间「是否可留痕」开关 | M01 R-007 / R-008 |
 | 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风险功能列表「考勤打卡」改「查看考勤」（治理地图 V0.12 L741、业务地图 V0.14 L930/L1865/L1445）；§6.2.1 改为字典硬校验口径并补 `parking`；自营标注改为合作方式 `self_operated` 既有字段；准入闸口现状改为员工域已接线、四域未接；GAP-20 按 V394 闭合 | 160 全量审计 M013-002/003/004/007/008；PGM L741、BSM L930/L1865/L1445 现文 |
 | 2026-09-14 | 改 | §12 已确认第15条 | 版本升 V0.05；集团基本聚合与审批属性已具备，项目级结果过滤仍待对齐 | M013-012；PGM 权限规则及集团治理 PRD §4 |
+| 2026-09-19 | 改 | §6.10 第 9 条 / §7.8 / §7.8.2 / §12 第 14B 条 | 版本升 V0.06；删去与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的仅职级变化、责任范围变化排除，只保留任意手工模板应用不触发 | M013-019；PGM L658/L669；AccessService 改职级重算现状 |
```
