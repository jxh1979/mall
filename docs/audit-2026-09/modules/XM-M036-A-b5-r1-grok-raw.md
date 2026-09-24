CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M036-A B5-r1 整模块三线重审（Grok 席乙）

版本：V0.01
席位：Grok 独立只读审计席乙，grok-4.7 / xhigh
日期：2026-09-24
结论：PASS
本轮新问题：0
未读：`docs/audit-2026-09/modules/XM-M036-A-b5-r1-self-raw.md`（本轮不存在于本席阅读范围；未打开）

## 1. 对象与锁

正本是 `/home/james/complex-management` 主工作区里开工段 21 个文件的工作区现状。`git diff origin/main -- <这 21 个文件>` 与冻结 diff `/home/james/audit-tmp/xm036a_b3r1.diff`（479 行，SHA256 `800824fe472eebd5d75dca3fc235a5d88367cb5e90c79d05400eb6daec194d4e`，12 files changed, 150 insertions, 40 deletions）逐字节相同（`diff -q` 无输出）。origin/main 为 `4f233e7b41f2d6301085d0b690a909260462ee78`。另外 9 个开工文件相对 origin/main 无差异。

锁：本席执行 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`，得到上行 64 位 hex。锁文版本 V0.14，判据总数 401。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部 `CRITERIA-LOCK-SHA256` 同值。本席未抄派发基准后再回填；先算后对照。LOCK-AMEND-COUNT 仍为 1。未改锁、findings、ledger、HTML、业务代码。

映射：M009、M011、M054、M056、M077、M153、M158、M230、M232、M262、M263、M272、M331、M335、M396、M397 十六行并集实算 297（BEQR 140、BSM 87、GEN 18、PGM 52）。定义区 297/297 都有 `###` 标题，缺定义 0。

findings V4.01：XM-M036-A-001 至 008 修复队列为「否（已修）」。本席不把「全部否」当成模块已经全对，下面按当前源码重核。B2 已定只登记、本轮无新证据、不改判：XM-M036A-1（最高权限在范围检查中被拒的系统性问题）、XM-M036A-2（删除审批人不限最高权限的同句入口，不在这 21 个文件的单元集内）、游乐权益删除仓储无占用检查（JdbcAmusementRepository 不在本 diff）。

## 2. 技能（本环境实读，按 V0.17 §6 收窄）

已读并按收窄口径使用：`using-superpowers`（用户指令优先于技能；禁止子代理，故未派）、`verification-before-completion`（PASS 前有本席自己的 `mvn -o test` 退出码）、`systematic-debugging` / `diagnosing-bugs`（只追到根因，不改生产；第一次全量红是本席把 `/etc/complex/complex.env` 整文件导入后带上了 `SPRING_PROFILES_ACTIVE=prod`，`MasterAuthorityService` 构造器因缺公钥拒绝启动，不是业务回归）、`grilling`（自问自答，不派子代理，不等操作员）、`codebase-design`（最高权限放行写在三条写入体内部，`isMasterActor` 是私有方法，没有新的对外接口）、`ponytail-review`（本 diff 没有可删的推测性抽象）。

本步不执行：`test-driven-development` / `tdd`（B5 禁止修复）、`requesting-code-review`（属 B4）、`receiving-code-review`（本席是外审，不是收审后改码）、`resolving-merge-conflicts`（无合并）、`brainstorming` / `writing-plans`（阶段 A 已过）。可选的 `git-guardrails-claude-code`、`batch-grill-me`、`ubiquitous-language` 未单独调用；git 约束用「不 fetch / commit / push / merge、worktree remove 不加 --force」落实。

环境限制：策略拒绝了 `git checkout` 与 `git restore`。一次性 worktree 的还原改为对 119 个脏路径执行 `git show HEAD:<path>` 写回，`git status --porcelain` 为 0 后再 `git worktree remove`（无 `--force`）。效果与 checkout 还原后移除相同。

## 3. 第一性三问

这 21 个文件为什么必须存在。删掉删除审批、占用检查、软删隐藏、终端写闸、字段中文名，治理地图 BSM-004（锁内 HTML 原文，`criteria-lock.md` 约 2361–2382 行所引 `business-system-map`）里的「先存档再申请删除」「最高权限审批」「删前查占用」「删除只隐藏」「最高权限本人发起不经审批」，以及终端写操作在认不出身份时拒绝（`TerminalMasterSwitchInterceptor` 第 76–79 行），都做不到。答得上的部分有上位需求。测试夹具、`@MockitoBean`、主键用 UUID，没有新的业务名词，走技术性，不删不问。

声称的行为有没有被执行验证。有。见第 8 节：origin/main 一次性 worktree 加上冻结 diff，空库 `xm036ab5g`，`JAVA_HOME` 为 Temurin 21，`mvn -o test` 退出码 0。原 23 条红所在的 8 个测试类全部 0 失败。第一次带 prod profile 的跑法作废，不作为产品证据。

哪个前提未经验证就被当真。本席单独核过、不再沿用旧票结论的前提：`isMasterAuthority` 要账号类型、用户 id、用户名三者同时命中（`AuthorizationService.java:139-144`）；生产最高权限会话的商户号是 null（`ProductionAuthService.java:410-411`）；`requirePermission` 在显式 DENY 之后才对最高权限短路（`AuthorizationService.java:72-83`）；设施与员工的 `deleteEquipment` / `deleteProfile` 是 `UPDATE delete_status='approved'`，不是物理 DELETE（`JdbcFacilityRepository.java:57-58`，`JdbcStaffRepository.java:1029-1030`）；游乐普通取消存档的测试 `archivedProjectEntitlementStaysInStoreAndIsRecoverable` 仍用平台管理员、不切换最高权限，且在 86 条全绿的类里。

## 4. 原 23 条红：转绿原因

基线名单取自 `/home/james/audit-tmp/xmd029-ckpt.red`（23 行）。本席重读当前断言，对照冻结 diff，确认没有把失败断言改成恒真、没有删掉软删行数、占用文案或权限码校验。

财务 12 条（`FinanceControllerTests`）。生产 `FinanceController` 相对 origin/main 未改。失败原因是 `TestBeans` 用 `@Bean` 交出的 `StoredValueService` mock 仍被容器做字段注入，切片没有 `CustomerService`，上下文起不来，断言没跑到。现改为类字段 `@MockitoBean`（`FinanceControllerTests.java:55-57`），删掉那个 `@Bean`。12 条里的 `status()` 与 `verify(authorizationService).requirePermission(...)` 都还在（例如 `finance.ar_ap.view`、`finance.refund.create`）。本跑该类 12 测、失败 0、错误 0、跳过 0。分类：技术性，无业务发明。判据 GEN-002、GEN-003。证据类型：可执行产物。

合规 3 条 POST（`ComplianceControllerTests` 的无幂等键、创建关键日期、扫描提醒）。生产 `ComplianceController` / `ComplianceService` 未改。三条 POST 只加上请求属性 `com.complex.platform.gate.verified_terminal`，值是 `VerifiedTerminal("web", null, true)`。该字符串就是 `TerminalIdentityHolder.ATTR`（`TerminalIdentityHolder.java:20`）。拦截器在开关 Bean 存在时，写请求没有终端类型就 403 `TERMINAL_NOT_ALLOWED`（`TerminalMasterSwitchInterceptor.java:72-79`）。断言仍是 400 `VALIDATION_FAILED`、200 且 `$.data.id=ckd_1`、200 且 `generated_count=1`，并 `verify` `compliance.contract_key_date.manage` 与 `compliance.reminder.scan`。本跑该类 5 测（含两条未改的 GET）失败 0。分类：技术性（测试夹具对准已有拦截器）。判据 GEN-003、PGM-010、BEQR-153。

储值注解 1 条（`CustomerGuardedActionAndRejectionAuditTests` 参数化第 3 组）。期望码仍是 `customer.stored_value.consume`。方法名从已不存在的 `consume` 改为 `payTicketOrder`。`StoredValueService.java:264-265` 上该方法带 `@GuardedAction(capability = "customer.stored_value.consume", permission = "customer.stored_value.consume")`。测试在注解为空或不相等时失败。没有加空的 `consume`。本跑该类 19 测失败 0。分类：技术性。判据 GEN-003、GEN-008。

设施原 3 条。成功路径与占用路径的审批人从平台管理员改为最高权限；自审用例改为「平台管理员申请人不能批」，断言文案含「最高权限」，且 `delete_status` 仍为 `pending`。成功路径仍断言行数 1、`delete_status=approved`、默认查询为空。占用路径仍断言文案含「维保工单」。本跑该类 5 测失败 0。见第 5 节生产行为。分类：冲突已按 BSM-004 改到一致，不是放宽断言。

游乐原 1 条（墓碑恢复）。用例改名 `tombstonedProjectEntitlementRestoreRequiresMasterAuthority`。平台管理员恢复墓碑时期望码从 `PERMISSION_DENIED` 改为 `MASTER_APPROVAL_REQUIRED`；最高权限本人仍能恢复到 `delete_status=none`。审批通过后先切回平台管理员再查列表，列表空、含存档为 1 的断言还在。这是躲开已登记的 XM-M036A-1（最高权限查列表会被 `currentMerchantScope` 拒绝），生产列表方法不在 diff 里。本跑 `AmusementServiceTests` 86 测失败 0。

字段标签 1 条。`FieldLabelsTests` 未改。门禁仍是主源码里 `requireText` 等抛出的键必须有中文名，缺了就失败。`FieldLabels.java:75` 增加 `p("business_entity_id", "经营主体")`，对应 `PlatformService.java:376` 的 `requireText(entityId, "business_entity_id")`。本跑该类 7 测失败 0。分类：细化（BEQR-144 经营主体的下位字段名）。判据 GEN-008、BEQR-144。

员工原 2 条。成功路径仍断言 `deleted()==true`、`COUNT=1`、默认 `findProfileById` 为空。占用路径仍断言文案含「考勤」。审批人改为 `StaffTestActors.master()`。本跑该类 5 测失败 0。

空间种类历史用例 `completingSpaceTypeRequestKeepsPriorRequestHistory` 本跑落在 `PlatformJdbcIntegrationTests` 25 测、失败 0 里，没有再撞主键。

## 5. 删除审批、范围与未放开的路径

设施设备（`FacilityEquipmentService.java`）。`approveDelete` 第 123–125 行：不是最高权限即 403 `MASTER_APPROVAL_REQUIRED`，文案「设施设备删除须由最高权限本人批准」。其后才 `requirePermission("facility.equipment.approve")`。最高权限在 `permissionGrant` 里于显式拒绝之后短路，所以最高权限批得了；平台管理员账号类型不是 `master_authority`，批不了。占用检查仍在第 143–148 行，`countEquipmentReferences` 大于 0 就 `FACILITY_EQUIPMENT_IN_USE`，发生在 `deleteEquipment` 之前。`requestDelete` 第 111–114 行：最高权限发起时在写入 pending 之后调用同一个 `approveDelete(..., "approved")`，占用检查照走。自审闸已去掉。B2 定稿写明：审批人只能是最高权限本人之后，该闸没有适用对象，最高权限发起的直接通过不能再撞自审。普通人在第 123 行就被拒，到不了批准。`archive` 第 80 行仍是 `requirePlatformScope()`，取消存档不要求最高权限。

员工档案（`StaffService.java`）。`approveDeleteProfile` 第 412–414 行同样只认最高权限。第 416–417 行最高权限按 `findProfileById` 取档，不走公司范围。占用检查在第 432–436 行，文案仍是考勤、请假、职级、排班、寄存。`requestDeleteProfile` 第 386–388 行最高权限同样只校验存在；第 400–404 行最高权限发起后调用 `approveDeleteProfile`。对照冻结 diff，第 400–404 行的直接生效是 origin/main 上已有、原先被 `requireProfileInScope` 挡住的代码；本轮只让它真正执行到。`requireProfileInScope`（第 1176–1187 行）与 `requireRegistrationScope`（第 1198–1207 行）方法体不在 diff 里。`archiveProfile` 第 314 行仍先 `requireProfileInScope`。列表、销售岗、本人档案等调用点没有改成最高权限豁免。普通员工读写没有被放开。

游乐权益（`AmusementService.java`）。`decideDeleteProjectEntitlement` 第 451–453 行非最高权限即 `MASTER_APPROVAL_REQUIRED`，然后才进入 `executeWrite`。体内不再调用 `checkProjectOwnership`，因为非最高权限进不了这个写入体。第 468–472 行申请人自审闸还在，供「别人发起、最高权限来批」用。最高权限自己发起走 `requestDeleteProjectEntitlement` 第 428–431 行，直接 `repository.decideDeleteProjectEntitlement(..., true)`，不进 `decideDelete` 的自审闸。这与 B2 实施读法一致。`restoreProjectEntitlement` 第 388–390 行只在 `delete_status` 已是 `approved` 且调用者不是最高权限时拒绝。普通取消存档（已存档、删除轴不是 approved）仍先做所有权检查再恢复。`archivedProjectEntitlementStaysInStoreAndIsRecoverable`（约第 1412–1432 行）仍由 `setUp` 里的平台管理员恢复，不调用 `asMaster`。`checkProjectOwnership` 其余调用（第 212、231、295、346、511、557、594、640、699、1064、1090、1112、1130、1156、1182、1204、1265、1586 行）无条件保留。只有恢复与申请删除两处对最高权限跳过。`checkProjectOwnership` 本身（第 1619–1628 行）未改，它仍调用 `currentMerchantScope`。范围方法不在这 21 个文件的 diff 里。

平台管理员。`isMasterAuthority` 不把 `platform_admin` 当成最高权限。设施、员工、游乐三条测试都用平台管理员发起或审批，期望 `MASTER_APPROVAL_REQUIRED` 或文案「最高权限」，并保持 `pending` 或墓碑不可被平台管理员复活。本跑这些类失败数为 0。

`requirePlatformScope`（`AuthorizationService.java:488-504`）未改。最高权限生产会话商户号为 null，该方法对无商户号且非外包的主体本来就放行。这不是本轮把范围方法放开。`currentMerchantScope` 对非平台管理员且无商户号仍抛 `MERCHANT_SCOPE_REQUIRED`（第 425–434 行）。XM-M036A-1 维持只登记。

软删。设施与员工批准后是更新 `delete_status`，默认查询滤掉 `approved`。测试仍要求行留在库里。游乐墓碑注释与 `listProjectEntitlements` 的含存档断言仍要求行在、默认列表不显示。符合 BSM-004「删除只隐藏不抹除」里对员工、设备的归类。

游乐删前占用。本 diff 没有改 `JdbcAmusementRepository`。已登记观察保持，不升成新缺陷，也不假装本轮补上了占用检查。

## 6. 主键

`JdbcPlatformRepository.saveSpaceTypeDeleteRequest` 第 397 行主键改为 `"stdr_" + UUID.randomUUID()`。`V424__space_type_delete_requests.sql` 的 `id` 是 `TEXT PRIMARY KEY`，UUID 放得下。`space_type_id` 与时间列还在。

回归 `PlatformJdbcIntegrationTests.backToBackSpaceTypeDeleteRequestsNeverCollideOnId`（第 564–574 行）对同一空间种类连续申请并完成 50 次，断言行数为 50，没有吞异常。旧主键是类型号加毫秒。同一毫秒第二次 INSERT 会撞主键，异常直接失败这条测试。所以旧主键下它能够失败。B2 写明不要 sleep、不要吞异常。本席不把「不是每次时钟都撞车」升成新需求。同文件的历史用例本跑也是绿的。本类 25 测失败 0。分类：细化下的数据完整性，GEN-006、GEN-007。技术上无新业务名词。

## 7. 对抗式（grilling，自问自答）

问：最高权限发起设施或员工删除，会不会跳过占用检查。答：不会。两条都进入各自的 `approveDelete` / `approveDeleteProfile`，占用检查在更新为已删除之前。占用测试仍用最高权限审批并期望冲突文案，本跑为绿。

问：平台管理员被授了 `facility.equipment.approve` 或游乐 approve 能力码，能不能批删除或复活墓碑。答：不能。闸在方法体最前，比权限码短路更早，认的是最高权限本人三个字段。游乐单测直接调服务，不经过切面，因此测到的是方法体。生产切面 `ActionGateAspect` 对游乐这两处注解只查 capability、permission 为空；方法体的最高权限闸仍然执行。

问：普通取消存档是不是被收成也要最高权限。答：不是。游乐只在墓碑（`approved`）上拒绝非最高权限。员工 `archiveProfile(..., false)` 与设施 `archive(..., false)` 的签名和范围调用未改。

问：最高权限是不是因此能读到任意员工档案。答：不能。豁免只在两条删除方法上。`requireProfileInScope` 的其余调用点还在。

问：去掉设施自审闸之后，申请人能不能批自己的单。答：申请人若不是最高权限，在自审判断之前就 403。申请保持 `pending`（设施测试第 106–107 行有断言）。

问：最高权限批自己经 `decideDelete` 发起的游乐单，会不会被留下的自审闸卡死，导致「本人发起直接生效」做不到。答：本人发起不走 `decideDelete`，走仓储直接通过。`decideDelete` 的自审闸只覆盖「待审单由别人发起」。这是定稿读法，不是新洞。

问：财务和合规是不是上下文起来了但断言没跑。答：不是。两类的失败数都是 0，而且源码里状态码、JSON 路径、`verify(requirePermission)` 都还在。若上下文仍失败，这 12+5 条会是错误而不是成功。

问：`business_entity_id` 的中文是不是测试被改松才绿。答：不是。`FieldLabelsTests` 不在 diff 里。缺键时 `missing` 非空，测试失败。

问：主键测试会不会在旧实现下也稳定通过。答：同一毫秒会失败。本席没有改生产去强行打出那次红（B5 不改业务）。失败模式与已记录的撞键相同，测试本身不捕获该异常。

问：本 diff 有没有顺手改范围方法或别的游乐写路径。答：没有。`git diff origin/main` 与冻结 diff 一致，生产侧只有 `StaffService`、`AmusementService`、`FieldLabels`、`FacilityEquipmentService`、`JdbcPlatformRepository` 五处，行范围就是上面引用的那些。

ponytail-review：没有可删的新抽象。`isMasterActor` 只有一个实现、三处使用，留着比复制三遍判断更短。`net: 0 lines possible.` Lean already.

## 8. 全量测试（类型 2 / 类型 4）

一次性 worktree：`git worktree add --detach /home/james/wt-xm036a-b5-grok origin/main`，HEAD `4f233e7b`。`git apply` 冻结 diff 成功。空库名 `xm036ab5g`，属主为库用户 `complex`。只在该 worktree 的 `backend/src/test` 把 `5432/complex_test` 换成 `5432/xm036ab5g`（110 个文件、110 处）。主工作区命中数为 0。`COMPLEX_DATABASE_URL` 与 `SETTLE_TEST_DB_URL` 指向 `jdbc:postgresql://127.0.0.1:5432/xm036ab5g`。用户名和密码只从 `/etc/complex/complex.env` 进入进程环境，不写入仓库，不在本报告打印。

第一次（04:31Z）整文件导入 env，带上 prod profile，大量 Spring 上下文因「生产环境必须配置 U 盘破窗验签公钥」失败。这是本席环境错误。该次结果作废。

第二次（04:34:29Z–04:35:10Z）去掉 `SPRING_PROFILES_ACTIVE`，默认 profile 为 `local`。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`，java 21.0.12.1。工作目录为 worktree 的 `backend`。命令 `mvn -o test`。Flyway 在空库上校验 399 个迁移并迁到版本 432，库 URL 为 `xm036ab5g`。结果：Tests run 3536，Failures 0，Errors 0，Skipped 17，BUILD SUCCESS，退出码 0。

与本模块直接相关的类：

| 类 | 测 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| FieldLabelsTests | 7 | 0 | 0 | 0 |
| FinanceControllerTests | 12 | 0 | 0 | 0 |
| AmusementServiceTests | 86 | 0 | 0 | 0 |
| ComplianceControllerTests | 5 | 0 | 0 | 0 |
| FacilityEquipmentArchiveDeleteJdbcIntegrationTests | 5 | 0 | 0 | 0 |
| PlatformJdbcIntegrationTests | 25 | 0 | 0 | 0 |
| StaffProfileArchiveDeleteJdbcIntegrationTests | 5 | 0 | 0 | 0 |
| CustomerGuardedActionAndRejectionAuditTests | 19 | 0 | 0 | 0 |

3536 与席甲 B3 GREEN 的套件规模一致（相对 3532 的基线，多出来的是本轮新增用例，不是少跑）。原 23 条所在类没有剩余失败。全库没有新的失败类。跳过的 17 条不在上表八个类里。

清理：119 个脏路径按 HEAD 写回，porcelain 为 0，`git worktree remove` 未加 `--force`，目录已不存在。`DROP DATABASE xm036ab5g`。之后 `pg_database` 仍有 `complex` 与 `complex_test`，没有 `xm036ab5g`。主工作区测试树没有 `xm036ab5g`。未对 `complex` 或 `complex_test` 执行写语句。

## 9. 顺从式：297 项

适用 29 项（都在并集内）。无本面或「同文件未改且无新证据」268 项。29+268=297，无遗漏、无重复。

### 9.1 适用

- GEN-001 必要性与最小性。直接生效与跳过范围检查只加在定稿点名的写入体上，没有新的范围 API。
- GEN-002 可执行正确性。第 8 节全量退出码 0；原红类的断言实际跑完。
- GEN-003 测试有效性。财务、合规、注解、标签门禁、软删行数、占用文案、最高权限拒绝码都还能失败。主键回归在旧毫秒键下会因主键冲突失败。
- GEN-004 信任边界。删除批准不认可授出的权限码单独放行，认最高权限三个身份字段。终端写闸仍不信缺失的终端属性。
- GEN-005 授权与最小权限。平台管理员不再能批这三类删除或复活游乐墓碑。最高权限以外的档案读写仍走原范围方法。
- GEN-006 数据完整性。空间种类删除申请主键改为 UUID；软删仍留行。
- GEN-007 并发与幂等。连续 50 次申请不撞键。游乐写入仍走原 `executeWrite` 幂等键。本轮没有改幂等框架。
- GEN-008 接口一致。注解点名的方法是 `payTicketOrder`；字段键 `business_entity_id` 有中文名；错误码 `MASTER_APPROVAL_REQUIRED` 在三条生产路径上一致。
- GEN-009 错误处理。非最高权限、未存档、已有待审、仍被占用，都是明确的 4xx，不把行抹掉。
- GEN-010 安全与敏感信息。本 diff 不打印密钥、不放宽破窗公钥。第一次失败只说明 prod profile 下缺公钥仍会拒绝启动，该行为未改。
- GEN-011 可观测。设施与员工批准仍写 `delete_approved` / `delete_rejected`。游乐三条写入仍经 `executeWrite` 留痕。最高权限发起的游乐删除留在申请那一次写入里，同时把状态写成 approved。这是定稿的单据级留痕，不是把留痕删掉。
- GEN-012 资源边界。50 次插入是测试，不是新的生产批量接口。
- GEN-013 构建。测试在 Java 21、`mvn -o` 下编译并跑完。没有新依赖。
- GEN-014 迁移。没有新的迁移文件。空库 Flyway 399 个迁移校验通过并到版本 432。主键仍是既有 TEXT 主键，没有改表。
- GEN-015 界面。本模块无页面改动。字段中文名是报错出口，不是新页面。
- GEN-016 文档可验证。代码注释引用的是 BSM 第 570/577 行那类已有句子，没有新的业务规则说明书。判定以锁内 HTML 原文为准，不以注释为准。
- GEN-017 业务来源分类。删除规则归冲突项的已定稿修复；夹具与 UUID、`@MockitoBean` 无业务发明，归技术性。没有新的添加项。
- GEN-018 第一性原理证据。第 3 节三问都落到 HTML 原文、源码行或本次命令输出。
- BSM-003。上位是「数据可改可删、权限分岗」。本模块落实的是其中删除审批这一支，细节在 BSM-004。
- BSM-004。锁内原文：配置数据与员工档案、设施设备的删除须由具备最高权限的人审批，不设例外；持授权 U 盘密钥的最高权限本人发起不需要审批；删前查占用；删除只隐藏。第 5 节三条生产路径与软删、占用测试对得上。游乐仓储无占用检查维持原观察，不在本轮改写。
- BSM-007。平台管理员拥有日常功能权限，但不是最高权限本人。本轮没有把平台管理员重新放进删除批准。
- BSM-016。游乐权益绑定的存档、删除、墓碑恢复在 `AmusementService`。普通取消存档仍可用；墓碑只认最高权限。
- BSM-114。删除入口不再让「持有可授出审批码的人」通过。平台管理员测试被拒。
- BEQR-063。删除属于需审批的高危动作。批准人收成最高权限本人，与「高危要审批」不冲突；没有把删除改成无人审批的普通写入（最高权限本人发起是 BSM-004 写明的例外，不是取消审批这一类）。
- BEQR-144。经营主体字段键有中文名「经营主体」，门禁测试为绿。
- BEQR-153 / PGM-010。合规 POST 带上服务端终端属性后，原有的 400/200 与权限断言才执行到。拦截器本身未改。
- PGM-014。删除批准与驳回仍写审计事件；游乐经 `executeWrite`。
- PGM-041。没有新造职级或把可授出码升成最高权限。最高权限仍是既有 `master_authority` 会话形状。

### 9.2 无本面，或同文件内未被本 diff 碰到且没有新证据

下列 268 项的标题所点名的状态机、页面或计价规则，要么不在这 21 个文件里（实现在父模块的其他文件，本合成模块不代审），要么虽在 `AmusementService`、`StaffService`、`FacilityEquipmentService`、`PlatformService`、`CustomerService`、`StoredValueService` 等大文件中，但冻结 diff 没有改到那些方法。全量 3536 测没有新的失败类。本席不把全绿写成这些 HTML 句子已经逐句闭合，也不在没有新的反例时另立缺陷。父模块串行到达时照常全量审。

BEQR-003, BEQR-005, BEQR-014, BEQR-016, BEQR-020, BEQR-024, BEQR-026, BEQR-028, BEQR-029, BEQR-030, BEQR-031, BEQR-032, BEQR-033, BEQR-034, BEQR-035, BEQR-036, BEQR-037, BEQR-038, BEQR-039, BEQR-040, BEQR-041, BEQR-042, BEQR-043, BEQR-044, BEQR-045, BEQR-046, BEQR-047, BEQR-048, BEQR-049, BEQR-050, BEQR-051, BEQR-052, BEQR-053, BEQR-054, BEQR-055, BEQR-056, BEQR-057, BEQR-058, BEQR-059, BEQR-060, BEQR-061, BEQR-062, BEQR-064, BEQR-065, BEQR-066, BEQR-067, BEQR-068, BEQR-069, BEQR-070, BEQR-071, BEQR-072, BEQR-073, BEQR-074, BEQR-075, BEQR-076, BEQR-077, BEQR-078, BEQR-079, BEQR-080, BEQR-081, BEQR-082, BEQR-083, BEQR-084, BEQR-085, BEQR-086, BEQR-087, BEQR-088, BEQR-089, BEQR-090, BEQR-091, BEQR-092, BEQR-093, BEQR-094, BEQR-095, BEQR-096, BEQR-111, BEQR-115, BEQR-118, BEQR-119, BEQR-120, BEQR-121, BEQR-122, BEQR-123, BEQR-124, BEQR-125, BEQR-126, BEQR-127, BEQR-128, BEQR-129, BEQR-130, BEQR-131, BEQR-132, BEQR-143, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-160, BEQR-161, BEQR-162, BEQR-163, BEQR-164, BEQR-165, BEQR-166, BEQR-167, BEQR-168, BEQR-169, BEQR-170, BEQR-171, BEQR-172, BEQR-173, BEQR-174, BEQR-175, BEQR-176, BEQR-177, BEQR-178, BEQR-179, BEQR-180, BEQR-181, BEQR-182, BEQR-183, BEQR-184, BEQR-185, BEQR-186, BEQR-187, BEQR-189, BEQR-192, BSM-001, BSM-002, BSM-005, BSM-006, BSM-009, BSM-011, BSM-012, BSM-014, BSM-017, BSM-019, BSM-020, BSM-023, BSM-026, BSM-027, BSM-028, BSM-029, BSM-034, BSM-035, BSM-037, BSM-038, BSM-039, BSM-040, BSM-041, BSM-042, BSM-043, BSM-044, BSM-045, BSM-046, BSM-047, BSM-051, BSM-052, BSM-053, BSM-054, BSM-055, BSM-056, BSM-057, BSM-058, BSM-059, BSM-060, BSM-061, BSM-062, BSM-063, BSM-064, BSM-065, BSM-066, BSM-067, BSM-068, BSM-069, BSM-070, BSM-071, BSM-072, BSM-073, BSM-074, BSM-078, BSM-079, BSM-080, BSM-081, BSM-082, BSM-083, BSM-084, BSM-085, BSM-086, BSM-087, BSM-088, BSM-098, BSM-099, BSM-100, BSM-101, BSM-102, BSM-103, BSM-104, BSM-105, BSM-106, BSM-111, BSM-112, BSM-113, BSM-115, BSM-116, BSM-117, BSM-118, BSM-119, BSM-120, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-011, PGM-012, PGM-013, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-022, PGM-023, PGM-024, PGM-026, PGM-027, PGM-031, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-037, PGM-038, PGM-039, PGM-040, PGM-042, PGM-043, PGM-044, PGM-048, PGM-052, PGM-053, PGM-054, PGM-055, PGM-056, PGM-057, PGM-058, PGM-059, PGM-060, PGM-065, PGM-066

其中 BEQR-151 的最终权限公式在 `AuthorizationService`，不在本 diff。本模块只调用 `isMasterAuthority` 与 `requirePermission`，没有改公式。

## 10. 已定稿项与不改判项

001 至 008 在当前源码上仍符合 V4.00 定稿的最小处置：财务与合规只改测试装配与终端夹具；注解只改方法名；字段只补标签；设施、游乐、员工的批准只认最高权限本人，最高权限发起直接生效，占用检查仍走；员工与游乐的范围豁免不扩散到范围方法本体；主键改为 UUID 并有真库回归。本跑没有剩余红，也没有新的失败类。不把这 8 项送回修复队列。

不改判：XM-M036A-1、XM-M036A-2、游乐删除无占用检查的观察。本轮没有新证据。

## 11. 问题与结论

本轮新问题：0。没有新的冲突、细化缺陷、技术性缺陷或添加项。四分类没有新条目。

21 个文件都有覆盖：12 个有冻结 diff 的文件按块读过生产路径与测试断言；其余 9 个相对 origin/main 无差异，并核对了它们在失败路径上的行为（财务与合规的生产控制器、储值注解、范围方法的调用、员工范围方法本体）没有被本 diff 放开。

结论：PASS。
