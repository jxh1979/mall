CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r3 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r3（B3 第 3 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r3 PASS、B3 自报或历史 B1/B5-r1/B5-r2 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M009 同一 20 文件；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 3/3（上限）；LOCK-AMEND-COUNT 1；ledger V2.14；findings V1.26
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 全文一行同值
- 映射：`M009 → [BEQR-063 … PGM-066]`，COUNT=87 unique=87 MISSING=[]；定义与 HTML 出处均从锁自读，不新造判据
- 正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；开工时工作区 after tree=`2f2bec12915a4e33e4316644b6eff7f23a98e93c`（临时索引 `GIT_INDEX_FILE=mktemp-dir/index` 下 `git read-tree HEAD && git add -A && git write-tree`，不替换主索引；对象类型 `tree`）；B3 修复未提交，审计对象=当前工作区 20 文件（含 B3-r1/r2/r3）
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m009-b5-r3-verify`，结束清理）
- 未读取：`M009-b5-r3-self-raw`（当前亦不存在）。B4-r3 / B5-r2 raw 与 findings V1.26 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V1.26 顶部「036已修转否，进入B5-r3」；001–012、014–016、026–028、036 已修；013/017–025、029–035、037 仅登记；M009-017 环境红（complex_test Flyway 409 vs 源码最新 399）不是源码缺陷
- 技能（V0.17 收窄，按实际存在与已读执行适用意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–012/014–016、026–028 与 036 修复后行为独立复验成立；013/017–025、029–035、037 维持仅登记（017 本席复现为环境红）。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。B3 已达上限 3；本票 PASS 不阻止席甲 FAIL 后记 BLOCKED-3ROUNDS。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。

LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| 字节 | 889020 |
| python3 hashlib | 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V2.14；2853 行 / 410516 字节 / SHA `f25afd8f923bb186fbebc5e93cecf6d3446f7d89bf6c4218e5bffc56533e3d2d` |
| findings | V1.26；3255 行 / 481831 字节 / SHA `e5b49240e942500e2fff5d7b20fcd9beddc66e1dc7b415d84982a09387238e6b` |
| 映射行抽出 | 87 条、无重复、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 抽检 BEQR-154/155、BSM-004、PGM-005/018/026 行邻域均命中；GEN 无 HTML 行 |
| 计划 FILES | 20 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 2476（切分时）；当前工作区 `wc -l` 合计 **2657**（B3-r1/r2/r3 增行，不改计划凑数） |
| questions | 本模块 0；目录仅 M003-023/024、M006-027，无未答 M009 问询 |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

映射闭集（锁 6388 行原文抽出，87 unique）：

BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001 … GEN-018, PGM-001 … PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040 … PGM-044, PGM-048, PGM-060, PGM-065, PGM-066.

### 1.1 20 文件当前行数 / 字节 / SHA256（工作区实算，`wc -l` = `\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| 1 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java` | 17 | 391 | `45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277` |
| 2 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java` | 19 | 463 | `f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422` |
| 3 | `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` | 90 | 5490 | `66a840d9b71935b15328b08b4daea61867851084dc27634025e45bd1cb12ace2` |
| 4 | `backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java` | 9 | 400 | `e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40` |
| 5 | `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | 1112 | 61986 | `10efae64b8dce9836d9eed6b339ddeda53caff78039b8234bfa882bab77139e2` |
| 6 | `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | 236 | 13580 | `05bd7cf4a2bfcb4398c6859abe6c56ce40041a982cdd308d51557ad00d92b743` |
| 7 | `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java` | 134 | 6645 | `09f4c02728bd7e6176272e8c8f33ac24bd0dcc11549d39747dca8b454598216f` |
| 8 | `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 876 | 43323 | `720b13d917433231150a707ed5572054566c656d589a1a48dc19a2632eb9aa94` |
| 9 | `backend/src/main/java/com/complex/platform/governance/package-info.java` | 1 | 41 | `e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9` |
| 10 | `.../dto/BusinessEntityBindingRequest.java` | 17 | 442 | `66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da` |
| 11 | `.../dto/BusinessEntityRequest.java` | 17 | 424 | `8c4b7795909a6dc597fab7937105d6b54979d9ad900a8e8d45d688fe1cbb725e` |
| 12 | `.../dto/MerchantArchiveRequest.java` | 6 | 319 | `e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93` |
| 13 | `.../dto/MerchantDeleteApprovalRequest.java` | 5 | 225 | `2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4` |
| 14 | `.../dto/MerchantDeleteRequest.java` | 5 | 214 | `4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45` |
| 15 | `.../dto/MerchantProfileRequest.java` | 27 | 767 | `cbcf242470823799c3d2fcbe996de4665e4d440614a4263a2635ccb4439bea99` |
| 16 | `.../dto/ProjectRequest.java` | 17 | 375 | `8316db69c16d3013dbfc07ead14673321a49d05c3131163ae58597543004c76e` |
| 17 | `.../dto/SpaceRequest.java` | 21 | 525 | `63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa` |
| 18 | `.../dto/SpaceTypeRequest.java` | 15 | 457 | `d181151ff1b58851a985a0effad2370b5b17937823a7caea525c7f944cd729f2` |
| 19 | `.../dto/StatusChangeRequest.java` | 11 | 231 | `75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f` |
| 20 | `.../dto/VenueBusinessHoursRequest.java` | 22 | 700 | `6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1` |

合计行 2657、字节 136998。Jdbc 1112 / Service 876 / Controller 236 均整文件读完（Jdbc 分段 L1–400 / L400–790 / L790–1112，含 L683+ `SupervisorReviewRepository` 实现），未按标题抽样。Jdbc 审批链方法按「文件已有职责不因模块名裁掉」纳入三线，但不把主管复核 Service 扩成全量审计。

相对 B5-r2 工作区指纹：仅 `PlatformService.java` 因 B3-r3 改变（876 行 / `720b13d9…`；B5-r2 为 863 行 / `2450c955…`）；其余 19 个 M009 文件 SHA 与 B5-r2 实算相同。20 文件内 `硬删`/`物理删除`/`parentEntityId`/`leasing_auto`/`expireBar` 命中数均为 0。

必要只读依赖（验证本模块调用/字段，不扩审模块）：

| 路径 | 行 | 字节 | SHA256 | 本轮用途 |
|---|---:|---:|---|---|
| `LeasingService.java` | 453 | 27927 | `169d2fcf52b1c6b933eeac2686e6a700351d9b1b5d773cfcdcf47c71c604cb0b` | L288–292：003 伴生，合作方式 `null` |
| `OrganizationMerchantPage.tsx` | 480 | 29371 | `76571cb692a4bc75ccd26f46be02e8c07c4c72806c9339feb55f91fb7144abe4` | L33–41 六类中文 option |
| `OrganizationMerchantPage.test.tsx` | 261 | 14985 | `99a3b8201d9d7a401a4013a27a5e76a325919a759c93b1ea34b0b04ac3e77f8e` | 12/12 含六类中文、拒收费模板码 |
| `PlatformControllerTests.java` | 351 | 15915 | `cbaec207daf3fc89fd72d42aa3fa42cccbefa7beb2e4eb16294685658cfeeb78` | 读端平台范围 |
| `PlatformJdbcIntegrationTests.java` | 457 | 20482 | `2b39492f831860f1960d9e25c22e939716fa26c935acf54d7d1675132ebd5057` | 027 主键空间；010 软删可查；017 环境红 |
| `PlatformServiceTests.java` | 810 | 39637 | `8775e50a0c61c7d4ef48f942e278f707a2a1836e991ca29084bbddd1251c3448` | 001–009/014/028/036 等单元（本轮 31 条） |
| `LeasingSignAtomicityJdbcIntegrationTests.java` | 162 | 8323 | `03a630dfacbc17d975945ccc688685aeca4789f60e46264f0b10807257a1ac64` | 006 伴生：seed 插入 `business_entities` |

另只读：已发布 `V203__spaces_status_check.sql`（`chk_spaces_status` = `active/unavailable`）；`V184__space_type_dictionary.sql`（注释 active/disabled，无 CHECK）；`V354` `chk_be_entity_role`；`AuthorizationService.requirePlatformScope` / `isMasterAuthority`；`AuditedOperationExecutor.executeWrite`；`PiiCipher`；`GlobalExceptionHandler` L151–155（仍不拦 `DataIntegrityViolationException`）；`VenueBusinessHoursService.save`（Controller 暴露面，day_of_week 0–6 已在该服务校验）。

活库 `complex_test` 本席 psql（只抽出三个 `COMPLEX_DATABASE_*`，库名改 `complex_test`；不打印密钥；USER_LEN=7 PASS_LEN=48）对六张主数据表 CHECK 实态：

| 表 | 约束 | 闭集 / 谓词 | 本模块服务层 |
|---|---|---|---|
| `spaces` | `chk_spaces_status` | `active`/`unavailable` | `SPACE_STATUSES` + `requireSpaceStatus` 仅 `createSpace`（036） |
| `space_type_dictionary` | 无 CHECK | V184 注释 active/disabled | `createSpaceType` 仍 `trimToDefault`（029） |
| `business_entities` | `chk_be_entity_role` | platform/project_company/management_company/**spv/operation** | 服务白名单不含后二者（005/025） |
| `merchants` | `chk_merchants_business_format` | 业态六值 | `MERCHANT_BUSINESS_FORMATS` |
| `merchants` | `chk_merchant_delete_status` | none/pending/approved | 服务只写此三态 |
| `merchants` | PII `enc:v1:%` | 电话密文 | Jdbc `piiCipher.encrypt` |
| `projects` | 无 status CHECK | — | `PROJECT_STATUSES`（004） |
| `business_entity_bindings` | 无 status CHECK；部分唯一索引 `uq_…_active_merchant` | active 每商户一条 | 服务三态 + unique |
| `venue_business_hours` | `day_of_week` 0–6 | — | 校验在 `VenueBusinessHoursService`（非本 20 文件） |

未发布 V400–V409 未给 `space_type_dictionary.status` 加 CHECK；活库 `chk_spaces_status` 与源码 V203 一致。

---

## 2. 全量三线覆盖表

| # | 文件 | 第一性原理 | 顺从式 | 对抗式 | 覆盖 |
|---|---|---|---|---|---|
| 1 | BusinessEntityRecord.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 2 | BusinessEntityBindingRecord.java | 已审 | 已审 | 已审 | 全文 19 行 |
| 3 | MerchantRecord.java | 已审 | 已审 | 已审 | 全文 90 行 |
| 4 | MerchantDeleteDecision.java | 已审 | 已审 | 已审 | 全文 9 行 |
| 5 | JdbcPlatformRepository.java | 已审 | 已审 | 已审 | 全文 1112 行（分段读完） |
| 6 | PlatformController.java | 已审 | 已审 | 已审 | 全文 236 行 |
| 7 | PlatformRepository.java | 已审 | 已审 | 已审 | 全文 134 行 |
| 8 | PlatformService.java | 已审 | 已审 | 已审 | 全文 876 行 |
| 9 | package-info.java | 已审 | 已审 | 已审 | 全文 1 行 |
| 10 | BusinessEntityBindingRequest.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 11 | BusinessEntityRequest.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 12 | MerchantArchiveRequest.java | 已审 | 已审 | 已审 | 全文 6 行 |
| 13 | MerchantDeleteApprovalRequest.java | 已审 | 已审 | 已审 | 全文 5 行 |
| 14 | MerchantDeleteRequest.java | 已审 | 已审 | 已审 | 全文 5 行 |
| 15 | MerchantProfileRequest.java | 已审 | 已审 | 已审 | 全文 27 行 |
| 16 | ProjectRequest.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 17 | SpaceRequest.java | 已审 | 已审 | 已审 | 全文 21 行 |
| 18 | SpaceTypeRequest.java | 已审 | 已审 | 已审 | 全文 15 行 |
| 19 | StatusChangeRequest.java | 已审 | 已审 | 已审 | 全文 11 行 |
| 20 | VenueBusinessHoursRequest.java | 已审 | 已审 | 已审 | 全文 22 行 |

无 finding 不等于未审。无中途回退。发现问题后继续把 87 判据与 20 文件审完再判定。

---

## 3. 第一性原理（逐文件）

删除测试：删掉这 20 文件，BSM-009/PGM-006/PGM-026「建立商户档案、四类主体绑定、综合体/空间」在后台将无写入面。答得上上位。B3-r3 后再把空间 status 的已发布 CHECK 闭集前置为中文 400（036），避免 `DataIntegrityViolationException` 落 500。

| 文件 | 为何必须存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| BusinessEntityRecord | 法律/经营主体主数据 | 创建经 Service 白名单角色+主体状态；Jdbc INSERT 不写 `parent_entity_id`；记录无 parent 字段 | DB 列仍在（V318，M010）；列 `is_nullable=YES`，本模块 INSERT 不碰它 |
| BusinessEntityBindingRecord | 四类主体一组关系 | 创建时拦第二份 active + 存在性 + 商户未下线；激活回写商户三列 | 停用绑定不清理三列（008/031 最小边界）；重新激活不走 `requireActiveMerchant`（037） |
| MerchantRecord | 商户档案+存档删除轴+电话掩码 | 25 参更新保留 archived_*/delete_*；`withEntities` 投影三列；`withMaskedPhones` 在 Controller；L27 注释为软删除标记（026） | 19 参构造仍把存档默认 none（创建/测试用） |
| MerchantDeleteDecision | 审批结果 | javadoc 为软删标记；`deleted=true` 对应 `delete_status=approved` | 内存 fake 仍 `remove`（M011/010 分叉） |
| JdbcPlatformRepository | Postgres 适配+审批链仓储 | JDBC：软删行 `findById` 仍在、`includeArchived=true` 可见；PII 加密回读；绑定 unique 部分索引拦第二份 active；`findSpaceById` 按主键（027 测试绿，listSpaces 仍 500） | 1 红=Flyway 409≠399（017）；`countMerchantReferences` 动态 COUNT；L683+ 审批链本模块 API 不调用 |
| PlatformController | HTTP 面 | 单元 10/10：无权限 403、缺幂等 400、读端 `requirePlatformScope` 403 | `createSpaceType` 控制器不预检幂等键（022）；写端平台范围在 Service；`ApiException` 仅 import（034） |
| PlatformRepository | 仓储接口/默认内存切片 | `listMerchants(true)` 全量；`deleteMerchant` 默认抛、Jdbc/fake 覆写；`businessEntityExists`/`projectExists` 按主键；默认 `findSpaceById` 仍滤 `listSpaces`，Jdbc 已覆写 | 无过滤 `listMerchants()` 仍被跨模块调用（020） |
| PlatformService | 写路径/校验/审计 | 单元 31/31：更新保档、404 更新、合作六类、状态六态、角色白名单、幽灵绑定、空间存在性、绑定投影、非最高权限拒批、最高权限直删、分页 500、已存档商户拒新引用（028）、空间非法 status 400（036） | 创建商户仍可写未校验存在的三主体列（030）；字典 status 无 CHECK（029）；无 TicketingService |
| package-info.java | 包声明 | `package com.complex.platform.governance;` | 无行为 |
| BusinessEntityBindingRequest | 绑写入参 | 四 ID + 生效日 + 理由；服务层存在性+未下线 | `operatorNote`/`evidenceAttachmentIds` 服务层不读（023） |
| BusinessEntityRequest | 主体入参 | 无 parent；角色/状态在服务层闭集 | 无 |
| MerchantArchiveRequest | 存档理由 | `reason` 存档必填；注释与 BSM-004 一致 | 取消存档可空 |
| MerchantDeleteApprovalRequest | 审批 | approved/rejected；最高权限闸在服务层 | `comment` 不落库（021） |
| MerchantDeleteRequest | 删除申请理由 | 须先存档；最高权限走直删 | 无 |
| MerchantProfileRequest | 商户档案 | 业态/合作/状态在服务层闭集 | 三主体列创建时可带、更新不覆盖 |
| ProjectRequest | 项目/场地 | 状态六态；merchantId 存在且未下线 | 无负责人/地址/经营范围（013/018） |
| SpaceRequest | 空间 | space_type 须字典 active；project/merchant/parent 存在性；merchant 未下线；status 经 `requireSpaceStatus` | 无更新空间入口；字典 status 仍任意串（029） |
| SpaceTypeRequest | 空间类型字典 | upsert by type_code | 控制器不预检幂等键；status 无服务闭集（029） |
| StatusChangeRequest | 绑定状态变更 | action∈active/inactive/suspended；报错中文+内部码 | 英文内部键属标识符例外 |
| VenueBusinessHoursRequest | 场地营业时间 DTO | Controller GET/POST 存在并 `requirePlatformScope`；day_of_week 校验在其它 Service | 保存语义不在本 20 文件；DTO 无草案/审批字段 |

---

## 4. 顺从式（87 条逐 ID）

分类固定顺序：冲突 → 细化 → 技术性 → 添加。无实现面写原因，不把 HTML 沉默当冲突。不 LOCK-AMEND。不新增 HTML 修改申请以外的锁外判据。

| ID | 覆盖结论 | 2.1 | 工程缺陷 |
|---|---|---|---|
| BEQR-063 | 沽清/反结/免单。20 文件无账单实现 | 不适用（无实现面） | 否 |
| BEQR-074 | 隐私桌台。无桌台权限实现 | 不适用 | 否 |
| BEQR-143 | 绑定表经营对象+运营/合同/结算；active 唯一。创建/变更前存在性；创建另拦已下线商户；unique 部分索引仍在；建档即绑定仅登记（006 后半） | 细化（存在性已落地） | 否（已修 006/028） |
| BEQR-144 | 多对多非隶属链。M009 面已删 `parentEntityId`（记录/DTO/Jdbc 写入/Service）；列留 M010 | 细化（012 最小面） | 否（已修 012） |
| BEQR-145 | 城市项目公司职责。本模块只存角色码 `project_company`，不闸酒吧人员设备定价 | 细化；职责闸在后续域 | 否 |
| BEQR-146 | 运营公司职责。`merchant_type` 六值与 BSM-072 对齐 | 细化 | 否 |
| BEQR-147 | 收入归属。本模块不记账 | 不适用 | 否 |
| BEQR-148 | 合作六类。`MERCHANT_COOPERATION_FORMS` 非空只接受六码；空=未定；页面中文六 option；Leasing L291–292 写 `null` | 细化（技术映射） | 否（已修 003） |
| BEQR-149 | 职级 M1–M10。无职级字段 | 不适用 | 否 |
| BEQR-150 | 十一端。无终端开关 | 不适用 | 否 |
| BEQR-151 | 最终权限公式。本模块 `requirePermission`+`requirePlatformScope` | 细化（调用既有授权） | 否 |
| BEQR-152 | 项目隔离/敏感价。电话脱敏在 Controller；内部价不在本文件 | 细化（PII 展示） | 否 |
| BEQR-153 | 终端开关优先级。无开关实现 | 不适用 | 否 |
| BEQR-154 | 综合体六态。`PROJECT_STATUSES` 六值；非法 `VALIDATION_FAILED` 中文 | 细化 | 否（已修 004） |
| BEQR-155 | 商户六态+退出缓冲期。可设五码+存档轴；缓冲期/暂停决定流程无实现面 | 可设状态：细化；缓冲期：018 登记 | 否（已修 004；缓冲期维持 018） |
| BEQR-156 | 合同状态。无合同表写入 | 不适用 | 否 |
| BEQR-157 | 员工状态。无 | 不适用 | 否 |
| BEQR-158 | 设备状态。无 | 不适用 | 否 |
| BEQR-159 | 权限状态。权限不在本文件维护 | 不适用 | 否 |
| BEQR-166 | 合同审批链。Jdbc 有链 SQL；商户删除走最高权限+禁自审，不是该条「主线+平行线」 | 商户删除：BSM-004 路径；链 SQL 属后续模块 | 否（009 已修删除审批；链引擎 023） |
| BEQR-189 | 服务点单管理费。无服务目录 | 不适用 | 否 |
| BSM-001 | 先管盘子。本模块是盘子写入面 | 细化 | 否 |
| BSM-003 | 通用可改可删。由 004 具体化 | — | 见 BSM-004 |
| BSM-004 | 存档/软删/先存档再审批/禁自审/占用检查/填理由/可查可恢复/下线不再被新业务选用。更新保档；软删 UPDATE；includeArchived 含 approved；最高权限直删；非最高拒批；禁自审；三创建路径 `requireActiveMerchant`；026 三处注释已改软删口径 | 细化（行为已对齐）；批量存档页/清理工具无实现面；变更绑定激活未纳入 028 三处（037） | 否（已修 001/009/010/011/026/028；流程页 018；037 登记否） |
| BSM-005 | 分步引导弹窗。后台 UI 不在本 20 文件 | 不适用 | 否 |
| BSM-006 | 动态码。无 | 不适用 | 否 |
| BSM-007 | 平台管理员≠最高权限。删除审批 `isMasterAuthority`；权限码 manage/approve 分离 | 细化 | 否（已修 009；种子在 M010） |
| BSM-009 | 基础管理入口。本模块承载商户/主体/空间/项目写入 | 细化 | 否 |
| BSM-028 | 建综合体/商户/六类合作/多对多。合作与层级见 003/012 | 细化 | 否（已修） |
| BSM-029 | 下游要空间商户归属。本模块提供 ID；空间存在性按主键 | 细化 | 否（已修 007/027） |
| BSM-072 | 业态六值。应用层闭集；读端平台范围 | 细化 | 否（015 读端已加） |
| BSM-074 | 商户不能看平台全局。写/读 `requirePlatformScope` | 细化 | 否（已修 015） |
| BSM-114 | 入口表。本模块是管理后台 API | 细化 | 否 |
| BSM-115 | 基础管理连所有业务。提供归属 ID | 细化 | 否 |
| BSM-120 | 已拍板清单。与本 20 文件无直接实现面的条目不发明 | 无实现面为主 | 否 |
| GEN-001 | 20 文件均有用途；`requireDate`/`readObjectMap` 已删 | 技术性 | 否（已修 016）；034 未用 import 仅登记 |
| GEN-002 | 单元 31+10+11 绿；JDBC 12/13 绿（027/010 绿；036 单元绿）；签约原子 2/2 绿；页面 12/12 绿 | 技术性 | 否（017 环境红单独登记） |
| GEN-003 | 关键分支有能失败断言（非法合作/状态/角色/幽灵绑定/非最高权限/更新 404/软删可查/501 条空间按 id/已存档拒引用/空间非法 status） | 技术性 | 否 |
| GEN-004 | 业态/合作/主体状态/项目状态/角色/绑定对象/空间归属/空间 status 均有校验；新引用拦已下线商户 | 技术性 | 否（已修 002–007/028/036）；创建路径三列存在性见 030；字典 status 见 029 |
| GEN-005 | 各写入口权限码+平台范围；删除审批最高权限；读端平台范围 | 细化 | 否（已修 009/015） |
| GEN-006 | 更新保档；绑定存在性；绑定激活投影三列；软删不抹行；空间按主键 | 细化 | 否（已修 001/006/008/010/027） |
| GEN-007 | 写路径幂等执行器；active 绑定 unique 索引；`createSpaceType` 空键服务层仍拒 | 技术性观察 | 否（022 仅风格） |
| GEN-008 | 档案更新不覆盖三列；分页 pageSize=500；合作码前后端同词表；`SPACE_STATUSES`≡已发布 V203 | 技术性 | 否（已修 003/008/014/036）；内存 fake 分叉归 M011 |
| GEN-009 | 错误码中文为主；括号内内部码；空间非法值 400 而非 CHECK 500 | 观察（023）；036 已修空间半边 | 否 |
| GEN-010 | 电话 PII 仓库加密（JDBC 测试绿）；动态 COUNT 表名来自 information_schema | 技术性 | 否 |
| GEN-011 | executeWrite 记成功/失败事件；存档/删除有事件名 | 细化 | 否 |
| GEN-012 | 列表 size cap 500；Jdbc paged LIMIT/OFFSET；空间存在性不再被 list LIMIT 截断 | 技术性 | 否（已修 027） |
| GEN-013 | 无本模块构建脚本 | 不适用 | 否 |
| GEN-014 | 无迁移文件（M010） | 不适用 | 否 |
| GEN-015 | 无本模块 UI；API JSON 用内部键 | 观察（内部标识符） | 否 |
| GEN-016 | 010 四处 + 026 三处注释均已改软删口径；20 文件 `硬删`/`物理删除` 零命中 | 技术性 | 否（已修 010/026）；M011 测试注释仍有「拒硬删」见观察 |
| GEN-017 | 本批无把发明标成技术性；025 spv/operation 转 M010；036 闭集来自已发布 CHECK 非 HTML 新造 | — | 否 |
| GEN-018 | 结论落到 HTML 原文、单元/JDBC/页面输出、psql 只读；017 失败如实 | — | — |
| PGM-001 | 治理底座/全中文/金额/先成功。本模块是底座写入；金额精度无本文件计算 | 细化；报错观察 | 否 |
| PGM-002 | 先看对象再合同。本模块只建对象，不卡合同 | 无合同闸实现面 | 否 |
| PGM-003 | 模块入口卡片。无 HTML/UI | 不适用 | 否 |
| PGM-004 | 综合体和集团卡片 | 不适用（文案） | 否 |
| PGM-005 | 空间点位卡片。空间 API+存在性按主键+status 二值前置 | 细化 | 否（已修 007/027/036） |
| PGM-006 | 商户和主体。合作六类+绑定存在性+未下线 | 细化 | 否（已修 003/006/028） |
| PGM-007 | 合同和准入。无合同写入 | 不适用 | 否 |
| PGM-008 | 员工和手机。无 | 不适用 | 否 |
| PGM-009 | 权限和职级。只消费权限码 | 不适用（权限配置面） | 否 |
| PGM-010 | 终端开关。无 | 不适用 | 否 |
| PGM-011 | 设备。无 | 不适用 | 否 |
| PGM-012 | 商户侧全开放。本模块是平台后台 API + `requirePlatformScope` | 细化 | 否 |
| PGM-013 | 开放接口。无 | 不适用 | 否 |
| PGM-014 | 留痕复核。写路径走审计执行器；审批链 SQL 在 Jdbc | 细化 | 否 |
| PGM-015 | 楼宇运营。无机电 | 不适用 | 否 |
| PGM-016 | 应急与隐私。电话脱敏+加密；U 盘破窗无本文件入口 | 细化（PII）；USB 无实现面 | 否 |
| PGM-017 | 当前一项目未来集团。projects 行带 merchant/venue，无集团视图 | 细化不足，013/018 登记 | 否 |
| PGM-018 | 多对多非隶属。M009 面已去 parent | 细化 | 否（已修 012） |
| PGM-019 | 各主体职责表。角色三码白名单；无收入入账 | 细化（角色）；收入不适用 | 否（已修 005） |
| PGM-020 | 建立综合体项目档案。`createProject` 存在；负责人/地址/经营范围无列 | 遗漏登记 013 | 否（队列否，不在本模块造列） |
| PGM-021 | 避免数据混。无跨项目隔离字段在本 DTO | 观察 | 否 |
| PGM-026 | 商户档案+四类绑定+六类合作。合作闭集+绑定存在+激活投影+创建拦已下线 | 细化 | 否（已修 003/006/008/028） |
| PGM-027 | 商户不能看平台全局。读端 `requirePlatformScope` | 细化 | 否（已修 015） |
| PGM-031 | 外部经销商主体/合同价格。无经销实现 | 不适用 | 否 |
| PGM-035 | 提成发放。无薪资实现 | 不适用 | 否 |
| PGM-040 | 单一功能授权。本模块只消费权限码 | 不适用（权限配置面） | 否 |
| PGM-041 | 权限规则/项目隔离。读端平台范围+权限码 | 细化 | 否（已修 015） |
| PGM-042 | 职级规则。无职级字段 | 不适用 | 否 |
| PGM-043 | 外包/审计人员。无 | 不适用 | 否 |
| PGM-044 | 桌台挂单经销权限。无 | 不适用 | 否 |
| PGM-048 | 商户侧全开放。本模块 fail-closed 平台范围 | 细化 | 否 |
| PGM-060 | 会员/生物信息。无 | 不适用 | 否 |
| PGM-065 | 治理连接各业务。本模块提供归属 ID | 细化 | 否 |
| PGM-066 | 已拍板规则。集团视图/全中文等与本 20 文件无新发明 | 无实现面为主 | 否 |

映射 87/87 均有覆盖记录。添加项 0（025 仍转 M010，本模块 20 文件不产生 spv/operation；活库 `spv_or_operation=0`）。无锁外判据。BEQR-170 不在本模块 87 条，036 未引入租赁看板六态。

---

## 5. 对抗式（自问自答，grilling 结构，不向操作员发问）

假定实现仍错，构造最小反例；本席自行取证：

1. **更新抹存档（001）**：`updateMerchantKeepsArchiveAndPendingDeleteState` 断言 `archivedAt`/`deleteStatus=pending`/`createdAt` 保留，且默认列表仍隐藏。**ok**。
2. **路径 id 建档（002）**：`updateMerchantRejectsUnknownMerchant` → `MERCHANT_NOT_FOUND`，仓储无该 id。**ok**。
3. **合作任意串（003）**：`fixed_rent_plus_commission` → `MERCHANT_COOPERATION_FORM_INVALID`；`lease` 接受；空接受。Leasing L291–292 注释+`null`。页面六类中文、不含 `fixed_rent`/`revenue_share`。psql `bad_coop=0`。**ok**。
4. **状态任意串（004）**：商户 `draft` 拒；`pending_admission` 接受；项目 `zzz` 拒。**ok**。
5. **角色 spv（005）**：`createBusinessEntityRejectsRoleOutsideWhitelist`。活库 `spv_or_operation=0`。**ok**（本模块 API）。
6. **幽灵绑定（006）**：缺商户 `MERCHANT_NOT_FOUND`；缺主体 `BUSINESS_ENTITY_NOT_FOUND`。签约夹具先 INSERT `business_entities`，原子测试 2/2。**ok**。
7. **空间幽灵归属（007）**：缺项目/商户/上级空间分别 404。**ok**。
8. **档案改三列（008）**：更新传入其它主体，读回仍绑定值；创建绑定后三列=绑定值。**ok**。
9. **非最高权限审批（009）**：`MERCHANT_DELETE_APPROVER_NOT_MASTER`，状态仍 pending；最高权限申请直达 `approved`。**ok**。
10. **物理删除（010）**：JDBC `approvedDeletedMerchantStaysQueryableWithIncludeArchived`：`findById` 在、includeArchived 含、默认列表不含。`deleteMerchant` SQL=`UPDATE delete_status='approved'`。**ok**。内存 fake 仍 remove（M011）。
11. **存档即撤场停票（011）**：`PlatformService` 无 `TicketingService`/`expireBarSingleStoreTickets`。**ok**。
12. **parent 链（012）**：20 文件 `parentEntityId` 零命中；Jdbc INSERT 列清单无 parent；DB 列可空。**ok**。
13. **分页（014）**：`listSpaceTypesPageSizeCoversAllItems` pageSize=500。**ok**。
14. **商户读平台列表（015）**：`listingMerchantsRequiresPlatformScope` 403 `PLATFORM_SCOPE_REQUIRED`（日志实见「无权查看平台级经营分析」）。**ok**。
15. **死代码（016）**：`requireDate`/`readObjectMap` 零命中。**ok**。
16. **残留硬删注释（026）**：20 文件 `硬删`/`物理删除` 零命中。MerchantRecord:27 / Jdbc:198 / PlatformRepository:54 均为软删除口径。**ok**。
17. **空间 LIMIT 500 截断存在性（027）**：Jdbc `findSpaceById`=`WHERE id=:id`；`findSpaceByIdIsNotTruncatedByListLimit` 插 501 条后最早 id 仍 present，同时 `listSpaces().size()==500`。**ok**。
18. **已存档商户被新引用（028）**：`archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 项目/空间/绑定均 `MERCHANT_ARCHIVED` 400。三处调用 `requireActiveMerchant`（L388/L486/L558）。**ok**。`changeBusinessEntityBinding` 未走该闸——037 定稿否。
19. **空间非法 status 落 500（036）**：`createSpaceRejectsStatusOutsideCheckSet`：`zzz` → `VALIDATION_FAILED`，列表无该 status；`unavailable` 接受。`requireSpaceStatus` 仅 L503 `createSpace` 与 L688 定义；中文「可用/不可用」。活库 `chk_spaces_status` 同 V203 二值。`createSpaceType` L457 仍 `trimToDefault`（029；活库该表无 CHECK）。**ok**。
20. **重复 active 绑定**：JDBC unique 索引仍抛 `uq_business_entity_bindings_active_merchant`。**ok**。
21. **幂等空键**：Controller 缺键 400 `idempotency_key is required`（日志实见）。**ok**。
22. **Flyway 头**：classpath 最新 V399，库 409，断言红——017 环境，不是本 20 文件缺陷。
23. **六表 CHECK 对服务层（席甲 B5-r2 猎法复扫）**：spaces 已对齐；字典无 CHECK；entity_role 服务窄于库（005/025）；业态/删除态服务覆盖库 CHECK；projects/bindings status 无 CHECK、服务有白名单；venue_business_hours 的 day_of_week 校验在其它 Service。无新的「CHECK→500」半边。

未发现新的越权写、部分写入抹档、空间存在性截断、已下线商户经三创建路径被选用、空间非法 status 落 500、或锁外业务发明。

Ponytail-review（只列不改）：`JdbcPlatformRepository.java:15` `import java.math.BigDecimal` 无类型使用（仅 `getBigDecimal` 方法名含子串）；`PlatformController.java:7` `import ApiException` 无类型使用（同根 034，已登记否）；`PlatformServiceTests.java:706` 测试注释仍写「拒硬删」（M011 文件，026 同根）；`PlatformServiceTests.java:722` `merchants.remove`（010→M011）。20 文件行为面无可再删的实现。

---

## 6. 已定稿项复验 vs 仅登记项

| ID | 队列（定稿） | B5-r3 复验 |
|---|---|---|
| 001 | 已修 | 更新保 25 参；测试绿 |
| 002 | 已修 | 更新缺失 404；测试绿 |
| 003 | 已修 | 六类闭集+页面+Leasing null；测试/页面绿 |
| 004 | 已修 | 商户/主体/项目白名单；测试绿 |
| 005 | 已修 | 三角色白名单；测试绿 |
| 006 | 已修（仅存在性） | 绑定存在性+签约夹具；测试绿 |
| 007 | 已修 | 空间/项目商户存在性；测试绿 |
| 008 | 已修（写侧投影） | 更新不覆盖、激活回写；测试绿 |
| 009 | 已修 | 非最高拒、最高直删；测试绿 |
| 010 | 已修 | JDBC 行保留可查；测试绿 |
| 011 | 已修 | 无停票调用 |
| 012 | 已修（只删 M009 面） | 接口无 parent；列可空留 M010 |
| 014 | 已修 | pageSize=500 |
| 015 | 已修 | 读端平台范围 403 |
| 016 | 已修 | 死代码已删 |
| 026 | 已修 | 三处注释改软删；20 文件零「硬删」 |
| 027 | 已修 | Jdbc 按主键；501 条测试绿 |
| 028 | 已修 | 三创建路径 `requireActiveMerchant`；单元绿 |
| 036 | 已修（B3-r3） | `SPACE_STATUSES`≡V203；`requireSpaceStatus` 只用于 `createSpace`；中文 400「可用/不可用」；单元绿；活库 CHECK 同二值；不扩 `createSpaceType` |
| 013 | 否 | 维持：ProjectRecord/迁移/页面不在本 FILES |
| 017 | 否（环境） | 本席复现 expected `"399"` but was `"409"`；psql `complex_test\|409`；Flyway warn newer than 399；源码目录最新 V399 |
| 018 | 否 | 缓冲期/已存档内容页/清理工具仍无实现面 |
| 019 | 否 | 同构审批不在本 20 文件 |
| 020 | 否 | `listMerchants()` 无过滤版本仍在接口 |
| 021 | 否 | `comment` 仍不落痕 |
| 022 | 否 | `createSpaceType` 控制器仍不调 `requireIdempotencyKey` |
| 023 | 否 | `saveChainNodes` 仍无方法级事务；切分缝仍在 |
| 024 | 否 | 拒绝文案/缺键译法不在本 FILES |
| 025 | 否 | 白名单不含 spv/operation；活库 0 行；转 M010 |
| 029 | 否 | 字典 `createSpaceType` status 仍 `trimToDefault`；活库无 CHECK；空间半边已由 036 升格修好 |
| 030 | 否 | 创建商户仍可写未校验存在的三主体列 |
| 031 | 否 | 停用绑定不清理商户三列 |
| 032 | 否 | PUT status 空仍缺省 active |
| 033 | 否 | 最高权限直删仍 save+deleteMerchant 双写同列 |
| 034 | 否 | 两处未用 import 仍在 |
| 035 | 否 | 绑定不校验三主体 status |
| 037 | 否 | `changeBusinessEntityBinding(active)` 仍不调用 `requireActiveMerchant`；028 最小边界 |

---

## 7. 本次 findings / 观察

临时编号 `GR-M009-B5-R3-xx`（与 B5-r1 的 `GR-M009-B5-01…06`、B5-r2 的 `GR-M009-B5-R2-01…06` 区分；后者已定稿为 026–037）。无新的未定稿工程缺陷，故无修复队列候选、不发 2.2、不申请改 HTML。

### 7.1 观察（不入修复队列，不构成 B5 FAIL）

| 编号 | 同根因 | 位置 | 事实 | 2.1 | 冻结 ID | 工程缺陷 |
|---|---|---|---|---|---|---|
| GR-M009-B5-R3-01 | M009-029 | `PlatformService.java:457` `createSpaceType` `trimToDefault(request.status(),"active")` | 字典 status 仍任意串。V184 无 CHECK；活库 `space_type_dictionary` 无 CHECK。036 定稿只修 spaces 半边 | 技术性 | GEN-004 | 否（维持 029） |
| GR-M009-B5-R3-02 | M009-037 | `PlatformService.changeBusinessEntityBinding` 不调用 `requireActiveMerchant` | 已存档商户不能经创建项目/空间/绑定被选用（028 测试绿）；把既有绑定改回 active 仍不检查 archived_at。037 定稿否 | 细化观察 | BSM-004 | 否（不扩 028 边界） |
| GR-M009-B5-R3-03 | M009-017 | `PlatformJdbcIntegrationTests.java:263` | 本席隔离跑：expected `"399"` but was `"409"`；psql `complex_test\|409`；Flyway「schema 409 newer than 399」。源码迁移目录最新 V399 | 技术性（环境） | GEN-003/GEN-013 | 否 |
| GR-M009-B5-R3-04 | M009-034 | `JdbcPlatformRepository.java:15`、`PlatformController.java:7` | 未用 `BigDecimal` 类型 import（`getBigDecimal` 方法名含子串）；`ApiException` 仅 import | 技术性 | GEN-001 | 否（登记） |
| GR-M009-B5-R3-05 | M009-026/010→M011 | `PlatformServiceTests.java:706`「拒硬删」；`:722` `merchants.remove` | 本 20 文件注释已清；该句与内存 fake 物理 remove 在 M011 | 技术性（跨模块） | GEN-016/GEN-008 | 否 |
| GR-M009-B5-R3-06 | M009-030 | `saveMerchant` 创建分支三主体列 | 创建仍可写入未校验存在的三列；更新不覆盖；绑定激活才投影 | 细化观察 | GEN-004/PGM-026 | 否 |

031–035 事实仍在，维持登记，不另开号。无添加项。无 HTML 自身冲突需申请修改。无锁外判据。questions 0。

---

## 8. 命令与真实输出 / 退出码

验证均在 `/tmp/m009-b5-r3-verify` 隔离副本（rsync 工作区 `backend/` 排除 target；web 复制 `apps/web` 并链仓库 `node_modules`、复制 `tsconfig.base.json`）。只从 `/etc/complex/complex.env` **抽出三个** `COMPLEX_DATABASE_*`（不 source 整文件），URL 库名改写为 `complex_test`（不打印密钥；USER_LEN=7 PASS_LEN=48）。JAVA_HOME=`/usr/lib/jvm/temurin-21-jdk-amd64`。主仓 cwd 未跑写模式 mvn/vitest。

### CMD-LOCK（exit 0）

```
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
```

python3 hashlib 同值 889020 字节；openssl dgst -sha256 同值。sidecar 全文一行同值。

### CMD-HEAD / TREE（exit 0）

```
HEAD=242f487233d028173cb9198eecd079f5d877405c
origin/main=242f487233d028173cb9198eecd079f5d877405c
after-tree(开工)=2f2bec12915a4e33e4316644b6eff7f23a98e93c
git cat-file -t 2f2bec… = tree
```

### CMD-PSQL 只读（exit 0）

```
current_database|current_user=complex_test|complex
max(flyway)=409
parent_entity_id is_nullable=YES
chk_spaces_status = active/unavailable
space_type_dictionary 无 CHECK
chk_be_entity_role = platform/project_company/management_company/spv/operation
chk_merchants_business_format = 业态六值
chk_merchant_delete_status = none/pending/approved
uq_business_entity_bindings_active_merchant 仍在
merchants=87  entities=6  bindings=0  projects=0  spaces=0  space_types=8  spv_or_operation=0  bad_coop=0
```

### CMD-UNIT-JDBC（Maven surefire；**进程 exit 1**，因 017 一条断言）

命令（隔离副本 cwd `/tmp/m009-b5-r3-verify/backend`）：

`mvn -Dtest=com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformJdbcIntegrationTests,com.complex.platform.leasing.LeasingServiceTests,com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests test`

关键输出：

```
Flyway: Successfully validated 387 migrations
Current version of schema "public": 409
WARN Schema "public" has a version (409) that is newer than the latest available migration (399) !
Schema "public" is up to date. No migration necessary.

PlatformJdbcIntegrationTests  Tests run: 13, Failures: 1, Errors: 0
  residualMigrationCreatesBindingContractTablesAndSeedsPermissions
    expected: "399" but was: "409"  (line 263)
  findSpaceByIdIsNotTruncatedByListLimit  ok
  approvedDeletedMerchantStaysQueryableWithIncludeArchived  ok
PlatformServiceTests          Tests run: 31, Failures: 0, Errors: 0
  createSpaceRejectsStatusOutsideCheckSet  ok
  archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding  ok
PlatformControllerTests       Tests run: 10, Failures: 0, Errors: 0
LeasingServiceTests           Tests run: 11, Failures: 0, Errors: 0
LeasingSignAtomicityJdbcIntegrationTests  Tests run: 2, Failures: 0, Errors: 0

Tests run: 67, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
MAVEN_EXIT=1
```

不得把未运行测试写通过：上表 5 个类均本席本步实跑。唯一失败=017 环境，不折算为本模块源码缺陷。027/028/036 归因测试均为 ok。

Controller 日志实见：`PLATFORM_SCOPE_REQUIRED raw_message=商户账号无权查看平台级经营分析`；`VALIDATION_FAILED raw_message=idempotency_key is required`。

### CMD-WEB（exit 0）

隔离副本 `npx vitest run src/admin/pages/OrganizationMerchantPage.test.tsx`：

```
✓ src/admin/pages/OrganizationMerchantPage.test.tsx (12 tests)
Test Files  1 passed (1)
Tests  12 passed (12)
WEB_EXIT=0
```

### CMD-GREP-20（exit 0）

本模块 20 文件：`硬删` 0、`物理删除` 0、`parentEntityId` 0、`leasing_auto` 0、`expireBar` 0。

`requireActiveMerchant` 调用点：L388 `createProject`、L486 `createSpace`、L558 `createBusinessEntityBinding`（定义 L346）。`requireSpaceStatus` 调用点：仅 L503 `createSpace`（定义 L688）。

临时副本结束清理：`rm -rf /tmp/m009-b5-r3-verify /tmp/m009-b5-r3-db.env /tmp/m009-b5-r3-logs /tmp/m009-b5-r3-npm-cache`。

---

## 9. 技能实用 / 缺口

已读并按 V0.17 收窄使用（用户/160 指令优先于技能本体；**不**派子代理、**不**写 `docs/arc/audits/`、**不**向操作员提问）：

| 技能 | 用法 |
|---|---|
| using-superpowers | 先核技能再取证；本任务以 160 V0.17 为唯一审核标准 |
| using-arc | 只借机械核对/证据口径；禁止评审子代理与另写 arc 报告 |
| grilling | 只用追问树自问自答（§5）；不向操作员发问。已读 SKILL.md：本体是对用户发问的设计树，与「不问操作员」冲突，故只取其追问结构 |
| verification-before-completion | 先跑隔离命令再声称 PASS；Maven exit 1 不粉饰 |
| ponytail-review | 只列可删注释/未用 import，不落地 |
| codebase-design | PlatformService 为深模块；Jdbc/内存 fake 为仓储适配器；删除测试证明 20 文件有上位 |
| diagnosing-bugs / systematic-debugging | 017：读错误→复现 Flyway 409 vs 399→判定环境非源码；036：对照 V203 CHECK 与服务闭集 |
| writing-for-agents | 本 raw 给席甲/操作员读：步骤+完成判据同文件 |
| long-running-background-tasks | Maven/vitest 在本步内跑完并读取 exit code，不把未完成任务报通过 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development 写新测试（B3 已落）、requesting-code-review（B4）、receiving-code-review、resolving-merge-conflicts、using-git-worktrees（阶段 B 不建 worktree）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未作为独立可调用项使用
- `grill-me`：本体转发 grilling；本环境按自问自答执行，不调用模型发问
- `ubiquitous-language`：未核到独立技能文件
- `git-guardrails-claude-code`：存在则需装 hook，本席只读不安装

明确不用：dispatching-parallel-agents、subagent-driven-development、executing-plans、research、domain-modeling、MCP、第三席。

---

## 10. 本席 B5 结论

**PASS**。

同时满足：20 文件均有三线覆盖记录；87 条映射判据均已核对；三线均执行；声称均有 2.3 四类证据之一；无未答 questions；无未定稿缺陷；无添加项未答；无 HTML 问题；锁行有效。017 红已复现并维持环境登记。001–012/014–016/026–028/036 行为独立复验成立。无有条件通过。

本票不等于双席合取或模块 PASS。
