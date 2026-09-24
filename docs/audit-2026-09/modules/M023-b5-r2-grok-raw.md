CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B5-r2 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B5-r2（修复后整模块三线全量重审，不只看 diff、不复用 B2/B4/B5-r1 票）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区；`git rev-parse HEAD` → `e456ba96c84185c8d39f18f548011daac48a6466`（与派发 after tree 不同，见下）
- after tree（派发声明）：`144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f`。本席对六文件逐文件 `sha256sum` 工作区现值，并与 `git show 144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f:<path> | sha256sum` 对照，**六文件全部 YES**。行数与 SHA 以工作区实算为准。
- 模块：`docs/audit-2026-09/plan/modules.md` V0.04「商户档案、商户类型与商户侧门户／网页与验证／第1段」六文件；计划有效内容行 991；工作区物理行合计 **1249**（`cat` 六文件 `| wc -l`）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar `criteria-lock.sha256`、ledger 顶部同值；本席未从基准抄入本行）
- 映射 21 项（锁 L6402）：`M023 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`
- findings.md 只读核对：V2.76；M023-001…007 修复队列均为「否（已修）」；当前队列「是」=0。本席不把「全部否」当作功能已全实现，允许真实新证据或回归。
- B3 累计：2（派发背景）；本席不改 ledger / findings
- 独立性：未读 `M023-b5-r2-self-raw.md`；可读旧 B1/B2/B4/B5-r1；未用 MCP / 子代理 / 第三席；未问人；未改业务代码 / 三 HTML / 锁 / findings / ledger；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时产物
- 结论：**PASS**（六文件全文覆盖、21 项逐项记录、三线执行完毕、001–007 独立复验已落地无回归、无未定稿工程缺陷、无添加项、无未答 questions）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令「只允许两席、不问人、不修」优先于技能本体里的问人 / 派子代理 / 改代码闸。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先 |
| using-arc | 已读 | 任务像 Arc audit，但用户禁止子代理/第三席，不走 Arc 派评审席、不写 `docs/arc/audits/` |
| graft | 已读并调用 | `graft grep listContractsByMerchant`、`graft grep DISPUTABLE_STATUSES` 定位后端；审计要求全文读六前端文件，graft 不替代全文 |
| long-running-background-tasks | 已读 | vitest/tsc 均前台短跑完成，未挂后台 |
| grilling / grill-me | 已读 | 只借追问结构（见 §5）；事实自读；不问人；不派子代理。grill-me 本体是 `/grilling` 包装，口径同上 |
| batch-grill-me | **缺口** | 本环境未找到独立 SKILL.md |
| ubiquitous-language | **缺口** | 本环境未找到；domain-modeling 按 §6 划掉 |
| codebase-design | 已读 | 三页是 AdminApi 上的浅界面；深度在后端闭集与页内字典对齐 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩 |
| tdd / test-driven-development | 已读 | 用来判断「改坏是否会红」；本席不写测试、不修代码 |
| verification-before-completion | 已读 | 先跑 vitest/tsc 再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；旧票只作背景，现码重核；B5 禁止 IMPLEMENT |
| requesting-code-review | 存在，B4 用 | B5 不适用 |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → `NO_MERGE`，无冲突可解 |
| git-guardrails-claude-code | 可选存在（多宿主副本） | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §7 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | 存在 | Codex 回执模式；本席正文只落 raw |

Arc `audit` 技能要求派子代理：与本轮「只允许两席」冲突，按用户指令不执行。

graft 本轮检索约节省 ~169,366 tokens（`listContractsByMerchant` ≈159,656 + `DISPUTABLE_STATUSES` ≈9,710）。`chk_contracts_lifecycle_status` 的 graft 无命中（SQL 迁移未进 graft 索引），改用仓库 `grep` 读 V314/V307/V026/V022。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l` + `sha256sum`（工作区现值）。与 after tree `144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f` 六文件 SHA 全等。

| 文件 | 版本标记 | 行 | SHA256 | 覆盖 |
|---|---|---|---|---|
| `apps/web/src/admin/pages/MerchantScopePage.tsx` | V0.01 | 217 | `c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9` | 全文 |
| `apps/web/src/admin/pages/MerchantScopePage.test.tsx` | V0.01 | 211 | `aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx` | V0.02 | 175 | `8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx` | V0.02 | 157 | `f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx` | V0.01 | 272 | `271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8` | 全文 |
| `apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx` | V0.01 | 217 | `2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b` | 全文 |

相关三 HTML（只读，铁律 4 未改）：

- `docs/business-system-map-for-operations-v0.1.html`：BSM-001（锁摘录 h2:505）、BSM-003（h2:563）；商户专区上下文 L1322（**未映射到 M023**，只作 2.1 分类对照，不新增判据）
- `docs/platform-governance-map-for-operations-v0.1.html`：PGM-001（锁摘录 h2:256，含全链可点击 / 零英文 / 金额到分 / 重复提交）
- `docs/business-enum-quick-reference-v0.1.html`：合同状态 `td.k` L239、退租结算状态 L252、商户服务工单状态 L264（**均未映射到 M023**，只作分类对照）

追现行真实现（只读，非本模块文件）：`MerchantPortalController.java` / `MerchantPortalService.java` V0.01、`MerchantBillRecord.java`、`MerchantWorkOrderRecord.java`、`TerminationSettlementMath.java`、`MerchantContractView.java`、`ContractSettlementRecord.java`、`ContractService.java`（drafting / amended）、`JdbcContractRepository.listContractsByMerchant:286-289`（无状态过滤）、`MerchantServiceController`/`MerchantServiceService.listWorkOrders`、`MerchantBillingController`/`MerchantBillingService.listBills`、`JdbcMerchantServiceRepository.findWorkOrdersByScope`、`JdbcMerchantBillingRepository.findBillsByMerchantId`、迁移 `V022`/`V026`/`V162`/`V163`/`V307`/`V314` 的 CHECK 闭集、`AdminApi`（`adminClient.ts` unwrap/snake_case）、`adminRoutes.ts` ADM-MER-002/005/006、`accountFormOf:171-176`、`routeAllowsAccountForm:179-181`、`AdminApp.tsx` `navigateTo:545-563`、`useAdminAction.ts`、`useSelectedRow.ts`、`useAdminResource.ts`、`PermissionButton`（`Can.tsx`）、`TraceLink.tsx`、`RowDetailDrawer.tsx`/`cnDate`、`AuditTrailHint`、`formatBusinessStatus`（`customer/i18n/statusLabels.ts`）、`apiError.ts`、`application.yml` `property-naming-strategy: SNAKE_CASE`。

---

## 2. 可执行证据（verification-before-completion）

命令 1（类型 2）：

```
cd /home/james/complex-management/apps/web
npm run test:unit -- src/admin/pages/MerchantScopePage.test.tsx \
  src/admin/pages/MerchantZoneContractsPage.test.tsx \
  src/admin/pages/MerchantZoneSettlementsPage.test.tsx
```

exit 0。`Test Files  3 passed (3)`；`Tests  32 passed (32)`（Scope 14 / Contracts 8 / Settlements 10）；Duration 1.15s。jsdom 打印 `Not implemented: Window's scrollTo()`（KPI 聚焦里 `scrollTo`，非失败）。

命令 2（类型 4 / 构建可执行性）：

```
cd /home/james/complex-management/apps/web && npm run typecheck
```

exit 0。`tsc -b --noEmit` 无诊断输出。

命令 3（类型 4，锁哈希，本席独立）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
```

→ `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`

未连库跑后端集成（本模块是网页与验证；派发允许的可执行范围是这三个测试文件与类型检查）。后端闭集用源码 + 迁移原文（类型 4 读文件命令产出）。

---

## 3. 第一性原理三问

### 3.1 为什么必须存在？删掉哪条 HTML 做不到？

三页对应业务地图 L1322 商户专区自助里「我的工单 / 我的账单 / 我的对账 / 我的合同」的网页承载（授权工作台是工单+账单的只读授权范围视图；专区两页是合同与对账）。删掉则 L1322 括号里的「该交多少」「进度自己看」「结算与分成明细，有疑问可留言提异议」「租期、到期情况」在本模块文件集上无入口。

映射到本模块的 HTML 锚点是 BSM-001（整体说明：签了什么合同、对账结算、商户表现）、BSM-003（权限分岗的父 h2）、PGM-001（归属/资格/责任底座 + 全链可点击 + 零英文 + 金额到分 + 重复提交只认一次）。答得上 → 有业务上位。L1322 本身未映射进 M023 21 项，只作 2.1 上位对照，不新增判据。

### 3.2 声称的行为有没有被实际执行验证？

有。§2 vitest 32/32 覆盖：KPI 聚焦与返回、本商户过滤、平台管理员无范围不请求、金额分转元、账单 issued≠已出票、未知码中文占位、无伪确认按钮、真实权限码与留痕事件名、无假下钻、行抽屉、起草态 `drafting→草拟`、留言 POST + 空理由/超长/已结清/已作废/无权限、未结清不含 voided、结算明细中文名与分转元、合同下钻 ADM-MER-006。tsc 无类型错误。

### 3.3 哪个前提未经验证就当真？

本席逐条核过、不再当「未验证前提」的：

- 页面对后端的每个调用、字段、事件、权限、下钻、页内字典键：见 §4 / §5，均追到 Controller/Service/Record/CHECK/AdminApi/路由。
- `amended`：B2-r2 双席定为登记不入队。本席重读 V314:13-17 与 `ContractService.java:1180`，确认仍是后端第 9 态、三份 HTML 仍无中文名、本页字典仍无该键 → 显示「未知状态」。**无新证据，不改判。**
- 工单状态走全局 `formatBusinessStatus`，中文与枚举 L264 用词不完全同字（`created→已创建` vs HTML「已提交」等）。L264 未映射到 M023；展示仍是中文、不是英文原码或「未知状态」。不为同义中文另开需求。
- 对账页字典多一个 `disputed_litigation` 键：全仓 Java/SQL 0 处写入，V307 CHECK 六值不含它；HTML L252 有「有争议，诉讼/仲裁」中文。多映射一个后端永不返回的键不会把真码打成未知，也不发明新业务态。不入队。

未连 `complex_test` 跑 CHECK 的 `SELECT`（派发未要求；迁移原文已读）。该缺口不构成未验证的功能前提。

---

## 4. 顺从式：21 项逐项适用 / 证据

判定顺序固定：冲突 → 细化 → 技术性 → 添加。不新增判据。

| ID | 适用 | 证据（类型） | 结论 |
|---|---|---|---|
| BSM-001 | 适用（整体说明含合同/对账/商户） | 类型 1：HTML h2:505「签了什么合同」「对账、结算、商户表现」。三页只读展示本商户合同/工单/账单/结算，改价审批仍走平台（ContractsPage.tsx:13）。类型 2：vitest 证明列表与 KPI 来自真实 API 形状。 | 通过 |
| BSM-003 | 适用（权限分岗父锚点） | 类型 1：h2:563 仅声明「整套系统都遵守的通用规则」。本页写动作仅对账留言，按 `merchant.portal.settlement.dispute` 门控（SettlementsPage.tsx:15,255-261；后端 Service:116）。查看走路由 requiredPermissions。类型 2：无权限用例按钮禁用且不 POST。 | 通过 |
| GEN-001 | 适用 | 六文件均有可验证用途（授权范围视图 / 我的合同 / 我的对账 + 测试）。001–006 已删伪字段/伪动作/假下钻。无不可达重复抽象。 | 通过 |
| GEN-002 | 适用 | 类型 2：32 测全绿；类型 4：tsc exit 0。声称入口（三 GET + 一 POST）与测试夹具/断言一致。 | 通过 |
| GEN-003 | 适用 | 测试执行真实渲染与点击，断言能因改坏而红（见 §5「改坏」）。007 夹具含 `lifecycle_status: "drafting"` 并断言「草拟」、不含「未知状态」、不含英文 `drafting`（ContractsPage.test.tsx:91-103）。不是「通过即正确」。 | 通过 |
| GEN-004 | 适用 | 留言理由 trim + 空拒绝 + maxLength 500（SettlementsPage.tsx:165-173）；后端 `disputeOnce` 再校验 reason / 列宽 / 商户归属（Service:132-153）。列表输入来自已鉴权 API。 | 通过 |
| GEN-005 | 适用 | 服务端：合同 `contract.view` + `requireMerchantScope`；结算 view/dispute 同；工单 `merchant.service_work_order.view` + `findWorkOrdersByScope`；账单 `merchant.billing.view` + `findBillsByMerchantId`。前端：路由权限；留言 PermissionButton DENY 优先。平台管理员开授权工作台因无 merchantId **不发请求**（ScopePage.tsx:110-117；测试 :115-122），避免打到工单「无 scope 则全表 LIMIT」与账单 `findAllBills`。 | 通过 |
| GEN-006 | 适用 | 本模块无写库。留言走后端事务 + 幂等键（Service:121-122）；页面不改结算状态。 | 通过 |
| GEN-007 | 适用 | 留言 `idempotencyKey: nextKey(...)`，AdminApi 另保底填键；后端 `idempotencyService.execute("merchantPortalSettlementDispute:"+id, key, ...)`。busyAction 提交中禁用按钮。同键重放由后端只认一次（PGM-001）。不同键的连点属后端幂等键粒度，不在本页发明第二套去重。 | 通过 |
| GEN-008 | 适用 | 见 §5 字典闭集表：账单 6/6、结算状态 CHECK 6/6（页内另有 HTML 有名、库无码的 `disputed_litigation` 闲键）、触发 3/3、合同 CHECK 8/9（缺 `amended`，B2-r2 不入队）、工单类型/优先级/状态均可经全局表中文化。字段 `totalAmountCent` / `lifecycleStatus` / `settlementItems.amountCent` 经 SNAKE_CASE + AdminApi `mapKeys` 对齐。事件名、权限码与后端 `recordSuccess` / `requirePermission` 同字。 | 通过 |
| GEN-009 | 适用 | 空理由/超长前端中文错误且不发请求（测试 :100-109）。不可留言态按钮禁用。后端冲突/校验经 `useAdminAction` 把 `err.message` 设到 formError（apiError 层要求中文业务话术）。scope 缺失抛中文「缺少商户授权范围」。无吞错、无伪成功。 | 通过 |
| GEN-010 | 适用 | 无注入点（React 文本节点）；越权靠服务端 scope；无秘密。AuditTrailHint / TechDetails 中的 API 路径与权限码属系统内部标识（PGM-001 四样例外）。 | 通过 |
| GEN-011 | 适用 | 页面声称的留痕事件均在后端存在：`merchant_service.work_order.viewed`、`merchant_billing.bill.viewed`、`merchant_portal.contract.viewed`、`merchant_portal.settlement.viewed` / `.disputed`。已无 `merchant.scope_confirmed`。测试断言证据文案。 | 通过 |
| GEN-012 | 适用 | 前端 `usePagedRows(..., 10)`；后端合同/结算默认 page size 500，工单 LIMIT 100，账单 LIMIT 200。有界。超限截断是后端模块口径，本页展示返回集，不为理论超限加前端假分页协议。 | 通过 |
| GEN-013 | 适用 | `apps/web` 的 `test:unit` / `typecheck` 已实跑 exit 0。 | 通过 |
| GEN-014 | 适用为依赖、本模块无 SQL | 页面字典所依赖的 CHECK 原文：V026 账单六态、V022 工单类型/优先级/状态、V162 触发三值、V307 结算六态、V314 合同九态。本模块不改迁移。 | 通过 |
| GEN-015 | 适用 | 行 `tabIndex=0` + Enter/Space；抽屉 role=dialog / Esc / 焦点陷阱（RowDetailDrawer）。未知码中文占位。日期 `cnDate` 中文。金额两位小数+「元」。工单/合同/账单状态无英文原码（测试）。 | 通过 |
| GEN-016 | 适用 | TEXT.evidence 写真实路径与权限码（ScopePage.tsx:19）；测试钉死不含 `merchant.portal.read`。AuditTrailHint 事件名可在后端 grep 到。 | 通过 |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加走完。001–007 已定稿不重开。无新添加项。`amended` 不改判。 | 通过 |
| GEN-018 | 适用 | 本报告结论落到 HTML 原文、vitest/tsc 产出、迁移/源码可复现读取。不引用「上轮我判过」。 | 通过 |
| PGM-001 | 适用 | 全链可点击：对账合同下钻 ADM-MER-006（商户可达）；合同号/空间/续签自/授权工作台商户号无商户可达目标 → 纯文本+行抽屉，不渲染点了没反应的按钮。零英文：状态/触发/条目/日期/金额中文；单据号与内部标识原样。金额分→元四舍五入到分（`Intl` 两位）。留言不改状态、不暂停付款（文案+后端只写 dispute_reason）。重复提交只认一次由后端幂等键执行。 | 通过 |

---

## 5. 对抗式（grilling 结构，事实自读）

不问人。对用户点名的反例逐条用现行码回答。

### 5.1 真实后端返回下，三页每列 / 每个 KPI 显示什么

**授权工作台** `GET /api/v1/merchant-service/work-orders` + `GET /api/v1/merchant-billing/bills`（AdminApi 解包 `data.items`，蛇形键转驼峰）。只保留 `row.merchantId === user.merchantId`。

| 表面 | 数据 | 显示 |
|---|---|---|
| KPI 商户工单 | `workOrders.length` | 数字，可点 → `?kpi=workOrders` 只留工单表 |
| KPI 商户账单 | `bills.length` | 数字，可点 → `?kpi=bills` 只留账单表 |
| KPI 账单金额 | `sum(totalAmountCent)` /100 | `12,345.00 元` 纯文本不可点 |
| 工单列 商户/标题 | `merchantId` / `title` | 原样（标识/标题） |
| 工单列 类型/优先级/状态 | `workOrderType` / `priority` / `status` | `formatBusinessStatus`；若返回值等于原码 →「未知状态」 |
| 账单列 商户/账单号 | `merchantId` / `billNo` | 原样 |
| 账单列 金额 | `totalAmountCent` | 分→元 |
| 账单列 状态 | `status` | 页内账单字典（见下） |

**我的合同** `GET /api/v1/merchant-portal/contracts` → `MerchantContractView`。

| 表面 | 数据 | 显示 |
|---|---|---|
| KPI 合同总数 | `rows.length` | 数字，可点 → `?kpi=contracts` |
| KPI 已生效 | `lifecycleStatus==="effective"` 计数 | 数字，可点 → `subset=effective` |
| 合同 | `id` | 表内截尾 8 位+title 全量；抽屉全量 |
| 租期 | `effectiveDate`~`expiryDate` | `cnDate`（`2026-01-01`→`2026年1月1日`） |
| 空间 | `spaceIds` | `、` 拼接或 `-`（内部标识，允许原文） |
| 状态 | `lifecycleStatus` | 页内合同字典 |
| 押金 | `depositCent` | 分→元 |
| 续签自 | `renewedFromContractId` | 截尾 8 位 / `-` |

**我的对账** `GET /api/v1/merchant-portal/settlements`。

| 表面 | 数据 | 显示 |
|---|---|---|
| KPI 结算单 | `rows.length` | 可点 → `?kpi=settlements` |
| KPI 未结清 | 状态∉{settled,voided} | 可点 → `subset=open`；测试钉死计数 3（排除已结清+已作废） |
| 结算单/合同 | `id` / `contractId` | 合同为 TraceLink ADM-MER-006 |
| 触发/状态 | `triggerType` / `status` | 页内触发/状态字典 |
| 留言理由 | `disputeReason` | 原文或 `-` |
| 开账时间 | `createdAt` | `cnDate` |
| 抽屉结算明细 | `settlementItems[].item/amountCent/note` | 条目中文 + 分→元 + 括号 note |

### 5.2 后端每个真实状态码在屏幕上显示什么

**账单** `chk_merchant_bill_status`（V026:18）= issued/viewed/confirmed/disputed/overdue/closed。页内 `BILL_STATUS_LABELS`（ScopePage.tsx:28-35）一一对应：已出账/已查看/已确认/有异议/已逾期/已结清。与 `MerchantZoneBillsPage.tsx:18-25` 同词。`issued` 不走全局「已出票」。未知 →「未知状态」，不回显英文。

**工单** V022: type ∈ {repair,complaint,rectification,service}；priority ∈ {low,medium,high,critical}；status ∈ {created,assigned,processing,pending_merchant,resolved,closed,cancelled}。全部能在 `statusLabels.ts` 落到中文（报修/投诉/整改/服务；低/中/高/紧急；已创建/已派单/处理中/商户待处理/已解决/已关闭/已取消）。与 L264 用词不完全同字，见 §3.3。

**合同** V314 CHECK 九值。页内八键（ContractsPage.tsx:18-27）：

| 码 | 页内 |
|---|---|
| drafting | 草拟 |
| pending_confirmation | 待确认 |
| pending_effective | 待生效 |
| effective | 已生效 |
| renewing | 续签中 |
| expired | 已到期 |
| terminated | 已终止 |
| archived | 归档 |
| amended | （无键）→「未知状态」。B2-r2 登记不入队，本席不改判 |

全局表 `drafting→起草中` 本页不用。测试断言不含「起草中」。

**结算状态** V307 CHECK 六值 vs 页内七键（SettlementsPage.tsx:39-47）：

| 码 | 页内 | CHECK |
|---|---|---|
| pending_settlement | 待结算 | 有 |
| deposit_offsetting | 押金抵扣中 | 有 |
| arrears_collecting | 欠费追缴中 | 有 |
| settled | 已结清 | 有 |
| disputed | 有争议 | 有 |
| voided | 已作废 | 有 |
| disputed_litigation | 有争议，诉讼/仲裁 | **无**（全仓 0 写入；闲键） |

**触发** V162: move_out/expiry/early_termination → 退租/到期/提前终止。

**明细 item** TerminationSettlementMath: unpaid_receivable_summary/deposit_disposal/termination_penalty → 未清应收合计/押金处置/提前退租违约金；其它 →「其他条目」（测试 mystery_item）。

### 5.3 商户点每个可点元素会怎样

授权工作台：KPI 工单/账单 → pushState 聚焦；账单金额不可点；行/「查看详情」→ 抽屉（中文字段）；Esc 关；无 TraceLink；无确认按钮。

我的合同：KPI 总数/已生效 → 聚焦；行/详情 → 抽屉（全量合同号、中文租期/状态、押金元、空间与续签自纯文本）；pager 翻页；无 navigateTo。

我的对账：KPI 结算单/未结清 → 聚焦（聚焦态**隐藏**留言表单，点「返回全部」恢复）；单选选行（`useSelectedRow` 在有行时会把空选收成第一行）；行/详情 → 抽屉（明细数字+合同 TraceLink）；点合同 TraceLink → `navigateTo("ADM-MER-006", contractId)` 且 stopPropagation 不弹抽屉（测试 :210-215）；提交留言 → POST `/api/v1/merchant-portal/settlements/{id}/dispute`，body `reason` + `idempotency_key`。

### 5.4 无留言权限 / 已结清 / 已作废 / 空理由 / 超长理由 / 重复提交

| 情形 | 现行行为 | 证据 |
|---|---|---|
| 无 `merchant.portal.settlement.dispute` | 按钮 disabled，onClick 被 PermissionButton 拦截，0 POST | 测试 :123-132；Can.tsx:66-78 |
| 已结清 / 已作废 | `DISPUTABLE` 仅 pending_settlement/deposit_offsetting/arrears_collecting，与后端 `DISPUTABLE_STATUSES` 同集；按钮禁用 | 测试 :112-121；Service:32-33,144-146 |
| 空理由 | 前端「提交留言必须填写理由」，不发请求 | 测试 :100-109；:165-168 |
| >500 字 | 输入 maxLength=500；trim 后仍超则「留言不能超过 500 字」不发请求；后端合并后超列宽再拒 | :169-173；Service:151-153 |
| 重复提交同键 | 后端幂等只认一次 | Service:121-122 |
| 提交中再点 | `busyAction==="merchant_zone_settlement_disputed"` 禁用 | :259 |

聚焦视图藏表单不是缺能力：返回全部后表单仍在。不为该交互加需求。

### 5.5 平台管理员打开授权工作台会怎样

ADM-MER-002 **未**标 `accountForms`，平台形态可进页。`accountFormOf(platform_admin)` 恒为 `platform`（即使种子挂了 merchantId）。页内 `scopeMissing = !user.merchantId`：无商户号则清空两表、抛中文「当前账号缺少商户授权范围」、**fetch 0 次**（测试 :115-122）。这挡住了后端工单无 scope 全表 LIMIT 与账单 `findAllBills` 的平台全量口径。ADM-MER-005/006 标 `accountForms:["merchant"]`，平台管理员进专区页会被 `routeAllowsAccountForm` 拦下并提示「此页面供商户账号使用」。

### 5.6 改坏任一页面行为是否有用例会红

| 改坏 | 会红的用例 |
|---|---|
| 合同字典键改回 `draft` | 「后端起草态真码 drafting 显示草拟」（:91-103） |
| 金额读 `totalAmount` | 「金额读 total_amount_cent」（Scope 测试 :131-137） |
| 账单走全局 issued→已出票 | 「issued→已出账（非票务义已出票）」（:139-150） |
| 恢复确认按钮 / 假事件 | 「没有不调后端的确认授权范围按钮」「不含 merchant.scope_confirmed」（:152-173） |
| 未结清只排除 settled | 「未结清不含已结清与已作废」计数 3（:141-154） |
| 合同下钻改 ADM-CONT-001 | 抽屉/表格断言 ADM-MER-006 且无 ADM-CONT-001（:197-198） |
| 空理由仍 POST | 「必须填写理由」且 `calls` 无 POST（:100-109） |
| 未知码回显英文 | 三页 `not.toContain("weird_…")` / 不含 `drafting` |

未覆盖到的字典键（如 `deposit_offsetting` 显示）是同一 `statusLabel` 查找表，改键会与 CHECK 对拍失败的风险已被 007 的 drafting 用例钉住同类缺陷。不为每个闲键加测。

### 5.7 是否还有不调后端的伪动作或后端不存在的字段

未发现。授权工作台已无 `confirmScope` / `authorizedScope` / `auditEvidenceRef` / `totalAmount` / 账单 title。合同/工单/账单列均落在 Record/View 字段上。留言是真 POST。KPI 只改 URL 与本地过滤，不冒充写接口。

---

## 6. 001–007 独立复验（不把「否」当已实现）

| ID | 现码 | 复验 |
|---|---|---|
| 001 | Scope 行类型 `totalAmountCent`/`billNo`/`title`（工单）；无授权范围列/KPI | 通过。测试金额 100.00/25.50、合计 125.50 |
| 002 | 无确认按钮；证据权限 `merchant.service_work_order.view` + `merchant.billing.view`；事件 work_order.viewed / bill.viewed | 通过 |
| 003 | 账单页内字典；结算 `voided:已作废`；三页未知中文占位 | 通过 |
| 004 | `CLOSED={settled,voided}` | 通过。KPI 未结清=3 |
| 005 | 对账合同 → ADM-MER-006；合同页/授权工作台无假 TraceLink | 通过 |
| 006 | 抽屉渲染 settlementItems；留言 POST dispute；文案不改状态、不暂停付款 | 通过 |
| 007 | 字典首键 `drafting: "草拟"`（ContractsPage.tsx:18-19）；夹具 dddd4444 + 行/抽屉断言 | 通过。与 `ContractService.java:206/291/330`、V314 CHECK 对齐 |

`amended`：仍无页内中文名，仍显示「未知状态」，仍不泄英文。无新证据，维持 B2-r2 登记不入队。

---

## 7. ponytail-review（可删项，B5 不修）

- 对账页 `disputed_litigation` 键：后端永不返回。留下无害（HTML 有该中文名）；删掉也不改变真码显示。不入队。
- 授权工作台客户端再按 `merchantId` 过滤：后端商户 scope 已过滤；客户端是防平台全量的第二闸，与「无 merchantId 不请求」一起构成最小防御，不删。

无「不调后端的伪动作」可删残留。

---

## 8. 本次 findings

无。

不把下列观察升格为 finding（不为理论场景加需求、不改 B2-r2 对 amended 的定稿）：

1. `amended` →「未知状态」（已登记不入队）。
2. `disputed_litigation` 闲键（库无此码）。
3. 工单全局中文与 L264 用词不完全同字（未映射 BEQR）。
4. 后端工单 LIMIT 100 / 账单 LIMIT 200（非本模块文件）。

无添加项。无 questions。无 HTML 自身问题需铁律 4 上报。

---

## 9. 三线执行记录

| 线 | 执行 |
|---|---|
| 第一性原理 | §3 三问；依据类型 1/2/4 |
| 顺从式 | §4 全部 21 项 |
| 对抗式 | §5 grilling 全套反例 |

六文件均有覆盖记录。声称均有四类证据之一。

---

## 10. 最终结论

**PASS**

B5-r2 整模块重审：001–007 修复落地、无回归、无新的未定稿工程缺陷。Grok 席乙票 PASS。
