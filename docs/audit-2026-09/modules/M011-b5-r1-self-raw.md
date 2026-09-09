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
