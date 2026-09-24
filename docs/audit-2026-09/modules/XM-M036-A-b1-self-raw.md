CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M036-A B1 修复前全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票；按 §4「合取不变、推翻权不对等」处理）。2026-09-24T03:27:08Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。基线 origin/main 4f233e7b。
- 范围：ledger「当前 XM-M036-A B1 开工」段所列 21 个文件（8 个红测试文件、失败路径上 13 个生产文件）；为定根因另读 AuthorizationService、SessionIssuer、ProductionAuthService、TerminalMasterSwitchInterceptor、DoubleSubmitCsrfFilter、TerminalIdentityHolder、StaffTestActors、TestSecurityActors、FacilityController、AmusementController、V266/V341 迁移，并 grep 全仓同根写法。判据：16 个目标模块映射并集 297 项。
- 顺序：本席先取证、落本报告；落盘前未读 Grok 本次正文（modules/XM-M036-A-b1-grok-raw.md）。只读承诺：未改任何已跟踪文件；探针在一次性 worktree 内做，已还原删除。

## 1. 文件覆盖

| 文件 | 所属模块 | 行数 | blob |
|---|---|---:|---|
| backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java | M335 | 429 | 0c570c9be9b0 |
| backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java | M263 | 250 | a5dee424c70a |
| backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | M077 | 107 | f6cb05ab4d20 |
| backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java | M232 | 3391 | eaa3385b5a67 |
| backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java | M397 | 133 | f0e4fa4e2fdf |
| backend/src/test/java/com/complex/platform/common/api/FieldLabelsTests.java | M397 | 94 | fc2ea604a6a1 |
| backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java | M056 | 110 | 305993f41f61 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | M011 | 760 | 40f0068afcc6 |
| backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java | 计划外（U-FIX e0446520 新增） | 161 | c754f2593361 |
| backend/src/main/java/com/complex/platform/common/api/FieldLabels.java | M396 | 553 | 51d75c79e8f1 |
| backend/src/main/java/com/complex/platform/customer/StoredValueService.java | M153 | 874 | cc0af2e3d332 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | M009 | 1338 | f4c086abb98e |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | M009 | 1213 | 2da0b8eed3cb |
| backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java | M272 | 276 | 891c8edccc1c |
| backend/src/main/java/com/complex/platform/amusement/AmusementService.java | M230 | 1930 | 033eea336e24 |
| backend/src/main/java/com/complex/platform/compliance/ComplianceController.java | M262 | 164 | 3b6e6fdaa0eb |
| backend/src/main/java/com/complex/platform/compliance/ComplianceService.java | M262 | 510 | b6f8e860b573 |
| backend/src/main/java/com/complex/platform/finance/FinanceController.java | M331 | 230 | 80b6928bb9f3 |
| backend/src/main/java/com/complex/platform/access/staff/StaffService.java | M054 | 1661 | 3990501a65b3 |
| backend/src/main/java/com/complex/platform/access/staff/StaffController.java | M054 | 386 | 5077e5cc8e76 |
| backend/src/main/java/com/complex/platform/customer/CustomerService.java | M158 | 1309 | 650ffd5a1ab2 |

每个测试文件读全红用例及其夹具（TestBeans、BeforeEach、测试工具）；生产文件读失败路径上的方法、其调用方与权限／范围守卫。

## 2. 证据

- 基线红名单（origin/main 4f233e7b，一次性空库后端全量 263 套 3532 测 23 红）：/home/james/audit-tmp/xmd029-ckpt.red；失败信息 xmd029_b5.msgs；完整日志 xmd029-ckpt.mvn.log。3ca1b507 基线 20 红：m036_mut_base.red。
- 探针（origin/main 一次性 worktree，按各条最小处置改 4 个测试与 1 处字段标签，只跑 5 个不连库的测试类）：

```
START=2026-09-24T03:21:36Z origin/main=4f233e7b
scratch at 4f233e7b
patched FinanceControllerTests.java
patched FinanceControllerTests.java
patched ComplianceControllerTests.java
patched ComplianceControllerTests.java
patched CustomerGuardedActionAndRejectionAuditTests.java
patched FieldLabels.java
patched AmusementServiceTests.java
 5 files changed, 14 insertions(+), 10 deletions(-)
MVN_EXIT=1
suites=5 tests=128 red=1
  RED: AmusementServiceTests.tombstonedProjectEntitlementRestoreRequiresApprovePermission
scratch dirty after cleanup: 0
scratch removed
END=2026-09-24T03:21:50Z
```

探针结论：Finance 12、Compliance 3、储值注解 1、FieldLabels 1 按最小处置全绿；Amusement 改由最高权限恢复墓碑后仍红，报 MERCHANT_SCOPE_REQUIRED「此操作必须在商户范围内进行」（AuthorizationService.currentMerchantScope:434 ← AmusementService.checkProjectOwnership:1603），印证 CX-06/CX-07 与登记①的最高权限范围问题。

## 3. 逐条根因

| 红用例 | 条数 | 根因 | 引入提交 | 归类 |
|---|---:|---|---|---|
| FinanceControllerTests 全部 | 12 | StoredValueService.java:78-81 两个必需的 `@Autowired` 字段（CustomerService、TicketingRepository）；测试 TestBeans 用 `@Bean` 返回 `Mockito.mock(StoredValueService)`，Spring 对工厂方法产出的实例照样做字段注入，切片上下文没有 CustomerService，上下文加载失败 | ceca1a54（M006，PR #415）加字段，测试未同步 | 测试过期 CX-01 |
| ComplianceControllerTests 三个 POST 用例 | 3 | 测试 TestBeans 提供 TerminalSwitchGuard（ComplianceController 构造所需），TerminalMasterSwitchInterceptor.java:53-75 因此生效；写请求没有服务端派生的终端身份即 403 TERMINAL_NOT_ALLOWED。拦截器行为符合治理地图第 652、655 行 | 8d2fafff（2026-08-03 写请求兜底拒绝），测试最后改于其前（5ba580c1） | 测试过期 CX-02 |
| CustomerGuardedActionAndRejectionAuditTests#customerActionsCarryNonEmptyCapability[3] | 1 | 公开的 `consume` 已拆为带 `@GuardedAction(capability=customer.stored_value.consume)` 的 `payTicketOrder`（StoredValueService.java:264-265）与内部 `consumeInternal`（:318，仅 BarBillingService:251 调用）；测试按旧方法名找注解得 null | ceca1a54 | 测试过期 CX-03 |
| AmusementServiceTests#tombstonedProjectEntitlementRestoreRequiresApprovePermission | 1 | 测试以平台管理员身份、靠 `permissionExists=true` 放行墓碑恢复；ceca1a54 起 AuthorizationService.isExplicitOrDailyGrant 不把 role_platform_admin 的 critical、需审批码当逐项授权（M006-I9-01），平台管理员被拒。改由最高权限恢复又被 checkProjectOwnership 挡（探针） | ceca1a54；最高权限一侧见 CX-06 | 测试过期 CX-04＋实现 CX-06 |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests 三条 | 3 | FacilityEquipmentService.approveDelete:117 `requirePermission("facility.equipment.approve")`（V266 critical、requires_approval）；平台管理员同上被拒 | ceca1a54 | 测试过期＋实现 CX-05 |
| FieldLabelsTests#everyFieldKeyThrownInMainSourcesHasChineseLabel | 1 | PlatformService.java:376 `requireText(entityId, "business_entity_id")`，FieldLabels 无此键 | 1aa4c9c2（M009/M011 发布） | 技术性 CX-09 |
| StaffProfileArchiveDeleteJdbcIntegrationTests 两条 | 2 | 最高权限审批 → StaffService.approveDeleteProfile:410 过最高权限闸 → :412 requireProfileInScope → :1175 `!isPlatformAdmin(actor) && merchantId==null` 进公司分支 → StaffRegistrationService.actorCompany:108-113。1aa4c9c2 起最高权限账号类型为 master_authority，不再是平台管理员桥；生产最高权限会话所属公司为 platform-company（ProductionAuthService:411），报 403 CROSS_ORG_FORBIDDEN；测试夹具所属公司为空，报「请填写所属公司」 | 1aa4c9c2（最高权限按会话类型识别，XM-D029 保留） | 实现缺陷 CX-07 |
| PlatformJdbcIntegrationTests#completingSpaceTypeRequestKeepsPriorRequestHistory（时红时绿） | — | JdbcPlatformRepository.java:397 主键 `"stdr_" + spaceTypeId + "_" + Instant.now().toEpochMilli()`，同一种类同一毫秒两次申请撞键 | 757ef452（M018） | 技术性 CX-08 |

## 4. 三线审计

- 第一性原理：这批测试要守的是——财务入口权限、合规写入口与终端兜底、储值扣款挂终端开关、字段报错中文、设施／员工档案／游乐配置的「存档→申请删除→最高权限审批→软删→可恢复」、空间种类删除申请留历史。实际：17 条红是测试没跟上 ceca1a54／8d2fafff 的正确生产改动，守护失效（Finance 12 条整类失守最重）；另有三处生产行为与 BSM 第 570、577 行不符（CX-05/06/07），一处主键拼法（CX-08），一处标签（CX-09）。未验证前提：最高权限范围问题在本范围外的 43＋22 个调用文件里的具体后果未逐个核（登记①）。
- 顺从式：297 项逐项见 §7。
- 对抗式（grilling）：
  - 问：Finance 能不能只在 TestBeans 再补 CustomerService、TicketingRepository 两个 mock？答：不行，CustomerService.java:81 自己也有必需的字段注入，会连锁；`@MockitoBean` 以手工单例注册、不做字段注入，探针已绿。
  - 问：Compliance 的 403 是不是 CSRF？答：不是。DoubleSubmitCsrfFilter 只在带后台 Cookie 时校验，这三条请求没有 Cookie；是终端写请求兜底。正确做法是请求带服务端派生的终端身份（生产由 DeviceCredentialInterceptor 写入同一属性），不是去掉 TerminalSwitchGuard。
  - 问：储值 consumeInternal 不挂终端开关是不是缺陷？答：它只供酒吧结账在已过酒吧域闸后内部调用、无控制器入口（M006 已审）；本轮只登记观察。
  - 问：设施审批码授给平台管理员就行了吗？答：不行。第 577 行原句「删除一律须由具备最高权限的人审批通过后执行，不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。现实现是可逐项授出的 critical 码，谁被授了谁就能批；最高权限自己发起的删除仍进待审批，且因自审闸永远批不掉。员工档案（U-FIX）已按此句实现（StaffService:385-412），设施未对齐。
  - 问：员工档案两条是不是测试夹具少了所属公司？答：不是。生产最高权限会话带 platform-company，照样被按公司口径拒绝；最高权限批员工档案删除、以及最高权限发起即直接删除（:385 同样先走 requireProfileInScope），在生产上都走不通。
  - 问：主键撞键在生产能发生吗？答：要同一种类同一毫秒两次写入（并发重复提交）；后果是 500 而非脏数据，归技术性。全仓同类「前缀＋毫秒」拼法只此一处；TicketVerificationService 另带 4 位随机，未撞。
- 本范围外同根：删除审批人不限最高权限的同族入口、最高权限在范围检查中被拒的系统性问题，见 §6 登记。

## 5. 问题（临时编号）

- **CX-01（技术性）** FinanceControllerTests.java TestBeans 用 `@Bean` 返回 StoredValueService 的 mock（:419-422），与 StoredValueService.java:78-81 的必需字段注入冲突，12 条全因上下文加载失败而红。正常触发：跑该测试类。证据：xmd029-ckpt.mvn.log「No qualifying bean of type CustomerService」；探针转绿。最小处置：改为 `@MockitoBean StoredValueService`，删去 TestBeans 中的对应 `@Bean`；不改生产。
- **CX-02（技术性）** ComplianceControllerTests.java 三个 POST（:74、:96、:156）未带服务端派生的终端身份，被 TerminalMasterSwitchInterceptor:70-75 兜底拒绝。已有判据：治理地图第 655 行「开关拉取失败时按"兜底关闭"处理，写操作一律拒绝」。最小处置：三个 POST 以请求属性带 `TerminalIdentityHolder.VerifiedTerminal("web", null, true)`；不改拦截器。
- **CX-03（技术性）** CustomerGuardedActionAndRejectionAuditTests.java:42 仍点名已不存在的 `consume`。最小处置：改点名 `payTicketOrder`（期望码不变 customer.stored_value.consume）。
- **CX-04（技术性）** AmusementServiceTests 墓碑恢复用例以平台管理员加 `permissionExists=true` 放行，ceca1a54 后该放行不成立；注释「最高权限 role_platform_admin 持有」混淆两个角色（BSM 第 588 行「平台管理员与最高权限是两个不同的角色」）。最小处置：随 CX-06 改由最高权限恢复；平台管理员被拒的前半断言保留。
- **CX-05（冲突，BSM 第 577 行）** FacilityEquipmentService.approveDelete:117 以可授出的 `facility.equipment.approve` 把关（FacilityController:67 同），非最高权限本人只要被授码即可审批；requestDelete:95-111 对最高权限发起的删除不直接执行，而 approveDelete 的自审闸（:128-130）又使其永远批不掉。已有判据：第 577 行原句（见 §4）。正常触发：设施设备存档后申请删除。最小处置：照 StaffService 同款——approveDelete 限最高权限本人（非最高权限 403 MASTER_APPROVAL_REQUIRED）、requestDelete 由最高权限发起时直接按通过处理并留痕；测试改由最高权限审批，自审用例改为「非最高权限申请人不能审批」并补「最高权限发起即删除」。
- **CX-06（冲突，BSM 第 570 行）** 游乐项目权益绑定（配置数据）：AmusementService.decideDeleteProjectEntitlement（:431-465）服务层不校验审批人，入口由 AmusementController:193 可授出的审批码把关；restoreProjectEntitlement:387 墓碑恢复亦按可授出码；checkProjectOwnership:1602-1611 调 currentMerchantScope，最高权限没有商户号即被拒（探针实证），最高权限既批不了删除、也恢复不了墓碑；最高权限发起的删除申请不直接执行。最小处置：审批与墓碑恢复限最高权限本人，最高权限发起即直接通过；checkProjectOwnership 对最高权限按逐笔放行（此处写入均经 executeWrite 留痕）；测试同步。
- **CX-07（冲突，BSM 第 577 行；治理地图第 528 行第④类）** StaffService.requireProfileInScope:1172-1181 把最高权限当普通公司员工（1aa4c9c2 起最高权限不再是平台管理员桥），approveDeleteProfile:412 与 requestDeleteProfile:385 都先走它；生产报 CROSS_ORG_FORBIDDEN，员工档案删除无人能批、最高权限发起的直接删除也走不通。最小处置：这两条单据级路径对最高权限按逐笔放行（取档案只校验存在），留痕由既有 delete_requested／delete_approved 事件承担；测试不改（夹具已是最高权限）。
- **CX-08（技术性）** JdbcPlatformRepository.java:397 删除申请主键含毫秒时间戳，同毫秒撞键。最小处置：主键改随机（`"stdr_" + UUID`），不动查询语义（按 requested_at 排序取最新）。
- **CX-09（技术性）** FieldLabels 缺 `business_entity_id`（PlatformService.java:376 抛出）。最小处置：补 `p("business_entity_id", "主体")`（与同文件「主体不存在」「主体名称」口径一致）。

## 6. 只登记（跨模块与观察）

- 登记①（→M032 AuthorizationService 与各调用模块）：1aa4c9c2 起最高权限（master_authority）不是平台管理员桥；AuthorizationService.currentMerchantScope:425-434 对其抛 MERCHANT_SCOPE_REQUIRED，currentProjectScopeQuietly:509-515 落到 platform-company（不存在的项目）。生产代码调 currentMerchantScope() 的 43 个文件、调 requireProjectScope／requirePlatformScope 的 22 个文件中，只有 AccessService、AuthorizationService、StaffService、PlatformService、SupervisorReviewService 显式处理最高权限；其余单据级操作对最高权限不可用，与第 528 行第④类「持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕」不符。须统一逐笔开放机制（单据级放行＋留痕、列表不整表），宜单独立项。
- 登记②（删除审批人不限最高权限的同族入口，→各模块）：BarController:419 bar.reservation.approve（M193）、BarPosCatalogController:84/133/181 bar.category/product/combo.approve（M170）、MerchantServiceController:106 merchant.service_work_order.approve（M266）、TicketingController:86/157/246 ticket.type/price_rule/entitlement.approve（M132）、PlatformController:139 merchant.profile.approve（M009）、ServiceOrderService:132 service.catalog.approve（M201）、DataCleanupController:54 data.cleanup.approve（M075）。均以可授出的 critical 码把关，与第 570、577 行「删除一律须由具备最高权限的人审批通过后执行，不设例外」不符；各模块串行到达时按此句审。
- 观察：consumeInternal 不挂储值终端开关能力码（仅酒吧结账内部调用，M006 已审）；TicketVerificationService 核销码为毫秒加 4 位随机，未见撞键；WebMvc 切片测试普遍用 `@Bean` 返回 mock，被 mock 类若有必需字段注入即连锁失败（本范围只 Finance 触发）。
- 根因共性：CI 不跑后端测试（XM-M038-A 已登记给 M003/M005），ceca1a54、8d2fafff、1aa4c9c2 三次改动均未跑后端全量即合并。

## 7. 297 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-003 定价、售票与收款主体 | business-enum-quick-reference-v0.1.html:32 | 无本面｜本合成模块范围（21 个文件）不触及「定价、售票与收款主体」所述对象｜通过 |
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（21 个文件）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-014 退款单状态 | business-enum-quick-reference-v0.1.html:45 | 无本面｜本合成模块范围（21 个文件）不触及「退款单状态」所述对象｜通过 |
| BEQR-016 售票与退款原则 | business-enum-quick-reference-v0.1.html:47 | 无本面｜本合成模块范围（21 个文件）不触及「售票与退款原则」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（21 个文件）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-024 游乐核销归属 | business-enum-quick-reference-v0.1.html:56 | 无本面｜本合成模块范围（21 个文件）不触及「游乐核销归属」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（21 个文件）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-028 会员唯一识别 | business-enum-quick-reference-v0.1.html:65 | 无本面｜本合成模块范围（21 个文件）不触及「会员唯一识别」所述对象｜通过 |
| BEQR-029 会员注册 | business-enum-quick-reference-v0.1.html:66 | 无本面｜本合成模块范围（21 个文件）不触及「会员注册」所述对象｜通过 |
| BEQR-030 会员身份状态 | business-enum-quick-reference-v0.1.html:68 | 无本面｜本合成模块范围（21 个文件）不触及「会员身份状态」所述对象｜通过 |
| BEQR-031 等级与成长值 | business-enum-quick-reference-v0.1.html:70 | 无本面｜本合成模块范围（21 个文件）不触及「等级与成长值」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（21 个文件）不触及「等级权益对照」所述对象｜通过 |
| BEQR-033 积分与成长值来源 | business-enum-quick-reference-v0.1.html:72 | 无本面｜本合成模块范围（21 个文件）不触及「积分与成长值来源」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（21 个文件）不触及「票务人次计算」所述对象｜通过 |
| BEQR-035 有效消费与冻结 | business-enum-quick-reference-v0.1.html:75 | 无本面｜本合成模块范围（21 个文件）不触及「有效消费与冻结」所述对象｜通过 |
| BEQR-036 积分明细类型 | business-enum-quick-reference-v0.1.html:77 | 无本面｜本合成模块范围（21 个文件）不触及「积分明细类型」所述对象｜通过 |
| BEQR-037 积分兑换定价 | business-enum-quick-reference-v0.1.html:78 | 无本面｜本合成模块范围（21 个文件）不触及「积分兑换定价」所述对象｜通过 |
| BEQR-038 商品规格 | business-enum-quick-reference-v0.1.html:79 | 无本面｜本合成模块范围（21 个文件）不触及「商品规格」所述对象｜通过 |
| BEQR-039 项目零售参考价 | business-enum-quick-reference-v0.1.html:80 | 无本面｜本合成模块范围（21 个文件）不触及「项目零售参考价」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（21 个文件）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-041 重复兑换与转赠 | business-enum-quick-reference-v0.1.html:82 | 无本面｜本合成模块范围（21 个文件）不触及「重复兑换与转赠」所述对象｜通过 |
| BEQR-042 券包状态 | business-enum-quick-reference-v0.1.html:83 | 无本面｜本合成模块范围（21 个文件）不触及「券包状态」所述对象｜通过 |
| BEQR-043 积分结算账期 | business-enum-quick-reference-v0.1.html:84 | 无本面｜本合成模块范围（21 个文件）不触及「积分结算账期」所述对象｜通过 |
| BEQR-044 会员系统服务费 | business-enum-quick-reference-v0.1.html:85 | 无本面｜本合成模块范围（21 个文件）不触及「会员系统服务费」所述对象｜通过 |
| BEQR-045 储值卡类型与收款主体 | business-enum-quick-reference-v0.1.html:86 | 适用｜储值卡：见 BSM-035｜通过 |
| BEQR-046 会员忠诚度扩展 | business-enum-quick-reference-v0.1.html:87 | 适用｜储值：见 BSM-035｜通过 |
| BEQR-047 会员注册与开卡 | business-enum-quick-reference-v0.1.html:89 | 无本面｜本合成模块范围（21 个文件）不触及「会员注册与开卡」所述对象｜通过 |
| BEQR-048 学生认证 | business-enum-quick-reference-v0.1.html:90 | 无本面｜本合成模块范围（21 个文件）不触及「学生认证」所述对象｜通过 |
| BEQR-049 会员积分活动状态 | business-enum-quick-reference-v0.1.html:91 | 无本面｜本合成模块范围（21 个文件）不触及「会员积分活动状态」所述对象｜通过 |
| BEQR-050 积分活动预算守恒 | business-enum-quick-reference-v0.1.html:92 | 无本面｜本合成模块范围（21 个文件）不触及「积分活动预算守恒」所述对象｜通过 |
| BEQR-051 充值积分 | business-enum-quick-reference-v0.1.html:93 | 无本面｜本合成模块范围（21 个文件）不触及「充值积分」所述对象｜通过 |
| BEQR-052 会员营销边界 | business-enum-quick-reference-v0.1.html:94 | 无本面｜本合成模块范围（21 个文件）不触及「会员营销边界」所述对象｜通过 |
| BEQR-053 发券对象 | business-enum-quick-reference-v0.1.html:95 | 无本面｜本合成模块范围（21 个文件）不触及「发券对象」所述对象｜通过 |
| BEQR-054 桌台/卡座/吧台位状态 | business-enum-quick-reference-v0.1.html:100 | 无本面｜本合成模块范围（21 个文件）不触及「桌台/卡座/吧台位状态」所述对象｜通过 |
| BEQR-055 账单状态 | business-enum-quick-reference-v0.1.html:101 | 无本面｜本合成模块范围（21 个文件）不触及「账单状态」所述对象｜通过 |
| BEQR-056 订台定金状态 | business-enum-quick-reference-v0.1.html:103 | 无本面｜本合成模块范围（21 个文件）不触及「订台定金状态」所述对象｜通过 |
| BEQR-057 订卡邀请函状态 | business-enum-quick-reference-v0.1.html:105 | 无本面｜本合成模块范围（21 个文件）不触及「订卡邀请函状态」所述对象｜通过 |
| BEQR-058 点酒方式 | business-enum-quick-reference-v0.1.html:106 | 无本面｜本合成模块范围（21 个文件）不触及「点酒方式」所述对象｜通过 |
| BEQR-059 支付方式 | business-enum-quick-reference-v0.1.html:107 | 无本面｜本合成模块范围（21 个文件）不触及「支付方式」所述对象｜通过 |
| BEQR-060 销售提成 | business-enum-quick-reference-v0.1.html:108 | 无本面｜本合成模块范围（21 个文件）不触及「销售提成」所述对象｜通过 |
| BEQR-061 服务员提成 | business-enum-quick-reference-v0.1.html:109 | 无本面｜本合成模块范围（21 个文件）不触及「服务员提成」所述对象｜通过 |
| BEQR-062 退菜退酒 | business-enum-quick-reference-v0.1.html:110 | 无本面｜本合成模块范围（21 个文件）不触及「退菜退酒」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作需审批：删除审批见 BSM-004｜通过（问题并入 CX-05/06） |
| BEQR-064 出品票据状态 | business-enum-quick-reference-v0.1.html:112 | 无本面｜本合成模块范围（21 个文件）不触及「出品票据状态」所述对象｜通过 |
| BEQR-065 账单操作 | business-enum-quick-reference-v0.1.html:114 | 无本面｜本合成模块范围（21 个文件）不触及「账单操作」所述对象｜通过 |
| BEQR-066 收银交班状态 | business-enum-quick-reference-v0.1.html:115 | 无本面｜本合成模块范围（21 个文件）不触及「收银交班状态」所述对象｜通过 |
| BEQR-067 最低消费 | business-enum-quick-reference-v0.1.html:117 | 无本面｜本合成模块范围（21 个文件）不触及「最低消费」所述对象｜通过 |
| BEQR-068 服务费 | business-enum-quick-reference-v0.1.html:118 | 无本面｜本合成模块范围（21 个文件）不触及「服务费」所述对象｜通过 |
| BEQR-069 小费 | business-enum-quick-reference-v0.1.html:119 | 无本面｜本合成模块范围（21 个文件）不触及「小费」所述对象｜通过 |
| BEQR-070 客人存酒有效期与转存 | business-enum-quick-reference-v0.1.html:120 | 无本面｜本合成模块范围（21 个文件）不触及「客人存酒有效期与转存」所述对象｜通过 |
| BEQR-071 存酒瓶数 | business-enum-quick-reference-v0.1.html:122 | 无本面｜本合成模块范围（21 个文件）不触及「存酒瓶数」所述对象｜通过 |
| BEQR-072 套餐赠券适用范围 | business-enum-quick-reference-v0.1.html:123 | 无本面｜本合成模块范围（21 个文件）不触及「套餐赠券适用范围」所述对象｜通过 |
| BEQR-073 非低消商品 | business-enum-quick-reference-v0.1.html:124 | 无本面｜本合成模块范围（21 个文件）不触及「非低消商品」所述对象｜通过 |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（21 个文件）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-075 桌台会话与菜单 | business-enum-quick-reference-v0.1.html:126 | 无本面｜本合成模块范围（21 个文件）不触及「桌台会话与菜单」所述对象｜通过 |
| BEQR-076 桌台计时产品 | business-enum-quick-reference-v0.1.html:127 | 无本面｜本合成模块范围（21 个文件）不触及「桌台计时产品」所述对象｜通过 |
| BEQR-077 门店支付方式 | business-enum-quick-reference-v0.1.html:128 | 无本面｜本合成模块范围（21 个文件）不触及「门店支付方式」所述对象｜通过 |
| BEQR-078 扫码点单身份 | business-enum-quick-reference-v0.1.html:129 | 无本面｜本合成模块范围（21 个文件）不触及「扫码点单身份」所述对象｜通过 |
| BEQR-079 优惠券自动选择 | business-enum-quick-reference-v0.1.html:130 | 无本面｜本合成模块范围（21 个文件）不触及「优惠券自动选择」所述对象｜通过 |
| BEQR-080 优惠券付款预留 | business-enum-quick-reference-v0.1.html:131 | 无本面｜本合成模块范围（21 个文件）不触及「优惠券付款预留」所述对象｜通过 |
| BEQR-081 正式挂起订单状态 | business-enum-quick-reference-v0.1.html:132 | 无本面｜本合成模块范围（21 个文件）不触及「正式挂起订单状态」所述对象｜通过 |
| BEQR-082 正式挂起订单规则 | business-enum-quick-reference-v0.1.html:133 | 无本面｜本合成模块范围（21 个文件）不触及「正式挂起订单规则」所述对象｜通过 |
| BEQR-083 挂单取消与付款互斥 | business-enum-quick-reference-v0.1.html:134 | 无本面｜本合成模块范围（21 个文件）不触及「挂单取消与付款互斥」所述对象｜通过 |
| BEQR-084 挂单坏账损耗 | business-enum-quick-reference-v0.1.html:135 | 无本面｜本合成模块范围（21 个文件）不触及「挂单坏账损耗」所述对象｜通过 |
| BEQR-085 项目开放状态 | business-enum-quick-reference-v0.1.html:140 | 无本面｜本合成模块范围（21 个文件）不触及「项目开放状态」所述对象｜通过 |
| BEQR-086 场次状态 | business-enum-quick-reference-v0.1.html:142 | 无本面｜本合成模块范围（21 个文件）不触及「场次状态」所述对象｜通过 |
| BEQR-087 游乐「当天／每天」口径 | business-enum-quick-reference-v0.1.html:144 | 无本面｜本合成模块范围（21 个文件）不触及「游乐「当天／每天」口径」所述对象｜通过 |
| BEQR-088 容量双阈值 | business-enum-quick-reference-v0.1.html:145 | 无本面｜本合成模块范围（21 个文件）不触及「容量双阈值」所述对象｜通过 |
| BEQR-089 园区容量 | business-enum-quick-reference-v0.1.html:146 | 无本面｜本合成模块范围（21 个文件）不触及「园区容量」所述对象｜通过 |
| BEQR-090 设备关键/辅助分级 | business-enum-quick-reference-v0.1.html:147 | 无本面｜设备关键/辅助分级未触及｜通过 |
| BEQR-091 全园故障与补偿通票 | business-enum-quick-reference-v0.1.html:148 | 无本面｜本合成模块范围（21 个文件）不触及「全园故障与补偿通票」所述对象｜通过 |
| BEQR-092 安全严重度 | business-enum-quick-reference-v0.1.html:149 | 无本面｜本合成模块范围（21 个文件）不触及「安全严重度」所述对象｜通过 |
| BEQR-093 巡检任务状态 | business-enum-quick-reference-v0.1.html:150 | 无本面｜本合成模块范围（21 个文件）不触及「巡检任务状态」所述对象｜通过 |
| BEQR-094 越界类型 | business-enum-quick-reference-v0.1.html:151 | 无本面｜本合成模块范围（21 个文件）不触及「越界类型」所述对象｜通过 |
| BEQR-095 团体/包场 | business-enum-quick-reference-v0.1.html:153 | 无本面｜本合成模块范围（21 个文件）不触及「团体/包场」所述对象｜通过 |
| BEQR-096 游乐增强阶段 | business-enum-quick-reference-v0.1.html:154 | 无本面｜本合成模块范围（21 个文件）不触及「游乐增强阶段」所述对象｜通过 |
| BEQR-111 经销支付方式 | business-enum-quick-reference-v0.1.html:176 | 无本面｜本合成模块范围（21 个文件）不触及「经销支付方式」所述对象｜通过 |
| BEQR-115 经销价格与税务快照 | business-enum-quick-reference-v0.1.html:180 | 无本面｜本合成模块范围（21 个文件）不触及「经销价格与税务快照」所述对象｜通过 |
| BEQR-118 运营营业日 | business-enum-quick-reference-v0.1.html:187 | 无本面｜本合成模块范围（21 个文件）不触及「运营营业日」所述对象｜通过 |
| BEQR-119 对账口径 | business-enum-quick-reference-v0.1.html:188 | 无本面｜本合成模块范围（21 个文件）不触及「对账口径」所述对象｜通过 |
| BEQR-120 对账差异四类 | business-enum-quick-reference-v0.1.html:189 | 无本面｜本合成模块范围（21 个文件）不触及「对账差异四类」所述对象｜通过 |
| BEQR-121 日结状态 | business-enum-quick-reference-v0.1.html:190 | 无本面｜本合成模块范围（21 个文件）不触及「日结状态」所述对象｜通过 |
| BEQR-122 差异处理状态 | business-enum-quick-reference-v0.1.html:191 | 无本面｜本合成模块范围（21 个文件）不触及「差异处理状态」所述对象｜通过 |
| BEQR-123 结算状态 | business-enum-quick-reference-v0.1.html:192 | 无本面｜本合成模块范围（21 个文件）不触及「结算状态」所述对象｜通过 |
| BEQR-124 综合通票定价 | business-enum-quick-reference-v0.1.html:193 | 无本面｜本合成模块范围（21 个文件）不触及「综合通票定价」所述对象｜通过 |
| BEQR-125 商户业态 | business-enum-quick-reference-v0.1.html:195 | 无本面｜本合成模块范围（21 个文件）不触及「商户业态」所述对象｜通过 |
| BEQR-126 合作模式 | business-enum-quick-reference-v0.1.html:196 | 无本面｜本合成模块范围（21 个文件）不触及「合作模式」所述对象｜通过 |
| BEQR-127 退款资金流向 | business-enum-quick-reference-v0.1.html:197 | 无本面｜本合成模块范围（21 个文件）不触及「退款资金流向」所述对象｜通过 |
| BEQR-128 物业费 | business-enum-quick-reference-v0.1.html:198 | 无本面｜本合成模块范围（21 个文件）不触及「物业费」所述对象｜通过 |
| BEQR-129 能源费与损耗费 | business-enum-quick-reference-v0.1.html:199 | 无本面｜本合成模块范围（21 个文件）不触及「能源费与损耗费」所述对象｜通过 |
| BEQR-130 税费配置 | business-enum-quick-reference-v0.1.html:201 | 无本面｜本合成模块范围（21 个文件）不触及「税费配置」所述对象｜通过 |
| BEQR-131 三方对账框架 | business-enum-quick-reference-v0.1.html:202 | 无本面｜本合成模块范围（21 个文件）不触及「三方对账框架」所述对象｜通过 |
| BEQR-132 开票状态 | business-enum-quick-reference-v0.1.html:203 | 无本面｜本合成模块范围（21 个文件）不触及「开票状态」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（21 个文件）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 适用｜经营主体关系：business_entity_id 字段缺中文名，接口报错回退为「提交内容有误」（CX-09）｜不通过（技术性，见 CX-09） |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（21 个文件）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（21 个文件）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（21 个文件）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（21 个文件）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 无本面｜本合成模块范围（21 个文件）不触及「职级」所述对象｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 适用｜终端十一端：拦截器按服务端派生身份判定，测试须带身份（CX-02）｜通过（问题在测试） |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 无本面｜本合成模块范围（21 个文件）不触及「最终权限计算」所述对象｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 适用｜项目与敏感数据权限：平台管理员全量（D-032）；最高权限逐笔见 CX-07｜通过（问题并入 CX-07） |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 无本面｜本合成模块范围（21 个文件）不触及「终端开关优先级」所述对象｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（21 个文件）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（21 个文件）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（21 个文件）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（21 个文件）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（21 个文件）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 无本面｜本合成模块范围（21 个文件）不触及「权限状态」所述对象｜通过 |
| BEQR-160 招商线索状态 | business-enum-quick-reference-v0.1.html:248 | 无本面｜本合成模块范围（21 个文件）不触及「招商线索状态」所述对象｜通过 |
| BEQR-161 招商意向阶段 | business-enum-quick-reference-v0.1.html:249 | 无本面｜本合成模块范围（21 个文件）不触及「招商意向阶段」所述对象｜通过 |
| BEQR-162 品牌/客户库类型 | business-enum-quick-reference-v0.1.html:250 | 无本面｜本合成模块范围（21 个文件）不触及「品牌/客户库类型」所述对象｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（21 个文件）不触及「合同变更类型」所述对象｜通过 |
| BEQR-164 退租结算状态 | business-enum-quick-reference-v0.1.html:252 | 无本面｜本合成模块范围（21 个文件）不触及「退租结算状态」所述对象｜通过 |
| BEQR-165 内部应付执行状态 | business-enum-quick-reference-v0.1.html:253 | 无本面｜本合成模块范围（21 个文件）不触及「内部应付执行状态」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（21 个文件）不触及「合同审批链」所述对象｜通过 |
| BEQR-167 租金/账单类型 | business-enum-quick-reference-v0.1.html:256 | 无本面｜本合成模块范围（21 个文件）不触及「租金/账单类型」所述对象｜通过 |
| BEQR-168 营业额抽成模式 | business-enum-quick-reference-v0.1.html:257 | 无本面｜本合成模块范围（21 个文件）不触及「营业额抽成模式」所述对象｜通过 |
| BEQR-169 公区能源费 | business-enum-quick-reference-v0.1.html:258 | 无本面｜本合成模块范围（21 个文件）不触及「公区能源费」所述对象｜通过 |
| BEQR-170 铺位租赁状态 | business-enum-quick-reference-v0.1.html:259 | 无本面｜本合成模块范围（21 个文件）不触及「铺位租赁状态」所述对象｜通过 |
| BEQR-171 物业工单类型 | business-enum-quick-reference-v0.1.html:260 | 无本面｜本合成模块范围（21 个文件）不触及「物业工单类型」所述对象｜通过 |
| BEQR-172 物业工单状态 | business-enum-quick-reference-v0.1.html:262 | 无本面｜本合成模块范围（21 个文件）不触及「物业工单状态」所述对象｜通过 |
| BEQR-173 商户服务工单状态 | business-enum-quick-reference-v0.1.html:264 | 无本面｜本合成模块范围（21 个文件）不触及「商户服务工单状态」所述对象｜通过 |
| BEQR-174 工单时效 | business-enum-quick-reference-v0.1.html:265 | 无本面｜本合成模块范围（21 个文件）不触及「工单时效」所述对象｜通过 |
| BEQR-175 楼宇机电设备状态 | business-enum-quick-reference-v0.1.html:266 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇机电设备状态」所述对象｜通过 |
| BEQR-176 设备维保类型 | business-enum-quick-reference-v0.1.html:267 | 无本面｜本合成模块范围（21 个文件）不触及「设备维保类型」所述对象｜通过 |
| BEQR-177 能耗表计类型 | business-enum-quick-reference-v0.1.html:268 | 无本面｜本合成模块范围（21 个文件）不触及「能耗表计类型」所述对象｜通过 |
| BEQR-178 抄表状态 | business-enum-quick-reference-v0.1.html:269 | 无本面｜本合成模块范围（21 个文件）不触及「抄表状态」所述对象｜通过 |
| BEQR-179 能耗归集对象 | business-enum-quick-reference-v0.1.html:270 | 无本面｜本合成模块范围（21 个文件）不触及「能耗归集对象」所述对象｜通过 |
| BEQR-180 楼宇巡检任务状态 | business-enum-quick-reference-v0.1.html:271 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇巡检任务状态」所述对象｜通过 |
| BEQR-181 楼宇巡检类型 | business-enum-quick-reference-v0.1.html:273 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇巡检类型」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（21 个文件）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（21 个文件）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（21 个文件）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（21 个文件）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（21 个文件）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（21 个文件）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（21 个文件）不触及「管理费方式」所述对象｜通过 |
| BEQR-192 分红税务 | business-enum-quick-reference-v0.1.html:298 | 无本面｜本合成模块范围（21 个文件）不触及「分红税务」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（21 个文件）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（21 个文件）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（21 个文件）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 适用｜「删除一律须由具备最高权限的人审批通过后执行，不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」（第 570 行配置数据、第 577 行业务记录含设施设备、员工档案）：设施与游乐审批码可授给他人、最高权限发起仍走待审批（CX-05/06）；员工档案最高权限审批被范围检查挡住（CX-07）｜不通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（21 个文件）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（21 个文件）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 适用｜平台管理员与最高权限是两个角色；平台管理员拥有全部日常功能权限——删除审批不是日常功能，ceca1a54 不把 critical 审批码随平台管理员角色放行，符合；测试把平台管理员当审批人已过期｜通过（问题在测试） |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（21 个文件）不触及「基础管理」所述对象｜通过 |
| BSM-011 客人端 | business-system-map-for-operations-v0.1.html:606 | 无本面｜本合成模块范围（21 个文件）不触及「客人端」所述对象｜通过 |
| BSM-012 酒吧平板/工作手机 | business-system-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（21 个文件）不触及「酒吧平板/工作手机」所述对象｜通过 |
| BSM-014 酒吧管理 | business-system-map-for-operations-v0.1.html:621 | 无本面｜本合成模块范围（21 个文件）不触及「酒吧管理」所述对象｜通过 |
| BSM-016 游乐管理 | business-system-map-for-operations-v0.1.html:631 | 适用｜游乐项目权益绑定属配置数据，删除与墓碑恢复见 BSM-004／CX-06｜通过（问题并入 CX-06） |
| BSM-017 商户管理 | business-system-map-for-operations-v0.1.html:637 | 无本面｜本合成模块范围（21 个文件）不触及「商户管理」所述对象｜通过 |
| BSM-019 物业服务 | business-system-map-for-operations-v0.1.html:650 | 无本面｜本合成模块范围（21 个文件）不触及「物业服务」所述对象｜通过 |
| BSM-020 楼宇运营 | business-system-map-for-operations-v0.1.html:656 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇运营」所述对象｜通过 |
| BSM-023 财务对账 | business-system-map-for-operations-v0.1.html:671 | 适用｜财务对账控制器 12 条测试因上下文加载失败全灭，入口权限断言失守（CX-01）｜不通过（随 CX-01） |
| BSM-026 各类入口 | business-system-map-for-operations-v0.1.html:686 | 无本面｜本合成模块范围（21 个文件）不触及「各类入口」所述对象｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（21 个文件）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 适用｜最高权限审批链：见 BSM-004；与本范围其余审批无关｜通过（问题并入 BSM-004） |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-034 核销、退款与有效期规则 | business-system-map-for-operations-v0.1.html:796 | 无本面｜本合成模块范围（21 个文件）不触及「核销、退款与有效期规则」所述对象｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 适用｜储值扣款终端开关能力码仍挂在 payTicketOrder 上；测试点名的 consume 方法已不存在（CX-03）｜通过（问题在测试） |
| BSM-037 客人端：客人自己买票、预定、查询、积分和出示二维码 | business-system-map-for-operations-v0.1.html:850 | 无本面｜本合成模块范围（21 个文件）不触及「客人端：客人自己买票、预定、查询、积分和出示二维码」所述对象｜通过 |
| BSM-038 主要功能 | business-system-map-for-operations-v0.1.html:854 | 适用｜储值：见 BSM-035｜通过 |
| BSM-039 和谁有关 | business-system-map-for-operations-v0.1.html:873 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-040 登录与关键规则 | business-system-map-for-operations-v0.1.html:883 | 无本面｜本合成模块范围（21 个文件）不触及「登录与关键规则」所述对象｜通过 |
| BSM-041 会员注册、认证与收费边界 | business-system-map-for-operations-v0.1.html:898 | 无本面｜本合成模块范围（21 个文件）不触及「会员注册、认证与收费边界」所述对象｜通过 |
| BSM-042 会员积分活动与发券 | business-system-map-for-operations-v0.1.html:907 | 无本面｜本合成模块范围（21 个文件）不触及「会员积分活动与发券」所述对象｜通过 |
| BSM-043 会员营销明确边界 | business-system-map-for-operations-v0.1.html:916 | 无本面｜本合成模块范围（21 个文件）不触及「会员营销明确边界」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（21 个文件）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 适用｜最高权限破窗会话账号类型 master_authority、所属公司 platform-company（ProductionAuthService:411）；进员工档案范围检查即被按公司口径拒绝（CX-07）｜不通过（随 CX-07） |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（21 个文件）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-051 酒吧管理：从入场、开台到结账关台 | business-system-map-for-operations-v0.1.html:1010 | 无本面｜本合成模块范围（21 个文件）不触及「酒吧管理：从入场、开台到结账关台」所述对象｜通过 |
| BSM-052 主要功能 | business-system-map-for-operations-v0.1.html:1014 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-053 和谁有关 | business-system-map-for-operations-v0.1.html:1041 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-054 桌台/账单状态与经营规则 | business-system-map-for-operations-v0.1.html:1053 | 无本面｜本合成模块范围（21 个文件）不触及「桌台/账单状态与经营规则」所述对象｜通过 |
| BSM-055 桌台、菜单与优惠券 | business-system-map-for-operations-v0.1.html:1088 | 无本面｜本合成模块范围（21 个文件）不触及「桌台、菜单与优惠券」所述对象｜通过 |
| BSM-056 正式未付款订单挂起 | business-system-map-for-operations-v0.1.html:1101 | 无本面｜本合成模块范围（21 个文件）不触及「正式未付款订单挂起」所述对象｜通过 |
| BSM-057 服务点单：客人点专项服务，公司统一收款、再按名录分账 | business-system-map-for-operations-v0.1.html:1120 | 无本面｜本合成模块范围（21 个文件）不触及「服务点单：客人点专项服务，公司统一收款、再按名录分账」所述对象｜通过 |
| BSM-058 怎么点、怎么收钱 | business-system-map-for-operations-v0.1.html:1124 | 无本面｜本合成模块范围（21 个文件）不触及「怎么点、怎么收钱」所述对象｜通过 |
| BSM-059 钱怎么分、怎么退 | business-system-map-for-operations-v0.1.html:1135 | 无本面｜本合成模块范围（21 个文件）不触及「钱怎么分、怎么退」所述对象｜通过 |
| BSM-060 供应链库存：管供应商、采购、库房、消耗和存酒 | business-system-map-for-operations-v0.1.html:1151 | 无本面｜本合成模块范围（21 个文件）不触及「供应链库存：管供应商、采购、库房、消耗和存酒」所述对象｜通过 |
| BSM-061 主要功能 | business-system-map-for-operations-v0.1.html:1155 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-062 和谁有关 | business-system-map-for-operations-v0.1.html:1174 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-063 库存类型与规则 | business-system-map-for-operations-v0.1.html:1186 | 无本面｜本合成模块范围（21 个文件）不触及「库存类型与规则」所述对象｜通过 |
| BSM-064 采购建议与门店要货 | business-system-map-for-operations-v0.1.html:1208 | 无本面｜本合成模块范围（21 个文件）不触及「采购建议与门店要货」所述对象｜通过 |
| BSM-065 外部经销商准入、价格与库存 | business-system-map-for-operations-v0.1.html:1216 | 无本面｜本合成模块范围（21 个文件）不触及「外部经销商准入、价格与库存」所述对象｜通过 |
| BSM-066 经销订单、付款与履约 | business-system-map-for-operations-v0.1.html:1226 | 无本面｜本合成模块范围（21 个文件）不触及「经销订单、付款与履约」所述对象｜通过 |
| BSM-067 经销退货、退款、红冲与资金归属 | business-system-map-for-operations-v0.1.html:1238 | 无本面｜本合成模块范围（21 个文件）不触及「经销退货、退款、红冲与资金归属」所述对象｜通过 |
| BSM-068 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | business-system-map-for-operations-v0.1.html:1253 | 无本面｜本合成模块范围（21 个文件）不触及「游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常」所述对象｜通过 |
| BSM-069 主要功能 | business-system-map-for-operations-v0.1.html:1257 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-070 和谁有关 | business-system-map-for-operations-v0.1.html:1276 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-071 游乐类型与规则 | business-system-map-for-operations-v0.1.html:1287 | 无本面｜本合成模块范围（21 个文件）不触及「游乐类型与规则」所述对象｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（21 个文件）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-078 物业服务：商户和现场的报修、投诉、整改都进工单 | business-system-map-for-operations-v0.1.html:1370 | 无本面｜本合成模块范围（21 个文件）不触及「物业服务：商户和现场的报修、投诉、整改都进工单」所述对象｜通过 |
| BSM-079 主要功能 | business-system-map-for-operations-v0.1.html:1374 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-080 和谁有关 | business-system-map-for-operations-v0.1.html:1388 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-081 楼宇运营：机电设备、能耗抄表、楼宇巡检 | business-system-map-for-operations-v0.1.html:1401 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇运营：机电设备、能耗抄表、楼宇巡检」所述对象｜通过 |
| BSM-082 主要功能 | business-system-map-for-operations-v0.1.html:1405 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-083 和谁有关 | business-system-map-for-operations-v0.1.html:1414 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 适用｜员工档案属员工手机端对应的业务记录，删除审批见 CX-07｜通过（问题并入 CX-07） |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（21 个文件）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（21 个文件）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（21 个文件）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（21 个文件）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-098 财务对账：把经营事实整理成财务能确认的口径 | business-system-map-for-operations-v0.1.html:1636 | 适用｜同 BSM-023｜通过（问题并入 CX-01） |
| BSM-099 内部结算价目录（按商品规格逐项建目录） | business-system-map-for-operations-v0.1.html:1639 | 无本面｜本合成模块范围（21 个文件）不触及「内部结算价目录（按商品规格逐项建目录）」所述对象｜通过 |
| BSM-100 结算配置中心（后台总控页） | business-system-map-for-operations-v0.1.html:1641 | 适用｜财务控制器测试装配过期（CX-01），不影响结算配置业务｜通过 |
| BSM-101 项目零售参考价 | business-system-map-for-operations-v0.1.html:1643 | 无本面｜本合成模块范围（21 个文件）不触及「项目零售参考价」所述对象｜通过 |
| BSM-102 主要功能 | business-system-map-for-operations-v0.1.html:1648 | 适用｜同 BSM-023｜通过（问题并入 CX-01） |
| BSM-103 和谁有关 | business-system-map-for-operations-v0.1.html:1666 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-104 收入归属：哪类收入归哪个业态主体 | business-system-map-for-operations-v0.1.html:1678 | 无本面｜本合成模块范围（21 个文件）不触及「收入归属：哪类收入归哪个业态主体」所述对象｜通过 |
| BSM-105 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | business-system-map-for-operations-v0.1.html:1697 | 无本面｜本合成模块范围（21 个文件）不触及「三方对账框架：系统流水、渠道对账单、资金到账两两核对」所述对象｜通过 |
| BSM-106 对账与结算类型规则 | business-system-map-for-operations-v0.1.html:1709 | 适用｜同 BSM-023｜通过（问题并入 CX-01） |
| BSM-111 经营看板：让管理层和运营看得懂、能追问 | business-system-map-for-operations-v0.1.html:1781 | 无本面｜本合成模块范围（21 个文件）不触及「经营看板：让管理层和运营看得懂、能追问」所述对象｜通过 |
| BSM-112 主要指标 | business-system-map-for-operations-v0.1.html:1785 | 无本面｜本合成模块范围（21 个文件）不触及「主要指标」所述对象｜通过 |
| BSM-113 谁会看 | business-system-map-for-operations-v0.1.html:1813 | 无本面｜本合成模块范围（21 个文件）不触及「谁会看」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜各入口按范围可见：最高权限逐笔开放在员工档案审批不可用（CX-07）；其余范围检查对最高权限的系统性问题登记 XM｜不通过（随 CX-07） |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（21 个文件）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | business-system-map-for-operations-v0.1.html:1928 | 无本面｜本合成模块范围（21 个文件）不触及「系统交付与上线：正式上线要做的安全加固和安装交付」所述对象｜通过 |
| BSM-117 正式上线时的安全加固 | business-system-map-for-operations-v0.1.html:1932 | 适用｜正式上线安全加固：终端写请求兜底拒绝保留｜通过 |
| BSM-118 能自动识别环境的安装包 + 内附教程 | business-system-map-for-operations-v0.1.html:1940 | 无本面｜本合成模块范围（21 个文件）不触及「能自动识别环境的安装包 + 内附教程」所述对象｜通过 |
| BSM-119 和谁有关 | business-system-map-for-operations-v0.1.html:1947 | 适用｜最高权限与破窗：见 BSM-045｜通过（问题并入 CX-07） |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 适用｜已确认规则：终端开关与最高权限规则见 CX-02、CX-07｜通过（问题并入各条） |
| GEN-001 必要性与最小性 | 通用 | 适用｜各处置只改红用例所需：测试同步 4 类、字段标签 1 行、主键拼法 1 处；实现缺陷只按 HTML 原句收（CX-05/06/07）；系统性最高权限范围问题登记不扩写｜通过（本项不单独致 FAIL） |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main 4f233e7b 后端全量 23 红＋时红时绿 1（§2）；员工档案删除在生产上最高权限批不了（CX-07），设施与游乐删除审批人不限最高权限（CX-05/06）｜不通过（见 CX-05…07） |
| GEN-003 测试有效性 | 通用 | 适用｜23 条红里 17 条是测试过期（Finance 12、Compliance 3、储值注解 1、Amusement 1 的平台管理员部分），测试未随 ceca1a54／8d2fafff 同步；探针按最小处置转绿（xm036a_probe.log）｜不通过（见 CX-01…04） |
| GEN-004 信任边界校验 | 通用 | 适用｜终端写请求兜底拒绝（8d2fafff）是正确的信任边界，测试须带服务端派生的终端身份而非绕开拦截器｜通过（问题在测试，CX-02） |
| GEN-005 授权与最小权限 | 通用 | 适用｜ceca1a54 起平台管理员角色不再带 critical 审批码（M006-I9-01）属正确最小权限；删除审批须限最高权限本人（BSM 第 570、577 行），设施与游乐未限（CX-05/06）；最高权限在员工档案逐笔审批被范围检查拦住（CX-07）｜不通过（见 CX-05…07） |
| GEN-006 数据完整性 | 通用 | 适用｜空间种类删除申请主键同毫秒撞键抛 DuplicateKeyException（CX-08），不写坏数据｜不通过（技术性，见 CX-08） |
| GEN-007 并发与幂等 | 通用 | 适用｜同一种类在同一毫秒内两次申请（测试中申请→完成→再申请）即撞键；幂等键另有，主键不该带时间戳碰撞面｜不通过（见 CX-08） |
| GEN-008 接口与模式一致性 | 通用 | 适用｜控制器构造依赖（ComplianceController 需 TerminalSwitchGuard、FinanceController 需 StoredValueService）与切片测试装配不一致｜不通过（测试侧，CX-01/02） |
| GEN-009 错误处理与恢复 | 通用 | 适用｜员工档案：最高权限走「所属公司」分支，生产报 CROSS_ORG_FORBIDDEN、测试报「请填写所属公司」，均非可理解的业务拒绝（CX-07）｜不通过（见 CX-07） |
| GEN-010 安全与敏感信息 | 通用 | 适用｜未见敏感信息暴露；CSRF 双提交未参与这 3 条 403｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜删除审批与恢复均有审计事件；最高权限逐笔开放的留痕由审批事件承载｜通过 |
| GEN-012 资源与性能边界 | 通用 | 无本面｜无性能面改动｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 适用｜spring-test 7.0.8 自带 MockitoBean，无需新依赖｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜不需迁移（主键为应用侧生成）｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜不涉界面｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜AmusementService 注释「墓碑复活…按同级最高权限把守」与实现（可授出的审批码）不一致；测试注释「最高权限 role_platform_admin 持有」把两个角色混为一谈（CX-04/06）｜不通过（随 CX-04/06） |
| GEN-017 业务来源分类 | 通用 | 适用｜CX-01…04、08、09 技术性；CX-05/06/07 冲突（BSM 第 570、577 行原句）；XM 登记另列｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜每条根因落到提交、源码行、日志与探针｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜治理底座：见 PGM-041、PGM-010｜通过（问题并入各条） |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 适用｜治理顺序：终端开关先于业务写（8d2fafff）｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（21 个文件）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（21 个文件）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（21 个文件）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 无本面｜本合成模块范围（21 个文件）不触及「商户和主体」所述对象｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（21 个文件）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 适用｜员工和手机：员工档案删除审批见 CX-07｜通过（问题并入 CX-07） |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 适用｜权限和职级：见 PGM-040/041｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 适用｜终端开关：「关闭后……接口也要拒绝业务动作」「开关拉取失败时按兜底关闭处理，写操作一律拒绝」（第 652、655 行）——拦截器正确，合规控制器测试未带终端身份（CX-02）｜通过（问题在测试） |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 适用｜设备管理：设施设备删除审批见 CX-05｜通过（问题并入 CX-05） |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（21 个文件）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（21 个文件）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜留痕复核：删除审批留痕在，审批人身份不合 BSM-004（CX-05/06）｜通过（问题并入 CX-05/06） |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 适用｜应急：最高权限会话逐笔操作被范围检查拦截（CX-07＋XM）｜通过（问题并入 CX-07） |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（21 个文件）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（21 个文件）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（21 个文件）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（21 个文件）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（21 个文件）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（21 个文件）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（21 个文件）不触及「业务边界」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（21 个文件）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（21 个文件）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（21 个文件）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（21 个文件）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（21 个文件）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-037 主要功能 | platform-governance-map-for-operations-v0.1.html:615 | 无本面｜本合成模块范围（21 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（21 个文件）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-039 智能设备场景联动范围 | platform-governance-map-for-operations-v0.1.html:636 | 无本面｜本合成模块范围（21 个文件）不触及「智能设备场景联动范围」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 适用｜单一功能单独授权：删除审批码可单独授出，与 BSM-004「不设例外」冲突（CX-05/06）｜通过（问题并入 CX-05/06） |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 适用｜权限规则：平台管理员 critical 码不随角色放行（ceca1a54）；最高权限逐笔开放（第 528 行第④类）在员工档案审批与游乐墓碑恢复路径不可用（CX-06/07）｜不通过（随 CX-06/07） |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 无本面｜本合成模块范围（21 个文件）不触及「职级规则」所述对象｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜本合成模块范围（21 个文件）不触及「外包人员与审计人员（2026-06-29 新增）」所述对象｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（21 个文件）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（21 个文件）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-052 楼宇运营：管好建筑本身的设备、能耗和巡检 | platform-governance-map-for-operations-v0.1.html:806 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇运营：管好建筑本身的设备、能耗和巡检」所述对象｜通过 |
| PGM-053 楼宇机电设备 | platform-governance-map-for-operations-v0.1.html:810 | 适用｜楼宇机电设备：设施设备删除见 CX-05｜通过（问题并入 CX-05） |
| PGM-054 能耗计量 | platform-governance-map-for-operations-v0.1.html:821 | 无本面｜本合成模块范围（21 个文件）不触及「能耗计量」所述对象｜通过 |
| PGM-055 楼宇巡检 | platform-governance-map-for-operations-v0.1.html:832 | 无本面｜本合成模块范围（21 个文件）不触及「楼宇巡检」所述对象｜通过 |
| PGM-056 和谁有关 | platform-governance-map-for-operations-v0.1.html:844 | 无本面｜本合成模块范围（21 个文件）不触及「和谁有关」所述对象｜通过 |
| PGM-057 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | platform-governance-map-for-operations-v0.1.html:857 | 无本面｜本合成模块范围（21 个文件）不触及「应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文」所述对象｜通过 |
| PGM-058 紧急恢复（破窗机制） | platform-governance-map-for-operations-v0.1.html:860 | 适用｜破窗：见 BSM-045｜通过（问题并入 CX-07） |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜本合成模块范围（21 个文件）不触及「客人与员工的个人信息保护」所述对象｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（21 个文件）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（21 个文件）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 适用｜已拍板规则：删除审批限最高权限（BSM-004）｜通过（问题并入 CX-05/06） |

共 297 项；逐项给出适用与结论的 55 项，其余 242 项无本面。

## 8. 结论

席甲 B1 票：**FAIL**（CX-01…09；登记①② 与观察另列）。范围全读、297 项逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## XM-M036-A B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-24T03:40:06Z。输入：两席 B1 完整 raw（席甲 V0.01＝上方本报告、Grok modules/XM-M036-A-b1-grok-raw.md V0.01）与候选 findings.md V3.99 顶部「当前 XM-M036-A B2-r1 整批候选」段（全文件 SHA256 583a1ce64eb1c3c560af99b183781719d1225a053cd207ea0919994dc3601d07）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01＋CX-01→001；GR-02＋CX-02→002；GR-05＋CX-03→003；GR-06＋CX-09→004；GR-03＋CX-05＋GR-09 设施部分→005；GR-04＋CX-04＋CX-06＋GR-09 游乐部分→006；GR-07＋CX-07→007；GR-08＋CX-08→008；GR-09 工单、预订与本席登记②合并为只登记 XM-M036A-2；本席登记①→XM-M036A-1（Grok B1 未提）。无漏并、无改写。
- 自审纠正：B1 对抗段称「CustomerService.java:81 自己也有必需的字段注入，补 mock 会连锁」，经核 :81 的 `@Autowired` 标在构造器上，此说不成立；Grok 补两个 mock 的处置同样可行。定稿仍取探针已实跑转绿的 `@MockitoBean`，理由是有执行证据，不是另一写法不行。今后引行号下结论前先读该行上下文。
- 对 Grok 的两处保留（请复核）：
  - 006：GR-04 称最高权限在墓碑恢复已被短路放行、只改测试即可。权限闸确短路，但写入体先过 checkProjectOwnership → currentMerchantScope，最高权限无商户号即抛 MERCHANT_SCOPE_REQUIRED；探针改由最高权限恢复后仍红，栈见 /home/james/audit-tmp/xm036a_probe.msgs。只改测试转不了绿。
  - 007：GR-07「requireProfileInScope 豁免加 isMasterAuthority」会把最高权限送进 else 分支 requireRegistrationScope（StaffService:1193-1201），对非平台管理员比较 requireProjectScope(actor)，生产最高权限为 platform-company，仍 403；且该方法被十余处单据读写共用，整体豁免会让最高权限无留痕打开全部员工档案读路径。本席取只放行两条删除路径的窄处置。
- 范围取舍：GR-09 拟在本模块改商户服务工单与卡座预订。二者不在开工段 21 个文件的单元集内、也不在红名单；同句入口还有酒吧商品目录、票务、商户档案、服务目录、数据清理共十余处。只改其中两处会让同一句 HTML 在各模块里口径不一，按「写入集合以模块单元集为界」整族登记 XM-M036A-2，各模块串行到达时按第 570、577 行统一处置。设施与游乐在单元集内且有红，按同句在本模块改（005、006）。
- 分类：001/002/003 技术性；004 采 Grok 细化（BEQR-144「经营主体关系」），标签用语采 Grok「经营主体」；005/006/007 冲突（BSM 第 570、577 行原句；007 另有第 528 行第④类）；008 采 Grok 细化（删除申请留历史的数据完整性）。
- 最小性：001–004 各只改一处测试或一行标签；005–007 照已审过的 StaffService 同款写法，只改删除审批、墓碑恢复、申请删除三条路径，不动 AuthorizationService 与其余写路径；008 只改主键拼法。系统性最高权限范围问题不在本批扩写。
- 无添加项、无 questions、不 BLOCKED。
- 席甲 B2-r1 票：**PASS**（同意候选整批：001…008 入队，XM-M036A-1、XM-M036A-2 与观察只登记）。
