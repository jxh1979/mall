CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B1 修复前全量审计 — Grok 席乙 raw（V0.01）

- 席位：Grok 席乙（独立；未读 `modules/M035-b1-self-raw.md`，未用 MCP / 子代理 / 第三席）
- 模型：grok-4.6 / xhigh
- 阶段：B1 修复前全量（本模块首次开工；ledger 已见 `PENDING→IN_PROGRESS`，本席不写 ledger）
- 提示词：`160-audit-prompt.md` V0.17；runbook V0.02；modules.md V0.04；criteria-lock.md V0.11
- 模块：M035「功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第1段」
- 范围文件（2，全文读，不抽样）：
  - `backend/src/test/java/com/complex/platform/access/AccessControllerTests.java`
  - `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java`
- 本席结论：**FAIL**（3 条临时 GR 工程缺陷成立；收齐后再统一判定，未边审边修）

---

## 0. 技能预检（§6，按环境实核）

本席运行环境：`hostname=y9000p-ai`，正本 `/home/james/complex-management`。技能目录与 Codex 本机 Windows 路径不同，按本机实有路径核对；**未核到的不写已调用**。

| 技能 | 本机路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| verification-before-completion | 同包 `verification-before-completion/SKILL.md` | PASS/FAIL 前先跑测试与 sha256；未跑不宣称绿 |
| systematic-debugging | 同包 `systematic-debugging/SKILL.md` | 只借读错误/追数据流；**不**给生产代码加 instrumentation（B1 禁止修复） |
| grilling / grill-me | mattpocock `skills/productivity/grilling/SKILL.md`、`grill-me/SKILL.md` | 对抗式自问自答；**不等操作员、不派子代理** |
| codebase-design | mattpocock `skills/engineering/codebase-design/SKILL.md` | 控制器门控 seam vs 仓储/授权计算 seam |
| diagnosing-bugs | mattpocock `skills/engineering/diagnosing-bugs/SKILL.md` | 假绿/断言能否变红；B1 不写诊断桩 |
| tdd | mattpocock `skills/engineering/tdd/SKILL.md` | 只借「好测试/ tautological / seam」口径审现有测试；B1 不落新测试 |
| writing-for-agents | mattpocock `skills/productivity/writing-for-agents/SKILL.md` | 本 raw 按步骤+完成标准写给席甲归并用 |
| resolving-merge-conflicts | mattpocock `skills/engineering/resolving-merge-conflicts/SKILL.md` | 本轮无合并冲突，未进入处置步骤 |
| git-guardrails-claude-code | mattpocock `skills/misc/git-guardrails-claude-code/SKILL.md` | 可选；未装 hook（会改仓库外/内配置）。本席未 fetch/commit/push/reset |
| i-have-adhd | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号、无寒暄 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删的测试例外/过声称；B1 不改代码 |
| graft | `/home/james/.claude/skills/graft/SKILL.md` | skeleton 两测试文件 + AccessController |
| brainstorming / writing-plans | 存在 | **阶段 A 技能，B1 不走方案产出** |
| test-driven-development / requesting-code-review / receiving-code-review | 存在 | B3/B4 节点，B1 不实施 |
| ponytail（ultra） | 存在 | B3 默认强度，B1 不用来改码 |
| batch-grill-me | **缺口** | 本机 `find` 无此技能目录 |
| ubiquitous-language | **缺口** | 本机 `find` 无此技能目录 |

按提示词划掉、本轮确认不用：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、research、ponytail-audit/debt/gain、domain-modeling。一次性 worktree 本轮**未建**（见 §1.4：主工作区可跑通，且 origin/main 缺 V427 不能对现库 validate）。

---

## 1. 环境事实（类型 4 命令）

### 1.1 锁哈希（本席独立实算，未抄 sidecar/ledger/派发词）

```text
$ sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar 与 ledger 顶部同值；`LOCK-AMEND-COUNT: 1`。本席只读。

### 1.2 git 与两文件身份

```text
HEAD     = e456ba96c84185c8d39f18f548011daac48a6466  (落后)
origin/main = c3a48ccdb2d39b41d73732becccfe4a18314e88d
```

| 文件 | wc -l | SHA256 | git hash-object（工作区=origin/main blob） | vs HEAD |
|---|---|---|---|---|
| AccessControllerTests.java | 327 | `466b780026d8bab9c814f5a6269fb76bfd0755c520cba5f1797778bc7a30ea0e` | `1b4ea7d72db6ec74666aa931e3ca8571c37fd136` | 相同 |
| AccessJdbcIntegrationTests.java | 873 | `291e657bc4363c3b202b08c3b02b88de032911d46f762fdb19828596d898ff96` | `9716ded51702a09b8ccea3717124cebb850ced60` | 工作区相对 HEAD 多 34 行（M032-001 / M034-001）；内容与 origin/main 逐字节相同 |
| 合计 | 1200 | — | — | `git ls-files -z -- <2 files> \| xargs -0 cat \| wc -l` = **1200** |

`plan/modules.md` 写有效内容 1166：A1 冻结时的行数，**以本轮实算 1200 为准**。`git status --porcelain` 对第二文件为 ` M`（未暂存），与派发所述一致。

AccessController.java 相对 origin/main diff 0 行（只读追证对象，已闭环 M031）。

### 1.3 D-029：结论依赖的协作类版本

`git diff origin/main -- AuthorizationService.java JdbcAccessRepository.java`：两文件主工作区 ≠ origin/main。

| 文件 | 主工作区 | origin/main |
|---|---|---|
| AuthorizationService.java | hash-object `4bfae6d2…` 注释 V0.13；`isMasterAuthority` 改走 `SessionIssuer.MASTER_ACCOUNT_TYPE`；scope 方法不再因平台管理员返回 empty | blob `69072714…` 注释 V0.08 |
| JdbcAccessRepository.java | hash-object `837b04aa…`；`organizationId/merchantId == null` 改为抛 `PROJECT_SCOPE_REQUIRED` / `MERCHANT_SCOPE_REQUIRED`；临时授权 source_id 用 `grant_group_id` | blob `2408af79…`；null 即全量列表 |

**本轮 JDBC 测试实跑加载的是主工作区这两份未发布协作类。** 凡依赖 `AuthorizationService.effectivePermissions()` / `requirePermission()` 或 `JdbcAccessRepository` 空范围行为的断言，成立版本 = **主工作区未发布版**。平台管理员日常桥（`isPlatformAdminBridge`）两版都在，本席**未**在 origin/main 工作区实跑（origin/main 无 `V427__m009_*.sql`，complex_test 已应用 V427，Flyway validate 会失败）。

### 1.4 可执行产物

未 `source /etc/complex/complex.env`。逐行读出三个变量，把 URL 库名改为 `complex_test` 后导出。进程里另外已有 `COMPLEX_CONFIG_DIR` / `COMPLEX_HOME` / `COMPLEX_LOG_DIR`（本席未从该文件装入）。

```text
$ env | awk -F= '/^COMPLEX_/ {print $1}' | sort
COMPLEX_CONFIG_DIR
COMPLEX_DATABASE_PASSWORD
COMPLEX_DATABASE_URL
COMPLEX_DATABASE_USERNAME
COMPLEX_HOME
COMPLEX_LOG_DIR

$ mvn -Dtest=com.complex.platform.access.AccessControllerTests,com.complex.platform.access.AccessJdbcIntegrationTests test
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0  — AccessControllerTests
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0  — AccessJdbcIntegrationTests
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
MVN_EXIT:0
Flyway: Successfully validated 397 migrations; Current version of schema "public": 429
```

psql 只对 `complex_test` 做 SELECT（口令不回显）：

- `flyway_schema_history` 最高 version **429**（`retire verification manual override`）
- `staff_levels`：**30** 行，`level_rank` 1–30，M1–M10 × lower/middle/upper
- `supervisor_review_policies.access_grant.parameters.deadlineWorkingDays` = `{"max":30,"min":1,"value":3}`
- `permissions.verification.manual_override`：**1 行 status=retired**（`permissionExists` 只计 active，故测试 `isFalse()` 与行仍在不矛盾）
- 角色/模板/覆盖/有效临时授权对该码 count 均为 0
- 测后 `agr_m087_%` / `usr_jdbc_%` / 本类创建的 `tpl_*`：**0 行**（类级 `@Transactional` 回滚成立）

AccessController `@*Mapping` 实算 **33**（origin/main 同样 33）。派发「34 个映射」与源码不符，以下按 33。

JUnit parallel：`backend/pom.xml` 无 `junit.jupiter.execution.parallel` 配置。

---

## 2. 全文覆盖（两文件每一测试）

### 2.1 AccessControllerTests.java（L1–L327，13 个 `@Test`；`@TestConfiguration` 会被 `rg '@Test'` 多计 1）

切片：`@WebMvcTest(AccessController)` + 真 `SecurityConfig`/`GlobalExceptionHandler`；`AccessService` 与 `AuthorizationService` **整对象 Mockito mock**。这是控制器 seam：不跑最终权限计算、不跑仓储。门控能否变红，只取决于是否 `doThrow(requirePermission)` 或 `verify`。

| 测试 | 行 | 是否执行目标入口 | 断言能否因被测实现变坏而红 | 假绿风险 |
|---|---|---|---|---|
| unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden | 57–65 | 真 GET `/api/v1/access/permissions` 无登录 | `status ∈ {401,403}`：变 200 会红；**变回 403 空 body 不红**（SecurityConfig L64–66 写明曾把无登录误成 403） | 弱断言，见观察 O-1 |
| createTemplateWithoutPermissionReturnsPermissionDenied | 67–85 | 真 POST `/permission-templates` | stub `access.permission_template.manage` 抛错；控制器若不再调用该码 → 不会 403 → **会红** | 门控真测 |
| listPermissionsReturnsApiResponsePageShape | 87–109 | 真 GET `/permissions`；service 桩返回一页 | JSON `code/items/total` 会因序列化坏而红；**删掉 `requirePermission("access.permission.view")`（控制器 L88）仍绿** | **GR-01** |
| createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed | 111–124 | 真 POST 缺幂等键 | 缺键 400 `VALIDATION_FAILED`；去掉 `requireIdempotencyKey` 会红 | 未测门控码（mock 默许），本条目标是必填 |
| assignUserRoleWithoutRoleAssignPermissionReturnsPermissionDenied | 126–144 | 真 POST `/role-assignments` | stub `access.role.assign`；漏调用会红 | 门控真测 |
| createTemporaryGrantMissingValidUntilReturnsValidationFailed | 146–160 | 真 POST `/temporary-grants` 无 `valid_until` | 对应控制器 L215–217 手写校验；删掉会红（日志原文 `必须填写授权失效时间`） | 未测 `access.temporary_grant.create` 门控 |
| userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions | 162–203 | 真 POST overrides 成功 + remove 缺幂等键 + 再 POST 时 stub 拒绝 | 成功路径只测 JSON `denies`；第三条 stub 后门控会红；remove 缺键会红 | 成功路径未 verify 门控；拒绝路径真测 |
| listStaffLevelsReturnsApiResponsePageShape | 205–219 | 真 GET `/staff-levels` | 页形状会红；**删掉 `requirePermission("access.staff_level.view")`（控制器 L258）仍绿** | **GR-01** |
| assignStaffLevelReturnsOkWithAssignment | 221–242 | 真 PUT `/users/{id}/staff-level` | 成功 JSON 会红；**删掉 `requirePermission("access.staff_level.manage")`（控制器 L267）仍绿** | **GR-01** |
| assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed | 244–256 | 真 PUT 缺幂等键 | 缺键会红 | 未测 manage 门控 |
| effectivePermissionsSelfReturnsGrantsDeniesAndWildcard | 258–277 | 真 GET `/effective-permissions`（控制器故意无功能权限门，L244–247） | 序列化 grants/denies/wildcard/computed_at 会红 | 无门控可测，符合 self 设计 |
| userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied | 279–289 | 真 GET `/users/{id}/effective-permissions` | stub `access.user.view`；漏调用会红 | 门控真测 |
| userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission | 291–308 | 同上成功路径 | 桩返回 `wildcard=true`；序列化会红。实现里 `wildcard` **恒 false**（EffectivePermissions.java L13–14 + AuthorizationService L206） | 测的是字段能写出 true，不是生产恒 false。见 O-2 |
| TestBeans | 310–326 | mock AccessService / AuthorizationService；`new AuthService("", "")` | — | 授权实现整棵替换，符合 WebMvcTest；假绿只发生在未 verify 的调用 |

**33 个映射 vs 本文件：**

已有 HTTP 行使：`GET /permissions`（未登录+页形状）、`POST /permission-templates`（无权限+缺幂等键）、`POST /role-assignments`（无权限）、`POST /temporary-grants`（缺 validUntil）、`POST .../permission-overrides`（成功+无权限）、`POST .../permission-overrides/remove`（缺幂等键）、`GET /staff-levels`、`PUT .../staff-level`（成功+缺幂等键）、`GET /effective-permissions`、`GET /users/{id}/effective-permissions`（无权限+成功）。

未行使（本文件无请求）：templates 的 GET/PUT/apply/copy；roles GET/POST；unassign；grant-policy GET/POST；grant-requests 申请/列表/审批/summary；position-role-templates 全部 5 个；temporary-grants revoke；users GET/POST；PUT position；PUT scope。**不为每个未行使入口补测立项**（见 §6；关键已行使入口的假绿见 GR-01）。

门控码本文件 `doThrow` 过的只有：`access.permission_template.manage`、`access.role.assign`、`access.user.view`。从未出现：`access.permission.view`、`access.staff_level.view`、`access.staff_level.manage`（以及其余 20+ 个控制器门控码）。

### 2.2 AccessJdbcIntegrationTests.java（L1–L873，26 个 `@Test`）

`@SpringBootTest` + 类级 `@Transactional`；注解写死 `jdbc:postgresql://127.0.0.1:5432/complex_test`、username `complex`、password `${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}`。真 `JdbcAccessRepository` + 真 `AuthorizationService`（**主工作区未发布版**）。

| 测试 | 行 | 目标行为是否真执行 | 断言能否红 | 顺序/残留 |
|---|---|---|---|---|
| migrationCreatesPermissionTablesAndSeedsAccessAndOpsPermissions | 45–64 | `to_regclass` + `listPermissions()` | 缺表/缺种子码会红 | 只读种子 |
| v181MovesOutsourcedFacilityPermissionsToTimeBoxTemplatesWithoutRuntimeRolePermissions | 66–147 | 真 SQL 读 role_permissions / template_items / position_role_templates | 外包角色若仍挂运行时权限会红 | 只读种子 |
| v181SeedsConstructionCompletionUploadAndKeepsRuntimeRoleTimeBoxed | 149–210 | 真列 + 角色/模板 | 缺完工上传码或审核码挂错角色会红 | 只读 |
| v180SeedsAuditorRolesTemplatesAsReadOnlyWithoutChangingApprovalFlags | 212–263 | 真 data_scope + 写权限后缀正则 | 审计角色出现 manage/approve/… 会红 | 正则不覆盖所有写动词，见 O-3 |
| auditorReadOnlyAccessAndTemporaryGrantExpiryUseExistingAuthorizationEffects | 265–341 | 真 `createUser`/`assignUserRole`/`createTemporaryGrant` + `requirePermission` | 内部稽核无 view 或外部过期仍能 view 会红 | UUID `2222…` 与到期测试同号；方法级回滚 + 无 parallel → 测后 0 行 |
| outsourcedTemporaryGrantExpiryUsesExistingAuthorizationEffects | 343–402 | 真临时授权 + requirePermission | 过期仍能 view / 有效窗口能 accept 会红 | UUID `4444`/`5555` |
| migrationCreatesStaffLevelTablesAndSeedsThirtyContiguousRanks | 404–419 | `listStaffLevels` + 两端 ID | 非 30 档或 rank 不连续、或 `lvl_m1_lower`/`lvl_m10_upper` 不对会红 | 未逐档断言 10×3 标签；库内实态已是 10×3（类型 4） |
| staffLevelAssignmentPersistsLevelAndSalary | 421–434 | 真 `saveStaffLevelAssignment` | 职级/分/币种写错会红 | 回滚 |
| saveTemplatePersistsGrantAndDenyItems | 436–453 | 真 `createTemplate` | grant/deny 未落库会红 | 不测 DENY 覆盖 GRANT |
| copyTemplateStoresSnapshotIndependentFromLaterSourceChanges | 455–471 | 真 copy 后再改源 | 副本跟着源变会红 | 模板快照 |
| appliedTemporaryTemplateCanBeReadAsActivePermissionEffect | 473–502 | 真 applyTemplate + `findPermissionEffects` | 有效期内读不到 GRANT 会红 | 仓储层来源，不经 HTTP |
| revokingTemporaryGrantIdRevokesEveryPermissionCodeInGrantGroup | 504–554 | 真 create + revoke + effects | 组内一码未撤会红 | 查 `grant_group_id` 列（与未发布 source_id 改动正交） |
| lapsedTemporaryGrantIsExpiredAndLoggedWhenEffectsAreRead | 557–589 | 真读 effects 触发 expire | 到期仍 GRANT、或日志非幂等会红 | 与 auditor 测试同 UUID `2222…`，回滚隔离 |
| lapsedTemplateApplicationIsExpiredAndLoggedWhenEffectsAreRead | 591–625 | 同上，模板应用 | 到期仍 GRANT / 无 expired 日志会红 | 回滚 |
| storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays | 629–645 | 真读库内政策 JSON + 4 参构造保存 | 缺 `deadlineWorkingDays` 或保存冲掉该键会红 | **M032-001**；saveGrantPolicy 合并写在 origin/main 已发布。成立版本：政策解析/合并写与 origin/main 一致 |
| manualOverridePermissionIsRetiredAndGrantedToNobody | 649–659 | `permissionExists`（只计 active）+ 四表 count | 仍 active 或仍有授予会红 | **M034-001**；库内 retired 行仍在，与断言一致 |
| staffLevelChangeRunsInOneTransactionLikePositionAssignment | 663–671 | **不调用** `assignStaffLevel` | 去掉 `@Transactional` 会红；四步写入坏了**不红** | **GR-02** |
| seededDisplayNamesAreChinese | 675–686 | 真 SQL | 挖掉 `AI` 后仍有拉丁字母会红；**展示名里的 AI 永不红** | **GR-03** |
| userPermissionOverridesAreUpsertedReadAsEffectsAndRemovable | 689–708 | 真 upsert 两次 + effects + delete | 非幂等或多条 USER_OVERRIDE 会红 | 无角色 GRANT，不测拒绝优先覆盖授予 |
| moneyAndSensitiveExportPermissionsRequireApproval | 711–720 | 真 `requires_approval=FALSE` 反查 | 四码任一带 FALSE 会红 | 种子 |
| scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck | 723–737 | 真 INSERT 两种 type + 非法 type | CHECK 拒合法/放非法会红 | 测后 0 行 |
| revokeTemplateApplicationsMarksOnlyTheGivenActiveApplicationsRevoked | 740–756 | 真 apply + revoke 两次 | 非幂等或 effects 仍含码会红 | 回滚 |
| sensitiveBusinessDataPermissionsRequireApprovalAndStayOutOfThePlatformAdminBridge | 760–767 | `listPermissions` + `effectivePermissions()`（平台管理员桥） | 四码缺审批或不该出现在 grants 会红 | **主工作区 AuthorizationService V0.13** |
| dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes | 772–787 | 风险档 CHECK + staff_self 申请码 + 日常桥 requirePermission | 非法 risk_level 或日常码过不了桥会红 | 同上版本；authenticate 不建 access_users 行 |
| fiveCategoryHighRiskCodesRequireApprovalBeforeGrant | 792–800 | 五码审批 + 日常桥不含它们 | 缺审批或桥误放会红 | 同上 |
| platformAdminRoleSeedYieldsGovernanceWriteCodesButNotBusinessHighRisk | 803–813 | 建用户+派 `role_platform_admin` + 日常桥 | 治理写码缺失或资金码出现会红 | 同上 |

辅助方法 L815–872：`regclass` 拼接表名仅测试硬编码；`authenticate` 写入 SecurityContext；`assertPermissionDenied` 断言 `PERMISSION_DENIED`（能因误放行而不抛失败）；template/item 夹具。

夹具与库内实态：三十档、deadlineWorkingDays=3、manual_override=retired、风险档约束名均与 SELECT 一致。

---

## 3. 第一性原理三问（2.3）

### 3.1 这段东西为什么必须存在？

删掉 **AccessControllerTests**：三份 HTML 的「权限点列表 / 职级 / 模板 / 有效权限」HTTP 门面将没有本聚合段的控制器级证据。上位（已映射）：BSM-028「管理员工、岗位、班次、职级、权限」、BSM-029「用户权限要知道…职级」、BSM-120「职级可按 M1–M10…」、PGM-066「员工权限按单一功能最小颗粒度…权限点首批由平台统一预置」。答得上 → 有业务上位。

删掉 **AccessJdbcIntegrationTests**：三十档种子、模板 grant/deny、临时授权到期留痕、拒绝覆盖来源、日常桥与人工放行停用将没有真库证据。同上 + PGM-001 全中文展示名。答得上。

### 3.2 声称的行为有没有被实际执行验证过？

有，证据是 §1.4：39 测试 BUILD SUCCESS，Flyway 429，回滚后无夹具残留。  
限制：控制器成功路径**没有**把门控码当成能失败的断言（GR-01）；`staffLevelChange…` **没有**执行改职级（GR-02）；全中文测试把「AI」挖掉（GR-03）。**测试通过 ≠ 声称场景成立。**

### 3.3 哪个前提没验证就当真？

1. Mockito 对未 stub 的 `requirePermission` 是 no-op，成功路径「看起来过了授权」。  
2. `wildcard=true` 的控制器夹具被当成有效集形状，但实现恒 false（O-2）。  
3. 平台管理员日常桥测试的结论写在未发布 AuthorizationService 上（D-029）。  
4. 注释把「HTML 正文出现 AI」当成展示名可以保留拉丁字母 AI 的依据（GR-03，与 PGM-001 明文相反）。  
5. 两测试共用 UUID `2222…` 却假定方法级回滚 + 非并行（本轮测后 0 行，前提目前成立）。

---

## 4. 顺从式：52 项逐条适用 / 无本面

锁映射原文：`M035 → [BEQR-157, BEQR-182…187, BSM-001,003,009,028,029,073,084–088,114,115,120, GEN-001–018, PGM-001,008,022,024,026,032–036,048,059,066]`，实数 52。每项定义全文已从 `criteria-lock.md` 抽出，并与三 HTML 对应锚点原文核对（抽查片段均在 HTML 与 lock 同时存在）。

**未映射但与模块名称直接对应的锚点**（BEQR-149 职级、BEQR-151 最终权限计算、PGM-009/040/041 权限和职级章）在 M038+ 与 HTML 源模块。本席**不**用它们作 finding 冻结 ID（禁止扩锁），只在第一性原理/对抗式引用 HTML 原文说明上下文。

| ID | 适用? | 理由（本两文件） |
|---|---|---|
| BEQR-157 员工状态 | 无本面 | 夹具只用 `"active"`，不枚举待入职/在职/试岗/停岗/离职，无冲突 |
| BEQR-182 考勤日结果 | 无本面 | 无考勤入口 |
| BEQR-183 打卡方式 | 无本面 | 无 |
| BEQR-184 请假类型 | 无本面 | 无 |
| BEQR-185 请假单状态 | 无本面 | 无 |
| BEQR-186 薪资单构成 | 无本面 | 职级薪资分/币种走 BSM-120，不测薪资单公式 |
| BEQR-187 员工提成发放 | 无本面 | 无 |
| BSM-001 整体说明 | 无本面 | 全局原则本面无策略配置/溯源 UI |
| BSM-003 权限分岗 | 适用 | 门控抽样、日常/高危桥、审计只读种子；平台管理员日常段在该 h2 正文（lock 摘录只到 lead，解释权以 HTML 全文为准） |
| BSM-009 基础管理 | 适用 | 权限/员工是基础管理入口的测试面 |
| BSM-028 主要功能 | 适用 | 「管理员工、岗位、班次、职级、权限」；三十档、模板、角色种子 |
| BSM-029 和谁有关 | 适用 | 「用户权限要知道…职级和审批状态」；职级表/分配/高危审批标记 |
| BSM-073 商户专区主要功能 | 无本面 | 无商户门户 HTTP |
| BSM-084 员工手机端 | 无本面 | 无 App；`role_staff_self` 申请码只邻接「自助」不是排班考勤 |
| BSM-085 日常工作 | 无本面 | 无注册/订位 |
| BSM-086 排班考勤请假 | 无本面 | 无 |
| BSM-087 我的薪资 | 无本面 | 无工资条 |
| BSM-088 提成与人员服务 | 无本面 | 无 |
| BSM-114 各类入口 | 无本面 | 无十一端 UI |
| BSM-115 相互关系 | 无本面 | 无跨子系统连接断言 |
| BSM-120 已确认规则 | 适用 | 「职级可按 M1-M10…上中下」「员工权限按单一功能最小颗粒度」；三十档测试 + 权限码种子 |
| GEN-001 | 适用 | 两文件均有可验证用途；GR-02 过声称属于应收窄的注释 |
| GEN-002 | 适用 | 39 测试可执行且与接口声称对照；GR 指出声称≠断言处 |
| GEN-003 | 适用 | **本模块主判据**；GR-01/02/03 |
| GEN-004 | 适用 | 幂等键、validUntil 必填 |
| GEN-005 | 适用 | 四条门控真测 + GR-01 三条成功路径未锁门控码 |
| GEN-006 | 适用 | 类级事务回滚已用 SELECT 证实；职级入口注解存在 |
| GEN-007 | 适用 | override 二次 upsert、revoke 第二次 0、到期日志 size=1 |
| GEN-008 | 适用 | API 页形状；O-2 wildcard 夹具与实现不一致 |
| GEN-009 | 适用 | `assertPermissionDenied`；非法 request_type 期望 DataIntegrityViolation；无吞异常 |
| GEN-010 | 适用 | 未登录与 PERMISSION_DENIED；无密钥入报告 |
| GEN-011 | 适用 | 到期写 `permission_template_usage_logs` |
| GEN-012 | 无本面 | 测试无无界批处理 |
| GEN-013 | 适用 | 注解写死 127.0.0.1/complex_test；密码来自导出变量；Flyway 397/429 |
| GEN-014 | 适用 | 断言已应用迁移终态，不改 V429 等已应用文件 |
| GEN-015 | 适用 | 展示名全中文测试；**GR-03 与 PGM-001 冲突** |
| GEN-016 | 适用 | GR-02 注释不可从当前断言复现四步写入 |
| GEN-017 | 适用 | 本报告四分类 |
| GEN-018 | 适用 | 哈希/测试输出/HTML/psql |
| PGM-001 治理层底座 | 适用（全中文段） | `seededDisplayNamesAreChinese`；金额精度/尾差/先成功等无本面。**GR-03** |
| PGM-008 员工和手机卡片 | 无本面 | 无注册/设备绑定测试 |
| PGM-022 空间和点位 | 无本面 | 无 |
| PGM-024 影响业务 | 无本面 | 无低消/容量 |
| PGM-026 商户主要功能 | 无本面 | 无商户档案 |
| PGM-032 员工和手机 h2 | 无本面 | 职级不是手机绑定 |
| PGM-033 员工主要功能 | 无本面 | 无注册双路径 |
| PGM-034 工作手机和企微 | 无本面 | `wecom.sync` 只作为高危码种子 |
| PGM-035 提成发放 | 无本面 | 无发放路径 |
| PGM-036 设备管理 | 无本面 | 无领用 |
| PGM-048 商户侧隔离 | 无本面 | 无跨商户读测试（AuthorizationService 跨组织闸在成功路径未测） |
| PGM-059 个人信息保护 | 适用（窄） | `pii.contact_phone.view` 须审批且不进日常桥；不测加密/脱敏界面 |
| PGM-066 已拍板 | 适用 | 最小颗粒度、权限点预置、全中文拍板；其余拍板（日结/第三方/下单时间）无本面 |

顺从式对照结论：映射项无「测试明文发明与 HTML 相反的职级档数/计算式」；**与 PGM-001 明文冲突的是展示名测试把 AI 挖掉**（GR-03）。DENY 优先公式的单元证据在 M036 `AuthorizationServiceTests.denyWinsWhenGrantAndDenyExistForSamePermission`（只读追证，不对本模块缺测立项）。

---

## 5. 对抗式（grilling 结构，自问自答）

❓ **Q1 — 控制器测试是不是把授权逻辑整棵换成桩，所以永远绿？**  
➡️ 授权 **实现** 是 mock，这是 WebMvcTest 的正确 seam。假绿只发生在**没有** `doThrow`/`verify` 的入口：listPermissions / listStaffLevels / assignStaffLevel 成功路径。createTemplate/assignRole/userView/overrides 拒绝路径会因漏调用门控而红。

❓ **Q2 — 把 AccessController 里三条门控删掉，本文件会不会红？**  
➡️ 不会。`requirePermission` 默认 no-op；成功路径只 stub `AccessService` 返回值。证据：测试源码无 `access.permission.view` / `access.staff_level.view` / `access.staff_level.manage` 字符串。

❓ **Q3 — 职级三十档测试会不会对「30 个连续 rank 但不是 M1–M10 上中下」假绿？**  
➡️ 会部分假绿：只钉 `hasSize(30)`、rank 1–30、两个 ID。把 M5 的 sub_level 改错只要 ID 仍在就会过。本轮 SELECT 显示库内确是 10×3，不是测试单独钉死的。未升为缺测立项（关键「30 档连续」已能红）。

❓ **Q4 — `staffLevelChangeRunsInOneTransaction…` 能不能为「套模板已提交、替换派生失败」变红？**  
➡️ 不能。只读 `TransactionAttributeSource`。四步写入在 `AccessService.assignStaffLevel`（L1326–1334 有岗位则走 `assignUserPositionDirect`）。测试名/注释过声称。GR-02。

❓ **Q5 — 全中文测试是不是为了让两行带 AI 的权限名过关而把闸挖了个洞？**  
➡️ 是。`replace(permission_name, 'AI', '')` 后：

```text
analytics.risk_alert.view | 查看经营分析风险预警和AI质量台账
wecom.ai_qc.manage        | 管理企微AI语义质检开关和适配器
```

PGM-001：「必须全部为中文，不得出现任何英文」；可不用中文的只有单据号、设备编号、二维码内容、系统内部标识符。权限**展示名**不是这四样。注释把「HTML 正文里的 AI」当成产品文案豁免，没有 HTML 依据。GR-03。

❓ **Q6 — 真库测试依赖执行顺序或未回滚残留吗？**  
➡️ 两个测试共用 grant UUID `2222…`。无 parallel 配置；类 `@Transactional`；测后 SELECT 夹具 ID 为 0。当前不成立顺序耦合。若将来打开并行，会撞主键——记 O-4，不立项（要两件不巧才发生）。

❓ **Q7 — 有没有永远为真的断言或被吞掉的异常？**  
➡️ `status.isIn(401,403)` 对受保护资源几乎总真，但 200/500 会红，不是恒真。`knownPermission` 吞 `RuntimeException` 是生产代码（M032），本测试文件无吞异常。非法 CHECK 用 `assertThatThrownBy`。

❓ **Q8 — 最终权限公式（角色+基础模板+临时模板+单独授予−拒绝）本文件有没有一条组合断言？**  
➡️ 没有单测把 GRANT 与 DENY 同时放在同一用户上再 `requirePermission`。有：模板同时存 grant/deny（只持久化）、USER_OVERRIDE deny 作为 effects 来源、到期后 effects 不含码。公式计算在 AuthorizationService，M036 已有 `denyWins…`。按「不为理论场景补需求 / 兄弟段已覆盖」**不立项**。

❓ **Q9 — 日常桥测试是不是测了未发布的「平台管理员看全量」？**  
➡️ 测的是 `accountType=platform_admin` 时日常码进入 `effectivePermissions().grants()`，不是 `currentMerchantScope()` 空范围。D-029 未发布的正是 scope 空范围与 master 判定。这几条日常桥在 V0.13 上绿；不能外推到「平台管理员可读全商户」。

❓ **Q10 — 33 个映射只测一小撮，要不要为每个入口补门控测试？**  
➡️ 否。未行使入口缺测不自动立项。本聚合关键且本文件**已经在跑**却不能为漏门控变红的，只有权限点列表 / 职级列表 / 职级分配（GR-01）。

---

## 6. Findings（临时 GR；正式 ID 待 B2）

### GR-01 — 已行使的权限点列表 / 职级列表 / 职级分配成功路径不能为漏门控变红

- **文件:行：** `AccessControllerTests.java:87-109`、`:205-219`、`:221-242`；对照 `AccessController.java:86-90`、`:256-260`、`:262-267`
- **原文（测试，无门控码）：**

```java
void listPermissionsReturnsApiResponsePageShape() {
    when(service.listPermissions()).thenReturn(new PageResponse<>(...));
    mockMvc.perform(get("/api/v1/access/permissions").with(user("ops").roles("P0_SMOKE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].permission_code").value("ops.production_readiness.view"));
}
```

```java
authorizationService.requirePermission("access.permission.view");   // AccessController.java:88
authorizationService.requirePermission("access.staff_level.view");  // :258
authorizationService.requirePermission("access.staff_level.manage"); // :267
```

本文件 `doThrow`/`requirePermission("…")` 只出现在 L71/L130/L194/L283，字符串集合不含上述三个码。

- **更长上下文：** 同文件 L67–85 对 `access.permission_template.manage` 的 `doThrow` 证明本切片**能够**把漏门控测红；这三条成功路径没有用同一手法。
- **正常触发 / 影响：** 无 `access.permission.view` / `access.staff_level.view|manage` 的账号走这三条已上线入口时，若控制器漏掉 `requirePermission`，本文件仍全绿。职级写入口是本聚合第一性使用面。
- **最小拟处置：** 在这三条各加 `verify(authorizationService).requirePermission("<对应码>")`，或各加一条与 L67–85 同构的 `doThrow` 用例。不铺 33 入口矩阵。
- **四分类：** 冲突？否（与 HTML 无相反规则）→ 细化？否（不是 HTML 下位业务规则，是测试是否锁门控）→ **技术性**（无业务发明）→ 非添加。
- **工程缺陷：** **成立**（GEN-003 能失败断言 + GEN-005 服务端授权；现有测试假绿，不是「缺一个理论入口」）
- **冻结 ID：** GEN-003、GEN-005、BSM-028、BSM-120
- **2.3 依据：** 类型 4（测试源码 grep + 控制器源码）及类型 2（39 测试在未改控制器的前提下全绿，不能证明门控被锁）

### GR-02 — `staffLevelChangeRunsInOneTransactionLikePositionAssignment` 声称四步写入，实际只读 `@Transactional` 元数据

- **文件:行：** `AccessJdbcIntegrationTests.java:661-671`
- **原文：**

```java
// M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
// 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
@Test
void staffLevelChangeRunsInOneTransactionLikePositionAssignment() throws NoSuchMethodException {
    assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
            "assignUserPosition", ...), AccessService.class)).isNotNull();
    assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
            "assignStaffLevel", ...), AccessService.class)).isNotNull();
}
```

- **更长上下文：** `AccessService.java:1298-1346` 在已定岗且职级变化时调用 `assignUserPositionDirect`。本测试零次调用该方法。去掉注解会红；四步中间失败不会红。
- **正常触发 / 影响：** 读测试的人会以为「已定岗改职级同一事务」已被真库锁住。实际只锁了注解存在。
- **最小拟处置：** 把测试名和注释改成只断言两个 public 方法带 `@Transactional`。四步路径留给已有 AccessService 测试（M036），本模块不新造。
- **四分类：** 冲突否 → 细化否 → **技术性** → 非添加。无业务发明。
- **工程缺陷：** **成立**（GEN-003 名称/注释场景 ≠ 断言场景；GEN-016 注释不可复现为四步证据）
- **冻结 ID：** GEN-003、GEN-016
- **2.3 依据：** 类型 4（测试与 AccessService 源码）

### GR-03 — `seededDisplayNamesAreChinese` 挖掉展示名中的「AI」，与 PGM-001「不得出现任何英文」冲突

- **文件:行：** `AccessJdbcIntegrationTests.java:673-686`
- **原文：**

```java
// M006-G-08（PGM-1.18「界面全中文」）：治理页 / 模板页直接上屏的权限名、角色名、模板名不含拉丁字母
// （HTML 正文自身使用的「AI」除外；permission_code / id 是机读标识，不在此列）。
SELECT permission_code FROM permissions
 WHERE replace(permission_name, 'AI', '') ~ '[A-Za-z]'
```

- **HTML 原文（冻结 PGM-001 / 同文 PGM-066 拍板）：**

> 平台所有终端……展示给用户的一切文字……**必须全部为中文，不得出现任何英文**……可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。

- **类型 4 库内实态（本测试放行的两行）：**

```text
analytics.risk_alert.view | 查看经营分析风险预警和AI质量台账
wecom.ai_qc.manage        | 管理企微AI语义质检开关和适配器
```

角色名、模板名无拉丁字母（同次 SELECT 0 行）。

- **正常触发 / 影响：** 权限点列表上屏即见拉丁字母「AI」。本测试按设计不会红。注释把需求 HTML 里出现「AI」误当成产品展示豁免；PGM-001 没有第五样豁免。
- **最小拟处置：** 删除 `replace(permission_name, 'AI', '')`，让本测试对上述两码变红。改展示名属于种子/迁移（M034 已闭环），本模块只修测试闸；B2 再定是否跨模块改名。冲突项按 2.1 **判定为删除该例外**。
- **四分类：** **冲突**（与 PGM-001 明文矛盾；先于细化/技术性/添加）
- **工程缺陷：** **成立**
- **冻结 ID：** PGM-001、PGM-066、GEN-003、GEN-015
- **2.3 依据：** 类型 1（PGM HTML 原文）+ 类型 4（psql 输出与测试 SQL）

---

## 7. 观察（不立项 / 工程缺陷不成立）

- **O-1** 未登录断言 `isIn(401,403)`。SecurityConfig L64–66 把无登录 403 当成已修缺陷。本轮 GlobalExceptionHandler 日志无该请求（走入口点），与 401 JSON 路径相符，但断言仍不能为回归到 403 变红。不单独立项（弱于 GR-01；未打印到具体 status 码）。
- **O-2** 控制器夹具 `wildcard=true` vs 实现恒 `false`。测的是字段可序列化。不立项。
- **O-3** 审计只读正则以 `manage|approve|export|…` 后缀为写操作近似，可能漏非后缀写码。种子现态下测试绿。不立项。
- **O-4** UUID `2222…` 跨两个测试；并行开启才会撞。不立项（提示词：要两三件不巧才成立 → 台账备注级）。
- **O-5** 映射 52 项不含 BEQR-149/151、PGM-040/041（权限公式与职级枚举正文）。不扩锁。
- **O-6** 派发「34 个映射」；源码 33。
- **O-7** `modules.md` 1166 行 vs 实算 1200。
- **O-8** DENY 覆盖 GRANT 的组合真库断言不在本文件；M036 已有内存测试。不补测。
- **O-9** 未行使的 20+ HTTP 入口缺门控/必填用例。抽样已覆盖幂等键与 validUntil 模式。不铺矩阵。
- **O-10** 英语 mock 展示名 `"View production readiness"` 仅夹具，真库中文由 GR-03 那条测。
- **O-11** ponytail-review：可删的是 GR-03 的 `replace(...,'AI','')`（1 行）和 GR-02 过声称注释。`net: -few lines`。其余真库断言不瘦。

---

## 8. 三线执行核对

| 线 | 是否整模块跑完 |
|---|---|
| 第一性原理 | 是，§3 |
| 顺从式 52 项 | 是，§4 逐项适用/无本面 |
| 对抗式 grilling | 是，§5 Q1–Q10 |

无未答 questions（B1 不发 2.2）。无 HTML 修改申请。未改计划/findings/ledger/源码/测试。未 fetch/commit/push/merge/deploy。未对数据库写（仅跑既有测试类，事务回滚；psql 仅 SELECT）。

---

## 9. 本席 B1 结论

**FAIL**

成立缺陷 3：GR-01（技术性，门控假绿）、GR-02（技术性，测试过声称）、GR-03（冲突，全中文测试挖 AI）。任一条即不得 PASS。39 测试绿灯不能覆盖这三条。

协作类版本脚注：日常桥/requirePermission 真库条在 **AuthorizationService.java 主工作区 V0.13** 上执行；仓储 expire/revoke/effects 在 **JdbcAccessRepository 主工作区未发布版** 上执行，其中与 origin/main 同口径的政策合并写、三十档、retired 人工放行不依赖 D-029 的 scope 闸。


---

# M035 B2-r1 整批归并复核（Grok 乙席）

- 席位：Grok 固定只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main 主工作区。
- 提示词：`160-audit-prompt.md` V0.17；runbook V0.02。已冻结判据不加判据。
- 输入（本席独立实算）：
  - 本文件上方 V0.01 B1 前缀 **38958** bytes，SHA256 `a03bf5ae91c906a9f4903ea9a0b5bf4a6d6bbf7751f42a38eae8f9c68228abdf`（FAIL：GR-01、GR-02、GR-03；观察 O-1…O-11）
  - `modules/M035-b1-self-raw.md` **仅 V0.01 前 24623 bytes**，SHA256 `732a60e07799ab70f67361654757edfe3491251df9a816b829357c5c8da7351a`（席甲 FAIL：CX-M035-001，OBS-1…4）。全文现 26035 bytes（SHA256 `23ce492c02729216b0875ff4a368daee3b32ee90e2092779560973f76857fa4f`）在该前缀之后另有追加段；本席按派发禁令 **未把该追加段当证据、未采信其票面**。
  - `findings.md` V3.08 全文件 SHA256 `adeec3a274b3ff35dd320f9e1106860c265054adae4c29e174e6226e2df39081`；只读顶部「当前 M035 B2-r1 整批候选」段。其下历史模块定稿段不作本批差集材料。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 B1 独占行同值）。**本段不写新的独占锁哈希字段行。**
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句（complex_test 仅 SELECT）；未用 MCP 或子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意；001–003 逐 ID 全 PASS；添加 0、不发 2.2；无 HTML 申请）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M035-001 | 本席 GR-03（席甲 B1 漏：把 `seededDisplayNamesAreChinese` 记成「有真库断言」／GEN-015 无本面，未拿 SQL 里的 `replace(…,'AI','')` 去对 PGM-001） | 冲突 / 是 / 是 | **同意入本模块队列**。配套前向迁移 V430 属 001 的最小完整处置，不是扩范围。`人工智能` 是同义直译，**不是业务发明，不必 2.2 定名** |
| M035-002 | 席甲 CX-M035-001 ＋ 本席 GR-01（同一主题：控制器门控未被测试锁住） | 技术性 / 是 / 是 | **同意**。未改写 GR-01 三条已行使成功路径的 verify 原意；并入席甲三处带分支／安全含义的门控是合取扩面，不是 33 入口矩阵 |
| M035-003 | 本席 GR-02（席甲 B1 记为 OBS-4） | 技术性 / 是 / 是 | **同意升队**。只改测试名与注释；不新造四步真库测试；不动 M006-G-07 两条注解断言 |
| XM-M035-A | 001 的跨模块登记：其它上屏面若仍含拉丁字母（含「AI」） | →各前端／文档模块 | **同意登记**；本模块 B3 不改前端／OpenAPI／三 HTML |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **同意；无漏并、无应升队项** |

冲突入队 1（001）。细化入队 0。技术性入队 2（002、003）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 3。本模块写入集合＝`AccessControllerTests.java`、`AccessJdbcIntegrationTests.java` ＋新增 `backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql` ＋检查点里隔离规格自动区块机械重生成 1 行（生成器拥有的「最新迁移」标记）。已应用迁移（含 V251／V429）不回改。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、M035-001（GR-03，冲突）＋配套 V430。票：同意。V430 属本条最小完整处置；`人工智能` 无业务发明之嫌；无更小且同样完整的改法；XM-M035-A 同意登记。**

独立核源（不重审模块）：

1. 测试闸。`AccessJdbcIntegrationTests.java:673-686` 注释写「HTML 正文自身使用的『AI』除外」；SQL 为 `WHERE replace(permission_name, 'AI', '') ~ '[A-Za-z]'`。角色名、模板名无此豁免。本席 B1 已指：这是为两行带「AI」的权限展示名挖洞，与 PGM-001 明文相反。
2. HTML／锁原文。冻结 PGM-001（`criteria-lock.md` 摘治理地图 L256 段，锁内 HTML 原文在约 L5034）：展示给用户的一切文字「必须全部为中文，不得出现任何英文」；可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。权限**展示名**不是这四样。`permission_code`（如 `wecom.ai_qc.manage`）是系统内部标识符，候选写「permission_code 不动」成立。PGM-066 拍板段（锁约 L6362）同文，候选未列不影响定性。
3. HTML 里的「AI」不是第五样豁免。治理地图 L532 一句：「P0 可引入本地 AI 大模型…但 AI 结果只作为“待人工确认建议”」——需求正文描述本地模型，不是权限列表／模板页上屏文案，不能反推产品展示名可留拉丁字母。
4. 库内终态（本票对 `complex_test` 只读 SELECT，未写）：`permissions.permission_name ~ 'AI'` **恰好 2 行**——`analytics.risk_alert.view`＝查看经营分析风险预警和AI质量台账、`wecom.ai_qc.manage`＝管理企微AI语义质检开关和适配器；挖掉「AI」后再查拉丁字母 0 行；角色名／模板名拉丁字母 0 行。与本席 B1 一致。已应用种子：V017／V096 英文原名，V251 L76／L120 写成上述两条中英混排；其后无迁移再改这两名。Flyway 最高成功版本 **429**。磁盘无 `V430__*.sql`，下号 430 正确。
5. 只改测试的后果。删掉 `replace(…,'AI','')` 后，本测试必对上述两码变红。主干留下一条已知红的真库测试，不是可发布状态。冲突项按 2.1 是「删除该例外」，但删除例外之后生产字典仍含拉丁字母，PGM-001 在权限列表页上屏面仍不成立。故「只修测试闸、改名留待跨模块」对本条不是完整处置。
6. 配套迁移是否最小。GEN-014：已应用的 V251 不回改；前向 UPDATE 展示名、不改码、不删行，与 M034 的 V429 前向修字典同形。候选 SQL：`UPDATE permissions SET permission_name = replace(permission_name, 'AI', '人工智能') WHERE permission_name ~ 'AI'`。现库命中闭集就是那两行，blanket 与按两码定点 UPDATE 对现值等价。无更小且同样完整的改法：①只删测试豁免→已知红；②按码把两行加入测试白名单→仍是第五样豁免，冲突未删；③测试事务内 UPDATE 再回滚→上屏终态不变；④回写 V251→违反 GEN-014。定点两码 UPDATE 不是「更小」，只是谓词写法不同，现库等价，B3 用候选 blanket 或两码定点均可，不作为否决。
7. 「人工智能」是不是业务发明／须不须 2.2。不是，不必提问。理由：①未新造权限、状态、金额、流程或展示面对象，只把已有展示名里的英文缩写换成中文；②「人工智能」是「AI」的词典对译，不改「质量台账／语义质检」的业务含义；③三 HTML 已在仓库摄像「事后须进行人工智能分析」等句使用同一中文词，不是本批发明的产品名；④PGM-001 要求全中文，完成 V251 未做完的直译是冲突收口，不是添加项。若事后要把用词收成更短的「智能」等，属发布后文案微调，不是本批入队或 2.2 条件。
8. 隔离规格 1 行。`tools/generate-data-isolation-spec.mjs:92-97` 自动区块头注写「当前解析到最新迁移 **${latest}**」，`latest` 取迁移目录排序最后一项。V430 不建表、不加 scope 列，表行数不变，只会改这一处版本标记。现规格仍写 **V428**（磁盘已有 V429，M034 B3 tree 实际未带该生成物，门禁 `verify:data-isolation-spec` 对最新号会红）。本批按检查点机械重生到 V430，是生成器拥有区域，禁止手改表行。非阻断订正：写「先例 M034」易读成 M034 B3 已经重生过——它没有；机制依据是生成器，不是那次 tree。不否决。
9. 其它上屏面。管理端路由用的是 `permission_code`（如 `analytics.risk_alert.view`），展示名走字典 API。前端／文档／契约里若另有拉丁「AI」文案，不在本两测试文件＋V430 的可观察闭集内。XM-M035-A 登记成立。不申请改三 HTML L532。

**明确同意**：001 冲突／是／是。测试去豁免 ＋ V430 同义中文改名 ＋ 规格自动区块 1 行，是相对 PGM-001 与「主干不得已知红」的最小完整处置。无添加、不发 2.2。

**二、M035-002（CX-M035-001 并 GR-01）。票：同意。归并未改写 GR-01 原意；边界合适；分类采技术性成立。**

独立核源：

1. GR-01 三条已行使成功路径仍无门控码字符串。`AccessControllerTests.java`：`listPermissionsReturnsApiResponsePageShape` L87-109、`listStaffLevelsReturnsApiResponsePageShape` L205-219、`assignStaffLevelReturnsOkWithAssignment` L221-242 只 stub `AccessService` 与 JSON 形状。控制器对应门控：`AccessController.java:88` `access.permission.view`、`:258` `access.staff_level.view`、`:267` `access.staff_level.manage`。本文件 `doThrow`／`requirePermission("…")` 只出现在建模板／派角色／覆盖拒绝／查他人有效权限；`verify(` 未 import。删掉这三处 `requirePermission`，这三条仍绿。候选「各加一句 `verify(authorizationService).requirePermission("<对应码>")`」＝本席 B1 最小拟处置原文，未改写。
2. 席甲 CX 三处带分支／安全含义的门控，本文件 0 引用（本席用测试目录检索复核，不是重审模块）：
   - 生命周期 `changePositionTemplateLifecycle` L192-202：`approve_delete`／`reject_delete` 走 `requirePermissions("access.role.assign", "governance.supervisor_review.approve")`，其余动作（含 archive）走 `requirePermission("access.role.assign")`。`requirePermissions` 在 access 测试目录 0 处。
   - 授权审批 `reviewGrant` L148-156：直接 `requirePermissions("access.grant_request.review", "governance.supervisor_review.approve")`。本文件无 `/grant-requests/{id}/review`。
   - 职级 L262-279、定岗 L281-299：用路径 `id` 重建请求，覆盖请求体 `userId`。记录类确有 `userId` 组件（`StaffLevelAssignmentRequest.java:6`、`UserPositionAssignmentRequest.java:6`）。现有职级成功路径不捕获入参；定岗端点本文件未行使。`ArgumentCaptor` 在本测试类 0 处。
3. `AuthorizationService.requirePermission` L36-38 转调 `requirePermissions`。控制器双码分支调用的是**复数方法**。B3 补测必须 `doThrow`／`verify` **`requirePermissions`**（双码），archive 单码分支 verify **`requirePermission`**；只 stub 单数方法锁不住双码。这是实施边界，候选「校验双权限／单权限」语义已覆盖，不否决。
4. 归并有没有改写原意。没有。GR-01 仍是「已行使三条成功路径加 verify、不铺 33 入口矩阵」。席甲三条是 GEN-003「关键分支」：if 双码／else 单码、双码审批、路径 id 覆盖——不是把其余无分支单权限入口做成样板矩阵。本席 B1 O-9 把未行使入口整批登记；候选从中只抽出这三处带分支的，与 O-9「其余无分支单权限不铺矩阵」同向。33 映射 − 已行使 10 − 本条新行使 3（lifecycle／review／position；staff-level 本已行使）＝其余 20，与候选观察段一致。
5. 分类。四分类顺序：冲突？否（实现与 HTML 不相反，缺的是测试锁）→ 细化？否（不是补一条 HTML 下位业务规则，被测代码不改）→ **技术性**（测试缺口，无业务发明）。席甲 B1 拟细化，上位句（删除配置须审批、授权按主管链、只做自己的职级／岗位）是旁证，用来说明这三处为什么算关键分支，不是本条在发明规则。同意采本席对 GR-01 的技术性，覆盖合并后的 002。
6. 边界。只改 `AccessControllerTests`；不改 `AccessController`／`AccessService`；不铺 33 入口矩阵；`reject_delete` 与 `approve_delete` 同一分支，测 approve_delete 即可。同意。

**明确同意**。

**三、M035-003（GR-02）。票：同意。**

`AccessJdbcIntegrationTests.java:661-671` 测试名 `staffLevelChangeRunsInOneTransactionLikePositionAssignment` 与注释声称「已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生）…同一事务」。断言只读 `TransactionAttributeSource` 上 `assignUserPosition`／`assignStaffLevel` 两个 public 方法的 `@Transactional` 元数据，零次调用被测方法。席甲 B1 记 OBS-4（「M006-G-07 既定写法」）并把 GEN-016 判通过——过声称不是「既定」能豁免的：GEN-003 名称／注释场景须等于断言场景，GEN-016 注释须可复现。升队正确。最小处置＝只改测试名与注释为「断言两个入口声明了事务」；不新造四步真库路径（行为在 M036）；两条 `assertThat(…getTransactionAttribute…).isNotNull()` 原样保留。同意。

**四、其余观察。票：全部只登记；无漏并须升队项。**

候选已覆盖：未登录 `isIn(401,403)`（O-1＝OBS-2，先例 M011）；控制器夹具 `wildcard=true` 对实现恒 false（O-2＝OBS-1）；其余 20 个无分支单权限未行使入口不铺矩阵（O-9＝OBS-3，三处带分支已入 002）；审计只读正则后缀近似（O-3）；UUID `2222…` 跨两个测试（O-4）；DENY 覆盖 GRANT 在 M036（O-8）；映射未含 BEQR-149/151、PGM-040/041，不扩锁（O-5）；`modules.md` 1166 vs 实算 1200（O-7）；派发「34 个映射」方法级 33（O-6）。

本席 O-10（控制器夹具英文展示名 `"View production readiness"` 不是界面文字）与席甲 GEN-015「测试内英文桩不是界面文字」同一事实，已含在观察口径，不必单列 ID。O-11 是 ponytail 过程注记（可删的豁免即 001 的 `replace`、003 的过声称注释），不进台账。D-029 协作类版本是环境脚注，不是本批缺陷。无应升队项。

**五、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 `M035*`。不申请改治理地图 L532。001 的中文写法不构成添加，不 BLOCKED-ASK。

---

### 二、分类理由

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **001 冲突。** PGM-001「不得出现任何英文／没有第五样」与测试把「AI」挖掉、字典两条展示名含拉丁字母「AI」不能同时成立。能指到 HTML 上位且相反 → 停在冲突。不是细化（不是把原则落成一条新的下位业务规则，而是删除与明文相反的例外）。不是技术性（例外本身就是与需求相反的产品文案闸）。不是添加（不是 HTML 没说、实现发明；改名是把已有含义写成中文）。同意采本席 B1 分类。
- **002 技术性。** 测试未锁已有门控／分支。无业务发明，不改被测代码。同意采本席对 GR-01 的分类覆盖合并项。
- **003 技术性。** 名称／注释过声称，无业务发明。

工程缺陷三条皆「是」。队列三条皆「是」（本模块写入集合内存在「改完后首次正常使用可观察行为变对、且不触碰已闭环模块、不回写已应用迁移」的改法：权限列表上屏不再出现「AI」、三条成功路径漏门控会红、双码／路径 id 回退会红、测试名与断言一致）。

冻结支撑：001 候选列 PGM-001／GEN-003／GEN-015，本席 B1 另列的 PGM-066／GEN-015 仍适用、不扩锁、不作为否决。002 候选列 GEN-003／GEN-005，本席 B1 另列 BSM-028／BSM-120 与席甲 BSM-003／BSM-114 作旁证、不扩锁。003 候选列 GEN-003／GEN-016，与本席 B1 一致。

---

### 三、最小修复边界

一次 B3 允许的写入集合＝两个测试文件 ＋ 新增 V430 ＋ 隔离规格自动区块机械 1 行。禁止改已应用迁移、被测 Java、前端、OpenAPI、三 HTML、锁、其它模块测试。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | `seededDisplayNamesAreChinese` 去掉 `replace(permission_name,'AI','')` 与注释豁免句，对任何拉丁字母都红。新增 `V430__permission_names_no_latin_ai.sql`：`permission_name` 内「AI」→「人工智能」（blanket 或两码定点，现值等价）；`permission_code` 不动。主工作区对 `complex_test` 跑既有真库类，Flyway 升到 430。`npm run docs:data-isolation-spec` 只改自动区块「最新迁移」标记 1 行 | 回写 V251／V017／V096；改 `permission_code`；把两码加入测试白名单；改三 HTML；手改规格表行；清授予表；把前端／契约拉丁字母当成本模块必改 |
| 002 | `AccessControllerTests` 补三条：lifecycle（`approve_delete` verify／stub **`requirePermissions` 双码**，`archive` verify **`requirePermission` 单码**）；`/grant-requests/{id}/review`（双码，被拒 403）；职级与定岗（请求体带别人的 `user_id`，`ArgumentCaptor` 断言服务层收到路径 id）。三条成功路径各加一句 `verify(authorizationService).requirePermission("<对应码>")` | 改 `AccessController`／`AccessService`；为其余 20 个无分支入口补 403 样板；只 stub 单数 `requirePermission` 冒充锁住双码 |
| 003 | 只改 `staffLevelChangeRunsInOneTransactionLikePositionAssignment` 的方法名与注释，如实写「断言 `assignUserPosition` 与 `assignStaffLevel` 声明了事务」 | 新造四步真库写入；删／改两条 `getTransactionAttribute` 断言；改 `AccessService.assignStaffLevel` |

B3 实施非否决提示（不改变本票）：双码测必须打在 `requirePermissions` 上；规格现文 V428、磁盘已有 V429，本批重生到 V430 会顺带对齐 M034 未带的那一处标记，仍只 1 行、仍属生成器拥有区域。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。001 的「人工智能」不是须操作员定名的产品名。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只改上表写入集合。

- 001：冲突／是／是。V430 是最小完整处置的生产一半，不是扩 scope。`人工智能` 同义直译，不发 2.2。XM-M035-A 同意登记。
- 002：技术性／是／是。GR-01 三条 verify 原意保留；CX 三处关键分支并入，不铺矩阵。
- 003：技术性／是／是。只改名与注释。
- 观察全部只登记，无升队。无添加、无 HTML 申请、无 questions。

### 结论

全批差集无原意改写、无漏并、无应升队观察。GR-03→001（席甲漏，采本席冲突）；CX-M035-001＋GR-01→002（技术性）；GR-02＝OBS-4→003。V430 属 001 最小处置，无更小完整改法，中文写法无业务发明。逐 ID：001 PASS、002 PASS、003 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M035 IN_PROGRESS；B3=0。

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **38958** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
