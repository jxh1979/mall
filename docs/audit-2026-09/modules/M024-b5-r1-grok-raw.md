CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（修复后全量重审，不只看 diff；B3 累计 1）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M024 商户档案、商户类型与商户侧门户／文档与接口定义／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock V0.11；提示词 `160-audit-prompt.md` V0.17
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- 背景：B1 双 FAIL → B2-r1 双 PASS 定稿 M024-001…005 → B3-r1 改本模块 1 文档 → B4-r1 内容/归因双 PASS（本席已核 B4 grok SHA `5f9635ef932dc6d417719edea38dc5d74714991fa333a8a63958dd58e9b584e3`、席甲 SHA `ea3bc899f21f9f98bd8bd1e45d7e4fa054a0f4db414a6cf1c922a25d89f80c3c`）；findings V2.79 队列 001–005 全部否（已修）。不把「全部否」当文档已全对。
- 独立性：本席未读取 `modules/M024-b5-r1-self-raw.md`；未依赖席甲本次结论；旧 B1/B2/B4 可读作背景，本轮对现行全文与现行源码重新取证，不复用 B2/B4 票。未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未建脚本、日志或新临时产物。
- 本席步骤结论：**PASS**

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/plugins/cache/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/skills/`。按提示词 §6 收窄口径，禁止项未调用。用户指令优先：只两席、禁子代理、禁改业务。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理/禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft ask --source` 门户分页/scope；`graft grep` merchant-zone / ADM-MER-00 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、`grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废。grill-me 本体只转发 grilling |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 本文是浅模块：HTML、路由、后端、验收写进同一张「当前实现」表 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 文档声称 vs 现行源码的反馈环=只读命令；不改代码、不加探针 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借数据流（路由→页→API→scope→迁移）；作废 instrumentation / 最小改码 |
| tdd / test-driven-development | 存在 | 文档模块、B5 不写测试；只作测试有效性透镜 |
| verification-before-completion | 存在 | PASS 前实跑 sha256sum / wc / 两门禁 / 读源码；不凭印象 |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删的过时句。结论：B3 已删平台全量/行号/权限斜杠；**Lean already. Ship.** `net: -0` |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 按步骤+完成标准组织 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；本 raw 编号清单；CLI 不回带正文 |
| requesting-code-review | 存在 | B5＝本席即外审；不用 HEAD~1、不用子代理 |
| receiving-code-review | 存在 | 本席是外审产出方；本体「逐条 IMPLEMENT」作废（B5 禁止修复） |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；本席只读、无 git 写，未装 hook |
| resolving-merge-conflicts | 存在 | 无进行中的 merge/rebase，未调用本体 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读；本步无长任务 |
| audit（Arc） | Arc `skills/audit/SKILL.md` | **缺口/未按本体执行**：本体要派子代理写 `docs/arc/audits/`，与双席+§7 封闭清单冲突 |
| audit-context-building | Trail of Bits skill | **缺口/未按本体执行**：本体强制派 function-analyzer 子代理写 `audit-context/`，用户禁第三席 |
| batch-grill-me | 本机 skill 目录无独立 SKILL | **缺口**；用 grilling 自问自答代替 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| brainstorming / writing-plans / ponytail 本体 | 存在 | 阶段 A / B3 技能，B5 只读不修 |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉，未调用 |

graft 本轮：ask 门户分页 ≈25067；grep merchant-zone ≈24522；grep ADM-MER-00 ≈22307。合计约 **71900 tokens**。

---

## 1. 范围、哈希、行数（实算，不抄期待值）

独立命令与输出：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
grep '^CRITERIA-LOCK-SHA256:' docs/audit-2026-09/ledger.md | awk '{print $2}'
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
wc -l docs/merchant-portal-zone-prd-v0.1.md
# 165
sha256sum docs/merchant-portal-zone-prd-v0.1.md
# 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc
git ls-files -z -- docs/merchant-portal-zone-prd-v0.1.md | xargs -0 cat | wc -l
# 165
git ls-tree 34866c4fbb427a498f7f0bffa4c47d71792a6d1f -- docs/merchant-portal-zone-prd-v0.1.md
# 100644 blob de2fc58db90ec36eb61617f82ad7be131d0ed5fb
git show 34866c4fbb427a498f7f0bffa4c47d71792a6d1f:docs/merchant-portal-zone-prd-v0.1.md | sha256sum
# 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc
sed -n '6403p' docs/audit-2026-09/plan/criteria-lock.md
# M024 → [BSM-114, GEN-001, …, GEN-018]
git rev-parse --abbrev-ref HEAD && git rev-parse HEAD
# main
# e456ba96c84185c8d39f18f548011daac48a6466
python3 /home/james/audit-tmp/m024_anchor_check.py ; echo exit=$?
# GAP-LISTED §9 暗纹: yes
# refs=16 problems=0
# exit=0
node tools/verify-entry-anchors.mjs ; echo exit=$?
# [entry-anchors] … PASS：条目名唯一、…引用可唯一定位、正文无行号锚点…
# exit=0
```

sidecar / ledger 顶部与本席实算锁哈希相同，仅作旁证，不以它们代替实算。LOCK-AMEND-COUNT 只读见到 1（未改 ledger）。

计划 `modules.md` 写有效内容 164；现行物理行与 `git ls-files | xargs cat | wc -l` 均为 **165**（以实算为准）。B3 after tree `34866c4fbb427a498f7f0bffa4c47d71792a6d1f` 上该 blob 的 SHA 与工作区文件相同。当前工作区另有大量非本模块未提交改动（临时索引 `git write-tree` 得 `b8fe8d1cf0ce488616a37dc5a4bd06eb6341492d`），审计对象=主工作区**现行** PRD + 现行源码（含工作区已改的 `MerchantPortal*` / 商户专区页），不审 origin 独有提交。

| 文件 | 物理行 | SHA256 |
|---|---:|---|
| `docs/merchant-portal-zone-prd-v0.1.md` | 165 | `95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc` |

全文覆盖：165 行每一行均已读，不抽样。模块内无测试文件。只读追证的代码/HTML/迁移/矩阵**不在本模块 FILES 列表**，仅作 GEN-016 复现对象。

---

## 2. 全文覆盖表（文档每一段）

| 行 | 段 | 本席动作 |
|---:|---|---|
| 1–4 | 标题 V0.14 / 2026-09-20 / 现行 PRD；文首自设「不足处标差距」 | 通读；与提示词 §2 业务最终解释权一致 |
| 6–19 | §1 定位与终端 | 对照 BSM-114 入口表、ENUM「终端十一端」、PGM 商户侧与十一类终端；核 `/admin/merchant-zone/*` 与 `accountForms` |
| 21–29 | §2 账号、权限与数据边界 | 对照 PGM 业务边界/权限规则/导出暗纹；核 scope、暗纹实现、客人句是否当完成声明 |
| 31–47 | §3.1 HTML 能力 vs 当前承载 | 对照 BSM 主要功能 ul、PGM 三类商户表 |
| 48–57 | §3.2 自助七项 | 对照 BSM 自助句、持股分红主理人、PGM 证照 |
| 58–63 | §3.3 账单十一类 | 对照 ENUM 租金/账单类型；核电损/水损未落地、页面未伪造这两目 |
| 65–72 | §4 工单边界 | 对照 ENUM 商户服务工单状态/物业工单、BSM 物业服务、V022/V166；核专区类型与第五态文案 |
| 74–82 | §5 证照与当地时间 | 对照 PGM 证照条；核 V028、LocalZone.ID、无商户侧证照页 |
| 84–100 | §6 异议 | 对照 BSM「给商户」/退租句、ENUM 退租结算/营业额抽成、PGM 独立酒吧 |
| 102–111 | §7 导出与敏感页 | 对照 PGM 两条导出；核六页无导出 |
| 113–128 | §8 当前一期实现 | 对照 adminRoutes、门户 Controller/Service、矩阵六行、PiiCipher、V339 |
| 130–139 | §9 当前未实现 | 与 §3–§7 差距及现行六页交叉；核暗纹已列入 |
| 141–158 | §10 验收检查 | 当文档自检表，不当成已通过证据；核会否把未实现当已实现 |
| 160–165 | §11 依据 | 核每个 `代号「名」` 按检查② 唯一定位；核所列迁移文件存在 |

---

## 3. M024-001…005 回归（已修≠自动全对，逐条对现行文+源码）

| ID | 定稿最小处置 | 现行文 | 现行源码 | 回归？ |
|---|---|---|---|---|
| 001 | §8 改为默认 page=1/size=500；按商户范围取全量后切片；无范围 403 `MERCHANT_SCOPE_REQUIRED`；不提供平台全量 | L126–127 逐字此口径 | `MerchantPortalController` GET contracts/settlements `defaultValue = "1"`/`"500"`；`MerchantPortalService.listMyContracts/listMySettlements` 首行 `requireMerchantScope()`，`PageResponse.of(listByMerchant, page, size)`；无 `listContractsPaged`/`listAllSettlementsPaged` 分支 | 无 |
| 002 | L95 改 BSM「给商户」；L133 改 BSM「商户管理·主要功能」与 BSM「商户专区自助」；不换新行号 | L95、L133 无 `HTML L`/`BSM L` | 注册表：给商户 exact 1；商户管理·主要功能 exact 1；商户专区自助 body 1（BSM-E0406 L1322） | 无 |
| 003 | §11 每个名字写成 `代号「名」` 且可唯一定位 | L162–164 均 `BSM/PGM/ENUM「…」` | `m024_anchor_check.py` refs=16 problems=0；`verify-entry-anchors.mjs` PASS | 无 |
| 004 | 004/005 权限列只留路由 view；写权限进当前能力 | L118 view +「发起另需 create」；L119 view +「留言另需 dispute」 | `adminRoutes.ts:90–91` 仅 view；`WO_CREATE`、`DISPUTE_PERMS` 仍在页内 | 无 |
| 005 | L29 句末标尚未实现见 §9；§9 增暗纹/敏感页 | L29 末句「当前商户专区尚未实现…见 §9。」；L138 §9 条 | `rg` apps/web/src 与 backend/src/main 无「暗纹」；`user-select: none` 仅 `.admin-tech-details > summary` 与 `.admin-sort-th` | 无 |

文中无 `平台全量取真实 total`、无 `view/create`/`view/dispute` 路由混写、无「实测 LNNN」式行号。

---

## 4. 对象只读追证（现行源码 / 迁移 / HTML，不以历史 PASS 或注释为事实）

### 4.1 条目名（检查② 同源规则，本席独立跑）

16 处 `BSM|PGM|ENUM「名」`：exact 或 entry_body 子串均为 1 击。

| 引用 | 定位 |
|---|---|
| BSM「商户管理：商户在授权范围内完整使用自己的系统」 | body→BSM-E0396 L1308 h2 |
| BSM「商户管理·主要功能」 | exact BSM-E0399 L1313 h3 |
| BSM「商户专区自助」 | body→BSM-E0406 L1322 自助长句（非独立 h3） |
| BSM「持股分红」 | exact BSM-E0601 L1737 h2 |
| BSM「给商户」 | exact BSM-E0499 L1566 |
| PGM「商户侧：在授权业务范围内全开放」 | body→PGM-E0213 L749 h2 |
| PGM「受控的十一类终端」 | body→PGM-E0210（注册表名「受控的十类终端」，正文「十一类」） |
| PGM 两条导出 + 证照 | exact PGM-E0190/E0191/E0132 |
| ENUM「终端十一端」 | body→ENUM-E0117（注册表名「终端十端」，`td.k` 文为「终端十一端」） |
| ENUM 租金/账单类型、商户服务工单状态、退租结算状态 | exact |

### 4.2 HTML 转述（逐句，不缩小/放大/发明）

- §1 L8 ≈ BSM L1310 逐字（合同、空间、业务和权限范围；不看平台全局和其他商户）。
- §1 两类终端 = ENUM `td.k`「终端十一端」中的商户网页门户、商户手机应用；BSM-114 入口表「商户侧页面」「商户 App」。
- §1 L17 ≈ BSM L1310 酒吧/游乐未来独立成系统；PGM L742 可只开对应商户和模块。
- §1 L19：十一类终端共用开关/权限/范围/拒绝留痕/兜底关闭（PGM L739 先列十一类，再写经销商适用与其他终端相同的规则）。不是把经销商规则扩成商户专有规则。
- §2 全开放 / 客人属于该商户 / 价格权限 / 查看导出修改分别授权：PGM L509–510、L654–655。L28「查看权不自动包含导出权」是「分别授权」的下位说法；「修改权也不代表可以绕过审批让价格或合同直接生效」与 ENUM 合同审批链「不得录入即生效」及 §9 L139 当前无绕过入口同向，未发明新审批链。
- §2 L29 暗纹/敏感页 = PGM-E0191 正文，已标差距。
- §3.1 表 HTML 口径：BSM L1315–1320 + PGM L760–772 三类商户开放面。供应链来自 BSM L1320，不是 PGM 业态表外发明。
- §3.2 自助：BSM L1322 五项 + 物业 L1384 保洁走设施/物业；分红主理人「全部经营数据…人工成本/姓名/工资」= BSM L1764，已标未完整承载。
- §3.3 十一类 = ENUM L256 逐字。电损/水损计费公式在 ENUM L199，本文标尚未落地、不得伪造——账单页 `FEE_TYPE_LABELS` 无这两键。
- §4 七态中文 = ENUM L264；代码七值 = V022 CHECK；第五态页面「已解决」已标未对齐。V166 无专区入口属实。
- §5 预警三个月/宽限期/阻断 = PGM L531。LocalZone 单地已标差距。
- §6.1 同通道/只争数字/付款不暂停 = BSM L1322。独立酒吧二十个工作日 = PGM L429。
- §6.2 七类异议期 + 积分互不包含 + 次日起算 + 出厂十个工作日 + 提醒不影响期限 + 全天不可用才顺延 + 未登录不顺延 + 首页未读提醒不消失 = BSM「给商户」L1566。营业额抽成另配长度、无十工作日默认 = ENUM L257。等待期 30 日、新出账并入、同合同一次、顺延可见 = BSM L1351 与 ENUM L252。
- §7 四条导出规则 = PGM-E0190/E0191；「本商户客户信息不脱敏」例外在 E0190。六页无导出已标差距。

未发现把 HTML 能力写成永久禁令，或把一期六页写成 HTML 上限。

### 4.3 adminRoutes / 六页 / 门户 API

`sed -n '89,94p' apps/web/src/admin/adminRoutes.ts`：ADM-MER-003…008 路由、view 权限、`accountForms: ["merchant"]` 与 §8 表逐字一致。`AdminApp` case ADM-MER-003…008 接到六页。`routeAllowsAccountForm`：未标 accountForms 的路由对全部形态展示——§1「隔离」是专区页只对商户形态显示（单向），文档未声称商户账号看不见未标形态的平台页。

门户：

- `GET /api/v1/merchant-portal/contracts|settlements` 默认 1/500。
- Service 两 list：`requirePermission` + `requireMerchantScope()` + 本商户 list 内存切片。
- 无商户归属：`403 MERCHANT_SCOPE_REQUIRED`「此操作必须在商户范围内进行」。
- `POST .../settlements/{id}/dispute`：页内 `DISPUTE_PERMS`；只留言、不改 status。§8 未写该 POST 路径，但「当前能力」写了异议留言；不是把未实现写成已实现。

`AuthorizationService.currentMerchantScope()`（L425–431）：无 merchantId **即 throw**，不返回 empty。因此 `MerchantBillingService.listBills` 源码里的 `findAllBills` 分支在现行 `currentMerchantScope()` 下不可达。§2「后端以 merchant scope 过滤账单…」对有商户号的账号成立；§8 未把账单 API 写成平台全量。

前端合同/结算 `api.get` 只取 items、客户端 `usePagedRows(..., 10)`，**未**传后端 page/size。API 默认 1/500 仍是后端事实（§8 写的是后端）。

### 4.4 手机号 / 通知开关 / 时区 / 迁移

- `PiiCipher`：`AES/GCM/NoPadding`，信封 `enc:v1:`。费用接收人仓储写 encrypt、读 decrypt。Controller 无 `pii.contact_phone.view`（`PhoneMask.VIEW_PLAINTEXT_PERMISSION`）返回掩码。与 L128 一致。矩阵 UI-ADM-MER-007 仍写明文——矩阵不在本模块 FILES，PRD 写的是后端事实。
- `NotificationSwitchResolver`：总闸 `sms_notification` 缺行默认 `false`；域/事件缺行默认 `true`。V177 出厂总闸 FALSE，并种 `sms.notify.merchant_fee` 及 rent/utilities/property/renewal（TRUE，可改 FALSE）。`NotificationService` 开关拒绝时 status=`skipped_switch_off`。费用事件集合四码。与 L54 冲突登记一致，未把可关闭写成业务规则。
- `LocalZone.ID = Asia/Shanghai`（L19），类注释写死单地。L82 差距成立。
- 迁移文件均在：V022（四类型七态 CHECK）、V028（`compliance_expiry_items` / `contract_key_dates`）、V166、V177、V197（`tcs_term_merchant_portal` TRUE）、V199（module `merchant` TRUE）、V200（settlement.view/dispute、fee_recipient.manage）、V339。无商户专区「我的证照」页；六页零 `/api/v1/facility`。电损/水损在 backend 无独立 feeType。

### 4.5 六页当前能力（工作区现状，含 M020/M023 未提交改动）

- 账单：`/api/v1/merchant-billing/bills`；账龄 KPI、费用行；无电损/水损键。
- 工单：四类型；`resolved:"已解决"`；无保洁、无 V166。
- 对账：结算明细中文名 + 异议留言；`voided:"已作废"`（ENUM 无此态；本文未把 voided 写成 HTML 态）。
- 合同：租期=生效~到期、空间、状态（`drafting:"草拟"`）、押金、续签自；无「关键条款」字段。
- 费用通知：rent/utilities/property/renewal。
- 分红：只读方案/计算单/留存/算式明细；无审批；无人工成本/员工工资。

六页无导出按钮。无页面暗纹、无敏感页禁选中/禁打印/打开留痕。

### 4.6 UI 矩阵

`docs/full-admin-ui-acceptance-matrix-v0.1.md` L56–61 确有 UI-ADM-MER-003..008。L124「不得再写成矩阵缺失」成立。

---

## 5. 三线记录

### 5.1 第一性原理三问

**Q1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

答得上。BSM-114 把「商户侧页面」「商户 App」与管理后台/Web 前端拆开；BSM「商户管理」要求完整使用+自助；PGM「商户侧：在授权业务范围内全开放」按业态列开放面；ENUM「终端十一端」点名商户网页门户与商户手机应用。本文把这些映射到一期六页与差距清单。删除后，实现者会把 `/admin/merchant-zone/*` 误当成平台后台，或把未落地能力当成永久禁令（L15、L111、L130–139）。有业务上位，不走删除。四分类：细化。

**Q2 声称的行为有没有被实际执行验证？**

文档不可执行（runbook：HTML 对照 + 命令/结构）。本席用类型 1+4：HTML 原文、`sha256sum`/`wc`、两门禁、读 adminRoutes/Controller/Service/PiiCipher/Resolver/LocalZone/六页/矩阵/迁移。§8 路由/API/分页/权限、§9 差距、001–005 修复句均可复现。无不可复现的当前事实声称。

**Q3 哪个前提未经验证就当真？**

B1 时的四个假前提（平台全量、权限斜杠、行号锚、暗纹已具备）在现行文中已消除。本轮未发现新的「把计划/旧证据写成当前事实」的前提。工作区 M020/M023 未提交改动（fail-closed、异议表单、起草态、合同纯文本）已被 §8/§3.2 覆盖，未把已补上的能力继续写成「完全没有」。

### 5.2 顺从式（19 项逐条；不新增判据）

先假定本文意图正确（记录 HTML + 诚实标差距），再核映射判据。

| ID | 适用？ | 证据 |
|---|---|---|
| BSM-114 | **适用** | 锁原文=各类入口表（h2「各类入口：不同人只看自己该看的、只做自己该做的」）。本文 §1 抽出商户网页门户/手机应用，不把管理后台、客人端写成商户终端。§3.1 酒吧/游乐/餐饮零售与 PGM 业态表同句。未把「当前六页」写成 HTML 上限（L15、L111、L130）。未缩小「供应/库存、员工、看板、不看平台全局」 |
| GEN-001 | 适用 | 有可验证用途（5.1 Q1）。B3 已删错误行号与过时全量句。不为极端场景加终端或分页协议 |
| GEN-002 | 适用（声称可执行入口） | 路由/API/默认分页/权限码与现行入口一致（§4.3）。无再发现口声称 |
| GEN-003 | **不适用（范围内无测试）** | FILES 仅 1 个 md。不把矩阵 e2e 或六页 `*.test.tsx` 算本模块测试 |
| GEN-004 | 弱适用 | 文档不处理输入。L128 掩码/信封描述后端边界，与 Controller/PiiCipher 一致 |
| GEN-005 | 适用 | `accountForms: ["merchant"]` 与六路由一致。门户三方法 fail-closed。权限列不再把 create/dispute 写成路由门控 |
| GEN-006 | 弱适用 | 异议「只留言不改状态、付款不暂停」与 Service 一致。无自创状态机 |
| GEN-007 | 弱适用 | 本文未把幂等键/500 字写成 HTML 规则（那些在 M020 代码）。不把未写当缺陷 |
| GEN-008 | 适用 | 十一类=ENUM L256。工单七态中文=ENUM L264；V022 七码；第五态「已解决」已标差距。退租中文与 ENUM L252 一致。路由权限码与 adminRoutes 一致 |
| GEN-009 | 弱适用 | 门户失败路径 `recordFailure`。文档未写英文校验句 |
| GEN-010 | 适用 | AES-GCM + 掩码与 PiiCipher/Controller/V339 一致，未扩成新业务规则 |
| GEN-011 | 弱适用 | 留痕规则来自 PGM-E0191。六页有 `AuditTrailHint`，无「打开敏感页」专用留痕（已在 §9） |
| GEN-012 | 弱适用 | 默认 size=500 是后端默认；未把前端 10 条/页写成 API 默认 |
| GEN-013 | 不适用 | 无构建/CI 声称 |
| GEN-014 | 适用（迁移编号声称） | V022/V028/V166/V177/V197/V199/V200/V339 文件均在。§11 列表未写 V166/V177，正文已引用且文件存在 |
| GEN-015 | 弱适用 | 中文文案。第五态中文偏差已标。无本模块 UI |
| GEN-016 | **核心适用 PASS** | 路径/路由/accountForms/API/分页默认/权限码/事件名/开关名/迁移/矩阵六行/条目名/差距声明可从当前纳入范围复现。001–005 旧不可复现句已改。不以历史 PASS、文档互引、注释当事实 |
| GEN-017 | 适用 | 主体=HTML 下位实现记录+差距，**细化**。未把一期六页发明成新终端。L96 不套用十工作日默认是对「给商户」适用范围的收窄提醒，不是新账单种类。四分类顺序：无与 HTML 明文冲突的现行完成声明 |
| GEN-018 | 适用（本席） | 结论落到 HTML 原文、源码、迁移、命令输出。不引用席甲本次 raw、不引用历史 PASS 当依据 |

### 5.3 对抗式（grilling 结构，自问自答；不等操作员、不派子代理）

派发指定五问，全部自答后才收口。

❓ **Q1 研发照第 8 节去调接口 / 配路由权限会怎样？**

➡️ 抄 §8 表：六路由 + 各行 **仅 view（或 fee_recipient.manage / dividend.view）** + `accountForms: ["merchant"]`，与 `adminRoutes.ts:89–94` 一致，能打开列表。004 发起另需 `merchant.service_work_order.create`、005 留言另需 `merchant.portal.settlement.dispute` 写在「当前能力」，第一次正常抄路由门控不会把写权限塞进 `requiredPermissions`。GET contracts/settlements 默认 1/500、无商户范围 403 `MERCHANT_SCOPE_REQUIRED`，与 Controller/Service 一致。账单/工单/分红/接收人走 `/merchant-billing`、`/merchant-service`、`/finance/dividends`，§8 没有把它们写成 merchant-portal 前缀。POST dispute 路径未写在 §8，但当前能力与专区页已有留言；第一次按表配路由不会配错。不立项。

❓ **Q2 读者按每个条目名去 HTML 搜会怎样？**

➡️ 16 处均能唯一定位（门禁 + 本席 python）。「给商户」「商户管理·主要功能」「持股分红」及三条 PGM 导出/证照、三条 ENUM 状态/账单类型：搜条目名即中。`BSM「商户专区自助」` 搜到 L1322 自助句（不是独立标题），正文确含自助五项。`PGM「受控的十一类终端」`、`ENUM「终端十一端」` 搜到正文/单元格「十一」字样（注册表标题仍是十类/十端）。不会落到空行或撞上多条。不立项。

❓ **Q3 验收照第 10 节逐行做，会不会把未实现当已实现？**

➡️ 第 10 节多数行是 HTML 能力验收（账单口径、证照、导出、页面安全），配合 §9 差距清单与最后一行「实现诚实」。终端/工单/费用通知写明「列差距 / 冲突已登记」。按第 10 节当**产品**验收，未实现项会失败（证照页、导出、暗纹、费用总表），不会误标通过。按「实现诚实」核路由/分页/手机号/待建能力与代码，现行一致。B3 明确不改第 10 节，暗纹能力仍在「页面安全」行——这是 HTML 要求，不是「当前已有暗纹」；§9 已列差距。不立项。

❓ **Q4 哪些差距声明其实已被后续代码补上而文档没跟上？**

➡️ 合同页已展示租期/到期日/空间/状态/押金/草拟码；L53 仍写「只读最小披露，关键条款与到期情况须完整展示」。到期日已有，**关键条款仍无字段**，整句仍有真实缺口。对账异议留言、M020 fail-closed、暗纹未实现、电损水损、证照、导出、手机应用、V166 入口、主理人人工成本——与代码一致，没有「已补上却仍写差距」。半句偏旧见观察 O-1，不单独立项。

❓ **Q5 M020 与 M023 已合并（含工作区未提交）的改动是否还有别处让文档过时？**

➡️ M020-001：§8 已去掉平台全量，与现行 `requireMerchantScope` 一致。M023：异议表单、起草态 `drafting`、合同号/空间改为纯文本（商户点平台工作台无权限）——文档未声称 TraceLink，§8「我的对账与异议留言」「我的合同只读」仍对。工作区 `MerchantPortalController` 幂等键改走 `ApiRequests.requireIdempotencyKey`，本文未写 IDEMPOTENCY_KEY_REQUIRED 英文码。未发现把已删全量写回、或把已落地异议写成「只读不能留言」。

补充对抗：

❓ **Q6 客人「全部对接信息」写在 §2、不在 §3/§9，是否与暗纹同类的假完成？**

➡️ PGM L510 是商户档案**业务边界**（「商户可以查看本商户客人的全部对接信息」），**没有**「在商户专区」落点。证照条（PGM L531）写明「商户在商户专区可查看」，故 §3.2/§9 单列。暗纹句原写「在线页面都带」，是对当前页面的完成声明。客人句与「全开放」同类，不是「当前六页已能看客人」。六页确无客人入口；不把每个 HTML 边界句扩成差距清单（提示词：不为理论场景加需求）。观察 O-5。

❓ **Q7 照 L126 调门户、超过 500 条会怎样？**

➡️ 前端不传 page/size、unwrap 只取 items。要「默认 500 + 前端丢分页 + 单商户>500」同时成立才丢行。提示词：不为极端场景补需求。观察 O-2。

---

## 6. Findings

无。临时编号空集。不把观察升格为 finding。不重开 M024-001…005（已修且本轮复现为已落地）。不新增判据。

---

## 7. 观察（不立项）

- **O-1** L53「关键条款与到期情况须完整展示」：到期日已展示，关键条款仍无字段。半句偏旧，整句仍有缺口。
- **O-2** 合同/结算前端 `api.get`+unwrap 丢分页元数据、客户端 10 条/页。后端默认 500 仍是 §8 所写事实。超 500 才丢行。
- **O-3** 矩阵 UI-ADM-MER-007 备注「明文显示手机号」与 PRD L128/Controller 掩码不一致。矩阵不在本模块。
- **O-4** `listBills` 源码仍有 `findAllBills`，现行 `currentMerchantScope()` 使其不可达。PRD 未写账单 API 平台全量。
- **O-5** 客人全部对接信息无专区页、未进 §3/§9；属 PGM 业务边界转述，无「商户专区」落点，不按暗纹假完成处理。
- **O-6** 专区工单页不展示 `sla_breached`；后端确有升级。本文写后端「超时自动升级并标记」，不假。
- **O-7** V199 种子 reason 未点名分红。PRD 未引用该 reason 字符串。
- **O-8** 结算页 `voided`「已作废」为代码态；ENUM 退租结算状态无此值；本文未把它写成 HTML 态。
- **O-9** 计划 modules.md 行数 164 vs 实算 165；以实算为准，不改计划（阶段 B 锁范围）。

ponytail-review：无可删的过时完成声明或权限混写。`Lean already. Ship.`

---

## 8. 本席结论

- 覆盖：模块 1 文件 165 行全文；19 项映射判据逐项有适用/证据；三线均执行。
- 001–005 修复落地，无回归。
- 无新的、带四类证据的工程缺陷。
- 无未答 questions（本席不发问）。
- 锁哈希独立实算 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。
- **本席 B5-r1：PASS**

合取与 ledger 收口由席甲在两份本次完整 raw 封存后进行；本席不改 ledger/findings、不写收口文件。
