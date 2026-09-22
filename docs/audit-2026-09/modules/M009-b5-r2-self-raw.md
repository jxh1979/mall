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


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B5 第2轮 Codex 修复后完整模块审计
版本：V0.03
审计结论：FAIL
模块生命周期：IN_PROGRESS；B3已执行2/3；LOCK-AMEND-COUNT=1。
本次独立报告在Grok B5调用27518期间完成；未读取本次Grok raw或原始JSON，也未以B4票代替整模块审计。旧报告前缀原样保留，本次全文起自最后一条哈希行。

## 范围、版本和事实
正本/home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。只用直接SSH，未调用MCP，未派第三审计席。
本次重读冻结plan/modules.md的M009同20文件全部内容，合计3368行；冻结规划2476行是修复前统计，不改计划或范围来迁就增长。逐项读取全部87映射定义（69业务＋18GEN），并核对三份HTML现行原文。下表列出每个文件全行覆盖与SHA256。
B3-r2 after=e4c10154a405e61e1bfad9fedb6fde11068f653a；本次20文件字节与该树相同。来源与测试旁证仍为B4中封存的修后版本，审计文档记账不算源码修复。
用户已明确“如果不是修改html，那么修复没有问题”，授权054-056继续；此前先改后确认的时间线保留，不倒填事先批准。013/038早前批准范围保留。三份HTML当前未因本轮改变，B3前后相同；本报告不申请或实施HTML修改。
findings V2.11当前23项=17项已修复（B4双PASS清队列）＋6项仅登记；“队列否”不作为B5免审凭据。B4双PASS仅证明r2全部diff修复/归因通过，不能代替当前B5。

## 20文件完整覆盖
|文件|完整读取行|SHA256|检查结果/边界|
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
|backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java|1-15|d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2|已完整读，旧注释与新增实际行为不一致，CX-B5-01。|
|backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java|1-11|75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f|通用状态请求具idempotency/reason/状态，服务按对象验证。|
|backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java|1-22|6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1|营业时间请求日期/时间与特殊日字段；具体营业服务属直接边界。|

## 第一性原理：三问落到执行事实
为什么存在：项目/集团/公司多对多与商户、空间、四类主体提供业务归属；HTML PGM“当前先按一个综合体落地，但所有基础对象都要带‘综合体项目归属’”，并要求项目状态、负责人、地址和经营范围。项目资料与归属必要；不据此自行添加完整集团UI、自动调度或新的财税流程。
实际执行什么：Controller功能点和响应脱敏→PlatformService授权/归属/状态校验→executeWrite/AuditedOperationExecutor→IdempotencyService TransactionTemplate→Jdbc仓储和审批/审计。已追AuthorizationService139-144真实身份、482-537范围和project必需，ProjectRecord与V427三可空字段，LeasingService281-311传intent.projectId且初始财务三主体为null。
哪些前提不能只相信：currentProjectScopeQuietly名称并不表示可任意为空，实际返回requireProjectScope，故缺省项目按异常拒绝；同商户锁必须在实际事务内才有效，已追IdempotencyService169-172且并发JDBC用例通过；最高权限不能凭platform_admin或高职级认定；DTO注释声称可更新必须用实际入口与断言核实，由此发现CX-B5-01。

## 顺从式：87项逐一核对
“已核对”代表已读定义并给出本段适用性，不意味着本段实现了该HTML下所有其他模块功能。无页面、设备、会员、终端或酒吧桌台实现在M009者明确记录边界，不擅自扩展20文件；旧六项登记是否有业务来源仍按冻结已确认口径验证。
|冻结ID|标题|本次结果|当前代码或适用性证据|
|---|---|---|---|
|BEQR-063|高危动作（需审批/双证据）|已核对；适用范围见证据|本段不实现酒吧高危动作；已有审批持久化保留申请人、受益人、理由和审批链，未新增免审业务。|
|BEQR-074|隐私桌台权限|已核对；适用范围见证据|本段无桌台接口；Controller保留平台范围检查，不因商户档案权限开放隐私桌台。|
|BEQR-143|四类主体|已核对；适用范围见证据|BusinessEntityBindingRecord四对象分开：merchant及contract/operation/settlement；Service756-801分别验证，不以单一公司字段替代。|
|BEQR-144|经营主体关系|已核对；适用范围见证据|MerchantRecord与binding分别持有经营和财务关系；Service945-968禁止普通入口改既有非空三主体。|
|BEQR-145|城市项目公司|已核对；适用范围见证据|Jdbc1223-1238保存公司与项目多对多，1241-1246历史商户只允许空项目补录或同项目，不发明固定隶属链。|
|BEQR-146|运营公司职责|已核对；适用范围见证据|主体角色枚举及独立operationEntityId保留，Service创建时校验角色；不是新增完整公司业务系统。|
|BEQR-147|主要收入归属|已核对；适用范围见证据|本段仅存财务主体和管理费字段，不计算收入分配；锁住财务三主体变更，不替代结算审批。|
|BEQR-148|合作模式|已核对；适用范围见证据|Service合作模式枚举与HTML六值相合，saveMerchant校验，旧值保留合法范围。|
|BEQR-149|职级|已核对；适用范围见证据|职级从授权服务判断；没有将最高职级等同真实U盘最高权限。|
|BEQR-150|终端十一端|已核对；适用范围见证据|本段无十一端实现；不以后台Controller对外声称所有终端完成。|
|BEQR-151|最终权限计算|已核对；适用范围见证据|Controller功能点加范围，Service对象范围加真实身份；没有用菜单隐藏替代服务端授权。|
|BEQR-152|项目与敏感数据权限|已核对；适用范围见证据|五组列表和COUNT共享项目谓词；MerchantRecord项目与Controller手机号脱敏分别控制，不把有项目权等同敏感原文权。|
|BEQR-153|终端开关优先级|已核对；适用范围见证据|本段未改终端开关；受保护入口未增加绕开公共授权的开关。|
|BEQR-154|综合体项目状态|已核对；适用范围见证据|Service项目六状态白名单与ProjectRequest/Record一致；容量非负。|
|BEQR-155|商户/运营主体状态|已核对；适用范围见证据|Service主体五状态及商户存档、删除标志分开；普通新建不接收终止，既有终止状态缺省更新保留。|
|BEQR-156|合同状态|已核对；适用范围见证据|合同状态由合同模块承担；当前只引用合同主体，不在本段发明合同流转。|
|BEQR-157|员工状态|已核对；适用范围见证据|员工状态由access模块承担；当前真密钥判定要求账号active，未新增员工状态。|
|BEQR-158|设备状态|已核对；适用范围见证据|本段不维护设备状态；没有以空间启用代替设备准入。|
|BEQR-159|权限状态|已核对；适用范围见证据|权限状态由授权模块承担；本段未增加全局权限状态或豁免。|
|BEQR-166|合同审批链|已核对；适用范围见证据|Supervisor持久化保留链、申请人和直接受益人；038不提供财税三主体免审通道。|
|BEQR-189|管理费方式|已核对；适用范围见证据|管理费值沿用既有字段；租金非负，当前模块不另发明计费算法。|
|BSM-001|整体说明：这套系统到底管什么|已核对；适用范围见证据|综合体项目、公司、商户、空间形成归属；不把M009后台通过写成全部系统已上线。|
|BSM-003|系统通用能力：数据可改可删、操作有引导、权限分岗|已核对；适用范围见证据|业务对象保存归属，生命周期操作留痕，删除保留数据库记录；普通档案更新不覆盖归属和历史状态。|
|BSM-004|配置数据可以存档、也可以删除（删除要审批）|已核对；适用范围见证据|商户和空间类型均先存档、占用阻止、真实最高权限审批、申请人不得自审；SpaceTypeRequest描述与新增入口冲突见CX-B5-01。|
|BSM-005|后台的"新建/录入"改成分步引导|已核对；适用范围见证据|M009仅后台DTO/服务/仓储；无向导页面，不宣称分步引导或旧项目补录页面已完成。|
|BSM-006|验证用的二维码一律动态刷新（间隔可在后台设定）|已核对；适用范围见证据|本段没有验证二维码生成/刷新实现，未把内部标识符当静态凭证。|
|BSM-007|平台管理员拥有全部日常功能权限|已核对；适用范围见证据|普通platform_admin仍有日常权限；授权密钥身份单独识别，054只修岗位模板生命周期已有入口。|
|BSM-009|基础管理|已核对；适用范围见证据|项目、商户、主体、空间基本档案持久化；项目负责人/地址/范围三项保存读取且允许历史为空。|
|BSM-028|主要功能|已核对；适用范围见证据|商户档案、空间和主体绑定提供楼宇运营的归属；具体招商合同和工单不由本段重新实现。|
|BSM-029|和谁有关|已核对；适用范围见证据|空间与商户项目一致、初始主体存在且同项目；跨域引用不以名称字符串猜测。|
|BSM-072|商户管理：商户在授权范围内完整使用自己的系统|已核对；适用范围见证据|普通商户不能访问平台治理配置，商户业务测试改为明确自身merchant；不开放其他商户。|
|BSM-074|和谁有关|已核对；适用范围见证据|合作商户关联项目及财务主体，合同路径采用intent.projectId，不再无依据塞默认项目。|
|BSM-114|各类入口：不同人只看自己该看的、只做自己该做的|已核对；适用范围见证据|后台入口实时权限与数据范围适用；锁中员工手机考勤片段与现HTML有字词漂移，M009不实现该动作，另见观察。|
|BSM-115|相互关系：为什么这些子系统必须连在一起|已核对；适用范围见证据|Leasing→Merchant→binding调用链实读，未来绑定不提前修改商户投影；同商户未结束绑定并发由行锁保护。|
|BSM-120|已确认规则和实施前输入项|已核对；适用范围见证据|当前M009执行最高权限、中文、隔离、留痕已确认边界；footer日期漂移不产生业务规则变更，不修改HTML或锁。|
|GEN-001|必要性与最小性|已核对；适用范围见证据|复用既有ProjectRecord、授权、审计、幂等和仓储；013已批准三个可空字段，不发明页面或调度。|
|GEN-002|可执行正确性|FAIL；CX-B5-01|283项测试通过覆盖主/拒绝路径；DTO文档声称更新与重复编码拒绝不符，CX-B5-01。|
|GEN-003|测试有效性|已核对；适用范围见证据|检查69服务、23JDBC及直接依赖测试断言；054先红后绿，055前提明确且原非法路径仍拒绝，056命中精确身份错误。|
|GEN-004|信任边界校验|已核对；适用范围见证据|Service在写入前校验项目归属、主体/商户/父空间存在、状态、非负数和生效日期；SQL参数化。|
|GEN-005|授权与最小权限|已核对；适用范围见证据|Controller功能权限+Service对象范围；真实MASTER的三个固定字段和active，普通最高职级不可代替。|
|GEN-006|数据完整性|已核对；适用范围见证据|Jdbc商户upsert不改project_id；专用补录仅空或同项目；绑定状态与商户投影在事务内更新。|
|GEN-007|并发与幂等|已核对；适用范围见证据|IdempotencyService169-172把action+完成放同TransactionTemplate；merchant rowlock586-602序列化同商户未结束绑定。|
|GEN-008|接口与模式一致性|FAIL；CX-B5-01|ProjectRequest/Record/V427/Jdbc三字段一致；SpaceTypeRequest注释仍声称重复编码可更新，见CX-B5-01。|
|GEN-009|错误处理与恢复|已核对；适用范围见证据|具体ApiException拒绝且不伪成功；历史绑定不重开，未来绑定不得早生效，失败不先改财务投影。|
|GEN-010|安全与敏感信息|已核对；适用范围见证据|手机号仓储加密，Controller所有商户响应调用脱敏；动态计数SQL表名来自系统目录、merchantId参数化，未输出秘密。|
|GEN-011|可观测与审计证据|已核对；适用范围见证据|审计metadata携带project、reason、真实decision及业务ID；spaceType删除完成最近待审申请，旧审批历史保留。|
|GEN-012|资源与性能边界|已核对；适用范围见证据|分页1..500；五组COUNT与分页同条件，M:N用EXISTS避免多行放大；直接按主键读取不依赖前500条。|
|GEN-013|构建配置与依赖|已核对；适用范围见证据|实际JDK21、Maven离线和complex_test验证；只解析三个数据库变量，不source全环境；没有新增依赖。|
|GEN-014|数据库迁移正确性|已核对；适用范围见证据|V427增加可空字段/FK/关系表，既有历史不猜填；23JDBC用例运行目标测试数据库。|
|GEN-015|界面可用性与可访问性|已核对；适用范围见证据|本模块无页面，键盘/焦点等不适用；后端面向用户message使用中文，内部code按HTML豁免。|
|GEN-016|文档可验证性|FAIL；CX-B5-01|旧DTO注释的更新承诺当前无法复现，CX-B5-01；已修项不宣称页面或项目卡消费可用。|
|GEN-017|业务来源分类|已核对；适用范围见证据|逐内容按冲突→细化→技术性→添加；有上位HTML的DTO/状态均属细化，不能整体塞技术性。|
|GEN-018|第一性原理证据|已核对；适用范围见证据|以当前代码/HTML/实跑测试为依据；旧PASS及注释本身不替代实际行为。|
|PGM-001|整体定位：平台治理层不是一个业务部门，而是所有业务的底座|已核对；适用范围见证据|后台治理提供项目归属、权限入口、空间/商户/主体基础对象，不取代业务部门。|
|PGM-002|治理顺序：一件业务能不能做，要先经过这些判断|已核对；适用范围见证据|入口功能点、平台/项目范围、对象状态、写入留痕逐层执行；真USB仅明文适用分支。|
|PGM-003|模块入口：平台治理层包含哪些管理模块|已核对；适用范围见证据|本段只承载治理模块部分入口，不宣称入口目录代表全功能已实现。|
|PGM-004|综合体和集团|已核对；适用范围见证据|ProjectRecord/Request与仓储支持项目档案三项资料；集团视图仅预留关系。|
|PGM-005|空间和点位|已核对；适用范围见证据|空间project/parent/merchant相互校验；类型生命周期保留；DTO新增说明错漏见CX-B5-01。|
|PGM-006|商户和主体|已核对；适用范围见证据|商户基本资料及独立经营/合同/结算主体引用，不把录档、业务合同和财税变更混为一项。|
|PGM-007|合同和准入|已核对；适用范围见证据|直接依赖Leasing成交前置项目真实传递；未新增合同审批豁免。|
|PGM-008|员工和手机|已核对；适用范围见证据|本段未实现员工手机端；授权账号状态和来源继续由现有access管理。|
|PGM-009|权限和职级|已核对；适用范围见证据|Controller细功能点与Service真实最高权限各司其职，未将管理员统称最高权限。|
|PGM-010|终端开关|已核对；适用范围见证据|未修改终端启停规则；M009没有新的关闭隔离开关。|
|PGM-011|设备管理|已核对；适用范围见证据|空间作为设备归属基础，当前无设备状态替代逻辑。|
|PGM-012|商户侧|已核对；适用范围见证据|平台治理配置不向普通商户开放；范围测试保持合法商户身份与原拒绝断言。|
|PGM-013|开放接口|已核对；适用范围见证据|Controller参数/状态失败显式；未对第三方新增全局数据通道。|
|PGM-014|留痕复核|已核对；适用范围见证据|审计元数据和审批申请/决策持久化；最近待审更新保留旧链。|
|PGM-015|楼宇运营|已核对；适用范围见证据|经营空间、招商转商户及公司项目关系提供楼宇归属，工单/能耗不在本段实现。|
|PGM-016|应急与隐私|已核对；适用范围见证据|手机号存储加密及返回脱敏；当前模块没有新增隐私原文出口。|
|PGM-017|综合体和集团：当前一个项目，未来能看集团|已核对；适用范围见证据|当前单综合体落地；项目负责人、地址、范围已按批准后台可空实现；集团关系预留不宣称汇总页面完成。|
|PGM-018|物理项目与运营公司：多对多协作，不是固定隶属层级|已核对；适用范围见证据|BusinessEntity_projects复合主键、多对多append，列表EXISTS项目过滤；禁止通过商户通用更新迁移项目。|
|PGM-019|各主体具体负责什么|已核对；适用范围见证据|contract/operation/settlement主体存在且同项目；既有非空三主体不能普通修改；未自动套默认公司。|
|PGM-020|主要功能|已核对；适用范围见证据|项目状态/负责人/地址/经营范围保存读取；商户和空间提供归属；新增空间类型注释失真单列。|
|PGM-021|业务意义|已核对；适用范围见证据|所属项目和独立主体支持后续跨域统计；不将当前后台有限能力扩大成集团所有业务完成。|
|PGM-026|主要功能|已核对；适用范围见证据|档案、空间类型、存档恢复/删除申请均有状态和权限；type占用时不得删除，未来未结束绑定仍计占用。|
|PGM-027|业务边界|已核对；适用范围见证据|商户状态不代替合同范围，财税主体变更另受约束；历史失效绑定不能重开。|
|PGM-031|外部经销商主体、合同价格与资金治理|已核对；适用范围见证据|外部经销主体不套内部员工权限；平台治理入口排除非平台范围；合同价格审批无新增绕过。|
|PGM-035|提成发放和人员服务治理|已核对；适用范围见证据|本段无提成计算；审批持久化保留直接受益人，056测试补登记后仍验证真实身份。|
|PGM-040|权限和职级：每一个单一功能都可以单独授权|已核对；适用范围见证据|Controller按单一功能授权，Service补对象和身份条件，不用职级替代全部条件。|
|PGM-041|权限规则|已核对；适用范围见证据|项目/商户隔离无关闭开关；最高权限与平台管理员分离；所有商户响应脱敏，审批申请人/受益人不得自审。|
|PGM-042|职级规则|已核对；适用范围见证据|普通高职级管理员测试期望MASTER_AUTHORITY_REQUIRED，真实USB依固定身份判定。|
|PGM-043|外包人员与审计人员（2026-06-29 新增）|已核对；适用范围见证据|requirePlatformScope排除商户/外包，仅active platform_admin既有桥接；手机号原文不因有档案权限自动放行。|
|PGM-044|桌台、挂单与经销业务权限|已核对；适用范围见证据|本段无桌台/挂单/经销订单实现；当前授权和审批记录未新增敏感权限免审分支。|
|PGM-048|商户侧：在授权业务范围内全开放|已核对；适用范围见证据|不开放跨商户/跨项目平台数据；055合法商户卡路径证明商户隔离仍执行，未声称项目卡消费已补齐。|
|PGM-060|会员身份、营销与生物信息治理|已核对；适用范围见证据|本段不注册会员/营销/人脸；无新增后台会员入口，商户联系人PII沿用加密脱敏。|
|PGM-065|连接关系：平台治理层怎么连接所有业务|已核对；适用范围见证据|实读Leasing→Merchant/绑定和事务；项目、空间、商户基础引用不形成无归属对象。|
|PGM-066|已拍板规则和待排期问题|已核对；适用范围见证据|中文message、操作留痕和项目归属适用；集团页面/终端/会员业务不以本段PASS代替。|

## 对抗式：反例及实际验证
1. 无项目/跨项目/商户角色：五组分页COUNT同过滤，M:N使用EXISTS避免公司多关联导致count重复；Service对象读写逐一校验，空项目不回落全局。Jdbc按ID直接取商户，不受前500条影响。
2. 普通最高职级冒充密钥、申请人/受益人自审：真实身份同时要求active和SessionIssuer三个固定值；054已让真实身份到达岗位模板已有动作，普通staff/admin仍拒绝，受益人必填和利益冲突继续拦截。
3. 财务主体重复或提前投影：未来生效保存inactive+end=null，不先改商户结算主体；merchant锁＋未结束检查拒绝重复记录；已有active不可普通禁用以清空投影，历史结束记录不得重开，非空三主体不可普通换值。
4. 空间类型删除恢复/历史：disabled和archived可恢复；占用拒绝删除，未来未结束绑定计占用；完整结束且日期有效的inactive记录方可不占用。删除只软删，审批更新最近pending记录并保留旧历史。
5. 输入与调用契约：面积/租金/容量不得负数，主体/merchant/parent必须存在和同项目，旧请求三项目资料可空；空间类型重复code被拒绝且新建固定active，与DTO注释“存在则更新”不一致。

现有17项修复逐一重核：
013 ProjectRequest/ProjectRecord/Jdbc/V427三可空资料保存读取；
018新建terminated拒绝、已有terminated缺省更新保留；
030初始已给财务主体存在且同项目；
031已结束绑定不重开、日期有效且不清投影；
038商户项目字段、专用null补录、公司项目多对多、五组列表COUNT隔离及真实密钥身份；
039空间项目必填/同项目parent和merchant；
040不允许普通禁用active财务绑定或改非空三主体；
041未来inactive不提前投影、锁商户防重复未结束绑定、Leasing初始财务null；
042两种归档状态可恢复；
043审计业务ID/reason和真实decision；
044审批最近待审终态及旧历史保留；
045只有真实结束失效记录不占用；
046非负容量/面积/租金；
047面向用户message中文；
054真实密钥进入岗位模板生命周期而原守卫不放宽；
055合法商户范围测试达到资金/通知/脱敏目标且原非法项目卡内部消费仍拒绝；
056直接受益人补完整后测试命中精确最高权限身份守卫。
048-053登记项不修，未把其ID存在误当待修；本次未增加业务发明。

## 测试证据与限制
使用本轮B3后、B4前已实跑的同一修后源码测试证据；本次验证文件SHA与after一致，没有冒称B5重新运行283项。Grok可独立复跑，不能借用其未收到的结论。
命令（160/backend，JDK21，独立测试库complex_test）：
mvn -o -q -Dtest=PlatformServiceTests,PlatformJdbcIntegrationTests,PlatformControllerTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,AuthorizationServiceTests,SupervisorReviewServiceTests,M006SecurityRegressionTests,M010RegressionTests,BarStoredValueFundChainJdbcIntegrationTests,StoredValueNotificationIntegrationTest -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test test
真实exit=0，11类283项，0failure/0error/0skip；054存在旧代码RED→修后GREEN，055额外拒绝断言保留。不是仅编译或只跑新增用例。
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

数据库凭据仅解析/etc/complex/complex.env三个既有数据库变量，不source全env，不输出密钥。无新依赖、无生产迁移、无发布。
本会话已实际读取并沿用收窄的using-superpowers、grilling、codebase-design、diagnosing-bugs、systematic-debugging、TDD、requesting/receiving-code-review及verification-before-completion；使用事实已记历史self原始报告。此次遵循其追真实入口、反证、验证结论的要求；没有第三席。graft用于已有依赖定位，不以索引代替全源码读取。

## 本席本次全部问题（1项）
### CX-B5-01：空间类型请求注释仍承诺存在则更新，实际新增入口拒绝
位置：backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java:6。
原句：/** 条目3 空间类型字典 录入/修改请求（后台管理员维护）。typeCode 为唯一键，存在则更新名称/排序/状态。 */
实际：PlatformService.java:533-559，551-553已有编码抛SPACE_TYPE_CODE_EXISTS，555-558新建固定active。PlatformServiceTests.java:374-387的createSpaceTypeRejectsExistingCodeEvenWhenActive和createSpaceTypeIgnoresRequestedDisabledAndStaysActive已实际通过，证明该更新承诺不能执行。
分类顺序：不认定HTML原文冲突；空间类型属于BSM-004与PGM-005/020现有字典维护的下位请求说明，因此为细化（有工程缺陷），不是新业务添加，也不能以DTO整体技术性跳过。依据类型：可执行产物＋可复现命令输出；冻结依据GEN-002/GEN-008/GEN-016，业务上位BSM-004、PGM-005、PGM-020。严重度低，但第一次按注释调用已有编码即失败，不依赖偶发组合。
最小拟改：删除“录入/修改”和“存在则更新”的错误承诺，说明为“空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。”；仅修该Java注释及必要自身版本，不增加修改接口、不改变状态行为、不动HTML。
影响：调用者不会再把新增API当作更新入口。所有运行规则保持，现有测试已提供行为证据。当前B5只记录，未修改；正式ID与队列须完整双席报告归并后由B2确定。


## 非功能观察（不列新增修复队列）
对69条业务判据保存的HTML片段做当前原文匹配：67完全匹配，BSM-114及BSM-120两条不同，未隐瞒为全匹配。
BSM-114：锁内员工手机文字“看排班、考勤打卡、请假”，现HTML“看排班、查看考勤、请假”。
BSM-120：页脚日期由锁内2026-09-08变为现HTML2026-09-10。
本次20文件没有考勤动作或HTML页脚实现；上述不改变本模块归属/财务/权限判断，不能据此认定HTML自身业务错误，也不申请修改HTML或擅自改criteria-lock。记录现行原文差异供相应模块按既有流程处理。
本次没有锁外必要判据；锁与sidecar/ledger继续同值。项目资料只后端和数据库，旧项目补录UI未实现，不宣称已实现。

## 本席终票
FAIL。整模块/87映射审完，本次唯一新问题CX-B5-01；严重度低仍不能有条件PASS。需要完整收齐Grok B5票后统一按回退规则进入B2；当前不得边审边改，也不得把B4双PASS写成M009收口。
