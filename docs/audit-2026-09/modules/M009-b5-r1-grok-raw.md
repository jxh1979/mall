CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B5-r1 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 PASS、B3 自报或历史 B1 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M009 同一 20 文件；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 1/3；LOCK-AMEND-COUNT 1
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）；sidecar 同行；ledger 顶行同行
- 映射：`M009 → [BEQR-063 … PGM-066]`，COUNT=87 unique=87 MISSING=[]；定义与 HTML 出处均从锁自读，不新造判据
- 正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；开工时工作区 after tree=`bf61aa21f9425ca176d3a7e1211459092282d9fd`（临时索引 `git add -A && git write-tree`）；B3 修复未提交，审计对象=当前工作区 20 文件（含 B3）
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m009-b5-r1-verify`，结束清理）
- 未读取：`M009-b5-r1-self-raw`（当前亦不存在）。B4-r1 / B1 raw 与 findings V1.20 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界）：findings V1.20 顶部「15 项已修转否、待 B5」；001–012、014–016 已修；013/017–025 仅登记；M009-017 环境红（complex_test Flyway 409 vs 源码最新 399）不是源码缺陷
- 技能（V0.17 收窄，按实际存在与已读执行适用意图；未核到的不称已调用）：见文末技能节

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问、无锁外判据、无 HTML 修改申请。001–012/014–016 修复后行为独立复验成立；013/017–025 维持仅登记（017 本席复现为环境红）。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲票仍是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| 字节 | 889020 |
| python3 hashlib | 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 同行 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V2.06；2815 行 / 398061 字节 / SHA `12cd8c9686e44ad3dd8b09b65d74f5e81e9190db4f6b46097964298070bba8b6` |
| findings | V1.20；3197 行 / 466221 字节 / SHA `d1abb35466eba36e44af1eca1abd75aa1919908392bfdfca24117ff7ac4b4243` |
| 映射行抽出 | 87 条、无重复、与开工闭集逐字相同 |
| 锁内 69 处 HTML 来源行 vs 现行三 HTML | MISS_COUNT=0（首条摘录行均落在引用行邻域） |
| 计划 FILES | 20 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 2476（B3 前）；当前工作区 `wc -l` 合计 **2624**（B3 增行，不改计划凑数） |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

### 1.1 20 文件当前行数 / 字节 / SHA256（工作区实算，`wc -l` = `\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| 1 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java` | 17 | 391 | `45431c580d0bca0c217bcdb8ea120604eb7ed2b441845a5ac52257d5130b1277` |
| 2 | `backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java` | 19 | 463 | `f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422` |
| 3 | `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` | 90 | 5415 | `3a37d33901b32a6455e6a9dbd43b4fd27fd0b94d7352de224b4c3e8afec626b2` |
| 4 | `backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java` | 9 | 400 | `e08618c856b35a1eda8447a9c9bd0c88ed00a9ee371c1ee33d3f2f3ab7ce5e40` |
| 5 | `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | 1101 | 61310 | `3985ae2ab90cba20cfdcca6007ac82bc83574c6303c2df0a76993b8940bfce4b` |
| 6 | `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | 236 | 13580 | `05bd7cf4a2bfcb4398c6859abe6c56ce40041a982cdd308d51557ad00d92b743` |
| 7 | `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java` | 134 | 6613 | `76b9a1d8c5c034f4503e6502249ca7ad8ca3521f09aad9708be6841e1c006e18` |
| 8 | `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 854 | 41905 | `2829c83fb0db7c6af8b841210ca954d8d63a746e8c9e3258a145f9c405354f08` |
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

合计行 2624、字节 134797。Jdbc 1101 / Service 854 / Controller 236 均整文件读完（含 Jdbc L683–1100 `SupervisorReviewRepository` 实现），未按标题抽样。Jdbc 审批链方法按「文件已有职责不因模块名裁掉」纳入三线，但不把主管复核 Service 扩成全量审计。

必要只读依赖（验证本模块调用/字段，不扩审模块）：`LeasingService.java` L288–292（003 伴生，453 行 / 27927 字节 / SHA `169d2fcf52b1c6b933eeac2686e6a700351d9b1b5d773cfcdcf47c71c604cb0b`）；`OrganizationMerchantPage.tsx` L33–41 与测试（480/261 行）；M011 三测试；`LeasingSignAtomicityJdbcIntegrationTests.java` L116–121 夹具；`V318` `parent_entity_id` 可空列；`AuthorizationService.requirePlatformScope` / `isMasterAuthority`；`AuditedOperationExecutor.executeWrite`；`PiiCipher`。

---

## 2. 全量三线覆盖表

| # | 文件 | 第一性原理 | 顺从式 | 对抗式 | 覆盖 |
|---|---|---|---|---|---|
| 1 | BusinessEntityRecord.java | 已审 | 已审 | 已审 | 全文 17 行 |
| 2 | BusinessEntityBindingRecord.java | 已审 | 已审 | 已审 | 全文 19 行 |
| 3 | MerchantRecord.java | 已审 | 已审 | 已审 | 全文 90 行 |
| 4 | MerchantDeleteDecision.java | 已审 | 已审 | 已审 | 全文 9 行 |
| 5 | JdbcPlatformRepository.java | 已审 | 已审 | 已审 | 全文 1101 行（OVERSIZE 分段读完） |
| 6 | PlatformController.java | 已审 | 已审 | 已审 | 全文 236 行 |
| 7 | PlatformRepository.java | 已审 | 已审 | 已审 | 全文 134 行 |
| 8 | PlatformService.java | 已审 | 已审 | 已审 | 全文 854 行 |
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

无 finding 不等于未审。无中途回退。

---

## 3. 第一性原理（逐文件）

删除测试：删掉这 20 文件，BSM-009/PGM-006/PGM-026「建立商户档案、四类主体绑定、综合体/空间」在后台将无写入面。答得上上位。B3 后错误枚举/错误删除语义/单父链写入面已从本模块接口拿走。

| 文件 | 为何必须存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| BusinessEntityRecord | 法律/经营主体主数据 | 创建经 Service 白名单角色+主体状态；Jdbc INSERT 不再写 `parent_entity_id`；记录无 parent 字段 | DB 列仍在（V318，M010）；列 `is_nullable=YES`，本模块 INSERT 不碰它 |
| BusinessEntityBindingRecord | 四类主体一组关系 | 创建时拦第二份 active + 存在性；激活回写商户三列 | 停用绑定不清理三列（008 最小边界，OBS） |
| MerchantRecord | 商户档案+存档删除轴+电话掩码 | 25 参更新保留 archived_*/delete_*；`withEntities` 投影三列；`withMaskedPhones` 在 Controller | 19 参构造仍把存档默认 none（只给创建/测试用）；字段注释仍写「硬删」（OBS，同根 010） |
| MerchantDeleteDecision | 审批结果 | javadoc 现为软删标记；`deleted=true` 对应 `delete_status=approved` | 内存 fake 仍 `remove`（M011/010 分叉） |
| JdbcPlatformRepository | Postgres 适配+审批链仓储 | JDBC 11/12 绿：软删行 `findById` 仍在、`includeArchived=true` 可见、默认列表不可见；PII 加密回读；绑定 unique 部分索引仍拦第二份 active | 1 红=Flyway 409≠399（017）；`countMerchantReferences` 动态 COUNT；L683+ 审批链本模块 API 不调用 |
| PlatformController | HTTP 面 | 单元 10/10：无权限 403、缺幂等 400、读端 `requirePlatformScope` 403 | `createSpaceType` 控制器不预检幂等键（022）；写端平台范围在 Service |
| PlatformRepository | 仓储接口/默认内存切片 | `listMerchants(true)` 全量；`deleteMerchant` 默认抛、Jdbc/fake 覆写；`businessEntityExists`/`projectExists` | 无过滤 `listMerchants()` 仍被跨模块调用（020） |
| PlatformService | 写路径/校验/审计 | 单元 29/29：更新保档、404 更新、合作六类、状态六态、角色白名单、幽灵绑定、空间存在性、绑定投影、非最高权限拒批、最高权限直删、分页 500 | 创建商户仍可写未校验存在的三主体列（006/008 最小边界）；空间 status 无闭集；无 TicketingService |
| package-info.java | 包声明 | `package com.complex.platform.governance;` | 无行为 |
| BusinessEntityBindingRequest | 绑写入参 | 四 ID + 生效日 + 理由；服务层存在性 | 不在 DTO 校验 |
| BusinessEntityRequest | 主体入参 | 无 parent；角色/状态在服务层闭集 | 无 |
| MerchantArchiveRequest | 存档理由 | `reason` 存档必填；注释与 BSM-004 一致 | 取消存档可空 |
| MerchantDeleteApprovalRequest | 审批 | approved/rejected；最高权限闸在服务层 | `comment` 不落库（021） |
| MerchantDeleteRequest | 删除申请理由 | 须先存档；最高权限走直删 | 无 |
| MerchantProfileRequest | 商户档案 | 业态/合作/状态在服务层闭集 | 三主体列创建时可带、更新不覆盖 |
| ProjectRequest | 项目/场地 | 状态六态；merchantId 存在性 | 无负责人/地址/经营范围（013/018） |
| SpaceRequest | 空间 | space_type 须字典 active；project/merchant/parent 存在性 | status 默认 active、无闭集 |
| SpaceTypeRequest | 空间类型字典 | upsert by type_code | 控制器不预检幂等键 |
| StatusChangeRequest | 绑定状态变更 | action∈active/inactive/suspended；报错中文+内部码 | 英文内部键属标识符例外 |
| VenueBusinessHoursRequest | 场地营业时间 DTO | Controller GET/POST 存在并 `requirePlatformScope` | 保存语义在其它模块 Service；本 DTO 无草案/审批字段 |

---

## 4. 顺从式（87 条逐 ID）

分类固定顺序：冲突 → 细化 → 技术性 → 添加。无实现面写原因，不把 HTML 沉默当冲突。不 LOCK-AMEND。

| ID | 覆盖结论 | 2.1 | 工程缺陷 |
|---|---|---|---|
| BEQR-063 | 沽清/反结/免单。20 文件无账单实现 | 不适用（无实现面） | 否 |
| BEQR-074 | 隐私桌台。无桌台权限实现 | 不适用 | 否 |
| BEQR-143 | 绑定表经营对象+运营/合同/结算；active 唯一。创建/变更前 `requireMerchant`+三主体存在；unique 部分索引仍在；建档即绑定仅登记（006 后半） | 细化（存在性已落地） | 否（已修 006） |
| BEQR-144 | 多对多非隶属链。M009 面已删 `parentEntityId`（记录/DTO/Jdbc 写入/Service）；列留 M010 | 细化（012 最小面） | 否（已修 012） |
| BEQR-145 | 城市项目公司职责。本模块只存角色码 `project_company`，不闸酒吧人员设备定价 | 细化；职责闸在后续域 | 否 |
| BEQR-146 | 运营公司职责。`merchant_type` 六值与 BSM-072 对齐 | 细化 | 否 |
| BEQR-147 | 收入归属。本模块不记账 | 不适用 | 否 |
| BEQR-148 | 合作六类。`MERCHANT_COOPERATION_FORMS` 非空只接受六码；空=未定；页面中文六 option；Leasing 不再写 `leasing_auto` | 细化（技术映射） | 否（已修 003） |
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
| BSM-004 | 存档/软删/先存档再审批/禁自审/占用检查/填理由/可查可恢复。更新保档；软删 UPDATE；includeArchived 含 approved；最高权限直删；非最高拒批；禁自审 | 细化（行为已对齐）；批量存档页/清理工具无实现面 | 否（已修 001/009/010/011；流程页 018） |
| BSM-005 | 分步引导弹窗。后台 UI 不在本 20 文件 | 不适用 | 否 |
| BSM-006 | 动态码。无 | 不适用 | 否 |
| BSM-007 | 平台管理员≠最高权限。删除审批 `isMasterAuthority`；权限码 manage/approve 分离 | 细化 | 否（已修 009；种子在 M010） |
| BSM-009 | 基础管理入口。本模块承载商户/主体/空间/项目写入 | 细化 | 否 |
| BSM-028 | 建综合体/商户/六类合作/多对多。合作与层级见 003/012 | 细化 | 否（已修） |
| BSM-029 | 下游要空间商户归属。本模块提供 ID | 细化 | 否 |
| BSM-072 | 业态六值。应用层闭集；读端平台范围 | 细化 | 否（015 读端已加） |
| BSM-074 | 商户不能看平台全局。写/读 `requirePlatformScope` | 细化 | 否（已修 015） |
| BSM-114 | 入口表。本模块是管理后台 API | 细化 | 否 |
| BSM-115 | 基础管理连所有业务。提供归属 ID | 细化 | 否 |
| BSM-120 | 已拍板清单。与本 20 文件无直接实现面的条目不发明 | 无实现面为主 | 否 |
| GEN-001 | 20 文件均有用途；`requireDate`/`readObjectMap` 已删 | 技术性 | 否（已修 016） |
| GEN-002 | 单元 29+10+11 绿；JDBC 11/12 绿；签约原子 2/2 绿；页面 12/12 绿 | 技术性 | 否（017 环境红单独登记） |
| GEN-003 | 关键分支有能失败断言（非法合作/状态/角色/幽灵绑定/非最高权限/更新 404/软删可查） | 技术性 | 否 |
| GEN-004 | 业态/合作/主体状态/项目状态/角色/绑定对象/空间归属均有校验 | 技术性 | 否（已修 002–007）；创建路径三列存在性见 OBS |
| GEN-005 | 各写入口权限码+平台范围；删除审批最高权限；读端平台范围 | 细化 | 否（已修 009/015） |
| GEN-006 | 更新保档；绑定存在性；绑定激活投影三列；软删不抹行 | 细化 | 否（已修 001/006/008/010） |
| GEN-007 | 写路径幂等执行器；active 绑定 unique 索引；`createSpaceType` 空键服务层仍拒 | 技术性观察 | 否（022 仅风格） |
| GEN-008 | 档案更新不覆盖三列；分页 pageSize=500；合作码前后端同词表 | 技术性 | 否（已修 003/008/014）；内存 fake 分叉归 M011 |
| GEN-009 | 错误码中文为主；括号内内部码 | 观察（023） | 否 |
| GEN-010 | 电话 PII 仓库加密（JDBC 测试绿）；动态 COUNT 表名来自 information_schema | 技术性 | 否 |
| GEN-011 | executeWrite 记成功/失败事件；存档/删除有事件名 | 细化 | 否 |
| GEN-012 | 列表 size cap 500；Jdbc paged LIMIT/OFFSET | 技术性 | 否 |
| GEN-013 | 无本模块构建脚本 | 不适用 | 否 |
| GEN-014 | 无迁移文件（M010） | 不适用 | 否 |
| GEN-015 | 无本模块 UI；API JSON 用内部键 | 观察（内部标识符） | 否 |
| GEN-016 | 四处指定注释已改为软删；MerchantRecord/Jdbc 占用检查仍有「硬删」字样 | 技术性观察（同根 010 注释扫尾） | 否（行为已软删；见 OBS-01） |
| GEN-017 | 本批无把发明标成技术性；025 spv/operation 转 M010 | — | 否 |
| GEN-018 | 结论落到 HTML 原文、单元/JDBC/页面输出、psql 只读；017 失败如实 | — | — |
| PGM-001 | 治理底座/全中文/金额/先成功。本模块是底座写入；金额精度无本文件计算 | 细化；报错观察 | 否 |
| PGM-002 | 先看对象再合同。本模块只建对象，不卡合同 | 无合同闸实现面 | 否 |
| PGM-003 | 模块入口卡片。无 HTML/UI | 不适用 | 否 |
| PGM-004 | 综合体和集团卡片 | 不适用（文案） | 否 |
| PGM-005 | 空间点位卡片。空间 API+存在性校验 | 细化 | 否（已修 007） |
| PGM-006 | 商户和主体。合作六类+绑定存在性 | 细化 | 否（已修 003/006） |
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
| PGM-026 | 商户档案+四类绑定+六类合作。合作闭集+绑定存在+激活投影 | 细化 | 否（已修 003/006/008） |
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

映射 87/87 均有覆盖记录。

---

## 5. 对抗式（自问自答，grilling 结构，不向操作员发问）

假定实现仍错，构造最小反例：

1. **更新抹存档（001）**：`updateMerchantKeepsArchiveAndPendingDeleteState` 断言 `archivedAt`/`deleteStatus=pending`/`createdAt` 保留，且默认列表仍隐藏。PASS。
2. **路径 id 建档（002）**：`updateMerchantRejectsUnknownMerchant` → `MERCHANT_NOT_FOUND`，仓储无该 id。PASS。
3. **合作任意串（003）**：`fixed_rent_plus_commission` → `MERCHANT_COOPERATION_FORM_INVALID`；`lease` 接受；空接受。Leasing L291 注释+`null`。页面六类中文。complex_test `bad_coop=0`。PASS。
4. **状态任意串（004）**：商户 `draft` 拒；`pending_admission` 接受；项目 `zzz` 拒。PASS。
5. **角色 spv（005）**：`createBusinessEntityRejectsRoleOutsideWhitelist`。活库 `spv_or_operation=0`。Jdbc 测试夹具仍可直写 `operation`（绕过服务层，025/M010）。PASS（本模块 API）。
6. **幽灵绑定（006）**：缺商户 `MERCHANT_NOT_FOUND`；缺主体 `BUSINESS_ENTITY_NOT_FOUND`；绑定表空。签约夹具先 INSERT `business_entities`。PASS。
7. **空间幽灵归属（007）**：缺项目/商户/上级空间分别 404。PASS。
8. **档案改三列（008）**：更新传入 `be_other` 等，读回仍 `be_1`；创建绑定后三列=绑定值。PASS。
9. **非最高权限审批（009）**：`MERCHANT_DELETE_APPROVER_NOT_MASTER`，状态仍 pending；最高权限申请直达 `approved`。PASS。
10. **物理删除（010）**：JDBC `approvedDeletedMerchantStaysQueryableWithIncludeArchived`：`findById` 在、includeArchived 含、默认列表不含。`deleteMerchant` SQL=`UPDATE delete_status='approved'`。PASS。内存 fake 仍 remove（M011）。
11. **存档即撤场停票（011）**：`PlatformService` 无 `TicketingService`/`expireBarSingleStoreTickets`。PASS。
12. **parent 链（012）**：20 文件 `parentEntityId` 零命中；Jdbc INSERT 列清单无 parent；DB 列可空。PASS。
13. **分页（014）**：`listSpaceTypesPageSizeCoversAllItems` pageSize=500。PASS。
14. **商户读平台列表（015）**：`listingMerchantsRequiresPlatformScope` 403 `PLATFORM_SCOPE_REQUIRED`。PASS。
15. **死代码（016）**：`requireDate`/`readObjectMap` 零命中。PASS。
16. **重复 active 绑定**：JDBC unique 索引仍抛 `uq_business_entity_bindings_active_merchant`。PASS。
17. **幂等空键**：Controller 缺键 400 `idempotency_key is required`（日志实见）。PASS。
18. **Flyway 头**：classpath 最新 399，库 409，断言红——017 环境，不是本 20 文件缺陷。

未发现新的越权写、部分写入抹档、或锁外业务发明。

Ponytail-review（只列不改）：`MerchantRecord.java:27` / `JdbcPlatformRepository.java:187` / `PlatformRepository.java:54` 仍用「硬删」字样，行为已是软标记——注释扫尾，净删约 3 处措辞即可，不构成新行为缺陷。19 参构造仍被 JDBC/测试使用，不是死代码。

---

## 6. 已定稿 15 项复验 vs 仅登记 10 项

| ID | 队列（定稿） | B5 复验 |
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
| 013 | 否 | 维持：ProjectRecord/迁移/页面不在本 FILES |
| 017 | 否（环境） | 本席复现 expected 399 but was 409；Flyway 日志「schema 409 newer than 399」 |
| 018 | 否 | 缓冲期/已存档内容页/清理工具仍无实现面 |
| 019 | 否 | 同构审批不在本 20 文件 |
| 020 | 否 | `listMerchants()` 无过滤版本仍在接口 |
| 021 | 否 | `comment` 仍不落痕 |
| 022 | 否 | `createSpaceType` 控制器仍不调 `requireIdempotencyKey` |
| 023 | 否 | `saveChainNodes` 仍无方法级事务；切分缝仍在 |
| 024 | 否 | 拒绝文案/缺键译法不在本 FILES |
| 025 | 否 | 白名单不含 spv/operation；活库 0 行；转 M010 |

---

## 7. 本次 findings / 观察

临时编号 `GR-M009-B5-xx`。无新的未定稿工程缺陷，故无修复队列候选。

### 7.1 观察（不入修复队列，不构成 B5 FAIL）

| 编号 | 同根因 | 位置 | 事实 | 2.1 | 冻结 ID | 工程缺陷 |
|---|---|---|---|---|---|---|
| GR-M009-B5-01 | M009-010 | `MerchantRecord.java:27`；`JdbcPlatformRepository.java:187`；`PlatformRepository.java:54` | 行为已是 `UPDATE delete_status='approved'`，三处历史注释仍写「硬删」。010 约定改的四处 javadoc 已改。 | 技术性 | GEN-016 | 否 |
| GR-M009-B5-02 | M009-004 未纳入 | `PlatformService.createSpace:491` `trimToDefault(...,"active")`；`createSpaceType:445` | 空间/字典 status 仍任意串。004 闭集只覆盖商户/主体/项目（BEQR-154/155）；映射无空间状态枚举。 | 技术性 | GEN-004 | 否（无 HTML 闭集，不发明） |
| GR-M009-B5-03 | M009-006/008 最小边界 | `PlatformService.saveMerchant:741-743` 创建分支 | 创建商户仍可写入未校验存在的三主体列；更新不覆盖；绑定激活才投影。B2 明确建档即绑定仅登记、008 只做激活投影。 | 细化观察 | GEN-004/PGM-026 | 否 |
| GR-M009-B5-04 | M009-010→M011 | `PlatformServiceTests.java:690-692` `merchants.remove` | 内存 fake 仍物理 remove；JDBC 软标。010 已把分叉归 M011。 | 技术性（跨模块） | GEN-008 | 否（非本 20 文件） |
| GR-M009-B5-05 | M009-017 | `PlatformJdbcIntegrationTests.java:263` | 本席隔离跑：expected `"399"` but was `"409"`；psql `complex_test\|409`；Flyway warn newer than 399。源码迁移目录最新 V399。 | 技术性（环境） | GEN-003/GEN-013 | 否 |
| GR-M009-B5-06 | M009-008 | `changeBusinessEntityBinding` 仅 active 时 `syncMerchantEntities` | 停用绑定不清理商户三列。008 最小边界仅创建/激活回写。消费方归 020。 | 细化观察 | PGM-026 | 否 |

无添加项。无 HTML 自身冲突需申请修改。无锁外判据。questions 0。

---

## 8. 命令与真实输出 / 退出码

验证均在 `/tmp/m009-b5-r1-verify` 隔离副本（rsync 工作区源、排除 target；web 另链根 `node_modules` 并补 `tsconfig.base.json`）。只 export 三个 `COMPLEX_DATABASE_*`，URL 库名改写为 `complex_test`（不打印密钥；USER_LEN=7 PASS_LEN=48）。JAVA_HOME=`/usr/lib/jvm/temurin-21-jdk-amd64`。主仓 cwd 未跑写模式 mvn/vitest。

### CMD-LOCK（exit 0）

```
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
```

python3 hashlib 同值；openssl 同值。

### CMD-HEAD / TREE（exit 0）

```
HEAD=242f487233d028173cb9198eecd079f5d877405c
after-tree(开工)=bf61aa21f9425ca176d3a7e1211459092282d9fd
```

### CMD-PSQL 只读（exit 0）

```
current_database|max(flyway)=complex_test|409
parent_entity_id is_nullable=YES
merchants=87  entities=6  spv_or_operation=0  bad_coop=0
```

### CMD-UNIT-JDBC（Maven surefire；**进程 exit 1**，因 017 一条断言）

命令：`mvn -Dtest=com.complex.platform.governance.PlatformServiceTests,com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformJdbcIntegrationTests,com.complex.platform.leasing.LeasingServiceTests,com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests test`

关键输出：

```
Flyway: Successfully validated 387 migrations
Current version of schema "public": 409
WARN Schema "public" has a version (409) that is newer than the latest available migration (399) !
Schema "public" is up to date. No migration necessary.

PlatformJdbcIntegrationTests  Tests run: 12, Failures: 1, Errors: 0
  residualMigrationCreatesBindingContractTablesAndSeedsPermissions
    expected: "399" but was: "409"  (line 263)
PlatformServiceTests          Tests run: 29, Failures: 0, Errors: 0
PlatformControllerTests       Tests run: 10, Failures: 0, Errors: 0
LeasingServiceTests           Tests run: 11, Failures: 0, Errors: 0
LeasingSignAtomicityJdbcIntegrationTests  Tests run: 2, Failures: 0, Errors: 0

Tests run: 64, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

不得把未运行测试写通过：上表 5 个类均本席本步实跑。唯一失败=017 环境，不折算为本模块源码缺陷。

### CMD-WEB

1. 隔离副本缺根 workspace 时 `npx vitest` → **exit 127** `vitest: not found`（未装依赖，如实）。
2. 仅链 web `node_modules`（无 vitest，hoist 在仓库根）且缺 `tsconfig.base.json` → **exit 1** `Tsconfig not found`。
3. 链根 `node_modules` 并复制 `tsconfig.base.json` 后：`npm run test:unit -- src/admin/pages/OrganizationMerchantPage.test.tsx` → **exit 0**

```
✓ src/admin/pages/OrganizationMerchantPage.test.tsx (12 tests) 535ms
Test Files  1 passed (1)
Tests  12 passed (12)
```

### CMD-HTML-LOCK（exit 0）

锁内映射 87 条中非 GEN 的 HTML 来源行邻域对照现行三 HTML：`checked 87 miss 0`。

临时副本结束清理：`rm -rf /tmp/m009-b5-r1-verify /tmp/m009-b5-r1-vitest-cache`。

---

## 9. 技能实用 / 缺口

已读并按 V0.17 收窄使用（用户/160 指令优先于技能本体；**不**派子代理、**不**写 `docs/arc/audits/`、**不**向操作员提问）：

| 技能 | 用法 |
|---|---|
| using-superpowers | 先核技能再取证；本任务以 160 V0.17 为唯一审核标准 |
| grilling | 只用追问树自问自答（§5）；不向操作员发问 |
| verification-before-completion | 先跑隔离命令再声称 PASS；Maven exit 1 不粉饰 |
| ponytail-review | 只列可删注释措辞，不落地 |
| codebase-design | PlatformService 为深模块；Jdbc/内存 fake 为仓储适配器；删除测试证明 20 文件有上位 |
| diagnosing-bugs / systematic-debugging | 017：读错误→复现 Flyway 409 vs 399→判定环境非源码 |
| writing-for-agents | 本 raw 给席甲/操作员读：步骤+完成判据同文件 |
| Arc `audit` | 只借机械核对/证据口径；**禁止**评审子代理与另写 arc 报告 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development 写新测试（B3 已落）、requesting-code-review（B4）、receiving-code-review、resolving-merge-conflicts、using-git-worktrees（阶段 B 不建 worktree）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未作为独立可调用项使用
- `grill-me`：本体转发 grilling；本环境按自问自答执行，不调用模型发问
- `ubiquitous-language`：未核到独立技能文件
- `git-guardrails-claude-code`：存在则需装 hook，本席只读不安装

明确不用：dispatching-parallel-agents、subagent-driven-development、executing-plans、research、domain-modeling、MCP。

---

## 10. 本席 B5 结论

**PASS**。

同时满足：20 文件均有三线覆盖记录；87 条映射判据均已核对；三线均执行；声称均有 2.3 四类证据之一；无未答 questions；无未定稿缺陷；无添加项未答；无 HTML 问题；锁行有效。017 红已复现并维持环境登记。无有条件通过。

本票不等于双席合取或模块 PASS。

## M009 第二批 B2-r1 整批归并复核（Grok 席乙）
文件自身版本：V0.02（本段追加；上方 V0.01 B5-r1 全文 35274 bytes 原样保留，不改写第 1 行锁段起点，本段不写独占锁哈希字段行）

- 席位：Grok（固定外审席乙）。步骤：第二批 B2-r1 归并复核。不是 B5 重审、不是修复、不是模块 PASS、不是 B3。
- 触发：B5-r1 合取 FAIL（席甲 FAIL／本席 PASS）。席甲三条无定稿 ID（CX-B5-01/02/03）按运行手册 §8 整批回 B2。本席不因 B5 判 PASS 拒绝复核。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M009 冻结 20 文件 / 87 判据）；ledger V2.07 IN_PROGRESS；B3 累计 1/3。
- 正本：`/home/james/complex-management` main `HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`（本席 `git rev-parse`）。B3 修复在工作区未提交，本席只读核源，不改。
- 本席本批 B2：**PASS**
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。三工具同值（`sha256sum` / `python3 hashlib` / `openssl dgst -sha256`）；锁文件 889020 字节；sidecar 全文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未用席甲本批 B2 追加段（当前 self-raw 仍为 V0.01 前缀，其后若有追加只保留不采信）。
- 席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本批不行使否决。
- 技能按 B2 收窄：grilling（自问自答取证，不向操作员发问）/ verification-before-completion（先核哈希与源码再给票）/ receiving-code-review（先核源与候选差集再接受或反驳，禁止表演性同意）/ ponytail-review（最懒可行，不落地代码）。未冒称 batch-grill-me。未派 Arc audit 子代理。

此票只判断 findings.md 顶部 V1.21「当前 M009 B5-r1 合取FAIL……第二批 B2-r1 整批候选（M009-026…035）」相对本次两份完整 B5-r1 raw 的全批差集、GEN-017 四分类、工程缺陷与修复队列。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B5 **PASS** 原文。已修 15 项（001–012、014–016）与 013/017–025 不在本批，保持不变。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.21，472063 bytes，SHA256 `f2748b48519a3b47de239f67223a33ebd552454be91ccf56453a0b839aff306b`。只读最上「第二批 B2-r1 整批候选」M009-026…035；下方他模块历史原文与 001–025 定稿不重审、不当本批依据。
- 席甲 B5-r1：`docs/audit-2026-09/modules/M009-b5-r1-self-raw.md` V0.01，15825 bytes，SHA256 `765da39b4e7a3de235dbc22f9e2c1e5b0ce3d083817ca94b3bf6bd2e6d01c284`（Claude Fable 5.1，作者自审票，结论 FAIL：CX-B5-01/02/03，OBS-B5-01…06；主仓实跑 74 条=72 通过+2 条 017 环境红）。
- 本席 B5-r1：本文件 V0.01 全文 35274 bytes，SHA256 `a2d884f0c39095ed06c9f6796df0b1fea053e2f5441d2151d68fe707352b074f`；结论 PASS，观察 GR-M009-B5-01…06，无新缺陷。
- 冻结判据：映射行 `M009 → [BEQR-063 … PGM-066]` COUNT=87 unique=87。本席独立三工具与 sidecar/ledger 三值全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。只用既有冻结判据，无 LOCK-AMEND。

### 差集（无遗漏、不改写本席 B5 原意）

| 本席 B5 | 席甲 B5 | 候选 ID | 差集结论 |
|---|---|---|---|
| GR-M009-B5-01 技术性观察，工程缺陷否 | CX-B5-01 技术性缺陷，建议入队 | 026 | 并入。B5 原意保留为「非新行为缺陷／观察」；B2 按 GEN-016 与 010 注释部分先例独立升格入队，见重点裁定。候选把「净删 3 处措辞」写成入队论据，属措辞改写，见误写节。 |
| （B5 未单列此项） | CX-B5-02 | 027 | 并入。本席 B5 未升缺陷不等于复核拒绝。独立核源后缺陷成立、入队，见重点裁定。 |
| （B5 未单列此项） | CX-B5-03 | 028 | 并入。独立按 BSM-004 原文裁定细化入队，**不**改否并入 020。席甲「促销、配套餐」非 HTML 原文，见误写节。 |
| GR-M009-B5-02 | （席甲无对等 CX） | 029 | 并入登记。不发明空间/字典状态闭集。 |
| GR-M009-B5-03 | （席甲无对等 CX） | 030 | 并入登记。006 后半 / 008 最小边界。 |
| GR-M009-B5-06 | （席甲无对等 CX） | 031 | 并入登记。008 激活投影边界。 |
| （B5 未单列） | OBS-B5-01 | 032 | 并入登记。 |
| （B5 未单列；GR-B5-04 是内存 fake remove） | OBS-B5-02 | 033 | 并入登记。与 GR-B5-04 分根，不另开、不并 010。 |
| （B5 未单列） | OBS-B5-03 | 034 | 并入登记。本席不升 GEN-001 缺陷，**不**并入 026。 |
| （B5 未单列） | OBS-B5-04 | 035 | 并入登记。HTML 对主体状态×绑定沉默。 |
| GR-M009-B5-04 内存 fake remove | （席甲 OBS-B5-02 相邻、非同一根） | 不新开 | 归既有 010→M011。候选未另开 ID，正确。 |
| GR-M009-B5-05 flyway 409 | 席甲 §2 两条 MigrationVersions 红 | 不新开 | 归既有 017。候选未另开 ID，正确。 |
| （无） | OBS-B5-05 库实态 / OBS-B5-06 技能 | 不新开 | 事实与技能记录，非 finding。正确。 |

席甲 CX-B5-01/02/03、OBS-B5-01…04 与本席 GR-M009-B5-01/02/03/06 均落在 026–035；GR-B5-04/05 归 010/017。无第三条根因、无新 ID、无 LOCK-AMEND、无 HTML 修改申请、无 2.2 添加项。

### 逐 ID 票（026–035）

| ID | 分类 | 工程缺陷 | 修复队列 | 本席票 | 理由（四分类→最懒处置） |
|---|---|---|---|---|---|
| 026 | 技术性（注释，010 同根因残留） | **是**（B5 观察→B2 升格） | **是** | PASS | 见重点裁定。三行改软删口径；编译级、无测试面。无业务发明。 |
| 027 | 技术性 | 是 | 是 | PASS | 见重点裁定。Jdbc 按主键覆写 `findSpaceById`；M011 JDBC 501 条红绿。无业务发明。 |
| 028 | 细化 | 是 | 是 | PASS | 见重点裁定。**不**改否、**不**并 020。`requireActiveMerchant` 仅三处创建路径。无业务发明。 |
| 029 | 技术性 | 否 | 否 | PASS | `createSpace:491` / `createSpaceType:445` `trimToDefault` 任意串。004 闭集只覆盖商户/主体/项目（BEQR-154/155）；映射无空间/字典状态枚举。HTML 无闭集，不发明。登记。 |
| 030 | 细化（观察） | 否 | 否 | PASS | `saveMerchant:741–743` 创建分支仍可写未校验存在的三主体列；更新不覆盖；绑定激活才投影。B2-r2 定稿 006 后半仅登记、008 仅激活投影。登记。 |
| 031 | 细化（观察） | 否 | 否 | PASS | `changeBusinessEntityBinding:620–622` 仅 `status=active` 时 `syncMerchantEntities`。008 最小边界仅创建/激活回写。停用清理会扩 008。登记。 |
| 032 | 技术性 | 否 | 否 | PASS | PUT 商户 `requireSubjectStatus` 空串缺省 active（`trimToDefault`）。PUT 为整档覆盖；HTML 无「空则保留原状态」。不发明。登记。 |
| 033 | 技术性 | 否 | 否 | PASS | 最高权限直删先 `saveMerchant(withDelete approved)` 再 `deleteMerchant`；Jdbc 双写同列幂等。保留为对齐内存 fake `remove`。登记。 |
| 034 | 技术性 | 否 | 否 | PASS | 见重点裁定。无引用 import 不升缺陷、不并 026。 |
| 035 | 细化（观察） | 否 | 否 | PASS | `requireBusinessEntity` 只 `businessEntityExists`，不读 status。HTML 对主体 terminated/blacklisted 与绑定关系沉默。不发明绑定禁则。登记。 |

队列「是」=026、027、028（3 项，B3-r2 范围）；「否」=029–035。已修 15 项与 013/017–025 保持否。添加 0，questions 0。

### 候选表对本席 B5 观察的误写（明确指出，不改写原意）

1. **026**：本席 B5 原文（§7.1 GR-M009-B5-01）工程缺陷=**否**；§5 对抗式 ponytail 句为「注释扫尾，净删约 3 处措辞即可，**不构成新行为缺陷**」。候选「位置与事实」把「Grok B5 亦称净删 3 处措辞」接在入队处置后，读成 B5 已支持入队。差集必须保留 B5「观察／缺陷否」；入队是本批 B2 **升格**，不是 B5 已判缺陷。定稿时工程缺陷列改为「是（B2 升格；B5 观察原意保留）」，删「Grok 亦称净删」当入队依据。
2. **028 / 席甲 CX-B5-03**：席甲写「不能再被拿去下单、促销、配套餐」。HTML 原文（`business-system-map-for-operations-v0.1.html:573` / 锁 BSM-004）是「下单、**配价、组套餐**」。候选沿用该转述。本席按原文+存档句裁定，不采信转述。
3. **031**「消费方归 020」沿用本席 B5 原句。020 实际是 `listMerchants()` 无过滤的跨模块读方。031 根因是 008 激活投影边界，不是 020。不另开、不改队列；定稿可把「消费方归 020」收成「008 最小边界，不扩停用清理」。
4. 其余 029/030/032/033/035 与 GR-B5-04→010/M011、GR-B5-05→017 未写错编号。

### 重点独立裁定

#### 026 三处残留「硬删」注释（B5 观察 → B2 升格入队）

本席 B5 GR-M009-B5-01 原意不改写：行为已是 `UPDATE delete_status='approved'`，不是新的可执行错误，故 B5 不因这三行把整模块判 FAIL。B2 问的是：该残留是否按 010 同标准进入本批修复队列。

本席本批类型 4（工作区现文，20 文件 grep `硬删|物理删除`）：

010 定稿四处**现文已改**（不再含「硬删/物理删除」）：

- `MerchantDeleteDecision.java:4–6` javadoc：软删除标记、行保留可查可恢复。
- `PlatformRepository.java:34` `deleteMerchant` javadoc：软删除、`delete_status='approved'`。
- `PlatformService.java:285–286` 审批 javadoc：软删除标记（B3 后原 :273「物理删除」句已不在）。
- `MerchantArchiveRequest.java:4`：reason 存档必填，与服务层一致。

010 四处中，MerchantArchiveRequest:3 与 PlatformService 原 :273 确为**纯注释**仍入队；MerchantDeleteDecision 与 `deleteMerchant` javadoc 同样是纯注释。010 另有 includeArchived / 恢复行为，**并不使注释部分变成「只有伴随行为才能入队」**。

本模块 20 文件仍写「硬删」的恰好三处，与候选逐字相符：

- `JdbcPlatformRepository.java:187`「>0 拒硬删」
- `MerchantRecord.java:27`「删除=硬删走审批」
- `PlatformRepository.java:54`「>0 则拒绝硬删」

行为对照：`JdbcPlatformRepository.java:181–183` `UPDATE merchants SET delete_status = 'approved'`。BSM-004 原文「删除只隐藏不抹除……不是真从数据库里抹掉」。GEN-016：「不得把计划或旧证据写成当前事实」。这三行把已作废的硬删口径写成当前文档事实，与 010 注释部分同一标准。

与 010 四处的区别：**不存在可支持「否」的区别。** 不是「占用检查注释可以继续叫硬删」——占用闸拦的就是这条软删路径。MerchantRecord:27 与 010 的 delete javadoc 同构。因此本席**不能**判否；若判否须 FAIL 并说明区别——说明不了，故升格入队，不行使否决。

分类：技术性（先于添加；无新业务规则）。工程缺陷：是。队列：是。无业务发明：只改与实现相反的旧词。最小处置：三行改为软删除口径（占用闸「>0 则拒绝删除/软删」即可）。不改行为、不加测试、不扩 M011 `PlatformServiceTests.java:675`「拒硬删」（M011 文件，归 010→M011 扫尾，本 ID 不强制）。不并 034。

#### 027 `findSpaceById` 默认走 `listSpaces()` LIMIT 500

本席 B5 未升此项。B2 独立核源（不采信席甲转述）：

- `PlatformRepository.java:79–82` 默认 `findSpaceById` = `listSpaces().stream()…`。注释写「空间量级小；Jdbc 可按需覆写」。
- `JdbcPlatformRepository.java:294–303` `listSpaces()` `ORDER BY created_at DESC, id LIMIT 500`。全文件 **无** `findSpaceById` 覆写（Jdbc 仅 1 处 `findSpaceById` 字面量=无）。
- 对照：`findMerchantById:168–178` 已 `WHERE id = :id`；`businessEntityExists:154–157` / `projectExists:161–164` 已 `COUNT(1) … WHERE id`（006/007）。空间存在性是 007 落地时沿用旧默认的残留。
- 消费方：`PlatformService.createSpace:477`（007 上级空间 404）；`PlatformService.spaceExists:397` → `LeasingService.java:424` `requireSpaceExistsIfPresent`。治理外 `ContractService.java:261/:954` 亦调 `platformRepository.findSpaceById`（爆心，不另开 ID）。
- `spaces` 表（V005）`project_id`/`merchant_id` 无 FK，501 条插入不造 501 个项目。

第 501 条及更早创建的空间在默认实现中对 `findSpaceById` 不可见 → 合法 parentSpaceId / 意向空间被拒。当前 `complex_test` 0 空间，潜伏；缺陷随数据量必然触发。

四分类：技术性（存在性判定错误，非 HTML 新规则）。GEN-006（引用关系：上级空间假阴性）、GEN-002（声称的按 id 存在性与实现不一致）。GEN-012 作辅：列表 LIMIT 500 对列表成立，套到主键存在性是错误边界；主判据仍是 006/002。无业务发明。

队列：是。最小处置（ponytail）：Jdbc 覆写 `findSpaceById` 为 `WHERE id = :id`（同 `findMerchantById`）。**不**撤 `listSpaces` LIMIT 500。一条覆写同时修 createSpace 与 `spaceExists`。TDD：1 条空间的查找在现码下仍绿（该行落在最近 500 内），**不能**当红；插入 501 条后断言 `created_at` 最早一条 `findSpaceById` 有值，才是能失败的最小红。M011 JDBC 测试归因本 ID，与 010 夹具同法。票 PASS。

#### 028 已存档/已批删商户仍可被新项目/空间/绑定引用（不并 020）

本席不为合取强行同意。独立读 HTML 原文（`docs/business-system-map-for-operations-v0.1.html:566–573`，锁 BSM-004 摘录同文）：

1. 配置类主数据点名 **商户档案**、票种、价格规则、酒吧商品/分类/套餐。
2. **存档（先下线）**：「从各处的选择列表里隐藏，**不再被新业务选用**；但历史照样能查、能对账。」
3. **下线就不能再用**：「已存档的**票种、商品等**，不能再被拿去**下单、配价、组套餐**——杜绝『明明下线了还能卖出去』。」
4. **删除只隐藏不抹除**：转入后台留存、前台不再显示，库中可查可审计可恢复。

第 3 条例示是票种/商品的交易再用，不是「禁止建项目」的专句；**第 2 条**对含商户档案在内的配置主数据写了「不再被新业务选用」。本席不以第 3 条例示扩成新禁则，而以第 2 条落到本模块**写路径**。

类型 4：

- `createProject:375–377`、`createSpace:472–475`、`createBusinessEntityBinding:546` 均 `requireMerchant`。
- `requireMerchant:337–339` = `findMerchantById`，Jdbc `:168–178` **不**过滤 `archived_at` / `delete_status`（更新/取消存档/删除申请必须命中已存档行，这是对的）。
- `listMerchants(false)` 已排除已存档+已批删（010）。列表隐藏、写路径仍接受客户端传来的已下线 id = 仅靠界面隐藏（GEN-004/005 禁止的形态）。

与 020 的区别（故**不改否、不并 020**）：020 是治理外 `ContractService` / 计费等调用无过滤 `listMerchants()` 的**读侧消费方**，定稿「由消费模块判定是否改用 `listMerchants(false)`；本模块保留两种方法」。028 是 **M009 自己的创建写路径**把已下线商户选为新项目/新空间/新绑定的经营对象，属「新业务选用」，实现面在本 20 文件。并入 020 会把本模块写闸推给合同/计费，修不到这三处。

不是 HTML 沉默：沉默的是「主体 terminated 能否绑定」（035），不是存档后能否再被选去建新归属。不是添加：不新造状态机、缓冲期、退出流程（018），不拦 `updateMerchant` / `unarchiveMerchant` / 删除申请审批（须命中已存档行）。分类细化（原则在、写路径缺口）。GEN-004（输入在首次使用前校验「可选用」）/ GEN-006（下线后仍增生引用，与删前占用检查互锁）。

队列：是。最小处置：新增 `requireActiveMerchant`（存在且 `archived_at==null` 且 `delete_status≠approved`，否则 400 中文，内部码 `MERCHANT_ARCHIVED` 属标识符例外），**只**用于上述三处创建；`requireMerchant` 其余路径不变。ServiceTests 各一条最小用例。不改列表 API、不改 020 消费方。票 PASS。

#### 034 无引用 import（不升缺陷、不并 026）

独立核：`PlatformController.java:7` `import …ApiException` 全文件仅此一行；`JdbcPlatformRepository.java:15` `import java.math.BigDecimal` 文件内无 `BigDecimal` 类型名（`getBigDecimal` 是方法名，不消费该 import）。HEAD 既有、非 B3 引入。GEN-001 可解释为「无用途应删」，但：无执行面、javac 不红、与 016（无调用的私有方法，已修）不是同一级死代码；026 根因是硬删口径，不是 import。并入 026 会扩 010 同根注释项。保持登记、队列否。票 PASS。

### 029–035 登记项合规性（逐条）

- 029：技术性、缺陷否、队列否。不发明空间状态枚举。合规。
- 030：细化观察、否、否。不回头扩 006 后半。合规。
- 031：细化观察、否、否。不扩 008 到停用清理。合规。
- 032：技术性、否、否。不发明「空 status 保留原值」。合规。
- 033：技术性、否、否。双写为 fake/Jdbc 语义对齐，不判缺陷。合规。
- 034：见上。合规。
- 035：细化观察、否、否。不发明绑定禁 terminated。合规。

是/否队列：入队三项均有本模块可修实现面与冻结判据；登记七项或无 HTML 闭集、或已定稿最小边界、或无执行面。不把登记项塞进 B3-r2。

### 最小修正意见（定稿措辞；**不**构成否决）

队列已与本席票一致（是=026/027/028，否=029–035），故整批 PASS，**不**要求 B2-r2。定稿时仅改表内措辞，不改队列：

1. 026 工程缺陷定为「是（B2 升格；B5 观察原意保留）」。删除「Grok B5 亦称净删 3 处」作为入队依据的写法。
2. 028 HTML 引用改为原文「不再被新业务选用」+「下单、配价、组套餐」；写明不并 020。
3. 031 可将「消费方归 020」收成「008 最小边界」。
4. 034 保持否，不并 026。
5. 已修 15 项与 013/017–025 不动。B3-r2 只修 026/027/028 及归因最小测试；不改 HTML/判据/权限种子/迁移。

### 本席实跑（B2，只读）

- 锁文件三工具：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- 输入 SHA/字节：self-raw 15825/`765da39b…c284`；本文件前缀 35274/`a2d884f0…074f`；findings V1.21 472063/`f2748b48…306b`
- HEAD=`242f487233d028173cb9198eecd079f5d877405c` = origin/main
- 20 文件 `硬删|物理删除`、`findSpaceById`、`listSpaces` LIMIT 500、`requireMerchant` 三处创建、`requireSubjectStatus`/`trimToDefault`、`syncMerchantEntities`、无引用 import：仓库 grep + 读源
- BSM-004 HTML 原文 L566–573 对照锁摘录
- 未在主仓跑写模式 mvn/vitest；未对业务库提交；未建 /tmp 验证副本（本步只读归并，不重复 B5 套件）

### 结论

本席第二批 B2-r1：**PASS**

026 升格入队（与 010 注释无区别，不能判否）；027 工程缺陷成立、501 条测试是能红的最小 TDD；028 按「不再被新业务选用」细化入队，不并 020、不为合取同意。029–035 否队列合规。差集无漏并。无添加项需 2.2。无 HTML 修改申请。无条件通过。本票只覆盖本批归并，**不是**模块 PASS，**不是** B5 合取，**不是**已修 026–028。
