CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M043 B5-r2 修复后完整模块独立审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r2
席位：固定 Grok 只读独立审计席乙 grok-4.6/xhigh
计划：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 §4/B5；`docs/audit-2026-09/plan/runbook.md` V0.02；`docs/audit-2026-09/plan/criteria-lock.md` V0.14
本文件路径：`docs/audit-2026-09/modules/M043-b5-r2-grok-raw.md`
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

审计结论：**PASS**

本席把 `plan/modules.md` 冻结的三文件从头读到末行，独立实算 106 条映射并逐条给出适用证据或不适用理由，三线（第一性原理 / 顺从式 / 对抗式）均跑完后再判定。不以 B4-r2 双 PASS、不以 B3 累计 2、不以「根 40 tests / typecheck 0」口头口径、不以席甲结论代替本次整模块重审。未把条件或待补写成 PASS。未发现首次正常就会撞上的工程缺陷。

未读取：任何 `M043-b5-r2-self-raw.md`（该路径当时已存在，本席未打开）。未用 MCP、未派子代理、未启用第三席、未提问、未改源码/测试/HTML/锁/ledger/findings、未 fetch/commit/push/merge/deploy、无数据库写入、未改主 index。唯一写入即本文件。`M043-b5-r1-grok-raw.md` 原失败证据未覆盖。

---

## 0. 锁、范围、技能与输入

本席独立命令（未抄 sidecar / ledger / 派发期望值）：

```bash
sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 hashlib 对同一文件再算一次
```

两者均为 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，命令 exit 0。旁路 `plan/criteria-lock.sha256` 同行同值；`ledger.md` 顶部同行同值、`LOCK-AMEND-COUNT: 1`。本席不把四值校验写成自己的收口动作（那是调度闸 1），只报本席实算。

| 项 | 值 |
|---|---|
| 正本 | `/home/james/complex-management`，分支 `main` |
| 主 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（`git rev-parse HEAD`，与派发给定一致） |
| origin/main 实查 | `27ad0e5fc25eb02e33cfe4743fb50072c2488593`（`git rev-parse origin/main`；与主 HEAD **不等**，origin 停在 `#481` merge；本席审的是主工作区当前三文件，不是 origin 树） |
| criteria-lock 版本 | V0.14；判据总数 401；映射 `M043 → [106 个 ID]`，本席数过 106 且无重复 |
| 计划冻结行数 | 660（`plan/modules.md` M043，不改） |
| 当前三源合计 | **1029** 行（`git ls-files -z -- <3 paths> \| xargs -0 cat \| wc -l`） |
| 分文件行数 | Page 486；test 465；chain.test 78 |
| 文件版本头 | Page `V0.02`；test `V0.04`；chain.test `V0.01` |
| B3 累计 | 2（派发给定；本席不据此降标准） |
| 修复队列上下文 | 派发称 001—011 经两轮 B4 双 PASS 已清；本席 **不把该清队列当成本次全量证据** |
| 三份业务 HTML | 只读，未改 |
| 主 index | M043 三文件 index blob 仍等于 HEAD blob；工作区相对 HEAD 有未暂存 diff（`3 files, +223/-59`）。本席未 `git add` / 未写 tree。审计对象是 **工作区现文**（修复后） |
| 浏览器 / 全后端 | **未执行**，不得声称 |

三源独立核 SHA（工作区文件；`sha256sum` 与 `git hash-object`）：

| 文件 | 行 | 版本 | git blob（工作区） | sha256 |
|---|---:|---|---|---|
| `apps/web/src/admin/pages/SupervisorReviewPage.tsx` | 486 | V0.02 | `853ec439d3d7bb2f3766833e4bb0438f510423ae` | `2f7526d005caf7901d64d3f3a28c27fbf6fe39fb83fbef41c68e55629f5bb46f` |
| `apps/web/src/admin/pages/SupervisorReviewPage.test.tsx` | 465 | V0.04 | `6f2cd57355a160e9ae3e7162ab5d4a68a9975f6c` | `d9ca54c9f8e8937fd1e7b8f35ab655cf367e258e7f2dc039bddbbaf62e63ab75` |
| `apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx` | 78 | V0.01 | `8314976f7db2e1746fc03bef4678a693a8e026d9` | `543e69399eab5f89d9812966cbf791d3a931c45fb21976b3c5b4281671a08862` |

HEAD blob（仅对照，不是本次审计对象）：Page `cedd05d222c258fca12274f3851adcc64a2f7191`；test `1bd056113cf5a61a5f65cd433163619933db821f`；chain `7d30eb3297999145cb5f01a2020d0cc82c7e8a84`。工作区 ≠ HEAD，符合「B3 第 2 次已落工作区、尚未进主 index」。

全文覆盖：上表三文件均从头读到末行。依赖只读追流（不扩修复面、不把依赖文件列入本模块修复）：`adminClient.ts` `mapKeys`/`unwrap`/`normalizeRequestBody`；`basePath.ts` `withBase`/`isDeployed`；`useAdminAction.ts`；`useAdminResource.ts`；`PermissionGate.tsx` `hasPermissions`；`Can.tsx` `PermissionButton`；`HighRiskConfirm.tsx`；`RowDetailDrawer.tsx` `cnDate`；`customer/i18n/statusLabels.ts`；`shared/apiError.ts`；`backend/.../review/SupervisorReviewController.java`、`SupervisorReviewService.java`（evaluate/approve/forward/requireDecisionEvidence/requiresBeneficiary/policies 目录）、`SupervisorReviewEvaluateRequest.java`、`SupervisorReviewApprovalRequest.java`、`SupervisorReviewForwardRequest.java`、`SupervisorReviewPolicyRecord.java`。未跑 Maven、未开浏览器、未扩到审批链页或其它模块源码修复。

### 技能自检（V0.17 §6 收窄；存在≠冒充调用）

| 技能 | 本环境 | 本节点用法 |
|---|---|---|
| `grilling` / `grill-me` | 已读 `.../mattpocock-skills/1.2.3/.../grilling/SKILL.md` | 只借追问结构，事实自读，不等操作员、不派子代理 |
| `batch-grill-me` | **缺口：无独立技能文件** | 未冒充调用 |
| `verification-before-completion` | 有 | 声称前先跑 vitest / tsc 并记录真实 exit |
| MCP / 任何子代理 / Arc audit 派工 | 禁用 | 未用 |

---

## 1. 三文件覆盖记录

| 文件 | 覆盖行 | 为什么必须存在（删掉则 HTML 哪条做不到） | 本席阅读结论 |
|---|---|---|---|
| `SupervisorReviewPage.tsx` | 1–486 全文 | PGM-014 卡片「敏感操作进复核队列」；BSM-028「不符合合同或基础定义的后续流程必须先被卡住，再按审批链逐级审批放行」；BSM-120「免单、退款、调账按可配置层级主管复核」「合同或基础定义违规全部先卡住」；PGM-041「只指定最终审批人、系统自动补全」「申请人与直接受益人不得审批」；PGM-040「一个单一功能一个权限点」 | 策略 KPI/明细、待复核清单、转交、评估、当前节点批准/驳回、链入口、身份闸、证据附件按 **policyCode**、中文结果句均在；评估动作面是本页发起的违规/免单/退款/调账/人工放行，不是全目录发起器 |
| `SupervisorReviewPage.test.tsx` | 1–465 全文 | GEN-003 关键分支须能失败 | 36 例：权限点、驳回 decision、转交/自审/受益人、KPI、抽屉、真实 12 条目录中文、policyCode 附件、节点状态句、服务端拒绝不伪成功 |
| `SupervisorReviewPage.chain.test.tsx` | 1–78 全文 | PGM-041/BSM-028 最终审批人可空；部署前缀下链入口 | 4 例：`usr_gm`/空最终人字段；`""` 与 `"/complex"` 前缀 href |

---

## 2. 可执行验证（2.3 类型 2/4）

工作目录：`/home/james/complex-management/apps/web`。

| 命令 | 真实 exit | 关键输出 |
|---|---|---|
| `npx vitest run src/admin/pages/SupervisorReviewPage.test.tsx src/admin/pages/SupervisorReviewPage.chain.test.tsx` | **0** | `Test Files 2 passed`；`Tests 40 passed (40)`；chain 4 / page 36；Duration ~1.20s。jsdom 打印 `Window's scrollTo() is not implemented`（KPI 滚动，见观察） |
| `npx tsc -b --noEmit` | **0** | 无诊断；stdout 仅 NO_COLOR 警告 |
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | **0** | 见第 0 节 |
| `git rev-parse HEAD` / `origin/main` | **0** | 见第 0 节 |
| 浏览器 E2E / Playwright | **未跑** | 不得声称页面在真实浏览器中的点击/焦点 |
| 后端 Maven / 活库 / 全链 HTTP | **未跑** | 后端只作只读实现对照，不得声称服务端单测或接口实打 |

测试绿 **不等于** 行为正确；下列顺从/对抗以源码 + HTML 原文 + 上述命令输出为据。40 例均有能失败断言（请求体字段、中文句、href、错误文案、未发请求），不是空 `toBeTruthy()` 堆砌。

---

## 3. 第一性原理线（2.3 三问）

### 3.1 这段东西为什么必须存在？

删掉本页则：

- PGM-014 原文「重要动作可追溯：谁发起、谁审批、谁复核、谁放行，敏感操作进复核队列」在 Web 后台没有对应队列与放行入口。
- BSM-028 原文「必须先被卡住，再按审批链逐级审批放行……发起单据时只需指定该事项的最终审批人」在本 Web 段没有评估入口与「最终审批人可空」。
- BSM-120 原文「免单、退款、调账按可配置层级主管复核」「合同或基础定义违规全部先卡住流程，允许更高级审核后放行」没有本页评估选项与待审清单。
- PGM-041 原文「审批人可将该单据的审批转发给指定人选……不得转发给申请人本人……也不得转发给直接受益人」「申请人与直接受益人不得审批与自己相关的申请」没有本页转交/自审闸。
- PGM-040 原文「一个单一功能就是一个权限点」：评估 `governance.supervisor_review.manage` 与审批/转交/驳回 `governance.supervisor_review.approve` 应对后端 `SupervisorReviewController` 的 `requirePermission`。

答得上 → 有业务上位。KPI 聚焦、行抽屉、中文码表是 BSM-003/GEN-015 的下位细化，不是新业务发明。

### 3.2 声称的行为有没有被实际执行验证？

| 声称 | 证据 |
|---|---|
| 空最终审批人不带 `final_approver_id` | chain.test `it.each(["usr_gm", ""])`；vitest exit 0 |
| `/complex` 下审批链 href 保留前缀 | chain.test `it.each(["", "/complex"])`；`withBase` 读 `pathname.startsWith("/complex")`；vitest exit 0 |
| 真实 12 条策略目录用 `policyCode` 作行 key/testid、动作/证据中文、未知码不泄漏 | test「真实目录、待审枚举及抽屉均显示中文」；对照后端 `policies` 12 条 `policyCode` 一致；vitest exit 0 |
| 批准按返回 `review_status` 区分「当前节点」与「全部节点」 | test `it.each` pending_review/approved；`makeResult` :83–97 |
| 附件按 **policyCode** 而非动作名 | test `bill_waiver_l3` 要附件，`contract_violation_l3`/`refund_l2` 不要 |
| 转交可空受益人（合同违规行）仍可提交 | test review_p2 |
| 免单/退款/调账评估先要受益人 | test `it.each` 三动作 |
| 服务端拒绝不报告成功 | test approve/forward 400 → `setActionResult({status:"failed",...})` 且表单错误含「直接受益人未登记」 |
| 管理权不能审批 | MANAGE_ONLY_PERMS 下 approve/reject `disabled` |
| 构建 | tsc exit 0 |

未验证：真实浏览器焦点/滚动；活后端组织层级补链；USB 主会话节点。

### 3.3 哪个前提未经验证就当真？

1. 活后端 `listPolicies` 永远返回与 Java 写死 12 条相同的目录 —— 本席只对照了 `SupervisorReviewService.java:51–67` 源码，未打 HTTP。
2. jsdom `history.replaceState` 会更新 `location.pathname` 从而让 `withBase` 看见 `/complex` —— 被 chain 4 例绿证明 **在本测试环境成立**；真实门户 rewrite 未跑。
3. 共享 `cnDate` 把 ISO `Z` 时分当字面展示 —— 见观察 OBS-02，不升缺陷。
4. 测试夹具 `fetch` 不是生产 Cookie/CSRF 管线 —— 只证明本页请求构造。

---

## 4. 顺从式：106 映射逐条

判定口径：本模块只有三文件网页+测试。宽映射里大量锚点业务不在本页实现；**不适用 ≠ 漏审**。不适用理由必须是「本页无该业务入口/无该枚举写入」，并确认无明文冲突、无本页发明该锚点规则。能指到 HTML 上位的实现走细化；技术夹具走技术性。

### 4.1 GEN-001 … GEN-018

| ID | 适用 | 证据或理由 | 分类 |
|---|---|---|---|
| GEN-001 | 是 | 三文件均承载主管复核队列/评估/裁决；无纯转发空壳 | 细化 |
| GEN-002 | 是 | vitest 40/40 exit 0；tsc exit 0；主路径与错误路径有断言 | 细化 |
| GEN-003 | 是 | 断言打在 fetch body、中文句、href、未发请求；不是「有测试即过」 | 细化 |
| GEN-004 | 是 | 评估/转交/裁决前校验空说明、受益人、自审、policyCode 附件；最终信任在服务端（对照 Controller `requirePermission` + Service 闸） | 细化 |
| GEN-005 | 是 | 评估 manage、审批/转交/驳回 approve；`PermissionButton`/`HighRiskConfirm` 无权限拦截 onClick；后端同码。界面禁用不是唯一闸 | 细化 |
| GEN-006 | 否（本页无持久化） | 状态机在后端事务/`lockSupervisorReview`；本页只 POST。不把后端完整性记成本页已证明 | 不适用 |
| GEN-007 | 是 | 每写动作 `nextKey` + `adminClient` 保证 `idempotency_key`；`busyAction` 禁重复点。幂等去重在服务端 `idempotencyService`（只对照，未跑） | 细化 |
| GEN-008 | 是 | 请求经 `toSnakeCaseKey`；响应 `toCamelCaseKey`；测试断言 `final_approver_id`/`forwarded_to`/`evidence_attachment_ids`/`decision`。前端 `SupervisorReviewPolicyRow.id` 后端策略记录无 `id` 字段，运行靠 `policyCode`（见观察 OBS-03） | 细化 |
| GEN-009 | 是 | 400 走 `useAdminAction` catch → 表单错误 + `status:"failed"`，测试锁住不伪成功 | 细化 |
| GEN-010 | 是 | 无密钥/注入点；用户编号输入不拼进 SQL；错误用中文业务句。链 href `encodeURIComponent(reviewId)` | 细化 |
| GEN-011 | 是 | `AuditTrailHint` 提示复核读写；真正 `recordSuccess` 在后端。Hint 文案与事件名不完全同字（观察 OBS-04） | 细化 |
| GEN-012 | 是 | 列表来自分页信封 unwrap；pending limit 在后端 200。本页无无界循环 | 细化 |
| GEN-013 | 部分 | 三文件无独立构建脚本；tsc 本包 exit 0 证明可纳入现有 tsconfig | 技术性 |
| GEN-014 | 否 | 无迁移文件 | 不适用 |
| GEN-015 | 是 | 标签/选项/抽屉/错误中文；未知枚举「未识别类型」；转交/最终人/受益人标明「用户编号不是登录名」；行键盘 Enter/Space | 细化 |
| GEN-016 | 否 | 三文件不是需求文档；页内 description 与 HTML 卡住/放行/留痕一致，无把计划写成已完成的路径谎言 | 不适用（无独立文档产物） |
| GEN-017 | 是 | 本报告按冲突→细化→技术性→添加；无锁外新判据 | 细化 |
| GEN-018 | 是 | 结论落到 HTML 原文、vitest/tsc 输出、后端源码对照、可复现命令 | 细化 |

### 4.2 BEQR

| ID | 适用 | 证据或理由 | 分类 |
|---|---|---|---|
| BEQR-063 | 部分 | 原文高危含反结/沽清/免单/账单调整/存酒作废。本页评估有免单，无反结/沽清/存酒作废 **发起**。待审清单可展示其它模块已生成的单。不把本页发明成酒吧沽清台 | 细化（免单）/不适用（未在本页发起的其余动作） |
| BEQR-074 | 否 | 隐私桌台三权不在本页 | 不适用 |
| BEQR-143 | 否 | 四类主体不在本页录入 | 不适用 |
| BEQR-144 | 否 | 经营主体关系不在本页 | 不适用 |
| BEQR-145 | 否 | 城市项目公司职责不在本页 | 不适用 |
| BEQR-146 | 否 | 运营公司职责不在本页 | 不适用 |
| BEQR-147 | 否 | 收入归属不在本页 | 不适用 |
| BEQR-148 | 否 | 合作模式不在本页 | 不适用 |
| BEQR-149 | 否 | 职级 M1–M10 不在本页配置；`requiredLevel` 是复核档位数字展示，不发明 30 档 | 不适用 |
| BEQR-150 | 否 | 十一端开关不在本页 | 不适用 |
| BEQR-151 | 部分 | 本页不计算最终权限；只消费 `effectivePermissions`（拒绝优先/wildcard 在 PermissionGate）。不与「角色+模板+临时−拒绝」公式冲突 | 细化（消费） |
| BEQR-152 | 否 | 不展示内部结算价等敏感经营数据 | 不适用 |
| BEQR-153 | 否 | 终端开关优先级不在本页 | 不适用 |
| BEQR-154 | 否 | 综合体项目状态不在本页 | 不适用 |
| BEQR-155 | 否 | 商户状态不在本页 | 不适用 |
| BEQR-156 | 否 | 合同状态机不在本页 | 不适用 |
| BEQR-157 | 否 | 员工状态不在本页 | 不适用 |
| BEQR-158 | 否 | 设备状态不在本页 | 不适用 |
| BEQR-159 | 否 | 「权限状态」草稿/已启用/暂停/已停用/归档是权限对象枚举。本页策略 `status` 用通用码表 `active→有效`、`disabled→已禁用`，不是该条权限状态机，亦不改枚举 | 不适用 |
| BEQR-166 | 部分 | 原文是合同三类模板+平行线。本页复用「指定主线最终人、可空、驳回退起草人」，**不**做合同三类模板或法务/财务平行线表单。税费平行线由后端 `tax_rule_adjustment_l3` 在其它入口 fail-closed，本页评估下拉无该动作 | 细化（通用模型）/不适用（合同模板本身） |
| BEQR-182 | 否 | 考勤日结果不在本页 | 不适用 |
| BEQR-183 | 否 | 打卡方式不在本页 | 不适用 |
| BEQR-184 | 否 | 请假类型不在本页 | 不适用 |
| BEQR-185 | 否 | 请假单状态不在本页 | 不适用 |
| BEQR-186 | 否 | 薪资单构成不在本页 | 不适用 |
| BEQR-187 | 否 | 提现发放不在本页 | 不适用 |
| BEQR-189 | 否 | 管理费方式不在本页 | 不适用 |

### 4.3 BSM

| ID | 适用 | 证据或理由 | 分类 |
|---|---|---|---|
| BSM-001 | 部分 | 整体说明「谁能操作、谁负责」：本页是治理复核入口之一，不实现全盘子 | 细化 |
| BSM-003 | 是 | 权限分岗：view 看目录、manage 评估、approve 裁决/转交 | 细化 |
| BSM-004 | 否 | 配置存档/删除审批不在本页 | 不适用 |
| BSM-005 | 否 | 本页不是分步新建向导 | 不适用 |
| BSM-006 | 否 | 无动态二维码 | 不适用 |
| BSM-007 | 部分 | 测试 wildcard 模拟平台管理员日常权；高危审批仍要 `approve` 点，与 PGM-041「高敏不因平台管理员自动获得」在 **本页按钮** 上不冲突（wildcard 语义在共享 PermissionGate，不扩面） | 细化 |
| BSM-009 | 部分 | 「基础管理」父锚点；本页不是建项目/商户 | 不适用（父节指引） |
| BSM-028 | 是 | 原文 L707 卡住+指定最终人+自动补全+七类红线。本页：违规评估默认阻断、最终人可空交给后端补链、不提供红线关闭开关、不把审批当万能通行证 | 细化 |
| BSM-029 | 部分 | 「和谁有关」票务/酒吧空间 — 本页只连治理复核，不改空间模型 | 不适用 |
| BSM-072 | 否 | 商户侧系统不在本页 | 不适用 |
| BSM-073 | 否 | 商户主要功能不在本页 | 不适用 |
| BSM-074 | 否 | 商户相关方不在本页 | 不适用 |
| BSM-084 | 否 | 员工手机端不在本页 | 不适用 |
| BSM-085 | 否 | 日常工作手机端不在本页 | 不适用 |
| BSM-086 | 否 | 排班考勤请假不在本页发起（目录里有排班策略行，只展示/待审，评估下拉无 `staff.shift.publish`） | 不适用 |
| BSM-087 | 否 | 我的薪资不在本页 | 不适用 |
| BSM-088 | 否 | 提成/人员服务边界不在本页 | 不适用 |
| BSM-114 | 部分 | 「不同人只看自己该看的」：路由 `governance.supervisor_review.view`，写按钮分权。不实现异常预警页 | 细化 |
| BSM-115 | 部分 | 相互关系/治理连接财务审批：本页是复核连接点，不实现日结 | 细化 |
| BSM-120 | 是 | 已拍板「免单、退款、调账按可配置层级主管复核」「合同或基础定义违规全部先卡住」。评估选项含这五类；清单按策略档位展示 `requiredLevel` | 细化 |

### 4.4 PGM

| ID | 适用 | 证据或理由 | 分类 |
|---|---|---|---|
| PGM-001 | 部分 | 治理底座定位；本页是底座上的复核页 | 细化 |
| PGM-002 | 是 | 先判断能不能做：评估阻断生成待审 | 细化 |
| PGM-003 | 是 | 模块入口清单含合同准入/权限/留痕；本页落实留痕复核操作面 | 细化 |
| PGM-004 | 否 | 综合体/集团卡片，无本页字段 | 不适用 |
| PGM-005 | 否 | 空间点位卡片 | 不适用 |
| PGM-006 | 否 | 商户主体卡片 | 不适用 |
| PGM-007 | 是 | 合同和准入卡片「后续业务能不能走」：违规评估+放行 | 细化 |
| PGM-008 | 否 | 员工和手机卡片 | 不适用 |
| PGM-009 | 是 | 权限和职级卡片；本页两个权限点 | 细化 |
| PGM-010 | 否 | 终端开关卡片 | 不适用 |
| PGM-011 | 否 | 设备管理卡片 | 不适用 |
| PGM-012 | 否 | 商户侧卡片 | 不适用 |
| PGM-013 | 否 | 开放接口卡片 | 不适用 |
| PGM-014 | 是 | 「敏感操作进复核队列」：pending 表+评估+裁决+转交 | 细化 |
| PGM-015 | 否 | 楼宇运营卡片 | 不适用 |
| PGM-016 | 否 | 应急与隐私卡片 | 不适用 |
| PGM-017 | 否 | 综合体集团正文不在本页 | 不适用 |
| PGM-018 | 否 | 多对多协作不在本页 | 不适用 |
| PGM-019 | 否 | 各主体职责正文不在本页 | 不适用 |
| PGM-020 | 否 | 综合体主要功能不在本页 | 不适用 |
| PGM-021 | 否 | 综合体业务意义不在本页 | 不适用 |
| PGM-022 | 否 | 空间点位正文不在本页 | 不适用 |
| PGM-024 | 否 | 空间影响业务不在本页 | 不适用 |
| PGM-026 | 否 | 该 ID 父锚点是「商户和主体」主要功能，不是合同审批长文 | 不适用 |
| PGM-027 | 否 | 商户业务边界 | 不适用 |
| PGM-031 | 否 | 经销商主体/合同价格 | 不适用 |
| PGM-032 | 否 | 员工手机绑定 | 不适用 |
| PGM-033 | 否 | 员工主要功能 | 不适用 |
| PGM-034 | 否 | 企微会话 | 不适用 |
| PGM-035 | 否 | 提成发放 | 不适用 |
| PGM-036 | 否 | 设备台账 | 不适用 |
| PGM-040 | 是 | 单一功能权限点：manage vs approve 分开，测试锁住 | 细化 |
| PGM-041 | 是 | 敏感动作上级审批；临时授权/角色装配/高危五类 **授出工作流** 不在本页发起。本页落实：最终人可空、转交禁申请人/受益人、自审禁、驳回退起草人、链入口。税费三审平行线不在本页评估表（动作未列入下拉，避免无平行线字段却能发起） | 细化 |
| PGM-042 | 是 | 「职级不能替代功能权限点」：本页不按职级放开按钮，只看权限码 | 细化 |
| PGM-043 | 否 | 外包/审计人员账号 | 不适用 |
| PGM-044 | 否 | 隐私桌台/挂单/经销权限表 | 不适用 |
| PGM-048 | 否 | 商户侧全开放 | 不适用 |
| PGM-059 | 否 | 本页不展示客人/员工手机号明文 | 不适用 |
| PGM-060 | 否 | 会员/人脸 | 不适用 |
| PGM-065 | 部分 | 治理连接酒吧免单/账单调整、连接财务审批：本页是连接点而非各域实现 | 细化 |
| PGM-066 | 是 | 已拍板与 BSM-120 同口径的复核项；本页覆盖免单/退款/调账/违规卡住 | 细化 |

映射 106/106 已核对。无锁外新判据。无因宽映射新增「本页必须发起存酒作废/平行线税费三审/合同三类模板」的业务。

---

## 5. 对抗式（grilling 结构，事实自答）

❓ **Q1 空最终人会不会仍把空串送给后端？**  
➡️ `finalApproverId: finalApproverId.trim() || undefined`，`mapKeys` 丢 `undefined`。chain.test 空串分支 `not.toHaveProperty("final_approver_id")`，vitest 0。r1 失败项本次独立复证通过。

❓ **Q2 `/complex` 链会不会掉前缀？**  
➡️ `href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(...)}`)}`。chain.test 前缀 `""` 与 `"/complex"`。vitest 0。r1 失败项本次独立复证通过。

❓ **Q3 真实目录行 key 会不会用错成 action_type/id？**  
➡️ `key={row.policyCode}` 且 `data-testid=supervisor-review-row-${row.policyCode}`。测试按 `REAL_POLICIES[].policy_code` 取行。12 条与后端 `policies` 的 `policyCode` 对齐。vitest 0。r1 失败项本次独立复证通过。

❓ **Q4 驳回是不是走错 URL 或 decision？**  
➡️ 与批准同 POST `.../approve`，`decision: "rejected"`，对齐 Controller 单一 approve 入口。测试断言 body 与中文「整单退回起草人」。

❓ **Q5 转交给申请人/受益人（含登录名）能否发出？**  
➡️ `forwardReview` 比 `initiatedBy`/`beneficiaryId`，并用 `sameAccount` 把当前用户登录名当自己拦下。测试 p1/p4/p5/第三人 usr_benef。合同违规无受益人仍可转交。

❓ **Q6 无申请人的待审单会不会放行？**  
➡️ 转交/批准/驳回均 fail-closed「申请人未登记」。测试 p3。

❓ **Q7 附件闸是否看错成动作名？**  
➡️ `rows.find(policyCode === pending.decision.policyCode)` 再 `includes("evidence_attachment")`。`bar_bill_waiver_block` 对不上目录则交给服务端（测试「待审动作对不上目录」）。`bill_waiver_l3` 批准/驳回都要附件 —— 与后端 `requireDecisionEvidence` 对 **approved 与 rejected 同样调用** 同口径，不是本页独创。HTML 未把驳回豁免于策略证据。不升缺陷。

❓ **Q8 评估自己的单会不会被自己批准？**  
➡️ 评估成功后 `initiatedBy` 后端是当前 actor；本页 `validateDecisionActor` 在 pending 找不到时回退 `user.id`，仍拦自审。从清单选他人的单才能批。HTML「申请人不得审批与自己相关」。

❓ **Q9 KPI 聚焦会不会把待审队列藏起来导致无法放行？**  
➡️ 会隐藏表单与清单，提供「清除筛选，返回全部」。属样板聚焦，不是无出口。观察 OBS-01。

❓ **Q10 评估下拉少于目录 12 条是否漏需求？**  
➡️ 有平行线的 `staff_penalty_cap_adjust` / `tax_rule_adjustment_l3` 若在本页无平行线字段却可发起，后端会 400。本页 **不把它们放进评估下拉** 是正确收窄，不是漏做税费三审（不扩面到发明平行线表单）。排班/授权类由其它入口 `blockForAdmission`。清单仍展示目录全量。

❓ **Q11 服务端失败会不会仍 `makeResult` 成功句？**  
➡️ `useAdminAction` catch 只写 `{status:"failed", message}`。测试锁住。

❓ **Q12 权限只有 manage 时能否点批准/驳回/转交？**  
➡️ 按钮 disabled + onClick fail-safe。测试 approve/reject。转交同样 `SUPERVISOR_APPROVE_PERMISSION`。

对抗结论：r1 三项在现文+40 例下闭合。未构造出首次正常就会失败且属于本三文件的新缺陷。

---

## 6. 原 r1 失败项独立复证（不引用 r1 结论当票）

派发点名的三项，本席只凭现文与本次 vitest：

1. **真实目录 key**：现文 `:370` `key={row.policyCode}`；测试 12 条 `policy_code` 行。通过。
2. **`/complex` 链接**：现文 `:423` `withBase(...)`；chain.test 两前缀。通过。
3. **空最终人测试**：现文 `:244` `trim() || undefined`；chain.test 空串不带字段。通过。

禁止用「B4-r2 已 PASS」替代上述复证；上述是本次重跑。

---

## 7. 观察（非缺陷，不进修复队列）

观察 = 要叠加少见数据/未跑浏览器/共享件才显现，或 HTML 未要求本页承担。

| ID | 说明 | 为何不升缺陷 |
|---|---|---|
| OBS-01 | KPI 聚焦隐藏待审清单与表单 | 有「返回全部」；非无出口；BSM-003 样板细化 |
| OBS-02 | `cnDate` 把 `...T02:00:00Z` 的 02:00 当字面展示 | 实现在共享 `RowDetailDrawer.tsx`，不扩修复面；清单仍有升级次数；合同当地时区锚点未映射到本页清单 |
| OBS-03 | 前端类型有必填 `id`，后端策略记录无 `id` | 运行键是 `policyCode`；Java 12 条均有 code |
| OBS-04 | `AuditTrailHint` 事件字面与后端 `policies_viewed`/`node_decided` 不完全同字 | 提示不是审计日志本身；真正 record 在后端 |
| OBS-05 | jsdom 无 `scrollTo` | 未跑浏览器；不影响请求/校验 |
| OBS-06 | 策略空目录时 `AdminDataState empty` 会连待审一并藏起 | 后端写死 12 条，首次正常不空 |
| OBS-07 | 驳回也走策略 `evidence_attachment` | 与后端 `requireDecisionEvidence` 一致；HTML 留痕未豁免驳回 |

无「添加」项。无「冲突」项。测试夹具/testid：技术性，无业务发明。

---

## 8. 临时 findings

无。本席临时编号空集。无未答 questions。无锁外主张。

四分类汇总：冲突 0；细化（有工程缺陷）0；技术性缺陷 0；添加 0。观察 7 条不进队列。

---

## 9. 三线收口

- 第一性原理：三问已答；证据类型 1（HTML 原文）+ 2/4（vitest/tsc/sha256/git）+ 后端源码对照（非可执行产物，只证实现、不冒充已跑）。
- 顺从式：106/106 有适用证据或不适用理由。
- 对抗式：r1 三项闭合；自审/转交/附件/权限/伪成功/前缀/空最终人已构造反例并由测试或源码挡住。

---

## 10. 最终判定

**PASS**

条件：本模块当前工作区三文件（1029 行，版本 Page V0.02 / test V0.04 / chain V0.01）在冻结 106 判据下，未发现与三份 HTML 明文冲突，未发现首次正常路径上的工程缺陷，关键可执行声称已用 vitest 40/40 exit 0 与 tsc exit 0 验证。未跑浏览器与全后端，故不声称那些。不得用 B4 票或席甲稿替代本结论——本结论仅约束本席本次 B5-r2 raw。
