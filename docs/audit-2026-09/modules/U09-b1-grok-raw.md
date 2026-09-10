CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U09 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 9 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U09 — 未发布材料第 9 段「主管审批联动」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：1 文件，物理行 890，标记 NORMAL；main 主归属 M040
- 冻结判据：M040 映射行并集 106 项（python 集合相对开工提示 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U09-001 … GR-U09-005
- 添加项（B1 只登记，不代用户回答 2.2）：**无**。两条新策略分别落实 BSM L1443「排班表最终发布须经该主管的上一级审批后生效」与 BSM L1458「调整须经人力、财务、法务三审并由顶层平台公司最终确认」，不是第二轴发明。
- 同根已定稿、不新开 2.2：U05-008 / U06-007（以 main 为底重放、跨段同 PR、迁移 ≥V411）；U06-004（顶班入口，本文件无顶班策略，正确保持沉默）
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（1872 bytes，SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | `/tmp/u09-b1-grok` 把本段 1 文件盖到 main 后端源码树的离线 `JAVA_HOME=temurin-21 mvn -o -DskipTests compile` 与 `SupervisorReviewServiceTests`；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段服务 → U05 `StaffApprovalService` → U10 V407 → U11/U12 测试 → main 对照 → SQL/HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；未跑的共享库集成测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（`required=false` 字段注入、动作码硬编码闸），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 两条新策略必须存在吗？必须。BSM L1443 明文「排班表最终发布须经该主管的上一级审批后生效」；BSM L1458 明文「各类扣除合计不得超过……20%……调整须经人力、财务、法务三审并由顶层平台公司最终确认」。删除后这两句做不到。不是添加。
2. 整文件覆盖当前 main 会不会回退 M009–M012？**对本文件不会。** `git rev-parse` 基线 blob = HEAD blob = `fb7d2a42306a488d1e82706e39fee90f3c43e361`（SHA256 `c707867d…`）。M001–M012 未改本路径。整棵未发布树覆盖仍会回退其它段已修项（U08 已证实 PlatformService 等）。
3. 只把本文件合到当前 main，首轮会不会编不过？不会。`/tmp/u09-b1-grok` 离线 compile **exit 0**。但 main 单测 `listPolicies` `isEqualTo(10)` 变 12，**41 测 1 失败**。
4. 老板能否在三审完成前最终确认处罚上限？在「发起人无直接上级、主线只剩 master 节点」的首轮形状下会。隔离探针：master 先批 → `DEPARTMENT_REVIEWS_PENDING`。同形状税费策略（基线）master 先批成功、整单仍 `pending_review`。本段闸只覆盖 `staff.payroll.penalty_cap.adjust`。
5. 事件落地两三件不巧才丢吗？Spring Boot 正常装配会注入 `ApplicationEventPublisher`。`required = false` + U12 `new SupervisorReviewService(四参)` 使单测永远发不出事件；只合本文件不合 U05 监听器时，批准后排班/上限不落地。首轮「只合本段」会发生。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：`M040 →` L6419；python 并集 106，相对开工提示 MISSING=[] EXTRA=[]

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.67，490794 bytes，SHA256 `2d3247d01ed47c187b12c7b99d2b8720905d75b5404650251a66771df6d635c1`（与开工提示一致）。findings V1.59，612672 bytes，SHA256 `022a7ff89583dcd1a87854b1f7994f54ffd1864b6bddec42cb3092334d7cbf3c`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U05-008 / U06-007 / U07-005 / U08-002：以 main 为底重放，禁止整树覆盖；本文件 main=基线，净发布物 = 本段 +14/−1
- U05 `StaffApprovalService`：排班/上限的发起方与 `@EventListener` 落地（第 5 段）
- U10 V407：`hr_review` 线种子 + `staff_penalty_cap_revisions`（第 10 段；main 已 V410，编号须重排）
- U11 `M010RegressionTests`：`shiftIsNotPublishedUntilSupervisorsSuperiorApproves`、`penaltyCapNeedsThreeDepartmentReviewsThenMasterAndDoesNotRewriteClosedMonth`（不断言错误码）
- U06-004：顶班入口冲突，本文件无 `staff.shift.cover` 类策略，与 HTML「顶班不受本条审批流程限制」同向
- M006-027 等 main 既有项（受益人自审、职级解析）归 M006/M040 正式模块，本材料未加重

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。锁内本段 88 条 HTML 派生判据出处行对现行正本 **88/88 命中**（标题窗口匹配 0 处 mismatch）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。PGM-029（含「全平台审批通用时限」全文）**不在** M040 映射，本席不用它作 finding 支撑 ID。

清单本段 1 条与独立 `sha256sum`/`wc`/`git cat-file` **1/1 match**：890 行、55916 bytes、`eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805`。副本 SHA 与树 `e28dad05:backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` blob（`5ed1090a3c648140bcce2a2b225fb9292ce56943`，55916 bytes）相同。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --shortstat` 实算，与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | + | − |
|---|---:|---:|
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | 14 | 1 |
| **合计** | **14** | **1** |

`git diff --stat` 汇总：`1 file changed, 14 insertions(+), 1 deletion(-)`。ledger 摘要写 +15/−1，以 git 为准。

相对 HEAD 同为 +14/−1（因本文件 HEAD blob = 基线 blob）。文件版本头 V0.07（main/基线）→ V0.09（未发布）。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

锁定义区 106 条全部读过。GEN 无 HTML 行。其余 88 条锁出处行在现行三 HTML 上均存在，标题/词条与锁摘录一致。本段直接用到的原文：

- BSM-086 / `business-system-map-for-operations-v0.1.html:1443`：「主管排班：……排班表最终发布须经该主管的上一级审批后生效。班次一经开始即锁定。但当班有人未到、需要当场顶班的，由领班直接安排其他工作人员顶班并在系统中记录顶班人员，不受本条审批流程限制……每一次修改同样须经该主管的上一级审批。」
- BSM-087 / 同文件 `:1458`：「各类扣除合计不得超过该员工当月总薪资的 20%（安全阈值，全平台统一可配，调整须经人力、财务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）」
- BSM-001 / 同文件 `:521`：「所有涉及钱的配置一律版本化……无法判断是否涉及钱的，一律按涉及钱处理。」
- PGM-041 / `platform-governance-map-for-operations-v0.1.html:647` 附近：「敏感动作可要求上一级、上两级或最高权限审批。」「税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」（税费三审是既有策略；处罚上限的三审来自 BSM-087，人力≠税务）。
- PGM-066 / `:951`：「界面全中文……可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样。」本段错误码说明为中文；`staff.shift.publish` 等为内部标识符。

## 2. 文件全量覆盖与第一性原理

文件：`.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java`（V0.09，890 行）。整文件按结构读完，不是只看 diff。

| 段 | 行 | 覆盖 |
|---|---|---|
| 包/版本/注入/策略目录 | 1–80 | 读完。新：`eventPublisher` 字段注入；`staff_shift_publish` / `staff_penalty_cap_adjust` 两条策略 |
| `listPolicies` | 82–93 | 读完。目录现 12 条（基线 10） |
| `evaluate` + 平行线闸 | 95–171 | 读完。`requireDefaultLines` 对处罚上限强制三线最终审批人 |
| `approve` | 178–297 | 读完。新：L214–219 `DEPARTMENT_REVIEWS_PENDING`；L292 `publishEvent` |
| 节点身份 / 角色闸 / 指定最终审批人 | 299–393 | 读完。未改 |
| `blockForAdmission` 三过载 | 395–490 | 读完。空平行线走 `requireDefaultLines`，处罚上限不能从准入无平行线路径发起 |
| 链生成 / 重生成 | 492–559 | 读完。未改 |
| 策略查找 / rank | 561–607 | 读完。未改 |
| 时限 / 升级 | 609–704 | 读完。`isMoneyAction` **未改**、未纳入新动作 |
| 转发 | 706–790 | 读完。未改 |
| 待审清单 / 放行消费 | 792–856 | 读完。未改 |
| `isAlwaysBlocked` / 证据 / normalize | 858–890 | 读完。`isAlwaysBlocked` **未**列入新动作；`before_after` 仍不强制 |

**为什么必须存在：** 主管复核链是合同/准入/高危授权/税费调整的共用引擎（PGM-007/PGM-041/BSM-004）。本段增量让员工排班发布与扣除上限调整复用同一引擎，对应 BSM-086/BSM-087。删除本文件则既有复核与两条新事项都失去服务端闸。

**声称是否执行：**

- 目录含两条新动作：未发布 U12 `listPolicies` 断言 `total()==12` 且 `contains` 两码；本席 overlay 实跑 41/41 PASS。
- 排班 `one_level_up`、缺平行线拒绝、master 先批处罚上限被拒、税费 master 仍可先批：隔离探针 3/3 PASS（见 §9）。
- 事件落地：U12 用四参构造，`eventPublisher==null`，批准不抛错也不发事件。U05 监听器不在本段。
- 共享库 `complex_test` 集成测试**未跑**（禁止写库）；U11 回归只作只读源码对照，不把未跑测试写成通过。

**未验证前提：** U11 在空库的 41/41（ledger 记载席甲曾跑）本席未复跑，不采信为己证。`hr_review` 在真实库是否已种子，只核 V407 文本。

## 3. 顺从式：106 项冻结判据

每条都对照本文件（及只读调用链）。分类：直接适用 / 引擎切片适用 / 无实现面。无 finding 不等于未审。

### 3.1 GEN-001–018

| ID | 适用 | 证据 / 缺口 |
|---|---|---|
| GEN-001 | 适用 | 两条策略各有 HTML 一句；`eventPublisher` 是落地耦合不是多余抽象。硬编码动作闸略窄，见观察 |
| GEN-002 | 适用 | overlay compile exit 0；main 单测 10≠12 失败；时限分类错误见 GR-U09-002 |
| GEN-003 | 适用 | U12 只改目录计数，新闸/事件无单测可失败断言 → GR-U09-004 |
| GEN-004 | 适用 | `approve` 职级来自会话不来自请求体（L192 注释+代码）；`requireText` 校验 |
| GEN-005 | 适用 | 自审拒绝 `initiatedBy`；受益人拒绝 `schedule.beneficiaryId`；master 须 USB 主会话。授权申请受益人由准入路径传入，本段未改 |
| GEN-006 | 适用 | `@Transactional` + `lockSupervisorReview` + 节点 CAS；事件与批准同事务（默认同步 `@EventListener`） |
| GEN-007 | 适用 | 幂等 `execute`；节点 CAS `APPROVAL_NODE_ALREADY_DECIDED` |
| GEN-008 | 适用 | 新动作码须与 U05/U10/U11/U12 同批；`isMoneyAction` 与税费策略分叉 → GR-U09-001/002/003/005 |
| GEN-009 | 适用 | 缺线 fail-closed；`required=false` 吞掉事件见观察 |
| GEN-010 | 适用 | 无注入点；审计含 `violation_summary`（排班提案含姓名，PGM-043 员工姓名不脱敏） |
| GEN-011 | 适用 | 评估/节点/整单/升级均 `recordSuccess`；另发 `admission.released`（基线既有，员工单也发） |
| GEN-012 | 适用 | `escalateOverdue` 批 200；`listPending` 先取 limit 再过滤（基线 ponytail 注释） |
| GEN-013 | 适用 | 单文件叠 main 可离线编译；依赖 Spring `ApplicationEventPublisher` |
| GEN-014 | 适用 | 本段无迁移；运行依赖 U10 `hr_review` → GR-U09-003 |
| GEN-015 | 无实现面 | 后端服务，无 UI。报错中文，满足 PGM-066 切片 |
| GEN-016 | 适用 | 注释引用 PGM-1.7.1 / PGM-1.10.1；本 raw 命令可复现 |
| GEN-017 | 适用 | 新策略=HTML 落实，不是添加；缺口按冲突→细化→技术性→添加判定，本批 finding 均为技术性 |
| GEN-018 | 适用 | 结论落到 HTML 原句与 `/tmp` 命令 |

### 3.2 BEQR（27）

| ID | 适用 | 说明 |
|---|---|---|
| BEQR-063 | 引擎切片 | 高危动作进复核目录（`manual_release`/`bill_waiver` 等）；本段未改这些策略 |
| BEQR-074 | 无实现面 | 隐私桌台权限不在本服务 |
| BEQR-143–148 | 无实现面 | 四类主体/合作模式；本文件不读写商户档案 |
| BEQR-149 | 切片 | 职级影响 `requiredApproverRank` / `currentSupervisorLevel`；本段未改解析器 |
| BEQR-150 | 无实现面 | 终端十一端 |
| BEQR-151–153 | 切片/无 | 最终权限在 `AuthorizationService`；本文件只要求 `governance.supervisor_review.*` |
| BEQR-154–159 | 无实现面 | 项目/商户/合同/员工/设备/权限状态枚举不在本文件定义 |
| BEQR-166 | 引擎切片 | 合同审批链复用本引擎；本段未改合同策略 |
| BEQR-182–185 | 无实现面 | 考勤/打卡/请假枚举由员工域承载；本文件只提供排班发布闸 |
| BEQR-186 | 切片 | 薪资单构成含处罚；上限调整策略为本段新增，封顶计算在 U05 仓储 |
| BEQR-187 | 无实现面 | 提成发放 |
| BEQR-189 | 无实现面 | 管理费方式 |

### 3.3 BSM（23）

| ID | 适用 | 说明 |
|---|---|---|
| BSM-001 | 适用 | 涉及钱的配置须审批；处罚上限是薪资安全阈值。时限未按本文件既有涉钱分类走 1 日 → GR-U09-002 |
| BSM-003 | 切片 | 权限分岗；本文件按权限码分 view/manage/approve |
| BSM-004 | 引擎切片 | 删除走最高权限审批；本段未改删除策略 |
| BSM-005–006 | 无实现面 | 分步引导/动态二维码 |
| BSM-007 | 切片 | 平台管理员日常功能 ≠ 最终业务审批；master 节点另认 USB |
| BSM-009 | 切片 | 基础管理含员工注册审批；本文件是复核引擎 |
| BSM-028–029 | 切片 | 管理员工/岗位/班次/职级/审批；调用方在 U05/U06 |
| BSM-072–074 | 无实现面 | 商户侧完整使用 |
| BSM-084 | 适用 | 员工手机端排班/薪资；发布与上限须本引擎 |
| BSM-085 | 无实现面 | 日常工作注册路径在 U05 |
| BSM-086 | **直接适用** | `staff.shift.publish` + `one_level_up` + `requiredLevel=1` 对齐「上一级审批」。顶班无策略=正确沉默 |
| BSM-087 | **直接适用** | `staff.payroll.penalty_cap.adjust` + `hr_review/finance_review/legal_review` + `top_authority` + `DEPARTMENT_REVIEWS_PENDING` 对齐「三审并由顶层最终确认」 |
| BSM-088 | 无实现面 | 提成/陪场边界 |
| BSM-114 | 切片 | `listPending` 按项目/商户过滤 |
| BSM-115 | 切片 | 审批连接员工与治理 |
| BSM-120 | 切片 | 已确认规则；本段落实其中排班/上限两条 |

### 3.4 PGM（38）

| ID | 适用 | 说明 |
|---|---|---|
| PGM-001–003 | 切片 | 治理底座/顺序/模块入口含权限职级与合同准入 |
| PGM-004–006 | 无实现面 | 综合体/空间/商户主数据 |
| PGM-007 | 引擎切片 | 合同和准入复用本链 |
| PGM-008 | 适用 | 员工和手机；排班/上限从员工域进入本链 |
| PGM-009 / PGM-040–042 | 适用 | 单一功能授权；`VIEW/MANAGE/APPROVE` 三码；档位 one_level_up / top_authority |
| PGM-010–013 | 无实现面 | 终端开关/设备/商户侧/开放接口 |
| PGM-014 | 适用 | 留痕复核；本文件是实现面 |
| PGM-015–016 | 无实现面 | 楼宇/应急专条 |
| PGM-017–021 | 切片/无 | 集团视图与主体职责不在本文件配置 |
| PGM-022 / PGM-024 | 无实现面 | 空间点位 |
| PGM-026 | 切片 | 含财税主体调整并行审核；本文件既有税费三审，新增人力三审 |
| PGM-027 | 无实现面 | 商户业务边界 |
| PGM-031 | 无实现面 | 经销商资金 |
| PGM-032–036 | 无实现面 | 手机/企微/提成/设备（本文件不绑设备） |
| PGM-041 | **直接适用** | 敏感动作档位；税费「再由老板确认」基线仍无顺序闸（观察）；处罚上限本段有闸 |
| PGM-043 | 切片 | 员工姓名不脱敏；审计带排班提案姓名 |
| PGM-044 | 无实现面 | 桌台挂单 |
| PGM-048 | 无实现面 | 商户侧全开放 |
| PGM-059 | 切片 | 不保存证件；本文件无证件字段 |
| PGM-060 | 无实现面 | 会员生物信息 |
| PGM-065 | 切片 | 治理连接员工/财务 |
| PGM-066 | 适用 | 报错中文；内部标识符除外 |

**顺从结论：** 两条新策略与 HTML 同向，不是添加。顺从缺口集中在工程面：时限分类、迁移/监听同批、单测覆盖、发布叠 main。

## 4. 对抗式

构造并实跑（隔离，不写 `complex_test`）：

| 反例 | 结果 | 收窄 |
|---|---|---|
| 处罚上限不传平行线 | `APPROVAL_LINE_FINAL_APPROVER_REQUIRED` | 首轮会撞，fail-closed，与税费同形 |
| 无上级时 master 先批处罚上限 | `DEPARTMENT_REVIEWS_PENDING` | 首轮 U11 形状会撞；闸有效 |
| 只过 hr 线再 master | 仍 `DEPARTMENT_REVIEWS_PENDING` | 三线未齐 |
| 无上级时 master 先批**税费** | 整单 `pending_review`，master 节点 `approved` | 基线既有；本段未修 |
| 排班 `one_level_up` 由直接上级批 | 整单 `approved` | 与 BSM-086 同向 |
| U12 四参构造下批准 | 不抛错、不发事件 | 单测盲区 |
| 只叠本文件跑 main 单测 | 41 测 1 失败，`expected 10L but was 12L` | 首轮会撞 |
| 负 `amountCent` 且空事由逃避恒进复核 | 未跑（非首轮正常路径；U05 传 0L+事由） | 收窄为观察 |
| 并发双末节点 | 基线已有锁+CAS，本段未改 | 不新开 |
| 自审 | `initiatedBy` 闸仍在 | 本段未改 |

越权/重复/状态跳转：未发现本段新引入的越权。金额精度不在本文件。配置漂移：新 `policy_code` 不进 `supervisor_review_policies` 表，时限只能走代码默认。

## 5. 与 main 分叉对照（对抗式固定问题）

本文件：

| 对象 | blob | SHA256 | 行 |
|---|---|---|---:|
| 基线 `242f4872` | `fb7d2a42…` | `c707867d8696e8c8c19ac2354579362ea912ff13fab8625eacee612ea26691c2` | 877 |
| HEAD `445a49f4` | **同一 blob** | 同 | 877 |
| 未发布树 `e28dad05` | `5ed1090a…` | `eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805` | 890 |

`git log 242f4872..HEAD -- <本路径>` 为空。M009–M012 **没有**改本文件。因此：

- **单文件覆盖 main ≠ 回退 M009–M012。** overlay compile exit 0。
- **单文件覆盖会打破 main M042 单测**（目录 10→12）。
- **整棵 76 文件树覆盖会回退** 其它段 main 已修项（U08：`parentEntityId` 删除、商户 25 参构造等）。禁止整树覆盖。
- 发布方式：以 main `445a49f4` 为底 **重放本文件 +14/−1 hunk**，并与 U05/U10/U11/U12 同 PR（迁移 ≥V411）。

相对基线 hunk 仅五处：版本头；`eventPublisher` 字段；两条策略；`DEPARTMENT_REVIEWS_PENDING`；`publishEvent`。

## 6. 数据流（副本 → 其它未发布段 → main → SQL → HTML）

```
U06 StaffService.upsertShift / requestPenaltyCap
  → U05 StaffApprovalService.submitShifts / submitPenaltyCap
      → 本段 evaluate(staff.shift.publish | staff.payroll.penalty_cap.adjust)
          → ApprovalChainService.generate（main 当前，本材料未改）
          → SupervisorReviewRepository / JdbcPlatformRepository（main）
          → approval_lines：finance/legal/tax 在 main V391；hr_review 仅 U10 V407
      → 保存 staff_shift_revisions / staff_penalty_cap_revisions（U10 V406/V407）
  → 本段 approve 终态
      → eventPublisher.publishEvent(SupervisorReviewDecisionRecord)
          → U05 StaffApprovalService.afterDecision
              → publishShiftRevision / applyPenaltyCapRevision
```

main 无 `StaffApprovalService`、无两策略、无 `hr_review`、无 `publishEvent`。只合本文件：策略可评估，但平行线 `hr_review` 未配置会 `APPROVAL_LINE_NOT_CONFIGURED`；批准后无监听器则排班/上限不落地。

## 7. Findings（临时 ID，B2 才分配正式 ID）

### GR-U09-001 — 以 main 为底重放；禁止整树覆盖；须与 U12 同 PR

- **文件:行号：** `SupervisorReviewService.java:3`（版本 V0.09）；相对 main 全文 +14/−1
- **完整原文：** 未发布树相对 HEAD 仅本文件 14 插入 1 删除；main 同文件自基线零提交。main 测试 `assertThat(page.total()).isEqualTo(10)`。
- **业务影响：** 整树覆盖会回退其它文件上的 M009–M012 已修项。只合本文件会让已发布 M042 单测红。不是业务口径冲突。
- **可复现：** `git diff --numstat 242f4872 HEAD -- <path>` 空；`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -Dtest=SupervisorReviewServiceTests test` 在只叠服务、保留 main 测试时 **Tests run: 41, Failures: 1**，`expected: 10L but was: 12L`。叠未发布测试后 41/41 PASS。
- **冻结判据：** GEN-002、GEN-003、GEN-008、GEN-013
- **2.3 依据类型：** 可复现命令
- **四分类：** 技术性
- **无业务发明理由：** 不改 HTML 规则，只约束发布叠法。
- **工程缺陷：** 是。建议登记：是。
- **最小处置（写给发布者）：** 以 main `445a49f4` 为底 cherry-pick/重放本文件 hunk；U09+U05+U10+U11+U12 同一 PR；迁移编号 ≥V411；禁止 `e28dad05` 整树覆盖。

### GR-U09-002 — 处罚上限未进入 `isMoneyAction`，出厂时限 3 个工作日

- **文件:行号：** `SupervisorReviewService.java:626–631`（`isMoneyAction`）；调用点 `:616–618` `scheduleDeadline`
- **完整原文：**
```
        return type.startsWith("finance.") || type.endsWith(".money")
                || List.of("bill_waiver", "refund", "ar_ap_adjustment", "bar.bill_waiver",
                "ticketing.refund_approve", "ticketing.verification_reverse").contains(type);
```
  不含 `staff.payroll.penalty_cap.adjust`。V389 只 UPDATE 表内行；新 `policy_code` 不在 `supervisor_review_policies`，`findDeadlineWorkingDays` 为空，回落到上述函数。
- **业务影响：** 扣除上限是薪资安全阈值（BSM-087），属涉及钱的配置（BSM-001）。同文件税费调整 `finance.tax_rule.adjust` 因 `finance.` 前缀走 1 个工作日；处罚上限走 3 个工作日。首轮发起即用错默认时限。
- **可复现：** `/tmp` 探针 `u09ProbePenaltyCap…` 断言 due≥48h PASS；`u09ProbeTaxRule…` 断言 due<40h PASS。
- **冻结判据：** GEN-002、GEN-008、BSM-001、BSM-087
- **2.3 依据类型：** 可复现命令 + HTML 原文（BSM-001「涉及钱」；BSM-087 上限调整）
- **四分类：** 技术性
- **无业务发明理由：** 不新造「处罚上限 1 日」业务规则；只是让新动作加入本文件已经用来实现涉钱/非涉钱分档的 `isMoneyAction`。PGM-029 时限全文不在 M040 映射，故不引用为支撑 ID。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** `isMoneyAction` 纳入 `staff.payroll.penalty_cap.adjust`（或 `staff.payroll.*`）；若保留 DB 参数，同时插入 `supervisor_review_policies` 行并设 `deadlineWorkingDays.value=1`。排班发布保持 3 日。

### GR-U09-003 — `hr_review` 与新 policy_code 不在本段交付面，缺 U10 则首轮发起失败

- **文件:行号：** `SupervisorReviewService.java:53–55`（`List.of("hr_review", "finance_review", "legal_review")`）；`ApprovalChainService.parallelNodes` 查 `approval_lines`
- **完整原文：** 策略固定三线含 `hr_review`。main V391 只种子 `finance_review` / `tax_review` / `legal_review`。U10 `V407__staff_penalty_cap_approval_history.sql:13–14`：`INSERT INTO approval_lines … ('hr_review','人力审核线',…)`。V407 不 INSERT `supervisor_review_policies`。
- **业务影响：** 只合本文件、不合 V407：`evaluate` 生成平行线时 `APPROVAL_LINE_NOT_CONFIGURED：hr_review`，扣除上限无法发起，BSM-087 做不到。main 已有 V410，V407 编号还须重排（U10 段）。
- **可复现：** 读 V391 与 V407；`requireDefaultLines` + `parallelNodes` 源码。未对共享库跑迁移。
- **冻结判据：** GEN-002、GEN-008、GEN-014、BSM-087
- **2.3 依据类型：** 可复现命令（读文件）+ HTML 原文
- **四分类：** 技术性
- **无业务发明理由：** 不发明第四条审批线；只要求代码引用的 `line_code` 与迁移种子同批存在。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** 与 U10 同 PR；`hr_review` 种子迁到 ≥V411；可选把两条新策略写入 `supervisor_review_policies` 以便时限可配。

### GR-U09-004 — U12 单测未执行新闸与事件，41/41 不能证明新行为

- **文件:行号：** 未发布 `SupervisorReviewServiceTests.java:55–63`（`isEqualTo(12)` + `contains` 两码）。本段服务 `L214–219`、`L292`。
- **完整原文：** 相对 main 测试，U12 只改目录断言。无 `DEPARTMENT_REVIEWS_PENDING`、无 `staff.shift.publish` evaluate/approve、无 `publishEvent` 断言。本席叠未发布测试：**Tests run: 41, Failures: 0**——绿灯不经过新分支。
- **业务影响：** 回归可以在闸被删或事件被去掉后仍绿。U11 有集成路径但 `assertThatThrownBy(...).isInstanceOf(ApiException.class)` 不断言错误码，MASTER 身份失败也会绿。
- **可复现：** 上述 41/41；对比 `git diff` U12 该测试方法。隔离探针 3 条不在材料内，不能算交付测试。
- **冻结判据：** GEN-003、GEN-002
- **2.3 依据类型：** 可复现命令
- **四分类：** 技术性
- **无业务发明理由：** 要求测试执行已声称行为，不新增业务规则。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** U12 补可失败用例：①缺 `hr_review` 最终审批人；②无上级时 master 先批 → `DEPARTMENT_REVIEWS_PENDING`；③排班 `one_level_up` 链；④注入 `ApplicationEventPublisher` 并断言发布。U11 补错误码断言。同 PR。

### GR-U09-005 — 批准后落地只靠可选事件，只合本文件则排班/上限不生效

- **文件:行号：** `SupervisorReviewService.java:39–40`、`:292`；对照 U05 `StaffApprovalService.java:69–82`
- **完整原文：**
```
    @Autowired(required = false)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    ...
                    if (eventPublisher != null) eventPublisher.publishEvent(result);
```
  监听器：`if (!"approved".equals(decision.reviewStatus())) return;` 然后 `applyPenaltyCapRevision` / `publishShiftRevision`。不走 `consumeApprovedRelease`。
- **业务影响：** HTML 要求「审批后生效」。只发布本文件：复核单可变为 approved，排班表仍未写入、上限仍 2000。`required = false` 使缺失发布器时静默成功。Spring Boot 全应用首轮会注入；「只合本段 / 非 Spring 构造」首轮会丢落地。
- **可复现：** U12 四参构造下移位探针批准成功且无监听器；读 U05 监听器。未跑 Spring 集成。
- **冻结判据：** GEN-002、GEN-008、GEN-009、BSM-086、BSM-087
- **2.3 依据类型：** 可复现命令 + HTML 原文
- **四分类：** 技术性
- **无业务发明理由：** 不改「须审批才生效」；要求发布器与监听器同批、失败可见。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** 与 U05 同 PR；构造函数必填注入 `ApplicationEventPublisher`（删 `required=false`）；或批准成功路径改为显式调用员工落地（仍同事务）而不是可选事件。禁止只合本文件。

## 8. 观察（无新根因，不入修复队列建议「否」）

1. 本文件 main blob = 基线 blob；与「同名文件都已与 main 分叉」的开工全称相比，本段是例外。对抗式仍记录整树覆盖风险（并入 GR-U09-001）。
2. 税费 `finance.tax_rule.adjust` 在无上级时仍允许 master 先把主线节点标 approved（探针 PASS）。HTML PGM-041「再由老板最终确认」的顺序闸基线就没有。本段只给处罚上限加闸，未加重税费。发布者可把闸改成「`parallelLineCodes` 非空且当前节点 `KIND_MASTER`」，属顺手收窄，不单开 finding。
3. `isAlwaysBlocked` 未列新动作；`minAmountCent=0` 且 U05 传 `0L`+事由，首轮仍恒进复核。负金额且空事由非首轮。
4. `requiredEvidenceTypes` 含 `before_after` 但 `requireDecisionEvidence` 只强制附件/意见。税费基线同样。U05 把前后值写入 `violationSummary` 并落修订表。
5. 整单终态仍发 `admission.released` / `admission.escalated`（L282–291，基线既有），员工单也用准入事件名。
6. U11 不断言 `DEPARTMENT_REVIEWS_PENDING` 码，归 U11 观察；本段以 GR-U09-004 要求 U12 补码。
7. 未发布提交对象本仓不可解析，审查以 tree `e28dad05` 为准。
8. 版本头 V0.07→V0.09，材料内无 V0.08。
9. `listPolicies` 自身不 `requirePermission`，由控制器层（M040 其它文件，不在本段 76 文件此条）。
10. 顶班：本文件无策略，与 BSM-086「不受本条审批流程限制」同向；入口缺失已定稿 U06-004。
11. `consumeApprovedRelease` 不被员工路径使用；员工靠事件。并入 GR-U09-005。
12. `ponytail:` `eventPublisher` 字段注入 vs 构造函数；建议随 005 改构造，不另开。
13. PGM-029 不在映射，1 个工作日的 HTML 全文不能当支撑 ID；GR-U09-002 只用本文件已有分类 + BSM-001/087。
14. 本席未跑 U11 JDBC 回归，不把 ledger 记载的 41/41 写成自己的通过证据。
15. 默认 javac 不在 PATH 的 `java-21-openjdk`（无 javac）会报 `release version 21 not supported`；须 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`。属本机工具链，不是业务 finding。

## 9. 命令证据

```
# 锁
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# → b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# 对象
git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb   # fatal
git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b   # tree
git cat-file -t 242f487233d028173cb9198eecd079f5d877405c   # commit
git rev-parse HEAD origin/main                              # 445a49f4… 两次相同

# 本段文件
sha256sum .scratch/.../SupervisorReviewService.java
# eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805
git diff --numstat 242f4872 e28dad05 -- backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
# 14  1  …/SupervisorReviewService.java
git diff --shortstat 242f4872 e28dad05
# 76 files changed, 4745 insertions(+), 1084 deletions(-)
git rev-parse 242f4872:…/SupervisorReviewService.java HEAD:…/SupervisorReviewService.java
# 两者 = fb7d2a42306a488d1e82706e39fee90f3c43e361

# overlay 编译（/tmp/u09-b1-grok，JAVA_HOME=temurin-21）
mvn -o -DskipTests compile     # EXIT 0；class 含 staff.shift.publish / DEPARTMENT_REVIEWS_PENDING

# main 测试 × 未发布服务
mvn -o -Dtest=SupervisorReviewServiceTests test
# Tests run: 41, Failures: 1  expected 10L but was 12L  EXIT 1

# 未发布测试 × 未发布服务
# Tests run: 41, Failures: 0  EXIT 0

# /tmp 探针（不在仓库）
# u09ProbePenaltyCap… / Shift… / TaxRule…  3/3 PASS
# 证明：缺线拒绝；DEPARTMENT_REVIEWS_PENDING；处罚 due≥48h；税费 due<40h 且 master 可先批主线节点
```

未跑：任何写 `complex_test*` 的脚本、U11 JDBC、部署、安装依赖。

## 10. 结论

**FAIL。**

覆盖：本段 1/1 文件整文件三线；106/106 判据逐项；与 main 分叉已对照；数据流已追到 U05/U10/U11/U12 与 HTML。无未答 2.2（无添加项）。有五条建议入队的技术性工程缺陷（GR-U09-001…005）。不存在有条件通过。

发布者最小包：以 main 为底重放本文件 hunk + U05 监听器 + U10 `hr_review`（≥V411）+ U11/U12 测试；`isMoneyAction` 纳入处罚上限；构造函数必填事件发布器；禁止整树覆盖。

## U09 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 38123 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 9 段：主管审批联动」（1 文件 890 行，main 归属 M040，判据＝M040 映射行并集 106 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。本文件 `HEAD:<path>` = `242f4872:<path>` = blob `fb7d2a42306a488d1e82706e39fee90f3c43e361`（M009–M012 未改本路径）。未发布树 `e28dad05` 对应副本 blob `5ed1090a3c648140bcce2a2b225fb9292ce56943`（SHA256 `eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805`，890 行 / 55916 字节）。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–006 逐 ID 全 PASS；添加 0、不发新 2.2；发布前须修「是」=001–005，「否」=006）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M040 →` L6419；python 并集 106，MISSING=[] EXTRA=[]，`PGM-029 in M040` = False。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、numstat、副本本文件/U05 监听器/U10 V407/U12 测试/U11 回归、main V391/V410/`isMoneyAction`、HTML 锚点、`questions/U04-001.md` 答复首行「删除」）；未重跑 mvn/测试、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u09-b2-append.md` 中转，落盘后删除。

此票只判断 findings.md V1.60 顶部「当前 U09 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U09-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 38123 | `72d025094461cd81ee81db640c04ec3dd689cd0b8de6120c3b089be4c9688f9a` |
| `docs/audit-2026-09/modules/U09-b1-self-raw.md` | V0.01 | 22073 | `9f9816472ce0e2c3fe353886d6e951b3baab8bc576b9dde2caa4f228db9022fe` |
| `docs/audit-2026-09/findings.md` | V1.60 | 618617 | `ce6c99b1bfb76bda71e1679c4e2f92205201797ebe8ba3acff4cfbbc46eee468` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.68 | 492411 | `21ef5e4d35595b1aa8e2f5bfaf82a4dc968eddf74e8797b1f99a8eb55b59a483` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | — | 答复首行精确「删除」（不新开 2.2；本段无自营布尔实现面） |

候选形态：V1.60 顶部 U09-001…006；questions 0（沿用 U04-001 已答删除，本段无新添加项）；添加 0；无 HTML 修改申请。U09 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 1/1 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --numstat 242f4872 e28dad05 -- <本路径>` = 14/1；`HEAD e28dad05` 同为 14/1；`242f4872 HEAD` 空。`git log 242f4872..HEAD -- <本路径>` 空。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。ledger 开工节写 +15/−1，以 git +14/−1 为准（本席 B1 已记；非阻断）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/main 与副本源码再接受或反驳席甲 OBS-1/4 降观察；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：002/005 是否确须修、BSM-001/087 锚是否够、003 是否发明第四条线、004 跨段登记是否合规、006 是否写错 §8.2/13/15 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/diff/python 并集/rg；本票不声称本轮新编译绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段服务 → U05 监听器 → U10 V407 → U11/U12 测试 → main 对照 → HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。002 只把新动作纳入既有 `isMoneyAction`；005 删 `required=false` 或显式落地；003 只引用 U10 种子 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 席甲把 002/005 记观察、票 PASS，是否应维持观察？否。见 (a)。
2. BSM-001/087 能否单独推出「1 个工作日」这个数字？不能。1 日来自本文件既有分档，不是从这两句 HTML 新造规则。PGM-029 不在映射，不作支撑。见 (a)。
3. 003 把 `hr_review` 归 U10 是否发明第四条线？否。BSM-087 只要人力/财务/法务，`tax_review` 是税费线。见 (b)。
4. 004 测试缺口登记在本段是否越界？否。GEN-003 在本段 106 项内，本文件是被测面。见 (c)。
5. 006 是否把 §8.2 改写成「本段必须给税费补闸」？否。见 (d)。

### 完整差集（GR-001…005、§8 观察 1–15、席甲 无 finding、OBS-1…5）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–006。未把本席 finding 降观察。席甲 OBS-1/4 由候选升队，与本席 GR-005/002 同向，不改写本席原意。席甲独有项无超出 OBS-1…5 与 §4/§5 预注。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U09-001 | U09-001 | 否：以 main 为底重放、禁整树、同 PR |
| GR-U09-002 | U09-002 | 否：`isMoneyAction` 漏处罚上限；席甲 OBS-4 升队并列 |
| GR-U09-003 | U09-003 | 否：`hr_review` 种子在 U10 V407；V407 不插 `supervisor_review_policies` |
| GR-U09-004 | U09-004 | 否：U12 只改目录断言；U11 不断言错误码 |
| GR-U09-005 | U09-005 | 否：可选事件落地；席甲 OBS-1 升队并列 |
| §8 1–15 | U09-006 | 「含」为抽样（见 (d)）；第 2/13/15 条未写错 |
| 席甲 §4 blob＝基线 | U09-001 | 与 GR-001 同一分叉事实 |
| 席甲 OBS-1 | U09-005 | 升队，不改写本席「须修」 |
| 席甲 OBS-4 | U09-002 | 升队，不改写本席「须修」 |
| 席甲 OBS-2/3/5 | U09-006 | 维持观察 |
| 席甲 GEN-014 / U05 预注 `hr_review` | U09-003 | 与 GR-003 同一种子缺口 |
| 席甲 U09 B1 GEN-003「第 12 段覆盖策略/闸」 | 不采信 | 被 GR-004 源码否决；候选已勘误接受本席 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U09-001 | GR-U09-001 + 席甲 §4 | 技术性（发布叠法，同根 U05-008）/是/是 | **PASS（技术性叠法/是/是）** |
| U09-002 | GR-U09-002 + 席甲 OBS-4（升队） | 技术性/是/是 | **PASS（技术性时限分档/是/是）** |
| U09-003 | GR-U09-003 + 席甲 U05 预注 | 技术性（跨段同批）/是/是 | **PASS（技术性同批/是/是；不发明第四条线）** |
| U09-004 | GR-U09-004 + 席甲 B2 核实 U12 体 | 技术性（测试缺口，归 U12/U11 同批）/是/是 | **PASS（技术性测试缺口/是/是；跨段登记合规）** |
| U09-005 | GR-U09-005 + 席甲 OBS-1（升队） | 技术性/是/是 | **PASS（技术性可选事件/是/是）** |
| U09-006 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；§8.2/13/15 未写错）** |

技术性入队 5（001 叠法、002 时限分档、003 `hr_review` 同批、004 测试缺口、005 可选事件落地）。登记观察 1（006）。冲突 0。添加 0。questions 0。无 HTML 修改申请。

发布前须修「是」=001、002、003、004、005（5）；「否」=006（1）。与候选汇总一致。席甲 B1 票 PASS、把 002/005 对应事实记观察，属低估；候选接受本席五项技术性并勘误，本席独立复核后维持。

### 重点 (a) 席甲 B1 票 PASS、002/005 升队须修 — **PASS（两件均确为发布前须修）**

独立核，不采信席甲「观察即可」四个字为己证。

#### 002 处罚上限走非涉钱 3 日

本轮源码（副本 `:626-631` / main 同函数未改）：

```
return type.startsWith("finance.") || type.endsWith(".money")
        || List.of("bill_waiver", "refund", "ar_ap_adjustment", "bar.bill_waiver",
        "ticketing.refund_approve", "ticketing.verification_reverse").contains(type);
```

不含 `staff.payroll.penalty_cap.adjust`。`:616-618` `scheduleDeadline`：`findDeadlineWorkingDays(policyCode).orElse(isMoneyAction ? 1 : 3)`。Jdbc 实现按 `supervisor_review_policies.parameters.deadlineWorkingDays` 查；新 `policy_code` 只在内存 `List.of`，V389 只 UPDATE 表内已有行，查空后回落分档。税费 `finance.tax_rule.adjust` 因 `finance.` 前缀走 1 日。

HTML / 锁（本轮正本）：

- BSM-087 锁出处 `:1455`（h3），现行正本 L1458：「各类扣除合计不得超过该员工当月总薪资的 20%（安全阈值……调整须经人力、财务、法务三审并由顶层平台公司最终确认……）」。**不定时限数字**，定身份：这是影响扣款金额的安全阈值，须三审+顶层确认。
- BSM-001 锁出处 `:505`（h2），涉钱句在现行正本 L521（锁 HTML 原文块内）：「系统里所有影响钱和业务判断的策略参数……各类预警阈值……所有涉及钱的配置一律版本化……无法判断是否涉及钱的，一律按涉及钱处理。」处罚上限是标明的安全阈值，属影响钱的策略参数。
- PGM-029 锁出处含「全平台审批通用时限……涉钱审批出厂默认 1 个工作日」。**`PGM-029 in M040` = False**，不得作支撑 ID。候选未列 PGM-029，合规。本文件注释 `:612-614` 引用「PGM-1.7.1 全平台审批通用时限」只说明 1/3 分档从哪抄来，不能把未映射全文当成本段判据。

四分类：**技术性**。不是冲突（HTML 对本动作沉默 1 日还是 3 日，不能写成与 L1458 原文打架）。不是添加（不新造「处罚上限必须 1 日」的业务规则）。是把新动作漏出本文件 **已经用来实现涉钱/非涉钱分档** 的 `isMoneyAction`。GEN-002 / GEN-008 够用；BSM-001 定「按涉及钱处理」，BSM-087 定「这就是那条安全阈值」。

发布前须修 **是**：首轮 `evaluate` 即给该单盖 3 个工作日到期。不是上线后才暴露的观感问题。席甲 OBS-4「HTML L1458 对时限沉默」只挡住「从 BSM-087 推出 1 日」这一跳，挡不住「本文件既有分档对新涉钱动作漏入」。不得因 PGM-029 不在映射就把 002 降回观察。

最小处置与本席 GR-002 同向、最懒：`isMoneyAction` 纳入 `staff.payroll.penalty_cap.adjust`（或 `staff.payroll.*`）；若保留 DB 参数，同批插入 `supervisor_review_policies` 行并设 `deadlineWorkingDays.value=1`。排班发布保持 3 日。禁止：引用 PGM-029 当本段支撑；把 1 日写成新的 HTML 规则去改三份业务文档。

#### 005 `required=false` 可选事件

本轮源码：副本 `:39-40` `@Autowired(required = false) ApplicationEventPublisher eventPublisher`；构造函数仍四参（`:69-79`），不接收发布器；整单终态 `:292` `if (eventPublisher != null) eventPublisher.publishEvent(result)`。U05 `StaffApprovalService.afterDecision` `:69-82`：只在 `approved` 且动作码匹配时 `applyPenaltyCapRevision` / `publishShiftRevision`。`consumeApprovedRelease` 在 `:838`，员工路径 0 调用。

HTML：BSM-086 L1443「排班表最终发布须经该主管的上一级审批后生效」；BSM-087 L1458 上限「调整须经……最终确认」。生效/确认后落地是声称，不是可选项。

席甲 OBS-1「Spring 上下文中恒非 null」对全应用启动为真（`ApplicationEventPublisher` 是容器内建 bean）。这 **不能** 把 005 降观察：

1. 只合本文件、不合 U05：发布器即便非 null，也没有监听器，复核单可 `approved`，排班表仍未写入、上限仍 2000。这是首轮「只合本段」的真实形态，001 的同 PR 约束解决监听器同批，**不能替代** 005 的 fail-open。
2. `required=false` + null 跳过 = GEN-009 伪成功（批准返回成功、业务不落地、无错误）。四参构造（U12 `:41-46`）永远走这条。
3. BSM-086/087 要的是审批后生效，不是「Spring 通常会注入所以静默跳过也可以」。

四分类：**技术性**。不改「须审批才生效」。工程缺陷是。发布前须修 **是**。

最小处置与本席 GR-005 同向、最懒：与 U05 同 PR；构造函数必填注入发布器（删 `required=false`），或批准成功路径显式调用员工落地（仍同事务）。禁止只合本文件。001 已写同 PR，005 仍独立：001 是叠法/目录测试/整树回退，005 是落地路径 fail-open。

票 **PASS**（002 须修是、005 须修是；席甲 B1 PASS 低估，候选勘误成立）。

### 重点 (b) U09-003 `hr_review` 归 U10、V407 不插策略表 — **PASS（成立；处置只引用不发明第四条线）**

独立核：

1. 副本策略 `:53-55` 固定 `List.of("hr_review", "finance_review", "legal_review")`。BSM-087 只要「人力、财务、法务」三审，不是税费那条 `tax_review`。不是第四条线。
2. main / 副本 V391 只种子 `finance_review` / `tax_review` / `legal_review`（两文件 SHA 全等 `1a0090a3…`）。main 无 `hr_review`。
3. U10 `V407__staff_penalty_cap_approval_history.sql:13-14`：`INSERT INTO approval_lines … ('hr_review','人力审核线',…)`。全文无 `supervisor_review_policies`（本轮 `rg` 0 命中）。
4. main `ApprovalChainService` 查线失败抛 `APPROVAL_LINE_NOT_CONFIGURED`。只合本文件、不合 V407：`evaluate` 生成平行线即失败，BSM-087 做不到。
5. main 已有 `V410__m010_business_entity_topology_and_role_isolation.sql`，V407 编号须重排 ≥V411（U10 段，U05-008 同根已定稿）。

四分类：**技术性（跨段同批）**。无业务发明：不增加「税务审核」作为处罚上限第四条线；不把 `tax_review` 塞进本策略。

最小处置：与 U10 同 PR，`hr_review` 种子随重编号 ≥V411。可选把两条新策略写入 `supervisor_review_policies` 使时限可配——这是 002 的 DB 参数备选，不是第三条处置、也不是第四条审批线。禁止：在本段发明 `tax_review`/`hr_review` 以外的线码；把种子写进本段 Java。

票 **PASS**。

### 重点 (c) U09-004 测试缺口登记在本段、U12/U11 补用例 — **PASS（跨段登记合规）**

缺陷本体：本文件新增闸 `:214-219` 与事件 `:292` 声称可执行，交付测试未执行这些分支。GEN-003 在本段 106 项内（「测试须实际执行目标行为、具备能失败的断言……测试通过本身不替代行为证据」）。被测面在本段，测试文件按 modules.md 分在第 12 段（同服务单测）与第 11 段（员工回归）。跨段登记「本段记缺口、同 PR 由 U12/U11 补灯」与已定稿 U05-002（本段改基数、同批改 U11 期望值）同一口径，不是把 U12 段的审计结论提前定稿。

独立核：

- `git diff HEAD e28dad05 -- SupervisorReviewServiceTests.java`：+4/−2，只改版本头与 `listPolicies` `isEqualTo(10)`→`12` 并 `contains` 两码。无 `DEPARTMENT_REVIEWS_PENDING`、无 `publishEvent`、无 `staff.shift.publish` 的 evaluate/approve 用例。四参构造不变。
- 席甲 overlay 41/41 与本席 B1「叠未发布测试 41/0」同值，绿灯不经过新分支。席甲 U09 B1 GEN-003 写「第 12 段单元测试覆盖策略/闸」与 diff 矛盾，属低估；候选「席甲 U12 体 OBS-1」实为席甲 B2 核实该 diff，不是 U09 B1 已有 OBS。不改 004 票。
- U11 `M010RegressionTests:230-256` `shiftIsNotPublishedUntilSupervisorsSuperiorApproves`、`:267-304` `penaltyCapNeedsThreeDepartmentReviewsThenMasterAndDoesNotRewriteClosedMonth` 会走到缺上级/master 先批，但一律 `isInstanceOf(ApiException.class)`，不断言 `DEPARTMENT_REVIEWS_PENDING` / `APPROVAL_LINE_FINAL_APPROVER_REQUIRED`。MASTER 身份失败也会绿。

四分类：**技术性**。要求测试执行已声称行为，不新增业务规则。发布前须修 **是**（否则闸被删或事件被去掉后 U12 仍绿）。

最小处置：U12 补可失败用例（缺 `hr_review` 最终审批人；无上级时 master 先批 → `DEPARTMENT_REVIEWS_PENDING`；排班 `one_level_up` 链；注入 `ApplicationEventPublisher` 断言发布）。U11 补错误码断言。同 PR。禁止：把 004 改成「等 U12 段再登记」而本段 GEN-003 放空；把 U11 已有路径当成错误码已覆盖。

票 **PASS**。

### 重点 (d) U09-006 观察是否写错本席 §8 — **PASS（第 2/13/15 条未写错；「含」为抽样）**

006 列席甲 OBS-2/3/5，并以「Grok §8 1–15（2 税费 master 先批主线为基线既有、顺手收窄不单开；3 `isAlwaysBlocked` 未列新动作但 minAmount=0 恒进复核；4 `before_after` 证据类型未强制；5 员工单沿用 admission 事件名；9 `listPolicies` 权限在控制器；10 顶班已定稿 U06-004；13 PGM-029 不在映射；15 本机 javac 工具链）」收本席观察。「含」是抽样不是闭集。

第 2 条：本席原文「税费 `finance.tax_rule.adjust` 在无上级时仍允许 master 先把主线节点标 approved。……HTML PGM-041『再由老板最终确认』的顺序闸基线就没有。本段只给处罚上限加闸，未加重税费。发布者可把闸改成『`parallelLineCodes` 非空且当前节点 `KIND_MASTER`』，属顺手收窄，不单开 finding。」006 写「税费 master 先批主线为基线既有、顺手收窄不单开」。未改写成「本段必须给税费补闸」。U01「main 既有」＝HEAD 同位置已有且本材料 diff 未触及该缺口；税费无顺序闸在基线/HEAD 同文件已存在，本段 +/- 只给处罚上限加闸，未加重。归 M040/M087 观察桶，须修否。成立。

第 13 条：本席原文「PGM-029 不在映射，1 个工作日的 HTML 全文不能当支撑 ID；GR-U09-002 只用本文件已有分类 + BSM-001/087。」006 写「13 PGM-029 不在映射」。未把 PGM-029 拉进 002 支撑。本轮 python：`PGM-029 in M040` = False。成立。

第 15 条：本席原文「默认 javac 不在 PATH 的 `java-21-openjdk`（无 javac）会报 `release version 21 not supported`；须 `JAVA_HOME=…temurin-21…`。属本机工具链，不是业务 finding。」006 写「15 本机 javac 工具链」。未立项。成立。

未抽样写出、定稿不得当成「Grok 无这两条」或升格入队：

| §8 | 去向 |
|---|---|
| 1 main blob＝基线 | 已并 001 |
| 6 U11 不断言错误码 | 已并 004 |
| 7 提交对象不可解析 | 对象核验，观察即可 |
| 8 无 V0.08 | 版本头跳号，观察即可 |
| 11 `consumeApprovedRelease` 不被员工路径使用 | 已并 005 |
| 12 `ponytail:` 字段注入 vs 构造 | 已并 005 |
| 14 未跑 U11 JDBC、不采信席甲 41/41 为己证 | 证据边界，观察即可 |

席甲 OBS-3（监听方抛错整单回滚 fail-closed；HTML 沉默）维持观察：属安全侧，不是缺陷。OBS-5（`requireDefaultLines` `:163-170` 只校验最终审批人非空，不校验线资格；税费同形）按 U01 口径 main 既有，须修否，归 M040。OBS-1/4 已升队，006 未重复降回观察。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见差集。本文件 main＝基线，净发布物＝本段 +14/−1。只合本文件则 main M042 `isEqualTo(10)` 红（本席 B1 实跑 41 run/1 fail）。整树覆盖回退其它段 M009–M012（U08 已证实）。同根 U05-008。最小处置：以 main `445a49f4` 为底重放本文件 hunk；U09+U05+U10+U11+U12 同一 PR；迁移 ≥V411；禁止整树覆盖。无业务发明。票 **PASS**。

### 002 独立裁定（技术性时限分档／须修是）

见 (a)。票 **PASS**。

### 003 独立裁定（技术性跨段同批／须修是；不发明第四条线）

见 (b)。票 **PASS**。

### 004 独立裁定（技术性测试缺口／须修是；跨段登记合规）

见 (c)。票 **PASS**。

### 005 独立裁定（技术性可选事件／须修是）

见 (a)。票 **PASS**。

### 006 独立裁定（登记／须修否）

见 (d)。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. 002 定稿支撑链写清：BSM-087 定身份、BSM-001 定「按涉及钱处理」、1 日来自本文件既有 `isMoneyAction`，不是 BSM-087 原文数字，也不是 PGM-029。
2. 004 来源「席甲 U12 体 OBS-1」改为「席甲 B2 核实 U12 `:55-63`」；席甲 U09 B1 GEN-003「覆盖策略/闸」是低估，不是已有 OBS。
3. 006 抽样未列第 1/6/7/8/11/12/14 条；1/6/11/12 已并入 001/004/005，定稿不必逐条抄全，不得写成「Grok 无这些条」。
4. ledger 开工节「+15/−1」与 git +14/−1 不符；本票不改 ledger。
5. BSM-001 锁出处行是 `:505`（h2），涉钱句在现行正本 L521；本席 B1 写 `:521` 是句子行，不改票。

逐 ID：001 PASS，002 PASS，003 PASS，004 PASS，005 PASS，006 PASS。发布前须修是=001/002/003/004/005，否=006。添加 0、questions 0、无 HTML 修改申请、无 LOCK-AMEND。合取尚待席甲本批 B2；本席票单独不能定稿。材料未发布，定稿后 U09 → REVIEWED-FAIL，处置写给发布者，不做 B3/B4/B5。共同处置：U09+U05+U10+U11+U12 同一 PR，以 main 为底重放，迁移 ≥V411；`isMoneyAction` 纳入处罚上限；构造函数必填事件发布器；禁止整树覆盖。

本批最终：**PASS**
