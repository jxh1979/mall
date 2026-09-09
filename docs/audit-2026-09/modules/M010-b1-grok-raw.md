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
