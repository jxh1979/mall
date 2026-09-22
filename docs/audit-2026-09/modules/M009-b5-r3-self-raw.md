CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r3 修复后全模块重审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B5-r3（B3 第 3 次整批修复后的整模块全量三线重审）。2026-09-09T10:03:19Z
- 正本 HEAD 242f487233d028173cb9198eecd079f5d877405c；本报告落盘时工作区 tree `5c4882dca3ffb1b7f2fcc98af81d662b1225b765`（含 B3-r1/r2/r3 修复与 B4-r1/r2/r3 两席 raw）。锁四值：实算 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a（sidecar/ledger 逐字相同；精确 Grok B5 raw 的最后独占锁行在读取 Grok 报告后于回执核对）。
- 顺序：Grok B5-r3 已真实退出（受理四项事实见回执）；本报告在读取 `M009-b5-r3-grok-raw.md` 任何正文之前落盘（该文件此刻存在=True，未读）。
- 只读承诺：本席 B5 未改任何源/HTML/锁/台账；测试在主仓运行、不改源；数据库探针全部 SELECT。

## 1. 文件覆盖表（20 文件当前内容，全文逐行读完；11 个未改文件 SHA 与 B1 相同并再次全读）

| 文件 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| governance/BusinessEntityRecord.java | 17 | 391 | 45431c580d0bca0c… |
| governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de8ed4d451… |
| governance/MerchantRecord.java | 90 | 5490 | 66a840d9b71935b1… |
| governance/MerchantDeleteDecision.java | 9 | 400 | e08618c856b35a1e… |
| governance/JdbcPlatformRepository.java | 1112 | 61986 | 10efae64b8dce983… |
| governance/PlatformController.java | 236 | 13580 | 05bd7cf4a2bfcb43… |
| governance/PlatformRepository.java | 134 | 6645 | 09f4c02728bd7e61… |
| governance/PlatformService.java | 876 | 43323 | 720b13d917433231… |
| governance/package-info.java | 1 | 41 | e3094c51b508bf10… |
| governance/dto/BusinessEntityBindingRequest.java | 17 | 442 | 66d4a8cd5c7f206f… |
| governance/dto/BusinessEntityRequest.java | 17 | 424 | 8c4b7795909a6dc5… |
| governance/dto/MerchantArchiveRequest.java | 6 | 319 | e0f16f5417b61e80… |
| governance/dto/MerchantDeleteApprovalRequest.java | 5 | 225 | 2516fb1d78e082fe… |
| governance/dto/MerchantDeleteRequest.java | 5 | 214 | 4898e50f2150e27c… |
| governance/dto/MerchantProfileRequest.java | 27 | 767 | cbcf242470823799… |
| governance/dto/ProjectRequest.java | 17 | 375 | 8316db69c16d3013… |
| governance/dto/SpaceRequest.java | 21 | 525 | 63857df14972fd69… |
| governance/dto/SpaceTypeRequest.java | 15 | 457 | d181151ff1b58851… |
| governance/dto/StatusChangeRequest.java | 11 | 231 | 75b75cfaee897096… |
| governance/dto/VenueBusinessHoursRequest.java | 22 | 700 | 6347c6dfe087be23… |

## 2. 可执行证据（本席实跑，/tmp/m009_b5r3_run.log 原文）

```
START=2026-09-09T10:02:50Z
TREE=5c4882dca3ffb1b7f2fcc98af81d662b1225b765
=== BACKEND: governance.*Tests + leasing.*Tests ===
BACKEND_EXIT=1
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 4.078 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.010 s <<< FAILURE!
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.101 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.086 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.835 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.079 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.048 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.004 s <<< FAILURE!
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] Tests run: 77, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
=== WEB: OrganizationMerchantPage vitest ===
WEB_EXIT=0
[2m Test Files [22m [1m[32m1 passed[39m[22m[90m (1)[39m
TREE_AFTER=5c4882dca3ffb1b7f2fcc98af81d662b1225b765
END=2026-09-09T10:03:00Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文重新推导本模块业务约束（BSM-004 存档/删除/最高权限/「存档（先下线）…不再被新业务选用」、BEQR-143 四类主体、BEQR-148 合作方式六类、BEQR-154/155 状态、PGM-018/019/026/027 主体角色与统一绑定），逐条对照当前实现（不依赖 B1–B5-r1 结论）。
- 顺从式线：87 冻结判据逐项（§5 增量表 + B1 报告 §5 全表 + B5-r1 §5；未变化者沿用并再次核对）。
- 对抗式线：针对 B3-r1/B3-r2/B3-r3 修复面构造反例（静态追流 + `complex_test` 只读探针 + 全套测试实跑）：LIMIT 500 存在性、`findSpaceById` 空参数（`Map.of` 不接受 null）、已存档商户复用、六张表全部 CHECK 约束与服务层白名单逐一对照（B5-r2 发现 `spaces.status` V203 CHECK 未前置，已由 036 修复；本轮再对照 NOT NULL/长度约束与服务层校验）、空状态覆盖、双写、残留注释、无引用 import、绑定主体状态、非最高权限驳回、越权读端、幻影创建、悬空引用。
- 依赖只读追流（不审）：`PlatformRepository` 默认方法与 Jdbc 覆写、`leasing/LeasingService:288-292,424`（`spaceExists` 消费方）、`contract/ContractService:261,954`（`findSpaceById` 治理外消费方：两处入参分别经 `requireText` 与同一 `resolved` 列表，均非 null）、`access/AuthorizationService.isMasterAuthority`、M011 三测试与 `LeasingSignAtomicityJdbcIntegrationTests` 夹具、`apps/web/.../OrganizationMerchantPage.tsx`。
- 20 文件覆盖：18 个文件 SHA 与 B5-r2 相同（再次全读）；B3-r3 只改 PlatformService（V0.03）与 M011 PlatformServiceTests，按当前内容全读，改动块与 B3-r3 diff（4472 bytes SHA eecbbb6c…）逐字对照。

## 4. 已定稿 37 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001–012、014–016 | 否（第一批已修） | 与 B5-r1 §4 相同；B3-r2/r3 未触及其行为 | 实跑：ServiceTests 31/31、ControllerTests 10/10、JDBC 13 中 12 通过（唯一红 017）、Leasing 11/11 + 2/2 + 4 中 3（017）、页面 12/12 |
| 026 | 否（已修） | `JdbcPlatformRepository:187`、`MerchantRecord:27`、`PlatformRepository:54` 注释改软删除口径 | 20 文件 `硬删|物理删除` grep 0 处 |
| 027 | 否（已修） | Jdbc 覆写 `findSpaceById` `WHERE id = :id`；`listSpaces()` LIMIT 500 不变 | `findSpaceByIdIsNotTruncatedByListLimit`（501 条）实跑 PASS；空参数：全部 4 个调用点（`spaceExists:407`、`createSpace:486`、`ContractService:261/954`）入参均先判非空或经 `requireText`，`Map.of` 不会收到 null |
| 028 | 否（已修） | `requireActiveMerchant` 用于 `createProject`/`createSpace`/`createBusinessEntityBinding`；其余路径仍 `requireMerchant` | `archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 实跑 PASS；`updateMerchantKeepsArchiveAndPendingDeleteState`、`approveDelete…`、`requestDeleteByMasterAuthority…` 仍 PASS（已存档行可更新/删除） |
| 036 | 否（已修 B3-r3） | `SPACE_STATUSES`≡V203 二值；`requireSpaceStatus` 只用于 `createSpace`；文案「可用/不可用」未引入未映射六态 | `createSpaceRejectsStatusOutsideCheckSet` 实跑 PASS；`createSpaceType`/029 字典部分未动 |
| 013、017–025、029–035、037 | 否（登记） | 内容未动；017 环境红仍在（flyway 409 > 399）；037 激活既有绑定不走 `requireActiveMerchant`（028 最小边界，外审维持否） | 与定稿一致 |

## 5. 87 判据增量核对（相对 B5-r2 §5 表；未列者结论不变）

| 判据 | B5-r2 结论 | 当前结论 |
|---|---|---|
| GEN-004 信任边界 / GEN-009 错误处理 | 残留 CX-B5R2-01 | 通过（036：`requireSpaceStatus` 前置 V203 二值，非法值 400 中文；六张表 CHECK 与服务层白名单全部对照一致） |
| GEN-008 接口与模式一致性 | 残留 CX-B5R2-01 | 通过（迁移枚举与服务层集合一致；`createSpaceType` 无 CHECK 维持 029） |
| GEN-002 可执行正确性 | 通过 | 通过（77 条 = 75 通过 + 2 条 017 环境红；036 测试红→绿） |
| GEN-001 必要性 | 034 登记 | 不变 |
| 其余 82 项 | 同 B5-r2 | 不变 |

## 6. 本次 Findings

无新的未定稿工程缺陷。

其余对抗式反例无新缺陷：

对抗式反例逐条结果：
0. 空间状态 `zzz`：400 `VALIDATION_FAILED` 中文（PASS）；`unavailable` 接受（PASS）；`space_type_dictionary` 无 CHECK（029 登记不变）。
1. 501 条空间后 `findSpaceById("sp_limit_0")`：Jdbc 覆写命中（PASS）。
2. `findSpaceById(null)`：`Map.of` 会抛 NPE，但全部调用点入参非空（`spaceExists` 先判 null；`createSpace` 先判 null；`ContractService.validateDraftSpaces` 经 `requireText`；`resolvePerSqmBaseRent` 用同一 `resolved` 列表）——与 B3-r1 的 `businessEntityExists`/`projectExists`（`Map.of`，调用点先 `requireText`/判空）同一形态；登记为观察 OBS-B5R2-01，不判缺陷、不入队。
3. 已存档商户建项目/空间/绑定：400 `MERCHANT_ARCHIVED`（PASS）；已批删商户同（`delete_status='approved'` 分支）；未存档商户不受影响（既有 29 条用例仍绿）。
4. 已存档商户更新档案 / 取消存档 / 删除申请 / 审批：仍命中已存档行（`updateMerchantKeepsArchiveAndPendingDeleteState`、`approveDeleteRemovesArchivedMerchantWhenNoReferences` PASS）。
5. 注释残留：grep 0。
6. 其余 B5-r1/B5-r2 反例（状态空串、双写、绑定主体状态、无引用 import、幂等空键、越权读端、幻影创建、悬空引用、激活既有绑定）结论不变，均为登记项 029–035、037。
7. NOT NULL 约束（entity_name/merchant_name/merchant_type/status/venue_type/name/space_name/space_type）均由服务层 `requireText`/白名单前置；VARCHAR 长度上限（如 entity_name 120、merchant_name 120）在本模块 DTO 为 0 处、全后端仅 2 处 `@Size`（grep 见 OBS-B5R3-01），超长输入将落 DB 错误 500——属全后端统一的输入长度约定缺口，非本 20 文件独有，登记不入队。

## 7. 观察（登记、不入队）

- OBS-B5R2-01：`JdbcPlatformRepository.findSpaceById` 与既有 `businessEntityExists`/`projectExists` 同用 `Map.of`（不接受 null 值）；当前全部调用点入参非空，行为正确；若日后新增调用点传 null 将抛 NPE 而非返回空 Optional——技术性观察，不发明防御、不入队（可并入 023 切分缝口径）。
- OBS-B5R2-02：`complex_test` 实态与 B5-r1 OBS-B5-05 相同（merchants 87/status 全 active/cooperation_form 全 NULL、projects 0、spaces 0、active 绑定 0、悬空引用 0、flyway 409）；测试插入的 501 条空间在 `@Transactional` 内回滚，库中 spaces 仍 0。
- OBS-B5R2-03：六张表 CHECK 对照——merchants（merchant_type/delete_status/两处电话 PII）、business_entities（entity_role）、spaces（status，036）均已由服务层前置校验；projects、space_type_dictionary、business_entity_bindings 无 CHECK。
- OBS-B5R3-01：VARCHAR 长度上限无服务层/DTO 校验（治理 DTO 0 处、全后端仅 2 处 `@Size`，见 §6 第 7 条），超长输入落 500；跨模块统一约定缺口，登记供全局收口（GEN-004），本模块不单独造长度规则。
- 017 环境红两条（`residualMigration…`、`migrationCreates…` 399≠409）仍在，须操作员处置。
- 技能：`using-superpowers`、`verification-before-completion`（全部结论附实跑/探针）、`receiving-code-review`（Grok B2 四条措辞修正已核实并写入定稿）、`grilling`（自问结构）实际使用；`ponytail-review` 用于最小处置尺度；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r3 票：**PASS**。20 文件三线全审、87 判据逐项核对；三批共 19 项修复经重审无回归、无新未定稿缺陷；无添加项、无 questions、无 HTML 修改申请；未提交/推送/合并/部署。本报告落盘前未读取 `M009-b5-r3-grok-raw.md` 任何正文。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B5-r3 Codex 修复后完整模块审计
版本：V0.02
审计结论：PASS
模块生命周期：IN_PROGRESS；B3已执行3/3。本票不等于双席B5或模块PASS。先派Grok4688，本席独立完成以下完整范围与三线复核后封存，未读取本次B5-r3-grok正文或原始JSON。

## 当前范围和审计方法
正本/home/james/complex-management main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。当前源等于B3-r3 after 39b7324829f44339c8c2afb4d2800dfce674ed2c；全部20文件通过逐字节/逐SHA核验，合计3369行。冻结plan/modules.md 20文件/2476行不改。B5映射重新从锁抽出87unique，与已完整读取的69业务及18GEN定义集合逐一相同，锁全文未变。
本席本会话上一轮已完整读取同20文件全部3368行、全87定义和直接依赖。本次对全部20文件重新读取全文并验证当前字节，重核所有公开入口及原私有守卫/事务/SQL行为，对每个冻结ID重新评估当前承载与反例；19文件与已读全文逐字一致，唯一SpaceTypeRequest的16行当前全文另行核完。不是仅审057或以B4票替代整模块评估。
B4-r3内容/归因已双PASS，只作为整批修复完成的前置。findings V2.14当前24行全否=18已修+6登记，本次仍逐项核现实现，不因否而跳过。
操作员“如果不是修改html，那么修复没有问题”授权适用；013/038批准边界保持，054-056先改后确认历史不倒填。仅直接SSH、无MCP/第三席/提交/部署；三份业务HTML不改，所有待审源码只读。

## 20文件完整覆盖
|文件|当前完整行范围|SHA256|复审范围/结果|
|---|---|---|---|
|backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java|1-17|45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277|四类主体字段、角色与状态；构造器无固定公司层级。|
|backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java|1-19|f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422|独立contract/operation/settlement、日期和status；未来/历史由服务验证。|
|backend/src/main/java/com/complex/platform/governance/MerchantRecord.java|1-116|dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00|projectId、生命周期、财务三主体、联系人与兼容构造器全读。|
|backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java|1-9|e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40|删除结果含业务对象ID及实际decision供审计。|
|backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java|1-1338|2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a|全部1338行：CRUD/映射/动态占用/审批链/行锁/项目补录/五组过滤分页。|
|backend/src/main/java/com/complex/platform/governance/PlatformController.java|1-296|9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2|全部296行：功能权限、平台范围、商户响应脱敏、项目与生命周期入口。|
|backend/src/main/java/com/complex/platform/governance/PlatformRepository.java|1-204|df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc|完整接口及旧构造兼容默认方法；真实Jdbc覆写项目过滤接口。|
|backend/src/main/java/com/complex/platform/governance/PlatformService.java|1-1173|984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1|全部1173行：授权、项目/对象校验、生命周期、未来绑定/历史绑定、审计/幂等。|
|backend/src/main/java/com/complex/platform/governance/package-info.java|1-1|e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9|只有包声明，无可执行业务。|
|backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java|1-17|66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da|绑定请求的独立三主体/日期，与服务验证一致。|
|backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java|1-26|18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19|主体类型/角色/状态字段及兼容构造器。|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java|1-6|e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93|归档理由与幂等键传到审计。|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java|1-5|2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4|审批decision/comment与服务真实分支一致。|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java|1-5|4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45|删除申请理由独立。|
|backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java|1-41|7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1|项目字段及档案更新字段；服务保存原project和既有财务关系。|
|backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java|1-26|6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108|负责人/地址/范围三个可空字段，旧构造器委派null。|
|backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java|1-21|63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa|项目/父空间/商户/类型/面积；服务检查存在、同项目和非负。|
|backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java|1-16|7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34|完整16行；新注释和V0.01正确，record字段/签名与运行代码未变。|
|backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java|1-11|75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f|通用状态请求具idempotency/reason/状态，服务按对象验证。|
|backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java|1-22|6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1|营业时间请求日期/时间与特殊日字段；具体营业服务属直接边界。|

## 第一性原理：必要性、实际执行、不能假设的前提
必要性：HTML PGM-017要求基础对象归属综合体项目；PGM-018要求公司与项目多对多；PGM-020要求项目状态、负责人、地址和经营范围；PGM-026要求档案与四类主体关系；BSM-004要求存档/审批删除/留存恢复。当前后端对象、范围与生命周期能直接对应这些要求。项目资料可空和仅后台范围已经获批；不把缺少集团页面/旧项目编辑页面转换成本轮未经批准的新业务。
实际执行：PlatformController按功能点授权并对商户响应脱敏；PlatformService各写口校验对象/project/状态，再由AuditedOperationExecutor及IdempotencyService把业务写入和审计放入既有事务；Jdbc存储项目/公司关联/商户/空间/主体绑定和审批链。Merchant upsert不更新project_id，专用补录SQL才可空到明确项目或同值；未来绑定不提前改商户三主体投影。
不能假设：高职级/同名普通管理员不等于真实USB；currentProjectScopeQuietly缺上下文仍调用requireProjectScope拒绝，不把名字当可随意全量的承诺；行锁须在业务事务内保持至写入；软删除不是物理抹除；文档声称须对应实际入口。057原注释已明确新建不可重复/固定启用，不能因此把旧“updated”事件名或旧测试名扩成新的修复范围。

## 顺从式：全部87映射逐项复审
下列结果仅针对冻结M009承载，不声称其他模块的终端/设备/会员/桌台/页面功能已实现。未直接承载者给出边界，仍核是否存在相反行为；没有删除或缩小映射。
|冻结ID|标题|结果|当前代码/适用性证据|
|---|---|---|---|
|BEQR-063|高危动作（需审批/双证据）|已核对；无本段待修项|本段不实现酒吧高危动作；已有审批持久化保留申请人、受益人、理由和审批链，未新增免审业务。|
|BEQR-074|隐私桌台权限|已核对；无本段待修项|本段无桌台接口；Controller保留平台范围检查，不因商户档案权限开放隐私桌台。|
|BEQR-143|四类主体|已核对；无本段待修项|BusinessEntityBindingRecord四对象分开：merchant及contract/operation/settlement；Service756-801分别验证，不以单一公司字段替代。|
|BEQR-144|经营主体关系|已核对；无本段待修项|MerchantRecord与binding分别持有经营和财务关系；Service945-968禁止普通入口改既有非空三主体。|
|BEQR-145|城市项目公司|已核对；无本段待修项|Jdbc1223-1238保存公司与项目多对多，1241-1246历史商户只允许空项目补录或同项目，不发明固定隶属链。|
|BEQR-146|运营公司职责|已核对；无本段待修项|主体角色枚举及独立operationEntityId保留，Service创建时校验角色；不是新增完整公司业务系统。|
|BEQR-147|主要收入归属|已核对；无本段待修项|本段仅存财务主体和管理费字段，不计算收入分配；锁住财务三主体变更，不替代结算审批。|
|BEQR-148|合作模式|已核对；无本段待修项|Service合作模式枚举与HTML六值相合，saveMerchant校验，旧值保留合法范围。|
|BEQR-149|职级|已核对；无本段待修项|职级从授权服务判断；没有将最高职级等同真实U盘最高权限。|
|BEQR-150|终端十一端|已核对；无本段待修项|本段无十一端实现；不以后台Controller对外声称所有终端完成。|
|BEQR-151|最终权限计算|已核对；无本段待修项|Controller功能点加范围，Service对象范围加真实身份；没有用菜单隐藏替代服务端授权。|
|BEQR-152|项目与敏感数据权限|已核对；无本段待修项|五组列表和COUNT共享项目谓词；MerchantRecord项目与Controller手机号脱敏分别控制，不把有项目权等同敏感原文权。|
|BEQR-153|终端开关优先级|已核对；无本段待修项|本段未改终端开关；受保护入口未增加绕开公共授权的开关。|
|BEQR-154|综合体项目状态|已核对；无本段待修项|Service项目六状态白名单与ProjectRequest/Record一致；容量非负。|
|BEQR-155|商户/运营主体状态|已核对；无本段待修项|Service主体五状态及商户存档、删除标志分开；普通新建不接收终止，既有终止状态缺省更新保留。|
|BEQR-156|合同状态|已核对；无本段待修项|合同状态由合同模块承担；当前只引用合同主体，不在本段发明合同流转。|
|BEQR-157|员工状态|已核对；无本段待修项|员工状态由access模块承担；当前真密钥判定要求账号active，未新增员工状态。|
|BEQR-158|设备状态|已核对；无本段待修项|本段不维护设备状态；没有以空间启用代替设备准入。|
|BEQR-159|权限状态|已核对；无本段待修项|权限状态由授权模块承担；本段未增加全局权限状态或豁免。|
|BEQR-166|合同审批链|已核对；无本段待修项|Supervisor持久化保留链、申请人和直接受益人；038不提供财税三主体免审通道。|
|BEQR-189|管理费方式|已核对；无本段待修项|管理费值沿用既有字段；租金非负，当前模块不另发明计费算法。|
|BSM-001|整体说明：这套系统到底管什么|已核对；无本段待修项|综合体项目、公司、商户、空间形成归属；不把M009后台通过写成全部系统已上线。|
|BSM-003|系统通用能力：数据可改可删、操作有引导、权限分岗|已核对；无本段待修项|业务对象保存归属，生命周期操作留痕，删除保留数据库记录；普通档案更新不覆盖归属和历史状态。|
|BSM-004|配置数据可以存档、也可以删除（删除要审批）|已核对；无本段待修项|商户和空间类型先存档、占用阻止、真实最高权限审批、申请人不得自审；SpaceTypeRequest新增说明现与实际拒重复/固定启用一致。|
|BSM-005|后台的"新建/录入"改成分步引导|已核对；无本段待修项|M009仅后台DTO/服务/仓储；无向导页面，不宣称分步引导或旧项目补录页面已完成。|
|BSM-006|验证用的二维码一律动态刷新（间隔可在后台设定）|已核对；无本段待修项|本段没有验证二维码生成/刷新实现，未把内部标识符当静态凭证。|
|BSM-007|平台管理员拥有全部日常功能权限|已核对；无本段待修项|普通platform_admin仍有日常权限；授权密钥身份单独识别，054只修岗位模板生命周期已有入口。|
|BSM-009|基础管理|已核对；无本段待修项|项目、商户、主体、空间基本档案持久化；项目负责人/地址/范围三项保存读取且允许历史为空。|
|BSM-028|主要功能|已核对；无本段待修项|商户档案、空间和主体绑定提供楼宇运营的归属；具体招商合同和工单不由本段重新实现。|
|BSM-029|和谁有关|已核对；无本段待修项|空间与商户项目一致、初始主体存在且同项目；跨域引用不以名称字符串猜测。|
|BSM-072|商户管理：商户在授权范围内完整使用自己的系统|已核对；无本段待修项|普通商户不能访问平台治理配置，商户业务测试改为明确自身merchant；不开放其他商户。|
|BSM-074|和谁有关|已核对；无本段待修项|合作商户关联项目及财务主体，合同路径采用intent.projectId，不再无依据塞默认项目。|
|BSM-114|各类入口：不同人只看自己该看的、只做自己该做的|已核对；无本段待修项|后台入口实时权限与数据范围适用；锁中员工手机考勤片段与现HTML有字词漂移，M009不实现该动作，另见观察。|
|BSM-115|相互关系：为什么这些子系统必须连在一起|已核对；无本段待修项|Leasing→Merchant→binding调用链实读，未来绑定不提前修改商户投影；同商户未结束绑定并发由行锁保护。|
|BSM-120|已确认规则和实施前输入项|已核对；无本段待修项|当前M009执行最高权限、中文、隔离、留痕已确认边界；footer日期漂移不产生业务规则变更，不修改HTML或锁。|
|GEN-001|必要性与最小性|已核对；无本段待修项|复用既有ProjectRecord、授权、审计、幂等和仓储；013已批准三个可空字段，不发明页面或调度。|
|GEN-002|可执行正确性|已核对；无本段待修项|当前入口与283项已执行测试所用运行源码一致；057注释已对齐重复编码拒绝/新建启用，无新增运行分支。|
|GEN-003|测试有效性|已核对；无本段待修项|检查69服务、23JDBC及直接依赖测试断言；054先红后绿，055前提明确且原非法路径仍拒绝，056命中精确身份错误。|
|GEN-004|信任边界校验|已核对；无本段待修项|Service在写入前校验项目归属、主体/商户/父空间存在、状态、非负数和生效日期；SQL参数化。|
|GEN-005|授权与最小权限|已核对；无本段待修项|Controller功能权限+Service对象范围；真实MASTER的三个固定字段和active，普通最高职级不可代替。|
|GEN-006|数据完整性|已核对；无本段待修项|Jdbc商户upsert不改project_id；专用补录仅空或同项目；绑定状态与商户投影在事务内更新。|
|GEN-007|并发与幂等|已核对；无本段待修项|IdempotencyService169-172把action+完成放同TransactionTemplate；merchant rowlock586-602序列化同商户未结束绑定。|
|GEN-008|接口与模式一致性|已核对；无本段待修项|ProjectRequest/Record/V427/Jdbc三字段与可空性一致；SpaceTypeRequest注释现与create-only、重复编码错误语义一致。|
|GEN-009|错误处理与恢复|已核对；无本段待修项|具体ApiException拒绝且不伪成功；历史绑定不重开，未来绑定不得早生效，失败不先改财务投影。|
|GEN-010|安全与敏感信息|已核对；无本段待修项|手机号仓储加密，Controller所有商户响应调用脱敏；动态计数SQL表名来自系统目录、merchantId参数化，未输出秘密。|
|GEN-011|可观测与审计证据|已核对；无本段待修项|审计metadata携带project、reason、真实decision及业务ID；spaceType删除完成最近待审申请，旧审批历史保留。|
|GEN-012|资源与性能边界|已核对；无本段待修项|分页1..500；五组COUNT与分页同条件，M:N用EXISTS避免多行放大；直接按主键读取不依赖前500条。|
|GEN-013|构建配置与依赖|已核对；无本段待修项|实际JDK21、Maven离线和complex_test验证；只解析三个数据库变量，不source全环境；没有新增依赖。|
|GEN-014|数据库迁移正确性|已核对；无本段待修项|V427增加可空字段/FK/关系表，既有历史不猜填；23JDBC用例运行目标测试数据库。|
|GEN-015|界面可用性与可访问性|已核对；无本段待修项|本模块无页面，键盘/焦点等不适用；后端面向用户message使用中文，内部code按HTML豁免。|
|GEN-016|文档可验证性|已核对；无本段待修项|057旧upsert承诺已删除；当前注释可由Service551-558及现有两测试复现。计划冻结行数与当前实算分开，页面/项目卡消费未冒称完成。|
|GEN-017|业务来源分类|已核对；无本段待修项|逐内容按冲突→细化→技术性→添加；有上位HTML的DTO/状态均属细化，不能整体塞技术性。|
|GEN-018|第一性原理证据|已核对；无本段待修项|以当前代码/HTML/实跑测试为依据；旧PASS及注释本身不替代实际行为。|
|PGM-001|整体定位：平台治理层不是一个业务部门，而是所有业务的底座|已核对；无本段待修项|后台治理提供项目归属、权限入口、空间/商户/主体基础对象，不取代业务部门。|
|PGM-002|治理顺序：一件业务能不能做，要先经过这些判断|已核对；无本段待修项|入口功能点、平台/项目范围、对象状态、写入留痕逐层执行；真USB仅明文适用分支。|
|PGM-003|模块入口：平台治理层包含哪些管理模块|已核对；无本段待修项|本段只承载治理模块部分入口，不宣称入口目录代表全功能已实现。|
|PGM-004|综合体和集团|已核对；无本段待修项|ProjectRecord/Request与仓储支持项目档案三项资料；集团视图仅预留关系。|
|PGM-005|空间和点位|已核对；无本段待修项|空间project/parent/merchant相互校验，类型生命周期保留；新增请求注释已与实际字典入口一致。|
|PGM-006|商户和主体|已核对；无本段待修项|商户基本资料及独立经营/合同/结算主体引用，不把录档、业务合同和财税变更混为一项。|
|PGM-007|合同和准入|已核对；无本段待修项|直接依赖Leasing成交前置项目真实传递；未新增合同审批豁免。|
|PGM-008|员工和手机|已核对；无本段待修项|本段未实现员工手机端；授权账号状态和来源继续由现有access管理。|
|PGM-009|权限和职级|已核对；无本段待修项|Controller细功能点与Service真实最高权限各司其职，未将管理员统称最高权限。|
|PGM-010|终端开关|已核对；无本段待修项|未修改终端启停规则；M009没有新的关闭隔离开关。|
|PGM-011|设备管理|已核对；无本段待修项|空间作为设备归属基础，当前无设备状态替代逻辑。|
|PGM-012|商户侧|已核对；无本段待修项|平台治理配置不向普通商户开放；范围测试保持合法商户身份与原拒绝断言。|
|PGM-013|开放接口|已核对；无本段待修项|Controller参数/状态失败显式；未对第三方新增全局数据通道。|
|PGM-014|留痕复核|已核对；无本段待修项|审计元数据和审批申请/决策持久化；最近待审更新保留旧链。|
|PGM-015|楼宇运营|已核对；无本段待修项|经营空间、招商转商户及公司项目关系提供楼宇归属，工单/能耗不在本段实现。|
|PGM-016|应急与隐私|已核对；无本段待修项|手机号存储加密及返回脱敏；当前模块没有新增隐私原文出口。|
|PGM-017|综合体和集团：当前一个项目，未来能看集团|已核对；无本段待修项|当前单综合体落地；项目负责人、地址、范围已按批准后台可空实现；集团关系预留不宣称汇总页面完成。|
|PGM-018|物理项目与运营公司：多对多协作，不是固定隶属层级|已核对；无本段待修项|BusinessEntity_projects复合主键、多对多append，列表EXISTS项目过滤；禁止通过商户通用更新迁移项目。|
|PGM-019|各主体具体负责什么|已核对；无本段待修项|contract/operation/settlement主体存在且同项目；既有非空三主体不能普通修改；未自动套默认公司。|
|PGM-020|主要功能|已核对；无本段待修项|项目状态/负责人/地址/经营范围保存读取；商户和空间提供归属；空间类型说明已纠正，未扩为新业务功能。|
|PGM-021|业务意义|已核对；无本段待修项|所属项目和独立主体支持后续跨域统计；不将当前后台有限能力扩大成集团所有业务完成。|
|PGM-026|主要功能|已核对；无本段待修项|档案、空间类型、存档恢复/删除申请均有状态和权限；type占用时不得删除，未来未结束绑定仍计占用。|
|PGM-027|业务边界|已核对；无本段待修项|商户状态不代替合同范围，财税主体变更另受约束；历史失效绑定不能重开。|
|PGM-031|外部经销商主体、合同价格与资金治理|已核对；无本段待修项|外部经销主体不套内部员工权限；平台治理入口排除非平台范围；合同价格审批无新增绕过。|
|PGM-035|提成发放和人员服务治理|已核对；无本段待修项|本段无提成计算；审批持久化保留直接受益人，056测试补登记后仍验证真实身份。|
|PGM-040|权限和职级：每一个单一功能都可以单独授权|已核对；无本段待修项|Controller按单一功能授权，Service补对象和身份条件，不用职级替代全部条件。|
|PGM-041|权限规则|已核对；无本段待修项|项目/商户隔离无关闭开关；最高权限与平台管理员分离；所有商户响应脱敏，审批申请人/受益人不得自审。|
|PGM-042|职级规则|已核对；无本段待修项|普通高职级管理员测试期望MASTER_AUTHORITY_REQUIRED，真实USB依固定身份判定。|
|PGM-043|外包人员与审计人员（2026-06-29 新增）|已核对；无本段待修项|requirePlatformScope排除商户/外包，仅active platform_admin既有桥接；手机号原文不因有档案权限自动放行。|
|PGM-044|桌台、挂单与经销业务权限|已核对；无本段待修项|本段无桌台/挂单/经销订单实现；当前授权和审批记录未新增敏感权限免审分支。|
|PGM-048|商户侧：在授权业务范围内全开放|已核对；无本段待修项|不开放跨商户/跨项目平台数据；055合法商户卡路径证明商户隔离仍执行，未声称项目卡消费已补齐。|
|PGM-060|会员身份、营销与生物信息治理|已核对；无本段待修项|本段不注册会员/营销/人脸；无新增后台会员入口，商户联系人PII沿用加密脱敏。|
|PGM-065|连接关系：平台治理层怎么连接所有业务|已核对；无本段待修项|实读Leasing→Merchant/绑定和事务；项目、空间、商户基础引用不形成无归属对象。|
|PGM-066|已拍板规则和待排期问题|已核对；无本段待修项|中文message、操作留痕和项目归属适用；集团页面/终端/会员业务不以本段PASS代替。|

## 对抗式：完整模块的反例检查
|攻击性提问/反例|当前结果与证据|
|---|---|
|甲项目账户增大分页或借公司多对多关联看乙项目COUNT|Service currentProject；Jdbc1248-1337五组COUNT/页相同谓词、公司用EXISTS；不加入乙项目总数。|
|没有真实项目、platform-company组织、普通商户请求平台配置|缺project scope拒绝；非项目scope列表空/total0；写入要求真实项目，requirePlatformScope拒绝普通商户/外包。|
|普通档案更新认领null项目或迁项目；最高权限配置覆盖其它资料|Service348-391专用入口仅真实最高权限且显式目标存在；Jdbc1241-1246只更新project_id且null/同值，旧归属不迁移。|
|把platform_admin最高职级或同名ID当USB|AuthorizationService139-144同时要求active及SessionIssuer三个既有常量；M006真实/普通身份断言仍有效。|
|删除审批人正是申请人或直接受益人|已有Supervisor规则先检查受益人完整性和利益冲突；054不移除Controller权限/占用/审计，056只补合法测试资料。|
|未来日期绑定现在修改结算主体或并发建第二预约|Service756-867未来inactive/end=null，不投影；事务内锁merchant后检查active或未结束inactive；Leasing初建三列null再调用绑定。|
|普通停用active后换财务三主体，或重开历史结束期间|Service拒绝active普通失效/非空三主体变更，已结束历史不重开；USB也不成为财税并审替代。|
|跨项目merchant/parent用于空间，或容量面积租金负值|Service670-722/873-942/982-1055要求存在、同项目、非负；旧资料三新增字段可空不猜填。|
|空间类型删除后不能恢复、最近申请永远pending、历史被覆盖|disabled/archived可恢复，pending不可绕；完成最近pending申请为真实决定，旧历史保留；删除软留存。|
|把未来inactive预约当已失效从占用中忽略|Jdbc197-231仅排除真实已结束且日期有效的inactive，未来end=null仍阻止删除。|
|平台档案/返回结果暴露个人手机号|仓储PiiCipher与Controller所有商户响应脱敏；M010本人/管理接口仍禁止明文断言。|
|055修改后项目卡内部消费被误称已可用|旧无merchant范围调用仍MERCHANT_SCOPE_REQUIRED，余额不动、无消费通知；成功消费通知通过合法商户卡验证。|
|按DTO文档用新增入口更新重复编码|新注释明确typeCode不得重复；Service551-558与两条测试一致，不再承诺更新。|

## 18已修与6登记逐项确认
|ID|当前完整复审依据|结果|
|---|---|---|
|013|ProjectRequest/ProjectRecord/Jdbc/V427三资料可空保存读取，旧构造兼容；无页面承诺|保持完成|
|018|普通新建/转换terminated拒绝，既有terminated无状态变化保存保留|保持完成|
|030|已提供初始财务主体必须存在且本项目，缺省仍可空|保持完成|
|031|历史结束期间不重开，日期不得倒置，不清空当前投影|保持完成|
|038|商户project、公司M:N、五列表COUNT/写口范围、专用空值补录/追加关系、真实USB识别|保持完成|
|039|空间project必填且当前项目，parent/merchant同项目|保持完成|
|040|普通active失效和非空财务三主体变更拒绝，无USB财税免审|保持完成|
|041|未来inactive不投影，merchant锁防重复未结束绑定，Leasing初始三列null|保持完成|
|042|空间类型disabled及archived可恢复|保持完成|
|043|审计真实业务ID/reason/decision、驳回事件不冒充批准|保持完成|
|044|最近pending申请结为真实终态，保留旧历史|保持完成|
|045|只有日期有效且结束的inactive不计占用，未来预约仍计|保持完成|
|046|容量、面积、租金非负，零/可空边界保留|保持完成|
|047|用户message中文，内部code不改|保持完成|
|054|AccessService生命周期真实USB入口可达，普通管理员不成为最高权限；原限制保留|保持完成|
|055|合法商户资金/通知/隐私测试前提，核心断言和非法拒绝路径保留|保持完成|
|056|直接受益人及schedule测试完整，普通高职级仍精确拒绝|保持完成|
|057|SpaceTypeRequest现注释匹配新增行为，V0.01；字段/类/运行token全等|完成，原CX-B5-01已关闭|
|048|按项目和经营主体双条件隔离反查|保持登记，无独立缺陷|
|049|已有审批链持久化、受益人计划与条件更新|保持登记，无新增流程|
|050|联系人加密与返回脱敏|保持登记|
|051|分页COUNT/LIMIT资源承载，无业务发明|保持登记|
|052|直接DELETE拒绝转原存档审批，不新业务|保持登记|
|053|package-info仅package声明，无业务发明|保持登记|

## 实际验证及可复现命令
当前20文件全部read_bytes/hashlib并与git show after:path逐字比较；合计3369行，87映射unique。公开入口枚举重新核Controller全部27入口、Service全部业务入口以及Jdbc CRUD/分页/审批/归属方法，私有守卫与事务依赖保持已读同字节。
B3-r3去除Java块/行注释后的非空token序列完全一致；完整两tree只有1文件1块，git diff --check before after exit0。没有运行代码/测试变化，不因注释重复跑全套。
本席真实执行过的当前运行源码证据：JDK21、complex_test，命令
mvn -o -q -Dtest=PlatformServiceTests,PlatformJdbcIntegrationTests,PlatformControllerTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,AuthorizationServiceTests,SupervisorReviewServiceTests,M006SecurityRegressionTests,M010RegressionTests,BarStoredValueFundChainJdbcIntegrationTests,StoredValueNotificationIntegrationTest -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test test
11类283项，exit0，0failure/0error/0skip。该执行在B3-r2后/B4-r2前；此后运行token和测试文件未改变。本次B5-r3明确复用同源已封存执行结果，未声称刚重新运行：
MAVEN_EXIT=0
TEST-com.complex.platform.governance.PlatformServiceTests.xml {'tests': '69', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.172'}
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests.xml {'tests': '23', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.455'}
TEST-com.complex.platform.governance.PlatformControllerTests.xml {'tests': '14', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.676'}
TEST-com.complex.platform.leasing.LeasingServiceTests.xml {'tests': '11', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.092'}
TEST-com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests.xml {'tests': '3', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.107'}
TEST-com.complex.platform.access.AuthorizationServiceTests.xml {'tests': '24', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.012'}
TEST-com.complex.platform.governance.review.SupervisorReviewServiceTests.xml {'tests': '44', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.218'}
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '39', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.656'}
TEST-com.complex.platform.access.staff.M010RegressionTests.xml {'tests': '44', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.979'}
TEST-com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.xml {'tests': '8', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.103'}
TEST-com.complex.platform.customer.StoredValueNotificationIntegrationTest.xml {'tests': '4', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '3.612'}

直接依赖已追：ProjectRecord、ProjectAssociationRequest、V427；AuthorizationService/SessionIssuer真实身份；LeasingService281-311真实先建档后绑定；AccessService485-548已有生命周期；SupervisorReviewService201-214受益人/申请人；AuditedOperationExecutor113-155与IdempotencyService169-172事务。未将重复查询或旧零调用方法当无来源新业务而乱删。
本次不以Grok尚未收到的B5-r3票作依据；前轮Grok239项是其已封存独立测试，不加到283当新增用例总数。未跑全仓/生产数据/全端UI，也不宣称它们已验收。

## 分类、观察和范围边界
有HTML上位的对象、DTO、状态、审批持久化归细化，不整体塞技术性；仅分页资源承载与package声明属无业务发明技术性。当前无与HTML矛盾的待修实现，无新添加项，无锁外必要判据。
锁内69业务片段与现行HTML，67全文匹配；BSM-114考勤打卡→查看考勤与BSM-120页脚日期不同。M009无对应考勤/页脚实现，不据此更改HTML或锁、不隐瞒成69全文同字节。旧“87/87邻域命中”只指首片段，不替代全文比较。
空间类型旧事件名和测试方法名观察保留，B2明确不改，无新的运行问题；不把允许status字段保留视为实现更新功能。无页面/旧项目编辑补录入口/集团汇总/新调度或财税审批功能承诺。
沿用实际已读的受控审计技能要求：完整覆盖、先核事实再给票、最小范围、反例和完成前验证；不新增第三席。无添加项、无未答问题、无HTML申请；HTML当前未修改。

## 本席本次完整结论
PASS。20文件/87冻结映射的第一性原理、顺从式、对抗式复审完成；本次新问题0，原CX-B5-01对应057按已定稿最小范围关闭，其余已修行为保持，6登记无待修义务。本票为独立自审，尚须Grok当前完整B5-r3同样PASS及四值全等，方可把ledger改PASS并写模块收口报告。当前不得提前宣布模块收口。
