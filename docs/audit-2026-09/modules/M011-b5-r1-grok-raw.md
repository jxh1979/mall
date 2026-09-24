CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M011 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M011 同一 3 文件；`runbook.md` V0.02；控制提示词 160-audit-prompt.md V0.17
- 模块：M011 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M011 → [BEQR-063 … PGM-066]`，COUNT=87 unique=87 MISSING=[]（与 M009 同集合）；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=bd445728a6ec7bb36a9e7ff19fe56927410ac61e`（派发写 242f4872；ledger 已把 M001–M010 检查点合并后的基准改为 bd445728）。B3-r1 after tree 对象 `dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a` 可解析为 tree；三测试文件当前工作区 blob 与该 after 全等。当前整工作区临时索引 `git add -A && git write-tree`=`42f9d6579056092811027038534a7ed5947fe525`（findings V1.37 / ledger 顶段 / B4 raw 已写入，不等于 B3 after，属台账前进，本席未改源）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m011-b5-r1-verify.TZAvL5`，结束已 `shutil.rmtree` 并删除含三库变量的临时 env）
- 未读取：`M011-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.37 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.37 — 001（fake 软删+断言）、002（夹具白名单值）、003（取消存档/驳回/理由/HTTP 存档）、004（作用域负例）、005（SixValues+幂等断言改名）已修转「否」；006（M009 跨模块观察）、007 仅登记。当前修复队列「是」=0

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–005 在当前三文件上独立复验成立；006/007 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 2933 行 / 437102 字节 / SHA256 `be65910dcd74c3bb0ef02124fb5d100e327e67a4e26375d7ddf63f9abe6afce5` |
| findings | V1.37；3361 行 / 514937 字节 / SHA256 `fbae23dba8cd40647dcf52074f69f5507bb149ecc922738c0cae16bdc787137e` |
| 映射行抽出 | 87 条、无重复、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | BEQR-148 `:231`、BEQR-155 `:238`、BSM-004 `:566/:571`、PGM-018 `:415`、PGM-027 `:507` 均命中；GEN 无 HTML 行 |
| 计划 FILES | 3 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1342（切分时）；当前工作区 `\n` 计数 **1769**（B1 时 1656；B3-r1 +135/−22 = +113，1656+113=1769，不改计划凑数） |
| questions | 本模块 0；目录仅 M003-023/024、M006-027、M010-002/003，无未答 M011 问询 |

映射闭集（锁 6390 行原文抽出，87 unique）：

BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066.

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

### 1.1 3 文件当前行数 / 字节 / SHA256（工作区实算，行=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | git blob | 全文 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java` | 408 | 19405 | `8d45ab35958af17b6b4355ff0d52d02f298648be5d9c35bd2aa7b6722d151480` | `02d9e49d672fbf867cb405b5d2d8576fd2bd2043` | 已读 L1–L408（12 @Test + 1 @TestConfiguration；文件头 V0.02） |
| 2 | `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` | 496 | 23454 | `710ba743d54d7a2fc94d0fbbe07a62ea47840e5b5c4b52d30963bd72a389bcf4` | `62a9b7a9de4be34baeb304439eb87ee79bad478e` | 已读 L1–L496（16 @Test；文件头 V0.04） |
| 3 | `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java` | 865 | 43634 | `3b8968be3fae0629fa38959c32b5c8bdd09e0af40be85de2e5414977fd3d1b3d` | `4f29f79b4ac503503e98f8bc54a9d3b1e78b1ca6` | 已读 L1–L865（34 @Test；含内部 InMemoryPlatformRepository L734–863；文件头 V0.04） |

合计行 1769、字节 86493。三文件均整文件读完，未按标题抽样。`rg "^\s*@Test"` 对 Controller 计 13 是因为 `@TestConfiguration` 前缀命中，Surefire XML 实为 12。

三测试文件 blob 与 B4-r1 所记 after 全等（Controller `02d9e49d` / Jdbc `62a9b7a9` / Service `4f29f79b`）。HEAD blob 仍为 before（B3 未提交）。

必要只读依赖（验证本模块调用/字段，不扩审模块；指纹与 M009 B5-r3 闭环时全等）：

| 路径 | 行 | 字节 | SHA256 | 本轮用途 |
|---|---:|---:|---|---|
| `PlatformService.java` | 876 | 43323 | `720b13d917433231150a707ed5572054566c656d589a1a48dc19a2632eb9aa94` | 软删/存档/驳回/作用域/六值白名单 |
| `PlatformController.java` | 236 | 13580 | `05bd7cf4a2bfcb4398c6859abe6c56ce40041a982cdd308d51557ad00d92b743` | HTTP 权限/幂等/读端作用域；createSpaceType 仍无 `requireIdempotencyKey`（006） |
| `JdbcPlatformRepository.java` | 1112 | 61986 | `10efae64b8dce9836d9eed6b339ddeda53caff78039b8234bfa882bab77139e2` | `UPDATE delete_status='approved'`；`listMerchants(true)` 含 approved |
| `PlatformRepository.java` | 134 | 6645 | `09f4c02728bd7e6176272e8c8f33ac24bd0dcc11549d39747dca8b454598216f` | 默认 `listMerchants(boolean)` 过滤；`deleteMerchant` 默认抛错由 fake/Jdbc 覆写 |
| `MerchantRecord.java` | — | — | （只读 `withDelete`/`withArchive`） | 001 fake 落点 |
| `TestSecurityActors.java` | 67 | 3263 | `c070f75b01b7f36c01768e0a8d50c27bc6e63edfbb38a643c0762ba812b8fe38` | `asMerchantUser` / `asPlatformAdmin` |

另只读：`AuthorizationService.requirePlatformScope`（merchantId 非空 → `PLATFORM_SCOPE_REQUIRED`「商户账号无权查看平台级经营分析」）；`ApiRequests.requireIdempotencyKey`。

### 1.2 活库 `complex_test`（只抽出三个 `COMPLEX_DATABASE_*`，库名改写为 `complex_test`；USER_LEN=7 PASS_LEN=48；不打印密文）

| 项 | 实算 |
|---|---|
| `current_database` / `current_user` | `complex_test` / `complex` |
| Flyway 当前版本（测前、测后） | **410** / **410** |
| JDBC 套件启动日志 | Successfully validated 378 migrations；Current version 410；Schema up to date. No migration necessary |
| `parent_entity_id` 列 | 0 |
| `idx_business_entities_parent` | 0 |
| `chk_be_entity_role` | `platform` / `project_company` / `management_company`；不含 `spv`/`operation` |
| merchants CHECK | `chk_merchant_delete_status`（none/pending/approved）；`chk_merchants_business_format`（六业态）；两列 PII 信封。**无** `cooperation_form` CHECK，**无** `merchants.status` CHECK |
| 旧库 `complex_test_pre_v410_20260909` | 仍存在 |
| BEGIN 插入 `entity_role='operation'` / `'spv'` | 均 `violates check constraint "chk_be_entity_role"`；事后 `leftover=0` |
| 任一 `complex_test*` 模式/数据 | 本席未改（JDBC 类上 `@Transactional` 回滚；探针包在 BEGIN 违例后回滚） |

---

## 2. 全量三线覆盖表

| 文件 | 段 | 第一性原理 | 顺从式 | 对抗式 | 结论 |
|---|---|---|---|---|---|
| PlatformControllerTests.java | L1–L114 鉴权/缺键 | 无登录/无权限/缺幂等键必须可失败 | 与 GEN-005/GEN-007 共享闸一致 | 未登录 isIn(401,403)；缺键 400 VALIDATION_FAILED；无 manage 403 | 覆盖 |
| 同上 | L115–L180 列表形状/PII | 删除后 BEQR-148/GEN-010 无 API 形状与掩码闸 | mock 合作方式已改 `lease`（002） | 无 pii 权限见掩码、有权限见明文 | 覆盖 |
| 同上 | L182–L251 空间权限 + 存档 HTTP | 003 最小：archive 须 manage + 幂等；缺键不进服务 | 对上 Controller:88–94 | 成功 verify `merchant.profile.manage`；缺键 400 且 `archiveMerchant` times=1 | 覆盖 |
| 同上 | L253–L311 读端平台作用域 | 004：商户不能看平台全局 | 七个治理 GET 均 `requirePlatformScope`（绑定+商户+其余五） | 循环五路径均 403 `PLATFORM_SCOPE_REQUIRED`（日志 6 次含商户列表） | 覆盖 |
| 同上 | L313–L382 绑定写权限 | 005 改名后名称=断言（只验 manage 码） | 缺键共享闸见 createMerchant 用例 | 不补第二条缺键（B2-r2 禁止） | 覆盖 |
| 同上 | L385–L408 TestBeans | 切片占位 mock | AuthService 空密钥仅让上下文起来 | 不升缺陷（007） | 登记 |
| PlatformJdbcIntegrationTests.java | L33–L86 表/权限/分页 | 删除后无法证明主数据表与真分页 | 权限种子含治理码 | 第二页 id 不与第一页重叠；业态谓词 bar 空 / other 命中 | 覆盖 |
| 同上 | L88–L205 主体/商户正式字段 | 002：夹具只用六类合作方式与 SUBJECT 状态 | `lease`→`joint_operation`，status=`active` | `fixed_rent_plus_commission`/`revenue_share` 在 saveMerchant 路径 0 命中 | 覆盖 |
| 同上 | L207–L340 空间层级/绑定唯一 | BEQR-143 一商户一 active | `uq_business_entity_bindings_active_merchant` | 第二条 active 抛 DuplicateKey | 覆盖 |
| 同上 | L343–L372 软删可查 / 空间主键 | 001 JDBC 正例；027 不受 LIMIT 500 截断 | includeArchived=true 含 approved；false 不含 | 501 条后最早 id 仍 findSpaceById | 覆盖 |
| 同上 | L388–L438 PII | GEN-010 信封 | 密文前缀 enc:v1:；二次加密不叠 | 明文 INSERT 打 `chk_merchants_contact_phone_pii`；该 INSERT 仍写 status=`draft`（永不提交，见观察 B5-03） | 覆盖（气味登记） |
| 同上 | L440–L476 V410 父链/角色/组织角色 | PGM-018/M010-001–004 | 列/索引 count=0；CHECK 三值；跨组织同名可插入 | 直写 spv 违例；operation 由本席 psql 补证 | 覆盖 |
| PlatformServiceTests.java | L50–L188 名称/合作方式/状态 | 白名单负例必须能红 | 退役码当**非法**（与 002 夹具分工） | `fixed_rent_plus_commission`→`MERCHANT_COOPERATION_FORM_INVALID`；商户 `draft`→VALIDATION_FAILED | 覆盖 |
| 同上 | L202–L269 角色/存在性/下线再用/空间状态 | 005/006/007/028/036 | 拒 spv；缺主体 404；已存档引用 MERCHANT_ARCHIVED；空间 zzz 400 | 正例 unavailable 接受 | 覆盖 |
| 同上 | L271–L427 绑定同步/最高权限/软删/恢复/驳回/商户主体 | 001/003/004/009 | fake `deleteMerchant` 改 `withDelete("approved")`；unarchive 默认列表可见；rejected→none 且仍在 `listMerchants(true)`；deleteReason 走 includeArchived；`asMerchantUser` 写路径 PLATFORM_SCOPE_REQUIRED | 若 fake 再 `remove`，001 断言会红（本席未改、62/62 绿） | 覆盖 |
| 同上 | L429–L732 占用/幂等/审计/字典/碰撞 | BSM-004 占用；GEN-007 幂等 | 引用>0→MERCHANT_IN_USE；重复建主体 saveCount=1 | slash 碰撞 id 不覆盖 | 覆盖 |
| 同上 | L734–L863 内存 fake | 001 适配器必须与 JDBC/HTML 同语义 | 注释已改软删；`setMerchantArchiveReason` 仍空（B2-r2 禁止造 fake） | `listMerchants()` 返回全量，默认 `listMerchants(false)` 滤 archived/approved | 覆盖（空方法登记 007） |

三线均执行。无 finding 不等于未审。

---

## 3. 数据流（测试 → M009 后台 → SQL → HTML）

```
HTTP  → PlatformControllerTests（WebMvcTest，PlatformService Mockito；不进服务实现、不进库）
服务  → PlatformServiceTests（内存 InMemoryPlatformRepository；不进 JDBC）
仓库  → PlatformJdbcIntegrationTests（JdbcPlatformRepository + complex_test；不进 PlatformService）
```

生产路径是 Controller → Service → Jdbc。本模块仍无「审批删除」全链联立用例（OBS，001 已把两适配器语义对齐，不再锁相反口径）。

`JdbcPlatformRepository.deleteMerchant`：`UPDATE merchants SET delete_status = 'approved'`。`listMerchants(true)` WHERE 为 `(:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))`，故 includeArchived=true **含**已审批删除行。内存 fake 现 `withDelete("approved", …)` 留行，与接口默认过滤及 BSM-004「只隐藏不抹除、可查可恢复」同口径。

`PlatformService.unarchiveMerchant`：`withArchive(null,…) + withDelete("none",…)`，取消存档同时清 pending/approved。`approveDeleteMerchant(decision=rejected)`：`withDelete("none", null, null, null, now)`，行保留。`archiveMerchant` 经 `requireText(reason)` 后 `setMerchantArchiveReason`（Jdbc 写 `archive_reason` 列；MerchantRecord 无该字段，B2-r2 禁止造 fake）。

读端平台作用域：Controller 七个治理 GET 均 `requirePermission` 后 `requirePlatformScope`。写端服务方法（create/update/archive/unarchive/delete-request/approve/createProject/createSpace/createSpaceType/createBinding/changeBinding）首行 `requirePlatformScope`。`listMerchants` **服务方法**本身无 `requirePlatformScope`（读端守卫在控制器；属 M009 已闭环分层，本模块 004 已锁 HTTP GET + 服务写路径）。

---

## 4. 三线审计（逐文件）

### 4.1 PlatformControllerTests.java（L1–L408，12 @Test）

**第一性原理**

1. 为什么必须存在：删除后 BSM-072/PGM-027 的读端平台作用域、GEN-005 权限码、GEN-010 联系电话掩码、BSM-004 存档 HTTP、四类主体绑定 HTTP 入口将无控制器层可失败断言。有业务上位。
2. 声称的行为是否实际执行：Surefire 12/12 绿。`listingMerchantsRequiresPlatformScope` 与 `otherGovernanceReadEndpointsRequirePlatformScope` 打到 `PLATFORM_SCOPE_REQUIRED`（日志「商户账号无权查看平台级经营分析」共 6 次＝1 条商户列表 + 循环 5 路径）。`archivingMerchantRequiresManagePermissionAndIdempotencyKey`：成功路径 verify `merchant.profile.manage` 且 JSON `archived_at` 非空；缺键 400 `VALIDATION_FAILED` 且 `archiveMerchant` 仍 times=1（不进第二次）。`creatingMerchantWithoutIdempotencyKey` 400。PII 两条分别断言掩码与明文。合作方式 JSON 现为 `lease`。
3. 未验证前提：写路径 `requirePlatformScope` 在 Controller 上多数不调用、依赖被 mock 掉的 Service——服务层 004 负例补这一层。unarchive / delete-request / delete-approve 三条 HTTP 无控制器测试（B2-r2：不为四条 HTTP 各写一条）。`createSpaceType` 控制器无 `requireIdempotencyKey`（006，M009 源码）。create 成功路径电话掩码仍无。

**顺从式**：假定意图是「控制器鉴权 + 响应形状 + 存档 HTTP 最小面」。与 HTML 读端「商户不能看平台全局」对齐（商户列表 + 绑定 + 其余五 GET）。存档 HTTP 权限/幂等已锁。合作方式 mock 值在六类之内。绑定用例名称不再声称未测的缺键。

**对抗式**：伪造商户作用域读七个治理 GET → 有测试。伪造无权限建商户 → 有测试。伪造缺幂等键（建商户 + 存档）→ 有测试。伪造无 PII 权限读电话 → 有测试。伪造未登录 → 有测试。缺：商户作用域打写 HTTP（服务层已打 create/archive）；缺：unarchive HTTP。

**ponytail-review**：`L70-77: shrink: isIn(401,403) 可收窄为当前 SecurityConfig 的确定码。保留亦可。` `L385-406: yagni: 空密钥 AuthService 切片占位，不升缺陷。`

### 4.2 PlatformJdbcIntegrationTests.java（L1–L496，16 @Test）

**第一性原理**

1. 为什么必须存在：删除后无法在目标库证明表/权限种子、分页、软删行保留、PII 信封、V410 父链列删除、角色 CHECK、组织隔离唯一约束。有业务上位。
2. 声称的行为是否实际执行：16/16 全绿。Flyway 410=410。`approvedDeletedMerchantStaysQueryableWithIncludeArchived` 断言 `findMerchantById` 仍在、`listMerchants(true)` 含、`listMerchants(false)` 不含，与 JDBC 软删及 BSM-004 一致，**可失败**。`createUpdateAndListMerchantPersistsFormalFields` 现 `lease`/`joint_operation`/`active`。`parentEntityColumnIsRemovedFromBusinessEntities` 列/索引 count=0。`entityRoleCheckRejectsSpvAndOperation` 约束定义不含 spv/operation，并 INSERT spv 期望违例。PII 三条打信封、二次加密、明文 CHECK。
3. 未验证前提：`plaintextPhoneWriteIsRejectedByDatabaseConstraint` 的失败 INSERT 仍写 status=`draft`（库无 status CHECK；该语句预期在 PII CHECK 处失败、永不提交，不把 draft 锁成可持久化商户状态）。`entityRoleCheckRejectsSpvAndOperation` 因同一事务只能一条违例 INSERT，未 INSERT `operation`（本席 psql 补证 operation 亦拒）。不经 PlatformService.unarchive / setMerchantArchiveReason（003 JDBC 面不在 B3 最小边界）。

**顺从式**：仓库层与 V410/PII/软删 SQL 一致。与服务层枚举白名单：saveMerchant 夹具已对齐；PII 负例 SQL 残留 draft 字面量（观察）。权限种子断言覆盖治理码与 platform_admin 子集。

**对抗式**：直写明文电话 → CHECK 拒。直写 spv → CHECK 拒。同组织同名角色 → DuplicateKey。跨组织同名 → 可插入。父链列 → 不存在。软删后再默认列表 → 不可见。未测：Jdbc `setMerchantArchiveReason` 列写入；未测：cooperation_form 非法值被库接受（事实：无 CHECK，属迁移模块）。

**ponytail-review**：`L22: yagni: password 默认 complex_dev_pwd。测试文件约定，不升缺陷。` `L430: shrink: 失败 INSERT 的 status 可改 active，避免与 002 词表并置。行为不改。`

### 4.3 PlatformServiceTests.java（L1–L865，34 @Test，含内部 fake）

**第一性原理**

1. 为什么必须存在：删除后服务层六类合作方式、商户/项目状态、三角色、绑定存在性、存档隐藏/恢复、占用拒删、最高权限审批、驳回、商户主体负例、幂等、空间类型硬校验将无单测。有业务上位。
2. 声称的行为是否实际执行：34/34 全绿。`approveDeleteSoftDeletesArchivedMerchantWhenNoReferences`：`listMerchants(false)` noneMatch、`listMerchants(true)` anyMatch `deleteStatus=approved`——与 JDBC 及 BSM-004 同口径；若 fake 再物理 `remove`，**本断言会红**。`unarchiveMerchantRestoresDefaultListVisibility`：存档后默认列表不可见，取消后可见且 `archivedAt==null` 且 `deleteStatus=none`。`rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus`：pending 时 `listMerchants(true)` 含且 `deleteReason` 为申请原文；rejected 后 `deleted()==false`、`deleteStatus=none`、仍在已存档列表。`merchantActorIsRejectedByPlatformScopeOnGovernanceWrite`：`asMerchantUser` 调 `createMerchant`/`archiveMerchant` 期望 `PLATFORM_SCOPE_REQUIRED`。`createMerchantRejectsBusinessFormatOutsideSixValues` 方法名/注释已改六值，`tenant` 仍能失败。
3. 未验证前提：`setMerchantArchiveReason` 仍空实现（B2-r2：不为 archive_reason 造 fake、不给 MerchantRecord 加字段）。`@BeforeEach` 仍默认 `asPlatformAdmin()`，但 004 已有独立负例切到 `asMerchantUser`。无「审批删除后再 unarchive」用例（003 最小是存档后取消，不是删除后恢复；生产 `unarchiveMerchant` 会 `withDelete("none")`，观察）。`createBusinessEntityRejectsRoleOutsideWhitelist` 只拒 spv。`requestDeleteByMasterAuthorityExecutesWithoutApproval` 只断言默认列表不含，不断言 includeArchived 仍含——001 专条已锁后者。

**顺从式**：服务白名单与 HTML 六类合作方式、BEQR-154/155、PGM-018 三角色、BSM-004 存档隐藏/占用/最高权限/取消存档/驳回/软删可查 对齐。删除「行仍可查」不再被 fake 推翻。方法名 SixValues 与六业态 CHECK 一致。

**对抗式**：非法合作方式 → 拒。非法商户状态 → 拒。缺主体绑定 → 404。已存档再被引用 → MERCHANT_ARCHIVED。非最高权限审批 → 禁。占用 → MERCHANT_IN_USE。幂等重复建主体 → saveCount=1。商户账号写治理 → PLATFORM_SCOPE_REQUIRED。驳回后仍可在已存档列表查到。缺：自审闸（最高权限申请直接软删，首轮不走到，007）；缺：绑定并发双插（顺序路径已覆盖）。

**grilling 追问树（内部自答，未问操作员）**

- Q1 删除是否只隐藏不抹除？HTML 是。JDBC 是。内存 fake 现是。服务测试把「留行+approved」锁成期望。→ 001 已修
- Q2 首轮正常会点「取消存档」吗？会。现有可失败断言。→ 003 已修
- Q3 夹具能否写下服务层拒绝的合作方式/状态作为「正式字段」？saveMerchant 路径否；PII 失败 INSERT 的 draft 永不提交。→ 002 已修；B5-03 气味
- Q4 商户账号打治理写/读会不会在本模块红？写：服务负例会红。读：七个 GET 控制器负例会红。→ 004 已修
- Q5 自审闸 `MERCHANT_SELF_APPROVE_FORBIDDEN` 首轮会撞上吗？最高权限申请会直接软删，不进 pending。铁律 6 不升缺陷。→ 007
- Q6 方法名还说五值或 Idempotency 未断言吗？SixValues；绑定方法名已去掉 Idempotency 字样，缺键共享闸仍在。→ 005 已修
- Q7 有无锁外新判据或 HTML 原文错误？无。不申请改 HTML。

**ponytail-review**：`L735: yagni: setMerchantArchiveReason 空方法是接口义务，B2-r2 禁止实现。保留。` `net: 以已落地的软删 fake 为准，不单删测试。` Lean already 于 001–005 修复面。

**codebase-design**：内部 fake 仍是 PlatformRepository 缝上的第二适配器。两个适配器现在都软删+includeArchived 可见。深度落在接口契约，而不是 `Map.remove`。

---

## 5. 001–005 独立复验（已定稿「否」，B5 必须重证，不继承 B4）

| ID | 定稿最小处置 | 当前证据 | 复验 |
|---|---|---|---|
| M011-001 | fake `deleteMerchant` 留行 `approved`；`listMerchants(true)` 含且 approved、`(false)` 不含；注释改软删 | ServiceTests:362–380 断言；:771–777 `withDelete("approved",…)`；:756 注释软删；JDBC:343–356 同口径；62 套件绿 | **成立已修** |
| M011-002 | JDBC/Controller 夹具改 `lease`/`joint_operation`/`active`；服务负例保留退役码 | JDBC:67–72/:143/:165/:347/:394/:416 `lease`/`active`；Controller:135/:159/:171 `lease`；Service:165 仍把 `fixed_rent_plus_commission` 当非法 | **成立已修** |
| M011-003 | unarchive 默认列表可见；rejected→none 且仍在 `listMerchants(true)`；deleteReason 走 includeArchived；控制器一条 archive 权限/幂等 | ServiceTests:384–413；Controller:217–251；不造 archive_reason fake | **成立已修** |
| M011-004 | 服务 `asMerchantUser` 负例；控制器循环其余五 GET | ServiceTests:418–426；Controller:299–311；日志 6 次 PLATFORM_SCOPE_REQUIRED | **成立已修** |
| M011-005 | FiveValues→SixValues；绑定方法名去掉 Idempotency；不补缺键请求 | ServiceTests:321–323 `…OutsideSixValues`；Controller:314–315 / :351 方法名无 Idempotency；缺键仍只 `creatingMerchantWithoutIdempotencyKey` | **成立已修** |

006/007 本席不重开、不改 M009 源码。

---

## 6. 87 冻结判据核对

说明：本模块是**测试**。判据「覆盖」= 测试实际执行该锚点目标行为且断言能失败并与 HTML 一致。「部分」= 有相关可失败断言但锚点另有无实现面的子句。「无实现面」= 三文件不承载该锚点业务，未发明相反规则。无实现面不升冲突。B1 时标注 FAIL/缺口的条目以下表当前证据为准。

| ID | 标题 | 结论 | 证据 |
|---|---|---|---|
| BEQR-063 | 高危动作（酒吧） | 无实现面 | 三文件无沽清/免单/反结 |
| BEQR-074 | 隐私桌台权限 | 无实现面 | 无桌台授权 |
| BEQR-143 | 四类主体 | 覆盖 | ServiceTests 一商户一 active；JDBC `uq_…_active_merchant`；三主体经 seedEntity 存在性校验 |
| BEQR-144 | 经营主体关系 | 覆盖 | `parentEntityColumnIsRemoved`；psql parent_col=0。符合「不是行政隶属链」 |
| BEQR-145 | 城市项目公司 | 无实现面（角色码有间接） | 无「资产公司兼任」语义测试；三角色白名单含 project_company |
| BEQR-146 | 运营公司职责 | 无实现面 | 无域职责收入测试 |
| BEQR-147 | 主要收入归属 | 无实现面 | 无票款归属 |
| BEQR-148 | 合作模式 | **覆盖** | 服务拒退役码为合法负例；JDBC/Controller 正例 `lease`/`joint_operation`。六类正例 `lease`+空。HTML `:231` 六类命中 |
| BEQR-149 | 职级 | 无实现面 | 无 M1–M10 |
| BEQR-150 | 终端十一端 | 无实现面 | 无终端开关 |
| BEQR-151 | 最终权限计算 | 无实现面 | 无拒绝优先公式 |
| BEQR-152 | 项目与敏感数据权限 | 部分 | PII 电话掩码/明文/信封有测；内部结算价等无 |
| BEQR-153 | 终端开关优先级 | 无实现面 | — |
| BEQR-154 | 综合体项目状态 | 覆盖 | `createProjectRejectsUnknownStatusOrMerchant` 拒 `zzz`；`draft` 作为项目状态出现在商户缺失用例（项目草稿合法） |
| BEQR-155 | 商户/运营主体状态 | **覆盖（可设态）**；退出缓冲期无实现面 | 服务拒商户 `draft`；JDBC saveMerchant 写 `active`。归档走 archived_at 轴有测。缓冲期/停票不在本三文件 |
| BEQR-156 | 合同状态 | 无实现面 | JDBC 只断言 contract 权限码种子 |
| BEQR-157 | 员工状态 | 无实现面 | — |
| BEQR-158 | 设备状态 | 无实现面 | — |
| BEQR-159 | 权限状态 | 无实现面 | — |
| BEQR-166 | 合同审批链 | 无实现面 | — |
| BEQR-189 | 管理费方式 | 无实现面 | — |
| BSM-001 | 整体说明 | 无直接断言 | 测试未发明相反原则 |
| BSM-003 | 系统通用能力 | 指向 004/007 | 见下 |
| BSM-004 | 存档/删除审批软删 | **覆盖（本模块测试面）** | 存档隐藏、取消存档可见、占用拒删、最高权限、驳回留行、软删 includeArchived 可查、deleteReason、存档 HTTP 权限/幂等均有可失败断言。集中清理工具/交易类只存档/内部工单无实现面。HTML `:566/:571` 命中 |
| BSM-005 | 分步引导新建 | 无实现面 | 后台 UI 属 M012 |
| BSM-006 | 动态二维码 | 无实现面 | — |
| BSM-007 | 平台管理员 vs 最高权限 | 覆盖部分 | 非最高权限不能批删；最高权限申请直接执行。人数上限无 |
| BSM-009 | 基础管理入口 | 无实现面 | 卡片文案 |
| BSM-028 | 基础管理主要功能 | 部分 | 商户/空间/绑定/合作方式有测；合同八类变更无 |
| BSM-029 | 和谁有关 | 无实现面 | 连接叙述 |
| BSM-072 | 商户在授权范围内 | **覆盖** | 控制器七个治理 GET 作用域负例 + 服务写路径 `asMerchantUser`。业态六值另有 SixValues 用例 |
| BSM-074 | 和谁有关（商户） | 同 072 | 「不能看平台全局」已扩到其余五 GET |
| BSM-114 | 各类入口 | 无实现面 | — |
| BSM-115 | 相互关系 | 无实现面 | — |
| BSM-120 | 已确认规则 | 无实现面 | 未发明相反拍板 |
| GEN-001 | 必要性与最小性 | 覆盖 | 三文件均有可验证用途；fake 软删不再是错误适配器 |
| GEN-002 | 可执行正确性 | 覆盖 | 62/62 可执行；服务删除路径与 JDBC/HTML 声称一致 |
| GEN-003 | 测试有效性 | **覆盖（关键分支）** | 001 断言指向目标行为（正确实现绿、错误 remove 会红）。恢复/驳回/作用域/存档 HTTP 已补。62 绿本身不替代本表 |
| GEN-004 | 信任边界 | 部分 | 服务校验名称/枚举/存在性；JDBC 夹具绕过服务（仓库层职责） |
| GEN-005 | 授权与最小权限 | 覆盖 | 控制器权限码 + 读端作用域；写端平台作用域在服务单测失败 |
| GEN-006 | 数据完整性 | 部分 | 绑定唯一、占用检查、软删标记；无全链事务联立 |
| GEN-007 | 并发与幂等 | 部分 | 主体幂等有；存档/建商户缺键有；绑定并发无（顺序有）。createSpaceType 控制器缺键未测（006） |
| GEN-008 | 接口与模式一致性 | **覆盖（原冲突点）** | fake vs JDBC 删除语义已对齐；saveMerchant 夹具枚举 vs 服务白名单已对齐；SixValues vs 六业态已对齐 |
| GEN-009 | 错误处理 | 部分 | 非法枚举 400；缺商户 404；CHECK 违例 JDBC 有 |
| GEN-010 | 安全与敏感信息 | 部分 | 电话掩码/信封/明文 CHECK 有；create 成功路径掩码无控制器测试 |
| GEN-011 | 可观测与审计 | 部分 | 服务测 audit 事件名；deleteReason 已断言落到 `listMerchants(true)`；archive_reason 列无 JDBC 断言（007/B2-r2） |
| GEN-012 | 资源边界 | 部分 | 分页 size 上限 500；空间 LIMIT 500 vs findById 有测 |
| GEN-013 | 构建配置 | 部分 | JDBC 硬编码 127.0.0.1/complex_test/complex；密码走环境变量 |
| GEN-014 | 迁移正确性 | 覆盖 | 表存在、Flyway latest=410、V410 列/CHECK/角色唯一。本模块不改历史迁移 |
| GEN-015 | 界面可用性 | 无实现面 | 无 UI |
| GEN-016 | 文档可验证性 | 覆盖 | 「拒硬删」注释已改软删；SixValues 方法名与六业态相符 |
| GEN-017 | 业务来源分类 | 本席已依序分类 | 见 §7；无把发明标成技术性；无添加项 |
| GEN-018 | 第一性原理证据 | 本报告 | 结论落到 HTML/可执行产物/命令，不以 62 绿或席位意见为依据 |
| PGM-001 | 治理层定位 | 无实现面 | 叙述 |
| PGM-002 | 治理顺序 | 无实现面 | — |
| PGM-003 | 模块入口 | 无实现面 | 卡片 |
| PGM-004 | 综合体和集团 | 部分 | 父链删除测试支撑「非隶属」 |
| PGM-005 | 空间和点位 | 部分 | 空间层级/类型/状态有测 |
| PGM-006 | 商户和主体 | 部分 | 档案/绑定/合作方式/存档删除有测 |
| PGM-007 | 合同和准入 | 无实现面 | 仅权限种子 |
| PGM-008 | 员工和手机 | 无实现面 | — |
| PGM-009 | 权限和职级 | 部分 | 权限码种子；非职级规则 |
| PGM-010 | 终端开关 | 无实现面 | — |
| PGM-011 | 设备管理 | 无实现面 | — |
| PGM-012 | 商户侧 | 同 BSM-072 | — |
| PGM-013 | 开放接口 | 无实现面 | — |
| PGM-014 | 留痕复核 | 部分 | audit 事件；删除理由已落到记录断言 |
| PGM-015 | 楼宇运营 | 无实现面 | — |
| PGM-016 | 应急与隐私 | 部分 | 电话 PII；U 盘密钥最高权限有测 |
| PGM-017 | 当前一个项目 | 无实现面 | 无集团视图测试 |
| PGM-018 | 多对多非隶属 | 覆盖 | 父链列/索引删除；三角色 CHECK；服务拒 spv |
| PGM-019 | 各主体职责 | 部分 | 角色白名单；职责表无收入测试 |
| PGM-020 | 主要功能（项目） | 部分 | 建项目有测 |
| PGM-021 | 业务意义 | 无实现面 | — |
| PGM-026 | 商户主要功能 | 部分 | 绑定同步三列；合作方式夹具已对齐 |
| PGM-027 | 业务边界 | **覆盖** | 同 BSM-072/074 |
| PGM-031 | 经销商资金 | 无实现面 | — |
| PGM-035 | 提成发放 | 无实现面 | — |
| PGM-040 | 单一功能授权 | 部分 | 权限码；非模板装配 |
| PGM-041 | 权限规则 | 部分 | 作用域/权限码 |
| PGM-042 | 职级规则 | 无实现面 | — |
| PGM-043 | 外包与审计人员 | 无实现面 | — |
| PGM-044 | 桌台挂单经销权限 | 无实现面 | — |
| PGM-048 | 商户侧全开放 | 同 072 | — |
| PGM-060 | 会员生物信息 | 部分 | 仅电话 PII，非会员注册 |
| PGM-065 | 连接关系 | 无实现面 | 叙述 |
| PGM-066 | 已拍板规则 | 无实现面 | 未发明相反拍板 |

无实现面的判据：三文件未引入与 HTML 相反的业务规则，不因 HTML 沉默判冲突或删除。

---

## 7. 本次 findings / 观察（临时编号 GR-M011-B5-xx）

无新的工程缺陷成立项。无添加项。无 HTML 修改申请。下列为观察（缺陷否），与已定稿 ID 同根因者标明。

### GR-M011-B5-01（同根因 M011-006）

- 文件:行号：`PlatformController.java:156-160` `createSpaceType` 无 `requireIdempotencyKey`（其余写入口有）；本模块无对应测试
- 冻结判据：GEN-007
- 2.1 分类：技术性（跨模块观察）
- 工程缺陷：否（属 M009 源码，已登记 006；本模块不修）
- 本席不入队

### GR-M011-B5-02（同根因 M011-007 / B2-r2 对 003 的收窄）

- 文件:行号：`PlatformServiceTests.java:735` `setMerchantArchiveReason` 空实现；Jdbc `setMerchantArchiveReason` 写 `archive_reason` 列，本模块无 JDBC 断言
- 冻结判据：GEN-011、BSM-004「存档及删除都需要填写理由」
- 事实：存档路径服务层 `requireText(reason)` 仍执行（空理由会 400）；理由落到 MerchantRecord 无字段（B2-r2 禁止加字段/造 fake）。deleteReason 已由 003 经 `listMerchants(true)` 锁住
- 工程缺陷：否（按定稿边界）
- 本席不入队

### GR-M011-B5-03（002 夹具气味残留，不构成新冲突）

- 文件:行号：`PlatformJdbcIntegrationTests.java:428-431` 明文电话负例 INSERT 仍写 `status='draft'`
- 事实：该语句预期被 `chk_merchants_contact_phone_pii` 拒绝，**永不提交**；saveMerchant 正例已全部 `active`。不断言 draft 为合法商户状态
- 冻结判据：BEQR-155、GEN-008
- 2.1：技术性气味
- 工程缺陷：否
- 本席不入队（若随批改字面量为 `active` 更干净，不单独占用修复配额）

### GR-M011-B5-04（003 最小边界残留）

- 事实：无「`delete_status=approved` 后再 `unarchiveMerchant`」用例。生产 `unarchiveMerchant` 会 `withDelete("none")`，003 最小是存档后取消
- 冻结判据：BSM-004「可恢复」
- 工程缺陷：否（铁律 6 / B2-r2 最小已覆盖取消存档与驳回）
- 本席不入队

### GR-M011-B5-05（M009 已闭环分层）

- 事实：`PlatformService.listMerchants` 方法体无 `requirePlatformScope`；HTTP GET 在 Controller 调用。004 已锁七个 GET + 服务写路径
- 工程缺陷：否
- 本席不入队

### GR-M011-B5-06（环境，非本三文件）

- 派发写正本 HEAD=`242f4872…`；本席 `git rev-parse HEAD origin/main` 均为 `bd445728…`（M001–M010 检查点 PR #428）。三测试文件指纹与 B3 after 全等，不影响本模块结论
- 工程缺陷：否

### 其余登记（承接 007，本席独立复验仍成立）

- 自审闸 `MERCHANT_SELF_APPROVE_FORBIDDEN` 首轮不走到（最高权限申请直接软删）
- 无并发双插 active 绑定测试（顺序路径服务+库约束已覆盖首轮双点）
- JDBC `@SpringBootTest` 硬编码 127.0.0.1/complex_test/complex，密码走环境变量
- `entityRoleCheckRejectsSpvAndOperation` 只 INSERT spv；约束定义 + 本席 psql operation 亦拒
- 绑定夹具 id `be_operation_*` 是主键字符串不是角色码
- 无 PlatformService+Jdbc 联立用例（001 已对齐两适配器语义）
- `createBusinessEntityRejectsRoleOutsideWhitelist` 只拒 spv
- TestBeans `AuthService("", "")` 空密钥仅切片占位
- 62/62 BUILD SUCCESS **不能**单独当作 GEN-003 通过；本席 PASS 依据是断言指向目标行为 + 87 逐项核对

添加项：无。须完整列出供 2.2 的添加项：空。

---

## 8. 命令与真实输出 / 退出码

敏感值未输出。三个 `COMPLEX_DATABASE_*` 仅从 `~/.complex/complex.env` 抽出，URL 库名改写为 `complex_test`，不 `source` 整个 env。USER_LEN=7 PASS_LEN=48。

```
# 锁（独立实算，三工具同值；独占锁行见本文第一行）
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# exit 0
# → <64 hex>  docs/audit-2026-09/plan/criteria-lock.md
# python3 hashlib 同值；openssl dgst -sha256 同值；889020 字节

# HEAD
git rev-parse HEAD origin/main
# exit 0
# → bd445728a6ec7bb36a9e7ff19fe56927410ac61e
# → bd445728a6ec7bb36a9e7ff19fe56927410ac61e

# after tree 对象
git cat-file -t dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a
# exit 0 → tree

# 当前整树（含台账/B4 raw，不等于 B3 after）
GIT_INDEX_FILE=$(mktemp -d)/index
git read-tree HEAD && git add -A && git write-tree
# → 42f9d6579056092811027038534a7ed5947fe525
# 临时索引已弃，未替换主索引

# 映射
# M011 COUNT=87 unique=87 MISSING=[]

# 活库只读
# current_database=complex_test; current_user=complex
# flyway_version=410（测前）
# parent_col=0; idx_parent=0
# chk_be_entity_role = platform|project_company|management_company
# merchants CHECK = delete_status / business_format / 两列 PII；无 cooperation_form、无 merchants.status
# old_db_kept=t
# INSERT operation → ERROR chk_be_entity_role；INSERT spv → 同；leftover=0

# 隔离验证（主仓 cwd 未跑写模式 mvn）
# 首次 JAVA_HOME=ubuntu-openjdk（无 javac）→ 编译失败 exit 1：
#   "Fatal error compiling: error: release version 21 not supported"
# 该失败是工具链，不是业务 finding，不折算 PASS/FAIL。
# 改 JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 后：
cd /tmp/m011-b5-r1-verify.TZAvL5/backend
mvn -Dtest=com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformJdbcIntegrationTests test
# MVN_EXIT=0
# Tests run: 16, Failures: 0, Errors: 0, Skipped: 0  PlatformJdbcIntegrationTests
# Tests run: 34, Failures: 0, Errors: 0, Skipped: 0  PlatformServiceTests
# Tests run: 12, Failures: 0, Errors: 0, Skipped: 0  PlatformControllerTests
# Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
# Flyway：validated 378 migrations；Current version 410；No migration necessary
# 日志：PLATFORM_SCOPE_REQUIRED ×6；idempotency_key is required ×2；PERMISSION_DENIED ×1
# 测后 flyway_after=410；leftover=0
# 副本与含密 env 已删除。mvn 日志 /tmp/m011-b5-r1-mvn.log SHA256 82e9c73ee67589be6ad4034b0b05bbf2e60d4a2f674d0b31d16295b9e5cad226（无 password 字面量）
```

主仓 `git status` 在本席开工时已有三测试文件 + findings/ledger/B1 raw 的既有脏区；本席未改那些路径，只新建本 raw。

子代理失败重跑次数：0（未派子代理）。Grok 交付失败重跑：0。

---

## 9. 技能实用 / 缺口

本席实际读取并按 V0.17 §6 / B5 收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| verification-before-completion | superpowers `verification-before-completion/SKILL.md` | 先跑命令再声称；62 绿≠行为对；编译失败如实记 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 隔离 mvn 13s 内结束，未起 monitor |
| diagnosing-bugs / systematic-debugging | 口径 | 编译失败先辨工具链（JAVA_HOME 无 javac）再重跑，不把工具红写成业务 finding |
| codebase-design | 口径 | fake 与 JDBC 两适配器深度应落在软删契约 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未作为独立 SKILL.md 核到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP。

---

## 10. 本席 B5 结论

单席 PASS 条件核对：

| 条件 | 本席 |
|---|---|
| 本模块所有文件有覆盖记录 | 3/3 全文 + 三线表 |
| 映射的所有判据 ID 已核对 | 87/87 |
| 三线均执行 | 是 |
| 所有声称均有四类证据之一 | 是（1+2+4；无类型 3 需要） |
| 无未答 questions | 是 |
| 无未定稿缺陷 | 是（无新成立项；无添加项） |
| 无 HTML 修改申请 | 是 |
| 哈希行有效 | 第一行独占；三工具同值；正文无第二处同名独占行 |

**本席 B5-r1：PASS。** 无有条件通过。本票不等于双席合取或模块 PASS。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M011 当前锁 B5-r1 整模块全量三线审计（Grok 席乙，同名追加）

- 文件版本：V0.02（指定路径已有 V0.01 历史报告；本次完整报告追加到末尾，不覆盖、不另编号）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本次 `M011-b5-r1-self-raw` 新增正文）
- 步骤：B5-r1（历史 B3 累计 1；本次无新 B3/B4；B1/B2 两席已 PASS、findings V3.54 顶部 7ID 全否 + 12 观察全登记；仍须整模块重审，不得因队列空直接收口）
- 控制提示词：`160-audit-prompt.md` V0.17；`runbook.md` V0.02
- 模块：M011 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段
- 范围：`plan/modules.md` 三文件，实算 `git ls-files -z -- <3 paths> | xargs -0 cat | wc -l` = **2776**
- 映射：`criteria-lock.md` M011 行 **87** 条（BEQR 21 + BSM 14 + GEN 18 + PGM 34）；本席独立点清 87/87、无重复
- 锁哈希：本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 sidecar / ledger 顶部同值；历史 V0.01 报告第一行是旧锁 `b012328f…`，不作为本次④）
- 禁止项已遵守：不改 ledger/findings/锁/源码/测试/SQL/HTML/.git；不建删库、不 DDL、不写业务数据；不 fetch/提交/推送；不 Maven、不改 target；不沿用 B1/B2 或历史 B5 票作结论；边审边修禁止
- 测试执行声明：本席**未新跑** Maven。Codex B1 声称 113=19/24/70 全通过。本席只读 Surefire XML/TXT 独立核对，不得把未跑说成新跑。

## 0. 技能自核（V0.17 §6 收窄，B5 节点）

开工前核对本席运行环境技能目录（不是 Codex Windows 路径）：

| 技能 | 存在性 | 本席用法 |
|---|---|---|
| `using-superpowers` | 有 | 开工技能路由 |
| `grilling` / `grill-me` | 有 | 对抗式追问结构；**不等操作员、不派子代理**，事实自读 |
| `codebase-design` | 有 | 测缝/接口/fake 深度：控制器 HTTP 缝、服务内存缝、JDBC 真库缝 |
| `diagnosing-bugs` | 有 | 只借读错误→追数据流；**不插桩、不改码** |
| `verification-before-completion` | 有 | PASS 前只引用本席实跑命令或只读产物；Maven 未跑不宣称「本席新跑通过」 |
| `ponytail-review` | 有 | 找可删；B5 只列不修 |
| `i-have-adhd` | 有 | 输出形态：先结论、编号、无寒暄 |
| `writing-for-agents` | 有 | B5 本席不写收口文件 |
| `receiving-code-review` | 有 | B5 本席是外审，不收 Codex 票 |
| `requesting-code-review` | 有 | 仅 B4，本步不用 |
| `tdd` / `test-driven-development` / `ponytail` | 有 | 仅 B3，本步不用 |
| `systematic-debugging` | 有 | 无新缺陷根因需定位 |
| `resolving-merge-conflicts` | 有 | 无冲突 |
| `git-guardrails-claude-code` | 有（可选） | 只读、无 git 写 |
| `batch-grill-me` | **缺口** | 目录无此技能；不冒充已调用 |
| `ubiquitous-language` | **缺口** | 同上 |
| `research` / `dispatching-parallel-agents` / `subagent-driven-development` / `using-git-worktrees` / `ponytail-audit` 等 | 按 §6 划掉 | 不用 |

Arc `audit` 全仓机械检查 + 子代理评审：**不用**（与双席、禁止子代理、禁止 Maven 冲突）。graft 仅只读骨架定位三测试文件。

## 1. 范围与源字节（类型 4）

命令：

```
git ls-files -z -- \
  backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java \
  backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java \
  backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java \
  | xargs -0 cat | wc -l
```

输出：`2776`。逐文件 `wc -l`：Controller 532 + Jdbc 760 + Service 1484 = 2776。

本席独立 SHA256（源文件，非 Maven）：

| 文件 | 行 | SHA256 |
|---|---|---|
| `PlatformControllerTests.java` V0.04 | 532 | `21e657c73b07a6e974e55f90156c2bbdd1338838729c097dcc67b373b7d57a8e` |
| `PlatformJdbcIntegrationTests.java` V0.09 | 760 | `b42d8a292f7bcf3718703d996a4fe5d275069cbefe2472eba96f504c73ed48da` |
| `PlatformServiceTests.java` V0.17 | 1484 | `c942935719f044f997b614783a3aacec8848172f9bf1d6a89c1a8e63248f1e16` |

`questions/` 无 `M011*` 文件。无添加项待答。无三份 HTML 修改申请。

## 2. Surefire 只读核对（类型 4；非本席新跑）

路径：`backend/target/surefire-reports/`。同一 JVM：`surefirebooter-20260922111022639_3.jar`，`sun.java.command` 含 `2026-09-22T11-10-22_603-jvmRun1`，`-Dtest=PlatformControllerTests,PlatformJdbcIntegrationTests,PlatformServiceTests`。

| 产物 | tests | failures | errors | skipped | time |
|---|---|---|---|---|---|
| `TEST-…PlatformControllerTests.xml` + `.txt` | 19 | 0 | 0 | 0 | 0.998s |
| `TEST-…PlatformJdbcIntegrationTests.xml` + `.txt` | 24 | 0 | 0 | 0 | 4.728s |
| `TEST-…PlatformServiceTests.xml` + `.txt` | 70 | 0 | 0 | 0 | 0.169s |
| 合计 | **113** | **0** | **0** | **0** | — |

XML 内 `testcase/failure|error|skipped` 子节点计数均为 0。注解：Controller 源文件 `@Test` 15 + 全限定名 `@org.junit.jupiter.params.ParameterizedTest` 1 方法 × CsvSource 4 行 = 19，与 XML 一致；Jdbc `@Test` 24；Service `@Test` 70。静态注解数不得与运行次数混称。

本席结论：Codex B1「113=19/24/70 全通过」与只读 XML 一致。**绿 XML 不是断言有效性证明**（GEN-003）；有效性见下文逐文件审阅。

## 3. 逐文件覆盖（真实全文，非抽样）

### 3.1 PlatformControllerTests.java（L1–L532）

**为什么必须存在**：删掉则 HTML 要求的平台治理 HTTP 面（商户档案、主体绑定、空间、项目关联、联系人电话展示）没有控制器层可失败断言。上位：BSM-004/072/074、PGM-026/027/043 指针下的两联系人例外、PGM-040/041、GEN-003/005/007/010。

**声称是否被执行**：本席未跑；XML 19/0/0/0。源码方法与 XML `testcase@name` 一一对应。

**未验证就当真的前提**：MockMvc + mock `PlatformService`/`AuthorizationService` 不能代替服务层/库层；本文件只锁控制器守卫与脱敏分支。单次权限用真实 `AuthorizationService` + mock `AccessRepository` 另缝（`merchantPhoneSingleUseAppliesToOneResponse`）。

覆盖要点（对抗式反例均有可失败断言）：

- 未认证 GET `/merchants` 接受 401 或 403（`unauthenticatedPlatformListReturnsUnauthorizedOrForbidden` L70–78）——只证明未放行。
- 无 `merchant.profile.manage` → 403 `PERMISSION_DENIED`（L80–98）。
- 缺幂等键建商户 → 400 `VALIDATION_FAILED`，共享闸（L100–113）；B2-r2 收窄 005：**不为每个 verb 再补一条缺键测试**。
- 列表 JSON 形状；无 PII 权两电话掩码 `188****0011/0013`；合作方式 `lease`（L116–161）。有 `pii.contact_phone.view` 才明文（L163–180）。**当前 HTML 两联系人明文例外适用**（PGM-043 原文把商户档案两联系人手机号指向「客人与员工的个人信息保护」条；该条即 PGM-059，本模块未映射，不扩锁）。
- 建空间校验 `governance.space.manage`（L182–213）。
- 存档 HTTP：`merchant.profile.manage` + 幂等键；缺键 400 且 `archiveMerchant` 只进服务一次（L216–251）——落实 003 的「控制器一条 archive 权限/幂等」，不测 fake `archiveReason`。
- 绑定列表 `entity.binding.view` + `requirePlatformScope`（L253–282）。
- 商户列表平台作用域负例（L285–295）；其余五 GET（business-entities/projects/spaces/space-types/venue-business-hours）同法 fail-closed（L298–311）。七 GET 齐。
- 绑定创建/变更只验 `entity.binding.manage`（L313–382）；方法名与断言一致。
- 项目关联：商户 PUT 与主体 POST 各消费一次功能权限；缺键/拒权不进服务（L386–429）。
- 单次许可：四组合（读/写 × 同批/分权）；明文一次后同批再调 `PERMISSION_DENIED`，分权则掩码；`markSingleUsePermissionConsumed` 一次（L432–491）。消费失败不返回明文且 `verifyNoInteractions(service)`（L493–507）。
- `TestBeans`：`AuthService("", "")` 空密钥仅为切片占位。

### 3.2 PlatformJdbcIntegrationTests.java（L1–L760）

**为什么必须存在**：内存 fake 不能证明库约束、加密信封、软删 UPDATE、真分页、行锁并发、项目 SQL 过滤。删掉则 BSM-004/PGM-026/GEN-006/007/010/014 在本模块没有真库证据。

**声称是否被执行**：XML 24/0/0/0；类注解 `@SpringBootTest` 指向 `127.0.0.1:5432/complex_test`，口令 `${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}`。本席未连库、未跑。

**未验证前提**：本席未独立连 `complex_test`。并发例等待 `pg_stat_activity` 有时序窗口（5s）；本次 XML 该例 `time=1.304` 且无 failure，**未证实正常路径缺陷**。

覆盖要点：

- 联系人姓名落库 `enc:v1:`、读回明文；空/null 不加密（L37–60）。
- 主数据表 `to_regclass` + 权限种子（L62–88）。
- 真分页 COUNT/LIMIT/OFFSET；业态 SQL 谓词（L90–115）。夹具合作方式 `lease`，状态 `active`。
- 主体/商户/项目/空间持久化；更新路径 `lease→joint_operation`（L117–287）。
- 绑定表/合同权限种子；Flyway 版本 = `MigrationVersions.latest()`（L289–304）。
- 同一商户第二条 active 绑定抛 `uq_business_entity_bindings_active_merchant`；历史行保留（L306–369）。`be_operation_*` 是主键字符串不是角色码。
- `deleteMerchant` 后 `findMerchantById` 仍在且 `deleteStatus=approved`；`listMerchants(true)`/`paged true` 含，`false` 不含（L373–388）。对照生产 `JdbcPlatformRepository.deleteMerchant`：`UPDATE … SET delete_status='approved'`（只读核生产，本模块不改）。
- 501 条空间：`listSpaces` 截 500，`findSpaceById` 仍命中（L391–401）。
- 空间类型非法 status 拒 CHECK（L404–410）。
- 项目可选列与 `business_entity_projects` 存在、无历史回填声称（L413–424）。
- 仅已结束 inactive 绑定不再占商户引用（L426–450）。
- 五类列表/COUNT 按项目过滤；公司多对多；空/非法 scope 五总数为 0（L473–511）。
- 仅回填 `project_id`，其余 jsonb 列不变；不能改已有项目（L513–545）。夹具 `suspended`+存档+pending 删除是旧资料列保留，不是合法新建状态。
- 空间类型删除申请历史两行保留（L547–561）。
- **真库并发**：`NOT_SUPPORTED` + 预锁商户行，两线程 `createBusinessEntityBinding` 未来生效；断言两请求都在 `FOR UPDATE` 上等待；结果 `CREATED`+`ACTIVE_BINDING_EXISTS`；商户三主体仍空；finally 清理（L564–636）。
- 电话密文/防二次加密/绕仓库明文 INSERT 命中 `chk_merchants_contact_phone_pii`（L651–696）。明文 INSERT 用 `status='draft'` **只证 PII CHECK**，不把 draft 锁成合法商户状态。
- `pii.contact_phone.view` 在 permissions 与 `role_platform_admin` 种子中存在（L698–702）。
- `parent_entity_id` 列与旧索引为 0（L705–713）。
- `chk_be_entity_role` 定义含 platform/project_company/management_company、不含 spv/operation；本事务只插一条 spv 违例（L716–730）。
- 不同组织可同名角色，同组织同名唯一（L733–740）。

### 3.3 PlatformServiceTests.java（L1–L1484）

**为什么必须存在**：状态机、四类主体唯一绑定、未来生效不投影、项目隔离、自审删除、软删 fake 与 JDBC 同口径——这些是服务层规格。删掉则 BSM-004、PGM-026/041、BEQR-143/148/155、GEN-003/005/006 在本模块没有服务证据。

**声称是否被执行**：XML 70/0/0/0 与 `@Test` 70 一致。

**未验证前提**：`InMemoryPlatformRepository` 不是数据库；`lockMerchant` 空实现；`setMerchantArchiveReason` 空实现（B2-r2 收窄 003 明确保留）。并发/约束以 JDBC 为准。`AuthorizationService(null)` 只走 `requirePlatformScope` 的 actor 分支，不查权限字典。

关键可失败断言（挑接缝，全文 70 例均读）：

- 软删：`deleteMerchant` fake 保留行并 `withDelete("approved",…)`（L1359–1367）；`approveDeleteSoftDeletesArchivedMerchantWhenNoReferences` 默认列表无、includeArchived 有且 approved（L542–562）。与 JDBC L373–388 同口径。**M011-001 当前事实成立。**
- 合作方式六类正例 `lease`，旧码 `fixed_rent_plus_commission` 只作负例（L207–218）。商户状态 `draft` 拒绝、`pending_admission` 接受（L222–232）。**M011-002。**
- `unarchiveMerchantRestoresDefaultListVisibility`；`rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus` 含 `deleteReason`（L565–596）。不测 archiveReason 字段。**M011-003 + B2-r2 收窄。**
- `merchantActorIsRejectedByPlatformScopeOnGovernanceWrite`：`asMerchantUser` → `PLATFORM_SCOPE_REQUIRED`（L600–609）。**M011-004。**
- 方法名 `createMerchantRejectsBusinessFormatOutsideSixValues`（L502–510）。**M011-005。**
- `createSpaceType` 生产走 `executeWrite` 仍要幂等键（只读 `PlatformService.java:539–565`）；控制器 `createSpaceType` 无重复 `requireIdempotencyKey`（`PlatformController.java:176–180`）。**M011-006 观察，缺陷否（M009-022）。**
- 未来绑定：保存 inactive、不改三主体；第二未来绑定拒绝；到期前不能 active/end；到期可激活；已结束历史不能重开；已有三主体即使 master 也不能普通替换（L900–983）。
- 项目隔离：普通管理员五列表 total=1 且只见本项目；员工不能读他项空间；空间必须当前项目；普通档案不能认领/搬项目；master 列表无全局旁路（L1141–1300）。
- 自审：`requesterCannotApproveOwnMerchantDelete` → `MERCHANT_SELF_APPROVE_FORBIDDEN`（L382–392）。**本席读到该例，旧「自审闸不可达」不能代表现在。**
- `createSpaceTypeUpsertsDictionaryAndEnablesItForSpaceValidation` 只证新建后字典可用于 createSpace；重复编码拒绝在 `createSpaceTypeRejectsExistingCodeEvenWhenActive`。名称「Upserts」过宽。
- 占用/存档/审批删除空间类型、审计 objectId、负面积、终止缓冲、角色校验中文等：断言可失败。

## 4. 三线

### 4.1 第一性原理

1. 三文件必须存在：否则 HTML 治理主数据（商户/主体/项目/空间/绑定/存档删除/联系人电话）在测试层没有可失败锁。答得上上位需求，分类为细化/技术性，不是添加。
2. 行为证据：断言有效性来自源码（错误码、状态、密文前缀、SQL 约束名、verify 次数）；可执行产物本席只核 XML，不冒充新跑。
3. 未经验证当真的：本席未连库；并发等待锁依赖环境；`setMerchantArchiveReason` 空 fake 按已定稿收窄不升缺陷。

### 4.2 顺从式（87/87）

假定实现意图正确，逐条核映射。本模块是**测试**，判据适用方式 =「夹具/断言不得与 HTML 冲突，且对本模块声称的行为给出能失败证据」。生产实现属 M009 等，本席只读对照断言口径。

| ID | 本模块证据 | 结论 |
|---|---|---|
| GEN-001 | 三文件均承载治理测试；无纯转发空测 | 满足 |
| GEN-002 | 入口/主路径/错误路径均有断言；本席未构建，XML 为他人跑 | 满足（可执行声称不由本席新跑证明） |
| GEN-003 | 关键接缝断言可失败；绿 XML 不替代审阅 | 满足 |
| GEN-004 | 缺键/非法枚举/负面积/未知主体在边界拒绝 | 满足 |
| GEN-005 | 权限码、平台作用域、项目隔离、自审、一次性权限 | 满足 |
| GEN-006 | 软删标记、唯一 active 绑定、引用占用 | 满足 |
| GEN-007 | 幂等建主体；JDBC 真锁并发；控制器缺键共享闸 | 满足 |
| GEN-008 | 夹具六类合作方式/六业态/三角色与 HTML 一致；旧码仅负例 | 满足 |
| GEN-009 | 拒绝后状态保留（占用、自审、未存档删除） | 满足 |
| GEN-010 | 电话/姓名加密；明文 INSERT 拒；展示掩码+例外 | 满足 |
| GEN-011 | 商户/空间类型审计 objectId、理由、rejected | 满足 |
| GEN-012 | 分页 pageSize、listSpaces 500 截断与按 id 查 | 满足 |
| GEN-013 | JDBC 数据源/口令环境变量；TestBeans 空密钥占位已登记 | 满足（观察见 OBS） |
| GEN-014 | 表存在、CHECK、父链列删除、Flyway latest | 满足 |
| GEN-015 | 角色校验错误信息不含英文枚举码 | 满足（本模块无 UI） |
| GEN-016 | SixValues/ManagePermission 名称与断言一致；Upserts 名称局限已登记 | 满足 |
| GEN-017 | 无新业务发明；无添加项 | 满足 |
| GEN-018 | 本报告依据 HTML 原文、源码行、sha256、Surefire XML | 满足 |
| BEQR-063 | 商户/空间类型删除审批、双人（申请≠审批人）、master 直办 | 满足 |
| BEQR-074 | 无桌台夹具锁错规则；本三文件不实现桌台权限 | 无冲突 |
| BEQR-143 | 绑定三主体+经营对象；唯一 active | 满足 |
| BEQR-144 | 无父链列；公司-项目多对多 | 满足 |
| BEQR-145 | 主体角色白名单无资产公司独立角色；夹具 management_company | 无冲突 |
| BEQR-146 | 未发明域主管新角色 | 无冲突 |
| BEQR-147 | 无收入归属夹具发明 | 无冲突 |
| BEQR-148 | 六类正例+旧收费码负例 | 满足 |
| BEQR-149 | 无职级夹具发明 | 无冲突 |
| BEQR-150 | 无终端开关发明 | 无冲突 |
| BEQR-151 | 单次授权消费与角色/临时来源 | 满足（权限计算细节属 access 模块） |
| BEQR-152 | 项目隔离列表/查找/写入 | 满足 |
| BEQR-153 | 无终端开关优先级夹具 | 无冲突 |
| BEQR-154 | 项目非法 status 拒绝 | 满足 |
| BEQR-155 | 商户状态白名单；draft 拒绝；终止非立即 | 满足 |
| BEQR-156 | 无合同状态机测试发明 | 无冲突 |
| BEQR-157 | 无员工状态发明 | 无冲突 |
| BEQR-158 | 无设备状态发明 | 无冲突 |
| BEQR-159 | 无权限状态发明 | 无冲突 |
| BEQR-166 | 无合同审批链发明 | 无冲突 |
| BEQR-189 | 无管理费方式发明 | 无冲突 |
| BSM-001 | 基础盘子对象测试存在 | 满足 |
| BSM-003 | 可改可删+分岗在存档删除与权限测试 | 满足 |
| BSM-004 | 存档隐藏、取消存档、删除审批、软删可查、理由（删除） | 满足 |
| BSM-005 | 无分步引导 UI；无冲突 | 无冲突 |
| BSM-006 | 无动态码；无冲突 | 无冲突 |
| BSM-007 | 平台管理员≠最高权限；删除审批只认 master；日常权限桥不自动给 PII | 满足 |
| BSM-009 | 基础管理对象测试 | 满足 |
| BSM-028 | 项目/商户/六类合作/多对多 | 满足 |
| BSM-029 | 无跨系统假完成 | 无冲突 |
| BSM-072 | 业态六值；商户 actor 拒平台治理 | 满足 |
| BSM-074 | 同上 + 七 GET 平台作用域 | 满足 |
| BSM-114 | HTTP 入口权限分岗 | 满足 |
| BSM-115 | 基础对象卡住后续引用（存档商户不可再绑） | 满足 |
| BSM-120 | 无把未拍板当已完成 | 无冲突 |
| PGM-001–016 | 卡片级定位：本模块测治理底座对象，无把治理写成业务部门 | 无冲突 |
| PGM-017 | 对象带项目归属；普通无跨项目 | 满足 |
| PGM-018 | 多对多非隶属链；三角色 | 满足 |
| PGM-019 | 无把收入职责锁进错误角色码 | 无冲突 |
| PGM-020 | 项目档案/可选字段/归属 | 满足 |
| PGM-021 | 隔离防数据混 | 满足 |
| PGM-026 | 两联系人、六类合作、四类主体唯一绑定、未来生效不改当前投影 | 满足 |
| PGM-027 | 商户不能看平台全局 | 满足 |
| PGM-031 | 无经销商门户发明 | 无冲突 |
| PGM-035 | 无提成发放发明 | 无冲突 |
| PGM-040 | 单一功能权限点 | 满足 |
| PGM-041 | 项目隔离无关闭开关；敏感查看另权 | 满足 |
| PGM-042 | 无职级替代功能权限的夹具 | 无冲突 |
| PGM-043 | 外包看不到平台全局（作用域）；导出/脱敏原文含两联系人例外指针，页面查看走 PII 权+掩码测试 | 满足（不扩 PGM-059） |
| PGM-044 | 无桌台挂单发明 | 无冲突 |
| PGM-048 | 商户侧边界由作用域负例锁 | 满足 |
| PGM-060 | 无会员注册发明 | 无冲突 |
| PGM-065 | 治理连接后续业务：存档卡住引用 | 满足 |
| PGM-066 | 项目归属已拍板有测试；无把待排期写成完成 | 无冲突 |

**PGM-059 未映射**：两联系人明文例外按 PGM-043 指针解释现行 HTML，不 `LOCK-AMEND`。

### 4.3 对抗式（grilling 自问，不等待）

Q1 软删若改回 `merchants.remove`，服务例与 JDBC 例是否仍绿？→ 会红（list(true) 含 approved / findById present）。  
Q2 正例夹具若仍写 `fixed_rent_plus_commission`/`draft` 当合法值？→ 与服务白名单冲突；现正例已改，旧码只在负例。  
Q3 商户 GET 若去掉 `requirePlatformScope`？→ `listingMerchantsRequiresPlatformScope` 与五路径例会红。  
Q4 未来绑定若立即投影三主体？→ `futureBindingDoesNotChangeCurrentMerchantSubjects` 与并发例商户三主体仍空会红。  
Q5 两并发未来绑定若都不锁？→ JDBC 并发例要求 `waiting==2` 且一成功一 `ACTIVE_BINDING_EXISTS`。  
Q6 单次 PII 若第二次仍明文？→ 参数化例会红。  
Q7 平台管理员种子行是否等于有效明文权？→ `isExplicitOrDailyGrant` 排除 `role_platform_admin` 高危；`pii.contact_phone.view` 在 V394 `requires_approval=TRUE` 且非 low/medium 日常桥。测试只断言种子存在，不把种子当有效权。  
Q8 自审删除？→ `MERCHANT_SELF_APPROVE_FORBIDDEN` 例存在且可失败。  
Q9 控制器 `createSpaceType` 无缺键校验是否单独成洞？→ 服务 `executeWrite` 仍要键；B2-r2/M009-022 不定为本模块必修。  
Q10 `setMerchantArchiveReason` 空 fake 是否必须升缺陷？→ 否，B2-r2 收窄 003。

未发现新的能独立于已登记观察、且违反冻结判据的工程缺陷。

## 5. 既有 7ID 全核（当前源码，不沿用旧票）

| ID | 当前源码事实 | 工程缺陷 | 队列 |
|---|---|---|---|
| M011-001 | Service L543–562 / fake L1360–1366；Jdbc L373–388；生产 UPDATE 软删 | 否 | 否 |
| M011-002 | 正例 lease/joint_operation/active；旧码仅负例；PII INSERT draft 只证 CHECK | 否 | 否 |
| M011-003 | 恢复/驳回/deleteReason/HTTP 存档权限+缺键；**不**新增 archiveReason fake | 否 | 否 |
| M011-004 | 商户 actor 服务负例 + 七 GET 作用域 | 否 | 否 |
| M011-005 | SixValues 方法名；两 ManagePermission 名称与断言一致；不按 verb 补缺键 | 否 | 否 |
| M011-006 | 控制器 createSpaceType 无重复校验键；服务 executeWrite 仍必填 | 否（跨模块观察） | 否 |
| M011-007 | 环境/夹具/覆盖边界；当前以 XML 113 为准，旧 410/57、「无联立/自审不可达」不能代表现在 | 否 | 否 |

## 6. 12 观察全核（均否，不升缺陷）

1. **OBS-R-01**：`v339Seeds…` 断言管理员角色含 PII 种子；V394 `requires_approval=TRUE`；`AuthorizationService.isExplicitOrDailyGrant` 排除默认管理员高危。不把种子当有效明文权。本模块不删迁移种子。
2. **OBS-R-02**：PGM-059 未映射。PGM-043 HTML 末句：「商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行」。该例外正文在 PGM-059。按指针解释，不扩锁。
3. **OBS-R-03**：`createSpaceTypeUpserts…` 名称沿用 Upserts，例本身只证新建后可用；拒绝重复编码另例。不发明覆盖写入。
4. **OBS-R-04**：并发例等锁有环境时序；XML 该例通过且 finally 清理。未证实正常路径缺陷。
5. **OBS-R-05**：未认证接受 401 或 403，只证未放行。
6. **OBS-R-06**：`AuthService("", "")` 切片占位。
7. **OBS-R-07**：明文 PII INSERT 的 draft 不作为合法业务状态。
8. **OBS-R-08**：空间类型 HTTP 无重复幂等/审批入口无普通管理权限闸，服务认 master + executeWrite 要键。不按 verb 补测或改 M009。
9. **OBS-R-09**：`setMerchantArchiveReason` 空 fake 按 B2-r2 003 收窄保留。
10. **OBS-R-10**：role CHECK 只插一条 spv + 完整约束定义证明排除 operation。
11. **OBS-R-11**：本席未连库；XML 来自 Codex 已跑环境。不显示秘密。
12. **OBS-R-12**：`PhoneMask` 注释不列 master 例外；真正放行在 `AuthorizationService.isMasterAuthority`。常量类不在本三文件锁错行为。

## 7. 新问题

无。临时编号未占用（无 GR-B5-01）。无添加项。无 HTML 自身冲突需铁律 4 暂停。

## 8. ponytail-review（只列不修）

- `PlatformServiceTests.java:L1303`：`setMerchantArchiveReason` 空实现 — 按已定稿收窄保留，**不 delete**。
- `createSpaceTypeUpserts…` 名称过宽 — 已观察，不因命名另补测试。
- Lean already for required seams. `net: 0` 可删业务行（在不违反 B2-r2 前提下）。

## 9. 接缝总表

| 接缝 | 证据位置 | 有效性 |
|---|---|---|
| 权限 | Controller 权限码/作用域；Service 商户 actor | 可失败 |
| PII | 掩码/明文例外；库信封；绕写 CHECK | 可失败 |
| 软删 | fake+JDBC+服务列表谓词 | 可失败 |
| 主体绑定 | 唯一 active、财务主体不可普通替换 | 可失败 |
| 项目隔离 | 五列表/COUNT、跨项目写拒绝 | 可失败 |
| 未来绑定 | 不投影、占槽、到期前不能动 | 可失败 |
| 真库并发 | JDBC FOR UPDATE 双等待 | XML 绿；时序观察保留 |
| 一次性权限 | 真 AuthorizationService 四组合 + 消费失败不回明文 | 可失败 |

## 10. 单席 PASS 条件

| 条件 | 本席 |
|---|---|
| 三文件全文覆盖 | 是（532+760+1484） |
| 87 判据已核 | 是 |
| 三线均执行 | 是 |
| 声称有四类证据 | 是（1 HTML、2 仅 XML 只读、4 sha256/wc/XML；无类型 3） |
| 无未答 questions | 是 |
| 无未定稿缺陷 | 是 |
| 无 HTML 修改申请 | 是 |
| 哈希行有效 | 本次报告第一行独占；正文无第二处同名独占行 |
| 未把未跑说成新跑 | 是 |
| 未读本次 self-raw 正文 | 是 |
| 未沿用 B1/B2/历史 B5 票 | 是（独立重判，结论与 V3.54 顶部一致但不引用其作为依据） |

**本席 B5-r1（当前锁、同名追加）：PASS。** 无有条件通过。本票不等于双席合取或模块 PASS。主调度须在两席本次完整报告都落盘后才归并。
