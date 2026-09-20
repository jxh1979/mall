CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（文档与接口定义模块）
findings 正本：`docs/audit-2026-09/findings.md` V2.78，归属「是」且已定稿：M024-001…005
独立性：本席未读 `M024-b4-r1-self-raw.md`；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本、日志或临时产物。旧 B1/B2 可对照（`M024-b1-grok-raw.md` B2-r1 段、findings V2.78）；本轮以 V2.78 定稿最小处置为准。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检。
2. `using-arc`：已读；其默认「派评审子代理」与双席铁律冲突，本席不走 Arc 审计流程。
3. `graft`：定位 `MerchantPortalService.requireMerchantScope` / `listMyContracts` / `listMySettlements`、`MERCHANT_SCOPE_REQUIRED`、`adminRoutes.ts` ADM-MER-003…008（只读对照，未改那些文件）。三呼约节省 107,299 tokens。
4. `grilling` / `grill-me`：对抗式自问自答；本体「等用户 / 派子代理」作废。`grill-me` 本体只转发 grilling。
5. `ponytail-review`：只找可删的多余抽象；不改码。结论：Lean already. Ship.
6. `verification-before-completion`：PASS 前当场复跑 python 检查、`node tools/verify-entry-anchors.mjs`、两树 `git diff --binary` 与冻结 diff `cmp`、暗纹词检索。
7. `systematic-debugging` / `diagnosing-bugs`：只借读错误→复现→追数据流。红环=`python3 /home/james/audit-tmp/m024_anchor_check.py`（既有，本席不新建）。本体 instrumentation / 最小改码试假说作废。
8. `tdd` / `test-driven-development`：本 diff 是文档内容修复，按提示词不强制落测试；改用 2.3 类型 4 命令举证。
9. `requesting-code-review`：B4＝本席即外审；不用 HEAD~1、不用子代理复审。
10. `codebase-design`：核路由门控 vs 页内 `PermissionButton` 既有缝，确认 004 只描述不新开缝。
11. `writing-for-agents`：本 raw 给下游席读。
12. `i-have-adhd`：编号、无寒暄；本文件第一行仍必须是判据锁哈希（提示词覆盖输出模式）。
13. `long-running-background-tasks`：已读；本步无长任务。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts` 无合并。

缺口（实找无目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。
可选存在未用：`git-guardrails-claude-code`（本席只读、无 git 写）。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 与 ledger 顶部同值，仅作旁证，不以它们代替实算。

冻结 diff：
```
wc -c /home/james/audit-tmp/m024_b3_r1.diff → 7238
sha256sum → e302e64e5fed61286e94a1a3440ffa507604a7861ea83784c4402e9351335f28
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `7f7c404c89acc2ed2c77befadc115bb19097f7fd`
- after  `34866c4fbb427a498f7f0bffa4c47d71792a6d1f`

```
git diff --binary <before> <after> | sha256sum
→ e302e64e5fed61286e94a1a3440ffa507604a7861ea83784c4402e9351335f28
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m024_b3_r1.diff → CMP_OK
git diff --stat：1 file changed, 12 insertions(+), 11 deletions(-)
git diff --name-status：仅 M docs/merchant-portal-zone-prd-v0.1.md
hunks=5（冻结 diff 5 个 @@）
```

blob：
- before `fa388e15cb531be481f3458cd33a46fed4c5ad55` 内容 SHA256 `3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f`（与 B1 grok-raw 对象文档 SHA 一致）
- after  `de2fc58db90ec36eb61617f82ad7be131d0ed5fb` 内容 SHA256 `95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc`（165 行）
- 工作区 `git hash-object docs/merchant-portal-zone-prd-v0.1.md` = after blob。分支 `main`。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.78 归属「是」的 ID；归不到即越界。

---

## 2. 定稿最小处置（审计标尺，摘 V2.78）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M024-001 | 细化 | 是 | L126 改写为现行事实：默认 `page=1/size=500`；按当前账号商户范围取回本商户全部记录后切片，`total` 为本商户真实总数；无商户范围一律拒绝（403 `MERCHANT_SCOPE_REQUIRED`），不提供平台全量口径。不加新规则 |
| M024-002 | 细化 | 是 | 只换锚点不动句意。L95→BSM「给商户」；L133→BSM「商户管理·主要功能」与 BSM「商户专区自助」…统一入口。不改为新行号 |
| M024-003 | 细化 | 是 | §11 每个名字写成 `代号「名」` 并换成可唯一定位名：BSM「商户管理·主要功能」；PGM「受控的十一类终端」、PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」、PGM「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定」（暗纹与敏感页句在此条）、PGM「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」；其余只补代号前缀。修后 `node tools/verify-entry-anchors.mjs` 须 PASS |
| M024-004 | 细化 | 是 | 004/005「权限」列只留路由门控 `…view`；页内权限写入「当前能力」：004「我的工单读与发起（发起另需 `merchant.service_work_order.create`）」、005「我的对账与异议留言（留言另需 `merchant.portal.settlement.dispute`）」。不改权限模型 |
| M024-005 | 细化 | 是 | L29 保留 HTML 转述原句，句末补「当前商户专区尚未实现页面暗纹与敏感页防护，见 §9。」；§9 增一条「页面暗纹与敏感页防护（文字不可选中与复制、不提供另存和打印、打开留痕）。」。不删 HTML 要求、不改 §10 验收条件 |

文首 V0.13→V0.14 与最后更新日：findings V2.78 候选段写明「写入集合＝本模块 1 文件（同批把文首版本 V0.13→V0.14、最后更新日改为修复当日）」；B2-r1 双席同意（本席 B1 grok-raw V0.02：「文首 V0.13→V0.14 属本文件最小处置」）。无第六 ID，不是无 ID 新缺陷。

只登记不入队（不得混进本 diff）：门禁检查②/③ 盲区、矩阵 UI-ADM-MER-007 明文手机号、工单 `resolved` 中文、V166 注释、前端超 500 丢行、`sla_breached`、V199 种子、`listBills` 不可达分支、合同关键条款字段、注册表「十类/十端」与 HTML「十一」不同名。本 diff 未碰这些。

---

## 3. diff 全文（冻结件 7238 bytes，与 `git diff --binary` 字节级相同）

```
diff --git a/docs/merchant-portal-zone-prd-v0.1.md b/docs/merchant-portal-zone-prd-v0.1.md
index fa388e15..de2fc58d 100644
--- a/docs/merchant-portal-zone-prd-v0.1.md
+++ b/docs/merchant-portal-zone-prd-v0.1.md
@@ -1,6 +1,6 @@
-# 商户侧与商户专区产品口径 PRD V0.13
+# 商户侧与商户专区产品口径 PRD V0.14
 
-> 版本：V0.13 ｜ 最后更新：2026-08-31 ｜ 状态：现行 PRD
+> 版本：V0.14 ｜ 最后更新：2026-09-20 ｜ 状态：现行 PRD
 > 业务最终解释权：三份业务 HTML。本文同时记录当前实现；当前页面、接口或终端不足时，标为实现差距，不能据此缩小 HTML 的商户侧能力。
 
 ## 1. 定位与终端
@@ -26,7 +26,7 @@ HTML 明列两类商户终端：
 - 商户的客人属于该商户；商户可查看本商户客人的全部对接信息，但不得查看其他商户客人或平台全局数据。
 - 未获价格权限的商户人员，即使业务计算使用了内部结算价，也不得查看或修改该价格。
 - 查看、导出、修改分别授权；查看权不自动包含导出权，修改权也不代表可以绕过审批让价格或合同直接生效。
-- 后台、商户专区和员工手机端的在线页面都带可反向推算账号、设备和打开时间的暗纹。敏感页文字不可选中、不可复制，不提供另存和打印；打开敏感页本身须留痕。
+- 后台、商户专区和员工手机端的在线页面都带可反向推算账号、设备和打开时间的暗纹。敏感页文字不可选中、不可复制，不提供另存和打印；打开敏感页本身须留痕。当前商户专区尚未实现页面暗纹与敏感页防护，见 §9。
 
 ## 3. HTML 要求的商户侧能力
 
@@ -92,7 +92,7 @@ HTML 明列两类商户终端：
 
 ### 6.2 异议期
 
-- BSM L1562 所定的异议期只适用于房租、水电、物业、结算金额出账与调整、押金、违约金、分红七类非积分商户账单；积分结算账单的账期与异议期由全国积分结算中心统一规定，两者互不包含。
+- BSM「给商户」所定的异议期只适用于房租、水电、物业、结算金额出账与调整、押金、违约金、分红七类非积分商户账单；积分结算账单的账期与异议期由全国积分结算中心统一规定，两者互不包含。
 - 上述七类账单的异议期自账单在商户专区可查看之日的次日起算；长度按项目可配，出厂默认十个工作日，须同时配置上下限。营业额抽成账单另按其专门条款配置异议期长度并同时配置上下限，同样从账单在商户专区可查看之日的次日起算，不继承十个工作日的出厂默认值；其他未被 HTML 指定适用范围的账单不得自行套用该默认值。
 - 短信和商户手机应用推送只是提醒；无论是否发送、发送是否成功，都不影响异议期的起算与届满。
 - 只有商户专区在项目时区一个完整自然日全天不可用，该日才不计入；部分时段不可用不顺延。
@@ -115,26 +115,27 @@ HTML 明列两类商户终端：
 | 页面 | 路由 | 权限 | 当前能力 |
 |---|---|---|---|
 | ADM-MER-003 | `/admin/merchant-zone/bills` | `merchant.billing.view` | 我的账单只读 |
-| ADM-MER-004 | `/admin/merchant-zone/work-orders` | `merchant.service_work_order.view/create` | 我的工单读与发起 |
-| ADM-MER-005 | `/admin/merchant-zone/settlements` | `merchant.portal.settlement.view/dispute` | 我的对账与异议留言 |
+| ADM-MER-004 | `/admin/merchant-zone/work-orders` | `merchant.service_work_order.view` | 我的工单读与发起（发起另需 `merchant.service_work_order.create`） |
+| ADM-MER-005 | `/admin/merchant-zone/settlements` | `merchant.portal.settlement.view` | 我的对账与异议留言（留言另需 `merchant.portal.settlement.dispute`） |
 | ADM-MER-006 | `/admin/merchant-zone/contracts` | `contract.view` | 我的合同只读 |
 | ADM-MER-007 | `/admin/merchant-zone/fee-recipients` | `merchant.portal.fee_recipient.manage` | 费用接收人维护 |
 | ADM-MER-008 | `/admin/merchant-zone/dividends` | `finance.dividend.view` | 我的分红只读 |
 
 `docs/full-admin-ui-acceptance-matrix-v0.1.md` 已存在 UI-ADM-MER-003..008 六行；不得再写成矩阵缺失。
 
-`GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-portal/settlements` 使用真分页，默认 `page=1/size=500`，平台全量取真实 total；商户 scope 只见本商户。
+`GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-portal/settlements` 默认 `page=1/size=500`；后端按当前账号的商户范围取回本商户全部记录后切片，`total` 为本商户记录的真实总数；当前账号没有商户范围时一律拒绝（403 `MERCHANT_SCOPE_REQUIRED`），不提供平台全量口径。
 
 费用通知接收人手机号当前落库使用 AES-GCM 信封密文。当前后端无 `pii.contact_phone.view` 时返回掩码，短信发送链路在仓储解密后使用明文；这是当前代码事实，不扩成新的业务规则。
 
 ## 9. 当前未实现，不等于业务禁止
 
 - 商户手机应用。
-- HTML L1311-L1318 的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。
+- BSM「商户管理·主要功能」与 BSM「商户专区自助」的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。
 - 酒吧与游乐/乐园未来独立成系统的承载能力。
 - 费用总表与一张综合账单的完整展示。
 - 商户侧“我的证照”及宽限期闭环。
 - 商户专区完整导出能力。
+- 页面暗纹与敏感页防护（文字不可选中与复制、不提供另存和打印、打开留痕）。
 - 当前页面没有在线支付通道绑定，也没有绕过审批直接让价格或合同生效的入口；这只说明当前实现，不形成永久禁止商户发起经授权申请的业务规则。
 
 ## 10. 验收检查
@@ -158,7 +159,7 @@ HTML 明列两类商户终端：
 
 ## 11. 依据
 
-- BSM「商户管理：商户在授权范围内完整使用自己的系统」「主要功能」「商户专区自助」「持股分红」
-- PGM「商户侧：在授权业务范围内全开放」「终端十一端」「导出与暗纹」「证照与合同关键日期」
-- ENUM「终端十一端」「租金/账单类型」「商户服务工单状态」「退租结算状态」
+- BSM「商户管理：商户在授权范围内完整使用自己的系统」、BSM「商户管理·主要功能」、BSM「商户专区自助」、BSM「持股分红」
+- PGM「商户侧：在授权业务范围内全开放」、PGM「受控的十一类终端」、PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」、PGM「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定」、PGM「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」
+- ENUM「终端十一端」、ENUM「租金/账单类型」、ENUM「商户服务工单状态」、ENUM「退租结算状态」
 - 当前实现：V022、V028、V197、V199、V200、V339；`adminRoutes.ts`、商户专区六页、`MerchantPortalController/Service`
```

---

## 4. diff 逐块 / 逐行归因（5 hunk，12+/11-）

归因符号：主 ID。版本行随本文件本批全部「是」ID 记，不另开无 ID 行。

### 4.1 H01 `@@ -1,6 +1,6`

| 行 | 旧→新 | ID |
|---|---|---|
| `-# … PRD V0.13` / `+# … PRD V0.14` | 文首版本 | 001–005 同批写入集合（B2 双 PASS；V2.78 候选段明文） |
| `-> 版本：V0.13 ｜ 最后更新：2026-08-31 …` / `+> 版本：V0.14 ｜ 最后更新：2026-09-20 …` | 元数据日 | 同上。日期＝B3 当日（2026-09-20），与执行日志 START 一致 |

未改 L4 自设规则句。无越界。

### 4.2 H02 `@@ -26,7 +26,7`

| 行 | 内容 | ID |
|---|---|---|
| `-` 原暗纹/敏感页整句 | 删除未标差距的现行口吻 | 005 |
| `+` 原句一字不删 +「当前商户专区尚未实现页面暗纹与敏感页防护，见 §9。」 | 最小处置句末补差距 | 005 |

### 4.3 H03 `@@ -92,7 +92,7`

| 行 | 内容 | ID |
|---|---|---|
| `- BSM L1562 所定的异议期…` | 废行号锚点 | 002 |
| `+ BSM「给商户」所定的异议期…` | 只换锚点，七类账单句意不动 | 002 |

未写成新行号（未用 L1566）。

### 4.4 H04 `@@ -115,26 +115,27`

| 行 | 内容 | ID |
|---|---|---|
| `- ADM-MER-004 … view/create \| 我的工单读与发起` / `+ … view \| …（发起另需 create）` | 权限列只留路由门控；页内码进能力列 | 004 |
| `- ADM-MER-005 … view/dispute \| 我的对账与异议留言` / `+ … view \| …（留言另需 dispute）` | 同上 | 004 |
| 上下文 ADM-MER-003/006/007/008 | 无 `+`/`-` | 不入归因（未改） |
| `- GET … 使用真分页…平台全量取真实 total；商户 scope 只见本商户。` / `+ GET … 默认 page=1/size=500；…切片…403 MERCHANT_SCOPE_REQUIRED…不提供平台全量口径。` | 去掉已删的平台全量口径，写成现行切片 | 001 |
| `- HTML L1311-L1318 的资料、…统一入口。` / `+ BSM「商户管理·主要功能」与 BSM「商户专区自助」的资料、…统一入口。` | 只换锚点 | 002 |
| `+ - 页面暗纹与敏感页防护（文字不可选中与复制、不提供另存和打印、打开留痕）。` | §9 新增差距条 | 005 |

H04 上下文含 `## 10. 验收检查` 标题行，无 `+`/`-`，§10 表体不在本 hunk 的变更集（见 §5.4）。

### 4.5 H05 `@@ -158,7 +159,7`

| 行 | 内容 | ID |
|---|---|---|
| `- BSM「…完整使用自己的系统」「主要功能」「商户专区自助」「持股分红」` / `+` 四名各带 `BSM`，「主要功能」→「商户管理·主要功能」 | 003 |
| `- PGM「…全开放」「终端十一端」「导出与暗纹」「证照与合同关键日期」` / `+` 五名各带 `PGM`，三处 0 命中名换成定稿长名 | 003 |
| `- ENUM「终端十一端」…` / `+ ENUM「终端十一端」、ENUM「租金/账单类型」、ENUM「商户服务工单状态」、ENUM「退租结算状态」` | 只补代号前缀 | 003 |

「当前实现：V022…」上下文未改。

### 4.6 归因结论

12 插入、11 删除均可落到 M024-001…005（版本两行归本批写入集合）。  
未见「顺手优化 / 换行号 / 改 §10 / 删 HTML 要求 / 改其他文件 / 改门禁脚本」。  
`git diff --name-only` 闭集＝1 文件。  
**归因票：PASS。**

---

## 5. 内容审计

### 5.1 三线（本步范围＝该 diff，不是整模块重审）

**第一性原理**
1. 删掉这些改写，V2.78 五条「是」仍红：分页句与代码相反、行号锚点、§11 不可定位、权限列与路由不一致、暗纹写成已实现。必须存在。
2. 声称的现行事实：下列命令/源码已核（类型 4）。
3. 未验证前提：无。暗纹「零实现」按派发词在 `apps/web/src` 与 `backend/src/main` 检索，不以全仓 CSS `user-select` 冒充。

**顺从式**  
假定 B3 意图正确，逐 ID 对照最小处置原文与 after 树对应句，见 5.3。

**对抗式（grilling 自答）**
- Q：版本行无独立 ID，是否越界？→ 否。V2.78 写入集合与 B2 双 PASS 已收进本批；改内容后标题仍自称 V0.13/2026-08-31 即 GEN-016 过时自称。先例：M023 B4 把版本行归该文件主修 ID。
- Q：BSM「给商户」四字名是否指错条、不承载异议期？→ 注册表 exact n=1 = BSM-E0499，`line_now=1566`。HTML L1566 正文含「本条所定的异议期适用于非积分类的商户账单（房租、水电、物业、结算金额出账与调整、押金、违约金、分红）」。承载。未改用「重要事项短信通知·给商户」（B2 已否）。
- Q：「商户管理·主要功能」注册表 body 只是「主要功能」，是否空壳？→ `entry_kind=section`，HTML L1313 `<h3>主要功能</h3>` 下列 L1315 资料、L1316 订单履约、L1317 差异、L1319 异常、L1320 授权业务功能。承载。自助句在同节 L1322，且 BSM「商户专区自助」经 body 唯一定位到 BSM-E0406（该 li 含「商户专区自助：我的账单…」）。
- Q：删「真分页」是否删需求？→ HTML 无「真分页」一词。现行是 `PageResponse.of(listByMerchant, page, size)` 内存切片。删的是假实现句。
- Q：§10「页面安全」仍要求暗纹，与 §9 差距是否矛盾？→ 005 明确不改 §10、不删 HTML 要求。验收条件仍在；差距进 §9。一致。
- Q：`styles.css` 两处 `user-select: none` 是否已实现敏感页？→ `.admin-tech-details > summary` 与 `.admin-sort-th`，表头/折叠摘要，不是页面暗纹或敏感页。`暗纹|watermark|敏感页|sensitive page` 在 `apps/web/src` 与 `backend/src/main` 0 命中。
- Q：004 能力列反引号会不会插出额外 `|` 拆表？→ §8 表 8 行（表头+分隔+6 数据）每行 pipes=5、cells=4。完好。
- Q：§11 长 PGM 名是否发明新规则？→ 注册表/HTML 既有条目名或正文子串，003 最小处置逐字指定。

### 5.2 与现行源码 / 三 HTML（只读）

**001 分页与 scope**
- `MerchantPortalController.java:23-43`：`@RequestMapping("/api/v1/merchant-portal")`；`GET /contracts`、`GET /settlements`；`@RequestParam page defaultValue="1"`、`size defaultValue="500"`。
- `MerchantPortalService.java:58-111`：两列表方法首行 `requirePermission` 后立即 `requireMerchantScope()`；`PageResponse.of(contractRepository.list*ByMerchant(merchantId), page, size)`。
- `requireMerchantScope:172-175`：`currentMerchantScope().orElseThrow` → 403 `MERCHANT_SCOPE_REQUIRED`，「不存在平台全量口径」。
- `PageResponse.of:16-21`：对已取全量 `subList` 切片，`total = all.size()`。
- after L126 与上述逐句同义。去掉「真分页」「平台全量取真实 total」。不加新规则。

**004 路由 vs 页内权限**
`adminRoutes.ts:89-94` 现行 `requiredPermissions`：

| pageId | route | requiredPermissions |
|---|---|---|
| ADM-MER-003 | `/admin/merchant-zone/bills` | `merchant.billing.view` |
| ADM-MER-004 | `/admin/merchant-zone/work-orders` | `merchant.service_work_order.view` |
| ADM-MER-005 | `/admin/merchant-zone/settlements` | `merchant.portal.settlement.view` |
| ADM-MER-006 | `/admin/merchant-zone/contracts` | `contract.view` |
| ADM-MER-007 | `/admin/merchant-zone/fee-recipients` | `merchant.portal.fee_recipient.manage` |
| ADM-MER-008 | `/admin/merchant-zone/dividends` | `finance.dividend.view` |

after §8「权限」列与上表六行逐字一致。  
`MerchantZoneWorkOrdersPage.tsx:13` `WO_CREATE = ["merchant.service_work_order.create"]`，L182-187 `PermissionButton permissions={WO_CREATE}`。  
`MerchantZoneSettlementsPage.tsx:15` `DISPUTE_PERMS = ["merchant.portal.settlement.dispute"]`，L255-261 `PermissionButton permissions={DISPUTE_PERMS}`。  
能力列括注与页内码一致。未改路由、未改权限模型。

**005 暗纹 / 敏感页零实现**
```
rg -i '暗纹|watermark|敏感页|sensitive page' apps/web/src backend/src/main
→ 0 命中，exit 1
```
L29 原句保留（HTML 要求仍在）；句末指向 §9；§9 L138 新增差距条。§7 导出差距（L111）未动。

**002/003 条目名 × 检查②**

检查②规则（`tools/verify-entry-anchors.mjs:240-263`）：`(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」`；先对注册表 `entry_name` 精确命中，否则正文（可省略号分段）须恰好一条。本席对 after PRD 16 处引用逐个定位，全部 n=1：

| 位置 | 引用 | 定位 | 是否承载所引 |
|---|---|---|---|
| L95 | BSM「给商户」 | exact BSM-E0499 | 是。L1566 异议期七类+积分互不包含 |
| L133,L162 | BSM「商户管理·主要功能」 | exact BSM-E0399 | 是。L1315-1320 资料/订单/差异/异常/授权功能 |
| L133,L162 | BSM「商户专区自助」 | body BSM-E0406 | 是。L1322「商户专区自助：我的账单…工单…对账…合同…费用通知」 |
| L162 | BSM「商户管理：商户在授权范围内完整使用自己的系统」 | body BSM-E0396 | 是。L1308 h2 同文 |
| L162 | BSM「持股分红」 | exact BSM-E0601 | 是。节标题 |
| L163 | PGM「商户侧：在授权业务范围内全开放」 | body PGM-E0213 | 是。节标题 body |
| L163 | PGM「受控的十一类终端」 | body PGM-E0210（注册表名仍「受控的十类终端」） | 是。body 列十一类含商户网页门户/手机应用 |
| L163 | PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」 | exact PGM-E0190 | 是。L680 脱敏封闭清单 |
| L163 | PGM「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定」 | exact PGM-E0191 | 是。L681 含密码、暗纹、敏感页不可选中复制、打开留痕 |
| L163 | PGM「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」 | exact PGM-E0132 | 是。证照预警/宽限/阻断 |
| L164 | ENUM「终端十一端」 | body ENUM-E0117（注册表名「终端十端」） | 是。十一类终端名单 |
| L164 | ENUM「租金/账单类型」 | exact ENUM-E0134 | 是 |
| L164 | ENUM「商户服务工单状态」 | exact ENUM-E0140 | 是 |
| L164 | ENUM「退租结算状态」 | exact ENUM-E0132 | 是 |

未出现 `BSM L` / `HTML L` / `实测 L`。未换成新行号。

### 5.3 按 ID 核最小处置

**001** after L126 含默认 page/size、按商户取全量后切片、total＝本商户真实总数、无范围 403 `MERCHANT_SCOPE_REQUIRED`、不提供平台全量。通过。

**002** L95/L133 句意（七类异议期；资料/订单履约/差异/异常/授权业务功能/自助入口）未改，只换条目名。通过。

**003** §11 每个「名」前均有代号；替换名与定稿表逐字相同；门禁当场 PASS。通过。

**004** 两行权限列只留 `view`；能力列括注 create/dispute，措辞与定稿一致。通过。

**005** 原句保留；句末补差距；§9 增一条，括注覆盖不可选中复制、另存打印、打开留痕。通过。

### 5.4 禁区：HTML / §10 / 行号 / 他文件 / 表列

- 三份需求 HTML：两树 diff 无名。未删 HTML 要求（L29 原句、§10「页面安全」「导出」仍要暗纹）。
- §10：before/after 全文逐字相同（14 行表，每行 cells=2 / pipes=3）。H04 仅上下文扫到标题。
- 新行号：after 对 `BSM\|PGM\|ENUM\|HTML L\d` 与 `实测 L\d` 0 命中。
- 其他文件：`git diff --name-only` 仅 PRD。
- §8 表：4 列完好（见 5.1）。§3 能力表不在 diff 内。
- 业务发明：无新权限、无新接口、无新验收条、无把差距写成永久禁止（§9 标题仍是「当前未实现，不等于业务禁止」）。

### 5.5 类型 4 当场复跑（verification-before-completion）

修前（冻结 red 日志，本席未改 before 树、采信 `/home/james/audit-tmp/m024_b3r1_red.log`）：problems=10，exit 1。十处＝L95 行号、L118/119 路由混写、L126 平台全量、L133 行号、L161「主要功能」hits=15、PGM 三名 hits=0、§9 无暗纹。与 001–005 一一对应。

修后本席当场：
```
python3 /home/james/audit-tmp/m024_anchor_check.py
→ GAP-LISTED §9 暗纹: yes
  refs=16 problems=0
  PY_EXIT=0
```
与 `/home/james/audit-tmp/m024_b3r1_green.log` 同文。

```
node tools/verify-entry-anchors.mjs
→ 注册表 1228；账本 1235；承接矩阵 1244；镜像列 1565；Markdown 426；引用 1561（原名 1389，正文 172）
  [entry-anchors] PASS
  NODE_EXIT=0
```
与 `/home/james/audit-tmp/m024_b3r1_gate.log` 同文。

执行日志 `/home/james/audit-tmp/m024_b3r1_run.log`：BEFORE/AFTER tree、diff sha、1 file 12+/11-、hunks=5、after 内容 SHA `95ac24eba2…` 与本席实算一致。本席不把该日志当唯一证据，只作交叉。

**内容票：PASS。**

---

## 6. ponytail-review（只读）

无额外可删抽象。§11 PGM 长名是 003 指定的唯一可定位名，不是膨胀。  
`net: -0 lines possible.`  
Lean already. Ship.

---

## 7. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | PASS | 12+/11- 及版本行均可归 M024-001…005；无无 ID 行 |
| 内容 | PASS | 五条最小处置落地；与 Controller/Service/路由/WO_CREATE/DISPUTE_PERMS/HTML 一致；检查② 16 处 n=1 且承载；§10 未改；表列完好；他文件 0 |
| 本席 B4-r1 | PASS | 两轴均 PASS；任一 FAIL 即 FAIL，此处未触发 |

本票不是模块 PASS，不是授权改归属。B4 双 PASS 仍须席甲本次完整 raw 合取。本席未读 self-raw。

步骤：B4-r1。模块 M024。B3 次数＝本轮审的 r1。未写 findings/ledger。
