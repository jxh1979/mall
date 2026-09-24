CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；两文件现行版本均含席甲所写内容，自审性质加倍，以 Grok 票为准）。2026-09-20T19:06:32Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `ffca9860fcfe31222a407f692190fb9a887c9d81`。
- 模块 M022 2 测试文件（§1 全读），22 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M022-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；测试实跑经事务回滚/内存仓储，无残留。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java | 74 | 3728 | 12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef | 全读 |
| backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java | 292 | 16999 | 091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m022_b5r1_run.log；B1 同一取证脚本修后重跑，含两类实跑）

```
START=2026-09-20T19:06:25Z HEAD=e456ba96 origin/main=86b8dd2a
== 0. two files (lines/bytes/sha256)
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java 74 3728 12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java 292 16999 091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a
== 1. vs origin/main (expect empty) / version markers / test counts
 .../MerchantPortalJdbcIntegrationTests.java        | 37 +++++++++---------
 .../merchantportal/MerchantPortalServiceTests.java | 44 ++++++++++++++++++++--
 2 files changed, 57 insertions(+), 24 deletions(-)
MerchantPortalJdbcIntegrationTests.java: marker=// 文件版本：V0.01 @Test=2
MerchantPortalServiceTests.java: marker=// 文件版本：V0.02 @Test=10
== 2. tests actually run (complex_test, three vars only)
MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.783 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.489 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests="2" failures="0" errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests="10" failures="0" errors="0"
== 3. JDBC class claims vs current production callers

/**
 * 商户专区(3.18)读路径的真实库验证:合同仓储的商户维度读取必须按商户谓词隔离——
 * 「我的合同」listContractsByMerchant(+ listContractSpaceIds)与「我的对账」listSettlementsByMerchant
 * 都只回本商户的数据(业务地图「商户侧页面…不看平台全局」)。
 * 商户专区服务层不提供平台全量口径(M020-001:无商户归属账号 fail-closed),故本类不承载平台全量分页用例(M022-001);
service calls to platform-all paging: 0
production callers of listAllSettlementsPaged (outside repository): 0
production callers of listContractsPaged (outside repository): backend/src/main/java/com/complex/platform/contract/ContractService.java:132 
service scope doc (M020-001): 25: * 平台端点,本前缀不提供全量口径,M020-001);资金/审批的平台侧权威流程不变(异议只留言�
== 4. real-DB coverage of the portal read paths
service read paths: 64 66 95 
listContractsByMerchant referenced by tests: MerchantPortalServiceTests.java MerchantPortalJdbcIntegrationTests.java ContractServiceTests.java ContractScopeGuardTests.java TicketingServiceTests.java BarServiceTests.java  | real-DB (SpringBootTest) among them: MerchantPortalJdbcIntegrationTests.java 
listContractSpaceIds referenced by tests: MerchantPortalServiceTests.java MerchantPortalJdbcIntegrationTests.java ContractServiceTests.java  | real-DB (SpringBootTest) among them: MerchantPortalJdbcIntegrationTests.java 
listSettlementsByMerchant referenced by tests: MerchantPortalServiceTests.java MerchantPortalJdbcIntegrationTests.java  | real-DB (SpringBootTest) among them: MerchantPortalJdbcIntegrationTests.java 
JdbcContractRepository.listContractsByMerchant SQL: 288:                "SELECT " + CONTRACT_COLS + " FROM contracts WHERE merchant_id = :mid ORDER BY created_at DESC, id",
== 5. service-test semantics vs real implementation
scope stub throws like real impl: 105:        when(authz.currentMerchantScope()).thenThrow(SCOPE_REQUIRED);; impossible stub Optional.empty: 0
real IdempotencyService in tests: 56
AuthorizationService.currentMerchantScope real: 431:        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
test method names: setUp merchantSeesOnlyOwnContractsWithSpaceIds callerWithoutMerchantScopeIsRejectedAndAudited merchantSeesOnlyOwnSettlements merchantCanDisputeOwnOpenSettlementWithoutChangingStatus disputeKeepsResolutionTrailAndAppendsToExistingReason disputeReasonBeyondColumnWidthIsRejected disputeWithSameIdempotencyKeyIsProcessedOnce disputeOnForeignSettlementIsForbiddenAndAudited settledOrAlreadyDisputedSettlementCannotBeDisputed disputeReasonIsRequired merchantScopeContractsStayIsolated merchantScopeSettlementsStayIsolated 
== 6. controller-level behavior with no test in this package
controller tests in package: 0; repo controller test classes: 30; controller idempotency guard: 52:        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
== 7. HTML anchor sentences (read-only)
<td>商户侧页面</td>
<td>餐饮、零售、酒吧等商户</td>
<td>在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。</td>
我的对账（结算与分成明细，有疑问可留言提异议）|我的合同（关键条款、租期、到期情况）|
== 8. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=14ecc7496cfdd3492400dd236084731673f6e879
END=2026-09-20T19:06:32Z
```

## 3. 三线审计

- 第一性原理线：上位句同 B1（业务地图 L1848-1850、L1322）。修后：JDBC 类只承载商户维度读取的真实库隔离（合同 + 结算两条 SQL），类注释与现行实现一致、不再宣称平台全量；服务测试类 10 例覆盖 scope 三入口、权限码三处、成功/失败审计、不改状态、裁决痕迹保留与理由合并、新留言与合并后两种列宽、同键只认一次、跨商户、不可异议中文文案、空理由出网中文。
- 顺从式线：22 判据逐项——B1 不通过的 BSM-114/GEN-001/003/016 由 001/002 闭合；Grok 指出的 GEN-003/005/006/011/PGM-001 断言缺口由 003–007 闭合；其余同 B1（通过/登记/无本面）。
- 对抗式线（grilling）：B3 六条变异（仓储谓词、合并长度、scope 闸、事件名、权限码、英文文案）均使对应新断言变红、还原后全绿（/home/james/audit-tmp/m022_b3r1_run.log MUT_002…007）；被测路径均有生产调用方（探针 3/4：服务层三条读路径 + 异议写路径）；无守护死路径的用例。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–007 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1/B2 登记项（本包无控制器测试、数据源属性硬编码范式、`listAllSettlementsPaged` 生产零调用归合同域、`requireMerchantScope` 防御性冗余、DISPUTABLE 另两态无成功用例——HTML 未点名不立项）。

## 7. 结论

席甲 B5-r1 票：**PASS**。2 文件全读、22 判据逐项、取证脚本与两类实跑复核；未改任何文件；落盘前未读 Grok 本次正文。
