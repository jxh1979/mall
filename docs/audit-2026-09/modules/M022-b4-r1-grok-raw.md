CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M022-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M022 / 共 471，B3 第 1 次之后的 B4。计划名：商户档案、商户类型与商户侧门户／后台验证／第1段。计划文件 2 个＝本模块全部单元。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M022-b4-r1-self-raw.md`（目录 listing 可见该路径已存在，本席按独立性禁令整文件不打开、不哈希其正文）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V2.69 顶部「当前 M022 B2-r1 双PASS定稿」M022-001…007、本席旧 `M022-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、冻结 diff 与两 tree、`MerchantPortalService` V0.01 真实现、`AuthorizationService.currentMerchantScope`／`currentMerchantIdQuietly`、`IdempotencyService.execute` 失败再抛、`AuditEventService` 仅核调用形状、`JdbcContractRepository.listContractsByMerchant`／`listContractSpaceIds`／`listSettlementsByMerchant`、`FieldLabels.translate` 与 `GlobalExceptionHandler:61`、`PageResponse.of`、业务地图 L1310／L1850、criteria-lock V0.11 映射行 `M022 → [BSM-001…PGM-001]`、B3 执行／变异／终态绿日志。ledger 只读 M022 行状态与 B3 次数。未读席甲本轮 self raw。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M022-001…007 是否按 findings V2.69 定稿最小处置实现；断言是否按被测真实现语义；有无为通过而放松断言；001 删平台全量用例后类注释是否只说现行事实；生产代码是否零改动；每一变更行能否归因到归属「是」的 ID。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自问自答，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审；基线＝两 tree id。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落红绿证据并本席对 `complex_test` 终态复跑，不新写测试、不重放会改生产文件的变异。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席 mvn 在前台跑完，未丢后台。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft ask --source` / `graft grep` / `graft callers` 定位 `listMySettlements`／`disputeOnce`／`listContractsByMerchant`／`listContractSpaceIds`／`listSettlementsByMerchant`／`listAllSettlementsPaged` | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 测缝＝服务公开方法与 JDBC 仓储公开查询；007 锁 `FieldLabels.translate` 出网口径而非改服务抛点，符合深模块（翻译留在 handler/FieldLabels） | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环＝变异日志里的具体断言失败行 + 本席终态 mvn 12/12 + 只读 leftover SELECT | 本席不重放会改坏生产文件的 MUT |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→真实现→新断言→MUT 红形态）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑三工具 lock 哈希、`cmp` 冻结 diff、生产 blob 对账、只读 leftover、终态两类测试 | 首次 mvn 因 `JAVA_HOME` 指向 Ubuntu OpenJDK 报 `release version 21 not supported`；改 Temurin 21.0.12.1 后 exit 0。不把失败编译折成业务 FAIL |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 MUT_002…007 红 + GREEN-2 + 本席复跑绿；001 为删除无变异项 | 本席不重写测试 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：2 测试文件、无新抽象 | 不修 |
| `i-have-adhd` | marketplaces/cache 均存在 | 输出模式；raw 锁行优先 | 无 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物。未 `source /etc/complex/complex.env`；测库只导出 `COMPLEX_DATABASE_URL`／`USERNAME`／`PASSWORD` 三变量，URL 钉死 `jdbc:postgresql://127.0.0.1:5432/complex_test`。

graft 本席调用节省合计约 269989 tokens（`listMySettlements`/`disputeOnce`、`listContractsByMerchant`、`listContractSpaceIds`、`listSettlementsByMerchant`、`listAllSettlementsPaged` callers + main grep）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。findings.md 版本 V2.69，本席 `sha256sum` = `bf0cc7ab640f1fc8df355cc2e511c48c382715b3ca6638b678ba9236304cbc45`。160-audit-prompt.md V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`。

| 项 | 值 |
|---|---|
| 模块 | M022 后台验证／第1段（计划 2 文件，有效内容行 227；本轮 B3 写入集合＝本模块 2 个测试文件） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V2.69：M022-001…007 均「是」；只登记 XM-1…3／OBS 不在本 diff |
| B3 | ledger M022 行：`IN_PROGRESS`，B3 已执行 1 次 |
| before（对象类型 tree） | `1393447c3f19e6293ede77e0714499e83af25f18` |
| after（对象类型 tree） | `ffca9860fcfe31222a407f692190fb9a887c9d81` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 1393447c3f19e6293ede77e0714499e83af25f18 ffca9860fcfe31222a407f692190fb9a887c9d81`（无路径过滤） |
| 冻结 diff | `/home/james/audit-tmp/m022_b3_r1.diff` 11579 bytes；SHA256 `cb80ab95be12ecf881aa31da30a53d9246136adc054b0b25cb93cc7a1d5ab754` |
| 本席复核 | `git diff --binary <before> <after> \| sha256sum` 同值；`cmp -s` 冻结文件 **CMP_FROZEN_OK** |
| 实际差异 | 2 files changed, 57 insertions(+), 24 deletions(-)；**13 hunk**；UTF-8 |
| 改动路径 | 仅 `MerchantPortalJdbcIntegrationTests.java`、`MerchantPortalServiceTests.java`。`git ls-tree -r` before/after comm 仅这 2 个 blob 变化 |
| after 测试 blob | JDBC `f69a6734a2b05f6f53ece54e73b0c9edd2bf24e7`；ServiceTests `48db84abbce4f973fd68ae62b7e16251c4f57127`；工作区 `git hash-object` 同值 |
| after 测试内容 SHA256 | JDBC `12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef`；ServiceTests `091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a`（与 run.log 写入核同） |

`git diff --name-only <before> <after> -- backend/src/main/**` 为空。生产代码在本 diff 内零改动。

---

## 2. 生产文件 SHA（变异未残留）

闸 3 对象是两 tree 全文。after tree 相对 before 只有上表 2 个测试 blob。下列生产文件 **before blob = after blob = 工作区 hash-object**：

| 文件 | git blob（before=after=worktree） | 工作区 sha256 |
|---|---|---|
| `MerchantPortalService.java` | `74d2e5ed15869135f32224a941e8b998ad5bf96e` | `64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0` |
| `JdbcContractRepository.java` | `8f47f70c3845060d53cec8f72e35b423e2e37430` | `9ac271149e9b03320ab85c7de9e499c73c4d27dbed136cf92c7b6257f1f8899a` |
| `FieldLabels.java` | `51d75c79e8f1bfb40e3c67d5ac47e5c094b42f78` | `cae7c8437e223223849fc86c1875ddeac75a44052bc36d87de556d409b139d2a` |
| `GlobalExceptionHandler.java` | `07a7429a25df2761bc76777b65f9f11c578b1e90` | `4169e171419606e60565292c465fe4114f17c9581ae75e321ae986ea97bcf587` |
| `AuthorizationService.java` | `4bfae6d2ddded7e2810666bac72bfce818ff9e22` | `990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062` |
| `IdempotencyService.java` | `05f286a34f2ef449ccffa9bd88901f468e425be9` | `b71b659ce968221a0727d5263faa04d2acfac57ba1f08e2e477078fa55b5a275` |
| `AuditEventService.java` | `12a6a8300cfdeea67c4e7c352fb7bca81ee1b807` | `4556a83edbbcf3950b473000dfeb6481dea85f9c03e536d10f648709d4e6336e` |

after tree 内检索 `viewed_mutant`／`OR 1=1`／`1 = 1`／`currentMerchantIdQuietly`（merchantportal 包）／`reason is missing`／`reason missing`：**0 命中**。`platformAllSettlementsPagedRealTotalAndSecondPageReachable` 与「全量合同/结算」在 after 商户门户测试前缀：**0 命中**。

说明：工作区相对 **HEAD/index** 对 `MerchantPortalService.java` 显示 ` M`，因 HEAD blob 仍是已提交的 `ead8b422…`，工作区/两 tree 已是 M020 闭环后的 `74d2e5ed…`。该差异在 before 与 after **两侧相同**，不进入本 B3 diff，不是变异残留。`JdbcContractRepository` HEAD=index=worktree=before=after 全等。

---

## 3. diff 逐块归因（闸 3）

规则：只审 `+`／`-` 行；未改 context 不占 ID。13 hunk，无第三文件、无 binary 块。

### 3.1 `MerchantPortalJdbcIntegrationTests.java`（index `29f2cfac` → `f69a6734`）

| hunk | 定位 | 变更行（摘要） | 归属 ID | 理由 |
|---|---|---|---|---|
| J1 | `@@ -1,4 +1,5 @@` | `+// 文件版本：V0.01` | **M022-001** | 定稿最小处置明文「加版本标记 V0.01」 |
| J2 | `@@ -14,10 +15,11 @@` | 类 javadoc 4 行删、5 行新（商户谓词隔离／不承载平台全量／`listAllSettlementsPaged` 无生产调用方归合同域） | **M022-001** | 定稿「类注释改写为现行事实」 |
| J3 | `@@ -27,7 +29,7 @@` | 注入注释「listAllSettlementsPaged 走 SQL 覆写」→「商户维度读取走单 SQL 覆写」 | **M022-001** | 定稿「行内注释改写为现行事实」 |
| J4a | `@@ -43,23 +45,18 @@` 的 **删除** | 整例 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`（含 `ctr_mp_a/b`、`listAllSettlementsPaged`、真 total／第二页） | **M022-001** | 定稿「删除该用例」 |
| J4b | 同上 hunk 的 **新增** | `// M022-002` 注释 + `merchantScopeContractsStayIsolated`：两商户各存合同、`listContractsByMerchant` 只回本商户、`listContractSpaceIds("ctr_mp_e")` 回空 | **M022-002** | 定稿加 1 例真实库隔离；同一 hunk 内删/增分属两 ID，按行切 |
| J5 | `@@ -70,7 +67,7 @@` | 结算隔离例行内注释去掉「平台全量真分页不污染…」 | **M022-001** | 定稿行内注释改写；该例断言未改 |

### 3.2 `MerchantPortalServiceTests.java`（index `f6154809` → `48db84ab`）

| hunk | 定位 | 变更行（摘要） | 归属 ID | 理由 |
|---|---|---|---|---|
| S1 | `@@ -1,5 +1,5 @@` | `// 文件版本：V0.01` → `V0.02` | **M022-003…007 共有** | 该文件本轮唯一版本行；内容变更全部来自 003–007（001 的版本行在 JDBC）。不是新抽象、不是换框架 |
| S2 | `@@ -15,6 +15,7 @@` | `+import com.complex.platform.common.api.FieldLabels;` | **M022-007** | 仅 007 调用 `FieldLabels.translate` |
| S3 | `@@ -119,6 +120,18 @@` | 无 scope 用例追加 `submitSettlementDispute` → `MERCHANT_SCOPE_REQUIRED` + `never save` + `dispute_failed` | **M022-004** | 定稿原文 |
| S4 | `@@ -130,7 +143,11 @@` | `page.total()==1L` + `recordSuccess(..., "merchant_portal.settlement.viewed", … Map.of("count",1))` | **M022-005** | 定稿补审计与 total |
| S5 | `@@ -141,6 +158,8 @@` | 异议成功例 `verify(authz).requirePermission("merchant.portal.settlement.dispute")` | **M022-006** | 定稿「异议成功用例补该 verify」 |
| S6 | `@@ -170,7 +189,7 @@` | 列宽用例注释补「既有+新留言合并后超」 | **M022-003** | 与新增断言同 ID |
| S7 | `@@ -183,6 +202,20 @@` | 既有 300 + `\n` + 新 250＝551→400 且 never save；再 restub 无理由后保留恰 500 | **M022-003** | 定稿「合并后>500」且「保留恰 500 允许」；restub 是该 ID 的负载行，否则 500 例会吃到 300 字既有理由 |
| S8 | `@@ -247,8 +280,11 @@` | 空理由在原错误码之外追加 `FieldLabels.translate(e.getMessage()).isEqualTo("原因未填写")` | **M022-007** | 定稿原文；未删原 code 断言 |

**越界盘点：** 13 hunk 的全部 `+`/`-` 行均落到 001–007。无顺手优化、无换测试框架、无生产文件、无 HTML/锁/ledger。版本行与 import 行均有归属（J1=001，S1=003–007 共有，S2=007）。

归因票：**PASS**。

---

## 4. 内容审计（定稿最小处置 × 真实现语义）

真实现（只读，不在 diff 内）：

- `MerchantPortalService.listMySettlements`：`requirePermission("merchant.portal.settlement.view")` → `requireMerchantScope()` → `listSettlementsByMerchant` → `PageResponse.of`（total＝全量 size）→ `recordSuccess("merchant_portal.settlement.viewed", …, merchantId, null, null, Map.of("count", items.items().size()))`。
- `submitSettlementDispute`：先 `requirePermission("merchant.portal.settlement.dispute")`，再 `idempotencyService.execute(..., () -> disputeOnce, …)`；失败外层 `recordFailure(..., "dispute_failed", settlementId, key, codeOf)` 后原样抛（`IdempotencyService` L175–176 `throw exception`）。
- `disputeOnce`：`trimToNull(reason)==null` → `"reason is required"`；再查结算/合同；再 `requireMerchantScope().equals(contract.merchantId())`；`merged = existing==null ? reason : existing+"\n"+reason`；`merged.length()>500` → `"异议留言不能超过 500 字"`。
- `requireMerchantScope`：`currentMerchantScope().orElseThrow(MERCHANT_SCOPE_REQUIRED)`。`AuthorizationService.currentMerchantScope` **本身**在无商户号时抛同一码（不是返回 empty）；`currentMerchantIdQuietly` 才是空 Optional。
- JDBC：`listContractsByMerchant` `WHERE merchant_id=:mid ORDER BY created_at DESC, id`；`listSettlementsByMerchant` join 合同同一谓词；`listContractSpaceIds` `SELECT space_id FROM contract_spaces WHERE contract_id=:contract_id`（无绑定行 → 空列表）。
- `GlobalExceptionHandler:61`：`FieldLabels.translate(exception.getMessage())`。`translate("reason is required")`：正则 `^([A-Za-z][A-Za-z0-9_./]*) is (required|invalid)$` 命中 group1=`reason` group2=`required`；`p("reason","原因")`；返回 **「原因未填写」**。
- `listAllSettlementsPaged`：`graft grep --in backend/src/main` 仅定义、**0 生产入边**。`MerchantPortalService` 全文 0 调用。类注释「现无生产调用方，去留归合同域」是现行事实。

### 4.1 M022-001（冲突／删平台全量例 + 注释改现行事实）

删除 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`。新类注释只陈述：本类锁商户维度谓词隔离；服务层不提供平台全量（M020-001 fail-closed）；本类不承载平台全量分页；仓储全量方法去留归合同域。对照业务地图 L1310「不能看平台全局和其他商户数据」、L1850「不看平台全局」。无「商户专区平台管理员全量」残留。未改仓储生产方法。版本行 V0.01 按定稿加上。001 无变异项。

**未放松：** 留下的 `merchantScopeSettlementsStayIsolated` 断言未改，只改注释。

### 4.2 M022-002（合同谓词真实库例）

`merchantScopeContractsStayIsolated`：自建 `mch_mp_e/f` + `ctr_mp_e/f`，`containsExactly` 各 1 id；未绑定铺位的 `listContractSpaceIds` `isEmpty()`。不读库内既有行。与结算例 ID 空间（`c/d`）分立。类上 `@Transactional`，无 `@TestMethodOrder`。本席 leftover SELECT `ctr_mp_%`／`mch_mp_%`／`stl_mp_%` 在 B3 后与本席复跑后均为 **0 行**。不依赖执行顺序。单元素 `containsExactly` 不吃 ORDER BY 风险。

MUT_002（仓储谓词加 `OR 1=1`）：`["ctr_mp_e","ctr_mp_f"]` 漏进 `mch_mp_e`，mvn_exit=1，restored_jr=ok。新断言会红。

### 4.3 M022-003（合并后超 500）

真实现量 `merged.length()`。新分支：既有 `"平".repeat(300)` + 换行 + `"漏".repeat(250)`＝551>500 → `VALIDATION_FAILED` 且 message 含 `"500"` 且 never save。新留言单独 250 并不超限。保留原 502 字新留言拒绝与 `"漏算".repeat(250)` 恰 500 允许；551 之后 restub 无理由结算单，否则 500 例会与 300 字合并。

MUT_003（改量新留言）：L209「Expecting code to raise a throwable」，mvn_exit=1，restored_sv=ok。

### 4.4 M022-004（异议入口无 scope）

追加在原列表 fail-closed 用例末尾。真实现 `disputeOnce` 在查完结算/合同之后才 `requireMerchantScope()`，故必须 stub `findSettlementById`／`findContractById`，否则 mock 默认 empty → `CONTRACT_SETTLEMENT_NOT_FOUND`，锁不到 scope 闸。无 scope stub 为 `currentMerchantScope().thenThrow(SCOPE_REQUIRED)`，与 `AuthorizationService` 真实现（无商户号即抛）同义。期望码 `MERCHANT_SCOPE_REQUIRED`、`never save`、`dispute_failed`（id=`s1`，key=`k0`）。未改列表侧既有断言。

MUT_004（scope 闸改 quietly）：失败在 **新** `assertThatThrownBy` L128（异议未抛），列表两抛仍成立。新断言负载。mvn_exit=1，restored_sv=ok。

### 4.5 M022-005（结算列表成功审计 + total）

`PageResponse.of(1 条, page=1, size=500)` → `total==1`；`recordSuccess` 事件名／权限码／业务类型／`mch_a`／两 null／`count=1` 与 `listMySettlements` L96–103 逐参相同（count 取 **当页 items.size**，本夹具＝1）。未删权限 verify、未删 `never listAllSettlementsPaged`。

MUT_005（事件名改 `viewed_mutant`）：wanted `merchant_portal.settlement.viewed`，mvn_exit=1，restored_sv=ok。

### 4.6 M022-006（异议权限码）

仅成功例补 `verify(authz).requirePermission("merchant.portal.settlement.dispute")`，对应 `submitSettlementDispute` L116。最小。

MUT_006（权限码改坏为 `settlement.view`）：verify 红，mvn_exit=1，restored_sv=ok。

### 4.7 M022-007（出网中文「原因未填写」，不改 M020 服务）

与 B2 定稿及本席 B2-r1 raw 第三节一致：**不采 XM-1 改服务抛点**；锁 `FieldLabels.translate(e.getMessage())=="原因未填写"`。仍保留 `VALIDATION_FAILED`、never save、`dispute_failed`。空理由 `"  "` → `trimToNull` null → 服务仍抛 `"reason is required"`（M020 文件 blob 未变）。单元测试直接调 `FieldLabels.translate`，与 handler L61 同一函数；这是 B2 写死的测缝，不是放松（未把英文断言改成「含中文即可」）。

MUT_007（英文改不可译，实为 `"reason missing"` 一类）：expected `"原因未填写"` but was `"原因missing"`，mvn_exit=1，restored_sv=ok。证明锁的是 required 形态的出网译文，不是任意中文。

007 口径与 B2：**一致**。

---

## 5. 红绿证据与本席复跑

B3 执行器 `/home/james/audit-tmp/m022_b3r1_run.log`（本席实读）：

| 项 | 记录 |
|---|---|
| GREEN-1 | JDBC 2/2、ServiceTests 10/10，`GREEN1_MVN_EXIT=0` |
| MUT_002 | `mvn_exit=1` `restored_sv=ok restored_jr=ok` 目标 `merchantScopeContractsStayIsolated` |
| MUT_003 | `mvn_exit=1` 目标 `disputeReasonBeyondColumnWidthIsRejected` restored ok |
| MUT_004 | `mvn_exit=1` 目标 `callerWithoutMerchantScopeIsRejectedAndAudited` restored ok |
| MUT_005 | `mvn_exit=1` 目标 `merchantSeesOnlyOwnSettlements` restored ok |
| MUT_006 | `mvn_exit=1` 目标 `merchantCanDisputeOwnOpenSettlementWithoutChangingStatus` restored ok |
| MUT_007 | `mvn_exit=1` 目标 `disputeReasonIsRequired` restored ok |
| GREEN-2 | JDBC 2/2、ServiceTests 10/10，`GREEN_MVN_EXIT=0`；surefire `failures="0"` |
| 001 | 删除与注释，无 MUT 行 |

本席终态复跑（Temurin 21.0.12.1，`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`，仅三变量，URL=`complex_test`）：

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0  MerchantPortalServiceTests
Tests run: 2,  Failures: 0, Errors: 0, Skipped: 0  MerchantPortalJdbcIntegrationTests
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
MVN_EXIT=0
```

Flyway：`complex_test` PostgreSQL 16.14，schema v428，无新迁移。复跑后再 SELECT leftover：**0 行**。`@Transactional` 回滚成立。

未重放变异（会改生产文件）。变异未残留由 §2 blob 全等证明。

---

## 6. 对抗式自问（grilling 结构，自答）

1. **新例是否吃库内脏数据或顺序？** 否。自建唯一 `mch_mp_*`／`ctr_mp_*`／`stl_mp_*`；两 JDBC 例 ID 空间分立；`containsExactly` 单 id； leftover 0。JUnit 无顺序注解。
2. **为绿而放松？** 否。001 是按冲突删除，不是把全量断言改弱。003–007 均加严。005／006／007 保留原 verify。
3. **007 是否测错缝、逼改 M020？** 否。B2 明确锁 translate 出网、不采 XM-1。服务 blob 未变。
4. **004 是否因 `thenThrow` 而测不到 `orElseThrow`？** `currentMerchantScope` 真实现已抛；测缝是异议路径仍调用该闸。MUT_004 去掉／quietly 后新断言红。XM-3 已把纯 `orElseThrow` 冗余只登记。
5. **类注释是否仍宣称全量？** after 检索 0。现行事实（0 生产调用方）已用 graft 核。
6. **生产是否被 MUT 留下？** after=before blob，工作区生产 hash-object 同 after。

ponytail-review：**Lean already. Ship.** `net: -0`。无可删越界抽象。002 复用本类 `contract()` 夹具；003 restub 必要；004 的结算 stub 为到达 scope 闸所必需。

---

## 7. 票

| 维度 | 票 | 一句话 |
|---|---|---|
| 归因（闸 3） | **PASS** | 13 hunk 全部 `+`/`-` 落到 M022-001…007；版本行与 FieldLabels import 有归属；无越界行 |
| 内容 | **PASS** | 七条均按 V2.69 最小处置；断言贴真实现；无放松；001 注释只述现行事实；生产零改动；007 与 B2 锁 translate／不改 M020 一致；变异红且还原；本席复跑 12/12 且 leftover 0 |
| 本步 B4-r1 | **PASS** | 两票合取本席侧均为 PASS |

不是模块 PASS，不是授权跳 B5。合取仍须席甲本轮 B4 独立票。本席 FAIL 否决权本轮不行使。

冻结判据引用（仅锁内）：BSM-114、GEN-001、GEN-003、GEN-005、GEN-006、GEN-011、GEN-016、PGM-001。未主张锁外判据。

---

## 8. 本轮完整 diff（与冻结文件逐字节相同）

```diff
diff --git a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
index 29f2cfac..f69a6734 100644
--- a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.merchantportal;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -14,10 +15,11 @@ import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.transaction.annotation.Transactional;
 
 /**
- * AUD-005 收尾(#187 复核残留,四域之外):商户专区平台管理员「全量合同/结算」真分页。
- * 坐实平台全量结算 listAllSettlementsPaged = SQL COUNT 取真 total(跨所有商户)+ LIMIT/OFFSET 第二页可达;
- * 商户自 scope 的 listSettlementsByMerchant 仍谓词隔离(只见本商户、无 LIMIT),不受影响。
- * 合同侧平台全量走既有 listContractsPaged(ContractJdbcIntegrationTests 已覆盖 501/多页)。
+ * 商户专区(3.18)读路径的真实库验证:合同仓储的商户维度读取必须按商户谓词隔离——
+ * 「我的合同」listContractsByMerchant(+ listContractSpaceIds)与「我的对账」listSettlementsByMerchant
+ * 都只回本商户的数据(业务地图「商户侧页面…不看平台全局」)。
+ * 商户专区服务层不提供平台全量口径(M020-001:无商户归属账号 fail-closed),故本类不承载平台全量分页用例(M022-001);
+ * 仓储 listAllSettlementsPaged 现无生产调用方,去留归合同域。
  */
 @SpringBootTest(properties = {
         "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
@@ -27,7 +29,7 @@ import org.springframework.transaction.annotation.Transactional;
 @Transactional
 class MerchantPortalJdbcIntegrationTests {
 
-    // 注入公有接口: 运行时是 JdbcContractRepository bean, listAllSettlementsPaged 走 SQL 覆写。
+    // 注入公有接口: 运行时是 JdbcContractRepository bean, 商户维度读取走单 SQL 覆写。
     @Autowired
     private com.complex.platform.contract.ContractRepository repository;
 
@@ -43,23 +45,18 @@ class MerchantPortalJdbcIntegrationTests {
                 null, null, null, "proj_1", at, at);
     }
 
+    // M022-002:「我的合同只见本商户」——真实 SQL 的 merchant_id 谓词(服务层 listMyContracts 的两条读路径)。
     @Test
-    void platformAllSettlementsPagedRealTotalAndSecondPageReachable() {
+    void merchantScopeContractsStayIsolated() {
         Instant now = Instant.now();
-        repository.saveContract(contract("ctr_mp_a", "mch_mp_a", now));
-        repository.saveContract(contract("ctr_mp_b", "mch_mp_b", now.plusSeconds(1)));
-        repository.saveSettlement(settlement("stl_mp_1", "ctr_mp_a", now));
-        repository.saveSettlement(settlement("stl_mp_2", "ctr_mp_b", now.plusSeconds(1)));
+        repository.saveContract(contract("ctr_mp_e", "mch_mp_e", now));
+        repository.saveContract(contract("ctr_mp_f", "mch_mp_f", now));
 
-        var page1 = repository.listAllSettlementsPaged(1, 1);
-        var page2 = repository.listAllSettlementsPaged(2, 1);
-        // 真 total: 跨所有商户 COUNT(*),不是当前页条数。
-        assertThat(page1.total()).isGreaterThanOrEqualTo(2L);
-        assertThat(page1.items()).hasSize(1);
-        assertThat(page2.items()).hasSize(1);
-        // 第二页可达且与首页不重(稳定排序 created_at DESC, id)。
-        assertThat(page2.items()).extracting(ContractSettlementRecord::id)
-                .doesNotContainAnyElementsOf(page1.items().stream().map(ContractSettlementRecord::id).toList());
+        assertThat(repository.listContractsByMerchant("mch_mp_e"))
+                .extracting(ContractRecord::id).containsExactly("ctr_mp_e");
+        assertThat(repository.listContractsByMerchant("mch_mp_f"))
+                .extracting(ContractRecord::id).containsExactly("ctr_mp_f");
+        assertThat(repository.listContractSpaceIds("ctr_mp_e")).as("未绑定铺位的合同回空列表").isEmpty();
     }
 
     @Test
@@ -70,7 +67,7 @@ class MerchantPortalJdbcIntegrationTests {
         repository.saveSettlement(settlement("stl_mp_c", "ctr_mp_c", now));
         repository.saveSettlement(settlement("stl_mp_d", "ctr_mp_d", now));
 
-        // 商户自 scope 仍只见本商户(平台全量真分页不污染隔离口径)。
+        // 「我的对账」只见本商户:结算单经合同 merchant_id 谓词隔离。
         assertThat(repository.listSettlementsByMerchant("mch_mp_c"))
                 .extracting(ContractSettlementRecord::id).containsExactly("stl_mp_c");
     }
diff --git a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
index f6154809..48db84ab 100644
--- a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.merchantportal;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -15,6 +15,7 @@ import static org.mockito.Mockito.when;
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
+import com.complex.platform.common.api.FieldLabels;
 import com.complex.platform.common.api.PageResponse;
 import com.complex.platform.common.idempotency.IdempotencyService;
 import com.complex.platform.contract.ContractRecord;
@@ -119,6 +120,18 @@ class MerchantPortalServiceTests {
                 "merchant_portal_contract", null, null, null, "MERCHANT_SCOPE_REQUIRED");
         verify(audit).recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
                 "merchant_portal_settlement", null, null, null, "MERCHANT_SCOPE_REQUIRED");
+
+        // M022-004:异议入口同样 fail-closed(disputeOnce 内的商户 scope 闸),不写库并落 dispute_failed。
+        when(contractRepository.findSettlementById("s1"))
+                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
+        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
+        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k0", null, "无归属账号留言")))
+                .isInstanceOfSatisfying(ApiException.class,
+                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
+        verify(contractRepository, never()).saveSettlement(any());
+        verify(audit).recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
+                "merchant_portal_settlement", "s1", null, "k0", "MERCHANT_SCOPE_REQUIRED");
     }
 
     @Test
@@ -130,7 +143,11 @@ class MerchantPortalServiceTests {
         PageResponse<ContractSettlementRecord> page = service.listMySettlements(1, 500);
 
         assertThat(page.items()).extracting(ContractSettlementRecord::id).containsExactly("s1");
+        assertThat(page.total()).isEqualTo(1L);
         verify(authz).requirePermission("merchant.portal.settlement.view");
+        // M022-005:成功路径落 merchant_portal.settlement.viewed 审计。
+        verify(audit).recordSuccess(eq("merchant_portal.settlement.viewed"), eq("merchant.portal.settlement.view"),
+                eq("merchant_portal_settlement"), eq("mch_a"), isNull(), isNull(), eq(Map.of("count", 1)));
         verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
     }
 
@@ -141,6 +158,8 @@ class MerchantPortalServiceTests {
         ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k1", null, "金额与合同约定不符"));
 
+        // M022-006:异议入口先过 merchant.portal.settlement.dispute 权限码。
+        verify(authz).requirePermission("merchant.portal.settlement.dispute");
         assertThat(disputed.status()).isEqualTo("pending_settlement");
         assertThat(disputed.disputeReason()).isEqualTo("金额与合同约定不符");
         ArgumentCaptor<ContractSettlementRecord> captor = ArgumentCaptor.forClass(ContractSettlementRecord.class);
@@ -170,7 +189,7 @@ class MerchantPortalServiceTests {
         assertThat(disputed.status()).isEqualTo("pending_settlement");
     }
 
-    // M020-003:合并后超过 dispute_reason 列宽(500)→ 400 且不写库;恰为 500 允许。
+    // M020-003 / M022-003:新留言单独超列宽、或「既有理由 + 新留言」合并后超列宽(500)→ 400 且不写库;恰为 500 允许。
     @Test
     void disputeReasonBeyondColumnWidthIsRejected() {
         stubOwnOpenSettlement();
@@ -183,6 +202,20 @@ class MerchantPortalServiceTests {
                 });
         verify(contractRepository, never()).saveSettlement(any());
 
+        // M022-003:既有理由 300 字 + 换行 + 新留言 250 字 = 551 > 500;新留言单独并不超限,量的是合并后长度。
+        when(contractRepository.findSettlementById("s1")).thenReturn(Optional.of(
+                settlement("s1", "c1", "pending_settlement", "平".repeat(300), "usr_manager",
+                        Instant.parse("2026-09-01T08:00:00Z"))));
+        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k3b", null, "漏".repeat(250))))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(e.getMessage()).contains("500");
+                });
+        verify(contractRepository, never()).saveSettlement(any());
+
+        when(contractRepository.findSettlementById("s1"))
+                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
         assertThat(service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k4", null, "漏算".repeat(250))).disputeReason())
                 .hasSize(500);
@@ -247,8 +280,11 @@ class MerchantPortalServiceTests {
 
         assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k8", null, "  ")))
-                .isInstanceOfSatisfying(ApiException.class,
-                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    // M022-007:服务层机读英文经 GlobalExceptionHandler → FieldLabels.translate 出网,用户看到的是中文。
+                    assertThat(FieldLabels.translate(e.getMessage())).isEqualTo("原因未填写");
+                });
         verify(contractRepository, never()).saveSettlement(any());
         verify(audit).recordFailure(eq("merchant_portal.settlement.dispute_failed"), any(), any(), eq("s1"),
                 isNull(), eq("k8"), eq("VALIDATION_FAILED"));
```
