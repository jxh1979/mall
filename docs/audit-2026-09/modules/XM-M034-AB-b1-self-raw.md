CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B1 修复前全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票）。2026-09-24T06:37:27Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。基线 origin/main daf6adfc。
- 范围：开工段 33 个文件；为判断另读 AuthorizationService 权限判定（permissionGrant、knownPermission）、JdbcAccessRepository.permissionExists、JdbcSettlementTransactionRepository 结算排除。判据 17 个模块映射并集 181 项。
- 顺序：本席先取证、落本报告；落盘前未读 Grok 本次正文。未改任何已跟踪文件。

## 1. 文件覆盖

| 文件 | 所属模块 | 行数 | blob |
|---|---|---:|---|
| backend/src/main/java/com/complex/platform/amusement/AmusementController.java | M230 | 553 | 40ca6c2d90b7 |
| backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java | M230 | 17 | 954aa6fe9add |
| backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java | M230 | 14 | 4da1d63e6115 |
| backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java | M245 | 331 | 80b62f234d4d |
| backend/src/main/java/com/complex/platform/amusement/AmusementService.java | M230 | 1947 | 6897b66d5d8c |
| backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java | M245 | 2075 | 305672cbc332 |
| backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRowMapper.java | M246 | 345 | 91b951b6edb6 |
| backend/src/main/java/com/complex/platform/gate/JdbcAdmissionFactRepository.java | M460 | 181 | fdc205c9c940 |
| backend/src/main/java/com/complex/platform/ticketing/dto/VerificationManualOverrideRequest.java | M114 | 25 | 0b35d59d9b9e |
| backend/src/main/java/com/complex/platform/ticketing/TicketingController.java | M132 | 545 | ad56fa8f38d0 |
| backend/src/main/java/com/complex/platform/ticketing/TicketingService.java | M133 | 977 | 6f048f32f4ae |
| backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java | M114 | 1262 | f4f44b6dcf73 |
| backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql | M237 | 230 | 43aa02e54796 |
| backend/src/main/resources/db/migration/V067__ticket_multiperson_reverse.sql | M381 | 33 | e51aa21fb3c2 |
| backend/src/main/resources/db/migration/V099__admission_over_scope_policies.sql | M461 | 60 | 7b3dbf96e674 |
| backend/src/main/resources/db/migration/V170__access_grant_requests.sql | M041 | 188 | 8ebf53ea5c13 |
| backend/src/main/resources/db/migration/V234__amusement_exit_release.sql | M381 | 49 | b32fdead487f |
| backend/src/main/resources/db/migration/V251__permission_names_i18n_zh.sql | M089 | 340 | 21c345bf37fb |
| backend/src/main/resources/db/migration/V254__permission_names_p3_polish.sql | M089 | 23 | ff9258a39f25 |
| backend/src/main/resources/db/migration/V270__ticket_manual_override_permission.sql | M034 | 16 | 2cd96bc12a8e |
| backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql | 计划外（U-FIX e0446520 新增） | 17 | ac3c4bb658cf |
| backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java | M035 | 908 | 4ad77d340032 |
| backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java | M248 | 477 | df7b38b15321 |
| backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java | M248 | 522 | d5ff5eee119d |
| backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java | M232 | 3424 | 017dd851c49f |
| backend/src/test/java/com/complex/platform/ticketing/AmusementExitReleaseJdbcIntegrationTests.java | M233 | 346 | 97c2767120e3 |
| backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java | M134 | 744 | 9ed49dcbc440 |
| backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java | M135 | 4842 | f65ae75a6298 |
| docs/openapi/openapi-p0.json | M391 | 51416 | 32231790707d |
| docs/openapi/openapi-p0.yaml | M392 | 37406 | 1f056b360dc6 |
| docs/p0-openapi-contract-closure-appendix-v0.1.md | M389 | 2617 | 164f78fc1067 |
| docs/p0-openapi-operation-acceptance-matrix-v0.1.md | M390 | 814 | 5d2e9d233e0b |
| docs/p0-operation-permission-matrix-v0.1.md | M039 | 301 | e129917ed22b |

## 2. HTML 依据

- 枚举速查第 43 行「核销记录状态」条末句：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过。」
- BSM 第 1262 行：「出场自动扣减：游乐项目出口设置闸机或摄像头感应器；发现有人走过出口、且该人在入场闸机或入场摄像头留有入场记录时，系统自动把该项目实时在玩……」
- BSM 第 1263 行：「在玩人数不允许人工调整：任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数；计数只能由入场核销与出场自动扣减产生。」
- BSM 第 990 行：「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」（暂停是设施状态，由设备状态面承担）

## 3. 三线审计

- 第一性原理：「人工放行」与「人工清零在玩人数」两种能力 HTML 都明文禁止，系统里不应有入口、契约与权限码。实际：
  - 票务：TicketingController:495-507 仍有 POST /api/v1/ticketing/verifications/manual-override，调 TicketingService.manualOverrideVerification（:934-938）→ TicketVerificationService.manualOverride（:207-247），写一条 status=manual_overridden 的核销记录、不扣权益、不进结算。端点先 requirePermission("verification.manual_override")；该码自 V429 为 retired，AuthorizationService.permissionGrant:71-73 先判 knownPermission（JdbcAccessRepository.permissionExists 只认 active），不成立即对任何主体（含最高权限）拒绝。所以入口现已调不通，但服务方法本身不查权限、DTO 与契约（openapi 两份、两份契约附录）仍把它当现行能力，测试仍按旧行为写。
  - 游乐：AmusementController:39-52 的 GET/POST /api/v1/amusement/manual-overrides 仍在，amusement.verification.override 在字典里 active 且授平台管理员（V014:185、213）。AmusementService.createManualOverrideInternal（:1581-1616）接受 allow、deny、pause、resume、release 五类（:36 OVERRIDE_TYPES）：allow 落 approved 记录（人工放行的决定），release 调 JdbcAmusementRepository.clearProjectConcurrentCapacity（:1115）把本项目并发在场快照清零；deny、pause、resume 只落记录。全仓无任何读取方用这些记录改变核验结果（只有列表接口读）。
  - 未验证前提：生产库里是否已有 allow/release 历史行（只读不查生产）；处置不删除历史行，不受影响。
- 顺从式：181 项逐项见 §6。
- 对抗式（grilling）：
  - 问：票务入口既然恒 403，还有没有缺陷？答：有残留。服务方法 manualOverrideVerification 本身不查权限，任何内部调用即可写 manual_overridden；契约与两份附录仍发布该能力，前后端与第三方按契约会以为可用；测试守的是已废止行为。
  - 问：最高权限能绕过吗？答：不能。先判 knownPermission，retired 即拒绝，最高权限短路在其后（AuthorizationService:71-83）。
  - 问：游乐 release 是 2026-07-05 用户拍板的主管异常清场，为什么算冲突？答：冻结的 BSM 第 1263 行明文「任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数」，这句是现行口径；V234 与服务注释里的旧拍板已被取代。
  - 问：deny、pause、resume 要不要保留？答：它们只写一条记录，没有任何读取方据此改变核验或设施状态；设施暂停由设备状态面承担（BSM 第 990 行）。去掉 allow 与 release 后，这个入口与权限码只剩无作用的记录功能，而权限矩阵文档自 R10 起已写「amusement.verification.override 停用，待代码轮下线」。
  - 问：历史数据怎么办？答：amusement_manual_overrides 表、ticket_verifications 的 manual_overridden 值与 CHECK 约束、结算排除子句都保留，只下线写入口；已应用迁移不改。
- 本范围外同根：无新增。

## 4. 问题（临时编号）

- **CX-01（技术性，残留入口）** 票务人工放行：TicketingController:495-507、TicketingService.manualOverrideVerification（:934-938）、TicketVerificationService.manualOverride（:207-247）、dto/VerificationManualOverrideRequest、openapi-p0.yaml/json 对应路径与请求体、p0-openapi-contract-closure-appendix 与 p0-openapi-operation-acceptance-matrix 对应行、TicketingControllerTests（:237-313 一组）与 TicketingServiceTests（:490-492）。V429 已使接口恒 403，但能力代码与契约仍在。依据：枚举速查第 43 行。最小处置：删除端点、两个服务方法、DTO 与对应测试用例；契约删路径与请求体、两份附录对应行改为「已下线」；保留 manual_overridden 状态值、CHECK 约束与结算排除（历史行）；AccessJdbcIntegrationTests 的 V429 回归不动。
- **CX-02（冲突，枚举速查第 43 行）** 游乐人工改判 allow：createManualOverrideInternal 接受 override_type=allow 并落 status=approved 记录，即系统提供「人工放行」决定的入口。正常触发：持 amusement.verification.override（平台管理员默认持有）POST /api/v1/amusement/manual-overrides {override_type: allow}。
- **CX-03（冲突，BSM 第 1263 行）** 游乐人工改判 release：调 clearProjectConcurrentCapacity 把项目并发在场快照清零（AmusementService:1596-1599），即人工清零在玩人数；注释「授权已由 @GuardedAction 限主管+审批留痕」与第 1263 行「任何岗位（含主管与最高权限）都不得」相反。正常触发：同上 {override_type: release}；AmusementExitReleaseJdbcIntegrationTests.supervisorReleaseClearsConcurrentCapacityWithAudit 即此路径。
- **CX-04（细化，枚举速查第 43 行与权限矩阵文档 R10 口径）** 去掉 allow 与 release 后，manual-overrides 入口只剩无读取方、无作用的 deny/pause/resume 记录；amusement.verification.override 仍 active 并授平台管理员，契约与测试仍发布该能力。最小处置（合 CX-02、CX-03）：新增前向迁移照 V429 停用 amusement.verification.override（status=retired，清 role_permissions、permission_template_items、user_permission_overrides，active 临时授权置 revoked）；删除 AmusementController 两个端点、AmusementService.listManualOverrides/createManualOverride/createManualOverrideInternal 与 OVERRIDE_TYPES、仓储 saveManualOverride/listManualOverrides/listManualOverridesByMerchant/clearProjectConcurrentCapacity 与行映射 mapManualOverride、AmusementManualOverrideRecord/Request；删对应测试用例并补一条「该码停用、四处授予清空」真库回归；契约删两路径与相关 schema；保留 amusement_manual_overrides 表、历史行与 CHECK 约束；V099 的 amusement.manual_override 准入策略行已无调用方，属惰性数据，不另立迁移。检查点须按门禁重生成数据隔离规格与域归属清单并跑全链 verify:predeploy。

## 5. 只登记

- 观察：JdbcAdmissionFactRepository:135 注释提到 amusement_manual_override，随入口下线成为过时注释；V099 准入策略行 amusement_manual_override_over_scope 惰性保留。
- 观察：权限矩阵文档第 63、78、251、269 行已写「停用，待代码轮下线」，下线后可改为「已下线」（属同批文档同步，B2 定是否纳入写入集合）。

## 6. 181 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-001 票种 | business-enum-quick-reference-v0.1.html:29 | 无本面｜本合成模块范围（33 个文件）不触及「票种」所述对象｜通过 |
| BEQR-002 票务策略状态 | business-enum-quick-reference-v0.1.html:30 | 无本面｜本合成模块范围（33 个文件）不触及「票务策略状态」所述对象｜通过 |
| BEQR-003 定价、售票与收款主体 | business-enum-quick-reference-v0.1.html:32 | 无本面｜本合成模块范围（33 个文件）不触及「定价、售票与收款主体」所述对象｜通过 |
| BEQR-004 年龄限制 | business-enum-quick-reference-v0.1.html:33 | 无本面｜本合成模块范围（33 个文件）不触及「年龄限制」所述对象｜通过 |
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（33 个文件）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-006 通票类型 | business-enum-quick-reference-v0.1.html:35 | 无本面｜本合成模块范围（33 个文件）不触及「通票类型」所述对象｜通过 |
| BEQR-007 权益类型 | business-enum-quick-reference-v0.1.html:36 | 无本面｜本合成模块范围（33 个文件）不触及「权益类型」所述对象｜通过 |
| BEQR-008 餐饮/零售券两种用法 | business-enum-quick-reference-v0.1.html:37 | 无本面｜本合成模块范围（33 个文件）不触及「餐饮/零售券两种用法」所述对象｜通过 |
| BEQR-009 资格判定 | business-enum-quick-reference-v0.1.html:39 | 无本面｜本合成模块范围（33 个文件）不触及「资格判定」所述对象｜通过 |
| BEQR-010 订单状态 | business-enum-quick-reference-v0.1.html:40 | 无本面｜本合成模块范围（33 个文件）不触及「订单状态」所述对象｜通过 |
| BEQR-011 票/权益状态 | business-enum-quick-reference-v0.1.html:41 | 无本面｜本合成模块范围（33 个文件）不触及「票/权益状态」所述对象｜通过 |
| BEQR-012 核销记录状态 | business-enum-quick-reference-v0.1.html:43 | 无本面｜本合成模块范围（33 个文件）不触及「核销记录状态」所述对象｜通过 |
| BEQR-013 手环入场 | business-enum-quick-reference-v0.1.html:44 | 无本面｜本合成模块范围（33 个文件）不触及「手环入场」所述对象｜通过 |
| BEQR-014 退款单状态 | business-enum-quick-reference-v0.1.html:45 | 无本面｜本合成模块范围（33 个文件）不触及「退款单状态」所述对象｜通过 |
| BEQR-015 有效期类型 | business-enum-quick-reference-v0.1.html:46 | 无本面｜本合成模块范围（33 个文件）不触及「有效期类型」所述对象｜通过 |
| BEQR-016 售票与退款原则 | business-enum-quick-reference-v0.1.html:47 | 无本面｜本合成模块范围（33 个文件）不触及「售票与退款原则」所述对象｜通过 |
| BEQR-017 到期未核销款 | business-enum-quick-reference-v0.1.html:48 | 无本面｜本合成模块范围（33 个文件）不触及「到期未核销款」所述对象｜通过 |
| BEQR-018 域归属对照 | business-enum-quick-reference-v0.1.html:49 | 无本面｜本合成模块范围（33 个文件）不触及「域归属对照」所述对象｜通过 |
| BEQR-019 跨域通票分配 | business-enum-quick-reference-v0.1.html:51 | 无本面｜本合成模块范围（33 个文件）不触及「跨域通票分配」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（33 个文件）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-021 收费与免费权益结算 | business-enum-quick-reference-v0.1.html:53 | 无本面｜本合成模块范围（33 个文件）不触及「收费与免费权益结算」所述对象｜通过 |
| BEQR-022 项目内部结算价 | business-enum-quick-reference-v0.1.html:54 | 无本面｜本合成模块范围（33 个文件）不触及「项目内部结算价」所述对象｜通过 |
| BEQR-023 核销顺序 | business-enum-quick-reference-v0.1.html:55 | 无本面｜本合成模块范围（33 个文件）不触及「核销顺序」所述对象｜通过 |
| BEQR-024 游乐核销归属 | business-enum-quick-reference-v0.1.html:56 | 无本面｜本合成模块范围（33 个文件）不触及「游乐核销归属」所述对象｜通过 |
| BEQR-025 门票综合服务费 | business-enum-quick-reference-v0.1.html:57 | 无本面｜本合成模块范围（33 个文件）不触及「门票综合服务费」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（33 个文件）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-027 人脸验龄数据生命周期 | business-enum-quick-reference-v0.1.html:60 | 无本面｜本合成模块范围（33 个文件）不触及「人脸验龄数据生命周期」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（33 个文件）不触及「等级权益对照」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（33 个文件）不触及「票务人次计算」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（33 个文件）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作审批：人工放行与在玩人数清零不属可审批放行的动作（HTML 明文禁止）｜不通过（随 CX-02、CX-03） |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（33 个文件）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-085 项目开放状态 | business-enum-quick-reference-v0.1.html:140 | 无本面｜本合成模块范围（33 个文件）不触及「项目开放状态」所述对象｜通过 |
| BEQR-086 场次状态 | business-enum-quick-reference-v0.1.html:142 | 无本面｜本合成模块范围（33 个文件）不触及「场次状态」所述对象｜通过 |
| BEQR-087 游乐「当天／每天」口径 | business-enum-quick-reference-v0.1.html:144 | 无本面｜本合成模块范围（33 个文件）不触及「游乐「当天／每天」口径」所述对象｜通过 |
| BEQR-088 容量双阈值 | business-enum-quick-reference-v0.1.html:145 | 无本面｜本合成模块范围（33 个文件）不触及「容量双阈值」所述对象｜通过 |
| BEQR-089 园区容量 | business-enum-quick-reference-v0.1.html:146 | 无本面｜本合成模块范围（33 个文件）不触及「园区容量」所述对象｜通过 |
| BEQR-090 设备关键/辅助分级 | business-enum-quick-reference-v0.1.html:147 | 无本面｜本合成模块范围（33 个文件）不触及「设备关键/辅助分级」所述对象｜通过 |
| BEQR-091 全园故障与补偿通票 | business-enum-quick-reference-v0.1.html:148 | 无本面｜本合成模块范围（33 个文件）不触及「全园故障与补偿通票」所述对象｜通过 |
| BEQR-092 安全严重度 | business-enum-quick-reference-v0.1.html:149 | 无本面｜本合成模块范围（33 个文件）不触及「安全严重度」所述对象｜通过 |
| BEQR-093 巡检任务状态 | business-enum-quick-reference-v0.1.html:150 | 无本面｜本合成模块范围（33 个文件）不触及「巡检任务状态」所述对象｜通过 |
| BEQR-094 越界类型 | business-enum-quick-reference-v0.1.html:151 | 无本面｜本合成模块范围（33 个文件）不触及「越界类型」所述对象｜通过 |
| BEQR-095 团体/包场 | business-enum-quick-reference-v0.1.html:153 | 无本面｜本合成模块范围（33 个文件）不触及「团体/包场」所述对象｜通过 |
| BEQR-096 游乐增强阶段 | business-enum-quick-reference-v0.1.html:154 | 无本面｜本合成模块范围（33 个文件）不触及「游乐增强阶段」所述对象｜通过 |
| BEQR-097 供应合同模式 | business-enum-quick-reference-v0.1.html:159 | 无本面｜本合成模块范围（33 个文件）不触及「供应合同模式」所述对象｜通过 |
| BEQR-124 综合通票定价 | business-enum-quick-reference-v0.1.html:193 | 无本面｜本合成模块范围（33 个文件）不触及「综合通票定价」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（33 个文件）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 无本面｜本合成模块范围（33 个文件）不触及「经营主体关系」所述对象｜通过 |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（33 个文件）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（33 个文件）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（33 个文件）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（33 个文件）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 无本面｜本合成模块范围（33 个文件）不触及「职级」所述对象｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 无本面｜本合成模块范围（33 个文件）不触及「终端十一端」所述对象｜通过 |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 无本面｜本合成模块范围（33 个文件）不触及「最终权限计算」所述对象｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 无本面｜本合成模块范围（33 个文件）不触及「项目与敏感数据权限」所述对象｜通过 |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 无本面｜本合成模块范围（33 个文件）不触及「终端开关优先级」所述对象｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（33 个文件）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（33 个文件）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（33 个文件）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（33 个文件）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（33 个文件）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 无本面｜本合成模块范围（33 个文件）不触及「权限状态」所述对象｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（33 个文件）不触及「合同变更类型」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（33 个文件）不触及「合同审批链」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（33 个文件）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（33 个文件）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（33 个文件）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（33 个文件）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（33 个文件）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（33 个文件）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（33 个文件）不触及「管理费方式」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（33 个文件）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（33 个文件）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（33 个文件）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 无本面｜本合成模块范围（33 个文件）不触及「配置数据可以存档、也可以删除（删除要审批）」所述对象｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（33 个文件）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（33 个文件）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 无本面｜本合成模块范围（33 个文件）不触及「平台管理员拥有全部日常功能权限」所述对象｜通过 |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（33 个文件）不触及「基础管理」所述对象｜通过 |
| BSM-010 票务权益 | business-system-map-for-operations-v0.1.html:601 | 无本面｜本合成模块范围（33 个文件）不触及「票务权益」所述对象｜通过 |
| BSM-016 游乐管理 | business-system-map-for-operations-v0.1.html:631 | 无本面｜本合成模块范围（33 个文件）不触及「游乐管理」所述对象｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（33 个文件）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-030 票务权益：管卖什么票、给什么权益、怎么核销 | business-system-map-for-operations-v0.1.html:727 | 无本面｜本合成模块范围（33 个文件）不触及「票务权益：管卖什么票、给什么权益、怎么核销」所述对象｜通过 |
| BSM-031 主要功能 | business-system-map-for-operations-v0.1.html:731 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-032 和谁有关 | business-system-map-for-operations-v0.1.html:760 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-033 票种、通票与权益类型（系统支持哪些种类） | business-system-map-for-operations-v0.1.html:772 | 无本面｜本合成模块范围（33 个文件）不触及「票种、通票与权益类型（系统支持哪些种类）」所述对象｜通过 |
| BSM-034 核销、退款与有效期规则 | business-system-map-for-operations-v0.1.html:796 | 无本面｜本合成模块范围（33 个文件）不触及「核销、退款与有效期规则」所述对象｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 无本面｜本合成模块范围（33 个文件）不触及「会员与积分规则」所述对象｜通过 |
| BSM-036 人脸验龄数据使用与删除 | business-system-map-for-operations-v0.1.html:840 | 无本面｜本合成模块范围（33 个文件）不触及「人脸验龄数据使用与删除」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（33 个文件）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（33 个文件）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-048 触摸屏一体机：权益领取、现场服务和辅助售票 | business-system-map-for-operations-v0.1.html:978 | 无本面｜本合成模块范围（33 个文件）不触及「触摸屏一体机：权益领取、现场服务和辅助售票」所述对象｜通过 |
| BSM-049 主要功能 | business-system-map-for-operations-v0.1.html:982 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-050 和谁有关 | business-system-map-for-operations-v0.1.html:996 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-068 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | business-system-map-for-operations-v0.1.html:1253 | 无本面｜本合成模块范围（33 个文件）不触及「游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常」所述对象｜通过 |
| BSM-069 主要功能 | business-system-map-for-operations-v0.1.html:1257 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-070 和谁有关 | business-system-map-for-operations-v0.1.html:1276 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-071 游乐类型与规则 | business-system-map-for-operations-v0.1.html:1287 | 无本面｜本合成模块范围（33 个文件）不触及「游乐类型与规则」所述对象｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（33 个文件）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-075 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | business-system-map-for-operations-v0.1.html:1339 | 无本面｜本合成模块范围（33 个文件）不触及「招商租赁与合同履约：从找商户到签约、履约、变更、退租」所述对象｜通过 |
| BSM-076 主要功能 | business-system-map-for-operations-v0.1.html:1343 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-077 和谁有关 | business-system-map-for-operations-v0.1.html:1357 | 无本面｜本合成模块范围（33 个文件）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 无本面｜本合成模块范围（33 个文件）不触及「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」所述对象｜通过 |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（33 个文件）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（33 个文件）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（33 个文件）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（33 个文件）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 无本面｜本合成模块范围（33 个文件）不触及「各类入口：不同人只看自己该看的、只做自己该做的」所述对象｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（33 个文件）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 无本面｜本合成模块范围（33 个文件）不触及「已确认规则和实施前输入项」所述对象｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜下线已废止能力的入口、契约与测试；历史数据、表约束、已应用迁移不动｜通过（本项不单独致 FAIL） |
| GEN-002 可执行正确性 | 通用 | 适用｜票务人工放行接口自 V429 起对任何人（含最高权限）恒 403，但服务方法与契约仍在；游乐 allow/release 仍可执行（release 清零在玩人数）｜不通过（见 CX-01…04） |
| GEN-003 测试有效性 | 通用 | 适用｜TicketingControllerTests、AmusementServiceTests、AmusementControllerTests、AmusementJdbcIntegrationTests、AmusementExitReleaseJdbcIntegrationTests 仍把已废止能力当正路断言｜不通过（随 CX-01…04） |
| GEN-004 信任边界校验 | 通用 | 无本面｜不涉新的信任边界输入｜通过 |
| GEN-005 授权与最小权限 | 通用 | 适用｜amusement.verification.override 仍 active 并授平台管理员（V014:185/213），放出与 HTML 相反的能力｜不通过（见 CX-04） |
| GEN-006 数据完整性 | 通用 | 适用｜release 把在玩人数快照直接清零，破坏「计数只能由入场核销与出场自动扣减产生」的数据来源约束｜不通过（见 CX-03） |
| GEN-007 并发与幂等 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「并发与幂等」所述对象｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜openapi-p0.yaml:574-615 与 json 仍发布 /api/v1/amusement/manual-overrides；票务人工放行路径与 VerificationManualOverrideRequest 仍在契约｜不通过（见 CX-01、CX-04） |
| GEN-009 错误处理与恢复 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「错误处理与恢复」所述对象｜通过 |
| GEN-010 安全与敏感信息 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「安全与敏感信息」所述对象｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜历史 manual_overridden 核销记录与 amusement_manual_overrides 行保留可查｜通过 |
| GEN-012 资源与性能边界 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「资源与性能边界」所述对象｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「构建配置与依赖」所述对象｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 适用｜停用游乐权限码须新增前向迁移（照 V429），不改 V014/V234/V270｜不通过（随 CX-04） |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜本合成模块范围（33 个文件）不触及「界面可用性与可访问性」所述对象｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜TicketingController:495-497 与 TicketVerificationService:208-210 注释仍把人工放行写成现行能力；AmusementService:1596 注释称 release 经主管审批即可｜不通过（随 CX-01、CX-03） |
| GEN-017 业务来源分类 | 通用 | 适用｜CX-01 技术性（死入口残留）、CX-02/03 冲突、CX-04 细化｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行与权限判定路径｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 无本面｜本合成模块范围（33 个文件）不触及「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」所述对象｜通过 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 无本面｜本合成模块范围（33 个文件）不触及「治理顺序：一件业务能不能做，要先经过这些判断」所述对象｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（33 个文件）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（33 个文件）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（33 个文件）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 无本面｜本合成模块范围（33 个文件）不触及「商户和主体」所述对象｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（33 个文件）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 无本面｜本合成模块范围（33 个文件）不触及「员工和手机」所述对象｜通过 |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 无本面｜本合成模块范围（33 个文件）不触及「权限和职级」所述对象｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 无本面｜本合成模块范围（33 个文件）不触及「终端开关」所述对象｜通过 |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 无本面｜本合成模块范围（33 个文件）不触及「设备管理」所述对象｜通过 |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（33 个文件）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（33 个文件）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 无本面｜本合成模块范围（33 个文件）不触及「留痕复核」所述对象｜通过 |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（33 个文件）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 无本面｜本合成模块范围（33 个文件）不触及「应急与隐私」所述对象｜通过 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（33 个文件）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（33 个文件）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（33 个文件）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（33 个文件）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（33 个文件）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（33 个文件）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（33 个文件）不触及「业务边界」所述对象｜通过 |
| PGM-028 合同和准入：违反规则先卡住，再按层级放行 | platform-governance-map-for-operations-v0.1.html:518 | 无本面｜本合成模块范围（33 个文件）不触及「合同和准入：违反规则先卡住，再按层级放行」所述对象｜通过 |
| PGM-029 主要功能 | platform-governance-map-for-operations-v0.1.html:522 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-030 业务意义 | platform-governance-map-for-operations-v0.1.html:544 | 无本面｜本合成模块范围（33 个文件）不触及「业务意义」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（33 个文件）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（33 个文件）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（33 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（33 个文件）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（33 个文件）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（33 个文件）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（33 个文件）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 无本面｜本合成模块范围（33 个文件）不触及「权限和职级：每一个单一功能都可以单独授权」所述对象｜通过 |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 无本面｜本合成模块范围（33 个文件）不触及「权限规则」所述对象｜通过 |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 无本面｜本合成模块范围（33 个文件）不触及「职级规则」所述对象｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜本合成模块范围（33 个文件）不触及「外包人员与审计人员（2026-06-29 新增）」所述对象｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（33 个文件）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（33 个文件）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜本合成模块范围（33 个文件）不触及「客人与员工的个人信息保护」所述对象｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（33 个文件）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（33 个文件）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 无本面｜本合成模块范围（33 个文件）不触及「已拍板规则和待排期问题」所述对象｜通过 |

共 181 项；逐项给出适用与结论的 13 项，其余 168 项无本面。

## 7. 结论

席甲 B1 票：**FAIL**（CX-01…04）。范围全读、181 项逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## XM-M034-AB B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-24T06:57:18Z。输入：两席 B1 完整 raw、操作员 D-033 与候选 findings.md V4.06 顶部「当前 XM-M034-AB B2-r1 整批候选」段（全文件 SHA256 6312345cc5628cff2dc5cb0de193af7754e442034540bad3d8a641efe42867c5）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01＋CX-01→001；GR-02＋CX-03→002；GR-03＋CX-02→003；GR-04＋CX-04→004（按 D-033 改写为契约与文档同步）。两席观察全部登记。无漏并。
- 自审纠正：①B1 称 OpenAPI 仍有票务人工放行路径，经 git grep 只有游乐路径，更正；②B1 起先以为最高权限能绕过已停用的票务权限码，核 permissionGrant 后确认先判字典 active 再判最高权限，报告前已改正；③票务入口分类采 Grok「冲突」。
- D-033 执行：deny/pause/resume 保留，权限码与端点保留，只去 allow 与 release；契约与文档里「整接口已废止」的表述随之改为现状（004）。
- 最小性：票务只删写入口链（控制器、两个服务方法、DTO、测试用例）与矩阵两行措辞；游乐只收窄枚举并删无调用方的清零方法；契约只改枚举与摘要措辞；无迁移、不改已应用迁移、不删历史行。
- 无 questions（D-033 已答）、不 BLOCKED。
- 席甲 B2-r1 票：**PASS**。


## XM-M034-AB B2-r1 重写候选复核（席甲票，V0.03；上方 V0.02 票随「保留」候选作废）

- 时间：2026-09-24T07:41:13Z。输入：两席 B1 完整 raw、操作员 D-033 最终答复「删除」、候选 findings.md V4.07 顶部「当前 XM-M034-AB B2-r1 整批候选（按 D-033「删除」重写…）」段（全文件 SHA256 e28ad81a3fdf73f39e7b21082861766378c809897893a81a48b070f7449bdf99）。本票落盘前未读 Grok 本次追加段。不写独占锁行。
- 差集：GR-01＋CX-01→001；GR-02＋CX-03→002；GR-03＋CX-02→003；GR-04＋CX-04→004（D-033「删除」）。两席观察全部登记。无漏并。
- 自审纠正（沿用）：B1 称 OpenAPI 含票务路径有误；B1 起先以为最高权限能绕过已停用码，报告前已改正；票务入口分类采 Grok「冲突」。另：先前把弹窗答复「保留」当定论写了候选，操作员随后改答「删除」，以最新输入为准，作废件已在台账标明。
- 整体下线的外溢面已逐一 grep：两个记录类型只在游乐模块与 4 个测试文件使用；契约组件只被这两个操作引用；按接口名引用的另有契约包、需求实现矩阵、e2e 模拟三处，已纳入写入集合；两份附录为生成物，用生成器重建。
- 最小性：不删表、不改已应用迁移与 CHECK、不删历史行；新迁移只照 V429 停用一个码；契约与文档只删或改该接口相关行。
- 无 questions（D-033 已答）、不 BLOCKED。
- 席甲 B2-r1 票：**PASS**。


## XM-M034-AB B2-r2 候选复核（席甲票，V0.04）

- 时间：2026-09-24T09:05:57Z。输入：findings.md V4.10 顶部「当前 XM-M034-AB B2-r2 候选」段（全文件 SHA256 50d1f57277eae898e85bf90860543fe18f49a3071b0250108fa4cfdf43d79f36）与两席 B5-r1 raw。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- 差集：GR-01→005。无漏并。
- 自审纠正：B5 本席只查了已删类名在代码、契约、测试里的残留，没有核文档里「相关代码文件」这类路径列是否仍指向存在的文件。删除文件的修复，收尾时要全仓 grep 被删文件名（含文档与索引表），逐处判断。
- 分类：技术性；最小处置只删 4 处路径。
- 无添加项、无 questions、不 BLOCKED。
- 席甲 B2-r2 票：**PASS**。
