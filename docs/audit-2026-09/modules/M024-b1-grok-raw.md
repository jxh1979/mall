CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B1 席乙（Grok）修复前全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B1（修复前全量；本模块首次开工，历史未审，PENDING→IN_PROGRESS 由调度已写 ledger；本席不改 ledger/findings/计划）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M024 商户档案、商户类型与商户侧门户／文档与接口定义／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock V0.11；提示词 `160-audit-prompt.md` V0.17（本机 `.scratch/module-recheck-20260908/160-audit-prompt.md`）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`0c04a5e2ef9c1e1878291b0553e60ea1d88c5cd5`（主工作区 ahead 49 / behind 26；含未推本地提交与大量未提交改动。审计对象=主工作区现状文件，含工作区已改前端/后端，不含 origin 独有提交）
- 本席未读取 `modules/M024-b1-self-raw.md`，未依赖席甲发现；未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席
- 本席步骤结论：**FAIL**

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/plugins/cache/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/skills/`。按提示词 §6 收窄口径，禁止项未调用。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读全文；用户指令优先于技能 |
| using-arc | Arc plugin `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理/禁改业务**，不跑 Arc 默认派评审 |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep` / `graft ask --source` 定位路由、门户、通知开关、LocalZone、PII 权限 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、`.../grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 本文是浅模块：把 HTML、路由、后端、验收矩阵写进同一张「当前实现」表，接口（读者须记住的事实）几乎等于全文 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 文档声称 vs 现行源码的反馈环=只读命令；不改代码、不加探针 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借数据流追踪（路由→页→API→scope→迁移）；作废 instrumentation / 最小改码 |
| tdd / test-driven-development | 存在 | 只作测试有效性透镜；本模块无测试文件，B1 不写测试 |
| verification-before-completion | 存在 | PASS/FAIL 前实跑 sha256sum / sed / 读源码；不凭印象 |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：过时「平台全量」、错误 HTML 行号、把写权限塞进路由权限格 |
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

graft 本轮（grep ADM-MER + ask 门户分页/scope + grep NotificationSwitchResolver + grep LocalZone + grep pii.contact_phone.view）约节省 ~306k tokens。

---

## 1. 范围、哈希、行数（实算，不抄期待值）

独立命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
sha256sum docs/merchant-portal-zone-prd-v0.1.md
# 3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f
wc -l -c docs/merchant-portal-zone-prd-v0.1.md
# 164 17116
sed -n '6403p' docs/audit-2026-09/plan/criteria-lock.md
# M024 → [BSM-114, GEN-001 … GEN-018]
git rev-parse HEAD origin/main
# e456ba96c84185c8d39f18f548011daac48a6466
# 0c04a5e2ef9c1e1878291b0553e60ea1d88c5cd5
```

sidecar 与本席实算锁哈希相同。LOCK-AMEND-COUNT 只读见到 1（未改 ledger）。计划写有效内容 164；现行物理行 **164**（以实算为准）。`git diff --stat HEAD -- docs/merchant-portal-zone-prd-v0.1.md` 空：该文档相对 HEAD 未改；最后触及提交 `44d59ddf docs: 收口 M05 商户合同空间文档出处对齐`。

| 文件 | 物理行 | SHA256 |
|---|---:|---|
| `docs/merchant-portal-zone-prd-v0.1.md` | 164 | `3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f` |

全文覆盖：164 行每一行均已读，不抽样。模块内无测试文件。只读追证的代码/HTML/迁移/矩阵**不在本模块 FILES 列表**，仅作 GEN-016 复现对象。

---

## 2. 全文覆盖表（文档每一段）

| 行 | 段 | 本席动作 |
|---:|---|---|
| 1–4 | 标题/版本 V0.13 / 2026-08-31 / 现行 PRD | 通读；业务最终解释权声明与提示词 §2 一致 |
| 6–19 | §1 定位与终端 | 对照 BSM-114 入口表、ENUM L233 终端十一端、PGM 商户侧；核 `/admin/merchant-zone/*` 与 `accountForms` |
| 21–29 | §2 账号、权限与数据边界 | 对照 PGM L509–510 / L654 / L681；核 scope 硬闸与暗纹实现 |
| 31–47 | §3.1 HTML 能力 vs 当前承载 | 对照 BSM L1315–L1322、PGM L760–772 三类商户表 |
| 48–56 | §3.2 自助七项 | 对照 BSM L1322 自助句、BSM L1764 主理人、PGM L531 证照 |
| 58–63 | §3.3 账单十一类 | 对照 ENUM L256；核电损/水损未落地 |
| 65–72 | §4 工单边界 | 对照 ENUM L264 / L260–262、BSM L1384、V022/V166；核专区页类型与第五态文案 |
| 74–82 | §5 证照与当地时间 | 对照 PGM L531；核 V028、LocalZone.ID、无商户侧证照页 |
| 84–100 | §6 异议 | 对照 BSM L1322 / L1566 / L1351、ENUM L252 / 营业额抽成模式、PGM L429 独立酒吧 |
| 102–111 | §7 导出与敏感页 | 对照 PGM L680–681；核六页无导出 |
| 113–128 | §8 当前一期实现 | 对照 adminRoutes、AdminApp、门户 Controller/Service、矩阵、PiiCipher、V339 |
| 130–137 | §9 当前未实现 | 与 §3–§7 差距声明交叉 |
| 139–157 | §10 验收检查 | 作为文档自检表，不当成已通过证据 |
| 159–164 | §11 依据 | 核所列 HTML 锚点与迁移文件存在 |

---

## 3. 转入既有事实（只核文档是否过时，不重开已定稿 ID）

| 转入事实 | 现行源码 | 本文是否过时 |
|---|---|---|
| M020-001：门户三方法无商户范围即拒绝 `MERCHANT_SCOPE_REQUIRED`，已删平台全量分支 | `MerchantPortalService.requireMerchantScope` L171–174；`AuthorizationService.currentMerchantScope` L425–431 无 merchantId 即 throw；源码注释「本前缀不提供全量口径」 | **过时**：L126 仍写「平台全量取真实 total」→ GR-01 |
| M020-003–006：异议合并、500 字、幂等、失败留痕、中文报错 | Service L34–35 / L148–153 / idempotency L121；`SETTLEMENT_NOT_DISPUTABLE` 中文；专区页 `REASON_MAX=500` | 本文未复述已删的覆盖/英文码细节，无新过时句 |
| M023-001–007：授权工作台/我的对账/我的合同按后端真字段；结算明细与留言入口；起草态真码 | 对账页 V0.01 有 `ITEM_LABELS`+异议表单；合同页 V0.02 `drafting:"草拟"`、租期/到期/空间/押金 | L53「ADM-MER-006 当前只读最小披露，关键条款与到期情况须完整展示」：到期日已展示、关键条款仍缺。半句偏旧，不单独立项（见观察 O-1） |
| origin/main `0c04a5e2`（PR #464） | `git rev-parse origin/main` 实算一致 | 审计对象仍是主工作区现状；PRD 相对 HEAD 未改 |

---

## 4. 对象只读追证（现行源码 / 迁移 / HTML sed，不以历史 PASS 或注释为事实）

### 4.1 adminRoutes.ts ADM-MER-003..008

`sed -n '89,94p' apps/web/src/admin/adminRoutes.ts`：

| pageId | 路由 | requiredPermissions | accountForms |
|---|---|---|---|
| ADM-MER-003 | `/admin/merchant-zone/bills` | `merchant.billing.view` | `["merchant"]` |
| ADM-MER-004 | `/admin/merchant-zone/work-orders` | `merchant.service_work_order.view` | `["merchant"]` |
| ADM-MER-005 | `/admin/merchant-zone/settlements` | `merchant.portal.settlement.view` | `["merchant"]` |
| ADM-MER-006 | `/admin/merchant-zone/contracts` | `contract.view` | `["merchant"]` |
| ADM-MER-007 | `/admin/merchant-zone/fee-recipients` | `merchant.portal.fee_recipient.manage` | `["merchant"]` |
| ADM-MER-008 | `/admin/merchant-zone/dividends` | `finance.dividend.view` | `["merchant"]` |

AdminApp：`ADM-MER-003..007` L247–256；`ADM-MER-008` L355–356 接到对应六页。六页文件均存在。

写权限不在路由门控：工单页 `WO_CREATE = ["merchant.service_work_order.create"]`；对账页 `DISPUTE_PERMS = ["merchant.portal.settlement.dispute"]`（V200 有该码）。→ GR-02。

### 4.2 MerchantPortalController/Service V0.01

两文件头均为 `文件版本：V0.01`。

- `GET /api/v1/merchant-portal/contracts`、`GET .../settlements`：`page`/`size` 默认 `"1"`/`"500"`。
- Service 两 list：`requirePermission` + `requireMerchantScope()` + `PageResponse.of(listByMerchant, page, size)`。`PageResponse.of` 注释：对已取全量做内存切片，`total` 取该列表真实大小。
- 无平台全量分支。无商户归属 throw `MERCHANT_SCOPE_REQUIRED`「此操作必须在商户范围内进行」。
- 第三方法 `POST .../settlements/{id}/dispute`：幂等键、合并留言、500 字、不改 status。
- 前端合同/结算页 `api.get(...)` 经 `unwrap` 只取 `data.items`（`adminClient.ts:42–49`），客户端 `usePagedRows(..., 10)`，**未**把后端 page/size 传出。API 默认 1/500 仍是后端事实。

### 4.3 费用接收人手机号

- `PiiCipher`：`Cipher.getInstance("AES/GCM/NoPadding")`，信封 `enc:v1:`。
- `JdbcMerchantFeeNotificationRecipientRepository` 写 `encrypt`、读 `decrypt`。
- `MerchantFeeNotificationRecipientController` L95–112：无 `pii.contact_phone.view` 返回掩码；注释写明短信链路走解密后明文。
- V339 加宽列 + 种 `pii.contact_phone.view`。与 L128「当前代码事实」一致。

### 4.4 NotificationSwitchResolver 与 V177

- 总闸 `sms_notification`：`isSwitchEnabled(MASTER_SWITCH, false)` → **缺行默认关闭**。
- 域/事件闸：缺行默认 `true`。
- V177 L8–9 出厂插入 `sms_notification` `enabled=FALSE`；并种 `sms.notify.merchant_fee` 及 `rent/utilities/property/renewal` 分项（均 TRUE，可被改 FALSE）。
- `NotificationService.STATUS_SKIPPED_SWITCH_OFF = "skipped_switch_off"`；集成测试断言总闸关闭时 status=`skipped_switch_off`。
- `MerchantFeeNotificationRecipientService.FEE_EVENTS = rent/utilities/property/renewal`。与 L54 一致；该条把与 HTML「金额变化不得关闭」的冲突登记为当前实现差距，**不是**把可关闭写成业务规则。

### 4.5 LocalZone / V022 / V028 / V166 / V197 / V199 / V200 / V339

| 声称 | 实核 |
|---|---|
| `LocalZone.ID` = `Asia/Shanghai` | `LocalZone.java:19` `ZoneId.of("Asia/Shanghai")`。类注释写死单地。L82 差距句成立 |
| V022 `merchant_service_work_orders` 四类型七态 | CHECK `repair/complaint/rectification/service`；`created/assigned/processing/pending_merchant/resolved/closed/cancelled` |
| V028 合规到期表+通用接口 | `compliance_expiry_items` / `contract_key_dates` 存在；平台页 `GET /api/v1/compliance/expiry-items`。无商户专区「我的证照」页 |
| V166 `facility_work_orders` | 表存在；类型含 `cleaning`；`source` 含 `merchant_self`。六页 **零** `/api/v1/facility` 调用。L51/L70 无入口成立 |
| V197 `tcs_term_merchant_portal` | 存在，TRUE |
| V199 商户专区模块开关 | `terminal_type=merchant_portal` `scope_id=merchant` TRUE |
| V200 门户权限 | `settlement.view/dispute`、`fee_recipient.manage` |
| V339 | 存在，费用接收人电话加宽+PII 权限 |

后端 `MerchantServiceService.escalateOverdueWorkOrders` 打 `sla_breached`+`escalated_at`。专区工单页 **零** `sla_breached`/`escalated` 展示（不否定后端升级事实）。

### 4.6 六页当前能力（工作区现状）

- 账单：账龄 KPI、费用行 `lineSummary`；`FEE_TYPE_LABELS` 无电损/水损，未伪造这两目。
- 工单：类型仅四码；`resolved: "已解决"`（L61）。无保洁、无 V166。
- 对账：结算明细中文名 + 异议留言；状态含 `voided: "已作废"`。
- 合同：租期=生效~到期、空间、状态（含草拟）、押金、续签自。无「关键条款」字段。
- 费用通知：事件四类 rent/utilities/property/renewal；展示 API 返回的手机号。
- 分红：只读方案/计算单/留存；无审批入口。无人工成本明细/员工工资。

六页无导出按钮（`export function` 命中除外）。`apps/web/src` 无「暗纹」实现；`user-select: none` 仅 `.admin-tech-details > summary`（L1702），不是敏感页规则。

### 4.7 UI 矩阵

`docs/full-admin-ui-acceptance-matrix-v0.1.md` L56–61 确有 UI-ADM-MER-003..008 六行。L124「不得再写成矩阵缺失」成立。

矩阵 UI-ADM-MER-007 备注写该页明文显示手机号；**矩阵不在本模块 FILES**。PRD L128 写的是后端掩码，与 Controller 一致。不把矩阵正文当成本文档缺陷，作观察 O-2。

### 4.8 HTML 行号 sed（文中仅两处点名行号）

**BSM `sed -n '1311,1322p'`：**

- L1311：`<div class="detail-grid">`（不是资料/自助）
- L1315：基础资料
- L1316：订单履约
- L1317：对账差异
- L1318：可结算结果
- L1319：异常
- L1320：授权酒吧/游乐/供应链
- L1321：HTML 注释「商户专区自助五项」
- L1322：投诉异议通道 + 费用总表/综合账单 + 自助五项

PRD L133 写「HTML L1311-L1318 的资料、订单履约、差异、异常、授权业务功能和自助事项」——异常/授权/自助不在 L1311–L1318。→ GR-03。

**BSM `sed -n '1562,1567p'`：**

- L1562：`<div class="info-block">`
- L1563：`<h3>重要事项短信通知（2026-06-29 新增）</h3>`
- L1566：给商户长句，含「本条所定的异议期适用于非积分类的商户账单…出厂默认 10 个工作日」

PRD L95「BSM L1562 所定的异议期」——L1562 无异议期规则，规则在 L1566。→ GR-04。

criteria-lock BSM-114 源行 `:1827` 与 `sed` 该行 `<h2>各类入口…</h2>` 一致（锁自身，非本 PRD 引用）。

---

## 5. 三线记录

### 5.1 第一性原理三问

**Q1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

答得上。BSM-114 入口表把「商户侧页面」「商户 App」与管理后台/Web 前端拆开；BSM「商户管理」L1309–L1322 要求完整使用+自助五项；PGM「商户侧：在授权业务范围内全开放」L749–772 按业态列开放面；ENUM「终端十一端」L233 点名商户网页门户与商户手机应用。本文是把这些 HTML 条映射到一期六页与差距清单的产品口径。删除后，实现者会把 `/admin/merchant-zone/*` 误当成平台后台，或把未落地能力当成永久禁令（本文 L15、L111、L130 正是为防这两件事）。有业务上位，不走删除。

**Q2 声称的行为有没有被实际执行验证？**

文档本身不可执行（runbook：文档用 HTML 对照 + 命令/结构，不强制跑测）。本席用类型 1+4：HTML `sed`、`sha256sum`、读 adminRoutes/Controller/Service/PiiCipher/Resolver/LocalZone/六页/矩阵/迁移头。多数「当前实现/差距」可复现；不可复现的见 findings。

**Q3 哪个前提未经验证就当真？**

1. L126「平台全量取真实 total」把 M020-001 之前的双口径当成现行事实（已证伪）。
2. L117–119 权限列把路由门控与写权限用 `/` 写在同一格，前提是「这就是 adminRoutes 的 requiredPermissions」（已证伪）。
3. L133/L95 把 HTML 行号当成稳定锚（sed 已证伪）。
4. L29 把 PGM 暗纹/敏感页写成未标差距的现行边界，前提是壳层已做（源码无暗纹、无敏感页禁选中）。

### 5.2 顺从式（19 项逐条）

先假定本文意图正确（记录 HTML + 诚实标差距），再核映射判据。

| ID | 适用？ | 证据 |
|---|---|---|
| BSM-114 | **适用** | 锁原文=各类入口表。本文 §1 把商户网页门户/手机应用从十一端里抽出，不把管理后台、客人端写成商户终端。§3.1 酒吧/游乐/餐饮零售开放面与 PGM L760–772 同句。未把「当前六页」写成 HTML 能力上限（L15、L111）。**未缩小** BSM-114「供应/库存、员工、看板、不看平台全局」。缺陷是实现声称与行号，不是把入口表改写成别的业务 |
| GEN-001 | 适用 | 文档有可验证用途（见 5.1 Q1）。无重复转发段。不为极端场景加终端 |
| GEN-002 | 适用（声称可执行入口） | 路由/API/默认分页/权限码是可执行声称。部分与现行入口不一致（GR-01、GR-02） |
| GEN-003 | **不适用（范围内无测试）** | FILES 仅 1 个 md。不把矩阵 e2e 断言或六页 `*.test.tsx` 算本模块测试——那些文件不在 M024 |
| GEN-004 | 弱适用 | 文档不处理输入。L128 掩码/信封是在描述后端校验边界，与 Controller 一致 |
| GEN-005 | 适用 | `accountForms: ["merchant"]` 与六路由一致。L24 scope 过滤：门户三方法 fail-closed 成立；账单 `listBills` 源码仍留 `findAllBills` 分支，但现行 `currentMerchantScope()` 无商户号即 throw，该分支不可达。权限列把 create/dispute 写成路由权限 → GR-02（最小权限门控被写宽） |
| GEN-006 | 弱适用 | 异议「只留言不改状态、付款不暂停」与 Service 一致。文档无自创状态机 |
| GEN-007 | 弱适用 | 本文未写幂等键/500 字（那些在 M020 代码）。不把未写当成缺陷 |
| GEN-008 | 适用 | 十一类=ENUM L256 逐字。工单七态中文=ENUM L264；代码值七码与 V022 CHECK 一致；第五态页面「已解决」已标未对齐。退租状态中文与 ENUM L252 一致，专区页另有 `voided`（代码事实，ENUM 无「已作废」；页有中文「已作废」，本文未把 voided 写成 HTML 态）。权限码斜杠与路由表不一致 → GR-02 |
| GEN-009 | 弱适用 | 跨商户拒绝留痕：门户失败路径 `recordFailure`。文档未写英文 `reason is required`（Service L134 仍有，非本文声称） |
| GEN-010 | 适用 | AES-GCM + 掩码与 PiiCipher/Controller/V339 一致，未扩成新业务规则。未泄漏密钥 |
| GEN-011 | 弱适用 | L29/L107 留痕规则来自 PGM L681。六页有 `AuditTrailHint`，无「打开敏感页」专用留痕 |
| GEN-012 | 弱适用 | 默认 size=500 是后端默认；文档未把前端 10 条/页写成 API 默认 |
| GEN-013 | 不适用 | 无构建/CI 声称 |
| GEN-014 | 适用（迁移编号声称） | V022/V028/V166/V177/V197/V199/V200/V339 文件均在。§11 依据列表未写 V166/V177，但正文已引用且文件存在，不单独立项 |
| GEN-015 | 弱适用 | 中文文案。工单第五态中文偏差已标差距。无本模块 UI |
| GEN-016 | **核心适用 FAIL** | 路径/路由/accountForms/API/分页默认/字段/枚举/状态/迁移/矩阵六行/多数差距声明可复现。不可复现：平台全量、路由权限斜杠、两处 HTML 行号、暗纹未标差距。不以历史 PASS、文档互引、注释当事实 |
| GEN-017 | 适用 | 本文主体=HTML 下位实现记录+差距，**细化**。未把一期六页发明成 HTML 没有的商户终端。L96「其他账单不得自行套用十个工作日默认」是对 L1566 适用范围的收窄提醒（ENUM 营业额抽成条自有异议期、无十工作日默认），不是新账单种类。暗纹未标差距是完成声明问题（GEN-016），不是把 HTML 沉默当删除 |
| GEN-018 | 适用（本席） | 本 raw 结论落到 HTML sed、源码、迁移、命令输出。不引用席甲、不引用历史 PASS |

### 5.3 对抗式（grilling 结构，自问自答；不等操作员、不派子代理）

❓ **Q1 本文有没有把「当前六页」写成 HTML 商户侧的全部能力，从而缩小 BSM-114 / PGM 全开放？**

➡️ 没有。L15、L43–45、L111、L130–136 明确一期缺口；酒吧/游乐/餐饮零售/供应链仍按 HTML 列能力。不立项。

❓ **Q2 「平台全量取真实 total」会不会被照着做成商户门户前缀下的跨商户列表，直接打穿「不看平台全局」？**

➡️ 会。第一次按 L126 正常实现就会去找平台全量分支；现行三方法已删除该分支并无商户范围即 403。这不是极端并发。→ GR-01。

❓ **Q3 把 view/create、view/dispute 写进「路由」旁的权限格，第一次抄进 adminRoutes.requiredPermissions 会怎样？**

➡️ 只有 view 的商户账号将打不开「我的工单/我的对账」列表（含已有工单/结算单）。写权限在页内 `PermissionButton`，路由门控只有 view。→ GR-02。

❓ **Q4 按 L133 去 HTML L1311–L1318 找「自助事项统一入口」，会落到什么？**

➡️ 落到 `detail-grid`/「主要功能」开头到「可结算结果」。自助长句在 L1322，异常/授权在 L1319–L1320。行号不能复现所指内容。→ GR-03。

❓ **Q5 「BSM L1562 所定的异议期」sed 对不对？会不会把「给客人」那条当成异议期正文？**

➡️ L1562 是 info-block 开标签；L1565 是给客人；异议期在 L1566 给商户条。第一次按行号引用会贴错段。→ GR-04。

❓ **Q6 导出已标差距，暗纹写在 §2 却未进 §9。QA 按 §2 当现行能力，会不会以为敏感页已禁选中/已有暗纹？**

➡️ 会。六页与 admin 壳无暗纹、无敏感页 `user-select` 禁则、无打开敏感页留痕。与同属 PGM L681 的导出（已标差距）待遇不一致。→ GR-05。

❓ **Q7 电损/水损「页面不得伪造」——账单页有没有用「其他/手工」冒充十一类？**

➡️ `FEE_TYPE_LABELS` 有 cam/other/manual/contract，**没有**电损/水损键。本文未声称页面已列十一类名目，只要求不伪造未落地两目。不立项。

❓ **Q8 V166 迁移头写「V022 死表、由本表统一替代」。本文坚持分表，是不是发明？**

➡️ 不是。ENUM 分列「物业工单状态」与「商户服务工单状态」；BSM L1384 物业自助 vs L1322 商户服务自助（含整改）。本文 L67–71 与 HTML 一致。V166 头注释与 HTML 冲突，注释不是依据（2.3 禁令）。观察 O-3。

❓ **Q9 金额变化通知「不得关闭」vs 代码可关：本文是把可关闭写成规则，还是标冲突？**

➡️ L54 写「与 HTML 不得关闭的要求冲突」，§10 费用通知行要求该冲突已登记。这是诚实差距，不是发明可关闭规则。不立项。

❓ **Q10 有没有为「商户>500 份合同、unwrap 丢掉第 2 页」补需求？**

➡️ 前端不传 page/size、unwrap 只取 items，超过 500 会丢。要两件不巧（默认 500 + 前端丢分页）才爆。提示词：不为极端场景补需求。观察 O-4，不立项。

---

## 6. Findings（临时 GR 编号；正式 ID 待 B2）

分类顺序已走：冲突 → 细化 → 技术性 → 添加。下列均能指到 HTML 上位或「当前实现」记录职责，故为**细化**；工程缺陷成立。无添加项，B1 不发问。

### GR-01 门户 API「平台全量取真实 total」已不是现行事实

- **文件:行**：`docs/merchant-portal-zone-prd-v0.1.md:126`
- **原文**：`GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-portal/settlements` 使用真分页，默认 `page=1/size=500`，**平台全量取真实 total**；商户 scope 只见本商户。
- **更长上下文**：§8 当前一期实现，紧接矩阵六行与手机号 AES-GCM 事实。读者会把该句当现行双口径（平台全量 + 商户过滤）。
- **正常触发/影响**：第一次按该句实现或验收，会去找平台全量列表并断言跨商户 `total`。现行 `MerchantPortalService.listMyContracts/listMySettlements` 无全量分支；`requireMerchantScope()` 无归属即 403 `MERCHANT_SCOPE_REQUIRED`。转入事实 M020-001。过时当前事实 = GEN-016。
- **最小拟处置**：删除「平台全量取真实 total」；改为：默认 `page=1/size=500`，`total` 为本商户列表真实长度；无商户范围拒绝，不提供平台全量口径。
- **四分类**：细化（记录现行 API；上位 BSM-114「不看平台全局」+ 本文自身 §2）。不是把 HTML 改写成允许跨商户。
- **工程缺陷**：是
- **冻结 ID**：GEN-016；BSM-114（入口「不看平台全局」）；GEN-002
- **第一性依据**：类型 4——`MerchantPortalService.java:58–110,171–174`；`MerchantPortalController.java:32–44`；`AuthorizationService.java:425–431`。类型 1——BSM-114 商户侧页面「不看平台全局」

### GR-02 §8 权限列把写权限写进路由门控

- **文件:行**：`docs/merchant-portal-zone-prd-v0.1.md:117–119`（表 ADM-MER-004、ADM-MER-005）
- **原文**：ADM-MER-004 权限 `merchant.service_work_order.view/create`；ADM-MER-005 权限 `merchant.portal.settlement.view/dispute`。同表 003/006/007/008 与 adminRoutes 单码一致。
- **更长上下文**：列名「页面 / 路由 / 权限 / 当前能力」，权限紧贴路由，形态同 `adminRoutes.ts`。004 当前能力「读与发起」、005「对账与异议留言」是页内按钮，不是 `requiredPermissions`。
- **正常触发/影响**：第一次把该格抄进 `requiredPermissions`，仅有 view 的商户账号无法打开工单列表/结算列表。现行路由只有 view；create/dispute 在页内 `PermissionButton`（工单 `WO_CREATE`，对账 `DISPUTE_PERMS`）。
- **最小拟处置**：权限列改为与 `adminRoutes.ts` 逐字相同；发起/异议改为「页内另需 …」或写入「当前能力」列。
- **四分类**：细化（描述一期页面门控，上位仍是商户专区自助）
- **工程缺陷**：是
- **冻结 ID**：GEN-016；GEN-005；GEN-008
- **第一性依据**：类型 4——`adminRoutes.ts:89–94`；`MerchantZoneWorkOrdersPage.tsx:13`；`MerchantZoneSettlementsPage.tsx:15`

### GR-03 「HTML L1311-L1318」与 sed 所指内容不符

- **文件:行**：`docs/merchant-portal-zone-prd-v0.1.md:133`
- **原文**：HTML L1311-L1318 的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。
- **更长上下文**：§9 当前未实现。点名行号是可复现声称（GEN-016）。
- **正常触发/影响**：`sed -n '1311,1318p' docs/business-system-map-for-operations-v0.1.html` 实际为 `detail-grid` → 「主要功能」→ 资料/订单/差异/**可结算**。异常=L1319，授权=L1320，自助长句=L1322。按该范围验收会漏自助五项与费用总表句。
- **最小拟处置**：改为 L1315–L1322，或删行号改锚「商户管理·主要功能」。
- **四分类**：细化（引用 BSM 商户管理段）
- **工程缺陷**：是
- **冻结 ID**：GEN-016；BSM-114（入口能力面在该段展开）
- **第一性依据**：类型 1+4——上述 sed 输出（本 raw §4.8）

### GR-04 「BSM L1562 所定的异议期」行号错位

- **文件:行**：`docs/merchant-portal-zone-prd-v0.1.md:95`
- **原文**：BSM L1562 所定的异议期只适用于房租、水电、物业、结算金额出账与调整、押金、违约金、分红七类非积分商户账单…
- **更长上下文**：§6.2。七类名单与 L1566「本条所定的异议期适用于非积分类的商户账单（…）」一致，**行号**指向包装 div。
- **正常触发/影响**：`sed -n '1562p'` 为 `<div class="info-block">`；异议期正文在 L1566。L1565 是给客人短信。按 L1562 引用会贴错段。
- **最小拟处置**：改为 BSM L1566，或「重要事项短信通知·给商户」。
- **四分类**：细化（转述 HTML 异议期，未改七类名单）
- **工程缺陷**：是
- **冻结 ID**：GEN-016
- **第一性依据**：类型 1+4——`sed -n '1562,1567p'`（本 raw §4.8）

### GR-05 暗纹/敏感页写成现行边界，未标实现差距

- **文件:行**：`docs/merchant-portal-zone-prd-v0.1.md:29`（§9 L130–136 未列此项）
- **原文**：后台、商户专区和员工手机端的在线页面都带可反向推算账号、设备和打开时间的暗纹。敏感页文字不可选中、不可复制，不提供另存和打印；打开敏感页本身须留痕。
- **更长上下文**：§2 混排「当前商户网页专区…」（L23）与 HTML 规则。文首 L4：「当前页面、接口或终端不足时，标为实现差距」。同属 PGM L681 的导出在 §7 L111 已标「六页没有完整导出」。暗纹未标、也未进 §9。
- **正常触发/影响**：按 §2 当现行能力做验收，会认为专区已有暗纹与敏感页禁选中。`rg 暗纹 apps/web/src` 无实现；`user-select: none` 仅技术折叠 summary。第一次正常验收即失败。
- **最小拟处置**：L29 改为 HTML 要求并标「当前专区未实现」，或把暗纹/敏感页追加进 §9。
- **四分类**：细化（转述 PGM 导出与暗纹；缺陷是完成/差距声明，不是发明暗纹规则）
- **工程缺陷**：是
- **冻结 ID**：GEN-016；GEN-017（未把 HTML 要求标成已完成，也不得因未实现而删 HTML）
- **第一性依据**：类型 1——PGM L681 暗纹/敏感页原文。类型 4——`rg` 六页无暗纹；`styles.css:1702` 仅 summary

---

## 7. 观察（不立项）

- **O-1** L53 ADM-MER-006「只读最小披露，关键条款与到期情况须完整展示」：工作区合同页已展示租期/到期日/空间/状态/押金/草拟码；关键条款仍无字段。半句偏旧，整句仍有真实缺口，不单独立项。
- **O-2** 矩阵 UI-ADM-MER-007 备注「明文显示手机号」与 PRD L128/Controller 掩码不一致。矩阵不在本模块；PRD 后端声称可复现。
- **O-3** V166 文件头称 V022 为死表并由设施表替代。PRD 按 ENUM/BSM 坚持分表，正确；注释不是依据。
- **O-4** 合同/结算前端 `api.get`+unwrap 丢分页元数据、默认后端 500。超 500 才丢行，不立项。
- **O-5** 专区工单页不展示 `sla_breached`；后端确有升级。本文写的是后端「超时自动升级并标记」，不假。
- **O-6** V199 种子 reason 只点名账单/工单/对账/合同/费用通知，未写分红。PRD 未引用该 reason 字符串。
- **O-7** `MerchantBillingService.listBills` 源码仍有 `findAllBills`，现行 `currentMerchantScope()` 使其不可达。非本模块文件；PRD 未写账单 API 平台全量。
- **O-8** L72 设施时限「须对商户公开」是 HTML 原句（BSM L1384）；L70 已标无 V166 入口。不另标完成声明缺陷。

ponytail-review：`L126: delete: 「平台全量取真实 total」。无平台全量口径。` `L117-119: shrink: 权限格只留 requiredPermissions。` `L133/L95: delete: 错误行号，改锚点。` `L29: shrink: 标差距，勿写成已落地。` net: 删过时子句与错行号，不删 HTML 转述。

---

## 8. 本席结论

- 模块范围 1 文件 164 行全文覆盖。
- 映射 19 项均已核对；核心 GEN-016 **FAIL**（4 条不可复现的当前事实/行号 + 1 条未标差距的完成声明）。BSM-114 业务转述未缩小入口表与全开放面。
- 三线均执行。添加项 0。无 HTML 自身缺陷需铁律 4 暂停。
- 临时 findings：GR-01 … GR-05，均细化、工程缺陷=是。不重开 M020/M023 已定稿 ID。
- **步骤结论：FAIL**。无有条件通过。

（完）

## M024 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **33051** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M024 FILES 1 / 锁映射 19：`BSM-114, GEN-001 … GEN-018`）
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `0c04a5e2ef9c1e1878291b0553e60ea1d88c5cd5`（未 fetch）。B3=0。无源修复。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，**不读**席甲 raw 在封存 24182 字节之后的 V0.03 B2 结论段。
- 本席 B2-r1：**PASS**（001–005 逐 ID 全 PASS；添加 0、不发 2.2、无 HTML 申请、无 questions。非阻断执行注见 002 节标题条目 body 仅四字、003「主要功能」14/15 口径差，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值，LOCK-AMEND-COUNT 1。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/前缀字节/候选段/两份 B1 封存前缀/注册表/门禁源码/BSM·PGM 原文/adminRoutes/两页 PermissionButton/`MerchantPortalService.requireMerchantScope`/`PageResponse.of`/暗纹零命中；未对任一 `complex_test*` 库做写操作，未重审模块结论、未改 PRD。

此票只判断 findings.md V2.77 顶部「当前 M024 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占 CRITERIA-LOCK-SHA256 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M024-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 33051 | `d49a241c601340000ba7056bf68d9058131b97f3f3e321161387888f6be9b223` |
| `docs/audit-2026-09/modules/M024-b1-self-raw.md` 前缀（席甲 B1 V0.01+V0.02 更正段；本席只读此前缀） | V0.01+V0.02 | 24182 | `6b90a10c44a9b5f1e846c19e0bc54e772088aea4ddf627694fc936f72f41e6d1` |
| `docs/audit-2026-09/findings.md` | V2.77 | 1063979 | `0e791fa4f4e2919b64f25760b8d0605248bfcb4fe0f48d3fc4a425fc34263e6d` |

席甲 raw 全文现大于 24182 bytes（本席用前 24182 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。V0.02 更正段只改覆盖表行数笔误（0→164），bytes/SHA/结论不变——与封存说明一致。

controller / runbook / modules.md 只读核验（本席未改；与 B1 同）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

对象文档（未改）：`docs/merchant-portal-zone-prd-v0.1.md` 164 行，SHA `3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f`，文首 V0.13 / 最后更新 2026-08-31。

`questions/M024*`：不存在。

门禁检查② 规则（`tools/verify-entry-anchors.mjs` 头注 + `REF` 实现，本席实读）：全库 Markdown 的 `文件代号「名」` 必须在该 HTML 内唯一定位——要么等于注册表 `entry_name`，要么（含省略号分段）恰为一条条目名或 `flatEntry` 正文的子串；命中 0 或 >1 即红。检查③ 只认「实测 LNNN」句式，故正文「BSM L1562」「HTML L1311-L1318」不红。检查② 的正则只吃紧邻代号的第一个「名」，同一行后续无代号前缀的「名」漏检。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：002 三名唯一性、003 替代名承载、004 权限列边界、005 暗纹零命中、版本行是否入最小处置 |
| grilling | 只借追问结构自查：差集有无漏并/改写、002 是否偷换行号、003 替代名是否错条、004 是否改权限模型、005 是否删 HTML/改 §10、版本行是否发明 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/注册表定位脚本/BSM L1562·L1311-L1322 sed/`adminRoutes`/`PageResponse.of`/暗纹 rg |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审模块结论 |
| codebase-design / ponytail-review | 最小是改一句过时实现、换条目名、权限格收窄、标差距；不发明暗纹实现、不改 HTML |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本节点对象是文档锚点/注册表/HTML，graft 不索引；改用注册表脚本 + sed/rg。未管道 head/tail 截 graft |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…05、观察 O-1…O-8、席甲 CX-M024-001…003、OBS-1…5）

漏并（findings）：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–005 或只登记段。无单席独有缺陷被丢。无本批新开「否」。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M024-001 | GR-01 + CX-M024-001 | 细化/是/是（L126 改写为现行 fail-closed + 本商户切片 total；不加新规则） | **PASS（细化/是/是）** |
| M024-002 | GR-03 + GR-04 + CX-M024-002 | 细化/是/是（只换条目名，不换新行号） | **PASS（细化/是/是）** |
| M024-003 | CX-M024-003（本席 B1 未报） | 细化/是/是（§11 每个名字写成 `代号「名」` 并换成可唯一名字） | **PASS（细化/是/是；席甲单方发现，独立核源成立）** |
| M024-004 | GR-02（席甲 B1 判通过，B2 采本席） | 细化/是/是（权限列只留路由门控码，页内权限进当前能力列；不改权限模型） | **PASS（细化/是/是）** |
| M024-005 | GR-05（席甲 B1 漏） | 细化/是/是（L29 保留 HTML 转述并指向 §9，§9 增一条；不删 HTML、不改 §10） | **PASS（细化/是/是）** |

冲突入队 0。细化入队 5（001–005）。技术性入队 0。观察否：双方 OBS 按候选「只登记不入队」或否定性观察闭合。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–005（5）；「否」本批新开 = 0。

**整批候选：同意。**

---

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-01 + CX-001 | 001 | 同根：L126「平台全量取真实 total」。现行 `listMyContracts`/`listMySettlements` 首行 `requireMerchantScope()`，无范围 403 `MERCHANT_SCOPE_REQUIRED`（`:172-174` 注释「不存在平台全量口径」）；`PageResponse.of(listByMerchant, page, size)` 的 `total=all.size()`；Controller 默认 `page=1/size=500` 仍成立。最小处置与两席拟稿同向，候选写明「取全量后切片」比「真分页」更诚实。未改写。 |
| GR-03 + GR-04 + CX-002 | 002 | 同根两处失效行号。L95 `sed -n 1562p`＝`<div class="info-block">`，异议期在 L1566「给商户」条；L133 所写 L1311–L1318 只到「可结算结果」，异常 L1319、授权 L1320、自助 L1322 在区间外。DEC-R8-17 废行号。候选只采条目名一支，拒绝本席 B1「改为 L1315–L1322」——正确（见第二节）。未改写句意。 |
| CX-003 | 003 | 本席 B1 未报。席甲：§11 L161「主要功能」不唯一；L162 PGM 三名 0 命中；门禁只检紧邻第一个名字故没红。独立核源成立（见第三节）。未改写。 |
| GR-02 | 004 | 本席独有。席甲 B1 GEN-005 以按钮级权限判通过。候选采本席：权限列对 `adminRoutes.ts` 对不上即 GEN-016。核源见第四节。未改写。 |
| GR-05 | 005 | 本席独有。席甲 B1 只核 HTML 转述一致、未按 L4 自设规则查实现。候选采本席。`apps/web/src` 与 `backend/src/main` 对「暗纹/watermark/敏感页/sensitive page」**0 命中**。未改写。 |

观察（无应升添加、无应升本批缺陷被降）：

- 本席 O-1 关键条款无字段：候选只登记（本文 L54 已标差距）。
- 本席 O-2 矩阵 UI-ADM-MER-007「明文显示手机号」：候选只登记（矩阵所在模块）。
- 本席 O-3 V166 头注释称 V022 死表：候选只登记（注释不是依据）。
- 本席 O-4 前端超 500 丢行：候选只登记。
- 本席 O-5 专区工单页不展示 `sla_breached`：候选只登记。
- 本席 O-6 V199 种子 reason 未写分红：候选只登记。
- 本席 O-7 `listBills` 不可达 `findAllBills`：候选只登记（M023 已登记）。
- 本席 O-8 L72 设施时限「须对商户公开」是 HTML 原句、L70 已标无入口：否定性观察（不另标完成声明缺陷），候选只登记段未点名。**不构成漏并**——B1 本就不立项，无应升队项。
- 席甲 OBS-1 `resolved`「已解决」vs「已处理待关闭」：候选只登记（页面模块；本文 L69 已如实写）。
- 席甲 OBS-2 门禁两处盲区：候选只登记（`verify-entry-anchors.mjs` 所在模块）。
- 席甲 OBS-3 注册表冻结名「受控的十类终端/终端十端」与 HTML 现文「十一」不同名：候选只登记（DEC-R8-17 设计）。003 替代名正是用现文子串「受控的十一类终端」，与该观察一致。
- 席甲 OBS-4 L54 产品口径多列空间、状态「不构成放大」：否定性观察，候选未点名。**不构成漏并**。
- 席甲 OBS-5 M023 amended 第 9 态「不并入」：否定性观察，候选未点名。**不构成漏并**。

本席 GR-03 备选「新行号 L1315–L1322」、GR-04 备选「重要事项短信通知·给商户」：候选不采。后者按检查② **n=0**，采之会红（见第二节）。属边界收窄，不是改写缺陷。

---

### 二、002 三名按检查② 能否在 BSM 内唯一定位——能；只换锚点、不换新行号

独立跑与门禁同一套 `nameSeen` / `flatEntry` / `norm`（注册表 1228 行）。

| 候选名 | 检查② | n | 定位 | 是否承载所引 |
|---|---|---:|---|---|
| BSM「给商户」 | exact-name | 1 | BSM-E0499 L1566 entry | 是。正文含「本条所定的异议期适用于非积分类的商户账单（房租、水电、物业、结算金额出账与调整、押金、违约金、分红）」——即 L95 七类名单与异议期规则 |
| BSM「商户管理·主要功能」 | exact-name | 1 | BSM-E0399 L1313 section | 是（节标题）。注册表 body 仅「主要功能」四字；同 ul 下独立 entry 为 E0400 资料、E0401 订单、E0402 差异、E0403 可结算、E0404 异常、E0405 授权。读者按该名落到 h3 即见这六条 |
| BSM「商户专区自助」 | entry-body-substr（非注册表原名） | 1 | BSM-E0406 L1322「商户提出的投诉与异议…」 | 是。正文含「商户专区自助：我的账单…我的工单…我的对账…我的合同…费用通知」 |

反证（原锚失效）：

- BSM「主要功能」：检查② 走条目名子串 **14** 条（`·主要功能` 各子系统节标题）；席甲记正文子串 **15**（`entry_body` 含该四字 15 条）。口径差 1，结论同为**不唯一**。
- 「BSM L1562」：L1562 是 info-block 开标签，不是异议期。检查③ 不认该句式，故门禁不红。
- 「HTML L1311-L1318」：实为 detail-grid → h3「主要功能」→ 资料/订单/差异/可结算；异常/授权/自助在区间外。

本席 B1 两个备选为何只许采条目名一支：

1. 「改为 L1315–L1322」＝换新行号。头注 DEC-R8-17「彻底废行号、只留条目名」；病史即行号漂移返工。采之等于再写一套门禁故意不检的行号。**不采。**
2. 「重要事项短信通知·给商户」：检查② **n=0**（无此条目名，正文也无该拼接串）。**不采。** 唯一能定位异议期的是注册表原名「给商户」。

L133 用「商户管理·主要功能」**与**「商户专区自助」两名：本席 B1 GR-03 只写了节标题一支；席甲补上自助句（L1322 不在节标题 body 内）。两名并列覆盖 L133 所列举的资料/订单/差异/异常/授权 + 自助事项，**不动句意**。同意。

非阻断（不否决 002）：B3 写 L133 时不得把节标题理解成「该条 body 已含六条 li」——六条是子条目；锚的是节，不是把 E0400–E0405 的正文抄进 PRD。禁止写回任何 `LNNNN`。

票：**PASS（细化／是／是）**。

---

### 三、003 席甲单方发现——事实成立；替代名各自唯一且承载所引

原文 `docs/merchant-portal-zone-prd-v0.1.md:161-162`：

- L161 `BSM「商户管理：商户在授权范围内完整使用自己的系统」「主要功能」「商户专区自助」「持股分红」`
- L162 `PGM「商户侧：在授权业务范围内全开放」「终端十一端」「导出与暗纹」「证照与合同关键日期」`

检查② 正则 `(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」` 每处只吃紧邻代号的第一个名字，故现行门禁只核了「商户管理：…」与「商户侧：…」两条（各 n=1，通过），后续名字漏检。本席实核漏检项：

| 原名 | 所指 HTML | 检查② n | 事实 |
|---|---|---:|---|
| 「主要功能」 | BSM | 14（名子串）/15（正文子串） | 不唯一。15 条子系统都有「主要功能」块 |
| 「终端十一端」 | PGM | 0 | PGM 0 命中；ENUM「终端十一端」n=1（ENUM-E0117 正文子串，冻结名「终端十端」）。L163 ENUM 已引，L162 误挂 PGM |
| 「导出与暗纹」 | PGM | 0 | 无此条目名、无此正文子串 |
| 「证照与合同关键日期」 | PGM | 0 | 无此条目名、无此正文子串 |

候选替代名（各 n=1，独立核承载）：

| 替代名 | 检查② | 定位 | 承载所引 |
|---|---|---|---|
| BSM「商户管理·主要功能」 | exact-name n=1 | BSM-E0399 L1313 | §11 原「主要功能」所指即商户管理段功能块标题；同 ul 六条能力见 E0400–E0405。与 002 L133 同名，不冲突 |
| PGM「受控的十一类终端」 | body-substr n=1 | PGM-E0210（冻结名「受控的十类终端」）L739 | 正文起句「受控的十一类终端：…商户网页门户、商户手机应用…」共十一类。用现文子串而非冻结「十类」，与 OBS-3 一致 |
| PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」 | exact-name n=1 | PGM-E0190 L680 | 脱敏强制、不提供关闭开关、商户客户信息例外。覆盖原「导出与暗纹」的导出/脱敏面 |
| PGM「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定」 | exact-name n=1 | PGM-E0191 L681 | 密码/暗纹/「后台、商户专区和员工手机端的所有在线页面都带…暗纹」；「敏感页上的文字不能被选中、不能被复制，也不提供页面另存与打印」；「打开敏感页这件事本身也要留痕」。暗纹与敏感页句在此条（席甲所述成立）。原「导出与暗纹」拆成脱敏+密码/暗纹两条，因 HTML 本就是相邻两条，不是发明 |
| PGM「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」 | exact-name n=1 | PGM-E0132 L531 | 证照/资质/保险/合同关键日期到期预警、商户专区可查、宽限期与到期阻断 |

其余名字（独立核仍 n=1，仅补代号前缀）：BSM「商户管理：商户在授权范围内完整使用自己的系统」（body-substr → BSM-E0396）；BSM「商户专区自助」（同上 E0406）；BSM「持股分红」（exact-name BSM-E0601）；PGM「商户侧：在授权业务范围内全开放」（body-substr → PGM-E0213）；ENUM 四名（「终端十一端」body-substr ENUM-E0117；其余三 exact-name）。L163 后续三名同样缺代号前缀、同样漏检，补前缀后各 n=1，不会新红。

修后跑 `node tools/verify-entry-anchors.mjs` 须 PASS：是 B3 验收，不是本批改门禁。写入集合不含 `tools/verify-entry-anchors.mjs`。同意。

票：**PASS（细化／是／是）**。

---

### 四、004 权限列——同意只留路由门控码；页内权限进当前能力列；不改权限模型

独立核：

| 页 | PRD 现行「权限」列 | `adminRoutes.ts` `requiredPermissions` | 页内 |
|---|---|---|---|
| ADM-MER-004 L118 | `merchant.service_work_order.view/create` | 仅 `merchant.service_work_order.view`（`:90`） | `WO_CREATE = ["merchant.service_work_order.create"]`，`PermissionButton` `:184` |
| ADM-MER-005 L119 | `merchant.portal.settlement.view/dispute` | 仅 `merchant.portal.settlement.view`（`:91`） | `DISPUTE_PERMS = ["merchant.portal.settlement.dispute"]`，`:257` |
| 其余四行 | 与路由逐字一致 | 一致 | 无斜杠写权限 |

列名「页面 / 路由 / 权限 / 当前能力」，权限格紧贴路由，形态同 `requiredPermissions`。把 create/dispute 写进该格，第一次抄进路由会让仅有 view 的商户打不开列表。席甲 B1 以「按钮级权限」判 GEN-005 通过——按钮级是真的，但**该列拿去对路由对不上**，GEN-016 仍成立。采本席 GR-02。分类细化（描述一期门控，不改谁拥有何码）。

措辞边界（同意候选，B3 不得越）：

- 「权限」列只留路由门控码：`merchant.service_work_order.view`、`merchant.portal.settlement.view`。
- 「当前能力」列保留原「读与发起 / 对账与异议留言」，括号补「发起另需 `…create`」「留言另需 `…dispute`」。`另需` 把打开页与写操作分开，不把 create/dispute 写成进页条件。
- **不改权限模型**：不把 create/dispute 加进 `adminRoutes.requiredPermissions`；不删页内 `PermissionButton`；不改 V200 授予；不改后端 `requirePermission`。只改这一文件两行的文档分列。
- 同表 003/006/007/008 已与路由一致，不动。

票：**PASS（细化／是／是）**。

---

### 五、005 暗纹/敏感页——零命中成立；L29 保留转述 + 指向 §9；不删 HTML、不改 §10

独立 `rg -n -i "暗纹|watermark|敏感页|sensitive page|sensitive-page" apps/web/src backend/src/main` → **0 命中**。`user-select: none` 仅 `.admin-tech-details > summary`（`styles.css:1702`）与 `.admin-sort-th`（`:1935`），不是敏感页防护。

L4 自设：「当前页面、接口或终端不足时，标为实现差距」。同出处导出在 §7 L111 已标「六页没有完整导出」；L29 暗纹/敏感页既未标也未进 §9。按 §2 当现行能力验收会假通过。GEN-016 + GEN-017（未把 HTML 要求标成已完成，也不得因未实现而删 HTML）。

处置边界（同意候选）：

- L29 **保留** HTML 转述原句（后台、商户专区和员工手机端…暗纹。敏感页文字不可选中…打开敏感页本身须留痕）。
- 句末补「当前商户专区尚未实现页面暗纹与敏感页防护，见 §9。」——满足 L4，不把未实现写成已落地。
- §9 增一条「页面暗纹与敏感页防护（文字不可选中与复制、不提供另存和打印、打开留痕）。」与 L29 原句同范围；与已有「商户专区完整导出能力」并列（页内暗纹 ≠ 导出文件暗纹）。
- **不删** HTML 要求（L29 原句留下）。
- **不改 §10**：`页面安全 | 页面暗纹；敏感页不可选中复制、打开留痕` 仍是 HTML 验收杠，不把「已标差距」改写成该项通过。`实现诚实` 行继续要求待建能力与代码一致——005 修完后该行才站得住。
- 不在本模块实现暗纹/敏感页（FILES 仅此 md）。不改 PGM。

分类细化：缺陷是完成/差距声明，不是发明暗纹规则，也不是把 HTML 与代码的未实现升成要在本文件修代码的冲突。同意。

票：**PASS（细化／是／是）**。

---

### 六、文首 V0.13→V0.14 与最后更新日——属于本文件最小处置范围

意见：**同意列入**，不单列 ID，不升添加。

理由：写入集合就是这一份 PRD。001–005 改的是现行实现句、锚点、权限分列、差距标注——改完若标题仍自称 V0.13 / 2026-08-31，本身构成 GEN-016 过时自称。席甲 CX-003 拟处置已含此句；先例 M019-001 标题随同文件内容前进一步。

边界：

- 只改 L1 标题与 L3 `版本：V0.13 ｜ 最后更新：2026-08-31` 两处（全文仅此两处 V0.13）。
- 最后更新日＝**B3 修复当日**，不是 B2 日、不是今天预填。
- 不加变更日志段、不改 §10、不改其它文件版本标记、不把版本行写成独立 finding。

---

### 七、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M024*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML（002/003 改的是 PRD 里的引用写法，HTML 原文不动）。002/003 的替代名全部落在已有注册表条目上，无自拟业务规则。

只登记不入队（无应升添加、无应升本批缺陷）：门禁两处盲区、矩阵手机号备注、工单第五态页面用词、V166 头注释、超 500 丢行、sla_breached 不展示、V199 reason 未写分红、findAllBills 死分支、合同关键条款无字段、注册表「十类/十端」与现文「十一」不同名。O-8 / OBS-4 / OBS-5 为否定性观察，不入队。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 1 文件：`docs/merchant-portal-zone-prd-v0.1.md`。

- 001：L126 改写为默认 `page=1/size=500`；按当前账号商户范围取回本商户全部记录后切片，`total` 为本商户真实总数；无商户范围一律拒绝（403 `MERCHANT_SCOPE_REQUIRED`），不提供平台全量口径。不加新规则。
- 002：L95「BSM「给商户」所定的异议期…」；L133「BSM「商户管理·主要功能」与 BSM「商户专区自助」的资料、订单履约、…统一入口」。不改为新行号。
- 003：§11 三行每个名字写成 `代号「名」`；「主要功能」→ BSM「商户管理·主要功能」；PGM 三名 →「受控的十一类终端」「导出文件是否脱敏…」「所有导出的文件一律带密码…」「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」；其余只补代号前缀。修后 `node tools/verify-entry-anchors.mjs` 须 PASS。
- 004：004/005 两行「权限」列只留 view；create/dispute 写进「当前能力」括号「另需」。不改 `adminRoutes` / 页内按钮 / 迁移授予。
- 005：L29 原句保留，句末补未实现并见 §9；§9 增暗纹/敏感页一条。不删 HTML 要求、不改 §10。
- 文首：V0.13→V0.14，最后更新日改 B3 当日。

禁止：改三 HTML / 锁 / 台账 / 源码 / 门禁实现 / 矩阵；把 002 写成新行号；用「重要事项短信通知·给商户」（n=0）；把 004 的 create/dispute 加进路由门控；在本模块实现暗纹；因未实现而删 L29；把 §10 页面安全改成「已标差距即通过」；发 2.2。

### 结论

全批差集 findings 无漏并、无原意改写。001–005 全部细化／是／是。002 三名按检查② 各 n=1；本席 B1 行号备选与「重要事项短信通知·给商户」均不采。003 席甲单方发现，独立核源：原「主要功能」不唯一、PGM 三名 0 命中、门禁漏检后续名字；替代名各 n=1 且承载所引（暗纹/敏感页在 PGM-E0191）。004 采本席 GR-02，权限列与路由对齐、写权限留页内，不改权限模型。005 采本席 GR-05，暗纹/watermark/敏感页在 `apps/web/src` 与 `backend/src/main` 0 命中；L29 保留转述、§9 增一条、不改 §10。V0.13→V0.14 属本文件最小处置。添加 0。HTML 申请 0。questions 0。写入集合＝本模块 1 文件。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M024 IN_PROGRESS；B3=0。

