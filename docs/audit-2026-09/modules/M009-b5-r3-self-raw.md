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
