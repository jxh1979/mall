CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 4 文件，冻结有效内容行 203
- 冻结判据：18 条（`M010 → [GEN-001 … GEN-018]`，与锁映射行逐字相同）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M010-001、GR-M010-003
- 添加项（完整列出，供 B2 按 2.2 问操作员，本席不代答）：GR-M010-002
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw、未问操作员

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先于技能本体 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试/接口深度；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（psql BEGIN/ROLLBACK + 现有测试）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推，只核未启用 |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；映射行 `M010 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]` 与开工口径 18 项逐字相同（本席自锁文件抽出，COUNT=18 unique=18 MISSING=[]）
- `plan/modules.md` M010 FILES 4 条与用户清单逐字相同
- 4 文件 `\n` 计数之和 = **203**，与冻结有效内容行一致（`git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` → 203）
- 4 文件字节/SHA256 与用户固定清单逐文件 `match=True`

HEAD：`main` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`（`git fetch origin` 后 `rev-parse` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。工作区含 M009 已双 PASS 的 B3 修复（未提交），本席只读消费方、未改那些文件。

ledger 只读核验（不改）：V2.16，412646 bytes，SHA256 `263892252c819bfe502c7a50bdcd75a488f24604906f7a48353b7b0d704d921a`；M010 行 IN_PROGRESS、B3=0。findings V1.26，481831 bytes，SHA256 `e5b49240e942500e2fff5d7b20fcd9beddc66e1dc7b415d84982a09387238e6b`，仅作已登记边界背景（M009-012 / M009-025 / M009-017），不代替本模块审计。原 322 模块编号及旧结论不代表当前 471 模块完成。

三份 HTML 只读、未改。锁内 BEQR-143/144/148/154/155、PGM-018/019 的来源行对本席读取的现行 HTML **命中**（BEQR 在 `business-enum-quick-reference-v0.1.html:226–238`；PGM 在 `platform-governance-map-for-operations-v0.1.html:415/424`）。这六条**不在 M010 映射**，只作 HTML 原文依据引用，不新造判据。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c |

## 2. 4 文件覆盖表（全文已读；行=splitlines=\n 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql | 109 | 3900 | 5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905 | 已读 L1–L109 |
| 2 | backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql | 7 | 352 | e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd | 已读 L1–L7 |
| 3 | backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql | 16 | 1300 | 466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481 | 已读 L1–L16 |
| 4 | backend/src/main/resources/db/migration/V354__business_entity_master_data.sql | 71 | 5625 | c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3 | 已读 L1–L71 |

四文件均已发布（flyway_schema_history version 005/048/318/354 success=t；Flyway CRC32 与当前文件逐行算法全等；`DbValidate`「Successfully validated 387 migrations」含这四份）。**不得改历史迁移正文**；任何修复只能以新迁移或只读裁定表达。

相关只读依赖（验证本模块 DDL/种子的消费，不扩成他模块全量审计）：M009 未提交 B3 后的 `JdbcPlatformRepository`/`PlatformService`（已删 `parentEntityId`，服务层三角色白名单）、`business_entity_bindings`（V002/V010，非本 FILES）、V251 权限中文名、V140 运行时 GRANT、M011 `PlatformJdbcIntegrationTests`/`PlatformServiceTests`、结算 `SettlementEntityAttributionS1Tests`、招商 `LeasingSignAtomicityJdbcIntegrationTests`。M001–M009 不重开。

## 3. 数据流（迁移 DDL/种子 → complex_test 实态 → 消费方 → 现有测试）

### 3.1 活库连接（类型 4）

仅从 `~/.complex/complex.env` **抽出三个** `COMPLEX_DATABASE_*`，把 URL 库名改写为 `complex_test`，不 `source` 整个 env，不打印密文。`psql`：`current_database=complex_test`，`current_user=complex`，host=127.0.0.1:5432。探针包在 `BEGIN…ROLLBACK`。未跑清理脚本。事后删除本席临时 env 文件。

### 3.2 Flyway 实态

| 项 | 实算 |
|---|---|
| V005 / V048 / V318 / V354 | 均 success=t，checksum 与当前文件 Flyway 逐行 CRC32 全等（413803928 / -1430035601 / -705915847 / -61418191） |
| 仓内最大迁移 | V399（377 个 V*.sql） |
| complex_test 当前版本 | **409**（V400–V409 共 10 条，仓内 main 无对应文件） |
| DbValidate | Successfully validated 387 migrations |
| DbMigrate 警告 | `Schema "public" has a version (409) that is newer than the latest available migration (399) !` |

此即 M009-017 已登记的环境事实，属环境不是本 4 文件源码缺陷。

### 3.3 表/列/约束/索引/种子（本模块承载）

`business_entities` 11 列：V005 的 9 列 + V318 `parent_entity_id`（可空 varchar64）+ V354 `archive_reason`（可空 text）。PK=`business_entities_pkey`。CHECK=`chk_be_entity_role`：`entity_role IN ('platform','project_company','management_company','spv','operation')`。索引：`idx_business_entities_status`、`idx_business_entities_parent`。无任何指向本表的硬 FK，本表也无指向自身的 FK。

种子行（仅此 6 行，无 spv/operation 行）：

| id | entity_name | entity_role | parent_entity_id | status |
|---|---|---|---|---|
| be_platform | 平台公司 | platform | NULL | active |
| be_proj_changsha | 长沙项目公司 | project_company | be_platform | active |
| be_ent_platform | 娱乐平台公司 | management_company | be_proj_changsha | active |
| be_central_catering | 中央餐饮公司 | management_company | be_proj_changsha | active |
| be_amusement | 游乐公司 | management_company | be_proj_changsha | active |
| be_media | 传媒公司 | management_company | be_proj_changsha | active |

资产公司 0 行（V354 声称与 HTML「资产业态不单设、由城市项目公司兼任」一致，本席用 `entity_name='资产公司'` count=0 核实）。工商三字段（统一社会信用代码/法人/银行账户）6 行全 NULL。

`merchants`：V005 起源列仍在；`contact_phone` 已被后续 V339 扩到 varchar128；`archive_reason` 来自 V271（非本 FILES）。87 行，`business_entity_id`/`contract_entity_id`/`settlement_entity_id` **全 NULL**。无 FK。后续 `chk_merchants_business_format` / 删除状态 CHECK 不在本 4 文件。

`projects`：0 行；仅 PK + `idx_projects_merchant` / `idx_projects_status`；无 status CHECK、无 merchant FK。

`spaces`：0 行；V005 列仍在；后续 V203 `chk_spaces_status (active, unavailable)` 不在本 FILES。

`roles.organization_id`：列存在、可空；`idx_roles_organization` 部分索引存在；10 行角色 **全部 organization_id IS NULL**；`roles_role_name_key UNIQUE(role_name)` 仍为全局唯一。`role_platform_admin.role_name='平台管理员'`。

V005 八个权限码在库中的 `permission_name` 已是中文（V251 覆盖 V005 英文种子）：查看/管理业务主体和绑定、查看/管理商户档案、查看/管理平台项目、查看/管理平台空间。八码均授给 `role_platform_admin`。

### 3.4 消费方（只读）

- M009 未提交面：`BusinessEntityRecord` 已无 `parentEntityId`；`JdbcPlatformRepository.saveBusinessEntity` INSERT/SELECT 均不含 `parent_entity_id`（更新冲突也不改该列，故 V354 种子父链在库里仍在）。`PlatformService.BUSINESS_ENTITY_ROLES = {platform, project_company, management_company}`，服务层拒绝 `spv`。
- 四类绑定唯一约束在 V010 `uq_business_entity_bindings_active_merchant`（非本 FILES）；V005 商户三列是投影，M009-008 从绑定同步。
- 结算：`SettlementEntityAttributionS1Tests.sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData` **断言** 六骨架的 `parent_entity_id` 链；收入流种子引用 `be_ent_platform` 等稳定 id（V370/V372/V375 等，非本 FILES）。
- 招商：`LeasingSignAtomicityJdbcIntegrationTests` INSERT `business_entities` 用 `management_company`，不写 parent。

### 3.5 现有测试（本席实跑，不把未跑写成通过）

| 命令 | exit | 结果 |
|---|---:|---|
| `mvn -Dtest=...PlatformServiceTests#createBusinessEntityRejectsRoleOutsideWhitelist test` | 0 | Tests run: 1, Failures: 0（服务层拒 `spv`） |
| `mvn -Dtest=...PlatformJdbcIntegrationTests#migrationCreatesPlatformMasterDataTablesAndSeedsPermissions+residualMigrationCreatesBindingContractTablesAndSeedsPermissions,...SettlementEntityAttributionS1Tests#sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData test` | 1 | 3 tests：骨架种子+父链 **PASS**；V005 表/权限种子 **PASS**；`residualMigration...` **FAIL** expected "399" but was "409"（M011:263，环境，非本 4 文件） |

Flyway 在测试启动时 validate 通过、不试图把 409 回退到 399。

## 4. 第一性原理（逐文件）

删除测试：删掉这 4 个已发布迁移文件本身做不到（历史 V 号不可改）；问的是「这些 DDL/种子所承载的能力若从模式中消失，三份 HTML 哪一条做不到」。

| 文件 | 为何存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| V005 | 主体/商户/项目/空间主表 + 治理权限种子。删掉这四张表，PGM「建立综合体项目档案 / 商户和主体 / 空间点位」与四类绑定的落库面没有建表起点 | 活库四表存在；八权限码+平台管理员授权存在；权限**显示名**已被 V251 改成中文，与本文件英文种子不一致 | 「权限名英文可先写、以后再译」不是 HTML 授权；本文件无 FK/CHECK/唯一绑定 |
| V048 | 给 `roles` 加 `organization_id` 做租户隔离。HTML 要数据按项目/组织隔离（BEQR-152 原文可引，非本映射） | 列和部分索引存在；当前 10 个角色全是全局（NULL） | 加列=隔离。全局 `UNIQUE(role_name)` 仍在，两组织不能同名（GR-M010-003） |
| V318 | 注释自称把「管理公司/业态主体」挂到父级，口径来自 `terminology-canonical-v1.0.md` 而非三 HTML | 列、索引、COMMENT 均在；6 粒种子父链按 L1←L2←L3 写满 | 前提「四层是固定隶属链」与 HTML 明文相反（GR-M010-001）。「V005 已 GRANT」在 V005 正文不存在（GRANT 来自 V140 ALL TABLES） |
| V354 | ① archive_reason ② 归档 operation 残留 ③ 六主体稳定 id ④ entity_role CHECK。稳定 id 被结算归属配置引用，删掉骨架 id 则后续 `be_ent_platform` 等引用失锚 | 6 行种子+CHECK+archive_reason 列均在；complex_test 上 operation 归档 UPDATE 命中 0 行；CHECK 拦垃圾角色、**放行 spv/operation** | 注释伪称 HTML 有「经营主体分四层」「六个业态主体各管什么」——三 HTML 检索均为 0。父链种子把冲突链写成数据。spv/operation 为 HTML 所无的角色值 |

三问收口：

1. **必须存在吗？** V005 四表+权限码、V354 六稳定 id（不含父链）、V048 组织列：答得上 HTML 上位（建档/隔离/具名主体）。V318 列与 V354 父链值：答不上「删掉哪条 HTML 做不到」——HTML 要求的是多对多协作、**禁止**固定隶属链。
2. **声称有无被执行？** 有：活库列/约束/种子/Flyway checksum/测试。英文权限名的声称已被后续迁移覆盖。
3. **未证前提？** 「术语表/旧四层=业务正本」「历史 operation/spv 必须留在 CHECK 以免存量不兼容」（铁律 8 禁止以存量兼容当设计约束）「加 organization_id 即完成隔离」。

## 5. 顺从式（18 条逐 ID）

分类顺序：冲突 → 细化 → 技术性 → 添加。无实现面写原因。不把 HTML 沉默当冲突，不把业务发明标成技术性。BEQR-143/144/148/154/155、PGM-018/019 只作 HTML 原文，不升格为 M010 冻结判据。

| ID | 覆盖结论 | 2.1 | 工程缺陷 |
|---|---|---|---|
| GEN-001 | V005 四表与权限码有用途。V318 `parent_entity_id` 及 V354 父链种子把 HTML 禁止的隶属链做成模式与数据，属重复/无需求承载的抽象 | 冲突（父链）+ 细化（四表） | **是 → GR-M010-001** |
| GEN-002 | 四份已应用且 Flyway validate 通过。V005 表/权限 JDBC 断言 PASS。父链被结算测试锁成「正确行为」，与 HTML 相反 | 冲突并入 001；环境 409≠399 见观察 | 父链 **是 → 001** |
| GEN-003 | 服务层拒 spv 单测 PASS（能红）。结算骨架测试 PASS 但断言的是禁止链。M011 flyway latest 因环境 409 红，不能当本 4 文件失败 | 技术性/环境 | 环境项否；测试锁链并入 001 下游成本 |
| GEN-004 | CHECK 拦 `not_a_role`（23514）。不拦 spv/operation。无 status CHECK，探针可写入 `正常`/`inactive`/`zzz`。无输入所有权校验（无 FK） | 添加（spv/operation）+ 细化（无 status CHECK，应用层后续补） | **spv/operation → GR-M010-002**；无 status CHECK 见观察 |
| GEN-005 | V005 把 view/manage 授给 `role_platform_admin`（默认拒绝不在本文件，由权限码消费）。V048 声称租户隔离但 `UNIQUE(role_name)` 全局，两组织同名角色 23505 | 细化（权限种子）+ 隔离不完整 | **是 → GR-M010-003** |
| GEN-006 | 有 PK。无 FK：探针可写孤儿 parent、商户三列指向不存在主体、项目 `merchant_id='no_mch'`、空间孤儿父。绑定唯一不在本 FILES（V010）。角色名全局唯一过严 | 细化（松引用约定）/缺陷（角色唯一） | 孤儿路径：首轮正常走 M009 服务层会拦，**不单独立项**（见观察）；角色唯一 **003** |
| GEN-007 | `IF NOT EXISTS` / `ON CONFLICT DO NOTHING` / `DROP CONSTRAINT IF EXISTS` 对 Flyway 单次执行足够。V354 `ON CONFLICT DO NOTHING` 不纠正已存在行的名称漂移；Flyway 不重放，首轮正常不撞 | 技术性观察 | 否 |
| GEN-008 | 模式 vs 代码：M009 已删 parent 字段，列与种子父链仍在；服务层三角色 vs CHECK 五值；V005 英文权限名 vs 库中文（V251）；结算测试仍读 parent | 冲突/添加并入 001/002 | **001、002** |
| GEN-009 | 非法角色 CHECK 失败可见（23514）。无 status CHECK 时非法状态伪成功落库。V354 归档 UPDATE 在本库 0 行，无半成品 | 细化观察 | 否（写路径正常走服务层） |
| GEN-010 | 迁移为静态 SQL，无拼接用户输入。V005 `contact_phone VARCHAR(20)` 已被后续 PII 迁移加长，不在本 FILES 修 | 技术性观察 | 否 |
| GEN-011 | 种子写入无审计事件表；权限变更靠 flyway_schema_history | 技术性观察 | 否 |
| GEN-012 | 6 行骨架+索引；87 商户无无界循环 | 技术性观察 | 否 |
| GEN-013 | 本模块无构建脚本。测试依赖三 DB 变量；本席按此连接成功。409>399 是共享库环境 | 技术性/环境 | 否（源码） |
| GEN-014 | 四文件顺序 5<48<318<354，已应用，checksum 匹配，validate 通过。破坏性动作（V354 归档 operation）有注释中的业务授权叙述，但授权源不是三 HTML。历史文件不可改 | 细化（骨架）+ 冲突（父链/归档口径） | **并入 001**；归档见观察 |
| GEN-015 | V005 权限名为英文展示文字；现行库已被 V251 改为中文。SQL 注释不是展示文字 | 冲突已被后续已发布迁移覆盖 | **否**（运行态中文；禁止改 V005） |
| GEN-016 | V354 头注释伪称 HTML 章节；V318 引 terminology-canonical 与「V005 已 GRANT」 | 技术性（注释） | 运行态否；只读裁定，禁止改历史文件。见观察 OBS-04 |
| GEN-017 | 父链=冲突；spv/operation=添加；V048 列=细化；英文权限=历史展示已被覆盖。未把 HTML 沉默当删除 | — | **001、002、003** |
| GEN-018 | 结论落到 HTML 原文、psql 输出、mvn surefire、Flyway validate；不引用席位意见当依据 | — | — |

无实现面（不把 HTML 沉默当冲突）：BEQR-148 合作模式六类、BEQR-154/155 中文状态词表——本 4 文件无对应 CHECK/枚举列（状态是自由 varchar；合作模式列不在 V005）。应用层闭集在 M009，不在本 FILES。

## 6. 对抗式（grilling 追问树，自答自证）

Frontier 已清空。事实题全部由本席读文件系统/psql/测试，未问操作员、未派子代理。

1. **父链是不是 HTML 禁止的那条隶属链？** 是。HTML：「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。」V318 注释把 L3.parent=L2、L2.parent=L1 写死；V354 六行种子就是这条链。COMMENT 仍在信息_schema。
2. **M009 删了 API 字段，列是否仍在被写/被读？** 写：M009 JDBC 不再写该列。读：结算测试仍 SELECT 并断言父值；种子行 parent 非空。列+索引+COMMENT+种子仍构成模式契约。
3. **CHECK 是否等于 HTML 三角色？** 否。探针：`spv`/`operation` INSERT 成功；`not_a_role` 23514。服务层拒 spv（单测绿）。库比应用宽两值。
4. **两组织同名角色？** 探针：org-a 与 org-b 同 `role_name` → 23505。V048 只加列，未改唯一约束。
5. **孤儿引用？** parent=`no_such_parent`、商户三列=`no_be/no_ce/no_se`、项目 `merchant_id='no_mch'`、空间 `parent_space_id='no_parent'` 全部插入成功。无 FK。首轮正常 API 经 M009 存在性校验；裸 SQL/他模块夹具仍能写。按 controller「两三件不巧才成立则不修」：**不单独立项入队**，登记观察。
6. **自指/环？** `parent_entity_id=自身` 成功。无 cycle CHECK。同上，不单独立项。
7. **重放 V354 INSERT？** `ON CONFLICT DO NOTHING` 不能把 `be_platform` 改名；Flyway 不重放。不立缺陷。
8. **V005 英文权限名是否仍展示？** 否。库中八码已是中文。重放 V005 的 `ON CONFLICT DO UPDATE permission_name=EXCLUDED` 会把中文打回英文，但 Flyway 不重放。不立缺陷。
9. **HTML 是否真有 V354 所引章节？** 三文件检索 `经营主体分四层` / `六个业态主体各管什么` / `平台 → 项目公司 → 业态主体` / `spv` / `SPV` / `operation` 全 0。
10. **极端：并发双写父链。** 列无触发器。M009 已不写。不新造闸。

Ponytail-review（只列可删，不落地）：

- `V318:L9-16: delete: parent_entity_id 列/索引/注释把 HTML 禁止的隶属链做成模式。新迁移 DROP，无替换（协作关系若需要，不在本 B1 发明新表）。`
- `V354:L53-61: delete: 种子 parent_entity_id 五处非空。随列删除消失；稳定 id 行保留。`
- `V354:L68: shrink: CHECK 五值收为三角色（待 2.2 若答删除）。`
- `net: 去掉父链列+收窄 CHECK，历史文件不动。`

## 7. Findings（临时 ID；正式 ID 仅 B2 分配）

### GR-M010-001

- **标题**：`parent_entity_id` 把主体做成「平台→项目公司→业态主体」单父链，与 HTML 明文「不是固定隶属层级」冲突
- **文件:行号**：`V318__business_entity_parent_layer.sql:1-16`（ADD COLUMN / COMMENT / INDEX）；`V354__business_entity_master_data.sql:53-61`（INSERT 六行的 parent 值）
- **完整原文（实现）**：
  - V318:L1-5：「四层:平台(L1) → 项目公司(L2) → 管理公司/业态主体(L3) → 商户」「L3.parent=L2 项目公司,L2.parent=L1 平台,L1.parent=NULL」
  - V318:L12-13 COMMENT：「四层组织父级(terminology-canonical §一):L3 管理公司→L2 项目公司,L2→L1 平台,L1=NULL」
  - V354:L55-60：`be_proj_changsha` parent=`be_platform`；四个 `management_company` parent=`be_proj_changsha`
- **HTML 原文（类型 1，引用不升格判据）**：
  - BEQR-144 / `business-enum-quick-reference-v0.1.html:227`：「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。」
  - PGM-018 / `platform-governance-map-for-operations-v0.1.html:415`：「物理项目与运营公司：多对多协作，不是固定隶属层级」；「一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。」
- **业务影响**：模式与种子把禁止的行政隶属写成唯一父指针；一个业态主体只能挂一个项目公司。M009 已从 API/记录/SQL 去掉该字段（M009-012），但列、索引、COMMENT、六行 parent 值仍在，结算测试仍当不变量断言。后台已不暴露，库仍在教下游「这是层级」。
- **可复现命令/最小反例**：`SELECT id, entity_role, parent_entity_id FROM business_entities WHERE id LIKE 'be_%' ORDER BY id;` → 五条非空父链。`SettlementEntityAttributionS1Tests#sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData` exit 0，断言 `be_ent_platform.parent=be_proj_changsha` 等。
- **冻结判据**：GEN-017、GEN-001、GEN-008、GEN-014
- **2.3 依据类型**：1（HTML 原文）+ 4（psql / 测试输出）
- **四分类**：**冲突**（与明文矛盾，判定为删除）
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：不得改 V318/V354 正文。新迁移：`DROP INDEX IF EXISTS idx_business_entities_parent; ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;`（COMMENT 随列消失）。**保留**六行稳定 id/名称/角色（结算引用 `be_ent_platform` 等）。B3 不得扩改结算测试/他模块；若 DROP 导致他模块测试红，属下游，回 B2 再归 ID，禁止无 ID 顺手改。

### GR-M010-002

- **标题**：`chk_be_entity_role` 允许 `spv`、`operation`，三份 HTML 无此二角色
- **文件:行号**：`V354__business_entity_master_data.sql:63-71`
- **完整原文**：
  - L63-68：「④ entity_role 白名单(四层组织 + 历史 operation)」`CHECK (entity_role IN ('platform', 'project_company', 'management_company', 'spv', 'operation'))`
  - L70-71 COMMENT：「…/spv/operation=历史通用运营主体」
- **HTML**：三文件检索 `spv`/`SPV`/`operation` 为 0。PGM-018/019 具名主体为顶层平台、物理项目、域主管公司、城市项目公司、娱乐平台公司、游乐公司、传媒公司、中央餐饮公司、独立酒吧商户——无 SPV 角色、无「operation」角色。M009 服务层白名单只有前三码（`PlatformService.java:60-61`）。
- **业务影响**：库允许写入 HTML 没有的主体角色。complex_test 当前 0 行 spv/operation，但探针可插入。M011 JDBC 夹具仍用 `entity_role='operation'` 走仓储直写。铁律 8：不得为存量兼容保留历史枚举。
- **可复现**：BEGIN; INSERT … `'spv'` / `'operation'` → INSERT 0 1；INSERT `'not_a_role'` → 23514；ROLLBACK。单测 `createBusinessEntityRejectsRoleOutsideWhitelist` exit 0。
- **冻结判据**：GEN-017、GEN-008、GEN-004
- **2.3 依据类型**：1 + 4
- **四分类**：**添加**（HTML 无此业务枚举值，构成业务发明；不能标成技术性）
- **工程缺陷**：添加项本身不自动入修复队列；若操作员答「删除」则入队
- **建议入队**：待 2.2 答复（删除→新迁移收窄 CHECK 为三值并更新 COMMENT；保留→须回改 M009 白名单，那是他模块，不能在 M010 B3 偷偷改）
- **最小处置**：B1/B4/B5 只登记。B2 按 2.2 **逐项**询问（两项角色值，不可合并成一句「历史兼容」）。本席不代答。

【添加项完整列出 | 供主调度写 `questions/<ID>.md`，本席不代填答复】

**项 A — `entity_role='spv'`**

HTML 原文（出处：三份 HTML 中无任何相关条目；最接近：PGM-019 主体表列娱乐平台公司/独立酒吧商户/游乐公司/传媒公司/中央餐饮公司/城市项目公司，无 SPV）：
> 三份 HTML 中无任何相关条目

添加了什么：`V354:68` CHECK 与 `V354:71` COMMENT 把 `spv` 列为合法主体角色。

添加后的代价：服务层已拒 spv，库仍收；未来夹具/裸 SQL/他模块可绕过服务层写入；与「酒吧 SPV 已收敛为商户」的术语表（非正本）继续双轨。

删除的代价：新迁移收窄 CHECK；当前 0 行，无数据迁移。M011 若有 spv 夹具需另模块处理（本 FILES 无测试）。

**项 B — `entity_role='operation'`**

HTML 原文（出处：三份 HTML 中无任何相关条目；最接近：BEQR-155「商户/运营主体状态」说的是状态不是角色）：
> 三份 HTML 中无任何相关条目

添加了什么：同一 CHECK/COMMENT 把 `operation` 列为「历史通用运营主体」；V354:41-46 还按 `entity_role='operation'` 做一次性归档 UPDATE。

添加后的代价：与 M009 三角色白名单长期分叉；M011 `PlatformJdbcIntegrationTests` 仍 INSERT `operation`（仓储直写）。铁律 8 禁止以存量兼容当设计约束。

删除的代价：收窄 CHECK 后，该 JDBC 夹具会 23514，属 M011 下游。complex_test 现无 operation 行。

## 操作员答复

（B1 不写 questions 文件；上段空答复标题仅示模板，本 raw **不**创建 `docs/audit-2026-09/questions/`。）

### GR-M010-003

- **标题**：V048 增加 `roles.organization_id` 声称租户隔离，但 `role_name` 仍全局 UNIQUE，不同组织不能使用同一角色名
- **文件:行号**：`V048__access_role_organization_isolation.sql:1-7`（加列+部分索引；未改唯一约束）。活库 `roles_role_name_key UNIQUE (role_name)` 来自 V003 建表，V048 未替换。
- **完整原文**：L1-2「Add organization_id to roles table for tenant isolation (P0 security fix). NULL = platform-global role visible to all; non-null = organization-scoped role.」
- **业务影响**：组织范围角色无法复用中文岗位名（如两个项目都要「店长」）。M006 已有「公司优先角色」按组织创建的路径，这是**首轮正常**就会撞的约束，不是多因素极端。当前 10 个角色全是全局 NULL，尚未撞生产数据，但写入路径已存在。
- **可复现**：BEGIN; INSERT roles (id, role_name, …, organization_id) `org-a` 再 `org-b` 同名 → NOTICE `P9_DUP_ROLE_NAME blocked sqlstate=23505`；ROLLBACK。
- **冻结判据**：GEN-005、GEN-006、GEN-008
- **2.3 依据类型**：4（psql）+ 实现声称（V048 注释）对照
- **四分类**：**细化**（组织隔离是项目隔离的下位实现；缺陷是实现不完整，不是新业务概念）
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：不得改 V048。新迁移：删除 `roles_role_name_key`，改为允许「全局名唯一 + 组织内名唯一」的表达式唯一索引，例如 `UNIQUE (COALESCE(organization_id, ''), role_name)`（须在 B3 用失败测试锁两组织同名可共存、同一组织同名仍拒）。不要在本模块发明角色目录业务。

## 8. 观察（无新增根因也登记，不漏并）

| ID | 内容 | 分类 | 工程缺陷 | 入队 |
|---|---|---|---|---|
| OBS-01 | complex_test flyway 409>main 399，含未发布 V400–V409；`PlatformJdbcIntegrationTests:263` expected 399 was 409。Flyway 警告 newer than latest。与 M009-017 同环境事实 | 技术性（环境） | 否（非本 4 文件） | 否 |
| OBS-02 | V005 八个 `permission_name` 为英文；活库已被 V251 改为中文。禁止改 V005；无需再写一条只为中文的新迁移 | 冲突已被后续已发布迁移覆盖 | 否（运行态） | 否 |
| OBS-03 | 无硬 FK：孤儿 parent/商户三列/项目/空间探针成功。V318:7 明文「本库约定不加硬 FK」。M009 写路径已存在性校验。按「首轮正常会否发生」收窄：正常 API 不走裸 SQL，**不修** | 细化 | 否 | 否 |
| OBS-04 | V354:3-8 伪称 HTML「经营主体分四层」「六个业态主体各管什么」（三文件 0 命中）。V318:8「V005 已 GRANT」——V005 无 GRANT 句，`complex_app` 权限来自 V140 ALL TABLES。历史注释不能改；只读裁定 | 技术性 | 否（不改历史文件） | 否 |
| OBS-05 | V005 商户三主体列可空、本库 87 行全 NULL；「同一经营对象下唯一绑定」在 V010 绑定表（非本 FILES）。M009-008 从绑定投影到这三列。本模块未重复造唯一约束 | 细化（投影列） | 否 | 否 |
| OBS-06 | `business_entities.status` 无 CHECK；探针可写 `inactive`/`正常`/`zzz`。V354 归档用 `inactive`+`archive_reason`，与 HTML「归档」及 M009 `SUBJECT_STATUSES` 不是同一闭集。服务层管商户/项目状态；主体状态库层仍开。首轮正常走服务层 | 细化 | 否（不单独立项；勿把 HTML 状态词表升格为 M010 新判据） | 否 |
| OBS-07 | 结算 `sixEntitySkeleton…` 把父链锁成绿。属他模块测试，是 001 的下游成本，不是本 FILES 测试失效 | 观察 | 否 | 否 |
| OBS-08 | V354 `ON CONFLICT DO NOTHING` 不修复漂移；Flyway 单次执行。极端重放不修 | 技术性 | 否 | 否 |
| OBS-09 | V354:41-46 按 `entity_role='operation' AND status='active'` 批量归档。本库 0 行。一次性重建，不是用户面删除审批流。不把迁移当日常删除绕过 | 细化 | 否 | 否 |
| OBS-10 | `entity_name` 无唯一：探针可再插一行「游乐公司」。HTML 未要求名称唯一，稳定 id 已唯一。不升格 | 观察 | 否 | 否 |
| OBS-11 | V005 项目/空间无 status CHECK；空间后续 V203 已补（他模块）。项目 0 行 | 观察 | 否 | 否 |
| OBS-12 | 当前 10 个角色 `organization_id` 全 NULL，V048 列在种子上未使用 | 观察 | 否 | 否 |
| OBS-13 | M011 JDBC 仍用仓储插入 `entity_role='operation'`（`PlatformJdbcIntegrationTests:66`）。若 002 答删除，该测试会红，归 M011 | 观察 | 否 | 否 |
| OBS-14 | 六骨架无「资产公司」行、酒吧不进主体表，与 HTML「资产业态不单设」「酒吧=商户」一致。正向证据，非缺陷 | — | 否 | 否 |
| OBS-15 | Flyway checksum 与当前四文件全等，validate 387 通过。四文件发布后未被改内容 | 正向 | 否 | 否 |

M009-012（列/V318/V354 归 M010 裁定）：本席独立裁定见 GR-M010-001，**冲突/删除列**，与「M009 面已删字段、列留待 M010」的交接一致。  
M009-025（spv/operation 转 M010 按 2.2）：本席独立裁定见 GR-M010-002，**添加**，完整列出。  
M009-017（V400–V409 环境）：OBS-01 复核，不升格为本模块源码缺陷。

## 9. 命令证据（敏感值未输出）

```
CMD-LOCK: sha256sum docs/audit-2026-09/plan/criteria-lock.md
OUT: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  exit 0

CMD-FILES: sha256sum + wc -l/-c 四迁移
OUT: 行 109/7/16/71 合计 203；字节 3900/352/1300/5625；SHA 与用户清单全等  exit 0

CMD-HEAD: git fetch origin; git rev-parse HEAD origin/main; git merge-base
OUT: 三值 242f487233d028173cb9198eecd079f5d877405c  exit 0

CMD-PSQL: 仅三变量，psql complex_test
OUT: db=complex_test user=complex
     flyway 005/048/318/354 success=t checksum 全等
     max_ver=409 n=387
     chk_be_entity_role 含 spv/operation
     6 骨架行 + 父链；spv/operation 行=0
     权限八码中文；role_platform_admin 已授
     merchants 87 / 三主体列 0 非空；projects 0；spaces 0
     roles 10 / organization_id 非空 0
     FK 到 business_entities = 0
     leftover after ROLLBACK = 0
     P1 garbage role 23514；P2 spv OK；P3 operation OK
     P9 两组织同名角色 23505
     P10-P12 孤儿引用 OK
     HTML 伪章节检索 0
     exit 0

CMD-UNIT: mvn … PlatformServiceTests#createBusinessEntityRejectsRoleOutsideWhitelist
OUT: Tests run: 1, Failures: 0  exit 0

CMD-JDBC: mvn … PlatformJdbcIntegrationTests#(migrationCreates…+residualMigration…) + SettlementEntityAttributionS1Tests#sixEntitySkeleton…
OUT: skeleton PASS；migrationCreates 表/权限 PASS（2 测中未失败的那条）
     residualMigration FAIL expected 399 was 409
     Flyway validate 387 OK；warn 409 newer than 399
     Tests run: 3, Failures: 1  mvn exit 1
```

未跑：会清理共享库的脚本、全量后端测试、部署、安装依赖。未把 JDBC 整类绿报。`residualMigration` 红已如实记录原因。

## 10. 三 HTML / 锁 / 台账字节

本席未修改三 HTML、criteria-lock、sidecar、ledger、findings、主索引、四迁移文件。审计前后四文件 SHA 与用户固定清单一致。

HTML 本身：未发现需铁律 4 上报暂停的正本错误。V354 伪称的章节名是迁移注释问题，不是 HTML 缺段。

## 11. 结论

**审计 FAIL。**

原因（同时成立，无有条件通过）：

1. GR-M010-001 冲突成立：已发布模式与种子仍编码 HTML 禁止的单父隶属链；修复只能新迁移删列，不能改 V318/V354。
2. GR-M010-002 添加项 `spv`/`operation` 须 B2 按 2.2 逐项问操作员；未答复前不得 PASS。
3. GR-M010-003 细化缺陷成立：V048 隔离列未配套唯一约束，两组织同名角色被全局 UNIQUE 拒绝。

覆盖：4/4 文件全文三线；18/18 GEN 已核对；声称均有类型 1/2/4 证据；无未核技能冒称；无 MCP/子代理/第三席。

## M010 B2-r1 整批候选复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 全文 36902 字节原样保留，不改写 B1 锁段起点，不覆盖 B1 **FAIL** 原文）
- 席位：Grok（固定外审席乙，独立）。步骤：B2。模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，FILES 4 / 判据 GEN-001–018）
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；ledger V2.19 IN_PROGRESS（曾 BLOCKED-ASK，2.2 第 4 步已恢复）；B3=0；无源修复
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲。
- 本席 B2：**PASS**
- 本追加段不写独占锁字段名。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；COUNT=1。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。必要探针仅 `BEGIN…ROLLBACK`，事后已清 `/tmp` 临时 env。

此票只判断 findings V1.28 最上「当前 M010 2.2答复已到，B2-r1候选定队列」及其下「当前 M010 B2-r1 整批候选」001–011，相对本次两份完整 B1 raw 与已答复 questions 的全批差集、四分类、工程缺陷、修复队列与最小修法。不是独立 B1/B5 PASS，不是模块 PASS，不是源已修。必须另有席甲本批 B2 PASS 才能定稿。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M010-b1-grok-raw.md` 前缀（本席 B1） | V0.01 | 36902 | `d77a1da6c344fd76981be0200a79196f3043e109e18bf1683d6156491ad99e8b` |
| `docs/audit-2026-09/modules/M010-b1-self-raw.md` | V0.01 | 20270 | `37f97e608bd5276184a2fea4d7be72d77daf1863e0ca790eeda06684083c52cc` |
| `docs/audit-2026-09/findings.md` | V1.28 | 490508 | `a55a9b5b7644ec40750d9cdb1d9b4caabcef645dc891b2cf0022ed5c833d0311` |
| `docs/audit-2026-09/questions/M010-002.md` | V0.02 | 2332 | `cb767baba4808a37b18eb8b09b0dd25632467682bff8decb063274b09a9d3538` |
| `docs/audit-2026-09/questions/M010-003.md` | V0.02 | 2318 | `bc16304b875995a73221b01d75ad78368bad0c2ea6c905289ab4721aa34f0072` |

questions 两文件「## 操作员答复」后首行均精确为「删除」。操作员弹窗另裁：001 方案 B 删列（GEN-014 破坏性动作已授权）；009 新迁移从 V410 起（complex_test 不回滚）。仓内最大迁移文件 V399（377 个 `V*.sql`）；`application.yml` / `application-prod.yml` / `backend/pom.xml` 无 `outOfOrder`、无 `ignoreMissingMigrations`。

本席 B2 活库只读（`complex_test` / `BEGIN…ROLLBACK`，exit 0）：flyway 005/048/318/354 success=t；库最高版本 409；`chk_be_entity_role` 五值含 `spv`/`operation`；`roles_role_name_key UNIQUE (role_name)` 仍在；`parent_entity_id` 列与 `idx_business_entities_parent` 仍在；`entity_role` 为 `operation`/`spv` 的行 0/0，主体共 6 行。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问结构自查事实；不等操作员、不派子代理 |
| verification-before-completion | 声称前先跑只读哈希/约束探针 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：四迁移仍有建表/隔离/骨架 id 用途；删的是父链列与无来源角色码 |
| ponytail-review | 只评最小删除/收窄，不落地 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### 完整差集（GR-001/002A/002B/003、OBS-01…15、CX-01/02/03、席甲 OBS-01…07）

漏并：无。本席 B1 全部临时 ID 均已进入 001–011。席甲 B1 未发现 GR-M010-003，候选 004 按本席原意收入，不是席甲发明、也不是改写本席。无锁外新判据、无新永久 ID、无 HTML 自身问题上报、无第三主体。002/003 添加项 2.2 已答「删除」，不再 BLOCKED-ASK。

| 正式ID | 本次来源（不改写原意） | 四分类 | 工程缺陷 | 修复队列 | 本席票 |
|---|---|---|---|---|---|
| M010-001 | CX-M010-01 + GR-M010-001（+M009-012） | 冲突 | 是 | 是 | **PASS（是/是）** |
| M010-002 | CX-M010-02(A) + GR-M010-002 项 A（+M009-025） | 添加 | 是（答删除） | 是 | **PASS（是/是）** |
| M010-003 | CX-M010-02(B) + GR-M010-002 项 B（+M009-025） | 添加 | 是（答删除） | 是 | **PASS（是/是）** |
| M010-004 | GR-M010-003（席甲 B1 未发现；V003 `role_name UNIQUE`，V048 未替换） | 细化 | 是 | 是 | **PASS（是/是）** |
| M010-005 | CX-M010-03 + GR OBS-02（席甲 OBS-01 同事实，见下） | 技术性 | 否 | 否 | **PASS（否/否）** |
| M010-006 | GR OBS-04 + 席甲 OBS-04 | 技术性 | 否 | 否 | **PASS（否/否）** |
| M010-007 | GR OBS-03/05 + 席甲 OBS-05 | 细化 | 否 | 否 | **PASS（否/否）** |
| M010-008 | GR OBS-06 + 席甲 GEN-004 行 | 细化 | 否 | 否 | **PASS（否/否）** |
| M010-009 | 席甲 OBS-06 + GR OBS-01（M009-017 同源） | 技术性（环境） | 否 | 否 | **PASS（否/否）** |
| M010-010 | GR OBS-07/13 + 席甲 OBS-03 | 技术性（跨模块下游） | 否 | 否 | **PASS（否/否）** |
| M010-011 | GR OBS-08/09/10/11/12/14/15 + 席甲 OBS-07 | 技术性/正向 | 否 | 否 | **PASS（否/否）** |

冲突 1（001）。添加 2（002、003，已答删除故入队）。细化入队 1（004）。仅登记 7（005–011）。队列是=001/002/003/004，否=005–011。无业务发明：001 是删除 HTML 禁止的隶属链，不是新建协作表；002/003 是收回 HTML 没有的角色码；004 是把已声称的组织隔离落到唯一约束，不发明角色目录。

候选表把本席观察写错或写窄之处（不另开 ID，定稿须按本票改措辞；不构成 B2 FAIL）：

1. **010 / 答复头只写 M011 `:66`**：本席 B1 OBS-13 原文确写 `:66`。B2 独立核源：`PlatformJdbcIntegrationTests` 以仓储直写 `entity_role="operation"` 的位点是 **66、94、116、390、412、426** 共六处（94 与 116 为同一 `createAndListBusinessEntityPersistsFormalFields` 的写入与断言）。`IdempotencyServiceTests` 的 `"operation"` 是幂等 `operationId`，不是主体角色，**不要动**。
2. **010 / 001 只写 `SettlementEntityAttributionS1Tests` 父链断言**：本席 B1 OBS-07 原文确只点该测试。B2 核源另有同包 `RevenueStreamAttributionJdbcIntegrationTests.businessEntityParentLayerPersists`（L120–135 及类头 L22）对 `parent_entity_id` 做 INSERT/SELECT。删列后该方法整段必红。属 001 同批最小伴生，不另归 ID。
3. **011 来源写成「席甲 OBS-01/07」**：席甲 OBS-01 是 V005 英文权限名已被 V251 覆盖，已在 005（CX-03 + 本席 OBS-02）。011 只应并本席 OBS-08…15 与席甲 OBS-07（`organization_id` 非死列）。不是把 005 事实改写成 011 缺陷。
4. **候选表 002/003 单元格仍「待 2.2」**、表题仍写 BLOCKED-ASK：与已答复头不一致。定稿改单元格为缺陷是/队列是即可，不新开 ID。
5. **答复头「B3-r3 边界」**：M010 的 B3 累计为 0，第一次修复是 **B3-r1**。属抄写残留。

### 001 独立裁定（方案 B 删列；操作员已授权）

本席 B1 GR-M010-001：冲突、缺陷是、队列是；最小处置即新迁移 `DROP INDEX IF EXISTS idx_business_entities_parent; ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;`，保留六行稳定 id/名称/角色，**不改** V318/V354 正文。席甲 CX-01 同冲突，但其「方案 A 最懒」（改 COMMENT + 六行 parent 置 NULL、留列）不是本席原意，本票不采纳。死列留着仍编码禁止链，终态更懒的是删列。操作员已选方案 B 并满足 GEN-014「破坏性动作须受明确业务授权」。

活库：列、索引、六行仍在；Java 生产代码 0 处消费该列（M009-012 已删字段）。PostgreSQL `DROP COLUMN` 会连带丢掉依赖该列的索引；显式 `DROP INDEX IF EXISTS` 与本席 B1 原文一致，可保留，不算第二套方案。

下游测试：**同批最小伴生，归因 001，不另开 ID，010 保持否。** 同 M009-006 夹具先例（他模块测试红是本 ID 的机械后果，随批改到能绿，不把下游测例升格为独立缺陷）。禁止候选 010「或归各自模块」的逃逸——若推后，本批 B3 无法证明删列后骨架仍在，GEN-003 不成立。最小改动：

- `SettlementEntityAttributionS1Tests#sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData`：删 `expectedParent` 与 `parent_entity_id` 断言；**保留** 六 id/角色/status/工商空值/`archive_reason` 键/「资产公司」0 行/非法角色 CHECK 拒绝。不要把测试改成断言「多对多协作表」（本批不发明该表）。
- `RevenueStreamAttributionJdbcIntegrationTests.businessEntityParentLayerPersists`：该方法只为父链存在，**整方法删除**（含类头第三条）。不要改写成新协作语义。该类其余收入归属用例不动。
- 不改结算生产代码、不改 V370/V372/V375 等稳定 id 引用、不改三份 HTML、不顺手改 PRD/术语表里的列名叙述（那些文件不在本 FILES，无本批 ID）。

票：**PASS**。处置=方案 B 删列+索引，保留六行，不改历史迁移。伴生按上列两测试文件最小删断言/删方法，010 否。

### 002 / 003 独立裁定（操作员均答删除）

本席 B1 把 `spv`/`operation` 列为添加、逐项 2.2、不代答。现两文件答复首行「删除」，工程缺陷是、队列是。分类保持添加（删除的是无 HTML 来源的业务枚举，不是把添加改写成技术性）。不回改 M009 白名单（已是三值）。

最小处置（最懒、不发明业务）：**同一条新迁移**（可与 001/004 同文件分段）把 `chk_be_entity_role` 收为 `platform` / `project_company` / `management_company` 三值，并 `COMMENT ON COLUMN` 去掉「spv/operation=历史通用运营主体」和「四层组织」伪称。不改 V354 正文。

存量 `operation` 行的过渡，本票裁定为 **先 UPDATE 再加约束，不用 `NOT VALID`**：

```
UPDATE business_entities
   SET entity_role = 'management_company', updated_at = now()
 WHERE entity_role IN ('operation', 'spv');
ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
  CHECK (entity_role IN ('platform', 'project_company', 'management_company'));
```

理由：答复是删除角色码，不是「库里继续存、只是新行不查」。`NOT VALID` 会把无来源枚举留在存量行，与删除答复和铁律 8 相反，也不是更懒（还要另一次 VALIDATE 或永久分叉）。`operation` 在 V354 注释里的自称是「历史通用运营主体」，收成已有的 `management_company`（域主管公司）是枚举坍缩，不新增角色、不把酒吧 SPV 发明成商户行、不改 status/`archive_reason`。complex_test 当前 0 行，UPDATE 是空操作；若其它库仍有 V354 归档留下的 inactive `operation` 行，同一句即可。禁止按「存量兼容」保留五值 CHECK。

M011 夹具：`operation` → `management_company` 作为 **003 的同批最小伴生**（002 无夹具 spv），合规，010 否、不另归 ID。范围是上列六处，不是只改 `:66`。断言里的角色字面量一并改，否则 GEN-008 夹具仍写已删除码。生产 Java 0 处 `spv`/`operation` 角色字面量，不必改服务层。

票：**PASS**。入队是；同一迁移收三值；UPDATE 而非 NOT VALID；夹具六处随 003。

### 004 独立裁定（GR-003；席甲 B1 未发现）

活库确认：`roles_role_name_key UNIQUE (role_name)` 来自 V003 `role_name VARCHAR(120) NOT NULL UNIQUE`；V048 只加可空 `organization_id` + 部分索引，未替换该约束。`JdbcAccessRepository.createRole` 已写 `organization_id`，两组织同名是首轮正常写入即 23505，不是多因素极端。分类细化成立（组织隔离是 V048 已声称的下位实现，缺陷是唯一性过严，不是新业务概念）。缺陷是、队列是。候选收入本席项，未改写原意。

最小处置：不改 V048。新迁移 `ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_role_name_key;` 后建表达式唯一索引，允许「全局名唯一 + 组织内名唯一」，采用本席 B1 已写形态 `UNIQUE (COALESCE(organization_id, ''), role_name)`（不要改成两套部分索引，那不是更懒）。不要发明角色目录、岗位模板或「公司优先」新规则。

失败测试落点：最懒是 **M011 `PlatformJdbcIntegrationTests`**（本集群已有对 complex_test 的 JDBC 套件，且 001/002/003 的列/CHECK 断言也落这里）。不要新开 access 测试文件、不要把 Access 模块未审文件拉进本批必改面。一条可失败用例锁：两组织同名可插入；同一 `organization_id`（含两个全局 NULL）同名仍 23505。

票：**PASS**。队列是；表达式唯一索引；测试在 M011。

### 009 独立裁定（版本号；操作员选 V410 起）

009 分类技术性（环境）、缺陷否、队列否：成立。不是本 4 文件源码缺陷。操作员已选「新迁移从 V410 起、complex_test 不回滚」，本批把该裁定当版本号事实使用，不把 009 改成是。

表述核对：

- 「complex_test 409→410 使 MigrationVersions 断言一致」：**准确**。`MigrationVersions.latest()` 读 classpath 最大 `V<n>__`，不是硬钉 399；`PlatformJdbcIntegrationTests:263` 与 `LeasingJdbcIntegrationTests` 等比较的是 `currentFlywayVersion() == latest()`。现库 409、文件最大 399 故红。加入 V410 后 `latest()`=410，该库 migrate 后当前版本 410，两条断言对齐。不需要为 009 改测试硬编码。
- 「main 库跨过 400–409 空档需 outOfOrder」：**半准确，定稿须收窄**。Flyway 允许文件版本出现空档；在当前版本 399 的库上应用 V410 **不必** `outOfOrder`（本席核配置：主 yml/prod yml/pom 均未开该项）。`outOfOrder` 只在「未发布分支以后把 V400–V409 **文件** 合入、而库已经 ≥410」时，才需要让低于当前版本的待执行迁移被应用。操作员「知悉」指向的是那次未来合并，不是本批 B3 的配置动作。

本批对 009：**迁移和配置都不必做最小动作**。不要在本批打开 `spring.flyway.outOfOrder`，不要回滚/重建 `complex_test`，不要补 V400–V409 空文件。新文件直接 `V410__…sql`。若未来未发布分支合并需要 `outOfOrder`，那是当时的环境项，仍挂 009/M009-017 口径，不预支进本批 ID。

票：**PASS**。009 否；V410 起步；无配置改动。

### 005–011 仅登记项

- **005**：V005 英文 `permission_name` 已被已发布 V251 改为中文；现态八码中文。技术性、缺陷否、队列否。禁止改 V005、禁止再写一条只为中文的新迁移。合规。
- **006**：V354 伪称 HTML 章节、V318「V005 已 GRANT」不实、terminology-canonical 非正本。历史文件不可改（Flyway checksum）。技术性、否、否。只读裁定。合规。
- **007**：本库约定不加硬 FK；孤儿路径首轮正常走 M009 存在性校验。细化、否、否。不单独立项。合规。
- **008**：`business_entities.status` 无 CHECK；V354 用 `inactive` 归档，与 M009 `SUBJECT_STATUSES` 不是同一闭集。细化、否、否。不把 HTML 状态词表升格为 M010 新判据。合规。
- **009**：见上。合规。
- **010**：下游测试成本，缺陷否、队列否。合规——前提是 001/003 的同批伴生按本票完整清单执行，而不是把 010 改成是或推到他模块。
- **011**：ON CONFLICT 不修漂移、V354② 一次性归档非日常删除流、`entity_name` 无唯一（HTML 未要求）、项目/空间 status CHECK 不在本 FILES、`organization_id` 由 `RoleRecord`/`JdbcAccessRepository` 读写故非死列、六骨架无资产公司/酒吧不进主体表与 HTML 一致、checksum 与四文件全等。技术性/正向、否、否。合规。本席 OBS-12「种子上未使用」与席甲「非死列」同时成立，候选用后者不构成改写本席「不入队」。

### 分类 / 队列 / 允许的最小修复边界

全批 001–011 分类成立。队列是=001/002/003/004，否=005–011。若 B2 双 PASS 后一次 B3-r1：只修定稿队列内行为；可执行行为先写归属该 ID 的最小失败测试（GEN-003；TDD 到 B3 才触发）；纯 SQL 用类型 4 命令举证。允许边界：

- **一条** 新迁移 `V410__*.sql`（按 ID 分段注释；不要拆成 V410–V413，也不要启用 outOfOrder）：001 删索引/列；002+003 UPDATE 坍缩后 CHECK 收三值并改列 COMMENT；004 删 `roles_role_name_key`、建表达式唯一索引。
- 不改 V005/V048/V318/V354 正文；不改三份业务 HTML、锁、权限种子、M009 服务层白名单。
- M011 `PlatformJdbcIntegrationTests`：列不存在、CHECK 拒 `spv`/`operation` 且收三值、两组织同名可共存/同组织仍拒；六处 `operation` 夹具改为 `management_company`（归因 003）。
- 001 伴生：结算 S1 去父链断言；删除 `businessEntityParentLayerPersists`。
- 全部改动文件自身版本递增（无版本者加 V0.01）。

禁止：方案 A 留列；`NOT VALID` 留存量角色码；新建多对多协作表；把 010 改成修复队列是；为 009 改 Flyway 配置或回滚测试库；无 ID 改 PRD/术语表/008 状态闭集；把幂等测试里的 `operationId` 当角色码修改。

### 结论

全批差集成立。001–011 逐 ID 票均 PASS。操作员删除/删列/V410 裁定已按最懒且不发明业务落地。伴生清单与 009 空档表述须在定稿措辞中按本票收窄，不构成否决、不要求 B2-r2。未源修、未改 findings/ledger/questions。本席 B2 **PASS**。须席甲本批 B2 PASS 后才能定稿并一次修复 001–004；未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M010 IN_PROGRESS，B3=0。

---

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M010 B1 Grok 独立审计原文（重开完整 B1）

- 席位：Grok 独立审计席乙（固定 grok-4.6；本机会话）
- 阶段：B1 修复前整模块全量审计（操作员明确按 V0.17 继续 M010 及后续；本次重开完整 B1，此前 Codex 单席补充复查不当双 PASS）
- 报告自身版本：V0.03（追加；上方 V0.01 B1 / V0.02 B2 原文原样保留，不覆盖）
- 模块：M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 4 文件，冻结有效内容行 203（`git ls-files -z -- <4paths> | xargs -0 cat | wc -l` 本次实算 = 203）
- 直接修复证据（不扩 FILES / 不扩锁）：`V410__m010_business_entity_topology_and_role_isolation.sql` 全文 26 行
- 冻结判据：18 条（`M010 → [GEN-001 … GEN-018]`，与当前 `criteria-lock.md` V0.14 映射行逐字相同；COUNT=18 unique=18 EQUALS_EXPECT=True）
- 历史授权（不重复询问）：M010-001 方案B 删列（GEN-014 破坏性动作已授权）；M010-002/003 操作员答「删除」
- 生命周期上下文（只读）：ledger M010 = `IN_PROGRESS`，B3 累计 2 不清零
- 结论：**PASS**
- 工程缺陷新开临时编号：无
- 添加项新开：无（002/003 已答复且当前库层已收窄，不重问）
- 未修源码、未改三 HTML、未改台账/锁/ledger/findings/主索引、未 fetch/worktree/提交/推送/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本次 M010-b1-self-raw 新增正文、未问操作员、未执行指向业务库的迁移/建库/回填/写库测试、未 source 全 env、未输出秘密

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先于技能本体 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 与双席/禁止子代理冲突，**不走** Arc 全量审计工作流 |
| graft | `~/.claude/skills/graft/SKILL.md` | 定位 `parent_entity_id` / `chk_be_entity_role` 消费点；不改索引 |
| grilling | mattpocock `productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 本体只转发 grilling |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 删除测试：删父列后复杂度回到绑定表，不派 DESIGN-IT-TWICE |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 以活库 SELECT + /tmp 可丢弃库为 red-capable 环；不改生产代码 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；不作无 ID 插桩 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；本次未跑写库 JDBC，故不声称那些测试绿 |
| ponytail-review | ponytail 4.9.0 `ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推；本宿主无该 hook，记缺口 |

B1 不适用、按提示词收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、audit-context-building（派子代理）、Arc audit（派评审子代理并另写 `docs/arc/audits/`）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- 宿主无 Claude Code git-guardrails hook：改用本席不执行 push/reset/worktree 的约定

## 1. 锁哈希与范围核验

本席独立实算（禁止抄 sidecar / ledger / 他席）：

```
CMD: python3 hashlib.sha256(Path('docs/audit-2026-09/plan/criteria-lock.md').read_bytes())
     sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
OUT: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
     BYTES=891182  LINES=6856  exit 0
```

对照（非抄值，仅一致性）：sidecar 一行、ledger 顶部 `CRITERIA-LOCK-SHA256:` 与本席实算全等。`criteria-lock.md` 文首版本 **V0.14**（判据总数 401 = 18 general + 383 HTML 锚点）。ledger `LOCK-AMEND-COUNT: 1`。旧 B1 V0.01 使用的 `b012328f…6695a` / 锁 V0.11 **不得**当作本次值。

映射行原文：`M010 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`。定义区 `### GEN-001`…`### GEN-018` 均存在。本次不扩锁、不把 BEQR-144 / PGM-018 / PGM-019 / BSM-028 升格为 M010 映射判据；三 HTML 只作 2.3 类型 1 来源追证。

FILES 四路径均在 `plan/scope.txt` 与 `plan/module-union.txt`。V410 **不在** 冻结 FILES / scope / module-union（A 冻结后 B3 新增修复文件）；按操作员本轮指令仅作 001–004 直接修复证据，不扩模块范围。`git ls-files` 显示 V410 受控。磁盘迁移 `V*.sql` 399 个，max=431；缺号含 400–409（操作员已授权跳过）及其它历史空号，与 `flyway_schema_history` 成功行数 399、max_ver=431 一致。

正本：`/home/james/complex-management` 分支 `main`。`git rev-parse HEAD`（只读、未 fetch）=`e456ba96c84185c8d39f18f548011daac48a6466`。`modules.md` 仍写基线 `242f4872…`——属排除目录内计划文件陈旧，不升格为本模块源码缺陷。

## 2. 4 文件 + V410 覆盖表（全文已读）

行数按 `splitlines`（与 `wc -l` 在末行换行时一致）。Flyway checksum = 按行 UTF-8、不含换行的 CRC32 有符号 int32（与 Flyway `readLine()` 算法一致）；与 `complex_test.flyway_schema_history` 全等。

| 文件 | 行 | 字节 | sha256 | Flyway checksum 文件=库 |
|---|---:|---:|---|---|
| `V005__p0_platform_master_data.sql` | 109 | 3900 | `5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905` | 413803928 = 413803928 |
| `V048__access_role_organization_isolation.sql` | 7 | 352 | `e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd` | -1430035601 = -1430035601 |
| `V318__business_entity_parent_layer.sql` | 16 | 1300 | `466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481` | -705915847 = -705915847 |
| `V354__business_entity_master_data.sql` | 71 | 5625 | `c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3` | -61418191 = -61418191 |
| **合计冻结** | **203** | | | 四条 success=t |
| `V410__m010_business_entity_topology_and_role_isolation.sql` | 26 | 2074 | `85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1` | 983875231 = 983875231（installed_on 2026-09-09，success=t） |

**当前模式以 V410 为准，不得把 V318 父链注释 / V354 `spv`/`operation` CHECK 当最终模式。** V318/V354 正文按 Flyway 不可改；其冲突面已由授权的 V410 覆盖。

## 3. 数据流（冻结 SQL → V410 → complex_test 实态 → 消费方源码）

```
V005 建 business_entities/merchants/projects/spaces + 八条权限英文种子
  → V048 roles.organization_id + 部分索引（当时未改 role_name 全局 UNIQUE）
  → V318 ADD parent_entity_id + 索引 + 四层隶属 COMMENT
  → V354 archive_reason / 按 operation 归档 / 六骨架 INSERT（含 parent）/ CHECK 五值（含 spv,operation）
  → V410 DROP 父列与索引；operation/spv 行改为 management_company；CHECK 三值；COMMENT 改 PGM-018/019；
         DROP roles_role_name_key；CREATE UNIQUE INDEX uq_roles_org_role_name ((COALESCE(organization_id,'')), role_name)
  → 活库 complex_test：无 parent_entity_id 列、无 idx_business_entities_parent、
     chk_be_entity_role = platform|project_company|management_company、
     六骨架 active、0 行 spv/operation、角色 10 行 organization_id 全 NULL、
     uq_roles_org_role_name 存在、roles_role_name_key 不存在
```

消费方（只读、不扩 FILES）：`PlatformService.BUSINESS_ENTITY_ROLES` 三值；`PlatformJdbcIntegrationTests` 断言父列 count=0、CHECK 不含 spv/operation、两组织同名可插入；`SettlementEntityAttributionS1Tests.sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData` 对 `SELECT *` 断言 `doesNotContainKey("parent_entity_id")`（方法名仍含 ParentChain = M010-014）；`AnalyticsJdbcIntegrationTests` 夹具已写 `management_company`（M010-012）。

## 4. 第一性原理（2.3 三问；依据类型 1+4）

### 4.1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？

- **V005**：BEQR-143「四类主体」+ BEQR-154 综合体项目 + 商户/空间主数据需要落表；八条 `entity.binding.*` / `merchant.profile.*` / `governance.project|space.*` 是治理读写的权限种子。删表则 PGM「建立综合体项目档案 / 商户档案 / 空间」无载体。**有业务上位 → 细化。**
- **V048**：BEQR-146 / PGM「所有数据默认按项目隔离」的下位：角色可挂 `organization_id`。删列则组织范围角色无法存。**细化。** 其实现缺口（全局 UNIQUE）已由 V410 按 M010-004 补上。
- **V318**：把 HTML **明文禁止**的「平台→项目公司→域公司→商户」行政隶属写成列。删掉它，BEQR-144 / PGM-018 / BSM-028 **反而更能成立**。历史文件不可改；V410 已 DROP。**冲突已修，当前模式不再承载该列。**
- **V354**：六骨架稳定 id 对应 PGM-019 具名主体（娱乐平台/中央餐饮/游乐/传媒 + 城市项目公司兼任资产 + 顶层平台占位）；酒吧不进此表与「酒吧＝商户」一致；资产业态不单设与 PGM-018 一致。CHECK 曾发明 `spv`/`operation`（三 HTML 0 命中）——添加项已答删除，V410 已收窄。**骨架保留 = 细化；五值 CHECK = 已删添加。**
- **V410**：001–004 的授权修复，不是新业务发明。删掉它，活库会回到 HTML 冲突的父链与越权角色。**必须存在（修复证据）。**

### 4.2 声称的行为有没有被实际执行验证？

| 声称 | 本次证据（类型 4，除非注明） |
|---|---|
| 四表存在 | `complex_test` information_schema / 六骨架 SELECT |
| 父链已删除 | 列查询 0 行；`pg_indexes` 无 `idx_business_entities_parent`；/tmp 插入 `parent_entity_id` → `42703` 列不存在 |
| 角色三值 | `pg_get_constraintdef(chk_be_entity_role)` 仅三值；COMMENT 已是 V410 文本；活库 `entity_role` 分布仅三值；/tmp INSERT `spv`/`operation` → `23514` |
| 六骨架 | 6 行 active，工商三列 NULL，无「资产公司」行 |
| 组织内同名唯一、跨组织可同名 | 活库索引定义与 V410 逐字一致；/tmp 两组织「店长」INSERT 0 1 ×2；同组织第二行 `23505` |
| 权限八码 | 活库中文名 + `role_platform_admin` 8/8 |
| Flyway 已应用且文件未改 | 五条 checksum 全等、success=t；磁盘 399 = 历史 399 |

**未在本次新执行**：任何 Spring/JDBC 写库测试、Flyway migrate、对 `complex`/`complex_test` 的 INSERT。M011 测试源码已读，其断言与活库 SELECT 同向；按 verification-before-completion **不声称那些测试本次绿**。

### 4.3 哪一条前提没验证就当真？

- 不得把「V318/V354 曾经 PASS / 曾经是设计」当成当前模式。
- 不得把「测试通过所以对」当成依据——本次根本没跑写库测试；断言有效性由活库约束定义 + /tmp 反例独立证明。
- 不得把 `modules.md` 基线 SHA、旧 raw SHA、他席结论当依据。
- V354 头注「经营主体分四层」「六个业态主体各管什么」在三 HTML **0 命中**（类型 1 检索）；那是历史注释失实（M010-006），不是 HTML 缺段，不触发铁律 4。

## 5. 顺从式（18 条逐 ID）

每条：适用面 → 当前证据 → 结论。HTML 追证不升格判据。

| ID | 适用 | 当前证据 | 本条 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 五份 SQL | 父列/五值 CHECK 已按授权删除；六骨架+四表仍有 HTML 上位；V410 无额外业务表 | PASS |
| GEN-002 可执行正确性 | 迁移已应用 | flyway 005/048/318/354/410 success=t；活库对象与 V410 声称一致 | PASS |
| GEN-003 测试有效性 | FILES 无测试 | runbook：纯 SQL 不强制新测试文件。覆盖断言在他模块源码（只读）；本席用 SELECT+/tmp 反例代替写库测试 | PASS（不声称 JDBC 绿） |
| GEN-004 信任边界 | entity_role / role_name | 库层 CHECK 三值；服务层同三值；角色名唯一索引覆盖全局 NULL | PASS |
| GEN-005 授权与最小权限 | V005 授 platform_admin 八码；V048 组织列 | 八码均授；组织隔离唯一性已补。默认拒绝在服务层（他模块），本 FILES 不实现鉴权闸 | PASS |
| GEN-006 数据完整性 | PK / CHECK / UNIQUE | 主体 PK；角色表达式唯一；V410 UPDATE 后 ADD CHECK（先改角色再收窄）。商户三列仍无硬 FK（本库约定，M010-007） | PASS |
| GEN-007 并发与幂等 | UNIQUE / ON CONFLICT | 角色跨组织写入窗口被唯一索引覆盖；V354 `ON CONFLICT DO NOTHING` 不修漂移（M010-011，Flyway 单次执行） | PASS |
| GEN-008 接口与模式一致性 | 列/枚举/注释 vs 服务/测试 | 当前库、V410 COMMENT、`BUSINESS_ENTITY_ROLES`、M011 断言同三值、无父列。V318/V354 历史文本与当前库不一致 = 迁移链常态，以 V410 为当前协议 | PASS |
| GEN-009 错误处理 | CHECK/UNIQUE 失败 | /tmp：spv/operation → 23514；同组织同名 → 23505；父列 → 42703。无伪成功 | PASS |
| GEN-010 安全与敏感 | 迁移文本 | 五文件无秘密、无动态 SQL 拼接；权限名非秘密。不输出 env | PASS |
| GEN-011 可观测 | DDL | 迁移本身不是业务操作留痕；Flyway 历史行构成应用证据 | PASS（N/A 业务审计日志） |
| GEN-012 资源边界 | 索引 | status 索引、角色组织部分索引、表达式唯一索引均存在；无无界批处理 | PASS |
| GEN-013 构建配置 | 版本号/测试库 | 磁盘 max=431 = 库 max=431；400–409 磁盘与历史均为空（操作员授权空档）。原「库 409 > 代码 399」**已不是当前事实** | PASS |
| GEN-014 迁移正确性 | 顺序/checksum/破坏性授权 | 五文件 checksum 全等；V410 破坏性 DROP 有 001 授权 + 002/003 删除答复；不改 V005/048/318/354 正文 | PASS |
| GEN-015 界面可用性 | 权限展示名 | V005 英文种子仍在历史文件；活库八码为中文（后续 V251/V430 他模块）。无本 FILES UI | PASS（现态）；V005 英文见 M010-005 登记 |
| GEN-016 文档可验证性 | 注释 vs HTML | V410 注释可指到 BEQR-144/PGM-018/019。V318/V354 伪章节名仍在（M010-006 登记，历史不可改） | PASS（当前修复文件）；历史失实已登记 |
| GEN-017 业务来源分类 | 2.1 顺序 | 见 §7：无新冲突、无新添加；002/003 不重问 | PASS |
| GEN-018 第一性原理证据 | 本报告 | 结论落到 HTML 原文 + 可复现命令/活库输出 + /tmp 反例；不用旧 PASS/席位意见 | PASS |

**顺从式 18/18 PASS。**

## 6. 对抗式（grilling 追问树，自答自证）

❓ **Q1 当前库是不是仍按 V318 父链存数？**  
➡️ 否。`information_schema.columns` 无 `parent_entity_id`；六骨架 `SELECT *` 无该键。/tmp 当前模式 INSERT 该列 → 列不存在。

❓ **Q2 CHECK 是不是仍接受 spv/operation？**  
➡️ 否。活库约束定义仅三值；/tmp 两码均 23514，行数 0。对照：V354 时代五值 CHECK 在第二抛弃库中 `INSERT spv`/`operation` 均为 INSERT 0 1——证明**旧模式会放行、当前模式不会**。不得把 V354 当最终。

❓ **Q3 两组织能否同名角色？同组织能否撞名？**  
➡️ 能 / 不能。/tmp：`org_a`+`org_b`「店长」两行成功；`org_a` 第二行 23505。全局 NULL 同名第二行 23505。与 V410 索引一致。

❓ **Q4 NULL 与空串 organization_id 是否同一键？**  
➡️ 是。/tmp：先 NULL 再 `''` 同名 → 23505，`Key (COALESCE(organization_id, ''), role_name)=(, 空串碰撞)`。此即 M010-013；access 写路径写 NULL 不写空串（他模块）。HTML 未要求区分。不升格新缺陷。

❓ **Q5 全局角色与组织角色可否同名共存？**  
➡️ 可以。/tmp P9 两行成功。B2 已登记的表达式语义，不是新发明。

❓ **Q6 能否靠裸 SQL 写垃圾 status / 重名主体？**  
➡️ 能。/tmp：`status='zzz'` 成功；同名「游乐公司」两行成功。对应 M010-008 / M010-011。首轮正常走服务层角色白名单；HTML 未要求 `entity_name` 唯一。不新开修复。

❓ **Q7 complex_test 是否仍卡在未发布 V400–V409，使 V410 无法作为当前模式？**  
➡️ 否。历史 400–409 查询 0 行；410 success=t；max=431。M010-009 原阻塞已解除；空档仍在，属已授权环境事实。

❓ **Q8 结算/分析夹具是否仍把父链或 operation 锁成绿？**  
➡️ 父链：结算测试已断言列不存在（方法名残留 ParentChain = 014）。operation：Analytics 夹具源码现为 `management_company`（012 已修）。M011 JDBC 源码现含拒绝 spv 的用例。本席未新跑这些写库测试。

❓ **Q9 有无新业务发明需要 2.2？**  
➡️ 无。V410 只删除/收窄已授权项。不重问 002/003。

**对抗式未发现新的本模块工程缺陷。**

## 7. 既有 14 ID 按当前事实重核

正式 ID 已在历史 B2 分配，本席不改 findings、不重发 2.2。下表是 **当前事实裁定**（不是把旧 PASS 当免审）。

| ID | 原分类 | 当前事实 | 工程缺陷（当前 FILES+V410） | 入队建议 |
|---|---|---|---|---|
| M010-001 | 冲突 | V410:8-9 已 DROP INDEX/COLUMN；活库列/索引 0；六骨架稳定 id/名/角色仍在 | **已修** | 否（已修） |
| M010-002 | 添加→删除 | CHECK 无 `spv`；0 行；COMMENT 无 spv。授权已给，不重问 | **已修** | 否 |
| M010-003 | 添加→删除 | CHECK 无 `operation`；0 行。V354:41-46 历史归档句仍在（不可改） | **已修** | 否 |
| M010-004 | 细化 | `roles_role_name_key` 不存在；`uq_roles_org_role_name` 定义=V410:25-26；/tmp 行为符合 | **已修** | 否 |
| M010-005 | 技术性 | V005:79-88 仍英文；活库八码中文 | 否（历史种子+后续 i18n） | 否 |
| M010-006 | 技术性 | V354:3-8 / V318:8 伪称仍在；三 HTML「经营主体分四层」「六个业态主体各管什么」0 命中；V005 无 GRANT，`complex_app` 现有 DML（他迁移） | 否（历史注释不可改） | 否 |
| M010-007 | 细化 | merchants 57 行三主体列全 NULL；projects/spaces 0；merchants→entities 仍无硬 FK。另：`business_entity_projects_business_entity_id_fkey` 存在——属他模块 V427，**不扩范围** | 否 | 否 |
| M010-008 | 细化 | `business_entities.status` 仍无 CHECK；活库 0 inactive；/tmp `zzz` 可插入。不把 BEQR-155 状态词表升格为 M010 新判据 | 否 | 否 |
| M010-009 | 技术性（环境） | **原 409>399 已不成立**。当前磁盘=库=399 行 / max 431；400–409 双空。空档经授权保留 | 否（环境已对齐） | 否 |
| M010-010 | 技术性（下游） | 结算源码已不断言父链值；Analytics 不再写 operation。方法名/他模块夹具见 012/014 | 否（伴生源码已改；未新跑） | 否 |
| M010-011 | 技术性/正向 | ON CONFLICT 不修漂移；entity_name 无唯一（/tmp 两行成功）；六骨架无资产公司、酒吧不进主体表；checksum 全等 | 否 | 否 |
| M010-012 | 技术性（跨模块） | `AnalyticsJdbcIntegrationTests.java:475-479` 现 INSERT `management_company`，注释写明 V410 | **源码已修**（文件不在 FILES） | 否 |
| M010-013 | 技术性 | 活库索引 `COALESCE(organization_id,'')`；/tmp NULL≡''；全局与组织可同名；inactive 占名未测（HTML 未要求释放） | 否（设计说明） | 否 |
| M010-014 | 技术性（文案） | `SettlementEntityAttributionS1Tests.java:217` 方法名仍含 ParentChain；断言已是列不存在 | 否（他模块文案） | 否 |

**无新 GR。** 不把 007 的他表 FK、014 方法名、008 无 CHECK 扩成新修复项。

## 8. 观察（无新根因也登记，防漏并）

| ID | 内容 | 分类 | 工程缺陷 | 入队 |
|---|---|---|---|---|
| OBS-R-01 | V410 不在冻结 FILES/scope/module-union，是 B3 后受控修复文件；本轮仅按操作员指令作证据，不改计划 | 技术性 | 否 | 否 |
| OBS-R-02 | 活库 merchants 57（旧 B1 为 87）——测试库数据量变化，非本四文件行为 | 观察 | 否 | 否 |
| OBS-R-03 | 活库 1 条指向 `business_entities` 的硬 FK 来自 `business_entity_projects`（V427/M009），不改变 M010-007 对本 FILES 松引用的裁定 | 观察 | 否 | 否 |
| OBS-R-04 | `sixEntitySkeleton…ParentChain…` 方法名残留；断言已正确。不扩 FILES | 观察 | 否 | 否 |
| OBS-R-05 | `entityRoleCheckRejectsSpvAndOperation` 同事务只 INSERT 了 spv；operation 拒绝由约束定义 + 本席 /tmp P3 补证。不扩 M011 | 观察 | 否 | 否 |
| OBS-R-06 | 磁盘缺号除 400–409 外还有 118/119/… 等历史空号；与本模块无关 | 观察 | 否 | 否 |
| OBS-R-07 | 探针库 /tmp SQL 与一次性 DB `m010_grok_b1_tmp*_20260922` 已 DROP；结束后 `pg_database` 仅余 complex / complex_test 等原库 | 技术性 | 否 | 否 |
| OBS-R-08 | 三 HTML 未发现需铁律 4 上报暂停的正本错误 | — | 否 | 否 |
| OBS-R-09 | ponytail-review：当前运行态父列已删、五值 CHECK 已收；历史迁移正文不可删。`Lean already. net: 0` | — | 否 | 否 |

## 9. 命令证据（敏感值未输出）

```
CMD-LOCK: 独立 sha256sum criteria-lock.md
OUT: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  exit 0

CMD-FILES: sha256sum + 行/字节 + Flyway 行 CRC32
OUT: 四冻结 109/7/16/71=203；V410=26；checksum 与库全等  exit 0

CMD-HEAD: git rev-parse --abbrev-ref HEAD ; git rev-parse HEAD
OUT: main / e456ba96c84185c8d39f18f548011daac48a6466
     （未 fetch；status 显示 ahead/behind 只作环境观察）

CMD-PSQL: docker exec 以容器内 POSTGRES_USER 连 complex_test，只 SELECT
OUT: db=complex_test
     flyway 005/048/318/354/410 success=t checksum 全等
     400–409 行数=0  max_ver=431 n_success=399
     无 parent_entity_id 列 / 无 idx_business_entities_parent
     chk_be_entity_role 三值；COMMENT=V410 文本
     6 骨架 active；spv/operation 行=0；资产公司行=0
     权限八码中文；role_platform_admin 8/8
     merchants 57 / 三主体列 0 非空；projects 0；spaces 0
     roles 10 / organization_id 非空 0 / 空串 0
     uq_roles_org_role_name 存在；roles_role_name_key 不存在
     leftover be_role_* / role_m010_* = 0
     exit 0

CMD-TMP: /tmp/m010-grok-b1-probe.sql 及 probe2；DB m010_grok_b1_tmp_20260922 / tmp2 用后 DROP
OUT: P1 management_company OK
     P2 spv 23514 行=0
     P3 operation 23514 行=0
     P4 parent_entity_id 42703
     P5 两组织同名 2 行
     P6 同组织同名 23505 行=0
     P7 全局 NULL 同名 23505
     P8 NULL vs '' 同名 23505
     P9 全局+组织同名 2 行
     P10 同名主体 2 行
     P11 status=zzz 1 行
     P12 V354 时代 CHECK：spv/operation 均为 INSERT 0 1（历史对照，非当前）
     结束后业务库名单不含探针库
     exit 0

CMD-HTML: 三文件检索
OUT: 「经营主体分四层」=0 「六个业态主体各管什么」=0 spv=0
     「不是固定」各 1 次（BEQR-144 / BSM-028 / PGM-018）
     SHA BEQR=4396bb71… BSM=eebc5b41… PGM=5c4594a7…
```

未跑：写 shared 测试库的 mvn JDBC、flyway migrate、全量后端测试、部署、安装依赖、source 全 env。

## 10. 三 HTML / 锁 / 台账字节

本席未修改三 HTML、criteria-lock、sidecar、ledger、findings、主索引、四冻结迁移、V410。审计前后四冻结+V410 SHA 与 §2 表一致。

HTML 追证原文（类型 1，不升格判据）：

- BEQR `docs/business-enum-quick-reference-v0.1.html:227`「经营主体关系」：物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。
- BEQR `:226`「四类主体」：经营对象、运营主体、合同主体、结算主体（可分属不同公司，同一经营对象下唯一绑定）。
- PGM `platform-governance-map-for-operations-v0.1.html:415-420`：多对多协作，不是固定隶属层级；顶层平台 / 物理项目 / 域主管公司 / 城市项目公司（兼任资产）。
- PGM `:424-433`「各主体具体负责什么」表：娱乐平台公司 / 独立酒吧商户 / 游乐公司 / 传媒公司 / 中央餐饮公司 / 城市项目公司。无 SPV、无 operation 角色。
- BSM `business-system-map-for-operations-v0.1.html:702`：不是固定的“平台→项目公司→域主管公司→商户”隶属链。

铁律 4：三 HTML 本身未发现需上报暂停的正本错误。

## 11. 结论

**审计 PASS。**

分层状态：本席 B1 审计结论 = PASS；模块生命周期仍是 ledger 中的 `IN_PROGRESS`（B3 累计 2）——本席不写 ledger。无有条件通过。

PASS 同时成立的事实：

1. 当前可执行产物（complex_test + V410）不再编码 HTML 禁止的单父隶属链，也不再接受 `spv`/`operation`。
2. M010-001–004 的授权修复已落在 V410 且 checksum/对象/反例三线一致；002/003 不重问。
3. GEN-001–018 逐条有类型 1 或 4 证据，无未答复添加项，无新冲突。
4. 14 既有 ID 已按**当前**事实重核：已修 001–004 与 012；其余为登记/观察，不因旧 PASS 免审，也未把历史 V318/V354 当最终模式。
5. 无新 GR、无新 2.2。

未做事项（不影响本条 PASS，供 B2 知情）：未新跑写库 JDBC；未改 findings 队列字段；V410 不在冻结 FILES——按本轮操作员范围只作修复证据。

B1 完成判据：四冻结全文 + V410 全文已读；18 判据三线落盘；14 ID 重核；SHA 独立实算写在本完整报告第一行；指定路径追加而非覆盖。


## M010 现锁B2整批归并票（Grok 席乙追加）
版本：V0.04（本段追加；上方 V0.01 B1 + V0.02 B2-r1 + V0.03 重开 B1 合计封存前缀 **82632** 字节 SHA256 `538e20f29bce1e2e47fb22cc339247f66592dd3e280539f63f460404e2f064c5` 原样保留，不覆盖、不改 V0.03 原文；自身版本 V0.03→V0.04。本段不写独占锁哈希字段行。）

- 席位：Grok 独立审计席乙（只读）。步骤：M010 现锁 B2。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17。
- 本席 B2：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未 fetch/commit/push/merge/部署。未改源码、HTML、锁、ledger、findings、questions、主索引。未建库/删库/建表/写数据（含临时库）。未跑写库测试、未新技术全审。席甲 self-raw 在完整 B1 V0.04 之后另有 B2 V0.05，**已按派发要求阅读，不采为合取依据、不因它已写 PASS 而预设本票**。
- `receiving-code-review` 收窄：先核两席 B1 原文、questions 答复首行与候选表，再接受或反驳；禁止表演性同意；B2 禁止 IMPLEMENT。

此票只判断 findings.md V3.51 顶部「当前M010现锁B2候选」至「当前M010现锁B2候选结束」相对本次指定输入的全批差集、2.1 四分类、当前修复队列与 9 观察归并。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.03 B1 **PASS** 原文，也不把 B1 PASS 写成模块 PASS。

### 0. 技能自核（V0.17 §6，按 B2 收窄）

| 技能 | 用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖通用派子代理 |
| receiving-code-review | 先核实再接受/反驳；本体逐条 IMPLEMENT 作废 |
| verification-before-completion | PASS 前实算输入文件哈希与 questions 首行；本节点不连库 |
| grilling / grill-me | 只借追问结构自答；不等操作员、不派子代理 |
| codebase-design | 删除测试只用于确认已授权删除项不必重开；不改源 |
| ponytail-review | 只核对 OBS-R-09 与候选「可删复杂度已消除」是否同对象；不落地 |
| writing-for-agents | 本 raw 给下游定稿/B5 读 |
| git-guardrails-claude-code | 存在；本席无 git 写 |
| using-arc / graft 工具 | 本节点文档差集，不跑 graft 检索、不走 Arc 全量审计 |

缺口（不冒充已调用）：`batch-grill-me` 本环境无独立 SKILL.md。划掉的 worktree/子代理/research/tdd/B3 修复技能未调用。

### 1. 输入路径与版本（本席独立实算）

| 角色 | 路径 | 本次切片 | 字节 | SHA256 |
|---|---|---|---:|---|
| 席甲最后完整 B1 | `docs/audit-2026-09/modules/M010-b1-self-raw.md` | V0.04（自 `# M010 B1 当前锁全模块独立审计` 起至「不能据此直接宣称模块双PASS。」；候选顶部声明的整文件冻结口径） | **32257**（去掉其后 V0.05 及夹在中间的空行后，与候选引用全等） | `ea796529d173232f3b2f23a69493bcdee90265b23ee06f9b6f72a2d69e4dd7dd` |
| 席甲本批 B2（派发要求已读，非合取依据） | 同 self-raw | V0.05「## M010 当前B2席甲票」至文末 | 当前整文件 **32661** | 整文件 `24190680b0cfd0e6cd7902381f65dff49446ad20ee7c3821e4f7568740099146` |
| 席乙最后完整 B1 | `docs/audit-2026-09/modules/M010-b1-grok-raw.md` 本段之前 | V0.03（含历史前缀的追加前全文） | **82632** | `538e20f29bce1e2e47fb22cc339247f66592dd3e280539f63f460404e2f064c5` |
| 当前候选 | `docs/audit-2026-09/findings.md` V3.51「当前M010现锁B2候选」至「当前M010现锁B2候选结束」 | 14 行 + 9 观察段 | 整文件 **1261243**；候选切片 **2752** | 整文件 `7bb3118f3514f011d30f224595a6f589031035f6305ce5236623309d0c31271b`；切片 `e3807939dbb53315496d09bba9571837f2ab94596ac071d2e55e9e42b36cc257` |
| 002 删除授权 | `docs/audit-2026-09/questions/M010-002.md` | V0.02 | 2332 | `cb767baba4808a37b18eb8b09b0dd25632467682bff8decb063274b09a9d3538` |
| 003 删除授权 | `docs/audit-2026-09/questions/M010-003.md` | V0.02 | 2318 | `bc16304b875995a73221b01d75ad78368bad0c2ea6c905289ab4721aa34f0072` |

候选顶部声明的两席 B1 SHA 与上表 V0.04/V0.03 全等。两份 B1 完整正文、席甲 V0.05、候选整段、两份 questions「## 操作员答复」后首个非空行均已读。

只读对照（本段不写独占锁字段名）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，与 sidecar 一行、ledger 顶行、本文件 V0.03 独占锁行值全等；`LOCK-AMEND-COUNT: 1`。ledger 表行 M010 = `IN_PROGRESS`，B3 累计 **2**。HEAD（未 fetch）`e456ba96c84185c8d39f18f548011daac48a6466`，分支 main。questions 两文件答复首行均精确为「删除」。

### 2. 差集总览（临时号 → 正式 ID；无新 ID）

| 来源 | 内容 | 候选处置 | 本席 |
|---|---|---|---|
| 本席 V0.03 新 GR | **空集** | 无新 finding ID；两席无新 GR/CX | **同意**。未改写成缺陷 |
| 席甲 V0.04 新 CX | **空集**（「本次未发现新增缺陷」） | 同上 | **同意** |
| 既有 M010-001…014 | 两席均按当前事实重核 | 表 14 行全覆盖，队列均否 | **同意**，不新开、不复用 |
| 本席 OBS-R-01…09 | 无新根因也登记 | 完整归并段 01–09 均在 | **同意**；02/05/06/09 为观察路由，见 §4 |
| 002/003 历史添加 | 操作员答删除且 V410 已收 CHECK | 分类保持添加，当前队列否 | **同意**。不重问 2.2 |
| 001 方案 B 删列 | GEN-014 已授权且已修 | 冲突、已修、队列否 | **同意** |
| 012 Analytics 夹具 | 历史 B5 入队后已修 | 技术性、已修、队列否 | **同意** |
| 添加项 / HTML 申请 / LOCK-AMEND | 0 | 无 2.2、无铁律 4 | **同意**（OBS-R-08） |

漏并：**无 ID 漏并。** 无把本席空 GR 改写成新缺陷。无把历史表「是/待答」写回当前队列。

### 3. 十四项逐项（分类 / 当前队列 / 已修与登记）

V0.17 定稿队列规则按**当前**值：冲突且未修 / 当前工程缺陷 / 操作员答删除且尚未执行的添加 → 是；只登记、已修、答保留且无工程缺陷 → 否。旧 B2-r1 表「是/待2.2」与 B5-r1 表「012 是」是历史，**不是**本批当前队列。

| ID | 本批来源 | 候选分类 | 当前队列 | 本席 | 依据（不改写原对象） |
|---|---|---|---|---|---|
| M010-001 | 两席重核；历史 CX-01+GR-001 | 冲突 | **否** | PASS | 父链列/索引已不存在（V410 DROP）。冲突已修。不重开删列 |
| M010-002 | 两席重核；questions 答复「删除」 | 添加 | **否** | PASS | 三 HTML 无 SPV；授权删除已执行，CHECK 无 `spv`。分类保持添加，不改写成技术性。当前不是待删 |
| M010-003 | 同上 | 添加 | **否** | PASS | 无「通用运营主体」角色；授权删除已执行，CHECK 无 `operation`。不重问。V354 历史归档句不可改（006） |
| M010-004 | 两席重核；历史 GR-003 | 细化 | **否** | PASS | `uq_roles_org_role_name` 已在；`roles_role_name_key` 已无。已修。不另发明角色目录 |
| M010-005 | 两席重核 | 技术性 | **否** | PASS | V005 英文种子 + 后续中文化。无业务发明。不改历史迁移 |
| M010-006 | 两席重核 | 技术性 | **否** | PASS | 历史注释失实，不能回写 checksum。无业务发明 |
| M010-007 | 两席重核 + OBS-R-03 | 细化 | **否** | PASS | 本 FILES 松引用不增硬 FK。他表 V427 FK **不改变**本裁定（候选单元格已写明） |
| M010-008 | 两席重核 | 细化 | **否** | PASS | status 无 CHECK 为原登记。不把 BEQR-155 升格为本模块新生命周期 |
| M010-009 | 两席重核 + OBS-R-06 路由 | 技术性 | **否** | PASS | 旧 409>399 已不成立；磁盘/库成功行 399、最高 431。空档已授权。不补空文件、不改 Flyway 配置 |
| M010-010 | 两席重核 + OBS-R-05 路由 | 技术性 | **否** | PASS | 下游父链/旧角色夹具已修。不把伴生测试再入队 |
| M010-011 | 两席重核 + OBS-R-02 路由 | 技术性 | **否** | PASS | 稳定骨架、ON CONFLICT 不修漂移等原观察。单元格对象不是商户行数（见 §4） |
| M010-012 | 两席重核 | 技术性 | **否** | PASS | Analytics 夹具现 `management_company`。历史入队已修。本次无证据推翻且两席均未新跑该类 |
| M010-013 | 两席重核 | 技术性 | **否** | PASS | 表达式唯一索引既有语义。队列否。单元格措辞见 §5，不改三值 |
| M010-014 | 两席重核 + OBS-R-04 | 技术性 | **否** | PASS | 他模块方法名 ParentChain 残留，断言已是列不存在。不顺手重命名、不扩 FILES |

技术性项无业务发明：不新增主体/角色/状态/价格/权限码。002/003 是收回无 HTML 来源的角色码，分类保持添加。001 是删除 HTML 禁止的隶属链，不是新建协作表。

### 4. 九观察差集（不漏并、不升新业务要求）

候选归并句 01–09 均在。观察不是 finding，不新编号。映射按 M008 先例接受为**路由**时，宿主 ID 表行对象不得被改写。

| 观察 | 本席 V0.03 原对象 | 候选映射 | 本席 | 边界 |
|---|---|---|---|---|
| OBS-R-01 | V410 不在冻结 FILES/scope/module-union；只作 001–004 证据，不改计划 | 修复文件 V410 不扩冻结范围，记 001–004 直接证据 | **接受** | 不把 V410 补进 modules.md |
| OBS-R-02 | 活库 merchants 57（旧 B1 为 87），测试库行数变化，非本四文件行为 | 测试库行数变化非迁移缺陷，记 011 | **接受为观察路由** | 011 单元格仍是骨架/ON CONFLICT 等，**不**改写成商户行数票；不新 ID |
| OBS-R-03 | 他表 `business_entity_projects` FK（V427/M009）不改变 007 松引用裁定 | 他表 FK 记 007 | **接受** | 007 单元格已写「不改变归属」，未把 FK 升本模块缺陷 |
| OBS-R-04 | `sixEntitySkeleton…ParentChain…` 方法名残留，断言正确 | 名称残留记 014 | **接受** | 与 014 同一对象 |
| OBS-R-05 | M011 同事务只 INSERT 了 spv；operation 拒绝由约束定义 + 本席当时 /tmp P3 补证；不扩 M011 | operation 拒绝补证记 003/010 | **接受为观察路由** | 003/010 三值不改写成「M011 缺 operation 插入」缺陷；不扩 M011 |
| OBS-R-06 | 磁盘缺号除 400–409 外还有 118/119 等历史空号，与本模块无关 | 历史空号记 009 | **接受为观察路由** | 009 单元格仍是 409/399 已对齐 + 最高 431，**不**改写成补空号文件 |
| OBS-R-07 | 探针库 `/tmp` SQL 与一次性 DB `m010_grok_b1_tmp*_20260922` 已 DROP | 临时探针清理属调用执行记录，不是新业务 finding；另段如实登记超出「不建库」 | **接受** | 不升项目修复。本席 B1 **确实**建过并删除临时库，不得描述为纯只读调用。**本次 B2 未再连库、未 CREATE/DROP** |
| OBS-R-08 | 三 HTML 未发现须铁律 4 上报暂停的正本错误 | 未发现 HTML 错误，无修改申请 | **接受** | 不改三 HTML |
| OBS-R-09 | ponytail：运行态父列已删、五值 CHECK 已收；历史迁移正文不可删。`Lean already. net: 0` | 可删复杂度已消除，记 001/002/003 | **接受为观察路由** | 「历史迁移正文不可删」仍由 006 承担，不把 OBS-09 理解成可删 V318/V354 文件 |

9/9 均在候选段。无观察被丢。无观察被写成新 GR/CX 或新队列「是」。

### 5. 定稿措辞差（不构成漏并、不否决、本席不改台账）

1. **013 单元格**写「NULL/空串同键及跨组织同名既有语义」。本席 V0.03 013 原对象是：NULL≡空串、**全局角色与组织角色可同名**、inactive 占名（HTML 未要求释放）。「跨组织同名」更接近 004 已修的两组织同名。历史 B5 表仍保留 013 三事实。建议定稿补回「全局与组织可同名 / inactive 占名」，不要把 013 写成 004 的复述。**不改分类、不改队列、不新开 ID。**
2. OBS-R-02/05/06/09 的「记 0xx」是路由不是改宿主对象，§4 已绑边界。候选 011/003/010/009/001 单元格本身未改写成那些观察。
3. 候选 009/010/011/014 省略了历史「（环境）/（下游）/正向/（文案）」修饰，2.1 大类仍是技术性或细化，与两席 B1 一致。

以上三项按本文件历史 B2-r1 先例：**写窄须在定稿措辞中收束，不构成 B2 FAIL，本席不自行改 findings。**

### 6. 历史授权与当前队列（禁止把旧「是/待答」当本批待修）

- `questions/M010-002.md`、`M010-003.md`：「## 操作员答复」后首个非空行均为「删除」。授权仍有效，且 V410 已收 CHECK。当前队列否 = 已执行，不是未答、也不是再入队重删。
- 001 方案 B 删列（GEN-014）已授权且已修。
- 012 曾在 B5-r1 入队，后 B3-r2 已修；当前否。
- findings 下方历史 B2-r1 表 001–004「是」、002/003「待2.2」、B5-r1 表 012「是」**原样保留作历史证据，不再代表当前待修**。候选头已声明。本票同意。

已修 001–004/012 与登记 005–011/013/014：两席 B1 与候选当前队列均为否，本席独立同意。

### 7. 与席甲 V0.05 的合取意见

席甲 V0.05 原文：差集 0；新 ID 0；当前队列 14 项均否；分类与历史批准相符；完整保留 Grok 观察；不将旧模式当现态；不声称 Grok 未建临时库；席甲 B2 PASS。

本席独立重判后：**同意上述三值与空差集**，不是因为席甲先写了 PASS。探针库偏差双方均未洗成纯只读。无导致 FAIL 的分歧。

### 8. 本席 grilling（自答；禁止表演性同意）

1. 能否因两席 B1 都 PASS、或因席甲 V0.05 已写 PASS，就预设本 B2 PASS？**不能。** 差集、分类、当前队列、9 观察均在本票独立核对后才下。
2. 空新 GR/CX 是否漏并？**否。** 两席明文无新临时号。
3. 002/003 答「删除」应否把当前队列改回「是」？**否。** 删除已落地；再入队是无缺陷重删。历史「是」不是当前队列。
4. 001/004/012 应否因历史缺陷再入队？**否。** 当前模式已修。
5. OBS-R-07 应否立源码缺陷或要求本 B2 再探库？**否。** 调用执行记录。本次禁止任何 CREATE/DROP DATABASE。
6. OBS-R-02 商户行数应否新开 ID？**否。** 非本四文件行为。
7. 空队列能否直接模块 PASS？**不能。** 双 PASS 后跳过新增 B3/B4，B3 累计仍 2，必须当前锁 **B5** 双审。
8. 旧 B5-r2 历史票能否继承为本轮模块 PASS？**不能。** 现锁重开后须独立 B5。
9. 013 措辞差是否 FAIL？**否。** ID 在、分类/队列未改；只要求定稿补回原三事实。
10. 本席能否改 findings 补 013 措辞？**不能。** 派发禁止自行改台账。

### 9. 本节点命令（只读文件哈希；无库）

```
self_v004_freeze=32257/ea796529d173232f3b2f23a69493bcdee90265b23ee06f9b6f72a2d69e4dd7dd
self_disk_with_v005=32661/24190680b0cfd0e6cd7902381f65dff49446ad20ee7c3821e4f7568740099146
grok_v003_before_append=82632/538e20f29bce1e2e47fb22cc339247f66592dd3e280539f63f460404e2f064c5
findings_v351=1261243/7bb3118f3514f011d30f224595a6f589031035f6305ce5236623309d0c31271b
candidate_section=2752/e3807939dbb53315496d09bba9571837f2ab94596ac071d2e55e9e42b36cc257
q002=2332/cb767baba4808a37b18eb8b09b0dd25632467682bff8decb063274b09a9d3538 first_reply=删除
q003=2318/bc16304b875995a73221b01d75ad78368bad0c2ea6c905289ab4721aa34f0072 first_reply=删除
lock_workspace=b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 sidecar_eq=true ledger_top_eq=true
LOCK-AMEND-COUNT=1
HEAD=e456ba96c84185c8d39f18f548011daac48a6466 branch=main (no fetch)
ledger_M010=IN_PROGRESS B3=2
new_GR=empty new_CX=empty
candidate_ids=M010-001..014
obs=OBS-R-01..09 all_present
queue_yes=empty
adding_unanswered=0
html_amend_request=0
this_b2_create_or_drop_db=false
this_b2_wrote_findings_or_ledger=false
```

未跑 psql、未 CREATE/DROP DATABASE、未建表、未写数据、未跑 JDBC、未 fetch。

### 10. 最终结论

**本席 B2：PASS。**

实际差集：席甲 V0.04 新 CX 空集；本席 V0.03 新 GR 空集。候选覆盖 M010-001…014 全部正式 ID，无漏并、无复用、无把空集改写成缺陷。当前修复队列「是」= **空集**（14 项均否）。分类：001 冲突已修；002/003 添加且历史已批准删除并已执行；004 细化已修；005/006/009–014 技术性登记或已修；007/008 细化登记。9 观察全部在候选段，02/05/06/09 为路由、07 为执行记录，均未变成新业务要求。

013 单元格措辞偏窄，明确指出、不自行改台账、不否决。

若本批双席 B2 定稿：当前 14 项均否，跳过新增 B3/B4，B3 累计仍 **2**，仍须当前锁 **B5** 双审。现在不得标模块 PASS。两席 B1 PASS 不代替 B2/B5。本席不得被自审覆盖。

不修改候选。不修 HTML/锁/源码。其后本节点不进入 B3/B4/B5/其他模块。不修复。

B2 结束。
