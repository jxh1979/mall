CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审）。2026-09-09T08:26:59Z
- 正本 HEAD 242f487233d028173cb9198eecd079f5d877405c；本报告落盘时工作区 tree `52b7cb9227a60d244bc82ae70490d7d35dc5facd`（含 B3 修复与 B4 两席 raw）。锁四值：实算 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a（sidecar/ledger 逐字相同；精确 Grok B5 raw 的最后独占锁行在读取 Grok 报告后于回执核对）。
- 顺序：Grok B5-r1 已真实退出（受理四项事实见回执）；本报告在读取 `M009-b5-r1-grok-raw.md` 任何正文之前落盘（该文件此刻存在=True，未读）。
- 只读承诺：本席 B5 未改任何源/HTML/锁/台账；测试在主仓运行、不改源；数据库探针全部 SELECT。

## 1. 文件覆盖表（20 文件当前内容，全文逐行读完；11 个未改文件 SHA 与 B1 相同并再次全读）

| 文件 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| governance/BusinessEntityRecord.java | 17 | 391 | 45431c580d0bca0c… |
| governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de8ed4d451… |
| governance/MerchantRecord.java | 90 | 5415 | 3a37d33901b32a64… |
| governance/MerchantDeleteDecision.java | 9 | 400 | e08618c856b35a1e… |
| governance/JdbcPlatformRepository.java | 1101 | 61310 | 3985ae2ab90cba20… |
| governance/PlatformController.java | 236 | 13580 | 05bd7cf4a2bfcb43… |
| governance/PlatformRepository.java | 134 | 6613 | 76b9a1d8c5c034f4… |
| governance/PlatformService.java | 854 | 41905 | 2829c83fb0db7c6a… |
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

## 2. 可执行证据（本席实跑，/tmp/m009_b5_run.log 原文）

```
START=2026-09-09T08:26:24Z
TREE=52b7cb9227a60d244bc82ae70490d7d35dc5facd
=== BACKEND: governance.*Tests + leasing.*Tests ===
BACKEND_EXIT=1
[ERROR] Tests run: 12, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.762 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.009 s <<< FAILURE!
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.103 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.091 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.724 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.073 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.041 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.003 s <<< FAILURE!
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] Tests run: 74, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
=== WEB: OrganizationMerchantPage vitest ===
WEB_EXIT=0
[2m Test Files [22m [1m[32m1 passed[39m[22m[90m (1)[39m
TREE_AFTER=52b7cb9227a60d244bc82ae70490d7d35dc5facd
END=2026-09-09T08:26:34Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文重新推导本模块的业务约束（BSM-004 存档/删除/最高权限/「下线就不能再用」、BEQR-143 四类主体、BEQR-148 合作方式六类、BEQR-154/155 状态、PGM-018/019/026/027 主体角色与统一绑定），逐条对照当前实现（不依赖 B1–B4 结论）。
- 顺从式线：87 冻结判据逐项（§5 增量表 + B1 报告 §5 全表；未变化者沿用 B1 结论并再次核对）。
- 对抗式线：针对 B3 修复面构造反例（静态追流 + `complex_test` 只读探针 + 全套测试实跑）：LIMIT 500 存在性、已存档商户复用、空状态覆盖、双写、残留注释、无引用 import、绑定主体状态、非最高权限驳回、越权读端、幻影创建、悬空引用。
- 依赖只读追流（不审）：`PlatformRepository` 默认方法与 Jdbc 覆写、`leasing/LeasingService:288-292,424`（`spaceExists` 消费方）、`access/AuthorizationService.isMasterAuthority`、M011 三测试与 `LeasingSignAtomicityJdbcIntegrationTests` 夹具、`apps/web/.../OrganizationMerchantPage.tsx`。

## 4. 已定稿 25 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001/002 | 否（已修） | `PlatformService.saveMerchant(update=true)` 先 `requireMerchant`，25 参构造保留 archived_*/delete_*/createdAt；不存在 404 | 静态 + `updateMerchantKeepsArchiveAndPendingDeleteState`/`updateMerchantRejectsUnknownMerchant` 实跑 PASS |
| 003 | 否（已修） | `MERCHANT_COOPERATION_FORMS` 六类、空允许；`LeasingService:290` null；页面六类中文 option | 实跑 PASS；`complex_test` 87 商户 cooperation_form 全 NULL（无存量非法值） |
| 004 | 否（已修） | `SUBJECT_STATUSES`/`PROJECT_STATUSES` 白名单 | 实跑 PASS；存量 87 商户 status 全 active |
| 005 | 否（已修） | `BUSINESS_ENTITY_ROLES` 三角色，400 中文 | 实跑 PASS；库 CHECK 仍含 spv/operation（025/M010） |
| 006 | 否（已修） | 绑定前 `requireMerchant` + 三主体 `businessEntityExists`（Jdbc COUNT 按主键） | 实跑 PASS；存量 0 悬空绑定 |
| 007 | 否（已修） | 项目 merchantId、空间 projectId/merchantId/parentSpaceId 存在性 | 实跑 PASS；但 parentSpaceId 用默认 `findSpaceById`（LIMIT 500）→ 见 CX-B5-02 |
| 008 | 否（已修） | `syncMerchantEntities` 绑定创建/激活回写三列；档案更新不覆盖三列 | 实跑 PASS；存量 0 条 active 绑定，无失配 |
| 009 | 否（已修） | approve 增 `isMasterAuthority` 闸 403；最高权限发起直接软删（查占用、留痕） | 实跑 PASS |
| 010 | 否（已修） | 定稿四处注释已改；`includeArchived=true` 纳入 approved 行；恢复复用 unarchive | 实跑 PASS；另有三处同类残留注释 → CX-B5-01 |
| 011 | 否（已修） | 存档路径不再调 `expireBarSingleStoreTickets`，单一四参构造 | 静态：`PlatformService` 0 处 ticketing 引用 |
| 012 | 否（已修） | M009 面 parentEntityId 全部删除；列仍在（可空） | 静态 + 探针 `parent_entity_id|YES`；`docs/openapi`、`apps/web` 0 引用 |
| 014 | 否（已修） | `listSpaceTypes` → `PageResponse.of(records, 1, 500)`；`page()`/`DEFAULT_PAGE_SIZE` 已删 | 实跑 PASS |
| 015 | 否（已修） | 七个 GET 均 `requirePlatformScope()` | 静态计数 7/7 + `listingMerchantsRequiresPlatformScope` PASS |
| 016 | 否（已修） | `requireDate`/`readObjectMap`/`OBJECT_MAP_TYPE` 已删 | 静态 0 处 |
| 013/017–025 | 否（登记） | 内容未动；017 环境红仍在（flyway 409 > 399） | 与定稿一致 |

## 5. 87 判据增量核对（相对 B1 §5 表；未列者结论不变）

| 判据 | B1 结论 | 当前结论 |
|---|---|---|
| BEQR-143 四类主体 | CX-06 | 通过（006）；CX-B5-03 涉及「下线商户」再绑定 |
| BEQR-144 / PGM-018 主体关系 | OBS-06 | 通过（012 删除 M009 面父链） |
| BEQR-148 合作模式六类 | CX-03 | 通过（003） |
| BEQR-154 / BEQR-155 状态 | CX-04 | 通过（004）；缓冲期等无实现面见 018 |
| BSM-003 / BSM-004 存档与删除 | CX-01/09/10 | 001/009/010 通过；**残留**：CX-B5-01（三处「硬删」注释）、CX-B5-03（已存档/已批删商户仍可被新项目/空间/绑定引用，「下线就不能再用」） |
| BSM-007 最高权限 | 通过 | 通过（009 复用 `isMasterAuthority`） |
| BSM-028/029、PGM-026/027 | CX-03/06/08/12 | 通过（003/006/008/015） |
| BSM-072/074/114/115、PGM-041 作用域 | CX-12 | 通过（015，7 读端） |
| GEN-001 必要性 | 通过 | 016 通过；OBS-B5-03 两处无引用 import（HEAD 既有） |
| GEN-002 可执行正确性 | CX-01/02 | 通过（实跑 66 PASS + 2 环境红） |
| GEN-004 信任边界 | CX-03–07 | 通过（白名单 + 存在性） |
| GEN-005 授权 | CX-12 | 通过（015） |
| GEN-006 数据完整性 | CX-01/06/08 | 001/006/008 通过；**残留** CX-B5-02（空间存在性判定被 LIMIT 500 截断） |
| GEN-008 接口一致性 | CX-03/04/08/11 | 通过（003/004/008/014） |
| GEN-009 错误处理 | CX-02/05 | 通过（404/400 中文） |
| GEN-012 资源边界 | 通过 | **残留** CX-B5-02 |
| GEN-016 文档可验证 | CX-10 | 定稿四处通过；**残留** CX-B5-01 |
| GEN-017 分类 / GEN-018 第一性 | 通过 | 通过；本轮无添加项、无 HTML 修改申请 |

## 6. 本次 Findings（临时编号 CX-B5-，正式 ID 在 B2 分配；ID 001–025 不复用）

### CX-B5-01 三处残留注释仍称「硬删」（M009-010 同根因，未在定稿四处之内）
- 位置：`JdbcPlatformRepository.java:187`「>0 拒硬删」、`MerchantRecord.java:27`「删除=硬删走审批」、`PlatformRepository.java:54`「>0 则拒绝硬删」。
- 事实：`deleteMerchant` 为 `UPDATE merchants SET delete_status='approved'`（Jdbc:181-183）；三处注释与实现及 BSM-004「删除只隐藏不抹除」相反。B1 两席均漏（未按同模式 grep 全单元集）；B3 只改了定稿列出的四处。
- 分类：技术性（注释）；依据 2.3 类型 4（源码原文）；判据 GEN-016 / BSM-004；工程缺陷：是（与 010 同标准：注释宣称物理删除）。
- 建议入队：是。最小处置：三行注释改软删除口径；不改行为，无测试面（编译级）。

### CX-B5-02 空间存在性判定依赖 LIMIT 500 列表，超过 500 条后旧空间被判「不存在」
- 位置：`PlatformRepository.java:79-82` 默认 `findSpaceById` 基于 `listSpaces()`；`JdbcPlatformRepository.java:294-303` `listSpaces()` `ORDER BY created_at DESC LIMIT 500`，Jdbc 未覆写 `findSpaceById`；消费方 `PlatformService.createSpace:477`（007 上级空间 404 闸）、`PlatformService.spaceExists:397`（3.13，`LeasingService:424` 招商意向空间校验）。
- 事实：第 501 条及更早创建的空间在默认实现中不可见 → 合法的 parentSpaceId/意向空间被拒（`SPACE_NOT_FOUND`/招商校验失败）。与 006/007 的 `businessEntityExists`/`projectExists`（COUNT 按主键、无截断）不一致，是本席 B3 落地 007 时沿用旧默认方法造成的残留。`complex_test` 当前 0 条空间，缺陷潜伏。
- 分类：技术性；依据类型 4（源码）；判据 GEN-006 / GEN-012 / GEN-002；工程缺陷：是（存在性判定错误，随数据量必然触发）。
- 建议入队：是。最小处置：Jdbc 覆写 `findSpaceById` 按主键 `WHERE id = :id`（与 `findMerchantById` 同法）；M011 JDBC 测试插入 501 条空间后断言最早一条可按 id 查到（TDD 红→绿）。

### CX-B5-03 已存档 / 已审批删除的商户仍可作为新项目、新空间、新绑定的引用对象
- 位置：`PlatformService.createProject:375-377`、`createSpace:472-475`、`createBusinessEntityBinding:546` 均用 `requireMerchant`（`findMerchantById` 不过滤 archived_at / delete_status）。
- 事实：对 `archived_at IS NOT NULL` 或 `delete_status='approved'` 的商户，仍可创建项目/空间归属与四类主体绑定。BSM-004 原文「下线就不能再用：已存档的（票种、商品等）不能再被拿去下单、促销、配套餐」「删除只隐藏不抹除…始终存在库里可查」——存档＝可逆软下线（M009-011 口径），把下线商户拿去建新归属/新绑定属于「再用」。更新档案、取消存档、删除申请/审批仍应命中已存档行（不改）。
- 分类：细化（HTML 原则在本模块写路径的落地缺口）；依据类型 1（HTML 原文）+ 类型 4；判据 BSM-004 / GEN-004 / GEN-006；工程缺陷：本席判是，请外审裁定（若判「HTML 沉默/属消费方模块」则改否并入 020 口径）。
- 建议入队：是（待 B2 裁定）。最小处置：`requireActiveMerchant`（存在且未存档且 delete_status≠approved，否则 400 中文 `MERCHANT_ARCHIVED`）用于上述三处；ServiceTests 各一条最小用例。

## 7. 观察（登记、不入队）

- OBS-B5-01：`PUT /merchants/{id}` 请求 status 为空时按 `requireSubjectStatus` 缺省回 active（B3 前同为 `trimToDefault(…,"active")`）；PUT 为整档覆盖语义，HTML 无「更新保留原状态」要求，不判缺陷；M012 页面提交完整档案。
- OBS-B5-02：最高权限直接软删路径先 `saveMerchant(withDelete("approved"…))` 再 `deleteMerchant(id)`，Jdbc 侧双写同一列（幂等、留痕一次）；保留是为让内存 fake（remove）与 Jdbc 语义一致，不判缺陷。
- OBS-B5-03：无引用 import：`JdbcPlatformRepository.java:15 java.math.BigDecimal`、`PlatformController.java:7 ApiException`，HEAD 已如此、非 B3 引入；无可执行内容、编译无告警，按 GEN-001 判观察（外审若判缺陷可并入清理项）。
- OBS-B5-04：绑定不校验三主体的 status（terminated/blacklisted 主体可被绑定）；HTML 对主体状态与绑定关系沉默，不判缺陷、登记。
- OBS-B5-05：`complex_test` 实态（只读）：merchants 87 行全 status=active、cooperation_form 全 NULL、delete_status 全 none、未存档；business_entities 6 行（management_company 4 / platform 1 / project_company 1，全 active）；projects 0、spaces 0、active 绑定 0；三类悬空引用 0；`chk_be_entity_role` 仍含 spv/operation（025→M010）；`parent_entity_id` 列仍在可空；flyway 最高 409（017 环境项，两条 MigrationVersions 断言红仍在）。
- OBS-B5-06：技能：`using-superpowers`、`verification-before-completion`（全部结论附实跑/探针）、`receiving-code-review`（对 Grok B4 观察逐条核实：`countMerchantReferences` 注释残留经 grep 全单元集扩为 CX-B5-01）、`grilling`（自问结构）实际使用；`ponytail-review` 用于最小处置尺度；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**FAIL**。三条新 finding（CX-B5-01/02/03）均无定稿 ID、不在修复队列；按运行手册 §8 整批回 B2 分配永久 ID（026–028）并裁定队列，再 B3-r2（B3 累计 1/3）。已修 15 项经本席重审无回归；无添加项、无 questions、无 HTML 修改申请；未提交/推送/合并/部署。本报告落盘前未读取 `M009-b5-r1-grok-raw.md` 任何正文。


## M009 第二批 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B5-r1 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M009-b5-r1-self-raw.md` V0.01 前缀 15825 bytes SHA256 765da39b4e7a3de235dbc22f9e2c1e5b0ce3d083817ca94b3bf6bd2e6d01c284；`modules/M009-b5-r1-grok-raw.md` V0.01 前缀 35274 bytes SHA256 a2d884f0c39095ed06c9f6796df0b1fea053e2f5441d2151d68fe707352b074f（Grok B5：PASS、GR-M009-B5-01…06）；候选 `findings.md` V1.21 472063 bytes SHA256 f2748b48519a3b47de239f67223a33ebd552454be91ccf56453a0b839aff306b「当前 M009 B5-r1 合取FAIL…第二批 B2-r1 整批候选」M009-026…035。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B5 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok 第二批 B2-r1 真实退出后、读取其追加段之前落盘。按 `receiving-code-review` 收窄：Grok B5 六条观察逐条对照源码核实后归并，未表演性同意；B2 不修复。
- 差集：席甲 CX-B5-01/02/03 + OBS-B5-01…04 → 026/027/028 + 032–035；Grok GR-M009-B5-01/02/03/06 → 026/029/030/031；GR-B5-04（fake remove）→ 010 既有「归 M011」口径；GR-B5-05（flyway 409）→ 017。无遗漏、无改写 Grok 原意（Grok 对 026 的「观察／否」立场在候选「工程缺陷」列并列写明，待其 B2 裁定）。

| ID | 本席票 | 理由 |
|---|---|---|
| 026 | PASS（技术性／是） | 三处注释与 `deleteMerchant` 实现（UPDATE）及 BSM-004 相反；010 定稿四处里 MerchantArchiveRequest:3、PlatformService:273 亦为纯注释而入队，同标准；处置 3 行、编译级 |
| 027 | PASS（技术性／是） | 本席 B3 落地 007 时沿用默认 `findSpaceById`（LIMIT 500 列表过滤）造成的存在性截断；与 006/007 按主键 COUNT 不一致；Jdbc 按主键覆写 + 501 条 JDBC 红绿测试为最懒可行 |
| 028 | PASS（细化／是，待外审） | BSM-004「下线就不能再用」对存档商户成立；三处写路径改用 `requireActiveMerchant`，其余路径（更新/存档/取消存档/删除申请与审批）保持命中已存档行；若外审判 HTML 沉默或消费方职责，本席接受改否并入 020 |
| 029 | PASS（技术性／否） | 空间/字典状态无 HTML 闭集，不发明 |
| 030 | PASS（细化观察／否） | B2-r2 定稿 006 后半「建档即绑定仅登记」、008 仅激活投影；不扩边界 |
| 031 | PASS（细化观察／否） | 008 最小边界；停用绑定的消费方处置归 020 |
| 032 | PASS（技术性／否） | PUT 整档覆盖语义，B3 前同；HTML 无「更新保留原状态」要求 |
| 033 | PASS（技术性／否） | 双写同列幂等，保 fake/Jdbc 语义一致 |
| 034 | PASS（技术性／否） | 无引用 import 无可执行内容，HEAD 既有；外审若判 GEN-001 缺陷，本席接受并入 026 同批清理 |
| 035 | PASS（细化观察／否） | HTML 对主体状态与绑定关系沉默 |

本席第二批 B2-r1 票：**PASS**（候选 026–035 分类、缺陷、队列如表；队列是＝026/027/028，否＝029–035）。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。
