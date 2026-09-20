CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B5-r1 席乙（Grok 4.6/xhigh）修复后整模块三线全量重审 · V0.01

- 席位：审计席乙（独立只读；**未读取** `modules/M020-b5-r1-self-raw.md`；两席本次完整 raw 封存前不互读；可读旧 B1/B4；未派子代理/第三席/MCP；未问人；未修业务/HTML/锁/findings/ledger）
- 阶段：V0.17 阶段 B / B5-r1（B3 累计 1；after tree `d063773a67499e1b19fb8d2a46b1ee0324a9acbf`；整模块重审，不只看 diff，不复用 B2/B4 票当事实）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`；本模块 4 文件工作区相对 HEAD 有未提交修复（Controller/Service），`git diff --stat d063773a -- <4 files>` 空 → **审计对象 = after tree = 当前工作区**
- 计划：`plan/modules.md` V0.04 M020「商户档案、商户类型与商户侧门户／后台实现／第1段」；计划冻结有效内容行 242（B3 前）；本席实算 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **284**（以实算为准）
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行；sidecar `plan/criteria-lock.sha256` 同值；`ledger.md` 顶行同值；`LOCK-AMEND-COUNT: 1`（只读，不改）
- 映射 22 项：BSM-001、BSM-003、BSM-114、GEN-001…GEN-018、PGM-001（未新增判据）
- 背景只作回归清单、不作证据：B1 双席 FAIL → B2-r1 双 PASS 定稿 M020-001…006（007 XM 否）→ B3-r1 修本模块 2 文件 + M022 测试类 → B4-r1 双 PASS；findings V2.64 记 001–006 否（已修）、007 否（XM M392/M021）、operator_note 只登记。本席按现行源码/HTML/命令重追，**不把「全部否」当功能全实现**
- 结论：**PASS**（本模块 4 文件无新入队工程缺陷；001–006 现行已不复现；007/operator_note/OpenAPI/V200/前端表单/JDBC 测试类仍为跨模块观察，不入本模块队列）
- 本席未改任何业务文件，未 fetch/commit/push/merge/deploy，未建脚本/日志/临时产物（surefire 为既有 Maven 目标目录）

---

## 0. 技能预检与节点应用（本环境实核）

按 `160-audit-prompt.md` V0.17 §6 与 `plan/runbook.md` V0.02 §2 在本机路径核验，不抄 Codex 预检表。B5 只借收窄口径；本体要问人/派子代理/改代码的步骤一律作废。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别 audit；**不执行** Arc 全量审计工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，与「禁子代理、唯一可写 grok-raw」冲突；`references/platform-tools.md` **不存在** |
| Arc `audit` | howells `skills/audit/SKILL.md` | 不跑 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | grep/skeleton 定位 `currentMerchantScope`、`listSettlementsByMerchant`、`IdempotencyService` | 无；本席 graft 约省 ~695k tokens（4 次） |
| `grilling` | mattpocock `productivity/grilling/SKILL.md` | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `grill-me` | 同包 `productivity/grill-me/SKILL.md` | 本体只转发 `/grilling`，按 grilling 收窄执行 | 无 |
| `batch-grill-me` | — | — | **不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | 同包 `engineering/codebase-design/SKILL.md` | Controller 浅适配器、Service 深模块；幂等/审计/仓储为既有 seam，未再发明执行器 | 无 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 用现行源码+单测作 red-capable 信号核 001–006 是否回归；JDBC 三变量未导出故不跑库测 | JDBC 未跑，不伪称已跑 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流；作废插桩/改代码 | 无 |
| `verification-before-completion` | superpowers | PASS 前实跑 `MerchantPortalServiceTests` 与 sha/wc/git | JDBC 因三变量缺失未跑 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：`operatorNote` 仍未消费（已只登记）；`orElseThrow` 相对永抛的 `currentMerchantScope` 为皮带，不单独立项 | 无 |
| `ponytail` | 同包 | B3 默认强度，B5 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 | 节点不适用 |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 | 节点不适用 |
| `writing-plans` / `brainstorming` | 存在 | 阶段 A | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | — | 可选 | **本环境未核到安装态**；只读审计不装 hook |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃；raw 首行按提示词必须是锁哈希 | 不套到本 raw 以外 |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。

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
$ git cat-file -t d063773a67499e1b19fb8d2a46b1ee0324a9acbf
tree
$ git diff --stat d063773a67499e1b19fb8d2a46b1ee0324a9acbf -- <4 M020 files>
# 空
$ git ls-files -z -- <4 files> | xargs -0 cat | wc -l
284
$ env | grep -E '^COMPLEX_DATABASE_' || echo NO_COMPLEX_DATABASE_VARS
NO_COMPLEX_DATABASE_VARS
```

| 行数 | SHA256（工作区文件内容） | 路径 |
|---:|---|---|
| 40 | `3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java` |
| 55 | `536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java` V0.01 |
| 180 | `64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java` V0.01 |
| 9 | `c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052` | `backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java` |
| **284** | （cat 合计） | View/DTO 与 B1 内容哈希相同；Controller/Service 已变 |

`git status --porcelain`：上述 Controller/Service 及 M022 `MerchantPortalServiceTests.java` 为已跟踪修改。View 与 DTO 干净。

只读追依赖（不纳入本模块队列；缺陷只记跨模块观察）：`AuthorizationService.requirePermission` / `currentMerchantScope` / `currentMerchantIdQuietly` / `isPlatformAdmin` / `CallerScopeResolver.resolve`；`ContractRepository` default 与 `JdbcContractRepository` 覆写；`ContractSettlementRecord` 字段序；`ContractService.settle` / `resolveDispute` / `completeSettlement` / `advanceSettlement`；`AuditEventService.recordSuccess`/`recordFailure` + `JdbcAuditEventRepository.save`；`IdempotencyService.execute`；`ApiRequests.requireIdempotencyKey`；`FieldLabels.translate` + `GlobalExceptionHandler.handleApiException`；`PageResponse.of`；迁移 V162/V163/V171/V307/V200；`docs/openapi/openapi-p0.yaml` 三条 `/api/v1/merchant-portal/*`；`apps/web` 两页；M022 两测试类。

---

## 2. 四文件覆盖（全文已读）

### 2.1 MerchantContractView.java（L1–40）

| 段 | 行 | 现行 |
|---|---|---|
| record 字段 | 12–24 | id/merchantId/effectiveDate/expiryDate/lifecycleStatus/depositCent/projectId/spaceIds/renewedFromContractId/createdAt/updatedAt |
| 工厂 | 25–38 | `of(ContractRecord, List<String> spaceIds)`；`spaceIds == null ? List.of() : List.copyOf(spaceIds)` |
| 披露 | 8–10 注释 | 「最小披露——不含平台侧审批链、收益固化快照、报表口径」 |

无写路径、无授权、无状态机。

### 2.2 MerchantPortalController.java V0.01（L1–55）

| 段 | 行 | 现行 |
|---|---|---|
| 类注释 | 17–21 | `/api/v1/merchant-portal/*`；账单/工单复用既有域端点；此处承载合同与结算的商户口径 |
| 路由 | 22–30 | `@RequestMapping("/api/v1/merchant-portal")` 注入 Service |
| GET contracts | 32–37 | `page`/`size` 默认 1/500 → `service.listMyContracts` |
| GET settlements | 39–44 | 同上 → `listMySettlements` |
| POST dispute | 46–54 | `@Valid` + `ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey())` 后 `submitSettlementDispute` |

原句 L51–52：「M020-004:幂等键校验走共享助手(VALIDATION_FAILED),与全仓其它控制器一致。」

### 2.3 MerchantPortalService.java V0.01（L1–180）

| 段 | 行 | 现行 |
|---|---|---|
| 类注释 | 21–26 | 「currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律 fail-closed 抛 MERCHANT_SCOPE_REQUIRED」；「异议只留言、不改结算状态、不暂停付款义务」；「同一幂等键只认一次」；「三个入口成功/失败均落审计」 |
| DISPUTABLE | 31–33 | `pending_settlement` / `deposit_offsetting` / `arrears_collecting` |
| 列宽 | 34–35 | `DISPUTE_REASON_MAX_LENGTH = 500`（V162） |
| listMyContracts | 58–83 | `requirePermission("contract.view")` → `requireMerchantScope()` → `listContractsByMerchant` + 内存 `PageResponse.of` → 空间 ID → `recordSuccess(..., Map.of("count", views.size()))`；catch `recordFailure(... view_failed ...)` |
| listMySettlements | 89–111 | `merchant.portal.settlement.view` + `listSettlementsByMerchant`；从不调用 `listAllSettlementsPaged` |
| submitSettlementDispute | 114–128 | 权限 `merchant.portal.settlement.dispute`；空请求 400「请求内容不能为空」；`idempotencyService.execute("merchantPortalSettlementDispute:" + id, key, request, () -> disputeOnce, ContractSettlementRecord.class)`；catch `recordFailure(... dispute_failed ...)` |
| disputeOnce | 131–169 | `trimToNull(reason)` 空则 `"reason is required"`；按 id 取结算/合同；跨商户 403；状态不在 DISPUTABLE → 固定中文「结算单当前状态不可发起异议」；合并既有 `disputeReason`+换行+留言，>500 →「异议留言不能超过 500 字」；`saveSettlement` **保持** `status`/`resolvedBy`/`resolvedAt`；成功审计 metadata `merchant_id`+`reason` |
| requireMerchantScope | 171–175 | `currentMerchantScope().orElseThrow(MERCHANT_SCOPE_REQUIRED)` |

### 2.4 MerchantSettlementDisputeRequest.java（L1–9）

```
idempotencyKey, operatorNote, reason
```

服务只读 `idempotencyKey`/`reason`；`operatorNote` 仍未写入任何列（与定稿「只登记」一致，不重开队列）。

---

## 3. 三线

### 3.1 第一性原理三问

**Q1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

- GET 合同/结算：删掉则 BSM-114 商户侧页面「对账、结算…不看平台全局」与业务地图 L1322「我的对账」「我的合同」无本前缀只读入口。上位可指认。
- POST 异议留言：删掉则 L1322「有疑问可留言提异议」无本模块写口（前端现行无表单属 M023，不使后端端点无需求）。
- 答得上 → 有业务上位；不是无主代码。

**Q2 它声称的行为有没有被实际执行验证过？证据在哪？**

- 声称 fail-closed / 只见本商户 / 不改状态 / 同键一次 / 失败审计 / 中文状态报错 / 列宽 500：`MerchantPortalServiceTests` **本席实跑** `mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests test` → **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**（surefire txt，0.488 s）。测试文件属 M022，按真实现判断有效性，不以「绿」替代源码对照。
- JDBC `MerchantPortalJdbcIntegrationTests`：**未跑**。`COMPLEX_DATABASE_URL/USERNAME/PASSWORD` 均未导出；禁止 `source /etc/complex/complex.env`。且该类测的是仓储 `listAllSettlementsPaged` / `listSettlementsByMerchant`，**不调用** `MerchantPortalService`。
- HTTP 出网中文：抛点 `"reason is required"` 经 `GlobalExceptionHandler` L56–61 `FieldLabels.translate`；`p("reason","原因")` + `REQUIRED_OR_INVALID` → 「原因未填写」。`FieldLabelsTests` 对同形态 `"order_id is required"` 有断言。本席未另起 HTTP 进程。

**Q3 它依赖的前提，哪一条是没验证过就当真的？**

- 「`currentMerchantScope` 对平台管理员返回 empty」——**假**。`AuthorizationService.java:425–431` 无 merchantId **抛** `MERCHANT_SCOPE_REQUIRED`，永不 `Optional.empty()`。`orElseThrow` 对生产实现不可达，fail-closed 由抛错本身完成。
- 「OpenAPI 全量监督 / 挂起有争议仍是本模块行为」——**假**。代码无全量分支、不改 status。契约文本属 M392（007 XM），不以对齐 yaml 为由改回 `disputed`。
- 「收集幂等键即幂等」——现行已走 `IdempotencyService.execute`（见 3.2 GEN-007）。
- 「单测 10/10 证明 JDBC 分页/列宽」——**假**；列宽在单测用字符串长度拦截，未打库。
- 「成功审计在失败时仍会留下」——生产 `IdempotencyService` L166–172 把 `actionAndComplete`（含 `saveSettlement`+`recordSuccess`+`complete`）包进 `TransactionTemplate`；`AuditEventService.record` 无 `REQUIRES_NEW`，`JdbcAuditEventRepository` 用同一 `NamedParameterJdbcTemplate`，成功审计随写路径回滚；失败审计在 `execute` 外层 catch（L123–127）于回滚之后另插。单测 `new IdempotencyService(...)` **不注入** TM，不能当生产事务证据。

### 3.2 顺从式（22 项逐条；全部适用/证据均记录）

相关 HTML 已对照原文：业务地图 L505 BSM-001、L563 BSM-003 导语、L1322 商户专区自助、L1351 退租有争议挂起、L1827/L1848–1850 BSM-114 商户侧行；治理地图 L256 PGM-001（含全中文报错、重复提交只认一次、付款/同时操作原则）。锁内定义全文已读。BEQR 未映射，不扩审。

| ID | 适用结论 | 证据 |
|---|---|---|
| BSM-001 | 适用；本切片落实「谁在经营 / 签了什么合同 / 对账、结算」中的商户自助读+留言 | 四文件只暴露合同摘要与退租/到期结算单；未写死税率等策略参数。HTML L505–522 原文与锁一致 |
| BSM-003 | 适用导语「权限分岗」；子条 BSM-004…007 **未映射、不扩审** | 三权限码分岗。无存档删除/分步新建/动态码于本四文件 |
| BSM-114 | 适用；商户侧「对账、结算、不看平台全局」 | 有 scope 时 `list*ByMerchant`；无 scope 403；跨商户 403。留言不改状态对齐 L1322「付款义务不暂停」。OpenAPI「全量监督」仍相反 → 观察/007，非本四文件缺陷 |
| GEN-001 | 适用 | GET 两入口+POST 留言有 HTML 上位。无参 list overload 为转调。`operatorNote` 未读：已只登记，不重复入队。`orElseGet` 全量分支**已不存在** |
| GEN-002 | 适用；本四文件声称与现行一致 | 类注释 L23–26 与 `currentMerchantScope` 抛错、只留言不改状态、幂等 execute、失败审计一致。OpenAPI 仍过时 → XM 007 |
| GEN-003 | 适用（测试属 M022） | 10 个单测覆盖：本商户可见、无 scope 拒并失败审计、不改 status、裁决痕迹保留、列宽、同键一次、跨商户失败审计、settled/disputed/voided 中文报错、reason 必填。桩 `thenThrow(SCOPE_REQUIRED)` 与生产一致，不再桩 empty。JDBC 类不测 Service、本席未跑库 |
| GEN-004 | 适用 | `settlement_id` `requireText`；reason trim 后空拒绝；合并后 >500 拒绝且 `never().saveSettlement`。无数字异议内容审核——HTML 未要求本 API 解析「漏算/少算」，不发明规则 |
| GEN-005 | 适用；fail-closed | `requirePermission` 在取数前；`currentMerchantScope` 无商户即 403，不因 `isPlatformAdmin` 旁路（本模块零调用 isPlatformAdmin）。V200 把 portal 码授给 `role_platform_admin` 仍过不了 scope（M021 观察） |
| GEN-006 | 适用 | 异议只 UPSERT `contract_settlements` 一行；保留 `resolvedBy`/`resolvedAt`；合并 reason。`saveSettlement` L249–251 覆写这三列，现行传入旧裁决字段，不再清空。不改 status，不破坏待结算约束 |
| GEN-007 | 适用 | `idempotencyService.execute`；同键+同 hash replay，测试 `times(1).saveSettlement`。异键再跑会再 append（不同操作，非「同一操作重试」）。状态推进后同键 replay 旧成功体、不再写库，符合「只认一次」 |
| GEN-008 | 适用；本模块代码与 HTML 一致，与 OpenAPI 仍不一致 | yaml L20386/20409「平台管理员为全量监督口径」、L20432「挂起为有争议」、L20472 reason maxLength 2000 vs VARCHAR(500)——归 007 XM，本 B5 不为对齐 yaml 改代码。Jackson `SNAKE_CASE`（application.yml）对齐 DTO 驼峰↔`idempotency_key` |
| GEN-009 | 适用 | 超长 → 400 中文；不可异议 → 固定中文、测试 `doesNotContain(status)`。`"reason is required"` 出网经 FieldLabels，不另立项。非 ApiException 的 `codeOf` 只进失败审计 error_code |
| GEN-010 | 适用 | 命名参数 SQL。跨商户先查后 403：猜 UUID 非首次正常，不立项。审计 metadata 含 reason 明文（对账异议，非秘密） |
| GEN-011 | 适用 | 三入口 success+failure 事件名与 OpenAPI x-audit-* 一致。失败 catch 再抛。成功审计在幂等事务内，失败审计在事务外。`AuditEventService` L117 终端硬编码 `"web"`——依赖观察，本模块未设终端字段 |
| GEN-012 | 适用；首次正常不立项 | 商户路径先按 merchant 全取再内存切片；一商户退租单通常有界。`PageResponse.of` 不封顶 size（仓储全量页有 500 帽）。页内 `listContractSpaceIds` N+1 记观察 |
| GEN-013 | 不适用本四文件 | 无构建/依赖/CI 内容 |
| GEN-014 | 不适用本四文件（无迁移） | 只读对齐：V162 `dispute_reason VARCHAR(500)` + 初态 CHECK；V163 五态；V171 列宽；V307 加 `voided`。DISPUTABLE 不含 `voided`/`disputed`/`settled`，与 CHECK 兼容 |
| GEN-015 | 不适用本四文件 | 无 UI。前端中文标签属 M023。API 报错出口在 FieldLabels |
| GEN-016 | 适用 | 本四文件类注释已改为 fail-closed/只留言，不再把「平台全量」写成当前事实。OpenAPI/V200 注释仍过时 → 007/M021 |
| GEN-017 | 适用 | 本席全部条目按冲突→细化→技术性→添加；无把业务发明标成技术性；无新添加项；operator_note 维持只登记 |
| GEN-018 | 适用 | 结论落到 HTML 原文、Java 原文、mvn/surefire/sha256/git；不以 B4 PASS、findings「否」或席位意见为据 |
| PGM-001 | 适用整体定位+全局原则 | 归属按合同 merchantId。资格：权限+scope。责任：成败审计。重复提交只认一次：IdempotencyService。付款不暂停：不改 status，`completeSettlement` 仍接三态。报错：状态句已中文；`reason is required` 出口翻译。金额本切片不计算 |

### 3.3 对抗式（grilling 结构，自问自答，不等操作员）

用户指定九问全部作答；事实自读，不派子代理。

❓ **Q1 — 无商户账号 / 平台管理员调用会怎样？**  
➡️ `requirePermission`：平台管理员对 medium 日常码（V200 风险 medium）可通过。随后 `currentMerchantScope` **抛** 403 `MERCHANT_SCOPE_REQUIRED`「此操作必须在商户范围内进行」。GET 两入口在 `requireMerchantScope` 处倒下，测试 `callerWithoutMerchantScopeIsRejectedAndAudited` 断言不触 `list*ByMerchant`/`list*Paged`，并落 `*_view_failed`。POST 在 `disputeOnce` 内同样抛，落 `dispute_failed`。**不是全量监督入口。** 红线④ / BSM-114「不看平台全局」。

❓ **Q2 — 平台裁决后商户留言会怎样？**  
➡️ `ContractService.resolveDispute` L578–587：仅当 `status=="disputed"` 才裁决，写回 `pending_settlement`，保留 `disputeReason`，写入 `resolvedBy`/`resolvedAt`。此后状态落入 DISPUTABLE。`disputeOnce` 合并「既有理由\\n商户留言」，**不清空**裁决人/时间。测试 `disputeKeepsResolutionTrailAndAppendsToExistingReason`。裁决前仍为 `disputed` 时商户入口 409（测试含 `disputed`）——与「已有争议拒绝」的状态口径一致；不为「争议中再留言」加需求。

❓ **Q3 — 同键重试 / 不同键重试 / 状态被推进后重试会怎样？**  
➡️ 同键同 body：`tryReserve` 失败 → `replay` 返回已存 `ContractSettlementRecord`，`saveSettlement` 一次（测试 `disputeWithSameIdempotencyKeyIsProcessedOnce`）。不同键：新 operation 再跑 `disputeOnce`，再 append（不同操作）。推进到 `settled` 后**同键**：replay 旧成功，不再写。推进后**新键**：`SETTLEMENT_NOT_DISPUTABLE`。首次失败标 `FAILED_RETRYABLE` 后同 hash 可再执行。

❓ **Q4 — 超长留言？**  
➡️ 合并后 >500：400「异议留言不能超过 500 字」，`never().saveSettlement`。恰 500 允许。`"漏算".repeat(251)`=502 / `repeat(250)`=500 已测。不再依赖 DB 500。

❓ **Q5 — 跨商户？**  
➡️ `findSettlementById` 全局取行后比 `contract.merchantId` 与 scope，403 `CROSS_MERCHANT_FORBIDDEN`「无权查看其他商户的结算单」，失败审计。存在性侧信道需猜 `cst_`+UUID，非首次正常，不立项。

❓ **Q6 — 已结清 / 已挂起争议 / 已作废？**  
➡️ 测试循环 `settled`/`disputed`/`voided`：409 文案精确「结算单当前状态不可发起异议」且不含英文状态。`deposit_offsetting`/`arrears_collecting` 仍可留言（DISPUTABLE），与「未结清可留言、不暂停付款」一致。

❓ **Q7 — 失败是否留痕？**  
➡️ 三方法均 try/catch `recordFailure` 再抛。已测：无 scope、跨商户、空 reason。幂等层另 `markRetryableFailure`。成功路径在 `disputeOnce` 末 `recordSuccess`。

❓ **Q8 — 报错是否全中文？**  
➡️ 业务中文句（空请求/不存在/跨商户/不可异议/超长/scope）本身含 CJK，`translate` 原样。`"reason is required"` / `"settlement_id is required"` / `"idempotency_key is required"` 走 FieldLabels（原因未填写 / 结算单号未填写 / 请勿重复提交…）。未知英文走「提交内容有误…」，「绝不外泄英文」（FieldLabels L535–536）。本模块不再把内部 status 拼进报文（M020-006 现行不复现）。

❓ **Q9 — 成功审计 metadata 是否回退？**  
➡️ 生产：reserve 在事务外；`action`（save+recordSuccess）+`complete` 在同一 `TransactionTemplate`。审计 INSERT 无独立事务属性 → 失败则成功审计与结算 UPSERT 一起回滚；外层再记 `*_failed`。单测无 TM，不能反证生产。无证据表明 `REQUIRES_NEW` 把成功审计钉死在回滚之外。

---

## 4. 定稿 001–006 回归（按现行源码+命令，不抄「否」）

| ID | 原缺陷要点 | 现行 | 复现？ |
|---|---|---|---|
| M020-001 | `orElseGet(listContractsPaged/listAllSettlementsPaged)` + 注释「平台全量」+ 测试桩 empty | 全量调用已删除；`verify(never())` 两条 paged；类注释 fail-closed；测试 `thenThrow` | **不复现** |
| M020-002 | `saveSettlement(..., reason, null, null)` 清空裁决人并覆盖平台理由 | L155–158 传入 `merged, settlement.resolvedBy(), settlement.resolvedAt()`；测试保留 `usr_manager` 与平台理由 | **不复现** |
| M020-003 | 无列宽 → DB 500 | L151–153 合并后 >500 400；测试超长不写库 | **不复现** |
| M020-004 | 只验键非空、不经 IdempotencyService | Controller `ApiRequests.requireIdempotencyKey`；Service `idempotencyService.execute`；同键两次只 save 一次 | **不复现** |
| M020-005 | 只 recordSuccess | 三入口 catch `recordFailure`；测试无 scope / 跨商户 | **不复现** |
| M020-006 | 报错拼接英文 status | L146 固定中文；测试 `doesNotContain(status)` | **不复现** |

M020-007 / OpenAPI / V200 注释：仍为跨模块契约漂移，**本席不重新入本模块队列**（与定稿 XM 一致；代码已按 HTML L1322 不挂起、按红线④ fail-closed）。`operatorNote` 仍丢弃：只登记，不升格。

---

## 5. 跨模块观察（不入本模块修复队列）

1. `docs/openapi/openapi-p0.yaml` L20386/20409/20432–20433/20472 仍写全量监督、挂起有争议、reason 2000（M392 / 007）。
2. `V200__merchant_portal_zone_permissions.sql` L10 注释「平台管理员默认可监督」；权限可过、scope 不可过（M021）。
3. `MerchantZoneSettlementsPage.tsx` L13「本页只读」且无 `merchant-zone-settlement-dispute-form`；L83 只 GET。后端 POST 仍在（M023）。
4. `MerchantZoneContractsPage.tsx` L109 GET contracts；状态中文映射在前端。
5. `MerchantPortalJdbcIntegrationTests` 仍测仓储全量真分页，不测 Service；本席未跑库。
6. `AuditEventService` 终端字段硬编码 `web`。
7. 合同页内 `listContractSpaceIds` 每条一查（页级 N+1）。
8. HTML L1351「退租金额异议在退租审批中提出」与 L1322「对账可留言」并行；本入口只留言、不进 `resolveDispute`，与 L1322 一致、与 OpenAPI 闭环声称不一致（同 007）。

---

## 6. 临时 findings（GR）

本席审完四文件与 22 项后：**无新入队工程缺陷。**

判定顺序冲突→细化→技术性→添加均已走。锁外主张无。无添加项。无 HTML 自身问题需铁律 4 上报。不为「争议中再留言」「前端补表单」「改 OpenAPI」加需求。

---

## 7. 可执行证据（verification-before-completion）

```text
$ cd backend && mvn -q -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests test
# exit 0
$ cat target/surefire-reports/com.complex.platform.merchantportal.MerchantPortalServiceTests.txt
Test set: com.complex.platform.merchantportal.MerchantPortalServiceTests
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.488 s
$ env | grep -E '^COMPLEX_DATABASE_' || echo NO_COMPLEX_DATABASE_VARS
NO_COMPLEX_DATABASE_VARS
```

JDBC 集成测试未执行（缺三变量、未 source `/etc/complex/complex.env`）。不把未跑写成已跑。

---

## 8. 单席结论

覆盖：4/4 文件全文；22/22 映射判据均有适用结论与证据；三线均执行；声称落到 HTML 原文 / 源码原文 / 命令输出；无未答 questions；无未定稿本模块缺陷；锁哈希独占首行。

**PASS**
