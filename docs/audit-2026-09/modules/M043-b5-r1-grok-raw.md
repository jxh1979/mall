CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M043 B5-r1 Grok 乙席完整审计报告

- 文件版本：V0.01（本路径原先不存在，本次新建）
- 席位：Grok 独立审计席乙（只读）
- 步骤：B5-r1（B3 累计 1 之后的修复后整模块全量审计）。调度称 B4 双 PASS 才派本命令；本席**不把 B4 票、001—008 清队或席甲结论当成本席事实**，按下整模块三线重审
- 计划：`docs/audit-2026-09/plan/modules.md` 中 M043；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M043 — 高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证／第1段
- 模块生命周期（只读上下文，本席不改台账）：`IN_PROGRESS`；B3 累计 1
- 判据锁：`criteria-lock.md` V0.14；本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`，结果只写在本报告第一行。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部基准同行值。正文不再重复该字段名
- 本席约束：只读业务代码与计划；不调用 MCP；不派子代理/第三席；不修源/测试/HTML/锁/ledger/findings/主 index；不 commit/push/merge/deploy/写库；原始 JSON 不落盘。未读本次 self B5
- 技能：已读并按提示词 §6 收窄口径使用 `using-superpowers`、`grilling`（只借追问结构，事实自读，不等操作员、不派子代理）、`verification-before-completion`、`diagnosing-bugs`、`codebase-design`、`ponytail-review`。未核到独立可执行的 `grill-me`/`batch-grill-me` CLI 与 `i-have-adhd` 技能文件，**不冒充已调用**。未用 worktree/子代理/research
- 需求源：仅三份 HTML。后端/shared/审批链页只作实现对照，不扩修复面、不把未映射锚点写成新判据

## 1. 范围与指纹（命令实算，不是计划抄写）

`plan/modules.md` FILES-BEGIN 冻结 3 文件、有效内容行数 660（A1 冻结口径，不改）。本席对当前工作区现文：

```bash
git ls-files -z -- \
  apps/web/src/admin/pages/SupervisorReviewPage.tsx \
  apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx \
  apps/web/src/admin/pages/SupervisorReviewPage.test.tsx \
  | xargs -0 cat | wc -l
# → 1018
sha256sum <三文件>
```

| 文件 | 版本头 | 行数 | 本席 SHA256 |
|---|---|---|---|
| `apps/web/src/admin/pages/SupervisorReviewPage.tsx` | V0.01 | 485 | `c7a08798740eb56aafe5d04b32d12dda6f26705900ec3ac4b189deca55ac4533` |
| `apps/web/src/admin/pages/SupervisorReviewPage.test.tsx` | V0.03 | 458 | `88169e9fbe829ee0f2665ae43224571c953c6916f6299b75fd96f88eca984f11` |
| `apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx` | 无版本头 | 75 | `9640cc82d5d14fbff14c340d07da1cfc28999eb04697e0d7fb845cec9840565d` |

三文件全部逐行实读，不抽样、不只看 8 个修复 ID、不只看 diff。

映射区 `M043 → [` 实拆 **106** 个 ID（与指令「全部 106 映射」一致，无缺定义）。锁内 HTML 摘录对三份 HTML 现文关键词核对 mismatch=0（106/106，含 18 条 GEN 无 HTML 摘录）。

Git（只读）：

- 主工作区 `HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`（与派发给定值一致）
- `origin/main` 本地跟踪 = `27ad0e5fc25eb02e33cfe4743fb50072c2488593`（`Merge pull request #481 ... m009-m011-publish`），**与 HEAD 不同**
- 本席未改 `.git/index`，未 fetch/push
- 三份 HTML 只读，未申请铁律 4

## 2. 可执行验证（类型 2/4）

命令：

```bash
cd /home/james/complex-management/apps/web && npx vitest run \
  src/admin/pages/SupervisorReviewPage.test.tsx \
  src/admin/pages/SupervisorReviewPage.chain.test.tsx
```

- **进程 exit code = 0**
- Vitest：Test Files 2 passed；Tests **38 passed / 38**（主文件 36 + chain 2）；Duration 1.11s
- **测试绿 ≠ 行为正确。** 同一次运行 stderr 出现：

```
stderr | src/admin/pages/SupervisorReviewPage.test.tsx > M043 真实审批信封与既有规则 > approve 按 policyCode 而非动作名要求免单附件
Each child in a list should have a unique "key" prop.
Check the render method of `SupervisorReviewPage`.
```

该用例夹具 `REAL_POLICIES` **没有 `id` 字段**，与后端策略记录形状一致；带 `id: "sp-1"` 的 `POLICIES` 夹具把缺陷藏掉。见 GR-01。

未跑浏览器、未跑 Maven/全后端/psql。后端 Java 只读对照实现，不声称后端单测已跑。

## 3. 第一性原理（模块三问 + 分文件）

### 3.1 为什么必须存在

删掉本页，三份 HTML 中「敏感操作进复核队列」「免单/退款/调账按层级主管复核」「合同或基础定义违规先卡住再按审批链放行」「谁发起、谁审批、谁放行」在 Web 后台将没有对应操作面（PGM-014 卡片原文；BSM-120 已拍板两条；BSM-028 卡住+最终审批人）。答得上 → 有业务上位。策略 KPI/明细、评估、待复核、转交、审批、驳回、链入口都是该上位的下位实现（2.1 细化），不是本席新发明的业务。

### 3.2 声称是否被实际执行

- 声称「评估 / 待复核 / 转交 / 审批 / 驳回 / 链入口 / 中文 / policyCode 附件 / 节点结果」：vitest 38 条对 **mock fetch** 路径有断言，exit 0。
- 声称「真实目录行可稳定点选」：同一次 vitest 在真实目录夹具下打出 React 重复 key（类型 2）。
- 声称「审批链入口可用」：测试只断言 `href="/admin/governance/approval-chain?review_id=..."`。`AdminApp` 换页走 `withBase`；本页 `<a>` 不走。生产 `basePath.ts` 在 pathname 以 `/complex` 开头时补前缀。本席**没有**打开浏览器点该链，故生产跳转未用浏览器实证，依据是源码 + `basePath.test.ts` 对 `withBase` 的现有单测口径（类型 1/4 对照，不是本席新开浏览器）。

### 3.3 未验证就当真的前提

- 后端 `listPolicies()` 的 `SupervisorReviewPolicyRecord` **没有 id**，前端类型却把 `id: string` 当必填。夹具补 id 会让人以为生产也有 id。
- 链测试在 jsdom 默认非 `/complex` 路径下把无前缀 href 写成期望，生产前缀被测不到。
- 测试绿被当成「真实策略表 / 生产链入口已好」。本席拒绝该前提。

### 3.4 分文件

**`SupervisorReviewPage.tsx`（485）**

- 存在理由：见 3.1。
- 行为：`GET` 策略与 pending；评估 `POST .../evaluate`（幂等键、可空 `finalApproverId`、三动作受益人客户端先拦）；转交 `POST .../forward`（申请人/受益人客户端先拦，空受益人放行到服务端）；审批/驳回同 `.../approve` + `decision`；`HighRiskConfirm` 二次确认；`manage` 与 `approve` 分权；KPI `?kpi=policies` 子集；行抽屉中文。
- 未验证前提：`key={row.id}`、链 `<a href="/admin/...">` 见 findings。

**`SupervisorReviewPage.test.tsx`（458）**

- 存在理由：锁 GEN-003；覆盖评估权限、驳回、转交、自审、KPI、抽屉、policyCode 附件、节点文案、三动作受益人、服务端拒绝不上报成功。
- 有效性：多数断言能失败（决策体、中文结果、不发请求）。**不能**抓住无 `id` 的真实目录（只在 stderr 警告）；「待审动作对不上目录 → 提交服务端校验」只断言发出 approve，并不断言服务端校验了动作类型。

**`SupervisorReviewPage.chain.test.tsx`（75）**

- 存在理由：最终审批人字段与链入口。
- 有效性缺陷见 GR-03：标题写「不填则不带」，体只测填写后带 `final_approver_id`；链 href 写死无前缀。

## 4. 顺从式（假定意图正确，逐项对 106）

页内中文、policyCode 附件、节点结果（`pending_review` vs `approved`）、转交在无受益人合同单上可发、既有三动作评估先要受益人、用户编号标签——测试有覆盖，**不等于**其余全量规则已满足。顺从核对结论：上述已覆盖项与 HTML/后端主路径一致；策略行身份键与生产链入口不一致，不能顺从通过。

后端对照（只证实现，不扩面）：

- `SupervisorReviewController`：view 读策略/pending/chain；manage 评估；approve 审批与转交。与页上权限点一致。
- `requiresBeneficiary`：waiver/refund/grant/authorization/debit/deduction/`.money`/`ar_ap_adjustment`。本页评估下拉只有其中免单/退款/调账，客户端这三动作先拦，与下拉对齐；授权类不在本页评估下拉（他页发起、本页清单承接）。
- `requireDefaultLines`：仅 `staff_penalty_cap_adjust`、`tax_rule_adjustment_l3` 固定平行线，二者不在本页评估下拉，故本页评估不发 `parallelLines` 不与这两条策略的 fail-closed 相撞。
- `approve`/`forward` 服务端再拦自审、受益人、转发目标；`requireDecisionEvidence` 审批侧查附件与意见。本页审批按 `policyCode` 查 `evidence_attachment`，与后端附件闸一致。
- `AdminApi` snake↔camel；`useAdminAction` 失败写 `{status:"failed", message}` 且 `setFormError`。测试「服务端拒绝不报告成功」咬住该形状。

## 5. 对抗式（假定实现错误；grilling 结构自答）

最小反例：

1. **真实策略无 id** → `REAL_POLICIES` 渲染即 React 重复 key。成立（vitest stderr）。GR-01。
2. **生产 `/complex` 点「审批链」** → `<a href="/admin/governance/approval-chain?...">` 整页跳到无前缀路径；`AdminApp` 无拦截 `<a>` 的 click 处理器，换页只在菜单 `navigate`/`navigateTo` 里 `withBase`。jsdom 默认非 `/complex`，链测试绿。GR-02。
3. **空最终审批人** → 实现 `finalApproverId.trim() || undefined`，`mapKeys` 丢 undefined，请求体应不带键。链测试标题声称覆盖，体未评空值。GR-03。
4. **评估后同一人点放行** → `hasPendingReview` 只看 `latestReviewId`，按钮可用；`validateDecisionActor` 在 click/precheck 拦住。安全闭环在，反馈差。记观察，不单开票。
5. **KPI 聚焦** → 隐藏待复核与评估表，有「清除筛选」。要先点 KPI 才发生。观察。
6. **预填 `objectId=contract-1`** → 只填违规说明即可评估假对象。HTML 未禁预填；后端仍要求非空。观察，不因宽映射发明「禁止预填」新规则。
7. **第三人把申请人登录名填进转交** → 客户端只在「当前用户即申请人/受益人」时用 `sameAccount` 拦登录名；后端按 id 相等拦。转交给登录名字符串多半转给无人认领的目标，原审批人仍保留权限（HTML：原审批人保留）。观察。
8. **双击评估** → `setBusyAction` 在 await 前同步。不单开。

ponytail-review：`REVIEW_LABELS` 与 `statusLabels` 有重叠但是中文闸，不是该删的业务。不因「可缩短」开票。

## 6. 全部 106 映射覆盖

判定：适用 = 本三文件有对应实现或展示，须给出现文证据；不适用 = 本页不承载该锚点业务，写理由。不适用不是漏审。宽映射不把未在本页出现的域规则扩成新业务。

依据类型：1=HTML 原文（锁摘录已对三份 HTML 现文，mismatch 0）；2=vitest 行为；4=命令输出。无类型 3。

### 6.1 BEQR（27）

| ID | 适用 | 证据或不适用理由 | 本席 |
|---|---|---|---|
| BEQR-063 | 部分 | HTML：反结、沽清、免单、账单调整、存酒作废须审批/双证据。本页评估下拉有免单，无反结/沽清/存酒作废（不把「本页必须发起全部高危动作」写成新需求）。待复核可展示他页发起的单。 | 无新冲突 |
| BEQR-074 | 否 | 隐私桌台三权。本页无桌台。 | N/A |
| BEQR-143 | 否 | 四类主体。本页不建主体。 | N/A |
| BEQR-144 | 否 | 经营主体多对多。 | N/A |
| BEQR-145 | 否 | 城市项目公司职责。 | N/A |
| BEQR-146 | 否 | 运营公司职责。 | N/A |
| BEQR-147 | 否 | 收入归属。 | N/A |
| BEQR-148 | 否 | 合作模式六类。 | N/A |
| BEQR-149 | 否 | M1–M10 职级配置不在本页；只展示策略 `requiredLevel`。 | N/A |
| BEQR-150 | 否 | 终端十一端。 | N/A |
| BEQR-151 | 否 | 最终权限公式在 access；本页只消费 `effectivePermissions`。 | N/A |
| BEQR-152 | 否 | 敏感经营数据分权配置不在本页。 | N/A |
| BEQR-153 | 否 | 终端开关优先级。 | N/A |
| BEQR-154 | 否 | 综合体项目状态。 | N/A |
| BEQR-155 | 否 | 商户/运营主体状态。 | N/A |
| BEQR-156 | 否 | 合同状态机。评估对象类型可写「合同」，不编辑合同状态。 | N/A |
| BEQR-157 | 否 | 员工状态。 | N/A |
| BEQR-158 | 否 | 设备状态。 | N/A |
| BEQR-159 | 弱 | 权限状态枚举。本页策略 `status` 经 `formatReviewValue`；`active`→词典「有效」。观察，不升票。 | 观察 |
| BEQR-166 | 是 | 最终审批人可空（实现 `trim() \|\| undefined`）；驳回文案「整单退回起草人」且 `decision=rejected`。平行线最终审批人：本页评估集不含税费/扣罚三审策略。链入口见 GR-02。 | GR-02 伤入口 |
| BEQR-182 | 否 | 考勤日结果。 | N/A |
| BEQR-183 | 否 | 打卡方式。 | N/A |
| BEQR-184 | 否 | 请假类型。 | N/A |
| BEQR-185 | 否 | 请假单状态。 | N/A |
| BEQR-186 | 否 | 薪资单构成。 | N/A |
| BEQR-187 | 否 | 提成发放。 | N/A |
| BEQR-189 | 否 | 服务点单管理费。 | N/A |

### 6.2 BSM（20）

| ID | 适用 | 证据或不适用理由 | 本席 |
|---|---|---|---|
| BSM-001 | 否 | 整系统「管什么」总述。 | N/A |
| BSM-003 | 是 | 权限分岗（manage/approve/view）；操作有引导（表单校验、KPI 返回）。 | 通过本锚点；他 ID 另计 |
| BSM-004 | 否 | 本页不存档/删除策略主数据。 | N/A |
| BSM-005 | 否 | 「＋新建」分步弹窗清单未列主管复核；文称持续铺开。不把缺 stepper 当冲突。 | N/A |
| BSM-006 | 否 | 无出示验证码。 | N/A |
| BSM-007 | 是 | 审批权独立 `governance.supervisor_review.approve`；无 approve 不能放行/驳回/转交。自审拦截。通配夹具不证明生产平台管理员自动有审批权。 | 通过 |
| BSM-009 | 否 | 基础管理总章。 | N/A |
| BSM-028 | 是 | 合同/基础定义卡住（评估类型+默认阻断策略）；最终审批人可空；待复核审批。七类红线本页无关闭开关。 | GR-01 伤策略表 |
| BSM-029 | 否 | 「和谁有关」他子系统。 | N/A |
| BSM-072 | 否 | 商户在授权范围用自己的系统。 | N/A |
| BSM-073 | 否 | 商户主要功能。 | N/A |
| BSM-074 | 否 | 商户和谁有关。 | N/A |
| BSM-084 | 否 | 员工手机端。 | N/A |
| BSM-085 | 否 | 日常工作。 | N/A |
| BSM-086 | 否 | 排班考勤请假。 | N/A |
| BSM-087 | 否 | 我的薪资。 | N/A |
| BSM-088 | 否 | 提成与人员服务边界。 | N/A |
| BSM-114 | 是 | 路由 `requiredPermissions: governance.supervisor_review.view`（`adminRoutes.ts` 只证入口，不扩修）。 | 通过 |
| BSM-115 | 弱 | 子系统必须连在一起。本页链到审批链页。 | GR-02 |
| BSM-120 | 是 | 已拍板：免单/退款/调账主管复核；合同或基础定义违规先卡住。评估下拉与策略目录对应这几类。高价值报损等由他页发起。 | 通过本锚点 |

### 6.3 GEN（18）

| ID | 适用 | 证据 | 本席 |
|---|---|---|---|
| GEN-001 | 是 | 三文件均有复核队列用途；无纯空转发模块。 | 通过 |
| GEN-002 | 是 | 主路径 mock 可跑；真实策略行 key、生产链 href 与声称不一致。 | **FAIL GR-01/02** |
| GEN-003 | 是 | 38 测试 exit 0，但真实目录无 id 不失败、链 href 不测 `/complex`、空最终审批人标题过声称。 | **FAIL GR-03** |
| GEN-004 | 是 | 评估/转交/审批有非空与身份校验；对象编号无格式闸。观察。 | 观察 |
| GEN-005 | 是 | 按钮级权限 + 后端对照分权；客户端自审不是唯一闸。 | 通过 |
| GEN-006 | 是 | 本页不写库；状态在服务端事务。 | N/A 本层 |
| GEN-007 | 是 | 每次动作新 `idempotencyKey`；busy 同步。 | 通过 |
| GEN-008 | 是 | 请求 camel→snake 与测试体一致；**策略记录前端要 id、后端无 id**。 | **FAIL GR-01** |
| GEN-009 | 是 | 服务端 400 走 failed 对象；不把失败当成功。 | 通过 |
| GEN-010 | 是 | 展示用户编号不是手机号；无密钥。 | 通过 |
| GEN-011 | 是 | `AuditTrailHint` + 请求带幂等键；审计事件在后端。 | 通过（后端不扩） |
| GEN-012 | 是 | pending 上限在后端 200；本页无无界循环。 | 通过 |
| GEN-013 | 是 | vitest 配置可复现。 | 通过 |
| GEN-014 | 否 | 无迁移文件。 | N/A |
| GEN-015 | 是 | 中文主文案、抽屉、未知枚举「未识别类型」；评估后放行按钮仍可点。观察。 | 观察 |
| GEN-016 | 否 | 本模块无独立文档文件。 | N/A |
| GEN-017 | 是 | findings 按冲突→细化→技术性→添加。无添加项。 | 本报告 |
| GEN-018 | 是 | 结论落到 HTML/命令/vitest，不引用席甲。 | 本报告 |

### 6.4 PGM（41）

| ID | 适用 | 证据或不适用理由 | 本席 |
|---|---|---|---|
| PGM-001 | 否 | 治理层总定位。 | N/A |
| PGM-002 | 弱 | 业务先过治理判断；本页是复核面不是全部门闸。 | N/A 主责 |
| PGM-003 | 弱 | 模块入口列表。 | N/A 卡片源 |
| PGM-004 | 否 | 综合体和集团。 | N/A |
| PGM-005 | 否 | 空间和点位。 | N/A |
| PGM-006 | 否 | 商户和主体。 | N/A |
| PGM-007 | 否 | 合同和准入**入口卡片**。正文审批模型在未映射的 PGM-029；本页实现对照 BSM-028，不拿未映射 ID 开票。 | N/A 本 ID |
| PGM-008 | 否 | 员工和手机入口。 | N/A |
| PGM-009 | 弱 | 权限和职级入口。细则在 PGM-040/041。 | 见 040/041 |
| PGM-010 | 否 | 终端开关入口。 | N/A |
| PGM-011 | 否 | 设备管理入口。 | N/A |
| PGM-012 | 否 | 商户侧入口。 | N/A |
| PGM-013 | 否 | 开放接口入口。 | N/A |
| PGM-014 | 是 | 「重要动作可追溯：谁发起、谁审批、谁复核、谁放行，敏感操作进复核队列。」本页即该队列。策略表身份键与链入口见 GR-01/02。 | **GR-01/02** |
| PGM-015 | 否 | 楼宇运营。 | N/A |
| PGM-016 | 否 | 应急与隐私入口。 | N/A |
| PGM-017 | 否 | 当前一个项目。 | N/A |
| PGM-018 | 否 | 物理项目与运营公司多对多。 | N/A |
| PGM-019 | 否 | 各主体负责什么。 | N/A |
| PGM-020 | 否 | 综合体主要功能。 | N/A |
| PGM-021 | 否 | 业务意义。 | N/A |
| PGM-022 | 否 | 空间点位。 | N/A |
| PGM-024 | 否 | 空间影响业务。 | N/A |
| PGM-026 | 否 | 商户主要功能。 | N/A |
| PGM-027 | 否 | 商户业务边界。 | N/A |
| PGM-031 | 否 | 外部经销商。 | N/A |
| PGM-032 | 否 | 员工手机对应。 | N/A |
| PGM-033 | 否 | 员工注册路径。 | N/A |
| PGM-034 | 否 | 工作手机企微。 | N/A |
| PGM-035 | 否 | 提成发放治理。 | N/A |
| PGM-036 | 否 | 设备台账。 | N/A |
| PGM-040 | 是 | 单一功能权限点。评估 manage、放行/驳回/转交 approve。 | 通过 |
| PGM-041 | 是 | 敏感动作分级审批；提交人不能审自己（页+后端）；高危授权/税费细则主落地不在本三文件。转发「不得转申请人/受益人」正文在未映射 PGM-029，本页仍实现且后端再拦——作 BSM-028/PGM-014 细化，不 LOCK-AMEND。 | 通过本锚点 |
| PGM-042 | 否 | 职级规则配置。 | N/A |
| PGM-043 | 否 | 外包/审计人员。 | N/A |
| PGM-044 | 否 | 桌台/挂单/经销权限表。 | N/A |
| PGM-048 | 否 | 商户侧全开放。 | N/A |
| PGM-059 | 否 | 个人信息加密/手机号反查。本页不查客人、不展示手机明文。 | N/A |
| PGM-060 | 否 | 会员营销生物信息。 | N/A |
| PGM-065 | 弱 | 治理连接酒吧免单/供应链复核。本页是连接面之一。 | 通过弱适用 |
| PGM-066 | 弱 | 已拍板与 BSM-120 重叠条见上；其余集团视图等不在本页。 | 通过重叠条 |

**106 条均已单独给证据或 N/A 理由。** 锁外主张不进队列。PGM-028/029/061–063、BEQR-140 未映射到 M043：转发细则与「主管复核强制项」全文不作为本模块新判据；已实现部分只按已映射 ID 审。

## 7. 四分类登记（本批）

判定顺序：冲突 → 细化 → 技术性 → 添加。无 HTML 明文冲突。无添加（不把预填对象编号、KPI 聚焦升成业务发明问操作员）。

## 8. 全部发现

### GR-01 — 策略表用 `row.id` 当 React key，后端策略记录没有 id

- 位置：`SupervisorReviewPage.tsx:368-369` `key={row.id}` / `data-testid={supervisor-review-row-${row.id}}`；`apps/web/src/admin/types.ts:427-428` `id: string` 必填；对照 `SupervisorReviewPolicyRecord.java` 字段无 `id`，权威键是 `policyCode`
- 证据（类型 2/4）：vitest 对 `REAL_POLICIES`（仅 `policy_code`，无 `id`）打印 unique key 警告；带 `id: "sp-1"` 的 `POLICIES` 用例不报。生产 `listPolicies()` 返回内存 12 条同样无 id。第一次打开本页策略表即撞上，不是极端巧合
- 2.1：细化（策略目录是 PGM-014/BSM-028 下位表）
- 2.3：类型 2+4
- 判据：GEN-002、GEN-008、PGM-014
- 工程缺陷：**成立**。测试绿不能否决

### GR-02 — 审批链入口裸 `href="/admin/..."`，不走 `withBase`，也不是 SPA `navigateTo`

- 位置：`SupervisorReviewPage.tsx:422`；对照 `apps/web/src/basePath.ts:25-26`、`AdminApp.tsx:540,562`（菜单/下钻才 `withBase`）；`ApprovalChainPage.tsx:67-69` 读 `review_id` 查询参数（参数名正确）
- 证据：源码；`basePath.test.ts` 生产 pathname `/complex/admin` 时 `withBase("/admin/customers")` → `/complex/admin/customers`。本页 `<a>` 会触发浏览器整页导航到无前缀路径。jsdom 默认非 `/complex`，链测试仍绿
- 2.1：细化（链入口是 BEQR-166/BSM-115/PGM-014 下位）
- 2.3：类型 1（部署前缀约定写在 `basePath.ts` 注释与现有测试）+ 类型 4（该测试文件，本席未改）
- 判据：GEN-002、GEN-008、PGM-014、BEQR-166
- 工程缺陷：**成立**。本席未跑浏览器，不声称已看见 404；缺陷由入口构造与 `withBase` 契约直接推出。生产点「审批链」是待复核每一行的正常动作

### GR-03 — 测试不能红 GR-01/GR-02，且链测试过声称空最终审批人

- 位置：`SupervisorReviewPage.chain.test.tsx:55-65,68-73`；`SupervisorReviewPage.test.tsx:9-12` vs `14-27`
- 证据：链测试名「不填则不带」，体只 `final_approver_id === "usr_gm"`；href 期望写死 `/admin/governance/approval-chain?review_id=review_p1`。主测试真实目录用例 exit 0 同时 stderr 重复 key
- 2.1：技术性（测试，无业务发明）
- 2.3：类型 4
- 判据：GEN-003
- 工程缺陷：**成立**

### 观察（不升票，首次正常路径不单独成立或 HTML 未要求）

- 评估成功后放行/驳回按钮仍 enabled，precheck 才报「不能复核自己发起的单」。规则被执行，按钮态偏松。GEN-015
- KPI 聚焦藏起待复核与表单，有清除。BSM-003 引导仍在
- 默认 `objectType=合同` `objectId=contract-1`。不发明「禁止预填」
- 策略状态 `active` 显示「有效」，KPI 文案「启用策略」。BEQR-159 并非策略状态枚举
- 评估动作集是目录子集；报损/税费/排班由他页评估。不扩「本页必须发起全目录」
- `AdminDataState empty={rows.length===0}`：后端策略目录非空时不触发。依赖目录非空
- 计划 660 行 vs 现文 1018：A1 冻结数，现文实算以本节为准

## 9. 原有评估 / KPI / 转交 / 审批 / 驳回 / 链 / 身份 / 证据 / 错误（整模块清单，不只修复票）

| 面 | 现文结论 |
|---|---|
| 原有评估 | 违规说明必填；三动作受益人先拦；最终审批人可空且不得为当前用户或受益人；下拉无 `billing_discount`。假对象预填见观察 |
| 策略 KPI/明细 | 总数/默认阻断/启用可聚焦；最高级别不可点。抽屉中文。**真实目录 key 见 GR-01** |
| 转交 | 必选单+用户编号；申请人未登记则拦；不得转申请人/受益人（含当前用户登录名自转）；无受益人合同单可发。服务端再拦 |
| 审批 | approve 权限；意见必填；`policyCode` 含 `evidence_attachment` 才要附件；节点文案分「当前节点」/「全部节点」。自审/受益人 click 拦 |
| 驳回 | 同 approve 端点 `decision=rejected`；文案整单退回起草人 |
| 链入口 | 每行「审批链」+`review_id`。**生产前缀见 GR-02** |
| 身份/范围 | view 入页；manage 评估；approve 放行/驳回/转交。无项目切换 UI（隔离在网关） |
| 字段/证据 | 附件按策略；原因/快照类不另开字段，意见与硬编码 `operatorNote` 顶后端 reason/operator_note。不把未映射证据枚举写成新需求 |
| 错误 | 表单中文；API 失败 `status:failed` 不上报成功 |
| 测试有效性 | 38 绿不能覆盖 GR-01/02；见 GR-03 |

## 10. 结论

**FAIL**

不是有条件通过。GR-01/GR-02/GR-03 为未定稿工程缺陷（无本席正式 findings ID；001—008 清队不消灭本批）。任一条即本席 B5 FAIL。未跑浏览器/全后端的路径已标明，未冒称。

本席不改 findings/ledger、不修复、不写其它审计产物。
