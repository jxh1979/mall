CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-D029 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理；本合成模块 B1 席甲 CX-05 判断有误、漏 GR-02/GR-04，均以 Grok 为准）。2026-09-23T23:54:05Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。主工作区 HEAD e456ba96；origin/main 4aa318a5；本范围文件的主工作区现状＝origin/main＋冻结 diff /home/james/audit-tmp/xmd029_b3r1.diff。
- 范围：1aa4c9c2 带出的 27 个代码文件现状＋B3-r1 改到的 PlatformService 与 5 个范围外测试文件；判据：锁内 13 个模块映射并集 229 项；操作员 D-032 为业务输入。B3 累计 1。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/XM-D029-b5-r1-grok-raw.md）。只读承诺：未改任何已跟踪文件；一次性 worktree 与一次性空库已删。

## 1. 文件覆盖（本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 545 | 25959 | 36d90b2e488cc7a7a6a39f4d66bf85b916c0bbb2793dbf96e46a2558a07d4fb4 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java | 1489 | 76754 | 206198c2af31a6f0ed18e548799f4a86a56be824a742d6d00ad19a8cdaf71b9e | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/AuthService.java | 515 | 21103 | a440662824b86484a721ea1100ac3e2111833c028e500fee3fe8cf15ab2482bf | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java | 54 | 2307 | f5ab45e20aef66bf7c676e8de9fa52589d1cbbf92dacee158ad56ff16e84191c | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/MasterAuthorityService.java | 207 | 11128 | 347aa079021b7e830623514a571491e60bb1c3e225564d2dee815634f057d2ef | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java | 742 | 35416 | a6afab157e8c98f6cb982f5680b6f77868608198c541f0567ea088ecf0408270 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/ProductionBearerAuthenticationFilter.java | 53 | 2220 | bfcfd45dac83bba0cf681a374e614a29e36458a8f75ceecb0d3a82c817ed9059 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/auth/SessionIssuer.java | 22 | 1203 | 11f5ee992282182c99e34504ffc9d849c59e580de76a32d05fbd0baaeabecc4d | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java | 5 | 170 | 98c6280743e484c7c954173ac973e21184e2a7fd50489e169fc2f2f6fcf4be62 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1213 | 63769 | 7b8616fea2236fb95dca410fc9cf8d266ce4bd6ba55e9f89dc77fb3a04c162fa | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 25 | 770 | 3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewEvaluateRequest.java | 41 | 1582 | a7c7c24472af2527e4fa767b7845faad4af8caca45d0b436586065a8d3344e7e | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java | 1024 | 64821 | 2ab18fb79b08c40510196b441e2e7156bc6ef5676a96044e2837e2b3e15a136a | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/leasing/LeasingService.java | 453 | 27863 | af8a6d0161cd8c5dd69be5a6a8793a89601c68e5bc1cdf9d137370d6176f49f3 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java | 908 | 47089 | f310b213b17dc728efe1f6c4baa13a8d9ce7563d0f3e6961e70d52ecf75cfd06 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/AccessServiceTests.java | 2172 | 129758 | 3a21f9ad97ffed99359d17f121763a5e5fddcd4ac41e1044f65bff94830e2499 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java | 619 | 27217 | d2cb7625c7a49d3b6f7103b2d109654b3d37b57e5a90f5cb5331d3a0f4e894bf | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java | 836 | 60977 | c9b7e264c9ddb15fed66cca6ee98a6dadc4d68789b3f57be24cfbe47e4f5dad6 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java | 743 | 51909 | 033712594906985a2190286eb78fff626ba308aa0e07edc4cef330575f6c2bc6 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java | 44 | 2770 | b98923cc05f907c3f10739bee8bc37ccde2dde62364694ed7f5af99fee355c7b | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java | 269 | 12742 | 60e3f5a9d17b841374b2419805998efda20679fc2550ef7705b021b58793702d | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/auth/MasterAuthorityServiceTests.java | 223 | 11286 | 25e8517e9be4c620ef64d8b17404b20ae02ff7493351207d1b90a386ec73c282 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/auth/ProductionAuthJdbcIntegrationTests.java | 419 | 22664 | e362d5076bd0ea18d690075c1beb738a61ef37fe36e62a8a50a243754bf2ad74 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java | 304 | 16002 | 8ea8361ba41b55ac83d09d94e43a963cf00e49cec86423fb6efedab88a2fd32e | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java | 100 | 4944 | 5970bc415a7905340bfcf05be6caeabe554cc3bbebd86b7c94f21610e9732ce3 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java | 302 | 14409 | 9cc9e59fdb48bae1b3d4a201b4d4c8a53aecf5f9076ff0923bb51c52fa2debf7 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 1500 | 85507 | 8ee81f4c363c0f71007d03fb3404856371ebca1126802041b746cfea42e2c18c | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java | 1303 | 91466 | c79fbd07082d2797962441c8ae1d9235a030f7155cf54ca5eaf12404cf5b3a66 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java | 377 | 22287 | 76717b0a51386aecae36bff49cc0900f0ca88febcdf972fd1200edebcd958096 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java | 193 | 10616 | 50b76a1cd6598f9e2befeb97d643880137604df4353d9da3941da2feea1854b5 | 改动处及上下文全读 |
| tools/generate-domain-ownership-scope-audit.mjs | 233 | 14340 | cb6bc721c8c39f6c99906be0981b5b1a74bc346ffc5c6d3eb513adc4473cc228 | 改动处及上下文全读 |
| tools/verify-pii-encryption.mjs | 71 | 5834 | d840a4b9f0cca0142ce66524a840216e85b07e8c5be5ae41dafdc566c45c42e2 | 改动处及上下文全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/xmd029_b5_run.log）

```
START=2026-09-23T23:52:12Z HEAD=e456ba96 origin/main=4aa318a5
scratch at 4aa318a5
冻结 diff 已应用
match AuthorizationService.java
match JdbcAccessRepository.java
match AuthService.java
match ProductionAuthService.java
match PlatformService.java
match SupervisorReviewService.java
match AccessJdbcIntegrationTests.java
match AccessServiceTests.java
match AuthorizationServiceTests.java
match M006SecurityRegressionTests.java
match AuthServiceTests.java
match WecomRiskHitsEndpointReproTests.java
match PlatformServiceTests.java
match SupervisorReviewServiceTests.java
scratch db xmd029b5_test created
MVN_EXIT=1
suites=263 tests=3532 red=23
相对 XM-M036-A 预期残留（23 条）：
scratch db dropped
scratch dirty after cleanup: 0
scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T23:53:04Z
```

后端全量 23 红的名单与 XM-M036-A 预期残留（3ca1b507 以来旧 20 条＋FieldLabelsTests 1＋StaffProfileArchiveDeleteJdbcIntegrationTests 2）逐条相同，无意外红、无意外转绿。

## 3. 三线审计

- 第一性原理：这批改动要做到的——平台管理员（项目之上岗位）全平台可见可管（D-032、BSM-007、PGM-041/BEQR-152）；最高权限单独识别且只逐笔开放（第 528 行第④类后半句、BSM-007）；直接受益人不得审批／被转发在能比较的一侧真实生效（第 529 行）；单次临时授权一次用掉整组且逐码留痕（PGM-041）；只有分红由最高权限审批（PGM-041）；破窗闭环（PGM-058）。实际行为：以上均有回归用例，后端全量绿（除 XM-M036-A 既有 23 条）。未验证前提：无——平台管理员在 PlatformService 的每个入口（五类列表、单对象、建空间父空间、新建显式项目、商户档案更新）逐个核过。
- 顺从式：229 项逐项见 §4。
- 对抗式（grilling）：Q 平台管理员恢复全量会不会把商户用户也放开？A 桥只认 account_type=platform_admin 且 active；商户用户仍按 merchant_id 过滤（AuthorizationServiceTests.currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser 绿）。Q 普通项目岗位还受限吗？A PlatformService 非平台管理员路径逐字未改，「普通岗位只见本项目」「须在当前项目建空间」「员工不能越项目读写」三条用例以普通员工身份跑且绿。Q 最高权限还能整表看待审吗？A 不能，按可见规则过滤；按编号读取跨项目单时逐笔留痕（新回归绿）。Q 受益人收窄会不会放掉员工自审？A 授权类仍强制，被授权人自审仍拒（新回归绿）；:208/:800 比较对任何动作都在，bill_waiver 传入员工受益人时转发与自审仍拒（既有用例绿）。Q 资金类授权还要审批吗？A 要，按可配政策档位走审批链，只是不再强制最高权限；分红仍升最高权限档（新回归绿）。Q 网页续期放开会不会让设备会话跳过设备校验？A 绑定了设备的会话缺或错设备号仍拒（新回归绿）。Q 有无新引入问题？A 无；发现一条范围外时红时绿的测试（空间种类删除申请主键含毫秒时间戳），已登记 XM-M036-A。
- 无新缺陷、无添加项、无 HTML 问题。

## 4. 229 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-028 会员唯一识别 | business-enum-quick-reference-v0.1.html:65 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员唯一识别」所述对象｜通过 |
| BEQR-029 会员注册 | business-enum-quick-reference-v0.1.html:66 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员注册」所述对象｜通过 |
| BEQR-030 会员身份状态 | business-enum-quick-reference-v0.1.html:68 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员身份状态」所述对象｜通过 |
| BEQR-031 等级与成长值 | business-enum-quick-reference-v0.1.html:70 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「等级与成长值」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「等级权益对照」所述对象｜通过 |
| BEQR-033 积分与成长值来源 | business-enum-quick-reference-v0.1.html:72 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「积分与成长值来源」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「票务人次计算」所述对象｜通过 |
| BEQR-035 有效消费与冻结 | business-enum-quick-reference-v0.1.html:75 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「有效消费与冻结」所述对象｜通过 |
| BEQR-036 积分明细类型 | business-enum-quick-reference-v0.1.html:77 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「积分明细类型」所述对象｜通过 |
| BEQR-037 积分兑换定价 | business-enum-quick-reference-v0.1.html:78 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「积分兑换定价」所述对象｜通过 |
| BEQR-038 商品规格 | business-enum-quick-reference-v0.1.html:79 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商品规格」所述对象｜通过 |
| BEQR-039 项目零售参考价 | business-enum-quick-reference-v0.1.html:80 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「项目零售参考价」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-041 重复兑换与转赠 | business-enum-quick-reference-v0.1.html:82 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「重复兑换与转赠」所述对象｜通过 |
| BEQR-042 券包状态 | business-enum-quick-reference-v0.1.html:83 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「券包状态」所述对象｜通过 |
| BEQR-043 积分结算账期 | business-enum-quick-reference-v0.1.html:84 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「积分结算账期」所述对象｜通过 |
| BEQR-044 会员系统服务费 | business-enum-quick-reference-v0.1.html:85 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员系统服务费」所述对象｜通过 |
| BEQR-045 储值卡类型与收款主体 | business-enum-quick-reference-v0.1.html:86 | 适用｜储值两份测试夹具未改，仍绿｜通过 |
| BEQR-046 会员忠诚度扩展 | business-enum-quick-reference-v0.1.html:87 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员忠诚度扩展」所述对象｜通过 |
| BEQR-047 会员注册与开卡 | business-enum-quick-reference-v0.1.html:89 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员注册与开卡」所述对象｜通过 |
| BEQR-048 学生认证 | business-enum-quick-reference-v0.1.html:90 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「学生认证」所述对象｜通过 |
| BEQR-049 会员积分活动状态 | business-enum-quick-reference-v0.1.html:91 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员积分活动状态」所述对象｜通过 |
| BEQR-050 积分活动预算守恒 | business-enum-quick-reference-v0.1.html:92 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「积分活动预算守恒」所述对象｜通过 |
| BEQR-051 充值积分 | business-enum-quick-reference-v0.1.html:93 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「充值积分」所述对象｜通过 |
| BEQR-052 会员营销边界 | business-enum-quick-reference-v0.1.html:94 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员营销边界」所述对象｜通过 |
| BEQR-053 发券对象 | business-enum-quick-reference-v0.1.html:95 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「发券对象」所述对象｜通过 |
| BEQR-054 桌台/卡座/吧台位状态 | business-enum-quick-reference-v0.1.html:100 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台/卡座/吧台位状态」所述对象｜通过 |
| BEQR-055 账单状态 | business-enum-quick-reference-v0.1.html:101 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「账单状态」所述对象｜通过 |
| BEQR-056 订台定金状态 | business-enum-quick-reference-v0.1.html:103 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「订台定金状态」所述对象｜通过 |
| BEQR-057 订卡邀请函状态 | business-enum-quick-reference-v0.1.html:105 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「订卡邀请函状态」所述对象｜通过 |
| BEQR-058 点酒方式 | business-enum-quick-reference-v0.1.html:106 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「点酒方式」所述对象｜通过 |
| BEQR-059 支付方式 | business-enum-quick-reference-v0.1.html:107 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「支付方式」所述对象｜通过 |
| BEQR-060 销售提成 | business-enum-quick-reference-v0.1.html:108 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「销售提成」所述对象｜通过 |
| BEQR-061 服务员提成 | business-enum-quick-reference-v0.1.html:109 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「服务员提成」所述对象｜通过 |
| BEQR-062 退菜退酒 | business-enum-quick-reference-v0.1.html:110 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「退菜退酒」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作须审批：授权/资金类恒进复核保留；只分红升最高权限档｜通过 |
| BEQR-064 出品票据状态 | business-enum-quick-reference-v0.1.html:112 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「出品票据状态」所述对象｜通过 |
| BEQR-065 账单操作 | business-enum-quick-reference-v0.1.html:114 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「账单操作」所述对象｜通过 |
| BEQR-066 收银交班状态 | business-enum-quick-reference-v0.1.html:115 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「收银交班状态」所述对象｜通过 |
| BEQR-067 最低消费 | business-enum-quick-reference-v0.1.html:117 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「最低消费」所述对象｜通过 |
| BEQR-068 服务费 | business-enum-quick-reference-v0.1.html:118 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「服务费」所述对象｜通过 |
| BEQR-069 小费 | business-enum-quick-reference-v0.1.html:119 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「小费」所述对象｜通过 |
| BEQR-070 客人存酒有效期与转存 | business-enum-quick-reference-v0.1.html:120 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「客人存酒有效期与转存」所述对象｜通过 |
| BEQR-071 存酒瓶数 | business-enum-quick-reference-v0.1.html:122 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「存酒瓶数」所述对象｜通过 |
| BEQR-072 套餐赠券适用范围 | business-enum-quick-reference-v0.1.html:123 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「套餐赠券适用范围」所述对象｜通过 |
| BEQR-073 非低消商品 | business-enum-quick-reference-v0.1.html:124 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「非低消商品」所述对象｜通过 |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-075 桌台会话与菜单 | business-enum-quick-reference-v0.1.html:126 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台会话与菜单」所述对象｜通过 |
| BEQR-076 桌台计时产品 | business-enum-quick-reference-v0.1.html:127 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台计时产品」所述对象｜通过 |
| BEQR-077 门店支付方式 | business-enum-quick-reference-v0.1.html:128 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「门店支付方式」所述对象｜通过 |
| BEQR-078 扫码点单身份 | business-enum-quick-reference-v0.1.html:129 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「扫码点单身份」所述对象｜通过 |
| BEQR-079 优惠券自动选择 | business-enum-quick-reference-v0.1.html:130 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「优惠券自动选择」所述对象｜通过 |
| BEQR-080 优惠券付款预留 | business-enum-quick-reference-v0.1.html:131 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「优惠券付款预留」所述对象｜通过 |
| BEQR-081 正式挂起订单状态 | business-enum-quick-reference-v0.1.html:132 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「正式挂起订单状态」所述对象｜通过 |
| BEQR-082 正式挂起订单规则 | business-enum-quick-reference-v0.1.html:133 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「正式挂起订单规则」所述对象｜通过 |
| BEQR-083 挂单取消与付款互斥 | business-enum-quick-reference-v0.1.html:134 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「挂单取消与付款互斥」所述对象｜通过 |
| BEQR-084 挂单坏账损耗 | business-enum-quick-reference-v0.1.html:135 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「挂单坏账损耗」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「经营主体关系」所述对象｜通过 |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 适用｜职级影响审批层级：角色节点职级闸与派生档位封顶两级未动｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「终端十一端」所述对象｜通过 |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「最终权限计算」所述对象｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 适用｜「只有项目之上的获授权岗位可以跨项目汇总」：平台管理员跨项目、普通岗位不跨｜通过 |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「终端开关优先级」所述对象｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「权限状态」所述对象｜通过 |
| BEQR-160 招商线索状态 | business-enum-quick-reference-v0.1.html:248 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「招商线索状态」所述对象｜通过 |
| BEQR-161 招商意向阶段 | business-enum-quick-reference-v0.1.html:249 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「招商意向阶段」所述对象｜通过 |
| BEQR-162 品牌/客户库类型 | business-enum-quick-reference-v0.1.html:250 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「品牌/客户库类型」所述对象｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「合同变更类型」所述对象｜通过 |
| BEQR-164 退租结算状态 | business-enum-quick-reference-v0.1.html:252 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「退租结算状态」所述对象｜通过 |
| BEQR-165 内部应付执行状态 | business-enum-quick-reference-v0.1.html:253 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「内部应付执行状态」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「合同审批链」所述对象｜通过 |
| BEQR-167 租金/账单类型 | business-enum-quick-reference-v0.1.html:256 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「租金/账单类型」所述对象｜通过 |
| BEQR-168 营业额抽成模式 | business-enum-quick-reference-v0.1.html:257 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「营业额抽成模式」所述对象｜通过 |
| BEQR-169 公区能源费 | business-enum-quick-reference-v0.1.html:258 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「公区能源费」所述对象｜通过 |
| BEQR-170 铺位租赁状态 | business-enum-quick-reference-v0.1.html:259 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「铺位租赁状态」所述对象｜通过 |
| BEQR-171 物业工单类型 | business-enum-quick-reference-v0.1.html:260 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「物业工单类型」所述对象｜通过 |
| BEQR-172 物业工单状态 | business-enum-quick-reference-v0.1.html:262 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「物业工单状态」所述对象｜通过 |
| BEQR-173 商户服务工单状态 | business-enum-quick-reference-v0.1.html:264 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户服务工单状态」所述对象｜通过 |
| BEQR-174 工单时效 | business-enum-quick-reference-v0.1.html:265 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「工单时效」所述对象｜通过 |
| BEQR-175 楼宇机电设备状态 | business-enum-quick-reference-v0.1.html:266 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「楼宇机电设备状态」所述对象｜通过 |
| BEQR-176 设备维保类型 | business-enum-quick-reference-v0.1.html:267 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「设备维保类型」所述对象｜通过 |
| BEQR-177 能耗表计类型 | business-enum-quick-reference-v0.1.html:268 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「能耗表计类型」所述对象｜通过 |
| BEQR-178 抄表状态 | business-enum-quick-reference-v0.1.html:269 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「抄表状态」所述对象｜通过 |
| BEQR-179 能耗归集对象 | business-enum-quick-reference-v0.1.html:270 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「能耗归集对象」所述对象｜通过 |
| BEQR-180 楼宇巡检任务状态 | business-enum-quick-reference-v0.1.html:271 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「楼宇巡检任务状态」所述对象｜通过 |
| BEQR-181 楼宇巡检类型 | business-enum-quick-reference-v0.1.html:273 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「楼宇巡检类型」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「管理费方式」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 适用｜配置删除仍须最高权限审批（M009 项目归属配置须最高权限的两句断言保留且绿）｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 适用｜平台管理员拥有全部日常功能、不会出现管理员反而没权限：五类列表跨项目、治理页不再 500；平台管理员与最高权限两个角色分开识别｜通过 |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「基础管理」所述对象｜通过 |
| BSM-011 客人端 | business-system-map-for-operations-v0.1.html:606 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「客人端」所述对象｜通过 |
| BSM-012 酒吧平板/工作手机 | business-system-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「酒吧平板/工作手机」所述对象｜通过 |
| BSM-014 酒吧管理 | business-system-map-for-operations-v0.1.html:621 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「酒吧管理」所述对象｜通过 |
| BSM-017 商户管理 | business-system-map-for-operations-v0.1.html:637 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户管理」所述对象｜通过 |
| BSM-018 招商租赁 | business-system-map-for-operations-v0.1.html:643 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「招商租赁」所述对象｜通过 |
| BSM-026 各类入口 | business-system-map-for-operations-v0.1.html:686 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「各类入口」所述对象｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 适用｜审批链按指定最终审批人、平行线另指定：资金类不再被强制改到最高权限档，分红仍由最高权限｜通过 |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员与积分规则」所述对象｜通过 |
| BSM-037 客人端：客人自己买票、预定、查询、积分和出示二维码 | business-system-map-for-operations-v0.1.html:850 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「客人端：客人自己买票、预定、查询、积分和出示二维码」所述对象｜通过 |
| BSM-038 主要功能 | business-system-map-for-operations-v0.1.html:854 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| BSM-039 和谁有关 | business-system-map-for-operations-v0.1.html:873 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-040 登录与关键规则 | business-system-map-for-operations-v0.1.html:883 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「登录与关键规则」所述对象｜通过 |
| BSM-041 会员注册、认证与收费边界 | business-system-map-for-operations-v0.1.html:898 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员注册、认证与收费边界」所述对象｜通过 |
| BSM-042 会员积分活动与发券 | business-system-map-for-operations-v0.1.html:907 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员积分活动与发券」所述对象｜通过 |
| BSM-043 会员营销明确边界 | business-system-map-for-operations-v0.1.html:916 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员营销明确边界」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 适用｜最高权限破窗登录、会话与 U 盘：主会话不可续期保留｜通过 |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-051 酒吧管理：从入场、开台到结账关台 | business-system-map-for-operations-v0.1.html:1010 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「酒吧管理：从入场、开台到结账关台」所述对象｜通过 |
| BSM-052 主要功能 | business-system-map-for-operations-v0.1.html:1014 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| BSM-053 和谁有关 | business-system-map-for-operations-v0.1.html:1041 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-054 桌台/账单状态与经营规则 | business-system-map-for-operations-v0.1.html:1053 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台/账单状态与经营规则」所述对象｜通过 |
| BSM-055 桌台、菜单与优惠券 | business-system-map-for-operations-v0.1.html:1088 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台、菜单与优惠券」所述对象｜通过 |
| BSM-056 正式未付款订单挂起 | business-system-map-for-operations-v0.1.html:1101 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「正式未付款订单挂起」所述对象｜通过 |
| BSM-057 服务点单：客人点专项服务，公司统一收款、再按名录分账 | business-system-map-for-operations-v0.1.html:1120 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「服务点单：客人点专项服务，公司统一收款、再按名录分账」所述对象｜通过 |
| BSM-058 怎么点、怎么收钱 | business-system-map-for-operations-v0.1.html:1124 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「怎么点、怎么收钱」所述对象｜通过 |
| BSM-059 钱怎么分、怎么退 | business-system-map-for-operations-v0.1.html:1135 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「钱怎么分、怎么退」所述对象｜通过 |
| BSM-060 供应链库存：管供应商、采购、库房、消耗和存酒 | business-system-map-for-operations-v0.1.html:1151 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「供应链库存：管供应商、采购、库房、消耗和存酒」所述对象｜通过 |
| BSM-061 主要功能 | business-system-map-for-operations-v0.1.html:1155 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| BSM-062 和谁有关 | business-system-map-for-operations-v0.1.html:1174 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-063 库存类型与规则 | business-system-map-for-operations-v0.1.html:1186 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「库存类型与规则」所述对象｜通过 |
| BSM-064 采购建议与门店要货 | business-system-map-for-operations-v0.1.html:1208 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「采购建议与门店要货」所述对象｜通过 |
| BSM-065 外部经销商准入、价格与库存 | business-system-map-for-operations-v0.1.html:1216 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「外部经销商准入、价格与库存」所述对象｜通过 |
| BSM-066 经销订单、付款与履约 | business-system-map-for-operations-v0.1.html:1226 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「经销订单、付款与履约」所述对象｜通过 |
| BSM-067 经销退货、退款、红冲与资金归属 | business-system-map-for-operations-v0.1.html:1238 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「经销退货、退款、红冲与资金归属」所述对象｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-075 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | business-system-map-for-operations-v0.1.html:1339 | 适用｜签约自动建商户档案带入意向项目（显式项目，平台管理员路径同样成立）｜通过 |
| BSM-076 主要功能 | business-system-map-for-operations-v0.1.html:1343 | 适用｜同上｜通过 |
| BSM-077 和谁有关 | business-system-map-for-operations-v0.1.html:1357 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」所述对象｜通过 |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜各入口按范围可见：平台管理员全平台、普通岗位本项目、商户本商户｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「已确认规则和实施前输入项」所述对象｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜资金类升档只留分红；受益人强制只留授权类；无无需求承载的新增｜通过 |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main＋冻结 diff 后端全量 3532 测 23 红，全为 XM-M036-A 既有红（§2）；平台管理员治理页、授权申请、网页续期路径均有回归用例且绿｜通过 |
| GEN-003 测试有效性 | 通用 | 适用｜RED 阶段 11 条新增或改动测试相对原样基线新红、GREEN 全绿；M037-002 不落写入校验已恢复；prod 配置测试补测试公钥后可起｜通过 |
| GEN-004 信任边界校验 | 通用 | 适用｜刷新设备号：绑定设备会话缺或错即拒、提供了须一致；评估入口拒自报档位；负金额恒进复核｜通过 |
| GEN-005 授权与最小权限 | 通用 | 适用｜平台管理员全量视角（D-032，BSM-007、PGM-041）；普通项目岗位与商户仍隔离（ordinary 用例改以普通员工身份跑且绿，商户用户仍按商户号过滤）；最高权限只逐笔开放｜通过 |
| GEN-006 数据完整性 | 通用 | 适用｜整组消耗 UPDATE…RETURNING 原子，逐码留痕在同一事务｜通过 |
| GEN-007 并发与幂等 | 通用 | 适用｜整组消耗条件更新 consumed=FALSE 防并发重复｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜网页刷新与后端设备号契约一致；审批受益人契约只对授权类且 AccessService 恒传；PlatformService 对范围为空走不带项目参数的重载｜通过 |
| GEN-009 错误处理与恢复 | 通用 | 适用｜平台管理员进 PlatformService 不再空 Optional 抛错；商户档案项目比较空安全｜通过 |
| GEN-010 安全与敏感信息 | 通用 | 适用｜主会话不可续期、换钥作废旧票、生产缺公钥拒启动均保留｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜最高权限跨范围按编号读取逐笔留痕（viewed_cross_scope）；平台管理员整表留痕保留；整组消耗逐码留痕｜通过 |
| GEN-012 资源与性能边界 | 通用 | 适用｜启动作废主会话 UPDATE 限 user_id；待审列表过滤在已取的 limit 行内｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 适用｜生产必须配公钥的门禁保留；prod 配置测试按既有方式注入测试公钥｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜范围不含迁移｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 适用｜网页后台访问令牌到期可续期｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜范围方法与审批注释改为现状（D-032／逐笔开放）；受益人报错文案改为只说授权事项｜通过 |
| GEN-017 业务来源分类 | 通用 | 适用｜001/002/005 冲突、003/004 细化、006/007/008 技术性，B2 双席定稿｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行与后端全量命令输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜治理底座：本范围授权、审批、破窗改动均符合其下细则｜通过 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「治理顺序：一件业务能不能做，要先经过这些判断」所述对象｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户和主体」所述对象｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工和手机」所述对象｜通过 |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 适用｜权限和职级：见 PGM-041/042｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「终端开关」所述对象｜通过 |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「设备管理」所述对象｜通过 |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜留痕复核：授权类受益人登记与「受益人不得审批」检查真实生效；客人/往来方收款人另登记 XM｜通过 |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 适用｜应急与隐私：U 盘破窗闭环保留｜通过 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「业务边界」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 适用｜单一功能单独授权：转发与角色节点须持审批权限点保留｜通过 |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 适用｜项目之上岗位跨项目汇总（平台管理员）、高危授权按指定最终审批人链（只分红由最高权限）、临时授权单次消耗全部留痕｜通过 |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 适用｜职级规则：职级不能替代权限点、封顶两级保留｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜外包与审计人员口径未被改动｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-057 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | platform-governance-map-for-operations-v0.1.html:857 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文」所述对象｜通过 |
| PGM-058 紧急恢复（破窗机制） | platform-governance-map-for-operations-v0.1.html:860 | 适用｜破窗：作废旧钥匙后旧票不可用、主会话不可续期保留｜通过 |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜个人信息保护：本范围仅含 M009 发布修复已审的姓名加密校验脚本｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 无本面｜本合成模块范围（27 个文件现状＋B3 改动文件）不触及「已拍板规则和待排期问题」所述对象｜通过 |

共 229 项；逐项给出适用与结论的 39 项，其余 190 项无本面。

## 5. 已定稿 001…008 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」＝0。

## 6. 本次 Findings

无新增。登记不变：OBS-1…4、M010 演员；XM→XM-M036-A（FieldLabels、StaffProfile、旧 20 红、空间种类删除申请主键撞键）；XM→酒吧 M193／财务 M332／票务 M133 与身份模块（收款人不得审批/被转发对客人与往来方无从执行，须先有员工与客人／往来方身份关联）。

## 7. 结论

席甲 B5-r1 票：**PASS**。范围全覆盖、229 项逐项、后端全量实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
