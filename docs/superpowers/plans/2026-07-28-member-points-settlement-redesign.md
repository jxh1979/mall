# Member, Points, and Merchant Settlement Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以已批准的书面设计为唯一重构依据，建立会员、积分与商户积分结算三份权威文档，完成所有相关 PRD 的逐文件双重审计，并在不修改业务 HTML 的前提下形成逐条替代清单。

**Architecture:** 文档采用“三份领域权威文档 + 现有 PRD 精准引用与同步 + HTML 替代清单”的单向权威结构。每个阶段独立建分支、核验、合并并推送到 `192.168.88.160:/home/james/complex-management` 的 `main`；`main` 是唯一最终版本。用户已选择根代理串行执行，因此实施时使用 `superpowers:executing-plans`，不启动子代理。

**Tech Stack:** Markdown、CSV、只读 HTML、Git、`rg`、SSH、SHA-256。

## Global Constraints

- 只审计和修改 PRD 及其直接相关的文档入口、决策记录、范围清单与覆盖矩阵。
- 不修改代码、OpenAPI、数据库、测试、开发设计、运行时配置或部署资产。
- 三个业务 HTML 在替代清单获用户逐项批准前保持只读；任何阶段都不得提前改写。
- 已批准设计文件：`docs/superpowers/specs/2026-07-28-member-points-settlement-redesign-design.md`。
- 新建三份权威文档：
  - `docs/member-and-tier-product-prd-v1.0.md`
  - `docs/points-and-voucher-product-prd-v1.0.md`
  - `docs/merchant-points-settlement-rules-v1.0.md`
- 对冲突逐项裁决，不给历史文档、备份分支或 HTML 之外的来源设置统一优先级。
- `/home/james/codex-backups/` 只能作为差异线索和证据来源，不能直接覆盖 `main`。
- 保留 `main` 中现有的四项未跟踪审计资料，不纳入提交、不删除、不移动。
- 每阶段开始前记录 `main` 与 `origin/main` 一致；结束后合并并推送，再次验证两者一致。
- 所有参数、规则、审批、版本和运行依据必须由系统管理；文档不得把线下表格定义为运行时真源。
- 每次修改都必须能映射到设计条款、审计发现或用户明确裁决，禁止顺带重写无关内容。

---

## Phase 1：冻结基线并建立审计范围

### Task 1：建立基线与文件清单

**Files:**

- Create: `docs/prd-audit/member-points-rebuild-scope-2026-07-28.md`
- Read: `docs/superpowers/specs/2026-07-28-member-points-settlement-redesign-design.md`
- Read: `docs/prd-audit/document-scope-manifest.csv`
- Read: `docs/prd-audit/html-prd-coverage.csv`
- Read: `CONTEXT.md`

- [ ] 在远程创建阶段分支和工作区：

```bash
cd /home/james/complex-management
git fetch origin
test "$(git rev-parse main)" = "$(git rev-parse origin/main)"
git worktree add -b codex/member-points-phase1-baseline /home/james/wt-member-points-phase1 origin/main
```

- [ ] 记录基线提交、设计文件 SHA-256 及以下三个业务 HTML 的 SHA-256：

```text
docs/business-system-map-for-operations-v0.1.html
docs/platform-governance-map-for-operations-v0.1.html
docs/business-enum-quick-reference-v0.1.html
```

- [ ] 从 `docs/prd-audit/document-scope-manifest.csv` 取出所有 `MAIN,prd` 文件，并以会员、等级、成长值、积分、冻结、退款、兑换、虚拟券、结算、商户、营销费用、内部结算价、零售参考价等词进行全文发现。

- [ ] 在范围文档中为每个命中文件建立一行，字段固定为：路径、当前角色、命中主题、正式审计状态、目标权威文档、处置方式、证据。

- [ ] 处置方式只允许：`CREATE_AUTHORITY`、`REPLACE_WITH_REFERENCE`、`PRECISE_SYNC`、`AUDIT_ONLY`、`OUT_OF_SCOPE_WITH_REASON`。

- [ ] 明确纳入以下直接相关文档，不因关键词缺失而遗漏：

```text
docs/member-loyalty-and-stored-value-product-prd-v0.1.md
docs/member-points-tier-and-clearing-prd-v1.0.md
docs/member-points-p1-ops-parameter-tables-v0.1.md
docs/stored-value-membership-checkin-birthday-prd-v0.1.md
docs/finance-settlement-product-prd-v0.1.md
docs/settlement-rebuild-prd-v1.0.md
CONTEXT.md
docs/business-decision-log-v0.1.md
docs/prd-audit/business-decision-ledger.md
docs/00-current-effective-document-entry-v0.1.md
docs/product-documentation-index-v0.1.md
docs/system-capability-layers-product-index-v0.1.md
docs/documentation-map-v0.1.md
docs/prd-audit/document-scope-manifest.csv
docs/prd-audit/html-prd-coverage.csv
```

- [ ] 验证范围文档没有把代码、OpenAPI、数据库、测试或开发设计纳入修改范围：

```bash
rg -n "(src/|app/|api/|openapi|database|migration|test/)" docs/prd-audit/member-points-rebuild-scope-2026-07-28.md
git diff --check
```

- [ ] 提交阶段成果：

```bash
git add docs/prd-audit/member-points-rebuild-scope-2026-07-28.md
git commit -m "docs(PRD): 冻结会员积分重构审计范围"
```

### Task 2：合并并推送 Phase 1

- [ ] 在主工作区确认除既有四项未跟踪资料外无额外改动，获取最新 `origin/main`，再将阶段分支合并到 `main`。

- [ ] 若 `origin/main` 在阶段执行期间前进，先把最新主线合入阶段分支并重新执行本阶段检查，禁止覆盖远程提交。

- [ ] 推送唯一最终版本：

```bash
cd /home/james/complex-management
git merge --no-ff codex/member-points-phase1-baseline -m "merge: 会员积分重构阶段一范围基线"
ALLOW_MAIN_PUSH=1 git push origin main
test "$(git rev-parse main)" = "$(git rev-parse origin/main)"
```

---

## Phase 2：建立三份领域权威文档

### Task 3：编写会员与等级权威 PRD

**Files:**

- Create: `docs/member-and-tier-product-prd-v1.0.md`
- Read: approved design sections 3, 4, 10, 11, 12

- [ ] 从最新 `origin/main` 创建 `codex/member-points-phase2-domain-prds` 和 `/home/james/wt-member-points-phase2`。

- [ ] 文档至少包含：目的与范围、术语、角色、全国统一免费会员、跨项目唯一身份、成长值账本、成长值来源、滚动 12 个月统计周期、即时升级、12 个月保级期、30 天降级通知、直接降至符合门槛等级、六级初始等级、连续无空档门槛、等级倍率、版本锁定、状态转换、异常与审计证据、验收标准。

- [ ] 明确成长值与可消费积分完全分账；等级倍率只作用于基础积分，不放大成长值。

- [ ] 明确只有完成且符合资格的付费消费产生成长值；注册、签到、生日、营销奖励、储值充值、押金、积分兑换、零元订单及积分加现金兑换均不产生成长值。积分与成长值分别配置，特殊通票或 SKU 可分别配置固定值；成长值活动倍率必须经过审批、限定期限并版本化。

- [ ] 用可执行验收句覆盖升级、保级、降级、跨项目身份、门槛缺口、版本切换和追溯证据。

- [ ] 验证关键规则均出现且没有引入储值、付费会员、签到或生日赠送的新范围：

```bash
rg -n "滚动12个月|12个月保级|30天|新乐会员|银卡|金卡|白金|钻石|黑钻|成长值|等级倍率" docs/member-and-tier-product-prd-v1.0.md
git diff --check
```

- [ ] 提交：

```bash
git add docs/member-and-tier-product-prd-v1.0.md
git commit -m "docs(PRD): 建立会员与等级权威规则"
```

### Task 4：编写积分与虚拟券权威 PRD

**Files:**

- Create: `docs/points-and-voucher-product-prd-v1.0.md`
- Read: approved design sections 3, 5, 8, 10, 11, 12

- [ ] 明确平台按全国统一业态配置“每 1 积分所需消费金额”，同一业态跨项目一致，项目和商户不可覆盖。

- [ ] 明确有效消费金额只含折后实际支付的现金、银行卡、微信、支付宝和储值余额；不含券抵扣、平台补贴、商户折扣，积分加现金兑换整单不计有效消费。

- [ ] 写入并逐例验证三条计算公式：

```text
基础积分 = floor(本单有效消费金额 ÷ 业态每1积分所需消费金额)
等级加赠积分 = floor(基础积分 × (会员等级倍率 - 1))
本单消费积分 = 基础积分 + 等级加赠积分
```

- [ ] 明确基础积分和等级加赠积分分账、均计商户成本；营销加赠积分另行记账并锁定成本承担方；每单取整且余数不跨单累计。

- [ ] 定义订单锁定业态规则版本与会员倍率版本；部分退款按原版本和保留的有效消费金额重算。

- [ ] 定义待确认、可用、已消耗、已取消、待追回状态；退款期内冻结，正常退款撤销待确认积分；极少数确认后退款形成待追回积分，后续积分抵扣、禁止兑换、不形成负余额和现金债务。

- [ ] 定义积分有效期由平台配置、默认 12 个月；只有达到平台最低门槛且默认不少于 10 元的合格消费刷新全部积分有效期，并列明所有不刷新场景。

- [ ] 定义积分不可转赠且只能用于积分商城；兑换一律生成虚拟券；发券扣积分并预占库存但不结算，核销才产生实际商户补偿；券转赠默认关闭，资格型券强制不可转赠。

- [ ] 明确虚拟券过期通常不返还积分；合规取消、商户无法履约或平台作废时返还积分，并保留原因、审批与原兑换关联。

- [ ] 覆盖纯积分与积分加现金两类兑换的输入、状态、退款、券过期、合规撤销、商户无法履约、平台作废和审计证据。

- [ ] 验证：

```bash
rg -n "每1积分所需消费金额|基础积分 =|等级加赠积分 =|本单消费积分 =|待确认|待追回积分|积分商城|预占库存|核销" docs/points-and-voucher-product-prd-v1.0.md
git diff --check
```

- [ ] 提交：

```bash
git add docs/points-and-voucher-product-prd-v1.0.md
git commit -m "docs(PRD): 建立积分与虚拟券权威规则"
```

### Task 5：编写商户积分结算权威规则

**Files:**

- Create: `docs/merchant-points-settlement-rules-v1.0.md`
- Read: approved design sections 6, 7, 8, 9, 10, 11, 12

- [ ] 定义全国结算中心法律主体、项目、商户、营销活动发起方和成本承担方的职责；商户直接向会员销售和履约，结算中心不是转售方。

- [ ] 定义商户在平台覆盖范围内强制参与、双向总额记账、现金可按月净额支付，废止旧的净积分清算口径。

- [ ] 定义发放单价由平台统一配置且不按商户变化；会员系统服务费按合同在“比例”或“固定月费”中二选一，后台可配置为 0。

- [ ] 定义项目级零售参考价：同一项目、同一 SKU、滚动 30 天内最低有效实际成交价；排除其他项目和外部渠道；新 SKU 在形成有效成交数据前使用经审批的项目首发公开价。

- [ ] 定义项目级内部结算价：各项目独立管理页面和版本，SKU 只能关联本项目价格。

- [ ] 定义兑换结算单价为发放单价乘平台系数，默认 0.7，只用于内部结算覆盖校验。

- [ ] 写入纯积分兑换双下限公式：

```text
零售下限积分 = ceil(零售参考价 ÷ 积分发放单价)
结算下限积分 = ceil(内部结算价 ÷ 积分兑换结算单价)
最低兑换积分 = max(零售下限积分, 结算下限积分)
```

- [ ] 写入积分加现金三项约束和实际补偿公式：

```text
积分数 × 积分发放单价 + 现金支付额 >= 零售参考价
积分数 × 积分兑换结算单价 + 现金支付额 >= 内部结算价
现金支付额 <= 内部结算价
实际结算中心补偿 = max(0, 内部结算价 - 会员向商户支付的现金额)
```

- [ ] 定义特殊审批可低于两项下限，但必须填写原因、目标、期间、数量、缺口、申请人、成本承担方、预算，由结算中心审批；结算中心不默认承担补贴。

- [ ] 定义营销费用可由商户、项目发起平台法律主体、全国平台或其他发起方承担；支持多个承担方按固定额或比例拆分，比例总和必须为 100%，预算分别锁定。

- [ ] 定义自然月账期、每日明细、5 个工作日争议期、次月 15 日净额付款、银行休息日顺延；跨月退款在当期冲正且历史账单不可改写，红字发票和税务按中国税法执行。

- [ ] 明确所有价格、系数、合同、费用、预算、审批、账单、冲正和版本均由系统管理。

- [ ] 验证所有公式、默认值、项目边界和结算时点：

```bash
rg -n "滚动30天|同一项目|内部结算价|默认0.7|ceil\(|max\(0|5个工作日|次月15日|中国税法|系统管理" docs/merchant-points-settlement-rules-v1.0.md
git diff --check
```

- [ ] 提交：

```bash
git add docs/merchant-points-settlement-rules-v1.0.md
git commit -m "docs(PRD): 建立商户积分结算权威规则"
```

### Task 6：收口旧领域文档并更新权威入口

**Files:**

- Modify: `docs/member-points-tier-and-clearing-prd-v1.0.md`
- Modify: `docs/member-loyalty-and-stored-value-product-prd-v0.1.md`
- Modify: `docs/member-points-p1-ops-parameter-tables-v0.1.md`
- Modify: `docs/stored-value-membership-checkin-birthday-prd-v0.1.md`
- Modify: `CONTEXT.md`
- Modify: `docs/business-decision-log-v0.1.md`
- Modify: `docs/prd-audit/business-decision-ledger.md`
- Modify: `docs/00-current-effective-document-entry-v0.1.md`
- Modify: `docs/product-documentation-index-v0.1.md`
- Modify: `docs/system-capability-layers-product-index-v0.1.md`
- Modify: `docs/documentation-map-v0.1.md`

- [ ] 将旧的混合会员积分 PRD 改成清晰的已被替代说明和三份新权威文档入口，保留必要历史说明，不继续承载现行规则。

- [ ] 将“会员、积分、储值”混合文档收缩为储值相关现行内容；会员和积分段落改为指向新权威文档，避免复制规则形成第二真源。

- [ ] 将运营参数表改成“系统配置覆盖检查表”，明确其不是线下配置载体或运行时真源。

- [ ] 在签到、生日和储值文档中明确：此类奖励如发积分，按营销积分规则记账；不产生成长值，也不刷新消费积分有效期。

- [ ] 在 `CONTEXT.md` 增补领域术语与职责边界，但不复制计算公式。

- [ ] 将用户已批准裁决逐条写入两份决策记录，并附三份权威文档链接。

- [ ] 更新当前有效入口和三个文档索引，使三份新文档成为唯一现行入口，旧文档标明被替代或收缩后的职责。

- [ ] 搜索旧净积分清算、线下参数真源、项目覆盖全国业态规则等冲突性表述；逐条判断后修改，不允许批量替换。

- [ ] 验证本任务只修改列出的文档，且没有 HTML：

```bash
git diff --name-only HEAD~1..HEAD
git diff --check
git diff --name-only | rg "\.html$" && exit 1 || true
```

- [ ] 逐一暂存本任务列出的明确路径并提交：

```bash
git add docs/member-points-tier-and-clearing-prd-v1.0.md
git add docs/member-loyalty-and-stored-value-product-prd-v0.1.md
git add docs/member-points-p1-ops-parameter-tables-v0.1.md
git add docs/stored-value-membership-checkin-birthday-prd-v0.1.md
git add CONTEXT.md
git add docs/business-decision-log-v0.1.md
git add docs/prd-audit/business-decision-ledger.md
git add docs/00-current-effective-document-entry-v0.1.md
git add docs/product-documentation-index-v0.1.md
git add docs/system-capability-layers-product-index-v0.1.md
git add docs/documentation-map-v0.1.md
git commit -m "docs(PRD): 收口旧会员积分文档并更新权威入口"
```

### Task 7：完成三份权威文档的顺从性与对抗性复核

- [ ] 顺从性审计：将批准设计第 3 至 12 节的每条规则映射到三份新文档的具体标题，确认无遗漏、无相互矛盾、无未定义名词。

- [ ] 对抗性审计至少尝试以下反例：跨项目价格污染、项目自行改业态积分率、积分加现金被计入有效消费、优惠金额被计入有效消费、发券即结算、券过期错误退积分、确认后退款形成负余额、营销成本无承担方、特殊审批无预算、服务费两种模式同时生效、历史账单被回写。

- [ ] 把审计结果写入三份文档各自的验收章节；发现缺口时只修正对应规则，不扩展新业务能力。

- [ ] 执行全阶段检查：

```bash
git diff --check origin/main...HEAD
git diff --name-only origin/main...HEAD | rg "\.(html|js|ts|tsx|jsx|py|sql|yaml|yml|json)$" && exit 1 || true
rg -n "净积分清算|线下.*真源|发券.*结算" docs/member-and-tier-product-prd-v1.0.md docs/points-and-voucher-product-prd-v1.0.md docs/merchant-points-settlement-rules-v1.0.md
```

- [ ] 若复核产生修正，提交：

```bash
git commit -m "docs(PRD): 完成会员积分结算双重审计"
```

### Task 8：合并并推送 Phase 2

- [ ] 重复 Phase 1 的远程前进检查、阶段分支同步、全阶段复核、合并、推送和 `main == origin/main` 验证。

- [ ] 合并提交信息：`merge: 会员积分重构阶段二领域权威文档`。

---

## Phase 3：审计全部相关 PRD 并精准同步

### Task 9：建立逐文件正式审计报告

**Files:**

- Create: `docs/prd-audit/member-points-related-doc-audit-2026-07-28.md`
- Read: every `MAIN,prd` row in `docs/prd-audit/document-scope-manifest.csv`
- Read: `docs/prd-audit/member-points-rebuild-scope-2026-07-28.md`

- [ ] 从最新 `origin/main` 创建 `codex/member-points-phase3-related-docs` 和 `/home/james/wt-member-points-phase3`。

- [ ] 审计报告必须为每一份现行 PRD 保留一行，即使结论是不适用；不允许只列出发现问题的文件。

- [ ] 正式审计状态只允许：`FULL`、`PARTIAL`、`CONFLICT`、`MISSING`、`AMBIGUOUS`、`STALE`、`NOT_APPLICABLE`。

- [ ] 处置动作只允许：`NO_CHANGE`、`SYNC_REQUIRED`、`SUPERSEDED`、`BLOCKED_BY_USER`。

- [ ] 每行记录：文件、相关条款、当前表述摘要、顺从性结论、对抗性反例、权威依据、状态、动作、修改证据或不修改原因。

- [ ] 对 `/home/james/codex-backups/` 和 `main` 的同名文件进行一次性快速差异检查；只把差异作为逐条裁决证据，不设统一覆盖优先级。

- [ ] 关键词扫描只能用于发现候选，不得把命中或未命中直接等同于缺陷。

- [ ] 验证审计报告中的现行 PRD 行数与范围清单中的 `MAIN,prd` 行数一致，并人工复核路径去重。

- [ ] 提交：

```bash
git add docs/prd-audit/member-points-related-doc-audit-2026-07-28.md
git commit -m "docs(PRD): 完成会员积分相关文档逐文件审计"
```

### Task 10：精准同步被判定为需要修改的相关 PRD

**Files:**

- Modify: only files marked `SYNC_REQUIRED` in `docs/prd-audit/member-points-related-doc-audit-2026-07-28.md`
- Modify: `docs/prd-audit/document-scope-manifest.csv`

- [ ] 对每个 `SYNC_REQUIRED` 文件先写清要替换的原句、权威依据和目标句，再只修改相关段落；保留相邻业务规则、格式和文件职责。

- [ ] 对 `SUPERSEDED` 文件只维护替代入口和历史地位，不复制新规则。

- [ ] 对 `BLOCKED_BY_USER` 不做实体修改，在报告中留下冲突、选项与所需裁决。

- [ ] 更新范围清单中的角色、状态或备注，使之与实际文档职责一致。

- [ ] 逐文件运行冲突词扫描和人工复核；确认所有公式只在对应权威文档定义，其他 PRD 只引用或陈述业务效果。

- [ ] 验证变更清单严格等于审计报告授权的路径集合，加上审计报告和范围清单；若出现额外路径立即停止并恢复该额外修改。

- [ ] 按报告中列出的明确路径逐一暂存，不使用通配符或动态文件集合，然后提交：

```bash
git commit -m "docs(PRD): 同步会员积分相关现行文档"
```

### Task 11：更新覆盖矩阵与文档导航

**Files:**

- Modify: `docs/prd-audit/html-prd-coverage.csv`
- Modify: `docs/00-current-effective-document-entry-v0.1.md`
- Modify: `docs/product-documentation-index-v0.1.md`
- Modify: `docs/system-capability-layers-product-index-v0.1.md`
- Modify: `docs/documentation-map-v0.1.md`

- [ ] 把三份新权威文档及其与三个业务 HTML 的覆盖关系写入矩阵；此处只登记关系，不修改 HTML。

- [ ] 复核所有入口只把现行读者导向三份权威文档，不再把旧混合 PRD 标成当前规则源。

- [ ] 运行链接路径存在性检查、CSV 列数检查、重复入口检查和 `git diff --check`。

- [ ] 提交：

```bash
git add docs/prd-audit/html-prd-coverage.csv docs/00-current-effective-document-entry-v0.1.md docs/product-documentation-index-v0.1.md docs/system-capability-layers-product-index-v0.1.md docs/documentation-map-v0.1.md
git commit -m "docs(PRD): 更新会员积分文档覆盖与导航"
```

### Task 12：合并并推送 Phase 3

- [ ] 重复远程前进检查和范围核验，确认阶段差异没有 HTML、代码、OpenAPI、数据库、测试、开发设计或运行时文件。

- [ ] 合并提交信息：`merge: 会员积分重构阶段三相关文档审计`。

- [ ] 推送后验证 `main == origin/main`，并在审计报告记录最终合并提交。

---

## Phase 4：生成 HTML 逐条替代清单并停在批准门

### Task 13：只读审计三个业务 HTML

**Files:**

- Create: `docs/prd-audit/member-points-html-replacement-list-2026-07-28.md`
- Read only: `docs/business-system-map-for-operations-v0.1.html`
- Read only: `docs/platform-governance-map-for-operations-v0.1.html`
- Read only: `docs/business-enum-quick-reference-v0.1.html`

- [ ] 从最新 `origin/main` 创建 `codex/member-points-phase4-html-list` 和 `/home/james/wt-member-points-phase4`。

- [ ] 对三个 HTML 中会员、等级、成长值、积分、冻结、退款、积分商城、虚拟券、价格、结算、服务费、营销成本、税务等所有命中逐项人工判读。

- [ ] 每一条候选建立稳定编号，字段固定为：编号、HTML 文件、标题或锚点、当前表述摘要、问题、替代权威、动作、拟替代文本、影响、用户状态。

- [ ] 动作只允许：`REPLACE`、`DELETE_DUPLICATE`、`CHANGE_TO_REFERENCE`、`KEEP_UNCHANGED`。

- [ ] 用户状态初始统一为 `PENDING_APPROVAL`；即使建议保持不变，也必须纳入清单供用户确认。

- [ ] 拟替代文本必须足够具体，可在批准后直接实施；禁止写成概括性意图。

- [ ] 对每个 HTML 文件记录审计前后 SHA-256，并与 Phase 1 基线值比较，证明文件未被修改。

- [ ] 验证 Git 差异只包含替代清单：

```bash
git diff --check
git diff --name-only
git diff --name-only | rg "\.html$" && exit 1 || true
```

- [ ] 提交：

```bash
git add docs/prd-audit/member-points-html-replacement-list-2026-07-28.md
git commit -m "docs(PRD): 提交会员积分HTML逐条替代清单"
```

### Task 14：合并并推送 Phase 4，然后停止

- [ ] 合并提交信息：`merge: 会员积分重构阶段四HTML替代清单`。

- [ ] 推送后验证 `main == origin/main`、三个 HTML SHA-256 与基线一致、阶段差异不含 HTML。

- [ ] 向用户提交清单路径、编号范围、每条动作与影响，请用户逐项或按明确编号批量批准。

- [ ] 在收到批准前停止，不修改任何 HTML，不把“书面规格批准”解释为 HTML 改写批准。

- [ ] 用户批准具体编号后，另建后续计划，只实施获批编号的 HTML 精准改写、对应验证和最终收口。

---

## Final Verification Checklist

- [ ] 三份权威文档完整覆盖批准设计第 3 至 12 节。
- [ ] 积分产生公式、有效消费口径、取整、版本锁定和部分退款重算均有明确验收例。
- [ ] 项目级零售参考价与项目级内部结算价没有被误写为全国统一价格。
- [ ] 发券只扣积分并预占库存，核销才产生商户补偿。
- [ ] 纯积分与积分加现金的双下限、现金上限和实际补偿公式均一致。
- [ ] 特殊审批、营销成本分摊、服务费、账期、冲正和中国税法要求均可追溯。
- [ ] 所有现行 PRD 均有正式审计结论；不适用文件也有理由。
- [ ] 旧混合文档不再与三份权威文档竞争最终解释权。
- [ ] 运行参数均被定义为系统管理，离线表格不再是运行时真源。
- [ ] 三个业务 HTML 在用户批准逐条替代清单前保持字节级不变。
- [ ] 每阶段均已合并推送，远程 `main` 与 `origin/main` 一致。
- [ ] 既有四项未跟踪审计资料未被删除、移动或提交。
