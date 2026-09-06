CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M006 B5-r5 席甲（Claude Fable 5.1）全模块复审原始报告

版本：V0.01
模块：M006 `10-backend-main-access-01`，6/322。
步骤：B5；ledger IN_PROGRESS；B3 已执行 5 次（第 4、5 次为操作员例外与批准，由 Claude 执行）；方案复核已修 2；子代理失败重跑 0。
审计结论：PASS（本方完整审计无新意见；旧 001—036 均已修且无回退；登记项不构成缺陷）。仅本席票，须与 Grok B5-r5 合取且 B5 后四值全等才收口。

## 0. 席位与输入

操作员指令原话：“你和Grok按2方审计提示词中的要求做一个m006全量审计,只是把codex gpt 6要执行的全部内容换成 fable 5.1 extra”；“恢复 M006，批准第 5 次修复”。控制提示词 V0.17；findings V0.71（035/036 第 5 次 B3 已修；B4-r5 两席 raw 见 modules/M006-b4-r5-*-raw.md）。
本次为修复后全模块复审：14 文件、52 条冻结判据全部审阅（含第 5 次修复后重读 AuthorizationService 全文 521 行与 AccessService 1948 行），未只重看 diff。B5 期间未改业务源、测试源、三份 HTML、锁、ledger、findings。
双席独立顺序：先派 Grok（目标 modules/M006-b5-r5-grok-raw.md）；本席报告在读取 Grok raw 之前写成，只在 Grok 进程退出后落盘；未读取 Grok 原始 JSON、stderr 或本轮 raw 正文。
当前源码 = 第 5 次 B3 后工作区（after tree 7e3d9e970fd7b01735a78089cbeb4921e122cf80 的业务文件，工作树 SHA 与 after blob 逐一相等）。三份 HTML 与 HEAD 逐字相同。

## 1. 14 文件完整覆盖（SHA256 为 160 上实算；合计 3289 行）

| 文件 | 行 / 字节 | SHA256 | 全文核对结果 |
|---|---|---|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 / 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 六字段主体记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 299 / 15252 | 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c | 31 个入口权限码/幂等键前置；审批类双权限；L215 英文经翻译层。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 / 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 契约 + noop。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 / 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 / 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 申请快照。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 / 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 / 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 259 / 10273 | c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680 | 仓储契约，默认实现 fail-closed。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 1948 / 97875 | 8d0b2ca064302aa3837577f415d452285292ab112db9078c1a5a9b7e1c7db14e | V0.14。036 已修（L186-193 先 requireProjectScope）；034 四处中文；其余英文键文案经翻译层。无新意见。 |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 37 / 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | phoneHash @JsonIgnore。无意见。 |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 521 / 24407 | 6ad9fa0fc3b37b27f137aaf82fc63adcbb5719fec9048e72f1232399d920cbb8 | V0.05。035 已修：dailyPermissions() 惰性 Supplier，requirePermissions / permissionGrant / computeEffectivePermissions / holdsPermissionViaRole 各持一份；DENY 优先、日常桥、role_platform_admin 高危剔除、一次性消费、主体范围（031）逐行未变。无新意见。 |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 48 / 2000 | 8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39 | dev/local；缺账号拒绝。无意见。 |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 / 882 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 输出记录；wildcard 恒 false（登记）。无意见。 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 69 / 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | prod；bcrypt；affected!=1 报错。无意见。 |

## 2. 52 条冻结判据逐项

“本 14 文件无该业务执行器”= 按模块载体核对后无可归责实现，不是删除判据，也不是该业务全仓通过。

| 判据 | 结论 | 证据 |
|---|---|---|
| GEN-001 必要性 | PASS（登记 2 项非缺陷） | `listGrantRequests(Integer)` 单参重载、私有 `slug()` 无调用方；无行为影响，登记不入队。 |
| GEN-002 可执行正确性 | PASS | 60 源 javac exit 0；101 项回归 0 FAIL（§4）。 |
| GEN-003 测试有效性 | PASS | 101 项真实行为断言本轮实跑；035/036 两条新测试在 before 树 FAIL、after 树 PASS（B4-r5 raw）。JDBC 类 12 项按名核对（本轮未跑）。 |
| GEN-004 信任边界 | PASS | 入口 requireText/requireRequest/白名单；@Valid DTO；政策只读。 |
| GEN-005 授权最小权限 | PASS | 逐入口 requirePermission(s)；DENY 优先；桥仅日常权限；高危须审批；授权人不得授出自身不持有权限；受益人/申请人不得自审（测试 administratorDenyWins…、beneficiaryCannotApproveOwnGrant 等 PASS）。 |
| GEN-006 数据完整性 | PASS | @Transactional + lockGrantAuthorizationFacts + 指纹；凭证 upsert affected==1。 |
| GEN-007 并发幂等 | PASS | IdempotencyService.execute 全覆盖写入口；主体范围 u:len:id:org；Supplier 为调用局部，无跨请求共享状态。 |
| GEN-008 接口一致性 | PASS | 本轮未改接口/DTO/错误码。 |
| GEN-009 错误处理 | PASS | 036 已修：orphan copy → 403 PROJECT_SCOPE_REQUIRED；本轮 18 个入口 orphan 主体扫描 0 次原始异常（§4 探针 S）。 |
| GEN-010 安全敏感信息 | PASS | bcrypt；phoneHash 不出 JSON；错误不回显输入原值。 |
| GEN-011 可观测审计 | PASS | 写入口 recordSuccess/recordFailure 成对。 |
| GEN-012 资源性能边界 | PASS | 035 已修：字典 200 条时 effectivePermissions() 触发 listPermissions() 2 次（修复前 201）；requirePermission 1 次；普通 staff 0 次（§4 探针 G）。 |
| GEN-013 构建依赖 | PASS（本模块无构建文件） | 既有 classpath 下编译通过。 |
| GEN-014 迁移 | PASS（本模块无迁移文件） | 本轮未改 SQL。 |
| GEN-015 界面文案语言 | PASS | 034 中文；6 处英文字段键经 FieldLabels.translate 全中文（§4 探针 T）；本轮新增文案为中文注释。 |
| GEN-016 文档可验证 | PASS（本模块无文档文件） | 类注释引用与代码一致。 |
| GEN-017 业务来源分类 | PASS | 本轮无新意见；登记项均为技术性、无业务发明。 |
| GEN-018 第一性原理证据 | PASS | 结论均落到源码行号 + 可复现命令输出。 |
| BEQR-157 / 182 / 183 / 184 / 185 / 186 / 187 | 本 14 文件无该业务执行器 | 账号状态非人事状态；考勤/请假/薪资单/提成在 M007/M008；StaffLevelAssignment 仅职级与基本工资（CNY、非负）。无冲突、无发明。 |
| BSM-001 策略可配置 | PASS | AccessGrantPolicy 后台配置带上下限，查询不可覆盖。 |
| BSM-003 可改可删有引导权限分岗 | PASS | 岗位模板软删除+存档+审批；角色/模板/职级分岗。 |
| BSM-009 基础管理 | PASS | 员工须注册审批（STAFF_REGISTRATION_REQUIRED）。 |
| BSM-028 / 029 / 073 | 细化 PASS | 组织与商户隔离（CROSS_ORG/CROSS_MERCHANT，本轮 orphan 扫描全部 PROJECT_SCOPE_REQUIRED）；用户权限知道组织、商户、职级、审批状态。 |
| BSM-084 / 085 | PASS | 审批通过自动赋 role_staff_self（测试 registrationCreatesAccountOnlyAfterCompanyApproval）；手工建号仅 outsourced_vendor。 |
| BSM-086 / 087 / 088 | 本 14 文件无该业务执行器 | 同 BEQR-18x。 |
| BSM-114 / 115 / 120 | PASS | 三层 scope 守卫；权限闸前置各域；权限按最小颗粒度。 |
| PGM-001 | PASS | 归属/资格/责任可追溯；提示全中文。 |
| PGM-008 / 022 / 024 / 034 / 035 / 036 | 本 14 文件无该业务执行器 | 设备/空间/企微/提成/设备管理在其他模块。 |
| PGM-026 | 细化 PASS | 商户账号必须指定商户。 |
| PGM-032 / 033 | PASS（注册路径）/ 无执行器（手机绑定） | 同 BSM-085。 |
| PGM-048 | PASS | 商户管理员本商户操作可做、跨商户拒绝；平台配置需平台管理员。 |
| PGM-059 | PASS | 不存手机号摘要、不输出 phoneHash。 |
| PGM-066 | PASS | 高危先审批；报错全中文。 |

## 3. 三线复核

- 第一性原理：本模块承载 BSM-115“先被卡住再放行”与 PGM-066“高危授出前先审批”；声称行为经 101 项回归 + 4 个探针验证；未验证前提：JDBC 集成类、完整 mvn test、生产 profile 行为。
- 顺从式：§2 逐条；旧 001—036 复核：001/007/009/011/013/025/026/031/032/034 由既有测试 PASS；035/036 由本轮探针与新测试 PASS，无回退。
- 对抗式（grilling 自问）：Q1 Supplier 会否跨请求缓存？——每次调用新建，局部持有；不会。Q2 普通员工是否因修复新增查询？——探针 G：staff 0 次。Q3 MASTER 主体？——wildcard 分支 MASTER 短路保留。Q4 除 copy 外还有无 orphan NPE？——18 入口扫描 0 原始异常。Q5 英文回显？——探针 T 6/6 中文。Q6 高危绕过？——isExplicitOrDailyGrant 逻辑不变，测试 administratorKeepsDailyPermissionsWithoutAutomaticHighRole PASS。
- ponytail-review：`AccessService.java:L789-804 delete: 单参 listGrantRequests(Integer)`；`AccessService.java:L1898 delete: 未使用 slug()`；`net: -20 lines possible.` 均为登记项，不构成 FAIL。

## 4. 实际验证（本席实跑，160 上 .scratch 临时目录，自动清理；2026-09-06）

1. 回归（60 源 javac exit 0）：`M006SecurityRegressionTests 36 PASS / 0 FAIL`、`AccessServiceTests 35 PASS / 0 FAIL`、`AuthorizationServiceTests 21 PASS / 0 FAIL`、`IdempotencyServiceTests 9 PASS / 0 FAIL`；`RESULT 101 PASS / 0 FAIL exit 0`。
2. 探针 G（GEN-012，修复后）：`effectivePermissions() listPermissions_calls=2 grants=133`；`requirePermission(一个日常码) listPermissions_calls=1`；普通 staff `listPermissions_calls=0`。
3. 探针 H（GEN-009，修复后）：`copyPermissionTemplate(orphan staff, org-owned template) -> ApiException code=PROJECT_SCOPE_REQUIRED status=403`；同组织 staff 对照 NO_EXCEPTION。
4. 探针 S（GEN-009 全入口扫描）：orphan staff 调用 18 个入口 → 16 个 PROJECT_SCOPE_REQUIRED、2 个 INVALID_GRANT_POLICY（探针政策 JSON 只配 low 等级，属夹具限制）、effectivePermissions 自身视图 OK；`SWEEP_RESULT raw_exceptions=0`。
5. 探针 T（GEN-015）：6 条英文字段键文案经真实 FieldLabels.translate 全部中文，`RESULT PASS all translated to Chinese`。
6. 残留扫描：src/main 无用户可见“（approve）/（reject）/实际为：+currency/USB 最高权限审批”。
未跑：M006JdbcRegressionTests（需 complex_test 库）、完整 mvn test、数据库迁移、前端、真实支付/短信厂商。

## 5. 本方意见

无新缺陷。登记（不入队，与 findings V0.71 一致）：单参 listGrantRequests、私有 slug()、EffectivePermissions.wildcard 恒 false、StaffService.java:363 同类英文回显（M008）。

## 6. 技能自检与四值

实际调用：superpowers:verification-before-completion、superpowers:receiving-code-review、grilling、ponytail:ponytail-review、tdd、diagnosing-bugs（本轮 B3）；未调用：codebase-design、writing-for-agents（收口文件时）；缺口：batch-grill-me、ubiquitous-language、git-guardrails-claude-code。
B5 后四值（④ 取 modules/M006-b5-r5-grok-raw.md 最后一条）由席甲在两席 raw 落齐后实算并打进回执；本报告落盘时 ①②③ = 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de。

结论：本席 PASS。收口须 Grok B5-r5 PASS 合取 + 四值全等；届时先改 ledger 为 PASS，再写 modules/M006-10-backend-main-access-01.md。

落盘 UTC：2026-09-06T16:31:57.203877+00:00


## V0.02 — 接收 Grok B5-r5 报告与收口（非新独立票，原 V0.01 13014 字节前缀保留）

2026-09-06T16:33:39Z 取得适配器回执 REPORT_PATH=docs/audit-2026-09/modules/M006-b5-r5-grok-raw.md、EXIT_CODE=0（16:17:26Z–16:31:47Z）。Grok raw 31051 字节 / SHA256 da7fa5257660d431d8dce08fe1c16363a804f27d0955e2caf89d9bcbf3b37d1f，第一行锁哈希与本席实算一致；本席原票先于读取封存。
Grok：PASS，无新 GR，旧 001—036 不回退，111 PASS/0 FAIL；GEN-012/009 独立探针与本席一致。合取 PASS。
B5 后四值：① ② ③ ④ = 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。先写 ledger PASS，再写收口文件 modules/M006-10-backend-main-access-01.md。
