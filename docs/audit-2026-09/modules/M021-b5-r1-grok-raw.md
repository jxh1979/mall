CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B5-r1 席乙（Grok）修复后整模块三线全量重审

- 报告版本：V0.01（指定路径原先不存在，本次创建完整报告；不覆盖）
- 席位：固定独立只读审计席乙 / grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 次之后的修复后全量审计；整模块重审，不只看 diff，不复用 B2/B4 票作本票结论）
- 模块：M021 — 商户档案、商户类型与商户侧门户／数据库迁移／第1段（`plan/modules.md` V0.04）
- 控制合同：`160-audit-prompt.md` V0.17（路径 `.scratch/module-recheck-20260908/160-audit-prompt.md`；本席实算 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）
- 执行说明：`docs/audit-2026-09/plan/runbook.md` V0.02
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；映射 `M021 → [GEN-001…GEN-018]`（定义区 18 条全文已逐条再读；未扩 HTML 锚点判据、未新增判据）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- after tree（派发给定、本席 `git cat-file -t` 核为 `tree`）：`a5e4fadfe03ff042c00bd240719b76d9ab7341d6`
- findings 台账版本：V2.67（只读；当前值 M021-001 否/已修、M021-002 否/登记；本席不改台账）
- 互盲：未打开 `modules/M021-b5-r1-self-raw.md`（目录可见该路径存在亦不读）；未派 MCP/子代理/第三席；未问人；未改业务/HTML/lock/findings/ledger；未 fetch/commit/push/merge/deploy；未建脚本/日志/临时审计产物
- 旧票可读且已读：`M021-b1-grok-raw.md` V0.01+V0.02、`M021-b4-r1-grok-raw.md` V0.01、findings V2.67 顶部与 M021-001/002 行。本票结论以本轮全文+库实态+本次 mvn 为准，不把 B4 PASS 折算成本票 PASS
- 范围：计划 5 个已应用迁移（零改写）+ B3 修复产物 `V428__m021_settlement_format_share_media.sql` 一并全文审
- 本席 B5-r1 结论：**PASS**（整模块审完、18 项均有适用证据、三线均跑、无未定稿新缺陷、001 回归已修、002 在 HTML L587-588 下仍只登记）

本票仅为席乙一票。不等于 B5 双 PASS，也不等于模块收口。

---

## 0. 强制技能自检与节点应用（本环境实核，不抄 Codex 表）

本席开工前按 `160-audit-prompt.md` V0.17 §6 在本机路径核技能。grilling 只借追问树**自问自答**（事实自己读；作废等用户/派子代理）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与「只允许两席、唯一可写本 grok-raw」冲突，**不执行**。`audit-context-building` / `differential-review` 本体强制子代理，**不用**。`requesting-code-review` 的 `HEAD~1`/同会话子代理复审不用——本席自身即外审。`test-driven-development` / mattpocock `tdd`：本轮不新写测试，只跑已归因 M021-001 的真实库例。`resolving-merge-conflicts`：无进行中 merge/rebase，节点不适用。`ponytail` 是 B3 修复强度，B5 只审不修。`i-have-adhd` 与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`writing-for-agents`：本 raw 按完成准则写给下游合取席。`long-running-background-tasks`：本次 mvn 同步跑完（5.726s），未留后台作业。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户专项指令优先 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 识别 Arc audit 默认派子代理；**不执行**全量机械审计 | 本体第三席+§7 外产物，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用** |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep aggregateFormatRevenue`（tokens saved ≈ 34892）定位结算写份额路径 | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗线 6 问自问自答 | 技能原文访谈用户；本轮按合同收窄 |
| `batch-grill-me` | — | — | **本机 `skills/productivity/` 无此目录**，记缺口，未冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 业态词表的深模块接口=库 CHECK 闭集；V428 把第三处封口拉回与前两处同一接口 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=本次 `mediaShareIsRecordedAndAttributedToMediaEntity` + `pg_get_constraintdef`；禁止手工写库 | 未插桩 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（media 商户→`aggregateFormatRevenue`→`upsertFormatShare`→`chk_sbfs_format`）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑三工具锁哈希、after tree、psql、mvn 8/8 | 无 |
| `ponytail-review` | ponytail 4.9.0 `skills/ponytail-review/SKILL.md` | V428：DROP IF EXISTS + ADD 六值 CHECK，无可删层 | 不落地修改 |
| `tdd` / `test-driven-development` | 存在 | 核已有跨单元真实库例；本席不新写测试 | 纯 SQL 本可不强制测；本轮按定稿跑 S11 |
| `writing-for-agents` | mattpocock productivity | 本 raw 给下游合取席；完成准则=整模块覆盖+18 证据+三线+PASS/FAIL | 技能主场景是 skill/AGENTS.md |
| `git-guardrails-claude-code` | mattpocock `skills/misc/git-guardrails-claude-code` | 只读；未 push/reset/clean | 存在，本席未装 hook |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `ponytail` | ponytail 4.9.0 | B5 不修 | 节点不适用 |
| `long-running-background-tasks` | grok bundled | mvn 同步完成 | 节点不适用 |
| `receiving-code-review` | superpowers | 本席产出意见，不收意见去修 | 本体 IMPLEMENT 作废 |
| `resolving-merge-conflicts` | 存在 | 无 merge/rebase | 节点不适用 |

明确不用（提示词已划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、mattpocock `code-review`、`domain-modeling`、`ponytail-audit`、`ponytail-debt`。未使用 MCP。

graft 本席调用节省合计约 34892 tokens（`aggregateFormatRevenue` / `upsertFormatShare` 调用链）。

---

## 1. 独立锁实算与输入冻结

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`，与本文件第一行独占锁行一致。sidecar 一行同值。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值。criteria-lock 版本仍 V0.11。

B4 两 raw 本席 `sha256sum` 复核（只作历史对照，不作本票）：

| 文件 | SHA256 |
|---|---|
| `modules/M021-b4-r1-grok-raw.md` | `797795b5aaef3e7d91ccff0a43e1235e4761b5f0c50909bb8d274aae7d106e0e` |
| `modules/M021-b4-r1-self-raw.md` | `7b633b5290e190489409724190f0107f8af3b5ca86c739ef0fe75c7b17d868cb` |

与 findings V2.67 顶部所记一致。after tree `a5e4fadfe03ff042c00bd240719b76d9ab7341d6` 对象类型 `tree`。

---

## 2. 范围与全文覆盖（不抽样）

`plan/modules.md` V0.04 计划有效内容行 103。本席按提示词命令实算原 5 文件：

```
git ls-files -z -- \
  backend/src/main/resources/db/migration/V147__merchants_owner_person.sql \
  backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql \
  backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql \
  backend/src/main/resources/db/migration/V348__merchant_business_format.sql \
  backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql \
  | xargs -0 cat | wc -l
# 输出：103
```

B3 修复产物 V428 一并全文读。六文件均全文阅读，无抽样、不只看 diff。

| 文件 | wc -l | SHA256（本席 sha256sum） | after tree `git hash-object` | 覆盖 |
|---|---:|---|---|---|
| `V147__merchants_owner_person.sql` | 6 | `70a00f5d8a1936fac2e8bdf54d61150a5e30257a2e80deaff5dbd9049bb6734a` | `7be3c3a9dd73279ba23cbacd43bcff30e42dce4c` | 全文；与 after tree blob 一致 |
| `V199__merchant_portal_zone_module_switch.sql` | 9 | `2b162b399a1f84aa419f90977dbe8c29947dd006c66aa8f40f76d74f4489d6ff` | `2bb0a18a7b16ce97404689413e5de511a1ca2364` | 全文；一致 |
| `V200__merchant_portal_zone_permissions.sql` | 18 | `b082220f5bde8aba00f3ac590c4604badb1d660c641fae7d8a9961f517214e81` | `75711dbb9e665accbeb883113b5e772f537a6399` | 全文；一致 |
| `V348__merchant_business_format.sql` | 33 | `b01046851bb04ac5de83fba558ca052220f415f80e561fb5eace2888b142df4e` | `fcb26229a304bde8da27218a3b9e0a896085beef` | 全文；一致 |
| `V384__merchant_business_format_media.sql` | 37 | `36a8fc281957253926c07be557b3ab6e960a06f941312af14f6732422e588e2f` | `48457df74b49b230068c3beab0ac6498b863302b` | 全文；一致 |
| `V428__m021_settlement_format_share_media.sql` | 13 | `239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589` | `5920e7d53d41952129085f4626e1baaf82c4c6af` | 全文；`git cat-file -p <after>:…V428… \| sha256sum` 同值 |
| 原 5 文件合计 | **103** | 与计划有效行一致 | 已应用零改写 | |
| +V428 | **116** | 1215 bytes | 修复产物 | |

上表 after tree 列为 `git hash-object` 实算，与 `git ls-tree a5e4fadfe03ff042c00bd240719b76d9ab7341d6` 对应 blob 逐文件相等。五份已应用迁移工作区字节与 after tree 一致 → **已应用文件零改写**成立。

after tree 另含 `V427__m009_project_profiles_and_associations.sql`（blob `e485a5d29fea55fa714091485aa26facc8c45160`），属 M009 修复产物，不纳入本模块缺陷队列；仅在 grilling「V427 与 V428 并入顺序」中作依赖事实。

依赖对照（只读、不扩模块队列）：V197 总开关、V251 权限中文化、V354 `be_media`、V366 份额表、V375 种子范式；现行词表 `PlatformService.MERCHANT_BUSINESS_FORMATS`、`TicketDistributionSettlementService.ENTITY_BENEFICIARY_FORMATS`、`JdbcSettlementBillRepository.aggregateFormatRevenue` / `upsertFormatShare`。

---

## 3. 三 HTML 逐字核（类型 1）

### 3.1 枚举速查「商户业态」`docs/business-enum-quick-reference-v0.1.html:195`

```
酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。每个商户必须归入一种业态，业态用于确定经营域、主管公司和域内结算规则：酒吧归娱乐平台公司，游乐归游乐公司，演出传媒归传媒公司，餐饮与零售归城市项目公司（中央餐饮供货部分仍由中央餐饮公司审批）；选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账。只有跨域组合通票按收费权益价格拆分域份额，酒吧通票、纯游乐通票等域内通票不得仅因商户业态再次做域间拆分。
```

### 3.2 业务地图商户管理 lead `docs/business-system-map-for-operations-v0.1.html:1308-1309`

```
每个商户必须归入一个业态：酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media，归传媒公司）、其他。业态用于确定经营域、主管公司和域内结算规则；只有跨域组合通票按收费权益价格拆分域份额，酒吧通票、纯游乐通票等域内通票不因商户业态再次做域间拆分。
```

### 3.3 收入归属「演出票」`docs/business-system-map-for-operations-v0.1.html:1690`

```
演出票 | 传媒公司
```

### 3.4 平台管理员日常功能 `docs/business-system-map-for-operations-v0.1.html:587-588`（002 核源）

`:587` `<h3>平台管理员拥有全部日常功能权限</h3>`

`:588` 原句含：

```
平台管理员与最高权限是两个不同的角色。平台管理员是日常运维岗位……拥有系统里全部日常功能权限；今后每上线一个新功能，都必须先定好它算日常功能还是高危功能才允许上线，定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况，还没定类的一律先当高危功能对待、不自动同步；但资金操作（退款、调账、分红发放、税费调整）、敏感数据查看与导出……删除，以及最终业务审批这几类，不因"平台管理员"这个身份自动获得，必须逐项单独授予并留痕……
```

### 3.5 治理地图建档句 `docs/platform-governance-map-for-operations-v0.1.html:494`

```
建立商户档案、运营主体、负责人、第一联系人和第二联系人。
```

V147 注释把该句标成 `:429`。现行 `:429` 是「独立酒吧商户」职责段。行锚陈旧；已应用迁移不可改写。见观察，不立项。

### 3.6 商户专区自助 `docs/business-system-map-for-operations-v0.1.html:1322`

```
商户专区自助：我的账单……我的对账（结算与分成明细，有疑问可留言提异议）；我的合同……费用通知接收人自己维护……
```

`:1331`：「商户不能看平台全局和其他商户数据」。

---

## 4. complex_test 只读实态（类型 4）与本次可执行（类型 2）

### 4.1 连接约束

未 `source /etc/complex/complex.env`。从该文件**只抽出**三键，把 JDBC URL 库名改写为 `complex_test`（原 URL 指向 `complex`，不得连主库），导出：

- `COMPLEX_DATABASE_URL` → `jdbc:postgresql://127.0.0.1:5432/complex_test`
- `COMPLEX_DATABASE_USERNAME` = `complex`
- `COMPLEX_DATABASE_PASSWORD`（len=48，正文不写值）

`psql` 证明：`current_database()=complex_test`，`current_user=complex`。未手工 INSERT/UPDATE/DELETE。

### 4.2 flyway_schema_history（含 428）

```
 version | description                               | success | checksum    | installed_on
 147     | merchants owner person                    | t       | 235932490   | 2026-09-09 06:45:51.758553
 199     | merchant portal zone module switch        | t       | -1183753096 | 2026-09-09 06:45:52.1653
 200     | merchant portal zone permissions          | t       | 1214979897  | 2026-09-09 06:45:52.167025
 348     | merchant business format                  | t       | 448719403   | 2026-09-09 06:45:53.076254
 366     | settlement bill format shares             | t       | 1936730037  | 2026-09-09 06:45:53.208901
 384     | merchant business format media            | t       | -663687440  | 2026-09-09 06:45:53.36398
 427     | m009 project profiles and associations    | t       | 216061286   | 2026-09-14 10:31:43.377139
 428     | m021 settlement format share media        | t       | 1696678370  | 2026-09-20 10:47:14.191735
```

≥420 连续：420…428 皆 `success=t`，无空洞。最新已应用版本 **428**。

### 4.3 同词表三处 CHECK（`pg_get_constraintdef`）

```
chk_merchants_business_format:
  CHECK (merchant_type IN ('bar','catering','retail','amusement','media','other'))

chk_leasing_leads_business_format:
  CHECK (intended_business_type IS NULL OR intended_business_type IN ('bar','catering','retail','amusement','media','other'))

chk_sbfs_format:
  CHECK (business_format IN ('bar','catering','retail','amusement','media','other'))
```

三处闭集相同、均含 `media`。`settlement_bill_format_shares` 另有 `chk_sbfs_beneficiary`（beneficiary_kind 二值，不是业态词表）。

宽搜 `pg_constraint` 中定义含 `amusement` 的 CHECK 还有 process_type / subject_type / source_stream / ref_type / revenue_stream / source_domain 等，词表与「商户业态六值」不同，**不是第四处同词表封口**。

### 4.4 开关行 / 权限行 / 归属种子行

开关：

```
tcs_mp_mod_merchant      | merchant_portal | terminal.enabled | module | merchant | t
tcs_term_merchant_portal | merchant_portal | terminal.enabled | global | global   | t
```

`merchant_portal` 终端仅上述两行。

权限（V251 后中文名现行）：

```
merchant.portal.fee_recipient.manage | 管理商户端自有费用通知接收人 | merchant_portal | medium | requires_approval=f | active
merchant.portal.settlement.dispute   | 发起商户端自有未结结算争议   | merchant_portal | medium | f | active
merchant.portal.settlement.view      | 查看商户端自有结算           | merchant_portal | medium | f | active
```

`role_permissions` 持有者仅 `role_platform_admin` 三行。

归属：

```
rsa_performance_ticket_default | project_id=NULL | performance_ticket | be_media | entity | active | is_draft=f | updated_by=migration:V384
be_media | 传媒公司 | management_company | active
```

`merchants.owner_person`：`varchar(120)` 可空。`merchant_type` NOT NULL。现行 `merchants` 仅 `bar×1`；`leasing_leads` 空；`settlement_bill_format_shares` 测后仍 0 行（S11 `@AfterEach` 清场）。

### 4.5 后续迁移与现行代码词表

磁盘 `V399` 之后为 V410–V428。V428 之后无更新的迁移文件。全仓 `'bar', 'catering', 'retail', 'amusement'` 字面：V348/V366 历史五值正文（已应用、不可改写）；V384/V428 六值。无第四处 SQL CHECK 仍停在五值。

现行代码（只读，不在本模块文件列表）：

- `PlatformService.MERCHANT_BUSINESS_FORMATS` L47：`bar, catering, retail, amusement, media, other`
- `PlatformService.requireBusinessFormat` L875–877 / `listMerchants` L146–148：非六值 → `MERCHANT_BUSINESS_FORMAT_INVALID`
- `TicketDistributionSettlementService` L76 `FORMAT_MEDIA="media"`；L87–90 `ENTITY_BENEFICIARY_FORMATS`：`bar→all_pass_ticket`、`amusement→amusement_ticket`、`media→performance_ticket`
- `JdbcSettlementBillRepository.aggregateFormatRevenue` L186：`COALESCE(m.merchant_type,'bar') AS business_format`
- `upsertFormatShare` L830–851：`INSERT … business_format`
- 前端 `OrganizationMerchantPage.tsx` / `LeasingWorkbenchPage.tsx`：同一六值（跨模块对照，非本模块文件）
- `LeasingService.java:285` 注释仍写「同五值词表」——代码走 `createMerchant`→六值校验。跨模块观察

### 4.6 本次 `SettlementFormatShareS11Tests`（只导出三变量，目标 `complex_test`）

```
EXPORTED=COMPLEX_DATABASE_URL,COMPLEX_DATABASE_USERNAME,COMPLEX_DATABASE_PASSWORD
URL_DB=complex_test
OTHER_COMPLEX_IN_ENV=['COMPLEX_DATABASE_URL','COMPLEX_DATABASE_USERNAME','COMPLEX_DATABASE_PASSWORD']
CMD=mvn -Dtest=com.complex.platform.settlement.SettlementFormatShareS11Tests test
```

关键日志：

```
Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
Successfully validated 396 migrations
Current version of schema "public": 428
Schema "public" is up to date. No migration necessary.
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 5.726 s
```

surefire XML：`tests="8" errors="0" skipped="0" failures="0"`。含

```
<testcase name="mediaShareIsRecordedAndAttributedToMediaEntity" … time="0.035"/>
```

该例断言：`shares` 含 `bar` 与 `media`；`media.net_revenue_cent=20000`；`beneficiary_kind=entity`；`beneficiary_entity_id=be_media`；`available_pool_cent=50000`（传媒不进酒吧池）；分池明细只有酒吧商户。断言能失败：若 `chk_sbfs_format` 仍五值，INSERT `media` 行会以约束违例红。本轮绿。

测后复 SELECT：`chk_sbfs_format` 仍六值；`share_rows=0`；`flyway 428 success=t`。测试写库已按 `@AfterEach` 清掉，本席未手工留行。

---

## 5. 第一性原理线（每文件三问）

### 5.1 V147 `owner_person`

1. **为何必须存在？** 删列则治理地图 `:494`「负责人、第一联系人和第二联系人」无法与联系人分列存储。有上位。
2. **声称是否执行？** 列 `varchar(120)` 可空，Flyway V147 success。HTML 未写库级 NOT NULL。
3. **未验证前提？** 注释行锚 `:429` 与现行不符（观察）。不因此否定列。

### 5.2 V199 商户专区模块开关

1. **为何必须存在？** `:1322` 商户专区自助需要可关的菜单组模块行。有上位。
2. **声称是否执行？** 行 `tcs_mp_mod_merchant` enabled=t；总开关 `tcs_term_merchant_portal` 存在。
3. **未验证前提？** 拦截器硬拒在别模块；本对象是开关行，行已核到。

### 5.3 V200 商户自助权限点

1. **为何必须存在？** `:1322` 对账/异议/费用通知接收人需要服务端权限码（GEN-005）。有上位。
2. **声称是否执行？** 三码 active/medium/无需审批；授予 `role_platform_admin`。`ON CONFLICT DO NOTHING`。
3. **未验证前提？** L10「默认可监督商户自助层」把授予写成监督通路。门户层对无商户归属 fail-closed。B2 定稿为技术性/否（L587-588）。本轮独立复核该前提：**授予本身被 HTML 日常功能句要求**；「监督」注释仍失实但已应用不可改写。见 §7 Q6 / §8.2。

### 5.4 V348 业态封口（历史五值正文）

1. **为何必须存在？** 「每个商户必须归入一种业态」；无 CHECK 则直连 SQL 可写任意词。复用 `merchant_type` 不另建列（GEN-001）。
2. **声称是否执行？** Flyway V348 success；随后 **V384 替换**为六值。现行库是六值。
3. **未验证前提？** 注释把当时五值写成现行 HTML 原文——现行已是六值。运行时封口已被后续迁移裁过。

### 5.5 V384 补 media + 演出票归属种子

1. **为何必须存在？** HTML 业态含 `media`；「演出票 → 传媒公司」。仅五值会把演出提供方打成 `other`。
2. **声称是否执行？** merchants/leasing_leads CHECK 含 media；种子行存在；`be_media` 存在。
3. **未验证前提？** 注释「词表仍由数据库封口」。B1 时第三处份额 CHECK 未同步。本轮库内 `chk_sbfs_format` **已含 media**（V428）。该前提在修复后成立。

### 5.6 V428（本模块修复产物，全文审）

V428 正文（13 行）要点：注释指向 M021-001；`ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;` 再 ADD 与 V384 同一六值 CHECK。不改金额列、不改已应用 V366/V384 字节。

1. **为何必须存在？** 删掉它，HTML「商户业态」六值 +「业态用于确定…域内结算规则」+「演出票 → 传媒公司」在月结份额留痕上做不到：`media` 商户核销进 `settleMonth` → `upsertFormatShare(business_format='media')` 会被五值 CHECK 拒绝，整张结算单失败。这是 B1 已定稿冲突的最小前向修复。有上位。
2. **声称是否执行？** Flyway 428 success=t、checksum 钉住、`DbValidate` 396 通过；`pg_get_constraintdef(chk_sbfs_format)` 含 media；S11 `mediaShareIsRecordedAndAttributedToMediaEntity` 8/8 中该例绿。
3. **未验证前提？** （a）重复执行：Flyway 版本只跑一次；SQL 带 `DROP IF EXISTS`，手工重跑仍收敛到同名六值 CHECK。（b）乱序：版本号 428，依赖表由 V366 创建（366≪428）；本库 427 已于 2026-09-14 先于 428 应用。（c）存量行：份额表测前测后均为 0 行；六值是五值超集，既有五值行（若有）仍满足新 CHECK。三条均用命令核过，不再当真。

---

## 6. 顺从式：GEN-001…018 逐项适用证据

锁映射仅 18 条通用工程判据。每条对照本模块 5 文件 + V428。不引用未映射的 BEQR/BSM/PGM ID 作为 finding 冻结 ID（HTML 原文仅作 2.3 类型 1）。

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 | 适用 | 六文件均能指到 HTML 建档/专区/业态/演出归属。V348 复用 `merchant_type`。V428 只扩第三处 CHECK、不新建表/列。ponytail-review：V428 Lean already |
| GEN-002 | 适用 | Flyway 147/199/200/348/366/384/428 success=t；本次 mvn 连 `complex_test`、current version 428、S11 8/8。V384 声称的分账封口在份额表上已被 V428 补齐。V200「可监督」注释仍与门户 fail-closed 不符，但属已定稿 002 只登记面 |
| GEN-003 | 适用（本模块计划无测试文件；V428 行为由跨单元真实库例执行） | 本次实跑 `SettlementFormatShareS11Tests` Tests run 8 Failures 0；`mediaShareIsRecordedAndAttributedToMediaEntity` 有能失败的断言（份额键、净额、`be_media`、不进池）。测试通过本身不替代：本席同时核了 CHECK 原文与 surefire 该例节点 |
| GEN-004 | 弱适用 | 迁移无用户输入。业态词在库端 CHECK。应用层 `requireBusinessFormat` 拒六值外（对照，不在本模块文件） |
| GEN-005 | 适用 | V200 三码 `requires_approval=false`。日常功能口径见 `AuthorizationService.dailyPermissions`：active ∧ ¬requiresApproval ∧ risk∈{low,medium} → 三码是日常功能。L587-588 要求自动同步给平台管理员。授予仍在。门户 fail-closed 是数据范围闸，不取消日常功能授权 |
| GEN-006 | 适用 | 三处同词表 CHECK 均为六值。V428 在空表上 ADD 超集 CHECK，失败不会留下「约束已 DROP 未 ADD」（PostgreSQL DDL 事务 + Flyway 单版本）。种子 WHERE NOT EXISTS。份额 PK `(bill_id, business_format)` |
| GEN-007 | 适用 | V147 IF NOT EXISTS；V199/V200 ON CONFLICT；V384/V428 DROP IF EXISTS；V384 种子 WHERE NOT EXISTS。V348 ADD 无 IF NOT EXISTS 但 Flyway 一次。V428 重复执行见 §7 Q3 |
| GEN-008 | 适用 | 六值闭集：HTML `:195`/`:1309`、三处 CHECK、`MERCHANT_BUSINESS_FORMATS`、前端两页、`SettlementBillFormatShareRecord` 注释一致。V366 正文仍写五值——已应用历史，现行以 CHECK 实态为准 |
| GEN-009 | 适用 | 迁移失败停 Flyway。无归属种子则演出流 fail-loud（种子已在）。份额 CHECK 拒非法业态是响亮失败。media 合法路径不再被拒 |
| GEN-010 | 适用 | 纯 DDL/DML 种子，无密钥。权限码无拼接注入。V200 全授不构成越权：日常功能句要求授予；门户另有商户隔离闸 |
| GEN-011 | 弱适用 | 种子 `updated_by='migration:V384'`。Flyway 428 行带 checksum/installed_on。S11 日志可复现 |
| GEN-012 | 弱适用 | V348 业态索引。V428 无全表重写。S11 按项目清理 |
| GEN-013 | 适用 | 只导出三变量；测试数据源落到 `complex_test`（日志 `jdbc:postgresql://127.0.0.1:5432/complex_test`）。`validate-on-migrate: true`（`application.yml:30`），本次 `Successfully validated 396 migrations`。未 `source` 全量 env |
| GEN-014 | **核心适用** | 顺序确定：366 建表 → 384 前两处六值 → 428 第三处六值。已应用五文件零改写（hash-object=after tree）。破坏性：V348 一次性 UPDATE 旧词→other（开发库注释已声明）。V428 不改金额。001 的最小前向迁移已落地且校验和钉住 |
| GEN-015 | 不适用 | 无界面。权限展示名中文化在 V251 |
| GEN-016 | 适用 | V147 `:429` 行锚错误、V348 HTML 引文非现行原文、V200 L10「可监督」、V384「同词表同步」在份额表上曾漏第三处（现已用 V428 补，注释不可改已应用文件）。文档完成声明以库实态+测试为准，不以注释为据 |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加。001 原冲突已修，不把「队列已否」当成功能未实现的反证，本轮用实态证明已修。002 仍技术性/否。无新添加项。不为理论第四处/第七值发明需求 |
| GEN-018 | 适用 | 结论落到 HTML 原文、mvn 输出、psql 输出、Flyway 一次版本+validate。不以 B4 票、注释或「一直这样」为据 |

---

## 7. 对抗式（grilling 结构；派发指定六问 + 必要反例）

❓ **Q1 — media 业态商户进月结现在怎样？**  
路径：`merchants.merchant_type='media'`（V384 CHECK 允许）→ 核销进 `settlement_transactions` → `aggregateFormatRevenue` `GROUP BY COALESCE(m.merchant_type,'bar')` 得到 `media` → `settleMonth` L359–368 `upsertFormatShare` 写入 `business_format='media'` → `ENTITY_BENEFICIARY_FORMATS` 映射 `performance_ticket` → 种子 `be_media`。  
现态：`chk_sbfs_format` 含 media；S11 `mediaShareIsRecordedAndAttributedToMediaEntity` 绿：份额行写下、归 `be_media`、不进酒吧池、分池明细无传媒商户。  
➡️ 首次正常月结路径已通。001 回归未现。

❓ **Q2 — 同词表还有没有第四处封口没放开？**  
SQL 字面与 `pg_constraint` 宽搜：同闭集只有 merchants / leasing_leads / settlement_bill_format_shares 三处，均已六值。其它含 `amusement` 的 CHECK 是流程类型、合规主体、日结来源、平面图 ref_type、收入流名、风险域，不是商户业态词表。应用层/前端对照亦六值。  
➡️ 没有第四处同词表五值封口。不为「也许还有一张表」发明扫描需求。

❓ **Q3 — V428 重复执行 / 乱序风险？**  
重复：Flyway `success=t` 且 `validate-on-migrate` 通过 → 不会按版本再跑。SQL `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT chk_sbfs_format`：若脱离 Flyway 重跑，第二次仍 DROP 再 ADD，收敛到同一六值。部分失败：PG DDL 与 Flyway 同一事务，不会留下「无约束」窗口作为已提交状态。  
乱序：V428 只改 V366 已建之表；版本 428>366，顺序 Flyway 不会先于 366 跑 428。本库实态 428 在 427 之后应用。`outOfOrder` 未在 `application.yml` 打开（默认 false）；当前 head=428，无未应用的更低版本（427 已 success）。  
➡️ 首次正常 migrate 不撞；不为双 Flyway 竞态加锁（框架已有）。

❓ **Q4 — V427 与 V428 并入顺序？**  
V427（M009）：`projects` 加列、`merchants.project_id`、`business_entity_projects`。V428：只动 `settlement_bill_format_shares` CHECK。无共享对象、无外键互相依赖。版本号 427<428。实态：427 于 2026-09-14 应用，428 于 2026-09-20 应用。本次 `DbValidate` 396 通过、current=428。after tree 两文件均在。  
➡️ 并入顺序安全。V427 失败不会让已应用的 428 回滚；新库按版本先 427 后 428。不把 M009 文件拉进本模块队列。

❓ **Q5 — 五值外的值是否仍被拒？**  
须拆两层：（a）**第六值 `media`** 曾是「五值外」，现在必须放行——CHECK 与 S11 已证明放行。（b）**六值闭集之外**（如 `entertainment` / `foo`）仍须拒。证据：三处 CHECK 的 `ARRAY` 恰好六元；`PlatformService.requireBusinessFormat` 非集合成员抛 `MERCHANT_BUSINESS_FORMAT_INVALID`。本席不手工 INSERT 第七值（禁手工写库）；闭集原文即类型 4。  
➡️ media 不再被拒；闭集外仍被拒。不为「再写一个负例迁移」加需求。

❓ **Q6 — 002 登记项在 HTML L587-588 下是否仍成立？**  
三码现行 `active` / `medium` / `requires_approval=f` → 落入 `AuthorizationService.dailyPermissions()`（L108–110）。L587-588 明文：日常功能「会自动同步给」平台管理员，「不会出现管理员反而没权限」。V200 `:11-18` 把三码授 `role_platform_admin` 正是该句的迁移时落地。删除这三行授予会与 L587-588 **冲突**（B1 拟 DELETE 已被 B2 双 PASS 否决，本轮不复活）。  
「持码却 403」：门户 `requireMerchantScope` 是商户隔离闸（`:1331`），与权限闸正交；资金/删除/最终审批仍不在这三码里。注释 L10「可监督」仍不准确，但已应用 Flyway checksum `1214979897` 不可改写，无可执行修复面。  
➡️ 002 分类技术性、队列否、只登记：**仍成立**。无新证据把它打回修复队列。

附加反例（不为极端补需求）：

- V147 可空负责人：HTML 未写 NOT NULL；不立项。
- 出厂商户角色缺失：HTML 未要求出厂名叫「商户」的角色；补角色=添加；不立项。
- `COALESCE(merchant_type,'bar')`：夹具无商户行才兜底；真 media 商户有行。S11 media 例种了商户行。不在本模块改 Java。
- 份额表现 0 行：ADD 六值 CHECK 无存量违法行。不需要 NOT VALID。

ponytail-review：`V428:L10-13` Lean already. Ship. `net: -0`。

---

## 8. 已定稿 ID 本轮回归（不是新 finding）

### 8.1 M021-001（冲突 / 否·已修）

- 原缺陷：V384 自称同词表同步，漏 `chk_sbfs_format` 五值，media 月结写不进份额行。
- 本轮证据：V428 已应用；CHECK 六值；S11 media 例绿；已应用 V366/V384 字节未改。
- 分类仍为冲突（历史）；队列否（已修）与实态一致。
- **不把「否」当成未实现**；本轮用类型 2+4 证明已实现。

### 8.2 M021-002（技术性 / 否·登记）

- 原登记：V200 L10 注释与门户 fail-closed 不符；授予本身是 L587-588 下位。
- 本轮：L587-588 原文仍在；三码仍是日常功能；授予仍在；无新工程缺陷。
- 四分类顺序：先冲突——授予与 L587-588 **不矛盾**（删除才矛盾）；非细化缺陷；**技术性**（无业务发明：不新增角色/权限/状态，只涉已应用注释）。无添加。
- 冻结 ID：GEN-016 / GEN-005 / GEN-017。类型 1+4。
- 维持只登记。

---

## 9. 本次 findings

审完全部 6 个文件、18 项判据、三线与指定六问之后：

- **新临时 GR：0**
- **回归未修好的旧 ID：0**
- **添加项：0**（B5 不发问）
- **未答 questions：无**

观察（不进缺陷队列，不为它们加修复）：

| ID | 内容 | 处置 |
|---|---|---|
| OBS-M021-B5-01 | V147 L1 行锚 `:429` 应为 `:494` | 已应用不可改注释 |
| OBS-M021-B5-02 | V348 L2–4 HTML 引文仍为五值 | 运行时已是六值；注释不可改 |
| OBS-M021-B5-03 | `LeasingService.java:285`「同五值词表」 | 跨模块；代码走六值 |
| OBS-M021-B5-04 | after tree / flyway 含 V427（M009） | 与 V428 无 DDL 耦合；不进本模块队列 |
| OBS-M021-B5-05 | V200 英文 permission_name 已被 V251 改为中文 | 现行库中文；不判 V200 字面为现行缺陷 |
| OBS-M021-B5-06 | V428 工作区未跟踪 | 阶段 B 不提交；Flyway 已从 classpath 应用并 validate |

---

## 10. 覆盖完整性与 PASS/FAIL

- 计划 5 文件全文覆盖：是（103 行）
- V428 修复产物全文覆盖：是（13 行 / 1215 bytes）
- 已应用五文件零改写：是（hash-object = after tree）
- 18 条映射判据均有适用/不适用证据：是
- 三线均执行：是
- 声称均有四类依据之一：是（HTML / mvn / psql / Flyway validate）
- 未答 questions：无
- 未定稿新缺陷：无
- 001 回归：已修（类型 2+4）
- 002：L587-588 下仍只登记
- 哈希行：本报告第一行
- 未读本轮 self-raw：是
- 未修代码/HTML/lock/findings/ledger：是

**本席 B5-r1 结论：PASS。**

不修复、不改 ledger/findings、不等席甲互读。合取以两席本次完整报告落齐后由调度席按双 PASS 规则判定。本票不等于模块 `PASS` 收口。
