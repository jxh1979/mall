CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U11 未发布材料第 11 段（员工回归测试）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-10T00:26:36Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `4a11e702bd5464d1408666d6724496f960c4dc35`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 11 段，8 文件 2247 行（backend/src/test/.../access/staff：M010RegistrationHttpTests 38 新增 / M010RegressionTests 676 新增 / PayrollSafetyJdbcIntegrationTests 207 / StaffAttendanceLocalZoneTests 165 / StaffJdbcIntegrationTests 942 / StaffProfileArchiveDeleteJdbcIntegrationTests 110 / StaffScopeTests 80 / StaffTestActors 29 新增），main 主归属 M056；适用判据＝M056 映射行并集 52 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。可执行验证在 `/tmp/u-overlay/backend`（基线 242f4872 `git archive` + 76 未发布文件）内进行：席甲新建一次性空库 `u11_scratch`（非 complex_test*），仅在 overlay 副本内把测试的数据源改指该库，由 Flyway 从空库迁移 V001–V409 后运行本段 7 个测试类，结束后删除该库；未对任何 complex_test* 库做写操作。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U11-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U10 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U11-00N，处置写给发布者），不做 B3/B4/B5。
- 跨段引用：U05 定稿（事假扣薪基数、公开注册信任边界）、U01-001/U02-001（客户端自报终端类型）、questions/U04-001.md（自营独立布尔已答删除）、U10（迁移编号）——本段测试把这些已定稿缺陷锁成断言处按引用登记。只读依赖追流（不审）：副本 `JdbcStaffRepository`/`StaffService`/`StaffRegistrationService`（第 5/6 段）、`SupervisorReviewService`（第 9 段）、V400–V409（第 10 段）；main `support/MigrationVersions.java`、`TestSecurityActors`。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java | 38 | 2291 | 1972b4506db19caf3ace491d766baf8150a4f410dad1c189f2b976187f846fbd | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | 676 | 44746 | 9805d8eb5614ae4aa38196aab889b2cb71859d96cf7104561eefd0f1f91596cd | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java | 207 | 10978 | 854d792d1b0115a15cb0ec10451e44c1318da0a22fc367aa22c9438331aebddb | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java | 165 | 10221 | 559a74ad36ede377005c33a22d31dda69a49529793be280f7d810b854127be48 | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java | 942 | 58934 | 8ac57d3efeb7b8f2a12d0dea5719b9cb22df79d94d8447b510764ec22a523883 | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java | 110 | 5064 | 4a010733c614bd6ca6e5620b27ddae2ed6cba14a764da6cba630d24973ba5632 | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java | 80 | 4359 | 96993632643830c3e5967e45e7da4f1ac87558556f8b71e4a9bd8dd062ad46b8 | 全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | 29 | 1892 | 229787b24976e4fb14c7abee65165715a5ec64437a55f43a4a10415cbe55e9c8 | 全读 |

## 2. 可执行证据原文（/tmp/u11_b1_run.log）

```
START=2026-09-09T21:00:23Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java | 38 | 2291 | 1972b4506db19caf3ace491d766baf8150a4f410dad1c189f2b976187f846fbd
backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | 676 | 44746 | 9805d8eb5614ae4aa38196aab889b2cb71859d96cf7104561eefd0f1f91596cd
backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java | 207 | 10978 | 854d792d1b0115a15cb0ec10451e44c1318da0a22fc367aa22c9438331aebddb
backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java | 165 | 10221 | 559a74ad36ede377005c33a22d31dda69a49529793be280f7d810b854127be48
backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java | 942 | 58934 | 8ac57d3efeb7b8f2a12d0dea5719b9cb22df79d94d8447b510764ec22a523883
backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java | 110 | 5064 | 4a010733c614bd6ca6e5620b27ddae2ed6cba14a764da6cba630d24973ba5632
backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java | 80 | 4359 | 96993632643830c3e5967e45e7da4f1ac87558556f8b71e4a9bd8dd062ad46b8
backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | 29 | 1892 | 229787b24976e4fb14c7abee65165715a5ec64437a55f43a4a10415cbe55e9c8
=== manifest entries ===
backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java 38 2291 1972b4506db19caf3ace491d766baf8150a4f410dad1c189f2b976187f846fbd
backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java 676 44746 9805d8eb5614ae4aa38196aab889b2cb71859d96cf7104561eefd0f1f91596cd
backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java 207 10978 854d792d1b0115a15cb0ec10451e44c1318da0a22fc367aa22c9438331aebddb
backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java 165 10221 559a74ad36ede377005c33a22d31dda69a49529793be280f7d810b854127be48
backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java 942 58934 8ac57d3efeb7b8f2a12d0dea5719b9cb22df79d94d8447b510764ec22a523883
backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java 110 5064 4a010733c614bd6ca6e5620b27ddae2ed6cba14a764da6cba630d24973ba5632
backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java 80 4359 96993632643830c3e5967e45e7da4f1ac87558556f8b71e4a9bd8dd062ad46b8
backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java 29 1892 229787b24976e4fb14c7abee65165715a5ec64437a55f43a4a10415cbe55e9c8
=== baseline 242f4872 -> unpublished tree e28dad05 (numstat) ===
38	0	backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java
676	0	backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
5	0	backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java
27	14	backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java
90	78	backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java
12	4	backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
13	6	backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java
29	0	backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
=== main HEAD vs baseline (blob) ===
backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java main_exists=no
backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java main_exists=no
backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java baseline=828945fe22 head=828945fe22 main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java baseline=d8eeb94435 head=d8eeb94435 main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java baseline=765c2c1a2b head=765c2c1a2b main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java baseline=94c9550225 head=94c9550225 main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java baseline=674ee3cec0 head=674ee3cec0 main_unchanged_since_baseline=yes
backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java main_exists=no
=== probes: locked behaviours in unpublished tests ===
652:                Long.class, partial.userId())).isEqualTo(5000L);
672:                Long.class, profile.userId())).isEqualTo(2200L);
674:                Long.class, profile.userId())).isEqualTo(12800L);
24:        mvc.perform(post("/api/v1/staff/self-registrations").header("X-Terminal-Type", "app").contentType(MediaType.APPLICATION_JSON).content(body)
323:        platform.saveMerchant(merchant.withSelfOperated(true).withArchive(Instant.now(),manager,Instant.now()).withArchive(null,null,Instant.now()
324:        assertThat(platform.findMerchantById(registrationMerchant).orElseThrow().selfOperated()).isTrue();
85:    @AfterEach void clear() { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestCo
307:        SecurityContextHolder.clearContext();
327:        SecurityContextHolder.clearContext();
523:        var start = day.atTime(9, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant();
555:                LocalDate.ofInstant(from, ZoneId.of("Asia/Shanghai")), from, Instant.parse(end),
287:                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"
296:                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java:760:     
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java:1
17:        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES('role_m010_fixture','M010 测试管理','M010 测试角�
18:        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code 
19:        access.assignUserRole(id,"role_m010_fixture");
104:        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
713:    private String currentFlywayVersion() {
=== probes: complex_test hardcoded in @SpringBootTest properties; main V410 present; unpublished V400-V409 ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java:1 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java:1 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java:1 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java:0 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java:1 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java:1 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java:0 .scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java:0 
1
10
=== HTML greps (main 正本) ===
1450:                <li>请假扣薪：本综合体实行综合计算工时。只有事假（无薪
1458:                <li>每位员工都有一个结构化的薪资配置：基础薪资、全勤奖
=== isolated scratch-DB run (/tmp/u11_scratch_run.sh -> /tmp/u11_tests.log): fresh DB u11_scratch, overlay copy, Flyway V001..V409 ===
START=2026-09-09T20:56:15Z
CREATE DATABASE
SCRATCH_DB_CREATED
repointed_files=5 remaining_complex_test_refs=0
MVN_EXIT=0
target/surefire-reports/com.complex.platform.access.staff.M010RegistrationHttpTests.txt:Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.677 s -- in com.complex.platform.access.staff.M010RegistrationHttpTests
target/surefire-reports/com.complex.platform.access.staff.M010RegressionTests.txt:Tests run: 43, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.178 s -- in com.complex.platform.access.staff.M010RegressionTests
target/surefire-reports/com.complex.platform.access.staff.PayrollSafetyJdbcIntegrationTests.txt:Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.063 s -- in com.complex.platform.access.staff.PayrollSafetyJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffAttendanceLocalZoneTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in com.complex.platform.access.staff.StaffAttendanceLocalZoneTests
target/surefire-reports/com.complex.platform.access.staff.StaffJdbcIntegrationTests.txt:Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.605 s -- in com.complex.platform.access.staff.StaffJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffProfileArchiveDeleteJdbcIntegrationTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.complex.platform.access.staff.StaffProfileArchiveDeleteJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffScopeTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.547 s -- in com.complex.platform.access.staff.StaffScopeTests
flyway_max_version=409
DROP DATABASE
SCRATCH_DB_DROPPED
END=2026-09-09T20:56:33Z
END=2026-09-09T21:00:24Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 11 段（8 个员工域测试文件 2247 行；`M010RegistrationHttpTests`、`M010RegressionTests`、`StaffTestActors` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 numstat），main 当前版本仅作分叉对照。
- 第一性原理线：测试的价值在于把 HTML 规则锁成可失败断言——逐用例对照 HTML：BSM L1450 事假扣薪（基数「当期应发薪资」、工时比例、零下限、跨月归班次结束月）、L1458 扣除上限 20%（三审 + 顶层确认、已月结沿用原上限、总薪资口径）、L1443 排班上一级审批与修改留痕、L1444 可见天数、L1445 领取工作手机三种方式取最早一次、L1446 迟到早退、PGM L578/L579 两条注册路径与设备绑定、PGM L582/BSM L938 寄存柜自动记录、PGM L870–871 加密与脱敏、BEQR L240 五态。
- 顺从式线：52 项判据逐项（§5）。
- 对抗式线：哪些断言把已定稿缺陷锁死（U05 事假基数、U01/U02 客户端自报终端类型与匿名注册、U04-001 自营布尔）；测试能否在 main 上运行（硬编码 complex_test 与迁移编号 U10-001）；夹具授权是否过宽（`role_m010_fixture` 授全部 `staff.%` + 审批权限）；硬编码时区、旧哈希列、主会话 id 字面量；测试自编号「M010-xxx」与本审计模块号撞名。
- 只读依赖追流（不审、不计入本段结论）：副本 `JdbcStaffRepository`/`StaffService`/`StaffRegistrationService`（第 5/6 段）、`SupervisorReviewService`（第 9 段）、V400–V409（第 10 段）；main `support/MigrationVersions.java`（读类路径迁移目录取最新版本）、`TestSecurityActors`。
- 8 文件全文逐行读完（§1）；与基线的逐文件 diff 逐块读完（§2 numstat）。可执行验证（席甲环境决定，记 ledger）：共享测试库 complex_test 不写；在 160 新建一次性空库 `u11_scratch`，仅在 `/tmp/u-overlay` 副本内把 5 个硬编码 `complex_test` 的测试改指该库，由 Flyway 从空库迁移 V001–V409 后运行本段 7 个测试类，结束后删库（§2：`MVN_EXIT=0`，7 类 92 例 0 失败 0 错误，`flyway_max_version=409`，`SCRATCH_DB_DROPPED`）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- 5 个既有测试 main 自基线是否改动见 §2 blob 对照（M009–M012 未触及 access/staff 测试）；3 个新增文件 main 无对应。
- 本段测试只能在应用 V400–V409 的库上运行；main 已有 V410（U10-001 编号重排）→ 发布者重排迁移后本段测试不需改动（`MigrationVersions.latest()` 从类路径取最新版本）。

## 5. 52 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 每个用例对应 HTML 规则或既有缺陷回归 |
| GEN-002 可执行正确性 | 通过 | 一次性空库实跑 92/92 通过（§2） |
| GEN-003 测试有效性 | 不通过 | 三组断言把已定稿缺陷锁死：事假扣薪基数取固定工资（CX-U11-001）、注册接口自报 `X-Terminal-Type: app` 与匿名注册（CX-U11-002）、自营独立布尔（CX-U11-003）；发布者按 U05/U01/U04-001 处置修改源码时这些用例会由绿转红，须同批改断言 |
| GEN-004 信任边界 | 通过（登记） | 测试覆盖匿名注册只落待审、公开端口不开管理接口；但把「匿名可写」当作期望行为锁定（CX-U11-002） |
| GEN-005 授权 | 通过（登记） | 覆盖 SoD、跨商户/跨公司拒绝、主会话删除、`role_staff_self` 绑定；夹具 `role_m010_fixture` 授全部 `staff.%` 权限（OBS-1） |
| GEN-006 数据完整性 | 通过 | 覆盖已月结不改写、唯一 active 凭证、批量原子性、幂等 |
| GEN-007 并发幂等 | 通过 | 幂等重放用例（注册/绑定/奖惩/提成/请假） |
| GEN-008 接口一致性 | 通过（登记） | HTTP 用例只 2 例（自助注册、未授权 401）；其余走服务层 |
| GEN-009 错误处理 | 通过 | 失败路径审计事件用例 |
| GEN-010 安全 | 通过 | 加密落库 `enc:v1:`、脱敏响应、明文不外泄 |
| GEN-011 审计证据 | 通过 | 失败/成功审计事件断言 |
| GEN-012 资源 | 通过 | 单库事务回滚 |
| GEN-013 构建 | 通过（登记） | 硬编码 `jdbc:postgresql://127.0.0.1:5432/complex_test`（与 main 既有测试同范式）；编号重排前在 main 不可运行（U10-001） |
| GEN-014 迁移 | 不适用（登记） | 依赖第 10 段 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | 用例注释以「M010-xxx」自编号，与本审计 M010 模块 ID 撞名（OBS-2） |
| GEN-017 分类 | 通过 | 本段无添加；CX-U11-003 为 U04-001 已答删除项的测试面 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 实跑 |
| 其余 M056 映射行（BSM/PGM/BEQR 34 项） | 通过／不适用 | 测试锁定的规则均与 HTML 同向（五态、六态、三种打卡方式、跨月归属、脱敏、SoD、可见天数默认 7（但缺行回落无上下限，见 OBS-8）、上限出厂 20%、三审+顶层）；例外仅上述三组 |

## 6. 本次 Findings（临时 ID CX-U11-00N；正式 ID 由 B2 分配）

### CX-U11-001 · 技术性 · 发布前须修：是

- 位置：`M010RegressionTests.java:637-655`（`personalLeaveUsesScheduledHoursAndTotalCannotBecomeNegative`：固定工资 10000、两班请一班 → 断言 `leave_deduction_cent = 5000`）、`:657-675`（`ordinaryPenaltyCapAggregatesEntriesAndExcludesCommissionAndPersonalLeave`：`penalty_cent = 2200`、`total_cent = 12800` 以固定工资为事假基数推得）。
- HTML 原句：BSM L1450「按该员工当期请假工时占当期总工时的比例，从其当期应发薪资中扣减」。
- 判据：GEN-003、BSM-086、BSM-120；依据类型：HTML 原句 + 代码事实。
- 业务影响：两条用例把 U05 定稿冲突项（事假基数取固定工资而非当期应发薪资）锁成绿色断言；发布者按 U05 处置改基数后，这两例必红，若不同批改断言则要么回退修复要么留红。
- 无业务发明理由：断言随 HTML 口径改（示例中应发 = 10000 + 9000 提成 + 1000 奖金 → 事假扣 10000，总额 7800），不新增规则。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：与 U05 事假基数修复同 PR 改这两处期望值；不在本轮修复。

### CX-U11-002 · 技术性 · 发布前须修：是

- 位置：`M010RegistrationHttpTests.java:24`（`POST /api/v1/staff/self-registrations` 带 `X-Terminal-Type: app` 无设备头、无 Authorization，断言 200）；`M010RegressionTests.java:306-319`（`SecurityContextHolder.clearContext()` 后匿名 `selfRegister` 成功落待审）；`:321-330`。
- HTML 原句：PGM L579/BSM L935 私人手机 App 安装时生成并绑定设备标识；PGM L578/BSM L1433 两条注册路径。
- 判据：GEN-003、GEN-004、PGM-032/033；依据类型：HTML 原句 + 代码事实。
- 业务影响：用例把 U01-001/U02-001（客户端自报终端类型放行）与 U05 定稿（公开注册无设备证明/频控）锁成期望行为；按定稿修复后这些用例必红。
- 无业务发明理由：断言改为携带设备证明的注册成功、无设备证明被拒，随定稿处置走。
- 工程缺陷：是。建议登记：是。最小处置：与 U01-001/U02-001/U05 修复同 PR 改这三处（HTTP 用例改发设备签名头或改断言为拒绝；服务层用例注入已验签私人手机身份）；不在本轮修复。

### CX-U11-003 · 技术性（U04-001 已答删除的测试面）· 发布前须修：是

- 位置：`M010RegressionTests.java:321-330`（`selfOperatedMerchantUsesCompanySelfRegistrationAndFlagSurvivesArchive`：`merchant.withSelfOperated(true)…`、断言 `selfOperated()` 为真并按布尔分流）。
- 判据：GEN-003、GEN-017；依据类型：操作员 2.2 答复（questions/U04-001.md）。
- 事实：操作员已答「删除」独立自营布尔；本用例是该布尔在测试层的唯一直接引用。
- 工程缺陷：是。建议登记：是（引用已答复，不再问）。最小处置：改为把商户 `cooperationForm` 置 `self_operated` 后断言走公司自有员工路径，去掉 `withSelfOperated`；与 U03/U04/U05/U08/U10 同批；不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`StaffTestActors.grant` 建 `role_m010_fixture` 并授予全部 `staff.%` 权限 + `governance.supervisor_review.approve`（`:17-18`），拒绝类用例另建无角色用户，隔离性未受影响；夹具范围偏宽。
- OBS-2：用例名/注释以「M010-001/006/017/019/020/029」自编号，与本审计 M010 模块 ID 撞名；文档口径需区分（U10 OBS-2 同）。
- OBS-3：`M010RegressionTests.java:523/:555` 硬编码 `ZoneId.of("Asia/Shanghai")`，其余用 `LocalZone.ID`（U05 OBS-7 同根）；`:286-287/:295-296` 以字面量 `usr_master_authority` 构造主会话，`masterOwnDeletionDoesNotRequireAnotherApprovalStep` 则用 `SessionIssuer.MASTER_USER_ID` 常量，两种写法并存。
- OBS-4：`StaffJdbcIntegrationTests.seedProfile`/`PayrollSafetyJdbcIntegrationTests.seedProfile` 仍向旧列 `phone_hash/id_doc_hash` 写 `sha256:*`（V086 可空，V403 后不再读），夹具陈旧（U10 OBS-1 同根）。
- OBS-5：`M010RegressionTests.java:196-201` 断言重新登记工作手机保留 `staff_id`，但 76 文件内仍无任何把 `staff_id` 写入终端设备的领用路径（U07 定稿），该用例只覆盖「保留」不覆盖「首次分配」。
- OBS-6：`dailyProfileSubmitPermissionCanArchiveWithoutReviewPermission`（`:390-396`）把存档权限放宽（U05 OBS-1）锁成期望。
- OBS-7：本段 7 类在空库实跑 92/92 通过，且 `migrationCreatesStaffThreeTablesAtLatestVersionAndSeedsPermissions` 通过（`flyway_max_version=409` 与类路径最新一致）；用时 §2。
- OBS-8：`M010RegressionTests.java:203-215` `shiftVisibilityUsesEmployerSettingAndDefaultIsExactlySevenDays` 在无任何 `staff_policy_settings` 行时断言可见 7 天，随后自插测试行 `(2,1,14)`——它锁定的是 U05 缺行回落 `(7,null,null)` 的行为（U05-005 / U10-003：V402 未种子可见天数出厂行、BSM L1444「须同时配置上下限」）；若发布者采用「补出厂行 7 + 上下限」则前半段仍通过但不能证明上下限存在，若采用缺行 fail-closed 则该断言须改；随 U05-005/U10-003 处置同批调整，不单独入队。
- 技能：`using-superpowers`、`verification-before-completion`（一次性空库实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。8 文件全文三线审计、52 项判据逐项核对；发现 3 项技术性须修（CX-U11-001 事假基数断言、CX-U11-002 自报终端类型/匿名注册断言、CX-U11-003 自营布尔用例，均为已定稿/已答项的测试面，须与对应修复同批改断言）、8 项观察；无添加项；无 HTML 修改申请；一次性空库实跑 92/92 通过、库已删除、未写任何 complex_test* 库；未修源、未提交/推送。本报告落盘前未读取 `U11-b1-grok-raw.md` 任何正文。
---

# U11 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.64「当前 U11 B2-r1 整批候选」U11-001…009；modules/U11-b1-grok-raw.md V0.01（49927bytes SHA7803e95672af00d12be0d6eeed9293fb508765ab8bd0c33fb1355a36f1dca2e8）；本文件 B1 段（26783bytes SHA16b34ffb0571ec4cc22f6b035bc3f9dd4f9c07803d69c70855d1501a3ef8d79b）。2026-09-10T00:44:54Z
- 席甲 B2 核实（只读，见 ledger V2.74 U11 节）：main `V410:9` `DROP COLUMN IF EXISTS parent_entity_id` 与 `M010RegressionTests.java:334` INSERT 含该列；`StaffJdbcIntegrationTests.java:104` `MigrationVersions.latest()`；`grep -c 'isInstanceOf(ApiException.class)'`=27、`.code()`/`hasMessageContaining` 0；`:94-125` 员工会话+注入 kiosk 验签后才成功；`:657-675` 期望 2200/12800；`:203-228` 缺行断言 7；U05-002/003/004/005/006、U09-004、U10-003 定稿处置与候选一致；questions/U04-001.md 答复「删除」。

## 勘误（席甲 B1 段）

- §4「发布者重排迁移后本段测试不需改动」有误：`:334` 向 `business_entities` 插 `parent_entity_id`，main V410:9 已删该列，席甲空库到 V409 的实跑看不到该冲突；B2 接受 Grok GR-U11-001 为 U11-001。
- 漏判：封顶分母断言 `:657-675`（CX-001 只记事假基数）→ U11-004；柜机双身份用例 `:94-125` → U11-006；27 处无错误码断言 → U11-008；OBS-8 可见天数默认 7 用例升队 → U11-005。
- 分类：席甲把测试面一律记技术性；B2 按 GEN-017 顺序，断言内容与 HTML 原句相反者记冲突（003/004/005/007），与同根 U05 项一致。席甲 B1 票 FAIL 不变，缺陷计数由 3 升为 8。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | V410:9 删列与 :334 插列、:104 latest()、新类型编译红三事实成立；处置只重放/重编号/同 PR |
| 002 | PASS | :321-330 是 U04-001 已答删除布尔的测试面；不再问 |
| 003 | PASS | :637-655 期望 5000/0 按固定工资推得，BSM L1450 明文当期应发；随 U05-002 同批改 |
| 004 | PASS | 2200＝20%×(10000+1000)，BSM L1458 总薪资定义不同；随 U05-004 同批改 |
| 005 | PASS | :212 缺行断言 7；BSM L1444 须同时配置上下限；随 U05-005/U10-003 同批改 |
| 006 | PASS | 双身份注入才绿；PGM L582/BSM L938 柜子自动记录；随 U05-006 补单端用例 |
| 007 | PASS | 无认证无设备证明 200/落待审；PGM L579/BSM L935；随 U05-003 改 4xx/pending |
| 008 | PASS | 27/0 计数属实；U09-004 已要求补错误码断言 |
| 009 | PASS | 观察归并未改写双方原意；§5.9 BSM-004 未映射本段，登记给调度不升队 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 6 条，定稿以此为准）

1. U11-001 事实格「`:27,38`」读作 `M010RegressionTests.java:27,38`（`@Autowired StaffLockerService`/`StaffRegistrationService`），与 `StaffJdbcIntegrationTests.java:104` 分属两文件。
2. U11-002 冻结判据补本段映射内 BSM-028、BSM-085、PGM-026；分类仍为添加（已答删除）。
3. U11-003 冻结判据删 BSM-120（设备/柜机拍板，过宽）；本条 HTML 锚为 BSM L1450 / BSM-086。
4. U11-007 同根补 U01-007/U02-002（无设备标识契约链）；U01-001/U02-001 只覆盖自报类型头，处置含「改发设备签名头」，不得读成只改请求头。
5. U11-009 第 9 条「归 U06/主流程」为调度去向注释，不得读成已由 U06-004（顶班入口）吸收；U06 已 REVIEWED，调度按未映射 BSM-004 另挂删除生命周期。
6. U11-009 所列为抽样转述：Grok §5.4（首次误用 docker 口令失败不计）、§5.18（构造/字段注入）已列；席甲 OBS-5/6 维持观察；定稿不得写成「Grok 无这两条」或升格入队。

## 结论

席甲 B2-r1 票：**PASS**（候选 U11-001…009 成立；发布前须修「是」8 / 「否」1；添加 1（U11-002，操作员已答删除，引用 questions/U04-001.md）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 74758bytes SHA4e6d9d213f818a692da616927c6e604cd0f55c25abd8f99a39976cb440121135，前 49927bytes 原字节不变）。
