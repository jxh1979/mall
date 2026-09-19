CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M013 B4-r5 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r5
席位：固定 Grok 只读独立审计席乙（grok-4.6/xhigh）
计划：160-audit-prompt.md V0.17（本席读 B4 / §5；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M013-b4-r5-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r5 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B2 双 PASS 票替代本次实际 diff 审计。席甲自审票不得覆盖本席；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。B3 累计已记 5（操作员 2026-09-19 第二次批准的追加一次）；本席未因获批或额度降低任何标准。

未读取：任何 `M013-b4-r5-self-raw.md` 正文（`ls` 见该路径存在、5351 bytes；本席未 `read`/`cat`/`grep` 其内容）。可读：V0.17 B4/§5、findings.md V2.36 顶端定稿、旧 B1/B2/B4/B5。正本 `/home/james/complex-management` 分支 `main`。

本批队列（findings V2.36 定稿，闸 3 唯一来源）：仅 M013-024（细化、工程缺陷）＝是；M013-001–023＝否。当前 24 项：1 是、23 否。B3 本批已完成，累计 5。定稿候选段「本批逐句原句与拟改」共 3 处即 B3 执行文本。文档类：可复现命令+输出，不造执行测试；只读源码/HTML 为实现对照，未跑 Java/HTTP/数据库。不增判据、不 LOCK-AMEND、不造新规则/日志/缺口闭合。本文件无变更日志段，不新造。

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
| 阶段 | B4-r5 只读审计 B3 第 5 次整批修复的「修复内容」本身（操作员 2026-09-19 第二次批准的追加一次；标准不降） |
| 正式队列「是」（1） | M013-024 |
| 正式队列「否」 | M013-001–023 |
| B3 | 已执行 5 次（本轮为第二次获批追加），模块仍 IN_PROGRESS；本席未降标准 |
| before（对象类型 tree） | `2a13fce88ca2b0893863bf13a2b6dca7433a0319` |
| after（对象类型 tree） | `2720a4e02b917ab6e88b5ba20d4912d59c007fae` |
| `git cat-file -t` / `-s` | 两者均为 `tree`；size 各 987 |
| 完整 diff 命令 | `git diff --binary 2a13fce88ca2b0893863bf13a2b6dca7433a0319 2720a4e02b917ab6e88b5ba20d4912d59c007fae`（无路径过滤） |
| 实际差异 | 1517 bytes；SHA256 `824d0994ba499072dc33fb65dd6dab3f60fa86b67388d440f2524e0ebc203963`；1 文件；2 hunk；+3 / −3；22 个 LF、0 个 CR、0 个 NUL |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3782 / after 3782；`comm -3` 空（无增删路径）；递归 blob 比较仅 1 路径哈希变化 |
| 顶层 tree 对象 | 仅 `docs` 条目哈希变化：`a21c57ded7bf04a7d44c98172bdd65d58d2a9345` → `fff1bdd32bdcfe8f18e53c617e6273d3e7ed6ece`；`.claude` / `apps` / `backend` / `index` / 其它顶层条目 before=after |
| HEAD（对照，非本 diff 基线） | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}`=`ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。本席只审派发 before/after，不以 HEAD 或事后提交替换 |
| 真实索引 | `git write-tree` = `a86628fd3f8b13c88aa625589041296f37aec19e` ≠ after、≠ HEAD^{tree}（工作区另有已暂存的 graft/后端等，不在本两树 diff）。绑定设计 index blob 仍 `33aca9a010f048cca9a20ec9ed994e456372a911`（早于本轮 before `bad5f736…`），≠ after `583106e5…`。本轮未把修复写入真实索引，与 B3 临时索引口径一致。仓库根文件 `index` 两树 blob 同为 `65d675154f23ffb2d0196e017d44a5e7017550f5`，工作区字节与 after 全等 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，before=after；工作区字节亦与 after 全等 |
| 锁/sidecar 两树 blob | 相同：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| findings/ledger 两树 blob | 相同（本 diff 未改）：findings `74561c951fda7985b24100f8dbfbe9aeb15731dd`，ledger `e0782dc17b5010b371fcb96e80111148b49c5f7c`。after 树 findings 仍 V2.36、024 修复队列「是」、001–023 否；队列未在本 diff 提前清 |
| 源码/迁移/OpenAPI | `git diff --name-only` 仅绑定设计一 md；AccessService / PlatformService / PlatformController / JdbcPlatformRepository / V427 / `docs/openapi/openapi-p0.yaml` before=after |
| 其他三文档 | 集团 PRD / 验收包 / 平台治理 PRD before=after（见下表） |
| 测试面 | 文档类；本席只读对照，未执行 Maven/Java/HTTP/DB |
| 合并状态 | 无 MERGE_HEAD / rebase-merge / rebase-apply / CHERRY_PICK_HEAD。`On branch main` |
| questions/M013* | 无 |
| 添加项 / HTML 申请 | 0 / 0 |
| 当地日 | `date`：2026-09-19 14:34 PDT；`date -u`：2026-09-19 21:34 UTC。L3 最后更新 2026-09-19 与当地日、UTC 日一致 |

`git diff --stat --binary`：

```
 docs/business-entity-binding-development-design-v0.1.md | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)
```

`git diff --numstat`：

```
3	3	docs/business-entity-binding-development-design-v0.1.md
```

`git diff --name-status`：1 处 `M`，无 A/D/R。

after 树绑定设计 vs 工作区（`git cat-file -p <after>:<path>` SHA256 + 行数；工作区 `open().read()`）：

| 文件 | 行 | SHA256 | bytes | before=after | 工作区=after |
|---|---|---|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 154 | `b85d4d42a6bf7bfcdba197da90cf132ed07b8012080a311c3110f97506f25143` | 14885 | 否 | 是 |

git blob：

| 文件 | before blob | after blob |
|---|---|---|
| business-entity-binding-development-design-v0.1.md | `bad5f736e4a4cf2c6d5657753c965b953568e784` | `583106e516a7519e3eccfaf4789a56428a38ec63` |
| group-governance-rebuild-prd-v1.0.md | `d252bdc30d89bc195f80cb8ed6eda835df60de84` | 同左 |
| platform-governance-acceptance-pack-v0.2.md | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` | 同左 |
| platform-governance-prd-v0.1.md | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` | 同左 |

其他三文档 after SHA256（工作区=after）：集团 PRD `7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52`（20430 bytes / 117 行标题仍 V1.03）；验收包 `56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0`（35710 / 标题仍 V0.06）；平台治理 PRD `e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c`（146213 / 标题仍 V0.06）。

代码/OpenAPI 两树 blob（未动）：

| 路径 | blob（before=after） |
|---|---|
| AccessService.java | `3720af8cf3a6a5d1b114789b8342fda2b790be1d` |
| PlatformService.java | `b325adc09bd66d73505130138889b7a87f9bd211` |
| PlatformController.java | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |
| JdbcPlatformRepository.java | `aa34c220de5a8aa45499d11e860a6797891a1dbf` |
| V427__m009_project_profiles_and_associations.sql | `e485a5d29fea55fa714091485aa26facc8c45160` |
| docs/openapi/openapi-p0.yaml | `1f056b360dc60c37b6c026c761f0fa57b3688e63` |

`plan/modules.md` `^## M[0-9]` 计数 471。after 树 findings 版本 V2.36。本轮只用已冻结判据核「拟改是否落地 / 是否越界 / 是否与 HTML·代码现状一致」，不 LOCK-AMEND。四分类不重开。冻结 ID 存在性只核：`GEN-001` / `GEN-002` / `GEN-016` / `PGM-006` 均在 `criteria-lock.md`（V0.11），本席不增判据。

### 1.1 两个 tree 对象原文（`git cat-file -p`，TAB 分隔 object 与路径）

before `2a13fce88ca2b0893863bf13a2b6dca7433a0319`（1673 bytes，SHA256 `7a8ff49d11fc0bb1daf0b053841249f79f141f346c2d58dd9312c091de3032c1`，26 行 / 26 TAB）：

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
040000 tree a21c57ded7bf04a7d44c98172bdd65d58d2a9345	docs
100644 blob 65d675154f23ffb2d0196e017d44a5e7017550f5	index
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

after `2720a4e02b917ab6e88b5ba20d4912d59c007fae`（1673 bytes，SHA256 `3ca13818b72e5da3557261bc7b8c9e54ffe45e24d3833cdc838fa1d4d382165b`，26 行 / 26 TAB）：与上逐行相同，唯一差异为 `docs` 树 `a21c57ded7bf04a7d44c98172bdd65d58d2a9345` → `fff1bdd32bdcfe8f18e53c617e6273d3e7ed6ece`。`backend` 树仍 `1ebe13e6d9ed813f92724d361fd46cea5432fe95`；根 `index` 仍 `65d675154f23ffb2d0196e017d44a5e7017550f5`。

after 全文：

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
040000 tree fff1bdd32bdcfe8f18e53c617e6273d3e7ed6ece	docs
100644 blob 65d675154f23ffb2d0196e017d44a5e7017550f5	index
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

完整原样 `git diff --binary` 见文末 §9（1517 bytes，SHA256 已核）。

---

## 2. 强制技能预检与按意图执行（铁律 3）

环境实核（列目录 / 读 SKILL.md），缺则记缺口，不冒充已调用。

| 技能 | 存在 | 本轮用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| using-arc | 是 | 路由：用户指令优先于 Arc 全量审计/评审工作流。Arc audit/review 本体要派子代理、写 `docs/arc/audits/`、向人提问 → **不执行本体** |
| graft | 是（SKILL.md 存在） | **未运行** `graft` 命令：工具会刷新 `graft/` 图，超出本席唯一可写路径。PlatformController 以 after 树 `git grep`/`git cat-file` 为准（工作区该 Java 文件另有未提交改动，不能当本 diff 证据） |
| grilling / grill-me | 是（`skills/productivity/grilling`、`grill-me`） | 对抗式自问结构；**不等操作员、不派子代理**。grilling 本体要问人 → 不执行本体。事实自己读 |
| batch-grill-me | **缺口** | 技能目录无此名 |
| ubiquitous-language | **缺口** | 技能目录无此名 |
| requesting-code-review | 是 | B4＝本席即审；**不派** reviewer 子代理、不用 `HEAD~1`、不即刻修。基线=派发 tree id |
| receiving-code-review | 是 | 本席是外审不是收票方；只借「先核实再下票、禁止表演性同意」；**不 IMPLEMENT** |
| verification-before-completion | 是 | PASS 前实跑 sha256/wc/`git diff --binary`/`git cat-file`/`git diff --check`/3 句包含/HTML·代码对照 |
| systematic-debugging | 是 | 只借读错误→追数据流；**不加** diagnostic instrumentation。B4 期间不修 |
| diagnosing-bugs | 是 | 只读追拟改落地与删后句子完整；文档类无红循环命令，不造 harness |
| test-driven-development / tdd | 是 | 文档类**不强制**落测试；未造假测试 |
| codebase-design | 是 | 删除测试：本 diff 无新模块接口、无加深/抽缝 |
| ponytail / ponytail-review | 是 | B3 已完成；本席审多余行。净 +3/−3，无旁路抽象 |
| i-have-adhd | 是（`plugins/cache/i-have-adhd/.../skills/i-have-adhd`） | 编号、复述「模块 M013 / 共 471，B3 第 5 次」；本 raw 承载正文。CLI 按派发不回正文 |
| writing-for-agents | 是 | 本 raw 给席甲/操作员读：完成条件可检查（tree/SHA/3 句/逐行归属） |
| resolving-merge-conflicts | 是 | 核无进行中 merge/rebase；不 resolve |
| git-guardrails-claude-code | 是（可选，`skills/misc/git-guardrails-claude-code`） | 本 harness 未装 Claude Code hook；**未安装 hook**。等价禁力：本席未 fetch/commit/push/merge/deploy/`reset --hard` |
| brainstorming / writing-plans | 是 | 阶段 A 技能，本轮 B4 **不适用** |
| ~~dispatching-parallel-agents / subagent-driven-development / executing-plans / using-git-worktrees / finishing-a-development-branch / research / ponytail-audit~~ | 划掉或禁第三席 | **未调用** |

未用 MCP。未派任何子代理/第三席。未提问。未改 findings/ledger/源码/文档。未新建脚本或临时日志文件（核 diff 用过 `/tmp/m013-b4-r5-diff.bin` 后已删，不属 §7 产物）。未读取本轮 self-raw 正文。

---

## 3. 第一性原理三问（2.3）

1. **这段修复为什么必须存在？** 删掉它：绑定设计 L49 仍写「（归档走存档端点）」，接入方会去找主体存档接口。after 树 `PlatformController` 对 `business-entities` 仅 L42 `GET /business-entities`、L52 `POST /business-entities`、L97 `POST /business-entities/{entity_id}/projects` 三入口；存档端点只有 merchants（L108/L117）与 space-types（L190/L199）。同文 L108「完整存档/删除流程未在当前接口落地」、L121「也不存在主体完整 CRUD」与括注相反。上位是已冻结 HTML/实现现状与 GEN-016 当前事实，不是新业务发明。答得上「为何修」。
2. **声称的行为有没有被实际执行验证过？** 文档类不强制跑 Java。证据类型 4：无过滤 `git diff --binary`（1517 bytes / SHA `824d0994…`）、`git cat-file`、工作区 vs after SHA `b85d4d42…` / 154 行、3 处拟改 `==` True 且各恰好 1 次、删后单元格 4 个 `|`（三列）完整、L1=L3=V0.13、最后更新仍 2026-09-19、无变更日志段。类型 1 HTML 原文本轮未改（三 HTML blob 不变）。对照只读 PlatformController/PlatformService/JdbcPlatformRepository/OpenAPI 行，**未执行**测试，不把测试通过当证据。
3. **哪一条前提没验证就当真？** （a）未跑 JVM/HTTP/DB，本轮也不需要——本批只改一份文档；（b）不以 HEAD 或真实索引当修复结果；（c）不以「已获批追加 / B3=5」放宽闸 3；（d）L57/L108/L121 与 before 逐字节相同，不当成被顺手改掉。

不接受的理由未使用：未写「B2 已 PASS 所以 B4 通过」「额度已用满从宽」「获批所以降低标准」「测试绿所以对」。

---

## 4. 逐 ID 修复内容审计（3 处拟改）

定稿执行文本：findings V2.36「本批逐句原句与拟改」1–3。check：每处拟改在 after/工作区恰好 1 次且与拟改字符串 `==` True。对应原句在 after 为 0、在 before 各 1。行级 diff 仅 L1 / L3 / L49（`zip` 三处 `!=`，行数 154=154）。

### 4.1 M013-024 — 细化：删 status 括注「（归档走存档端点）」；版本 V0.12→V0.13

落地（after 行号；工作区同）：

| # | 文件:行 | 结果 |
|---|---|---|
| 1 | 绑定设计:1 标题 V0.12→V0.13 | `p1==L1` True；after 中 V0.12 出现 0 次、V0.13 出现 2 次（仅 L1 与 L3） |
| 2 | 绑定设计:3 版本行 | L3=`> 版本：V0.13 ｜ 最后更新：2026-09-19 ｜ 状态：现行开发设计`；L1=L3=V0.13；最后更新同日未改 |
| 3 | 绑定设计:49 status 行 | 拟改逐字；`（归档走存档端点）` / `归档走存档端点` after 均为 0。单元格 `|` 数 4（与 L50 同行结构），len 140→131（恰删 9 字括注）。句止于 `` `blacklisted` `` |

删后 L49 全文：

```text
| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted` |
```

对照（只读 after 树，未改、未跑）：

- PlatformController.java：`@GetMapping("/business-entities")` L42；`@PostMapping("/business-entities")` L52；`@PostMapping("/business-entities/{entity_id}/projects")` L97。全文件 `@*Mapping("/business-entities…")` 仅此三行。无 `business-entities`×archive。
- 存档入口仍仅：`POST /merchants/{merchant_id}/archive` L108、`…/unarchive` L117；`POST /space-types/{id}/archive` L190、`…/unarchive` L199。
- PlatformService.java:53–55 注释「归档只经存档端点」+ `SUBJECT_STATUSES` 五值（pending_admission / active / suspended / terminated / blacklisted）；:893–898 `requireSubjectStatus` 提示「归档请使用存档操作」，被创建主体（:119）与商户档案更新（:1001）共用。
- JdbcPlatformRepository：`archive_reason`/`archived_at` 写在 merchants 面；`business_entities` 为 INSERT/SELECT/项目关联，无主体存档 UPDATE。
- OpenAPI `BusinessEntityRequest.status` enum 仍 draft / active / suspended / archived（:29231–29235）；blob 未改。023 披露句 L55 与 before 逐字节相同，缺口只披露不闭合。

同文登记仍在且与删括注不矛盾（before=after 逐字节）：

- L57：HTML 词表含「归档」；「当前 `status` 未实现该完整词表和缓冲期闭环。」
- L108：主体档案存档/删除行，实现列「`business_entities` 有 `archive_reason`，完整存档/删除流程未在当前接口落地」。
- L121：「也不存在主体完整 CRUD。文档不得调用不存在的端点。」

L49 现只陈述服务可设五值，不再指向不存在的主体存档端点；差距仍由 L57 / L108 / L121 承载。未改 HTML、代码、OpenAPI、其他三文档、锁。本文件无「变更日志」「变更记录」命中，本 diff 未新造。

### 4.2 版本、日期、越界、不新造

- 绑定设计当前标题/L3 均为 +0.01：V0.12→V0.13，L1=L3。
- 最后更新仍 2026-09-19（定稿：同日不变）。
- 未新造业务规则、未新造日志体系、未把 OpenAPI/代码缺口写闭。
- 三 HTML / 锁 / 根 `index` / 代码 / OpenAPI / 其他三文档 blob 未因本批变为 after。
- 001–023：本 diff 无对应改动。

---

## 5. §5 闸 3 逐变更行归因

规则：每一 `+`/`−` 内容行必须归到本批「是」的 ID；版本行归该文件本批 ID。无无 ID 行。归因是唯一判据，不列黑名单。

### 5.1 绑定设计（hunk 2；−3/+3）— 本文件本批 ID＝024

| 行类 | 摘要 | ID |
|---|---|---|
| − L1 标题 V0.12 | 版本 | 024 |
| ＋ L1 标题 V0.13 | 版本 | 024 |
| − L3 版本 V0.12 / 2026-09-19 | 版本 | 024 |
| ＋ L3 版本 V0.13 / 2026-09-19 | 版本 | 024 |
| − L49 status 含「（归档走存档端点）」 | 024 | 024 |
| ＋ L49 status 句止于 `blacklisted` | 024 | 024 |

hunk 上下文未改：L4 解释权句、L6 文档类型、L46–48 三列、L50 `archive_reason`、L51 时间留痕。无空行插入。无第二文件。

合计变更内容行 −3/+3＝6，全部有 ID（024）。无过滤 diff 仅此一路。归因结论：PASS。

---

## 6. 对抗式（grilling 结构，自问自答；不问人）

1. **第二次获批追加是否降低闸 3？** 否。仍要求 3 处逐字、无无 ID 行、未动 HTML/代码/其他三文档。本席未因 B3=5 放行任何越界。
2. **是否顺手改 Java/OpenAPI 来「闭合」主体存档？** 否。Controller/Service/Repository/OpenAPI blob 未变。
3. **删括注后单元格是否裂开或残句？** 否。仍三列、4 个 `|`，句止于 `blacklisted`，与 L50 结构一致。
4. **L1 是否不等于 L3，或跳号？** 否。皆 V0.13，+0.01。
5. **是否新造变更日志段？** 否。after 中「变更日志」「变更记录」均为 0。
6. **L57/108/121 是否被顺手改掉，造成新矛盾？** 否。与 before 逐字节相同；L49 不再宣称有主体存档端点，与 L108/L121 同向。
7. **Controller 是否其实已有第四个 business-entities 入口？** after 树全文件 `@*Mapping("/business-entities…")` 仅三行。工作区该文件有未提交改动，不纳入本 diff。
8. **真实索引里大量 staged 后端是否算本 diff？** 否。两树 `backend` 哈希相同；`git diff --name-only` 仅一 md。
9. **是否把 023 的 OpenAPI 四值披露一并删掉？** 否。L55 未变。
10. **方案 B 的改写句是否混入？** 否。after 无「`archived` 不由该字段写入」句；执行的是方案 A 删括注。

未发现越界、未发现无 ID 行、未发现新造规则。

---

## 7. 顺从式（本批「是」ID × 落地）

| ID | 拟改处数 | 落地 | 越界 | 与对照一致 |
|---|---|---|---|---|
| 024 | 3（标题/版本行/status 单元格） | 是（逐字 `==` True，各 1 次） | 无 | Controller 仅三入口；L57/108/121 仍登记差距；SUBJECT_STATUSES 五值 |

001–023：本 diff 无行可归，符合「否」不动。

---

## 8. 统一判定

- 修复内容：PASS（3 处拟改逐字落地；删后单元格完整；L1=L3=V0.13；最后更新 2026-09-19；不新造规则/日志/缺口闭合；三 HTML/锁/根 index/代码/OpenAPI/其他三文档未动；主索引四文档仍非 after blob；L57/108/121 仍在且不矛盾）。
- 范围归因：PASS（2 hunk、+3/−3、仅绑定设计；每一变更行归 024；无无 ID 行）。
- 席乙 B4-r5 总结论：PASS。
- 本票不是双 PASS，不是 B5，不是模块收口。

---

## 9. 完整原样 diff（无过滤 `git diff --binary`）

命令：`git diff --binary 2a13fce88ca2b0893863bf13a2b6dca7433a0319 2720a4e02b917ab6e88b5ba20d4912d59c007fae`

指纹：1517 bytes；SHA256 `824d0994ba499072dc33fb65dd6dab3f60fa86b67388d440f2524e0ebc203963`；2 hunk；+3/−3。下文为该命令 stdout 原样（二进制安全；本 diff 无 NUL）。

```diff
diff --git a/docs/business-entity-binding-development-design-v0.1.md b/docs/business-entity-binding-development-design-v0.1.md
index bad5f736..583106e5 100644
--- a/docs/business-entity-binding-development-design-v0.1.md
+++ b/docs/business-entity-binding-development-design-v0.1.md
@@ -1,6 +1,6 @@
-# 四类主体唯一绑定开发设计 V0.12
+# 四类主体唯一绑定开发设计 V0.13
 
-> 版本：V0.12 ｜ 最后更新：2026-09-19 ｜ 状态：现行开发设计
+> 版本：V0.13 ｜ 最后更新：2026-09-19 ｜ 状态：现行开发设计
 > 业务最终解释权：`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。代码、迁移和接口只说明当前实现，不得覆盖三份 HTML。
 
 文档类型：Implementation  
@@ -46,7 +46,7 @@
 | `unified_social_credit_code` | `VARCHAR(128)` | 统一社会信用代码 |
 | `legal_representative` | `VARCHAR(255)` | 法定代表人 |
 | `bank_account_ref` | `TEXT` | 银行账户引用 |
-| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`（归档走存档端点） |
+| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted` |
 | `archive_reason` | `TEXT` | 存档/下线理由 |
 | `created_at/updated_at` | `TIMESTAMPTZ` | 时间留痕 |
 
```
