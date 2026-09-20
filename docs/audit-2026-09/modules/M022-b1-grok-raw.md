CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B1 席乙（Grok）修复前全模块审计报告

- 报告版本：V0.01（本路径此前不存在，本次创建）
- 席位：Grok 只读独立审计席乙 / grok-4.6 / reasoning xhigh
- 阶段：160-audit-prompt.md V0.17 阶段 B / B1（PENDING→IN_PROGRESS，本模块历史未审）
- 正本：`/home/james/complex-management` main 主工作区
- 模块：plan/modules.md V0.04 **M022**「商户档案、商户类型与商户侧门户／后台验证／第1段」
- 计划有效内容行：227；本席实算：`git ls-files -z -- <两路径> | xargs -0 cat | wc -l` → **333**
- 判据锁：criteria-lock.md V0.11；本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶部同值，系实算结果而非抄写）
- 映射 22 项：BSM-001、BSM-003、BSM-114、GEN-001…018、PGM-001（criteria-lock.md 映射区 M022 行）
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): 钉住破窗不能无盘续期、换钥作废旧票`）
- origin/main：`86b8dd2abf8bdc58726a7f4ad32b40182298dc3b`（M021 检查点 PR #462 已合并）
- merge-base(HEAD, origin/main)：`88c7b465475fb8eb26e46200ae90ad3e00500522`（本地 HEAD 与 origin/main 已分叉；本席不 fetch/commit）
- 审计对象：工作区现行两测试文件。`git diff origin/main --` 两文件为空。工作区相对 HEAD：`MerchantPortalServiceTests.java` 为未暂存修改（工作区=origin/main 的 M020 B3-r1 V0.01；HEAD blob 为 eaccb9c5 旧构造器版本）。同批工作区相对 origin/main 干净的被测实现：`MerchantPortalService.java` V0.01、`MerchantPortalController.java` V0.01。
- 转入既有登记（本席独立复核，不采信为结论）：M020 观察——JDBC 类注释「商户专区平台管理员全量合同/结算真分页」随 M020-001 过时。
- 互盲：未读 `modules/M022-b1-self-raw.md`，未派子代理/第三席，未改计划/findings/ledger，未修业务代码。
- 本席结论：**FAIL**（全文覆盖与 22 项核对已完成；存在工程缺陷成立的临时 finding，见 §7）

---

## 0. 强制技能自检与本节点应用

本席在本机实际 `ls` 技能 `SKILL.md`，再按 160 §6 收窄口径使用。未核到的不写「已核实」。

### 0.1 superpowers

| 技能 | 本机路径 | 本节点 |
|---|---|---|
| using-superpowers | 存在 `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前已读；按用户指令优先于技能本体 |
| verification-before-completion | 存在 | 实跑两测试类、读完整 surefire 输出后才写「12 例 0 失败」；绿测≠有效断言 |
| systematic-debugging | 存在 | 只借「读错误→复现→追数据流」；B1 禁止修复，不落地 instrumentation |
| brainstorming / writing-plans | 存在 | 阶段 A 节点，本 B1 不适用 |
| test-driven-development | 存在 | 仅 B3，本节点不适用 |
| requesting-code-review / receiving-code-review | 存在 | B4/收甲票，本节点不适用 |
| executing-plans / dispatching-parallel-agents / subagent-driven-development / using-git-worktrees / finishing-a-development-branch | 提示词划掉 | 不用 |

### 0.2 mattpocock

| 技能 | 本机 | 本节点 |
|---|---|---|
| grilling | 存在 `.../productivity/grilling/SKILL.md` | **按意图执行**：只借追问结构自问自答（§6）；「等用户回答」「dispatch 子代理」作废 |
| grill-me | 存在；正文为 `Run a /grilling session.` | 并入 grilling 自问自答 |
| batch-grill-me | **缺口**：本环境无该 SKILL.md | 不冒充已调用 |
| ubiquitous-language | **缺口**：本环境未找到 | 不冒充已调用 |
| codebase-design | 存在 | 用 deletion test / 接口即测试面 判断 JDBC 全量分页用例是否仍赚其 keep |
| diagnosing-bugs | 存在 | 本轮测试未红，未进入诊断环 |
| tdd / resolving-merge-conflicts | 存在 | B3 / 无冲突，不适用 |
| writing-for-agents | 存在 | 本 raw 按步骤+完成标准组织，供席甲/B2 下游读取 |
| git-guardrails-claude-code | 存在 | 可选；本席无 git 写操作，未触发 |
| research / code-review / domain-modeling 等 | 提示词划掉 | 不用 |

### 0.3 ponytail / i-have-adhd / 其它

| 技能 | 本机 | 本节点 |
|---|---|---|
| ponytail | 存在 | B3 默认强度，本节点不修代码 |
| ponytail-review | 存在 | 已用：可删除项见 §6 Q 与 GR-01 |
| ponytail-audit/debt/gain | 提示词划掉 | 不用 |
| i-have-adhd | 存在 | 输出模式；与「CLI 不回带正文」冲突时以 160 适配器为准，结论只留本 raw |
| graft | 存在 `/home/james/.npm-global/bin/graft` | 已实际调用 skeleton/ask/grep/callers；未管道 head/tail |
| Arc audit 全量 reviewer 派发 | 存在 using-arc/audit | **不用**：用户禁止子代理/第三席 |
| audit-context-building | 存在 | **缺口/不用**：本体强制派 function-analyzer 子代理并另写 `audit-context/`，撞双席与 §7 |
| long-running-background-tasks | 存在 | 已读；mvn 在本进程内跑完（exit 0），未另建 watcher 脚本/日志文件 |

graft 本回合 tokens saved 约：1893+308+37204+14265+16521+337970+10356+31029+22709 ≈ **472255**（currentMerchantScope 一次 `--in backend/src/main/java` 仍扇出 45 文件，浪费记实）。

---

## 1. 范围与全文覆盖

### 1.1 纳入文件（不抽样）

| 路径 | 行数 | SHA256（工作区） | git blob origin/main | 覆盖 |
|---|---|---|---|---|
| `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java` | 77 | `b3d3f2356ade47a71ec1bb97790ffd213b4cadc080ee8cbd5714f58bd2088976` | `29f2cfacf010cbd2c1057c62de5e0996016d48be` | 全文 L1–L77 |
| `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java` | 256 | `ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159` | `f6154809365b32c8146d21d3b6b97e507d272507` | 全文 L1–L256（V0.01） |

被测实现（只读追语义，不纳入本模块修复队列）：

- `MerchantPortalService.java` V0.01 L1–L180
- `MerchantPortalController.java` V0.01 L1–L55
- `AuthorizationService.currentMerchantScope` L425–L432（无商户号 **抛** `MERCHANT_SCOPE_REQUIRED`，不返回 empty）
- `AuthorizationService.requirePermission` L37–L38
- `IdempotencyService.execute` L57–L123（真实现；单测经 `InMemoryIdempotencyRepository` 第二适配器）
- `AuditEventService.recordSuccess` L27–L38 / `recordFailure` L59–L71
- `ContractRepository` default `listSettlementsByMerchant` L63–L67、`listAllSettlementsPaged` L70–L75
- `JdbcContractRepository.listSettlementsByMerchant` L293–L303、`listAllSettlementsPaged` L306–L319、`saveSettlement` L242–L265（含 `resolved_by`/`resolved_at` upsert）
- `PageResponse.of` L16–L22（商户列表内存切片）
- `MerchantSettlementDisputeRequest` 三字段 record
- `MerchantContractView.of` L25–L38
- V162 `dispute_reason VARCHAR(500)`
- V200 `merchant.portal.settlement.view` / `dispute` 权限码

生产调用方（graft grep `--in backend/src/main`）：

- `listSettlementsByMerchant`：`MerchantPortalService.listMySettlements` L95 **有**生产调用
- `listContractsByMerchant`：`MerchantPortalService.listMyContracts` L64 **有**生产调用
- `listAllSettlementsPaged`：仅接口 default + JDBC 覆写定义，**零生产服务调用方**
- `listContractsPaged`：`ContractService.listContracts` L132 有调用（合同平台端点，非商户门户）

### 1.2 用例清单（12）

MerchantPortalServiceTests（10）：

1. `merchantSeesOnlyOwnContractsWithSpaceIds` L83–L99
2. `callerWithoutMerchantScopeIsRejectedAndAudited` L102–L122
3. `merchantSeesOnlyOwnSettlements` L124–L135
4. `merchantCanDisputeOwnOpenSettlementWithoutChangingStatus` L137–L152
5. `disputeKeepsResolutionTrailAndAppendsToExistingReason` L155–L171
6. `disputeReasonBeyondColumnWidthIsRejected` L174–L189
7. `disputeWithSameIdempotencyKeyIsProcessedOnce` L192–L204
8. `disputeOnForeignSettlementIsForbiddenAndAudited` L207–L221
9. `settledOrAlreadyDisputedSettlementCannotBeDisputed` L224–L242
10. `disputeReasonIsRequired` L244–L255

MerchantPortalJdbcIntegrationTests（2）：

11. `platformAllSettlementsPagedRealTotalAndSecondPageReachable` L46–L63
12. `merchantScopeSettlementsStayIsolated` L65–L76

---

## 2. 取证命令与可执行产物

### 2.1 锁哈希

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

### 2.2 测试实跑

未 `source /etc/complex/complex.env`。用 Python 只解析三键写入进程环境后 `mvn test`。进程里另外已有（非本席 source）`COMPLEX_CONFIG_DIR` / `COMPLEX_HOME` / `COMPLEX_LOG_DIR`。

导出 URL 去密：`jdbc:postgresql://127.0.0.1:5432/complex`（库名 **complex**）。USERNAME=`complex`。PASSWORD 已设。

JDBC 类注解写死 `jdbc:postgresql://127.0.0.1:5432/complex_test` + username `complex` + password `${COMPLEX_DATABASE_PASSWORD:...}`。实跑 Flyway：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`；`Successfully validated 396 migrations`；`Current version of schema "public": 428`；`No migration necessary`。

```
mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests,com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests test
```

关键输出：

```
[INFO] Running com.complex.platform.merchantportal.MerchantPortalServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.488 s
[INFO] Running com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.730 s
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
=== MVN EXIT 0 ===
```

**绿测不是 GEN-003 通过证据。** 下列 finding 均在绿测前提下成立。

---

## 3. 第一性原理三问（逐文件）

### 3.1 MerchantPortalServiceTests.java

**用途 / 删掉 HTML 哪条做不到？**
支撑业务地图 L1310「商户侧不是平台后台…不能看平台全局和其他商户数据」、L1322「我的对账（…有疑问可留言提异议）；我的合同」、入口表 L1848–1850「商户侧页面…不看平台全局」，以及治理地图 L256 段归属/资格/责任与零英文。删掉本文件，上述门户行为在本模块没有可失败断言。答得上，有业务上位。

**声称是否被实际执行验证？**
类注释 L37–L40 声称：只见本商户、无商户归属 `MERCHANT_SCOPE_REQUIRED`、跨商户 `CROSS_MERCHANT_FORBIDDEN`、已结清/已挂起不可再提、留言不覆盖裁决痕迹、列宽、同幂等键一次、成功/失败均落审计。
- 已用真 `MerchantPortalService` + 真 `IdempotencyService` + 内存幂等仓储执行：用例 1/2/4/5/7/8/9 对对应声称有能失败断言。
- 桩：`currentMerchantScope().thenReturn(Optional.of(...))` 与真实现商户账号分支一致；`thenThrow(SCOPE_REQUIRED)` 与真实现 L425–L431「无 merchantId 即抛」一致，**不是** `currentMerchantIdQuietly` 的 empty。`saveSettlement` thenAnswer 返回入参，与 JDBC `return s` 一致。
- 未锁住的声称见 GR-02/03/04/05/06。

**未经验证前提？**
- Mockito `requirePermission` 默认 void=授权通过，未对「无码即拒」做反例（异议路径连 verify 调用都没有，GR-05）。
- 列宽用例前提「合并后」未构造既有理由（GR-02）。
- 空理由文案中文未被断言（GR-06）；真实现是英文。
- 内存幂等仓储 `tryUpdateWhenResponseCode` 走接口 default `false`，不模拟 FAILED_RETRYABLE 重试；本文件成功重放路径不依赖它。

### 3.2 MerchantPortalJdbcIntegrationTests.java

**用途 / 删掉 HTML 哪条做不到？**
- `merchantScopeSettlementsStayIsolated`：锁 JDBC `listSettlementsByMerchant` 谓词隔离，对应 L1310/L1850「不能看其他商户」。门户 `listMySettlements` 生产调用该方法。有上位。
- `platformAllSettlementsPagedRealTotalAndSecondPageReachable`：锁 `listAllSettlementsPaged` 全库 COUNT+第二页。门户现行实现 **不再调用** 该方法（M020-001 fail-closed）。HTML 商户侧「不看平台全局」不要求、且禁止把该能力标成商户专区。删掉该用例，没有任何已映射 HTML 句做不到。答不上必须存在 → 走四分类，见 GR-01。

**声称是否被实际执行验证？**
类注释 L16–L21 原文：

> AUD-005 收尾(#187 复核残留,四域之外):商户专区平台管理员「全量合同/结算」真分页。
> 坐实平台全量结算 listAllSettlementsPaged = SQL COUNT 取真 total(跨所有商户)+ LIMIT/OFFSET 第二页可达;
> 商户自 scope 的 listSettlementsByMerchant 仍谓词隔离(只见本商户、无 LIMIT),不受影响。
> 合同侧平台全量走既有 listContractsPaged(ContractJdbcIntegrationTests 已覆盖 501/多页)。

- 「真 total + 第二页」：用例 11 对 **仓储方法** 实跑，且能因 total=页长而红（`isGreaterThanOrEqualTo(2L)` + size=1）。这验证的是死生产路径，不是门户。
- 「商户专区平台管理员全量」：现行 `MerchantPortalService` L23–L26、L89–L95 无平台全量分支；无商户账号抛 `MERCHANT_SCOPE_REQUIRED`。注释不可从现行门户实现复现。
- 「无 LIMIT」：用例 12 每商户只插 1 条结算，加 `LIMIT 1` 仍绿。该分句未锁。
- 「ContractJdbcIntegrationTests 已覆盖 listContractsPaged」：该文件存在且有 `listContractsPagedReturnsRealTotalAndReachesNextPage`（类型 4 路径存在性）。属别模块。

**未经验证前提？**
- 假定 `listAllSettlementsPaged` 仍是商户专区行为。graft grep 生产零调用，前提为假（GR-01）。
- `@SpringBootTest` 不消费 `COMPLEX_DATABASE_URL`/`USERNAME`（导出 URL 指向 `complex`，注解写死 `complex_test`）。实跑打到 complex_test 是注解护栏，不是三变量绑定。

---

## 4. 顺从式：22 项逐条适用证据

每条：适用性 → 本模块文件证据 → 结论。不新增判据。

### BSM-001 — 整体说明：这套系统到底管什么
- 锁定义来源 HTML `:505`；本席另读业务地图 L1310/L1322 商户专区句（映射指引）。
- 适用：测试锁「谁在经营 / 签了什么合同 / 谁能操作」在商户自助面上的只读合同+对账+异议留言，不发明新子系统。
- 证据：用例 1 合同+spaceIds；用例 3 本商户结算；用例 4 异议不改状态（与「提出异议期间付款义务不暂停」同向）。
- 缺口：不在本文件的账单/工单/费用总表属别模块。不因此 FAIL 本项映射（本模块只有两测试文件）。

### BSM-003 — 系统通用能力：数据可改可删、操作有引导、权限分岗
- 锁 HTML 原文仅 h2+lead（`:563`）；本席读了后续 h3 存档删除段，**不把锁外 h3 当新判据**。
- 适用（窄）：「权限分岗」——测试传入的权限码与 V200/`requirePermission` 一致。
- 不适用：存档/删除/分步引导/动态码——本模块无对应测试。
- 证据：用例 1 verify `contract.view`；用例 3 verify `merchant.portal.settlement.view`。异议权限码未 verify（GR-05）。

### BSM-114 — 各类入口：不同人只看自己该看的、只做自己该做的
- HTML 原文入口表商户侧页面行（文件 L1848–L1850；锁摘录同等）：「在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。」
- 适用：本模块核心。
- 证据：用例 1/3 只打本商户仓储方法且 never 全量分页；用例 2 无商户范围 fail-closed；用例 8 跨商户禁。
- 冲突点：JDBC 类注释把「平台管理员全量」写成商户专区行为（GR-01）。

### PGM-001 — 整体定位：平台治理层…底座
- HTML `:256` 段：归属/资格/责任；数据可溯源；**界面全中文（所有终端零英文）**，例外仅单据号/设备编号/二维码/系统内部标识符。
- 适用：资格（合同归属+权限码）、责任（审计事件名）、报错文案零英文。
- 证据：用例 9 锁「结算单当前状态不可发起异议」且 `doesNotContain(status)`；用例 6 消息含 `500`（中文句「异议留言不能超过 500 字」被部分锁）。
- 缺口：用例 10 不锁文案；真实现 L134 `"reason is required"` 英文（GR-06 + XM-1）。权限码/内部状态码属「系统内部标识符」例外，测试不展示它们给用户即合规。

### GEN-001 — 必要性与最小性
- 适用。JDBC 全量分页用例对现行门户无需求承载（GR-01）。其余 10 个服务用例 + 隔离 JDBC 用例有上位。

### GEN-002 — 可执行正确性
- 适用。12 例实跑 exit 0。入口声称与 `MerchantPortalService`/`JdbcContractRepository` 对照见各用例。绿测不替代 GEN-003。

### GEN-003 — 测试有效性（核心）
- 适用。按「真实现语义 / 桩是否不可能 / 注释可否复现 / 生产调用方 / 改坏是否红 / 首次正常无锁」审查。成立缺陷：GR-01…06。
- 能红的正向锁（摘）：改 `listMyContracts` 走 `listContractsPaged` → 用例 1 `never()` 红；改异议为改 status=`disputed` → 用例 4 红；清空 `resolvedBy` → 用例 5 红；去掉单段 500 检查 → 用例 6 红；去掉幂等 → 用例 7 `times(1)` 红；去掉跨商户闸 → 用例 8 红；允许 settled 异议或把 status 拼进消息 → 用例 9 红。

### GEN-004 — 信任边界校验
- 适用（窄）。空理由 / 超宽 / settlement_id `requireText`。空理由有用例 10。超宽有用例 6（合并缺口 GR-02）。缺 id / 结算不存在无用例——非类注释声称的首次正常主路径，作观察不立项。

### GEN-005 — 授权与最小权限
- 适用。列表路径 verify 权限码。无 scope 列表 fail-closed 有用例 2。异议路径未 verify `merchant.portal.settlement.dispute`（GR-05）；异议无 scope 无用例（GR-03）。真实现 `currentMerchantScope` 无商户号即抛，桩 throw 合法。

### GEN-006 — 数据完整性
- 适用。异议不改 status、合并理由、列宽 500（V162）。合并超宽未按「合并后」构造（GR-02）。JDBC `saveSettlement` 含 resolved 列属仓储，本模块 JDBC 未跑异议 upsert（XM-2）。

### GEN-007 — 并发与幂等
- 适用。用例 7 用真 `IdempotencyService`+内存仓储锁同键一次。未测并发双飞——首次正常是重试而非竞态，不立项。`InMemoryIdempotencyRepository` 无 `tryUpdateWhenResponseCode` 覆写，失败可重试路径未在本文件出现。

### GEN-008 — 接口与模式一致性
- 适用。夹具状态字 `pending_settlement`/`settled`/`disputed`/`voided` 与 `ContractService.advanceSettlement`/`DISPUTABLE_STATUSES` 同词。`MerchantContractView` 字段由 `of()` 映射，用例 1 只断言 merchantId/spaceIds/total，不发明额外字段。Controller 幂等键助手不在本模块文件。

### GEN-009 — 错误处理与恢复
- 适用。失败路径 recordFailure 在用例 2/8/10。成功列表结算未锁失败之外的成功审计（GR-04）。服务 catch 后 rethrow，测试 `assertThatThrownBy` 覆盖。

### GEN-010 — 安全与敏感信息
- 适用（窄）。跨商户禁；无注入/密钥。JDBC 注解写死 complex_test，避免导出 URL 的 `complex` 库被测到——实跑 Flyway 证实 test 库。类内默认密码字面量 `complex_dev_pwd` 仅 fallback；实跑走环境密码。

### GEN-011 — 可观测与审计证据
- 适用。合同列表成功审计有锁（用例 1）。结算列表成功审计无锁（GR-04）。异议成功/失败有锁（4/8/10）。无商户范围列表失败审计有锁（用例 2）。

### GEN-012 — 资源与性能边界
- 适用（窄）。JDBC 全量分页 SQL LIMIT/OFFSET（该方法生产未用）。门户列表走 `PageResponse.of` 内存切片，用例均 1 条+page=1,size=500，不锁第二页。非首次正常缺页，观察不立项。`listAllSettlementsPaged` 实现 `Math.min(size,500)` 无对应断言。

### GEN-013 — 构建配置与依赖
- 适用。单元测无库。JDBC 只应用密码变量+写死 test URL；不读 `COMPLEX_DATABASE_URL`。本席按 runbook 仍导出三变量。mvn 可复现 BUILD SUCCESS。

### GEN-014 — 数据库迁移正确性
- 不适用为主：本模块无迁移文件。JDBC 依赖已迁到 428 的 complex_test，属运行前提（命令输出），不把迁移缺陷记入本模块队列。

### GEN-015 — 界面可用性与可访问性
- 适用（文案语言，无 UI 文件）。用例 9 锁中文报错。用例 10 不锁，真实现英文（GR-06）。

### GEN-016 — 文档可验证性
- 适用。JDBC 类注释「商户专区平台管理员全量」不可从现行门户复现（GR-01）。服务测试方法注释 L173「合并后超过」不可从该用例 fixture 复现（GR-02）。服务类注释 L37–L40 与 V0.01 实现大体可复现，异议无 scope / 结算成功审计两处过宽（GR-03/04）。

### GEN-017 — 业务来源分类
- 适用。本席 findings 按 冲突→细化→技术性→添加。无添加项，不发 2.2 问询。

### GEN-018 — 第一性原理证据
- 适用。本报告每条 GR 落到 HTML 原文 / 可执行产物 / 迁移原文 / 命令输出之一。不把 M020 历史 PASS 或注释当依据。

---

## 5. 对抗式 grilling（自问自答，不等操作员）

❓ **Q1 — 绿了是否等于门户行为被锁住？**
➡️ 否。surefire 12/12 只证明断言通过。GEN-003 要问改坏实现是否红。结算列表去掉 `recordSuccess`、异议去掉 `requirePermission`、把列宽检查改成只量新留言、去掉异议 `requireMerchantScope`，现有对应用例仍绿。

❓ **Q2 — JDBC「全量真分页」是不是还在服务商户专区？**
➡️ 否。`listMySettlements` 只调 `listSettlementsByMerchant`。graft grep `backend/src/main` 中 `listAllSettlementsPaged` 零服务调用。类注释仍写「商户专区平台管理员全量」——与 BSM-114「不看平台全局」及服务 L23–L26 fail-closed 冲突。ponytail-review：`MerchantPortalJdbcIntegrationTests.java:L16-63 delete: 过时全量分页注释与无生产调用方用例。隔离用例保留。`

❓ **Q3 — 桩有没有伪造真实现不可能的返回？**
➡️ `Optional.of(merchantId)` 与 throw `MERCHANT_SCOPE_REQUIRED` 均可由真 `currentMerchantScope` 产生。未使用 `Optional.empty()`（那是 `currentMerchantIdQuietly`）。幂等用真服务+第二适配器，不是手写「第二次直接返回」。此项通过。

❓ **Q4 — 「合并后 500」有没有被第一次正常操作打到？**
➡️ 平台已留理由、商户再留言是 M020-002/003 的正常续写。用例 5 有既有理由但不碰 500；用例 6 碰 500 但既有理由为 null。把 `merged.length()` 改成 `reason.length()` 两例都仍绿。立项 GR-02。

❓ **Q5 — 无商户账号打异议是不是极端？**
➡️ 否。类注释写「无商户归属一律 MERCHANT_SCOPE_REQUIRED，不存在平台全量口径」。列表有用例 2；`disputeOnce` L140 同样 `requireMerchantScope`。平台管理员误入异议是红线④首次正常。立项 GR-03。

❓ **Q6 — 英文 "reason is required" 是不是测试发明的业务？**
➡️ 否。测试不断言该字符串。真实现 L134 写出英文，PGM-001 禁止展示英文。测试已走到该分支却让英文存活。测试缺陷=未锁零英文；实现缺陷=跨模块观察。

❓ **Q7 — deposit_offsetting / arrears_collecting 成功异议要不要立项？**
➡️ 实现集合含这两态，与 `ContractService.advanceSettlement` 同词。HTML 未点名状态字。首次正常开账态是 `pending_settlement`（已锁）。删掉另两态现有用例不红。属未锁分支，**不立项**（不为 HTML 未写的状态补需求），记观察 OBS-3。

❓ **Q8 — 有没有添加项/锁外判据？**
➡️ 没有。不把「费用总表/综合账单/工单」塞进本模块。Controller 幂等键、OpenAPI、前端页属别模块。

---

## 6. 用例×真实现对照（改坏是否红）

| 用例 | 真实现锚点 | 断言能否为该语义变红 | 缺口 |
|---|---|---|---|
| 1 本商户合同+spaceIds | Service L58–L77 | 能：错商户/改走 listContractsPaged/去掉 spaceIds/去掉成功审计 | 不锁内存第二页 |
| 2 无 scope 列表拒+失败审计 | currentMerchantScope 抛；Service L78–L81/L106–L109 | 能：列表 fail-closed 或失败审计被删 | 不覆盖异议（GR-03） |
| 3 本商户结算 | Service L89–L105 | 能：改走 listAllSettlementsPaged 或去掉 view 权限调用 | 不锁成功审计/total（GR-04） |
| 4 异议不改状态 | disputeOnce L144–L158 | 能：status 被改 | 不 verify dispute 权限（GR-05） |
| 5 保留裁决+追加理由 | L148–L158 | 能：resolved 被清空或理由被覆盖 | 不与 500 合并 |
| 6 列宽 500 | L151–L153；V162 VARCHAR(500) | 能：单段超宽漏检 | 不测合并后超宽（GR-02） |
| 7 同幂等键一次 | IdempotencyService.execute 真路径 | 能：第二次再 saveSettlement | 内存仓储非 JDBC 幂等表 |
| 8 跨商户 | L140–L143 | 能：闸被删 | — |
| 9 settled/disputed/voided | DISPUTABLE_STATUSES L32–L33；L144–L147 | 能：这三态被放行或消息回显英文状态 | 不锁另两可异议态（观察） |
| 10 空理由 | L132–L135 | 能：空白被接受 | 不锁中文（GR-06） |
| 11 全量分页 | Jdbc L306–L319 | 能：total=页长时红 | **无生产调用方**（GR-01） |
| 12 商户隔离 | Jdbc L293–L303；门户 L95 调用 | 能：谓词失效会看到外商户 id | 「无 LIMIT」未锁 |

---

## 7. 临时 findings

编号本席临时 GR。冻结 ID 只引用锁内。工程缺陷是否成立按「改现行测试目标实现/注释后，本模块文件是否在说假话或不能为该假话变红」。

### GR-01 — JDBC 类注释与全量分页用例把已删除的「商户专区平台全量」当成现行事实

- 位置：`MerchantPortalJdbcIntegrationTests.java:16-21` 类注释；`:46-63` `platformAllSettlementsPagedRealTotalAndSecondPageReachable`
- 原文：见 §3.2 引用。「商户专区平台管理员「全量合同/结算」真分页」；测试体调用 `repository.listAllSettlementsPaged(1,1)/(2,1)`。
- 更长上下文：同文件 L30 注入 `ContractRepository`，L73–L75 才是门户仍用的 `listSettlementsByMerchant`。服务 L89–L95 现行只走商户谓词+内存切片；L23–L26 写明「本前缀不提供全量口径,M020-001」。
- 正常触发：读本测试类即看到与 HTML/现行门户相反的产品声称；跑用例 11 只锻炼无生产调用方的仓储方法。
- 影响：GEN-016 把旧 AUD-005 写成当前商户专区事实；GEN-001 保留无门户需求承载的用例；BSM-114 明文「不看平台全局」。
- 四分类：**冲突**（先于细化/技术性）。上位 HTML：业务地图 L1848–1850 商户侧「不看平台全局」；L1310「不能看平台全局和其他商户数据」。
- 冻结 ID：BSM-114、GEN-001、GEN-016、GEN-003
- 依据类型：HTML 原文 + 可复现命令（graft grep 生产零调用；mvn 该例绿不能证明门户语义）
- 工程缺陷：**成立**
- 最小拟处置：删除或改写 L16–L21 中「商户专区平台管理员全量」表述；删除用例 11（或迁到合同仓储模块并去掉商户专区语义）。保留用例 12，注释改为只锁 `listSettlementsByMerchant` 隔离。不改 HTML。

### GR-02 — 「合并后超过 500」注释不可从用例复现；只量新留言时改坏合并检查不红

- 位置：`MerchantPortalServiceTests.java:173-188`；对照实现 `MerchantPortalService.java:148-153`
- 原文 L173：`// M020-003:合并后超过 dispute_reason 列宽(500)→ 400 且不写库;恰为 500 允许。`
- 更长上下文：`stubOwnOpenSettlement()` L75–L81 的 settlement 无既有 `disputeReason`。`"漏算".repeat(251)` 只是新留言 502 字。实现却是 `merged = existing + "\n" + reason` 再比 500。V162：`dispute_reason VARCHAR(500)`。
- 正常触发：平台已留「平台：…」理由（用例 5 那种单）后再贴接近 500 字商户留言——第一次正常续写就会超列宽。当前用例 6 打不到。
- 影响：把 `if (merged.length() > 500)` 改成 `if (reason.length() > 500)`，用例 5 与 6 仍绿，库写入可超 VARCHAR(500)。
- 四分类：**细化**（上位：L1322「有疑问可留言提异议」+ V162 列宽；非新业务发明）
- 冻结 ID：GEN-003、GEN-016、GEN-006
- 依据类型：外部/库规范原文（V162 `VARCHAR(500)`）+ 现行实现可执行路径对照
- 工程缺陷：**成立**
- 最小拟处置：在用例 6 或另例构造既有理由+新留言使 `merged.length()>500`，断言 400 且 never save；保留恰 500 允许。

### GR-03 — 类注释「无商户归属一律 MERCHANT_SCOPE_REQUIRED」未覆盖异议入口

- 位置：`MerchantPortalServiceTests.java:37-40` 类注释；`:102-122` 仅列表；对照 `MerchantPortalService.java:140-143`（`disputeOnce` 内 `requireMerchantScope`）
- 原文类注释：`无商户归属一律 MERCHANT_SCOPE_REQUIRED,不存在平台全量口径`
- 更长上下文：用例 2 只 `listMyContracts` / `listMySettlements`。`submitSettlementDispute` 在 execute 的 lambda 里才进 `disputeOnce`。无 scope 时真 `currentMerchantScope` 抛同样错误码。
- 正常触发：无商户号账号（含平台管理员）对 `/settlements/{id}/dispute` 发留言。
- 影响：删除 `disputeOnce` 的 `requireMerchantScope` 后，无 scope 会在 `requireMerchantScope().equals(contract.merchantId())` 前就已因 throw 消失；若改成 quietly empty，可能 NPE 或其它码，现有用例不红。
- 四分类：**细化**（BSM-114 / 红线④ 已有条目的下位）
- 冻结 ID：GEN-003、GEN-005、BSM-114
- 依据类型：HTML 原文（不看平台全局）+ 现行实现 L140
- 工程缺陷：**成立**
- 最小拟处置：用例 2 增加 `submitSettlementDispute` 同样 `MERCHANT_SCOPE_REQUIRED` + `dispute_failed` 审计 + never save。

### GR-04 — 结算列表成功路径不断言审计，与类注释「成功/失败均落审计」及真实现不一致

- 位置：`MerchantPortalServiceTests.java:37-40`、`:124-135`；对照 Service L96–L104
- 原文类注释：`成功/失败均落审计(M020-001…006)`
- 更长上下文：用例 1 对合同 `recordSuccess(..., Map.of("count", 1))` 有 verify。用例 3 只 verify 权限码与 never `listAllSettlementsPaged`，无 `recordSuccess`、无 `total`。
- 正常触发：商户打开「我的对账」成功列表。
- 影响：删除 `listMySettlements` 的 `recordSuccess`，用例 3 仍绿。
- 四分类：**细化**（PGM-001「谁创建、谁审批、谁复核」留痕 + GEN-011）
- 冻结 ID：GEN-003、GEN-011、PGM-001
- 依据类型：HTML 原文（治理地图责任盒）+ 现行实现 L96–L104
- 工程缺陷：**成立**
- 最小拟处置：用例 3 补 `verify(audit).recordSuccess("merchant_portal.settlement.viewed", ...)` 及 `page.total()`。

### GR-05 — 异议入口不断言 `requirePermission("merchant.portal.settlement.dispute")`

- 位置：用例 4/5/7 等异议成功路径；对照 Service L116、V200 权限码
- 原文：测试文件无该 verify。真实现 L116：`authorizationService.requirePermission("merchant.portal.settlement.dispute");` 在幂等 execute 之前。
- 更长上下文：用例 1/3 对 view 权限有 verify。V200 插入 `merchant.portal.settlement.dispute`。Controller 只转调 service。
- 正常触发：无该权限的主体调异议。
- 影响：删除 L116，现有用例仍绿（mock 默认 void）。
- 四分类：**细化**（BSM-114「只做自己该做的」）
- 冻结 ID：GEN-003、GEN-005、BSM-114
- 依据类型：HTML 原文 + 可执行实现 L116 + 迁移 V200 原文
- 工程缺陷：**成立**
- 最小拟处置：至少一条异议用例 `verify(authz).requirePermission("merchant.portal.settlement.dispute")`。

### GR-06 — 空理由路径不锁中文报错；真实现英文，PGM-001 不能被该例变红

- 位置：`MerchantPortalServiceTests.java:244-255`；对照 `MerchantPortalService.java:132-135`
- 原文测试：只断言 `e.code()=="VALIDATION_FAILED"`。实现：`throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");`
- 更长上下文：用例 9 已示范锁中文且禁止回显英文状态。PGM-001：「展示给用户的一切文字…必须全部为中文，不得出现任何英文…报错」。`reason is required` 不是四类例外。
- 正常触发：商户提交空白异议理由。
- 影响：英文报错保持现状则用例 10 绿；即使将来改中文，该例也不保护回归。
- 四分类：**细化**（PGM-001 零英文的下位；测试未发明新规则）。实现侧英文本身是别模块冲突，见 XM-1，不入本模块修复队列。
- 冻结 ID：GEN-003、GEN-015、PGM-001
- 依据类型：HTML 原文（治理地图零英文段）
- 工程缺陷：**成立**（本模块=断言缺口；不在 B3 改 Service 文案，除非 B2 把实现一并划入——本席按指令只登记测试侧）
- 最小拟处置：断言消息为中文且不含 `reason is required`。若现行实现仍英文，该测试会红——那是 XM-1 的可失败锁，属跨单元 TDD，B2 决定写入集合。

---

## 8. 观察（不入本模块队列）

- **OBS-转入/GR-01 同源**：M020 所记 JDBC 类注释过时——本席独立确认成立，已升格为 GR-01，不再只当观察。
- **XM-1**（M020 实现）：`MerchantPortalService.disputeOnce` L134 `"reason is required"` 英文，与 PGM-001 冲突。本模块测试未锁。
- **XM-2**（合同仓储）：`listAllSettlementsPaged` 生产零调用仍留在 `ContractRepository`/`JdbcContractRepository`。本席不在 M022 修仓储。
- **XM-3**（M020 实现）：`requireMerchantScope` 的 `orElseThrow` 在真 `currentMerchantScope` 已抛的前提下为死代码。测试桩 throw 与真实现一致。
- **OBS-3**：`DISPUTABLE_STATUSES` 含 `deposit_offsetting`/`arrears_collecting`，无成功用例；HTML 未点名这两态，不立项。
- **OBS-4**：JDBC 用例 12 每商户 1 行，注释「无 LIMIT」不能失败。
- **OBS-5**：Controller `ApiRequests.requireIdempotencyKey`、前端两页、OpenAPI 不在本模块文件列表。
- **OBS-6**：工作区相对 HEAD 脏、相对 origin/main 干净；审计的是工作区=origin/main V0.01。
- **OBS-7**：V200 `permission_name` 英文字面属迁移模块（M021），内部标识例外，不记本模块缺陷。
- **OBS-8**：导出 URL 指向 `complex`；注解写死 `complex_test` 是隔离护栏。实跑未写库（`@Transactional` + BUILD SUCCESS）。

无添加项。无 HTML 自身问题需铁律 4 暂停。无 questions。

---

## 9. ponytail-review（只列可删）

`MerchantPortalJdbcIntegrationTests.java:L16-63: delete: 将已废除的商户专区平台全量分页写成现行事实的注释与用例。隔离用例保留并改注释。`

`net: 约 -30 行（注释+用例 11），另需数行注释改写。`

其余服务测试不是膨胀：真 IdempotencyService、fail-closed、中文状态、列宽、跨商户都是第一次正常路径的锁。Lean already on the service class except the assertion gaps in GR-02…06.

---

## 10. 结论

| 项 | 值 |
|---|---|
| 文件覆盖 | 2/2 全文 |
| 22 判据 | 已逐项记录适用/不适用/证据 |
| 三线 | 已执行 |
| 临时 finding | GR-01…06，工程缺陷均成立 |
| 添加项 | 0 |
| 本席 B1 | **FAIL** |
| 哈希行 | 本文件第一行，独立实算 |

完成标准：整模块审完、问题收齐、不中途修复、不读席甲 raw。满足。B2 归并前本席不再改本文件。

## M022 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **36216** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M022 FILES 2 / 锁映射 22 / 计划冻结 227 行）
- 正本：`/home/james/complex-management`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `86b8dd2abf8bdc58726a7f4ad32b40182298dc3b`。B3=0。无源修复。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。本席只读 `M022-b1-self-raw.md` 前 **17072** 字节（V0.01）；其后 V0.02 B2 结论段不作为本票依据。
- 本席 B2-r1：**PASS**（001–007 逐 ID 全 PASS；添加 0、不发 2.2。非阻断勘误见 002 引用面措辞、007 全仓 requireText 条数，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/前缀字节/候选段/两测试文件/FieldLabels/GlobalExceptionHandler/JdbcContractRepository/全仓测试引用；未对任一 `complex_test*` 库做写操作，未重审模块、未跑 mvn。

此票只判断 findings.md V2.68 顶部「当前 M022 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占 `CRITERIA-LOCK-SHA256:` 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M022-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 36216 | `30169fb47486896f875a51adcd5d4c1397b08aa5f8510770c459fa820cbc1bc2` |
| `docs/audit-2026-09/modules/M022-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 17072 | `dc4c17f4b9bc10289b168e2e06de59b94f24175c9ecc42b6310afbd064374e69` |
| `docs/audit-2026-09/findings.md` | V2.68 | 1041959 | `ace41683c71e87d24329d2b8798bb135d9254f59f9ae35944c3ac33683f59b5d` |

席甲 raw 全文现大于 17072 bytes；本席按指令只取前 17072 字节，不读其后 V0.02 B2 段。本文件独占锁哈希字段行仍仅文首一处（B1 正文提及该字段名的说明句不算新独占行），本段未新增。

controller / runbook / modules.md 只读核验（本席未改；与 B1 同）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

上位句（本席 B2 只核分类所需原文，不重审模块）：

- 业务地图 L1310：`商户侧不是平台后台……但不能看平台全局和其他商户数据。`
- 业务地图 L1848–1850 商户侧页面行：`在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。`
- 治理地图 L287：`全局原则 · 界面全中文（所有终端零英文）`……`必须全部为中文，不得出现任何英文`……`报错`……四样例外不含机读校验键。

`questions/M022*`：不存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：001 冲突+删用例、002 真实库缺口、007 不采 XM-1 |
| grilling | 只借追问结构自查：差集有无漏并/改写、002/007 核源是否成立、有无应升添加 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/HTML 行号/translate 与 handler 原文/全仓测试引用 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审两测试文件的模块结论 |
| codebase-design / ponytail-review | 最小是删过时全量用例、补隔离/断言，不发明业务规则 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 已实际调用 grep：`listContractsByMerchant` / `listContractSpaceIds` / `FieldLabels` / `translate` / `p("reason"` / `requireText`；未管道 head/tail |

### 完整差集（GR-01…06、XM-1…3、OBS-3…8、席甲 CX-M022-001/002、OBS-1…4）

漏并：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–007 或只登记段。无单席独有缺陷被丢。无本批新开「否」。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M022-001 | GR-01 + CX-M022-001 | 冲突/是/是（采本席：冲突、删用例） | **PASS（冲突/是/是）** |
| M022-002 | CX-M022-002（本席 B1 未发现） | 细化/是/是 | **PASS（细化/是/是；核源成立）** |
| M022-003 | GR-02 | 细化/是/是 | **PASS（细化/是/是）** |
| M022-004 | GR-03 | 细化/是/是 | **PASS（细化/是/是）** |
| M022-005 | GR-04 | 细化/是/是 | **PASS（细化/是/是）** |
| M022-006 | GR-05 | 细化/是/是 | **PASS（细化/是/是）** |
| M022-007 | GR-06（XM-1 处置口径） | 细化/是/是（锁 translate 出网中文，不改 M020 服务） | **PASS（细化/是/是；核源成立；XM-1 改服务不采）** |

冲突入队 1（001）。细化入队 6（002–007）。技术性入队 0。观察否：双方 OBS/XM 按候选「只登记不入队」。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–007（7）；「否」本批新开 = 0。

---

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-01 + CX-001 | 001 | 同根：JDBC 类注释「商户专区平台管理员全量合同/结算真分页」+ 用例 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`；服务层 M020-001 后 0 处调用 `listAllSettlementsPaged`/`listContractsPaged`；`listAllSettlementsPaged` 生产 0 调用方。分类与处置采本席 B1：**冲突**、**删除该用例**（席甲原判细化且拟保留用例改注释）。事实未改写。席甲 `:30`/`:73` 行内注释候选一并改写，未丢。 |
| CX-002 | 002 | 席甲独有，本席 B1 未立。候选原样入队。核源见第二节，**成立**。 |
| GR-02 | 003 | 同根：注释「合并后超过列宽」但 `stubOwnOpenSettlement` 无既有理由，只测新留言 502 字；改 `merged.length()`→`reason.length()` 两例仍绿。未改写。 |
| GR-03 | 004 | 同根：类注释「无商户归属一律 MERCHANT_SCOPE_REQUIRED」，`:102-122` 只覆盖两条列表；`disputeOnce` 内 `requireMerchantScope` 无用例。未改写。 |
| GR-04 | 005 | 同根：结算列表成功路径不断言 `recordSuccess`/`total`。候选 verify 参数与 `MerchantPortalService:96-104` 原文同形。未改写。 |
| GR-05 | 006 | 同根：异议路径未 verify `merchant.portal.settlement.dispute`。未改写。 |
| GR-06 + XM-1 口径 | 007 | 同根：空理由只断言错误码。处置不采本席 B1「若锁中文则现网英文会红、B2 再决是否改服务」——改为断言 `FieldLabels.translate(e.getMessage())`＝「原因未填写」。核源见第三节，**成立**；不是把缺陷改写成别的事，是按全仓出网约定收窄写入集合。XM-1（改 M020 服务文案）只登记不入队。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：

- 本席 XM-1：服务层 `"reason is required"` 机读英文——并入 007 口径后不改生产，只登记。
- 本席 XM-2 ＝ 席甲 OBS-3：`listAllSettlementsPaged` 生产零调用，归合同域。
- 本席 XM-3：`requireMerchantScope` 的 `orElseThrow` 在真 `currentMerchantScope` 已抛前提下为防御性冗余。
- 本席 OBS-3…8：另两可异议态无成功例、JDBC「无 LIMIT」不能失败、Controller/前端/OpenAPI 不在本模块文件、工作区相对 HEAD 脏相对 origin/main 干净、V200 权限码英文字面、注解写死 `complex_test`。
- 席甲 OBS-1：本包无控制器测试。
- 席甲 OBS-2：服务测试为席甲 M020 自审——候选引言已写明「席甲判通过、Grok 查出 003–007 经核源成立、如实入队」，不是漏并的代码缺陷。
- 席甲 OBS-4：数据源属性硬编码范式。
- 本席 OBS-转入/GR-01 同源：已升格为 001，不再只当观察。

席甲 B1 对 `MerchantPortalServiceTests` 10 例判「通过」（自审）。本席 B1 GR-02…06 五处断言缺口候选全部入队（003–007），未被席甲自审票吞掉。符合席甲 OBS-2「若 Grok FAIL 以 Grok 为准」。

---

### 二、002 独立核实（本席 B1 未发现）—— **成立**

候选事实：`JdbcContractRepository.listContractsByMerchant:288`（`WHERE merchant_id = :mid`）+ `listContractSpaceIds` 在全仓测试中无 `@SpringBootTest` 真实库用例；把该谓词改坏无用例会红。本票不沿用席甲探针结论，独立检索。

1. **JDBC 原文** `JdbcContractRepository.java:285-290`：
   `SELECT … FROM contracts WHERE merchant_id = :mid ORDER BY created_at DESC, id`。
   `listContractSpaceIds:480-485`：`SELECT space_id FROM contract_spaces WHERE contract_id = :contract_id`（无商户谓词，按合同 id 取绑定）。

2. **全仓测试引用 `listContractsByMerchant`**（test 树）：
   - Mockito：`MerchantPortalServiceTests:86/:115`、`ContractScopeGuardTests`（12 处 `when`）、`BarServiceTests:596/:642`、`TicketingServiceTests:3368/:3380`。
   - 非 Mockito、亦非真实库：`ContractServiceTests:822/:844` 调接口方法。该类无 `@SpringBootTest`、类注释写「无 DB」；`InMemoryContractRepository` **不覆写** `listContractsByMerchant`，走 `ContractRepository:57-61` default（`listContracts().stream().filter(merchantId)` 内存过滤）。改 JDBC `WHERE merchant_id` **不会**使该例变红。
   - `@SpringBootTest` / `*Jdbc*.java`：对 `listContractsByMerchant` **0** 命中。本模块 `MerchantPortalJdbcIntegrationTests` 只测 `listAllSettlementsPaged` 与 `listSettlementsByMerchant`。`ContractJdbcIntegrationTests` / `ContractScopeEnforcementJdbcIntegrationTests` 亦不调用该方法。

3. **全仓测试引用 `listContractSpaceIds`**：Mockito 仅 `MerchantPortalServiceTests:87`；`ContractServiceTests` 走 InMemory 覆写（`boundSpaces` 映射，L1088-1091），无 DB。`*Jdbc*.java` **0** 命中。

4. **改坏 `WHERE merchant_id` 会不会红**：现有 12 例与全仓其它测试均不执行 JDBC 该 SQL。Mockito 桩返回写死列表；InMemory default 不读该 SQL。**无用例会红。** 结算侧对等真实库用例 `merchantScopeSettlementsStayIsolated:65-76` 已能因 `listSettlementsByMerchant` JOIN 谓词失效变红，合同侧缺对等锁。

工程结论：**成立**。四分类：**细化**（上位 L1310/L1850「只见本商户／不看平台全局」的下位 SQL 隔离；不是新隔离规则）。工程缺陷：**是**（GEN-003）。队列：**是**。最小处置——同意：在本 JDBC 类加 `merchantScopeContractsStayIsolated`（两商户各存一份合同，`listContractsByMerchant` 只回本商户；无绑定合同时 `listContractSpaceIds` 回空），沿用本类 `contract(...)` 夹具与 `@Transactional` 回滚；不改生产代码。

非阻断：候选「只被 Mockito 引用」对 `ContractServiceTests` 接口 default／InMemory 覆写略宽；不影响「无真实库用例、改坏 JDBC 谓词不红」。不否决 002。

票：**PASS（细化／是／是）**。

---

### 三、007 处置口径独立核实—— **成立**

本席 B1 GR-06 拟「断言消息为中文且不含 `reason is required`」；若现网英文则该测试会红，并把实现侧英文记 XM-1（跨模块，B2 再决写入集合）。候选不采改已闭环 M020 服务文件，改为断言 `FieldLabels.translate(e.getMessage())` 等于「原因未填写」。本票独立读出口层，并核对本席 M020 B1。

1. **抛点** `MerchantPortalService.java:132-135`：`trimToNull(request.reason())==null` → `ApiException.badRequest("VALIDATION_FAILED", "reason is required")`。服务测试直调 service，`e.getMessage()` 仍是该英文字面。

2. **全仓约定** `Strings.requireText:22-30`：消息里的英文字段键是**机读**信息；「写 HTTP 响应体时由 `GlobalExceptionHandler` 经 `FieldLabels.translate` 统一换成中文」。同形：`field + " is required"`。`FieldLabels` 类注释：抛点保留英文键给日志，只在写响应体的一瞬间换成中文。

3. **出网** `GlobalExceptionHandler.java:48-61`：`handleApiException` 在写 body 前 `FieldLabels.translate(exception.getMessage())`（L61）。注释：「展示文案一律中文……抛点保留英文字段键给日志排障，只在写响应体时翻译。」

4. **`translate("reason is required")`**：
   - `REQUIRED_OR_INVALID`（L34-35）匹配 `reason` + `required`。
   - `WHOLE_MESSAGES` 无 `reason`。
   - `p("reason", "原因")` 在 `FieldLabels.java:461`。
   - 返回 `label + "未填写"` ＝ **「原因未填写」**。
   - 纯英文识别不出才走 `FALLBACK`；本键有表项，不走兜底。

5. **本席 M020 B1**（`M020-b1-grok-raw.md` GEN-009 行原文）：`FieldLabels.translate` 能把 `reason is required` / `idempotency_key is required` 译成中文，这两处 **不** 另立项。PGM-001 行：用户可见报错经 FieldLabels。本批 007 与该已落盘判断同向。

6. **不采 XM-1 改服务**：把抛点改成中文会打破 `Strings.requireText` 同形出口（两套文案、盖不住手写抛点），且 M020 已闭环。用户可见零英文已由 handler 成立；缺口是本模块用例 10 不锁这条出网口径。

7. **最小处置**：在 `disputeReasonIsRequired` 断言 `FieldLabels.translate(e.getMessage())` 等于「原因未填写」。删 `p("reason")`、改抛点键、或改成不可译英文 → 该断言红。不改 M020 服务／handler／FieldLabels 生产表。写入集合仍是本模块测试文件。

四分类：**细化**（PGM-001 L287 零英文的下位测试锁；出网已中文，不是与 HTML 相反的冲突，也不是新文案发明）。工程缺陷：**是**（GEN-003 断言缺口）。队列：**是**。本席 B1「若锁中文则可能逼改服务」的 XM-1 生产面 **撤回**；测试侧缺口保留为 007。

非阻断：候选「70+ 处」与 `FieldLabels`/`Strings` 注释「975 处 requireText + 95 处手写」条数不符；同形出口事实成立，不依赖 70 这个数字。该断言锁的是 `translate` 出口而非 handler 被调用——handler:61 已核走同一函数，服务单测本来就不进 `@RestControllerAdvice`。不否决 007。

票：**PASS（细化／是／是；不采 XM-1 改服务）**。

---

### 四、最小处置边界

写入集合限本模块 2 个测试文件，不改生产代码／迁移／OpenAPI／HTML。一次 B3 允许改：

- `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java`（001、002）
- `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java`（003–007）

禁止：改 `MerchantPortalService`/`Controller`、`JdbcContractRepository`/`ContractRepository`、V162/V200、OpenAPI、三份 HTML、锁、台账、FieldLabels、GlobalExceptionHandler。

**001 —— 同意冲突 + 删用例。** 2.1：与 L1848-1850／L1310「不看平台全局」明文不能同时把「商户专区平台管理员全量」写成现行验收。冲突＝判定为删除：删除 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`；类注释与 `:30`/`:73` 改写为「本类只锁商户维度读取的谓词隔离；商户专区不提供平台全量口径」；加版本标记 V0.01；仓储方法去留归合同域（XM-2）。不把该用例迁进本模块改名留下。席甲「不删用例」不采——保留会继续在商户专区测试包里锻炼无生产调用方路径，与 GEN-001／冲突删除同向相反。票：**PASS**。

**002 —— 同意加 `merchantScopeContractsStayIsolated`。** 不发明隔离规则，只锁现行 `WHERE merchant_id = :mid` 与无绑定空间回空。票：**PASS**。

**003 —— 同意合并后超 500。** 既有理由取自带理由夹具 + 新留言使 `merged.length()>500` → 400 且 never save；保留恰 500 允许。不扩 VARCHAR。票：**PASS**。

**004 —— 同意无 scope 异议断言。** 追加 `submitSettlementDispute` → `MERCHANT_SCOPE_REQUIRED` + `merchant_portal.settlement.dispute_failed` + never save。非阻断执行注：`requireMerchantScope` 在 `findSettlementById`/`findContractById` 之后（`:136-140`），追加时须桩结算单与合同，否则先 NPE／NOT_FOUND，锁不到闸。不改处置口径。票：**PASS**。

**005 —— 同意成功审计与 total。** verify 参数与实现 `:96-104` 一致（事件 `merchant_portal.settlement.viewed`、权限 `merchant.portal.settlement.view`、`count`＝页内条数）；`page.total()` 随锁。票：**PASS**。

**006 —— 同意权限 verify。** 至少一条异议成功用例 `verify(authz).requirePermission("merchant.portal.settlement.dispute")`，与 `:116`／V200 同码。票：**PASS**。

**007 —— 同意 translate 锁「原因未填写」。** 见第三节。票：**PASS**。

有无发明业务规则或应升添加：**无。** 001 删的是与 HTML 相反的过时声称，不是删「不看平台全局」。002 不新增商户隔离语义。003 列宽已在 V162／M020-003。004 类注释已写 fail-closed。005/006 锁现行审计／权限码。007 锁已有 translate 整句，不新造报错。本席 OBS-3 另两可异议态若立项会把 HTML 未点名状态写成须测集合——候选正确不升。OBS-4「无 LIMIT」仍只登记。

同文件多 ID 版本铁律只前进一次：JDBC 类现无版本标记，001 加 V0.01；服务测试类已 V0.01，B3 改 003–007 时只升一次。

---

### 五、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M022*`。不 BLOCKED-ASK。

只登记不入队（无应升添加、无应升本批缺陷）：XM-1（改服务不采）、XM-2／席甲 OBS-3、XM-3、本席 OBS-3…8、席甲 OBS-1／OBS-4；席甲 OBS-2 为自审程序说明，已由 003–007 入队落实。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 2 个测试文件，不改生产／迁移／OpenAPI／HTML／锁。

- 001：删除 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`；改写类注释与 `:30`/`:73`；JDBC 类加版本标记 V0.01。`listAllSettlementsPaged` 去留 XM。
- 002：加 `merchantScopeContractsStayIsolated`（两商户合同隔离 + 无绑定 `listContractSpaceIds` 回空）。
- 003：既有理由 + 新留言合并后 >500 → 400 且 never save；保留恰 500。
- 004：无 scope 调 `submitSettlementDispute` → `MERCHANT_SCOPE_REQUIRED` + `dispute_failed` + never save（须桩结算／合同）。
- 005：补 `recordSuccess("merchant_portal.settlement.viewed", … Map.of("count", 1))` 与 `page.total()`。
- 006：异议成功路径 verify `merchant.portal.settlement.dispute`。
- 007：`FieldLabels.translate(e.getMessage())` 等于「原因未填写」；不改 M020 服务。

禁止：改生产 Java／迁移／OpenAPI／HTML、把 001 改成「保留全量分页用例只改注释」、把 007 改成改抛点中文、把 002 写成新业务规则、把 OBS-3 两态升添加、重开 M020-001…006。

### 结论

全批差集无漏并、无原意改写。001 冲突／是／是（采本席删用例）。002 经独立检索：**成立**（JDBC `:288` 与 `listContractSpaceIds` 无 `@SpringBootTest` 真实库用例，改坏 `WHERE merchant_id` 无用例会红）。003–006 为本席 B1 断言缺口，席甲自审「通过」不吞。007 经独立读 `FieldLabels.translate` 与 `GlobalExceptionHandler:61`：**成立**（`translate("reason is required")`＝「原因未填写」；不采 XM-1 改已闭环 M020 服务）。最小写入集合＝本模块 2 测试文件。添加 0。HTML 申请 0。questions 0。非阻断：002「只被 Mockito 引用」对 InMemory default 略宽；007「70+ 处」条数不采。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M022 IN_PROGRESS；B3=0。
