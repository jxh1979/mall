CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T21:46:18Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M024 B3-r1 修复 diff：before tree `7f7c404c89acc2ed2c77befadc115bb19097f7fd` → after tree `34866c4fbb427a498f7f0bffa4c47d71792a6d1f`；冻结 diff `/home/james/audit-tmp/m024_b3_r1.diff`（7238 bytes，SHA256 e302e64e5fed61286e94a1a3440ffa507604a7861ea83784c4402e9351335f28），全文见 §2。定稿 ID：M024-001…005（队列「是」）。
- 顺序：Grok 已先派（modules/M024-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3；文档 diff 行少，按 ± 行逐条归）

| ± | 归因 ID | 行首 70 字 |
|---|---|---|
| `-` | 版本行(001–005同批) | # 商户侧与商户专区产品口径 PRD V0.13 |
| `+` | 版本行(001–005同批) | # 商户侧与商户专区产品口径 PRD V0.14 |
| `-` | 版本行(001–005同批) | > 版本：V0.13 ｜ 最后更新：2026-08-31 ｜ 状态：现行 PRD |
| `+` | 版本行(001–005同批) | > 版本：V0.14 ｜ 最后更新：2026-09-20 ｜ 状态：现行 PRD |
| `-` | 005 | - 后台、商户专区和员工手机端的在线页面都带可反向推算账号、设备和打开时间的暗纹。敏感页文字不可选中、不可复制，不提供另存和打印；打开敏感 |
| `+` | 005 | - 后台、商户专区和员工手机端的在线页面都带可反向推算账号、设备和打开时间的暗纹。敏感页文字不可选中、不可复制，不提供另存和打印；打开敏感 |
| `-` | 002 | - BSM L1562 所定的异议期只适用于房租、水电、物业、结算金额出账与调整、押金、违约金、分红七类非积分商户账单；积分结算账单的账期 |
| `+` | 002/003 | - BSM「给商户」所定的异议期只适用于房租、水电、物业、结算金额出账与调整、押金、违约金、分红七类非积分商户账单；积分结算账单的账期与异 |
| `-` | 004 | ¦ ADM-MER-004 ¦ `/admin/merchant-zone/work-orders` ¦ `merchant.servic |
| `-` | 004 | ¦ ADM-MER-005 ¦ `/admin/merchant-zone/settlements` ¦ `merchant.portal |
| `+` | 004 | ¦ ADM-MER-004 ¦ `/admin/merchant-zone/work-orders` ¦ `merchant.servic |
| `+` | 004 | ¦ ADM-MER-005 ¦ `/admin/merchant-zone/settlements` ¦ `merchant.portal |
| `-` | 001 | `GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-porta |
| `+` | 001 | `GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-porta |
| `-` | 002 | - HTML L1311-L1318 的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。 |
| `+` | 002/003 | - BSM「商户管理·主要功能」与 BSM「商户专区自助」的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。 |
| `+` | 005 | - 页面暗纹与敏感页防护（文字不可选中与复制、不提供另存和打印、打开留痕）。 |
| `-` | 003 | - BSM「商户管理：商户在授权范围内完整使用自己的系统」「主要功能」「商户专区自助」「持股分红」 |
| `-` | 003/005 | - PGM「商户侧：在授权业务范围内全开放」「终端十一端」「导出与暗纹」「证照与合同关键日期」 |
| `-` | 003 | - ENUM「终端十一端」「租金/账单类型」「商户服务工单状态」「退租结算状态」 |
| `+` | 003 | - BSM「商户管理：商户在授权范围内完整使用自己的系统」、BSM「商户管理·主要功能」、BSM「商户专区自助」、BSM「持股分红」 |
| `+` | 003 | - PGM「商户侧：在授权业务范围内全开放」、PGM「受控的十一类终端」、PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」、P |
| `+` | 003 | - ENUM「终端十一端」、ENUM「租金/账单类型」、ENUM「商户服务工单状态」、ENUM「退租结算状态」 |

- 1 文件 5 个 hunk、23 条 ± 行全部命中 001–005 或随批版本行（B2 双席同意同批升版）；无归因不到的行；未改其他文件。归因票：**PASS**。

## 2. diff 全文

```diff
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

## 3. 内容审计

- 001：新句三点逐一对现行源码——默认 `page=1/size=500`（Controller L34-35/41-42）；`PageResponse.of(listXxxByMerchant(merchantId), page, size)` 取全量后切片、total 为本商户真实总数；`requireMerchantScope()` 无范围即 403 `MERCHANT_SCOPE_REQUIRED`。未新增规则。
- 002：L95 → BSM「给商户」（注册表 BSM-E0499，原句所在条）；L133 → BSM「商户管理·主要功能」与 BSM「商户专区自助」（六条主要功能 + 自助句，正好覆盖原句列举的六类）。句意未动。
- 003：§11 三行 13 个名字全部带代号；修后检查逐个唯一定位（见下 GREEN）；替代名承载内容：终端清单句 / 脱敏与「本商户客户信息不脱敏」例外句 / 密码、暗纹与敏感页句 / 证照预警与宽限句。
- 004：§8 六行「权限」列现与 `adminRoutes.ts` requiredPermissions 逐字一致（执行日志 routes vs doc table 六个 OK）；页内权限码与 `WO_CREATE`、`DISPUTE_PERMS` 一致；表格每行仍 4 格。
- 005：L29 HTML 转述原句保留，句末补差距指向；§9 增一条；§10 验收条件未动；未删任何 HTML 要求。
- 修前/修后检查原文：
```
[RED]
LINE-ANCHOR L95 BSM L1562
ROUTE-PERM-MIX L118
ROUTE-PERM-MIX L119
STALE-IMPL L126 平台全量取真实 total
LINE-ANCHOR L133 HTML L1311-L1318
UNLOCATABLE L161 BSM「主要功能」 hits=15
UNLOCATABLE L162 PGM「终端十一端」 hits=0
UNLOCATABLE L162 PGM「导出与暗纹」 hits=0
UNLOCATABLE L162 PGM「证照与合同关键日期」 hits=0
GAP-MISSING §9 未列暗纹/敏感页差距
refs=12 problems=10
[GREEN]
GAP-LISTED §9 暗纹: yes
refs=16 problems=0
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m024_b3r1_run.log）

```
START=2026-09-20T21:45:49Z HEAD=e456ba96
check OK 12 subs
BEFORE_TREE=7f7c404c89acc2ed2c77befadc115bb19097f7fd
== RED: check against old doc
RED_CHECK_EXIT=1
LINE-ANCHOR L95 BSM L1562
ROUTE-PERM-MIX L118
ROUTE-PERM-MIX L119
STALE-IMPL L126 平台全量取真实 total
LINE-ANCHOR L133 HTML L1311-L1318
UNLOCATABLE L161 BSM「主要功能」 hits=15
UNLOCATABLE L162 PGM「终端十一端」 hits=0
UNLOCATABLE L162 PGM「导出与暗纹」 hits=0
UNLOCATABLE L162 PGM「证照与合同关键日期」 hits=0
GAP-MISSING §9 未列暗纹/敏感页差距
refs=12 problems=10
== apply
wrote merchant-portal-zone-prd-v0.1.md 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc lines 165
== GREEN: same check + entry-anchor gate
GREEN_CHECK_EXIT=0
GAP-LISTED §9 暗纹: yes
refs=16 problems=0
GATE_EXIT=0
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 426 份；条目名引用 1561 处（注册表原名 1389 处，正文唯一定位 172 
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
== post-fix facts
routes vs doc table: 003=OK 004=OK 005=OK 006=OK 007=OK 008=OK 
table column counts (§8 rows must have 4 cells):  6 4;
version lines: # 商户侧与商户专区产品口径 PRD V0.14 > 版本：V0.14 ｜ 最后更新：2026-09-20 ｜ 状态：现行 PRD 
AFTER_TREE=34866c4fbb427a498f7f0bffa4c47d71792a6d1f
other files touched: 0
DIFF bytes=7238 sha256=e302e64e5fed61286e94a1a3440ffa507604a7861ea83784c4402e9351335f28
 docs/merchant-portal-zone-prd-v0.1.md | 23 ++++++++++++-----------
 1 file changed, 12 insertions(+), 11 deletions(-)
hunks=5 files=1
docs/merchant-portal-zone-prd-v0.1.md 165 17901 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T21:45:51Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
