CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B4-r1 修复 diff 审计 · 席甲报告
版本：V0.01

## 0. 输入与方法

- 席甲：Claude Opus 5.5（作者自审票，B3 由本席实施）。2026-09-24T08:08:05Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14，LOCK-AMEND-COUNT 1）。定稿：findings V4.08「XM-M034-AB B2-r1 双 PASS 定稿（按 D-033「删除」）」段及其所引候选段，修复队列「是」XM-M034-AB-001…004。
- diff：before tree d6e19dfb67d8583a3c1257c8f3f0f820f2743dd4 → after tree 46ea3be99b8fbe5ffd382ebbfaa0da0238a1ccb6；冻结 /home/james/audit-tmp/xm034_b3r1.diff（127358bytes SHA256 2c163c613e0e3924d79c7eafc2a6f5a3ac3767df74e582efc7c66b44db4dacc3），30 files changed, 71 insertions, 831 deletions；删除 4 个文件、新增 1 个迁移；无文件模式变更。落盘前未读 Grok 本次正文。

## 1. 可执行证据

RED（首轮，只落测试补丁；与第二轮测试补丁相同）与 GREEN（第二轮，含生产就绪证据同步）：

```
START=2026-09-24T08:02:05Z HEAD=e456ba96 origin/main=daf6adfc
scratch at daf6adfc
== RED（只落测试补丁）
MVN_EXIT(red)=1
  red: suites=263 tests=3529 red=1
  RED 相对原样基线（origin/main 0 红）新增红：
    NEW-RED: AmusementJdbcIntegrationTests.amusementOverridePermissionIsRetiredAndGrantedToNobody
== GREEN（全部补丁＋生成物）
  docs:operation-matrix=0
  docs:openapi-closure=0
  docs:data-isolation-spec=0
  docs:security-baseline=0
START=2026-09-24T08:04:54Z HEAD=e456ba96 origin/main=daf6adfc
scratch at daf6adfc
== GREEN（全部补丁＋生成物）
  docs:operation-matrix=0
  docs:openapi-closure=0
  docs:data-isolation-spec=0
  docs:security-baseline=0
  scratch diff:  30 files changed, 71 insertions(+), 831 deletions(-)
    M	backend/src/main/java/com/complex/platform/amusement/AmusementController.java
    D	backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java
    D	backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java
    M	backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java
    M	backend/src/main/java/com/complex/platform/amusement/AmusementService.java
    M	backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java
    M	backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRowMapper.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketingController.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketingService.java
    D	backend/src/main/java/com/complex/platform/ticketing/dto/VerificationManualOverrideRequest.java
    A	backend/src/main/resources/db/migration/V433__retire_amusement_verification_override.sql
    M	backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java
    M	backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java
    M	backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/AmusementExitReleaseJdbcIntegrationTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java
    M	docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
    M	docs/openapi/openapi-p0.json
    M	docs/openapi/openapi-p0.yaml
    M	docs/p0-api-contract-pack-v0.1.md
    M	docs/p0-openapi-contract-closure-appendix-v0.1.md
    M	docs/p0-openapi-operation-acceptance-matrix-v0.1.md
    M	docs/p0-operation-permission-matrix-v0.1.md
    M	docs/production-readiness-evidence/security-baseline-latest.json
    M	docs/requirements-code-implementation-matrix-v0.1.csv
    M	security-scans/findings-register-v1.md
    M	security-scans/formal-security-baseline-v1.md
    M	tests/e2e/support/mock-api.ts
MVN_EXIT(green)=0
  green: suites=263 tests=3529 red=0
  GREEN 全部红（预期 0）：
  verify:predeploy=0
PRODUCTION_BLOCKER_REGISTER_PASS
PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
  scratch dirty after cleanup: 0
  scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T08:05:50Z
```

首轮 GREEN 的 verify:predeploy 失败原文（促成生产就绪证据同步）：

```
START=2026-09-24T08:02:05Z HEAD=e456ba96 origin/main=daf6adfc
scratch at daf6adfc
== RED（只落测试补丁）
MVN_EXIT(red)=1
  red: suites=263 tests=3529 red=1
  RED 相对原样基线（origin/main 0 红）新增红：
    NEW-RED: AmusementJdbcIntegrationTests.amusementOverridePermissionIsRetiredAndGrantedToNobody
== GREEN（全部补丁＋生成物）
  docs:operation-matrix=0
  docs:openapi-closure=0
  docs:data-isolation-spec=0
  docs:security-baseline=0
  scratch diff:  29 files changed, 67 insertions(+), 827 deletions(-)
    M	backend/src/main/java/com/complex/platform/amusement/AmusementController.java
    D	backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java
    D	backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java
    M	backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java
    M	backend/src/main/java/com/complex/platform/amusement/AmusementService.java
    M	backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java
    M	backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRowMapper.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketingController.java
    M	backend/src/main/java/com/complex/platform/ticketing/TicketingService.java
    D	backend/src/main/java/com/complex/platform/ticketing/dto/VerificationManualOverrideRequest.java
    A	backend/src/main/resources/db/migration/V433__retire_amusement_verification_override.sql
    M	backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java
    M	backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java
    M	backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/AmusementExitReleaseJdbcIntegrationTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java
    M	backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java
    M	docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
    M	docs/openapi/openapi-p0.json
    M	docs/openapi/openapi-p0.yaml
    M	docs/p0-api-contract-pack-v0.1.md
    M	docs/p0-openapi-contract-closure-appendix-v0.1.md
    M	docs/p0-openapi-operation-acceptance-matrix-v0.1.md
    M	docs/p0-operation-permission-matrix-v0.1.md
    M	docs/requirements-code-implementation-matrix-v0.1.csv
    M	security-scans/findings-register-v1.md
    M	security-scans/formal-security-baseline-v1.md
    M	tests/e2e/support/mock-api.ts
MVN_EXIT(green)=0
  green: suites=263 tests=3529 red=0
  GREEN 全部红（预期 0）：
  verify:predeploy=1
Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.
PRODUCTION_READINESS_EVIDENCE_FAIL
- security evidence openapi_operation_count must be 730, got 732
  scratch dirty after cleanup: 0
  scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T08:03:58Z
```

落主工作区：

```
START=2026-09-24T08:05:58Z HEAD=e456ba96 origin/main=daf6adfc
== 主工作区落补丁（拷 GREEN 改动文件副本，不在主工作区跑生成器）
before tree=d6e19dfb67d8583a3c1257c8f3f0f820f2743dd4
after tree=46ea3be99b8fbe5ffd382ebbfaa0da0238a1ccb6
frozen diff: 127358 bytes sha256 2c163c613e0e3924d79c7eafc2a6f5a3ac3767df74e582efc7c66b44db4dacc3
 30 files changed, 71 insertions(+), 831 deletions(-)
主工作区 diff 与一次性 worktree 验证过的 diff：same
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T08:06:00Z
```

## 2. 逐块归因（69 块）

| 文件 | 块 | 块头 | ± | 归因 ID | 首条改动行（截断） |
|---|---:|---|---|---|---|
| AmusementController.java | 1 | `@@ -36,21 +36,6` | +0/−15 | XM-M034-AB-004 | @GetMapping("/manual-overrides") |
| AmusementManualOverrideRecord.java | 1 | `@@ -1,17 +0,0` | +0/−17 | XM-M034-AB-004 | package com.complex.platform.amusement; |
| AmusementManualOverrideRequest.java | 1 | `@@ -1,14 +0,0` | +0/−14 | XM-M034-AB-004 | package com.complex.platform.amusement; |
| AmusementRepository.java | 1 | `@@ -244,9 +244,6` | +0/−3 | XM-M034-AB-002 | /** 阶段2 主管释放（异常清场）：清零项目并发在场快照（point_device+project 维），返回涉及行数。 */ |
| AmusementRepository.java | 2 | `@@ -257,12 +254,6` | +0/−6 | XM-M034-AB-004 | AmusementManualOverrideRecord saveManualOverride(AmusementManualOverri |
| AmusementService.java | 1 | `@@ -33,7 +33,6` | +0/−1 | XM-M034-AB-004 | private static final Set<String> OVERRIDE_TYPES = Set.of("allow", "den |
| AmusementService.java | 2 | `@@ -106,33 +105,6` | +0/−27 | XM-M034-AB-004 | public PageResponse<AmusementManualOverrideRecord> listManualOverrides |
| AmusementService.java | 3 | `@@ -1578,44 +1550,6` | +0/−38 | XM-M034-AB-004 | private AmusementManualOverrideRecord createManualOverrideInternal(Amu |
| AmusementService.java | 4 | `@@ -1847,9 +1781,6` | +0/−3 | XM-M034-AB-004 | if (response instanceof AmusementManualOverrideRecord record) { |
| JdbcAmusementRepository.java | 1 | `@@ -1106,25 +1106,6` | +0/−19 | XM-M034-AB-002 | /** |
| JdbcAmusementRepository.java | 2 | `@@ -1367,48 +1348,6` | +0/−42 | XM-M034-AB-004 | @Override |
| JdbcAmusementRepository.java | 3 | `@@ -1993,19 +1932,6` | +0/−13 | XM-M034-AB-004 | private MapSqlParameterSource manualOverrideParameters(AmusementManual |
| JdbcAmusementRowMapper.java | 1 | `@@ -200,20 +200,6` | +0/−14 | XM-M034-AB-004 | AmusementManualOverrideRecord mapManualOverride(ResultSet rs, int rowN |
| TicketVerificationService.java | 1 | `@@ -204,49 +204,6` | +0/−43 | XM-M034-AB-001 | /** |
| TicketingController.java | 1 | `@@ -21,7 +21,6` | +0/−1 | XM-M034-AB-001 | import com.complex.platform.ticketing.dto.VerificationManualOverrideRe |
| TicketingController.java | 2 | `@@ -492,20 +491,6` | +0/−14 | XM-M034-AB-001 | // C1/REQ-05：人工放行（主管）。主管对核销做人工放行决策，留痕 status=manual_overridden。 |
| TicketingService.java | 1 | `@@ -931,12 +931,6` | +0/−6 | XM-M034-AB-001 | /** C1/REQ-05：人工放行（主管），留痕 status=manual_overridden。权限在端点层按主管权限码控制。 */ |
| VerificationManualOverrideRequest.java | 1 | `@@ -1,25 +0,0` | +0/−25 | XM-M034-AB-001 | package com.complex.platform.ticketing.dto; |
| V433__retire_amusement_verification_override.sql | 1 | `@@ -0,0 +1,16` | +16/−0 | XM-M034-AB-004 | -- V433：XM-M034-AB（2026-09-24）004，操作员 D-033「删除」。 |
| AmusementControllerTests.java | 1 | `@@ -61,8 +61,6` | +0/−2 | XM-M034-AB-004 | when(service.listManualOverrides()).thenReturn(new PageResponse<>(List |
| AmusementControllerTests.java | 2 | `@@ -113,8 +111,6` | +0/−2 | XM-M034-AB-004 | "GET,/api/v1/amusement/manual-overrides,amusement.verification.overrid |
| AmusementControllerTests.java | 3 | `@@ -192,7 +188,6` | +0/−1 | XM-M034-AB-004 | "/api/v1/amusement/manual-overrides", |
| AmusementControllerTests.java | 4 | `@@ -233,11 +228,6` | +0/−5 | XM-M034-AB-004 | if (path.contains("manual-overrides")) { |
| AmusementControllerTests.java | 5 | `@@ -388,10 +378,6` | +0/−4 | XM-M034-AB-004 | private AmusementManualOverrideRecord manualOverride() { |
| AmusementJdbcIntegrationTests.java | 1 | `@@ -77,7 +77,6` | +0/−1 | XM-M034-AB-004 | "amusement.verification.override", |
| AmusementJdbcIntegrationTests.java | 2 | `@@ -191,17 +190,6` | +0/−11 | XM-M034-AB-004 | repository.saveManualOverride(new AmusementManualOverrideRecord( |
| AmusementJdbcIntegrationTests.java | 3 | `@@ -288,9 +276,6` | +0/−3 | XM-M034-AB-004 | assertThat(repository.listManualOverrides()) |
| AmusementJdbcIntegrationTests.java | 4 | `@@ -487,6 +472,22` | +16/−0 | XM-M034-AB-004 | // XM-M034-AB-004（操作员 D-033「删除」）：游乐人工改判入口整体下线，其权限码停用且不留任何授予来源；历史表保留。 |
| AmusementServiceTests.java | 1 | `@@ -100,26 +100,6` | +0/−20 | XM-M034-AB-003/004 | @Test |
| AmusementServiceTests.java | 2 | `@@ -1614,42 +1594,6` | +0/−36 | XM-M034-AB-003/004 | @Test |
| AmusementServiceTests.java | 3 | `@@ -2308,7 +2252,6` | +0/−1 | XM-M034-AB-004 | private final Map<String, AmusementManualOverrideRecord> manualOverrid |
| AmusementServiceTests.java | 4 | `@@ -3091,27 +3034,11` | +0/−16 | XM-M034-AB-002/004 | @Override |
| AmusementServiceTests.java | 5 | `@@ -3239,13 +3166,6` | +0/−7 | XM-M034-AB-004 | @Override |
| AmusementExitReleaseJdbcIntegrationTests.java | 1 | `@@ -4,7 +4,6` | +0/−1 | XM-M034-AB-002 | import com.complex.platform.amusement.AmusementManualOverrideRequest; |
| AmusementExitReleaseJdbcIntegrationTests.java | 2 | `@@ -25,21 +24,21` | +5/−5 | XM-M034-AB-002 | * 阶段2（2026-07-05 用户拍板）游乐核销：出场核销（并发容量回收）+ 主管释放 + 并发容量硬化（唯一约束防超扣） |
| AmusementExitReleaseJdbcIntegrationTests.java | 3 | `@@ -73,7 +72,6` | +0/−1 | XM-M034-AB-004 | jdbcTemplate.update("DELETE FROM amusement_manual_overrides WHERE proj |
| AmusementExitReleaseJdbcIntegrationTests.java | 4 | `@@ -143,26 +141,6` | +0/−20 | XM-M034-AB-002 | @Test |
| AmusementExitReleaseJdbcIntegrationTests.java | 5 | `@@ -212,15 +190,15` | +4/−4 | XM-M034-AB-002 | * 场次/批次型（木马每轮、密室每批）席位不因出场或主管释放回收——满了等下一场。 |
| AmusementExitReleaseJdbcIntegrationTests.java | 6 | `@@ -239,13 +217,6` | +0/−7 | XM-M034-AB-002 | // 主管释放（异常清场）：同样只清并发在场快照，场次席位仍不受影响。 |
| TicketingControllerTests.java | 1 | `@@ -234,99 +234,6` | +0/−93 | XM-M034-AB-001 | // item7 C1/REQ-05: manual release endpoint uses supervisor permission |
| TicketingServiceTests.java | 1 | `@@ -485,19 +485,6` | +0/−13 | XM-M034-AB-001 | // C1/REQ-05: 人工放行留痕 manual_overridden |
| data-isolation-and-tenant-scope-security-spec-v0.1.md | 1 | `@@ -56,7 +56,7` | +1/−1 | XM-M034-AB-004 | > 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V432**，发现  |
| openapi-p0.json | 1 | `@@ -847,73 +847,6` | +0/−67 | XM-M034-AB-004 | "/api/v1/amusement/manual-overrides": { |
| openapi-p0.json | 2 | `@@ -38077,16 +38010,6` | +0/−10 | XM-M034-AB-004 | "AmusementManualOverrideRequest": { |
| openapi-p0.json | 3 | `@@ -43421,63 +43344,6` | +0/−57 | XM-M034-AB-004 | "AmusementManualOverrideRequest": { |
| openapi-p0.yaml | 1 | `@@ -571,58 +571,6` | +0/−52 | XM-M034-AB-004 | /api/v1/amusement/manual-overrides: |
| openapi-p0.yaml | 2 | `@@ -27208,12 +27156,6` | +0/−6 | XM-M034-AB-004 | AmusementManualOverrideRequest: |
| openapi-p0.yaml | 3 | `@@ -31119,51 +31061,6` | +0/−45 | XM-M034-AB-004 | AmusementManualOverrideRequest: |
| p0-api-contract-pack-v0.1.md | 1 | `@@ -331,7 +331,7` | +1/−1 | XM-M034-AB-004 | ¦ `POST` ¦ `/api/v1/amusement/manual-overrides` ¦ 人工放行（**已废止·见文首口径**，待 |
| p0-openapi-contract-closure-appendix-v0.1.md | 1 | `@@ -14,11 +14,11` | +5/−5 | XM-M034-AB-004 | ¦ OpenAPI operation 数 ¦ 732 ¦ 全部必须进入 Java Controller、DTO、Service、权限、事件 |
| p0-openapi-contract-closure-appendix-v0.1.md | 2 | `@@ -81,7 +81,6` | +0/−1 | XM-M034-AB-004 | ¦ `amusement.verification.override` ¦ staff-token ¦ project-amusement, |
| p0-openapi-contract-closure-appendix-v0.1.md | 3 | `@@ -443,8 +442,6` | +0/−2 | XM-M034-AB-004 | ¦ `amusement.manual_override.view_failed` ¦ failure ¦ `listAmusementMa |
| p0-openapi-contract-closure-appendix-v0.1.md | 4 | `@@ -513,8 +510,6` | +0/−2 | XM-M034-AB-004 | ¦ `amusement.verification_overridden` ¦ success ¦ `createAmusementManu |
| p0-openapi-contract-closure-appendix-v0.1.md | 5 | `@@ -1851,7 +1846,6` | +0/−1 | XM-M034-AB-004 | ¦ AMU-游乐安全 ¦ `createAmusementManualOverride` ¦ `POST` ¦ `/api/v1/amuse |
| p0-openapi-contract-closure-appendix-v0.1.md | 6 | `@@ -1876,7 +1870,6` | +0/−1 | XM-M034-AB-004 | ¦ AMU-游乐安全 ¦ `listAmusementManualOverrides` ¦ `GET` ¦ `/api/v1/amuseme |
| p0-openapi-operation-acceptance-matrix-v0.1.md | 1 | `@@ -17,7 +17,7` | +1/−1 | XM-M034-AB-004 | > 当前 `docs/openapi/openapi-p0.json` 共 732 个 operation。下表由 OpenAPI 生成；若 |
| p0-openapi-operation-acceptance-matrix-v0.1.md | 2 | `@@ -40,8 +40,6` | +0/−2 | XM-M034-AB-004 | ¦ amusement ¦ `GET` ¦ `/api/v1/amusement/manual-overrides` ¦ `listAmus |
| p0-operation-permission-matrix-v0.1.md | 1 | `@@ -60,7 +60,7` | +1/−1 | XM-M034-AB-001 | ¦ 人工放行（**已废止·见文首口径**） ¦ `verification.manual_override`（停用，待代码轮下线） ¦ —  |
| p0-operation-permission-matrix-v0.1.md | 2 | `@@ -75,7 +75,7` | +1/−1 | XM-M034-AB-004 | ¦ 游乐核销（原含「/人工放行」，已废止·见文首口径） ¦ `amusement.verification.execute`（`amusem |
| p0-operation-permission-matrix-v0.1.md | 3 | `@@ -248,7 +248,7` | +1/−1 | XM-M034-AB-001 | ¦ 核销撤销（原含「/ 人工放行」，已废止·见文首口径） ¦ `verification.reverse`（`verification.ma |
| p0-operation-permission-matrix-v0.1.md | 4 | `@@ -266,7 +266,7` | +1/−1 | XM-M034-AB-004 | ¦ 核销执行（原含「/ 人工放行」，已废止·见文首口径） ¦ `amusement.verification.execute`（`amuse |
| security-baseline-latest.json | 1 | `@@ -1,14 +1,14` | +4/−4 | XM-M034-AB-004 | "verified_at": "2026-09-10T05:00:53Z", |
| requirements-code-implementation-matrix-v0.1.csv | 1 | `@@ -46,10 +46,10` | +4/−4 | XM-M034-AB-004 | REQ-45,游乐,游乐可独立成乐园 SaaS,apps/web/src/main.tsx; tests/e2e/full-admin-mv |
| findings-register-v1.md | 1 | `@@ -3,8 +3,8` | +2/−2 | XM-M034-AB-004 | - OpenAPI operation count: 732 |
| formal-security-baseline-v1.md | 1 | `@@ -3,8 +3,8` | +2/−2 | XM-M034-AB-004 | - OpenAPI operation count: 732 |
| formal-security-baseline-v1.md | 2 | `@@ -42,7 +42,7` | +1/−1 | XM-M034-AB-004 | ¦ `staff-token` ¦ 677 ¦ |
| formal-security-baseline-v1.md | 3 | `@@ -57,8 +57,8` | +2/−2 | XM-M034-AB-004 | ¦ 全部 operation ¦ 732 ¦ |
| formal-security-baseline-v1.md | 4 | `@@ -73,9 +73,9` | +3/−3 | XM-M034-AB-004 | ¦ `staff_work_phone` ¦ 81 ¦ |
| mock-api.ts | 1 | `@@ -259,7 +259,6` | +0/−1 | XM-M034-AB-004 | "/api/v1/amusement/manual-overrides": empty, |

共 69 块，+71/−831，逐块归因齐全。

归因不到定稿 ID 的行：0。「003/004」「002/004」为同一块同时承担两项的删除（以 allow 为样例的测试、内存仓里清零方法与改判方法相邻）。

## 3. 内容审计

- 001：只删 TicketingController 的 import 与映射（含其前三行注释）、TicketingService.manualOverrideVerification（含 javadoc 与 @GuardedAction）、TicketVerificationService.manualOverride（含 javadoc）、dto/VerificationManualOverrideRequest 文件、TicketingControllerTests 四条与 TicketingServiceTests 一条人工放行用例；V067、V270、V429 未动；AccessJdbcIntegrationTests 的 V429 回归未动；权限矩阵第 63、251 行只把「停用，待代码轮下线」「停用」改为「停用，写入口已下线」。
- 002：删 clearProjectConcurrentCapacity 的接口声明（含单行 javadoc）、Jdbc 实现（含 javadoc 与 @Override）、测试内存仓实现；releaseEntryConcurrentCapacity 四处未动；出场测试删 supervisorReleaseClearsConcurrentCapacityWithAudit、sessionBatchCapacity 用例去末段主管释放并改名为 sessionBatchCapacityUnaffectedByExit、类注释与方法注释去掉主管释放并按删除后的用例编号改证伪句、删 import 与清理改判表的 DELETE。
- 003/004：删 AmusementController 两个映射、AmusementService 的 OVERRIDE_TYPES、listManualOverrides、createManualOverride（含 @GuardedAction/@Transactional）、createManualOverrideInternal、返回值 id 的 instanceof 分支；仓储三个改判方法与 manualOverrideParameters；行映射 mapManualOverride；两个记录类型文件。各删除块前后相邻成员完整（逐块核过边界）。
- V433：与 V429 同形，码换为 amusement.verification.override；不 DELETE FROM permissions，不改 V014/V234/V251/V254/V099。
- 测试：AmusementJdbcIntegrationTests 平台管理员清单去掉该码、全量权限清单保留、删 saveManualOverride 与 listManualOverrides 两段、补停用回归（status=retired、四处授予为 0、表仍在），RED 下该回归红（旧库该码 active 且授平台管理员）；AmusementControllerTests 删两处桩、三行 CsvSource（含第 195 行）、bodyFor 分支与样例；AmusementServiceTests 删两条改判用例与内存仓 Map 及四个实现。
- 契约：两份 OpenAPI 删同一路径（POST/GET）与 requestBodies、schemas 的 AmusementManualOverrideRequest，verify:openapi 深相等通过；附录两份由生成器重建，只去掉这两个操作、四个事件行与权限行，计数 732→730。
- 生成物与证据：数据隔离规格只改最新迁移戳 V432→V433；安全基线与发现登记只改 OpenAPI 计数与由此变化的分项计数；生产就绪证据 JSON 改接口数、verified_at、notes、version（无生成器，定稿未点名，属「门禁要求的产物一并带入」，已请 Grok 判定）。
- 文档：权限矩阵第 78 行括号改为「停用，游乐改判接口已下线」并从事件列去掉已不再发出的 amusement.verification_overridden，第 269 行括号同改；契约包第 334 行「待代码轮下线，不得新增调用」改为「接口已下线」；需求实现矩阵第 49–52 行各去掉 GET 与 POST 两处接口引用共 8 处，同行其余接口与表名保留；e2e mock-api.ts 删一行模拟。
- GREEN 全量 3529 测 0 红，全链 verify:predeploy 退出码 0。
- 借机删弱、新业务、新依赖：无。删去的测试断言全是守护已废止能力的；新增一条回归守停用。

## 4. 结论

- 归因票：**PASS**（69 块全部归因到 XM-M034-AB-001…004，越界 0；生产就绪证据 JSON 归 004，待 Grok 判定）。
- 内容票：**PASS**。
- 席甲 B4-r1 票：**PASS**。
