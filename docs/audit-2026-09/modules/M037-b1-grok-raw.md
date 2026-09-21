CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B1 席乙 Grok 独立审计报告

- 席位：乙（Grok 4.6 / reasoning xhigh）
- 步骤：B1 修复前全量审计（本模块首次开工，本版历史未审）
- 报告版本：V0.01（指定路径原先不存在，本次创建）
- 控制提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11（本席独立 `sha256sum`，见首行）
- 正本：`/home/james/complex-management` main 主工作区
- 互盲：未读取 `modules/M037-b1-self-raw.md`，未使用 MCP / 子代理 / 第三席
- 本席结论：**FAIL**（1 条工程缺陷 GR-01 成立于发布口径 origin/main 版，入拟修复队列；4 条仅未发布工作区版成立的 W 项按 D-029 不入队）

---

## 0. 技能预检（按环境如实）

本席在本机实际目录核过、本轮按收窄口径用过：

| 技能 | 路径核验 | 本轮用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` 存在 | 开工先核技能 |
| `using-arc` | `/home/james/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` 存在 | 用户指定 V0.17 双席只读优先于 Arc 默认并行 reviewer |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md`；CLI 可用 | skeleton 测试类 + AccessService + AuthorizationService；grep 主会话/权限；ask 授权判定与 approve 闸序 |
| `grilling` | `mattpocock-skills/.../productivity/grilling/SKILL.md` 存在 | 对抗线用其问答结构**自问自答**（B1 禁止等人，不向操作员发问） |
| `grill-me` | 同插件 `productivity/grill-me/SKILL.md` 存在 | 未启动交互式会话；结构并入 grilling 自问 |
| `ponytail` ultra | `ponytail/4.9.0/skills/ponytail/SKILL.md` 存在 | 不为理论路径补需求；缺测试只在本聚合关键分支且首次正常使用可复现时立项 |
| `systematic-debugging` | superpowers 存在 | 只借「读错误 → 复现 → 追数据流」。未改代码 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs/SKILL.md` 存在 | 只借「断言能否对目标症状变红」；未加诊断桩 |
| `codebase-design` | mattpocock 存在 | 用 seam/adapter 语言看 Mockito 仓储桩是否把被测逻辑换成测试自己的合成 |
| `verification-before-completion` | superpowers 存在 | 先实跑再下可执行结论 |
| `long-running-background-tasks` | grok bundled 存在 | Maven 均在 15s 内前台结束，未建 monitor |
| `writing-for-agents` | mattpocock 存在 | raw 按下游席可读结构写 |
| `i-have-adhd` | marketplace/cache 存在 | **未按该技能压缩全文**。V0.17 要求完整 raw，与该技能极简输出冲突，以提示词完整落盘为准 |
| `git-guardrails-claude-code` | 存在 | 可选；B1 只读未动 git 写操作，未调用本体 |

明确不用 / 缺口：

- Arc `audit` 默认派并行 reviewer：用户与 V0.17 禁止第三席与子代理，**未派**。
- `writing-plans` / `test-driven-development` / `tdd` / `requesting-code-review`：B1 只读，不适用。
- `batch-grill-me`：本环境技能目录**未核到**，记缺口，不冒充已调用。
- `ubiquitous-language`：本环境**未核到**，记缺口。
- `research`：V0.17 §6 已划掉。
- 未使用 MCP。
- 未读取 `modules/M037-b1-self-raw.md`。

graft 本轮 tokens saved ≈ 12,476 + 27,435 + 4,635 + 137,349 + 59,027 + 14,827 + 14,063 + 14,062 ≈ 283,874。

---

## 1. 环境事实（命令实算，非手抄）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar `plan/criteria-lock.sha256` 同行相等。ledger 顶部同值。本席只报实算，不改 ledger。

| 项 | 值 |
|---|---|
| 主工作区 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `40fcc8f1b3429d2874ccf856a404b214364decc7`（Merge PR #477 M036 检查点） |
| HEAD 相对 origin/main | `ahead 49, behind 52` |
| complex_test Flyway | `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1` → **430** |
| staff_levels | `SELECT count(*) FROM staff_levels` → **30**；`min/max(level_rank)` = 1 / 30 |

本模块 1 文件两版：

| 版本 | 行数 | SHA256 | git hash-object | `@Test` |
|---|---|---|---|---|
| 主工作区（含暂存） | 823 | `167733bc7c2b9fb07271779bb17d9447ccbefe6c5f7c56da3a6329b12c0b40b6` | `2d815f0e662115e30099fffb25ce2e0b6b88e29d` | 39 |
| origin/main | 809 | `c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3` | `c83e3c778a9916c449df2101105b011c73536f18` | 39 |

两版文件头均为 `// 文件版本：V0.30`，测试方法名集合相同。差异全在未发布工作流夹具（见 §2.3 / W 项）。

D-029：发布口径是 origin/main。两版都读；只在未发布版成立的记 W，不入修复队列。

被测追证（只读，不重开已闭环模块）：AccessService / AccessRepository（M031）、JdbcAccessRepository / AuthorizationService（M032）、DTO（M033）、权限迁移（M034）、同聚合第 1/2 段测试（M035、M036）。`SupervisorReviewService`、`StaffService` 属别的模块，只读。主工作区 `AuthorizationService.isMasterAuthority` 依赖 `SessionIssuer.MASTER_ACCOUNT_TYPE`（origin/main 的 `SessionIssuer.java` 无该常量）；origin `isMasterAuthority` 为 `platform_admin` 桥 + `MASTER_USER_ID` + `MASTER_USERNAME`。

---

## 2. 实跑（可执行产物，2.3 类型 2/4）

后端只从 `/etc/complex/complex.env` **逐行读取**三个键（未 `source` 该文件），URL 库名改为 `complex_test`。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`。本类无 `@SpringBootTest`，纯 JUnit + Mockito，不连库。psql 只对 `complex_test` 做 SELECT，无写语句。

### 2.1 发布口径：origin/main 一次性 worktree

```
git worktree add --detach /home/james/wt/m037-b1-grok-origin-main origin/main
# HEAD = 40fcc8f1b3429d2874ccf856a404b214364decc7
# origin 无 V427，本类不触发 Flyway，未拷迁移
mvn -f backend/pom.xml -o test -Dtest=com.complex.platform.access.M006SecurityRegressionTests
```

结果（exit 0）：

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  Total time: 9.503 s   # 含 javac
# 同 worktree 复跑 2.063 s，仍 39/39
```

未拷 V427。worktree 的 `backend/target` 将在本报告落盘后删除，`git worktree remove` 不用 `--force`。

说明：第一次跑在已 source 三变量的 shell 里完成。用 `bash -lc` 的 `env -i` 复跑时 login profile 把用户环境里已有的 `COMPLEX_CONFIG_DIR` / `COMPLEX_HOME` / `COMPLEX_LOG_DIR` 带进来——本类不读它们。再用 `bash --noprofile --norc` 的 `env -i` 时只剩三个 `COMPLEX_DATABASE_*`，但 Maven offline 解析 surefire 的 `plexus-utils:1.1` 失败（**插件解析失败，不是 39 个测试变红**），不折算业务 finding。发布口径以第一次 worktree 全绿为准。

### 2.2 未发布对照：主工作区 HEAD + 暂存 SUT（非发布口径）

同一测试类、主工作区未发布 `AccessService` / `AuthorizationService` / `SupervisorReviewService` / `SessionIssuer.MASTER_ACCOUNT_TYPE`：

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  Total time: 2.538 s
```

主工作区进程环境另有 `COMPLEX_CONFIG_DIR` / `COMPLEX_HOME` / `COMPLEX_LOG_DIR`（不是本席 source 出来的）。本类不连库，未观察到影响。按 D-029 分开写，不把未发布全绿当成发布口径。

### 2.3 两版 diff 摘要（`git diff origin/main -- <本文件>`）

未改测试方法名单。实质插入/替换：

1. `positionChangeDropsDerivedRole…` / `staffLevelChangeRederivesPositionGrants`：origin 用 `MASTER_USER_ID` + `platform_admin` 当模板管理员；工作区改为普通 `platform_admin` `template-admin`。
2. `topAuthorityReview…`：工作区补 `findSupervisorReviewSchedule` 夹具；把「职级 30 的普通管理员」失败从 `isInstanceOf(ApiException)` 收紧为 `MASTER_AUTHORITY_REQUIRED`；主会话 `accountType` 从 `"platform_admin"` 改为 `SessionIssuer.MASTER_ACCOUNT_TYPE`。
3. `positionTemplateLifecycle…`：工作区加普通员工 `archive` → `PLATFORM_ADMIN_REQUIRED`；`approve_delete` 收紧错误码；插入 `reject_delete` 后再重新申请；恢复/自审前后显式切回 platform_admin 与主会话。

这些依赖 origin 没有的常量/未发布 `SupervisorReviewService.approve` 的受益人闸，见 W 项。

---

## 3. 全文覆盖（不抽样）

范围文件只有这一份。主工作区 823 行 / origin 809 行均通读。夹具：`mock(AccessRepository)` 对接口 default 方法走 `CALLS_REAL_METHODS`，其余 `when`/`doAnswer` 写成内存 Map；`AuthorizationService` 与 `AccessService` 为真实 `new`（除两处故意 mock 的控制器门禁）。`@BeforeEach` 新实例，无 `PER_CLASS`，测试间不共享可变状态。

### 3.1 每个 `@Test`（行号：工作区 / 若与 origin 不同则另注 origin）

| 工作区行 | 测试 | 是否执行声称行为 | 断言能否红 | 备注 |
|---|---|---|---|---|
| 105–113 | administratorDenyWinsAndHighPermissionNeedsExplicitGrant | 是：DENY 后 `requirePermission`/`hasPermission`/`effectivePermissions`；再显式 GRANT HIGH | 能（DENY 不优先则红；HIGH 无授权却放行则红） | `assertDenied` 核 `PERMISSION_DENIED`。**两版同** |
| 116–120 | administratorKeepsDailyPermissionsWithoutAutomaticHighRole | 是：日常码走平台管理员日桥；`role_platform_admin` 的 HIGH 被 `isExplicitOrDailyGrant` 滤掉 | 能 | **两版同** |
| 124–136 | administratorHoldsGovernanceWritePermissionsFromDefaultRoleButNotBusinessHighRisk | 是：治理本职码随默认管理员角色；资金 HIGH 不行；再 DENY 治理码 | 能 | 注释引 BSM-1.3.4 / PGM-1.10.1；映射无 BSM-007/PGM-040，见顺从式。**两版同** |
| 139–143 | untrustedPrincipalIsRejected | 是：String principal → `currentActor` | 能（不抛则红） | 未核 `UNAUTHENTICATED`（O-1）。**两版同** |
| 146–151 | missingOrganizationCannotListAllUsersOrResolveGlobalProject | 是：`listAccessUsers` 走 `requireProjectScope`；`currentProjectScopeQuietly` 对无组织 staff 抛 | 能 | origin Quietly 对平台管理员返回 empty，本测试主体是 staff，两版都抛。未核错误码（O-1） |
| 154–165 | organizationReadWorksButCrossMerchantWriteDoesNot | 是：同组织读有效集；商户管理员跨商户 `assignUserRole` | 能：`never().assignUserRole` 在真写入时红 | 先 `requireSameOrganization` 再 `requireActorCanGrant`；当前会到 `CROSS_MERCHANT_FORBIDDEN`。未核码（O-1）。**两版同** |
| 168–173 | collidingUserSlugsRemainDifferentUsers | 是：两次 `createAccessUser` | 能 | **两版同** |
| 176–183 | templateEditCannotGrantAnUnownedHighPermission | 是：真实 `create`/`update` | 能：子项仍为 LOW | 未核码（O-1） |
| 186–194 | templateEditPreservesOwnerAndRejectsMissingTemplate | 是 | 能 | 未核码（O-1） |
| 197–207 | emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite | 是：真实 `createTemporaryGrant` 校验 | 能：`never().createTemporaryGrant` | 未核码（O-1） |
| 210–217 | invalidStatusAndBlankPasswordHaveNoWrites | 是 | 能：`never().createUser` + `verifyNoInteractions(credentials)` | 未核码（O-1） |
| 220–224 | manualAccountDoesNotPersistOrReturnPhoneDigest | 是：`createAccessUser` 返回 `phoneHash()==null` | 能 | **两版同** |
| 227–229 | administratorWithoutApprovalRoleHasNoSupervisorLevel | 是：`currentSupervisorLevel` | 能（非 0 则红） | **两版同** |
| 232–248 | twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass | 是：真实 `AccessController`+`AuthorizationService.requirePermissions`；`AccessService` 故意 mock | 能：缺第二码时 `consumed` 空；双码后只消耗一次 source | 桩没有替换授权核验。**两版同** |
| 251–264 | credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword | 是：真实 `JdbcAccessCredentialProvisioner` + mock JDBC | 能：status=disabled；空白密码后 `verifyNoMoreInteractions` | **两版同** |
| 267–273 | developmentCredentialAdapterRejectsMissingAccount | 是：真实 `DevAccessCredentialProvisioner` | 能：`verifyNoInteractions(auth)` | **两版同** |
| 276–291 | changedRoleCannotBeAppliedUnderAnOldApproval | 是：真实 apply/review + 指纹 | 能：核 `GRANT_SNAPSHOT_CHANGED`；`never().approve` | **两版同** |
| 294–307 | beneficiaryCannotApproveOwnGrant | 是 | 能：核 `BENEFICIARY_SELF_REVIEW_FORBIDDEN` | **两版同** |
| 325–355 | positionChangeDropsDerivedRoleButKeepsIndependentAssignment | 是：真实 `assignUserPosition` → `replacePositionGrants` | 对「仓储被写成新角色」能红；对 `AuthorizationService.effectivePermissions` **不能**（O-2） | origin 用 MASTER 会话建模板；工作区用 `template-admin`（W-4） |
| 359–372 | appliedTemplateCannotExtendTimeOrRemoveSingleUse | 是：真实 update | 能 | 未核码（O-1）；缩权成功路径有断言 |
| 375–383 | pendingTemporaryGrantRejectsInvertedTimeWindow | 是：`applyGrant` 校验先于复核 | 能：`never().blockForAdmission` | **两版同** |
| 386–421 | topAuthorityReviewRequiresMasterSessionRatherThanRankAlone | 是：真实 `SupervisorReviewService.approve` | origin：职级 30 失败只 `isInstanceOf(ApiException)` + `never().saveSupervisorReview`——当前 origin 闸序会到 `MASTER_AUTHORITY_REQUIRED`，never 能锁误写入。工作区收紧错误码并加排期夹具（W-1、W-2） | 被测属 SupervisorReview 模块，本文件只读追证 |
| 425–433 | manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval | 是：五种员工类型 | 能：`never().createUser` | 未核 `STAFF_REGISTRATION_REQUIRED`（O-1） |
| 436–441 | accountJsonDoesNotExposeStoredPhoneDigest | 是：Jackson 序列化 `AccessUser`（`@JsonIgnore phoneHash`） | 能 | **两版同** |
| 444–451 | missingCredentialAdapterCannotReportSuccessfulCreation | 是：`credentialProvisioner=null` | 能：`never().createUser` | **两版同** |
| 456–477 | employeeApprovalWithoutCredentialAdapterWritesNoAccount | 是：真实 `StaffService.reviewRegistration`；反射注入 `registrations` | 能：`never().createUser` | 夹具与生产 `@Autowired` 字段一致（O-5）。Staff 属别模块 |
| 480–514 | registrationCreatesAccountOnlyAfterCompanyApproval | 是：提交不建账号；商户自批失败；公司批后建账号+`role_staff_self`+凭证 | 能 | **两版同** |
| 518–521 | summaryQueryCannotOverrideBusinessThresholds | 是：`grantSummary(null, MAX, null)` 先 `grantPolicy()` 再拒绝覆盖 | 能（不抛则红） | 未核 `QUERY_POLICY_OVERRIDE_FORBIDDEN`（O-1）。桩 JSON 无 `deadlineWorkingDays`，两版实跑仍绿 → 现行记录类可空 |
| 524–531 | referenceDictionaryDoesNotPretendItWasTruncatedAtFifty | 是：`listPermissions` 55 条 | 能：`pageSize==55` | **两版同** |
| 534–539 | grantListHasADefaultTimeWindow | 是：`listGrantRequests(null)` 把 cutoff 传给仓储 | 能：null cutoff 则红 | **不断言约 30 天**（O-3）。接口 default `listGrantRequests` 返回空列表，verify 仍捕获参数 |
| 543–550 | directPositionTemplateDeleteCannotBypassArchiveAndApproval | 是：`deletePositionRoleTemplate` 恒抛 | 能：`never().deletePositionRoleTemplate` | 未核 `ARCHIVE_APPROVAL_REQUIRED`（O-1） |
| 554–624 / origin 553–608 | positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | 部分：存档/占用/申请/主会话删除/恢复/自审免复核都跑了 | origin：**不能**锁「非平台管理员不能存档」和 `reject_delete`（GR-01）；工作区补了这两支 | 见 GR-01 / W-3 |
| 671–695 | companyRequestsHaveIndependentResultsAndRetryOnlyOnce | 是：真实控制器 + `IdempotencyService` 按组织分钥 | 能 | **两版同** |
| 699–716 | invalidAccountTypeResponseIsChinese | 是：控制器 `platform_admin` 开户失败；反射 `GlobalExceptionHandler.handleApiException` | 能：码 + 中文 + 无拉丁 | 反射的是生产 `@ExceptionHandler` 同一方法 |
| 722–744 | temporaryGrantMissingValidUntilAndUnauthenticatedMessagesAreChinese | 是：控制器缺失效时间（AuthorizationService 故意 mock 放行门禁）；未登录走真实 `currentActor` | 能 | **两版同** |
| 746–756 | invalidDecisionMessageIsChinese | 反射 `AccessService.normalizeDecision` | 能 | 生产 `reviewGrant` L979 调用同一私有方法（O-4） |
| 761–774 | platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission | 是：真实 `effectivePermissions`/`requirePermission` + 计数 | 能：日常条数或调用次数超 2 则红 | **两版同** |
| 778–785 | orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed | 是：`copyPermissionTemplate` | 能：核 `PROJECT_SCOPE_REQUIRED` | **两版同** |
| 790–821 | staffLevelChangeRederivesPositionGrants | 是：真实 `assignStaffLevel` → `assignUserPositionDirect` → `replacePositionGrants` | 同岗位测试：锁仓储指针，不锁有效集计算（O-2） | origin 用 MASTER 建模板（W-4） |

辅助方法（626–667、638–654）：`account`/`template`/`user`/`actor`/`effect`/`assertDenied`/`reviews`/`withRegistrations` 已读，无独立 `@Test`。

---

## 4. 顺从式：52 项映射（定义全文已读，HTML 原文已对照锁内摘录并抽查三 HTML 文件）

映射 ID 实数 52、无重复。锁定义区「须与锚点原文一致」对**测试模块**解释为：本文件声称/锁住的行为不得与该锚点冲突；锚点业务若本文件无对应测试面则记无本面，不为无本面补测（ponytail / 提示词「不为理论场景补需求」）。

抽查三 HTML 原文命中：BEQR-157「待入职、在职…」；BSM-120「职级可按 M1-M10」；PGM-001「必须全部为中文」；PGM-066「员工权限按单一功能最小颗粒度」；BSM「拥有系统里全部日常功能」（BSM-007 正文，**未映射**）。

| ID | 适用? | 理由 |
|---|---|---|
| BEQR-157 | 无本面（枚举）/ 部分适用（账号状态） | HTML 是员工人事五态。本文件测的是账号 `active`/`invited`/`typo`/`disabled`，不实现也不声称那五态。不冲突 |
| BEQR-182 | 无本面 | 考勤日结果。本文件无排班/打卡断言 |
| BEQR-183 | 无本面 | 打卡方式 |
| BEQR-184 | 无本面 | 请假类型 |
| BEQR-185 | 无本面 | 请假单状态 |
| BEQR-186 | 无本面 | 薪资单构成 |
| BEQR-187 | 无本面 | 提成发放。注册测的是账号开通不是发薪 |
| BSM-001 | 适用（背景） | 「谁能操作」是基础盘子。本文件锁权限/范围/最高权限，不发明综合体经营规则 |
| BSM-003 | 适用 | 父锚「权限分岗」。DENY 优先、日常/高危分界、跨商户、生命周期审批均落在此 |
| BSM-009 | 适用 | 基础管理含员工与权限 |
| BSM-028 | 适用 | 「管理员工、岗位、班次、职级、权限、…员工注册审批」——定岗、改职级、注册、模板是本文件主面。三十档枚举在库（psql=30）不在本 mock 夹具（O-6） |
| BSM-029 | 适用（部分） | 「用户权限要知道…职级和审批状态」。有职级 30 不能代替 USB、注册审批；无设备绑定测试 |
| BSM-073 | 无本面 | 商户专区账单/工单/对账。跨商户隔离归 PGM-048/BSM-114，不在本锚点的商户自助五项 |
| BSM-084 | 适用（部分） | 「审批通过的员工都自动获得员工自助」——`registrationCreatesAccount…` verify `role_staff_self`。排班考勤无本面 |
| BSM-085 | 适用（部分） | 注册两条路径：商户代提交 + 公司批。本文件覆盖商户代提交/商户不得自批/公司批。私人手机自助注册无本面 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资 |
| BSM-088 | 无本面 | 提成/陪场边界 |
| BSM-114 | 适用 | 不同人只做该做的：跨商户、缺组织、平台管理员 vs 最高权限、员工须登记 |
| BSM-115 | 适用（背景） | 「基础管理连所有业务」含权限卡住。本文件不测票码/酒吧连接 |
| BSM-120 | 适用（部分） | 「职级可按 M1-M10，并可分上中下」——测试用 rank 1/4/30，不枚举 30 行（O-6）；「员工权限按单一功能最小颗粒度」——LOW/HIGH 分码，适用 |
| GEN-001 | 适用 | 安全回归有用途；无纯转发空类 |
| GEN-002 | 适用 | 入口与断言已实跑 39/39 |
| GEN-003 | 适用 | **GR-01**：origin 生命周期测试不能使 `requirePlatformAdmin` / `reject_delete` 变红 |
| GEN-004 | 适用 | 状态/密码/时间窗/账号类型在写入前校验 |
| GEN-005 | 适用 | 服务端授权、默认拒绝、DENY 优先、主会话 vs 职级 |
| GEN-006 | 适用（部分） | 指纹/快照、占用检查；无真事务（单元测试） |
| GEN-007 | 适用（部分） | 幂等按组织分钥；无并发测试，不立项 |
| GEN-008 | 适用 | 错误码/中文/账号类型与服务层一致 |
| GEN-009 | 适用 | 缺适配器/空白密码/未知主体失败而非伪成功 |
| GEN-010 | 适用 | 手机号不入 JSON；不测加密算法（仓储层） |
| GEN-011 | 适用（部分） | 复核消耗、生命周期失败留痕在服务层；本文件主要 verify 写入/不写入 |
| GEN-012 | 适用 | 平台管理员有效集字典 ≤2 次 |
| GEN-013 | 适用 | 单测不依赖本机库；三变量约束已遵守（本类不连库） |
| GEN-014 | 无本面 | 无迁移文件 |
| GEN-015 | 无本面 | 无 UI |
| GEN-016 | 适用 | 注释 M006 编号与断言场景对照见 §3；无把计划写成现状 |
| GEN-017 | 适用 | 本文件是 HTML 权限规则的下位测试（细化），不是业务发明 |
| GEN-018 | 适用 | 本报告依据 HTML / 实跑 / 命令输出 |
| PGM-001 | 适用 | 资格/权限底座；界面全中文——中文报错三测 |
| PGM-008 | 适用（卡片） | 员工注册/岗位状态；企微/工作手机无本面 |
| PGM-022 | 无本面 | 空间点位 |
| PGM-024 | 无本面 | 低消/容量/库位 |
| PGM-026 | 无本面（主体档案）/ 部分（注册 merchant） | 商户代提交用了 merchant 夹具，不实现四类主体绑定 |
| PGM-032 | 适用（部分） | 「审核是否通过」——登记审批。设备标识无本面 |
| PGM-033 | 适用（部分） | 注册两条路径，同 BSM-085 |
| PGM-034 | 无本面 | 企微会话 |
| PGM-035 | 无本面 | 提成发放 |
| PGM-036 | 无本面 | 设备领用 |
| PGM-048 | 适用 | 商户不能跨商户/看全局——`organizationReadWorksButCrossMerchantWriteDoesNot` |
| PGM-059 | 适用（部分） | 界面不见明文手机号——`phoneHash` 忽略。不测加密存放（仓储） |
| PGM-066 | 适用 | 单一功能颗粒度、全中文、权限点预置（字典 55 条不分页截断） |

未映射但测试注释引用的 BSM-007 / PGM-040（最终权限公式、岗位模板重算）：不作为本模块冻结判据扩写；行为已由映射的 BSM-003/028/PGM-066 与 GEN-003 覆盖。不 LOCK-AMEND。

---

## 5. 第一性原理三问

**Q1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

`M006SecurityRegressionTests` 是「功能权限点、职级、模板、最终权限」在后台的安全回归网。删掉它：

- BSM-003 / BSM-114 / PGM-066：DENY 优先、日常自动授予 vs 高危须显式授、跨商户、单一功能码，没有本切片的失败断言。
- PGM-001 全中文：控制器/异常处理中文没有本切片锁。
- BSM-004 虽未映射，删除入口与存档审批在本文件有对应测试（M006-026）。
- 员工注册须公司批、手工入口只留外包：BSM-085 / PGM-033。

答得上 → 有业务上位。文件本身是细化（测试），不是添加。

**Q2 声称的行为有没有被实际执行验证过？**

有。类型 2：origin worktree 39/39 绿；主工作区未发布 39/39 绿。类型 4：全文 + SUT 对照。例外：岗位两条断言读的是测试自己写的 `findPermissionEffects` 合成（O-2）；origin 生命周期声称覆盖存档审批，但非管理员存档与 `reject_delete` 未执行（GR-01）。

**Q3 依赖的前提哪一条没验证就当真？**

1. Mockito `findPermissionEffects` 合成 ≡ `JdbcAccessRepository` SQL（M036 已在同聚合第 2 段补真库/有效集；本文件仍用桩合成，O-2）。
2. origin `isMasterAuthority` ≡ `platform_admin`+主用户 ID；工作区 ≡ `MASTER_ACCOUNT_TYPE`（W-1）。
3. 反射 `normalizeDecision` / `GlobalExceptionHandler.handleApiException` / `StaffService.registrations` 等于生产装配（前两者有源码同方法；后者有 `@Autowired` 字段，O-4/O-5）。
4. `listGrantRequests` 非 null cutoff ≡「配置窗口」而非任意过去时刻（O-3）。
5. origin 生命周期测试从 `setUp` 的 platform_admin 起手，把 `requirePlatformAdmin` 当成已经锁住（**不成立**，GR-01）。

---

## 6. 对抗式（grilling 结构自问自答，不等操作员）

❓ **Q1 改坏 `requirePlatformAdmin`，origin 这份测试会红吗？**

➡️ 不会。origin `positionTemplateLifecycle…` L585 直接 `archive`，此时 `setUp` 已是 `platform_admin`。去掉 AccessService L500–502 的平台管理员闸，origin 39 个测试仍可绿。工作区 L586–590 用 `ordinary-staff` 锁 `PLATFORM_ADMIN_REQUIRED`，该断言不依赖 `MASTER_ACCOUNT_TYPE`。**此为发布口径工程缺陷。**

❓ **Q2 Mockito 有没有把授权逻辑整个换成桩返回值？**

➡️ `twoPermissionReview` mock 的是 `AccessService.reviewGrant`，核验在真实 `AuthorizationService.requirePermissions`，consumed 来自 `markSingleUsePermissionConsumed` 的 sourceId，不是测试手写的「已通过」。`temporaryGrant…Chinese` mock 门禁只为看校验文案，未登录走真实 `currentActor`。岗位两条 `assertThat(repository.findPermissionEffects…)` 断言的是测试自己写的 default answer 合成（O-2），不是 `effectivePermissions()`。

❓ **Q3 只断言 `ApiException` 会不会假绿？**

➡️ 会弱、不一定假绿。跨商户有 `never().assignUserRole`；空时间窗有 `never().createTemporaryGrant`；DELETE 有 `never().delete…`。origin 顶层审批有 `never().saveSupervisorReview`。这些写入闸是能红的。缺的是错误码（O-1）。origin 生命周期对 `approve_delete` 也只 `isInstanceOf(ApiException)`，当前 origin `resolveDecidableNode` 会抛 `MASTER_AUTHORITY_REQUIRED`，但若改成别的 ApiException 仍绿——工作区已收紧；只在未发布版成立，记 W-3 的一部分，不单独当 origin 假绿（写入仍被后续 `deletedAt` 与主会话成功路径约束）。

❓ **Q4 `verify never` 真能证明未写入吗？**

➡️ 对已 stub 成 `doAnswer` 记账的方法：never 表示该方法没被调用。`createUser`/`createTemporaryGrant`/`assignUserRole`/`deletePositionRoleTemplate`/`approve`/`blockForAdmission` 如此。`CALLS_REAL_METHODS` 的 default 若被调用会走接口 default（如 `listGrantRequests` 返回空），不是 never。

❓ **Q5 测试名/M006 编号是否等于实际场景？**

➡️ 对照过。M006-001 DENY/日常/高危、M006-014 双权限一次消耗、M006-006 快照、M006-007 受益人自审、M006-026 删除入口、M006-011 登记审批、M006-032/034 中文，名实相符。M006-026 注释写「存档→申请→最高权限复核→软删除→恢复；占用和自审均被拒绝」——origin **没有**「普通员工不能存档」「reject_delete」这两句的执行（GR-01）。

❓ **Q6 反射/字段注入与生产装配一致吗？**

➡️ `StaffService.registrations` 生产是 `@Autowired` 字段，测试 `setAccessible` 注入同一类型，一致（O-5）。`normalizeDecision` 是 `reviewGrant` 私有步骤。`handleApiException` 是全局处理器私有实现。不另立项。

❓ **Q7 测试间顺序/共享状态？**

➡️ 无 `TestInstance(PER_CLASS)`。Map 是实例字段，每方法新实例。`SecurityContextHolder` `@AfterEach` 清。`reviews()` 重赋 `service` 只在当次测试。

❓ **Q8 缺「三十档列表」要立项吗？**

➡️ 不要。库内 30 档已由 psql 证实，属迁移/种子（M034）与 M035/M036 夹具观察。本文件是安全回归 mock，不是字典完整性测试。首次正常使用「列出职级」不走本类。

❓ **Q9 未发布 BENEFICIARY_REQUIRED 会不会让 origin 顶层审批假绿？**

➡️ origin `approve` **没有**「缺排期就 BENEFICIARY_REQUIRED」。工作区有，所以必须补 `findSupervisorReviewSchedule`（W-2）。origin 职级 30 管理员走到 `resolveDecidableNode` 的 MASTER 闸。不把未发布闸套到 origin。

---

## 7. Findings

### GR-01 — origin 岗位模板生命周期测试锁不住「非平台管理员存档」和 `reject_delete`

- **成立版本：origin/main（发布口径）。** 主工作区未发布版已加对应断言，故本条**不是** W 项。
- 文件:行：
  - origin 测试 `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java:553–608`（尤其 L585 起手 `archive`，无 staff 主体；无 `reject_delete` 调用）
  - 工作区对照 L586–590（staff → `PLATFORM_ADMIN_REQUIRED`）、L605–607（`reject_delete`）
  - SUT（origin 与工作区均有）：`AccessService.changePositionTemplateLifecycleAudited` L500–502 `requirePlatformAdmin`；`case "approve_delete", "reject_delete"`
- 原文（origin L552–585 上下文）：

```
    // M006-026：存档→申请→最高权限复核→软删除→恢复；占用和自审均被拒绝。
    @Test void positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval() {
        ...
        service = new AccessService(...);
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
```

`setUp` L99 `actor("admin", "platform_admin", "org", null)`，故 L585 从未离开平台管理员。

- 正常触发 / 影响：公司员工（`accountType=staff`）调用存档，生产抛 `PLATFORM_ADMIN_REQUIRED`。把 `requirePlatformAdmin` 删掉或让 staff 也能 archive：origin 本类 39 个测试仍绿。`reject_delete` 同理——SUT 有分支，origin 测试从不调用；删掉该 case origin 仍绿。这是本聚合「配置删除走审批」的首次正常使用路径，不是理论。
- 最小拟处置：在**发布口径**测试（origin 夹具：主会话仍用 `MASTER_USER_ID` + `"platform_admin"`，不要引入 `MASTER_ACCOUNT_TYPE`）中：① `actor(staff)` 后 `archive` 断言 `PLATFORM_ADMIN_REQUIRED` 且 `never().savePositionTemplateLifecycle`；② 申请删除后主会话 `reject_delete`，断言未软删且可再次 `request_delete`。工作区已有近似断言，检查点须做成 origin 可编译变体。
- 四分类：冲突？否（与 HTML 权限分岗不矛盾，是测试缺口）。细化？是（BSM-003/028、PGM 配置删除的下位测试）。技术性？测试技术缺口，上位仍是权限规则。添加？否。**顺序命中：细化。**
- 工程缺陷：成立（GEN-003：断言不能使目标分支失败）。
- 冻结 ID：GEN-003（辅 BSM-003、BSM-028、GEN-005）。
- 第一性依据：类型 4（origin 测试原文 + origin/工作区 SUT L500–502）+ 类型 2（origin worktree 39 绿，说明现状测试通过但未锁该闸）+ 类型 1（BSM-003「权限分岗」；BSM-114「只做自己该做的」）。全仓测试检索 `PLATFORM_ADMIN_REQUIRED` / `reject_delete` 仅本文件工作区版命中，M035/M036 未代锁。

### W 项（仅未发布工作区成立，不入修复队列）

| ID | 内容 | 为何不入队 |
|---|---|---|
| W-1 | 主会话 `accountType` 改为 `MASTER_ACCOUNT_TYPE`；SUT `isMasterAuthority` 同步改。origin 仍用 `platform_admin`+`MASTER_USER_ID` | 未发布工作流；origin 无该常量，不能随本模块单独发布 |
| W-2 | `topAuthorityReview` 补 `findSupervisorReviewSchedule`。工作区 `SupervisorReviewService.approve` 对授权类缺受益人抛 `BENEFICIARY_REQUIRED`（M036 记过未发布 6 ERROR） | 只在未发布 approve 成立；origin approve 无此闸，origin 测试不加夹具仍 39 绿 |
| W-3 | 生命周期 `approve_delete` 收紧为 `MASTER_AUTHORITY_REQUIRED`；主会话切换用 `MASTER_ACCOUNT_TYPE` | 错误码收紧可随 GR-01 用 origin 夹具做；常量本身是未发布 |
| W-4 | 建岗位模板的 actor 从 `MASTER_USER_ID` 改为 `template-admin` | 适配 W-1：未发布后 MASTER_USER_ID+platform_admin 不再是主会话。origin 仍把该组合当主会话，旧夹具合法 |

未发布对照 39/39 绿，只说明「未发布测试 + 未发布 SUT」自洽，不能证明 origin 已锁 GR-01。

---

## 8. 观察（不入队）

- **O-1** 多处只 `isInstanceOf(ApiException)`。有 `verify never` 的写入路径仍能红。不单独立项。
- **O-2** 定岗/改职级断言 `repository.findPermissionEffects`（测试合成），不经 `AuthorizationService.effectivePermissions`。同聚合 M036 已补有效集/真库。无新证据显示本桩让「岗位派生进有效集」假绿到可首次复现的程度（本桩**会**把 `positionRoles` 并入 effects）。不重复立项。
- **O-3** `grantListHasADefaultTimeWindow` 只断言 cutoff 非 null，不锁 30 天。null 无界已被锁。不立项。
- **O-4** `invalidDecisionMessageIsChinese` 反射私有方法；`reviewGrant` L979 同方法。
- **O-5** `withRegistrations` 反射字段，与 `StaffService` `@Autowired registrations` 一致。
- **O-6** 本 mock 不列出 30 档；库内 30 档由 SELECT 证实，属别模块。
- **O-7** XM-M036-A：origin 全量后端 3471 中 20 红属别的模块。本类 39 绿，比较时减去该基线。
- **O-8** 映射无 BSM-007 / PGM-040 / BEQR-151，测试注释引用了最终权限公式与「管理员日常权限」。不扩锁。

---

## 9. 模块结论

| 项 | 值 |
|---|---|
| 全文覆盖 | 是（1 文件两版） |
| 52 判据 | 逐项记录 |
| 三线 | 已跑 |
| 发布口径实跑 | origin worktree 39/39 绿 |
| 未发布对照 | 主工作区 39/39 绿（分开写） |
| 工程缺陷 | GR-01 成立于 origin/main |
| 添加项 | 0 |
| HTML 修改申请 | 无 |
| 本席票 | **FAIL** |

下一步（席甲调度，本席不修）：B2 归并本 raw 与 self raw；GR-01 拟入队。本席 B1 结束。


# M037 B2-r1 整批归并复核（Grok 乙席）

- 席位：Grok 固定只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main 主工作区。
- 提示词：`160-audit-prompt.md` V0.17。已冻结判据不加判据。
- 输入（本席独立实算）：
  - 本文件上方 V0.01 B1 前缀 **34828** bytes，SHA256 `57ce8fc531d664a542b2e7151e6056da90d0a62ade15bb5938a184d136b98ca8`（FAIL：GR-01，W-1…W-4，观察 O-1…O-8）
  - `modules/M037-b1-self-raw.md` **仅 V0.01 前 37297 bytes**，SHA256 `a6a0dd618c7816f0ca0c89772891dd7a178ea82f81e83770b48e0a727c87bfd8`（席甲 FAIL：CX-M037-001①②，OBS-1…3、W-M037-1、XM-M037-A）。全文现 38450 bytes（SHA256 `c3307cf8668fce77a4af5556dfc62844711fbf06180cb6acdfce7295fd309182`）在该前缀之后另有追加段；本席按派发禁令 **未把该追加段当证据、未采信其票面**。V0.01 截断点由前缀 SHA 命中确认。
  - `findings.md` V3.14 全文件 SHA256 `e80c7dc97cdec168331535d9df9c1f7c3fd5515efa1fcac4e46ff21a21495941`；只读顶部「当前 M037 B2-r1 整批候选」段。其下历史模块定稿段不作本批差集材料。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 B1 独占行同值）。**本段不写新的独占锁哈希字段行。**
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句（本票只读已有日志与源码，未跑 Maven、未 psql 写）；未用 MCP 或子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意；001–002 逐 ID 全 PASS；添加 0、不发 2.2；无 HTML 申请）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M037-001 | 席甲 CX-M037-001①（本席 B1 未报；O-1 把多处只断言 `ApiException` 整体记为观察） | 技术性 / 是 / 是 | **同意入本模块队列**。证据独立成立；属本聚合「权限模板」测试面 `updatePermissionTemplate` 两道闸的关键未锁分支 |
| M037-002 | 本席 GR-01（①非平台管理员存档、②`reject_delete`）＋席甲 CX-M037-001②（③普通管理员批准删除被不得自审顶替）；采本席细化 | 细化 / 是 / 是 | **同意**。未改写 GR-01 两处原意；第三处并入合适（本席 W-3 已写「错误码收紧可随 GR-01 做」） |
| W-M037-1 | 本席 W-1＋W-3 余下常量部分＋W-4 ＝ 席甲 W-M037-1 拆出未发布常量后的残余 | D-029 只登记不入队 | **同意** |
| W-M037-2 | 本席 W-2（与 W-M036-3 同源） | D-029 只登记不入队 | **同意** |
| XM-M037-A | 席甲 §6：`actor_can_grant_open` 只红 AccessServiceTests 两条 | →已闭环 M036，待操作员决定是否重开 | **同意登记**；不在本模块文件补 |
| （无新 ID） | 两席其余观察（含本席 O-3） | 只登记不入队 | **同意；无漏并、无应升队项** |

冲突入队 0。细化入队 1（002）。技术性入队 1（001）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 2。本模块写入集合＝`backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` 一个文件（主工作区版只落 001；发布变体＝origin/main 版＋001＋002）。不改被测代码、不加迁移。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、M037-001（CX-M037-001①，技术性）。票：同意。本聚合关键分支；本席 B1 漏报（只记在 O-1）。**

独立核源（不重审模块）：

1. 测试与闸序。origin/main 与主工作区同文同行 L175-183 `templateEditCannotGrantAnUnownedHighPermission`：`manager` 只持有 `LOW`（`ops.production_readiness.view`，`requiresApproval=false`），`updatePermissionTemplate` 把 `HIGH`（`finance.refund.create`，`requiresApproval=true`）加进已有模板；断言只 `isInstanceOf(ApiException)` 且模板项仍为 `LOW`。origin/main `AccessService.updatePermissionTemplate` L174 `requireActorCanGrant(added)`、L175 `requireGrantApprovalIfNeeded(added)`。`HIGH` 同时满足「不持有」与「须审批」，两道闸谁先抛都满足现断言。
2. 现行实抛码。`/home/james/audit-tmp/m037_codes.log`：origin L181 现抛 `GRANT_EXCEEDS_OWN_PERMISSIONS`（第一道先于第二道）。与候选「原场景核第一道」一致。
3. 单独删闸。只读 `/home/james/audit-tmp/m037_mutation2.log`：origin/main `40fcc8f1` 一次性 scratch，基线 suites=263 tests=**3473** red=20（XM-M036-A 同 20 条）；`template_edit_no_own_gate` 与 `template_edit_no_approval_gate` 之后仍 red=20、无 NEW-RED 行。全量 3473 个后端测试 0 个新增变红。另 `/home/james/audit-tmp/m037_mutation.log` 的 `actor_can_grant_open`（整闸放开）NEW-RED 只有 AccessServiceTests 两条，**本测试不红**——第二道仍抛 `GRANT_REQUIRES_APPROVAL`。
4. 两道闸不是同一件事。第一道：`requireActorCanGrant` 对 `effectivePermissions().grants()` 缺码抛 `GRANT_EXCEEDS_OWN_PERMISSIONS`（wildcard 才放行；本测试主体是 staff）。第二道：`grantApprovalEnabled=true` 时 `approvalRequiredPermissions` 过滤 `PermissionDefinition::requiresApproval`，非空则 `GRANT_REQUIRES_APPROVAL`。模板管理员把**不持有的日常码**加进已应用模板，只有第一道拦；把自己**持有的高危码**加进去直接发给已套用者，只有第二道拦。这正是 M006-005 当初定稿的缺陷面，不是旁路。
5. 是否本聚合关键分支。M037 聚合维度是「功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第3段」，FILES 仅本安全回归文件。`updatePermissionTemplate` 的授出能力闸与审批闸是本切片「权限模板」主面；该测试名称与 M006-005 注释声称锁住「不持有的高危」，断言却分不清哪一道起作用。首次正常使用即可走到（模板编辑页加码）。同意入队，不同意继续留在 O-1。
6. 分类。冲突？否（SUT 已实现两闸，与「既有高危不得因处于岗位模板内而免除授出前审批」同向）。细化？否——路径已被调用，缺的不是业务场景，是断言不能使**各自**的闸失败。**技术性**（测试技术缺口，无业务发明）。添加？否。冻结 GEN-003／GEN-005；治理地图 L658／L703／L660 作上位旁证，不扩锁、不 LOCK-AMEND。
7. 边界。只改这一条测试，不改被测代码；两版同文，打同一处补丁。原场景（不持有且高危）断言 `GRANT_EXCEEDS_OWN_PERMISSIONS`；补「不持有但不须审批的码」→ `GRANT_EXCEEDS_OWN_PERMISSIONS`；补「持有的高危码」→ `GRANT_REQUIRES_APPROVAL`；三种情形后模板项均不变。三场景合取才能分别锁两闸：删第一道时原场景会改抛审批码、日常不持有码会写入；删第二道时「持有的高危」会写入。发布＝origin/main 版＋同一处补丁。

本席 B1 把 25 处 `isInstanceOf` 一记 O-1，未做「单独删 `updatePermissionTemplate` 两闸」变异，漏报成立。候选未改写席甲原意。

**明确同意**：001 技术性／是／是。

**二、M037-002（GR-01＋CX-M037-001②，细化）。票：同意。未改写原意；第三处并入合适。**

独立核源：

1. 本席 GR-01 两处（origin/main 成立；主工作区未发布版已有近似断言）。origin L553-608 `positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval`：`setUp` 已是 `platform_admin`，L585 直接 `archive`，从未以 `staff` 主体触发 `changePositionTemplateLifecycleAudited` L500-502 的 `requirePlatformAdmin`（非主会话则 `PLATFORM_ADMIN_REQUIRED`）。全文 0 处 `reject_delete`。SUT L538-555 `approve_delete`/`reject_delete` 同分支，驳回路径测试从不调用。
2. 席甲第三处。origin L592-593「普通管理员批准删除」只 `isInstanceOf(ApiException)`。来批准的就是刚 `request_delete` 的同一个 `admin`。`/home/james/audit-tmp/m037_codes.log` L593 在闸完好时实抛 `MASTER_AUTHORITY_REQUIRED`（AccessService L542-543，文案「U 盘密钥」）。`/home/james/audit-tmp/m037_mutation.log`：`lifecycle_no_master` 全量 3473／红 20，0 NEW-RED。`/home/james/audit-tmp/m037_equiv.log`：同一变异下把该处收紧为核 `MASTER_AUTHORITY_REQUIRED` 后，实抛 `SUPERVISOR_SELF_REVIEW_FORBIDDEN`——被不得自审顶替，与最高权限闸无关。复核服务那一层最高权限节点另由 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` 锁住（`review_master_node_open` NEW-RED 含该测试），缺的是 AccessService 这一层自己的回归。
3. 席甲补跑本席两处。只读 `/home/james/audit-tmp/m037_mutation3.log`：`lifecycle_no_platform_admin`、`reject_delete_as_approve` 均 3473／红 20，0 NEW-RED。证实 GR-01：非平台管理员存档闸与 `reject_delete` 分支在全库都没有能失败的测试。
4. 归并有没有改写 GR-01。候选 002 ①②＝本席最小拟处置原文（staff `archive` → `PLATFORM_ADMIN_REQUIRED` 且 `savePositionTemplateLifecycle` 未被调用；申请后主会话 `reject_delete`，未软删且可再申请）。「删除复核单号清空」不是加戏：SUT L526 `request_delete` 要求 `deleteReviewId == null`，不清空就不能再次申请；主工作区未发布版 L605-609 已是此写法。③＝席甲 CX-M037-001②，本席 W-3 原句「错误码收紧可随 GR-01 用 origin 夹具做」——并入同一条测试、同一发布变体，不是把 W 项扩成新缺陷 ID，也不是把 GR-01 改写成别的故事。
5. 分类。冲突？否。细化？是（BSM-003／BSM-028、配置删除须独立最高权限审批的下位测试；停在细化）。第三处单独看偏技术性（断言被顶替），但与①②同方法、同「须独立最高权限审批」声称，整 ID 采细化正确。添加？否。冻结 GEN-003（辅 BSM-003、BSM-028、GEN-005）。
6. 边界。只改**发布口径**这一条测试；主会话沿用 origin/main 夹具＝`MASTER_USER_ID`＋`"platform_admin"`，**不引入** `SessionIssuer.MASTER_ACCOUNT_TYPE`（origin `SessionIssuer.java` 无该常量；origin `isMasterAuthority` 为 `platform_admin` 桥＋主用户 ID／名）。不改被测代码。主工作区未发布版该测试已含①②③等价断言（L586-609，夹具用的是未发布常量），B3 主工作区 diff 不再改 002。发布变体＝origin/main 版＋001＋002。

**明确同意**。第三处并入合适。

**三、W 项。票：同意 W-M037-1 与 W-M037-2。**

| 新号 | 本席 B1 | 席甲 B1 | 事实（仅未发布版，D-029 不入队） |
|---|---|---|---|
| W-M037-1 | W-1＋W-3 里未发布常量＋W-4 | 原 W-M037-1 拆出 002／W-M037-2 后的残余 | 主会话改按 `SessionIssuer.MASTER_ACCOUNT_TYPE` 识别；建岗位模板主体改为 `template-admin`。origin/main 无该常量，origin＋主工作区版无法编译 |
| W-M037-2 | W-2 | 原 W-M037-1 里的复核排期夹具 | `topAuthorityReview…` 补 `findSupervisorReviewSchedule`，只为满足未发布 `SupervisorReviewService.approve` 的「须登记直接受益人」；与 W-M036-3 同源。origin `approve` 无此闸 |

席甲原 W-M037-1 把 `PLATFORM_ADMIN_REQUIRED`、`reject_delete` 后重新申请、排期夹具与未发布常量捆在一起。候选已拆：origin 可编译的①②③进 002，排期夹具改号 W-M037-2，余下才是 W-M037-1。与本席 B1 的 D-029 切分一致。W-3「错误码收紧可随 GR-01 做」已并入 002③，同意；不把未发布常量随本模块发布。

**四、XM-M037-A。票：同意登记待操作员决定是否重开 M036；不应在本模块文件里补。**

只读 `m037_mutation.log`：`actor_can_grant_open` NEW-RED 仅

- `AccessServiceTests.assignUserRoleRejectsRolePermissionsActorDoesNotHold`
- `AccessServiceTests.createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold`

本席核 `requireActorCanGrant` 调用点：`updatePermissionTemplate`（本模块，001 将锁）、`resolveRoleGrant`／`createTemporaryGrantDirect`（上两条已锁）、以及 `applyPermissionTemplate`、`createRole`、`changeUserPermissionOverrides`、`unassignUserRole`。岗位路径走 `requireActorCanGrantImmediate`（`resolvePositionGrant`、`savePositionRoleTemplate`），审批关闭时才退回整闸。AccessServiceTests 全文件 `GRANT_EXCEEDS_OWN_PERMISSIONS` 只出现在上述两条。M036 已闭环，FILES 不含 `AccessServiceTests`。本模块 001 锁的是模板**编辑**加码，不是套用／建角色／员工单独授予／定岗的各自失败面。不在 `M006SecurityRegressionTests` 铺这些调用点。同意 XM 登记，不升本模块队列。

**五、其余观察。票：全部只登记；无漏并须升队项。**

候选「只登记不入队」已覆盖两席观察合取：

- 其余 23 处只断言异常类型（origin 25 处 `isInstanceOf(ApiException.class)`，本席点数＝25；001 收 L181、002③ 收 L593，余 23）。`m037_codes.log` 25 处现抛都是各自所守的码；6 处变异能红（`review_master_node_open`、`staff_company_approval_open`、`cross_merchant_write_open`、`widens_window_open`、`summary_override_open`、`position_in_use_open`），其余 `verify(never)`／状态断言或单一守卫。两席一致不逐一改成核码。
- 两条岗位派生测试断言的是测试自己的桩（本席 O-2＝席甲 OBS-2）；库内语义由已修 M036-001 守住。
- `grantListHasADefaultTimeWindow` 只断言 cutoff 非空、不锁 30 天（本席 **O-3**，候选已点名）。null 无界已被锁；30 天是政策配置值，不是本聚合未锁关键分支。不升队。
- 反射调私有方法与一次字段注入（本席 O-4／O-5＝席甲 OBS-3）；与生产装配一致。
- 桩内职级只 1–2 档（本席 O-6）；库内 30 档属已闭环迁移。
- 映射无 BSM-007／PGM-040／BEQR-151（本席 O-8）；不扩锁。
- XM-M036-A 基线 20 红（本席 O-7；本轮全量 3473＝M036 检查点后加测，仍红这 20 条）。

无应升队项。不为理论路径补需求。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 M037 文件。不申请改三 HTML。不 BLOCKED-ASK。候选表无添加行。

---

### 三、最小修复边界

一次 B3 允许的写入集合＝`backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` 一个文件。禁止改被测 Java、已应用迁移、前端、OpenAPI、三 HTML、锁。主工作区未发布版只落 001；002 只打在发布变体（origin/main 版＋补丁）。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | 只改 `templateEditCannotGrantAnUnownedHighPermission`：原场景核 `GRANT_EXCEEDS_OWN_PERMISSIONS`；补不持有、不须审批的码 → 同码且模板项不变；补持有的高危码 → `GRANT_REQUIRES_APPROVAL` 且模板项不变。两版同一处 | 改 `AccessService.updatePermissionTemplate` 或两闸实现；只收紧原场景错误码而不补另外两支；把套用／建角色／员工授予／岗位路径的 `requireActorCanGrant` 失败面写进本文件（那是 XM-M037-A） |
| 002 | 只改发布口径 `positionTemplateLifecycle…`：① staff `archive` → `PLATFORM_ADMIN_REQUIRED` 且 `savePositionTemplateLifecycle` 未被调用；② 申请后主会话 `reject_delete`，`deletedAt` 仍空、`deleteReviewId` 清空，随后可再申请并批准；③ 「普通管理员批准删除」核 `MASTER_AUTHORITY_REQUIRED`。主会话夹具＝`MASTER_USER_ID`＋`"platform_admin"`。主工作区版不再改 | 引入 `MASTER_ACCOUNT_TYPE`；改 `AccessService.changePositionTemplateLifecycleAudited`；把未发布工作区整段生命周期夹具原样打进 origin 变体 |

B3 实施非否决提示（不改变本票）：001「不持有但不须审批的码」在现行 `setUp` 字典只有 `LOW`（已持有）与 `HIGH`（须审批）时表达不了，须在**本条测试**内加一条 `requiresApproval=false` 且 manager 未持有的第三码（并让 `listPermissions`／`permissionExists` 认得它）；「持有的高危」须先 `effect(manager, HIGH, GRANT)` 再改模板。不得把第三码写进被测生产字典。002 发布变体不要复制主工作区的 `MASTER_ACCOUNT_TYPE` 行。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只改上表写入集合。

- 001：技术性／是／是。本席 B1 漏报，候选未改写；属本聚合模板编辑两闸关键分支。
- 002：细化／是／是。GR-01 原意保留；第三处并入合适；发布变体用 origin 夹具。
- W-M037-1、W-M037-2：D-029 登记，同意改号与拆分。
- XM-M037-A：同意；不在本模块文件补。
- 观察全部只登记，含 O-3；无添加、无 HTML 申请、无 questions。

### 结论

全批差集无原意改写、无漏并、无应升队观察。CX-M037-001①→001（本席 O-1 子集升队，采技术性）；GR-01＋CX-M037-001②→002（采本席细化；W-3 错误码收紧并入③）。逐 ID：001 PASS、002 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M037 IN_PROGRESS；B3=0。

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **34828** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
