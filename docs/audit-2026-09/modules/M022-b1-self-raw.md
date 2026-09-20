CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理；其中 `MerchantPortalServiceTests` 现行版本为席甲在 M020 B3-r1 所写，自审性质加倍——本席对该文件的「通过」结论不构成独立意见，以 Grok 票为准）。2026-09-20T18:21:07Z
- 控制合同 V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本主工作区 HEAD `e456ba96`，origin/main `86b8dd2a`（两文件对 origin/main 差异为空）；工作区 tree `167d26a2b287133b9db3e6627d35388125ebb15e`。三份业务 HTML 只读。
- 模块 M022「商户档案、商户类型与商户侧门户／后台验证／第1段」，2 个测试文件（计划有效内容行 227；现行 333 行，§1 实算——服务测试类已由 M020 重写），映射 22 判据（BSM-001/003/114、GEN-001…018、PGM-001）。历史未审。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M022-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/测试/文档/HTML/锁；两个测试类实跑只写 target/，JDBC 类 @Transactional 回滚；未手工写库。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java | 77 | 3898 | b3d3f2356ade47a71ec1bb97790ffd213b4cadc080ee8cbd5714f58bd2088976 | 全读 |
| backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java | 256 | 14178 | ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159 | 全读 |

只读依赖（追流不审）：`merchantportal/MerchantPortalService` + `MerchantPortalController`（M020 V0.01）、`access/AuthorizationService`、`common/idempotency/IdempotencyService`、`support/InMemoryIdempotencyRepository`、`evidence/AuditEventService`、`contract/ContractRepository` + `JdbcContractRepository`（listContractsByMerchant / listContractSpaceIds / listSettlementsByMerchant / listAllSettlementsPaged / saveContract / saveSettlement）、`contract/ContractService:132`、全仓测试对上述方法的引用。

## 2. 可执行证据原文（/home/james/audit-tmp/m022_b1_run.log）

```
START=2026-09-20T18:19:11Z HEAD=e456ba96 origin/main=86b8dd2a
== 0. two files (lines/bytes/sha256)
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java 77 3898 b3d3f2356ade47a71ec1bb97790ffd213b4cadc080ee8cbd5714f58bd2088976
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java 256 14178 ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159
== 1. vs origin/main (expect empty) / version markers / test counts
MerchantPortalJdbcIntegrationTests.java: marker= @Test=2
MerchantPortalServiceTests.java: marker=// 文件版本：V0.01 @Test=10
== 2. tests actually run (complex_test, three vars only)
MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.746 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.480 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests="2" failures="0" errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests="10" failures="0" errors="0"
== 3. JDBC class claims vs current production callers
/**
 * AUD-005 收尾(#187 复核残留,四域之外):商户专区平台管理员「全量合同/结算」真分页。
 * 坐实平台全量结算 listAllSettlementsPaged = SQL COUNT 取真 total(跨所有商户)+ LIMIT/OFFSET 第二页可达;
 * 商户自 scope 的 listSettlementsByMerchant 仍谓词隔离(只见本商户、无 LIMIT),不受影响。
 * 合同侧平台全量走既有 listContractsPaged(ContractJdbcIntegrationTests 已覆盖 501/多页)。
 */
service calls to platform-all paging: 0
production callers of listAllSettlementsPaged (outside repository): 0
production callers of listContractsPaged (outside repository): backend/src/main/java/com/complex/platform/contract/ContractService.java:132 
service scope doc (M020-001): 25: * 平台端点,本前缀不提供全量口径,M020-001);资金/审批的平台侧权威流程不变(异议只留言�
== 4. real-DB coverage of the portal read paths
service read paths: 64 66 95 
listContractsByMerchant referenced by tests: MerchantPortalServiceTests.java ContractServiceTests.java ContractScopeGuardTests.java TicketingServiceTests.java BarServiceTests.java  | real-DB (SpringBootTest) among them: 
listContractSpaceIds referenced by tests: MerchantPortalServiceTests.java ContractServiceTests.java  | real-DB (SpringBootTest) among them: 
listSettlementsByMerchant referenced by tests: MerchantPortalServiceTests.java MerchantPortalJdbcIntegrationTests.java  | real-DB (SpringBootTest) among them: MerchantPortalJdbcIntegrationTests.java 
JdbcContractRepository.listContractsByMerchant SQL: 288:                "SELECT " + CONTRACT_COLS + " FROM contracts WHERE merchant_id = :mid ORDER BY created_at DESC, id",
== 5. service-test semantics vs real implementation
scope stub throws like real impl: 104:        when(authz.currentMerchantScope()).thenThrow(SCOPE_REQUIRED);; impossible stub Optional.empty: 0
real IdempotencyService in tests: 55
AuthorizationService.currentMerchantScope real: 431:        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
test method names: setUp merchantSeesOnlyOwnContractsWithSpaceIds callerWithoutMerchantScopeIsRejectedAndAudited merchantSeesOnlyOwnSettlements merchantCanDisputeOwnOpenSettlementWithoutChangingStatus disputeKeepsResolutionTrailAndAppendsToExistingReason disputeReasonBeyondColumnWidthIsRejected disputeWithSameIdempotencyKeyIsProcessedOnce disputeOnForeignSettlementIsForbiddenAndAudited settledOrAlreadyDisputedSettlementCannotBeDisputed disputeReasonIsRequired platformAllSettlementsPagedRealTotalAndSecondPageReachable merchantScopeSettlementsStayIsolated 
== 6. controller-level behavior with no test in this package
controller tests in package: 0; repo controller test classes: 30; controller idempotency guard: 52:        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
== 7. HTML anchor sentences (read-only)
<td>商户侧页面</td>
<td>餐饮、零售、酒吧等商户</td>
<td>在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。</td>
我的对账（结算与分成明细，有疑问可留言提异议）|我的合同（关键条款、租期、到期情况）|
== 8. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=167d26a2b287133b9db3e6627d35388125ebb15e
END=2026-09-20T18:19:18Z
```

## 3. 三线审计方法与覆盖

- 对象口径：2 文件 333 行逐行读完、12 个用例逐个对照被测真实现；22 判据逐项；类注释声称按对象追现行生产调用方（§2 探针 3–6）。
- 第一性原理线：上位是业务地图 L1848-1850 商户侧页面「处理本商户…对账、结算…不看平台全局」与 L1322「我的对账（…可留言提异议）」「我的合同（关键条款、租期、到期情况）」；两文件的存在理由＝锁住商户专区后台的商户隔离、异议留言规则与真实库读路径。三问：用途成立；实际行为——12 例全绿（探针 2）；未经验证前提——JDBC 类自称验证「商户专区平台管理员全量…真分页」，而服务层自 M020-001 起没有平台全量口径、`listAllSettlementsPaged` 已无任何生产调用方（CX-001）；「我的合同只见本商户」这一铁律只在 Mockito 层被断言，真实 SQL 无任何真实库用例锁定（CX-002）。
- 顺从式线：22 判据逐项（§5）。对抗式线（grilling）：把 `JdbcContractRepository.listContractsByMerchant` 的谓词改坏（如去掉 `WHERE merchant_id`）哪个用例会红——没有（只有 Mockito 桩引用它）；把 `listSettlementsByMerchant` 的 JOIN 谓词改坏——`merchantScopeSettlementsStayIsolated` 会红（有效）；把服务层 scope 闸改回 Optional.empty 全量——`callerWithoutMerchantScopeIsRejectedAndAudited` 会红（有效，桩按真实现抛）；把合并/列宽/幂等/失败审计/中文报错任一改回——对应用例会红（有效）；JDBC 类第一例把仓储分页改坏会红，但它守护的路径现无生产调用方。
- 本轮实跑两个测试类（complex_test，只导出三变量）；本席禁止修复。

## 4. 与现行实现的对照（要害）

| 测试/注释声称 | 现行事实（§2 探针） | 结论 |
|---|---|---|
| `MerchantPortalJdbcIntegrationTests:16-21` 类注释「商户专区平台管理员「全量合同/结算」真分页…坐实平台全量结算 listAllSettlementsPaged…」；`:46-63` 第一例按此前提测仓储分页 | `MerchantPortalService` 0 处调用 `listAllSettlementsPaged`/`listContractsPaged`（M020-001：本前缀不提供全量口径，无商户账号 fail-closed）；`listAllSettlementsPaged` 在生产代码中除仓储接口/实现外 0 个调用方；`listContractsPaged` 唯一调用方是 `ContractService:132`（平台合同工作台） | CX-001 |
| 「我的合同只见本商户」由 `MerchantPortalServiceTests.merchantSeesOnlyOwnContractsWithSpaceIds` 锁定 | 该例以 Mockito 桩替掉仓储；`JdbcContractRepository.listContractsByMerchant:288`（`WHERE merchant_id = :mid`）与 `listContractSpaceIds` 在全仓测试中只被 Mockito 引用，真实库（@SpringBootTest）引用 0；服务层两条读路径（:64/:66）中仅结算侧（:95）有真实库隔离用例 | CX-002 |
| 服务测试 10 例（scope 拒绝、不改状态、保留裁决痕迹并合并理由、列宽 500、同键只认一次、跨商户失败审计、中文报错、理由必填） | 桩语义与真实现一致（`thenThrow(SCOPE_REQUIRED)` 对 `AuthorizationService:431`；真 `IdempotencyService`+内存仓储）；无「真实现不可能的返回」桩；10/10 绿 | 通过（自审，见 §0） |
| JDBC 第二例 `merchantScopeSettlementsStayIsolated` | 真实库两商户各一结算单，`listSettlementsByMerchant` 只回本商户；@Transactional 回滚 | 通过 |

## 5. 22 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| BSM-001 / BSM-003 | 通过（登记） | 测试覆盖商户自助对账/合同/留言三项，不发明业务规则 |
| BSM-114 各类入口 | 不通过（并入 CX-001/002） | 「不看平台全局」：JDBC 类注释仍宣称商户专区平台全量口径；「只见本商户」的合同侧真实 SQL 未被真实库用例锁定 |
| PGM-001 全局原则 | 通过 | 可溯源（裁决痕迹保留用例）、重复提交只认一次（幂等用例）、报错全中文（中文文案用例）均有断言 |
| GEN-001 必要性最小性 | 不通过（并入 CX-001） | 第一例守护的仓储方法现无生产调用方，其存在理由（注释）为假 |
| GEN-002 可执行正确性 | 通过 | 12/12 绿（探针 2） |
| GEN-003 测试有效性 | 不通过 | CX-002（合同侧隔离 SQL 改坏无用例会红）；CX-001（用例目的声称为假） |
| GEN-004 / GEN-009 | 通过 | 列宽超限、理由必填、不可异议状态均有 400/409 断言 |
| GEN-005 授权与最小权限 | 通过 | 三权限码 `verify(authz).requirePermission(...)`；scope 拒绝用例 |
| GEN-006 数据完整性 | 通过 | 裁决痕迹保留 + 理由合并用例 |
| GEN-007 并发与幂等 | 通过 | 真 IdempotencyService 同键两次只写一次 |
| GEN-008 接口与模式一致性 | 通过（登记） | 控制器层无测试（OBS-1） |
| GEN-010 / GEN-012 | 通过 | 无敏感数据夹具；JDBC 类事务回滚、无残留 |
| GEN-011 可观测与审计证据 | 通过 | 成功/失败审计事件名与参数均被断言 |
| GEN-013 / GEN-014 / GEN-015 | 无本面 | 无构建/迁移/界面 |
| GEN-016 文档可验证性 | 不通过 | CX-001（类注释声称不可从现行实现复现） |
| GEN-017 业务来源分类 | 通过 | 两条均细化（BSM-114 L1850 的下位验证），无添加项 |
| GEN-018 第一性原理证据 | 通过 | §2 命令原文（实跑、grep 调用方/引用方）+ 源码原文 + HTML 逐字 |

## 6. 本次 Findings（临时 ID CX-M022-00N；正式 ID 由 B2 分配）

### CX-M022-001 · 细化 · JDBC 测试类的存在理由声称为假：商户专区已无平台全量口径，`listAllSettlementsPaged` 无生产调用方

- 位置：`MerchantPortalJdbcIntegrationTests.java:16-21`（类注释）、`:30`（行内注释「listAllSettlementsPaged 走 SQL 覆写」）、`:46-63`（第一例 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`）、`:73`（「平台全量真分页不污染隔离口径」）。
- 事实：§4 第 1 行；M020-001 定稿后服务层 fail-closed、类注释明写「本前缀不提供全量口径」；`listAllSettlementsPaged` 生产 0 调用方。测试自身可执行且绿，但注释把它说成「商户专区平台管理员全量真分页」的验收——该能力不存在，读者按注释会以为商户专区对平台管理员开放全量。
- 依据类型：4（grep 调用方、实跑）+ 2（服务/测试原文）。HTML 上位：业务地图 L1850「不看平台全局」。判据：GEN-016 / GEN-001 / GEN-003 / BSM-114。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：改写类注释与两处行内注释为现行事实——本类验证合同仓储的商户维度读取（谓词隔离）与全量结算分页的仓储行为；商户专区服务层不提供平台全量口径（M020-001），`listAllSettlementsPaged` 现无生产调用方（去留归合同域，XM）；加版本标记 V0.01。不删用例（仓储方法仍在，删它属合同域决定）、不改断言。

### CX-M022-002 · 细化 · 「我的合同只见本商户」的真实 SQL 无任何真实库用例锁定

- 位置：`MerchantPortalJdbcIntegrationTests.java:65-76`（只有结算侧隔离用例）；`MerchantPortalServiceTests.java:88-103`（合同侧用 Mockito 桩）。
- 事实：§4 第 2 行；服务层合同读路径 `listContractsByMerchant`（`JdbcContractRepository:288` `WHERE merchant_id = :mid`）+ `listContractSpaceIds`，全仓真实库测试 0 引用；把该谓词改坏，现有 12 例与全仓其它测试无一会红。商户隔离是本域铁律（L1850），结算侧已有对等的真实库用例，合同侧缺。
- 依据类型：4（grep 引用方）+ 2（仓储/服务原文）。判据：GEN-003 / BSM-114 / GEN-005。四分类：细化。工程缺陷：是（测试有效性缺口）。入队：是。
- 最小拟处置：在本 JDBC 类加 1 例 `merchantScopeContractsStayIsolated`（两商户各存一份合同，`listContractsByMerchant` 只回本商户；`listContractSpaceIds` 对无绑定合同回空列表），沿用本类 `contract(...)` 夹具与 @Transactional 回滚；不改生产代码。

## 7. 观察（登记、不入队）

- OBS-1：本包无控制器测试（全仓 30 个 *ControllerTests）；控制器三条映射均薄转发，幂等键校验走共享 `ApiRequests.requireIdempotencyKey`（他处已测）；登记。
- OBS-2：`MerchantPortalServiceTests` 为席甲 M020 所写，本席对其结论为自审；若 Grok 对该文件有 FAIL，以 Grok 为准。
- OBS-3（XM → 合同域后台模块）：`ContractRepository.listAllSettlementsPaged` 与其 Jdbc 覆写现无生产调用方（M020-001 后）；去留由合同域模块审计时裁定。
- OBS-4：JDBC 类数据源属性硬编码 complex_test/complex（全仓真实库测试同范式，verify:test-db-isolation 门禁覆盖）；登记。
- 无添加项；无 HTML 修改申请；无 questions。

## 8. 结论

本席 B1 票：**FAIL**（CX-M022-001/002 为工程缺陷且建议入队；写入集合仅 `MerchantPortalJdbcIntegrationTests.java`）。2 文件全文三线审完、22 判据逐项核对、12 例实跑与 grep 调用/引用方为证据；未修文、未改 HTML/锁/台账。本报告落盘前未读取 `M022-b1-grok-raw.md` 任何正文。


## M022 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T18:37:44Z。输入：modules/M022-b1-grok-raw.md V0.01（36216 bytes，SHA256 30169fb47486896f875a51adcd5d4c1397b08aa5f8510770c459fa820cbc1bc2）、modules/M022-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M022 B2-r1 整批候选」段（全文件 SHA256 ace41683c71e87d24329d2b8798bb135d9254f59f9ae35944c3ac33683f59b5d）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01…06 与席甲 CX-001/002 全部归并（GR-01+CX-001→001；CX-002→002；GR-02…06→003…007）；XM/OBS 全部登记；无漏并、无改写。
- 自审纠正：席甲 B1 对自写的 `MerchantPortalServiceTests` 判「通过」；Grok GR-02…06 指出的 5 处断言缺口逐条核源成立（合并后列宽、异议入口 scope、结算列表成功审计、异议权限码、空理由用户可见文案），B1 §4 第 3 行与 §5 GEN-003/005/011 的「通过」撤回。
- 001 分类与处置采 Grok（冲突、删用例）；007 处置口径：服务层英文机读文案经 `GlobalExceptionHandler`→`FieldLabels.translate` 出网为「原因未填写」，用户侧零英文成立，测试锁该出网文案，不改已闭环的 M020 服务文件。
- 最小处置：只改本模块 2 个测试文件；不改生产代码/迁移/OpenAPI/HTML。无业务发明。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
