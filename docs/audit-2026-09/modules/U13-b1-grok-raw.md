CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U13 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 13 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U13 — 未发布材料第 13 段「员工设计及交付说明」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：7 文件，物理行 1184，标记 NORMAL；main 主归属：**M028**（隔离规格 + 域归属 CSV）+ **M058**（T1/T9 设计）+ **M411**（security-baseline-latest.json）+ **M420**（findings-register + formal-security-baseline）
- 冻结判据：M028 ∪ M058 ∪ M411 ∪ M420 映射行并集 **60** 项（相对开工提示 MISSING=[] EXTRA=[]；M028=33，M058=49，M411=18，M420=18，并集=60）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U13-001、GR-U13-002、GR-U13-003、GR-U13-004、GR-U13-005、GR-U13-006、GR-U13-007、GR-U13-008
- 添加项（B1 只登记，不代用户回答 2.2）：**无**。T1/T9「自营商户视同公司自有」能指到 BSM L1433 / PGM L578；工作手机领取/充电断开/扫码 NFC 能指到 BEQR-183 / BSM L1446；PiiCipher 加密能指到 PGM-059；首次私人手机绑定直接生效能指到 PGM L579「安装时生成并绑定」。均属细化，不是添加。独立自营布尔列不在本段 7 文件正文出现（该列在 U10 V408，已答删除）。
- 同根已定稿、不新开 2.2：U04-001 / U05-001 / U05-002 / U05-003 / U05-004 / U05-006 / U05-008 / U07-004 / U10-001 / U14/U15/U18 将承接的 OpenAPI 与生成器
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。
- 本席独立实跑：未发布副本树 `node tools/generate-data-isolation-spec.mjs --check` **PASS**（声称最新迁移 V409）；`node tools/generate-domain-ownership-scope-audit.mjs --check` **PASS**（332 tables）；`node tools/verify-formal-security-baseline.mjs` **PASS**；`node tools/verify-pii-encryption.mjs` **PASS**（16 列）。main 同三项 **PASS**（隔离规格 V410 / CSV 328 表 / 安全基线 720）。只把本段 7 文件覆到 main 迁移+生成器：隔离规格 `--check` **FAIL**（V409 文档 vs V410 迁移）、CSV `--check` **FAIL**（多 `file_version` 列与 4 张新表）。本段安全文档 + main OpenAPI：`verify-formal-security-baseline.mjs --strict` 会因「operation count is not in sync with OpenAPI count 720」红（本席非严格模式已打出同样两条 mismatch）。副本自洽 **不能** 消解 HTML 冲突或对 main 的分叉。只读 `complex_test` flyway 成功版本最高 **410**。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠 7 文件复现 V409/V410 与 730/720 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段文档 → U10/U14/U18 副本 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；副本四项绿不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（T1 仍保留 V066 历史 DDL 草案作背景，不落地删除） |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 7 个文件必须存在吗？必须有**等价的隔离基线、域归属盘点、员工设计说明和安全基线**。删掉它们，BSM-114/116 的交付闸、PGM-059 的加密口径、BSM-085/PGM-033 的注册与设备绑定，在文档/机审上没有本段这一层。文件作为规格与生成物是细化，不是添加。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**不会。** 只把 7 文件覆到 HEAD 迁移/生成器：`generate-data-isolation-spec.mjs --check` 因文档写 V409、main 迁到 V410 而红；CSV `--check` 因多 `file_version` 列和 4 张 U10 表而红；安全基线相对 main 720 项 OpenAPI 报 count 不同步。见 CMD-OVERLAY / CMD-SEC-OVL。
3. 副本树自己能不能绿？能。COPY cwd 四项检查 PASS。这只证明「文档与未发布 U10/U14/U18 生成器一致」，**不能**证明与 HTML 或与当前 main 一致。
4. T1「当前实现」段会不会把已定稿的错误公式锁进设计？会。`:14` 写「按当月固定工资与奖金的比例封顶」和未写「当期应发」的事假扣款，与 HTML 总薪资/应发原文冲突，且与 U03 页已用的「试岗/停岗」中文也不一致。首轮正常阅读设计即发生，不是两三件不巧。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M028 映射行 L6407（33 ID）；M058 映射行 L6437（49 ID）；M411 映射行 L6790（18 ID）；M420 映射行 L6799（18 ID）；并集 60 ID 与开工提示逐项相同（MISSING=[] EXTRA=[]）；定义区均从锁全文读过；HTML 出处行本席对现行正本逐条 HIT（见 §1.2）

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.79，509433 bytes，SHA256 `1702dd1f38a184fc5ff45b6058eb00c7e09e40a4126944f90b063cde62b3523e`（与开工提示一致）。findings V1.67，650136 bytes，SHA256 `062d72e3e264b65809a6f1e903c0eaef6f99db7451c61d2d3285b6f75ead7e75`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉
- U10-001：V400–V409 须重编号 ≥V411，并**重生成数据隔离说明与域归属 CSV**
- U05-002 / U05-004：事假基数=当期应发；封顶分母=HTML 总薪资（不是「底薪+奖金」）
- U05-003 / U07-004：自助注册须设备公钥/指纹；公开端口频控
- U05-006：柜机单端契约，禁止员工会话+柜机验签双身份
- U04-001 / U05-001：独立自营布尔已答删除，改读 `cooperationForm==self_operated`
- U05-008：以 main 为底重放，禁止整树覆盖

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。本段冻结集含 HTML 派生 ID；分类对照现行正本原文（类型 1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

清单本段 7 条与独立 `sha256sum`/`wc -l`/`wc -c` **7/7 MATCH**。副本 SHA 与树 `e28dad05:<path>` blob（`git hash-object`）**7/7 COPY==TREE**。全 76 文件相对基线 `git diff --shortstat 242f4872 e28dad05` = **76 files changed, 4745 insertions(+), 1084 deletions(-)**（与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | + | − | 树 blob | 副本 SHA256 | 清单/字节/行 |
|---|---:|---:|---|---|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 2 | 2 | `f4f0b6c6cb4de800745854c8216ca7c48f5c62c9` | `f41d8414b5011ad31d558531e675ac9c16c030d7fbda2cc1ac6471b64e0c8217` | MATCH 41083 / 371 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 333 | 329 | `199d3261165896f07408aee7190b4688d07ed6e1` | `2049652b0a9aca3ca50769c30a36591309f49488d54925030deca96aa5c0b928` | MATCH 59483 / 333 |
| `docs/production-readiness-evidence/security-baseline-latest.json` | 5 | 4 | `9b25c02b38d5e01edaa2f83dc87265e78de7c3c0` | `c849b52f854c57e7ee79eb1df158ea1c3a460f2c4ec129b829f91ec631740fd9` | MATCH 730 / 14 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | 18 | 9 | `32f0c5bc70ea51551e49a0503ca60cffcb517ec8` | `ed164937c9f3da0e89cca43bf57c1a7f26eb8b1fc330ce8a878309a773d4009f` | MATCH 19290 / 197 |
| `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md` | 17 | 7 | `9c966cfbf5d5bb35fd47dc3eb5c888cde70d7132` | `8ea401b8eb6413a42dd24dde3dd954e721259c650c55a8c30911aac46b8287e6` | MATCH 5841 / 54 |
| `security-scans/findings-register-v1.md` | 3 | 3 | `7e466d104ba94281290871228cb42f9b9edc3012` | `00abf315b8cd5ed81a9f5c3cd05a2f5bf28a89b635abbae32b647cb32caedeba` | MATCH 7806 / 53 |
| `security-scans/formal-security-baseline-v1.md` | 15 | 15 | `a3ccd2c0685924abb4795462ada8de9a417532df` | `7e9dd55bd8fa69f0ecd4c8edef0b7a265cfef7521747d026391ca7d452d6fd05` | MATCH 15321 / 162 |
| **合计** | **393** | **369** | | | 1184 行 |

`git diff --stat 242f4872 e28dad05 -- <7 paths>`：`7 files changed, 393 insertions(+), 369 deletions(-)`。

HEAD 同名：7/7 HAS。BASE vs HEAD：仅隔离规格 **1 文件 +1/−1**（`2e812adf` M001–M010 检查点把 §3A 最新迁移 **V399→V410**）。其余 6 文件 **BASE blob == HEAD blob**（`git log 242f4872..HEAD --` 这 6 路径空）。TREE vs HEAD 隔离规格是 **V0.1+V410（main） vs V0.02+V409（未发布）** 三方冲突。

### 1.1 与 main 分叉对照（对抗式固定问题）

1. **隔离规格会覆盖 main 已修项（GR-U13-001）**。main `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:59` 写最新迁移 **V410**（检查点 `2e812adf`，伴随 `V410__m010_business_entity_topology_and_role_isolation.sql`）。未发布同位置写 **V409**，标题 V0.1→V0.02。按同名路径用 TREE 覆盖 HEAD = 回退 M010 检查点在该文件里的唯一修复。本席 `/tmp/u13_grok/main_plus_u13`（main 迁移 + 未发布该规格）`node tools/generate-data-isolation-spec.mjs --check` EXIT 1：`data isolation spec is out of sync with Flyway migrations`。
2. **CSV 不是回退 M009–M012 正文，但叠到 main 门禁必红**。328 张共有表的 `scope_type/scope_source/scope_path/multi_complex_note/status` 与 main **逐格相同**；未发布多一列 `file_version`（U18 生成器新增，main 生成器无此列）并多 4 表：`staff_locker_records`、`staff_penalty_cap_revisions`、`staff_policy_settings`、`staff_shift_revisions`（U10 V402/V406/V407/V409）。main 生成器 `--check` 对未发布 CSV EXIT 1。
3. **安全基线锁未发布 OpenAPI 730，main 仍是 720**。未发布 register/baseline SHA `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384` = 未发布 `docs/openapi/openapi-p0.json`（U14）独立实算 SHA；main OpenAPI SHA `308dd6313c63db6938b5a0243d8369e43e03684fd8f55abbcfded4f8f0ce30bb`，720 ops。本席 `/tmp/u13_grok_sec2`（U13 三份安全产物 + main OpenAPI + main `package.json`）verifier 输出：`formal baseline operation count is not in sync with OpenAPI count 720` 与 findings register 同样 mismatch。`npm run verify:security-baseline` 走 `--strict`，发布门禁会红。换回未发布 OpenAPI 后同 verifier PASS。
4. **T1 把占用号 V400–V409 写成「本次 M010 在现有迁移之后追加」**。main 已有 V410；U10-001 已定稿须 ≥V411。设计文档若原样合入，会指导发布者继续占用已用号段。
5. **整棵未发布树覆盖**仍会回退其它段 M009–M012（U03/U08 已证实）。本段 6/7 文件 BASE==HEAD，不是那些检查点的载体；但隔离规格 **是** 载体。发布说明必须禁止整树覆盖，且本段必须与 U05+U06+U07+U09+U10+U11+U12+U14+U15+U18 **同一 PR**、以 main 445a49f4 为底**重新生成**隔离规格/CSV/安全基线，迁移 ≥V411。

只读 `complex_test`（`docker exec complex-postgres psql -U complex -d complex_test`，仅 SELECT）：flyway 成功版本 **410,399,398,397,396**。本段无迁移文件、无 JDBC 测试；此只读用于证明 U10 种子未入当前库——隔离规格若声称 V409 为「当前解析到最新」对已迁到 V410 的库是假完成。

### 1.2 HTML 锚点（本席对现行正本逐行 HIT）

锁出处行 → 正本该行含锚点标题/关键字，42/42 HTML 派生 ID HIT + 18 GEN = **60/60**：

| ID | 锁出处 | HIT |
|---|---|---|
| GEN-001…018 | 通用工程判据 | 定义区已读 |
| BEQR-074 | beqr:125 隐私桌台权限 | HIT |
| BEQR-115 | beqr:180 经销价格与税务快照 | HIT |
| BEQR-157 | beqr:240 员工状态 | HIT |
| BEQR-182 | beqr:279 考勤日结果 | HIT |
| BEQR-183 | beqr:280 打卡方式 | HIT |
| BEQR-184 | beqr:281 请假类型 | HIT |
| BEQR-185 | beqr:282 请假单状态 | HIT |
| BEQR-186 | beqr:283 薪资单构成 | HIT |
| BEQR-187 | beqr:285 员工提成发放 | HIT |
| BEQR-192 | beqr:298 分红税务 | HIT |
| BSM-009 | bsm:596 基础管理 | HIT |
| BSM-028 | bsm:698 主要功能 | HIT |
| BSM-029 | bsm:714 和谁有关 | HIT |
| BSM-073 | bsm:1313 主要功能 | HIT |
| BSM-084 | bsm:1427 员工手机端 | HIT |
| BSM-085 | bsm:1431 日常工作 | HIT |
| BSM-086 | bsm:1441 排班、考勤与请假 | HIT |
| BSM-087 | bsm:1455 我的薪资 | HIT |
| BSM-088 | bsm:1467 员工提成、活动执行与人员服务边界 | HIT |
| BSM-114 | bsm:1827 各类入口 | HIT |
| BSM-115 | bsm:1897 相互关系 | HIT |
| BSM-116 | bsm:1928 系统交付与上线 | HIT |
| BSM-117 | bsm:1932 正式上线时的安全加固 | HIT |
| BSM-118 | bsm:1940 能自动识别环境的安装包 | HIT |
| BSM-119 | bsm:1947 和谁有关 | HIT |
| BSM-120 | bsm:1957 已确认规则和实施前输入项 | HIT |
| PGM-008 | pgm:362 员工和手机 | HIT |
| PGM-016 | pgm:404 应急与隐私 | HIT |
| PGM-022 | pgm:463 空间和点位 | HIT |
| PGM-024 | pgm:476 影响业务 | HIT |
| PGM-026 | pgm:492 主要功能 | HIT |
| PGM-032 | pgm:572 员工和手机：人、私人手机、工作手机要能对应起来 | HIT |
| PGM-033 | pgm:576 主要功能 | HIT |
| PGM-034 | pgm:586 工作手机和企业微信 | HIT |
| PGM-035 | pgm:599 提成发放和人员服务治理 | HIT |
| PGM-036 | pgm:611 设备管理 | HIT |
| PGM-048 | pgm:749 商户侧 | HIT |
| PGM-057 | pgm:857 应急恢复和客人隐私 | HIT |
| PGM-058 | pgm:860 紧急恢复 | HIT |
| PGM-059 | pgm:868 客人与员工的个人信息保护 | HIT |
| PGM-060 | pgm:879 会员身份、营销与生物信息治理 | HIT |
| PGM-066 | pgm:951 已拍板规则和待排期问题 | HIT |

本段文档直接咬住的 HTML 原文（类型 1，分类用）：

- BEQR-157 / beqr:240：「待入职、在职、试岗、停岗、离职」
- BEQR-186 / beqr:283 与 BSM-087 / bsm:1458：「总薪资指基础工资、各项补贴、绩效工资与奖金之和」；「请假扣薪不计入上述合计上限……其自身以当期应发薪资为零作为下限」
- BSM-086 / bsm:1450：「按该员工当期请假工时占当期总工时的比例，从其当期应发薪资中扣减」
- BSM-085 / bsm:1433 与 PGM-033 / pgm:578：「在商户档案上标注为自营的商户视同公司自有」；「公司自有员工由本人在私人手机 App 自助注册」
- PGM-033 / pgm:579：「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」
- PGM-033 / pgm:582：「开箱与取件一律扫码……由柜子自动记录，不设手工登记」
- PGM-059 / pgm:868：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」
- BEQR-183 / beqr:280：「上班打卡以领取工作手机为开始……工作手机离开充电设施」

## 2. 文件覆盖（全量，不只 diff）

七文件均完整读完。CSV 332 数据行用 `csv.DictReader` 全量装入（非抽样）；隔离规格 371 行按 §1–§10 与自动区块逐段覆盖。

### 2.1 `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md`（371 行）

| 行段 | 内容 | 覆盖 |
|---|---|---|
| 1–10 | 标题 **V0.02**（相对基线 V0.1）；机器标记；计数漂移说明 | 已读 |
| 11–35 | §1 scope 语义；§2 token 注入、禁止请求参数；nullable 旧行 | 已读 |
| 36–56 | §3 历史人工清单 V044–V061（背景） | 已读 |
| 57–235 | §3A 自动基线：**最新迁移 V409**、170 张含 scope 列的表；起止标记 `DATA_ISOLATION_SPEC_AUTO_*` | 已读（表名从 `access_grant_requests` 到 `wecom_risk_hits` 逐行经过） |
| 236–258 | §4 AC-ISO-1/2/3/4；§5 旧行回填（main 既有，本段未改） | 已读 |
| 259–318 | §6 ISO-GAP；§6A `verify:tenant-isolation` | 已读 |
| 319–371 | §7 主管复核表无 scope 列；§8 CSV 双向绑定；§9 append-only；§10 外包/审计边界 | 已读 |

相对基线真实 diff 仅两处：标题 V0.1→V0.02；§3A 最新迁移 V399→**V409**。main 同位置是 V410。§3A **不含** `staff_locker_records` / `staff_penalty_cap_revisions` / `staff_policy_settings` / `staff_shift_revisions` / `staff_device_credentials`（生成器 `SCOPE_COLUMNS` 无 `scope_id`/`staff_profile_id`）。CSV 有这几张表。§8「§3 ↔ CSV 逐行对应」在 main 已不成立（170 vs 328），本段扩到 332 后缺口更大——登记为观察，不把 HTML 沉默当冲突。

### 2.2 `docs/domain-ownership-scope-audit-v0.1.csv`（333 行 = 1 头 + 332 表）

- 表头：`table_name,scope_type,scope_source,scope_path,multi_complex_note,file_version,status`（main 无 `file_version`）
- 332/332 `status=covered`；`file_version` 全 `V0.01`
- `scope_type` 取值：direct / inherited / global_reference / single_complex_default
- 本席列出全部 332 个 `table_name`（命令证据见 §9 CMD-CSV-TABLES）；与 main 328 表的对称差 = 上述 4 张新表
- 员工相关行：`staff_profiles` 仍 `direct,project_id`（与 main 同；V408 `company_id` 未进入 `directColumns`）；`staff_policy_settings` 由 U18 生成器特判 `scope_id`；`staff_penalty_cap_revisions` = `global_reference`；`staff_locker_records` / `staff_shift_revisions` = inherited
- 生成器在 U18：`tools/generate-domain-ownership-scope-audit.mjs`（副本 `:148-152` 特判、`:201-205` 写 `file_version`）。main 同名生成器 **无** `file_version`。本段 CSV 是生成物，发布须与 U18 生成器同 PR 重生

### 2.3 `docs/production-readiness-evidence/security-baseline-latest.json`（14 行）

```json
"openapi_operation_count": 730,
"security_baseline_version": "V1.02",
"verified_at": "2026-09-08T11:59:55Z",
"notes": "M010 新增注册、排班审批参数、档案状态和寄存事件接口后同步730项接口；…"
```

main 同文件：720 / V1 / 2026-08-03。本文件是一次 `verify:security-baseline` 的证据快照，不是规格正文；计数必须与当时 OpenAPI 一致。未发布 OpenAPI 730 且 SHA 匹配；叠 main 则证据撒谎。

### 2.4 `docs/r5-spec-T1-staff-tables-dev-design-20260618.md`（197 行）

| 行段 | 内容 | 覆盖 |
|---|---|---|
| 1–8 | 标题 V0.12；PiiCipher；**V400—V409** | 已读 |
| 9–16 | **新增**「M010 当前实现」五条：公开自助注册、试用/停职、排班上一级审批、**固定工资与奖金封顶**、柜机双身份 | 已读 |
| 20–87 | V066 历史 DDL 草案（name/phone/id_doc 改为密文 TEXT） | 已读 |
| 91–123 | 状态机：首次绑定直接 active；Service 建议 | 已读 |
| 127–167 | §4 API 表：**仍写** `POST /registrations` =「私人手机 App 提交注册」；无 self-registrations / locker-events 行；批量排班与跨月归属口径（main 既有） | 已读 |
| 161–197 | 事件、权限、GWT、测试计划仍写 `latest()` **到 V066**、变更日志止于 2026-08-06（未记 2026-09-08 当前实现） | 已读 |

相对基线 +18/−9，全部是本段新增设计口径。§4 表与「当前实现」段互相矛盾（GR-U13-007）。

### 2.5 `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md`（54 行）

全文已读。新增：自助 `/staff/self-registrations`；首次绑定直接 active；复用 `DeviceKeystore` 而非新 `DeviceCredentialManager`；**M010 工作手机领取与签验**（`terminal_devices.staff_id`、`terminal_device_signing_keys`、`DeviceRequestHeaders`、「只声明终端类型不足以打卡」、充电断开、扫码/NFC、柜机厂商接口未验收）。GWT 把「注册并绑定」写在一起，但 §1.1 仍把公开自助注册与设备凭证 POST 分成两步、注册步无设备指纹字段。变更日志未记 M010 段。

T9 工作手机段与 U07-001「bind 不写 staffId」对照：T9 写的是**正确合同**（人-机对应），实现缺口归 U07，本段不把正确设计降为缺陷。T9「只声明终端类型不足以打卡」与 U01-001 同向，属细化。

### 2.6 `security-scans/findings-register-v1.md`（53 行）

标题 V1→V1.02；OpenAPI 720→**730**；SHA 改为未发布 `f8de4f8c…`。finding 表 11 行未改业务状态（P0 MDM/NAS 仍 open，TENANT-ISOLATION resolved）。本席逐行读完 status-policy / findings / production-blocking-summary / resolved-evidence / next-verification。无明文密钥。

### 2.7 `security-scans/formal-security-baseline-v1.md`（162 行）

标题 V1→V1.02；operation 720→730、写 434→440、public **5→7**、staff-token 668→676；通道计数 staff_private_phone 30→37、staff_work_phone 70→79、web 687→693、app/kiosk/merchant_* 各 +1/+2。authz 表 public 7 = 未发布 OpenAPI 的 5 个旧 public + `GET /staff/registration-companies` + `POST /staff/self-registrations`（本席对未发布 openapi-p0.json 逐条列出）。§ 仍写「当前 SecurityConfig 允许 `/docs/**` 公开访问」——与 finding SEC-P1-DOCS-EXPOSURE-001 resolved（prod profile 保护）并存，main 既有生成器措辞，观察。

## 3. 数据流（本段文件 → 未发布其它段 → main → SQL/HTML）

```
U13 隔离规格 §3A
  → U18 副本 tools/generate-data-isolation-spec.mjs
       SCOPE_COLUMNS = organization_id/merchant_id/project_id/venue_id/bar_venue_id/space_id
       latest = 迁移目录最后 V 号
  → U10 副本 V400–V409（本段不包含这些 SQL）
  → main 迁移目录最高 V410；main 规格已重生为 V410
  → HTML BSM-114 / PGM 数据范围（token 注入，BSM L707 ④看不到别的商户）

U13 域归属 CSV
  → U18 副本 generate-domain-ownership-scope-audit.mjs
       增 file_version；staff_policy_settings 特判；locker/shift_revisions inheritedRules
  → 同上 U10 四张新表
  → main 生成器无 file_version、无这 4 表
  → HTML BSM-114 入口隔离

U13 T1 / T9
  → U05 StaffRegistrationService / StaffLockerService / JdbcStaffRepository 薪资 SQL
  → U06 StaffService 自助注册 DTO（无设备指纹）
  → U07 SecurityConfig permitAll 两路公开注册；TerminalDeviceService bind 不写 staffId
  → U03 StaffProfilesPage EMPLOYMENT 试岗/停岗（中文正确）
  → U10 V404 employment_status 英文码 trial/suspended（内部标识，允许）
  → U14/U15 OpenAPI 730、两条 public 员工接口
  → HTML BEQR-157 / BEQR-186 / BSM-085–087 / PGM-032–033 / PGM-059

U13 安全基线三件套
  → U18/U14 generate-formal-security-baseline.mjs 读未发布 openapi-p0.json
  → main 720 项契约
  → package.json verify:security-baseline = verifier --strict
```

本段 **没有** Java/SQL 可执行面；可执行正确性落在生成器 `--check` 与 verifier。副本绿 ≠ HTML 通过。

## 4. 三线

### 4.1 第一性原理

- **为何必须存在**：没有隔离规格/CSV，新增员工表不会进入 `verify:data-isolation-spec` / `verify:domain-ownership-scope`；没有 T1/T9，注册/设备/打卡的编码口径没有员工域设计落点；没有安全基线，730 项契约与 P0 闸对不上。删掉它们，BSM-116 交付加固和 PGM-033 设备绑定在文档闸上落空。答得上，不删文件。
- **声称是否执行**：副本树声称「最新 V409 / 332 表 / 730 ops / 与 OpenAPI SHA 一致」——在 COPY cwd **已执行且绿**。把同一声称放到 HEAD：隔离规格、CSV、安全计数 **都不成立**。T1 声称「当前实现」的封顶公式与任职中文，对照 HTML 与 U03 页 **不成立**。
- **未验证前提**：① 发布时以 main 为底重生生成物而不是覆盖；② 迁移已重编号 ≥V411；③ U14 OpenAPI 与本段安全基线同 PR；④ T1「当前实现」已按 U05 定稿改公式与中文。四前提均未在本段文件内被可失败断言锁住。

### 4.2 顺从式

假定实现意图正确（把未发布员工能力写进规格并重生闸口文档），核对本段 60 项：输入（迁移目录/OpenAPI）、状态（生成区块/CSV covered）、输出（V 号、表数、730）、错误（`--check` fail-closed）、权限（public 7）、持久化（无）、测试（本段无单测）、文档。在**未发布树内部**，生成物与 U10/U14/U18 共同实现同一口径。相对 HTML 与 HEAD，T1 当前实现段与安全基线 public 自助注册 **没有** 与 BEQR-157/BEQR-186/PGM-033 实现同一口径。顺从式结论：生成链路在副本内自洽；设计「当前实现」段把已定稿的错误实现写成权威。

### 4.3 对抗式

先假定实现错误并构造反例（均在首轮正常发布路径，非极端巧合）：

| 反例 | 结果 | 收窄 |
|---|---|---|
| 只合本段 7 文件到 445a49f4 | 隔离规格 `--check` 红；CSV `--check` 红；安全基线 count 730 vs 720 | 首轮必红，GR-U13-001 |
| 整文件覆盖隔离规格 | main V410 回退成 V409 | 首轮覆盖即丢检查点，GR-U13-001 |
| 按 T1:14 实现封顶分母 | 锁成底薪+奖金，缺补贴/绩效 | 首轮按设计编码即与 HTML 冲突，GR-U13-002 |
| 按 T1:14 实现事假扣款 | 未钉「当期应发」 | 同根 U05-002，GR-U13-003 |
| 按 T1:11 / T9:11 做公开自助注册 | OpenAPI public + SecurityConfig permitAll，无设备指纹 | 首轮即与 PGM L579 冲突，GR-U13-004 |
| 按 T1:12 做任职中文 | 试用/停职；U03 页已是试岗/停岗 | 设计与同提交 UI 及 HTML 枚举冲突，GR-U13-005 |
| 按 T1:15 做寄存 | 员工会话+柜机双身份 | 同根 U05-006，GR-U13-006 |
| 按 §4 API 表接私人手机到 `/registrations` | 与「当前实现」商户代提交路径对不上 | 首轮接错端点，GR-U13-007 |
| 按 §8 测试计划断言 latest()==V066 | 与 V400–V409/V410 矛盾 | 首轮测计划即过期，GR-U13-008 |
| ISO-GAP-06 明文电话 / §5 回填存量 | main 既有，本段 diff 未改 | 不新开；铁律 8 与 §5 的张力登记观察 |
| 并发改 CSV 手改自动区块 | 生成器 `--check` 会红 | 门禁已覆盖，不升 finding |

## 5. 适用冻结判据逐项

判定：FAIL = 本段文件对该 ID 引入或锁住缺陷；COVERED = 本段有可核对证据且与 HTML 不冲突；N/A = 本段无实现面，给原因。FAIL 不阻止其它 ID 继续审完。

| ID | 结论 | 依据 |
|---|---|---|
| GEN-001 | COVERED | 七文件有规格/闸口用途；不是无需求转发 |
| GEN-002 | **FAIL GR-U13-001/008** | 叠 main 门禁红；T1 测试计划 latest()=V066 与自身 V400–V409 矛盾 |
| GEN-003 | N/A | 本段无测试文件；关联 U11 用例不在本段 |
| GEN-004 | **FAIL GR-U13-004** | 设计与安全基线把无设备证明的自助注册锁成 public |
| GEN-005 | COVERED（设计意图） | T1「提交人不得自审」、T9「后台核对设备状态和员工归属」与 HTML 同向；实现缺口归 U06/U07 |
| GEN-006 | N/A | 本段无事务/约束实现；CSV 只分类 |
| GEN-007 | N/A | 本段无并发路径 |
| GEN-008 | **FAIL GR-U13-001/007** | 生成物与 main 迁移/OpenAPI/生成器不一致；T1 §4 表与「当前实现」不一致 |
| GEN-009 | COVERED | 生成器 `--check` fail-closed；未把失败写成通过 |
| GEN-010 | COVERED | 本段无密钥明文；PiiCipher 方向与 PGM-059 同向；ISO-GAP-06 为 main 既有 |
| GEN-011 | COVERED（有缺口登记观察） | 安全基线有 verified_at；T1/T9 变更日志未记 2026-09-08 段 |
| GEN-012 | N/A | 无批处理/连接 |
| GEN-013 | **FAIL GR-U13-001** | 发布叠法：只合本段则与 HEAD 生成器/OpenAPI/迁移不一致 |
| GEN-014 | **FAIL GR-U13-001** | 规格声称最新 V409，main/库已 V410；U10-001 要求 ≥V411 后重生 |
| GEN-015 | **FAIL GR-U13-005** | T1 展示中文「试用/停职」违反全中文枚举与 BEQR-157；U03 页已用正确词 |
| GEN-016 | **FAIL GR-U13-001/007/008** | 文档 V 号、API 表、latest() 声明不可从当前 main 复现 |
| GEN-017 | COVERED | 本席按冲突→细化→技术性→添加；无把业务发明标技术性；添加项 0 |
| GEN-018 | COVERED | 结论落到 HTML 原文、命令输出、锁定义 |
| BEQR-074 | N/A | 隐私桌台不在本段 7 文件 |
| BEQR-115 | N/A | 经销价格快照不在本段 |
| BEQR-157 | **FAIL GR-U13-005** | T1:12 试用/停职 vs 原文试岗/停岗 |
| BEQR-182 | N/A | 本段不定义考勤日结果枚举 |
| BEQR-183 | COVERED | T9 充电断开/扫码/NFC 与原文同向 |
| BEQR-184 | N/A | 本段未改请假类型 |
| BEQR-185 | N/A | 本段未写撤销/销假（缺口在 U06 已定稿） |
| BEQR-186 | **FAIL GR-U13-002** | T1:14 用固定工资+奖金冒充总薪资 |
| BEQR-187 | N/A | 本段不写提现抽成；未与「不允许现金现抽」冲突 |
| BEQR-192 | N/A | 分红税务不在本段 |
| BSM-009 | COVERED | 员工/设备作为基础管理入口，规格有承载 |
| BSM-028 | COVERED | 「管理员工、岗位、班次…注册审批」有 T1 承载；自营走六类码不在本段发明布尔 |
| BSM-029 | N/A | 票务/酒吧空间归属不在本段展开 |
| BSM-073 | N/A | 商户专区五项不在本段 |
| BSM-084 | COVERED | T9 私人手机仅注册/通知/状态，与「自有手机只开放低风险」同向 |
| BSM-085 | **FAIL GR-U13-004** | 自助注册路径有上位需求，但写成公开无设备标识 |
| BSM-086 | **FAIL GR-U13-003** | 事假扣款未钉当期应发；可见天数「须同时配置上下限」写对了（实现缺口归 U05-005） |
| BSM-087 | **FAIL GR-U13-002** | 同 BEQR-186 总薪资定义 |
| BSM-088 | N/A | 营销打卡/陪场不在本段 |
| BSM-114 | COVERED | 隔离规格 §2 token 注入与入口隔离同向 |
| BSM-115 | COVERED | 工作手机连设备管控在 T9 有承接 |
| BSM-116 | COVERED | 安全基线作为上线闸证据；叠 main 则证据无效（并入 001） |
| BSM-117 | N/A | U 盘密钥破窗不在本段 7 文件实现 |
| BSM-118 | N/A | 安装包环境识别不在本段 |
| BSM-119 | N/A | 上线留痕连接不在本段展开 |
| BSM-120 | **FAIL GR-U13-002/003** | 已确认薪资/请假规则被 T1 当前实现段写窄 |
| PGM-008 | COVERED | 员工和手机模块入口；T1/T9 是其设计 |
| PGM-016 | N/A | 破窗卡片不在本段 |
| PGM-022 | N/A | 空间点位主数据不在本段 |
| PGM-024 | COVERED | T1 排班可指定点位；T9 打卡不手填 |
| PGM-026 | COVERED | 「标注为自营视同公司自有」T1/T9 用 HTML 原句，未写 is_self_operated 列 |
| PGM-032 | **FAIL GR-U13-004** | 人/私人手机对应要求安装时绑定；公开注册无指纹 |
| PGM-033 | **FAIL GR-U13-004/006** | 设备标识与寄存柜自动记录；双身份与「不设手工登记」的单端自动记录不一致 |
| PGM-034 | N/A | 企微会话不在本段 |
| PGM-035 | N/A | 提成发放银行路径不在本段 |
| PGM-036 | COVERED（设计） | T9 要求 staff_id 对应；实现不写归 U07-001 |
| PGM-048 | N/A | 商户侧全开放表不在本段展开 |
| PGM-057 | N/A | 破窗/隐私总述不在本段实现 |
| PGM-058 | N/A | 钥匙更换闭环不在本段 |
| PGM-059 | COVERED | T1 PiiCipher + 日常脱敏与原文同向 |
| PGM-060 | N/A | 会员/人脸不在本段 |
| PGM-066 | COVERED | 界面全中文已拍板；T1 试用/停职违反（并入 005） |

## 6. Findings

### GR-U13-001

- 临时 ID：GR-U13-001
- 文件:行号：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:1,59`；`docs/domain-ownership-scope-audit-v0.1.csv:1` 及 4 张新表行；`docs/production-readiness-evidence/security-baseline-latest.json:9-12`；`security-scans/findings-register-v1.md:6-7`；`security-scans/formal-security-baseline-v1.md:6-7,44`；`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:7`
- 完整原文（隔离规格 `:59`）：`当前解析到最新迁移 **V409**，发现 **170** 张含 scope 字段的表。`
- 业务影响：按同名路径发布会把 main 检查点已写的 **V410** 改回 V409；CSV/安全基线与当前 main 迁移、OpenAPI、生成器不一致，`verify:predeploy` 链上 `verify:data-isolation-spec` / `verify:domain-ownership-scope` / `verify:security-baseline`（`--strict`）首轮即红。不是业务新规则，是发布叠法。
- 可复现命令/最小反例：见 §9 CMD-OVERLAY、CMD-SEC-OVL。隔离规格 BASE→HEAD 唯一 diff 是 V399→V410（`2e812adf`）；TREE 是 V399→V409。
- 冻结判据：GEN-002、GEN-008、GEN-013、GEN-014、GEN-016
- 2.3 依据类型：**4**（命令输出）+ 对照 HEAD 文件
- 四分类：**技术性**（无业务发明理由：V 号、生成列表、OpenAPI 计数是构建/闸口一致性，不引入新业务实体。同根 U10-001 / U05-008）
- 工程缺陷：是，建议登记
- 最小处置（写给发布者，本轮不修）：以 main `445a49f4` 为底重放本段；隔离规格与 CSV 在迁移重编号 **≥V411** 后用当时生成器重生；安全基线在 U14/U15 OpenAPI 定稿后 `npm run docs:security-baseline` 再生成；与 U05+U06+U07+U09+U10+U11+U12+U14+U15+U18 同一 PR；**禁止整树覆盖**。

### GR-U13-002

- 临时 ID：GR-U13-002
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:14`
- 完整原文：`普通扣除合计按当月固定工资与奖金的比例封顶，默认20%，提成不进入分母，事假单算。`
- 业务影响：把 HTML「总薪资 = 基础工资、各项补贴、绩效工资与奖金之和」写成「固定工资与奖金」。提成排除与「事假单算」两点与 HTML 同向；缺补贴/绩效。设计若原样指导编码，会把 U05-004 的错误公式锁成规格。
- 可复现命令：`sed -n '14p'` 该文件；对照 `docs/business-enum-quick-reference-v0.1.html:283` 与 `docs/business-system-map-for-operations-v0.1.html` BSM-087 原文。
- 冻结判据：BEQR-186、BSM-087、BSM-120、GEN-016
- 2.3 依据类型：**1**（HTML 逐字）
- 四分类：**冲突**（同根 U05-004）
- 工程缺陷：是，建议登记
- 最小处置：随 U05-004 改「当前实现」句：分母按 HTML 总薪资各项之和（现有列先取全，补贴/绩效列落地后并入）；注释不得称底薪+奖金为总薪资；提成排除与事假单算保留。

### GR-U13-003

- 临时 ID：GR-U13-003
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:14`
- 完整原文：`事假仅按已批准且覆盖实际排班的工时占比扣款，扣减至零为止。`
- 业务影响：HTML 要求「从其**当期应发薪资**中扣减」。本句只写工时占比与零下限，未写被减数=当期应发；与上一句把「固定工资与奖金」当薪资基数连读，会把 U05-002 的窄基数写成设计。工时比例、已批准、零下限三点与 HTML 同向，须保留。
- 可复现：对照 BSM-086 / bsm:1450 原文。
- 冻结判据：BSM-086、BSM-120、GEN-016
- 2.3 依据类型：**1**
- 四分类：**冲突**（同根 U05-002）
- 工程缺陷：是，建议登记
- 最小处置：改写为按当期请假工时占当期总工时的比例，从当期应发薪资扣减，下限为零；随 U05-002 同批。

### GR-U13-004

- 临时 ID：GR-U13-004
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:11`；`docs/r5-spec-T9-staff-device-android-dev-design-20260618.md:11`；`security-scans/formal-security-baseline-v1.md:44`（public 7）；未发布 OpenAPI `POST /api/v1/staff/self-registrations` `x-auth-context: public`
- 完整原文（T1:11）：`公司员工通过公开 /staff/self-registrations 本人提交……公开提交只返回申请编号与待审状态`
- 业务影响：PGM L579 / BSM L935 要求私人手机 App **安装时**生成并绑定该手机专属设备标识。本段把公司自助注册写成公开接口、安全基线把该操作锁进 public=7，与设备绑定顺序冲突。同根 U05-003 / U07-004 / U01-007。
- 可复现：未发布 OpenAPI 列出 7 个 public，后两个即 `listStaffRegistrationCompanies` 与 `submitStaffSelfRegistration`；main 仅 5 个 public。
- 冻结判据：PGM-032、PGM-033、BSM-085、BSM-120、GEN-004、GEN-010
- 2.3 依据类型：**1** + **2**（OpenAPI 实际字段）
- 四分类：**冲突**
- 工程缺陷：是，建议登记
- 最小处置：随 U05-003：自助注册请求带设备公钥/指纹；无指纹 4xx。安全基线/OpenAPI 在契约改完后重生；公开端口频控归 U07-004。不新开 2.2。

### GR-U13-005

- 临时 ID：GR-U13-005
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:12`
- 完整原文：`任职状态独立保存待入职、在职、试用、停职、离职`
- 业务影响：BEQR-157 明文「待入职、在职、**试岗**、**停岗**、离职」。同提交 U03 `StaffProfilesPage.tsx:26` 已用试岗/停岗。T1 用近义错词，会指导错误展示中文（铁律 7 / PGM-066 界面全中文，枚举以 HTML 为准）。英文码 `trial`/`suspended` 是内部标识，允许。
- 可复现：`python3` 统计 T1 含「试用/停职」不含「试岗/停岗」；UI 相反。
- 冻结判据：BEQR-157、GEN-015、GEN-008、PGM-066
- 2.3 依据类型：**1**
- 四分类：**冲突**（本段独有文档面；实现/UI 已正确）
- 工程缺陷：是，建议登记
- 最小处置：T1 该句改为「待入职、在职、试岗、停岗、离职」，与 HTML 及 U03 页一致。

### GR-U13-006

- 临时 ID：GR-U13-006
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:15`
- 完整原文：`/staff/locker-events 接收已验证柜机的扫码完成事件，员工取认证身份、柜机取设备身份`
- 业务影响：把 U05-006 的双身份契约写成设计现状。HTML PGM L582「由柜子自动记录，不设手工登记」要求柜机单端自动上报。76 文件内无单端调用方。属实现技术债被文档固化。
- 可复现：对照 U05 `StaffLockerService` 与 U11 双身份用例（已定稿 U05-006 / U11-006）。
- 冻结判据：PGM-033、BSM-120、GEN-002、GEN-004
- 2.3 依据类型：**1** + **2**
- 四分类：**技术性**（无业务发明理由：未新造寄存业务，只是把错误调用形态写进规格；上位需求是 HTML 已有的自动记录。同根 U05-006）
- 工程缺陷：是，建议登记
- 最小处置：随 U05-006 改单端契约后，T1 该句改为柜机以已登记 kiosk 签名上报、员工身份由扫码载荷承载；删除「员工取认证身份、柜机取设备身份」并列要求。

### GR-U13-007

- 临时 ID：GR-U13-007
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:130` 对比 `:11`
- 完整原文（`:130`）：`| POST | /api/v1/staff/registrations | staff.profile.submit | 私人手机 App 提交注册 |`
- 业务影响：同文件 `:11` 已改成「商户代提交走 `/staff/registrations`，公司员工走公开 `/staff/self-registrations`」。§4 表未改、也未增 self-registrations / locker-events / employment-status / settings 行。按表接线会把私人手机接到商户代提交端点。
- 可复现：同一文件 `:11` 与 `:130` 对照。
- 冻结判据：GEN-008、GEN-016
- 2.3 依据类型：**4**（文件内对照命令即可）
- 四分类：**技术性**（无业务发明理由：两条路径 HTML 已有；缺陷是文档两节口径不一致，不是新业务）
- 工程缺陷：是，建议登记
- 最小处置：§4 表与「当前实现」对齐：`/registrations` 改为商户代提交；增 self-registrations（随 004 补设备字段后的鉴权上下文）、locker-events、employment-status、settings 行；或删除过时表、只保留指向 OpenAPI 的一句。

### GR-U13-008

- 临时 ID：GR-U13-008
- 文件:行号：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:179`
- 完整原文：`- 迁移应用：\`MigrationVersions.latest()\` 动态断言到 V066；CHECK 约束拒绝非法枚举。`
- 业务影响：同文件 `:7` 已写 V400—V409；main 已 V410；U10-001 要求 ≥V411。测试计划仍钉 V066，按计划写测试会与 `latest()` 真值冲突。
- 可复现：同文件 `:7` vs `:179`。
- 冻结判据：GEN-002、GEN-016
- 2.3 依据类型：**4**
- 四分类：**技术性**（无业务发明理由：版本钉是测试卫生，不引入业务规则）
- 工程缺陷：是，建议登记
- 最小处置：改回「动态读 `MigrationVersions.latest()`，不硬钉」；删除「断言到 V066」。随 U10 重编号自然成立。

## 7. 添加项（供主调度按 2.2 处理）

**无。** 不代用户回答。U04-001 已答删除不在本段文件出现独立布尔列，T1/T9 只用 HTML「标注为自营」原句。

## 8. 观察（无新增根因，完整登记，建议队列「否」）

1. 本段 6/7 文件 BASE==HEAD，M009–M012 **未改** 这些路径；唯一 HEAD 已修项是隔离规格 V410。
2. 整树覆盖禁令仍适用（他段载体）。
3. 副本 COPY cwd 四项检查 PASS 与席甲将跑的 `/tmp/u-overlay` 互不采信；本席以自己的 COPY cwd 与 `/tmp/u13_grok*` 为准。
4. 首次稀疏 overlay 缺 `package.json` 导致 verifier `JSON.parse` Unexpected end of JSON input——**不计入**安全基线缺陷；补齐 package.json 后得到 count 720 mismatch 才是证据。
5. `verify-formal-security-baseline.mjs` 无 `--strict` 时打印 failed 仍 exit 0；`package.json` 的 `verify:security-baseline` 带 `--strict`。本席 overlay 未加 `--strict` 时已打出 mismatch 文本；发布走 npm 会非 0。
6. 隔离规格 §8「§3 ↔ CSV 逐行对应」在 main 已不成立（170 vs 328），本段 332 后更宽。生成器设计使无六列的表只进 CSV。登记，不升冲突。
7. `staff_policy_settings.scope_id` 不在隔离规格 `SCOPE_COLUMNS`，故 §3A 无该表；CSV 有特判。跨文档，生成器限制，观察。
8. V408 `staff_profiles.company_id` 未进入 CSV `directColumns`，`staff_profiles` 仍标 `project_id`（与 main 同）。归 U05-010 / U18 生成器，本段不新开。
9. 隔离规格 §5 旧行回填 vs 铁律 8「不为存量数据做兼容」：main 既有段落，本段 diff 未改。登记给调度，不在本段发明删除。
10. ISO-GAP-06 `saved_bottles.customer_phone` 明文：main 既有。
11. formal baseline 仍写 SecurityConfig 允许 docs 公开，与 SEC-P1-DOCS-EXPOSURE-001 resolved 并存：main 既有生成器措辞。
12. main 自身 findings-register 的 OpenAPI SHA `4e19b1a3…` 与当前 main OpenAPI SHA `308dd631…` 不一致，但 **count 仍 720**，verifier 不校验 SHA。属 main 既有漂移，不记本段须修。
13. T1/T9 变更日志未记 2026-09-08「当前实现」/工作手机段。
14. T1 仍保留 V066 历史 DDL 作草案：标明历史则可；与「当前实现」并存易混，观察。
15. T9 工作手机「沿用 staff_id」是正确合同；U07-001 实现不写是另一段已定稿项。
16. T9「只声明终端类型不足以打卡」与 U01-001 同向，属正确细化。
17. T1 可见天数「出厂含今天共七天，配置须同时填写上下限」与 BSM L1444 同向；实现缺种子归 U05-005/U10-003，本段设计句不判冲突。
18. 安全基线 notes 引用「160可丢弃副本 r1-predeploy-final.log」：本席未打开该日志；不以未读日志声称那次跑通过。副本 verifier PASS 已足以证明**当前**文件与未发布 OpenAPI 自洽。
19. `staff_device_credentials` 在 CSV 为 `single_complex_default`（无直接 scope 列，经 staff_profile 继承）：合理分类，观察。
20. 未发布 commit 对象本仓不存在，只认 tree `e28dad05` 与输入副本。

## 9. 命令证据

### CMD-LOCK

`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`

### CMD-HEAD

`git rev-parse HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`

### CMD-OBJECTS

`git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b` = tree；`242f4872…` = commit；`03acdb98…` fatal

### CMD-HASHES

七文件 sha256/wc -l/wc -c 与开工提示及清单 **7/7 MATCH**；`git hash-object` = tree blob **7/7**。

### CMD-DIFF-BASE

`git diff --shortstat 242f4872 e28dad05` = `76 files changed, 4745 insertions(+), 1084 deletions(-)`

本段 `7 files changed, 393 insertions(+), 369 deletions(-)`

### CMD-UNPUB（cwd = 输入副本根，未写文件）

```
node tools/generate-data-isolation-spec.mjs --check
# exit 0  data isolation spec is in sync with Flyway migration scope baseline.

node tools/generate-domain-ownership-scope-audit.mjs --check
# exit 0  DOMAIN_OWNERSHIP_SCOPE_PASS: 332 tables classified

node tools/verify-formal-security-baseline.mjs
# exit 0  Formal security baseline verification passed: …

node tools/verify-pii-encryption.mjs
# exit 0  PII_ENCRYPTION_PASS: 16 个 PII 列均经 piiCipher.encrypt 落库
```

### CMD-MAIN（cwd = 主工作区，只读）

同三项 exit 0：隔离规格 V410 同步；CSV 328 tables；安全基线 passed。

### CMD-OVERLAY（`/tmp/u13_grok/main_plus_u13`：main 迁移+main 生成器 + 仅本段文档）

```
node tools/generate-data-isolation-spec.mjs --check
# exit 1  data isolation spec is out of sync with Flyway migrations. Run: npm run docs:data-isolation-spec

node tools/generate-domain-ownership-scope-audit.mjs --check
# exit 1  docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations; run npm run docs:domain-ownership-scope
```

（同目录首次安全 verifier 因缺 package.json 报 JSON.parse，**作废**；不以该失败当 count 证据。）

### CMD-SEC-OVL（`/tmp/u13_grok_sec2`：U13 三份安全产物 + **main** OpenAPI + main package.json）

```
node tools/verify-formal-security-baseline.mjs
# Formal security baseline verification failed:
# - formal baseline operation count is not in sync with OpenAPI count 720
# - findings register operation count is not in sync with OpenAPI count 720
```

换入未发布 OpenAPI 后同命令：`verification passed`。`package.json` 脚本为 `--strict`，合入 main 走 npm 会非 0。

### CMD-OPENAPI-SHA

未发布 `docs/openapi/openapi-p0.json` SHA256 = `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384` = 本段 register/baseline 所载。main OpenAPI SHA256 = `308dd6313c63db6938b5a0243d8369e43e03684fd8f55abbcfded4f8f0ce30bb`。未发布 730 ops / public 7；main 720 / public 5。

### CMD-CSV-TABLES

`csv.DictReader` 332 unique table_name，status 全 covered；only-copy = `staff_locker_records, staff_penalty_cap_revisions, staff_policy_settings, staff_shift_revisions`；shared 328 行业务列与 main 无 diff。

### CMD-HTML-HIT

42/42 HTML 派生 ID 锁出处行含锚点关键字。MISS_COUNT=0。

### CMD-DB-RO

`docker exec complex-postgres psql -U complex -d complex_test -tAc "SELECT version FROM flyway_schema_history WHERE success … LIMIT 5"` → 410,399,398,397,396。无写。

### CMD-ISO-THREEWAY

```
git diff 242f4872 HEAD -- docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
# 仅 §3A V399 → V410

git diff 242f4872 e28dad05 -- <同路径>
# 标题 V0.1→V0.02；§3A V399 → V409

git diff HEAD e28dad05 -- <同路径>
# 标题 V0.1 vs V0.02；V410 vs V409
```

## 10. 结论

**FAIL**

本段 7/7 文件全量三线已跑完，60/60 冻结判据已逐项核对，无未答 2.2，无「有条件通过」。工程须修 8 项（001 叠法/重生；002–005 冲突；006–008 技术性）；添加 0。副本四项绿只证明未发布树自洽。发布者须以 main `445a49f4` 为底重生生成物、按 U05 定稿改正 T1「当前实现」段，禁止整树覆盖。

本席未读 `U13-b1-self-raw` 或其它本轮本方结论。适配器只应回路径与 exit。

## U13 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 52906 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 13 段：员工设计及交付说明」（7 文件 1184 行，main 主归属 M028+M058+M411+M420；适用判据＝四模块映射行并集 60 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；添加 0、不发新 2.2；发布前须修「是」=001–008，「否」=009）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M028 映射行 L6407 共 33 ID，M058 映射行 L6437 共 49 ID，M411 映射行 L6790 共 18 ID，M420 映射行 L6799 共 18 ID，并集 60，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob 7/7 与树 `e28dad05` 全等、T1 `:7/:11/:12/:14/:15/:130/:179`、T9 `:11/:29`、隔离规格 COPY `:59` V409 vs main V410、安全基线 730/public 7、未发布 OpenAPI SHA `f8de4f8c…`、main OpenAPI SHA `308dd631…` 与 register `4e19b1a3…` 漂移、HTML BEQR L240/L283、BSM L1433/L1450/L1458/L935/L938、PGM L578/L579/L582、U03 `StaffProfilesPage.tsx:26` 试岗/停岗、`questions/U04-001.md` 答复首行「删除」、已定稿 U01/U05/U07/U10 同行、package.json 既有 `docs:`/`verify:` 脚本名）；未重跑 overlay/生成器、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u13-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.68 顶部「当前 U13 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U13-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 52906 | `c92778a5544b7df47c613e995c058811efcdaa2efc6a2d5bde98db2363a3f173` |
| `docs/audit-2026-09/modules/U13-b1-self-raw.md` | V0.01 | 22836 | `3c8149cd5d710b6f8c0fb8b42f4ef8acd15019cbb6c500d0e65d406d48490803` |
| `docs/audit-2026-09/findings.md` | V1.68 | 658509 | `4ef6971335845ce85ef3d3aef1d927653360082aba4926aab721a831a2735f6e` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.81 | 514379 | `71330177e04ff08886a5c02f784d738a64b9999eb319e07ceaf5b99072217798` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.68 顶部 U13-001…009；questions 0；添加 0；无 HTML 修改申请。U13 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 7/7 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --stat 242f4872 e28dad05 --` 本 7 路径 = 7 files, +393/−369。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。隔离规格 BASE→HEAD 已改（V399→V410）；其余 6 文件 BASE blob == HEAD blob。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 四分类与 GEN-017/同根 U05、(b) 001 叠法是否一条、(c) T9:29 移观察是否放生缺陷、(d) 009 是否写错 §8.4/9/12 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/原文行号/HTML 锚点；本票不声称本轮新跑 overlay 或四项门禁 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本段文档 → U10/U14/U18 生成器 → main V410/720 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001 只重生既有生成物；002–005 只改与 HTML 相反的规格句；006–008 只改调用形态/过时表/版本钉 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 席甲把 T1/T9 四处措辞捆成一条技术性，候选拆 002–005 为冲突、001/006–008 为技术性，是否把业务发明伪装回去？否。见 (a)。
2. 隔离规格、CSV+U18、三件安全产物、T1:7 能否并成一条生成物/门禁项？能。见 (b)。
3. 把 T9:29「沿用 staff_id」移出 finding，是否放生人-机对应缺陷？否。缺陷已在 U07-001。见 (c)。
4. 009 把第 4/9/12 条写成观察，是否把应升队项降级？否。见 (d)。

### 完整差集（GR-001…008、§8 观察 1–20、席甲 CX-001…003、OBS-1…5）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–009。未把本席 finding 降观察。席甲 CX-003 之 T9:29 由候选移观察，与本席 §8.15 同向，不改写本席原意。席甲独有项无超出 CX-001…003 与 OBS-1…5。席甲 B1 漏判五处（事假被减数、五态中文、柜机双身份措辞、§4 表口径、V066 钉）且把封顶/注册句记技术性——候选已勘误接受本席八项须修并按 GEN-017 把 002–005 改记冲突，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U13-001 | U13-001 | 否：技术性叠法；重生隔离规格/CSV/安全三件；T1:7 改实际编号；≥V411；同 PR；禁整树 |
| GR-U13-002 | U13-002 | 否：冲突；封顶分母＝HTML 总薪资；同根 U05-004 |
| GR-U13-003 | U13-003 | 否：冲突；事假被减数＝当期应发；同根 U05-002 |
| GR-U13-004 | U13-004 | 否：冲突；公开自助注册无设备证明；同根 U05-003/U07-004/U01-007 |
| GR-U13-005 | U13-005 | 否：冲突；试用/停职→试岗/停岗 |
| GR-U13-006 | U13-006 | 否：技术性；柜机双身份措辞；同根 U05-006 |
| GR-U13-007 | U13-007 | 否：技术性；§4 表与「当前实现」口径相反 |
| GR-U13-008 | U13-008 | 否：技术性；latest() 钉 V066 |
| §8 1–20 | U13-009 | 「含」为闭集转述；第 4/9/12 条未把观察升冲突，也未把 finding 降观察（见 (d)） |
| 席甲 CX-U13-001 | U13-001 | 子集（仅隔离规格 V409）；并入 GR-001 不改技术性 |
| 席甲 CX-U13-002 | U13-001 | 子集（CSV+U18 同批）；并入 GR-001 不改技术性 |
| 席甲 CX-U13-003 封顶句 | U13-002 | 分类由技术性改冲突，与 GR-002 一致 |
| 席甲 CX-U13-003 注册句 | U13-004 | 分类由技术性改冲突，与 GR-004 一致 |
| 席甲 CX-U13-003 之 T1:7 | U13-001 | 吸收进叠法，不另开；与 GR-001 含 `:7` 一致 |
| 席甲 CX-U13-003 之 T9:29 | U13-009 | 移观察，与 §8.15 一致；缺陷留 U07-001 |
| 席甲 OBS-1 | U13-001（重生面）+ U13-009（当前 SHA 与未发布 OpenAPI 一致这一事实） | 重生须修在 001，不按席甲「不另立须修」放生 |
| 席甲 OBS-2…5 | U13-009 | 维持观察 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U13-001 | GR-U13-001 + 席甲 CX-U13-001/002 | 技术性（生成物与门禁，同根 U10-001/U05-008）/是/是 | **PASS（技术性叠法/是/是；只引用既有生成器与门禁）** |
| U13-002 | GR-U13-002 + 席甲 CX-U13-003 封顶句 | 冲突（同根 U05-004）/是/是 | **PASS（冲突/是/是）** |
| U13-003 | GR-U13-003（席甲漏） | 冲突（同根 U05-002）/是/是 | **PASS（冲突/是/是）** |
| U13-004 | GR-U13-004 + 席甲 CX-U13-003 注册句 | 冲突（同根 U05-003/U07-004/U01-007/U02-002）/是/是 | **PASS（冲突/是/是）** |
| U13-005 | GR-U13-005（席甲漏） | 冲突/是/是 | **PASS（冲突/是/是）** |
| U13-006 | GR-U13-006（席甲漏） | 技术性（同根 U05-006）/是/是 | **PASS（技术性/是/是）** |
| U13-007 | GR-U13-007（席甲漏） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U13-008 | GR-U13-008（席甲漏） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U13-009 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 4/9/12 条未写错原意）** |

技术性入队 4（001 生成物重生、006 柜机措辞、007 §4 表、008 V066 钉）。冲突入队 4（002 封顶分母、003 事假基数、004 公开注册无设备证明、005 五态中文）。登记观察 1（009）。添加 0。questions 0。无 HTML 修改申请。

发布前须修「是」=001–008（8）；「否」=009（1）。与候选汇总一致。席甲 B1 票 FAIL 但只立三项且分类过低，属低估；候选接受本席八项须修并勘误，本席独立复核后维持。

### 重点 (a) 002–005＝冲突，001/006/007/008＝技术性 — **PASS（与 GR-001…008 四分类一致；GEN-017 顺序与同根 U05 项一致）**

独立核，不采信席甲「文档随代码定稿改」或候选「按 GEN-017 改记冲突」六个字为己证。

GEN-017 原文：每项内容按 **冲突→细化→技术性→添加** 顺序分类；不得把业务发明伪装成技术实现。

本席 B1 四分类 vs 候选：

| GR | 本席四分类 | 候选 | 同根已定稿 |
|---|---|---|---|
| 001 | 技术性 | 技术性 | U10-001 / U05-008 技术性叠法 |
| 002 | 冲突 | 冲突 | U05-004 冲突 |
| 003 | 冲突 | 冲突 | U05-002 冲突 |
| 004 | 冲突 | 冲突 | U05-003 / U07-004 / U01-007 / U02-002 冲突 |
| 005 | 冲突 | 冲突 | 本段文档面独有；U03 页已用正确中文 |
| 006 | 技术性 | 技术性 | U05-006 技术性 |
| 007 | 技术性 | 技术性 | 文档两节口径；HTML 两条路径已有 |
| 008 | 技术性 | 技术性 | 测试卫生；随 U10 重编号 |

本轮原文（副本，本席抽出，不采信席甲引文为己证）：

- T1 `:14`「普通扣除合计按当月固定工资与奖金的比例封顶」。HTML BEQR L283 / BSM L1458「总薪资指基础工资、各项补贴、绩效工资与奖金之和」。规格句用固定工资+奖金冒充总薪资，与 HTML 原句相反 → **冲突优先**。提成排除与事假单算两点同向，处置保留。不得因「这是设计文档」降技术性。同根 U05-004 已是冲突。席甲 CX-003 记技术性才是把冲突伪装成实现债。
- 同段后句「事假仅按已批准且覆盖实际排班的工时占比扣款，扣减至零为止」未写被减数。HTML BSM L1450「从其当期**应发薪资**中扣减」。与前句连读会把固定工资+奖金当基数。工时比例、已批准、零下限同向，须保留。同根 U05-002 冲突。席甲漏判；候选立 003 成立。
- T1 `:11`「公司员工通过公开 `/staff/self-registrations` 本人提交」+ T9 `:11` 登录页自助 + 安全基线 `:44` public 7。HTML PGM L579 / BSM L935「私人手机 App **安装时**生成并绑定该手机专属的设备标识」。公开无指纹与绑定顺序相反 → **冲突**。同根 U05-003。席甲捆进 CX-003 技术性，过低。
- T1 `:12`「待入职、在职、试用、停职、离职」。HTML BEQR L240「待入职、在职、**试岗**、**停岗**、离职」。同提交 U03 `StaffProfilesPage.tsx:26` 已是试岗/停岗。英文码 `trial`/`suspended` 是内部标识，允许。展示中文近义错词指导错误界面（GEN-015 / PGM-066）→ **冲突**。席甲漏判。

001/006/007/008 保持技术性：V 号/生成列表/OpenAPI 计数是闸口一致性，不引入新业务实体；柜机双身份不改「扫码留痕、不手工登记」的上位需求；§4 表与「当前实现」对不上是文档卫生；V066 钉是测试计划过期。无业务发明。与 U10-001/U05-008/U05-006 一致。

票 **PASS**。

### 重点 (b) U13-001 把隔离规格、CSV+第 18 段生成器、三件安全产物、T1:7 并为一条生成物/门禁项 — **PASS（成立；处置只引用既有生成器与门禁）**

独立核：

1. 副本隔离规格 `:59` 原文「当前解析到最新迁移 **V409**」；main 同行「**V410**」。BASE→HEAD 该文件唯一实质 diff 是 V399→V410（检查点）。TREE 是 V399→V409。按同名路径覆盖 = 回退检查点。B1 CMD-OVERLAY：main 生成器 `--check` EXIT 1。本轮不重跑，源码行仍成立。
2. 副本 CSV 表头含 `file_version`，332 表；main 无该列、328 表。对称差四张：`staff_locker_records`、`staff_penalty_cap_revisions`、`staff_policy_settings`、`staff_shift_revisions`。生成器改动在第 18 段。只合 CSV 不合工具则 `verify:domain-ownership-scope` 红。席甲 CX-002 单独成立，但是与隔离规格同一发布动作（以 main 为底重生生成物）。
3. 安全三件钉未发布 OpenAPI SHA `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384`（本轮复算未发布 `openapi-p0.json` 全等）、730 ops、public 7。main OpenAPI SHA `308dd631…`、720/public 5。B1 CMD-SEC-OVL 打出 count 720 mismatch。`package.json` 的 `verify:security-baseline` = verifier `--strict`。席甲 OBS-1 把重生推到第 14 段、不另立须修——过低：叠到当前 main **已经**红，不必等 U14 删自营端点。并入 001 与 GR-001 一致。
4. T1 `:7`「本次 M010 在现有迁移之后追加 V400—V409」。main 已有 V410；U10-001 已定稿须 ≥V411。这不是生成器输出，但是同一编号根因：不改这句，发布说明会指导继续占用已用号段。GR-001 位置表已含 `:7`。不并入 008（008 是 `:179` 把 `latest()` 钉死 V066，失败形态不同）。

四条失败形态不同（规格 V 号、CSV 列/表、OpenAPI 计数、设计文档号段），**同一发布动作**：以 main `445a49f4` 为底重放本段，迁移 ≥V411 后用当时生成器重生，OpenAPI 定稿后重生安全三件，禁止整树覆盖。同根已定稿 U10-001 / U05-008 已是这条叠法。并成一条成立，不是把 002–005 的 HTML 冲突捆进「技术性」。

最小处置只引用既有，无业务发明。本轮核 `package.json` 已有：

- `docs:data-isolation-spec` / `verify:data-isolation-spec`
- `docs:domain-ownership-scope` / `verify:domain-ownership-scope`
- `docs:security-baseline` / `verify:security-baseline`（`--strict`）

候选处置：以 main 为底；≥V411 后跑上述脚本；U14/U15 OpenAPI 定稿后重生三件；T1 `:7` 改为实际编号；不手改自动块；禁止整树覆盖。无新闸、无新业务实体。

「用 main 生成器」须与「第 18 段工具同批」连读：当前 main 生成器无 `file_version`、无四张新表特判，只跑 main 脚本会把本段 CSV 生成回 328 表。定稿应写成「以 main 为底叠入第 18 段生成器后重生」。属措辞修正，不改分类/须修，见文末第 1 条。

票 **PASS**。

### 重点 (c) 席甲 CX-003 中 T9:29「工作手机沿用 staff_id」被移为观察 — **PASS（与本席 §8.15 一致；缺陷在实现 U07-001）**

独立核本席 §8.15 原文，不采信候选转述为己证。

本席原文：「T9 工作手机「沿用 staff_id」是正确合同；U07-001 实现不写是另一段已定稿项。」B1 §5 PGM-036：「COVERED（设计）T9 要求 staff_id 对应；实现不写归 U07-001」。

本轮核 T9 `:29` 原文：「公司工作手机沿用 `terminal_devices.staff_id` 对应员工，重新登记保持该对应关系。」HTML PGM L572 人/私人手机/工作手机要能对应起来；U07-001 已定稿冲突、须修是：`TerminalDeviceService` bind 不写 `staffId`，首次签名 403 `device_owner_mismatch`。处置是 **实现写入 staffId**，不是改掉 T9 这句正确合同。

席甲 CX-003 把该句与封顶/注册捆成「文档与定稿代码不一致」的技术性须修，最小处置「随 U07-001 改措辞」——若按损坏实现去改规格，会把人-机对应从设计里删掉，方向反了。候选移出 009、须修否，**与 §8.15 一致**。本段不重复立项。T9「只声明终端类型不足以打卡」（§8.16）仍同向 U01-001，维持观察。

票 **PASS**。

### 重点 (d) U13-009 观察是否写错本席 §8（第 4/9/12 条） — **PASS（未写错；无应升队被降观察项）**

独立核本席 §8 原文。

**第 4 条。** 本席原文：「首次稀疏 overlay 缺 `package.json` 导致 verifier `JSON.parse` Unexpected end of JSON input——**不计入**安全基线缺陷；补齐 package.json 后得到 count 720 mismatch 才是证据。」候选「4 首次稀疏 overlay 缺 package.json 不计」是该句前半的压缩。「不计」只覆盖缺 `package.json` 的 JSON.parse，**不**覆盖 CMD-SEC-OVL 的 730 vs 720（已入 001）。未把安全计数缺陷降观察。

**第 9 条。** 本席原文：「隔离规格 §5 旧行回填 vs 铁律 8「不为存量数据做兼容」：main 既有段落，本段 diff 未改。登记给调度，不在本段发明删除。」本轮核副本 `:253-257` 仍是「旧行回填策略（待执行）」；相对基线该文件实质 diff 只有标题与 V409，§5 未改。沿用 U01「main 既有」口径：本材料 diff 未触及 → 须修否、登记给调度。候选「9 §5 旧行回填 vs 铁律 8 为 main 既有登记给调度」未写成「本段删除回填段」，也未升本段冲突。正确。

**第 12 条。** 本席原文：「main 自身 findings-register 的 OpenAPI SHA `4e19b1a3…` 与当前 main OpenAPI SHA `308dd631…` 不一致，但 **count 仍 720**，verifier 不校验 SHA。属 main 既有漂移，不记本段须修。」本轮复算：main register `:7` = `4e19b1a3…`；main `openapi-p0.json` = `308dd631…`；count 720。候选把 11/12 压缩为「main 既有基线措辞与 SHA 漂移」：第 11 条是 formal baseline 仍写 `/docs/**` 公开（生成器措辞，main 既有），第 12 条是 SHA 漂移。压缩可拆开写，但未把 **未发布** SHA `f8de4f8c…`（本段钉死、须修在 001）与 **main** SHA 漂移混成一条须修。未写错。

**应升队而被降观察？无。** 本席 GR-001…008 全部入 001–008 且须修是。席甲 OBS-1 的重生面升入 001，与 GR-001 同向。T9:29 本席 B1 即要求不升本段 finding。OBS-5 首绑直接生效：HTML 对首绑审批沉默，本席 B1 已定为细化不是添加，维持观察合法。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (b)。工程缺陷是。发布前须修是。最小处置不发明业务，只引用既有 `docs:`/`verify:` 脚本、U10-001 编号、U05-008 重放、禁止整树覆盖。票 **PASS**。

### 002 独立裁定（冲突／须修是；同根 U05-004）

见 (a)。T1 `:14` 封顶分母锁固定工资+奖金。HTML 总薪资含补贴/绩效。提成排除与事假单算保留。随 U05-004 改「当前实现」句；现有列先取全，补贴/绩效列落地后并入；注释不得称底薪+奖金为总薪资。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U05-002）

见 (a)。同句未写被减数＝当期应发。改写为按当期请假工时占当期总工时的比例，从当期应发薪资扣减，下限为零。工时比例/已批准/零下限保留。随 U05-002 同批。票 **PASS**。

### 004 独立裁定（冲突／须修是；同根 U05-003）

见 (a)。公开自助注册无设备公钥/指纹，安全基线把该操作锁进 public 7。随 U05-003：请求带设备公钥/指纹，无指纹 4xx。安全基线/OpenAPI 在契约改完后重生（与 001 同一生成动作）。公开端口频控归 U07-004。不新开 2.2。候选处置并写 U01-001「自助注册归私人手机通道」：与 HTML BSM L1433 / PGM L578 同向，不是新业务；通道归属是 U01-001，设备指纹仍是 U05-003，不得读成只改通道、不补指纹。票 **PASS**。

### 005 独立裁定（冲突／须修是）

见 (a)。T1 `:12` 试用/停职 → 待入职、在职、试岗、停岗、离职。英文内部码不动。票 **PASS**。

### 006 独立裁定（技术性／须修是；同根 U05-006）

T1 `:15`「员工取认证身份、柜机取设备身份」。HTML PGM L582 / BSM L938 柜子自动记录、不设手工登记。76 文件无单端调用方。属把错误调用形态写进规格，不新造寄存业务。随 U05-006 改「柜机以已登记 kiosk 签名上报，员工身份由扫码载荷承载」，删除并列要求。票 **PASS**。

### 007 独立裁定（技术性／须修是）

T1 `:130`「POST /api/v1/staff/registrations … 私人手机 App 提交注册」与 `:11`「商户代提交走 `/staff/registrations`、公司员工走 `/staff/self-registrations`」相反。§4 表未增 self-registrations / locker-events / employment-status / settings。两条路径 HTML 已有；缺陷是文档两节不一致。§4 表与「当前实现」对齐，或删表只留指向 OpenAPI 一句。票 **PASS**。

### 008 独立裁定（技术性／须修是）

T1 `:179`「`MigrationVersions.latest()` 动态断言到 V066」与 `:7` V400—V409、main V410、U10-001 ≥V411 矛盾。改回动态读 `latest()`、删除「断言到 V066」。版本钉是测试卫生。票 **PASS**。

### 009 独立裁定（登记／须修否）

见 (c)(d)。双方观察归并，工程缺陷否，发布前须修否。§5 旧行回填、ISO-GAP-06、main register SHA 漂移按 U01「main 既有」口径须修否。T9:29 正确合同不在本段改。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U13-001 处置「用 main 生成器」须与「第 18 段工具同批」连读。定稿写成「以 main `445a49f4` 为底叠入第 18 段生成器后跑 `docs:data-isolation-spec` / `docs:domain-ownership-scope`」，避免读成用当前无 `file_version` 的 main 脚本把 CSV 生成回 328 表。分类/须修不变。
2. U13-001 处置格补「与 U05+U06+U07+U09+U10+U11+U12+U14+U15+U18 同一 PR」（候选汇总已有，单元格未写）。不改票。
3. U13-002 冻结判据可补 BSM-120（本席 GR-002 有；U05-004 定稿未留）。分类仍冲突。
4. U13-003 冻结判据可补 BSM-120（本席 GR-003 与 U05-002 定稿有）。分类仍冲突。
5. U13-004 同根 U01-001 是私人手机通道/终端身份，U05-003 才是设备公钥/指纹。处置已两句都有，定稿不得读成只改通道。
6. U13-009 的 OBS-1「并入 001」不得读成把安全三件重生从须修拿掉；重生面在 001。009 只保留「当前未发布 SHA 与未发布 OpenAPI 一致、证据 JSON 引用的 r1 日志不在 76 文件」这些观察。
7. 009 第 4 条「不计」只覆盖缺 `package.json` 的 JSON.parse，不覆盖 CMD-SEC-OVL 的 count mismatch。
8. 009 「11/12」定稿宜拆开：11＝formal baseline 仍写 docs 公开（main 既有生成器措辞）；12＝main register SHA `4e19b1a3…` vs 现行 OpenAPI `308dd631…`、count 仍 720。勿与未发布 `f8de4f8c…` 混淆。
9. T1 `:7` 留在 001，不并 008。008 只处理 `:179` 的 V066 钉。

全批差集无漏并。四分类与 GR-001…008 一致；席甲文档面改冲突符合 GEN-017 与同根 U05。001 叠法成立且只引用既有生成器与门禁。T9:29 移观察与 §8.15 一致。009 未写错第 4/9/12 条原意，无应升队被降观察项。逐 ID 001–009 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U13 或 main 模块 PASS。步骤仍 B2；U13 IN_PROGRESS；材料未发布，不做 B3。

## U13 B2-r2 单条复核 U13-010（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 前缀 52906 字节 + V0.02 B2-r1 追加段合计 78905 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2 单条复核 findings.md V1.74 顶部「当前 U13 追加候选 U13-010」。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17（本机可读副本 `.scratch/module-recheck-20260908/160-audit-prompt.md`，任务指定路径 `docs/audit-2026-09/160-audit-prompt.md` 本仓不存在）；runbook V0.02 §4/§7/§8；criteria-lock V0.11。
- 正本：`/home/james/complex-management`。本席实算 `HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 席甲：Claude Fable 5.1（U-FIX 唯一修复者，ledger V2.90/V2.91「U-FIX 合成修复模块开工」节）。作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账；命令、exit、哈希、条件真假均本席在 `/tmp` 纯净副本独立实跑。
- 本席 B2-r2 单条票：**PASS**（U13-010：四分类=技术性（main 既有）、工程缺陷=是、发布前须修=是、最小处置成立；添加 0、不发 2.2；不属于任一已定稿 ID）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未装依赖（只软链主仓 `node_modules` 到 `/tmp` 副本）。未碰 `complex_test*`。追加稿经 `/tmp/u13-010-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作已发布修复。

此票只判断 U13-010 一条：四分类、工程缺陷、「发布前须修」是/否、最小处置、是否只引用既有文件与门禁、有无业务发明、为何不属于已定稿 ID、模块归属。必须另有席甲本条 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 与 V0.02 B2-r1 **PASS** 原文。定稿后并入 U-FIX B3 批；U13 状态保持 REVIEWED-FAIL。

### 本条输入（完整指定文件，本席实算）

| 路径 | 版本（文件行 / 任务标签） | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/findings.md` | V1.74（任务给定且本席两次实算不变） | 682231 | `9eccbd45d887075336ded8d857b7d375de2dbc1463aa18fca23ffeb4ab0d1420` |
| `docs/audit-2026-09/ledger.md` | 任务标签 V2.90；文件版本行 V2.91。开工时 bytes/SHA 与任务给定全等；落盘前并行席甲写入后变大（见下） | 开工 526589 / 落盘前 529139 | 开工 `2b21364af2ed8342ffac8bc6f1fbfd7bb7b949fa631f4379b457a57f2adbd6ee`；落盘前 `aa87d852e28968c5a51476d7dc3287b2db7797bf1eec126bab1ae7efb1dc1975` |
| `docs/audit-2026-09/modules/U13-b1-grok-raw.md` 前缀（本段追加前） | V0.02 | 78905 | `8a5b859249f5f6e948b59b5853251fe04c1bbe4ca28705b1ba9c362f491e0eab` |
| `docs/audit-2026-09/plan/criteria-lock.md` | V0.11 | 889020 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| `docs/audit-2026-09/plan/runbook.md` | V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |

候选形态：V1.74 顶部单条 U13-010；questions 0；添加 0；无 HTML 修改申请。本文件独占锁哈希字段行仍仅文首一处，本段未新增。ledger 落盘前变大不改 findings 候选正文（V1.74 顶部 U13-010 仍在），不阻断本票。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 单条复核覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 四分类是否技术性、(b) 根因是否只 reminder_days、(c) 处置是否小于改 runbook/放宽校验器、(d) 是否已被 U13-001 覆盖、(e) 挂 U13 还是归 M419 |
| verification-before-completion | 声称前先跑 git archive / generate / verify --strict / 16 条件真假 / git log -S / package.json / workflows |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（runbook 搬家 → 生成器 :193 → productionCredentialLedgerVerifiable=false → 不含 NAS 分支 → 校验器 :106-153 四条红） |
| ponytail-review | 只评最小删除/收窄，不落地。扩读取面 < 改回 runbook 一行 < 放宽校验器 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 只重生（U13-001）能否覆盖生成器读取面缺陷？不能。见 §5。
2. `flyway_version` 钉 018 vs 证据 JSON `039` 是否必须并进本条？不必。见 §2 非阻断。
3. 改 runbook 加回 `reminder_days` 是否更小？否，与 b969b8cf 搬家意图相反。
4. 放宽校验器是否更小？否，会让已提交的 NAS 合同从门禁消失。
5. 归 M419 还是挂 U13？根因文件属 M419；本轮不重开 main 模块，挂 U13-010 作为 U13-001 执行面成立。

### §1 纯净 main 副本：生成器 + 校验器

副本：`git archive 445a49f409f0cfbe6dd616ae079be1299824c48f` → `/tmp/u13-010-grok-b2-3305493`，路径集合与任务一致（`docs` `ops` `security-scans` `tools` `package.json` `backend/pom.xml` `backend/src/main/java/com/complex/platform/auth` `backend/src/main/java/com/complex/platform/config` `backend/src/main/resources/db/migration/V018__p0_production_auth_provider.sql` `backend/src/test/java/com/complex/platform/auth`）。`ln -s /home/james/complex-management/node_modules node_modules`。未写主仓。

**已提交产物（生成前）**

```
git show 445a49f4:security-scans/formal-security-baseline-v1.md | grep -c '192.168.88.113'
2
git show 445a49f4:security-scans/formal-security-baseline-v1.md | grep -c 'restore.drill_completed'
2
git show 445a49f4:security-scans/findings-register-v1.md | grep -c '192.168.88.113'
1
git show 445a49f4:security-scans/findings-register-v1.md | grep -c 'restore.drill_completed'
1
```

`cd /tmp/u13-010-grok-b2-3305493 && node tools/verify-formal-security-baseline.mjs --strict`

- stdout：`Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.`
- **VERIFY_BEFORE_EXIT=0**

**生成**

`node tools/generate-formal-security-baseline.mjs`

```
generated security-scans/formal-security-baseline-v1.md with 720 operations and 11 findings
generated security-scans/findings-register-v1.md with 720 operations and 11 findings
GENERATE_EXIT=0
```

**生成后计数**

```
regen baseline 192.168.88.113: 0
regen baseline restore.drill_completed: 0
regen findings 192.168.88.113: 0
regen findings restore.drill_completed: 0
```

重生后 threat-model「160 运维、NAS、密钥」行落在「无恢复演练、密钥轮换台账结构和证据不足」；SEC-P0-OPS-RECOVERY-001 标题「NAS 恢复演练和生产密钥轮换台账未完成」，evidence「PRD-NAS-002 与 PRD-CRED-001 仍需实施证据」。与生成器 `:253-256` / `:387` 未触发回填一致。

**生成后校验**

`node tools/verify-formal-security-baseline.mjs --strict`

```
Formal security baseline verification failed:
- formal baseline must mention the confirmed NAS server 192.168.88.113
- formal baseline must mention restore.drill_completed when NAS restore drill evidence exists
- findings register must mention the confirmed NAS server 192.168.88.113
- findings register must mention restore.drill_completed when NAS restore drill evidence exists
VERIFY_AFTER_EXIT=1
```

四条与候选一致。main 已提交 baseline **2** 处 `192.168.88.113`（候选写「2 处」指 baseline；findings-register 另 **1** 处，非阻断）。

对照：安全基线最后一次提交 `192704dc`（2026-08-03），早于搬家提交 `b969b8cf`（2026-08-28）；`git log b969b8cf^..HEAD -- tools/generate-formal-security-baseline.mjs tools/verify-formal-security-baseline.mjs` 空。已提交三件是搬家前产物，当前输入不可重生。

### §2 `productionCredentialLedgerVerifiable` 逐项真假

生成器 `:180-196` 数组实为 **16** 项（14 条正则 + 2 条 `package.json` 脚本），不是 13。本席按源码 16 项在同一 archive 输入上打印：

```
TRUE   1 ledger ## 4.1 生产凭证明细台账模板
TRUE   2 ledger PRD-CRED-OPS-001
TRUE   3 ledger PRD-CRED-DB-001
TRUE   4 ledger PRD-CRED-OBJ-001
TRUE   5 ledger PRD-CRED-JWT-001
TRUE   6 ledger PRD-CRED-3P-001
TRUE   7 ledger PRD-CRED-OPEN-001
TRUE   8 runbook 生产服务器/运维账号
TRUE   9 runbook PostgreSQL 生产库
TRUE   10 runbook 对象存储
TRUE   11 runbook JWT/Token 签名密钥
TRUE   12 runbook 第三方供应商 API 密钥
FALSE  13 runbook reminder_days
TRUE   14 runbook rollback_step
TRUE   15 package.json verify:production-credentials
TRUE   16 package.json verify:ops160-recovery includes verify:production-credentials
productionCredentialLedgerVerifiable.every = false
true_count=15 false_count=1 total=16
```

唯一 FALSE：`:193` `/reminder_days/.test(credentialRunbookText)`，`credentialRunbookText` 只读 `docs/production-credential-rotation-runbook-v0.1.md`。候选「其余 12 项正则均命中」在「7 条 ledger + 5 条 runbook 类型名」口径上成立；漏写 `rollback_step` 与两条脚本（三者均为 TRUE），不改根因。

附加（本席多印，用于核对 :387 回填是否触发，不升新 ID）：

```
nasServerKnown = true（ledger / nas runbook / server-160 runbook / ops160 env.example 四处均含 192.168.88.113）
nasRestoreDrillEvidenceReady = false
  TRUE  event === restore.drill_completed
  TRUE  verification_result === passed
  TRUE  restore_database_dropped === true
  FALSE flyway_version === 018   （证据 JSON 实为 "039"）
  TRUE  verified_tables includes flyway_schema_history
nasRestoreDrillEvidenceFromNas = true
nasRestoreDrillEvidencePresent (file exists) = true
canonical reminder_days = true
runbook reminder_days = false
ledgerText reminder_days = true
```

`:387` 只在 `nasRestoreDrillEvidenceReady` 时回填 NAS 字面量。当前 ready=false，故 `productionCredentialLedgerVerifiable=false` 时整文件 0 处 NAS / 0 处 `restore.drill_completed`。若只把 reminder_days 读取面扩到 canonical，则 `productionCredentialLedgerVerifiable=true` 且 `nasRestoreDrillEvidenceFromNas=true`，初值分支（不依赖 ready）已写入 `192.168.88.113` 与 `restore.drill_completed`，校验器四条可绿。不必把 flyway 钉 018 并进本条。

**git log -S reminder_days**

```
git log --oneline -S reminder_days -- docs/production-credential-rotation-runbook-v0.1.md
b969b8cf refactor(scope): relocate unique coding rules
0efe4c29 chore: establish protected baseline before p2 coding
```

`b969b8cfdc5d232f2ae10d40157e13034f25fa64` 完整 subject：`refactor(scope): relocate unique coding rules`。

runbook 侧删除：

```
-| `reminder_days` | 到期提醒策略，P0 默认 `30,7,1` |
```

canonical 侧新增（现 `docs/scope-wide-coding-rule-canonical-V0.01.md:1449`）：

```
+| `reminder_days` | 到期提醒策略，P0 默认 `30,7,1` |
```

计数：parent `b5bb5c91` runbook 1 处；`b969b8cf` 与 HEAD runbook 0 处；HEAD canonical 1 处。搬家事实成立。

### §3 门禁链

`package.json`（HEAD=445a49f4，与工作区同 blob）：

- `docs:security-baseline` = `node tools/generate-formal-security-baseline.mjs`
- `verify:security-baseline` = `npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict`
- `verify:predeploy` **含** `verify:security-baseline`（脚本串第 11 段）

`.github/workflows/predeploy-gates.yml`：

- `:24` name: `verify:predeploy (governance gates)`
- `:27` `run: npm run verify:predeploy`
- `on.pull_request` / `on.push` 分支 `main`

故 U13-001 重生之后，本缺陷会把 `verify:predeploy`（因而 CI `predeploy-gates`）打红。当前 main 已提交产物仍 `--strict` 绿，只因未重生；不能据此把「发布前须修」改否。

### §4 四分类 / 工程缺陷 / 须修 / 最小处置

按 GEN-017 固定顺序：

1. **冲突**：否。三份 HTML 无「凭证 runbook 必须含 `reminder_days` 字面量」或「禁止 NAS 写入安全基线」的相反句。生成器正则与校验器要求互斥是工程漂移，不是 HTML 矛盾。
2. **细化**：否。指不出三份 HTML 某一具体条目作为「生成器只扫 runbook 的 `/reminder_days/`」的上位需求。
3. **技术性**：是。HTML 沉默；只服务于 `docs:security-baseline` 与 `verify:security-baseline --strict` 能对当前输入复现同一合同（GEN-002/013/016）。无新业务实体、枚举、状态、权限、价格、流程分支。**无业务发明。**
4. **添加**：否。不进入 2.2。

**工程缺陷=是。** 声称的生成入口不能再生产物；`--strict` 在重生后 exit 1；与已提交产物及校验器合同不一致。

**发布前须修=是。** U13-001 已定稿要求 OpenAPI 定稿后 `docs:security-baseline` 重生；重生即红；U-FIX B3 的 `verify:predeploy` 链含该闸。按 U01「本批必经门禁的 main 既有 → 须修是」口径，不得因「main HEAD 现绿」改否（对比 U07-006 那种不挡本批门禁、归旧模块的 main 既有）。

**最小处置成立，且小于两条反例：**

- 生成器把 `reminder_days` 判据读取面扩到 canonical（`credentialRunbookText` **或** `docs/scope-wide-coding-rule-canonical-V0.01.md` 任一命中即可）。
- **不改**校验器、**不改** runbook、**不改**三份 HTML。
- 随后 `docs:security-baseline` 重生（本脚本实写两件 md：`formal-security-baseline-v1.md` / `findings-register-v1.md`）并 `verify:security-baseline --strict` 绿；与 U13-001/U14-001 同一 PR。

只引用既有文件与门禁：生成器、校验器、canonical、runbook、`package.json` 既有脚本、`predeploy-gates.yml`。无业务发明。

比「改 runbook 加回一行」更小且符合 b969b8cf：该提交的意图就是把唯一规则搬到 canonical，runbook 删的正是这一行。加回等于撤回搬家。

比「放宽校验器」更小：校验器要求「证据文件存在则产物须提 NAS / `restore.drill_completed`」与已提交产物一致；放宽会让合同从门禁消失，重生后基线永久丢 NAS 段。

### §5 为何不是已定稿 ID；模块归属

**不属于 U13-001。** 定稿 U13-001 处置是：以 main 为底、迁移 ≥V411 后用生成器重生隔离规格与域归属 CSV，OpenAPI 定稿后 `docs:security-baseline` 重生三件，T1 `:7` 改实际编号，不手改自动块。它假定 main 生成器对当前输入能产出 `--strict` 绿的产物。本条是生成器读取面在 b969b8cf 后与校验器漂移；只执行 001 的「重生」会稳定四条红。U13-002…008 是 T1/T9 措辞与柜机/五态/V066；U13-009 是登记（含 main register SHA 漂移观察，不是 reminder_days）。其它 U 段 ID 不覆盖本生成器。新 ID U13-010 成立，永久不复用。

**模块归属：** 根因文件 `tools/generate-formal-security-baseline.mjs` 与 `tools/verify-formal-security-baseline.mjs` 在 `plan/modules.md` 属 **M419**（检查及生成工具，2 文件）；产物 `security-scans/formal-security-baseline-v1.md` / `findings-register-v1.md` 属 **M420**。聚合历史号 M079「安全基线…」含 M417–M420。本轮是未发布 U 段 + U-FIX，不重开已收口 main 模块。按任务允许的「无法在本轮重开则挂 U13」：**接受挂在 U13 下作为 U13-001 的执行面**，正式 ID U13-010。定稿可写「根因 M419 / 产物 M420，本轮挂 U13」。

冻结判据 GEN-002、GEN-013、GEN-016 均在 M419/M420 映射行（各 18 项 GEN-001…018）及 U13 并集 60 项内。不必 LOCK-AMEND。

### 票

| ID | 四分类 | 工程缺陷 | 发布前须修 | 本席票 |
|---|---|---|---|---|
| U13-010 | 技术性（main 既有生成器/校验器漂移） | 是 | 是 | **PASS** |

添加 0；questions 0；无 HTML 修改申请。

### 最小修正意见（非阻断，不改票）

1. 候选写「13 个条件」：源码数组 16 项。定稿可改「16 项中唯一 FALSE 为 runbook `/reminder_days/`」。分类/须修不变。
2. 定稿宜补一句：重生整文件 0 处 NAS 还叠加 `nasRestoreDrillEvidenceReady=false`（证据 JSON `flyway_version="039"` vs 生成器钉 `"018"`），故 `:387` 回填未触发。只扩 reminder_days 读取面后走 `nasRestoreDrillEvidenceFromNas` 初值分支即可 `--strict` 绿，不必并修 flyway 钉，不新开 ID。
3. 「main 已提交版本 2 处」建议写明是 baseline 2 处；findings-register 另 1 处。
4. `docs:security-baseline` 脚本只写两件 md；U13-001「三件」含 `security-baseline-latest.json`（另一路径）。本条失败面是两件 md 的 4 条校验，json 不挡本票。
5. 同 PR 集合可补 U15（yaml 与 json 同批后才重生基线）。不改票。
6. 任务标签 ledger V2.90 与文件版本行 V2.91：开工哈希已对齐任务给定；落盘前 ledger 被并行写入变 SHA。以 findings V1.74 候选正文为准。

未经双 PASS 不得把候选当定稿，更不称 U13 或 main 模块 PASS。步骤仍 B2；U13 保持 REVIEWED-FAIL；定稿后并入 U-FIX B3 批再修生成器一处读取面并重生。本席未修源。
