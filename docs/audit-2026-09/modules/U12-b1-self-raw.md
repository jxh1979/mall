CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U12 未发布材料第 12 段（设备与主管审批测试）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T01:01:04Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `a30cae1bda0f6a2c6cf660b26a2967ffa48fdb3c`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 12 段，2 文件 1519 行（gate/DeviceCredentialVerifierTests 392 / governance/review/SupervisorReviewServiceTests 1127），main 主归属 M066/M042；适用判据＝映射行并集 107 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。两文件均为纯单元测试（mock/内存仓储），在 `/tmp/u-overlay/backend` 离线 `mvn -o -Dtest=…` 运行（不连库；/tmp/u12_tests.log + surefire）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U12-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U11 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U12-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：U07 定稿（验签器改读设备级密钥）、U09 定稿（两条新策略、顶层闸、事件发布）、U11 定稿（DB 回归覆盖顶层闸）。只读依赖追流（不审）：副本 `DeviceCredentialVerifier.java`（第 7 段）、`SupervisorReviewService.java`（第 9 段）、`M010RegressionTests.java`（第 11 段）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java | 392 | 19456 | 09abf3c2de8a035095bd8eec2a5becca12d79dd79c2463af4e54f0e3c64cc9cb | 全读 |
| backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | 1127 | 76894 | bdf54d41a712ec9f38e18a023d6fe611ffd5029909c72012c46f16d80dde0972 | 全读 |

## 2. 可执行证据原文（/tmp/u12_b1_run.log）

```
START=2026-09-09T21:00:24Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java | 392 | 19456 | 09abf3c2de8a035095bd8eec2a5becca12d79dd79c2463af4e54f0e3c64cc9cb
backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | 1127 | 76894 | bdf54d41a712ec9f38e18a023d6fe611ffd5029909c72012c46f16d80dde0972
backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java 392 19456 09abf3c2de8a035095bd8eec2a5becca12d79dd79c2463af4e54f0e3c64cc9cb
backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java 1127 76894 bdf54d41a712ec9f38e18a023d6fe611ffd5029909c72012c46f16d80dde0972
=== baseline 242f4872 -> unpublished tree e28dad05 (full diff, both files) ===
diff --git a/backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java b/backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java
index 55d1cd8a..98b57af6 100644
--- a/backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java
+++ b/backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.gate;
 
+// 文件版本：V0.01
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.any;
@@ -59,8 +61,7 @@ class DeviceCredentialVerifierTests {
         when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("staff_work_phone", "bound")));
         when(authz.currentActor()).thenReturn(new AccessActor("u1", "user", "staff", "org", null, "active"));
         when(staffRepo.findProfileByUserId("u1")).thenReturn(Optional.of(profile()));
-        when(staffRepo.findActiveCredential("sp1"))
-                .thenReturn(Optional.of(credential(pubB64)));
+        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));
     }
 
     private DeviceCredentialVerifier verifier(String defaultMode) {
@@ -108,6 +109,14 @@ class DeviceCredentialVerifierTests {
         assertThat(vt.deviceId()).isEqualTo(DEVICE);
     }
 
+    @Test
+    void privatePhoneKeyCannotReplaceWorkPhoneKey() throws Exception {
+        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.empty());
+        when(staffRepo.findActiveCredential("sp1")).thenReturn(Optional.of(credential(pubB64)));
+        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sign("POST", NOW, "n1"))))
+                .isInstanceOf(ApiException.class);
+    }
+
     @Test
     void enforceRejectsWriteWithoutCredential() {
         MockHttpServletRequest r = new MockHttpServletRequest("POST", URI);
diff --git a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
index 516ebc6e..95b18586 100644
--- a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
@@ -1,3 +1,4 @@
+// 文件版本：V0.02
 package com.complex.platform.governance.review;
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
@@ -54,11 +55,12 @@ class SupervisorReviewServiceTests {
     void listPoliciesReturnsBlockingCatalogAndWritesAudit() {
         var page = service.listPolicies();
 
-        assertThat(page.total()).isEqualTo(10); // + 税费调整三审策略（M087-A-03）
+        assertThat(page.total()).isEqualTo(12); // 含排班发布和普通扣除上限审批。
         assertThat(page.items())
                 .extracting(SupervisorReviewPolicyRecord::actionType)
                 .contains("manual_release", "bill_waiver", "contract_violation", "base_definition_violation",
-                        "permission_grant_l1", "permission_grant_l2", "permission_grant_l3");
+                        "permission_grant_l1", "permission_grant_l2", "permission_grant_l3",
+                        "staff.shift.publish", "staff.payroll.penalty_cap.adjust");
         assertThat(page.items())
                 .filteredOn(policy -> "contract_violation".equals(policy.actionType()))
                 .first()
=== main HEAD vs baseline (blob) ===
backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java baseline=55d1cd8a8a head=55d1cd8a8a main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java baseline=516ebc6e3c head=516ebc6e3c main_unchanged_since_baseline=yes
=== probes: coverage of the U07/U09 changes in these unit tests ===
64:        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));
113:    void privatePhoneKeyCannotReplaceWorkPhoneKey() throws Exception {
114:        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.empty());
115:        when(staffRepo.findActiveCredential("sp1")).thenReturn(Optional.of(credential(pubB64)));
292:        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));
58:        assertThat(page.total()).isEqualTo(12); // 含排班发布和普通扣除上限审批。
63:                        "staff.shift.publish", "staff.payroll.penalty_cap.adjust");
unit_tests_covering_penalty_cap_gate_in_U12=0; integration_coverage_in_U11=1
=== overlay unit-test run (no DB): /tmp/u12_tests.log + surefire ===
START=2026-09-09T18:56:58Z
MVN_EXIT=0
END=2026-09-09T18:57:06Z
/tmp/u-overlay/backend/target/surefire-reports/com.complex.platform.gate.DeviceCredentialVerifierTests.txt:Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.295 s -- in com.complex.platform.gate.DeviceCredentialVerifierTests
/tmp/u-overlay/backend/target/surefire-reports/com.complex.platform.governance.review.SupervisorReviewServiceTests.txt:Tests run: 41, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.570 s -- in com.complex.platform.governance.review.SupervisorReviewServiceTests
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:1443:                <li>主管排班：排到几点几分�
docs/business-system-map-for-operations-v0.1.html:1458:                <li>每位员工都有一个结构化�
docs/platform-governance-map-for-operations-v0.1.html:579:                <li>私人手机 App 安装时生成
END=2026-09-09T21:00:24Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 12 段（2 文件 1519 行：`gate/DeviceCredentialVerifierTests.java` 392、`governance/review/SupervisorReviewServiceTests.java` 1127），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段真实改动 = `git diff 242f4872 e28dad05`（§2 全文 diff）：验签器测试 setUp 改为桩 `signingKeyStore.findActivePublicKey(DEVICE)`（设备级密钥）而非员工凭证，新增 `privatePhoneKeyCannotReplaceWorkPhoneKey`（无设备密钥时即使员工凭证存在也拒）；审批服务测试策略数 10→12 并断言含 `staff.shift.publish`/`staff.payroll.penalty_cap.adjust`。
- 第一性原理线：两文件是第 7 段（验签改读设备级密钥）与第 9 段（两条员工域策略）的单元级证据——HTML PGM L579/BSM L935（私人手机密钥不得替代工作手机密钥）、BSM L1443/L1458（排班上一级审批、上限三审+顶层确认）。审计点：改动是否把第 7/9 段行为锁成可失败断言；第 9 段新增的顶层闸（`DEPARTMENT_REVIEWS_PENDING`）与事件发布是否有单元覆盖。
- 顺从式线：107 项判据逐项（§5）。
- 对抗式线：单元层是否覆盖 `DEPARTMENT_REVIEWS_PENDING`（本文件 0 命中；第 11 段 `penaltyCapNeedsThreeDepartmentReviewsThenMaster…` 在空库实跑中覆盖：主会话在三线未通过前被拒）；`eventPublisher` 在单元测试为 null 时不发事件（第 9 段 OBS-1）——本文件未断言事件发布；验签器测试是否仍覆盖 kiosk 设备级密钥（`kioskUsesDeviceLevelKey` 保留）。
- 只读依赖追流：第 7 段 `DeviceCredentialVerifier`、第 9 段 `SupervisorReviewService`、第 11 段 `M010RegressionTests`。
- 2 文件全文逐行读完（§1）；diff 逐块读完（§2）。可执行验证：两文件为纯单元测试，在 `/tmp/u-overlay/backend` 离线运行：`DeviceCredentialVerifierTests` 23/23、`SupervisorReviewServiceTests` 41/41，0 失败 0 错误（§2 surefire）。本材料未发布，不做 B3。

## 4. 与发布基线 / main 的分叉对照

- 两文件 main 自基线未改（blob＝基线，§2）；须与第 7/9 段同一 PR（U09-001：只叠第 9 段服务而不带本文件时 main M042 单测 `expected 10 but was 12` 红，Grok U09 B1 实跑 41 run/1 fail；反之只合本文件则策略数断言 12 对 main 的 10 红）。

## 5. 107 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 断言随第 7/9 段行为改动而改 |
| GEN-002 可执行正确性 | 通过 | 64/64 通过 |
| GEN-003 测试有效性 | 不通过 | 新增用例把「私人手机密钥不可替代工作手机密钥」锁成可失败断言；但第 9 段顶层闸 `DEPARTMENT_REVIEWS_PENDING`、`staff.shift.publish` 评估/审批链、终态 `publishEvent` 在本文件 0 用例——U09-004（2026-09-09 定稿，发布前须修）已把补用例指派给本文件（CX-U12-001） |
| GEN-004/005/006/007/009/010/011/012/014/015 | 不适用 | 测试代码 |
| GEN-008 接口一致性 | 通过 | 断言与第 7/9 段实现一致 |
| GEN-013 构建 | 通过 | overlay 编译与运行绿 |
| GEN-016 文档 | 通过 | 版本行 V0.01/V0.02；注释「含排班发布和普通扣除上限审批」 |
| GEN-017 分类 | 通过 | 无添加 |
| GEN-018 证据 | 通过 | surefire 报告 |
| 其余 M042/M066 映射行（89 项） | 通过／不适用 | 验签分级（enforce/grace）、类型篡改、时间戳、未登记设备、体篡改、自报类型不越过设备闸等既有用例全部保留并通过；审批链、转发、升级、可见范围既有用例保留并通过 |

## 6. 本次 Findings（临时 ID CX-U12-00N；正式 ID 由 B2 分配）

### CX-U12-001 · 技术性（U09-004 定稿的测试面）· 发布前须修：是

- 位置：`governance/review/SupervisorReviewServiceTests.java:55-63`（策略目录 10→12 与两动作码断言）；全文 `DEPARTMENT_REVIEWS_PENDING` 0 命中、`publishEvent`/`ApplicationEventPublisher` 0 命中、`staff.shift.publish` 仅在目录断言中出现；`InMemorySupervisorReviewRepository` 与服务构造未注入事件发布器（`eventPublisher` 为 null，第 9 段 `:39-40/:292` 静默不发）。
- HTML 原句：BSM L1443（排班上一级审批）、BSM L1458（上限三审并由顶层平台公司最终确认）。
- 判据：GEN-003、GEN-002；依据类型：代码事实 + 已定稿 U09-004/U09-005。
- 业务影响：41/41 绿不经过第 9 段任何新分支；U09-005 处置（构造函数必填注入发布器）落地后本测试的服务构造必改，若不同批则编译红或继续绕过事件断言。
- 无业务发明理由：只补可失败用例，不改审批口径。
- 工程缺陷：是。建议登记：是（引用 U09-004，不重开根因）。最小处置（给发布者）：与 U09 同 PR 补四条用例——缺 `hr_review` 最终审批人拒绝、master 先批 → `DEPARTMENT_REVIEWS_PENDING`、排班 `one_level_up` 链、注入 `ApplicationEventPublisher` 桩并断言终态发布；不在本轮修复。

两处既有改动本身（设备级密钥桩、策略数 12）为第 7/9 段行为的单元级锁定，实跑 64/64 通过，不另立项。

## 7. 观察（登记、不入队）

- OBS-1：第 11 段 `M010RegressionTests.penaltyCapNeedsThreeDepartmentReviewsThenMasterAndDoesNotRewriteClosedMonth` 在空库实跑中覆盖了处罚上限三审闸与生效链路（席甲 U11 §2 92/92），但属 DB 回归、且 U11-008 已记其只断言异常类型；不能替代 CX-U12-001 的单元用例。
- OBS-2：验签器测试 `setUp` 不再桩 `staffRepo.findActiveCredential`，仅在新增用例中桩以证明「不回落」；与第 7 段 `DeviceCredentialVerifier` 去掉员工凭证回落一致。
- OBS-3：`SupervisorReviewServiceTests` 内存仓储 `InMemorySupervisorReviewRepository` 未实现事件发布，`SupervisorReviewService.eventPublisher` 为 null → U09-005 同根，已并入 CX-U12-001 处置。
- 技能：`using-superpowers`、`verification-before-completion`（单测实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。2 文件全文三线审计、107 项判据逐项核对；1 项技术性须修（CX-U12-001：U09-004 已定稿指派本文件补可失败用例，本文件 0 覆盖第 9 段新闸/事件）、无添加项、无 HTML 修改申请；overlay 离线单测 23/23 + 41/41（绿不经过新分支）；3 项观察；未修源、未提交/推送。本报告落盘前未读取 `U12-b1-grok-raw.md` 任何正文。
---

# U12 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.66「当前 U12 B2-r1 整批候选」U12-001…006；modules/U12-b1-grok-raw.md V0.01（50166bytes SHA583d81b933b1e9d70fc6edfcc4ec7e11a4584e7e14222b98017c09d558cc3fb1）；本文件 B1 段（16586bytes SHAe2b995457e8ba00e406d42c4e447bd98ebfcdae0ea952609f54b9cfd849a94ac）。2026-09-10T01:19:00Z
- 席甲 B2 核实（只读，见 ledger V2.77 U12 节）：`DeviceCredentialVerifierTests.java:64` 桩 `signingKeyStore.findActivePublicKey(DEVICE)`、`:112-118` 新用例、`:94-100` 无设备号自报 web 通过；`isInstanceOfSatisfying`=13、`isInstanceOf(ApiException.class)`=1（`:117`）；main 验签器 `:249` `staffRepository.findActiveCredential` vs 未发布 `:251` 只读设备钥；两测试文件 `git diff 242f4872 HEAD` 空；`SupervisorReviewServiceTests` `DEPARTMENT_REVIEWS_PENDING`/`publishEvent` 0 命中、`staff.shift.publish` 仅 `:63`；U07-002/U07-003/U09-004 定稿处置一致。

## 勘误（席甲 B1 段）

- 席甲 B1 只立 CX-U12-001，漏判：只合本段叠 main 首轮红（§4 仅定性，B2 立 U12-001）；验签器改读设备级公钥的测试面记 OBS-2（升 U12-002）；`:116-117` 新用例无错误码断言（U12-004）；`:94-100` main 既有用例锁 U07-003 须修行为、同 PR 必改（U12-005，冲突）。席甲 B1 票 FAIL 不变，缺陷计数由 1 升为 5。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 两文件 main＝基线；Grok 实测仅叠测试 3 fail/2 error；处置只重放/同 PR/禁整树 |
| 002 | PASS | main `:249` 员工钥 vs 未发布 `:251` 设备钥事实成立；HTML 不规定钥表，技术性与 U07-002 同 |
| 003 | PASS | 新闸/事件 0 命中、41/41 不进新分支；四条用例即 U09-004 定稿处置原文 |
| 004 | PASS | 13 处带码 vs 1 处不带码属实；与 U11-008 同模式 |
| 005 | PASS | 用例锁 U07-003 须修行为；U07-003 为「是」且同 PR 落地后本用例必红，故不适用「main 既有→否」；分类随 U07-003 记冲突 |
| 006 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 6 条，定稿以此为准）

1. U12-002 冻结判据补本段映射内 BSM-044；分类仍为技术性。
2. U12-003 冻结判据补 BSM-028（排班发布下位）；四条用例不变。
3. U12-005 事实格读作「方法体与 main/基线逐字节相同；副本行号 `:94-100`，HEAD 为 `:93-99`（副本多两行版本注释）」。
4. U12-001 同 PR 集合以 U01/U02/U05/U06/U07/U09/U10/U11 并集为准。
5. U12-006 所列为抽样转述：Grok §8.3（`any` import 在、调用 0）、§8.12（15 参构造）已列；席甲 OBS-1 维持观察；定稿不得写成「Grok 无这两条」或升格入队。
6. Grok B1 GR-004「12 处 Satisfying」为笔误，以本轮复算 13 为准（候选已按 13 登记）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U12-001…006 成立；发布前须修「是」5 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 71259bytes SHA213c840fe1785111cfb1119e0212568b1ca9775ed568704cac65489e1584b34c，前 50166bytes 原字节不变）。
