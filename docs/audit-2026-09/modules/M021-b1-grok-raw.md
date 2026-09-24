CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B1 席乙（Grok）修复前全量审计

- 报告版本：V0.01（本模块本席首次开工，文件不存在故创建；历史未审）
- 席位：乙 / Grok 4.6 / reasoning-effort xhigh
- 阶段：B1（PENDING→IN_PROGRESS 由调度席写 ledger；本席只读业务与计划，只写本文件）
- 模块：M021 — 商户档案、商户类型与商户侧门户／数据库迁移／第1段（`plan/modules.md` V0.04）
- 控制合同：`160-audit-prompt.md` V0.17（路径 `.scratch/module-recheck-20260908/160-audit-prompt.md`；本席实算 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）
- 执行说明：`docs/audit-2026-09/plan/runbook.md` V0.02
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；映射 `M021 → [GEN-001…GEN-018]`（定义区全文已逐条读；未扩 HTML 锚点判据、未扩别模块）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`e456ba96`）
- `origin/main`：`a60ba6608764e2de12f7208e4d33a0e259b0ca68`（`a60ba660`；M020 检查点 PR #461 已合并）
- 本席独立实算 `criteria-lock.md` SHA256（命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`）：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同行：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席核对，非代填）
- 互盲：未读 `modules/M021-b1-self-raw.md`；未派子代理/MCP/第三席；未改计划/findings/ledger；未 fetch/commit/push/merge/deploy
- 范围外明示：工作区另有 M009 遗留迁移 `V427__m009_project_profiles_and_associations.sql`（不属本模块；见观察）
- 结论：**FAIL**（有未定稿工程缺陷；见 GR-M021-B1-01、GR-M021-B1-02）

---

## 0. 强制技能自检与节点应用（缺口如实）

本环境实际核到并按节点应用：

| 技能 | 路径核到 | 本轮节点 | 缺口 |
|---|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户专项指令优先于技能默认 | 无 |
| using-arc | Arc plugin `skills/using-arc/SKILL.md` | 识别 Arc `audit`/`review` 默认会派子代理、写 `docs/arc/audits/` | **缺口**：用户禁止子代理与写非指定产物，故不执行 Arc 全量机械构建/派评审代理/落 Arc 报告。按意图在本席内做迁移/数据完整性视角 |
| graft | `/home/james/.claude/skills/graft/SKILL.md` | `graft grep MERCHANT_BUSINESS_FORMATS`；`graft ask` 商户门户 fail-closed | 无 |
| grilling | mattpocock `skills/productivity/grilling/SKILL.md` | 对抗线用 grilling 结构**自问自答**（用户明示，不等操作员） | 技能原文是访谈用户；本轮按操作员收窄为自问自答 |
| grill-me | 同包 `skills/productivity/grill-me/SKILL.md`（正文仅 “Run a /grilling session”，`disable-model-invocation: true`） | 视为 grilling 入口，不另开访谈 | 无独立可执行器 |
| batch-grill-me | 本环境 `skills/productivity/` 仅有 grilling/grill-me，**无** `batch-grill-me/` | — | **缺口**：runbook 写 Codex 机有可选 `batch-grill-me`；本席未核到可调用实现，未冒充已调用 |
| ponytail | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | 只读审计；用于「不为极端场景补需求 / 最小拟处置」 | 技能主场景是编码；本轮无代码修改 |
| verification-before-completion | superpowers 同名 | 哈希、行数、psql、单测均本轮实跑后再下结论 | 无 |
| audit（Arc） | Arc `skills/audit/SKILL.md` | 见 using-arc | **缺口**：未跑构建/lint/全仓测试/secrets 扫描，未写 `docs/arc/audits/` |
| audit-context-building | Trail of Bits 技能 | 五文件假设/保证写在本 raw，不另落 `audit-context/` | **缺口**：禁止子代理，未派 `function-analyzer` |
| differential-review | Trail of Bits 技能 | 风险优先：权限种子、词表封口、收入归属；本席自做对抗 | **缺口**：未派 `adversarial-modeler` |
| code-review（知识工作） | 已读维度表 | 安全/正确性/一致性用于迁移 | 无 PR URL；按文件审 |
| 160 §6 划掉的 research | — | 未用 | 按合同不派 background agent、不另写研究文件 |

未使用 MCP。未建脚本/临时报告/日志。`complex_test` 只跑 SELECT。

---

## 1. 范围与全文覆盖（不抽样）

`plan/modules.md` V0.04 计划有效内容行 103。本席按提示词命令实算：

```bash
git ls-files -z -- \
  backend/src/main/resources/db/migration/V147__merchants_owner_person.sql \
  backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql \
  backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql \
  backend/src/main/resources/db/migration/V348__merchant_business_format.sql \
  backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql \
  | xargs -0 cat | wc -l
# 输出：103
```

五文件均全文阅读，无抽样。

| 文件 | wc -l | SHA256（本席 sha256sum） | 覆盖 |
|---|---:|---|---|
| `backend/src/main/resources/db/migration/V147__merchants_owner_person.sql` | 6 | `70a00f5d8a1936fac2e8bdf54d61150a5e30257a2e80deaff5dbd9049bb6734a` | 全文 |
| `backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql` | 9 | `2b162b399a1f84aa419f90977dbe8c29947dd006c66aa8f40f76d74f4489d6ff` | 全文 |
| `backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql` | 18 | `b082220f5bde8aba00f3ac590c4604badb1d660c641fae7d8a9961f517214e81` | 全文 |
| `backend/src/main/resources/db/migration/V348__merchant_business_format.sql` | 33 | `b01046851bb04ac5de83fba558ca052220f415f80e561fb5eace2888b142df4e` | 全文 |
| `backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql` | 37 | `36a8fc281957253926c07be557b3ab6e960a06f941312af14f6732422e588e2f` | 全文 |
| **合计** | **103** | 与计划有效行一致 | |

依赖对照（只读、不纳入本模块队列除非本模块文件自身不完整）：V197、V198、V251、V354、V366、V375。现行词表只读：`PlatformService.MERCHANT_BUSINESS_FORMATS`、`LeasingService.autoCreateMerchantForSign`、`TicketDistributionSettlementService`。

---

## 2. 三 HTML 逐字核（迁移注释点名句）

### 2.1 治理地图：商户档案 / 负责人 / 联系人

V147 L1 注释声称：`治理 HTML platform-governance-map :429「建立商户档案、运营主体、负责人、第一联系人和第二联系人」`。

**现行 HTML 该句不在 :429。** `docs/platform-governance-map-for-operations-v0.1.html:429` 是「独立酒吧商户」职责段（夜班值班经理为夜间现场最高负责人等），不是建档字段句。

该句现行位置 `:494`，在 `section#merchant` / `h3`「主要功能」下：

```
建立商户档案、运营主体、负责人、第一联系人和第二联系人。
```

同节 lead `:489`：「商户管理不是只录一个名字。它要记录商户能经营什么、在哪经营、谁负责、联系方式、合作方式和后续系统开放范围。」

列存在性与 HTML 建档要求对齐；注释行号陈旧。见观察 OBS-M021-B1-01。已应用迁移不可改写注释。

### 2.2 业务地图：商户管理段业态句

V348 L2–4 声称真源为业务地图「§商户管理 开篇」五值（酒吧、餐饮、零售、游乐、其他）及「业态是通票分账…招商录入必填…不另设「娱乐」业态」。

现行 `docs/business-system-map-for-operations-v0.1.html:1308–1309` `section#merchant` / `h2`「商户管理：商户在授权范围内完整使用自己的系统」lead **逐字**：

```
每个商户必须归入一个业态：酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media，归传媒公司）、其他。业态用于确定经营域、主管公司和域内结算规则；只有跨域组合通票按收费权益价格拆分域份额，酒吧通票、纯游乐通票等域内通票不因商户业态再次做域间拆分。
```

V348 引文与现行原文不一致（缺「演出传媒 / media」；其余分账表述亦非逐字）。席位陈旧事实对照后续 **V384** 再裁：现行 `chk_merchants_business_format` 已是六值（见 §4）。注释陈旧见观察 OBS-M021-B1-02。

### 2.3 枚举速查「商户业态」行

`docs/business-enum-quick-reference-v0.1.html:195` `td.k`「商户业态」**逐字**：

```
酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。每个商户必须归入一种业态，业态用于确定经营域、主管公司和域内结算规则：酒吧归娱乐平台公司，游乐归游乐公司，演出传媒归传媒公司，餐饮与零售归城市项目公司（中央餐饮供货部分仍由中央餐饮公司审批）；选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账。只有跨域组合通票按收费权益价格拆分域份额，酒吧通票、纯游乐通票等域内通票不得仅因商户业态再次做域间拆分。
```

V384 将 `media` 纳入 merchants / leasing_leads CHECK，与本行枚举码一致。「其他须指定主管公司」不在本五文件 CHECK 内（跨模块观察）。

### 2.4 演出归传媒公司

V384 L4–6 声称：

- 枚举速查「到期未核销款」含「演出归传媒公司」
- 业务地图「收入归属:哪类收入归哪个业态主体」含「演出票 → 传媒公司」

现行 `business-enum-quick-reference-v0.1.html:48`「到期未核销款」含（摘与注释对应的归属子句，原文更长）：

```
余额按票种经营域归属（见本表「域归属对照」条）：酒吧相关归娱乐平台公司（此为总规则在酒吧票上的适用），纯游乐归游乐公司，演出归传媒公司，餐饮与零售归城市项目公司；跨域组合通票按既定域份额归属。
```

「演出归传媒公司」逐字在原文中。注释为缩写，未把「餐饮与零售归城市项目公司」写入，但不与原文冲突。

现行 `business-system-map-for-operations-v0.1.html:1678` `h3`「收入归属：哪类收入归哪个业态主体」；`:1690` 行：

```
演出票 | 传媒公司
```

注释箭头「演出票 → 传媒公司」是表行压缩，语义与原文一致。

同文件 `:431`（治理地图）传媒公司职责亦写演出票不向娱乐平台公司支付门票综合服务费，与枚举速查门票综合服务费条一致；本模块不扩审该条。

---

## 3. 只读命令与可执行产物（类型 4 / 类型 2）

密码：只读 `/etc/complex/complex.env` 中 `COMPLEX_DATABASE_PASSWORD=` 行，未 source 该文件。连接：`psql -h 127.0.0.1 -U complex -d complex_test`。全程 SELECT / 目录只读，无写库。

### 3.1 flyway_schema_history（本模块五版本及声明依赖）

| version | script | checksum | success | installed_on |
|---|---|---|---|---|
| 147 | V147__merchants_owner_person.sql | 235932490 | t | 2026-09-09 06:45:51 |
| 197 | V197__terminal_master_switches.sql | 505337431 | t | 2026-09-09 06:45:52 |
| 198 | V198__terminal_web_module_switches.sql | 843266487 | t | 2026-09-09 06:45:52 |
| 199 | V199__merchant_portal_zone_module_switch.sql | -1183753096 | t | 2026-09-09 06:45:52 |
| 200 | V200__merchant_portal_zone_permissions.sql | 1214979897 | t | 2026-09-09 06:45:52 |
| 251 | V251__permission_names_i18n_zh.sql | 909338479 | t | 2026-09-09 06:45:52 |
| 348 | V348__merchant_business_format.sql | 448719403 | t | 2026-09-09 06:45:53 |
| 354 | V354__business_entity_master_data.sql | -61418191 | t | 2026-09-09 06:45:53 |
| 366 | V366__settlement_bill_format_shares.sql | 1936730037 | t | 2026-09-09 06:45:53 |
| 375 | V375__settlement_pool_undistributed_and_unverified_attribution.sql | 397205326 | t | 2026-09-09 06:45:53 |
| 384 | V384__merchant_business_format_media.sql | -663687440 | t | 2026-09-09 06:45:53 |

顺序 147 < 197 < 198 < 199 < 200 < 251 < 348 < 354 < 366 < 375 < 384，与注释依赖方向一致。已应用迁移 checksum 已钉死，**不可改写**这五份 SQL。

`flyway_schema_history` 最大 version=427（含 `V427__m009_project_profiles_and_associations.sql` success=t）。V427 不属本模块。

### 3.2 列类型

`merchants.owner_person`：`character varying(120)` NULL，与 V147 声明一致。  
`merchants.merchant_type`：`varchar(32)` NOT NULL。  
`merchants.contact_name`：`varchar(120)` NULL；`secondary_contact_name`：`varchar(120)` NULL（V147 注释所称第一/第二联系人列存在）。  
`leasing_leads.intended_business_type`：`varchar(64)` NULL。

### 3.3 CHECK 定义（现行事实，非注释）

```
chk_merchants_business_format =
  CHECK (merchant_type IN ('bar','catering','retail','amusement','media','other'))

chk_leasing_leads_business_format =
  CHECK (intended_business_type IS NULL OR intended_business_type IN
         ('bar','catering','retail','amusement','media','other'))

chk_sbfs_format =
  CHECK (business_format IN ('bar','catering','retail','amusement','other'))
  -- 无 media
```

旁证：`SELECT 'media' = ANY (ARRAY['bar','catering','retail','amusement','other']::text[])` → `f`；六值数组 → `t`。

`idx_merchants_business_format` 存在（V348 ③）。

### 3.4 开关行

```
tcs_mp_mod_merchant | merchant_portal | terminal.enabled | module | merchant | t
  | 商户专区·我的账单/工单/对账/合同/费用通知
tcs_term_merchant_portal | merchant_portal | terminal.enabled | global | global | t
  | 商户门户总开关
```

`merchant_portal` 终端仅上述两行。`ON CONFLICT (id)` 的 PK 存在：`terminal_capability_switches_pkey PRIMARY KEY (id)`。另有 `UNIQUE (terminal_type, capability_code, scope_type, scope_id)`。

### 3.5 权限行（V251 中文化后现行名）

```
merchant.portal.fee_recipient.manage | 管理商户端自有费用通知接收人 | merchant_portal | medium | requires_approval=f | active
merchant.portal.settlement.dispute   | 发起商户端自有未结结算争议   | merchant_portal | medium | f | active
merchant.portal.settlement.view      | 查看商户端自有结算           | merchant_portal | medium | f | active
```

`role_permissions` 持有者 **仅** `role_platform_admin`（角色名现行「平台管理员」）三行。`roles` 出厂十行：platform_admin / ops_manager / facility_manager / 审计内外 / 外包四类 / staff_self。**无商户角色。**

### 3.6 归属种子

```
rsa_performance_ticket_default | project_id=NULL | performance_ticket | be_media | entity | active | is_draft=f | updated_by=migration:V384
be_media | 传媒公司 | management_company | active
```

与 V375 `WHERE NOT EXISTS (stream + project_id IS NULL + active + is_draft=false)` 范式一致；V354 已建 `be_media`。

### 3.7 词表对照（现行代码，只读）

- `PlatformService.MERCHANT_BUSINESS_FORMATS` L46–47：`bar, catering, retail, amusement, media, other`（六值，与 V384 CHECK 一致）。
- `LeasingService.autoCreateMerchantForSign` L285–287：带入 `intendedBusinessType`，空则 `'other'`；**注释仍写「同五值词表」**（代码走 `createMerchant` → 六值校验）。跨模块观察。
- `TicketDistributionSettlementService` L73–90：`FORMAT_MEDIA="media"` → `performance_ticket`；`settleMonth` L359–368 按 `share.format()`（来自 `merchants.merchant_type`）`upsertFormatShare`。

### 3.8 类型 2：fail-closed 单测（不写 complex_test）

```
mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests#callerWithoutMerchantScopeIsRejectedAndAudited test
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

`MerchantPortalServiceTests` 为 mock 单测。测试名 `callerWithoutMerchantScopeIsRejectedAndAudited` 断言无商户归属调用 `listMyContracts` / `listMySettlements` → `MERCHANT_SCOPE_REQUIRED`。

---

## 4. 第一性原理线（逐文件三问）

### 4.1 V147 `merchants.owner_person`

1. **为何必须存在？** 删掉该列，治理地图 `:494`「建立商户档案…负责人、第一联系人和第二联系人」无法把负责人与联系人分列存储。V005 仅有 `contact_name/contact_phone`；V042 补第二联系人。有业务上位。
2. **声称是否执行？** `ADD COLUMN IF NOT EXISTS owner_person VARCHAR(120)`。psql 列类型/可空与声称一致。Flyway V147 success。可空：HTML 未写库级 NOT NULL；联系人列同样可空，不另发明必填。
3. **未验证前提？** 注释行锚 `:429` 与现行 HTML 不符（见 OBS-01）。`JdbcPlatformRepository` 已读写该列（别模块，行为消费证据，不扩审）。

### 4.2 V199 商户专区模块开关

1. **为何必须存在？** 业务地图 `:1322` 商户专区自助五项需要可关的菜单组；无显式 module 行则 `TerminalSwitchGuard` 文档口径为「无行=默认开」，运营无法在矩阵关掉「商户」组。有上位。
2. **声称是否执行？** 行已在库：`scope_type=module, scope_id=merchant, enabled=t`。`apps/web/src/admin/adminRoutes.ts` 商户专区路由 `group: "merchant"`，与 V198「scope_id = 前端菜单组 key」同范式。V197 `tcs_term_merchant_portal` 总开关行存在。`TerminalMasterSwitchInterceptor` / `GateRequestContext` 读 `X-Module-Code` 为 module scope。注释「关闭即硬拒」由闸实现（别模块），本行是闸的数据。
3. **未验证前提？** 未在本席对拦截器做请求级集成（属别模块）；本模块对象是开关行，行与范式已用 SELECT 核到。

### 4.3 V200 商户自助权限点

1. **为何必须存在？** 业务地图 `:1322` 我的对账/异议留言/费用通知接收人；无权限码则服务端无法按主体授权（GEN-005）。权限点本身有上位。`requires_approval=FALSE` 对齐注释「商户自助动作不设审批」。
2. **声称是否执行？** 三权限行存在；V251 后中文名已落地。`ON CONFLICT (permission_code) DO NOTHING` + `role_permissions` PK `(role_id, permission_code)` 使 Flyway 一次应用可完成。
3. **未验证前提？** L10「平台管理员默认可监督商户自助层」把 `role_platform_admin` 全授写成监督通路。现行 `MerchantPortalService` 对无商户归属（含平台管理员）fail-closed；单测已跑。该前提不成立。见 **GR-M021-B1-01**。出厂无商户角色见观察 OBS-03。

### 4.4 V348 业态五值封口

1. **为何必须存在？** 枚举速查「商户业态」+ 业务地图商户管理 lead：每个商户必须归入一个业态；业态驱动分账。无 CHECK 则直连 SQL 可写入任意词。有上位。把 `merchant_type` 定为权威字段、不新建列，避免双词表，符合 GEN-001。
2. **声称是否执行？** Flyway V348 success；随后 **V384 替换** CHECK 为六值。现行库是六值，不是本文件正文五值。UPDATE 旧词到 `other` 为一次性归一；当前 `merchants` 仅 `bar`×1，`leasing_leads` 空。
3. **未验证前提？** 注释把当时五值写成「真源 HTML 原文」——现行 HTML 已是六值。对照 V384 后，**运行时封口已矫**；注释不可改写。招商线索可空与 HTML「招商录入商户时必填」（商户档案，非线索）不冲突。

### 4.5 V384 补 media + 演出票归属种子

1. **为何必须存在？** 现行 HTML 业态含演出传媒 `media`；「演出归传媒公司」「演出票 | 传媒公司」。仅五值会把演出提供方打成 `other`，分账/未核销款不能进 `performance_ticket`。有上位。种子按 V375 范式，主体 `be_media` 由 V354 先建。
2. **声称是否执行？** merchants/leasing_leads CHECK 已含 media；`rsa_performance_ticket_default → be_media` 存在；`PlatformService` 六值一致。
3. **未验证前提？** 注释「词表仍由数据库封口」「业态错＝分账份额拆错」。V366 `chk_sbfs_format` **仍五值无 media**，而结算引擎按 `merchant_type` 写入份额行。该前提对份额表不成立。见 **GR-M021-B1-02**。

---

## 5. 顺从式：GEN-001…018 逐项适用证据

锁映射仅 18 条通用工程判据。每条读定义全文后对照本五文件。不引用未映射的 BEQR/BSM/PGM ID 作为 finding 冻结 ID（HTML 原文仅作 2.3 类型 1 证据）。

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 | 适用 | V147/V199/V200/V348/V384 均能指到 HTML 建档/专区/业态/演出归属。V348 复用 `merchant_type` 不另建列。V200 对 `role_platform_admin` 的三项授权在 fail-closed 下对无商户归属管理员不可用，必要性在 GR-01 审查 |
| GEN-002 | 适用 | Flyway 五版本 success=t；psql 列/CHECK/行与入口声明一致。V200 注释的「可监督」主路径与服务实际不一致（GR-01）。V384 声称分账封口与份额表 CHECK 不一致（GR-02） |
| GEN-003 | 本模块无测试文件（测试在 M022，不扩审） | SQL 模块按 runbook 用目标库解析/SELECT 代替强制测试文件。另跑 mock 单测一条作 fail-closed 行为证据，非本模块产物 |
| GEN-004 | 弱适用 | 迁移无用户输入。CHECK 在库端约束业态词。V199/V200 无动态拼接 |
| GEN-005 | 适用 | V200 建权限点 + `requires_approval=false`。授权只播 `role_platform_admin`。服务端 `requirePermission` + `requireMerchantScope` 默认拒绝无商户归属。注释「监督」与最小权限/fail-closed 冲突（GR-01）。出厂无商户角色 → 商户主体默认无这三项（观察，不发明商户角色=添加） |
| GEN-006 | 适用 | V348/V384 CHECK；V384 种子 WHERE NOT EXISTS；V366 份额 CHECK 拒 media 会让含传媒业态的月结 INSERT 失败、事务回滚（GR-02）。V147 可空不破坏既有行 |
| GEN-007 | 适用 | V147 IF NOT EXISTS；V199/V200 ON CONFLICT DO NOTHING；V384 DROP IF EXISTS + WHERE NOT EXISTS。V348 ADD CONSTRAINT 无 IF NOT EXISTS，但 Flyway 版本只跑一次；首次正常应用不重入 |
| GEN-008 | 适用 | 六值：HTML / merchants CHECK / leasing_leads CHECK / `MERCHANT_BUSINESS_FORMATS` 一致。`chk_sbfs_format` 仍五值（GR-02）。权限英文名已被 V251 改为中文，现行库与 V200 字面不一致但属后续迁移裁过的陈旧正文 |
| GEN-009 | 适用 | 迁移失败会停 Flyway（success 列均为 t）。V384 注释 fail-loud：无归属种子则演出未核销款拒绝落账；种子已在。份额 CHECK 拒 media 是响亮失败而非静默错账，但仍阻断正常月结 |
| GEN-010 | 适用 | 纯 DDL/DML 种子，无密钥。权限码无注入点。V200 全授不构成越权，因门户层 fail-closed（单测） |
| GEN-011 | 弱适用 | 种子 `updated_by='migration:V384'`。开关/权限无独立审计表；应用层审计在别模块 |
| GEN-012 | 弱适用 | V348 业态索引。INSERT 单行。无无界循环 |
| GEN-013 | 适用 | Flyway 版本链完整；complex_test 已应用到含 V384。不依赖未声明本机状态即可 SELECT 核到 |
| GEN-014 | **核心适用** | 顺序确定且已应用。破坏性：V348 UPDATE 旧词→other（开发库可接受，注释已写明）。V384 未改已应用文件，用前向 DROP/ADD CHECK。缺陷最小处置只能是**新的前向迁移**或登记。GR-02 即前向迁移候选 |
| GEN-015 | 不适用 | 无界面。权限展示名中文化在 V251（依赖成立，不在本五文件） |
| GEN-016 | 适用 | V147 `:429` 行锚错误；V348 HTML 引文非现行原文；V200 L10「可监督」与现行服务不符（GR-01）。V384 对到期未核销款/收入归属的压缩引文与原文不矛盾 |
| GEN-017 | 适用 | 本席 findings 按冲突→细化→技术性→添加。未把业务发明写成技术性。未因 HTML 沉默删除列/权限点 |
| GEN-018 | 适用 | 结论落到 HTML 原文、psql 输出、mvn 单测输出、Flyway 官方一次版本应用语义。不以注释或历史模块结论为据。M020-007 转入项已独立复核 |

---

## 6. 对抗式（grilling 结构自问自答）

❓ **Q1 — V200 把三项自助权限授给平台管理员，是否等于「可监督」？**  
现行 `MerchantPortalService.requireMerchantScope`：无 `currentMerchantScope` 即 `MERCHANT_SCOPE_REQUIRED`。单测 `callerWithoutMerchantScopeIsRejectedAndAudited` 绿。前端 `accountForms: ["merchant"]` 对非商户账号隐藏专区页。费用通知控制器虽对持有 `merchant.billing.manage` 的平台口径走另一分支，但那不是 V200 这三项自助码。  
➡️ 注释「默认可监督商户自助层」不成立。全授范式在本域被 fail-closed 打断。立项 GR-01。

❓ **Q2 — 出厂没有商户角色，是否要在本模块发明一个商户角色？**  
HTML 未写必须有名为「商户」的出厂角色；授权模型是 `user_roles` 运营授予。发明角色=添加。  
➡️ 只观察（OBS-03），不立项补角色。

❓ **Q3 — 传媒商户第一次正常参加通票月结，份额行能否落下？**  
引擎按 `merchants.merchant_type` 聚合并 `INSERT settlement_bill_format_shares.business_format`。库 CHECK 五值不含 `media`。HTML 将演出传媒列为必选业态之一。这不是「要两三件不巧才发生」：建 media 商户（V384 允许）+ 该商户核销进月结（分账主路径）即可撞上。  
➡️ 立项 GR-02。拟处置=新前向迁移扩 CHECK，不改 V366/V384 已应用字节。

❓ **Q4 — 能否改 V348/V384 正文把五值改成六值 / 补份额表？**  
Flyway checksum 已钉。改已应用文件=校验和失败。  
➡️ 禁止。只能前向迁移或登记。

❓ **Q5 — V147 可空负责人是否冲突「必须建立负责人」？**  
HTML 是档案应记录的字段清单，未写库级 NOT NULL。联系人同样可空。首次正常建档不因 NULL 崩溃。  
➡️ 不立项。不为「未填负责人」补 NOT NULL（添加/极端）。

❓ **Q6 — V199 只种 merchant 一组，商户端其他 group 会不会没开关？**  
专区路由均为 `group: "merchant"`。无行=默认开，只种一组即可关专区。  
➡️ 不立项。

❓ **Q7 — V384 种子是否可能重复或指向空主体？**  
`be_media` 行存在。WHERE NOT EXISTS 与 V329 部分唯一（active+is_draft）一致。库内恰一行默认。  
➡️ 种子本身成立。

❓ **Q8 — 「其他须指定主管公司」是否本模块 CHECK 漏了？**  
该句在枚举速查商户业态。本五文件只封业态词，不封主管公司。实施应在商户档案服务/结算开通闸（别模块）。  
➡️ 跨模块观察，不进本模块队列。

---

## 7. Findings（临时 GR；正式 ID 待 B2）

### GR-M021-B1-01

- **文件:行号**：`backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql:10`（授权体 L11–18）
- **原文**：
  ```
  -- 平台管理员默认可监督商户自助层(与其它域 role_platform_admin 全授范式一致)。
  INSERT INTO role_permissions (role_id, permission_code)
  SELECT 'role_platform_admin', permission_code
    FROM permissions
   WHERE permission_code IN (
          'merchant.portal.settlement.view',
          'merchant.portal.settlement.dispute',
          'merchant.portal.fee_recipient.manage')
  ON CONFLICT (role_id, permission_code) DO NOTHING;
  ```
- **更长上下文**：同文件 L1–8 定义三项自助权限且 `requires_approval=FALSE`；L10 将「监督」绑定到上述全授。
- **正常触发 / 影响**：出厂仅 `role_platform_admin` 持有这三项（psql）。平台管理员无 `merchantId` 调商户专区对账/异议 → `MERCHANT_SCOPE_REQUIRED`（单测绿）。注释所写「默认可监督」在首次正常使用专区自助 API 时不发生。权限矩阵仍显示管理员拥有这三项，造成「有权却 403」。转入项 M020-007 的 V200 L10 部分：本席独立复核成立。
- **四分类（冲突→细化→技术性→添加）**：HTML 未写「平台管理员经商户自助层监督」；业务地图 `:1310`「商户侧不是平台后台」、`:1331`「商户不能看平台全局和其他商户数据」——fail-closed 是下位实现。注释/全授与该边界的关系属已有条目的实现细节。**细化**（非与明文直接相反的「必须删除列」冲突；非无上位的添加）。
- **工程缺陷是否成立**：**是**（注释完成声明与可执行行为不一致；GEN-016/GEN-002/GEN-005）。
- **冻结 ID**：GEN-016、GEN-002、GEN-005、GEN-001、GEN-018
- **第一性依据**：类型 1 HTML `:1310`/`:1331`；类型 2 `MerchantPortalServiceTests#callerWithoutMerchantScopeIsRejectedAndAudited` Tests run 1 Failures 0；类型 4 psql `role_permissions` 仅 platform_admin 三行
- **最小拟处置**：已应用 V200 不可改写。前向迁移：`DELETE FROM role_permissions WHERE role_id='role_platform_admin' AND permission_code IN ('merchant.portal.settlement.view','merchant.portal.settlement.dispute','merchant.portal.fee_recipient.manage')`，使权限点保留、由运营授予**有商户归属**的账号；平台监督继续走既有平台口径（如费用通知的 `merchant.billing.manage`）。不在本模块发明商户角色（添加）。若 B2 认「全授范式无业务发明且 fail-closed 已挡住越权」可改只登记、修复队列否——本席仍判缺陷成立，因注释是对完成状态的虚假声称。

### GR-M021-B1-02

- **文件:行号**：`backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql:15–26`（本模块不完整同步）；对照依赖表现行约束（非本模块文件，作证据）`V366__settlement_bill_format_shares.sql:37` 仍为五值
- **原文（V384）**：
  ```
  -- ① 业态六值封口(加 media=传媒)。业态错＝分账份额拆错、钱分错人,故词表仍由数据库封口。
  ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_business_format;
  ...
  CHECK (merchant_type IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
  -- ② 招商线索「意向业态」同词表同步
  ...
  CHECK (... 'media' ...);
  ```
- **更长上下文**：V384 同步了 `merchants` 与 `leasing_leads`，未碰 `settlement_bill_format_shares`。V366 L37：`CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'other'))`。`TicketDistributionSettlementService.settleMonth` L359–368 对每个 `FormatShare`（`businessFormat` 来自 `COALESCE(m.merchant_type,'bar')`）`upsertFormatShare`。
- **正常触发 / 影响**：招商/后台把商户业态设为 `media`（V384 后合法且 HTML 必选之一）→ 该商户核销进入通票月结 → `INSERT settlement_bill_format_shares (... business_format='media' ...)` → `chk_sbfs_format` 拒绝 → 整月结算事务失败。psql：`chk_sbfs_format` 无 media；`chk_merchants_business_format` 有 media。这是首次正常分账路径，不是极端并发。
- **四分类**：先冲突：现行 HTML「商户业态」含演出传媒且「业态用于确定…域内结算规则」「演出传媒归传媒公司」。份额留痕表拒绝 `media`，使合法业态无法作为分账行键，与该明文不能同时成立。**冲突**（删除过时五值封口，改为与六值词表同一闭集）。即使有人改判细化，工程缺陷仍成立。
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-014、GEN-008、GEN-006、GEN-002、GEN-018
- **第一性依据**：类型 1 HTML 枚举速查 `:195` 商户业态、业务地图 `:1309` 业态句、`:1690` 演出票/传媒公司；类型 4 `pg_get_constraintdef` 两条 CHECK + `'media' = ANY(...)` 对五值数组为 f
- **最小拟处置**：禁止改写 V366/V384。新前向迁移：`ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;` 再 ADD 含 `media` 的同一六值 CHECK（可 NOT VALID 后 VALIDATE，按库规模选最小锁）。不在本模块改结算 Java。

---

## 8. 观察（不进本模块缺陷队列）

| ID | 内容 | 处置 |
|---|---|---|
| OBS-M021-B1-01 | V147 L1 把建档句标成治理 HTML `:429`；现行该句在 `:494`，`:429` 是独立酒吧商户职责。首次正常跑不因行号失败 | 只登记。已应用文件不可改注释 |
| OBS-M021-B1-02 | V348 L2–4 HTML 引文为五值且非现行逐字；运行时封口已被 V384 改为六值 | 只登记陈旧注释 |
| OBS-M021-B1-03 | V200 出厂只给 `role_platform_admin` 播种；`roles` 无商户角色。与 M020 观察一致，本席 psql 独立确认。HTML 未要求出厂商户角色 | 观察；补角色=添加，不问不建 |
| OBS-M021-B1-04 | `LeasingService.java:285` 注释「同五值词表」；实际 `createMerchant` 走六值。文件不在 M021 | 跨模块观察 |
| OBS-M021-B1-05 | 枚举速查「其他」须指定主管公司并说明理由，未指定不得开通结算。本五文件 CHECK 不覆盖该规则 | 跨模块观察（档案服务/结算开通） |
| OBS-M021-B1-06 | 主工作区 staged `V427__m009_project_profiles_and_associations.sql`；complex_test flyway 已 success 应用 V427。属 M009 遗留，不属本模块 | 跨模块观察 |
| OBS-M021-B1-07 | V251 已将三项权限名改为中文；V197/V198 开关范式、V354 `be_media`、V375 种子范式均成立 | 正向依赖核实，非缺陷 |

转入登记 M020-007：findings 写 V200 L10 注释归 M021。本席未读席甲 M021 raw，仅独立核对 V200 正文 + 服务 fail-closed + psql。GR-01 承接该转入并给出本席分类。

---

## 9. 依赖裁断摘要（席位陈旧事实 → 后续迁移）

| 声称 | 后续/现行事实 | 裁断 |
|---|---|---|
| V348 五值封口 | V384 六值 CHECK 已替换 | 现行以 V384 为准；V348 正文是历史 |
| V200 英文 permission_name | V251 中文名 | 现行中文；不判 V200 英文为现行缺陷 |
| V199 同 V198 范式 | V198 同形 INSERT + ON CONFLICT (id)；scope_id=菜单 group | 成立 |
| 总开关 V197 | `tcs_term_merchant_portal` 行存在 | 成立 |
| V354 be_media | 行存在 | 成立 |
| V375 种子范式 | V384 INSERT SELECT WHERE NOT EXISTS 同构 | 成立 |
| V366 业态份额表随词表 | `chk_sbfs_format` 仍五值 | **不成立** → GR-02 |
| 已应用迁移可改 | Flyway checksum 已钉 | **不可改写**；缺陷走前向迁移或登记 |

---

## 10. 覆盖完整性与 PASS/FAIL

- 五文件全文覆盖：是（103 行，与计划有效行一致）
- 18 条映射判据均有适用证据：是
- 三线均执行：是
- 声称均有四类依据之一：是（HTML / psql / 单测 / Flyway 一次应用）
- 未答 questions：无（添加项 0；B1 不发问）
- 未定稿缺陷：GR-M021-B1-01、GR-M021-B1-02
- 哈希行：本报告第一行

**本席 B1 结论：FAIL。**

不修复、不改 ledger/findings、不等席甲互读。B2 归并时以本段完整报告（本文件最后一个 `CRITERIA-LOCK-SHA256:` 起）为准。

## M021 B2-r1 整批归并复核（Grok 乙席）

- 报告版本：V0.02（本文件末尾追加；上方 V0.01 全文一字不改）
- 席位：乙 / Grok 4.6 / reasoning-effort xhigh
- 阶段：B2-r1（只审本批归并／分类／队列／最小修复边界；不重审模块）
- 正本：`/home/james/complex-management` `main` 主工作区
- 时间：2026-09-20T17:44:22Z
- 输入（只读）：
  - `modules/M021-b1-grok-raw.md` V0.01（本文件旧前缀 35139bytes，SHA256 `cd58fda5bcc82b083b5042972e946bdf6adfd93f33eb97b3456a2b59e08fdcfa`；FAIL：GR-M021-B1-01/02、OBS-M021-B1-01…07）
  - `modules/M021-b1-self-raw.md` V0.01 封存段（前 20006bytes，SHA256 `cd8ad7840729d91266ab330e4f59ed88290263381cc8cf7e524f24f8b81b57ac`；FAIL：CX-M021-001、OBS-1…6）。本票**未把**该文件 V0.02 追加段（席甲 B2 结论）当作输入。
  - 候选 `docs/audit-2026-09/findings.md` V2.65 顶部「当前 M021 B2-r1 整批候选」段（全文件 SHA256 `522291065c922fb5576248c2772564cf635c30fd6b27684d528ebde4c1431659`）
- 判据锁：沿用 B1 已冻结 V0.11 / `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。B2 **不写**任何新的独占 `CRITERIA-LOCK-SHA256:` 行；不加锁外判据。
- 只读承诺：未改源码／测试／HTML／lock／findings／ledger；未 fetch／commit／push／merge／deploy；未写新产物；psql 只 SELECT `complex_test` 与 `complex`；未用 MCP／子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意）

| ID | 来源归并 | 候选 分类／缺陷／队列 | 本席票 |
|---|---|---|---|
| M021-001 | GR-M021-B1-02 + 席甲 CX-M021-001 | 冲突／是／是 | **PASS**（冲突／是／是；前向 V428 扩 `chk_sbfs_format` 为六值；TDD 落既有 `SettlementFormatShareS11Tests`） |
| M021-002 | GR-M021-B1-01 + 席甲 OBS-2/OBS-3 | 技术性／否／否 | **PASS**（L587-588 成立；不采本席 B1 拟 DELETE；注释无修复面） |

冲突入队 1（001）。技术性登记 1（002）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001（1）；「否」= 002（1，只登记）。

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-M021-B1-02 + CX-M021-001 | 001 | 同根：V384 自称「词表一次性放开／同词表同步」只重建 merchants／leasing_leads CHECK；`chk_sbfs_format`（V366:37）仍五值无 media；`aggregateFormatRevenue:186` 取 `merchants.merchant_type`，`TicketDistributionSettlementService:359-368` upsert `business_format`；media 商户首次进通票月结违反 CHECK、整张月结失败。分类采本席 B1「冲突」（席甲原判细化，2.1 顺序从严）。事实未改写。 |
| GR-M021-B1-01 + 席甲 OBS-2/OBS-3 | 002 | 同根：V200:10 注释「默认可监督」与 fail-closed 不符；:11-18 三码授 `role_platform_admin`；出厂无商户角色。候选把本席 B1「细化／是／前向 DELETE」经核源改判为技术性／否／否——见第二节，**成立**，非漏并、非把 B1 事实改写成别的缺陷。 |
| OBS-M021-B1-01 ＝ 席甲 OBS-1 | 只登记 | V147:1 `:429`→现行 L494。未改写。 |
| OBS-M021-B1-02 ＋ 席甲 OBS-4 前半 | 只登记 | V348:2-4 五值引句已被六值句取代。未改写。 |
| 席甲 OBS-4 后半 | 只登记 | V366:27 列注释仍写「V348 五值」（M319）。本席 B1 未单列，候选作登记不入队，无升队、无改写。 |
| OBS-M021-B1-03 ＝ 席甲 OBS-3 | 002 上下文 ＋ 只登记 | 出厂无商户角色；补角色＝添加，不问不建。并入 002 说明持有者现状，同时在只登记段保留「不问不建」。未发明角色。 |
| 席甲 OBS-5 | 只登记 | V348 旧词表归一到 other（开发环境口径）。本席 B1 §4.4 已述同一事实未编号；候选登记。 |
| OBS-M021-B1-04 | 只登记 | `LeasingService.java:285` 注释「同五值词表」（别模块）。本票复读 :285 原文仍在。 |
| OBS-M021-B1-05 | 只登记 | 枚举速查「其他须指定主管公司」不在本五文件。 |
| OBS-M021-B1-06 ＝ 席甲 OBS-6 | 只登记 | 主工作区 M009 遗留暂存 V427；complex_test 已应用、origin/main 最大 V426。候选附加生产库 flyway 385 与 `validate-on-migrate`／无 out-of-order 的部署顺序提示——见第三节环境核实，属操作员登记，不新开 ID。 |
| OBS-M021-B1-07 | 只登记 | V251 中文化、V197/V198 开关范式、V354 `be_media`、V375 种子范式。正向依赖，非缺陷。 |

双方 findings 2 条、观察（本席 7、席甲 6）全部落点。无漏并。无应升添加、无应升本批缺陷被降（002 是分歧项，核源后改判，见下）。转入项 M020-007 的 V200 部分闭合为 002 登记。

### 二、002 独立核实（分歧项）—— **L587-588 成立；技术性／否／否**

本席 B1 对 GR-01 判细化／工程缺陷是，拟前向迁移 `DELETE` 三行 `role_platform_admin` 授予。候选不采 DELETE。本票按用户点名的 L587-588 **独立读原文**，不沿用席甲 B2 票。

1. **业务地图原文** `docs/business-system-map-for-operations-v0.1.html` `h3`「平台管理员拥有全部日常功能权限」在 **L587**；**L588** 逐字含：
   > 拥有系统里全部**日常功能**权限；今后每上线一个新功能，都必须先定好它算日常功能还是高危功能才允许上线，定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况，还没定类的一律先当高危功能对待、不自动同步；但资金操作…敏感数据…删除，以及最终业务审批这几类，不因"平台管理员"这个身份自动获得…
   「日常功能自动同步、不会出现管理员反而没权限」是明文。三码不在资金／敏感／删除／最终审批例外清单。

2. **日常功能口径** `AuthorizationService.dailyPermissions:100-115`：`status=active` 且 `requiresApproval=false` 且 `riskLevel ∈ {low, medium}`。本票 SELECT `complex_test`：
   `merchant.portal.settlement.view` / `settlement.dispute` / `fee_recipient.manage` 均为 `active` / `medium` / `requires_approval=f`。三码 **属于** 该口径。同文件 `isPlatformAdminBridge:509-512`（`accountType=platform_admin`）在 `permissionGrant:81` 对 daily 集合直接放行——权限闸已按 L588 自动给日常码。

3. **授予范式** 仓内 `INSERT INTO role_permissions` 且含 `role_platform_admin` 的迁移本席实算 **117** 条（候选写「83 条迁移」条数不采，见非阻断）。V200:11-18 与该范式同构，是 L588 的下位种子，不是多余授权。

4. **DELETE 三行** 与 L588「全部日常功能权限／自动同步／不会出现管理员反而没权限」**不能同时成立**（2.1 冲突优先：删授予＝与明文矛盾）。即便删行，权限闸仍走 daily 桥，**不能**消除无商户归属时的 403，也达不到本席 B1「只留给有商户归属账号」的拟效果。故 DELETE 既与 HTML 冲突，又无可执行修复面。**不采。**

5. **持码却 403** 是两道闸：权限闸（日常码）× 数据范围闸（`MerchantPortalService:171-174` 红线④，无 `merchantId` → `MERCHANT_SCOPE_REQUIRED`）。平台监督走平台口径：`MerchantFeeNotificationRecipientController:19-22/:29-30/:85-89` 先 `merchant.billing.manage`（平台任意商户），自助码 `merchant.portal.fee_recipient.manage` 才要求商户 scope。与 M020 定稿 fail-closed 同构，不是「管理员没权限」。

6. **L10 注释**「平台管理员默认可监督商户自助层」与 fail-closed 不符，属已应用迁移（Flyway checksum **1214979897**，complex_test `success=t`）不可改写。无可执行修复面。GEN-016 登记。

7. **先例**：M010-006（迁移注释失实、checksum 钉死、技术性／否／否）；M016-004（含 `role_platform_admin` 授予范式登记、技术性／否／否）。同构。

四分类：**技术性**（无业务发明：不新增角色／权限／状态；授予是 L587-588 明文的下位实现；只剩一句不可改写的历史注释。无与 HTML 冲突——冲突的是 DELETE 本身，故不删授予）。工程缺陷：**否**。队列：**否**（只登记）。本席 B1「工程缺陷是 + 前向 DELETE」**撤回**。M020-007 的 V200 部分就此闭合为登记。票：**PASS（技术性／否／否）**。

非阻断：候选「全仓 83 条迁移」条数与本席实算不符（117 条迁移；`role_platform_admin` 授予行 376，其中 daily 口径 158）。同范式事实成立，不依赖 83 这个数字，不否决 002。

### 三、001 分类、队列与最小修复边界—— **同意**

**分类＝冲突（采本席 B1，2.1 从严）。** 现行 HTML 枚举 L195／业务地图 L1309 业态闭集含演出传媒 `media`，且「业态用于确定…域内结算规则」；L1690「演出票 | 传媒公司」。`chk_sbfs_format` 仍五值（本票 `pg_get_constraintdef`：complex_test 与生产 `complex` 均为无 media）。合法业态不能作分账行键，与上述明文不能同时成立。2.1：「冲突」＝与 HTML 明文矛盾 → **判定为删除**（删除过时五值封口，改为与六值词表同一闭集），进修复队列。席甲 B1 原判细化（V384 同步不全）在冲突成立时不得跳序改细化。即使改判细化，缺陷仍是、队列仍是；本票维持冲突。冻结判据未出锁：GEN-014／006／008／002／003（本席 B1 另列的 GEN-018 为取证方法，候选未列入 freeze 行不构成加判据）。

**工程缺陷＝是。队列＝是。** 首次正常流：招商／后台设 `media`（V384 后合法）→ 权益进通票月结 → INSERT `business_format='media'` → CHECK 拒 → 整月事务失败。非极端场景。

**最小处置——同意，无发明业务规则：**

- 已应用 V366／V384 不可改写（checksum 已钉）。不改结算 Java、不改 HTML。
- 新前向迁移 `V428__m021_settlement_format_share_media.sql`（文件版本 V0.01）：`ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;` 再 `ADD CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar','catering','retail','amusement','media','other'))`。同名六值 CHECK，与 merchants／leasing_leads／`MERCHANT_BUSINESS_FORMATS` 同一闭集。**不用 NOT VALID**：本票 SELECT 留痕表 `complex_test` 0 行、生产 `complex` **2** 行，全表校验成本可忽略；本席 B1「可 NOT VALID」收窄为直接 ADD，不是放宽。
- **编号 V428**：origin/main 最大 `V426__space_type_legacy_disabled_to_archived.sql`；主工作区 staged `V427__m009_project_profiles_and_associations.sql`（`git status` `A`）；complex_test flyway 最大 427 且 V427 `success=t`；工作区无 V428 文件。取 428 避免与已应用的 427 撞号。生产 `complex` flyway 最大 **385**（本票 SELECT）。`application.yml:30` `validate-on-migrate: true`，yml／pom **无** `outOfOrder`。只登记提醒（不新开 ID）：V427 须先于或随同 V428 进入发布 classpath，否则日后补 V427 会低于已应用的 428。属环境事实，与席甲 OBS-6／本席 OBS-06 同根。
- **TDD**：在既有真实库类 `backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java` 加一例 media 业态商户进月结。该类已 `@SpringBootTest` 连 `complex_test`、走 `settleMonth`、有 `seedMerchant(id, format)`；既有游乐例断言 `beneficiary_kind=entity` 且 `beneficiary_entity_id=be_amusement`。候选断言 `business_format=media`、`beneficiary_kind=entity`、`beneficiary_entity_id=be_media` 与 `ENTITY_BENEFICIARY_FORMATS`（`FORMAT_MEDIA → performance_ticket`，`:87-90`）及 V384 种子 `rsa_performance_ticket_default → be_media` 同构，**不发明**归属规则。RED（现行 CHECK 违例）后加迁移转 GREEN。属别模块单元的跨单元 TDD（先例 M015 改 M017、M020 改 M022）。不改结算生产 Java。
- 随新迁移重生成 data-isolation spec／domain-ownership 等门禁产物并跑 `verify:predeploy`：仓内 `tools/generate-data-isolation-spec.mjs` 以迁移目录为事实源，属既有门禁，不是新业务规则。

有无发明业务规则或应升添加：**无。** 六值闭集已在 HTML／merchants CHECK／代码词表；TDD 只锁既有 media→performance_ticket→be_media 路径；不新增业态／主体／费率／状态。2.1 冲突处置＝删除五值封口，不是添加第七值。票：**PASS（冲突／是／是）**。

### 四、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M021*`。不 BLOCKED-ASK。

只登记不入队（无应升添加、无应升本批缺陷）：V147 行号漂移、V348 五值引句、V366:27 列注释、V348 旧词归一、出厂无商户角色（不问不建）、`LeasingService:285`、枚举「其他须指定主管公司」、V427 环境事实、OBS-07 正向依赖。

### 五、分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。

- 001：新文件 `V428__m021_settlement_format_share_media.sql`：DROP IF EXISTS `chk_sbfs_format` 后 ADD 同名六值 CHECK（含 media）；不用 NOT VALID；不改 V366／V384／结算 Java／HTML。`SettlementFormatShareS11Tests` 加一例 media 商户进月结（份额行 `business_format=media`、`beneficiary_kind=entity`、`beneficiary_entity_id=be_media`），RED 后 GREEN。随迁移重生成 data-isolation spec 等门禁产物。
- 002：不修。不 DELETE 三行授予。不改 V200 注释。只登记。
- 禁止：改已应用迁移字节、发明商户角色、为「有权却 403」加平台全量旁路、把 L10 监督声称写成须修的完成声明、把 002 升添加或再入队、为 001 改结算引擎／HTML／第七业态值。

### 结论

全批差集无漏并、无原意改写。001 冲突／是／是（五值封口与 HTML 六值闭集不能同时成立；最小面＝前向六值 CHECK + S11 一例 media 月结）。002 经独立读 L587-588 原文：日常功能自动同步明文**成立**；三码属 `dailyPermissions`；DELETE 反与该明文冲突且不能消 403；注释无修复面；技术性／否／否。本席 B1 GR-01 的 DELETE 拟处置撤回。添加 0。HTML 申请 0。questions 0。非阻断：候选「83 条迁移」条数不采（实算 117），不影响 002 结论。逐 ID：001 PASS、002 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；B3 累计 0。
