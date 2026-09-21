CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B1 席乙（Grok）修复前全量审计

- 报告版本：V0.01（本路径原先不存在，本次创建）
- 席位：乙 / Grok 4.6 xhigh / 独立发现，未读 `modules/M032-b1-self-raw.md`
- 提示词：160-audit-prompt.md V0.17 阶段 B1
- 模块：`plan/modules.md` V0.04 M032「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段」；标记 NORMAL；计划有效内容 2494 行（B1 开工口径）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- 生命周期：ledger 记 PENDING→IN_PROGRESS；本席只写本 raw，不改 ledger/findings/计划
- 审计对象：主工作区**现状文件**（含未推提交与已暂存改动）；凡结论依赖未发布改动的，同时核 origin/main 并注明在哪一版成立
- 结论：**FAIL**（GR-01、GR-02、GR-03 工程缺陷成立）
- 时间：2026-09-21T05:53:50Z / 宿主 y9000p-ai
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 未 fetch/commit/push/merge/deploy；未改源码/测试/三 HTML/锁/findings/ledger；未用 MCP/子代理/第三席；不对库执行写语句（跑既有测试类除外）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B1 禁止修复，故 TDD 落测试、ponytail 修复、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被 ledger D-028 与本轮用户指令覆盖：后端测试在基于 origin/main 的一次性 worktree 里拷入本模块现行文件后跑；核完已 `git worktree remove --force`。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由识别为质量审查；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。用户钉死 25 文件且要求全文读不抽样；`graft skeleton` 仅作目录，覆盖以 Read 全文为准 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。Maven / psql 均前台完成，未挂 monitor、未派 loop 子代理 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| grill-me | 同目录 `grill-me/SKILL.md`（disable-model-invocation；正文只写 Run a /grilling session） | 已读头；并入 grilling，不另开会话 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | Jdbc 深适配器、AuthorizationService 深计算、记录类浅 DTO、AccessRepository 为已闭环缝 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B1 禁止修复、不加诊断桩。本轮反馈环 = worktree M006JdbcRegressionTests + 主工作区 AuthorizationServiceTests + psql SELECT |
| tdd / test-driven-development | 已读 | B1 不写测试 |
| verification-before-completion | 已读 | FAIL 前当场：独立 sha256、psql SELECT、worktree 12 测 1 ERROR、主工作区 AuthorizationServiceTests 24/24 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收 |
| requesting-code-review | 存在，B4 用 | B1 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B1 不修；review 口径扫可删项，见观察 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B1 不适用 |
| i-have-adhd | marketplace / cache `skills/i-have-adhd/SKILL.md` | 存在；本席正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | Trail of Bits plugin | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 本机无独立 SKILL.md | **缺口**；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-028 允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove |

一次性 worktree：`git worktree add --detach /home/james/wt-m032-b1-grok-ro origin/main`（HEAD `2c178f7a`）。先拷入主工作区现行 25 个实现文件：编译失败——`AuthorizationService.java:141` `SessionIssuer.MASTER_ACCOUNT_TYPE` 在 origin/main 的 SessionIssuer 中不存在（属未发布跨模块工作流，SessionIssuer 非本模块文件）。随后只保留现行 `JdbcAccessRepository.java`、把 `AuthorizationService.java` 恢复为 origin/main，再跑 `M006JdbcRegressionTests`：**12 测、0 失败、1 ERROR**（见 GR-01 旁证与观察 O-1）。`git worktree remove --force`；`test ! -d /home/james/wt-m032-b1-grok-ro` 成立。主工作区两份 SHA 核后未变。

主工作区（含未发布 SessionIssuer）跑 `AuthorizationServiceTests`：**24/24 全绿**（exit 0）。未 `source /etc/complex/complex.env`；只导出三个 `COMPLEX_DATABASE_*`（URL 库名改为 `complex_test`）。psql 只对 `complex_test` 做 SELECT。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（会话默认 openjdk 树无 javac，属环境误指不是业务缺陷）。

---

## 1. 环境事实（本席实算）

| 项 | 实算 |
|---|---|
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `2c178f7a47c5f744cf769e2db5f25cddccb19a27`（M031 检查点 PR #472 已合并） |
| criteria-lock.md | V0.11；`sha256sum` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶行同值，本席独立实算，未抄期待值） |
| LOCK-AMEND-COUNT | ledger 顶行 1（只读） |
| 三 HTML SHA256 | enum `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；治理地图 `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 映射 52 项 | `M032 → [BEQR-157, BEQR-182…187, BSM-001, BSM-003, BSM-009, BSM-028, BSM-029, BSM-073, BSM-084…088, BSM-114, BSM-115, BSM-120, GEN-001…018, PGM-001, PGM-008, PGM-022, PGM-024, PGM-026, PGM-032…036, PGM-048, PGM-059, PGM-066]` |
| 未读 | `M032-b1-self-raw.md` |

相对 origin/main 的本模块差：仅 2 文件、`33 insertions, 39 deletions`。未推提交：

- `52074492` fix(access): 平台管理员不再整类关掉商户项目隔离
- `e9b88faf` fix(access): 非密钥主体审批层级不超过上两级
- `49aaaa94` fix(access): 单次临时授权按整组一次消耗

`AuthorizationService.java` 另有已暂存（相对 HEAD 再 +3/−2）：`isMasterAuthority` 改为 `SessionIssuer.MASTER_ACCOUNT_TYPE`，不再要求 `platform_admin` 桥。`JdbcAccessRepository.java` 相对 HEAD 无未暂存差（改动已在未推提交里）。

origin/main `SessionIssuer` **没有** `MASTER_ACCOUNT_TYPE`（注释仍写破窗签发 `platform_admin + active`）。因此现行 AuthorizationService 无法单独随本模块检查点并入 origin/main。

带入 XM-M031-A（→本模块）：已闭环 AccessService 对平台管理员列模板/角色/岗位映射传 `organizationId=null`。可读 `modules/M031-access-backend-seg1.md`。XM-M031-B 转 M092；M031-001 恢复路径依赖本模块 `PositionRoleTemplate.withLifecycle` 与 Jdbc 生命周期 SQL 恒写 `inactive`。

---

## 2. 全文覆盖（25/25，不抽样）

命令：`wc -l` + `sha256sum` + `git hash-object` + `git rev-parse origin/main:<path>`。合计 **2488** 行（计划 2494 为切分时口径）。

| 路径 | 物理行 | 文件版本 | SHA256 | 工作区 blob | origin/main blob |
|---|---:|---|---|---|---|
| `JdbcAccessRepository.java` | 1485 | V0.12 | `ecc78c0b65ecf3cf4154947c6160622b39eb881404bbb3501788839a74c9d86b` | `680fda305730eae25b21359fab0c43feb1f45c14` | `976a02abe85920b8983ce18f702c5093dc6fd27e` |
| `AuthorizationService.java` | 537 | V0.13 | `990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062` | `4bfae6d2ddded7e2810666bac72bfce818ff9e22` | `6907271481fd49d94b07d39c49b019b7e2088423` |
| `JdbcAccessCredentialProvisioner.java` | 69 | V0.01 | `06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e` | `11f7240b424e7bc99bba15998d3961034c89f363` | 同左 |
| `AccessGrantPolicy.java` | 44 | V0.01 | `6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7` | `6adf014460a30672f50301a334c79003dfa000ec` | 同左 |
| `PermissionTemplate.java` | 37 | — | `84749f393404f07ba97d8cde6686496532d6f9834b86c86273ab51cc167504ee` | `92cb51df142ef19c16616c3bf95c65fce7689a23` | 同左 |
| `AccessUser.java` | 37 | V0.01 | `717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1` | `4ae0bafd545dcf061059c3b0479339a2485ac7e0` | 同左 |
| `AccessGrantRequestRecord.java` | 35 | — | `b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89` | `a66e5ded56983647c22a504fc3017cbf5783b115` | 同左 |
| `PositionRoleTemplate.java` | 34 | V0.01 | `1d09de44f1d488d0dbef17a4c62dd42cb411e61f82ec1ec61ac63c8238442b2b` | `342040efb348cd33acd32a59edcf7406c77a564f` | 同左 |
| `EffectivePermissions.java` | 23 | — | `68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e` | `382629b951fb78ef3a88d7aa97a4ec40874ede08` | 同左 |
| `StaffLevelAssignment.java` | 20 | — | `602c779aa27cc96d3be7794df7f772a353a8eca8b4a9d37445e66dfbb6b0af29` | `090b814763c186ac65084e60483c7c8a91e4dd85` | 同左 |
| `PermissionTemplateApplication.java` | 20 | — | `660d7a9be8d5fbc06c4872278b1ce7d5d5bf8d856eea130714f1c590ae254a97` | `ccd19993f160251501639e2d0904e2afba70a3c0` | 同左 |
| `StaffLevelRecord.java` | 19 | — | `ed83f2f33532f072b88c2a271b6f39d7fdd20fbfe1ce0078634aaef66775c474` | `a8dce01b466fe164639ffab671a2b9af5e2f4efe` | 同左 |
| `RoleRecord.java` | 17 | — | `f0d867f56c06bbd8cfb886b6a713cbc7a85fb55e62109b28c6004e1f7ceae77b` | `19f7985153c707f44f7d9a7c184b5b32850baa2d` | 同左 |
| `UserPositionAssignment.java` | 14 | — | `015ec50b24fe7458a52131f7fe2e7446aab165000f5087d3f57cafc115ca3ff1` | `0dcb18635b6aeb161d021117cda27c7e812a9096` | 同左 |
| `AccessCredentialProvisioner.java` | 12 | — | `c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b` | `d4859ba531f98c96a364ad446e1aa3c79bb0032c` | 同左 |
| `AccessActor.java` | 11 | — | `5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a` | `8dd295ddcc4ba9d6aa6ae54ad66f142faecedafb` | 同左 |
| `PermissionDefinition.java` | 11 | — | `1d0ee72f430acf5a5dd8ff7f46af8abdcd630e84e9af83740ffe88c37aa792b4` | `769bcf1fbaeebe77f07a2a336a7cd6cd00989983` | 同左 |
| `PermissionTemplateItem.java` | 10 | — | `6f87855d883c18299f42e3703309c51cb77a1669587b6cc784a2853056c61b4f` | `52d2f5fa63a430f854aaa52d4d9049a945092ad1` | 同左 |
| `AccessGrantPositionSummary.java` | 10 | — | `08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df` | `2255bb2d17f4e1a77699f6441de70516fb63ef32` | 同左 |
| `UserPositionAssignmentResult.java` | 10 | — | `437d1c416d5027911eff9d34c421d93202dbe523f9658653406acdec9524775c` | `1a81a0ac58d12128ff1decc5b6f4331e0a139d15` | 同左 |
| `AccessGrantSummary.java` | 9 | — | `7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96` | `4ce965dfaf8886e8935aaec2ea7483a07eb7f02f` | 同左 |
| `PermissionTemplateApplyResult.java` | 8 | — | `80134f3a3e8157cbf3c8af227a1f4bea9c4aa6c8986466fc31f70dd9d8a2d076` | `a3b50e078abb79d51153e25bd3c5c95bb9e35a09` | 同左 |
| `AccessGrantUserSummary.java` | 8 | — | `80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e` | `26deb3fddad464c253bea791d1b298963c786a29` | 同左 |
| `RoleAssignmentRecord.java` | 7 | — | `2034337e12c8d9b38bb93205058b5844047148c5965932a259e9e17375f1ad0e` | `fff4cbb072c71d366a9595aedf6bf80eb2a57512` | 同左 |
| `package-info.java` | 1 | — | `bf3835ba131ca4ad8c2e1ab5311cab18d2f9fe5755ecee9a311b47095af9efe8` | `7a7a0c8667b33cc78b86ad6685438ba09cf54b19` | 同左 |
| **合计** | **2488** | | | | |

只读追证（非本模块，不扩大范围）：`AccessRepository.java` V0.09、`AccessService.java` V0.21（已闭环 M031）、`SessionIssuer.java`、`TemporaryGrant`（别模块记录类）。

---

## 3. JdbcAccessRepository：每条 SQL 对库（complex_test SELECT）

psql：`current_database()=complex_test`。18 张相关表均存在。`staff_levels` **30 行**，M1–M10 × lower/middle/upper，`level_rank` 1–30，与 BEQR 职级三十档及 `StaffLevelRecord` 注释一致。

### 3.1 读路径范围谓词

| 方法 | 范围谓词 | fail-closed？ | 备注 |
|---|---|---|---|
| `listTemplates()` 无参 | 无 | 否（全量） | 仍存在；AccessService 平台管理员**不走**此入口 |
| `listTemplates(org)` | `owner_scope_id = :organization_id` 且 **先** `requireProjectScopeId` | 空/空白 → 403（现状）；origin/main `null` 退回无参全量 | **GR-01**。谓词是等号，**不含** `owner_scope_id IS NULL` 的平台模板 |
| `listUsers()` 无参 | 无 | 否 | AccessService 平台管理员走这里，不踩 GR-01 |
| `listUsers(org)` | `organization_id = :organization_id` + require | 同 GR-01 口径 | AccessService 非管理员走 |
| `listUsersByMerchant` | `merchant_id = :merchant_id` + `requireMerchantScopeId` | 空 → 403；origin/main `null` 退回全量 | |
| `listRoles(org)` | `organization_id IS NULL OR = :org` + require | GR-01 | 含全局角色 |
| `listPositionRoleTemplates(org)` | 同角色（含 org NULL）+ require | GR-01 | origin/main `null` 另走无 WHERE 全表 |
| `pageGrantRequests` | `(:organization_id IS NULL OR (target.org=… AND (request.project_id IS NULL OR =)))`；商户同 | **`null` 仍跳过滤** | AccessService 对平台管理员传 null（L1039–1042）。未发布「不再整类关掉隔离」**没改这条** |
| `listGrantRequests` | 仅时间 cutoff，无 org/merchant | 否 | |
| `findTemplateById` / `findUserById` / `findRoleById` | 仅 id | 范围闸在 Service | |
| `findPositionRoleTemplate` | active 且未归档删除；org 精确或全局；`ORDER BY` 本组织优先；`LIMIT 1 FOR SHARE` | 行锁 FOR SHARE | |
| `findPositionRoleTemplateById` | id + **FOR UPDATE** | 写前锁 | |
| `findGrantRequestById` | id + **FOR UPDATE** | 写前锁 | |
| `lockGrantAuthorizationFacts` | `LOCK TABLE … IN SHARE MODE` | 表级共享锁 | 无 `pg_advisory_lock`（咨询锁函数库内存在但本模块不用） |
| `findPermissionEffects` | 五段：user_roles、岗位派生 assigned_role、模板应用、临时授权（`grant_group_id::text` 作 source_id）、user_permission_overrides | 读前 `expireLapsedEffects` **写**到期行并记 usage_logs | HTML「到期…留痕」；GET 有效权限会写库 |
| `listPermissions` / `listStaffLevels` | 无租户谓词 | 目录/职级定义全局 | 职级 30 档与库一致 |

### 3.2 写路径原子 / 单次消耗 / 到期

- `createTemplate` / `saveTemplate` / `createRole`：方法级 `@Transactional`；`copyTemplate` **无**自身事务、内部调 `createTemplate`（Spring 自调用不走代理）。生产调用面是 AccessService `executeTemplateWrite`（M031，外层幂等/事务），本模块单独被直接调时才裂。观察，不立项。
- `savePositionRoleTemplate`：`ON CONFLICT (id) DO UPDATE` 写 status/role/template/org，**不碰** `archived_at`/`deleted_at`/`delete_review_id`。与 M031-001「先 lifecycle 再 upsert 写回 active」相容。
- `savePositionTemplateLifecycle` L519–533：SQL **恒** `status='inactive'`，无视 mapping.status()。`PositionRoleTemplate.withLifecycle` L30–33 同样硬编码 `"inactive"`。这是生命周期方法的适配器语义，不是恢复口；恢复由已闭环 AccessService 两步完成。观察 O-2。
- `createTemporaryGrant`：`grant_group_id` 用 grant.id；首行 `id=UUID.fromString(grant.id)`，其余行新 UUID。AccessService L1573 `UUID.randomUUID().toString()`，可解析。库列 `temporary_permission_grants.grant_group_id varchar NOT NULL`（非 uuid），与 `.addValue("grant_group_id", grantGroupId)` 一致。
- `revokeTemporaryGrant`：按 `grant_group_id` 把整组 `active` 标 `revoked`。
- `markSingleUsePermissionConsumed` L1011–1062：模板应用按 `application.id`；临时授权按 **`grant_group_id = :source_id`，不再 AND permission_code**（相对 origin/main 的 49aaaa94）。`consumed=FALSE` 作原子闸。消耗后写 usage_logs。`findPermissionEffects` 的 TEMPORARY_GRANT `source_id` 已改为 `grant_group_id::text`，与整组消耗对齐。
- `expireLapsedEffects`：UPDATE…RETURNING 后逐条 logExpiry；只扫本人、只命中仍为 active 的到期行，重复读幂等。
- `replacePositionGrants`：先撤旧 position_application_ids，再写 assigned_role_id / assigned_position_template_id / position_application_ids；`changed!=1` 抛中文 IllegalStateException。
- `saveUserPositionAssignment`：ON CONFLICT(user_id) **不更新** assigned_role_id 三列（那些只由 replacePositionGrants 写）。库有这三列及 FK。

### 3.3 CHECK / 唯一 / FK 与代码闭集

| 对象 | 库约束 | 代码 |
|---|---|---|
| staff_levels | main M1–M10；sub upper/middle/lower；rank 1–30；UNIQUE(main,sub)、(rank) | 只读；不发明档位 |
| position_role_templates.status | CHECK `active\|inactive` | lifecycle 写 inactive；upsert 可写 active |
| access_grant_requests.status | pending\|applied\|rejected | saveGrantRequest 非 pending 走 UPDATE…AND status='pending' |
| access_grant_requests.request_type | 六值含 `role_permission` | 本模块不归一化类型（Service 更严，M031） |
| permission_template_items.effect / user_permission_overrides.effect | grant\|deny | `effectCode` lower-case |
| permissions.risk_level | low\|medium\|high\|critical | AccessGrantPolicy.riskReviews 四键闭集 |
| auth_credentials.status | active\|disabled\|locked | Provisioner 只写 active/disabled |
| staff_level_assignments.salary_currency | CNY；金额 ≥0 或 NULL | 记录类不强制；插入原样 |
| supervisor_review_policies.approval_level | 含 **specific_role** | AccessGrantPolicy.ReviewRule **不含** specific_role（只 three） |
| temporary_permission_grants.status | **无 CHECK** | 代码写 active/revoked/expired |
| uq_position_role_templates_active_scope | 仅 status=active 的 (coalesce(org,''), position, level) | 启用中同岗同级唯一 |
| access_users.partner_org_id | 列存在 | AccessUser **无此字段**（别模块列） |

`permission_templates` 种子 5 行：`owner_scope_type=platform`，`owner_scope_id` 空。`listTemplates(org)` 等号谓词**看不见**这些全局模板。

### 3.4 未发布 Jdbc 差（相对 origin/main）

origin/main：`listTemplates/listUsers/listRoles/listUsersByMerchant` 对 `null` **退回无参全量**；`listPositionRoleTemplates(null)` 无 WHERE。`findPermissionEffects` TEMPORARY source_id 为 `id::text`；`markSingleUse` TEMPORARY 按 `id::text` **且** `permission_code`。

现状：上述列表 `require*ScopeId`；消耗按 `grant_group_id` 整组、去掉 permission_code 谓词。

---

## 4. AuthorizationService 最终权限链（全文 L1–L537）

计算（`permissionGrant` / `computeEffectivePermissions`）：

1. 账号非 active 或权限码未知 → deny。`knownPermission` 捕获仓储异常当「不存在」→ 403（失败时偏拒绝，不偏放行）。
2. **DENY 优先**（匹配码任一 DENY 即拒）。与治理地图「显式拒绝优先级最高」、枚举「最终权限计算」一致。
3. 最高权限（`isMasterAuthority`）/ 平台管理员日常码 / 客人自助白名单 → 返回 null（不消费单次）。
4. GRANT 须 `isExplicitOrDailyGrant`：ROLE 来源若为 `role_platform_admin`，高危条目不自动算显式授予，**治理本职码集合除外**（L124–128）。
5. 通配：`computeEffectivePermissions` 在 wildcard=true 时把字典日常码（master 则全部 active 码）并入 grants，再减 denies。
6. 时间窗 / 单次：`isActive` 看 consumed、validFrom、validUntil。`requirePermissions` 按 `source:sourceId` 去重后 `markSingleUsePermissionConsumed`；失败 deny。self 端 `effectivePermissions()` **不消费**（L146–148）。
7. `effectivePermissionsForUser`：非 master 须同组织、若有商户则同商户。现状用 `isMasterAuthority` 跳过闸；origin/main 用 `isPlatformAdmin`。平台管理员查他组织用户：现状 403，origin/main 放行。

最高权限识别（已暂存 V0.13）：`active` + `MASTER_ACCOUNT_TYPE` + 固定 id/username。不再要求 platform_admin 桥。与 BSM-007「平台管理员与最高权限是两个不同的角色」一致。**仅现状成立**；origin/main `isMasterAuthority` 仍 `isPlatformAdminBridge && MASTER_USER_ID && MASTER_USERNAME`。主工作区 AuthorizationServiceTests 24/24 钉住现行口径。origin/main 无 `MASTER_ACCOUNT_TYPE`，现行文件无法在纯 origin/main 树上编译。

审批层级 `currentSupervisorLevel`：master→3；无主管审批角色权限→0；否则 `(rank+2)/3` 再 **`Math.min(derived, 2)`**。职级只到上两级，第 3 级只给密钥主体。origin/main 无封顶，M10 upper（rank 30）会得到 10。**封顶仅现状/e9b88faf 成立。** HTML「上一级、上两级或最高权限」与「最高权限是持 U 盘密钥的那一个人」支持封顶。

scope 方法：

| 方法 | 现状 | origin/main | 抛？ |
|---|---|---|---|
| `requireProjectScope` | 无 org → PROJECT_SCOPE_REQUIRED | 同 | 抛 |
| `currentMerchantScope` | 有商户号返回 of；**无则抛** MERCHANT_SCOPE_REQUIRED。注释引红线④ | 平台管理员桥 **empty=全量**；其余无商户才抛 | 现状对无商户的管理员也抛 |
| `currentMerchantIdQuietly` | 有商户号 of，无则 empty，不因管理员改成全量 | 管理员桥先 empty | **不抛** |
| `currentProjectScopeQuietly` | `Optional.of(requireProjectScope(actor))` → **无项目即抛** | 管理员桥 empty=全量；有 org 返回 of；**永不抛**（旧注释） | **现状抛，与方法名 Quietly 及调用面 `.orElse(null)` 相反 → GR-02** |
| `requirePlatformScope` / `requirePlatformComplianceScope` | 管理员桥放行；外包/带 merchantId 拒 | 同结构 | 抛 |

`effectivePermissions` 构造 `new EffectivePermissions(..., false, checkedAt)`：**wildcard 字段恒 false**（L206），与同类 javadoc「wildcard=true 仅当平台管理员桥」不一致。实际日常码已并进 grants。前端 `hasPermissions` 认 wildcard 旗标；生产靠 grants。观察 O-3，不立项（首次打开菜单仍可凭 grants 工作）。

主工作区 `AuthorizationServiceTests` 24/24 绿。origin/main 该测试类仍有 `currentMerchantScopeReturnsEmptyForPlatformAdmin`，与现状实现相反（测试文件非本模块）。

---

## 5. JdbcAccessCredentialProvisioner / AccessGrantPolicy / 记录类

**口令**（L18–64）：`@Profile("prod")`；`BCryptPasswordEncoder(10, new SecureRandom())`；空白账号/口令 400；状态闭集 active/invited/disabled，非 active 则凭证 disabled；`ON CONFLICT(user_id)` 重写哈希并 **清零失败次数、解锁**。`auth_credentials` CHECK 与列对齐。接口 `noop()` 给非 prod。无明文落库。符合 GEN-010。观察：重发初始口令会解锁，属运维口。

**AccessGrantPolicy**：构造要求 window/position/user 三阈值 + riskReviews 恰好四键 low/medium/high/critical；BoundedInt `min≥1` 且 value 在 [min,max]；ReviewRule 仅 `one_level_up|two_levels_up|top_authority`，top 必须 requiredLevel=3。

complex_test 现存 `access_grant` 政策 JSON **另有** `deadlineWorkingDays: {value:3, min:1, max:30}`（V389 写入；`SupervisorReviewService` 按此算审批时限）。记录类 **没有**该字段。`AccessService.updateGrantPolicy`（M031）`writeValueAsString(request.policy())` 再 `saveGrantPolicy` **整份覆盖** jsonb → 首次正常保存授权政策会丢掉时限键。见 **GR-03**。origin/main 此记录类与 Jdbc `saveGrantPolicy` **字节相同**，该缺陷在两版都成立。

**记录类 ↔ 列 / JSON**：

- AccessUser：phoneHash `@JsonIgnore`；createdVia 默认 `"legacy"`。库多 `partner_org_id`（本记录不映射）。
- RoleRecord.organizationId `@JsonIgnore`（API 不回组织，仓储仍用）。
- PermissionTemplate.validUntil ← 列 `valid_to`（mapTemplate L1185）。
- StaffLevelAssignment 金额分、CNY CHECK。
- PositionRoleTemplate 含 archived/deleted/review/reason；SQL SELECT 未读 archived_by/deleted_by（有列）。
- UserPositionAssignment 记录无 assigned_role_id 三列（由 replacePositionGrants 写库）。
- AccessGrantRequestRecord.permissionCodes 用逗号文本编解码，对库 `permission_codes text`（另有遗留列 `permission_code` 单数，本模块不写）。
- EffectivePermissions.wildcard 见上。
- package-info.java 仅 `package com.complex.platform.access;`。

报错给调用方的 `ApiException` 消息为中文；code 为内部标识。未发现把 SQL/栈回显给用户。

---

## 6. 顺从式：52 项逐项（定义全文已读 + 相关 HTML 原文）

相关 HTML 已通读：治理地图 `#permission` 权限规则/职级/外包审计/桌台挂单经销（含最终权限公式、角色装配、高危五类、红线④在 PGM 审批段）、`#staff` 员工和手机、破窗 `#emergency`、七类红线（治理地图约 L528 与 BSM-028 同文）；业务地图 BSM-003/007 通用能力与平台管理员日常权、BSM-028 基础管理主要功能（含七类红线与「管理员工、岗位、职级、权限」）；枚举九「职级」「最终权限计算」「项目与敏感数据权限」「员工状态」（后三行的稳定 ID BEQR-149/151/152 **不在本模块映射**，作相关原文，不另发明判据）。

| ID | 适用？ | 理由（本面） |
|---|---|---|
| BEQR-157 员工状态 | 部分 | 本模块 `access_users.status` 为账号状态（active 等），不是待入职/在职/试岗/停岗/离职。账号闸用 `"active".equals(actor.status())`。员工档案状态在别模块。无冲突。 |
| BEQR-182…187 考勤/请假/薪资/提成 | **无本面** | 无打卡、请假单、薪资单、提成发放实现。StaffLevelRecord 只提供职级与薪资大范围摘要；StaffLevelAssignment 只存具体薪资分。 |
| BSM-001 整体说明 | 适用 | 「谁能操作、谁负责」由权限点/职级/最终计算承载。 |
| BSM-003 通用能力·权限分岗 | 适用 | 单一功能点、角色/岗位模板、员工覆盖、存档删除审批的仓储面（岗位模板禁止物理删）。 |
| BSM-009 基础管理入口 | 适用 | 「员工、设备、权限」中的权限/职级后端。 |
| BSM-028 主要功能 | 适用 | 「管理员工、岗位、班次、职级、权限」；同段七类红线④（数据隔离）约束 scope 方法。班次无本面。 |
| BSM-029 和谁有关 | 适用 | 「用户权限要知道员工注册信息、职级和审批状态」——账号/职级/权限效应。 |
| BSM-073 商户专区主要功能 | 部分 | 商户账号 `listUsersByMerchant`、scope 拒跨商户；专区账单/工单无本面。 |
| BSM-084…088 员工手机端 | **无本面** | 排班考勤请假薪资自助、代订、提成发放不在这 25 文件。职级记录仅为薪资大范围。 |
| BSM-114 各类入口 | 适用 | 按身份看自己的：effectivePermissions、商户/项目 scope、外包 enforceOutsourcedAssigneeScope。 |
| BSM-115 相互关系 | 适用 | 「基础管理连所有业务」的权限闸。 |
| BSM-120 已确认规则 | 部分 | 「权限点首批预置」在字典表；本模块读 permissions。购票/通票等无本面。 |
| GEN-001 | 适用 | 见第一性三问。 |
| GEN-002 | 适用 | 声明与行为：GR-02 Quietly 名不副实。AuthorizationServiceTests 现状 24/24。 |
| GEN-003 | 适用 | 主工作区本类测试绿；Jdbc 整组消耗的回归夹具未跟上（O-1，测试文件非本模块）。 |
| GEN-004 | 适用 | 空范围拒绝、政策 BoundedInt、凭证状态闭集。 |
| GEN-005 | 适用 | DENY 优先、默认拒绝、master 与平台管理员分离。GR-01 把管理员配置列表误关。 |
| GEN-006 | 适用 | 事务、ON CONFLICT、消耗原子闸。GR-03 整份覆盖 jsonb 丢键。 |
| GEN-007 | 适用 | 消耗 `consumed=FALSE`；到期 UPDATE 幂等；表锁 SHARE MODE。 |
| GEN-008 | 适用 | 列/JSON/接口契约。GR-01 违约 AccessRepository；GR-02 方法名；GR-03 政策形状。 |
| GEN-009 | 适用 | 未知权限当拒绝；knownPermission 吞仓储异常（偏关闭，观察）。 |
| GEN-010 | 适用 | BCrypt；phoneHash JsonIgnore；口令不回显。 |
| GEN-011 | 适用 | 到期/单次消耗写 usage_logs；政策变更留痕在 Service。 |
| GEN-012 | 适用 | `listUserIdsAssignedToPosition` Jdbc **未覆盖**，默认 N+1（M031 已登记观察）。角色列表已 batch attachPermissionCodes。 |
| GEN-013 | 适用 | `@Profile("prod")` 凭证器。 |
| GEN-014 | **无本面** | 本模块无迁移文件（M034+）。只对照已应用库结构。 |
| GEN-015 | **无本面** | 无 UI。ApiException 中文。 |
| GEN-016 | 适用 | EffectivePermissions javadoc vs wildcard 恒 false（O-3）。 |
| GEN-017 | 适用 | 本报告四分类。 |
| GEN-018 | 适用 | 依据：HTML / 测试输出 / psql / git diff。 |
| PGM-001 整体定位 | 适用 | 资格（员工是否有权限）+ 责任链审批层级。界面全中文：对外消息中文。 |
| PGM-008 员工和手机卡片 | 部分 | 卡片「岗位状态」；注册/设备绑定无本面。 |
| PGM-022 / PGM-024 空间点位 | **无本面** | 无楼层/点位。员工班次责任区域不在此。 |
| PGM-026 商户主体主要功能 | 适用 | 同文件段含审批链自动补全、七类红线、审批时限。本模块：scope 红线④、政策 requiredLevel、GR-03 时限键。 |
| PGM-032…035 员工和手机正文 | **无本面**（032/033/034）；035 提成发放 **无本面** | 注册双路径、设备标识、企微会话不在这 25 文件。 |
| PGM-036 设备管理 | **无本面** | |
| PGM-048 商户侧 | 适用 | `listUsersByMerchant`、currentMerchantScope；商户看不到平台全局（requirePlatformScope）。 |
| PGM-059 个人信息保护 | 部分 | phoneHash 忽略序列化；查询客人通道/界面脱敏无本面。 |
| PGM-066 已拍板 | 适用 | 「员工权限按单一功能最小颗粒度、支持角色和员工级完全自定义」由权限点+覆盖+模板实现。 |

未映射但已读的相关原文：BEQR-149 职级三十档（库+记录已对齐）；BEQR-151 最终权限公式（计算链对齐）；BEQR-152 项目隔离（scope 方法）；治理地图权限与职级全文、破窗（isMasterAuthority 识别，破窗登录在 SessionIssuer 非本模块）。

---

## 7. 第一性原理三问

**用途（删掉它哪条 HTML 做不到）**

- JdbcAccessRepository：没有它，权限模板/角色/岗位映射/临时授权/覆盖无法持久化，BSM-028「管理…权限」、治理地图最终权限公式与角色装配全部落空。
- AuthorizationService：没有它，每次操作的实时校验、DENY 优先、日常/高危分界、审批层级、商户/项目 scope 做不到（BSM-007、治理地图权限规则、红线④）。
- JdbcAccessCredentialProvisioner：没有它，prod 新建账号没有登录哈希，BSM「新建员工账号」无法落地。
- AccessGrantPolicy：没有它，授权政策上下限与四级风险审批规则不能在写入前挡住非法 JSON。
- 记录类：仓储与 API 的形状；删掉则编译失败，属细化不是业务发明。

**声称的行为是否被实际执行验证**

- 职级 30 档：psql 30 行。类型 4。
- 最终权限 DENY 优先 / 平台管理员日常码：AuthorizationServiceTests 24/24（主工作区，类型 2）。
- 单次整组消耗：生产 SQL 与列存在（psql）；Jdbc 回归夹具 TEMP 表无 `grant_group_id`，`temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` **ERROR**（worktree，类型 2）。生产路径未在该夹具上绿。
- 平台管理员列模板：无本模块集成测试覆盖 Jdbc `listTemplates(null)`；由 AccessService L108–109 + Jdbc L73–75 静态组成。类型 4 命令链，缺类型 2 单测（内存仓走接口默认 null=全量，**测不到** Jdbc 覆盖）。
- AccessGrantPolicy 丢 `deadlineWorkingDays`：psql 见键 + 记录类四字段 + Service 整份覆盖。类型 4。未跑 updateGrantPolicy 真写（禁写库）。

**未经验证就当真的前提**

- 「红线④ = 列表接口对平台管理员传 null 必须 403」——HTML 原文是看不到别的商户/项目**数据**，且最高权限可逐笔开放；管理员配置模板是另一句明文。Jdbc 未发布改动把两句收成一句。
- 「Quietly 永不抛」——origin/main 注释与调用面；现状已抛。
- 「授权政策 JSON 只有记录类四个字段」——库内多 deadlineWorkingDays。
- 「M006JdbcRegressionTests TEMP 表等于生产表」——缺 grant_group_id。
- 「wildcard 旗标会告诉前端这是平台管理员」——代码恒 false。

---

## 8. 对抗式（grilling 结构，自问自答）

❓ **Q1** 平台管理员打开权限模板页会怎样？  
➡️ AccessService `isPlatformAdmin ? null : requireProjectScope` 再 `listTemplates(orgId)`。现状 Jdbc `requireProjectScopeId(null)` → 403 PROJECT_SCOPE_REQUIRED。origin/main 退回全量。种子管理员 `account_type=platform_admin` 且 `organization_id=platform-company`，Service **仍传 null**，有项目号也没用。首次正常配置模板失败。**GR-01。**

❓ **Q2** 这是不是红线④要求的？  
➡️ 红线④禁止的是看**别的商户/项目的数据**；「平台管理员配置各岗位权限模板」是治理地图角色装配原文。把配置列表入口整类 403，不是逐笔开放，也不是「有项目号就按项目过滤」（Service 根本不传他的 org）。同文件 `listUsers()` 无参仍给管理员全量，口径自相矛盾。

❓ **Q3** 单次临时授权用其中一个权限码，其余码还在吗？  
➡️ 现状 `source_id=grant_group_id`，UPDATE 不带 permission_code，整组 consumed。HTML「单次使用」按一次授权理解说得通。origin/main 按行 id+permission_code。夹具未改导致回归 ERROR，不是生产缺列。

❓ **Q4** 最高权限是不是平台管理员？  
➡️ 现状不是（MASTER_ACCOUNT_TYPE）。HTML 要求分开。origin/main 要求 platform_admin 桥，与 HTML 更远。本项现状更贴 HTML，但依赖未发布 SessionIssuer。

❓ **Q5** currentProjectScopeQuietly 对无组织主体？  
➡️ 调用 `requireProjectScope` → 抛。PartnerService/InventoryService/FloorPlanService/AccessService L1135 `.orElse(null)` 假定 empty。名称与调用面被打破。**GR-02。** origin/main 管理员 empty、有 org 返回 of、不抛。

❓ **Q6** 保存授权政策会不会改审批时限？  
➡️ 会。记录类序列化不含 deadlineWorkingDays，saveGrantPolicy 覆盖 jsonb。SupervisorReviewService 随后读空回落默认日。**GR-03。** 两版记录类相同。

❓ **Q7** 岗位模板恢复后仍 inactive？  
➡️ 本模块 lifecycle SQL/withLifecycle 恒 inactive；M031-001 已在 AccessService 用 upsert 写回 active。本席不把已闭环修复当新缺陷。Jdbc upsert 确实不碰 archived_at，与「先清生命周期再 upsert」匹配。

❓ **Q8** 读有效权限会不会悄悄改库？  
➡️ 会：expireLapsedEffects。HTML 要求到期留痕。UPDATE 只打 active 到期行。属细化，不立项。

❓ **Q9** 平台管理员列授权申请还能看全量吗？  
➡️ 能。pageGrantRequests 对 null org 跳过滤。未发布隔离改动没覆盖这条。若 GR-01 按契约恢复 null=全量，此项只是观察；若按红线④收紧列表，此处仍漏。

❓ **Q10** 有没有咨询锁？  
➡️ 没有 pg_advisory_*。有 LOCK TABLE SHARE MODE、FOR SHARE、FOR UPDATE。满足「行/表锁」问法，不是咨询锁。

---

## 9. Findings（临时 GR；正式 ID 待 B2）

### GR-01 平台管理员列权限模板/角色/岗位映射即 PROJECT_SCOPE_REQUIRED

- **文件:行** `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java:73-75`、`:307-309`、`:416-418`；方法 `:1473-1477`。追证（只读）`AccessService.java:106-109`、`:291-294`、`:411-414`；`AccessRepository.java:19-28`、`:79-88`。
- **原文**
```
    public List<PermissionTemplate> listTemplates(String organizationId) {
        requireProjectScopeId(organizationId);
```
```
    private static void requireProjectScopeId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
        }
    }
```
更长上下文：同文件仍保留无参 `listTemplates()` 全量；`listUsers()` 无参仍给管理员用。origin/main 对应行为是 `if (organizationId == null) return listTemplates();`。
- **正常触发 / 影响**：平台管理员（含种子 `p0_prod_test_admin`）打开权限模板/角色/岗位映射列表。AccessService 因 `isPlatformAdmin` **固定传 null**，Jdbc 403。治理地图「平台管理员配置各岗位权限模板」首次做不到。种子模板 `owner_scope_id` 为空，即便改成传其 `platform-company`，等号谓词也列不出平台模板。
- **最小拟处置**：Jdbc 对 `listTemplates(String)` / `listRoles(String)` / `listPositionRoleTemplates(String)` 恢复与 AccessRepository 契约一致的 `null → 无参全量`（origin/main 行为）；空白字符串仍可拒绝。不要在本模块改已闭环 AccessService。`listUsers(String)` 同口径，但管理员已走无参，非主路径。
- **四分类**：冲突 →（与「平台管理员配置各岗位权限模板」及接口「Null = platform admin (all）」明文矛盾；红线④不能解释成关掉管理员配置列表。不进入细化。）
- **工程缺陷**：成立。冻结 ID：GEN-005、GEN-008、BSM-028、PGM-001（资格）、相关 HTML 治理地图 L658 角色装配、BSM-007。
- **第一性依据**：类型 1 治理地图角色装配原文；类型 4 `git diff origin/main -- JdbcAccessRepository.java` + 只读 AccessService L108–109。类型 2：内存测试走接口默认测不到此覆盖。
- **哪一版**：仅主工作区未发布提交 `52074492` 成立；**origin/main 不复现**（XM-M031-A 转述独立核实）。

### GR-02 `currentProjectScopeQuietly` 会抛，与名称和调用面相反

- **文件:行** `AuthorizationService.java:499-507`
- **原文**
```
     * 项目维度只读 scope：返回当前主体的项目 ID。没有项目归属则拒绝。
     * 红线④：不因平台管理员身份返回空（全量）。
     */
    public Optional<String> currentProjectScopeQuietly() {
        AccessActor actor = currentActor();
        return Optional.of(requireProjectScope(actor));
    }
```
更长上下文：origin/main 同方法对平台管理员桥 `return Optional.empty()`，有 org 返回 of，注释写「永不抛」。调用面 `PartnerService` / `InventoryService` / `FloorPlanService` / `AccessService.java:1135` 使用 `.orElse(null)` 表示无过滤。
- **正常触发 / 影响**：无 `organizationId` 的已登录主体（新建平台管理员账号默认 org 可空）进入上述列表/匹配。现状 403 PROJECT_SCOPE_REQUIRED，而不是 empty。种子管理员有 `platform-company`，本条不炸；首次建一个无组织的平台管理员或员工再进伙伴/库存页即炸。
- **最小拟处置**：Quietly 保持不抛：无项目 → `Optional.empty()`；红线④的「管理员不当成全量」已由「有商户号/项目号就过滤、不因管理员身份丢掉绑定」覆盖（`currentMerchantIdQuietly` 现状即此）。不要用 require 冒充 Quietly。
- **四分类**：冲突？否（HTML 未规定方法名）。细化（红线④的实现细节）→ 技术性命名/契约。取 **细化**。
- **工程缺陷**：成立（GEN-002、GEN-008）。
- **第一性依据**：类型 4 现状/origin `git diff`；类型 1 红线④原文（约束的是数据隔离，不是「Quietly 必须抛」）。
- **哪一版**：仅现状（未发布 AuthorizationService）。origin/main 不抛。

同文件 `currentMerchantScope` 对无 merchantId 一律抛（含平台管理员与 master）。这与红线④字面更近、与「日常功能」及 InventoryService 大量 `currentMerchantScope()` 相张力；最高权限「逐笔开放」本模块未实现。**不单独立项**（避免为理论逐笔补需求）；记观察 O-4。种子管理员 `merchant_id` 空，走 `currentMerchantScope` 的接口在现状会 403。

### GR-03 `AccessGrantPolicy` 整份覆盖 jsonb 丢掉 `deadlineWorkingDays`

- **文件:行** `AccessGrantPolicy.java:9-18`；`JdbcAccessRepository.java:776-783`。追证 AccessService.java:1933–1960（M031，只读）。
- **原文**
```
public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
```
```
                UPDATE supervisor_review_policies SET parameters = CAST(:parameters AS jsonb), updated_at = now()
                 WHERE policy_code = 'access_grant' AND enabled = true
```
更长上下文：psql `parameters` 含 `deadlineWorkingDays: {value:3,min:1,max:30}`。ReviewRule 闭集无 `specific_role`（库 CHECK 有）。Jackson 读记录类默认忽略未知字段；写回只有四字段。
- **正常触发 / 影响**：后台保存授权政策（时间窗/频繁阈值/风险审批）。首次保存成功后，审批通用时限键消失，主管复核回落到代码默认日，与「全平台审批通用时限须按政策参数」漂移。
- **最小拟处置**：记录类增加 `BoundedInt deadlineWorkingDays`（与库内形状一致，构造校验 min/max/value），或 Jdbc `saveGrantPolicy` 改为 `parameters = parameters || CAST(:parameters AS jsonb)` 合并而非整份替换。前者才能让 GET 看见时限。
- **四分类**：冲突？否。细化（PGM 审批时限的下位字段）→ **细化**。
- **工程缺陷**：成立（GEN-006、GEN-008、PGM-026 审批时限）。
- **第一性依据**：类型 4 `SELECT parameters FROM supervisor_review_policies WHERE policy_code='access_grant'`；类型 1 治理地图「全平台审批通用时限…业务参数」。
- **哪一版**：**现状与 origin/main 均成立**（两文件相对 origin 无差）。

---

## 10. 观察（不入队，除非 B2 升）

| ID | 内容 |
|---|---|
| O-1 | `M006JdbcRegressionTests` TEMP `temporary_permission_grants` 无 `grant_group_id`。worktree 对现行 Jdbc：12 测 1 ERROR（`temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` L71，`column "grant_group_id" does not exist`）。生产表有该列。测试文件非本模块（M031 已指向 M037）。 |
| O-2 | `withLifecycle` + lifecycle SQL 恒 inactive；恢复靠 M031-001 两步。本模块文件行为与定稿一致。 |
| O-3 | `EffectivePermissions.wildcard` 恒 false；javadoc/前端 mock 仍讲管理员 wildcard。grants 已含日常码。 |
| O-4 | `currentMerchantScope` 对无商户的管理员/master 抛；`currentMerchantIdQuietly` 仍 empty。调用面未改齐。红线④逐笔开放未做。 |
| O-5 | `pageGrantRequests`/`listGrantRequests` 对管理员 null 仍全量，与 listTemplates 收紧不一致。 |
| O-6 | `listUserIdsAssignedToPosition` Jdbc 未覆盖（N+1）。M031 已登记。 |
| O-7 | `copyTemplate` 仓储自调用无事务；生产有 Service 外壳。 |
| O-8 | `knownPermission` 吞异常当无权限。 |
| O-9 | 凭证 ON CONFLICT 清失败次数/锁。 |
| O-10 | `access_users.partner_org_id`、grant_requests.`permission_code` 单数列、position 的 archived_by 未映射。 |
| O-11 | 无 pg_advisory_lock；有表锁/行锁。 |
| O-12 | 现行 AuthorizationService 依赖未发布 `SessionIssuer.MASTER_ACCOUNT_TYPE`，不能单独检查点并入 origin/main。 |
| O-13 | 主工作区 AccessServiceTests 6 个报错属别模块未发布改动（用户/ledger D-028）；本席未在主工作区用它当本模块证据。 |

ponytail-review（可删，不修）：`listTemplates()` 无参在管理员改走它之后才有用；`PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS` 与日常码双轨必要。`net: 0` 在缺陷修复前不删隔离闸。

---

## 11. 步骤结论

**FAIL**

- 25/25 全文覆盖；52 项逐项适用/无本面已记；三线已跑。
- 成立工程缺陷 3：GR-01（冲突，仅现状）、GR-02（细化，仅现状）、GR-03（细化，两版）。
- 添加项 0；无 HTML 修改申请；无 questions。
- 无条件通过。B2 归并后再定修复队列。检查点若只带本模块 25 文件：AuthorizationService 在 origin/main 不能编译（O-12）；Jdbc GR-01 会把已合并的 M031 AccessService 管理员列表打 403。

---

## 12. 本席未做 / 未验证

- 未跑主工作区 AccessServiceTests（已知 6 个外来报错）。
- 未对 complex_test 执行 UPDATE/INSERT（GR-03 未做真写复现，依据 SELECT+代码）。
- 未读席甲 self-raw。
- 未改任何非本 raw 文件。

## M032 B2-r1 整批候选复核（席乙票，V0.02）

- 时间：2026-09-21T06:15:02Z。席位：乙 / Grok 4.6 xhigh / 只读外审，正本 `/home/james/complex-management` main 主工作区。
- 输入：`modules/M032-b1-grok-raw.md` V0.01（46223 bytes，SHA256 `1ac439d8812b0550e153d7e3c4ca71d2715023795d88962388bfc773beefb165`，本席 B1：FAIL，GR-01、GR-02、GR-03，观察 O-1…O-13）；`modules/M032-b1-self-raw.md` V0.01 封存前缀（32223 bytes，SHA256 `88bf80c3a26e1f8b31c349c26de2ebc009468e18716c561606dfc421bc5fa011`，席甲 B1：FAIL，CX-M032-001…004，OBS-1…7）。席甲 raw 现文件更长，本票只读该 32223 字节前缀，未通读、未采信其后追加的 B2 结论段。
- 候选：`docs/audit-2026-09/findings.md` V3.00 顶部「当前 M032 B2-r1 整批候选」段（全文件 SHA256 `c8a6005ab395930b5e84f8fdb4c213caebcb4f5ea6d2c3243877ce6196f71ee9`）。B1 后四值仍全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席本步只读复核，不写新的独占 CRITERIA-LOCK-SHA256 行）。
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句（仅对 `complex_test` 做 SELECT）；未用 MCP 或子代理。探针日志与探针测试只读。
- 本席 B2-r1 票：**PASS**（整批候选同意）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M032-001 | 本席 GR-03（席甲 B1 漏）＋席甲 B2 真库探针（比 GR-03 所写更重） | 细化 / 是 / 是 | **同意入队**；确认加重；本席 B1「Jackson 读记录类默认忽略未知字段」不成立 |
| M032-002 | 席甲 CX-M032-001 ＋ 本席 O-3 | 技术性 / 是 / 是 | **同意入队**；只改类注释 |
| W-1 | 本席 GR-01 ＝ 席甲 CX-M032-002 ＝ XM-M031-A | 只在主工作区未发布版成立；按 D-029 登记未发布工作流，不入本模块队列 | **明确同意不在 M032 入队、不随本模块发布** |
| W-2 | 本席 GR-02 ＝ 席甲 OBS-1 | 同上 | **明确同意不在 M032 入队** |
| W-3 | 席甲 CX-M032-003 ＝ 本席 O-1 | 同上 | **同意登记到该工作流** |
| W-4 | 席甲 CX-M032-004 ＝ 本席 O-12 | 同上 | **同意** |
| （无新 ID） | 两席其余观察 ＋ 本席 O-4/O-5 | 只登记不入队 | **同意；无应升队项** |

冲突入队 0。细化入队 1（001）。技术性入队 1（002）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001、002（2）；本批新开「否」ID = 0。未发布工作流登记 W-1…W-4。B1 两席均 FAIL，合取 FAIL，本批不撤回任何 B1 票。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、001（GR-03 加重）。票：确认席甲加重；反驳本席 B1 的 Jackson 默认忽略说；同意甲＋乙＋丙；可空妥当；无更小且同准的改法。**

本席 B1 GR-03 只把缺陷写成「整份覆盖 jsonb 丢掉 `deadlineWorkingDays`」，并写「Jackson 读记录类默认忽略未知字段；写回只有四字段」。该读侧判断不成立。独立核源（不重审模块）：

1. `AccessService.grantPolicy()`（已闭环 M031，只读追证）`AccessService.java:1933-1940`：`new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, AccessGrantPolicy.class)`；`JsonProcessingException` → 409 `INVALID_GRANT_POLICY`「后台授权政策格式或上下限无效」。此即探针文案。
2. 仓内 jackson-databind **2.21.5**。`DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` 枚举构造第三参为 `iconst_1`（`enabledByDefault=true`）。`new ObjectMapper()` 不是 Spring 的 `Jackson2ObjectMapperBuilder`（后者才会关掉该闸）。
3. `AccessGrantPolicy.java:9-18` 记录类 4 组件，无 `@JsonIgnoreProperties`、无自配 `ObjectMapper`。
4. 只读 `SELECT parameters FROM supervisor_review_policies WHERE policy_code='access_grant'`（`complex_test`）：JSON 含 `"deadlineWorkingDays": {"max": 30, "min": 1, "value": 3}`。`flyway_schema_history` 中 V389 `success=t`。与 V389 第二段 UPDATE（非涉钱行补 value=3）一致。
5. 探针只读：`/home/james/audit-tmp/m032_probe.log` `M032PROBE grantPolicy FAILED ApiException 后台授权政策格式或上下限无效`；测试 `/home/james/audit-tmp/M032GrantPolicyProbeTests.java` 只调 `service.grantPolicy()`，worktree＝origin/main `2c178f7a` 原样、库 `complex_test`。`UnrecognizedPropertyException` 是 `JsonProcessingException` 子类，必进上述 409 包装。本席采信该只读日志，未重跑、未对库写。
6. 凡经 `grantPolicy()` 的路径在跑过 V389 的库上都会 409。本席核到的调用点：`AccessController.grantPolicy` GET；`AccessService.listGrantRequests` 两重载（`:1036`、`:1049`）；`grantSummary`（`:1066`）；`grantReviewPolicy`（`:1916`）← `applyGrant`（含须审批权限的授权申请）、`scopeChangeReviewPolicy`（范围变更）、`holdHighRiskPositionPermissions`（岗位模板高危定岗）。与候选所列路径一致。
7. 写侧仍成立：`JdbcAccessRepository.saveGrantPolicy:776-783` `parameters = CAST(:parameters AS jsonb)` 整份覆盖。`AccessService.updateGrantPolicyAudited:1960` `writeValueAsString(request.policy())`。记录类序列化无该键。`SupervisorReviewService.scheduleDeadline:682-683` 读空则回落涉钱 1 日 / 其余 3 日。两文件相对 origin/main **逐字节相同**（AccessGrantPolicy SHA256 `6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7`；origin/main `saveGrantPolicy` SQL 与工作区 L776-783 同文）。

**确认：席甲加重成立。本席 B1「Jackson 读记录类默认忽略未知字段」撤回。缺陷比「保存才丢键」更重——读路径在现行库上已经 409。**

最小处置逐点：

- **甲、同意。** 增第 5 组件 `BoundedInt deadlineWorkingDays`，可空。未跑 V389 的库无该键；Jackson 默认 `FAIL_ON_MISSING_CREATOR_PROPERTIES=false`，缺键 → null，不进 `BoundedInt` 紧凑构造。非空走既有 min/max/value 校验。紧凑构造器**不得**把第 5 组件列入非空闭集（现 L12-15 只强制前四项，保持）。保留 4 参便捷构造器：`M006JdbcRegressionTests.java:144-146` 确在用 `new AccessGrantPolicy(BoundedInt, BoundedInt, BoundedInt, riskReviews)`（全仓仅此一处）；第 5 组件入规范构造器后若无 4 参转发，M037 该测编译失败。便捷构造器必须 `this(..., null)`。
- **「序列化时为空不输出」是甲的硬约束，不是可选项。** `updateGrantPolicy` 仍是已闭环的 `new ObjectMapper().writeValueAsString`（默认输出 `"deadlineWorkingDays":null`）。若 4 参对象带着 JSON null 再走乙的 `||`，PostgreSQL 会把库内对象盖成 JSON null，时限键名还在、值没了，比整份覆盖更隐蔽。必须在记录类（类级或该组件）加 `@JsonInclude(NON_NULL)`，**禁止**为这件事改 M031 的 `AccessService`。禁止把第 5 组件写成 primitive `int`（无法表达未跑 V389）。
- **乙、同意。** `parameters = parameters || CAST(:parameters AS jsonb)`：右侧出现的键覆盖，客户端没带的键保持库内原值。与甲合用：GET 能看见时限；旧客户端／4 参构造器不抹键。
- **丙、同意。** `AccessJdbcIntegrationTests`（M035，PENDING，与 origin/main 逐字节相同，现 0 处 grantPolicy 断言）补真库回归：经同一解析路径（`grantPolicy()` 或同款 `new ObjectMapper().readValue`）得到含 `deadlineWorkingDays` 的对象（现行行 value=3）；保存一份不带该键的政策后 `parameters ? 'deadlineWorkingDays'` 仍为真。`AccessServiceTests.java:1678` 内存政策 JSON 现四键、与 origin/main 同一行，补上该键。内存假仓不实现 `||`，抹键防护以真库条为准。

有无更小或更准：

- 只加 `@JsonIgnoreProperties(ignoreUnknown=true)`：409 消、GET 仍看不见时限，且须另配乙才能挡住抹键。比甲窄，不准。
- 只改 `ObjectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES)`：要动已闭环 AccessService。
- 只乙不甲：写不抹，读仍 409。
- 第 5 组件非空＋整份覆盖：未跑 V389 的库缺键即炸；4 参构造器无法保留；要把 M037 一并改。比候选大。
- 甲＋整份覆盖、靠 GET 往返：前端现只编四字段，4 参测试会抹键。乙不能省。

**可空妥当。** 它表达的是「键不在文档里」，不是「时限为 0」。不发明出厂默认（出厂默认已在 V389 / `SupervisorReviewService` 回落里）。无更小且同时修读 409、写抹键、又不重开 M031 的改法。

非阻断（不否决）：候选冻结列写 PGM-026。该 ID 锚点是治理地图「商户和主体·主要功能」（`criteria-lock.md` PGM-026），原文无审批时限句。时限句在 **PGM-029**（`platform-governance-map-for-operations-v0.1.html:529` / 锁 PGM-029 L5556）。PGM-029 **不在** M032 映射，**不扩锁**。分类栏上位句已能指到该 HTML。定稿冻结支撑以 GEN-002 / GEN-006 / GEN-008 为准；PGM-026 不作本条支撑（席甲 B1 已判 PGM-026 无本面）。不 LOCK-AMEND。

**二、002（CX-M032-001＋O-3）。票：同意入队；只改类注释。**

- `EffectivePermissions.java:13-14` 现在时：「`wildcard=true` 仅当 actor 命中平台管理员桥」。
- 全后端唯一构造点 `AuthorizationService.java:206` 第四参恒 `false`（origin/main 同；本席 `rg "new EffectivePermissions("` 仅此处）。L200-204 的 `if (wildcard)` 因此是死分支；平台管理员日常码并入 `grants`（L191-199）。
- 两文件相对 origin/main 逐字节相同（EffectivePermissions SHA256 `68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e`）。
- GEN-016：现在时声称不可复现。先例 M026-001 注释收口入队。只改注释为现状（`wildcard` 现恒 false；日常码已在 `grants`；`denies` 仍优先），不改字段、不改计算、不改前端 185 处读方。同意。

**三、D-029 与 W-1…W-4。票：明确同意不在 M032 入队、不随本模块发布；没有必须在主工作区修并随本模块检查点带出的技术理由。**

独立实核「只在未发布版成立」：

| 项 | 本席实核 |
|---|---|
| GR-01 / W-1 | 工作区 `JdbcAccessRepository.java:75/309/418`（另 `:202` `listUsers(String)` 同函数）`requireProjectScopeId`；origin/main 对应 `if (organizationId == null) return listTemplates()` / `listRoles()` / 无 WHERE 全表。提交 `52074492` **不是** origin/main 祖先（`merge-base --is-ancestor` 退出 1）。提交说明原文：「带参列人员/模板/角色时空范围改为拒绝，不再退成全表。」 |
| GR-02 / W-2 | 工作区 `AuthorizationService.java:500-507` Quietly＝`Optional.of(requireProjectScope(actor))`，注释「红线④：不因平台管理员身份返回空（全量）」。origin/main `:510-515` 先 `isPlatformAdminBridge` → `Optional.empty()`，注释「永不抛」。仅平台管理员／无项目桥这一支在未发布版改为抛；非管理员无 org 在 origin/main 经 `requireProjectScope` 本来就会抛。 |
| W-3 | 工作区 `grant_group_id` 整组消耗来自 `49aaaa94`（非 origin 祖先）。本席 B1 worktree 对现行 Jdbc：`M006JdbcRegressionTests` 12 测 1 ERROR，夹具 TEMP 表无该列。生产表有该列。 |
| W-4 | 工作区 `AuthorizationService.java:141` `SessionIssuer.MASTER_ACCOUNT_TYPE`。origin/main `SessionIssuer` 无此常量（仅 `MASTER_USER_ID` / `MASTER_USERNAME`）。席甲基线二：origin/main＋本模块 25 个现行文件编译失败。本席 B1 一次性 worktree 同结果（O-12）。 |
| 本席拟处置 vs 提交说明 | B1 拟「Jdbc 恢复 null→全量」「Quietly 无项目返回 empty」与 `52074492` / Quietly 新注释**正相反**。 |

七类红线第四条原文（治理地图 L528，锁 PGM-029）：「④看不到别的商户和别的项目的数据」；持授权 U 盘密钥的最高权限可**逐笔**开放；不设关闭开关。它约束的是**数据可见边界**，没有写「平台管理员列权限模板必须带项目号 / 必须 403」。把红线④读成「管理员列表也不得退全表」是业务口径，不是本模块可在 origin/main 上单独验证的工程事实。取「恢复契约 null＝全量」还是「改已闭环 AccessService 调用点＋种子 `owner_scope_id`」取决于该口径，须报操作员。

**必须在 M032 入队并在主工作区修的技术理由：无。** 可验证办法也不存在：GR-01/GR-02 在 origin/main 基线**不复现**；把恢复 null＝全量写进本模块检查点，等于在 origin/main 上为一条那里不存在的闸做回退；把「继续拒绝」写进检查点，又带不走未发布的 `SessionIssuer.MASTER_ACCOUNT_TYPE`，origin/main＋25 现行文件仍不能编译，也无法在该基线上演示 403 已消失。D-029（检查点只带两版都成立、能在 origin/main 独立验证的修复；只在未发布版成立的项登记到 2026-09-13 工作流，先例 D-027）成立。

**明确同意：** W-1…W-4 不入本模块队列、不随本模块发布；无论操作员取哪边，该工作流发布前必须消掉平台管理员列表 403（恢复 Jdbc null＝全量，或改 AccessService 调用点并使种子平台模板可被等号谓词命中）。本席 B1 拟处置不得当作本模块 B3 边界。W-1 旁注：`listUsers(String):202` 同 `requireProjectScopeId`，管理员主路径已走无参 `listUsers()`，不另开 ID、不升队。

**四、JdbcAccessRepository.java 发布变体。票：可接受；B4 归因成立。**

- 工作区 Jdbc 相对 origin/main：`19 insertions, 16 deletions`，即 `52074492`＋`49aaaa94`，**不含** `saveGrantPolicy` 差。`saveGrantPolicy` 两版同文，同一处补丁能干净打到 origin/main 版。
- AccessGrantPolicy / EffectivePermissions / AccessJdbcIntegrationTests / AccessServiceTests 政策 JSON 行均与 origin/main 相同，无需发布变体。
- 先例：M031 的 `M006SecurityRegressionTests` 发布变体（origin/main 版加同一处补丁；工作区另含别模块未发布改动）。
- B3 在主工作区与发布变体上各打同一处 `saveGrantPolicy` SQL：工作区 before 树已含未发布 hunk，B3 diff 只新增这一处（加 001 的记录类、002 的注释、丙的测试），B4 按 B3 before/after 逐行绑 ID，未发布 hunk 不进 B3 diff，归因成立。
- 检查点只带 origin/main Jdbc＋该 SQL；禁止把 `requireProjectScopeId` / `grant_group_id` 消耗改动夹进发布变体。两份补丁文本必须逐字相同。

**五、其余观察。票：全部只登记；无漏并须升队项。**

候选「两版都有」段已覆盖：`withLifecycle`／生命周期 SQL 恒 inactive（O-2＝OBS-4，M031-001 已在调用侧解决）；`findTemporaryGrant` 非 UUID 抛 IAE（OBS-2）；origin/main `currentSupervisorLevel` 不封顶（OBS-3）；`listUserIdsAssignedToPosition` Jdbc 未覆盖（O-6，M031 已登记）；`copyTemplate` 仓储自调用无事务（O-7）；`knownPermission` 吞异常（O-8）；凭证 upsert 清失败次数与锁（O-9）；`access_users.partner_org_id`、`position_role_templates.archived_by` 等未映射（O-10，「等列」含 grant_requests 单列 `permission_code`）；无咨询锁、有表锁与行锁（O-11＝OBS-6）；`listTemplates(org)` 不含平台模板而 `listRoles(org)` 含全局角色（OBS-5）；读有效权限顺带清理到期行（O- grilling Q8）；`createTemporaryGrant` 逐码 INSERT（OBS-7）。O-4／O-5 已进未发布工作流附记（`currentMerchantScope` 对无商户管理员抛；`pageGrantRequests` 对 null 仍全量）。O-1／O-12 进 W-3／W-4。O-13（主工作区 AccessServiceTests 6 个外来报错）属 D-028，不是两版都有的本模块观察，不升队。O-3 进 002。

不应升队：N+1（GEN-012）；非 UUID 的 IAE（id 来自列表）；恒 inactive（M031-001 已消化）；OpenAPI 不在本模块；红线④是否约束平台管理员（W-1 业务口径）。本席 B1 旁记 ReviewRule 闭集无 `specific_role`：现行 `access_grant` JSON 的 `riskReviews` 只有 low/medium/high/critical 四键，与记录类闭集一致，不立项。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 `M032*`。001 把库内已有键纳入记录类，不发明新时限规则、不改三 HTML。002 只改注释。W 项不入队故不发 2.2。不 BLOCKED-ASK。

---

### 二、分类理由

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **001 细化。** 能指到治理地图「全平台审批通用时限」为后台业务参数（HTML L529）。不是与 HTML 不能同时成立的冲突句（HTML 未写「授权政策 JSON 必须只有四字段」）。不是添加。读侧 409 是 GEN-002；模式／夹具与库 JSON 不一致是 GEN-008；整份覆盖丢键是 GEN-006。同意。
- **002 技术性。** 三 HTML 未点名 `wildcard` 字段；缺口是类注释现在时不可复现（GEN-016）。同意不升细化。
- W-1…W-4 只登记，不进四分类入队。本席 B1 曾把 GR-01 判冲突：那是未发布工作流若整份发布时的产品张力，不是本模块检查点可修的冲突项。不撤回 B1 事实，撤回「在本模块入队」的处置。

工程缺陷 001、002 均为「是」。队列两「是」零「否」。

---

### 三、最小修复边界

写入集合＝`backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java`、`JdbcAccessRepository.java`（只动 `saveGrantPolicy` 一条 SQL；检查点用 origin/main 版＋同一处补丁）、`EffectivePermissions.java` ＋ `AccessJdbcIntegrationTests.java`（M035）＋ `AccessServiceTests.java`（M036）。一次 B3 禁止改 `AccessService.java`、`AuthorizationService.java`、`SessionIssuer`、三 HTML、锁、未发布隔离／整组消耗 hunk。

| ID | 允许 | 禁止 |
|---|---|---|
| 001 | 第 5 组件可空 `BoundedInt deadlineWorkingDays`；紧凑构造器不强制它非空；`@JsonInclude(NON_NULL)`（类或该组件）；4 参便捷构造器 `this(..., null)`；`saveGrantPolicy` 改为 `parameters = parameters \|\| CAST(:parameters AS jsonb)`，主工作区与发布变体同一处同一文本；AccessJdbcIntegrationTests 真库两条断言（读出含该键；不带该键的保存不删键）；AccessServiceTests 内存 JSON 补该键 | 改 AccessService 的 ObjectMapper；第 5 组件 primitive int；缺 `@JsonInclude(NON_NULL)` 却依赖乙去吃 JSON null；只 ignore-unknown 不建模该键；发布变体夹带 `requireProjectScopeId`／`grant_group_id`；动 M037 夹具（4 参构造器就是为了不改它） |
| 002 | 只改 `EffectivePermissions` 类注释为现状 | 改 `wildcard` 字段、构造第四参、`if (wildcard)` 计算、前端读方 |

禁止把 GR-01／GR-02 当成本模块 B3（不得在检查点恢复 null＝全量，也不得在检查点把 Quietly 改回 empty——那是未发布工作流加操作员口径）。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。

- 001：细化／是／是。确认加重：读路径在 V389 库上已 409，本席 B1 Jackson 忽略说不成立。甲可空第 5 组件＋NON_NULL＋4 参便捷构造器；乙 jsonb `||` 合并；丙真库回归＋内存 JSON。冻结支撑 GEN-002／GEN-006／GEN-008；PGM-026 不作本条支撑、不扩锁。
- 002：技术性／是／是。只改类注释。
- W-1…W-4 按 D-029 登记 2026-09-13 未发布工作流，不入本模块队列、不随本模块发布。明确同意。该工作流发布前必须消掉平台管理员列表 403。无必须在 M032 入队的技术理由（origin/main 基线无法验证这两条）。
- Jdbc 发布变体可接受；B4 按 B3 diff 归因成立。
- 观察全部只登记，无升队。

### 结论

全批差集无原意改写。GR-03→001（加重成立，Jackson 默认 FAIL_ON_UNKNOWN_PROPERTIES=true，探针 409 与 `grantPolicy()` 包装句逐字相符）；CX-001＋O-3→002；GR-01／GR-02／CX-003／CX-004→W-1…W-4（明确同意不入队）。甲可空妥当；乙合并必要；甲的 NON_NULL 是乙不把键写成 JSON null 的前提。无添加、无 HTML 申请、无 questions。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M032 IN_PROGRESS；B3=0。
