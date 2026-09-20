CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B5-r1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B5-r1（修复后整模块三线全量重审，不只看 diff、不复用 B2/B4 票）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；六文件工作区内容与 after tree `1812e0f7017ccf3268183b8b94ea25521e246fc3` 无 diff
- 模块：`plan/modules.md` V0.04「商户档案、商户类型与商户侧门户／网页与验证／第1段」
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar / ledger 顶部同值，本席未抄基准写入本行）
- 映射 21 项：`M023 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]`
- findings.md 现态（只读核对）：V2.73，M023-001…006 修复队列「否（已修）」；本席不把「全部否」当作功能已全实现
- B3 累计：1（派发背景）；本席不改 ledger
- 独立性：未读 `M023-b5-r1-self-raw.md`；未用 MCP / 子代理 / 第三席；未改业务代码 / 三 HTML / 锁 / findings / ledger；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时产物
- 结论：**FAIL**（收齐全部文件与 21 项后统一判定；有一条未定稿工程缺陷）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先于 Arc 全量审计派席 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc audit 派评审席 |
| graft | 已读并调用 | `graft grep` 定位 MerchantPortal / DISPUTABLE / Bill/WorkOrder/Math |
| long-running-background-tasks | 已读 | 本轮 vitest/tsc 均短前台完成，未挂后台 |
| grilling / grill-me | 已读 | 只借追问结构，事实自读，不问人、不派子代理 |
| batch-grill-me | **缺口** | 已并入 grilling，无独立技能可调 |
| ubiquitous-language | **缺口** | 已退役并入 domain-modeling；domain-modeling 按 §6 划掉 |
| codebase-design | 已读 | 三页是 AdminApi 上的浅界面；缺陷在枚举键与后端码不一致 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现，B5 禁止修复、不加诊断桩 |
| tdd / test-driven-development | 已读 | 用来判断测试能否因行为变坏而红，本席不写测试 |
| verification-before-completion | 已读 | 先跑 vitest/tsc 再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；旧票只作背景，现码重核 |
| requesting-code-review | 存在，B4 用 | B5 不适用 |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE，无冲突可解 |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §7 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | 存在 | Codex 回执模式；本席正文只落 raw |

Arc `audit` 技能要求派子代理：与本轮「只允许两席」冲突，按用户指令不执行。

graft 本轮检索约节省 ~121,121 tokens（DISPUTABLE / MerchantPortal / MerchantBillRecord / MerchantWorkOrderRecord / TerminationSettlementMath 五次调用合计）。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l` + `sha256sum`（工作区现值）；有效内容 `git ls-files -z -- <6 paths> | xargs -0 cat | wc -l` → **1234**（`modules.md` 记 991，以实算为准）。

| 文件 | 行 | SHA256 | 覆盖 |
|---|---|---|---|
| `apps/web/src/admin/pages/MerchantScopePage.tsx` | 217 | `c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9` | 全文 |
| `apps/web/src/admin/pages/MerchantScopePage.test.tsx` | 211 | `aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx` | 175 | `6c9a0b9a12ebc05a17ac1fc70a3c013cb7660d536a748a12d7d736a4527487a3` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx` | 142 | `6013ffe5085e736203603588493681b692c9c3f4bfa8ffdbc3dbd9a8833767f5` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx` | 272 | `271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx` | 217 | `2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b` | 全文 |

相关三 HTML（只读，铁律 4 未改）：

- `docs/business-system-map-for-operations-v0.1.html`：BSM-001（锁摘录 h2:505）、BSM-003（h2:563）、商户专区上下文 L1308–1322（BSM-072/073，**未映射到 M023**，只作 2.1 分类对照，不新增判据）
- `docs/platform-governance-map-for-operations-v0.1.html`：PGM-001（锁摘录 h2:256，含全链可点击 / 零英文 / 金额到分 / 重复提交）
- `docs/business-enum-quick-reference-v0.1.html`：合同状态 `td.k` L239（**未映射到 M023**，只作分类对照）

追现行真实现（只读，非本模块文件）：`MerchantPortalController.java` / `MerchantPortalService.java` V0.01、`MerchantBillRecord.java`、`MerchantWorkOrderRecord.java`、`TerminationSettlementMath.java`、`MerchantContractView.java`、`ContractSettlementRecord.java`、`AdminApi`（`adminClient.ts` unwrap/snake_case）、`adminRoutes.ts` ADM-MER-002/005/006、`AdminApp.tsx` `navigateTo:545-563` 与 `accountFormOf:171-176`、`useAdminAction.ts`、`useSelectedRow.ts`、`PermissionButton`（`Can.tsx`）、`TraceLink.tsx`、`AuditTrailHint.tsx`、`formatBusinessStatus`（`customer/i18n/statusLabels.ts`）、`apiError.ts`。

---

## 2. 可执行证据（verification-before-completion）

```
cd /home/james/complex-management/apps/web
npm run test:unit -- src/admin/pages/MerchantScopePage.test.tsx \
  src/admin/pages/MerchantZoneContractsPage.test.tsx \
  src/admin/pages/MerchantZoneSettlementsPage.test.tsx
```

- exit 0
- Test Files 3 passed；Tests **31 passed (31)**（Scope 14 / Contracts 7 / Settlements 10）
- Duration 673ms；jsdom `Window.scrollTo` 未实现警告，不断言失败

```
npm run typecheck   # tsc -b --noEmit
```

- exit 0

测试通过 **不** 替代 GEN-003：夹具未覆盖后端真码 `drafting`，见 GR-01。

---

## 3. 第一性原理三问（2.3）

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？

- **MerchantScopePage（ADM-MER-002 商户授权工作台）**：BSM-001「先管清楚基础盘子……谁在经营……谁能操作」+ PGM-001「先管归属 / 再管资格」。删掉则后台无「在授权范围内看本商户工单/账单」的治理页。平台管理员无 `merchantId` 时本页拒绝取数，与 `MerchantPortalService.requireMerchantScope` 的 fail-closed 同方向（本页实际打的是 `merchant-service/work-orders` 与 `merchant-billing/bills`，不是 merchant-portal 前缀）。
- **MerchantZoneContractsPage（ADM-MER-006 我的合同）**：BSM-001 结果侧「结算、商户」+ PGM-001 资格/合同。HTML 商户专区自助「我的合同（关键条款、租期、到期情况）」在 **BSM-073（未映射）**；本页实现租期/空间/状态/押金/续签自，明确把改价与关键条款留在平台侧（`MerchantContractView` 注释同口径）。删掉则商户专区合同只读入口不存在。
- **MerchantZoneSettlementsPage（ADM-MER-005 我的对账）**：PGM-001 责任与可点击 + 页内自称「有疑问可留言提异议」。对应后端 `GET /api/v1/merchant-portal/settlements` 与 `POST .../dispute`（只记理由、不改状态）。删掉则商户看不到退租/到期结算明细、无法走已存在的留言端点。

答得上 → 有业务上位；未映射的 BSM-072/073/BEQR 合同状态 **不** 当作本模块新判据。

### 3.2 声称的行为有没有被实际执行验证过？

有，类型 2+4：

- 31 条 vitest 驱动真实页面组件 + 真实 `AdminApi`（stub `fetch` 信封为后端蛇形键）
- 类型检查 `tsc -b --noEmit` exit 0
- 对照后端源码：账单金额字段 `totalAmountCent`、工单 `title`、结算 `settlementItems`/`DISPUTABLE_STATUSES`、事件名与权限码

未验证就当真的前提见 3.3。

### 3.3 依赖的前提，哪一条没验证过就当真？

1. **合同生命周期码 = 页内字典键。** 页写 `draft: "草拟"`，后端起草落库是 `"drafting"`（`ContractService.java:206`）。此前提为假，见 GR-01。
2. 列表 `api.get` 一次拿全量：合同/结算控制器默认 `size=500`；工单 `DEFAULT_PAGE_LIMIT=100`。超限截断本席未跑后端集成，按铁律 6 不把「>100 张工单」写成必须修的场景。
3. `TechDetails` 里的权限码/事件名视为「系统内部标识符」（PGM-001 四样例外之一），不按零英文展示失败论处。
4. `setActionResult` 成功后的英文 JSON 由 **AdminApp.tsx:734** `<pre>` 渲染，不在本模块六文件，不单列为本模块缺陷。

---

## 4. 顺从式：21 项逐条（不新增判据）

每条：适用与否 / 证据 / 是否构成缺陷。

### BSM-001 整体说明：这套系统到底管什么

- **适用。** 三页分别覆盖「谁在经营 / 签了什么合同 / 对账结算」。
- **证据。** Scope 调 work-orders + bills；Contracts 调 `merchant-portal/contracts`；Settlements 调 settlements + dispute。HTML 原文「先管清楚基础盘子……最后管清楚结果」。
- **缺陷。** 无与该锚点摘录的明文冲突。合同状态展示错误不在该锚点逐字原文里（见 GEN-008）。

### BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗

- **适用（弱）。** 锁内该 ID 的 HTML 只有 h2 + lead「下面这几条不属于某一个子系统……」。子条 BSM-004–007 **未映射**，不拿来扩判据。
- **证据。** 本六文件无存档/删除/分步新建/动态码。权限分岗：Settlements 留言走 `PermissionButton` + `merchant.portal.settlement.dispute`；路由 ADM-MER-005/006 `accountForms: ["merchant"]`。
- **缺陷。** 无。

### GEN-001 必要性与最小性

- **适用。** B3 后已删确认授权伪动作、不存在的列/KPI。KPI 聚焦与行抽屉是既有后台样板，有点击行为。
- **证据。** Scope 无 `merchant-scope-confirm`；账单表头测试断言 `商户账单号金额状态详情`。
- **缺陷。** 无新的无需求抽象。字典键 `draft` 是错键不是多余抽象。

### GEN-002 可执行正确性

- **适用。** 主路径/错误路径须与接口一致。
- **证据。** 31 测绿；金额分→元；留言 POST `/settlements/{id}/dispute` 带 `reason`+`idempotency_key`；平台管理员无 merchantId 不 fetch。
- **缺陷。** **是（GR-01）**：后端真码 `drafting` 在页上走未知占位，与「状态列中文业务名」的页面声称不一致。

### GEN-003 测试有效性

- **适用。**
- **证据。** 断言打在 DOM 文案、URL、fetch URL/body、按钮 disabled、navigateTo 参数；夹具用蛇形真字段。改坏金额列/已作废 KPI/未知码回退/无权限留言，现有用例会红。
- **缺陷。** **是（GR-01 的测试面）**：Contracts 夹具只有 `effective` / `expired` / `weird_status`，没有 `drafting`；把 `draft` 错键改对或改错，当前 7 条都不会红。测试通过不能证明起草态展示正确。

### GEN-004 信任边界校验

- **适用（写路径）。** 留言：空理由、>500 字前端拦住；后端再校验合并长度与状态。
- **证据。** `MerchantZoneSettlementsPage.tsx:165-173`；`MerchantPortalService.disputeOnce:132-153`。
- **缺陷。** 前端 500 只量新输入、不管已有 `disputeReason` 合并；后端会中文拒绝。错误可检测，不单独升为 FAIL 条（见对抗式）。无越权写。

### GEN-005 授权与最小权限

- **适用。**
- **证据。** 路由权限：ADM-MER-002 `merchant.service_work_order.view`+`merchant.billing.view`；005 `merchant.portal.settlement.view`；006 `contract.view`。后端 `requirePermission` 同码。留言另要 `merchant.portal.settlement.dispute`。无权限按钮 `disabled` 且 onClick 拦截。平台管理员打开 002：无 merchantId 不请求。专区 005/006 对 platform 形态 `routeAllowsAccountForm` 拒绝渲染。
- **缺陷。** 无。界面隐藏不是唯一闸，服务端仍拒。

### GEN-006 数据完整性

- **适用（弱）。** 前端不写库；留言成功依赖后端事务/幂等。
- **证据。** `idempotencyService.execute("merchantPortalSettlementDispute:"+id, ...)`。
- **缺陷。** 无本页部分写入。

### GEN-007 并发与幂等

- **适用（写路径）。**
- **证据。** 每次提交 `nextKey("merchant-zone-settlement-dispute")`；`busyAction` 禁用按钮；同键后端只认一次，异键会再合并一条留言。
- **缺陷。** 连点在重绘前理论上可发出两个键。busy 门已在。不升为独立 FAIL（铁律 6：要抢在 setState 前双击）。

### GEN-008 接口与模式一致性

- **适用。**
- **证据。** 账单 `totalAmountCent` 对 `MerchantBillRecord.totalAmountCent`；工单 `title/workOrderType/priority/status` 对 `MerchantWorkOrderRecord`；结算 items 键 `unpaid_receivable_summary` / `deposit_disposal` / `termination_penalty` 对 `TerminationSettlementMath`；事件 `merchant_service.work_order.viewed`、`merchant_billing.bill.viewed`、`merchant_portal.contract.viewed`、`merchant_portal.settlement.viewed|disputed` 均在对应 Service。账单状态页内字典与 `MerchantZoneBillsPage` 同词，避开全局 `issued→已出票`。
- **缺陷。** **是（GR-01）**：`STATUS_LABELS.draft` 对不上后端 `lifecycleStatus="drafting"`。`listContractsByMerchant` 无状态过滤，起草合同会进本页。

### GEN-009 错误处理与恢复

- **适用。**
- **证据。** `useAdminResource` 失败进 `admin-error`；留言失败 `formError`；空理由/过长/未选/不可留言均中文。后端英文 `reason is required` 若直达，`apiError.ts` 400 无中文时走「操作未能完成，请稍后重试」（仍中文）。`SETTLEMENT_NOT_DISPUTABLE` 409 中文「结算单当前状态不可发起异议」。
- **缺陷。** 无吞错、无伪成功。平台无范围时标题是「读取失败」+「缺少商户授权范围」，语义略混但中文可读，有测试，不单列。

### GEN-010 安全与敏感信息

- **适用。**
- **证据。** 无密钥；Scope 客户端再滤 `merchantId`（后端工单/账单在有 scope 时已按商户查；平台无 scope 时本页不请求）。留言不改结算状态，与后端一致。
- **缺陷。** 无。

### GEN-011 可观测与审计证据

- **适用。**
- **证据。** `AuditTrailHint` 事件名与后端 `recordSuccess` 一致（Scope `:187`，Contracts `:152`，Settlements `:237`）。不再出现 B1 的 `merchant.scope_confirmed` / `merchant.portal.read`。
- **缺陷。** 无。

### GEN-012 资源与性能边界

- **适用（弱）。** 无自写无限循环。合同/结算默认 500；工单后端 100 条上限。
- **证据。** `MerchantPortalController` default size=500；`MerchantServiceService.DEFAULT_PAGE_LIMIT=100`。
- **缺陷。** 不把超 100 张工单写成必须项。

### GEN-013 构建配置与依赖

- **不适用。** 六文件无 package/CI。本席仍跑了 `apps/web` 的 vitest/tsc 作为可执行证据。

### GEN-014 数据库迁移正确性

- **不适用。** 无 SQL/迁移。

### GEN-015 界面可用性与可访问性

- **适用。**
- **证据。** 行 `tabIndex={0}` + Enter/Space；抽屉 `role=dialog` + Esc；金额/日期/状态中文；未知码「未知状态/未知触发/其他条目」。留言按钮无权限 `title="当前账号无此操作权限"`。
- **缺陷。** 起草合同显示「未知状态」损害可读性，并入 GR-01，不另开 ID。

### GEN-016 文档可验证性

- **适用（页内证据文案）。**
- **证据。** Scope `TEXT.evidence` 的路径与权限码与 Controller/Service 一致；Settlements 描述与后端「只记理由、不改状态、不暂停付款」一致。
- **缺陷。** 无把计划写成事实。

### GEN-017 业务来源分类

- **适用。** 本席对现码按冲突→细化→技术性→添加走完。KPI 聚焦、行抽屉、短 ID、页内状态字典 = 细化（指向 PGM-001 可点击/零英文与 BSM-001 合同/结算）。无新业务实体/状态发明。`disputed_litigation` 字典项后端合同域 0 命中，属预置中文名，不是新状态机。
- **缺陷。** 无添加项要问人。

### GEN-018 第一性原理证据

- **适用。** 本报告结论落到 HTML 原文、vitest 输出、或可复现命令，不用旧票/惯例。

### PGM-001 整体定位（归属/资格/责任；全链可点击；零英文；金额到分；重复提交）

- **适用。**
- **证据。**
  - 归属：无 merchantId 不展示他商数据；专区页 `accountForms:["merchant"]`。
  - 可点击：结算合同 `TraceLink pageId="ADM-MER-006"`（商户可达）；Scope/Contracts 无商户可达目标则纯文本（点了不会被 `navigateTo` 静默吞掉）。整行开抽屉。
  - 零英文：未知码不回显；账单 issued→已出账 非已出票；voided→已作废；测试断言不含英文枚举。URL `?kpi=` 为内部标识。TechDetails 内权限/事件名按内部标识符。
  - 金额：`cent/100` + `minimumFractionDigits: 2`。
  - 重复提交：幂等键 + busy。
- **缺陷。** 零英文本身成立；起草态「未知状态」是枚举映射错误而非英文泄漏。

---

## 5. 对抗式（grilling 自答，不等人）

### 5.1 真实后端返回下，三页每列每个 KPI 显示什么？

**授权工作台**（`MerchantBillRecord` / `MerchantWorkOrderRecord` 蛇形经 AdminApi 转驼峰）

| 区域 | 显示 |
|---|---|
| KPI 商户工单 | 本商户工单条数，可点 → `?kpi=workOrders` 只留工单表 |
| KPI 商户账单 | 本商户账单条数，可点 → `?kpi=bills` |
| KPI 账单金额 | `sum(totalAmountCent)/100` 两位+「元」，**不可点** |
| 工单列 商户/类型/标题/优先级/状态 | merchantId 原样；类型/优先级/状态走 `formatBusinessStatus`（repair→报修，high→高，processing→处理中）；未知→未知状态；标题后端有 |
| 账单列 商户/账单号/金额/状态 | 金额分转元；issued→已出账，viewed→已查看，closed→已结清；未知→未知状态；**不读** title/authorizedScope/auditEvidenceRef/totalAmount |

**我的合同**（`MerchantContractView`）

| 区域 | 显示 |
|---|---|
| KPI 合同总数 | `rows.length`，可点聚焦全表 |
| KPI 已生效 | `lifecycleStatus==="effective"` 条数 |
| 列 合同 | 末 8 位，title 全量 |
| 租期 | `cnDate`「2026年1月1日」 |
| 空间 | spaceIds 顿号连接，空为 `-` |
| 状态 | 字典；**`drafting` → 未知状态（GR-01）**；effective→已生效；expired→已到期 |
| 押金 | 分转元 |
| 续签自 | 短 ID 或 `-` |

**我的对账**（`ContractSettlementRecord` + `TerminationSettlementMath` items）

| 区域 | 显示 |
|---|---|
| KPI 结算单 | 全量条数 |
| KPI 未结清 | `status` 不在 `{settled, voided}`（含 pending_* / disputed / 未知码） |
| 列 选择/结算单/合同/触发/状态/留言理由/开账时间 | 触发 move_out→退租；voided→已作废；未知→未知触发/未知状态 |
| 抽屉结算明细 | item 中文名 + 分转元 + note；未知 item→其他条目 |

### 5.2 商户点每个可点元素会怎样？

- KPI 数字：pushState 聚焦，表还在，**对账页聚焦时不渲染留言表单**（须「清除筛选，返回全部」才再见到表单）。HTML 未要求聚焦态仍留表单，不升需求。
- 行 / 查看详情：开抽屉，Esc 关。
- 对账合同 TraceLink：`navigateTo("ADM-MER-006", contractId)`，`stopPropagation` 不开抽屉；商户有 `contract.view` 且 accountForm=merchant，不会静默 return。
- 授权工作台/我的合同：无 TraceLink。
- 留言提交：校验 → POST dispute → 成功清理由并 reload。
- 单选：选中行；`useSelectedRow` 在列表变化时若当前 id 不在列表则落到第一行。

### 5.3 无留言权限 / 已结清 / 已作废 / 空理由 / 超长理由 / 重复提交？

- 无 `merchant.portal.settlement.dispute`：按钮禁用 + onClick 直接 return，测试已红能力。
- 已结清/已作废：`DISPUTABLE` 与后端 `pending_settlement|deposit_offsetting|arrears_collecting` 一致；按钮禁用。
- 空理由：前端「提交留言必须填写理由。」不发请求（`useSelectedRow` 会预选第一行，故空点走的是空理由而非未选）。
- >500 字：前端拦；input `maxLength=500`。合并已有理由超 500：前端可能放行，后端「异议留言不能超过 500 字」，`hasChinese` 原样展示。
- 重复提交：同幂等键后端一次；异键再追加合并。busy 禁用。

### 5.4 后端返回未知状态码，屏幕是否仍全中文？

是。三页未知码均为「未知状态 / 未知触发 / 其他条目」，测试含 `weird_code` / `weird_status` / `weird_trigger` / `mystery_item`。**但是** `drafting` 被误判为未知（中文占位掩盖了真状态）。

### 5.5 平台管理员打开授权工作台会怎样？

ADM-MER-002 无 `accountForms`，platform 可进页。`accountFormOf(platform_admin)` 恒为 platform，但本页闸的是 `user.merchantId`：空则 `scopeMissing`，抛「当前账号缺少商户授权范围…」，**不调用 fetch**（测试 115–122）。有 merchantId 的 platform_admin 会请求；账单 Service 在无 currentMerchantScope 时 `findAllBills()`，本页再按 merchantId 客户端过滤。专区 005/006 对 platform 直接 `admin-account-form-mismatch`，不挂载本模块页。

### 5.6 改坏任一页面行为是否有用例会红？

会红的例子：金额改回 `totalAmount`；恢复确认按钮；issued 走全局「已出票」；voided 计入未结清；TraceLink 改回 ADM-CONT-001；去掉留言 POST；未知码回显英文。

**不会红：** 把 `draft: "草拟"` 留着而后端继续返回 `drafting`；删掉 `draft` 键；给 `drafting` 错译。GEN-003 缺口。

### 5.7 是否还有不调后端的伪动作或后端不存在的字段？

- 伪动作：确认授权已删。KPI 聚焦只改 URL/本地 state，不冒充写接口。
- 不存在字段：不再读 `totalAmount` / 账单 title / authorizedScope / auditEvidenceRef。工单 title 后端有。结算 `settlementItems` 后端有。
- 字典 `draft` 不是后端字段名，是**错误的状态码键**。
- `types.ts` 仍留 `MerchantScopeBillRow` 旧假字段，**不在本模块六文件**，不列入本模块 finding。

---

## 6. M023-001…006 独立重核（不复用旧票正文）

对照现码与后端，六条修复均落地，无回归：

| ID | 现码事实 | 本席 |
|---|---|---|
| 001 | 金额 `totalAmountCent`；无假列/假 KPI；夹具蛇形真字段 | 已修成立 |
| 002 | 无 confirmScope；证据码 `merchant.service_work_order.view` / `merchant.billing.view`；事件 `merchant_service.work_order.viewed / merchant_billing.bill.viewed` | 已修成立 |
| 003 | 账单页内字典；结算 `voided: "已作废"`；三页未知中文占位 | 已修成立；**未覆盖合同 `drafting`** |
| 004 | `CLOSED = settled|voided`；KPI 未结清测试期望 `"3"` | 已修成立 |
| 005 | Scope/Contracts 无 TraceLink；Settlements → ADM-MER-006 | 已修成立 |
| 006 | 明细渲染 items；留言 POST；文案不改状态/不暂停付款；权限门 | 已修成立 |

「队列否」≠ 模块无缺陷。

---

## 7. ponytail-review（可删，不修）

- `MerchantZoneContractsPage.tsx:L19`：`draft: "草拟"` 对后端无用，正确键应是 `drafting`（这是缺陷不是单纯死码）。
- `archived: "归档"`：合同 Java 域 0 处 `archived` 生命周期写入，预置键。不单独立项。
- Lean already 其余。`net: 错键 1 处需改不是删。`

---

## 8. Findings（临时编号；B2 才给正式 ID）

### GR-01 我的合同把后端真码 `drafting` 当成未知状态

- **文件:行 / 原句**
  - `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:18-34`

```
const STATUS_LABELS: Record<string, string> = {
  draft: "草拟",
  ...
};
function statusLabel(code: string | null | undefined): string {
  if (!code) return "-";
  return STATUS_LABELS[code] ?? UNKNOWN_STATUS;
}
```

  - `backend/src/main/java/com/complex/platform/contract/ContractService.java:206` 原句：`"drafting", trimToNull(actual.projectId()), ...`（起草落库）
  - 同文件 `:291` / `:330` 驳回回 `drafting`
  - `JdbcContractRepository.java:286-289` `listContractsByMerchant`：`WHERE merchant_id = :mid` **无状态过滤**
  - 测试夹具 `MerchantZoneContractsPage.test.tsx:9-12` 只有 effective/expired/weird_status

- **正常触发（第一次就会撞上）**  
  平台起草一份该商户合同后，商户打开「我的合同」。接口 `lifecycle_status=drafting`。状态列与抽屉均为「未知状态」，不是「草拟」。英文原码不会出现（未知占位挡住了），所以旧的 003 用例仍绿。

- **四分类顺序**  
  冲突？映射的 BSM-001/BSM-003/PGM-001 摘录没有「草拟」二字，**不构成与已映射 HTML 明文冲突**。  
  → **细化**（合同状态展示是「我的合同」只读页的下位实现）且 **有工程缺陷**。  
  不是技术性（业务枚举）。不是添加。

- **已有判据**  
  GEN-008（前后端口径不一致）、GEN-002（状态列声称与真接口不符）、GEN-003（关键分支 drafting 无会失败的断言）、GEN-015（已知态被显示成未知）。  
  不引用未映射的 BEQR-合同状态 / BSM-073 当判据（锁外主张作废）；BEQR L239「草拟、待确认…」仅作分类对照。

- **依据类型**  
  **4**：命令与输出：

```
python3 - <<'PY'
STATUS_LABELS = {"draft":"草拟","pending_confirmation":"待确认","pending_effective":"待生效","effective":"已生效","renewing":"续签中","expired":"已到期","terminated":"已终止","archived":"归档"}
print("drafting ->", STATUS_LABELS.get("drafting", "未知状态"))
print("draft ->", STATUS_LABELS.get("draft", "未知状态"))
PY
# drafting -> 未知状态
# draft -> 草拟
```

  加类型 4 的源码对照（`rg '"drafting"' ContractService.java` 命中 L206/291/330）。

- **工程缺陷是否成立**  
  成立。最小处置（供 B2 定稿，本席不修）：字典键 `draft` 改为 `drafting: "草拟"`；夹具加一条 `lifecycle_status: "drafting"` 断言「草拟」且不含 `drafting` 英文。不要为对表去改 `types.ts`/后端/全局 `statusLabels`（全局 `drafting→起草中` 与页内「草拟」不同，本页已有页内字典）。

无其他独立 FAIL 条。无添加项。001–006 不重开。

---

## 9. 三线执行记录

| 线 | 做了什么 | 结果 |
|---|---|---|
| 第一性 | §3 三问 | 三页有存在理由；合同状态前提为假 |
| 顺从 | §4 全部 21 ID | GEN-002/003/008/015 在 GR-01 上失败；GEN-013/014 不适用并登记 |
| 对抗 | §5 全问 | 伪字段/伪动作未回归；起草态映射是新的真证据 |

---

## 10. 单席结论

runbook §9：有未定稿缺陷 → 不得 PASS。

**FAIL**

回退含义（给席甲，本席不执行）：GR-01 无正式 ID 且 001–006 已不在修复队列 → 整批回 B2 分类定稿后再 B3，禁止直接修。
