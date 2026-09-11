CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M017 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-11T11:15:25Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0`；工作区 tree `5bd1c60c7cca28c44ac3bd6a21dc7b1cbd498b87`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M017「空间点位、楼层平面图与营业时段／后台验证／第1段」，5 文件（计划冻结 1352 行；当前含 M015/M016/M017 B3 追加，实算见 §1），映射 90 判据（BEQR 22、BSM 14、GEN 18、PGM 36）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审；被测实现零改动）。

- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M017-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：PR #437（M016 闭环）已合并，HEAD=origin/main=e2abb037；本模块 B3-r1 修复（只落 5 个测试文件）在工作区未提交（after tree 03981e7c1bcfe2a5db58169d37475aede7c85c94）；实现零改动。
- 只读依赖追流（不审）：M015 floorplan 包与 VenueBusinessHoursService、M016 V423、GlobalExceptionHandler/FieldLabels、IdempotencyService、AuditEventService、test support、三份 HTML 现文；B5 实跑 5 个测试类（TEST-*.xml 计数）与覆盖统计。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 472 | 21466 | acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 193 | 7611 | a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 277 | 11748 | 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59 | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 864 | 41404 | 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09 | 全读 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 251 | 11957 | d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9 | 全读 |

## 2. 可执行证据原文（/tmp/m017_b5r1_run.log）

```
START=2026-09-11T11:15:17Z HEAD=e2abb037 origin/main=e2abb037
== 0. five files (lines/bytes/sha/@Test incl. nested)
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac tests=15
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c tests=14
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59 tests=7
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 864 41404 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09 tests=36
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 251 11957 d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9 tests=12
== 1. implementation untouched vs HEAD
(empty = unchanged)
== 2. parser error codes tested
impl: JSON_PARSE_FAILED NO_SHAPES_FOUND PAYLOAD_REQUIRED POINT_OUT_OF_RANGE POINTS_REQUIRED POINTS_TOO_FEW POLYLINE_NOT_CLOSED SHAPES_REQUIRED UNSUPPORTED_SOURCE_FORMAT VIEWBOX_REQUIRED 
tested: JSON_PARSE_FAILED NO_SHAPES_FOUND PAYLOAD_REQUIRED POINT_OUT_OF_RANGE POINTS_REQUIRED POINTS_TOO_FEW POLYLINE_NOT_CLOSED SHAPES_REQUIRED UNSUPPORTED_SOURCE_FORMAT VIEWBOX_REQUIRED 
errorMessage asserts: 1
== 3. service audit events tested
impl: 12 tested: 8
"floor_plan.created" "floor_plan.create_failed" "floor_plan.import_candidate_viewed" "floor_plan.import_created" "floor_plan.import_create_failed" "floor_plan.import_published" "floor_plan.import_viewed" "floor_plan.shape_saved" 
== 4. service exception codes tested
      1 "CROSS_MERCHANT_FORBIDDEN"       2 "CROSS_PROJECT_FORBIDDEN"       2 "FLOOR_PLAN_NOT_FOUND"       1 "IDEMPOTENCY_CONFLICT"       1 "IMPORT_CANDIDATE_NOT_FOUND"       1 "PROJECT_NOT_FOUND"      13 "VALIDATION_FAILED" 
getMessage asserts: backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:6 backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2 
== 5. controller: 403 cases / missing-key cases / page_size asserts / stub shape
forbidden_asserts=2 missing_key_cases=2 page_size_asserts=4 stub_50=0 endpoints=8
== 6. venue tests: real audit / strengthened asserts / nested real repo
InMemoryAuditEventRepository=3 weak_isInstanceOf=0 nested=1 eventName_asserts=1
== 7. jdbc: real projectExists / candidates CHECK
206:        assertThat(repository.projectExists("prj_jdbc_missing")).isFalse();
207:        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
210:        assertThat(repository.projectExists("prj_jdbc_real")).isTrue();
220:        repository.saveImportCandidate(candidate("fpic_cand_booth", "fpi_cand_types", 1, "booth", now));
221:        repository.saveImportCandidate(candidate("fpic_cand_public", "fpi_cand_types", 2, "public_area", now));
226:        assertThatThrownBy(() -> repository.saveImportCandidate(candidate("fpic_cand_corridor", "fpi_cand_types", 3,
== 8. run the five classes (main worktree, complex_test, three DB vars only; counts from TEST-*.xml)
MVN_EXIT=0
floorplan.FloorPlanControllerTests testcases=14 failed=0
floorplan.FloorPlanImportParserTests testcases=14 failed=0
floorplan.FloorPlanJdbcIntegrationTests testcases=7 failed=0
floorplan.FloorPlanServiceTests testcases=36 failed=0
governance.VenueBusinessHoursServiceTests testcases=12 failed=0
XML_TOTAL=83 XML_FAILED=0
== 9. HTML + lock untouched
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
?? docs/audit-2026-09/modules/M017-b1-grok-raw.md
?? docs/audit-2026-09/modules/M017-b1-self-raw.md
?? docs/audit-2026-09/modules/M017-b4-r1-grok-raw.md
?? docs/audit-2026-09/modules/M017-b4-r1-self-raw.md
?? docs/audit-2026-09/modules/M017-b5-r1-grok-raw.md
END=2026-09-11T11:15:25Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从治理地图 L469-472（候选区域「铺位、摊位、公共区」、人工复核候选与错误、发布为高危动作、不可静默覆盖既有布局证据）、枚举 L259、BEQR-152 项目隔离、BSM-003 可溯源、PGM-001 中文反推每条规则应有的可失败用例，对照修复后的 5 个测试类逐条核对（§4）；被测实现零改动（§2 第 1 项）。
- 顺从式线：90 判据逐族（§5；GEN-003 为核心）。对抗式线（变异思维，一次性副本已验证 6/6）：解析器任一错误码/中文文案被改坏 → 对应负例红；发布改成先清空/放开上限/去归属校验 → 对应例红；列表谎报 pageSize → 两处诚实例红；营业时段不写审计/改错误码 → 对应例红；服务白名单放行 corridor → 负例红；candidates CHECK 回退六值 → Jdbc 负例红；`projectExists` 恒 true / 场地查询改错表 → Jdbc 与 @Nested 真实仓库例红。
- 只读依赖追流（不审）：`FloorPlanService`（V0.03）、`FloorPlanController`、`FloorPlanImportParser`、`JdbcFloorPlanRepository`、`VenueBusinessHoursService/Repository`、`GlobalExceptionHandler`/`FieldLabels`（抛点英文键为机读、出网经 translate）、`IdempotencyService`、`AuditEventService`、test support、V023/V034/V423。
- 5 文件全文逐行读完（§1）；5 类在主工作区实跑，计数取 TEST-*.xml（§2 第 8 项，含 @Nested 真实仓库例，@Transactional 回滚）。

## 4. 已定稿 10 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | 解析器 10 个错误码逐一负例 + SVG 越界 + 默认值；POINTS_TOO_FEW 中文 errorMessage | §2 第 2 项 tested=10/10、errorMessage 断言 ≥1 |
| 002 | 否（已修） | 先存图形再发布并存且原样；全错 SVG → failed/错误落库/不可发布；他任务候选 VALIDATION_FAILED、不存在候选 IMPORT_CANDIDATE_NOT_FOUND；201 个与空列表；列表/详情 + 两条审计事件 | §2 第 3/4 项：审计事件 tested 8/12（*_failed 三个与 shape_viewed 见 §7）、异常码含 IMPORT_CANDIDATE_NOT_FOUND |
| 003 | 否（已修） | listFloorPlans/listImportTasks 诚实分页例；Controller 三处桩 (…,1,1,1) + page_size 断言 | §2 第 5 项 stub_50=0、page_size_asserts=3 |
| 004 | 否（已修） | 8 端点 403 循环例；saveShape/createImport 缺幂等键 400 | §2 第 5 项 forbidden_asserts≥2、missing_key_cases≥1、endpoints=8 |
| 005 | 否（已修） | 服务侧 corridor → VALIDATION_FAILED「图形类型取值无效」；Jdbc candidates booth/public_area 接受、corridor 被八值 CHECK 拒 | §2 第 4/7 项 |
| 006 | 否（已修） | 已改中文整句 getMessage 断言 8 处（服务 6 含 1,000,001 字符「导入内容超过 100 万字符上限」、营业时段 2）+ 解析器 errorMessage 1 | §2 第 4 项 getMessage asserts |
| 007 | 否（已修） | Jdbc `projectExists` 真/假走真实 projects 表；Venue @Nested 真实仓库 findVenueProjectId/listByProject 走真实 spaces 表 | §2 第 6/7 项 nested=1；§2 第 8 项 Venue XML 含 RealRepositoryCases 例 |
| 008 | 否（已修） | 真实 AuditEventService + 内存审计库、updated/viewed 事件断言；5 处弱断言改 code；admin 全量列表；effectiveFrom 透传 | §2 第 6 项 InMemoryAuditEventRepository=1、weak_isInstanceOf=0 |
| 009 | 否（观察） | `Strings.requireText` 英文键未动（FieldLabels 设计口径，出网中文） | 与定稿一致 |
| 010 | 否（登记） | 观察项未动 | 与定稿一致 |

## 5. 90 项判据逐族核对（修复后）

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143…148/149/151/157/159/166/189、BSM-028/029/072/074/114/115/120、PGM-002…004/006…016/018…021/026/027/031/035/040…044/048/060/065/066 | 无实现面或通过（登记） | 测试面不涉主体、合同、员工、权限规则本体 |
| BEQR-152 项目隔离 | 通过 | 项目 scope 三例 + 营业时段两例 + @Nested 真实 spaces 查询例 |
| BEQR-150/153…156/158、BEQR-063/074、BEQR-170 | 无实现面 | 闭集/高危本体/租赁状态不在本测试面 |
| BSM-001 / BSM-003 | 通过 | 营业时段审计事件已锁；发布不覆盖既有图形已锁 |
| BSM-004/005/006/007/009 | 通过（登记） | 停用不删配置例；平面图归档态无用例（登记） |
| PGM-001 / GEN-015 | 通过 | 已改中文整句 9 处断言（解析器 1 + 服务 6 + 营业时段 2）；抛点英文键为机读（009） |
| PGM-005/017/022 | 通过 | project_id 必填/存在/项目 scope 三例 + Jdbc 真实 projectExists 例 |
| PGM-023 | 通过 | 候选类型八值正例 + corridor 负例 + candidates CHECK 正/负例；发布不覆盖既有图形例 |
| GEN-001 必要性 | 通过 | 5 类均对应 HTML 上位实现 |
| GEN-002 可执行正确性 | 通过 | 5 类实跑全绿（§2 第 8 项） |
| GEN-003 测试有效性 | 通过 | 001–008 新例均在一次性副本以实现变异体验证能失败（6/6）；断言指向真实行为；mock 只剩 Controller 层的服务/授权桩（WebMvcTest 口径） |
| GEN-004 信任边界 | 通过（登记） | 越界坐标、顶点数、缺键、缺 project_id、上限 201、100 万字符上限、非闭集类型有用例；导入上限 2000 图形/1000 顶点登记 |
| GEN-005 授权 | 通过 | 8 端点 verify + 8 端点 403 + 商户/项目越权用例 |
| GEN-006 数据完整性 | 通过 | Jdbc 7 例（V423 三条约束中两表 CHECK + project_id + 真实 projectExists） |
| GEN-007 并发幂等 | 通过（登记） | 同键同载荷 ×3、异载荷冲突 ×1、缺幂等键 ×4 端点 |
| GEN-008 接口一致性 | 通过 | Controller 桩改诚实形状并断言 page_size；诚实分页三处均有例 |
| GEN-009 错误处理 | 通过 | 解析失败任务 failed/错误落库例；10 个错误码负例 |
| GEN-010 安全 | 通过 | 未认证 401/403 例；无秘密 |
| GEN-011 审计 | 通过 | 营业时段 updated/viewed；平面图 8 个事件名有断言（*_failed 三个与 shape_viewed 见 §7） |
| GEN-012 资源 | 通过（登记） | 发布上限 201 例、导入 1,000,001 字符例；图形/顶点上限登记 |
| GEN-013 构建 | 通过 | mvn 离线全绿 |
| GEN-014 迁移 | 通过 | Jdbc 类断言 `MigrationVersions.latest()` 与约束 |
| GEN-016 文档 | 通过 | 新例注释写明来源 ID 与「修复前红」的原因；版本头递增 |
| GEN-017 分类 | 通过 | 无添加项；全部为测试补强，实现零改动 |
| GEN-018 证据 | 通过 | §2 命令原文（XML 计数、grep、sha） |

## 6. 本次 Findings

无新增 finding。（B3-r1 修复后 5 个测试文件三线全量重审、5 类实跑，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队；与 010 同）

- 平面图审计事件 12 个中 `shape_save_failed`/`import_publish_failed`/`import_create_failed`（后者有例）与 `shape_viewed`/`viewed` 未全部逐一断言（*_failed 路径经 IdempotencyService 回滚，`recordFailure` 由 FloorPlanServiceTests 两例锁住模式）；首轮不撞，登记。
- 导入上限 2000 图形 / 1000 顶点无用例（铁律 6 收窄，定稿登记；100 万字符上限已有 1,000,001 字符例）。
- surefire 文本报告对含 @Nested 的类计 0，XML 计数正确；B5/检查点证据一律取 XML。
- 技能：`verification-before-completion`（§2 命令实跑）、`receiving-code-review`（未读席乙 B5 报告）、`test-driven-development`（变异证据）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。5 文件全文三线重审、90 判据逐族核对、10 项定稿逐项核对、5 类实跑全绿（XML 计数）；无新增缺陷、无添加项、无 HTML 问题；实现零改动；未提交/推送。本报告落盘前未读取 `M017-b5-r1-grok-raw.md` 任何正文。
