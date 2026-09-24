CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B1 席乙（Grok 4.6/xhigh）修复前全模块审计 · V0.01

- 席位：审计席乙（独立；未读取 `modules/M020-b1-self-raw.md`，不依赖席甲发现；未派子代理/第三席/MCP）
- 阶段：V0.17 阶段 B / B1（本模块首次开工，历史未审；本席不改 ledger/findings/plan）
- 正本：`/home/james/complex-management` main 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): 钉住破窗不能无盘续期、换钥作废旧票`）
- 祖先：`eaccb9c5`「audit M020: settlement dispute is a note only, does not hang payment」∈ HEAD（`merge-base --is-ancestor` exit 0）；该提交只改 `MerchantPortalService.java` + `MerchantPortalServiceTests.java`，**未改 OpenAPI**；审计对象＝主工作区现状，**不把 eaccb9c5 当作已审 PASS**
- 计划：`plan/modules.md` V0.04 M020「商户档案、商户类型与商户侧门户／后台实现／第1段」；计划冻结有效内容行 242；本席实算 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **242**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行；sidecar `plan/criteria-lock.sha256` 同值
- 映射 22 项：BSM-001、BSM-003、BSM-114、GEN-001…GEN-018、PGM-001（未新增判据、未扩别模块）
- 结论：**FAIL**（有工程缺陷入本模块队列；无添加项；无 HTML 修改申请）
- 本席未改任何业务文件，未 fetch/commit/push/merge/deploy

---

## 0. 技能预检与节点应用（本环境实核）

按 `160-audit-prompt.md` §6 与 `plan/runbook.md` V0.02 §2 在本机路径核验，不抄 Codex 预检表。B1 只借收窄口径；本体要问人/派子代理/改代码的步骤一律作废。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别 audit；**不执行** Arc 全量审计工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上，不跑机械检查+并行评审 | **不用**（第三席+§7 外产物）；`references/platform-tools.md` **不存在** |
| `graft` | `~/.claude/skills/graft/SKILL.md` | skeleton/grep 定位 AuthorizationService、ContractRepository、调用边 | 无 |
| `grilling` | mattpocock `productivity/grilling/SKILL.md` | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `grill-me` | 同包 `productivity/grill-me/SKILL.md` | 本体只转发 `/grilling`，按 grilling 收窄口径执行 | 无 |
| `batch-grill-me` | — | — | **不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | 同包 `engineering/codebase-design/SKILL.md` | 以 Controller 为浅适配器、Service 为深度模块，查 seam（幂等/审计执行器未复用） | 无 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 用 OpenAPI/类注释 vs 真实现构造 red-capable 信号；本机未跑 JDBC | 本机 `COMPLEX_DATABASE_*` 三变量均未导出，禁止 `source /etc/complex/complex.env`，Java 集成测试未跑，不伪称已跑 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流；作废插桩/改代码 | 无 |
| `verification-before-completion` | superpowers | PASS/FAIL 前实跑 unit 测试与 sha/wc/git | JDBC 集成测试因三变量缺失未跑，结论不以「应能过」代替 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删死代码（orElseGet 全量分支） | 无 |
| `ponytail` | 同包 | B3 默认强度，B1 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 | 节点不适用 |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 | 节点不适用 |
| `writing-plans` / `brainstorming` | 存在 | 阶段 A | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 可选；本体要问人装 hook | **存在但未安装**（只读审计、禁写仓库其它文件） |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃 | 不套到本 raw 以外 |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。

---

## 1. 范围指纹（实算，非手抄）

命令与输出：

```text
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ git rev-parse HEAD
e456ba96c84185c8d39f18f548011daac48a6466
$ git merge-base --is-ancestor eaccb9c5 HEAD; echo $?
0
$ git diff HEAD -- backend/src/main/java/com/complex/platform/merchantportal/
# 空
$ git ls-files -z -- <4 files> | xargs -0 cat | wc -l
242
```

| 行数 | SHA256 | 路径 |
|---:|---|---|
| 40 | `3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java` |
| 55 | `5485f91571fde266488a9b6d0acb6c827befc63022500b4ea4a5ba0094dc8bbc` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java` |
| 138 | `54c3506234ad1545e4d9de2b1a61213506325d0a62f910eca86a4267cc2987c3` | `backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java` |
| 9 | `c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052` | `backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java` |
| **242** | （cat 合计） | 与计划「有效内容行 242」一致 |

`git diff HEAD -- <4 files>` 空：审计对象 = HEAD blob（含 eaccb9c5 的服务层改动）。

只读追依赖（不纳入本模块队列，缺陷记跨模块观察）：`AuthorizationService`（`requirePermission` / `currentMerchantScope` / `currentMerchantIdQuietly` / `isPlatformAdmin` / 红线④）；`ContractRepository` 接口 default 与 `JdbcContractRepository` 覆写；`ContractSettlementRecord` / `ContractRecord` 字段序；`ContractService.settle` / `resolveDispute` / `completeSettlement`；`AuditEventService.recordSuccess`；`IdempotencyService` + `AuditedOperationExecutor.executeWrite`；迁移 V162/V163/V171/V307/V200；`docs/openapi/openapi-p0.yaml` 三条 `/api/v1/merchant-portal/*`；`apps/web` 商户专区页；测试文件属 M022。

---

## 2. 全文覆盖（四文件，不抽样）

### 2.1 `MerchantContractView.java`（L1–L40）

| 段 | 行 | 覆盖 |
|---|---|---|
| 包/import | 1–6 | 依赖 `ContractRecord`、时间与 List |
| 类注释 | 8–11 | 声称「我的合同」只读关键信息（租期/空间/状态）、最小披露、不含审批链/收益快照 |
| record 字段 | 12–24 | id/merchantId/effectiveDate/expiryDate/lifecycleStatus/depositCent/projectId/spaceIds/renewedFromContractId/createdAt/updatedAt |
| `of` | 25–39 | 按名取值；`spaceIds == null ? List.of() : List.copyOf` |

第一性三问：

1. **为何必须存在**：HTML 商户专区「我的合同（关键条款、租期、到期情况）」+「查看…空间和合同摘要」。若删除，只能回传完整 `ContractRecord`（含 `rentTerms`/`snapshot`/`penaltyTerms`/`settlementRules`），与类注释「最小披露」及 BSM-114「不看平台全局」相悖。
2. **声称是否执行**：`of` 确实不拷贝 snapshot/审批链。`spaceIds` 由调用方传入（Service 再查 `listContractSpaceIds`）。未单独可执行；类型 4 读源 + 类型 1 HTML。
3. **未验证前提**：注释写 PRD 3.18 而非三 HTML；HTML「关键条款」未进入本视图（租金/抽成/结算口径缺席）。一期切片，记观察不入队（见 §6）。

分类：细化（上位 BSM-114 商户侧「我的合同」+ L1310 合同摘要）。本文件无工程缺陷入队。

### 2.2 `MerchantPortalController.java`（L1–L55）

| 段 | 行 | 覆盖 |
|---|---|---|
| 类注释 | 16–20 | `/api/v1/merchant-portal/*`；账单/工单复用既有域端点；此处只承载合同与结算的商户只读口径 |
| 路由 | 21–28 | `@RequestMapping("/api/v1/merchant-portal")` 注入 Service |
| GET `/contracts` | 31–36 | page 默认 1、size 默认 500 → `listMyContracts(page,size)` |
| GET `/settlements` | 38–43 | 同上 → `listMySettlements` |
| POST `.../dispute` | 45–54 | 校验 `idempotencyKey` 非空，否则 `IDEMPOTENCY_KEY_REQUIRED`；`@Valid` 但 DTO 无 Bean 约束；转 Service |

第一性三问：

1. **为何必须存在**：HTTP 入口。删则 HTML「我的合同/我的对账/可留言」无本切片 REST 面。
2. **声称是否执行**：三条路径与 OpenAPI path 一致。`@Valid` 对无注解 record 是空操作；真正校验在 Controller 手写 key + Service 手写 reason。
3. **未验证前提**：注释「需要商户只读口径」与 POST 异议写路径并存（写的是留言不是改状态，见 Service）。幂等键只做非空检查，不调用 `IdempotencyService`（GR-03）。

### 2.3 `MerchantPortalService.java`（L1–L138）

| 段 | 行 | 覆盖 |
|---|---|---|
| 类注释 | 21–26 | 我的合同/对账只读 + 结算异议留言；`currentMerchantScope` 商户只见本商户、无 merchant 抛 `MERCHANT_SCOPE_REQUIRED`；**「平台管理员 scope 为空走全量监督口径」**；异议只留言、不改状态、不暂停付款 |
| DISPUTABLE | 30–32 | `pending_settlement` / `deposit_offsetting` / `arrears_collecting`（eaccb9c5 去掉 `in_progress`） |
| 构造 | 38–46 | 注入仓储、授权、审计 |
| `listMyContracts()` | 48–50 | 无参转发 (1,500)；测试调用 |
| `listMyContracts(page,size)` | 52–72 | `requirePermission("contract.view")` → `currentMerchantScope()` → 有值则 `listContractsByMerchant` + 内存 `PageResponse.of`；**empty 则 `listContractsPaged`**；每条再 `listContractSpaceIds`；`recordSuccess(merchant_portal.contract.viewed, …)` |
| `listMySettlements()` | 74–76 | 无参转发 |
| `listMySettlements(page,size)` | 78–95 | `merchant.portal.settlement.view`；同样 scope 二分（商户 `listSettlementsByMerchant` / empty `listAllSettlementsPaged`）；直接回传 `ContractSettlementRecord`（无最小披露视图） |
| `submitSettlementDispute` | 97–137 | 权限 `merchant.portal.settlement.dispute`；reason 必填；按 id 取结算+合同；scope 存在且 ≠ `contract.merchantId` → `CROSS_MERCHANT_FORBIDDEN`；状态不在 DISPUTABLE → `SETTLEMENT_NOT_DISPUTABLE`；`saveSettlement` **保持 `settlement.status()`**，写入 `reason`，`resolvedBy/resolvedAt` 置 null；`recordSuccess(..., request.idempotencyKey(), {merchant_id, reason})` |

第一性三问：见 §3.1 与 GR-01/02/03/04/05/06。

### 2.4 `MerchantSettlementDisputeRequest.java`（L1–L9）

record：`idempotencyKey` / `operatorNote` / `reason`。无校验注解。Jackson 全局 `SNAKE_CASE`（`application.yml`）故 JSON `idempotency_key`/`operator_note`/`reason` 可绑定。`operatorNote` 全模块零读取（GR-07 只登记）。

---

## 3. 三线记录

### 3.1 第一性原理

**用途（删掉哪条 HTML 做不到）**

- 四文件共同承载业务地图「商户管理」L1310–1322 商户专区自助里的「我的合同」「我的对账（…有疑问可留言提异议）」以及 BSM-114 入口表「商户侧页面…对账、结算，不看平台全局」。
- HTML 原文（`docs/business-system-map-for-operations-v0.1.html` L1322）：「我的对账（结算与分成明细，有疑问可留言提异议）」；同句「提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补」。
- HTML 原文（同文件 L1848–1850，BSM-114 表「商户侧页面」行）：「在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。」
- 治理地图 L749–750：「商户侧不是平台后台，也不是只读页面。商户在自己的授权业务范围内完整使用相关功能，但不能跨商户和看平台全局。」（相关段；本模块映射 PGM-001 锚点是 L256 整体定位，见顺从式。）

**实际行为（类型 4 命令 + 源码，不以注释/测试桩为事实）**

1. `AuthorizationService.currentMerchantScope`（L425–431）有 merchantId → `Optional.of`；否则 **抛** `MERCHANT_SCOPE_REQUIRED`，**永不** `Optional.empty()`。注释 L423：「红线④：不因平台管理员身份整类关掉商户隔离」。`currentMerchantIdQuietly`（L440–451）才是空 Optional。`isPlatformAdmin` 本模块零调用。
2. 因此 Service L56–58 / L82–84 的 `.orElseGet(listContractsPaged / listAllSettlementsPaged)` 在生产 `AuthorizationService` 上 **不可达**。类注释 L23–24「平台管理员 scope 为空走全量监督口径」与 OpenAPI L20386/L20409「平台管理员为全量监督口径」均为假声称。
3. eaccb9c5 把异议从 `status="disputed"` 改为 `settlement.status()`。现行 `saveSettlement` 字段序与 `ContractSettlementRecord`（id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, projectId, createdAt, updatedAt）及 `JdbcContractRepository.saveSettlement` INSERT 列序一致。`ContractService.resolveDispute` 仍要求 `status=="disputed"` 才进入裁决流——商户留言 **不会** 进入该流。
4. OpenAPI L20432 仍写「结算单挂起为「有争议」; 平台按既有裁决流(contract.settlement.dispute.resolve)闭环」。`git show eaccb9c5 --stat` 仅 2 个 Java 文件，OpenAPI 未动。
5. 控制器收 `idempotency_key` 但 Service 只把它传给 `recordSuccess` 第 6 参。仓内写路径通例是 `AuditedOperationExecutor.executeWrite` → `IdempotencyService.execute`（先 reserve 再 action+complete）。本模块零注入。
6. `dispute_reason` 列 V162 L121 `VARCHAR(500)`；后续 V163/V171/V307 只改 status CHECK/列宽，**未扩 reason**。Java 无 max；OpenAPI `reason.maxLength: 2000`。超 500 走 `DataIntegrityViolationException` → `GlobalExceptionHandler` L162–167 **500 INTERNAL_ERROR**（明确不拦父类完整性违例）。
7. 成功审计：`recordSuccess(event, permission, businessType, businessId, requestId=null, idempotencyKey, metadata)` 参数序与 `AuditEventService` L27–37 一致。失败路径无 `recordFailure`。`AuditEventService.record` L117 终端字段硬编码 `"web"`（依赖观察）。
8. 前端 `MerchantZoneContractsPage.tsx` L109 `api.get("/api/v1/merchant-portal/contracts")`；`MerchantZoneSettlementsPage.tsx` L83 同 settlements。`adminClient.unwrap` 会把 Page 信封拍成 `data.items` 数组。**现行对账页无异议表单**（testId `merchant-zone-settlement-dispute-form` 断言为 null）；文案 L13「退租金额异议在退租审批中提出。本页只读，不改变结算状态、不暂停付款。」
9. 单元测试（不需库）：`mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests test` → **Tests run: 7, Failures: 0, Errors: 0**（surefire txt）。其中 `platformAdminSeesAllContracts` **桩** `currentMerchantScope() → Optional.empty()`，与生产抛错语义相反。JDBC 集成测试未跑（三变量未导出）。

**未经验证就当真的前提（已拆开）**

- 「`currentMerchantScope` 对平台管理员返回 empty」——假（生产抛错）。
- 「OpenAPI / 类注释的全量监督、挂起争议仍是现行行为」——全量监督不可达；挂起已被 eaccb9c5 从代码拿掉但 OpenAPI 仍声称。
- 「收集 idempotency_key 即幂等」——假。
- 「测试 7/7 绿证明生产授权/分页」——假（桩 + JDBC 未跑）。

### 3.2 顺从式（22 项逐条）

相关 HTML 已读全文：BSM-001 定义（业务地图 L505 段）、BSM-003 定义（L563 仅 h2+导语，子 h3 是别的 ID）、BSM-114 定义（L1827 入口表，商户侧页面行 L1848）、PGM-001 定义（治理地图 L256 整体定位+四条全局原则）；并读用户指定相关段：业务地图 L1310 商户侧、治理地图 L749 商户侧。

| ID | 适用结论 | 证据 |
|---|---|---|
| BSM-001 | 适用；本切片落实「谁在经营 / 签了什么合同 / 对账、结算」中的商户自助读+留言，无另发明业态枚举 | 四文件只暴露合同摘要与退租/到期结算单；未写死税率等策略参数 |
| BSM-003 | 适用导语「权限分岗」；子条 BSM-004…007 **未映射、不扩审** | `requirePermission` 三码分岗；无存档删除/分步新建/动态码实现于本四文件 |
| BSM-114 | 适用；商户侧页面行「对账、结算、不看平台全局」 | 有 scope 时按 `merchant_id` 过滤；跨商户异议 403。**文档/死代码声称平台全量**与「不看平台全局」及红线④对立，见 GR-02。留言不改状态对齐 L1322「付款义务不暂停」，见 GR-01 |
| GEN-001 | 适用 | GET 两入口有 HTML 上位。orElseGet 全量分支不可达（GR-02）。`operatorNote` 未读（GR-07 只登记）。无参 list  overload 仅测调用，技术性保留 |
| GEN-002 | 适用；**有缺陷** | 类注释/OpenAPI 全量监督、OpenAPI 挂起争议、幂等键「必须返回同一结果」均与现行真实现不一致（GR-01/02/03） |
| GEN-003 | 适用（测试文件属 M022，按真实现判断有效性） | 7 个 mock 测覆盖商户可见性/跨商户/结清不可异议/reason 必填/eaccb9c5 不改 status。`platformAdminSeesAllContracts` 桩 empty Optional，**不能**证明生产全量。JDBC 测的是仓储 `listAllSettlementsPaged`（服务层不可达）。不把 7/7 绿当行为证据 |
| GEN-004 | 适用；**有缺陷** | reason 首次写入前只 `trimToNull`，无长度/内容（数字异议）校验；超 DB 列宽变 500（GR-04）。settlementId `requireText`。请求体 null 有 400 |
| GEN-005 | 适用；生产授权 fail-closed 与红线④一致；**文档相反** | 真 `currentMerchantScope` 无商户即 403，默认拒绝。死代码若被「修 scope 返回 empty」会立刻变成商户侧接口看全平台（GR-02） |
| GEN-006 | 适用；写路径 `@Transactional` + `saveSettlement` UPSERT 单行 | 异议只改一表一行，无部分写入窗口。不改结算状态故不破坏待结算约束。无跨表事务需求 |
| GEN-007 | 适用；**有缺陷** | PGM-001「同一个操作被重复提交，系统只认一次」。OpenAPI `x-idempotency-required: true` 且「重复提交必须返回同一处理结果」。现行重复 POST 重复 UPDATE+重复审计（GR-03） |
| GEN-008 | 适用；**有缺陷** | OpenAPI 挂起 vs 代码不改状态；OpenAPI/注释全量 vs scope 永非空；reason 2000 vs VARCHAR(500)；DTO `operatorNote` 有契约无实现；前端已不提交异议但后端仍提供 POST（前端属 M023 观察） |
| GEN-009 | 适用；**有缺陷** | 超长 reason → 500 而非 400（GR-04）。`SETTLEMENT_NOT_DISPUTABLE` 把内部英文 status 拼进中文报错（GR-06）。`FieldLabels.translate` 能把 `reason is required` / `idempotency_key is required` 译成中文，这两处 **不** 另立项 |
| GEN-010 | 适用；未见注入/明文秘密 | 动态 SQL 均命名参数。跨商户先查后拒，存在性侧信道属猜测 ID，非首次正常，不立项。审计 metadata 含 reason 明文（对账异议，非秘密） |
| GEN-011 | 适用；**有缺陷** | 成功有结构化 `recordSuccess`。失败无 `recordFailure`，与 OpenAPI `*_failed` 及 `AuditedOperationExecutor` 通例不一致（GR-05） |
| GEN-012 | 适用；首次正常不立项 | 商户路径先按 merchant 全取再内存切片，一商户合同/退租单通常有界。平台真分页有 500 帽但不可达。N+1 `listContractSpaceIds` 记观察 |
| GEN-013 | 不适用本四文件 | 无构建/依赖/CI 内容 |
| GEN-014 | 不适用本四文件（无迁移） | 只读对齐：V162 建表 3 态 VARCHAR(16)+reason 500；V163 5 态 CHECK；V171 status→VARCHAR(32)；V307 加 `voided`。Service DISPUTABLE 不含 `voided`/`disputed`/`settled`，与 CHECK 兼容 |
| GEN-015 | 不适用本四文件 | 无 UI。前端中文标签属 M023 |
| GEN-016 | 适用；**有缺陷** | 类注释与 OpenAPI 把计划中的「平台全量」「挂起争议」写成当前事实（GR-01/02） |
| GEN-017 | 适用 | 本席全部条目按冲突→细化→技术性→添加顺序；无把业务发明标成技术性。无添加项 |
| GEN-018 | 适用 | 结论落到 HTML 原文、OpenAPI 原文、Java 原文、mvn/surefire/sha256/git 输出；不以 7/7 绿、eaccb9c5 提交说明或席位意见为据 |
| PGM-001 | 适用整体定位+全局原则 | 归属：按合同 `merchantId` 隔离。资格：权限码+scope。责任：成功审计。全链可点：本模块只 API，前端 TraceLink 属 M023。「同时操作以先成功为准 / 重复提交只认一次」→ GR-03。金额精度本切片不计算金额。界面全中文：用户可见报错经 FieldLabels；GR-06 为夹带英文状态码 |

### 3.3 对抗式（grilling 结构，自问自答，不等操作员）

❓ **Q1 — 平台管理员调 GET /contracts 能看到全部商户吗？**  
➡️ OpenAPI 与类注释说能。真 `currentMerchantScope` 在无 merchantId 时抛 403。`.orElseGet(listContractsPaged)` 死。V200 虽把 portal 权限授给 `role_platform_admin`，过不了 scope。**不是全量监督入口。**

❓ **Q2 — 商户重复点异议（同一 idempotency_key）会怎样？**  
➡️ 控制器只验非空。第二次 `saveSettlement` 再写同一 id，更新 `dispute_reason`/`updated_at`，再插一条 success 审计。不是「只认一次」。

❓ **Q3 — 异议会不会把退租结算挂起、卡住付款/结清？**  
➡️ 现行不改 status。`completeSettlement` 仍接受 `pending_settlement` 等三态。与 L1322「付款义务不暂停」一致，与 OpenAPI「挂起为有争议」不一致。`resolveDispute` 看不到这些单（要求 `disputed`）。

❓ **Q4 — 商户 B 用猜到的 settlement_id 打 A 的单？**  
➡️ 能读到存在性后 403 `CROSS_MERCHANT_FORBIDDEN`。首次正常是本商户操作自己的单，不立项。

❓ **Q5 — reason 贴一长段明细（>500 字）？**  
➡️ 无校验，JDBC 绑定超 `VARCHAR(500)` → 未处理的完整性违例 → 500「服务出现异常」。首次正常可复现。

❓ **Q6 — 对已结清单再异议？**  
➡️ `SETTLEMENT_NOT_DISPUTABLE` + 中文前缀 + 英文 `settled`。首次正常误操作可复现。

❓ **Q7 — 已有留言但 status 仍 pending 再提一条不同 reason？**  
➡️ DISPUTABLE 仍包含该态，覆盖 reason。OpenAPI「已有争议拒绝」在 eaccb9c5 后不再由本入口产生 `disputed`。属声称过时，并入 GR-01。

❓ **Q8 — 前端还会打 POST dispute 吗？**  
➡️ 现行 `MerchantZoneSettlementsPage` 无表单。后端端点仍在。缺陷以 API 契约为准，不因 UI 暂时不调用而消失。

❓ **Q9 — 能否用 `isPlatformAdmin` 打开全量以「修好」死代码？**  
➡️ 否。BSM-114 商户侧「不看平台全局」；红线④禁止因管理员身份整类关掉隔离。全量监督应走管理后台合同工作台，不是本前缀。最小处置是删死分支并改文档，而不是加旁路。

---

## 4. 临时 findings（GR）

判定顺序均为冲突 → 细化 → 技术性 → 添加。冻结 ID 仅引用 V0.11 已映射项。

### GR-01 — OpenAPI 声称异议挂起「有争议」并走 resolve-dispute；现行实现只写留言、不改状态

- **位置**：`MerchantPortalService.java:97–126`（方法注释 L97「只记理由，不改状态、不暂停付款」；L123–126 `saveSettlement(..., settlement.status(), ..., reason, null, null, ...)`）；对照 `docs/openapi/openapi-p0.yaml:20432–20433`（「结算单挂起为「有争议」; 平台按既有裁决流(contract.settlement.dispute.resolve)闭环」）；`ContractService.java:573–579`（裁决入口要求 `"disputed".equals(settlement.status())`）
- **更长上下文**：eaccb9c5 把 L123 第 4 实参从 `"disputed"` 改为 `settlement.status()`，DISPUTABLE 去掉 `in_progress`；`git show eaccb9c5 --stat` 不含 OpenAPI。
- **触发 / 影响**：商户对 `pending_settlement` 单 POST 异议 → 行仍待结算，平台 `resolve-dispute` 409 `CONTRACT_SETTLEMENT_NOT_DISPUTED`；与 OpenAPI 闭环声称相反。行为本身对齐 HTML L1322「可留言提异议」「付款义务不暂停」。
- **四分类**：细化（上位 BSM-114 相关 HTML L1322 留言/不暂停）。不是对 HTML 的冲突（代码不挂起）。缺陷是接口文档/历史裁决流声称未随 eaccb9c5 更新。
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-002、GEN-008、GEN-016、BSM-114
- **依据类型**：1 HTML 原文 L1322；4 `sed -n '20432,20433p' docs/openapi/openapi-p0.yaml` 与 `git show eaccb9c5`
- **最小拟处置**：以现行代码+HTML 为准，改 OpenAPI description（去掉挂起/resolve-dispute 闭环），使三条声称一致。禁止为「对齐 OpenAPI」把状态改回 `disputed`（会与 L1322 付款不暂停冲突）。OpenAPI 不在本模块四文件内，B2 须标明跨文件归属，不得在 B3 无 ID 改 yaml。

### GR-02 — 类注释/OpenAPI「平台管理员空 scope=全量」；`currentMerchantScope` 永不 empty，orElseGet 死代码

- **位置**：`MerchantPortalService.java:22–26`（「平台管理员 scope 为空走全量监督口径」）、`:52–58`、`:78–84`（`.orElseGet(() -> contractRepository.listContractsPaged / listAllSettlementsPaged)`）；`AuthorizationService.java:419–432`（无 merchantId **抛** `MERCHANT_SCOPE_REQUIRED`；L423 红线④）
- **更长上下文**：OpenAPI L20386、L20409 同声称。`currentMerchantIdQuietly` L440–451 才返回 empty，本模块未调用。`isPlatformAdmin` 本模块未调用。
- **触发 / 影响**：无商户归属的平台管理员（日常权限含 `contract.view` / V200 授了 portal 码）调本前缀 → 403，不是全量列表。死分支若被错误「修复」成 empty，商户侧 API 立即读全平台，违反 BSM-114「不看平台全局」与红线④。
- **四分类**：细化（上位 BSM-114「不看平台全局」+ GEN-005 默认拒绝）。不是添加（未发明新角色）。
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-001、GEN-002、GEN-005、GEN-016、BSM-114
- **依据类型**：4 读取 `AuthorizationService.java:425-431`；OpenAPI 原文
- **最小拟处置**：删除两处 `orElseGet` 全量分支（商户 map 即可；scope 方法在 empty 前已抛）。改正类注释与 OpenAPI「全量监督」句。**不要**加 `isPlatformAdmin` → 全量旁路。

### GR-03 — 声明幂等但未走仓内 `IdempotencyService`

- **位置**：`MerchantPortalController.java:50–53`（只验 key 非空）；`MerchantPortalService.java:127–135`（key 仅入审计）；对照 `AuditedOperationExecutor.java:113–138`（`idempotencyService.execute` + 成功审计）与 OpenAPI L20440、L20461–20464
- **触发 / 影响**：同一 key 连点两次 → 两次 UPSERT、两条 `merchant_portal.settlement.disputed`。PGM-001「不会因为重试就重复…」；本操作虽非扣款，契约写明「必须返回同一处理结果，避免重复写入」。
- **四分类**：细化（上位 PGM-001 重复提交只认一次）
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-007、GEN-002、PGM-001
- **依据类型**：1 PGM-001「同一个操作被重复提交，系统只认一次」；4 源码对照
- **最小拟处置**：写路径改走已有 `AuditedOperationExecutor.executeWrite`（或直接 `IdempotencyService.execute`），operationId 固定本动作。可与 GR-05 同一次最小改动覆盖失败审计。

### GR-04 — reason 无列宽校验，超 500 变 500 错误

- **位置**：`MerchantPortalService.java:104–107`；`MerchantSettlementDisputeRequest.java:7`；`V162__contract_lifecycle.sql:121` `dispute_reason VARCHAR(500)`；OpenAPI L20469–20472 `maxLength: 2000`；`GlobalExceptionHandler.java:148–167`
- **触发 / 影响**：留言超过 500 字符（对账「漏算/少算」贴明细属首次正常）→ 未捕获完整性违例 → HTTP 500「服务出现异常」。
- **四分类**：细化（上位 留言字段的实现细节）
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-004、GEN-008、GEN-009
- **依据类型**：4 读迁移与 handler；OpenAPI 原文
- **最小拟处置**：Service 在首次使用前拒绝 `reason.length()>500` 为 400 `VALIDATION_FAILED`（与现列宽一致）。不在本模块扩迁移；若要 2000 须另模块改 V162 列且走对应 ID。

### GR-05 — 失败路径不写 OpenAPI 所报的 `*_failed` 审计

- **位置**：`MerchantPortalService.java:52–72 / 78–95 / 98–137` 仅 `recordSuccess`；OpenAPI L20395、L20418、L20441 `x-audit-event-failure`；对照 `AuditedOperationExecutor.readPageResult/executeWrite` 的 catch `recordFailure`
- **触发 / 影响**：跨商户、状态不可异议、缺 reason、缺 scope 等首次正常失败不落失败事件，成功与失败证据不对称。
- **四分类**：细化（上位 GEN-011 / PGM-001 留痕）
- **工程缺陷是否成立**：**是**
- **冻结 ID**：GEN-011
- **依据类型**：4 源码；OpenAPI 原文
- **最小拟处置**：失败 catch 记 `recordFailure` 再抛；优先复用执行器（与 GR-03 合并）。

### GR-06 — 不可异议报错回显英文内部状态码

- **位置**：`MerchantPortalService.java:118–120` `"结算单当前状态不可发起异议：" + settlement.status()`
- **触发 / 影响**：对 `settled`/`disputed`/`voided` 再异议 → 响应体含中文+`settled` 等。`FieldLabels.translate` 遇 CJK 原样透传，英文码出网。PGM-001「报错必须全部为中文」。
- **四分类**：细化（上位 PGM-001 零英文报错）
- **工程缺陷是否成立**：**是**
- **冻结 ID**：PGM-001、GEN-009
- **依据类型**：1 PGM-001「展示给用户的一切文字…报错…必须全部为中文」；4 读 Service L118–120 与 `FieldLabels.java:535–536`
- **最小拟处置**：去掉内部码，固定中文「结算单当前状态不可发起异议」。

### GR-07 — `operatorNote` 契约字段从未读取（只登记）

- **位置**：`MerchantSettlementDisputeRequest.java:4–7`；OpenAPI L20465–20468；Service 全文件无 `operatorNote`
- **四分类**：**技术性**（无业务发明：操作备注不是新业务实体/状态/权限；与 HTML 无冲突）
- **工程缺陷是否成立**：**否**（缺填不失败；首次正常不依赖该字段）
- **冻结 ID**：GEN-001、GEN-017
- **依据类型**：4 grep 本模块零引用
- **最小拟处置**：不修。若 B2 要收口文档可删 OpenAPI 属性；非修复队列。

无冲突类、无添加类。无需 `questions/`。

---

## 5. ponytail-review（只列可删，不修）

- `MerchantPortalService.java:L56-58`: delete: `orElseGet(listContractsPaged)` 生产不可达。删除，只留商户 `map`。
- `MerchantPortalService.java:L82-84`: delete: 同上 `listAllSettlementsPaged`。
- `MerchantPortalService.java:L48-50,L74-76`: yagni: 无参 overload 仅测试。可留。
- net: 约 −8 行可达删除（两处 orElseGet）。Lean already 不成立。

---

## 6. 跨模块观察（不入本模块队列）

1. **M022 测试桩伪造空 scope**：`MerchantPortalServiceTests.java:70–79` `platformAdminSeesAllContracts`；JDBC 类注释写「平台管理员全量真分页」但测的是仓储 `listAllSettlementsPaged`，服务层不可达。GEN-003 在本席按真实现判为无效覆盖，文件本身属 M022。
2. **M023 前端**：对账页已去掉异议表单与「有异议」KPI，文案改只读；`unwrap` 拍平 Page。e2e `full-admin-remaining.spec.ts` 仍 `getByTestId("merchant-zone-settlement-dispute-form")` 可见——前后端/e2e 漂移。
3. **V200**：portal 三码只 INSERT `role_platform_admin`；`contract.view` 仍是 V010 平台码，V200 未给商户角色播种。商户能否打开「我的合同」取决于运行时模板（e2e mock 含 `contract.view`）。属 M021。
4. **`AuditEventService.record` 终端硬编码 `"web"`**：商户专区会话上报 `merchant_portal`（`AdminApp.tsx`）。属 evidence 模块。
5. **`ContractSettlementRecord` 类注释**仍写 `in_progress/disputed/settled` 与「争议挂起」，与 V163 五态+本入口不挂起不一致。属合同域。
6. **HTML「与客人投诉并入同一通道」**：本入口只写 `dispute_reason` 列，未见客诉模块读取。一期留言可见于对账表，通道接线不在本四文件。
7. **「关键条款」**：视图无租金/抽成。一期注释自承租期/空间/状态。遗漏不升格为本模块冲突。
8. **JDBC 集成测试未跑**：`env | awk '/^COMPLEX_DATABASE_/'` 空；禁止 source `/etc/complex/complex.env`。不把未跑写成 PASS。

---

## 7. 可运行命令与输出（节选）

```text
$ mvn -Dtest=com.complex.platform.merchantportal.MerchantPortalServiceTests test
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.366 s
$ env | awk -F= '/^COMPLEX_DATABASE_/{print $1}'
# 空
$ git show --stat eaccb9c5
MerchantPortalService.java | 8 ++++----
MerchantPortalServiceTests.java | 4 ++--
```

---

## 8. 本席结论

- 四文件全文覆盖；22 判据均有适用/不适用说明；三线已跑。
- 工程缺陷入队：GR-01、GR-02、GR-03、GR-04、GR-05、GR-06。GR-07 只登记。无添加、无 HTML 申请、无未答 questions。
- 单席 B1 审计结论：**FAIL**（存在未定稿工程缺陷；铁律 1 不存在有条件通过）。
- 不把 eaccb9c5、7/7 mock 绿、或 OpenAPI/注释中的「全量/挂起」当作已审 PASS。

## M020 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 34806 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M020 FILES 4 / 锁映射 22 / 计划冻结 242 行，现文 242 行）
- 正本：`/home/james/complex-management`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `a29220dbf34b5736ffe90300b975ce441e24fa4d`。本模块 4 文件指纹与 B1 覆盖表全等。B3=0。无源修复。三份 HTML 现文：治理地图 V0.12、业务地图 V0.14、枚举 V0.07。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。本席只读 `M020-b1-self-raw.md` 前 26375 字节（V0.01 SHA `b039564c2343ec78ca994c23d2152f8a1b9b45e3edc1b0e67b040d0f1b459d9d`）；其后 V0.02 B2 结论段不作为本票依据。
- 本席 B2-r1：**PASS**（001–007 逐 ID 全 PASS；002 独立核源成立；007 按 M015-011 登记 XM 不在 B3 改 yaml；GR-07/CX-005 只登记。添加 0、不发 2.2。非阻断勘误见 002 上位句精确原文在 L272，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M020 → [BSM-001, BSM-003, BSM-114, GEN-001…GEN-018, PGM-001]` COUNT=22 unique=22。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/行号/两份 B1 前缀/候选段/源码/HTML L1322·L1850·L256 段/M015-011 定稿行；未对任一 `complex_test*` 库做写操作，未重审四文件、未跑 mvn。

此票只判断 findings.md V2.62 顶部「当前 M020 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M020-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 34806 | `b170414d3505366c326cd6a7381e19819f8ed4fe855cef0dfa1172b7bbd725b0` |
| `docs/audit-2026-09/modules/M020-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 26375 | `b039564c2343ec78ca994c23d2152f8a1b9b45e3edc1b0e67b040d0f1b459d9d` |
| `docs/audit-2026-09/findings.md` | V2.62 | 1027317 | `d28270b261b71347d76b587e5c11b2f43a344b07e50e95a1472f8022318034bc` |

席甲 raw 全文现 28360 bytes（SHA `bc85cd697c4e596913068e7c46abd9cffe81c34e21b79c53efd0307bbb6a8cc4`）；本席按指令只取前 26375 字节，不读其后 V0.02 B2 段。本文件独占锁哈希字段行仍仅文首一处（B1 正文提及该字段名的说明句不算新独占行），本段未新增。

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

本模块 4 文件指纹（与 B1 覆盖表 match）：

| 路径 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| `MerchantContractView.java` | 40 | 1348 | `3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8` |
| `MerchantPortalController.java` | 55 | 2583 | `5485f91571fde266488a9b6d0acb6c827befc63022500b4ea4a5ba0094dc8bbc` |
| `MerchantPortalService.java` | 138 | 7155 | `54c3506234ad1545e4d9de2b1a61213506325d0a62f910eca86a4267cc2987c3` |
| `dto/MerchantSettlementDisputeRequest.java` | 9 | 245 | `c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052` |

上位句（本席 B2 只核分类所需原文，不重审模块）：

- 业务地图 L1322：`我的对账（结算与分成明细，有疑问可留言提异议）`；同句 `提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补`。
- 业务地图 L1850（BSM-114 商户侧页面行）：`在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。`
- 治理地图 L256 段题 `整体定位`；同段 L272 `不允许出现无法点击溯源的孤立数据`；L283–284 `正反可查`；L287 `报错…必须全部为中文，不得出现任何英文（如 active/draft 英文状态…）`；L303 `同一个操作被重复提交，系统只认一次`。

`questions/M020*`：不存在。`docs/openapi/openapi-p0.yaml` 主归属 M392；`V200__merchant_portal_zone_permissions.sql` 主归属 M021；`MerchantPortalServiceTests.java` 主归属 M022。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复。本批重点独立核 002（本席 B1 未立为 finding） |
| grilling | 只借追问结构自查：002 开账→裁决→留言是否清空字段、001 是否发明 isPlatformAdmin 旁路、004 走 execute 而非执行器、007 XM 是否成立、operator_note 只登记、有无应升添加 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/计数（31 execute / 32 requireIdempotencyKey） |
| graft | skeleton/grep 定位 ContractSettlementRecord、saveSettlement、idempotencyService.execute |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| ponytail-review | 只评最小删除/收窄，不落地。001 删死代码不加旁路；004 直接 execute；003 不扩列；007 不改 yaml |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（GR-01…07、跨模块观察 1…8、席甲 CX-M020-001…005、OBS-1…8）

漏并：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–007 或只登记。无单席独有缺陷被丢。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M020-001 | GR-02 + CX-M020-001 | 细化/是/是 | **PASS（细化/是/是；删死代码，不加 isPlatformAdmin 旁路）** |
| M020-002 | CX-M020-002（本席 B1 未立；Q7 只及 reason 覆盖并入 GR-01） | 细化/是/是 | **PASS（独立核源成立；见下节）** |
| M020-003 | GR-04 + CX-M020-003 | 细化/是/是 | **PASS（细化/是/是；合并后 >500 拒 400，不扩迁移）** |
| M020-004 | GR-03 + CX-M020-004 | 细化/是/是 | **PASS（直接 IdempotencyService.execute，非 AuditedOperationExecutor）** |
| M020-005 | GR-05（席甲 B1 只记 OBS-4） | 细化/是/是 | **PASS（升队合法：本席 B1 已立工程缺陷）** |
| M020-006 | GR-06 | 细化/是/是 | **PASS（去掉英文状态后缀）** |
| M020-007 | GR-01 + GR-02/GR-04 的 OpenAPI 部分 + 席甲 OBS-1/OBS-8 | 细化/是/否（XM→M392/M021） | **PASS（M015-011 先例；B3 不改 yaml）** |
| （不新开） | GR-07 + CX-M020-005 | 只登记（XM→M392 契约字段） | **PASS（缺填不失败；同域 ContractService 亦不消费 operatorNote）** |

冲突入队 0。细化入队 6（001–006）。细化登记 XM 1（007）。技术性入队 0。观察否：双方其余 OBS 按候选「只登记不入队」。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–006（6）；「否」本批新开 = 007（1，XM）。

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-02 + CX-001 | 001 | 同根：类注释/orElseGet/`platform-all`/测试桩 empty Optional vs `currentMerchantScope` 永不 empty。OpenAPI/V200 句拆到 007。未改写。 |
| CX-002 | 002 | 席甲独有。本席 B1 Q7 只写「再提一条覆盖 reason」并入 GR-01，GEN-006 写成「一表一行无部分写入」，未核 `resolvedBy`/`resolvedAt` 置 null。候选原样入队。见下节：**成立**。 |
| GR-04 + CX-003 | 003 | 同根：reason 无列宽校验，VARCHAR(500)，超长 500。OpenAPI maxLength 2000 拆到 007。未改写。 |
| GR-03 + CX-004 | 004 | 同根：幂等键只验非空、不经 IdempotencyService。候选把本席「执行器或直接 execute」收窄为直接 execute，原意（同键只认一次）未改。 |
| GR-05 | 005 | 本席独有（席甲 OBS-4）。升队合法：B1 已立失败路径无 `*_failed`。 |
| GR-06 | 006 | 本席独有。未改写。 |
| GR-01 + GR-02/04 OpenAPI + 席甲 OBS-1/8 | 007 | 同根：契约「挂起有争议/全量监督/reason 2000/仅 200 响应」vs 代码+HTML。本席 GR-01 最小拟处置本就是改 OpenAPI、禁止改回 disputed；yaml 不在本模块 4 文件。 |
| GR-07 + CX-005 | 只登记 | 席甲 B1 原拟入队（审计 metadata 加 operator_note）；候选按本席 GR-07「缺填不失败、首次正常不依赖」只登记，席甲撤回入队。独立核：DTO 有字段、Service 0 读、`ContractService.java` `operatorNote`/`operator_note` 计数 0。成立。 |
| 席甲 OBS-2 | 007 / 只登记 | V200 L10 归 M021，写在 007 处置里。 |
| 席甲 OBS-3/6；本席观察 1…8；席甲 OBS-5/7 | 只登记 | 无应升添加、无应升本批缺陷被降。席甲 OBS-4 已升 005。OBS-7 跨单元测试由 001–006 写入 M022 测试类，注释本身仍只登记。 |
| 席甲追加 FinanceService:169-171 | 只登记 XM | 本席核源：`listArApItems:169-171` 确有 `currentMerchantScope().orElse(null)` + 注释「平台口径两者皆 null=全量」。属 finance，不新开 M020 ID。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：本席跨模块观察 1…8（M022 JDBC 注释、M023 对账页无表单且 e2e 仍找表单、V200 未播种商户角色、AuditEventService 终端硬编码 web、ContractSettlementRecord 注释三态、客诉通道未接线、视图缺「关键条款」、JDBC 集成测试未跑）；席甲 OBS-3、OBS-6。

### 二、002 独立核实（本席 B1 未发现）—— **成立**

核源（只读，行号与候选一致）：

1. **字段序** `ContractSettlementRecord.java:8-20`：`(id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, projectId, createdAt, updatedAt)`。第 6/7/8 位是理由/裁决人/裁决时间。
2. **商户写入** `MerchantPortalService.java:123-126`：
   ```
   new ContractSettlementRecord(
       settlement.id(), settlement.contractId(), settlement.triggerType(), settlement.status(),
       settlement.settlementItems(), reason, null, null,
       settlement.projectId(), settlement.createdAt(), now)
   ```
   第 6 实参 = 商户 `reason`（覆盖既有 `disputeReason`）；第 7/8 实参字面 `null, null`（清空 `resolvedBy`/`resolvedAt`）；第 4 实参保持 `settlement.status()`（eaccb9c5 note-only，不改状态）。
3. **upsert** `JdbcContractRepository.saveSettlement:248-251`：`ON CONFLICT (id) DO UPDATE SET … dispute_reason = EXCLUDED.dispute_reason, resolved_by = EXCLUDED.resolved_by, resolved_at = EXCLUDED.resolved_at`。三列均覆写，不是部分列保留。
4. **开账** `ContractService.settle:558-565`：`String dispute = trimToNull(actual.disputeReason());` → `status = dispute != null ? "disputed" : "pending_settlement"` → `saveSettlement(..., status, items, dispute, null, null, ...)`。带异议开账写入平台理由、状态 `disputed`、裁决人空。
5. **裁决** `resolveDispute:578-587`：要求 `"disputed".equals(settlement.status())`；写回 `status="pending_settlement"`、`settlement.disputeReason()`（**保留**平台理由）、`authorizationService.currentActor().id()`、`now`（写入裁决人/裁决时间）。
6. **可异议集合** `MerchantPortalService:31-32`：`pending_settlement` / `deposit_offsetting` / `arrears_collecting`。**不含** `disputed`。故商户在挂起争议期间不能留言；裁决回到 `pending_settlement` 后即可留言。
7. **开账审计** `:567` `contract.settle_opened` metadata 仅 `trigger`/`status`，不含理由。裁决审计 `:589-590` metadata 空 Map。平台理由只活在行上的 `dispute_reason`。

因此首次可达流「平台开账（有争议）→ 主管裁决 → 商户留言」：行从 `(disputed, 平台理由, null, null)` → `(pending_settlement, 平台理由, 裁决人, now)` → `(pending_settlement, 商户留言, null, null)`。裁决人/时间被清空，平台理由被覆盖且审计无处回查。GEN-006（失败不得留下部分写入/清字段）与 PGM-001 段「正反可查 / 不允许出现无法点击溯源的孤立数据」成立。本席 B1 GEN-006「一表一行无部分写入」**撤回该满足判定**。

本席 B1 Q7 只覆盖「同态再提一条覆盖 reason」，未追平台开账+裁决后再留言。候选把 CX-002 单列 002、不并进 GR-01，正确：GR-01 是契约挂起声称，002 是字段清空。

四分类：**细化**（上位 L1322「可留言」+ L272/L283 可溯源；不是新实体/状态/权限，故非添加；HTML 未禁止「裁决后留言」，故非冲突）。工程缺陷：**是**。队列：**是**。最小处置：保留 `settlement.resolvedBy()`/`resolvedAt()`；既有 `disputeReason` 非空则「既有 + 换行 + 商户留言」（合并后走 003 的 500 上限）。**不同意**席甲 B1 备选「已有平台争议记录则 SETTLEMENT_NOT_DISPUTABLE」——那会发明「裁决后不能留言」，与 L1322 相悖。票：**PASS**。

### 三、分类

2.1 顺序：冲突 → 细化 → 技术性 → 添加。能指到 HTML 上位的一律先归细化，不进技术性。

**001 细化（非冲突、非添加）**：生产 `currentMerchantScope:425-432` 无 merchantId 即抛 `MERCHANT_SCOPE_REQUIRED`，与红线④、L1850「不看平台全局」一致。缺陷是类注释/死代码/假绿测试声称相反。删 `orElseGet` 是把实现收窄到已在跑的 fail-closed，不是发明平台全量。若加 `isPlatformAdmin` 旁路才是与 L1850 冲突的业务发明（且会抄错 M015-015 的项目全量桥）。票：**PASS（细化）**。

**002 细化**：见上节。L256 段可指到 L272/L283。非阻断：候选写「不允许无法溯源」是 L272「不允许出现无法点击溯源的孤立数据」的缩写，不否决。票：**PASS（细化）**。

**003 细化**：L1322「可留言」字段的实现细节；列宽已是 V162:121 `VARCHAR(500)`；超长变 500 是 GEN-004/009 下位，不是新长度政策。不扩迁移＝不发明 2000 字业务上限。票：**PASS（细化）**。

**004 细化**：上位 L303「同一个操作被重复提交，系统只认一次」+ OpenAPI `x-idempotency-required: true`。不是新幂等语义。票：**PASS（细化）**。

**005 细化**：上位 L256 段责任/留痕 + GEN-011；OpenAPI 已列 `*_failed`。补 catch 记失败再抛，不发明新事件名。票：**PASS（细化）**。

**006 细化（非冲突）**：上位 L287「报错必须全部为中文…如 active/draft 英文状态」。错误句夹带 `settled`/`disputed`/`voided` 是该原则的下位实现，不是另写一套状态机。最小修法去掉后缀即原意。票：**PASS（细化）**。

**007 细化 + XM**：代码已按 L1322 不挂起、按红线④ fail-closed；缺陷全在 `openapi-p0.yaml`（M392）与 V200 注释（M021）。与 M015-011 同构：011 为「契约/前端/后端须同批加确认字段，本模块单独改会 400」，队列否、登记 XM。此处本模块单独改 yaml 不在写入集合；为「对齐 OpenAPI」把状态改回 `disputed` 会与 L1322 付款不暂停冲突。票：**PASS（细化/否/XM）**。

**operator_note 只登记**：可选契约字段，缺填不失败，首次正常用 `reason`。本席 GR-07 工程缺陷否；席甲 CX-005 原拟入队。同域 `ContractService` 结算写路径 0 处消费 `operatorNote`。闭合面在 M392 契约，不在本四文件修 DTO 或强行入审计。票：**PASS（只登记）**。

### 四、最小处置边界

写入集合限：本模块 4 文件 + `backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java`（M022，跨单元 TDD；先例 M015 B3 改 M017 测试类）。不改 `AuthorizationService`、不改迁移、不改 `openapi-p0.yaml`、不改三份 HTML、不改 JDBC 集成测试类。

**001 删两处 orElseGet 与 platform-all、改注释、测试改为无商户账号被拒（不加 isPlatformAdmin）——同意。** `currentMerchantScope` 在 empty 前已抛，删 `:56-58`/`:82-84` 后商户号 `orElseThrow` 与真实现同语义。`:66`/`:89` `scope.orElse("platform-all")` 随死分支删除。类注释改为「无商户归属账号（含平台管理员）fail-closed 403，平台监督走各域平台端点」。测试 `platformAdminSeesAllContracts` → `callerWithoutMerchantIsRejected`（桩按真实现抛，断言不触库）。**禁止**抄 M015-015 的 `isPlatformAdmin` 全量桥：那是项目 scope 的平台端点，本前缀是商户侧 L1850。票：**PASS**。

**002 保留 resolvedBy/resolvedAt 并合并既有理由——同意。** 不改字段集、不改 DISPUTABLE、不改 status。合并规则「既有非空则换行追加」是单列 VARCHAR 上保留两段文本的最小写法，不是新业务对象。第二次商户留言同样走合并+003 上限，不另发明「追加留言状态机」。票：**PASS**。

**003 合并后长度 >500 拒 400——同意。** 文案「异议留言不能超过 500 字」；`VALIDATION_FAILED`。不扩 V162、不把 OpenAPI 2000 当本模块目标。与 002 同一写入点校验，避免合并后再撞 500。票：**PASS**。

**004 服务注入 IdempotencyService 直接 execute，控制器改 ApiRequests.requireIdempotencyKey——同意，而非 AuditedOperationExecutor。** 本席独立计数：`backend/src/main/java` 中含 `idempotencyService.execute` 的文件 **31**；含 `ApiRequests.requireIdempotencyKey` 的 Controller **32**；仓内手写 `IDEMPOTENCY_KEY_REQUIRED` **仅本控制器 :50-52**。本席 GR-03 曾写「执行器或直接 execute」；执行器 `executeWrite` 会接管成功审计（虽可传 `successMetadata`），且两条 GET 仍要 005 的 catch。直接 `idempotencyService.execute("merchantPortalSettlementDispute:" + settlementId, key, request, action, ContractSettlementRecord.class)` 对齐 31 服务范式，成功审计 metadata 可保持 `:127-135` 的 `merchant_id`/`reason`。测试用真 `IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper())`（该类存在于 `backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java`）同键两次只写一次——比席甲 B1「桩直通」更严，不改原意。票：**PASS**。

**005 三方法 try/catch 记 recordFailure 再抛，成功审计 metadata 不变——同意。** 事件名用 OpenAPI 已声明的 `merchant_portal.contract.view_failed` / `settlement.view_failed` / `settlement.dispute_failed`。与 004 同一次改动：写路径 catch 包住校验+execute，使跨商户/不可异议/缺 reason 等首次正常失败也落失败事件。不把成功 metadata 改成执行器默认空 Map。测试补跨商户失败落 `dispute_failed`。票：**PASS**。

**006 去英文状态后缀——同意。** `:118-120` 固定「结算单当前状态不可发起异议」。`FieldLabels.translate:536` 遇 CJK 原样透传，去后缀后无英文出网。测试断言文案不含状态码。票：**PASS**。

**007 不在 B3 改 yaml——同意。** `plan/modules.md`：`docs/openapi/openapi-p0.yaml` → M392；V200 → M021。M015-011 定稿行：细化/是/否（XM，契约+前端+后端同批）。本条同构。票：**PASS**。

有无发明业务规则或应升添加：无。001 删死代码；002 保留已有列；003 对齐已有列宽；004/005 对齐已有仓内范式与已声明事件；006 删英文后缀；007 不改行为。无应升 2.2 的项。

### 五、添加 / HTML / questions

本批无添加项、无 HTML 申请、无 questions。`questions/M020*` 不存在。不 BLOCKED-ASK。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 4 文件 + `MerchantPortalServiceTests.java`，不改 AuthorizationService / 迁移 / OpenAPI / HTML / JDBC 集成测试。

- 001：删两处 `orElseGet` 与 `platform-all`；改类注释为 fail-closed；测试 `callerWithoutMerchantIsRejected`。不加 `isPlatformAdmin`。
- 002：`saveSettlement` 传 `settlement.resolvedBy()`/`resolvedAt()`；既有理由非空则换行合并；测试「裁决后留言不清空裁决人、不丢平台理由」。
- 003：合并后 `length()>500` → 400 `VALIDATION_FAILED`「异议留言不能超过 500 字」；超长用例。不扩迁移。
- 004：注入 `IdempotencyService`，`submitSettlementDispute` 主体 `execute(...)`；控制器 `ApiRequests.requireIdempotencyKey`；真内存仓库同键两次只写一次。
- 005：三方法体 `try/catch RuntimeException` → `recordFailure` 再抛；成功 metadata 不变；跨商户失败落 `dispute_failed`。
- 006：去掉 `+ settlement.status()`。
- 007：不修；等 M392/M021 XM。
- GR-07/CX-005：不修。

禁止：改回 `status="disputed"`、加平台全量旁路、扩 `dispute_reason` 列、改 OpenAPI/V200/HTML、把 operator_note 当本批入队项、用「已有平台理由则拒绝留言」替代 002 合并、把 GET 全量监督做成 isPlatformAdmin。

### 结论

全批差集无漏并、无原意改写。002 经字段序/upsert/settle:558-565/resolveDispute:583-587/DISPUTABLE:31-32/Service:123-126 独立核实成立，本席 B1 对 GEN-006「无部分写入」的满足判定撤回。001/003/004/005/006/007 上位 L1850/L1322/L256 段指得出。007 按 M015-011 登记 XM、B3 不改 `openapi-p0.yaml` 成立。operator_note 只登记成立。004 直接 execute 成立。001 不加 isPlatformAdmin。写入集合限 4 文件+M022 测试类成立。添加 0。非阻断：002 上位精确原文为 L272「不允许出现无法点击溯源的孤立数据」。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M020 IN_PROGRESS；B3=0。
