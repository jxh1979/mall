CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M038 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M038-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M038 / 共 471，B3 第 1 次之后的 B4。计划名：功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未打开：`modules/M038-b4-r1-self-raw.md`（落盘时该路径已存在）。开工时对 `docs/audit-2026-09` 的 `M038` 文件名检索曾命中该路径的若干行（归因表片段），本席未整文件打开、未采信其票面；本票以两 tree 全文 diff、findings V3.18 定稿、本席 B2-r1 边界、本席 origin/main 一次性 worktree 实跑为准。已读：`160-audit-prompt.md` V0.17 B4／§5 闸 3／§6、`findings.md` V3.18 顶部「当前 M038 B2-r1 双PASS定稿」、本席旧 `M038-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、冻结 diff、`m038_verify.log` 职级闭集、两 tree 与 origin/main 的 16 文件 blob。ledger 只读取模块状态与 B3 次数，不写入。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M038-001…010 是否按 findings V3.18 定稿最小处置与本席 B2-r1 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID；新测试断言能否失败；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自问自答，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿证据并本席重跑 GREEN，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席 vitest/tsc 同步跑完，无后台作业遗留。`graft`：本环境存在，已用于定位 `runWrite`／`zhPosition`／`STAFF_LEVEL`／`grantStatusLabel`／`createdViaLabel`／`AdminApi`。Trail of Bits `differential-review` 本体强制子代理，**不用**。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位 runWrite／zhPosition／职级闭集／开通方式映射／AdminApi | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 测试 seam 对齐页内映射／runWrite／formatBusinessStatus，不改被测闸语义 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、worktree vitest/tsc） | 未本席重放 RED（会改被测）；以冻结 `m038_b3r1_run.log` 为旁证 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→页内映射／runWrite／toDateTimeLocal→测试断言）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实算哈希/`cmp`/16 文件 blob/worktree 11 文件 57 绿 + tsc | 全量 928 未本席重跑 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 001…010 新断言能否失败；本席重跑 GREEN 57；不改被测 | 无 |
| `long-running-background-tasks` | grok bundled | vitest/tsc 同步跑完 | 节点不适用遗留 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：页内小映射 + 一包装 + 页内 30 档，ultra 已满足 | 不修 |
| `i-have-adhd` | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号；与独占锁行冲突时 160 优先 | 无 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`（B3/B4/B5 主工作区就地；本席为 GREEN 建的 origin/main 一次性 worktree `/tmp/m038-b4-r1-grok-wt` 已 `git worktree remove` 删除、未用 `--force`）、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（`/tmp/m038_b4_r1_live.diff` 在 `cmp` 后已删；一次性 worktree 已删除）。

graft 本席调用节省合计约 57452 tokens（`runWrite` 12207 + `zhPosition` 12504 + `STAFF_LEVEL` 12787 + `grantStatusLabel` 12621 + `createdViaLabel` 4953 + `class AdminApi` 2380）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值（对照，非抄写来源）。ledger 顶部基准同值（对照）。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。未 LOCK-AMEND。两 tree 的 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（本 diff 未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M038 网页与验证／第1段（计划 16 文件；本轮 B3 写入集合＝三页 + 各页测试 + `PermissionGate.test.ts`） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.18：M038-001…004 冲突／是／是；005…007 技术性／是／是；008…010 细化／是／是；修复队列「否」0 |
| findings 全文 SHA256 | `67f839c7c6e100c6cc2a9ba1ab86ade731a69f51cd186dbbd057136423e650a7`（本席实算）；1178081 字节；版本 V3.18 |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `df21dfdfa3aaa27416857eeca14dedcd04cee631` |
| after（对象类型 tree） | `9df1fad44caa2ef615993fe970d3efc4620aca25` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary df21dfdfa3aaa27416857eeca14dedcd04cee631 9df1fad44caa2ef615993fe970d3efc4620aca25`（无路径过滤） |
| HEAD / 分支 | `e456ba96c84185c8d39f18f548011daac48a6466` / `main` |
| origin/main | `ebb1eb99c610df330c07d24278798c2d3611299a` |
| 本席 B2-r1 边界 | `M038-b1-grok-raw.md` V0.02；全文 65479 bytes SHA256 `0d09b4c1035d80616349f4120305a902a7052a87fed1697a01e318914e01872d` |

### 1.1 冻结产物独立复核

| 产物 | 声称 | 本席实算 | 对照 |
|---|---|---|---|
| 冻结 diff `/home/james/audit-tmp/m038_b3r1.diff` | 73515 bytes SHA256 `30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b`；8 files, +484/−61 | 73515 bytes，同 SHA；`git diff --binary` 两 tree 写入临时文件后 `cmp` **identical**，同 SHA；CR=0 LF=869 NUL=0；8 个 `diff --git`；37 hunk | 通过 |
| after blob vs 主工作区 8 个改动文件 | after 即发布版 | 8 个 `git hash-object` 与 `git ls-tree after` 全 MATCH | 通过 |
| 修前 16 文件 vs origin/main | 逐字节相同 | 16 个 before blob 均等于 `origin/main` 同路径 blob | 通过 |
| 未改的 8/16 文件 | after=before | degraded／scopechange／metricdetail／partnerbind／robustness／templates.guidedcreate／Can.tsx／PermissionGate.tsx 的 before=after=origin=工作区 | 通过 |

文件模式：8 个路径 before/after 均为 `100644`（`git diff --raw`）。无 A/D/R。可执行位未变。

路径集合：两 tree diff **仅**下列 8 个、全部落在本模块 16 文件之内：

1. `apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx`
2. `apps/web/src/admin/pages/AccessGovernancePage.test.tsx`
3. `apps/web/src/admin/pages/AccessGovernancePage.tsx`
4. `apps/web/src/admin/pages/AccessUsersPage.test.tsx`
5. `apps/web/src/admin/pages/AccessUsersPage.tsx`
6. `apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx`
7. `apps/web/src/admin/pages/PermissionTemplatesPage.tsx`
8. `apps/web/src/admin/security/PermissionGate.test.ts`

`git diff --name-only` 不含 `statusLabels.ts`、`adminClient.ts`、`backend/`、三 HTML、lock、OpenAPI、迁移。未新建 `AccessGovernanceGrantRequest.guidedcreate.test.tsx`（M080 同名文件不在本 diff；仓内该路径属别的模块，本轮未碰）。

主工作区另有别的模块未提交改动（FloorPlanImport／MerchantScope 等）；**不进入本 B4 对象**。B4 只认两 tree id 之差。

---

## 2. 本轮 diff 全文

冻结文件与 live `git diff --binary` 逐字节相同。全文见本文件附录（从「附录：两 tree binary diff 全文」起）。stat：`8 files changed, 484 insertions(+), 61 deletions(-)`。± 行合计 545。

---

## 3. 逐块／逐行归因（闸 3）

归因只认 findings V3.18 归属「是」的 M038-001…010。混合 hunk 按行切开。验证性测试归因到所锁 ID。文件版本行随该文件所含 ID。无「顺手优化」行。

### 3.1 `AccessGovernancePage.guidedcreate.test.tsx`（3 hunk，+80/−2）

| 全 diff hunk | @@ | ± | 归属 |
|---|---|---|---|
| 1 | −86,7 +86,7 | 1/1 | **008**：`lvl_p1`→`lvl_m2_upper`（非闭集夹具） |
| 2 | −94,7 +94,7 | 1/1 | **008**：断言 `staff_level_id` 同步 |
| 3 | −111,3 +111,81 | 78/0 | **010**：补「入口→类型与用户→权限码与过期时间→提交」及步内校验拦截；未新建 M080 同名文件 |

### 3.2 `AccessGovernancePage.test.tsx`（2 hunk，+144/−1）

| 全 diff hunk | @@ | ± | 归属 |
|---|---|---|---|
| 4 | −95,7 +95,7 | 1/1 | **002**：`getByLabelText("覆盖用户 id")`→`"覆盖用户"` |
| 5 | −183,3 +183,146 | 143/0 | 见下行拆分 |

hunk 5 行拆分（新文件约 L187–329）：

- L187 注释 + `describe` + `afterEach` + `M038_GRANTS`/`M038_TEMPLATES`/`renderM038` 夹具：**001/003/004/005/007/008/009** 共用（夹具同时服务这七条）
- 「授权申请的三种状态与内部类型全部中文上屏」：**001**
- 「审批不填意见时留痕为中文「通过」」：**001**
- 「未映射的岗位码显示为「未登记岗位（岗位码）」」：**003**
- 「已存档的按钮写「取消存档」、已删除的写「恢复」」：**004**
- 「装配角色被后端拒绝时页面报出原因」：**005**
- 「提交未完成时再次点击不发第二笔」：**005**
- 「智能总结表表头列数等于数据行单元格数」：**007**
- 「职级是 30 档下拉，范围变更处另有「沿用原职级」」：**008**
- 「岗位模板弹窗里的状态用词与表格同一套」：**009**

### 3.3 `AccessGovernancePage.tsx`（15 hunk，+82/−41）

| 全 diff hunk | @@ | 行级归属 |
|---|---|---|
| 6 | −1,4 +1,4 | **随本文件所含 ID（001/002/003/004/005/007/008/009）**：`// 文件版本：V0.03`→`V0.04`（派发核项「两个页面各加一」） |
| 7 | −49,7 +49,28 | **003**：`zhPosition` 未映射→`未登记岗位（${code}）`；**008**：`STAFF_LEVEL_IDS` 循环 1..10×三档 + `staffLevelOptions()` 走 `zhLevel`；**001**：`GRANT_STATUS_LABELS`／`GRANT_TYPE_LABELS`／`grantStatusLabel`／`grantTypeLabel` |
| 8 | −81,14 +102,31 | **001**：`statusBadge` 增加 `label` 形参；**005**：`writeBusy`／`writeBusyRef`／`runWrite`（置忙、执行、失败 `Error.message` 否则兜底、复位） |
| 9 | −371,7 +409,7 | **001**：申请已提交提示走 `grantStatusLabel` |
| 10 | −386,11 +424,12 | **001**：空意见 `comment`／`operatorNote` 改为「通过」／「驳回」；结果提示走 `grantStatusLabel` |
| 11 | −415,12 +454,12 | **001**：抽屉类型／状态走 `grantTypeLabel`／`grantStatusLabel` |
| 12 | −479,12 +518,12 | **005**：存档／申请删除／批准删除／驳回删除／恢复均 `disabled={writeBusy}` 且 `runWrite(...)`；**004**：恢复按钮文案 `{row.deletedAt ? "恢复" : "取消存档"}`（删「恢复为停用模板」）。该恢复行双属 004+005 |
| 13 | −503,48 +542,48 | **002**：aria-label／占位去掉「id／ID」（用户、职级、变更用户、新所属项目编号、新所属商户编号、新范围职级、装配用户、装配角色、覆盖用户）；既有 `data-testid` 字符串未改。**008**：定岗／范围变更职级改 `<select>{staffLevelOptions()}`，范围变更加 `<option value="">沿用原职级</option>`；定岗新增 `data-testid="access-user-position-level"`（旧输入无 testid，为 008 测试而加，不是改旧 id）。**005**：定岗／范围变更／装配／卸载／覆盖设置／覆移除均 `disabled={writeBusy}`+`runWrite` |
| 14 | −569,12 +608,12 | **001**：申请表类型／状态走 `grantTypeLabel`／`statusBadge(..., grantStatusLabel)`；**005**：通过／驳回 `onClick` 包 `runWrite`（RowActions 接口无 `disabled`，16 文件外组件不能改；双击由 `writeBusyRef` 闸） |
| 15 | −609,7 +648,7 | **005**：保存授权政策 `disabled={writeBusy}`+`runWrite` |
| 16 | −617,7 +656,7 | **007**：删重复 `<th>详情</th>` |
| 17 | −652,16 +691,16 | **008**：模板弹窗职级改下拉；**002**：职级／角色／权限模板编号的 label／aria／占位去掉 id；`data-testid="access-position-role-template-level|role|template-id"` 未改。**009**：模板状态下拉 `formatBusinessStatus("active"|"inactive")` |
| 18 | −671,7 +710,7 | **009**：确认摘要状态改 `formatBusinessStatus(templateForm.status)` |
| 19 | −713,7 +752,7 | **008**：申请职级改下拉；**002**：去掉占位「职级 id」 |
| 20 | −723,7 +762,7 | **001**：申请确认摘要类型走 `grantTypeLabel` |

### 3.4 `AccessUsersPage.test.tsx`（2 hunk，+45/−1）

| 全 diff hunk | @@ | 归属 |
|---|---|---|
| 21 | −1,4 +1,4 | **随本文件 001/009**：该测试文件原有版本行 V0.01→V0.02 |
| 22 | −90,3 +90,47 | **001**：开通方式「历史账号／后台手动录入」抽屉断言；**009**：初始状态下拉 `["有效","待激活","已禁用"]` |

### 3.5 `AccessUsersPage.tsx`（6 hunk，+13/−9）

| 全 diff hunk | @@ | 归属 |
|---|---|---|
| 23 | −1,4 +1,4 | **随本文件所含 ID（001/002/009）**：页面版本 V0.02→V0.03 |
| 24 | −15,6 +15,12 | **001**：`CREATED_VIA_LABELS` `{manual_admin:"后台手动录入", legacy:"历史账号"}` + `createdViaLabel` |
| 25 | −92,7 +98,7 | **002**：`validateForm`「组织 ID 不能为空」→「所属组织不能为空」 |
| 26 | −179,7 +185,7 | **001**：抽屉开通方式走 `createdViaLabel` |
| 27 | −319,14 +325,14 | **002**：步骤校验句、占位「组织编号」「附件编号」；`data-testid="access-user-org|evidence"` 未改 |
| 28 | −338,9 +344,7 | **009**：初始状态下拉改 `formatBusinessStatus(value)`（确认摘要本已走 `formatBusinessStatus(status)`，before 即如此，本 diff 不必再改） |

### 3.6 `PermissionTemplatesPage.test.tsx`（1 hunk，+59/−0）

| 全 diff hunk | @@ | 归属 |
|---|---|---|
| 29 | −92,3 +92,62 | **001**：`role` 显示「角色模板」、编辑下拉含该项；**006**：`vi.stubEnv("TZ","Asia/Shanghai")` 往返同一时刻 |

### 3.7 `PermissionTemplatesPage.tsx`（6 hunk，+8/−4）

| 全 diff hunk | @@ | 归属 |
|---|---|---|
| 30 | −15,7 +15,7 | **001**：映射补 `role: "角色模板"` |
| 31 | −39,11 +39,13 | **006**：`toDateTimeLocal` 按 `getFullYear/getMonth/getDate/getHours/getMinutes` 本地时区取年月日时分 |
| 32 | −312,7 +314,7 | **002**：校验句「目标员工编号」 |
| 33 | −376,6 +378,7 | **001**：编辑下拉补 `<option value="role">角色模板</option>` |
| 34 | −410,7 +413,7 | **002**：label「目标员工编号」；`data-testid="permission-template-target-users"` 未改 |
| 35 | −496,6 +499,7 | **001**：新建下拉补同一 `role` 项 |

该页无「文件版本」行（B2 只登记 OBS-6，不入队）。

### 3.8 `PermissionGate.test.ts`（2 hunk，+54/−2）

| 全 diff hunk | @@ | 归属 |
|---|---|---|
| 36 | −1,7 +1,10 | **010**：为组件渲染引入 `@testing-library/react`／`createElement`／`Can`／`PermissionGate` |
| 37 | −59,3 +62,52 | **010**：无权限禁用且点击不触发；Can fallback；Gate 加载中／权限不足文案。多测的「有权限点击触发」「显式拒绝压过授予」「放行」仍锁同一组件分支，归因 010 |

### 3.9 归因合计与越界检查

- ± 行 545 全部落入上表。无空归属行。
- 无黑名单例外。无「顺手抽象／换框架／改公共字典」。
- 新增 `data-testid="access-user-position-level"` 与测试内 `gate-btn`／`gate-btn-deny` 为 008／010 测试夹具，合法。
- 既有 testid 字符串（含 `access-position-role-template-template-id`）未改。
- **闸 3 归因票：PASS。**

---

## 4. 逐 ID 内容审计（定稿最小处置 × 本席 B2-r1 边界）

### 4.1 M038-001 — 页内映射覆盖后端闭集 · 内容 PASS

生产：

- 授权申请状态闭集三值：`pending→待处理`（与字典同文）、`applied→已生效`（定稿用词）、`rejected→已驳回`（与字典同文）。表／抽屉／提交与审批结果提示走 `grantStatusLabel`，不再把 `applied` 交给字典原样回英文。
- 类型闭集六值：`user_role→用户角色`、`user_position→用户职位`、`temporary_permission→临时权限`（三码与 `statusLabels.ts` 同文）、`role_permission→角色权限`、`user_scope_change→人员范围变更`、`position_high_risk_release→岗位高危权限释放`（后三码为定稿七个中文之三）。下拉 value 仍走原码。申请弹窗下拉**未**扩成后端另三种服务生成类型（B2 禁止项未犯）。
- 开通方式：`manual_admin→后台手动录入`、`legacy→历史账号`。字典无这两键，页内小映射。
- 模板类型：映射与**两个**下拉（编辑 `permission-template-edit-type`、新建 `permission-template-type`）补 `role→角色模板`。
- 空审批意见：`通过`／`驳回`（沿用本页按钮原文，非发明）。

七个定稿中文与实现逐字相同。未改 `apps/web/src/customer/i18n/statusLabels.ts`（admin 侧只 re-export）。无业务发明。

测试：三种状态 + 三种内部类型上屏；空意见 body.comment／operator_note 为「通过」；开通方式两值抽屉；`role` 列与编辑下拉。断言为独立中文／英文码字面量，能失败。

### 4.2 M038-002 — 字段名／占位／aria／校验去 id · 内容 PASS

B2 点名的给人看的 id／ID 均已改为「编号」或去掉。抽查 after：无「用户 id」「职级 id」「组织 ID」「目标员工 ID」「附件 ID」。`data-testid` 旧值保留。代码注释／取值展示 `row.id`／四样例外不在本 ID 范围。随改 `getByLabelText("覆盖用户")`。

### 4.3 M038-003 — 未登记岗位（岗位码） · 内容 PASS

`zhPosition`：`POSITION_LABELS[code] ?? \`未登记岗位（${code}）\``。不新造岗位字典、不改输入。测试钉死单元格 `未登记岗位（front_desk）`。本席 B2 同意括号留码，不坚持不得出现岗位码。

### 4.4 M038-004 — 取消存档／恢复 · 内容 PASS

已存档未删除：`取消存档`；已删除：`恢复`。测试断言两种文案且表内不再出现「停用模板」。未改后端 restore。

### 4.5 M038-005 — runWrite 七个写动作 · 内容 PASS

页内一个 `writeBusy` + `writeBusyRef` + 一个 `runWrite`：先拒重入、置忙、清错、执行、`catch` 用 `Error.message`（`apiErrorFromResponse` 在中文 `message` 时原样抛出）否则动作兜底句、`finally` 复位。七个写动作走它：`saveGrantPolicy`、`changePositionLifecycle`、`assignPosition`、`changeUserScope`、`roleAssignment`、`userPermissionOverride`、`reviewGrant`。引导保存仍用既有 `templateSaving`／`grantSaving`，未再包一层（B2 禁止项未犯）。未改 `adminClient` 幂等键。

提交中：上述 PermissionButton 均 `disabled={writeBusy}`。审批走 `RowActions`（不在 16 文件、无 disabled 槽），双击由 `writeBusyRef` 闸住——在写入集合约束下的最小可行。

测试：装配角色 403 后 `setGlobalError` 被调用且无成功提示；提交未完成再点不发第二笔且按钮 disabled。定稿还写了「审批各打桩一次 403」：本 diff 未单列该用例，但审批与装配走同一 `runWrite`+同一 `AdminApi.request` 抛错路径；不构成未修生产缺陷，亦不构成无 ID 新行。403 断言为 `/\S/`，弱于钉死后端中文句，但仍能在「完全不报错」时失败。

`saveGrantPolicy`／`changePositionLifecycle` 函数体内既有 try/catch **不在本 diff**（pre-existing）；调用点另包 `runWrite` 只补提交中态。ponytail：不借本轮删除旧 catch。

### 4.6 M038-006 — toDateTimeLocal 本地时区 · 内容 PASS

不再 `toISOString().slice(0,16)`。按本地 `getFullYear`…`getMinutes` 拼 `YYYY-MM-DDTHH:mm`。保存侧 `toIsoOrUndefined` 未改（仍 `new Date(value).toISOString()`，与本地 datetime-local 对称）。未改后端有效期闸。测试：`TZ=Asia/Shanghai` 且 `getHours()===8` 自检；输入框 `2026-06-14T08:00`／`2026-06-15T08:00`；PUT 体与 `2026-06-14T00:00:00Z`／`2026-06-15T00:00:00Z` 同一时刻。该自检在非 +8 且 stub 失效时会红，能失败。

### 4.7 M038-007 — 删重复表头 · 内容 PASS

thead 由 7 列（两个「详情」）改为 6 列，与每行 6 td 对齐。测试 `thead th.length === 首行 td.length`。未改总结接口。

### 4.8 M038-008 — 四处 30 档下拉 · 内容 PASS

四处：定岗、范围变更、岗位模板弹窗、授权申请弹窗，均 `<select>{staffLevelOptions()}`。范围变更另有 `<option value="">沿用原职级</option>`。选项页内 `for main=1..10 × {lower,middle,upper}` 生成 `lvl_m${main}_${band}`，显示 `zhLevel`（正则 `^lvl_m(\d+)(?:_(lower|middle|upper))?$` → `M1·下`…`M10·上`）。

本席独立生成 30 个 id，与 `/home/james/audit-tmp/m038_verify.log` 的 `staff_level ids:` 逐键全等；30 个显示互异。本模块 `staff-levels` 字面命中 0。未新增 `access.staff_level.view`。引导测试 `lvl_p1` 已改为闭集 `lvl_m2_upper`。

测试验 length=30、首 `lvl_m1_lower`／`M1·下`、末 `lvl_m10_upper`／`M10·上`、范围 31 项且首项「沿用原职级」。未在断言里列出全部 30 键；本席已用闭集生成器对照库列表补核。四处中后两处与前两处共用同一 helper，生产四处均已改。

### 4.9 M038-009 — 下拉／摘要与表格同一套中文 · 内容 PASS

账号页初始状态下拉：`active/invited/disabled` 经 `formatBusinessStatus` → 测试期望 `["有效","待激活","已禁用"]`（字典 L23／L91／L92）。该页确认摘要 before 已走 `formatBusinessStatus(status)`，本 diff 无摘要行——不是漏改。

治理页模板状态下拉与确认摘要：`formatBusinessStatus("active"|"inactive")` → 「有效／未激活」，不再「启用／停用」。公共字典未改。

### 4.10 M038-010 — 闸组件渲染 + 本模块引导提交 · 内容 PASS

`PermissionGate.test.ts` 仍为 `.ts`，`createElement` 渲染：无权限按钮 `disabled`＋`title="当前账号无此操作权限"`＋点击不触发；Can 无权限 fallback／有权限子内容；Gate `权限加载中`／`权限不足`。对照 `Can.tsx` L76-78 与 `PermissionGate.tsx` L29-40 原文。未改生产闸语义。

治理页引导提交补进**已有** `AccessGovernancePage.guidedcreate.test.tsx`：POST `/api/v1/access/grant-requests` 断言 `request_type`／`target_user_id`／`permission_codes`／`expires_at`／`single_use`。未新建 M080 同名文件。步内校验拦截属同一路径锁测，归因 010。

### 4.11 写入集合／版本／模式／发明

- 两 tree 仅 8 文件，均在 16 文件集合内。`Can.tsx`／`PermissionGate.tsx` 未改（010 只改测试）。
- 未改公共字典、`adminClient.ts`、后端、OpenAPI、迁移、三 HTML、锁。
- 页面版本：`AccessGovernancePage.tsx` V0.03→V0.04；`AccessUsersPage.tsx` V0.02→V0.03。`PermissionTemplatesPage.tsx` 无版本行（OBS-6）。测试文件 `AccessUsersPage.test.tsx` V0.01→V0.02 随该文件已有版本行。
- 文件模式 100644 未变。无新文件、无删除、无改名。
- 无业务发明：七个中文为定稿直译／本页或 PGM-041 原文；「未登记岗位（码）」为定稿；「沿用原职级」为定稿；「通过／驳回」为本页按钮原文。

**内容票：PASS。**

---

## 5. 新断言能否失败（TDD／verification）

| 断言 | 独立期望 | 若生产回退 |
|---|---|---|
| 001 表含「已生效」等、不含 `applied` | 字面量 | 回英文则红 |
| 001 comment==="通过" | 字面量 | 再发 `approve` 则红 |
| 001 开通方式「历史账号／后台手动录入」 | 字面量 | 回 `legacy` 则红 |
| 001 列「角色模板」 | 字面量 | 回 `role` 则红 |
| 002 `getByLabelText("覆盖用户")` | 旧文案不存在 | 仍写「覆盖用户 id」则本用例找不到元素（随改的既有测试） |
| 003 `未登记岗位（front_desk）` | 精确单元格 | 裸码则红 |
| 004 含「取消存档」「恢复」、不含「停用模板」 | 字面量 | 旧文案则红 |
| 005 `setGlobalError` 被调用 | 失败路径 | 无 catch 则可能未调用／未处理拒绝 |
| 005 未完成时 writes.length===1 且按钮 disabled | 行为 | 无 busy 则第二笔 |
| 006 框值 `2026-06-14T08:00` 且 PUT 时刻等于 Z | 独立时刻 | UTC slice 则框为 `T00:00` 或发出早 8 小时 |
| 007 th 数=首行 td 数 | 结构 | 双「详情」则 7≠6 |
| 008 length 30／首末键与「沿用原职级」 | 独立字面量 | 自由文本或少档则红 |
| 009 选项 `["有效","待激活","已禁用"]`／`["有效","未激活"]` | 字典中文 | 仍写启用／禁用／停用则红 |
| 010 无权限 disabled＋onClick 0 次；fallback 文案；加载中／权限不足 | 组件行为 | 无拦截则红 |
| 010 POST body 五字段 | 独立字面量 | 不提交或字段错则红 |

无恒真断言（无 `expect(true)`、无用实现重算期望）。010 组件用例在**未改页面**时即绿，符合定稿「补测、属缺测」。001/003/004/005/006/007/008/009 及随 002 改 aria 的既有测试在只落测试补丁时应红——本席未重放 RED（会改被测）；旁证为冻结 `m038_b3r1_run.log` 所述 RED 14 条。本席独立 GREEN：见 §6。

---

## 6. 本席实跑（verification-before-completion）

一次性 worktree：`git worktree add --detach /tmp/m038-b4-r1-grok-wt origin/main`（ebb1eb99）；`git apply /home/james/audit-tmp/m038_b3r1.diff`；软链主工作区 `node_modules` 与 `apps/web/node_modules`；`apps/web` 下 `npx vitest run` 本模块 11 个测试文件；`npx tsc --noEmit -p tsconfig.json`；然后 `rm` 软链、`git checkout -- .`、`git worktree remove`（**未** `--force`）。路径已不存在。

| 命令 | 结果 |
|---|---|
| vitest 11 文件 | Test Files 11 passed；Tests **57 passed (57)**；exit 0；Duration 2.45s |
| tsc --noEmit | TSC_EXIT=0 |

11 文件即计划 M038 的 11 个测试文件（3 页 × 既有测试 + PermissionGate.test.ts）。57=基线 38 + 本轮新/回归用例，与派发证据口径一致。

全量前端 928 测：**未**本席重跑（主工作区混有其他模块未提交改动，不能当 after 树；第二次 worktree 本席不建）。不以「应能过」宣称无新增失败。冻结 `m038_b3r1_run.log` 记 928 中失败 3 条与 origin/main 基线相同（TraceLinkDrilldown／TraceLinkP2c，别的模块）——作旁证，不作本席实跑。

未对任何数据库执行写语句。未调用会写的接口。未 fetch/commit/push/merge/deploy。未装依赖。

---

## 7. 对抗式（grilling 自问自答）

1. 申请类型下拉是否扩成六种？否，仍 temporary_permission／user_role／user_position 三值。B2 禁止项未犯。
2. 是否 GET `/staff-levels` 或新权限码？本模块 `staff-levels` 命中 0。
3. 是否改公共字典／客户端／后端？两 tree 路径集合无。
4. 是否把引导保存再包进 runWrite？否，仍 `templateSaving`／`grantSaving`。
5. 「已生效」等是否 2.2 发明？否，B2 已核直译／本页或 PGM-041 原文；本席对照实现逐字相同。
6. 职级生成是否与 zhLevel／库 30 键漂移？独立生成器与 verify.log 全等。
7. 未映射岗位是否仍裸英文？测试钉死「未登记岗位（front_desk）」；映射内 ops_manager 仍中文。
8. 恢复文案是否仍「停用模板」？测试禁止该串。
9. datetime-local 是否仍 UTC 切片？函数体已改；006 测试在 +8 自检失败时即红。
10. 是否新建 M080 同名文件？diff 无 A；引导补丁在本模块已有 guidedcreate 文件末尾。
11. 新 testid 是否改旧 id？旧 testid 字符串保留；仅定岗职级新增 testid（旧输入无）。
12. 混合 hunk 是否夹带无关重构？无；同行多为 002 文案与 008 控件或 005 包装的必要同触。

---

## 8. ponytail-review（只找可删越界）

Lean already. Ship.

页内 Record 映射、一包装、页内 30 档循环，均为定稿最小面。010 多测的拒绝压过授予／引导步内校验是同一 finding 的锁测，不是新能力。不建议删。

net: -0 lines possible beyond 定稿。

---

## 9. 登记（不入本轮 FAIL）

下列不构成无 ID 新缺陷、不构成未完成最小处置：

- 005 审批 403 未单独打桩（同包装已由装配角色 403 锁住）。
- 005 403 断言为非空而非后端中文句。
- 008 测试未列出全部 30 键（本席已对照 verify.log）。
- 001 测试未点名「用户角色」「用户职位」（映射与弹窗下拉已有且与字典同文；定稿七个新中文均有断言）。
- 审批按钮因 RowActions 无 disabled 槽而不能视觉置忙（16 文件外；`writeBusyRef` 仍防第二笔）。

不为这些新造 ID，不发 2.2，不 LOCK-AMEND。

---

## 10. 结论

| 票 | 结果 |
|---|---|
| 修复内容（001…010 最小处置与 B2 边界） | **PASS** |
| 范围归因（闸 3，545 ± 行） | **PASS** |
| 本席 B4-r1 总结论 | **PASS** |

任一 FAIL 即 FAIL：本轮两票均 PASS，总结论 PASS。不是模块 PASS，不是 B5，不是授权改 findings 队列。合取仍须席甲本次完整 B4 raw 双 PASS。

未修代码、未改 HTML／锁／findings／ledger。未询问操作员。


---

## 附录：两 tree binary diff 全文

命令：`git diff --binary df21dfdfa3aaa27416857eeca14dedcd04cee631 9df1fad44caa2ef615993fe970d3efc4620aca25`。与冻结文件 `/home/james/audit-tmp/m038_b3r1.diff` `cmp` 全等。

```diff
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx b/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
index 0e798a4e..84566763 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
@@ -86,7 +86,7 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));
 
     fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "front_desk" } });
-    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_p1" } });
+    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_m2_upper" } });
     fireEvent.click(screen.getByTestId("access-position-role-template-guided-next")); // 步2→确认
     await waitFor(() => expect(screen.getByTestId("access-position-role-template-guided-summary")).toBeTruthy());
     fireEvent.click(screen.getByTestId("access-position-role-template-save"));
@@ -94,7 +94,7 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     await waitFor(() => expect(lastPostBody).toBeTruthy());
     expect(lastPostUrl).not.toContain("prt-"); // 新建走 POST 集合路径，非 PUT /{id}
     expect((lastPostBody as { position_code: string }).position_code).toBe("front_desk");
-    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_p1");
+    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_m2_upper");
     expect((lastPostBody as { role_id: string }).role_id).toBe("role-1");
     await waitFor(() => expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull());
     expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^岗位模板已保存/)); // PGM-1.18：结果提示全中文
@@ -111,3 +111,81 @@ describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () =
     expect(lastPostBody).toBeNull();
   });
 });
+
+// M038-010：发起授权申请的引导提交（入口→类型与用户→权限码与过期时间→提交）此前没有测试。
+describe("AccessGovernancePage 引导式发起授权申请（M038-010）", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  it("入口→步进→提交：POST /grant-requests 带类型、目标用户、权限码、过期时间与单次有效", async () => {
+    let grantPost: { url: string; body: Record<string, unknown> } | null = null;
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/api/v1/access/grant-requests")) {
+        grantPost = { url, body: JSON.parse(String(init?.body)) };
+        return Promise.resolve(raw({ id: "agr-new", status: "pending" }));
+      }
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
+      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const setActionResult = vi.fn();
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult,
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessGovernancePage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
+    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步1→步2
+    fireEvent.change(screen.getByLabelText("临时权限码"), { target: { value: "finance.refund.create" } });
+    fireEvent.change(screen.getByLabelText("过期时间"), { target: { value: "2026-12-31T18:00" } });
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步2→确认
+    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-request-create"));
+
+    await waitFor(() => expect(grantPost).toBeTruthy());
+    const body = grantPost!.body;
+    expect(body.request_type).toBe("temporary_permission");
+    expect(body.target_user_id).toBe("u-1");
+    expect(body.permission_codes).toEqual(["finance.refund.create"]);
+    expect(new Date(String(body.expires_at)).getTime()).toBe(new Date("2026-12-31T18:00").getTime());
+    expect(body.single_use).toBe(true);
+    await waitFor(() => expect(screen.queryByTestId("access-grant-request-guided")).toBeNull());
+    expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^授权申请已提交：agr-new（待处理）/));
+  });
+
+  it("步内校验拦截：临时权限未填过期时间则不进确认、不提交", async () => {
+    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    });
+    vi.stubGlobal("fetch", fetchMock);
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(),
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessGovernancePage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
+    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
+    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 过期时间未填
+    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("过期时间");
+    expect(screen.queryByTestId("access-grant-request-guided-summary")).toBeNull();
+    expect(fetchMock.mock.calls.some(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET").toUpperCase() === "POST")).toBe(false);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.test.tsx b/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
index 70036e26..9ac75006 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.test.tsx
@@ -95,7 +95,7 @@ describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
   it("员工单独授予 / 显式拒绝有写入口：显式拒绝按目标用户提交（PGM-1.10.1）", async () => {
     const props = renderPage();
     await waitFor(() => expect(screen.getByTestId("access-user-override-form")).toBeTruthy());
-    fireEvent.change(screen.getByLabelText("覆盖用户 id"), { target: { value: "u-1" } });
+    fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { value: "u-1" } });
     fireEvent.change(screen.getByLabelText("覆盖权限码"), { target: { value: "access.role.assign" } });
     fireEvent.change(screen.getByLabelText("覆盖效果"), { target: { value: "deny" } });
     fireEvent.click(screen.getByTestId("access-user-override-set"));
@@ -183,3 +183,146 @@ describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
     expect(text).not.toMatch(/\buser\b/);
   });
 });
+
+// M038 回归：取值全中文（001）、未登记岗位（003）、恢复文案（004）、写动作失败提示与防双击（005）、表头列数（007）、职级 30 档下拉（008）、状态用词（009）。
+describe("M038 访问治理页回归", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  const M038_GRANTS = [
+    { id: "gr-a", request_type: "user_scope_change", target_user_id: "u-1", status: "applied" },
+    { id: "gr-b", request_type: "position_high_risk_release", target_user_id: "u-1", status: "pending" },
+    { id: "gr-c", request_type: "role_permission", target_user_id: "u-1", status: "rejected" },
+  ];
+  const M038_TEMPLATES = [
+    { id: "prt-x", position_code: "front_desk", staff_level_id: "lvl_m2_upper", role_id: "role-1", status: "active" },
+    { id: "prt-arch", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z" },
+    { id: "prt-del", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z", deleted_at: "2026-08-02T00:00:00Z", delete_review_id: "rv-1" },
+  ];
+  type Write = { url: string; body: Record<string, unknown> };
+
+  function renderM038(onWrite: (write: Write) => Promise<Response>) {
+    const writes: Write[] = [];
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() !== "GET") {
+        const write = { url, body: init?.body ? JSON.parse(String(init.body)) : {} };
+        writes.push(write);
+        return onWrite(write);
+      }
+      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
+      if (url.includes("grant-requests")) return Promise.resolve(envelope(M038_GRANTS));
+      if (url.includes("position-role-templates")) return Promise.resolve(envelope(M038_TEMPLATES));
+      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(),
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessGovernancePage {...props} />);
+    return { props, writes };
+  }
+
+  it("授权申请的三种状态与内部类型全部中文上屏（001）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-grant-request-gr-a")).toBeTruthy());
+    const text = screen.getByTestId("access-grant-requests-table").textContent ?? "";
+    for (const label of ["已生效", "待处理", "已驳回", "人员范围变更", "岗位高危权限释放", "角色权限"]) expect(text).toContain(label);
+    for (const code of ["applied", "user_scope_change", "position_high_risk_release", "role_permission"]) expect(text).not.toContain(code);
+    fireEvent.click(screen.getByTestId("access-grant-request-gr-a"));
+    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
+    expect(drawer).toContain("人员范围变更");
+    expect(drawer).toContain("已生效");
+    expect(drawer).not.toContain("applied");
+  });
+
+  it("审批不填意见时留痕为中文「通过」，结果提示为中文状态（001）", async () => {
+    const { props, writes } = renderM038(() => Promise.resolve(raw({ id: "gr-b", status: "applied" })));
+    await waitFor(() => expect(screen.getByTestId("access-grant-actions-gr-b")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-actions-gr-b"));
+    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-b")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-grant-approve-gr-b"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    expect(writes[0]!.url).toContain("/grant-requests/gr-b/review");
+    expect(writes[0]!.body.comment).toBe("通过");
+    expect(writes[0]!.body.operator_note).toBe("通过");
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("已生效")));
+  });
+
+  it("未映射的岗位码显示为「未登记岗位（岗位码）」（003）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-x")).toBeTruthy());
+    const cells = screen.getByTestId("access-position-role-template-prt-x").querySelectorAll("td");
+    expect(cells[1]?.textContent).toBe("未登记岗位（front_desk）");
+    expect(cells[2]?.textContent).toBe("M2·上");
+  });
+
+  it("已存档的按钮写「取消存档」、已删除的写「恢复」，不再写「停用模板」（004）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-arch")).toBeTruthy());
+    const buttonTexts = (rowId: string) => Array.from(screen.getByTestId(`access-position-role-template-${rowId}`).querySelectorAll("button")).map((button) => button.textContent);
+    expect(buttonTexts("prt-arch")).toContain("取消存档");
+    expect(buttonTexts("prt-del")).toContain("恢复");
+    expect(buttonTexts("prt-del")).not.toContain("取消存档");
+    expect(screen.getByTestId("access-position-role-templates-table").textContent).not.toContain("停用模板");
+  });
+
+  it("装配角色被后端拒绝时页面报出原因（005）", async () => {
+    const { props, writes } = renderM038(() => Promise.resolve(new Response(
+      JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPROVAL", message: "以下权限的授予需要审批：[finance.refund.create]" }),
+      { status: 403, headers: { "content-type": "application/json" } },
+    )));
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringMatching(/\S/)));
+    expect(props.setActionResult).not.toHaveBeenCalled();
+  });
+
+  it("提交未完成时再次点击不发第二笔（005）", async () => {
+    let release: (value: Response) => void = () => undefined;
+    const { writes } = renderM038(() => new Promise<Response>((resolve) => { release = resolve; }));
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    fireEvent.click(screen.getByTestId("access-role-assign"));
+    await waitFor(() => expect(writes).toHaveLength(1));
+    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(true));
+    release(raw({}));
+    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(false));
+    expect(writes.filter((write) => write.url.includes("/role-assignments"))).toHaveLength(1);
+  });
+
+  it("智能总结表表头列数等于数据行单元格数（007）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-grant-summary-position-ops_manager")).toBeTruthy());
+    const table = screen.getByTestId("access-grant-summary-table");
+    expect(table.querySelectorAll("thead th")).toHaveLength(table.querySelectorAll("tbody tr")[0]!.querySelectorAll("td").length);
+  });
+
+  it("职级是 30 档下拉，范围变更处另有「沿用原职级」（008）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-user-position-level")).toBeTruthy());
+    const options = Array.from((screen.getByTestId("access-user-position-level") as HTMLSelectElement).options);
+    expect(options).toHaveLength(30);
+    expect(options[0]!.value).toBe("lvl_m1_lower");
+    expect(options[0]!.textContent).toBe("M1·下");
+    expect(options[29]!.value).toBe("lvl_m10_upper");
+    expect(options[29]!.textContent).toBe("M10·上");
+    const scopeOptions = Array.from((screen.getByTestId("access-user-scope-level") as HTMLSelectElement).options);
+    expect(scopeOptions).toHaveLength(31);
+    expect(scopeOptions[0]!.textContent).toBe("沿用原职级");
+  });
+
+  it("岗位模板弹窗里的状态用词与表格同一套（009）", async () => {
+    renderM038(() => Promise.resolve(raw({})));
+    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
+    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
+    fireEvent.click(screen.getByTestId("access-position-role-template-create-open")); // 引导弹窗为单页填写，状态下拉就在第一页
+    const status = await waitFor(() => screen.getByTestId("access-position-role-template-status") as HTMLSelectElement);
+    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "未激活"]);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessGovernancePage.tsx b/apps/web/src/admin/pages/AccessGovernancePage.tsx
index 55ed4928..1e3b6545 100644
--- a/apps/web/src/admin/pages/AccessGovernancePage.tsx
+++ b/apps/web/src/admin/pages/AccessGovernancePage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -49,7 +49,28 @@ const POSITION_LABELS: Record<string, string> = {
 };
 function zhPosition(code?: string | null): string {
   if (!code) return "—";
-  return POSITION_LABELS[code] ?? code;
+  // M038-003：未映射的岗位码不裸露英文——中文说明在前，岗位码作为系统内部标识符放括号里。
+  return POSITION_LABELS[code] ?? `未登记岗位（${code}）`;
+}
+// M038-008：职级是速查表写明的 30 档闭集（M1–M10 × 上/中/下），编号与库内 staff_levels 一致；页内生成下拉，不手填。
+const STAFF_LEVEL_IDS: string[] = [];
+for (let main = 1; main <= 10; main += 1) {
+  for (const band of ["lower", "middle", "upper"]) STAFF_LEVEL_IDS.push(`lvl_m${main}_${band}`);
+}
+function staffLevelOptions() {
+  return STAFF_LEVEL_IDS.map((id) => <option key={id} value={id}>{zhLevel(id)}</option>);
+}
+// M038-001：授权申请的状态与类型按后端闭集在页内给全中文（公共字典缺「已生效」与三种内部类型）。
+const GRANT_STATUS_LABELS: Record<string, string> = { pending: "待处理", applied: "已生效", rejected: "已驳回" };
+const GRANT_TYPE_LABELS: Record<string, string> = {
+  user_role: "用户角色", user_position: "用户职位", temporary_permission: "临时权限",
+  role_permission: "角色权限", user_scope_change: "人员范围变更", position_high_risk_release: "岗位高危权限释放",
+};
+function grantStatusLabel(code?: string | null): string {
+  return code ? GRANT_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
+}
+function grantTypeLabel(code?: string | null): string {
+  return code ? GRANT_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
 }
 function zhLevel(code?: string | null): string {
   if (!code) return "—";
@@ -81,14 +102,31 @@ function roleLabel(row: RoleRow | undefined) {
   return row ? `${row.roleName ?? row.roleCode ?? row.id} (${row.id})` : "-";
 }
 
-function statusBadge(status?: string | null) {
+function statusBadge(status?: string | null, label: (code: string) => string = formatBusinessStatus) {
   const value = status || "";
   const kind = value === "approved" || value === "active" ? "is-ok" : value === "rejected" ? "is-danger" : value === "pending" ? "is-warn" : "is-info";
   // 徽标文字中文化（业务HTML零英文规则）；颜色仍按原始码判定。
-  return <span className={`admin-badge ${kind}`}>{value ? formatBusinessStatus(value) : "—"}</span>;
+  return <span className={`admin-badge ${kind}`}>{value ? label(value) : "—"}</span>;
 }
 
 export function AccessGovernancePage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
+  // M038-005：页内写动作统一走 runWrite——提交中置忙（防双击发两笔）、失败把后端中文消息报到页面。
+  const [writeBusy, setWriteBusy] = React.useState(false);
+  const writeBusyRef = React.useRef(false);
+  async function runWrite(action: () => Promise<void>, fallbackMessage: string) {
+    if (writeBusyRef.current) return;
+    writeBusyRef.current = true;
+    setWriteBusy(true);
+    setGlobalError(null);
+    try {
+      await action();
+    } catch (actionError) {
+      setGlobalError(actionError instanceof Error ? actionError.message : fallbackMessage);
+    } finally {
+      writeBusyRef.current = false;
+      setWriteBusy(false);
+    }
+  }
   const [users, setUsers] = React.useState<AccessUserRow[]>([]);
   const [roles, setRoles] = React.useState<RoleRow[]>([]);
   const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
@@ -371,7 +409,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
         reason: grantForm.reason.trim() || "后台高危授权申请",
         operatorNote: grantForm.reason.trim() || "后台高危授权申请",
       });
-      setActionResult(`授权申请已提交：${result.id}（${formatBusinessStatus(result.status ?? "")}）`);
+      setActionResult(`授权申请已提交：${result.id}（${grantStatusLabel(result.status)}）`);
       setGrantOpen(false);
       await reload();
     } catch (actionError) {
@@ -386,11 +424,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
   async function reviewGrant(row: AccessGrantRequestRow, decision: "approve" | "reject") {
     const result = await api.post<AccessGrantRequestRow>(`/api/v1/access/grant-requests/${encodeURIComponent(row.id)}/review`, {
       decision,
-      comment: reviewForm.comment.trim() || decision,
-      operatorNote: reviewForm.comment.trim() || decision,
+      // M038-001：不填意见时留痕用中文，不把 approve／reject 当意见。
+      comment: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
+      operatorNote: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
       evidenceAttachmentIds: splitTokens(reviewForm.evidenceAttachmentIds),
     });
-    setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通过" : "驳回"}（${formatBusinessStatus(result.status ?? "")}）`);
+    setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通过" : "驳回"}（${grantStatusLabel(result.status)}）`);
     await reload();
   }
 
@@ -415,12 +454,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
       title: `授权申请 · ${row.id}`,
       fields: [
         { label: "申请编号", value: row.id },
-        { label: "类型", value: formatBusinessStatus(row.requestType) },
+        { label: "类型", value: grantTypeLabel(row.requestType) },
         { label: "目标用户", value: row.targetUserId },
         { label: "角色", value: row.targetRoleId ?? "—" },
         { label: "岗位", value: row.positionCode ? `${zhPosition(row.positionCode)} / ${zhLevel(row.staffLevelId)}` : "—" },
         { label: "权限码", value: row.permissionCodes?.length ? row.permissionCodes.join("、") : "—" },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: grantStatusLabel(row.status) },
         { label: "单次有效", value: row.singleUse ? "是" : "否" },
         { label: "过期时间", value: cnDate(row.expiresAt) },
         { label: "申请原因", value: row.reason ?? "—" },
@@ -479,12 +518,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
                 <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{row.deletedAt ? "已删除（可恢复）" : row.archivedAt ? "已存档" : statusBadge(row.status)}</td>
                 <td onClick={(event) => event.stopPropagation()}>
                   <button type="button" data-testid={`access-position-role-template-select-${row.id}`} disabled={Boolean(row.archivedAt)} onClick={() => selectTemplate(row)}>选中</button>
-                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} onClick={() => void changePositionLifecycle(row.id, "archive")}>存档</PermissionButton> : <>
-                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "request_delete")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
+                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "archive"), "岗位模板处理失败")}>存档</PermissionButton> : <>
+                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "request_delete"), "岗位模板处理失败")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
                     {!row.deletedAt && row.deleteReviewId ? <>
-                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "approve_delete")}>最高权限批准删除</PermissionButton>
-                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} onClick={() => void changePositionLifecycle(row.id, "reject_delete")}>驳回删除</PermissionButton>
-                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} onClick={() => void changePositionLifecycle(row.id, "restore")}>恢复为停用模板</PermissionButton>}
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "approve_delete"), "岗位模板处理失败")}>最高权限批准删除</PermissionButton>
+                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "reject_delete"), "岗位模板处理失败")}>驳回删除</PermissionButton>
+                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "restore"), "岗位模板处理失败")}>{row.deletedAt ? "恢复" : "取消存档"}</PermissionButton>}
                   </>}
                 </td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位角色模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td></tr>
@@ -503,48 +542,48 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
 
         <h3>给用户定职位自动适配</h3>
         <div className="admin-inline-form" data-testid="access-user-position-form">
-          <select aria-label="用户 id" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="用户" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
           <input aria-label="岗位编码" placeholder="岗位编码" value={positionForm.positionCode} onChange={(event) => setPositionForm({ ...positionForm, positionCode: event.target.value })} />
-          <input aria-label="职级 id" placeholder="职级 id" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })} />
+          <select aria-label="职级" data-testid="access-user-position-level" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select>
           <input aria-label="基础薪资元" type="number" min="0" placeholder="基础薪资(元)" value={positionForm.baseSalaryYuan} onChange={(event) => setPositionForm({ ...positionForm, baseSalaryYuan: event.target.value })} />
           <input aria-label="薪资备注" placeholder="薪资备注" value={positionForm.salaryDetail} onChange={(event) => setPositionForm({ ...positionForm, salaryDetail: event.target.value })} />
           <input aria-label="操作备注" placeholder="操作备注" value={positionForm.operatorNote} onChange={(event) => setPositionForm({ ...positionForm, operatorNote: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" onClick={() => void assignPosition()}>定职位并适配</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" disabled={writeBusy} onClick={() => void runWrite(assignPosition, "定岗失败")}>定职位并适配</PermissionButton>
         </div>
 
         <h3>人员范围变更（所属商户 / 跨项目）</h3>
         <div className="admin-inline-form" data-testid="access-user-scope-form">
-          <select aria-label="变更用户 id" data-testid="access-user-scope-user" value={scopeForm.userId || positionForm.userId} onChange={(event) => setScopeForm({ ...scopeForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
-          <input aria-label="新所属项目 id" data-testid="access-user-scope-org" placeholder="新所属项目 id（不变可空）" value={scopeForm.organizationId} onChange={(event) => setScopeForm({ ...scopeForm, organizationId: event.target.value })} />
-          <input aria-label="新所属商户 id" data-testid="access-user-scope-merchant" placeholder="新所属商户 id（平台员工留空）" value={scopeForm.merchantId} onChange={(event) => setScopeForm({ ...scopeForm, merchantId: event.target.value })} />
+          <select aria-label="变更用户" data-testid="access-user-scope-user" value={scopeForm.userId || positionForm.userId} onChange={(event) => setScopeForm({ ...scopeForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <input aria-label="新所属项目编号" data-testid="access-user-scope-org" placeholder="新所属项目编号（不变可空）" value={scopeForm.organizationId} onChange={(event) => setScopeForm({ ...scopeForm, organizationId: event.target.value })} />
+          <input aria-label="新所属商户编号" data-testid="access-user-scope-merchant" placeholder="新所属商户编号（平台员工留空）" value={scopeForm.merchantId} onChange={(event) => setScopeForm({ ...scopeForm, merchantId: event.target.value })} />
           <input aria-label="新范围岗位编码" data-testid="access-user-scope-position" placeholder="新范围岗位编码（沿用可空）" value={scopeForm.positionCode} onChange={(event) => setScopeForm({ ...scopeForm, positionCode: event.target.value })} />
-          <input aria-label="新范围职级 id" data-testid="access-user-scope-level" placeholder="新范围职级 id（沿用可空）" value={scopeForm.staffLevelId} onChange={(event) => setScopeForm({ ...scopeForm, staffLevelId: event.target.value })} />
+          <select aria-label="新范围职级" data-testid="access-user-scope-level" value={scopeForm.staffLevelId} onChange={(event) => setScopeForm({ ...scopeForm, staffLevelId: event.target.value })}><option value="">沿用原职级</option>{staffLevelOptions()}</select>
           <input aria-label="变更理由" data-testid="access-user-scope-reason" placeholder="变更理由" value={scopeForm.reason} onChange={(event) => setScopeForm({ ...scopeForm, reason: event.target.value })} />
           <input aria-label="主线最终审批人" data-testid="access-user-scope-final-approver" placeholder="主线最终审批人账号（可空）" value={scopeForm.finalApproverId} onChange={(event) => setScopeForm({ ...scopeForm, finalApproverId: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-scope-change" onClick={() => void changeUserScope()}>提交范围变更（先审批后生效）</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-scope-change" disabled={writeBusy} onClick={() => void runWrite(changeUserScope, "范围变更提交失败")}>提交范围变更（先审批后生效）</PermissionButton>
         </div>
         <div className="admin-muted-action" data-testid="access-user-scope-boundary">保存所属商户或跨项目变更时，旧岗位或旧范围派生权限立即失效；新范围权限每次均须先审批，审批通过后方可生效；模板内既有高危权限不因在模板内而免审。</div>
 
         <h3>直接装配角色</h3>
         <div className="admin-inline-form" data-testid="access-role-assignment-form">
-          <select aria-label="装配用户 id" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
-          <select aria-label="装配角色 id" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
+          <select aria-label="装配用户" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="装配角色" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
           <input aria-label="装配备注" placeholder="操作备注" value={roleForm.operatorNote} onChange={(event) => setRoleForm({ ...roleForm, operatorNote: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" onClick={() => void roleAssignment("assign")}>装配角色</PermissionButton>
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" onClick={() => void roleAssignment("unassign")}>卸载角色</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("assign"), "装配角色失败")}>装配角色</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("unassign"), "卸载角色失败")}>卸载角色</PermissionButton>
         </div>
 
         <h3>员工单独授予 / 显式拒绝</h3>
         <div className="admin-inline-form" data-testid="access-user-override-form">
-          <select aria-label="覆盖用户 id" value={overrideForm.userId} onChange={(event) => setOverrideForm({ ...overrideForm, userId: event.target.value })}><option value="">请选择用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
+          <select aria-label="覆盖用户" value={overrideForm.userId} onChange={(event) => setOverrideForm({ ...overrideForm, userId: event.target.value })}><option value="">请选择用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
           <input aria-label="覆盖权限码" placeholder="权限码，逗号分隔" value={overrideForm.permissionCodes} onChange={(event) => setOverrideForm({ ...overrideForm, permissionCodes: event.target.value })} />
           <select aria-label="覆盖效果" value={overrideForm.effect} onChange={(event) => setOverrideForm({ ...overrideForm, effect: event.target.value })}>
             <option value="deny">显式拒绝</option>
             <option value="grant">单独授予</option>
           </select>
           <input aria-label="覆盖理由" placeholder="理由" value={overrideForm.reason} onChange={(event) => setOverrideForm({ ...overrideForm, reason: event.target.value })} />
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-set" onClick={() => void userPermissionOverride("set")}>设置</PermissionButton>
-          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-remove" onClick={() => void userPermissionOverride("remove")}>移除</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-set" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("set"), "员工级覆盖设置失败")}>设置</PermissionButton>
+          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-remove" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("remove"), "员工级覆盖移除失败")}>移除</PermissionButton>
         </div>
 
           </>) },
@@ -569,12 +608,12 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
             <thead><tr><th>申请</th><th>类型</th><th>目标</th><th>角色/岗位</th><th>权限码</th><th>状态</th><th>过期</th><th>操作</th><th>详情</th></tr></thead>
             <tbody>{grantRequests.map((row) => (
               <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-request-${row.id}`} onClick={() => openGrantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openGrantDetail(row); } }}>
-                <td>{row.id}</td><td>{formatBusinessStatus(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status)}</td><td>{cnDate(row.expiresAt)}</td>
+                <td>{row.id}</td><td>{grantTypeLabel(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status, grantStatusLabel)}</td><td>{cnDate(row.expiresAt)}</td>
                 <td onClick={(event) => event.stopPropagation()}>{(() => {
                   const acts: RowAction[] = [];
                   if (row.status === "pending") {
-                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void reviewGrant(row, "approve"), testId: `access-grant-approve-${row.id}` });
-                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void reviewGrant(row, "reject"), testId: `access-grant-reject-${row.id}` });
+                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void runWrite(() => reviewGrant(row, "approve"), "授权审批失败"), testId: `access-grant-approve-${row.id}` });
+                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void runWrite(() => reviewGrant(row, "reject"), "授权审批失败"), testId: `access-grant-reject-${row.id}` });
                   }
                   return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`access-grant-actions-${row.id}`} />;
                 })()}</td>
@@ -609,7 +648,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
                   [risk]: { ...grantPolicy.riskReviews[risk], requiredLevel: Number(event.target.value) } } })} /></label>
             </fieldset>)}
             <label>修改理由<input data-testid="grant-policy-reason" value={grantPolicyReason} onChange={(event) => setGrantPolicyReason(event.target.value)} /></label>
-            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" onClick={() => void saveGrantPolicy()}>保存授权政策</PermissionButton>
+            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" disabled={writeBusy} onClick={() => void runWrite(saveGrantPolicy, "授权政策保存失败")}>保存授权政策</PermissionButton>
           </> : null}
         </details>
 
@@ -617,7 +656,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
         {summaryNotice ? <div className="admin-state" data-testid="access-grant-summary-degraded">{summaryNotice}</div> : null}
         <div className="admin-table-scroll">
           <table className="admin-table" data-testid="access-grant-summary-table">
-            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th><th>详情</th></tr></thead>
+            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th></tr></thead>
             <tbody>
               {(summary?.positionRecommendations ?? []).map((row) => <tr key={`position-${row.positionCode}-${row.permissionCode}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-position-${row.positionCode}`} onClick={() => openSummaryPositionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryPositionDetail(row); } }}><td>岗位</td><td>{zhPosition(row.positionCode)} / {zhLevel(row.staffLevelId)}</td><td>{row.permissionCode ?? "-"}</td><td>{row.requestCount}</td><td>{row.recommendation ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位授权建议详情" onClick={(event) => { event.stopPropagation(); openSummaryPositionDetail(row); }}>查看详情</button></td></tr>)}
               {(summary?.userAnomalies ?? []).map((row) => <tr key={`user-${row.userId}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-user-${row.userId}`} onClick={() => openSummaryUserDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryUserDetail(row); } }}><td>用户</td><td>{row.userId}</td><td>-</td><td>{row.requestCount}</td><td>{row.summary ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看用户授权异常详情" onClick={(event) => { event.stopPropagation(); openSummaryUserDetail(row); }}>查看详情</button></td></tr>)}
@@ -652,16 +691,16 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
             content: (
               <>
                 <label>岗位编码<input data-testid="access-position-role-template-position" aria-label="岗位编码" placeholder="岗位编码" value={templateForm.positionCode} onChange={(event) => setTemplateForm({ ...templateForm, positionCode: event.target.value })} /></label>
-                <label>职级 id<input data-testid="access-position-role-template-level" aria-label="职级 id" placeholder="职级 id" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })} /></label>
-                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色 id" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
-                <label>权限模板 id<input data-testid="access-position-role-template-template-id" aria-label="权限模板 id" placeholder="权限模板 id" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
+                <label>职级<select data-testid="access-position-role-template-level" aria-label="职级" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
+                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
+                <label>权限模板编号<input data-testid="access-position-role-template-template-id" aria-label="权限模板编号" placeholder="权限模板编号" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
               </>
             ),
           },
           {
             title: "状态",
             content: (
-              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">启用</option><option value="inactive">停用</option></select></label>
+              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">{formatBusinessStatus("active")}</option><option value="inactive">{formatBusinessStatus("inactive")}</option></select></label>
             ),
           },
         ]}
@@ -671,7 +710,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
           { label: "职级", value: templateForm.staffLevelId.trim() || "未填写" },
           { label: "角色", value: templateForm.roleId ? roleLabel(roles.find((r) => r.id === templateForm.roleId)) : "不绑定" },
           { label: "权限模板", value: templateForm.templateId.trim() || "—" },
-          { label: "状态", value: templateForm.status === "active" ? "启用" : "停用" },
+          { label: "状态", value: formatBusinessStatus(templateForm.status) },
         ]}
       />
       <GuidedFlowModal
@@ -713,7 +752,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
               <>
                 <label>申请角色<select aria-label="申请角色" value={grantForm.roleId} onChange={(event) => setGrantForm({ ...grantForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                 <label>申请岗位<input aria-label="申请岗位" placeholder="岗位编码" value={grantForm.positionCode} onChange={(event) => setGrantForm({ ...grantForm, positionCode: event.target.value })} /></label>
-                <label>申请职级<input aria-label="申请职级" placeholder="职级 id" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })} /></label>
+                <label>申请职级<select aria-label="申请职级" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
                 <label>临时权限码<input aria-label="临时权限码" placeholder="权限码，逗号分隔" value={grantForm.permissionCodes} onChange={(event) => setGrantForm({ ...grantForm, permissionCodes: event.target.value })} /></label>
                 <label>过期时间<input aria-label="过期时间" type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} /></label>
                 <label><input type="checkbox" checked={grantForm.singleUse} onChange={(event) => setGrantForm({ ...grantForm, singleUse: event.target.checked })} />单次有效</label>
@@ -723,7 +762,7 @@ export function AccessGovernancePage({ api, user, setActionResult, setGlobalErro
           },
         ]}
         summary={[
-          { label: "申请类型", value: formatBusinessStatus(grantForm.requestType) },
+          { label: "申请类型", value: grantTypeLabel(grantForm.requestType) },
           { label: "目标用户", value: grantForm.targetUserId.trim() || "未选择" },
           { label: "角色", value: grantForm.requestType === "user_role" ? (grantForm.roleId ? roleLabel(roles.find((r) => r.id === grantForm.roleId)) : "未选择") : "—" },
           { label: "岗位/职级", value: grantForm.requestType === "user_position" ? `${grantForm.positionCode.trim() || "—"} / ${grantForm.staffLevelId.trim() || "—"}` : "—" },
diff --git a/apps/web/src/admin/pages/AccessUsersPage.test.tsx b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
index 438888d2..ac386bfc 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.test.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -90,3 +90,47 @@ describe("AccessUsersPage 行明细抽屉 + 零英文", () => {
     expect(text).not.toContain("disabled");
   });
 });
+
+// M038-001（开通方式中文）、M038-009（初始状态下拉与表格同一套用词）。
+describe("M038 员工账号页回归", () => {
+  afterEach(() => vi.unstubAllGlobals());
+
+  it("账号详情的开通方式为中文：历史账号 / 后台手动录入（001）", async () => {
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("access/users")) return Promise.resolve(envelope([
+        { id: "u-legacy", username: "老账号", account_type: "staff", status: "active", created_via: "legacy" },
+        { id: "u-manual", username: "外协甲", account_type: "outsourced_vendor", status: "active", created_via: "manual_admin" },
+      ]));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = {
+      api: new AdminApi("test-token"),
+      user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(),
+      setGlobalError: vi.fn(),
+      effectivePermissions: ADMIN_PERMS,
+    };
+    render(<AccessUsersPage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("access-user-row-u-legacy")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-user-row-u-legacy"));
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("历史账号");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("legacy");
+    fireEvent.keyDown(window, { key: "Escape" });
+    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
+    fireEvent.click(screen.getByTestId("access-user-row-u-manual"));
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("后台手动录入");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("manual_admin");
+  });
+
+  it("新建外部协作账号的初始状态下拉与账号表同一套用词（009）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("access-user-create-open"));
+    fireEvent.change(screen.getByTestId("access-user-account-type"), { target: { value: "outsourced_vendor" } });
+    fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "vendor-a" } });
+    fireEvent.change(screen.getByTestId("access-user-password"), { target: { value: "init-pass" } });
+    const status = await waitFor(() => screen.getByTestId("access-user-status") as HTMLSelectElement);
+    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "待激活", "已禁用"]);
+  });
+});
diff --git a/apps/web/src/admin/pages/AccessUsersPage.tsx b/apps/web/src/admin/pages/AccessUsersPage.tsx
index a0e196a2..6555b949 100644
--- a/apps/web/src/admin/pages/AccessUsersPage.tsx
+++ b/apps/web/src/admin/pages/AccessUsersPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -15,6 +15,12 @@ function templateLabel(template: PermissionTemplateRow) {
   return template.templateName ?? template.name ?? template.id;
 }
 
+// M038-001：开通方式按后端现有取值在页内给中文（公共字典无这两个键）。
+const CREATED_VIA_LABELS: Record<string, string> = { manual_admin: "后台手动录入", legacy: "历史账号" };
+function createdViaLabel(code: string): string {
+  return CREATED_VIA_LABELS[code] ?? formatBusinessStatus(code);
+}
+
 // ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
 function shortId(id: string | null | undefined): string {
   if (!id) return "-";
@@ -92,7 +98,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
       return false;
     }
     if (!organizationId.trim()) {
-      setFormError("组织 ID 不能为空，用于数据范围和留痕。 ");
+      setFormError("所属组织不能为空，用于数据范围和留痕。 ");
       return false;
     }
     setFormError("");
@@ -179,7 +185,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
         { label: "组织", value: orgDisplay(row.organizationId) },
         { label: "商户", value: merchantDisplay(row.merchantId) },
         { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
-        { label: "开通方式", value: row.createdVia ? formatBusinessStatus(row.createdVia) : "—" },
+        { label: "开通方式", value: row.createdVia ? createdViaLabel(row.createdVia) : "—" },
         { label: "授权凭据", value: row.consentReference ?? "—" },
         { label: "证据附件", value: row.evidenceAttachmentIds?.length ? row.evidenceAttachmentIds.join("、") : "—" },
       ],
@@ -319,14 +325,14 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
           {
             title: "归属与授权",
             description: accountType === "outsourced_vendor" ? "指定账号归属的组织/商户，并留存授权凭据。" : "指定员工归属的组织与商户。",
-            validate: () => (organizationId.trim() ? null : "组织 ID 不能为空，用于数据范围和留痕。"),
+            validate: () => (organizationId.trim() ? null : "所属组织不能为空，用于数据范围和留痕。"),
             content: (
               <>
-                <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织 ID" /></label>
+                <label>所属组织<input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织编号" /></label>
                 <label>所属商户<select data-testid="access-user-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)}><option value="">不绑定 / 平台账号</option>{merchants.map((m) => <option key={m.id} value={m.id}>{(m.displayName ?? m.companyName ?? m.id)}（{shortId(m.id)}）</option>)}</select></label>
                 {accountType === "outsourced_vendor" ? <>
                 <label>授权凭据<input data-testid="access-user-consent" value={consentReference} onChange={(event) => setConsentReference(event.target.value)} placeholder="同意/授权凭据编号（可选）" /></label>
-                <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件 ID，逗号分隔（可选）" /></label>
+                <label>证据附件<input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="附件编号，逗号分隔（可选）" /></label>
                 </> : null}
               </>
             ),
@@ -338,9 +344,7 @@ export function AccessUsersPage({ api, setActionResult, setGlobalError, effectiv
               <>
                 {accountType !== "outsourced_vendor" ? <p>注册申请提交后，请在员工档案中完成公司审批；此处不提前生成可用账号或发放权限。</p> : <>
                 <label>初始状态<select data-testid="access-user-status" value={status} onChange={(event) => setStatus(event.target.value)}>
-                  <option value="active">启用</option>
-                  <option value="invited">待激活</option>
-                  <option value="disabled">禁用</option>
+                  {["active", "invited", "disabled"].map((value) => <option key={value} value={value}>{formatBusinessStatus(value)}</option>)}
                 </select></label>
                 <label>权限模板<select data-testid="access-user-template" value={selectedTemplateId} onChange={(event) => setSelectedTemplateId(event.target.value)}>
                   <option value="">暂不赋权</option>
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
index ffd51a28..266a7f57 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
@@ -92,3 +92,62 @@ describe("PermissionTemplatesPage 行明细抽屉 + 零英文", () => {
     expect(text).not.toContain("inactive");
   });
 });
+
+// M038-001（模板类型 role 中文）、M038-006（编辑保存的时间往返）。
+describe("M038 权限模板页回归", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.unstubAllEnvs();
+  });
+
+  const TIMED = [
+    { id: "tpl-role", template_name: "平台管理员模板", template_type: "role", status: "active", permission_codes: ["access.user.manage"],
+      valid_from: "2026-06-14T00:00:00Z", valid_until: "2026-06-15T00:00:00Z" },
+  ];
+
+  it("库内类型 role 显示为「角色模板」，编辑下拉也有这一项（001）", async () => {
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
+      const url = String(input);
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
+      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
+    render(<PermissionTemplatesPage {...props} />);
+    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-role")).toBeTruthy());
+    const cells = screen.getByTestId("permission-template-row-tpl-role").querySelectorAll("td");
+    expect(cells[2]?.textContent).toBe("角色模板");
+    const editType = screen.getByTestId("permission-template-edit-type") as HTMLSelectElement;
+    await waitFor(() => expect(editType.value).toBe("role"));
+    expect(Array.from(editType.options).map((option) => option.textContent)).toContain("角色模板");
+  });
+
+  it("北京时间下选中带生效/截止时间的模板原样保存，发出的仍是同一时刻（006）", async () => {
+    vi.stubEnv("TZ", "Asia/Shanghai");
+    expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); // 时区确已生效，否则本用例无意义
+    let putBody: Record<string, unknown> | null = null;
+    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "PUT" && url.includes("/permission-templates/tpl-role")) {
+        putBody = JSON.parse(String(init?.body));
+        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-role" } }),
+          { status: 200, headers: { "content-type": "application/json" } }));
+      }
+      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
+      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
+      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
+      return Promise.resolve(envelope([]));
+    }));
+    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
+      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
+    render(<PermissionTemplatesPage {...props} />);
+    await waitFor(() => expect((screen.getByTestId("permission-template-edit-valid-from") as HTMLInputElement).value).toBe("2026-06-14T08:00"));
+    expect((screen.getByTestId("permission-template-edit-valid-until") as HTMLInputElement).value).toBe("2026-06-15T08:00");
+    fireEvent.click(screen.getByTestId("permission-template-update"));
+    await waitFor(() => expect(putBody).toBeTruthy());
+    expect(new Date(String(putBody!.valid_from)).getTime()).toBe(new Date("2026-06-14T00:00:00Z").getTime());
+    expect(new Date(String(putBody!.valid_until)).getTime()).toBe(new Date("2026-06-15T00:00:00Z").getTime());
+  });
+});
diff --git a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
index 09e1db4a..47478fa3 100644
--- a/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
+++ b/apps/web/src/admin/pages/PermissionTemplatesPage.tsx
@@ -15,7 +15,7 @@ const TEMPLATE_MANAGE_PERMISSION = "access.permission_template.manage";
 const TEMPLATE_APPLY_PERMISSION = "access.permission_template.apply";
 
 // 模板类型枚举中文化（base/temporary/copy 不进全局表：base 全局义为“普通会员”会串味，页内小映射）。
-const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板" };
+const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板", role: "角色模板" };
 function templateTypeLabel(type?: string | null) {
   const value = type ?? "base";
   return TEMPLATE_TYPE_LABELS[value] ?? value;
@@ -39,11 +39,13 @@ function displayTemplateName(row: PermissionTemplateRow) {
   return row.templateName ?? row.name ?? row.templateCode ?? row.id;
 }
 
+// M038-006：datetime-local 输入框按本地时区解释，这里也按本地时区取年月日时分，「读出→不改→保存」才是同一时刻。
 function toDateTimeLocal(value?: string | null) {
   if (!value) return "";
   const date = new Date(value);
   if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
-  return date.toISOString().slice(0, 16);
+  const pad = (part: number) => String(part).padStart(2, "0");
+  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
 }
 
 function toIsoOrUndefined(value: string) {
@@ -312,7 +314,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
       return false;
     }
     if (parseTargetUsers(targetUsersText).length === 0) {
-      setFormError("至少输入一个目标员工 ID。");
+      setFormError("至少输入一个目标员工编号。");
       return false;
     }
     setFormError("");
@@ -376,6 +378,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
               <option value="base">基础模板</option>
               <option value="temporary">临时模板</option>
               <option value="copy">复制模板</option>
+              <option value="role">角色模板</option>
             </select>
           </label>
           <label>生效时间
@@ -410,7 +413,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
           >
             复制模板
           </PermissionButton>
-          <label>目标员工 ID
+          <label>目标员工编号
             <input data-testid="permission-template-target-users" value={targetUsersText} onChange={(event) => setTargetUsersText(event.target.value)} placeholder="多个用逗号分隔" />
           </label>
           <HighRiskConfirm
@@ -496,6 +499,7 @@ export function PermissionTemplatesPage({ api, setActionResult, setGlobalError,
                     <option value="base">基础模板</option>
                     <option value="temporary">临时模板</option>
                     <option value="copy">复制模板</option>
+                    <option value="role">角色模板</option>
                   </select>
                 </label>
                 <label>生效时间<input data-testid="permission-template-valid-from" type="datetime-local" value={newValidFrom} onChange={(event) => setNewValidFrom(event.target.value)} /></label>
diff --git a/apps/web/src/admin/security/PermissionGate.test.ts b/apps/web/src/admin/security/PermissionGate.test.ts
index 679f9034..4b865fbf 100644
--- a/apps/web/src/admin/security/PermissionGate.test.ts
+++ b/apps/web/src/admin/security/PermissionGate.test.ts
@@ -1,7 +1,10 @@
-import { describe, expect, it } from "vitest";
+import { fireEvent, render, screen } from "@testing-library/react";
+import { createElement } from "react";
+import { describe, expect, it, vi } from "vitest";
 
 import type { EffectivePermissions, EffectivePermissionsState } from "../types";
-import { hasPermissions } from "./PermissionGate";
+import { Can, PermissionButton } from "./Can";
+import { hasPermissions, PermissionGate } from "./PermissionGate";
 
 function ready(value: Partial<EffectivePermissions>): EffectivePermissionsState {
   return {
@@ -59,3 +62,52 @@ describe("hasPermissions", () => {
     expect(hasPermissions(ready({ wildcard: true }), [])).toBe(true);
   });
 });
+
+// M038-010：权限闸组件的渲染分支（此前只测了纯函数 hasPermissions）。本文件是 .ts，用 createElement 渲染。
+describe("PermissionButton / Can / PermissionGate 组件", () => {
+  const denied = ready({ grants: ["access.role.view"] });
+  const allowed = ready({ grants: ["access.role.assign"] });
+
+  it("无权限：按钮禁用、带提示，点击不触发；有权限：点击触发", () => {
+    const onClick = vi.fn();
+    const view = render(createElement(PermissionButton, { permissionsState: denied, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
+    const button = screen.getByTestId("gate-btn") as HTMLButtonElement;
+    expect(button.disabled).toBe(true);
+    expect(button.title).toBe("当前账号无此操作权限");
+    fireEvent.click(button);
+    expect(onClick).not.toHaveBeenCalled();
+    view.rerender(createElement(PermissionButton, { permissionsState: allowed, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
+    expect((screen.getByTestId("gate-btn") as HTMLButtonElement).disabled).toBe(false);
+    fireEvent.click(screen.getByTestId("gate-btn"));
+    expect(onClick).toHaveBeenCalledTimes(1);
+    view.unmount();
+  });
+
+  it("显式拒绝压过授予：按钮仍禁用", () => {
+    const onClick = vi.fn();
+    const view = render(createElement(PermissionButton, { permissionsState: ready({ grants: ["access.role.assign"], denies: ["access.role.assign"] }), permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn-deny" , children: "装配角色" }));
+    fireEvent.click(screen.getByTestId("gate-btn-deny"));
+    expect((screen.getByTestId("gate-btn-deny") as HTMLButtonElement).disabled).toBe(true);
+    expect(onClick).not.toHaveBeenCalled();
+    view.unmount();
+  });
+
+  it("Can：无权限渲染 fallback，有权限渲染子内容", () => {
+    const view = render(createElement(Can, { permissionsState: denied, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
+    expect(view.container.textContent).toBe("无权查看");
+    view.rerender(createElement(Can, { permissionsState: allowed, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
+    expect(view.container.textContent).toBe("敏感内容");
+    view.unmount();
+  });
+
+  it("PermissionGate：加载中、权限不足、放行三种渲染", () => {
+    const view = render(createElement(PermissionGate, { permissionsState: { status: "loading" } as EffectivePermissionsState, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(screen.getByTestId("admin-permission-loading").textContent).toContain("权限加载中");
+    view.rerender(createElement(PermissionGate, { permissionsState: denied, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(screen.getByTestId("admin-permission-denied").textContent).toContain("权限不足");
+    expect(view.container.textContent).not.toContain("页面内容");
+    view.rerender(createElement(PermissionGate, { permissionsState: allowed, permissions: ["access.role.assign"] , children: "页面内容" }));
+    expect(view.container.textContent).toBe("页面内容");
+    view.unmount();
+  });
+});

```
