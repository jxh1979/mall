CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 B5-r5 修复后整模块独立三线审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r5
席位：固定 Grok 只读外审席乙 grok-4.6
计划：160-audit-prompt.md V0.17 §4/B5；runbook.md V0.02；criteria-lock.md V0.14
本文件路径：docs/audit-2026-09/modules/M012-b5-r5-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

审计结论：**PASS**

审完整模块全部六文件、冻结 21 项、三线之后判定。未把 B3 累计 5、额度已用、B4-r5 双 PASS 或旧 B5-r4 收口折成 PASS。未用 B4 替代 B5。有未完不得 PASS——本席未发现未完项、未发现新的工程缺陷、无未答 questions、无锁外新判据。

以上仅为席乙本轮 B5-r5 票，不等于双 PASS，也不等于模块 ledger `PASS`。席甲自审票本席未读。本席 FAIL 具否决权；本席不行使否决。本席不发布、不提交、不推送、不合并。

未读取：`docs/audit-2026-09/modules/M012-b5-r5-self-raw.md`（`ls` 见路径存在、8547 字节、mtime 2026-09-22 17:34；**未打开正文**）。未读适配器缓冲、未读席甲本次 B5 意见。未用 MCP、未派子代理、未设第三席、未提问、未改源码/测试/HTML/锁/findings/ledger/git/index、未 fetch/commit/push/merge/deploy、无数据库/Maven、无 `/tmp` 隔离副本、无 git archive、无探针文件。唯一写入即本文件。全部命令在 `/home/james/complex-management` 及其下。

---

## 0. 技能自核（§6，不冒充）

本席核到且按提示词收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| verification-before-completion | 同 superpowers | PASS 前在正本 `apps/web` 实跑三测试 + typecheck |
| systematic-debugging | 同 superpowers | 只借读错误→复现→追数据流；不改生产码、不加诊断桩 |
| requesting-code-review | 同 superpowers | B5 外审=本席；禁止 `HEAD~1`、禁止子代理、禁止无 ID 即刻修 |
| receiving-code-review | 同 superpowers | 本席是外审，B5 期间不 IMPLEMENT |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 三页缝仍是 `useServerPagedRows` + KPI URL + 行抽屉；018 横幅走可见表命中 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=018 无条件「已定位」、019 把密钥直删写成一律上级审批；本轮核修复后行为 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 只读找可删。当前六源无新膨胀。Lean already. |
| writing-for-agents | 存在 | 本 raw 给下游合取/调度读 |
| git-guardrails-claude-code | 可选存在 | 不安装 hook、不写 `.git`、不设 `GIT_INDEX_FILE` |

缺口（不冒充已调用）：`batch-grill-me` 已并入 `grilling`，无独立技能；`ubiquitous-language` 已并入 domain-modeling（提示词划掉 domain-modeling，不调用）。划掉的 worktree/子代理/research/Arc 全仓 audit 未调用。graft SKILL.md 已读；本轮不跑 graft CLI（会刷新图、可能写 `graft/`，与只读铁律冲突）。

---

## 1. 输入、独立锁实算、范围

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
python3 hashlib  b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  bytes=891182
sha256sum        b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
openssl dgst     SHA2-256(...)= b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
```

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger **顶行**同值。ledger 正文另有历史行 `CRITERIA-LOCK-SHA256: cf25d716…`（约 L7088），不当作当前基准。criteria-lock 版本 V0.14。`LOCK-AMEND-COUNT: 1`。aftertree 锁 blob `69dccbb728feb863971f2fd7aca76da47db11e64`，内容 SHA256 同上。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段 |
| 阶段 | B5-r5 修复后**整模块**三线全审（不是 diff） |
| 计划冻结行数 | 1485（`plan/modules.md`，不改） |
| 当前六源合计 | **2129** 行（274/308/524/475/257/291） |
| 映射 21 项 | BSM-001、BSM-003、GEN-001…GEN-018、PGM-001 |
| aftertree | `3eca41eb9aa21c6791ef70d5695deb940abeb992`（`git cat-file -t` = tree） |
| 六源 vs aftertree | `git diff --stat <after> -- <六路径>` 空；工作区 `git hash-object` = aftertree blob |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main` |
| HEAD^{tree} | `ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。六源是 aftertree 快照，不是 HEAD 已提交树 |
| B3 | 累计 **5**（操作员明确仅 018/019 追加一次）；**无第 6 次许可** |
| findings 当前队列 | 001—019 全否；009 观察登记。018/019 已修并经 B4 有效双 PASS 清队 |
| B4 有效双 PASS（只作 after 对齐与 018/019 已修上下文，**不替代 B5**） | self V0.01；本席 B4-r5 **V0.02**。本席 B4-r5 **V0.01** 因 `/tmp` 验证落点无效，**本报告不引用其测试证据** |
| questions | `M012-006.md` / `M012-007.md` 答复段首行均为精确 `删除`；无未答 |
| 三份业务 HTML | 唯一业务源，只读；aftertree blob 与工作区 `git hash-object` 全等 |
| 其它源 | 仅实现证据（AdminApi unwrap、PermissionGate、useServerPagedRows、PlatformController/Service、statusLabels、RowDetailDrawer/cnDate、AuditTrailHint、adminRoutes）。不新功能、不新判据、不扩模块 |
| 用户边界 | 旧 009 不补附件/绑定创建/项目分配/三项目资料表单；现 HTML 两联系人查看例外不扩导出 |

三 HTML 工作区 SHA256（只读对照）：

| 文件 | sha256 |
|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |

aftertree blob：BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `6e9d813c985406aaf14c1645af2b494d342ae657`；工作区 `git hash-object` 同。

---

## 2. 六源覆盖 / SHA（全文审，不只 018/019 或 diff）

| 文件 | 行 | 版本注释 | git blob（=aftertree） | sha256 |
|---|---:|---|---|---|
| `apps/web/src/admin/pages/GovernanceTopologyPage.tsx` | 274 | V0.04 | `89af88ebf200d11fcd1d47082a788411311cd047` | `6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036` |
| `apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx` | 308 | V0.04 | `5bf4bf26411f020c81780c66681b6485099f539f` | `985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb` |
| `apps/web/src/admin/pages/OrganizationMerchantPage.tsx` | 524 | V0.06 | `2f1f1cbd965ef6c747228ab402f5d7cd951de3f1` | `55dc9a69205df3254c4031015bfe53907a3b1fbd7605e97c8f64a8ab515cb328` |
| `apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx` | 475 | V0.08 | `ce494c7535024948b157e9cf15c2c42fbcbdbfdc` | `3e91c3bbd589558e50bab1192036db2179b0dbdd4158fb0841eba1b0184d33e1` |
| `apps/web/src/admin/pages/SubjectBindingsPage.tsx` | 257 | V0.03 | `0d916b81f72c4d8ea0acf46e43e85121e2ba71db` | `94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c` |
| `apps/web/src/admin/pages/SubjectBindingsPage.test.tsx` | 291 | V0.04 | `e145f2b54a5c3271d7601c382044486e37720872` | `ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b` |
| 合计 | 2129 | | | |

全文覆盖记录（无抽样宣称整文件 PASS）：

| 文件 | 覆盖段 |
|---|---|
| GovernanceTopologyPage.tsx | L1–34 状态/空间类型词表；L35–66 文案与 KPI URL；L68–135 权限分页加载；L137–185 挂接计数/下钻/抽屉；L186–274 两表+KPI+空态 |
| GovernanceTopologyPage.test.tsx | L1–114 KPI/中文状态；L116–141 业态/挂接；L143–184 抽屉键盘；L186–230 无主体权；L233–308 归属/字典/分页/下钻/未加载/重试 |
| OrganizationMerchantPage.tsx | L1–85 词表/KPI；L86–177 表单与分页/018 滚动守卫；L179–283 创建/存档/019 删除提示；L285–370 抽屉与行动作；L372–524 三表/018 横幅/引导流 |
| OrganizationMerchantPage.test.tsx | L1–231 KPI/中文/创建；L233–280 抽屉；L282–338 存档删除含 019；L341–419 权限/状态/分页/键盘/末页回退；L422–437 重试；L439–475 018 |
| SubjectBindingsPage.tsx | L1–72 真实绑定字段/三态；L74–185 分页/校验/变更 POST；L187–257 KPI/表/键盘/抽屉 |
| SubjectBindingsPage.test.tsx | L1–128 KPI/中文表头；L130–175 真实字段/抽屉；L177–222 变更请求；L225–273 键盘/暂停/分页/财税拒绝；L276–291 重试 |

---

## 3. 可执行验证（正本 `apps/web`，类型 2+4）

根 `npm run typecheck`（workspace `apps/web` → `tsc -b --noEmit`）exit **0**。仅作参考对齐。

权威本轮证据（直接正本，配置 `apps/web/vitest.config.ts`，environment=jsdom，setup `src/test/setup.ts`）：

```
cd /home/james/complex-management/apps/web && npx vitest run \
  src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

输出：`Test Files  3 passed (3)`；`Tests  59 passed (59)`；Duration 3.85s；exit **0**。展开计数：拓扑 19 + 组织 24 + 绑定 16 = 59。

jsdom 打印 `Not implemented: Window's scrollTo()` / `getComputedStyle() … pseudo-elements`，测试仍全绿。不据此声称浏览器或真实 HTTP 行为。

一次误跑：在仓库根对相对路径 `src/admin/pages/…` 调 `npx vitest`，未用 `apps/web` 配置，扫进 `.scratch/` 且无 jsdom（`window is not defined`）。该次 **作废**，不计入产品缺陷、不引用为测试证据。

未跑 Maven、未连数据库、未开 dev server、未用浏览器工具、未对 `/api` 发真实请求。页面行为证据是 stub `fetch` → 真实 `AdminApi` unwrap → 组件。后端契约只读 `PlatformController` / `PlatformService` 源码对照，不作可执行后端声称。

---

## 4. 21 项锁映射（逐项证据 / 不适用理由）

判定顺序：冲突 → 细化 → 技术性 → 添加。阶段 B 不启用锁外判据。

### BSM-001 — 整体说明：这套系统到底管什么

- HTML：`docs/business-system-map-for-operations-v0.1.html` h2「整体说明：这套系统到底管什么」及「先管清楚基础盘子：哪个综合体、谁在经营、在哪经营…」（锁 L2278–2285）。
- 适用：三页正是归属盘子的只读/档案/绑定入口。拓扑导语 L36–39、组织 L28–31、绑定 L44–47 与「谁在经营 / 在哪经营 / 合同结算责任」同口径。
- 证据：拓扑按 `merchant.projectId` 正向归属当前项目（L192–195）；组织项目表不再把 `merchantId` 当「项目属于商户」；绑定读 `merchantId/operationEntityId/contractEntityId/settlementEntityId`。测试「商户按真实 project_id 归属」绿。
- 不发明多综合体切换器、不发明经营动作/结果域。可配置钱策原则不在本模块实现（无税率/分账表单）——不适用本页写路径。
- 结论：满足。无冲突。

### BSM-003 — 系统通用能力：数据可改可删、操作有引导、权限分岗

- HTML：该 h2 正文仅 lead「下面这几条不属于某一个子系统…」（锁 L2357–2358）。子锚点 BSM-004/005/007 **未映射**本模块。
- 适用：h2 标题点名可改可删、引导、分岗。组织页存档/申请删除/审批、引导式新建、行内 `RowActions` 按 `merchant.profile.manage/approve` 过滤；绑定变更 `entity.binding.manage` + `HighRiskConfirm`；拓扑只展示。
- 019 用 BSM-004 原文（map 570/576–577，含「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）作**已有规则类型 1 解释**，不新增映射、不 LOCK-AMEND。
- 后端只读对照：`PlatformService` L283–295 密钥本人申请即软删；L328–336 非最高权禁批、禁自审。前端提示现与此一致。
- 引导：`GuidedFlowModal` 默认单屏分节 + 确认页，按钮中文「取消/下一步/上一步/创建商户档案」。BSM-005 未映射，不把单屏判冲突。
- 结论：在映射范围内满足。不把未映射的 BSM-004 全文当本模块收口清单。

### PGM-001 — 整体定位：平台治理层不是一个业务部门，而是所有业务的底座

- HTML：h2「先管归属 / 再管资格 / 最后管责任」；全链可点击；界面全中文（四样例外：单据号、设备编号、二维码内容、系统内部标识符）；金额/并发原则本页无金额拆分。
- 归属：三页展示综合体项目、商户、空间、四类主体绑定。路由 `ADM-GOV-001/002/003` 要求 `governance.project.view`/`merchant.profile.view`/`entity.binding.view` 等。
- 见名可点：整行开抽屉；拓扑商户名 `TraceLink`→组织页；组织商户抽屉 `auditTraceField`→审计页。未给每个单元格单独 TraceLink——与既有 009「绑定抽屉追溯」观察同根，不新开添加、不把抽屉模式改判冲突。
- 零英文：项目/商户/绑定/空间状态走页内中文表（暂停≠封禁；`formatBusinessStatus` 全局 `suspended=已封禁` 不用于这些列）。业态 `bar→酒吧`，空间类型字典优先否则种子中文否则「未识别类型」。表头「合同主体编号」不含 `ID`。KPI/`?kpi=`/`status=active` 为 URL 内部标识（四样例外）。`AuditTrailHint` 事件名收在 `<details>操作留痕`（用户已裁决技术段）。绑定表 `effectiveFrom` 仍可能直出 ISO 日——009「ISO日期」已登记，不重开。
- 两联系人：PGM 另锚（约 L494/680/871）要求第一/第二联系人及查看例外、**查看例外不授予未脱敏导出**。用户本轮明确：不扩导出。第二联系人表单/列属 009 不补。掩码在 Controller `maskPhonesForUnauthorized`（`pii.contact_phone.view`），前端展示接口返回值。PGM-059 未映射——009「联系人后端掩码/未映射PGM-059」维持观察。
- 结论：映射锚点满足；未映射联系人锚点不升级为 finding。

### GEN-001 必要性与最小性

三页删除则 BSM-001/PGM-001 归属入口落空。018/019 只改横幅守卫与一句 prompt + 最小测试，无新定位器/新接口/新附件。满足。

### GEN-002 可执行正确性

入口：三页 export + `adminRoutes` ADM-GOV-001/002/003。主路径与错误路径：创建/存档/删除申请/绑定变更/分页/KPI/下钻均有可失败断言。018：仅 `visibleMerchants` 命中且商户表在当前视图时才称「已定位」，否则「当前列表尚未定位到该商户，请检查筛选或翻页」（L415–420）；滚动亦先 `merchants.some(id)`（L169–171）。019：prompt 全文与测试 L317–318 锁定。59/59 绿。满足。

### GEN-003 测试有效性

夹具走真实 snake_case + AdminApi。断言含：中文/非英文码、真实字段非 `-`、POST body 含 `action`/`reason`、不含假证据、501 翻页、权限不请求主体、键盘不截获子控件、018 后页/隐藏表、019 prompt。测试通过不替代读源；本席已读六源并实跑。满足。

### GEN-004 信任边界校验

创建：商户名/业态/电话非空（L179–192）；电话无 11 位正则（006/007 答删除）。绑定：必选行、`action` 三态、原因非空。存档/删除理由空则拦截。分页 `page/size` 由 hook 约束。满足。不把未映射的联系人格式闭集当添加。

### GEN-005 授权与最小权限

路由权限见 `adminRoutes.ts` L16–18。缺 `entity.binding.view` 时拓扑/组织不请求 `/business-entities`，KPI/列降级为 `-`，抽屉不写「主体资料未加载」（有权但资料在后页才写未加载）。写：创建 `merchant.profile.manage`；存档/申请删除同码；审批 `merchant.profile.approve`；绑定变更 `entity.binding.manage`。后端 Controller 再 `requirePermission`。前端隐藏不是唯一闸。满足。

### GEN-006 数据完整性

本模块无本地库。每笔写单 POST + 幂等键；失败不关引导窗（创建）或 `reportWriteFailure`。绑定财税拒绝保留原因。后端占用检查/软删不在本模块实现，只读对照。满足（前端侧）。

### GEN-007 并发与幂等

`HighRiskConfirm` + `submitting`；`Date.now()` 键每次新值——009「Date.now与submitting」已登记。按钮禁用覆盖首次连点。不把重试换新键升级为新 finding。满足（在已登记观察下）。

### GEN-008 接口与模式一致性

绑定字段与 `BusinessEntityBindingRecord` 一致；商户 `projectId` 正向；空间 `projectId` 挂接；`getPaged` 保留 total；合作六类与 `MERCHANT_COOPERATION_FORMS` 同词表；空合作=后台未定（`requireCooperationForm` 可空）。`api.get('/space-types')` 经 unwrap 取 `data.items`。满足。

### GEN-009 错误处理与恢复

三页 `onError: setGlobalError` + 读取开始清 `null`；503 后重试测「最后一次 `setGlobalError(null)`」。写路径 catch。018 不再在未命中时伪称已定位。满足。

### GEN-010 安全与敏感信息

无注入拼接进 HTML（React 文本节点）。电话明文取决于后端掩码权限，本页不另造导出/打印。不扩导出。满足（映射范围内）。

### GEN-011 可观测与审计证据

写操作 `setActionResult`。组织商户抽屉 `auditTraceField(navigateTo, "merchant", id)`。绑定页 `AuditTrailHint event="entity_binding.viewed / entity_binding.changed"`（与 Controller 审计名一致，收在技术详情）。017 已删拓扑/组织错误 Hint。满足。

### GEN-012 资源与性能边界

页大小 500；`TablePager` 用服务端 total；KPI「本页」不去冒称全量。无无限抓取。满足。

### GEN-013 构建配置与依赖

本模块不新增依赖。`apps/web` `tsc -b --noEmit` 0。满足（本模块无独立构建清单，适用=可类型检查）。

### GEN-014 数据库迁移正确性

**不适用**：六文件无 SQL/迁移。理由：模块切分仅为网页与验证。

### GEN-015 界面可用性与可访问性

中文文案；行 `tabIndex=0` 且仅 `event.target===event.currentTarget` 处理 Enter/Space；子控件 Space/Enter 测过；抽屉 Esc/遮罩/关闭 `aria-label="关闭"`；引导流中文按钮。018 横幅在 `AdminDataState` 外，加载中走「尚未定位」分支（与定稿「未展示商户表则收窄」一致）。jsdom 无 `scrollTo` 实现——不声称滚动在真浏览器成功。满足（可访问性基本面）；浏览器未核。

### GEN-016 文档可验证性

页内接口证据在 `TechDetails`，路径与 Controller 映射一致。测试注释 L142「项目表…商户下钻（merchant_id）」已过时（表已无该列）——技术性观察，不构成用户可见假完成。满足。

### GEN-017 业务来源分类

本轮无新业务发明。018/019 为既有下钻反馈与既有删除规则的细化，已定稿已修已清队。009 观察不升添加。无 HTML 自身缺陷需铁律 4 暂停。满足。

### GEN-018 第一性原理证据

结论落到：HTML 逐字（BSM/PGM）、正本 vitest/tsc 输出、只读后端源码、可复现 `sha256sum`/`git hash-object`/`git diff --stat`。不采用「历史 PASS」「B4 已过」或席位意见本身。满足。

---

## 5. 三线（整模块）

### 5.1 第一性原理

1. **为什么必须存在？** 删三页则 HTML「先管清楚基础盘子 / 先管归属」无后台入口：哪个综合体、谁在经营、点位、四类主体唯一绑定。答得上，有业务上位。
2. **声称是否执行验证？** 是。证据=正本 59 tests + typecheck 0 + 全文对照 Controller/Service。未验证：真浏览器、真 HTTP、密钥本人删路径（需鉴权上下文/Maven）。
3. **未验证就当真的前提？** （a）jsdom≈浏览器滚动/焦点；（b）mock 200/409/503≈生产信封；（c）`/projects` 当前项目 0–1 条（只读 `listProjectsPaged(currentProject())` L451，本席未跑 SQL）。这些前提未当缺陷。501 项目反例仍撤回，不重开 013。

### 5.2 顺从式（假定实现意图正确）

| 对象 | 核对 |
|---|---|
| 对象/项目归属 | 商户 `projectId`→当前项目；空间 `projectId`；创建不传项目时后端 `creationProject` 填当前项目。组织不展示「项目属于商户」。 |
| 权限 | 见 GEN-005。无主体权不请求、不白屏。 |
| 分页 | 三页 `useServerPagedRows` 500；total 真值；组织筛选参数随翻页；末页存档回第 1 页。 |
| 字段/中文 | 局部状态表；业态/合作/角色经中文表或 `formatBusinessStatus`；未知空间「未识别类型」；未知状态「未识别状态」。 |
| KPI/下钻/键盘 | KPI 带参聚焦；组织 018 横幅与滚动以可见表为准；拓扑 KPI 商户/空间可点，主体/项目数字不可点（无独立表）。键盘 target=currentTarget。 |
| 生命周期/绑定动作 | 生效中→存档→申请删除→审批；密钥本人申请直删（后端）+ 前端提示；绑定三态 POST `action`+`reason`，无假附件；财税 409 可见。 |
| 错误 | 读失败可重试并清全局错误；写失败可见。 |
| 测试 | 59 条覆盖上述关键分支。 |

未要求补：附件上传、绑定创建、项目分配 UI、三项目资料表单、第二联系人、导出。

### 5.3 对抗式（grilling 自问，不派子代理）

| 反例 | 结果 |
|---|---|
| focus 在第 501 条仍称已定位 | 否。018 未命中走「尚未定位」；测 L446–459 绿。 |
| `kpi=projects` 仍称已定位 | 否。条件要求 `!kpiFocus \|\| table==="merchants"`；测 L462–474 绿。 |
| `status=active` 隐藏停用商户仍称已定位 | 否。`visibleMerchants` 过滤；同测。 |
| 加载中闪「已定位」 | 条件含 `!loading`。加载中走尚未定位（定稿收窄）。不升缺陷。 |
| 删除 prompt 仍写「提交后需上级审批」 | 否。L256 与 019 测试锁定新句。 |
| 无 manage 仍提交绑定 | `HighRiskConfirm` 无权限不弹窗；通配测试账号除外。 |
| 无 entity.binding.view 整页白屏 | 否。不请求主体。 |
| 501 只见 500 且无后页 | 否。pager total=501，next 到第 501 行。 |
| `suspended` 显示已封禁 | 否。页内「暂停」；绑定/商户测绿。 |
| 自定义空间类型 kiosk 直出英文 | 否。字典 `一体机位`。 |
| 子控件 Space 被行快捷键截获 | 否。radio 空格选中不开抽屉。 |
| 把 501 项目塞进 `/projects` | 现行 `WHERE id=:project_id`，0–1 条。不重开 013。 |
| 第二联系人/导出 | 用户禁止补/扩。不 FAIL。 |
| 行内英文事件名当业务正文 | 在 TechDetails。017 已删错误 Hint。 |

---

## 6. 018 / 019 修复后核验（B5 仍整模块，此为已清队项回归）

**M012-018**（队列否）：`OrganizationMerchantPage.tsx` L168–177 仅已加载且 `id===focusObjectId` 才滚动；L415–420 仅 `!loading && !error && 商户表可见 && visibleMerchants 命中` 才显示「已从下钻定位到商户…在下表查看」，否则「当前列表尚未定位到该商户，请检查筛选或翻页」。测试后页目标、KPI 隐藏目标、清除后成功。分类：细化。无残留工程缺陷。

**M012-019**（队列否）：L256 prompt 为「删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：」。测试 L317–318 `toHaveBeenCalledWith` 该全文。只读后端 L283–295 / L328–336 与句意一致。分类：细化。无残留工程缺陷。

**M012-001–017**：保持否（已修或登记）。**M012-009**：观察登记，不补附件/绑定创建/项目分配/三项目资料表单。

---

## 7. Findings

无新临时编号。无冲突、无未定稿工程缺陷、无添加项、无 HTML 铁律 4 问题。

不把下列事项写入修复队列（与 009 / 用户边界 / 铁律 6 一致）：

| 观察 | 理由 |
|---|---|
| 009 十项（掩码/PGM-059、Date.now、KPI 内部键、旧测试注释、绑定抽屉追溯、词表、无主体权 KPI0、ISO 日期、项目重载无入边等） | 已登记否 |
| 加载中 018 横幅走「尚未定位」 | 定稿「未展示商户表则收窄」 |
| 绑定日期未走 `cnDate` | 009 ISO |
| 无第二联系人列/表单、无导出 | 009 + 用户「不扩导出」 |
| 拓扑 space 行无 `data-focus-node` | 009 拓扑下钻不补测/不扩 |
| jsdom 无 scrollTo | 环境限制，未做浏览器核 |
| `formatBusinessStatus` 未知业态回退原码 | 无现行闭集外业态的第一次正常触发证据 |
| 组织测试注释仍写项目表 merchant_id | 技术性过时注释，非用户可见声称 |

分类复核：上述均为观察或既有登记，不是添加、不是未修冲突。

---

## 8. 限制（未做不得声称）

- 未用浏览器工具，未做真实点击/焦点/滚动核验。
- 未对运行中后端发 HTTP。
- 未跑 Maven / 数据库。
- 未验证持 U 盘密钥会话下删除申请的服务端直删（只读源码）。
- 工作区其它脏文件不在本模块，未审。

---

## 9. 最终票

| 项 | 值 |
|---|---|
| 六源全文覆盖 | 是（2129 行） |
| 21 映射逐项 | 是（GEN-014 明示不适用） |
| 三线 | 是 |
| 可执行证据 | 正本 59 passed / typecheck 0 |
| 未答 questions | 无 |
| 新缺陷 | 无 |
| 锁外判据 | 无 |
| 条件通过 | 禁止；本票不使用 |
| **本席 B5-r5** | **PASS** |

本票不等于双 PASS 或模块收口。发布/推送/合并由后续 Codex 按操作员指令执行；本席不能发布。
