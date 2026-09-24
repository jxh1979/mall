CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M020-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M020 / 共 471，B3 第 1 次之后的 B4。计划名：商户档案、商户类型与商户侧门户／后台实现／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M020-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3、`findings.md` V2.63 顶部「当前 M020 B2-r1 双PASS定稿」、本席旧 `M020-b1-grok-raw.md` V0.01/V0.02、冻结 diff 与两 tree、现行 `MerchantPortalService`/`MerchantPortalController`/`MerchantPortalServiceTests`、`AuthorizationService.currentMerchantScope`、`IdempotencyService.execute`/`runAndComplete`/`markRetryableFailure`、`AuditEventService.recordSuccess`/`recordFailure`、`ContractSettlementRecord` 字段序、`JdbcContractRepository.saveSettlement`、`ContractService.settle`/`resolveDispute`、`V162` 列宽、`ApiRequests.requireIdempotencyKey`、`FieldLabels.translate`、OpenAPI 三条 `/api/v1/merchant-portal/*` 的 `x-audit-event-failure`。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、只读探针、本席复跑单元测试与本席未导出三变量故不重跑 JDBC 的缺口声明为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M020-001…006 是否按 findings V2.63 定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到 001–006；是否误删无参重载或改动 MerchantContractView/dto；`@Transactional` 移除后原子性是否由 IdempotencyService 事务模板承接；Map.of 参数是否可能为 null；成功审计 metadata 是否不变；是否加平台全量旁路或改结算状态；读路径失败审计 businessId 是否为空；幂等失败路径是否先标 FAILED_RETRYABLE 再记失败审计；测试是否按真实现语义。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿证据并本席复跑单元测试，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物）；`references/platform-tools.md` **不存在** |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep`/`skeleton` 定位 `currentMerchantScope`/`recordFailure`/`requireIdempotencyKey`/`saveSettlement`/`IdempotencyService`/`MerchantPortalService` 测试 API | 无 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | Controller 浅适配器、Service 深模块；execute 为写路径缝；本 diff 把死代码全量分支与自写幂等键校验从接口上删掉 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、`rg` 残留、读 V162:121、本席复跑单元测试） | 本席环境未导出 `COMPLEX_DATABASE_*` 三变量，禁止 `source /etc/complex/complex.env`，JDBC 集成测试未本席重跑 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→Service/Controller→IdempotencyService.runAndComplete→AuditEventService→JdbcContractRepository.saveSettlement→V162）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑下列命令；JDBC 不以「应能过」代替 | JDBC 缺口见上 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 RED 构造器不成立 + GREEN 10+2；本席复跑 10 单元 | 本席不重写测试 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。

graft 本席调用节省合计约 496700 tokens（`currentMerchantScope` / `recordFailure` / `requireIdempotencyKey` / `saveSettlement` / `ContractSettlementRecord` / `IdempotencyService` / `MerchantPortalService` / `MerchantPortalServiceTests`）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。findings.md 版本 V2.63，本席 `sha256sum` = `4daae9d3cd65db27f695cae8a4094434a578ad453c3b72ece89916886d8c48be`。

| 项 | 值 |
|---|---|
| 模块 | M020 后台实现／第1段（计划 4 文件，有效内容行 242） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V2.63：M020-001…006 全部归属「是」（全部细化）；007 否（XM，本 diff 未改 yaml/迁移） |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `8baac60a74362e8b8279b861e162d0b0f54088cc` |
| after（对象类型 tree） | `d063773a67499e1b19fb8d2a46b1ee0324a9acbf` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 8baac60a74362e8b8279b861e162d0b0f54088cc d063773a67499e1b19fb8d2a46b1ee0324a9acbf`（无路径过滤） |
| 实际差异 | 32036 bytes；SHA256 `2739f6165931e99a97cbb64488c927bf412bd58c276ff6f0e93f9526325d41c6`；3 files changed, 238 insertions(+), 90 deletions(-)；13 hunk；UTF-8 |
| 与冻结文件 `/home/james/audit-tmp/m020_b3_r1.diff` | 字节、SHA256、`cmp` 全等 |
| `git diff --check` | exit 0 |
| `git diff --name-status` | 仅下列 3 路径，全 `M`，无 A/D/R |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（3 改动文件工作区 blob 与 after tree 全等） |
| MerchantContractView / dto / AuthorizationService / 迁移 / OpenAPI / HTML | 本 diff 路径清单为空；两 tree 上 View/dto blob 全等（`b85e23ae…` / `d7f46f00…`） |
| 只读探针 | `/home/james/audit-tmp/m020_b3r1_post.log`；本席复跑残留计数与之同向（见 §4） |
| TDD | RED `/home/james/audit-tmp/m020_b3r1_red.log`（新测试对旧 3 参构造器，MVN_EXIT=1）；GREEN `/home/james/audit-tmp/m020_b3r1_green.log`（10+2，GREEN_MVN_EXIT=0）；本席复跑单元 10/10 |

三改动文件 blob（`git rev-parse TREE:path` / 工作区 `git hash-object`）与内容 SHA256：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 |
|---|---|---|---:|---|
| `MerchantPortalController.java` | `38fae0825f28dfd094256cc9ef32e432be40bcf3` | `e2c6a269c0c62666803503d805dc74250145d2a3` | 55 | `536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda` |
| `MerchantPortalService.java` | `ead8b4223dc8d29890c7f9d3ce0373e23ee876de` | `74d2e5ed15869135f32224a941e8b998ad5bf96e` | 180 | `64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0` |
| `MerchantPortalServiceTests.java` | `f967d5c686196669601888dba1b5823587972cda` | `f6154809365b32c8146d21d3b6b97e507d272507` | 256 | `ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159` |

本席未改任何源文件，只新建本 raw。未 commit/push/PR/merge/部署；未对主索引 `git add`；未新建脚本/日志/临时产物（复核用 `/tmp/m020_b4_r1_verify.diff` 已删）。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M020 的集合（BSM-001/003/114、GEN-001…018、PGM-001）。本轮只用定稿 001–006 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。007 本轮仍「否」，diff 未改 OpenAPI/V200。

B2-r1 定稿（findings.md V2.63）最小处置（摘录，作为本席内容票的尺子）：

- **001** 细化：类注释改为无商户归属（含平台管理员）fail-closed 403，平台监督走各域平台端点；删两处 `orElseGet` 与 `platform-all` 占位；商户号 `orElseThrow` 取，不加 `isPlatformAdmin` 旁路；测试改为桩按真实现抛、断言不触库。
- **002** 细化：保留 `resolvedBy`/`resolvedAt`；既有 `disputeReason` 非空时「既有理由 + 换行 + 商户留言」；测试补裁决后留言不清空、不丢平台理由。
- **003** 细化：合并后长度 >500 → 400 `VALIDATION_FAILED`「异议留言不能超过 500 字」；测试补超长；不扩迁移。
- **004** 细化：服务注入 `IdempotencyService`，主体经 `idempotencyService.execute("merchantPortalSettlementDispute:" + id, key, request, action, ContractSettlementRecord.class)`；控制器改 `ApiRequests.requireIdempotencyKey`；测试用真 `IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper())` 同键两次只写一次。
- **005** 细化：三方法 try/catch `RuntimeException` → `recordFailure(<x-audit-event-failure>, 权限码, businessType, businessId 或 null, null, 幂等键或 null, 错误码)` 再抛；成功审计 metadata 不变；测试补跨商户失败落 `dispute_failed`。
- **006** 细化：去掉状态后缀，固定「结算单当前状态不可发起异议」；测试断言文案不含状态码。

operator_note 只登记，本 diff 未消费、未删除 DTO 字段。

---

## 3. diff 逐块归因（闸 3）

13 hunk，全部变更行都能绑到 V2.63 归属「是」的 ID。混合 hunk 按下表拆到行。无无 ID 行。验证性测试归因到对应 ID（闸 3 明文允许）。版本标记行按派发口径：Service V0.01 → 001…006；Controller V0.01 → 004；测试 V0.01 → 跨单元 TDD 服务 001…006。

| # | 文件 | hunk | 变更行（摘要） | ID | 越界? |
|---|---|---|---|---|---|
| 1 | Controller | `@@ -1,6 +1,7` | `// 文件版本：V0.01` | 004（本文件唯一定稿 ID） | 否 |
| 1 | 同上 | 同上 | `ApiException` → `ApiRequests` | 004 | 否 |
| 2 | Controller | `@@ -47,9 +48,8` | 删自写 `IDEMPOTENCY_KEY_REQUIRED`；`ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey())` | 004 | 否 |
| 3 | Service | `@@ -1,8 +1,10` | 版本标记 V0.01 | 001+002+003+004+005+006（本文件内容 ID 同集） | 否 |
| 3 | 同上 | 同上 | `import IdempotencyService` | 004 | 否 |
| 4 | Service | `@@ -11,18 +13,17` | 删 `Optional` import | 001 | 否 |
| 4 | 同上 | 同上 | 删 `@Transactional` import | 004 | 否 |
| 4 | 同上 | 同上 | 类注释 fail-closed、删全量口径 | 001 | 否 |
| 4 | 同上 | 同上 | 类注释补「同一幂等键只认一次」「三个入口成功/失败均落审计」 | 004+005 | 否 |
| 5 | Service | `@@ -30,19 +31,24` | `DISPUTE_REASON_MAX_LENGTH = 500` | 003 | 否 |
| 5 | 同上 | 同上 | 字段/构造器注入 `IdempotencyService` | 004 | 否 |
| 6 | Service | `@@ -50,25 +56,30` | 删 `orElseGet(listContractsPaged)` 与 `platform-all`；`requireMerchantScope()` | 001 | 否 |
| 6 | 同上 | 同上 | try/catch `recordFailure(contract.view_failed, …, null, null, null, codeOf)` | 005 | 否 |
| 6 | 同上 | 同上 | 成功 `Map.of("count", views.size())` 仍在；businessId 改为 `merchantId`（删占位） | 001（占位）+005（metadata 形状未改） | 否 |
| 7 | Service | `@@ -76,53 +87,74` | 结算列表删 `orElseGet(listAllSettlementsPaged)`/`platform-all` | 001 | 否 |
| 7 | 同上 | 同上 | 结算列表 try/catch `settlement.view_failed`，businessId null | 005 | 否 |
| 7 | 同上 | 同上 | 删方法上 `@Transactional`；`idempotencyService.execute("merchantPortalSettlementDispute:"+id, …, ContractSettlementRecord.class)` | 004 | 否 |
| 7 | 同上 | 同上 | 外层 catch `dispute_failed` 再抛 | 005 | 否 |
| 7 | 同上 | 同上 | 抽出 `disputeOnce`；`merged`/`resolvedBy()`/`resolvedAt()` | 002+004（lambda 承载） | 否 |
| 7 | 同上 | 同上 | `merged.length() > 500` →「异议留言不能超过 500 字」 | 003 | 否 |
| 7 | 同上 | 同上 | 报错去掉 `+ settlement.status()` | 006 | 否 |
| 7 | 同上 | 同上 | 跨商户改为 `requireMerchantScope().equals`（删 `scope.isPresent()` 空口径跳过） | 001 | 否 |
| 8 | Service | `@@ -135,4 +167,14` | `requireMerchantScope()` `orElseThrow(MERCHANT_SCOPE_REQUIRED)` | 001 | 否 |
| 8 | 同上 | 同上 | `codeOf` | 005 | 否 |
| 9 | Tests | `@@ -1,20 +1,29` | 版本 V0.01；`IdempotencyService`/`InMemoryIdempotencyRepository`/`times`/`isNull` | 001…006 TDD（004 主导新增 import） | 否 |
| 10 | Tests | `@@ -26,18 +35,24` | 类注释 fail-closed；`SCOPE_REQUIRED` 真码 | 001+005 | 否 |
| 11 | Tests | `@@ -48,8 +63,21` | 4 参构造（真 IdempotencyService+内存仓储）；settlement 重载带 resolved 字段；`stubOwnOpenSettlement` | 004+002（辅助 001…006） | 否 |
| 12 | Tests | `@@ -58,24 +86,39` | 合同列表断言从不打全量分页、成功审计 businessId=`mch_a` | 001 | 否 |
| 12 | 同上 | 同上 | `callerWithoutMerchantScopeIsRejectedAndAudited`：`thenThrow(SCOPE_REQUIRED)`、不触库、两条 `*_view_failed` | 001+005 | 否 |
| 13 | Tests | `@@ -84,67 +127,130` | 结算列表 `never listAllSettlementsPaged` | 001 | 否 |
| 13 | 同上 | 同上 | 异议不改状态 + 成功 metadata 原样 | 001 边界/005 metadata | 否 |
| 13 | 同上 | 同上 | `disputeKeepsResolutionTrailAndAppendsToExistingReason` | 002 | 否 |
| 13 | 同上 | 同上 | `disputeReasonBeyondColumnWidthIsRejected`（502 拒/500 过） | 003 | 否 |
| 13 | 同上 | 同上 | `disputeWithSameIdempotencyKeyIsProcessedOnce` | 004 | 否 |
| 13 | 同上 | 同上 | `disputeOnForeignSettlementIsForbiddenAndAudited` 落 `dispute_failed` | 005 | 否 |
| 13 | 同上 | 同上 | 不可异议文案 `isEqualTo("结算单当前状态不可发起异议").doesNotContain(status)`，并扩 `voided` | 006 | 否 |
| 13 | 同上 | 同上 | 缺 reason 补失败审计 | 005 | 否 |

无参重载 `listMyContracts()` / `listMySettlements()` 仅作为 hunk 上下文出现，方法体未删。`recordSuccess` 的 `Map.of("merchant_id", …, "reason", …)` 在 hunk 8 上下文中原样保留。

文首版本标记：

| 文件 | 标记 | 点名/归属 ID | 该文件实际内容 ID | 同集? |
|---|---|---|---|---|
| Controller | V0.01 | 004 | 004 | 是 |
| Service | V0.01 | 001…006 | 001…006 | 是 |
| Tests（M022） | V0.01 | 001…006 TDD | 001…006 | 是 |

归因票：**PASS**。无越界行。未改 View/dto/OpenAPI/HTML/锁/findings/ledger/迁移。

---

## 4. 内容审计（最小处置 × 现行依赖）

### 4.1 现行依赖（本席实读，非口称）

- `AuthorizationService.currentMerchantScope:425-432`：有商户号 `Optional.of`，否则直接 `forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行")`，从不 empty。`orElseThrow` 与真实现同义。
- `IdempotencyService.execute:57-65` 五参签名与定稿逐字相符。`runAndComplete:160-177`：有 `TransactionTemplate` 时把 `actionAndComplete`（action 多写 + `complete(OK)`）包进同一事务；无模板（单测 `new`）直跑。失败在 catch 里先 `markRetryableFailure`（`FAILED_RETRYABLE`）再 `throw exception`。reserve 在事务外。
- `AuditEventService.recordSuccess` 7 参序：eventName, permissionCode, businessType, businessId, requestId, idempotencyKey, metadata。`recordFailure` 7 参序：…, errorCode；方法体内 `Map.of("error_code", errorCode)`。
- `ContractSettlementRecord` 字段序：`(id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, projectId, createdAt, updatedAt)`。`JdbcContractRepository.saveSettlement:249-251` upsert 覆写 `dispute_reason`/`resolved_by`/`resolved_at`。`ContractService.resolveDispute:583-587` 裁决后回 `pending_settlement` 并写 resolvedBy/resolvedAt、保留理由。
- `V162__contract_lifecycle.sql:121` `dispute_reason VARCHAR(500)`。
- `ApiRequests.requireIdempotencyKey:12-15`：空键 → `VALIDATION_FAILED`「idempotency_key is required」（全仓控制器同形，含 `request == null ? null : request.idempotencyKey()`）。
- `FieldLabels.translate:535-536`：已含中文则原样透传。OpenAPI L20395/L20418/L20442：`merchant_portal.contract.view_failed` / `settlement.view_failed` / `settlement.dispute_failed`。

### 4.2 逐 ID

**001** — 类注释已改为 fail-closed +「平台监督走各域平台端点」。两处 `orElseGet`/`platform-all` 已删。`requireMerchantScope()` 以 `currentMerchantScope().orElseThrow(MERCHANT_SCOPE_REQUIRED)` 取号。全文无 `isPlatformAdmin`。测试 `callerWithoutMerchantScopeIsRejectedAndAudited` 桩 `thenThrow(SCOPE_REQUIRED)`（非 `Optional.empty()`），`never()` 四类仓储查询。无参重载仍委托 `(1, 500)`。

**002** — `saveSettlement` 传入 `merged, settlement.resolvedBy(), settlement.resolvedAt()`，不再 `reason, null, null`。`status` 仍是 `settlement.status()`。测试断言 `usr_manager` / 固定 Instant / `"平台：押金核算存疑\n商户：违约金多算"`。

**003** — `merged.length() > 500` → `badRequest("VALIDATION_FAILED", "异议留言不能超过 500 字")`（CJK，translate 原样透传）。测试 `"漏算".repeat(251)`=502 拒且 `never saveSettlement`；`repeat(250)`=500 过。未改迁移。

**004** — 服务 4 参注入；`execute("merchantPortalSettlementDispute:" + id, request.idempotencyKey(), request, () -> disputeOnce(...), ContractSettlementRecord.class)`。控制器改共享助手。测试真 `IdempotencyService` + `InMemoryIdempotencyRepository`，同键两次 `saveSettlement`/`recordSuccess` 各 1 次。删方法级 `@Transactional`：生产路径由 `runAndComplete` 的事务模板承接 save+complete(OK)+action 内成功审计；失败留痕在模板外。单测无 TM，与 IdempotencyService 注释「单测 new 构造直跑」一致。

**005** — 三方法均 catch `RuntimeException` → 对应 OpenAPI failure 事件名再抛。读路径 businessId 为 `null`；写路径 `trimToNull(settlementId)`。成功 metadata：`Map.of("count", …)` 与 `Map.of("merchant_id", contract.merchantId(), "reason", reason)` 与 before 同形。跨商户测试落 `dispute_failed` + `CROSS_MERCHANT_FORBIDDEN` + `never save`。失败发生在 `execute` 的 action 内时：`runAndComplete` 先 `markRetryableFailure` 再抛，外层再 `recordFailure` —— 先 FAILED_RETRYABLE 再失败审计。

**006** — 固定中文、不拼接 `settlement.status()`。测试对 `settled`/`disputed`/`voided` 断言全文相等且 `doesNotContain(status)`。`FieldLabels.translate:536` 对已含中文原样透传，不再夹带英文状态。

### 4.3 指定核点

**无参重载 / View / dto** — `listMyContracts()` L54-56、`listMySettlements()` L85-87 仍在。View/dto 两 tree blob 不变。JDBC 集成测试只 `@Autowired ContractRepository`，未 `new MerchantPortalService(…)` 三参，故未因 4 参构造器编译失败；GREEN 能拉起 Spring 上下文，说明第 4 参 `IdempotencyService` bean 可装配。

**Map.of 是否可能 null** — 本 diff 内三处：`Map.of("count", views.size())` / `items.items().size()` 为 int 装箱，非 null；`Map.of("merchant_id", contract.merchantId(), "reason", reason)` 中 `reason` 经 `trimToNull` 非空才到达；`contract.merchantId()` 若 null，`requireMerchantScope().equals(null)` 为 false，先抛 `CROSS_MERCHANT_FORBIDDEN`，到不了成功 Map.of。`codeOf` 对 `ApiException` 取 `code()`（工厂方法均传字面量），否则 `getClass().getSimpleName()`，不返回 null，故 `recordFailure` 内部 `Map.of("error_code", errorCode)` 对本模块抛出类型不会 NPE。

**行为回退 / 业务发明** — 未把状态改成 `disputed`；未加平台全量旁路；未扩列宽；未改 OpenAPI。`reason is required` 英文是 before 已有、FieldLabels `REQUIRED_OR_INVALID` 会译成「原因未填写」，不在 006 最小处置内。operator_note 仍不读，符合只登记。

### 4.4 TDD 与本席复跑

- RED：`m020_b3r1_red.log` 新测试对旧实现，`MerchantPortalService` 仍 3 参、测试传 4 参，编译失败，`RED_MVN_EXIT=1`。与派发「构造器与断言不成立」相符（构造器变更使断言未跑到，闸 3 允许测试归因 004）。
- GREEN：冻结 log `MerchantPortalServiceTests` 10 例 + `MerchantPortalJdbcIntegrationTests` 2 例，`GREEN_MVN_EXIT=0`；`complex_test`、Flyway 顶 V427、schema up to date。
- 本席：环境无 `COMPLEX_DATABASE_*`，不 source `/etc/complex/complex.env`，**不重跑 JDBC**。本席 `mvn -Dtest=MerchantPortalServiceTests test` exit 0；surefire `tests=10 failures=0 errors=0 skipped=0`，十个方法名与源文件 `@Test` 闭集一致。

### 4.5 残留探针（本席 `rg`）

三改动文件：无 `orElseGet`/`platform-all`/`isPlatformAdmin`/`IDEMPOTENCY_KEY_REQUIRED`/`@Transactional`/`Optional.empty()`。`listContractsPaged`/`listAllSettlementsPaged` 仅出现在测试 `never()`。`settlement.status()` 仅用于 DISPUTABLE 判断与原样写回，不进报错串。`idempotencyService.execute` 1 处；`recordFailure` 3 处；`requireIdempotencyKey` 1 处。与 `m020_b3r1_post.log` 同向。

内容票：**PASS**。

---

## 5. 对抗式自问（grilling 收窄口径，不派子代理、不问人）

1. **是否借 001 把平台管理员改回全量？** 否。无 `isPlatformAdmin`/`orElseGet`/`platform-all`。
2. **是否借 OpenAPI「挂起为有争议」把状态改回 disputed？** 否。`settlement.status()` 原样写入。属 007 XM，本 diff 未改 yaml。
3. **execute 失败审计是否写在 lambda 内、早于 FAILED_RETRYABLE？** 否。`recordFailure` 在 `execute` 外层 catch；`markRetryableFailure` 在 `runAndComplete` catch、rethrow 之前。
4. **读失败 businessId 是否误用 merchantId？** 否。两处 view_failed 第 4 参为 `null`。
5. **成功 metadata 是否被改成 merged 理由或加上 status？** 否。仍是 `count` 与 `(merchant_id, reason=商户留言)`。
6. **4 参构造器是否弄坏 JDBC 测试？** JDBC 不手动 new Service；GREEN 上下文启动成功。
7. **003 测试是否只测了留言本身、没测「合并后」超长？** 长度闸打在 `merged.length()`；002 覆盖拼接，003 覆盖阈值。同一 `if`。不升 FAIL。
8. **版本行是否无 ID？** 否。按文件内容 ID 同集归属。

无可升 FAIL 的反例。

---

## 6. 非阻断观察（不入本轮队列、不改票）

- 服务 L134 仍抛 `"reason is required"`（before 已有）。出网经 FieldLabels 译为中文；006 最小处置只收状态后缀。
- 003 测试只 `contains("500")`，未钉全文「异议留言不能超过 500 字」；实现句本身符合定稿。
- `currentMerchantScope` 从不 empty，`orElseThrow` 对真实现为死分支，但是定稿明文「与真实现同义」。
- M022 `MerchantPortalJdbcIntegrationTests` 类注释仍写「平台管理员全量真分页」，测的是仓储而非本服务；写入集合不含该文件，属跨模块观察（B1 已登记）。
- 本席未导出三变量，JDBC 2 例以冻结 GREEN surefire 为准，不以「应能过」代替本席复跑。

---

## 7. 本席结论

| 票 | 结论 |
|---|---|
| 内容（是否按 V2.63 最小处置、是否与现行依赖一致、有无发明/回退、测试是否按真实现） | **PASS** |
| 归因（闸 3：每一变更行是否落到 001–006） | **PASS** |
| 本轮 B4-r1 总结论 | **PASS**（两票均 PASS；任一 FAIL 即 FAIL，本轮未触发） |

本票不是模块 PASS，不是授权进 B5 的单票。B5 仍须另起双席全量审计。本席未修复、未改 findings/ledger/锁/HTML。
