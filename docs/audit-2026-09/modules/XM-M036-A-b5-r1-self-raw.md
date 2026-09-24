CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M036-A B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票；B1 对 CustomerService:81 与治理地图行号两处引错，已在 B2 更正）。2026-09-24T04:22:39Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。主工作区开工段 21 个文件现状＝origin/main 4f233e7b＋冻结 diff /home/james/audit-tmp/xm036a_b3r1.diff。
- 范围：开工段 21 个文件现状（8 个原红测试文件、13 个失败路径生产文件，其中 12 个本轮改过）；判据 16 个模块映射并集 297 项。B3 累计 1。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/XM-M036-A-b5-r1-grok-raw.md）。只读承诺：未改任何已跟踪文件；一次性 worktree 与一次性空库已删。

## 1. 文件覆盖（本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java | 428 | 19898 | e88b1f6f42a6018be7d752daea3b5279846922029c8a1727841585ef10628388 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java | 250 | 10892 | 288dfd458a36746d909269a2a274c79b384ac4e532afaca14d292c3e4fe5bc87 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java | 132 | 6776 | 132143a0662919174cc61ee9b14d61686d9bcb46fee861f4b4ef63075700dc6d | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java | 3424 | 179833 | 0c428b2888bdffc260ac217302d397f690fa2c3b83ec2d3b9b4f062701a2cbb3 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java | 133 | 7601 | acafb231993b6d234b47de9f3aa2ab5dbe91e204b1b9fae2fd8c6eb16498fa5d | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/common/api/FieldLabelsTests.java | 94 | 4756 | ff618431c9eebeb31ed27df4ba9b0d13e17a7e9dbb983a496fa9728a67883c5b | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java | 121 | 5622 | 5d3c11aaa765c3dfcbaddc2945399a15e266659db23f6a44181cfc18e289afc2 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 773 | 42199 | e20b852f888ea72ab6ae0411854b104445abf415ccf430ef67d83fabd0f9d648 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java | 161 | 10053 | 493ccabf5da1ed5b715c1af52bc6349c6d6619d62b2d1a80b795eae2d8123c49 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/common/api/FieldLabels.java | 554 | 25045 | 15c28d059e306a76f69ef125da253f814d92e9429fe4fe54caa02a34be114951 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/customer/StoredValueService.java | 874 | 56461 | 82a11969f64cc38330d3def05516d62f29a5ff25a971015ffe2da1297ecc355a | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1338 | 76821 | a29ab13853f9d4a3586189baee10a55582daeedc09c3c2ad8dd2581406af4964 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1213 | 63769 | 7b8616fea2236fb95dca410fc9cf8d266ce4bd6ba55e9f89dc77fb3a04c162fa | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java | 282 | 15493 | 0c767493c0bbe7723bd5c86d40671decc9bf4a38870e3581ca1875b66bf874f1 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/amusement/AmusementService.java | 1947 | 101583 | 4e22fa5b48221122e168b77a6686b8bcb7f028ab39fb634b7c2cfc8ee6a3a4ae | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/compliance/ComplianceController.java | 164 | 9131 | dbc0cb0dd5e747f7ee3f8b878e6f1c7c9d607ec0b4054481ca37b9659bcf0250 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/compliance/ComplianceService.java | 510 | 22179 | c6fd3d62a98f1c1e343e22795dc60fcf72cd8087d8529543d9f7f9aa8bc1e837 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/finance/FinanceController.java | 230 | 13066 | d724ee79b2112f9977a4c466250a2312a48c55fff903f608e5925424943a55eb | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffService.java | 1666 | 106198 | 7ad932c75a256af62c78f197e769630368dd3097187967ff18cd1102bc419720 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffController.java | 386 | 20421 | 98e562d9cbf0ca038ead0dc61d30615741490fe57cc0da89c544c928eb3df7ad | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/customer/CustomerService.java | 1309 | 68703 | 3c7970a59f51346d957a0ea1b13da5df2c99946c9b22a6b037987af18bf2bdf3 | 改动处及上下文全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/xm036a_b5_run.log）

```
START=2026-09-24T04:20:42Z HEAD=e456ba96 origin/main=4f233e7b
scratch at 4f233e7b
冻结 diff 已应用
match StaffService.java
match AmusementService.java
match FieldLabels.java
match FacilityEquipmentService.java
match JdbcPlatformRepository.java
match StaffProfileArchiveDeleteJdbcIntegrationTests.java
match AmusementServiceTests.java
match ComplianceControllerTests.java
match FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java
match FinanceControllerTests.java
match CustomerGuardedActionAndRejectionAuditTests.java
match PlatformJdbcIntegrationTests.java
scratch db xm036ab5_test created
MVN_EXIT=0
suites=263 tests=3536 red=0
全部红（预期 0）：
相对基线 xmd029-ckpt.red（23）转绿：23
scratch db dropped
scratch dirty after cleanup: 0
scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T04:21:33Z
```

## 3. 三线审计

- 第一性原理：这批测试与代码要做到——财务与合规入口断言真正执行；储值扣款挂终端开关；字段报错中文；设施设备、员工档案、游乐权益绑定「存档→申请删除→最高权限本人审批（最高权限本人发起即直接生效）→软删→可恢复」且删前查占用；空间种类删除申请留历史。实际：后端全量 0 红，原 23 红全部由正确处置转绿；三类删除的审批人只认最高权限本人，平台管理员被拒有断言；最高权限发起即删除三类都有回归（员工档案用生产形状夹具）；主键连续 50 次申请不撞。未验证前提：最高权限在其余模块单据路径上的可用性（XM-M036A-1，只登记）。
- 顺从式：297 项逐项见 §4。
- 对抗式（grilling）：问：平台管理员还能批设施或游乐删除吗？答：不能，服务层首句 MASTER_APPROVAL_REQUIRED，用例断言申请仍待审批。问：被显式授了审批码的普通账号呢？答：同样被最高权限闸拦下，闸在权限校验之前。问：最高权限直接删除会不会绕过占用检查？答：设施走 approveDelete 原通过分支，占用抛冲突并整笔回滚；员工档案走原 approveDeleteProfile；游乐仓储原本就无占用检查，已登记观察给 M230，本轮未改其语义。问：普通取消存档是否被收成最高权限？答：否，只在墓碑分支；非最高权限的普通取消存档仍过 checkProjectOwnership。问：员工档案其余读写是否对最高权限放开？答：否，只两条删除路径按存在取档案，requireProfileInScope 与 requireRegistrationScope 本体未改。问：财务 MockitoBean 会不会把别的 Bean 一并替掉？答：只替 StoredValueService，其余 TestBeans 原样，12 条执行到金额与权限断言。问：合规测试是不是绕开了终端闸？答：没有，guard Bean 与拦截器照旧，请求带的是生产里由设备凭证拦截器写入的同一属性。问：主键回归在旧代码下真能红？答：RED 轮实红。问：有无新引入问题？答：未见。
- 无新缺陷、无添加项、无 HTML 问题。

## 4. 297 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-003 定价、售票与收款主体 | business-enum-quick-reference-v0.1.html:32 | 无本面｜本合成模块范围（21 个文件现状）不触及「定价、售票与收款主体」所述对象｜通过 |
| BEQR-005 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 无本面｜本合成模块范围（21 个文件现状）不触及「通票赠酒下限」所述对象｜通过 |
| BEQR-014 退款单状态 | business-enum-quick-reference-v0.1.html:45 | 无本面｜本合成模块范围（21 个文件现状）不触及「退款单状态」所述对象｜通过 |
| BEQR-016 售票与退款原则 | business-enum-quick-reference-v0.1.html:47 | 无本面｜本合成模块范围（21 个文件现状）不触及「售票与退款原则」所述对象｜通过 |
| BEQR-020 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 无本面｜本合成模块范围（21 个文件现状）不触及「酒吧通票可分配池」所述对象｜通过 |
| BEQR-024 游乐核销归属 | business-enum-quick-reference-v0.1.html:56 | 无本面｜本合成模块范围（21 个文件现状）不触及「游乐核销归属」所述对象｜通过 |
| BEQR-026 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 无本面｜本合成模块范围（21 个文件现状）不触及「动态价与会员价」所述对象｜通过 |
| BEQR-028 会员唯一识别 | business-enum-quick-reference-v0.1.html:65 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员唯一识别」所述对象｜通过 |
| BEQR-029 会员注册 | business-enum-quick-reference-v0.1.html:66 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员注册」所述对象｜通过 |
| BEQR-030 会员身份状态 | business-enum-quick-reference-v0.1.html:68 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员身份状态」所述对象｜通过 |
| BEQR-031 等级与成长值 | business-enum-quick-reference-v0.1.html:70 | 无本面｜本合成模块范围（21 个文件现状）不触及「等级与成长值」所述对象｜通过 |
| BEQR-032 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 无本面｜本合成模块范围（21 个文件现状）不触及「等级权益对照」所述对象｜通过 |
| BEQR-033 积分与成长值来源 | business-enum-quick-reference-v0.1.html:72 | 无本面｜本合成模块范围（21 个文件现状）不触及「积分与成长值来源」所述对象｜通过 |
| BEQR-034 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 无本面｜本合成模块范围（21 个文件现状）不触及「票务人次计算」所述对象｜通过 |
| BEQR-035 有效消费与冻结 | business-enum-quick-reference-v0.1.html:75 | 无本面｜本合成模块范围（21 个文件现状）不触及「有效消费与冻结」所述对象｜通过 |
| BEQR-036 积分明细类型 | business-enum-quick-reference-v0.1.html:77 | 无本面｜本合成模块范围（21 个文件现状）不触及「积分明细类型」所述对象｜通过 |
| BEQR-037 积分兑换定价 | business-enum-quick-reference-v0.1.html:78 | 无本面｜本合成模块范围（21 个文件现状）不触及「积分兑换定价」所述对象｜通过 |
| BEQR-038 商品规格 | business-enum-quick-reference-v0.1.html:79 | 无本面｜本合成模块范围（21 个文件现状）不触及「商品规格」所述对象｜通过 |
| BEQR-039 项目零售参考价 | business-enum-quick-reference-v0.1.html:80 | 无本面｜本合成模块范围（21 个文件现状）不触及「项目零售参考价」所述对象｜通过 |
| BEQR-040 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 无本面｜本合成模块范围（21 个文件现状）不触及「发券与核销结算」所述对象｜通过 |
| BEQR-041 重复兑换与转赠 | business-enum-quick-reference-v0.1.html:82 | 无本面｜本合成模块范围（21 个文件现状）不触及「重复兑换与转赠」所述对象｜通过 |
| BEQR-042 券包状态 | business-enum-quick-reference-v0.1.html:83 | 无本面｜本合成模块范围（21 个文件现状）不触及「券包状态」所述对象｜通过 |
| BEQR-043 积分结算账期 | business-enum-quick-reference-v0.1.html:84 | 无本面｜本合成模块范围（21 个文件现状）不触及「积分结算账期」所述对象｜通过 |
| BEQR-044 会员系统服务费 | business-enum-quick-reference-v0.1.html:85 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员系统服务费」所述对象｜通过 |
| BEQR-045 储值卡类型与收款主体 | business-enum-quick-reference-v0.1.html:86 | 适用｜储值见 BSM-035｜通过 |
| BEQR-046 会员忠诚度扩展 | business-enum-quick-reference-v0.1.html:87 | 适用｜储值见 BSM-035｜通过 |
| BEQR-047 会员注册与开卡 | business-enum-quick-reference-v0.1.html:89 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员注册与开卡」所述对象｜通过 |
| BEQR-048 学生认证 | business-enum-quick-reference-v0.1.html:90 | 无本面｜本合成模块范围（21 个文件现状）不触及「学生认证」所述对象｜通过 |
| BEQR-049 会员积分活动状态 | business-enum-quick-reference-v0.1.html:91 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员积分活动状态」所述对象｜通过 |
| BEQR-050 积分活动预算守恒 | business-enum-quick-reference-v0.1.html:92 | 无本面｜本合成模块范围（21 个文件现状）不触及「积分活动预算守恒」所述对象｜通过 |
| BEQR-051 充值积分 | business-enum-quick-reference-v0.1.html:93 | 无本面｜本合成模块范围（21 个文件现状）不触及「充值积分」所述对象｜通过 |
| BEQR-052 会员营销边界 | business-enum-quick-reference-v0.1.html:94 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员营销边界」所述对象｜通过 |
| BEQR-053 发券对象 | business-enum-quick-reference-v0.1.html:95 | 无本面｜本合成模块范围（21 个文件现状）不触及「发券对象」所述对象｜通过 |
| BEQR-054 桌台/卡座/吧台位状态 | business-enum-quick-reference-v0.1.html:100 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台/卡座/吧台位状态」所述对象｜通过 |
| BEQR-055 账单状态 | business-enum-quick-reference-v0.1.html:101 | 无本面｜本合成模块范围（21 个文件现状）不触及「账单状态」所述对象｜通过 |
| BEQR-056 订台定金状态 | business-enum-quick-reference-v0.1.html:103 | 无本面｜本合成模块范围（21 个文件现状）不触及「订台定金状态」所述对象｜通过 |
| BEQR-057 订卡邀请函状态 | business-enum-quick-reference-v0.1.html:105 | 无本面｜本合成模块范围（21 个文件现状）不触及「订卡邀请函状态」所述对象｜通过 |
| BEQR-058 点酒方式 | business-enum-quick-reference-v0.1.html:106 | 无本面｜本合成模块范围（21 个文件现状）不触及「点酒方式」所述对象｜通过 |
| BEQR-059 支付方式 | business-enum-quick-reference-v0.1.html:107 | 无本面｜本合成模块范围（21 个文件现状）不触及「支付方式」所述对象｜通过 |
| BEQR-060 销售提成 | business-enum-quick-reference-v0.1.html:108 | 无本面｜本合成模块范围（21 个文件现状）不触及「销售提成」所述对象｜通过 |
| BEQR-061 服务员提成 | business-enum-quick-reference-v0.1.html:109 | 无本面｜本合成模块范围（21 个文件现状）不触及「服务员提成」所述对象｜通过 |
| BEQR-062 退菜退酒 | business-enum-quick-reference-v0.1.html:110 | 无本面｜本合成模块范围（21 个文件现状）不触及「退菜退酒」所述对象｜通过 |
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作审批：删除审批只认最高权限｜通过 |
| BEQR-064 出品票据状态 | business-enum-quick-reference-v0.1.html:112 | 无本面｜本合成模块范围（21 个文件现状）不触及「出品票据状态」所述对象｜通过 |
| BEQR-065 账单操作 | business-enum-quick-reference-v0.1.html:114 | 无本面｜本合成模块范围（21 个文件现状）不触及「账单操作」所述对象｜通过 |
| BEQR-066 收银交班状态 | business-enum-quick-reference-v0.1.html:115 | 无本面｜本合成模块范围（21 个文件现状）不触及「收银交班状态」所述对象｜通过 |
| BEQR-067 最低消费 | business-enum-quick-reference-v0.1.html:117 | 无本面｜本合成模块范围（21 个文件现状）不触及「最低消费」所述对象｜通过 |
| BEQR-068 服务费 | business-enum-quick-reference-v0.1.html:118 | 无本面｜本合成模块范围（21 个文件现状）不触及「服务费」所述对象｜通过 |
| BEQR-069 小费 | business-enum-quick-reference-v0.1.html:119 | 无本面｜本合成模块范围（21 个文件现状）不触及「小费」所述对象｜通过 |
| BEQR-070 客人存酒有效期与转存 | business-enum-quick-reference-v0.1.html:120 | 无本面｜本合成模块范围（21 个文件现状）不触及「客人存酒有效期与转存」所述对象｜通过 |
| BEQR-071 存酒瓶数 | business-enum-quick-reference-v0.1.html:122 | 无本面｜本合成模块范围（21 个文件现状）不触及「存酒瓶数」所述对象｜通过 |
| BEQR-072 套餐赠券适用范围 | business-enum-quick-reference-v0.1.html:123 | 无本面｜本合成模块范围（21 个文件现状）不触及「套餐赠券适用范围」所述对象｜通过 |
| BEQR-073 非低消商品 | business-enum-quick-reference-v0.1.html:124 | 无本面｜本合成模块范围（21 个文件现状）不触及「非低消商品」所述对象｜通过 |
| BEQR-074 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 无本面｜本合成模块范围（21 个文件现状）不触及「隐私桌台权限」所述对象｜通过 |
| BEQR-075 桌台会话与菜单 | business-enum-quick-reference-v0.1.html:126 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台会话与菜单」所述对象｜通过 |
| BEQR-076 桌台计时产品 | business-enum-quick-reference-v0.1.html:127 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台计时产品」所述对象｜通过 |
| BEQR-077 门店支付方式 | business-enum-quick-reference-v0.1.html:128 | 无本面｜本合成模块范围（21 个文件现状）不触及「门店支付方式」所述对象｜通过 |
| BEQR-078 扫码点单身份 | business-enum-quick-reference-v0.1.html:129 | 无本面｜本合成模块范围（21 个文件现状）不触及「扫码点单身份」所述对象｜通过 |
| BEQR-079 优惠券自动选择 | business-enum-quick-reference-v0.1.html:130 | 无本面｜本合成模块范围（21 个文件现状）不触及「优惠券自动选择」所述对象｜通过 |
| BEQR-080 优惠券付款预留 | business-enum-quick-reference-v0.1.html:131 | 无本面｜本合成模块范围（21 个文件现状）不触及「优惠券付款预留」所述对象｜通过 |
| BEQR-081 正式挂起订单状态 | business-enum-quick-reference-v0.1.html:132 | 无本面｜本合成模块范围（21 个文件现状）不触及「正式挂起订单状态」所述对象｜通过 |
| BEQR-082 正式挂起订单规则 | business-enum-quick-reference-v0.1.html:133 | 无本面｜本合成模块范围（21 个文件现状）不触及「正式挂起订单规则」所述对象｜通过 |
| BEQR-083 挂单取消与付款互斥 | business-enum-quick-reference-v0.1.html:134 | 无本面｜本合成模块范围（21 个文件现状）不触及「挂单取消与付款互斥」所述对象｜通过 |
| BEQR-084 挂单坏账损耗 | business-enum-quick-reference-v0.1.html:135 | 无本面｜本合成模块范围（21 个文件现状）不触及「挂单坏账损耗」所述对象｜通过 |
| BEQR-085 项目开放状态 | business-enum-quick-reference-v0.1.html:140 | 无本面｜本合成模块范围（21 个文件现状）不触及「项目开放状态」所述对象｜通过 |
| BEQR-086 场次状态 | business-enum-quick-reference-v0.1.html:142 | 无本面｜本合成模块范围（21 个文件现状）不触及「场次状态」所述对象｜通过 |
| BEQR-087 游乐「当天／每天」口径 | business-enum-quick-reference-v0.1.html:144 | 无本面｜本合成模块范围（21 个文件现状）不触及「游乐「当天／每天」口径」所述对象｜通过 |
| BEQR-088 容量双阈值 | business-enum-quick-reference-v0.1.html:145 | 无本面｜本合成模块范围（21 个文件现状）不触及「容量双阈值」所述对象｜通过 |
| BEQR-089 园区容量 | business-enum-quick-reference-v0.1.html:146 | 无本面｜本合成模块范围（21 个文件现状）不触及「园区容量」所述对象｜通过 |
| BEQR-090 设备关键/辅助分级 | business-enum-quick-reference-v0.1.html:147 | 无本面｜设备分级未触及｜通过 |
| BEQR-091 全园故障与补偿通票 | business-enum-quick-reference-v0.1.html:148 | 无本面｜本合成模块范围（21 个文件现状）不触及「全园故障与补偿通票」所述对象｜通过 |
| BEQR-092 安全严重度 | business-enum-quick-reference-v0.1.html:149 | 无本面｜本合成模块范围（21 个文件现状）不触及「安全严重度」所述对象｜通过 |
| BEQR-093 巡检任务状态 | business-enum-quick-reference-v0.1.html:150 | 无本面｜本合成模块范围（21 个文件现状）不触及「巡检任务状态」所述对象｜通过 |
| BEQR-094 越界类型 | business-enum-quick-reference-v0.1.html:151 | 无本面｜本合成模块范围（21 个文件现状）不触及「越界类型」所述对象｜通过 |
| BEQR-095 团体/包场 | business-enum-quick-reference-v0.1.html:153 | 无本面｜本合成模块范围（21 个文件现状）不触及「团体/包场」所述对象｜通过 |
| BEQR-096 游乐增强阶段 | business-enum-quick-reference-v0.1.html:154 | 无本面｜本合成模块范围（21 个文件现状）不触及「游乐增强阶段」所述对象｜通过 |
| BEQR-111 经销支付方式 | business-enum-quick-reference-v0.1.html:176 | 无本面｜本合成模块范围（21 个文件现状）不触及「经销支付方式」所述对象｜通过 |
| BEQR-115 经销价格与税务快照 | business-enum-quick-reference-v0.1.html:180 | 无本面｜本合成模块范围（21 个文件现状）不触及「经销价格与税务快照」所述对象｜通过 |
| BEQR-118 运营营业日 | business-enum-quick-reference-v0.1.html:187 | 无本面｜本合成模块范围（21 个文件现状）不触及「运营营业日」所述对象｜通过 |
| BEQR-119 对账口径 | business-enum-quick-reference-v0.1.html:188 | 无本面｜本合成模块范围（21 个文件现状）不触及「对账口径」所述对象｜通过 |
| BEQR-120 对账差异四类 | business-enum-quick-reference-v0.1.html:189 | 无本面｜本合成模块范围（21 个文件现状）不触及「对账差异四类」所述对象｜通过 |
| BEQR-121 日结状态 | business-enum-quick-reference-v0.1.html:190 | 无本面｜本合成模块范围（21 个文件现状）不触及「日结状态」所述对象｜通过 |
| BEQR-122 差异处理状态 | business-enum-quick-reference-v0.1.html:191 | 无本面｜本合成模块范围（21 个文件现状）不触及「差异处理状态」所述对象｜通过 |
| BEQR-123 结算状态 | business-enum-quick-reference-v0.1.html:192 | 无本面｜本合成模块范围（21 个文件现状）不触及「结算状态」所述对象｜通过 |
| BEQR-124 综合通票定价 | business-enum-quick-reference-v0.1.html:193 | 无本面｜本合成模块范围（21 个文件现状）不触及「综合通票定价」所述对象｜通过 |
| BEQR-125 商户业态 | business-enum-quick-reference-v0.1.html:195 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户业态」所述对象｜通过 |
| BEQR-126 合作模式 | business-enum-quick-reference-v0.1.html:196 | 无本面｜本合成模块范围（21 个文件现状）不触及「合作模式」所述对象｜通过 |
| BEQR-127 退款资金流向 | business-enum-quick-reference-v0.1.html:197 | 无本面｜本合成模块范围（21 个文件现状）不触及「退款资金流向」所述对象｜通过 |
| BEQR-128 物业费 | business-enum-quick-reference-v0.1.html:198 | 无本面｜本合成模块范围（21 个文件现状）不触及「物业费」所述对象｜通过 |
| BEQR-129 能源费与损耗费 | business-enum-quick-reference-v0.1.html:199 | 无本面｜本合成模块范围（21 个文件现状）不触及「能源费与损耗费」所述对象｜通过 |
| BEQR-130 税费配置 | business-enum-quick-reference-v0.1.html:201 | 无本面｜本合成模块范围（21 个文件现状）不触及「税费配置」所述对象｜通过 |
| BEQR-131 三方对账框架 | business-enum-quick-reference-v0.1.html:202 | 无本面｜本合成模块范围（21 个文件现状）不触及「三方对账框架」所述对象｜通过 |
| BEQR-132 开票状态 | business-enum-quick-reference-v0.1.html:203 | 无本面｜本合成模块范围（21 个文件现状）不触及「开票状态」所述对象｜通过 |
| BEQR-143 四类主体 | business-enum-quick-reference-v0.1.html:226 | 无本面｜本合成模块范围（21 个文件现状）不触及「四类主体」所述对象｜通过 |
| BEQR-144 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 适用｜business_entity_id 中文名「经营主体」｜通过 |
| BEQR-145 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 无本面｜本合成模块范围（21 个文件现状）不触及「城市项目公司」所述对象｜通过 |
| BEQR-146 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 无本面｜本合成模块范围（21 个文件现状）不触及「运营公司职责」所述对象｜通过 |
| BEQR-147 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要收入归属」所述对象｜通过 |
| BEQR-148 合作模式 | business-enum-quick-reference-v0.1.html:231 | 无本面｜本合成模块范围（21 个文件现状）不触及「合作模式」所述对象｜通过 |
| BEQR-149 职级 | business-enum-quick-reference-v0.1.html:232 | 无本面｜本合成模块范围（21 个文件现状）不触及「职级」所述对象｜通过 |
| BEQR-150 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 适用｜终端身份按服务端派生属性｜通过 |
| BEQR-151 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 无本面｜本合成模块范围（21 个文件现状）不触及「最终权限计算」所述对象｜通过 |
| BEQR-152 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 适用｜平台管理员全量（D-032）未变；最高权限逐笔见 PGM-041｜通过 |
| BEQR-153 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 适用｜开关兜底拒绝写请求未改｜通过 |
| BEQR-154 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 无本面｜本合成模块范围（21 个文件现状）不触及「综合体项目状态」所述对象｜通过 |
| BEQR-155 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户/运营主体状态」所述对象｜通过 |
| BEQR-156 合同状态 | business-enum-quick-reference-v0.1.html:239 | 无本面｜本合成模块范围（21 个文件现状）不触及「合同状态」所述对象｜通过 |
| BEQR-157 员工状态 | business-enum-quick-reference-v0.1.html:240 | 无本面｜本合成模块范围（21 个文件现状）不触及「员工状态」所述对象｜通过 |
| BEQR-158 设备状态 | business-enum-quick-reference-v0.1.html:241 | 无本面｜本合成模块范围（21 个文件现状）不触及「设备状态」所述对象｜通过 |
| BEQR-159 权限状态 | business-enum-quick-reference-v0.1.html:242 | 无本面｜本合成模块范围（21 个文件现状）不触及「权限状态」所述对象｜通过 |
| BEQR-160 招商线索状态 | business-enum-quick-reference-v0.1.html:248 | 无本面｜本合成模块范围（21 个文件现状）不触及「招商线索状态」所述对象｜通过 |
| BEQR-161 招商意向阶段 | business-enum-quick-reference-v0.1.html:249 | 无本面｜本合成模块范围（21 个文件现状）不触及「招商意向阶段」所述对象｜通过 |
| BEQR-162 品牌/客户库类型 | business-enum-quick-reference-v0.1.html:250 | 无本面｜本合成模块范围（21 个文件现状）不触及「品牌/客户库类型」所述对象｜通过 |
| BEQR-163 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 无本面｜本合成模块范围（21 个文件现状）不触及「合同变更类型」所述对象｜通过 |
| BEQR-164 退租结算状态 | business-enum-quick-reference-v0.1.html:252 | 无本面｜本合成模块范围（21 个文件现状）不触及「退租结算状态」所述对象｜通过 |
| BEQR-165 内部应付执行状态 | business-enum-quick-reference-v0.1.html:253 | 无本面｜本合成模块范围（21 个文件现状）不触及「内部应付执行状态」所述对象｜通过 |
| BEQR-166 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 无本面｜本合成模块范围（21 个文件现状）不触及「合同审批链」所述对象｜通过 |
| BEQR-167 租金/账单类型 | business-enum-quick-reference-v0.1.html:256 | 无本面｜本合成模块范围（21 个文件现状）不触及「租金/账单类型」所述对象｜通过 |
| BEQR-168 营业额抽成模式 | business-enum-quick-reference-v0.1.html:257 | 无本面｜本合成模块范围（21 个文件现状）不触及「营业额抽成模式」所述对象｜通过 |
| BEQR-169 公区能源费 | business-enum-quick-reference-v0.1.html:258 | 无本面｜本合成模块范围（21 个文件现状）不触及「公区能源费」所述对象｜通过 |
| BEQR-170 铺位租赁状态 | business-enum-quick-reference-v0.1.html:259 | 无本面｜本合成模块范围（21 个文件现状）不触及「铺位租赁状态」所述对象｜通过 |
| BEQR-171 物业工单类型 | business-enum-quick-reference-v0.1.html:260 | 无本面｜本合成模块范围（21 个文件现状）不触及「物业工单类型」所述对象｜通过 |
| BEQR-172 物业工单状态 | business-enum-quick-reference-v0.1.html:262 | 无本面｜本合成模块范围（21 个文件现状）不触及「物业工单状态」所述对象｜通过 |
| BEQR-173 商户服务工单状态 | business-enum-quick-reference-v0.1.html:264 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户服务工单状态」所述对象｜通过 |
| BEQR-174 工单时效 | business-enum-quick-reference-v0.1.html:265 | 无本面｜本合成模块范围（21 个文件现状）不触及「工单时效」所述对象｜通过 |
| BEQR-175 楼宇机电设备状态 | business-enum-quick-reference-v0.1.html:266 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇机电设备状态」所述对象｜通过 |
| BEQR-176 设备维保类型 | business-enum-quick-reference-v0.1.html:267 | 无本面｜本合成模块范围（21 个文件现状）不触及「设备维保类型」所述对象｜通过 |
| BEQR-177 能耗表计类型 | business-enum-quick-reference-v0.1.html:268 | 无本面｜本合成模块范围（21 个文件现状）不触及「能耗表计类型」所述对象｜通过 |
| BEQR-178 抄表状态 | business-enum-quick-reference-v0.1.html:269 | 无本面｜本合成模块范围（21 个文件现状）不触及「抄表状态」所述对象｜通过 |
| BEQR-179 能耗归集对象 | business-enum-quick-reference-v0.1.html:270 | 无本面｜本合成模块范围（21 个文件现状）不触及「能耗归集对象」所述对象｜通过 |
| BEQR-180 楼宇巡检任务状态 | business-enum-quick-reference-v0.1.html:271 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇巡检任务状态」所述对象｜通过 |
| BEQR-181 楼宇巡检类型 | business-enum-quick-reference-v0.1.html:273 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇巡检类型」所述对象｜通过 |
| BEQR-182 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 无本面｜本合成模块范围（21 个文件现状）不触及「考勤日结果」所述对象｜通过 |
| BEQR-183 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 无本面｜本合成模块范围（21 个文件现状）不触及「打卡方式」所述对象｜通过 |
| BEQR-184 请假类型 | business-enum-quick-reference-v0.1.html:281 | 无本面｜本合成模块范围（21 个文件现状）不触及「请假类型」所述对象｜通过 |
| BEQR-185 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 无本面｜本合成模块范围（21 个文件现状）不触及「请假单状态」所述对象｜通过 |
| BEQR-186 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 无本面｜本合成模块范围（21 个文件现状）不触及「薪资单构成」所述对象｜通过 |
| BEQR-187 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 无本面｜本合成模块范围（21 个文件现状）不触及「员工提成发放」所述对象｜通过 |
| BEQR-189 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 无本面｜本合成模块范围（21 个文件现状）不触及「管理费方式」所述对象｜通过 |
| BEQR-192 分红税务 | business-enum-quick-reference-v0.1.html:298 | 无本面｜本合成模块范围（21 个文件现状）不触及「分红税务」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（21 个文件现状）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-002 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 无本面｜本合成模块范围（21 个文件现状）不触及「一笔业务怎么走」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜本合成模块范围（21 个文件现状）不触及「系统通用能力：数据可改可删、操作有引导、权限分岗」所述对象｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 适用｜第 570、576、577 行：设施与员工档案删除由最高权限本人审批、最高权限发起即直接生效、申请人无从自审、占用检查照走；游乐权益绑定同上，墓碑只由最高权限恢复；同句其余入口已登记 XM-M036A-2｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 无本面｜本合成模块范围（21 个文件现状）不触及「后台的"新建/录入"改成分步引导」所述对象｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 无本面｜本合成模块范围（21 个文件现状）不触及「验证用的二维码一律动态刷新（间隔可在后台设定）」所述对象｜通过 |
| BSM-007 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 适用｜平台管理员不因身份获得删除审批与墓碑复活（用例断言）；日常功能未收紧｜通过 |
| BSM-009 基础管理 | business-system-map-for-operations-v0.1.html:596 | 无本面｜本合成模块范围（21 个文件现状）不触及「基础管理」所述对象｜通过 |
| BSM-011 客人端 | business-system-map-for-operations-v0.1.html:606 | 无本面｜本合成模块范围（21 个文件现状）不触及「客人端」所述对象｜通过 |
| BSM-012 酒吧平板/工作手机 | business-system-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（21 个文件现状）不触及「酒吧平板/工作手机」所述对象｜通过 |
| BSM-014 酒吧管理 | business-system-map-for-operations-v0.1.html:621 | 无本面｜本合成模块范围（21 个文件现状）不触及「酒吧管理」所述对象｜通过 |
| BSM-016 游乐管理 | business-system-map-for-operations-v0.1.html:631 | 适用｜游乐项目权益绑定删除与墓碑恢复见 BSM-004｜通过 |
| BSM-017 商户管理 | business-system-map-for-operations-v0.1.html:637 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户管理」所述对象｜通过 |
| BSM-019 物业服务 | business-system-map-for-operations-v0.1.html:650 | 无本面｜本合成模块范围（21 个文件现状）不触及「物业服务」所述对象｜通过 |
| BSM-020 楼宇运营 | business-system-map-for-operations-v0.1.html:656 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇运营」所述对象｜通过 |
| BSM-023 财务对账 | business-system-map-for-operations-v0.1.html:671 | 适用｜财务对账控制器 12 条断言执行并通过｜通过 |
| BSM-026 各类入口 | business-system-map-for-operations-v0.1.html:686 | 无本面｜本合成模块范围（21 个文件现状）不触及「各类入口」所述对象｜通过 |
| BSM-027 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 无本面｜本合成模块范围（21 个文件现状）不触及「基础管理：先把综合体的盘子管清楚」所述对象｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 适用｜并入 BSM-004｜通过 |
| BSM-029 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-034 核销、退款与有效期规则 | business-system-map-for-operations-v0.1.html:796 | 无本面｜本合成模块范围（21 个文件现状）不触及「核销、退款与有效期规则」所述对象｜通过 |
| BSM-035 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 适用｜储值扣款能力码仍在 payTicketOrder，测试已按现名校验｜通过 |
| BSM-037 客人端：客人自己买票、预定、查询、积分和出示二维码 | business-system-map-for-operations-v0.1.html:850 | 无本面｜本合成模块范围（21 个文件现状）不触及「客人端：客人自己买票、预定、查询、积分和出示二维码」所述对象｜通过 |
| BSM-038 主要功能 | business-system-map-for-operations-v0.1.html:854 | 适用｜同 BSM-035｜通过 |
| BSM-039 和谁有关 | business-system-map-for-operations-v0.1.html:873 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-040 登录与关键规则 | business-system-map-for-operations-v0.1.html:883 | 无本面｜本合成模块范围（21 个文件现状）不触及「登录与关键规则」所述对象｜通过 |
| BSM-041 会员注册、认证与收费边界 | business-system-map-for-operations-v0.1.html:898 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员注册、认证与收费边界」所述对象｜通过 |
| BSM-042 会员积分活动与发券 | business-system-map-for-operations-v0.1.html:907 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员积分活动与发券」所述对象｜通过 |
| BSM-043 会员营销明确边界 | business-system-map-for-operations-v0.1.html:916 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员营销明确边界」所述对象｜通过 |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 无本面｜本合成模块范围（21 个文件现状）不触及「酒吧平板/工作手机：平板点酒，工作手机核销」所述对象｜通过 |
| BSM-045 主要功能 | business-system-map-for-operations-v0.1.html:933 | 适用｜最高权限会话（master_authority、platform-company）在员工档案两条删除路径可用（回归以生产形状夹具执行）｜通过 |
| BSM-046 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 无本面｜本合成模块范围（21 个文件现状）不触及「平板图形点单（POS）」所述对象｜通过 |
| BSM-047 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-051 酒吧管理：从入场、开台到结账关台 | business-system-map-for-operations-v0.1.html:1010 | 无本面｜本合成模块范围（21 个文件现状）不触及「酒吧管理：从入场、开台到结账关台」所述对象｜通过 |
| BSM-052 主要功能 | business-system-map-for-operations-v0.1.html:1014 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-053 和谁有关 | business-system-map-for-operations-v0.1.html:1041 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-054 桌台/账单状态与经营规则 | business-system-map-for-operations-v0.1.html:1053 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台/账单状态与经营规则」所述对象｜通过 |
| BSM-055 桌台、菜单与优惠券 | business-system-map-for-operations-v0.1.html:1088 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台、菜单与优惠券」所述对象｜通过 |
| BSM-056 正式未付款订单挂起 | business-system-map-for-operations-v0.1.html:1101 | 无本面｜本合成模块范围（21 个文件现状）不触及「正式未付款订单挂起」所述对象｜通过 |
| BSM-057 服务点单：客人点专项服务，公司统一收款、再按名录分账 | business-system-map-for-operations-v0.1.html:1120 | 无本面｜本合成模块范围（21 个文件现状）不触及「服务点单：客人点专项服务，公司统一收款、再按名录分账」所述对象｜通过 |
| BSM-058 怎么点、怎么收钱 | business-system-map-for-operations-v0.1.html:1124 | 无本面｜本合成模块范围（21 个文件现状）不触及「怎么点、怎么收钱」所述对象｜通过 |
| BSM-059 钱怎么分、怎么退 | business-system-map-for-operations-v0.1.html:1135 | 无本面｜本合成模块范围（21 个文件现状）不触及「钱怎么分、怎么退」所述对象｜通过 |
| BSM-060 供应链库存：管供应商、采购、库房、消耗和存酒 | business-system-map-for-operations-v0.1.html:1151 | 无本面｜本合成模块范围（21 个文件现状）不触及「供应链库存：管供应商、采购、库房、消耗和存酒」所述对象｜通过 |
| BSM-061 主要功能 | business-system-map-for-operations-v0.1.html:1155 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-062 和谁有关 | business-system-map-for-operations-v0.1.html:1174 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-063 库存类型与规则 | business-system-map-for-operations-v0.1.html:1186 | 无本面｜本合成模块范围（21 个文件现状）不触及「库存类型与规则」所述对象｜通过 |
| BSM-064 采购建议与门店要货 | business-system-map-for-operations-v0.1.html:1208 | 无本面｜本合成模块范围（21 个文件现状）不触及「采购建议与门店要货」所述对象｜通过 |
| BSM-065 外部经销商准入、价格与库存 | business-system-map-for-operations-v0.1.html:1216 | 无本面｜本合成模块范围（21 个文件现状）不触及「外部经销商准入、价格与库存」所述对象｜通过 |
| BSM-066 经销订单、付款与履约 | business-system-map-for-operations-v0.1.html:1226 | 无本面｜本合成模块范围（21 个文件现状）不触及「经销订单、付款与履约」所述对象｜通过 |
| BSM-067 经销退货、退款、红冲与资金归属 | business-system-map-for-operations-v0.1.html:1238 | 无本面｜本合成模块范围（21 个文件现状）不触及「经销退货、退款、红冲与资金归属」所述对象｜通过 |
| BSM-068 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | business-system-map-for-operations-v0.1.html:1253 | 无本面｜本合成模块范围（21 个文件现状）不触及「游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常」所述对象｜通过 |
| BSM-069 主要功能 | business-system-map-for-operations-v0.1.html:1257 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-070 和谁有关 | business-system-map-for-operations-v0.1.html:1276 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-071 游乐类型与规则 | business-system-map-for-operations-v0.1.html:1287 | 无本面｜本合成模块范围（21 个文件现状）不触及「游乐类型与规则」所述对象｜通过 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户管理：商户在授权范围内完整使用自己的系统」所述对象｜通过 |
| BSM-073 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-074 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-078 物业服务：商户和现场的报修、投诉、整改都进工单 | business-system-map-for-operations-v0.1.html:1370 | 无本面｜本合成模块范围（21 个文件现状）不触及「物业服务：商户和现场的报修、投诉、整改都进工单」所述对象｜通过 |
| BSM-079 主要功能 | business-system-map-for-operations-v0.1.html:1374 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-080 和谁有关 | business-system-map-for-operations-v0.1.html:1388 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-081 楼宇运营：机电设备、能耗抄表、楼宇巡检 | business-system-map-for-operations-v0.1.html:1401 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇运营：机电设备、能耗抄表、楼宇巡检」所述对象｜通过 |
| BSM-082 主要功能 | business-system-map-for-operations-v0.1.html:1405 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-083 和谁有关 | business-system-map-for-operations-v0.1.html:1414 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 适用｜员工档案删除见 BSM-004｜通过 |
| BSM-085 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 无本面｜本合成模块范围（21 个文件现状）不触及「日常工作」所述对象｜通过 |
| BSM-086 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 无本面｜本合成模块范围（21 个文件现状）不触及「排班、考勤与请假」所述对象｜通过 |
| BSM-087 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 无本面｜本合成模块范围（21 个文件现状）不触及「我的薪资」所述对象｜通过 |
| BSM-088 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 无本面｜本合成模块范围（21 个文件现状）不触及「员工提成、活动执行与人员服务边界」所述对象｜通过 |
| BSM-098 财务对账：把经营事实整理成财务能确认的口径 | business-system-map-for-operations-v0.1.html:1636 | 适用｜同 BSM-023｜通过 |
| BSM-099 内部结算价目录（按商品规格逐项建目录） | business-system-map-for-operations-v0.1.html:1639 | 无本面｜本合成模块范围（21 个文件现状）不触及「内部结算价目录（按商品规格逐项建目录）」所述对象｜通过 |
| BSM-100 结算配置中心（后台总控页） | business-system-map-for-operations-v0.1.html:1641 | 适用｜财务控制器切片恢复，入口权限断言执行｜通过 |
| BSM-101 项目零售参考价 | business-system-map-for-operations-v0.1.html:1643 | 无本面｜本合成模块范围（21 个文件现状）不触及「项目零售参考价」所述对象｜通过 |
| BSM-102 主要功能 | business-system-map-for-operations-v0.1.html:1648 | 适用｜同 BSM-023｜通过 |
| BSM-103 和谁有关 | business-system-map-for-operations-v0.1.html:1666 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| BSM-104 收入归属：哪类收入归哪个业态主体 | business-system-map-for-operations-v0.1.html:1678 | 无本面｜本合成模块范围（21 个文件现状）不触及「收入归属：哪类收入归哪个业态主体」所述对象｜通过 |
| BSM-105 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | business-system-map-for-operations-v0.1.html:1697 | 无本面｜本合成模块范围（21 个文件现状）不触及「三方对账框架：系统流水、渠道对账单、资金到账两两核对」所述对象｜通过 |
| BSM-106 对账与结算类型规则 | business-system-map-for-operations-v0.1.html:1709 | 适用｜同 BSM-023｜通过 |
| BSM-111 经营看板：让管理层和运营看得懂、能追问 | business-system-map-for-operations-v0.1.html:1781 | 无本面｜本合成模块范围（21 个文件现状）不触及「经营看板：让管理层和运营看得懂、能追问」所述对象｜通过 |
| BSM-112 主要指标 | business-system-map-for-operations-v0.1.html:1785 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要指标」所述对象｜通过 |
| BSM-113 谁会看 | business-system-map-for-operations-v0.1.html:1813 | 无本面｜本合成模块范围（21 个文件现状）不触及「谁会看」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜普通员工档案读写、游乐其余写路径、列表口径未放开；最高权限列表口径登记 XM-M036A-1｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（21 个文件现状）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | business-system-map-for-operations-v0.1.html:1928 | 无本面｜本合成模块范围（21 个文件现状）不触及「系统交付与上线：正式上线要做的安全加固和安装交付」所述对象｜通过 |
| BSM-117 正式上线时的安全加固 | business-system-map-for-operations-v0.1.html:1932 | 适用｜终端写请求兜底拒绝保留｜通过 |
| BSM-118 能自动识别环境的安装包 + 内附教程 | business-system-map-for-operations-v0.1.html:1940 | 无本面｜本合成模块范围（21 个文件现状）不触及「能自动识别环境的安装包 + 内附教程」所述对象｜通过 |
| BSM-119 和谁有关 | business-system-map-for-operations-v0.1.html:1947 | 适用｜并入 BSM-045｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 适用｜终端开关与最高权限规则见上｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜只改红用例所需与 B2 定稿三处 HTML 冲突；系统性最高权限范围与同句入口只登记｜通过 |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main＋冻结 diff 后端全量 263 套 3536 测 0 红（§2），原 23 红全部执行到断言并通过｜通过 |
| GEN-003 测试有效性 | 通用 | 适用｜RED 轮 10 条新增或改动用例在旧生产上红（xm036a_b3_run.log）；财务 12 条、合规 3 条改后真正执行到金额与权限断言；删去的两处自审断言换成更强的「非最高权限不能审批」｜通过 |
| GEN-004 信任边界校验 | 通用 | 适用｜终端写请求兜底拒绝未改，测试以服务端派生属性带终端身份｜通过 |
| GEN-005 授权与最小权限 | 通用 | 适用｜设施、游乐、员工档案删除审批只认最高权限本人；平台管理员不能审批、不能复活墓碑（用例断言 MASTER_APPROVAL_REQUIRED）；最高权限只在删除三类路径上按单据放行，范围方法未改｜通过 |
| GEN-006 数据完整性 | 通用 | 适用｜空间种类删除申请主键随机，连续 50 次申请不撞键（回归）｜通过 |
| GEN-007 并发与幂等 | 通用 | 适用｜同上；游乐最高权限直接删除在同一写入体内｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜切片装配与控制器构造一致（MockitoBean、guard Bean 保留）｜通过 |
| GEN-009 错误处理与恢复 | 通用 | 适用｜最高权限审批员工档案不再误报「所属公司」；设施最高权限直接删除遇占用照抛冲突、整笔回滚｜通过 |
| GEN-010 安全与敏感信息 | 通用 | 适用｜无敏感信息面变化｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜删除申请、审批、直接删除均走既有审计事件或 executeWrite 留痕｜通过 |
| GEN-012 资源与性能边界 | 通用 | 无本面｜无性能面改动｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 适用｜无新依赖｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜无迁移｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜不涉界面｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜游乐两处 javadoc 已改为「只认最高权限本人」，测试注释不再混淆平台管理员与最高权限｜通过 |
| GEN-017 业务来源分类 | 通用 | 适用｜001–003 技术性、004/008 细化、005–007 冲突，B2 双席定稿｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行与后端全量命令输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜治理底座各条见下｜通过 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | platform-governance-map-for-operations-v0.1.html:308 | 适用｜终端开关先于业务写保留｜通过 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | platform-governance-map-for-operations-v0.1.html:339 | 无本面｜本合成模块范围（21 个文件现状）不触及「模块入口：平台治理层包含哪些管理模块」所述对象｜通过 |
| PGM-004 综合体和集团 | platform-governance-map-for-operations-v0.1.html:342 | 无本面｜本合成模块范围（21 个文件现状）不触及「综合体和集团」所述对象｜通过 |
| PGM-005 空间和点位 | platform-governance-map-for-operations-v0.1.html:347 | 无本面｜本合成模块范围（21 个文件现状）不触及「空间和点位」所述对象｜通过 |
| PGM-006 商户和主体 | platform-governance-map-for-operations-v0.1.html:352 | 适用｜经营主体关联字段报错可读（FieldLabels）｜通过 |
| PGM-007 合同和准入 | platform-governance-map-for-operations-v0.1.html:357 | 无本面｜本合成模块范围（21 个文件现状）不触及「合同和准入」所述对象｜通过 |
| PGM-008 员工和手机 | platform-governance-map-for-operations-v0.1.html:362 | 适用｜员工档案删除见 BSM-004｜通过 |
| PGM-009 权限和职级 | platform-governance-map-for-operations-v0.1.html:367 | 适用｜权限：删除审批码不再能让非最高权限批删除｜通过 |
| PGM-010 终端开关 | platform-governance-map-for-operations-v0.1.html:372 | 适用｜治理地图第 738 行「写操作一律拒绝」：拦截器未改，合规测试带身份后执行到权限与幂等断言｜通过 |
| PGM-011 设备管理 | platform-governance-map-for-operations-v0.1.html:378 | 适用｜设施设备删除见 BSM-004｜通过 |
| PGM-012 商户侧 | platform-governance-map-for-operations-v0.1.html:383 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户侧」所述对象｜通过 |
| PGM-013 开放接口 | platform-governance-map-for-operations-v0.1.html:388 | 无本面｜本合成模块范围（21 个文件现状）不触及「开放接口」所述对象｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜删除审批留痕在｜通过 |
| PGM-015 楼宇运营 | platform-governance-map-for-operations-v0.1.html:399 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇运营」所述对象｜通过 |
| PGM-016 应急与隐私 | platform-governance-map-for-operations-v0.1.html:404 | 适用｜最高权限会话在删除路径可用｜通过 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | platform-governance-map-for-operations-v0.1.html:412 | 无本面｜本合成模块范围（21 个文件现状）不触及「综合体和集团：当前一个项目，未来能看集团」所述对象｜通过 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | platform-governance-map-for-operations-v0.1.html:415 | 无本面｜本合成模块范围（21 个文件现状）不触及「物理项目与运营公司：多对多协作，不是固定隶属层级」所述对象｜通过 |
| PGM-019 各主体具体负责什么 | platform-governance-map-for-operations-v0.1.html:424 | 无本面｜本合成模块范围（21 个文件现状）不触及「各主体具体负责什么」所述对象｜通过 |
| PGM-020 主要功能 | platform-governance-map-for-operations-v0.1.html:440 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-021 业务意义 | platform-governance-map-for-operations-v0.1.html:452 | 无本面｜本合成模块范围（21 个文件现状）不触及「业务意义」所述对象｜通过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | platform-governance-map-for-operations-v0.1.html:463 | 无本面｜本合成模块范围（21 个文件现状）不触及「空间和点位：所有业务都要知道“发生在哪”」所述对象｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-024 影响业务 | platform-governance-map-for-operations-v0.1.html:476 | 无本面｜本合成模块范围（21 个文件现状）不触及「影响业务」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-027 业务边界 | platform-governance-map-for-operations-v0.1.html:507 | 无本面｜本合成模块范围（21 个文件现状）不触及「业务边界」所述对象｜通过 |
| PGM-031 外部经销商主体、合同价格与资金治理 | platform-governance-map-for-operations-v0.1.html:555 | 无本面｜本合成模块范围（21 个文件现状）不触及「外部经销商主体、合同价格与资金治理」所述对象｜通过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | platform-governance-map-for-operations-v0.1.html:572 | 无本面｜本合成模块范围（21 个文件现状）不触及「员工和手机：人、私人手机、工作手机要能对应起来」所述对象｜通过 |
| PGM-033 主要功能 | platform-governance-map-for-operations-v0.1.html:576 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-034 工作手机和企业微信 | platform-governance-map-for-operations-v0.1.html:586 | 无本面｜本合成模块范围（21 个文件现状）不触及「工作手机和企业微信」所述对象｜通过 |
| PGM-035 提成发放和人员服务治理 | platform-governance-map-for-operations-v0.1.html:599 | 无本面｜本合成模块范围（21 个文件现状）不触及「提成发放和人员服务治理」所述对象｜通过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | platform-governance-map-for-operations-v0.1.html:611 | 无本面｜本合成模块范围（21 个文件现状）不触及「设备管理：谁领了什么设备，设备能不能用」所述对象｜通过 |
| PGM-037 主要功能 | platform-governance-map-for-operations-v0.1.html:615 | 无本面｜本合成模块范围（21 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-038 和现场业务的关系 | platform-governance-map-for-operations-v0.1.html:624 | 无本面｜本合成模块范围（21 个文件现状）不触及「和现场业务的关系」所述对象｜通过 |
| PGM-039 智能设备场景联动范围 | platform-governance-map-for-operations-v0.1.html:636 | 无本面｜本合成模块范围（21 个文件现状）不触及「智能设备场景联动范围」所述对象｜通过 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | platform-governance-map-for-operations-v0.1.html:643 | 适用｜单独授权不再能绕开「删除只由最高权限审批」（三处）｜通过 |
| PGM-041 权限规则 | platform-governance-map-for-operations-v0.1.html:647 | 适用｜第 528 行第④类逐笔开放：仅删除三类单据路径对最高权限放行并留痕；系统性部分登记 XM-M036A-1｜通过 |
| PGM-042 职级规则 | platform-governance-map-for-operations-v0.1.html:664 | 无本面｜本合成模块范围（21 个文件现状）不触及「职级规则」所述对象｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | platform-governance-map-for-operations-v0.1.html:673 | 无本面｜本合成模块范围（21 个文件现状）不触及「外包人员与审计人员（2026-06-29 新增）」所述对象｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | platform-governance-map-for-operations-v0.1.html:688 | 无本面｜本合成模块范围（21 个文件现状）不触及「桌台、挂单与经销业务权限」所述对象｜通过 |
| PGM-048 商户侧：在授权业务范围内全开放 | platform-governance-map-for-operations-v0.1.html:749 | 无本面｜本合成模块范围（21 个文件现状）不触及「商户侧：在授权业务范围内全开放」所述对象｜通过 |
| PGM-052 楼宇运营：管好建筑本身的设备、能耗和巡检 | platform-governance-map-for-operations-v0.1.html:806 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇运营：管好建筑本身的设备、能耗和巡检」所述对象｜通过 |
| PGM-053 楼宇机电设备 | platform-governance-map-for-operations-v0.1.html:810 | 适用｜设施设备删除见 BSM-004｜通过 |
| PGM-054 能耗计量 | platform-governance-map-for-operations-v0.1.html:821 | 无本面｜本合成模块范围（21 个文件现状）不触及「能耗计量」所述对象｜通过 |
| PGM-055 楼宇巡检 | platform-governance-map-for-operations-v0.1.html:832 | 无本面｜本合成模块范围（21 个文件现状）不触及「楼宇巡检」所述对象｜通过 |
| PGM-056 和谁有关 | platform-governance-map-for-operations-v0.1.html:844 | 无本面｜本合成模块范围（21 个文件现状）不触及「和谁有关」所述对象｜通过 |
| PGM-057 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | platform-governance-map-for-operations-v0.1.html:857 | 无本面｜本合成模块范围（21 个文件现状）不触及「应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文」所述对象｜通过 |
| PGM-058 紧急恢复（破窗机制） | platform-governance-map-for-operations-v0.1.html:860 | 适用｜破窗会话见 BSM-045｜通过 |
| PGM-059 客人与员工的个人信息保护 | platform-governance-map-for-operations-v0.1.html:868 | 无本面｜本合成模块范围（21 个文件现状）不触及「客人与员工的个人信息保护」所述对象｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | platform-governance-map-for-operations-v0.1.html:879 | 无本面｜本合成模块范围（21 个文件现状）不触及「会员身份、营销与生物信息治理」所述对象｜通过 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | platform-governance-map-for-operations-v0.1.html:935 | 无本面｜本合成模块范围（21 个文件现状）不触及「连接关系：平台治理层怎么连接所有业务」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 适用｜已拍板删除审批规则见 BSM-004｜通过 |

共 297 项；逐项给出适用与结论的 57 项，其余 240 项无本面。

## 5. 已定稿 001…008 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」＝0。

## 6. 本次 Findings

无新增。登记不变：XM-M036A-1（最高权限在范围检查中被拒的系统性问题，宜单独立项）、XM-M036A-2（删除审批人不限最高权限的同句入口，→M266、M193、M170、M132、M009、M201、M075）、游乐删除无删前占用检查（→M230）、consumeInternal 不挂储值能力码、核销码毫秒加随机、CI 不跑后端测试（→M003/M005）；另补观察：M010RegressionTests 的最高权限夹具带项目号 m010_p，恰好绕过员工档案公司范围，掩盖了本轮 007 的生产缺陷（→M010 所在模块串行审时核夹具形状）。

## 7. 结论

席甲 B5-r1 票：**PASS**。范围全覆盖、297 项逐项、后端全量实跑复核 0 红；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
