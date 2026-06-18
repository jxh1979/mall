# 代码实现 goal 模式：对抗式审计与修复报告 2026-06-18

> 性质：实测审计 + 直接修复。所有"通过"均为本轮在 `/home/james/complex-management` 当前工作树实际执行命令的输出。
> 方法：先建立需求→代码实现矩阵基线，再用 4 个并行子代理对全部业务模块做 controller↔service↔repository↔Flyway↔test 对抗式交叉核查，逐条**读代码验证后**再分级修复，TDD（先红测试→实现→绿）。

## 1. 基线实测（修复前）

| 闸口 | 结果 |
|---|---|
| 后端全量测试 | 673 通过，0 失败/错误（surefire 汇总实测）|
| OpenAPI/客户端契约 `verify:p0-contract` | PASS（207 operations，97 路由模板，74 方法级调用）|
| Semgrep `p/secrets`+`p/owasp-top-ten` | 0 findings / 587 文件 |
| `git diff --check` | 通过 |
| 需求矩阵 `requirements-code-implementation-matrix-v0.1.csv` | 96 条：88 已完整实现、8 部分实现（REQ-17/21/23b/25/32/40/41/44，均卡真实供应商/生产证据，非代码缺陷）|

## 2. 本轮发现并修复（已 TDD 验证）

逐条均**读取真实代码路径核实**后才修复；范式沿用代码库既有 `checkProjectOwnership`/`checkMerchantOwnership` 与 UUID 主键。

| 编号 | 级别 | 模块 | 缺陷（已核实） | 修复 | 新增测试 |
|---|---|---|---|---|---|
| F-01 | P1（运行期阻断）| bar | `BarOrderService.productionActionType` 对 `*->exception` 返回 `"mark_exception"`，写入 `bar_production_ticket_events.action_type`；V029 CHECK 仅允许 `'exception'` → 真实 Postgres 下生产工单异常流转必抛约束违例。单元测试用内存仓库故漏检 | 返回值改为 `"exception"` | `BarServiceTests.productionTicketExceptionTransitionUsesDbAllowedActionType` |
| F-02 | P0（跨租户覆盖）| floorplan | `createFloorPlanOnce` 主键 `"fp_"+slug(idempotencyKey)` 无商户成分，`saveFloorPlan` 为 `ON CONFLICT(id) DO UPDATE`；两商户用同一幂等键即 slug 冲突 → B 覆盖 A 的平面图。幂等层按商户隔离不去重，确为可达覆盖 | 主键改为 `"fp_"+UUID`（与 compliance 一致），靠幂等层去重 | `FloorPlanServiceTests.createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` |
| F-03 | P1（跨租户写 IDOR）| merchantbilling | `confirmBill/submitDispute/recordDunning` 经 `findBill(id)` 仅按 id 查，无 scope 校验（`listBills` 有）。商户可操作他商户账单 | `findBill` 内强制 `currentMerchantScope` ownership | confirm/dispute/dunning 三条跨商户拒绝测试 |
| F-04 | P1（跨租户写 IDOR）| amusement | `AmusementService` 的 session/projectRule/verification/manualOverride 内部写法按 id 载入 project 后直接改，无 ownership 校验（兄弟 `AmusementBookingService`/`InspectionService` 每个写都有 `checkProjectOwnership`）；`getOpenEligibility` 同样泄漏他商户安全态 | 新增 `checkProjectOwnership` 并在 4 个写路径 + `getOpenEligibility` 调用 | session/manualOverride/getOpenEligibility 三条跨商户拒绝测试 |
| F-05 | P1（跨租户写 IDOR）| amusement | `acknowledgeWaiver` 仅 `ensureNoticeExists`（无 scope）；可对他商户安全告知书登记弃权确认。`AmusementSafetyNoticeRecord` 已带 `merchantId` | `ensureNoticeExists` 内强制 notice ownership | `AmusementSafetyServiceTests.acknowledgeWaiverRejectsCrossMerchantNotice` |
| F-06 | P1（跨租户写）| compliance | `createContractKeyDateOnce/createExpiryItemOnce` 的 `merchant_id` 直接取请求体（读侧按商户隔离），违反 V044/V051"merchant_id 取自令牌而非参数"不变量 | 新增 `resolveMerchantScopedId`：有 scope 则强制用 scope、体内不一致则拒绝；无 scope（平台管理员）才用体内值 | `ComplianceServiceTests.createContractKeyDateRejectsBodyMerchantIdDifferentFromScope` |
| F-07 | P1（状态机缺守卫）| ticketing | `TicketRefundService.approveRefund` 不校验 `current.status()`，已 `approved` 的退款可被反复再批准、重写行 | 仅允许 `"requested"` 状态批准，否则 409 `TICKET_REFUND_NOT_APPROVABLE` | `TicketingServiceTests.approveTicketRefundRejectsAlreadyApprovedRefund` |
| F-08 | P1（原子性）| merchantservice | `createWorkOrder/assignWorkOrder/addAction/closeWorkOrder` 多写（工单+动作）无 `@Transactional`（兄弟 merchantbilling 全有）；中途失败留半截 | 4 个公有写方法补 `@Transactional` | 行为不变，靠既有测试回归 |
| F-09 | P2（职责分离）| governance | `SupervisorReviewService.approve` 仅校验 reviewerLevel，不比对发起人；表无发起人列，自审无法防（同一主管可批准自己触发的复核）| 新增迁移 `V053` 加 `initiated_by` 列；`evaluate` 记录发起人 = `currentActor().id()`；`approve` 在级别校验后拒绝 `reviewer == initiated_by`（`SUPERVISOR_SELF_REVIEW_FORBIDDEN`）。新增 `asSupervisor(userId,level)` 测试夹具 | `approveRejectsSelfReviewByInitiator` + 改造 3 个 happy-path 用异主管审批 |
| F-10 | P2（读隔离）| finance | 对账批次/差异列表 `listReconciliationBatches/Differences` 无任何 scope 过滤；持 view 权限的商户用户可读全平台对账数据 | 对账表无 merchant 维度（platform/channel 级），按语义正确做**平台专属门禁**：商户 scope 存在即拒绝（`RECONCILIATION_PLATFORM_ONLY`），平台管理员可读 | `reconciliationReadsAreForbiddenForMerchantScopedCallers` |
| F-11 | P2（状态机）| merchantservice | `addAction` 接受任意 `ACTION_TARGET_STATUSES`，无 from→to 校验：`created` 可直跳 `resolved`、`resolved` 可回退 `assigned` | 新增 `ALLOWED_ACTION_TRANSITIONS` 白名单 + `requireValidActionTransition`（同状态=评论放行，非法跳转/回退抛 `MERCHANT_WORK_ORDER_INVALID_TRANSITION`）| 非法跳转/回退拒绝 + 合法前进 3 条测试 |
| F-12 | P2（证据完整性）| evidence | `completeAttachment` 的 checksum 取自客户端逐字入库，无任何校验，可存入垃圾值冒充防篡改摘要 | 本切片无对象存储无法按字节回算哈希；改为强制 checksum 为规范化 `sha256:<64 hex>`（大小写归一），垃圾/弱格式拒绝（`EVIDENCE_CHECKSUM_INVALID`）；配合既有状态机（uploaded 不可重complete）使摘要结构合法、不可变、可审计 | malformed 拒绝 + 大写归一 2 条测试 |
| F-13 | P2（认证强度）| auth(dev) | `AuthService`（local/dev/test profile）密码用无盐 SHA-256、令牌永不过期 | 密码改用 `BCryptPasswordEncoder(10)`（加盐+工作因子，登录走 `matches`）；会话增 `accessExpiresAt`（默认 30m，可配 `complex.auth.access-token-ttl`），`authenticateToken` 拒绝过期令牌；删除无盐 `hash()`/`constantTimeEquals` 死代码 | bcrypt 加盐+非明文/SHA256 + 令牌 TTL 过期拒绝 2 条测试 |
| F-14 | P2（数据兜底）| customer | `customer_points_accounts.balance` 无 `CHECK(>=0)` DB 兜底（兑换路径已 FOR UPDATE 安全，但 DB 层缺约束）| 新增迁移 `V054` 加 `chk_customer_points_accounts_balance_non_negative CHECK(balance>=0)`，与 V025 账本 `balance_after` 对齐 | JDBC 集成测试直插负值断言 `DataAccessException` |
| F-15 | P2（并发）| auth(prod) | `registerFailedLogin` 读 login 时载入的 `failedAttemptCount` 再写 `+1`，并发失败登录读同一基线 → 计数少算、锁定可能不触发 | 改为单条原子 `UPDATE ... SET failed_attempt_count = failed_attempt_count + 1, locked_until/status = CASE WHEN +1 >= :max ...`（行锁内基于旧值计算，零少算）| 既有"3 次错密码锁定"测试回归（行为不变，仅去除竞态）|
| F-16 | P2（会话撤销）| auth(prod) | 凭证被锁定/禁用后，`authenticateToken`/`refresh` 只查 `user_status`，已发令牌在过期前仍有效 | 会话查询联表 `auth_credentials`，`authenticateToken` 与 `refresh` 均校验 `credentialUsable`（status 非 active/locked 或当前处于锁定窗口即拒）| 锁定后已发 access token 立即被拒（401/403）JDBC 测试 |
| F-17 | P2（信息泄漏）| system | `/system/health`（permitAll 匿名可访问）向匿名返回 `active_profiles` | 移除 `activeProfiles` 字段（含 `Environment` 依赖），仅返回 ok/status/service/checkedAt | 反射断言响应记录不含 `activeProfiles` 字段 |
| F-18 | P3（PII）| analytics | `AiTextRedactor` 兜底窄：仅 CN 手机/18 位身份证/通用卡号/邮箱；国际电话、IBAN、密钥/令牌、SSN 原样送 LLM | 扩展正则：JWT、`sk-/AKIA/xox*` 等密钥、IBAN、E.164 国际电话、SSN，及 32+ 长令牌兜底；保留原有标签与行为 | 国际电话/IBAN/AWS key/长 hex/SSN 脱敏新测试（原测试回归）|
| F-19 | P3（脱敏）| ticketing | 终端 sync payload 仅对 `cred_` 前缀值脱敏，敏感字段下非该前缀值原样落库（两处重复实现）| 抽出共享 `TerminalSyncPayloadSanitizer`：按**字段名**（credentialPayload/entitlementCode/token…）无条件哈希 + 保留 `cred_` 前缀兜底；消除两处重复 | 字段名脱敏/cred_ 兜底/已哈希与空值不变 3 条测试 |
| F-20 | P3（碰撞）| customer | `slug(key)` 截断至 40 字符，两个仅在 40 字符后不同的幂等键 → 同 id → ON CONFLICT 覆盖 | 超长时改为 `前31字符 + "_" + sha256(完整key)[:8]`，短键行为不变（不破坏现有 id 断言）| 两个超长仅尾部不同的键产生不同 consent id 测试 |

**误报（已核实驳回）**：子代理报"integration 的 list* 端点未鉴权 P0"——私有 helper `IntegrationController.listLedger` 自身调用 `requirePermission(VIEW_PERMISSION)`，所有 list* 均经其鉴权，**非缺陷**。

## 3. 修复后实测（全绿）

| 闸口 | 结果 |
|---|---|
| 后端全量测试 | **700 通过，0 失败/错误**（+28 条新增 TDD 测试，无回归）|
| `verify:p0-contract` | PASS |
| Semgrep（含未跟踪新代码目录扫描）| 0 findings / 587 文件 |
| `git diff --check` | 通过 |
| `verify:delivery-gates`（聚合闸口）| **exit 0**，无 FAIL/ERROR/No-Go |

## 4. 已记录、本轮未修（需产品/Schema/外部决策，按级别移交）

| 级别 | 模块 | 项 | 不修原因 |
|---|---|---|---|
| ~~P2~~ ✅已修 | governance | supervisor 自审 → 见 F-09 | — |
| ~~P2~~ ✅已修 | finance | 对账读隔离 → 见 F-10 | — |
| ~~P2~~ ✅已修 | merchantservice | `addAction` 状态机 → 见 F-11 | — |
| ~~P2~~ ✅已修(部分) | evidence | checksum 格式校验 → 见 F-12；**字节级真实回算仍需对象存储集成** | 真实 byte-hash 校验待对象存储接入 |
| ~~P2~~ ✅已修 | auth(dev) | dev 认证强度 → 见 F-13 | — |
| ~~P2~~ ✅已修 | customer | 积分余额 DB 兜底 → 见 F-14 | — |
| ~~P2~~ ✅已修 | auth(prod) | 失败登录原子计数 + 锁定撤销会话 → 见 F-15/F-16 | — |
| ~~P2~~ ✅已修 | system | `/system/health` active profiles 泄漏 → 见 F-17 | — |
| ~~P3~~ ✅已修 | analytics | PII 正则兜底窄 → 见 F-18 | — |
| ~~P3~~ ✅已修 | terminal | payload 按值前缀脱敏 → 见 F-19 | — |
| ~~P3~~ ✅已修 | customer | slug id 截断碰撞 → 见 F-20 | — |

> 全部 P0/P1/P2/P3 编码侧发现已闭环。剩余仅 evidence checksum 的**字节级真实回算**（F-12 已做格式校验，真实回算待对象存储集成）与 8 项"部分实现"需求 + 生产前置证据——均属外部依赖/IT-运维-Owner 线下职责，非编码缺陷。

## 5. 结论

- 核心业务可构建、可测试、可联调；本轮关闭 1 项运行期阻断（F-01）与 1 项跨租户数据覆盖（F-02），并系统性补齐 5 处跨租户写隔离 + 1 处退款状态守卫 + 1 处事务原子性。
- 不宣称生产上线就绪：8 项"部分实现"需求与 12 项生产前置证据仍属 IT/运维/安全/Owner 线下职责，非代码缺陷。
- 第 4 节项按级别移交 Owner；多数需 Schema 变更或外部集成，非本轮"能直接修复"范围。
