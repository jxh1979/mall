CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T11:30:54Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `d063773a67499e1b19fb8d2a46b1ee0324a9acbf`。
- 模块 M020 4 文件（§1 全读），22 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M020-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件（测试实跑只读源码、写 target/）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java | 40 | 1348 | 3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8 | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java | 55 | 2587 | 536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java | 180 | 9761 | 64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0 | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java | 9 | 245 | c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m020_b5r1_run.log；B1 同一只读取证脚本修后重跑 + 测试复跑）

```
START=2026-09-20T11:30:46Z HEAD=e456ba96 origin/main=a29220db
== 0. four files (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java 40 1348 3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java 55 2587 536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java 180 9761 64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0
backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java 9 245 c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052
== 1. vs HEAD (expect empty) / porcelain / eaccb9c5 containment
 .../merchantportal/MerchantPortalController.java   |   8 +-
 .../merchantportal/MerchantPortalService.java      | 148 +++++++++++++--------
 2 files changed, 99 insertions(+), 57 deletions(-)
 M backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java
 M backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java
eaccb9c5 in HEAD: yes; in origin/main: no; open PR head audit-m020-r1: 443
== 2. merchant scope: real semantics vs class doc / dead branches / test stub / openapi / V200
 public Optional<String> currentMerchantScope() {
 AccessActor actor = currentActor();
 String merchantId = actor.merchantId();
 if (merchantId != null && !merchantId.isBlank()) {
 return Optional.of(merchantId);
 }
 throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
 }

service scope call lines: 173 ; orElseGet/platform-all lines: 
class doc L23-24:  * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律  * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域 
test stub: 
openapi 全量 desc lines: 20386 20409 
V200 L10: -- 平台管理员默认可监督商户自助层(与其它域 role_platform_admin 全授范式一致)。
MERCHANT_SCOPE_REQUIRED since: b1fd6534 2026-06-18; portal feature: 4aa4e251 2026-07-02 feat(merchant-portal): 商户专区切片3/4——我的对�
currentMerchantIdQuietly users outside access: backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java backend/src/main/java/com/complex/platform/ticketing/TicketingService.java backend/src/main/java/com/complex/platform/gate/AdmissionGuard.java 
== 3. dispute note overwrites platform dispute trail
record fields:  String id,  String contractId,  String triggerType,   String status,   List<Map<String, Object>> settlementItems,   String disputeReason,  String resolvedBy,  Instant resolvedAt,  String projectId,  Instant createdAt,  Instant updatedAt 
portal save L123-126:  } catch (RuntimeException exception) {  auditEventService.recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",  "merchant_portal_settlement", trimToNull(settlementId), null,  request == null ? null : request.idempotencyKey(), codeOf(exception)); 
DISPUTABLE L31-32:  /** 结算单可提异议的驻留态:开账后未结清且未挂起争议。 */  private static final Set<String> DISPUTABLE_STATUSES = 
settle() dispute open: 561:        String status = dispute != null ? "disputed" : "pending_settlement";
resolveDispute writes: 3:586-                settlement.disputeReason(), authorizationService.currentActor().id(), now, settlement.projectId(),
upsert overwrites: 250:                    dispute_reason = EXCLUDED.dispute_reason, resolved_by = EXCLUDED.resolved_by,
settle_opened metadata: 567- settlement.id(), null, actual.idempotencyKey(), Map.of("trigger", trigger, "status", status));
== 4. reason length: column / openapi / service / handler
backend/src/main/resources/db/migration/V162__contract_lifecycle.sql:121:    dispute_reason VARCHAR(500),
20432: description: 商户对未结清结算单提交异议,结算单挂起为「有争议」; 平台按既有裁决流(contract.settlement.dispute.re
20440: x-idempotency-required: true
20449: maxLength: 64
20463: description: 操作幂等键；重复提交必须返回同一处理结果，避免重复写入。
20464: maxLength: 128
20465: operator_note:
20467: description: 操作留痕备注。
20468: maxLength: 1000
20472: maxLength: 2000
20473: responses:
20474: "200":
service length() checks: 1; handler DataIntegrity: 151:     * 不拦父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug,仍应落 500 +
== 5. idempotency: norm vs portal
services calling idempotencyService.execute: 32; portal: 5
controllers using ApiRequests.requireIdempotencyKey: 33; portal inline: 
== 6. operator_note: dto / openapi vs service
dto: 6: String operatorNote,; service uses operatorNote: 0; audit metadata L134:  throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");
other services consuming operatorNote(): 18
== 7. permissions: code / seeds / complex_test actual (read-only)
requirePermission lines: 60: authorizationService.requirePermission("contract.view");;91: authorizationService.requirePermission("merchant.portal.settlement.view");;116: authorizationService.requirePermission("merchant.portal.settlement.dispute");;
contract.view|role_auditor_internal,role_platform_admin
merchant.portal.settlement.dispute|role_platform_admin
merchant.portal.settlement.view|role_platform_admin
merchant roles seeded: 0; flyway top: 427
== 8. i18n: handler translates ApiException messages
61:                FieldLabels.translate(exception.getMessage())));
31: "idempotency_key", "请勿重复提交，请刷新后再试");
256: p("settlement_id", "结算单号");
461: p("reason", "原因");
== 9. frontend callers / routes
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:83:      setRows(await api.get<MerchantZoneSettlementRow[]>("/api/v1/merchant-porta
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:109:      setRows(await api.get<MerchantZoneContractRow[]>("/api/v1/merchant-portal/c
apps/web/src/admin/adminRoutes.ts:48:  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance",
91:  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms:
92:  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
== 10. failure audit norm
services with recordFailure/withFailureAudit: 48; ContractService: 0; portal: 3; openapi x-audit-event-failure ops: 732
== 11. HTML anchor sentences (read-only)
<td>商户侧页面</td>
<td>餐饮、零售、酒吧等商户</td>
<td>在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。</td>
商户提出的投诉与异议，与客人投诉并入同一通道，由客户关怀部统一受理，不区分对客与对商户；商户只能就数字提出异议——包括漏算、少算、多算，不得对规则本身提出异议，规则以合同约定为准；提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补。商户专区须设|我的对账（结算与分成明细，有疑问可留言提异议）|
<h2>商户侧：在授权业务范围内全开放</h2>
<p class="lead">商户侧不是平台后台，也不是只读页面。商户在自己的授权业务范围内完整使用相关功能，但不能跨商户和看
== 12. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=2b8716e5f24df07d27e8d09e386441a83c14a4e9
== 13. tests rerun (unit + JDBC integration, complex_test, three vars only)
B5_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.796 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.497 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests=tests="2" failures=failures="0" errors=errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests=tests="10" failures=failures="0" errors=errors="0"
== 14. fix-state probes
orElseGet/platform-all: 0; recordFailure sites: 3; idempotencyService.execute: 1; resolvedBy carried: 1; max length: 1; english status suffix: 2; requireIdempotencyKey: 1
END=2026-09-20T11:30:54Z
```

## 3. 三线审计

- 第一性原理线：业务地图 L1848-1850/L1322、治理地图 L749-750/L256 段为上位；修后逐条追现行实现（探针 2–10）：scope 分支只剩 fail-closed（orElseGet/platform-all 0 处，类注释与真实现一致）；留言保留 resolvedBy/resolvedAt 并合并既有理由；合并后长度受 500 列宽约束；幂等经 IdempotencyService；三入口失败落 x-audit-event-failure 事件；报错全中文（英文 is required 经 FieldLabels 中文化）。
- 顺从式线：22 判据逐族——GEN-001/002/003/004/006/007/008/009/011/016 与 PGM-001 由 001–006 修复闭合；BSM-001/003/114、GEN-005/010/012/017/018 结论同 B1（通过/登记）；GEN-013/014/015 无本面；007（OpenAPI/V200 文本）维持 XM 不升格。
- 对抗式线（grilling）：无商户账号（含平台管理员）调三端点 → 403 MERCHANT_SCOPE_REQUIRED 且落 view_failed，不触库；平台开账（有争议）→裁决→商户留言 → 裁决人/时间保留、理由合并；同键重试只写一次并回放；不同键再留言 → 再合并（受 500 上限）；状态被推进为 settled 后重试同键 → 幂等回放原结果、不同键 → 409 中文文案；超长 → 400 中文；跨商户 → 403 + dispute_failed；成功审计 metadata（merchant_id/reason、count）未回退；把任一改写回退，对应测试/探针即再次报差。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–007 现态

001–006 已修（B3-r1），B4-r1 双 PASS；007 XM（M392/M021）；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1 §7 与 B2 登记项（operator_note 同域范式不消费、OpenAPI/V200 文本 XM、前端无异议入口 M023、失败审计规范不统一但本模块已落、无参重载仅测试、FinanceService 同类 orElse(null) 前提属 finance 模块）。

## 7. 结论

席甲 B5-r1 票：**PASS**。4 文件全读、22 判据逐族、只读探针与测试复跑；未改任何文件；落盘前未读 Grok 本次正文。
