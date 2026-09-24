CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B5-r1 席乙（Grok 4.6/xhigh）修复后整模块三线全量重审 · V0.01

- 席位：审计席乙（独立只读；**未读取** `modules/M022-b5-r1-self-raw.md`；两席本次完整 raw 封存前不互读；可读旧 B1/B2/B4；未派子代理/第三席/MCP；未问人；未修业务/HTML/锁/findings/ledger）
- 阶段：V0.17 阶段 B / B5-r1（B3 累计 1；after tree `ffca9860fcfe31222a407f692190fb9a887c9d81`；整模块重审，不只看 diff，不把 B2/B4 票当事实、不把 findings「全部否」当测试已完备）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`；本模块 2 文件 git blob = after tree（见 §1）
- 计划：`plan/modules.md` V0.04 M022「商户档案、商户类型与商户侧门户／后台验证／第1段」；计划冻结有效内容行 227（B3 前）；本席实算 `git ls-files -z -- <2 paths> | xargs -0 cat | wc -l` = **366**（以实算为准）
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行；sidecar 与 ledger 顶行同值（实算相遇，非抄基准）；`LOCK-AMEND-COUNT` 只读不改
- 映射 22 项：`BSM-001, BSM-003, BSM-114, GEN-001 … GEN-018, PGM-001`（`criteria-lock.md` 映射区 M022 行，未新增判据）
- 被测真实现（核语义，非本模块文件）：`MerchantPortalService.java` V0.01、`MerchantPortalController.java` V0.01、`AuthorizationService.currentMerchantScope`、`IdempotencyService.execute`、`AuditEventService.recordSuccess/recordFailure`、`JdbcContractRepository.listContractsByMerchant/listSettlementsByMerchant/listContractSpaceIds`、`FieldLabels.translate` + `GlobalExceptionHandler` L61
- 背景只作回归清单：B1 双席 FAIL → B2-r1 双 PASS 定稿 M022-001…007 → B3-r1 只改 2 个测试文件 → B4-r1 内容/归因双 PASS。findings 现记 001–007 修复队列「否（已修）」。本席按现行源码/HTML/命令重追。
- 本席结论：**PASS**（2 文件全覆盖、22 项逐项有证据、三线完成、001–007 现行不复现、无新入队工程缺陷）
- 本席未改任何业务文件，未 fetch/commit/push/merge/deploy，未建脚本/日志/临时产物（surefire 为既有 Maven 目标目录；变异未落临时副本，对抗用断言×真实现对照）

---

## 0. 技能预检与节点应用（本环境实核）

按 `160-audit-prompt.md` V0.17 §6 与 `plan/runbook.md` V0.02 §2 在本机路径核验。B5 只借收窄口径；本体要问人/派子代理/改代码的步骤一律作废。用户指令优先于 Arc/superpowers 全量工作流。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 识别 Arc 有 audit 路由；**不执行** Arc 全量审计 | 本体要派评审子代理并写 `docs/arc/audits/`，与「禁子代理、唯一可写 grok-raw」冲突 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 不跑 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `ask`/`grep` 定位 Service/SQL/`currentMerchantScope`/`list*ByMerchant` | 无；本席 5 次合计约省 766,322 tokens |
| `grilling` | mattpocock `productivity/grilling/SKILL.md` | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `grill-me` | 同包 `productivity/grill-me/SKILL.md` | 本体只转发 grilling，按收窄执行 | 无 |
| `batch-grill-me` | — | — | **不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | 同包 `engineering/codebase-design/SKILL.md` | 本模块测试缝：Service 深模块用 Mockito 适配器；SQL 隔离缝在 `JdbcContractRepository` 用 @SpringBootTest 真库 | 无 |
| `diagnosing-bugs` | 同包 | 以两测试类作 red-capable 信号核 001–007 是否回归；密文已脱敏 | 无 |
| `systematic-debugging` | superpowers | 只借读实现→追数据流；作废插桩/改代码 | 无 |
| `verification-before-completion` | superpowers | 声明前实跑两测试类 + sha/wc/git blob | 无 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：平台全量分页用例已删；残留 `never().listAllSettlementsPaged` 是「门户不走该路径」锁，不是守护无调用方方法本身 | 无 |
| `ponytail` | 同包 | B3 默认强度，B5 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 | 节点不适用 |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 / 收 Grok 票 | 节点不适用（本席即外审） |
| `writing-plans` / `brainstorming` | 存在 | 阶段 A | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 可选；只读审计不装 hook | 存在但本节点不用 |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃；结论只留本 raw | 不套到本 raw 以外 |
| Trail of Bits `audit-context-building` | 存在 | 本体强制子代理+落 `audit-context/` | **不用** |

明确不用（提示词已划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、mattpocock `code-review`、`domain-modeling`、`ponytail-audit`。

---

## 1. 范围指纹（实算，非手抄）

```text
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ git rev-parse --abbrev-ref HEAD && git rev-parse HEAD
main
e456ba96c84185c8d39f18f548011daac48a6466
$ git cat-file -t ffca9860fcfe31222a407f692190fb9a887c9d81
tree
$ git ls-tree ffca9860fcfe31222a407f692190fb9a887c9d81 -- <2 files>
100644 blob f69a6734a2b05f6f53ece54e73b0c9edd2bf24e7  .../MerchantPortalJdbcIntegrationTests.java
100644 blob 48db84abbce4f973fd68ae62b7e16251c4f57127  .../MerchantPortalServiceTests.java
$ git hash-object <同上两文件>
f69a6734a2b05f6f53ece54e73b0c9edd2bf24e7
48db84abbce4f973fd68ae62b7e16251c4f57127
$ git ls-files -z -- <2 files> | xargs -0 cat | wc -l
366
```

| 行数 | SHA256（工作区文件内容） | git blob | 路径 |
|---:|---|---|---|
| 74 | `12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef` | `f69a6734a2b05f6f53ece54e73b0c9edd2bf24e7` | `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java` V0.01 |
| 292 | `091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a` | `48db84abbce4f973fd68ae62b7e16251c4f57127` | `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java` V0.02 |
| **366** | （cat 合计） | = after tree | 两文件工作区 = `ffca9860…` |

### 1.1 纳入文件全文覆盖（不抽样）

两文件每一方法均已通读（见下表）。无第三测试文件、无本模块生产文件。

### 1.2 用例清单（12）

`MerchantPortalServiceTests.java`（10）：

1. `merchantSeesOnlyOwnContractsWithSpaceIds` L85–100
2. `callerWithoutMerchantScopeIsRejectedAndAudited` L104–135
3. `merchantSeesOnlyOwnSettlements` L138–152
4. `merchantCanDisputeOwnOpenSettlementWithoutChangingStatus` L155–171
5. `disputeKeepsResolutionTrailAndAppendsToExistingReason` L175–190
6. `disputeReasonBeyondColumnWidthIsRejected` L194–222
7. `disputeWithSameIdempotencyKeyIsProcessedOnce` L226–237
8. `disputeOnForeignSettlementIsForbiddenAndAudited` L241–254
9. `settledOrAlreadyDisputedSettlementCannotBeDisputed` L258–275
10. `disputeReasonIsRequired` L278–291

`MerchantPortalJdbcIntegrationTests.java`（2）：

11. `merchantScopeContractsStayIsolated` L50–60
12. `merchantScopeSettlementsStayIsolated` L63–73

**已删除、本席全文检索确认不在本模块：** `platformAllSettlementsPagedRealTotalAndSecondPageReachable`。`listAllSettlementsPaged` 仅出现在类注释 L22 与服务测试 `never()`（用例 2/3）。

---

## 2. 取证命令与可执行产物

### 2.1 锁哈希

见独占行与 §1。类型 4。

### 2.2 测试实跑（verification-before-completion）

未 `source /etc/complex/complex.env`。Python 只解析该文件三键，子进程 `env` 仅保留 PATH/HOME/USER/LANG/JAVA_HOME/JAVA_TOOL_OPTIONS + 下列三变量；`COMPLEX_DATABASE_URL` **强制**为 `complex_test`（env 文件原 URL 库名是 `complex`，不采用）。未手工写库。

```
EXPORT_URL=jdbc:postgresql://127.0.0.1:5432/complex_test
EXPORT_USER=complex
EXPORT_PASS_SET=yes
mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests,com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests test
```

关键输出：

```
[INFO] Running com.complex.platform.merchantportal.MerchantPortalServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.499 s
[INFO] Running com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
… Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
… Successfully validated 396 migrations
… Current version of schema "public": 428
… Schema "public" is up to date. No migration necessary.
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.822 s
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
=== MVN EXIT 0 ===
```

**绿测不是 GEN-003 通过证据。** 下列逐项对照才是。JDBC 连的是 `complex_test`（Flyway 原文），不是 `complex`。

### 2.3 相关三 HTML（只读，铁律 4）

已对照源文件，不只信 lock 摘录：

- `docs/business-system-map-for-operations-v0.1.html` L505–522（BSM-001）、L563–564（BSM-003）、L1310 / L1322 / L1848–1850（BSM-114 商户侧）
- `docs/platform-governance-map-for-operations-v0.1.html` L256–287（PGM-001 归属/资格/责任 + 零英文）
- `docs/business-enum-quick-reference-v0.1.html`：本模块映射 **零** `BEQR-*`；已打开确认 `td.k` 结构存在，不把未映射锚点当本模块判据

### 2.4 列宽外部规范

`backend/src/main/resources/db/migration/V162__contract_lifecycle.sql:121`：`dispute_reason VARCHAR(500)`。类型 4（读迁移原文）。

---

## 3. 第一性原理三问（逐文件）

### 3.1 MerchantPortalServiceTests.java V0.02（292 行）

**为什么必须存在？删掉哪条 HTML 做不到？**
支撑业务地图 L1310「商户侧不是平台后台…不能看平台全局和其他商户数据」、L1322「我的对账（…有疑问可留言提异议）；我的合同」、入口表 L1848–1850「商户侧页面…不看平台全局」，以及治理地图 L256 段归属/资格/责任与 L287 零英文。删掉本文件，上述门户行为在本模块没有可失败断言。答得上 → 有业务上位（细化，非添加）。

**声称的行为有没有被实际执行验证？**
类注释 L38–41 声称：只见本商户、无商户归属一律 `MERCHANT_SCOPE_REQUIRED`、跨商户 `CROSS_MERCHANT_FORBIDDEN`、已结清/已挂起争议不可再提异议、留言不覆盖平台裁决痕迹、受列宽约束、同幂等键只认一次、成功/失败均落审计。
逐条：用例 1/3 锁权限码+本商户列表+`never` 全量分页；用例 2 锁三入口无 scope（列表两条 + 异议 `submitSettlementDispute`）及失败审计、`never save`；用例 4 锁不改 status + `merchant.portal.settlement.dispute` + 成功审计；用例 5 锁 `resolvedBy/resolvedAt` 与理由 `\n` 合并；用例 6 锁单段 502 拒、合并 300+1+250=551 拒、恰 500 允许；用例 7 真 `IdempotencyService`+内存仓储 `times(1) save`；用例 8 跨商户；用例 9 中文「结算单当前状态不可发起异议」且 `doesNotContain(status)`；用例 10 `FieldLabels.translate("reason is required")` =「原因未填写」。实跑 10/10 绿。类型 2+4。

**哪一条前提未验证就当真？**
- Mockito 默认 `requirePermission` 不抛 → 「无码即拒」无反例；成功路径已 `verify` 三处权限码，删掉调用会红。不把「未做拒绝反例」升级为缺陷（不为理论场景加需求）。
- `currentMerchantScope` 真实现（`AuthorizationService` L425–431）无商户号即 **抛** `MERCHANT_SCOPE_REQUIRED`，从不返回 empty。用例 2 桩 `thenThrow` 与真实现同义；`MerchantPortalService.requireMerchantScope` 的 `orElseThrow` 相对永抛 API 为皮带，首次正常走 throw。
- `deposit_offsetting` / `arrears_collecting` 在允许集合中（Service L32–33），无正向用例。HTML 未点名这两内部状态字。首次正常开账态 `pending_settlement` 已锁。不立项。
- 列宽/跨商户/缺 scope 的用户可见中文：列宽抛点已是中文（Service L152），测试只 `contains("500")`；跨商户/缺 scope 只锁错误码。零英文主锁在用例 9/10（类注释点名的中文路径）。不把其余已是中文的抛点未锁文案升为缺陷。
- 空理由测试调用 `FieldLabels.translate` 而非 `GlobalExceptionHandler` 实例。处理器 L61 即该函数。M022-007 定稿处置就是锁 translate 输出。本席核 `REQUIRED_OR_INVALID` + `p("reason","原因")` →「原因未填写」，与绿测一致。

### 3.2 MerchantPortalJdbcIntegrationTests.java V0.01（74 行）

**为什么必须存在？**
业务地图 L1850「不看平台全局」要求「我的合同/我的对账」在 SQL 谓词上隔离。服务单测把仓储 Mockito 掉，删掉本文件则两条真实 SQL 改坏不红。答得上。

**声称是否被执行验证？**
类注释 L17–22：商户维度读取按谓词隔离；不承载平台全量分页（M022-001）；`listAllSettlementsPaged` 无生产调用方。
- 用例 11：两商户各存合同，`listContractsByMerchant` `containsExactly` 本商户 id。对应 Jdbc L286–289 `WHERE merchant_id = :mid`。
- 用例 12：两商户各存结算，`listSettlementsByMerchant("mch_mp_c")` `containsExactly("stl_mp_c")`。对应 Jdbc L293–302 `JOIN contracts … WHERE c.merchant_id = :mid`。
- 无 `platformAllSettlementsPaged*` 方法。graft grep 生产调用方：`listAllSettlementsPaged` 仅仓储接口 default + Jdbc 覆写 + 合同域测试，门户服务 0 调用。
- `listContractSpaceIds("ctr_mp_e")` 只断言未绑定回空（L59）。正例绑定未插入。见 §5 Q。

**未验证前提：**
- 结算隔离只断言一侧。SQL 去掉谓词会返回两单，`containsExactly` 仍红。红-capable，不立项。
- `listContractSpaceIds` 无「有绑定合同只回本契约铺位」正例。方法按 `contract_id` 而非 `merchant_id` 过滤（Jdbc L481–484）；门户只对已按商户过滤的合同调它。类注释把该方法与商户隔离并写，略宽。不把「空列表」当成铺位隔离已完备，亦不为此加需求。

---

## 4. 顺从式：22 项逐条适用证据

每条：适用性 → 本模块文件证据 → 结论。不新增判据。

### BSM-001 — 整体说明：这套系统到底管什么
- 适用（全局业务实现；本模块是「谁在经营 / 签了什么合同 / 对账结算」的测试缝）。
- 证据：HTML L505–522；用例 1/3/11/12 锁合同与结算只见本商户。账单/工单/费用总表不在本模块两文件。
- 结论：不冲突、不遗漏本模块职责。

### BSM-003 — 系统通用能力：数据可改可删、操作有引导、权限分岗
- 适用（权限分岗）。
- 证据：HTML L563–564；用例 1 `contract.view`；用例 3 `merchant.portal.settlement.view`；用例 4 `merchant.portal.settlement.dispute`。
- 结论：三入口权限码均有 verify。

### BSM-114 — 各类入口：不同人只看自己该看的、只做自己该做的
- 适用（标签含 merchantportal）。
- 证据：HTML L1848–1850「商户侧页面…不看平台全局」、L1310「不能看平台全局和其他商户数据」、L1322 我的合同/我的对账可留言。用例 2 无归属 fail-closed；用例 8 跨商户；用例 11/12 真 SQL 隔离；类注释不再宣称平台全量。
- 结论：与 HTML 一致。原冲突（全量分页用例）已不在文件中。

### PGM-001 — 整体定位：平台治理层不是一个业务部门，而是所有业务的底座
- 适用（归属/资格/责任 + 零英文）。
- 证据：HTML L256–270、L287。用例 4/5 不改状态、保留裁决人；用例 9 中文报错不回显内部状态；用例 10 出网「原因未填写」。权限码/内部状态码属「系统内部标识符」例外。
- 结论：本模块声称的用户可见中文路径已锁。

### GEN-001 — 必要性与最小性
- 适用。12 个用例均有上位。已删除对现行门户无需求承载的全量分页用例。`never().listAllSettlementsPaged` 锁的是「门户不调用」，不是给无生产调用方方法当 SUT。
- 结论：无多余不可达测试方法。

### GEN-002 — 可执行正确性
- 适用。`mvn test` 两班 12 绿，JDBC 连 `complex_test`。类注释/方法名与断言一致。
- 结论：通过。

### GEN-003 — 测试有效性（核心）
- 适用。按「真实现语义 / 桩是否不可能 / 注释可否复现 / 改坏是否红」审查（§6）。001–007 对应缺口在现行断言下，把被测关键行为改坏会红（见 §6）。绿测本身不替代该表。
- 结论：通过；不把「全部否」当成无需再找缺口。本轮未发现新的「改坏仍绿」工程缺陷。

### GEN-004 — 信任边界校验
- 适用（窄）。空理由 / 超宽 / `requireText(settlement_id)`。空理由用例 10；超宽含合并用例 6。缺 id / 结算不存在 / `request==null` 无用例——非类注释声称的首次正常主路径，作观察不立项。

### GEN-005 — 授权与最小权限
- 适用。三权限码 verify；无 scope 三入口 fail-closed；跨商户 forbidden。桩 throw 对齐 `currentMerchantScope` 真实现（L425–431 无商户号即抛）。

### GEN-006 — 数据完整性
- 适用。异议不改 status（用例 4/5）；合并理由（用例 5）；列宽 500 含合并（用例 6）；V162 `VARCHAR(500)`。JDBC 本类不跑异议 upsert（属仓储/合同域）。

### GEN-007 — 并发与幂等
- 适用。用例 7 真 `IdempotencyService.execute`（L57–65）+ `InMemoryIdempotencyRepository`，同键 `times(1) save` 与成功审计一次。未测「同键不同 body」冲突——非类注释首次正常，不立项。

### GEN-008 — 接口与模式一致性
- 适用。错误码 `MERCHANT_SCOPE_REQUIRED` / `CROSS_MERCHANT_FORBIDDEN` / `SETTLEMENT_NOT_DISPUTABLE` / `VALIDATION_FAILED` 与 Service 抛点一致。权限码字符串与 Service L60/L91/L116 一致。审计事件名与 Service L68–80 / L96–108 / L159–166 / L124–126 一致。

### GEN-009 — 错误处理与恢复
- 适用。失败路径 `recordFailure`：用例 2/8/10。成功不伪造成功。`assertThatThrownBy` 覆盖 rethrow。`SETTLEMENT_NOT_DISPUTABLE` 与 catch-all 同一 `catch (RuntimeException)`，删掉 catch 内 recordFailure 会被用例 2/8/10 抓住。

### GEN-010 — 安全与敏感信息
- 适用。跨商户拒绝+不写库。JDBC 属性写死 `complex_test`，密码走 `${COMPLEX_DATABASE_PASSWORD:…}` 回退词是测试夹具默认，非生产秘密出网。本席实跑未打印口令。

### GEN-011 — 可观测与审计证据
- 适用。合同列表成功审计用例 1；结算列表成功审计用例 3（`merchant_portal.settlement.viewed` + `total`）；异议成功用例 4；失败用例 2/8/10。

### GEN-012 — 资源与性能边界
- 适用（窄）。测试入参 page=1 size=500 与 Controller 默认一致。Jdbc 商户查询无 LIMIT、靠商户谓词有界——本模块 JDBC 用例不锁分页切片。内存切片属 Service，单测用 1 条夹具。不立项。

### GEN-013 — 构建配置与依赖
- 适用。`@SpringBootTest` 三属性钉 `complex_test`；本席只导出三变量实跑 EXIT 0。不依赖未声明本机 source。

### GEN-014 — 数据库迁移正确性
- 本模块无迁移文件。适用性：仅引用 V162 列宽作为用例 6 的外部规范原文。不审迁移模块。

### GEN-015 — 界面可用性与可访问性
- 适用（文案语言，无 UI 文件）。用例 9 锁中文报错；用例 10 锁出网中文。

### GEN-016 — 文档可验证性
- 适用。JDBC 类注释 L17–22 与现行门户一致（不再写平台全量）。服务类注释 L38–41 与 10 个用例可复现。方法注释「合并后超过列宽」（L192）可由用例 6 第二段复现（既有 300 字 + 新 250 字）。`listContractSpaceIds` 并入「只回本商户」略宽，见 §3.2，不构成不可复现的完成声明。

### GEN-017 — 业务来源分类
- 适用。本模块内容按 冲突→细化→技术性→添加：无与 HTML 明文矛盾的残留用例；测试是 BSM-114/PGM-001 下位实现细节（细化）；Mockito/`@Transactional`/Spring 属性为技术性、无业务发明。无添加项，不发 2.2。

### GEN-018 — 第一性原理证据
- 适用。本报告每条落到 HTML 原文 / 可执行产物 / 迁移原文 / 命令输出。不把 B1/B4 结论、注释或绿测单独当依据。

---

## 5. 对抗式 grilling（自问自答，不等操作员）

❓ **Q1 — 商户隔离两条真实 SQL 改坏是否红？**
➡️ 是。`listContractsByMerchant` 去掉 `WHERE merchant_id = :mid`：用例 11 `containsExactly` 失败（两商户交叉）。`listSettlementsByMerchant` 去掉 JOIN 谓词：用例 12 `containsExactly("stl_mp_c")` 失败。

❓ **Q2 — scope 闸三入口改坏是否红？**
➡️ 是。`requireMerchantScope` 从 `listMyContracts` / `listMySettlements` / `disputeOnce` 任一删除：用例 2 对应 `assertThatThrownBy` 或 `never() list/save` 失败。桩与真实现同为 throw。

❓ **Q3 — 权限码三处删掉是否红？**
➡️ 是。删 L60/L91/L116 的 `requirePermission`：用例 1/3/4 的 `verify(authz).requirePermission(...)` 失败。

❓ **Q4 — 成功与失败审计删掉是否红？**
➡️ 成功：删 `recordSuccess` 合同/结算/异议 → 用例 1/3/4 失败。失败：删 catch 内 `recordFailure` → 用例 2/8/10 失败。

❓ **Q5 — 异议改状态是否红？**
➡️ 是。`saveSettlement` 写入非 `pending_settlement`：用例 4 `assertThat(disputed.status())` 与 captor 失败。用例 5 同样锁 status。

❓ **Q6 — 裁决痕迹清空或覆盖理由是否红？**
➡️ 是。用例 5 断言 `resolvedBy=usr_manager`、`resolvedAt` 原值、理由为「平台：…\n商户：…」。

❓ **Q7 — 合并后列宽改成只量新留言是否红？**
➡️ 是。`merged.length() > 500` 改 `reason.length() > 500`：第二段既有 300 + 换行 + 新 250=551，新留言单独 250 不超，会走 save，随后 `never().saveSettlement` 失败。单段 502 仍拒，不能单独当合并锁。

❓ **Q8 — 同键执行两次是否红？**
➡️ 是。用例 7 `times(1) save` 与 `times(1) recordSuccess`。

❓ **Q9 — 跨商户放行是否红？**
➡️ 是。用例 8 期望 `CROSS_MERCHANT_FORBIDDEN` 且 never save + dispute_failed。

❓ **Q10 — 不可异议中文文案改成英文或回显状态码是否红？**
➡️ 是。用例 9 `isEqualTo("结算单当前状态不可发起异议").doesNotContain(status)`，对 `settled/disputed/voided` 三态。

❓ **Q11 — 空理由出网中文改坏是否红？**
➡️ 是。`translate` 不再映射到「原因未填写」（例如改 FieldLabels 或服务改成别的英文键）：用例 10 失败。处理器 L61 与测试调用同一函数。

❓ **Q12 — 类注释声称是否都可复现？**
➡️ 服务类注释 L38–41：是（§3.1）。JDBC 类注释 L17–22：隔离与「不承载全量」可复现；`listContractSpaceIds` 仅空列表，正例未插绑定——过宽并写，不构成「平台全量」那种与 HTML 冲突的完成声明。

❓ **Q13 — 是否还有守护无生产调用方路径的用例？**
➡️ 没有以 `listAllSettlementsPaged` 为 SUT 的用例。残留 `never()` 守护的是门户 **不调用** 该路径（有生产调用方的是 `listMySettlements` → `listSettlementsByMerchant`）。graft：门户服务 0 处调用 `listAllSettlementsPaged`；`ContractService` 调的是 `listContractsPaged`（平台合同域，非本模块）。

❓ **Q14 — `deposit_offsetting`/`arrears_collecting` 移出可异议集合是否红？**
➡️ 否。HTML 未点名这两内部状态。不为理论场景补需求。观察，不立项。

---

## 6. 用例×真实现对照（改坏是否红）

| # | 用例 | 真实现 | 改坏是否红 | 备注 |
|---|---|---|---|---|
| 1 | 本商户合同+铺位+view 权限+成功审计 | Service L58–77；View.of | 能 | `never listContractsPaged` |
| 2 | 无 scope 三入口拒+失败审计+never 读/写 | Authz L425–431 抛；Service L78–81/L106–109/L123–127/L140 | 能 | 含异议入口 |
| 3 | 本商户结算+view 权限+成功审计+total | Service L89–105 | 能 | `never listAllSettlementsPaged` |
| 4 | 异议不改状态+dispute 权限+成功审计 | Service L113–168 | 能 | |
| 5 | 裁决痕迹保留+理由合并 | Service L148–158 | 能 | |
| 6 | 列宽 500：单段/合并/恰 500 | Service L151–153；V162 | 能：含合并 | |
| 7 | 同幂等键一次 | IdempotencyService L57+ | 能 | 真服务非 stub |
| 8 | 跨商户 | Service L140–142 | 能 | |
| 9 | 不可异议中文 | Service L144–146 | 能 | |
| 10 | 空理由出网中文 | Service L132–134；FieldLabels L511–524 + p("reason")；Handler L61 | 能 | |
| 11 | 合同真 SQL 隔离 | Jdbc L286–289 | 能 | 两商户双向 |
| 12 | 结算真 SQL 隔离 | Jdbc L293–302 | 能 | 一侧断言仍红-capable |
| — | （已删）全量分页 | Jdbc L306–319 无门户调用方 | 无用例当 SUT | 符合 HTML「不看平台全局」 |

---

## 7. M022-001…007 回归核（不当成已完备，独立重追）

只核现行文件是否仍能抓住原缺陷；不复用 B2 票文当证据。

| ID | 原缺口（独立重述） | 现行锁 | 改坏仍绿？ | 回归？ |
|---|---|---|---|---|
| 001 | JDBC 用例把 `listAllSettlementsPaged` 当商户专区行为，与 HTML「不看平台全局」冲突 | 该方法已不存在；类注释 L21–22 写明不承载；门户 `never()` | 把全量用例加回才是新添加。现行无该 SUT | 否 |
| 002 | `listContractsByMerchant` 无真库隔离用例 | 用例 11 两商户 `containsExactly` | 去掉 SQL 谓词会红 | 否 |
| 003 | 列宽只量新留言，注释「合并后」不可复现 | 用例 6 第二段既有 300+新 250 | 改 `reason.length()` 会红 | 否 |
| 004 | 异议入口无 scope 闸用例 | 用例 2 L124–134 | 删 `disputeOnce` 内 `requireMerchantScope` 会红 | 否 |
| 005 | 结算列表成功审计未锁 | 用例 3 L148–150 + `page.total()` | 删 `recordSuccess(viewed)` 会红 | 否 |
| 006 | 异议未 verify 权限码 | 用例 4 L162 | 删 L116 `requirePermission` 会红 | 否 |
| 007 | 空理由只锁错误码，不锁出网中文 | 用例 10 L286「原因未填写」 | 翻译结果改掉会红 | 否 |

---

## 8. 四分类与临时 findings

审完全部 12 用例、22 判据、grilling 13 问后：

- **冲突：** 无（原 001 所涉全量分页用例与过时类注释已不在文件中）。
- **细化：** 全部现行测试为 BSM-114 / PGM-001 / BSM-001 下位，无工程缺陷入队。
- **技术性：** Spring/Mockito/`@Transactional` 无业务发明。
- **添加：** 无。

**临时 findings：无。** 不分配 GR 编号。

观察（不入队，避免为理论场景加需求）：

- JDBC `listContractSpaceIds` 仅空列表；正例绑定未插。
- 可异议集合另两内部状态无正向用例（HTML 未点名）。
- 列宽/跨商户/缺 scope 用户文案未像用例 9/10 那样逐字锁中文（抛点已是中文或只锁码）。
- `requireMerchantScope.orElseThrow` 相对永抛的 `currentMerchantScope` 为皮带。

ponytail-review：`Lean already. Ship.` 净可删行：0（全量分页用例已在 B3 删除）。

---

## 9. 最终结论

- 覆盖：M022 2/2 文件全文；12/12 用例；22/22 映射判据。
- 三线：第一性原理三问、顺从 22 项、grilling 对抗均已落盘。
- 证据类型：HTML 原文（1）、可执行产物（2）、迁移原文（3）、命令输出（4）均有。
- 001–007：现行不复现。
- 新工程缺陷：无。
- questions：无。
- 本席步骤结论：**PASS**

（CLI 不回带本正文或机器标记。）
