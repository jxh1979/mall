CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-11T08:37:09Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `a041458d06051fed3811d562764cd5900bba6f72`；工作区 tree `66381f8746bcb7f11d862c9d3630d053e0b836fb`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M015「空间点位、楼层平面图与营业时段／后台实现／第1段」，23 文件（计划冻结 1999 行；当前含 B3-r1/r2 修复），映射 90 判据（BEQR 22、BSM 14、GEN 18、PGM 36）。步骤：B5-r1（B3 r1+r2 两次整批修复后的整模块全量三线重审；B3 累计 2/3）。

- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M015-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：PR #435（M014 闭环）已合并，HEAD=origin/main=a041458d；本模块 B3-r1/r2 修复在工作区未提交（after tree e7d8355fc16574f780cf51b134f06c72550cb666）。
- 只读依赖追流（不审）：V005/V023/V034/V226/V201 迁移、`IdempotencyService`（TransactionTemplate）、`AuthorizationService`、`AuditEventService`、`PlatformService`、`contract/JdbcContractRepository`、前端 `FloorPlanImportPage.tsx`、三份 HTML 现文；B5 实跑 5 个测试类。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java | 95 | 4323 | 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java | 21 | 503 | 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 51 | 2148 | 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 677 | 31050 | d912fb381895537f930728f17f6e06df45f7ccd715331572ccd8debbe9834005 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 454 | 21976 | bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java | 23 | 596 | 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java | 14 | 458 | 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java | 14 | 295 | 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java | 35 | 912 | 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 303 | 12350 | 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java | 19 | 440 | 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java | 7 | 112 | cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java | 22 | 546 | 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java | 16 | 383 | 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java | 10 | 240 | 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java | 13 | 348 | ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java | 18 | 438 | b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d | 全读 |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 17 | 342 | be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceRecord.java | 22 | 515 | f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java | 15 | 386 | 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java | 29 | 1244 | ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 119 | 6399 | f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 115 | 5939 | 866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12 | 全读 |

## 2. 可执行证据原文（/tmp/m015_b5r1_run.log）

```
START=2026-09-11T08:37:04Z HEAD=a041458d origin/main=a041458d
== 0. 23 files + 3 test classes after B3 (lines/bytes/sha)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java 95 4323 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java 21 503 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java 51 2148 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 677 31050 d912fb381895537f930728f17f6e06df45f7ccd715331572ccd8debbe9834005
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java 454 21976 bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java 23 596 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java 14 458 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java 14 295 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java 35 912 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java 303 12350 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java 19 440 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270
backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java 7 112 cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java 22 546 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java 16 383 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java 10 240 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java 13 348 ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java 18 438 b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java 17 342 be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5
backend/src/main/java/com/complex/platform/governance/SpaceRecord.java 22 515 f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1
backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java 15 386 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java 29 1244 ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java 119 6399 f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java 115 5939 866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 688 30980 4a0f962e9ec981baf49395b86cd82c8c503460058c867c8cafea39116e3f34a0
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 210 7741 b15c73338d23e07628f9899f3c5c27b13830d73c9e922622069e459db6b64601
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 175 7649 bf1a9d3232f5318a2ab15313533a54c255efe77831589af9ca256ff8c477cc4b
== 1. version headers
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:1
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:1
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:1
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:1
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:1
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:1
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:1
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:2:// 文件版本：V0.02
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.02
== 2. 001 derived keys (floor-plan domain, SHA-256 prefix, fit VARCHAR(64))
268:                requestedShapeId == null ? derivedId("fps_", floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
296:        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
316:                    "fpic_" + slug(taskId + "_" + candidateNo),
335:                    "fpie_" + slug(taskId + "_" + errorNo),
393:                    derivedId("fps_", importTaskId + "_" + candidate.id()),
652:    private String derivedId(String prefix, String seed) {
== 3. 002/003 project_id + scope
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:72:        Optional<String> merchantScope = authorizationService.currentMerchantIdQui
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:73:        // M015-003：商户 scope 优先（用永不抛的 currentMerchantIdQuiet
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:76:                : authorizationService.currentProjectScopeQuietly()
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:77:                        .map(repository::findAllFloorPlansByProject)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:230:        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:508:        if (!repository.projectExists(projectId)) {
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:509:            throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不�
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:511:        if (authorizationService.currentMerchantIdQuietly().isEmpty()) {
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:512:            authorizationService.currentProjectScopeQuietly().ifPresent(scope -> 
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:514:                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只�
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:596:        checkProjectOwnership(fp.projectId());
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:601:    private void checkProjectOwnership(String resourceProjectId) {
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:602:        if (authorizationService.currentMerchantIdQuietly().isPresent()) {
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:605:        Optional<String> scope = authorizationService.currentProjectScopeQuietly(
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:607:            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "平面图属
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:612:        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:17:    default List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) 
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:24:    boolean projectExists(String projectId);
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:53:    public List<FloorPlanRecord> findAllFloorPlansByProject(String projectI
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:65:    public boolean projectExists(String projectId) {
== 4. 004/005/006 English operator text left in module (expect none)
(remaining requireText keys floor_no/image_url/version_no/idempotency_key are translated by FieldLabels at the boundary)
235:                requireText(request.floorNo(), "floor_no"),
237:                requireText(request.imageUrl(), "image_url"),
240:                requireText(request.versionNo(), "version_no"),
505:        requireText(request.idempotencyKey(), "idempotency_key");
518:        requireText(request.floorNo(), "floor_no");
520:        requireText(request.imageUrl(), "image_url");
521:        requireText(request.versionNo(), "version_no");
531:        requireText(request.idempotencyKey(), "idempotency_key");
== 5. 007 honest paging
88:        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
104:        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
150:        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
== 6. 008 publish cap
46:    private static final int MAX_PUBLISH_CANDIDATES = 200;
587:        if (request.candidateIds().size() > MAX_PUBLISH_CANDIDATES) {
588:            throw ApiException.badRequest("VALIDATION_FAILED", "一次发布的候选项不能超过 " + MAX_PUBLISH_CANDIDATES + " 个"
21325:                  maxItems: 200
28452:              maxItems: 200
== 7. 009 venue existence + scope
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:33:    private final AuthorizationService authorizationService;
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:36:                              AuthorizationService authorizationService)
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:39:        this.authorizationService = authorizationService;
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:44:        Optional<String> projectScope = authorizationService.currentProj
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:46:                ? repository.listByProject(projectScope.get())
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:63:        String venueProjectId = repository.findVenueProjectId(venueId)
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:64:                .orElseThrow(() -> ApiException.notFound("VENUE_NOT_FOUN
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:65:        authorizationService.currentProjectScopeQuietly().ifPresent(scop
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:67:                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", 
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:51:    Optional<String> findVenueProjectId(String venueId) {
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:57:    List<VenueBusinessHoursRecord> listByProject(String projectId) {
== 8. 010–013 untouched surfaces (XM/否)
25: * 多一道发布只会让「改了没生效」变成新的事故来源。库内 is_draft 列在此复用为「停�
80:        boolean disabled = Boolean.TRUE.equals(request.disabled());
84:                venueId, dayOfWeek, openTime, closeTime, disabled, request.effectiveFrom(), now);
92:                        "disabled", disabled));
0
34:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route");
changed outside module surfaces: 0
== 9. tests (main worktree, complex_test, three DB vars only)
MVN_EXIT=0
floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.716 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.143 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.895 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
floorplan.FloorPlanServiceTests.txt:Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
governance.VenueBusinessHoursServiceTests.txt:Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== 10. HTML + lock untouched
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
 M backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
?? docs/audit-2026-09/modules/M015-b1-grok-raw.md
?? docs/audit-2026-09/modules/M015-b1-self-raw.md
?? docs/audit-2026-09/modules/M015-b4-r1-grok-raw.md
?? docs/audit-2026-09/modules/M015-b4-r1-self-raw.md
?? docs/audit-2026-09/modules/M015-b4-r2-grok-raw.md
?? docs/audit-2026-09/modules/M015-b4-r2-self-raw.md
?? docs/audit-2026-09/modules/M015-b5-r1-grok-raw.md
END=2026-09-11T08:37:09Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文（PGM-017/022/023 项目归属与空间点位、PGM-001 中文、BSM-001 涉钱配置版本化、BSM-003 可溯源、BEQR-152 项目隔离）反推本包 23 文件应当实现什么，逐一对照 B3-r1/r2 修复后的当前源码与测试（不依赖 B1–B4 结论）。
- 顺从式线：90 冻结判据逐族（§5；无实现面的判据明确原因）。
- 对抗式线：假定两位操作员在不同平面图各自从「shape-1」编号（001）、项目员工以本项目 token 列表/读取/创建他项目平面图（003）、发布 201 个候选（008）、给不存在或他项目的场地配营业时间（009）、导入 600 个多边形后列表（007）、用 curl 绕过前端弹窗直接发布（011，定稿否/XM，本批不升缺陷）、用 17 字符现网键与 UUID 键导入/发布/保存图形后落库（001-r2）、以无 merchantId 的项目员工 token 列表/读取/创建（003-r2）；B2 定稿禁止项（改迁移/OpenAPI/三份 HTML/权限种子/FieldLabels/PlatformController/前端/ticketing、新增字段或接口或业务规则、把 010–013 当本批可修）是否被越界引入。
- 依赖只读追流（不审）：V005（projects/spaces）、V023/V034（mall_floor_plans、shape_type CHECK）、V226（venue_business_hours）、V184（space_type_dictionary）、`IdempotencyService.runAndComplete`（TransactionTemplate 包裹）、`AuthorizationService.currentMerchantScope/currentProjectScopeQuietly/requireProjectScope`、`AuditEventService`、`PageResponse`、`FieldLabels.translate`（`GlobalExceptionHandler`）、`PlatformController`（营业时段端点，未改）、前端 `FloorPlanImportPage.tsx` / `VenueBusinessHoursPage.tsx`（未改）、三份 HTML 现文（§2 第 10 项）。
- 23 文件 + 3 个测试类全文逐行读完（修复后行数/字节/SHA 见 §1 与 §2 第 0 项）；B3-r1 diff（B4-r1：范围归因双 PASS、内容 Grok FAIL 001/003）与 B3-r2 diff（B4-r2 双 PASS）逐字对照；5 个测试类 55 例在主工作区实跑（§2 第 9 项）。

## 4. 已定稿 14 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修，r2） | 图形/任务/发布图形主键 `derivedId(prefix, seed)`＝前缀 + SHA-256 前 32 位（恒 36 字符；seed 含平面图 ID，或任务 ID+候选 ID）；候选/错误 ID 随任务 ≤46 | §2 第 2 项三处；跨图同键两例 + `derivedIdsFitVarchar64Columns`（现网键与 UUID 键）绿；发布追加语义未动；迁移未改 |
| 002 | 否（已修） | project_id 必填、`projectExists` 不存在 → PROJECT_NOT_FOUND | §2 第 3 项；Jdbc `SELECT COUNT(1) FROM projects WHERE id = :id`；列级 NOT NULL 仍归 M016（定稿） |
| 003 | 否（已修，r2） | 五处用永不抛的 `currentMerchantIdQuietly()`：商户 scope 优先；否则项目 scope → `findAllFloorPlansByProject`；`checkProjectOwnership`；创建时 scope 相等 | §2 第 3 项；三例（桩为生产语义：项目员工 `currentMerchantScope` 抛 MERCHANT_SCOPE_REQUIRED）绿；商户员工不触发项目 scope；平台管理员桥仍全量 |
| 004 | 否（已修） | Parser 13 处中文 error_message，error_code/field 不变 | §2 第 4 项 grep 无英文残句；FloorPlanImportParserTests 4/4 |
| 005 | 否（已修） | 平面图名称/候选项列表/取值无效 中文整句；FieldLabels 未改 | §2 第 4 项；`FIELD_LABELS` 四键本模块表 |
| 006 | 否（已修） | `:517`/`:535` 与 VenueBusinessHoursService 三处纯中文 | §2 第 4 项 |
| 007 | 否（已修，r2） | 三处 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())` | §2 第 5 项；`listShapesReportsHonestPageSizeAndTotal` 绿；无硬编码 500、无新参数 |
| 008 | 否（已修） | `MAX_PUBLISH_CANDIDATES=200` 超限中文 VALIDATION_FAILED | §2 第 6 项；与 OpenAPI maxItems 200 一致（OpenAPI 未改） |
| 009 | 否（已修） | `findVenueProjectId` → VENUE_NOT_FOUND；项目 scope → CROSS_PROJECT_FORBIDDEN；列表 `listByProject` JOIN spaces | §2 第 7 项；三例绿；`PlatformController` 未改 |
| 010 | 否（XM 同批） | `VenueBusinessHoursService` 仍「保存即生效」、`is_draft` 仍作停用 | §2 第 8 项；与定稿一致（本模块草案化 + M247 V226 版本化 + M009 转正端点 + M018 前端 + ticketing effective_from 同批） |
| 011 | 否（XM 同批） | 发布 DTO 无确认字段；前端 HighRiskConfirm 弹窗为现网唯一二次确认 | §2 第 8 项 `confirm` 0；与定稿一致（M392 OpenAPI + M018 前端同批） |
| 012 | 否（全局遗漏） | spaces 无用途/负责人列；本模块无迁移 | §2 第 8 项迁移目录无改动；与 M009-013/018 同口径 |
| 013 | 否（XM→M016/M018） | `SHAPE_TYPES` 六值未动；CHECK 未动 | §2 第 8 项；与定稿一致 |
| 014 | 否（登记） | 观察项未动（含 VenueBusinessHours 靠 UNIQUE last-write-wins、判据 90 勘误） | 与定稿一致 |

## 5. 90 项判据逐族核对（修复后）

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143…148/149/151/157/159/166/189、BSM-028/029/072/074/114/115/120、PGM-002…004/006…016/018…021/026/027/031/035/040…044/048/060/065/066 | 无实现面或通过（登记） | 本包只承载空间/平面图/营业时段后台，不涉主体、合同、员工、权限规则本体；`projects.capacity` 与 PGM-017 同向 |
| BEQR-152 项目隔离 | 通过 | 003/009：项目员工列表/读取/创建/营业时段保存均按 `currentProjectScopeQuietly` 隔离（平面图侧经 `currentMerchantIdQuietly` 到达，不再被会抛的商户闸截断）；商户 scope 优先；平台管理员全量（§2 第 3/7 项） |
| BEQR-150/153/154/155/156/158 | 无实现面 | 终端、项目/商户/合同/设备状态闭集不在本包 |
| BEQR-063/074 | 无实现面 | 高危动作本体/隐私桌台不在本包（发布权限码 requires_approval 未改） |
| BEQR-170 铺位租赁状态 | 无实现面（登记） | 派生在 contract 仓 |
| BSM-001 涉钱配置版本化 | 登记（010 否/XM） | 营业时段草案/转正跨 M247/M009/M018/ticketing，本模块本批按定稿不单独改行为；不是新缺陷 |
| BSM-003 可溯源 / GEN-006 数据完整性 / GEN-007 并发幂等 | 通过 | 001：跨图同键不再覆盖；幂等操作键与主键同按平面图分域；派生主键恒 36 字符落入 VARCHAR(64)；`IdempotencyService` 事务包裹 |
| BSM-004/005/006/007/009 | 通过（登记） | 平面图三态含 archived；无物理删除；营业时段停用不删行 |
| PGM-001 / GEN-015 中文 | 通过 | 004/005/006：模块内无面向操作员英文残句（§2 第 4 项 grep 空）；其余 `requireText` 键经 `FieldLabels.translate` 出网为中文 |
| PGM-005/017/022 项目归属 | 通过 | 002：project_id 必填且须存在；003 项目 scope；列级 NOT NULL 登记 M016 |
| PGM-023 空间点位/发布 | 通过（登记 012/013） | 候选区域类型摊位/公共区（013）与用途/负责人（012）为定稿跨模块/全局遗漏；发布追加不覆盖旧行未动；发布二次确认 011 定稿否/XM |
| GEN-001 必要性 | 通过 | 平面图导入/发布、空间记录、场次营业时段均有 HTML 上位 |
| GEN-002 可执行正确性 | 通过 | 001/003/009 修复后按 §3 对抗场景实跑无静默覆盖、无越项目读写 |
| GEN-003 测试有效性 | 通过 | 5 类 55 例实跑全绿（§2 第 9 项）；新增 12 例均先红后绿（B3-r1 11 例；B3-r2 1 例 + 3 例改桩为生产语义再红） |
| GEN-004 信任边界 | 通过 | 坐标范围、payload/形状/顶点上限、格式白名单、发布候选 ≤200、project_id/venue_id 存在性 |
| GEN-005 授权 | 通过 | 逐端点权限码；高危码 requires_approval；商户 scope + 项目 scope |
| GEN-008 接口一致性 | 通过（登记） | 白名单与 CHECK 一致；`PageResponse` pageSize/total 与 items 一致；`SpaceRecord.polygonPoints` 为 `List<Map>`（观察） |
| GEN-009 错误处理 | 通过（登记） | 失败路径经 IdempotencyService 事务回滚并 `recordFailure`；非白名单类型整单拒绝（并入 013） |
| GEN-010 安全 | 通过 | 无秘密；SVG 正则解析且受长度上限 |
| GEN-011 审计 | 通过 | 每个读/写路径 `recordSuccess/recordFailure`；营业时段列表审计 count 为 scope 内条数 |
| GEN-012 资源 | 通过 | 上限见 GEN-004；列表不分页但 total/pageSize 诚实（007） |
| GEN-013 构建 | 通过 | mvn 离线编译运行（§2 第 9 项） |
| GEN-014 迁移 | 无实现面 | 本模块无 SQL；迁移目录无改动（§2 第 8 项） |
| GEN-016 文档 | 通过（登记） | 改动文件版本头 V0.01（r2 再改的两文件 V0.02）；`VenueBusinessHoursService` 类注释「保存即生效」与 V226 注释不一致随 010 XM 同批改（观察） |
| GEN-017 分类 | 通过 | 无添加项；未把业务发明写成技术修复（B4 归因全部命中） |
| GEN-018 证据 | 通过 | §2 命令与测试原文 |

## 6. 本次 Findings

无新增 finding。（B3-r1/r2 修复后 23 文件三线全量重审、5 类 55 例实跑，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队；与 014 同）

- `VenueBusinessHoursService` 类注释「保存即生效、不做草稿/发布」与 V226 注释的 `is_draft/effective_from` 语义分歧属 010（否/XM），本批不改。
- `SpaceRecord.polygonPoints` 为 `List<Map<String,Object>>`（非 `FloorPlanPoint`），只读记录类，登记。
- 无认证上下文下 `AuthorizationService` 各 scope 方法抛 UNAUTHENTICATED，测试以匿名子类按生产语义覆盖（平台管理员桥三者皆空；项目员工 `currentMerchantScope` 抛、`currentMerchantIdQuietly` 空；商户员工 `currentProjectScopeQuietly` 抛），不触碰 SecurityContext。
- Grok B4-r1 观察（不入队）：008 上限分支无专门用例；004 残留 svg/standard_json/polyline/polygon/shapes/x/y 为格式或标签名。
- 技能：`verification-before-completion`（§2 命令实跑）、`receiving-code-review`（B4-r1 核源后撤票；未读席乙 B5 报告）、`test-driven-development`（B3 两轮先红后绿）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。23 文件全文三线重审、90 判据逐族核对、14 项定稿逐项核对、5 类 55 例实跑全绿；无新增缺陷、无添加项、无 HTML 问题；未提交/推送。本报告落盘前未读取 `M015-b5-r1-grok-raw.md` 任何正文。


---
# M015 席甲独立补充复查完整报告
版本：V0.02（2026-09-19T22:55:41Z；上方 V0.01 为 2026-09-11 B5-r1 原文原样保留，31876 bytes，SHA256 2184901a923e29ad41acac364b228fca5e9c3318b027cce3e48c2b774824a666）

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票）。操作员 2026-09-19 指令「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」续作；M013 已重开收口并合并（PR #453），M014 重开的 B5-r2 Grok 票此刻仍在跑（与本次无共享文件，独立并行）。本次为 M015 单席补充复查：无问题只记录、不是新双 PASS；发现问题则重开 B1 完整双席。
- 正本 `/home/james/complex-management` main 工作区，HEAD `e456ba96`，origin/main `246385d9`；工作区含未提交的 M009 阶段性修改（本模块内仅 `ProjectRecord.java` 已暂存）。
- 范围：`plan/modules.md` V0.04 的 23 文件（当前 2118 行）；判据 90 项（BEQR 22、BSM 14、GEN 18、PGM 36），与 B5-r1 同一冻结集合；锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。
- 只读承诺：未改源码/测试/三份 HTML/锁/findings；数据库探针全部 SELECT；测试在主工作区、`complex_test`（Flyway 427）、只导出 `COMPLEX_DATABASE_URL/_USERNAME/_PASSWORD`（`SPRING_PROFILES_ACTIVE` 未导出，§5 log「== 6.」计数 0）。
- Grok：NOT-CALLED（无命令/退出码/新票）。旧 Grok B5-r1 V0.01 raw SHA256 `73f8f552a491c2dd1de844adb054bdfebd85a2001c7fb3b00cefe3469a7321ac`、其锁行同值，保持不动、不冒称本次调用。

## 1. 文件覆盖表（当前；行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java | 95 | 4323 | 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java | 21 | 503 | 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 51 | 2148 | 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 678 | 31232 | 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 454 | 21976 | bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java | 23 | 596 | 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java | 14 | 458 | 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java | 14 | 295 | 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java | 35 | 912 | 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 303 | 12350 | 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java | 19 | 440 | 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java | 7 | 112 | cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java | 22 | 546 | 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java | 16 | 383 | 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java | 10 | 240 | 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java | 13 | 348 | ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java | 18 | 438 | b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d | 全读 |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 25 | 770 | 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceRecord.java | 22 | 515 | f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java | 15 | 386 | 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java | 29 | 1244 | ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 119 | 6399 | f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 115 | 5939 | 866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12 | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 472 | 21466 | acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 193 | 7611 | a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 277 | 11748 | 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59 | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 864 | 41404 | 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09 | 全读 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 251 | 11957 | d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9 | 全读 |

## 2. 与 B5-r1 的差异定位（逐文件 SHA 比对）

23 文件中 21 个 SHA256 与 V0.01 §1 覆盖表逐一相等；2 个不同：

| 文件 | B5-r1 SHA256 | 当前 SHA256 | 变更来源 | 本席核对 |
|---|---|---|---|---|
| `FloorPlanService.java` | `d912fb381895537f930728f17f6e06df45f7ccd715331572ccd8debbe9834005` | `21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c` | commit `9bb45125`（2026-09-11 M016 闭环，M016-002＝M015-013 转入；已在 origin/main）：V0.02→V0.03，`SHAPE_TYPES` 六值→八值（+ `booth`、`public_area`）+ 一行注释，其余 0 行变化 | 与 V423 两处 CHECK 八值同闭集（§5 log「== 2.」迁移原文 +「== 5.」活库 `pg_constraint` 实查）；`docs/openapi/openapi-p0.yaml` 无 shape_type 枚举、`frontend/src` 无六值硬编码（grep `risk_zone` 均 0）；M015-013 定稿处置「XM→M016 放宽 CHECK + 本模块白名单同批」已按原句落地；M017-005 已补正/反向用例（本次实跑绿）。不是新缺陷 |
| `governance/ProjectRecord.java` | `be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5` | `3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487` | 主工作区已暂存、未提交（M009-013 获批范围：项目档案补 `ownerPerson`/`address`/`businessScope`，允许为空兼容旧调用）：+ 版本头 V0.01、+ 3 字段、+ 10 参兼容构造器 | `floorplan` 包 0 处引用 `ProjectRecord`（§5 log「== 3.」），本模块行为不受影响；字段属 M009 单元集与其定稿授权，不是本模块添加项；本席不对 M009 在途修改投票，仅记录状态 |

## 3. 三线审计（23 文件全文重读，不复用 B5-r1 结论）

- 第一性原理线：从治理 HTML「空间点位／楼层平面图／营业时段」与 PGM-017/022/023、BSM-001/003、BEQR-152 反推本包应实现什么：平面图必归属现存项目且项目员工只在本项目下创建/读取（`validateCreateRequest` :507–518、`checkProjectOwnership` :601–610、`listFloorPlans` :73–79）；图形/任务/发布图形主键含平面图域且恒 36 字符（`derivedId` :651–655）；候选/错误 ID ≤46；发布上限 200（:588）且候选须属本任务、pending；营业时段场地必存在且项目隔离（`VenueBusinessHoursService` :62–69、`list` :43–47）；导入解析 13 处中文错误码；每个读/写路径 `recordSuccess/recordFailure`。与当前源码逐行对照成立。
- 顺从式线：90 判据逐族结论与 V0.01 §5 相同；变化两处：GEN-008「白名单与 CHECK 一致」现为八值同闭集（活库实查）；GEN-003 测试有效性按当前 5 类 83 例（XML 口径）实跑。
- 对抗式线：在当前代码上复核 V0.01 §3 全部对抗场景（跨图同键、项目员工以本项目 token 列表/读取/创建他项目平面图、201 候选、他项目或不存在场地配营业时间、非白名单类型整单拒绝、curl 绕过前端弹窗直接发布＝011 定稿否/XM、17 字符现网键与 UUID 键落库 ≤64）；新增：以 `booth`/`public_area` 保存图形与发布导入候选（`saveShapeAcceptsBoothAndPublicArea`、JDBC booth 例与 `import_candidates` 八值 CHECK 例，本次实跑绿）。B2 定稿禁止项（改迁移/OpenAPI/三份 HTML/权限种子/FieldLabels/PlatformController/前端/ticketing、新增字段或接口或业务规则）在本模块 23 文件内未被越界引入（V423 属 M016 单元集并经 M016 双席闭环）。未发现未定稿缺陷、添加项或 HTML 问题。
- 只读依赖追流（不审）：同 V0.01（V005/V023/V034/V184/V226 + V423 迁移、`IdempotencyService`、`AuthorizationService`、`AuditEventService`、`PageResponse`、`FieldLabels`、`PlatformController`、前端 `FloorPlanImportPage.tsx`/`VenueBusinessHoursPage.tsx`、三份 HTML 现文——三 HTML SHA 与 B5-r1 §2 第 10 项逐一相同）。

## 4. 已定稿 14 项在当前工作区的状态

001–009 已修状态与 V0.01 §4 逐项相同（对应源码文件 SHA 未变，或仅 `SHAPE_TYPES` 一行变化）；010/011 XM 同批、012 全局遗漏、014 观察不变；013：XM→M016 部分已由 V423 + `SHAPE_TYPES` 八值落地（commit `9bb45125`，M016 双席闭环），M018 前端部分不在本模块。修复队列仍 14 个 ID 全否。

## 5. 可执行证据原文（`/home/james/audit-tmp/m015_supp_run.log`，含追加段「== 6b.」）

```
START=2026-09-19T22:49:57Z HEAD=e456ba96 origin/main=246385d9 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
== 0. 23 files + 5 test classes (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java 95 4323 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java 21 503 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java 51 2148 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 678 31232 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java 454 21976 bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java 23 596 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java 14 458 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java 14 295 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java 35 912 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java 303 12350 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java 19 440 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270
backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java 7 112 cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java 22 546 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java 16 383 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java 10 240 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java 13 348 ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java 18 438 b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java 25 770 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487
backend/src/main/java/com/complex/platform/governance/SpaceRecord.java 22 515 f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1
backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java 15 386 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java 29 1244 ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java 119 6399 f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java 115 5939 866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 864 41404 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 251 11957 d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9
== 1. version headers
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:2:// 文件版本：V0.03
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.04
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2:// 文件版本：V0.02
== 2. SHAPE_TYPES vs V423 CHECK (M016-002 / M015-013)
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
6:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
9:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
== 3. ProjectRecord staged diff (M009-013 获批范围)
 1 file changed, 9 insertions(+), 1 deletion(-)
0
== 4. English operator text residue in module (expect none)
(none)
== 5. complex_test flyway level (SELECT only)
427
chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
== 6. tests (main worktree, complex_test, three DB vars only)
0
CLEAN_EXIT=0
15:50:08.112 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:50:09.004 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
15:50:09.033 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:50:09.034 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T15:50:09.186-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Starting FloorPlanJdbcIntegrationTests using Java 21.0.12.1 with PID 1384282 (started by james in /home/james/complex-management/backend)
2026-09-19T15:50:09.187-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T15:50:09.187-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : No active profile set, falling back to 1 default profile: "local"
2026-09-19T15:50:10.037-07:00  INFO 1384282 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-19T15:50:10.107-07:00  INFO 1384282 --- [complex-api] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@70f76276
2026-09-19T15:50:10.107-07:00  INFO 1384282 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-19T15:50:10.117-07:00  INFO 1384282 --- [complex-api] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
2026-09-19T15:50:10.215-07:00  INFO 1384282 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 395 migrations (execution time 00:00.064s)
2026-09-19T15:50:10.227-07:00  INFO 1384282 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 427
2026-09-19T15:50:10.229-07:00  INFO 1384282 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-09-19T15:50:11.040-07:00  INFO 1384282 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T15:50:11.465-07:00  INFO 1384282 --- [complex-api] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
2026-09-19T15:50:11.496-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Started FloorPlanJdbcIntegrationTests in 2.444 seconds (process running for 3.93)
2026-09-19T15:50:11.534-07:00  INFO 1384282 --- [complex-api] [           main] .p.c.c.MultiPartyContactPhonePiiBackfill : PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留
2026-09-19T15:50:12.817-07:00  WARN 1384282 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/openapi endpoint is enabled by default. To disable it in production, set the property 'springdoc.api-docs.enabled=false'
2026-09-19T15:50:12.817-07:00  WARN 1384282 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/swagger-ui endpoint is enabled by default. To disable it in production, set the property 'springdoc.swagger-ui.enabled=false'
2026-09-19T15:50:13.145-07:00  INFO 1384282 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests
2026-09-19T15:50:13.147-07:00  INFO 1384282 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T15:50:13.160-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Starting FloorPlanControllerTests using Java 21.0.12.1 with PID 1384282 (started by james in /home/james/complex-management/backend)
2026-09-19T15:50:13.160-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T15:50:13.160-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : No active profile set, falling back to 1 default profile: "local"
2026-09-19T15:50:13.474-07:00  INFO 1384282 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T15:50:13.562-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.auth.CustomerSessionCookieFilter   : Filter 'customerSessionCookieFilter' configured for use
2026-09-19T15:50:13.563-07:00 DEBUG 1384282 --- [complex-api] [           main] .c.p.g.DeviceCredentialBodyCachingFilter : Filter 'deviceCredentialBodyCachingFilter' configured for use
2026-09-19T15:50:13.563-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.auth.BearerAuthenticationFilter    : Filter 'bearerAuthenticationFilter' configured for use
2026-09-19T15:50:13.563-07:00 DEBUG 1384282 --- [complex-api] [           main] .c.p.a.CustomerTokenAuthenticationFilter : Filter 'customerTokenAuthenticationFilter' configured for use
2026-09-19T15:50:13.563-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.auth.DoubleSubmitCsrfFilter        : Filter 'doubleSubmitCsrfFilter' configured for use
2026-09-19T15:50:13.564-07:00  INFO 1384282 --- [complex-api] [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-09-19T15:50:13.564-07:00  INFO 1384282 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-19T15:50:13.565-07:00  INFO 1384282 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-19T15:50:13.579-07:00  INFO 1384282 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Started FloorPlanControllerTests in 0.43 seconds (process running for 6.013)
2026-09-19T15:50:13.804-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.817-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T15:50:13.846-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T15:50:13.851-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T15:50:13.881-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T15:50:13.888-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.891-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.893-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.896-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.899-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.901-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.903-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.905-07:00 DEBUG 1384282 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T15:50:13.926-07:00  INFO 1384282 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T15:50:13.945-07:00  INFO 1384282 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
2026-09-19T15:50:13.946-07:00  INFO 1384282 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T15:50:13.946-07:00  INFO 1384282 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests]: VenueBusinessHoursServiceTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T15:50:13.947-07:00  INFO 1384282 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
MVN_EXIT=0
com.complex.platform.floorplan.FloorPlanControllerTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.774 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
com.complex.platform.floorplan.FloorPlanImportParserTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.159 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.txt: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.991 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
com.complex.platform.floorplan.FloorPlanServiceTests.txt: Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
com.complex.platform.governance.VenueBusinessHoursServiceTests.txt: Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== 7. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
M  backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
END=2026-09-19T22:50:14Z
== 6b. VenueBusinessHoursServiceTests with nested classes (-Dtest=VenueBusinessHoursServiceTests*) 2026-09-19T22:51:10Z
MVN6B_EXIT=0
com.complex.platform.governance.VenueBusinessHoursServiceTests.txt: Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.919 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
```

说明：surefire 的 `.txt` 摘要对含 `@Nested` 内部类的 `VenueBusinessHoursServiceTests` 写 `Tests run: 0`（两种 -Dtest 写法均如此，为摘要口径问题），以同名 `TEST-*.xml` testsuite 为准：tests=12 failures=0 errors=0 skipped=0，12 个 testcase：rejectsUnknownVenue, listFiltersByProjectScope, rejectsVenueOutsideProjectScope, rejectsMalformedTimeAndMissingVenue, rejectsIdenticalOpenAndCloseTime, savesNormalBusinessWindow, saveAndListWriteAuditEvents, savesDisabledDayWithoutDeletingConfiguration, adminListsAllVenuesAndEffectiveFromIsPersisted, allowsWindowCrossingMidnight, rejectsDayOfWeekOutOfRange, findVenueProjectIdAndListByProjectHitRealSpacesTable。5 类合计 83 例、0 失败/错误/跳过：

| 测试类（XML testsuite） | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| floorplan.FloorPlanControllerTests | 14 | 0 | 0 | 0 |
| floorplan.FloorPlanImportParserTests | 14 | 0 | 0 | 0 |
| floorplan.FloorPlanJdbcIntegrationTests | 7 | 0 | 0 | 0 |
| floorplan.FloorPlanServiceTests | 36 | 0 | 0 | 0 |
| governance.VenueBusinessHoursServiceTests | 12 | 0 | 0 | 0 |

## 6. 本次 Findings

无新增 finding。（23 文件三线全量重读、5 类 83 例实跑，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队）

- surefire `.txt` 摘要对 `@Nested` 的 0 计数属工具口径，非代码缺陷；以 XML 为准。
- `ProjectRecord.java` 暂存改动随 M009 在途（M009-013 获批），不在本模块裁决。
- `VenueBusinessHoursService` 类注释「保存即生效」与 V226 `is_draft/effective_from` 语义分歧仍属 010（否/XM），不变。

## 8. 结论

席甲补充复查：**PASS**（不是新双 PASS；历史 B5-r1 Claude/Grok 双 PASS 保留）。Grok NOT-CALLED。未改源码/测试/三份 HTML/锁/findings；未提交/推送。锁实算/sidecar/ledger 首行三值全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；旧 Grok B5-r1 raw 锁行同值但不作本次调用报数；LOCK-AMEND-COUNT 1。先写 ledger V4.38 保留 PASS，再更新收口文件，本报告最后落盘。


---
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B5-r1 再执行 · 席甲完整报告
版本：V0.03（2026-09-20T04:34:34Z；上方 V0.01/V0.02 原样保留，66454 bytes，SHA256 1dfa94ba6be7f0364d127df40d9d5ecefe1673de78cb13370f9f654c60f87b56）

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票）。操作员 2026-09-20 裁定：M015–M017 补派 Grok B5 全量重审，两席同一 B3 次数（累计 2）下向同名 b5 raw 追加完整报告，双 PASS 才维持 PASS。本报告是席甲本次 B5 再执行的完整报告：从本行上方独占锁行起至文件末尾。
- 范围：23 文件（floorplan 包 17 + governance 包 ProjectRecord/SpaceRecord/SpaceTypeRecord/VenueBusinessHoursRecord/VenueBusinessHoursRepository/VenueBusinessHoursService）；90 判据（BEQR 22、BSM 14、GEN 18、PGM 36）；锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 顺序：Grok 已先派（追加到 modules/M015-b5-r1-grok-raw.md，存在=True），本报告落盘前未读其本次追加段；只读承诺同 V0.02（未改源码/测试/三 HTML/锁/findings，测试只导出三个 DB 变量、complex_test）。

## 1. 与 V0.02 审本的一致性

- V0.02（席甲补充复查完整报告）覆盖表 28 个文件当前 SHA256 逐一复算：全部相等——V0.02 的三线审计、判据逐族核对与 finding 结论对本次工作区状态直接有效，本报告不重复抄写。
- 三份业务 HTML 与锁的 SHA 与 V0.02 相同（§2 log 末段）。

## 2. 本次重跑证据原文（/home/james/audit-tmp/m015_reexec_run.log）

```
START=2026-09-20T04:34:14Z HEAD=e456ba96 origin/main=84e3441f URL=jdbc:postgresql://127.0.0.1:5432/complex_test
== 0. 23 files + 5 test classes (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java 95 4323 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java 21 503 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java 51 2148 9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 678 31232 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java 454 21976 bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java 23 596 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java 14 458 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java 14 295 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java 35 912 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java 303 12350 9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java 19 440 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270
backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java 7 112 cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5
backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java 22 546 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java 16 383 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java 10 240 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java 13 348 ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java 18 438 b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java 25 770 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487
backend/src/main/java/com/complex/platform/governance/SpaceRecord.java 22 515 f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1
backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java 15 386 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java 29 1244 ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java 119 6399 f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java 115 5939 866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 864 41404 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 251 11957 d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9
== 1. version headers
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:2:// 文件版本：V0.03
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java:2:// 文件版本：V0.01
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.04
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2:// 文件版本：V0.02
== 2. SHAPE_TYPES vs V423 CHECK (M016-002 / M015-013)
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
6:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
9:    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
== 3. ProjectRecord staged diff (M009-013 获批范围)
 1 file changed, 9 insertions(+), 1 deletion(-)
0
== 4. English operator text residue in module (expect none)
(none)
== 5. complex_test flyway level (SELECT only)
427
chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
== 6. tests (main worktree, complex_test, three DB vars only)
0
CLEAN_EXIT=0
21:34:25.009 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
21:34:25.073 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
21:34:25.105 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
21:34:25.106 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T21:34:25.257-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Starting FloorPlanJdbcIntegrationTests using Java 21.0.12.1 with PID 1547337 (started by james in /home/james/complex-management/backend)
2026-09-19T21:34:25.257-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T21:34:25.258-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : No active profile set, falling back to 1 default profile: "local"
2026-09-19T21:34:26.132-07:00  INFO 1547337 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-19T21:34:26.208-07:00  INFO 1547337 --- [complex-api] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@43094e0c
2026-09-19T21:34:26.209-07:00  INFO 1547337 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-19T21:34:26.221-07:00  INFO 1547337 --- [complex-api] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
2026-09-19T21:34:26.347-07:00  INFO 1547337 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 395 migrations (execution time 00:00.088s)
2026-09-19T21:34:26.360-07:00  INFO 1547337 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 427
2026-09-19T21:34:26.362-07:00  INFO 1547337 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-09-19T21:34:27.196-07:00  INFO 1547337 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T21:34:27.627-07:00  INFO 1547337 --- [complex-api] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
2026-09-19T21:34:27.664-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Started FloorPlanJdbcIntegrationTests in 2.54 seconds (process running for 3.198)
2026-09-19T21:34:27.721-07:00  INFO 1547337 --- [complex-api] [           main] .p.c.c.MultiPartyContactPhonePiiBackfill : PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留
2026-09-19T21:34:27.725-07:00  WARN 1547337 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/openapi endpoint is enabled by default. To disable it in production, set the property 'springdoc.api-docs.enabled=false'
2026-09-19T21:34:27.725-07:00  WARN 1547337 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/swagger-ui endpoint is enabled by default. To disable it in production, set the property 'springdoc.swagger-ui.enabled=false'
2026-09-19T21:34:28.090-07:00  INFO 1547337 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests
2026-09-19T21:34:28.092-07:00  INFO 1547337 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T21:34:28.106-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Starting FloorPlanControllerTests using Java 21.0.12.1 with PID 1547337 (started by james in /home/james/complex-management/backend)
2026-09-19T21:34:28.106-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T21:34:28.106-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : No active profile set, falling back to 1 default profile: "local"
2026-09-19T21:34:28.449-07:00  INFO 1547337 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T21:34:28.586-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.auth.CustomerSessionCookieFilter   : Filter 'customerSessionCookieFilter' configured for use
2026-09-19T21:34:28.587-07:00 DEBUG 1547337 --- [complex-api] [           main] .c.p.g.DeviceCredentialBodyCachingFilter : Filter 'deviceCredentialBodyCachingFilter' configured for use
2026-09-19T21:34:28.587-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.auth.BearerAuthenticationFilter    : Filter 'bearerAuthenticationFilter' configured for use
2026-09-19T21:34:28.587-07:00 DEBUG 1547337 --- [complex-api] [           main] .c.p.a.CustomerTokenAuthenticationFilter : Filter 'customerTokenAuthenticationFilter' configured for use
2026-09-19T21:34:28.587-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.auth.DoubleSubmitCsrfFilter        : Filter 'doubleSubmitCsrfFilter' configured for use
2026-09-19T21:34:28.588-07:00  INFO 1547337 --- [complex-api] [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-09-19T21:34:28.588-07:00  INFO 1547337 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-19T21:34:28.590-07:00  INFO 1547337 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 2 ms
2026-09-19T21:34:28.614-07:00  INFO 1547337 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Started FloorPlanControllerTests in 0.518 seconds (process running for 4.147)
2026-09-19T21:34:28.831-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.840-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T21:34:28.862-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T21:34:28.865-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T21:34:28.882-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T21:34:28.885-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.887-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.888-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.890-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.891-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.892-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.893-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.894-07:00 DEBUG 1547337 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T21:34:28.912-07:00  INFO 1547337 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T21:34:28.928-07:00  INFO 1547337 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
2026-09-19T21:34:28.930-07:00  INFO 1547337 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T21:34:28.930-07:00  INFO 1547337 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests]: VenueBusinessHoursServiceTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T21:34:28.931-07:00  INFO 1547337 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
MVN_EXIT=0
com.complex.platform.floorplan.FloorPlanControllerTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.816 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
com.complex.platform.floorplan.FloorPlanImportParserTests.txt: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.153 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.txt: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.022 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
com.complex.platform.floorplan.FloorPlanServiceTests.txt: Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.113 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
com.complex.platform.governance.VenueBusinessHoursServiceTests.txt: Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== 7. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
M  backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
END=2026-09-20T04:34:29Z
```

5 类（FloorPlanControllerTests/FloorPlanImportParserTests/FloorPlanJdbcIntegrationTests/FloorPlanServiceTests/VenueBusinessHoursServiceTests） 本次实跑：5 类 83 例（TEST-*.xml 口径）零失败/错误/跳过，MVN_EXIT=0。

## 3. 三线结论

- 第一性原理线 / 顺从式线 / 对抗式线的逐条内容以 V0.02 为准（同一文件集合、SHA 未变）；本次再执行重跑全部可执行证据，结果与 V0.02 一致，未发现新缺陷、添加项或 HTML 问题。
- 修复队列不变（历史 finding 全否）。

## 4. 结论

席甲 B5-r1 再执行票：**PASS**。Grok 本次追加段未读；两席落齐后做四值校验并合取判定。


---
## 席甲 B5-r1 再执行票复核撤回（V0.04，2026-09-20T04:53:46Z；不新增独占锁行，本段属上方 V0.03 报告的追加）

- 两席落齐后读 Grok V0.02 追加段（modules/M015-b5-r1-grok-raw.md 全文 73711 bytes，SHA256 837327dbb73b46d13675de525863593d8e87fadc12ab8a98d65e99e40bb104a6）：结论 FAIL，GR-M015-B5r1-01/02。
- 席甲核源（只读）：`AuthorizationService.java:504-507` `currentProjectScopeQuietly()` = `Optional.of(requireProjectScope(actor))`，`:289-294` 无 organizationId 抛 `PROJECT_SCOPE_REQUIRED`（注释「红线④」，M006 回归 `:150` 锁定）；`FloorPlanServiceTests:445-461` 与 `VenueBusinessHoursServiceTests:89-94` 桩伪造「永不抛」——GR-01 成立；`FloorPlanService.java:365-367/422-435` 发布只接受 parsed 且发布后整任务 published——GR-02 成立。两条均为席甲 V0.02/V0.03 漏审（沿用了测试桩语义而未追 `AuthorizationService` 真实现，违反 2.3「测试通过了所以是对的」禁令），如实登记。
- 席甲 B5-r1 再执行内容票撤回为 **FAIL**；合取 FAIL。两条无 ID → 按回退统一规则整批回 B2（B2-r3），席甲不修。
