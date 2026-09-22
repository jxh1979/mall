CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M043 B1 Grok 完整报告 V0.01

- 席位：Grok（乙）
- 步骤：B1 修复前全量审计
- 模块：M043 `高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证／第1段`
- 生命周期（ledger 只读）：IN_PROGRESS；B3 已执行次数=0
- 正本：`/home/james/complex-management` 分支 `main`；工作区 HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`；操作员给出的 origin/main=`27ad0e5fc25eb02e33cfe4743fb50072c2488593`（本席实算 `git rev-parse origin/main` 与之相同）
- 提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 锁：`docs/audit-2026-09/plan/criteria-lock.md` 版本行 V0.14；runbook V0.02
- 三 HTML：只读，未改
- 本席未读本次 self raw、未接触 CLI 缓冲；未改 ledger / findings / 源 / 测试 / HTML / DB / 主 index
- 映射 ID 自算：106（unique 106，无重复）
- 有效内容：`git ls-files -z -- <3 paths> | xargs -0 cat | wc -l` = **865**（计划冻结 660=origin/main 三文件行数，本席不改计划）
- 结论：**FAIL**（无条件；有工程缺陷，见 GR-01…GR-08）

## 0. 技能自检（prompt §6 收窄，无第三席）

已读并按收窄口径使用：

| 技能 | 本席路径 | 用法 |
|---|---|---|
| using-superpowers | 已加载 | 开工技能门 |
| grilling / grill-me | `mattpocock-skills/.../grilling` 与 `grill-me` 均存在 | 只借追问结构，事实自读，不派子代理、不等操作员 |
| verification-before-completion | 已加载 | PASS/FAIL 前实跑测试与哈希 |
| systematic-debugging | 已加载 | 只借读错→复现→追数据流；禁止打点改码 |
| diagnosing-bugs | 已加载 | 用现有单测+源码控制流作红环，不写产品文件 |
| codebase-design | 已加载 | 本页浅封装、裁决在后端模块 |
| ponytail-review | 已加载 | 只列可删/可收窄，不改码 |
| writing-for-agents | 已加载 | 本 raw 给 Codex/B2 读 |
| i-have-adhd | 存在 | 报告结构编号；首行被 V0.17 哈希行覆盖 |
| git-guardrails-claude-code | 存在 | 本席只读，未动 git |

缺口（不冒充已调用）：`batch-grill-me`、`ubiquitous-language` 在本环境未找到。`tdd` / `test-driven-development` / `ponytail`（B3）/ `receiving-code-review` 属修复席，B1 不用。未派子代理、未用 MCP。

## 1. 锁哈希（本席独立实算）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`

| 源 | 值 |
|---|---|
| ① 实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| ② sidecar `plan/criteria-lock.sha256` | 同上 |
| ③ ledger 顶部首行 `CRITERIA-LOCK-SHA256:` | 同上 |

ledger 另有历史行 L7088 `cf25d7168a384952200e17af747a2a59f5a8247fd7350f795604ee72319dfce6`（非顶部基准）。闸 1 口径取顶部。本报告第一行=①。本席不写 ledger。

## 2. 范围、文件覆盖、旧 PR450

计划三文件（`plan/modules.md` FILES 段）：

| 文件 | HEAD 行 | origin/main 行 | blob HEAD | blob origin/main |
|---|---:|---:|---|---|
| `apps/web/src/admin/pages/SupervisorReviewPage.tsx` | 467 | 392 | `cedd05d2…` | `c7089303…` |
| `apps/web/src/admin/pages/SupervisorReviewPage.test.tsx` | 323 | 193 | `1bd05611…` | `145e21f3…` |
| `apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx` | 75 | 75 | `7d30eb32…` | 同 HEAD |
| 合计 | **865** | **660** | | |

三文件均在 `origin/main` 树内；**不是「文件不存在」**。#450 相关两文件（tsx + test.tsx）的 **HEAD 内容未发布到 origin/main**（chain.test 已与 origin/main 同 blob）。origin/main..HEAD 独有增量（只读）：`38314f57`…`feb2417e` 等前端提交。本席审 **工作区现文全文件**，不只审 #450 diff，也不预设那次 PR 通过。

后端审批/会话依赖按操作员说明已由 #481 发布；本席对照现工作区 `SupervisorReviewService.java` / `SupervisorReviewController.java`（只读，不纳入本模块修复面）。

未跑旧 `complex_test`、未跑新 432 迁移。只跑现有前端单测与 `tsc -b --noEmit`。

### 2.1 可执行证据

```
cd apps/web && npm run test:unit -- src/admin/pages/SupervisorReviewPage.test.tsx src/admin/pages/SupervisorReviewPage.chain.test.tsx src/customer/i18n/statusLabels.test.ts
```

- exit 0；33 passed（page 21 + chain 2 + statusLabels 10）；Duration 829ms
- `npx tsc -b --noEmit` exit 0

**测试通过本身不证明实现正确**（GEN-003 / 提示词 2.3 禁「测试通过所以是对的」）。见 GR-01/GR-02/GR-07：夹具避开了真实后端目录与放行主路径。

全文件已读：tsx 1–467、test 1–323、chain.test 1–75。无 OVERSIZE。

## 3. 第一性原理（2.3 三问）

### 3.1 这段东西为什么必须存在？

删掉本页，三份 HTML 里至少这些做不到（操作员在本后台页完成主管复核）：

- PGM-014 原文：`重要动作可追溯：谁发起、谁审批、谁复核、谁放行，敏感操作进复核队列。`
- PGM-007 原文：`合同不是只归档，还会决定后续业务能不能走。`
- BEQR-063 原文含「免单」须审批/双证据（酒吧高危动作）。
- PGM-041：`敏感动作可要求上一级、上两级或最高权限审批。` 以及 `提交人不能审批自己的单据。`
- PGM-066：界面全中文。
- BSM-007：`平台管理员即使同时具备业务权限，也不得审批自己发起的上述事项。`

答得上 → 有业务上位。不是无主代码。

### 3.2 声称的行为有没有被实际执行验证过？

页面自称（tsx L14–16、L37、L61 注释）：默认阻断、评估、审批放行须幂等键/证据/意见；结果只交中文句子；评估用 `governance.supervisor_review.manage`、审批用 `approve`。

已跑单测覆盖：评估中文结果、manage 无 approve 时按钮禁用、驳回 decision=rejected、转交 body、若干自审/转交预检、KPI 聚焦、抽屉 Esc。

**未**被现有测试执行到的关键声称：

- 放行成功（无 `decision=approved` 成功断言）
- 放行后仍 `pending_review` 的逐级节点
- 用真实后端策略目录（`bill_waiver` / `refund` / `one_level_up` 等）渲染中文
- 申请人/受益人禁自**审**（代码有预检，测试只覆盖转交/终审人，未点放行）

### 3.3 哪个前提没验证就当真了？

1. `formatBusinessStatus` 能翻本页真实动作/层级/证据类型 → **假**（见 GR-01）。
2. 待审 `actionType` 等于策略表 `actionType` 才能放行 → 与后端按 `policy_code` 取证、准入动作 `bar.bill_waiver` **不一致**（GR-02）。
3. `requiredEvidenceTypes.length>0` 即须附件编号 → 后端只认 `evidence_attachment`（GR-02）。
4. 转交「账号」= 后端 `initiatedBy`/`beneficiaryId`/`forwardedTo` 的主体 id（GR-08）。
5. `makeResult` 默认句「已审批通过」= 整单终态 approved → 后端中间节点仍返回 pending（GR-03）。
6. 所有待审都已登记直接受益人才能转交 → 后端仅资金/授权/退款/免单/调账类要求（GR-04）。

## 4. 顺从式：106 项逐项核对

映射原文（lock L6428）：  
`M043 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-182, BEQR-183, BEQR-184, BEQR-185, BEQR-186, BEQR-187, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-073, BSM-074, BSM-084, BSM-085, BSM-086, BSM-087, BSM-088, BSM-114, BSM-115, BSM-120, GEN-001 … GEN-018, PGM-001 … PGM-022, PGM-024, PGM-026, PGM-027, PGM-031 … PGM-036, PGM-040 … PGM-044, PGM-048, PGM-059, PGM-060, PGM-065, PGM-066]`  
自算 106、无重复。

判定：本三文件有实现点则对照原文；无实现点记「本模块无直接代码、不与原文冲突」。不把无实现点写成缺陷。锁外主张不立项。

### 4.1 GEN（18）

| ID | 核对 | 结果 |
|---|---|---|
| GEN-001 | 本页有 PGM-014/007 上位；非空转发 | 成立；无「整页该删」 |
| GEN-002 | 放行/转交/文案与后端主路径 | **GR-02/03/04/05/06/08** |
| GEN-003 | 33 测通过但不能失败于真实英文枚举、无成功放行 | **GR-07**（并支撑 GR-01/02） |
| GEN-004 | 表单 trim/空检；信任边界在后端 | 本页预检不替代后端；未单列缺陷 |
| GEN-005 | 按钮 `manage`/`approve` 与 Controller 门禁同码；清单隔离在后端 `listPending` | 前端未自造权限；隔离不靠 UI 隐藏。无本页越权入口 |
| GEN-006 | 状态机在后端事务；前端不写库 | 无本页部分写入 |
| GEN-007 | 请求带 `idempotencyKey`；`AdminApi.normalizeRequestBody` 也会补 | 正常路径有键。同毫秒双击见观察，不入 finding |
| GEN-008 | 动作名/证据类型/账号/受益人规则跨层 | **GR-02/04/05/08** |
| GEN-009 | `useAdminAction` catch 后中文 fallback；预检失败不发请求 | 免单驳回缺附件会先过预检再 400，**GR-05** |
| GEN-010 | 无密钥/明文秘密 | 通过 |
| GEN-011 | `AuditTrailHint` 事件名进折叠技术详情；写路径靠后端 audit | 技术详情英文事件名按既有「内部标识」折叠，不单列；业务表英文见 GR-01 |
| GEN-012 | 待审 limit=200 在后端 | 本页无无界循环 |
| GEN-013 | 无构建文件 | 不适用 |
| GEN-014 | 无 SQL | 不适用 |
| GEN-015 | 键盘行、抽屉 Esc、中文标签 | 策略/待审格英文 **GR-01**；「转交」兼选择待审见观察 |
| GEN-016 | 无独立用户文档于本三文件 | 注释中的 PGM 编号不是完成声明 |
| GEN-017 | 本席分类见第 6 节 | 无伪装添加 |
| GEN-018 | 本报告依据 HTML/命令/源码控制流 | 本文件 |

### 4.2 BEQR（27）

| ID | 适用 | 结果 |
|---|---|---|
| BEQR-063 | 策略/评估含免单等高危 | 页可评估 `bill_waiver`；表渲染英文 **GR-01**；放行匹配 **GR-02**。HTML 原文见 finding。双证据照片+视频不在本页采集，属其他模块 |
| BEQR-074 | 隐私桌台 | 无代码，不冲突 |
| BEQR-143…159 | 主体/状态枚举 | 无代码，不冲突 |
| BEQR-166 | 合同审批链（三类模板/平行线） | 本页只链到 `/admin/governance/approval-chain`；链生成在后端。不在本页发明合同模板。无冲突 |
| BEQR-182…187 | 考勤薪资 | 无代码 |
| BEQR-189 | 管理费 | 无代码 |

### 4.3 BSM（21）

| ID | 适用 | 结果 |
|---|---|---|
| BSM-001 | 整体说明 | 无发明新系统边界 |
| BSM-003 | 操作有引导、权限分岗 | 有表单/二次确认/权限钮；转交按钮兼选待审（观察）。缺陷以 GR 为准 |
| BSM-004/005/006 | 存档删除/分步新建/动态码 | 无代码 |
| BSM-007 | 管理员不得自批资金/最终审批 | 前端 `sameAccount`+后端 `SUPERVISOR_SELF_REVIEW_FORBIDDEN`。测试未打放行自审，**GR-07** |
| BSM-009/028/029 | 基础管理/主要功能/和谁有关 | 入口卡片级，无本页发明 |
| BSM-072…074 | 商户门户 | 无代码 |
| BSM-084…088 | 员工手机 | 无代码 |
| BSM-114 | 入口只看该看的 | 待审过滤在后端 `visibleTo`；前端照单渲染。不在前端另造跨项目开关 |
| BSM-115 | 子系统连接 | 链出审批链页，细化 |
| BSM-120 | 已确认规则 | 不在本页改规则文本 |

### 4.4 PGM（40）

| ID | 适用 | 结果 |
|---|---|---|
| PGM-001…006,008…013,015…022,024,027,031…036,042…044,048,059,060,065 | 卡片/他域章节 | 本三文件无对应实现，不冲突 |
| PGM-007 | 合同和准入卡片「后续业务能不能走」 | 本页是卡住后的复核 UI，应存在 |
| PGM-014 | 留痕复核卡片 | 本页正是该入口的网页实现 |
| PGM-026 | 商户主体「主要功能」（非合同章） | 无招商/四类主体绑定 UI，不冲突 |
| PGM-040 | 单一功能一个权限点 | evaluate=manage、approve/reject/forward=approve，与 Controller 一致 |
| PGM-041 | 权限规则：隔离、自审、高危授出 | 隔离后端；自审前端+后端。本页不授出五类权限 |
| PGM-066 | 全中文 | **GR-01 冲突** |
| （锁内但未映射到 M043 的 PGM-029） | 合同章「主要功能」含全平台审批/转发原文 | 本页实现该模型的网页。finding 引用其 HTML 原文（冻结条目 a），不新增判据 |

顺从式结论：映射已逐项过；缺陷集中在 GEN-002/003/008/015 与 PGM-066，外加与已发布后端审批规则不对齐。

## 5. 对抗式（假定实现错）

自问（grilling 收窄，不派人）：

1. 刚评估能否自批？`validateDecisionActor`：无 pending 行时 `initiator = user.id`，`sameAccount` 拦住。后端 `reviewerId.equals(initiatedBy)`。正常路径挡住。
2. 受益人自批？有 `schedule.beneficiaryId` 且 `sameAccount`。刚评估后依赖 reload 是否带回 beneficiary；后端仍拦。不单列（后端兜底）。
3. 用登录名转给自己？当前用户即申请人时拦住（有测）。**第三人填申请人登录名不拦** → GR-08。
4. 待审 `bar.bill_waiver` 点放行？客户端 `actionType ===` 失败 → GR-02。测试把该拦当作正确。
5. 默认动作合同违规、不填附件点放行？`requiredEvidenceTypes` 非空即要附件；后端合同策略要的是 `violation_summary`/`contract_snapshot` 不是 `evidence_attachment` → GR-02。
6. 中间节点通过？文案仍「已审批通过」→ GR-03。
7. 合同违规无受益人转交？前端死拦；后端 `requiresBeneficiary("contract_violation")` 为 false → GR-04。
8. 免单评估不填受益人？前端发出；后端 `BENEFICIARY_REQUIRED` → GR-06。
9. 免单驳回不填附件？前端只 `validateDecisionActor`；后端 `requireDecisionEvidence` → GR-05。
10. 无 view 只 manage？读接口 403，页 `AdminDataState` 失败。不发明。
11. 伪造 reviewerLevel？前端不传；后端从会话取。通过。
12. 跨项目看待审？前端不带隔离参数；后端 `listPending` 过滤。通过（本页）。
13. 并发双击转发？`busyAction` 通常挡住；同毫秒 `Date.now()` 观察保留。
14. KPI 聚焦藏表单：有意；不构成越权。

## 6. Findings

每条：临时编号、文件行、HTML/GEN 原句、2.1 分类、2.3 依据、冻结 ID、工程缺陷、是否添加、最小处置（①删 ②收窄 ③改已有点）。B1 不修。

### GR-01 策略表与待审表对用户展示英文枚举

- 文件：`SupervisorReviewPage.tsx` L355–358 策略表 `formatBusinessStatus(row.actionType)` / `requiredEvidenceTypes`；L397–401 待审 `actionType`/`objectType`/`approvalLevel`。
- 冻结原文 PGM-066：`已拍板：界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字（菜单、字段、状态、枚举、按钮、提示、报错、导出、短信、日期时间）必须全中文，不得出现英文；可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样，此外一律中文；强制规则，适用全部终端与模块。` 同页另写：`不得以“这是标识”为由保留英文。`
- GEN-015 原文：`用户界面须保持……展示文案与项目声明的语言约定一致。`
- 证据（类型 1+4）：
  - `formatBusinessStatus`：`return STATUS_LABELS[code] ?? code;`（`statusLabels.ts` L255–259）。
  - 现测 `statusLabels.test.ts`：「未知码原样兜底」`some_unmapped_code` 原样返回。
  - 本席枚举 STATUS_LABELS：**无** `bill_waiver`/`refund`/`ar_ap_adjustment`/`one_level_up`/`two_levels_up`/`specific_role`/`top_authority`/`bar.bill_waiver`/`reason`/`evidence_attachment`/`contract`/`contract_snapshot`。有 `contract_violation`/`manual_release`/`pending_review`/`active`。
  - 后端 `listPolicies()` 固定目录含 `bill_waiver`/`refund`/`ar_ap_adjustment`/`staff.shift.publish` 等（`SupervisorReviewService.java` L51–67）。本页加载即用该目录填表。
  - 本模块测试夹具 POL-1 `required_evidence_types: ["contract"]`（test.tsx L9），表单元格会走出 `contract`。所谓「不含英文枚举」的测只 `not.toContain("contract_violation")`（L301），**不会对 `contract`/`one_level_up` 失败**。
- 分类：**冲突**（展示层英文 vs PGM-066 明文）。
- 工程缺陷：**是**。添加：**否**。
- 最小处置：收窄本页展示映射（或补本页用到的码表），下拉已有的中文标签复用于表/待审/抽屉；证据类型用中文（原因/附件/合同快照等）。不新增业务动作。

### GR-02 放行客户端策略匹配与证据规则与已发布后端不一致（含默认合同违规主路径）

- 文件：`SupervisorReviewPage.tsx` L286–300 `validateApprove`：
  - `rows.find((row) => row.actionType === (pending?.decision.actionType ?? actionType))`
  - `needsAttachment = requiredEvidenceTypes.length > 0` → 「请填写证据附件编号」
  - 对不上则 「找不到对应复核策略，不能放行」
- 测试原文（test.tsx L242–251）把 `bar.bill_waiver` 对不上策略当作正确拦截。
- 后端：`approve` 用 `findPolicyByCode(pending.policyCode())`；`requireDecisionEvidence` 只对 `required.contains("evidence_attachment")` 要附件（Service L982–991）。合同策略 `requiredEvidenceTypes = List.of("violation_summary", "contract_snapshot")`（L61），**不要** `evidence_attachment`。准入免单动作名为 `bar.bill_waiver`（`isMoneyAction` L695–696），策略目录动作名是 `bill_waiver`。
- HTML（锁内 PGM-029，合同章「主要功能」）：`本条适用于退款、免单、调账、临时权限申请、高危授权、人工补录复核及其他一切业务审批`；`任一节点驳回则整单退回起草人`。BEQR-063：`高危动作（需审批/双证据）……免单……`。GEN-002：`声明的入口、主路径和错误路径须能构建或执行，并与其接口……一致。` GEN-008：前后端口径须一致。
- 正常路径：
  1. 默认表单动作即 `contract_violation`（tsx L120、L417）。评估成功后点放行、不填附件 → 前端拦「请填写证据附件编号」，后端本不会因缺 `evidence_attachment` 拒。
  2. 待审出现酒吧免单 `bar.bill_waiver`（测试夹具 review_p1 即此）→ 前端永不发 `/approve`。
- 分类：**细化**（审批放行的下位实现）下的工程缺陷，不是新业务发明。
- 工程缺陷：**是**。添加：**否**。
- 最小处置：放行证据预检与后端 `requireDecisionEvidence` 对齐（只对 `evidence_attachment` 要编号）；匹配策略改用 `policyCode` 或与后端同一动作别名，**删除**「对不上 actionType 就不发」这条比后端更严的客户端闸。测试「对不上就不能放行」一并改成能失败的真实后端口径。

### GR-03 中间节点通过仍提示「已审批通过」

- 文件：tsx L71–74 `makeResult`：非 forward/evaluate/rejected 一律 `` `复核单 ${reviewId} 已审批通过` ``，不读 `reviewStatus`/`decision`。
- 后端 Service L247–248：`if (!anyRejected && !ApprovalChainService.allApproved(after)) { return pending; }` 复核单保持 `pending_review`。
- HTML PGM-029：`系统按组织层级自动补全……逐级审批`；`主线与各条平行线全部通过合同才生效`（同条合同段）。PGM-066 禁止把未完成状态显示成完成。GEN-002。
- 正常路径：`contract_violation` 策略 `two_levels_up`（Service L61）。第一节点通过 ≠ 整单通过。本页是该评估的默认动作。
- 依据：类型 1（HTML）+ 源码无歧义控制流。现有 21 个页测无「approve 返回 pending」分支（GR-07）。
- 分类：**冲突**（完成文案 vs 逐级未完成）。
- 工程缺陷：**是**。添加：**否**。
- 最小处置：按返回的 `reviewStatus`/`decision` 区分「本节点已通过，单据仍待审」与「整单已通过」；驳回句可保留。

### GR-04 转交无条件要求已登记直接受益人，后端仅资金/授权类要求

- 文件：tsx L235–238 `if (!pending?.schedule?.beneficiaryId) { setFormError("直接受益人未登记，不能转交"); return; }`
- 测试原文 L141：`合同违规待审行没有直接受益人 → 转交被拦且不发请求`（review_p2 `beneficiary_id: null`，`action_type: contract_violation`）。
- 后端 `requiresBeneficiary`（L627–632）含 grant/authorization/refund/waiver/debit/deduction/`.money`/`ar_ap_adjustment`；**不含** `contract_violation`。`forward` L790–792 仅当 `requiresBeneficiary(actionType)` 且无受益人才拒。
- HTML PGM-029：`不得转发给该单据的申请人本人，也不得转发给该单据的直接受益人——如该笔退款的收款人、临时权限或高危授权的被授权对象本人`。约束是「不得转给受益人」，不是「无受益人禁止转交」。无受益人时该禁止不适用。
- 分类：**细化**过度，把「禁止转给受益人」做成「所有单必须先有受益人」。
- 工程缺陷：**是**。添加：**否**（未引入新实体，只是比 HTML/后端更严）。
- 最小处置：收窄到与 `requiresBeneficiary` 同类动作；合同违规等允许转交，仍拦转给已填的受益人/申请人。

### GR-05 驳回不预检策略附件，免单等会先确认再被后端 400

- 文件：tsx L318–329 `rejectReview` 只 `validateDecisionActor()`；L322–327 仍发送 `evidenceAttachmentIds: [evidenceAttachment.trim()]`（空串也会进数组）。`validateApprove` 的附件闸不用于驳回。
- 后端 `requireDecisionEvidence` 对 approved/rejected 同样执行；`bill_waiver` 要 `evidence_attachment`（L57）。空串 `textPresent` 为假。
- HTML BEQR-063 免单须审批；PGM-029 驳回是合法节点动作。GEN-002/008/009。
- 正常路径：待审免单 → 填意见 → 确认「主管驳回退回起草人」→ 后端「该策略要求审批附带证据附件」。
- 分类：**细化**。工程缺陷：**是**。添加：**否**。
- 最小处置：驳回与放行共用后端同一套证据预检；空附件送 `[]` 不送 `[""]`。

### GR-06 评估退款/免单不预检直接受益人

- 文件：tsx L193–221 `evaluateViolation`：只拦说明空、终审人=申请人、终审人=受益人字符串相等。不要求 `beneficiaryId`。
- 后端 evaluate L142–143 `requireBeneficiaryIfNeeded`；缺则 `BENEFICIARY_REQUIRED`「退款、免单、授权与资金事项必须登记直接受益人」。
- 下拉含免单/退款/调账（tsx L417）。正常点「评估违规动作」不填受益人 → 请求发出 → 400。
- GEN-002/008；BEQR-063 免单。PGM-029 受益人例子正是退款收款人。
- 分类：**细化**。工程缺陷：**是**。添加：**否**。
- 最小处置：评估预检与 `requiresBeneficiary` 对齐（本页动作为 `bill_waiver`/`refund`/`ar_ap_adjustment` 时必填受益人）。

### GR-07 本模块测试未覆盖放行主路径，且把错误客户端闸/假全中文钉成绿

- 文件：`SupervisorReviewPage.test.tsx` 全文；chain.test 两例只覆盖 `final_approver_id` 与链链接。
- GEN-003 原文：`测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支；测试通过本身不替代行为证据。`
- 实跑 21+2 页测全绿，但：
  - 无 `decision: "approved"` 成功断言；
  - 无放行「不能复核自己发起的单 / 直接受益人不能复核」；
  - L254「动作类型选项与服务端策略一致」选项是硬编码下拉，并不从 `POLICIES` 生成（夹具还有 `billing_discount`，下拉本来就没有）；
  - L294「不含英文枚举」不失败于 `contract`/`bill_waiver`/`one_level_up`；
  - L242 把 GR-02 的错误闸当正确。
- 分类：**技术性**（测试夹具，无业务发明）+ 工程缺陷。
- 工程缺陷：**是**。添加：**否**。无业务发明理由：断言与夹具不引入新枚举/流程，只是无效或钉死错误行为。
- 最小处置：补能失败的放行成功/自审/真实目录中文断言；删掉或改写与后端相反的「对不上策略不能放行」例。

### GR-08 转交「账号」按登录名采集，后端按主体 id 等值匹配

- 文件：tsx L45 `forwardTarget: "转交给（账号）"`；L243–250 仅当 `target === initiatedBy` **或**（当前用户即申请人 **且** `sameAccount(target, user)`）才拦申请人。第三人填申请人 **username** 时两条件都不成立。
- 后端 forward L798–801：`target.equals(pending.initiatedBy()) || target.equals(s.beneficiaryId())`，无登录名解析；`forwarded_to` 原样入库。approve L315：`reviewerId.equals(node.forwardedTo())`，`reviewerId = currentActor().id()`。
- 本页测试用户 `id: "u1", username: "admin"`（test.tsx L58）证明账号≠id。L163–169 只覆盖「申请人用登录名转交给自己」（当前用户即申请人）。
- HTML PGM-029：`审批人可将该单据的审批转发给指定人选……被转发人同时获得审批权限`；`不得转发给该单据的申请人本人，也不得转发给该单据的直接受益人`。
- 正常路径：审批人在「账号」框填申请人或同事的登录名 → 前端放行（非自己）→ 后端不视为申请人、转交「成功」并中文提示 → 被转发人用自己的 id 登录无法命中 `forwardedTo`，不能接审。
- 分类：**细化**（账号字段如何对应主体）。工程缺陷：**是**。添加：**否**。
- 最小处置：与后端同一主体键（只收 id，或前后端都做登录名解析）；第三人转给申请人对登录名与 id 都拦。本席不发明新解析服务——能删前端「账号」暗示、改成与后端字段同名的主体编号即可。

## 7. 观察（极端/非正常路径，不入 finding）

- `nextKey` 用 `Date.now()`：busyAction + HighRiskConfirm 挡住连点；同毫秒双提交属极端并发，ledger 备注级，不修。
- 待审行按钮文案「转交」同时 `setLatestReviewId` 才能放行/驳回：易混淆，但是否「第一次正常就会撞」不够硬，观察。
- KPI 小字「默认拦截」vs「默认阻断」：皆中文，不单列。
- `finalApprover === beneficiary` 只做字符串相等、不用 `sameAccount`：申请人自任终审已被 `sameAccount(user)` 拦住；跨主体登录名需后端链生成闸，观察。
- 前端不传项目/商户：隔离在后端 `listPending`/`requireVisibleReview`，与 PGM-041「不提供关闭开关」一致。
- 计划 660 vs 现文 865：现文含未进 origin/main 的两文件增量；不改计划。
- 税费三审/平行线：本页评估下拉无 `finance.tax_rule.adjust`，不在本页发起，不记本页漏功能（无新需求）。

## 8. ponytail-review（可删，服务冲突/细化）

- `SupervisorReviewPage.tsx:L286-292`：`actionType ===` 客户端策略闸 — **delete**（GR-02），改走后端已有证据规则。
- `SupervisorReviewPage.tsx:L235-238`：无受益人一律禁转交 — **delete/shrink**（GR-04）到资金类。
- test.tsx L242–251 钉死错误闸 — **delete** 该例或改口径（GR-07）。
- 硬编码下拉与策略表双份动作列表 — **shrink** 为同一中文映射（GR-01/02），不新造动作。

`net: 以删除客户端多余闸为主，不扩功能。`

## 9. 四分类汇总 / 添加项

| GR | 分类 | 工程缺陷 | 添加 | 修复队列（B2 候选） |
|---|---|---|---|---|
| GR-01 | 冲突 | 是 | 否 | 是（删除英文展示） |
| GR-02 | 细化 | 是 | 否 | 是 |
| GR-03 | 冲突 | 是 | 否 | 是 |
| GR-04 | 细化 | 是 | 否 | 是 |
| GR-05 | 细化 | 是 | 否 | 是 |
| GR-06 | 细化 | 是 | 否 | 是 |
| GR-07 | 技术性 | 是 | 否 | 是 |
| GR-08 | 细化 | 是 | 否 | 是 |

**添加项：0。** B1 不发 2.2 问。无锁外新判据。冲突按 V0.17 已有修法（删英文展示、收窄客户端闸、改 makeResult 已有文案），不提新闸/新角色/新产物。

## 10. 结论

**B1 本席：FAIL**

不是有条件通过。整模块三文件已读、106 项已核、三线已跑、哈希已独算。存在可复现正常路径工程缺陷 GR-01…GR-08。现有 33 个前端测全绿不能翻盘。

下一步（给 Codex/B2，本席不修）：归并本 raw 与 self raw，分配正式 ID；冲突/有缺陷细化/技术性进修复队列。
## M043 B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **28539** 字节原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；Grok FAIL 具否决权）。步骤：M043 本次旧PR遗漏 B2 整批候选复核。不是 B1 重审、不是 B3 修复、不是模块 PASS、不是 B4/B5。
- 正本：`/home/james/complex-management` main。本席 `rev-parse HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `27ad0e5fc25eb02e33cfe4743fb50072c2488593`。未 commit/push/merge/deploy、未改数据库、未改主 index。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（SHA `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）；runbook V0.02（SHA `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`）；criteria-lock 版本行 V0.14；modules.md V0.04 M043 FILES 3。
- 锁实算（本席本步 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`，**正文叙述、非独占行**）：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 文件内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。四值全等。本席不写 ledger。
- ledger 只读背景：M043 `IN_PROGRESS`，B3 累计 0、本次 0。
- 本席 B2：**PASS**
- 添加项：0。questions：0。无 HTML 修改申请。无新判据。无超三文件（测试增量归对应 ID，仍在本模块两测试文件内）。
- 无 MCP、无子代理、无第三席、不问操作员。除本指定路径外未写仓库内任何文件。未改 findings/ledger/方案/锁/业务 HTML/代码/测试。
- **本票不表示代码已修、不表示模块 PASS、不授权 B3。** 合取须另有席甲本批 B2 PASS。不把本票写成「台账已按本席意见改过」。

此票只判断 `docs/audit-2026-09/findings.md` **V3.59** 顶部「M043 本次旧PR遗漏 B2候选（未定稿、未修复）」相对本次两份完整 **B1 V0.01** 的全批差集、§2.1 四分类、工程缺陷、修复队列与最小修法。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 / 范围 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M043-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 本次 B1；全文即本次完整报告 | 28539 | `092aa8bbce46a860e9a67fb65aceb6e0e75a92a70cf6564051354b8c25a90863` |
| `docs/audit-2026-09/modules/M043-b1-self-raw.md` **V0.01 前缀**（最后独占锁行起至 B1「逐项覆盖」表结束，不含其后任何 B2 段） | V0.01 B1（CX-01..04） | 25224 | `b95d62bd3df8c161ef7728affe715ade8affee5940cf3c41f1515292e3e01c04` |
| `docs/audit-2026-09/findings.md` 顶部 M043 候选节（至下一 `## 当前M009` 前） | V3.59 未定稿 8 行 | 4075 | `4805adb3bd44cf734052a4d79ad3e41af36bfb507e5b18700a2ccba2cfbb1177` |

B1 finding 集合**只取两份 B1 正文**。席甲 self-raw 若在 V0.01 之后另有 B2 追加段，本席不以其为输入、不沿用其票。输入以操作员指定的两份 V0.01 完整 B1 为准。

三文件当前 SHA（本席重算，**未改**）：

| 路径 | 行 | SHA256 |
|---|---:|---|
| `apps/web/src/admin/pages/SupervisorReviewPage.tsx` | 467 | `11c7e32395ecaeea2d4141ed85753580a35ce8d59e6e73a60ce29ed795488b81` |
| `apps/web/src/admin/pages/SupervisorReviewPage.test.tsx` | 323 | `2144efd4d5e9614039d2c633455d82a746bf5f68cfdcc8348e12d54ac198f1ec` |
| `apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx` | 75 | `9640cc82d5d14fbff14c340d07da1cfc28999eb04697e0d7fb845cec9840565d` |

`git ls-files -z -- <3 paths> | xargs -0 cat | wc -l` = **865**。映射 106 未增。只读依赖（不纳入本模块修复面）：`SupervisorReviewService.java` `requiresBeneficiary` / `requireDecisionEvidence` / `findPolicyByCode` / 中间节点 `return pending`；`ApprovalChainPage.tsx` L72–81 `makeResult`；`apps/web/src/admin/i18n/statusLabels.ts` 仅 re-export `customer/i18n/statusLabels`；`formatBusinessStatus` L255–259 `STATUS_LABELS[code] ?? code`。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| receiving-code-review | 先核源/HTML/命令再接受或反驳候选；禁止表演性同意本席 B1 或席甲分类 |
| grilling | 只借追问结构自查：8 项是否漏并、004/008 是否替根、002 未匹配是否仍硬拒、007 是否添加 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| ponytail-review | 只评最小删除/收窄，不落地 |
| writing-for-agents | 本 raw 结构；未改 AGENTS.md |
| requesting-code-review / tdd / ponytail | 本体要派子代理或落地修复 → **未用** |
| Arc audit / grilling 派子代理 | 与双席冲突 → **未用** |
| batch-grill-me | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集

漏并：无。误合：无。把观察升独立缺陷：无。把本席观察静默丢弃：无（§7 七条仍观察，不入 001–008）。被改写原意：无（004/008 相对本席 B1「可复制后端谓词 / 可做登录名解析」的**额外修法**按操作员关键最小范围收窄，**根缺陷仍在**，见各条）。无新正式号。添加 0。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M043-001 | GR-01（CX 无对等项，属本席多出、必须纳入） | 冲突/是/是 | **PASS（冲突/是/是）** |
| M043-002 | GR-02 + CX-03 + CX-04 | 细化/是/是 | **PASS（细化/是/是）** |
| M043-003 | GR-03 + CX-02 | 冲突/是/是（CX-02 原写细化） | **PASS（改判冲突/是/是；CX 原票完整保留在 B1）** |
| M043-004 | GR-04 + CX-01 | 细化/是/是 | **PASS（细化/是/是；最小修法取删前端一概必填，不复制 `requiresBeneficiary`）** |
| M043-005 | GR-05（CX 无） | 细化/是/是 | **PASS（细化/是/是）** |
| M043-006 | GR-06（CX 无） | 细化/是/是 | **PASS（细化/是/是）** |
| M043-007 | GR-07；关联 CX-01..04 的测试钉死/缺口 | 技术性/是/是 | **PASS（技术性/是/是；无业务发明）** |
| M043-008 | GR-08（CX 无） | 细化/是/是 | **PASS（细化/是/是；只改标签/提示，不实现 username 解析）** |

映射核对：GR-01→001，GR-02→002，GR-03→003，GR-04→004，GR-05→005，GR-06→006，GR-07→007，GR-08→008；CX-01→004，CX-02→003，CX-03/CX-04→002。CX 四条、GR 八条全部入表。冲突入队 2（001、003）。细化入队 5（002、004、005、006、008）。技术性入队 1（007）。修复队列「是」8。

### 001 独立裁定（冲突／是／是）

核源：

- 页 `SupervisorReviewPage.tsx` L355/358/359 策略表、L337–341 抽屉、L397–401 待审：`formatBusinessStatus(actionType|requiredEvidenceTypes|status|objectType|approvalLevel)`。
- `apps/web/src/admin/i18n/statusLabels.ts` 只 re-export 客人表；`customer/i18n/statusLabels.ts` L255–259 `STATUS_LABELS[code] ?? code`。本席核表内有 `contract_violation`/`base_definition_violation`/`manual_release`/`pending_review`/`active`，**无** `bill_waiver`/`refund`/`ar_ap_adjustment`/`one_level_up`/`two_levels_up`/`specific_role`/`top_authority`/`bar.bill_waiver`/`reason`/`evidence_attachment`/`contract_snapshot`/`contract`。
- 后端 `listPolicies()` L51–67 固定含 `bill_waiver`/`refund`/`ar_ap_adjustment`/`one_level_up` 等；本页加载即渲染该目录。待审夹具 `review_p1.action_type=bar.bill_waiver`、`approval_level=one_level_up`；策略夹具 `required_evidence_types:["contract"]`。
- 现测 L294–301 / L311–318 只禁 `contract_violation` 原文，**不对** `contract`/`one_level_up`/`bill_waiver` 失败。

HTML：治理地图 L287 / L965 PGM-066「枚举取值……必须全部为中文，不得出现任何英文」「不得以“这是标识”为由保留英文」。动作类型/证据类型/审批层级是给人看的枚举，不是四样例外。

四分类：**冲突**（明文全中文 vs 英文原码上屏）。缺陷 **是**。队列 **是**。不是添加新动作。

最小边界与候选一致，且收窄本席 B1「或补本页用到的码表」中易被读成改全局字典的那半句：

- **只**改本页既有枚举字段展示（策略表/待审/抽屉同一局部映射）。
- 复用本页下拉已有中文词（合同违规/基础定义违规/免单/退款/调账/人工放行）及本页已有标签（违规说明、证据附件编号）。
- 未知码 **中文占位**（如未识别），不得 `?? code` 英文回退。
- 单据号、对象编号、发起人/受益人/转交目标等内部标识符原值保留。
- **不**改 `customer/i18n/statusLabels.ts` 或 admin re-export，**不**新增下拉动作/业务码。

票：**PASS（是）**。

### 002 独立裁定（细化／是／是）——目录闸 + 证据类型，CX-03/04 同根

核源：

- `validateApprove` L290–295：`rows.find(row => row.actionType === (pending?.decision.actionType ?? actionType))`，未命中即「找不到对应复核策略，不能放行」。
- L296–299：`requiredEvidenceTypes.length > 0` 即要附件。
- L311 / L325：`evidenceAttachmentIds: [evidenceAttachment.trim()]`，空串变成 `[""]`。
- 测试 L242–251 把 `bar.bill_waiver` 对不上目录钉成正确。
- 后端 `approve` 用 `findPolicyByCode(pending.policyCode())`（L226）；未知策略返回 `null`（L640–648），`requireDecisionEvidence` 对 `policy==null` 直接 return（L983–985）；只当 `required.contains("evidence_attachment")` 才要附件（L989–990）。合同策略证据是 `violation_summary`/`contract_snapshot`（L61），**不要**附件。`refund` 只要 `reason`（L58）。准入动作名 `bar.bill_waiver` 对不上目录动作名 `bill_waiver`。夹具 `policy_code: "bar_bill_waiver_block"` **不在** `listPolicies()` 闭集。
- 页头 L16 / L37 声称「所有放行动作必须带……证据附件」。

HTML：PGM-014「重要动作可追溯」；L529 本条适用于退款、免单、调账及其他一切业务审批（引用原文、不扩锁）。GEN-002/008 入口与后端口径须一致。

四分类：有上位审批/证据规则的下位实现过严 → **细化**。缺陷 **是**。不是新策略目录。CX-03 与 CX-04 是同一 `validateApprove` 的两瓣，并一条正确，不另开号。

最小边界（确认候选，**拒绝**本席 B1「或与后端同一动作别名」那条扩法）：

- **删除**「目录 actionType 找不到就不发」前端闸。
- 已加载策略按待审 **policyCode** 匹配（API 已有 `policy_code`；本页 `PendingRow` 类型未声明该字段，B3 只补读已有信封字段，不新接口）。
- **未匹配不硬拒**，把节点/证据交给已发布服务端；前端不是安全边界。
- 证据仅明确 `evidence_attachment` 才要附件编号；`reason`/`operator_note`/`violation_summary` 走已有审批意见/违规说明。
- 放行/驳回空附件发 `[]` 不发 `[""]`；说明/边界文案与现规则同步，删除「一律附件」过声称。
- 不自行新增 `bar.bill_waiver→bill_waiver` 等客户端动作别名表，不补后端策略。

票：**PASS（是）**。

### 003 独立裁定（冲突／是／是）——推翻 CX-02「细化」分类，不改事实

核源：

- 本页 `makeResult` L71–74：非 forward/evaluate/rejected 一律 `` `复核单 ${id} 已审批通过` ``，不读 `reviewStatus`。
- HighRiskConfirm 影响 L437：「记录审批通过结论」。
- 后端 L247–248：链未 `allApproved` 时 `return pending`，单据保持 `pending_review`。默认动作 `contract_violation` 策略 `two_levels_up`（L61）。第一节点通过是正常路径。
- 已有 `ApprovalChainPage.tsx` L72–76：`reviewStatus === "approved"` →「全部节点已通过，单据生效」，否则「当前节点已通过，等待后续节点审批」。

HTML：L528「全部直接上级节点，逐级审批」；L536「主线与各条平行线全部通过合同才生效」；PGM-066 禁止把未完成显示成完成。把仍待审写成「已审批通过」与明文完成条件**直接矛盾**，不是单纯文案口味。

§2.1 冲突优先。CX-02 写细化，本席 B1 写冲突；候选采冲突。CX 原文事实保留，只改分类。

最小：改本页已有 `makeResult` 与确认影响，**复用** ApprovalChainPage 已有两分支语义（当前节点通过待后续 vs 整单通过）。不改后端、不把链页拷进本页。测试两个返回分支。驳回句可保留。

票：**PASS（是）**。

### 004 独立裁定（细化／是／是）——根=删除一概必填；不同意复制后端谓词

核源：

- L235–238：`if (!pending?.schedule?.beneficiaryId) { setFormError("直接受益人未登记，不能转交"); return; }` 无动作条件。
- 测试 L141–150 把合同违规 `beneficiary_id: null` 转交钉绿。
- 后端 `requiresBeneficiary` L627–631：grant/authorization/refund/waiver/debit/deduction/`.money`/`ar_ap_adjustment`；**不含** `contract_violation`。`forward` L790–792 仅当该谓词为真且无受益人才拒。
- 利益冲突：L243–250 已拦申请人/已填受益人（含当前用户登录名 `sameAccount`）。

HTML L529：约束是「不得转发给申请人本人，也不得转发给该单据的直接受益人」，例是退款收款人/被授权对象。**不是**「无受益人禁止转交」。无受益人时该禁止不适用。

四分类：把禁止对象做成全单必有字段 → **细化**过度。缺陷 **是**。CX-01 与 GR-04 同根，并一条。

本席 B1 曾写「收窄到与 `requiresBeneficiary` 同类动作」。操作员关键最小范围与 CX-01、候选均为：**删除无受益人一律拒绝的前端分支**，保留利益冲突与可空读取；资金/授权缺受益人**交服务端** `requiresBeneficiary` 拒绝，前端如实显示错误；**不在前端复制全部后端谓词**。

本席同意该收窄，**不是替根**：

- 根是合同违规等无受益人被前端死拦（默认动作、测试已钉死）。删该分支即根修。
- 把 `contains("waiver")` 等宽谓词抄到前端 = 第二份业务规则，会与后端漂移，且把授权/`.money` 等本页下拉没有的动作塞进客户端。铁律 6 收窄已有点，不新造前端业务谓词。
- 资金类转交缺受益人走服务端 400，由 `useAdminAction` 已有错误路径展示；004 测试「服务端拒绝不显示成功」覆盖此残留，不把预检谓词搬回来。
- 006 只在**评估表单**对已有三动作做必填提示，与 004 不矛盾：004 处理已存在的待审转交，006 处理本页正在填写的评估。

最小：删 L235–238；保留 L239–250；后续 `beneficiaryId` 访问改可空。改原错误测试为合同无受益人仍提交转交。不改 Java。

票：**PASS（是）**。

### 005 独立裁定（细化／是／是）

核源：`rejectReview` L318–329 只 `validateDecisionActor()`；HighRiskConfirm 驳回 `precheck={validateDecisionActor}`（L449）。后端 `requireDecisionEvidence` 对 approved/rejected 同样执行；`bill_waiver` 要 `evidence_attachment`（L57）。空串 `textPresent` 为假 → 先弹出「主管驳回退回起草人」再 400。

正常路径：待审免单、填意见、不填附件、确认驳回。GEN-002/008/009。

分类 **细化**。缺陷 **是**。

最小：驳回复用**收窄后的**审批预检（身份/意见 + 已知 policyCode 且明确 `evidence_attachment` 才要附件）；两动作空附件 `[]`。不新设后端条件。补免单驳回缺附件/带附件。必须与 002 同一 B3 整批，禁止在未删目录闸前把现行 `validateApprove` 原样套到驳回（否则 `bar.bill_waiver` 驳回也会被错误目录闸拦住）。

票：**PASS（是）**。

### 006 独立裁定（细化／是／是）

核源：`evaluateViolation` L193–221 只拦说明空、终审人=申请人、终审人=受益人字符串相等，**不**要求 `beneficiaryId`。后端 evaluate `requireBeneficiaryIfNeeded`；缺则 `BENEFICIARY_REQUIRED`。下拉已有免单/退款/调账（L417）。正常点「评估违规动作」不填受益人 → 发出 → 400。

分类 **细化**（已有表单校验收窄到后端已要求的三动作）。缺陷 **是**。不是新受益人制度。

最小：仅本页既有 `bill_waiver`/`refund`/`ar_ap_adjustment` 在已有 `evaluateViolation` 里必填直接受益人，用已有错误区。合同违规/基础定义违规/人工放行仍不强求。不改变后端、不把 `requiresBeneficiary` 全谓词（grant/authorization/debit/`.money` 等）抄进前端、不新增角色/事项。测试这三动作与合同例外。

票：**PASS（是）**。

### 007 独立裁定（技术性／是／是）——无业务发明

核源：页测 21 + chain 2，本席 B1 实跑另含 statusLabels 10 共 33 绿（statusLabels **不是**本模块文件）。现测无 `decision:"approved"` 成功、无放行返回 `pending_review`、无放行自审/受益人自审、中文断言不失败于真实目录码；L242 钉死 002 的错误闸。

GEN-003：测试须执行目标行为且能失败。分类 **技术性**（测试装置；能指的 HTML 上位已由 001–006/008 承担产品缺陷）。无业务发明理由：不引入新枚举/流程/默认值，只让公共 UI + fetch 真实信封能失败于已有行为。若改判添加则以本席为准走 2.2——本席**不**改判添加。

最小：在**原测试文件**用页面公共交互（点选待审、填意见/附件、确认）与 fetch 信封检验：批准成功、两终态（整单通过 vs 仍 `pending_review`）、自审/受益人拒绝、真实目录中文。不测私有函数、不改测试框架。002/004 钉死错误闸的例子归那些 ID 改写，007 不删除原权限/驳回/金额/链链接断言。B3 可执行行为变更按提示词先红后绿。

票：**PASS（是）**。

### 008 独立裁定（细化／是／是）——根=标签误导；不同意做 username 解析服务

核源：

- L45 `forwardTarget: "转交给（账号）"`；L230「填写转交账号」；L412 `aria-label` 用该标签。
- 前端拦申请人：`target === initiatedBy` **或**（当前用户即申请人 **且** `sameAccount(target, user)`）。第三人填申请人 **username** 两条件都不成立。
- 后端 L798–801：`target.equals(initiatedBy) || target.equals(beneficiaryId)`，无登录名解析；approve 用 `currentActor().id()` 对 `forwardedTo`。
- 测试用户 `id:"u1", username:"admin"` 证明账号≠id。L163–169 只覆盖「申请人用登录名转交给自己」。

正常路径：审批人按「账号」填同事登录名 → 前端放行 → 后端当主体 ID 存入 → 中文「已转交」→ 同事用自己的 id 登录命中不了 `forwardedTo`，不能接审。

分类 **细化**（字段如何对应主体）。缺陷 **是**。

本席 B1 曾并列「只收 id，或前后端都做登录名解析」并写「第三人转给申请人对登录名与 id 都拦」。操作员关键最小范围与候选、以及本席 B1 已写的「不发明新解析服务——能改成主体编号即可」：

- **只**改现有标签及提示为「用户编号（不是登录名）」/「填写用户编号不填登录名」。
- 终审人与受益人保持同一 ID 语义，不给那些框加 username 解析。
- 保留已有当前用户登录名利益冲突拒绝（`sameAccount`）。
- 第三人申请人/受益人的 **ID** 仍拒绝。
- **不**引入解析服务、账号搜索、新接口；**不**宣称支持任意 username。

同意该收窄，**不是替根**：根是 UI 采集键与后端匹配键不一致，由「账号」一词把人导向登录名。改标签/提示是本席已承认的最小删除暗示。第三人 username 命中申请人若无解析服务则无法在前端诚实拦住，把「任意 username 都拦」做成客户端猜测会误伤合法用户编号；后端仍按 ID 等值拦申请人/受益人。残留是操作员明确不修的解析能力，不是把缺陷改写成「已支持登录名」。

票：**PASS（是）**。

### 观察（完整保留，不入队、不另开号）

本席 B1 §7 全部仍为观察：

1. `nextKey`=`Date.now()` 同毫秒双提交：busyAction + 确认挡住连点；极端并发，不修。
2. 待审按钮文案「转交」同时 `setLatestReviewId`：易混淆，未达「第一次正常必撞」硬门槛，不另立功能、不改文案授权。
3. KPI 小字「默认拦截」vs「默认阻断」：皆中文，不单列。
4. `finalApprover === beneficiary` 字符串相等、不用 `sameAccount`：申请人自任终审已被 `sameAccount(user)` 拦住；跨主体登录名属后端链生成，观察。
5. 前端不传项目/商户：隔离在后端 `listPending`，与 PGM-041 一致。
6. 计划 660 vs 现文 865：含未进 origin/main 的两文件增量；不改计划。
7. 税费三审/平行线：本页下拉无 `finance.tax_rule.adjust`，不在本页发起，不记本页漏功能。

CX「边界与不扩需求」六条（不删 manual_release、不猜 objectType 必为英文 contract、不新造实体选择器、不在本页重建平行线/工资税务、前端非安全边界、证据采集/范围授权不在本三文件）与上列观察一致，**不升 finding**。

### 八项归因与三文件边界

候选执行面只 M043 三文件及自身版本递增。本席逐项核：

| ID | 是否够三文件 | 是否新业务/超界 |
|---|---|---|
| 001 | 是（页内局部映射+三处展示+测试夹具改真实目录码） | 否。不改全局 statusLabels |
| 002 | 是（删闸、改匹配键、证据谓词、空数组、说明；测试） | 否。不造动作别名、不改 Java |
| 003 | 是（makeResult+确认影响+两分支测试） | 否。只读复用链页句式，不改 ApprovalChainPage 源 |
| 004 | 是（删 L235–238+测试） | 否。不复制 requiresBeneficiary |
| 005 | 是（驳回 precheck 共用收窄后的审批预检+`[]`+测试） | 否。与 002 同批 |
| 006 | 是（evaluate 三动作必填+合同例外测试） | 否。不新增受益人制度 |
| 007 | 是（原测试文件公共交互） | 否。不测私有函数 |
| 008 | 是（TEXT.forwardTarget/hint/error） | 否。不改链页同文案、不写解析器 |

无需第四生产文件。无需 2.2。铁律 6：删除过严闸/过声称、收窄已有校验、改已有展示键/文案。不是必须新增。B3 若改 Java、全局字典、ApprovalChainPage、HTML、锁，属越界，由 B4 闸 3 整批回滚；本票不授权那些路径。

八项均工程缺陷是、队列是，须 **整批** B3；Grok 本批 B2 与席甲本批 B2 双 PASS 后才能修。本席不修。

### 真实执行与限制

| 项 | 结果 |
|---|---|
| 锁哈希独算 | 与 sidecar/ledger/B1 独占行同值（正文叙述，非独占行） |
| 双方 B1 V0.01 | 全文从最后独占锁行读完；self 只采 25224 字节前缀 |
| 3 文件 | 重读关键行；SHA/865 与 B1 清单一致；未改 |
| 后端谓词/链页 makeResult/全局码表 | 只读追流，不扩模块 |
| 浏览器 / 真实 HTTP / 后台测试 | **本步未新跑**；采信两份 B1 已记录的 `test:unit`/`tsc` exit0，并用源码控制流复核缺陷仍在（代码 SHA 未变） |
| 写入 | 仅追加本文件 |
| 子代理 / MCP | 未使用 |
| 席甲本次 B2 正文 | 不作为本票依据 |

未验证前提（不阻本票）：真实浏览器带会话点放行；生产库待审是否出现 `bar.bill_waiver`。源码级目录闸 + 夹具 `review_p1` + `listPolicies` 动作名不一致，足够定 002。

### 整批结论

**PASS**

8 行全部给票，无待补、无条件通过、无添加项、无 HTML 申请、无新判据。本票不授权 B3、不修改台账、**不表示代码已修、不表示模块 PASS**。合取须席甲本批 B2 亦 PASS。不覆盖 V0.01 B1 FAIL。

## M043 B5-r1 回退 B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 与 V0.02 首次 B2 前缀 **52776** 字节原样保留，不改写 B1 锁段起点，**本段不写独占锁哈希字段行**）
- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；Grok FAIL 具否决权）。步骤：M043 B5-r1 合取 FAIL 回退的 B2 整批候选复核。不是 B1 重审、不是 B3 修复、不是模块 PASS、不是 B4/B5。
- 提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` **V0.17**；runbook V0.02；锁 `criteria-lock.md` V0.14
- 模块生命周期（只读）：`IN_PROGRESS`；B3 累计 **1**
- 本席 B2：**PASS**
- 输入 raw（触发回退的那一对本次完整报告，**不是**旧 B1 票）：
  - `docs/audit-2026-09/modules/M043-b5-r1-self-raw.md` **V0.01** 完整 PASS（24075 字节，SHA256 `784e323010f88f47413690cdef2fdcc8a50f7aec1080b6c44dcda391ab0301bd`）
  - `docs/audit-2026-09/modules/M043-b5-r1-grok-raw.md` **V0.01** 完整 FAIL（25399 字节，SHA256 `3dfea4e82cd8100774637d2de5f221f53b19dc1f573205f47db6cc253825d34d`）
- 候选台账：`docs/audit-2026-09/findings.md` **V3.62** 顶部「M043 B5-r1回退 B2候选（009—011未定稿）」三行
- **本票不表示代码已修、不表示模块 PASS、不授权 B3。** 合取须另有席甲本批 B2 PASS。不把本票写成「台账已按本席意见改过」。不覆盖上方 V0.01 B1 FAIL、不覆盖 V0.02 对 001—008 的首次 B2 PASS。
- 本席未读席甲同轮 B2 追加段（`M043-b1-self-raw.md` 任何 B2 段），不以对席同轮 B2 票作依据。

此票只判断 findings 顶部 009—011 相对本次两份完整 **B5-r1 V0.01** 的全批差集、§2.1 四分类、工程缺陷、修复队列与最小修法。调度给出的 009=GR01 / 010=GR02 / 011=GR03 拟方案只作待核文本，本席按现文与锁独立裁定，不沿用调度结论。

本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` = `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，与 sidecar 全文及 ledger 顶部基准同行。本段按 B2 特别规则把该值写在叙述里，不另起独占 `CRITERIA-LOCK-SHA256:` 行。

### 技能（V0.17 §6 收窄，本节点实读）

已读并按收窄口径使用：`using-superpowers`、`grilling`（只借追问结构，事实自读，不派子代理、不等操作员）、`receiving-code-review`（先核源再接受或反驳，本步禁止修复）、`ponytail` ultra / `ponytail-review`、`diagnosing-bugs`（现有 vitest+源码作红环，不改产品文件）、`codebase-design`、`verification-before-completion`、`writing-for-agents`。未核到独立可执行的 `grill-me`/`batch-grill-me` CLI 与 `i-have-adhd` 技能文件，**不冒充已调用**。未用 MCP、子代理、第三席、worktree、research。

### 差集（穷尽）

| 来源 | 本批 findings | 候选归并 |
|---|---|---|
| Grok B5-r1 GR-01 | 策略表 `key`/`testid` 用 `row.id`，后端记录无 id | **M043-009** |
| Grok B5-r1 GR-02 | 审批链 `<a href="/admin/...">` 不走 `withBase` | **M043-010** |
| Grok B5-r1 GR-03 | 测试不能红 GR-01/02，链测试过声称空最终审批人 | **M043-011** |
| Grok B5-r1 §8 观察 7 条 | 评估后按钮仍 enabled、KPI 聚焦、预填对象、`active`→「有效」、评估动作是目录子集、`empty={rows.length===0}`、计划 660 vs 现文 1018 | **不入队**（Grok B5 已写不升票；首次正常路径不单独成立或 HTML 未要求） |
| 席甲 B5-r1 | 结论 PASS，无新正式 ID；把无 id 的 React key 警告与链测试空最终人写成观察 | **无条可并**。Grok 否决：自审 PASS 不得覆盖 B5 FAIL。本席核源后确认 GR-01/02/03 成立，不把席甲观察当结案 |
| 001—008 | B4-r1 双 PASS 后队列否 | **不重开**。B5 两席均未给出这八条未修好的新证据 |

漏并：无。Grok B5 三条工程缺陷全部进入 009—011，无一遗漏、无一与观察对调。
误合：无。一条 GR 对一条正式 ID；009/010 的测试句与 011 是双归因（产品 ID 带选择器同步，GEN-003 总覆盖另立，同 001—008 与 007 的已接受口径），不是把三个产品根塞进一个 ID。
旧 B1 八条：不并入本批、不改分类、不改队列否。

### 核源（本席本步实跑，类型 2/4）

三文件现文 SHA 与 B5 两席一致：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `SupervisorReviewPage.tsx` | 485 | `c7a08798740eb56aafe5d04b32d12dda6f26705900ec3ac4b189deca55ac4533` |
| `SupervisorReviewPage.test.tsx` | 458 | `88169e9fbe829ee0f2665ae43224571c953c6916f6299b75fd96f88eca984f11` |
| `SupervisorReviewPage.chain.test.tsx` | 75 | `9640cc82d5d14fbff14c340d07da1cfc28999eb04697e0d7fb845cec9840565d` |

`git ls-files -z -- <3 paths> | xargs -0 cat | wc -l` = **1018**。HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`27ad0e5fc25eb02e33cfe4743fb50072c2488593`。本席未改源、未改 index。

```
cd apps/web && npx vitest run src/admin/pages/SupervisorReviewPage.test.tsx src/admin/pages/SupervisorReviewPage.chain.test.tsx
```

进程 exit 0；Test Files 2 passed；Tests **38 passed / 38**。同一次 stderr：

```
stderr | ... > approve 按 policyCode 而非动作名要求免单附件
Each child in a list should have a unique "key" prop.
Check the render method of `SupervisorReviewPage`.
```

测试绿不能否决 GR-01。未跑浏览器、未跑 Maven/全后端。

对照（只读，不扩模块）：

- `SupervisorReviewPolicyRecord.java` 首字段是 `policyCode`，构造无 `id`。`listPolicies()` 内存 12 条同样无 id。权威键是 `policyCode`，12 码互异。
- 本页 L368–369：`key={row.id}`、`data-testid={supervisor-review-row-${row.id}}`。展示与抽屉已写 `row.policyCode ?? row.id`（L352–354、L372），证明作者知道业务键是 policyCode，却把身份键钉在不存在的 `id` 上。
- `apps/web/src/admin/types.ts:426-428` `id: string` 必填——**该文件不在 M043 三文件内**，本批禁止改它。
- 主测 `POLICIES` 带假 `id: "sp-1"`；抽屉例用 `supervisor-review-row-sp-1`。`REAL_POLICIES` 无 id，中文例用 `getByText(policy_code)` 而不断言唯一 testid，故 38 绿仍打出重复 key。
- 本页 L422：`<a href={`/admin/governance/approval-chain?review_id=${encodeURIComponent(...)}`}>`。`AdminApp` 的 `navigate`/`navigateTo` 才 `withBase`（L540、L562）；`navigateTo` 拼的是 `?focus=`，链页读的是 `review_id`（`ApprovalChainPage.tsx:67-69`）。全仓 `admin/pages` 裸 `/admin` href 仅此一行。
- `basePath.ts:25-26`：pathname 以 `/complex` 开头时 `withBase("/admin/...")` → `/complex/admin/...`；根路径原样。`basePath.test.ts` 已钉这两枝。门户 `rewrite_html` 只管构建期资源，管不到 React 运行时 href。
- 链测 L55 标题「不填则不带」，L65 只 `expect(body.final_approver_id).toBe("usr_gm")`。主测 `evaluateOnce`（L85–90）不读请求体。产品 L243 `finalApproverId.trim() || undefined` 加 `mapKeys` 丢 undefined，**产品空值路径本身成立**，过声称在测试。

### 009 独立裁定（细化／是／是）——根=身份键用了不存在的 id

四分类顺序：HTML 无「策略行必须用 id」明文，**不是冲突**。策略目录是 PGM-014「敏感操作进复核队列」与 BSM-028「卡住再按审批链放行」的下位表，**细化**。不是添加（不新实体、不新枚举）。React key 本身是实现细节，但能指到 HTML 上位则不进技术性。

工程缺陷：**是**。第一次打开本页即 `GET` 无 id 的 12 行；`key` 与 testid 全落到 `undefined`，testid 撞名，KPI 子集重挂载时调和不稳定。席甲「无局部编辑故只是警告」不成立：身份键错误在首次正常渲染就发生，GEN-002/008 不要求先证明点错行。假 id 夹具把缺陷藏进绿测。

拟方案核：仅本页 `key`/`testid` 改用已有 `policyCode`；不改 `types.ts`、不改后端、不引入 `id`、不造 `id||policyCode` 兼容层。

独立同意，**不是替根**：

- 根是用了信封里没有的字段。生产唯一稳定键已经是 `policyCode`，展示列已经在用它。
- 给后端加 id 或改共享 types = 越 M043 三文件、发明兼容字段，铁律 6 禁止。
- `key={row.policyCode}` 即可；12 条 policyCode 互异。不必保留 `?? row.id`（那会让假 id 夹具继续当身份键）。展示列已有的 `?? row.id` 不在本 ID 必改范围，B3 不得顺手扩到 types。
- 测试：现有 `supervisor-review-row-sp-1` 必须改到 `…-POL-1`（或该夹具的 policyCode）。真实目录唯一 testid 的能失败断言与 011 双归因，不另开 ID。

最小：本页策略 `<tr>` 的 `key` 与 `data-testid` 只用 `row.policyCode`；同步原选择器。不改 Java、不改 `types.ts`、不加路由。票：**PASS（是）**。

### 010 独立裁定（细化／是／是）——根=运行时 href 不补已有前缀；拒绝改成 SPA navigateTo

四分类：HTML 要求审批链可追溯（BEQR-166 合同审批链；BSM-115 子系统必须连在一起；PGM-014 谁审批谁复核）。链入口是该上位的下位实现，**细化**。`/complex` 是已有部署前缀，不是新业务路径，**不是添加**。不是冲突。

工程缺陷：**是**。生产 pathname 以 `/complex` 开头时，裸 `/admin/governance/approval-chain?...` 整页离开前缀。待复核每一行的「审批链」是正常动作，不是极端巧合。jsdom 默认非 `/complex`，链测绿。本席未跑浏览器，不声称看见 404；缺陷由 L422 与 `withBase` 契约直接推出。

拟方案核：现有 `<a href>` 复用 `../../basePath` 的 `withBase`；保留 `review_id` 的 `encodeURIComponent`、根路径与 `/complex` 两枝；不强制 `navigateTo`、不新增路由、不改 `ApprovalChainPage`。

独立同意，**不是替根**：

- 根是运行时锚点没走已有前缀函数。`withBase` 已存在、已有单测；从 `pages/` 到 `basePath.ts` 的相对路径就是 `../../basePath`。
- `withBase("/admin/governance/approval-chain?review_id=…")` 在 `/complex` 下变成 `/complex/admin/governance/approval-chain?review_id=…`，查询串保留。
- **禁止**改成 `navigateTo`：该函数拼 `?focus=`，链页读 `review_id`；本页当前也未接 `navigateTo`。改 SPA = 新导航契约 + 可能改链页，越界。
- **禁止**改 `ApprovalChainPage`（不在本模块三文件）。它已读 `review_id`。
- **禁止**新路由。`adminRoutes` 已有审批链页。
- 根路径：`withBase` 原样返回，现有 href 语义不变。

双部署 href 断言与 011 双归因。票：**PASS（是）**。

### 011 独立裁定（技术性／是／是）——无业务发明

四分类：测试装置。无新实体/枚举/流程/默认值。能指的 HTML 上位由 009/010 承担产品缺陷。**技术性**。本席**不**改判添加。

工程缺陷：**是**。GEN-003：测试须执行目标行为且能失败。现 38 绿不能红 GR-01（真实目录无 id 只警告）、不能红 GR-02（href 写死无前缀）、链测标题声称空最终人但体只测 `"usr_gm"`。席甲「evaluateOnce 另有覆盖」不成立：`evaluateOnce` 不读 body，不能证明「不带 `final_approver_id`」。产品空值已由 L243+`mapKeys` 实现，011 只补能失败断言，不改产品空值逻辑。

拟方案核：只在原两测试文件覆盖——真实目录每行 policyCode 与明细对应、根路径与 `/complex` href、空和非空 `finalApprover` 请求体；不新框架；保留其余断言。

独立同意：

- 主文件已有 `REAL_POLICIES`：补每行唯一 `data-testid`（policyCode）及点行后抽屉对应该码；这才能红 009。
- 链文件已有 href 与填值最终人：补 `final_approver_id` 缺键；补 pathname `/complex/admin` 时 href 为 `/complex/admin/governance/approval-chain?review_id=…`（复用 `basePath.test.ts` 已有的 `location.pathname` 桩，不是新框架）。
- 不删 001—008 的权限/金额/身份/节点/中文/附件/转交断言。
- 不新测试文件、不加 Playwright/全后端。

最小：原两文件补上述能失败断言。009 选择器同步与 011 唯一行、010 产品 href 与 011 双部署断言，B3 可写在同一测试块，diff 逐块双标 ID。票：**PASS（是）**。

### 001—008（本批明确不重开）

B5 顺从面仍覆盖：局部中文、policyCode 附件、节点两终态、合同无受益人可转、三动作评估先要受益人、用户编号标签、服务端拒绝不上报成功。无「未修好」新证据。队列保持 **否**。禁止本批 B3 重写那八条的产品行为。

### 四分类汇总（本批无添加则明示）

判定顺序冲突 → 细化 → 技术性 → 添加。

| 分类 | 本批 |
|---|---|
| 冲突 | **无** |
| 细化 | 009、010 |
| 技术性 | 011（无业务发明：只让测试能失败于已有行为） |
| 添加 | **无**。不发明策略 id、不发明 SPA 路由、不发明新审批链页、不把观察升成业务规则。无 `questions/`，不触发 2.2 |

无 HTML 修改申请。无锁外判据。无 LOCK-AMEND。

### 三文件边界与越界红线

| ID | 是否够三文件 | 是否新业务/超界 |
|---|---|---|
| 009 | 是（本页 key/testid + 原测试选择器） | 否。禁止 `types.ts`、禁止后端 id、禁止兼容层 |
| 010 | 是（本页 `<a href={withBase(...)}>` + 链测双部署） | 否。禁止 `navigateTo`、禁止新路由、禁止改 `ApprovalChainPage` |
| 011 | 是（原两测试文件） | 否。禁止新框架/第三测试文件 |

B3 若改 Java、共享 types、链页、HTML、锁，属越界，由 B4 闸 3 整批回滚。本票不授权那些路径。铁律 6：用已有 `policyCode` 和已有 `withBase`，删除错误身份键与裸路径；不是必须新增。

三项均工程缺陷是、队列是，须 **整批** 下一次 B3（累计将 1→2，本席不写 ledger）。Grok 本批 B2 与席甲本批 B2 双 PASS 后才能修。本席不修。

### 真实执行与限制

| 项 | 结果 |
|---|---|
| 锁哈希独算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（正文叙述，非独占行） |
| B5-r1 两席 V0.01 | 全文从各自最后独占锁行读完 |
| 3 文件 + 后端记录 + basePath + 链页读参 | 本步重读；SHA/1018 与 B5 清单一致；未改 |
| vitest | 本步实跑 exit 0 / 38 绿 / REAL_POLICIES 重复 key stderr |
| 浏览器 / 真实 HTTP / 后台测试 | **本步未跑** |
| 写入 | 仅追加本文件 |
| 子代理 / MCP / 对席同轮 B2 | 未使用、未读 |

### 整批结论

**PASS**

3 行全部给票，无漏并、无误合、无待补、无条件通过、无添加项、无 HTML 申请、无新判据。001—008 不重开。本票不授权 B3、不修改台账、**不表示代码已修、不表示模块 PASS**。合取须席甲本批 B2 亦 PASS。不覆盖 V0.01 B1 FAIL 与 V0.02 首次 B2。
