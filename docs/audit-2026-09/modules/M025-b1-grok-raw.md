CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B1 席乙（Grok）修复前全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B1（修复前全量；本模块首次开工，历史未审，PENDING→IN_PROGRESS 由调度已写 ledger；本席不改 ledger/findings/计划）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M025 商户档案、商户类型与商户侧门户／运行和发布脚本／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock.md V0.11；提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): 钉住破窗不能无盘续期、换钥作废旧票`）
- origin/main：`6da4e51bd98354c0789b6daa2772828f5c8b3171`（PR #465 已合并）。主工作区落后 origin 且含未推本地提交；其中 `1c59c6a8` / `173ef102`（2026-09-11）改过本脚本、未进 origin/main。审计对象＝主工作区现状文件，不是 origin 独有提交。
- 本席未读取 `modules/M025-b1-self-raw.md`，未依赖席甲发现；未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未调用会写库的接口（含登录）；未运行本脚本
- 本席步骤结论：**FAIL**（有工程缺陷；无添加项；无 HTML 修改申请；无未答 questions）

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/plugins/cache/`、`/home/james/.claude/skills/`、`/home/james/.grok/bundled/skills/`。按提示词 §6 收窄口径；本体要问人 / 派子代理 / 改代码 / 另写 §7 外产物的步骤一律作废。禁止项未调用。

开工前实读全文：`using-superpowers`、`using-arc`、`graft`、`grilling`。其余强制技能核路径存在后按收窄口径按意图执行，未把 Arc `audit` / Trail of Bits 子代理工作流跑起来。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前列技能；用户指令优先于技能 |
| using-arc | Arc plugin `skills/using-arc/SKILL.md` | 路由识别为 audit；**用户禁子代理/禁改业务/唯一可写 grok-raw**，不跑 Arc 默认派评审 |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep` / `graft ask --source` / `graft skeleton` 定位 PlatformController、MerchantProfileRequest、登录 token、SNAKE_CASE、幂等、状态/业态闭集 |
| grilling / grill-me | mattpocock `productivity/grilling/SKILL.md`、`grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废。grill-me 仅转发 grilling |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 本模块是浅客户端：脚本只应记住「调哪些接口、字段名、凭据、幂等键」；业务闭集在 PlatformService / 迁移 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 声称 vs 现行源码的反馈环＝只读命令（bash -n、python 模拟、psql SELECT）；不改代码、不加探针、不打登录 |
| systematic-debugging | superpowers | 只借读错误→追数据流（脚本→curl JSON→Controller→Service→迁移/库）；作废插桩 / 最小改码 |
| tdd / test-driven-development | 存在 | 只作测试有效性透镜；本模块无测试文件，B1 不写测试 |
| verification-before-completion | superpowers | PASS/FAIL 前实跑 sha256sum / bash -n / python 模拟 / psql SELECT / git rev-parse |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：已死的 `merchantName` 跳过分支；「接口必填」假声称 |
| writing-for-agents | mattpocock productivity | 本 raw 按步骤+完成标准组织，供 B2 归并 |
| i-have-adhd | marketplace/cache `i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；本 raw 编号清单；CLI 不回带正文（提示词适配器） |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 可选；本轮无 git 写，未装 hook |
| audit（Arc） | Arc `skills/audit/SKILL.md` | **缺口/未按本体执行**：本体要派子代理写 `docs/arc/audits/`，与双席+§7 封闭清单冲突 |
| audit-context-building / differential-review | Trail of Bits | **缺口/未按本体执行**：本体强制子代理，用户禁第三席 |
| batch-grill-me | 本机 skill 目录无独立 SKILL | **缺口**；用 grilling 自问自答代替 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，B1 不调用 |
| ponytail / TDD 落测试 | 存在 | 仅 B3；B1 禁止修复 |
| requesting-code-review / receiving-code-review | 存在 | B4；B1 不调用 |
| resolving-merge-conflicts | 存在 | 无合并 |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉，未调用 |

graft 本轮（PlatformController / MerchantRecord / createMerchant / SNAKE_CASE / pending_admission / merchant_type / 登录 access_token / IdempotencyService skeleton）约节省 ~239k tokens。

---

## 1. 范围、哈希、行数（实算，不抄期待值）

独立命令与输出：

```text
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ git rev-parse HEAD
e456ba96c84185c8d39f18f548011daac48a6466
$ git rev-parse origin/main
6da4e51bd98354c0789b6daa2772828f5c8b3171
$ git ls-files -z -- ops/160/seed-dev-bars.sh | xargs -0 cat | wc -l
55
$ wc -l -c ops/160/seed-dev-bars.sh
  55 2642 ops/160/seed-dev-bars.sh
$ sha256sum ops/160/seed-dev-bars.sh
bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044
$ bash -n ops/160/seed-dev-bars.sh ; echo exit=$?
exit=0
$ sed -n '6404p' docs/audit-2026-09/plan/criteria-lock.md | tr ',' '\n' | wc -l
79
$ sha256sum .scratch/module-recheck-20260908/160-audit-prompt.md
c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21
$ sha256sum docs/audit-2026-09/plan/runbook.md
c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f
$ sha256sum docs/audit-2026-09/plan/modules.md
8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa
```

sidecar 与本席实算锁哈希相同。LOCK-AMEND-COUNT 只读见到 1（未改 ledger）。计划写有效内容 55；现行物理行 **55**（以实算为准）。`git status --porcelain -- ops/160/seed-dev-bars.sh` 空：相对 HEAD 工作区未再改。相对 origin/main 有 6 行注释/状态差异（本地 `1c59c6a8`/`173ef102`）：注释不再立法「参股故挂娱乐平台公司」、`status` 从 `active` 改为 `pending_admission`。

| 文件 | 物理行 | SHA256 |
|---|---:|---|
| `ops/160/seed-dev-bars.sh` | 55 | `bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044` |

全文覆盖：55 行每一行均已读，不抽样。模块内无测试文件。只读追证的后端/迁移/HTML/锁**不在本模块 FILES 列表**，仅作接口与判据复现对象。

---

## 2. 全文覆盖表（脚本每一段）

| 行 | 内容 | 覆盖结论 |
|---:|---|---|
| 1 | `#!/usr/bin/env bash` | 技术性 shebang；`bash -n` 通过 |
| 2–3 | 开发环境、后台建商户接口、不替代招商/合同/准入、不写进正式迁移 | 声称；与 V354 迁移注释「10 家酒吧不在本迁移、开发用本脚本」一致。准入正路见 BSM 酒吧段 / PGM-026，本脚本自承不替代 |
| 4–6 | 为何不是迁移：家数可配 | 与 V354 L26–28 同因；非业务发明 |
| 8–9 | 默认 10 家 A98…J98；参数 25 家 | L13 `COUNT="${1:-10}"` + L34–36 `LETTERS`/`seq` 实现；i=0..9 → A..J |
| 10 | **幂等：同名已存在则跳过（按 merchant_name 查）** | **声称不成立**，见 GR-01 |
| 11 | `set -euo pipefail` | 技术性；未引号的 `$API`（L18）在默认值下可跑 |
| 13–16 | COUNT 默认 10；API 默认 `http://127.0.0.1:8080`；JSON Content-Type；LETTERS | 默认值已核。无整数校验（首次正常 `./seed-dev-bars.sh` 或 `25` 不撞） |
| 18–20 | `tok()`：`POST $API/api/v1/auth/login`，body `login_name`/`password`，读 `data.access_token` | 路径与字段与 `AuthController` `@RequestMapping("/api/v1/auth")` + `@PostMapping("/login")` + `LoginRequest.loginName` + `LoginResponse.accessToken` + 全局 SNAKE_CASE 一致。**未调用登录**（写会话） |
| 22–27 | 先 `tok admin admin-pass`（失败吞掉）；否则读 `~/.complex/complex.env` 的 `P0_ADMIN_USERNAME`/`P0_ADMIN_PASSWORD` | 凭据来源已核：本机该文件存在且含这两个键名；`/etc/complex/complex.env` 亦含同名键，但本脚本不读它（与 `complex-verify-p0.sh` 等兄弟脚本双路径不一致，本机 home 文件存在故本机 fallback 可走）。硬编码 `admin-pass` 与 `.env.example` / `application-local.yml` 开发默认同值；`ProductionAuthService` 在 prod 拒绝该弱口令。AuthService 种子组织写死 `seed-org`（见追证） |
| 28 | `Authorization: Bearer` | 与 `BearerAuthenticationFilter`「头优先」及 CSRF「Bearer 短路」一致，设备/脚本通道不需要 CSRF cookie |
| 30–31 | `ENTITY=be_ent_platform`；注释「只为接口必填」 | 主体 id 在 V354 / complex_test 存在。**「接口必填」为假**，见 GR-02。项目关联缺失见 GR-03 |
| 33–36 | 循环建 `酒吧${SUFFIX}98` | 业态码 `bar` 见创建体 |
| 37–39 | `GET /api/v1/platform/merchants?page=1&size=500`，python 按 `merchantName` 取 id | 路径/分页默认与 `PlatformController.listMerchants` 一致；**响应字段名错误**，见 GR-01 |
| 40–52 | `POST /api/v1/platform/merchants`，snake_case 体：`idempotency_key`/`business_entity_id`/`merchant_name`/`merchant_type=bar`/`contact_name`/`status=pending_admission`/`operator_note`；读 `data.id` | 请求字段名与 SNAKE_CASE + `MerchantProfileRequest` 一致；状态码在 SUBJECT_STATUSES 内＝待准入；业态在六值闭集。创建仍受项目范围与主体-项目关联约束，见 GR-03 |
| 53–55 | 打印建成 / 完成 | 无业务规则 |

未抽样：上表覆盖 L1–L55 全部物理行。

---

## 3. 接口与实现追证（只读；未打登录、未跑本脚本）

### 3.1 登录

| 项 | 脚本 | 现行实现 |
|---|---|---|
| 路径 | `POST /api/v1/auth/login` | `AuthController`（`@Profile local/dev/test`）与 `ProductionAuthController`（`@Profile prod`）均 `@RequestMapping("/api/v1/auth")` `@PostMapping("/login")` |
| 请求 | `login_name`, `password` | `AuthService.LoginRequest.loginName`（`@JsonAlias("username")`）+ `password`；SNAKE_CASE → `login_name` |
| 响应 token | `data.access_token` | `LoginResponse.accessToken` → `access_token`；包络 `ApiResponse.data` |
| 默认账号 | 先 `admin` / `admin-pass` | local：`AuthService` 仅当 `complex.smoke.admin.*` 或 `P0_ADMIN_*` 非空才种子，组织/商户写死 `seed-org` / `seed-merchant`，类型 `platform_admin`。yml **无** `complex.smoke.admin` 默认值。`application-local.yml` 的 `complex.auth.production.seed-admin` 只喂 `@Profile prod` 的 `ProductionAuthService`，且 prod 拒绝 `admin-pass` |
| CSRF | 仅 Bearer | `DoubleSubmitCsrfFilter`：有 `Authorization: Bearer` 则不校验 CSRF |

### 3.2 JSON 命名

`backend/src/main/resources/application.yml` L15–16：`spring.jackson.property-naming-strategy: SNAKE_CASE`。

`PlatformControllerTests.listingMerchantsReturnsApiResponsePageShape` 断言 `$.data.items[0].merchant_name`（不是 `merchantName`）。

### 3.3 列表与创建

`PlatformController` L59–76：

- `GET /api/v1/platform/merchants`：query `includeArchived` 默认 false、`merchantType` 可选、`page` 默认 1、`size` 默认 500；权限 `merchant.profile.view` + `requirePlatformScope`
- `POST /api/v1/platform/merchants`：body `MerchantProfileRequest`；权限 `merchant.profile.manage`；`requireIdempotencyKey`

`MerchantProfileRequest` 字段（Java camelCase ↔ JSON snake_case）：`idempotencyKey`、`operatorNote`、`evidenceAttachmentIds`、`merchantName`、`merchantType`、`businessEntityId`、`contractEntityId`、`settlementEntityId`、`contactName`、`contactPhone`、`ownerPerson`、`mallBlockCode`、面积/租金、`cooperationForm`、`status`、`projectId`。脚本未传的字段服务端可空。

`PageResponse`：`items` / `page` / `pageSize` / `total` → JSON `items`（脚本用对了）/ `page_size`。

列表实现 `listMerchantsPaged(currentProject(), …)`：`WHERE project_id = :project_id`，size 上限 500。

### 3.4 状态与业态闭集

`PlatformService`：

```
MERCHANT_BUSINESS_FORMATS = bar, catering, retail, amusement, media, other
SUBJECT_STATUSES = pending_admission, active, suspended, terminated, blacklisted
```

`requireSubjectStatus`：缺省 **active**（正常）；归档不是可设代码。`requireBusinessFormat`：业态必填。`terminated` 创建直接拒绝（须退出缓冲）。脚本显式 `pending_admission`、`bar`，不走缺省正常、不写终止。

HTML BEQR-155：待准入、正常、暂停、终止、归档、黑名单。脚本 `pending_admission`＝待准入，与「试运营系统不管」无涉。

HTML 商户业态（枚举速查 L195）+ 商户管理开篇：酒吧/餐饮/零售/游乐/演出传媒(media)/其他。`bar`＝酒吧。

迁移 `chk_merchants_business_format`（V384）与六值一致。**merchants.status 无 CHECK**；**merchant_name 无 UNIQUE 索引**。

### 3.5 `be_ent_platform`

V354 L57 种子：`be_ent_platform` / 娱乐平台公司 / `management_company` / parent `be_proj_changsha`。

complex_test SELECT：该行存在且 `active`。

`requireBusinessEntity`：存在性 + `businessEntityInProject(id, currentProject())`。`business_entity_projects` 由 V427 建表，**迁移无 INSERT**。complex_test：该表 **0 行**；`projects` 表 **0 行**（全库无 `INSERT INTO projects`）。

`currentProject()`：`actor.organizationId()` 当项目 id，再 `projectExists`；不存在则返回 null。`creationProject` → `requireCurrentProject`：current 为 null 即 `PROJECT_SCOPE_MISMATCH`。local `AuthService` 种子组织写死 `seed-org`，complex_test 无此 projects 行。

因此：在已应用现行迁移的 complex_test 上，脚本创建主路径的项目范围前提不成立（GR-03）。本席未对库 `complex` 做 SELECT（指令只许 complex_test）。

`business_entity_id` **非** `requireText`；仅非空才校验。注释「接口必填」与实现不符（GR-02）。

### 3.6 幂等重放

`saveMerchant` → `executeWrite("createMerchant", idempotencyKey, request, …)`。`IdempotencyService.DEFAULT_TTL_SECONDS = 86400`。同 scoped operation + 同 key + 同 request hash → `replay` 返回已存 `MerchantRecord`（含 `id`）。不同 body 同 key → `IDEMPOTENCY_CONFLICT`。

脚本声称的客户端跳过按 **名字**；服务端幂等按 **键**。跳过字段名错误后，24h 内重跑可能靠服务端重放仍拿到同一 `data.id`（脚本会打印「建成」而非「跳过」）；TTL 过后且无 UNIQUE(merchant_name) 会插入重名行。

### 3.7 环境文件

| 路径 | 本机存在 | 脚本是否读 | 键名（只列名） |
|---|---|---|---|
| `~/.complex/complex.env` | 是 | 是（仅 fallback） | `P0_ADMIN_USERNAME`、`P0_ADMIN_PASSWORD` |
| `/etc/complex/complex.env` | 是 | **否** | 上两键 + `COMPLEX_DATABASE_PASSWORD` |
| `ops/160/complex.env.example` | 是 | 否 | 示例键名 `P0_ADMIN_USERNAME`/`P0_ADMIN_PASSWORD` |

兄弟脚本（`complex-verify-p0.sh`、`complex-runtime.sh`）两处都读。本机 home 文件有键，本机 fallback 不因「只读 home」立刻失败。

### 3.8 可执行验证（无写）

- `bash -n` exit 0
- python3 3.12.3 存在
- 跳过字段模拟（不碰 API）：

```text
get merchantName:     （空）
get merchant_name: mch_1
```

- psql `complex_test`（口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD` 行读取，未 source、未回显）：`be_ent_platform` 1 行 active；`business_entity_projects` 0；`projects` 0；`酒吧%98` 商户 0；现存 merchants 1 行 `active/bar`；无 merchant_name 唯一索引；业态 CHECK 含 bar。

---

## 4. 第一性原理三问

### 4.1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？

答不上「HTML 某条会做不到」。三份 HTML 要求酒吧经招商→合同→准入引进，**禁止把固定家数写进发布链**的是实现约束（V354 迁移注释 + 用户「家数可配」），不是 HTML 原文要求「必须有 seed-dev-bars.sh」。

删掉脚本：HTML 商户档案/待准入/酒吧业态仍可由后台 `POST /merchants` 或招商签约自动建档实现。脚本是开发联调夹具。

四分类：先冲突——与「不单独凭空新建酒吧」字面相比，本脚本确用平台建档 API 造酒吧商户；但它自标开发环境、状态为待准入、注释写明不替代招商。HTML 同时把「建立商户档案」列为功能（BSM-028 / PGM-026）。**不按冲突删除**。指得出上位＝**细化**（建立商户档案 + 待准入 + 酒吧业态的开发数据面）。不是添加。技术性通道不走（有业务名词：商户/酒吧/待准入/经营主体）。

### 4.2 声称的行为有没有被实际执行验证过？

| 声称 | 验证 | 结果 |
|---|---|---|
| bash 可解析 | `bash -n` exit 0 | 语法成立 |
| 登录字段 `login_name` / `access_token` | 读 AuthService + application.yml SNAKE_CASE | 字段名一致；**未打登录** |
| 列表按 `merchant_name` 跳过 | 脚本 L38 写 `merchantName`；测试断言 `merchant_name`；python 模拟空匹配 | **未执行声称**（GR-01） |
| POST snake_case 创建 | 与 MerchantProfileRequest + SNAKE_CASE 一致 | 请求形状对 |
| `pending_admission` / `bar` | SUBJECT_STATUSES / FORMAT / CHECK / HTML | 词表对 |
| `be_ent_platform` 存在 | V354 + SELECT | 行存在 |
| 接口必填 business_entity_id | saveMerchant 仅非空才 requireBusinessEntity | **声称假**（GR-02） |
| 建成 N 家 | 创建依赖 projects + business_entity_projects；complex_test 皆空 | **主路径前提不成立**（GR-03） |
| 不写进 Flyway | 本文件是 sh 不是 sql | 成立 |

### 4.3 哪个前提没验证就当真？

1. 列表 JSON 有 `merchantName` —— 与全局 SNAKE_CASE 相反，已证伪。
2. `business_entity_id` 必填 —— 已证伪。
3. 当前登录主体的 `organizationId` 等于 `projects.id` 且该项目已关联 `be_ent_platform` —— complex_test 上 projects=0、关联=0，前提不成立。
4. local 进程已按 `P0_ADMIN_*` 种子出可登录管理员 —— 未打登录，未验证。
5. 服务端 24h 幂等可以代替按名跳过 —— 读到代码，但未重放调用。
6. 运行时 API 连的是有项目数据的库而不是空的 complex_test —— 未验证；指令只许 SELECT complex_test。

---

## 5. 顺从式：映射 79 项逐条

判据全文已从 `criteria-lock.md` V0.11 定义区逐条阅读（GEN-001…018、BEQR-085…096/118/146/155/160…181、BSM-020/028/081–083/111–119、PGM-015/018/026/039/052–056/066）。下表「适用」＝本脚本对该锚点有实现面或引用面；「无本面」＝脚本不含该业务对象，不整类略过。

### 5.1 GEN-001 … GEN-018

| ID | 适用? | 理由与证据 |
|---|---|---|
| GEN-001 | 适用 | 开发夹具有用途（家数可配、不进 Flyway）；死跳过分支无用途，见 GR-01 |
| GEN-002 | 适用 **未满足** | 按名跳过未执行；创建主路径在 complex_test 前提上不能完成「建成 N 家」。GR-01/GR-03 |
| GEN-003 | 适用 **未满足** | 本模块无测试；`merchantName` 与 Controller 测试的 `merchant_name` 从未被脚本侧断言。不另立「必须补测试文件」添加项；并入 GR-01 证据 |
| GEN-004 | 适用 | COUNT/API/凭据/JSON 为外部输入。COUNT 无类型校验（首次正常路径是缺省或整数，不立项）。登录/创建体未在脚本侧做模式校验，依赖服务端 |
| GEN-005 | 适用 | 走 Bearer + `merchant.profile.manage/view`；平台管理员桥 `requirePlatformScope` 放行。脚本本身不实现授权 |
| GEN-006 | 适用 **未满足** | 无 UNIQUE(merchant_name)；跳过失效后可插入重名。GR-01 影响面 |
| GEN-007 | 适用 **未满足** | 声称按名幂等；实现按错误字段，服务端按 24h 键。GR-01 |
| GEN-008 | 适用 **未满足** | 列表读 camelCase，全局 SNAKE_CASE。创建体 snake_case 正确。GR-01 |
| GEN-009 | 适用 | 创建失败 python 打 RESPONSE 前 400 字节并 exit 1；登录首选失败被 `2>/dev/null \|\| true` 吞掉后走 fallback。curl 无 `-f`，HTTP 4xx 仍进 python |
| GEN-010 | 适用 | 源码含开发默认口令字符串；prod 种子拒绝同一弱口令。本脚本自标开发。不按生产破窗项立项 |
| GEN-011 | 适用 | 服务端 createMerchant 走 AuditedOperationExecutor；脚本只 echo。无矛盾 |
| GEN-012 | 适用 | 循环上限为调用方 COUNT；列表 size=500 与 LIST_MAX_PAGE_SIZE 同。家数「二三十」不撞 500。不按极端 COUNT>500 补需求 |
| GEN-013 | 适用 | 依赖 curl/python3/API/`~/.complex/complex.env`；python3 本机存在。不读 `/etc/complex/complex.env`（兄弟脚本读）。本机 home 有键，不把「仅 /etc 有凭据」的他机场景立项 |
| GEN-014 | 无本面 | 本文件不是迁移；被 V354 注释引用但不改库模式 |
| GEN-015 | 无本面 | 无 GUI。echo 中文。内部码 `bar`/`pending_admission`/`be_ent_platform` 属系统内部标识符 |
| GEN-016 | 适用 **未满足** | L10 按 merchant_name 跳过、L30 接口必填，均不可从现行接口复现。GR-01/GR-02 |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加逐条分类；三项 finding 均细化 |
| GEN-018 | 适用 | 结论落到 HTML 原文、源码、bash -n、python 模拟、psql。未用注释/历史 PASS 当事实 |

### 5.2 BEQR（37）

| ID | 锚点 | 适用? | 理由 |
|---|---|---|---|
| BEQR-085 | 项目开放状态 | 无本面 | 游乐设施状态；脚本不写该枚举 |
| BEQR-086 | 场次状态 | 无本面 | 同上 |
| BEQR-087 | 游乐当天/每天 | 无本面 | 无营业日逻辑 |
| BEQR-088 | 容量双阈值 | 无本面 | 无容量 |
| BEQR-089 | 园区容量 | 无本面 | 同上 |
| BEQR-090 | 设备关键/辅助 | 无本面 | 无设备台账 |
| BEQR-091 | 全园故障与补偿通票 | 无本面 | 无票务补偿 |
| BEQR-092 | 安全严重度 | 无本面 | 无安全事件 |
| BEQR-093 | 巡检任务状态 | 无本面 | 无巡检 |
| BEQR-094 | 越界类型 | 无本面 | 不判经营范围越界 |
| BEQR-095 | 团体/包场 | 无本面 | 无包场 |
| BEQR-096 | 游乐增强阶段 | 无本面 | 无 P1/P2 |
| BEQR-118 | 运营营业日 | 无本面 | 不切日结/营业日 |
| BEQR-146 | 运营公司职责 | 适用（引用） | 挂 `be_ent_platform`＝娱乐平台公司；注释声明不把参股写成经营主体。HTML：娱乐平台公司管酒吧域并参股独立酒吧商户。开发挂同一主体＝夹具，不立法。通过（业务口径）；工程前提见 GR-03 |
| BEQR-155 | 商户/运营主体状态 | **适用** | 脚本 `status=pending_admission` ∈ 待准入。不写试运营。不直接 `terminated`。归档走存档端点、脚本不碰。**通过**（相对 HTML 词表）。origin/main 仍为 `active`，非本审计对象 |
| BEQR-160 | 招商线索状态 | 无本面 | 不写线索 |
| BEQR-161 | 招商意向阶段 | 无本面 | 不写意向 |
| BEQR-162 | 品牌/客户库 | 无本面 | 无品牌库 |
| BEQR-163 | 合同变更类型 | 无本面 | 无合同 |
| BEQR-164 | 退租结算状态 | 无本面 | 无退租 |
| BEQR-165 | 内部应付执行状态 | 无本面 | 无付款 |
| BEQR-166 | 合同审批链 | 无本面 | 无审批链 |
| BEQR-167 | 租金/账单类型 | 无本面 | 无账单 |
| BEQR-168 | 营业额抽成模式 | 无本面 | 无抽成 |
| BEQR-169 | 公区能源费 | 无本面 | 无能源费 |
| BEQR-170 | 铺位租赁状态 | 无本面 | 无铺位 |
| BEQR-171 | 物业工单类型 | 无本面 | 无工单 |
| BEQR-172 | 物业工单状态 | 无本面 | 同上 |
| BEQR-173 | 商户服务工单状态 | 无本面 | 同上 |
| BEQR-174 | 工单时效 | 无本面 | 同上 |
| BEQR-175 | 楼宇机电设备状态 | 无本面 | 无楼宇设备 |
| BEQR-176 | 设备维保类型 | 无本面 | 无维保 |
| BEQR-177 | 能耗表计类型 | 无本面 | 无表计 |
| BEQR-178 | 抄表状态 | 无本面 | 无抄表 |
| BEQR-179 | 能耗归集对象 | 无本面 | 无归集 |
| BEQR-180 | 楼宇巡检任务状态 | 无本面 | 无楼宇巡检 |
| BEQR-181 | 楼宇巡检类型 | 无本面 | 同上 |

### 5.3 BSM（14）

| ID | 锚点 | 适用? | 理由 |
|---|---|---|---|
| BSM-020 | 楼宇运营（入口卡） | 无本面 | 脚本不涉及机电/能耗/巡检 |
| BSM-028 | 基础管理·主要功能 | **适用** | 「建立商户档案」；六类合作方式脚本未填（服务端允许空＝尚未确定）。业态用 `bar`。不实现合同/红线七类。通过（建档细化）；工程缺陷不在本锚点词表 |
| BSM-081 | 楼宇运营 h2 | 无本面 | 同 BSM-020 |
| BSM-082 | 楼宇运营·主要功能 | 无本面 | 无台账/抄表/巡检 |
| BSM-083 | 楼宇运营·和谁有关 | 无本面 | 无连接面 |
| BSM-111 | 经营看板 | 无本面 | 无看板 |
| BSM-112 | 主要指标 | 无本面 | 无指标 |
| BSM-113 | 谁会看 | 无本面 | 无展示 |
| BSM-114 | 各类入口 | **适用（引用）** | 脚本走管理后台 API（管理层/系统管理员配置），Bearer 非商户侧/非客人端。不实现各入口产品。无冲突 |
| BSM-115 | 相互关系 | 无本面 | 不连票务/酒吧营业/供应链 |
| BSM-116 | 系统交付与上线 | **适用（对照）** | HTML：上线不是把测试那套原样搬过去。本脚本自标开发、不进 Flyway，正是把测试数据留在发布链外。通过 |
| BSM-117 | 正式上线时的安全加固 | **适用（对照）** | HTML：上线必须换掉开发默认弱口令、U 盘托管。本脚本不是生产上线脚本；首选 `admin-pass` 仅开发。不立项要求本脚本实现破窗。无本生产加固面 |
| BSM-118 | 安装包+教程 | 无本面 | 不是安装包 |
| BSM-119 | 交付·和谁有关 | 无本面 | 不留上线换密痕迹 |

商户管理段（BSM 商户管理 h2，不在本模块映射 ID 内，按指令仍核 HTML）：业态闭集含酒吧；准入口径是招商→合同→准入。脚本 `bar` + `pending_admission` + 注释不替代招商。细化，不按冲突删开发夹具。

### 5.4 PGM（10）

| ID | 锚点 | 适用? | 理由 |
|---|---|---|---|
| PGM-015 | 楼宇运营入口卡 | 无本面 | 同 BSM-020 |
| PGM-018 | 物理项目与运营公司 | **适用** | HTML：多对多，不是固定隶属；娱乐平台公司不因顶层取得商户收入；独立酒吧商户自负经营。脚本把所有开发酒吧挂同一 `be_ent_platform`，注释写明不表示生产同一经营主体、不表示参股＝经营主体。业务上不立法。工程上该 id 必须落入当前**项目**（`projects` 表，不是 `business_entities.parent`）——GR-03 |
| PGM-026 | 商户和主体·主要功能 | **适用** | 「建立商户档案、运营主体、联系人」；合作方式未填允许。招商「签约后自动转正式档案」不是本脚本路径。状态待准入≠已准入正式经营 |
| PGM-039 | 智能设备场景联动 | 无本面 | 无设备联动 |
| PGM-052 | 楼宇运营 h2 | 无本面 | 无楼宇运营 |
| PGM-053 | 楼宇机电设备 | 无本面 | 无 |
| PGM-054 | 能耗计量 | 无本面 | 无 |
| PGM-055 | 楼宇巡检 | 无本面 | 无 |
| PGM-056 | 楼宇运营·和谁有关 | 无本面 | 无 |
| PGM-066 | 已拍板规则 | 适用（窄） | 「商户侧授权范围内全开放」本脚本不实现商户侧。界面全中文：echo/商户名中文；内部码属标识符。对象带项目归属：创建路径把商户写入 `creationProject`——但当前库无项目，见 GR-03 |

79 项均已单独判定，无整类跳过。

---

## 6. 对抗式（grilling 结构，自问自答）

❓ **Q1 — 声称的按名跳过在第一次正常重跑会不会执行？**
➡️ 不会。列表 JSON 是 `merchant_name`（Controller 测试 L150；application.yml SNAKE_CASE）。脚本 L38 `m.get('merchantName')` 恒空。python 模拟：同名对象用错键得到空串，用对键得到 id。正常重跑不会打印「跳过」。

❓ **Q2 — 那服务端幂等会不会把重跑变成无害重放？**
➡️ 24h 内、同一调用方 scope、同一 `idempotency_key=seed-bar-${SUFFIX}98`、同一 request hash：会 replay，脚本误报「建成」但仍是同一 id。TTL 后或换账号：`tryReserve` 成功则 **新 UUID 再插一行**。DB 无 `merchant_name` UNIQUE。这是声称幂等的真实失败模式，不是极端并发。

❓ **Q3 — 把所有酒吧挂娱乐平台公司是否与 PGM-018 冲突？**
➡️ HTML 要求独立酒吧商户与娱乐平台公司职责分开，参股≠经营主体。脚本注释已否定立法。分类＝细化夹具，不是冲突删除。真正的工程问题是该主体未关联任何 `projects` 行。

❓ **Q4 — 首次正常 `./seed-dev-bars.sh` 在已迁移的 complex_test 上会不会建成 10 家？**
➡️ 不会（在本席允许的 SELECT 证据下）。`projects` 0 行 → `currentProject()` 对 `seed-org` 返回 null → `creationProject` 抛 `PROJECT_SCOPE_MISMATCH`。即便有项目，`business_entity_projects` 0 行 → `be_ent_platform` 触发 `PROJECT_SCOPE_MISMATCH`。脚本会在第一家 POST 的 python 处 exit 1。未打该 POST（禁写）；依据是服务端源码 + SELECT。

❓ **Q5 — `pending_admission` 是否发明了 HTML 没有的状态？**
➡️ 否。BEQR-155 待准入；`SUBJECT_STATUSES` 含 `pending_admission`。比 origin/main 的 `active`（正常、绕过准入）更贴 HTML。

❓ **Q6 — 无测试是否单独立项？**
➡️ 否。GEN-003 记未满足；缺陷入口仍是错误字段名（第一次正常对照就能复现），不另发明「必须给 sh 加 pytest」需求。

❓ **Q7 — 硬编码 admin-pass 是否要按 BSM-117 删除？**
➡️ 否。BSM-117 管生产上线换弱口令。本脚本是开发播种；prod 种子器已拒绝该口令。本机 fallback 读 home 环境文件。不为生产破窗补脚本闸。

❓ **Q8 — COUNT 非数字 / >26 / >500 要不要补需求？**
➡️ 不要。首次正常是缺省 10 或注释中的 25。那三类要额外误用才成立。

---

## 7. Findings（临时 GR；正式 ID 待 B2）

分类顺序均为：冲突？否 → 细化？是 → 不进入技术性/添加。

### GR-01 — 列表跳过读取 `merchantName`，响应字段是 `merchant_name`

- **文件:行**：`ops/160/seed-dev-bars.sh:37-39`（声称在 L10）
- **原文**：
  ```
  # 幂等:同名商户已存在则跳过(按 merchant_name 查)。
  EXISTS=$(curl -s "$API/api/v1/platform/merchants?page=1&size=500" -H "$AH" \
    | python3 -c "…print(next((m['id'] for m in items if m.get('merchantName')=='$NAME'),''))")
  ```
- **更长上下文**：L10 声称按 `merchant_name`；L37–39 实际 `.get('merchantName')`；随后 L40 在空 EXISTS 时 POST 创建。
- **正常触发 / 影响**：第一次成功创建后再次执行脚本（文档化的幂等路径）。GET 200 且 items 含 `merchant_name=酒吧A98` 时仍视为不存在，不会打印「跳过」。24h 内可能靠服务端幂等键重放；超时或不同调用方则插入重名商户（无 UNIQUE）。
- **最小拟处置**：python 改为 `m.get('merchant_name')`（可再 `or m.get('merchantName')`，非必须）。L10 声称与实现对齐。
- **四分类**：细化（上位：BSM-028 建立商户档案 / GEN-007 幂等 / GEN-008 命名一致）。非冲突。
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-002、GEN-007、GEN-008、GEN-016（证据链）；BEQR-155 词表无涉本字段名
- **2.3 依据**：类型 4 可复现命令（python 模拟输出空 vs `mch_1`）+ 类型 4 测试源码 `PlatformControllerTests` L150 `$.data.items[0].merchant_name` + `application.yml` L16 SNAKE_CASE。未用注释当事实。

### GR-02 — 注释声称 `business_entity_id`「接口必填」，服务端可空

- **文件:行**：`ops/160/seed-dev-bars.sh:30-31`
- **原文**：`# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。` 后接 `ENTITY=be_ent_platform`
- **更长上下文**：L40–48 POST 体始终带 `"business_entity_id":"$ENTITY"`。`PlatformService.saveMerchant` L1010–1013：仅 `trimToNull(request.businessEntityId()) != null` 才 `requireBusinessEntity`。`MerchantProfileRequest.businessEntityId` 无 `@NotNull`。
- **正常触发 / 影响**：读脚本的人会以为不传主体则 400；实际可不传。该假前提还把脚本绑死在必须通过「主体∈当前项目」校验（GR-03）。
- **最小拟处置**：删「只为接口必填」；改为「可选；本脚本填写已有主体 id，不表示生产经营关系」。不强制改是否传字段（传值本身是细化夹具）。
- **四分类**：细化（上位 PGM-026 建立运营主体绑定 / GEN-016 文档可验证）
- **工程缺陷是否成立**：**是**（声称不可复现）
- **冻结 ID**：GEN-002、GEN-016
- **2.3 依据**：类型 4 源码行 `PlatformService.java:1010-1013`（实现证据，不是业务依据）；注释是被审对象自身的声称。

### GR-03 — 创建主路径依赖「当前项目」和 `business_entity_projects`，complex_test 两者皆空，脚本未准备这两项

- **文件:行**：`ops/160/seed-dev-bars.sh:30-31,40-52`；追证 `PlatformService.java:405-428,933-942,1005-1013`；`JdbcPlatformRepository.java:1224-1229`；`V427__m009_project_profiles_and_associations.sql:13-17`（建关联表、无种子）；`V354` 只种子 `business_entities` 不种子 `projects`
- **原文（脚本）**：挂 `be_ent_platform` 并 POST 创建；无建项目、无 `POST /business-entities/{id}/projects`、无 `project_id` 字段。
- **更长上下文**：`requireProjectScope` 用 `actor.organizationId()` 当项目 id。local `AuthService` 种子 `seed-org`。`creationProject` 在 current 为 null 或不匹配时 `PROJECT_SCOPE_MISMATCH`。非空 `business_entity_id` 还要求 `businessEntityInProject`。
- **正常触发 / 影响**：按注释对开发库执行 `./seed-dev-bars.sh`（默认 10 家）。在本席 SELECT 的 complex_test：`projects` 0 行、`business_entity_projects` 0 行、`be_ent_platform` 存在。列表因 `project_id = null` 也筛空。第一家 POST 按服务端逻辑不能完成「建成」。脚本会把错误包络打到 stderr 并非 0 退出（L51–52），不会伪造成功，但 **L55「完成:10 家」的主路径声称不能兑现**。
- **最小拟处置**：不发明新业务对象。① 创建前若主体未关联当前项目或当前项目不存在，用中文错误退出并指出须先有项目、须关联主体（或省略 `business_entity_id`——仍须项目存在）；② 不在本模块补项目种子迁移（那是别模块 / 添加）。本席不在 B1 修。
- **四分类**：细化（上位 PGM-018 项目隔离维度、PGM-026 建档、PGM-066 对象带项目归属、GEN-002）
- **工程缺陷是否成立**：**是**（第一次按文档对已迁移测试库跑就会撞上的前提缺口；不是要两三件不巧才成立）
- **冻结 ID**：GEN-002、GEN-013、PGM-018、PGM-026
- **2.3 依据**：类型 4 psql 输出（`project_count=0`、`bep_rows=0`、`be_ent_platform` 1 行）+ 类型 4 源码。未调用创建接口。

无添加项。无 HTML 自身缺陷需铁律 4 暂停。无 LOCK-AMEND 提案。

---

## 8. 观察（不立项）

- origin/main 脚本仍 `status=active` 且注释「参股故挂娱乐平台公司」；主工作区已改 pending_admission 并撤回立法。只审现状。
- `requireIdempotencyKey` 英文 `"idempotency_key is required"` 在后端公共类，非本 FILES。
- 脚本不读 `/etc/complex/complex.env`；本机 `~/.complex/complex.env` 已有 `P0_ADMIN_*` 键。
- CSRF 对 Bearer 放行，脚本通道正确。
- 登录/创建请求字段名（snake_case）与 Jackson 策略一致；问题只在**列表响应**读取。
- 未对 `complex` 业务库 SELECT；未确认 8080 进程连哪库、是否已登录。
- `i-have-adhd` / Arc audit / batch-grill-me / ubiquitous-language / Trail of Bits 子代理：见 §0 缺口。

---

## 9. 三线收口与步骤结论

| 线 | 是否执行 | 结果 |
|---|---|---|
| 第一性原理 | 是，§4 | 脚本是开发细化夹具；三条声称中有未执行/前提不成立 |
| 顺从式 | 是，79/79 | 见 §5；GEN-002/007/008/016 与 PGM-018/026 创建前提未满足 |
| 对抗式 | 是，§6 grilling 自问自答 | 重跑跳过死；空项目库建不成；状态/业态词表本身不冲突 |

**本席 B1 结论：FAIL。**

PASS 条件不满足：存在未定稿工程缺陷 GR-01/GR-02/GR-03。无未答 questions。哈希行有效。全文 55 行已覆盖。79 判据均有适用或无本面记录。

本席不修复、不写 findings.md、不等操作员。B2 归并后再追加本文件末尾（不得写第二行 `CRITERIA-LOCK-SHA256:`）。

## M025 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **37899** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M025 FILES 1 / 锁映射 79：`BEQR-085…096/118/146/155/160…181, BSM-020/028/081–083/111–119, GEN-001…018, PGM-015/018/026/039/052–056/066`）
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `6da4e51bd98354c0789b6daa2772828f5c8b3171`（未 fetch）。B3=0。无源修复。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，**不读**席甲 raw 在封存 35922 字节之后的 B2 结论段。
- 本席 B2-r1：**PASS**（001–002 逐 ID 全 PASS；GR-03 只登记不入队 PASS；添加 0、不发 2.2、无 HTML 申请、无 questions。本席撤回 B1 对 GR-03 的入队主张）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值，LOCK-AMEND-COUNT 1。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未 fetch。未运行本脚本、未调用任何接口。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/前缀字节/候选段/两份 B1 封存前缀/脚本原文/`git show origin/main:…PlatformService.java`/`git status --short`/两态 DTO/幂等 TTL/V005 建表/ops/160 与 M020–M024 版本标记；未对任一 `complex_test*` 库做写操作，未重审模块结论。

此票只判断 findings.md V2.80 顶部「当前 M025 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占 CRITERIA-LOCK-SHA256 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M025-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 37899 | `d39f496c6d7880038327042495bc4b32187ac0d5542ab8bedf2a6e115d226337` |
| `docs/audit-2026-09/modules/M025-b1-self-raw.md` 前缀（席甲 B1 V0.01；本席只读此前缀） | V0.01 | 35922 | `7faaf51ff23a04e6dbd2d42ab66d81f102d9ba4e3ac66de4e0e35676a233545a` |
| `docs/audit-2026-09/findings.md` | V2.80 | 1070685 | `b75a04570cba9fcec40ca4ef13b222b2f45b5950f8d13b1d2d0ce574048db7da` |

席甲 raw 全文现大于 35922 bytes（本席用前 35922 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改；与 B1 同）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

对象文件（未改）：`ops/160/seed-dev-bars.sh` 55 行，2642 bytes，SHA `bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044`。无 `# 文件版本` 行。

`questions/M025*`：不存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：001 细化对技术性、24h 过期插入、版本标记是否最小；002 两态可空与措辞；GR-03 origin/main 零命中 vs 主工作区暂存 |
| grilling | 只借追问结构自查：差集有无漏并/改写、版本行是否发明功能、002 是否改传字段、GR-03 入队是否会描述未发布规则 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/`git show origin/main`/`git status --short`/python 同款表达式/V005 建表/TTL 常量/ops/160 与 M020–M024 版本行 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审模块结论 |
| codebase-design / ponytail-review | 最小是改一处键名、改半句假声称、加既有款式的版本行；不发明预检、不写未发布项目规则 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本节点对象是归并/两态 git 核源，用户点名 `git show`/`git status`；未管道 head/tail 截 graft |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…03、本席观察若干、席甲 CX-M025-001、OBS-1…5）

漏并（findings）：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–002 或只登记段。无单席独有缺陷被丢。无本批新开「否」ID。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M025-001 | GR-01 + CX-M025-001 | 细化/是/是（L38 键名改 `merchant_name` 一处；第 2 行加 `# 文件版本：V0.01`；不改接口/幂等键/不加功能） | **PASS（细化/是/是）** |
| M025-002 | GR-02（席甲 B1 漏） | 细化/是/是（只改 L30 注释前半；后半原样；不改是否传该字段） | **PASS（细化/是/是）** |
| （无新 ID） | GR-03 | 只登记不入队（挂 M009 未发布改动联动项） | **PASS（只登记；本席撤回 B1 入队）** |

冲突入队 0。细化入队 2（001–002）。技术性入队 0。观察否：双方 OBS 按候选「只登记不入队」闭合；GR-03 由 findings 降为跨模块观察。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–002（2）；「否」本批新开 ID = 0。

**整批候选：同意。**

---

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-01 + CX-001 | 001 | 同根：L10 声称按 `merchant_name` 跳过，L38 实读 `merchantName`。后端全局 SNAKE_CASE（`application.yml:16`）；`PlatformControllerTests.java` 断言 `$.data.items[0].merchant_name`。本席同款 python 对蛇形样例：`merchantName`→空，`merchant_name`→`m-1`。影响面三种：24h 内同键同体重放不重复但谎报「建成」；请求体变过（本地提交 status `active`→`pending_admission`）→ 409 首家退出；TTL 过期后重跑插入重名。席甲 B1 只写前两种，第三种采本席。未改写。本席 B1 备选 `or m.get('merchantName')` 候选不采——正确（后端形状无该键，加回退不是最小）。 |
| GR-02 | 002 | 本席独有。席甲 B1 把 L30「只为接口必填」写成「与代码一致」并 PGM-018 通过，未单列缺陷。候选采本席。两态均可空，见第三节。未改写。 |
| GR-03 | 只登记 | 本席 B1 入队；候选只登记。独立核席甲新事实成立，见第四节。原意（创建主路径在未发布项目规则下会撞上空项目/空关联）保留为跨模块观察，不是改写成「不成立」。 |

观察（无应升添加、无应升本批缺陷被降为漏并）：

- 席甲 OBS-1 L8「与 2026-07 主数据一致」无法从纳入范围复现：候选只登记。
- 席甲 OBS-2 L22 先用开发默认口令试登、已换口令环境留失败登录：候选只登记。
- 席甲 OBS-3 口令经 curl 传参短暂出现在进程列表：候选只登记。
- 席甲 OBS-4 查重只取第一页 500：候选只登记。
- 席甲 OBS-5 两个本地提交 / OPEN PR #445：与本席 origin/main 仍 `status=active`、立法式「参股故挂娱乐平台公司」观察并入只登记（本席实读 `git show origin/main:ops/160/seed-dev-bars.sh` L30 与 L46 属实）。
- 本席 `requireIdempotencyKey` 英文报错属后端公共类：候选只登记。
- 本席「脚本不读 `/etc/complex/complex.env`」「CSRF Bearer 放行」「请求字段名与 SNAKE_CASE 一致」「未对 `complex` 业务库 SELECT」「技能缺口」：方法/确认/环境观察，B1 本就不立项。候选只登记段未点名。**不构成漏并**——无应升队项。

---

### 二、001 分类、影响面、版本标记

四分类顺序（GEN-017）：冲突→细化→技术性→添加。冲突？否——三 HTML 与脚本声称并非两句不能同时成立。细化？是——脚本经后台 `POST /api/v1/platform/merchants` 做的就是业务地图「基础管理·主要功能」与治理地图「商户和主体」的「建立商户档案」（BSM `docs/business-system-map-for-operations-v0.1.html:701`；PGM `:494`）；L10「同名已存在则跳过」是该动作的下位幂等声称，键名与 SNAKE_CASE 不一致使声称不可复现。席甲 B1 判技术性（「HTML 对播种脚本沉默、无上位业务句」）——按顺序细化先于技术性，不采。分类 **细化**。工程缺陷 **是**。队列 **是**。

影响面第三种独立核源：

- `IdempotencyService.DEFAULT_TTL_SECONDS = 86_400`（24 小时）。
- `V005__p0_platform_master_data.sql:16-28` `merchants.merchant_name VARCHAR(120) NOT NULL`，仅 PK `id`；其后迁移无 `UNIQUE(merchant_name)` / `uq_merchants*`。TTL 过后同键不再命中幂等记录，L38 查重仍空 → `INSERT` 重名行。席甲 B1 漏此枝，采本席。

最小处置边界（同意候选）：

- L38 一处 `m.get('merchantName')` → `m.get('merchant_name')`。不加 `or m.get('merchantName')`。
- 不改接口、不改幂等键 `seed-bar-<后缀>98`、不改请求体、不加查重页、不加 UNIQUE。
- 文件第 2 行加 `# 文件版本：V0.01`。属最小处置，不升添加、不加功能。依据：同目录 `ops/160/merge-pr.sh:2`、`install-git-hooks.sh:2`、`propose-change.sh:2`、`complex-deploy-api.sh:2`、`complex-deploy-web.sh:2` 已是「shebang 下一行 `# 文件版本：…`」；M020 `MerchantPortalService.java`/`MerchantPortalController.java`、M021 `V428__m021_settlement_format_share_media.sql`、M022 两测试类、M023 三页及测试、M024 PRD 文首版本行，均为同批修复文件加版本标记。本脚本目前无该行，B3 在 shebang 之后**插入**新行，不得覆盖现 L2「开发环境:用后台建商户接口…」。
- 验证：同款 python 对蛇形样例修前空、修后该 id；不调用接口。

票：**PASS（细化／是／是）**。

---

### 三、002 两态可空与措辞

席甲 B1 漏。独立两态：

- `MerchantProfileRequest.businessEntityId`：origin/main 与主工作区均无 `@NotNull` / `@NotBlank`（主工作区仅多未发布的 `projectId` 字段，不改变本字段）。
- origin/main `PlatformService.saveMerchant`（`git show origin/main` `:874`）`existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId()`——创建路径把空收成 null 写入，**不**调用 `requireBusinessEntity`。origin/main 该文件对 `creationProject`／`businessEntityInProject`／`PROJECT_SCOPE_MISMATCH` **0 命中**。
- 主工作区 `:1011` `if (trimToNull(request.businessEntityId()) != null) requireBusinessEntity(...)`——仅非空才校验。空仍可创建。

「接口必填」在两态都不成立。采本席 GR-02。分类细化（注释是对「建立商户档案」接口的下位声称）。工程缺陷是。队列是。

措辞（同意候选，核现文 L30）：

现文：`# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。`

拟改整句：`# 开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。`

- 前半「挂一个已有经营主体,只为接口必填」→「给商户填一个已有经营主体 id(接口不强制该字段)」：承认脚本仍填写 `ENTITY=be_ent_platform`，同时去掉假必填。「id」与 V354 稳定键一致。
- 后半「不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。」原样保留。不采本席 B1「不表示生产经营关系」的改写。
- **不改是否传该字段**：`ENTITY=be_ent_platform` 与 POST `"business_entity_id":"$ENTITY"` 不动。
- 不写项目／`business_entity_projects`／当前项目——避免把未发布规则写进将随检查点进 origin/main 的注释。

票：**PASS（细化／是／是）**。

---

### 四、GR-03 分歧项——席甲新事实独立成立；只登记；不入队

本席 B1 把「创建主路径依赖当前项目与 `business_entity_projects`，complex_test 两者皆空」判工程缺陷并拟入队。席甲给出未发布边界。本席按点名命令独立复核，**不读**席甲 B2 结论段：

1. `git show origin/main:backend/src/main/java/com/complex/platform/governance/PlatformService.java` 对 `creationProject`／`businessEntityInProject`／`PROJECT_SCOPE_MISMATCH`：**0 命中**。
2. 主工作区同一文件 **7 命中**：`:121` `creationProject`；`:414` `PROJECT_SCOPE_MISMATCH`；`:419` `creationProject` 定义；`:939` `businessEntityInProject`；`:940` `PROJECT_SCOPE_MISMATCH`；`:1005` `creationProject`；`:1008` `PROJECT_SCOPE_MISMATCH`。
3. `git status --short`：`M  backend/src/main/java/com/complex/platform/governance/PlatformService.java`（暂存修改，工作区相对暂存干净）；`A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql`（暂存新增）。`git diff --cached --numstat`：`238  56`（+238/−56），与席甲所述一致。
4. 该批是 M009 闭环留在主工作区、检查点从未带入 origin/main 的改动。ledger 既有「V427 须先于或随同 V428 并入」登记。M025 FILES 只有本脚本，不含 `PlatformService.java` / V427。

四条理由，本席逐条独立同意：

① 触发证据取自 `complex_test`。它是单测迁移态空库（本席 B1 自记 `projects=0`、`business_entity_projects=0`），不是脚本目标。脚本面向正在运行的联调 API；其库有无项目、主体是否已关联，两席按只读约束都没有查、也不能查。不能把测试空库上的未发布规则当成「第一次按文档跑本脚本」的必然失败。

② 后端拒绝时 L48–52 把响应原文打到 stderr 并非零退出；`set -euo pipefail`；L55「完成」只在循环全部成功后打印。不存在谎报成功。GEN-009 成立。本席 B1 已写明「不会伪造成功」——入队理由只剩「主路径声称不能兑现」，而该声称的阻碍来自未发布规则。

③ 项目范围／主体-项目关联来自 M009 尚未发布的代码。现在把「须先有项目、须关联主体」写进本脚本注释，检查点合入 origin/main 后注释描述的是 origin/main 里不存在的规则，反而造出 GEN-016。

④ 本席 B1 拟处置①「创建前预检并中文报错」要新增接口调用与分支，超出最小处置，且构成对未发布规则的实现。拟处置②「不在本模块补项目种子」本席 B1 已排除。

**不入队。** 不写「origin/main 尚无该规则时的入队最小处置」——在不得描述未发布规则、不得新增预检功能的约束下，本脚本在 origin/main 上没有可执行修复面。本席撤回 B1 入队主张。

登记口径（同意候选）：跨模块观察，挂 M009 未发布改动的联动项：该批改动发布时，须同时核本脚本在「无项目／主体未关联项目」的新库上的前置说明。不在本模块 B3 改脚本。

票：**PASS（只登记不入队）**。

---

### 五、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M025*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML。001/002 不发明业务规则（不改是否建档、不改主体关系、不改状态/业态闭集）。

只登记不入队（无应升添加、无应升本批缺陷）：GR-03（M009 未发布联动）；L8 2026-07 主数据句；L22 默认口令试登；口令进进程列表；查重第一页 500；origin/main `status=active` 与立法式注释 / PR #445；`requireIdempotencyKey` 英文公共类。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 1 文件：`ops/160/seed-dev-bars.sh`。

- 001：L38 `merchantName` → `merchant_name`（一处）；shebang 下一行插入 `# 文件版本：V0.01`（不得覆盖现 L2 头注）。不改接口、不改幂等键、不加 UNIQUE、不加 `or merchantName`。
- 002：只改 L30 前半为「开发播种给商户填一个已有经营主体 id(接口不强制该字段)」；后半「不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。」原样；`ENTITY` 与 POST 字段不动。
- GR-03：不改脚本。

禁止：改 `PlatformService` / V427 / 三 HTML / 锁 / 台账 / 其它 ops 脚本；把未发布项目规则写进注释；新增创建前预检或额外接口调用；改是否传 `business_entity_id`；发 2.2。

### 结论

全批差集 findings 无漏并、无原意改写。001–002 细化／是／是。001 采本席细化（席甲 B1 技术性不采）；影响面含 24h 幂等过期后插入重名；版本行属最小处置（ops/160 与 M020–M024 同款）。002 席甲漏，两态字段均可空，措辞只改前半、后半原样、不改是否传字段。GR-03 席甲新事实独立成立（origin/main 零命中、主工作区 7 命中、`M ` +238/−56 暂存、V427 `A ` 暂存新增）；只登记、挂 M009 未发布联动；本席撤回入队。添加 0。HTML 申请 0。questions 0。写入集合＝本模块 1 文件。逐 ID：001 PASS、002 PASS、GR-03 只登记 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M025 IN_PROGRESS；B3=0。
