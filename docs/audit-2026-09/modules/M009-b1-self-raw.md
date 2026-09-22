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


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B1 Codex 独立全量审计
版本：V0.04
日期：2026-09-14
审计结论：FAIL
生命周期：IN_PROGRESS；本次新授权轮 B3=0/3。旧轮记录保留，不将旧 PASS 当作当前证据。
操作者：当前 Codex 席；SSH james@192.168.88.160。正本 /home/james/complex-management main。
基线 HEAD：c039798a8a89993ace0d03bad566665d37a1877e。
范围：原冻结471模块计划中的M009，20文件，2879行。已完整阅读，不将相关依赖文件并入范围。
独立性：本报告形成前未读取 Grok 本次/旧轮 M009 原始审计内容。Grok 由既有无头命令经进程内回执适配器运行，其结果仅在本报告落盘后读取。按操作员指令并行工作、保持互盲。

## 第一性原理三问
1. 为什么存在：PGM-017/020要求项目归属与空间基础数据；PGM-026和BEQR-143要求商户及四类主体唯一绑定；BSM-004要求存档、删除审批、恢复和证据。records/DTO承载这些字段，service实现动作，repository/JDBC承载持久化，controller承载已授权HTTP入口。删除这些会使上述明确要求失去实现。package-info仅包声明，为技术性、无业务发明。
2. 声称是否实际执行：已逐读并实跑46服务单测、12控制器测试、17真实JDBC测试，全部通过。另用现有测试夹具调用真实Service的JShell反例，以及回滚事务中的真实JdbcRepository探针，证实原测试未覆盖的失败场景。测试通过不替代业务正确性。
3. 哪些前提未验证：控制器测试多用mock，不能证明权限链；项目作用域另通过真实AuthorizationService守卫复现。事务不能凭缺@Transactional推测，已读共享IdempotencyService的TransactionTemplate。电话和幂等响应加密都已核。没有跑完整前端/生产流量，也未声称审完相关模块。HTML没有“联合审批”的暂停/黑名单规则，不擅自添加。

## 冻结来源
- docs/business-enum-quick-reference-v0.1.html V0.07，SHA256 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66。
- docs/business-system-map-for-operations-v0.1.html V0.14，SHA256 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66。
- docs/platform-governance-map-for-operations-v0.1.html V0.12，SHA256 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1。
- criteria-lock.md V0.11，LOCK-AMEND=1；该锁和HTML保持只读。20文件SHA在测试后重算均未变。

## 逐文件全量覆盖
路径前缀：backend/src/main/java/com/complex/platform/governance/。
|文件|行数|SHA256|审计线|
|---|---:|---|---|
|BusinessEntityRecord.java|17|45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277|第一性原理、顺从式、对抗式均已完整覆盖|
|BusinessEntityBindingRecord.java|19|f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422|第一性原理、顺从式、对抗式均已完整覆盖|
|MerchantRecord.java|90|66a840d9b71935b15328b08b4daea61867851084dc27634025e45bd1cb12ace2|第一性原理、顺从式、对抗式均已完整覆盖|
|MerchantDeleteDecision.java|9|e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40|第一性原理、顺从式、对抗式均已完整覆盖|
|JdbcPlatformRepository.java|1167|8ce7f0af00668768c0430a58158481444cfd45d2f358e6500b8bd99dd78527c4|第一性原理、顺从式、对抗式均已完整覆盖|
|PlatformController.java|277|9b476721e1768d39ca8043fe556cfb26b2917595bb32af1ddaa275e571e525c3|第一性原理、顺从式、对抗式均已完整覆盖|
|PlatformRepository.java|145|42e2e86a36bdd7dbd8975ef5f9b21082b5242a3fab52a1dbe8b7af7091b371cd|第一性原理、顺从式、对抗式均已完整覆盖|
|PlatformService.java|991|9ca85b1b07c4432e3f3000c1e1343b2b19f5cd0109e8dfd92855634384febcf3|第一性原理、顺从式、对抗式均已完整覆盖|
|package-info.java|1|e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/BusinessEntityBindingRequest.java|17|66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/BusinessEntityRequest.java|17|8c4b7795909a6dc597fab7937105d6b54979d9ad900a8e8d45d688fe1cbb725e|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/MerchantArchiveRequest.java|6|e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/MerchantDeleteApprovalRequest.java|5|2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/MerchantDeleteRequest.java|5|4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/MerchantProfileRequest.java|27|cbcf242470823799c3d2fcbe996de4665e4d440614a4263a2635ccb4439bea99|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/ProjectRequest.java|17|8316db69c16d3013dbfc07ead14673321a49d05c3131163ae58597543004c76e|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/SpaceRequest.java|21|63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/SpaceTypeRequest.java|15|d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/StatusChangeRequest.java|11|75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f|第一性原理、顺从式、对抗式均已完整覆盖|
|dto/VenueBusinessHoursRequest.java|22|6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1|第一性原理、顺从式、对抗式均已完整覆盖|

范围内业务内容分类：BusinessEntityRecord/BindingRecord及请求=PGM-018/026细化；MerchantRecord及生命周期DTO=PGM-026/BSM-004细化；ProjectRequest/SpaceRequest/SpaceTypeRequest=PGM-017/020、BSM-028细化；VenueBusinessHoursRequest=BSM-028空间/经营基础资料细化；StatusChangeRequest=对应实体生命周期细化。Controller/Service/Repository/JDBC为上述实现承载，缺陷见下表。package-info.java技术性只登记、无业务实体/枚举/状态/权限/校验/价格/默认值发明。既有默认值及枚举不以迁移、注释或旧PASS取得业务权威；逐字检查可指向上位对象，未形成新的无来源业务概念finding。

## 顺从式：87条冻结判据逐条核对
下列“本段未承载”仅说明这20文件的适用边界，不表示删掉映射或其它模块已PASS。
- BEQR-063：本模块删除入口已有最高权限及申请人分离检查；财税主体普通替换未受变更审批控制，CX-03。
- BEQR-074：隐私桌台及收桌请求不在这20文件内；DTO/仓库不定义其例外权限。
- BEQR-143：四主体字段与 active 唯一索引存在；初建商户投影不经绑定与引用校验，CX-10；未来绑定问题CX-04。
- BEQR-144：主体role与物理project分开存储；未把字段间简单引用直接当成公司管理权。
- BEQR-145：project_company角色存在；空间数据归属缺口CX-02；未在此实现城市公司自动抽成。
- BEQR-146：域公司职责的结算变更边界存在CX-03；没有依据普通role字符串自行认定跨域收入权。
- BEQR-147：M009不计算各域收入，结算主体投影会影响下游，CX-03/04；未声称下游全部结算已验证。
- BEQR-148：六种合作方式白名单与原文一致；非法值有测试，空值代表签约前待定，没有据空值发明默认收费。
- BEQR-149：20文件不定义薪级/职级；依赖权限服务，不把其它模块职责遗漏算本模块缺陷。
- BEQR-150：控制器终端能力声明已逐入口阅读；本模块不生成十一端清单。
- BEQR-151：控制器依赖requirePermission；字段权限通过不等于对象隔离或主体审批，CX-01/03。
- BEQR-152：项目普通人员通过平台作用域守卫，服务查询未限制project，CX-01。
- BEQR-153：终端开关由共享控制层执行；本模块没有引入能关闭红线的新开关，不声称共享控制链已全量复审。
- BEQR-154：项目状态六值与原文一致，status空缺使用现有active；英文报错CX-12。
- BEQR-155：商户状态存在对应值；存档为独立可逆动作；直接terminated绕过退出缓冲，CX-13。原文是两部门决定，未新增所谓联合审批要求。
- BEQR-156：无合同状态转换实现，contractEntityId不是合同状态；本模块不改合同既有快照。
- BEQR-157：无员工状态转换实现，只用当前操作者作授权/审计关联。
- BEQR-158：无设施设备状态转换实现；空间可用状态不混称设备检修状态。
- BEQR-159：权限状态由共享AuthorizationService计算；当前文件没有新权限生命周期枚举。
- BEQR-166：普通绑定修改不调用财税法务变更审核，CX-03；其他合同审批链由合同模块承载。
- BEQR-189：租金摘要不是管理费计算器，未见本模块新增管理费模式或固定抽成；负租金有效性CX-09。
- BSM-001：20文件承载基础治理、数据归属、主体投影；发现的边界缺口按下位条目列出，不将概述当无限扩范围许可。
- BSM-003：通用存档删除要求应用于商户和空间类型；恢复/证据缺口CX-05/06。
- BSM-004：已实现商户/空间类型存档、申请、最高权限和自审批防护；已删除空间类型不能恢复、理由遗失、申请无终态、历史引用误拦，CX-05/06/07/08。
- BSM-005：分步引导由页面负责；本模块DTO与API提供输入，不凭后端文件缺页面判遗漏。
- BSM-006：这20文件没有二维码生成/核验路径，不新增刷新频率规则。
- BSM-007：日常权限由共享授权服务处理，主权限和普通平台管理员有区分；不据管理员身份豁免财税主体变更审批。
- BSM-009：基础管理入口与项目/空间/商户/主体实体对应，必要性成立；对象归属缺口CX-01/02。
- BSM-028：项目、空间、商户及六类合作存在；项目隔离和财税变更未贯通CX-01/03。其余业务域仅按本模块交接字段检查。
- BSM-029：核对基础对象向合同/权限/结算的关联；初建无效主体可持久化CX-10。
- BSM-072：商户读取分支使用merchantId，平台普通项目分支未隔离CX-01；未发现允许普通商户随意创建平台主体的相同守卫路径。
- BSM-074：共享商户对象/绑定面向上下游；读取结算投影证据见CX-04/10，不把独立部署构建等其它模块内容重复审计。
- BSM-114：当前控制器公开入口均有权限调用或平台守卫；仍缺对象project限制CX-01。
- BSM-115：追到下游结算读取真实商户当前主体，确认绑定字段有业务效果CX-03/04。
- BSM-120：项目归属、留痕、权限与默认关闭原则按实际承载路径检查；本模块CX-01/02/06。未把地图其余全部业务逐项实现强加给M009。
- GEN-001：复用已有AuditedOperationExecutor、IdempotencyService、Jdbc模板；未提出新框架、角色、文件类型或整表重构。
- GEN-002：运行58服务/控制器测试及17真实JDBC测试；关键未覆盖反例CX-04/05/08/13。
- GEN-003：逐读75测试断言：控制器多使用mock、服务内存仓库、JDBC事务回滚，不能以75绿代替负向验证。追加进程内探针已复现边界。
- GEN-004：DTO范围、存在性、对象所有权逐入口检查；空间缺项目/父跨项目、负数、幽灵主体CX-02/09/10。
- GEN-005：已核功能权限和最高权限；普通项目数据隔离及财税变更授权不足CX-01/03。
- GEN-006：核实active部分唯一索引和executeWrite事务；日期投影、负数量、无效引用、删除申请终态、退出缓冲CX-02/04/07/09/10/13。
- GEN-007：共享IdempotencyService真实Spring使用TransactionTemplate，JDBC有唯一性；不误报普通治理写无事务。空间类型生命周期没用此通道，故障注入只记录限制不扩建重试系统。
- GEN-008：API/DTO/数据库字段与枚举已对照；未来生效字段与即时active矛盾CX-04，展示message混英文CX-12。
- GEN-009：恢复入口对disabled无可走路径CX-05；历史占用误阻CX-08；未把单纯错误日志当功能失败。
- GEN-010：SQL参数化；商户电话经PiiCipher写入，幂等response_body加密已核；对象越权见CX-01。测试/报告不输出凭证。
- GEN-011：AuditEvent对象关联与metadata实测CX-06，申请表终态CX-07；历史结论不替代新执行证据。
- GEN-012：Jdbc连接由模板管理、探针finally/事务释放；list全表读取与当前范围相关风险归CX-01，不为未测负载新增缓存/分页框架。
- GEN-013：使用项目现有Maven/JDK21/Surefire依赖离线测试成功；未加依赖或新工具链。
- GEN-014：complex_test实际应用既有V424-V426迁移后17JDBC全绿；未改迁移文件或生产库。M009自身不含迁移文件，依赖核对有边界。
- GEN-015：本模块无前端组件；实际HTTP message经过FieldLabels仍混英文CX-12。未宣称完成键盘/焦点视觉审查。
- GEN-016：本模块package-info仅包声明；所有当前执行与旧证据区分，20文件清单和新raw可机器复核。
- GEN-017：每份业务DTO/record均有项目/空间/商户/主体上位需求；共享接口与JDBC是承载下位实现。package-info技术性无业务发明单独登记；未把实施层事实当业务权威。
- GEN-018：所有缺陷落在当前HTML逐字原文、实际测试/探针或带命令的源码/约束输出，排除未经证实的联合审批解读。
- PGM-001：归属、责任、中文文案要求分别对应CX-01/02/06/12；共享事务/幂等已核，不新发明金额舍入口径。
- PGM-002：权限检查存在但对象隔离和财税主体变更流程不完整CX-01/03。
- PGM-003：所列治理模块在本段仅实现项目空间商户主体后端；其余模块仅检查接口关联。
- PGM-004：项目CRUD存在；项目归属和跨项目访问见CX-01/02。
- PGM-005：空间CRUD及类型生命周期存在；空间父归属和类型恢复CX-02/05。
- PGM-006：商户CRUD及唯一绑定存在；初建与变更、生效问题CX-03/04/10/13。
- PGM-007：本段保留contractEntityId及合作摘要，不实现整个合同审签流程；财税修改绕开已有要求CX-03。
- PGM-008：本段不处理员工手机绑定，当前actor只作权限/审计来源。
- PGM-009：逐入口核对权限守卫，数据范围缺口CX-01。
- PGM-010：本段无终端开关管理实现；不以未承载的界面开关判缺陷。
- PGM-011：本段无设备登记/合规流程；空间point字段不能替代设备权限。
- PGM-012：商户限定读自身档案分支已核；其它平台分支问题CX-01。
- PGM-013：主体绑定会被第三方交接使用；当前投影不一致CX-04/10；未杜撰外部接口规范。
- PGM-014：对象ID和删除理由追溯不足CX-06/07。
- PGM-015：楼宇/空间基础记录由该模块供给，归属/容量CX-02/09；设施工单流程不在20文件。
- PGM-016：无应急放行或隐私桌台实施；不能用此标题豁免项目隔离。
- PGM-017：所有基础对象要带项目；空间可无项目及父跨项目CX-02。
- PGM-018：公司与物理项目分开，角色三值存在；未把域公司业务职责推成固定父子管理链。
- PGM-019：角色表字段不自动产生票务分账权；核对真实结算投影变化CX-03/04。
- PGM-020：项目状态/地址/归属等存在；普通岗位跨项目可读与空间归属缺口CX-01/02。
- PGM-021：现有projectId为未来多项目基础，现有路径不能保持隔离CX-01/02。
- PGM-026：唯一绑定索引与三个商户主体投影存在；未审先改、未来提前生效、初建幽灵引用CX-03/04/10；数值摘要有效性CX-09。
- PGM-027：商户读取按actor限定本商户；有功能权限的非商户employee仍跨项目CX-01。
- PGM-031：此处普通商户档案与经销商专档分开，无采购/价格目录算法；不把额外模式加到本模块。
- PGM-035：本模块不修改员工手机/工作机状态；仅使用共享身份信息。
- PGM-040：功能权限粒度与控制器permission常量对应；数据范围错误CX-01不由功能授权抵消。
- PGM-041：普通项目人员跨项目读取、财税主体变更未审批CX-01/03；未把日常管理员身份视作高敏豁免。
- PGM-042：不在20文件定义薪级规则，当前组织层级仅供共享授权；不擅自新增角色。
- PGM-043：requirePlatformScope显式拒绝outsourced已有代码；审计员是否高敏必须走共享权限，不靠本模块新默认。
- PGM-044：本模块没有桌台、挂单/经销商生产操作，实现边界无相应新增规则。
- PGM-048：商户侧读自身资料与联系人脱敏路径已检查；未将平台读权限自动赋给商户。
- PGM-060：联系人电话经过加密及脱敏；本模块无生物信息或会员成长计算，不发明会员规则。
- PGM-065：追踪基础记录→主体绑定→下游结算以及审计引用，CX-04/06/10。
- PGM-066：中文、对象归属、过程留痕和已拍板边界对应CX-01/02/06/12；无修改任何HTML。

## 全部已确认问题
共12项。临时编号保留跳号CX-11：原“黑名单须联合审批”解释证据不成立，未纳入问题。没有源代码修复，正式ID及队列由B2全量归并后确定。
### CX-01 P1 冲突
问题：项目普通岗位仅因没有 merchantId 即通过 requirePlatformScope；项目、空间、商户读取仍查全表，写入也不校验目标项目。
位置：backend/src/main/java/com/complex/platform/governance/PlatformController.java:58-67、125-147；PlatformService.java:140-147、362-363、421-422；JdbcPlatformRepository.java:454-505
冻结判据：BEQR-152 / PGM-041 / GEN-005
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-041：所有业务数据默认按项目隔离，普通岗位没有跨项目查看权；只有项目之上的获授权岗位才可以跨项目汇总。
证据：实际 REPL 以项目甲 employee 主体通过 requirePlatformScope 后，listProjects 返回甲乙两个项目；数据库五表 relrowsecurity/relforcerowsecurity 全 false，查询没有项目谓词。这里只证明已获得相应功能权限的普通项目人员仍缺对象隔离，不声称无权限也可调用。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-02 P1 冲突
问题：新空间可不填项目；父空间只查存在、不校验同项目，允许将项目乙空间挂到项目甲父节点。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:595-618；dto/SpaceRequest.java:9-19
冻结判据：PGM-017 / PGM-020 / GEN-004 / GEN-006
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-017：所有基础对象都要带“综合体项目归属”。PGM-020：为商户、空间、合同、员工、设备、票务和看板提供归属。
证据：PROBE_PROJECT_REQUIRED=null；PROBE_PARENT_CROSS_PROJECT=true。真实数据库 spaces.project_id 可空，无所属关系 FK/触发器兜底。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-03 P1 冲突
问题：已有主体绑定停用后，普通治理操作可创建另一组并立即覆盖商户结算主体，没有公司权责变更及财税、法务并审。功能授权不等于此笔主体变更已经审批。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:658-749、832-835；PlatformController.java:238-254
冻结判据：PGM-026 / GEN-005 / GEN-006
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-026：任何一笔业务的收款方、结算方或收入归属方需要变更时，只能通过“公司主体与权责配置”办理，不得由后台的普通开关或运营操作改动；财税主体的调整须经平台财务与税务、法务并行审核。
证据：现有可执行测试 changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness 以普通平台管理员直接 inactive→replacement active，已运行通过；本方另用 REPL 验证同步到 merchants。实际下游 JdbcSettlementInvoiceRepository.java:111-124 用 COALESCE(m.settlement_entity_id,m.business_entity_id) 读取结算主体，故不是无业务效果的备注字段。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-04 P1 冲突
问题：主体绑定的 effectiveFrom 不控制生效：明年生效也立即 active 并同步商户；停用又保留商户当前投影，且产生 effectiveTo 早于 effectiveFrom 的记录。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:683-697、733-749；JdbcPlatformRepository.java:551-560
冻结判据：PGM-026 / GEN-006 / GEN-008
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-026：所有调整一律只对生效时间之后的业务有效，不追溯改变历史合同、订单、票据和结算快照。
证据：PROBE_FUTURE=2027-09-14;STATUS=active;CURRENT_ENTITY=be_future；停用后 PROBE_INACTIVE=activeBinding:false;CURRENT_ENTITY=be_future，记录 effectiveTo=2026-09-14 < effectiveFrom=2027-09-14。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-05 P2 冲突
问题：空间类型删除后置 disabled，而恢复只接受 archived；存档也拒绝 disabled，新增同编码又拒绝，形成不可恢复记录。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:487-488、499-509、567-570
冻结判据：BSM-004 / GEN-002 / GEN-009
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：数据始终留在库里可查、可审计、可恢复
证据：真实服务完整执行存档→申请删除→最高权限批准→unarchive，PROBE_RESTORE=SPACE_TYPE_NOT_ARCHIVED;STATE=disabled。全仓搜索未发现其他空间类型恢复方法；现有测试只覆盖存档恢复，没有删除恢复。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-06 P1 冲突
问题：新建空间类型和商户删除审批的审计事件缺对象 ID；商户存档/删除理由只在可变业务行中，恢复或驳回会清空，审计 metadata 全为空，不能保留完整历史理由与裁决。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:209-239、298-339、443-469、916-924、966-982
冻结判据：BSM-004 / PGM-001 / PGM-066 / GEN-011
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：谁在什么时候、以什么理由删了什么，都有记录可查。
证据：PROBE_TYPE_AUDIT=[businessId=null;metadata={}]；PROBE_DELETE_AUDIT 同样为空；archive/request/approve/unarchive 五类 merchant 事件 metadata={}，恢复后 CURRENT_REASON=null。AuditEventService 只保存调用方 metadata，不自动取请求；JdbcAuditEventRepository 按 business_id 精确查询，无法从上述空ID反查。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-07 P2 细化
问题：空间类型审批完成后，删除申请表的 status 仍为 pending，没有写入最终审批结果，申请记录与空间类型最终状态不一致。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:515-574；JdbcPlatformRepository.java:385-403
冻结判据：GEN-006 / GEN-009 / GEN-011；上位 BSM-004
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：删除走审批、要留痕。
证据：正常路径源码证据：请求 INSERT status=pending，审批只更新空间类型及通用审计事件；全文搜索申请表无终态更新。额外故障注入观察到两次写入缺原子性，但这不作为独立待修项，不据此增加新流程或重试框架。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-08 P2 细化
问题：删前占用统计把所有 merchant_id 历史行都当作活占用，已失效主体绑定也阻止软删除已退出商户。
位置：backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:196-226
冻结判据：BSM-004 / GEN-002 / GEN-006
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：系统会自动检查这条数据还有没有被业务在用（合同、账单、结算、订单、已发权益、库存等），只要还有占用就拦住不让删，避免删出一堆"找不到主人"的记录。
证据：真实 complex_test JDBC Repository：在可回滚事务中仅创建已终止/存档商户和 effectiveTo 已过期、status=inactive 的历史绑定，PROBE_HISTORICAL_BINDING_BLOCKS_DELETE=1；回滚后商户残留数0。统计没有任何活跃状态/已完成判定。不要通过排除仍有效合同或资金占用扩大删除范围。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-09 P2 细化
问题：容量、面积、租金等数量缺最低有效范围校验，-1 被当正常值保存；DTO无 Bean 约束，数据库同样没有数值 CHECK。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:403、616、885-889；dto/ProjectRequest.java、dto/SpaceRequest.java、dto/MerchantProfileRequest.java
冻结判据：GEN-004 / GEN-006；上位 PGM-020、PGM-026
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-020：记录项目状态、负责人、地址和经营范围。PGM-026：绑定空间、业态、合同和经营范围。空间用途与容量亦属于业务对象有效数据。
证据：PROBE_NEGATIVE_CAPACITY=-1；PROBE_MERCHANT_NEGATIVE=-1;RENT=-1。这里只要求拒绝负物理量/负租金，未发明任何业务上限；存储列/约束已通过 pg_constraint 实查。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-10 P2 细化
问题：初建商户可直接填不存在的经营/合同/结算主体，未走 requireBusinessEntity，也未建立对应唯一绑定，形成与绑定表不一致的主体投影。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:874-880；dto/MerchantProfileRequest.java:12-14
冻结判据：PGM-026 / BEQR-143 / GEN-004 / GEN-006
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-026：把一个商户的“经营对象、运营主体、合同主体、结算主体”绑成一组唯一关系；合同、结算、权限、看板、第三方接入都引用同一组绑定。
证据：PROBE_MERCHANT_NEGATIVE 输出 ENTITY=nonexistent；创建过程未添加绑定。真实数据库 merchants 三主体列无外键。区别于 CX-03 的后续权责变更审批，此项是初建引用完整性。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-12 P2 冲突
问题：对外校验文案带 active、platform 等英文业务状态/角色列表；共用翻译器对含中文的整句原样透传。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:143-145、763-764、773-775、784-797、806-807、816-817
冻结判据：PGM-001 / PGM-066 / GEN-008 / GEN-015
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：PGM-001：展示给用户的一切文字……必须全部为中文，不得出现任何英文（如 active/draft 英文状态）。
证据：真实 service 错误通过 FieldLabels.translate 后：主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)。错误码等内部标识可以保留，本项只针对展示 message。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。

### CX-13 P1 冲突
问题：普通商户档案更新能将正常商户立即改为 terminated，没有商户退出缓冲期或退出生效日期。
位置：backend/src/main/java/com/complex/platform/governance/PlatformService.java:780-788、850-901；PlatformController.java:85-91
冻结判据：BEQR-155 / GEN-006
HTML依据（原句或带条目编号的节选；疑似省略不新增含义）：商户退出不是立即生效，一律设缓冲期（业务参数，按项目可配，上下限出厂为三十至六十天，出厂默认三十天）。
证据：真实服务 REPL 更新现有 active 商户，仅传普通档案字段和 terminated，立即返回 PROBE_IMMEDIATE_EXIT=terminated。全仓生产 Java/迁移搜索 merchant_exit、MerchantExit、退出缓冲、exit_buffer、exitBuffer 只命中本服务一条说明性注释，没有可验证的生效机制；此处针对直接终止入口，不把存档当作退出，不要求新增审批。
依据类型：HTML逐字原文 + 可执行产物实际行为/可复现命令输出。源码定位由SSH按UTF-8逐行读取，读到的实现事实仅用于复现和缺陷定位，不作为新业务规则。


## 对抗式：实际执行、输出与限制
命令在唯一正本运行，使用项目现有JDK21/Maven与依赖。
1. cd backend; mvn -o -q -Dtest=PlatformServiceTests,PlatformControllerTests test
   实际exit=0，46+12 tests，errors=0，failures=0，skipped=0。
2. cd backend; mvn -o -q -Dtest=PlatformJdbcIntegrationTests test
   目标仅complex_test，实际exit=0，17 tests，errors=0，failures=0，skipped=0。
   注意测试启动按项目配置自动执行了仓库既有V424/V425/V426，测试库从423到426；不是生产库，不是本轮新增迁移，不修改三份HTML。
3. 下附五段JShell原文；使用新鲜 TEST-com.complex.platform.governance.PlatformServiceTests.xml 内 java.home/java.class.path 启动 jshell，经stdin投递。JShell exit=0不代表每个业务场景PASS；PROBE行是本报告使用的反例证据。in-memory fixture不修改真实商户。第三段真实JDBC位于外层回滚事务，回滚后残留=0。
4. 只读约束检查：psql通过COMPLEX_DATABASE环境从配置读取凭证，固定complex_test，查pg_constraint及pg_class；无凭证输出。项目/空间/商户/绑定未有可代替所述输入与对象归属校验的数据库约束，relrowsecurity/relforcerowsecurity均false。空间类型状态迁移前后分别核对，未将V423旧约束当V426当前事实。
5. 搜索命令：rg -n 'space_type_delete_requests' backend/src/main/java backend/src/main/resources/db/migration；正常删除申请写pending，批准无终态更新。rg -n 'merchant_exit|MerchantExit|退出缓冲|exit_buffer|exitBuffer' backend/src/main/java backend/src/main/resources/db/migration 仅命中PlatformService:212注释，随后执行直接terminated探针。检索未命中不单独当缺陷，和真实行为一并判断。

### 探针1
```java
import com.complex.platform.governance.*;
import com.complex.platform.governance.dto.*;
import com.complex.platform.access.*;
import com.complex.platform.common.api.*;
import com.complex.platform.support.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
class Probe { Object fixture; PlatformService s; PlatformRepository r; Probe() throws Exception {Class<?> c=Class.forName("com.complex.platform.governance.PlatformServiceTests");var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);fixture=ctor.newInstance();var m=c.getDeclaredMethod("setUp");m.setAccessible(true);m.invoke(fixture);var sf=c.getDeclaredField("service");sf.setAccessible(true);s=(PlatformService)sf.get(fixture);var rf=c.getDeclaredField("platformRepository");rf.setAccessible(true);r=(PlatformRepository)rf.get(fixture);} MerchantRecord merchant(String key)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedMerchant",String.class);m.setAccessible(true);return (MerchantRecord)m.invoke(fixture,key);} void entity(String id)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedEntity",String.class);m.setAccessible(true);m.invoke(fixture,id);} void master()throws Exception{var m=fixture.getClass().getDeclaredMethod("asMasterAuthority");m.setAccessible(true);m.invoke(null);} }
var p=new Probe();
p.s.archiveSpaceType("sptype_booth","恢复验证");p.s.requestDeleteSpaceType("sptype_booth","恢复验证");p.master();p.s.approveDeleteSpaceType("sptype_booth","approved");
try {p.s.unarchiveSpaceType("sptype_booth");System.out.println("PROBE_RESTORE=RESTORED");}catch(ApiException e){System.out.println("PROBE_RESTORE="+e.code()+";STATE="+p.r.findSpaceTypeById("sptype_booth").get().status());}
var b=new Probe();var m=b.merchant("future");b.entity("be_future");
var binding=b.s.createBusinessEntityBinding(new BusinessEntityBindingRequest("future1",null,List.of(),m.id(),"be_future","be_future","be_future",LocalDate.now().plusYears(1),"明年生效"));
System.out.println("PROBE_FUTURE="+binding.effectiveFrom()+";STATUS="+binding.status()+";CURRENT_ENTITY="+b.r.findMerchantById(m.id()).get().settlementEntityId());
b.s.changeBusinessEntityBinding(binding.id(),new StatusChangeRequest("stop","inactive","停用",List.of()));
System.out.println("PROBE_INACTIVE=activeBinding:"+b.r.findActiveBusinessEntityBindingByMerchantId(m.id()).isPresent()+";CURRENT_ENTITY="+b.r.findMerchantById(m.id()).get().settlementEntityId());
var q=new Probe();var space=q.s.createSpace(new SpaceRequest("no-project",null,List.of(),null,null,null,"无项目空间","shop",null,null,null,"active"));System.out.println("PROBE_PROJECT_REQUIRED="+space.projectId());
var pa=q.s.createProject(new ProjectRequest("prja",null,List.of(),null,"complex","项目甲",null,null,null,"active"));var pb=q.s.createProject(new ProjectRequest("prjb",null,List.of(),null,"complex","项目乙",null,null,null,"active"));var sa=q.s.createSpace(new SpaceRequest("spa",null,List.of(),pa.id(),null,null,"甲空间","shop",null,null,null,"active"));var sb=q.s.createSpace(new SpaceRequest("spb",null,List.of(),pb.id(),null,sa.id(),"乙空间","shop",null,null,null,"active"));System.out.println("PROBE_PARENT_CROSS_PROJECT="+!sb.projectId().equals(sa.projectId()));
var actor=new AccessActor("employee-a","employee-a","employee",pa.id(),null,"active");
org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor,"token",List.of()));
new AuthorizationService(null).requirePlatformScope();System.out.println("PROBE_SCOPE=employeeProject:"+pa.id()+";RETURNED_PROJECTS="+q.s.listProjects().items().stream().map(ProjectRecord::id).toList());
var neg=new Probe();var np=neg.s.createProject(new ProjectRequest("negative",null,List.of(),null,"complex","负容量项目",null,null,-1,"active"));System.out.println("PROBE_NEGATIVE_CAPACITY="+np.capacity());
TestSecurityActors.clear();
/exit

```
实际输出：
```text
jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> PROBE_RESTORE=SPACE_TYPE_NOT_ARCHIVED;STATE=disabled
jshell> jshell> jshell> PROBE_FUTURE=2027-09-14;STATUS=active;CURRENT_ENTITY=be_future
jshell> $21 ==> BusinessEntityBindingRecord[id=852b4950-df54-4ae7-90fa-8f340c8ec645, merchantId=mch_900b9c46-537b-44aa-b2d9-3317fe592c07, operationEntityId=be_future, contractEntityId=be_future, settlementEntityId=be_future, status=inactive, effectiveFrom=2027-09-14, effectiveTo=2026-09-14, changeReason=停用, createdAt=2026-09-14T15:17:51.500588880Z, updatedAt=2026-09-14T15:17:51.912444750Z]
jshell> PROBE_INACTIVE=activeBinding:false;CURRENT_ENTITY=be_future
jshell> PROBE_PROJECT_REQUIRED=null
jshell> PROBE_PARENT_CROSS_PROJECT=true
jshell> jshell> jshell> PROBE_SCOPE=employeeProject:prj_470bd63e-191f-42db-a63a-25ad7a5fe43a;RETURNED_PROJECTS=[prj_470bd63e-191f-42db-a63a-25ad7a5fe43a, prj_b5257457-e7ea-46eb-8b09-0b6d1087c039]
jshell> PROBE_NEGATIVE_CAPACITY=-1
JSHELL_EXIT=0
```

### 探针2
```java
import com.complex.platform.governance.*;
import com.complex.platform.governance.dto.*;
import com.complex.platform.access.*;
import com.complex.platform.common.api.*;
import com.complex.platform.support.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
class Probe { Object fixture; PlatformService s; PlatformRepository r; Probe() throws Exception {Class<?> c=Class.forName("com.complex.platform.governance.PlatformServiceTests");var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);fixture=ctor.newInstance();var m=c.getDeclaredMethod("setUp");m.setAccessible(true);m.invoke(fixture);var sf=c.getDeclaredField("service");sf.setAccessible(true);s=(PlatformService)sf.get(fixture);var rf=c.getDeclaredField("platformRepository");rf.setAccessible(true);r=(PlatformRepository)rf.get(fixture);} MerchantRecord merchant(String key)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedMerchant",String.class);m.setAccessible(true);return (MerchantRecord)m.invoke(fixture,key);} void entity(String id)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedEntity",String.class);m.setAccessible(true);m.invoke(fixture,id);} void master()throws Exception{var m=fixture.getClass().getDeclaredMethod("asMasterAuthority");m.setAccessible(true);m.invoke(null);} }

import com.complex.platform.evidence.*;
import org.mockito.Mockito;
var a=new Probe();var ty=a.s.createSpaceType(new SpaceTypeRequest("audit-type",null,List.of(),"auditkind","审计种类",10,"active"));
var field=a.fixture.getClass().getDeclaredField("auditEventRepository");field.setAccessible(true);var audit=(InMemoryAuditEventRepository)field.get(a.fixture);
System.out.println("PROBE_TYPE_AUDIT="+audit.events.stream().filter(x->x.eventName().equals("space_type.updated")).map(x->"businessId="+x.businessId()+";metadata="+x.metadata()).toList());
var mer=a.merchant("audit-merchant");a.s.archiveMerchant(mer.id(),new MerchantArchiveRequest("ma","保留历史理由"));a.s.requestDeleteMerchant(mer.id(),new MerchantDeleteRequest("md","删除申请理由"));a.master();a.s.approveDeleteMerchant(mer.id(),new MerchantDeleteApprovalRequest("map","approved","同意"));
System.out.println("PROBE_DELETE_AUDIT="+audit.events.stream().filter(x->x.eventName().equals("merchant.delete_approved")).map(x->"businessId="+x.businessId()+";metadata="+x.metadata()).toList());
a.s.unarchiveMerchant(mer.id(),new MerchantArchiveRequest("mu",null));
System.out.println("PROBE_REASON_HISTORY="+audit.events.stream().filter(x->x.businessType().equals("merchant")).map(x->x.eventName()+":"+x.metadata()).toList()+";CURRENT_REASON="+a.r.findMerchantById(mer.id()).get().deleteReason());
var x=new Probe();var xmm=x.merchant("status");var changed=x.s.updateMerchant(xmm.id(),new MerchantProfileRequest("status-black",null,List.of(),"测试商户","other",null,null,null,null,null,null,null,null,null,null,null,null,null,"blacklisted"));System.out.println("PROBE_BLACKLIST_DIRECT="+changed.status());
var spy=Mockito.spy(x.r);Mockito.doThrow(new IllegalStateException("delete request write failed")).when(spy).saveSpaceTypeDeleteRequest(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
var audit2=new AuditEventService(new InMemoryAuditEventRepository());var s2=new PlatformService(spy,new AuditedOperationExecutor(new com.complex.platform.common.idempotency.IdempotencyService(new InMemoryIdempotencyRepository(),new com.fasterxml.jackson.databind.ObjectMapper()),audit2),audit2,new AuthorizationService(null));
s2.archiveSpaceType("sptype_booth","测试");
try{s2.requestDeleteSpaceType("sptype_booth","测试");}catch(Exception e){System.out.println("PROBE_PARTIAL_REQUEST="+e.getMessage()+";TYPE_STATUS="+spy.findSpaceTypeById("sptype_booth").get().status()+";REQUESTER="+spy.findSpaceTypeDeleteRequestedBy("sptype_booth"));}
TestSecurityActors.clear();
/exit

```
实际输出：
```text
jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> PROBE_TYPE_AUDIT=[businessId=null;metadata={}]
jshell> PROBE_DELETE_AUDIT=[businessId=null;metadata={}]
jshell> PROBE_REASON_HISTORY=[merchant.updated:{}, merchant.archived:{}, merchant.delete_requested:{}, merchant.delete_approved:{}, merchant.unarchived:{}];CURRENT_REASON=null
jshell> PROBE_BLACKLIST_DIRECT=blacklisted
jshell> jshell> jshell> PROBE_PARTIAL_REQUEST=delete request write failed;TYPE_STATUS=pending_delete;REQUESTER=null
JSHELL_EXIT=0
```

### 探针3
```java
import com.complex.platform.governance.*;
import com.complex.platform.common.crypto.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.transaction.support.*;
import java.util.*;
var ds=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:5432/complex_test","complex",System.getenv("COMPLEX_DATABASE_PASSWORD"));
var jdbc=new JdbcTemplate(ds);
var c=Class.forName("com.complex.platform.governance.JdbcPlatformRepository").getDeclaredConstructors()[0];c.setAccessible(true);
var repo=(PlatformRepository)c.newInstance(new NamedParameterJdbcTemplate(ds),new ObjectMapper(),org.mockito.Mockito.mock(PiiCipher.class));
var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
tx.execute(st->{try{jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,archived_at,delete_status) VALUES ('cxr_m009_retired','已退出商户','other','terminated',now(),now(),now(),'pending')");jdbc.update("INSERT INTO business_entity_bindings(id,source_entity_type,source_entity_id,target_entity_type,target_entity_id,binding_type,status,created_at,updated_at,merchant_id,effective_from,effective_to) VALUES ('faaa0000-0000-0000-0000-000000000009','merchant','cxr_m009_retired','business_entity','old_entity','four_party','inactive',now(),now(),'cxr_m009_retired',DATE '2026-01-01',DATE '2026-01-02')");System.out.println("PROBE_HISTORICAL_BINDING_BLOCKS_DELETE="+repo.countMerchantReferences("cxr_m009_retired"));return null;}finally{st.setRollbackOnly();}});
System.out.println("PROBE_SQL_ROLLBACK="+jdbc.queryForObject("SELECT count(*) FROM merchants WHERE id='cxr_m009_retired'",Integer.class));
/exit

```
实际输出：
```text
jshell> jshell> PROBE_HISTORICAL_BINDING_BLOCKS_DELETE=1
jshell> PROBE_SQL_ROLLBACK=0
JSHELL_EXIT=0
```

### 探针4
```java
import com.complex.platform.governance.*;
import com.complex.platform.governance.dto.*;
import com.complex.platform.access.*;
import com.complex.platform.common.api.*;
import com.complex.platform.support.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
class Probe { Object fixture; PlatformService s; PlatformRepository r; Probe() throws Exception {Class<?> c=Class.forName("com.complex.platform.governance.PlatformServiceTests");var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);fixture=ctor.newInstance();var m=c.getDeclaredMethod("setUp");m.setAccessible(true);m.invoke(fixture);var sf=c.getDeclaredField("service");sf.setAccessible(true);s=(PlatformService)sf.get(fixture);var rf=c.getDeclaredField("platformRepository");rf.setAccessible(true);r=(PlatformRepository)rf.get(fixture);} MerchantRecord merchant(String key)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedMerchant",String.class);m.setAccessible(true);return (MerchantRecord)m.invoke(fixture,key);} void entity(String id)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedEntity",String.class);m.setAccessible(true);m.invoke(fixture,id);} void master()throws Exception{var m=fixture.getClass().getDeclaredMethod("asMasterAuthority");m.setAccessible(true);m.invoke(null);} }

var a=new Probe();
try {a.s.createBusinessEntity(new BusinessEntityRequest("invalid",null,List.of(),"主体","bad-role",null,null,null,"active"));}catch(ApiException e){System.out.println("PROBE_API_MESSAGE="+com.complex.platform.common.api.FieldLabels.translate(e.getMessage()));}
var p=a.s.createProject(new ProjectRequest("malformed",null,List.of(),null,"complex","项目",null,null,null,"active"));
var mr=a.s.createMerchant(new MerchantProfileRequest("negative-money",null,List.of(),"负金额商户","other","nonexistent",null,null,null,null,null,null,new java.math.BigDecimal("-1"),null,null,null,new java.math.BigDecimal("-1"),null,"active"));
System.out.println("PROBE_MERCHANT_NEGATIVE="+mr.areaSquareMeter()+";RENT="+mr.rentPerSquareMeter()+";ENTITY="+mr.businessEntityId());
TestSecurityActors.clear();
/exit

```
实际输出：
```text
jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> PROBE_API_MESSAGE=主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)
jshell> jshell> jshell> PROBE_MERCHANT_NEGATIVE=-1;RENT=-1;ENTITY=nonexistent
JSHELL_EXIT=0
```

### 探针5
```java
import com.complex.platform.governance.*;
import com.complex.platform.governance.dto.*;
import com.complex.platform.access.*;
import com.complex.platform.common.api.*;
import com.complex.platform.support.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
class Probe { Object fixture; PlatformService s; PlatformRepository r; Probe() throws Exception {Class<?> c=Class.forName("com.complex.platform.governance.PlatformServiceTests");var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);fixture=ctor.newInstance();var m=c.getDeclaredMethod("setUp");m.setAccessible(true);m.invoke(fixture);var sf=c.getDeclaredField("service");sf.setAccessible(true);s=(PlatformService)sf.get(fixture);var rf=c.getDeclaredField("platformRepository");rf.setAccessible(true);r=(PlatformRepository)rf.get(fixture);} MerchantRecord merchant(String key)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedMerchant",String.class);m.setAccessible(true);return (MerchantRecord)m.invoke(fixture,key);} void entity(String id)throws Exception {var m=fixture.getClass().getDeclaredMethod("seedEntity",String.class);m.setAccessible(true);m.invoke(fixture,id);} void master()throws Exception{var m=fixture.getClass().getDeclaredMethod("asMasterAuthority");m.setAccessible(true);m.invoke(null);} }


var a=new Probe();var m=a.merchant("exit");
var updated=a.s.updateMerchant(m.id(),new MerchantProfileRequest("immediate-exit",null,List.of(),"立即退出测试","other",null,null,null,null,null,null,null,null,null,null,null,null,null,"terminated"));
System.out.println("PROBE_IMMEDIATE_EXIT="+updated.status()+";EXPLICIT_APPROVAL=false;EXIT_DATE_INPUT=false");
TestSecurityActors.clear();
/exit

```
实际输出：
```text
PROBE_IMMEDIATE_EXIT=terminated;EXPLICIT_APPROVAL=false;EXIT_DATE_INPUT=false
JSHELL_EXIT=0
```


## 已排除的误报及边界
- 已验证共享写事务及幂等响应加密，未重复报“无事务/响应明文”。
- 暂停/黑名单原句“暂停与列入黑名单由招商部门与运营部门决定”；“联合审批”不是原句，不以单次设置blacklisted构造新审批要求。
- 空间类型请求写失败探针只表明当前原子性不足的故障表现；本批CX-07只要求现有申请记录与正常审批终态一致，不另增重试、计数器或流程状态。
- 未因未来多项目/集团需求就扩建新架构，未以其它模块函数未在当前20文件出现而判缺陷。
- 未修复、未提交、未推送、未改变任何业务HTML。测试生成文件只在项目既有target中；未落新探针/适配器/日志文件。用户原有dirty/untracked保持原样。

## 技能与方法记录
已实际读取并使用：using-superpowers、verification-before-completion（证据门槛）、grilling（本方自问，不派代理）、codebase-design、diagnosing-bugs、systematic-debugging（真实调用链和反例）、writing-for-agents（可定位报告）、ponytail-review（只检复杂度，不替代正确性审计）。本次未改源码，无可归因复杂度删改；没有为了技能创建额外角色/产物。

## B1结论
FAIL。已独立审完原M009全部20文件和87判据，发现12项已确认问题；现有75测试通过不能消除反例。下一步仅在两席raw齐备后做B2整批归并，未批准任何源代码修复。



## M009 新授权轮 B2 第1次 Codex复核票
版本：V0.05
阶段：B2整批候选复核；本次不写锁哈希行。
输入：modules/M009-b1-self-raw.md本次B1 V0.04；modules/M009-b1-grok-raw.md本次B1 V0.05；findings.md V2.03顶部20行候选；ledger.md V3.53。
操作员对013回复原文：“同意上述具体范围”。批准后端/数据库三个暂可空资料字段及必要读写验证，不含页面和旧项目补录入口；不得再次为同一范围要求确认。

本席B2结论：FAIL。
全部20行已核对，没有以本票宣布B1/B2或模块PASS。核心事实有证据，正式ID与归并数量无漏项，但候选尚需定稿修正后才能作为整批B3执行边界：
1. 030同时列“冲突（Grok）/细化（Codex）”与“待B2复核”，不是已统一的最终分类。须依据PGM-026和真实调用链明确：非法主体引用必须拦截；“每次初建商户是否自动建绑定”不能凭意见强加。
2. 038的“复用项目作用域”尚未列清受影响入口和无法直接按project_id过滤的数据对象。AuthorizationService.requirePlatformScope:481–497放行无merchantId的普通employee，并不保证项目隔离；requireProjectScope:288–293把actor.organizationId作为当前项目。项目/空间可按已有id/project_id限定，商户/经营主体表没有同名project_id。修复前必须说清最小可执行边界，不能未经授权新增项目归属字段，也不能把一个守卫误当整个隔离已完成。
3. 031/040/041须一起限定最小处置：停用/暂停/替换不能借普通入口改变财税主体，直接清空商户结算主体同样是改变。未来绑定不能提前生效；LeasingService:300–311会把合同effectiveFrom传入，不能只修人工表单而让签约路径提前切换。保持历史快照，不新造审批或调度系统。应把具体拒绝/保留/生效分支写入定稿。
4. 013既有ProjectRequest/ProjectRecord的构造调用已检索；新增字段会影响Java构造及SQL映射，需在定稿明确兼容旧调用的最小方式与读回验证。已获授权，可执行性问题不再触发同一审批。
5. 043应同时覆盖对象ID、历史理由、实际决定；只改事件名字不能使缺对象ID/空metadata的证据链完整。

逐行核对：
|ID|本席事实/队列意见|
|---|---|
|013|缺失属实，授权明确；候选队列是，按获批范围落实。|
|018|立即terminated可执行反例成立；收窄普通档案更新通道，不把存档当退出、不新增所谓联合审批。|
|030|两席非法引用事实一致；队列是，分类与初建绑定边界需统一。|
|031|停用投影不一致已复现；队列是，和040/041合并设计但保留独立ID。|
|038|项目普通账号跨项目读取事实成立；队列是，范围须写清，不能默认新建商户/公司归属能力。|
|039|空间空项目及父跨项目成立；队列是，复用现有字段校验。|
|040|普通入口缺财税主体变更审核成立；队列是，优先收窄已有入口。|
|041|未来日期立即投影成立；队列是，须覆盖签约调用链。|
|042|disabled无法恢复成立；队列是，复用已有恢复入口。|
|043|缺对象ID/理由丢失、驳回事件名错误成立；队列是，复用审计承载。|
|044|正常审批后申请status仍pending成立；队列是，更新已有申请终态。|
|045|真实JDBC仅有失效绑定也阻止软删成立；队列是，只收窄失效绑定，不放开资金/有效合同占用。|
|046|负数量可保存成立；队列是，不添加新上限或价格规则。|
|047|已真实验证含中文的英文列表原样透传；队列是，仅展示message中文化。|
|048|有PGM-027/BSM-072上位项，候选细化、队列否；请Grok复核原技术性分类调整。|
|049|有BEQR-166上位项，候选细化、队列否。|
|050|有敏感资料控制上位项，候选细化、队列否。|
|051|资源分页，无业务发明；技术性、队列否。|
|052|BSM-004现有存档审批承载；细化、队列否，恢复缺陷另归042。|
|053|package声明无业务发明；技术性、队列否。|

本票依据：当前HTML、已落盘的实际探针与75项测试证据，以及B2新读的AuthorizationService、LeasingService、ProjectRequest/Record调用点。未修改业务代码、HTML或测试。等待Grok完整B2票后整批修订候选，再重复B2；本轮B3仍0/3。

## M009 新授权轮 B2 收到Grok第1票后的Codex归并裁定
版本：V0.06
输入：本文件B1 V0.04和B2 V0.05；modules/M009-b1-grok-raw.md B1 V0.05及刚交付B2 V0.06；findings.md V2.03。
Grok真实EXIT_CODE=0，本次完整B2追加后166965bytes，SHA256 b53dc0cf008823b261111180d044759cc54c0bc3c89801fda9a89de67125259b；原146230bytes前缀SHA保持ffc57c87438442c0afceeed9848721abf2a75b5655cbd81a17673a8ed0b2daf9。Grok票PASS，不代其改写。

本方结论：FAIL；双席合取仍FAIL，B3=0/3。
全部20项的事实/队列复核保留本方V0.05逐项表。接受对030统一为细化/工程缺陷是/队列是，仅校验已提供主体ID，不强制建档自动绑定；接受048/049/050/052归细化、051/053归技术性，六项均否；013授权有效、不重问。
038不能按Grok“仅项目/空间、platform_admin保持全量、商户跨项目残留不修”的意见放行。PGM-041/BEQR-152明确所有业务数据默认项目隔离，平台身份不能被当作获授跨项目数据范围；AuthorizationService.currentProjectScopeQuietly注释和行为同样不因平台管理员返回空全量。项目/空间修复只能关闭其中一部分，商户及公司/绑定残留仍属本ID事实，不另开ID或单方删掉。
已补查PGM-018原文（HTML415–418）：公司与项目明确多对多。V410迁移已删除旧parent_entity_id固定父链；现有MerchantProfileRequest、BusinessEntityRequest、主体SQL及迁移没有可复用的完整商户项目归属/公司项目关联维护。不能用公司ID当项目ID，不能为通过而从一份历史合同、空间或审计行猜归属。需要新增承载与必要后端维护，超出013授权。
因此铁律6必要新增触发，完整038提案置于findings.md V2.04；ledger转BLOCKED-ASK。在操作员答复前不启动B3、不启动下一模块或新Grok调用。此为已有HTML需求的实现缺口，不是无来源业务添加，不新增2.2 questions文件。

031/040/041仍须按三项一起设计：普通财税主体变化禁止；未来日期不提前投影；新建同一服务入口覆盖签约调用；不能用清空投影规避审批。045排除的仅真正失效历史绑定，不应把未来尚未生效的记录误作无占用。043包括商户与空间类型的实际拒绝决定、对象ID、理由。上述不在B2期间实施。
外部主分支从c039798a8a89993ace0d03bad566665d37a1877e推进至e456ba96c84185c8d39f18f548011daac48a6466，仅三份M046认证测试变化；M00920文件和三HTML SHA与B1相同，保留他人改动，之后B3如获准须重新取完整工作区tree基线。

## M009第二批B2 Codex复核票
版本：V0.07
输入：findings.md V2.06当前20行完整候选，SHA256 24c96cdb46ccf686356ed8ac6d62a93a53a851d1c34a551bad5c4662f473d551；modules/M009-b1-self-raw.md的本次B1 V0.04及第一批B2 V0.05/V0.06；modules/M009-b1-grok-raw.md本次B1 V0.05及第一批B2 V0.06。本批Grok V0.07仍在执行，未读取其本次在写的追加段。
本席结论：FAIL。不是授权缺口；013/038均已批准，B3仍0/3。

全20行差集、ID、分类及队列无漏项；14个缺陷是，6个登记否。新授权不使遗留跨项目全表查询合法。本轮需要在B3前把038维护入口写清：
1. 商户归属补录应只更新project_id，保留全部其它档案字段。V2.06写“复用updateMerchant”而当前saveMerchant首先必填merchantName/merchantType并全量覆盖联系人等资料（PlatformService:865–899），当前查询又正确不向普通列表开放无归属档案。不能要求操作人为了补一个项目提交或猜完整档案。最小可执行方式：一个项目归属专用后端操作，携带idempotencyKey/projectId/reason；只允许null→明确项目或同值重放，禁止跨项目迁移，数据库条件更新防覆盖；普通岗位不能认领未归属对象，复用现有USB最高权限及既有功能权限/审计。公司关联维护可共享同一个简短项目关联请求记录，无额外业务状态。此在038已获批的必要维护接口范围内，无需重问操作员。
2. 公司创建的项目输入须说清：旧BusinessEntityRequest没有projectId；默认由当前项目取得，若经现有USB最高权限显式建立到指定项目，应有可核验的目标项目输入而不能猜或要求伪造身份组织值。可在原BusinessEntityRequest追加可选projectId并保留旧构造；普通岗位只能当前项目，最高权限显式目标校验存在后创建/关联。本条仍不提供全局列表或新权限规则。
3. 未来未结束的inactive绑定在原“仅active”的数据库唯一索引之外。V2.06已经要求唯一，B3落实应在同一既有事务内锁住对应商户再检查/写绑定，使用数据库行锁防正常双请求分别插入。无需另设审计判据、状态或修复ID，归031/040/041的同一生效关系一致性；045继续计算未结束未来绑定为引用。

其它事项逐项票（事实及队列接受；不替整个批次PASS）：
|ID|分类/队列|核对结果|
|---|---|---|
|013|细化/是|获批三可空字段及兼容读写；无页面/旧项目编辑入口。|
|018|冲突/是|拒绝新建或普通状态切换直接terminated；保留原terminated不动的档案更新。|
|030|细化/是|提供ID校验存在及关联；缺省可空，不强制建档自动绑定。|
|031|冲突/是|普通生效绑定不能失效、倒置日期不写、历史结束期间不重新打开；见唯一性落实。|
|038|冲突/是|新增已批准；具体补录和公司新建项目输入需上述改清。|
|039|冲突/是|必填项目、父空间/商户同项目。|
|040|冲突/是|普通入口不能改变已有财税主体，最高权限不能替代财税并审。|
|041|冲突/是|未来inactive不投影，到期现有动作；Leasing前置三列必须去掉以覆盖实际顺序。|
|042|冲突/是|disabled复用恢复，pending_delete仍不旁路。|
|043|冲突/是|ID、理由、实际决定及两类驳回事件。|
|044|细化/是|本次申请approved/rejected与类型行同事务；不改旧历史。|
|045|细化/是|只排真正结束历史，未来未结束仍占用。|
|046|细化/是|容量/面积/租金单价负数拦截，可空/零保留，无新上限。|
|047|冲突/是|展示中文保留内部code。|
|048|细化/否|既有主体隔离查询承载，有上位条目，无新缺陷。|
|049|细化/否|既有审批持久化，有上位条目。|
|050|细化/否|PII加密脱敏，有上位条目。|
|051|技术性/否|分页资源承载，无业务发明。|
|052|细化/否|直接删除拒绝导向既有审批，无独立缺陷。|
|053|技术性/否|package编译声明，无业务发明。|

当前只读核验：Graft查询后读取实际AuthorizationService.isMasterAuthority（固定主授权身份）、permissionGrant（DENY优先、真实master分支），验证无需新增角色；实体/商户DTO与Leasing调用顺序已核。complex_test只读索引查询exit0：uq_business_entity_bindings_active_merchant仅保护active，不能代替未来未结束记录的一致性。一次额外聚合误用了不存在的users表，SQL exit1，未据其给结论；随后改为实际schema元数据及目标索引查询exit0，未读写业务库complex，未改测试/源码。
待本批Grok完整票落齐后，按双方全部意见整批修订候选并重做B2。不得现在局部修代码，仍IN_PROGRESS。



## M009第三批B2 Codex完整复核票
版本：V0.08
本席结论：PASS（仅本批B2候选票；不是B3修复完成、不是模块PASS）。
输入：findings.md V2.07当前20行完整候选，833659bytes，SHA256 7276a84f4af5d44937de776329b6cb151501cf0c84e8839a4f194b259a8d1178；本席modules/M009-b1-self-raw.md V0.04 B1与V0.05/V0.06/V0.07 B2；对方modules/M009-b1-grok-raw.md V0.05 B1、V0.06/V0.07 B2。第二批Grok真实exit0，追加后190101bytes SHA256 5c90c2708cc3f19fcd52d7ad41bd1f4d64817d93ce2a370d4661be445e0527e6，原166965bytes前缀原样保留。本次Grok V0.08仍在运行，未读其在写内容。

差集完整：CX-01…10、12、13和GR-01…05及T-01…06全部归并；CX-11黑名单联合审批仍排除。20行9冲突+5有缺陷细化入队是，6登记否；旧013/018/030/031沿用，旧021由043覆盖不复开，旧其它001–037保持否。不新增判据/角色/业务状态/审批链/报告类型。013/038操作员授权均有效且完整保留，无未答问题。

第二批意见处理：
1. 本席指出归属补录不能全量覆盖档案，V2.07已改专用project_id条件更新；公司创建显式projectId与旧构造兼容已经写清。
2. Grok指出生产USB识别不一致，本席核到SessionIssuer:11–16的三常量、ProductionAuthService:373–414实际签发master_authority/platform-company；AuthorizationService:139–143却要求platform_admin。不能因为函数叫isMasterAuthority就假定它识别真实USB。本批改共用识别的账号类型条件，其余固定id/用户名保留，现有requirePermission已引用该函数，无需另写角色判断。Graft实际调用链已核，直接受影响的身份测试夹具改用真实类型，不改原断言业务含义。
3. 当前项目必须实际存在，非项目organizationId不退全表；缺上下文保留原403，非空无项目时列表空/total0。USB显式配置目标来自请求，不能与platform-company做相等要求；普通岗位仍不能认领或迁移。
4. 合取核到BSM-004 HTML570/576/577与现有requestDeleteMerchant/approveDeleteMerchant已有最高权限分支；这些按明确对象执行的原业务授权必须保留，否则新增scope把真实USB全封。候选已限定只保留原有最高权限删除/审批路径，不扩展到普通档案修改、全量列表或财税主体替换。来源是现有HTML和原分支，不是新增跨项目授权规则。
5. 双方共同指出未来inactive会逃出仅active唯一索引，V2.07已经明确创建与激活都检查active或inactive且effectiveTo为null，并在原事务锁商户行后检查/写入；不只是等激活才检查。Leasing已追到先建商户再绑主体的真实顺序，前置三列null和意向项目传入均保留。045不能排掉未来预约。

逐项完整票：
|ID|分类/队列|本席结论及边界|
|---|---|---|
|M009-038|冲突/是|PASS；接受双方修正；真实USB三件套、真实项目与显式配置分开，普通岗位不得认领；专用项目SQL不覆盖其它字段；公司多对多及COUNT/反查均项目过滤。AuthorizationService及直接身份夹具为同ID依赖。|
|M009-039|冲突/是|PASS；项目必填/当前项目/父空间及商户同项目；spaceExists同范围。|
|M009-040|冲突/是|PASS；普通财税身份替换/生效停用拒绝，实际USB也不能绕过；不清空已有主体。|
|M009-041|冲突/是|PASS；创建与激活共用未结束槽，事务内商户行锁；未来inactive无投影，Leasing前置三列null加项目传递。|
|M009-031|冲突/是|PASS；不写倒置结束日期、不重开历史期间；与040/041统一，不改历史快照。|
|M009-042|冲突/是|PASS；disabled可经恢复入口重启用；pending_delete仍不能绕过。|
|M009-043|冲突/是|PASS；真实对象ID、理由、决定和驳回事件；保留原明确最高权限删除业务路径及留痕。|
|M009-044|细化/是|PASS；只结清本次申请，与类型行同事务；最高权限直删本次请求同样结清。|
|M009-045|细化/是|PASS；只排真正结束inactive历史；未来未结束仍占用，其它引用保持。|
|M009-046|细化/是|PASS；负容量/面积/租金拦截，可空/零保留，无新上限。|
|M009-030|细化/是|PASS；提供主体ID才校验存在/本项目关系，缺省可空，不强制每次建档自动绑定。|
|M009-047|冲突/是|PASS；仅删用户中文message英文枚举括注，内部code保持。|
|M009-018|冲突/是|PASS；拒绝新建/普通更新立即terminated，原terminated无状态变化保存保持；黑名单联合审批不成立。|
|M009-013|细化/是|PASS；获批三可空字段后端/DB新建读取与兼容验证，无页面/旧项目编辑入口。|
|M009-048|细化/否|PASS；细化无独立缺陷，保留查询；其项目谓词修改归038，不另开ID。|
|M009-049|细化/否|PASS；已有上位审批持久化，登记否，无新角色/流程。|
|M009-050|细化/否|PASS；PII加密/脱敏有上位要求，无独立缺陷。|
|M009-051|技术性/否|PASS；分页纯接口资源承载，无业务发明。|
|M009-052|细化/否|PASS；既有删除审批流程细化，恢复缺陷单列042。|
|M009-053|技术性/否|PASS；package声明是编译单元，无业务发明。|

执行证据：本批只读实际源码/HTML与Graft调用链；Graft首次误用不存在的--path参数exit1，按实际--help改成--in后exit0取得10个测试文件相关身份位置，无据失败命令推结论。20源文件仍为B1原字节；未重复跑未改代码的75项既有基线测试，不把历史测试绿当修复验证。B3将按已读TDD技能做正常行为失败→最小实现→通过；不以编译失败当RED。
技能按节点沿用已实读的receiving-code-review、verification-before-completion、codebase-design、grilling、writing-for-agents及ponytail-review结构；本批未调用额外子代理或新增流程。
当前M009 IN_PROGRESS，B3=0/3；本票只使Codex第三批B2为PASS，仍等本次Grok完整票；任一FAIL不进入B3。


## 第四批 B2：整批候选 V2.09 独立复核（Codex）
版本：V0.09
候选SHA256: 121cca9e64a137a9fb500eae161a2c23c6104e7aaf5683311a7fac2269749d21
输入：modules/M009-b4-r1-self-raw.md末尾V0.02（SHA256 3ac0c87d592b3cbe0e3b7697e3112cd819d1c9e3649e35fed2c1e1f7eb98ea74）；modules/M009-b4-r1-grok-raw.md末尾V0.02（SHA256 92c5964ee2fbf14077f24dc595295a86ea1a0658bd75872a43dd72b1c688bdd1）。两份完整B4都已落齐，都是内容FAIL/归因PASS，各含全部185872byte binary diff。不是以旧B1或旧r1作为新票。
当前Grok B2已派发，本席在读取它本批B2投票前独立核候选并封存本票。无第三席、无B3源修改；只读源码及已封存原始结果。

|正式ID|分类|工程缺陷|队列归属|本席B2判断|
|---|---|---|---|---|
|M009-038|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-039|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-040|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-041|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-031|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-042|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-043|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-044|细化|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-045|细化|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-046|细化|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-030|细化|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-047|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-018|冲突|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-013|细化|是|是|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-054|冲突|是|是|PASS：有已封存B4事实及明确最小范围，见下|
|M009-055|细化|是|是|PASS：有已封存B4事实及明确最小范围，见下|
|M009-056|细化|是|是|PASS：有已封存B4事实及明确最小范围，见下|
|M009-048|细化|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-049|细化|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-050|细化|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-051|技术性|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-052|细化|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|
|M009-053|技术性|否|否|PASS：继承既有定稿及已批准边界；本次不擅改归属|

### 完整合并与分类
原14是完整保留（038、039、040、041、031、042、043、044、045、046、030、047、018、013），六否048—053保持登记。无任何一项因B4部分实现绿而提前翻否。新054—056使当前17是/6否，未增加锁外判据。
054合并CX-B4-01和GR-01同一根因。BSM-004删除由最高权限审批、持授权U盘本人发起无需审批、配置可恢复；真实生产MASTER_ACCOUNT_TYPE已在038校正，但单一显式对象生命周期入口的requirePlatformAdmin仍令真实最高权限不可达，因此按Grok归“冲突”，而不是本席B4分类建议的细化。最小范围按本候选仅改changePositionTemplateLifecycle，不改共享私有守卫或其它Access接口；保留原动作存档/恢复/申请/审批/驳回和所有其它检查。这样同时保留旧假最高权限测试曾声称可行的既有配置操作语义，与真实SessionIssuer一致，不创造新角色权限。非最高权限仍须平台管理员，普通管理员仍过不了审批内层最高权限检查。
055：原测试目的分别是储值结账/退款/幂等与通知、员工个人资料脱敏。缺有效商户范围时一律先抛MERCHANT_SCOPE_REQUIRED，未执行这些目标，符合GEN-003有效性缺陷。GR-02给出两资金类；本席B4实际94例基线输出明确另列M010隐私用例同样失败且该类在本批diff，归同一项不另扩模块。候选只改变合法测试操作人、同商户资料和既有可用调用前提；不会让无merchantId的USB得到全商户资金权限，不改生产服务；原金额、隔离、通知和隐私断言保留且仍能失败。
本席核读StoredValueService:318-320的consumeInternal调用consumeBalance(false)，344/818-827会执行商户卡范围；所以通知旧项目卡无merchantId调用无法通过当前正式规则，仅换一个actor仍不足。候选明确不伪造成功前提：项目卡充值通知保持；合法商户卡消费场景验证消费通知，旧无范围项目卡内部扣款明确验证拒绝，不对项目卡消费业务作完成声明。必要夹具必须使用实际商户卡数据与已存在资金路径，不能移除原金额/余额/card_type检查以过关。
056：M006的permission_grant_l3测试缺schedule/beneficiary。SupervisorReviewService:202-205先检查直接受益人，207-213继续检查受益人/申请人不能自审；测试因缺资料并未验证原本要验证的真实USB审批身份。只补这个模拟授权对象的明确受益人和既有schedule，不换actionType、不豁免受益人或利益冲突；先普通最高职级被拒、再真实USB可合法审批，仍验证原目标。归细化中的验证缺陷，GEN-003/005/008，不是添加业务规则。

### 事实分歧核实，不以席位结论替代证据
GR-02/03的“本批引入/本批夹具连带”因果标签拒绝采纳：本席B3已在before树的AuthorizationService与原platform_admin夹具组合上实跑94例，仍相同1failure+7errors，原输出封存在self B4。before与after currentMerchantScope均在merchantId空时抛错，根本不按platform_admin类型放行；且直接受益人要求在before同样生效。这证明是既有无效前提，不能证明是本次accountType改变引入。但不以“原先存在”宣布当前验证通过，已把相关直接验证范围纳入055/056。
Grok freshXML实查13类320例、1failure+13errors数字成立：包含AccessJdbc24、AccessService50、ProductionAuth8；M01044的mtime为较早本方调用，不在该次320中。原本方282/1failure+8errors是另一十一类组合，不是ProductionAuth8errors；Grok上票对派发的该说法无依据。未改AccessServiceTests六项受益人失败留观察，不扩当前生产修复或测试框架。
冻结plan/modules.md行125模块原统计2476行；本次before树20文件2879行、after3368行。两种事实都实算/实读过；本席B4“原20文件/2879行”仅指修复前字节统计，不能称冻结计划统计。当前候选明确区分。计划及锁不改。
ledger模块行原B3=0与顶部实际1不一致属于漏同步；候选记录修复为1，未额外执行B3，不是增加第二轮。

### 第一性原理与最小性
保留054是为了BSM-004已经定义的最高权限能执行现有配置流程；可执行反例已经在当前树与before身份+真实夹具分别复现，不靠注释判断。055/056是已有测试正确到达其目标行为的前提，删掉会继续出现与要验证行为无关的前置异常，不能依赖测试绿作为业务正确性的替代。
不存在新增主体、状态、收费、权限码、审批层级、项目卡消费通道或跨商户豁免。承载沿用现有记录/角色/测试工具/服务入口。没有新增依赖/抽象/模块清单。未把AccessService所有platform_admin门槛全局改松。
本批无需新操作员决定；013/038批准继续有效，HTML不改。不以此前PASS作为本批票。技能实际按收窄方式应用receiving-code-review（先反证因果标签）、grilling（检查真实卡范围/审批前提）、verification-before-completion（候选SHA与计数、冻结映射、代码路径证据）。

B2_VERDICT: PASS
范围：findings V2.09当前23行及全部执行边界。尚须Grok同一候选新票PASS才可定稿。当前B3=1/3，本票不直接触发源修改。
END_CURRENT_M009_B2_SELF_V0.09


## M009 第五批B2 Codex整批定稿复核
版本：V0.10
输入：modules/M009-b5-r2-self-raw.md本次V0.03（完整文件SHA256 5137ff3d152bb08b00561bc57f470d95d9fdd938e27a2624ac601cf6cd481435）；modules/M009-b5-r2-grok-raw.md本次V0.02（9d6e4c6fa44b480e1e46c2e60582a4c6fb9abef1d633d7f542cc31c25efcc75a）。
候选：findings.md V2.12，866097bytes，SHA256 c37083760956300222e0ca223e29cffc4523a37408677560c04643491efc5539，顶部唯一当前表24行。M009 IN_PROGRESS，B3=2/3。

完整差集：本席B5只有CX-B5-01；Grok本次B5无问题、票PASS。并集1项→新M009-057，原056及以前ID未复用。24行中057是，原17已修及6登记共23项否；全部本次新问题无漏并，不改变Grok原票或把GrokPASS当成同意该新finding。
直接核对SpaceTypeRequest原注释与PlatformService551-558及既有两测试：新建入口确实拒绝已存在编码且固定active。更精确行定位为注释第5行、紧接record声明第6行；报告“:6”是所附声明锚点，原句引用无误。057只改该Javadoc和必须的自身版本，不改变方法或字段，不构造更新功能。上位字典维护BSM-004/PGM-005/020成立，按冲突→细化顺序归细化+工程缺陷，GEN-002/008/016，无新业务添加。队列归属是符合B2规则。
候选保留两处HTML锁引文观察，未将其当HTML本身错误、未增加锁外要求。Grok的87/87首片段邻域命中不等于69全文都相等；候选如实区分，未改Grok报告。两者不影响本模块源码修复范围。
用户非HTML修复授权适用，原句/拟改/影响已在普通对话提供，无未答问题；不重新弹窗。历史先改后确认054-056保留。无需新增文件/测试框架/判据/业务功能；只修注释，既有测试已提供对应运行语义证据。
本票核完整24行与执行边界，不只核057一句。B2_VERDICT: PASS。仍须同一候选GrokPASS才可定稿并进入最后一次B3；当前未修改源码。
END_CURRENT_M009_B2_SELF_V0.10


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 2026-09-21重开 B1 Codex完整独立报告
版本：V0.11

## 结论与独立性
本席B1结论：FAIL。完整读取20文件3369行，逐项核对87条映射判据，完成第一性原理、顺从式、对抗式三线。当前无代码/测试/HTML修复；B3累计3保留。未读取Grok本次raw或原始CLI输出。旧self的116315字节及其SHA全部保留，本段不覆盖旧结论。

三项候选是待B2归并输入，不是已定稿修复授权。范围仅M009；M013当前已PASS及M038现场均未动。审计结论不能用83项测试通过代替。

流程缺陷自检：本次Grok派发文案包含预期criteria-lock哈希，违反提示词A2“不得在派发指令里告诉Grok期望值”。其当前调用即使交付，也不能充当独立哈希验证的合格B1票。本席保留实做审计与旧证据，不读取对方结论；待现有进程实际结束后重新按不含预期哈希的完整指令派发Grok独立B1。不是交付失败，不篡改失败重跑次数，不中断活进程。此错误不涉及源码/HTML变动。

## 第一性原理：三个问题
1. 为什么存在：PGM-017/020的项目档案与归属，PGM-026/BEQR-143的商户和四类绑定，BSM-004的存档/删除/恢复要求直接承载于本模块。删去它们会失去对象归属、主体关系与生命周期。DTO/Record是这些需求的下位接口细节；package-info仅包声明是技术性、无业务发明。
2. 实际行为：在160 backend运行 mvn -o -Dtest=PlatformServiceTests,PlatformControllerTests test -q，真实退出0；新鲜Surefire XML分别69/14 tests，failures=errors=skipped=0。PlatformControllerTests:164–179实际GET /api/v1/platform/merchants，在pii权限返回true时返回第一/第二联系人完整手机号，故该“绿”恰好复现CX-01。服务测试使用内存仓储，不冒称真实数据库验证。
3. 未验证前提：本次未运行数据库集成、迁移回填、全前端或生产接口；姓名落库依据是可复现源代码路径，不声称查到了真实个人数据。R-006是历史操作员裁决输入，不能冒充三份HTML。保留项目三字段“后端/数据库、暂可空、不含页面/旧资料补录”已批准范围。

## 三线过程
- 顺从式：按87条逐项追Controller→Service→Repository→Record/DTO；下表给出适用点，不把外部域需求强塞进本模块。
- 对抗式：检查平台/商户/项目伪造，档案更新覆盖生命周期与四主体，未来日期提前投影、已结束绑定复活、删除引用/申请人自审，手机号权限条件、单次权限与查询留痕。既有保护在当前源码/定向测试中保留；真实反例为CX-01/02/03。没有以极端组合新增守卫。
- 第一性原理：原文对照、全部文件读取与哈希、83项真实测试及具体断言；历史PASS与注释均未当证据。
- 技能：沿本任务已读取的using-superpowers、grilling、codebase-design、diagnosing-bugs、systematic-debugging、verification-before-completion、writing-for-agents、ponytail-review执行其允许的结构。receiving-code-review将在看对方后按核实再采纳执行。不派第三席；无B3，不宣称TDD已运行。缺少宿主Claude hooks只执行既有禁破坏Git约定；可选batch-grill-me/ubiquitous-language未实际调用。

## 本次全部候选

### CX-01 联系人明文开关与单次权限不消费
- 位置：PlatformController.java:277–294；MerchantRecord.java的withMaskedPhones；直接依赖AuthorizationService.java:36–61,266–276；PlatformControllerTests.java:164–179。
- 事实：Controller的两个maskPhonesForUnauthorized在currentActorHasPermission返回true时直接回传解密后的MerchantRecord/page，覆盖读列表及档案写响应。现有MockMvc用例实跑证明第一/第二手机号原文返回。
- 反例：持有仅单次pii.contact_phone.view授权的普通有效账号，布尔校验只调用permissionGrant，不执行requirePermissions内markSingleUsePermissionConsumed；重复读列表仍能继续走同一原文分支。此反例为静态调用链证明，未冒称运行单次授权端到端测试。
- 冻结依据：GEN-005/010、映射PGM-041“临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。”；criteria-lock已定义PGM-059原文“员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。”
- 分类：原文分支为冲突；单次不消费属于细化的工程缺陷，最小处置同一处，合并描述而不多造修复。
- 边界：PGM-059虽已冻结在全局定义区但不在M009的87条映射中；不得静默改映射。本条已能以映射PGM-041的单次消费问题成立；对“界面无开关”的专项适用，B2须显式核实提示词§5(a)已冻结条目引用口径，若要求补映射则先走LOCK-AMEND，不用GEN兜出一个新业务规则。
- 最小拟处置：B2确认后删除本Controller的明文豁免分支，所有此类响应复用现有withMaskedPhones；不新增另一个明文查询通道、不修改HTML、不改变其他控制器（已登记所属模块）。关联历史M009-050，避免把已登记项旧“无缺陷”当豁免。证据类型1/2/4。

### CX-02 商户敏感列表查询无法追到本次返回对象
- 位置：PlatformService.java:144–160；AuditEventService.java:73–128；PlatformController.java商户列表路由。
- 事实：每次列表确实有merchant.viewed，故“完全没有留痕”不成立。该事件businessId固定all，metadata仅count/total，请求ID为空；AuditEventService补当前actor/time/web但不补页面/返回商户IDs。两个相同条数/总数的不同页留下同类证据，无法证明这一回看到哪些商户。
- 补证检查：已完整读三种MVC拦截器（设备凭证、终端总开关、限速），仅前置身份/开关/频控，无返回对象补记；rg在evidence及apps/web的敏感页/page-view命名路径未命中相应补记。结论依赖真实已追调用链，未把缺少某个事件字符串作为唯一证明。
- 冻结原文PGM-043：“打开敏感页这件事本身也要留痕，记下是谁打开的、什么时间打开的、打开的是哪一个页面、这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）；这条留痕不提供关闭开关。”以及GEN-011。
- 分类：细化，有工程缺陷。最小拟处置是在现有merchant.viewed metadata补本次返回的商户ID与稳定页面/入口、筛选/页信息，不记姓名/手机号原文；复用已有事件和表，不新增审计平台。B2定稿前不实施。证据类型1/4。若已有M009-043包含相同义务则复用该ID，不能复用不相同的修复内容掩盖新问题。

### CX-03 第一/第二联系人姓名仍直接明文持久化
- 位置：JdbcPlatformRepository.java:625,631,718,724（同块手机号分别经piiCipher加解密）；MerchantRecord/Request提供这些字段。
- 事实：merchantParameters将contactName/secondaryContactName直接addValue；mapper直接rs.getString。既有PiiCipher能够AES-GCM加解密字符串，但未用于此两列。未读取真实个人行、不宣称存量库扫描已做。
- 冻结上位：PGM-026“建立商户档案、运营主体、负责人、第一联系人和第二联系人”；GEN-010安全与敏感信息；全局已冻结PGM-059“客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。”
- 历史输入：R-006用户曾答“覆盖：生日与姓名也要加密”，它并非三份HTML；当前要处理联系人姓名是否落在“客人和员工”范围，应在B2完整列明，不能偷换为全部商事主体姓名必然已获HTML授权。客户生日归M158/M159，不混入本模块。
- 分类候选：现有联系人档案属于细化；工程缺陷是否成立取决于上述原文覆盖对象及既有批准的适用，不把历史裁决单独当新冻结判据。B2暂不直接写修复队列“是”，若需扩业务原文必须给操作员完整原句、改法、影响，不能自行修改HTML。
- 最小拟处置（确认范围后）：复用PiiCipher调整两列现有读写；同时必须检查既有列长度与历史数据处理，不宣称只加encrypt即可完成。不得擅自新建回填组件。证据类型1/4。

## 已有内容分类与去重
既有M009-048项目SQL分页是PGM-041/GEN-012细化；049审批链持久化是BEQR-166细化；050手机加密/响应脱敏重开见CX-01/03；051分页机制是技术性，无新业务默认，GEN-012；052拒绝直接物理DELETE是BSM-004细化；053 package-info只有包名属技术性；057修正SpaceTypeRequest旧冲突注释当前已修，不制造重复ID。其他旧修复（038/039/040/041/031/042/043/044/045/046/030/047/018/013/054/055/056）仅作为核查线索，已据本轮源码核查其M009路径；模块外Access测试修复不冒称本轮再次实跑全部。


## 全20文件完整覆盖与实算SHA
| 文件 | 完整行数 | SHA256 |
|---|---:|---|
| backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 |
| backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 |
| backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 296 | 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1173 | 984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1 |
| backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java | 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java | 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java | 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java | 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java | 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 |
| backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java | 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 |
| backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java | 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa |
| backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java | 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 |
| backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java | 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f |
| backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java | 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 |

## 87条映射判据逐项核对
每项为本模块适用性/证据，不表示各条独立PASS；全模块结论FAIL。Service/Controller/Jdbc均指本模块完整覆盖文件。
| 判据 | 当前证据与边界 |
|---|---|
| BEQR-063 | 本范围不执行反结/沽清/存酒；Controller未提供此类绕过接口，审批设施不替代酒吧业务规则。 |
| BEQR-074 | Space档案不承担隐私桌台查询授权；仅维护归属，本范围无隐私桌台查看接口。 |
| BEQR-143 | BindingRecord及Service:756–866四类关系、槽位唯一检查、激活投影路径；主体可不同。 |
| BEQR-144 | Repository实体项目关联表/assignEntityProject允许多对多；未恢复固定父链。 |
| BEQR-145 | BusinessEntityRequest/Service主体类型约束无独立spv父链，不赋城市项目公司跨域结算权。 |
| BEQR-146 | 实体档案只记录主体类型/归属；未按城市项目默认接管其他域定价或票务。 |
| BEQR-147 | Service财税主体改变拒绝普通档案入口；未新增平台收费、收入比例或历史投影改写。 |
| BEQR-148 | Service合作方式六值；其他需备注；合同收费模板生效校验不在此档案模块替代。 |
| BEQR-149 | 本范围不定义职级；Controller调用既有AuthorizationService，不把职级当功能授权。 |
| BEQR-150 | 统一Controller并非新增终端；设备/总开关由既有拦截器接入，非本模块新增十一端。 |
| BEQR-151 | requirePermission走既有授权；读取phone使用布尔grant查询，单次消费漏点CX-01。 |
| BEQR-152 | Service.currentProject及Jdbc末段scoped读；对象更改要求项目匹配，默认不跨项目。 |
| BEQR-153 | 统一设备/总开关拦截器对本Controller适用；本模块不修改层级或关闭留痕。 |
| BEQR-154 | Service项目状态集合与原文对应，创建拒绝未知值；无新状态。 |
| BEQR-155 | Service状态集合及terminated立即生效阻断，既有退出缓冲由专门流程；保留M009-018。 |
| BEQR-156 | 本范围不实现合同到期转换；Jdbc审批记录只存关系，不擅自自动终止合同。 |
| BEQR-157 | 本范围未新增员工状态，当前身份校验调用access组件。 |
| BEQR-158 | 本范围未新增设备状态，空间归属资料不替代设备生命周期。 |
| BEQR-159 | 状态请求作用对象为本模块主体/绑定/空间，未添加权限字典状态。 |
| BEQR-166 | Jdbc:804–1181既有supervisor review读写链保留；合同业务生效由合同模块控制，不把持久层当完整合同实现。 |
| BEQR-189 | 商户rentalRate/propertyFee不是服务点单管理费配置；本范围没有改该业务费率。 |
| BSM-001 | 档案基础定义及守卫不增加收费规则；未改变判据规定的可配置边界。 |
| BSM-003 | 主体/商户/空间/项目为既有平台基础；记录归属与操作责任，CX-02查询责任不足。 |
| BSM-004 | 商户与空间类型走存档/申请/审批/软删/恢复；引用存在阻止删；真实最高权限规则由access校验。 |
| BSM-005 | 分步引导属于网页；本模块提供最终创建DTO与字段校验，不把后端创建接口声称为分步弹窗已验收。 |
| BSM-006 | 本模块无二维码生成/刷新/核销实现；主体/商户/空间档案未新增固定凭证码。 |
| BSM-007 | 逐功能权限闸存在；高敏phone授权不能当作永久明文豁免，CX-01。 |
| BSM-009 | 本范围写操作复用幂等与审计，项目隔离不提供关闭入口；敏感查询缺对象留痕CX-02。 |
| BSM-028 | 主体多对多、六合作方式、空间项目校验；普通更新不能改结算主体，未来绑定不即刻投影。 |
| BSM-029 | Project/Space/Merchant保存关联ID供各域引用；不宣称各域所有消费端已逐一运行。 |
| BSM-072 | Service业态六值；平台scope拒绝商户级平台视图，未以合作方式重定义业态。 |
| BSM-074 | 联系人及合同概要档案提供连接，未将平台列表授权下放给商户或泄露跨项目。 |
| BSM-114 | 后台接口权限与统一终端链核对；冻结考勤用词与现HTML存在已有批准差异，不重开业务HTML。 |
| BSM-115 | Jdbc实体/商户/空间/project关联及Service写前校验提供基础；跨域履约不在本范围冒报完成。 |
| BSM-120 | 项目归属保留；所有档案修改经审计/幂等；商户查询责任CX-02；旧footer差异非新缺陷。 |
| GEN-001 | Record/DTO承载档案与关系；package-info只定包名；范围内无新增抽象。 |
| GEN-002 | 实跑PlatformServiceTests 69及PlatformControllerTests 14全部通过；实际明文响应也被测试复现，见CX-01。 |
| GEN-003 | 审读现有明文断言PlatformControllerTests:164–179；断言能验证当前行为但不能证明HTML一致。 |
| GEN-004 | Service创建/更新校验业态、合作方式、金额、项目/主体/商户所有权；Request使用约束；数据库参数绑定。 |
| GEN-005 | Controller逐动作requirePermission；Service.currentProject强制归属；PhoneMask布尔权限不消费单次授权，CX-01。 |
| GEN-006 | Binding创建/状态切换锁商户并重读，财税主体变更拒绝；软删除保留记录；数据库真实事务未在本次集成实跑。 |
| GEN-007 | 写入口沿现有IdempotencyService；binding锁后重查；现有服务测试通过，不宣称全并发集成已验证。 |
| GEN-008 | ProjectRequest/ProjectRecord及Jdbc三个资料字段一致；MerchantRecord与参数/mapper对应；不是页面完成声明。 |
| GEN-009 | 非法状态、跨项目、无对象均明确拒绝；Phone布尔权限异常收窄为脱敏；姓名存储见CX-03。 |
| GEN-010 | 命名参数SQL未见注入；手机写密文读解密；明文响应CX-01及姓名明文CX-03分别登记。 |
| GEN-011 | 写动作ID/reason/decision进入审计；商户列表仅count/total无对象集合，CX-02。 |
| GEN-012 | 分页默认/上限及SQL LIMIT/OFFSET、project谓词有边界；引用扫描随库表数，未以极端负载新增要求。 |
| GEN-013 | 160 Maven离线定向测试exit0；本模块无新构建配置，不声明全库构建/部署通过。 |
| GEN-014 | 本范围无迁移文件；仅核Jdbc已有字段读写，真实迁移执行属于M010等，不虚报实跑。 |
| GEN-015 | 本范围无UI；Service错误主要中文；英文状态作为内部标识交展示层，未将DTO误作界面。 |
| GEN-016 | 文件行数/SHA现算；旧PASS非当前依据；历史已批准资料后端范围保留。 |
| GEN-017 | 三项本轮候选及范围内既有细化/技术条目逐条分类，未直接修复。 |
| GEN-018 | 全20文件命令读取、冻结原文、83实跑结果；无法证明之存量库/UI明确不作已完成。 |
| PGM-001 | Record携ID与关联，金额非负，幂等写；全中文错误检查；CX-01/02敏感数据与溯源。 |
| PGM-002 | Controller鉴权→Service对象范围→Repository持久化→审计；列表具体对象证据CX-02。 |
| PGM-003 | 本范围后台接口支撑治理模块，不承载入口卡片UI，无UI新增或遗漏断言。 |
| PGM-004 | currentProject与project associations预留集团归属；当前普通查询仍限定单项目。 |
| PGM-005 | SpaceRecord创建及space type生命周期；父空间/商户须同项目，保留已修恢复。 |
| PGM-006 | MerchantRecord及DTO覆盖第一/第二联系人和主体；联系人信息安全CX-01/03。 |
| PGM-007 | 档案经营范围仅供准入引用；普通更新不能重写合同/财税权责。 |
| PGM-008 | 本范围无员工手机注册实现，只调用既有actor上下文；不混为M009缺失。 |
| PGM-009 | Controller使用单一功能permission；phone布尔检查单次使用缺口CX-01。 |
| PGM-010 | 本模块未定义终端开关，已核共用拦截器，无关闭强制脱敏的新合法需求。 |
| PGM-011 | Space归属提供设备点位支持，不包含设备领还实现；直接调用无新台账类型。 |
| PGM-012 | requirePlatformScope拒绝merchantId/外包主体平台全局访问；本范围不提供商户门户。 |
| PGM-013 | 本模块为内部/api/v1/platform，不替代open接口身份核验；无新第三方协议。 |
| PGM-014 | Service.executeWrite携业务ID及原因；merchant.viewed仅all/count/total，CX-02。 |
| PGM-015 | 空间维护提供楼宇归属，本范围不执行能耗/消防业务。 |
| PGM-016 | 本模块未实现破窗密钥，已有最高权限调用；隐私手机/姓名输入单列CX-01/03。 |
| PGM-017 | 项目ID贯穿merchant/spaces及实体关联；currentProject不提供空scope全量。 |
| PGM-018 | entity_projects关联多对多，无固定父链；城市项目职责不通过普通档案扩大。 |
| PGM-019 | 四主体独立记录，财税变更守卫；不新增股权算法、域间收入或借贷规则。 |
| PGM-020 | ProjectRequest/Record/Jdbc包含负责人地址经营范围，暂可空是已批准范围；不声称UI/旧项目补录完成。 |
| PGM-021 | 项目隔离SQL/currentProject避免混读；集团视图不作为本轮额外功能开发。 |
| PGM-026 | 四类绑定唯一槽位，未来inactive且不改当前主体；更新档案保留四主体；联系人姓名CX-03待裁定。 |
| PGM-027 | 平台列表server-scope拒绝商户；平台不因可查对接信息取得默认原文phone豁免。 |
| PGM-031 | BusinessEntity不等于经销商门户账号；不在此增加自动采购或经销支付规则。 |
| PGM-035 | 本范围无提成、人员服务商品或营销打卡；不以档案字段实现新收入规则。 |
| PGM-040 | Controller功能权限与项目scope并行；普通最高职级不等于真实master。 |
| PGM-041 | currentActorHasPermission不消费singleUse而放原文，CX-01；项目隔离已核。 |
| PGM-042 | 授权按permission不按职级字符串；本模块不增加等级或薪资规则。 |
| PGM-043 | 敏感页访问要求谁/何时/哪个页面/哪些对象；本商户列表事件缺对象与页面，CX-02。 |
| PGM-044 | 本范围没有桌台/经销交易；绑定操作实时检查权限及范围，不回滚历史事件。 |
| PGM-048 | requirePlatformScope防商户读取平台档案；营业时间下游service有project范围，其完整业务属M015。 |
| PGM-060 | 本模块无会员新增、定向营销或人脸比对；XM-011仍归M049/M128，不混入M009。 |
| PGM-065 | 基础对象ID与关系供票务/酒吧/财务/物业连接；引用扫描阻止删除被使用商户。 |
| PGM-066 | 状态时间与审计、归属、中文信息核对；存在CX-02，故不得以83测试绿称全模块PASS。 |

本席完整报告封存UTC：2026-09-22T04:31:39.400694+00:00。尚未读取Grok本次结论。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 2026-09-21重开 B1 Codex完整独立报告
版本：V0.12

## 结论与独立性
本席B1结论：FAIL。完整读取20文件3369行，逐项核对87条映射判据，完成第一性原理、顺从式、对抗式三线。当前无代码/测试/HTML修复；B3累计3保留。未读取Grok本次raw或原始CLI输出。旧self的139387字节及其SHA全部保留，本段不覆盖旧结论。

三项候选是待B2归并输入，不是已定稿修复授权。范围仅M009；M013当前已PASS及M038现场均未动。审计结论不能用83项测试通过代替。

派发纠偏已实际执行：前次提供预期锁哈希的Grok调用交付exit0，仅保留为无效流程记录，未读其正文；新的完整B1调用不提供期望值，参数仍为grok-4.6/xhigh，无第三席。当前20文件字节与本轮已完整读取的版本一致，87条原文不变，沿用本轮真实83项测试而不虚报重复运行。以下为新的完整本方独立结论；本席在封存前未读Grok任一本次新段。

## 第一性原理：三个问题
1. 为什么存在：PGM-017/020的项目档案与归属，PGM-026/BEQR-143的商户和四类绑定，BSM-004的存档/删除/恢复要求直接承载于本模块。删去它们会失去对象归属、主体关系与生命周期。DTO/Record是这些需求的下位接口细节；package-info仅包声明是技术性、无业务发明。
2. 实际行为：在160 backend运行 mvn -o -Dtest=PlatformServiceTests,PlatformControllerTests test -q，真实退出0；新鲜Surefire XML分别69/14 tests，failures=errors=skipped=0。PlatformControllerTests:164–179实际GET /api/v1/platform/merchants，在pii权限返回true时返回第一/第二联系人完整手机号，故该“绿”恰好复现CX-01。服务测试使用内存仓储，不冒称真实数据库验证。
3. 未验证前提：本次未运行数据库集成、迁移回填、全前端或生产接口；姓名落库依据是可复现源代码路径，不声称查到了真实个人数据。R-006是历史操作员裁决输入，不能冒充三份HTML。保留项目三字段“后端/数据库、暂可空、不含页面/旧资料补录”已批准范围。

## 三线过程
- 顺从式：按87条逐项追Controller→Service→Repository→Record/DTO；下表给出适用点，不把外部域需求强塞进本模块。
- 对抗式：检查平台/商户/项目伪造，档案更新覆盖生命周期与四主体，未来日期提前投影、已结束绑定复活、删除引用/申请人自审，手机号权限条件、单次权限与查询留痕。既有保护在当前源码/定向测试中保留；真实反例为CX-01/02/03。没有以极端组合新增守卫。
- 第一性原理：原文对照、全部文件读取与哈希、83项真实测试及具体断言；历史PASS与注释均未当证据。
- 技能：沿本任务已读取的using-superpowers、grilling、codebase-design、diagnosing-bugs、systematic-debugging、verification-before-completion、writing-for-agents、ponytail-review执行其允许的结构。receiving-code-review将在看对方后按核实再采纳执行。不派第三席；无B3，不宣称TDD已运行。缺少宿主Claude hooks只执行既有禁破坏Git约定；可选batch-grill-me/ubiquitous-language未实际调用。

## 本次全部候选

### CX-01 联系人明文开关与单次权限不消费
- 位置：PlatformController.java:277–294；MerchantRecord.java的withMaskedPhones；直接依赖AuthorizationService.java:36–61,266–276；PlatformControllerTests.java:164–179。
- 事实：Controller的两个maskPhonesForUnauthorized在currentActorHasPermission返回true时直接回传解密后的MerchantRecord/page，覆盖读列表及档案写响应。现有MockMvc用例实跑证明第一/第二手机号原文返回。
- 反例：持有仅单次pii.contact_phone.view授权的普通有效账号，布尔校验只调用permissionGrant，不执行requirePermissions内markSingleUsePermissionConsumed；重复读列表仍能继续走同一原文分支。此反例为静态调用链证明，未冒称运行单次授权端到端测试。
- 冻结依据：GEN-005/010、映射PGM-041“临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。”；criteria-lock已定义PGM-059原文“员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。”
- 分类：原文分支为冲突；单次不消费属于细化的工程缺陷，最小处置同一处，合并描述而不多造修复。
- 边界：PGM-059虽已冻结在全局定义区但不在M009的87条映射中；不得静默改映射。本条已能以映射PGM-041的单次消费问题成立；对“界面无开关”的专项适用，B2须显式核实提示词§5(a)已冻结条目引用口径，若要求补映射则先走LOCK-AMEND，不用GEN兜出一个新业务规则。
- 最小拟处置：B2确认后删除本Controller的明文豁免分支，所有此类响应复用现有withMaskedPhones；不新增另一个明文查询通道、不修改HTML、不改变其他控制器（已登记所属模块）。关联历史M009-050，避免把已登记项旧“无缺陷”当豁免。证据类型1/2/4。

### CX-02 商户敏感列表查询无法追到本次返回对象
- 位置：PlatformService.java:144–160；AuditEventService.java:73–128；PlatformController.java商户列表路由。
- 事实：每次列表确实有merchant.viewed，故“完全没有留痕”不成立。该事件businessId固定all，metadata仅count/total，请求ID为空；AuditEventService补当前actor/time/web但不补页面/返回商户IDs。两个相同条数/总数的不同页留下同类证据，无法证明这一回看到哪些商户。
- 补证检查：已完整读三种MVC拦截器（设备凭证、终端总开关、限速），仅前置身份/开关/频控，无返回对象补记；rg在evidence及apps/web的敏感页/page-view命名路径未命中相应补记。结论依赖真实已追调用链，未把缺少某个事件字符串作为唯一证明。
- 冻结原文PGM-043：“打开敏感页这件事本身也要留痕，记下是谁打开的、什么时间打开的、打开的是哪一个页面、这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）；这条留痕不提供关闭开关。”以及GEN-011。
- 分类：细化，有工程缺陷。最小拟处置是在现有merchant.viewed metadata补本次返回的商户ID与稳定页面/入口、筛选/页信息，不记姓名/手机号原文；复用已有事件和表，不新增审计平台。B2定稿前不实施。证据类型1/4。若已有M009-043包含相同义务则复用该ID，不能复用不相同的修复内容掩盖新问题。

### CX-03 第一/第二联系人姓名仍直接明文持久化
- 位置：JdbcPlatformRepository.java:625,631,718,724（同块手机号分别经piiCipher加解密）；MerchantRecord/Request提供这些字段。
- 事实：merchantParameters将contactName/secondaryContactName直接addValue；mapper直接rs.getString。既有PiiCipher能够AES-GCM加解密字符串，但未用于此两列。未读取真实个人行、不宣称存量库扫描已做。
- 冻结上位：PGM-026“建立商户档案、运营主体、负责人、第一联系人和第二联系人”；GEN-010安全与敏感信息；全局已冻结PGM-059“客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。”
- 历史输入：R-006用户曾答“覆盖：生日与姓名也要加密”，它并非三份HTML；当前要处理联系人姓名是否落在“客人和员工”范围，应在B2完整列明，不能偷换为全部商事主体姓名必然已获HTML授权。客户生日归M158/M159，不混入本模块。
- 分类候选：现有联系人档案属于细化；工程缺陷是否成立取决于上述原文覆盖对象及既有批准的适用，不把历史裁决单独当新冻结判据。B2暂不直接写修复队列“是”，若需扩业务原文必须给操作员完整原句、改法、影响，不能自行修改HTML。
- 最小拟处置（确认范围后）：复用PiiCipher调整两列现有读写；同时必须检查既有列长度与历史数据处理，不宣称只加encrypt即可完成。不得擅自新建回填组件。证据类型1/4。

## 已有内容分类与去重
既有M009-048项目SQL分页是PGM-041/GEN-012细化；049审批链持久化是BEQR-166细化；050手机加密/响应脱敏重开见CX-01/03；051分页机制是技术性，无新业务默认，GEN-012；052拒绝直接物理DELETE是BSM-004细化；053 package-info只有包名属技术性；057修正SpaceTypeRequest旧冲突注释当前已修，不制造重复ID。其他旧修复（038/039/040/041/031/042/043/044/045/046/030/047/018/013/054/055/056）仅作为核查线索，已据本轮源码核查其M009路径；模块外Access测试修复不冒称本轮再次实跑全部。


## 本次直接依赖补证（仅只读，不扩大模块修复范围）
- OrganizationMerchantPage.tsx:144–149调用同一商户列表；57–62的display仅String转换；314详情、390表格直接显示row.contactPhone，未再次脱敏。因此CX-01不是“后端原文但页面自行打码”的误报。此为源代码调用链核实，不声称浏览器端到端实跑。
- 商户写入口搜索：rg -n 'INSERT INTO merchants' backend/src/main/java/com/complex/platform仅JdbcPlatformRepository:81；UPDATE除同库upsert外见193软删、1210存档原因、1243项目归属。contactName/secondaryContactName在本模块请求→Service trim→同一仓储。
- V005:23第一姓名及V042:5第二姓名都定义VARCHAR(120)；本次仅查迁移源码，不声称查询了实际库schema。需加密时密文容量必须兼容现有有效长度，不能直接加encrypt后忽略列宽。
- 已完整读取既有MultiPartyContactPhonePiiBackfill:1–157，它按表分组登记并同一UPDATE处理多列；商户目前仅登记两phone列。若姓名加密范围最终成立，应优先复用其现有能力并核查列宽与登记，不能无故新造回填组件。tools/verify-pii-encryption.mjs登记需交所属M053，不先改。
- 补证rg最初同时指定了不存在的packages路径，exit2；有效前端命中已以具体存在文件重新读取确认，未把该次exit2当完整检索成功。


## 全20文件覆盖
| 文件 | 行数 | SHA256 |
|---|---:|---|
| backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java | 17 | 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277 |
| backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java | 19 | f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 116 | dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00 |
| backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java | 9 | e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a |
| backend/src/main/java/com/complex/platform/governance/PlatformController.java | 296 | 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 204 | df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1173 | 984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1 |
| backend/src/main/java/com/complex/platform/governance/package-info.java | 1 | e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9 |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java | 17 | 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da |
| backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java | 26 | 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java | 6 | e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java | 5 | 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java | 5 | 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45 |
| backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java | 41 | 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1 |
| backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java | 26 | 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108 |
| backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java | 21 | 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa |
| backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java | 16 | 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34 |
| backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java | 11 | 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f |
| backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java | 22 | 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1 |

## 全87映射判据逐项核对
表内为核查事实/边界，非单条PASS；本席整模块FAIL。
| 判据 | 证据 |
|---|---|
| BEQR-063 | 本范围不执行反结/沽清/存酒；Controller未提供此类绕过接口，审批设施不替代酒吧业务规则。 |
| BEQR-074 | Space档案不承担隐私桌台查询授权；仅维护归属，本范围无隐私桌台查看接口。 |
| BEQR-143 | BindingRecord及Service:756–866四类关系、槽位唯一检查、激活投影路径；主体可不同。 |
| BEQR-144 | Repository实体项目关联表/assignEntityProject允许多对多；未恢复固定父链。 |
| BEQR-145 | BusinessEntityRequest/Service主体类型约束无独立spv父链，不赋城市项目公司跨域结算权。 |
| BEQR-146 | 实体档案只记录主体类型/归属；未按城市项目默认接管其他域定价或票务。 |
| BEQR-147 | Service财税主体改变拒绝普通档案入口；未新增平台收费、收入比例或历史投影改写。 |
| BEQR-148 | Service合作方式六值；其他需备注；合同收费模板生效校验不在此档案模块替代。 |
| BEQR-149 | 本范围不定义职级；Controller调用既有AuthorizationService，不把职级当功能授权。 |
| BEQR-150 | 统一Controller并非新增终端；设备/总开关由既有拦截器接入，非本模块新增十一端。 |
| BEQR-151 | requirePermission走既有授权；读取phone使用布尔grant查询，单次消费漏点CX-01。 |
| BEQR-152 | Service.currentProject及Jdbc末段scoped读；对象更改要求项目匹配，默认不跨项目。 |
| BEQR-153 | 统一设备/总开关拦截器对本Controller适用；本模块不修改层级或关闭留痕。 |
| BEQR-154 | Service项目状态集合与原文对应，创建拒绝未知值；无新状态。 |
| BEQR-155 | Service状态集合及terminated立即生效阻断，既有退出缓冲由专门流程；保留M009-018。 |
| BEQR-156 | 本范围不实现合同到期转换；Jdbc审批记录只存关系，不擅自自动终止合同。 |
| BEQR-157 | 本范围未新增员工状态，当前身份校验调用access组件。 |
| BEQR-158 | 本范围未新增设备状态，空间归属资料不替代设备生命周期。 |
| BEQR-159 | 状态请求作用对象为本模块主体/绑定/空间，未添加权限字典状态。 |
| BEQR-166 | Jdbc:804–1181既有supervisor review读写链保留；合同业务生效由合同模块控制，不把持久层当完整合同实现。 |
| BEQR-189 | 商户rentalRate/propertyFee不是服务点单管理费配置；本范围没有改该业务费率。 |
| BSM-001 | 档案基础定义及守卫不增加收费规则；未改变判据规定的可配置边界。 |
| BSM-003 | 主体/商户/空间/项目为既有平台基础；记录归属与操作责任，CX-02查询责任不足。 |
| BSM-004 | 商户与空间类型走存档/申请/审批/软删/恢复；引用存在阻止删；真实最高权限规则由access校验。 |
| BSM-005 | 分步引导属于网页；本模块提供最终创建DTO与字段校验，不把后端创建接口声称为分步弹窗已验收。 |
| BSM-006 | 本模块无二维码生成/刷新/核销实现；主体/商户/空间档案未新增固定凭证码。 |
| BSM-007 | 逐功能权限闸存在；高敏phone授权不能当作永久明文豁免，CX-01。 |
| BSM-009 | 本范围写操作复用幂等与审计，项目隔离不提供关闭入口；敏感查询缺对象留痕CX-02。 |
| BSM-028 | 主体多对多、六合作方式、空间项目校验；普通更新不能改结算主体，未来绑定不即刻投影。 |
| BSM-029 | Project/Space/Merchant保存关联ID供各域引用；不宣称各域所有消费端已逐一运行。 |
| BSM-072 | Service业态六值；平台scope拒绝商户级平台视图，未以合作方式重定义业态。 |
| BSM-074 | 联系人及合同概要档案提供连接，未将平台列表授权下放给商户或泄露跨项目。 |
| BSM-114 | 后台接口权限与统一终端链核对；冻结考勤用词与现HTML存在已有批准差异，不重开业务HTML。 |
| BSM-115 | Jdbc实体/商户/空间/project关联及Service写前校验提供基础；跨域履约不在本范围冒报完成。 |
| BSM-120 | 项目归属保留；所有档案修改经审计/幂等；商户查询责任CX-02；旧footer差异非新缺陷。 |
| GEN-001 | Record/DTO承载档案与关系；package-info只定包名；范围内无新增抽象。 |
| GEN-002 | 实跑PlatformServiceTests 69及PlatformControllerTests 14全部通过；实际明文响应也被测试复现，见CX-01。 |
| GEN-003 | 审读现有明文断言PlatformControllerTests:164–179；断言能验证当前行为但不能证明HTML一致。 |
| GEN-004 | Service创建/更新校验业态、合作方式、金额、项目/主体/商户所有权；Request使用约束；数据库参数绑定。 |
| GEN-005 | Controller逐动作requirePermission；Service.currentProject强制归属；PhoneMask布尔权限不消费单次授权，CX-01。 |
| GEN-006 | Binding创建/状态切换锁商户并重读，财税主体变更拒绝；软删除保留记录；数据库真实事务未在本次集成实跑。 |
| GEN-007 | 写入口沿现有IdempotencyService；binding锁后重查；现有服务测试通过，不宣称全并发集成已验证。 |
| GEN-008 | ProjectRequest/ProjectRecord及Jdbc三个资料字段一致；MerchantRecord与参数/mapper对应；不是页面完成声明。 |
| GEN-009 | 非法状态、跨项目、无对象均明确拒绝；Phone布尔权限异常收窄为脱敏；姓名存储见CX-03。 |
| GEN-010 | 命名参数SQL未见注入；手机写密文读解密；明文响应CX-01及姓名明文CX-03分别登记。 |
| GEN-011 | 写动作ID/reason/decision进入审计；商户列表仅count/total无对象集合，CX-02。 |
| GEN-012 | 分页默认/上限及SQL LIMIT/OFFSET、project谓词有边界；引用扫描随库表数，未以极端负载新增要求。 |
| GEN-013 | 160 Maven离线定向测试exit0；本模块无新构建配置，不声明全库构建/部署通过。 |
| GEN-014 | 本范围无迁移文件；仅核Jdbc已有字段读写，真实迁移执行属于M010等，不虚报实跑。 |
| GEN-015 | 本范围无UI；Service错误主要中文；英文状态作为内部标识交展示层，未将DTO误作界面。 |
| GEN-016 | 文件行数/SHA现算；旧PASS非当前依据；历史已批准资料后端范围保留。 |
| GEN-017 | 三项本轮候选及范围内既有细化/技术条目逐条分类，未直接修复。 |
| GEN-018 | 全20文件命令读取、冻结原文、83实跑结果；无法证明之存量库/UI明确不作已完成。 |
| PGM-001 | Record携ID与关联，金额非负，幂等写；全中文错误检查；CX-01/02敏感数据与溯源。 |
| PGM-002 | Controller鉴权→Service对象范围→Repository持久化→审计；列表具体对象证据CX-02。 |
| PGM-003 | 本范围后台接口支撑治理模块，不承载入口卡片UI，无UI新增或遗漏断言。 |
| PGM-004 | currentProject与project associations预留集团归属；当前普通查询仍限定单项目。 |
| PGM-005 | SpaceRecord创建及space type生命周期；父空间/商户须同项目，保留已修恢复。 |
| PGM-006 | MerchantRecord及DTO覆盖第一/第二联系人和主体；联系人信息安全CX-01/03。 |
| PGM-007 | 档案经营范围仅供准入引用；普通更新不能重写合同/财税权责。 |
| PGM-008 | 本范围无员工手机注册实现，只调用既有actor上下文；不混为M009缺失。 |
| PGM-009 | Controller使用单一功能permission；phone布尔检查单次使用缺口CX-01。 |
| PGM-010 | 本模块未定义终端开关，已核共用拦截器，无关闭强制脱敏的新合法需求。 |
| PGM-011 | Space归属提供设备点位支持，不包含设备领还实现；直接调用无新台账类型。 |
| PGM-012 | requirePlatformScope拒绝merchantId/外包主体平台全局访问；本范围不提供商户门户。 |
| PGM-013 | 本模块为内部/api/v1/platform，不替代open接口身份核验；无新第三方协议。 |
| PGM-014 | Service.executeWrite携业务ID及原因；merchant.viewed仅all/count/total，CX-02。 |
| PGM-015 | 空间维护提供楼宇归属，本范围不执行能耗/消防业务。 |
| PGM-016 | 本模块未实现破窗密钥，已有最高权限调用；隐私手机/姓名输入单列CX-01/03。 |
| PGM-017 | 项目ID贯穿merchant/spaces及实体关联；currentProject不提供空scope全量。 |
| PGM-018 | entity_projects关联多对多，无固定父链；城市项目职责不通过普通档案扩大。 |
| PGM-019 | 四主体独立记录，财税变更守卫；不新增股权算法、域间收入或借贷规则。 |
| PGM-020 | ProjectRequest/Record/Jdbc包含负责人地址经营范围，暂可空是已批准范围；不声称UI/旧项目补录完成。 |
| PGM-021 | 项目隔离SQL/currentProject避免混读；集团视图不作为本轮额外功能开发。 |
| PGM-026 | 四类绑定唯一槽位，未来inactive且不改当前主体；更新档案保留四主体；联系人姓名CX-03待裁定。 |
| PGM-027 | 平台列表server-scope拒绝商户；平台不因可查对接信息取得默认原文phone豁免。 |
| PGM-031 | BusinessEntity不等于经销商门户账号；不在此增加自动采购或经销支付规则。 |
| PGM-035 | 本范围无提成、人员服务商品或营销打卡；不以档案字段实现新收入规则。 |
| PGM-040 | Controller功能权限与项目scope并行；普通最高职级不等于真实master。 |
| PGM-041 | currentActorHasPermission不消费singleUse而放原文，CX-01；项目隔离已核。 |
| PGM-042 | 授权按permission不按职级字符串；本模块不增加等级或薪资规则。 |
| PGM-043 | 敏感页访问要求谁/何时/哪个页面/哪些对象；本商户列表事件缺对象与页面，CX-02。 |
| PGM-044 | 本范围没有桌台/经销交易；绑定操作实时检查权限及范围，不回滚历史事件。 |
| PGM-048 | requirePlatformScope防商户读取平台档案；营业时间下游service有project范围，其完整业务属M015。 |
| PGM-060 | 本模块无会员新增、定向营销或人脸比对；XM-011仍归M049/M128，不混入M009。 |
| PGM-065 | 基础对象ID与关系供票务/酒吧/财务/物业连接；引用扫描阻止删除被使用商户。 |
| PGM-066 | 状态时间与审计、归属、中文信息核对；存在CX-02，故不得以83测试绿称全模块PASS。 |

完整封存UTC 2026-09-22T04:47:45.745467+00:00；未读取Grok本次报告正文，B3仍3，无业务修复。


## M009 B2 2026-09-21重开批次：Codex整批候选复核
版本：V0.13
本席B2结论：PASS（仅表示候选归并/分类/队列有效，不表示模块或修复PASS）。
输入：modules/M009-b1-self-raw.md末个哈希行起的完整B1 V0.12（封存前缀163739bytes SHA256 69b413e04d3b50acfb7a84ef8b6de2d57c00cd82a5449f708772f6ed07a70f19）；modules/M009-b1-grok-raw.md完整B1 V0.12（封存前缀301012bytes SHA256 0996c0167e39458ee87b7bcd4b9a9fd125a10be660a2d8e8054b44b5d2a8368a）。不采前次错误派发V0.11。候选findings.md V3.21 SHA256 943b552433a7210fa07043e34b4a29d22e004f660dcd3019ad3d53c04123bd40。

### 差集与事实复核
| 全部输入 | 候选 | 本席结论 |
|---|---|---|
| CX-01、CX-02、GR-M009-B1-01 | 新M009-058 | 手机明文豁免与对象级列表留痕完整纳入同一项；未省略写回及USB场景、无权限时正常掩码、已有merchant.viewed而非零审计、前端直接显示、单次布尔权限不消费。 |
| CX-03、GR-M009-B1-02 | 新M009-059 | 直绑姓名事实保留；按Grok明示可降观察分支，未改写为已加密，未偷采R006或将导出保护自动变成存储义务；不把字段类型和回填影响删掉。 |
| Grok §7 LOCK-AMEND建议 | 候选完整理由段 | 保留PGM-057/059未映射与建议；058用已映射PGM-043/041成立，059不入修复队列，故本批没有启用映射外存储加密判据，不改lock。若后续确需映射，须完整五步及实际最早引用模块计算，不能按建议擅定M009。 |
| OBS-01/02/03/04 | 候选完整观察段 | 幂等下游守卫、外部营业时间依赖、其他姓名字段、V339/V394背景全保留；未顺改其他模块。 |
| 历史24项 | 完整26项表内保留 | 18已修和6原登记继续否；050的当前边界澄清，历史原文保留；058不是旧043的写事件，也不沿用050泛称无缺陷免审。 |

### 分类与队列
058冲突/工程是/队列是：映射PGM-043明确页面强制脱敏与每次打开敏感页对象留痕；PGM-041单次授权原则；具体服务端反例及现有MockMvc真实结果已落B1。采用删除原文豁免+补现有事件metadata的最小方案，不增加明文审批通道。不能把“USB未经审批”本身当全部最高权限均须审批的新规则，本方案不这样做。
059细化/当前未证成工程缺陷/队列否：PGM-026确有联系人档案上位；“客人与员工”是否适用于每个商户联系人没有身份事实，也不使用只有导出语境的名单替代存储义务。按GEN-017应登记现有实现与范围，不把不确定推断当已定稿缺陷。本票不是“姓名明文永久合法”裁决，仍保留XM010跨模块输入。若Grok否决此处，则B2 FAIL不得定稿。
051/053技术性历史项的无业务发明理由与证据保持；无添加项，不借此制造人闸。完整表26行=1候选是+25否（18已修、7登记/观察）。无新的业务概念、字段、角色、枚举、HTML或锁变更。

### 最小修复范围与验证边界
058仅PlatformController（两类响应均调用现有mask，不按pii码原样返回）、PlatformService（现有merchant.viewed补IDs/入口/分页筛选），及既有PlatformControllerTests/PlatformServiceTests必要反例和自身版本；不改MerchantRecord掩码算法、AuthorizationService、其他四控制器、加密存储/迁移/回填。
后续可执行修改必须在B3先RED→最小修复→GREEN；本轮83测试已通过但错误明文断言须纠正，此时尚未修改或重跑任何测试。
当前M009 IN_PROGRESS，B3累计3；尚待Grok本批B2票。不读取其本批追加前先封存此票。即使B2双PASS，仍不能自加第4次B3或把B2当修复；按提示词轮次上限处理。



## V0.14 M009 B2 操作员第2、3项裁决后重新归并
结论：PASS（仅对本批候选范围与归并；不是模块PASS）。
输入：modules/M009-b1-self-raw.md有效完整B1 V0.12封存前缀163739bytes；modules/M009-b1-grok-raw.md有效完整B1 V0.12封存前缀301012bytes；两席V0.13历史B2。本批findings V3.24 SHA256 d044dbc707df525c0442e56e72c741688d01c100f29a4af3baed24f0a6dd4300。
操作员依次“2 可以 3.应该加密 1不动”“继续”，后者明确回应一次追加修复授权请求。保留历史B3=3，获准最多一次至4；B2期间未改业务源码、测试、迁移、HTML。
完整差集：原24条历史ID038/039/040/041/031/042/043/044/045/046/030/047/018/013/054/055/056/048/049/050/051/052/053/057在新完整表仍全数保留且队列否；未重新定义其已修事实。CX01+CX02+GR01→058不遗漏，原第1项冲突由操作员明确不修而保留未解决记录；只修第2项。CX03+GR02→059不遗漏，操作员明确应加密使此前范围未证成理由不再是当前处置；只限商户主次联系人两列。OBS01幂等、OBS02营业时段、OBS03owner_person/legal_representative、OBS04权限种子以及PGM057/059映射建议保留原记录，不随本批扩大修复。
分类：058原冲突事实仍是，不能以操作员不动改成已符合HTML；当前修复队列仅留痕部分是，原文豁免部分按新指令不执行。059属已存在商户档案的安全实现细化（PGM026/GEN010/GEN008）；本次操作员明确的是具体修复范围，不给其它模块创造泛化判据，不篡改R006/PGM059映射或锁。059当前工程缺陷与队列是，非自行新增业务功能。无新findingID，无新questions，无HTML修改。
根因和最小范围核查：
- PlatformService144–160只记录count/total；AuditEventService73–128仅补人/时间，确无返回对象/查询页。添加既有事件metadata即可；从返回PageResponse取归一化page/pageSize，商户ID仅取实际本页，不取全库；projectId复用同一次currentProject结果，未知项目可能null，metadata使用空值表示而不能Map.of(null)导致原空列表接口出错。businessFormat已trim及白名单，可空用空串；不写联系人姓名/电话。
- 生产java全域搜索仅JdbcPlatformRepository:81写两姓名；所有select共用mapMerchant，create/update/save及查询复用四处加解密足够，其他UPDATE仅状态/项目/归档理由，不需要改控制器。两个原列VARCHAR120，AES-GCM信封开销=7+base64(UTF8字节+28)，原120中文字可达527字符，因此必要新迁移改TEXT，不改历史V005/V042。
- PiiCipher现有encrypt/decrypt及密钥用途直接复用，手机号逻辑保持。AGENTS第8条不新增历史兼容/回填设计，已有PiiCipher历史透传保持，旧行未重新保存不宣称已加密。
- 既有PlatformServiceTests用公开服务入口检查本页IDs、筛选/空页/归一化，不测私有方法；既有PlatformJdbcIntegrationTests在@Transactional下从公开repository存取并检查持久化密文（此任务加密落库本身就是被测边界），覆盖新写/更新、120中文字符、null/empty。用原门禁两列登记防回归，不造新验证框架。
范围：Service/Repository、必要新迁移、上述既有测试、现有PII门禁及自身版本。Controller/授权逻辑/手机号/其他姓名/HTML不动。新迁移落前核最大号，测试只complex_test、不生产，不提交发布。
复核技能using-superpowers/receiving-code-review/systematic-debugging/tdd实际读取；按提示词不派第三席、不因技能另造审批/产物。
本票不预判Grok；Grok任何FAIL须处理，不以本票覆盖。原第1项残留使整模块不能自动B5 PASS，后续须如实列明。

时间：2026-09-22T05:33:54.015688+00:00


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 现锁B1 Codex完整独立审计
版本：V0.15

当前M009 / 471，B3累计4，生命周期IN_PROGRESS。保留V0.01—V0.14全部历史。Grok先派发，本报告封存前未读对席本次正文。正本main HEAD e456ba96c84185c8d39f18f548011daac48a6466。
全部20FILES逐行阅读3375行（计划历史2476不冒充当前），全部88判据定义与HTML摘录已读。原计划不自行重切。当前PGM V0.13已批准页面明文例外及导出边界，L656单次规则不变。

## 第一性原理
存在理由：项目/空间/商户/公司及唯一四类绑定是治理归属底座，来自PGM017—027/BSM028；去除这些会失去业务对象及范围。DTO、分页、SQL和审计结构是其细化/技术承载，不能因HTML没有Java方法名就删除。
真实行为：全20文件读完，追所有7个商户返回调用；实际109测试0失败；真实AuthorizationService配仓储mock的隔离Java探针复现两次明文且未消费。不是只看注释推断。
前提限制：不宣称本次跑全后端/数据库迁移/全页面；联系人旧数据未回填；单测不证明数据库并发，只确认已有CAS源码及授权真实调用路径。既有V427顺序红不在本批顺便修。

## 逐文件证据及分类
- backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java：17行，SHA256 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277。主体类型/法人资料及生命周期载体，无固定父链。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java：19行，SHA256 f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422。四类主体分字段，effectiveFrom/effectiveTo状态用于生效；本身不推导财税权限。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/MerchantRecord.java：116行，SHA256 dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00。兼容构造、project、存档删除状态及withMaskedPhones复制保留非电话字段。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java：9行，SHA256 e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40。审批决定载体，申请ID/对象/状态不扩审批者范围。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java：1338行，SHA256 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855。项目分页COUNT/items同条件；名字与电话分别PiiCipher；merchant行锁及未结束绑定排他；软删除、占用检查、审批CAS。全文1338行。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformController.java：296行，SHA256 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2。七个商户响应统一经两个maskPhones重载→canViewPhonePlaintext；此helper仅查询权限，单次不消费。其他入口权限/幂等/范围不绕过。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformRepository.java：204行，SHA256 df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc。接口默认方法兼容原实现；生产Jdbc实现覆盖项目分页/绑定/审批读写。未据默认空实现新增需求。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformService.java：1179行，SHA256 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e。当前project fail closed、真实最高身份、主体关系/未来生效、终止禁止、存档恢复/占用/自审、元数据审计、可空项目资料；全文1179行。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/package-info.java：1行，SHA256 e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9。包声明，技术性。 分类：技术性。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java：17行，SHA256 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da。明确四类主体及生效日期/变更理由，非父链。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java：26行，SHA256 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19。既有枚举及中文校验，非新增主体种类。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java：6行，SHA256 e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93。幂等键和理由载体，服务层再校验。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java：5行，SHA256 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4。批准动作及理由，身份由服务判断。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java：5行，SHA256 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45。删除申请及理由，不直接物理删除。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java：41行，SHA256 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1。主次联系人/经营/面积租金/主体资料；不增加新业务类别。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java：26行，SHA256 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108。项目负责人地址经营范围可空，后台/数据库范围不等于页面可用。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java：21行，SHA256 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa。项目/商户/父空间和空间资料，服务层验证归属。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java：16行，SHA256 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34。仅create入参，注释无更新承诺，不新增更新入口。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java：11行，SHA256 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f。action/reason/幂等键通用载体，具体白名单在服务。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java：22行，SHA256 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1。营业时段委托已有服务，未将本模块扩至场地排程。 分类：细化（缺陷单独列058冲突）。

## 三线：顺从式与对抗式
顺从：当前项目列表按同一project过滤、主管身份并不等于真实密钥最高权限；存档/删除不擦库、有理由/占用/申请人禁自审；已删除空间类型可恢复；项目资料三字段可空；未来主体绑定inactive直到日期，不能立即改结算主体。名单/枚举对应六合作、六业态；二级请求不产生第七类。
对抗：无项目/未知项目/跨项目主体、已有主体被换、archived商户绑定、未来日期强行激活、重复未结束绑定、ordinary终止、删除本人审批、申请理由空均追到原拒绝路径。现有测试含这些反例。角色平台管理员敏感权限不自动放行，显式拒绝优先，失效/已消费授权无效；但Controller用不消费的boolean判断导致单次许可可重复。并发CAS失败应拒绝明文，不应改boolean公共方法为有副作用。
实际流程：Controller先功能权限/范围及service成功，再输出号码。service.listMerchants已有merchant.viewed记录实际返回IDs/分页/筛选/project，不以新增明文查看端点代替。七调用共享helper，因此修该helper可覆盖全部，列表一次响应只消费一次，不能每商户消费。

## 冻结判据逐条矩阵
- BEQR-063：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-074：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-143：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-144：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-145：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-146：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-147：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-148：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-149：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-150：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-151：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-152：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-153：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-154：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-155：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-156：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-157：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-158：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-159：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-166：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-189：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BSM-001：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-003：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-004：商户和空间类型archive/delete/restore、理由占用审批及真实master路径符合本模块范围。
- BSM-005：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-006：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-007：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- BSM-009：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-028：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-029：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-072：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-074：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-114：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-115：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-120：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- GEN-001：存在理由见第一性；复用既有授权消费，不新造服务。
- GEN-002：109测试与隔离探针；058存在实际错误。
- GEN-003：原14Controller测试仅mock boolean明文，没有消费断言，须058TDD。
- GEN-004：DTO校验+服务枚举/理由/范围/非负；SQL参数化。
- GEN-005：真实master身份/显式DENY/平台与项目范围有效；058单次消费遗漏。
- GEN-006：软删除/保留历史；名字加密写解密读；旧数据限制明确。
- GEN-007：幂等executor及merchant FOR UPDATE/未结束绑定；授权已有原子消费未被调用。
- GEN-008：请求/记录/SQL字段匹配，项目字段和主次名字一致。
- GEN-009：非法状态/未知对象/审批结果明确拒绝；消费失败不得返回明文。
- GEN-010：权限boolean不泄钥，敏感号码mask路径；058缺陷。
- GEN-011：merchant.viewed实际IDs/entry/project/filters留痕；复用权限消费日志。
- GEN-012：分页cap500/longoffset/稳定order，无正常路径资源无限增长证据。
- GEN-013：使用已有Spring/JDBC/PiiCipher/Mockito，无加依赖。
- GEN-014：本模块不新迁移；V427/V431既有输入，未跑生产库。
- GEN-015：无页面文件；中文业务异常/DTO，内部标识不翻译成业务新枚举。
- GEN-016：旧闭环/旧注释不冒充当前完成；013不含UI，059不含回填。
- GEN-017：依冲突→细化→技术性→添加；058冲突仅删除不消费的放行路径，无新增功能。
- GEN-018：HTML原文+完整源码+真实复现命令证据，不引用假工具结果。
- PGM-001：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-002：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-003：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-004：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-005：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-006：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-007：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-008：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-009：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-010：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-011：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-012：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-013：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-014：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-015：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-016：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-017：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-018：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-019：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-020：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-021：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-026：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-027：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-031：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-035：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-040：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- PGM-041：L656单次消费仍冲突，见058；其它有效期/拒绝/范围追AuthorizationService。
- PGM-042：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- PGM-043：获批PGM V0.13商户主次联系人查看例外生效；范围与留痕仍需执行；本模块没有导出打印入口，不扩大到其他PII。
- PGM-044：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-048：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-059：获批PGM V0.13商户主次联系人查看例外生效；范围与留痕仍需执行；本模块没有导出打印入口，不扩大到其他PII。
- PGM-060：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-065：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-066：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。

## 当前26项既有输入逐项重判
- M009-038：项目隔离及多对多主体关联有效，master不整类关闭隔离。当前本批队列否，历史修复事实保留。
- M009-039：空间当前项目与父空间、商户一致。当前本批队列否，历史修复事实保留。
- M009-040：普通资料修改不能换非空财税主体；不补审批系统。当前本批队列否，历史修复事实保留。
- M009-041：未来绑定不立即投影，排他行锁及日期有效。当前本批队列否，历史修复事实保留。
- M009-031：非空财税主体不能借停用清空，历史绑定不回写。当前本批队列否，历史修复事实保留。
- M009-042：disabled空间类型走已有恢复入口。当前本批队列否，历史修复事实保留。
- M009-043：对象/理由/决定审计元数据存在。当前本批队列否，历史修复事实保留。
- M009-044：pending申请批准/驳回更新最终状态。当前本批队列否，历史修复事实保留。
- M009-045：真正到期且inactive绑定排除占用，未来inactive不排除。当前本批队列否，历史修复事实保留。
- M009-046：面积、租金、容量非负校验，未发明上限。当前本批队列否，历史修复事实保留。
- M009-030：新建给定主体先校验当前项目存在。当前本批队列否，历史修复事实保留。
- M009-047：用户展示校验语句中文，内部enum保持内部码。当前本批队列否，历史修复事实保留。
- M009-018：普通档案路径不能直接terminated，不新增退租流程。当前本批队列否，历史修复事实保留。
- M009-013：三项目字段已保存返回、可空、后端DB范围；旧数据无自动填充。当前本批队列否，历史修复事实保留。
- M009-054：此前岗位模板入口修正作为既有输入，不跨模块重修。当前本批队列否，历史修复事实保留。
- M009-055：此前商户范围测试前提修正保留，不放宽生产隔离。当前本批队列否，历史修复事实保留。
- M009-056：此前受益人测试资料修正保留，不绕过利益冲突。当前本批队列否，历史修复事实保留。
- M009-048：项目范围商户列表为HTML下位细化。当前本批队列否，历史修复事实保留。
- M009-049：既有审批链仓储为下位实现，本模块不发明角色链。当前本批队列否，历史修复事实保留。
- M009-050：电话加密基础有效，与058单次遗漏和059姓名分别处理。当前本批队列否，历史修复事实保留。
- M009-051：稳定分页及上限500/long偏移技术实现有效。当前本批队列否，历史修复事实保留。
- M009-052：拒绝直接DELETE、走软删除审批为细化。当前本批队列否，历史修复事实保留。
- M009-053：包声明技术性。当前本批队列否，历史修复事实保留。
- M009-057：空间类型注释与实际create行为一致。当前本批队列否，历史修复事实保留。
- M009-058：残余冲突：单次授权查看两次仍有效；当前HTML已允许持权明文，旧全面禁止判断失效；对象查看元数据已补。候选当前缺陷是、队列是，待B2。
- M009-059：主次联系人姓名新建/修改加密并读取解密；旧行未重存仍不保证加密，不回填。当前本批队列否，历史修复事实保留。

## 唯一修复候选M009-058
分类冲突，证据PGM041 L656（临时授权可设有效期或单次使用，单次消耗留痕）、PGM059获批查看例外+GEN002/005/010。
PlatformController281—283仅调用currentActorHasPermission；AuthorizationService266—278只读，36—60 requirePermission(s)才调用markSingleUsePermissionConsumed；JdbcAccessRepository1011—1062消费CAS同时记录usage_logs。真实两次调用probe输出first=18800000011 second=18800000011 consumed=false；javac0/java1 AssertionError。因此删除“只查询即明文”路径，持权后必须调用现有requirePermission再return true；无权仍false。只改本controller共享helper，公共boolean保持只读。必要最小回归覆盖列表/单记录/消费失败/无权，不新业务端点。
批准来源：操作员“都批准修改”的第三项；前两项HTML已落地不是本次再次修改授权。B3累计4，追加一次至5仅限已定稿队列。

## 可复现命令与实测
首次误用./mvnw得到FileNotFoundError，仓库无wrapper，未运行测试；纠正为cd backend && mvn -q -Dtest=PlatformServiceTests,PlatformControllerTests,AuthorizationServiceTests test：exit0，70+14+25=109，failure/error/skipped均0。
探针在/tmp/m009-single-use-绝对临时目录、退出自动清理，classpath取本次surefire XML java.class.path，javac/java执行真实Controller和AuthorizationService。仓储mock只有一次可消费许可，service返回两商户同页；两次调用listMerchants(false,null,1,50)。完整源码如下，可按相同classpath重复：
```java
import com.complex.platform.access.*;
import com.complex.platform.governance.*;
import com.complex.platform.common.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
public class SingleUseProbe {
 public static void main(String[] args) {
  AccessRepository repo=mock(AccessRepository.class);
  AtomicBoolean consumed=new AtomicBoolean();
  when(repo.permissionExists(anyString())).thenReturn(true);
  when(repo.findPermissionEffects(eq("probe"),any())).thenAnswer(x->List.of(
   new AccessRepository.PermissionEffect("probe","merchant.profile.view",AccessRepository.Effect.GRANT,AccessRepository.Source.ROLE,"role",null,null,false,false),
   new AccessRepository.PermissionEffect("probe","pii.contact_phone.view",AccessRepository.Effect.GRANT,AccessRepository.Source.TEMPORARY_GRANT,"once",null,null,true,consumed.get())));
  when(repo.markSingleUsePermissionConsumed(eq("once"),eq("pii.contact_phone.view"),eq("probe"),any())).thenAnswer(x->consumed.compareAndSet(false,true));
  var auth=new AuthorizationService(repo);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor("probe","probe","staff","project_a",null,"active"),"token",List.of()));
  var service=mock(PlatformService.class);
  var m=new MerchantRecord("m","商户","other",null,null,null,"甲","18800000011",null,null,null,null,"乙","18800000013",null,null,"active",Instant.EPOCH,Instant.EPOCH);
  when(service.listMerchants(false,null,1,50)).thenReturn(new PageResponse<>(List.of(m,m),1,50,2));
  var controller=new PlatformController(service,auth,mock(VenueBusinessHoursService.class));
  String first=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  String second=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  System.out.println("first="+first+" second="+second+" consumed="+consumed.get());
  if (!first.equals("18800000011")||!second.equals("188****0011")||!consumed.get()) throw new AssertionError("单次授权未在第一次查看后消费");
 }
}
```

## 观察与范围
OBS01 createSpaceType幂等由既有executor负责；OBS02营业时段仅委托已有服务，不扩模块；OBS03owner_person/legal_representative未在本批准姓名范围，不擅加密；OBS04V339/V394权限种子属对应模块，不能以管理员身份默认敏感。
XM008仅PlatformController归本模块，其余四控制器回所属B1；XM010只已批准两联系人姓名，其余生日姓名按归属；XM011人脸比对、XM012测试库卫生均其他模块既有输入，不拿它们当本模块修复授权。
技能沿用本轮已实际读取并收窄的using-superpowers、grilling、codebase-design、diagnosing-bugs/systematic-debugging、verification等；本段实际用追问/追数据流/反例命令，无子代理。Graft已在源码前查PlatformController权限路径；不把检索当全文件读。

结论：B1 FAIL（M009-058单次消费实际缺陷）；生命周期IN_PROGRESS，未修改生产代码/测试，未新增判据或功能，等待双席完整报告后B2归并。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 现锁B1 Codex完整独立审计
版本：V0.16

当前M009 / 471，B3累计4，生命周期IN_PROGRESS。保留V0.01—V0.15全部历史；V0.15封存后仍未读取Grok本轮报告，追加同授权组正常路径的实测，修正候选修法。Grok先派发，本报告封存前未读对席本次正文。正本main HEAD e456ba96c84185c8d39f18f548011daac48a6466。
全部20FILES逐行阅读3375行（计划历史2476不冒充当前），全部88判据定义与HTML摘录已读。原计划不自行重切。当前PGM V0.13已批准页面明文例外及导出边界，L656单次规则不变。

## 第一性原理
存在理由：项目/空间/商户/公司及唯一四类绑定是治理归属底座，来自PGM017—027/BSM028；去除这些会失去业务对象及范围。DTO、分页、SQL和审计结构是其细化/技术承载，不能因HTML没有Java方法名就删除。
真实行为：全20文件读完，追所有7个商户返回调用；实际109测试0失败；真实AuthorizationService配仓储mock的隔离Java探针复现两次明文且未消费。不是只看注释推断。
前提限制：不宣称本次跑全后端/数据库迁移/全页面；联系人旧数据未回填；单测不证明数据库并发，只确认已有CAS源码及授权真实调用路径。既有V427顺序红不在本批顺便修。

## 逐文件证据及分类
- backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java：17行，SHA256 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277。主体类型/法人资料及生命周期载体，无固定父链。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java：19行，SHA256 f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422。四类主体分字段，effectiveFrom/effectiveTo状态用于生效；本身不推导财税权限。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/MerchantRecord.java：116行，SHA256 dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00。兼容构造、project、存档删除状态及withMaskedPhones复制保留非电话字段。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java：9行，SHA256 e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40。审批决定载体，申请ID/对象/状态不扩审批者范围。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java：1338行，SHA256 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855。项目分页COUNT/items同条件；名字与电话分别PiiCipher；merchant行锁及未结束绑定排他；软删除、占用检查、审批CAS。全文1338行。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformController.java：296行，SHA256 9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2。七个商户响应统一经两个maskPhones重载→canViewPhonePlaintext；此helper仅查询权限，单次不消费。其他入口权限/幂等/范围不绕过。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformRepository.java：204行，SHA256 df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc。接口默认方法兼容原实现；生产Jdbc实现覆盖项目分页/绑定/审批读写。未据默认空实现新增需求。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/PlatformService.java：1179行，SHA256 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e。当前project fail closed、真实最高身份、主体关系/未来生效、终止禁止、存档恢复/占用/自审、元数据审计、可空项目资料；全文1179行。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/package-info.java：1行，SHA256 e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9。包声明，技术性。 分类：技术性。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java：17行，SHA256 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da。明确四类主体及生效日期/变更理由，非父链。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java：26行，SHA256 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19。既有枚举及中文校验，非新增主体种类。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java：6行，SHA256 e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93。幂等键和理由载体，服务层再校验。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java：5行，SHA256 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4。批准动作及理由，身份由服务判断。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java：5行，SHA256 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45。删除申请及理由，不直接物理删除。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java：41行，SHA256 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1。主次联系人/经营/面积租金/主体资料；不增加新业务类别。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java：26行，SHA256 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108。项目负责人地址经营范围可空，后台/数据库范围不等于页面可用。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java：21行，SHA256 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa。项目/商户/父空间和空间资料，服务层验证归属。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java：16行，SHA256 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34。仅create入参，注释无更新承诺，不新增更新入口。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java：11行，SHA256 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f。action/reason/幂等键通用载体，具体白名单在服务。 分类：细化（缺陷单独列058冲突）。
- backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java：22行，SHA256 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1。营业时段委托已有服务，未将本模块扩至场地排程。 分类：细化（缺陷单独列058冲突）。

## 三线：顺从式与对抗式
顺从：当前项目列表按同一project过滤、主管身份并不等于真实密钥最高权限；存档/删除不擦库、有理由/占用/申请人禁自审；已删除空间类型可恢复；项目资料三字段可空；未来主体绑定inactive直到日期，不能立即改结算主体。名单/枚举对应六合作、六业态；二级请求不产生第七类。
对抗：无项目/未知项目/跨项目主体、已有主体被换、archived商户绑定、未来日期强行激活、重复未结束绑定、ordinary终止、删除本人审批、申请理由空均追到原拒绝路径。现有测试含这些反例。角色平台管理员敏感权限不自动放行，显式拒绝优先，失效/已消费授权无效；但Controller用不消费的boolean判断导致单次许可可重复。并发CAS失败应拒绝明文，不应改boolean公共方法为有副作用。
实际流程：Controller先功能权限/范围及service成功，再输出号码。service.listMerchants已有merchant.viewed记录实际返回IDs/分页/筛选/project，不以新增明文查看端点代替。七调用共享helper，但简单给helper增加requirePermission不够：功能权与电话权在同一单次授权组时，前置功能检查已消费整组，第一响应反而被脱敏。需在各七入口取本次是否请求明文，调用已有requirePermissions把功能权和电话权一起核验/按来源一次消费，再将本次结果传入两个mask重载；列表不能按每条记录消费。

## 冻结判据逐条矩阵
- BEQR-063：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-074：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-143：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-144：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-145：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-146：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-147：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-148：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-149：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-150：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-151：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-152：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-153：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-154：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-155：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-156：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-157：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-158：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-159：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-166：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BEQR-189：核对本条枚举原文与实体/DTO/service白名单及状态；四主体、合作业态、空间与存档审批语义一致；超出本模块的财务收费/角色下游不据此补功能。
- BSM-001：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-003：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-004：商户和空间类型archive/delete/restore、理由占用审批及真实master路径符合本模块范围。
- BSM-005：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-006：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-007：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- BSM-009：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-028：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-029：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-072：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-074：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-114：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-115：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-120：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- GEN-001：存在理由见第一性；复用既有授权消费，不新造服务。
- GEN-002：109测试与隔离探针；058存在实际错误。
- GEN-003：原14Controller测试仅mock boolean明文，没有消费断言，须058TDD。
- GEN-004：DTO校验+服务枚举/理由/范围/非负；SQL参数化。
- GEN-005：真实master身份/显式DENY/平台与项目范围有效；058单次消费遗漏。
- GEN-006：软删除/保留历史；名字加密写解密读；旧数据限制明确。
- GEN-007：幂等executor及merchant FOR UPDATE/未结束绑定；授权已有原子消费未被调用。
- GEN-008：请求/记录/SQL字段匹配，项目字段和主次名字一致。
- GEN-009：非法状态/未知对象/审批结果明确拒绝；消费失败不得返回明文。
- GEN-010：权限boolean不泄钥，敏感号码mask路径；058缺陷。
- GEN-011：merchant.viewed实际IDs/entry/project/filters留痕；复用权限消费日志。
- GEN-012：分页cap500/longoffset/稳定order，无正常路径资源无限增长证据。
- GEN-013：使用已有Spring/JDBC/PiiCipher/Mockito，无加依赖。
- GEN-014：本模块不新迁移；V427/V431既有输入，未跑生产库。
- GEN-015：无页面文件；中文业务异常/DTO，内部标识不翻译成业务新枚举。
- GEN-016：旧闭环/旧注释不冒充当前完成；013不含UI，059不含回填。
- GEN-017：依冲突→细化→技术性→添加；058冲突仅删除不消费的放行路径，无新增功能。
- GEN-018：HTML原文+完整源码+真实复现命令证据，不引用假工具结果。
- PGM-001：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-002：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-003：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-004：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-005：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-006：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-007：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-008：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-009：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-010：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-011：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-012：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-013：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-014：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-015：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-016：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-017：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-018：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-019：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-020：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-021：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-026：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-027：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-031：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-035：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-040：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- PGM-041：L656单次消费仍冲突，见058；其它有效期/拒绝/范围追AuthorizationService。
- PGM-042：单功能权限与最高身份分别判断；职级不是密钥权限；单次残余归058。
- PGM-043：获批PGM V0.13商户主次联系人查看例外生效；范围与留痕仍需执行；本模块没有导出打印入口，不扩大到其他PII。
- PGM-044：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-048：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-059：获批PGM V0.13商户主次联系人查看例外生效；范围与留痕仍需执行；本模块没有导出打印入口，不扩大到其他PII。
- PGM-060：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-065：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-066：完整读取该锚点正文并审查适用性；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。

## 当前26项既有输入逐项重判
- M009-038：项目隔离及多对多主体关联有效，master不整类关闭隔离。当前本批队列否，历史修复事实保留。
- M009-039：空间当前项目与父空间、商户一致。当前本批队列否，历史修复事实保留。
- M009-040：普通资料修改不能换非空财税主体；不补审批系统。当前本批队列否，历史修复事实保留。
- M009-041：未来绑定不立即投影，排他行锁及日期有效。当前本批队列否，历史修复事实保留。
- M009-031：非空财税主体不能借停用清空，历史绑定不回写。当前本批队列否，历史修复事实保留。
- M009-042：disabled空间类型走已有恢复入口。当前本批队列否，历史修复事实保留。
- M009-043：对象/理由/决定审计元数据存在。当前本批队列否，历史修复事实保留。
- M009-044：pending申请批准/驳回更新最终状态。当前本批队列否，历史修复事实保留。
- M009-045：真正到期且inactive绑定排除占用，未来inactive不排除。当前本批队列否，历史修复事实保留。
- M009-046：面积、租金、容量非负校验，未发明上限。当前本批队列否，历史修复事实保留。
- M009-030：新建给定主体先校验当前项目存在。当前本批队列否，历史修复事实保留。
- M009-047：用户展示校验语句中文，内部enum保持内部码。当前本批队列否，历史修复事实保留。
- M009-018：普通档案路径不能直接terminated，不新增退租流程。当前本批队列否，历史修复事实保留。
- M009-013：三项目字段已保存返回、可空、后端DB范围；旧数据无自动填充。当前本批队列否，历史修复事实保留。
- M009-054：此前岗位模板入口修正作为既有输入，不跨模块重修。当前本批队列否，历史修复事实保留。
- M009-055：此前商户范围测试前提修正保留，不放宽生产隔离。当前本批队列否，历史修复事实保留。
- M009-056：此前受益人测试资料修正保留，不绕过利益冲突。当前本批队列否，历史修复事实保留。
- M009-048：项目范围商户列表为HTML下位细化。当前本批队列否，历史修复事实保留。
- M009-049：既有审批链仓储为下位实现，本模块不发明角色链。当前本批队列否，历史修复事实保留。
- M009-050：电话加密基础有效，与058单次遗漏和059姓名分别处理。当前本批队列否，历史修复事实保留。
- M009-051：稳定分页及上限500/long偏移技术实现有效。当前本批队列否，历史修复事实保留。
- M009-052：拒绝直接DELETE、走软删除审批为细化。当前本批队列否，历史修复事实保留。
- M009-053：包声明技术性。当前本批队列否，历史修复事实保留。
- M009-057：空间类型注释与实际create行为一致。当前本批队列否，历史修复事实保留。
- M009-058：残余冲突：单次授权查看两次仍有效；当前HTML已允许持权明文，旧全面禁止判断失效；对象查看元数据已补。候选当前缺陷是、队列是，待B2。
- M009-059：主次联系人姓名新建/修改加密并读取解密；旧行未重存仍不保证加密，不回填。当前本批队列否，历史修复事实保留。

## 唯一修复候选M009-058
分类冲突，证据PGM041 L656（临时授权可设有效期或单次使用，单次消耗留痕）、PGM059获批查看例外+GEN002/005/010。
PlatformController281—283仅调用currentActorHasPermission；AuthorizationService266—278只读，36—60 requirePermission(s)才调用markSingleUsePermissionConsumed；JdbcAccessRepository1011—1062消费CAS同时记录usage_logs。真实两次调用probe输出first=18800000011 second=18800000011 consumed=false；javac0/java1 AssertionError。因此删除“只查询即明文”路径，复用已有requirePermissions一次核验功能权和可选电话权；公共boolean保持只读。七入口共用一个接收功能权限码的现有helper改法，拿到本次明文许可后供两个mask重载使用，防同组双消费/消费后再判无权。无电话权只校验原功能权并脱敏。必要最小回归覆盖列表/单记录、同组单次、单独电话单次、消费失败、无权；不新业务端点。
批准来源：操作员“都批准修改”的第三项；前两项HTML已落地不是本次再次修改授权。B3累计4，追加一次至5仅限已定稿队列。

## 可复现命令与实测
首次误用./mvnw得到FileNotFoundError，仓库无wrapper，未运行测试；纠正为cd backend && mvn -q -Dtest=PlatformServiceTests,PlatformControllerTests,AuthorizationServiceTests test：exit0，70+14+25=109，failure/error/skipped均0。
探针在/tmp/m009-single-use-绝对临时目录、退出自动清理，classpath取本次surefire XML java.class.path，javac/java执行真实Controller和AuthorizationService。仓储mock只有一次可消费许可，service返回两商户同页；两次调用listMerchants(false,null,1,50)。完整源码如下，可按相同classpath重复：
```java
import com.complex.platform.access.*;
import com.complex.platform.governance.*;
import com.complex.platform.common.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
public class SingleUseProbe {
 public static void main(String[] args) {
  AccessRepository repo=mock(AccessRepository.class);
  AtomicBoolean consumed=new AtomicBoolean();
  when(repo.permissionExists(anyString())).thenReturn(true);
  when(repo.findPermissionEffects(eq("probe"),any())).thenAnswer(x->List.of(
   new AccessRepository.PermissionEffect("probe","merchant.profile.view",AccessRepository.Effect.GRANT,AccessRepository.Source.ROLE,"role",null,null,false,false),
   new AccessRepository.PermissionEffect("probe","pii.contact_phone.view",AccessRepository.Effect.GRANT,AccessRepository.Source.TEMPORARY_GRANT,"once",null,null,true,consumed.get())));
  when(repo.markSingleUsePermissionConsumed(eq("once"),eq("pii.contact_phone.view"),eq("probe"),any())).thenAnswer(x->consumed.compareAndSet(false,true));
  var auth=new AuthorizationService(repo);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor("probe","probe","staff","project_a",null,"active"),"token",List.of()));
  var service=mock(PlatformService.class);
  var m=new MerchantRecord("m","商户","other",null,null,null,"甲","18800000011",null,null,null,null,"乙","18800000013",null,null,"active",Instant.EPOCH,Instant.EPOCH);
  when(service.listMerchants(false,null,1,50)).thenReturn(new PageResponse<>(List.of(m,m),1,50,2));
  var controller=new PlatformController(service,auth,mock(VenueBusinessHoursService.class));
  String first=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  String second=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  System.out.println("first="+first+" second="+second+" consumed="+consumed.get());
  if (!first.equals("18800000011")||!second.equals("188****0011")||!consumed.get()) throw new AssertionError("单次授权未在第一次查看后消费");
 }
}
```

## 观察与范围
OBS01 createSpaceType幂等由既有executor负责；OBS02营业时段仅委托已有服务，不扩模块；OBS03owner_person/legal_representative未在本批准姓名范围，不擅加密；OBS04V339/V394权限种子属对应模块，不能以管理员身份默认敏感。
XM008仅PlatformController归本模块，其余四控制器回所属B1；XM010只已批准两联系人姓名，其余生日姓名按归属；XM011人脸比对、XM012测试库卫生均其他模块既有输入，不拿它们当本模块修复授权。
技能沿用本轮已实际读取并收窄的using-superpowers、grilling、codebase-design、diagnosing-bugs/systematic-debugging、verification等；本段实际用追问/追数据流/反例命令，无子代理。Graft已在源码前查PlatformController权限路径；不把检索当全文件读。

结论：B1 FAIL（M009-058单次消费实际缺陷）；生命周期IN_PROGRESS，未修改生产代码/测试，未新增判据或功能，等待双席完整报告后B2归并。

## V0.16新增反证（同一058，非新增业务需求）
同上隔离探针将merchant.profile.view也设为TEMPORARY_GRANT来源once且singleUse=true，并使消费mock接受该组任一permissionCode，首次响应后立即打印。实跑javac0/java1：FIRST=188****0011 consumed=true；第二次抛PERMISSION_DENIED merchant.profile.view。首次查看权与电话权都有效却未返回明文，证明简单在后置helper追加消费无法覆盖普通的同组授权。现有AuthorizationService.requirePermissions在同次调用先核全部权限，再按source+sourceId合并消费，正好复用，无须修改全局授权语义。以下为完整可重复探针：
```java
import com.complex.platform.access.*;
import com.complex.platform.governance.*;
import com.complex.platform.common.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
public class SingleUseProbe {
 public static void main(String[] args) {
  AccessRepository repo=mock(AccessRepository.class);
  AtomicBoolean consumed=new AtomicBoolean();
  when(repo.permissionExists(anyString())).thenReturn(true);
  when(repo.findPermissionEffects(eq("probe"),any())).thenAnswer(x->List.of(
   new AccessRepository.PermissionEffect("probe","merchant.profile.view",AccessRepository.Effect.GRANT,AccessRepository.Source.TEMPORARY_GRANT,"once",null,null,true,consumed.get()),
   new AccessRepository.PermissionEffect("probe","pii.contact_phone.view",AccessRepository.Effect.GRANT,AccessRepository.Source.TEMPORARY_GRANT,"once",null,null,true,consumed.get())));
  when(repo.markSingleUsePermissionConsumed(eq("once"),anyString(),eq("probe"),any())).thenAnswer(x->consumed.compareAndSet(false,true));
  var auth=new AuthorizationService(repo);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor("probe","probe","staff","project_a",null,"active"),"token",List.of()));
  var service=mock(PlatformService.class);
  var m=new MerchantRecord("m","商户","other",null,null,null,"甲","18800000011",null,null,null,null,"乙","18800000013",null,null,"active",Instant.EPOCH,Instant.EPOCH);
  when(service.listMerchants(false,null,1,50)).thenReturn(new PageResponse<>(List.of(m,m),1,50,2));
  var controller=new PlatformController(service,auth,mock(VenueBusinessHoursService.class));
  String first=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  System.out.println("FIRST="+first+" consumed="+consumed.get());\n  String second=controller.listMerchants(false,null,1,50).data().items().get(0).contactPhone();
  System.out.println("first="+first+" second="+second+" consumed="+consumed.get());
  if (!first.equals("18800000011")||!second.equals("188****0011")||!consumed.get()) throw new AssertionError("单次授权未在第一次查看后消费");
 }
}
```
本次完整B1结论仍FAIL，仅058残余单次行为；先双席B2定稿，不据本段动生产代码。


## M009现锁B2归并 Codex
版本：V0.17
输入：本路径完整B1V0.16及M009-b1-grok-raw.md完整B1V0.15；候选findingsV3.47。26项差集无遗漏，唯一GR-M009-B1-01并入旧058不另分ID。分类冲突、队列是，其余否。真实同组反证修正后置requirePermission方案为前置requirePermissions同次核验，消费后本次许可传入mask；限制只Controller与现有测试，既有功能权限/范围保持，不新增业务。OBS编号差异已在候选澄清，059旧行未回填保留批准边界。Codex B2结论PASS，等Grok独立复核后才定稿。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 第6次发布修复前 Codex 完整B1
版本：V0.18
结论：FAIL。用户已授权追加一次发布修复；B3累计5未执行第6次。Grok先派发，本报告封存前未读本次对席报告。
20源当前3381行与此前完整读取的M009-b5-r5-self-raw.md逐hash全部相同；全文读取证据复用，不冒称刚重新逐行读3381行。当前88锁项重新判断以下逐项表；冻结锁/三HTML未变。上一轮隔离候选全部backend生产Java与正本同字节，clean 299例绿可复用，但不证明迁移号或新库发布已通。
第一性原理：项目三资料和M:N归属已有HTML与逐项批准，不能因打包故障删掉业务实现。此次必要动作只是让已审实现可按现有Flyway发布序列执行及事实清单对应；不创造角色、字段、权限、开关或页面。
顺从式：同源20文件DTO/服务/JDBC/控制器与获批PGM680/871和原656保持，26历史项全核；发布GEN014/016旧观察在本次明确发布任务下实际阻挡正常第一次发布，必须入队。
对抗式：源码单测全绿不足以覆盖430基线；新号若与431冲突会失败，已执行427测试库直接改名会触发Flyway missing/重复DDL；不改其history，不把repair/删库当测试；隔离库独立验证430升级和空库全迁移。旧审计引用记录原当时V427是历史事实，不全局替换。
## 本次临时 findings（完整）
|编号|分类|工程缺陷|证据/冻结判据|最小范围与无业务发明|
|---|---|---|---|---|
|CX-PUB-01|技术性|是|V427未发布且小于origin/main最大V430，现有verify:migration-version-order退出1；GEN014/013/006|改未发迁移到执行时空闲高号（当前V432，V431已用于姓名扩容），保持SQL业务目标。旧库427/431历史完整留存；独立库验证空库和430升级。非新增业务。原013/038业务已修，本次为独立发布缺陷，建议新ID060，不假写原业务缺失。|
|CX-PUB-02|技术性|是|候选predeploy首红domain-ownership-scope CSV漂移；新表/项目字段与两自动生成材料未同批；GEN016/008|现有两生成器机械重生成，更新已有文件版本与必要现行引用，不改生成器规则，不改HTML或旧raw，建议新ID061。生成镜像不增加业务。|
## 发布依赖观察（不误作新增缺陷）
AuthorizationService/SessionIssuer真实master身份、认证签发链和既有审批/单次消费来源语义须随当前代码集核验，不能只因编译可过就漏掉运行时依赖。最新用户授权发布已审字节，不授权发明其它功能。旧未合并PR不按标题直接合并；候选以远端main为底，保持已合并M013—M037内容。JdbcAccessRepository是否需要完整带入必须按CAS授权组语义核实；本批不修改其业务逻辑。
## 全文件指纹（本次重算均匹配）
- backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java：17行；SHA256 45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277。
- backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java：19行；SHA256 f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422。
- backend/src/main/java/com/complex/platform/governance/MerchantRecord.java：116行；SHA256 dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00。
- backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java：9行；SHA256 e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40。
- backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java：1338行；SHA256 84c868bb9739fc68d630ac6c7da1c99767c7a974ba2e763102ba473f52618855。
- backend/src/main/java/com/complex/platform/governance/PlatformController.java：302行；SHA256 53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6。
- backend/src/main/java/com/complex/platform/governance/PlatformRepository.java：204行；SHA256 df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc。
- backend/src/main/java/com/complex/platform/governance/PlatformService.java：1179行；SHA256 993cfdb8cdacb285a37027c8bfda1c983fcb5c3e0f26f7462f2235454f45827e。
- backend/src/main/java/com/complex/platform/governance/package-info.java：1行；SHA256 e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java：17行；SHA256 66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da。
- backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java：26行；SHA256 18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java：6行；SHA256 e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java：5行；SHA256 2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java：5行；SHA256 4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45。
- backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java：41行；SHA256 7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1。
- backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java：26行；SHA256 6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java：21行；SHA256 63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa。
- backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java：16行；SHA256 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34。
- backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java：11行；SHA256 75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f。
- backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java：22行；SHA256 6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1。

## 本次全88判据判断
- BEQR-063：酒吧高危动作无本模块执行入口；共享审批仓储不自行定义沽清或双证据规则。
- BEQR-074：隐私桌台不在本模块；商户档案权限不冒充桌台三类权限。
- BEQR-143：BindingRecord四类字段及Service创建/激活；同merchant行锁+未结束记录检查保持唯一。
- BEQR-144：business_entity_projects多对多，无固定父公司链。
- BEQR-145：主体角色区分project_company，无独立资产业态或统管其它经营域逻辑。
- BEQR-146：六业态技术代码没有新增运营公司权责或跨域管理。
- BEQR-147：不定义收入拆分/平台费，绑定不自动生成收入。
- BEQR-148：六合作方式白名单；其他收费模板生效检查属合同模块，本模块不放行合同。
- BEQR-149：无新增职级；删除审批凭真实master而非最高职级。
- BEQR-150：无终端增减，控制器为现有员工后台。
- BEQR-151：requirePermissions沿用拒绝优先和有效来源；本次不改变权限计算规则。
- BEQR-152：Service.currentProject与所有范围分页谓词一致；未知项目空集、越域写拒绝；页面查看无导出能力。
- BEQR-153：不实现终端开关，不引绕过或留痕关闭开关。
- BEQR-154：项目六态白名单与HTML一致。
- BEQR-155：商户业务状态与存档轴分开；直接退出被拒绝，无新立即终止路径。
- BEQR-156：无合同状态执行入口，不改变合同当地日期规则。
- BEQR-157：无员工状态写入路径。
- BEQR-158：无设备状态写入路径。
- BEQR-159：无权限配置状态新增；消费复用现有引擎。
- BEQR-166：审批链仓储保存与CAS承载既有模型；不声称本模块完成合同三类流程。
- BEQR-189：无服务管理费收取/费率配置路径。
- BSM-001：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-003：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-004：商户和空间类型archive/delete/restore、理由占用审批及真实master路径符合本模块范围。
- BSM-005：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-006：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-007：日常管理员不自动享PII；真实密钥身份独立；单次已修。
- BSM-009：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-028：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-029：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-072：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-074：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- BSM-114：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-115：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- BSM-120：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- GEN-001：复用原授权核验与消费；本批仅两文件，不新建功能或依赖。
- GEN-002：上一执行轮候选clean编译10类299例全PASS，源码逐字节保持；本B1只复用明确同字节证据，不谎称新跑。覆盖项目/商户/生命周期/权限/加密/租赁与审批。迁移未来新号另须隔离实测。
- GEN-003：4组列表/写回×单独/同组单次均检查第一次和第二次及消费次数；消费失败HTTP403不调用业务服务。
- GEN-004：DTO校验/服务范围、枚举、理由、非负、引用；SQL使用绑定参数。
- GEN-005：七响应路径合并原功能权和可选PII权；无PII权仍原功能+脱敏；真实master/显式DENY不改。
- GEN-006：软删除和归属保留；名字写加密读解密；项目新三字段可空；无旧数据已全回填声称。
- GEN-007：现有幂等执行器、merchant行锁、单次来源去重/CAS继续使用；同授权组不提前被分拆消耗。
- GEN-008：DTO/Record/JDBC往返字段一致，响应分页与掩码保持形状。
- GEN-009：消费失败先拒绝、不会返回明文；非法值异常保持中文；DB凭据失败纠正环境后仅补失败测试。
- GEN-010：两个电话号码受同一请求内成功许可控制；本批不扩到其它PII；不把布尔查询改为全局有副作用。
- GEN-011：merchant.viewed包含本次对象IDs/入口/项目/筛选/分页；既有单次消费usage日志不绕过。
- GEN-012：稳定分页、500技术上限、long偏移；无每商户行重复消费。
- GEN-013：现有Spring/JDBC/Mockito，零依赖新增；编译和测试执行成功。
- GEN-014：FAIL。发布候选新增V427低于origin最大430，verify:migration-version-order真实exit1；拟换到空闲V432。现complex_test有427/431成功历史，禁止篡改history或删业务；隔离数据库验证新安装及430升级。归本次CX-PUB-01。
- GEN-015：无网页文件；中文校验保留；未声称新增项目页面已可用。
- GEN-016：FAIL。候选包含business_entity_projects新增表，但domain-ownership-scope CSV和data-isolation自动区块仍origin基线；前者predeploy真实exit1。仅重生成/版本及必要引用同步，归CX-PUB-02；旧raw历史V427不改写。
- GEN-017：主体/档案为HTML细化、分页SQL技术性；058冲突已按批准方式修复；未检出新添加项。
- GEN-018：20源全部hash与已完整阅读B5-r5一致，88项本次重新判定；真实发布红与299绿并列，不因单测绿覆盖迁移发布失败。
- PGM-001：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-002：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-003：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-004：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-005：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-006：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-007：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-008：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-009：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-010：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-011：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-012：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-013：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-014：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-015：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-016：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-017：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-018：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-019：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-020：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-021：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-026：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-027：项目/多对多公司/商户/四类绑定/财税变更边界逐项对应service及repository；历史已修当前仍成立，后续合同财税审批/集团UI不在本模块新增。
- PGM-031：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-035：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-040：七入口保留各自原单功能码，电话权限可选而非新强制权。
- PGM-041：L656不改：requirePermissions一起核验并消费同来源一次；重复调用无权或脱敏，失败不执行服务。
- PGM-042：职级与最高权限身份分开；未按职位冒充密钥。
- PGM-043：L680查看例外不赋予导出/打印；20文件没有新增导出打印接口，查看审计保留。
- PGM-044：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-048：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-059：L871例外只商户两个联系号码，普通管理员不自动享有；平台/项目范围保持，两个名字保存加密；旧行不回填限制已批准。
- PGM-060：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-065：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。
- PGM-066：依据已读同锁原文重新核对全文件；全局范围/留痕/中文/独立域要求由现有归属与调用边界承载；该锚点其余票务设备营销薪酬等没有本模块实现，不宣称已实现亦不新增。

## 26既有ID逐项复核（与同字节旧修复对应；不把新发布缺陷藏在旧“否”中）
- M009-038：项目隔离及多对多主体关联有效，master不整类关闭隔离。当前本批队列否，历史修复事实保留。
- M009-039：空间当前项目与父空间、商户一致。当前本批队列否，历史修复事实保留。
- M009-040：普通资料修改不能换非空财税主体；不补审批系统。当前本批队列否，历史修复事实保留。
- M009-041：未来绑定不立即投影，排他行锁及日期有效。当前本批队列否，历史修复事实保留。
- M009-031：非空财税主体不能借停用清空，历史绑定不回写。当前本批队列否，历史修复事实保留。
- M009-042：disabled空间类型走已有恢复入口。当前本批队列否，历史修复事实保留。
- M009-043：对象/理由/决定审计元数据存在。当前本批队列否，历史修复事实保留。
- M009-044：pending申请批准/驳回更新最终状态。当前本批队列否，历史修复事实保留。
- M009-045：真正到期且inactive绑定排除占用，未来inactive不排除。当前本批队列否，历史修复事实保留。
- M009-046：面积、租金、容量非负校验，未发明上限。当前本批队列否，历史修复事实保留。
- M009-030：新建给定主体先校验当前项目存在。当前本批队列否，历史修复事实保留。
- M009-047：用户展示校验语句中文，内部enum保持内部码。当前本批队列否，历史修复事实保留。
- M009-018：普通档案路径不能直接terminated，不新增退租流程。当前本批队列否，历史修复事实保留。
- M009-013：三项目字段已保存返回、可空、后端DB范围；旧数据无自动填充。当前本批队列否，历史修复事实保留。
- M009-054：此前岗位模板入口修正作为既有输入，不跨模块重修。当前本批队列否，历史修复事实保留。
- M009-055：此前商户范围测试前提修正保留，不放宽生产隔离。当前本批队列否，历史修复事实保留。
- M009-056：此前受益人测试资料修正保留，不绕过利益冲突。当前本批队列否，历史修复事实保留。
- M009-048：项目范围商户列表为HTML下位细化。当前本批队列否，历史修复事实保留。
- M009-049：既有审批链仓储为下位实现，本模块不发明角色链。当前本批队列否，历史修复事实保留。
- M009-050：电话加密基础有效，与058单次遗漏和059姓名分别处理。当前本批队列否，历史修复事实保留。
- M009-051：稳定分页及上限500/long偏移技术实现有效。当前本批队列否，历史修复事实保留。
- M009-052：拒绝直接DELETE、走软删除审批为细化。当前本批队列否，历史修复事实保留。
- M009-053：包声明技术性。当前本批队列否，历史修复事实保留。
- M009-057：空间类型注释与实际create行为一致。当前本批队列否，历史修复事实保留。
- M009-058：当前缺陷否、队列否。七入口改用已有requirePermissions，同组单次仅一次消费，独立电话单次第二次脱敏；原查看metadata保留，获批HTML例外有效。
- M009-059：主次联系人姓名新建/修改加密并读取解密；旧行未重存仍不保证加密，不回填。当前本批队列否，历史修复事实保留。

## 实证与范围
origin/main=d8f12c33766c2a0f39a45d5f4b8d66f6753804e4；主HEAD e456ba96，indexSHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331。
只读SQL BEGIN READ ONLY查询complex_test：427 checksum216061286、428 1696678370、429 832923190、430 -1296710179、431 -1214242991全部success=t；432不存在。complex角色有createdb，但本B1未建/删数据库，未修history。
编号门禁属于delivery-gates而非predeploy链；predeploy实际首红是CSV，不沿用Grok发布补票中的链路概括。299用例为上一轮真实clean结果，不重复无效跑。未测新号迁移、新库升级、生产部署；无业务源码修复，无HTML更改。
技能沿用已在本会话实际读取并按V0.17收窄的审计/诊断/验证技能，不新增角色、判据、产物类型。锁四值待收到本次Grok报告后核，不先填对席值。

### 本次B1技术边界补充（同一完整报告）
CSV已有file_version列且现生成器固定V0.01；遵守操作员版本铁律，本次重生成CSV需该列进V0.02，并仅同步生成器该既有常量以及其自身V0.04→V0.05；不变分类规则、列结构或状态规则。此为CX-PUB-02必要版本同步，不新增功能/判据。安全规格自身V0.03→V0.04，自动区块机械生成；历史审计raw不改V427字样。该补充在读取本次Grok之前独立写入。

版本：V0.19（本次B1完整V0.18报告及上述版本同步补充合并为当前本席票；原字节保留。）

版本：V0.20（同一完整B1报告补真实发布反例）
为验证CX-PUB-01，在正本.scratch/m009-publication-r6创建仅运行时Java FlywayReleaseProbe V0.01，读取已装Flyway12.4及Surefireclasspath，不改业务源码。新建隔离库complex_test_m009_pub_upgrade_20260922（事先pg_database确认不存在），以已发布main迁移目录迁至430：397迁移、validate成功、exit0；改用当前正本迁移目录执行migrate：exit1，Flyway明确“Detected resolved migration not applied to database: 427”。未启用outOfOrder或ignore规则。失败发生在迁移前校验，隔离库保持430。原complex_test未改。这比单独编号脚本更直接证明正常已到430安装不能接受未发427。临时运行时夹具位于.scratch，不是新审计产物类别；不假称本B1完全无数据库写入，写入仅新隔离验证库。当前本席结论仍FAIL，两项完整问题集合不变。


## M009 第6次发布修复 B2 Codex整批归并票
版本：V0.21
输入：本文件本次完整B1 V0.18及V0.19/0.20补证，M009-b1-grok-raw.md V0.17，findings V3.55顶部完整30行。两席问题全部归并：GR01/CX01→060；GR02/CX02CSV→061；GR03/CX02规格→062；GR04/CX01边界→063否。26旧ID均否；当前3项队列是。全部技术性，无业务发明，依据GEN014/016等原冻结项。版本变更按用户AGENTS执行，SQL纯重命名保持字节；生成器仅file_version及自身版本常量，不改规则。授权一次B3至累计6，不另增功能/判据。根本席B2 PASS；Grok B2未收前不定稿、不修。
