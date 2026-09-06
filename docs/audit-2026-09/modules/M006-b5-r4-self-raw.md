CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M006 B5-r4 席甲（Claude Fable 5.1）全模块复审原始报告

版本：V0.01
模块：M006 `10-backend-main-access-01`，6/322。
步骤：B5；ledger IN_PROGRESS；B3 已执行 4 次（第 4 次为操作员例外，由 Claude 执行）；方案复核已修 2；子代理失败重跑 0。
审计结论：FAIL（本方完整审计共 2 条新意见 CX-B5-01、CX-B5-02，均为工程缺陷；旧 001—034 未见回归）。

## 0. 席位与输入

操作员指令原话（2026-09-06）：“你和Grok按2方审计提示词中的要求做一个m006全量审计,只是把codex gpt 6要执行的全部内容换成 fable 5.1 extra”。本轮控制提示词 V0.17 中“你（Codex）”的职责由 Claude Fable 5.1 承担；Grok 席乙不变。
控制提示词：D:\AI\ccwork\160-audit-prompt.md V0.17。findings.md V0.66；B4-r4 两席结论见 modules/M006-b4-r4-*-raw.md。
本次为修复后全模块复审：14 文件、52 条冻结判据全部审阅；未只重看 034 或修复 diff。B5 期间未改业务源、测试源、三份 HTML、锁、ledger、findings。
双席独立顺序：先派 Grok（目标 modules/M006-b5-r4-grok-raw.md）；本席报告在读取 Grok raw 正文之前写成，只在 Grok 进程退出后落盘到 160；本席未读取 Grok 原始 JSON、stderr 或本轮 raw 正文。历史 raw（b1、b4-r1…r3、b5-r2/r3）仅用于核对旧 ID 与既往裁定，不作为依据。
当前源码 = 第 4 次 B3 后工作区（after tree 54999203b0587456b8d2f473b5ee0f5e3d67908b，此后业务源未再变，见 §5 SHA 表）。三份 HTML 与 HEAD 逐字相同。

## 1. 14 文件完整覆盖

| 文件 | 行 / 字节 | SHA256 | 全文核对结果 |
|---|---|---|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 / 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 六字段主体记录，无逻辑。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 299 / 15252 | 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c | 31 个入口逐一核对权限码与幂等键前置；审批类入口双权限（review、lifecycle approve/reject）；self 端 effective-permissions 仅需已登录；L215 英文文案经 FieldLabels 翻译（§4 探针）。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 / 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 函数式契约 + noop 工厂；生产构造器已改为 getIfAvailable() 且 Service 对 null 显式拒绝（M006-029）。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 / 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 / 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 申请快照 24 字段，permissionCodes 防空复制。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 / 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 / 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 输出记录。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 259 / 10273 | c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680 | 仓储契约：默认实现 fail-closed（UnsupportedOperationException）、锁/指纹/分页契约与 Jdbc 实现对应；pageGrantRequests 默认按组织+商户过滤。无意见。 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 1946 / 97639 | 01f5ede7c81dd51e3bcdfccc4bcff08f840756f46ea09e6fa6efb8d8b713dd39 | 完整 1946 行：模板/角色/岗位/临时授权/申请审批/账号/职级薪资/统计/私有辅助。034 四处已为纯中文（L515、1061、1491、1694）。发现 CX-B5-02（L189）。其余英文键文案（L1189、1329、1686、1716）经翻译层转中文（§4）。 |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 37 / 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | phoneHash @JsonIgnore（M006-025）；legacy 构造器只用于测试夹具。无意见。 |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 496 / 22963 | 1260154b681dd7027b17f3d371b2e0304ca4df034204e885b4dd743bd271e4ad | 完整 496 行：DENY 优先、一次性消费、平台管理员日常桥、客人自助白名单、主体范围（M006-031）、审批级别、scope 守卫。发现 CX-B5-01（L89-94、L141-168）。L174 英文经翻译层转中文。 |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 48 / 2000 | 8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39 | 仅 local/dev profile；缺账号拒绝；状态白名单。无意见。 |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 / 882 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 输出记录，注释与实现 A（grants 预扣 denies）一致。无意见。 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 69 / 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | prod profile；bcrypt(10)；invited/disabled → 凭证 disabled；affected!=1 报错不伪成功。无意见。 |

（14 行 SHA256 均为 2026-09-06 15:09Z 在 160 上 `sha256sum` 实算；合计 3262 行。）

## 2. 52 条冻结判据逐项

“本 14 文件无该业务执行器”= 按模块载体核对后无可归责实现，不是删除判据，也不是该业务全仓通过。

| 判据 | 结论 | 证据 |
|---|---|---|
| GEN-001 必要性 | PASS（登记 1 项非缺陷） | 全部公共入口均有控制器调用；`listGrantRequests(Integer)` 单参重载在 src/main 无调用方（仅测试 grantListHasADefaultTimeWindow 使用），无行为影响，登记不入队。 |
| GEN-002 可执行正确性 | PASS | 60 源 javac exit 0；99 项回归 0 FAIL（§4）。 |
| GEN-003 测试有效性 | PASS | 99 项均为真实行为断言并在本轮实跑；新增 invalidDecisionMessageIsChinese 在 before 树 FAIL、after 树 PASS（B4-r4 raw）。M006JdbcRegressionTests 12 项按名核对覆盖锁/指纹/分页/软删除（本轮未跑 JDBC 类）。 |
| GEN-004 信任边界 | PASS | 入口 requireText/requireRequest/白名单（账号类型、状态、审批结论、申请类型）；@Valid DTO；窗口/阈值只读政策（QUERY_POLICY_OVERRIDE_FORBIDDEN）。 |
| GEN-005 授权最小权限 | PASS | 服务端逐入口 requirePermission(s)；DENY 优先；platform_admin 桥仅日常权限；高危须审批（GRANT_REQUIRES_APPROVAL）；授权人不得授出自身不持有权限（GRANT_EXCEEDS_OWN_PERMISSIONS）；受益人/申请人不得自审。 |
| GEN-006 数据完整性 | PASS | applyGrant/reviewGrant @Transactional + lockGrantAuthorizationFacts + 指纹 GRANT_SNAPSHOT_CHANGED；岗位生命周期 @Transactional；凭证 upsert affected==1。 |
| GEN-007 并发幂等 | PASS | 所有写入口经 IdempotencyService.execute；主体范围 u:len:id:org（M006-031）；一次性权限原子消费 markSingleUsePermissionConsumed。 |
| GEN-008 接口一致性 | PASS | DTO 字段名/可空性与 Controller 组装一致；错误码枚举与前端下钻码稳定；本轮未改接口。 |
| GEN-009 错误处理 | **FAIL → CX-B5-02** | AccessService.java:189 `actor.organizationId().equals(...)` 对 organizationId 为 null 的非平台管理员抛 NullPointerException（500 INTERNAL_ERROR），非 ApiException；探针见 §4。fail-closed 但不可定位。 |
| GEN-010 安全敏感信息 | PASS | 无明文密码落库（bcrypt）；phoneHash 不出 JSON；错误响应不回显输入原值（034）。 |
| GEN-011 可观测审计 | PASS | 每个写入口 recordSuccess/recordFailure 成对，含幂等键与错误码；查看类也留痕。 |
| GEN-012 资源性能边界 | **FAIL → CX-B5-01** | AuthorizationService.isDailyPermission 每次调用 `accessRepository.listPermissions()`（Jdbc 为一条 SELECT）；computeEffectivePermissions 对平台管理员按字典逐项调用 → 字典 200 条时一次 effectivePermissions() 触发 201 次 listPermissions（探针 §4）。放大倍数 = 权限字典规模（当前约 219）。 |
| GEN-013 构建依赖 | PASS（本模块无构建文件） | 本 14 文件不含构建/配置；编译在既有 classpath 下通过。 |
| GEN-014 迁移 | PASS（本模块无迁移文件） | V386 属第 1 轮修复产物，不在本 14 文件；本轮未改 SQL。 |
| GEN-015 界面文案语言 | PASS | 034 四处 + StaffService 两处已中文；剩余 6 处英文字段键文案（AccessController:215、AccessService:1189/1329/1686/1716、AuthorizationService:174）经 GlobalExceptionHandler→FieldLabels.translate 全部输出中文（§4 探针 6/6 LATIN=false）。 |
| GEN-016 文档可验证 | PASS（本模块无文档文件） | 类注释中的 REQ/DEC 引用与代码一致；未见把计划写成事实。 |
| GEN-017 业务来源分类 | PASS | 本轮两条新意见均为技术性（无业务发明）；无添加项。 |
| GEN-018 第一性原理证据 | PASS | 本报告所有结论落到源码行号 + 可复现命令输出（§4）。 |
| BEQR-157 员工状态 | 本 14 文件无该业务执行器 | 账号状态 active/invited/disabled 是登录账号状态，非人事五态；人事状态在 access/staff（M007）。无冲突。 |
| BEQR-182/183/184/185/186/187 考勤、打卡、请假、薪资单、提成 | 本 14 文件无该业务执行器 | StaffLevelAssignment 仅承载职级与基本工资（CNY、非负）；不实现考勤/请假/薪资单/提成。无冲突、无发明。 |
| BSM-001 全局原则·策略可配置 | PASS | 授权政策（窗口、阈值、风险等级→审批级别）由后台 AccessGrantPolicy 配置且带上下限，查询不可覆盖。 |
| BSM-003 数据可改可删有引导权限分岗 | PASS | 岗位模板软删除+存档+审批（M006-026）；权限分岗由角色/模板/职级实现。 |
| BSM-009 基础管理 | PASS | 员工与权限为准入基础：员工须注册审批（STAFF_REGISTRATION_REQUIRED）。 |
| BSM-028 主要功能（多主体、合同变更…） | 细化 PASS | 本模块承接“员工/权限”一栏：组织与商户隔离（CROSS_ORG/CROSS_MERCHANT）；其余条目在别的模块。 |
| BSM-029 和谁有关 | PASS | 用户权限知道组织、商户、职级、审批状态（AccessUser/StaffLevelAssignment/GrantRequest）。 |
| BSM-073 商户主要功能 | 细化 PASS | 商户管理员只能操作本商户账号（listUsersByMerchant、requireSameOrganization）。 |
| BSM-084 员工手机端·员工自助权限 | 细化 PASS | 审批通过自动赋 role_staff_self（M006-011，测试 registrationCreatesAccountOnlyAfterCompanyApproval）；实现体在 M007。 |
| BSM-085 日常工作·注册两路径 | PASS | createAccessUser 拒绝 staff 类手工建号，仅 outsourced_vendor；注册路径在 M007。 |
| BSM-086/087/088 排班薪资提成 | 本 14 文件无该业务执行器 | 同 BEQR-18x。 |
| BSM-114 各类入口只看该看的 | PASS | 平台/组织/商户三层 scope 守卫；外包供应商 enforceOutsourcedAssigneeScope。 |
| BSM-115 相互关系·基础管理连所有业务 | PASS | 授权闸（requirePermission）为各域前置；不允许时先卡住（PERMISSION_DENIED）再由审批放行（applyGrant/reviewGrant）。 |
| BSM-120 已确认规则 | PASS | 与本模块相关的“权限按最小颗粒度、角色与员工级自定义”由权限字典+角色+模板+临时授权实现。 |
| PGM-001 平台治理层定位·全中文 | PASS | 归属/资格/责任三层在本模块可追溯（审计事件带 actor、target、幂等键）；提示全中文（GEN-015）。 |
| PGM-008 员工和手机 | 本 14 文件无该业务执行器 | 设备绑定在 terminal/staff 模块。 |
| PGM-022/024 空间点位 | 本 14 文件无该业务执行器 | 数据范围 dataScope 仅为标签透传。 |
| PGM-026 商户主体主要功能 | 细化 PASS | 商户账号必须指定商户（requiresMerchantScope）。 |
| PGM-032/033 员工注册两路径、换机解绑 | PASS（注册路径）/ 无执行器（手机绑定） | 同 BSM-085。 |
| PGM-034 工作手机企微 | 本 14 文件无该业务执行器 | — |
| PGM-035 提成发放治理 | 本 14 文件无该业务执行器 | — |
| PGM-036 设备管理 | 本 14 文件无该业务执行器 | — |
| PGM-048 商户侧授权范围内全开放 | PASS | 商户管理员：本商户账号/角色分配可做，跨商户拒绝；平台全局配置需平台管理员（requirePlatformAdmin）。 |
| PGM-059 个人信息保护 | PASS | 手工账号不存手机号摘要，AccessUser 不输出 phoneHash（M006-025）。 |
| PGM-066 已拍板规则（含四类高危授权先审批、全中文） | PASS | 高危 requiresApproval → GRANT_REQUIRES_APPROVAL/主管复核；报错全中文（§4）。 |

## 3. 三线复核

- 第一性原理：本模块为什么必须存在——BSM-115“如果合同或基础定义不允许，后续流程必须先被卡住”与 PGM-066“高危权限授出前须先审批”只能由服务端权限闸与授权申请审批链承载；删除任一入口，上述条款无法做到。声称行为是否被执行验证：34 项回归 + 3 个探针（§4）。未验证前提：本轮未跑 JDBC 集成类与完整 mvn test；未验证生产 profile 下 bcrypt 成本与数据库锁的真实行为。
- 顺从式：§2 逐条；旧 001—034 抽样复核：001（DENY 优先，测试 administratorDenyWins…）、011（STAFF_REGISTRATION_REQUIRED）、025（phoneHash）、026（生命周期）、031（主体范围）、032/034（中文报错）均在当前源码与测试中成立，未见回归。
- 对抗式（grilling 自问，不等操作员）：Q1 平台管理员能否绕过高危审批？——isExplicitOrDailyGrant 剔除 role_platform_admin 高危，requireGrantApprovalIfNeeded 对四个授出口一致 fail-closed；不能。Q2 复制他组织模板？——L189 判定存在，但无组织主体触发 NPE（CX-B5-02）。Q3 有效权限查询代价？——平台管理员每次 N+1（CX-B5-01）。Q4 幂等键跨组织串用？——u:len:id:org（031），测试 companyRequestsHaveIndependentResults… PASS。Q5 英文回显？——6 处经翻译层，探针 6/6 中文。Q6 岗位模板物理删除？——DELETE 入口恒拒绝，软删除须存档+最高权限复核。
- ponytail-review（只列可删，不修）：`AccessService.java:L789-804 delete: listGrantRequests(Integer) 单参重载，无生产调用方；测试改用双参重载即可，净 -16 行。` `net: -16 lines possible.` 该项无行为缺陷，仅登记（GEN-001）。

## 4. 实际验证（本席实跑，160 上 .scratch 临时目录，自动清理；2026-09-06）

1. 回归（本轮实跑，60 源 javac exit 0）：`M006SecurityRegressionTests 34 PASS / 0 FAIL`、`AccessServiceTests 35 PASS / 0 FAIL`、`AuthorizationServiceTests 21 PASS / 0 FAIL`、`IdempotencyServiceTests 9 PASS / 0 FAIL`；`RESULT 99 PASS / 0 FAIL exit 0`（每个 @Test 新实例；与历次 98 项集合相同，因本批新增 1 项为 99）。
2. 翻译层探针（FieldLabels.translate，真实类）：
   `valid_until is required → 有效期至未填写`、`expires_at is required → 到期时间未填写`、`request_type is invalid → 申请类型格式不正确`、`status is invalid → 状态格式不正确`、`authentication is required → 登录凭证未填写`；`RESULT PASS all translated to Chinese`。
3. 探针 G（GEN-012）：mock AccessRepository 计数 listPermissions；字典 200 条、platform_admin 主体：`effectivePermissions() listPermissions_calls=201 grants=133`；`requirePermission(一个日常码) listPermissions_calls=1`；普通 staff `listPermissions_calls=0`。
4. 探针 H（GEN-009）：`copyPermissionTemplate(orphan staff, org-owned template) -> RuntimeException NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "AccessActor.organizationId()" is null`；对照 `copyPermissionTemplate(same-org staff) -> NO_EXCEPTION`。
5. 残留扫描：`grep` src/main 无“（approve）/（reject）/实际为：+currency/USB 最高权限审批”用户可见文案（仅 PurchaseRequestRecord.java:14 Javadoc）。
未跑：M006JdbcRegressionTests（需 complex_test 库，本轮未导出 DB 变量）；完整 mvn test；数据库迁移；前端；真实支付/短信厂商。

## 5. 本方意见（候选，正式 ID 在 B2 分配）

### CX-B5-01 — 平台管理员有效权限计算对权限字典逐项重复查询（N+1）
- 位置：backend/src/main/java/com/complex/platform/access/AuthorizationService.java:89-94（isDailyPermission 每次 listPermissions()）、141-168（computeEffectivePermissions wildcard 分支对每个 p 调用 isDailyPermission）、96-101（isExplicitOrDailyGrant 对 role_platform_admin 效果逐条调用）。JdbcAccessRepository.listPermissions:49-56 为无缓存 SELECT。
- 证据：类型 4，探针 G（201 次/1 次 effectivePermissions；字典规模线性放大）。
- 分类：技术性（无业务发明，不与 HTML 冲突）；冻结判据 GEN-012；工程缺陷 是（查询次数随权限字典规模线性放大，每次后台加载 /effective-permissions 触发 ~220 次 SELECT）；候选修复队列 是。
- 最小修法（供 B2 定稿，本轮不修）：在一次 computeEffectivePermissions / requirePermissions 内只取一次 listPermissions() 并构造日常权限码集合复用；不改判定语义，不加缓存框架。旧测试 administratorKeepsDailyPermissionsWithoutAutomaticHighRole 等应保持通过；补一条“单次调用 listPermissions 不超过 2 次”的可失败断言。
- 溯源：该路径由第 1 轮 B3（M006-001 修复）引入，b4-r1 两席 diff 审均未评估查询次数；非 034 回归。

### CX-B5-02 — 无组织归属的非平台管理员复制模板时抛 NullPointerException
- 位置：backend/src/main/java/com/complex/platform/access/AccessService.java:186-191（`!actor.organizationId().equals(source.ownerScopeId())` 在 organizationId 为 null 时 NPE，早于 L193 的 requireProjectScope）。
- 证据：类型 4，探针 H（NPE，对照同组织 staff 正常）。M006-003 已把“无组织主体”作为真实存在的情形（测试 missingOrganizationCannotListAllUsersOrResolveGlobalProject）。
- 分类：细化（上位 BSM-114/PGM-048 跨组织隔离）；冻结判据 GEN-009（错误须可定位并安全失败——现为 500 INTERNAL_ERROR 而非 403 PROJECT_SCOPE_REQUIRED/CROSS_ORG_FORBIDDEN）；工程缺陷 是；候选修复队列 是。无越权（fail-closed）。
- 最小修法（供 B2 定稿，本轮不修）：L189 改为先 `authorizationService.requireProjectScope(actor)` 再比较（与 L146 updatePermissionTemplate 同范式）；补一条 orphan 主体得 ApiException 的可失败断言。

### 登记（无工程缺陷）
- GEN-001：AccessService.listGrantRequests(Integer) 单参重载无生产调用方（见 §3 ponytail-review）。只登记，不入队。
- 模块外观察（不作为 M006 finding）：backend/src/main/java/com/complex/platform/access/staff/StaffService.java:363 仍有“（approved）或驳回（rejected）”英文回显（Grok B4-r4 亦指出）；该文件属 M008，留待 M008 或操作员另行指令；本轮不改。

## 6. 技能自检与四值

技能：superpowers:verification-before-completion（PASS 前实跑）、superpowers:receiving-code-review（先核实旧 ID）、grilling（自问自答）、ponytail:ponytail-review（列可删）已实际调用；diagnosing-bugs / systematic-debugging 按“读错误→复现→追数据流”借用于两条探针；codebase-design、writing-for-agents、tdd 本节点未触发修复，未调用；`batch-grill-me`、`ubiquitous-language`、`git-guardrails-claude-code` 缺口。
B5 后四值校验（§5 闸 1，④ 取本轮 modules/M006-b5-r4-grok-raw.md 最后一条）在两席 raw 落齐后由席甲实算并打进回执；本报告落盘时 ①②③ = 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de。

结论：本席 FAIL。按【回退统一规则】：CX-B5-01/02 均为无 ID 新缺陷 → 整批先回 B2 定稿分配 ID（候选 M006-035、M006-036）；B3 已执行 4 次，已超控制提示词 3 次上限，第 5 次修复须操作员另行指令。

落盘 UTC：2026-09-06T15:34:33.826012+00:00


## V0.02 — 接收 Grok B5-r4 报告（非新独立票，原 V0.01 20933 字节前缀保留）

2026-09-06T15:36:19Z 取得适配器回执 REPORT_PATH=docs/audit-2026-09/modules/M006-b5-r4-grok-raw.md、EXIT_CODE=0（Grok 进程 15:21:48Z–15:34:22Z）。Grok raw 27075 字节 / SHA256 2e9767db6979830e1385e71938d5ff06f6994ccc4a490aab28ba315472efa8f8，第一行锁哈希与本席实算一致；本席原票（FAIL，2 条）先于读取封存。
Grok：PASS，无新 GR，旧 34 ID 不回退，109 PASS/0 FAIL/2 SKIP（含 JDBC 12），翻译/HTTP 探针与本席 §4 一致。Grok 未评估有效权限计算的查询次数，也未构造无组织主体复制模板反例；本席两条意见不因对方 PASS 撤回。合取：B5-r4 FAIL。
B5 后四值：① ② ③ ④ = 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0。
接收 Grok ponytail 两项为登记（slug() 未使用、wildcard 恒 false），已并入 findings V0.68 登记段。
