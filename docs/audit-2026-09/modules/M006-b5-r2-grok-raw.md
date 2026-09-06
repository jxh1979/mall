CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B5-r2 Grok 全量只读审计报告

- 版本：V0.01
- 席位：Grok（审计席乙）
- 控制：V0.17
- 模块：M006 / 322（`10-backend-main-access-01`）
- B3 已执行次数：2（本席不改 ledger）
- ledger 状态（调用提示给定，本席未改）：`IN_PROGRESS`
- 本席动作：只审不修、不启动 M007、不发布、不派第三席
- 审计结论：**FAIL**

独立实算（本席本轮命令，非派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

sidecar `plan/criteria-lock.sha256` 与 ledger 顶部同值。本席不把四值合取写成模块 PASS。

---

## 0. 技能预检（本席自核）

已核读并按合同收窄使用：

| 技能 | 本席环境 | 用法 |
|---|---|---|
| `using-superpowers` | 存在 | 开工技能路由 |
| `grilling` / `grill-me` | 存在 | 对抗式追问结构；事实自读；**不派子代理、不等操作员** |
| `codebase-design` | 存在 | 接口/seam/深度用语 |
| `diagnosing-bugs` | 存在 | 只读复现环；不改生产代码 |
| `systematic-debugging` | 存在 | 追数据流；不作无 ID 改动 |
| `tdd` / `test-driven-development` | 存在 | B5 只读，不落修复测试 |
| `verification-before-completion` | 存在 | PASS 前必须有本轮命令输出 |
| `receiving-code-review` | 存在 | 只核实、不修复 |
| `writing-for-agents` | 存在 | 本 raw 给下游席读 |
| `ponytail-review` | 存在 | 只列可删点，不改码 |
| `i-have-adhd` | 存在 | 编号、无寒暄 |
| `git-guardrails-claude-code` | 存在 | Claude Code hook 安装器；本席非该宿主，**未安装、未改持久配置** |

缺口（不得冒充已调用）：

- `batch-grill-me`：**不存在**
- `ubiquitous-language`：**不存在**
- Arc `audit` 的 reviewer 分派：会引入第三席，与双席合同冲突，**未派**

划掉且未用：`dispatching-parallel-agents`、`subagent-driven-development`、`research`、`ponytail-audit`、`ponytail-debt`、`using-git-worktrees`。

互盲：未读任何 M006 B1/B2/B4/B5 raw、未读其他模块报告。`findings.md` 只抽 30 行当前队列表（文件 SHA256=`32665375af0b696a921c7fd3086e6a467b7f9a95144fbc2570ed2a15bf4db1d5`，与提示 V0.60 一致）。当前 30 项「修复队列归属」均为「否」——有旧 ID 也不当成本席可修队列。

---

## 1. 十四文件完整范围（当前工作区）

`plan/modules.md` 的 M006 边界 14 路径。`git ls-files -z -- <14 paths> | xargs -0 cat | wc -l` = **3264**（与提示合计一致；modules.md 所记 2988 是切分时旧计量，本席按当前文件审）。

| 路径 | 行 | SHA256 | 与提示 |
|---|---:|---|---|
| `backend/src/main/java/com/complex/platform/access/AccessActor.java` | 11 | `5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessController.java` | 299 | `42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java` | 12 | `c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java` | 10 | `08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java` | 35 | `b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java` | 9 | `7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java` | 8 | `80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | 259 | `c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessService.java` | 1946 | `79449d002be76914aa1e74681e112b77b25b6673ed7ad650117e9d0992dc82d0` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AccessUser.java` | 37 | `717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1` | 一致 |
| `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | 498 | `6c76917544710d8f49e0f098377e09922f4d462266f0eae3533ecb612eb5cb3d` | 一致 |
| `backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java` | 48 | `8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39` | 一致 |
| `backend/src/main/java/com/complex/platform/access/EffectivePermissions.java` | 23 | `68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e` | 一致 |
| `backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java` | 69 | `06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e` | 一致 |

14 文件均已逐行读完。未把 `JdbcAccessRepository`、DTO、Staff、V386 算进本模块文件集合；那些只作授权/用户已定稿流程的窄读。

映射行（`criteria-lock.md:6386`）与提示 52 ID 一致。未把 BSM-004/BSM-007/BSM-035/PGM-009/PGM-041 写成新增 M006 映射。需上位原文时按已映射 GEN 归因。

---

## 2. 当前 30 项唯一队列表（只抽表，不当已通过）

来源：`docs/audit-2026-09/findings.md` 节「M006 当前队列表」。列：ID / 来源 / 候选分类 / 工程缺陷 / **修复队列归属** / 问题。

全部 30 行「修复队列归属」= **否**。本席不沿用它们当可修队列；新缺陷用 GR 临时号。

| ID | 分类 | 工程缺陷 | 队列 | 问题（原文摘要） |
|---|---|---|---|---|
| M006-001 | 冲突 | 是 | 否 | 平台管理员无条件通配提前返回，绕过显式 DENY 和高危权限单独授予 |
| M006-002 | 冲突 | 是 | 否 | 同组织被当成同商户 |
| M006-003 | 细化 | 是 | 否 | 无组织的非管理员被当成全局主体 |
| M006-004 | 细化 | 是 | 否 | 名称/幂等键 slug 生成全局主键 |
| M006-005 | 细化 | 是 | 否 | 修改已应用模板可直接给用户新增自己不持有的高危权限 |
| M006-006 | 细化 | 是 | 否 | 审批用申请快照，执行却重读变动后模板 |
| M006-007 | 冲突 | 是 | 否 | 直接受益人可以审批发给自己的高危授权 |
| M006-008 | 细化 | 是 | 否 | 两不同幂等键并发审批同一申请都生效 |
| M006-009 | 冲突 | 是 | 否 | 岗位切换只追加不使旧岗位派生权限失效 |
| M006-010 | 细化 | 是 | 否 | 主管账号/管理职级无法被审批级别解析器识别 |
| M006-011 | 冲突 | 是 | 否 | 手工员工建号直接 active 及发密码 |
| M006-012 | 细化 | 是 | 否 | 按用户 ID 查有效权限只认商户范围 |
| M006-013 | 细化 | 是 | 否 | 临时授权接受空权限集合和倒置时间窗 |
| M006-014 | 细化 | 是 | 否 | 一次检查两个权限时第一次消耗整份单次模板 |
| M006-015 | 冲突 | 是 | 否 | 岗位建议和个人异常总结返回固定英文 |
| M006-016 | 冲突 | 是 | 否 | 业务阈值/窗口及授权风险对应审批层级写死 |
| M006-017 | 细化 | 是 | 否 | 单次临时授权首次消费留痕外键指错 |
| M006-018 | 细化 | 是 | 否 | 平台管理员改组织模板把原 owner 改成 null |
| M006-019 | 技术性 | 否 | 否 | 记录容器防御复制和兼容构造器 |
| M006-020 | 技术性 | 否 | 否 | 凭证接口、Spring 注入和按 profile 选择实现 |
| M006-021 | 技术性 | 否 | 否 | 密码 BCrypt、UTF-8/SHA-256 和 JDBC 时间参数 |
| M006-022 | 技术性 | 否 | 否 | HTTP 映射、响应包装和仓储接口默认失败 |
| M006-023 | 细化 | 否 | 否 | 主体、权限集合和临时授权统计数据记录 |
| M006-024 | 细化 | 是 | 否 | 账号状态未校验且非 disabled 一律发 active 凭证 |
| M006-025 | 冲突 | 是 | 否 | 手工建号把手机号存为无密钥 SHA-256 |
| M006-026 | 冲突 | 是 | 否 | 岗位角色模板直接物理删除 |
| M006-027 | 细化 | 是 | 否 | 保留线上充值和订单余额支付；现场按手机号及短信验证码 |
| M006-028 | 技术性 | 是 | 否 | 未知或 UserDetails 认证主体被填成 staff/active |
| M006-029 | 技术性 | 是 | 否 | 手工建号空密码成功返回但无登录凭证 |
| M006-030 | 细化 | 是 | 否 | 列表声称每页 50 却返回全量；授权申请缺默认时间过滤 |

---

## 3. 第一性原理三问

### 3.1 为什么必须存在？

删掉本 14 文件，下列已映射 HTML 做不到：

- BSM-009 / BSM-028：管理员工、岗位、职级、权限、注册审批入口
- BSM-114 / PGM-048：不同入口只看自己范围
- BSM-120 / PGM-066：权限按单一功能最小颗粒度
- PGM-001：先管资格（员工是否有权限）
- PGM-033：注册两条路径在本模块的落点是「禁止手工给普通员工直接建号」

答得上 → 有业务上位。记录类（AccessActor 等）是该接口的数据形状，属细化，不是添加。

### 3.2 声称的行为有没有被实际执行验证？

| 声称 | 本席验证 | 缺口 |
|---|---|---|
| DENY 优先于平台管理员日常桥 | 读 `AuthorizationService.java:68-86,141-168`；scratch 隔离 javac 反例 `AUTHZ_PASS=true` | 未跑 JUnit（禁止 Maven test） |
| 高危须单独授予，不因 platform_admin 自动得 | 同上；`isDailyPermission` 排除 `requiresApproval` 与 high/critical | 未跑 Spring 集成 |
| 员工须走注册审批 | 读 `AccessService.java:944-947` `STAFF_REGISTRATION_REQUIRED` | 未跑 createAccessUser 单测 |
| 岗位模板禁止物理删除 | `AccessService.java:472-474` 抛 `ARCHIVE_APPROVAL_REQUIRED`；Jdbc 同拒绝 | complex_test 无 V386 列，未跑真实 UPDATE |
| 授权政策上下限 | 实编 `AccessGrantPolicy`：`ZERO_BOUNDS_CODE=GRANT_POLICY_OUT_OF_BOUNDS`，中文消息 | — |
| 储值：无付款码、短信、渠道未配失败关闭 | 窄读 StoredValue/Bar/Wallet/Finance 网关/OTP | 未开真实厂商、未写共享库 |
| **展示/报错全中文** | 源码探针 6/6 命中英文字面量（见 GR-01） | 该声称**未成立** |

### 3.3 哪个前提未验证就当真？

1. `complex_test` 已应用 V386：只读 `information_schema` 三列均为 `f`。本席未 APPLY。Jdbc 新列路径在共享库未验证。
2. 现有 `M006SecurityRegressionTests` 能在当前树绿：未执行，**不声称通过**。
3. `Strings.requireText` 把字段名拼成 `"{field} is required"`：已读 `Strings.java:27-31`；字段名由本模块传入。
4. 前端 `hasPermissions` 在 `wildcard=false` 时改看 `grants`：已读 `PermissionGate.tsx`；后端 `computeEffectivePermissions` 把日常权扩进 grants 且返回 `wildcard=false`。此前提已核，不作缺陷。

---

## 4. 顺从式：52 判据逐项覆盖

每条含本模块事实，不写「已覆盖」空话。BSM-035/PGM-041 **不是**本模块映射。

| ID | 适用？ | 三线要点 |
|---|---|---|
| GEN-001 | 适用 | 14 文件承载权限/账号/授权申请。私有 `slug()`（AccessService:1898）无调用，第一次正常请求撞不上，不单列缺陷。 |
| GEN-002 | 适用 | 主路径与错误路径可指到代码。英文报错与「全中文」声称不一致 → GR-01。 |
| GEN-003 | 适用 | 仓库有 `M006SecurityRegressionTests.java` 等。本席**未跑** Maven test。隔离反例与英文探针已跑。测试通过本身不替代 GR-01。 |
| GEN-004 | 适用 | `currentActor` 拒未认证/未知主体（174、190）。权限码 `permissionExists`。 |
| GEN-005 | 适用 | Controller 写操作均 `requirePermission`。`permissionGrant` 先 DENY，再日常桥，再显式 GRANT。高危不走日常桥。客人桥仅 `customer.stored_value.topup/consume`。 |
| GEN-006 | 适用 | `requirePermissions` 先核再按来源消费，失败回滚。指纹 `authorization_fingerprint` 只写一次（Jdbc:735-742）。 |
| GEN-007 | 适用 | 写路径 `requireIdempotencyKey` + `IdempotencyService.execute`。`lockGrantAuthorizationFacts` SHARE 锁定义表。单次授权 UPDATE `consumed=FALSE`。 |
| GEN-008 | 适用 | 授权申请列表走 `pageGrantRequests` 每页 50。其它 list 用 `page(items)` 一页全量（诚实 total=size，不再谎称 50）。账号状态 `active/invited/disabled` 不是 BEQR-157 五态。 |
| GEN-009 | 适用 | 失败抛 ApiException。prod 凭证把非 active 映成 disabled。未知主体不再填 staff/active。 |
| GEN-010 | 适用 | `AccessUser.phoneHash` `@JsonIgnore`。手工建号不写手机号。Jdbc 凭证 BCrypt(10)+SecureRandom。 |
| GEN-011 | 适用 | 写失败均 `auditEventService.recordSuccess/Failure`。 |
| GEN-012 | 适用 | 授权申请有窗+分页。用户/角色/模板/权限列表无上限；种子数据第一次打开不会爆，不作单独缺陷。 |
| GEN-013 | 本模块文件无构建脚本 | 14 文件不含 pom/CI。依赖验证用已有仓库依赖 + 临时 classpath。 |
| GEN-014 | 适用（迁移在模块外，代码依赖它） | V386 文件存在。本席不在共享库 APPLY。`complex_test` 无 `authorization_fingerprint`/`archived_at`/`assigned_role_id`。TEMP 表验证了「快照只写一次」与「换岗只撤岗位应用」。 |
| GEN-015 | 适用 **未满足** | 见 GR-01。grantSummary 中文（846、855）成立；API 报错仍有英文。 |
| GEN-016 | 适用 | EffectivePermissions 注释仍写 wildcard=true，实现恒回 false 但 grants 已展开。注释不是用户展示。 |
| GEN-017 | 适用 | GR-01=冲突。其余映射条目无业务发明伪装。 |
| GEN-018 | 适用 | 本报告依据 HTML、命令输出、源码行。 |
| BEQR-157 | 本 14 文件不适用员工五态 | 原文：待入职、在职、试岗、停岗、离职。`AccessUser.status` 是账号启用态。五态在 Staff 档案（窄读 StaffService），不在本 14 文件实现。 |
| BEQR-182 | 不适用 | 考勤日结果。14 文件无打卡/日结果。 |
| BEQR-183 | 不适用 | 打卡方式。无 NFC/扫码领取。 |
| BEQR-184 | 不适用 | 请假类型。 |
| BEQR-185 | 不适用 | 请假单状态。 |
| BEQR-186 | 不适用 | 薪资单构成。本模块只存职级与基本工资分（assignStaffLevel），不算薪资单。 |
| BEQR-187 | 不适用 | 提成发放。无实时提成入账。 |
| BSM-001 | 适用（全局底座） | 「谁能操作」由 AuthorizationService 服务端闸。 |
| BSM-003 | 适用（h2 导语） | 正文只声明通用规则。存档删除细则在未映射 BSM-004；岗位模板软删按 GEN-005/GEN-006 用该上位原文，**不扩锁**。实现：`deletePositionRoleTemplate` 拒绝物理删；lifecycle archive→request_delete→master approve。 |
| BSM-009 | 适用 | 基础管理含员工与权限。本模块是权限/账号 API。 |
| BSM-028 | 适用 | 「管理员工、岗位、班次、职级、权限、工作设备和员工注册审批」中，本文件做权限/岗位模板/职级分配/授权申请；班次设备在后续模块。手工建号仅 `outsourced_vendor`。 |
| BSM-029 | 适用 | 「用户权限要知道员工注册信息、设备绑定…」：本模块强制注册后才有员工账号；设备绑定不在 14 文件。 |
| BSM-073 | 适用 | 商户在授权范围内操作本商户员工。`listAccessUsers` 有 merchant 则 `listUsersByMerchant`。 |
| BSM-084 | 部分/其余不适用 | 员工自助权限由注册审批调用 `role_staff_self`（StaffService 窄读）。排班考勤薪资 UI 不在 14 文件。 |
| BSM-085 | 注册路径适用 | 两条注册路径：本模块挡住第三条手工员工建号。商户代提/自助提交在 StaffService。 |
| BSM-086 | 不适用 | 排班考勤请假。 |
| BSM-087 | 不适用 | 我的薪资展示。 |
| BSM-088 | 不适用 | 提成现金现抽等，14 文件无发放。 |
| BSM-114 | 适用 | org/merchant/project scope；平台管理员桥看全量。外包 `enforceOutsourcedAssigneeScope`。 |
| BSM-115 | 适用 | 「如果合同或基础定义不允许，后续流程必须先被卡住」：`requireGrantApprovalIfNeeded`、`GRANT_REQUIRES_APPROVAL`。 |
| BSM-120 | 适用 | 「员工权限按单一功能最小颗粒度」：permissionCode 级。职级 M1-M10 在 staff_levels 列表，本模块只引用。开关 opt-in 不在这 14 文件。 |
| PGM-001 | 适用 **报错全中文未满足** | 资格/责任链：审批人≠申请人≠受益人。界面全中文：GR-01。 |
| PGM-008 | 入口适用 | 卡片「管理员工注册、私人手机…」。注册落点见 PGM-033；手机绑定不在 14 文件。 |
| PGM-022 | 不适用空间主数据 | 无楼层点位 CRUD。组织/商户只作隔离键。 |
| PGM-024 | 班次责任区不适用 | 「设备和员工班次要知道责任区域」不在 14 文件。 |
| PGM-026 | 商户权限隔离适用 | merchant 范围；平台合规/平台分析 fail-closed 拒商户账号。 |
| PGM-032 | 导语适用 | 「审核是否通过、上岗时能做什么」：账号+有效权限。 |
| PGM-033 | 注册两条路径适用 | 与 BSM-085 同：禁止第三入口。 |
| PGM-034 | 不适用 | 企微会话。无 wecom 代码。 |
| PGM-035 | 不适用 | 提成月结发放。 |
| PGM-036 | 不适用 | 设备台账。 |
| PGM-048 | 适用 | 商户不看平台全局：`currentMerchantScope` / `listUsersByMerchant` / `requirePlatformScope`。 |
| PGM-059 | 适用 | 手机号不进手工建号 JSON（DTO `@JsonIgnore`，本模块 create 传 `phoneHash=null`）。界面脱敏开关不在这 14 文件。 |
| PGM-066 | 适用 **全中文未满足** | 最小颗粒度权限点成立。报错英文与「界面全中文」冲突 → GR-01。 |

027 口径（按已映射 GEN-002/GEN-005/GEN-009 与提示给定流程核，**不新增 BSM-035 映射**）：

- 在线充值：`StoredValueService.topup` 只建支付意图；`settleTopup` 要求网关启用且 `confirmOnlinePaymentIntent` 校验真实回执，否则 `PAYMENT_GATEWAY_UNAVAILABLE`。
- POS：`confirmOfflineTopupAndSettle` 柜台确认后入账。
- App 订单：`payTicketOrder` 限 `accountType=customer` 且 `customerId=actor.id()`，指定卡，短信 `verifyStoredValueCustomer`。
- 现场：`BarBillingService` `stored_value` 渠道要 phone+smsCode，`consumeInternal`；前端 `BarOrdersPage` 无储值付款码。
- `WalletPage` 文案：订单储值支付；现场报手机号验证短信。
- 门票核销码仍在 `MyOrdersPage`「出示二维码」，不是储值付款码。
- HTML 1480 第三方先自研接口；枚举 210「预留接口+开关默认关+不伪造」。`FailClosedCustomerOtpVerifier`（prod）verify 直接失败；`requestCode` 默认同样未配置失败。`ReservedPaymentGateway` 返回 NOT_CONFIGURED，不伪造成功。

---

## 5. 对抗式（grilling 结构，自答）

1. 平台管理员能否靠通配越过 DENY？  
   `permissionGrant` 先扫 DENY 再桥。隔离反例：`ADMIN_DENY_DAILY=DENY`，有效集不含被 DENY 的日常码。`MASTER_DENY_HIGH=DENY`。

2. `role_platform_admin` 播种高危是否算「单独授予」？  
   `isExplicitOrDailyGrant`：来源 ROLE 且 sourceId=`role_platform_admin` 时，非日常码不算 GRANT。`ADMIN_HIGH_WITHOUT_GRANT=DENY`。

3. 受益人/申请人自审？  
   `reviewGrant` 717-722：`BENEFICIARY_SELF_REVIEW_FORBIDDEN`、`SUPERVISOR_SELF_REVIEW_FORBIDDEN`。

4. 审批后模板被改？  
   批准前比对 `authorizationFingerprint`；只写一次指纹。TEMP：第二次 UPDATE 影响 0 行，快照仍 `abc`。

5. 换岗后旧岗位角色是否仍在 `user_roles` 生效？  
   有效权第二段从 `user_position_assignments.assigned_role_id` 读；`replacePositionGrants` 撤旧 `position_application_ids` 并换 role。TEMP：`pta_old=revoked`，`pta_keep=active`，`assigned_role_id=role_new`。

6. DELETE 岗位模板？  
   Controller 仍有 DELETE，Service 抛必须存档审批。Jdbc `deletePositionRoleTemplate` 抛禁止物理删除。

7. 手工建员工号？  
   非 `outsourced_vendor` → `STAFF_REGISTRATION_REQUIRED`。空密码 `requireText(password)`。未知主体抛「无法识别的认证主体」。

8. 客人桥能否退款/冻结？  
   白名单只有 topup/consume。退款仍员工权限。

9. 未认证调用？  
   英文「authentication is required」——对抗式找到的**现存缺陷**（GR-01），不是已修项。

10. 储值出示付款码？  
    客人订单/钱包/酒吧结账均手机号+短信。门票码仍是核销码。

---

## 6. 本轮真实命令与输出（节录）

### 6.1 判据哈希

```
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

### 6.2 英文字面量探针（javac --release 21，输出在 `.scratch`）

```
AccessController.java CONTAINS [valid_until is required]=true
AuthorizationService.java CONTAINS [authentication is required]=true
AccessService.java CONTAINS [valid_until is required]=true
AccessService.java CONTAINS [expires_at is required]=true
AccessService.java CONTAINS [request_type is invalid]=true
AccessService.java CONTAINS [status is invalid]=true
ENGLISH_LITERAL_HITS=6
english_exit=0
```

### 6.3 授权决策隔离反例

```
ADMIN_DENY_DAILY=DENY
ADMIN_HIGH_WITHOUT_GRANT=DENY
ADMIN_EFFECTIVE_HAS_OPS=false
MASTER_DENY_HIGH=DENY
AUTHZ_PASS=true
authz_exit=0
```

说明：该反例复现 `AuthorizationService.permissionGrant` / `computeEffectivePermissions` 的 DENY-first 与日常桥规则，在 scratch 编译运行，**不是** Spring 容器里的原类。原类已通读。

### 6.4 实编 AccessGrantPolicy（classpath 来自 `dependency:build-classpath` 写到 scratch/`cp.txt`）

```
REAL_POLICY_WINDOW=30
ZERO_BOUNDS_CODE=GRANT_POLICY_OUT_OF_BOUNDS MSG=配置值须在有效的正整数上下限内
MISSING_RISK_CODE=INVALID_GRANT_POLICY
REAL_POLICY_PASS=true
```

Maven：`org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath`，`-Dmdep.outputFile=$SCRATCH/cp.txt`，`-Dproject.build.directory=$SCRATCH/mvn-target`。事后按 size+mtime 逐文件比 `backend/target`：**size 变化 0、mtime 变化 0**（初比把 JSON list 与 tuple 整包相比造成假「全变」，元素比后否定）。未跑 compiler/test/package。

### 6.5 数据库（complex_test，只读信息架构 + 连接级 TEMP）

```
current_database = complex_test
grant_fp_col = f
prt_archived_col = f
pos_assigned_role_col = f
```

TEMP：指纹二次更新 0 行，保留 `abc`。换岗应用：`pta_old` revoked、`pta_keep` active、role_new。均 `ROLLBACK`。未 APPLY V386，未改共享表。

### 6.6 验证缺口（不声称通过）

- 未跑任何 Maven `test`/`compile`/`verify`。
- 未在 JVM 中加载完整 `AccessService`（依赖面超出本轮隔离 javac）。
- 共享库无 V386 列，Jdbc 新 SQL 未对真实表执行。
- 未配置真实支付/SMS，按口径只验证失败关闭，不要求打开开关。

---

## 7. Findings（本席临时号；整模块审完一次性列出）

### GR-01

- **标题**：本模块对外 API 报错仍含英文整句，与已映射「界面全中文」冲突
- **文件:行**
  - `AccessController.java:215` `"valid_until is required"`
  - `AuthorizationService.java:174` `"authentication is required"`
  - `AccessService.java:1189` `"valid_until is required"`
  - `AccessService.java:1329` `"expires_at is required"`
  - `AccessService.java:1686` `"request_type is invalid"`
  - `AccessService.java:1716` `"status is invalid"`
  - 同文件多处 `requireText(..., "user_id"|"username"|"organization_id"|"password"|"action"|"reason"|"id"|"role_id"|"staff_level_id"|"position_code"|"target_user_id"|"template_id"|"operator_note")`，经 `Strings.requireText` 拼出 `"{field} is required"`
- **证据**：§6.2 探针 6/6；`Strings.java:30` `field + " is required"`（本模块传入英文字段名）。第一次漏填 username/未登录/缺 valid_until 就会把英文返回给调用方。
- **2.1 分类**：**冲突**
- **2.3 依据类型**：1（HTML 逐字）+ 4（命令输出）
- **已冻结判据**：PGM-001（「报错…必须全部为中文，不得出现任何英文」）；PGM-066 同口径；GEN-015
- **HTML 原文（PGM-001）**：平台所有终端展示给用户的一切文字，包括…报错…必须全部为中文，不得出现任何英文。可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符。
- **工程缺陷**：成立。英文整句不是四类标识符。
- **与旧 ID**：不是 M006-015（那是岗位建议/异常总结；当前 grantSummary 已是中文）。队列全否，即令相近也不当可修。须经后续 B2 定稿。
- **本席不修**。

无其它新 GR。ponytail-review：`AccessService.slug` 无引用、`EffectivePermissions.wildcard()` 对调用方恒 false、构造器 `summaryPositionThreshold` 未存字段——第一次正常请求不撞，不进缺陷队列。

添加项：无（GR-01 能指到 PGM-001，不走 2.2）。

---

## 8. 审计结论

**FAIL**

理由（同时成立即不得 PASS）：

1. GR-01 为映射内冲突，有源码行与可复现探针。
2. 无未答 questions；失败原因不是「待问」而是缺陷未消除。
3. 不存在有条件通过。

回退路由提示（本席不执行）：GR-01 无当前修复队列归属，整批应回 B2 定稿后再修。禁止本席改代码。

---

## 9. 文件覆盖核对

| 文件 | 已通读 | 三线 |
|---|---|---|
| AccessActor.java | 是 | 数据形状；组织/商户/状态隔离键 |
| AccessController.java | 是 | 权限码映射；英文 valid_until；DELETE 仍在但服务拒绝 |
| AccessCredentialProvisioner.java | 是 | 凭证 seam |
| AccessGrantPositionSummary.java | 是 | 中文 recommendation 由 Service 填 |
| AccessGrantRequestRecord.java | 是 | 申请快照字段 |
| AccessGrantSummary.java | 是 | 汇总容器 |
| AccessGrantUserSummary.java | 是 | 中文 summary 由 Service 填 |
| AccessRepository.java | 是 | 默认失败；分页/指纹/换岗接口 |
| AccessService.java | 是 | 注册闸、审批、岗位生命周期、政策、英文报错 |
| AccessUser.java | 是 | phoneHash 忽略；legacy 构造仍在（019 技术性） |
| AuthorizationService.java | 是 | DENY/日常桥/客人桥/职级；英文 authentication |
| DevAccessCredentialProvisioner.java | 是 | 校验账号状态后交给 AuthService |
| EffectivePermissions.java | 是 | grants 预扣 DENY；返回 wildcard=false |
| JdbcAccessCredentialProvisioner.java | 是 | 非 active→disabled 凭证；BCrypt |

模块生命周期仍是调用方 ledger 的 `IN_PROGRESS`。本席不写收口文件、不改 findings、不改 lock。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B5-r2 Grok 席整模块三线全量复审（V0.02）

- 席位：Grok（审计席乙）
- 步骤：B5-r2（修复后整模块重审，不是 diff 复审）
- 模块：M006 `10-backend-main-access-01`
- 控制提示：V0.17
- 工作区：`160:/home/james/complex-management/` 分支 `main` 当前工作区
- ledger 口径（本席读取，不改）：M006 `IN_PROGRESS`，B3 已执行 2 次
- 本席结论：FAIL
- 未读：`M006-b5-r2-self-raw.md`；未用既有 `M006-b5-r2-grok-raw.md` 正文替代独立复审；旧报告 26892 字节前缀保留，本文件为追加
- 未修代码、未改方案、未改三份 HTML、未改 `criteria-lock.md`、未开 M001—M005/M007、未进 B2/B3
- 操作员已确认口径已执行：027 现场报本人手机号并短信验证、不用二维码；线上支付/短信未配置或 RESERVED/OFF 时失败关闭，不开启开关、不虚构厂商接通

## 0. §6 技能自核（实际列目录，不假造）

| 技能 | 存在性 | 本席本轮 |
|---|---|---|
| `using-superpowers` | 存在 `superpowers/6.3.0/skills/using-superpowers` | 已读并开工前自检 |
| `verification-before-completion` | 存在 | 已按「先跑命令再下结论」执行 javac / FieldLabels / TEMP 表 |
| `systematic-debugging` | 存在 | 只借读错误→复现→追数据流；未改生产代码、未加诊断桩 |
| `grilling` / `grill-me` | 存在 | 只借追问结构自问自答；不等操作员、不派子代理 |
| `codebase-design` | 存在 | 用于凭证适配器 seam / 授权接口深度 |
| `diagnosing-bugs` | 存在 | 用于英文报错与编译依赖的红环构造 |
| `tdd` / `test-driven-development` | 存在 | B5 不写测试；测缝须用户确认已按铁律 5 作废 |
| `ponytail-review` | 存在 | 只列可删项，不改代码 |
| `ponytail` | 存在 | B5 不修复，未当修复强度使用 |
| `writing-for-agents` | 存在 | B5 不写模块收口文件 |
| `resolving-merge-conflicts` | 存在 | 当前无合并冲突，未调用本体步骤 |
| `git-guardrails-claude-code` | 存在（可选） | 未安装 hook；本席无 git 写操作 |
| `receiving-code-review` / `requesting-code-review` | 存在 | B5 禁止修复；本席即外审，不派第三席 |
| `writing-plans` / `brainstorming` | 存在 | 阶段 A 已过，本轮不用 |
| `batch-grill-me` | **缺口：目录中不存在** | 不冒充已调用 |
| `ubiquitous-language` | **缺口：目录中不存在** | 不冒充已调用 |
| `i-have-adhd` | 存在 | 报告结构按编号、无寒暄 |
| 划掉项（executing-plans / 子代理 / worktree / research / ponytail-audit 等） | 存在但不适用 | 未调用 |

Maven 全量编译未跑（会写共享 `backend/target`）。本席用 Java 21 `javac --release 21` 在 `/tmp/m006-b5-r2-javac` 对 14 个现源独立编译。数据库只连 `complex_test` 做会话级 TEMP 表，未迁移。

## 1. 文件覆盖（modules.md M006 全部 14 个，当前工作区行数）

| # | 路径 | 行数 | 本席覆盖 |
|---|---|---|---|
| 1 | `backend/src/main/java/com/complex/platform/access/AccessActor.java` | 11 | 全文 |
| 2 | `backend/src/main/java/com/complex/platform/access/AccessController.java` | 299 | 全文 |
| 3 | `backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java` | 12 | 全文 |
| 4 | `backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java` | 10 | 全文 |
| 5 | `backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java` | 35 | 全文 |
| 6 | `backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java` | 9 | 全文 |
| 7 | `backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java` | 8 | 全文 |
| 8 | `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | 259 | 全文 |
| 9 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | 1946 | 全文 |
| 10 | `backend/src/main/java/com/complex/platform/access/AccessUser.java` | 37 | 全文 |
| 11 | `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` | 498 | 全文 |
| 12 | `backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java` | 48 | 全文 |
| 13 | `backend/src/main/java/com/complex/platform/access/EffectivePermissions.java` | 23 | 全文 |
| 14 | `backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java` | 69 | 全文 |
| 合计 | | 3264 | 与 `git ls-files -z -- <14 paths> \| xargs -0 cat \| wc -l` 及工作区 `wc -l` 同为 3264 |

直接调用链（不算开别的模块）：`FieldLabels`/`GlobalExceptionHandler`、`StoredValueService.payTicketOrder`、`CustomerTokenAuthenticationFilter`、`AccessUserRequest`、`AccessGrantPolicy.java`（工作区未跟踪）、测试 `AuthorizationServiceTests`/`M006SecurityRegressionTests`/`AccessServiceTests`/`AccessControllerTests`。未把测试文件计入本模块文件集。

## 2. 映射判据覆盖（52 项，逐项核对）

映射原文：`criteria-lock.md` `M006 → [GEN-001 … GEN-018, BEQR-157, BEQR-182…187, BSM-001, BSM-003, BSM-009, BSM-028, BSM-029, BSM-073, BSM-084…088, BSM-114, BSM-115, BSM-120, PGM-001, PGM-008, PGM-022, PGM-024, PGM-026, PGM-032…036, PGM-048, PGM-059, PGM-066]`。

| ID | 本模块结论 | 证据要点 |
|---|---|---|
| GEN-001 | 见 GR-02；`AccessService.slug` 私有未用（ponytail，不单独入队） | 当前 14 文件均有授权/账号/凭证用途 |
| GEN-002 | FAIL（GR-01 错误路径文案；GR-02 跟踪树缺类型） | javac 14 文件对现 target 退出码 0；去掉 `AccessGrantPolicy` 后 `AccessService` javac 退出码 1 |
| GEN-003 | 测试在模块外；本席未跑 Maven。测试源有可失败断言，但 `INVALID_ACCOUNT_TYPE` 未断言中文 | `AccessServiceTests.createAccessUserWithPlatformAdminAccountTypeThrowsValidationError` 只断言 code |
| GEN-004 | PASS | 写入口校验账号/密码/状态；未知 principal 拒绝 |
| GEN-005 | PASS | `requirePermission` 默认拒绝；DENY 优先；高危不因 `role_platform_admin` 自动获得 |
| GEN-006 | PASS | 凭证 provision `@Transactional`；授权审批先锁事实再指纹 |
| GEN-007 | PASS | 写路径 `idempotencyKey`；单次授权先全核再消费；指纹防审批窗口内容被换 |
| GEN-008 | 备注不入队 | `EffectivePermissions` 注释写 platform_admin 的 `wildcard=true`，实现恒 `false` 且测试锁定 false；前端靠 grants 扩张日常权 |
| GEN-009 | PASS（除 GR-01 展示） | 凭证缺失 fail-closed；邀请态凭证落 `disabled` |
| GEN-010 | PASS | 密码 BCrypt；`phoneHash` `@JsonIgnore`；手工开户不落手机摘要 |
| GEN-011 | PASS | 关键写路径 `auditEventService.recordSuccess/Failure` |
| GEN-012 | 列表接口整页返回，首跑种子数据不炸；不按理论放大入队 | `page()` 固定 page=1 |
| GEN-013 | FAIL（GR-02） | `AccessService`/`AccessController` 依赖未跟踪、未进 scope 的 `AccessGrantPolicy` |
| GEN-014 | N/A | 本 14 文件无迁移 |
| GEN-015 | FAIL（GR-01） | 报错可选值英文枚举泄漏 |
| GEN-016 | N/A | 本模块无独立文档文件 |
| GEN-017 | PASS | 见第 6 节分类；027 储值白名单按 HTML「账户预存款 + 短信」归细化 |
| GEN-018 | PASS（本席结论均落到四类证据） | 见各 finding |
| BEQR-157 | PASS（无冲突） | 本模块账号态为启用/待激活/禁用，不是「员工状态」五值；14 文件无「待入职/试岗/停岗」 |
| BEQR-182 | PASS（无实现、无发明） | 14 文件无「考勤/打卡/请假」字样 |
| BEQR-183 | 同上 | 同上 |
| BEQR-184 | 同上 | 同上 |
| BEQR-185 | 同上 | 同上 |
| BEQR-186 | PASS（本切片只做职级+基本工资分，完整薪资单不在 14 文件） | `assignStaffLevel` 非负分、CNY；对齐 BSM-120「职级只给薪资大范围，具体薪资仍需单独填写」 |
| BEQR-187 | PASS（无提现抽成实现） | 无现金现抽路径 |
| BSM-001 | PASS | 授权政策可配且有上下限（调用链 `AccessGrantPolicy.BoundedInt`）；多综合体按 organization/project scope |
| BSM-003 | PASS | 岗位模板存档→申请删除→占用检查→最高权限审批；硬 DELETE 被拒 |
| BSM-009 | PASS | 本切片实现员工账号、岗位、职级、权限 |
| BSM-028 | PASS（班次/设备/注册审批在后续 access 切片） | 「管理员工、岗位、职级、权限」在本切片；员工注册被本接口拒绝并要求登记审批 |
| BSM-029 | PASS | 授权知道组织/商户/职级/审批状态 |
| BSM-073 | PASS | 商户 scope 列表与跨商户写拒绝 |
| BSM-084 | PASS | 手工开户不能发员工账号；员工须登记审批（测试 `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval`） |
| BSM-085 | PASS（注册两条路径不在本 14 文件；本切片挡住手工发员工号） | `STAFF_REGISTRATION_REQUIRED` |
| BSM-086 | PASS（无考勤实现） | 无冲突规则 |
| BSM-087 | PASS（见 BEQR-186） | 基本工资分 |
| BSM-088 | PASS | 无现金抽成 |
| BSM-114 | PASS | 权限码按入口动作；客人自助仅白名单两码 |
| BSM-115 | PASS | 基础管理连权限；商户隔离 |
| BSM-120 | PASS（相关已拍板） | 权限最小颗粒；职级+单独薪资；端开关不在本切片 |
| PGM-001 | FAIL（GR-01 报错未全中文） | HTML 原文「报错…必须全部为中文」；`FieldLabels.translate` 对含汉字的英文枚举原样透传 |
| PGM-008 | PASS | 本切片管账号与权限，人员注册在 staff 切片 |
| PGM-022 | PASS | 授权带 project/organization |
| PGM-024 | PASS | 班次责任区域不在本切片，无冲突 |
| PGM-026 | PASS | 商户账号必须带 merchant |
| PGM-032 | PASS | 账号与组织/商户对应；私人手机绑定不在本切片 |
| PGM-033 | PASS | 手工接口仅 `outsourced_vendor` |
| PGM-034 | PASS | 无企微实现、无冲突 |
| PGM-035 | PASS | 无现金抽成 |
| PGM-036 | PASS | 设备不在本切片 |
| PGM-048 | PASS | `listUsersByMerchant` / `CROSS_MERCHANT_FORBIDDEN` / 平台合规 scope |
| PGM-059 | PASS | 列表 JSON 不输出 `phoneHash`；手工开户 `phoneHash=null` |
| PGM-066 | FAIL（GR-01） | 与 PGM-001 同一「界面全中文」已拍板 |

## 3. 第一性原理线

1. **为什么必须存在？** 删掉本切片，BSM-009/028「管理员工、权限」、BSM-114「不同人只看自己该看的」、PGM-048 商户隔离、PGM-066 单一功能权限都做不到。14 个文件是授权执行面与账号写入面。
2. **声称行为是否实际执行？**
   - Java 21 `javac --release 21` 对 14 个现源、classpath=既有 `backend/target/classes`+m2：退出码 **0**，写出 21 个 class 到 `/tmp/m006-b5-r2-javac`。
   - 从 classpath 去掉 `AccessGrantPolicy*` 后再编 `AccessService.java`：退出码 **1**，`cannot find symbol: class AccessGrantPolicy`（`AccessService.java:1549/1560/777`）。
   - `FieldLabels.translate` 实跑：`valid_until is required`→`有效期至未填写`；含汉字的账号类型句保留 `staff` 等英文。
   - `complex_test` 会话 TEMP 表：`CREATE TEMP TABLE m006_b5_r2_probe` + INSERT + `count=1`，`current_database()=complex_test`，psql 退出码 0；未迁移、未碰业务表。
3. **未验证就当真的前提：** Maven/Surefire 未跑（禁写共享 target）；生产短信/支付厂商未接通（按操作员口径 fail-closed，不作为本模块缺陷）。既有 30 项队列「否」与 B4-r2 双 PASS **不是** 本席通过依据。

## 4. 顺从式线

假定实现意图正确后逐项核对 52 条。授权主路径与 HTML 一致的部分：DENY 优先、日常权给平台管理员、高危须显式授予、申请人/受益人不能自审、USB 最高权限可跳过配置删除审批、员工不能走手工开户、商户不能跨商户、邀请态不能拿到可用密码、缺凭证适配器不能报成功。客人储值两码白名单是 HTML「账户预存款经手机号加短信验证支付」的下位实现；`StoredValueService.payTicketOrder` 仍调用 `verifyStoredValueCustomer(customerId, phone, smsCode)`，本席不开启开关。失败点见 GR-01/GR-02。

## 5. 对抗式线（grilling 自问，不等操作员）

1. 平台管理员是否拿到全部高危？`isDailyPermission` 仅 low/medium 且 `!requiresApproval`；`role_platform_admin` 的高危被 `isExplicitOrDailyGrant` 剔除。测试 `administratorKeepsDailyPermissionsWithoutAutomaticHighRole`。不能越权。
2. 客人储值是否绕过短信？授权白名单只过权限闸；支付仍要短信。不虚构厂商。
3. 商户是否读他商户用户？`merchantId` 非空走 `listUsersByMerchant`。跨商户写 `CROSS_MERCHANT_FORBIDDEN`。
4. 自己批自己？`BENEFICIARY_SELF_REVIEW_FORBIDDEN` / `SUPERVISOR_SELF_REVIEW_FORBIDDEN`。
5. 审批窗口换权限？指纹 `GRANT_SNAPSHOT_CHANGED`。
6. 英文报错能否出网？`GlobalExceptionHandler` 对 ApiException 一律 `FieldLabels.translate`。`field is required` 能中文化；**含汉字的句子原样透传**，英文枚举出网。见 GR-01。
7. 跟踪源能否编过？当前工作区因未跟踪 `AccessGrantPolicy.java`/`*.class` 能编过；权威跟踪树没有该类型。见 GR-02。

## 6. 临时 findings（整批，不中途 B2/B3）

### GR-01 — 手工开户非法账号类型报错把英文枚举送给用户

- 文件:行号：`AccessService.java:48-49,921-923`
- 冻结 ID：PGM-001、PGM-066、GEN-015、GEN-002
- 2.1 分类：**冲突**（与 HTML「报错必须全部为中文；枚举取值不得英文」矛盾）
- 2.3 依据：类型 1 HTML 原文 + 类型 4 实跑
- 工程缺陷：是
- 首跑：`createAccessUser(accountType=platform_admin)` 是已有测试覆盖的错误路径，第一次打该路径即出网
- HTML：`docs/platform-governance-map-for-operations-v0.1.html:287`「提示、报错…必须全部为中文，不得出现任何英文（如 active/draft 英文状态）」；同文件 :965 已拍板。
- 代码：

```
"账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES
```

`ALLOWED_ACCOUNT_TYPES = Set.of("staff","merchant","merchant_admin","supervisor","sales","outsourced_vendor")`。
- 复现：`/tmp/M006FieldProbe` 调既有 `FieldLabels.translate`：

```
TRANS=账号类型「platform_admin」不允许，可选值：[sales, supervisor, outsourced_vendor, merchant_admin, merchant, staff]
TRANS_HAS_STAFF=true
```

`FieldLabels.java:529`：已含汉字则原样返回，不走英文兜底。`GlobalExceptionHandler.java:58-61` 把该字符串写入响应体。
- 对照：同文件 `requireAccountStatus` 用「启用、待激活或禁用」，说明账号态已经能中文说；账号类型可选值没有同等处理。
- 不是内部标识符例外：这是报错里给操作者看的「可选值」清单，不是单据号/设备编号/二维码/系统内部标识符。

### GR-02 — AccessService/AccessController 依赖未跟踪、未进模块清单的 AccessGrantPolicy

- 文件:行号：`AccessService.java:777,790,807,1549-1573`；`AccessController.java:119-128`
- 冻结 ID：GEN-002、GEN-013
- 2.1 分类：**技术性**（无业务发明：授权政策上下限是 BSM-001 已有「可配+上下限」的实现细节）
- 无业务发明理由：时间窗/阈值/风险审批层级是已冻结策略参数，不是新业务实体。
- 2.3 依据：类型 4
- 工程缺陷：是
- 首跑：对「只使用 git 跟踪源 + scope 内类型」做 javac，第一次编 `AccessService` 即 `cannot find symbol`
- 事实：
  - `git ls-files --error-unmatch .../AccessGrantPolicy.java` → pathspec 不匹配
  - `git status --porcelain` → `?? backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java`
  - `plan/scope.txt` / `plan/modules.md` / `plan/module-union.txt` 均无该路径
  - HEAD 的 `AccessService.java` 无 `AccessGrantPolicy` 引用；工作区相对 HEAD 改了 +437/-236 行后出现引用
  - 现工作区 javac 14 文件（classpath 含 target 里已有的 `AccessGrantPolicy.class`）退出码 0
  - 复制 target 并剔除 `AccessGrantPolicy*` 后再编：退出码 1，定位 `AccessService.java:1549`
- 这不是「忘了 git add 要两件不巧」的纯理论：本模块当前源已经把该类型写进公开方法签名；权威纳入清单里没有对应文件。

未入队（首跑不触发 / 非缺陷）：
- `AccessService.slug` 私有未调用：ponytail `delete:`，运行路径碰不到。
- `EffectivePermissions.wildcard` 恒 false：测试锁定，日常权走 grants 扩张，与「高危不自动给平台管理员」一致。
- 列表不分页：种子体量不炸。
- 027 储值白名单：细化，SMS 在调用链，不打开支付/短信开关。

## 7. ponytail-review（只列可删，不改）

- `AccessService.java:L1898-1904: delete: 私有 slug() 无调用。Nothing replaces it.`
- `EffectivePermissions.java:L13-14: shrink: 注释仍写 wildcard=true；实现与测试均为 false。删掉过时句子。`
- net: 约 -10 行。其余授权/指纹/隔离不是过度设计。

## 8. 可执行证据摘要

| 命令 | 退出码 | 关键输出 |
|---|---|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar、ledger 同值） |
| `javac --release 21` 14 个现源 → `/tmp/m006-b5-r2-javac` | 0 | 21 class |
| 同编译器、classpath 去掉 AccessGrantPolicy | 1 | `cannot find symbol: class AccessGrantPolicy` |
| `M006FieldProbe` + FieldLabels | 0 | 账号类型句保留 staff 等英文；`valid_until is required`→中文 |
| `psql` `complex_test` TEMP 表 | 0 | `current_database=complex_test`，count=1 |
| Maven | 未跑 | 避免写共享 `backend/target` |
| 模块测试 | 未跑 | 只读测试源确认可失败断言存在 |

三份 HTML 抽检：`员工状态` 仍在 enum HTML:240；「手机号加短信验证支付」仍在业务地图:585；「界面全中文」仍在治理地图:287 与 :965。本席未改 HTML。

## 9. 独立结论

**FAIL**

有待修工程缺陷 GR-01（冲突）与 GR-02（技术性缺陷）。不存在有条件通过。既有 30 项队列否、B4-r2 双 PASS 不构成本席 PASS。不进入 B2/B3。
