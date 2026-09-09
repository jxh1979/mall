CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r2 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r2 PASS、B3 自报或历史 B1/B5-r1 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M009 同一 20 文件；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 2/3；LOCK-AMEND-COUNT 1
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 全文一行同值
- 映射：`M009 → [BEQR-063 … PGM-066]`，COUNT=87 unique=87 MISSING=[]；定义与 HTML 出处均从锁自读，不新造判据
- 正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；开工时工作区 after tree=`bea6236755d5114c8e38007e07f0f3a75717b341`（临时索引 `GIT_INDEX_FILE=mktemp-dir/index` 下 `git add -A && git write-tree`）；B3 修复未提交，审计对象=当前工作区 20 文件（含 B3-r1 与 B3-r2）
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m009-b5-r2-verify`，结束清理）
- 未读取：`M009-b5-r2-self-raw`（当前亦不存在）。B4-r2 / B5-r1 raw 与 findings V1.23 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V1.23 顶部「026–028已修转否，进入B5-r2」；001–012、014–016、026–028 已修；013/017–025 与 029–035 仅登记；M009-017 环境红（complex_test Flyway 409 vs 源码最新 399）不是源码缺陷
- 技能（V0.17 收窄，按实际存在与已读执行适用意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–012/014–016 与 026–028 修复后行为独立复验成立；013/017–025 与 029–035 维持仅登记（017 本席复现为环境红）。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。

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
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V2.10；2834 行 / 404507 字节 / SHA `07c960826274615378d97a400861436d9035d65dcaeeb5185b5df0b76c5c226d` |
| findings | V1.23；3230 行 / 475359 字节 / SHA `6061e17cf5f74d359f5968efa1c83216cbb613b3920a78282df21bf3d4273119` |
| 映射行抽出 | 87 条、无重复、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | checked 87 miss 0（GEN 无 HTML 行；其余首条摘录落在引用行邻域） |
| 计划 FILES | 20 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 2476（切分时）；当前工作区 `wc -l` 合计 **2644**（B3-r1/r2 增行，不改计划凑数） |
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
| 8 | `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 863 | 42578 | `2450c955ac3379c4780d37c811d90aeb5f32fb9e4a3373ab7316cf1fa3515092` |
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

合计行 2644、字节 136253。Jdbc 1112 / Service 863 / Controller 236 均整文件读完（Jdbc 分段 L1–400 / L400–790 / L790–1112，含 L683+ `SupervisorReviewRepository` 实现），未按标题抽样。Jdbc 审批链方法按「文件已有职责不因模块名裁掉」纳入三线，但不把主管复核 Service 扩成全量审计。

相对 B5-r1 工作区指纹：MerchantRecord / Jdbc / PlatformRepository / PlatformService 因 B3-r2 改变；其余 16 个文件 SHA 与 B5-r1 实算相同。20 文件内 `硬删`/`物理删除`/`parentEntityId`/`leasing_auto`/`expireBar` 命中数均为 0。

必要只读依赖（验证本模块调用/字段，不扩审模块）：

| 路径 | 行 | 字节 | SHA256 | 本轮用途 |
|---|---:|---:|---|---|
| `LeasingService.java` | 453 | 27927 | `169d2fcf52b1c6b933eeac2686e6a700351d9b1b5d773cfcdcf47c71c604cb0b` | L288–292：003 伴生，合作方式 `null` |
| `OrganizationMerchantPage.tsx` | 480 | 29371 | `76571cb692a4bc75ccd26f46be02e8c07c4c72806c9339feb55f91fb7144abe4` | L33–41 六类中文 option |
| `OrganizationMerchantPage.test.tsx` | 261 | 14985 | `99a3b8201d9d7a401a4013a27a5e76a325919a759c93b1ea34b0b04ac3e77f8e` | 12/12 含六类中文、拒收费模板码 |
| `PlatformControllerTests.java` | 351 | 15915 | `cbaec207daf3fc89fd72d42aa3fa42cccbefa7beb2e4eb16294685658cfeeb78` | 读端平台范围 |
| `PlatformJdbcIntegrationTests.java` | 457 | 20482 | `2b39492f831860f1960d9e25c22e939716fa26c935acf54d7d1675132ebd5057` | 027 主键空间；010 软删可查；017 环境红 |
| `PlatformServiceTests.java` | 798 | 38738 | `d8b31418687fb7110fbf00d37a29ebb148396dfddedceac403d569aec80ad3b9` | 001–009/014/028 等单元 |
| `LeasingSignAtomicityJdbcIntegrationTests.java` | （夹具 L116–121） | — | — | 006 伴生：seed 插入 `business_entities` |

另只读：`V318` `parent_entity_id` 可空列（M010）；`AuthorizationService.requirePlatformScope` / `isMasterAuthority`；`AuditedOperationExecutor.executeWrite`；`PiiCipher`。

---

## 2. 全量三线覆盖表

| # | 文件 | 第一性原理 | 顺从式 | 对抗式 | 覆盖 |
|---|---|---|---|---|---|
| 1 | BusinessEntityRecord.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 2 | BusinessEntityBindingRecord.java | 已审 | 已审 | 已审 | 全文 19 行 |
| 3 | MerchantRecord.java | 已审 | 已审 | 已审 | 全文 90 行 |
| 4 | MerchantDeleteDecision.java | 已审 | 已审 | 已审 | 全文 9 行 |
| 5 | JdbcPlatformRepository.java | 已审 | 已审 | 已审 | 全文 1112 行（OVERSIZE 分段读完） |
| 6 | PlatformController.java | 已审 | 已审 | 已审 | 全文 236 行 |
| 7 | PlatformRepository.java | 已审 | 已审 | 已审 | 全文 134 行 |
| 8 | PlatformService.java | 已审 | 已审 | 已审 | 全文 863 行 |
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

删除测试：删掉这 20 文件，BSM-009/PGM-006/PGM-026「建立商户档案、四类主体绑定、综合体/空间」在后台将无写入面。答得上上位。B3 后错误枚举/错误删除语义/单父链写入面已从本模块接口拿走；B3-r2 后再把「硬删」注释、空间主键存在性、已下线商户被新业务引用三处对齐 BSM-004/GEN-006/GEN-012。

| 文件 | 为何必须存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| BusinessEntityRecord | 法律/经营主体主数据 | 创建经 Service 白名单角色+主体状态；Jdbc INSERT 不写 `parent_entity_id`；记录无 parent 字段 | DB 列仍在（V318，M010）；列 `is_nullable=YES`，本模块 INSERT 不碰它 |
| BusinessEntityBindingRecord | 四类主体一组关系 | 创建时拦第二份 active + 存在性 + 商户未下线；激活回写商户三列 | 停用绑定不清理三列（008/031 最小边界） |
| MerchantRecord | 商户档案+存档删除轴+电话掩码 | 25 参更新保留 archived_*/delete_*；`withEntities` 投影三列；`withMaskedPhones` 在 Controller；L27 注释已改为软删除标记（026） | 19 参构造仍把存档默认 none（创建/测试用） |
| MerchantDeleteDecision | 审批结果 | javadoc 为软删标记；`deleted=true` 对应 `delete_status=approved` | 内存 fake 仍 `remove`（M011/010 分叉） |
| JdbcPlatformRepository | Postgres 适配+审批链仓储 | JDBC：软删行 `findById` 仍在、`includeArchived=true` 可见；PII 加密回读；绑定 unique 部分索引拦第二份 active；`findSpaceById` 按主键（027 测试绿，listSpaces 仍 500） | 1 红=Flyway 409≠399（017）；`countMerchantReferences` 动态 COUNT；L683+ 审批链本模块 API 不调用 |
| PlatformController | HTTP 面 | 单元 10/10：无权限 403、缺幂等 400、读端 `requirePlatformScope` 403 | `createSpaceType` 控制器不预检幂等键（022）；写端平台范围在 Service；`ApiException` 仅 import（034） |
| PlatformRepository | 仓储接口/默认内存切片 | `listMerchants(true)` 全量；`deleteMerchant` 默认抛、Jdbc/fake 覆写；`businessEntityExists`/`projectExists` 按主键；默认 `findSpaceById` 仍滤 `listSpaces`，Jdbc 已覆写 | 无过滤 `listMerchants()` 仍被跨模块调用（020） |
| PlatformService | 写路径/校验/审计 | 单元 30/30：更新保档、404 更新、合作六类、状态六态、角色白名单、幽灵绑定、空间存在性、绑定投影、非最高权限拒批、最高权限直删、分页 500、已存档商户拒新引用（028） | 创建商户仍可写未校验存在的三主体列（030）；空间 status 无闭集（029）；无 TicketingService |
| package-info.java | 包声明 | `package com.complex.platform.governance;` | 无行为 |
| BusinessEntityBindingRequest | 绑写入参 | 四 ID + 生效日 + 理由；服务层存在性+未下线 | `operatorNote`/`evidenceAttachmentIds` 服务层不读（023） |
| BusinessEntityRequest | 主体入参 | 无 parent；角色/状态在服务层闭集 | 无 |
| MerchantArchiveRequest | 存档理由 | `reason` 存档必填；注释与 BSM-004 一致 | 取消存档可空 |
| MerchantDeleteApprovalRequest | 审批 | approved/rejected；最高权限闸在服务层 | `comment` 不落库（021） |
| MerchantDeleteRequest | 删除申请理由 | 须先存档；最高权限走直删 | 无 |
| MerchantProfileRequest | 商户档案 | 业态/合作/状态在服务层闭集 | 三主体列创建时可带、更新不覆盖 |
| ProjectRequest | 项目/场地 | 状态六态；merchantId 存在且未下线 | 无负责人/地址/经营范围（013/018） |
| SpaceRequest | 空间 | space_type 须字典 active；project/merchant/parent 存在性；merchant 未下线 | status 默认 active、无闭集（029） |
| SpaceTypeRequest | 空间类型字典 | upsert by type_code | 控制器不预检幂等键 |
| StatusChangeRequest | 绑定状态变更 | action∈active/inactive/suspended；报错中文+内部码 | 英文内部键属标识符例外 |
| VenueBusinessHoursRequest | 场地营业时间 DTO | Controller GET/POST 存在并 `requirePlatformScope` | 保存语义在其它 Service；本 DTO 无草案/审批字段 |

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
| BSM-004 | 存档/软删/先存档再审批/禁自审/占用检查/填理由/可查可恢复/下线不再被新业务选用。更新保档；软删 UPDATE；includeArchived 含 approved；最高权限直删；非最高拒批；禁自审；三创建路径 `requireActiveMerchant`；026 三处注释已改软删口径 | 细化（行为已对齐）；批量存档页/清理工具无实现面；变更绑定激活未纳入 028 三处（见观察） | 否（已修 001/009/010/011/026/028；流程页 018） |
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
| GEN-002 | 单元 30+10+11 绿；JDBC 12/13 绿（027 绿）；签约原子 2/2 绿；页面 12/12 绿 | 技术性 | 否（017 环境红单独登记） |
| GEN-003 | 关键分支有能失败断言（非法合作/状态/角色/幽灵绑定/非最高权限/更新 404/软删可查/501 条空间按 id/已存档拒引用） | 技术性 | 否 |
| GEN-004 | 业态/合作/主体状态/项目状态/角色/绑定对象/空间归属均有校验；新引用拦已下线商户 | 技术性 | 否（已修 002–007/028）；创建路径三列存在性见 030 |
| GEN-005 | 各写入口权限码+平台范围；删除审批最高权限；读端平台范围 | 细化 | 否（已修 009/015） |
| GEN-006 | 更新保档；绑定存在性；绑定激活投影三列；软删不抹行；空间按主键 | 细化 | 否（已修 001/006/008/010/027） |
| GEN-007 | 写路径幂等执行器；active 绑定 unique 索引；`createSpaceType` 空键服务层仍拒 | 技术性观察 | 否（022 仅风格） |
| GEN-008 | 档案更新不覆盖三列；分页 pageSize=500；合作码前后端同词表 | 技术性 | 否（已修 003/008/014）；内存 fake 分叉归 M011 |
| GEN-009 | 错误码中文为主；括号内内部码 | 观察（023） | 否 |
| GEN-010 | 电话 PII 仓库加密（JDBC 测试绿）；动态 COUNT 表名来自 information_schema | 技术性 | 否 |
| GEN-011 | executeWrite 记成功/失败事件；存档/删除有事件名 | 细化 | 否 |
| GEN-012 | 列表 size cap 500；Jdbc paged LIMIT/OFFSET；空间存在性不再被 list LIMIT 截断 | 技术性 | 否（已修 027） |
| GEN-013 | 无本模块构建脚本 | 不适用 | 否 |
| GEN-014 | 无迁移文件（M010） | 不适用 | 否 |
| GEN-015 | 无本模块 UI；API JSON 用内部键 | 观察（内部标识符） | 否 |
| GEN-016 | 010 四处 + 026 三处注释均已改软删口径；20 文件 `硬删`/`物理删除` 零命中 | 技术性 | 否（已修 010/026）；M011 测试注释仍有「拒硬删」见观察 |
| GEN-017 | 本批无把发明标成技术性；025 spv/operation 转 M010 | — | 否 |
| GEN-018 | 结论落到 HTML 原文、单元/JDBC/页面输出、psql 只读；017 失败如实 | — | — |
| PGM-001 | 治理底座/全中文/金额/先成功。本模块是底座写入；金额精度无本文件计算 | 细化；报错观察 | 否 |
| PGM-002 | 先看对象再合同。本模块只建对象，不卡合同 | 无合同闸实现面 | 否 |
| PGM-003 | 模块入口卡片。无 HTML/UI | 不适用 | 否 |
| PGM-004 | 综合体和集团卡片 | 不适用（文案） | 否 |
| PGM-005 | 空间点位卡片。空间 API+存在性按主键 | 细化 | 否（已修 007/027） |
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

映射 87/87 均有覆盖记录。添加项 0（025 仍转 M010，本模块 20 文件不产生 spv/operation）。无锁外判据。

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
8. **档案改三列（008）**：更新传入 `be_other` 等，读回仍 `be_1`；创建绑定后三列=绑定值。**ok**。
9. **非最高权限审批（009）**：`MERCHANT_DELETE_APPROVER_NOT_MASTER`，状态仍 pending；最高权限申请直达 `approved`。**ok**。
10. **物理删除（010）**：JDBC `approvedDeletedMerchantStaysQueryableWithIncludeArchived`：`findById` 在、includeArchived 含、默认列表不含。`deleteMerchant` SQL=`UPDATE delete_status='approved'`。**ok**。内存 fake 仍 remove（M011）。
11. **存档即撤场停票（011）**：`PlatformService` 无 `TicketingService`/`expireBarSingleStoreTickets`。**ok**。
12. **parent 链（012）**：20 文件 `parentEntityId` 零命中；Jdbc INSERT 列清单无 parent；DB 列可空。**ok**。
13. **分页（014）**：`listSpaceTypesPageSizeCoversAllItems` pageSize=500。**ok**。
14. **商户读平台列表（015）**：`listingMerchantsRequiresPlatformScope` 403 `PLATFORM_SCOPE_REQUIRED`（日志实见「无权查看平台级经营分析」）。**ok**。
15. **死代码（016）**：`requireDate`/`readObjectMap` 零命中。**ok**。
16. **残留硬删注释（026）**：20 文件 `硬删`/`物理删除` 零命中。MerchantRecord:27 / Jdbc:198 / PlatformRepository:54 均为软删除口径。**ok**。
17. **空间 LIMIT 500 截断存在性（027）**：Jdbc `findSpaceById`=`WHERE id=:id`；`findSpaceByIdIsNotTruncatedByListLimit` 插 501 条后最早 id 仍 present，同时 `listSpaces().size()==500`。**ok**。
18. **已存档商户被新引用（028）**：`archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 项目/空间/绑定均 `MERCHANT_ARCHIVED` 400。三处调用 `requireActiveMerchant`。**ok**。`changeBusinessEntityBinding` 未走该闸——028 定稿「其余路径不变」，见观察，不升新缺陷。
19. **重复 active 绑定**：JDBC unique 索引仍抛 `uq_business_entity_bindings_active_merchant`。**ok**。
20. **幂等空键**：Controller 缺键 400 `idempotency_key is required`（日志实见）。**ok**。
21. **Flyway 头**：classpath 最新 V399，库 409，断言红——017 环境，不是本 20 文件缺陷。

未发现新的越权写、部分写入抹档、空间存在性截断、已下线商户经三创建路径被选用、或锁外业务发明。

Ponytail-review（只列不改）：`JdbcPlatformRepository.java:15` `import java.math.BigDecimal` 与 `PlatformController.java:7` `import ApiException` 无类型使用（同根 034，已登记否）；`PlatformServiceTests.java:694` 测试注释仍写「拒硬删」（M011 文件，026 同根）；`LeasingServiceTests.java:222` mock 返回值仍带 `leasing_auto`（请求侧已是 `null`，不在本 20 文件）。20 文件行为面无可再删的实现。`net: 注释/未用 import 数行，不构成新缺陷。`

---

## 6. 已定稿项复验 vs 仅登记项

| ID | 队列（定稿） | B5-r2 复验 |
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
| 026 | 已修（B3-r2） | 三处注释改软删；20 文件零「硬删」 |
| 027 | 已修（B3-r2） | Jdbc 按主键；501 条测试绿 |
| 028 | 已修（B3-r2） | 三创建路径 `requireActiveMerchant`；单元绿 |
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
| 029 | 否 | 空间/字典 status 仍 `trimToDefault` 任意串；映射无闭集 |
| 030 | 否 | 创建商户仍可写未校验存在的三主体列 |
| 031 | 否 | 停用绑定不清理商户三列 |
| 032 | 否 | PUT status 空仍缺省 active |
| 033 | 否 | 最高权限直删仍 save+deleteMerchant 双写同列 |
| 034 | 否 | 两处未用 import 仍在（HEAD 既有） |
| 035 | 否 | 绑定不校验三主体 status |

---

## 7. 本次 findings / 观察

临时编号 `GR-M009-B5-R2-xx`（与 B5-r1 的 `GR-M009-B5-01…06` 区分；后者已定稿为 026–035）。无新的未定稿工程缺陷，故无修复队列候选、不发 2.2、不申请改 HTML。

### 7.1 观察（不入修复队列，不构成 B5 FAIL）

| 编号 | 同根因 | 位置 | 事实 | 2.1 | 冻结 ID | 工程缺陷 |
|---|---|---|---|---|---|---|
| GR-M009-B5-R2-01 | M009-028「其余路径不变」 | `PlatformService.changeBusinessEntityBinding` 不调用 `requireActiveMerchant` | 已存档商户不能经创建项目/空间/绑定被选用（028 测试绿）；把既有绑定改回 active 仍不检查 archived_at。028 定稿处置仅三处创建路径 | 细化观察 | BSM-004 | 否（不扩 028 边界） |
| GR-M009-B5-R2-02 | M009-026→M011 | `PlatformServiceTests.java:694` 「>0 拒硬删」 | 本 20 文件注释已清；该句在 M011 测试夹具 | 技术性（跨模块注释） | GEN-016 | 否（非本 20 文件） |
| GR-M009-B5-R2-03 | M009-010→M011 | `PlatformServiceTests.java:709-710` `merchants.remove` | 内存 fake 仍物理 remove；JDBC 软标。010 已把分叉归 M011 | 技术性（跨模块） | GEN-008 | 否 |
| GR-M009-B5-R2-04 | M009-017 | `PlatformJdbcIntegrationTests.java:263` | 本席隔离跑：expected `"399"` but was `"409"`；psql `complex_test\|409`；Flyway「schema 409 newer than 399」。源码迁移目录最新 V399 | 技术性（环境） | GEN-003/GEN-013 | 否 |
| GR-M009-B5-R2-05 | M009-029 | `createSpace`/`createSpaceType` `trimToDefault(...,"active")` | 空间/字典 status 仍任意串。004 闭集只覆盖商户/主体/项目 | 技术性 | GEN-004 | 否（无 HTML 闭集，不发明） |
| GR-M009-B5-R2-06 | M009-030 | `saveMerchant` 创建分支三主体列 | 创建仍可写入未校验存在的三列；更新不覆盖；绑定激活才投影 | 细化观察 | GEN-004/PGM-026 | 否 |

031–035 事实仍在，维持登记，不另开号。无添加项。无 HTML 自身冲突需申请修改。无锁外判据。questions 0。

---

## 8. 命令与真实输出 / 退出码

验证均在 `/tmp/m009-b5-r2-verify` 隔离副本（rsync 工作区 `backend/` 排除 target；web 复制 `apps/web` 并链仓库根 `node_modules`、复制 `tsconfig.base.json`）。只从 `/etc/complex/complex.env` **抽出三个** `COMPLEX_DATABASE_*`（不 source 整文件），URL 库名改写为 `complex_test`（不打印密钥；USER_LEN=7 PASS_LEN=48）。JAVA_HOME=`/usr/lib/jvm/temurin-21-jdk-amd64`。主仓 cwd 未跑写模式 mvn/vitest。

### CMD-LOCK（exit 0）

```
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
```

python3 hashlib 同值 889020 字节；openssl dgst -sha256 同值。sidecar 全文一行同值。

### CMD-HEAD / TREE（exit 0）

```
HEAD=242f487233d028173cb9198eecd079f5d877405c
origin/main=242f487233d028173cb9198eecd079f5d877405c
after-tree(开工)=bea6236755d5114c8e38007e07f0f3a75717b341
```

### CMD-PSQL 只读（exit 0）

```
current_database|current_user=complex_test|complex
max(flyway)=409
parent_entity_id is_nullable=YES
merchants=87  entities=6  spv_or_operation=0  bad_coop=0  spaces=0
```

### CMD-UNIT-JDBC（Maven surefire；**进程 exit 1**，因 017 一条断言）

命令（隔离副本 cwd `/tmp/m009-b5-r2-verify/backend`）：

`mvn -q -Dtest=com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformJdbcIntegrationTests,com.complex.platform.leasing.LeasingServiceTests,com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests test`

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
PlatformServiceTests          Tests run: 30, Failures: 0, Errors: 0
  archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding  ok
PlatformControllerTests       Tests run: 10, Failures: 0, Errors: 0
LeasingServiceTests           Tests run: 11, Failures: 0, Errors: 0
LeasingSignAtomicityJdbcIntegrationTests  Tests run: 2, Failures: 0, Errors: 0

Tests run: 66, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
MAVEN_EXIT=1
```

不得把未运行测试写通过：上表 5 个类均本席本步实跑。唯一失败=017 环境，不折算为本模块源码缺陷。027/028 归因测试均为 ok。

Controller 日志实见：`PLATFORM_SCOPE_REQUIRED raw_message=商户账号无权查看平台级经营分析`；`VALIDATION_FAILED raw_message=idempotency_key is required`。

### CMD-WEB（exit 0）

隔离副本 `npm run test:unit -- src/admin/pages/OrganizationMerchantPage.test.tsx`（链根 `node_modules` + `tsconfig.base.json`）：

```
✓ src/admin/pages/OrganizationMerchantPage.test.tsx (12 tests) 534ms
Test Files  1 passed (1)
Tests  12 passed (12)
WEB_EXIT=0
```

### CMD-HTML-LOCK（exit 0）

锁内映射 87 条中非 GEN 的 HTML 来源行邻域对照现行三 HTML：`checked 87 miss 0`。

### CMD-GREP-20（exit 0）

本模块 20 文件：`硬删` 0、`物理删除` 0、`parentEntityId` 0、`leasing_auto` 0、`expireBar` 0。

临时副本结束清理：`rm -rf /tmp/m009-b5-r2-verify /tmp/m009-b5-r2-vitest-cache /tmp/m009-b5-r2-db.env`。

---

## 9. 技能实用 / 缺口

已读并按 V0.17 收窄使用（用户/160 指令优先于技能本体；**不**派子代理、**不**写 `docs/arc/audits/`、**不**向操作员提问）：

| 技能 | 用法 |
|---|---|
| using-superpowers | 先核技能再取证；本任务以 160 V0.17 为唯一审核标准 |
| using-arc | 只借机械核对/证据口径；禁止评审子代理与另写 arc 报告 |
| grilling | 只用追问树自问自答（§5）；不向操作员发问 |
| verification-before-completion | 先跑隔离命令再声称 PASS；Maven exit 1 不粉饰 |
| ponytail-review | 只列可删注释/未用 import，不落地 |
| codebase-design | PlatformService 为深模块；Jdbc/内存 fake 为仓储适配器；删除测试证明 20 文件有上位 |
| diagnosing-bugs / systematic-debugging | 017：读错误→复现 Flyway 409 vs 399→判定环境非源码 |
| writing-for-agents | 本 raw 给席甲/操作员读：步骤+完成判据同文件 |

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

同时满足：20 文件均有三线覆盖记录；87 条映射判据均已核对；三线均执行；声称均有 2.3 四类证据之一；无未答 questions；无未定稿缺陷；无添加项未答；无 HTML 问题；锁行有效。017 红已复现并维持环境登记。026–028 行为独立复验成立。无有条件通过。

本票不等于双席合取或模块 PASS。

## M009 第三批 B2-r1 整批归并复核（Grok 席乙）
文件自身版本：V0.02（本段追加；上方 V0.01 B5-r2 全文 40835 bytes 原样保留，不改写第 1 行锁段起点，本段不写独占锁哈希字段行）

- 席位：Grok（固定外审席乙）。步骤：第三批 B2-r1 归并复核。不是 B5 重审、不是修复、不是模块 PASS、不是 B3。
- 触发：B5-r2 合取 FAIL（席甲 FAIL／本席 PASS）。席甲 CX-B5R2-01 无定稿 ID，按运行手册 §8 整批回 B2。本席不因 B5 判 PASS 拒绝复核。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M009 冻结 20 文件 / 87 判据）；ledger V2.11 IN_PROGRESS；B3 累计 2/3（若本批定稿入队则 B3-r3 为最后一次）。
- 正本：`/home/james/complex-management` main `HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）。B3-r1/B3-r2 修复在工作区未提交，本席只读核源，不改。
- 本席本批 B2：**PASS**
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。三工具同值（`sha256sum` / `python3 hashlib` / `openssl dgst -sha256`）；锁文件 889020 字节；sidecar 全文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未用席甲本批 B2 追加段（当前 self-raw 仍为 V0.01 前缀，其后若有追加只保留不采信）。
- 席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本批不行使否决。
- 技能按 B2 收窄：grilling（自问自答取证，不向操作员发问）/ verification-before-completion（先核哈希、源码、psql 再给票）/ receiving-code-review（先核源与候选差集再接受或反驳，禁止表演性同意）/ ponytail-review（最懒可行，不落地代码）。未冒称 batch-grill-me。未派 Arc audit 子代理。

此票只判断 findings.md 顶部 V1.24「当前 M009 B5-r2 合取FAIL……第三批 B2-r1 整批候选（M009-036…）」相对本次两份完整 B5-r2 raw 的全批差集、GEN-017 四分类、工程缺陷与修复队列。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B5 **PASS** 原文。已修 18 项（001–012、014–016、026–028）与 013/017–025/029–035 不在本批，保持不变。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.24，478580 bytes，SHA256 `f8933e73f9c1786cdac39b762527f6f221dede39e1426292c0c6ab0b06d7fd89`。只读最上「第三批 B2-r1 整批候选」M009-036…037；下方他模块历史原文与 001–035 定稿不重审、不当本批依据。
- 席甲 B5-r2：`docs/audit-2026-09/modules/M009-b5-r2-self-raw.md` V0.01，14019 bytes，SHA256 `c0e62b33b588e838caf0ab6cc3bb78cf660a70ed3626ff688b9345b2c27857cb`（Claude Fable 5.1，作者自审票，结论 FAIL：CX-B5R2-01，OBS-B5R2-01…03；主仓实跑 76 条=74 通过+2 条 017 环境红）。
- 本席 B5-r2：本文件 V0.01 全文 40835 bytes，SHA256 `fdaa87631e0b63027118c71b4cb89bca29e21530c33393ff880b52dc48dca505`；结论 PASS，观察 GR-M009-B5-R2-01…06，无新缺陷。
- 冻结判据：映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 unique=87。本席独立三工具与 sidecar/ledger 三值全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。只用既有冻结判据，无 LOCK-AMEND。

### 差集（无遗漏、不改写本席 B5 原意）

| 本席 B5-r2 | 席甲 B5-r2 | 候选 ID | 差集结论 |
|---|---|---|---|
| GR-M009-B5-R2-05 技术性观察，工程缺陷否（无 HTML 闭集，不发明）；覆盖 `createSpace` **与** `createSpaceType` | CX-B5R2-01 技术性缺陷，建议入队；只覆盖 `spaces.status` | 036 | 并入。B5 原意保留为「无 HTML 闭集、不发明」；B2 仅对 **spaces 半边** 按已发布 V203 CHECK 与 005 同形态升格入队，见重点裁定。字典半边仍 029，不另开 ID。候选把 R2-05「归 029 不另开 ID」写对了编号，但必须标明 036 是 B2 升格而非 B5 已判缺陷，见误写节。 |
| GR-M009-B5-R2-01 细化观察，工程缺陷否（不扩 028 边界） | （席甲无对等 CX） | 037 | 并入登记。按本席 B5 原意维持否。候选队列否与原意一致。本席 **不** 改判入队。 |
| GR-M009-B5-R2-02 测试注释「拒硬删」 | （席甲无对等） | 不新开 | 归既有 026→M011。候选未另开 ID，正确。 |
| GR-M009-B5-R2-03 内存 fake `remove` | （席甲无对等） | 不新开 | 归既有 010→M011。候选未另开 ID，正确。 |
| GR-M009-B5-R2-04 flyway 409 | 席甲 §2 两条 MigrationVersions 红 | 不新开 | 归既有 017。候选未另开 ID，正确。 |
| GR-M009-B5-R2-06 创建商户三主体列 | （席甲无对等） | 不新开 | 归既有 030。候选未另开 ID，正确。 |
| （B5 未单列此项为缺陷；R2-05 只作观察） | CX-B5R2-01 | 036 | 见上行。本席 B5 未升缺陷不等于复核拒绝。 |
| （无） | OBS-B5R2-01 `findSpaceById`/`Map.of` 空参 | 不新开 | 技术性观察，调用点入参非空；可并 023 切分缝。候选未开 ID，正确。 |
| （无） | OBS-B5R2-02 库实态 | 不新开 | 事实记录，非 finding。正确。 |
| （无） | OBS-B5R2-03 六张表 CHECK 对照 | 不新开 | 036 的证据面，不是第三条根因。正确。 |

席甲 CX-B5R2-01 与本席 GR-M009-B5-R2-01 均落在 036–037；R2-02/03/04/05/06 归 026→M011、010→M011、017、029（字典半边）、030。无第三条根因、无新 ID、无 LOCK-AMEND、无 HTML 修改申请、无 2.2 添加项。

### 逐 ID 票（036–037）

| ID | 分类 | 工程缺陷 | 修复队列 | 本席票 | 理由（四分类→最懒处置） |
|---|---|---|---|---|---|
| 036 | 技术性 | **是**（B5 观察→B2 升格 spaces 半边） | **是** | PASS | 见重点裁定。`SPACE_STATUSES`=V203 集合、`requireSpaceStatus` 用于 `createSpace`、ServiceTests 一条最小用例。不改迁移、不改全局异常处理、不把未映射的铺位租赁六态写入本列。无业务发明。029 字典部分维持登记、本批不重开 029。 |
| 037 | 细化（观察） | 否 | 否 | PASS | 见重点裁定。**维持否**。028 最小边界仅三处创建路径，不扩到既有绑定的 `action=active`。候选括号「若外审改判入队」作废——本席不改判。 |

队列「是」=036（1 项，B3-r3 范围，B3 累计 2/3，最后一次）；「否」=037。已修 18 项与 013/017–025/029–035 保持否。添加 0，questions 0。

### 候选表对本席 B5 观察的误写（明确指出，不改写原意）

1. **036 / R2-05**：本席 B5-r2 原文（§7.1 GR-M009-B5-R2-05）工程缺陷=**否**；覆盖 `createSpace` **与** `createSpaceType` 的 `trimToDefault` 任意串，理由是「004 闭集只覆盖商户/主体/项目；映射无空间/字典状态枚举；无 HTML 闭集，不发明」。候选引言写「R2-05 归 029 既有口径不另开 ID」——**编号正确**，但 036 行把席甲 CX 写成独立缺陷入队时，未标明这是对 R2-05 **spaces 半边** 的 B2 升格。差集必须保留 B5「观察／缺陷否／无 HTML 闭集」；入队不是 B5 已判缺陷。定稿时 036 工程缺陷列改为「是（B2 升格 spaces 半边；B5-r2 R2-05 原意保留）」；029 维持登记且范围收成字典半边（V184 无 CHECK），**不**重开 029、**不**改 029 队列。
2. **037**：候选分类/缺陷/队列与 GR-M009-B5-R2-01 原意一致，未写错。括号「若外审改判入队，处置为激活分支加一行 requireActiveMerchant…」是预留句，不是本席票；本席维持否，该预留作废。
3. R2-02→026/M011、R2-03→010/M011、R2-04→017、R2-06→030 编号未写错。
4. 席甲 OBS-B5R2-01…03 未开新 ID，不是漏并。

### 重点独立裁定

#### 036 `createSpace` 的 status 未按已发布 V203 CHECK 前置校验（B5 观察 → B2 升格入队）

本席不为合取强行同意，也不因 B5-r2 判 PASS / R2-05 判否而拒绝复核。先独立取证，再套四分类。

**类型 4（工作区现文 + complex_test 只读探针）：**

- `PlatformService.createSpace` L500：`trimToDefault(request.status(), "active")` 直传入 `saveSpace`。全文件无 `requireSpaceStatus` / `SPACE_STATUSES`。对照同文件 L387 `createProject` 已走 `requireProjectStatus`（004）、L115/L741 走 `requireSubjectStatus`、L684–690 `requireBusinessEntityRole`（005，注释写明「非法值在服务层以中文 400 拒绝，不落到数据库 CHECK」）。
- `JdbcPlatformRepository.saveSpace` L280–301：`INSERT … status … ON CONFLICT DO UPDATE SET status = EXCLUDED.status`，无应用层白名单。
- `PlatformController` 仅 `POST /spaces` → `createSpace`；无 updateSpace。本模块写路径就是这一处。
- 已发布迁移 `V203__spaces_status_check.sql` L5–6：`chk_spaces_status CHECK (status IN ('active', 'unavailable'))`。其后迁移未再改该约束名。
- `GlobalExceptionHandler.java` L148–167：L148–153 注释明示「仅拦精确的 DuplicateKeyException…**不拦**父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug，仍应落 500」；L154–160 只处理 `DuplicateKeyException`→409；L162–167 兜底 `Exception`→500 固定文案「服务出现异常，请稍后重试」。候选写 :150–155，事实在 :148–167；不改结论。
- `complex_test` 本席 psql（只抽出三个 `COMPLEX_DATABASE_*`，库名改 `complex_test`；BEGIN 后非法插入失败、连接断开回滚；第二会话确认 `spaces` 仍 0 行）：
  - `current_database=complex_test`，flyway max=409。
  - `chk_spaces_status` 实态：`CHECK (status = ANY (ARRAY['active','unavailable']))`。
  - `chk_be_entity_role` 仍在（005 对照）。
  - `space_type_dictionary` **无** CHECK 约束（V184 注释写 `active/disabled`，无 CONSTRAINT；本席 `pg_constraint contype='c'` 0 行）。
  - `INSERT spaces status='zzz'` → `ERROR: new row for relation "spaces" violates check constraint "chk_spaces_status"`（Failing row `zzz`）。
  - 同事务外另 BEGIN：`active` / `unavailable` 均可插入；`space_type_dictionary status='zzz'` **插入成功**。两次 ROLLBACK 后 `spaces` 仍 0。

经 API 的非法 status 必走：Service 放行 → JDBC CHECK → `DataIntegrityViolationException` → 兜底 500。用户得不到字段级中文 400。内存 fake 无 CHECK，现有 `PlatformServiceTests` 对 `zzz` 会绿——故一条 ServiceTests 用例 **能红**，是最小 TDD，不必再加 JDBC。

**与 029 / 本席 B5-r1、B5-r2 原意的关系（不改写）：**

B5-r1 GR-M009-B5-02 / 第二批 B2 定稿 029：「`createSpace`/`createSpaceType` `trimToDefault` 任意串。004 闭集只覆盖商户/主体/项目（BEQR-154/155）；映射无空间/字典状态枚举。**HTML 无闭集，不发明。登记。**」B5-r2 R2-05 原文仍是这一句。该原意针对的是 **从 HTML 沉默发明枚举**。本席 B5 未读 V203、未把已发布 CHECK 当作闭集证据，故当时不能把 spaces 半边升为缺陷。B2 问的是另一件事：闭集若已作为 **已发布迁移的代码事实** 存在，服务层是否还允许非法值落到 500。

映射 87 条不含 BEQR-170「铺位租赁状态」（空置/洽谈中/已租/即将到期/已退租/不可租）。本席 **禁止** 用未映射锚点给 M009 发明六态，也禁止把该六态写入 `SPACE_STATUSES`。V203 注释把本列收成 `active/unavailable`，与租赁看板六态不是同一列语义（六态由合同联动派生；本列是库已接受的二值）。036 只对齐该二值，不实现租赁看板。

**与 005 的同形态 / 不同值源（不为合取省略）：**

- 同形态：用户输入无服务层范围校验 → 已发布 CHECK 拒 → Handler 不拦 `DataIntegrityViolationException` → 500 固定文案。005 定稿处置是服务层白名单 + 中文 400，**不**改 Handler、**不**改迁移。Handler L148–153 把 CHECK 违例定义为服务端 bug 应 500——因此正确修法是服务层不要把非法值送进库，不是把 Handler 改成 400。
- 不同值源：005 的三角色来自已映射 PGM-018/019，且是 CHECK（含 `spv`/`operation`）的 **真子集**；`spv`/`operation` 另登 025→M010。036 **没有** 已映射 HTML 闭集，服务层闭集只能等于 V203 已有二值。用 CHECK 当服务层集合，不是从 HTML 新造状态机，也不是把 005 的「HTML 子集」套路套到空间。

**四分类（冲突 → 细化 → 技术性 → 添加）：**

- 冲突：否。已映射 BEQR-154/155 管的是项目/商户六态，不是 `spaces.status`。未映射 BEQR-170 不得拿来判 M009 冲突。
- 细化：否。已映射 HTML 没有「创建空间必须校验状态闭集」的原则句可落到本 20 文件。
- 技术性：是。GEN-004（输入在首次使用前做范围校验）、GEN-008（迁移枚举与服务层不一致）、GEN-009（CHECK 违例被 500 掩盖，调用方看不到字段错误）。先于添加。
- 添加：否。不新增态、不写状态机、不改 HTML、不加租赁看板六态。

无业务发明：`SPACE_STATUSES` ≡ 已发布 `chk_spaces_status`；缺省仍 `active`（现码 `trimToDefault` 已如此）；`unavailable` 仍合法。中文 400 只译这二值（可用/不可用），括号内 `active/unavailable` 为内部标识符例外；不得写空置/洽谈中/已租/即将到期/已退租。

工程缺陷：是。队列：是。最小处置（ponytail）：

1. `PlatformService` 增加 `SPACE_STATUSES = Set.of("active", "unavailable")`。
2. `requireSpaceStatus`：`trimToDefault(raw, "active")`，不在集合则 `ApiException.badRequest("VALIDATION_FAILED", 中文只陈述上述二值)`——与 `requireProjectStatus` 同法。
3. **只**替换 `createSpace` L500 的 `trimToDefault`；`createSpaceType` 不动（V184/活库无 CHECK，029 字典部分）。
4. `PlatformServiceTests` 一条最小用例：`status="zzz"` → `VALIDATION_FAILED` 400（现码内存 fake 会接受，故能红）。不必 JDBC、不必测 `unavailable` 仍接受（集合包含即保持）。
5. 不改 V203、不改 `GlobalExceptionHandler`、不改 `saveSpace` 仓储、不加 updateSpace。

与 029：本批 **不** 重开、**不** 改队列。029 原文「空间/字典」在 036 修复后空间半边会被覆盖；定稿只在 036 处置注「029 字典部分（V184 无 CHECK）维持登记」。B3-r3 不得顺手给 `createSpaceType` 发明 `active/disabled` 闭集。

票 PASS。若判否，须说明与 005「CHECK→500」的可执行区别——本席说明不了：都是已发布 CHECK、同一 Handler、同一 500 固定文案。故升格入队，不行使否决。

#### 037 `changeBusinessEntityBinding(action=active)` 不走 `requireActiveMerchant`（维持否）

本席 B5-r2 GR-M009-B5-R2-01 原意不改写：已存档商户不能经 **创建** 项目/空间/绑定被选用（028 测试绿）；把既有绑定改回 `active` 仍不检查 `archived_at`。028 定稿处置仅三处创建路径，「其余路径不变」。B5 标细化观察、缺陷否、不扩边界。

独立核源（不采信候选转述）：

- `requireActiveMerchant` L343–348：存在且 `archivedAt==null` 且 `deleteStatus≠approved`，否则 400 `MERCHANT_ARCHIVED`，文案「不能再用于 **新的** 项目、空间或主体绑定」。
- 调用点仅三处：`createProject` L385、`createSpace` L483、`createBusinessEntityBinding` L555。与 028 定稿逐字相同。
- `changeBusinessEntityBinding` L585–633：`action` 白名单 `active/inactive/suspended`；`active` 分支只拦第二份 active 绑定（L606–613），然后按 `existing.merchantId()` 回写，**无** `requireActiveMerchant`。`syncMerchantEntities` 仅在 `active` 时投影三列（031 边界）。

BSM-004「存档（先下线）…不再被 **新业务** 选用」在 028 已落到本模块 **创建写路径**。重新激活是已存在绑定的状态变更，不是从选择列表新选该商户去建项目/空间/新绑定。文案自己写了「新的」。把激活闸并进 028 会在修复完成后扩边界，不是最懒。

四分类停在细化观察（原则已在创建路径落地；激活是否算「新选用」HTML 沉默）。不是冲突，不是技术性必修，不是添加。工程缺陷否。队列否。最小处置：不修。候选预留「激活分支加一行 + 一条用例」仅当本席改判才用——本席不改判，预留作废。

与 031 分根：031 是停用时不清理三列（008 激活投影边界）；037 是激活时不拦已下线商户。不合并、不新开行为项。票 PASS（维持否）。

### 是/否队列合规与最小性

- 入队仅 036：本模块可修实现面（`PlatformService.createSpace`）、冻结判据 GEN-004/008/009、能红的最小测试、不发明业务。
- 037 登记：028 已画最小边界；扩激活闸不是本批必须。
- 029–035 与已修 18 项、013/017–025 不在本批，不塞进 B3-r3。
- B3-r3 只修 036 及归因的一条 ServiceTests；不改 HTML/判据/权限种子/迁移/Handler/其它 ID。

### 最小修正意见（定稿措辞；**不**构成否决）

队列已与本席票一致（是=036，否=037），故整批 PASS，**不**要求 B2-r2。定稿时仅改表内措辞，不改队列：

1. 036 工程缺陷定为「是（B2 升格 spaces 半边；B5-r2 R2-05 原意「无 HTML 闭集、不发明」保留）」。删除任何读成「Grok B5 已支持入队」的写法。
2. 036 处置写明：闭集 ≡ V203 `active/unavailable`；中文 400 只译这二值；**禁止**把未映射 BEQR-170 六态写入常量或文案；`createSpaceType`/029 字典部分不动。
3. Handler 行号改为 L148–167（注释不拦 CHECK + DuplicateKey + 兜底 500）。不改结论。
4. 037 删除「若外审改判入队」预留句，或标明外审维持否、预留作废。
5. 已修 18 项与 013/017–025/029–035 不动。B3-r3 只修 036 及一条最小 ServiceTests。

### 本席实跑（B2，只读）

- 锁文件三工具：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar/ledger 顶行同值
- 输入 SHA/字节：self-raw 14019/`c0e62b33…7857cb`；本文件前缀 40835/`fdaa8763…dca505`；findings V1.24 478580/`f8933e73…d7fd89`
- HEAD=`242f487233d028173cb9198eecd079f5d877405c` = origin/main
- 源码：`PlatformService.java` L343–348 / L385 / L483 / L500 / L555 / L585–633 / L674–690；`V203__spaces_status_check.sql`；`V184__space_type_dictionary.sql`（无 CHECK）；`GlobalExceptionHandler.java` L148–167；`JdbcPlatformRepository.saveSpace` L280–301；`PlatformController` POST `/spaces`；`PlatformServiceTests` 现有 createSpace 用例均传 `"active"`
- HTML/锁：BEQR-154/155 在映射内；BEQR-170 不在 M009 映射 87 条，本席不引用为入队依据
- psql `complex_test`：`chk_spaces_status` 实态二值；`zzz` 违例；`active`/`unavailable` 可插入；字典无 CHECK 且 `zzz` 可插入；回滚后 `spaces=0`
- 未在主仓跑写模式 mvn/vitest；未对业务库提交；未建长期 /tmp 验证副本（探针脚本用后删除）

### 结论

本席第三批 B2-r1：**PASS**

036 升格入队（已发布 V203 CHECK→500 与 005 同形态，不能用「无 HTML 闭集」挡住 spaces 半边；字典半边仍 029）。037 维持否（028 最小边界，不扩既有绑定激活）。差集无漏并。无添加项需 2.2。无 HTML 修改申请。无条件通过。本票只覆盖本批归并，**不是**模块 PASS，**不是** B5 合取，**不是**已修 036。
