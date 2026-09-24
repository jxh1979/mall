CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-D029 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.7 / xhigh）
时间：2026-09-23T23:47:22Z
计划：160-audit-prompt.md V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/XM-D029-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：自文首独占锁行起至文末。正文不再出现同名独占字段行。

模块：合成模块 XM-D029 / 计划模块标题 471，B3 第 1 次之后的 B4。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票。任一侧 FAIL 即本轮 FAIL。本席不行使否决。不等于 B4 双 PASS，也不等于 B5 或模块 PASS。

未读取：`modules/XM-D029-b4-r1-self-raw.md` 正文。落盘时该路径已存在，本席只做了 `stat`（84932 字节，mtime 2026-09-23 16:32:27 -0700），没有打开。ledger 顶部封存句在读 D-032 与开工段时进入过视野，不作为归因表、不作为本席票的依据。已读：`160-audit-prompt.md` V0.17 B4 / §5 闸 3 / §6；`findings.md` V3.97 顶部「XM-D029 B2-r2 双 PASS 定稿」与「XM-D029 B2-r1 双 PASS 定稿」及紧随的 B2-r1 候选段；本席旧 `modules/XM-D029-b1-grok-raw.md` 中 B2-r1 / B2-r2 写死边界；冻结 diff；两个 tree；红绿名单与失败信息；相关生产方法在 after tree 上的正文。未用 MCP、子代理、第三席。未改源码、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未装依赖。未对 `complex`、`complex_test` 执行写语句。未建脚本或持久日志。未另建 worktree、未另建空库（本席没有重跑 3532；证据核的是已落盘的红绿名单与冻结 diff）。

---

## 0. 技能预检（本环境实核，按实用强制技能记录）

用户指令禁止子代理、MCP、第三席、问人、修业务。技能本体与之冲突的动作作废，只借结构。本席读了下列 SKILL.md 的现行正文，不是凭记忆。

| 技能 | 本环境 | 本节点 | 记录 |
|---|---|---|---|
| `using-superpowers` | superpowers 6.3.0，已读 | 开工技能自检；用户指令优先于「先派技能再做事」 | 无缺口 |
| `verification-before-completion` | 已读 | PASS 前实算锁哈希、`cmp` 冻结 diff、重算红绿集合差、对照 after tree 源码 | 本席未重跑 Maven。3532 的红绿以席甲日志加本席对 `.red` 的集合重算为准，不把「日志写了 PASS」当成命令输出 |
| `systematic-debugging` | 已读开头 | 只借读错误→追数据流：空间类型主键、设备号哨兵、`requiresBeneficiary` 调用点。不插桩、不改生产代码 | 本体「先加诊断再最小改」作废 |
| `grilling` | mattpocock，已读 | 对抗式自问自答，事实自己读 after tree | 本体「等用户回答 / 派子代理查事实」作废 |
| `requesting-code-review` | 存在 | 本席即 B4 外审。基线是两个 tree id，不是 `HEAD~1` | 不派同会话子代理 |
| `receiving-code-review` | 存在 | 本节点不适用（本席出意见，不修） | — |
| `test-driven-development` / mattpocock `tdd` | 存在 | 核 RED 11 条新红与 GREEN 后这 11 条消失。本席不新写测试 | — |
| `ponytail` / `ponytail-review` | 4.9.0，存在 | 只审可删的越界添加。未落地修改 | — |
| `diagnosing-bugs` | 存在 | 空间类型撞键：读 after tree 的 `saveSpaceTypeDeleteRequest`，不复跑撞键 | — |
| `codebase-design` | 存在 | 看 `platformWide` / `scopeProjectOf` 是否把范围判断收进已有方法，而不是新业务类型 | 不派 DESIGN-IT-TWICE |
| `graft` | `~/.claude/skills/graft/SKILL.md` 存在 | **本轮未调用** | 审计对象是冻结 tree，不是当前脏工作区。工作区另有大量与本 diff 无关的未提交改动。`graft` 索引的是工作区现状，会把 tree 外的字节混进来。定位用 `git show <tree>:<path>` 与 `git grep <tree>` |
| `long-running-background-tasks` | 存在 | 本席没有留下后台作业 | — |
| `writing-for-agents` | 存在 | 本 raw 写给合取席 | — |
| `git-guardrails-claude-code` | 存在 | 只读。未 push / reset / clean | — |
| `resolving-merge-conflicts` | 存在 | 无进行中的 merge / rebase | 节点不适用 |
| `i-have-adhd` | 存在 | 结论在段首。与「raw 第一行必须是独占锁行」冲突时，160 优先 | — |
| `using-arc` / Arc `audit` | 存在 | 不执行。本体要派评审子代理并另写审计产物 | 记不适用 |
| `batch-grill-me` | 技能树中无 SKILL.md | — | **缺口**，不冒充已调用 |
| `ubiquitous-language` | 无 | — | **缺口** |

明确不用：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、mattpocock `code-review`、`domain-modeling`、`ponytail-audit`、`ponytail-debt`、`ponytail-gain`。未使用 MCP。

---

## 1. 输入与独立实算

锁哈希（先算，再对照 sidecar 与 ledger，对照不是抄写来源）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib.sha256 同一文件
openssl dgst -sha256 同一文件
```

三工具同值，与文首独占行一致。文件 891182 字节。判据锁版本 V0.14。sidecar 一行同值。ledger 顶部基准同值。`LOCK-AMEND-COUNT` 仍为 1。本轮未 LOCK-AMEND，未新增判据。

| 项 | 本席实算 |
|---|---|
| findings | V3.97，1379095 字节，SHA256 `088d11a06c6a0d559beb41adcf7650f18a113ecedee68457a84986b8fccea3bb`。队列「是」仍是 XM-D029-001…008，「否」0 |
| before / after | `ce088c52fb3031d1ae65380a2dd10d261cc3c1ca` / `74bb605e734ca7111c6a88873923e3e044f2deeb`，`git cat-file -t` 均为 `tree` |
| 冻结 diff | `/home/james/audit-tmp/xmd029_b3r1.diff`，65541 字节，SHA256 `1fa24e3a4dcd8e8dc2cc700d4c7801f1c18a92382e8b29e925c5e6cc19ed8eb4` |
| `git diff --binary <before> <after> \| cmp - <冻结 diff>` | 退出码 0。全文逐字节相同 |
| 规模 | `git diff --shortstat`：14 files changed, 336 insertions(+), 100 deletions(-)。本席另数 +/- 行（去掉 `+++`/`---`）为 336 / 100。hunk 头 `^@@` 61 个 |
| 模式 | `git diff --raw` 14 行都是 `100644 → 100644`。`git diff --summary` 空。无新增、删除、改名、改模式 |
| 工作区 | 这 14 个文件的工作区 blob 与 after tree 相同（`git diff <after> -- <14>` 为空；`PlatformService.java` 的 `git hash-object` 等于 after blob `2da0b8ee…`）。索引相对 after 仍有差异，符合未提交。工作区其他脏文件不在本 diff 内，不审 |
| before 与 origin/main | 这 14 个文件 `git diff --stat origin/main <before>` 为空。冻结 diff 可以落在 origin/main 上 |

定稿口径：001、002、004…008 用 findings「XM-D029 B2-r1 双 PASS 定稿」段加上方候选段，并对照本席 B2-r1 写死边界。003 用「XM-D029 B2-r2 双 PASS 定稿」丁案，并对照本席 B2-r2 写死边界。乙案已撤回，本 diff 不得再改酒吧、财务、票务、准入切面或 `AccessService` 生产代码。

---

## 2. 逐块归因（61 hunk，越界 0）

每一条 `+`/`-` 行都落在下表的 ID 上。混在同一 hunk 里的行按 ID 拆开。上下文行（diff 里无 `+`/`-`）不是改动。归因不到的行：无。

| # | 文件 | hunk | ID | 改动在做什么 |
|---|---|---|---|---|
| 1 | AuthorizationService.java | `-157,7` | 001 | `effectivePermissionsForUser`：平台管理员与最高权限都放行跨组织查询。职级封顶、DENY、日常通配不在本 hunk |
| 2 | 同上 | `-418,12` | 001 | `currentMerchantScope`：平台管理员返回空 Optional |
| 3 | 同上 | `-433,13` | 001 | `currentMerchantIdQuietly`：平台管理员即使绑了商户号也返回空 |
| 4 | 同上 | `-498,11` | 001 | `currentProjectScopeQuietly`：平台管理员返回空，其余仍 `requireProjectScope` |
| 5 | JdbcAccessRepository.java | `-72,7` | 001 | `listTemplates(null)` 走无参全量 |
| 6 | 同上 | `-199,7` | 001 | `listUsers(null)` 走无参全量 |
| 7 | 同上 | `-211,7` | 001 | `listUsersByMerchant(null)` 走 `listUsers()`。新增注释只说明这条空范围通道 |
| 8 | 同上 | `-306,7` | 001 | `listRoles(null)` 走无参全量 |
| 9 | 同上 | `-415,7` | 001 | `listPositionRoleTemplates(null)` 去掉 WHERE。接口没有无参重载，内联列与排序与带参查询相同 |
| 10 | 同上 | `-1034,7` | 004 | 临时授权整组 `UPDATE … RETURNING permission_code` |
| 11 | 同上 | `-1043,20` | 004 | 每个被标消耗的权限码各插一条使用日志，每条新的 `usage_log_id` |
| 12 | 同上 | `-1470,16` | 001 | 删除 `requireProjectScopeId` / `requireMerchantScopeId` |
| 13 | AuthService.java | `-174,8` | 006 | 刷新：绑定设备才强制同一设备号；空白与哨兵 `unknown` 不算绑定 |
| 14 | ProductionAuthService.java | `-199,8` | 006 | 与上一行同一条件。主会话禁止续期的下一行未改 |
| 15 | PlatformService.java | `-89,7` | 001 | 经营主体列表：全量走 `listBusinessEntitiesPaged(int,int)` |
| 16 | 同上 | `-147,8` | 001 | 商户列表：全量走 `listMerchantsPaged(boolean,String,int,int)`。`projectId` 局部变量在全量分支不传入带项目重载 |
| 17 | 同上 | `-404,17` | 001 | `requireMerchant` 全量只要求商户存在；新增 `platformWide()`；`currentProject` 对空范围返回 null 而不 `orElseThrow`；`requireCurrentProject` 对全量只要求项目存在 |
| 18 | 同上 | `-430,6` | 001 | `creationProject`：平台管理员必须显式给出已存在项目 |
| 19 | 同上 | `-448,7` | 001 | 项目列表走 `listProjectsPaged(int,int)` |
| 20 | 同上 | `-505,6` | 001 | `spaceExists`：全量只要求空间编号能查到 |
| 21 | 同上 | `-515,7` | 001 | 空间列表走 `listSpacesPaged(int,int)` |
| 22 | 同上 | `-705,7` | 001 | 建空间的上级空间：全量改为「与本次显式项目相同」，不再比对「当前项目」 |
| 23 | 同上 | `-734,7` | 001 | 主体绑定列表走 `listBusinessEntityBindingsPaged(int,int)` |
| 24 | 同上 | `-942,7` | 001 | `requireBusinessEntity`：全量只要求主体存在 |
| 25 | 同上 | `-1010,7` | 001 | 档案更新禁止改项目：`Objects.equals`。全量放行 `requireMerchant` 之后，`existing.projectId()` 可以为 null，原先的 `equals` 会 NPE。规则仍是「更新不能改项目」 |
| 26 | SupervisorReviewService.java | `-202,7` | 003 | 审批缺受益人的文案收成授权事项。`:208` 的 `reviewerId.equals(s.beneficiaryId())` 是上下文，未改 |
| 27 | 同上 | `-384,9` | 001 | 同商户闸加上平台管理员例外。最高权限逐笔例外保留 |
| 28 | 同上 | `-618,22` | 005、003 | `requiresTopAuthorityGrant` 只留 `contains("dividend")`（005）。`requiresBeneficiary` 改为含 `grant` / `authorization`，或恰为两个 `.money` 动作名（003）。建单文案与方法体一起收窄（003）。两段注释是这两条的说明 |
| 29 | 同上 | `-788,7` | 003 | 转发缺受益人的文案。改前第 800 行 `target.equals(s.beneficiaryId())` 未改；因上方多了 5 行，改后落在第 805 行 |
| 30 | 同上 | `-880,19` | 001、002 | 待审整表从最高权限改给平台管理员，并留原来的一条总数痕（001）。最高权限进入 `visibleTo` 过滤（002）。注释写明不整表 |
| 31 | 同上 | `-921,18` | 001、002 | `requireVisibleReview`：平台管理员直接可见（001）。不可见时最高权限按复核单号逐笔留痕后返回（002）。`scopeProjectOf`：最高权限无项目号返回空串，不调用 `requireProjectScope`（002） |
| 32 | AccessJdbcIntegrationTests.java | `-872,4` | 004 | 一组两码，用一个，两条消耗留痕 |
| 33 | AccessServiceTests.java | `-2111,6` | 003 | 内存仓库实现 `scheduleSupervisorReview` / `findSupervisorReviewSchedule` |
| 34 | AuthorizationServiceTests.java | `-224,15` | 001 | 两条「平台管理员返回绑定商户」改为返回空。种子绑了 `merchant` 仍必须为空 |
| 35 | M006SecurityRegressionTests.java | `-593,10` | 008 | 恢复 `verify(..., never()).savePositionTemplateLifecycle(any(), anyString())` 与第一处 M037-002 注释。注释正文与发布父 blob `09c86cb6dd7a` 该处一致 |
| 36 | 同上 | `-611,6` | 008 | 恢复第二处 M037-002 注释，正文与同一 blob 一致。生产规则未改 |
| 37 | AuthServiceTests.java | `-252,4` | 006 | 网页无设备号可续期；`login()` 带 `staff-phone-1` 的会话缺设备号、错设备号都拒 |
| 38 | WecomRiskHitsEndpointReproTests.java | `-42,6` | 007 | `DynamicPropertySource` 写入 `complex.auth.master.public-key`，Ed25519 测试公钥。属性名与 `ProductionAuthJdbcIntegrationTests` 第 237–240 行、`MasterAuthorityService` 的 `@Value` 相同。未恢复豁免 |
| 39 | PlatformServiceTests.java | `-82,7` | 001 | 平台管理员建商户补显式 `org_1` |
| 40 | 同上 | `-95,7` | 001 | 查询留痕的 `projectId` 期望从 `org_1` 改为 `""`。生产侧原有 `projectId == null ? "" : projectId`，全量后局部变量为 null |
| 41 | 同上 | `-136,7` | 001 | 校验失败用例补显式项目，断言仍是 `VALIDATION_FAILED` |
| 42 | 同上 | `-151,7` | 001 | 建商户夹具补 `org_1` |
| 43 | 同上 | `-207,12` | 001 | 合作方式用例补 `org_1`，错误码与成功断言未改 |
| 44 | 同上 | `-222,12` | 001 | 状态枚举用例补 `org_1`，错误码未改 |
| 45 | 同上 | `-248,7` | 001 | 主体角色白名单用例补 `org_1` |
| 46 | 同上 | `-504,7` | 001 | 业态错误码用例补 `org_1` |
| 47 | 同上 | `-516,7` | 001 | 种子商户补 `org_1` |
| 48 | 同上 | `-639,7` | 001 | 建主体请求补 `org_1` |
| 49 | 同上 | `-844,7` | 001 | 建主体补 `org_1` |
| 50 | 同上 | `-855,7` | 001 | 第二条建主体补 `org_1` |
| 51 | 同上 | `-894,7` | 001 | 未绑定商户夹具补 `org_1` |
| 52 | 同上 | `-1058,7` | 001 | 数值校验请求补 `org_1` |
| 53 | 同上 | `-1093,7` | 001 | 角色文案用例补 `org_1`，仍断言消息不含英文枚举 |
| 54 | 同上 | `-1141,6` | 001 | `projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin` 改由 `employee` / `org_1` 执行。断言仍是只见本项目 |
| 55 | 同上 | `-1172,6` | 001 | `spaceRequiresCurrentProjectAndNonnegativeArea` 同样改普通岗位。缺项目、跨项目、负面积三条抛错还在 |
| 56 | 同上 | `-1261,9` | 001 | 删掉 `companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster` 里平台管理员对 `other-project` 建主体必须抛错的断言。该断言锁的是「平台管理员不能在另一项目新建」。同方法里平台管理员不得 `associateBusinessEntityProject` / `assignMerchantProject` 的两条抛错还在 |
| 57 | 同上 | `-1481,4` | 001 | 新用例：平台管理员五个列表的 total 都是 2 |
| 58 | SupervisorReviewServiceTests.java | `-435,14` | 001、002 | 平台管理员待审改为可见 a 与 b。最高权限改为只见本项目 a。跨范围列表事件名仍要出现 |
| 59 | 同上 | `-759,8` | 005 | 资金类不再自动升档，这条转发用例改为显式 `permission_grant_l3`。转发断言未删 |
| 60 | 同上 | `-875,11` | 001、002 | 时限用例末尾的整表查看从最高权限改为平台管理员。时限的 `isAfter` 断言未删 |
| 61 | 同上 | `-1232,4` | 002、003、005 | 新增四条：最高权限按编号逐笔留痕；六参免单可建可审；授权申请缺受益人仍拒；被授权人自审仍拒；资金类保持 `permission_grant_l1`、分红类升到 `permission_grant_l3` |

文件集合：6 个生产文件 + 8 个测试文件。没有酒吧、财务、票务、`ActionGateAspect`、`AdmissionGuard`、`AccessService` 生产代码、`MasterAuthorityService`、`PlatformController`、`PlatformJdbcIntegrationTests`、`LeasingSignAtomicityJdbcIntegrationTests`。后两个不在写入的强制改动里：B2-r1 写的是「只改把平台管理员锁进单一项目的断言」。它们不在 GREEN 的 23 条红里，也不是 RED 相对基线的 11 条新红，说明现行断言在生产补丁下仍能通过。本 diff 没有为它们加行。

---

## 3. 内容审计

### 001 平台管理员数据范围

`isPlatformAdmin` 就是 `isPlatformAdminBridge`：`status=active` 且账号类型 `platform_admin`。不含最高权限。三个范围方法只在这个桥上返回空。`currentMerchantIdQuietly` 在读 `merchantId` 之前返回空，种子绑了 `merchant` 也看全量。非平台管理员缺商户号仍抛 `MERCHANT_SCOPE_REQUIRED`；缺项目号仍走 `requireProjectScope`。

`effectivePermissionsForUser` 只放宽了跨组织那一层的门。后面的通配仍按**被查账号**是不是平台管理员计算，这行不在 diff 里。`permissionGrant`（after tree 第 70–94 行）仍是：先 DENY，再最高权限，再「平台管理员且权限码属于日常集合」。日常通配没有改成全部权限。`currentSupervisorLevel` 仍是最高权限返回 3、其余 `min(derived, 2)`。这两处都不在 diff 里。

Jdbc 五处：`null` 走全量，空白字符串不走全量（落进带参查询，不再 403）。定稿要求删掉两个私有辅助方法，并让空范围（平台管理员的空 Optional，调用方传 null）恢复全量。空白不再 403 是删辅助方法的直接结果，没有把空白当成全量。岗位模板没有无参方法，内联 SELECT 的列和 `ORDER BY` 与带参查询一致，只是没有 `WHERE`。

`PlatformService.platformWide()` 先 `requirePlatformScope()`，再看 `currentProjectScopeQuietly().isEmpty()`。只有平台管理员为真。五个列表在真时调用 B2-r1 点名的不带项目参数重载，没有把 null 传进 `listXxxPaged(String projectId, …)`。带项目重载仍是 `projectId != null && 相等`，null 会滤成空表；最高权限不走 `platformWide()`，`masterHasNoGlobalListOrOrdinaryProfileBypass` 仍断言五个 total 为 0，该方法不在本 diff 里，也不在 GREEN 红名单里。

单对象：`requireMerchant`、`spaceExists`、`requireBusinessEntity` 在全量下只查存在。`requireCurrentProject` 在全量下只查项目存在。建空间、建主体、建商户：`creationProject` 对平台管理员调用 `requireText(explicit, "project_id")` 再查项目存在，不再用当前项目填空。最高权限显式项目的原分支留在它前面，平台管理员进不了那个分支。`listSpaceTypes` 不在 diff 里。

普通岗位：`projectLists…` 与 `spaceRequires…` 改为 `employee` / `org_1`。跨项目建空间仍抛错。`creationProject` 的非全量分支仍是 `requireCurrentProject(显式或当前)`。

`Objects.equals` 见上表第 25 行。它不新开「允许改项目」。

删掉的建主体抛错断言：所在用例先 `asMasterAuthority()` 再 `TestSecurityActors.asPlatformAdmin()`。删的是平台管理员在 `other-project` 新建必须失败。这是 D-032 要去掉的锁。同用例仍锁平台管理员不能走最高权限才有的关联与指派。

### 002 最高权限待审

`listPending`：最高权限不再 `items = all`，不再记一条总数。总数事件只在平台管理员整表分支。过滤走原有 `visibleTo`：当前节点、被转发人、发起人，否则同项目且同商户。`visibleTo` 方法体不在 diff 里。

`scopeProjectOf`：最高权限用 `organizationId`，null 成 `""`，不调用 `requireProjectScope`。`visibleTo` 的同项目比较是发起人组织编号 `equals(projectId)`。空串只命中组织编号也为空的发起人，不会把整表放进来。生产破窗把最高权限写成 `platform-company` 时，对不上普通项目编号，列表里只剩他作为节点、被转发人或发起人的单。

`requireVisibleReview`：平台管理员直接返回。其余先 `visibleTo`。不可见且是最高权限时写 `governance.supervisor_review.viewed_cross_scope`，`businessId` 与 metadata 的 `review_id` 都是这一张复核单号，然后返回。不可见且不是最高权限仍 403。`findReview` 仍调用 `requireVisibleReview`（第 882 行，不在 diff 里）。

新用例 `masterOpensAnotherProjectsReviewOnlyOneByOneAndLeavesATrace`：最高权限组织是 `org_1`，单据发起人组织是 `org_2`。列表不得包含该单；`findReview` 必须留下这条事件且 `businessId` 等于该复核单号。做不到就失败。本用例的最高权限带了 `org_1`，没有再单列「组织编号为 null 不 403」。`scopeProjectOf` 的 null 分支在源码里，不另造一条通过条件。

### 003 丁案

`requiresBeneficiary` 的真值集合与定稿一致：含 `grant`，或含 `authorization`，或恰为 `access.grant_request.money`，或恰为 `access.scope_change.money`。`refund`、`waiver`、`debit`、`deduction`、`ar_ap_adjustment`、以及「凡 `.money` 结尾」都不在。`access.scope_change`（无 `.money`）没有新加。`access.grant_request.money` 已被 `grant` 覆盖，多写的等于比较是定稿点名的那一项。

调用点仍是同一个方法：`evaluate` 里 `requireBeneficiaryIfNeeded`（after 第 143 行）、`blockForAdmission`（第 464 行）、审批（第 204 行）、转发（第 795 行）。六参 `blockForAdmission` 仍把受益人传 null（第 423 行，不在 diff 里）。

`:208` 仍是 `reviewerId.equals(s.beneficiaryId())`，不看动作名。转发等于比较改前在第 800 行，改后在第 805 行，语句未改：`target.equals(s.beneficiaryId())`。`isAlwaysBlocked` 仍含免单、退款等，不在 diff 里。丁案收的是「没有受益人就不许建、不许审、不许转」，不是关掉已经传入的员工账号比较。

三处文案从「退款、免单、授权与资金事项必须登记直接受益人」改为「授权事项必须登记直接受益人（被授权人）」。错误码仍是 `BENEFICIARY_REQUIRED`。旧文案在清单收窄之后是假话，改文案是这条强制收窄的一部分，没有新的错误码或新的校验种类。

没有改 `BarService`、`BarBillingService`、`BarOrderService`、`FinanceService`、`TicketingService`、`ActionGateAspect`、`AdmissionGuard`、`AccessService` 生产代码。身份关联没有做。XM 登记不在本 diff 里落代码，符合「本批不建这张关联」。

`AccessServiceTests` 内存仓库补的两个方法与接口默认方法同签名。`ReviewSchedule` 是六字段记录，测试用 `(reviewId, beneficiaryId, dueAt, 0, null, null)` 对得上。定稿点名的六条授权用例名，正是 RED 相对基线从红名单消失的那六条：夹具补上之后，旧生产代码已能把被授权人送进批准，所以它们在「只落测试补丁」时就不再红。免单那条在旧生产上仍红，生产补丁后消失。这是夹具与强制收窄分开的证据，不是把失败断言删掉。

回归三条都在 `SupervisorReviewServiceTests`：六参 `bar.bill_waiver` 能建单且审批不因缺受益人抛错；`access.grant_request` 无受益人抛 `BENEFICIARY_REQUIRED`；受益人 `usr_grantee_self` 自己审批抛 `SUPERVISOR_SELF_REVIEW_FORBIDDEN`。`asSupervisorLevel(1)` 的发起人是 `usr_supervisor_l1`，与受益人不是同一个人，所以第三条锁的是 `:208`，不是「发起人自审」。`doesNotThrowAnyException` 能在审批抛 `BENEFICIARY_REQUIRED` 时失败。定稿没有要求第四条。

### 004 逐码留痕

模板申请路径仍按 `application.id = :source_id` 更新一行，日志用触发的那一个权限码。整组消耗在 `templateRows == 0` 的临时授权：`WHERE grant_group_id = :source_id` 且 `RETURNING permission_code`，然后对返回的每个码插入一条 `permission_template_usage_logs`。`MapSqlParameterSource(Map)` 会把当时的值抄进新的参数源，循环里再覆盖 `usage_log_id` 与 `permission_code`。没有把多个码塞进一列，也没有改回按单个码消耗。

真库用例：同一 `TemporaryGrant` 两个码，消耗 `ops.production_readiness.view`，断言消耗日志按权限码排序恰好是 `check` 与 `view`。只写一条就会失败。该用例在 RED 新红名单里，GREEN 后不在 23 条里。

### 005 只留 dividend

`requiresTopAuthorityGrant` 返回 `actionType.contains("dividend")`。`.money`、`debit`、`deduction`、`authorization` 不再升档。`amountCent` 仍不参与，签名保留，避免改调用点。`access.grant_request.money` 不含 `dividend`，不会升到 L3。自报拒绝在 `resolvePolicy` 第 603–604 行，不在 diff 里：动作名等于 `permission_grant_l1` 或 `l2` 仍抛 `GRANT_LEVEL_NOT_SELECTABLE`。

新用例直接调用 `blockForAdmission`：`access.grant_request.money` 传入档 `permission_grant_l1`，断言 `policyCode` 仍是 `permission_grant_l1`；`finance.dividend.release` 传入同一档，断言变为 `permission_grant_l3`。升档只发生在 `requiresTopAuthorityGrant` 为真且找得到 `permission_grant_l3` 时。这条在 RED 新红名单里。

`deadline` 附近那条用例把资金类从依赖自动升档改为显式 `permission_grant_l3`，转发断言还在。自动升档改由上面的新用例锁。时限的 `isAfter` 没有删。

### 006 设备号

两处刷新条件相同：

```
deviceBound = 会话设备号非空白 且 不等于 "unknown"
拒绝当 (deviceBound 或 请求设备号非空白) 且 两者不相等
```

`createSession` 两处都是 `blankToDefault(deviceId, "unknown")`（AuthService 第 367 行，ProductionAuthService 第 435 行），不在 diff 里。网页登录不带设备号，库里的值是 `unknown`，刷新不带设备号时条件为假，放行。绑定了 `staff-phone-1` 的会话，缺或错都是 `SESSION_MISMATCH`。已绑定会话不会因为请求空白就跳过核对。主会话禁止续期仍在设备核对的下一行：`MASTER_SESSION_REFRESH_FORBIDDEN`。`AuthServiceTests` 里原有的主会话续期拒绝用例不在本 diff 的删除范围。`isBlank` 两个类里都有。

新用例能失败：旧生产对所有会话 `requireText(deviceId)`，RED 名单里有这条。`login()` 的设备号是 `staff-phone-1`，不是空白。

### 007 测试公钥

`WecomRiskHitsEndpointReproTests` 增加的公钥生成与 `ProductionAuthJdbcIntegrationTests` 第 234–248 行同形：类加载时 `Ed25519`，`DynamicPropertySource` 把公钥 Base64 写到 `complex.auth.master.public-key`。`MasterAuthorityService` 不在 diff 里，构造器缺公钥仍拒绝启动。基线红名单里的 `listRiskHitsFirstScreenReturns200OnEmptyTable` 在「只落测试补丁」后不再红，因为上下文能起来；生产豁免没有加回去。

### 008 恢复校验

`verify(repository, never()).savePositionTemplateLifecycle(any(), anyString())` 回到越权存档抛错之后、平台管理员真正存档之前。两处注释与 blob `09c86cb6dd7a` 的原文一致。没有改 `changePositionTemplateLifecycle` 的生产分支。

### 测试有没有借机删弱

相对基线从红名单消失的 7 条，本席从两份 `.red` 的集合差得出：

- `AccessServiceTests` 六条授权用例：夹具开始保存排期。旧生产已经把被授权员工传入。它们在只落测试补丁时转绿，是 003 的夹具半边。
- `WecomRiskHitsEndpointReproTests.listRiskHitsFirstScreenReturns200OnEmptyTable`：补上测试公钥后上下文能启动。

相对基线新出现、且生产补丁后消失的 11 条，就是上表里新断言或改写了期望的用例（含 `merchantViewAudit…` 的 `projectId`、`deadlineIsShorterForMoneyActions` 的查看人、`pendingListIsScoped…` 的平台管理员与最高权限对调）。GREEN 的 23 条全部是基线 339 条的子集，没有基线之外的意外红。

23 条的类分布：`FinanceControllerTests` 12、`FacilityEquipmentArchiveDeleteJdbcIntegrationTests` 3、`ComplianceControllerTests` 3、`AmusementServiceTests` 1、`CustomerGuardedActionAndRejectionAuditTests` 1、`FieldLabelsTests` 1、`StaffProfileArchiveDeleteJdbcIntegrationTests` 2。后两类加起来 3 条，其余 20 条。`FieldLabels` 的失败信息是缺 `business_entity_id` 中文名。两条员工档案失败信息是「请填写所属公司」。与留给 XM-M036-A 的口径一致。

本席没有重跑 Maven。上述数字是本席对 `/home/james/audit-tmp/xmd029_b3_red.red`（343）、`xmd029_b3_green.red`（23）、`xm036a_base.red`（339）做集合差，并与 `xmd029_b3_run.log` 第 5、20 行的 `3532` / `343` / `23` 以及第 7–17 行的 11 个类名核对后的结果。日志第 43 行写主工作区 diff 与 worktree diff 逐字节相同；本席自己的 `cmp` 证明冻结 diff 与两个 tree 的 `git diff --binary` 逐字节相同。14 个文件的 before tree 与 origin/main 无差，因此「补丁打在 origin/main 上」与「两个 tree 的差」是同一份字节。

### 空间类型删除申请撞键

`JdbcPlatformRepository.saveSpaceTypeDeleteRequest`（after tree 第 392–401 行，该文件不在本 diff）把主键写成 `"stdr_" + spaceTypeId + "_" + Instant.now().toEpochMilli()`。同一空间类型在同一毫秒两次插入会撞同一主键。`PlatformJdbcIntegrationTests.completingSpaceTypeRequestKeepsPriorRequestHistory` 不在 GREEN 的 23 条里，这次 GREEN 跑过了。撞键解释「有的跑次红、有的跑次绿」。该方法是 M009 已发布代码，不是本轮 14 个文件里的行。不在 XM-D029-001…008 的最小处置里。把它登记给 XM-M036-A 与「本 diff 不修它」一致。若本 diff 去改这个主键，那一行归因不到 001…008，闸 3 会算越界。

### 对抗式里看过、没有升成缺陷的点

- 最高权限会不会被 `platformWide()` 当成全量：不会。账号类型不是 `platform_admin`。
- 空项目会不会把 null 送进带 `projectId` 的重载：五个列表都走无项目重载。
- 未绑定会话若请求里带了别的设备号：按「提供了就得相等」拒绝。网页刷新体是空设备号，测试锁的是这条。已绑定会话不会因为请求空白而跳过。
- 文案三处是不是顺手改业务：错误码未变，句子收成授权事项，与清单收窄一致。
- `Objects.equals` 是不是顺手改更新规则：更新仍禁止改项目；变的是 null 项目编号不再 NPE。
- 岗位模板内联 SQL 是不是另一套过滤：列与排序与带参查询相同，只去掉 WHERE。
- 模板申请路径只记触发码：该 UPDATE 只标一行申请。整组码在临时授权的 `RETURNING` 上。定稿针对的是 `grant_group_id`。

新业务：没有新角色、新枚举、新接口、新的身份关联表，也没有把收款人编号写进受益人字段。

---

## 4. 票

内容：PASS。八个 ID 的生产改动落在定稿最小处置和本席 B2-r1、B2-r2 写死的边界里。最高权限识别、职级封顶、日常通配与显式 DENY 优先不在 diff 里。平台管理员列表走不带项目参数的重载。单对象只查存在，新建要显式项目，普通岗位仍按真实项目。最高权限待审不整表，按编号留痕，无项目号不抛 `PROJECT_SCOPE_REQUIRED`。受益人强制清单与定稿的那几项一致，`:208` 与转发等于比较未改。使用日志按被消耗的码逐条写。升档只看动作名含 `dividend`。未绑定含空白与 `unknown`。公钥与 `never()` 校验按定稿恢复。测试改动锁的是被去掉的行为或新的回归，新断言在旧生产上会红。文件模式未变。没有新的缺陷进入本轮修复队列，也没有新业务。

归因：PASS。61 个 hunk、336 条新增、100 条删除都能归到 XM-D029-001…008 里仍归属「是」的 ID。越界行 0。

总结论：PASS。
