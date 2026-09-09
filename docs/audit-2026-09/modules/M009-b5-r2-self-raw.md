CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r2 修复后全模块重审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线重审）。2026-09-09T09:21:10Z
- 正本 HEAD 242f487233d028173cb9198eecd079f5d877405c；本报告落盘时工作区 tree `ae8f003608b73e0a5b59b262a15da1ffc93d662e`（含 B3-r1、B3-r2 修复与 B4-r1/B4-r2 两席 raw）。锁四值：实算 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a（sidecar/ledger 逐字相同；精确 Grok B5 raw 的最后独占锁行在读取 Grok 报告后于回执核对）。
- 顺序：Grok B5-r2 已真实退出（受理四项事实见回执）；本报告在读取 `M009-b5-r2-grok-raw.md` 任何正文之前落盘（该文件此刻存在=True，未读）。
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
| governance/PlatformService.java | 863 | 42578 | 2450c955ac3379c4… |
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

## 2. 可执行证据（本席实跑，/tmp/m009_b5r2_run.log 原文）

```
START=2026-09-09T09:20:40Z
TREE=ae8f003608b73e0a5b59b262a15da1ffc93d662e
=== BACKEND: governance.*Tests + leasing.*Tests ===
BACKEND_EXIT=1
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.101 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.008 s <<< FAILURE!
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.113 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.098 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.677 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.087 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.056 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.005 s <<< FAILURE!
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.090 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] Tests run: 76, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
=== WEB: OrganizationMerchantPage vitest ===
WEB_EXIT=0
[2m Test Files [22m [1m[32m1 passed[39m[22m[90m (1)[39m
TREE_AFTER=ae8f003608b73e0a5b59b262a15da1ffc93d662e
END=2026-09-09T09:20:49Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文重新推导本模块业务约束（BSM-004 存档/删除/最高权限/「存档（先下线）…不再被新业务选用」、BEQR-143 四类主体、BEQR-148 合作方式六类、BEQR-154/155 状态、PGM-018/019/026/027 主体角色与统一绑定），逐条对照当前实现（不依赖 B1–B5-r1 结论）。
- 顺从式线：87 冻结判据逐项（§5 增量表 + B1 报告 §5 全表 + B5-r1 §5；未变化者沿用并再次核对）。
- 对抗式线：针对 B3-r1/B3-r2 修复面构造反例（静态追流 + `complex_test` 只读探针 + 全套测试实跑）：LIMIT 500 存在性、`findSpaceById` 空参数（`Map.of` 不接受 null）、已存档商户复用、六张表全部 CHECK 约束与服务层白名单逐一对照（新增：`spaces.status` 有 V203 CHECK 而服务层未校验）、空状态覆盖、双写、残留注释、无引用 import、绑定主体状态、非最高权限驳回、越权读端、幻影创建、悬空引用。
- 依赖只读追流（不审）：`PlatformRepository` 默认方法与 Jdbc 覆写、`leasing/LeasingService:288-292,424`（`spaceExists` 消费方）、`contract/ContractService:261,954`（`findSpaceById` 治理外消费方：两处入参分别经 `requireText` 与同一 `resolved` 列表，均非 null）、`access/AuthorizationService.isMasterAuthority`、M011 三测试与 `LeasingSignAtomicityJdbcIntegrationTests` 夹具、`apps/web/.../OrganizationMerchantPage.tsx`。
- 20 文件覆盖：14 个文件 SHA 与 B5-r1 相同（再次全读）；6 个 B3-r2 改动文件（PlatformService/JdbcPlatformRepository/PlatformRepository/MerchantRecord 与 M011 两测试）按当前内容全读，改动块与 B3-r2 diff（12568 bytes SHA 69229f6b…）逐字对照。

## 4. 已定稿 35 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001–012、014–016 | 否（第一批已修） | 与 B5-r1 §4 相同；B3-r2 未触及其行为 | 实跑：ServiceTests 30/30、ControllerTests 10/10、JDBC 13 中 12 通过（唯一红 017）、Leasing 11/11 + 2/2 + 4 中 3（017）、页面 12/12 |
| 026 | 否（已修） | `JdbcPlatformRepository:187`、`MerchantRecord:27`、`PlatformRepository:54` 注释改软删除口径 | 20 文件 `硬删|物理删除` grep 0 处 |
| 027 | 否（已修） | Jdbc 覆写 `findSpaceById` `WHERE id = :id`；`listSpaces()` LIMIT 500 不变 | `findSpaceByIdIsNotTruncatedByListLimit`（501 条）实跑 PASS；空参数：全部 4 个调用点（`spaceExists:407`、`createSpace:486`、`ContractService:261/954`）入参均先判非空或经 `requireText`，`Map.of` 不会收到 null |
| 028 | 否（已修） | `requireActiveMerchant` 用于 `createProject`/`createSpace`/`createBusinessEntityBinding`；其余路径仍 `requireMerchant` | `archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 实跑 PASS；`updateMerchantKeepsArchiveAndPendingDeleteState`、`approveDelete…`、`requestDeleteByMasterAuthority…` 仍 PASS（已存档行可更新/删除） |
| 013、017–025、029–035 | 否（登记） | 内容未动；017 环境红仍在（flyway 409 > 399） | 与定稿一致 |

## 5. 87 判据增量核对（相对 B5-r1 §5 表；未列者结论不变）

| 判据 | B5-r1 结论 | 当前结论 |
|---|---|---|
| BSM-003 / BSM-004 存档与删除 | 残留 CX-B5-01、CX-B5-03 | 通过（026 注释归口软删除；028 「不再被新业务选用」在三处创建写路径落地；更新/恢复/删除流程仍命中已存档行） |
| GEN-006 数据完整性 | 残留 CX-B5-02 | 通过（027 按主键存在性；上级空间/意向空间不再假阴性） |
| GEN-012 资源边界 | 残留 CX-B5-02 | 通过（列表 LIMIT 500 只作用于列表） |
| GEN-016 文档可验证 | 残留 CX-B5-01 | 通过（20 文件无「硬删/物理删除」；注释与 `deleteMerchant` UPDATE 一致） |
| GEN-002 可执行正确性 | 通过 | 通过（76 条 = 74 通过 + 2 条 017 环境红；两条新测试红→绿） |
| GEN-004 信任边界 / GEN-009 错误处理 | 通过 | **残留** CX-B5R2-01：`createSpace` 的 status 只 `trimToDefault`，而已发布迁移 V203 对 `spaces.status` 加了 CHECK（active/unavailable），非法值落 DB CHECK 违例→`GlobalExceptionHandler` 只拦 `DuplicateKeyException`（:150–155）→ 500 英文，与 005（entity_role CHECK→500）同一缺陷形态；B5-r1 两席把 029 判否的依据「无闭集」对空间不成立（闭集来自代码事实 V203，非 HTML） |
| GEN-001 必要性 | OBS-B5-03 | 034 登记（否），不变 |
| 其余 79 项 | 同 B5-r1 | 不变 |

## 6. 本次 Findings（临时编号 CX-B5R2-，正式 ID 在 B2 分配）

### CX-B5R2-01 `createSpace` 的空间状态未按已发布 CHECK（V203）校验，非法值落 500
- 位置：`PlatformService.createSpace:500`（`trimToDefault(request.status(), "active")` 直落库）；约束 `backend/src/main/resources/db/migration/V203__spaces_status_check.sql`（`chk_spaces_status CHECK (status IN ('active','unavailable'))`，complex_test 实态同）；`GlobalExceptionHandler.java:150–155` 明示不拦 `DataIntegrityViolationException`。
- 复现（只读，BEGIN…ROLLBACK）：`INSERT INTO spaces (…status…) VALUES (…'zzz'…)` → `violates check constraint "chk_spaces_status"`；经 API 即 500 固定文案，用户得不到字段级中文 400。
- 分类：技术性（闭集来自已发布迁移这一代码事实，不是 HTML 新规则；服务层只是把既有约束前移为安全失败）；依据类型 4（迁移原文 + psql 输出）；判据 GEN-004 / GEN-009 / GEN-008（迁移与服务层枚举一致）；工程缺陷：是（与 M009-005 同形态、同标准）。与 029 的关系：029「空间/字典 status 任意串」两席判否的理由是「HTML 无闭集、不发明」——对 `space_type_dictionary`（V184 无 CHECK）仍成立；对 `spaces` 不成立，故本条只覆盖空间部分，字典部分维持 029 登记。
- 建议入队：是。最小处置：`requireSpaceStatus`（active/unavailable，否则 400 中文 `VALIDATION_FAILED`，与 004 的 `requireProjectStatus` 同法）用于 `createSpace`；ServiceTests 一条最小用例（TDD 红→绿）。不改迁移、不改全局异常处理。

对抗式反例逐条结果：
1. 501 条空间后 `findSpaceById("sp_limit_0")`：Jdbc 覆写命中（PASS）。
2. `findSpaceById(null)`：`Map.of` 会抛 NPE，但全部调用点入参非空（`spaceExists` 先判 null；`createSpace` 先判 null；`ContractService.validateDraftSpaces` 经 `requireText`；`resolvePerSqmBaseRent` 用同一 `resolved` 列表）——与 B3-r1 的 `businessEntityExists`/`projectExists`（`Map.of`，调用点先 `requireText`/判空）同一形态；登记为观察 OBS-B5R2-01，不判缺陷、不入队。
3. 已存档商户建项目/空间/绑定：400 `MERCHANT_ARCHIVED`（PASS）；已批删商户同（`delete_status='approved'` 分支）；未存档商户不受影响（既有 29 条用例仍绿）。
4. 已存档商户更新档案 / 取消存档 / 删除申请 / 审批：仍命中已存档行（`updateMerchantKeepsArchiveAndPendingDeleteState`、`approveDeleteRemovesArchivedMerchantWhenNoReferences` PASS）。
5. 注释残留：grep 0。
6. 其余 B5-r1 反例（状态空串、双写、绑定主体状态、无引用 import、幂等空键、越权读端、幻影创建、悬空引用）结论不变，均为登记项 029–035。

## 7. 观察（登记、不入队）

- OBS-B5R2-01：`JdbcPlatformRepository.findSpaceById` 与既有 `businessEntityExists`/`projectExists` 同用 `Map.of`（不接受 null 值）；当前全部调用点入参非空，行为正确；若日后新增调用点传 null 将抛 NPE 而非返回空 Optional——技术性观察，不发明防御、不入队（可并入 023 切分缝口径）。
- OBS-B5R2-02：`complex_test` 实态与 B5-r1 OBS-B5-05 相同（merchants 87/status 全 active/cooperation_form 全 NULL、projects 0、spaces 0、active 绑定 0、悬空引用 0、flyway 409）；测试插入的 501 条空间在 `@Transactional` 内回滚，库中 spaces 仍 0。
- OBS-B5R2-03：六张表 CHECK 对照——merchants（merchant_type/delete_status/两处电话 PII）、business_entities（entity_role）均已由服务层前置校验；projects、space_type_dictionary、business_entity_bindings 无 CHECK；仅 spaces.status 未前置（CX-B5R2-01）。
- 017 环境红两条（`residualMigration…`、`migrationCreates…` 399≠409）仍在，须操作员处置。
- 技能：`using-superpowers`、`verification-before-completion`（全部结论附实跑/探针）、`receiving-code-review`（Grok B2 四条措辞修正已核实并写入定稿）、`grilling`（自问结构）实际使用；`ponytail-review` 用于最小处置尺度；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r2 票：**FAIL**。20 文件三线全审、87 判据逐项核对；第一批 15 项与第二批 3 项修复经重审无回归；但对抗式线新发现 CX-B5R2-01（无定稿 ID、不在修复队列），按运行手册 §8 整批回 B2（第三批，分配 036）；B3 累计 2/3，若定稿入队则 B3-r3 为最后一次。无添加项、无 questions、无 HTML 修改申请；未提交/推送/合并/部署。本报告落盘前未读取 `M009-b5-r2-grok-raw.md` 任何正文。


## M009 第三批 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B5-r2 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M009-b5-r2-self-raw.md` V0.01 前缀 14019 bytes SHA256 c0e62b33b588e838caf0ab6cc3bb78cf660a70ed3626ff688b9345b2c27857cb；`modules/M009-b5-r2-grok-raw.md` V0.01 前缀 40835 bytes SHA256 fdaa87631e0b63027118c71b4cb89bca29e21530c33393ff880b52dc48dca505（Grok B5-r2：PASS，观察 GR-M009-B5-R2-01…06、无新缺陷）；候选 `findings.md` V1.24 478580 bytes SHA256 f8933e73f9c1786cdac39b762527f6f221dede39e1426292c0c6ab0b06d7fd89「当前 M009 B5-r2 合取FAIL…第三批 B2-r1 整批候选」。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B5 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok 第三批 B2-r1 真实退出后、读取其追加段之前落盘。按 `receiving-code-review` 收窄：Grok B5-r2 观察逐条对照源码核实后归并；B2 不修复。
- 差集：席甲 CX-B5R2-01 → 036；OBS-B5R2-01（`Map.of` 空参数形态）并 023 切分缝口径、OBS-B5R2-02（库实态）与 OBS-B5R2-03（六表 CHECK 对照）为事实记录不开 ID；Grok GR-M009-B5-R2-01 → 037（按其 B5 原意登记否）；R2-02/03/04/05/06 → 026→M011、010→M011、017、029、030 既有口径。

| ID | 本席票 | 理由 |
|---|---|---|
| 036 | PASS（技术性／是） | 已发布 V203 CHECK 与服务层枚举不一致（GEN-008），非法输入落 500 而非字段级中文 400（GEN-004/GEN-009），与 005 同形态同标准；`requireSpaceStatus` 一处 + 一条最小用例为最懒可行，闭集取自迁移事实、无业务发明；029 的字典部分（V184 无 CHECK）维持登记 |
| 037 | PASS（细化观察／否） | 重新激活既有绑定不走 `requireActiveMerchant`：028 定稿处置限三处创建路径，本席同意按 Grok B5 原意登记；若外审改判入队，处置为激活分支加一行校验 + 一条最小用例 |
本席第三批 B2-r1 票：**PASS**（队列是＝036；已修 18 项与 013/017–025/029–035 保持否）。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。
