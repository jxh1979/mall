CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 B5-r2 修复后整模块三线全量重审（Grok 席乙）

- 文件自身版本：V0.01（指定路径原先不存在；本席同轮先写后发现草稿污染，以本完整正文覆盖同一份 V0.01，不另起版本、不保留污染段）
- 席位：固定 Grok 独立只读外审席乙（grok-4.6 / xhigh）
- 步骤：B5-r2（B3 累计 2 之后的整模块重审）。按 160-audit-prompt.md V0.17 的 B1 三线跑完整模块，不只看 diff，不复用旧票。
- 正本：`/home/james/complex-management` main 主工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。`origin/main`=`53dec14170f01a6b8ebb1b7beb34f5aaf10da2fb`（未 fetch）。派发所称 after tree `19ab58e07b9686054d8f4fd2fb0f279b727dc34a` 为规划标签；行数与 SHA 以磁盘实算为准。origin/main 一次性 worktree 依次 `git apply` `/home/james/audit-tmp/m038_b3r1.diff` 与 `/home/james/audit-tmp/m038_b3r2.diff` 后，16 个受审文件与主工作区磁盘全文逐文件 MATCH。
- 模块表：`docs/audit-2026-09/plan/modules.md` V0.04，M038 聚合「功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段」，16 文件。
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.14。本席独立 `sha256sum` 得上列独占行。sidecar `plan/criteria-lock.sha256` 文本为同一 64 hex。`LOCK-AMEND-COUNT` 1。映射内定义变更项 BSM-114、BSM-120、PGM-043、PGM-059 按现行正文重审。
- findings：`docs/audit-2026-09/findings.md` V3.90（本席实算 SHA256 `7e4d46723c5ad0b0334f53f0d18e9242fee767d882c08c4cf74ab00fdfa5f15b`）。M038-001…012 全部队列「否」。不把「全部否」当成模块已全对。
- 范围：仅 M038 16 文件。后端与库只读。未改业务 / HTML / 锁 / findings / ledger。未 fetch / commit / push / merge / deploy。未建脚本或日志临时产物。一次性 worktree 已删除（先删软链，再 `git worktree remove`，未用 `--force`）。未对数据库执行写语句。
- 独立性：未读 `docs/audit-2026-09/modules/M038-b5-r2-self-raw.md`（仅 `stat` 确认存在 37286 字节，存在性，不采信正文）。旧 raw 可读作对照，不作发现替代。全仓检索若命中该 self-raw 路径片段一律丢弃。只用两席；未用 MCP / 子代理 / 第三席；不问人。
- 本票不是模块收口，不是授权改 ledger。双席合取另走。Grok FAIL 具否决权。本席本票 **PASS**。无「有条件通过」。

## 0. 环境实用强制技能（如实）

160 V0.17 §6 在本席环境核对：

| 技能 | 本轮 |
|---|---|
| using-superpowers | 已读 SKILL.md，开工前技能自检 |
| using-arc | 已读；操作员禁止子代理/第三席，未派 Arc reviewer，未写 `docs/arc/audits` |
| graft | 仓库有 graft 索引。本模块 16 文件已由 `modules.md` 点名，全文直读。未跑 graft CLI（范围已冻结，160 禁 MCP）。记缺口：未调用 graft 工具 |
| grilling | 已读 SKILL.md。只借追问结构，自问自答；不问人、不派子代理 |
| long-running-background-tasks | 已读。vitest/tsc 在一次性 worktree 内跑完再下结论 |
| verification-before-completion | 先实跑 60 测与 `tsc -b --noEmit` 再写 PASS |
| using-git-worktrees | 160 划掉主流程 worktree；本轮派发明确要求 origin/main 一次性 worktree 跑前端测，已执行并删除 |
| Arc audit 机械审计/专家席 | 不适用（禁止子代理） |
| ponytail | B5 不修，未用 |
| systematic-debugging / diagnosing-bugs / tdd | 已核存在；本轮无新缺陷需定位，未走修复工作流 |
| codebase-design / writing-for-agents / batch-grill-me | 已核存在；B5 只读，未调用本体产出路径 |
| git-guardrails | 未 force-push / hard-reset |
| MCP | 按派发禁用。会话内若干 MCP 握手失败，未使用 |

提示词：仓库无 `docs/audit-2026-09/160-audit-prompt.md`；本席按 `ls` 使用 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17。判据锁为 `docs/audit-2026-09/plan/criteria-lock.md` V0.14。

## 1. 16 文件覆盖（磁盘实算）

合计 3347 行 / 非空 3115 行。`modules.md` 所记有效内容行 2787 为规划切分数字，以本表为准。三页与闸组件及 11 个测试均全文读完。

| 路径 | 行 | 非空 | SHA256 |
|---|---:|---:|---|
| apps/web/src/admin/pages/AccessGovernancePage.tsx | 777 | 739 | d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101 |
| apps/web/src/admin/pages/AccessGovernancePage.degraded.test.tsx | 61 | 55 | b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 |
| apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx | 191 | 175 | 521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871 |
| apps/web/src/admin/pages/AccessGovernancePage.scopechange.test.tsx | 82 | 73 | 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 |
| apps/web/src/admin/pages/AccessGovernancePage.test.tsx | 328 | 299 | bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327 |
| apps/web/src/admin/pages/AccessUsersPage.tsx | 385 | 371 | f0fc7976230726223325270293547065f166f41168f46b5c26cf6574daeb31cc |
| apps/web/src/admin/pages/AccessUsersPage.metricdetail.test.tsx | 41 | 36 | ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca |
| apps/web/src/admin/pages/AccessUsersPage.partnerbind.test.tsx | 113 | 101 | 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d |
| apps/web/src/admin/pages/AccessUsersPage.robustness.test.tsx | 45 | 38 | fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 |
| apps/web/src/admin/pages/AccessUsersPage.test.tsx | 191 | 177 | 62574a344c81f20b0a5722f30220832c9e458cec7a7a9cb030c8c5a37c4eefbb |
| apps/web/src/admin/pages/PermissionTemplatesPage.tsx | 565 | 534 | f27c12d96c4d74c8ecf43d236352c65cbec97eeedd2f24b91fafda091e326086 |
| apps/web/src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx | 116 | 104 | e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 |
| apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx | 211 | 192 | 7260e510802e3f71912d0409720d6fdb42d2a3bc729e066d68690be1104372b3 |
| apps/web/src/admin/security/Can.tsx | 84 | 80 | bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 |
| apps/web/src/admin/security/PermissionGate.tsx | 44 | 42 | a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 |
| apps/web/src/admin/security/PermissionGate.test.ts | 113 | 99 | 14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc |

## 2. 可执行证据（类型 2 / 4）

一次性 worktree：`git -C /home/james/complex-management worktree add --detach /home/james/m038-b5r2-grok-wt origin/main`（HEAD `53dec141`）。依次 `git apply` 两份 diff。软链主工作区 `node_modules` 与 `apps/web/node_modules`。未装依赖。

在 `apps/web` 执行：

```
npx vitest run \
  src/admin/pages/AccessGovernancePage.degraded.test.tsx \
  src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx \
  src/admin/pages/AccessGovernancePage.scopechange.test.tsx \
  src/admin/pages/AccessGovernancePage.test.tsx \
  src/admin/pages/AccessUsersPage.metricdetail.test.tsx \
  src/admin/pages/AccessUsersPage.partnerbind.test.tsx \
  src/admin/pages/AccessUsersPage.robustness.test.tsx \
  src/admin/pages/AccessUsersPage.test.tsx \
  src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx \
  src/admin/pages/PermissionTemplatesPage.test.tsx \
  src/admin/security/PermissionGate.test.ts
```

输出：`Test Files  11 passed (11)` / `Tests  60 passed (60)` / Duration 2.33s。`VITEST_EXIT:0`。

`npx tsc -b --noEmit` → `TSC_EXIT:0`。

清理：先 `rm` 两处软链；`git apply -R` 还原 apply 留下的 8 个已跟踪文件；删 `apps/web/tsconfig.tsbuildinfo`；`git worktree remove` 未用 `--force`。路径已不存在。

已知基线：origin/main 全量前端另有 3 条失败、全量后端另有 20 条失败，均属别的模块；本命令未跑全量。

只读 SQL（`complex_test`，`-h 127.0.0.1 -U complex`，口令只取 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD` 行，未 source、未 echo）：

- `permission_templates.template_type`：仅 `role` × 5
- `access_users.created_via`：仅 `legacy` × 2
- `access_users.account_type`：仅 `platform_admin` × 2
- `access_users.status`：仅 `active` × 2
- `access_grant_requests`：0 行
- `staff_levels.id`：恰好 30 条 `lvl_m{1..10}_{lower|middle|upper}`，与页内 `STAFF_LEVEL_IDS` 生成器逐键相同

## 3. 第一性原理三问

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？

三页是管理后台里「权限和职级」的操作面。删掉则 PGM-040/041/042（单一功能授权、角色模板、员工级自定义、职级 30 档）、PGM-043（外包账号与机构绑定入口）、BSM-005（新建员工账号 / 权限模板分步引导）、BSM-004（岗位模板存档/删除/恢复）、BEQR-149/151（职级闭集与最终权限公式的前台装配）在本网页层做不到。`Can.tsx` / `PermissionGate.tsx` 是按钮级与页面级授权闸，对应 PGM-041「权限按单一功能最小颗粒度」与 GEN-005。答得上 → 有业务上位需求。

### 3.2 它声称的行为，有没有被实际执行验证过？证据在哪？

有。类型 2：上述 11 文件 60 测全绿，含 001 中文取值、003 未登记岗位、004 取消存档/恢复、005 装配 403 与连点只一笔、006 北京时间读出再保存同一时刻、007 表头列数、008 三十档下拉、009 状态下拉用词、010 闸组件渲染与授权申请引导提交、011 复制/应用连点只一笔、012 套模板 403 后账号入表。类型 4：本席全文阅读 16 文件与锁内 HTML 原文；只读 SQL 核闭集。类型 1：PGM-066 L6368「界面全中文…四样例外」、BEQR-149「M1–M10，每级分上 / 中 / 下（共 30 档）」、PGM-041 最终权限公式。

### 3.3 它依赖的前提，哪一条是没验证过就当真的？

已核：16 文件与 after-apply worktree MATCH；60 测与 tsc 实跑；库内职级 30 档与页内生成器一致；库内模板类型现仅 `role`，页内映射含 `role→角色模板`。未在本模块 16 文件内执行后端 Java 或写 API。`apiErrorFromResponse`（`apps/web/src/shared/apiError.ts:191`）把任意 HTTP 403 译成「当前账号无权进行此操作」，不在本 16 文件，不新开需求；页面展示的是客户端给出的 `error.message`。B2-r3 已把 GR-02（绑定/解绑无 ref）定为只登记，本席无新的首次正常使用复现，不改判。

## 4. 对抗式（grilling 结构，自问自答）

Q1 三个页面里是否还有可双击重复提交的写按钮？
A1 逐页清单见 §5。治理页写动作走 `runWrite`（`writeBusyRef` 先挡，L116-128）或引导弹窗 `templateSaving`/`grantSaving`。模板页「复制模板」「应用模板」走 `runExclusive`（`exclusiveRef`，L82-91）且 `disabled={submitting || !selected?.id}`。账号页创建走 `submitting`；绑定/解绑走 `bindBusy`。B2-r3 已登记 GR-02 为只登记（React 离散事件下同步连点实测只一笔）。011 修复后 copy/apply 的连点测试本席实跑为绿。未发现新的「无忙态写函数」。

Q2 每个写动作失败时用户看到什么？
A2 见 §6。治理页 `runWrite` 的 catch 把 `Error.message` 或中文兜底写入 `setGlobalError`。引导弹窗失败留开并 `error` 上屏。012：套模板失败时账号入表、关弹窗、`setActionResult` 含「账号已创建」与「失败」，`setGlobalError` 非空。

Q3 上屏取值与字段名是否全中文？
A3 字段名、aria-label、placeholder、按钮、表头已是中文（002 的 id/ID 已改「编号」）。取值：授权申请状态/类型页内闭集映射（001）；开通方式 `legacy`/`manual_admin`；模板类型含 `role`；职级正则中文；未映射岗位「未登记岗位（码）」（003，码属 PGM-066 系统内部标识符例外）。权限码、用户/角色编号按四样例外保留。库内现有 `template_type=role`、`created_via=legacy` 均有中文。

Q4 读出再保存是否保值？
A4 `PermissionTemplatesPage.tsx` L42-48 `toDateTimeLocal` 按本地时区取年月日时分。测试把 TZ 设为 Asia/Shanghai，断言 `getHours()===8`（该断言能失败），再点「保存修改」，请求体 `valid_from`/`valid_until` 与 `2026-06-14T00:00:00Z` / `2026-06-15T00:00:00Z` 同一时刻。60 测含此条。

Q5 新补测试断言能否失败？
A5 能。011：连点两次若发出第二笔，`expect(posts).toHaveLength(1)` 失败；提交中按钮 `disabled` 断言失败。012：若仍把 apply 403 当整笔失败，则 `access-user-row-u-new` 不出现、弹窗不关、文案不含「账号已创建」。006：若 TZ 未生效，`getHours()===8` 失败。本席 60/60 绿，说明当前实现让这些能失败的断言通过。

Q6 四个 LOCK-AMEND 变更定义是否在本三页引出新冲突？
A6 见 §9。无新对象与现行正文冲突，不重开 001–010，不新造地理识别/加密存放/自助链接需求。

## 5. 三页写按钮清单（GEN-007）

### AccessGovernancePage.tsx

| 写动作 | 入口 | 忙态 | 失败可见 |
|---|---|---|---|
| 存档 / 申请删除 / 批准删除 / 驳回删除 / 恢复 / 取消存档 | PermissionButton L521-526 | `runWrite` + `disabled={writeBusy}` | `setGlobalError` |
| 保存岗位模板 | GuidedFlowModal L680 | `templateSaving` | 弹窗 `templateError` + `setGlobalError` |
| 定职位并适配 | L551 | `runWrite` | `setGlobalError`（兜底「定岗失败」） |
| 提交范围变更 | L563 | `runWrite` | `setGlobalError` |
| 装配/卸载角色 | L572-573 | `runWrite` | `setGlobalError` |
| 员工级覆盖设置/移除 | L585-586 | `runWrite` | `setGlobalError` |
| 发起授权申请 | GuidedFlowModal L725 | `grantSaving` | 弹窗 `grantError` + `setGlobalError` |
| 通过/驳回 | RowActions L615-616 | `runWrite`（ref 挡第二笔；RowActions 无 visual disabled） | `setGlobalError` |
| 保存授权政策 | L651 | `runWrite` | 内层 catch `setGlobalError` |

只读：翻页、读政策、刷新总结、显示存档勾选。

### AccessUsersPage.tsx

| 写动作 | 入口 | 忙态 | 失败可见 |
|---|---|---|---|
| 提交员工注册 / 新建外部协作账号 | GuidedFlowModal L315 | `submitting` | 账号 POST 失败：`formError`+`setGlobalError`+结果「失败」，弹窗留开。套模板失败：见 012 |
| 绑定机构 / 解绑 | L278-282 | `bindBusy` | `formError` 内联。B2-r3 GR-02 只登记 |

### PermissionTemplatesPage.tsx

| 写动作 | 入口 | 忙态 | 失败可见 |
|---|---|---|---|
| 保存修改 | L404-411 | `submitting`（B2 认定有忙态） | `formError`+`setGlobalError`+结果「失败」 |
| 复制模板 | L420-427 | `runExclusive` + `disabled={submitting \|\| !selected?.id}` | `formError`+`setGlobalError` |
| 应用模板 | HighRiskConfirm L432-441 | `runExclusive` + `disabled={submitting \|\| !selected?.id}`；`window.confirm` | `formError`+`setGlobalError`+结果「失败」 |
| 新建模板 | GuidedFlowModal L497 | `createSaving` | 弹窗 `createError`+`setGlobalError`+结果「失败」 |

## 6. 001–012 回归（不把队列否当已全对）

| ID | 本席核源 | 结论 |
|---|---|---|
| 001 | 治理页 `GRANT_STATUS_LABELS`/`GRANT_TYPE_LABELS`（L64-68）；账号页 `CREATED_VIA_LABELS`（L19）；模板页 `TEMPLATE_TYPE_LABELS` 含 `role`（L18）；审批空意见「通过」/「驳回」（L427-429）。测：三种状态与内部类型中文、开通方式中文、role 列「角色模板」。库内 `role`/`legacy` 有中文。 | 落地，无回归 |
| 002 | 字段名/aria/placeholder 为「编号」而非「id/ID」。测按新 aria 取元素。 | 落地 |
| 003 | `zhPosition` L50-54 未映射显示「未登记岗位（${code}）」。测 `front_desk` 单元格原文。 | 落地 |
| 004 | L526 `{row.deletedAt ? "恢复" : "取消存档"}`。测两种文案，表中无「停用模板」。 | 落地 |
| 005 | `runWrite` L116-128。测：装配 403 有 `setGlobalError`；同步连点装配只一笔且按钮 disabled。 | 落地 |
| 006 | `toDateTimeLocal` L42-48。测 TZ=Asia/Shanghai 保值。 | 落地 |
| 007 | 智能总结表头 6 列（L659）。测 thead th 数 = 首行 td 数。 | 落地 |
| 008 | `STAFF_LEVEL_IDS` 30 档（L56-59）；范围变更另有「沿用原职级」。测 options 长度 30/31。SQL 30 键一致。 | 落地 |
| 009 | 账号初始状态与岗位模板状态用 `formatBusinessStatus`。测「有效／待激活／已禁用」「有效／未激活」。 | 落地 |
| 010 | `PermissionGate.test.ts` 组件分支；治理引导测试 POST `/grant-requests` body。 | 落地 |
| 011 | `runExclusive` L82-91；复制/应用 `disabled` 并入 `submitting`。测两条连点只一笔。 | 落地 |
| 012 | `createUser` L136-156 套模板单独 try/catch；失败入表、关窗、文案「账号已创建」。测 apply 403。 | 落地 |

客户端把 HTTP 403 一律译成「当前账号无权进行此操作」（`apps/web/src/shared/apiError.ts:191`，非本模块文件）。012 页面仍展示该 `message`，并满足「账号已创建；…失败：」。不把改客户端列为新需求。

## 7. 顺从式：106 项（适用 / 适用部分 / 无本面）

锁内 HTML 派生项的判据句均为「映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。」GEN 用各自定义区全文。不新增判据。

| ID | 判定 | 本面证据或无本面理由 |
|---|---|---|
| GEN-001 | 适用 | 三页对应权限/职级/账号操作；无纯转发空页 |
| GEN-002 | 适用 | 入口与错误路径有测；60 测 / tsc 0 |
| GEN-003 | 适用 | 断言能失败（011/012/006）；覆盖写路径关键分支 |
| GEN-004 | 适用 | 表单校验（空用户、临时权限缺过期、模板无名等）在发请求前拦截 |
| GEN-005 | 适用 | `hasPermissions` DENY 优先、wildcard、loading 拒绝；PermissionButton 无权限拦截点击 |
| GEN-006 | 适用 | 012 部分写入说清；006 时间往返 |
| GEN-007 | 适用 | 治理 `runWrite`；模板 copy/apply `runExclusive`；011 测绿 |
| GEN-008 | 适用 | 页内映射与库内现有取值一致；请求体 camel→snake 由 AdminApi（他模块）转换，本页测断言 snake 字段 |
| GEN-009 | 适用 | 写失败不吞；引导失败留窗；012 不再伪造成整笔失败 |
| GEN-010 | 适用 | 无权限 fail-safe；密码 `type=password`；抽屉测不含手机号明文 |
| GEN-011 | 适用 | 写动作带 `operatorNote`；审批意见中文留痕 |
| GEN-012 | 适用部分 | 授权申请分页；无本面：后端查询边界 |
| GEN-013 | 无本面 | 16 文件无构建配置 |
| GEN-014 | 无本面 | 无迁移文件 |
| GEN-015 | 适用 | 键盘/aria/中文文案；表头列数；引导确认页 |
| GEN-016 | 适用 | 页内描述与按钮文案可对 HTML；页头「企业微信同步结果」无入口——旧观察，不升队 |
| GEN-017 | 适用 | 本票按冲突→细化→技术性→添加；本次无添加项 |
| GEN-018 | 适用 | 结论落到 HTML / 60 测 / SQL / 源码行 |
| BEQR-063 | 无本面 | 酒吧反结/沽清/免单，三页无此动作 |
| BEQR-074 | 无本面 | 隐私桌台，无此 UI |
| BEQR-143 | 无本面 | 四类主体档案不在本页 |
| BEQR-144 | 无本面 | 经营主体关系不在本页 |
| BEQR-145 | 无本面 | 城市项目公司不在本页 |
| BEQR-146 | 无本面 | 运营公司职责不在本页 |
| BEQR-147 | 无本面 | 收入归属不在本页 |
| BEQR-148 | 无本面 | 合作模式不在本页 |
| BEQR-149 | 适用 | 30 档下拉 + `zhLevel`；SQL 30 键 |
| BEQR-150 | 无本面 | 终端十一端开关不在本页 |
| BEQR-151 | 适用 | 员工级授予/拒绝、模板应用、角色装配；公式在页内覆盖写入口 |
| BEQR-152 | 适用部分 | 范围变更与权限码输入；无本面：敏感经营数据查看本身 |
| BEQR-153 | 无本面 | 终端开关优先级 |
| BEQR-154 | 无本面 | 综合体项目状态 |
| BEQR-155 | 无本面 | 商户/运营主体状态 |
| BEQR-156 | 无本面 | 合同状态 |
| BEQR-157 | 无本面 | 员工五态（待入职/在职/…）≠ 本页账号 status；账号态走字典 |
| BEQR-158 | 无本面 | 设备状态 |
| BEQR-159 | 无本面 | 权限五态无本页改状态接口；模板用 active/inactive（009） |
| BEQR-166 | 适用部分 | 范围变更 `finalApproverId`；授权申请弹窗不选手填链（HTML 允许默认总经理）——旧观察 |
| BEQR-182 | 无本面 | 考勤日结果 |
| BEQR-183 | 无本面 | 打卡方式 |
| BEQR-184 | 无本面 | 请假类型 |
| BEQR-185 | 无本面 | 请假单状态 |
| BEQR-186 | 适用部分 | 定岗表「基础薪资(元)」；无本面：薪资单构成其余项 |
| BEQR-187 | 无本面 | 提成发放 |
| BEQR-189 | 无本面 | 管理费方式 |
| BSM-001 | 适用部分 | 权限是「先管清楚基础盘子」的一环 |
| BSM-003 | 适用 | 存档删除、分步引导、分岗权限均在本三页有面 |
| BSM-004 | 适用 | 岗位模板存档/申请删除/恢复；文案「取消存档」 |
| BSM-005 | 适用 | 三页新建走 GuidedFlowModal（＋新建→确认→提交）；步内校验中文 |
| BSM-006 | 无本面 | 动态二维码 |
| BSM-007 | 适用部分 | 闸组件与 wildcard 测；高危不因管理员身份自动放行由后端执行，本页走授权申请 |
| BSM-009 | 适用 | 基础管理含权限 |
| BSM-028 | 无本面 | 建立综合体/商户档案等 |
| BSM-029 | 适用部分 | 「用户权限要知道员工…职级」；本页定岗/职级/账号 |
| BSM-072 | 无本面 | 商户侧系统 |
| BSM-073 | 无本面 | 商户主要功能 |
| BSM-074 | 无本面 | 商户连接 |
| BSM-084 | 无本面 | 员工手机端 |
| BSM-085 | 适用部分 | 账号页「员工注册申请」对应公司审批路径；无本面：私人手机 App 自助 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资页 |
| BSM-088 | 无本面 | 提成/活动边界 |
| BSM-114 | 适用部分 | 管理后台配置/复核；HighRiskConfirm=少量业务确认。现行正文「使用 Web 端必须打开地理识别」不在这 16 文件，无本面，不新造地理识别需求 |
| BSM-115 | 适用部分 | 基础管理连权限；无本面：票码/酒吧/供应链连接 |
| BSM-120 | 适用部分 | 「职级可按 M1-M10，并可分上中下」「员工权限按单一功能最小颗粒度」「Web 前端允许少量业务确认」。其余票务/酒吧已拍板项无本面 |
| PGM-001 | 适用部分 | 权限是治理底座之一；全链可点击：三表行开抽屉 |
| PGM-002 | 适用部分 | 治理顺序第 3 步「再看人员」：本页权限/职级/账号 |
| PGM-003 | 适用 | 模块入口含「权限和职级」 |
| PGM-004 | 无本面 | 综合体和集团模块页 |
| PGM-005 | 无本面 | 空间和点位模块页 |
| PGM-006 | 无本面 | 商户和主体模块页 |
| PGM-007 | 无本面 | 合同和准入模块页 |
| PGM-008 | 适用部分 | 账号页注册申请与外包账号；无本面：私人手机绑定 |
| PGM-009 | 适用 | 本模块正页 |
| PGM-010 | 无本面 | 终端开关 |
| PGM-011 | 无本面 | 设备管理 |
| PGM-012 | 无本面 | 商户侧 |
| PGM-013 | 无本面 | 开放接口 |
| PGM-014 | 适用 | 审批留痕、operatorNote、授权申请复核 |
| PGM-015 | 无本面 | 楼宇运营 |
| PGM-016 | 适用部分 | 指向 PGM-059；U 盘破窗无本面 |
| PGM-017 | 适用部分 | 范围变更带所属项目；无本面：集团视图本身 |
| PGM-018 | 无本面 | 多对多主体说明页 |
| PGM-019 | 无本面 | 各主体职责表 |
| PGM-020 | 无本面 | 综合体档案功能 |
| PGM-021 | 无本面 | 集团业务意义 |
| PGM-022 | 无本面 | 空间点位专页 |
| PGM-024 | 无本面 | 空间影响业务 |
| PGM-026 | 无本面 | 商户档案主要功能 |
| PGM-027 | 无本面 | 商户业务边界 |
| PGM-031 | 无本面 | 经销商门户 |
| PGM-032 | 适用部分 | 人与账号对应；无本面：设备标识绑定 |
| PGM-033 | 适用部分 | 注册两条路径在账号页有「员工注册申请」与外包直建 |
| PGM-034 | 无本面 | 工作手机和企业微信；页头提到无 UI（旧观察） |
| PGM-035 | 无本面 | 提成发放治理 |
| PGM-036 | 无本面 | 设备台账 |
| PGM-040 | 适用 | 权限点勾选网格、角色模板、员工级覆盖 |
| PGM-041 | 适用 | 模板、临时有效期/单次、员工授予/拒绝、高危走授权申请、范围变更先审批文案 L566 |
| PGM-042 | 适用 | 30 档；定岗另填基础薪资 |
| PGM-043 | 适用部分 | 外包账号创建、机构绑定/解绑（现行正文）。无本面：机构自助链接、下属额度、审计人员只读专页。不新造这些入口 |
| PGM-044 | 无本面 | 桌台/挂单/经销权限组 |
| PGM-048 | 无本面 | 商户侧全开放 |
| PGM-059 | 适用部分 | 账号抽屉字段无手机号，测断言不含 `13800000001`。现行正文「一律加密存放」「脱敏不提供关闭开关」在后端/他页，无本面，不新造加密 UI |
| PGM-060 | 无本面 | 会员/营销/人脸 |
| PGM-065 | 适用部分 | 治理连接权限；无本面：连接票务/酒吧等 |
| PGM-066 | 适用 | 「员工权限按单一功能最小颗粒度」；「界面全中文」字段名/状态/枚举/按钮/提示/报错已核，标识符四样例外；「所有有过程的业务动作都必须留痕」由 operatorNote/审批意见承载 |

106/106 均有适用或无本面记录。

## 8. 不改判的已登记观察（无新证据）

B2-r3 双席定为只登记，本席复读源码后维持：

- GR-02：绑定/解绑只有 `bindBusy` 无 ref。无新的首次正常使用双发证据。
- 岗位编码自由文本（HTML 未规定须下拉；003 只约束上屏）。
- 默认 `organizationId="org-1"`、`targetUsersText="user-1"` 为标识符占位。
- 页头「企业微信同步结果」无入口。
- 硬编码 `usr_master_authority` 只改按钮文案。
- `template_type=copy` 不在三份 HTML，作复制 provenance 技术性留下。
- `AdminDataState empty={false}` 治理空表无空态。
- GET `/partner/orgs` 403 降级为空下拉。

不为理论场景加需求。无添加项，不写 `questions/`。

## 9. 四个变更定义（V0.14 现行正文）

- **BSM-114** HTML：管理后台「配置、查询、复核」——本三页适用。Web「必须打开地理识别」——16 文件无地理 API，无本面，不立项。
- **BSM-120** HTML：职级 M1-M10 上中下；权限最小颗粒；Web 少量确认——本三页适用（30 档、权限勾选、HighRiskConfirm）。其余已拍板项无本面。
- **PGM-043** HTML：外包账号由管理员分配、机构绑定——账号页适用。自助链接、下属额度、审计两类只读——无本面。
- **PGM-059** HTML：日常界面不见手机明文——抽屉测适用。加密存放、商户联系人例外——无本面。

四条均不构成与本 16 文件实现的新冲突，不重开 001–010。

## 10. 本次 findings

无新临时编号。无应升队项。无冲突/细化/技术性工程缺陷未入队。无添加。001–012 无回归。

四分类顺序已走：未发现与三份 HTML 明文矛盾的本模块实现；未为理论双击（GR-02 类）加需求。

## 11. 步骤结论

本席 B5-r2 **PASS**。

不是模块 PASS，不是授权把 ledger 改 PASS。须席甲独立票合取，且 §5 闸 1 四值全等后才能收口。B3 累计 2。无 HTML 申请、无 LOCK-AMEND、无 questions。

依据：16 文件全文 + 锁内 106 定义/HTML 原文 + 11 文件 60 测全绿 + tsc 0 + 只读 SQL 闭集。无未验证前提当作事实。无「有条件通过」。
