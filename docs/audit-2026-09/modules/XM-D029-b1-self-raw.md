CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-D029 B1 修复前全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-23T22:20:25Z。
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。
- 合成模块 XM-D029（ledger D-032）：Codex 线 M009/M011 发布提交 1aa4c9c2 相对其第一父提交改动的 27 个代码文件（M009/M011 模块文件、迁移与审计文档除外）；冻结 diff /home/james/audit-tmp/xmd029_scope.diff（128489 bytes，SHA256 bd228b3491a84d9405a85a08c0282844a419ae86762af77f2b5bb9e92bcde7bf）；27 文件发布后未再改。判据＝锁内 M015、M029、M032、M036、M037、M040、M042、M044、M046、M053、M155、M249、M251 映射并集 229 项；操作员 D-032 裁决作业务输入。
- 顺序：Grok 已先派（grok-4.7，modules/XM-D029-b1-grok-raw.md），本报告落盘前未读其正文。只读承诺：未改任何已跟踪文件；三次全量都在 origin/main 一次性 worktree＋一次性空库里跑，跑完 git checkout 还原、DROP 空库；complex_test 只做 SELECT。

## 1. 范围

| 文件 | 所属模块 | 1aa4c9c2 增删 | 发布前 blob | origin/main blob |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | M032 | +14/-23 | 6907271481fd | 4bfae6d2ddde |
| backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java | M032 | +19/-16 | 2408af796642 | 837b04aa3544 |
| backend/src/main/java/com/complex/platform/auth/AuthService.java | M044 | +64/-5 | 94715b5c2e40 | a6d45e92d0ac |
| backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java | M044 | +1/-0 | 9b110c6e7760 | 098910d04dad |
| backend/src/main/java/com/complex/platform/auth/MasterAuthorityService.java | M044 | +4/-5 | aad7d69706ee | a1e3ac6a0fc1 |
| backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java | M044 | +50/-7 | c7edd31e9fa5 | 7ecb5646e761 |
| backend/src/main/java/com/complex/platform/auth/ProductionBearerAuthenticationFilter.java | M044 | +1/-0 | fffc87292d88 | 4c6b052a2d87 |
| backend/src/main/java/com/complex/platform/auth/SessionIssuer.java | M044 | +8/-2 | 304ca698e611 | ca43c31aa02f |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | M015 | +9/-1 | 355c9db8f09e | a9ada70d136c |
| backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java | 未入计划 | +5/-0 | (新文件) | 5223b206c415 |
| backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewEvaluateRequest.java | M040 | +18/-1 | 0154947457db | dc9828a7db30 |
| backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java | M040 | +132/-20 | 0d20ce24a424 | 90d22737dd48 |
| backend/src/main/java/com/complex/platform/leasing/LeasingService.java | M249 | +5/-5 | eb6b15afa9f9 | c1ff9e373035 |
| backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java | M036 | +40/-9 | cefc8dc8e32e | f3e040d0f539 |
| backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | M037 | +12/-14 | 09c86cb6dd7a | 1202d4a39804 |
| backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | 未入计划 | +8/-6 | 6c401544fb64 | 5a3d264cd881 |
| backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | 未入计划 | +2/-2 | 718256789bef | f8999bea3c30 |
| backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java | M046 | +22/-0 | a5964d6383eb | fb126844ea7c |
| backend/src/test/java/com/complex/platform/auth/MasterAuthorityServiceTests.java | M046 | +49/-5 | 9a644f66d3f2 | df685e57a4ac |
| backend/src/test/java/com/complex/platform/auth/ProductionAuthJdbcIntegrationTests.java | M046 | +22/-3 | 056114e5653b | 4b1119cad137 |
| backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java | M155 | +25/-7 | 474c55ced36d | 93d2a14fed6e |
| backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java | M155 | +33/-12 | b56a44592987 | 3b1b2c091979 |
| backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | M042 | +41/-19 | 4aff061f0dc3 | 8c29f53a57d7 |
| backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | M251 | +15/-5 | a39420575eec | 635a868877d5 |
| backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | M251 | +39/-8 | 0e60c08e246b | ac496f138f05 |
| tools/generate-domain-ownership-scope-audit.mjs | M029 | +2/-2 | d2b756d093cd | f530e3ab582d |
| tools/verify-pii-encryption.mjs | M053 | +3/-1 | bb5a6785e2d2 | d5864320e53f |

共 27 个文件。

全读 27 处改动的全部增删行及判断所需上下文：AuthorizationService 范围方法与平台管理员/最高权限判定（L118-175、L415-520）、JdbcAccessRepository 五个带参列表与整组消耗（L71-80、L199-220、L306-312、L415-425、L886-893、L1038-1050）、SupervisorReviewService 全部改动（评估、审批、转发、可见性、策略解析）、认证六文件、LeasingService、ProjectRecord、ProjectAssociationRequest、两个工具与 12 个测试文件的全部增删行。调用方追到 AccessService（受益人与 .money 动作）、PlatformService（M009，项目范围）、网页端 adminClient.ts 刷新请求、AuthController 刷新入口。

## 2. 可执行证据（origin/main 4aa318a5，一次性空库）

| 运行 | 做法 | 后端全量结果 | 日志 |
|---|---|---|---|
| 基线 | origin/main 原样 | 263 类 3524 测 339 红（3ca1b507 时 20 红，新增 319） | xm036a_base.log／.red／.msgs |
| 对照 | 只把 AuthorizationService 退回 1aa4c9c2 之前的 3ca1b507 版 | 105 红 | xm036a_revauth.log |
| 探针 A | 只恢复平台管理员全量视角（AuthorizationService 4 处、Jdbc 5 个列表、SupervisorReviewService 3 处，脚本 xmd029_probeA.py） | 83 红（新增 63） | xmd029_probeA.log、xm036a_probeA.red／.msgs |

探针 A 后新增 63 红的构成：PlatformServiceTests 46、LeasingSignAtomicityJdbcIntegrationTests 3、PlatformJdbcIntegrationTests 1（均为 PlatformService.java:413 `currentProjectScopeQuietly().orElseThrow()` 抛 NoSuchElementException）；AccessServiceTests 6（「退款、免单、授权与资金事项必须登记直接受益人」）；AuthorizationServiceTests 2、SupervisorReviewServiceTests 1（测试把平台管理员受限写成断言）；WecomRiskHitsEndpointReproTests 1（prod 配置上下文起不来）；FieldLabelsTests 1、StaffProfileArchiveDeleteJdbcIntegrationTests 2（M009/M010 已审部分带出，不在本范围，归 XM-M036-A）。3ca1b507 的 20 条旧红在三次运行中不变（归 XM-M036-A）。

测试库事实：complex_test 迁移历史仍记已应用的 427（原 M009 项目档案迁移，已改号为 V432 发布，内容逐字相同、不可重入），连它的 Spring 测试过不了 Flyway 校验，故改用一次性空库（D-032 段）。

## 3. 三线审计

### 3.1 第一性原理

- 这批改动要解决的事：①红线④（数据隔离）——把平台管理员也收进项目/商户隔离；②最高权限与平台管理员分离——最高权限按会话类型 master_authority 识别，不再借平台管理员桥；③审批——受益人登记、资金类升档、角色节点权限点、平行线；④破窗——主会话不可续期、换钥作废旧票；⑤单次临时授权整组消耗。
- 不可再拆的事实：①与操作员 D-032「不约束平台管理员」相反，也与 BSM-007「平台管理员…拥有系统里全部日常功能权限…不会出现管理员反而没权限」、PGM-041／BEQR-152「只有项目之上的获授权岗位才可以跨项目汇总」相反；②③④⑤多数有 HTML 依据（BSM-007「平台管理员与最高权限是两个不同的角色」；L529 直接受益人；L669 职级不能替代权限点；L862-864 钥匙闭环；PGM-041 临时授权单次使用），但落地时调用方与客户端没有同步，导致功能回归。

### 3.2 顺从式

229 项逐项见 §4。

### 3.3 对抗式（grilling）

- Q 平台管理员被收紧到底影响多大？A 探针 A 证实 339→83，约 256 条红直接由此造成；生产若按现 origin/main 部署，平台管理员在各业务模块的商户/项目范围操作大面积 403，权限模板/角色/岗位映射/账号列表 403（原 W-1）。
- Q 恢复平台管理员视角就够了吗？A 不够：M009 的 PlatformService 已按「项目范围永不为空」写成（L411-415 `currentProjectScopeQuietly().orElseThrow()`，15 处调用），恢复后平台管理员进治理页即 500。须连带改 M009 文件（CX-04），M009/M011 届时须重审。
- Q 受益人规则会不会只是测试过期？A 不是：AccessService 三处提交授权复核（L690、L770、L927）都不传受益人，审批服务在评估与审批两处拒绝（SupervisorReviewService.java:143、205、463、791），临时权限申请、角色授予申请、岗位高危部分授出全部走不通（CX-05）。
- Q 刷新强制 device_id 影响谁？A 网页后台刷新请求体为 `{}`（adminClient.ts doRefresh），AuthController 从请求体取 deviceId，AuthService.java:177／ProductionAuthService.java:202 `requireText(request.deviceId(), "device_id")` → 所有网页账号访问令牌到期即刷新失败（CX-07）。主会话禁止续期那一句有 PGM-058 依据，保留。
- Q 资金类一律升最高权限档有依据吗？A PGM-041 原句只把「持股分红的方案配置、算单、审批和发放」定为由最高权限审批，其余高危授权「审批链按只指定最终审批人、系统自动补全中间层级的方式生成」。代码 SupervisorReviewService.java:621 把 .money、debit、deduction、authorization 都送到 permission_grant_l3；AccessService 对资金类授权申请发 access.grant_request.money／access.scope_change.money，于是所有资金类授出都要 U 盘主会话批（CX-06）。
- Q 测试有没有被借机删弱？A 有：M006SecurityRegressionTests 删了席甲 M037-002 的 `verify(repository, never()).savePositionTemplateLifecycle(any(), anyString())`（越权操作不落写入）及两处注释，被测行为未变（CX-10）；AuthorizationServiceTests 删了两条守平台管理员全量视角的测试并换成相反断言（CX-09）。成因：该次发布用主工作区版整份覆盖了 M036/M037 检查点的发布变体。
- Q 去掉生产公钥的测试豁免对吗？A 对生产是对的（M008-A1-02 生产缺公钥须拒启动，豁免开关会被生产误设绕过），但 prod 配置集成测试未同步提供测试公钥（CX-08）。
- Q 其余改动有无问题？A 最高权限单独识别、职级档位封顶两级、角色节点须持权限点、带平行线事项重生成须重新指定、负金额恒进复核、整组消耗、换钥作废旧票、主会话不可续期：均与 HTML 一致，不立项；LeasingService／ProjectRecord／ProjectAssociationRequest／两个工具属 M009 第 6 次发布修复已审依赖，一致。

## 4. 229 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-028 会员唯一识别 | business-enum-quick-reference-v0.1.html:65 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员唯一识别」所述对象｜通过 |
| BEQR-029 会员注册 | business-enum-quick-reference-v0.1.html:66 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员注册」所述对象｜通过 |
| BEQR-030 会员身份状态 | business-enum-quick-reference-v0.1.html:68 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员身份状态」所述对象｜通过 |
| BEQR-031 等级与成长值 | business-enum-quick-reference-v0.1.html:70 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「等级与成长值」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「等级权益对照」所述对象｜通过 |
| BEQR-033 积分与成长值来源 | business-enum-quick-reference-v0.1.html:72 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「积分与成长值来源」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「票务人次计算」所述对象｜通过 |
| BEQR-035 有效消费与冻结 | business-enum-quick-reference-v0.1.html:75 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「有效消费与冻结」所述对象｜通过 |
| BEQR-036 积分明细类型 | business-enum-quick-reference-v0.1.html:77 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「积分明细类型」所述对象｜通过 |
| BEQR-037 积分兑换定价 | business-enum-quick-reference-v0.1.html:78 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「积分兑换定价」所述对象｜通过 |
| BEQR-038 商品规格 | business-enum-quick-reference-v0.1.html:79 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商品规格」所述对象｜通过 |
| BEQR-039 项目零售参考价 | business-enum-quick-reference-v0.1.html:80 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「项目零售参考价」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-041 重复兑换与转赠 | business-enum-quick-reference-v0.1.html:82 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「重复兑换与转赠」所述对象｜通过 |
| BEQR-042 券包状态 | business-enum-quick-reference-v0.1.html:83 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「券包状态」所述对象｜通过 |
| BEQR-043 积分结算账期 | business-enum-quick-reference-v0.1.html:84 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「积分结算账期」所述对象｜通过 |
| BEQR-044 会员系统服务费 | business-enum-quick-reference-v0.1.html:85 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员系统服务费」所述对象｜通过 |
| BEQR-045 储值卡类型与收款主体 | business-enum-quick-reference-v0.1.html:86 | 适用｜储值两份测试改为商户卡由本商户操作员扣款、无商户范围的项目卡扣款仍拒（与平台管理员无关）｜通过 |
| BEQR-046 会员忠诚度扩展 | business-enum-quick-reference-v0.1.html:87 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员忠诚度扩展」所述对象｜通过 |
| BEQR-047 会员注册与开卡 | business-enum-quick-reference-v0.1.html:89 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员注册与开卡」所述对象｜通过 |
| BEQR-048 学生认证 | business-enum-quick-reference-v0.1.html:90 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「学生认证」所述对象｜通过 |
| BEQR-049 会员积分活动状态 | business-enum-quick-reference-v0.1.html:91 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员积分活动状态」所述对象｜通过 |
| BEQR-050 积分活动预算守恒 | business-enum-quick-reference-v0.1.html:92 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「积分活动预算守恒」所述对象｜通过 |
| BEQR-051 充值积分 | business-enum-quick-reference-v0.1.html:93 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「充值积分」所述对象｜通过 |
| BEQR-052 会员营销边界 | business-enum-quick-reference-v0.1.html:94 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员营销边界」所述对象｜通过 |
| BEQR-053 发券对象 | business-enum-quick-reference-v0.1.html:95 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「发券对象」所述对象｜通过 |
| BEQR-054 桌台/卡座/吧台位状态 | business-enum-quick-reference-v0.1.html:100 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台/卡座/吧台位状态」所述对象｜通过 |
| BEQR-055 账单状态 | business-enum-quick-reference-v0.1.html:101 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「账单状态」所述对象｜通过 |
| BEQR-056 订台定金状态 | business-enum-quick-reference-v0.1.html:103 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「订台定金状态」所述对象｜通过 |
| BEQR-057 订卡邀请函状态 | business-enum-quick-reference-v0.1.html:105 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「订卡邀请函状态」所述对象｜通过 |
| BEQR-058 点酒方式 | business-enum-quick-reference-v0.1.html:106 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「点酒方式」所述对象｜通过 |
| BEQR-059 支付方式 | business-enum-quick-reference-v0.1.html:107 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「支付方式」所述对象｜通过 |
| BEQR-060 销售提成 | business-enum-quick-reference-v0.1.html:108 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「销售提成」所述对象｜通过 |
| BEQR-061 服务员提成 | business-enum-quick-reference-v0.1.html:109 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「服务员提成」所述对象｜通过 |
| BEQR-062 退菜退酒 | business-enum-quick-reference-v0.1.html:110 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「退菜退酒」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作须审批：授权/资金类恒进复核符合；资金类授出一律升最高权限档超出原文（CX-06）｜不通过（CX-06） |
| BEQR-064 出品票据状态 | business-enum-quick-reference-v0.1.html:112 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「出品票据状态」所述对象｜通过 |
| BEQR-065 账单操作 | business-enum-quick-reference-v0.1.html:114 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「账单操作」所述对象｜通过 |
| BEQR-066 收银交班状态 | business-enum-quick-reference-v0.1.html:115 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「收银交班状态」所述对象｜通过 |
| BEQR-067 最低消费 | business-enum-quick-reference-v0.1.html:117 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「最低消费」所述对象｜通过 |
| BEQR-068 服务费 | business-enum-quick-reference-v0.1.html:118 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「服务费」所述对象｜通过 |
| BEQR-069 小费 | business-enum-quick-reference-v0.1.html:119 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「小费」所述对象｜通过 |
| BEQR-070 客人存酒有效期与转存 | business-enum-quick-reference-v0.1.html:120 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「客人存酒有效期与转存」所述对象｜通过 |
| BEQR-071 存酒瓶数 | business-enum-quick-reference-v0.1.html:122 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「存酒瓶数」所述对象｜通过 |
| BEQR-072 套餐赠券适用范围 | business-enum-quick-reference-v0.1.html:123 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「套餐赠券适用范围」所述对象｜通过 |
| BEQR-073 非低消商品 | business-enum-quick-reference-v0.1.html:124 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「非低消商品」所述对象｜通过 |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-075 桌台会话与菜单 | business-enum-quick-reference-v0.1.html:126 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台会话与菜单」所述对象｜通过 |
| BEQR-076 桌台计时产品 | business-enum-quick-reference-v0.1.html:127 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台计时产品」所述对象｜通过 |
| BEQR-077 门店支付方式 | business-enum-quick-reference-v0.1.html:128 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「门店支付方式」所述对象｜通过 |
| BEQR-078 扫码点单身份 | business-enum-quick-reference-v0.1.html:129 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「扫码点单身份」所述对象｜通过 |
| BEQR-079 优惠券自动选择 | business-enum-quick-reference-v0.1.html:130 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「优惠券自动选择」所述对象｜通过 |
| BEQR-080 优惠券付款预留 | business-enum-quick-reference-v0.1.html:131 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「优惠券付款预留」所述对象｜通过 |
| BEQR-081 正式挂起订单状态 | business-enum-quick-reference-v0.1.html:132 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「正式挂起订单状态」所述对象｜通过 |
| BEQR-082 正式挂起订单规则 | business-enum-quick-reference-v0.1.html:133 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「正式挂起订单规则」所述对象｜通过 |
| BEQR-083 挂单取消与付款互斥 | business-enum-quick-reference-v0.1.html:134 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「挂单取消与付款互斥」所述对象｜通过 |
| BEQR-084 挂单坏账损耗 | business-enum-quick-reference-v0.1.html:135 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「挂单坏账损耗」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「经营主体关系」所述对象｜通过 |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 适用｜职级影响审批层级：角色节点职级闸与职级派生档位封顶两级，最高权限只给密钥主体｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「终端十一端」所述对象｜通过 |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「最终权限计算」所述对象｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 适用｜原句「只有项目之上的获授权岗位可以跨项目汇总」：平台管理员被锁进单一项目（CX-01…04）｜不通过（CX-01…04） |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「终端开关优先级」所述对象｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「权限状态」所述对象｜通过 |
| BEQR-160 招商线索状态 | business-enum-quick-reference-v0.1.html:248 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「招商线索状态」所述对象｜通过 |
| BEQR-161 招商意向阶段 | business-enum-quick-reference-v0.1.html:249 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「招商意向阶段」所述对象｜通过 |
| BEQR-162 品牌/客户库类型 | business-enum-quick-reference-v0.1.html:250 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「品牌/客户库类型」所述对象｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「合同变更类型」所述对象｜通过 |
| BEQR-164 退租结算状态 | business-enum-quick-reference-v0.1.html:252 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「退租结算状态」所述对象｜通过 |
| BEQR-165 内部应付执行状态 | business-enum-quick-reference-v0.1.html:253 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「内部应付执行状态」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「合同审批链」所述对象｜通过 |
| BEQR-167 租金/账单类型 | business-enum-quick-reference-v0.1.html:256 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「租金/账单类型」所述对象｜通过 |
| BEQR-168 营业额抽成模式 | business-enum-quick-reference-v0.1.html:257 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「营业额抽成模式」所述对象｜通过 |
| BEQR-169 公区能源费 | business-enum-quick-reference-v0.1.html:258 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「公区能源费」所述对象｜通过 |
| BEQR-170 铺位租赁状态 | business-enum-quick-reference-v0.1.html:259 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「铺位租赁状态」所述对象｜通过 |
| BEQR-171 物业工单类型 | business-enum-quick-reference-v0.1.html:260 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「物业工单类型」所述对象｜通过 |
| BEQR-172 物业工单状态 | business-enum-quick-reference-v0.1.html:262 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「物业工单状态」所述对象｜通过 |
| BEQR-173 商户服务工单状态 | business-enum-quick-reference-v0.1.html:264 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户服务工单状态」所述对象｜通过 |
| BEQR-174 工单时效 | business-enum-quick-reference-v0.1.html:265 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「工单时效」所述对象｜通过 |
| BEQR-175 楼宇机电设备状态 | business-enum-quick-reference-v0.1.html:266 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「楼宇机电设备状态」所述对象｜通过 |
| BEQR-176 设备维保类型 | business-enum-quick-reference-v0.1.html:267 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「设备维保类型」所述对象｜通过 |
| BEQR-177 能耗表计类型 | business-enum-quick-reference-v0.1.html:268 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「能耗表计类型」所述对象｜通过 |
| BEQR-178 抄表状态 | business-enum-quick-reference-v0.1.html:269 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「抄表状态」所述对象｜通过 |
| BEQR-179 能耗归集对象 | business-enum-quick-reference-v0.1.html:270 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「能耗归集对象」所述对象｜通过 |
| BEQR-180 楼宇巡检任务状态 | business-enum-quick-reference-v0.1.html:271 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「楼宇巡检任务状态」所述对象｜通过 |
| BEQR-181 楼宇巡检类型 | business-enum-quick-reference-v0.1.html:273 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「楼宇巡检类型」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「管理费方式」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 适用｜配置删除仍须最高权限审批；最高权限改按会话类型识别后删除审批仍可达（探针 A 下相关用例绿）｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 适用｜原句「平台管理员与最高权限是两个不同的角色…拥有系统里全部日常功能权限…不会出现管理员反而没权限」：最高权限单独识别符合；平台管理员被锁进单一项目/商户、列表 403 与之冲突（CX-01…04）｜不通过（CX-01…04） |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「基础管理」所述对象｜通过 |
| BSM-011 客人端 | business-system-map-for-operations-v0.1.html:606 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「客人端」所述对象｜通过 |
| BSM-012 酒吧平板/工作手机 | business-system-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「酒吧平板/工作手机」所述对象｜通过 |
| BSM-014 酒吧管理 | business-system-map-for-operations-v0.1.html:621 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「酒吧管理」所述对象｜通过 |
| BSM-017 商户管理 | business-system-map-for-operations-v0.1.html:637 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户管理」所述对象｜通过 |
| BSM-018 招商租赁 | business-system-map-for-operations-v0.1.html:643 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「招商租赁」所述对象｜通过 |
| BSM-026 各类入口 | business-system-map-for-operations-v0.1.html:686 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「各类入口」所述对象｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 适用｜审批链只指定最终审批人、平行线另指定：评估入口拒自报档位、带平行线事项重生成须重新指定各线最终审批人符合｜通过 |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员与积分规则」所述对象｜通过 |
| BSM-037 客人端：客人自己买票、预定、查询、积分和出示二维码 | business-system-map-for-operations-v0.1.html:850 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「客人端：客人自己买票、预定、查询、积分和出示二维码」所述对象｜通过 |
| BSM-038 主要功能 | business-system-map-for-operations-v0.1.html:854 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| BSM-039 和谁有关 | business-system-map-for-operations-v0.1.html:873 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-040 登录与关键规则 | business-system-map-for-operations-v0.1.html:883 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「登录与关键规则」所述对象｜通过 |
| BSM-041 会员注册、认证与收费边界 | business-system-map-for-operations-v0.1.html:898 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员注册、认证与收费边界」所述对象｜通过 |
| BSM-042 会员积分活动与发券 | business-system-map-for-operations-v0.1.html:907 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员积分活动与发券」所述对象｜通过 |
| BSM-043 会员营销明确边界 | business-system-map-for-operations-v0.1.html:916 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员营销明确边界」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 适用｜最高权限破窗登录、会话与 U 盘：主会话不能续期、带插盘来源设备标识符合｜通过 |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-051 酒吧管理：从入场、开台到结账关台 | business-system-map-for-operations-v0.1.html:1010 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「酒吧管理：从入场、开台到结账关台」所述对象｜通过 |
| BSM-052 主要功能 | business-system-map-for-operations-v0.1.html:1014 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| BSM-053 和谁有关 | business-system-map-for-operations-v0.1.html:1041 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-054 桌台/账单状态与经营规则 | business-system-map-for-operations-v0.1.html:1053 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台/账单状态与经营规则」所述对象｜通过 |
| BSM-055 桌台、菜单与优惠券 | business-system-map-for-operations-v0.1.html:1088 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台、菜单与优惠券」所述对象｜通过 |
| BSM-056 正式未付款订单挂起 | business-system-map-for-operations-v0.1.html:1101 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「正式未付款订单挂起」所述对象｜通过 |
| BSM-057 服务点单：客人点专项服务，公司统一收款、再按名录分账 | business-system-map-for-operations-v0.1.html:1120 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「服务点单：客人点专项服务，公司统一收款、再按名录分账」所述对象｜通过 |
| BSM-058 怎么点、怎么收钱 | business-system-map-for-operations-v0.1.html:1124 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「怎么点、怎么收钱」所述对象｜通过 |
| BSM-059 钱怎么分、怎么退 | business-system-map-for-operations-v0.1.html:1135 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「钱怎么分、怎么退」所述对象｜通过 |
| BSM-060 供应链库存：管供应商、采购、库房、消耗和存酒 | business-system-map-for-operations-v0.1.html:1151 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「供应链库存：管供应商、采购、库房、消耗和存酒」所述对象｜通过 |
| BSM-061 主要功能 | business-system-map-for-operations-v0.1.html:1155 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| BSM-062 和谁有关 | business-system-map-for-operations-v0.1.html:1174 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-063 库存类型与规则 | business-system-map-for-operations-v0.1.html:1186 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「库存类型与规则」所述对象｜通过 |
| BSM-064 采购建议与门店要货 | business-system-map-for-operations-v0.1.html:1208 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「采购建议与门店要货」所述对象｜通过 |
| BSM-065 外部经销商准入、价格与库存 | business-system-map-for-operations-v0.1.html:1216 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「外部经销商准入、价格与库存」所述对象｜通过 |
| BSM-066 经销订单、付款与履约 | business-system-map-for-operations-v0.1.html:1226 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「经销订单、付款与履约」所述对象｜通过 |
| BSM-067 经销退货、退款、红冲与资金归属 | business-system-map-for-operations-v0.1.html:1238 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「经销退货、退款、红冲与资金归属」所述对象｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-075 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | business-system-map-for-operations-v0.1.html:1339 | 适用｜签约自动建商户档案带入意向项目（LeasingService，M009 已审依赖）｜通过 |
| BSM-076 主要功能 | business-system-map-for-operations-v0.1.html:1343 | 适用｜同上｜通过 |
| BSM-077 和谁有关 | business-system-map-for-operations-v0.1.html:1357 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」所述对象｜通过 |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜各入口按范围可见：平台管理员属项目之上岗位，被收紧（CX-01…04）｜不通过（CX-01…04） |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「已确认规则和实施前输入项」所述对象｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜`requiresTopAuthorityGrant` 把 .money/debit/deduction/authorization 一律升最高权限档，无需求承载（CX-06）；其余新增守卫均有 HTML 或定稿依据｜不通过（CX-06） |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main 后端全量 339 红（xm036a_base.log）；授权申请全线 BENEFICIARY_REQUIRED（CX-05）；网页刷新 400（CX-07）；平台管理员进 PlatformService 抛 NoSuchElementException（CX-04）｜不通过（CX-01…05、CX-07、CX-08） |
| GEN-003 测试有效性 | 通用 | 适用｜范围内测试把与 D-032 冲突的行为写成断言（CX-09）；M037-002 的不落写入校验被删（CX-10）；prod 配置集成测试起不来（CX-08）｜不通过（CX-08、CX-09、CX-10） |
| GEN-004 信任边界校验 | 通用 | 适用｜刷新入口 device_id 校验、评估入口拒自报档位、负金额恒进复核均在首次使用前校验｜通过 |
| GEN-005 授权与最小权限 | 通用 | 适用｜平台管理员数据范围被收紧到单一项目/商户（CX-01…04，与 D-032、BSM-007、PGM-041 冲突）；最高权限按会话类型单独识别、职级审批封顶两级符合 BSM-007/PGM-041｜不通过（CX-01…04） |
| GEN-006 数据完整性 | 通用 | 适用｜单次临时授权按 grant_group_id 整组消耗，UPDATE 带 consumed=FALSE/status=active 条件原子执行｜通过 |
| GEN-007 并发与幂等 | 通用 | 适用｜同上，整组消耗条件更新无读后写窗口｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜后端刷新强制 device_id 而网页端请求体为 {}（CX-07）；审批服务新要求受益人而 AccessService 调用点不传（CX-05）；PlatformService 依赖「项目范围永不为空」的新语义（CX-04）｜不通过（CX-04、CX-05、CX-07） |
| GEN-009 错误处理与恢复 | 通用 | 适用｜PlatformService.currentProject 对空 Optional 调 orElseThrow，平台管理员下抛 NoSuchElementException 成 500（CX-04）｜不通过（CX-04） |
| GEN-010 安全与敏感信息 | 通用 | 适用｜最高权限会话不能续期、加载验签公钥即作废已签发主会话、生产缺公钥拒启动；未引入明文秘密｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜auth.master.sessions_revoked、跨范围查看留痕保留｜通过 |
| GEN-012 资源与性能边界 | 通用 | 适用｜启动时作废主会话的 UPDATE 限 user_id＋active｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 适用｜生产配置下验签公钥不再可由库凭证开关豁免，prod 配置集成测试须自带测试公钥（CX-08）｜不通过（CX-08） |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜范围不含迁移（V431/V432 属 M009 已审发布修复）｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 适用｜网页后台访问令牌到期后刷新必失败、用户被踢回登录（CX-07）｜不通过（CX-07） |
| GEN-016 文档可验证性 | 通用 | 适用｜AuthorizationService/SupervisorReviewService 注释「红线④：不因平台管理员身份…」「仅 USB 最高权限可逐笔越权」与 D-032 不符（CX-01、CX-03）｜不通过（CX-01、CX-03） |
| GEN-017 业务来源分类 | 通用 | 适用｜§5 各条按冲突→细化→技术性→添加顺序给分类｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行与可复现命令输出（§2 三次全量）｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜治理底座：本范围的授权、审批、破窗改动均落在该章节下，结论见各细项｜不通过（同 CX-01…07） |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「治理顺序：一件业务能不能做，要先经过这些判断」所述对象｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户和主体」所述对象｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工和手机」所述对象｜通过 |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 适用｜权限和职级：见 PGM-041/042｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「终端开关」所述对象｜通过 |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「设备管理」所述对象｜通过 |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜留痕复核：受益人登记使「直接受益人不得审批」可执行（L529），但调用方未接上（CX-05）｜不通过（CX-05） |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 适用｜应急与隐私：U 盘破窗闭环改动符合｜通过 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「业务边界」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 适用｜单一功能单独授权：转发与角色节点要求目标持有审批权限点（L669「职级不能替代功能权限点」）符合｜通过 |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 适用｜「普通岗位没有跨项目查看权；只有项目之上的获授权岗位才可以跨项目汇总」→ CX-01…04 冲突；「高危授权审批…只指定最终审批人…持股分红…由最高权限审批」→ CX-06 超出；「临时授权可设有效期或单次使用」→ 整组消耗符合｜不通过（CX-01…04、CX-06） |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 适用｜职级规则：角色节点职级闸封顶两级、转发须持权限点符合｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜外包与审计人员口径未被本范围改动触及（isOutsourcedVendor 未改）｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-057 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | platform-governance-map-for-operations-v0.1.html:857 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文」所述对象｜通过 |
| PGM-058 紧急恢复（破窗机制） | platform-governance-map-for-operations-v0.1.html:860 | 适用｜破窗：作废旧钥匙后旧票不能再用（加载公钥即作废已签发主会话）、主会话不能无盘续期符合 L862-864｜通过 |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜个人信息保护：本范围只改名字加密校验脚本的已审部分（M009 发布修复）｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 无本面｜本合成模块范围（1aa4c9c2 在 27 个文件的改动）不触及「已拍板规则和待排期问题」所述对象｜通过 |

共 229 项；逐项给出适用与结论的 39 项，其余 190 项无本面。

## 5. 本次 Findings（临时编号）

| 编号 | 位置与原句 | 正常触发 | 分类 | 判据 | 证据 | 最小处置意见 |
|---|---|---|---|---|---|---|
| CX-01 | `AuthorizationService.java:425/440/504` 三个范围方法去掉平台管理员桥（注释 L423、L502「红线④：不因平台管理员身份…」）；L160 查他人有效权限只放行 `isMasterAuthority` | 平台管理员调用任何经 currentMerchantScope 的商户范围操作即 MERCHANT_SCOPE_REQUIRED；经 currentProjectScopeQuietly 的读路径要求其自身项目 | 冲突（D-032；BSM-007、PGM-041、BEQR-152） | GEN-005、GEN-002、GEN-016 | 类型 4（探针 A：339→83）＋类型 1 | 三处范围方法对平台管理员返回 empty（全量），L160 同时放行平台管理员与最高权限；注释改为现状。最高权限按会话类型识别等其余改动不动 |
| CX-02 | `JdbcAccessRepository.java:75/202/214/309/418` 五个带参列表对空范围改为 PROJECT/MERCHANT_SCOPE_REQUIRED（W-1） | 平台管理员打开权限模板、角色、岗位映射、账号列表即 403 | 冲突（同 CX-01） | GEN-005、GEN-008 | 类型 4＋源码行 | 空范围恢复为全量查询；两个私有 require* 辅助方法随之删除；grant_group_id 整组消耗不动 |
| CX-03 | `SupervisorReviewService.java:390`（同商户审批闸越权）、`:890`（待复核清单跨范围）、`:924`（新增 requireVisibleReview）只认最高权限 | 平台管理员不能审批/查看别的商户、项目发起的复核单 | 冲突（同 CX-01） | GEN-005、GEN-016 | 类型 4（探针 A）＋源码行 | 三处改为平台管理员或最高权限放行；受益人、角色节点、平行线等其余规则不动 |
| CX-04 | `PlatformService.java:411-415` currentProject 对 `currentProjectScopeQuietly()` 调 `orElseThrow()`，requireCurrentProject／creationProject 把平台管理员锁进单一项目（15 处调用；M009 文件） | CX-01 恢复后平台管理员进治理页即 NoSuchElementException（500）；恢复前则只能管自身项目 | 冲突（D-032 连带；BSM-007、PGM-041） | GEN-005、GEN-009、GEN-008 | 类型 4（探针 A：PlatformServiceTests 46、LeasingSign 3、PlatformJdbc 1） | 平台管理员不受当前项目限定（列表不加项目过滤、按对象所属项目校验存在即可、新建须显式给项目）；普通项目岗位维持 M009-038 口径。须写明 M009/M011 因此重审 |
| CX-05 | `SupervisorReviewService.java:143/463`（评估、提交要求受益人）、`:205/791`（审批、转发拒无受益人）；`AccessService.java:690/770/927` 提交授权复核不传受益人 | 员工申请临时权限、角色授予、岗位含高危部分：BENEFICIARY_REQUIRED，授权流程全线走不通 | 细化（L529「直接受益人……临时权限或高危授权的被授权对象本人」；登记受益人是执行「受益人不得审批」的前提，采；调用方未接上是缺陷） | GEN-002、GEN-008、PGM-014 | 类型 4（AccessServiceTests 6 条）＋类型 1 | AccessService 三处把被授权对象作为受益人登记；不放宽审批服务的规则 |
| CX-06 | `SupervisorReviewService.java:621-625` requiresTopAuthorityGrant 把 .money、debit、deduction、dividend、authorization 一律送 permission_grant_l3；AccessService 对资金类授权发 .money 动作 | 资金类权限授出一律须 U 盘主会话审批 | 冲突（PGM-041／BSM-007 原句：只「持股分红」由最高权限审批，其余高危授权按指定最终审批人的审批链；超出部分无 HTML 依据） | GEN-001、BEQR-063 | 类型 1＋源码行 | 只保留分红类（dividend）升最高权限档；其余回到按事项策略的审批链。若判为添加则按 2.2 问操作员 |
| CX-07 | `AuthService.java:177`、`ProductionAuthService.java:202` 刷新对所有用户 `requireText(request.deviceId(), "device_id")`；网页端 adminClient.ts doRefresh 请求体 `{}` | 网页后台访问令牌到期后续期失败、被踢回登录 | 技术性（前后端契约；设备号必填无 HTML 依据，无业务发明） | GEN-008、GEN-002、GEN-015 | 源码行（客户端与入口） | 设备号恢复为「提供了才核对」；主会话禁止续期保留 |
| CX-08 | `MasterAuthorityService.java:95` 生产判定去掉库凭证开关豁免；prod 配置集成测试未提供测试公钥 | WecomRiskHitsEndpointReproTests 上下文加载失败 | 技术性（测试夹具） | GEN-013、GEN-003 | 类型 4 | 该测试属性里补测试用验签公钥；不恢复豁免 |
| CX-09 | `AuthorizationServiceTests` 删 `currentMerchantScopeReturnsEmptyForPlatformAdmin`、`currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId`，换成 `…ReturnsBoundMerchantForPlatformAdmin`；`SupervisorReviewServiceTests.pendingListIsScopedToTheActorsProjectUnlessCurrentApproverOrPlatformAdmin` 断言平台管理员也受限 | 与 CX-01/03 修复相反，修后即红 | 冲突（同 CX-01） | GEN-003 | 类型 4（探针 A 下 3 条红） | 恢复两条旧断言、删去相反断言；复核清单用例改断平台管理员可见跨范围单 |
| CX-10 | `M006SecurityRegressionTests` 删 `verify(repository, never()).savePositionTemplateLifecycle(any(), anyString())` 与两处 M037-002 注释 | 越权操作「不落任何写入」无测试守 | 技术性（测试有效性回退） | GEN-003 | 源码行（发布前后对比） | 恢复该校验与注释 |

写入集合（拟）：AuthorizationService、JdbcAccessRepository、SupervisorReviewService、PlatformService（M009）、AccessService（M031）、AuthService、ProductionAuthService，及相应测试（AuthorizationServiceTests、SupervisorReviewServiceTests、M006SecurityRegressionTests、PlatformServiceTests 等因 CX-04 需同步者、WecomRiskHitsEndpointReproTests、AuthServiceTests／ProductionAuthJdbcIntegrationTests 若需补 CX-07 回归）。以 B2 定稿为准。

## 6. 只登记

- OBS-1：换钥作废旧票在每次带公钥启动时都执行（不只在换钥后），重启即作废在用主会话；主会话本就短时，不立项。
- OBS-2：isAlwaysBlocked、requiresBeneficiary 用子串匹配动作类型（contains grant/penalty/payroll…），新增动作名撞词即被卷入；现有动作表下命中范围与 HTML 一致，只登记。
- OBS-3：待复核清单对无商户号的项目岗位收紧为只见无商户号发起人的单（SupervisorReviewService visibleTo）；是否应见同项目商户发起的单属 PGM-041 解读，登记待相应模块。
- OBS-4：储值两份测试把项目卡扣款改为商户卡由本商户操作员扣、无商户范围项目卡扣款仍拒，与平台管理员无关，符合红线④对商户的约束。
- XM→XM-M036-A：FieldLabelsTests（business_entity_id 缺中文名）、StaffProfileArchiveDeleteJdbcIntegrationTests 2 条（「请填写所属公司」）由 M009/M010 已审部分带出；3ca1b507 的 20 条旧红。

## 7. 结论

席甲 B1 票：**FAIL**（CX-01…10）。27 文件改动全读、229 判据逐项、三线完成；三次全量与探针在一次性 worktree＋空库里做并已还原；落盘前未读 Grok 本次正文。


## XM-D029 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-23T22:33:42Z。输入：两席 B1 完整 raw（席甲 V0.01＝上方本报告、Grok modules/XM-D029-b1-grok-raw.md V0.01）与候选 findings.md V3.94 顶部「当前 XM-D029 B2-r1 整批候选」段（全文件 SHA256 d6ca83f40fce26b824b54730572479a2fec1452768e87063e1a01aa86ea37ba9）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01＝CX-01/02/03（平台管理员部分）/04/09→001；GR-02→002（本席漏）；GR-03＋CX-05→003（本席原判断「授权申请不传受益人」经核源不成立，采 Grok：真正缺陷在准入闸六参路径）；GR-04→004（本席漏）；CX-06→005、CX-07→006、CX-08→007、CX-10→008（Grok B1 对 005/006 判成立、对 007 称钉不到范围，本席在候选段首列了核源请其复核）。无漏并、无改写。
- 自审纠正：B1 时对受益人只看了审批服务一侧就下结论，没有追到 AccessService 的调用参数；同类（「某调用方没传参」）今后先读调用方源码再写。
- 003 本席推荐甲案：准入闸拿不到收款人，是各业务模块守卫调用处的缺口，本合成模块写入集合不含这些文件；甲案恢复免单/退款可建可审（发布前行为），授权类仍强制受益人，缺口登记 XM 给酒吧、财务、票务。若 Grok 定乙案，本席照办。
- 分类：001/002/005 冲突（001 依据操作员 D-032 与 BSM-007、PGM-041、BEQR-152；002/005 依据 PGM-041 原句）；003/004 细化；006/007/008 技术性（无业务发明理由已写在候选行）。无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**。


## XM-D029 B2-r2 复核（只针对 003，席甲票，V0.03）

- 时间：2026-09-23T23:01:28Z。输入：findings.md V3.96 顶部「当前 XM-D029 B2-r2 候选」段（全文件 SHA256 c1599280b9ebcceb32ed4d8f6eac0cbc9d47b01d3b7253b4d95c31c26f572894）与两席 B1/B2-r1 raw。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- 三项新事实均为本席只读实查：bar_bills 列表无客人列；FinanceService.createRefund 仅 RefundRequest 一个参数；customers.id、ar_ap_items.counterparty_id 与 access_users.id 交集为 0，审批/转发检查比较的是员工账号 id。
- 自审：B2-r1 本席票写「若 Grok 定乙案，本席照办」时没有追到调用链与编号体系，这三项应在 B2-r1 就查清。
- 本席选丁案：受益人检查只有在受益人是员工账号时才有拦截效果，授权类满足；免单、退款的受益人是客人或往来方，登记它们不产生任何拦截，却让散客免单建不成、审不了，是比发布前更坏的结果。收款人约束的真正前提是员工与客人／往来方身份关联，属别的模块，登记 XM。
- 分类维持细化，工程缺陷是，队列是；无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r2 票：**PASS**（丁案）。
