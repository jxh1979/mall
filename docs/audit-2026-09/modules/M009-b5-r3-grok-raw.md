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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r3 Grok 乙席完整审计报告

- 文件版本：V0.02（指定路径已有 V0.01 前缀 45189 bytes / SHA256 `490ef1c60cf8a11ecabe35818a26b840210dc0b8a8faea219082318ccd47945a`，本次在末尾追加完整报告，不覆盖、不采信该前缀结论）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本次 `M009-b5-r3-self-raw` 任何正文）
- 步骤：B5-r3（B3 第 3 次整批修复后的整模块全量三线审计；不是只看 SpaceTypeRequest 注释或 B4-r3 859byte diff；不可用 B4-r3 双 PASS、B3 自报、历史 B1/B5-r1/B5-r2 或同名 V0.01 前缀替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M009 同一 20 文件；冻结有效内容行 **2476 不改**；当前同 20 文件实算 **3369**（修复后增长已登记，计划与映射未私改）
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3=3/3；LOCK-AMEND-COUNT 1；ledger V3.71；findings V2.14
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 全文一行同值；ledger 顶行同值
- 映射：锁 6388 行 `M009 → [BEQR-063 … PGM-066]`，COUNT=87 unique=87 MISSING=[]；定义与 HTML 出处均从锁自读，不新造判据
- 正本：`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`（相对 origin/main `495285da120e6c8c0cc85659c1a1501baf2b294d` ahead 58）；审计对象=工作区 20 文件，与 after tree `39b7324829f44339c8c2afb4d2800dfce674ed2c` 逐文件字节相等；before=`aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d` 仅作 B4-r3 已冻结背景，B5 不靠 diff 收口
- 本席约束：只读业务代码与计划；唯一写入本 raw；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试
- 未读取：任何本次 `M009-b5-r3-self-raw` 正文（仅 `ls` 见文件存在、46511 bytes）。findings V2.14 / ledger V3.71 / B4-r3 双方 SHA 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V2.14 SHA256 `881f53efae81521efecf48b8413b62dbd3273a47e03d0db2a81c36ecb0da3dd0` 当前 24 行全否=18 已修+6 登记；057 在 B4-r3 双 PASS 后才清队列，表内事实保留修复前描述。013/038 批准保留；054–056 先改后确认历史不倒填。用户非 HTML 修复授权已收到。三业务 HTML 只读。
- 技能（V0.17 收窄）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。18 项已修行为按现行源码与既有可失败断言独立复验成立；6 项登记否不因本票 NO 删除。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。B3 已达上限 3；本票 PASS 不阻止席甲 FAIL 后记 `BLOCKED-3ROUNDS`。

---

## 1. 锁、范围与源指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| 字节 | 889020 |
| python3 hashlib | 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger V3.71；3611 行 / 672104 字节 / SHA `1686db28f37de29da7f602d870209d7a46d090e3b2c5e460e3fa4a591c74ce8d` |
| findings | V2.14；4324 行 / 867068 字节 / SHA `881f53efae81521efecf48b8413b62dbd3273a47e03d0db2a81c36ecb0da3dd0` |
| 映射行抽出 | 87 条、无重复、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 69 条业务判据：**首片段邻域 69/69 命中**；**全文摘录 67/69**。未命中全文的仅 BSM-114（锁「考勤打卡」/现 HTML:1865「查看考勤」）与 BSM-120（锁页脚 `2026-09-08` / 现 HTML:2003 `2026-09-10`）。GEN 无 HTML 行。原「87/87」只表示首片段邻域，不等于 69 业务片段全文同字节。 |
| 计划 FILES | 20 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 2476（切分时，不改）；当前工作区 `wc -l` 合计 **3369** |
| questions | 本模块 0；目录无 `M009-*` |

三 HTML 工作区 SHA（本席独立；`git diff HEAD` 与 `git diff after` 均空；本轮未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

映射闭集（锁 6388 行原文抽出，87 unique）：

BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001 … GEN-018, PGM-001 … PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040 … PGM-044, PGM-048, PGM-060, PGM-065, PGM-066.

after 树：`git cat-file -t 39b7324829f44339c8c2afb4d2800dfce674ed2c` → `tree`。20 文件工作区 blob 均等于该 after（python 逐文件 `git show after:path` 比对，unequal=0）。B3-r3 仅 `SpaceTypeRequest.java`：before `aed0f2d9…` 注释为「录入/修改…存在则更新名称/排序/状态」且无文件版本；after 为「空间类型字典新建请求…typeCode 不得重复，新建状态固定为启用。」+ `// 文件版本：V0.01`。工作区与 after 字节相等；去注释非空 token 与 after 一致（24 token）。无运行代码变化。

### 1.1 20 文件当前行数 / 字节 / SHA256（工作区实算，`wc -l` = `\n` 计数；均 = after）

| # | 路径 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| 1 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java` | 17 | 391 | `45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277` |
| 2 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java` | 19 | 463 | `f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422` |
| 3 | `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` | 116 | 7319 | `dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00` |
| 4 | `backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java` | 9 | 400 | `e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40` |
| 5 | `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | 1338 | 76785 | `2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a` |
| 6 | `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | 296 | 16702 | `9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2` |
| 7 | `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java` | 204 | 10238 | `df6dfee9894fc34f1df28fdae3d549ba50320df14219129779fa267affb08cdc` |
| 8 | `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 1173 | 61467 | `984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1` |
| 9 | `backend/src/main/java/com/complex/platform/governance/package-info.java` | 1 | 41 | `e3094c51b508bf10015ead9e92ac2ae2f3edf3b09786d88816ab8009d21921d9` |
| 10 | `.../dto/BusinessEntityBindingRequest.java` | 17 | 442 | `66d4a8cd5c7f206f0c023e190ac5ed94ff6585263a737e35f062be9d8de211da` |
| 11 | `.../dto/BusinessEntityRequest.java` | 26 | 944 | `18911088cbde9575ca7c38f9a6ac6eee3f4a812098aea9a29482133fe14dec19` |
| 12 | `.../dto/MerchantArchiveRequest.java` | 6 | 319 | `e0f16f5417b61e801ed23803616450e90810853ec7588a49714b43040d809f93` |
| 13 | `.../dto/MerchantDeleteApprovalRequest.java` | 5 | 225 | `2516fb1d78e082fe89222a41f7178dde4b40d749c5ed2c2e712bbb14a74112c4` |
| 14 | `.../dto/MerchantDeleteRequest.java` | 5 | 214 | `4898e50f2150e27c73d6d400b317d792f02c342a0e95a8b3dadbef5a7ebc8f45` |
| 15 | `.../dto/MerchantProfileRequest.java` | 41 | 1800 | `7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1` |
| 16 | `.../dto/ProjectRequest.java` | 26 | 878 | `6ac09b14c1766bebd6df69fbf5579ccf602959d10179b497ff90ea1b3f521108` |
| 17 | `.../dto/SpaceRequest.java` | 21 | 525 | `63857df14972fd6904a32fc7aade0de49b88ec3b946df493f61086c77d1ab7aa` |
| 18 | `.../dto/SpaceTypeRequest.java` | 16 | 457 | `7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34` |
| 19 | `.../dto/StatusChangeRequest.java` | 11 | 231 | `75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f` |
| 20 | `.../dto/VenueBusinessHoursRequest.java` | 22 | 700 | `6347c6dfe087be2310fe5f618a0ac27e188f340d1af84add2ab831e98cb333f1` |
| | **合计** | **3369** | **180541** | |

三个大实现文件按结构完整阅读：`JdbcPlatformRepository` L24–1338（CRUD、COUNT/LIMIT、占用、行锁、未结束绑定、项目过滤重载、审批链持久化、PII 加解密）；`PlatformService` L34–1173（五类列表隔离、建档/补录、绑定未来生效/财税闸、空间类型恢复/审批、非负与中文错误、USB 识别）；`PlatformController` L28–296（权限一次检查、补录入口、脱敏、空间类型 DELETE 拒绝）。`PlatformRepository` L7–204 同步读完。

直接依赖（只读、不扩大模块清单）：`ProjectRecord`、`dto/ProjectAssociationRequest`、`V427__m009_project_profiles_and_associations.sql`、`AuthorizationService.isMasterAuthority`（对齐 `auth/SessionIssuer` 三常量）、`LeasingService.autoCreateMerchantForSign`/`createFourPartyBinding`、`AccessService.changePositionTemplateLifecycle`。未扩审未轮到模块。

---

## 2. 20 文件逐一覆盖

分类顺序：冲突 → 细化 → 技术性 → 添加。无新添加项。

| # | 文件 | 为什么必须存在（删掉则哪条 HTML 做不到） | 声称是否被执行 | 未验证前提 | 分类 | 覆盖结论 |
|---|---|---|---|---|---|---|
| 1 | BusinessEntityRecord | PGM-018/019 主体档案最小字段 | 创建写入 + JDBC 映射 | 无页面 | 细化 | 无缺陷 |
| 2 | BusinessEntityBindingRecord | BEQR-143/PGM-026 四类主体绑定 | 未来/到期/历史重开路径在服务层 | 无调度器 | 细化 | 无缺陷 |
| 3 | MerchantRecord V0.04 | PGM-026 商户档案 + 038 `projectId`；旧 19/25 参构造兼容 | JDBC 补录不改其它列（`to_jsonb - project_id` 差集空） | 不自动回填历史 | 细化 | 无缺陷 |
| 4 | MerchantDeleteDecision | BSM-004 删除审批结果 | 服务返回 approved/rejected | 无新审计系统 | 细化 | 无缺陷 |
| 5 | JdbcPlatformRepository V0.08 | 持久化与 COUNT 隔离 | 五类 scoped COUNT 与页同一 WHERE；空/platform-company/missing total=0 | 无参分页重载仅测试/兼容，生产 HTTP 不走 | 细化 | 见 §5.3 观察 |
| 6 | PlatformController V0.02 | REST + 功能权限一次检查 | 读端 `requirePlatformScope`；补录入口；脱敏在 controller | 空间类型审批端点未再挂功能权限码，服务层 USB 闸 fail-closed | 细化 | 无越权 |
| 7 | PlatformRepository V0.06 | 仓储缝；scoped 重载 + 空项目不退全表；`listMerchantsByBusinessEntity(projectId, entityId)` | 默认内存过滤与 JDBC 覆写对照 | `saveSpaceType` 注释「按 type_code upsert」描述仓储方法（存档/恢复走同一 upsert），不是请求层更新承诺 | 细化 | 无缺陷 |
| 8 | PlatformService V0.11 | 写口校验、隔离、绑定、恢复、中文错误 | 见 §4 十八项 | 集团视图按 HTML「未来」+038 不含 | 细化 | 见 §4 |
| 9 | package-info.java | 编译单元 | 仅 `package` 声明 | — | 技术性（053） | 无业务发明 |
| 10 | BusinessEntityBindingRequest | 绑定写入 | 创建绑定 | 无财税审批流（040 边界） | 细化 | 无缺陷 |
| 11 | BusinessEntityRequest V0.03 | 主体写入 + 可选 projectId；旧构造兼容 | USB 显式项目存在性；普通=当前项目 | — | 细化 | 无缺陷 |
| 12 | MerchantArchiveRequest | BSM-004 存档理由 | 存档路径 | 存档≠退出 | 细化 | 无缺陷 |
| 13 | MerchantDeleteApprovalRequest | BSM-004 审批 | 审批路径；驳回事件 `merchant.delete_rejected` | USB 直删例外保留 | 细化 | 无缺陷 |
| 14 | MerchantDeleteRequest | BSM-004 申请理由 | 申请路径须先存档 | — | 细化 | 无缺陷 |
| 15 | MerchantProfileRequest V0.02 | 商户档案 + 可选 projectId；旧构造兼容 | 更新禁迁项目；新建三主体可空但若给则校验 | 普通更新不能 null→项目 | 细化 | 无缺陷 |
| 16 | ProjectRequest V0.01 | PGM-020 三项可空 + 旧 10 字段构造 | 创建读回负责人/地址/范围；无 PUT /projects | 无旧项目编辑页 | 细化 | 无缺陷 |
| 17 | SpaceRequest | PGM-017 空间归属 | projectId 必填、等于当前、父空间/商户同项目 | 无楼层图页（M015） | 细化 | 无缺陷 |
| 18 | SpaceTypeRequest V0.01 | BSM-004/PGM-005 空间类型字典新建 | 注释现与 `createSpaceType` L551–558 拒重复、固定 `active` 一致 | DTO 仍有 `status` 字段但服务忽略并固定启用（既有测试 `createSpaceTypeIgnoresRequestedDisabledAndStaysActive`） | 细化（057 已修） | 无残留误导承诺 |
| 19 | StatusChangeRequest | 绑定 change | 停用拒绝/历史不重开 | 无新状态机 | 细化 | 无缺陷 |
| 20 | VenueBusinessHoursRequest | PGM-015 场地营业时间写入 DTO | 本模块仅 DTO+Controller 转调 `VenueBusinessHoursService` | 实现主体不在本 20 文件，未扩审 | 细化 | 无本模块新缺陷 |

---

## 3. 87 映射逐项适用性与证据

判据全文只引用锁，不重抄。适用性：本模块文件是否承载该锚点；不承载则核有无冲突发明。依据类型：1=HTML 原文；4=命令（哈希/摘录比对）；2=测试产物（封存，见 §7）。

### 3.1 本模块直接承载（须与实现同口径）

| ID | 锁标题 / HTML 原句要点 | 适用性 | 现行证据 |
|---|---|---|---|
| BEQR-143 | 「经营对象、运营主体、合同主体、结算主体…同一经营对象下唯一绑定」HTML:226 | 适用 | `createBusinessEntityBinding` + `requireUnfinishedBindingSlot`；未结束槽含 active 与 inactive+`effectiveTo IS NULL` |
| BEQR-144 | 物理项目与运营公司多对多，不是固定隶属链 HTML:227 | 适用 | `business_entity_projects` PK (entity, project)；`associate` `ON CONFLICT DO NOTHING`；不恢复已删父链 |
| BEQR-145 | 城市项目公司资产职责 HTML:228 | 适用（角色词表） | `BUSINESS_ENTITY_ROLES` 含 `project_company`；职责细则非本模块页面 |
| BEQR-146 | 运营公司职责 HTML:229 | 适用（角色） | `management_company`；不发明酒吧/游乐经营规则 |
| BEQR-147 | 主要收入归属 HTML:230 | 不适用经营分账；无冲突 | 本模块不设收费/分账默认值 |
| BEQR-148 | 合作模式六类 HTML:231 | 适用 | `MERCHANT_COOPERATION_FORMS` 六值；空=未定；中文报错无英文括注 |
| BEQR-152 | 「所有数据默认按项目隔离，普通岗位不得跨项目查看」HTML:235 | 适用 | 五类 `*Paged(currentProject())`；COUNT 同 WHERE；USB 列表仍按真实项目，`platform-company` total=0 |
| BEQR-154 | 综合体项目状态六态 HTML:237 | 适用 | `PROJECT_STATUSES`；中文「草稿/待启用/已启用/暂停/终止/归档」 |
| BEQR-155 | 商户状态含终止缓冲 HTML:238 | 适用（退出闸） | 018：普通新建/他态更新拒 `terminated`；不新建缓冲调度 |
| BEQR-166 | 合同审批链 HTML:255 | 邻域：仓储承载 | JDBC `findDirectSuperior`/`saveChainNodes`（049 登记）；本模块不新造合同流程 |
| BSM-001 | 系统管什么 | 邻域总述 | 治理后端是盘子，不发明票务/酒吧规则 |
| BSM-003 / BSM-004 | 配置可存档、删除要审批；USB 本人不经审批 HTML:566–577 | 适用 | 商户/空间类型存档→申请→最高权限审批；占用检查；USB 直删；自审禁止 |
| BSM-007 | 平台管理员≠最高权限；USB 全平台一人 HTML:587 | 适用 | `isMasterAuthority` 要求 `master_authority`+三常量；`platform_admin` 不是 USB |
| BSM-009 / BSM-028 / BSM-029 | 基础管理主要功能/和谁有关 HTML:596,698 | 适用 | 项目/商户/空间/主体/绑定入口；合同/员工细则在他模块 |
| BSM-072 / BSM-074 | 商户在授权范围内；不看平台全局 HTML:1308 | 适用 | `requirePlatformScope` 拒 merchant-scoped；列表不退全表 |
| PGM-001 / PGM-002 / PGM-003 | 治理底座与模块入口 | 适用总述 | Controller `/api/v1/platform/*` |
| PGM-004 / PGM-017 / PGM-018 / PGM-019 | 综合体/集团；当前一项目；多对多 HTML:412–424 | 适用 | 对象带 project；公司 M:N；集团视图未实现（HTML「未来」，038 不含） |
| PGM-005 | 空间和点位 HTML:347 | 适用空间/类型 | `createSpace` 必填 projectId；字典新建拒重复 |
| PGM-006 / PGM-026 | 商户和主体；四类主体唯一绑定；财税主体不得普通改 HTML:352,492–498 | 适用 | 非空三主体不一致拒绝；生效绑定普通停用拒绝；未来不投影 |
| PGM-020 | 项目档案负责人/地址/经营范围 HTML:440–443 | 适用 | 013 三字段可空；旧构造；无编辑页 |
| PGM-021 | 避免多综合体数据混在一起 HTML:452 | 适用隔离 | 同 038 |
| PGM-027 | 商户不能看平台全局和其他商户 HTML:507–510 | 适用 | 平台 scope + 项目过滤 |
| PGM-040 / PGM-041 | 单一功能授权；普通岗位无跨项目查看 HTML:643–654 | 适用 | Controller `requirePermission`；服务再按项目；USB 识别不冒充 |
| PGM-066 | 界面全中文；对象带综合体归属已拍板 HTML:951,965 | 适用 message/归属 | 用户 message 中文；内部 code 英文保留（047） |

### 3.2 映射继承、本 20 文件不承载（无本地冲突、不扩模块）

BEQR-063 酒吧高危动作、BEQR-074 隐私桌台、BEQR-149 职级三十档、BEQR-150 十一端、BEQR-151 最终权限公式、BEQR-153 终端开关、BEQR-156 合同状态、BEQR-157 员工状态、BEQR-158 设备状态、BEQR-159 权限状态、BEQR-189 管理费方式、BSM-005 分步引导页、BSM-006 动态二维码、BSM-114 各类入口（本模块无考勤实现）、BSM-115 子系统相互关系、BSM-120 已确认规则清单、PGM-007 合同准入、PGM-008 员工手机、PGM-009 权限模板装配、PGM-010 终端开关、PGM-011 设备、PGM-012/048 商户侧门户、PGM-013 开放接口、PGM-014 留痕复核专章、PGM-015 楼宇运营（营业时间 DTO 仅转调）、PGM-016 应急隐私、PGM-031 经销商资金、PGM-035 提成、PGM-042 职级薪资、PGM-043 外包审计、PGM-044 桌台挂单、PGM-060 会员生物、PGM-065 连接关系总述。

以上按冲突→细化：有 HTML 上位则细化，无本地发明、无工程缺陷。BSM-114/120 锁摘录与现 HTML 全文不等，见 §5.3；**本模块不承载这两处，不改 HTML/锁、不新增判据**。

### 3.3 GEN-001…018

| ID | 本模块落点 | 结论 |
|---|---|---|
| GEN-001 | 无死代码必删项；048 零调用隔离通道保留 | 通过 |
| GEN-002 | 入口与注释：057 后 SpaceTypeRequest 与 create-only 一致 | 通过 |
| GEN-003 | 既有测试含可失败断言；本席未新写、未重跑（§7） | 通过（封存+源码断言） |
| GEN-004 | 输入：业态/状态/非负/存在性/项目归属 | 通过 |
| GEN-005 | 服务端授权；USB≠platform_admin；普通无全量 | 通过 |
| GEN-006 | 事务+行锁+未结束槽+软删占用 | 通过 |
| GEN-007 | 幂等键；绑定并发锁；字典 create 顺序拒重 | 通过；upsert 残留见观察 |
| GEN-008 | DTO/SQL/服务三字段与 project_id 可空一致 | 通过 |
| GEN-009 | 错误可检测；空间类型 DELETE 不伪成功 | 通过 |
| GEN-010 | PII 落库加密、读端脱敏 | 通过（050） |
| GEN-011 | executeWrite 对象 ID；删除理由进 metadata | 通过（043） |
| GEN-012 | COUNT+LIMIT 500 | 通过（051） |
| GEN-013 | 本模块无新构建脚本 | 不适用/无冲突 |
| GEN-014 | V427 顺序下一号；本席未改迁移 | 依赖只读 |
| GEN-015 | 无本模块 UI | 不适用；message 中文属 047/066 |
| GEN-016 | 057 注释已与行为一致；旧测试名 `createSpaceTypeUpserts…`、事件 `space_type.updated` **不扩为 057**（B2 已非阻断） | 通过 |
| GEN-017 | 分类顺序遵守；6 登记不删 | 通过 |
| GEN-018 | 本票依据 HTML/源码/命令，不引用席位旧结论作证据 | 通过 |

---

## 4. 18 已修 + 6 登记：现行实现核真

不因队列已「否」跳过。事实列仍是修复前描述；本表核**现状**。

| ID | 独立核查（现行行） | 结果 |
|---|---|---|
| 038 | `list*` 均 `repository.*Paged(currentProject(),…)`；JDBC COUNT 与页同一 WHERE（实体 EXISTS 关联表；商户 `project_id=`；项目 `id=`；空间 `project_id=`；绑定 EXISTS merchant.project_id）；空/`platform-company`/`missing` total=0；普通更新禁改 project_id；USB 补录 `assignMerchantProject` 仅 `project_id IS NULL OR =`；公司 `ON CONFLICT DO NOTHING`；`saveMerchant` ON CONFLICT **不更新** project_id | 真实修复仍在 |
| 039 | `createSpace` L689–703：projectId 必填、存在、等于当前；父空间同项目；merchant 经 `requireActiveMerchant`（含项目） | 真实修复仍在 |
| 040 | 生效绑定普通停用 L831–833 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`；`requireUnchangedFinancialSubjects` 非空三列不一致拒绝 | 真实修复仍在 |
| 041 | 未来 `effectiveFrom.isAfter(today)` → 存 inactive + effectiveTo=null，不 `syncMerchantEntities`；创建/激活都 `requireUnfinishedBindingSlot`；`lockMerchant` FOR UPDATE；Leasing `autoCreateMerchantForSign` 三主体传 null，随即 `createFourPartyBinding` | 真实修复仍在 |
| 031 | `effectiveTo != null` → `BINDING_PERIOD_ENDED` 不能重开；未生效提前动作 `BINDING_NOT_EFFECTIVE` | 真实修复仍在 |
| 042 | `unarchiveSpaceType` 接受 archived 与 disabled；pending_delete 不能用存档撤回 | 真实修复仍在 |
| 043 | `businessId` 识别 SpaceTypeRecord/MerchantDeleteDecision；`auditMetadata` 含 reason/decision；驳回事件 `merchant.delete_rejected` / `space_type.delete_rejected` | 真实修复仍在 |
| 044 | `completeSpaceTypeDeleteRequest` 只更新当前 pending；JDBC 测试保留 rejected+approved 两行历史 | 真实修复仍在 |
| 045 | 占用排除「inactive 且 from/to 均非空且 to>=from 且 to<=今天」；未来 inactive to=null 仍占用 | 真实修复仍在 |
| 046 | capacity/面积/租金 `signum<0` 拒绝；可空和 0 保持 | 真实修复仍在 |
| 030 | 新建已给的三主体走 `requireBusinessEntity`（存在+本项目关联）；可空 | 真实修复仍在 |
| 047 | 用户 message 无英文枚举括注；内部 code 仍英文 | 真实修复仍在 |
| 018 | 新建/他态更新拒绝 terminated；已是 terminated 且请求 status 空则保持 | 真实修复仍在 |
| 013 | ProjectRequest/ProjectRecord 三字段可空、旧构造、JDBC 列、V427、无 PUT 项目、无页面 | 真实修复仍在 |
| 054 | 仅 `changePositionTemplateLifecycle`：`if (!isMasterAuthority) requirePlatformAdmin`；审批仍要求 USB；共享 `requirePlatformAdmin` 未改 | 真实修复仍在 |
| 055 | 只读夹具边界（资金/员工测试类）；生产资金守卫未放宽 | 登记为测试夹具修复，源码无回退迹象 |
| 056 | `isMasterAuthority` 三常量；高职级 `currentSupervisorLevel` 封顶 2；USB=3 | 真实修复仍在 |
| 057 | SpaceTypeRequest L6 现句与 `createSpaceType` L551–558 `SPACE_TYPE_CODE_EXISTS` + 固定 `"active"` 一致；before 旧句已不在工作区 | 真实修复仍在 |
| 048–053 | 隔离查询带 projectId 且空不退全表；审批链 JDBC；PiiCipher；分页；DELETE 拒绝；package-info 仅声明 | 维持登记否，不删 |

054–056 时序：代码与测试先落地，操作员后确认「如果不是修改 html，那么修复没有问题」。本席按该历史记录，**不**写成事先批准。

---

## 5. 三线审计

### 5.1 第一性原理

1. **为什么必须存在**：删掉这 20 文件，PGM-017「所有基础对象都要带综合体项目归属」、PGM-018 多对多、PGM-020 项目档案三字段、PGM-026 四类主体绑定与财税主体不得普通改、PGM-041「普通岗位没有跨项目查看权」、BSM-004 存档/审批删除，均无法落地。答得上业务上位需求。无页面、无集团汇总、无新调度/新财税审批系统——与已批准最小边界一致，不是缺口。
2. **声称是否被实际执行**：现行源码路径与既有可失败断言（§4、§7 封存 239）对齐。B3-r3 只改注释+V0.01，去注释 token 与 after 一致，**不构成新的运行声称**。本席本次**未重跑** Maven（见 §7）。
3. **未验证前提（如实）**：集团下钻/KPI 分层（PGM-020/021 未来条款）按 038 不含未实现，不当作已交付；空间类型字典仍是平台级配置、未按项目切片（038 五类列表未包含字典，HTML 也未要求按综合体复制字典）；无参 JDBC 分页重载仍存在，生产 HTTP 只走带 projectId 的服务重载（graft：生产入边为服务 scoped 调用）。以上不构成与冻结 HTML 的冲突，也不新造判据。

### 5.2 顺从式

假定实现意图正确，逐文件、逐 87 项核对输入/状态/输出/错误/权限/持久化。结论：18 已修行为仍在；6 登记仍无独立工程缺陷；057 注释与 create-only 一致。无新添加项。

### 5.3 对抗式（grilling 结构自问自答；读了 grilling SKILL，不等用户、不派子代理）

| 反例 | 结果 |
|---|---|
| 甲项目岗位改 page size 是否把乙项目计入 total？ | COUNT 与页同一 WHERE；JDBC 五类 total 甲=1，乙不进 |
| USB 持 platform-company 是否全表？ | `currentProject()` 非真实项目 → null → SQL 无行，total=0 |
| platform_admin 同 ID/用户名冒充 USB | `isMasterAuthority` 要求 `accountType=master_authority` |
| 普通高职级代替 USB 批删除 | 商户/空间类型/岗位模板审批均 `MASTER_AUTHORITY_REQUIRED`；职级封顶 2 |
| 普通更新 projectId=当前以认领历史 null | `requireMerchant` 先项目不匹配；显式改归属也禁 |
| 已有项目的补录改到乙 | assign 返回 0 → `MERCHANT_PROJECT_ALREADY_ASSIGNED`；其它列 JSON 差集为空 |
| 专用补录是否改三主体/状态？ | SQL 只 SET project_id；条件 null 或同值 |
| 未来绑定保存即改商户三列 | 不投影；leasing 建档三列 null |
| 再插一条未来绑定 | `ACTIVE_BINDING_EXISTS`；FOR UPDATE 行锁 |
| USB 建绑定改已有非空三主体 | `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` |
| 提前激活/结束未来绑定 | `BINDING_NOT_EFFECTIVE` |
| 重开已结束历史 | `BINDING_PERIOD_ENDED` |
| 负数租金/面积/容量 | `VALIDATION_FAILED` 中文「不得为负数」 |
| 直接 DELETE 空间类型 | `SPACE_TYPE_DELETE_REQUIRES_APPROVAL` |
| 只失效历史绑定能否软删 | 占用公式排除已结束 inactive，未来预约仍拦 |
| 重复 typeCode 走「更新」？ | 服务先拒 `SPACE_TYPE_CODE_EXISTS`；忽略请求 status，固定 active |
| 旧事件名/旧测试名是否还构成 057？ | 否。`space_type.updated` 与 `createSpaceTypeUpsertsDictionary…` 为 B2 已明确非阻断；057 只改注释+V0.01 |

无成立的越权/提前生效/全量泄露/注释再失真反例。

**观察（不入队、不改 HTML/锁、不新增判据）：**

1. BSM-114 锁摘录私人手机行「考勤打卡」vs 现行 HTML:1865「查看考勤」；BSM-120 锁页脚日期 2026-09-08 vs 现行 2026-09-10。完整 69 业务片段 67 全文匹配。本模块无考勤/页脚实现。
2. JDBC `saveSpaceType` 仍 `ON CONFLICT (type_code) DO UPDATE`，供存档/恢复改 status；create 路径顺序检查后插入。顺序重复已被测试挡住。不把仓储 upsert 扩成 057 行为改动。
3. `approveDeleteSpaceType` Controller 未再挂 `requirePermission`；服务 `requirePlatformScope` + USB 闸 fail-closed，与 BSM-004「持授权 U 盘最高权限审批」一致，不是旁路。

---

## 6. 三线问题全集

**无。** 无 GR-01 及后续临时编号。无冲突待删、无新细化缺陷、无新添加项、无锁外主张、无待答 questions、无 HTML 修改申请。6 项登记否保留。

---

## 7. 测试证据：封存 vs 本次

**本次未重跑 Maven / JDBC / Controller 套件。** 理由：B3-r3 仅注释与文件版本，去注释 token 与 after 一致，无运行逻辑或测试改动；用户明确「不必无理由重复相同套件，不新写测试」。无新疑点需要 isolation 验证。

**封存证据（B5-r2，不冒称本次实跑）：**

- Codex 283 / Grok 239 已实跑成功，含 69 服务 / 23 JDBC / 14 Controller 与直接依赖。
- Grok B5-r2 本次完整报告（`M009-b5-r2-grok-raw.md` 最后哈希行起）记录：`JAVA_HOME` JDK21；只解析三个 `COMPLEX_DATABASE_*`；库名 `complex_test`；Flyway schema 427；十类 Surefire **239 / 0 / 0 / 0**，`MAVEN_EXIT=0`。

**本次只核源码断言仍在（类型 4=文件存在性，不是测试进程输出）：**

| 类 | 源码 `@Test` 方法数（`@TestConfiguration` 不计） | 与封存 Surefire 是否同数 |
|---|---:|---|
| PlatformServiceTests | 69 | 同 |
| PlatformControllerTests | 14 | 同 |
| PlatformJdbcIntegrationTests | 23 | 同 |
| AuthorizationServiceTests | 24 | 同 |
| LeasingServiceTests | 11 | 同 |
| LeasingSignAtomicityJdbcIntegrationTests | 3 | 同 |
| M006SecurityRegressionTests | 39 | 同 |
| M010RegressionTests | 44 | 同 |
| BarStoredValueFundChainJdbcIntegrationTests | 8 | 同 |
| StoredValueNotificationIntegrationTest | 4 | 同 |
| **合计** | **239** | 与封存 Grok 239 同数 |

抽检仍存在且能失败的断言：`createSpaceTypeRejectsExistingCodeEvenWhenActive`、`createSpaceTypeIgnoresRequestedDisabledAndStaysActive`、`scopedSqlFiltersAllFiveTotalsAndSupportsCompanyManyToMany`、`projectOnlyBackfillPreservesEveryOtherDatabaseColumnAndLegacyNulls`、`concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot`、`completingSpaceTypeRequestKeepsPriorRequestHistory`。

未声称全仓；未新写测试；主仓业务文件未改。

---

## 8. 技能（V0.17 收窄，只记实际读取/使用）

| 技能 | 用法 | 缺口 |
|---|---|---|
| using-superpowers | 开工读 SKILL；用户指令优先于技能派席 | 无 |
| using-arc | 读 SKILL；本任务是指定席乙审计，不走 Arc audit 派评审 | 无 |
| graft | 实际调用 skeleton（Service/JDBC/Controller/Repository）与 callers（paged/isMasterAuthority/assignMerchantProject） | 只读 |
| grilling | 读 SKILL.md；对抗式用其追问结构自问自答 | 本体「等用户/派子代理」作废 |
| long-running-background-tasks | 读 SKILL；本次无长任务 | 未启动 monitor |
| 160-audit-prompt V0.17 | 角色/三线/双席合取/raw 追加/哈希首行 | 按席乙执行 |
| runbook V0.02 | 三线问法与产物豁免 | 无 |
| verification-before-completion | 运行声称须有证据；本席区分封存 239 与本次未重跑 | 未冒称重跑 |
| tdd / test-driven-development | B5 不写测试 | 未新写 |
| ponytail / ponytail-review | 未整文件精读；048–053 与无参分页不因零调用删除 | 未冒充调用 |

明确不用（提示词禁止）：子代理、MCP、第三席、修复、提交、推送、部署、新产物。未调用 dispatching-parallel-agents、subagent-driven-development、research、Arc audit 评审席。

---

## 9. 结论

- 20 文件全部覆盖，行数 **3369**，与 after `39b7324829f44339c8c2afb4d2800dfce674ed2c` 字节相等。
- 87 映射逐项核对：业务摘录首片段 69/69、全文 67/69；两处差异如实记录且不入队。
- 18 项已修独立复验成立；6 项登记否不删除；057 注释与 create-only/固定 active 一致。
- 054–056 历史为先改后确认，不倒填事先批准。
- 三线问题全集：无。
- 测试：本次未重跑；封存 B5-r2 Grok 239 全绿；源码断言计数仍 239。

**本席 B5-r3 审计结论：PASS。**

本票不是模块 PASS，也不是双席合取。须席甲独立封存本次 B5 票且双 PASS、且闸 1 四值全等后，才允许把 ledger 改 PASS。本席未改 findings/ledger/锁/源码/三 HTML。
