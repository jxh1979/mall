CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M011 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-09T15:26:03Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `242f487233d028173cb9198eecd079f5d877405c`；工作区 tree `49e0b97d9d2e86ddbe208b001b38e5f5af3c1736`（含 M009 与 M010 已闭环的 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M011「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段」，3 文件（计划冻结 1342 行；当前含 M009 与 M010 修复期新增测试），映射 87 判据（与 M009 相同集合）。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M011-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/测试/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| PlatformControllerTests.java | 351 | 15915 | cbaec207daf3fc89fd72d42aa3fa42cccbefa7beb2e4eb16294685658cfeeb78 | 全读 |
| PlatformJdbcIntegrationTests.java | 495 | 23380 | 247409ab89bcdc71a2b5aa616c654620d37625ba3993287e94cdbec5fb9dd738 | 全读 |
| PlatformServiceTests.java | 810 | 39637 | 8775e50a0c61c7d4ef48f942e278f707a2a1836e991ca29084bbddd1251c3448 | 全读 |

## 2. 可执行证据原文（/tmp/m011_b1_run.log）

```
START=2026-09-09T15:25:31Z
TREE=49e0b97d9d2e86ddbe208b001b38e5f5af3c1736
=== BACKEND: the three M011 classes ===
BACKEND_EXIT=0
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.148 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.814 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
=== test inventory ===
PlatformControllerTests: @Test=11 lines=351
PlatformJdbcIntegrationTests: @Test=16 lines=495
PlatformServiceTests: @Test=31 lines=810
=== static probes ===
fake remove: 722:            merchants.remove(merchantId);
拒硬删 comment: 706:        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒硬删）。
fixed_rent_plus_commission in JDBC: 5
draft status in JDBC: 5
TREE_AFTER=49e0b97d9d2e86ddbe208b001b38e5f5af3c1736
END=2026-09-09T15:25:37Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文（BSM-004 软删除/存档、BEQR-148 合作方式六类、BEQR-154/155 状态、BSM-072/074/PGM-027/041 作用域、PGM-018/019 主体角色、GEN-003 测试有效性）反推「这三份测试应当证明什么」，逐一对照当前测试实际断言了什么。
- 顺从式线：87 冻结判据逐项（§5；无实现面的判据明确原因）。
- 对抗式线：把每条测试当作可被绕过的证据看——夹具是否走了被测代码的校验（仓储直写 vs 服务层白名单）、内存 fake 是否与 Jdbc 语义一致、测试名声称与断言是否一致、被修复的安全闸（M009-015 七个读端）是否有覆盖、断言是否能失败（对照 M009/M010 B3 红日志）；实跑三套件（§2）。
- 依赖只读追流（不审）：被测的 `PlatformService`/`PlatformController`/`JdbcPlatformRepository`（M009 已闭环）、`support/TestSecurityActors`、`support/InMemory*Repository`、`support/MigrationVersions`、`GlobalExceptionHandler`、`SecurityConfig`、V410 后的 complex_test 实态。
- 3 文件全文逐行读完（PlatformControllerTests 351 行、PlatformJdbcIntegrationTests 495 行、PlatformServiceTests 810 行）。

## 4. 可执行证据（本席实跑）

- E-1 三套件实跑见 §2 原文：ControllerTests 10/10、JdbcIntegrationTests 16/16（含 M009 三批与 M010 一批新增断言）、ServiceTests 31/31；无环境红（complex_test 已重建至 V410，`residualMigration…` 410=410）。
- E-2 静态：内存 fake `deleteMerchant` 仍 `merchants.remove`（PlatformServiceTests:720-723）；注释「>0 拒硬删」（:706）；Controller 夹具 `cooperation_form "fixed_rent_plus_commission"` ×3（:133/:157/:169）；JDBC 夹具 `"fixed_rent_plus_commission"`/`"draft"` 多处（`listMerchantsPagedReturnsRealTotalAndReachesNextPage`、`createUpdateAndListMerchantPersistsFormalFields`、PII 三例）；七个 GET 中仅 `/merchants` 与 `/business-entity-bindings` 断言 `requirePlatformScope`；`…RequiresIdempotencyAndManagePermission` ×2 与 `creatingSpaceUsesGovernanceSpaceManagePermission` 未断言缺幂等键分支；`createMerchantRejectsBusinessFormatOutsideFiveValues` 名称与六值（含 media）不符。

## 5. 87 判据逐项核对（本模块为测试文件，判据落到「测试是否证明 HTML 口径」）

| 判据 | 适用面 | 结论 |
|---|---|---|
| BEQR-063/074/145/146/147/149/150/151/152/153/156/157/158/159/166/189、BSM-005/006/114/115/120、PGM-002–004/007–016/020/021/031/035/040/042–044/060/065/066 | 三份测试无对应实现面或仅间接（PII 脱敏 BEQR-152/PGM-060 已有 `listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 覆盖） | 不适用/通过 |
| BEQR-143 四类主体 | 绑定唯一/存在性/激活投影用例 | 通过 |
| BEQR-144/PGM-018/019 | 主体角色三值用例；夹具已改 management_company | 通过 |
| BEQR-148 合作方式六类 | 服务用例拒 `fixed_rent_plus_commission`，但 Controller/JDBC 夹具仍把该退役码当合法值 | CX-M011-02 |
| BEQR-154/155 状态 | 服务用例拒 `draft`/`zzz`；JDBC 夹具仍直写 `draft` 商户状态 | CX-M011-02 |
| BSM-003/004 存档与删除 | 内存 fake 物理 remove 使单测断言「审批删除后 includeArchived 列表也不见」——与 Jdbc/HTML「删除只隐藏不抹除、可查可恢复」相反 | CX-M011-01 |
| BSM-007 最高权限 | `asMasterAuthority`/非最高权限拒批用例 | 通过 |
| BSM-072/074、PGM-027/041 作用域 | 七个读端只测两个 | CX-M011-03 |
| BSM-001/009/028/029 | 建档/绑定/空间用例 | 通过 |
| GEN-001 | 无死测试；`TestBeans` 均被上下文使用 | 通过 |
| GEN-002 | 三套件绿 | 通过 |
| GEN-003 测试有效性 | 01（断言错误行为）、03（关键闸覆盖缺口）、04（名称声称未断言） | CX-M011-01/03/04 |
| GEN-004/005 | 权限码/作用域/幂等键用例存在但不完整 | CX-M011-03/04 |
| GEN-006/007 | 唯一 active 绑定、幂等去重用例 | 通过 |
| GEN-008 接口一致性 | 夹具值与服务层/迁移枚举不一致 | CX-M011-02 |
| GEN-009 | 400/403/404/409 断言 | 通过 |
| GEN-010 | 测试 `AuthService("", "")` 空密钥仅切片上下文；无真实秘密 | 通过（OBS-03） |
| GEN-011 | 审计事件断言 | 通过 |
| GEN-012 | 501 条空间用例有界 | 通过 |
| GEN-013 | `@SpringBootTest` 硬编码 `complex_test`/用户名 `complex`，密码取环境变量 | OBS-01 |
| GEN-014 | `residualMigration…` 动态取 latest() | 通过 |
| GEN-015 | 无界面 | 不适用 |
| GEN-016 文档可验证 | 注释「拒硬删」、名称「FiveValues」 | CX-M011-05/04 |
| GEN-017/018 | 本报告分类与证据 | 通过 |
| PGM-001 全中文 | 测试断言的中文文案与内部码 | 通过 |
| PGM-005/006/026/048 | 空间/商户/绑定用例 | 通过 |

## 6. Findings（临时编号 CX-M011-，正式 ID 在 B2 分配）

### CX-M011-01 内存 fake 以物理 remove 模拟删除，单测把「审批删除后 includeArchived 列表不可见」锁成绿，与 Jdbc 软删除及 BSM-004 相反
- 位置：`PlatformServiceTests.java:720-723`（`deleteMerchant` → `merchants.remove`）；`approveDeleteRemovesArchivedMerchantWhenNoReferences:374-375` 断言 `listMerchants(true)` 不含该商户。
- 事实：M009-010 已把 Jdbc `deleteMerchant` 定为 `UPDATE delete_status='approved'`，`listMerchants(true)` 含 approved 行（JDBC 用例 `approvedDeletedMerchantStaysQueryableWithIncludeArchived` 绿）；单测却断言相反行为，靠 fake 的物理 remove 通过——测试证明的是被 HTML 否定的语义（M009-010 已登记「分叉归 M011」）。
- 分类：技术性（测试双子与生产语义不一致）；依据类型 2+4；判据 GEN-003/GEN-008/BSM-004；工程缺陷：是。
- 建议入队：是。最小处置：fake `deleteMerchant` 改为把该商户 `withDelete("approved", …)` 写回（与 Jdbc 同语义）；断言改为「默认列表不含、includeArchived 含且 deleteStatus=approved」；注释「拒硬删」同步（见 05）。

### CX-M011-02 夹具沿用已退役/不在闭集的业务码：`fixed_rent_plus_commission`、商户 `draft`
- 位置：`PlatformControllerTests.java:133/157/169`（mock 返回 `cooperation_form "fixed_rent_plus_commission"` 并断言其原样透传）；`PlatformJdbcIntegrationTests.java` 商户夹具 `"fixed_rent_plus_commission"`/`"draft"`（`listMerchantsPaged…`、`createUpdateAndListMerchant…`、PII 三例）。
- 事实：M009-003/004 后服务层只接受六类合作方式与五态商户状态；仓储直写与 mock 绕过校验，夹具继续把 HTML 之外的码当合法数据（GEN-008）。JDBC 夹具中 `entity_role "operation"` 已随 M010-003 改，本项是同模式的残留。
- 分类：技术性；依据类型 1（BEQR-148/155）+4；判据 GEN-008/GEN-003；工程缺陷：是。
- 建议入队：是。最小处置：合作方式改 `lease`（或六类之一）、商户状态改 `active`/`pending_admission`；Controller 断言值随改；不改被测代码。

### CX-M011-03 M009-015 七个治理读端的平台作用域闸只有两个有测试
- 位置：`PlatformControllerTests.java:213-255`（`/business-entity-bindings`、`/merchants`）；`/business-entities`、`/projects`、`/spaces`、`/space-types`、`/venue-business-hours` 无 `requirePlatformScope` 断言。
- 事实：作用域闸是 fail-closed 安全边界（BSM-072/074/PGM-041），单元层只锁了 2/7；任一读端被回退到只 `requirePermission` 时无测试能红。
- 分类：细化（关键分支覆盖缺口）；依据类型 4；判据 GEN-003/GEN-005；工程缺陷：是。
- 建议入队：是。最小处置：一条参数化用例遍历七个 GET，`requirePlatformScope` 抛 403 时各返回 `PLATFORM_SCOPE_REQUIRED`（复用现有 doThrow 形态）。

### CX-M011-04 测试名声称的行为未被断言
- 位置：`createBusinessEntityBindingRequiresIdempotencyAndManagePermission:258-292`、`changeBusinessEntityBindingRequiresIdempotencyAndManagePermission:295-325`（只 verify 权限码，不测缺幂等键 400）；`createMerchantRejectsBusinessFormatOutsideFiveValues:323`（业态六值含 media）。
- 分类：技术性；依据类型 4；判据 GEN-003/GEN-016；工程缺陷：是（名称是测试文档，与断言不符）。
- 建议入队：是。最小处置：两条绑定用例各补一次「无 idempotency_key → 400 VALIDATION_FAILED」请求；`FiveValues` 改 `SixValues`。

### CX-M011-05 注释「>0 拒硬删」（M009-026 扫尾）
- 位置：`PlatformServiceTests.java:706`。分类：技术性；判据 GEN-016；工程缺陷：是（与 01 同批一行）。建议入队：是（随 01）。

## 7. 观察（登记、不入队）

- OBS-01：`@SpringBootTest(properties=…)` 硬编码 `jdbc:postgresql://127.0.0.1:5432/complex_test` 与用户名 `complex`、密码取 `COMPLEX_DATABASE_PASSWORD`——与仓库其它 JDBC 测试一致的项目约定（GEN-013 环境耦合），不在本模块单独改。
- OBS-02：`unauthenticatedPlatformListReturnsUnauthorizedOrForbidden` 接受 401 或 403（宽松断言）；安全过滤器语义属 access 模块，登记。
- OBS-03：`TestBeans.authService()` 用空字符串密钥构造，仅切片上下文占位；无真实秘密（GEN-010 通过）。
- OBS-04：JDBC 测试类 `@Transactional` 回滚，V410 后在新库上 16/16；`PlatformJdbcIntegrationTests` 现含 M009/M010 多批断言（文件 V0.03）。
- OBS-05：`seedMerchant` 直接以 `be_1` 作三主体列而不建主体行（M010-030 口径：创建不校验三列存在），测试口径与定稿一致，不判缺陷。
- OBS-06：§2 静态清点 PlatformControllerTests「@Test=11」含 L328 `@TestConfiguration` 一处的字面命中，实际 @Test 用例 10 条，与 Surefire 10/10 一致（清点方法误差，非缺陷）。

## 8. 结论

审计结论：**FAIL**（B1 修复前）。5 条 CX 均有工程缺陷（建议入队 5；均为测试文件内最小改动，不触及被测源码）；0 条添加；5 条观察。三份 HTML 本身未发现需上报的问题。本席 B1 未读取 Grok raw；本报告落盘后才读取并进入 B2 归并。

## M011 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M011-b1-self-raw.md` V0.01 前缀 13404 bytes SHA256 4ea215f5605d66177c036e113d3032b2d2587cd4b5a66dd2c02a609c0eff4313；`modules/M011-b1-grok-raw.md` V0.01 前缀 43085 bytes SHA256 ce463f1f601d4bad08eb2d535b7aa2c5df1bdf905b5ee5d2142db9759997871c（Grok B1：FAIL，GR-M011-001…005，OBS-01…12）；候选 `findings.md` V1.34 508207 bytes SHA256 6ca8f0a61fccf2d0fadd1fb20683b73fc388760a57469c670d5ef6797d084d2c「当前 M011 B2-r1 整批候选」M011-001…007。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。按 `receiving-code-review` 收窄核实 Grok B1 各项：GR-003（无取消存档/驳回/理由/HTTP 存档删除测试）经 grep 核实（三文件 `unarchive`/`"rejected"`/`asMerchantUser` 0 命中；`setMerchantArchiveReason` 空实现 :685；`PlatformService.unarchiveMerchant` :220 与 `approveDeleteMerchant` rejected 分支 :323 存在）；GR-002 的 `revenue_share`（JDBC :164/:201）经 grep 核实；OBS-09（`createSpaceType` 无 `requireIdempotencyKey`，PlatformController :156-160）经 grep 核实为唯一缺键写入口；GR-001 分类由本席 B1 的「技术性」改判为「冲突」——Grok 引 BSM-004:571 原句成立，测试把 HTML 否定的抹除语义锁为期望，位次靠前者优先。
- 差集：Grok GR-001…005 → 001/002/003/004/005；OBS-09 → 006；其余 OBS → 007。席甲 CX-01/05 → 001、CX-02 → 002、CX-03 → 004、CX-04 → 005、OBS-01…06 → 007。无遗漏、无改写 Grok 原意；005 由 Grok 的「登记否」改为「是（随批一行改名+两处补断言）」，理由：席甲 CX-04 另含两条 `…RequiresIdempotencyAndManagePermission` 名称声称未断言（GEN-003 实缺口，非纯文案），与改名同批最懒。

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（冲突／是） | fake 物理 remove + 反向断言把「抹除」锁成规格，与 JDBC 软删及 BSM-004 原句相反；最小处置只动 fake 与该用例断言与注释，不改 M009 源码；TDD 先红后绿可执行 |
| 002 | PASS（细化／是） | 夹具用服务层已判非法的退役码；库无 CHECK 故永绿；改为六类之一与合法状态即可，不发明业务 |
| 003 | PASS（细化／是） | BSM-004 三句（取消存档／可恢复／理由必填）在本模块零覆盖；服务与控制器各补最小可失败断言，fake 补 `setMerchantArchiveReason` |
| 004 | PASS（细化／是） | 服务层 31 用例全为平台管理员、控制器只锁 2/7 读端；一条 merchant actor 负例 + 一条参数化读端负例即最懒 |
| 005 | PASS（技术性／是，随批） | 名称/注释与六值不符；两条幂等用例名称声称未断言；无业务发明 |
| 006 | PASS（跨模块观察／否） | M009 源码缺键属已闭环模块，登记不修 |
| 007 | PASS（否） | 与 B1 两席观察一致，无本模块可修缺陷 |

本席 B2-r1 票：**PASS**（队列是＝001、002、003、004、005；否＝006、007）。B3-r1 边界：只改 `PlatformServiceTests` / `PlatformJdbcIntegrationTests` / `PlatformControllerTests` 三个测试文件（各加版本头），不改被测 M009 源码、三份 HTML、锁、迁移、权限种子；TDD：001 先改断言见红再改 fake 见绿，003/004/005 新断言先红（对照被测行为为真则以「删掉被测行为的等价物」的方式证明可失败，见 B3 记录）。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。

## M011 B2-r2 修订候选复核（席甲票）
版本：V0.03（本文件自身；V0.01 B1 正文与 V0.02 B2-r1 追加段原字节保留，本段追加）

- 输入：`modules/M011-b1-grok-raw.md` V0.02 前缀 61599 bytes SHA256 b72bf605c43128ab443c1c1bd98a4163d039d6e87d4edfdff06ab682b8c9f748（Grok B1 FAIL + B2-r1 FAIL：唯一否决点 005 并入；003/006/007 修正意见）；本文件 V0.02 前缀 17188 bytes SHA256 84980b2d44d95d0e2e2dceff66fa18616324ecd2e0980d214616929d5389b208；修订候选 `findings.md` V1.35 511341 bytes SHA256 c9ab33717d04ba79b9475485d9812c6c52b8691b8d2c3ad41846acc70dceae88「B2-r2 修订候选」。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r2 真实退出后、读取其追加段之前落盘。
- 按 `receiving-code-review` 逐条核实 Grok B2-r1 意见后的处理：(1) 005 并入 CX-04 缺键请求——核实 `requireIdempotencyKey` 为控制器共享闸且 `creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed` 已锁 400 VALIDATION_FAILED，再按 verb 各补一条与 004「不为每个 verb 各写一条」及铁律 6 相悖：接受，撤回并入；改为两条绑定用例方法名去掉 Idempotency 字样（GEN-016），缺键事实归 007。(2) 003 不为 archive_reason 造 fake——核实 `MerchantRecord` 无 archiveReason 字段、`JdbcPlatformRepository.setMerchantArchiveReason` 只写库列不映射回记录：接受，改为 deleteReason 断言（走既有 `withDelete`）。(3) 006 缺陷口径按 M009-022 为否：接受。(4) 007 补席甲 OBS-02…05 事实：接受。

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（冲突／是） | 口径不变 |
| 002 | PASS（细化／是） | 口径不变，含 :164/:201 revenue_share |
| 003 | PASS（细化／是，处置已收窄） | 三条服务断言 + 一条 HTTP 用例；不扩 fake、不改 record |
| 004 | PASS（细化／是） | 口径不变 |
| 005 | PASS（技术性／是，仅改名） | SixValues + 去 Idempotency 字样；不新写测试 |
| 006 | PASS（跨模块观察／否，缺陷否） | 按 M009-022 |
| 007 | PASS（否） | 含 CX-04 缺键与席甲 OBS-02…05 |

本席 B2-r2 票：**PASS**（队列是＝001、002、003、004、005；否＝006、007）。B3-r1 边界：只改三个测试文件（各版本递增）；TDD：001 先改断言见红再改 fake 见绿；003/004 新增用例首跑即绿属覆盖补齐，其断言指向真实行为（回退被测行为即红）；005 只改名。不以本席票覆盖外审。
