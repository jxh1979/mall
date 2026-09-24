CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B1 席乙（Grok）修复前全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B1（修复前全量；本模块首次开工，历史未审）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M023 商户档案、商户类型与商户侧门户／网页与验证／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock V0.11；提示词 `160-audit-prompt.md` V0.17（本机路径 `.scratch/module-recheck-20260908/160-audit-prompt.md`）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`89686538c7146a9891401b00e51369629ee1cfa4`
- 本席未读取 `modules/M023-b1-self-raw.md`，未依赖席甲发现；未改计划 / findings / ledger / 业务代码；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席
- 本席步骤结论：**FAIL**

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/plugins/cache/`、`/home/james/.grok/bundled/skills/`。按提示词 §6 收窄口径，禁止项未调用。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读全文；用户指令优先于技能 |
| using-arc | Arc plugin `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理/禁改业务**，不跑 Arc 默认派评审 |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft ask` / `graft grep` / `graft skeleton` 定位门户后端与页面调用 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、`grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 页面相对后端契约偏浅：读不存在的键、确认按钮无服务端实现 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 金额/KPI 失真：从页面 `totalAmount` 追到 `MerchantBillRecord.totalAmountCent` |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借数据流追踪；不改代码、不加探针 |
| tdd | `.../engineering/tdd/SKILL.md` | 只作测试有效性透镜（B1 不写测试）；夹具形状与后端记录不一致 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前实跑 vitest |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：伪确认、臆造列、把「无表单」写成正向断言 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 按步骤+完成标准组织，供 B2 归并 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；本 raw 编号清单；CLI 不回带正文（提示词适配器） |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选；本轮无 git 写，未装 hook |
| audit（Arc） | Arc `skills/audit/SKILL.md` | **缺口/未按本体执行**：本体要派子代理写 `docs/arc/audits/`，与双席+§7 封闭清单冲突 |
| audit-context-building | Trail of Bits skill | **缺口/未按本体执行**：本体强制派 function-analyzer 子代理写 `audit-context/`，用户禁第三席 |
| batch-grill-me | 本机 skill 目录无独立 SKILL | **缺口**；用 grilling 自问自答代替 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，B1 不调用 |
| ponytail / TDD 落测试 | 存在 | 仅 B3；B1 禁止修复 |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉，未调用 |

graft 本轮检索（ask+grep+skeleton）约节省 ~123k tokens，用于定位 `MerchantPortalController/Service`、scope/403、页面符号。

---

## 1. 范围、哈希、行数（实算，不抄期待值）

独立命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
git ls-files -z -- <6 files> | xargs -0 cat | wc -l
# 970
```

sidecar 与 ledger 顶部基准均为 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席只读比对，未改 ledger）。

计划写有效内容 991；现行 `git ls-files | xargs cat | wc -l` = **970**（以实算为准）。

| 文件 | 物理行 | SHA256 |
|---|---:|---|
| `apps/web/src/admin/pages/MerchantScopePage.tsx` | 216 | `086c74d98e46c8d56c611d4c83980a1f89313bfe5b7e7007a9f029c84f05696a` |
| `apps/web/src/admin/pages/MerchantScopePage.test.tsx` | 144 | `0a12649e5377c583c705881841857a43d4bfe7eee06494ed6ea667a14cbeed18` |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx` | 178 | `7e7328744b216cbd0897fa8455e6ab5c7f46134d614dc4b4137ea327b1eeea2d` |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx` | 142 | `8e7a60b67eb0711f5d0da46d492d2fc6a27db7ba7effa85909af4a37fa5ede8d` |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx` | 158 | `28428b650f8b874818cdcaf4893601270af1840cf92ae31ceb8c3699c22887c1` |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx` | 132 | `0e4d440ed8047eb36092cb7e6f19895012103ed3df825781c9d47d0fbccfe5fa` |

对 origin/main：`6 files changed, 105 insertions(+), 126 deletions(-)`（与派发事实一致）。

路由（只读，非本模块文件，作调用门控核实）：

- ADM-MER-002 商户授权工作台 `/admin/merchant-portal/scope`：`merchant.service_work_order.view` + `merchant.billing.view`，无 `accountForms`
- ADM-MER-005 我的对账 `/admin/merchant-zone/settlements`：`merchant.portal.settlement.view`，`accountForms: ["merchant"]`
- ADM-MER-006 我的合同 `/admin/merchant-zone/contracts`：`contract.view`，`accountForms: ["merchant"]`

全文覆盖：六个文件每一行均已读，不抽样。测试 22 个 `it(...)` 均已读。

---

## 2. 调用对象只读追踪（现行后端，非推测）

### 2.1 MerchantScopePage → 工单 / 账单

页面：`GET /api/v1/merchant-service/work-orders`、`GET /api/v1/merchant-billing/bills`（`MerchantScopePage.tsx:97-100`）。无请求体。`AdminApi.get` → `unwrap`：信封 `data.items` 拍成数组，丢 `total/page/size`（`adminClient.ts:42-49`）。

后端：

- `MerchantServiceController.listWorkOrders`：`requirePermission("merchant.service_work_order.view")`；`MerchantServiceService.listWorkOrders` 调 `authorizationService.currentMerchantScope()` 再 `findWorkOrdersByScope`。`currentMerchantScope`（`AuthorizationService.java:425-431`）无 `merchantId` **直接 throw 403 `MERCHANT_SCOPE_REQUIRED`「此操作必须在商户范围内进行」**，不返回 empty。
- `MerchantWorkOrderRecord` 字段含 `merchantId, spaceId, contractId, workOrderType, priority, status, title, ...`，**无** `authorizedScope` / `auditEvidenceRef`。
- `MerchantBillingController.listBills`：`merchant.billing.view`；`MerchantBillRecord` 含 `billNo, totalAmountCent, currency, status, lines, billPeriodStart/End, dueDate`，**无** `title` / `totalAmount` / `authorizedScope` / `auditEvidenceRef`。账单状态集合含 `issued, viewed, overdue, confirmed`（`MerchantBillingService.java:53-54, 209`）。
- Jackson SNAKE_CASE → 前端 unwrap camel：`total_amount_cent` → `totalAmountCent`。

页面另：无 `merchantId` 时不发请求、`throw new Error(TEXT.scopeMissing)`（`:89-96`）；有 `merchantId` 后再客户端 `matchesMerchant` 过滤（`:101-102`）。与后端 fail-closed 同向，但读到的行按臆造字段展示。

「确认授权范围」（`:111-122`）只 `setActionResult`，无 POST。`AdminApp.tsx:734` 把结果 `JSON.stringify` 进可见 `<pre data-testid="admin-action-result">`。

### 2.2 MerchantZoneContractsPage → 我的合同

`GET /api/v1/merchant-portal/contracts`（`:107-109`）。后端 `MerchantPortalController:32-36` 默认 `page=1,size=500`；`MerchantPortalService.listMyContracts`：`contract.view` + `requireMerchantScope()`（无归属 403 `MERCHANT_SCOPE_REQUIRED`）+ `listContractsByMerchant`。视图 `MerchantContractView`：`id, merchantId, effectiveDate, expiryDate, lifecycleStatus, depositCent, projectId, spaceIds, renewedFromContractId`。页面 `depositCent/100` 显示「元」，生命周期用本地字典（草拟/待确认/…/归档），与枚举速查表「合同状态」中文一致。前端 `usePagedRows(..., 10)`，**未**调 `getPaged`、**未**传 `page/size`。

### 2.3 MerchantZoneSettlementsPage → 我的对账

`GET /api/v1/merchant-portal/settlements`（`:81-84`）。后端 list 同 fail-closed + `merchant.portal.settlement.view`。`ContractSettlementRecord`：`id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, ...`。`settlementItems` 由 `TerminationSettlementMath` 写入 `amountCent` / `depositCent` / `offsetCent` / `netRefundCent` 及中文 `note`（未清应收、押金处置、提前退租违约金）。

后端另有写路径（本页不调用）：`POST /api/v1/merchant-portal/settlements/{settlement_id}/dispute`，body `idempotencyKey, operatorNote, reason`；`ApiRequests.requireIdempotencyKey`；`idempotencyService.execute` 同键只认一次；只合并 `dispute_reason`，**不改 status、不暂停付款**；跨商户 403 `CROSS_MERCHANT_FORBIDDEN`。

页面 KPI「未结清」= `status !== "settled"`（`:88`）。本地状态字典无 `voided`；`ContractService.finalizeSettlementWithLedger:1301-1304` 会把重复路径结算单标 `voided`。

### 2.4 门控组件

六文件内 **零** `Can` / `PermissionButton` / `HighRiskConfirm`。读页靠 `AdminApp` `PermissionGate`（无权限：「权限不足：当前账号不能访问该后台页面。」）+ 商户专区 `accountForms: ["merchant"]`（不匹配：「此页面供商户账号使用，当前账号无需使用。」）。`navigateTo`（`AdminApp.tsx:545-551`）缺权限或形态不匹配时 **直接 return，无提示**。`TraceLink` 只要有 `navigateTo` 就把引用画成按钮。

`useAdminResource` 把 `Error.message` 当作中文业务话术；403 走 `ApiError`，不把列表降级成全量。合约/结算页不在客户端预判 `merchantId`，无归属时打到后端 403。

---

## 3. 第一性原理三问（每页）

### 3.1 MerchantScopePage（商户授权工作台）

1. **为何必须存在？** 删掉后，HTML 商户管理「不能看平台全局和其他商户数据」（业务地图 L1310）仍主要由后端 scope 与「我的工单/我的账单」承担。本页是治理侧「授权范围」工作台，属 L1310/PGM L749 下位实现，不是五自助项之一。答得上「有上位」，不能因答不上就删。
2. **声称行为是否执行验证？** 单测 9/9 绿（见 §7），但夹具键是 `total_amount` / `authorized_scope` / `title`，与 `MerchantBillRecord`/`MerchantWorkOrderRecord` 不一致。对真实 GET 响应，金额列走 `moneyYuan(row.totalAmount)` → `"-"`，KPI「账单金额」`Number(undefined??0)` 求和 → **「0.00 元」**，KPI「授权范围」长度恒 0。声称「只展示当前授权范围」在真实现上展示空范围与零金额。
3. **未验证前提？** 前提「列表行带 `totalAmount`/`authorizedScope`/`auditEvidenceRef`/`title`」未被后端记录满足。前提「确认授权范围会留痕」无对应 POST / 审计事件。证据 TEXT 写 `merchant.portal.read`，路由实际只要工单 view + 账单 view。

### 3.2 MerchantZoneContractsPage（我的合同）

1. **为何必须存在？** 业务地图 L1322「我的合同（关键条款、租期、到期情况）」；删掉该页 HTML 该句做不到。本页有租期/状态/押金/空间，**无「关键条款」正文**（后端 `MerchantContractView` 也故意最小披露）。关键条款缺口记观察，不把合同域条款页扩进本模块。
2. **声称行为？** GET 契约、中文状态/日期/金额、KPI 聚焦、行抽屉、TraceLink stopPropagation：单测 7/7 覆盖这些 UI 声称。未跑 403、未跑真实分页信封、未跑空间下钻目标页是否为空间对象。
3. **未验证前提？** 前提「点合同/空间能下钻到对象页」：合同链到 `ADM-CONT-001`（权限 `contract.draft.manage`），本页入场只要 `contract.view`；空间链到 `ADM-GOV-007`（空间**类型**字典，不是空间实例）。`navigateTo` 失败静默。

### 3.3 MerchantZoneSettlementsPage（我的对账）

1. **为何必须存在？** L1322「我的对账（结算与分成明细，有疑问可留言提异议）」。删掉则该自助项无承载。
2. **声称行为？** 页面文案改成「退租金额异议在退租审批中提出。本页只读」（`:11-16`）。与枚举速查表 L252「商户对退租结算金额的异议在退租审批流程中提出并解决」同向，与 **L1322 同一句里的「结算与分成明细 + 留言提异议」** 以及后端已上线的 POST `/dispute` 反向。表不渲染 `settlementItems`（分位金额），商户第一次打开对账看不到押金净额/未清应收/违约金。单测把「没有异议表单」写成正向断言（`:50-59`），改坏「缺入口」不会红、补入口反而可能红。
3. **未验证前提？** 前提「本页只读即符合 HTML」把 L252 当成唯一通道，未验证 L1322 留言句与后端写路径。前提「`status !== settled` 即未结清」把 `voided` 算进未结清。

---

## 4. 顺从式：21 条冻结判据

每条只引用 lock 已有 ID。HTML 相关原文已读：业务地图 L1310–L1332、L1322、L1827–L1850；治理地图 L256–L304（全链可点击、全中文、重复提交只认一次）、L748–L777 商户侧。

| ID | 适用？ | 证据（本模块） |
|---|---|---|
| BSM-001 | 适用（全局原则：可溯源/全链可点击；经营结果含对账结算） | 合同/结算/授权页均有行点击抽屉；金额/空间/作废单等见 GR-01/04/05/06 |
| BSM-003 | 部分适用 | 本三页无新建/存档删除；权限分岗走路由 PermissionGate + accountForms，页内无按钮级 Can。引导式新建不适用 |
| GEN-001 | 适用 | 伪确认按钮无 HTML 承载且伪成功（GR-02）；臆造列无后端字段（GR-01） |
| GEN-002 | 适用 | 授权页金额/范围 KPI 对真响应恒零或「-」（GR-01）；对账不展示 settlementItems（GR-04） |
| GEN-003 | 适用 | 22/22 vitest 绿；授权夹具用 `total_amount` 非 `total_amount_cent`；对账测试锁定「无表单」。改坏真契约不一定红 |
| GEN-004 | 弱适用 | 本页无用户输入写路径（对账写路径被删）。客户端 `matchesMerchant` 不替代服务端 |
| GEN-005 | 适用 | 门户 GET 后端 `requireMerchantScope` fail-closed；授权页无 merchantId 不请求。平台管理员打开 ADM-MER-002 走客户端文案，不 dump。合约/结算靠后端 403。页内无 PermissionButton |
| GEN-006 | 弱适用 | 前端无持久化写。后端 dispute 合并理由受 500 字约束（他模块已修，本页未接） |
| GEN-007 | 适用（负向） | 后端 dispute 幂等键只认一次；本页无提交，重复提交原则在本模块写路径上未落地。伪确认连点只改本地 pre，无资金风险 |
| GEN-008 | 适用 | 账单金额键、账单状态码表、结算 voided、空间 TraceLink 目标页均与后端/路由不一致（GR-01/03/05/06） |
| GEN-009 | 适用 | 加载/错误/重试：`AdminDataState` 中文「加载中...」「读取失败」+重试。授权页 `empty={false}` 自绘空态。403 不降级成全量。合约/结算缺 403 单测 |
| GEN-010 | 适用 | 未见注入；动作结果 JSON 含 merchantId（非秘密）。无明文秘密 |
| GEN-011 | 适用 | `AuditTrailHint` 把英文事件名折进「操作留痕」details（技术折叠，用户裁决过）。伪确认 `evidenceAttachmentIds: ["att-merchant-scope-confirm"]` 无真实附件（GR-02） |
| GEN-012 | 观察 | GET 默认 size 500 / 工单 LIMIT 100，前端再切 10 条；超限静默截断。单商户合同/结算首次超 500 不常见，不立项 |
| GEN-013 | 不适用本切片 | 无构建配置 |
| GEN-014 | 不适用 | 无迁移 |
| GEN-015 | 适用 | 主文案中文；行键盘 Enter/Space；抽屉 Esc。GR-01/03/05 英文码或错译；GR-06 可点无反馈；GR-02 英文 JSON 预 |
| GEN-016 | 适用 | 页内 evidence 写 `merchant.portal.read` 与路由不符（GR-02）。e2e 仍找异议表单属他模块观察 |
| GEN-017 | 适用 | 本 raw 按冲突→细化→技术性→添加；未启用锁外判据 |
| GEN-018 | 适用 | 结论落到 HTML 原文 / 源码行 / vitest 输出 / `sha256sum` |
| PGM-001 | 适用 | 全链可点击、全中文、重复提交只认一次均落在本三页。见 GR-01/02/03/04/05/06 |

BSM-001/PGM-001 锚点正文已读（lock 定义区全文 + HTML L256 段与 L505 段）。L749 商户侧「不是只读页面」与对账页自称「本页只读」张力见 GR-04。

---

## 5. 对抗式（grilling 结构，自问自答，不等操作员）

❓ **Q1 授权工作台的金额列在真 GET 下显示什么？**
夹具 `total_amount: 100` → unwrap `totalAmount` → 「100.00 元」。真记录只有 `totalAmountCent`。`moneyYuan(undefined)` 为 `"-"`；KPI 求和 `Number(row.totalAmount ?? 0)` 为 0 → 「0.00 元」。首次打开有账单的授权页即可复现。

❓ **Q2 改坏金额键，现有测试会红吗？**
不会。测试夹具不包含 `total_amount_cent`。GEN-003 失败。

❓ **Q3 平台管理员打开授权页会不会看到全量？**
`!user.merchantId` 短路，不发 GET，无行。测试「平台管理员无商户范围时不看全量」会红若出现行。**不会**断言出现「缺少商户授权范围」文案——空态与错误态都无行。后端 `currentMerchantScope` 本就会 403，不走到 `findAllBills`。

❓ **Q4 点「确认授权范围」是否留痕？**
不调 API。可见结果是英文 JSON：`merchant_scope_confirmed` / `merchant_authorized_scope_checked` / `unscoped`。伪造 `att-merchant-scope-confirm`。首次点击即可。

❓ **Q5 账单状态 `issued`/`viewed` 显示什么？**
`formatBusinessStatus("issued")` = 「已出票」（票务义）；`viewed` 不在 `statusLabels.ts`，原样英文。兄弟页 `MerchantZoneBillsPage` 把 `issued` 译「已出账」。授权页未用账单专用字典。

❓ **Q6 商户打开「我的对账」如何提异议、如何看数字？**
无表单、无 POST。`settlementItems` 不渲染。L1322 要求明细+留言；L252 要求退租金额走退租审批。后端 POST `/dispute` 仍在。测试锁定无表单。首次正常对账即可。

❓ **Q7 `voided` 结算单算未结清吗？会不会英文外泄？**
`openRows = status !== "settled"` → voided 计入未结清 KPI。本地字典无 voided，单元格显示 `voided`。共享字典若调用会是「已失效」，本页没用。

❓ **Q8 点合同上的空间/合同链接，商户侧会发生什么？**
空间 → `ADM-GOV-007` 空间类型字典（错对象）。合同 → `ADM-CONT-001` 需 `contract.draft.manage`，商户专区入场只需 `contract.view`。`navigateTo` 失败静默。链接仍显示为按钮。首次点击即可。

❓ **Q9 重复提交结算异议？**
本页无提交。后端幂等存在但无调用方。不因此补前端需求以外的重试器。

❓ **Q10 无权限/错形态/加载失败是否可用？**
路由级中文拒绝可用。页级 `AdminDataState` 中文错误+重试。合约/结算 403 依赖 `ApiError` 中文消息，本模块测试未打 403。不单独立项（观察）。

---

## 6. Findings（临时 GR；正式 ID 待 B2）

判定顺序：冲突 → 细化 → 技术性 → 添加。依据四类之一。真实首次正常路径。

### GR-01 授权工作台按不存在的字段展示金额与授权范围；测试夹具与真记录同形假绿

- **位置：** `MerchantScopePage.tsx:46-51, 97-108, 139-151, 170-172, 196-200`；`MerchantScopePage.test.tsx:8-15, 136-142`；对照 `MerchantBillRecord.java:7-23`、`MerchantWorkOrderRecord.java:6-36`、`adminClient.ts:42-49`
- **原文：** 页面 `moneyYuan(row.totalAmount)`、`bills.reduce(... Number(row.totalAmount ?? 0))`、`row.authorizedScope`、`row.title`；账单记录字段为 `totalAmountCent`，无 `title`/`authorizedScope`/`auditEvidenceRef`。
- **上下文：** unwrap 后真键是 `totalAmountCent`。KPI「账单金额」对真实列表为「0.00 元」；单元格为「-」；「授权范围」计数为 0。工单真实 `spaceId`/`contractId` 不展示。
- **正常触发：** 商户或具备两 view 权限的账号打开 ADM-MER-002，后端返回本商户账单。
- **影响：** 金额与授权范围 KPI 系统性说错；全链可点击缺真实对象。
- **最小拟处置：** 读 `totalAmountCent` 并 `/100` 加「元」（照合约页 `depositCent` / 账单专区 `amountCent`）；授权范围改为后端真实字段或删除臆造列；展示 `spaceId`/`contractId` TraceLink；夹具改为 `total_amount_cent` 且断言非「0.00 元」。
- **四分类：** 细化（L1310 授权范围内查看的下位实现）
- **工程缺陷：** 成立
- **冻结 ID：** GEN-002, GEN-003, GEN-008, GEN-015, PGM-001, BSM-001
- **依据：** 可执行产物（vitest 夹具形状）+ 源码对照（类型 4 命令：`npm run test:unit -- <三文件>` 22 passed；类型 1 非直接，用后端记录定义）

### GR-02 「确认授权范围」不调后端、对用户展示英文 JSON，并声称不存在的权限码与附件

- **位置：** `MerchantScopePage.tsx:12-15, 111-122, 203-205`；`AdminApp.tsx:734`（只读对照）
- **原文：** `setActionResult({ status: "merchant_scope_confirmed", audit: "merchant_authorized_scope_checked", merchantId: user.merchantId ?? "unscoped", ..., evidenceAttachmentIds: ["att-merchant-scope-confirm"] })`；evidence：「权限由 merchant.portal.read、merchant.service_work_order.view、merchant.billing.view 控制。」
- **正常触发：** 点「确认授权范围」。
- **影响：** 界面出现英文状态/审计名；操作员以为已留痕；`merchant.portal.read` 不是本路由 requiredPermissions。
- **最小拟处置：** 删除该按钮及伪结果；或改为真实只读核对并不输出英文 JSON。修正/删除错误权限码文案（可留在技术折叠）。
- **四分类：** 细化（授权工作台的操作控件；HTML 无独立「确认授权」条目，但隶属本页）——若 B2 认为构成无上位业务动作，可改判添加。本席不另启 LOCK-AMEND。
- **工程缺陷：** 成立
- **冻结 ID：** GEN-001, GEN-002, GEN-011, GEN-015, GEN-016, PGM-001
- **依据：** 源码行 + AdminApp 可见 `<pre>`（类型 4 读文件）

### GR-03 授权页账单状态走通用 `formatBusinessStatus`，与账单真状态冲突并可能英文外泄

- **位置：** `MerchantScopePage.tsx:147, 172`；`statusLabels.ts:20 issued:"已出票"`；`viewed` 不在表中；对照 `MerchantBillingService.java:53-54` 与 `MerchantZoneBillsPage.tsx:18-25`（他页，只作正确对照，不审）
- **正常触发：** 列表出现 `status=issued` 或 `viewed` 的商户账单。
- **影响：** 「已出票」是票务义；`viewed` 原样英文。违反全中文与枚举一致。
- **最小拟处置：** 账单列改用与账单专区相同的中文字典（已出账/已查看/…），未知码中文兜底而非原文。
- **四分类：** 细化
- **工程缺陷：** 成立
- **冻结 ID：** GEN-008, GEN-015, PGM-001
- **依据：** HTML 全中文原文（PGM-001 lock 摘录）+ 源码字典

### GR-04 「我的对账」不展示结算/分成数字、不提供留言；测试把缺入口写成规范

- **位置：** `MerchantZoneSettlementsPage.tsx:11-16, 81-125`（无 POST、无 `settlementItems` 渲染）；`.test.tsx:50-59`；对照 `MerchantPortalController.java:46-54`、`MerchantPortalService.java:113-168`、`TerminationSettlementMath.java:40-75`；HTML 业务地图 L1322；枚举 L252
- **HTML 原文（L1322）：** 「我的对账（结算与分成明细，有疑问可留言提异议）」；同条「商户只能就数字提出异议……提出异议期间商户的付款义务不暂停」。
- **HTML 原文（L252，相关）：** 「商户对退租结算金额的异议在退租审批流程中提出并解决」。
- **页面原文：** 「查看本商户结算单。退租金额异议在退租审批中提出。本页只读，不改变结算状态、不暂停付款。」
- **正常触发：** 商户打开 ADM-MER-005。有结算清单时看不到分位金额；有疑问时无处留言。后端 POST 仍接受 `reason`+幂等键且不改状态。
- **影响：** L1322 该自助项的两个括号内容（明细、留言）均未落地；「就数字提出异议」没有数字。测试会阻止把表单加回来（除非先改测试）。e2e 仍找 `merchant-zone-settlement-dispute-form`（他模块，见观察）。
- **最小拟处置：** 渲染 `settlementItems`（`amountCent`→元、中文 note）；留言走已有 POST（`idempotencyKey`+`reason`），文案保持「不改状态、不暂停付款」。L252 与 L1322 并存时：退租金额审批通道不在本模块发明；本页至少恢复「数字可见 + 可留言」。若操作员以 L252 否决留言，仍须展示明细；留言项再走 B2。
- **四分类：** **冲突**（与 L1322 明文「结算与分成明细，有疑问可留言提异议」矛盾；L252 不能把 L1322 括号删掉）
- **工程缺陷：** 成立
- **冻结 ID：** GEN-002, GEN-003, GEN-008, GEN-015, PGM-001, BSM-001
- **依据：** HTML 逐字原文（类型 1）+ 后端控制器/算法源码（类型 4 读文件）

L1322 与 L252 通道并列，不是 HTML 文件损坏，**不**按铁律 4 申请改 HTML；在观察中登记张力。

### GR-05 作废结算单显示英文 `voided` 并计入「未结清」

- **位置：** `MerchantZoneSettlementsPage.tsx:24-31, 88-90, 120-121`；对照 `ContractService.java:1301-1304` `status, "voided"`
- **正常触发：** 同一合同重复结清路径产生 voided 单后打开我的对账。
- **影响：** KPI 未结清偏大；状态英文外泄。HTML 退租六态无「作废」，技术态仍须中文。
- **最小拟处置：** 字典补「已作废」（或等价中文）；未结清排除 `voided`/`settled`。
- **四分类：** 细化
- **工程缺陷：** 成立
- **冻结 ID：** GEN-002, GEN-015, PGM-001, GEN-008
- **依据：** 源码（类型 4）+ PGM-001 全中文原文（类型 1）

首次正常：退租确认竞态/重复单在合同域是写过的真路径，不是臆造极端。

### GR-06 合同页空间/合同 TraceLink 目标错误或对商户权限静默无响应

- **位置：** `MerchantZoneContractsPage.tsx:61-76, 130-141`；`TraceLink.tsx:14-28`；`AdminApp.tsx:545-551`；`adminRoutes.ts:19, 50, 92`
- **原文：** 空间 `pageId="ADM-GOV-007"`；合同 `pageId="ADM-CONT-001"`。ADM-GOV-007 标签「空间类型字典」权限 `governance.space.view`；ADM-CONT-001「合同生命周期工作台」权限 `contract.draft.manage`；本页只要 `contract.view` + merchant 形态。
- **正常触发：** 商户在我的合同点击空间或合同链接（按钮已渲染）。
- **影响：** 空间下钻到类型字典，对象对不上；合同下钻常因权限直接 return，用户以为可点。违反「不允许无法点击溯源的孤立数据」的可点承诺（看起来可点、点了无源）。
- **最小拟处置：** 空间改为承载空间实例的页（如拓扑 ADM-GOV-001，须核权限后只读可达）；合同下钻对仅有 `contract.view` 的账号应落在本页抽屉/本专区，或给中文「无权下钻」而非静默。不把合同工作台写权限塞给商户。
- **四分类：** 细化（PGM-001 全链可点击的下位实现）
- **工程缺陷：** 成立
- **冻结 ID：** PGM-001, BSM-001, GEN-015, GEN-005
- **依据：** HTML PGM-001 原文（类型 1）+ 路由表源码（类型 4）

授权页商户 TraceLink → ADM-GOV-002（`merchant.profile.view`）对仅有工单/账单 view 的账号同样可能静默，并入本条，不另编号。

---

## 7. 可执行证据（vitest）

命令（`apps/web`，仓库既有 `test:unit` = `vitest run`）：

```text
npm run test:unit -- src/admin/pages/MerchantScopePage.test.tsx src/admin/pages/MerchantZoneContractsPage.test.tsx src/admin/pages/MerchantZoneSettlementsPage.test.tsx
```

输出摘要：

```text
 ✓ MerchantZoneSettlementsPage.test.tsx (6 tests) 89ms
 ✓ MerchantZoneContractsPage.test.tsx (7 tests) 101ms
 ✓ MerchantScopePage.test.tsx (9 tests) 115ms
 Test Files  3 passed (3)
      Tests  22 passed (22)
 Duration  590ms
```

exit 0。22 绿 **不能** 证明真契约（GR-01/03/04）。jsdom `Window.scrollTo` 未实现警告，与断言无关。

---

## 8. 观察（不入本模块修复队列）

1. **跨模块 e2e：** `tests/e2e/full-admin-remaining.spec.ts:917` 仍 `getByTestId("merchant-zone-settlement-dispute-form")`。本页已无该节点。属 e2e 模块，转入登记属实。
2. **M020-001：** 门户对无商户归属 fail-closed 403。本三页中合约/结算依赖该后端；授权页客户端短路。后端已是现行事实，本模块不重复立项。
3. **HTML 张力：** L1322 留言 vs L252 退租审批。本席 GR-04 按冲突处理 L1322；不申请改三 HTML。
4. **我的合同缺「关键条款」正文：** L1322 括号含关键条款；`MerchantContractView` 有意最小披露。条款载体若在合同域他页，不扩 M023。
5. **费用总表 / 综合账单 / 我的账单 / 我的工单 / 费用通知：** L1322 其余自助项由 ADM-MER-003/004/007 等承载，非本六文件。
6. **分页：** 后端默认 500/100，前端再 10 条切片；超限静默。单商户首次正常少见，不立项。
7. **TechDetails 内 API 路径与事件名：** 用户裁决折进「技术详情」；PGM-001 四样例外可覆盖内部标识。不立项。
8. **MerchantBillingService.listBills 的 findAllBills 分支：** 若 `currentMerchantScope` 抛错则不可达。他模块死代码，不审。
9. **转入 M020 观察之「无异议入口」：** 本席独立复核后作为 GR-04 入队，不是「观察即可」。
10. **六文件无 Can/PermissionButton/HighRiskConfirm：** 读页用路由闸足够；写路径（异议）缺失见 GR-04。

无锁外新判据。无添加项需在 B1 发问（GR-02 若 B2 改判添加再问）。无 HTML 修改申请。

---

## 9. ponytail-review（只列可删，不修）

- `MerchantScopePage.tsx:111-122, 203-204`: delete: 伪确认无后端。Nothing replaces it until a real command exists.
- `MerchantScopePage.tsx` 授权范围/留痕证据列: delete or replace with `spaceId`/`contractId`/`totalAmountCent`.
- `MerchantZoneSettlementsPage.test.tsx:50-59`: delete: 把缺 HTML 入口写成正向规范. Assert the HTML path instead.
- net: 授权页可短一截；对账页若接明细+留言会变长（那是冲突修复，不是膨胀）。

---

## 10. 文件覆盖表（B1 全量）

| 文件 | 覆盖 | 备注 |
|---|---|---|
| MerchantScopePage.tsx L1–216 | 全文 | TEXT、KPI URL、useAdminResource、过滤、伪确认、两表、抽屉 |
| MerchantScopePage.test.tsx L1–144 | 全文 | 9 it：KPI/中文/隔离/无范围/抽屉 |
| MerchantZoneContractsPage.tsx L1–178 | 全文 | 字典、money(cent)、KPI、GET portal/contracts、抽屉、TraceLink |
| MerchantZoneContractsPage.test.tsx L1–142 | 全文 | 7 it：聚焦/中文状态日期/抽屉/stopPropagation |
| MerchantZoneSettlementsPage.tsx L1–158 | 全文 | 只读文案、GET portal/settlements、无 dispute、voided 未覆盖 |
| MerchantZoneSettlementsPage.test.tsx L1–132 | 全文 | 6 it：无表单、未结清聚焦、中文触发状态、抽屉 |

辅助只读（不计入模块行数、不作越界 finding 主体）：AdminApi unwrap、useAdminResource、adminRoutes、AdminApp navigateTo/actionResult、TraceLink、AdminDataState、statusLabels、MerchantPortalController/Service V0.01、AuthorizationService.currentMerchantScope、MerchantBillRecord、MerchantWorkOrderRecord、ContractSettlementRecord、TerminationSettlementMath、三 HTML 指定段、criteria-lock 21 条定义。

---

## 11. 步骤结论

- 映射 21 条均已核对并留下适用证据。
- 三线均执行。
- 六文件全文覆盖。
- 有工程缺陷 GR-01…GR-06，故 **本席 B1 = FAIL**。
- 不存在有条件通过。
- 修复队列建议（B2 定稿）：GR-01/02/03/04/05/06 均「是」。观察 1–10 否。
- 无 BLOCKED-ASK（B1 不发 2.2）。无 LOCK-AMEND。

本席 B1 结束。

## M023 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **33144** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M023 FILES 6 / 锁映射 21 / 计划冻结 991 行；现行物理行 970 以 B1 实算为准）
- 正本：`/home/james/complex-management`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `89686538c7146a9891401b00e51369629ee1cfa4`。B3=0。无源修复。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，**不读**席甲 raw 在 23658 字节之后的 V0.02 B2 结论段。
- 本席 B2-r1：**PASS**（001–006 逐 ID 全 PASS；添加 0、不发 2.2。非阻断执行注见 001 上位措辞、006「条目中文名」页内字典闭集，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/前缀字节/候选段/两份 B1 V0.01/HTML 锚句/MerchantPortalService DISPUTABLE 与 disputeOnce/TerminationSettlementMath.settlementItems/AdminApi 幂等注入/AdminPageProps.effectivePermissions/ADM-MER-006 路由；未对任一 `complex_test*` 库做写操作，未重审六文件模块结论、未跑 vitest。

此票只判断 findings.md V2.71 顶部「当前 M023 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占锁哈希字段行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M023-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 33144 | `9ed7728a96bf3d7b4a28622995d3c68e4107b6d2176ecd8e77db04bcde2f7404` |
| `docs/audit-2026-09/modules/M023-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 23658 | `aa3f811f350de0f9615b5e5148800aff0ec0a4d072316a366a4ca5bd520f70aa` |
| `docs/audit-2026-09/findings.md` | V2.71 | 1051242 | `746ae14bad44b198bd97ced7f97f2944be1aeb013dc96ba2df1bba72c3d531c5` |

席甲 raw 全文现大于 23658 bytes（本席用前 23658 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

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

上位句（本席 B2 只核分类与 006 并存读法所需原文，不重审模块）：

- 业务地图 L1322：`我的对账（结算与分成明细，有疑问可留言提异议）`；同条 `商户只能就数字提出异议……提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补。`
- 枚举速查 L252 退租结算状态：`商户对退租结算金额的异议在退租审批流程中提出并解决`（业务地图变更与退租段同句）。
- 治理地图 L287：界面全中文、状态/枚举取值必须全部为中文。
- 治理地图 L256 段：全链可点击、留痕。

`questions/M023*`：不存在。

M020 已闭环（ledger：B5-r1 双 PASS，检查点 PR #461 已合并）：上位正是 L1322「可留言提异议」+「提出异议期间付款义务不暂停」；现行 `MerchantPortalService.submitSettlementDispute` 注释与实现为 note-only（只记理由，不改 status、不暂停付款）；M020-007 把 OpenAPI「挂起为有争议」列为 XM，代码不为对齐契约改回 disputed。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：006 冲突+L1322/L252 并存、001 不新增列、002 删除伪动作不走 2.2 |
| grilling | 只借追问结构自查：差集有无漏并/改写、006 是否发明规则、L1322/L252 要否升 2.2、有无应升添加 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/HTML 行号/DISPUTABLE 原文/settlementItems 键/AdminApi 幂等/ADM-MER-006 路由 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审六文件的模块结论 |
| codebase-design / ponytail-review | 最小是删幽灵列/伪按钮、字典中文、可达下钻或纯文本、接既有 POST；不发明状态机 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 已实际调用 ask/grep：`submitSettlementDispute` / `DISPUTABLE` / `disputeOnce` / `settlementItems`；未管道 head/tail |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…06、观察 1…10、席甲 CX-M023-001…005、OBS-1…5）

漏并：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–006 或只登记段。无单席独有缺陷被丢。无本批新开「否」。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M023-001 | GR-01 + CX-M023-001 | 细化/是/是（删幽灵列与 KPI，金额读 `totalAmountCent`；不新增 spaceId/contractId 列） | **PASS（细化/是/是）** |
| M023-002 | GR-02 + CX-M023-002 | 细化/是/是（删除伪动作，不走 2.2） | **PASS（细化/是/是）** |
| M023-003 | GR-03 + GR-05 字典 + CX-M023-003 | 细化/是/是 | **PASS（细化/是/是）** |
| M023-004 | GR-05 KPI + CX-M023-004 | 细化/是/是 | **PASS（细化/是/是）** |
| M023-005 | GR-06 + CX-M023-005 | 细化/是/是 | **PASS（细化/是/是）** |
| M023-006 | GR-04（席甲 B1 判不成立，B2 核源后撤回） | 冲突/是/是（采本席：L1322 明文） | **PASS（冲突/是/是；处置同时满足 L1322 与 L252，无业务发明）** |

冲突入队 1（006）。细化入队 5（001–005）。技术性入队 0。观察否：双方 OBS 按候选「只登记不入队」。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–006（6）；「否」本批新开 = 0。

**整批候选：同意。**

---

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-01 + CX-001 | 001 | 同根：授权工作台读 `totalAmount`/`title`/`authorizedScope`/`auditEvidenceRef`，真记录金额键是 `totalAmountCent`，后三者后端 0 处；夹具 `total_amount/title/authorized_scope` 假绿。最小采席甲收口：删不存在列与 KPI、金额读分位；本席 B1「展示 spaceId/contractId TraceLink」候选明确标可选增强、不入本条——属边界收窄不是改写缺陷。事实未改写。 |
| GR-02 + CX-002 | 002 | 同根：`confirmScope` 0 次 API、伪英文 JSON/虚构附件、`merchant.portal.read` 与 `merchant.scope*` 事件后端 0 处。处置采删除伪按钮+改真实事件/权限码（本席 B1 允许「删除或改为真实只读核对」；删除不是新业务动作）。分类细化、不走 2.2。未改写。 |
| GR-03 + GR-05 字典 + CX-003 | 003 | 同根三面：授权页 `formatBusinessStatus` 把账单 `issued` 译「已出票」、`viewed` 英文外泄；结算字典缺 V307 `voided`；三页未知码回退英文原码（含合同页 `:69/:137`）。KPI 面从 GR-05 拆出归 004，字典面留 003。未改写。 |
| GR-05 KPI + CX-004 | 004 | 同根：`openRows = status !== "settled"` 把 `voided` 算进未结清。未改写。 |
| GR-06 + CX-005 | 005 | 同根：合同→ADM-CONT-001（`contract.draft.manage`）、空间→ADM-GOV-007（类型字典）、商户→ADM-GOV-002；`navigateTo` 无权限静默 return。本席 B1 拟「空间改拓扑页（须核权限）」；候选收为「对账页合同改 ADM-MER-006，其余无商户可达目标降纯文本」——仍覆盖同一缺陷（假按钮/错对象），不改 AdminApp/TraceLink/路由、不加平台权限。未改写原意。 |
| GR-04 | 006 | 本席独有冲突项。席甲 B1 以 L252 判无表单合规、M020 OBS-5 转入不成立。候选采本席冲突并入队。核源见第二节，**成立**。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：

- 本席观察 1 ＝ 席甲 OBS-2：e2e `full-admin-remaining.spec.ts:917` 仍找异议表单。候选登记（006 修复后 testid 恢复则 e2e 模块自核；本批写入集合不含 e2e）。
- 本席观察 2：M020-001 fail-closed。候选以 `MerchantBillingService.listBills` 的 `findAllBills` 分支（同一错误前提、别模块）登记。
- 本席观察 3：L1322 留言 vs L252 退租审批张力——升格为 006 冲突，不申请改 HTML。
- 本席观察 4 ＝ 候选「我的合同缺关键条款正文」。
- 本席观察 5：L1322 其余自助项（账单/工单/费用通知等）不在本六文件——覆盖边界笔记，非缺陷；不单列不构成漏并。
- 本席观察 6：前端 10 行叠后端默认页。候选登记。
- 本席观察 7：TechDetails 内路径/事件名。候选登记。
- 本席观察 8：`findAllBills` 死代码。候选登记。
- 本席观察 9：M020「无异议入口」转入——升格为 006，不再只当观察。
- 本席观察 10：六文件无 Can/PermissionButton——读页路由闸足够；写路径（异议）归 006 门控。
- 席甲 OBS-1：无月度结算与分成明细（后端/产品一期）保留登记；其中「无留言入口」被 006 吸收，不是漏并。
- 席甲 OBS-3：POST 无前端调用方——被 006 吸收（接既有端点），不再把端点必要性丢给后续模块。
- 席甲 OBS-4：ADM-MER-002 在 governance 组且无 accountForms。候选登记。
- 席甲 OBS-5：PR #444 三提交作基线。候选引言已写「含 PR #444 三个未合并本地提交的现状为基线」。
- 本席 GR-01 的 spaceId/contractId 展示：候选只登记为可选增强、不加。同意（见 001 边界）。

---

### 二、006 分歧项独立核实——成立；处置同时满足 L1322 与 L252；无业务发明

席甲 B1（V0.01 §4 第 6 行、对抗线）：本页对象是退租/到期结算单，枚举 L252 明文该类异议走退租审批，故无表单与 HTML 一致，M020 OBS-5 转入「不成立」。本席 B1 GR-04：页面既无 `settlementItems` 数字也无留言，测试把缺入口写成规范，与 L1322 括号明文冲突；L252 不能把 L1322 括号删掉。

本票不沿用任一席的结论句，独立对原文与已闭环后端。

1. **L1322 原文**（业务地图，criteria-lock 摘录区同句）：商户专区自助「我的对账（**结算与分成明细，有疑问可留言提异议**）」；同条「商户只能就**数字**提出异议」「提出异议期间商户的付款义务**不暂停**」。
2. **L252 原文**（枚举速查退租结算状态行）：「商户对退租结算金额的异议在**退租审批流程中提出并解决**」。业务地图变更与退租段同句。两句都在冻结 HTML 内，不是锁外主张。
3. **现行页面** `MerchantZoneSettlementsPage.tsx:11-16`：「退租金额异议在退租审批中提出。本页只读，不改变结算状态、不暂停付款。」`:81-125` 不渲染 `settlementItems`、无 POST。测试 `:50-59` 正向断言无 `merchant-zone-settlement-dispute-form`。这是与 L1322 括号**相反的页面规范**，不是「尚未实现」的空缺——2.1 先冲突。
4. **M020 已闭环的上位就是 L1322**，不是 L252 删括号：`MerchantPortalService.java:113-122` 注释「只记理由，不改状态、不暂停付款」；`:156-158` `saveSettlement` 保留 `settlement.status()`；权限码 `merchant.portal.settlement.dispute`（V200 已有）；`:32-33` `DISPUTABLE_STATUSES = pending_settlement / deposit_offsetting / arrears_collecting`；`:34-35`/`151-152` 合并后 ≤500 字（M020-003）。M020-007 已把 OpenAPI「挂起为有争议」标 XM，代码不为对齐契约改 status。前端接这条 note-only 端点，不是新通道。
5. **明细**：`TerminationSettlementMath.java:40-75` 写入的 `settlementItems` 键为 `item`（闭集 `unpaid_receivable_summary` / `deposit_disposal` / `termination_penalty`）、`amountCent`、中文 `note`（押金项另有 `depositCent`/`offsetCent`/`netRefundCent`）。列表已随 GET 返回，页面抽屉不展示——「只能就数字提出异议」却看不到数字。渲染已有字段不是发明分成规则。
6. **两句并存读法**（不升 2.2，见第四节）：L1322 给专区内**留言**（note-only，不改状态、不暂停付款）；L252 给退租金额异议的**正式提出与解决**（退租审批）。候选文案「留言不改变结算状态、不暂停付款；退租金额异议的正式处理在退租审批流程中进行」是两句原文的并列转写，不是第三套状态机。不把留言写成推进 `disputed`，不暂停付款，不在本页做审批裁决。

处置与既有契约对齐，无发明：

| 候选条款 | 来源（已有） | 是否发明 |
|---|---|---|
| 抽屉渲染 `settlementItems`（中文名、amountCent 分→元、note） | GET 已返回；数学类已写中文 note 与三项 `item` 闭集 | 否（页内字典覆盖三项英文键＝PGM-001 下位；不新开第四类条目） |
| 仅 `pending_settlement`/`deposit_offsetting`/`arrears_collecting` 可留言 | `DISPUTABLE_STATUSES` L32-33 原文 | 否 |
| 理由 ≤500 字 | V162 列宽 + M020-003 | 否 |
| 走既有 POST；AdminApi 自带幂等键 | `MerchantPortalController:46-54`；`adminClient.ts:91/:207` 写路径注入 camel `idempotencyKey` | 否 |
| `merchant.portal.settlement.dispute` 门控 | 服务 L116；`AdminPageProps.effectivePermissions` 已注入，可用既有 `Can`/`PermissionButton`（只 import，不改那些文件） | 否 |
| 文案：不改状态、不暂停付款；正式处理在退租审批 | L1322 + L252 原文 | 否 |
| 不改后端、不发明新状态/KPI | 写入集合 6 文件 | 否 |
| 重写「无表单」测试为断言明细与留言路径 | 解除 GR-04 假绿 | 否 |

非阻断执行注（不否决 006）：「条目中文名」不得把 `item` 英文原码（`unpaid_receivable_summary` 等）直接上屏；页内字典只覆盖上述三项闭集，中文可与既有 `note` 同义（未清应收合计／押金处置／提前退租违约金）。`types.ts` 的 `MerchantZoneSettlementRow` **已有** `settlementItems?: unknown[]`，B3 不改 `types.ts`。POST body 用 camel `reason`（及可选 `idempotencyKey`），让 AdminApi 注入/转 snake；不要先写 snake 再被 `normalizeRequestBody` 覆盖（M018-005 同构）。testid 建议恢复 `merchant-zone-settlement-dispute-form` 以便 e2e 自核，但 e2e 文件本身不在本批写入集合。

票：**PASS（冲突／是／是）**。

---

### 三、最小处置边界

写入集合限本模块 6 文件，一次 B3 允许改：

- `apps/web/src/admin/pages/MerchantScopePage.tsx`（001、002、003 账单字典、005 商户号纯文本）
- `apps/web/src/admin/pages/MerchantScopePage.test.tsx`
- `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx`（003 未知码、005 合同号/续签自/空间纯文本）
- `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx`
- `apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx`（003 voided/未知码、004、005 合同→ADM-MER-006、006）
- `apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx`

禁止：改 `AdminApp.tsx` / `TraceLink.tsx` / `adminRoutes.ts` / `types.ts` / `statusLabels.ts` / `Can.tsx` / `MerchantZoneBillsPage.tsx`（003 只抄其词，不改该文件）/ 后端 Java / 迁移 / OpenAPI / 三 HTML / 锁 / 台账；不给商户加 `contract.draft.manage` 等平台权限；不新增 spaceId/contractId 列。

**001 —— 同意细化／删幽灵列。** 金额读 `totalAmountCent` 分→元；删账单「标题」、两表「授权范围」「留痕证据」、「授权范围」KPI；行类型页内局部收口（`MerchantScopeBillRow` 在别模块 `types.ts`，不改）；夹具改 `total_amount_cent` 并断言合计非「0.00 元」。本席 B1 的 spaceId/contractId TraceLink **不入本条**（全链可点击的假按钮面已由 005 收口）。非阻断：候选「上位 L1322 我的账单」对 ADM-MER-002 授权工作台略宽（本席 B1 用 L1310）；缺陷事实仍是读不存在字段，不否决。票：**PASS**。

**002 —— 同意细化／删除伪动作，不走 2.2。** 删确认按钮、`confirmScope`、`TEXT.confirm`；`AuditTrailHint` 改真实事件 `merchant_service.work_order.viewed / merchant_billing.bill.viewed`；`TEXT.evidence` 改路由真实两码。删除无 HTML 条目的伪成功不是新业务动作，2.1 不是添加。本席 B1「若构成无上位可改判添加」在处置为删除后不适用。票：**PASS**。

**003 —— 同意细化／页内字典。** 授权工作台账单状态用页内字典，词与账单专区 `MerchantZoneBillsPage.tsx:18-25` 相同（已出账／已查看／已确认／有异议／已逾期／已结清）；结算字典补 `voided: "已作废"`；三页未知码回退中文占位（不回显英文原码）；测试补 issued/viewed/voided/未知码。不改共享 `statusLabels.ts`。票：**PASS**。

**004 —— 同意细化／未结清排除 settled 与 voided。** `openRows` 状态不在 `(settled, voided)`；测试补 voided 行。不新开「作废」KPI。票：**PASS**。

**005 —— 同意细化／可达则改指、不可达则纯文本。** 对账页合同下钻改 `ADM-MER-006`（`adminRoutes.ts:92`：`contract.view` + `accountForms: ["merchant"]`，与本页受众同闸）。合同页自身合同号/续签自/空间、授权工作台商户号：无商户可达目标 → TraceLink 不传 `navigateTo`（既有组件即纯文本）。不改 AdminApp/TraceLink/路由。本席 B1「空间改 ADM-GOV-001」不采——商户专区无空间实例页，改指会变成另一颗假按钮或逼加平台权限。票：**PASS**。

**006 —— 同意冲突／接既有 POST + 渲染已有明细。** 见第二节。票：**PASS**。

有无发明业务规则或应升添加：**无。** 001 删的是后端不存在的列。002 删伪动作。003/004 是既有闭集的中文与 KPI 口径。005 是 PGM-001 下位可达性，不加权限。006 接 M020 已闭环 note-only 端点并展示已返回字段。本席观察 5 的其它自助项不升本批。月度结算/分成明细仍只登记（后端一期范围）。

同文件多 ID 版本铁律：六文件现无版本标记则 B3 只前进一次（各生产文件至多加一处 V0.01 或从既有再升一档）。

---

### 四、添加／HTML／questions；L1322 与 L252 并存是否升 2.2

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M023*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML（两句都不是文件损坏）。

**L1322 与 L252 并存读法：不升 2.2。** 理由：

1. 2.2 只服务**添加**（HTML 无句、要发明规则）。两句都已在冻结 HTML 与 lock 摘录内，不是缺口。
2. 分层已由 M020 闭环写进代码：专区留言 note-only；正式解决仍在退租审批；付款不暂停。本批 006 只把已有后端接到本页，不是问操作员「要不要有异议通道」。
3. 若把并存拿去 2.2，等于请操作员挑选哪一句 HTML 作废——那是 LOCK-AMEND / 改 HTML，不是 2.2。
4. 候选已在回执报操作员：可**否决留言项**，明细项不受影响。这是操作员对修复范围的否决权，不是发问。本席同意该回执口径；即使否决留言，006 的明细面仍须入队（L1322「结算与分成明细」+「就数字提出异议」）。

只登记不入队（无应升添加、无应升本批缺陷）：e2e:917、无月度/分成明细、合同缺关键条款、ADM-MER-002 分组、findAllBills 前提、前端分页、TechDetails 内部标识、spaceId/contractId 可选增强。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 6 文件。

- 001：金额 `totalAmountCent`；删幽灵列与授权范围 KPI；页内类型；夹具真形状。不加 spaceId/contractId。
- 002：删除伪确认；真实事件与权限码。
- 003：账单页内字典＝账单专区同词；`voided`＝已作废；未知码中文占位。
- 004：未结清排除 settled 与 voided。
- 005：对账合同→ADM-MER-006；其余不可达降纯文本。
- 006：抽屉渲染 settlementItems（三项 item 中文、amountCent 分→元、note）；DISPUTABLE 三态留言 ≤500、既有 POST、dispute 门控；文案并列 L1322 与 L252；重写无表单测试。

禁止：改后端/HTML/锁/types.ts/AdminApp/TraceLink/路由、给商户加平台权限、把 002 做成新「确认授权」业务动作、把 006 做成推进 disputed 或暂停付款、把月度分成升添加、发 2.2。

### 结论

全批差集无漏并、无原意改写。001–005 细化／是／是。006 冲突／是／是（采本席 GR-04；席甲 B1 以 L252 判无表单合规不采）。006 经独立读 L1322/L252 原文、M020 闭环 note-only 端点、`DISPUTABLE_STATUSES`、`TerminationSettlementMath` 三项 item：**处置同时满足两句 HTML，无业务发明**。L1322/L252 并存**不升 2.2**。最小写入集合＝本模块 6 文件。添加 0。HTML 申请 0。questions 0。非阻断：001 上位「我的账单」措辞略宽；006 条目中文名须页内三项闭集、不改 types.ts。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M023 IN_PROGRESS；B3=0。

## M023 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 + V0.02 B2-r1 前缀 **57171** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：固定 Grok 只读外审席乙（grok-4.6 / xhigh；独立）。步骤：B2-r2 整批候选复核，不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M023 FILES 6 / 锁映射 21：`BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001`；**不含** BEQR-156 / BSM-073）
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `89686538c7146a9891401b00e51369629ee1cfa4`（未 fetch）。B3 累计 1；本批定稿后 B3-r2 为第 2 次。无源修复。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账。本席 **未读** `M023-b1-self-raw.md` 若已追加的 V0.03 B2-r2 结论段。
- 背景：M023 B5-r1 本席票 FAIL（GR-01：页内字典键 `draft` 对不上后端真码 `drafting`）；席甲 B5 票 PASS 经核源撤回为 FAIL；合取 FAIL。依据无 ID 且 001–006 全否 → 按回退统一规则整批回 B2。
- 本席 B2-r2：**PASS**（差集 GR-01↔007 一一对应、无第二条可并、无改写；007 分类细化、工程缺陷是、队列是、最小处置成立；amended 登记成立、不入队、不升 2.2；001–006 不动；无添加项、无 HTML 申请、无 questions）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 fetch/commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（源码 / HTML 行 / 哈希 / graft CLI 本地检索）；未对任一 `complex_test*` 库做写操作；本票不声称 vitest 本轮新绿。

此票只判断 findings.md V2.74 顶部「当前 M023 B2-r2 整批候选」相对本次两份完整 B5-r1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **PASS**，亦不以本票代替 B5。不加判据。B2 不写任何新的独占 CRITERIA-LOCK-SHA256 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M023-b5-r1-grok-raw.md`（本席 B5-r1 票 FAIL：GR-01） | V0.01 | 28046 | `4e9a232dfd76f972324e08ce429c2bf285ad0ad1eb1cbd4e7abb46ff28d5b6cd` |
| `docs/audit-2026-09/modules/M023-b5-r1-self-raw.md`（席甲 V0.01 PASS + V0.02 撤回与更正段 FAIL） | V0.01+V0.02 | 12857 | `135bf4f27092a63b7f47dc63f1fbd48c30e49e9c3ff7bd4098c00ba09fe90788` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M023 B2-r2 整批候选」 | V2.74 | 1055590 | `9701c3e3e43ac38a09d2b000f7f87c5b3cd9e69f978b24caf4edbf0b5441a575` |
| 本文件前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 57171 | `54be68670679d2087ce7540320b07f0b1b7fe79549505a76e1544407811bac0a` |

三输入字节/SHA 与派发清单全等。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

候选形态：V2.74 顶部为 r2 **新 ID 段**（非整表重写 001–006）。表内仅 M023-007 一行；其下「修复队列是（1）／否（不变）001–006」。本席按此读取，不把历史 B2-r1 表残留单元格当作本批有效处置。`questions/M023*` 不存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| graft | 本节点全文实读 SKILL.md 并调用：`STATUS_LABELS` / `drafting` / `amended` / `listContractsByMerchant`；apps/web 对 `amended` 0 命中。未派 MCP graft、未管道 head/tail |
| receiving-code-review | 先核源/HTML/两份 B5-r1 raw 再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：差集有无第二条、007 应否改全局 `statusLabels`、amended 应否入队或 2.2、001–006 有无回潮 |
| verification-before-completion | 声称前先跑只读哈希与源码/HTML 点读；本票不声称 vitest 本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审六文件模块结论 |
| codebase-design / ponytail-review | 最小是改错键、补会红的夹具；不发明第九态中文名、不改后端 CHECK |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

graft 本轮检索约节省 ~224,021 tokens（STATUS_LABELS / skeleton / ContractService drafting / ContractService amended / listContractsByMerchant / statusLabels drafting）。

---

### 一、差集核实

本席 B5-r1 V0.01：**FAIL**，临时缺陷仅 **GR-01** 一条（「无其他独立 FAIL 条。无添加项。001–006 不重开。」）。席甲 V0.01 票原 PASS、Findings「无新增」；V0.02 撤回段核源后改 FAIL，只确认本席这一条，另把后端第 9 态 `amended` **请本席给意见**（未立第二条临时缺陷 ID、未主张入本模块队列）。

| 依据 | 所在 raw | 候选落点 | 同根？ | 本席票 |
|---|---|---|---|---|
| GR-01 页内 `draft: "草拟"` 对不上后端 `drafting`；商户第一次看起草中合同即「未知状态」；夹具无 `drafting` | 乙 V0.01 §8 | M023-007 | 是。甲 V0.02：`MerchantZoneContractsPage.tsx:18-27` 首键 `draft`；`ContractService.java:206/291/330` 写 `"drafting"`；CHECK 闭集含 `drafting` 不含 `draft`；`listContractsByMerchant` 无状态过滤——成立。候选位置/因果/最小修法与本席 GR-01 原文同根，未改写为「缺状态列」或「应改全局起草中」 | **并入，无改写** |
| 乙其余观察（`archived` 预置、结算字典 `disputed_litigation` 预置、留言合并超长走后端中文拒绝、连点异键、工单 100 条上限、平台无范围「读取失败」语义略混、`types.ts` 旧假字段、AdminApp `<pre>` 英文 JSON） | 乙 V0.01 §3.3/§5/§7/§8 | 未立项 | 本席 B5 已判不构成第二条 FAIL。候选未升格 | **同意不立项** |
| 席甲 V0.02 撤回段：其余 7 键与 V314 CHECK 逐个对上；账单/结算/工单闭集复查无第二缺口 | 甲 V0.02 | 007 边界「其余 7 键不动」 | 确认 001–006 已修面未回潮，不是新缺陷 | **无第二条可并** |
| 席甲 V0.02：后端第 9 态 `amended` 无中文名，请 Grok 给意见 | 甲 V0.02 | 只登记不入队 | 独立核源见第三节。**不是**可并的第二条队列项 | **同意登记、不同意入队或 2.2** |
| 席甲 V0.02：探针 `row.totalAmount` 前缀命中 `totalAmountCent` 的表述更正 | 甲 V0.02 | 未立项 | 结论（页面只读 `totalAmountCent`）不变，不是缺陷 | **同意不立项** |
| 001–006 | 乙 §6 已修成立；甲撤回未重开 | 否（不变） | 候选表无 `| M023-001 |`…`| M023-006 |` 行 | **同意不动** |

漏并：无。改写原意：无。第二条可并队列项：无。新 ID 仅 007，符合「仅给无 ID 条目分配新 ID」。B1/B4 两席均漏本条——属实（B1 合同页当时对照的是枚举八态中文词，未对后端 CHECK 闭集；B3-r1 未改该键；B4 只核 001–006 已修面）。无本批添加项、无 HTML 申请、无 questions 文件。

票：**PASS（差集）**。

---

### 二、M023-007 分类与最小处置（独立核源）

**事实（类型 4，工作区源；与候选一致）：**

- `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:1` 版本 V0.01；`:18-27` 页内字典首键 `draft: "草拟"`，其余七键 `pending_confirmation/pending_effective/effective/renewing/expired/terminated/archived`；`:29-34` `UNKNOWN_STATUS = "未知状态"`，`statusLabel` 未命中即该占位。表格 `:138` 与抽屉 `:76` 都走 `statusLabel(row.lifecycleStatus)`。
- `ContractService.java:206` 起草落库 `"drafting"`；`:291` 提交门 `!"drafting".equals(...)`；`:330` 驳回回 `"drafting"`。合同包 Java **0** 处字符串 `"draft"`。
- `V162__contract_lifecycle.sql:32-44` 已把旧技术态 `draft` 收口为 `drafting`（行内注释「草拟」），闭集八值、无 `amended`。`V314:13-17` 重建 `chk_contracts_lifecycle_status` 为九值：`drafting, pending_confirmation, pending_effective, effective, renewing, expired, terminated, archived, amended`——**不含 `draft`**。
- `JdbcContractRepository.java:286-289`：`FROM contracts WHERE merchant_id = :mid` **无状态过滤**。`MerchantPortalService.listMyContracts:63-67` 原样映射 `MerchantContractView`，默认 size=500。起草合同会进「我的合同」。
- `MerchantZoneContractsPage.test.tsx:1` 版本 V0.01；`:9-12` 夹具只有 `effective` / `expired` / `weird_status`。`:74-88` 断言全表含「未知状态」、**不含「起草中」**、不含英文 `effective/expired/weird_status`。把 `draft` 改对或改错，这 7 例都不会为起草态红。
- 全局 `apps/web/src/customer/i18n/statusLabels.ts:54` `drafting: "起草中"`——与页内「草拟」不同词。本页不走 `formatBusinessStatus`。`types.ts:1042-1054` `MerchantZoneContractRow.lifecycleStatus?: string`，无枚举可对表。
- 其余 7 键与 V314 闭集逐个对上（本席点读，不抄席甲）：

| 页内键 | 页内词 | V314 CHECK |
|---|---|---|
| pending_confirmation | 待确认 | 有 |
| pending_effective | 待生效 | 有 |
| effective | 已生效 | 有 |
| renewing | 续签中 | 有 |
| expired | 已到期 | 有 |
| terminated | 已终止 | 有 |
| archived | 归档 | 有 |

**正常触发（第一次就会撞上）：** 平台起草一份该商户合同 → 商户打开 ADM-MER-006「我的合同」→ `lifecycle_status=drafting` → 状态列与抽屉均为「未知状态」，不是「草拟」。英文原码被 003 占位挡住，旧用例仍绿。

**四分类（2.1 顺序：冲突 → 细化 → 技术性 → 添加）：**

- 冲突？**否。** 本模块映射的 BSM-001 / BSM-003 / PGM-001 摘录没有「草拟」二字，也没有「起草键必须是 drafting」。PGM-001 零英文本身成立（不回显 `drafting`）。BEQR-156（枚举 L239 八态）**未映射到 M023**，不得拿来当冲突判据（锁外主张作废；本席 B5 同口径）。现网不是「多给了一条与已映射 HTML 相反的规则」，是下位字典键写错。
- 细化？**是。** 合同状态列是「我的合同」只读页的下位实现。上位指针：业务地图 L1322「我的合同（关键条款、租期、到期情况）」——只作本页存在理由/细化上位，**不**把未映射的 BSM-073 写成新判据。冻结判据：GEN-008（页内键与后端真码不一致）、GEN-002（状态列声称与真接口不符）、GEN-003（`drafting` 无会失败的断言）、GEN-015（已知起草态被显示成未知，可读性坏）。枚举 L239「草拟、待确认…归档」**仅作分类对照，不作判据**。
- 不到技术性（能指「我的合同」下位实现，且是业务枚举键，2.1 禁止进技术性）。
- 不到添加（不发明新状态/新中文名；页内词仍是已有的「草拟」，只改键对齐后端真码）。

工程缺陷：**是**。队列：**是**（本模块页 + 其测试可修）。不重开 003（003 已否＝未知码中文占位已落地；本条是**已知**起草态被误判为未知）。

**最小处置核实：** 字典键 `draft` 改为 `drafting: "草拟"`；夹具加一条 `lifecycle_status: "drafting"` 并断言**该表格行与其抽屉**均显示「草拟」、不含「未知状态」、不含英文 `drafting`；两文件版本标记 V0.01→V0.02。不改 `types.ts`／后端／全局 `statusLabels`；其余 7 键不动。写入集合＝这两文件。

- **最小？是。** 错的是键不是词。V162 行内注释与 L239 对照词都是「草拟」，不是全局「起草中」。改成「起草中」会与现有用例 `:82` `not.toContain("起草中")` 相反，且无必要动共享码表。
- **应改 `statusLabels.ts`？不同意。** 全局 `drafting→起草中` 服务合同工作台等别页；本页已有页内字典，003 先例即「页内覆盖、不改共享」。
- **应改后端/`types.ts`？不同意。** 后端真码与 CHECK 已是 `drafting`；行类型已是 `string`。为对表去改后端等于把旧技术态 `draft` 请回来，与 V162 收口相反。
- **应把其余 7 键或 `amended` 一并改？不同意。** 7 键已对上。`amended` 见第三节，007 **不得**顺手加自拟键。
- **有无业务发明？无。** 不新状态、不新中文名、不新过滤。

**B3 非阻断边界（不构成否决）：**

- 「不含未知状态」的断言作用域是 **drafting 那一行**（`merchant-zone-contract-row-<id>`）**与其抽屉**，不得对全表 `merchant-zone-contracts-table` 做 `not.toContain("未知状态")`——须保留 `weird_status` 行，003 占位用例 `:80` 仍绿。
- 新夹具须独立 `id`；KPI「已生效」现断言仍为 1，加 drafting 行不得改成 effective。
- 词必须是「草拟」，禁止改用全局「起草中」（现有 `:82` 已锁）。
- 不要给 `amended` 加字典项（见第三节）。不要过滤 `listContractsByMerchant` 的返回集。

票：**PASS（细化 / 缺陷是 / 队列是；处置最小；不同意改全局码表或后端）**。

---

### 三、专门意见：后端第 9 态 `amended` 的登记处置

席甲拟：不自拟中文名；007 修后该态在本页走 003「未知状态」、不泄英文；九态 vs 枚举 L239 八态的差异登记为跨模块观察（M254 后台实现 / M041 迁移 V314）并报操作员，**不入本模块修复队列**。派发要求本席明确：是否成立；应否改为入队或升 2.2；若入队须指出中文名的 HTML 原文出处，不得自拟。

**独立核源（不沿用席甲结论句）：**

1. **真路径成立。** `ContractService.java:1140` 注释「原合同置 amended」；`:1180` `repository.saveContract(withStatus(parent, "amended"))`；`:1190` 审计 `contract.amended`。`V314:13-17` 注释原文「被变更子合同接替的原合同；非生效，自动退出计费/到期/续约作业与经营范围拦截生效谓词」，CHECK 含 `amended`。`listContractsByMerchant` / `listMyContracts` 无状态过滤 → 变更完成后原合同会进本页。
2. **apps/web 0 处 `amended`。** graft grep `--in apps/web` 无命中。本页字典八键无此项。007 修后 `statusLabel("amended")` → 「未知状态」（003 占位），英文码不回显。
3. **三份 HTML 无该态中文名（本席逐句）：**
   - 枚举速查 L239：`草拟、待确认、待生效、已生效、续签中、已到期、已终止、归档。`——闭集八态，无第九词。锁摘录 BEQR-156 同句。
   - 业务地图 L1352：`变更子合同：如果同一商户只是变更经营位置或调整经营面积……基于原合同开一份有继承关系的「变更子合同」来接着处理`——只描述**子合同本身**，不给原合同生命周期一个中文状态名。
   - 业务地图 L1351 变更与退租八类、治理地图全文：`rg` `amended|被变更|原合同置|已修订|已变更` 在三 HTML **0 命中**。
4. **模块归属属实。** `modules.md`：`ContractService.java` / `JdbcContractRepository.java` 在 **M254** FILES；`V314__contract_scope_grant_and_amendment.sql` 在 **M041** FILES（全计划唯一命中）。M254 锁映射含 **BEQR-156**；M023 不含。九态/八态差是合同状态机与枚举原文的对象，不是本六文件的字典漏词。
5. **GEN-017：** 不得把业务发明伪装成技术实现，也不得因 HTML 沉默直接删除。给 `amended` 起中文名＝发明业务用词；把该行从商户列表滤掉＝因 HTML 沉默删除可见记录。两条都禁止。003 占位是已定稿的未知码展示，零英文仍成立。

**本席回答（二选一，无条件通过）：**

| 问题 | 答 |
|---|---|
| 该登记处置是否成立？ | **成立。同意。** |
| 是否应改为入本模块队列？ | **否。** 三份 HTML 与 BEQR-156 原文均无该态中文名。派发约束「若入队须指出 HTML 原文出处，不得自拟」——本席检索无出处，故不能入队，不能自拟「已变更／已接替／已修订」等。V314/Java 注释不是 HTML 原文，GEN-018 禁止用注释当依据。 |
| 是否升 2.2 向操作员发问？ | **否。** 2.2 只服务**添加**（本模块要发明规则才发问）。本批 007 不给该态起名、不新过滤、不改 CHECK。把「第九态叫什么」拿去 2.2，等于请操作员给 BEQR-156 补词——那是 LOCK-AMEND / 改 HTML，不是本页 2.2。回执报操作员即可（商户专区对被接替原合同显示「未知状态」；命名与九/八态差归 M254/M041）。不 BLOCKED-ASK。 |

票：**PASS（只登记、不入队、不 2.2）**。

---

### 四、001–006 不动

| ID | 本批候选 | 本席 |
|---|---|---|
| 001–006 | 否（已修，B3-r1） | **同意不动、不回潮。** 本席 B5-r1 §6 已逐条复验落地。007 不重开 003（未知码占位已在；本条是起草真码错键）。不把 001–006 改回「是」。 |

写入集合限 2 文件，不碰 Scope/Settlements 四文件。

---

### 五、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M023*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML（L239 八态不是本页损坏；补第九态名称属 BEQR-156 / M254 审计面）。007 用既有词「草拟」改键，不是新业务规则。amended 不升添加。

### 分类／队列／允许的最小修复边界

本席 B2-r2 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3-r2 只改本模块 2 文件（第 2 次 B3）。

- 007：`MerchantZoneContractsPage.tsx` 字典键 `draft` → `drafting: "草拟"`（其余 7 键不动、不加 `amended`）；`MerchantZoneContractsPage.test.tsx` 夹具加 `lifecycle_status: "drafting"`，断言**该行与其抽屉**显示「草拟」、不含「未知状态」、不含英文 `drafting`；两文件 `V0.01`→`V0.02`。
- 001–006：不动。
- amended：只登记，回执报操作员。

禁止：改 `types.ts` / `statusLabels.ts` / 后端 Java / V314 / 三 HTML / 锁 / 另四文件；把页内词改成「起草中」；给 `amended` 自拟中文名或过滤掉该行；发 2.2；LOCK-AMEND。

### 结论

全批差集无漏并、无原意改写、无第二条可并。M023-007 细化／是／是（采本席 GR-01；席甲 B5 原 PASS 经核源撤回）。amended 登记成立，**不入队、不升 2.2**（三 HTML 无中文名可引）。001–006 不动。最小写入集合＝合同页 + 其测试。添加 0。HTML 申请 0。questions 0。非阻断：drafting 断言作用域＝该行+抽屉，保留 weird_status；007 不得顺手加 `amended` 键。逐项：差集 PASS、007 PASS、amended 登记 PASS、001–006 不动 PASS。本席 B2-r2 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M023 IN_PROGRESS；B3 累计 1，定稿后 B3-r2 为第 2 次。

