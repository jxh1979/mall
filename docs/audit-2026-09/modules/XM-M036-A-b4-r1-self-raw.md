CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M036-A B4-r1 修复 diff 审计 · 席甲报告
版本：V0.01

## 0. 输入与方法

- 席甲：Claude Opus 5.5（作者自审票，B3 由本席实施）。2026-09-24T04:04:50Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14，LOCK-AMEND-COUNT 1）。定稿：findings V4.00「XM-M036-A B2-r1 双 PASS 定稿」段及其上方候选段，修复队列「是」XM-M036-A-001…008。
- diff：before tree bfe6bea44dcede386aa433ef71971ad8cf8852c5 → after tree b99e79c1e08757c04f6c9f869b402e2e3e68f31d；冻结 /home/james/audit-tmp/xm036a_b3r1.diff（32761bytes SHA256 800824fe472eebd5d75dca3fc235a5d88367cb5e90c79d05400eb6daec194d4e），12 files changed, 150 insertions, 40 deletions；无文件模式变更。落盘前未读 Grok 本次正文（modules/XM-M036-A-b4-r1-grok-raw.md）。

## 1. 可执行证据

```
START=2026-09-24T04:00:46Z HEAD=e456ba96 origin/main=4f233e7b
scratch at 4f233e7b
== RED（只落测试补丁）
MVN_EXIT(red)=1
  red: suites=263 tests=3536 red=13
  RED 相对原样基线（xmd029-ckpt.red，origin/main 4f233e7b 23 红）新增红：
    NEW-RED: AmusementServiceTests.deleteProjectEntitlementRequiresArchiveThenApprovalAndKeepsTombstone
    NEW-RED: AmusementServiceTests.masterRequestedProjectEntitlementDeletionTakesEffectDirectly
    NEW-RED: AmusementServiceTests.nonMasterCannotApproveProjectEntitlementDeletion
    NEW-RED: AmusementServiceTests.rejectedDeleteProjectEntitlementFallsBackToArchivedOnly
    NEW-RED: AmusementServiceTests.tombstonedProjectEntitlementRestoreRequiresMasterAuthority
    NEW-RED: FacilityEquipmentArchiveDeleteJdbcIntegrationTests.masterRequestedDeletionTakesEffectDirectly
    NEW-RED: FacilityEquipmentArchiveDeleteJdbcIntegrationTests.ordinaryApplicantCannotApproveMasterOnlyDeletion
    NEW-RED: PlatformJdbcIntegrationTests.backToBackSpaceTypeDeleteRequestsNeverCollideOnId
    NEW-RED: PlatformJdbcIntegrationTests.completingSpaceTypeRequestKeepsPriorRequestHistory
    NEW-RED: StaffProfileArchiveDeleteJdbcIntegrationTests.masterRequestedDeletionTakesEffectDirectly
== GREEN（测试＋生产补丁）
MVN_EXIT(green)=0
  green: suites=263 tests=3536 red=0
  GREEN 全部红（预期 0）：
  GREEN 相对基线转绿条数：23
  scratch dirty after cleanup: 0
  scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T04:02:30Z
```

```
主工作区落补丁（XM-M036-A B3-r1）2026-09-24T04:03:06Z
写入前 12 个文件与 origin/main 4f233e7b 相同（无 PRE-DIRTY）
before tree=bfe6bea44dcede386aa433ef71971ad8cf8852c5
after tree=b99e79c1e08757c04f6c9f869b402e2e3e68f31d
frozen diff /home/james/audit-tmp/xm036a_b3r1.diff: 32761 bytes sha256 800824fe472eebd5d75dca3fc235a5d88367cb5e90c79d05400eb6daec194d4e
 12 files changed, 150 insertions(+), 40 deletions(-)
主工作区 diff 与一次性 worktree 验证过的 diff：same
```

## 2. 逐块归因（30 块）

| 文件 | 块 | 块头 | ± | 归因 ID | 首条改动行（截断） |
|---|---:|---|---|---|---|
| StaffService.java | 1 | `@@ -382,7 +382,10` | +4/−1 | XM-M036-A-007 | StaffProfileRecord profile = requireProfileInScope(id); |
| StaffService.java | 2 | `@@ -409,7 +412,9` | +3/−1 | XM-M036-A-007 | StaffProfileRecord profile = requireProfileInScope(id); |
| AmusementService.java | 1 | `@@ -359,7 +359,7` | +1/−1 | XM-M036-A-006 | * 已被审批通过删除（墓碑）的绑定恢复须持审批权 amusement.project_entitlement.approve（只授 role |
| AmusementService.java | 2 | `@@ -377,14 +377,17` | +7/−4 | XM-M036-A-006 | checkProjectOwnership(id); |
| AmusementService.java | 3 | `@@ -410,7 +413,10` | +4/−1 | XM-M036-A-006 | checkProjectOwnership(id); |
| AmusementService.java | 4 | `@@ -419,6 +425,10` | +4/−0 | XM-M036-A-006 | if (master) { |
| AmusementService.java | 5 | `@@ -426,7 +436,7` | +1/−1 | XM-M036-A-006 | * rejected → 回 none。权限 amusement.project_entitlement.approve（只授 role_p |
| AmusementService.java | 6 | `@@ -438,6 +448,9` | +3/−0 | XM-M036-A-006 | if (!isMasterActor()) { |
| AmusementService.java | 7 | `@@ -448,7 +461,6` | +0/−1 | XM-M036-A-006 | checkProjectOwnership(id); |
| AmusementService.java | 8 | `@@ -464,6 +476,11` | +5/−0 | XM-M036-A-006 | /** BSM 第 570 行：配置数据删除审批与墓碑恢复只认持授权 U 盘密钥的最高权限本人；这些单据级路径对其逐笔放行（经 execut |
| FieldLabels.java | 1 | `@@ -72,6 +72,7` | +1/−0 | XM-M036-A-004 | p("business_entity_id", "经营主体"); |
| FacilityEquipmentService.java | 1 | `@@ -108,12 +108,21` | +9/−0 | XM-M036-A-005 | if (authorizationService.isMasterAuthority(authorizationService.curren |
| FacilityEquipmentService.java | 2 | `@@ -124,10 +133,7` | +1/−4 | XM-M036-A-005 | String actor = archiveActorId(); |
| JdbcPlatformRepository.java | 1 | `@@ -394,7 +394,7` | +1/−1 | XM-M036-A-008 | .addValue("id", "stdr_" + spaceTypeId + "_" + java.time.Instant.now(). |
| StaffProfileArchiveDeleteJdbcIntegrationTests.java | 1 | `@@ -107,4 +107,15` | +11/−0 | XM-M036-A-007 | @Test |
| AmusementServiceTests.java | 1 | `@@ -1482,11 +1482,13` | +4/−2 | XM-M036-A-006 | // 职责分离：换一名平台管理员审批（同人审批见 selfApprove 用例）。 |
| AmusementServiceTests.java | 2 | `@@ -1500,7 +1502,7` | +1/−1 | XM-M036-A-006 | TestSecurityActors.asPlatformAdmin("usr_admin_approver"); |
| AmusementServiceTests.java | 3 | `@@ -1508,36 +1510,57` | +30/−9 | XM-M036-A-006 | void deleteRequesterCannotApproveOwnProjectEntitlementDeletion() { |
| AmusementServiceTests.java | 4 | `@@ -1545,6 +1568,16` | +10/−0 | XM-M036-A-006 | /** 持授权 U 盘密钥的最高权限本人（生产会话形状：master_authority，所属公司 platform-company）。 * |
| ComplianceControllerTests.java | 1 | `@@ -71,7 +71,7` | +1/−1 | XM-M036-A-002 | mockMvc.perform(post("/api/v1/compliance/contract-key-dates") |
| ComplianceControllerTests.java | 2 | `@@ -93,7 +93,7` | +1/−1 | XM-M036-A-002 | mockMvc.perform(post("/api/v1/compliance/contract-key-dates") |
| ComplianceControllerTests.java | 3 | `@@ -153,7 +153,7` | +1/−1 | XM-M036-A-002 | mockMvc.perform(post("/api/v1/compliance/reminders/scan") |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | 1 | `@@ -50,7 +50,7` | +1/−1 | XM-M036-A-005 | TestSecurityActors.asPlatformAdmin("usr_approver"); |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | 2 | `@@ -86,7 +86,7` | +1/−1 | XM-M036-A-005 | TestSecurityActors.asPlatformAdmin("usr_approver"); |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | 3 | `@@ -94,7 +94,7` | +1/−1 | XM-M036-A-005 | void selfApproveForbidden() { |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | 4 | `@@ -102,6 +102,31` | +26/−1 | XM-M036-A-005 | .hasMessageContaining("发起人"); |
| FinanceControllerTests.java | 1 | `@@ -52,6 +52,10` | +4/−0 | XM-M036-A-001 | // StoredValueService 有必需的 @Autowired 字段；@Bean 交出的 mock 仍会被字段注入，故用 @Mo |
| FinanceControllerTests.java | 2 | `@@ -416,11 +420,6` | +0/−5 | XM-M036-A-001 | @Bean |
| CustomerGuardedActionAndRejectionAuditTests.java | 1 | `@@ -39,7 +39,7` | +1/−1 | XM-M036-A-003 | Arguments.of(StoredValueService.class, "consume", "customer.stored_val |
| PlatformJdbcIntegrationTests.java | 1 | `@@ -560,6 +560,19` | +13/−0 | XM-M036-A-008 | @Test |

共 30 块，+150/−40，逐块归因齐全。

归因不到定稿 ID 的行：0。

## 3. 内容审计（逐 ID）

- 001：只删 TestBeans 中 StoredValueService 的 `@Bean` 方法（5 行），在测试类加 `@MockitoBean StoredValueService` 字段与一行说明；TestBeans 其余三个 Bean 未动。GREEN 下 FinanceControllerTests 12 条执行到断言并通过。
- 002：三个 POST 各加 `.requestAttr("com.complex.platform.gate.verified_terminal", new TerminalIdentityHolder.VerifiedTerminal("web", null, true))`，键与 TerminalIdentityHolder.ATTR 一致；guard Bean 与拦截器未动。三条 GREEN。
- 003：参数行方法名 consume → payTicketOrder，期望码不变。
- 004：FieldLabels 加一行 `p("business_entity_id", "经营主体")`。FieldLabelsTests GREEN。
- 005：approveDelete 首句非最高权限 403 MASTER_APPROVAL_REQUIRED，原 requirePermission 保留（最高权限短路）；删去申请人自审闸 4 行并以注释说明——「审批只认最高权限」后该闸只可能拦最高权限自己发起的直接删除，与第 577 行后半句相反（B2 定稿实施读法）；requestDelete 由最高权限发起时存待审后调 approveDelete，占用检查（refs>0 抛冲突、整笔回滚）照走，返回 approved 记录。测试：成功与占用两条改由最高权限审批；原「发起人」自审用例改为「非最高权限申请人不能审批」并断言申请仍待审批（比原断言更强）；补「最高权限发起即删除」。RED 下后两条红、GREEN 下全绿。
- 006：decideDelete 的最高权限闸在结论值校验之后、executeWrite 之前（「未知结论」用例仍报 VALIDATION_FAILED）；decideDelete 写入体去掉 checkProjectOwnership（审批只认最高权限，无商户归属可比）；restore 只在 deleteStatus＝approved 且非最高权限时 403，普通取消存档仍对非最高权限走 checkProjectOwnership；requestDelete 最高权限发起时在写入体内直接调仓储按通过，不经 decideDelete 自审闸；跳过 checkProjectOwnership 只写在三条写入体，checkProjectOwnership 本体与 AuthorizationService 范围方法未改；两处 javadoc 改为现状。测试：两条审批改由最高权限；「发起人不能自审」改为「发起人与另一平台管理员都不能审批、申请仍待审批」；补「最高权限发起即删除」；墓碑恢复先断言平台管理员 MASTER_APPROVAL_REQUIRED、再由最高权限恢复成功，去掉 permissionExists 桩；留墓碑用例审批后切回平台管理员查看列表（最高权限列表口径属只登记的 XM-M036A-1，生产列表路径未改）。RED 下 5 条红、GREEN 全绿。
- 007：requestDeleteProfile 对最高权限按存在取档案、其余走 requireProfileInScope；approveDeleteProfile 在既有最高权限闸之后按存在取档案；requireProfileInScope、requireRegistrationScope 本体未改。测试补「最高权限发起即删除」真库回归（夹具为 StaffTestActors.master()，所属公司空），RED 红、GREEN 绿；两条既有红转绿。
- 008：主键改 `"stdr_" + UUID.randomUUID()`，space_type_id、requested_at、status 与按 requested_at 取最新的查询不变；补同一种类连续 50 次申请—办结的真库回归，RED（旧主键）红、GREEN 绿；原时红时绿用例在 RED 轮亦红、GREEN 绿。
- 借机删弱：无。唯一删去的断言是设施与游乐的「发起人自审」两处，均换成更强的「非最高权限不能审批」；设施生产删去的自审闸按定稿实施读法。
- 新业务或新依赖：无；`@MockitoBean` 来自既有 spring-test 7.0.8。
- 失效范围：写入集合所属 M009、M011、M054、M056、M077、M230、M232、M263、M272、M335、M396、M397 均为 PENDING。

## 4. 结论

- 归因票：**PASS**（30 块全部归因到 XM-M036-A-001…008，越界 0）。
- 内容票：**PASS**（按定稿最小处置实现；GREEN 0 红；无新缺陷、无添加项）。
- 席甲 B4-r1 票：**PASS**。
