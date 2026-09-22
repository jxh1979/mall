CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M011 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T16:36:45Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`；工作区 tree `db17a7e19abb46db81a421f88ed2018abb0d0e33`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M011「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段」，3 文件（计划冻结 1342 行；当前含 M009/M010 修复期与本模块 B3-r1 新增断言），映射 87 判据（与 M009 相同集合）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M011-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M010 已于 2026-09-09 经 PR #428 合并（HEAD=origin/main=bd445728）；本模块 B3-r1 修复仍在工作区未提交。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | 408 | 19405 | 8d45ab35958af17b6b4355ff0d52d02f298648be5d9c35bd2aa7b6722d151480 | 全读 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 496 | 23454 | 710ba743d54d7a2fc94d0fbbe07a62ea47840e5b5c4b52d30963bd72a389bcf4 | 全读 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 865 | 43634 | 3b8968be3fae0629fa38959c32b5c8bdd09e0af40be85de2e5414977fd3d1b3d | 全读 |

## 2. 可执行证据原文（/tmp/m011_b5r1_run.log）

```
START=2026-09-09T16:36:10Z
HEAD=bd445728a6ec7bb36a9e7ff19fe56927410ac61e
TREE=db17a7e19abb46db81a421f88ed2018abb0d0e33
=== BACKEND: the three M011 classes + governance neighbours ===
BACKEND_EXIT=0
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.941 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.736 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.116 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.753 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.257 s -- in com.complex.platform.access.AccessJdbcIntegrationTests
[INFO] Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
=== test inventory (@Test on its own line) ===
PlatformControllerTests: @Test=12 lines=408 version=// 文件版本：V0.02
PlatformJdbcIntegrationTests: @Test=16 lines=496 version=// 文件版本：V0.04
PlatformServiceTests: @Test=34 lines=865 version=// 文件版本：V0.04
=== static probes (post-fix) ===
retired codes (fixed_rent_plus_commission|revenue_share|"draft" outside service negative cases):
backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java:165:                null, null, null, null, null, null, null, null, nu
backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java:180:                null, null, null, null, null, null, null, null, nu
backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java:197:                "project-bad-merchant", "create", List.of(), "mch_
backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java:247:                "project-archived-mch", "create", List.of(), merch
fake remove: 0
拒硬删: 0
FiveValues: 0
asMerchantUser in tests: 1
requirePlatformScope negatives in controller: 4
unarchive/rejected/deleteReason: 6
archive HTTP: 2
Idempotency in binding test names: 0
=== being-tested M009 sources untouched (git diff vs HEAD) ===
(empty above = unchanged)
=== DB (complex_test, SELECT only) ===
410
chk_merchant_delete_status
chk_merchants_business_format
chk_merchants_contact_phone_pii
chk_merchants_secondary_contact_phone_pii
TREE_AFTER=db17a7e19abb46db81a421f88ed2018abb0d0e33
END=2026-09-09T16:36:18Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文（BSM-004 软删/存档/恢复/理由、BEQR-148 合作方式六类、BEQR-154/155 状态、BSM-072/074/PGM-027/041 作用域、PGM-018/019 主体角色、GEN-003 测试有效性）反推「这三份测试应当证明什么」，逐一对照修复后测试实际断言了什么（不依赖 B1–B4 结论）。
- 顺从式线：87 冻结判据逐项（§5；无实现面的判据明确原因）。
- 对抗式线：把每条测试当作可被绕过的证据看——fake 语义是否与 Jdbc/HTML 一致、夹具是否仍绕过服务层白名单、名称与断言是否一致、被修复的安全闸覆盖是否可回退即红、首跑即绿的覆盖用例是否指向真实行为；实跑三套件 + 相邻 access/结算套件（§2）。
- 依赖只读追流（不审）：被测 `PlatformService`/`PlatformController`/`JdbcPlatformRepository`/`PlatformRepository`（M009 已闭环，本轮 git diff HEAD 为空）、`support/TestSecurityActors`、`MerchantRecord.withDelete/withArchive`、complex_test（V410）。
- 3 文件全文逐行读完（修复后 PlatformControllerTests V0.02、PlatformJdbcIntegrationTests V0.04、PlatformServiceTests V0.04，行数见 §1）；B3-r1 diff 19791 bytes 逐字对照（B4-r1 双 PASS）。

## 4. 已定稿 7 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | fake `deleteMerchant` 置 `deleteStatus=approved` 留行；`approveDeleteSoftDeletesArchivedMerchantWhenNoReferences` 断言默认列表不含、`listMerchants(true)` 含且 approved；:706 注释软删 | RED 1 条→GREEN（§2 B3 记录）；与 JDBC `approvedDeletedMerchantStaysQueryableWithIncludeArchived` 同口径；`merchants.remove` 0 命中 |
| 002 | 否（已修） | JDBC 5 处 lease/active、2 处 joint_operation；Controller 3 处 lease | 三文件残留退役码只剩服务层非法值负例（:165/:180 保留，符合定稿）；16/16、12/12 绿 |
| 003 | 否（已修） | `unarchiveMerchantRestoresDefaultListVisibility`、`rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus`（含 deleteReason 与仍在 `listMerchants(true)`）、控制器 `archivingMerchantRequiresManagePermissionAndIdempotencyKey`（200 形状+权限码+缺键 400+服务只调一次） | 未造 archive_reason fake、未改 MerchantRecord；对抗：把 unarchive 改为不清 archivedAt / rejected 改为 approved / 缺键放行，各自即红（断言值具体） |
| 004 | 否（已修） | 服务层 `merchantActorIsRejectedByPlatformScopeOnGovernanceWrite`（createMerchant/archiveMerchant → PLATFORM_SCOPE_REQUIRED）；控制器 `otherGovernanceReadEndpointsRequirePlatformScope` 循环五 GET | 删掉任一读端 `requirePlatformScope()` 或服务写路径首行即红；不为每个 verb 各写一条 |
| 005 | 否（已修） | `…OutsideSixValues` + 六值注释；两条绑定用例改名 `…RequiresManagePermission` + 注释指向共享缺键闸 | 无新请求；名称与断言一致 |
| 006 | 否（登记） | `PlatformController.createSpaceType` 仍无 `requireIdempotencyKey`（M009-022 口径缺陷否；`IdempotencyService.execute` 仍必填） | 未改源码，与定稿一致 |
| 007 | 否（登记） | 观察项均未动 | 与定稿一致 |

## 5. 87 判据逐项核对（修复后；本模块为测试文件，判据落到「测试是否证明 HTML 口径」）

| 判据 | 适用面 | 结论 |
|---|---|---|
| BEQR-063/074/145/146/147/149/150/151/153/156/157/158/159/166/189、BSM-005/006/009/029/114/115/120、PGM-002/003/007/008/010/011/013/015/017/021/031/035/042/043/044/065/066 | 三份测试无对应实现面；未发明相反规则 | 不适用 |
| BEQR-143 四类主体 | 绑定唯一/存在性/激活投影用例 | 通过 |
| BEQR-144/PGM-018/019 | 三角色白名单用例；父链列删除断言 | 通过 |
| BEQR-148 合作方式六类 | 服务负例拒退役码；JDBC/Controller 夹具现为 lease/joint_operation（002 已修） | 通过 |
| BEQR-152/PGM-060 | PII 掩码/明文/信封用例 | 通过 |
| BEQR-154/155 状态 | 服务负例拒 draft/zzz；JDBC 商户夹具现为 active（002 已修） | 通过 |
| BSM-001/003 | 建档/绑定/存档删除引导与可改可删（003 用例） | 通过 |
| BSM-004 软删/存档/恢复/理由 | fake 软删同口径（001）；取消存档、驳回、deleteReason、存档 HTTP（003） | 通过 |
| BSM-007 最高权限 | `asMasterAuthority`/非最高权限拒批用例 | 通过 |
| BSM-028 基础管理主要功能 | 商户/空间/绑定/合作方式用例 | 通过 |
| BSM-072/074、PGM-012/027/041/048 作用域 | 服务层商户 actor 负例 + 七个治理 GET 全部有 403 负例/verify（004 已修） | 通过 |
| GEN-001 | 无死测试；新增用例各有 HTML 上位 | 通过 |
| GEN-002 | 94/94 绿（§2：三类 62 + AccessJdbc 24 + 结算 S1 8） | 通过 |
| GEN-003 测试有效性 | 001 断言指向 HTML/JDBC 语义并经红→绿；003/004 覆盖用例断言具体可回退即红；005 名称与断言一致 | 通过 |
| GEN-004 | 服务校验名称/枚举/存在性；夹具不再直写白名单外业务码 | 通过 |
| GEN-005 | 权限码 + 平台作用域两层均有可失败断言 | 通过 |
| GEN-006/007 | 唯一 active 绑定、幂等去重、共享缺键闸用例 | 通过 |
| GEN-008 | fake 与 Jdbc 删除语义一致；夹具枚举与服务白名单一致；方法名与实现六值一致 | 通过 |
| GEN-009 | 400/403/404/409 断言 | 通过 |
| GEN-010 | 无真实秘密；PII 用例 | 通过 |
| GEN-011 | 审计事件断言；删除理由落记录断言 | 通过 |
| GEN-012 | 分页上限用例 | 通过 |
| GEN-013 | JDBC 硬编码 complex_test（007 登记的项目约定） | 通过（OBS） |
| GEN-014 | `residualMigration…` latest()=410 | 通过 |
| GEN-015 | 无界面 | 不适用 |
| GEN-016 | 「拒硬删」/FiveValues/Idempotency 名称均已改 | 通过 |
| GEN-017/018 | 本报告分类与证据 | 通过 |
| PGM-001 全中文 | 测试断言的中文文案与内部码 | 通过 |
| PGM-004/005/006/009/014/016/020/026/040 | 相应用例存在（部分覆盖，无相反规则） | 通过 |

## 6. 本次 Findings

无新的未定稿工程缺陷。

对抗式反例逐条结果（只读推演 + 实跑）：
1. fake 软删后 `findMerchantById` 仍命中、`countMerchantReferences` 路径不变；`approveDelete…StillReferenced` 仍 MERCHANT_IN_USE 且商户仍在（8 条存档/删除用例全绿）。
2. 夹具改为 lease/active 后 JDBC 分页、正式字段、PII 三例断言值同步且绿；`revenue_share`→`joint_operation` 更新路径仍证明列可改。
3. 五个治理 GET 的 403 循环：任一端点缺 `requirePlatformScope` 即 200/缺 detail_code 而红。
4. 商户 actor 打 createMerchant/archiveMerchant：`AuthorizationService.requirePlatformScope` 对 merchantId 非空抛 PLATFORM_SCOPE_REQUIRED（服务首行），与控制器无关。
5. 被测 M009 源码本轮 git diff HEAD 为空（§2）；三份 HTML/锁/迁移/权限种子未改。

## 7. 观察（登记、不入队）

- OBS-B5-01：controller 的 `archivingMerchant…` 用例第二次请求依赖 `requireIdempotencyKey` 在 `service.archiveMerchant` 之前执行（生产顺序即如此）；若日后调整控制器顺序，该用例会红——这是期望的保护，非缺陷。
- OBS-B5-02：`rejecting…` 用例沿用 `asMasterAuthority()` 换人，覆盖了 MERCHANT_SELF_APPROVE_FORBIDDEN 之外的路径；自审闸首轮不走到（007 口径）。
- OBS-B5-03：检查点提交（PR #428）后 HEAD=bd445728，三份测试文件相对 HEAD 的差异即 B3-r1 diff；未提交。
- 技能：`using-superpowers`、`verification-before-completion`（结论附实跑）、`receiving-code-review`（Grok B2-r1 意见逐条核实后落地）、`test-driven-development`（001 红→绿；覆盖类用例首跑绿并做可回退推演）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。3 文件三线全审、87 判据逐项核对；5 项修复无回归、无新未定稿缺陷；无添加项、无 questions、无 HTML 修改申请；未提交/推送/合并/部署（M011 修复未入检查点）。本报告落盘前未读取 `M011-b5-r1-grok-raw.md` 任何正文。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M011 Codex独立补充复查完整报告
版本：V0.02

结论PASS；Grok NOT-CALLED，无本次命令/退出码/新Grok票。遵循用户“从M009自己再审；无问题只需自己审，有问题才恢复原完整流程”的指令；保留历史Claude Fable 5.1/Grok双PASS及B3累计1，不把此次单席写成新双PASS。本次修复0，无BLOCKED、未答问题或HTML申请。
日期2026-09-14；160:/home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466；仅直接SSH，无MCP/第三席。旧self V0.01的13184bytes完整保留，SHA a5f39791ec90b6cb1dcf5b1f432e089e44c510daf0a07b557b35c58a300dc58b。
控制V0.17、计划V0.04、锁V0.11；三文件冻结1342行，当前2632行（M009/M010/M016等已批准伴生及本模块历史修复），本次全文审，不按旧行数截断。87判据与M009相同，不增删判据。

## 1. 三文件全文覆盖
| 路径 | 行 | SHA256 |
|---|---:|---|
| backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java | 454 | a6ca2117d2dcfbe39af334a0fa13281ddb98e6e703fd72637c56e44521b1c01a |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 734 | 6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 1444 | 55621eb2ce9de563100d5513cdfcd6e19ee2140420f0434e4530880ab14a3f1e |

Controller454行、JDBC734行、Service1444行含fake全部读完。读取分段无缺口；未修改三文件。直接被测PlatformService/JdbcPlatformRepository/Controller在本会话M009已全文审验并双PASS，当前M011按断言有效性重新独立检查。

## 2. 三线与四分类
第一性原理依据为三份HTML中的“所有数据默认按项目隔离”“申请人不能审批自己的申请”“删除不是真从数据库里抹掉…可查、可审计、可恢复”“持授权U盘密钥的最高权限本人发起的操作，不需要经过任何审批”“物理项目与运营公司是多对多关系”和项目档案负责人/地址/经营范围要求。来源分别BEQR-152/144、BSM-004/007、PGM-004/017/020。本次不改HTML。
顺从式逐87判据及所有106测试方法核对请求、夹具、实际调用、断言、finally资源回收。
对抗式反证：fake若物理remove，已存档列表断言会失败；普通管理员若冒充最高权限，精确身份负例会失败；未来绑定若提前写三主体，三处null断言会失败；查询若去项目谓词，五组内容与COUNT会失败；若去行锁，两条事务等待断言或唯一预约结果会失败；若取消脱敏/加密，对应响应/DB断言会失败。以上是实际断言的静态反证，不声称运行变异测试。
分类依冲突→细化→技术性→添加：业务断言可指向上述HTML条目，为细化；测试框架/固定夹具/mock/fake/安全上下文/真实事务探针/限定范围清理/断言工具为无业务发明技术性。JDBC试写仅complex_test并默认回滚；并发用例使用随机唯一前缀且只清理自身行。没有发现新添加业务、新冲突或必须修复缺陷。
边界：fake的setMerchantArchiveReason和lockMerchant为空，故不能证明真实持久化/锁；真实SQL锁由JDBC539–609证明，审核理由由事件与JDBC历史断言证明。未把fake证明范围夸大。Service测试名含Upserts的一条只做新建，现有重复拒绝测试明确拒绝更新；不改历史测试名凑新修复。

## 3. 本会话实际执行证据与当前一致性
本次未重复运行。M009已实际运行三类共106（Controller14+JDBC23+Service69），全部0失败/0错误/0跳过，Maven exit0。根11类283/Grok10类239为有重叠结果，不相加。
本次逐源SHA比较M009 B3-r2 after tree e4c10154a405e61e1bfad9fedb6fde11068f653a与当前三文件：全相同。读取当前Surefire XML，全部case可在当前源找到；与本会话封存M009-b5-r2-self-raw.md/M009-b5-r3-self-raw.md实际输出相符。M009-r3只改DTO注释，不改运行逻辑。
```json
{
  "files": {
    "backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java": {
      "sha256": "a6ca2117d2dcfbe39af334a0fa13281ddb98e6e703fd72637c56e44521b1c01a",
      "r2_after_tree_sha256": "a6ca2117d2dcfbe39af334a0fa13281ddb98e6e703fd72637c56e44521b1c01a",
      "same_as_M009_test_source": true
    },
    "backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java": {
      "sha256": "6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f",
      "r2_after_tree_sha256": "6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f",
      "same_as_M009_test_source": true
    },
    "backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java": {
      "sha256": "55621eb2ce9de563100d5513cdfcd6e19ee2140420f0434e4530880ab14a3f1e",
      "r2_after_tree_sha256": "55621eb2ce9de563100d5513cdfcd6e19ee2140420f0434e4530880ab14a3f1e",
      "same_as_M009_test_source": true
    }
  },
  "suites": {
    "PlatformControllerTests": {
      "tests": "14",
      "failures": "0",
      "errors": "0",
      "skipped": "0",
      "all_executed_methods_in_current_source": true,
      "cases": [
        "archivingMerchantRequiresManagePermissionAndIdempotencyKey",
        "listBusinessEntityBindingsUsesEntityBindingViewPermission",
        "creatingSpaceUsesGovernanceSpaceManagePermission",
        "listingMerchantsReturnsPlaintextPhoneWithPiiPermission",
        "projectAssociationEndpointsConsumeEachFunctionalPermissionOnce",
        "createBusinessEntityBindingRequiresManagePermission",
        "associationEndpointRejectsMissingKeyAndDeniedPermissionBeforeService",
        "otherGovernanceReadEndpointsRequirePlatformScope",
        "listingMerchantsReturnsApiResponsePageShape",
        "listingMerchantsRequiresPlatformScope",
        "creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed",
        "creatingMerchantWithoutManagePermissionReturnsPermissionDenied",
        "unauthenticatedPlatformListReturnsUnauthorizedOrForbidden",
        "changeBusinessEntityBindingRequiresManagePermission"
      ]
    },
    "PlatformJdbcIntegrationTests": {
      "tests": "23",
      "failures": "0",
      "errors": "0",
      "skipped": "0",
      "all_executed_methods_in_current_source": true,
      "cases": [
        "approvedDeletedMerchantStaysQueryableWithIncludeArchived",
        "v339SeedsContactPhonePlaintextViewPermission",
        "concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot",
        "onlyEndedInactiveBindingsStopOccupyingMerchant",
        "createUpdateAndListMerchantPersistsFormalFields",
        "entityRoleCheckRejectsSpvAndOperation",
        "organizationScopedRolesMayShareNameAcrossOrganizations",
        "findSpaceByIdIsNotTruncatedByListLimit",
        "approvedProjectStorageExistsWithoutHistoricalBackfill",
        "migrationCreatesPlatformMasterDataTablesAndSeedsPermissions",
        "createAndListBusinessEntityPersistsFormalFields",
        "projectOnlyBackfillPreservesEveryOtherDatabaseColumnAndLegacyNulls",
        "merchantPhoneReSaveDoesNotDoubleEncrypt",
        "listMerchantsPagedReturnsRealTotalAndReachesNextPage",
        "parentEntityColumnIsRemovedFromBusinessEntities",
        "completingSpaceTypeRequestKeepsPriorRequestHistory",
        "plaintextPhoneWriteIsRejectedByDatabaseConstraint",
        "residualMigrationCreatesBindingContractTablesAndSeedsPermissions",
        "spaceTypeDictionaryRejectsUnknownStatus",
        "createProjectAndSpacePersistsHierarchyFields",
        "scopedSqlFiltersAllFiveTotalsAndSupportsCompanyManyToMany",
        "bindingRepositoryPreventsSecondActiveBindingForSameMerchantAndKeepsHistory",
        "merchantContactPhonesArePersistedAsCiphertextAndReadBackAsPlaintext"
      ]
    },
    "PlatformServiceTests": {
      "tests": "69",
      "failures": "0",
      "errors": "0",
      "skipped": "0",
      "all_executed_methods_in_current_source": true,
      "cases": [
        "spaceTypeAuditHasObjectIdAndActualRejectedDecision",
        "createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant",
        "directDeleteSpaceTypeRequiresArchiveAndMasterApproval",
        "createSpaceTypeUpsertsDictionaryAndEnablesItForSpaceValidation",
        "activeBindingCannotBeDisabledToReplaceFinancialSubjects",
        "archiveSpaceTypeRequiresReasonAndCanUnarchive",
        "archiveMerchantHidesItFromDefaultListingAndKeepsWithIncludeArchived",
        "createMerchantRejectsBusinessFormatOutsideSixValues",
        "createSpaceTypeIgnoresRequestedDisabledAndStaysActive",
        "savingAnAlreadyTerminatedMerchantDoesNotReactivateItWhenStatusIsOmitted",
        "approveDeleteSoftDeletesArchivedMerchantWhenNoReferences",
        "futureBindingDoesNotChangeCurrentMerchantSubjects",
        "requestDeleteMerchantWithoutArchiveIsRejected",
        "occupiedSpaceTypeCannotBeDeleted",
        "createMerchantRequiresFormalMerchantName",
        "createSpaceRejectsStatusOutsideCheckSet",
        "updateMerchantKeepsArchiveAndPendingDeleteState",
        "initialMerchantRejectsUnknownOrForeignBusinessEntities",
        "createBusinessEntityBindingSyncsMerchantEntityColumns",
        "createSpaceRejectsUnknownSpaceType",
        "dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen",
        "requestDeleteOnAlreadyDeletedMerchantIsRejected",
        "rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus",
        "masterProjectBackfillChangesOnlyTheProjectAndCannotMoveIt",
        "createSpaceRejectsUnknownProjectMerchantOrParent",
        "deletedSpaceTypeCanBeRestoredButPendingRequestCannotBeBypassed",
        "createProjectRejectsUnknownStatusOrMerchant",
        "unarchiveMerchantRestoresDefaultListVisibility",
        "createBusinessEntityUsesIdempotencyAndWritesAudit",
        "masterDirectDeleteStillRejectedWhenMerchantReferenced",
        "updateMerchantWritesAuditAndPreservesId",
        "masterRequestDeleteSpaceTypeExecutesWithoutApproval",
        "ordinaryProfileUpdateCannotClaimOrMoveProject",
        "approveDeleteByNonMasterIsForbidden",
        "updateMerchantRejectsUnknownMerchant",
        "projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin",
        "bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority",
        "negativeCapacityAreasAndRentAreRejectedWhileZeroAndNullRemainValid",
        "createMerchantRejectsCooperationFormOutsideSixModes",
        "requestDeleteByMasterAuthorityExecutesWithoutApproval",
        "projectCreationReturnsApprovedOptionalProfileFields",
        "spaceTypeDeletionCompletesOnlyTheCurrentRequest",
        "requesterCannotApproveOwnMerchantDelete",
        "archiveCannotPullPendingDeleteBack",
        "listMerchantsFiltersByBusinessFormat",
        "listSpaceTypesPageSizeCoversAllItems",
        "createProjectUsesOpenApiAuditEventNames",
        "changeBusinessEntityBindingRejectsInvalidAction",
        "createSpaceAcceptsDictionarySpaceType",
        "createMerchantRejectsStatusOutsideEnumeratedStates",
        "merchantActorIsRejectedByPlatformScopeOnGovernanceWrite",
        "spaceTypeDeleteGoesArchiveRequestThenMasterApprove",
        "approveDeleteRejectsWhenMerchantStillReferenced",
        "companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster",
        "merchantDeleteAuditKeepsReasonAndRecordsRejectionAgainstTheMerchant",
        "createBusinessEntityWithSlashCollidingKeysGetDistinctIds",
        "createBusinessEntityRejectsRoleOutsideWhitelist",
        "createBusinessEntityBindingRejectsMissingMerchantOrEntity",
        "employeeCannotReadOtherProjectThroughSpaceLookupOrWrite",
        "spaceRequiresCurrentProjectAndNonnegativeArea",
        "futureBindingCannotActivateOrEndBeforeItsStart",
        "ordinaryMerchantProfileCannotCauseImmediateExit",
        "createSpaceTypeRejectsExistingCodeEvenWhenActive",
        "userFacingRoleValidationDoesNotIncludeEnglishEnumNames",
        "listSpacesWritesViewedAudit",
        "anotherFutureBindingCannotReserveTheSameMerchant",
        "associationRequiresExistingExplicitProjectReasonAndIdempotency",
        "masterHasNoGlobalListOrOrdinaryProfileBypass",
        "archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding"
      ]
    }
  },
  "lock": "b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a",
  "head": "e456ba96c84185c8d39f18f548011daac48a6466"
}
```

## 4. 全部87冻结判据逐项核对
| 判据 | 本次结论与证据 |
|---|---|
| BEQR-063 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-074 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-143 | 细化/PASS：Service278–289、710–766、929–943分别断言三主体与财税变更锁定；JDBC448–485证明公司项目多对多，无固定父级。 |
| BEQR-144 | 细化/PASS：Service278–289、710–766、929–943分别断言三主体与财税变更锁定；JDBC448–485证明公司项目多对多，无固定父级。 |
| BEQR-145 | 细化/PASS：主体角色白名单与三主体分离测试保留；不使用档案权限更换既有财务主体，不测试或宣称收入分配算法完成。 |
| BEQR-146 | 细化/PASS：主体角色白名单与三主体分离测试保留；不使用档案权限更换既有财务主体，不测试或宣称收入分配算法完成。 |
| BEQR-147 | 细化/PASS：主体角色白名单与三主体分离测试保留；不使用档案权限更换既有财务主体，不测试或宣称收入分配算法完成。 |
| BEQR-148 | 细化/PASS：Service167–178验证退役合作值拒绝、lease和空值；JDBC128–208以lease→joint_operation验证更新，未定义新模式。 |
| BEQR-149 | 细化/PASS：真实最高权限按SessionIssuer三固定字段；普通管理员审批被拒，申请人自审被拒，密钥本人发起与占用约束分别有断言。 |
| BEQR-150 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-151 | 细化/PASS：真实最高权限按SessionIssuer三固定字段；普通管理员审批被拒，申请人自审被拒，密钥本人发起与占用约束分别有断言。 |
| BEQR-152 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| BEQR-153 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-154 | 细化/PASS：Service182–205、1025–1074断言项目/商户合法状态、负值拒绝、终止资料不误复活；没有让普通档案更新直接退出。 |
| BEQR-155 | 细化/PASS：Service182–205、1025–1074断言项目/商户合法状态、负值拒绝、终止资料不误复活；没有让普通档案更新直接退出。 |
| BEQR-156 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-157 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-158 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-159 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-166 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BEQR-189 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BSM-001 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| BSM-003 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| BSM-004 | 细化/PASS：软删fake保留行；Service291–458、491–588、960–1016验证存档/删除/恢复、占用、自审限制及审计；JDBC346–362、522–535验证留库及历史申请。 |
| BSM-005 | 不适用：三份后台测试无业务向导页面，未承诺页面已完成。 |
| BSM-006 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| BSM-007 | 细化/PASS：Service306–316和438–445验证密钥本人直办；355–364仍拒占用；Controller权限守卫和Service身份守卫分层验证。 |
| BSM-009 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| BSM-028 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| BSM-029 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| BSM-072 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| BSM-074 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| BSM-114 | 不适用：本测试模块无员工手机考勤功能；冻结片段与现HTML考勤措辞差异为已登记观察，未新增规则或修改HTML。 |
| BSM-115 | 细化/PASS：JDBC539–609真实事务与双线程锁等待+唯一成功，未来绑定不提前改商户三主体；不以顺序fake测试替代并发事实。 |
| BSM-120 | 细化/PASS：中文错误、隔离、可查可恢复和留痕有对应断言；已知HTML页脚日期变化不构成业务测试要求。 |
| GEN-001 | PASS：必要性：HTTP形状与授权、服务业务分支、真实SQL三层各自检不同事实；fake仅支撑服务测试，无新增抽象。 |
| GEN-002 | PASS：可执行正确性：当前源码与本会话已执行after树逐SHA相同，14+23+69=106，零失败/错误/跳过。 |
| GEN-003 | PASS：测试有效性：检查实际调用和断言；删查询谓词/保留物理删除/提前写财务主体/去掉行锁均会触发已有针对性断言（静态反证，未声称运行变异测试）。 |
| GEN-004 | PASS：信任边界：缺名称/非法枚举/未知对象/跨项目/负数均验证；三份测试输入为固定假数据，不导入用户真实敏感资料。 |
| GEN-005 | PASS：授权：商户/普通管理员/真实密钥/同申请人分别构造；Controller权限mock仅测试控制器，不作为Service授权通过的证据。 |
| GEN-006 | PASS：完整性：更新保留存档与删除状态；只补project_id用整行JSON差异断言；真实DB唯一绑定拒绝，无静默覆盖。 |
| GEN-007 | PASS：并发幂等：主体同键只存一次且只记一次成功事件；JDBC2线程先证实均等待商户锁，再断言仅一条未来预约且商户三主体未变。 |
| GEN-008 | PASS：模式一致：三文件当前DTO签名/六类枚举/项目字段匹配当前生产代码；JDBC判定实际CHECK与索引；HTTPmock不是仓储一致性证据。 |
| GEN-009 | PASS：错误恢复：assertThrows/精确错误码或约束名+状态保留，错误不可被成功响应掩盖；HTTP缺权限和缺键不进service。 |
| GEN-010 | PASS：敏感信息：模拟手机号固定；真实DB查询密文前缀且不能含原号码；响应原文需PII权限，未记录凭据。 |
| GEN-011 | PASS：审计证据：业务ID/reason/decision/actorId断言真实事件；已封存XML、源SHA、行覆盖与本次不重跑声明一致。 |
| GEN-012 | PASS：资源边界：分页/501条边界有实际断言；并发线程数2、等待上限5/10秒、finally清理仅本测试随机前缀数据并关闭线程。 |
| GEN-013 | PASS：构建：本会话JDK21/Maven已成功执行，当前同SHA；本次只读报告和源码，不修改依赖或配置。 |
| GEN-014 | PASS：迁移：本文件只验证Flyway版本/列/索引/约束；DB集成@Transactional默认回滚，并发独立事务仅清理自身夹具，不执行生产迁移。 |
| GEN-015 | PASS：界面：无UI，不要求焦点等；userFacingRoleValidation断言错误message不夹带内部英文枚举，固定测试英文名不对业务用户展示。 |
| GEN-016 | PASS：文档：记录计划1342行与当前2632行不同；旧Claude票与本次Codex票分开；Upserts命名测试实际只验证插入，重复拒绝由375–379另测，不据名称声称支持更新。 |
| GEN-017 | PASS：分类：业务断言为HTML条目细化；mock/fake/TestSecurityActors/事务/测试随机前缀/清理为无业务发明技术性；无新添加或冲突。 |
| GEN-018 | PASS：第一性原理：HTML原文、当前106条实际方法/断言和同源实际执行结果共同支撑；旧PASS不作为充分证据。 |
| PGM-001 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-002 | 细化/PASS：Service306–316和438–445验证密钥本人直办；355–364仍拒占用；Controller权限守卫和Service身份守卫分层验证。 |
| PGM-003 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-004 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-005 | 细化/PASS：软删fake保留行；Service291–458、491–588、960–1016验证存档/删除/恢复、占用、自审限制及审计；JDBC346–362、522–535验证留库及历史申请。 |
| PGM-006 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| PGM-007 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-008 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-009 | 细化/PASS：真实最高权限按SessionIssuer三固定字段；普通管理员审批被拒，申请人自审被拒，密钥本人发起与占用约束分别有断言。 |
| PGM-010 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-011 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-012 | 细化/PASS：平台范围和权限拒绝在HTTP面验证，缺键与无权限时verifyNoInteractions(service)，未创建第三方越权路径。 |
| PGM-013 | 细化/PASS：平台范围和权限拒绝在HTTP面验证，缺键与无权限时verifyNoInteractions(service)，未创建第三方越权路径。 |
| PGM-014 | 细化/PASS：Service984–1016断言事件对象ID、真实rejected、理由与comment；JDBC522–535保存先前rejected和当前approved两条历史。 |
| PGM-015 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| PGM-016 | 细化/PASS：JDBC626–675断言密文存储/可读回/不二次加密/明文直写拒绝，Controller分别验证有无敏感权限的原文与掩码。 |
| PGM-017 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-018 | 细化/PASS：Service278–289、710–766、929–943分别断言三主体与财税变更锁定；JDBC448–485证明公司项目多对多，无固定父级。 |
| PGM-019 | 细化/PASS：Service278–289、710–766、929–943分别断言三主体与财税变更锁定；JDBC448–485证明公司项目多对多，无固定父级。 |
| PGM-020 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-021 | 细化/PASS：三测试覆盖档案/项目资料及归属；Service946–957和JDBC448–518验证负责人/地址/范围保存与历史空值，未将后端测试称为集团页面完成。 |
| PGM-026 | 细化/PASS：软删fake保留行；Service291–458、491–588、960–1016验证存档/删除/恢复、占用、自审限制及审计；JDBC346–362、522–535验证留库及历史申请。 |
| PGM-027 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| PGM-031 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-035 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-040 | 细化/PASS：真实最高权限按SessionIssuer三固定字段；普通管理员审批被拒，申请人自审被拒，密钥本人发起与占用约束分别有断言。 |
| PGM-041 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| PGM-042 | 细化/PASS：真实最高权限按SessionIssuer三固定字段；普通管理员审批被拒，申请人自审被拒，密钥本人发起与占用约束分别有断言。 |
| PGM-043 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| PGM-044 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-048 | 细化/PASS：Controller所有治理读范围负例及脱敏双分支，Service1101–1177、1248–1260和JDBC448–518验证行/总数/旧资料归属不越项目；最高权限无全局列表旁路。 |
| PGM-060 | 不适用：本三文件不实现该判据对应的酒吧高危/桌台/终端/合同审批/员工设备/权限状态/管理费/二维码/经销商/提成/会员流程；夹具未引入相反业务规则，不据此宣称其他模块已完成。 |
| PGM-065 | 细化/PASS：空间/项目/商户/主体引用分别有缺失与跨项目反例，先验合法夹具与失败后的无写入断言并存；合同完整流程属其他模块。 |
| PGM-066 | 细化/PASS：中文错误、隔离、可查可恢复和留痕有对应断言；已知HTML页脚日期变化不构成业务测试要求。 |

## 5. 结论与历史保持
PASS：当前三文件2632行全文及87判据完成，未发现新的必须修复问题。旧M011-001–005已修、006/007登记，共7项队列全否保留；不以当前新发现名义重复旧观察。006历史“字典入口缺幂等”仅登记，未改成已修。无新ID、无本次B2/B3/B4/Grok，B3仍1。
三HTML、源码、测试、锁均未改；未提交推送合并部署。旧self前缀及Grok原报告保持。本次按用户例外完成Codex单席补充复查，不冒充双PASS。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M011 当前锁完整 B5-r1（Codex 独立席甲）
版本：V0.03

## 输入与独立性
2026-09-22，160 main 主工作区 HEAD e456ba96c84185c8d39f18f548011daac48a6466。完整读取三份当前测试 2776 行，冻结时1342行仅历史计数，未以历史PASS代替此次审查。三线：逐项业务上位要求及四分类；测试断言、fake与真实实现的对应关系和反例；跨文件接口/数据库/授权接缝。本B5再次逐行读取三完整文件，87个冻结判据（69 HTML与18 GEN）以本轮已读、哈希未变的完整定义逐项重新对照。不新增判据/功能，不修改测试或生产代码；本报告落盘前未读取本次 Grok B5 新正文。旧B2-r2用于确认既有ID最终范围，不能以旧B2-r1被撤回的方案重新造缺陷。

| 文件（backend/src/test/java/com/complex/platform/governance/） | 版本/行数 | SHA256 |
|---|---|---|
| PlatformControllerTests.java | V0.04 /532 | 21e657c73b07a6e974e55f90156c2bbdd1338838729c097dcc67b373b7d57a8e |
| PlatformJdbcIntegrationTests.java | V0.09 /760 | b42d8a292f7bcf3718703d996a4fe5d275069cbefe2472eba96f504c73ed48da |
| PlatformServiceTests.java | V0.17 /1484 | c942935719f044f997b614783a3aacec8848172f9bf1d6a89c1a8e63248f1e16 |

## 本轮有效验证及B5复核
在正本backend运行 mvn -q -Dtest=PlatformControllerTests,PlatformJdbcIntegrationTests,PlatformServiceTests test，真实exit=0。只读取/etc/complex/complex.env中三个测试连接变量，不显示密钥；从Surefire XML核对：Controller19、Jdbc24、Service70，合计113，failure/error/skipped均0。该命令在本轮B1真实运行，B5未重复执行；B5重新读取XML核准113结果并逐文件SHA全等，不拼接历史次数，不把B5读XML说成新跑。Grok被要求不并发运行Maven。绿测试不是单独的审计依据，下面逐类检查能否捕获错误。

## 完整范围审阅与反例追证
Controller 1—532：未经认证、缺权限、缺幂等键、普通响应遮蔽、合法PII权限明文、字典操作权限、存档入口、7个读取入口平台范围、两绑定写入口管理权限、两关联入口的明确权限和错误路径，均有行为或调用断言。315/352名称现为ManagePermission，没有虚称验证幂等；435—493真实AuthorizationService参数化四种读写/授权组合，第一次响应后消费一次性权限，同一grant复用403、分离grant后续遮蔽，并verify消费一次；494消费争用失败关闭且verify未进入服务。服务本体在MVC切片被mock，故只宣称入口安全/映射，业务效果另由后两文件验证。当前治理HTML批准持有效权限的两联系号码明文，164用例并非旧隐私规则的回归。

Jdbc 1—760：联系人姓名与手机号密文且往返一致（38/652/673）；不以可读回代替落库加密断言，687确实匹配明文约束错误。主体、项目、空间字段与层级、合同/绑定结构、合法枚举、动态迁移、唯一活动绑定、软删除默认隐藏/含存档保留374，直接走真实Jdbc。392查到500条列表之外记录，避免列表裁切被当不存在；474两项目列表/总数/分页/主体多对多均检查，未知/空项目与错误范围不泄漏。514仅回补project_id且整行其余字段不变、既有归属不能移动、老调用不抹归属。548申请历史闭环。566并发用两个真实service+独立事务，在数据库锁等待发生后放行，结果一成功一ACTIVE_BINDING_EXISTS；未来绑定不改当前三个财务主体。该用例NOT_SUPPORTED并finally清理创建的业务关系/实体；其余Transactional回滚。706无父链；717检查闭集完整定义并用一个非法值触发约束，不能要求同一已失败事务连续验证另一个值。734同名不同组织成功、同组织重复拒绝。测试库参数沿用既有约定，无新业务默认值。

Service 1—1484：每例独立fake/授权上下文，收尾clear；默认/显式项目隔离与商户actor拒绝；必要字段、枚举、非负量与引用校验；合法更新不改三财务主体/删除状态；真实最高权限与同申请人不得自审；存档/恢复/驳回/删除占用检查及审计；创建类型不能覆写或复活已删除类型、显式恢复可以；已有merchant terminated状态可保留但普通更新不可把正常商户直接退场；主体slug冲突不混淆；未来绑定预留、到期激活、关闭后不可重开、挂起不能偷换当前主体；项目新增可空三资料、经营公司与项目多对多、最高权限显式关联且原因/幂等键/存在性必填；普通资料更新不能搬项目；分页审计只含当次对象及必要上下文。1302—1484 fake逐行追踪：deleteMerchant保留记录并approved；lockMerchant空实现仅单线程fake，真实并发已由Jdbc覆盖；setMerchantArchiveReason空实现按既有B2-r2明确收窄，不为测试虚构record字段。fake不证明SQL，Jdbc单独对应。

最小反事实核对：移除入口scope拒绝→Controller299遍历负例不再403；物理删merchant→Service543/ Jdbc374包含已存档行断言失败；取消加密→Jdbc38/652/687密文/约束断言失败；忽略project过滤→474或1142跨项目计数/结果失败；忽略未来绑定排他→Jdbc566双成功违反断言；不消费一次权限或消费失败继续→Controller435/494失败；提前改变结算主体→Service901/Jdbc566断言失败。以上是源码级反事实，不冒称执行了变异测试。

## 冻结判据覆盖（分组列明全部ID）
- BEQR-143/144/145/146/147、PGM-004/006/017/018/019/020/021、BSM-001/009/028/029/115：主体类别、无父链/多对多、项目资料及三财务主体绑定断言对应Service249/318/901/986/1249与Jdbc118/237/706/717。业务细化；未声称测试模块本身补齐集团报表或资金全链。
- BEQR-148/154/155、PGM-005/026/027：合法合作方式/项目状态/商户状态、空间与点位、恢复类型，Service207—318/430—503/1000、Jdbc405。细化；退役码仅负例或特定约束反例，不是业务合法值。
- BEQR-063/149/151/152/159、BSM-003/004/007/072/074/114、PGM-002/009/012/014/040/041/042/043/048/065：权限、范围、真实最高权限、申请人利益冲突、软删留痕。Controller71—101/217—416，Service333—395/543—612/1142—1288。测试技术实现服从细化要求，不把角色名等同真实密钥；审计人员独立流程并不由此模块穷尽。
- PGM-001/003/066：模块定位/中文业务错误/已拍板规则，Service1094及当前授权手机号例外与恢复、未来生效、项目关联都按现锁核。既有明确授权生效；未据概要发明新实现。
- BEQR-074/150/153/156/157/158/166/189、BSM-005/006/120、PGM-007/008/010/011/013/015/016/031/035/044/060：分别涉及隐私桌台、终端、合同/员工/设备/审批链/管理费、引导UI/二维码、其它已确认输入、开放API/楼宇/应急/经销/提成/会员生物等。已读映射原文；本三份拓扑后台测试未实现这些子系统，未出现反向业务断言，不强加跨域全覆盖。PII相关现存联系人测试确实受PGM-041/043当前文本约束，不能借不适用绕开。
- GEN-001：复用测试结构，未扩功能；GEN-002：新113通过与真实断言；GEN-003：上述反事实、fake/Jdbc分界；GEN-004：缺参数/枚举/引用负例；GEN-005：真实授权/范围/一次性消费；GEN-006：保留删除行、财务主体/关系不损坏；GEN-007：幂等返回/审计一次与真实并发；GEN-008：DTO/record/SQL列与合法fixture对应；GEN-009：确定错误码和负例；GEN-010：加密/遮蔽/授权明文且无秘密输出；GEN-011：成功失败、原因、对象与拒绝留痕；GEN-012：分页边界、500之外查找、线程finally关闭；GEN-013：现有Maven依赖环境实跑，未引包；GEN-014：Jdbc查迁移与结构，不修改历史SQL；GEN-015：本模块非界面；GEN-016：方法名现已收窄，旧报告不代表现行为；GEN-017：测试实现技术性、业务断言细化，001历史冲突已删除；GEN-018：上述必要性及错误路径证据，无锁外推断入队。

## 既有7 ID 当前复核
| ID | 现态/最小处置 | 本次修复队列 |
|---|---|---|
| M011-001 | 历史冲突已修；fake1360软删，Service543默认隐藏但includeArchived保留，Jdbc374真实留行。 | 否 |
| M011-002 | 历史细化已修；正例lease/joint_operation+active；退役码仅负例。Jdbc明文PII反例匹配目标约束，不以draft说明合法状态。 | 否 |
| M011-003 | 历史细化已修；恢复566、驳回579/deleteReason585、HTTP217。按B2-r2不造archiveReason fake字段；不能以被撤回r1反指漏修。 | 否 |
| M011-004 | 历史细化已修；Service601商户actor，Controller299五读端+既有另外两端。 | 否 |
| M011-005 | 历史技术性已修；SixValues、两个ManagePermission方法名，不虚称幂等；B2-r2明确不增两重复用例。 | 否 |
| M011-006 | 既有跨模块技术观察；Controller177不重复requireIdempotencyKey，Service539经executeWrite。沿M009-022最终口径缺陷否；不擅自新增接口要求。 | 否 |
| M011-007 | 既有技术观察更新：现113绿、真实并发已覆盖、同申请人真实master拒绝已直接到达；旧环境红/旧计数/旧未联立不能冒充当前问题。保留硬编码测试库、宽泛认证401或403、权限实现归access等边界。 | 否 |

## 独立结论
B5-r1席甲整模块审计结论PASS。本B5重新完整读取Controller 1—532、Jdbc 1—760、Service 1—1484（工具截短1113—1116已另外取回），不是只看旧finding或B3差异。当前完整范围未证实新工程缺陷，无新增finding，无添加项，无HTML修改申请；findings V3.54现定稿7ID全否仍成立。B3累计1，本轮未修复。不以此票覆盖Grok，模块在Grok本次完整B5报告和四值校验前保持IN_PROGRESS。
额外复核B2定稿12观察：01种子不等于有效PII权；02PGM-043指针解释不扩锁；03Upserts只名称观察且重复码另例拒绝；04真JDBC并发有时序依赖但本次已实际通过；05接受401/403的认证负例范围有限；06空密钥是切片占位；07draft原始INSERT仅PII约束反例；08生命周期HTTP不重复校验不等于服务缺权限；09archiveReason空fake按r2最终范围；10spv单违例+CHECK字符串；11默认TCP口令失败不等同当前应用测试失败；12PhoneMask注释与master放行分层。全12不遗漏，不升格为新增业务。现GEN-016观察不得用来发明upsert/扩fake。
旧报告的“存档/删除理由落仓储”只能按当前实际已覆盖deleteReason解释，不宣称fake存了archiveReason；旧报告的106/57计数不能代替本次113。
本次未调用子代理或MCP。用户新增提交/合并/推送授权已登记；fetch只更新远端引用，本模块源/HTML/锁/主index/pre-push均未改；发布审查与最终合并尚未完成，不能称已发布。
