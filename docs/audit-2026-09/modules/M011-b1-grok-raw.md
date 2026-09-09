CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M011 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M011 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 3 文件，计划冻结有效内容行 1342；当前工作区含 M009 三批与 M010 一批 B3 新增测试，实算 `\n` 计数 **1656**
- 冻结判据：87 条（`M011 → [BEQR-063 … PGM-066]`，与锁映射行逐字相同；COUNT=87 unique=87 MISSING=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M011-001、GR-M011-002、GR-M011-003、GR-M011-004
- 只登记、建议队列「否」：GR-M011-005
- 添加项：无（不代用户回答 2.2）
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员
- 本报告不表示全仓或发布门禁通过。M001–M010 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试/接口深度；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（现有测试绿在错误断言上 + psql BEGIN 违例）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；测试绿≠行为对 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B1 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 测试 5.8s 内结束，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11
- 映射行 `M011 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]` 与开工口径 87 项逐字相同
- `plan/modules.md` M011 FILES 3 条与用户清单逐字相同
- 3 文件 `git ls-files -z -- <3 paths> | xargs -0 cat | wc -l` → **1656**（计划冻结 1342 为切分时数字；工作区相对 origin/main `+343 / -29`，属 M009/M010 B3 已双 PASS 未提交测试增量，不构成越界切分缺陷）
- 3 文件字节/SHA256 与用户固定清单逐文件 `match=True`

HEAD：`main` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`（`git fetch origin` 后 `rev-parse` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。工作区含 M009 与 M010 已双 PASS 的 B3 修复（未提交），本席只读消费方、未改那些文件。

ledger 只读核验（不改）：V2.28，430694 bytes，SHA256 `f6883226b8ddae397e6e39e50baa6445ce7bdd9d1e09825ed7f80c9ad4ce4441`；M011 行 IN_PROGRESS、B3=0。findings V1.33，500770 bytes，SHA256 `0d3313ea9b3a4fca5c33d75466e4ab0ce006537774dc25a850974ada33e1fdb9`，仅作已登记边界背景（M009-010 / 017 / 026 / 003 / 005），不代替本模块审计。

三份 HTML 只读、未改。锁内 BEQR-143–155 来源行对本席读取的现行 HTML **命中**（`business-enum-quick-reference-v0.1.html:226–238`）；BSM-004 命中 `:566`；PGM-018/019 命中 `platform-governance-map-for-operations-v0.1.html:415/424`。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

## 2. 3 文件覆盖表（全文已读；行=splitlines=\n 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | 351 | 15915 | cbaec207daf3fc89fd72d42aa3fa42cccbefa7beb2e4eb16294685658cfeeb78 | 已读 L1–L351（10 @Test） |
| 2 | backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 495 | 23380 | 247409ab89bcdc71a2b5aa616c654620d37625ba3993287e94cdbec5fb9dd738 | 已读 L1–L495（16 @Test） |
| 3 | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 810 | 39637 | 8775e50a0c61c7d4ef48f942e278f707a2a1836e991ca29084bbddd1251c3448 | 已读 L1–L810（31 @Test；含内部 InMemoryPlatformRepository L684–810） |

三文件均属本模块可修范围。被测源码属 M009 已闭环：`PlatformService` / `PlatformController` / `JdbcPlatformRepository` / `PlatformRepository` 只读追数据流，发现的源码新缺陷登记为跨模块观察，不在本模块修。

相关只读依赖（不扩成他模块全量审计）：M009 未提交 B3 后的服务层六类合作方式/三角色白名单与软删；M010 V410（`parent_entity_id` 已删、`chk_be_entity_role` 三值、角色组织隔离）；`AuthorizationService.requirePlatformScope`；`TestSecurityActors`；`MigrationVersions.latest()`。M001–M010 不重开。

## 3. 数据流（测试 → M009 后台 → SQL/迁移/夹具 → HTML）

### 3.1 活库连接（类型 4）

仅从 `~/.complex/complex.env` **抽出三个** `COMPLEX_DATABASE_*`，把 URL 库名改写为 `complex_test`，不 `source` 整个 env，不打印密文。`psql`：`current_database=complex_test`，`current_user=complex`，host=127.0.0.1:5432。探针包在 `BEGIN…` 违例后连接结束回滚。未跑清理脚本。事后删除本席临时 env 文件。

### 3.2 Flyway 实态（类型 4）

| 项 | 实算 |
|---|---|
| complex_test 当前版本 | **410** |
| DbValidate（JDBC 套件启动日志） | Successfully validated 378 migrations |
| DbMigrate | Current version 410；Schema up to date. No migration necessary |
| `parent_entity_id` 列 | 0 |
| `idx_business_entities_parent` | 0 |
| `chk_be_entity_role` | `platform` / `project_company` / `management_company`；不含 `spv`/`operation` |
| 旧库 `complex_test_pre_v410_20260909` | 仍存在（M009-017 环境红已随重建消失，本席复验） |
| merchants CHECK | `chk_merchant_delete_status`（none/pending/approved）；`chk_merchants_business_format`（六业态）；两列 PII 信封。**无** `cooperation_form` CHECK，**无** `merchants.status` CHECK |

独立 `BEGIN` 插入 `entity_role='operation'` 与 `'spv'` 均 `violates check constraint "chk_be_entity_role"`；事后 `leftover=0`。

### 3.3 三层测试各打哪一段（GEN-003 关键）

```
HTTP  → PlatformControllerTests（WebMvcTest，PlatformService Mockito；不进服务实现、不进库）
服务  → PlatformServiceTests（内存 InMemoryPlatformRepository；不进 JDBC）
仓库  → PlatformJdbcIntegrationTests（JdbcPlatformRepository + complex_test；不进 PlatformService）
```

生产路径是 Controller → Service → Jdbc。本模块没有任何用例沿该全链走「审批删除」。因此内存 fake 的物理 `remove` 与 JDBC 的 `UPDATE delete_status='approved'` 可以同时绿，却锁住相反业务口径。这是本席 FAIL 的主因，不是「测试通过所以对」。

`JdbcPlatformRepository.deleteMerchant`（M009，只读）：`UPDATE merchants SET delete_status = 'approved' …`。`listMerchants(true)` / `listMerchantsPaged(true, …)` 的 WHERE 为 `(:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))`，故 `includeArchived=true` **含**已审批删除行。`PlatformRepository` 默认注释（M009）写明内存 fake 亦应软删、行保留。本模块内部 fake 未遵守该契约。

## 4. 三线审计（逐文件）

### 4.1 PlatformControllerTests.java（L1–L351）

**第一性原理**

1. 为什么必须存在：删除后 BSM-072/PGM-027 的读端平台作用域、GEN-005 权限码、GEN-010 联系电话掩码、四类主体绑定 HTTP 入口将无控制器层可失败断言。有业务上位。
2. 声称的行为是否实际执行：10 个 @Test 经 `mvn -Dtest=…PlatformControllerTests` 全绿（10/10）。`listingMerchantsRequiresPlatformScope` 打到 `PLATFORM_SCOPE_REQUIRED`（日志 `商户账号无权查看平台级经营分析`）。`creatingMerchantWithoutManagePermission` 打到 `PERMISSION_DENIED`。PII 两条分别断言掩码与明文。绑定 GET/POST/change 校验权限码。未认证列表 `status isIn(401,403)` 可失败（200 会红）。
3. 未验证前提：写路径的 `requirePlatformScope` 在 Controller 上多数不调用、依赖被 mock 掉的 Service，故控制器测试删掉服务层作用域也不会红。`fixed_rent_plus_commission` 作为 mock 返回值被断言为 JSON 合同字段（见 GR-M011-002）。archive/unarchive/delete-request/delete-approve 四条 HTTP 无测试（见 GR-M011-003）。GET `/business-entities` `/projects` `/spaces` `/space-types` `/venue-business-hours` 的平台作用域无测试（见 GR-M011-004）。

**顺从式**：假定意图是「控制器鉴权 + 响应形状」。与 HTML 读端「商户不能看平台全局」部分对齐（仅商户列表负例 + 绑定列表 verify）。不覆盖 BSM-004 存档删除 HTTP。不覆盖写端作用域。合作方式 mock 值不在六类之内。

**对抗式**：伪造商户作用域读平台商户列表 → 有测试。伪造无权限建商户 → 有测试。伪造缺幂等键 → 有测试。伪造无 PII 权限读电话 → 有测试。伪造未登录 → 有测试。缺：商户作用域打其它治理 GET；缺：存档/删除 HTTP；缺：createMerchant 成功路径的电话掩码。

**ponytail-review**：`L70-77: shrink: isIn(401,403) 可收窄为当前 SecurityConfig 的确定码。保留亦可。` `L133: yagni: mock 用白名单外合作方式。换成 lease。`

### 4.2 PlatformJdbcIntegrationTests.java（L1–L495）

**第一性原理**

1. 为什么必须存在：删除后无法在目标库证明表/权限种子、分页、软删行保留、PII 信封、V410 父链列删除、角色 CHECK、组织隔离唯一约束。有业务上位（BSM-004/BEQR-143/144/PGM-018 + GEN-014）。
2. 声称的行为是否实际执行：16/16 全绿。Flyway 410=410（M009-017 环境红本席复验已消失）。`approvedDeletedMerchantStaysQueryableWithIncludeArchived` 断言 `findMerchantById` 仍在、`listMerchants(true)` 含、`listMerchants(false)` 不含，与 JDBC 软删及 BSM-004「只隐藏不抹除」一致，**可失败**。`parentEntityColumnIsRemovedFromBusinessEntities` 列/索引 count=0，与 PGM-018「不是隶属链」及 V410 一致。`entityRoleCheckRejectsSpvAndOperation` 约束定义不含 spv/operation，并 INSERT spv 期望违例。PII 三条打信封前缀、二次加密、明文 CHECK。绑定第二条 active 打 `uq_business_entity_bindings_active_merchant`。分页第二页 id 不与第一页重叠。
3. 未验证前提：所有 `saveMerchant` 绕过服务层白名单，写入 `fixed_rent_plus_commission`/`draft`/`revenue_share`（库无对应 CHECK，故不会红）。`entityRoleCheckRejectsSpvAndOperation` 因同一事务只能一条违例 INSERT，未 INSERT `operation`（本席另用 psql 补证 operation 亦拒）。不经 PlatformService.approveDeleteMerchant，故与服务测试的相反断言互不碰撞（见 GR-M011-001）。

**顺从式**：仓库层与 V410/PII/软删 SQL 一致。与服务层枚举白名单不一致（GR-M011-002）。权限种子断言覆盖治理码与 platform_admin 子集，不断言「全部日常功能」。

**对抗式**：直写明文电话 → CHECK 拒。直写 spv → CHECK 拒。同组织同名角色 → DuplicateKey。跨组织同名 → 可插入。父链列 → 不存在。软删后再默认列表 → 不可见。未测：恢复（unarchive）；未测：cooperation_form 非法值被库接受（事实：无 CHECK）。

**ponytail-review**：`L22: yagni: password 默认 complex_dev_pwd。测试文件约定，不升缺陷。` `L69-72: delete: 白名单外合作方式/状态。换成 lease/active。`

### 4.3 PlatformServiceTests.java（L1–L810，含内部 fake）

**第一性原理**

1. 为什么必须存在：删除后服务层六类合作方式、商户/项目状态、三角色、绑定存在性、存档隐藏、占用拒删、最高权限审批、幂等、空间类型硬校验将无单测。有业务上位。
2. 声称的行为是否实际执行：31/31 全绿。`createMerchantRejectsCooperationFormOutsideSixModes` 用 `fixed_rent_plus_commission` 作**非法**值，断言 `MERCHANT_COOPERATION_FORM_INVALID`，与 BEQR-148 及服务白名单一致（此条有效）。`createMerchantRejectsStatusOutsideEnumeratedStates` 拒 `draft` 商户状态。`createBusinessEntityRejectsRoleOutsideWhitelist` 拒 `spv`。`approveDeleteByNonMasterIsForbidden` / `requestDeleteByMasterAuthorityExecutesWithoutApproval` 覆盖最高权限闸。`archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 覆盖「下线就不能再用」。`createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant` 覆盖 BEQR-143 唯一绑定。
3. 未验证前提：内部 `deleteMerchant` 做 `merchants.remove`（L721–723），与接口契约/JDBC/HTML 相反。`approveDeleteRemovesArchivedMerchantWhenNoReferences`（L362–376）对 `listMerchants(true)` 断言 `noneMatch`——若 fake 改为软删（HTML/JDBC 正确行为），**本断言会红**。即：当前断言在错误实现上绿、在正确实现上红，不满足 GEN-003「能失败的断言」指向目标行为。`setMerchantArchiveReason` 空实现（L685）。无 `unarchiveMerchant`。无 `decision=rejected`。`@BeforeEach` 一律 `asPlatformAdmin()`，从不用 `TestSecurityActors.asMerchantUser` 打写路径作用域负例。方法名 `createMerchantRejectsBusinessFormatOutsideFiveValues` 称五值，服务与库均为六值。

**顺从式**：服务白名单与 HTML 六类合作方式、BEQR-154/155、PGM-018 三角色、BSM-004 存档隐藏/占用检查/最高权限 大部分对齐。删除「行仍可查可恢复」被 fake 与错误断言推翻。

**对抗式**：非法合作方式 → 拒（有效）。非法商户状态 → 拒。缺主体绑定 → 404。已存档再被引用 → MERCHANT_ARCHIVED。非最高权限审批 → 禁。占用 → MERCHANT_IN_USE。幂等重复建主体 → saveCount=1。slash 碰撞 id 不覆盖。缺：取消存档；缺：驳回删除；缺：商户作用域写治理；缺：审批删除后 includeArchived 仍可见（被反断言）。

**grilling 追问树（内部自答，未问操作员）**

- Q1 删除是否只隐藏不抹除？HTML 是。JDBC 是。内存 fake 否。服务测试把「抹除」锁成期望。→ GR-M011-001
- Q2 首轮正常会点「取消存档」吗？会。无测试。→ GR-M011-003
- Q3 夹具能否写下服务层拒绝的合作方式/状态？JDBC 能（无 CHECK）。→ GR-M011-002
- Q4 商户账号打治理写/读会不会在服务单测红？不会，从未注入 merchant actor。→ GR-M011-004
- Q5 自审闸 `MERCHANT_SELF_APPROVE_FORBIDDEN` 首轮会撞上吗？最高权限申请会直接软删，不进 pending。要两三件事（同一人先非最高权限申请再变最高权限审批）才走。按铁律 6 **不升缺陷**，观察 OBS-03。
- Q6 并发双 active 绑定？顺序路径有服务+库约束。并发属第二件不巧。观察 OBS-04。

**ponytail-review**：`L321: shrink: 方法名 FiveValues。改 SixValues。` `L706: delete: 「拒硬删」措辞。改软删。` `L721-723: delete: remove。改为标记 approved。` `net: 以行为修正为准，不单删测试。`

**codebase-design**：内部 fake 是 PlatformRepository 缝上的第二适配器。一个适配器（JDBC）软删、一个（内存）物理删，缝是真的，但测试把错误适配器的行为当成服务层规格。深度应落在「软删+includeArchived」接口，而不是 `Map.remove`。

## 5. 87 冻结判据核对

说明：本模块是**测试**。判据「覆盖」= 测试实际执行该锚点目标行为且断言能失败并与 HTML 一致。「缺口」= 本模块表面应当覆盖却缺失或断言反了。「无实现面」= 三文件不承载该锚点业务，未发明相反规则。无实现面不升冲突。

| ID | 标题 | 结论 | 证据 |
|---|---|---|---|
| BEQR-063 | 高危动作（酒吧） | 无实现面 | 三文件无沽清/免单/反结 |
| BEQR-074 | 隐私桌台权限 | 无实现面 | 无桌台授权 |
| BEQR-143 | 四类主体 | 覆盖（服务+JDBC 绑定唯一） | ServiceTests 一商户一 active；JDBC uq_…_active_merchant。经营对象=商户，三主体 id 经 seedEntity 存在性校验 |
| BEQR-144 | 经营主体关系 | 覆盖（JDBC 父链列删除） | parentEntityColumnIsRemoved；psql parent_col=0。符合「不是行政隶属链」 |
| BEQR-145 | 城市项目公司 | 无实现面（角色码有间接） | 无「资产公司兼任」语义测试；三角色白名单含 project_company 但服务测试未建该角色成功路径 |
| BEQR-146 | 运营公司职责 | 无实现面 | 无域职责收入测试 |
| BEQR-147 | 主要收入归属 | 无实现面 | 无票款归属 |
| BEQR-148 | 合作模式 | **缺口/冲突夹具** | 服务拒 `fixed_rent_plus_commission` 为合法负例；JDBC/Controller 把它（及 `revenue_share`）当可持久化正式字段。见 GR-M011-002。六类正例只测了 `lease`+空 |
| BEQR-149 | 职级 | 无实现面 | 无 M1–M10 |
| BEQR-150 | 终端十一端 | 无实现面 | 无终端开关 |
| BEQR-151 | 最终权限计算 | 无实现面 | 无拒绝优先公式 |
| BEQR-152 | 项目与敏感数据权限 | 部分 | PII 电话掩码/明文/信封有测；内部结算价等无 |
| BEQR-153 | 终端开关优先级 | 无实现面 | — |
| BEQR-154 | 综合体项目状态 | 覆盖 | createProjectRejectsUnknownStatusOrMerchant 拒 `zzz`；`draft` 作为项目状态出现在商户缺失用例（项目草稿合法） |
| BEQR-155 | 商户/运营主体状态 | **缺口** | 服务拒商户 `draft`；JDBC 仍写 `draft`。归档走 archived_at 轴有测。退出缓冲期无实现面 |
| BEQR-156 | 合同状态 | 无实现面 | JDBC 只断言 contract 权限码种子 |
| BEQR-157 | 员工状态 | 无实现面 | — |
| BEQR-158 | 设备状态 | 无实现面 | — |
| BEQR-159 | 权限状态 | 无实现面 | — |
| BEQR-166 | 合同审批链 | 无实现面 | — |
| BEQR-189 | 管理费方式 | 无实现面 | — |
| BSM-001 | 整体说明 | 无直接断言 | 测试未发明相反原则；策略可配不在本三文件 |
| BSM-003 | 系统通用能力 | 指向 004/007 | 见下 |
| BSM-004 | 存档/删除审批软删 | **FAIL 主锚点** | 存档隐藏有测；占用拒删有测；最高权限有测。软删行保留：JDBC 正、服务测试反。恢复/驳回/HTTP 无。见 GR-M011-001/003 |
| BSM-005 | 分步引导新建 | 无实现面 | 后台 UI 属 M012 |
| BSM-006 | 动态二维码 | 无实现面 | — |
| BSM-007 | 平台管理员 vs 最高权限 | 覆盖部分 | 非最高权限不能批删；最高权限申请直接执行。平台管理员权限种子有子集断言。人数上限无 |
| BSM-009 | 基础管理入口 | 无实现面 | 卡片文案 |
| BSM-028 | 基础管理主要功能 | 部分 | 商户/空间/绑定/合作方式有测；合同八类变更无 |
| BSM-029 | 和谁有关 | 无实现面 | 连接叙述 |
| BSM-072 | 商户在授权范围内 | **缺口** | 控制器仅测 GET merchants 作用域负例；服务写路径无商户主体负例。见 GR-M011-004 |
| BSM-074 | 和谁有关（商户） | 同 072 | 「不能看平台全局」只测一条 GET |
| BSM-114 | 各类入口 | 无实现面 | — |
| BSM-115 | 相互关系 | 无实现面 | — |
| BSM-120 | 已确认规则 | 无实现面 | 未发明相反拍板 |
| GEN-001 | 必要性与最小性 | 覆盖 | 三文件均有可验证用途；内部 fake 物理删不是多余抽象而是错误适配器 |
| GEN-002 | 可执行正确性 | **缺口** | 57/57 可执行，但服务删除路径与 JDBC/HTML 声称不一致 |
| GEN-003 | 测试有效性 | **FAIL** | 见 001：错误断言在错误 fake 上绿。关键分支存档恢复/作用域/写路径缺口 |
| GEN-004 | 信任边界 | 部分 | 服务校验名称/枚举/存在性；JDBC 夹具绕过 |
| GEN-005 | 授权与最小权限 | **缺口** | 控制器权限码有测；写端平台作用域未在服务单测失败 |
| GEN-006 | 数据完整性 | 部分 | 绑定唯一、占用检查有；删除原子性在 fake 上变成物理删 |
| GEN-007 | 并发与幂等 | 部分 | 主体幂等有；绑定并发无（顺序有）。观察 OBS-04 |
| GEN-008 | 接口与模式一致性 | **FAIL** | fake vs JDBC 删除语义；夹具枚举 vs 服务白名单；FiveValues vs 六值 |
| GEN-009 | 错误处理 | 部分 | 非法枚举 400；缺商户 404；CHECK 违例 JDBC 有 |
| GEN-010 | 安全与敏感信息 | 部分 | 电话掩码/信封/明文 CHECK 有；create 成功路径掩码无控制器测试 |
| GEN-011 | 可观测与审计 | 部分 | 服务测 audit 事件名；删除留痕理由未断言落库（fake 丢弃） |
| GEN-012 | 资源边界 | 部分 | 分页 size 上限 500；空间 LIMIT 500 vs findById |
| GEN-013 | 构建配置 | 部分 | JDBC 硬编码 127.0.0.1/complex_test/complex；密码走环境变量。观察 OBS-05 |
| GEN-014 | 迁移正确性 | 覆盖 | 表存在、Flyway latest=410、V410 列/CHECK/角色唯一。本模块不改历史迁移 |
| GEN-015 | 界面可用性 | 无实现面 | 无 UI |
| GEN-016 | 文档可验证性 | **缺口** | 「拒硬删」注释；FiveValues 方法名；与现行软删/六业态不符 |
| GEN-017 | 业务来源分类 | 本席已依序分类 | 见 §6；无把发明标成技术性 |
| GEN-018 | 第一性原理证据 | 本报告 | 结论落到 HTML/可执行产物/命令，不以 57 绿或席位意见为依据 |
| PGM-001 | 治理层定位 | 无实现面 | 叙述 |
| PGM-002 | 治理顺序 | 无实现面 | — |
| PGM-003 | 模块入口 | 无实现面 | 卡片 |
| PGM-004 | 综合体和集团 | 部分 | 父链删除测试支撑「非隶属」 |
| PGM-005 | 空间和点位 | 部分 | 空间层级/类型/状态有测 |
| PGM-006 | 商户和主体 | 部分 | 档案/绑定有测；见 148/004 缺口 |
| PGM-007 | 合同和准入 | 无实现面 | 仅权限种子 |
| PGM-008 | 员工和手机 | 无实现面 | — |
| PGM-009 | 权限和职级 | 部分 | 权限码种子；非职级规则 |
| PGM-010 | 终端开关 | 无实现面 | — |
| PGM-011 | 设备管理 | 无实现面 | — |
| PGM-012 | 商户侧 | 同 BSM-072 | — |
| PGM-013 | 开放接口 | 无实现面 | — |
| PGM-014 | 留痕复核 | 部分 | audit 事件；删除理由未落库断言 |
| PGM-015 | 楼宇运营 | 无实现面 | — |
| PGM-016 | 应急与隐私 | 部分 | 电话 PII；U 盘密钥最高权限有测 |
| PGM-017 | 当前一个项目 | 无实现面 | 无集团视图测试 |
| PGM-018 | 多对多非隶属 | 覆盖 | 父链列/索引删除；三角色 CHECK；服务拒 spv |
| PGM-019 | 各主体职责 | 部分 | 角色白名单；职责表无收入测试 |
| PGM-020 | 主要功能（项目） | 部分 | 建项目有测 |
| PGM-021 | 业务意义 | 无实现面 | — |
| PGM-026 | 商户主要功能 | 部分 | 绑定同步三列；合作方式夹具冲突 |
| PGM-027 | 业务边界 | **缺口** | 同 BSM-072/074，作用域覆盖不全 |
| PGM-031 | 经销商资金 | 无实现面 | — |
| PGM-035 | 提成发放 | 无实现面 | — |
| PGM-040 | 单一功能授权 | 部分 | 权限码；非模板装配 |
| PGM-041 | 权限规则 | 部分 | 作用域/权限码 |
| PGM-042 | 职级规则 | 无实现面 | — |
| PGM-043 | 外包与审计人员 | 无实现面 | — |
| PGM-044 | 桌台挂单经销权限 | 无实现面 | — |
| PGM-048 | 商户侧全开放 | 同 072 | — |
| PGM-060 | 会员生物信息 | 部分 | 仅电话 PII，非会员注册 |
| PGM-065 | 连接关系 | 无实现面 | 叙述 |
| PGM-066 | 已拍板规则 | 无实现面 | 未发明相反拍板 |

无实现面的判据：三文件未引入与 HTML 相反的业务规则，不因 HTML 沉默判冲突或删除。

## 6. Findings（临时 ID，正式号仅 B2 分配）

### GR-M011-001

- 席位临时编号：GR-M011-001
- 文件:行号：
  - `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java:706` 注释「>0 拒硬删」
  - 同文件 `:721-723` `deleteMerchant` → `merchants.remove(merchantId)`
  - 同文件 `:362-376` `approveDeleteRemovesArchivedMerchantWhenNoReferences` 对 `service.listMerchants(true)` 断言 `noneMatch`
  - 对照（同模块、相反口径）：`PlatformJdbcIntegrationTests.java:342-355` `approvedDeletedMerchantStaysQueryableWithIncludeArchived` 断言 `listMerchants(true)` **含**该 id、`listMerchants(false)` 不含
- 完整原文（fake）：

```
        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒硬删）。
        private long merchantReferenceCount;
        ...
        public void deleteMerchant(String merchantId) {
            merchants.remove(merchantId);
        }
```

- 完整原文（错误断言）：

```
        assertThat(decision.deleted()).isTrue();
        assertThat(service.listMerchants(true).items())
                .noneMatch(m -> m.id().equals(merchant.id()));
```

- HTML 原文（BSM-004，`business-system-map-for-operations-v0.1.html:571`）：
  > 这里的"删除"不是真从数据库里抹掉，而是把数据转入后台留存、前台各处不再显示，数据始终留在库里可查、可审计、可恢复
- 被测 JDBC（M009 只读，`JdbcPlatformRepository.java:192-194`）：`UPDATE merchants SET delete_status = 'approved' …`；`listMerchantsPaged` includeArchived=true 含 approved 行。
- 业务影响：服务单测把「物理消失」锁成规格。若 B3 把 fake 改成与 JDBC/HTML 一致的软删，该测试会红；若按该测试去改 JDBC，会破坏已闭环的 M009-010 软删与「已存档内容可查」。首轮正常「审批删除后在已存档页还能看到并恢复」会被服务测试拒绝。
- 可复现命令/最小反例（类型 2+4）：
  1. `mvn -Dtest=com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformJdbcIntegrationTests test` → 两套件均绿（31+16）。
  2. 同一 `deleteMerchant` 语义：服务测试要求 `includeArchived=true` 列表**没有**该商户；JDBC 测试要求**有**。
  3. 只读对照：`JdbcPlatformRepository.deleteMerchant` 为 UPDATE 非 DELETE；psql `chk_merchant_delete_status` 含 `approved`。
- 冻结判据：BSM-004、GEN-003、GEN-008、GEN-002、GEN-016
- 2.3 依据类型：1（HTML 逐字）+ 2（两套件实际执行且双绿）+ 4（命令与行号）
- 四分类：**冲突**（与 BSM-004「只隐藏不抹除、可查可恢复」明文矛盾；服务测试把抹除写成期望。判定删除该错误期望与物理 `remove`，改为标记 `delete_status=approved` 且 `listMerchants(true)` 仍含、`listMerchants(false)` 不含，与 JDBC 测试对齐）
- 工程缺陷：成立
- 建议入队：是
- 最小处置：改内部 fake 的 `deleteMerchant` 为保留行并置 `approved`（`findMerchantById` 仍命中）；把 `approveDeleteRemovesArchivedMerchantWhenNoReferences` 的 `listMerchants(true) noneMatch` 改为 `contains` + `listMerchants(false) noneMatch`（与 JDBC 用例同口径）；注释「拒硬删」改为软删。TDD：先改断言见红，再改 fake 见绿。不改 M009 源码。

### GR-M011-002

- 席位临时编号：GR-M011-002
- 文件:行号：
  - JDBC `PlatformJdbcIntegrationTests.java:69-72, 142-143, 164, 393, 415`：`saveMerchant` 直写 `cooperation_form="fixed_rent_plus_commission"`、`status="draft"`；更新路径再写 `"revenue_share"`
  - Controller `PlatformControllerTests.java:133,157,169`：mock `MerchantRecord` 的 `cooperation_form` 为 `fixed_rent_plus_commission` 并断言 JSON 同值
- HTML 原文（BEQR-148，`:231`）：
  > 自营、租赁、联营、代运营、服务商接入、其他
- HTML 原文（BEQR-155，`:238`）：
  > 待准入、正常、暂停、终止、归档、黑名单
- 服务层对照（M009 只读，本模块服务测试已作为**负例**使用）：`MERCHANT_COOPERATION_FORMS={self_operated,lease,joint_operation,managed_operation,service_provider,other}`；`SUBJECT_STATUSES` 不含 `draft`（草稿只属项目 BEQR-154）。`createMerchantRejectsCooperationFormOutsideSixModes` 对 `fixed_rent_plus_commission` 期望 `MERCHANT_COOPERATION_FORM_INVALID`；`createMerchantRejectsStatusOutsideEnumeratedStates` 对商户 `draft` 期望 `VALIDATION_FAILED`。
- 业务影响：仓库「正式字段持久化」用例把收费模板词/分成词写成合作方式，把项目草稿写成商户状态。库无 `cooperation_form`/`merchants.status` CHECK（类型 4：`pg_constraint` 仅 delete_status、business_format、PII），故直写永绿，不能抓住与服务白名单的分叉。控制器 JSON 把非法合作方式标成 API 形状的一部分。
- 可复现：`rg "fixed_rent_plus_commission|revenue_share" backend/src/test/java/com/complex/platform/governance/Platform*Tests.java`；JDBC 16/16 绿；psql merchants CHECK 列表无 cooperation_form。
- 冻结判据：BEQR-148、BEQR-155、PGM-026、GEN-008、GEN-003
- 2.3 依据类型：1 + 4（grep/psql）+ 2（服务负例与 JDBC 正例同时绿）
- 四分类：**细化**（上位=建立商户档案/区分六类合作方式；夹具是实现细节）。**不是添加**：未引入第六类之外的新合作模式作为产品规则，而是测试双缝用错已有词表。**不是技术性**：夹具里的业务枚举不在无条件豁免之列。工程上必须把夹具改成白名单词，否则 GEN-008 持续分叉。
- 无业务发明理由：不适用（本条非技术性）
- 工程缺陷：成立
- 建议入队：是
- 最小处置：JDBC/Controller 夹具 `cooperation_form` 改为 `lease`（或六类之一），商户 `status` 改为 `active`/`pending_admission`；保留服务测试把 `fixed_rent_plus_commission`/`draft` 当非法值。不新增库 CHECK（属迁移模块，除非 B2 另定）。

### GR-M011-003

- 席位临时编号：GR-M011-003
- 文件:行号：三文件均无 `unarchiveMerchant` / `decision","rejected"` / HTTP `archive|unarchive|delete-request|delete-approve` 成功路径。`PlatformServiceTests.java:685` `setMerchantArchiveReason` 空实现。控制器有对应映射（M009 `PlatformController.java:88-121`，只读）。
- HTML 原文（BSM-004 `:569/:571/:576`）：
  > 想恢复点"取消存档"即可
  > 数据始终留在库里可查、可审计、可恢复
  > 存档及删除都需要填写理由
- 业务影响：首轮正常会走「存档 → 取消存档」「申请删除 → 最高权限驳回」「已存档页恢复」。本模块作为后台验证段未执行这些目标行为。理由字段被 fake 丢弃，断言永不失败。与 GR-M011-001 叠加后，服务层甚至无法表达「删除后恢复」。
- 可复现：`rg "unarchive|asMerchantUser|\"rejected\"" backend/src/test/java/com/complex/platform/governance/Platform*Tests.java` → 无业务命中（仅 idempotency 字符串含 `delete-approve`）。
- 冻结判据：BSM-004、GEN-003、GEN-011
- 2.3 依据类型：1 + 4
- 四分类：**细化**（BSM-004 下位：恢复/驳回/理由持久化/HTTP 面的测试缺口）。HTML 已有条目，非添加。
- 工程缺陷：成立
- 建议入队：是
- 最小处置：服务层最少各一条可失败断言：`unarchiveMerchant` 使默认列表重新可见；`approveDelete(decision=rejected)` 后 `deleteStatus=none`；fake 实现 `setMerchantArchiveReason` 或断言 `deleteReason`。控制器最少一条 archive 或 delete-approve 的权限/幂等。不扩 M009 源码。

### GR-M011-004

- 席位临时编号：GR-M011-004
- 文件:行号：
  - `PlatformServiceTests.java:65-66` `@BeforeEach` 一律 `TestSecurityActors.asPlatformAdmin()`；全文无 `asMerchantUser`
  - `PlatformControllerTests.java:246-254` 仅 GET `/platform/merchants` 作用域负例；`:241` 绑定列表 verify `requirePlatformScope`
  - 控制器其它 GET（`/business-entities` `/projects` `/spaces` `/space-types` `/venue-business-hours`）均有 `requirePlatformScope`（M009），无对应测试
- HTML 原文（BSM-072 `:1310` / BSM-074 `:1331` / PGM-027）：
  > 但不能看平台全局和其他商户数据
  > 商户不能看平台全局和其他商户数据
- 业务影响：F-AUTHZ-02 写路径 `PlatformService.*` 首行 `requirePlatformScope()`。服务测试永远平台管理员，删掉该行 31 个用例仍绿。读端只锁了商户列表与绑定列表两条。首轮商户账号打 GET `/projects` 或 POST `/merchants` 的回归无本模块网。
- 可复现：`rg asMerchantUser PlatformServiceTests.java` 空；现有 `listingMerchantsRequiresPlatformScope` 10/10 中仅 1 条负例。
- 冻结判据：BSM-072、BSM-074、PGM-027、GEN-003、GEN-005
- 2.3 依据类型：1 + 4
- 四分类：**细化**（平台作用域守卫的测试覆盖是 BSM-072 下位实现）
- 工程缺陷：成立
- 建议入队：是
- 最小处置：服务层一条 `asMerchantUser` 调 `createMerchant`/`listMerchants` 期望 `PLATFORM_SCOPE_REQUIRED`。控制器将同一负例扩到至少一个其它治理 GET（如 projects 或 business-entities）。不必为每个 verb 各写一条（铁律 6：首轮会撞上的路径覆盖即可）。

### GR-M011-005

- 席位临时编号：GR-M011-005
- 文件:行号：`PlatformServiceTests.java:321-329` 方法名 `createMerchantRejectsBusinessFormatOutsideFiveValues` 与注释「五值之外的业态一律拒收」
- 完整原文：

```
    // V348 商户业态标准字段：五值之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。
    @Test
    void createMerchantRejectsBusinessFormatOutsideFiveValues() {
```

- 事实：`PlatformService.MERCHANT_BUSINESS_FORMATS` 与 `chk_merchants_business_format` 均为六值 `bar/catering/retail/amusement/media/other`（psql 已证）。用例本身用 `tenant` 作非法值，断言仍能失败，行为与 HTML「酒吧、餐饮、零售、游乐、演出传媒、其他」不冲突。
- 冻结判据：GEN-008、GEN-016、BSM-072（业态六值）
- 2.3 依据类型：4（方法名/注释 vs CHECK 定义）
- 四分类：**技术性**
- 无业务发明理由：未新增第七业态，也未把五值写成产品规则去执行；只是测试名称/注释计数错误。无 HTML 冲突。
- 工程缺陷：成立（误导后续席）
- 建议入队：否（登记；若本批已因 001–004 进 B3，可顺手改名，不单独占用修复配额理由）
- 最小处置：方法名/注释改为六值。

## 7. 观察（无新根因，不漏并）

- **OBS-01**（承接 M009-010/026 边界，本席独立复验）：内存 fake 物理 remove vs JDBC 软删分叉成立，且服务测试反断言已把它锁死。根因写入 GR-M011-001，不另开 ID。注释「拒硬删」同根因。
- **OBS-02**（承接 M009-017）：`complex_test` Flyway=410，`MigrationVersions.latest()` 断言绿；旧库名保留。环境红本席复验已消失。非本三文件缺陷。
- **OBS-03**：`MERCHANT_SELF_APPROVE_FORBIDDEN` 在「最高权限申请即直接软删」下，首轮正常不走到审批自审。按铁律 6 不升缺陷、不加测试闸。
- **OBS-04**：无并发双插 active 绑定测试。顺序路径服务 `ACTIVE_BINDING_EXISTS` + 库 `uq_business_entity_bindings_active_merchant` 已覆盖首轮双点。不升缺陷。
- **OBS-05**：JDBC `@SpringBootTest` 硬编码 `127.0.0.1:5432/complex_test` 与用户 `complex`，密码 `${COMPLEX_DATABASE_PASSWORD:…}`。符合本仓测试库约定，不升缺陷。
- **OBS-06**：`entityRoleCheckRejectsSpvAndOperation` 只 INSERT `spv`。约束定义 `doesNotContain operation`；本席 psql INSERT operation 亦拒。覆盖充分，不另开缺口。
- **OBS-07**：绑定夹具 id `be_operation_*` 是主体主键字符串，不是 `entity_role=operation`。M010-003 后角色直写已改 `management_company`。不构成残留角色发明。
- **OBS-08**：无 `PlatformService`+`JdbcPlatformRepository` 联立用例。001 已覆盖该缝上的行为冲突。不另开 ID。
- **OBS-09** 跨模块（M009 已闭环，不修）：`PlatformController.createSpaceType` 无 `requireIdempotencyKey`（L156–160），其它写入口有。供全局收口，不入本模块队列。
- **OBS-10**：`createBusinessEntityRejectsRoleOutsideWhitelist` 只拒 `spv`，未拒 `operation`、未正例 `platform`/`project_company`。服务白名单三值 + 库 CHECK 已由其它用例/本席探针支撑。不升缺陷。
- **OBS-11**：57/57 BUILD SUCCESS **不能**当作 GEN-003 通过。本席 FAIL 不依赖测试红。
- **OBS-12**：findings V1.33 所记 M009-003/005 夹具直写、M009-010 fake/Jdbc 分叉、M009-026「拒硬删」注释，本席均在现行 3 文件复验仍在（operation 角色直写已随 M010 消失）。本席 ID 独立，不复用 M009 号。

## 8. 命令证据（敏感值未输出）

```
# 锁
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# → b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# HEAD
git fetch origin && git rev-parse HEAD origin/main
# → 242f487233d028173cb9198eecd079f5d877405c 三值相等

# 三文件哈希（python hashlib）与用户清单 match=True；wc -l 并集 1656

# 活库只读
psql … -d complex_test
# current_database=complex_test; flyway version=410; parent_col=0; idx_parent=0
# chk_be_entity_role = platform|project_company|management_company
# old_db_kept=t for complex_test_pre_v410_20260909

# BEGIN 探针（违例，无残留）
INSERT … entity_role='operation' → chk_be_entity_role
INSERT … entity_role='spv' → chk_be_entity_role
leftover=0

# 测试（仅三个 COMPLEX_DATABASE_*；JDBC 套件打 complex_test）
cd backend && mvn -Dtest=com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformJdbcIntegrationTests test
# PlatformJdbcIntegrationTests  Tests run: 16, Failures: 0, Errors: 0
# PlatformServiceTests          Tests run: 31, Failures: 0, Errors: 0
# PlatformControllerTests       Tests run: 10, Failures: 0, Errors: 0
# Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS  Total time: 5.260 s
# Flyway: validated 378 migrations; schema public version 410; no migration necessary
```

未跑任何清理共享库脚本。未写永久测试。临时 env 已删。

## 9. 结论

**FAIL**

同时满足：三文件全文覆盖记录；87 判据均已核对（含无实现面原因）；三线均执行；声称均有 2.3 四类证据之一；无未答 questions；Grok 哈希行有效。

不得 PASS 的原因：存在未定稿工程缺陷 GR-M011-001（冲突，GEN-003 断言指向错误行为，否决级）以及 GR-M011-002/003/004（细化，建议入队）。无有条件通过。无未闭合待问却标 PASS。添加项无，不进入 BLOCKED-ASK。

建议 B2：001–004 入修复队列；005 登记否；跨模块 OBS-09 不进本模块队列。B3 只修本模块 3 个测试文件，不重开 M009 源码。

完成判据（本 raw）：指定路径已写入本次完整报告；第一行为独立实算锁哈希；结论二元且为 FAIL。

## M011 B2-r1 整批候选复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 全文 43085 字节原样保留，不改写 B1 锁段起点，不覆盖 B1 **FAIL** 原文）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，FILES 3 / 判据 87）
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；ledger V2.29 IN_PROGRESS；B3=0；无源修复
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲。
- 本席 B2：**FAIL**（否决点仅 005 并入/入队；001–004、006、007 票 PASS）
- 本追加段不写独占锁字段名。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；COUNT=1。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未重跑 57 用例（三文件指纹与 B1 表全等，B2 只核差集）。

此票只判断 findings.md 顶部 V1.34「当前 M011 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M011-b1-grok-raw.md` 前缀（本席 B1） | V0.01 | 43085 | `ce463f1f601d4bad08eb2d535b7aa2c5df1bdf905b5ee5d2142db9759997871c` |
| `docs/audit-2026-09/modules/M011-b1-self-raw.md` | V0.01 | 13404 | `4ea215f5605d66177c036e113d3032b2d2587cd4b5a66dd2c02a609c0eff4313` |
| `docs/audit-2026-09/findings.md` | V1.34 | 508207 | `6ca8f0a61fccf2d0fadd1fb20683b73fc388760a57469c670d5ef6797d084d2c` |

ledger 只读：V2.29，431922 bytes，SHA256 `0490c786ba2d8c3ac9b5489afe8dbd41433ac2f5d0cc61614e1192a3af0e69fe`；M011 IN_PROGRESS、B3=0。三测试文件指纹与 B1 覆盖表逐文件全等（Controller 351/15915/`cbaec207…feeb78`；JDBC 495/23380/`247409ab…9dd738`；Service 810/39637/`8775e50a…1c3448`）。工作区相对 HEAD 含 M009/M010 已闭环未提交修复，本三文件无相对 B1 的漂移。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问结构自查事实；不等操作员、不派子代理 |
| verification-before-completion | 声称前先跑只读哈希/行号核对；本票不声称 57 用例本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：内部 fake 是第二适配器，深度落在软删+includeArchived 缝，不落 Map.remove |
| ponytail-review | 只评最小删除/收窄，不落地。005 缺键两条=yagni（createMerchant 已测共享闸） |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理并另写 `docs/arc/audits/`，与双席冲突；**未用** |

### 完整差集（GR-001…005、OBS-01…12、席甲 CX-01…05、席甲 OBS-01…06）

漏并：无。本席 B1 全部临时 ID 均已进入 001–007。席甲 B1 未发现 GR-003，候选 003 按本席原意收入，不是席甲发明、也不是改写本席。无锁外新判据、无新永久 ID、无 HTML 自身问题上报、无第三主体、无添加项、不进 BLOCKED-ASK。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M011-001 | GR-M011-001 + 席甲 CX-01/05（+M009-010/026） | 冲突/是/是 | **PASS（冲突/是/是）** |
| M011-002 | GR-M011-002 + 席甲 CX-02（+M009-003/005） | 细化/是/是 | **PASS（细化/是/是）** |
| M011-003 | GR-M011-003（席甲 B1 未列） | 细化/是/是 | **PASS（细化/是/是；理由断言收窄见下）** |
| M011-004 | GR-M011-004 + 席甲 CX-03 | 细化/是/是 | **PASS（细化/是/是）** |
| M011-005 | GR-M011-005 + 席甲 CX-04 | 技术性/是/是（改名+两条缺键） | **FAIL（并入与入队不合规）** |
| M011-006 | GR OBS-09（跨模块） | 技术性（跨模块）/是（属 M009）/否 | **PASS（否/否；缺陷口径见下）** |
| M011-007 | GR OBS-02/03/04/05/06/07/08/10/11/12 + 席甲 OBS-01…06 | 技术性/正向 /否/否 | **PASS（否/否）** |

冲突 1（001）。细化入队 3（002、003、004）。技术性入队：候选把 005 写成是，本席否决该并入后 005 仅改名可随批、CX-04 缺键归 007。否 2（006、007）。添加 0。

候选把本席观察写错或写窄之处（007 不构成 FAIL；005 构成 FAIL）：

1. **005 并入改写本席原意**：本席 GR-005 只覆盖方法名/注释「五值」；建议入队否，并写明「若 001–004 已进 B3 可顺手改名，不单独占配额」。候选把席甲 CX-04（两条 `…RequiresIdempotencyAndManagePermission` 未测缺幂等键→400）并入同一 ID 并改为入队必须补两次请求。这不是归并，是把本席已写「伪造缺幂等键 → 有测试」的控制器面升格成新修复项。
2. **006 工程缺陷「是（属 M009 源码）」过强**：本席 OBS-09 是跨模块观察、不入本模块队列。同事实已在 M009-022 定稿为技术性/缺陷否（`IdempotencyService.execute` 仍必填，仅控制器未显式调用）。006 队列否成立；定稿不得把 M009-022 改写成「源码缺陷待修」。
3. **007 来源写「席甲 OBS-01…06」但事实栏未写入席甲 OBS-02（401/403）、OBS-03（空密钥）、OBS-04（`@Transactional`）、OBS-05（`seedMerchant` 以 `be_1` 作三列）**。不是把本席观察写错。本席 OBS-12「ID 独立、不复用 M009 号」由候选头「M009 转入 010/026→001、003/005→002」覆盖。

### 001 独立裁定（冲突／是；席甲 B1 原判技术性，本席维持冲突）

核源（只读）：

- `PlatformServiceTests.java:721-723` `merchants.remove(merchantId)`。
- 同文件 `:362-376` `approveDeleteRemovesArchivedMerchantWhenNoReferences` 对 `listMerchants(true)` 断言 `noneMatch`。
- 同文件 `:706` 注释「>0 拒硬删」。
- 对照 `PlatformJdbcIntegrationTests.java:342-355`：`deleteMerchant` 后 `findMerchantById` 仍在、`listMerchants(true)` 含、`(false)` 不含。
- 被测 `JdbcPlatformRepository.java:192-194`：`UPDATE merchants SET delete_status = 'approved'`，不是 DELETE。
- `PlatformRepository.java:22-31` 默认 `listMerchants(false)` 排除 `archived_at` 与 `delete_status=approved`；`includeArchived=true` 返回全量。接口注释写明内存 fake 亦应软删、行保留。
- HTML BSM-004（`business-system-map-for-operations-v0.1.html:571`）原句：「这里的"删除"不是真从数据库里抹掉，而是把数据转入后台留存、前台各处不再显示，数据始终留在库里可查、可审计、可恢复」。
- `MerchantRecord.withDelete(...)` 已存在（M009，只读）。审批通过路径 `PlatformService.approveDeleteMerchant` 在 approved 分支只调 `repository.deleteMerchant(id)`，**不**先 `saveMerchant(withDelete("approved"))`；最高权限申请路径才会先 save 再 delete。故 fake 必须在 `deleteMerchant` 内置 `approved` 并留行，不能改成空操作。

四分类：冲突成立。测试把「includeArchived 列表物理消失」锁成规格，与 BSM-004 明文相反，不是夹具风格问题。席甲 B1 标技术性，本席不改判、不以席甲 B2「按本席原句改冲突」为正确性证明。无业务发明：删除的是错误期望与物理 `remove`，不新造恢复页或清理工具。

最小处置（最懒、TDD 可先红后绿、不改 M009 源码）：

1. 先改 `approveDeleteRemovesArchivedMerchantWhenNoReferences`：`listMerchants(true)` 含该 id 且 `deleteStatus=approved`；`listMerchants(false)` 不含。现状 `remove` 下该断言必红。
2. 再改 fake `deleteMerchant`：命中行则 `merchants.put(id, existing.withDelete("approved", existing.deleteRequestedBy(), existing.deleteRequestedAt(), existing.deleteReason(), now))`，`findMerchantById` 仍命中。
3. `:706` 注释改为软删/占用则拒删除（含软删标记），与 M009-026 已改生产注释同口径。

票：**PASS**。

### 002 独立裁定（细化／是；含 revenue_share 更新路径）

核源：JDBC `saveMerchant` 退役码位点恰好为 `:69-72`（分页两条 `fixed_rent_plus_commission`+商户 `draft`）、`:142-143`（创建）、`:164`（更新写入 `revenue_share`）、`:201`（更新断言 `revenue_share`）、`:393`、`:415`（PII 两条，同退役码）。Controller `:133/:157/:169` mock 并断言 `fixed_rent_plus_commission`。同三文件其余 `fixed_rent_plus_commission`/`draft` 在 ServiceTests `:165/:180`，是**非法值负例**，必须保留。项目状态 `draft`（`:197/:247`）属 BEQR-154，不是商户状态，不动。`be_operation` 是主体主键字符串（OBS-07），不是角色码，不动。无第三种退役合作方式字面量。

分类细化成立（上位=六类合作方式/商户五态可设代码；夹具用错已有词表）。不是添加、不是技术性豁免。库无 `cooperation_form`/`merchants.status` CHECK，直写永绿，GEN-008 分叉要靠夹具改白名单才能锁。

最小处置：上列 JDBC/Controller 位点 `cooperation_form` 改 `lease`（或六类之一），商户 `status` 改 `active`/`pending_admission`；`:164/:201` 的 `revenue_share` 一并改，否则更新路径仍把收费模板词当合作方式。Controller JSON 断言随改。不新增库 CHECK。不改服务负例。

票：**PASS**。未漏同文件其它退役码。

### 003 独立裁定（细化／是；GR-003；不新增 fake 能力）

核源：三测试文件无 `unarchiveMerchant` / `decision=rejected` 业务命中。`setMerchantArchiveReason` 在 fake `:685` 空实现。服务层 `unarchiveMerchant:220-241`、`approveDeleteMerchant` rejected 分支 `:323-326` 均存在。控制器 archive/unarchive/delete-request/delete-approve `:88-121` 均有 `requireIdempotencyKey`，本模块无对应 HTTP 用例。

`MerchantRecord` **没有** `archiveReason` 字段；`archive_reason` 只经 Jdbc `:1108-1110` 写库列，不映射回 record。候选「fake 实现 `setMerchantArchiveReason` 并断言理由落到记录」会逼改 M009 record 或在测试里造旁路 map，都不是最懒、且后者测不到生产映射。本席 B1 原文是「fake 实现 *或* 断言 `deleteReason`」——后者可走已有 `withDelete`，不改被测源码。

最小处置（最懒、不发明业务、不改 M009）：

- 服务层 `unarchiveMerchant` 一条：存档后默认列表不可见，取消存档后默认列表重新可见（现有 `saveMerchant`/`withArchive` 足够，不需新 fake）。
- 服务层 `approveDelete(decision=rejected)` 一条：之后 `deleteStatus=none`（走 `saveMerchant(withDelete("none"))`，不调 `deleteMerchant`）。
- 理由一条：对 `requestDeleteMerchant` 返回值或随后 `findMerchantById` 断言 `deleteReason` 非空（`withDelete` 已写入）。不要为 archive_reason 新增 fake、不要给 MerchantRecord 加字段。
- 控制器一条 archive **或** delete-approve 的权限/幂等（缺键或无 manage/approve 码）。不要四条 HTTP 各写一条。

票：**PASS**。不需要新增 `setMerchantArchiveReason` fake 能力。

### 004 独立裁定（细化／是；一条服务负例+一条参数化读端）

核源：ServiceTests `:65-66` `@BeforeEach` 一律 `asPlatformAdmin()`；三文件测试无 `asMerchantUser`。`TestSecurityActors.asMerchantUser` 存在。Controller 七个治理 GET 均有 `requirePlatformScope`（`:46/:65/:129/:145/:152/:175/:193`）。本模块负例只打 GET `/merchants`（`:246-254` 403）；绑定列表 `:241` 为 verify 调用，不是 403。其余五个 GET 无测试。服务写路径 `createMerchant` 等首行 `requirePlatformScope()`；管理员夹具下删掉该行 31 用例仍绿。

分类细化成立。HTML BSM-072/074「不能看平台全局」是上位；覆盖是下位实现。无业务发明。

最小处置：服务层一条 `asMerchantUser(...)` 调 `createMerchant`（或 `listMerchants`）期望 `PLATFORM_SCOPE_REQUIRED`。控制器一条参数化用例、同一 `doThrow` 形态，覆盖其余五个治理 GET（`/business-entities` `/projects` `/spaces` `/space-types` `/venue-business-hours`）。这是一条方法不是每个 verb 一条；比本席 B1「至少一个其它 GET」略宽，仍最懒且能锁 M009-015 七读端回退。不要为 POST/PUT 每个 verb 再写作用域负例。绑定列表已有 verify，不必强行并进 403 参数化。

票：**PASS**。足以覆盖「首轮会撞上」，不为每个 verb 各写一条。

### 005 独立裁定（并入不合规 → 本 ID 票 FAIL）

核源：

- `createMerchantRejectsBusinessFormatOutsideFiveValues` `:321-329`：注释/方法名「五值」；用例以 `tenant` 为非法值，断言 `MERCHANT_BUSINESS_FORMAT_INVALID`，行为与六值 CHECK/`MERCHANT_BUSINESS_FORMATS` 不冲突。这是本席 GR-005 的全部范围。
- `creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed` `:100-112`：POST `/merchants` 无 `idempotency_key` → 400 `VALIDATION_FAILED`。本席 B1 §4.1「伪造缺幂等键 → 有测试」即此条。共享闸是 `requireIdempotencyKey`。
- `createBusinessEntityBindingRequiresIdempotencyAndManagePermission` `:258-292` 与 `changeBusinessEntityBindingRequiresIdempotencyAndManagePermission` `:295-325`：请求体**带**键且 200，只 `verify` `entity.binding.manage`。名称含 Idempotency，未打缺键 400。这是席甲 CX-04 的增量，**不是** GR-005。

四分类：FiveValues 技术性成立，无业务发明（不增第七业态）。CX-04 缺键未断言也是 GEN-016 名称声称，不是新业务规则。

队列：

- FiveValues：本席 B1 登记否；「001–004 已进 B3 则可顺手改名」。候选把 005 标「是（随批）」**仅就改名而言**与顺手条款相容，不单独构成否决。
- 把 CX-04 两条缺键请求并入 005 并列为必须修：**不合规**。理由：(1) 改写 GR-005 原意；(2) 缺键闸已有 createMerchant 用例，再按 verb 补两条违反本席 004「不为每个 verb 各写一条」与铁律 6；(3) 003 已要求一条 archive/delete-approve 权限/幂等，覆盖的是真正未测的 HTTP 面，不是绑定成功路径再插负例。

B2-r2 正确口径（本批不得按候选 005 定稿）：

- 005 保持技术性 / 缺陷是 / 队列是（随批）：**只**把方法名/注释改为六值。TDD 用该用例自身已能失败的非法值断言，不必新写测试。
- CX-04 两条缺键：归 007 否。可选随 005 把方法名去掉 `Idempotency` 字样（GEN-016），**禁止**各补一次缺键请求。

票：**FAIL**。否决本批定稿。

### 006 独立裁定（跨模块观察／否）

`PlatformController.createSpaceType` `:156-160` 无 `requireIdempotencyKey`，其余写入口有。本席 OBS-09。M009-022 已闭环：行为上 `IdempotencyService.execute` 仍必填，仅控制器风格。登记于 findings 本节、M009 不重开、本模块不修源码、不为其写测试。队列否。工程缺陷按 M009-022 为否；候选「是（属 M009 源码）」不得解读为重开 M009。

票：**PASS（否）**。

### 007 独立裁定（其余观察／否）

本席 OBS-02 环境红已消失、OBS-03 自审闸首轮不走、OBS-04 绑定并发顺序已覆盖、OBS-05 JDBC 硬编码属约定、OBS-06 单违例 INSERT 充分、OBS-07 `be_operation_*` 主键、OBS-08 无联立用例（001 已覆盖缝）、OBS-10 只拒 spv、OBS-11 57 绿不作 GEN-003、OBS-12 不复用 M009 号——候选均维持否，未把其中任一条改写成缺陷入队。席甲 OBS-01 与本席 OBS-05 同事实。候选未写错本席观察。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本批不得定稿。B2-r2 若按上列拆分 005，则队列是=001、002、003、004、005（005 仅改名）；否=006、007（007 含 CX-04 缺键）。一次 B3-r1 只改本模块 3 个测试文件，不改 M009 源码、三 HTML、锁、迁移、权限种子。可执行行为先写归属该 ID 的最小失败测试（GEN-003）：

- 001：先改断言见红，再改 fake `withDelete("approved")` 见绿；注释随改。
- 002：夹具/断言改 `lease`+`active`/`pending_admission`，含 `:164/:201` `revenue_share`。
- 003：unarchive / rejected / `deleteReason` 各一条服务断言 + 控制器一条 archive 或 delete-approve；不实现 archive_reason fake。
- 004：服务一条 `asMerchantUser`；控制器一条参数化五 GET。
- 005：方法名/注释 SixValues；不加缺键请求。

禁止：按候选 005 给绑定两条补缺键 400；改 Jdbc/`PlatformService`/`MerchantRecord`；为 003 给 record 加 `archiveReason`；新增库 CHECK；把 006 当 M009 重开；每个 HTTP verb 各写作用域或幂等负例。

改动文件自身版本递增（Controller V0.01、JDBC/Service V0.03）。

### 结论

全批差集无漏并。001–004、006、007 逐 ID 票 PASS。005 并入 CX-04 缺键并改为入队，改写本席 GR-005 原意且重复已有缺键闸，本席 **FAIL**。无添加项。未源修、未改 findings/ledger。本席 B2 **FAIL**。须 B2-r2 按本票拆分 005 后再票；未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M011 IN_PROGRESS，B3=0。

## M011 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 43085 字节 + V0.02 B2-r1 追加，全文 61599 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，FILES 3 / 判据 87）
- 正本：`/home/james/complex-management`。用户给定审计基线 `origin/main=242f487233d028173cb9198eecd079f5d877405c`。本席 `rev-parse`：当前检出分支 `audit/20260909/M001-M010-checkpoint` = `2e812adf3692205d5bf7395c4c0c644edc335693`，`origin/main` 与 merge-base 仍为 `242f4872…`。三测试文件指纹与 B1 覆盖表逐文件全等，不构成候选改写。ledger V2.30 IN_PROGRESS；B3=0；无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲 B2-r1 PASS，只核 r2 候选是否落实本席 B2-r1。
- 本席 B2-r2：**PASS**（001–007 逐 ID 均 PASS；r1 唯一否决点 005 已闭合）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未重跑 57 用例（三文件指纹与 B1 表全等，B2 只核差集与 r1 意见落实）。

此票只判断 findings.md 顶部 V1.35「当前 M011 B2-r1 合取FAIL…B2-r2 修订候选」相对本次两份完整 B1 raw（含各自 B2-r1 追加段）的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法，并核对该候选是否准确落实本席 B2-r1（V0.02）否决与收窄意见、有无新的改写或遗漏。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M011-b1-grok-raw.md` 前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 61599 | `b72bf605c43128ab443c1c1bd98a4163d039d6e87d4edfdff06ab682b8c9f748` |
| 其中 B1 前缀 | V0.01 | 43085 | `ce463f1f601d4bad08eb2d535b7aa2c5df1bdf905b5ee5d2142db9759997871c` |
| `docs/audit-2026-09/modules/M011-b1-self-raw.md`（席甲 B1+B2-r1） | V0.02 | 17188 | `84980b2d44d95d0e2e2dceff66fa18616324ecd2e0980d214616929d5389b208` |
| 其中席甲 B1 前缀 | V0.01 | 13404 | `4ea215f5605d66177c036e113d3032b2d2587cd4b5a66dd2c02a609c0eff4313` |
| `docs/audit-2026-09/findings.md` | V1.35 | 511341 | `c9ab33717d04ba79b9475485d9812c6c52b8691b8d2c3ad41846acc70dceae88` |

ledger 只读：V2.30，433098 bytes，SHA256 `235684e210f265b3bafb712207fe2782545a732dc32792d469a6d4e0e8465a37`；M011 IN_PROGRESS、B3=0。三测试文件指纹与 B1 覆盖表逐文件全等（Controller 351/15915/`cbaec207…feeb78`；JDBC 495/23380/`247409ab…9dd738`；Service 810/39637/`8775e50a…1c3448`）。映射行 `M011 → [BEQR-063 … PGM-066]` COUNT=87 unique=87 MISSING=[]。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

候选形态：V1.35 顶部为 r2 **条目覆盖段**（非整表重写）；其下「当前 M011 B2-r1 整批候选」001–007 原表保留。未改条目（001/002/004）沿用 r1 表七列；修订条目（003/005/006/007）以顶部四条为准覆盖对应单元格。本席按此读取，不把 r1 表残留「两处补断言 / fake 实现 setMerchantArchiveReason / 工程缺陷是（属 M009）」当作本批有效处置。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：005 是否仍并入缺键、003 是否仍逼 fake、006 是否仍写缺陷是、有无新改写 |
| verification-before-completion | 声称前先跑只读哈希/行号核对；本票不声称 57 用例本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：003 深度落在 withArchive/withDelete + listMerchants(false/true)，不落 archiveReason 旁路 |
| ponytail-review | 只评最小删除/收窄，不落地。005 缺键两条仍 = yagni |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理并另写 `docs/arc/audits/`，与双席冲突；**未用** |

### r1 意见 → r2 候选对照（本席 B2-r1 原文，不采信席甲转述）

本席 B2-r1：**FAIL**。唯一否决点 005 把席甲 CX-M011-04 两条缺幂等键请求并入并列为必修。另提出 003 处置收窄（不为 archive_reason 造 fake、不给 MerchantRecord 加字段）与 006 缺陷口径（按 M009-022 为否）。001/002/004 票 PASS，口径不变。

| 本席 r1 要求 | r2 候选（V1.35 顶部） | 落实？ |
|---|---|---|
| 005 技术性／缺陷是／队列是（随批）：**只**改 FiveValues→六值；TDD 用该用例自身非法值断言，不新写测试 | 「只把 `createMerchantRejectsBusinessFormatOutsideFiveValues` 方法名/注释改为六值……TDD以该用例自身已能失败的非法值断言为准，不新写测试」 | 是 |
| CX-04 两条缺键：**禁止**各补一次请求；归 007 否 | 005「不补缺键请求」；007「两条绑定用例缺幂等键未断言……按铁律6不按verb各补」 | 是 |
| 可选随 005 把两条绑定方法名去掉 Idempotency 字样 | 005 写成必做改名（GEN-016 名称与断言一致） | 是（r1 允许的随批改名收为必做，仍仅改名、无新测试） |
| 003：unarchive 后默认列表重新可见（存档时不可见）；rejected 后 deleteStatus=none；requestDelete 后 deleteReason 非空（withDelete）；控制器一条 archive 或 delete-approve；不为 archive_reason 造 fake、不加字段、不为四条 HTTP 各写一条 | 三条服务断言 + 控制器一条；明确「不为 archive_reason 新增 fake 能力、不给 MerchantRecord 加字段、不为四条 HTTP 各写一条」。rejected 另写「且商户仍在已存档列表」 | 是（加严见下，不构成新改写） |
| 006 工程缺陷按 M009-022 为否；队列否；M009 不重开 | 「否（按 M009-022 定稿：IdempotencyService.execute 仍必填，仅控制器未显式调用）」，跨模块观察登记、M009不重开 | 是 |
| 007 补入 CX-04 缺键事实 + 席甲 OBS-02/03/04/05 | 顶部 007 条逐项写入；队列仍否 | 是 |
| 001/002/004 口径不变 | 「其余001/002/004口径不变」；r1 表三行七列未改写 | 是 |

**r1 裁定未被误读。** 无把「005 仅改名」读成仍补缺键、无把「003 收窄」读成仍实现 archive_reason fake、无把「006 缺陷否」读成重开 M009、无把「CX-04 归 007 否」读成删除事实。无新永久 ID、无锁外判据、无 HTML 修改申请、无添加项、不进 BLOCKED-ASK。

### 完整差集（GR-001…005、OBS-01…12、席甲 CX-01…05、席甲 OBS-01…06）

漏并：无。两席 B1 全部临时 ID 仍落在 001–007。003 仍按本席 GR-003 收入，不是席甲发明。

| 正式ID | 本次来源（不改写原意） | r2 分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M011-001 | GR-M011-001 + 席甲 CX-01/05（+M009-010/026） | 冲突/是/是 | **PASS（冲突/是/是）** |
| M011-002 | GR-M011-002 + 席甲 CX-02（+M009-003/005） | 细化/是/是 | **PASS（细化/是/是）** |
| M011-003 | GR-M011-003（席甲 B1 未列） | 细化/是/是（处置已收窄） | **PASS（细化/是/是）** |
| M011-004 | GR-M011-004 + 席甲 CX-03 | 细化/是/是 | **PASS（细化/是/是）** |
| M011-005 | GR-M011-005；席甲 CX-04 **改名半边**（缺键半边改归 007） | 技术性/是/是（随批，仅改名） | **PASS（技术性/是/是；仅改名）** |
| M011-006 | GR OBS-09（跨模块） | 技术性（跨模块观察）/否（M009-022）/否 | **PASS（否/否）** |
| M011-007 | GR OBS-02/03/04/05/06/07/08/10/11/12 + 席甲 OBS-01…06 + CX-04 缺键事实 | 技术性/正向 /否/否 | **PASS（否/否）** |

冲突 1（001）。细化入队 3（002、003、004）。技术性入队 1（005，仅改名）。否 2（006、007）。添加 0。

### 001 独立裁定（冲突／是；口径与 r1 同，未改）

r1 表单元格未改。核源仍成立：`PlatformServiceTests.java:721-723` `merchants.remove`；`:362-376` 对 `listMerchants(true)` 断言 `noneMatch`；`:706`「拒硬删」；对照 JDBC `:342-355` 与 `JdbcPlatformRepository` `UPDATE delete_status='approved'`；HTML BSM-004 `:571`「不是真从数据库里抹掉」。四分类冲突。最小处置仍：先改断言见红，再改 fake `withDelete("approved")` 留行，注释改软删；不改 M009 源码。票：**PASS**。

### 002 独立裁定（细化／是；口径与 r1 同，未改）

r1 表仍含 JDBC `:69-72/:142-143/:164/:201/:393/:415`（含更新路径 `revenue_share`）与 Controller `:133/:157/:169`。服务负例 `:165/:180` 必须保留。项目状态 `draft` 与主键 `be_operation_*` 不动。最小处置仍：夹具改 `lease`（或六类之一）+ `active`/`pending_admission`，`:164/:201` 一并改，不新增库 CHECK。票：**PASS**。

### 003 修订核实（细化／是；r1 收窄已落地）

本席 r1：「不要为 archive_reason 新增 fake、不要给 MerchantRecord 加字段」；服务三条 + 控制器一条；不为四条 HTTP 各写一条。

本批类型 4 核源（只读，不采信转述）：

- `MerchantRecord.java` 字段止于 `deleteReason`，**无** `archiveReason`。`archive_reason` 只经 Jdbc 写库列。
- fake `:685` `setMerchantArchiveReason` 仍空实现。r2 **禁止**为实现它而造旁路。
- `unarchiveMerchant:220-241`：`withArchive(null,null,now).withDelete("none",…)`，默认接口 `listMerchants(false)` 按 `archivedAt==null && deleteStatus≠approved` 过滤；存档后默认列表不可见、取消存档后重新可见，现有 `saveMerchant`/`withArchive` 足够。
- `requestDeleteMerchant:259-261` 未存档则 `MERCHANT_NOT_ARCHIVED`；非最高权限走 `withDelete("pending", … reason …)`。`approveDelete` rejected 分支 `:323-326` 只 `withDelete("none")`，**不**清 `archivedAt`。故「rejected 后仍在已存档列表」是同一条用例的加严，不是第二条测试，也不发明恢复页。
- 既有 `asMasterAuthority()`（`:120-125`）已把申请发起人与审批人分开，驳回断言可走与 `approveDeleteRemovesArchivedMerchantWhenNoReferences` 同构的换人，不改被测源码。
- 控制器 archive/delete-approve `:88-121` 均有 `requireIdempotencyKey` + 权限码；本模块仍无对应 HTTP 用例。r2 仍只要求 **一条** archive 或 delete-approve，不是四条。

「仍在已存档列表」相对 r1 原文多一句，但是 `MERCHANT_NOT_ARCHIVED` 闸 + rejected 不清档的直接推论，锁的是「驳回 ≠ 抹除」，与 001 同缝、不增 fake。不构成新改写，不因此 FAIL。

「`findMerchantById`/列表记录的 deleteReason」：默认列表不含已存档行，列表须为 `listMerchants(true)` 或 `findMerchantById`。定稿措辞见下，不改队列。

票：**PASS**。

### 004 独立裁定（细化／是；口径与 r1 同，未改）

r1 表未改：服务一条 `asMerchantUser` 调 `createMerchant`/`listMerchants` 期望 `PLATFORM_SCOPE_REQUIRED`；控制器一条参数化负例扩到其余五个治理 GET；不为每个 verb 各写一条。核源：`@BeforeEach` 仍一律 `asPlatformAdmin()`；`asMerchantUser` 存在；七个 GET 生产侧均有 `requirePlatformScope`，本模块负例仍只打 `/merchants`。票：**PASS**。

### 005 修订核实（r1 唯一否决点；已闭合）

本席 r1 FAIL 理由：(1) 并入 CX-04 缺键改写 GR-005 原意；(2) 缺键闸已有 `creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed`（`:100-112`，POST `/merchants` 无键 → 400 `VALIDATION_FAILED`），再按 verb 补两条违反「不为每个 verb 各写一条」；(3) 003 已覆盖真正未测的 archive/delete-approve HTTP 面。

r2：

- 分类技术性、缺陷是、队列是（随批）——仅就改名而言，与 r1「001–004 已进 B3 则可顺手改名」相容。
- 处置删除「两条用例各补一次缺键请求断言」。
- 绑定两条方法名去掉 Idempotency：r1 写可选，r2 收为必做；仍是 GEN-016 名称与断言对齐，**零新请求**。
- TDD：该用例以 `tenant` 断言 `MERCHANT_BUSINESS_FORMAT_INVALID`，自身已能失败；不新写测试。

核源：`:321-329` 方法名/注释仍「五值」；`:258-292`/`:295-325` 请求体**带**键且 200，只 verify `entity.binding.manage`。r2 禁止补缺键后，这两条成功路径保持，只改名。

票：**PASS**。r1 否决点闭合。

### 006 修订核实（跨模块观察／否；缺陷口径已收）

`PlatformController.createSpaceType` `:157-160` 仍无 `requireIdempotencyKey`，其余写入口有。M009-022 定稿：技术性、缺陷否、队列否（`IdempotencyService.execute` 仍必填，仅控制器风格）。r2 工程缺陷改为「否（按 M009-022…）」，M009 不重开，本模块不修源码、不为其写测试。队列否。票：**PASS（否）**。

### 007 修订核实（其余观察／否；r1 事实补全已落地）

r1 不构成 FAIL 的措辞缺口：CX-04 缺键应归本 ID 否；席甲 OBS-02（401/403）、OBS-03（空密钥）、OBS-04（`@Transactional`/文件 V0.03）、OBS-05（`seedMerchant` 以 `be_1` 作三列）应写入事实栏。r2 顶部均写入，并写明共享闸已由 `creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed` 覆盖、按铁律 6 不按 verb 各补。本席 OBS-02…12 无一条被改写成缺陷入队。席甲 OBS-01/06 原已在 r1 表 007。改名归因 005，不把 007 队列改是。票：**PASS（否）**。

### 残留措辞（不构成新改写，不因此 FAIL）

r1 表 003 处置仍写「fake 实现 `setMerchantArchiveReason` 并断言理由落到记录」；005 仍写「一行改名+两处补断言」「各补一次缺键请求」；006 仍写「工程缺陷是（属 M009 源码）」。这是未改条目沿用原表的历史单元格，**不是**本批有效口径。定稿后须以 V1.35 顶部 r2 四条覆盖这三行；B3 若按 r1 表 005 补缺键请求，即违反本席本票。

003「列表记录」未写死 `includeArchived=true`。定稿写明：deleteReason 断言走 `findMerchantById` 或 `listMerchants(true)`，不得拿默认列表（已存档行本就不在其中）。

005 把 r1「可选」去 Idempotency 字样收为必做，可接受。

三个测试文件自身版本递增（Controller 现 V0.01、JDBC/Service 现 V0.03）r2 顶部未重述，沿用 r1 边界，定稿补一句即可。

### 分类 / 队列 / 允许的最小修复边界

本批可定稿（仍须席甲本批 B2-r2 PASS 合取）。队列是=001、002、003、004、005（005 仅改名）；否=006、007（007 含 CX-04 缺键事实）。一次 B3-r1 只改本模块 3 个测试文件，不改 M009 源码、三 HTML、锁、迁移、权限种子。可执行行为先写归属该 ID 的最小失败测试（GEN-003）：

- 001：先改断言见红，再改 fake `withDelete("approved")` 见绿；`:706` 注释随改。
- 002：夹具/断言改 `lease`+`active`/`pending_admission`，含 `:164/:201` `revenue_share`；服务非法值负例保留。
- 003：`unarchiveMerchant` 后默认列表重新可见（存档时不可见）；`approveDelete(decision=rejected)` 后 `deleteStatus=none` 且仍在 `listMerchants(true)`；`requestDeleteMerchant` 后 `findMerchantById`/`listMerchants(true)` 的 `deleteReason` 非空（走既有 `withDelete`）；控制器一条 archive **或** delete-approve 的权限/幂等。不为 archive_reason 新增 fake、不给 MerchantRecord 加字段、不为四条 HTTP 各写一条。驳回用例换人沿用既有 `asMasterAuthority()`。
- 004：服务一条 `asMerchantUser`；控制器一条参数化五 GET。不为每个 verb 各写一条。
- 005：方法名/注释 SixValues；两条绑定用例方法名去掉 Idempotency 字样；不加缺键请求；不新写测试。

禁止：按 r1 表 005 给绑定两条补缺键 400；改 Jdbc/`PlatformService`/`MerchantRecord`；为 003 给 record 加 `archiveReason` 或实现 `setMerchantArchiveReason` fake；新增库 CHECK；把 006 当 M009 重开；每个 HTTP verb 各写作用域或幂等负例。

改动文件自身版本递增。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 hashlib / sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶行、本文件既有文首独占行同值）
- 输入三文件字节与 SHA：上表全等
- 三测试文件指纹：与 B1 表全等
- 点读：MerchantRecord 无 archiveReason；fake `:685` 空实现；unarchive/rejected/requestDelete 行号如上；`creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed` `:100-112`；两条绑定用例带键 200；FiveValues `:323`；createSpaceType `:157-160` 无 `requireIdempotencyKey`；`asMasterAuthority()` `:120-125` 已存在
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

候选按本席 B2-r1 意见修订：005 拆出 CX-04 缺键、仅改名入队；003 收窄为三条服务断言 + 一条控制器、禁止 archive_reason fake；006 缺陷按 M009-022 为否；007 补入缺键事实与席甲 OBS-02…05。001/002/004 口径未改。全量差集无漏并、无新 ID、无新判据、无本批添加项。r1 任一裁定均未被误读。r1 表残留单元格不改变已裁定的队列/分类。

本席队列：**001、002、003、004、005 是（5 项，005 仅改名）；006、007 否（2 项）**。

本席 B2-r2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M011 `IN_PROGRESS`，B3=0。不覆盖上方 B1 FAIL 与 B2-r1 FAIL 原文。
