CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲执行者：Claude Fable 5.1（操作员 2026-09-09 指令：Codex 额度耗尽，自 M009 起席甲全部由 Fable 接替；Grok 席乙不变）。本报告为作者自审票，权重按控制合同 §4「合取不变、推翻权不对等」处理。
- 控制合同：`D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（本席实算哈希见首行，与 sidecar、ledger 顶部基准逐字相同；LOCK-AMEND-COUNT 1）。
- 正本：`/home/james/complex-management` main，HEAD = origin/main = `242f487233d028173cb9198eecd079f5d877405c`；三份业务 HTML 只读（工作区 `business-system-map` 为 M001 已批 V0.13，本轮未改）。
- 模块：M009「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段」，20 文件 2476 行，映射冻结判据 87 项（BEQR 21、BSM 14、GEN 18、PGM 34）。
- 顺序：Grok 先派（Codex 首派因会话中断未观测退出码，按铁律 2 原样重跑同命令追加同路径；重跑真实退出后本席才写本报告；本报告落盘前未读取 `M009-b1-grok-raw.md` 任何正文）。
- 三线：第一性原理（2.3 四类依据）、顺从式（逐判据核对）、对抗式（构造反例并在 `complex_test` 以 `BEGIN…ROLLBACK` 事务实跑）。技能预检：`using-superpowers` 已载入；`grilling`（只借追问结构、不问操作员）、`verification-before-completion`、`ponytail-review` 本轮实际调用；`batch-grill-me`、`ubiquitous-language`、`grill-me` 在本席运行环境不存在（技能清单核对），记缺口、未冒充调用；`requesting-code-review`/`receiving-code-review`/`systematic-debugging`/`tdd` 留待 B2/B3/B4 节点。
- 只读承诺：本席 B1 未修改任何源码、HTML、锁、台账以外文件；所有数据库复现均在事务内并 `ROLLBACK`。

## 1. 文件覆盖表（全文逐行读完；行数/字节/SHA256 为本席独立实算，与 ledger V2.00 登记一致）

| # | 文件 | 行 | bytes | SHA256 | 覆盖记录 |
|---|---|---:|---:|---|---|
| 1 | governance/BusinessEntityRecord.java | 19 | 664 | 595d53d5…96eef7 | 全读；注释描述固定四层 parent 链（见 OBS-06） |
| 2 | governance/BusinessEntityBindingRecord.java | 19 | 463 | f97af4de…78f422 | 全读；四类主体字段齐（BEQR-143） |
| 3 | governance/MerchantRecord.java | 79 | 4611 | e881a989…0e24ff | 全读；19 参兼容构造把 archive/delete 置空（CX-01 根因之一） |
| 4 | governance/MerchantDeleteDecision.java | 8 | 307 | 07da344c…28db33 | 全读；javadoc「已物理删除」与实现不符（CX-10） |
| 5 | governance/JdbcPlatformRepository.java | 1100 | 61439 | a9dce38d…8539e3 | 全读 1100 行：主数据 upsert/分页/引用计数/绑定/复核单/审批链/工作日历 |
| 6 | governance/PlatformController.java | 227 | 13040 | 49fc52cb…c5ed2 | 全读；写端权限+幂等键，读端仅权限（CX-12） |
| 7 | governance/PlatformRepository.java | 122 | 5769 | a77bff24…109ced | 全读；默认方法与 Jdbc 覆写语义核对 |
| 8 | governance/PlatformService.java | 739 | 34404 | 85e0d87f…62472c | 全读；写路径 requirePlatformScope+executeWrite；saveMerchant 更新路径缺陷（CX-01/02） |
| 9 | governance/package-info.java | 1 | 41 | e3094c51…921ff | 全读；仅包声明 |
| 10 | dto/BusinessEntityBindingRequest.java | 17 | 442 | 66d4a8cd…e211da | 全读 |
| 11 | dto/BusinessEntityRequest.java | 17 | 431 | 85e3e5d2…c0b867f | 全读 |
| 12 | dto/MerchantArchiveRequest.java | 5 | 218 | 210a1af4…03b6d | 全读；javadoc「仅需幂等键」与 reason 字段/服务必填不符（CX-10 同项） |
| 13 | dto/MerchantDeleteApprovalRequest.java | 5 | 225 | 2516fb1d…4112c4 | 全读；comment 字段未被服务使用（OBS-08） |
| 14 | dto/MerchantDeleteRequest.java | 5 | 214 | 4898e50f…f45 | 全读 |
| 15 | dto/MerchantProfileRequest.java | 27 | 767 | cbcf2424…9bea99 | 全读 |
| 16 | dto/ProjectRequest.java | 17 | 375 | 8316db69…04c76e | 全读 |
| 17 | dto/SpaceRequest.java | 21 | 525 | 63857df1…1ab7aa | 全读 |
| 18 | dto/SpaceTypeRequest.java | 15 | 457 | d181151f…4d729f | 全读 |
| 19 | dto/StatusChangeRequest.java | 11 | 231 | 75b75cfa…425f60f | 全读 |
| 20 | dto/VenueBusinessHoursRequest.java | 22 | 700 | 6347c6df…c333f1 | 全读 |

依赖只读追流（不扩范围、不审）：`access/AuthorizationService`（requirePermission / requirePlatformScope / currentActorHasPermission / isMasterAuthority）、`evidence/AuditedOperationExecutor`、`common/idempotency/IdempotencyService.execute`、`common/Strings`、`common/api/PageResponse`、`common/PhoneMask`、`common/crypto/PiiCipher`、`governance/VenueBusinessHoursService`、`leasing/LeasingService.autoCreateMerchantForSign`、`settlement/JdbcSettlementInvoiceRepository:114`、`common/api/GlobalExceptionHandler:150-155`、迁移 V002/V005/V010/V042/V184/V259/V271/V318/V339/V348/V354/V384、OpenAPI `docs/openapi/openapi-p0.yaml` 的 `/api/v1/platform/*` 路径、前端 `apps/web/src/admin/pages/OrganizationMerchantPage.tsx:32`（COOPERATION_FORMS）、`customer/i18n/statusLabels.ts:185-193`。

## 2. 可执行证据（2.3 类型 2 / 类型 4）

E-1 现有测试实跑（160，`complex_test`，仅注入 `COMPLEX_DATABASE_PASSWORD`）：

```
mvn -f backend/pom.xml -Dtest="com.complex.platform.governance.*Tests" -Dsurefire.failIfNoSpecifiedTests=false test
Tests run: 43, Failures: 1, Errors: 0, Skipped: 0 → BUILD FAILURE（exit 1）
  PlatformServiceTests 17/17 PASS；PlatformControllerTests 9/9 PASS；VenueBusinessHoursServiceTests 6/6 PASS；
  PlatformJdbcIntegrationTests 10/11：residualMigrationCreatesBindingContractTablesAndSeedsPermissions:263
    expected "399"（MigrationVersions.latest()）but was "409"
```
失败原因是环境而非 M009 源码：`complex_test.flyway_schema_history` 已含 V400–V409（staff profile pii … staff locker scan records），且 `merchants.is_self_operated boolean NOT NULL DEFAULT false` 存在于 `complex_test` 却不存在于 main 任何迁移（`grep -rln is_self_operated backend/src/main/resources/db/migration/` 无命中）。即 `complex_test` 已被未发布分支（操作员授权只读复审的 76 文件材料）的迁移污染。登记 OBS-01；测试文件本身归 M011。

E-2 psql 复现（全部 `BEGIN…ROLLBACK`，不留数据）：

- R1（CX-01）：对 `archived_at=now()`、`delete_status='pending'` 的商户执行 `JdbcPlatformRepository.saveMerchant` 的逐字 upsert，EXCLUDED 的 archive/delete 列按 `PlatformService.saveMerchant` 的 19 参构造（null/'none'）→ `after|archived=f|delete_status=none|delete_requested_by=<空>|archive_reason=停用`；`visible_in_default_list=1`。
- R2（CX-02）：`PUT /merchants/{id}` 的同一 upsert 对不存在 id → `exists_before=0 … exists_after=1`。
- R3（CX-05）：`INSERT business_entities … entity_role='bogus_role'` → `violates check constraint "chk_be_entity_role"`；服务层无白名单，`GlobalExceptionHandler:150-155` 明示只拦 `DuplicateKeyException`，CHECK 违例落 500。
- R4（CX-03/CX-04/CX-20）：`merchants.status='whatever-status'`、`cooperation_form='not-a-mode'`、`projects.status='zzz'` 均被接受（无 DB CHECK，服务层 `trimToDefault`/`trimToNull` 不校验）。
- R5（CX-06）：对不存在的 `merchant_id`/主体 id 建 four_party active 绑定成功（无 FK，服务层不查存在性）；同商户第二条 active 绑定被 `uq_business_entity_bindings_active_merchant` 拒绝（BEQR-143 唯一绑定在库层成立）。
- R6（OBS-01）：`complex_test` 列 `is_self_operated`、迁移 400–409 清单。

E-3 静态：`grep -rn "/api/v1/platform/(merchants|business-entit|projects|spaces|space-types|venue-business-hours)" apps/web/src` 非 admin 目录 0 命中（读端加平台作用域守卫不影响商户端页面）；`grep -rn "parentEntityId|parent_entity_id"` governance 之外 0 命中（父链仅存储）；`grep -rn "settlementEntityId|contract_entity_id"` governance 之外仅 `JdbcSettlementInvoiceRepository:114 COALESCE(m.settlement_entity_id, m.business_entity_id…)`，`findActiveBusinessEntityBindingByMerchantId` governance 之外 0 消费方。

## 3. Findings（临时编号 CX-，正式 ID 在 B2 分配）

每条：位置 / 原文 / 证据 / 四分类（冲突→细化→技术性→添加）/ 2.3 依据类型 / 判据 ID / 工程缺陷 / 建议入队 / 最小处置。

### CX-01 更新商户档案会静默取消存档并抹掉待审批删除申请
- 位置：`PlatformService.java:599-640`（`saveMerchant` 更新与创建共用，构造 19 参 `MerchantRecord`）；`MerchantRecord.java:37-47`（19 参构造把 archivedAt/archivedBy=null、deleteStatus="none"）；`JdbcPlatformRepository.java:97-120`（`ON CONFLICT (id) DO UPDATE SET … archived_at = EXCLUDED.archived_at, delete_status = EXCLUDED.delete_status, delete_requested_by = …`）。
- 原文：BSM-004「删除（走审批·软删除）：要删除，得先存档，再申请删除…由具备最高权限的人审批通过后执行」「删除留痕：谁在什么时候、以什么理由删了什么，都有记录可查」。
- 证据：R1（类型 4）；代码路径（类型 2 读法 + SQL）。任何持 `merchant.profile.manage` 的更新调用（含改一个联系人字段）都会把已存档商户重新放回默认列表，并把 pending 删除申请及申请人/时间/理由清空，`archive_reason` 却保留 → 状态自相矛盾。
- 分类：细化（BSM-004 范式的下位实现）；依据类型 4 + 2；判据 BSM-004、GEN-006、GEN-009、GEN-002。
- 工程缺陷：是。建议入队：是。
- 最小处置：更新路径先 `requireMerchant(id)` 取现有记录，用 25 参构造保留 `archivedAt/archivedBy/deleteStatus/deleteRequestedBy/deleteRequestedAt/deleteReason` 与 `createdAt`（与 CX-02 同一处修）。

### CX-02 更新不存在的商户 id 会凭空创建商户
- 位置：`PlatformService.java:166-173`（`updateMerchant` 直接 `saveMerchant(...)`，不检查存在）；`JdbcPlatformRepository.java:81-123`（INSERT … ON CONFLICT 即 upsert）。
- 原文：BSM-028「建立商户档案…」、PGM-026「建立商户档案…」（创建与更新是两种动作；HTML 未授权以更新接口凭客户端指定 id 建档）。
- 证据：R2（类型 4）；`PlatformControllerTests`/`PlatformServiceTests` 无「更新不存在商户」用例（类型 2：测试实跑 43 条无此断言）。
- 分类：细化；判据 GEN-004、GEN-009、GEN-002。工程缺陷：是。建议入队：是。
- 最小处置：与 CX-01 同一处：更新路径 `requireMerchant(id)` → 不存在抛 `MERCHANT_NOT_FOUND`（404）。

### CX-03 合作方式（cooperation_form）未按 HTML 六类词表校验；三条写入路径各写各的
- 位置：`PlatformService.java:633`（`trimToNull(request.cooperationForm())` 直接入库）；`JdbcPlatformRepository.java:503-530`；DB `merchants.cooperation_form VARCHAR(64)` 无 CHECK（V042）。
- 原文：BEQR-148「合作模式：自营、租赁、联营、代运营、服务商接入、其他…」；BSM-028「建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；PGM-026「合作模式分六类…」；BSM-001「业务分类定义…不做后台可配；增加或删减一类必须先经批准写入三份 HTML」。
- 证据：R4（类型 4，`'not-a-mode'` 入库）；类型 1 原文；类型 4 grep：前端 `OrganizationMerchantPage.tsx:32 COOPERATION_FORMS = ["fixed_rent_plus_commission","revenue_share","fixed_rent"]`（这是收费模板词汇，不是六类合作方式；归 M012）；`LeasingService.java:290` 签约自动建档写死 `"leasing_auto"`（六类之外的值；归 M249–M253）；`complex_test` 现有 87 商户 `cooperation_form` 全为 NULL。
- 分类：细化（字段即 HTML「合作方式」，本模块只是不校验）；判据 BEQR-148、BSM-028、PGM-026、GEN-004、GEN-008。工程缺陷：是（信任边界无枚举校验，接口/前端/迁移词表不一致）。建议入队：是（本模块部分：非空时按六类代码白名单拒绝，空值允许——现有数据全空）。
- 跨模块：前端三值与 leasing 的 `leasing_auto` 是词表来源方缺陷，分别归 M012、M249–M253 的 B1；本模块加白名单时须与两处同批对齐，否则会把签约建档打断——B2 定稿时决定是否把这两处一行改动一并归因本 ID。六类代码为技术映射（如 self_operated/lease/joint_operation/managed_operation/service_provider/other），不构成业务发明。

### CX-04 商户状态（merchants.status）不受 HTML 六态约束
- 位置：`PlatformService.java:634`（`trimToDefault(request.status(),"active")`）；DB 无 CHECK。
- 原文：BEQR-155「商户/运营主体状态：待准入、正常、暂停、终止、归档、黑名单（…）。暂停与列入黑名单由招商部门与运营部门决定；商户退出不是立即生效，一律设缓冲期…」。
- 证据：R4（`'whatever-status'` 入库）；`complex_test` 87 行全为 `active`；「归档」另由 `archived_at` 轴表达（V259），两套表达并存。
- 分类：细化；判据 BEQR-155、GEN-004、GEN-008。工程缺陷：是（词表不封口）。建议入队：是（白名单六态代码；`archived_at` 轴与「归档」态的对应在处置中写明，不另造状态机）。
- 遗漏登记（不在本模块修复队列，无实现面）：退出缓冲期（30–60 天、按项目可配）、暂停/黑名单的决定流程在 M009 无任何代码；属 BEQR-155 未实现部分，记 OBS-02 供全局收口与后续模块。

### CX-05 经营主体角色（entity_role）不校验，非法值落 500
- 位置：`PlatformService.java:101-130`（`requireText(entityRole)` 仅非空）；DB `chk_be_entity_role`（V354）白名单 platform/project_company/management_company/spv/operation；`GlobalExceptionHandler.java:150-155` 只把 `DuplicateKeyException` 映射 409，其余完整性违例 500。
- 原文：PGM-018/PGM-019 主体分层与各主体职责；GEN-004「输入须在首次可信使用前完成…范围校验」；GEN-009「错误须可检测、可定位并安全失败」。
- 证据：R3（类型 4）。
- 分类：技术性（无业务发明；DB 已封口，服务层缺 400 级校验）；判据 GEN-004、GEN-009、GEN-008。工程缺陷：是。建议入队：是（服务层白名单，错误码 VALIDATION_FAILED 中文提示）。

### CX-06 四类主体绑定不校验商户与主体是否存在
- 位置：`PlatformService.java:499-535`（只 `requireText` 四个 id）、`537-583`；`JdbcPlatformRepository.java:330-356`；本库约定无 FK（V318 注释）。
- 原文：BEQR-143「四类主体：经营对象、运营主体、合同主体、结算主体（可分属不同公司，同一经营对象下唯一绑定）」；PGM-026「把一个商户的…绑成一组唯一关系…合同、结算、权限、看板、第三方接入都引用同一组绑定」；GEN-006「引用关系…须由事务、约束或等价原子机制保护」。
- 证据：R5（悬空引用入库成功；唯一 active 由偏索引保证）。
- 分类：细化；判据 BEQR-143、PGM-026、GEN-004、GEN-006。工程缺陷：是。建议入队：是（创建/变更前 `requireMerchant` + 三个主体 id 存在性查询，缺失抛 404/400；仓库补一个 `findBusinessEntityById` 或 `businessEntityExists`）。

### CX-07 项目/空间创建不校验 merchantId、projectId、parentSpaceId 存在
- 位置：`PlatformService.java:345-374`、`429-465`；`JdbcPlatformRepository.java:226-292`。
- 原文：PGM-005「管理楼层、区域、卡座、吧台、游乐点位、库房和设备位置」；PGM-001「先管归属…哪个综合体、哪个楼层、哪个区域、哪个点位、哪个商户」；GEN-004/GEN-006。
- 证据：类型 2 读法（无查询）+ R5 同理（无 FK）。
- 分类：细化；工程缺陷：是（低严重度，归属链可挂空）。建议入队：是（存在性校验，与 CX-06 同批同法）。

### CX-08 四类主体关系两处存储、消费方不引用绑定
- 位置：`merchants.business_entity_id/contract_entity_id/settlement_entity_id`（V005；`PlatformService.java:621-623` 写入）与 `business_entity_bindings`（V010；`createBusinessEntityBinding`）互不同步；唯一外部消费方 `settlement/JdbcSettlementInvoiceRepository.java:114` 用的是 merchants 三列，`findActiveBusinessEntityBindingByMerchantId` 在 governance 之外 0 消费方。
- 原文：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」。
- 证据：E-3 grep（类型 4）；`LeasingService.java:280-290` 签约时把三列与绑定都写成 contractSubjectId，之后 `changeBusinessEntityBinding` 只改绑定表。
- 分类：细化；判据 PGM-026、GEN-006、GEN-008。工程缺陷：是（同一事实两源，变更后必然分叉）。建议入队：是（最懒可行解：绑定创建/激活时把该商户三列同步为绑定值，商户档案更新不再允许改这三列或以绑定为准；不重写结算侧）。B2 若判为跨模块设计项则改「否」并转 M318+ 结算模块登记。

### CX-09 商户删除审批人不限于最高权限，最高权限本人发起仍需他人审批
- 位置：`PlatformController.java:111-118`（`requirePermission("merchant.profile.approve")`）；`PlatformService.java:276-320`（自审禁止，无最高权限判定）；`AuthorizationService.isMasterAuthority` 已存在但未用于本流程；V259 把 `merchant.profile.approve`（critical、requires_approval）种给 role_platform_admin。
- 原文：BSM-004「删除一律须由具备最高权限的人审批通过后执行，不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」；BSM-007「最高权限是另一个角色，全平台只设一人，即持有授权 U 盘密钥的那个人」「删除…不因平台管理员身份自动获得」。
- 证据：类型 1 原文 + 类型 2 代码；`AuthorizationService.isExplicitOrDailyGrant` 使 role_platform_admin 的 critical 条目不自动生效（与 BSM-007 后半句一致），但任何被逐项授予 `merchant.profile.approve` 的非最高权限员工均可审批通过删除；最高权限本人发起删除申请后被自审闸拦住，须另一人审批，与「不需要经过任何审批」相反。
- 分类：冲突（实现允许 HTML 明文禁止的审批主体，且阻断 HTML 明文允许的免审路径）；判据 BSM-004、BSM-007、GEN-005。工程缺陷：是。建议入队：是。
- 最小处置：`approveDeleteMerchant` 增加 `isMasterAuthority(currentActor)` 闸（保留既有权限检查）；`requestDeleteMerchant` 发起人为最高权限时直接按 approved 路径执行（仍做活引用检查与留痕）。同构模式存在于票种/价格规则/权益模板（`TicketCatalogService.approveDeleteCatalogRecord`）、设施设备、服务目录、游乐项目权益——记 OBS-03 跨模块，不在本模块修。

### CX-10 注释/javadoc 宣称「物理删除」，实现与 HTML 均为软删除
- 位置：`MerchantDeleteDecision.java:4`「approved 时商户已物理删除」；`PlatformRepository.java:32`「物理删除商户」；`PlatformService.java:273`「approved 且无活引用 → 物理删除」；`JdbcPlatformRepository.java:170-172` 实为 `UPDATE … delete_status='approved'`；另 `MerchantArchiveRequest.java:3`「仅需幂等键（存档动作无额外入参）」而记录含 `reason` 且 `archiveMerchant` 必填。
- 原文：BSM-004「删除只隐藏不抹除…数据始终留在库里可查、可审计、可恢复」。
- 证据：类型 1 + 类型 2。
- 分类：技术性（文档漂移，无业务发明）；判据 GEN-016。工程缺陷：是（误导维护者朝相反方向改）。建议入队：是（改四处注释；V259 头注释属其所在模块，不在本批）。

### CX-11 `listSpaceTypes` 分页元数据与内容不符
- 位置：`PlatformService.java:399-405`、`665-667`（`page()` 固定 `pageSize=50`，`items` 为全量，`total=items.size()`）；对照 `PlatformController.java:166-168` 营业时间用 `PageResponse.of(list,1,500)`。
- 原文：GEN-008「…名称、类型、可空性…约定须一致」。证据：类型 2 读法（字典种子 8 行时表面正常，超过 50 行即 `items.size()>pageSize`）。
- 分类：技术性；工程缺陷：是（低）。建议入队：是（改为 `PageResponse.of(records, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE)` 或删除 `page()`）。

### CX-12 平台治理读端缺平台作用域守卫，商户账号可列全平台商户/主体/空间
- 位置：`PlatformController.java:39-45, 54-63, 120-126, 135-147, 165-169, 180-186`（只 `requirePermission`，无 `requirePlatformScope`/商户谓词）；对照写端 `PlatformService` 各写方法首行 `requirePlatformScope()`（F-AUTHZ-02）；`PlatformRepository.listMerchantsByBusinessEntity` 已具备 scoped 读通道但 0 调用（注释自述「建成待激活」）。
- 原文：BSM-072「商户…不能看平台全局和其他商户数据」；BSM-074「商户不能看平台全局和其他商户数据」；PGM-012/PGM-027/PGM-048「看不到平台全局和其他商户」；PGM-041「所有业务数据默认按项目隔离…数据的项目与商户隔离不提供关闭开关」；GEN-005「受保护操作须在服务端按主体、对象和动作授权；默认拒绝」。
- 证据：类型 2 代码；反例：持 `merchant.profile.view` 的商户作用域账号（角色可按 V048 组织范围配置）调用 `GET /api/v1/platform/merchants` 得到全部 87 商户（含脱敏后联系人、面积、租金）；E-3 grep 证明非 admin 前端不调用这些读端，加守卫无功能损失。
- 分类：细化；判据 BSM-072、BSM-074、PGM-027、GEN-005。工程缺陷：是。建议入队：是（读端与写端同法：`requirePlatformScope()` fail-closed；或对商户主体改走 `listMerchantsByBusinessEntity`——按最懒可行解取前者）。

## 4. 观察（登记、不入队）

- OBS-01（技术性/环境）：`complex_test` 已应用未发布 V400–V409 且含 `merchants.is_self_operated`，与 main（最新 V399）不一致；导致 M011 `PlatformJdbcIntegrationTests:263` 失败。对本模块源码无缺陷；对 M011 测试证据、以及任何依赖 `MigrationVersions.latest()` 的门禁构成环境污染。处置建议（运维，非本审计修复项）：把 `complex_test` 回滚到 V399 或另建干净测试库；须操作员知悉。
- OBS-02（遗漏）：BEQR-155 退出缓冲期、暂停/黑名单决定流程、BSM-004「一次性存档超过后台配置条数须审批」「已存档内容页」「数据清理工具」在 M009 无实现面；PGM-020「记录项目状态、负责人、地址和经营范围」在 `projects` 表无负责人/地址/经营范围列（本模块 `projects` 同时承载场地类型 venue_type/floor/capacity，`complex_test` 中 0 行）。均非本模块现有代码缺陷，登记供全局收口。
- OBS-03（跨模块）：CX-09 的「审批人非最高权限」模式在 `TicketCatalogService.approveDeleteCatalogRecord`、`FacilityEquipmentService.approveDelete`、`ServiceOrderService.approveDeleteCatalogItem`、`AmusementController.approveDeleteProjectEntitlement` 同构，各归其模块 B1。
- OBS-04（跨模块）：`PlatformRepository.listMerchants()` 无过滤版本（含已存档/已批删）被 `ContractService:755`、`MerchantBillingService:497`、`MerchantFeeNotificationRecipientResolver:64` 使用，对照 BSM-004「下线就不能再用」，由各消费模块判定是否应改用 `listMerchants(false)`。
- OBS-05（技术性）：`countMerchantReferences` 每次枚举 information_schema 并逐表 COUNT（约 40 表）；仅在审批删除时调用，负载相称（GEN-012 通过）。
- OBS-06（技术性）：`business_entities.parent_entity_id` 单父链（V318 注释「四层：平台→项目公司→管理公司→商户」、`BusinessEntityRecord.java:9-10`）与 BEQR-144/PGM-018「物理项目与运营公司多对多、不是固定隶属链」表述方向相反；但 governance 之外 0 消费方、无任何权限或归属逻辑读取父链，V354 仅用于 seed 六主体骨架。判为存储级技术细节，不构成冲突；注释措辞建议在触及该文件的修复中顺带不改（铁律 6：不加改动）。
- OBS-07（技术性）：`createSpaceType` 控制器未调 `requireIdempotencyKey`，但 `IdempotencyService.execute:75` 仍必填，行为一致，仅风格不一。
- OBS-08（技术性）：`MerchantDeleteApprovalRequest.comment` 未被服务使用（审批意见未留痕）；BSM-004「删除留痕：谁在什么时候、以什么理由删了什么」——理由为申请人 `delete_reason` 已留，审批人意见未留；低严重度，B2 可并入 CX-10 或单列。
- OBS-09（PII，登记）：联系人姓名（contact_name/secondary_contact_name/owner_person）明文返回给所有持 `merchant.profile.view` 者，手机号按 `pii.contact_phone.view` 脱敏（PGM-060/PGM-043 手机号规则已落实）；PGM-043 的姓名规则仅对导出与客人/员工明文规定，商户联系人姓名无 HTML 明文脱敏要求，不判缺陷。
- OBS-10（技术性）：读端每次列表都写 `audit_events`（`*.viewed`），GEN-011 留痕成立；`archiveMerchant` 三写（saveMerchant / setMerchantArchiveReason / expireBarSingleStoreTickets）在 `IdempotencyService.execute` 内执行，事务边界由其保证与否留待 M011/M026 侧核；本席未在 M009 范围内发现独立缺陷。
- OBS-11（技术性）：错误文案主体为中文（PGM-001 全中文原则），`"idempotency_key is required"`/`"entity_name is required"` 为机读字段键，由 `GlobalExceptionHandler`+`FieldLabels` 出网前翻译（`Strings.java:22-25` 注释；V251 已把权限名中文化）。

## 5. 87 判据逐项核对

| 判据 | 适用面 | 结论 |
|---|---|---|
| BEQR-063 高危动作（酒吧） | 本模块无酒吧动作实现面 | 不适用，无冲突 |
| BEQR-074 隐私桌台 | 无实现面 | 不适用 |
| BEQR-143 四类主体 | 绑定记录四字段 + 唯一 active 偏索引 | 结构符合；引用完整性缺口见 CX-06 |
| BEQR-144 经营主体关系 | 父链仅存储 | OBS-06，无冲突 |
| BEQR-145/146/147 城市项目公司/运营公司职责/收入归属 | 本模块只存主体档案，无职责/收入逻辑 | 不适用（V354 seed 六主体与原文名称一致） |
| BEQR-148 合作模式六类 | cooperation_form | CX-03 |
| BEQR-149 职级 / BEQR-150 终端十一端 / BEQR-151 权限计算 / BEQR-152 项目与敏感数据权限 / BEQR-153 开关优先级 | 无实现面；BEQR-152 的项目隔离见 CX-12 | CX-12 |
| BEQR-154 综合体项目状态六态 | projects.status 任意字符串 | 并入 CX-04 同法处置（见 §3 CX-04 末句与 OBS-02） |
| BEQR-155 商户状态 | merchants.status | CX-04、OBS-02 |
| BEQR-156 合同状态 / BEQR-157 员工状态 / BEQR-158 设备状态 / BEQR-159 权限状态 / BEQR-166 合同审批链 / BEQR-189 管理费方式 | 无实现面（复核单/审批链仓库方法属 M468–M471 的实现，本模块只承载 Jdbc 落盘） | 不适用 |
| BSM-001 可配置/可溯源原则 | 无金额策略参数；分页与审计事件可回溯 | 通过 |
| BSM-003/BSM-004 存档与删除 | archive/unarchive/delete-request/approve | CX-01、CX-09、CX-10、OBS-02、OBS-08 |
| BSM-005 分步引导 | 前端（M012） | 不适用 |
| BSM-006 动态二维码 | 无实现面 | 不适用 |
| BSM-007 平台管理员日常权限 | V005/V259 种子 + `isExplicitOrDailyGrant` | 通过（manage/approve 为 high/critical 不自动生效） |
| BSM-009 基础管理 | 本模块即其后台实现 | 见各 CX |
| BSM-028/029 主要功能/和谁有关 | 商户档案、六类合作方式、主体职责 | CX-03、CX-06、CX-08 |
| BSM-072/074 商户管理边界 | 读端作用域 | CX-12 |
| BSM-114/115 入口与关系 | 管理后台入口 | CX-12（商户侧不得看平台全局） |
| BSM-120 已确认规则 | 「集团视图/项目归属」「所有端后台开关」等 | 无实现面冲突；项目归属见 OBS-02 |
| GEN-001 必要性 | `listMerchantsByBusinessEntity` 0 调用但为门禁判据保留（注释自证） | 通过 |
| GEN-002 可执行正确性 | 43 测试实跑；CX-01/02 主路径行为与声称不一致 | CX-01、CX-02 |
| GEN-003 测试有效性 | 测试文件属 M011；本模块行为由 42/43 通过 + 1 环境失败佐证 | OBS-01 |
| GEN-004 信任边界 | 枚举/存在性校验 | CX-03/04/05/06/07 |
| GEN-005 授权 | 写端 platform-scope；读端缺 | CX-12 |
| GEN-006 数据完整性 | upsert 覆盖状态、悬空引用、双源 | CX-01、CX-06、CX-08 |
| GEN-007 并发幂等 | 全部写入经 `IdempotencyService`（键必填、请求哈希、caller scope）；绑定唯一 active 由偏索引兜底 → 409 | 通过 |
| GEN-008 接口一致性 | 词表/分页元数据/双源 | CX-03、CX-04、CX-08、CX-11 |
| GEN-009 错误处理 | CHECK 违例 500；幻影创建 | CX-02、CX-05 |
| GEN-010 安全 | SQL 全参数绑定；information_schema 表名拼接来自系统元数据；PII AES-GCM 落库、控制器脱敏 | 通过 |
| GEN-011 可观测 | 每次读写记 audit_events，含 idempotencyKey 与 businessId | 通过 |
| GEN-012 资源边界 | LIMIT 500 / size 上限 500 / 引用计数循环 | 通过（OBS-05） |
| GEN-013 构建配置 / GEN-014 迁移 | 本模块无构建/迁移文件（迁移归 M010） | 不适用 |
| GEN-015 界面 | 后端；文案中文（OBS-11） | 通过 |
| GEN-016 文档可验证 | 注释与实现相反 | CX-10 |
| GEN-017 分类 | 本报告逐条按 冲突→细化→技术性→添加；未发现「添加」项（无 HTML 之外的业务实体/枚举/流程发明；`leasing_auto` 词值归 M249） | 无添加项，B2 无需 2.2 询问 |
| GEN-018 第一性原理 | 每条 CX 落到原文/可执行/命令输出 | 通过 |
| PGM-001 整体定位（可溯源/全中文/金额精度/先成功为准/仲裁顺序） | 金额 NUMERIC(18,2)+BigDecimal；幂等；中文 | 通过 |
| PGM-002 治理顺序 / PGM-003–016 模块入口卡片 | 描述性；本模块对应 PGM-004/005/006 | 见 CX-06/07/12 |
| PGM-017/018/019/020/021 综合体与集团 | 归属字段、父链、主体职责 | OBS-02、OBS-06 |
| PGM-026/027 商户和主体主要功能/边界 | 四类绑定、六类合作方式、商户边界 | CX-03、CX-06、CX-08、CX-12 |
| PGM-031 经销商 / PGM-035 提成 / PGM-040–044 权限与职级 / PGM-048 商户侧 / PGM-060 会员与生物信息 / PGM-065 连接关系 | 无直接实现面；PGM-041/048 数据隔离与 PGM-060 PII 由 CX-12、OBS-09 覆盖 | CX-12 |
| PGM-066 已拍板规则 | 「所有对象带综合体项目归属」（merchants/business_entities 无 project_id，spaces 有）→ OBS-02；「全中文」通过 | OBS-02 |

## 6. 结论

审计结论：**FAIL**（B1 修复前）。12 条 CX 中 12 条有工程缺陷（建议入队 12，其中 CX-03/CX-08 需 B2 裁定跨模块边界）；1 条冲突（CX-09）；0 条添加（无需 2.2 询问）；11 条观察登记。三份 HTML 本身未发现需上报的问题（不触发铁律 4/铁律 5 闸）。

本席 B1 未读取 Grok raw；本报告落盘后才读取并进入 B2 归并。


## M009 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M009-b1-self-raw.md` V0.01 前缀 31878 bytes SHA256 76f4af4b282b89831e27f6e32b003e0980a4a5b1f64d2e457cd6314a201da0a6；`modules/M009-b1-grok-raw.md` 本次完整报告（第 386 行独占锁行至 EOF，文件 V0.02 75079 bytes SHA256 37c0eca7809fa90d319c5dfd51c05cd4256c011c69c56ff620c5809f36c51cf4）；候选 `findings.md` V1.17 449655 bytes SHA256 443c7380e0aa6581823384ef6c35e6bf306be73e432756fdbae1895938eefdd4，「当前 M009 B2-r1 整批候选」M009-001…025。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。按 `receiving-code-review` 收窄：先核源码/命令再接受或反驳，禁止表演性同意；B2 不修复。
- 差集：Grok GR-M009-001…011 与其 OBS-01…11、本席 CX-01…12 与 OBS-01…11 逐条对照候选 001–025，无遗漏、无改写；Grok 原意分歧处在候选「来源归并」与本席意见并列写明。

| ID | 本席票 |
|---|---|
| M009-001 | PASS：R1 与 Grok 探针同证；冲突分类接受（BSM-004 明文「想恢复点取消存档」被改档路径绕过）；处置最小。 |
| M009-002 | PASS：R2 幻影建档；同处修复。 |
| M009-003 | PASS：六类是 HTML 业务分类（BSM-001「不做后台可配」），代码映射为技术；跨文件三处最小改动归因同一 ID 以免打断签约/录入，若外审拒绝跨文件则退为后端白名单仅拒六类之外非空值并把 leasing/前端记 M249/M012。 |
| M009-004 | PASS：R4；归档不作可设代码以免与 archived_at 双轴打架；不造状态机。 |
| M009-005 | PASS：R3；白名单三角色均有 PGM-018/019 上位；spv/operation 转 025。 |
| M009-006 | PASS：R5；自动建绑定部分登记不入队——PGM-026 写「绑成一组唯一关系」「签约转商户…四类主体默认指向同一公司」，未写「建档即须绑定」；由外审独立裁定。 |
| M009-007 | PASS：同法存在性校验。 |
| M009-008 | PASS（保留跨模块退路）：E-3 grep 证明结算侧只读商户三列、绑定表无外部消费方；同步三列是让「引用同一组绑定」在事实上成立的最小改动。 |
| M009-009 | PASS：BSM-004/007 原文明确「具备最高权限的人」与「本人发起…不需要经过任何审批」；两面合并一 ID。 |
| M009-010 | PASS：四处注释 + includeArchived 纳入已批删行 + 复用 unarchive 恢复；不真 DELETE。 |
| M009-011 | PASS：存档≠退出；删除调用即消除「立即生效」；下单侧 TICKET_BAR_OFFLINE 与本调用无关（只读核 TicketingService 调用链未变）。 |
| M009-012 | PASS（改判）：本席 B1 判技术性依据是 0 消费方；外审指出 HTML 逐字否定该链，且注释/种子把它当行政隶属落库——采冲突；删除范围限 M009 面，不动列与 V318/V354。 |
| M009-013 | PASS（附条件说明不是有条件通过：本票对候选文本投 PASS，队列取「是」；若外审判超范围，本席接受改否并登记遗漏）。 |
| M009-014 | PASS。 |
| M009-015 | PASS：GEN-005 默认拒绝；写端已有同法守卫；非 admin 前端 0 调用，无功能损失。 |
| M009-016 | PASS：GEN-001 死代码删除。 |
| M009-017 | PASS：环境漂移证据（flyway 409、is_self_operated）与源码无关；否队列；须操作员知悉。 |
| M009-018 | PASS：无实现面遗漏登记。 |
| M009-019 | PASS：跨模块登记。 |
| M009-020 | PASS：跨模块登记。 |
| M009-021 | PASS：仅登记。 |
| M009-022 | PASS：行为一致不改。 |
| M009-023 | PASS：登记。 |
| M009-024 | PASS：归其它模块。 |
| M009-025 | PASS：spv/operation 不在 M009 二十文件，M009 白名单不含二者，转 M010 按 2.2；本批不发问不构成绕过。 |

三 HTML 未改，无 HTML 修改申请；添加项 0（025 路由待外审确认）。全批修复只针对正式定稿「是」的 ID 及可归因最小测试；任何文件更新进自身版本；不改判据/权限种子/其它模块业务逻辑。B3 仍为 0。

最终本批本席票：PASS。下一步才读取 Grok 本轮 B2 全文并合取，未先修复。


## M009 B2-r2 整批候选复核（席甲票）
版本：V0.03（本文件自身；此前 V0.01 B1 与 V0.02 B2-r1 票原字节保留，本段追加）

- 输入：本席 B1 V0.01 前缀 31878 bytes（SHA256 76f4af4b…01da0a6）；Grok B1（V0.02 段）与 Grok B2-r1（V0.03 追加段，文件 97095 bytes SHA256 9a647121e1256610b96f68052aa5770c2eb4f484af629ef18a03c7c5137aeda7）；候选 `findings.md` V1.18 462980 bytes SHA256 9f38b7e49c6a2dc16fb6eaf5c13cb284aba7e16e9e78d0314558cb9bbc2e33f0「当前 M009 B2-r2 整批候选」。
- 本段不写独占锁行；锁实算仍 b012328f…6695a。
- B2-r1 合取结果：席甲 PASS、Grok FAIL（仅 013）。按 `receiving-code-review` 收窄先核实再接受：`plan/modules.md` 第 248 行 `ProjectRecord.java` 在 M015 FILES、M009 FILES 无迁移文件（类型 4：grep）；三列属新建能力，外审否决成立，本席改判并按其意见修订候选。Grok r1 对 012/015/006 后半/003/008/025 的独立裁定与本席 r1 票一致，无需再改。
- 适配器故障（REPORT_MISSING 但四项事实满足）已修正为退出后等待路径存在且大小稳定，并以派发前基线重做验收；未重跑 Grok。

| ID | 本席票 |
|---|---|
| 001–012、014–016 | PASS：分类/缺陷/队列与 r1 相同，外审 r1 均接受。 |
| 013 | PASS（改判）：细化（遗漏）、否队列、并 018 口径登记全局遗漏；永久 ID 保留。 |
| 017–025 | PASS：与 r1 相同。 |

无 HTML 修改申请；添加项 0（025 转 M010）。全批修复只针对定稿「是」的 15 个 ID 及可归因最小测试；改动文件自身版本递增；不改判据/权限种子/其它模块业务逻辑；B3 仍为 0。

最终本批本席票：PASS。下一步才读取 Grok 本轮 B2-r2 全文并合取，未先修复。
