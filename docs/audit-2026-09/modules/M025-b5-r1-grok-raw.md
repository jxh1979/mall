CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（修复后整模块重审，不只看 diff；B3 累计 1）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M025 商户档案、商户类型与商户侧门户／运行和发布脚本／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock.md V0.11；提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main（本地 ref，未 fetch）：`6da4e51bd98354c0789b6daa2772828f5c8b3171`
- after tree（调度给定）：`0395a637691b73a32a1eb1e81642cfc0313edbb5`；该 tree 中 `ops/160/seed-dev-bars.sh` blob `24dc5aeeebfc7f38e1c4587ea0032061ce7a8d2c` 的内容 SHA256 与工作区文件相同
- findings 台账版本（只读）：V2.82；M025-001、M025-002 队列「否（已修）」；GR-03 只登记（B2-r1 双席定稿）
- 本席未打开 `modules/M025-b5-r1-self-raw.md`。全仓检索 `admin-pass` 时该路径作为命中文件名出现过一次，未跟进打开、未采信其正文或结论。可读旧 B1/B2/B4 raw 与 findings/ledger 作背景，结论依据只落 2.3 四类。
- 未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未运行本脚本；未调用任何接口（含登录）；未建脚本日志或临时产物
- 本席步骤结论：**PASS**

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/plugins/cache/`、`/home/james/.claude/skills/`、`/home/james/.grok/`。按提示词 §6 收窄口径；本体要问人 / 派子代理 / 改代码 / 另写 §7 外产物的步骤一律作废。禁止项未调用。

开工前实读全文或本节点所需节：`using-superpowers`、`using-arc`、`graft`、`grilling`、`verification-before-completion`、`systematic-debugging`、`diagnosing-bugs`、`codebase-design`、`ponytail-review`、`grill-me`（仅转发 grilling）、`i-have-adhd`（存在；与「79 项逐条 + 完整 raw」冲突时任务优先，不压缩判据表）。Arc `audit` 与 Trail of Bits `audit-context-building` 本体要派子代理，用户禁第三席，**按意图不执行**其派发。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | superpowers `using-superpowers/SKILL.md` | 开工前列技能；用户指令优先 |
| using-arc | Arc `using-arc/SKILL.md` | 识别为审计；用户禁子代理/禁改业务/唯一可写 grok-raw，不跑 Arc 默认派评审 |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位 login、merchants、DTO、SNAKE_CASE、TTL、status/bar、be_ent_platform |
| grilling / grill-me | mattpocock `productivity/grilling`、`grill-me` | 对抗线自问自答；「等用户 / 派子代理」作废 |
| verification-before-completion | superpowers | PASS 前实跑 bash -n、m025_selfcheck.py、sha256、psql SELECT |
| systematic-debugging | superpowers | 只借读错误/追数据流；不改代码、不加诊断埋点 |
| diagnosing-bugs | mattpocock engineering | 禁 curl 登录，用源码+自检+complex_test SELECT 作反馈环 |
| codebase-design | mattpocock engineering | 本模块接口极浅（bash 调已有 HTTP），无新缝；不建议加深 |
| ponytail-review | ponytail 4.9.0 | 找可删；56 行脚本无可删项（`Lean already`） |
| ponytail | 同目录 | B5 不修复，不调用本体改码 |
| tdd / test-driven-development | 均存在 | B5 禁止修复，不落测试 |
| receiving-code-review | 存在 | 本席是外审产出方，不收自己的票 |
| requesting-code-review | 存在 | 只派本席，无第三席 |
| writing-for-agents | 存在 | raw 分节供席甲合取读 |
| writing-plans / brainstorming | 存在 | 阶段 A 技能，本节点不启用 |
| resolving-merge-conflicts | 存在 | 无合并冲突，不适用 |
| git-guardrails-claude-code | 存在（可选） | 本席无 git 写 |
| i-have-adhd | `i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；完整 79 表是答案本身，不按「列表≤5」砍 |
| batch-grill-me / ubiquitous-language | 未找到独立 SKILL.md | **缺口**，不得冒充已调用 |
| ~~dispatching-parallel-agents / subagent-driven-development / using-git-worktrees / executing-plans / finishing-a-development-branch / research / code-review / domain-modeling / ponytail-audit~~ | 存在但提示词划掉 | 未调用 |

graft 本回合 tokens saved 合计约 210,974（10 次 grep：login / merchants / MerchantProfileRequest / access_token / login_name / SNAKE_CASE / be_ent_platform / DEFAULT_TTL_SECONDS / pending_admission / `"bar"`）。

---

## 1. 范围与实算

| 项 | 实算 |
|---|---|
| 判据锁独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（`sha256sum docs/audit-2026-09/plan/criteria-lock.md`；与 sidecar 全等，非抄基准） |
| 锁版本 | V0.11 |
| M025 映射 ID 数 | 79（定义区逐条抽出，unique=79） |
| 模块文件 | 1：`ops/160/seed-dev-bars.sh`（scope.txt 第 2852 行） |
| 工作区 SHA256 | `b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331` |
| 行数 | `wc -l`=56（计划写有效内容行 55，差 1 为 B3 所加版本行；以实算为准） |
| 字节 | 2685 |
| 非空行 | 51 |
| 模式 | 775（可执行位保留；after tree `100755`） |
| `bash -n` | exit 0 |
| `/home/james/audit-tmp/m025_selfcheck.py` | exit 0；`EXISTS=[mch_1]`；无「接口必填」；文件版本行存在 |
| after tree blob 内容 SHA256 | 与工作区相同 |

本席按现行工作区全文审该脚本，并只读追现行后端与迁移；origin/main 仅作「是否已发布」对照，不把未跟踪/暂存的 M009 当已发布规则。

---

## 2. 文件覆盖（56 行）

| 行 | 内容摘要 | 本席动作 |
|---|---|---|
| 1 | `#!/usr/bin/env bash` | 覆盖 |
| 2 | `# 文件版本：V0.01` | 覆盖；001 最小处置落地 |
| 3–4 | 开发播种、不替代招商/合同/准入、不进正式迁移 | 覆盖；与 BSM-028/PGM-026 对照 |
| 6–7 | 为何不是 Flyway；家数可配 | 覆盖 |
| 8–10 | 默认 10（A98…J98）、参数 25、按 `merchant_name` 跳过 | 覆盖；幂等声称复验 |
| 11 | `set -euo pipefail` | 覆盖 |
| 14–17 | `COUNT` 默认 10、`API` 默认 `http://127.0.0.1:8080`、JSON 头、字母表 | 覆盖 |
| 19–21 | `tok()`：`POST /api/v1/auth/login`，`login_name`/`password`，读 `data.access_token` | 覆盖；对照 Auth DTO |
| 23–29 | 先 `admin`/`admin-pass`，失败再读 `~/.complex/complex.env` 的 `P0_ADMIN_*`，Bearer | 覆盖 |
| 31–32 | `ENTITY=be_ent_platform` 与「接口不强制」注释 | 覆盖；002 落地 |
| 34–55 | 循环命名、列表查重、POST 建档、失败打 RESPONSE 并 exit 1、完成行 | 覆盖 |

无第二文件。无 OVERSIZE 抽样。

---

## 3. 接口 / 字段 / 注释追证（现行源码 + 迁移 + 类型 4 命令）

### 3.1 登录 `POST ${API}/api/v1/auth/login`

| 声称 | 现行事实 | 证据类型 |
|---|---|---|
| 路径 | `AuthController` `@Profile({"local","dev","test"})` `@RequestMapping("/api/v1/auth")` `@PostMapping("/login")`；prod 另有 `ProductionAuthController` 同路径。脚本默认 127.0.0.1:8080 + 开发注释，对得上 local/dev | 4 读源 |
| 请求 `login_name`,`password` | `AuthService.LoginRequest`：`loginName` + `@JsonAlias("username")`，`password`；全局 SNAKE_CASE → JSON `login_name` | 4 |
| 响应 `data.access_token` | `ApiResponse.data` + `LoginResponse.accessToken` → `access_token`（`application.yml:16` `property-naming-strategy: SNAKE_CASE`） | 4 |
| 先试 `admin`/`admin-pass` | `application-local.yml:16-17` 本地默认；`ProductionAuthService` `@Profile("prod")` 拒绝该弱口令。脚本不写生产 | 4 |
| fallback `~/.complex/complex.env` 的 `P0_ADMIN_USERNAME`/`P0_ADMIN_PASSWORD` | 本机该文件存在且含这两个**键名**（值未写入本 raw）。脚本不读 `/etc/complex/complex.env` | 4 |
| Bearer | `BearerAuthenticationFilter`：`Authorization: Bearer ` 优先于 Cookie；无 Cookie 则 CSRF 双提交不触发 | 4 |

未调用登录。`tok()` 第一次 `2>/dev/null || true`，第二次无吞错：登录最终失败则整脚本非 0。

### 3.2 列表 `GET /api/v1/platform/merchants?page=1&size=500`

| 项 | 现行事实 |
|---|---|
| 路径 | `PlatformController.listMerchants` `@GetMapping("/merchants")` |
| 查询参数 | 显式名 `page` 默认 1、`size` 默认 500、`includeArchived` 默认 false、`merchantType` 可选。脚本只传 page/size，与默认一致 |
| 页大小上限 | `PlatformService.LIST_MAX_PAGE_SIZE=500`；`JdbcPlatformRepository.listMerchantsPaged` `safeSize = min(size,500)`。脚本 size=500 不被截断 |
| 权限 | `merchant.profile.view` + `requirePlatformScope()` |
| 响应 | `ApiResponse<PageResponse<MerchantRecord>>`；`PageResponse.items`；`MerchantRecord.merchantName` → JSON `merchant_name`；测试 `PlatformControllerTests.java:150` 断言 `$.data.items[0].merchant_name` |
| 工作区未发布差异 | 现行 `listMerchantsPaged(currentProject(), …)` 且 `WHERE project_id = :project_id`。origin/main 的 list **无** project 参数（`git grep origin/main` 无 `creationProject`/`businessEntityInProject`/`PROJECT_SCOPE_MISMATCH`） |

脚本查重键现为 `m.get('merchant_name')`（L39）。`merchantName` 在本文件 **0 命中**。

### 3.3 创建 `POST /api/v1/platform/merchants`

请求体字段（脚本 L41–48）对 DTO（SNAKE_CASE）：

| JSON 字段 | Java | 校验 |
|---|---|---|
| `idempotency_key` | `idempotencyKey` | 控制器 `requireIdempotencyKey`（空则英文 `"idempotency_key is required"`，属公共类，非本脚本发明） |
| `business_entity_id` | `businessEntityId` | **无** `@NotNull`；`saveMerchant` 仅 `trimToNull(...) != null` 才 `requireBusinessEntity`。origin/main `:874` 同样 `trimToNull` 可空 |
| `merchant_name` | `merchantName` | `requireText(..., "merchant_name")` 必填 |
| `merchant_type` | `merchantType` | `requireBusinessFormat` 闭集 `bar,catering,retail,amusement,media,other`；脚本 `bar` ∈ 闭集；DB `chk_merchants_business_format` 同词 |
| `contact_name` | `contactName` | 可空；脚本传 `"$NAME 负责人"` |
| `status` | `status` | `SUBJECT_STATUSES` 含 `pending_admission`；缺省 `active`。脚本显式待准入 |
| `operator_note` | `operatorNote` | 不写入 MerchantRecord；进入幂等请求指纹 |

未传：`cooperation_form`（空＝尚未确定，服务端允许）、`project_id`、电话/面积等。响应读 `data.id` 与 `MerchantRecord.id` 一致。解析失败：stderr 写 `RESPONSE:` 前 400 字 + `SystemExit(1)`；`pipefail` 下不打印 L56「完成」。

幂等：`IdempotencyService.DEFAULT_TTL_SECONDS = 86_400`；同键不同哈希 → `IDEMPOTENCY_CONFLICT`。脚本在 POST 前按名跳过，TTL 不再是顺序重跑的主护栏。

### 3.4 迁移 / complex_test（仅 SELECT）

- `be_ent_platform`：`V354__business_entity_master_data.sql:57` 种子「娱乐平台公司」。complex_test：`COUNT(*)=1`
- `merchants.merchant_name`：`V005` 仅 `NOT NULL`，**无 UNIQUE**。complex_test `pg_indexes`/`pg_constraint` 无名称唯一约束（主键只在 `id`）
- `project_id` / `business_entity_projects`：只出现在暂存 `V427__m009_project_profiles_and_associations.sql`（`git status` `A `；`origin/main` 无此文件）。complex_test 已有该表/列（Flyway 吃到工作区文件），但 `projects` 行数 0、`business_entity_projects` 行数 0、`酒吧%98` 商户 0。这与 B1 同一类证据，**不是新发布**
- origin/main `requireBusinessEntity` 只查主体存在，不查项目关联；`saveMerchant` **不**调用 `creationProject`

### 3.5 注释声称

| 原句 | 判定 |
|---|---|
| L3 开发环境播 N 家酒吧商户供联调 | 与默认 API/账号/待准入一致。通过 |
| L4 不替代招商、合同、准入审批；不写正式迁移 | 脚本不调招商/合同 API、不改 sql。通过 |
| L6–7 家数可配、不进 Flyway | `COUNT="${1:-10}"`。通过 |
| L9 默认 10 家 A98…J98 | `seq 0 9` + `LETTERS` → A–J。类型 4：`COUNT=10; seq 0 $((COUNT-1))` 输出 0–9。通过 |
| L9 「与 2026-07 主数据一致」 | 纳入范围可复现：`docs/business-decision-log-v0.1.md`（scope 第 2257 行）与 `docs/terminology-canonical-v1.0.md`（2805 行）写明酒吧A98–J98 共 10 家。类型 4 grep。B2 曾登记「无法从纳入范围复现」；本席核到可复现，**不升缺陷** |
| L10 `./seed-dev-bars.sh 25` → 25 家 | `seq 0 24` 共 25 项，类型 4 `wc -l`=25。i=0..24 全字母 A–Y，不触及 i≥26 数字后缀。通过 |
| L11 幂等按 `merchant_name` 查 | L39 现读该键；自检蛇形样例命中 `mch_1`。通过 |
| L31 接口不强制该字段；不表示生产同一经营主体；不表示参股=经营主体 | 与 DTO/服务端可空一致；后半保留。002 落地。通过 |

---

## 4. 第一性原理三问

**Q1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

删掉本脚本，BSM-028 / PGM-026「建立商户档案」的**产品写入面仍在** `PlatformController.createMerchant`。本文件不是那条需求的唯一承载，是开发联调夹具：把可配家数的酒吧档案播进运行中的 API，避免把家数写进 Flyway。答不上「删了 HTML 做不到」≠该删。四分类：冲突？否（自标开发、待准入、明文不替代招商）。细化？是——上位原文：

- `docs/business-system-map-for-operations-v0.1.html:701`：「建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。」
- `docs/platform-governance-map-for-operations-v0.1.html:494`：「建立商户档案、运营主体、负责人、第一联系人和第二联系人。」
- `docs/business-enum-quick-reference-v0.1.html:195` 商户业态含「酒吧」；`:238` 状态含「待准入」。

指得出上位＝细化，不进技术性/添加。不为夹具补招商全链或六类合作方式（GEN-001）。

**Q2 它声称的行为有没有被实际执行验证过？**

禁跑脚本、禁调接口。本席用：`bash -n` exit 0；自检脚本把**脚本内原样**查重表达式喂 SNAKE_CASE 样例 → `EXISTS=[mch_1]`；`rg merchantName` 0 命中；源码对照登录/列表/创建字段；complex_test 只读索引/种子。类型 2 的「对 8080 实跑播种」按指令豁免，改用类型 4。声称与现行源码+自检一致。

**Q3 它依赖的前提，哪一条是没验证过就当真的？**

仍未验证（且本席被禁止验证）的：运行中的 `complex` 库是否已有项目、主体是否已关联项目、8080 是否已起、env 口令现值。这些是运行环境前提，不是本脚本新发明的业务规则。工作区 `creationProject` / `businessEntityInProject` 仍只在暂存 M009，origin/main 0 命中——与 B2 定稿的 GR-03 只登记同一事实，无新证据。脚本在后端拒绝时不伪造成功（见对抗 Q7/Q8）。

---

## 5. 顺从式：79 项逐条

判据全文以锁定义区为准。本面＝本脚本实现或注释是否触及该锚点。

### 5.1 GEN-001 … GEN-018

| ID | 本面 | 记录 |
|---|---|---|
| GEN-001 | 适用 | 开发夹具有可验证用途（家数可配、不进发布链）。无死代码。通过 |
| GEN-002 | 适用 | 入口可 `bash -n`；主路径字段与后端一致；错误路径 `SystemExit(1)` 不打印完成。001 的键名错误已不在。通过 |
| GEN-003 | 适用（无模块内测试） | 本模块无测试文件；不以「缺测试文件」单独立项（不为夹具加测试框架）。关键查重用仓库外只读自检，能失败（修前 RED / 修后 GREEN）。通过 |
| GEN-004 | 适用 | COUNT 在算术前受 `set -u`/`set -e` 约束；非数字 exit 1。API/口令来自 env 或默认。通过（不另加校验分支） |
| GEN-005 | 适用（调用方） | 写操作走服务端 `merchant.profile.manage` + 平台范围；脚本不伪造权限字段。通过 |
| GEN-006 | 适用 | 名称无 UNIQUE 仍为真；顺序重跑现靠查重跳过，不再依赖「插失败」。部分成功（前几家建成后中途 非0）是种子脚本+`set -e` 的可见停机，完成行不出现。不把「加 UNIQUE」当本模块新需求。通过 |
| GEN-007 | 适用 | 声称按名跳过，实现已读 `merchant_name`。TTL 24h 仍在服务端，但跳过在 POST 前。通过 |
| GEN-008 | 适用 | 请求/响应蛇形与全局 Jackson、Controller 测试、DTO 一致。通过 |
| GEN-009 | 适用 | 见对抗：拒绝时打 RESPONSE 并非 0；跳过打「跳过」不打「建成」。通过 |
| GEN-010 | 适用 | 口令进 curl 参数会短暂出现在本机进程列表——B2 只登记，无新证据。脚本不 echo 口令、不写生产。不立项 |
| GEN-011 | 适用 | 每家 echo 建成/跳过；失败有 RESPONSE 前缀。通过 |
| GEN-012 | 适用 | 循环上限=COUNT；列表 size=500=服务端上限。默认 10 / 文档 25 远小于 500。通过 |
| GEN-013 | 适用 | 依赖 bash/curl/python3（160 有 python3，自检已跑）。`API`/`P0_*` 有默认或 fallback。通过 |
| GEN-014 | 无本面 | 脚本不写迁移 |
| GEN-015 | 无本面 | 无 UI |
| GEN-016 | 适用 | 001/002 的假声称已改。L9 主数据句现可从纳入范围文档复现。版本行可从文件头复现。通过 |
| GEN-017 | 适用 | 模块整体细化；无添加；见 §4。通过 |
| GEN-018 | 适用 | 本 raw 依据为 HTML 原文、源码、命令输出。通过 |

### 5.2 BEQR

| ID | 名称 | 本面 | 记录 |
|---|---|---|---|
| BEQR-085 | 项目开放状态 | 无本面 | 脚本不写游乐开放状态 |
| BEQR-086 | 场次状态 | 无本面 | 同 |
| BEQR-087 | 游乐当天/每天 | 无本面 | 同 |
| BEQR-088 | 容量双阈值 | 无本面 | 同 |
| BEQR-089 | 园区容量 | 无本面 | 同 |
| BEQR-090 | 设备关键/辅助 | 无本面 | 同 |
| BEQR-091 | 全园故障与补偿通票 | 无本面 | 同 |
| BEQR-092 | 安全严重度 | 无本面 | 同 |
| BEQR-093 | 巡检任务状态 | 无本面 | 同 |
| BEQR-094 | 越界类型 | 无本面 | 同 |
| BEQR-095 | 团体/包场 | 无本面 | 同 |
| BEQR-096 | 游乐增强阶段 | 无本面 | 同 |
| BEQR-118 | 运营营业日 | 无本面 | 不归集钱账 |
| BEQR-146 | 运营公司职责 | 适用 | 挂 `be_ent_platform`＝娱乐平台公司（V354）；注释声明不把参股写成经营主体、不表示各酒吧生产同一主体。HTML：娱乐平台公司管酒吧域并参股独立酒吧商户。夹具不立法。通过 |
| BEQR-155 | 商户/运营主体状态 | 适用 | 脚本 `pending_admission` ∈ 服务端闭集，对应「待准入」；不写「试运营」。不把状态打成已准入从而绕过准入。通过 |
| BEQR-160 | 招商线索状态 | 无本面 | 不实施招商前链 |
| BEQR-161 | 招商意向阶段 | 无本面 | 同 |
| BEQR-162 | 品牌/客户库类型 | 无本面 | 同 |
| BEQR-163 | 合同变更类型 | 无本面 | 同 |
| BEQR-164 | 退租结算状态 | 无本面 | 同 |
| BEQR-165 | 内部应付执行状态 | 无本面 | 同 |
| BEQR-166 | 合同审批链 | 无本面 | 同 |
| BEQR-167 | 租金/账单类型 | 无本面 | 同 |
| BEQR-168 | 营业额抽成模式 | 无本面 | 同 |
| BEQR-169 | 公区能源费 | 无本面 | 同 |
| BEQR-170 | 铺位租赁状态 | 无本面 | 同 |
| BEQR-171 | 物业工单类型 | 无本面 | 同 |
| BEQR-172 | 物业工单状态 | 无本面 | 同 |
| BEQR-173 | 商户服务工单状态 | 无本面 | 同 |
| BEQR-174 | 工单时效 | 无本面 | 同 |
| BEQR-175 | 楼宇机电设备状态 | 无本面 | 同 |
| BEQR-176 | 设备维保类型 | 无本面 | 同 |
| BEQR-177 | 能耗表计类型 | 无本面 | 同 |
| BEQR-178 | 抄表状态 | 无本面 | 同 |
| BEQR-179 | 能耗归集对象 | 无本面 | 同 |
| BEQR-180 | 楼宇巡检任务状态 | 无本面 | 同 |
| BEQR-181 | 楼宇巡检类型 | 无本面 | 同 |

### 5.3 BSM

| ID | 名称 | 本面 | 记录 |
|---|---|---|---|
| BSM-020 | 楼宇运营（入口卡） | 无本面 | 不管机电/能耗/楼宇巡检 |
| BSM-028 | 基础管理·主要功能 | 适用 | 「建立商户档案」；六类合作方式未填（服务端允许空）。业态 `bar`。不实现合同/红线七类。开发夹具细化。通过 |
| BSM-081 | 楼宇运营专章 | 无本面 | 同 BSM-020 |
| BSM-082 | 楼宇·主要功能 | 无本面 | 同 |
| BSM-083 | 楼宇·和谁有关 | 无本面 | 同 |
| BSM-111 | 经营看板 | 无本面 | 不实现看板 |
| BSM-112 | 主要指标 | 无本面 | 同 |
| BSM-113 | 谁会看 | 无本面 | 同 |
| BSM-114 | 各类入口 | 适用（窄） | 脚本不是表中任一入口；用管理后台 API 播种。不把商户侧做成平台全局。通过 |
| BSM-115 | 相互关系 | 适用（窄） | 「基础管理连所有业务」——夹具只造待准入档案，不声称打通票务/酒吧。通过 |
| BSM-116 | 系统交付与上线 | 适用（对照） | HTML：上线不是把测试原样搬去。本脚本自标开发联调，不是生产上线包。通过 |
| BSM-117 | 正式上线时的安全加固 | 适用（对照） | HTML：生产须换掉开发弱口令。脚本先试 `admin-pass` 仅开发路径；prod 种子拒绝该值。B2 对「先试弱口令留失败登录」只登记。不立项要本脚本实现 U 盘破窗。通过 |
| BSM-118 | 安装包+教程 | 无本面 | 本文件不是安装包 |
| BSM-119 | 交付·和谁有关 | 无本面 | 不实施上线换密留痕 |

### 5.4 PGM

| ID | 名称 | 本面 | 记录 |
|---|---|---|---|
| PGM-015 | 楼宇运营入口卡 | 无本面 | 同 BSM-020 |
| PGM-018 | 物理项目与运营公司 | 适用 | HTML：多对多，不是固定隶属；独立酒吧自负经营。脚本把开发酒吧挂同一 `be_ent_platform`，注释写明不表示生产同一主体、不表示参股=经营主体。业务不立法。工程上「主体须落入当前项目」仍是未发布 M009，见 GR-03 只登记。通过 |
| PGM-026 | 商户和主体·主要功能 | 适用 | 「建立商户档案…」；合作方式未填允许。招商「签约后自动转正式档案」不是本脚本路径。状态待准入≠已准入经营。通过 |
| PGM-039 | 智能设备场景联动 | 无本面 | 不定义设备动作 |
| PGM-052 | 楼宇运营专章 | 无本面 | 同 |
| PGM-053 | 楼宇机电设备 | 无本面 | 同 |
| PGM-054 | 能耗计量 | 无本面 | 同 |
| PGM-055 | 楼宇巡检 | 无本面 | 同 |
| PGM-056 | 楼宇·和谁有关 | 无本面 | 同 |
| PGM-066 | 已拍板规则 | 适用（窄） | 「商户侧授权范围内全开放」本脚本不实现商户侧。界面全中文：echo/商户名中文；内部码 `bar`/`pending_admission`/`be_ent_platform` 属系统内部标识符例外。通过 |

79/79 均有适用或无本面记录。不新增判据。

---

## 6. 对抗式（grilling 结构，自问自答）

事实自己读，不等操作员、不派子代理。

❓ **Q1 空库第一次跑**

➡️ 跳过不命中 → POST。origin/main：主体可空、有值则只查存在；`be_ent_platform` 由 V354 种子；`bar`/`pending_admission` 合法。工作区未发布路径会在 `creationProject`/`businessEntityInProject` 上 403——同一 GR-03，无新证据，不改判。脚本若被拒：L48–53 打 RESPONSE 并 exit 1，无「完成」。

❓ **Q2 原样第二次跑**

➡️ GET 按 `merchant_name` 命中 → `跳过(已存在)` → continue，不 POST。自检：蛇形样例 `EXISTS=[mch_1]`。不把跳过写成建成。L56 仍打印「完成:$COUNT 家」表示循环处理完请求家数，逐行已区分跳过/建成，不构成伪造成功。

❓ **Q3 隔天幂等记录过期后再跑**

➡️ TTL=86400 只约束 POST 重放。过期后若仍按名跳过，根本不 POST，不会插入重名。001 修前：错键 → 跳过永不命中 → 过期后第二张同名行（表无名称 UNIQUE）。修后该窗口关掉。complex_test 仍无名称 UNIQUE，但顺序重跑不再走到插入。

❓ **Q4 家数 10 改 25**

➡️ 已有 A–J 跳过；K–Y 新建。`seq` 25 项全字母。不改幂等键格式。通过。

❓ **Q5 在旧版已播过的环境上跑现版**

➡️ 旧版用错键所以当时靠 24h 幂等或直接插入；库里仍是 `merchant_name=酒吧A98` 这种蛇形列。现版列表读 `merchant_name` → 跳过。不更新已有行的 status（旧版若曾写 `active`，现版不会改它）。脚本未声称会纠正旧状态。通过。

❓ **Q6 家数给非数字**

➡️ 类型 4：`set -euo pipefail; COUNT=abc; echo $((COUNT-1))` → `abc: unbound variable`，exit 1。发生在任何 curl 之前，无「完成」、无接口调用。不为非法 argv 加新用法说明（铁律 6）。

❓ **Q7 后端拒绝时脚本输出什么**

➡️ python `except` 写 `RESPONSE: `+原文[:400]，`SystemExit(1)`；`pipefail`+`set -e` 使脚本非 0；L56 不可达。类型 4 读 L48–53 控制流（禁实调接口）。

❓ **Q8 是否谎报成功**

➡️ 「建成」只在解析到 `data.id` 之后。跳过走另一 echo。拒绝非 0。第二次跑不会把跳过说成建成。通过。

其它边界（不立项）：COUNT=0 时 GNU `seq 0 -1` 空循环，完成行写 0 家，自洽；i≥26 的数字后缀缺口不在文档的 10/25 路径上；并发双进程仍可能重名（无 UNIQUE）——要两三件同时发生，不修。

ponytail-review：无可删。`Lean already. Ship.` net: -0。

---

## 7. M025-001 / M025-002 回归（不把「队列否」当成已全对）

**001 原缺陷：** L10 声称按 `merchant_name` 跳过，实现读 `merchantName`。

| 检查 | 结果 |
|---|---|
| 现行 L39 | `m.get('merchant_name')` |
| 文件内 `merchantName` | 0 命中 |
| 自检（脚本内原表达式 + 后端形状样例） | `EXISTS=[mch_1]`，problems=0，exit 0 |
| 版本行 | L2 `# 文件版本：V0.01` |
| 未越界 | 未改接口、未改幂等键、未加预检/新功能 |

回归：**无**。原三种影响面（24h 内谎报建成 / 同键不同体 409 / TTL 后重名插入）在顺序重跑下被查重截断。

**002 原缺陷：** 注释「只为接口必填」，服务端可空。

| 检查 | 结果 |
|---|---|
| 现行 L31 | `开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。` 与定稿最小处置逐字一致 |
| 「接口必填」 | 本文件 0 命中 |
| DTO | `businessEntityId` 无必填注解 |
| 主工作区 | `PlatformService.java:1011` 仅非空才校验 |
| origin/main | `:874` `trimToNull` 可空 |
| 仍传该字段 | 定稿明确不改是否传 |

回归：**无**。

---

## 8. GR-03 — 无新证据，不改判

B2-r1 双席：只登记，挂 M009 未发布改动联动项。本席 B5 复核：

| 事实 | 本席独立核 |
|---|---|
| origin/main `creationProject` / `businessEntityInProject` / `PROJECT_SCOPE_MISMATCH` | **0 命中** |
| 工作区 `PlatformService.java` | `git status` 仍为暂存修改 `M `；`creationProject` L419、`businessEntityInProject` L939、创建路径 L1005 |
| `V427__m009_project_profiles_and_associations.sql` | 仍为暂存新增 `A `；origin/main 无此文件 |
| complex_test | `projects` 0 行；`business_entity_projects` 0 行（表因工作区迁移存在） |
| 脚本被拒时是否谎报成功 | 否（§6 Q7/Q8） |

以上与 B1/B2 同一类证据，**不是**「规则已进 origin/main」的新证据。不把未发布规则写进本脚本注释，不加预检接口。维持只登记。

---

## 9. 问题清单

审完全文后统一给出。本席临时编号若为空，即无新工程缺陷。

| 编号 | 文件:行 / 原句 | 正常触发 | 四分类（冲突→细化→技术性→添加） | 已有判据 | 证据类型 | 工程缺陷？ |
|---|---|---|---|---|---|---|
| （无） | — | — | — | — | — | 无新项 |

不把下列事项升为 B5 新 finding（B2 已只登记或第一次正常跑不撞上）：先试开发弱口令、口令进进程列表、查重只取第一页 500、`requireIdempotencyKey` 英文公共类、名称无 UNIQUE、未发布项目范围。

无添加项。无三 HTML 自身缺陷需上报暂停。无 questions。

---

## 10. 结论

- 覆盖：本模块 1 文件 56 行全文
- 79 项：逐项适用或无本面
- 三线：第一性 / 顺从 / 对抗均已跑完
- 001、002：修复落地，无回归
- GR-03：无新证据，维持只登记
- 新工程缺陷：0
- 未答 questions：0
- 哈希行：本文件第一行独立实算

**本席 B5-r1：PASS**

本票不是模块收口（须双 PASS 且闸 1 四值全等，由调度写 ledger/收口文件）。本席不改 ledger/findings。
