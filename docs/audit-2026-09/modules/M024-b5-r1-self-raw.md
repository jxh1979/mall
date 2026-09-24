CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M024 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；文档现行 V0.14 的改动为席甲 B3 所写，以 Grok 票为准）。2026-09-20T21:57:33Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `34866c4fbb427a498f7f0bffa4c47d71792a6d1f`。
- 模块 M024 1 文档（§1 全读），19 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M024-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件。
- 本轮补的方法（B1 漏了 GR-05）：按文首 L4 自设规则，把文中每一条写成「现有/都带/已」语气的能力逐条去源码查是否真实现，见 §3 对抗线。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/merchant-portal-zone-prd-v0.1.md | 165 | 17901 | 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc | 全读（L1-165） |

## 2. 可执行证据原文（/home/james/audit-tmp/m024_b5r1_run.log；B1 同一取证脚本修后重跑）

```
START=2026-09-20T21:56:59Z HEAD=e456ba96 origin/main=0c04a5e2
== 0. file (lines/bytes/sha256)
docs/merchant-portal-zone-prd-v0.1.md 165 17901 95ac24eba23cb4d2a843c419148394282ab46626f8ba676b480dc235b4c974fc
== 1. vs HEAD / vs origin/main / local unmerged commits
 1 file changed, 12 insertions(+), 11 deletions(-)
 1 file changed, 12 insertions(+), 11 deletions(-)
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
END=2026-09-20T21:56:59Z
```

说明：探针 10 里 `sed -n 1562p` 与 `1311,1318p` 两段是 B1 为查行号缺陷所写，修后文档已不再引用行号，输出仅作对照。

## 2b. 条目名定位/行号/过时句/权限列/§9 差距 检查 + 条目锚点门禁（/home/james/audit-tmp/m024_b5r1_check.log）

```
GAP-LISTED §9 暗纹: yes
refs=16 problems=0
CHECK_EXIT=0
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账
GATE_EXIT=0
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后两条未经验证的前提都已落地——§8 L126 写的是现行行为（无商户范围 403、按商户切片、total 真实）；文中所有 HTML 出处都是可唯一定位的条目名，无行号。
- 顺从式线：19 判据逐项——B1 不通过的 GEN-002/008/016 由 001–005 闭合；BSM-114、GEN-001/004/005/010/011/014/017/018 同 B1 通过；GEN-003/006/007/009/012/013/015 无本面或登记。
- 对抗式线（grilling）：研发照 §8 调接口→平台账号 403，与文一致；照 §8 表配路由权限→与 `adminRoutes.ts` 逐字一致（§2 探针 2 对照 + B3 执行日志六个 OK）；读者按 §11 每个名字去 HTML 搜→§2b 全部 hits=1；验收照 §10 逐行做会不会把未实现当已实现→逐条核：终端（§1 已标手机应用未落地）、工单保洁入口（§4 L70 已标）、费用总表/综合账单（§3.2、§9 已标）、证照页（§5 L82、§9 已标）、导出（§7 L111、§9 已标）、暗纹/敏感页（L29、§9 已标，005）、通知开关冲突（L55 已登记）、合同日期时区（L82 已标 `LocalZone` 固定 Asia/Shanghai）、第五态文案（L69 已标）；差距声明有没有已被后续代码补上→M023 恢复的留言入口对应 L53/L119「异议留言」本就写为已承载，仍准；「我的合同」关键条款仍无字段（L54 差距仍真）；无商户侧证照页（探针：adminRoutes 无）；M020/M023 是否还让别处过时→全文再无「平台全量」「只读最小披露」以外的接口行为句，L54「当前只读最小披露」仍准。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–005 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1/B2 登记项（条目锚点门禁两处盲区、UI 矩阵 UI-ADM-MER-007 备注、专区工单页第五态文案、前端超 500 条丢行等，均属其他模块）。

## 7. 结论

席甲 B5-r1 票：**PASS**。1 文档全读、19 判据逐项、取证脚本与锚点检查/门禁复核；未改任何文件；落盘前未读 Grok 本次正文。
