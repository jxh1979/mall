CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审；不只看 diff；不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：`plan/modules.md` V0.04 M032「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段」；标记 NORMAL；计划有效内容 2494 行（B1 开工口径，现已过期）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`2c178f7a47c5f744cf769e2db5f25cddccb19a27`（含 M031 检查点 PR #472）
- after tree（B3-r1 冻结）：`2418fd96bd71264a3e84bd137cd8d183e9d976d0`
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 派发词）。sidecar 一行与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射 52 项：`M032 → [BEQR-157, BEQR-182, BEQR-183, BEQR-184, BEQR-185, BEQR-186, BEQR-187, BSM-001, BSM-003, BSM-009, BSM-028, BSM-029, BSM-073, BSM-084, BSM-085, BSM-086, BSM-087, BSM-088, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-008, PGM-022, PGM-024, PGM-026, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-048, PGM-059, PGM-066]`
- findings 输入：V3.02 SHA256 `e1b89a9725c4f72a1811392b40f21fe5c6cf427a1603187ea5b1bb3d4bc8f8df`；M032-001、002 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。不把全部否当成模块已全对
- 独立性：未打开、未通读、未采信 `modules/M032-b5-r1-self-raw.md`。`ls` 见该路径存在（本会话未 `Read` 正文）。读 ledger「B5-r1 派发」段时见到席甲封存一行摘要，**不把该摘要当本席证据或结论**。旧 B1（含 B2-r1 追加段）与 B4-r1 可读，只作「B2 已定为只登记」的边界；结论以本席全文重审 + 当场实跑为准
- 未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；不对任何数据库执行写语句（跑既有测试类除外）；除 D-029 允许的一次性 worktree 外未建脚本、日志或审计临时产物（曾误写 `/tmp/m032-b5-env-export.sh`，导出前已 `rm -f`，未残留）
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 本席步骤结论：**PASS**（001、002 修复落地无回归；无新的须入队工程缺陷；W-1…W-4 与 B2 只登记观察维持；添加 0；无 HTML 修改申请）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD 落测试、ponytail 修复、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被 ledger D-029 与本轮用户指令覆盖：后端测试只在基于 origin/main 的一次性 worktree 里拷入发布口径文件后跑。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由识别为质量审查；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读并调用。`graft grep "saveGrantPolicy"` / `"new EffectivePermissions"` / `"grantPolicy"`（`--in` 收窄）。tokens saved ≈ **52,509 + 5,600 + 34,770 = 92,879**。用户要求 25 文件全文读，graft 不替代 Read。生产 `saveGrantPolicy` 入边仍是 `AccessService.updateGrantPolicyAudited`；全后端 `new EffectivePermissions(` 生产点仍仅 `AuthorizationService.computeEffectivePermissions:206` 第四参恒 `false` |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。Maven 被宿主 15s 自动转后台，本席等到进程结束读完整输出与 surefire；未挂 monitor、未派 loop 子代理 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废。结论见 §8 |
| grill-me | 同插件 productivity 目录（disable-model-invocation） | 已核路径；并入 grilling，不另开会话 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 仓储深、记录类浅缝；删除测试：删 25 文件则权限持久化与最终权限计算链消失。001 不新开接口，只把库内已有键纳入既有记录缝 |
| diagnosing-bugs / systematic-debugging | 已读 | 反馈环 = 发布口径 worktree 六类测试 + psql 只 SELECT；B5 禁止修复、不加诊断桩 |
| tdd / test-driven-development | 已核存在 | 用来判断 001 真库条能否因目标行为变坏而红（`isPositive()` / Unrecognized field）；本席不写测试 |
| verification-before-completion | 已读 | PASS 前当场：独立 sha256、after tree blob、origin/main worktree 164/164、psql 只 SELECT、jsonb `||`、Jackson 默认、worktree 删除 |
| receiving-code-review | 已读 | 禁止表演性同意。本轮无对方 raw 可收（未读本次 self-raw）。对 B2 已定为只登记的 W-1…W-4：核源后维持，无新证据不改判 |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已核 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8。`Lean already. Ship.` |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | marketplace / cache 技能列表 | 存在；A2 交叉报数要求本文件**第一行**独占 `CRITERIA-LOCK-SHA256:`，优先于该技能「首行即下一步」；CLI 不回结论 |
| audit-context-building | Trail of Bits plugin | 要求派 function-analyzer 子代理并写 `audit-context/`：与「只允许两席」及「不建临时产物」冲突，**不执行**，记缺口 |
| Arc `audit` | Arc plugin | 默认派多评审子代理并写 `docs/arc/audits/`：冲突，**不执行** |
| security-review / differential-review | Trail of Bits / Sentry 技能 | 只借授权/注入/竞态透镜在本席内跑；不派 adversarial-modeler、不另写报告文件 |
| batch-grill-me | 本机无独立 SKILL.md | **缺口**；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-029 允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove |

一次性 worktree：`git worktree add --detach /home/james/wt-m032-b5r1-grok-ro origin/main`（HEAD `2c178f7a`）。拷入主工作区现行 `AccessGrantPolicy.java`、`EffectivePermissions.java`、`AccessServiceTests.java`、`AccessJdbcIntegrationTests.java`，Jdbc ← `/home/james/audit-tmp/m032_JdbcAccessRepository.publish.java`。为过 Flyway 校验临时拷入主工作区未发布 `V427__m009_project_profiles_and_associations.sql`。后端测试只导出三个 `COMPLEX_DATABASE_*`（URL 库名改为 `complex_test`），**未** `source /etc/complex/complex.env`。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`。`git worktree remove --force`；`test ! -e /home/james/wt-m032-b5r1-grok-ro` 成立。主工作区三实现文件 git blob 核后未变。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`python3` 计 `b'\n'` + `sha256sum` 语义哈希；`git hash-object`；`git ls-files -z -- <25 paths> | xargs -0 cat | wc -l` → **2499**（计划 2494 是 B1 开工口径；B3 后 AccessGrantPolicy +5 行量级，以实算为准）。覆盖 **25/25 全文逐行，不抽样**。

| 路径 | 物理行 | 文件版本 | SHA256 | 工作区 git blob | after tree blob | origin/main blob |
|---|---:|---|---|---|---|---|
| `JdbcAccessRepository.java` | 1485 | V0.13 | `7639b374fd956a5d02a24bca0a7d6165873f2df9fe52a06fd421e7088ef995ba` | `837b04aa3544` | 同左 | `976a02abe859` DIFF |
| `AccessActor.java` | 11 | — | `5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a` | `8dd295ddcc4b` | 同 origin | 同 |
| `AccessCredentialProvisioner.java` | 12 | — | `c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b` | `d4859ba531f9` | 同 origin | 同 |
| `JdbcAccessCredentialProvisioner.java` | 69 | V0.01 | `06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e` | `11f7240b424e` | 同 origin | 同 |
| `AccessGrantPolicy.java` | 55 | V0.02 | `81873e72e8332532af9469de5e2182f2988ca2f5ab5fdf31550470930796e8e2` | `d9f4d3d27b49` | 同左 | `6adf014460a3` DIFF |
| `AccessGrantPositionSummary.java` | 10 | — | `08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df` | `2255bb2d17f4` | 同 origin | 同 |
| `AccessGrantRequestRecord.java` | 35 | — | `b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89` | `a66e5ded5698` | 同 origin | 同 |
| `AccessGrantSummary.java` | 9 | — | `7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96` | `4ce965dfaf88` | 同 origin | 同 |
| `AccessGrantUserSummary.java` | 8 | — | `80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e` | `26deb3fddad4` | 同 origin | 同 |
| `AccessUser.java` | 37 | V0.01 | `717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1` | `4ae0bafd545d` | 同 origin | 同 |
| `AuthorizationService.java` | 537 | V0.13 | `990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062` | `4bfae6d2ddde` | （主工作区未发布；after 树含此文件当时快照） | `6907271481fd` DIFF |
| `EffectivePermissions.java` | 23 | — | `55bef627be66a02a45ddc4968a15c60b1e9d3357de7e3eaeb8a819f085e0247e` | `a5e210a2289e` | 同左 | `382629b951fb` DIFF |
| `PermissionDefinition.java` | 11 | — | `1d0ee72f430acf5a5dd8ff7f46af8abdcd630e84e9af83740ffe88c37aa792b4` | `769bcf1fbaee` | 同 origin | 同 |
| `PermissionTemplate.java` | 37 | — | `84749f393404f07ba97d8cde6686496532d6f9834b86c86273ab51cc167504ee` | `92cb51df142e` | 同 origin | 同 |
| `PermissionTemplateApplication.java` | 20 | — | `660d7a9be8d5fbc06c4872278b1ce7d5d5bf8d856eea130714f1c590ae254a97` | `ccd19993f160` | 同 origin | 同 |
| `PermissionTemplateApplyResult.java` | 8 | — | `80134f3a3e8157cbf3c8af227a1f4bea9c4aa6c8986466fc31f70dd9d8a2d076` | `a3b50e078abb` | 同 origin | 同 |
| `PermissionTemplateItem.java` | 10 | — | `6f87855d883c18299f42e3703309c51cb77a1669587b6cc784a2853056c61b4f` | `52d2f5fa63a4` | 同 origin | 同 |
| `PositionRoleTemplate.java` | 34 | V0.01 | `1d09de44f1d488d0dbef17a4c62dd42cb411e61f82ec1ec61ac63c8238442b2b` | `342040efb348` | 同 origin | 同 |
| `RoleRecord.java` | 17 | — | `f0d867f56c06bbd8cfb886b6a713cbc7a85fb55e62109b28c6004e1f7ceae77b` | `19f7985153c7` | 同 origin | 同 |
| `RoleAssignmentRecord.java` | 7 | — | `2034337e12c8d9b38bb93205058b5844047148c5965932a259e9e17375f1ad0e` | `fff4cbb072c7` | 同 origin | 同 |
| `StaffLevelRecord.java` | 19 | — | `ed83f2f33532f072b88c2a271b6f39d7fdd20fbfe1ce0078634aaef66775c474` | `a8dce01b466f` | 同 origin | 同 |
| `StaffLevelAssignment.java` | 20 | — | `602c779aa27cc96d3be7794df7f772a353a8eca8b4a9d37445e66dfbb6b0af29` | `090b814763c1` | 同 origin | 同 |
| `UserPositionAssignment.java` | 14 | — | `015ec50b24fe7458a52131f7fe2e7446aab165000f5087d3f57cafc115ca3ff1` | `0dcb18635b6a` | 同 origin | 同 |
| `UserPositionAssignmentResult.java` | 10 | — | `437d1c416d5027911eff9d34c421d93202dbe523f9658653406acdec9524775c` | `1a81a0ac58d1` | 同 origin | 同 |
| `package-info.java` | 1 | — | `bf3835ba131ca4ad8c2e1ab5311cab18d2f9fe5755ecee9a311b47095af9efe8` | `7a7a0c8667b3` | 同 origin | 同 |
| 合计 | **2499** | | | | | |

发布变体 `m032_JdbcAccessRepository.publish.java`：1482 行，SHA256 `30faa630325fe784785b29ca37fd5eb4d5429f9215ca03f17ef9c6d1aec1ee5f`，git blob `2408af796642c3cfbc5b86d3e0d1d11e90594085`。相对 origin/main **+2 / −2**（版本行 V0.12→V0.13 + `saveGrantPolicy` SQL 一行）。`rg requireProjectScopeId` 在发布变体 **0 处**。主工作区 Jdbc 与发布变体 **不是**同一字节（主工作区另含未发布范围闸 / `grant_group_id` 消耗等，D-029）。

21/25 文件与 origin/main 逐字节相同。DIFF 四文件：`AccessGrantPolicy`（001）、`EffectivePermissions`（002）、`JdbcAccessRepository`（001 SQL + 未发布 hunk）、`AuthorizationService`（仅未发布；002 不改此文件）。`AccessGrantPolicy` / `EffectivePermissions` / 主工作区 `JdbcAccessRepository` 的 git blob 与 after tree `2418fd96` 同路径 blob **逐字节一致**。

只读追证（非本模块文件，不扩大入队范围）：`AccessService.grantPolicy` / `updateGrantPolicyAudited`（`new ObjectMapper()`）；`AccessController` GET/POST `/grant-policy`；`AccessRepository` 接口契约 `listTemplates(null)=全量` 与 `listUserIdsAssignedToPosition` 默认实现；`V389`；`SessionIssuer` origin 无 `MASTER_ACCOUNT_TYPE`；前端 `AccessGovernancePage.tsx` GrantPolicy 类型（API 多出可选字段影响）；`jackson-databind` 2.21.5。

相关三 HTML（只读，铁律 4 未改；本席 `sha256sum`：enum `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`、治理地图 `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`）：

- 治理地图 PGM-026 / PGM-029 原文「全平台审批通用时限：任一业务审批节点超过后台配置的时限（业务参数…出厂默认 3 个工作日…须同时配置上下限）」
- 业务地图 BSM-120「员工权限按单一功能最小颗粒度配置」「职级可按 M1-M10，并可分上中下」
- 业务地图 BSM-003 / BSM-007（BSM-007 不在本模块 52 项映射内，只作对照）：平台管理员拥有全部日常功能权限；资金/敏感/删除/最终审批不因该身份自动获得
- 治理地图 PGM-001「界面全中文」；系统内部标识符四样例外

---

## 2. 第一性原理三问

### 2.1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？

本模块 25 文件是权限域的持久化适配器 + 最终权限计算 + 记录类型。删掉则：

- BSM-009 / BSM-028「管理员工、岗位、职级、权限」没有后台落点
- BSM-120「权限按单一功能最小颗粒度」「职级 M1–M10 上中下」没有 `permissions` / `staff_levels` 读写
- PGM-026 全平台审批通用时限没有 `access_grant` 政策行的读写缝（001 正是把该键纳入记录类）
- PGM-001「再管资格：员工是否有权限」没有 `AuthorizationService.requirePermission` / `EffectivePermissions`

`package-info.java` 仅一行 `package com.complex.platform.access;`：Java 包声明，删了不影响 HTML，归技术性、无缺陷。若干记录类是仓储 SELECT 的投影，删了编译失败，不是业务发明。

答得上 → 有业务上位。答不上的（考勤日结果、请假类型、设备领用、企微会话原文）走 2.1：**无本面**，见 §7，不因此删实现、不发明考勤模块。

### 2.2 它声称的行为，有没有被实际执行验证过？证据在哪？

类型 2 + 4：

1. 发布口径一次性 worktree（origin/main + 001/002 补丁 + 发布变体 Jdbc + 两份测试 + 临时 V427）：
   ```
   export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
   # 只导出 COMPLEX_DATABASE_URL（dbname=complex_test）/ USERNAME / PASSWORD
   mvn -o -q -Dtest=AccessControllerTests,AccessJdbcIntegrationTests,AccessServiceTests,AuthorizationServiceTests,M006JdbcRegressionTests,M006SecurityRegressionTests test
   ```
   `EXPORTED_KEYS=['COMPLEX_DATABASE_PASSWORD','COMPLEX_DATABASE_URL','COMPLEX_DATABASE_USERNAME']`；`URL_DB=complex_test`。Flyway `Successfully validated 396 migrations`；schema 当前 428，无新迁移。jdbc `jdbc:postgresql://127.0.0.1:5432/complex_test`。`MVN_EXIT:0`。surefire：

   | 类 | tests | failures | errors | skipped |
   |---|---:|---:|---:|---:|
   | AccessControllerTests | 13 | 0 | 0 | 0 |
   | AccessJdbcIntegrationTests | 25 | 0 | 0 | 0 |
   | AccessServiceTests | 54 | 0 | 0 | 0 |
   | AuthorizationServiceTests | 21 | 0 | 0 | 0 |
   | M006JdbcRegressionTests | 12 | 0 | 0 | 0 |
   | M006SecurityRegressionTests | 39 | 0 | 0 | 0 |
   | 合计 | **164** | **0** | **0** | **0** |

2. `psql` 只对 `complex_test` 做 SELECT（Maven 之后）。`current_database()=complex_test`。`access_grant` 行 `enabled=t`，keys=`{deadlineWorkingDays,positionThreshold,riskReviews,userThreshold,windowDays}`，`deadlineWorkingDays.value=3,min=1,max=30`，`windowDays.value=30`。真库条跑过后键仍在、值仍为正（与测试断言一致，未把政策写坏）。

3. Jackson 2.21.5：`new ObjectMapper()` jshell → `FAIL_ON_UNKNOWN_PROPERTIES=true`，`FAIL_ON_MISSING_CREATOR_PROPERTIES=false`。字节码 `<init>(String,IZ)` 对 `FAIL_ON_UNKNOWN_PROPERTIES` 末参 `iconst_1`，对 `FAIL_ON_MISSING_CREATOR_PROPERTIES` 末参 `iconst_0`。

类型 1：见各判据 HTML 原文引用。类型 3：Jackson `DeserializationFeature` 枚举默认态（上）；PostgreSQL `jsonb ||` 浅合并（本席 SELECT 实跑，见 §4）。

### 2.3 它依赖的前提，哪一条是没验证过就当真的？

已验证：

- 跑过 V389 的 `complex_test`：政策 JSON **恰好 5 个顶层键**，无第 6 未知键（否则 `new ObjectMapper()` 仍会 409）。
- 未跑 V389：缺 `deadlineWorkingDays` 时 Jackson 缺 creator 属性默认不失败 → null；紧凑构造器非空闭集不含第 5 组件（源码 L17–21）。本席无「未跑 V389 的第二套库」可挂，用类型 3+源码闭合，不假装跑过第二套库。
- `jsonb ||`：省略键保留左侧；同名顶层键右侧覆盖；嵌套对象整值替换（SELECT 见 §4）。
- `@JsonInclude(NON_NULL)`：4 参序列化不写 `"deadlineWorkingDays":null`，避免 `||` 把对象盖成 JSON null（SELECT 证明右侧 JSON null 会覆盖）。
- 发布口径 Jdbc **没有** `requireProjectScopeId`；主工作区有。W-1 只在未发布版成立。
- origin/main `SessionIssuer` **无** `MASTER_ACCOUNT_TYPE`；主工作区 `AuthorizationService:141` 依赖它。W-4 只在未发布版成立，「origin/main + 本模块 25 个现行文件」编译失败的前提成立。

未当成缺陷的前提（不是没验证，是验证后不发明需求）：

- 前端 `GrantPolicy` 类型未声明第 5 字段（M038，本模块外）。运行时 `api.get` 的对象会带上额外键；保存走 `{ ...grantPolicy, [key]: ... }` 展开，会把 `deadlineWorkingDays` 原样带回。即使剥掉，仓储 `||` 仍保库内值。不把「前端必须画出时限输入框」立成本模块需求。

---

## 3. 重点核：001 / 002 修复后行为

### 3.1 授权政策读写（跑过 V389 / 未跑 V389）

**读（跑过 V389）。** `AccessService.grantPolicy:1933-1940`：`new ObjectMapper().readValue(json, AccessGrantPolicy.class)`，失败文案「后台授权政策格式或上下限无效」。记录类现 5 组件（L13–16 原句）：

```
public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews,
                                @com.fasterxml.jackson.annotation.JsonInclude(...) BoundedInt deadlineWorkingDays)
```

紧凑构造器非空闭集仍只强制前四（L17–21 原句 `windowDays == null || positionThreshold == null || userThreshold == null || riskReviews == null || !riskReviews.keySet().equals(Set.of("low","medium","high","critical"))`），**未**把第 5 列入。GREEN 真库条 `storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays`：`readValue(stored, AccessGrantPolicy.class)` 成功且 `deadlineWorkingDays.value` 为正。

**读（未跑 V389）。** 库 JSON 无该键。`FAIL_ON_MISSING_CREATOR_PROPERTIES=false` → 第 5 组件 null，不进 `BoundedInt` 校验。4 参便捷构造器 `this(..., null)`（L26–29）仍在。正常触发：尚未执行 V389 的库上 GET `/grant-policy` 与凡经 `grantPolicy()` 的路径不再因未知字段 409，也不因缺键 409。

**写。** 主工作区 `JdbcAccessRepository.java:776-783` 与发布变体 L784-787 同一 SQL 文本：

```
UPDATE supervisor_review_policies SET parameters = parameters || CAST(:parameters AS jsonb), updated_at = now()
 WHERE policy_code = 'access_grant' AND enabled = true
```

origin/main 仍是 `parameters = CAST(:parameters AS jsonb)` 整份覆盖。GREEN 真库条：四参保存后 `parameters -> 'deadlineWorkingDays' ->> 'value'` 仍等于保存前。V389 自身也用 `parameters = parameters || jsonb_build_object('deadlineWorkingDays', ...)` 且 `WHERE NOT (parameters ? 'deadlineWorkingDays')`（`V389:17-22`），与 001 合并写同方向。

### 3.2 jsonb 合并写：被删键与嵌套键

本席 SELECT（只读）：

```
-- 省略 deadline、只换 windowDays、riskReviews 只带 low
{"windowDays":{"value":30},"deadlineWorkingDays":{"value":3,...},"riskReviews":{"low":...,"high":...}}
  || {"windowDays":{"value":14},"riskReviews":{"low":{"approvalLevel":"two_levels_up"}}}
→ deadline 保留；windowDays=14；riskReviews 只剩 low（整键替换，high 消失）
```

```
{"deadlineWorkingDays":{"value":3}} || {"deadlineWorkingDays":null}
→ {"deadlineWorkingDays": null}
```

```
{"a":1,"b":2} || {"b":9,"c":3} → {"a":1,"b":9,"c":3}
```

**被删键：** `||` 不能删除左侧键。001 的最小处置就是「客户端没带的键保持库内原值」。要通过记录类「删掉」`deadlineWorkingDays`，4 参 + NON_NULL 不会序列化该键，库内保留——这是预期，不是缺陷。若有人绕过记录类直接把 JSON null 写入 `saveGrantPolicy(String)`，SELECT 证明会盖成 null；生产入边是 `writeValueAsString(request.policy())`，NON_NULL 挡住这条。不为「需要删除时限键」加 `-` 算子（不为理论场景加需求）。

**嵌套键 / riskReviews 整键替换：** PostgreSQL `jsonb ||` 对同名键做**浅**覆盖，嵌套 object 整值替换。记录类强制 `riskReviews` 四键闭集（low/medium/high/critical），经 `AccessGrantPolicy` 的第一次正常保存**不能**只带 low。SELECT 里「只带 low 则 high 消失」**不是**本模块记录类的正常触发路径。整键替换符合「保存一份完整审批规则表」的预期。不把深合并立成新需求。

### 3.3 API 对外 JSON 多出可选字段

GET `/api/v1/access/grant-policy`（`AccessController:120-122`，M031 文件，本席只读追证）返回 `AccessGrantPolicy`。跑过 V389 后 JSON **会多出** `deadlineWorkingDays`（NON_NULL，未跑 V389 则不输出）。

前端 `apps/web/src/admin/pages/AccessGovernancePage.tsx:16-17` 原句：

```
type GrantPolicy = { windowDays: BoundedInt; positionThreshold: BoundedInt; userThreshold: BoundedInt;
  riskReviews: Record<"low" | "medium" | "high" | "critical", { approvalLevel: string; requiredLevel: number }> };
```

类型未声明第 5 字段。TypeScript 擦除后，`api.get` 的运行时对象仍持有额外键；保存 `setGrantPolicy({ ...grantPolicy, [key]: ... })` / `riskReviews: { ...grantPolicy.riskReviews, ... }` 会把该键带回 POST。服务端现能反序列化该键；即使前端剥掉，仓储 `||` 仍保库内值。UI 不展示时限输入——属 M038，**不**在本模块发明「必须加表单项」。OpenAPI 本就不列 `/grant-policy`（XM-M031-C → M392，已登记）。加法字段与 PGM-026「后台可配时限」同向，不是冲突。

`updateGrantPolicyAudited` 返回 `request.policy()` 而非再读库：四参请求的响应对象 `deadlineWorkingDays=null`，与库内仍有该键不一致。第一次正常路径：前端用 GET 填充再 POST，展开带回键；或剥掉则 UI 暂不显示但库正确。不把「写接口必须回显合并后的整份 jsonb」立成新需求。观察，不升队。

### 3.4 EffectivePermissions 注释与实现

origin/main 类注释现在时（原句）：「`wildcard=true` 仅当 actor 命中平台管理员桥（status=active 且 accountType=platform_admin），但前端/后端仍须让 denies 优先于 wildcard。」

现行（L13–14 原句）：「`wildcard` 现恒为 `false`：平台管理员不再是通配，其日常功能码已直接并入 `grants`（见 `AuthorizationService#effectivePermissions`）；`denies` 仍优先。字段保留是为不改接口形状。」

实现：`AuthorizationService:206` 原句 `return new EffectivePermissions(userId, List.copyOf(effective), denies, false, checkedAt);`。graft：全后端 `new EffectivePermissions(` 仅此一处。`computeEffectivePermissions` 仍用参数 `wildcard` 在 L200-204 把日常码（及主授权的全部码）并入 `effective`，但**记录字段**恒 false。origin/main 同文件同样第四参 `false`。字段与计算未改。GEN-016：现在时声称可从当前源码复现。

平台管理员日常码并入 grants 的路径：`effectivePermissions()` 传 `isPlatformAdminBridge(actor)`；`effectivePermissionsForUser` 对目标 `platform_admin` 传 true。与注释「已并入 grants」一致。`requirePermission` 对平台管理员桥走 `permissionGrant` 的短路（L80-83），不依赖 `EffectivePermissions.wildcard`。

---

## 4. 其余仓储 SQL / 最终权限链 / 单次消耗与到期留痕 / 锁

**最终权限计算链（两版相同的核心）。** `findPermissionEffects`（Jdbc L854-904）：角色 + 定岗派生角色 + 模板应用 + 临时授权 + 用户覆盖；读前 `expireLapsedEffects`（L910-939）把到期仍 active 的临时授权/模板应用置 `expired` 并写 `permission_template_usage_logs`。`computeEffectivePermissions`：DENY 列表、GRANT 预扣 DENY、wildcard 参数只用于补日常码、返回字段 false。`requirePermissions` 按 `source+sourceId` 去重后 `markSingleUsePermissionConsumed`。与 HTML「到期、单次消耗、失效全部留痕」同向。读时顺带清理：B2 已观察，无新证据。

**单次消耗。** 发布变体 / origin/main：`WHERE id::text = :source_id`（按行 id）。主工作区：`WHERE grant_group_id = :source_id`（整组消耗）。后者只在未发布版成立，归 W-3 工作流，不入本模块队列。GREEN 发布口径 39 条安全回归含消耗路径。

**到期留痕。** `expireLapsedEffects` UPDATE … RETURNING 后 `logExpiry`；`AND status = 'active'` 使重复读不再写。幂等。B2 观察维持。

**锁。** `lockGrantAuthorizationFacts`（L41-46）`LOCK TABLE ... IN SHARE MODE` 覆盖 users/roles/permissions/templates/position_role_templates/staff_levels，**不含** `supervisor_review_policies` / `temporary_permission_grants`。无咨询锁。`findGrantRequestById` / `findPositionRoleTemplateById` `FOR UPDATE`；`findPositionRoleTemplate` `FOR SHARE`。B2 观察「无咨询锁、用表锁与行锁」维持。不为「政策行也要进 SHARE 锁」加需求（两管理员同时改政策不是第一次正常跑必撞）。

**其它 SQL / 库结构（主工作区，注明版本）。**

- `listTemplates(org)` / `listUsers(org)` / `listRoles(org)` / `listPositionRoleTemplates(org)` 调 `requireProjectScopeId`（L1473-1476）：null/blank → `PROJECT_SCOPE_REQUIRED`。与 `AccessRepository:19-22`「Null = platform admin (all templates)」冲突。**只在主工作区未发布版成立**（origin/main 与发布变体 0 处 `requireProjectScopeId`）。= W-1，不改判。
- `listTemplates(org)` 只 `owner_scope_id = :organization_id`，不含平台模板；`listRoles(org)` 含 `organization_id IS NULL` 全局角色。B2 观察维持。
- `listUserIdsAssignedToPosition`：Jdbc **未覆盖**，走接口默认 N+1（`AccessRepository:149-157`）。M031 已登记。无新证据。
- `copyTemplate` 自调用 `createTemplate`：同类自调用不走 Spring `@Transactional` 代理。生产有服务层幂等事务。B2 观察维持。
- `knownPermission` 吞异常当无权限（`AuthorizationService:257-262`）。保守方向。观察维持。
- `findTemporaryGrant` `UUID.fromString(id)`：非 UUID 抛 `IllegalArgumentException`。观察维持。
- 凭证 upsert 清 `failed_attempt_count` / `locked_until`（`JdbcAccessCredentialProvisioner:48-55`）。观察维持。
- `access_users.partner_org_id`、`position_role_templates.archived_by` 未映射到记录类。观察维持。
- `PositionRoleTemplate.withLifecycle` 恒写 `"inactive"`（L30-32）；`savePositionTemplateLifecycle` SQL 恒 `SET status = 'inactive'`。M031-001 已在调用侧解决。观察维持。
- `pageGrantRequests` / `listGrantRequests` 对 null 组织仍全量。与 W-1 收紧不一致。Grok O-4/O-5，B2 登记未发布工作流。维持。
- `createTemporaryGrant` 逐码插入（L615-646）。观察维持。
- `deletePositionRoleTemplate` 抛「须经过存档及删除审批，禁止物理删除」（L500-502）。与 BSM-003/004 删除走审批同向。

**AuthorizationService 未发布差异（W-2 / W-4 / 观察）。**

- W-4：`isMasterAuthority` 用 `SessionIssuer.MASTER_ACCOUNT_TYPE`（L141）。origin/main 用 `isPlatformAdminBridge` + `MASTER_USER_ID` + `MASTER_USERNAME`，且 origin `SessionIssuer` 无该常量。维持。
- W-2：`currentProjectScopeQuietly`（L504-507）`Optional.of(requireProjectScope(actor))`，无组织即抛。origin/main：平台管理员返回 empty（全量），注释写「永不抛」。维持。
- `currentMerchantScope`（L425-431）：无商户号即抛，**不**给平台管理员 empty。origin/main 平台管理员 empty。Grok O-4/O-5。维持。
- `currentSupervisorLevel`（L308-320）`Math.min(derived, 2)` 封顶。B2 观察「origin/main 不封顶；封顶在未发布工作流」。维持。

无新证据，不把 W-1…W-4 或上述观察改判入本模块队列。

---

## 5. 001 / 002 是否修好（回归）

| ID | 原缺陷 | 现证 | 回归？ |
|---|---|---|---|
| M032-001 | 四字段记录 + 整份覆盖 jsonb → 跑过 V389 的库上 `grantPolicy()` 409；保存抹掉 `deadlineWorkingDays` | 五组件可空 + NON_NULL + 4 参构造器；`||` 合并（主工作区与发布变体同文）；GREEN 真库条读出正数并在四参保存后仍在；psql 五键仍在；Jackson 未知字段仍默认失败但该键已建模 | 否 |
| M032-002 | 类注释声称 `wildcard=true` 仅当平台管理员桥 | 注释改为恒 false + 日常码并入 grants；构造点仍 L206 第四参 `false`；字段未改 | 否 |

正常触发（001 修复后）：跑过 V389 的库上 GET 授权政策 / 授权申请列表 / 含须审批权限的申请不再因 Unrecognized field 409。未跑 V389 的库缺键仍能反序列化。保存四参政策不抹时限键。

四分类：001 仍是**细化**（上位 PGM-026 全平台审批通用时限；已修，队列否）。002 仍是**技术性**（无业务发明，只改现在时注释；已修，队列否）。依据类型 2+3+4。冻结判据 GEN-002 / GEN-006 / GEN-008（001）、GEN-016（002）；PGM-026 为 001 上位，本席不扩锁。

---

## 6. 顺从式：52 项逐项

每条：适用 / 无本面。不新增判据。证据落到本模块 25 文件或只读追证。

| ID | 本面 | 证据（摘要） |
|---|---|---|
| BEQR-157 员工状态 | 无本面 | HTML「待入职、在职、试岗、停岗、离职」。本段 `AccessUser.status` / `AccessActor.status` 是账号状态字符串（如 active），不是该人事枚举的落地。无冲突可指。人事枚举不在这 25 文件 |
| BEQR-182 考勤日结果 | 无本面 | 无出勤/缺勤/迟到字段或 SQL |
| BEQR-183 打卡方式 | 无本面 | 无 NFC/扫码/充电桩 |
| BEQR-184 请假类型 | 无本面 | 无请假实体 |
| BEQR-185 请假单状态 | 无本面 | 同上 |
| BEQR-186 薪资单构成 | 无本面 | `StaffLevelAssignment.baseSalaryAmountCent` 是定岗薪资基数，不是薪资单项目/绩效档 |
| BEQR-187 员工提成发放 | 无本面 | 无发放/个税/银行通道 |
| BSM-001 整体说明 | 适用 | 「谁能操作」：权限仓储与最终权限计算。无「策略写死」冲突。时限键来自已有政策行，不新发明 |
| BSM-003 权限分岗 | 适用 | 角色/模板/岗位映射/覆盖；删除岗位模板禁止物理删（L500-502） |
| BSM-009 基础管理 | 适用 | 员工/权限是基础管理的本段落点 |
| BSM-028 主要功能 | 适用 | 「管理员工、岗位、班次、职级、权限」：users/roles/staff_levels/position_role_templates/permissions。班次实体不在本段（无本面子句，不升队）。红线④数据隔离：发布口径不因平台管理员关隔离；主工作区未发布闸归 W-1/W-2 |
| BSM-029 和谁有关 | 适用 | 「用户权限要知道员工注册信息、职级」：AccessUser + StaffLevel*。设备绑定/私人手机无本面 |
| BSM-073 商户主要功能 | 部分无本面 | 商户账单/工单不在本段。`merchantId` 隔离键适用 PGM-048 交叉。不实现商户专区 |
| BSM-084 员工手机端 | 无本面 | 排班考勤请假薪资 App 不在这 25 文件。职级记录是前置，见 BSM-120 |
| BSM-085 日常工作 | 无本面 | 注册路径/公告/工单/代订不在本段。`AccessUser` 只存账号投影 |
| BSM-086 排班考勤请假 | 无本面 | 无排班表/打卡 |
| BSM-087 我的薪资 | 无本面 | 无薪资单 |
| BSM-088 提成与人员服务边界 | 无本面 | 无提成计算/发放 |
| BSM-114 各类入口 | 适用 | 有效权限集供各入口裁剪；本段不算前端入口本身 |
| BSM-115 相互关系 | 适用 | 「基础管理连所有业务」的权限/归属键 |
| BSM-120 已确认规则 | 适用 | 「职级 M1-M10 上中下」→ `StaffLevelRecord.mainLevel/subLevel/levelRank`（注释 L5-8 原句）。「权限最小颗粒度」→ `PermissionDefinition.permissionCode`。私人手机设备标识无本面 |
| GEN-001 | 适用 | 25 文件均有可验证用途；`package-info` 仅包声明，无重复抽象。001 未新开接口 |
| GEN-002 | 适用 | 声称的政策读写与有效权限构造：GREEN 164 + psql。001 原 409 路径已通 |
| GEN-003 | 适用 | 真库条 `isPositive()` 能因缺键/抹键而红；内存政策 JSON 已补键。测试在 M035/M036，本席跑了。本模块自身无测试文件 |
| GEN-004 | 适用 | 仓储入参走命名参数；`UUID.fromString` 非 UUID 抛错（观察，不升队）；政策 jsonb 经记录类校验四键闭集与 BoundedInt 上下限 |
| GEN-005 | 适用 | `requirePermission` 服务端鉴权；平台管理员日常码并入 grants 但资金/敏感/删除仍须逐项（`isExplicitOrDailyGrant` + 治理本职白名单）。wildcard 字段不放行 |
| GEN-006 | 适用 | 001 `||` 避免抹键；`replacePositionGrants` / 凭证 upsert 有行数检查。政策行无独立行锁（观察） |
| GEN-007 | 适用 | 单次消耗 `consumed = FALSE` 原子守卫（L1023-1024 注释原句 TOCTOU）。到期清理幂等。copyTemplate 自调用无事务：观察 |
| GEN-008 | 适用 | 001 记录类与库 jsonb / V389 键名对齐。前端类型未声明第 5 字段但不破坏运行时。发布口径与主工作区 Jdbc 故意分叉（D-029） |
| GEN-009 | 适用 | 政策解析失败显式 409 中文；`saveGrantPolicy` changed!=1 抛「授权政策未配置」。`knownPermission` 吞异常：观察 |
| GEN-010 | 适用 | SQL 均命名参数，未见字符串拼接用户输入。`phoneHash` `@JsonIgnore`。凭证 bcrypt。不 source 全量 env |
| GEN-011 | 适用 | 消耗/到期写 usage_logs。政策变更成功/失败留痕在 AccessService（M031-003），本段仓储不写审计表 |
| GEN-012 | 适用 | `listRoles` 批量挂权限码消 1+N（L318-337）。`listUserIdsAssignedToPosition` 默认 N+1：已登记。SHARE 表锁范围有限：观察 |
| GEN-013 | 适用 | 发布口径 worktree 离线 Maven 可构建。主工作区 AuthorizationService 依赖未发布常量：W-4 |
| GEN-014 | 适用 | 本段无迁移文件。行为依赖 V389（M041）。临时拷 V427 仅过 Flyway 校验，未改变政策语义 |
| GEN-015 | 无本面 | 无 UI。异常文案中文。内部 status 英文作系统内部标识符（PGM-001 四样例外） |
| GEN-016 | 适用 | 002 注释与唯一构造点一致。类注释不再把 wildcard=true 写成现在时 |
| GEN-017 | 适用 | 本席分类未把时限键当添加（HTML 已有可配时限）；未把 W 项改判添加 |
| GEN-018 | 适用 | 结论落到 HTML / 测试输出 / Jackson 字节码 / psql / jsonb SELECT |
| PGM-001 整体定位 | 适用 | 资格/权限底座。界面全中文：本段无页面；API 记录类字段名为英文标识符 |
| PGM-008 员工和手机 | 部分无本面 | 员工账号投影适用。私人手机绑定/企微无本面 |
| PGM-022 空间和点位 | 无本面 | 无楼层/点位实体 |
| PGM-024 影响业务 | 无本面 | 「班次要知道责任区域」本段无班次-区域绑定 |
| PGM-026 主要功能 | 适用 | 商户档案不在本段；**审批通用时限**作为 `access_grant.parameters.deadlineWorkingDays` 的读写缝，001 使其可读可保。出厂 3 日与 psql value=3 一致 |
| PGM-032 员工和手机 | 部分无本面 | 账号/状态适用；设备标识无本面 |
| PGM-033 主要功能 | 无本面 | 注册两条路径/换机解绑不在本段 |
| PGM-034 工作手机和企微 | 无本面 | 无企微同步 |
| PGM-035 提成发放 | 无本面 | 无发放 |
| PGM-036 设备管理 | 无本面 | 无设备台账 |
| PGM-048 商户侧 | 适用 | `merchantId` 过滤；跨商户拒绝在 AuthorizationService。主工作区 `currentMerchantScope` 对无商户管理员抛错：未发布，O-4 |
| PGM-059 个人信息保护 | 适用 | `AccessUser.phoneHash` `@JsonIgnore`；加密信封不在本段（M049） |
| PGM-066 已拍板 | 适用 | 「权限最小颗粒度」；「所有有过程的动作留痕」→ 消耗/到期日志。集团视图无本面 |

52/52 已核对。无锁外主张。

---

## 7. grilling 自问自答（不派子代理、不等操作员）

1. **001 是否只加了 `@JsonIgnoreProperties` 而没建模该键？** 否。记录类有第 5 组件；GREEN 读出 value>0。
2. **紧凑构造器是否把第 5 列入非空闭集，导致未跑 V389 的库 409？** 否。L17-21 未列入。`FAIL_ON_MISSING_CREATOR_PROPERTIES=false`。
3. **缺 NON_NULL 时 4 参保存会不会把时限盖成 JSON null？** 会（本席 SELECT：右侧 null 覆盖）。源码有组件级 NON_NULL；GREEN 四参保存后 value 仍为正。
4. **`||` 会不会让人「删掉」riskReviews 里的 high？** 经记录类不能：四键闭集。绕过记录类的半份 JSON 会整键替换——不是第一次经 `AccessGrantPolicy` 的正常路径。
5. **GET 多出 `deadlineWorkingDays` 会不会让前端 TS 崩？** 类型在编译期；运行时 JSON.parse 保留额外键。页面不读该字段。保存展开带回。剥掉则 `||` 保库。不崩。
6. **前端保存是否仍抹键？** 若 POST 的 policy 不含该键，`||` 保留。GREEN 真库条即此。若展开带回则写回原值。
7. **发布变体有没有夹带 `requireProjectScopeId` / `grant_group_id` 消耗改动？** `rg requireProjectScopeId` 空；消耗仍 `id::text = :source_id`。相对 origin 仅 +2/−2。
8. **002 是否改了计算？** graft 唯一构造点第四参仍 `false`。origin 同。只改注释。
9. **注释「日常码已并入 grants」是否假？** `computeEffectivePermissions` 在 wildcard 参数为 true 时把 daily（及主授权全码）加入 `effective` 再预扣 denies。`effectivePermissions()` 对平台管理员桥传 true。真。
10. **`wildcard` 参数仍存在、字段恒 false，是否还要删字段？** B2 最小处置「不改字段与计算」。删字段是接口形状变化。不为对齐再删。
11. **主工作区 W-1 是否变成「发布口径也 403」？** 发布变体无 `requireProjectScopeId`；GREEN 含列表类测试 13+25。否。
12. **有无第 6 未知键等着再 409？** psql keys 恰好 5 个。没有。
13. **测试是不是同义反复？** 真库条读现行行、断言正数、四参保存后再读键。缺键时 `asInt()==0` 会红；origin 四字段时 Unrecognized field。能红。
14. **是否要把前端补第 5 表单项立成 001 回归？** 否。HTML 要求后台可配，未要求本模块的治理页在本轮画出该字段。M038 PENDING。
15. **`updateGrantPolicy` 返回请求对象不算回显合并结果，要不要入队？** 第一次正常前端路径用 GET 或展开带回。库正确。不入队。
16. **W-1…W-4 有没有新证据证明必须在本模块入队？** 无。发布口径可独立验证 001/002；那四项仍只在主工作区未发布版成立。

---

## 8. ponytail-review / codebase-design

- 001：组件级 FQCN `@JsonInclude` 省 import；4 参转发为不改 M037 调用点；真库一条同时钉读与不抹键。不另抽 PolicyMerger。
- 002：只改注释。
- 记录类多为仓储投影，浅缝、深适配器在 Jdbc。`Lean already. Ship.`
- 可删项未发现须在本模块入队的死代码。`package-info` 一行保留。

net: -0 本轮（B5 不修）。

---

## 9. 问题清单与分类

本席 **零条** 新的须入队 finding。不把全部否当已全对：已用全文 + 发布口径实跑 + psql 再找过。

维持 B2 定稿（无新证据不改判）：

- M032-001、002：已修，队列否。本席复核成立。
- W-1…W-4：只在主工作区未发布版成立，登记到 2026-09-13 未发布工作流，不入本模块队列、不随本模块发布。该工作流发布前必须消掉平台管理员列表 403（W-1）及 Quietly 抛错（W-2）等。
- 只登记观察（两版或注明版本）：`withLifecycle` 恒 inactive；`findTemporaryGrant` 非 UUID 抛 IAE；origin `currentSupervisorLevel` 不封顶（封顶在未发布）；`listUserIdsAssignedToPosition` Jdbc 未覆盖；`copyTemplate` 自调用无事务；`knownPermission` 吞异常；凭证 upsert 清失败次数；`partner_org_id` / `archived_by` 未映射；无咨询锁；`listTemplates(org)` 不含平台模板而 `listRoles(org)` 含全局角色；读有效权限时顺带清理到期行；`createTemporaryGrant` 逐码插入；`pageGrantRequests` null 仍全量；`currentMerchantScope` 未发布版对无商户管理员抛错；写接口返回请求对象而非再读库（观察，不升队）。

添加项：0。HTML 修改申请：0。questions：0。

若将来要立 finding，本席预备格式（本次不启用）：无。

---

## 10. 票

- **覆盖：** 25/25 全文；行数实算 2499；SHA 实算见 §1。
- **52 项：** 全部适用或无本面，逐项有证据；不新增判据。
- **三线：** 第一性原理三问 §2；顺从式 §6；对抗式 §7。
- **001/002：** 修复落地，无回归。
- **W-1…W-4 与观察：** 维持只登记。
- **新缺陷：** 0 须入队。
- **步骤结论：PASS。**

本票不是模块收口（须双席合取 + B5 后四值全等后由席甲写 ledger PASS）。本席未读本次 self-raw。
