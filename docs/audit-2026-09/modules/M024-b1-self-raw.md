CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T21:20:51Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`（落后 origin/main 0c04a5e2，审计对象为主工作区现状文件；本文件与 HEAD、与 origin/main 均无差异——探针 1 两条 diff --stat 为空）。
- 模块 M024「商户档案、商户类型与商户侧门户／文档与接口定义／第1段」1 文档，19 判据（BSM-114、GEN-001…018）。B3 累计 0。
- 顺序：Grok 已先派（modules/M024-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/merchant-portal-zone-prd-v0.1.md | 0 | 17116 | 3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f | 全读（L1-164） |

## 2. 可执行证据原文（/home/james/audit-tmp/m024_b1_run.log）

```
START=2026-09-20T21:15:25Z HEAD=e456ba96 origin/main=0c04a5e2
== 0. file (lines/bytes/sha256)
docs/merchant-portal-zone-prd-v0.1.md 164 17116 3efd4600cfde1f2b64e2a9747cd8f7b713a0f70e8a77aa759328980f5629f99f
== 1. vs HEAD / vs origin/main / local unmerged commits
44d59ddf docs: 收口 M05 商户合同空间文档出处对齐
32b63708 docs(authority): 同步 R13 五项用户裁决 (#370)
3bff6692 docs(r9): 锚点制度换代——废行号立条目名（DEC-R8-17 落地）+第三批 HTML 修订（DEC-R8-35~38）+非代码遗留清零
== 2. routes ADM-MER-002..008 (doc L15, L115-122)
routes file: apps/web/src/admin/adminRoutes.ts
48:  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.service_work_order.view", "merchant.billing.view"] },
89:  { pageId: "ADM-MER-003", label: "我的账单", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/bills", requiredPermissions: ["merchant.billing.view"], accountForms: ["merchant"] },
90:  { pageId: "ADM-MER-004", label: "我的工单", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/work-orders", requiredPermissions: ["merchant.service_work_order.view"], accountForms: ["merchant"] },
91:  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms: ["merchant"] },
92:  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
93:  { pageId: "ADM-MER-007", label: "费用通知", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/fee-recipients", requiredPermissions: ["merchant.portal.fee_recipient.manage"], accountForms: ["merchant"] },
94:  { pageId: "ADM-MER-008", label: "我的分红", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/dividends", requiredPermissions: ["finance.dividend.view"], accountForms: ["merchant"] },
== 3. merchant-portal controller/service (doc L126: true paging, default page=1/size=500, platform sees all with real total)
9:import org.springframework.web.bind.annotation.GetMapping;
11:import org.springframework.web.bind.annotation.PostMapping;
13:import org.springframework.web.bind.annotation.RequestMapping;
14:import org.springframework.web.bind.annotation.RequestParam;
23:@RequestMapping("/api/v1/merchant-portal")
32:    @GetMapping("/contracts")
34:            @RequestParam(name = "page", defaultValue = "1") int page,
35:            @RequestParam(name = "size", defaultValue = "500") int size) {
39:    @GetMapping("/settlements")
41:            @RequestParam(name = "page", defaultValue = "1") int page,
42:            @RequestParam(name = "size", defaultValue = "500") int size) {
46:    @PostMapping("/settlements/{settlement_id}/dispute")
24: * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域
61:            String merchantId = requireMerchantScope();
64:                    PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size);
92:            String merchantId = requireMerchantScope();
93:            // 商户自 scope: listSettlementsByMerchant(谓词隔离、无 LIMIT)内存切片。
95:                    PageResponse.of(contractRepository.listSettlementsByMerchant(merchantId), page, size);
140:        if (!requireMerchantScope().equals(contract.merchantId())) {
172:    private String requireMerchantScope() {
174:                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
== 4. acceptance matrix rows UI-ADM-MER-003..008 (doc L124)
6
      2 UI-ADM-MER-001;      1 UI-ADM-MER-002;      1 UI-ADM-MER-003;      1 UI-ADM-MER-004;      1 UI-ADM-MER-005;      1 UI-ADM-MER-006;      1 UI-ADM-MER-007;      1 UI-ADM-MER-008;
== 5. migrations cited (doc L52, L69-70, L82, L164, L55)
V022: V022__p1_merchant_service_work_orders.sql 
V028: V028__p1_contract_compliance_expiry.sql 
V166: V166__facility_work_orders.sql 
V177: V177__sms_notification_scaffold.sql 
V197: V197__terminal_master_switches.sql 
V199: V199__merchant_portal_zone_module_switch.sql 
V200: V200__merchant_portal_zone_permissions.sql 
V339: V339__multi_party_contact_phone_pii.sql 
== 6. LocalZone (doc L82)
backend/src/main/java/com/complex/platform/common/LocalZone.java:19:    public static final ZoneId ID = ZoneId.of("Asia/Shanghai");
== 7. fee recipient phone (doc L128: AES-GCM envelope; masked without pii.contact_phone.view; SMS chain decrypts in repository)
backend/src/main/java/com/complex/platform/common/PhoneMask.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientController.java
backend/src/main/java/com/complex/platform/inventory/InventorySupplierRecord.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportRecord.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportController.java
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
backend/src/main/java/com/complex/platform/governance/PlatformController.java
backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java:4:import com.complex.platform.common.crypto.PiiCipher;
backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java:15: * 自 V399 起列为 text、只存 PiiCipher 的 enc:v1: 信封（库上 CHECK 守新写入），读回
backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java:22:    private final PiiCipher piiCipher;
backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java:24:    JdbcIdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyResponseBodyPiiBackfill.java:3:import com.complex.platform.common.crypto.PiiCipher;
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyResponseBodyPiiBackfill.java:22:    private final PiiCipher piiCipher;
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyResponseBodyPiiBackfill.java:24:    IdempotencyResponseBodyPiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCi
backend/src/main/java/com/complex/platform/common/crypto/PiiCipher.java:18:public class PiiCipher {
== 8. notification switches (doc L55)
resolver: backend/src/main/java/com/complex/platform/notification/NotificationSwitchResolver.java
12:    static final String MASTER_SWITCH = "sms_notification";
8:    ('tcs_sms_notification_off', '*', 'sms_notification', 'global', 'global', FALSE,
11:     'SMS notification domain switch for stored wine lifecycle notices; gated by sms_notification master switch', now(), now()),
13:     'SMS notification domain switch for stored value balance notices; gated by sms_notification master switch', now(), now()),
15:     'SMS notification domain switch for merchant fee billing notices; gated by sms_notification master switch', now(), now()),
backend/src/main/java/com/complex/platform/notification/NotificationService.java
== 9. merchant service work orders (doc L52, L69): V022 type/status closed sets now; zone page label for resolved
22:    CONSTRAINT chk_merchant_service_work_order_type CHECK (work_order_type IN ('repair', 'complaint', 'rectification', 'service')),
23:    CONSTRAINT chk_merchant_service_work_order_priority CHECK (priority IN ('low', 'medium', 'high', 'critical')),
24:    CONSTRAINT chk_merchant_service_work_order_status CHECK (status IN ('created', 'assigned', 'processing', 'pending_merchant', 'resolved', 'closed', 'cancelled')),
25:    CONSTRAINT chk_merchant_service_satisfaction_score CHECK (satisfaction_score IS NULL OR satisfaction_score BETWEEN 1 AND 5)
49:    CONSTRAINT chk_merchant_service_work_order_action_type CHECK (action_type IN ('created', 'assigned', 'commented', 'status_changed', 'closed'))
72: WHERE permission_code IN (
apps/web/src/admin/pages/MerchantZoneWorkOrdersPage.tsx:61:  resolved: "已解决",
apps/web/src/customer/i18n/statusLabels.ts:116:  resolved: "已解决",
264:    <tr><td class="k">商户服务工单状态</td><td>已提交、已派单、处理中、待商户确认、已处理待关闭、已关闭、已取消（商户专区发起；超过时限自动升级并标记）</td></tr>
== 10. HTML line anchors cited in doc (L95 BSM L1562; L133 BSM L1311-L1318; §11 headings)
            <div class="info-block">
          <div class="detail-grid">
            <div class="info-block">
              <h3>主要功能</h3>
              <ul>
                <li>查看本商户基础资料、联系人、空间和合同摘要。</li>
                <li>查看本商户订单、核销、退款或第三方回流结果。</li>
                <li>查看与平台口径不一致的对账差异。</li>
                <li>查看周期内可结算结果。</li>
BSM[商户管理：商户在授权范围内完整使用自己的系统]=1
BSM[商户专区自助]=2
BSM[持股分红]=4
PGM[商户侧：在授权业务范围内全开放]=1
PGM[终端十一端]=0
PGM[导出与暗纹]=0
PGM[证照与合同关键日期]=0
ENUM[终端十一端]=1
ENUM[租金/账单类型]=1
ENUM[商户服务工单状态]=1
ENUM[退租结算状态]=1
== 11. settlement page now (doc L39, L53, L119: 我的对账与异议留言) and contracts page (L54, L120)
1
32:    @GetMapping("/contracts")
39:    @GetMapping("/settlements")
== 12. permissions cited in doc table exist in migrations
merchant.billing.view: 2
merchant.service_work_order.view: 2
merchant.service_work_order.create: 2
merchant.portal.settlement.view: 2
merchant.portal.settlement.dispute: 2
contract.view: 2
merchant.portal.fee_recipient.manage: 2
finance.dividend.view: 1
== 13. relative paths cited in doc exist
OK docs/full-admin-ui-acceptance-matrix-v0.1.md
== 14. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T21:15:26Z
```

补充只读核对（命令在本席会话内执行，结果摘录）：
- `node tools/verify-entry-anchors.mjs` → `[entry-anchors] PASS`（被检 Markdown 426 份）。门禁检查② 只匹配 `文件代号「名」` 紧邻的名字。
- 用门禁同一定位规则（等于注册表条目名，或恰为一条条目正文的子串）逐个实查 §11 三行 12 个名字（`docs/prd-audit/entry-registry.csv` 1228 行）：
  - L161 BSM「商户管理：商户在授权范围内完整使用自己的系统」正文子串 1 条 ✓；「主要功能」正文子串 **15 条**（不唯一）✗；「商户专区自助」正文子串 1 条 ✓；「持股分红」条目名 1 条 ✓。
  - L162 PGM「商户侧：在授权业务范围内全开放」1 条 ✓；「终端十一端」**0**、「导出与暗纹」**0**、「证照与合同关键日期」**0** ✗。
  - L163 ENUM 四个名字各 1 条 ✓。
  - 可定位的替代名（实查各 1 条）：BSM「商户管理·主要功能」；PGM「受控的十一类终端」（正文子串；注册表冻结名为「受控的十类终端」）、PGM「导出文件是否脱敏，不由导出人判断，也不提供关闭开关」、PGM「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定」（暗纹与敏感页句在此条）、PGM「管理证照、资质、保险和合同关键日期的到期预警与到期阻断」。
- 行号：`sed -n 1562p` BSM ＝ `<div class="info-block">`；异议期原句在 L1566（注册表 BSM-E0499「给商户」）。`sed -n 1311,1318p` ＝ detail-grid/info-block/`<h3>主要功能</h3>`/`<ul>` + 前 4 条 li；六条主要功能实为 L1315-L1320，「商户专区自助」句在 L1322——文中 L133 列举的「异常、授权业务功能、自助事项」都落在所写区间之外。
- 通知开关：`NotificationSwitchResolver.resolve` = 总闸 `isSwitchEnabled("sms_notification", false)` → 域 `sms.notify.<domain>` 默认 true → 分项默认 true；`MerchantFeeNotificationRecipientService.FEE_EVENTS = (rent, utilities, property, renewal)`。与 L55 一致。

## 3. 三线审计

- 第一性原理线。用途：把三份 HTML 的商户侧口径与「当前实现/差距」并排写给产品与研发，且被承接矩阵引用。实际行为：读者按 §8、§11 去找接口行为与 HTML 出处。未经验证的前提：①「文中写的当前实现仍是现行实现」——L126 在 M020-001 之后已不成立；②「文中给的出处能把人带到原文」——两处行号、四个条目名带不到。
- 顺从式线：19 判据逐项见 §4；全文 164 行逐段对过（§1 终端、§2 边界、§3 能力表、§4 工单、§5 证照、§6 异议、§7 导出、§8 一期实现、§9 未实现、§10 验收、§11 依据）。
- 对抗式线（grilling）：平台账号照 L126 去调 `GET /merchant-portal/contracts` 会怎样→403 MERCHANT_SCOPE_REQUIRED，不是「全量 + 真实 total」；新同事照 L95 打开 BSM 第 1562 行会看到什么→一个 div；照 §11 去 PGM 搜「导出与暗纹」→0 命中；门禁为什么没红→只检紧邻代号的第一个名字、行号检查只认「实测 LNNN」句式；HTML 转述有没有缩小/放大→逐句对过，未见；「差距」有没有被写成禁令→§7 L111、§9 L138 明确写了不构成禁令，合规；M023 恢复留言后 L39/L53/L119「异议留言」是否仍准→准。

## 4. 19 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BSM-114 | 适用｜「各类入口」表：商户侧页面＝在授权业务范围内全开放、不看平台全局；商户 App＝商户侧手机端。文 §1 L8-19 明列两类终端、如实标「商户手机应用尚未落地」为差距、不据此缩小 HTML 能力；§9 L132 同。与原文一致｜通过 |
| GEN-001 必要性与最小性 | 适用｜本文是现行领域 PRD，被 `html-prd-coverage.csv` PGM-0115…0118 等行引用为承接文档，有存在理由；无多余章节｜通过 |
| GEN-002 可执行正确性 | 适用｜文中「当前实现」声称须与现行代码一致：§8 L126「平台全量取真实 total」已不成立（CX-M024-001）｜不通过 |
| GEN-003 测试有效性 | 无本面（纯文档，无测试）；文档侧的「可失败检查」由条目锚点门禁承担——本次实跑 `node tools/verify-entry-anchors.mjs` PASS，但门禁只检 `代号「名」` 紧邻的第一个名字，§11 同一行后续名字漏检（见 CX-M024-003）｜登记 |
| GEN-004 信任边界校验 | 适用｜§2 L24 写跨商户读写拒绝并留痕；现行 `MerchantPortalService.requireMerchantScope` fail-closed + recordFailure（M020）一致｜通过 |
| GEN-005 授权与最小权限 | 适用｜§8 六行权限码逐个在迁移中存在（探针 12）；路由 `requiredPermissions` 与表一致（探针 2；004/005 表内 `/create`、`/dispute` 为按钮级权限，页面内 PermissionButton 门控）｜通过 |
| GEN-006 数据完整性 | 无本面（文档不落库） |
| GEN-007 并发与幂等 | 无本面 |
| GEN-008 接口与模式一致性 | 适用｜§8 L126 对接口行为的描述与现行 Controller/Service 不一致（CX-M024-001）；其余路由、默认 page/size、事件集 rent/utilities/property/renewal、七态代码值与源码一致｜不通过 |
| GEN-009 错误处理与恢复 | 无本面 |
| GEN-010 安全与敏感信息 | 适用｜§8 L128 手机号 AES-GCM 信封（`PiiCipher` `AES/GCM/NoPadding`、`enc:v1:`）、无 `pii.contact_phone.view` 看掩码（Controller `maskPhone…ForUnauthorized`）、仓储读时 decrypt——逐条复现｜通过 |
| GEN-011 可观测与审计证据 | 适用｜§2/§7 留痕要求为 HTML 转述（PGM「所有导出的文件一律带密码…」条），与原文一致｜通过 |
| GEN-012 资源与性能边界 | 无本面 |
| GEN-013 构建配置与依赖 | 无本面 |
| GEN-014 数据库迁移正确性 | 适用（引用面）｜文中 V022/V028/V166/V177/V197/V199/V200/V339 八个迁移文件全部存在（探针 5）；V022 类型/状态闭集与 §4 L69 所列代码值一致（探针 9）｜通过 |
| GEN-015 界面可用性与可访问性 | 无本面（文档）；§4 L69 如实登记了页面「已解决」与 HTML「已处理待关闭」未对齐的差距（属 MerchantZoneWorkOrdersPage 所在模块，登记） |
| GEN-016 文档可验证性 | 适用｜核心判据。行号锚点两处失效且违反 DEC-R8-17（CX-M024-002）；§11 四个条目名无法在所指 HTML 内唯一定位（CX-M024-003）；L126 实现声称过时（CX-M024-001）。其余可复现：UI 矩阵 UI-ADM-MER-003…008 六行在、`LocalZone.ID`=Asia/Shanghai、`accountForms: ["merchant"]`、总闸缺行默认关（`isSwitchEnabled(MASTER_SWITCH, false)`）、V177 出厂关、域/分项开关可关、`skipped_switch_off` 存在、账单页有账龄与费用构成、无商户侧证照页｜不通过 |
| GEN-017 业务来源分类 | 适用｜三条发现均为细化（文档对现行实现/锚点制度的下位表述），非添加；文中 HTML 转述逐句对过原文（BSM「商户提出的投诉与异议…」「给商户」、PGM「管理证照…」「导出文件是否脱敏…」「所有导出的文件一律带密码…」、PGM 酒吧商户异议二十个工作日、ENUM「租金/账单类型」营业额抽成异议期、BSM 退租等待期三十日/最后一个审批完成日），未见缩小、放大或发明｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论全部落到 HTML 原文、只读命令输出（m024_b1_run.log）与注册表实查，不引用历史 PASS｜通过 |

## 5. 本次 Findings（临时编号）

### CX-M024-001 §8 L126 对商户门户接口的「当前实现」声称已过时
- 原文 L126：「`GET /api/v1/merchant-portal/contracts` 与 `GET /api/v1/merchant-portal/settlements` 使用真分页，默认 `page=1/size=500`，平台全量取真实 total；商户 scope 只见本商户。」
- 事实：`MerchantPortalService` 现行 V0.01（M020-001，PR #461 已合并）两方法首行 `requireMerchantScope()`，无商户范围即 403 `MERCHANT_SCOPE_REQUIRED`，平台全量分支已删除（探针 3：无 `orElseGet`/`findAll`）；分页是 `PageResponse.of(listXxxByMerchant(merchantId), page, size)`——按商户取全量后内存切片，total 为该商户真实总数。默认 `page=1/size=500` 仍成立。
- 正常触发：研发/测试照本文理解「平台账号可全量查看」，第一次调用即 403。§10 L157「实现诚实」自设的通过条件（分页…与当前代码一致）不满足。
- 四分类：冲突？HTML 对该接口沉默，非冲突 → **细化**；工程缺陷：成立。判据 GEN-016 / GEN-002 / GEN-008。依据类型 4（只读命令输出）。
- 最小拟处置：改写该句为现行事实（无商户范围的账号一律拒绝；商户只见本商户；按商户取全量后切片、total 为真实总数；默认 page=1/size=500），不加新规则。

### CX-M024-002 两处 HTML 行号锚点失效且违反「废行号、立条目名」
- 原文 L95「BSM L1562 所定的异议期…」；L133「HTML L1311-L1318 的资料、订单履约、差异、异常、授权业务功能和自助事项统一入口。」
- 事实：见 §2 行号核对——L1562 是 div，原句在 L1566；L1311-L1318 不含所列后三项。DEC-R8-17（`tools/verify-entry-anchors.mjs` 头注：「三份业务最终解释权 HTML 的引用一律用『条目名』，不得再用行号」）已废行号；门禁检查③ 只认「实测 LNNN」句式，所以没红。
- 正常触发：读者第一次按行号查原文即落空。四分类：**细化**；工程缺陷：成立。判据 GEN-016。依据类型 1+4。
- 最小拟处置：L95 改 BSM「给商户」；L133 改 BSM「商户管理·主要功能」与 BSM「商户专区自助」。只换锚点，不动句意。

### CX-M024-003 §11 依据里四个条目名无法在所指 HTML 内唯一定位
- 原文 L161 BSM…「主要功能」…；L162 PGM…「终端十一端」「导出与暗纹」「证照与合同关键日期」。
- 事实：见 §2 注册表实查——「主要功能」15 条不唯一；PGM 三个名字 0 命中（「终端十一端」只在 ENUM）。门禁只检每行紧邻代号的第一个名字，其余漏检。
- 正常触发：按名字去 PGM 搜 → 0 结果。四分类：**细化**；工程缺陷：成立。判据 GEN-016。依据类型 4。
- 最小拟处置：每个名字都写成 `代号「名」` 形式并换成可唯一定位的名字（替代名见 §2）；不改其余依据。同批把文首版本 V0.13→V0.14、最后更新日改为修复当日。

## 6. 观察（登记，不立项）

- OBS-1 §4 L69 如实写了专区页面把 `resolved` 显示「已解决」、HTML 为「已处理待关闭」——文档诚实，差距本体属 `MerchantZoneWorkOrdersPage` 所在模块。
- OBS-2 条目锚点门禁的两个盲区（只检紧邻第一个名字；行号只认「实测 LNNN」句式）属 `tools/verify-entry-anchors.mjs` 所在模块，跨模块观察。
- OBS-3 注册表冻结名「受控的十类终端」「终端十端」与 HTML 现文「十一类/十一端」不同名——按 DEC-R8-17 条目名冻结是设计如此，不是缺陷。
- OBS-4 §3.2 L54「我的合同」产品口径列比 BSM 自助句多列「空间、状态」；该列标题是「产品口径」且 BSM「商户侧页面」与合同枚举支撑，不构成放大。
- OBS-5 M023 登记项（后端合同第 9 态 amended 无中文名）与本文无直接声称，不并入。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M024-001…003，均细化、工程缺陷成立）。1 文件全读、19 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。


---
## 席甲更正（V0.02，2026-09-20T21:30:28Z；不新增独占锁行，本段属上方 V0.01 报告的追加；落盘时未读 Grok 本次正文，Grok 仍在运行）

- §1 文件覆盖表「行」列误写为 0：生成脚本统计换行符时把转义写错，属本席笔误。实际 164 行（§2 探针 0 原文 `docs/merchant-portal-zone-prd-v0.1.md 164 17116 3efd4600…`，wc -l）。bytes、SHA256、覆盖范围（L1-164 全读）与全部结论不变。


## M024 B2-r1 整批候选复核（席甲票，V0.03）

- 时间：2026-09-20T21:33:44Z。输入：modules/M024-b1-grok-raw.md V0.01（33051 bytes，SHA256 d49a241c601340000ba7056bf68d9058131b97f3f3e321161387888f6be9b223）、modules/M024-b1-self-raw.md V0.01+V0.02（上方本报告）；候选 findings.md 顶部「当前 M024 B2-r1 整批候选」段（全文件 SHA256 0e791fa4f4e2919b64f25760b8d0605248bfcb4fe0f48d3fc4a425fc34263e6d）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01…05 与席甲 CX-001…003 全部归并（见候选「来源归并」列）；两席观察全部登记；无漏并、无改写。
- 自审纠正两处：①GR-02（§8 权限列）席甲 B1 在 GEN-005 行判「通过」，理由是 create/dispute 属按钮级权限——但该列拿去对 `adminRoutes.ts` 确实对不上，文档可验证性不成立，撤回，采 Grok。②GR-05（L29 暗纹/敏感页未标差距）席甲漏：只核了「HTML 转述与原文一致」，没按文首 L4 自设规则去查该项是否已实现；复核 apps/web/src 与 backend/src/main 0 命中，成立，采 Grok。
- 002 只采条目名一支（DEC-R8-17 已废行号）；三个名字席甲按门禁同一规则实查各 1 条。003 替代名逐个实查各 1 条且承载所引内容（终端清单句、脱敏例外句、密码与暗纹/敏感页句、证照预警与宽限句）。
- 处置全部只改本模块 1 文件的措辞/锚点/差距标注与文首版本行；不改代码、不改 HTML、不改门禁、不发明业务规则。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
